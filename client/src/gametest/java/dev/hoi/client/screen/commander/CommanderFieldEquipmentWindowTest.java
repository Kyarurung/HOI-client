package dev.hoi.client.screen.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.map.commander.CommanderCombatLogHud;
import dev.hoi.protocol.commander.CommanderOverviewPayloads.Tab;
import dev.hoi.protocol.commander.CommanderOverviewPayloads.View;
import dev.hoi.protocol.commander.CommanderRecordPayloads.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class CommanderFieldEquipmentWindowTest {
    private CommanderFieldEquipmentWindowTest() {}
    static void run(ClientGameTestContext game, CommanderOverview overview, View view, String directory, String suffix) throws IOException {
        var tab = overview.tab(); String name = tab.name().toLowerCase(Locale.ROOT);
        var fixture = new EquipmentSnapshot(Files.readString(Path.of(directory, "kor-field-equipment-" + name + ".json"))).equipment();
        var requests = new ArrayList<Request>();
        try {
            game.runOnClient(client -> {
                CommanderCombatLogHud.requestObserver = requests::add;
                check(overview.equipment() == null, "The overview opens without the field equipment");
                press(overview, overview.frame().x(CommanderOverview.equipmentBox().x() + 18), overview.frame().y(CommanderOverview.equipmentBox().y() + 18));
                check(overview.equipment() != null && requests.size() == 1 && requests.getFirst().action() == Action.EQUIPMENT && requests.getFirst().target().equals(tab.name()), "The header button asks for its field equipment: " + requests);
                overview.accept(reply(fixture, UUID.randomUUID().toString()));
                check(overview.equipment().equipment() == null, "Another token is ignored");
                overview.accept(reply(fixture, requests.getFirst().token()));
                check(overview.equipment().equipment() != null, "The window accepts its own snapshot");
            });
            away(game);
            game.takeScreenshot("hoi-field-equipment-" + name + suffix);
            if (!game.computeOnClient(c -> overview.covers(overview.frame().x(CommanderOverview.equipmentBox().x() + 18), overview.frame().y(CommanderOverview.equipmentBox().y() + 18)))) {
                hover(game, overview, CommanderOverview.equipmentBox().x() + 18, CommanderOverview.equipmentBox().y() + 18, true);
                check(game.computeOnClient(c -> overview.hoveredTooltip()).equals(view.label("EQUIPMENT_DETAILS")) && !view.label("EQUIPMENT_DETAILS").isBlank(), "The header button shows its original tooltip");
            }
            game.runOnClient(client -> {
                var window = overview.equipment(); var frame = window.frame();
                check(frame.x(CommanderFieldEquipmentWindow.WIDTH) <= client.getWindow().getGuiScaledWidth() + .5 && window.bottom() <= client.getWindow().getGuiScaledHeight() + .5 && overview.covers(frame.x(160), frame.y(60)), "The field equipment stays inside the screen");
                for (int i = 0; i < fixture.totals().size(); i++) {
                    var cell = window.layout().cells().get(i);
                    check(window.source(frame.x(cell.iconX() + 8), frame.y(80)).equals(fixture.totals().get(i).tooltip()) && window.source(frame.x(cell.textX() + 8), frame.y(80)).equals(fixture.totals().get(i).tooltip()), "Total " + i + " shows its original tooltip");
                }
                for (var entry : fixture.rows()) check((client.font.width(entry.amount()) + 1) * dev.hoi.client.ui.UiText.scale(client.font) <= frame.size(190) + .5, "The whole amount fits its row: " + entry.amount());
                var row = fixture.rows().getFirst(); var box = window.rowBox(row.id());
                check(window.source(frame.x(box.x() + 100), frame.y(box.y() + 15)).equals(fixture.label("EQUIPMENT_VARIANTS_TOGGLE_ON")), "A closed archetype offers its details");
                press(overview, frame.x(box.x() + 100), frame.y(box.y() + 15));
                check(window.expanded().equals(Set.of(row.id())) && window.source(frame.x(box.x() + 100), frame.y(box.y() + 15)).equals(fixture.label("EQUIPMENT_VARIANTS_TOGGLE_OFF")), "A click opens the variants of an archetype");
                check(window.source(frame.x(box.x() + 100), frame.y(box.y() + 30 + 20)).isEmpty(), "A variant has no tooltip like the original");
                if (fixture.rows().size() > 1) check(window.rowBox(fixture.rows().get(1).id()).y() == box.y() + 30 + row.items().size() * window.layout().itemHeight(), "The next archetype follows the variants");
            });
            away(game);
            game.takeScreenshot("hoi-field-equipment-" + name + "-open" + suffix);
            game.runOnClient(client -> {
                var window = overview.equipment(); var frame = window.frame();
                for (var row : fixture.rows()) window.expanded().add(row.id());
                overview.mouseScrolled(frame.x(160), frame.y(200), 0, -1000);
                var last = window.rowBox(fixture.rows().getLast().id()); int items = fixture.rows().getLast().items().size() * window.layout().itemHeight();
                check(last.y() + 30 + items <= window.listBottom() + .5, "The wheel reaches the last variant");
            });
            away(game);
            game.takeScreenshot("hoi-field-equipment-" + name + "-end" + suffix);
            game.runOnClient(client -> {
                var window = overview.equipment(); var frame = window.frame();
                overview.mouseScrolled(frame.x(160), frame.y(200), 0, 1000);
                var box = window.rowBox(fixture.rows().getFirst().id());
                press(overview, frame.x(box.x() + 100), frame.y(box.y() + 15));
                check(!window.expanded().contains(fixture.rows().getFirst().id()), "A second click closes the variants");
            });
            if (tab == Tab.NAVY) headquarters(game, overview, view, suffix);
            game.runOnClient(client -> {
                if (tab == Tab.NAVY) {
                    press(overview, overview.frame().x(CommanderOverview.lossesBox().x() + 18), overview.frame().y(CommanderOverview.lossesBox().y() + 18));
                    if (overview.losses() != null) {
                        check(overview.equipment() == null && requests.stream().anyMatch(request -> request.action() == Action.CLOSE_EQUIPMENT), "The losses replace the field equipment");
                        press(overview, overview.frame().x(CommanderOverview.equipmentBox().x() + 18), overview.frame().y(CommanderOverview.equipmentBox().y() + 18));
                    }
                }
                if (overview.equipment() == null) return;
                check(overview.losses() == null, "The field equipment replaces the losses");
                overview.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                check(overview.equipment() == null && requests.getLast().action() == Action.CLOSE_EQUIPMENT, "Escape closes the field equipment before the overview");
            });
        } finally { game.runOnClient(client -> CommanderCombatLogHud.requestObserver = null); }
    }
    private static void headquarters(ClientGameTestContext game, CommanderOverview overview, View view, String suffix) {
        boolean reachable = game.computeOnClient(c -> !overview.covers(overview.frame().x(CommanderOverview.headquartersBox().x() + 18), overview.frame().y(CommanderOverview.headquartersBox().y() + 18)));
        if (!reachable) return;
        hover(game, overview, CommanderOverview.headquartersBox().x() + 18, CommanderOverview.headquartersBox().y() + 18, true);
        check(game.computeOnClient(c -> overview.hoveredTooltip()).equals(view.label("naval_headquarters")) && !view.label("naval_headquarters").isBlank(), "The headquarters button shows its original tooltip");
        game.runOnClient(client -> {
            press(overview, overview.frame().x(CommanderOverview.headquartersBox().x() + 18), overview.frame().y(CommanderOverview.headquartersBox().y() + 18));
            check(overview.headquarters() != null && overview.equipment() != null, "The headquarters open beside the field equipment like the original");
        });
        away(game);
        game.takeScreenshot("hoi-naval-headquarters" + suffix);
        game.runOnClient(client -> {
            var window = overview.headquarters(); var frame = window.frame();
            check(frame.x(CommanderNavalHeadquartersWindow.WIDTH) <= client.getWindow().getGuiScaledWidth() + .5 && frame.y(CommanderNavalHeadquartersWindow.HEIGHT) <= client.getWindow().getGuiScaledHeight() + .5, "The headquarters stay inside the screen");
            check(window.source(frame.x(288), frame.y(76), view).equals(view.label("NAVAL_HEADQUARTER_ACTIVE_TT")) && view.label("NAVAL_HEADQUARTER_ACTIVE").contains("0") && view.label("NAVAL_HEADQUARTER_ACTIVE").contains("3"), "The assigned count shows its original text: " + view.label("NAVAL_HEADQUARTER_ACTIVE"));
            check(window.source(frame.x(CommanderNavalHeadquartersWindow.WIDTH - 27), frame.y(26), view).equals(view.label("CLOSE")), "The close button shows its tooltip");
            overview.mouseScrolled(frame.x(200), frame.y(300), 0, -2);
            check(window.scroll() == 74, "The wheel scrolls the list picture like the original: " + window.scroll());
            overview.mouseScrolled(frame.x(200), frame.y(300), 0, 100);
            check(window.scroll() == 0, "The list picture scrolls back");
            float left = frame.x();
            var event = new MouseButtonEvent(frame.x(200), frame.y(300), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            check(overview.mouseClicked(event, false), "The headquarters take the press");
            overview.mouseDragged(new MouseButtonEvent(event.x() + 12, event.y(), event.buttonInfo()), 12, 0);
            overview.mouseReleased(event);
            check(overview.headquarters().frame().x() > left || overview.headquarters().frame().x(CommanderNavalHeadquartersWindow.WIDTH) >= client.getWindow().getGuiScaledWidth() - 1, "The headquarters move with the mouse");
            overview.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            check(overview.headquarters() == null && overview.equipment() != null, "Escape closes the headquarters first");
            press(overview, overview.frame().x(CommanderOverview.headquartersBox().x() + 18), overview.frame().y(CommanderOverview.headquartersBox().y() + 18));
            var next = overview.headquarters().frame();
            press(overview, next.x(CommanderNavalHeadquartersWindow.WIDTH - 27), next.y(26));
            check(overview.headquarters() == null, "The close button closes the headquarters");
        });
    }
    private static FieldEquipment reply(FieldEquipment source, String token) { return new FieldEquipment(token, source.target(), source.totals(), source.rows(), source.labels(), source.error()); }
    private static void away(ClientGameTestContext game) {
        game.getInput().setCursorPos(game.computeOnClient(c -> c.getWindow().getWidth()) - 4, game.computeOnClient(c -> c.getWindow().getHeight()) - 4);
        game.waitTicks(3);
    }
    private static void hover(ClientGameTestContext game, CommanderOverview overview, double x, double y, boolean header) {
        double[] point = game.computeOnClient(c -> { var frame = header ? overview.frame() : overview.equipment().frame(); return new double[]{frame.x(x) * c.getWindow().getGuiScale(), frame.y(y) * c.getWindow().getGuiScale()}; });
        game.getInput().setCursorPos(point[0], point[1]);
        game.waitTicks(4);
    }
    private static void press(CommanderOverview screen, double x, double y) {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        check(screen.mouseClicked(event, false), "The click is consumed");
        screen.mouseReleased(event);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
