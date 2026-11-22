package dev.hoi.client.map.commander;

import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.commander.CommanderBattlePayloads.*;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

public final class CommanderLandBattleWindow extends CommanderBattleWindow {
    static final int WIDTH = 567, HEIGHT = 617;
    private static final String ART = "commander/battle/";
    private static final int ROW = 52, RESERVE = 26, RESERVE_WIDTH = 119, COLUMNS = 2, SHADE = 0xFF170F0F;
    private final Scroll[] committed = {new Scroll(5, ROW), new Scroll(544, ROW)}, reserves = {new Scroll(5, RESERVE), new Scroll(544, RESERVE)};
    private Land land;
    CommanderLandBattleWindow(String id) { super(Kind.LAND, id, WIDTH, HEIGHT, -512, -256); }
    Land land() { return land; }
    @Override boolean accept(Snapshot snapshot) {
        var next = snapshot.land();
        if (!next.token().equals(token())) return true;
        land = next;
        return next.error().isEmpty();
    }
    @Override void render(GuiGraphicsExtractor g, Font font) {
        frame.art(g, ART + "land_bg", 0, 0);
        if (land == null || !land.error().isEmpty()) { closeButton(g, "raids/gfx_closebutton_small/frame_0", 533, 5, 26, 26, ""); return; }
        UiMenuStyle.title(g, font, frame, land.title(), 55, 18, 450, 0, UiMenuStyle.TEXT);
        closeButton(g, "raids/gfx_closebutton_small/frame_0", 533, 5, 26, 26, land.label("CLOSE"));
        frame.art(g, land.terrain(), 77, 37, 413, 70);
        hit(77, 37, 413, 70, land.terrainTooltip());
        progress(g);
        side(g, font, true);
        side(g, font, false);
        frame.art(g, ART + "tactics", 261, 184);
        hit(261, 184, 46, 46, land.label("TACTICS_LIST_DESC"), "ui.click", () -> CommanderBattleHud.tactics(this));
        if (width(font, land.width()) > 28) for (int arrow : new int[]{270, 290}) g.fill(frame.ix(arrow), frame.iy(281), frame.ix(arrow + 8), frame.iy(294), SHADE);
        text(g, font, land.width(), 242, 289, 85, 0);
        hit(263, 271, 43, 38, land.label("COMBAT_COMBAT_WIDTH_DESC"));
    }
    private void progress(GuiGraphicsExtractor g) {
        double progress = land.progress();
        part(g, ART + "progress_attack", 185, 116, 179, 14, 0, progress);
        part(g, ART + "progress_defence", 185, 116, 179, 14, progress, 1);
        frame.art(g, ART + "progress_frame", 183, 114);
        int heading = land.heading();
        frame.art(g, ART + "heading/frame_" + heading, 185 + 179 * progress - (heading < 2 ? 12 : heading == 2 ? 6 : 0), 116);
        hit(183, 114, 185, 20, land.label("COMBAT_PROGRESS_DESC"));
    }
    private void side(GuiGraphicsExtractor g, Font font, boolean attacker) {
        var side = land.side(attacker);
        double face = attacker ? 9 : 481;
        frame.art(g, side.portrait(), face, 112, 78, 105);
        frame.art(g, ART + "leader_frame", attacker ? 5 : 478, 110);
        hit(face, 112, 78, 105, side.leaderTooltip());
        if (side.initiative()) {
            frame.art(g, ART + "initiative/frame_0", attacker ? 4 : 476, 108);
            float glow = (1 - (float)Math.cos(Util.getMillis() / 1000.0 * 1.5 * Math.PI)) / 2;
            UiAssets.drawExact(g, ART + "initiative/frame_1", frame.x(attacker ? 4 : 476), frame.y(108), frame.size(87), frame.size(118), Math.round(glow * 255) << 24 | 0xFFFFFF);
            hit(attacker ? 4 : 476, 205, 87, 21, land.label("LEADER_HAS_INITIATIVE"));
        }
        traits(g, side.traits(), attacker ? 100 : 470, 148, 21, 25, 16, !attacker);
        text(g, font, side.leader(), attacker ? 22 : 355, 245, 190, attacker ? -1 : 1);
        double skill = attacker ? 230 : 304;
        frame.art(g, "commander/commands/land_battle_leader_skill_bg/frame_0", skill, 230);
        text(g, font, side.skill().isEmpty() ? "" : "§e" + side.skill() + "§r", skill + 2, 251, 30, 0);
        hit(skill, 230, 35, 50, side.skillTooltip());
        double arrows = attacker ? 117 : 405, spread = Math.max(0, (width(font, side.width()) - 18) / 2);
        part(g, ART + "width", arrows - spread, 285, 40, 19, 0, .5);
        part(g, ART + "width", arrows + spread, 285, 40, 19, .5, 1);
        text(g, font, side.width(), attacker ? 47 : 335, 295, 180, 0);
        hit(attacker ? 117 : 405, 285, 40, 19, land.label("COMBAT_USED_COMBAT_WIDTH"));
        flag(g, side.country(), attacker ? 90 : 436, 111, side.countryTooltip());
        frame.art(g, "map_detail/flag_frame_thin", attacker ? 87 : 433, 109);
        tactic(g, side.tactic(), attacker ? 100 : 378);
        for (int i = 0; i < side.modifiers().size(); i++) {
            double x = attacker ? 22 + 25 * i : 543 - 25 * (side.modifiers().size() - i);
            frame.art(g, side.modifiers().get(i).image(), x, 252);
            hit(x, 252, 25, 25, side.modifiers().get(i).tooltip());
        }
        if (side.air() != null) air(g, font, side.air(), attacker);
        text(g, font, side.committedLabel(), attacker ? 60 : 343, 320, 165, 0);
        hit(attacker ? 60 : 343, 310, 165, 20, side.committedTooltip());
        text(g, font, side.reservesLabel(), attacker ? 27 : 295, 506, 245, 0);
        hit(attacker ? 87 : 355, 496, 125, 20, land.label("COMBAT_RESERVES"));
        divisions(g, font, side.committed(), attacker);
        reserves(g, font, side.reserves(), attacker);
    }
    private void tactic(GuiGraphicsExtractor g, Tactic tactic, double x) {
        frame.art(g, tactic.image(), x, 183, 89, 47);
        if (tactic.countered()) frame.art(g, ART + "countered", x, 183, 89, 47);
        hit(x, 183, 89, 47, tactic.tooltip());
    }
    private void air(GuiGraphicsExtractor g, Font font, Air air, boolean attacker) {
        frame.art(g, ART + "plane/frame_" + (attacker ? 1 : 0), attacker ? 80 : 454, 40);
        outlined(g, font, air.count(), attacker ? 138 : 385, 48, 60, attacker ? -1 : 1);
        var lines = air.text().split("\n");
        int step = Math.min(line(), 18);
        for (int i = 0; i < lines.length; i++) outlined(g, font, lines[i], attacker ? 83 : 302, 64 + step * i, 180, attacker ? -1 : 1);
        hit(attacker ? 80 : 302, 38, 185, 69, air.tooltip());
    }
    private double rowHeight() { return Math.max(ROW, Math.ceil(24 / frame.scale())); }
    private void divisions(GuiGraphicsExtractor g, Font font, List<Division> rows, boolean attacker) {
        double row = rowHeight(), x = attacker ? 20 : 284;
        var scroll = committed[attacker ? 0 : 1];
        scroll.begin(g, new Box(attacker ? 0 : 285, 325, 280, 169), 4 + (int)Math.ceil(row * rows.size()));
        for (int i = 0; i < rows.size(); i++) division(g, font, rows.get(i), x, 329 + row * i - scroll.offset, row, attacker);
        scroll.end(g);
    }
    private void division(GuiGraphicsExtractor g, Font font, Division division, double x, double y, double row, boolean attacker) {
        nine(g, ART + "unit_bg", x + 10, y, 244, row + 3, 6);
        String icon = CommanderPanels.armyUnitIcon(division.icon());
        double iconX = x + (attacker ? 6 : 213), iconY = y + (row - 40) / 2;
        Runnable select = division.own() ? () -> CommanderUnitHud.selectDivisions(List.of(division.id()), CommanderPanels.shift()) : null;
        hit(x + 10, y, 244, row, division.tooltip("row"), "ui.division.select", select);
        frame.art(g, icon, iconX, iconY, 45.6, 25.2);
        double bars = x + (attacker ? 17 : 225.5);
        bar(g, "bar_org", bars, y + row - 18, division.organization());
        bar(g, "bar_str", bars + 7, y + row - 18, division.strength());
        hit(bars, y + row - 18, 7, 16, division.tooltip("organization"));
        hit(bars + 7, y + row - 18, 7, 16, division.tooltip("strength"));
        double flag = division.flag().isEmpty() ? 0 : 14;
        if (attacker) {
            if (flag > 0) smallFlag(g, division.flag(), x + 48, y + row * .28 - 3.5, 10, 7);
            text(g, font, division.name(), x + 48 + flag, y + row * .28, 132 - flag, -1);
        } else {
            int shown = Math.round(font.width(dev.hoi.client.ui.tooltip.Tooltips.panelText(division.name())) / frame.scale());
            if (flag > 0) smallFlag(g, division.flag(), x + Math.max(84, 214 - shown - flag), y + row * .28 - 3.5, 10, 7);
            text(g, font, division.name(), x + 84 + flag, y + row * .28, 132 - flag, 1);
        }
        for (int i = 0; i < division.modifiers().size(); i++) {
            double px = attacker ? x + 45 + 19 * i : x + 219 - 19 * (i + 1);
            frame.art(g, division.modifiers().get(i).image(), px, y + row - 22);
            hit(px, y + row - 22, 19, 19, division.modifiers().get(i).tooltip());
        }
        String[] names = {"soft", "hard", "guard"}, values = {division.soft(), division.hard(), division.guard()};
        double[] widths = {18, 21, 18}, heights = {13, 12, 10};
        for (int i = 0; i < names.length; i++) {
            double center = y + 1.5 + row * (2 * i + 1) / 6;
            frame.art(g, ART + names[i] + "/frame_" + (attacker ? 0 : 1), attacker ? x + 201 - widths[i] : x + 64, center - heights[i] / 2, widths[i], heights[i]);
            text(g, font, values[i], x + (attacker ? 202 : 14), center, 48, attacker ? 1 : -1);
            hit(x + (attacker ? 180 : 14), center - row / 6, 70, row / 3, division.tooltip(names[i]));
        }
    }
    private void bar(GuiGraphicsExtractor g, String name, double x, double y, double ratio) {
        upright(g, ART + "bar_bg", x, y, 7, 16, 1);
        upright(g, ART + name, x, y, 7, 16, ratio);
    }
    private void reserves(GuiGraphicsExtractor g, Font font, List<Division> rows, boolean attacker) {
        double row = Math.max(RESERVE, Math.ceil(10 / frame.scale())), left = attacker ? 20 : 290;
        var scroll = reserves[attacker ? 0 : 1];
        scroll.begin(g, new Box(attacker ? 1 : 295, 515, 270, 87), (int)Math.ceil(row * ((rows.size() + COLUMNS - 1) / COLUMNS)) + 3);
        for (int i = 0; i < rows.size(); i++) {
            var division = rows.get(i);
            double x = left + RESERVE_WIDTH * (i % COLUMNS), y = 515 + row * (i / COLUMNS) - scroll.offset;
            nine(g, ART + "reserve_bg", x + 10, y, RESERVE_WIDTH, row + 3, 6);
            Runnable select = division.own() ? () -> CommanderUnitHud.selectDivisions(List.of(division.id()), CommanderPanels.shift()) : null;
            hit(x + 10, y, RESERVE_WIDTH, row, division.tooltip("row"), "ui.division.select", select);
            frame.art(g, CommanderPanels.armyUnitIcon(division.icon()), x + 6, y + 2, 45.6, 25.2);
            if (!division.flag().isEmpty()) smallFlag(g, division.flag(), x + 12, y + row - 8, 10, 7);
            text(g, font, division.name(), x + 46, y + row / 2 + 1.5, 57, -1);
            bar(g, "bar_org", x + 104, y + 6, division.organization());
            bar(g, "bar_str", x + 114, y + 6, division.strength());
            hit(x + 104, y + 6, 7, 16, division.tooltip("organization"));
            hit(x + 114, y + 6, 7, 16, division.tooltip("strength"));
        }
        scroll.end(g);
    }
}
