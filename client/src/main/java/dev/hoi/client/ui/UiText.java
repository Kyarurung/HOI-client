package dev.hoi.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Text sizes are measured in GUI pixels, independently of the window scale. */
public final class UiText {
    public static final int MIN_HEIGHT = 10;
    private UiText() {}
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
