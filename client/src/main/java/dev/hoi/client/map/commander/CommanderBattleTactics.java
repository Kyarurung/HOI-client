package dev.hoi.client.map.commander;

import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.protocol.commander.CommanderBattlePayloads.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CommanderBattleTactics extends CommanderBattleWindow {
    static final int WIDTH = 392, HEIGHT = 613;
    private static final int ENTRY = 94, TOP = 68;
    private static final String[] ICONS = {"attack", "defend", "width", "speed"};
    private static final double[] ICON_X = {115, 210, 108, 210}, ICON_Y = {33, 33, 61, 61}, VALUE_X = {140, 235, 140, 235};
    private final CommanderLandBattleWindow battle;
    private final Scroll list = new Scroll(366, ENTRY / 2);
    CommanderBattleTactics(CommanderLandBattleWindow battle) {
        super(Kind.LAND, battle.id(), WIDTH, HEIGHT, 0, 0);
        this.battle = battle;
        var frame = battle.frame();
        place(frame.x(CommanderLandBattleWindow.WIDTH + 4), frame.y(0));
    }
    @Override void close() {}
    @Override void render(GuiGraphicsExtractor g, Font font) {
        var land = battle.land();
        frame.tile(g, "commander/commands/tiled_window_1b_border/frame_0", 0, 0, WIDTH, HEIGHT, 64, UiMenuStyle.TILE);
        closeButton(g, "raids/gfx_closebutton_small/frame_0", 361, 5, 26, 26, land == null ? "" : land.label("CLOSE"));
        if (land == null) return;
        UiMenuStyle.title(g, font, frame, land.label("TACTICS_LIST_VIEW_TITLE"), 14, 18, 340, 0, UiMenuStyle.TEXT);
        text(g, font, land.phase(), 14, 52, 340, 0);
        hit(14, 42, 340, 20, land.label("TACTICS_CURRENT_PHASE_DESC"));
        var rows = land.tactics();
        list.begin(g, new Box(13, TOP, 352, HEIGHT - TOP - 14), ENTRY * rows.size());
        for (int i = 0; i < rows.size(); i++) row(g, font, rows.get(i), TOP + ENTRY * i - list.offset);
        list.end(g);
    }
    private void row(GuiGraphicsExtractor g, Font font, TacticRow row, double y) {
        frame.art(g, "tactic/ui/background/frame_2", 13, y, 350, ENTRY);
        hit(13, y, 350, ENTRY, row.tooltip());
        text(g, font, "§e" + row.name() + "§r", 27, y + 18, row.phase().isEmpty() ? 320 : 150, -1);
        text(g, font, row.phase(), 180, y + 18, 168, 1);
        frame.art(g, row.image(), 28, y + 33, 89, 47);
        for (int i = 0; i < Math.min(ICONS.length, row.effects().size()); i++) {
            frame.art(g, "tactic/ui/" + ICONS[i] + "/frame_0", 13 + ICON_X[i], y + ICON_Y[i]);
            text(g, font, row.effects().get(i), 13 + VALUE_X[i], y + ICON_Y[i] + 11, 64, -1);
        }
        if (!row.unlocked()) frame.art(g, "tactic/ui/locked/frame_0", 288, y + 50);
    }
}
