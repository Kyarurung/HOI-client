package dev.hoi.client;

import dev.hoi.protocol.DialogView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Compact completion window; its optional details never execute completion effects. */
final class CompletionScreen extends DialogScreen {
    private final java.util.function.BiConsumer<DialogView, String> send;
    private int left, top, pane, panelHeight, bodyTop, bodyBottom, scroll, total;
    private boolean details;

    CompletionScreen(DialogView view) { this(view, DialogClient::choose); }
    CompletionScreen(DialogView view, java.util.function.BiConsumer<DialogView, String> send) {
        super(view); this.send = send;
    }
    @Override int panelLeft() { return left; }
    @Override int panelWidth() { return pane; }
    int scrollOffset() { return scroll; }
    @Override protected void init() {
        pane = Math.min(width - 16, 310); panelHeight = Math.min(height - 16, details ? 410 : 168);
        left = (width - pane) / 2; top = (height - panelHeight) / 2;
        bodyTop = top + 34; bodyBottom = top + panelHeight - 34;
        int buttonWidth = Math.min(96, (pane - 38) / 2);
        addRenderableWidget(new HoiMenuButton(details ? "간단히" : "세부 사항", left + 14, bodyBottom + 7, buttonWidth, 20,
                () -> { details = !details; scroll = 0; rebuildWidgets(); }));
        addRenderableWidget(new HoiMenuButton("확인", left + pane - buttonWidth - 14, bodyBottom + 7, buttonWidth, 20,
                () -> send.accept(view(), "ack")));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
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
        if (details) {
            y += 8;
            for (var line : font.split(Component.literal(view.body()), pane - 30)) {
                g.text(font, line, left + 15, y, HoiMenuStyle.TEXT); y += 13;
            }
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
    @Override public void onClose() { send.accept(view(), ""); minecraft.gui.setScreen(null); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            send.accept(view(), "ack"); return true;
        }
        return super.keyPressed(event);
    }
}
