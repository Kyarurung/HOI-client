package dev.hoi.client.screen;

import dev.hoi.client.ui.HoiMenuButton;
import dev.hoi.client.ui.HoiMenuStyle;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.DialogView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;


final class CompletionScreen extends DialogScreen {
    private final java.util.function.BiConsumer<DialogView, String> send;
    private net.minecraft.client.gui.screens.Screen backdrop;
    private int left, top, pane, panelHeight, bodyTop, bodyBottom, scroll, total;

    CompletionScreen(DialogView view) { this(view, DialogClient::choose); }
    CompletionScreen(DialogView view, net.minecraft.client.gui.screens.Screen backdrop) {
        this(view); this.backdrop = backdrop;
    }
    net.minecraft.client.gui.screens.Screen backdrop() { return backdrop; }
    void dismiss() {
        var parent = backdrop; backdrop = null;
        minecraft.gui.setScreen(parent);
    }
    @Override public void tick() { if (backdrop != null) backdrop.tick(); }
    @Override public void removed() {
        super.removed();
        if (backdrop != null && !DialogClient.suspending()) { backdrop.removed(); backdrop = null; }
    }
    CompletionScreen(DialogView view, java.util.function.BiConsumer<DialogView, String> send) {
        super(view); this.send = send;
    }
    @Override int panelLeft() { return left; }
    @Override int panelWidth() { return pane; }
    int scrollOffset() { return scroll; }
    @Override protected void init() {
        pane = Math.min(width - 16, 310); panelHeight = Math.min(height - 16, 168);
        left = (width - pane) / 2; top = (height - panelHeight) / 2;
        bodyTop = top + 34; bodyBottom = top + panelHeight - 34;
        int buttonWidth = Math.min(96, (pane - 38) / 2);
        var details = addRenderableWidget(new HoiMenuButton("세부 사항", left + 14, bodyBottom + 7, buttonWidth, 20,
                () -> send.accept(view(), "details")));
        details.active = view().choices().stream().anyMatch(c -> c.id().equals("details"));
        addRenderableWidget(new HoiMenuButton("확인", left + pane - buttonWidth - 14, bodyBottom + 7, buttonWidth, 20,
                () -> send.accept(view(), "ack")));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (backdrop != null) {
            backdrop.extractRenderState(g, -1, -1, delta);
            g.nextStratum();
        }
        var view = view();
        g.fill(0, 0, width, height, 0x50000000);
        HoiMenuStyle.panel(g, left, top, pane, panelHeight);
        g.centeredText(font, view.presentation().equals("research_complete") ? "연구 완료!" : "국가 중점 완료", left + pane / 2, top + 12, HoiMenuStyle.TEXT);
        g.enableScissor(left + 8, bodyTop, left + pane - 8, bodyBottom);
        int y = bodyTop + 2 - scroll;
        if (!UiAssets.draw(g, view.image(), left + pane / 2 - 56, y, 112, 52))
            UiAssets.draw(g, view.presentation().equals("research_complete") ? "menu/research" : "menu/focus", left + pane / 2 - 22, y + 4, 44, 44);
        y += 57;
        for (var line : font.split(Component.literal(view.title()), pane - 30)) {
            g.centeredText(font, line, left + pane / 2, y, 0xFFE6C779); y += 13;
        }
        total = y + 6 + scroll - bodyTop;
        g.disableScissor();
        int available = bodyBottom - bodyTop;
        if (total > available) {
            int thumb = Math.max(8, available * available / total);
            int sy = bodyTop + (available - thumb) * scroll / Math.max(1, total - available);
            g.fill(left + pane - 6, sy, left + pane - 3, sy + thumb, 0xff969997);
        }
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget)
            widget.extractRenderState(g, mx, my, delta);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= left && x <= left + pane && y >= bodyTop && y <= bodyBottom) {
            scroll = Math.clamp(scroll - (int)(vertical * 32), 0, Math.max(0, total - (bodyBottom - bodyTop))); return true;
        }
        return false;
    }
    @Override public void onClose() { send.accept(view(), ""); dismiss(); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            send.accept(view(), "ack"); return true;
        }
        return super.keyPressed(event);
    }
}
