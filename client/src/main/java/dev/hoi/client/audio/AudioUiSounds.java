package dev.hoi.client.audio;

import dev.hoi.protocol.AudioProtocol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;

public final class AudioUiSounds {
    static final java.util.List<String> START_SOUNDS=java.util.List.of("ui.game_start","ui.game_start_signal");
    private AudioUiSounds() {}
    static Identifier id(String path){return Identifier.fromNamespaceAndPath("hoi",path);}
    public static SimpleSoundInstance instance(String path) {
        return new SimpleSoundInstance(id(path), net.minecraft.sounds.SoundSource.MASTER, 1, 1,
                net.minecraft.util.RandomSource.create(), false, 0,
                net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE, 0, 0, 0, true);
    }
    public static void play(String path) {
        if (path.isEmpty()) return;
        var sounds=Minecraft.getInstance().getSoundManager();var id=id(path);
        if(sounds.getSoundEvent(id)!=null)sounds.play(instance(path));
    }
    public static void receive(AudioProtocol.Cue cue) {
        switch(cue) {
            case SELECT -> { dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.HudProtocol.State.HIDDEN); AudioKoreanMusic.select(); }
            case START -> {AudioKoreanMusic.enable();START_SOUNDS.forEach(AudioUiSounds::play);}
            case STOP -> { dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.HudProtocol.State.HIDDEN); reset(); dev.hoi.client.screen.ScreenDialogCoordinator.reset(); }
            default -> play(cue.sound());
        }
    }
    public static void reset(){AudioKoreanMusic.reset();AudioStoryMusic.reset();AudioSuperEvent.reset();}
}
