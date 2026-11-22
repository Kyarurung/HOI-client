package dev.hoi.client.screen.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.map.commander.CommanderCombatLogHud;
import dev.hoi.protocol.commander.CommanderRecordPayloads.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class CommanderNavalLossWindowTest {
    private CommanderNavalLossWindowTest() {}
    static void run(ClientGameTestContext game, CommanderOverview overview, String directory, String button, String suffix) throws IOException {
        var fixture = new LossesSnapshot(Files.readString(Path.of(directory, "kor-naval-losses.json"))).losses();
        var ships = new LossesSnapshot(Files.readString(Path.of(directory, "kor-naval-losses-ships.json"))).losses();
        var requests = new ArrayList<Request>();
        try {
            game.runOnClient(client -> {
                CommanderCombatLogHud.requestObserver = requests::add;
                check(overview.losses() == null, "The overview opens without the losses");
                var box = CommanderOverview.lossesBox();
                press(overview, overview.frame().x(box.x() + box.w() / 2), overview.frame().y(box.y() + box.h() / 2));
                check(overview.losses() != null && requests.size() == 1 && requests.getFirst().action() == Action.NAVY && requests.getFirst().target().isEmpty() && requests.getFirst().filters().equals(List.of("", "")), "The header button requests the naval losses: " + requests);
                overview.accept(reply(fixture, requests.getFirst().token(), ""));
                check(overview.losses().losses() != null, "The window accepts its own snapshot");
            });
            game.getInput().setCursorPos(game.computeOnClient(c -> c.getWindow().getWidth()) - 4, game.computeOnClient(c -> c.getWindow().getHeight()) - 4);
            game.waitTicks(3);
            game.takeScreenshot("hoi-naval-losses" + suffix);
            if (!game.computeOnClient(c -> overview.covers(overview.frame().x(CommanderOverview.lossesBox().x() + 18), overview.frame().y(CommanderOverview.lossesBox().y() + 18)))) {
                hover(game, overview, CommanderOverview.lossesBox().x() + 18, CommanderOverview.lossesBox().y() + 18, true);
                check(game.computeOnClient(c -> overview.hoveredTooltip()).equals(button), "The header button shows its original tooltip");
                game.takeScreenshot("hoi-naval-losses-button" + suffix);
            }
            game.runOnClient(client -> {
                var window = overview.losses(); var frame = window.frame();
                check(frame.x(0) >= overview.frame().x(CommanderNavalLossWindow.X) - .5 || frame.x(CommanderNavalLossWindow.WIDTH) <= client.getWindow().getGuiScaledWidth() + .5, "The losses open beside the overview");
                check(frame.x(CommanderNavalLossWindow.WIDTH) <= client.getWindow().getGuiScaledWidth() + .5 && window.bottom() <= client.getWindow().getGuiScaledHeight() + .5 && overview.covers(frame.x(200), frame.y(200)), "The losses stay on the screen and cover the map");
                check(window.source(frame.x(60), frame.y(70)).equals(fixture.label("TOTAL_SHIPS_LOST")) && window.source(frame.x(180), frame.y(70)).equals(fixture.label("TOTAL_SHIPS_SUNK")) && window.source(frame.x(467), frame.y(25)).equals(fixture.label("CLOSE")), "The totals and the close button show their original tooltips");
                check(window.source(frame.x(100), frame.y(108)).equals(fixture.label("SHIPS_LOST_FILTER_COUNTRY")) && window.source(frame.x(350), frame.y(108)).equals(fixture.label("SHIPS_LOST_FILTER_FACTION")), "The filters show their original tooltips");
                double first = 150 + 24 + 15;
                check(window.source(frame.x(200), frame.y(first)).equals(fixture.label("OUR_LOSSES")) && window.source(frame.x(255), frame.y(first)).equals(fixture.label("OUR_KILLS")), "The battle counters show their original tooltips");
                press(overview, frame.x(80), frame.y(first));
                check(window.expanded().contains(fixture.battles().getFirst().id()), "A battle opens its ship classes");
                check(window.source(frame.x(120), frame.y(150 + 24 + 30 + 30)).equals(fixture.battles().getFirst().hulls().getFirst().tooltip()), "A ship class shows its original tooltip");
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-naval-losses-battle" + suffix);
            hover(game, overview, 120, 150 + 24 + 30 + 30, false);
            game.waitTicks(25);
            game.takeScreenshot("hoi-naval-losses-hull-tooltip" + suffix);
            game.runOnClient(client -> {
                var window = overview.losses(); var frame = window.frame();
                for (var section : fixture.periods()) window.expand(section.id());
                overview.mouseScrolled(frame.x(200), frame.y(180), 0, -100);
                var last = fixture.periods().getLast().accidents().getLast();
                check(window.source(frame.x(120), frame.y(window.listBottom() - 30)).equals(last.tooltip()), "The wheel reaches the accidents and their original tooltip: " + window.source(frame.x(120), frame.y(window.listBottom() - 30)));
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-naval-losses-periods" + suffix);
            game.runOnClient(client -> {
                var window = overview.losses(); var frame = window.frame();
                overview.mouseScrolled(frame.x(200), frame.y(180), 0, 100);
                String target = ships.target(), hull = target.substring(target.lastIndexOf('/') + 1);
                int index = fixture.battles().getFirst().hulls().stream().map(Hull::key).toList().indexOf(hull);
                check(index >= 0 && target.startsWith(fixture.battles().getFirst().id() + "/"), "The ship fixture belongs to the first battle");
                double y = 150 + 24 + 30 + index * 69 + 34;
                overview.mouseScrolled(frame.x(200), frame.y(180), 0, -Math.max(0, (y - window.listBottom() + 40) / 40));
                press(overview, frame.x(120), frame.y(y - window.scroll()));
                check(requests.getLast().action() == Action.NAVY && requests.getLast().target().equals(target), "A ship class asks for its ships: " + requests.getLast());
                overview.accept(reply(fixture, requests.getLast().token(), ""));
                check(window.losses().ships().isEmpty(), "A stale answer does not replace the ship list");
                overview.accept(reply(ships, requests.getLast().token(), target));
                check(window.source(frame.x(200), frame.y(150 + 45)).equals(ships.ships().getFirst().tooltip()) && window.source(frame.x(396), frame.y(70)).equals(ships.label("BACK")), "The ship list shows its original tooltips");
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-naval-losses-ships" + suffix);
            hover(game, overview, 200, 150 + 45, false);
            game.waitTicks(25);
            game.takeScreenshot("hoi-naval-losses-ship-tooltip" + suffix);
            game.runOnClient(client -> {
                var window = overview.losses(); var frame = window.frame();
                press(overview, frame.x(396), frame.y(70));
                check(requests.getLast().target().isEmpty(), "Back returns to the battles");
                overview.accept(reply(fixture, requests.getLast().token(), ""));
                press(overview, frame.x(100), frame.y(108));
                check(window.choices().size() == fixture.countries().size() + 1 && window.choices().getFirst().name().equals(fixture.label("ALL_COUNTRIES")), "The country filter lists every opponent");
            });
            game.waitTicks(3);
            game.takeScreenshot("hoi-naval-losses-countries" + suffix);
            game.runOnClient(client -> {
                var window = overview.losses(); var frame = window.frame();
                press(overview, frame.x(80), frame.y(115 + 7 + 40 + 20));
                check(requests.getLast().filters().equals(List.of(fixture.countries().getFirst().id(), "")), "A country narrows the losses: " + requests.getLast());
                overview.accept(reply(fixture, requests.getLast().token(), ""));
                press(overview, frame.x(350), frame.y(108));
                check(window.choices().getFirst().name().equals(fixture.label("ALL_FACTIONS")), "The faction filter lists the factions");
                overview.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                check(overview.losses() == window, "Escape closes the list before the window");
                overview.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                check(overview.losses() == null && requests.getLast().action() == Action.CLOSE_NAVY, "Escape closes the losses before the overview");
                var box = CommanderOverview.lossesBox();
                press(overview, overview.frame().x(box.x() + box.w() / 2), overview.frame().y(box.y() + box.h() / 2));
                overview.accept(reply(fixture, requests.getLast().token(), ""));
                var next = overview.losses();
                press(overview, next.frame().x(467), next.frame().y(25));
                check(overview.losses() == null && requests.getLast().action() == Action.CLOSE_NAVY, "The close button closes the losses");
            });
        } finally { game.runOnClient(client -> CommanderCombatLogHud.requestObserver = null); }
    }
    private static Losses reply(Losses source, String token, String target) {
        return new Losses(token, target, source.viewer(), source.lost(), source.sunk(), source.country(), source.faction(), source.countries(), source.factions(), source.battles(), source.periods(), target.isEmpty() ? List.of() : source.ships(), source.labels(), "");
    }
    private static void hover(ClientGameTestContext game, CommanderOverview overview, double x, double y, boolean header) {
        double[] point = game.computeOnClient(c -> { var frame = header ? overview.frame() : overview.losses().frame(); return new double[]{frame.x(x) * c.getWindow().getGuiScale(), frame.y(y) * c.getWindow().getGuiScale()}; });
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
