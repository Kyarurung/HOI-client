package dev.hoi.client.ui.hud;

import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.screen.ScreenKoreanMusic;
import dev.hoi.client.ui.UiAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class HudMusicButton extends Button {
    private static final int SIZE = 18, Y = 17;
    public static int x(int width) { return HudMenuBar.tensionX(width) - SIZE - 5; }
    public HudMusicButton(int width) {
        super(x(width), Y, SIZE, SIZE, Component.literal("플레이리스트 목록"), button -> {
            var client = Minecraft.getInstance();
            client.gui.setScreen(new ScreenKoreanMusic(client.gui.screen()));
        }, DEFAULT_NARRATION);
        setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal("플레이리스트 목록")));
    }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play("ui.click"); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (dev.hoi.client.screen.dialog.DialogCoordinator.renderingContent()) return;
        UiAssets.draw(g, "music/toggle", getX(), getY(), getWidth(), getHeight());
    }
    public static void draw(GuiGraphicsExtractor g, int width) {
        UiAssets.draw(g, "music/toggle", x(width), Y, SIZE, SIZE);
    }
}
