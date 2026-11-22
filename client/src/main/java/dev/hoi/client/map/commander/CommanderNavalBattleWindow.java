package dev.hoi.client.map.commander;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.protocol.commander.CommanderBattlePayloads.*;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CommanderNavalBattleWindow extends CommanderBattleWindow {
    static final int WIDTH = 1056, HEIGHT = 736;
    private static final String ART = "commander/battle/naval/";
    private static final String[] STATS = {"depth", "anti_air", "torpedo", "heavy", "light", "positioning"}, SKILLS = {"attack", "defense", "maneuvering", "coordination"};
    private static final double[] LEFT_STAT = {149, 188, 229, 269, 309, 352}, RIGHT_STAT = {883, 843, 803, 763, 723, 680}, LEFT_SKILL = {217, 237, 257, 277}, RIGHT_SKILL = {811, 791, 771, 749}, LEFT_WIDE = {217, 244, 271, 298}, RIGHT_WIDE = {811, 784, 757, 730};
    private static final String[] LANES = {"outside", "carrier", "battle", "screen", "hidden", "visible"};
    private static final double[] LEFT_LANE = {71, 194, 334, 466, 194, 466}, RIGHT_LANE = {986, 862, 722, 588, 862, 588}, LANE_TOP = {270, 272, 272, 272, 535, 535};
    private static final int SUNK_COLUMNS = 3;
    private final Scroll[] sunk = {new Scroll(130, 27), new Scroll(848, 27)};
    private Naval naval;
    CommanderNavalBattleWindow(String id) { super(Kind.NAVAL, id, WIDTH, HEIGHT, -528, -356); }
    Naval naval() { return naval; }
    @Override boolean accept(Snapshot snapshot) {
        var next = snapshot.naval();
        if (!next.token().equals(token())) return true;
        naval = next;
        return next.error().isEmpty();
    }
    @Override boolean contains(double x, double y) {
        return frame != null && (gui(new Box(127, 0, 802, HEIGHT)).contains(x, y) || gui(new Box(6, 238, 1044, 388)).contains(x, y));
    }
    @Override void render(GuiGraphicsExtractor g, Font font) {
        frame.art(g, ART + "bg", 0, 0);
        if (naval == null || !naval.error().isEmpty()) { closeButton(g, "menu/close", 727, 11, 32, 33, ""); return; }
        frame.art(g, ART + "weather_none", 323, 55);
        hit(323, 55, 486, 63, naval.conditions());
        UiMenuStyle.title(g, font, frame, naval.title(), 335, 27, 360, 0, UiMenuStyle.TEXT);
        if (!naval.weather().isEmpty()) { frame.art(g, naval.weather(), 697, 5); hit(703, 14, 27, 27, naval.conditions()); }
        closeButton(g, "menu/close", 727, 11, 32, 33, naval.label("CLOSE"));
        progress(g);
        frame.art(g, ART + "front", 133, 251);
        side(g, font, true);
        side(g, font, false);
        if (naval.disengage()) {
            frame.art(g, ART + "disengage", 512, 155);
            hit(512, 155, 31, 31, naval.disengageTooltip(), "ui.click", () -> send(Action.DISENGAGE));
        }
    }
    private void progress(GuiGraphicsExtractor g) {
        part(g, ART + (naval.attacking() ? "progress_loose" : "progress_win"), 352, 134, 351, 14, 0, naval.progress());
        part(g, ART + (naval.attacking() ? "progress_win" : "progress_loose"), 352, 134, 351, 14, naval.progress(), 1);
        hit(352, 134, 351, 14, naval.label("COMBAT_PROGRESS_DESC"));
    }
    private void side(GuiGraphicsExtractor g, Font font, boolean attacker) {
        var fleet = naval.fleet(attacker);
        double face = attacker ? 145 : 840;
        frame.art(g, fleet.portrait(), face, 59, 70.2, 94.5);
        frame.art(g, ART + "leader_frame", attacker ? 140 : 835, 53);
        hit(attacker ? 140 : 835, 53, 81, 107, fleet.leaderTooltip());
        boolean wide = fleet.skills().stream().anyMatch(skill -> skill.length() > 1);
        for (int i = 0; i < fleet.skills().size(); i++) {
            double x = (wide ? attacker ? LEFT_WIDE : RIGHT_WIDE : attacker ? LEFT_SKILL : RIGHT_SKILL)[i];
            frame.art(g, "commander/commands/trait_" + SKILLS[i] + "/frame_0", x, 57);
            outlined(g, font, fleet.skills().get(i), x - 2, 90, 30, 0);
            hit(x, 57, 27, 42, fleet.statTooltips().getOrDefault("skill_" + i, ""));
        }
        traits(g, fleet.traits(), attacker ? 225 : 830, 130, 24, 32, 6, !attacker);
        text(g, font, fleet.leader(), attacker ? 149 : 721, 177, 188, attacker ? -1 : 1);
        flag(g, fleet.country(), attacker ? 344 : 672, 162, fleet.countryTooltip());
        frame.art(g, "map_detail/flag_frame", attacker ? 339 : 667, 158);
        for (int i = 0; i < STATS.length; i++) {
            double x = attacker ? LEFT_STAT[i] : RIGHT_STAT[i], y = STATS[i].equals("positioning") ? 194 : 196;
            frame.art(g, ART + "stat/frame_" + (5 - i), x, y);
            text(g, font, fleet.stats().getOrDefault(STATS[i], ""), x - 8, 226, 42, 0);
            hit(x - 6, 194, 38, 46, fleet.statTooltips().getOrDefault(STATS[i], ""));
        }
        screening(g, font, attacker ? 375 : 618, attacker ? 3 : 0, attacker ? 375 : 634, fleet.capitalScreening());
        screening(g, font, attacker ? 230 : 764, attacker ? 2 : 1, attacker ? 230 : 779, fleet.carrierScreening());
        column(attacker ? 406 : 528, 122, "SCREENING_BOX_TOOLTIP");
        column(attacker ? 260 : 650, 146, "CAPITAL_BOX_TOOLTIP");
        column(attacker ? 142 : 796, 118, "CARRIER_BOX_TOOLTIP");
        hit(attacker ? 142 : 528, 520, 386, 82, naval.label("SUBMARINE_BOX_TOOLTIP"));
        for (int i = 0; i < LANES.length; i++) {
            var ships = fleet.lane(LANES[i]);
            for (int n = 0; n < ships.size(); n++) entry(g, font, ships.get(n), attacker ? LEFT_LANE[i] : RIGHT_LANE[i], LANE_TOP[i] + 21 * n, attacker, false);
        }
        sunk(g, font, fleet.lane("sunk"), attacker);
    }
    private void screening(GuiGraphicsExtractor g, Font font, double x, int art, double textX, String value) {
        frame.art(g, ART + "screening/frame_" + art, x, 237);
        text(g, font, value, textX, 251, 48, 0);
        hit(x, 237, 62, 27, naval.label("screening_efficiency_info"));
    }
    private void column(double x, double w, String key) { hit(x, 264, w, 256, naval.label(key)); }
    private void entry(GuiGraphicsExtractor g, Font font, Ships ships, double center, double y, boolean attacker, boolean sunk) {
        String art = ships.image().replaceAll("frame_[0-9]+$", "frame_" + (attacker ? 1 : 0));
        int[] size = UiAssets.imageSize(art);
        double x = center - size[0] / 2.0;
        if (!sunk) {
            bar(g, "bar_str", center - 16.5, y + 1, ships.strength());
            bar(g, "bar_org", center + .5, y + 1, ships.organization());
        }
        frame.art(g, art, x, y);
        if (ships.hidden()) frame.art(g, ART + "hidden", center - 10.5, y + 1);
        String count = Integer.toString(ships.count());
        double flag = attacker ? x - (sunk ? 5 : 8.5) : x + size[0] - 4, digits = width(font, count) + 1;
        smallFlag(g, ships.flag(), flag, y + 5, 10, 7);
        outlined(g, font, count, attacker ? x + size[0] + 1 : x - 2 - digits, y + 9, digits, attacker ? -1 : 1);
        hit(x - (attacker ? 9 : digits + 2), y, size[0] + digits + 14, 20, ships.tooltip());
    }
    private void bar(GuiGraphicsExtractor g, String name, double x, double y, double ratio) {
        frame.art(g, ART + "bar_bg", x, y, 17, 6);
        part(g, ART + name, x, y, 17, 6, 0, ratio);
    }
    private void sunk(GuiGraphicsExtractor g, Font font, List<Ships> ships, boolean attacker) {
        var scroll = sunk[attacker ? 0 : 1];
        int rows = (ships.size() + SUNK_COLUMNS - 1) / SUNK_COLUMNS;
        scroll.begin(g, new Box(attacker ? 146 : 534, 632, attacker ? 376 : 378, 82), 27 * rows + 2);
        for (int i = 0; i < ships.size(); i++) {
            int column = i % SUNK_COLUMNS, row = i / SUNK_COLUMNS, inRow = Math.min(SUNK_COLUMNS, ships.size() - row * SUNK_COLUMNS);
            int[] size = UiAssets.imageSize(ships.get(i).image().replaceAll("frame_[0-9]+$", "frame_" + (attacker ? 1 : 0)));
            double left = attacker ? 426 - 100 * (inRow - 1 - column) : 566 + 100 * column;
            entry(g, font, ships.get(i), left + size[0] / 2.0, 634 + 27 * row - scroll.offset, attacker, true);
        }
        scroll.end(g);
    }
}
