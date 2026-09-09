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
        super(x, y, width, heightFor(width), Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        this.category = category; this.selected = selected;
        setTooltip(Tooltip.create(Component.literal(label)));
    }

    static int heightFor(int width) { return (int)Math.ceil(width * 61.0 / 86); }

    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { UiSounds.play("ui.research.tab." + category.toLowerCase(java.util.Locale.ROOT)); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth();
        if (selected) g.fill(x + 2, y + getHeight() - 5, x + w - 2, y + getHeight() + 1, 0xFF101A21);
        String art = "tabs/" + category.toLowerCase(java.util.Locale.ROOT) + (selected ? "_selected" : "");
        // The unselected 91px asset has 2px/3px transparent side gutters.
        // Remove those gutters so adjacent buttons also have adjacent visible frames.
        if (!UiAssets.draw(g, art, x, y, w, getHeight(), selected ? 0 : 2, selected ? 0 : 3))
            ResearchIcons.fallback(g, category, x + w / 2 - 10, y + 9, 0xFF92AD83, 2);
        if (isHoveredOrFocused()) g.outline(x + 2, y + 4, w - 4, getHeight() - 6, 0xFF9B9C96);
    }
}
