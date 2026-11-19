package dev.hoi.client.ui;

import dev.hoi.client.ui.tooltip.Tooltips;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class UiMenuStyle {
    public static final int TEXT = 0xFFE3E1D8, ACCENT = 0xFFB8BDB6, MUTED = 0xFF9B9D97;
    public record Window(String tile, int width, int headerX, int titleX) {}
    public static final int NO_HEADER = Integer.MIN_VALUE;
    private static final int HEADER_WIDTH = 794, HEADER_HEIGHT = 21;
    private static final String THIN_BORDER = "commander/commands/tiled_window_thin_border2/frame_0", TILED_BG = "commander/commands/tiled_bg/frame_0";
    public static final Window DIPLOMACY = new Window(TILED_BG, 550, NO_HEADER, 14), OCCUPATION = new Window(TILED_BG, 550, -249, 45),
            TEMPLATES = new Window("commander/commands/tiled_window_1b_thin_border/frame_0", 550, NO_HEADER, 45);
    private UiMenuStyle() {}
    public static Window window(dev.hoi.protocol.menu.MenuTab tab) {
        return switch (tab) {
            case POLITICS -> new Window(THIN_BORDER, 726, -70, 40);
            case TRADE -> new Window("commander/commands/tiled_window_1b_border/frame_0", 620, -170, 45);
            case CONSTRUCTION -> new Window(THIN_BORDER, 550, -248, 45);
            case PRODUCTION -> new Window("commander/commands/tiled_plain_bg/frame_0", 550, -245, 45);
            case RECRUITMENT -> new Window("commander/commands/tiled_window/frame_0", 550, -248, 45);
            case LOGISTICS -> new Window(THIN_BORDER, 560, -240, 45);
            case INTELLIGENCE -> new Window(TILED_BG, 550, -245, 45);
            case OFFICER_CORPS -> new Window(TILED_BG, 550, 5, 45);
            case DECISIONS -> new Window("diplomacy/dialog/panel_dark", 550, -248, 45);
            case RESEARCH -> new Window("research/source/gfx_tiled_plain_bg2/frame_0", 550, NO_HEADER, 45);
        };
    }
    public static void window(GuiGraphicsExtractor g, Window window, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
        UiAssets.tiledNineSlice(g, window.tile(), x - 32, y, w + 32, h + 32, 64, .5);
        g.disableScissor();
        if (window.headerX() == NO_HEADER) return;
        int start = Math.max(0, -window.headerX()), end = Math.min(HEADER_WIDTH, window.width() - window.headerX());
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/decision/ui/window_header.png"), x + Math.max(0, window.headerX()) / 2, y, x + w, y + HEADER_HEIGHT,
                (float)start / HEADER_WIDTH, (float)end / HEADER_WIDTH, 0, 1);
    }
    public static void title(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, Window window, String text, int x, int y, int width) {
        heading(g, font, text, x + window.titleX() / 2, y + (window.headerX() == NO_HEADER ? 9 : 6), width - window.titleX() / 2 - 22, TEXT);
    }
    public static void close(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        UiAssets.draw(g, "menu/close", x + 2, y + 2, w - 4, h - 4);
    }
    public static void heading(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                               String text, int x, int y, int width, int color) {
        float scale = (font.lineHeight + 2f) / font.lineHeight;
        var label = net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(Tooltips.officerText(text, true), Math.max(1, (int)(width / scale))));
        g.pose().pushMatrix(); g.pose().translate(x, y - 1); g.pose().scale(scale);
        g.text(font, label, 0, 0, 0xFFFFFFFF); g.pose().popMatrix();
    }
    public static boolean symbol(GuiGraphicsExtractor g, String label, int x, int y, int w, int h, int color) {
        if (label.equals("×") || label.equals("X")) { close(g, x, y, w, h, color); return true; }
        if (!java.util.Set.of("−", "-", "+", "↑", "↓").contains(label)) return false;
        int size = Math.min(7, Math.min(w, h) - 6);
        if (size % 2 == 0) size--;
        int left = x + (w - size) / 2, top = y + (h - size) / 2, middle = size / 2;
        if (label.equals("↑") || label.equals("↓")) {
            g.fill(left + middle, top, left + middle + 1, top + size, color);
            for (int i = 1; i <= middle; i++) {
                int row = label.equals("↑") ? top + i : top + size - i - 1;
                g.fill(left + middle - i, row, left + middle - i + 1, row + 1, color);
                g.fill(left + middle + i, row, left + middle + i + 1, row + 1, color);
            }
        } else {
            g.fill(left, top + middle, left + size, top + middle + 1, color);
            if (label.equals("+")) g.fill(left + middle, top, left + middle + 1, top + size, color);
        }
        return true;
    }
    public static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean inset) {
        g.outline(x, y, w, h, 0xFF08090A);
        g.horizontalLine(x + 1, x + w - 2, y + 1, inset ? 0xFF121316 : 0xFF73767B);
        g.verticalLine(x + 1, y + 1, y + h - 2, inset ? 0xFF121316 : 0xFF51555A);
        g.horizontalLine(x + 1, x + w - 2, y + h - 2, inset ? 0xFF51555A : 0xFF131416);
        g.verticalLine(x + w - 2, y + 1, y + h - 2, inset ? 0xFF51555A : 0xFF131416);
    }
    public static void metal(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h / 2, 0xFF484A51, 0xFF303138);
        g.fillGradient(x, y + h / 2, x + w, y + h, 0xFF2A2B31, 0xFF1A1B20);
        bevel(g, x, y, w, h, false);
    }
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 3, y + 3, x + w + 3, y + h, 0x60000000);
        g.fillGradient(x, y, x + w, y + h, 0xFF202126, 0xFF121317);
        bevel(g, x, y, w, h, false);
        g.outline(x + 3, y + 3, w - 6, h - 6, 0xFF08090B);
        metal(g, x, y, w, 26);
    }
    public static void recess(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, 0xFF0B0C0E, 0xFF191A1F);
        bevel(g, x, y, w, h, true);
    }
    public static void control(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected, boolean hover) {
        if (selected) {
            g.fillGradient(x, y, x + w, y + h, 0xFF41494A, 0xFF202728);
            bevel(g, x, y, w, h, true);
            g.horizontalLine(x + 3, x + w - 4, y + h - 3, ACCENT);
        } else metal(g, x, y, w, h);
    }
}
