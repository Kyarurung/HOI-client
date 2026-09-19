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
    private static boolean enabled;
    private static boolean manuallyStopped;
    private static boolean stationEnabled = true;
    private static double volume = 1.0;
    private static boolean selected;
    public static final PlaylistPlayback PLAYBACK = new PlaylistPlayback(new PlaylistPlayback.Output() {
        public boolean start(PlaylistPlayback.Track track) {
            var client = Minecraft.getInstance(); var id = UiSounds.id(track.event());
            if (client.getSoundManager().getSoundEvent(id) == null) return false;
            client.getMusicManager().stopPlaying();
            sound = new AbstractTickableSoundInstance(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id), SoundSource.MASTER, RandomSource.create()) {
                { volume = (float)KoreanMusic.volume; attenuation = SoundInstance.Attenuation.NONE; relative = true; }
                @Override public void tick(){}
                @Override public float getVolume(){return (float)KoreanMusic.volume;}
                @Override public boolean canStartSilent(){return true;}
            };
            client.getSoundManager().play(sound); return true;
        }
        public void pause(boolean paused) {
            if (sound == null) return;
            var engine = ((dev.hoi.client.mixin.SoundManagerAccessor)Minecraft.getInstance().getSoundManager()).hoiSoundEngine();
            var channel = ((dev.hoi.client.mixin.SoundEngineAccessor)engine).hoiChannels().get(sound);
            if (channel != null) channel.execute(value -> { if (paused) value.pause(); else value.unpause(); });
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
    public static void select(){
        enable();
        if (!selected) { selected=true; StoryMusicAudio.play("music.tfr_theme"); }
    }
    public static double volume(){return volume;}
    public static void volume(double value){if(Double.isFinite(value))volume=Math.clamp(value,0.0,1.0);}
    public static boolean isPlaying(){return !PLAYBACK.paused()&&sound!=null&&Minecraft.getInstance().getSoundManager().isActive(sound);}
    public static boolean stationEnabled(){return stationEnabled;}
    public static void toggleStation(){
        stationEnabled = !stationEnabled;
        if (!stationEnabled) stop();
        else { enable(); if (!PLAYBACK.tracks().isEmpty()) play(PLAYBACK.selected()); }
    }
    public static void enable(){enabled=stationEnabled&&!manuallyStopped;}
    public static void togglePause(){
        if(!PLAYBACK.requested()){if(!PLAYBACK.tracks().isEmpty())play(PLAYBACK.selected());}
        else PLAYBACK.togglePause();
    }
    public static void stop(){enabled=false;manuallyStopped=true;PLAYBACK.stop();}
    public static void reset(){stop();manuallyStopped=false;selected=false;}
    public static void restorePause() {
        if (!PLAYBACK.paused() || sound == null) return;
        var engine = ((dev.hoi.client.mixin.SoundManagerAccessor)Minecraft.getInstance().getSoundManager()).hoiSoundEngine();
        var channel = ((dev.hoi.client.mixin.SoundEngineAccessor)engine).hoiChannels().get(sound);
        if (channel != null) channel.execute(value -> value.pause());
    }
    public static void suspend(){PLAYBACK.tick(false);}
    public static void mapChanged() {
        enable();StoryMusicAudio.reset();
        if(SuperEventAudio.active()||manuallyStopped||PLAYBACK.paused())return;
        if(stationEnabled&&!PLAYBACK.tracks().isEmpty())PLAYBACK.step(1);
    }
    public static void play(int index){manuallyStopped=false;stationEnabled=true;enabled=true;StoryMusicAudio.reset();PLAYBACK.play(index);}
    static SoundInstance playing(){return sound;}
    public static void tick() {
        var client = Minecraft.getInstance();
        if(client.level==null){reset();return;}
        if(enabled&&!PLAYBACK.requested()&&!PLAYBACK.tracks().isEmpty())PLAYBACK.play(PLAYBACK.selected());
        boolean allowed = enabled && client.options.getSoundSourceVolume(SoundSource.MASTER) > 0 && !SuperEventAudio.active() && !StoryMusicAudio.active();
        PLAYBACK.tick(allowed);
        restorePause();
        if (allowed && sound != null && client.getSoundManager().isActive(sound)) client.getMusicManager().stopPlaying();
    }
}
