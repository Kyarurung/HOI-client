package dev.hoi.client.screen.doctrine;

import dev.hoi.protocol.dialog.DialogPayloads;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.client.screen.dialog.DialogCompletion;
import dev.hoi.client.screen.dialog.DialogStack;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DoctrineNotificationTest {
    public static void run(ClientGameTestContext context) {
        int previousScale = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            String directory = System.getenv("HOI_COMPLETION_PREVIEW");
            var reward = read(directory, "kor-doctrine-reward-notification").issued(java.util.UUID.randomUUID().toString(), 1);
            var milestone = read(directory, "kor-doctrine-milestone-notification").issued(java.util.UUID.randomUUID().toString(), 1);
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var sent = new ArrayList<String>();
                context.setScreen(() -> new DialogCompletion(milestone, (view, choice) -> sent.add(choice)));
                context.waitTicks(3);
                context.takeScreenshot("hoi-doctrine-notifications-before-" + size[0] + "-gui2");
                context.setScreen(() -> {
                    var stack = new DialogStack(null, (view, choice) -> sent.add(view.token() + ":" + choice));
                    stack.add(reward); stack.add(milestone); return stack;
                });
                context.waitTicks(4);
                context.takeScreenshot("hoi-doctrine-notifications-" + size[0] + "-gui2");
                context.runOnClient(client -> {
                    var stack = (DialogStack)client.gui.screen();
                    if (stack.layers().stream().filter(DoctrineNotification.class::isInstance).count() != 2) throw new AssertionError("Both doctrine notifications must remain visible");
                    var window = (DoctrineNotification)stack.find(milestone.token());
                    var notification = (DoctrineNotification)stack.find(reward.token());
                    int rewardX = notification.panelLeft() + notification.panelWidth() - 12;
                    int rewardY = notification.panelTop() + 12;
                    if (stack.inputAt(rewardX, rewardY) != notification) throw new AssertionError("Reward owns its exposed input region");
                    int overlapX = Math.max(window.panelLeft(), notification.panelLeft()) + 3;
                    int overlapY = Math.max(window.panelTop(), notification.panelTop()) + 3;
                    if (overlapX < Math.min(window.panelLeft() + window.panelWidth(), notification.panelLeft() + notification.panelWidth())
                            && overlapY < Math.min(window.panelTop() + window.panelHeight(), notification.panelTop() + notification.panelHeight())
                            && stack.inputAt(overlapX, overlapY) != window) throw new AssertionError("Foreground milestone owns overlapping input");
                    window.keyPressed(new KeyEvent(257, 0, 0));
                    window.keyPressed(new KeyEvent(257, 0, 0));
                    if (!sent.equals(List.of(milestone.token() + ":ack"))) throw new AssertionError("Repeated input cannot acknowledge twice");
                    window.update(milestone.issued(java.util.UUID.randomUUID().toString(), milestone.revision() + 10));
                    window.keyPressed(new KeyEvent(257, 0, 0));
                    window.update(milestone.issued(milestone.token(), 0));
                    window.keyPressed(new KeyEvent(257, 0, 0));
                    if (sent.size() != 1) throw new AssertionError("Unrelated and stale responses preserve acknowledgement lock");
                    window.update(milestone.issued(milestone.token(), milestone.revision() + 1));
                    window.keyPressed(new KeyEvent(257, 0, 0));
                    if (sent.size() != 2) throw new AssertionError("New server revision releases pending state");
                    stack.mouseClicked(mouse(rewardX, rewardY, 0), false);
                    if (sent.size() != 2) throw new AssertionError("Left click does not dismiss reward");
                    stack.mouseClicked(mouse(rewardX, rewardY, 1), false);
                    stack.mouseClicked(mouse(rewardX, rewardY, 1), false);
                    if (sent.size() != 3 || !sent.getLast().equals(reward.token() + ":ack")) throw new AssertionError("Right click dismisses reward once");
                });
                var longBody = new DialogView(milestone.token(), 3, milestone.kind(), milestone.title(), milestone.subtitle(),
                        (milestone.body() + "\n\n").repeat(16), milestone.flag(), milestone.image(), milestone.sound(), milestone.tiles(), milestone.choices(), milestone.presentation());
                context.setScreen(() -> new DoctrineNotification(longBody, (view, choice) -> sent.add(choice)));
                context.waitTicks(3);
                context.takeScreenshot("hoi-doctrine-notification-scroll-top-" + size[0] + "-gui2-synthetic");
                context.runOnClient(client -> {
                    var window = (DoctrineNotification)client.gui.screen();
                    int x = window.panelLeft() + window.panelWidth() / 2;
                    int y = window.panelTop() + 60;
                    window.mouseScrolled(x, y, 0, -100);
                    if (window.scrollOffset() <= 0) throw new AssertionError("Long source text scrolls within its body");
                    int bottom = window.scrollOffset();
                    window.mouseScrolled(window.panelLeft() - 1, y, 0, 100);
                    if (window.scrollOffset() != bottom) throw new AssertionError("Outside wheel preserves body scroll");
                });
                context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-notification-scroll-bottom-" + size[0] + "-gui2-synthetic");
                context.runOnClient(client -> {
                    var window = (DoctrineNotification)client.gui.screen();
                    int x = window.panelLeft() + window.panelWidth() - 6;
                    int y = window.panelTop() + 42;
                    int bottom = window.scrollOffset();
                    window.mouseClicked(mouse(x, y + 1, 0), false);
                    if (window.scrollOffset() >= bottom) throw new AssertionError("Up arrow scrolls source text");
                    window.mouseClicked(mouse(x, y + 12, 0), false);
                    window.mouseDragged(mouse(x, y + 12, 0), 0, 0);
                    window.mouseReleased(mouse(x, y + 12, 0));
                    int released = window.scrollOffset();
                    if (window.mouseDragged(mouse(x, y + 50, 0), 0, 38) || window.scrollOffset() != released) throw new AssertionError("Released scrollbar does not drag");
                });
            }
        } catch (Exception failure) { throw new AssertionError("Doctrine notification source fixture failed", failure); }
        finally { context.setScreen(() -> null); context.runOnClient(client -> client.options.guiScale().set(previousScale)); }
    }
    private static MouseButtonEvent mouse(double x, double y, int button) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
    }
    private static DialogView read(String directory, String name) throws Exception {
        if (directory != null) return new DialogPayloads.Show(Files.readString(Path.of(directory, name + ".json")), true).view();
        try (var input = DoctrineNotificationTest.class.getResourceAsStream("/commander/" + name + ".json")) {
            if (input == null) throw new AssertionError("Missing source fixture: " + name);
            return new DialogPayloads.Show(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), true).view();
        }
    }
}
