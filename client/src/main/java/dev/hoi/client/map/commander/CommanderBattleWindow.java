package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.country.CountryFlags;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderBattlePayloads.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public abstract class CommanderBattleWindow {
    static Consumer<String> country = HoiClient::openCountry;
    record Box(double x, double y, double w, double h) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    record Hit(Box box, String tooltip, String sound, Runnable action) {}
    final class Scroll {
        private final double barX;
        private final int step;
        private Box view = new Box(0, 0, 0, 0);
        int offset, content;
        Scroll(double barX, int step) { this.barX = barX; this.step = step; }
        int maximum() { return (int)Math.max(0, Math.ceil(content - view.h())); }
        void begin(GuiGraphicsExtractor g, Box area, int size) {
            view = area; content = size; offset = Math.clamp(offset, 0, maximum());
            scrolls.add(this); clip = area;
            g.enableScissor(frame.ix(area.x()), frame.iy(area.y()), frame.ix(area.x() + area.w()), frame.iy(area.y() + area.h()));
        }
        void end(GuiGraphicsExtractor g) {
            g.disableScissor(); clip = null;
            if (maximum() > 0) bar().draw(g, Math.round(frame.size(offset)));
        }
        private UiScrollBar bar() { return new UiScrollBar(frame.ix(barX), frame.iy(view.y()), frame.iy(view.y() + view.h()), Math.round(frame.size(view.h())), Math.round(frame.size(content))); }
        private void move(int amount) { offset = Math.clamp(offset + amount, 0, maximum()); }
        private boolean press(double my) {
            if (bar().arrow(my)) { move(my < frame.y(view.y()) + 8 ? -step : step); return false; }
            drag(my);
            return true;
        }
        private void drag(double my) { offset = Math.clamp(Math.round(bar().offset(my) / frame.scale()), 0, maximum()); }
    }
    private static final int LINE = 9, OUTLINE = 0xFF1C1C1C;
    private final String token = UUID.randomUUID().toString(), id;
    private final Kind kind;
    private final int width, height;
    private final double offsetX, offsetY;
    private final List<Hit> hits = new ArrayList<>();
    private final List<Scroll> scrolls = new ArrayList<>();
    UiMenuStyle.Frame frame;
    private float left = Float.NaN, top;
    private double grabX, grabY;
    private boolean moving;
    private Scroll dragging;
    private Box clip;
    CommanderBattleWindow(Kind kind, String id, int width, int height, double offsetX, double offsetY) {
        this.kind = kind; this.id = id; this.width = width; this.height = height; this.offsetX = offsetX; this.offsetY = offsetY;
    }
    public String token() { return token; }
    public String id() { return id; }
    public Kind kind() { return kind; }
    public UiMenuStyle.Frame frame() { return frame; }
    void open() { AudioUiSounds.play("ui.pop_up"); send(Action.OPEN); }
    void close() { send(Action.CLOSE); }
    void send(Action action) { CommanderBattleHud.send(new Request(token, action, kind, id)); }
    boolean accept(Snapshot snapshot) { return true; }
    abstract void render(GuiGraphicsExtractor g, Font font);
    void place(CommanderBattleWindow previous) { left = previous.left; top = previous.top; }
    void place(float x, float y) { left = x; top = y; }
    void layout(int screenWidth, int screenHeight, float scale) {
        int bar = HudMenuBar.height(screenWidth);
        scale = Math.min(scale, Math.min((screenWidth - 8f) / width, (screenHeight - bar - 8f) / height));
        if (Float.isNaN(left)) { left = screenWidth / 2f + (float)offsetX * scale; top = screenHeight / 2f + (float)offsetY * scale; }
        left = Math.clamp(left, 0, Math.max(0, screenWidth - width * scale));
        top = Math.clamp(top, bar, Math.max(bar, screenHeight - height * scale));
        frame = new UiMenuStyle.Frame(left, top, scale);
    }
    boolean contains(double x, double y) { return frame != null && gui(new Box(0, 0, width, height)).contains(x, y); }
    Box gui(Box box) { return new Box(frame.x(box.x()), frame.y(box.y()), frame.size(box.w()), frame.size(box.h())); }
    int line() { return Math.max(2 * LINE, (int)Math.ceil(LINE / frame.scale())); }
    void draw(GuiGraphicsExtractor g, Font font) {
        if (frame == null) return;
        hits.clear(); scrolls.clear(); clip = null;
        g.nextStratum();
        render(g, font);
    }
    void hit(double x, double y, double w, double h, String tooltip) { hit(x, y, w, h, tooltip, "", null); }
    void hit(double x, double y, double w, double h, String tooltip, String sound, Runnable action) {
        if (clip != null) {
            double right = Math.min(x + w, clip.x() + clip.w()), bottom = Math.min(y + h, clip.y() + clip.h());
            x = Math.max(x, clip.x()); y = Math.max(y, clip.y()); w = right - x; h = bottom - y;
        }
        if (w > 0 && h > 0 && (action != null || !tooltip.isEmpty())) hits.add(new Hit(new Box(x, y, w, h), tooltip, sound, action));
    }
    void closeButton(GuiGraphicsExtractor g, String art, double x, double y, double w, double h, String tooltip) {
        frame.art(g, art, x, y, w, h);
        hit(x, y, w, h, tooltip, "ui.close", () -> CommanderBattleHud.close(this));
    }
    void flag(GuiGraphicsExtractor g, String tag, double x, double y, String tooltip) {
        CountryFlags.draw(g, HudMenuBar.flagTexture(tag), "small2", frame.x(x), frame.y(y), frame.size(41), frame.size(26));
        hit(x, y, 41, 26, tooltip, "ui.click", () -> country.accept(tag));
    }
    void smallFlag(GuiGraphicsExtractor g, String tag, double x, double y, double w, double h) { UiAssets.drawExact(g, HudMenuBar.flagTexture(tag), frame.x(x), frame.y(y), frame.size(w), frame.size(h)); }
    void traits(GuiGraphicsExtractor g, List<Icon> traits, double x, double y, double slotW, double slotH, int columns, boolean right) {
        for (int i = 0; i < traits.size(); i++) {
            double px = right ? x - slotW * (traits.size() - i) : x + slotW * (i % columns), py = right ? y : y + slotH * (i / columns);
            frame.art(g, traits.get(i).image(), px + (slotW - 18.4) / 2, py - 5, 18.4, 26.4);
            hit(px, py - 5, slotW, 26.4, traits.get(i).tooltip());
        }
    }
    void text(GuiGraphicsExtractor g, Font font, String value, double x, double center, double w, int align) {
        if (value.isEmpty()) return;
        var line = Language.getInstance().getVisualOrder(Tooltips.panelText(value));
        float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(x), UiText.centeredY(font, line, frame.y(center) - LINE / 2f, LINE, scale), frame.size(w), scale, UiMenuStyle.TEXT, false, align);
    }
    void outlined(GuiGraphicsExtractor g, Font font, String value, double x, double center, double w, int align) { outlined(g, font, value, x, center, w, align, false); }
    void outlined(GuiGraphicsExtractor g, Font font, String value, double x, double center, double w, int align, boolean large) {
        if (value.isEmpty()) return;
        var text = large ? Tooltips.officerText(value, true) : Tooltips.panelText(value);
        var dark = Component.empty();
        text.visit((style, part) -> { dark.append(Component.literal(part).withStyle(style.withColor(OUTLINE & 0xFFFFFF))); return Optional.empty(); }, Style.EMPTY);
        var line = Language.getInstance().getVisualOrder(text); var edge = Language.getInstance().getVisualOrder(dark);
        float scale = large ? (LINE + 2f) / LINE : UiText.scale(font), pixel = 1f / Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        float px = frame.x(x), py = large ? frame.y(center) - LINE / 2f * scale : UiText.centeredY(font, line, frame.y(center) - LINE / 2f, LINE, scale);
        for (int i = 0; i < 9; i++) if (i != 4) UiText.marquee(g, font, edge, px + (i % 3 - 1) * pixel, py + (i / 3 - 1) * pixel, frame.size(w), scale, OUTLINE, false, align);
        UiText.marquee(g, font, line, px, py, frame.size(w), scale, UiMenuStyle.TEXT, false, align);
    }
    double width(Font font, String value) { return font.width(value) * UiText.scale(font) / frame.scale(); }
    void nine(GuiGraphicsExtractor g, String path, double x, double y, double w, double h, int border) {
        g.pose().pushMatrix();
        g.pose().translate(frame.x(x), frame.y(y));
        g.pose().scale(frame.scale());
        UiAssets.nineSlice(g, path, 0, 0, (int)Math.round(w), (int)Math.round(h), border, 1);
        g.pose().popMatrix();
    }
    void part(GuiGraphicsExtractor g, String path, double x, double y, double w, double h, double from, double to) {
        if (to <= from) return;
        g.enableScissor(frame.ix(x + w * from), frame.iy(y) - 1, frame.ix(x + w * to), frame.iy(y + h) + 1);
        frame.art(g, path, x, y, w, h);
        g.disableScissor();
    }
    void upright(GuiGraphicsExtractor g, String path, double x, double y, double w, double h, double ratio) {
        g.pose().pushMatrix();
        g.pose().translate(frame.x(x), frame.y(y + h));
        g.pose().rotate(-(float)Math.PI / 2);
        g.pose().scale(frame.scale());
        g.blit(Identifier.parse("hoi:textures/gui/" + path + ".png"), 0, 0, (int)Math.round(h * Math.clamp(ratio, 0, 1)), (int)Math.round(w), 0, (float)Math.clamp(ratio, 0, 1), 0, 1);
        g.pose().popMatrix();
    }
    String tooltip(double mx, double my) {
        for (int i = hits.size() - 1; i >= 0; i--) if (!hits.get(i).tooltip().isEmpty() && gui(hits.get(i).box()).contains(mx, my)) return hits.get(i).tooltip();
        return "";
    }
    boolean mouseClicked(MouseButtonEvent event) {
        double mx = event.x(), my = event.y();
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        for (var scroll : scrolls) if (scroll.maximum() > 0 && scroll.bar().contains(mx, my)) { if (scroll.press(my)) dragging = scroll; return true; }
        for (int i = hits.size() - 1; i >= 0; i--) {
            var hit = hits.get(i);
            if (hit.action() == null || !gui(hit.box()).contains(mx, my)) continue;
            if (!hit.sound().isEmpty()) AudioUiSounds.play(hit.sound());
            hit.action().run();
            return true;
        }
        moving = true; grabX = mx - left; grabY = my - top;
        return true;
    }
    boolean mouseDragged(MouseButtonEvent event) {
        if (moving) { left = (float)(event.x() - grabX); top = (float)(event.y() - grabY); return true; }
        if (dragging == null) return false;
        dragging.drag(event.y());
        return true;
    }
    boolean mouseReleased() { boolean held = moving || dragging != null; moving = false; dragging = null; return held; }
    boolean mouseScrolled(double x, double y, double vertical) {
        for (var scroll : scrolls) if (gui(scroll.view).contains(x, y)) scroll.move(-(int)Math.round(vertical * scroll.step));
        return true;
    }
}
