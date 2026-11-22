package dev.hoi.client.screen.focus;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.protocol.focus.FocusPayloads;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

final class FocusForeignTest {
    private FocusForeignTest() {}
    static void run(ClientGameTestContext context, int width) {
        FocusPayloads.View view;
        try (var input = FocusForeignTest.class.getResourceAsStream("/hoi/focus/foreign-KOR.json")) {
            if (input == null) throw new AssertionError("Missing foreign focus fixture");
            view = new Gson().fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8), FocusPayloads.View.class);
        } catch (IOException error) { throw new AssertionError(error); }
        var sent = new ArrayList<FocusPayloads.Request>();
        var parent = context.computeOnClient(client -> new Screen(Component.literal("외교")) {});
        context.setScreen(() -> new FocusScreen(view, sent::add, "KOR", parent));
        context.waitTicks(3);
        context.takeScreenshot("hoi-focus-foreign-" + width + "-gui2");
        context.runOnClient(client -> {
            var screen = (FocusScreen)client.gui.screen();
            check(screen.target().equals("KOR"), "A foreign tree keeps its target");
            var node = view.nodes().stream().filter(n -> n.id().equals(view.detail().id())).findFirst().orElseThrow();
            var bounds = screen.nodeBounds(node.id());
            var click = new MouseButtonEvent(bounds[0] + bounds[2] / 2.0, bounds[1] + Math.min(12, bounds[3] / 2.0), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            check(screen.mouseClicked(click, false), "A foreign focus opens its detail");
            screen.mouseReleased(click);
            check(screen.selectedId().equals(node.id()), "The clicked foreign focus is selected");
            var start = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(button -> button.getMessage().getString().equals("중점 시작")).findFirst().orElseThrow();
            check(!start.visible && !start.active, "A foreign focus has no start button");
            var area = new MouseButtonEvent(start.getX() + start.getWidth() / 2.0, start.getY() + start.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            screen.mouseClicked(area, false); screen.mouseReleased(area);
            screen.keyPressed(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(screen.hoverTooltip(node).contains("§Y" + node.name()), "A foreign focus shows its tooltip");
        });
        context.waitTicks(45);
        context.takeScreenshot("hoi-focus-foreign-detail-" + width + "-gui2");
        check(!sent.isEmpty() && sent.stream().allMatch(request -> request.target().equals("KOR")), "Every foreign request names the target");
        check(sent.stream().allMatch(request -> request.action() == FocusPayloads.Action.REFRESH || request.action() == FocusPayloads.Action.OPEN), "A foreign tree never starts or cancels a focus");
        context.runOnClient(client -> client.gui.screen().onClose());
        context.waitTicks(2);
        check(context.computeOnClient(client -> DialogCoordinator.contentScreen() == parent), "Closing a foreign tree returns to the diplomacy screen");
        context.setScreen(() -> null);
        context.waitTicks(1);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
