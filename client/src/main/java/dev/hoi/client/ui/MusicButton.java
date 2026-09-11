package dev.hoi.client.ui;

import dev.hoi.client.audio.UiSounds;
import dev.hoi.client.screen.KoreanMusicScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class MusicButton extends Button {
    private static final int SIZE = 18, Y = 17;
    public static int x(int width) { return HoiMenuBar.statsRight(width) - 43; }
    public MusicButton(int width) {
        super(x(width), Y, SIZE, SIZE, Component.literal("음악 선택"), button -> {
            var client = Minecraft.getInstance();
            client.gui.setScreen(new KoreanMusicScreen(client.gui.screen()));
        }, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.literal("음악 선택")));
    }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { UiSounds.play("ui.click"); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        UiAssets.draw(g, "hud/music_player", getX(), getY(), getWidth(), getHeight());
    }
    public static void draw(GuiGraphicsExtractor g, int width) {
        UiAssets.draw(g, "hud/music_player", x(width), Y, SIZE, SIZE);
    }
}
