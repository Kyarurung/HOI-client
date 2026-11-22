package dev.hoi.client.screen.map;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.protocol.dialog.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;

public final class MapAirDetailChecks {
    private static final int[][] SIZES = {{2560, 1440}, {1600, 1000}, {854, 480}};
    private MapAirDetailChecks() {}
    public static void show(DialogView view) { DialogCoordinator.receive(DialogPayloads.Show.of(view, true)); }
    public static void run(ClientGameTestContext game) {
        var region = read("kor-air-region.json");
        var details = read("kor-air-region-details.json");
        for (int[] size : SIZES) {
            game.runOnClient(client -> client.options.guiScale().set(2));
            game.getInput().resizeWindow(size[0], size[1]);
            game.waitTicks(3);
            game.setScreen(() -> null);
            String suffix = "-" + size[0] + "-gui2";
            layout(game, region.issued(UUID.randomUUID().toString(), 1), size, "hoi-air-detail" + suffix);
            layout(game, details.issued(UUID.randomUUID().toString(), 1), size, "hoi-air-details-open" + suffix);
            if (size[0] == 2560) {
                clicks(game, region);
                tooltips(game, region);
                filters(game, region);
                chart(game, details);
                control(game, region);
            }
            renderers(game, region.issued(UUID.randomUUID().toString(), 1));
        }
        game.setScreen(() -> null);
    }
    private static DialogView read(String name) {
        String directory = Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW"), "Authoritative commander fixtures are required for the air region window");
        try { return new Gson().fromJson(Files.readString(Path.of(directory, name)), DialogView.class); } catch (IOException error) { throw new UncheckedIOException(error); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void away(ClientGameTestContext game) { game.getInput().setCursorPos(0, 0); game.waitTicks(3); }
    private static void layout(ClientGameTestContext game, DialogView view, int[] size, String shot) {
        game.runOnClient(client -> {
            DialogCoordinator.receive(DialogPayloads.Show.of(view, true));
            check(client.gui.screen() instanceof MapAirDetail, "Air marker must open air details");
            var screen = (MapAirDetail)client.gui.screen();
            check(screen.panelTop() >= HudMenuBar.height(screen.width) && screen.panelTop() + screen.panelHeight() <= screen.height && screen.panelLeft() >= 0 && screen.panelLeft() + screen.panelWidth() <= screen.width, "Air panel escaped viewport");
            for (var child : screen.children()) if (child instanceof Button button) check(button.getX() >= 0 && button.getY() >= 0 && button.getRight() <= screen.width && button.getBottom() <= screen.height, "Air button escaped viewport at " + button.getX() + "," + button.getY());
            if (screen.detailsOpen()) check(screen.details().x(513) <= screen.width + 1 && screen.details().y(495) <= screen.height + 1 && screen.details().y(0) >= 0, "Air details escaped viewport");
            else check(Math.abs(screen.panelLeft() * 2 + screen.panelWidth() - screen.width) <= 1, "Air detail must be centered");
            if (size[0] == 2560) check(screen.panelWidth() == 414, "Air panel must keep 1.5 times the original size at GUI2: " + screen.panelWidth());
            for (var tile : view.tiles()) if (!tile.icon().isBlank()) check(client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/" + tile.icon() + ".png")).isPresent(), "Missing air asset: " + tile.icon());
        });
        away(game);
        game.takeScreenshot(shot);
    }
    private static List<String> sent(ClientGameTestContext game, DialogView view) {
        var sent = new ArrayList<String>();
        game.setScreen(() -> new MapAirDetail(view, (shown, choice) -> sent.add(choice)));
        game.waitTicks(2);
        return sent;
    }
    private static void press(ClientGameTestContext game, boolean details, double px, double py) {
        game.runOnClient(client -> {
            var screen = (MapAirDetail)client.gui.screen();
            UiMenuStyle.Frame area = details ? screen.details() : screen.frame();
            var event = new MouseButtonEvent(area.x(px), area.y(py), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            screen.mouseClicked(event, false);
            screen.mouseReleased(event);
        });
    }
    private static void clicks(ClientGameTestContext game, DialogView view) {
        var sent = sent(game, view);
        press(game, false, 272, 126); press(game, false, 80, 241); press(game, false, 464, 241);
        press(game, false, 227, 240); press(game, false, 272, 240); press(game, false, 317, 240);
        check(sent.equals(List.of("air/details", "air/missions/own", "air/missions/enemy", "air/group/FIGHTER_SUPPORT", "air/group/NAVAL", "air/group/BOMBING_LOGISTICS")), "Air buttons send their choices: " + sent);
        sent.clear();
        game.getInput().holdShift();
        game.waitTicks(1);
        press(game, false, 272, 240);
        game.getInput().releaseShift();
        game.waitTicks(1);
        game.getInput().holdControl();
        game.waitTicks(1);
        press(game, false, 272, 240);
        game.getInput().releaseControl();
        game.waitTicks(1);
        check(sent.equals(List.of("air/only/NAVAL", "air/all")), "Shift and Ctrl clicks keep the original filter actions: " + sent);
        game.runOnClient(client -> {
            var screen = (MapAirDetail)client.gui.screen();
            int left = screen.panelLeft(), top = screen.panelTop();
            var down = new MouseButtonEvent(screen.frame().x(60), screen.frame().y(12), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            screen.mouseClicked(down, false);
            screen.mouseDragged(new MouseButtonEvent(down.x() - 60, down.y() + 20, down.buttonInfo()), -60, 20);
            check(screen.panelLeft() == left - 60 && screen.panelTop() == top + 20, "Dragging an empty place moves the window");
            screen.mouseDragged(new MouseButtonEvent(down.x() - 9000, down.y() + 9000, down.buttonInfo()), -9000, 9000);
            check(screen.panelLeft() == 0 && screen.panelTop() + screen.panelHeight() <= screen.height, "A dragged window stays inside the screen");
            screen.mouseDragged(new MouseButtonEvent(down.x(), down.y(), down.buttonInfo()), 10, 0);
            check(screen.panelLeft() == 10, "Dragging back starts from the edge");
            screen.mouseReleased(down);
        });
    }
    private static String hovered(ClientGameTestContext game, double px, double py) {
        var cursor = game.computeOnClient(client -> {
            var screen = (MapAirDetail)client.gui.screen();
            double gui = client.getWindow().getGuiScale();
            return new double[]{screen.frame().x(px) * gui, screen.frame().y(py) * gui};
        });
        game.getInput().setCursorPos(cursor[0], cursor[1]);
        game.waitTicks(3);
        return game.computeOnClient(client -> ((MapAirDetail)client.gui.screen()).tooltipText());
    }
    private static void tooltips(ClientGameTestContext game, DialogView view) {
        sent(game, view);
        var expected = new LinkedHashMap<String, double[]>();
        expected.put("details", new double[]{272, 126}); expected.put("missions_own", new double[]{80, 241}); expected.put("missions_enemy", new double[]{464, 241});
        expected.put("balance", new double[]{272, 88}); expected.put("weather", new double[]{272, 50}); expected.put("detection", new double[]{30, 126}); expected.put("enemy_efficiency", new double[]{411, 126});
        expected.put("own_fighters", new double[]{130, 162}); expected.put("enemy_planes", new double[]{445, 277}); expected.put("mission_filter_1", new double[]{272, 240});
        expected.put("fighter_area", new double[]{60, 200}); expected.put("mission_area", new double[]{60, 320}); expected.put("own_damage", new double[]{225, 279}); expected.put("enemy_damage", new double[]{355, 279});
        expected.forEach((section, point) -> {
            var tile = view.tiles().stream().filter(row -> row.section().equals(section)).findFirst().orElseThrow(() -> new AssertionError(section));
            String shown = hovered(game, point[0], point[1]);
            check(shown.equals(tile.detail()), section + " shows its own tooltip: " + shown);
            if (Set.of("balance", "weather", "detection", "own_damage", "mission_filter_1").contains(section)) game.takeScreenshot("hoi-air-tooltip-" + section + "-gui2");
        });
        var enemies = view.tiles().stream().filter(row -> row.section().equals("enemy")).toList();
        for (int i = 0; i < 4; i++) check(hovered(game, i < 2 ? 484 : 413, i % 2 == 0 ? 47 : 82).equals(enemies.get(i).detail()), "Enemy aircraft count " + i + " shows the enemy text");
        away(game);
    }
    private static void filters(ClientGameTestContext game, DialogView view) {
        var tiles = new ArrayList<>(view.tiles());
        tiles.replaceAll(tile -> tile.section().equals("mission_filter_1") ? new DialogView.Tile(tile.section(), tile.name(), "0", tile.icon(), tile.detail()) : tile);
        sent(game, new DialogView(UUID.randomUUID().toString(), 1, view.kind(), view.title(), "", "", "", view.image(), "", tiles, view.choices()));
        away(game);
        var shot = game.takeScreenshot("hoi-air-category-off-gui2");
        var dots = game.computeOnClient(client -> {
            var screen = (MapAirDetail)client.gui.screen();
            double gui = client.getWindow().getGuiScale();
            int[][] result = new int[3][2];
            for (int i = 0; i < 3; i++) { result[i][0] = (int)Math.round(screen.frame().x(205 + 45 * i + 6) * gui); result[i][1] = (int)Math.round(screen.frame().y(231) * gui); }
            return result;
        });
        var image = image(shot);
        for (int i = 0; i < 3; i++) {
            boolean green = false;
            for (int dy = -4; dy <= 4; dy++) for (int dx = -4; dx <= 4; dx++) {
                int rgb = image.getRGB(dots[i][0] + dx, dots[i][1] + dy), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                green |= g > r + 40 && g > b + 40;
            }
            check(green == (i != 1), "Mission filter " + i + " must use the original " + (i != 1 ? "ON" : "OFF") + " frame");
        }
    }
    private static java.awt.image.BufferedImage image(Path shot) { try { return javax.imageio.ImageIO.read(shot.toFile()); } catch (IOException error) { throw new UncheckedIOException(error); } }
    private static void chart(ClientGameTestContext game, DialogView view) {
        var sent = sent(game, view);
        press(game, true, 256, 215); press(game, true, 346, 215); press(game, true, 166, 215);
        check(sent.equals(List.of("air/span/MONTH", "air/span/YEAR", "air/span/DAY")), "Chart tabs send their spans: " + sent);
        away(game);
        var plain = image(game.takeScreenshot("hoi-air-chart-empty-gui2"));
        press(game, true, 120, 235 + 22 * 3 + 10); press(game, true, 266 + 120, 235 + 22 + 10); press(game, true, 266 + 120, 235 + 22 * 3 + 10); press(game, true, 266 + 120, 235 + 22 * 3 + 10);
        game.runOnClient(client -> check(((MapAirDetail)client.gui.screen()).channels().equals(Set.of(3, 13)), "Channel buttons toggle their own line"));
        game.waitTicks(2);
        var drawn = image(game.takeScreenshot("hoi-air-chart-lines-gui2"));
        var area = game.computeOnClient(client -> {
            var screen = (MapAirDetail)client.gui.screen();
            double gui = client.getWindow().getGuiScale();
            return new int[]{(int)(screen.details().x(54) * gui), (int)(screen.details().y(25) * gui), (int)(screen.details().x(434) * gui), (int)(screen.details().y(175) * gui)};
        });
        boolean red = false, yellow = false, before = false;
        for (int y = area[1]; y < area[3]; y++) for (int x = area[0]; x < area[2]; x++) {
            red |= line(drawn.getRGB(x, y), 255, 38, 0);
            yellow |= line(drawn.getRGB(x, y), 242, 255, 0);
            before |= line(plain.getRGB(x, y), 255, 38, 0) || line(plain.getRGB(x, y), 242, 255, 0);
        }
        check(red && yellow && !before, "Selected channels draw the original line colours: " + red + " " + yellow + " " + before);
        game.runOnClient(client -> {
            var screen = (MapAirDetail)client.gui.screen();
            screen.update(new DialogView(view.token(), view.revision(), view.kind(), view.title(), "", "", "", view.image(), "", view.tiles(), view.choices()));
            check(screen.channels().equals(Set.of(3, 13)), "Refreshing the view keeps the selected channels");
        });
    }
    private static boolean line(int rgb, int red, int green, int blue) { return Math.abs((rgb >> 16 & 255) - red) < 4 && Math.abs((rgb >> 8 & 255) - green) < 4 && Math.abs((rgb & 255) - blue) < 4; }
    private static void control(ClientGameTestContext game, DialogView view) {
        for (String state : List.of("none", "friendly", "enemy")) {
            double share = state.equals("none") ? 0 : state.equals("friendly") ? .8 : .2;
            var tiles = new ArrayList<>(view.tiles());
            tiles.replaceAll(tile -> tile.section().equals("balance") ? new DialogView.Tile("balance", tile.name(), "", "air_detail/progress_" + (state.equals("none") ? "disabled" : "good"), tile.detail(), new DialogView.Gauge(share, "")) : tile);
            sent(game, new DialogView(UUID.randomUUID().toString(), 1, view.kind(), view.title(), "", "", "", view.image(), "", tiles, view.choices()));
            away(game);
            var image = image(game.takeScreenshot("hoi-air-control-" + state + "-gui2"));
            var bar = game.computeOnClient(client -> {
                var screen = (MapAirDetail)client.gui.screen();
                double gui = client.getWindow().getGuiScale();
                return new int[]{(int)(screen.frame().x(147) * gui), (int)(screen.frame().x(397) * gui), (int)(screen.frame().y(87) * gui)};
            });
            int green = 0;
            for (int x = bar[0]; x < bar[1]; x++) { int rgb = image.getRGB(x, bar[2]), r = rgb >> 16 & 255, g = rgb >> 8 & 255; if (g > r + 30) green++; }
            check(Math.abs(green / (double)(bar[1] - bar[0]) - share) < .04, "Air superiority bar fills our share: " + state + " " + green);
        }
    }
    private static void renderers(ClientGameTestContext game, DialogView view) {
        game.setScreen(() -> null);
        game.runOnClient(client -> {
            DialogCoordinator.receive(DialogPayloads.Show.of(view, true));
            var state = new DialogView(view.token(), 2, view.kind(), view.title(), "", "", "", "map_detail/state", "", List.of(), List.of());
            DialogCoordinator.receive(DialogPayloads.Show.of(state, true));
            check(client.gui.screen() instanceof MapDetail, "Map mode refresh must switch renderer");
            DialogCoordinator.receive(DialogPayloads.Show.of(view.issued(view.token(), 3), true));
            check(client.gui.screen() instanceof MapAirDetail, "Air mode refresh must switch renderer");
            DialogCoordinator.receive(DialogPayloads.Show.of(state, true));
            check(client.gui.screen() instanceof MapAirDetail, "Stale state view replaced air view");
        });
        game.setScreen(() -> null);
    }
}
