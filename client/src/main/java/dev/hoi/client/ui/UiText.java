package dev.hoi.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class UiText {
    public static final int MIN_HEIGHT = 9;
    private UiText() {}
    public static void cell(GuiGraphicsExtractor g, Font font, String value, int x, int y, int width, int height, int color, boolean right) {
        float scale = scale(font);
        String clipped = font.plainSubstrByWidth(value, Math.max(1, (int)(width / scale)));
        float remaining = width - font.width(clipped) * scale;
        g.pose().pushMatrix();
        g.pose().translate(x + (right ? remaining : remaining / 2), y + (height - font.lineHeight * scale) / 2);
        g.pose().scale(scale);
        g.text(font, clipped, 0, 0, color);
        g.pose().popMatrix();
    }
    public static void iconText(GuiGraphicsExtractor g, Font font, String icon, String value, int x, int y, int width, int height, int iconSize, int color) {
        float scale = scale(font);
        int size = Math.min(iconSize, Math.min(height, Math.max(1, width - 4)));
        String clipped = font.plainSubstrByWidth(value, Math.max(1, (int)((width - size - 3) / scale)));
        int textWidth = Math.round(font.width(clipped) * scale);
        int left = x + (width - size - 3 - textWidth) / 2;
        UiAssets.draw(g, icon, left, y + (height - size) / 2, size, size);
        cell(g, font, clipped, left + size + 3, y, textWidth, height, color, false);
    }
    public static float scale(Font font) { return Math.max(1f, MIN_HEIGHT / (float)font.lineHeight); }
    public static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        text(g, font, Component.literal(text).getVisualOrderText(), x, y, color);
    }
    public static void text(GuiGraphicsExtractor g, Font font, FormattedCharSequence text, int x, int y, int color) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale(font));
        g.text(font, text, 0, 0, color); g.pose().popMatrix();
    }
    public static void text(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
        text(g, font, text.getVisualOrderText(), x, y, color);
    }
    public static void centeredAt(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        text(g, font, text, x - Math.round(font.width(text) * scale(font) / 2), y, color);
    }
    public static void centeredAt(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
        text(g, font, text, x - Math.round(font.width(text) * scale(font) / 2), y, color);
    }
    public static void centered(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int height, int color) {
        float scale = scale(font);
        var lines = font.split(Component.literal(text), Math.max(1, (int)(width / scale)));
        int count = Math.min(lines.size(), Math.max(1, height / (MIN_HEIGHT + 1)));
        int top = y + (height - count * (MIN_HEIGHT + 1) + 1) / 2;
        for (int i=0; i<count; i++) text(g, font, lines.get(i),
                x + Math.round((width - font.width(lines.get(i)) * scale) / 2), top + i * (MIN_HEIGHT + 1), color);
    }
}
