package dev.hoi.client.ui;

import dev.hoi.client.ui.tooltip.Tooltips;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class UiMenuStyle {
    public static final int TEXT = 0xFFE3E1D8, ACCENT = 0xFFB8BDB6, MUTED = 0xFF9B9D97;
    public record Window(String tile, int width, int originX, int headerX, int headerY, int titleX, int titleWidth, int closeX, int closeY, boolean clip) {
        public Window(String tile, int width, int headerX, int titleX) { this(tile, width, 0, headerX, 0, titleX, 0, 0, 0, true); }
    }
    public record Frame(float x, float y, float scale) {
        private static float pixel(float value) {
            double gui = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
            return gui > 0 ? (float)(Math.round(value * gui) / gui) : value;
        }
        public float x(double px) { return pixel(x + (float)(px * scale)); }
        public float y(double py) { return pixel(y + (float)(py * scale)); }
        public int ix(double px) { return Math.round(x(px)); }
        public int iy(double py) { return Math.round(y(py)); }
        public int len(double px) { return Math.round((float)(px * scale)); }
        public float size(double px) { return (float)(px * scale); }
        public boolean art(GuiGraphicsExtractor g, String path, double px, double py) {
            int[] size = UiAssets.imageSize(path);
            return size[0] > 0 && UiAssets.drawExact(g, path, x(px), y(py), size(size[0]), size(size[1]));
        }
        public boolean art(GuiGraphicsExtractor g, String path, double px, double py, double pw, double ph) {
            return UiAssets.drawExact(g, path, x(px), y(py), size(pw), size(ph));
        }
        public void tile(GuiGraphicsExtractor g, String path, double px, double py, int pw, int ph, int border, int declared) {
            g.pose().pushMatrix();
            g.pose().translate(x(px), y(py));
            g.pose().scale(scale);
            UiAssets.tiledNineSlice(g, path, 0, 0, pw, ph, border, 1, declared);
            g.pose().popMatrix();
        }
    }
    public static final int NO_HEADER = Integer.MIN_VALUE, TILE = 192;
    public static final Frame SCREEN = new Frame(0, 0, 1);
    private static final int HEADER_WIDTH = 794, HEADER_SOURCE_HEIGHT = 41, HEADER_CENTER = 19;
    private static final net.minecraft.resources.Identifier HEADER = net.minecraft.resources.Identifier.parse("hoi:textures/gui/decision/ui/window_header.png");
    private static final String THIN_BORDER = "commander/commands/tiled_window_thin_border2/frame_0", TILED_BG = "commander/commands/tiled_bg/frame_0";
    public static final Window DIPLOMACY = new Window(TILED_BG, 550, 0, -248, 6, 14, 0, 0, 0, true), OCCUPATION = new Window(TILED_BG, 550, -249, 45),
            TEMPLATES = new Window("commander/commands/tiled_window_1b_thin_border/frame_0", 550, NO_HEADER, 45);
    private UiMenuStyle() {}
    public static Window window(dev.hoi.protocol.menu.MenuTab tab) {
        return switch (tab) {
            case POLITICS -> new Window(THIN_BORDER, 726, -5, -70, 4, 40, 400, 683, 6, true);
            case TRADE -> new Window("commander/commands/tiled_window_1b_border/frame_0", 620, -6, -170, 6, 332, 217, 582, 8, false);
            case CONSTRUCTION -> new Window(THIN_BORDER, 550, -9, -248, 7, 45, 240, 507, 9, true);
            case PRODUCTION -> new Window("commander/commands/tiled_plain_bg/frame_0", 550, -6, -245, 7, 45, 440, 507, 9, true);
            case RECRUITMENT -> new Window("commander/commands/tiled_window/frame_0", 550, -6, -248, 7, 45, 440, 507, 9, true);
            case LOGISTICS -> new Window(THIN_BORDER, 560, -6, -240, 7, 45, 240, 514, 10, true);
            case INTELLIGENCE -> new Window(TILED_BG, 550, -6, -245, 7, 45, 440, 507, 9, true);
            case OFFICER_CORPS -> new Window(TILED_BG, 550, -6, 5, 7, 45, 440, 507, 10, true);
            case DECISIONS -> new Window("diplomacy/dialog/panel_dark", 550, -6, -248, 7, 45, 440, 507, 9, true);
            case RESEARCH -> new Window(THIN_BORDER, 550, -6, -245, 7, 45, 440, 507, 9, true);
        };
    }
    public static Frame frame(Window window, int screenWidth, int pane) {
        float scale = pane / (float)window.width();
        return new Frame(window.originX() * scale, dev.hoi.client.ui.hud.HudMenuBar.windowTop(screenWidth), scale);
    }
    public static void window(GuiGraphicsExtractor g, Window window, Frame frame, int bottom) {
        int height = (int)Math.ceil((bottom - frame.y()) / frame.scale());
        frame.tile(g, window.tile(), 0, 0, window.width(), height, 64, TILE);
        header(g, window, frame);
    }
    private static void header(GuiGraphicsExtractor g, Window window, Frame frame) {
        if (window.headerX() == NO_HEADER) return;
        int start = Math.max(0, -window.headerX()), end = window.clip() ? Math.min(HEADER_WIDTH, window.width() - window.headerX()) : HEADER_WIDTH;
        if (end <= start) return;
        g.pose().pushMatrix();
        g.pose().translate(frame.x(window.headerX() + start), frame.y(window.headerY()));
        g.pose().scale(frame.scale());
        g.blit(HEADER, 0, 0, end - start, HEADER_SOURCE_HEIGHT, (float)start / HEADER_WIDTH, (float)end / HEADER_WIDTH, 0, 1);
        g.pose().popMatrix();
    }
    public static void title(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, Window window, Frame frame, String text) {
        title(g, font, frame, text, window.titleX() + 2, window.headerY() + HEADER_CENTER, window.titleWidth() - 2, -1, TEXT);
    }
    public static void title(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, Frame frame, String text, int x, double center, int width, int align, int color) {
        title(g, font, frame, text, x, center, width, align, color, false);
    }
    public static void title(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, Frame frame, String text, int x, double center, int width, int align, int color, boolean outline) {
        float scale = (font.lineHeight + 2f) / font.lineHeight;
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.officerText(text, true));
        if (outline) UiText.outlined(g, font, line, frame.x(x), frame.y(center) - 4.5f * scale, frame.size(width), scale, color, true, align);
        else UiText.marquee(g, font, line, frame.x(x), frame.y(center) - 4.5f * scale, frame.size(width), scale, color, true, align);
    }
    public static UiMenuButton close(Window window, Frame frame, Runnable action) {
        return new UiMenuButton("×", frame.ix(window.closeX()), frame.iy(window.closeY()), frame.len(32), frame.len(33), action)
                .caption("").background("menu/close").art(frame.x(window.closeX()), frame.y(window.closeY()), frame.size(32), frame.size(33)).sound("ui.close");
    }
    public static UiMenuButton close(float x, float y, Runnable action) { return close("×", "menu/close", x, y, 16, 16.5f, action); }
    public static UiMenuButton close(String sprite, float x, float y, float w, float h, Runnable action) { return close("×", sprite, x, y, w, h, action); }
    public static UiMenuButton close(String label, String sprite, float x, float y, float w, float h, Runnable action) {
        int left = (int)Math.floor(x), upper = (int)Math.floor(y);
        return new UiMenuButton(label, left, upper, (int)Math.ceil(x + w) - left, (int)Math.ceil(y + h) - upper, action).caption("").background(sprite).art(x, y, w, h).sound("ui.close");
    }
    public static void window(GuiGraphicsExtractor g, Window window, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
        UiAssets.tiledNineSlice(g, window.tile(), x - 32, y, w + 32, h + 32, 64, .5);
        g.disableScissor();
        header(g, window, new Frame(x, y, w / (float)window.width()));
    }
    public static void title(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, Window window, String text, int x, int y, int width) {
        if (window.headerX() == NO_HEADER) { heading(g, font, text, x + window.titleX() / 2, y + 9, width - window.titleX() / 2 - 22, TEXT); return; }
        title(g, font, new Frame(x, y, width / (float)window.width()), text, window.titleX(), window.headerY() + HEADER_CENTER, window.width() - window.titleX() - 44, -1, TEXT);
    }
    public static void close(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        UiAssets.draw(g, "menu/close", x + 2, y + 2, w - 4, h - 4);
    }
    public static void heading(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                               String text, int x, int y, int width, int color) {
        float scale = (font.lineHeight + 2f) / font.lineHeight;
        UiText.marquee(g, font, net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.officerText(text, true)), x, y - 1, width, scale, 0xFFFFFFFF, true, -1);
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
