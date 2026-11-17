package dev.hoi.client.audio;

import com.google.gson.Gson;
import dev.hoi.protocol.audio.AudioPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;
import java.util.List;

public final class AudioKoreanMusic {
    private record AudioMusicCatalog(int version, List<AudioPlaylistPlayback.AudioTrack> tracks) {}
    private static SoundInstance sound;
    private static boolean enabled;
    private static boolean manuallyStopped;
    private static boolean stationEnabled;
    private static boolean packRequested;
    private static final AudioPlaylistSettings SETTINGS = new AudioPlaylistSettings(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("hoi-audio.properties"));
    private static boolean selected;
    public static final AudioPlaylistPlayback PLAYBACK = new AudioPlaylistPlayback(new AudioPlaylistPlayback.AudioOutput() {
        public boolean start(AudioPlaylistPlayback.AudioTrack track) {
            var client = Minecraft.getInstance(); var id = AudioUiSounds.id(track.event());
            if (client.getSoundManager().getSoundEvent(id) == null) return false;
            client.getMusicManager().stopPlaying();
            sound = new AudioPlaylistSound(track.event());
            client.getSoundManager().play(sound); return true;
        }
        public void pause(boolean paused) {
            if (sound == null) return;
            var engine = ((dev.hoi.client.mixin.audio.AudioManagerAccess)Minecraft.getInstance().getSoundManager()).hoiSoundEngine();
            var channel = ((dev.hoi.client.mixin.audio.AudioEngineAccess)engine).hoiChannels().get(sound);
            if (channel != null) channel.execute(value -> { if (paused) value.pause(); else value.unpause(); });
        }
        public boolean active() { return sound != null && Minecraft.getInstance().getSoundManager().isActive(sound); }
        public void stop() { if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound); sound = null; }
    });
    private AudioKoreanMusic() {}
    public static void reload(ResourceManager resources) {
        List<AudioPlaylistPlayback.AudioTrack> tracks = List.of();
        var file = resources.getResource(Identifier.fromNamespaceAndPath("hoi", "music/korean.json"));
        if (file.isPresent()) try (var stream = file.get().open()) {
            var bytes = stream.readNBytes(1_048_577);
            if (bytes.length > 1_048_576) throw new IllegalArgumentException("Korean playlist file limit");
            var data = new Gson().fromJson(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), AudioMusicCatalog.class);
            if (data.version() != 1) throw new IllegalArgumentException("Unsupported Korean playlist");
            tracks = AudioPlaylistPlayback.validate(data.tracks());
        } catch (java.io.IOException | RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(AudioKoreanMusic.class).warn("한국 음악 목록을 불러오지 못했습니다", ex);
        }
        var loaded = tracks;
        Minecraft.getInstance().execute(() -> PLAYBACK.replace(loaded));
    }
    public static void select(){
        enable();
        if (!selected) { selected=true; AudioStoryMusic.play("music.tfr_theme"); }
    }
    public static void loadSettings(){
        try { SETTINGS.load(); stationEnabled = SETTINGS.station(); }
        catch (java.io.IOException | IllegalArgumentException error) { org.slf4j.LoggerFactory.getLogger(AudioKoreanMusic.class).warn("음량 설정을 불러오지 못했습니다", error); }
    }
    public static double volume(){return SETTINGS.value();}
    public static void volume(double value){
        try { SETTINGS.set(value); }
        catch (java.io.IOException error) { org.slf4j.LoggerFactory.getLogger(AudioKoreanMusic.class).warn("음량 설정을 저장하지 못했습니다", error); }
    }
    public static boolean isPlaying(){return !PLAYBACK.paused()&&sound!=null&&Minecraft.getInstance().getSoundManager().isActive(sound);}
    public static boolean stationEnabled(){return stationEnabled;}
    public static void toggleStation(){
        station(!stationEnabled);
        if (!stationEnabled) stop();
        else { enable(); requestPack(); if (!PLAYBACK.tracks().isEmpty()) play(PLAYBACK.selected()); }
    }
    private static void station(boolean enabled){
        stationEnabled = enabled;
        try { SETTINGS.station(enabled); }
        catch (java.io.IOException error) { org.slf4j.LoggerFactory.getLogger(AudioKoreanMusic.class).warn("음악 설정을 저장하지 못했습니다", error); }
    }
    private static void requestPack(){
        if (packRequested || !ClientPlayNetworking.canSend(AudioPayloads.KoreanMusicRequest.TYPE)) return;
        ClientPlayNetworking.send(AudioPayloads.KoreanMusicRequest.INSTANCE);
        packRequested = true;
    }
    static boolean packReady(Minecraft client){
        var sounds = client.getSoundManager();
        return PLAYBACK.tracks().stream().allMatch(track -> sounds.getSoundEvent(AudioUiSounds.id(track.event())) != null);
    }
    public static void enable(){enabled=stationEnabled&&!manuallyStopped;}
    public static void togglePause(){
        if(AudioStoryMusic.active()||!PLAYBACK.requested()){if(!PLAYBACK.tracks().isEmpty())play(PLAYBACK.selected());}
        else PLAYBACK.togglePause();
    }
    public static void stop(){enabled=false;manuallyStopped=true;PLAYBACK.stop();}
    public static void reset(){stop();manuallyStopped=false;selected=false;}
    public static void restorePause() {
        if (!PLAYBACK.paused() || sound == null) return;
        var engine = ((dev.hoi.client.mixin.audio.AudioManagerAccess)Minecraft.getInstance().getSoundManager()).hoiSoundEngine();
        var channel = ((dev.hoi.client.mixin.audio.AudioEngineAccess)engine).hoiChannels().get(sound);
        if (channel != null) channel.execute(value -> value.pause());
    }
    public static void suspend(){PLAYBACK.tick(false);}
    public static void play(int index){manuallyStopped=false;if(!stationEnabled)station(true);enabled=true;requestPack();AudioStoryMusic.reset();PLAYBACK.play(index);}
    static SoundInstance playing(){return sound;}
    public static void tick() {
        var client = Minecraft.getInstance();
        if(client.level==null){reset();packRequested=false;return;}
        if(stationEnabled)requestPack();
        if(enabled&&!PLAYBACK.requested()&&!PLAYBACK.tracks().isEmpty())PLAYBACK.play(PLAYBACK.selected());
        boolean allowed = enabled && client.options.getSoundSourceVolume(SoundSource.MASTER) > 0 && !AudioSuperEvent.active() && !AudioStoryMusic.active() && packReady(client);
        PLAYBACK.tick(allowed);
        restorePause();
        if (allowed && sound != null && client.getSoundManager().isActive(sound)) client.getMusicManager().stopPlaying();
    }
}
