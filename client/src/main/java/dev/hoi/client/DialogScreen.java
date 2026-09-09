package dev.hoi.client;

import dev.hoi.protocol.DialogView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Centered, scrollable private presentation; the server owns every actionable choice. */
public class DialogScreen extends Screen {
    private DialogView view;
    private int left, top, pane, panelHeight, bodyTop, bodyBottom, scroll, total, choicePage, quoteViewport;

    DialogScreen(DialogView view) { super(Component.literal(view.title())); this.view = view; }
    DialogView view() { return view; }
    int panelLeft() { return left; }
    int panelWidth() { return pane; }
    int bodyViewportHeight() { return bodyBottom - bodyTop; }
    int scrollOffset() { return scroll; }
    void update(DialogView next) {
        if (!view.token().equals(next.token()) || next.revision() < view.revision()) return;
        view = next; rebuildWidgets();
    }
    private boolean paper() { return view.kind() == DialogView.Kind.EVENT || view.kind() == DialogView.Kind.GLOBAL_EVENT; }
    @Override protected void init() {
        pane = Math.min(width - 16, view.kind() == DialogView.Kind.SUPER_EVENT ? 600 : view.kind() == DialogView.Kind.STATE ? 440 : 450);
        panelHeight = Math.min(height - 16, view.kind() == DialogView.Kind.SUPER_EVENT ? pane * 3 / 5 + 48 : 530); left = (width - pane) / 2; top = (height - panelHeight) / 2;
        // Reserve readable body space before allocating two-column choice rows and paging controls.
        int capacity = 2 * Math.clamp((panelHeight - 60 - 40 - 34) / 24, 1, 3);
        int shown = Math.min(capacity, view.choices().size()), rows = (shown + 1) / 2;
        int footer = shown == 0 ? 30 : rows * 24 + 10 + (view.choices().size() > capacity ? 24 : 0);
        bodyTop = top + 60; bodyBottom = top + panelHeight - footer;
        addRenderableWidget(new HoiMenuButton("×", left + pane - 26, top + 5, 20, 20, this::onClose));
        int pages = Math.max(1, (view.choices().size() + capacity - 1) / capacity); choicePage = Math.clamp(choicePage, 0, pages - 1);
        if (shown == 0) addRenderableWidget(new HoiMenuButton("닫기", left + pane / 2 - 44, bodyBottom + 5, 88, 20, this::onClose));
        else for (int i = 0; i < shown; i++) {
            int index = choicePage * capacity + i; if (index >= view.choices().size()) break;
            var choice = view.choices().get(index); int cell = (pane - 28) / 2;
            int bx = shown == 1 ? left + (pane - cell) / 2 : left + 12 + (i % 2) * (cell + 4);
            addRenderableWidget(new HoiMenuButton(choice.label(), bx, bodyBottom + 5 + (i / 2) * 24,
                    cell, 20, () -> choose(choice.id())));
        }
        if (pages > 1) {
            int y = top + panelHeight - 25;
            var prev = new HoiMenuButton("이전 선택지", left + 12, y, 100, 20, () -> { choicePage--; rebuildWidgets(); }); prev.active = choicePage > 0; addRenderableWidget(prev);
            var next = new HoiMenuButton("다음 선택지", left + pane - 112, y, 100, 20, () -> { choicePage++; rebuildWidgets(); }); next.active = choicePage + 1 < pages; addRenderableWidget(next);
        }
        scroll = Math.clamp(scroll, 0, Math.max(0, total - (bodyBottom - bodyTop)));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x70000000);
        if (view.kind() == DialogView.Kind.SUPER_EVENT) {
            superEvent(g); super.extractRenderState(g, mx, my, delta); return;
        }
        if (paper()) {
            for (int i = 7; i > 0; i--) g.outline(left - i, top - i, pane + i * 2, panelHeight + i * 2, 0x208d599f);
            g.fill(left, top, left + pane, top + panelHeight, 0xffe7e5e8);
            g.fill(left, top, left + pane, top + 29, 0xff51395e);
        } else HoiMenuStyle.panel(g, left, top, pane, panelHeight);
        String caption = switch (view.kind()) {
            case STATE -> "주 정보"; case DIPLOMACY -> "외교 알림"; case EVENT -> "국가 이벤트 · 자국";
            case GLOBAL_EVENT -> "국제 뉴스 · 전 세계"; case SUPER_EVENT -> "SUPER EVENT · 전 세계"; case IDEOLOGIES -> "TFR 이념";
        };
        g.text(font, caption, left + 12, top + 11, HoiMenuStyle.TEXT);
        int ink = paper() ? 0xff29212b : HoiMenuStyle.TEXT;
        int start = left + 12;
        if (!view.flag().isEmpty() && UiAssets.draw(g, view.flag(), start, top + 34, 27, 18)) start += 35;
        g.text(font, trim(view.title(), left + pane - 14 - start), start, top + 35, ink);
        g.text(font, trim(view.subtitle(), pane - 28), left + 12, top + 48, paper() ? 0xff62566a : HoiMenuStyle.MUTED);
        g.enableScissor(left + 8, bodyTop, left + pane - 8, bodyBottom);
        int y = bodyTop + 5 - scroll;
        if (view.kind() != DialogView.Kind.EVENT) y = image(g, y);
        if (view.kind() != DialogView.Kind.STATE) {
            for (var line : font.split(Component.literal(view.body()), pane - 32)) { g.text(font, line, left + 15, y, ink); y += 13; }
            if (!view.body().isEmpty()) y += 10;
        }
        if (view.kind() == DialogView.Kind.EVENT) y = image(g, y);
        if (view.kind() == DialogView.Kind.STATE) {
            for (int i = 0; i < view.tiles().size(); i++) {
                var tile = view.tiles().get(i); int cell = (pane - 32) / 3;
                int x = left + 12 + (i % 3) * (cell + 4), ty = y + (i / 3) * 59;
                HoiMenuStyle.recess(g, x, ty, cell, 55);
                UiAssets.draw(g, tile.icon(), x + 5, ty + 5, 25, 23);
                g.text(font, trim(tile.name(), cell - 10), x + 5, ty + 31, HoiMenuStyle.TEXT);
                g.text(font, trim(tile.value(), cell - 39), x + 35, ty + 12, HoiMenuStyle.ACCENT);
            }
            y += ((view.tiles().size() + 2) / 3) * 59;
            y += 8;
            for (var line : font.split(Component.literal(view.body()), pane - 32)) { g.text(font, line, left + 15, y, ink); y += 13; }
        } else for (var tile : view.tiles()) {
            g.text(font, trim(tile.name() + (tile.value().isEmpty() ? "" : " · " + tile.value()), pane - 32), left + 15, y, ink); y += 17;
            for (var line : font.split(Component.literal(tile.detail()), pane - 40)) { g.text(font, line, left + 19, y, ink); y += 13; }
            y += 15;
        }
        total = y + scroll - bodyTop;
        g.disableScissor();
        int available = bodyBottom - bodyTop;
        if (total > available) {
            int thumb = Math.max(12, available * available / total);
            int sy = bodyTop + (available - thumb) * scroll / Math.max(1, total - available);
            g.fill(left + pane - 6, sy, left + pane - 3, sy + thumb, 0xffa876b6);
        }
        super.extractRenderState(g, mx, my, delta);
    }
    private void superEvent(GuiGraphicsExtractor g) {
        HoiMenuStyle.panel(g, left, top, pane, panelHeight);
        UiAssets.draw(g, view.image(), left + 7, top + 28, pane - 14, panelHeight - 58);
        HoiMenuStyle.panel(g, left + pane / 10, top - 3, pane * 4 / 5, 32);
        g.fill(left + pane / 10 + 5, top + 2, left + pane * 9 / 10 - 5, top + 23, 0xb039245f);
        g.centeredText(font, trim(view.title(), pane * 4 / 5 - 20), left + pane / 2, top + 10, HoiMenuStyle.TEXT);
        int quoteWidth = pane * 3 / 4;
        var lines = font.split(Component.literal(view.body()), quoteWidth - 24);
        int quoteHeight = Math.min(Math.max(36, lines.size() * 12 + 16), Math.max(36, panelHeight / 3));
        int qx = left + (pane - quoteWidth) / 2, qy = bodyBottom - quoteHeight - 4;
        g.fill(qx, qy, qx + quoteWidth, qy + quoteHeight, 0xd0442f64);
        g.outline(qx, qy, quoteWidth, quoteHeight, 0xfff0a1e1);
        g.enableScissor(qx + 5, qy + 5, qx + quoteWidth - 5, qy + quoteHeight - 5);
        int y = qy + 8 - scroll;
        for (var line : lines) { g.text(font, line, left + pane / 2 - font.width(line) / 2, y, HoiMenuStyle.TEXT); y += 12; }
        g.disableScissor(); total = lines.size() * 12 + 16;
        quoteViewport = quoteHeight;
    }
    private int image(GuiGraphicsExtractor g, int y) {
        if (view.image().isEmpty()) return y;
        int imageHeight = Math.min(view.kind() == DialogView.Kind.SUPER_EVENT ? 230 : 180, Math.max(75, panelHeight * 2 / 5));
        if (UiAssets.draw(g, view.image(), left + 16, y, pane - 32, imageHeight)) return y + imageHeight + 12;
        return y;
    }
    private String trim(String text, int space) { return font.width(text) <= space ? text : font.plainSubstrByWidth(text, Math.max(1, space - 9)) + "…"; }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= left && x <= left + pane && y >= bodyTop && y <= bodyBottom) {
            int visible = view.kind() == DialogView.Kind.SUPER_EVENT ? quoteViewport : bodyBottom - bodyTop;
            scroll = Math.clamp(scroll - (int)(vertical * 32), 0, Math.max(0, total - visible)); return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    protected void choose(String choice) { DialogClient.choose(view, choice); }
    @Override public void onClose() { choose(""); minecraft.gui.setScreen(null); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            if (view.choices().size() == 1) { choose(view.choices().getFirst().id()); return true; }
            if (view.choices().isEmpty()) { onClose(); return true; }
            // With multiple gameplay choices, use the keyboard-focused button. With no selection, dismiss only.
            if (getFocused() instanceof net.minecraft.client.gui.components.Button button && button.active)
                button.onPress(event);
            else onClose();
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public void removed() { SuperEventAudio.stop(view.token()); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
