package dev.hoi.client.screen.commander;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.client.ui.UiStatCell;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderOverviewPayloads.*;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.menu.MenuView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class CommanderOverviewTest {
    private CommanderOverviewTest() {}
    public static void run(ClientGameTestContext game, MenuView menu) {
        String directory = System.getenv("HOI_COMMANDER_PREVIEW");
        if (directory == null) throw new AssertionError("Authoritative commander fixtures are required for the overview windows");
        var previous = game.computeOnClient(c -> ClientCampaignHud.visible() ? HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()) : HudPayloads.State.HIDDEN);
        var requests = new ArrayList<Request>();
        try {
            var views = new EnumMap<Tab, View>(Tab.class);
            for (var tab : Tab.values()) views.put(tab, new Snapshot(Files.readString(Path.of(directory, "kor-overview-" + tab.name().toLowerCase(Locale.ROOT) + ".json"))).view());
            game.getInput().resizeWindow(2560, 1440);
            game.runOnClient(c -> { c.options.guiScale().set(2); CommanderOverview.requestObserver = requests::add; ClientCampaignHud.accept(HudPayloads.State.of("KOR", menu.hud())); CommanderHud.reset(); });
            for (var tab : Tab.values()) {
                var screen = open(game, requests, views.get(tab), tab, false);
                game.runOnClient(c -> {
                    var fixture = views.get(tab);
                    check(screen.rows().size() == fixture.rows().size(), tab + " overview lists every authoritative row");
                    check(screen.rows().equals(fixture.rows().stream().sorted(Comparator.comparing(Row::type).thenComparing(Row::name).thenComparing(Row::id)).toList()), tab + " rows start sorted by type like the original");
                });
                game.takeScreenshot("hoi-overview-" + tab.name().toLowerCase(Locale.ROOT) + "-gui2");
                modifiers(game, screen, views.get(tab), tab);
            }
            var army = open(game, requests, views.get(Tab.ARMY), Tab.ARMY, true);
            game.runOnClient(c -> check(army.rows().getFirst().fighting(), "The fixture marks the first listed division as fighting"));
            game.takeScreenshot("hoi-overview-army-combat-gui2");
            hover(game, army, 13 + 10, 275 + 3 + 9); game.waitTicks(3);
            game.takeScreenshot("hoi-overview-army-battle-tooltip-gui2");
            hover(game, army, 13 + 200, 275 + 17); game.waitTicks(3);
            game.takeScreenshot("hoi-overview-army-row-tooltip-gui2");
            game.runOnClient(c -> {
                click(army, 138 + 80, 238 + 11, InputConstants.MOUSE_BUTTON_LEFT, 0);
                check(isSorted(army.rows(), Comparator.comparing(Row::name)), "The name column sorts by name");
                click(army, 138 + 80, 238 + 11, InputConstants.MOUSE_BUTTON_LEFT, 0);
                check(isSorted(army.rows(), Comparator.comparing(Row::name).reversed()), "A second name click reverses the order");
                String type = army.rows().stream().map(Row::type).filter(key -> !key.equals(army.rows().getFirst().type())).findFirst().orElse(army.rows().getFirst().type());
                int index = views.get(Tab.ARMY).types().stream().map(Count::key).toList().indexOf(type);
                click(army, 29 - 14 + index % 6 * 86 + 40, 175 + 6 + index / 6 * 29 + 14, InputConstants.MOUSE_BUTTON_LEFT, 0);
                check(!army.rows().isEmpty() && army.rows().stream().allMatch(row -> row.type().equals(type)), "A type entry filters the list to that division type");
                click(army, 29 - 14 + index % 6 * 86 + 40, 175 + 6 + index / 6 * 29 + 14, InputConstants.MOUSE_BUTTON_LEFT, 0);
                check(army.rows().size() == views.get(Tab.ARMY).rows().size(), "A second type click clears the filter");
                var target = army.rows().getFirst();
                clickRow(army, 0, InputConstants.MOUSE_BUTTON_LEFT, 0);
                check(CommanderHud.overviewSelection(Tab.ARMY).equals(Set.of(target.id())), "A row click selects that division");
                var second = army.rows().get(1);
                clickRow(army, 1, InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOD_SHIFT);
                check(CommanderHud.overviewSelection(Tab.ARMY).equals(Set.of(target.id(), second.id())), "Shift-click adds a division");
                clickRow(army, 1, InputConstants.MOUSE_BUTTON_RIGHT, 0);
                check(requests.getLast().action() == Action.FOCUS && requests.getLast().target().equals(second.id()), "Right-click asks the server to move to the division");
                army.mouseScrolled(army.frame().x(200), army.frame().y(400), 0, -3);
                check(army.scroll() > 0, "The mouse wheel scrolls the division list");
            });
            game.waitTicks(2);
            game.takeScreenshot("hoi-overview-army-scrolled-gui2");
            game.runOnClient(c -> army.mouseScrolled(army.frame().x(200), army.frame().y(400), 0, 100));
            hover(game, army, 29 - 14 + 40, 175 + 6 + 14); game.waitTicks(3);
            game.takeScreenshot("hoi-overview-army-type-tooltip-gui2");
            hover(game, army, 300, 93); game.waitTicks(3);
            game.takeScreenshot("hoi-overview-army-special-tooltip-gui2");
            game.runOnClient(c -> {
                army.setFocused(null);
                var filter = army.children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.EditBox).map(child -> (net.minecraft.client.gui.components.EditBox)child).findFirst().orElseThrow();
                String name = army.rows().getLast().name(); filter.setValue(name);
                check(!army.rows().isEmpty() && army.rows().stream().allMatch(row -> row.name().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))), "Typing a division name filters the list to that name: " + army.rows().size());
                filter.setValue("");
                check(army.rows().size() == views.get(Tab.ARMY).rows().size(), "Clearing the filter restores every division");
            });
            CommanderDivisionWindowTest.run(game, army, directory);
            CommanderNavalLossWindowTest.run(game, open(game, requests, views.get(Tab.NAVY), Tab.NAVY, false), directory, views.get(Tab.NAVY).label("SUNK_SHIP_DETAILS"), "-2560x1440-gui2");
            for (int[] size : new int[][]{{1920, 1080, 2}, {2560, 1440, 3}, {854, 480, 2}}) {
                game.getInput().resizeWindow(size[0], size[1]);
                game.runOnClient(c -> c.options.guiScale().set(size[2]));
                open(game, requests, views.get(Tab.NAVY), Tab.NAVY, false);
                game.takeScreenshot("hoi-overview-navy-" + size[0] + "x" + size[1] + "-gui" + size[2]);
            }
            for (int[] size : new int[][]{{1280, 720, 2}, {854, 480, 2}}) {
                game.getInput().resizeWindow(size[0], size[1]);
                game.runOnClient(c -> c.options.guiScale().set(size[2]));
                CommanderNavalLossWindowTest.run(game, open(game, requests, views.get(Tab.NAVY), Tab.NAVY, false), directory, views.get(Tab.NAVY).label("SUNK_SHIP_DETAILS"), "-" + size[0] + "x" + size[1] + "-gui" + size[2]);
            }
        } catch (IOException error) { throw new AssertionError(error); }
        finally {
            game.getInput().resizeWindow(2560, 1440);
            game.runOnClient(c -> { c.options.guiScale().set(2); CommanderOverview.requestObserver = null; CommanderHud.reset(); ClientCampaignHud.accept(previous); });
            game.setScreen(() -> null);
        }
    }
    private static void modifiers(ClientGameTestContext game, CommanderOverview screen, View fixture, Tab tab) throws IOException {
        check(fixture.modifiers().size() == (tab == Tab.NAVY ? 7 : 14), tab + " overview receives the original cells of its branch");
        var shown = NativeImage.read(Files.newInputStream(game.takeScreenshot("hoi-overview-modifiers-" + tab.name().toLowerCase(Locale.ROOT) + "-gui2")));
        game.runOnClient(c -> screen.accept(without(screen, fixture)));
        game.waitTicks(2);
        var empty = NativeImage.read(Files.newInputStream(game.takeScreenshot("hoi-overview-no-modifiers-" + tab.name().toLowerCase(Locale.ROOT) + "-gui2")));
        game.runOnClient(c -> screen.accept(fixture(screen, fixture)));
        for (int i = 0; i < fixture.modifiers().size(); i++) {
            double x = 15 + i % 7 * 74, y = 114 + i / 7 * 33;
            String value = fixture.modifiers().get(i).value();
            int[] area = game.computeOnClient(c -> new int[]{(int)Math.ceil(screen.frame().x(x + 2) * 2), (int)Math.ceil(screen.frame().y(y + 4) * 2), (int)Math.floor((screen.frame().x(x + 71) - UiStatCell.textWidth(c.font, value, screen.frame().scale())) * 2), (int)Math.floor(screen.frame().x(x + 71) * 2), (int)Math.floor(screen.frame().y(y + 28) * 2)});
            check(value.endsWith("시간§r") || game.computeOnClient(c -> UiStatCell.textWidth(c.font, value, screen.frame().scale()) >= c.font.width(Tooltips.officerText(value))), tab + " overview cell " + i + " shows its whole value");
            int icon = 0, text = 0;
            for (int py = area[1]; py < area[4]; py++) for (int px = area[0]; px < area[3]; px++) if (bright(shown.getPixel(px, py)) && !bright(empty.getPixel(px, py))) { if (px < area[2]) icon++; else text++; }
            check(icon >= 8 && text >= 8, tab + " overview draws the icon and the value of cell " + i + ": " + icon + ", " + text);
        }
        shown.close(); empty.close();
        int last = fixture.modifiers().size() - 1;
        for (int i : new int[]{0, last}) {
            hover(game, screen, 15 + i % 7 * 74 + 37, 114 + i / 7 * 33 + 16); game.waitTicks(3);
            check(game.computeOnClient(c -> screen.hoveredTooltip()).equals(fixture.modifiers().get(i).tooltip()), tab + " overview cell " + i + " shows its original tooltip");
            game.takeScreenshot("hoi-overview-" + tab.name().toLowerCase(Locale.ROOT) + "-modifier-" + i + "-gui2");
        }
        game.waitTicks(25);
        game.takeScreenshot("hoi-overview-" + tab.name().toLowerCase(Locale.ROOT) + "-modifier-delayed-gui2");
    }
    private static boolean bright(int argb) { return Math.max(argb >> 16 & 255, Math.max(argb >> 8 & 255, argb & 255)) > 110; }
    private static View fixture(CommanderOverview screen, View view) { return new View(screen.token(), view.tab(), view.country(), view.total(), view.secondary(), view.special(), view.specialTooltip(), view.modifiers(), view.types(), view.rows(), view.footer(), view.labels(), view.error()); }
    private static View without(CommanderOverview screen, View view) { return new View(screen.token(), view.tab(), view.country(), view.total(), view.secondary(), view.special(), view.specialTooltip(), List.of(), view.types(), view.rows(), view.footer(), view.labels(), view.error()); }
    private static CommanderOverview open(ClientGameTestContext game, List<Request> requests, View fixture, Tab tab, boolean fighting) {
        var screen = game.computeOnClient(c -> new CommanderOverview(tab));
        game.setScreen(() -> screen);
        game.runOnClient(c -> {
            screen.open();
            var open = requests.getLast();
            check(open.action() == Action.OPEN && open.tab() == tab, "Opening the overview requests it from the server");
            var rows = new ArrayList<>(fixture.rows());
            if (fighting) {
                var row = rows.stream().min(Comparator.comparing(Row::type).thenComparing(Row::name).thenComparing(Row::id)).orElseThrow();
                rows.set(rows.indexOf(row), new Row(row.id(), row.type(), row.icon(), row.name(), row.location(), row.group(), row.color(), row.insignia(), row.organization() / 3, row.strength(), row.level(), true, row.obsolete(), row.leader(), row.amount(), row.missions(), row.tooltip(), row.selection()));
            }
            screen.accept(new View(open.token(), fixture.tab(), fixture.country(), fixture.total(), fixture.secondary(), fixture.special(), fixture.specialTooltip(), fixture.modifiers(), fixture.types(), rows, fixture.footer(), fixture.labels(), fixture.error()));
        });
        game.getInput().setCursorPos(game.computeOnClient(c -> c.getWindow().getWidth()) - 4, game.computeOnClient(c -> c.getWindow().getHeight()) - 4);
        game.waitTicks(3);
        return screen;
    }
    private static void hover(ClientGameTestContext game, CommanderOverview screen, double x, double y) {
        double[] point = game.computeOnClient(c -> new double[]{screen.frame().x(x) * c.getWindow().getGuiScale(), screen.frame().y(y) * c.getWindow().getGuiScale()});
        game.getInput().setCursorPos(point[0], point[1]);
    }
    private static void click(CommanderOverview screen, double x, double y, int button, int modifiers) {
        var event = new MouseButtonEvent(screen.frame().x(x), screen.frame().y(y), new MouseButtonInfo(button, modifiers));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }
    private static void clickRow(CommanderOverview screen, int index, int button, int modifiers) {
        click(screen, 13 + 200, CommanderOverview.LAYOUTS.get(screen.tab()).rowY() + index * CommanderOverview.ROW_HEIGHT + 17 - screen.scroll(), button, modifiers);
    }
    private static boolean isSorted(List<Row> rows, Comparator<Row> order) {
        for (int i = 1; i < rows.size(); i++) if (order.compare(rows.get(i - 1), rows.get(i)) > 0) return false;
        return true;
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
