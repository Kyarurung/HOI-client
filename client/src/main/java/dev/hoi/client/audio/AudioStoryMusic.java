package dev.hoi.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;

public final class AudioStoryMusic {
    private static String requested="";
    private static SoundInstance sound;
    static SoundInstance playing(){return sound;}
    private static int retries, startup;
    private AudioStoryMusic() {}
    public static void play(String event) {
        if (Minecraft.getInstance().getSoundManager().getSoundEvent(AudioUiSounds.id(event)) == null) return;
        reset();AudioKoreanMusic.enable();AudioKoreanMusic.suspend();requested=event;
    }
    public static boolean active() {return !requested.isEmpty();}
    public static void tick() {
        if(requested.isEmpty())return;
        var client=Minecraft.getInstance();
        if(client.level==null){reset();return;}
        if(AudioSuperEvent.active()) {
            if(sound!=null)client.getSoundManager().stop(sound);sound=null;return;
        }
        if(sound!=null) {
            if(!client.getSoundManager().isActive(sound)&&startup--<=0)reset();
            else client.getMusicManager().stopPlaying();
            return;
        }
        if(client.options.getSoundSourceVolume(SoundSource.MASTER)<=0){reset();return;}
        var id=AudioUiSounds.id(requested);
        if(client.getSoundManager().getSoundEvent(id)==null){if(++retries>200)reset();return;}
        client.getMusicManager().stopPlaying();
        sound=new AudioPlaylistSound(requested);
        startup=20;client.getSoundManager().play(sound);
    }
    public static void reset() {if(sound!=null)Minecraft.getInstance().getSoundManager().stop(sound);sound=null;requested="";retries=0;startup=0;}
}
