package dev.hoi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Inset menu controls; an optional image replaces the visible caption only. */
final class HoiMenuButton extends Button {
    private final String icon;
    private final boolean selected;

    HoiMenuButton(String label, int x, int y, int w, int h, Runnable action) {
        this(label, null, false, x, y, w, h, action);
    }

    HoiMenuButton(String label, String icon, boolean selected, int x, int y, int w, int h, Runnable action) {
        super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        this.icon = icon; this.selected = selected;
        setTooltip(Tooltip.create(Component.literal(label)));
    }

    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        HoiMenuStyle.control(g, x, y, w, h, selected, active && isHoveredOrFocused());
        var font = Minecraft.getInstance().font;
        if (icon != null) {
            if (!UiAssets.draw(g, icon, x + 5, y + 3, w - 10, h - 7))
                g.centeredText(font, "◇", x + w / 2, y + (h - 8) / 2, HoiMenuStyle.ACCENT);
        } else {
            String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(1, w - 8));
            g.centeredText(font, label, x + w / 2, y + (h - 8) / 2, active ? HoiMenuStyle.TEXT : HoiMenuStyle.MUTED);
        }
    }
}
