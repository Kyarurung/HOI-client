package dev.hoi.client.ui;

import dev.hoi.client.audio.AudioUiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class UiPanelButton extends Button {
    private double progress = -1;
    private String clickSound = "ui.click";
    public void sound(String value) { clickSound = value; }
    public UiPanelButton(String label, int x, int y, int width, int height, Runnable action) {
        super(x, y, width, height, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        if (label.equals("×") || label.equals("x") || label.equals("X")) setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal("닫기")));
    }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play(getMessage().getString().equals("×") ? "ui.close" : clickSound); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        if (getMessage().getString().equals("×") || getMessage().getString().equals("X")) { UiAssets.draw(g, "menu/close", x, y, w, h); return; }
        boolean focused = false;
        g.fill(x, y, x + w, y + h, focused ? 0xFF344755 : active ? 0xFF24343F : 0xFF1A242C);
        g.outline(x, y, w, h, focused ? 0xFFE6C779 : 0xFF4A5B66);
        if (progress >= 0) g.fill(x + 1, y + h - 3, x + 1 + (int)((w - 2) * progress), y + h - 1, 0xFF74BCD8);
        var font = Minecraft.getInstance().font;
        float scale = UiText.scale(font);
        UiText.marquee(g, font, net.minecraft.locale.Language.getInstance().getVisualOrder(dev.hoi.client.ui.tooltip.Tooltips.officerText(getMessage().getString())), x + 3, y + (h - font.lineHeight * scale) / 2f, w - 6, scale, active ? 0xFFE0E6E8 : 0xFF81909A, true, 0);
    }
}
