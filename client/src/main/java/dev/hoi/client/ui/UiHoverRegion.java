package dev.hoi.client.ui;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class UiHoverRegion extends AbstractWidget {
    public UiHoverRegion(String label, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal(label));
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}
    @Override protected void handleCursor(GuiGraphicsExtractor graphics) {}
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) { return false; }
    @Override public boolean mouseReleased(MouseButtonEvent event) { return false; }
    @Override public boolean mouseDragged(MouseButtonEvent event, double x, double y) { return false; }
    @Override public ComponentPath nextFocusPath(FocusNavigationEvent event) { return null; }
    @Override public void setFocused(boolean focused) {}
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }
}
