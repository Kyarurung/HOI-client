package dev.hoi.client.screen.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderOverviewPayloads.View;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public final class CommanderNavalHeadquartersWindow {
    static final int WIDTH = 576, HEIGHT = 607, LIST = 100;
    private static final int LIST_BOTTOM = HEIGHT - 28, STEP = 37, BAR_X = 550, BAR_TOP = 97, BAR_BOTTOM = 575;
    private static final String PICTURE = "commander/equipment/headquarters_bg";
    private UiMenuStyle.Frame frame;
    private int width, height, scroll;
    private float base, movedX, movedY;
    private double grabX, grabY;
    private boolean moving, dragging;
    UiMenuStyle.Frame frame() { return frame; }
    int scroll() { return scroll; }
    void layout(int width, int height, float base) { this.width = width; this.height = height; this.base = base; place(); }
    private void place() {
        float s = Math.min(base, Math.min((width - 8f) / WIDTH, (height - 8f) / HEIGHT));
        float x = Math.clamp(width / 2f - 225 * s + movedX, 0, Math.max(0, width - WIDTH * s)), y = Math.clamp(height / 2f - 295 * s + movedY, 0, Math.max(0, height - HEIGHT * s));
        movedX = x - (width / 2f - 225 * s); movedY = y - (height / 2f - 295 * s);
        frame = new UiMenuStyle.Frame(x, y, s);
    }
    boolean contains(double x, double y) { return frame != null && over(x, y, 0, 0, WIDTH, HEIGHT); }
    private boolean over(double mx, double my, double x, double y, double w, double h) { return mx >= frame.x(x) && mx < frame.x(x + w) && my >= frame.y(y) && my < frame.y(y + h); }
    private boolean overClose(double mx, double my) { return over(mx, my, WIDTH - 43, 10, 32, 33); }
    private int maxScroll() { return Math.max(0, UiAssets.imageSize(PICTURE)[1] - (LIST_BOTTOM - LIST)); }
    private UiScrollBar bar() { return new UiScrollBar(frame.ix(BAR_X), frame.iy(BAR_TOP), frame.iy(BAR_BOTTOM), Math.round(frame.size(LIST_BOTTOM - LIST)), Math.round(frame.size(LIST_BOTTOM - LIST + maxScroll()))); }
    void draw(GuiGraphicsExtractor g, Font font, View view) {
        if (frame == null || view == null) return;
        g.nextStratum();
        frame.tile(g, "commander/commands/tiled_plain_bg/frame_0", 0, 0, WIDTH, HEIGHT, 64, 192);
        frame.tile(g, "commander/commands/tiled_window_1b_border/frame_0", 0, LIST, WIDTH, HEIGHT - LIST - 4, 64, 192);
        g.enableScissor(frame.ix(13), frame.iy(LIST + 8), frame.ix(WIDTH - 25), frame.iy(LIST_BOTTOM));
        frame.art(g, PICTURE, 13, LIST - scroll);
        g.disableScissor();
        bar().draw(g, Math.round(frame.size(scroll)));
        frame.art(g, "commander/equipment/headquarters_header", 1, 0);
        UiMenuStyle.title(g, font, frame, view.label("NAVAL_HEADQIARTER_HEADER"), 60, 31, 456, 0, UiMenuStyle.TEXT);
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.panelText(view.label("NAVAL_HEADQUARTER_ACTIVE")));
        float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(120), UiText.centeredY(font, line, frame.y(64), frame.size(23), scale), frame.size(334), scale, UiMenuStyle.TEXT, false, 0);
        frame.art(g, "menu/close", WIDTH - 43, 10);
    }
    String source(double mx, double my, View view) {
        if (view == null || !contains(mx, my)) return "";
        return overClose(mx, my) ? view.label("CLOSE") : over(mx, my, 160, 64, 256, 23) ? view.label("NAVAL_HEADQUARTER_ACTIVE_TT") : "";
    }
    boolean mouseClicked(MouseButtonEvent event, Runnable closer) {
        double mx = event.x(), my = event.y();
        if (!contains(mx, my)) return false;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (overClose(mx, my)) { AudioUiSounds.play("ui.close"); closer.run(); return true; }
        if (bar().contains(mx, my)) {
            if (bar().arrow(my)) scroll = Math.clamp(scroll + (my < frame.y(BAR_TOP) + 8 ? -STEP : STEP), 0, maxScroll());
            else { dragging = true; drag(my); }
            return true;
        }
        moving = true; grabX = mx - movedX; grabY = my - movedY;
        return true;
    }
    private void drag(double my) { scroll = Math.clamp(Math.round(bar().offset(my) / frame.scale()), 0, maxScroll()); }
    boolean mouseDragged(MouseButtonEvent event) {
        if (dragging) { drag(event.y()); return true; }
        if (!moving) return false;
        movedX = (float)(event.x() - grabX); movedY = (float)(event.y() - grabY); place();
        return true;
    }
    boolean mouseReleased() { boolean held = moving || dragging; moving = dragging = false; return held; }
    boolean mouseScrolled(double x, double y, double vertical) {
        if (!contains(x, y)) return false;
        scroll = Math.clamp(scroll - (int)Math.round(vertical * STEP), 0, maxScroll());
        return true;
    }
}
