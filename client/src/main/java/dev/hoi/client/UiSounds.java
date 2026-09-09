package dev.hoi.client;

import dev.hoi.protocol.AudioProtocol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Audio stays in the external pack. UI cues are relative, never world-positioned. */
final class UiSounds {
    private static final Identifier THEME = id("music.tfr_theme");
    private static SoundInstance theme;
    private static int retry;
    private static final LobbyAudio LOBBY = new LobbyAudio(new LobbyAudio.Output() {
        public void stopTheme() {
            if (theme != null) Minecraft.getInstance().getSoundManager().stop(theme);
            theme = null; retry = 0;
        }
        public void playTheme() {
            var client = Minecraft.getInstance();
            client.getMusicManager().stopPlaying();
            theme = new SimpleSoundInstance(THEME, SoundSource.MUSIC, 1, 1, RandomSource.create(),
                    true, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
            client.getSoundManager().play(theme);
        }
        public void playStart() { play("ui.game_start"); }
    });
    private UiSounds() {}
    static Identifier id(String path) { return Identifier.fromNamespaceAndPath("hoi", path); }
    static void play(String path) {
        var sounds = Minecraft.getInstance().getSoundManager();
        var id = id(path);
        if (sounds.getSoundEvent(id) != null)
            sounds.play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id), 1, 1));
    }
    static String construction(String building) {
        return "ui.construction." + switch (building) {
            case "infrastructure", "air_base", "anti_air", "radar" -> "state";
            case "hub", "port", "fort", "coastal_fort" -> "province";
            default -> "shared";
        };
    }
    static void receive(AudioProtocol.Cue cue) {
        switch (cue) {
            case SELECT -> { LOBBY.select(); retry = 0; tick(); }
            case START -> LOBBY.start();
            case STOP -> reset();
            default -> play(cue.sound());
        }
    }
    static void tick() {
        if (!LOBBY.requested()) return;
        var client = Minecraft.getInstance();
        var sounds = client.getSoundManager();
        boolean ready = sounds.getSoundEvent(THEME) != null
                && client.options.getSoundSourceVolume(SoundSource.MUSIC) > 0
                && client.options.getSoundSourceVolume(SoundSource.MASTER) > 0;
        if (ready) client.getMusicManager().stopPlaying();
        // Allow asynchronous decoding to finish and avoid retries every tick when muted/unavailable.
        if (retry > 0) { retry--; return; }
        retry = 100;
        LOBBY.tick(ready, theme != null && sounds.isActive(theme));
    }
    static void reset() { LOBBY.reset(); }
}
