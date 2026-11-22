package dev.hoi.client.ui;

import dev.hoi.client.ui.tooltip.Tooltips;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class UiStatCell {
    public static final int WIDTH = 74, HEIGHT = 32;
    private static final int LEFT = 2, RIGHT = 71, ICON = 24, ICON_HEIGHT = 20;
    private static final float GAP = 1, SMALLEST_ICON = 5;
    private UiStatCell() {}
    public static float textWidth(Font font, String value, float unit) {
        float inner = (RIGHT - LEFT) * unit, wanted = font.width(Tooltips.officerText(value)) * UiText.scale(font);
        return wanted <= inner - SMALLEST_ICON - GAP ? wanted : inner - Math.max(SMALLEST_ICON, 18 * unit) - GAP;
    }
    public static void draw(GuiGraphicsExtractor g, Font font, String icon, String value, float left, float top, float unit) {
        UiAssets.drawExact(g, "officer/stats/background", left, top, WIDTH * unit, HEIGHT * unit);
        float width = textWidth(font, value, unit), right = left + RIGHT * unit, scale = UiText.scale(font);
        int iconLeft = Math.round(left + LEFT * unit), iconWidth = (int)Math.min(Math.floor(right - width - GAP) - iconLeft, Math.round(ICON * unit)), iconHeight = Math.round(ICON_HEIGHT * unit);
        if (iconWidth > 0) UiAssets.drawContent(g, icon, iconLeft, Math.round(top + (HEIGHT * unit - iconHeight) / 2), iconWidth, iconHeight);
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.officerText(value));
        UiText.marquee(g, font, line, right - width, UiText.centeredY(font, line, top, HEIGHT * unit, scale), width, scale, Tooltips.GOLD | 0xFF000000, false, 1);
    }
}
