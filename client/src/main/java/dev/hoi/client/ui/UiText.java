package dev.hoi.client.ui;

import dev.hoi.client.ui.tooltip.Tooltips;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class UiText {
    public static final int MIN_HEIGHT = 9;
    private UiText() {}
    public static String ellipsize(Font font, String value, int width) {
        return ellipsize(font,value,width,"…");
    }
    public static String ellipsize(Font font, String value, int width, String marker) {
        if(font.width(value)<=width)return value;
        int suffix=font.width(marker);
        return width<suffix?"":font.plainSubstrByWidth(value,width-suffix)+marker;
    }
    public static void cell(GuiGraphicsExtractor g, Font font, String value, int x, int y, int width, int height, int color, boolean right) {
        cell(g, font, value, x, y, width, height, color, right, true);
    }
    public static void cell(GuiGraphicsExtractor g, Font font, String value, int x, int y, int width, int height, int color, boolean right, boolean shadow) {
        float scale = scale(font);
        String clipped = font.plainSubstrByWidth(value, Math.max(1, (int)(width / scale)));
        float remaining = width - font.width(clipped) * scale;
        g.pose().pushMatrix();
        g.pose().translate(x + (right ? remaining : remaining / 2), centeredY(font, clipped, y, height, scale));
        g.pose().scale(scale);
        g.text(font, clipped, 0, 0, color, shadow);
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
    private static boolean numeric(FormattedCharSequence text) {
        boolean[] numeric = {true, false};
        text.accept((index, style, point) -> {
            numeric[1] |= Character.isDigit(point);
            numeric[0] &= Character.isDigit(point) || " /.,%+-−–()".indexOf(point) >= 0;
            return true;
        });
        return numeric[0] && numeric[1];
    }
    public static float centeredY(Font font, FormattedCharSequence text, float y, float height, float scale) {
        if (!numeric(text)) return y + (height - font.lineHeight * scale) / 2;
        var bounds = font.prepareText(text, 0, 0, 0xFFFFFFFF, false, false, 0).bounds();
        return bounds == null ? y + (height - font.lineHeight * scale) / 2
                : y + (height - bounds.height() * scale) / 2 - bounds.top() * scale;
    }
    public static float centeredY(Font font, String text, float y, float height, float scale) {
        return centeredY(font, Component.literal(text).getVisualOrderText(), y, height, scale);
    }
    public static float scale(Font font) { return Math.max(1f, MIN_HEIGHT / (float)font.lineHeight); }
    public static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        text(g, font, Component.literal(text).getVisualOrderText(), x, y, color);
    }
    public static void text(GuiGraphicsExtractor g, Font font, FormattedCharSequence text, int x, int y, int color) {
        text(g, font, text, x, y, color, true);
    }
    public static void text(GuiGraphicsExtractor g, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale(font));
        g.text(font, text, 0, 0, color, shadow); g.pose().popMatrix();
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
    public static void leftSingleLine(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int height, int color) {
        float fitted = Math.min(scale(font), Math.min(width / (float)Math.max(1, font.width(text)), height / (float)font.lineHeight));
        g.pose().pushMatrix();
        g.pose().translate(x, centeredY(font, text.getVisualOrderText(), y, height, fitted));
        g.pose().scale(fitted);
        g.text(font, text, 0, 0, color);
        g.pose().popMatrix();
    }
    public static void scrollingCell(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int height, int color, boolean centered) {
        var styled = text.copy().withStyle(style -> style.withColor(color).withShadowColor(0));
        var bounds = font.prepareText(styled.getVisualOrderText(), 0, 0, color, false, false, 0).bounds();
        int lineHeight = bounds == null ? font.lineHeight : bounds.top() * 2 + bounds.height() + 2;
        var renderer = g.textRenderer();
        renderer.defaultScrollingHelper(styled, x + (centered ? width : Math.min(width, font.width(text))) / 2,
                x, x + width, y, y + height, font.width(text), lineHeight, renderer.defaultParameters());
    }
    public static void left(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int height, int color) {
        left(g, font, Component.literal(text), x, y, width, height, color);
    }
    public static void scrollingCell(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int height, int color, boolean centered, long elapsedMillis) {
        var bounds = font.prepareText(text.getVisualOrderText(), 0, 0, color, false, false, 0).bounds();
        int inset = bounds == null ? 0 : Math.min(0, bounds.left());
        int textWidth = Math.max(font.width(text), bounds == null ? 0 : bounds.right()) - inset, overflow = Math.max(0, textWidth - width);
        double travel = overflow / 24.0, phase = Math.max(0, elapsedMillis) / 1000.0 % (2 * travel + 2);
        int offset = (int)Math.round(24 * Math.clamp(phase <= travel + 1 ? phase - 1 : 2 * travel + 2 - phase, 0, travel));
        int left = overflow > 0 ? x - offset : centered ? x + (width - textWidth) / 2 : x;
        var styled = text.copy().withStyle(style -> style.withColor(color).withShadowColor(0));
        g.enableScissor(x, y, x + width, y + height);
        g.text(font, styled, left - inset, Math.round(centeredY(font, styled.getVisualOrderText(), y, height, 1)), color, false);
        g.disableScissor();
    }
    public static void left(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int height, int color) {
        var lines = Tooltips.split(font,text, Math.max(1, (int)(width / scale(font))));
        int count = Math.min(lines.size(), Math.max(1, height / (MIN_HEIGHT + 1)));
        int top = y + (height - count * (MIN_HEIGHT + 1) + 1) / 2;
        for (int i = 0; i < count; i++) text(g, font, lines.get(i), x, top + i * (MIN_HEIGHT + 1), color);
    }
    public static void centeredSingleLine(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int height, int color) {
        float fitted = Math.min(scale(font), Math.min(width / (float)Math.max(1, font.width(text)), height / (float)font.lineHeight));
        g.pose().pushMatrix();
        g.pose().translate(x + (width - font.width(text) * fitted) / 2, centeredY(font, text.getVisualOrderText(), y, height, fitted));
        g.pose().scale(fitted);
        g.text(font, text, 0, 0, color);
        g.pose().popMatrix();
    }
    public static void centered(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int height, int color) {
        centered(g,font,Component.literal(text),x,y,width,height,color);
    }
    public static void centered(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int height, int color, boolean shadow) {
        centered(g, font, Component.literal(text), x, y, width, height, color, shadow);
    }
    public static void centered(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int height, int color) {
        centered(g, font, text, x, y, width, height, color, true);
    }
    public static void centered(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int height, int color, boolean shadow) {
        float scale = scale(font);
        var lines = Tooltips.split(font,text, Math.max(1, (int)(width / scale)));
        int count = Math.min(lines.size(), Math.max(1, height / (MIN_HEIGHT + 1)));
        int top = y + (height - count * (MIN_HEIGHT + 1) + 1) / 2;
        if (count == 1 && numeric(lines.getFirst())) {
            var line = lines.getFirst();
            g.pose().pushMatrix();
            g.pose().translate(x + (width - font.width(line) * scale) / 2, centeredY(font, line, y, height, scale));
            g.pose().scale(scale);
            g.text(font, line, 0, 0, color, shadow);
            g.pose().popMatrix();
            return;
        }
        for (int i=0; i<count; i++) text(g, font, lines.get(i),
                x + Math.round((width - font.width(lines.get(i)) * scale) / 2), top + i * (MIN_HEIGHT + 1), color, shadow);
    }
}
