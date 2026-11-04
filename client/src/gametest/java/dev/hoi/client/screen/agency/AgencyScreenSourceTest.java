package dev.hoi.client.screen.agency;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.menu.*;
import java.util.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;

public final class AgencyScreenSourceTest {
    public static void run(ClientGameTestContext context) {
        String directory = System.getenv("HOI_AGENCY_PREVIEW");
        if (directory == null) return;
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        try {
            for (int stage = 0; stage <= 4; stage++) {
                var view = new AgencyPayloads.Response(Files.readString(Path.of(directory, "stage-" + stage + ".json"))).view();
                context.setScreen(() -> new AgencyScreen(null, view.session(), view, request -> {}));
                context.waitTicks(2);
                context.runOnClient(client -> {
                    if (UiPanelLayout.width(MenuTab.INTELLIGENCE, client.gui.screen().width) != 270)
                        throw new AssertionError("GUI2 agency sidebar must match the original 540 pixels");
                    for (var item : view.items()) if (item.id().startsWith("upgrade:")) {
                        var decorated = UiKeywordIcons.decorateLines(UiTooltips.styled(net.minecraft.network.chat.Component.literal(item.detail()), false), false);
                        long expected = item.detail().chars().filter(c -> c == '£').count();
                        long actual = decorated.getString().chars().filter(c -> c >= 0xe000 && c <= 0xf8ff).count();
                        if (actual != expected) throw new AssertionError("Missing original tooltip icons: " + item.id() + " " + actual + "/" + expected);
                        var lines = UiTooltips.exactLines(client.font, item.detail(), 199);
                        if (lines.stream().anyMatch(line -> client.font.width(line) > 175)) throw new AssertionError("Tooltip overflow: " + item.id());
                    }
                });
                if (stage == 0) {
                    context.takeScreenshot("hoi-agency-source-sidebar-gui2");
                    context.getInput().setCursorPos(24, (UiMenuBar.height(1280) + 130) * 2);
                    context.waitTicks(3); context.takeScreenshot("hoi-agency-source-indicator-gui2");
                    press(context, "암호학"); context.waitTicks(2);
                    context.takeScreenshot("hoi-agency-source-cryptology-locked-gui2");
                }
                press(context, "정보공동체"); context.waitTicks(2);
                context.takeScreenshot("hoi-agency-source-stage-" + stage + "-gui2");
                var buttons = context.computeOnClient(client -> client.gui.screen().children().stream()
                        .filter(c -> c instanceof AgencyUpgradeButton).map(c -> (AgencyUpgradeButton)c)
                        .map(b -> new Object[]{b.item().id(), b.getX(), b.getY()}).toList());
                if (buttons.size() != 23) throw new AssertionError("All original upgrades must fit GUI2 without scrolling: " + buttons.size());
                for (var button : buttons) {
                    if (stage > 0 && !button[0].toString().endsWith("DOMESTIC_INTELLIGENCE") && !button[0].toString().endsWith("COMMUNICATION_SECURITY")) continue;
                    context.getInput().setCursorPos(((Integer)button[1] + 40) * 2, ((Integer)button[2] + 44) * 2);
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-agency-source-" + button[0].toString().substring(8).toLowerCase(Locale.ROOT) + "-" + stage + "-gui2");
                }
            }
            for (String state : List.of("before-creation", "after-creation-kor", "after-creation-prc")) {
                var view = new AgencyPayloads.Response(Files.readString(Path.of(directory, state + ".json"))).view();
                context.setScreen(() -> new AgencyScreen(null, view.session(), view, request -> {}));
                context.getInput().setCursorPos(2400, 1300);
                context.waitTicks(3);
                context.takeScreenshot("hoi-agency-" + state + "-gui2");
                if (state.equals("after-creation-kor")) {
                    String[] ids = {"counter_intelligence", "spy_master", "recruit", "captured_count", "killed_count", "recruit_days"};
                    int[] xs = {100, 245, 35, 73, 106, 175};
                    int[] ys = {61, 46, 187, 187, 187, 188};
                    for (int i = 0; i < ids.length; i++) {
                        String id = ids[i];
                        var item = view.items().stream().filter(v -> v.id().equals(id)).findFirst().orElseThrow();
                        if (item.detail().isBlank()) throw new AssertionError("Missing issued sidebar tooltip: " + id);
                        context.runOnClient(client -> {
                            var lines = UiTooltips.exactLines(client.font, item.detail(), 199);
                            if (lines.stream().anyMatch(line -> client.font.width(line) > 175)) throw new AssertionError("Sidebar tooltip overflow: " + id);
                        });
                        context.getInput().setCursorPos(xs[i] * 2, (UiMenuBar.height(1280) + ys[i]) * 2);
                        context.waitTicks(3);
                        context.takeScreenshot("hoi-agency-sidebar-" + id + "-gui2");
                    }
                }
                if (state.equals("after-creation-kor")) {
                    press(context, "암호학"); context.waitTicks(2);
                    for (String id : List.of("cipher_level", "decryption_power")) {
                        var item = view.items().stream().filter(v -> v.id().equals(id)).findFirst().orElseThrow();
                        if (item.detail().isBlank() || item.detail().contains("$")) throw new AssertionError("Unresolved cryptology tooltip: " + id);
                        context.runOnClient(client -> {
                            var decorated = UiKeywordIcons.decorateLines(UiTooltips.styled(net.minecraft.network.chat.Component.literal(item.detail()), false), false);
                            long expected = java.util.regex.Pattern.compile("£[A-Za-z0-9_]+£?").matcher(item.detail()).results().count();
                            long actual = decorated.getString().chars().filter(c -> c >= 0xe000 && c <= 0xf8ff).count();
                            if (actual != expected) throw new AssertionError("Cryptology icon missing: " + id);
                        });
                        context.getInput().setCursorPos((id.equals("cipher_level") ? 80 : 246) * 2, (UiMenuBar.height(1280) + 237) * 2);
                        context.waitTicks(3); context.takeScreenshot("hoi-agency-" + id + "-gui2");
                    }
                    press(context, "정보공동체"); context.getInput().setCursorPos(2400, 1300);
                    context.waitTicks(2); context.takeScreenshot("hoi-agency-upgrade-detail-gui2");
                }
                if (state.endsWith("prc")) {
                    press(context, "정보공동체");
                    context.waitTicks(2);
                    var button = context.computeOnClient(client -> client.gui.screen().children().stream()
                            .filter(c -> c instanceof AgencyUpgradeButton b && b.item().id().equals("upgrade:HIDDEN_FRONT"))
                            .map(c -> (AgencyUpgradeButton)c).findFirst().orElseThrow());
                    context.getInput().setCursorPos((button.getX() + 40) * 2, (button.getY() + 44) * 2);
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-agency-hidden-front-gui2");
                }
            }
            for (int scale : new int[]{1, 3, 4}) {
                context.runOnClient(client -> client.options.guiScale().set(scale)); context.waitTicks(2);
                context.takeScreenshot("hoi-agency-source-scale-" + scale);
            }
            context.runOnClient(client -> client.options.guiScale().set(2));
            context.getInput().resizeWindow(1600, 1000); context.waitTicks(2);
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        context.setScreen(() -> null);
    }
    private static void press(ClientGameTestContext context, String label) {
        context.runOnClient(client -> {
            var screen = client.gui.screen();
            var button = screen.children().stream().filter(c -> c instanceof Button b && b.getMessage().getString().equals(label)).map(c -> (Button)c).findFirst().orElseThrow();
            var event = new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
            if (!screen.mouseClicked(event,false)) throw new AssertionError("Navigation click rejected: " + label + " active=" + button.active);
            screen.mouseReleased(event);
        });
    }
}
