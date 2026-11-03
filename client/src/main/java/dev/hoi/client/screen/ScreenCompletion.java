package dev.hoi.client.screen;

import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.ProtocolDialogView;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class ScreenCompletion extends ScreenDialog {
    private final java.util.function.BiConsumer<ProtocolDialogView, String> send;
    private net.minecraft.client.gui.screens.Screen backdrop;
    private int left, top, pane, panelHeight, bodyTop, bodyBottom, scroll, total;
    ScreenCompletion(ProtocolDialogView view) { this(view, ScreenDialogCoordinator::choose); }
    ScreenCompletion(ProtocolDialogView view, net.minecraft.client.gui.screens.Screen backdrop) {
        this(view); this.backdrop = backdrop;
    }
    net.minecraft.client.gui.screens.Screen backdrop() { return backdrop; }
    void dismiss() {
        if (stacked) { ScreenDialogCoordinator.close(view().token()); return; }
        var parent = backdrop; backdrop = null;
        minecraft.gui.setScreen(parent);
    }
    @Override public void tick() { if (backdrop != null) backdrop.tick(); }
    @Override public void removed() {
        super.removed();
        if (backdrop != null && !ScreenDialogCoordinator.suspending()) { backdrop.removed(); backdrop = null; }
    }
    ScreenCompletion(ProtocolDialogView view, java.util.function.BiConsumer<ProtocolDialogView, String> send) {
        super(view); this.send = send;
    }
    @Override int panelTop() { return top; }
    @Override int panelHeight() { return panelHeight; }
    @Override int panelLeft() { return left; }
    @Override int panelWidth() { return pane; }
    int scrollOffset() { return scroll; }
    @Override protected void init() {
        pane = Math.min(width - 16, 250); panelHeight = Math.min(height - 16, 130);
        left = (width - pane) / 2; top = (height - panelHeight) / 2;
        bodyTop = top + 36; bodyBottom = top + panelHeight - 30;
        int buttonWidth = Math.min(62, (pane - 38) / 2);
        var details = addRenderableWidget(new UiMenuButton("세부 사항", left + 20, bodyBottom + 1, buttonWidth, 17,
                () -> respond("details")).background("completion/button").sound(""));
        details.setTooltip(null);
        details.active = view().choices().stream().anyMatch(c -> c.id().equals("details"));
        addRenderableWidget(new UiMenuButton("확인", left + 170, bodyBottom + 1, buttonWidth, 17,
                () -> respond("ack")).background("completion/button").sound(""));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (backdrop != null) {
            backdrop.extractRenderState(g, -1, -1, delta);
            g.nextStratum();
        }
        var view = view();
        UiMenuStyle.panel(g, left, top, pane, panelHeight);
        UiAssets.draw(g, "completion/header", left + 1, top + 4, pane - 2, 43);
        boolean research = view.presentation().equals("research_complete");
        boolean agency = view.presentation().equals("agency_upgrade_complete");
        int innerTop = top + 36;
        if (research) g.fill(left + 7, bodyTop, left + pane - 7, bodyTop + 63, 0xFF161616);
        else if (agency) UiAssets.draw(g, "completion/research", left + 7, top + 38, pane - 13, 61);
        else UiAssets.draw(g, "completion/focus", left + 8, innerTop, 235, 69);
        UiAssets.draw(g, "completion/bottom", left + 2, top + panelHeight - 35, pane - 4, 36);
        dev.hoi.client.ui.UiText.centeredAt(g, font, dev.hoi.client.ui.UiTooltips.officerText(research ? "연구 완료" : agency ? "첩보기관 개선 완료!" : "국가중점 완료", true), left + pane / 2, top + 20 - font.lineHeight / 2 + (research || agency ? 2 : 0), UiMenuStyle.TEXT);
        g.enableScissor(left + 8, bodyTop, left + pane - 8, bodyBottom);
        int imageHeight = research ? 38 : 60;
        int imageWidth = research ? 112 : 128;
        int y = top + 65 - imageHeight / 2 - scroll;
        if (!research) {
            int[] size = UiAssets.imageSize(view.image());
            int w = (int)Math.round(size[0] * pane / 500.0), h = (int)Math.round(size[1] * pane / 500.0);
            UiAssets.draw(g, view.image(), left + pane / 2 - w / 2, top + 65 - h / 2, w, h);
        } else if (!UiAssets.drawContent(g, view.image(), left + (pane - imageWidth) / 2, y, imageWidth, imageHeight))
            UiAssets.draw(g, research ? "menu/research" : "menu/focus", left + pane / 2 - 22, y + (imageHeight - 44) / 2, 44, 44);
        if (research) view.tiles().stream().filter(tile -> tile.section().equals("design_team") && !tile.icon().isEmpty()).findFirst().ifPresent(team -> {
            int[] size = UiAssets.imageSize(team.icon());
            double scale = Math.min(pane / 500.0, 52.0 / Math.max(1, Math.max(size[0], size[1])));
            int w = (int)Math.round(size[0] * scale), h = (int)Math.round(size[1] * scale);
            UiAssets.draw(g, team.icon(), left + pane * 400 / 500 - w / 2, top + 65 - h / 2 - scroll, w, h);
        });
        g.nextStratum();
        if (!research && !agency) for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
            if (dx != 0 || dy != 0) dev.hoi.client.ui.UiText.centered(g, font, view.title(), left + 35 + dx,
                    top + 83 + dy, pane - 70, 12, 0xFF16110B, false);
        }
        g.nextStratum();
        dev.hoi.client.ui.UiText.centered(g, font, view.title(), left + 35,
                top + (research || agency ? 85 : 83), pane - 70, 12, research || agency ? 0xFFE6C779 : 0xFFFFBD00, research || agency);
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
    private void respond(String choice) {
        dev.hoi.client.audio.AudioUiSounds.play(choice.equals("ack") ? "ui.confirm" : "ui.click");
        send.accept(view(), choice);
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        return event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || super.mouseClicked(event, twice);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || super.mouseReleased(event);
    }
    @Override public void onClose() { send.accept(view(), ""); dismiss(); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
            respond("ack"); return true;
        }
        return super.keyPressed(event);
    }
}
