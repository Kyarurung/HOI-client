package dev.hoi.client.screen.diplomacy;

import dev.hoi.protocol.diplomacy.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import java.nio.file.*;
import java.util.*;

public final class DiplomacyWorldTensionChecks {
    private static DiplomacyWorldTensionView fixture(String directory, String name) {
        try { return new DiplomacyWorldTensionPayloads.Response(UUID.randomUUID().toString(), Files.readString(Path.of(directory, name + ".json"))).view(); }
        catch (java.io.IOException error) { throw new AssertionError("World tension server fixture unavailable: " + name, error); }
    }
    private static List<Button> buttons(DiplomacyWorldTension screen, java.util.function.Predicate<String> label) {
        return screen.children().stream().filter(w -> w instanceof Button candidate && label.test(candidate.getMessage().getString())).map(w -> (Button)w).toList();
    }
    public static void run(ClientGameTestContext context) {
        String directory = System.getenv("HOI_COMMANDER_PREVIEW"); if (directory == null) return;
        DiplomacyWorldTensionPayloads.registerPayloadTypes(); DiplomacyWorldTensionPayloads.registerPayloadTypes();
        var history = fixture(directory, "kor-world-tension-history");
        var wars = fixture(directory, "kor-world-tension-wars");
        if (history.entries().isEmpty() || wars.wars().size() < 3) throw new AssertionError("Server campaign fixtures must contain real tension history and wars");
        context.getInput().resizeWindow(2560, 1440); context.runOnClient(client -> client.options.guiScale().set(2));
        context.setScreen(() -> new DiplomacyWorldTension(history)); context.waitTicks(3);
        context.runOnClient(client -> {
            var screen = (DiplomacyWorldTension)client.gui.screen();
            screen.select(DiplomacyWorldTensionView.Sort.COUNTRY);
            var names = history.entries().stream().map(DiplomacyWorldTensionView.Entry::name).sorted().toList();
            if (!screen.view().entries().getFirst().name().equals(names.getFirst())) throw new AssertionError("Country sorting");
            screen.select(DiplomacyWorldTensionView.Sort.TENSION);
            double highest = history.entries().stream().mapToDouble(DiplomacyWorldTensionView.Entry::current).max().orElseThrow();
            if (screen.view().entries().getFirst().current() != highest) throw new AssertionError("Tension sorting");
            if (!screen.tooltip(150, 95).equals("국가순으로 정렬")) throw new AssertionError("Sort buttons keep the original SORT_BY tooltips: " + screen.tooltip(150, 95));
            screen.update(new DiplomacyWorldTensionPayloads.Response(UUID.randomUUID().toString(), ""));
            if (client.gui.screen() != screen) throw new AssertionError("Stale rejection closed current screen");
        });
        dev.hoi.client.ResearchScreenTest.gui2Screenshot(context, "hoi-world-tension-history");
        var allied = wars.wars().stream().filter(w -> w.major() && w.defenders().stream().anyMatch(p -> !p.faction().isEmpty())).findFirst().orElseThrow(() -> new AssertionError("Faction war fixture missing"));
        context.setScreen(() -> new DiplomacyWorldTension(wars)); context.waitTicks(2);
        context.runOnClient(client -> ((DiplomacyWorldTension)client.gui.screen()).select(DiplomacyWorldTensionView.Sort.WARS));
        context.waitTicks(3); dev.hoi.client.ResearchScreenTest.gui2Screenshot(context, "hoi-world-tension-wars");
        context.runOnClient(client -> {
            var screen = (DiplomacyWorldTension)client.gui.screen();
            var entry = buttons(screen, allied.name()::equals).stream().findFirst().orElseThrow(() -> new AssertionError("War entry must be clickable"));
            var close = buttons(screen, "닫기"::equals).stream().findFirst().orElseThrow();
            float scale = screen.scale();
            if (entry.getX() - close.getX() != Math.round(DiplomacyWorldTension.LIST_X * scale) - Math.round(204 * scale) || entry.getWidth() != Math.round(449 * scale))
                throw new AssertionError("War entry must use world_tension_war_entry x37 and width449: " + entry.getX() + " " + entry.getWidth());
            double ex = (entry.getX() - (close.getX() - Math.round(204 * scale))) / scale, ey = (entry.getY() - close.getY()) / scale + 475;
            if (!screen.tooltip(ex + 15, ey + 15).equals("하나 이상의 강대국이 참전한 전쟁")) throw new AssertionError("Major war marker keeps WAR_HAS_MAJOR: " + screen.tooltip(ex + 15, ey + 15));
            if (!screen.tooltip(ex + 150, ey + 20).equals("클릭하여 §Y" + allied.name() + "§!의 전쟁 개요 열기")) throw new AssertionError("War entry keeps CLICK_FOR_WAR_OVERVIEW: " + screen.tooltip(ex + 150, ey + 20));
            screen.openWar(allied.id());
        });
        context.waitTicks(3); dev.hoi.client.ResearchScreenTest.gui2Screenshot(context, "hoi-world-tension-war-detail");
        context.runOnClient(client -> {
            var screen = (DiplomacyWorldTension)client.gui.screen();
            if (!screen.headerTooltip(30, 165).equals("참전국") || !screen.headerTooltip(1080, 170).equals("사상자") || !screen.headerTooltip(250, 170).equals("전쟁 명분"))
                throw new AssertionError("War overview header icons keep the waroverview tooltips");
            if (!screen.headerTooltip(150, 170).isEmpty()) throw new AssertionError("Only the original header icons have tooltips");
            var attacker = allied.attackers().stream().filter(DiplomacyWorldTensionView.Participant::wargoal).findFirst().orElseThrow();
            var attackerRow = buttons(screen, attacker.name()::equals).stream().findFirst().orElseThrow();
            int headerOffset = Math.round(204 * screen.scale()) - Math.round(193 * screen.scale());
            var expand = buttons(screen, String::isEmpty).stream().filter(b -> b.getY() - attackerRow.getY() == headerOffset).toList();
            if (expand.size() != 1) throw new AssertionError("The faction side starts with its original faction header on the participants' first line");
            if (!screen.tooltip(25 + 237 + 5, 193 + 7 + 5).equals("전쟁 명분")) throw new AssertionError("War goal holder keeps the wargoal icon tooltip");
            var members = allied.defenders().stream().filter(p -> !p.faction().isEmpty()).map(DiplomacyWorldTensionView.Participant::name).toList();
            if (buttons(screen, members::contains).size() != members.size()) throw new AssertionError("Expanded faction lists every member");
            expand.getFirst().onPress(new net.minecraft.client.input.KeyEvent(0, 0, 0));
            if (!buttons(screen, members::contains).isEmpty()) throw new AssertionError("Collapsed faction hides its members");
            buttons(screen, String::isEmpty).stream().filter(b -> b.getY() - attackerRow.getY() == headerOffset).findFirst().orElseThrow().onPress(new net.minecraft.client.input.KeyEvent(0, 0, 0));
            if (buttons(screen, members::contains).size() != members.size()) throw new AssertionError("Expanding again restores the faction members");
        });
        context.runOnClient(client -> {
            var screen = (DiplomacyWorldTension)client.gui.screen(); screen.onClose();
            if (screen.warId() != null || client.gui.screen() != screen) throw new AssertionError("Detail close must return to history");
            screen.update(new DiplomacyWorldTensionPayloads.Response(screen.token(), ""));
            if (client.gui.screen() != null) throw new AssertionError("Revoked assignment must close tension screen");
        });
    }
}
