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
    private String background;

    public void textScale(float scale) { textScale = scale; }
    public HoiMenuButton caption(String value) { caption = value; if (isClose(value)) setTooltip(Tooltip.create(Component.literal("닫기"))); return this; }
    private static boolean isClose(String value) { return value.equals("×") || value.equals("x") || value.equals("X"); }
    public HoiMenuButton background(String value) { background = value; return this; }

    public HoiMenuButton(String label, int x, int y, int w, int h, Runnable action) {
        this(label, null, false, x, y, w, h, action);
    }

    public HoiMenuButton(String label, String icon, boolean selected, int x, int y, int w, int h, Runnable action) {
        super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        this.icon = icon; this.selected = selected;
        setTooltip(Tooltip.create(Component.literal(isClose(label) ? "닫기" : label)));
    }

    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { UiSounds.play(getMessage().getString().equals("×") ? "ui.close" : "ui.click"); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        if (!"none".equals(background) && (background == null || !UiAssets.draw(g, background, x, y, w, h))) HoiMenuStyle.control(g, x, y, w, h, selected, false);
        String displayed = caption == null ? getMessage().getString() : caption;
        if (icon == null && HoiMenuStyle.symbol(g, displayed, x, y, w, h, active ? HoiMenuStyle.TEXT : HoiMenuStyle.MUTED)) return;
        var font = Minecraft.getInstance().font;
        if (icon != null) {
            if (!UiAssets.draw(g, icon, x + 5, y + 3, w - 10, h - 7))
                g.centeredText(font, "◇", x + w / 2, y + (h - 8) / 2, HoiMenuStyle.ACCENT);
        } else {
            float scale = Math.max(UiText.scale(font), textScale + 1f / font.lineHeight);
            if (font.width(displayed) * scale > w - 6 && h >= 24) {
                UiText.centered(g, font, displayed, x + 3, y + 1, w - 6, h - 2, active ? HoiMenuStyle.TEXT : HoiMenuStyle.MUTED); return;
            }
            String label = font.plainSubstrByWidth(displayed, Math.max(1, (int)((w - 6) / scale)));
            g.pose().pushMatrix(); g.pose().translate(x + w / 2f, y + (h - font.lineHeight * scale) / 2f); g.pose().scale(scale);
            g.centeredText(font, label, 0, 0, active ? HoiMenuStyle.TEXT : HoiMenuStyle.MUTED);
            g.pose().popMatrix();
        }
    }
}
