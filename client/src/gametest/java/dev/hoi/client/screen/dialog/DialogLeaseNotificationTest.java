package dev.hoi.client.screen.dialog;

import dev.hoi.protocol.dialog.DialogView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

public final class DialogLeaseNotificationTest {
    private DialogLeaseNotificationTest() {}
    public static void run(ClientGameTestContext context) {
        for (int width : List.of(2560, 1280)) {
            context.getInput().resizeWindow(width, width * 9 / 16);
            context.runOnClient(client -> client.options.guiScale().set(2));
            for (String presentation : List.of("lend_lease_request", "lend_lease_reply")) {
                String body = presentation.equals("lend_lease_request") ? "검증 장비" : "§e검증 국가§r 측이 우리의 무기대여 요청에 응답하였습니다.\n현재 무기대여:\n   매월:\n      §e검증 장비§r의 §e7§r\n   일회성:\n      §e검증 장비§r의 §e1§r";
                String choice = presentation.equals("lend_lease_request") ? "answer_lease:fixture" : "confirm";
                var view = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.DIPLOMACY, presentation.equals("lend_lease_request") ? "무기대여 요청" : "무기대여 요청의 답변", "검증 국가", body, "country/prk/flag", "", "", List.of(), List.of(new DialogView.Choice(choice, "확인")), presentation);
                var sent = new ArrayList<String>();
                context.setScreen(() -> new DialogLeaseNotification(view, (v, id) -> sent.add(id)));
                context.waitTicks(3);
                context.takeScreenshot("hoi-" + presentation + "-synthetic-" + width + "-gui2");
                if (presentation.equals("lend_lease_reply")) {
                    context.runOnClient(client -> ((DialogLeaseNotification)client.gui.screen()).mouseScrolled(0, 0, 0, -100));
                    context.waitTicks(2);
                    context.takeScreenshot("hoi-" + presentation + "-synthetic-bottom-" + width + "-gui2");
                    scrollbar(context, width);
                }
                context.runOnClient(client -> {
                    var screen = (DialogLeaseNotification)client.gui.screen();
                    if (!screen.view().body().equals(body)) throw new AssertionError("Reply whitespace changed");
                    var button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).findFirst().orElseThrow();
                    button.onPress(new KeyEvent(257, 0, 0)); button.onPress(new KeyEvent(257, 0, 0));
                    if (!sent.equals(List.of(choice))) throw new AssertionError("Notification confirmation was duplicated");
                });
            }
        }
    }
    private static int[] body(ClientGameTestContext context, String name) {
        int[] bounds = context.computeOnClient(client -> {
            var screen = (DialogLeaseNotification)client.gui.screen();
            double unit = screen.panelWidth() / 350.0;
            float gui = client.getWindow().getGuiScale();
            return new int[]{Math.round((float)(screen.panelLeft() + 21 * unit) * gui), Math.round((float)(screen.panelTop() + 76 * unit) * gui), Math.round((float)(308 * unit) * gui), Math.round((float)(110 * unit) * gui)};
        });
        context.waitTicks(2);
        try {
            var image = ImageIO.read(context.takeScreenshot(name).toFile());
            return image.getRGB(bounds[0], bounds[1], bounds[2], bounds[3], null, 0, bounds[2]);
        } catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }
    private static void scrollbar(ClientGameTestContext context, int width) {
        int[] bottom = body(context, "hoi-lease-scroll-wheel-bottom-" + width);
        for (int button : new int[]{MOUSE_BUTTON_RIGHT, MOUSE_BUTTON_LEFT}) {
            context.runOnClient(client -> {
                var screen = (DialogLeaseNotification)client.gui.screen();
                double unit = screen.panelWidth() / 350.0;
                var event = new net.minecraft.client.input.MouseButtonEvent(screen.panelLeft() + 337 * unit, screen.panelTop() + 84 * unit, new net.minecraft.client.input.MouseButtonInfo(button, 0));
                screen.mouseClicked(event, false);
                screen.mouseReleased(event);
            });
            boolean unchanged = Arrays.equals(bottom, body(context, "hoi-lease-scroll-arrow-" + button + "-" + width));
            if (unchanged != (button == MOUSE_BUTTON_RIGHT)) throw new AssertionError("Notification scrollbar left/right click changed the wrong state");
        }
        context.runOnClient(client -> {
            var screen = (DialogLeaseNotification)client.gui.screen();
            double unit = screen.panelWidth() / 350.0;
            var from = new net.minecraft.client.input.MouseButtonEvent(screen.panelLeft() + 337 * unit, screen.panelTop() + 110 * unit, new net.minecraft.client.input.MouseButtonInfo(MOUSE_BUTTON_LEFT, 0));
            var to = new net.minecraft.client.input.MouseButtonEvent(from.x(), screen.panelTop() + 162 * unit, new net.minecraft.client.input.MouseButtonInfo(MOUSE_BUTTON_LEFT, 0));
            screen.mouseClicked(from, false);
            screen.mouseDragged(to, 0, to.y() - from.y());
            screen.mouseReleased(to);
        });
        if (!Arrays.equals(bottom, body(context, "hoi-lease-scroll-drag-bottom-" + width))) throw new AssertionError("Notification drag did not reach the same final body as the wheel");
    }
}
