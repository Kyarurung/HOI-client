package dev.hoi.client.screen.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.hud.HudTimeControls;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.protocol.commander.CommanderDivisionPayloads.*;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.commander.CommanderOverviewPayloads;
import dev.hoi.protocol.dialog.DialogPayloads;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class CommanderDivisionWindowTest {
    private CommanderDivisionWindowTest() {}
    static void run(ClientGameTestContext game, CommanderOverview overview, String directory) throws IOException {
        var fixture = new Snapshot(Files.readString(Path.of(directory, "kor-division.json"))).view();
        var requests = new ArrayList<Request>();
        var opened = new ArrayList<CommanderHudPayloads.Open>();
        var hud = new CommanderHudPayloads.Snapshot(Files.readString(Path.of(directory, "kor-command-panel-army-two-theaters.json"))).view();
        try {
            observe(game, opened::add);
            game.runOnClient(c -> CommanderHud.accept(new CommanderHudPayloads.View(hud.country(), c.level.dimension().identifier().toString(), hud.branch(), hud.cards(), hud.wings(), hud.running(), hud.theaters(), hud.armyTheaters())));
            game.setScreen(MapCursor::new);
            var map = capture(game, "hoi-division-window-map-gui2");
            game.setScreen(() -> overview);
            var before = capture(game, "hoi-division-window-closed-gui2");
            int[][] bars = game.computeOnClient(c -> { int w = c.getWindow().getGuiScaledWidth(); return new int[][]{{0, 0, w, 18}, {0, 18, 304, 37}, {HudTimeControls.left(w), 18, w, 40}}; });
            for (int[] bar : bars) same(map, before, bar, .01, "The overview keeps the flag, the menu tabs and the clock of the map screen");
            game.runOnClient(c -> {
                CommanderDivisionWindow.requestObserver = requests::add;
                int rowY = CommanderOverview.LAYOUTS.get(overview.tab()).rowY();
                var selection = dev.hoi.client.map.commander.CommanderHud.overviewSelection(overview.tab());
                click(overview, overview.frame().x(13 + 1 + 38), overview.frame().y(rowY + 2 + 15));
                check(overview.details() != null && overview.details().division().equals(overview.rows().getFirst().id()), "The unit details button opens the division window");
                check(dev.hoi.client.map.commander.CommanderHud.overviewSelection(overview.tab()).equals(selection), "The unit details button does not change the selection");
                check(requests.size() == 1 && requests.getFirst().action() == Action.OPEN && requests.getFirst().division().equals(overview.details().division()), "Opening the window requests that division");
                overview.accept(view(fixture, requests.getFirst().token(), overview.details().division()));
                check(overview.details().view() != null, "The window accepts its own snapshot");
            });
            var open = capture(game, "hoi-division-window-open-gui2");
            for (int[] bar : bars) same(before, open, bar, .01, "Opening the unit details hides no top bar picture");
            int[] rows = game.computeOnClient(c -> new int[]{overview.frame().ix(13), overview.frame().iy(CommanderOverview.LAYOUTS.get(overview.tab()).rowY()), overview.frame().ix(13 + 120), c.getWindow().getGuiScaledHeight() - 30});
            int[] cards = game.computeOnClient(c -> new int[]{overview.unitHudLeft() + 5, c.getWindow().getGuiScaledHeight() - 80, c.getWindow().getGuiScaledWidth(), c.getWindow().getGuiScaledHeight()});
            same(before, open, rows, .01, "Opening the unit details hides no list picture");
            same(before, open, cards, .03, "Opening the unit details hides no commander card or theater");
            double[] research = game.computeOnClient(c -> { var tab = widget(overview, HudMenuBar.HudTabButton.class, "연구"); check(tab.active && tab.visible, "Menu tabs stay active below the unit details"); return new double[]{tab.getX(), tab.getY(), tab.getRight(), tab.getBottom()}; });
            game.getInput().setCursorPos(research[0] + research[2], research[1] + research[3]);
            game.waitTicks(15);
            game.runOnClient(c -> check(widget(overview, HudMenuBar.HudTabButton.class, "연구").isHovered(), "A menu tab reacts to the cursor while the unit details are open"));
            var hovered = ImageIO.read(game.takeScreenshot("hoi-division-window-tab-hover-gui2").toFile());
            check(changed(open, hovered, new int[]{(int)research[0], (int)research[1], (int)research[2], (int)research[3]}) > .05, "The hovered menu tab draws its original hover picture");
            check(changed(open, hovered, new int[]{(int)research[0], (int)research[3], (int)research[2] + 60, (int)research[3] + 20}) > .05, "The hovered menu tab shows its tooltip");
            game.runOnClient(c -> {
                var officer = fixture.officer();
                overview.accept(view(fixture, requests.getFirst().token(), overview.details().division(), new Officer(officer.title(), officer.name(), officer.portrait(), true, officer.promotion())));
                var frame = overview.details().frame();
                click(overview, frame.x(740 + 45), frame.y(34 + 15));
                check(opened.equals(List.of(new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.FIELD_OFFICERS, "", List.of("unit", "promote/" + overview.details().division())))), "The promote button asks for the field officer confirmation: " + opened);
            });
            var confirm = new DialogPayloads.Show(Files.readString(Path.of(directory, "kor-field-officer-promote.json")), true).view().issued(UUID.randomUUID().toString(), 7);
            game.runOnClient(c -> {
                DialogCoordinator.receive(DialogPayloads.Show.of(confirm, true));
                check(c.gui.screen() instanceof CommanderConfirmation modal && modal.backdrop() == overview && overview.details() != null, "Promotion opens the field officer confirmation above the unit details");
            });
            var modal = capture(game, "hoi-division-window-promote-gui2");
            for (int[] bar : bars) same(open, modal, bar, .01, "The promotion confirmation hides no top bar picture");
            same(open, modal, rows, .01, "The promotion confirmation hides no list picture");
            same(open, modal, cards, .03, "The promotion confirmation hides no commander card or theater");
            game.runOnClient(c -> {
                int sent = requests.size();
                DialogCoordinator.close(confirm.token());
                check(c.gui.screen() == overview && overview.details() != null && overview.details().view() != null, "Closing the confirmation returns to the same unit details");
                check(requests.size() == sent + 1 && requests.getLast().action() == Action.OPEN && requests.getLast().division().equals(overview.details().division()), "The unit details are requested again after the confirmation");
            });
            for (int tab = 0; tab < 3; tab++) {
                int index = tab;
                game.runOnClient(c -> {
                    var frame = overview.details().frame();
                    click(overview, frame.x(10 + 120 * index + 56), frame.y(487 + 18));
                    check(overview.details().tab() == index, "Each tab of the window is clickable");
                });
                game.waitTicks(3);
                game.takeScreenshot("hoi-division-window-tab" + tab + "-gui2");
            }
            game.runOnClient(c -> {
                var frame = overview.details().frame();
                click(overview, frame.x(10 + 56), frame.y(487 + 18));
                for (int column = 0; column < CommanderDivisionWindow.GROUPS.length; column++) {
                    String group = CommanderDivisionWindow.GROUPS[column];
                    var stats = fixture.stats().stream().filter(stat -> stat.group().equals(group)).toList();
                    for (int r = 0; r < stats.size(); r++) {
                        String shown = overview.details().tooltip(frame.x(CommanderDivisionWindow.COLUMNS[column][1] + 60), frame.y(CommanderDivisionWindow.ROWS[r]));
                        check(shown.equals(stats.get(r).tooltip()) && shown.contains("\n\n§f§e"), group + " row " + r + " shows its original breakdown: " + shown);
                    }
                }
                for (var element : List.of(Map.entry("delete", new double[]{575, 17}), Map.entry("manpower", new double[]{613, 16}), Map.entry("equipment", new double[]{780, 16}), Map.entry("template", new double[]{70, 60}), Map.entry("name", new double[]{200, 52}),
                        Map.entry("experience", new double[]{478, 52}), Map.entry("composition", new double[]{216, 336}), Map.entry("hardness", new double[]{380, 336})))
                    check(overview.details().tooltip(frame.x(element.getValue()[0]), frame.y(element.getValue()[1])).equals(fixture.tooltip(element.getKey())) && !fixture.tooltip(element.getKey()).isEmpty(), "The " + element.getKey() + " element shows its original tooltip");
                boolean variant = false;
                for (double y = 121; y < 470; y += 10) variant |= overview.details().tooltip(frame.x(700), frame.y(y)).startsWith("§e");
                check(variant, "An equipment variant shows its original name and description");
                overview.mouseScrolled(frame.x(700), frame.y(300), 0, -4);
                check(overview.details().scroll() > 0, "The equipment list scrolls");
                overview.mouseScrolled(frame.x(700), frame.y(300), 0, 50);
                var terrain = overview.details().tooltip(frame.x(17 + 35), frame.y(353 + 5 + 20));
                check(terrain.startsWith("§e") && terrain.contains("%") && !terrain.contains("\n\n") && !terrain.contains("+0.0%"), "A terrain adjuster lists its effects without blank lines: " + terrain);
                var movement = overview.details().tooltip(frame.x(17 + 11), frame.y(353 + 5 + 50));
                check(!movement.contains("\n") && movement.contains("%") && !movement.startsWith("§e"), "A terrain adjuster icon shows only its own line: " + movement);
                overview.mouseScrolled(frame.x(200), frame.y(400), 0, -4);
                check(overview.details().adjustScroll() > 0, "The terrain adjusters scroll");
                overview.mouseScrolled(frame.x(200), frame.y(400), 0, 50);
                var gone = overview.details().tooltip(frame.x(822 + 15), frame.y(2 + 15));
                check(gone.equals(fixture.label("CLOSE")), "The close button shows its tooltip");
            });
            interactions(game, overview, fixture, requests, opened);
            for (int[] size : new int[][]{{1280, 720, 2}, {1920, 1080, 2}, {2560, 1440, 3}}) {
                game.getInput().resizeWindow(size[0], size[1]);
                game.runOnClient(c -> c.options.guiScale().set(size[2]));
                game.waitTicks(3);
                game.runOnClient(c -> {
                    var frame = overview.details().frame();
                    check(frame.x(0) >= 0 && frame.y(0) >= 0 && frame.x(CommanderDivisionWindow.WIDTH) <= overview.width && frame.y(CommanderDivisionWindow.HEIGHT) <= overview.height, "The window stays inside the screen");
                });
                game.takeScreenshot("hoi-division-window-" + size[0] + "x" + size[1] + "-gui" + size[2]);
            }
            game.getInput().resizeWindow(2560, 1440);
            game.runOnClient(c -> c.options.guiScale().set(2));
            game.waitTicks(3);
            game.runOnClient(c -> {
                var frame = overview.details().frame();
                click(overview, frame.x(822 + 15), frame.y(2 + 15));
                check(overview.details() == null && requests.getLast().action() == Action.CLOSE, "The close button closes the window");
                click(overview, overview.frame().x(13 + 1 + 38), overview.frame().y(CommanderOverview.LAYOUTS.get(overview.tab()).rowY() + 2 + 15));
                check(overview.details() != null, "The window opens again");
                overview.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                check(overview.details() == null && c.gui.screen() != null, "Escape closes only the window");
                click(overview, overview.frame().x(13 + 1 + 38), overview.frame().y(CommanderOverview.LAYOUTS.get(overview.tab()).rowY() + 2 + 15));
                var navy = overview.children().stream().filter(child -> child instanceof AbstractWidget w && w.getMessage().getString().equals(IndustryEquipmentDesigns.tooltip("TOPBAR_NAVY_DESC"))).map(child -> (AbstractWidget)child).findFirst().orElseThrow();
                click(overview, navy.getX() + navy.getWidth() / 2.0, navy.getY() + navy.getHeight() / 2.0);
                check(CommanderHud.overviewShown(CommanderOverviewPayloads.Tab.NAVY) && requests.getLast().action() == Action.CLOSE, "The top bar works while the unit details are open");
            });
        } finally {
            observe(game, null);
            game.runOnClient(c -> { CommanderDivisionWindow.requestObserver = null; CommanderHud.reset(); });
        }
    }
    private static void interactions(ClientGameTestContext game, CommanderOverview overview, View fixture, List<Request> requests, List<CommanderHudPayloads.Open> opened) {
        game.runOnClient(c -> {
            var frame = overview.details().frame();
            click(overview, frame.x(130 + 56), frame.y(487 + 18));
            check(overview.details().tooltip(frame.x(60), frame.y(84)).equals(fixture.tooltip("line")) && overview.details().tooltip(frame.x(480), frame.y(179)).equals(fixture.tooltip("support"))
                    && overview.details().tooltip(frame.x(230), frame.y(453)).equals(fixture.tooltip("regiment")) && overview.details().tooltip(frame.x(254), frame.y(125)).equals(fixture.tooltip("width")), "The template tab titles show their original tooltips");
            var unit = fixture.columns().getFirst().getFirst();
            var shown = overview.details().tooltip(frame.x(23 + 40), frame.y(162 + 20));
            check(shown.startsWith(unit.tooltip()) && (unit.terrains().isEmpty() || shown.contains("%")), "A battalion shows its original statistics: " + shown);
            click(overview, frame.x(10 + 56), frame.y(487 + 18));
            click(overview, frame.x(70), frame.y(60));
            check(overview.details().dropdown(), "The template button opens the template list");
        });
        game.waitTicks(3);
        game.takeScreenshot("hoi-division-window-templates-gui2");
        game.runOnClient(c -> {
            var frame = overview.details().frame();
            int allowed = 0;
            while (!fixture.templates().get(allowed).blocked().isEmpty()) allowed++;
            overview.mouseScrolled(frame.x(100), frame.y(200), 0, -allowed);
            opened.clear();
            for (double y = 75; y < 360 && opened.isEmpty(); y += 10) if (overview.details().tooltip(frame.x(60), frame.y(y)).isEmpty()) click(overview, frame.x(60), frame.y(y));
            check(opened.size() == 1 && opened.getFirst().target().equals("DIVISIONS_TEMPLATE") && opened.getFirst().selection().getFirst().equals("unit/" + overview.details().division()) && opened.getFirst().selection().get(1).startsWith("template/") && !overview.details().dropdown(), "A template entry asks for the change confirmation: " + opened);
            click(overview, frame.x(200), frame.y(52));
            check(overview.details().editing() && overview.getFocused() == overview.details().editor() && !overview.allowsMovement(), "Clicking the name starts editing it");
            overview.details().editor().setValue("시험 사단");
        });
        game.waitTicks(3);
        game.takeScreenshot("hoi-division-window-name-gui2");
        game.runOnClient(c -> {
            overview.keyPressed(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(!overview.details().editing() && requests.getLast().action() == Action.RENAME && requests.getLast().name().equals("시험 사단") && overview.allowsMovement(), "Enter sends the new name");
            var frame = overview.details().frame();
            click(overview, frame.x(200), frame.y(52));
            int sent = requests.size();
            overview.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            check(!overview.details().editing() && overview.details() != null && requests.size() == sent, "Escape cancels the name without closing the window");
            double before = frame.x(0), top = frame.y(0);
            var press = new MouseButtonEvent(frame.x(300), frame.y(20), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            overview.mouseClicked(press, false);
            overview.mouseDragged(new MouseButtonEvent(press.x() + 40, press.y() + 25, press.buttonInfo()), 40, 25);
            overview.mouseReleased(press);
            check(Math.abs(overview.details().frame().x(0) - before - 40) < 1 && Math.abs(overview.details().frame().y(0) - top - 25) < 1, "Dragging the background moves the window");
            var back = overview.details().frame();
            var again = new MouseButtonEvent(back.x(300), back.y(20), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            overview.mouseClicked(again, false);
            overview.mouseDragged(new MouseButtonEvent(again.x() - 40, again.y() - 25, again.buttonInfo()), -40, -25);
            overview.mouseReleased(again);
        });
    }
    private static void observe(ClientGameTestContext game, Consumer<CommanderHudPayloads.Open> observer) {
        game.runOnClient(c -> {
            try { var field = CommanderHud.class.getDeclaredField("requestObserver"); field.setAccessible(true); field.set(null, observer); }
            catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        });
    }
    private static BufferedImage capture(ClientGameTestContext game, String name) throws IOException {
        game.getInput().setCursorPos(game.computeOnClient(c -> c.getWindow().getWidth()) / 2.0, 250);
        game.waitTicks(3);
        return ImageIO.read(game.takeScreenshot(name).toFile());
    }
    private static double changed(BufferedImage first, BufferedImage second, int[] gui) {
        int count = 0, total = 0;
        for (int y = gui[1] * 2; y < gui[3] * 2; y++) for (int x = gui[0] * 2; x < gui[2] * 2; x++) {
            int a = first.getRGB(x, y), b = second.getRGB(x, y);
            total++;
            if (Math.abs((a >> 16 & 255) - (b >> 16 & 255)) > 24 || Math.abs((a >> 8 & 255) - (b >> 8 & 255)) > 24 || Math.abs((a & 255) - (b & 255)) > 24) count++;
        }
        return count / (double)total;
    }
    private static void same(BufferedImage first, BufferedImage second, int[] gui, double limit, String message) {
        double ratio = changed(first, second, gui);
        check(ratio <= limit, message + ": " + ratio + " of " + Arrays.toString(gui));
    }
    private static AbstractWidget widget(CommanderOverview screen, Class<?> type, String label) {
        return screen.children().stream().filter(child -> type.isInstance(child) && child instanceof AbstractWidget w && w.getMessage().getString().startsWith(label)).map(child -> (AbstractWidget)child).findFirst().orElseThrow();
    }
    private static View view(View source, String token, String division) { return view(source, token, division, source.officer()); }
    private static View view(View source, String token, String division, Officer officer) {
        return new View(token, division, source.country(), source.name(), source.custom(), source.template(), source.templateId(), source.locked(), source.priority(), source.templates(), source.tooltips(), source.icon(), source.obsolete(), source.level(), source.experience(), source.manpower(), source.equipment(), officer,
                source.stats(), source.equipmentRows(), source.columns(), source.support(), source.combatWidth(), source.hardness(), source.terrains(), source.labels(), source.error());
    }
    private static void click(CommanderOverview screen, double x, double y) {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
