package dev.hoi.client.ui;

import dev.hoi.client.audio.UiSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;


public class PanelButton extends Button {
    private double progress = -1;
    private String clickSound = "ui.click";
    public void sound(String value) { clickSound = value; }
    public PanelButton(String label, int x, int y, int width, int height, Runnable action) {
        super(x, y, width, height, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
    }
    public void progress(double value) { progress = value; }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { UiSounds.play(getMessage().getString().equals("×") ? "ui.close" : clickSound); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean focused = false;
        g.fill(x, y, x + w, y + h, focused ? 0xFF344755 : active ? 0xFF24343F : 0xFF1A242C);
        g.outline(x, y, w, h, focused ? 0xFFE6C779 : 0xFF4A5B66);
        if (progress >= 0) g.fill(x + 1, y + h - 3, x + 1 + (int)((w - 2) * progress), y + h - 1, 0xFF74BCD8);
        if (getMessage().getString().equals("×") || getMessage().getString().equals("X")) {
            HoiMenuStyle.close(g, x, y, w, h, active ? 0xFFE0E6E8 : 0xFF81909A);
            return;
        }
        g.centeredText(Minecraft.getInstance().font, getMessage(), x + w / 2, y + (h - 8) / 2, active ? 0xFFE0E6E8 : 0xFF81909A);
    }
}
