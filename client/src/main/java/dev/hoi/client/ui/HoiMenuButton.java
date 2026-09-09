package dev.hoi.client.ui;

import dev.hoi.client.audio.UiSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;


public final class HoiMenuButton extends Button {
    private final String icon;
    private final boolean selected;
    private float textScale = 1;
    private String caption;

    public void textScale(float scale) { textScale = scale; }
    public HoiMenuButton caption(String value) { caption = value; return this; }

    public HoiMenuButton(String label, int x, int y, int w, int h, Runnable action) {
        this(label, null, false, x, y, w, h, action);
    }

    public HoiMenuButton(String label, String icon, boolean selected, int x, int y, int w, int h, Runnable action) {
        super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        this.icon = icon; this.selected = selected;
        setTooltip(Tooltip.create(Component.literal(label)));
    }

    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { UiSounds.play(getMessage().getString().equals("×") ? "ui.close" : "ui.click"); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        HoiMenuStyle.control(g, x, y, w, h, selected, active && isHoveredOrFocused());
        var font = Minecraft.getInstance().font;
        if (icon != null) {
            if (!UiAssets.draw(g, icon, x + 5, y + 3, w - 10, h - 7))
                g.centeredText(font, "◇", x + w / 2, y + (h - 8) / 2, HoiMenuStyle.ACCENT);
        } else {
            String label = font.plainSubstrByWidth(caption == null ? getMessage().getString() : caption, Math.max(1, (int)((w - 8) / textScale)));
            if (textScale == 1) {
                g.centeredText(font, label, x + w / 2, y + (h - 8) / 2, active ? HoiMenuStyle.TEXT : HoiMenuStyle.MUTED);
                return;
            }
            g.pose().pushMatrix(); g.pose().translate(x + w / 2f, y + (h - 8 * textScale) / 2f); g.pose().scale(textScale);
            g.centeredText(font, label, 0, 0, active ? HoiMenuStyle.TEXT : HoiMenuStyle.MUTED);
            g.pose().popMatrix();
        }
    }
}
