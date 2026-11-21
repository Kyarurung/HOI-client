package dev.hoi.client.ui.hud;

import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.screen.ScreenKoreanMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class HudMusicButton extends Button {
    private boolean hovering;
    public static int x(int width) { return Math.round(HudTimeControls.x(width, 88)); }
    static int y(int width) { return Math.round(HudTimeControls.y(width, 48)); }
    static int size(int width) { return Math.round(33 * HudTimeControls.unit(width)); }
    public HudMusicButton(int width) {
        super(x(width), y(width), size(width), size(width), Component.literal("플레이리스트 목록"), button -> {
            var client = Minecraft.getInstance();
            client.gui.setScreen(new ScreenKoreanMusic(client.gui.screen()));
        }, DEFAULT_NARRATION);
        setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal("플레이리스트 목록")));
    }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play("ui.confirm"); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (isHovered() && !hovering) AudioUiSounds.play("ui.menu_over");
        hovering = isHovered();
    }
}
