package dev.hoi.client.screen.industry;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class IndustryDesignerButtonsTest {
    private IndustryDesignerButtonsTest() {}
    public static void run(ClientGameTestContext context) {
        for (int[] dimensions : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
            context.getInput().resizeWindow(dimensions[0], dimensions[1]);
            context.runOnClient(client -> client.options.guiScale().set(2));
            for (String branch : List.of("army", "navy", "air")) {
                String file = branch.equals("army") ? "tank-designer-fixture.json" : "branch-ready-" + (branch.equals("navy") ? "ship_hull_light_1" : "small_plane_airframe_1") + "-fixture.json";
                var fixture = IndustryScreenChecks.tankFixture(file);
                var requests = new ArrayList<IndustryPayloads.Request>();
                context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, fixture.session(), fixture, requests::add));
                context.getInput().setCursorPos(dimensions[0] - 2, dimensions[1] - 2);
                context.waitTicks(3);
                context.takeScreenshot("hoi-designer-buttons-" + branch + "-" + dimensions[0] + "-gui2");
                validate(context, fixture, requests, branch);
            }
        }
    }
    private static void validate(ClientGameTestContext context, IndustryView fixture, List<IndustryPayloads.Request> requests, String branch) {
        context.runOnClient(client -> {
            var screen = (IndustryScreen)client.gui.screen();
            int minY = UiMenuBar.height(screen.width) + client.font.lineHeight + 8;
            int width = Math.min(Math.min(screen.width - 24, Math.max(780, screen.width * 5 / 8)), (screen.height - minY - 12) * 1092 / 553);
            int x = (screen.width - width) / 2, y = Math.max(minY, (screen.height - width * 553 / 1092) / 2);
            var reset = button(screen, "설계 초기화");
            var save = button(screen, "설계 저장 · ");
            if (reset.getX() != x + 10 * width / 1092 || reset.getY() != y + 502 * width / 1092 || reset.getWidth() != 123 * width / 1092 || reset.getHeight() != 34 * width / 1092) throw new AssertionError("Reset must preserve the source rectangle");
            if (save.getX() != x + 400 * width / 1092 || save.getY() != y + 502 * width / 1092 || save.getWidth() != 123 * width / 1092 || save.getHeight() != 36 * width / 1092) throw new AssertionError("Save must preserve the full source sprite");
            var refit = button(screen,"최신 특성으로 개량");
            var manufacturer = button(screen,branch.equals("army") ? "전차 설계사 · " : branch.equals("navy") ? "함선 설계사 · " : "항공기 설계사 · ");
            if (refit.getRight()>manufacturer.getX()) throw new AssertionError("Upgrade and manufacturer hover regions must be disjoint");
            for (String action : List.of("최신 특성으로 개량", branch.equals("army") ? "전차 설계사 · " : branch.equals("navy") ? "함선 설계사 · " : "항공기 설계사 · ")) {
                var control = button(screen, action);
                if (control.active) throw new AssertionError("Unsupported manufacturer actions must stay disabled");
                click(screen, control, InputConstants.MOUSE_BUTTON_LEFT);
            }
            click(screen, reset, InputConstants.MOUSE_BUTTON_RIGHT);
            if (!requests.isEmpty()) throw new AssertionError("Disabled and right clicks must not request changes");
            click(screen, reset, InputConstants.MOUSE_BUTTON_LEFT);
            click(screen, button(screen, "설계 초기화"), InputConstants.MOUSE_BUTTON_LEFT);
            if (requests.size() != 1 || requests.getFirst().action() != IndustryPayloads.Action.TANK_RESET || !requests.getFirst().session().equals(fixture.session()) || requests.getFirst().revision() != fixture.revision()) throw new AssertionError("Reset must send one authenticated request");
        });
        context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, fixture.session(), fixture, requests::add));
        requests.clear();
        context.runOnClient(client -> {
            var screen = (IndustryScreen)client.gui.screen();
            var save = button(screen, "설계 저장 · ");
            if (!save.active) throw new AssertionError("Ready fixture must allow save");
            click(screen, save, InputConstants.MOUSE_BUTTON_RIGHT);
            if (!requests.isEmpty()) throw new AssertionError("Right click must not save");
            click(screen, save, InputConstants.MOUSE_BUTTON_LEFT);
            click(screen, button(screen, "설계 저장 · "), InputConstants.MOUSE_BUTTON_LEFT);
            if (requests.size() != 1 || requests.getFirst().action() != IndustryPayloads.Action.TANK_SAVE) throw new AssertionError("Save must send once while pending");
        });
        context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, fixture.session(), fixture, requests::add));
        requests.clear();
        context.runOnClient(client -> {
            var screen = (IndustryScreen)client.gui.screen();
            String title = branch.equals("army") ? "전차 설계사" : branch.equals("navy") ? "함선 설계사" : "항공기 설계사";
            click(screen, button(screen, title + " 창 닫기"), InputConstants.MOUSE_BUTTON_LEFT);
            if (requests.size() != 1 || requests.getFirst().action() != IndustryPayloads.Action.TANK_CLOSE) throw new AssertionError("Close must preserve the server request");
        });
    }
    private static Button button(IndustryScreen screen, String label) {
        return screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().startsWith(label)).map(Button.class::cast).findFirst().orElseThrow(() -> new AssertionError(label));
    }
    private static void click(IndustryScreen screen, Button button, int mouseButton) {
        screen.mouseClicked(new MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(mouseButton, 0)), false);
    }
}
