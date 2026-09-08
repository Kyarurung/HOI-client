package dev.hoi.client;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Renderer and input fixtures captured by IndustryGameTest; these are not a multiplayer session. */
final class IndustryScreenChecks {
    static void run(ClientGameTestContext context, CountryHud hud) {
        IndustryView fixture;
        try (var in = IndustryScreenChecks.class.getResourceAsStream("/industry-fixture.json")) {
            fixture = new IndustryProtocol.Response(new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8)).view();
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        var v = copy(fixture, hud, fixture.revision(), null);
        var requests = new ArrayList<IndustryProtocol.Request>();
        for (var tab : List.of(MenuTab.PRODUCTION, MenuTab.TRADE, MenuTab.LOGISTICS, MenuTab.RECRUITMENT)) {
            context.setScreen(() -> new IndustryScreen(tab, v.session(), v, requests::add));
            context.waitTicks(3); context.takeScreenshot("hoi-industry-" + tab.id());
            context.runOnClient(client -> {
                var screen = (IndustryScreen) client.gui.screen();
                check(screen.panelWidth() == HoiPanelLayout.width(tab, screen.width), "Original sidebar proportions");
                checkBounds(screen);
                for (String texture : java.util.stream.Stream.concat(v.equipment().stream().map(IndustryView.Equipment::texture),
                        java.util.stream.Stream.concat(v.battalions().stream().map(IndustryView.Choice::texture), v.companies().stream().map(IndustryView.Choice::texture))).toList())
                    check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + texture + ".png")).isPresent(), "Original external asset " + texture);
            });
            if (tab == MenuTab.TRADE) { click(context, "경제"); context.waitTicks(2); context.takeScreenshot("hoi-industry-economy"); click(context, "무역"); }
            context.getInput().resizeWindow(854, 480); context.waitTicks(3);
            context.runOnClient(client -> { var s = (IndustryScreen)client.gui.screen(); s.mouseScrolled(20, 170, 0, -100); checkBounds(s); });
            context.waitTicks(2); context.takeScreenshot("hoi-industry-compact-" + tab.id());
            context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
        }
        context.setScreen(() -> new IndustryScreen(MenuTab.TRADE, v.session(), v, requests::add)); context.waitTicks(2);
        var seller = v.partners().stream().filter(p -> p.route() && p.exports().getOrDefault("IRON", 0.0) > 0).findFirst().orElseThrow();
        click(context, seller.name() + "에서 수입"); context.waitTicks(2); context.takeScreenshot("hoi-industry-trade-contract");
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        click(context, "수입 계약 체결");
        context.runOnClient(client -> {
            var request = requests.getLast();
            check(request.action() == IndustryProtocol.Action.TRADE && request.item().equals(seller.id()) && request.other().equals("IRON") && request.amount() == 1,
                    "Trade submits the issued seller, resource and civilian factory count");
        });
        requests.clear();
        context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, v.session(), v, requests::add)); context.waitTicks(2);
        click(context, "공장 늘리기 · 보유량 안에서 배정");
        context.runOnClient(client -> {
            var s = (IndustryScreen)client.gui.screen(); var request = requests.getLast();
            check(request.action() == IndustryProtocol.Action.ASSIGN && request.amount() == v.lines().getFirst().factories() + 1
                    && request.revision() == v.revision(), "Only a bounded authenticated factory request");
            check(!button(s, "공장 늘리기 · 보유량 안에서 배정").active, "No duplicate request while waiting");
            s.update(copy(v, hud, v.revision() + 1, null));
        });
        click(context, "생산 라인 추가"); context.waitTicks(2); context.takeScreenshot("hoi-industry-equipment-picker");
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        context.setScreen(() -> new IndustryScreen(MenuTab.RECRUITMENT, v.session(), v, requests::add)); context.waitTicks(2);
        String template = v.templates().getFirst().name();
        click(context, template + " 훈련");
        context.runOnClient(client -> {
            check(requests.getLast().action() == IndustryProtocol.Action.RECRUIT && !requests.getLast().other().isEmpty(), "Training includes an issued deployment location");
            ((IndustryScreen)client.gui.screen()).update(copy(v, hud, v.revision() + 1, null));
        });
        click(context, template + " 편제 편집");
        context.runOnClient(client -> ((IndustryScreen)client.gui.screen()).update(copy(v, hud, v.revision() + 2, v.templates().getFirst())));
        context.waitTicks(3); context.takeScreenshot("hoi-industry-division-designer");
        context.getInput().setCursorPos(220,615); context.waitTicks(2); context.takeScreenshot("hoi-industry-division-equipment-tooltip");
        context.getInput().setCursorPos(1500,800);
        context.runOnClient(client -> { var s = (IndustryScreen)client.gui.screen(); check(!s.allowsMovement(), "Designer blocks movement"); checkBounds(s); });
        click(context, v.battalions().stream().filter(c -> c.id().equals(v.templates().getFirst().line().getFirst())).findFirst().orElseThrow().name());
        context.waitTicks(2); context.takeScreenshot("hoi-industry-battalion-choices");
        click(context, "대대 목록 닫기");
        context.getInput().resizeWindow(854, 480); context.waitTicks(3);
        context.runOnClient(client -> { var s = (IndustryScreen)client.gui.screen(); checkBounds(s); s.mouseScrolled(150, 160, 0, -100); checkBounds(s); });
        context.waitTicks(2); context.takeScreenshot("hoi-industry-compact-designer");
        context.runOnClient(client -> {
            var s = (IndustryScreen)client.gui.screen();
            s.update(IndustryView.revoked(v.session(), "권한 만료"));
            check(client.gui.screen() == null, "Revocation closes private screen");
            s.update(copy(v, hud, v.revision() + 100, v.templates().getFirst()));
            check(client.gui.screen() == null, "Late response cannot resurrect revoked screen");
        });
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
    private static IndustryView copy(IndustryView v, CountryHud hud, long revision, IndustryView.Template draft) {
        return new IndustryView(v.session(), revision, v.country(), hud, v.economy(), v.resources(), v.modifiers(), v.equipment(), v.lines(), v.partners(), v.trades(), v.templates(),
                v.battalions(), v.companies(), v.locations(), v.recruits(), v.deployed(), draft, "");
    }
    private static Button button(IndustryScreen screen, String name) {
        return (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow(() -> new AssertionError("Missing control: " + name));
    }
    private static void click(ClientGameTestContext context, String name) {
        context.runOnClient(client -> { var b = button((IndustryScreen)client.gui.screen(), name); check(b.active, "Enabled control: " + name); b.onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0)); });
    }
    private static void checkBounds(IndustryScreen screen) {
        var buttons = screen.children().stream().filter(w -> w instanceof Button).map(w -> (Button)w).toList();
        for (var b : buttons) check(b.getX() >= 0 && b.getY() >= 0 && b.getRight() <= screen.width && b.getBottom() <= screen.height, "Industry control inside viewport: " + b.getMessage().getString());
        for (int i = 0; i < buttons.size(); i++) for (int j = i + 1; j < buttons.size(); j++) {
            var a = buttons.get(i); var b = buttons.get(j);
            check(a.getRight() <= b.getX() || b.getRight() <= a.getX() || a.getBottom() <= b.getY() || b.getBottom() <= a.getY(),
                    "Controls must not overlap: " + a.getMessage().getString() + " / " + b.getMessage().getString());
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
