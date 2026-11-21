package dev.hoi.client.ui.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.client.screen.ScreenKoreanMusic;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.client.screen.commander.CommanderOverview;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.commander.CommanderOverviewPayloads;
import dev.hoi.protocol.commander.CommanderOverviewPayloads.Tab;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.menu.MenuView;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class HudTimeControlsTest {
    private static final String DATE = "16:00, 26 12월, 2022";
    private HudTimeControlsTest() {}
    public static void run(ClientGameTestContext game, MenuView menu) {
        String directory = System.getenv("HOI_COMMANDER_PREVIEW");
        if (directory == null) throw new AssertionError("Authoritative commander fixtures are required for the topbar overview buttons");
        var previous = game.computeOnClient(c -> ClientCampaignHud.visible() ? HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()) : HudPayloads.State.HIDDEN);
        var commands = new ArrayList<String>();
        var overviews = new ArrayList<CommanderOverviewPayloads.Request>();
        try {
            var army = view(directory, "army-two-theaters");
            game.getInput().resizeWindow(2560, 1440);
            game.runOnClient(c -> {
                c.options.guiScale().set(2);
                HudTimeControls.commandObserver = commands::add;
                CommanderOverview.requestObserver = overviews::add;
                ClientCampaignHud.accept(HudPayloads.State.of(army.country(), menu.hud()));
                ClientCampaignHud.accept(new HudPayloads.Clock(DATE, "X3", false));
                CommanderHud.reset();
                CommanderHud.accept(local(c, army));
            });
            game.setScreen(MapCursor::new);
            game.getInput().setCursorPos(1280, 900); game.waitTicks(3);
            game.runOnClient(c -> {
                for (String key : List.of("SPEED_INCREASE", "SPEED_DECREASE", "TOPBAR_ARMY", "TOPBAR_ARMY_DESC", "TOPBAR_NAVY", "TOPBAR_NAVY_DESC", "TOPBAR_AIR", "TOPBAR_AIR_DESC"))
                    check(!IndustryEquipmentDesigns.tooltip(key).equals(key), "Topbar label resolves from the original localisation: " + key);
                check(IndustryEquipmentDesigns.tooltip("SPEED_INCREASE").contains("속도 빠르게") && IndustryEquipmentDesigns.tooltip("SPEED_DECREASE").contains("속도 느리게"), "Speed buttons keep the original Korean labels");
                var screen = c.gui.screen();
                check(controls(screen).size() == 11, "Two speed buttons, the date bar, five speed steps and three branch buttons: " + controls(screen).size());
                for (String label : List.of("속도 느리게", "속도 빠르게", "일시정지", "2배속", "10배속")) check(!button(screen, label).active, "Only operators control the campaign speed: " + label);
                for (var branch : HudTimeControls.BRANCHES) check(button(screen, IndustryEquipmentDesigns.tooltip(branch.label() + "_DESC")).active, "Every assigned player can open the overview lists");
                click(screen, button(screen, "속도 빠르게"));
                click(screen, button(screen, "일시정지"));
                click(screen, button(screen, "10배속"));
                int left = HudTimeControls.left(screen.width), right = HudMenuBar.tensionX(screen.width);
                for (var widget : controls(screen))
                    check(widget.getX() >= left && widget.getRight() <= right + 1 && widget.getBottom() <= HudMenuBar.height(screen.width), "Topbar time controls stay inside the original background: " + widget.getMessage().getString());
                check(HudMenuBar.statsRight(screen.width) == left, "The resource strip ends where the original time background starts");
            });
            check(commands.isEmpty(), "A viewer click never sends a speed command: " + commands);
            var viewer = capture(game, "hoi-topbar-time-viewer");
            checkSteps(viewer, 2, false);
            checkDate(viewer);
            game.runOnClient(c -> ClientCampaignHud.accept(new HudPayloads.Clock(DATE, "X3", true)));
            game.waitTicks(2);
            var operator = capture(game, "hoi-topbar-time-operator");
            for (int bx : new int[]{79, 254})
                check(brightest(operator, bx, 10, 27, 27) > brightest(viewer, bx, 10, 27, 27) + 40, "Speed buttons are grey only for players without speed control: " + bx);
            game.runOnClient(c -> {
                var screen = c.gui.screen();
                for (String label : List.of("속도 느리게", "속도 빠르게", "일시정지", "2배속", "10배속")) check(button(screen, label).active, "Operators control the campaign speed: " + label);
                click(screen, button(screen, "속도 빠르게"));
                click(screen, button(screen, "속도 느리게"));
                click(screen, button(screen, "10배속"));
                click(screen, button(screen, "일시정지"));
            });
            check(commands.equals(List.of("속도 4", "속도 2", "속도 10", "속도 0")), "Speed clicks send the authoritative speed command: " + commands);
            commands.clear();
            game.runOnClient(c -> ClientCampaignHud.accept(new HudPayloads.Clock(DATE, "X10", true)));
            game.waitTicks(2);
            game.runOnClient(c -> {
                click(c.gui.screen(), button(c.gui.screen(), "속도 빠르게"));
                ClientCampaignHud.accept(new HudPayloads.Clock("17:00, 26 12월, 2022", "PAUSED", true));
            });
            game.waitTicks(2);
            var paused = capture(game, "hoi-topbar-time-paused");
            checkSteps(paused, 5, true);
            game.runOnClient(c -> {
                var screen = c.gui.screen();
                click(screen, button(screen, "속도 빠르게"));
                click(screen, button(screen, "속도 느리게"));
                click(screen, button(screen, "일시정지"));
            });
            check(commands.equals(List.of("속도 9")), "Paused speed changes wait for the resume click like the original: " + commands);
            hover(game, 254 + 13.5, 23.5, "hoi-topbar-speed-up-tooltip");
            hover(game, 124 + 19, 65, "hoi-topbar-army-tooltip");
            game.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT); game.waitTicks(2);
            var held = ImageIO.read(game.takeScreenshot("hoi-topbar-army-pressed-gui2").toFile());
            game.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT); game.waitTicks(2);
            var released = ImageIO.read(game.takeScreenshot("hoi-topbar-army-released-gui2").toFile());
            check(green(held, 124, 46, 38, 38) > 40 && green(released, 124, 46, 38, 38) < 10, "The army button is green only while it is pressed: " + green(held, 124, 46, 38, 38) + "/" + green(released, 124, 46, 38, 38));
            game.waitTicks(1);
            game.runOnClient(c -> check(CommanderHud.overviewShown(Tab.ARMY) && opened(overviews, Tab.ARMY), "The army button opens the army overview and requests it: " + overviews));
            game.runOnClient(c -> click(c.gui.screen(), branch(c.gui.screen(), Tab.ARMY)));
            game.waitTicks(2);
            game.runOnClient(c -> check(!CommanderHud.overviewShown(Tab.ARMY) && overviews.getLast().action() == CommanderOverviewPayloads.Action.CLOSE, "A second army click closes the army overview: " + overviews));
            game.setScreen(MapCursor::new); game.waitTicks(2);
            game.runOnClient(c -> click(c.gui.screen(), branch(c.gui.screen(), Tab.NAVY)));
            game.waitTicks(2);
            game.runOnClient(c -> check(CommanderHud.overviewShown(Tab.NAVY) && opened(overviews, Tab.NAVY), "The navy button opens the navy overview"));
            game.runOnClient(c -> click(c.gui.screen(), branch(c.gui.screen(), Tab.AIR)));
            game.waitTicks(2);
            game.runOnClient(c -> check(CommanderHud.overviewShown(Tab.AIR) && opened(overviews, Tab.AIR), "The air button switches to the air overview"));
            game.setScreen(() -> new ScreenMenu(menu)); game.waitTicks(3);
            game.runOnClient(c -> click(c.gui.screen(), branch(c.gui.screen(), Tab.ARMY)));
            game.waitTicks(3);
            game.runOnClient(c -> check(c.gui.screen() instanceof CommanderOverview overview && overview.tab() == Tab.ARMY, "The army button leaves a national menu for the army overview: " + c.gui.screen()));
            game.runOnClient(c -> {
                var music = c.gui.screen().children().stream().filter(child -> child instanceof HudMusicButton).map(child -> (AbstractWidget)child).findFirst().orElseThrow();
                check(Math.abs(music.getX() - HudTimeControls.x(c.gui.screen().width, 88)) < 1 && Math.abs(music.getY() - HudTimeControls.y(c.gui.screen().width, 48)) < 1, "The playlist button keeps the original socket");
                click(c.gui.screen(), music);
                check(c.gui.screen() instanceof ScreenKoreanMusic, "The playlist button opens the playlist");
            });
            game.setScreen(MapCursor::new);
            for (int[] size : new int[][]{{1920, 1080, 2}, {2560, 1440, 3}, {854, 480, 2}}) {
                game.getInput().resizeWindow(size[0], size[1]);
                game.runOnClient(c -> c.options.guiScale().set(size[2]));
                game.setScreen(MapCursor::new); game.waitTicks(3);
                game.runOnClient(c -> {
                    var screen = c.gui.screen();
                    var tabs = screen.children().stream().filter(child -> child instanceof HudMenuBar.HudTabButton).map(child -> (AbstractWidget)child).toList();
                    check(tabs.stream().allMatch(tab -> tab.getRight() <= HudTimeControls.left(screen.width)), "Time controls never cover a menu tab at " + size[0] + "x" + size[1] + " GUI " + size[2]);
                });
                game.takeScreenshot("hoi-topbar-time-" + size[0] + "x" + size[1] + "-gui" + size[2]);
            }
        } catch (IOException error) { throw new AssertionError(error); }
        finally {
            game.getInput().resizeWindow(2560, 1440);
            game.runOnClient(c -> { c.options.guiScale().set(2); HudTimeControls.commandObserver = null; CommanderOverview.requestObserver = null; CommanderHud.reset(); ClientCampaignHud.accept(previous); });
        }
    }
    private static CommanderHudPayloads.View view(String directory, String name) throws IOException {
        return new CommanderHudPayloads.Snapshot(Files.readString(Path.of(directory, "kor-command-panel-" + name + ".json"))).view();
    }
    private static CommanderHudPayloads.View local(Minecraft client, CommanderHudPayloads.View source) {
        return new CommanderHudPayloads.View(source.country(), client.level.dimension().identifier().toString(), source.branch(), source.cards(), source.wings(), source.running(), source.theaters(), source.armyTheaters());
    }
    private static List<AbstractWidget> controls(Screen screen) {
        return screen.children().stream().filter(child -> child instanceof HudTimeControls.HudTimeButton).map(child -> (AbstractWidget)child).toList();
    }
    private static AbstractWidget button(Screen screen, String label) {
        return controls(screen).stream().filter(widget -> widget.getMessage().getString().equals(label)).findFirst().orElseThrow(() -> new AssertionError("Missing topbar control: " + label));
    }
    private static AbstractWidget branch(Screen screen, Tab tab) {
        return button(screen, IndustryEquipmentDesigns.tooltip(HudTimeControls.BRANCHES.stream().filter(branch -> branch.tab() == tab).findFirst().orElseThrow().label() + "_DESC"));
    }
    private static boolean opened(List<CommanderOverviewPayloads.Request> requests, Tab tab) {
        return requests.stream().filter(request -> request.action() == CommanderOverviewPayloads.Action.OPEN).reduce((first, second) -> second).map(request -> request.tab() == tab).orElse(false);
    }
    private static void click(Screen screen, AbstractWidget widget) {
        var event = new MouseButtonEvent(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }
    private static BufferedImage capture(ClientGameTestContext game, String name) {
        game.getInput().setCursorPos(1280, 900); game.waitTicks(2);
        try { return ImageIO.read(game.takeScreenshot(name + "-gui2").toFile()); }
        catch (IOException error) { throw new AssertionError(error); }
    }
    private static void hover(ClientGameTestContext game, double bx, double by, String name) {
        game.getInput().setCursorPos(HudTimeControls.x(1280, bx) * 2, HudTimeControls.y(1280, by) * 2); game.waitTicks(4);
        game.takeScreenshot(name + "-gui2");
    }
    private static int px(double bx) { return Math.round(HudTimeControls.x(1280, bx) * 2); }
    private static int py(double by) { return Math.round(HudTimeControls.y(1280, by) * 2); }
    private static int brightest(BufferedImage image, double bx, double by, double bw, double bh) {
        int best = 0;
        for (int y = py(by); y < py(by + bh); y++) for (int x = px(bx); x < px(bx + bw); x++) {
            int rgb = image.getRGB(x, y);
            best = Math.max(best, Math.min((rgb >> 16) & 255, Math.min((rgb >> 8) & 255, rgb & 255)));
        }
        return best;
    }
    private static int green(BufferedImage image, double bx, double by, double bw, double bh) {
        int count = 0;
        for (int y = py(by); y < py(by + bh); y++) for (int x = px(bx); x < px(bx + bw); x++) {
            int rgb = image.getRGB(x, y), r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
            if (g > 90 && g > r + 30 && g > b + 30) count++;
        }
        return count;
    }
    private static void checkSteps(BufferedImage image, int lit, boolean redLast) {
        for (int i = 0; i < HudTimeControls.STEPS; i++) {
            int rgb = image.getRGB(px(129 + 25 * i), py(41)), r = (rgb >> 16) & 255, g = (rgb >> 8) & 255;
            boolean red = r > g + 30, green = g > 120 && g > r;
            if (i >= lit) check(!red && !green, "Speed step " + i + " stays dark");
            else if (redLast && i == HudTimeControls.STEPS - 1) check(red, "The last lit speed step is red like the original");
            else check(green, "Speed step " + i + " is lit green: " + Integer.toHexString(rgb));
        }
    }
    private static void checkDate(BufferedImage image) {
        int bright = 0, rightmost = -1;
        for (int y = py(11); y < py(31); y++) for (int x = px(99); x < px(254); x++) {
            int rgb = image.getRGB(x, y);
            if (((rgb >> 16) & 255) > 200 && ((rgb >> 8) & 255) > 200 && (rgb & 255) > 200) { bright++; rightmost = Math.max(rightmost, x); }
        }
        check(bright > 60, "The campaign date is drawn in the original clock font: " + bright);
        check(rightmost >= px(249) - 4 && rightmost <= px(249), "The date stays right-aligned to the original DateText box: " + rightmost + " vs " + px(249));
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
