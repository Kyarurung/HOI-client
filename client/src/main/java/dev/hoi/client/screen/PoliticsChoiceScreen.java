package dev.hoi.client.screen;

import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.HoiMenuButton;
import dev.hoi.client.ui.HoiMenuStyle;
import dev.hoi.client.ui.HoiPanelLayout;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.DialogView;
import dev.hoi.protocol.MenuTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class PoliticsChoiceScreen extends DialogScreen {
    private final Screen parent;
    private int left, top, pane, bottom, scroll;
    PoliticsChoiceScreen(DialogView view, Screen parent) { super(view); this.parent = parent; }
    void dismiss() { minecraft.gui.setScreen(parent); }
    @Override protected void init() {
        left = HoiPanelLayout.width(MenuTab.POLITICS, width);
        top = HoiMenuBar.height(width);
        pane = Math.min(240, width - left - 6);
        if (pane < 170) { pane = Math.min(240, width - 12); left = width - pane - 6; }
        bottom = Math.min(height - 10, top + 300);
        addRenderableWidget(new HoiMenuButton("×", left + pane - 23, top + 3, 19, 19, this::onClose));
        int start = top + 29;
        scroll = Math.clamp(scroll, 0, Math.max(0, view().tiles().size() * 39 - (bottom - start)));
        for (int i = 0; i < view().tiles().size(); i++) {
            var tile = view().tiles().get(i); int y = start + i * 39 - scroll;
            if (y < start || y + 36 > bottom) continue;
            var button = new HoiMenuButton(tile.name(), left + 6, y, pane - 12, 36,
                    () -> DialogClient.choose(view(), tile.section())).caption("");
            button.active = view().choices().stream().anyMatch(c -> c.id().equals(tile.section()));
            button.setTooltip(tile.name().equals("공석") || tile.icon().equals("politics/vacant") ? null : Tooltip.create(Component.literal(tile.name() + "\n" + tile.detail())));
            addRenderableWidget(button);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (parent != null) { parent.extractRenderState(g, mx < left ? mx : -1, mx < left ? my : -1, delta); g.nextStratum(); }
        HoiMenuStyle.panel(g, left, top, pane, bottom - top + 4);
        dev.hoi.client.ui.UiText.centeredAt(g, font, view().title(), left + (pane - 24) / 2, top + (25 - font.lineHeight) / 2, HoiMenuStyle.TEXT);
        superWidgets(g, mx, my, delta);
        int start = top + 29;
        for (int i = 0; i < view().tiles().size(); i++) {
            var tile = view().tiles().get(i); int y = start + i * 39 - scroll;
            if (y < start || y + 36 > bottom) continue;
            if (tile.value().equals("적용 중")) g.fill(left + 7, y + 1, left + pane - 7, y + 35, 0x603F6D1D);
            UiAssets.draw(g, tile.icon(), left + 9, y + 3, 30, 30);
            int textLeft = left + 43, textWidth = pane - 53;
            dev.hoi.client.ui.UiText.centered(g, font, tile.name(), textLeft, y + 3, textWidth, 13, HoiMenuStyle.TEXT);
            dev.hoi.client.ui.UiText.centered(g, font, tile.value(), textLeft, y + 20, textWidth, 12, HoiMenuStyle.ACCENT);
        }
        if (view().tiles().isEmpty()) dev.hoi.client.ui.UiText.centeredAt(g, font, font.plainSubstrByWidth(view().body(), pane - 12), left + pane / 2, start + 12, HoiMenuStyle.MUTED);
    }
    private void superWidgets(GuiGraphicsExtractor g, int mx, int my, float delta) {
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget) widget.extractRenderState(g, mx, my, delta);
    }
    Screen backdrop() { return parent; }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (parent != null && event.x() < left) {
            dismiss();
            return parent.mouseClicked(event, twice);
        }
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x < left || x > left + pane) return false;
        scroll -= (int)(vertical * 39); rebuildWidgets(); return true;
    }
    @Override public void onClose() { DialogClient.choose(view(), ""); dismiss(); }
}
