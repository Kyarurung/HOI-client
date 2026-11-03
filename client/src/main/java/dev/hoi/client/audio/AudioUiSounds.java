package dev.hoi.client.audio;

import dev.hoi.protocol.ProtocolAudioPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;

public final class AudioUiSounds {
    static final java.util.List<String> START_SOUNDS=java.util.List.of("ui.game_start","ui.game_start_signal");
    private AudioUiSounds() {}
    static Identifier id(String path){return Identifier.fromNamespaceAndPath("hoi",path);}
    public static net.minecraft.client.resources.sounds.SoundInstance instance(String path) {
        if (path.equals("ui.focus.complete")) return new AudioPlaylistSound(path);
        return new SimpleSoundInstance(id(path), net.minecraft.sounds.SoundSource.MASTER, 1, 1,
                net.minecraft.util.RandomSource.create(), false, 0,
                net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE, 0, 0, 0, true);
    }
    public static String selection(dev.hoi.protocol.ProtocolCommanderHud.Mode mode) {
        return switch(mode) {
            case NAVY_HQ, NAVY_FLEET, NAVAL_MISSION, NAVAL_TASK_FORCES -> "ui.fleet.select";
            case AIR, AIR_MISSION -> "ui.aircraft.select";
            default -> "ui.division.select";
        };
    }
    public static void play(String path) {
        if (path.isEmpty()) return;
        var sounds=Minecraft.getInstance().getSoundManager();var id=id(path);
        if(sounds.getSoundEvent(id)!=null)sounds.play(instance(path));
    }
    public static void receive(ProtocolAudioPayloads.Cue cue) {
        switch(cue) {
            case SELECT -> { dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.ProtocolHudPayloads.State.HIDDEN); AudioKoreanMusic.select(); }
            case START -> {AudioKoreanMusic.enable();START_SOUNDS.forEach(AudioUiSounds::play);}
            case STOP -> { dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.ProtocolHudPayloads.State.HIDDEN); reset(); dev.hoi.client.screen.ScreenDialogCoordinator.reset(); }
            default -> play(cue.sound());
        }
    }
    public static void stopWorldSounds(net.minecraft.client.sounds.SoundManager sounds) {
        if (AudioKoreanMusic.playing() == null && AudioStoryMusic.playing() == null && AudioSuperEvent.playing() == null) {
            sounds.stop();
            return;
        }
        var engine = ((dev.hoi.client.mixin.MixinManagerAccess)sounds).hoiSoundEngine();
        var access = (dev.hoi.client.mixin.MixinEngineAccess)engine;
        access.hoiQueuedSounds().clear();
        access.hoiQueuedTickableSounds().clear();
        for (var sound : java.util.List.copyOf(access.hoiChannels().keySet())) {
            if (sound != AudioKoreanMusic.playing() && sound != AudioStoryMusic.playing() && sound != AudioSuperEvent.playing()) {
                sounds.stop(sound);
                access.hoiChannels().remove(sound);
                access.hoiSoundDeleteTime().remove(sound);
                access.hoiSoundsBySource().remove(sound.getSource(), sound);
                access.hoiTickingSounds().remove(sound);
            }
        }
    }
    public static void reset(){AudioKoreanMusic.reset();AudioStoryMusic.reset();AudioSuperEvent.reset();}
}
