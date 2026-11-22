package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.network.NetworkScreens;
import dev.hoi.client.screen.commander.CommanderConfirmation;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.protocol.commander.CommanderDivisionPayloads.Officer;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.commander.CommanderHudPayloads.Mode;
import dev.hoi.protocol.commander.CommanderHudPayloads.Open;
import dev.hoi.protocol.commander.CommanderOrderPayloads;
import dev.hoi.protocol.commander.CommanderShipPayloads.*;
import dev.hoi.protocol.dialog.DialogPayloads;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.menu.MenuView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class CommanderShipWindowTest {
    private static final double[] LINES = {101, 120, 140, 160, 180, 200, 219, 239, 257};
    private CommanderShipWindowTest() {}
    static void run(ClientGameTestContext game, MenuView menu, String directory) throws IOException {
        for (int row = 0; row < CommanderShipWindow.ROWS.length; row++)
            check(Math.abs(CommanderShipWindow.ROWS[row] - (LINES[row] + LINES[row + 1]) / 2) <= .5, "Row " + row + " is centred between its dotted lines");
        var source = new CommanderHudPayloads.Snapshot(Files.readString(Path.of(directory, "kor-command-panel-navy.json"))).view();
        var force = new ForceSnapshot(Files.readString(Path.of(directory, "kor-ship-force.json"))).force();
        var fixture = new Snapshot(Files.readString(Path.of(directory, "kor-ship.json"))).view();
        var submarine = new Snapshot(Files.readString(Path.of(directory, "kor-ship-submarine.json"))).view();
        var fleet = source.cards().stream().filter(c -> c.panel() != null && c.panel().members().stream().anyMatch(m -> m.id().equals(force.force()))).findFirst().orElseThrow();
        for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
            var requests = new ArrayList<Request>();
            var opens = new ArrayList<Open>();
            var designs = new ArrayList<String>();
            game.getInput().resizeWindow(size[0], size[1]);
            game.runOnClient(client -> {
                client.options.guiScale().set(2);
                ClientCampaignHud.accept(HudPayloads.State.of("KOR", menu.hud()));
                CommanderHud.reset(); CommanderUnitHud.reset();
                CommanderShipHud.requestObserver = requests::add;
                CommanderShipWindow.designer = designs::add;
                CommanderHud.requestObserver = opens::add;
                CommanderOrders.selectionObserver = ignored -> {};
                CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(), 1, source.country(), client.level.dimension().identifier().toString(), ""));
                CommanderHud.accept(new CommanderHudPayloads.View(source.country(), client.level.dimension().identifier().toString(), source.branch(), source.cards(), source.wings(), true));
                CommanderPanels.select(fleet.id(), false);
            });
            game.setScreen(MapCursor::new);
            game.getInput().setCursorPos(size[0] - 30, 90);
            game.waitTicks(3);
            game.runOnClient(client -> {
                check(requests.isEmpty() && rows().isEmpty(), "A fleet selection alone requests no ships");
                click(client.gui.screen(), control("naval_force_select/" + force.force()));
            });
            game.waitTicks(3);
            game.runOnClient(client -> {
                check(requests.size() == 1 && requests.getFirst().action() == Action.LIST && requests.getFirst().target().equals(force.force()), "Selecting one task force requests its ships: " + requests);
                CommanderShipRows.accept(new Force(requests.getFirst().token(), force.force(), force.ships(), force.labels(), ""));
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-ship-list-" + size[0] + "x" + size[1] + "-gui2");
            var seen = new HashSet<String>();
            for (int page = 0; page < force.ships().size(); page++) {
                game.runOnClient(client -> {
                    rows().forEach(row -> seen.add(row.icon()));
                    var row = rows().getFirst();
                    client.gui.screen().mouseScrolled(row.x() + 60, row.y() + 5, 0, -1);
                });
                game.waitTicks(2);
            }
            game.runOnClient(client -> { var row = rows().getFirst(); client.gui.screen().mouseScrolled(row.x() + 60, row.y() + 5, 0, 1); });
            for (int page = 0; page < force.ships().size(); page++) {
                game.runOnClient(client -> { var row = rows().getFirst(); client.gui.screen().mouseScrolled(row.x() + 60, row.y() + 5, 0, 1); });
                game.waitTicks(1);
            }
            game.waitTicks(2);
            game.runOnClient(client -> {
                check(seen.size() == force.ships().size() && (size[1] < 1440 || rows().size() == force.ships().size()), "Every ship of the task force is reachable: " + seen);
                check(fleet.panel().members().stream().filter(m -> !m.id().equals(force.force())).noneMatch(m -> CommanderPanels.hits().stream().anyMatch(h -> h.icon().equals("naval_force_select/" + m.id()))), "Only the selected task force stays in the list");
                for (var ship : force.ships()) {
                    if (rows().stream().noneMatch(row -> row.icon().equals("ship_row/" + ship.id()))) continue;
                    var details = control("ship_details/" + ship.id()); var row = control("ship_row/" + ship.id());
                    check(details.tip().equals(force.label("CLICK_NAVY_SEE_DETAILS")) && control("ship_organization/" + ship.id()).tip().equals(ship.organizationTooltip()) && control("ship_strength/" + ship.id()).tip().equals(ship.strengthTooltip()), "Ship row tooltips are the original ones");
                    check(details.x() >= row.x() && details.x() + details.w() <= row.x() + row.w(), "The ship picture stays inside its row");
                }
                for (int i = 1; i < rows().size(); i++) check(rows().get(i).y() >= rows().get(i - 1).y() + rows().get(i - 1).h() - 1, "Ship rows do not overlap");
                click(client.gui.screen(), control("ship_details/" + fixture.ship()));
                check(CommanderShipHud.window() != null && CommanderShipHud.window().ship().equals(fixture.ship()) && requests.getLast().action() == Action.OPEN && requests.getLast().target().equals(fixture.ship()), "The ship picture opens its details");
                CommanderShipHud.accept(view(fixture, requests.getLast().token(), fixture.officer()));
                check(CommanderShipHud.window().view() != null && client.gui.screen() instanceof MapCursor, "The details are drawn above the map without another screen");
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-ship-window-" + size[0] + "x" + size[1] + "-gui2");
            game.runOnClient(client -> {
                var window = CommanderShipHud.window(); var frame = window.frame();
                check(frame.x(0) >= 0 && frame.y(0) >= 0 && frame.x(CommanderShipWindow.WIDTH) <= client.getWindow().getGuiScaledWidth() && frame.y(CommanderShipWindow.HEIGHT) <= client.getWindow().getGuiScaledHeight(), "The details stay on the screen");
                for (int c = 0; c < 3; c++) {
                    var stats = fixture.columns().get(c).stats();
                    for (int r = 0; r < stats.size(); r++) {
                        String shown = window.tooltip(frame.x(CommanderShipWindow.COLUMN_X[c] + 60), frame.y(CommanderShipWindow.ROWS[r]));
                        check(shown.equals(IndustryEquipmentDesigns.statTooltip(stats.get(r))) && shown.startsWith("§e") && shown.contains("\n"), stats.get(r).id() + " shows its original description: " + shown);
                    }
                }
                check(window.tooltip(frame.x(367 + 60), frame.y(CommanderShipWindow.ROWS[fixture.columns().get(2).stats().size() - 1])).contains("%"), "The hit profile lists its chances");
                for (var element : List.of(Map.entry("delete", new double[]{575, 18}), Map.entry("manpower", new double[]{60, 377}), Map.entry("experience", new double[]{450, 52})))
                    check(window.tooltip(frame.x(element.getValue()[0]), frame.y(element.getValue()[1])).equals(fixture.tooltip(element.getKey())), "The " + element.getKey() + " element shows its original tooltip");
                check(window.tooltip(frame.x(780), frame.y(17)).startsWith("§e") && window.tooltip(frame.x(837), frame.y(18)).equals(fixture.label("CLOSE")) && window.tooltip(frame.x(785), frame.y(48)).equals(fixture.officer().promotion()), "Fuel, close and promotion show their tooltips");
                check(window.tooltip(frame.x(270), frame.y(340)).startsWith("§e" + fixture.designName()), "The ship picture names its class");
                opens.clear();
                press(client.gui.screen(), frame.x(575), frame.y(18));
                check(opens.equals(List.of(new Open(Mode.NAVAL_ORGANIZE, "DELETE_SHIP", List.of("ship/" + fixture.ship())))), "Delete asks for its confirmation: " + opens);
                opens.clear();
                var officer = fixture.officer();
                CommanderShipHud.accept(view(fixture, requests.getLast().token(), new Officer(officer.title(), officer.name(), officer.portrait(), true, officer.promotion())));
                press(client.gui.screen(), frame.x(785), frame.y(48));
                check(opens.equals(List.of(new Open(Mode.SHIP_CAPTAINS, "", List.of("unit", "promote/" + fixture.ship())))), "Promotion asks for the officer confirmation: " + opens);
            });
            var confirm = new DialogPayloads.Show(Files.readString(Path.of(directory, "kor-ship-captain-promote.json")), true).view().issued(UUID.randomUUID().toString(), 7);
            game.runOnClient(client -> {
                var cursor = client.gui.screen();
                DialogCoordinator.receive(DialogPayloads.Show.of(confirm, true));
                check(client.gui.screen() instanceof CommanderConfirmation modal && modal.backdrop() == cursor && modal.restored() == cursor, "Promotion uses the field officer confirmation above the map");
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-ship-promote-" + size[0] + "x" + size[1] + "-gui2");
            game.runOnClient(client -> {
                DialogCoordinator.close(confirm.token());
                check(client.gui.screen() instanceof MapCursor && CommanderShipHud.window() != null && CommanderShipHud.window().view() != null, "Closing the confirmation returns to the map and the same details");
                check(requests.getLast().action() == Action.OPEN && requests.getLast().target().equals(fixture.ship()), "The details are requested again after the confirmation");
                var frame = CommanderShipHud.window().frame();
                press(client.gui.screen(), frame.x(CommanderShipWindow.TAB_X[1] + 56), frame.y(415));
                check(CommanderShipHud.window().tab() == 1, "The history tab is clickable");
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-ship-history-" + size[0] + "x" + size[1] + "-gui2");
            game.runOnClient(client -> {
                var window = CommanderShipHud.window(); var frame = window.frame();
                press(client.gui.screen(), frame.x(CommanderShipWindow.TAB_X[0] + 56), frame.y(415));
                check(window.tab() == 0, "The status tab is clickable");
                double before = frame.x(0), top = frame.y(0);
                var down = mouse(frame.x(700), frame.y(250));
                client.gui.screen().mouseClicked(down, false);
                client.gui.screen().mouseDragged(new MouseButtonEvent(down.x() + 20, down.y() + 15, down.buttonInfo()), 20, 15);
                client.gui.screen().mouseReleased(down);
                var moved = CommanderShipHud.window().frame();
                check(moved.x(0) >= before && moved.y(0) >= top && (moved.x(0) - before > 1 || moved.y(0) - top > 1), "Dragging moves the details");
                check(CommanderShipHud.covers(moved.x(300), moved.y(200)) && client.gui.screen() instanceof MapCursor cursor && cursor.covers(moved.x(300), moved.y(200)), "The details cover the map below them");
                press(client.gui.screen(), moved.x(CommanderShipWindow.TAB_X[2] + 56), moved.y(415));
                check(designs.equals(List.of(fixture.design())), "The design tab opens the designer of this class: " + designs);
            });
            game.runOnClient(client -> {
                int sent = requests.size();
                CommanderShipHud.open(submarine.ship());
                check(requests.size() == sent + 2 && requests.get(sent).action() == Action.CLOSE && requests.getLast().action() == Action.OPEN, "Opening another ship replaces the details");
                CommanderShipHud.accept(view(submarine, requests.getLast().token(), submarine.officer()));
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-ship-submarine-" + size[0] + "x" + size[1] + "-gui2");
            game.runOnClient(client -> {
                client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                check(CommanderShipHud.window() == null && client.gui.screen() instanceof MapCursor && requests.getLast().action() == Action.CLOSE && !CommanderPanels.hits().isEmpty(), "Escape closes the details before the selection");
                var back = control("naval_force_select/" + force.force());
                click(client.gui.screen(), back);
            });
            game.waitTicks(3);
            game.runOnClient(client -> {
                check(rows().isEmpty() && requests.getLast().action() == Action.CLOSE_LIST && CommanderPanels.commandSelection().size() == fleet.panel().members().size() && CommanderPanels.hits().stream().anyMatch(h -> h.icon().startsWith("naval_force_select/")), "Back returns to every task force of the fleet: " + requests.getLast() + CommanderPanels.commandSelection());
                CommanderShipHud.requestObserver = null; CommanderShipWindow.designer = NetworkScreens::openShipDesigner; CommanderHud.requestObserver = null; CommanderOrders.selectionObserver = null;
                CommanderHud.reset(); CommanderUnitHud.reset();
            });
        }
    }
    private static View view(View source, String token, Officer officer) {
        return new View(token, source.ship(), source.country(), source.name(), source.type(), source.design(), source.designName(), source.designer(), source.image(), source.level(), source.manpower(), source.fuel(), source.scrap(), officer, source.columns(), source.wings(), source.wingTotal(), source.tooltips(), source.labels(), source.error());
    }
    private static List<CommanderPanels.Hit> rows() { return CommanderPanels.hits().stream().filter(h -> h.icon().startsWith("ship_row/")).toList(); }
    private static CommanderPanels.Hit control(String id) { return CommanderPanels.hits().stream().filter(h -> h.icon().equals(id)).findFirst().orElseThrow(() -> new AssertionError("Missing control: " + id)); }
    private static MouseButtonEvent mouse(double x, double y) { return new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)); }
    private static void click(net.minecraft.client.gui.screens.Screen screen, CommanderPanels.Hit hit) { press(screen, hit.x() + hit.w() / 2., hit.y() + hit.h() / 2.); }
    private static void press(net.minecraft.client.gui.screens.Screen screen, double x, double y) {
        var event = mouse(x, y);
        check(screen.mouseClicked(event, false), "The click is consumed");
        screen.mouseReleased(event);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
