package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.protocol.commander.CommanderBattlePayloads.*;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.menu.MenuView;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class CommanderBattleTest {
    private static final String TOKEN = "11111111-2222-3333-4444-555555555555";
    private static final List<Request> requests = new ArrayList<>();
    private static final List<String> countries = new ArrayList<>();
    private CommanderBattleTest() {}
    public static void run(ClientGameTestContext game, MenuView menu) {
        String land = read("kor-land-battle.json"), naval = read("kor-naval-battle.json"), result = read("kor-naval-result.json");
        for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
            String suffix = "-" + size[0] + "x" + size[1] + "-gui2";
            game.getInput().resizeWindow(size[0], size[1]);
            game.runOnClient(client -> {
                client.options.guiScale().set(2);
                ClientCampaignHud.accept(HudPayloads.State.of("KOR", menu.hud()));
                CommanderHud.reset(); CommanderUnitHud.reset();
                requests.clear(); countries.clear();
                CommanderBattleHud.requestObserver = requests::add;
                CommanderBattleWindow.country = countries::add;
            });
            game.setScreen(MapCursor::new);
            away(game, size);
            markers(game, land, naval, result, suffix);
            land(game, land, size, suffix);
            naval(game, naval, size, suffix);
            result(game, result, size, suffix);
            game.runOnClient(client -> {
                CommanderBattleHud.open(Kind.LAND, "first"); CommanderBattleHud.open(Kind.NAVAL, "second");
                CommanderHud.reset();
                check(CommanderBattleHud.window(Kind.LAND).isEmpty() && CommanderBattleHud.window(Kind.NAVAL).isEmpty() && requests.getLast().action() == Action.CLOSE, "Resetting the command HUD closes every battle window");
                CommanderBattleHud.requestObserver = null; CommanderBattleWindow.country = HoiClient::openCountry;
                CommanderBattleMarkers.accept(null); CommanderUnitHud.reset();
            });
        }
        game.setScreen(() -> null);
    }
    private static String read(String name) {
        String directory = Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW"), "Authoritative commander fixtures are required for the battle windows");
        try { return Files.readString(Path.of(directory, name)); } catch (IOException error) { throw new UncheckedIOException(error); }
    }
    private static Marker marker(Kind kind, String id, double side, double up, double headingX, double headingZ, String value, Tone tone, boolean urban, int frame, String tooltip) {
        var client = Minecraft.getInstance();
        var camera = client.gameRenderer.mainCamera();
        var forward = camera.forwardVector(); var left = camera.leftVector(); var above = camera.upVector();
        return new Marker(id, kind, camera.position().x + forward.x() * 30 - left.x() * side + above.x() * up, camera.position().y + forward.y() * 30 - left.y() * side + above.y() * up - 3.1, camera.position().z + forward.z() * 30 - left.z() * side + above.z() * up, headingX, headingZ, value, tone, urban, frame, tooltip);
    }
    private static void markers(ClientGameTestContext game, String land, String naval, String result, String suffix) {
        game.runOnClient(client -> {
            var battle = new Snapshot(Kind.LAND, land).land(); var sea = new Snapshot(Kind.NAVAL, naval).naval(); var report = new Snapshot(Kind.RESULT, result).result();
            var camera = client.gameRenderer.mainCamera(); var left = camera.leftVector();
            double length = Math.hypot(left.x(), left.z()), headingX = -left.x() / length, headingZ = -left.z() / length;
            CommanderBattleMarkers.accept(new Markers(client.level.dimension().identifier().toString(), List.of(
                    marker(Kind.LAND, battle.id(), -8, 6, headingX, headingZ, "93", Tone.WINNING, false, 0, "§e서울§r\n공격 측: §e2§r개 사단(대한민국)"),
                    marker(Kind.LAND, "urban", -4, 6, -headingX, -headingZ, "32", Tone.LOSING, true, 0, "도시"),
                    marker(Kind.LAND, "air", 0, 6, 0, 0, "", Tone.BALANCED, false, 3, "공중"),
                    marker(Kind.NAVAL, sea.id(), 5, 6, 0, 0, "27", Tone.WINNING, false, 1, sea.title()),
                    marker(Kind.RESULT, report.id(), -8, 2, 0, 0, "", Tone.WINNING, false, 1, report.title()),
                    marker(Kind.RESULT, "older", -8, 2, 0, 0, "", Tone.LOSING, false, 0, "이전 전투"))));
        });
        game.waitTicks(3);
        game.takeScreenshot("hoi-battle-markers" + suffix);
        game.runOnClient(client -> {
            var placed = CommanderBattleMarkers.placed();
            check(placed.size() == 6, "Every battle in view has its icon: " + placed.size());
            for (int i = 0; i < placed.size(); i++) for (int n = i + 1; n < placed.size(); n++) check(!placed.get(i).contains(placed.get(n).x() + placed.get(n).w() / 2, placed.get(n).y() + placed.get(n).h() / 2), "The battle icons can be pressed separately: " + placed.get(i) + " " + placed.get(n));
            for (var kind : Kind.values()) {
                var icon = placed.stream().filter(p -> p.marker().kind() == kind).findFirst().orElseThrow();
                press(client.gui.screen(), icon.x() + icon.w() / 2, icon.y() + icon.h() / 2);
                check(requests.getLast().action() == Action.OPEN && requests.getLast().kind() == kind && requests.getLast().id().equals(icon.marker().id()) && CommanderBattleHud.window(kind).isPresent(), "The icon opens its window: " + requests.getLast());
                client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                check(CommanderBattleHud.window(kind).isEmpty() && client.gui.screen() instanceof MapCursor && requests.getLast().action() == Action.CLOSE, "Escape closes the battle window first");
            }
            var icon = placed.stream().filter(p -> p.marker().kind() == Kind.RESULT).findFirst().orElseThrow();
            var right = new MouseButtonEvent(icon.x() + icon.w() / 2, icon.y() + icon.h() / 2, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0));
            check(client.gui.screen().mouseClicked(right, false) && requests.getLast().action() == Action.DISMISS_ALL, "Right clicking a result dismisses every result: " + requests.getLast());
            client.gui.screen().mouseReleased(right);
            CommanderBattleMarkers.accept(null);
        });
        game.waitTicks(2);
        game.runOnClient(client -> check(CommanderBattleMarkers.placed().isEmpty(), "Cleared battles leave the map"));
    }
    private static void land(ClientGameTestContext game, String json, int[] size, String suffix) {
        game.runOnClient(client -> {
            CommanderBattleHud.open(Kind.LAND, new Snapshot(Kind.LAND, json).land().id());
            CommanderBattleHud.accept(new Snapshot(Kind.LAND, json));
            check(landWindow().land() == null, "Another session's snapshot is ignored");
            CommanderBattleHud.accept(reply(Kind.LAND, json));
            check(landWindow().land() != null, "The window accepts its own snapshot");
        });
        away(game, size);
        game.takeScreenshot("hoi-land-battle" + suffix);
        game.runOnClient(client -> {
            var land = landWindow().land(); var frame = landWindow().frame();
            onScreen(client, frame, CommanderLandBattleWindow.WIDTH, CommanderLandBattleWindow.HEIGHT);
            check(CommanderBattleHud.covers(frame.x(300), frame.y(300)) && ((MapCursor)client.gui.screen()).covers(frame.x(300), frame.y(300)), "The battle window covers the map below it");
            for (boolean attacker : new boolean[]{true, false}) {
                var side = land.side(attacker); var first = side.committed().getFirst(); double x = attacker ? 20 : 284;
                tip(landWindow(), side.countryTooltip(), attacker ? 110 : 456, 124); tip(landWindow(), side.leaderTooltip(), attacker ? 45 : 515, 160);
                tip(landWindow(), side.tactic().tooltip(), attacker ? 140 : 420, 205); tip(landWindow(), side.committedTooltip(), attacker ? 140 : 420, 320);
                tip(landWindow(), land.label("COMBAT_RESERVES"), attacker ? 150 : 420, 506); tip(landWindow(), land.label("COMBAT_USED_COMBAT_WIDTH"), attacker ? 120 : 408, 295);
                tip(landWindow(), first.tooltip("row"), x + 110, 333); tip(landWindow(), first.tooltip("organization"), x + (attacker ? 20 : 229), 329 + 42); tip(landWindow(), first.tooltip("strength"), x + (attacker ? 27 : 236), 329 + 42);
                tip(landWindow(), first.tooltip("soft"), x + (attacker ? 215 : 30), 329 + 10); tip(landWindow(), first.tooltip("hard"), x + (attacker ? 215 : 30), 329 + 27); tip(landWindow(), first.tooltip("guard"), x + (attacker ? 215 : 30), 329 + 45);
                if (!first.modifiers().isEmpty()) tip(landWindow(), first.modifiers().getFirst().tooltip(), x + (attacker ? 45 + 9 : 219 - 10), 329 + 40);
                for (int i = 0; i < side.traits().size(); i++) tip(landWindow(), side.traits().get(i).tooltip(), attacker ? 110 + 21 * i : 460 - 21 * (side.traits().size() - 1 - i), 155);
                for (int i = 0; i < side.modifiers().size(); i++) tip(landWindow(), side.modifiers().get(i).tooltip(), attacker ? 34 + 25 * i : 543 - 25 * (side.modifiers().size() - i) + 12, 264);
                if (side.air() != null) tip(landWindow(), side.air().tooltip(), attacker ? 160 : 400, 90);
                if (!side.skillTooltip().isEmpty()) tip(landWindow(), side.skillTooltip(), attacker ? 247 : 321, 260);
            }
            tip(landWindow(), land.terrainTooltip(), 285, 70); tip(landWindow(), land.label("COMBAT_PROGRESS_DESC"), 275, 123); tip(landWindow(), land.label("COMBAT_COMBAT_WIDTH_DESC"), 284, 289); tip(landWindow(), land.label("CLOSE"), 546, 18);
            press(client.gui.screen(), frame.x(110), frame.y(124)); press(client.gui.screen(), frame.x(456), frame.y(124));
            check(countries.equals(List.of(land.attacker().country(), land.defender().country())), "The flags open diplomacy: " + countries);
            press(client.gui.screen(), frame.x(284), frame.y(206));
            check(CommanderBattleHud.tactics().isPresent(), "The tactics button opens the list");
        });
        hover(game, landWindow(), 130, 345);
        game.takeScreenshot("hoi-land-battle-division-tooltip" + suffix);
        hover(game, landWindow(), 285, 70);
        game.takeScreenshot("hoi-land-battle-terrain-tooltip" + suffix);
        game.runOnClient(client -> {
            var tactics = CommanderBattleHud.tactics().orElseThrow(); var frame = tactics.frame(); var land = landWindow().land();
            onScreen(client, frame, CommanderBattleTactics.WIDTH, CommanderBattleTactics.HEIGHT);
            tip(tactics, land.tactics().getFirst().tooltip(), 100, 100); tip(tactics, land.label("CLOSE"), 374, 18); tip(tactics, land.label("TACTICS_CURRENT_PHASE_DESC"), 180, 52);
            client.gui.screen().mouseScrolled(frame.x(100), frame.y(300), 0, -200);
        });
        game.waitTicks(2);
        game.takeScreenshot("hoi-land-battle-tactics" + suffix);
        game.runOnClient(client -> {
            var tactics = CommanderBattleHud.tactics().orElseThrow(); var frame = tactics.frame();
            tip(tactics, landWindow().land().tactics().getLast().tooltip(), 100, CommanderBattleTactics.HEIGHT - 30);
            client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            check(CommanderBattleHud.tactics().isEmpty() && CommanderBattleHud.window(Kind.LAND).isPresent(), "Escape closes the list before the battle");
            CommanderBattleHud.accept(new Snapshot(Kind.LAND, crowded(json)));
            press(client.gui.screen(), landWindow().frame().x(284), landWindow().frame().y(206)); press(client.gui.screen(), landWindow().frame().x(284), landWindow().frame().y(206));
            check(CommanderBattleHud.tactics().isEmpty(), "Pressing the button again closes the list");
        });
        away(game, size);
        game.takeScreenshot("hoi-land-battle-crowded" + suffix);
        game.runOnClient(client -> {
            var land = landWindow().land(); var frame = landWindow().frame();
            for (boolean attacker : new boolean[]{true, false}) {
                client.gui.screen().mouseScrolled(frame.x(attacker ? 150 : 400), frame.y(400), 0, -200);
                client.gui.screen().mouseScrolled(frame.x(attacker ? 150 : 400), frame.y(550), 0, -200);
            }
        });
        game.waitTicks(2);
        game.takeScreenshot("hoi-land-battle-scrolled" + suffix);
        game.runOnClient(client -> {
            var land = landWindow().land(); var frame = landWindow().frame();
            tip(landWindow(), land.attacker().committed().getLast().tooltip("row"), 130, 452); tip(landWindow(), land.defender().committed().getLast().tooltip("row"), 400, 452);
            double left = frame.x(0);
            var down = mouse(frame.x(300), frame.y(18));
            client.gui.screen().mouseClicked(down, false);
            client.gui.screen().mouseDragged(new MouseButtonEvent(down.x() - 20, down.y(), down.buttonInfo()), -20, 0);
            client.gui.screen().mouseReleased(down);
            check(Math.abs(landWindow().frame().x(0) - Math.max(0, left - 20)) < 1, "Dragging the title moves the window: " + left + " -> " + landWindow().frame().x(0));
            CommanderBattleHud.accept(new Snapshot(Kind.LAND, ended(json)));
            check(CommanderBattleHud.window(Kind.LAND).isEmpty(), "The end of the battle closes the window");
        });
    }
    private static void naval(ClientGameTestContext game, String json, int[] size, String suffix) {
        game.runOnClient(client -> {
            CommanderBattleHud.open(Kind.NAVAL, new Snapshot(Kind.NAVAL, json).naval().id());
            CommanderBattleHud.accept(reply(Kind.NAVAL, json));
            check(navalWindow().naval() != null, "The naval window accepts its own snapshot");
        });
        away(game, size);
        game.takeScreenshot("hoi-naval-battle" + suffix);
        game.runOnClient(client -> {
            var naval = navalWindow().naval(); var frame = navalWindow().frame();
            onScreen(client, frame, CommanderNavalBattleWindow.WIDTH, CommanderNavalBattleWindow.HEIGHT);
            check(!CommanderBattleHud.covers(frame.x(40), frame.y(60)) && CommanderBattleHud.covers(frame.x(40), frame.y(400)) && CommanderBattleHud.covers(frame.x(400), frame.y(60)), "Only the drawn parts of the naval window cover the map");
            tip(navalWindow(), naval.conditions(), 560, 80); tip(navalWindow(), naval.conditions(), 716, 27); tip(navalWindow(), naval.label("CLOSE"), 743, 27); tip(navalWindow(), naval.label("COMBAT_PROGRESS_DESC"), 527, 141); tip(navalWindow(), naval.disengageTooltip(), 527, 170);
            for (boolean attacker : new boolean[]{true, false}) {
                var fleet = naval.fleet(attacker);
                tip(navalWindow(), fleet.countryTooltip(), attacker ? 360 : 690, 175);
                if (!fleet.leaderTooltip().isEmpty()) tip(navalWindow(), fleet.leaderTooltip(), attacker ? 180 : 870, 100);
                for (int i = 0; i < fleet.skills().size(); i++) tip(navalWindow(), fleet.statTooltips().get("skill_" + i), new double[][]{{230, 250, 270, 290}, {824, 804, 784, 762}}[attacker ? 0 : 1][i], 70);
                tip(navalWindow(), fleet.statTooltips().get("positioning"), attacker ? 364 : 692, 206); tip(navalWindow(), fleet.statTooltips().get("depth"), attacker ? 161 : 895, 206);
                tip(navalWindow(), naval.label("screening_efficiency_info"), attacker ? 405 : 640, 245); tip(navalWindow(), naval.label("SCREENING_BOX_TOOLTIP"), attacker ? 500 : 620, 500); tip(navalWindow(), naval.label("SUBMARINE_BOX_TOOLTIP"), attacker ? 330 : 700, 595);
                for (var lane : Map.of("screen", attacker ? 466 : 588, "visible", attacker ? 466 : 588, "hidden", attacker ? 194 : 862, "carrier", attacker ? 194 : 862).entrySet())
                    if (!fleet.lane(lane.getKey()).isEmpty()) tip(navalWindow(), fleet.lane(lane.getKey()).getFirst().tooltip(), lane.getValue(), List.of("visible", "hidden").contains(lane.getKey()) ? 545 : 282);
            }
            press(client.gui.screen(), frame.x(527), frame.y(170));
            check(requests.getLast().action() == Action.DISENGAGE && requests.getLast().token().equals(navalWindow().token()), "The withdrawal button orders the fleet out of the battle: " + requests.getLast());
        });
        hover(game, navalWindow(), 560, 80);
        game.takeScreenshot("hoi-naval-battle-conditions-tooltip" + suffix);
        hover(game, navalWindow(), 466, 282);
        game.takeScreenshot("hoi-naval-battle-ships-tooltip" + suffix);
        game.runOnClient(client -> CommanderBattleHud.accept(new Snapshot(Kind.NAVAL, lanes(json))));
        away(game, size);
        game.takeScreenshot("hoi-naval-battle-lanes" + suffix);
        game.runOnClient(client -> {
            press(client.gui.screen(), navalWindow().frame().x(743), navalWindow().frame().y(27));
            check(CommanderBattleHud.window(Kind.NAVAL).isEmpty() && requests.getLast().action() == Action.CLOSE, "The close button closes the naval window");
        });
    }
    private static void result(ClientGameTestContext game, String json, int[] size, String suffix) {
        game.runOnClient(client -> {
            CommanderBattleHud.open(Kind.RESULT, new Snapshot(Kind.RESULT, json).result().id());
            CommanderBattleHud.accept(reply(Kind.RESULT, json));
            check(resultWindow().result() != null, "The results accept their own snapshot");
        });
        away(game, size);
        game.takeScreenshot("hoi-naval-result" + suffix);
        game.runOnClient(client -> {
            var result = resultWindow().result(); var frame = resultWindow().frame();
            onScreen(client, frame, CommanderNavalResultWindow.WIDTH, CommanderNavalResultWindow.HEIGHT);
            tip(resultWindow(), result.label("CLOSE"), 569, 27); tip(resultWindow(), result.label("DETAILS"), 145, 492); tip(resultWindow(), result.label("BUTTON_DISMISS"), 355, 491);
            tip(resultWindow(), result.attacker().survivors().getFirst().tooltip(), 157, 104); tip(resultWindow(), result.defender().survivors().getFirst().tooltip(), 567 - 34 * result.defender().survivors().size() + 17, 104);
            tip(resultWindow(), result.attacker().lost().getFirst().tooltip(), 196, 164); tip(resultWindow(), result.defender().lost().getFirst().tooltip(), 508, 164);
            tip(resultWindow(), result.attacker().lost().getFirst().losses().getFirst().tooltip(), 261, 162); tip(resultWindow(), result.defender().lost().getFirst().losses().getFirst().tooltip(), 449, 162);
            press(client.gui.screen(), frame.x(145), frame.y(492));
            check(CommanderBattleHud.covers(frame.x(-100), frame.y(200)) && CommanderBattleHud.covers(frame.x(800), frame.y(200)), "The details open on both sides");
        });
        game.waitTicks(3);
        game.takeScreenshot("hoi-naval-result-details" + suffix);
        game.runOnClient(client -> {
            press(client.gui.screen(), resultWindow().frame().x(355), resultWindow().frame().y(491));
            check(CommanderBattleHud.window(Kind.RESULT).isEmpty() && requests.get(requests.size() - 2).action() == Action.DISMISS && requests.getLast().action() == Action.CLOSE, "Dismissing closes the results and removes the icon");
        });
    }
    private static CommanderLandBattleWindow landWindow() { return (CommanderLandBattleWindow)CommanderBattleHud.window(Kind.LAND).orElseThrow(); }
    private static CommanderNavalBattleWindow navalWindow() { return (CommanderNavalBattleWindow)CommanderBattleHud.window(Kind.NAVAL).orElseThrow(); }
    private static CommanderNavalResultWindow resultWindow() { return (CommanderNavalResultWindow)CommanderBattleHud.window(Kind.RESULT).orElseThrow(); }
    private static Snapshot reply(Kind kind, String json) { return new Snapshot(kind, own(json)); }
    private static String own(String json) { return json.replace(TOKEN, requests.getLast().token()); }
    private static String ended(String json) {
        var land = new Snapshot(Kind.LAND, own(json)).land();
        return Snapshot.of(new Land(landWindow().token(), land.id(), "", "", "", .5, 2, "", null, null, "", List.of(), Map.of(), "전투가 끝났습니다.")).json();
    }
    private static Side crowded(Side side, boolean attacker) {
        var committed = new ArrayList<Division>(); var reserves = new ArrayList<Division>();
        for (int i = 0; i < 9; i++) {
            var base = side.committed().get(i % side.committed().size());
            var icons = new ArrayList<Icon>();
            for (int n = 0; n < 6; n++) icons.add(new Icon(CommanderBattleTest.MODIFIER + (n == 0 ? 0 : n == 1 ? 1 : n * 2), "보정 " + n));
            committed.add(new Division("c" + i, base.name(), attacker && i % 2 == 0 ? "" : "JAP", base.icon(), false, 1 - i / 10.0, i / 10.0 + .1, Integer.toString(37 * i + 5), Integer.toString(211 * i), Integer.toString(17 + 133 * i), icons, Map.of("row", "사단 " + i)));
            reserves.add(new Division("r" + i, base.name(), attacker ? "" : "JAP", base.icon(), false, .5, .8, "", "", "", List.of(), Map.of("row", "예비대 " + i)));
        }
        var modifiers = List.of(new Icon("commander/battle/side_modifier/frame_0", "요새"), new Icon("commander/battle/side_modifier/frame_1", "적의 공중 우세"), new Icon("commander/battle/side_modifier/frame_5", "중첩"), new Icon("commander/battle/side_modifier/frame_7", "정보력"));
        return new Side(side.country(), side.countryTooltip(), side.leader(), side.portrait(), side.leaderTooltip(), side.skill(), side.skillTooltip(), side.traits(), attacker, side.headquarters(), new Tactic(side.tactic().image(), side.tactic().tooltip(), !attacker), side.width(), modifiers, side.air() == null ? new Air("3", "격추당한 총수: §c1§r\n£organization_texticon 피해 §c12§r\n£strenght_texticon 피해: §c3§r", "항공") : side.air(), "9 사단", side.committedTooltip(), committed, "9 예비대", reserves);
    }
    private static final String MODIFIER = "commander/battle/division_modifier/frame_";
    private static String crowded(String json) {
        var land = new Snapshot(Kind.LAND, json).land();
        return Snapshot.of(new Land(landWindow().token(), land.id(), land.title(), land.terrain(), land.terrainTooltip(), .93, 0, land.width(), crowded(land.attacker(), true), crowded(land.defender(), false), land.phase(), land.tactics(), land.labels(), "")).json();
    }
    private static Ships ships(String hull, String flag, int count, double organization, double strength, boolean hidden) { return new Ships("commander/commands/navalcombat_ship_icon_" + hull + "/frame_0", flag, count, organization, strength, hidden, hull + " " + count); }
    private static Fleet lanes(Fleet fleet, boolean attacker) {
        String flag = fleet.country();
        var lanes = new TreeMap<String, List<Ships>>();
        if (attacker) lanes.put("outside", List.of(ships("destroyer", flag, 4, .9, .7, false), ships("heavy_cruiser", flag, 2, .8, .5, false)));
        lanes.put("carrier", List.of(ships("carrier", flag, 1, 1, 1, false)));
        lanes.put("battle", List.of(ships("battle_cruiser", flag, 2, .6, .4, false), ships("heavy_cruiser", flag, 12, .3, .9, false)));
        lanes.put("screen", List.of(ships("destroyer", flag, 12, 1, .8, false), ships("light_cruiser", "JAP", 1, .5, .5, false)));
        lanes.put("hidden", List.of(ships("submarine", flag, 28, 1, 1, true)));
        lanes.put("visible", List.of(ships("submarine", flag, 7, .9, .9, false)));
        lanes.put("sunk", List.of(ships("submarine", flag, 2, 0, 0, false), ships("light_cruiser", "JAP", 2, 0, 0, false), ships("destroyer", flag, 3, 0, 0, false), ships("heavy_cruiser", flag, 1, 0, 0, false)));
        return new Fleet(flag, fleet.countryTooltip(), fleet.leader(), fleet.portrait(), fleet.leaderTooltip(), List.of("2", "1", "10", "3"), fleet.traits(), fleet.stats(), fleet.statTooltips(), "66%", "0%", lanes);
    }
    private static String lanes(String json) {
        var naval = new Snapshot(Kind.NAVAL, json).naval();
        return Snapshot.of(new Naval(navalWindow().token(), naval.id(), naval.title(), naval.weather(), naval.conditions(), .09, false, true, naval.disengageTooltip(), lanes(naval.attacker(), true), lanes(naval.defender(), false), naval.labels(), "")).json();
    }
    private static void onScreen(Minecraft client, dev.hoi.client.ui.UiMenuStyle.Frame frame, int width, int height) {
        int screen = client.getWindow().getGuiScaledWidth(), tall = client.getWindow().getGuiScaledHeight();
        check(frame.x(0) >= -.5 && frame.x(width) <= screen + .5 && frame.y(0) >= HudMenuBar.height(screen) - .5 && frame.y(height) <= tall + .5, "The window stays on the screen below the menu: " + frame);
    }
    private static void tip(CommanderBattleWindow window, String expected, double x, double y) {
        String shown = window.tooltip(window.frame().x(x), window.frame().y(y));
        check(expected.equals(shown), "The original tooltip is shown at " + x + "," + y + ": " + shown + " instead of " + expected);
    }
    private static void away(ClientGameTestContext game, int[] size) { game.getInput().setCursorPos(size[0] - 30, size[1] - 10); game.waitTicks(3); }
    private static void hover(ClientGameTestContext game, CommanderBattleWindow window, double x, double y) {
        double[] point = game.computeOnClient(client -> new double[]{window.frame().x(x) * client.getWindow().getGuiScale(), window.frame().y(y) * client.getWindow().getGuiScale()});
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
