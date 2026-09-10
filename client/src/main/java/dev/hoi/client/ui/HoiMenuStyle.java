package dev.hoi.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;


public final class HoiMenuStyle {
    public static final int TEXT = 0xFFE3E1D8, ACCENT = 0xFFB8BDB6, MUTED = 0xFF9B9D97;
    private HoiMenuStyle() {}

    public static void close(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        int size = Math.min(9, Math.min(w, h) - 6);
        int left = x + (w - size) / 2, top = y + (h - size) / 2;
        for (int i = 0; i < size; i++) {
            g.fill(left + i, top + i, left + i + 1, top + i + 1, color);
            g.fill(left + size - i - 1, top + i, left + size - i, top + i + 1, color);
        }
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
        if (hover) g.outline(x + 1, y + 1, w - 2, h - 2, ACCENT);
    }
}
