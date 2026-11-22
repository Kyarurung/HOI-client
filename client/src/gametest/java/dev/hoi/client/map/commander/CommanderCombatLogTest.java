package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.network.NetworkScreens;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.commander.CommanderOrderPayloads;
import dev.hoi.protocol.commander.CommanderRecordPayloads;
import dev.hoi.protocol.commander.CommanderRecordPayloads.*;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.menu.MenuView;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class CommanderCombatLogTest {
    private CommanderCombatLogTest() {}
    public static void run(ClientGameTestContext game, MenuView menu) {
        String directory = Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW"), "Authoritative commander fixtures are required for the combat log");
        CommanderHudPayloads.View source; Log fixture;
        try {
            source = new CommanderHudPayloads.Snapshot(Files.readString(Path.of(directory, "kor-command-panel-army-two-theaters.json"))).view();
            fixture = new LogSnapshot(Files.readString(Path.of(directory, "kor-combat-log.json"))).log();
        } catch (IOException error) { throw new UncheckedIOException(error); }
        check(CommanderTheaters.combatColor(0, 0) == 0xFF9C9C9C && CommanderTheaters.combatColor(3, 0) == 0xFF00B700 && CommanderTheaters.combatColor(4, 2) == 0xFFB77D00 && CommanderTheaters.combatColor(2, 2) == 0xFFB70000, "The theatre battle icons use the original combat colours");
        var theater = source.theaters().stream().filter(t -> t.id().equals(fixture.theater())).findFirst().orElseThrow();
        for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
            var requests = new ArrayList<Request>();
            var designs = new ArrayList<String>();
            var countries = new ArrayList<String>();
            String suffix = "-" + size[0] + "x" + size[1] + "-gui2";
            game.getInput().resizeWindow(size[0], size[1]);
            game.runOnClient(client -> {
                client.options.guiScale().set(2);
                ClientCampaignHud.accept(HudPayloads.State.of("KOR", menu.hud()));
                CommanderHud.reset(); CommanderUnitHud.reset();
                CommanderCombatLogHud.requestObserver = requests::add;
                CommanderCombatLogWindow.designer = designs::add;
                CommanderCombatLogWindow.country = countries::add;
                CommanderOrders.selectionObserver = ignored -> {};
                CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(), 1, source.country(), client.level.dimension().identifier().toString(), ""));
                CommanderHud.accept(new CommanderHudPayloads.View(source.country(), client.level.dimension().identifier().toString(), source.branch(), source.cards(), source.wings(), true, source.theaters()));
            });
            game.setScreen(MapCursor::new);
            game.getInput().setCursorPos(size[0] - 30, size[1] - 10);
            game.waitTicks(3);
            game.runOnClient(client -> {
                var logs = CommanderPanels.hits().stream().filter(h -> h.icon().equals("theater_log")).toList();
                check(logs.size() == source.theaters().size() && logs.stream().allMatch(h -> h.tip().isEmpty()), "Every theatre has its combat log button without a tooltip: " + logs.size());
                press(client.gui.screen(), logs.getFirst().x() + logs.getFirst().w() / 2., logs.getFirst().y() + logs.getFirst().h() / 2.);
                check(CommanderCombatLogHud.window() != null && CommanderCombatLogHud.window().theater().equals(theater.id()) && client.gui.screen() instanceof MapCursor, "The log opens above the map without another screen");
                check(requests.size() == 1 && requests.getFirst().action() == Action.LOG && requests.getFirst().target().equals(theater.id()) && requests.getFirst().months() == 0 && requests.getFirst().filters().isEmpty(), "Opening the log requests the theatre: " + requests);
                CommanderCombatLogHud.accept(reply(fixture, requests.getFirst().token(), fixture.battles(), 0, fixture.total(), fixture.months()));
                check(window().log() != null, "The window accepts its own snapshot");
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-combat-log" + suffix);
            game.runOnClient(client -> {
                var frame = window().frame();
                int width = client.getWindow().getGuiScaledWidth(), height = client.getWindow().getGuiScaledHeight();
                check(window().left() >= -.5 && window().right() <= width + .5 && frame.y(0) >= dev.hoi.client.ui.hud.HudMenuBar.height(width) - .5 && window().bottom() <= height + .5, "The log stays on the screen below the menu");
                check(CommanderCombatLogHud.covers(frame.x(300), frame.y(200)) && ((MapCursor)client.gui.screen()).covers(frame.x(300), frame.y(200)), "The log covers the map below it");
                var tips = List.of(
                        Map.entry(fixture.label("CLOSE"), new double[]{CommanderCombatLogWindow.INNER - 19, 29}), Map.entry(fixture.manpowerTooltip(), new double[]{120, 118}), Map.entry(fixture.label("RATIO_TOOLTIP"), new double[]{480, 118}),
                        Map.entry(fixture.battles().getFirst().iconTooltip(), new double[]{10 + 118, 175 + 16}), Map.entry(fixture.battles().getFirst().locationTooltip(), new double[]{10 + 200, 175 + 16}), Map.entry(fixture.battles().getFirst().dateTooltip(), new double[]{10 + 450, 175 + 16}),
                        Map.entry(fixture.battles().getFirst().imageTooltip(), new double[]{10 + 300, 175 + 65}), Map.entry(fixture.battles().getFirst().own().countries(), new double[]{10 + 32, 175 + 19}), Map.entry(fixture.battles().getFirst().enemy().countries(), new double[]{10 + 582, 175 + 19}),
                        Map.entry(fixture.battles().getFirst().enemy().leader(), new double[]{10 + 580, 175 + 68}));
                for (var tip : tips) check(tip.getKey().equals(window().tooltip(frame.x(tip.getValue()[0]), frame.y(tip.getValue()[1]))), "The original tooltip is shown at " + Arrays.toString(tip.getValue()) + ": " + window().tooltip(frame.x(tip.getValue()[0]), frame.y(tip.getValue()[1])));
                press(client.gui.screen(), frame.x(10 + 200), frame.y(175 + 16));
                check(requests.getLast().action() == Action.FOCUS && requests.getLast().target().equals(fixture.battles().getFirst().province()), "The battle place centres the map: " + requests.getLast());
                press(client.gui.screen(), frame.x(10 + 32), frame.y(175 + 19)); press(client.gui.screen(), frame.x(10 + 582), frame.y(175 + 19));
                check(countries.equals(List.of(fixture.battles().getFirst().own().country(), fixture.battles().getFirst().enemy().country())), "The flags open diplomacy: " + countries);
            });
            hover(game, 10 + 300, 175 + 65);
            game.takeScreenshot("hoi-combat-log-terrain-tooltip" + suffix);
            hover(game, 10 + 580, 175 + 68);
            game.takeScreenshot("hoi-combat-log-leader-tooltip" + suffix);
            game.runOnClient(client -> {
                var frame = window().frame(); int shown = window().visibleRows();
                client.gui.screen().mouseScrolled(frame.x(300), frame.y(300), 0, -100);
                check(window().firstRow() == Math.max(0, fixture.total() - shown), "The wheel reaches the last battle: " + window().firstRow());
                client.gui.screen().mouseScrolled(frame.x(300), frame.y(300), 0, 100);
                check(window().firstRow() == 0, "The wheel returns to the newest battle");
            });
            paging(game, requests, fixture);
            slider(game, requests, fixture);
            armies(game, requests, fixture, theater);
            equipment(game, fixture, suffix);
            templates(game, designs, fixture, suffix);
            game.runOnClient(client -> {
                var window = window(); var frame = window.frame();
                double left = frame.x(0), top = frame.y(0);
                var down = mouse(frame.x(40), frame.y(20));
                client.gui.screen().mouseClicked(down, false);
                client.gui.screen().mouseDragged(new MouseButtonEvent(down.x() - 30, down.y() + 5, down.buttonInfo()), -30, 5);
                client.gui.screen().mouseReleased(down);
                var moved = window().frame();
                check(Math.abs(moved.x(0) - Math.max(0, left - 30)) < 1 && moved.y(0) >= top - .5, "Dragging moves the log: " + left + " -> " + moved.x(0));
                int sent = requests.size();
                CommanderCombatLogHud.open(theater.id());
                check(CommanderCombatLogHud.window() == null && requests.size() == sent + 1 && requests.getLast().action() == Action.CLOSE_LOG, "Pressing the same theatre again closes the log");
                CommanderCombatLogHud.open(source.theaters().getLast().id());
                check(window().theater().equals(source.theaters().getLast().id()) && requests.getLast().action() == Action.LOG, "The other theatre opens its own log");
                client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                check(CommanderCombatLogHud.window() == null && client.gui.screen() instanceof MapCursor && requests.getLast().action() == Action.CLOSE_LOG, "Escape closes the log first");
            });
            empty(game, requests, fixture, theater.id(), suffix);
            game.runOnClient(client -> {
                CommanderCombatLogHud.requestObserver = null; CommanderCombatLogWindow.designer = NetworkScreens::openDesigner; CommanderCombatLogWindow.country = HoiClient::openCountry; CommanderOrders.selectionObserver = null;
                CommanderHud.reset(); CommanderUnitHud.reset();
                check(CommanderCombatLogHud.window() == null, "Resetting the command HUD closes the log");
            });
        }
        game.setScreen(() -> null);
    }
    private static void paging(ClientGameTestContext game, List<Request> requests, Log fixture) {
        var many = new ArrayList<Battle>();
        for (int i = 0; i < CommanderRecordPayloads.ROWS + 12; i++) { var b = fixture.battles().get(i % fixture.battles().size()); many.add(new Battle("page_" + i, b.province(), b.location(), b.locationTooltip(), b.date(), b.dateTooltip(), b.icon(), b.iconTooltip(), b.image(), b.imageTooltip(), b.own(), b.enemy())); }
        game.runOnClient(client -> {
            CommanderCombatLogHud.accept(reply(fixture, requests.getFirst().token(), many.subList(0, CommanderRecordPayloads.ROWS), 0, many.size(), fixture.months()));
            requests.clear();
            client.gui.screen().mouseScrolled(window().frame().x(300), window().frame().y(300), 0, -100);
        });
        game.waitFor(client -> !requests.isEmpty());
        game.runOnClient(client -> {
            var request = requests.getLast();
            check(request.action() == Action.LOG && request.offset() > 0 && request.offset() <= window().firstRow(), "Scrolling past the loaded battles requests the next page: " + request);
            CommanderCombatLogHud.accept(reply(fixture, request.token(), many.subList(request.offset(), Math.min(many.size(), request.offset() + CommanderRecordPayloads.ROWS)), request.offset(), many.size(), fixture.months()));
        });
        game.waitTicks(3);
        game.runOnClient(client -> {
            var frame = window().frame(); int count = requests.size(); var first = window().firstRow();
            press(client.gui.screen(), frame.x(10 + 200), frame.y(175 + 16));
            check(requests.size() == count + 1 && requests.getLast().action() == Action.FOCUS && requests.getLast().target().equals(many.get(first).province()), "The loaded page is drawn and clickable");
            CommanderCombatLogHud.accept(reply(fixture, requests.getLast().token(), fixture.battles(), 0, fixture.total(), fixture.months()));
            check(window().firstRow() <= Math.max(0, fixture.total() - window().visibleRows()), "A shorter log clamps the list");
        });
    }
    private static void slider(ClientGameTestContext game, List<Request> requests, Log fixture) {
        game.runOnClient(client -> { requests.clear(); press(client.gui.screen(), window().frame().x(259), window().frame().y(84)); check(requests.isEmpty(), "The slider stops at its maximum"); press(client.gui.screen(), window().frame().x(81), window().frame().y(84)); });
        game.waitFor(client -> !requests.isEmpty());
        game.runOnClient(client -> {
            check(requests.getLast().action() == Action.LOG && requests.getLast().months() == fixture.months() - 1, "The left arrow removes one month: " + requests);
            CommanderCombatLogHud.accept(reply(fixture, requests.getLast().token(), fixture.battles(), 0, fixture.total(), fixture.months() - 1));
            requests.clear();
            var frame = window().frame(); var down = mouse(frame.x(98), frame.y(84));
            client.gui.screen().mouseClicked(down, false);
            client.gui.screen().mouseDragged(new MouseButtonEvent(frame.x(60), frame.y(84), down.buttonInfo()), -38, 0);
            client.gui.screen().mouseReleased(down);
        });
        game.waitFor(client -> !requests.isEmpty() && requests.getLast().months() == 1);
        game.runOnClient(client -> { CommanderCombatLogHud.accept(reply(fixture, requests.getLast().token(), fixture.battles(), 0, fixture.total(), 1)); requests.clear(); });
    }
    private static void armies(ClientGameTestContext game, List<Request> requests, Log fixture, CommanderHudPayloads.Theater theater) {
        double x = 340 + 16;
        for (boolean excluded : new boolean[]{true, false}) {
            game.waitTicks(3);
            game.runOnClient(client -> { requests.clear(); press(client.gui.screen(), window().frame().x(x), window().frame().y(76)); });
            game.waitFor(client -> !requests.isEmpty());
            game.runOnClient(client -> {
                check(requests.getLast().filters().equals(excluded ? List.of(theater.armies().getFirst()) : List.of()), "An army icon toggles that army: " + requests.getLast());
                CommanderCombatLogHud.accept(reply(fixture, requests.getLast().token(), fixture.battles(), 0, fixture.total(), 1));
            });
        }
        game.runOnClient(client -> check(window().armyCount() == theater.armies().size(), "Every army of the theatre has its icon: " + window().armyCount()));
    }
    private static void equipment(ClientGameTestContext game, Log fixture, String suffix) {
        game.runOnClient(client -> { press(client.gui.screen(), window().frame().x(CommanderCombatLogWindow.TAB_X[1] + 100), window().frame().y(152)); check(window().tab() == 1, "The equipment tab is clickable"); });
        game.getInput().setCursorPos(20, 400); game.waitTicks(3);
        game.takeScreenshot("hoi-combat-log-equipment" + suffix);
        game.runOnClient(client -> {
            var frame = window().frame(); var row = fixture.losses().getFirst();
            check(fixture.lost().combatTooltip().equals(window().tooltip(frame.x(350), frame.y(165 + 55))), "The total names the captured equipment");
            check(row.combatTooltip().equals(window().tooltip(frame.x(25 + 310), frame.y(165 + 75 + 20))) && row.attritionTooltip().equals(window().tooltip(frame.x(25 + 420), frame.y(165 + 75 + 20))) && row.recoveredTooltip().equals(window().tooltip(frame.x(25 + 530), frame.y(165 + 75 + 20))), "Each loss column shows its original tooltip");
            check(fixture.enemy().getFirst().combatTooltip().equals(window().tooltip(frame.x(25 + 310), frame.y(window().enemyTop() + 20))), "Enemy losses name what we captured");
        });
        hover(game, 25 + 530, 165 + 75 + 20);
        game.takeScreenshot("hoi-combat-log-recovery-tooltip" + suffix);
        game.runOnClient(client -> {
            var frame = window().frame();
            press(client.gui.screen(), frame.x(65 + 100), frame.y(165 + 55));
            check(fixture.detailed().getFirst().recoveredTooltip().equals(window().tooltip(frame.x(25 + 530), frame.y(165 + 75 + 20))), "The detailed list shows every model");
        });
        game.getInput().setCursorPos(20, 400); game.waitTicks(3);
        game.takeScreenshot("hoi-combat-log-equipment-detailed" + suffix);
        game.runOnClient(client -> {
            var frame = window().frame();
            client.gui.screen().mouseScrolled(frame.x(300), frame.y(300), 0, -100);
            check(fixture.detailed().getLast().attritionTooltip().equals(window().tooltip(frame.x(25 + 420), frame.y(window().enemyTop() - 30 - 20))), "The wheel reaches the last model");
            press(client.gui.screen(), frame.x(65 + 100), frame.y(165 + 55));
            check(fixture.losses().getFirst().recoveredTooltip().equals(window().tooltip(frame.x(25 + 530), frame.y(165 + 75 + 20))), "The button returns to the summary");
        });
    }
    private static void templates(ClientGameTestContext game, List<String> designs, Log fixture, String suffix) {
        game.runOnClient(client -> { press(client.gui.screen(), window().frame().x(CommanderCombatLogWindow.TAB_X[2] + 100), window().frame().y(152)); check(window().tab() == 2, "The template tab is clickable"); });
        game.getInput().setCursorPos(20, 400); game.waitTicks(3);
        game.takeScreenshot("hoi-combat-log-templates" + suffix);
        game.runOnClient(client -> {
            var frame = window().frame();
            check(fixture.templates().getFirst().tooltip().equals(window().tooltip(frame.x(25 + 370), frame.y(165 + 45 + 17))) && fixture.label("DEPLOYMENT_TEMPLATE_ITEM_EDIT").equals(window().tooltip(frame.x(25 + 24), frame.y(165 + 45 + 17))), "Templates show the original tooltips");
            press(client.gui.screen(), frame.x(25 + 24), frame.y(165 + 45 + 17));
            check(designs.equals(List.of(fixture.templates().getFirst().id())), "The counter opens the division designer: " + designs);
            press(client.gui.screen(), frame.x(CommanderCombatLogWindow.TAB_X[0] + 100), frame.y(152));
            check(window().tab() == 0, "The combat tab is clickable");
        });
    }
    private static void empty(ClientGameTestContext game, List<Request> requests, Log fixture, String theater, String suffix) {
        game.runOnClient(client -> {
            CommanderCombatLogHud.open(theater);
            CommanderCombatLogHud.accept(new Log(requests.getLast().token(), theater, 36, 36, Set.of(), fixture.period(), "§H인력 손실:§! 0", "", "승률: §G100%§!", 0, 0, List.of(), zero(), List.of(), List.of(), zero(), List.of(), List.of(), List.of(), fixture.labels(), ""));
        });
        for (int tab = 0; tab < 3; tab++) {
            int index = tab;
            game.runOnClient(client -> press(client.gui.screen(), window().frame().x(CommanderCombatLogWindow.TAB_X[index] + 100), window().frame().y(152)));
            game.waitTicks(3);
            game.takeScreenshot("hoi-combat-log-empty-" + tab + suffix);
        }
        game.runOnClient(client -> {
            press(client.gui.screen(), window().frame().x(CommanderCombatLogWindow.INNER - 19), window().frame().y(29));
            check(CommanderCombatLogHud.window() == null && requests.getLast().action() == Action.CLOSE_LOG, "The close button closes the log");
        });
    }
    private static Loss zero() { return new Loss("", "", "0", "", "0", "", "0", ""); }
    private static Log reply(Log source, String token, List<Battle> battles, int offset, int total, int months) {
        return new Log(token, source.theater(), months, source.maximum(), Set.of(), source.period(), source.manpower(), source.manpowerTooltip(), source.ratio(), offset, total, battles, source.lost(), source.losses(), source.detailed(), source.enemyLost(), source.enemy(), source.enemyDetailed(), source.templates(), source.labels(), "");
    }
    private static CommanderCombatLogWindow window() { return CommanderCombatLogHud.window(); }
    private static void hover(ClientGameTestContext game, double x, double y) {
        double[] point = game.computeOnClient(client -> new double[]{window().frame().x(x) * client.getWindow().getGuiScale(), window().frame().y(y) * client.getWindow().getGuiScale()});
        game.getInput().setCursorPos(point[0], point[1]);
        game.waitTicks(4);
    }
    private static MouseButtonEvent mouse(double x, double y) { return new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)); }
    private static void press(net.minecraft.client.gui.screens.Screen screen, double x, double y) {
        var event = mouse(x, y);
        check(screen.mouseClicked(event, false), "The click is consumed");
        screen.mouseReleased(event);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
