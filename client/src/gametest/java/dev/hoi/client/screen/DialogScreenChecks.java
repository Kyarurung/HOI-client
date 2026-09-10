package dev.hoi.client.screen;

import com.google.gson.Gson;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DialogScreenChecks {
    record Fixtures(List<DialogView> views, List<String> sounds) {}
    public static void run(ClientGameTestContext context) {
        Fixtures fixture;
        try (var input = DialogScreenChecks.class.getResourceAsStream("/dialog-fixtures.json")) {
            fixture = new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(input), StandardCharsets.UTF_8), Fixtures.class);
        } catch (IOException error) { throw new UncheckedIOException(error); }
        context.runOnClient(client -> {
            DialogProtocol.registerPayloadTypes(); DialogProtocol.registerPayloadTypes();
            for (var path : fixture.sounds()) if (client.getSoundManager().getSoundEvent(Identifier.fromNamespaceAndPath("hoi", path)) == null)
                throw new AssertionError("Missing super-event sound: " + path);
            for (var view : fixture.views()) {
                if (!view.image().isEmpty() && client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/" + view.image() + ".png")).isEmpty())
                    throw new AssertionError("Missing event picture: " + view.title());
                var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
                try { var payload = DialogProtocol.Show.of(view, true); DialogProtocol.Show.CODEC.encode(buffer, payload);
                    if (!view.equals(DialogProtocol.Show.CODEC.decode(buffer).view())) throw new AssertionError("Dialog codec must preserve original text");
                } finally { buffer.release(); }
            }
        });
        var choices = java.util.stream.IntStream.rangeClosed(1, 11).mapToObj(i -> new DialogView.Choice("option-" + i, "선택 " + i)).toList();
        var multiChoice = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.EVENT, "여러 선택지가 있는 국가 이벤트", "",
                "긴 이벤트 본문과 선택지가 작은 창에서도 겹치지 않아야 합니다.\n".repeat(35), "", "", "", List.of(), choices);
        var views = new ArrayList<>(fixture.views()); views.add(multiChoice);
        for (var view : views) {
            context.setScreen(() -> new DialogScreen(view)); context.waitTicks(4);
            context.takeScreenshot("hoi-dialog-" + view.kind().name().toLowerCase(Locale.ROOT));
        }
        for (int[] size : new int[][]{{2560, 1440}, {1600, 1000}, {854, 480}, {640, 360}}) {
            context.runOnClient(client -> client.options.guiScale().set(2));
            context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
            for (var view : views) {
                context.setScreen(() -> new DialogScreen(view)); context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = (DialogScreen) client.gui.screen();
                    if (size[0] >= 1600 && (screen.width != size[0] / 2 || screen.height != size[1] / 2))
                        throw new AssertionError("Reference captures require effective GUI scale 2");
                    float expectedScale=view.kind()==DialogView.Kind.EVENT||view.kind()==DialogView.Kind.GLOBAL_EVENT?0.9f:1;
                    if (screen.textScale()!=expectedScale) throw new AssertionError("Only country and global event text is reduced");
                    if (Math.abs(screen.panelLeft() * 2 + screen.panelWidth() - screen.width) > 1) throw new AssertionError("Dialog is not centered");
                    if (size[0] == 2560 && view.kind() == DialogView.Kind.SUPER_EVENT && screen.panelWidth() <= screen.panelHeight())
                        throw new AssertionError("Super event keeps the wide reference proportions");
                    if (size[0] == 2560 && (view.kind() == DialogView.Kind.EVENT || view.kind() == DialogView.Kind.GLOBAL_EVENT) && screen.panelWidth() >= screen.panelHeight())
                        throw new AssertionError("Country and global events keep portrait proportions");
                    if (screen.bodyViewportHeight() <= 0) throw new AssertionError("Choices must leave a visible body viewport");
                    for (var child : screen.children()) if (child instanceof Button b && (b.getX() < 0 || b.getY() < 0 || b.getRight() > screen.width || b.getBottom() > screen.height))
                        throw new AssertionError("Dialog control outside compact screen");
                    screen.mouseScrolled(screen.width / 2.0, screen.height / 2.0, 0, -100);
                    if (view == multiChoice) {
                        if (screen.scrollOffset() <= 0) throw new AssertionError("Long event body must scroll with many choices");
                        var reached = new HashSet<String>();
                        for (int page = 0; page < 11; page++) {
                            for (var child : screen.children()) if (child instanceof Button b && b.getMessage().getString().startsWith("선택 ")) {
                                if (b.getY() < 0 || b.getBottom() > screen.height) throw new AssertionError("Paged choice outside viewport");
                                reached.add(b.getMessage().getString());
                            }
                            var next = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("다음 선택지")).findFirst().orElseThrow();
                            if (!next.active) break;
                            next.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0));
                        }
                        if (reached.size() != 11) throw new AssertionError("Every server choice must remain accessible in a compact viewport");
                    }
                });
                context.waitTicks(2); context.takeScreenshot("hoi-dialog-" + view.kind().name().toLowerCase(Locale.ROOT) + "-" + size[0] + (size[0] >= 1600 ? "-gui2" : "-compact"));
            }
        }
        context.runOnClient(client -> {
            for (var kind : List.of(DialogView.Kind.EVENT, DialogView.Kind.GLOBAL_EVENT, DialogView.Kind.SUPER_EVENT)) {
                var sent = new ArrayList<String>();
                var view = new DialogView(UUID.randomUUID().toString(), 7, kind, "Enter 확인", "", "서버 선택지", "", "", "",
                        List.of(), List.of(new DialogView.Choice("issued-choice", "확인")));
                var screen = new DialogScreen(view) {
                    @Override protected void choose(String choice) { sent.add(view().token() + "/" + view().revision() + "/" + choice); }
                };
                client.gui.setScreen(screen);
                for (int key : new int[]{org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER}) {
                    int before = sent.size();
                    screen.keyPressed(new net.minecraft.client.input.KeyEvent(key, 0, 0));
                    if (sent.size() != before + 1 || !sent.getLast().equals(view.token() + "/7/issued-choice"))
                        throw new AssertionError("Enter must submit exactly the server-issued event choice");
                }
                var multiple = new DialogView(view.token(), 8, kind, view.title(), "", view.body(), "", "", "", List.of(),
                        List.of(new DialogView.Choice("a", "첫 선택"), new DialogView.Choice("b", "둘째 선택")));
                screen.update(multiple); screen.setFocused(null);
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0));
                if (!sent.getLast().equals(view.token() + "/8/") || client.gui.screen() != null)
                    throw new AssertionError("Enter without a selected option dismisses without choosing gameplay effects");
                client.gui.setScreen(screen);
                var second = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("둘째 선택")).findFirst().orElseThrow();
                screen.setFocused(second);
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0));
                if (!sent.getLast().equals(view.token() + "/8/b")) throw new AssertionError("Enter activates the focused server choice");
            }
            client.gui.setScreen(null);
            var one = fixture.views().getFirst().issued(UUID.randomUUID().toString(), 0);
            DialogClient.receive(DialogProtocol.Show.of(one, true));
            DialogClient.close(one.token());
            DialogClient.receive(DialogProtocol.Show.of(one.issued(one.token(), 1), false));
            if (client.gui.screen() != null) throw new AssertionError("Late refresh reopened a revoked private dialog");
        });
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
}
