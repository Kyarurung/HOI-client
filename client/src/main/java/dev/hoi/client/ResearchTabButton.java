package dev.hoi.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Pictogram tabs retain a full accessible label and tooltip at compact resolutions. */
final class ResearchTabButton extends Button {
    private final String category;
    private final boolean selected;

    ResearchTabButton(String label, String category, boolean selected, int x, int y, int width, Runnable action) {
        super(x, y, width, 35, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        this.category = category; this.selected = selected;
        setTooltip(Tooltip.create(Component.literal(label)));
    }

    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth();
        int color = selected || isHoveredOrFocused() ? 0xFFE6C779 : 0xFF92AD83;
        int top = selected ? y : y + 3;
        g.fillGradient(x, top, x + w, y + getHeight() + 1, selected ? 0xFF35433D : 0xFF252E2A, selected ? 0xFF101A21 : 0xFF141A18);
        g.horizontalLine(x, x + w - 1, top, color);
        g.verticalLine(x, top, y + getHeight(), color);
        g.verticalLine(x + w - 1, top, y + getHeight(), color);
        // The selected tab opens directly into the shared research frame.
        if (selected) g.fill(x + 1, y + getHeight(), x + w - 1, y + getHeight() + 2, 0xFF101A21);
        else g.horizontalLine(x, x + w - 1, y + getHeight(), 0xFF778178);
        int iconWidth = Math.min(54, w - 6);
        if (!UiAssets.draw(g, "tabs/" + category.toLowerCase(java.util.Locale.ROOT),
                x + (w - iconWidth) / 2, y + 3, iconWidth, 29))
            ResearchIcons.fallback(g, category, x + w / 2 - 10, y + 9, color, 2);
    }
}
