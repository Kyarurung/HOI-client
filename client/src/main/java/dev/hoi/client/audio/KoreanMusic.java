package dev.hoi.client.audio;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import java.util.List;

public final class KoreanMusic {
    private record Catalog(int version, List<PlaylistPlayback.Track> tracks) {}
    private static SoundInstance sound;
    public static final PlaylistPlayback PLAYBACK = new PlaylistPlayback(new PlaylistPlayback.Output() {
        public boolean start(PlaylistPlayback.Track track) {
            var client = Minecraft.getInstance(); var id = UiSounds.id(track.event());
            if (client.getSoundManager().getSoundEvent(id) == null) return false;
            client.getMusicManager().stopPlaying();
            sound = new SimpleSoundInstance(id, SoundSource.MUSIC, 1, 1, RandomSource.create(),
                    false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
            client.getSoundManager().play(sound); return true;
        }
        public boolean active() { return sound != null && Minecraft.getInstance().getSoundManager().isActive(sound); }
        public void stop() { if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound); sound = null; }
    });
    private KoreanMusic() {}
    public static void reload(ResourceManager resources) {
        List<PlaylistPlayback.Track> tracks = List.of();
        var file = resources.getResource(Identifier.fromNamespaceAndPath("hoi", "music/korean.json"));
        if (file.isPresent()) try (var stream = file.get().open()) {
            var bytes = stream.readNBytes(1_048_577);
            if (bytes.length > 1_048_576) throw new IllegalArgumentException("Korean playlist file limit");
            var data = new Gson().fromJson(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), Catalog.class);
            if (data.version() != 1) throw new IllegalArgumentException("Unsupported Korean playlist");
            tracks = PlaylistPlayback.validate(data.tracks());
        } catch (java.io.IOException | RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(KoreanMusic.class).warn("한국 음악 목록을 불러오지 못했습니다", ex);
        }
        var loaded = tracks;
        Minecraft.getInstance().execute(() -> PLAYBACK.replace(loaded));
    }
    public static void play(int index) { UiSounds.reset(); StoryMusicAudio.reset(); PLAYBACK.play(index); }
    public static void tick() {
        var client = Minecraft.getInstance();
        boolean allowed = client.level != null && client.options.getSoundSourceVolume(SoundSource.MASTER) > 0 && client.options.getSoundSourceVolume(SoundSource.MUSIC) > 0 && !SuperEventAudio.active() && !StoryMusicAudio.active();
        PLAYBACK.tick(allowed);
        if (allowed && sound != null && client.getSoundManager().isActive(sound)) client.getMusicManager().stopPlaying();
    }
}
