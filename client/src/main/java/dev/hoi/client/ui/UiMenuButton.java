package dev.hoi.client.ui;

import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.ui.tooltip.Tooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class UiMenuButton extends Button {
    private final String icon;
    private final boolean selected;
    private float textScale = 1;
    private String caption;
    private String background;
    private String sound = "ui.click";
    private boolean textShadow = true;
    private boolean wrapLabel;
    private boolean bold;
    public UiMenuButton bold() { bold = true; return this; }
    public UiMenuButton wrapLabel() { wrapLabel = true; return this; }
    public static int labelHeight(Font font, String label, int width, float scale) {
        float renderedScale = Math.max(UiText.scale(font), scale + 1f / font.lineHeight);
        int lines = Tooltips.split(font,Component.literal(label), Math.max(1, (int)((width - 6) / renderedScale))).size();
        return (int)Math.ceil((lines * (font.lineHeight + 1) - 1) * renderedScale) + (lines > 1 ? 6 : 2);
    }
    public UiMenuButton textShadow(boolean value) { textShadow = value; return this; }
    public UiMenuButton sound(String value) { sound = value; return this; }
    public void textScale(float scale) { textScale = scale; }
    public UiMenuButton caption(String value) { caption = value; if (isClose(value)) setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal("닫기"))); return this; }
    private static boolean isClose(String value) { return value.equals("×") || value.equals("x") || value.equals("X"); }
    public UiMenuButton background(String value) { background = value; return this; }
    public UiMenuButton(String label, int x, int y, int w, int h, Runnable action) {
        this(label, null, false, x, y, w, h, action);
    }
    public UiMenuButton(String label, String icon, boolean selected, int x, int y, int w, int h, Runnable action) {
        super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        this.icon = icon; this.selected = selected;
        setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal(isClose(label) ? "닫기" : label)));
    }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play(isClose(caption == null ? getMessage().getString() : caption) ? "ui.close" : sound); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        if (wrapLabel && ("events/leader/choice".equals(background) || "events/frame/spacebar".equals(background)) && h > UiAssets.imageHeight(background, w)) UiAssets.nineSlice(g, background, x, y, w, h, 4, w / ("events/frame/spacebar".equals(background) ? 300.0 : 352.0));
        else if (background != null && background.startsWith("agency/ui/tab")) UiAssets.nineSlice(g, background, x, y, w, h, 20, .5);
        else if (!"none".equals(background) && (background == null || !(background.equals("agency/ui/category") ? UiAssets.drawContent(g, background, x, y, w, h) : UiAssets.draw(g, background, x, y, w, h)))) UiMenuStyle.control(g, x, y, w, h, selected, false);
        String displayed = caption == null ? getMessage().getString() : caption;
        if (icon == null && UiMenuStyle.symbol(g, displayed, x, y, w, h, active ? UiMenuStyle.TEXT : UiMenuStyle.MUTED)) return;
        var font = Minecraft.getInstance().font;
        if (icon != null) {
            if (icon.equals("events/leader/minimize")) { UiAssets.draw(g, icon, x, y, w, h); return; }
            if (!UiAssets.draw(g, icon, x + 5, y + 3, w - 10, h - 7))
                g.centeredText(font, "◇", x + w / 2, y + (h - 8) / 2, UiMenuStyle.ACCENT);
        } else {
            var captionText = Component.literal(displayed).withStyle(style -> style.withBold(bold));
            float scale = Math.max(UiText.scale(font), textScale + 1f / font.lineHeight);
            if (wrapLabel && font.width(captionText) * scale > w - 6) {
                var lines = Tooltips.split(font,captionText, Math.max(1, (int)((w - 6) / scale)));
                float height = (lines.size() * (font.lineHeight + 1) - 1) * scale;
                g.pose().pushMatrix(); g.pose().translate(x + w / 2f, y + (h - height) / 2); g.pose().scale(scale);
                for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), -font.width(lines.get(i)) / 2, i * (font.lineHeight + 1), active ? UiMenuStyle.TEXT : UiMenuStyle.MUTED, textShadow);
                g.pose().popMatrix(); return;
            }
            if (font.width(captionText) * scale > w - 6 && h >= 24) {
                UiText.centered(g, font, captionText, x + 3, y + 1, w - 6, h - 2, active ? UiMenuStyle.TEXT : UiMenuStyle.MUTED, textShadow); return;
            }
            var label = net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(captionText, Math.max(1, (int)((w - 6) / scale))));
            g.pose().pushMatrix(); g.pose().translate(x + w / 2f, y + (h - font.lineHeight * scale) / 2f); g.pose().scale(scale);
            g.text(font, label, -font.width(label) / 2, 0, active ? UiMenuStyle.TEXT : UiMenuStyle.MUTED, textShadow);
            g.pose().popMatrix();
        }
    }
}
