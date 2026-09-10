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
        pane = Math.min(width - 16, 250); panelHeight = Math.min(height - 16, 130);
        left = (width - pane) / 2; top = (height - panelHeight) / 2;
        bodyTop = top + 36; bodyBottom = top + panelHeight - 30;
        int buttonWidth = Math.min(62, (pane - 38) / 2);
        var details = addRenderableWidget(new HoiMenuButton("세부 사항", left + 20, bodyBottom + 1, buttonWidth, 17,
                () -> send.accept(view(), "details")).background("completion/button"));
        details.active = view().choices().stream().anyMatch(c -> c.id().equals("details"));
        addRenderableWidget(new HoiMenuButton("확인", left + pane - buttonWidth - 18, bodyBottom + 1, buttonWidth, 17,
                () -> send.accept(view(), "ack")).background("completion/button"));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (backdrop != null) {
            backdrop.extractRenderState(g, -1, -1, delta);
            g.nextStratum();
        }
        var view = view();
        HoiMenuStyle.panel(g, left, top, pane, panelHeight);
        UiAssets.draw(g, "completion/header", left + 1, top + 4, pane - 2, 43);
        UiAssets.draw(g, "completion/" + (view.presentation().equals("research_complete") ? "research" : "focus"), left + 7, bodyTop, pane - 14, 63);
        UiAssets.draw(g, "completion/bottom", left + 2, top + 95, pane - 4, 36);
        g.centeredText(font, view.presentation().equals("research_complete") ? "기술 완료" : "국가중점 완료", left + pane / 2, top + 20 - font.lineHeight / 2, HoiMenuStyle.TEXT);
        g.enableScissor(left + 8, bodyTop, left + pane - 8, bodyBottom);
        int y = bodyTop + 2 - scroll;
        if (!UiAssets.draw(g, view.image(), left + pane / 2 - 56, y - 4, 112, 52))
            UiAssets.draw(g, view.presentation().equals("research_complete") ? "menu/research" : "menu/focus", left + pane / 2 - 22, y + 4, 44, 44);
        g.pose().pushMatrix(); g.pose().translate(left + pane / 2f, bodyTop + 48); g.pose().scale(.75f);
        g.centeredText(font, font.plainSubstrByWidth(view.title(), (int)((pane - 30) / .75)), 0, 0, 0xFFE6C779);
        g.pose().popMatrix();
        total = bodyBottom - bodyTop;
        g.disableScissor();
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
