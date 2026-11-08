package dev.hoi.client.screen.diplomacy;

import dev.hoi.protocol.country.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.*;

public final class DiplomacyJustificationTest {
    private DiplomacyJustificationTest() {}
    static void run(ClientGameTestContext context, CountryView base) {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2));
        var pending = fixture(base, "KOR", "timed_wargoal_receiver", "126일 남음 · 취소 시 정치력은 반환되지 않습니다.");
        context.runOnClient(c -> {
            for (String icon : List.of("timed_wargoal_initiator", "timed_wargoal_receiver", "wargoal_initiator", "wargoal_receiver"))
                if (c.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/diplomacy/relations/" + icon + ".png")).isEmpty())
                    throw new AssertionError("Missing source-backed justification icon: " + icon);
        });
        context.setScreen(() -> new DiplomacyCountryScreen(pending)); context.waitTicks(3);
        var point = context.computeOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            int y = DiplomacyHeader.body(dev.hoi.client.ui.hud.HudMenuBar.height(screen.width), screen.panelWidth()) + 8;
            if (!screen.relationTooltipAt(15, y).contains("126일 남음")) throw new AssertionError("Saved day134/ready260 justification status missing from visible relation row");
            if (screen.flagAt(40, y) == null || !screen.flagAt(40, y).tag().equals("PRK")) throw new AssertionError("Incoming justification identifies actual initiator flag");
            if (!screen.relationTooltipAt(screen.panelWidth() / 2.0 + 5, y).isEmpty()) throw new AssertionError("Status tooltip leaked into diplomacy action column");
            return new double[]{15 * c.getWindow().getGuiScale(), y * c.getWindow().getGuiScale()};
        });
        context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(3);
        context.takeScreenshot("hoi-diplomacy-justification-126-days-gui2");
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            var oldToken = screen.token();
            screen.update(CountryPayloads.Response.of(oldToken, fixture(base, "KOR", "timed_wargoal_receiver", "125일 남음")));
            int y = DiplomacyHeader.body(dev.hoi.client.ui.hud.HudMenuBar.height(screen.width), screen.panelWidth()) + 8;
            if (!screen.relationTooltipAt(15, y).contains("125일 남음")) throw new AssertionError("Current server day must refresh remaining justification days");
            screen.select("JAP");
            screen.update(CountryPayloads.Response.of(oldToken, pending));
            if (screen.view() != null || !screen.relationTooltipAt(15, y).isEmpty()) throw new AssertionError("Late previous-country justification leaked after selecting Japan");
            screen.update(CountryPayloads.Response.of(screen.token(), empty(base, "JAP")));
            if (!screen.view().relations().isEmpty()) throw new AssertionError("Unrelated country's snapshot retained justification");
            screen.select("KOR");
            screen.update(CountryPayloads.Response.of(screen.token(), pending));
            if (!screen.relationTooltipAt(15, y).contains("126일 남음")) throw new AssertionError("Returning to affected country must restore authoritative status");
            screen.update(CountryPayloads.Response.of(screen.token(), fixture(base, "KOR", "wargoal_receiver", "명분 유효 기간: 60일 남음")));
            if (!screen.view().relations().getFirst().icon().endsWith("/wargoal_receiver") || !screen.relationTooltipAt(15, y).contains("60일 남음"))
                throw new AssertionError("Completed justification must show usable goal and actual validity");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-diplomacy-ready-wargoal-gui2");
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            screen.update(CountryPayloads.Response.of(screen.token(), empty(base, "KOR")));
            if (!screen.view().relations().isEmpty()) throw new AssertionError("Cancelled or expired justification did not disappear");
            var disconnectedToken = screen.token();
            screen.update(new CountryPayloads.Response(disconnectedToken, ""));
            if (c.gui.screen() != null) throw new AssertionError("Revoked country session did not close");
            var reopened = new DiplomacyCountryScreen("KOR"); c.gui.setScreen(reopened);
            reopened.update(CountryPayloads.Response.of(disconnectedToken, pending));
            if (reopened.view() != null) throw new AssertionError("Stale previous-session goal reopened after reconnect");
            reopened.update(CountryPayloads.Response.of(reopened.token(), pending));
            if (reopened.view() == null || reopened.view().relations().size() != 1) throw new AssertionError("Fresh authenticated response failed to restore saved justification");
            reopened.update(CountryPayloads.Response.of(reopened.token(), fixture(base, "PRK", "timed_wargoal_initiator", "126일 남음")));
            if (!reopened.view().target().equals("KOR")) throw new AssertionError("Another country's outgoing response bypassed target authorization");
        });
        context.setScreen(() -> new DiplomacyCountryScreen(fixture(base, "PRK", "timed_wargoal_initiator", "126일 남음")));
        context.waitTicks(3); context.takeScreenshot("hoi-diplomacy-outgoing-justification-gui2");
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            int y = DiplomacyHeader.body(dev.hoi.client.ui.hud.HudMenuBar.height(screen.width), screen.panelWidth()) + 8;
            if (!screen.flagAt(40, y).tag().equals("KOR")) throw new AssertionError("Outgoing justification flag must name its target");
        });
    }
    private static CountryView fixture(CountryView base, String target, String icon, String status) {
        String other = target.equals("KOR") ? "PRK" : "KOR";
        var row = new CountryView.Relation(icon.startsWith("timed_") ? "전쟁 명분 정당화" : "전쟁 명분",
                "diplomacy/relations/" + icon, List.of(new CountryView.Choice(other, other)), "PRK → KOR\n" + status);
        return view(base, target, List.of(row));
    }
    private static CountryView empty(CountryView base, String target) { return view(base, target, List.of()); }
    private static CountryView view(CountryView base, String target, List<CountryView.Relation> rows) {
        return new CountryView("PRK", base.hud(), target, target, false, base.countries(), List.of(), List.of(), base.reports(), null, rows);
    }
}
