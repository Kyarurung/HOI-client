package dev.hoi.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class StoryMusicAudio {
    private static String requested="";
    private static SoundInstance sound;
    private static int retries;
    private StoryMusicAudio() {}
    public static void play(String event) {reset();UiSounds.reset();requested=event;}
    public static boolean active() {return !requested.isEmpty();}
    public static void tick() {
        if(requested.isEmpty())return;
        var client=Minecraft.getInstance();
        if(client.level==null){reset();return;}
        if(SuperEventAudio.active()) {
            if(sound!=null)client.getSoundManager().stop(sound);sound=null;return;
        }
        if(sound!=null) {
            if(!client.getSoundManager().isActive(sound))reset();
            else client.getMusicManager().stopPlaying();
            return;
        }
        if(client.options.getSoundSourceVolume(SoundSource.MASTER)<=0 || client.options.getSoundSourceVolume(SoundSource.MUSIC)<=0){reset();return;}
        var id=UiSounds.id(requested);
        if(client.getSoundManager().getSoundEvent(id)==null){if(++retries>200)reset();return;}
        client.getMusicManager().stopPlaying();
        sound=new SimpleSoundInstance(id,SoundSource.MUSIC,1,1,RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
        client.getSoundManager().play(sound);
    }
    public static void reset() {if(sound!=null)Minecraft.getInstance().getSoundManager().stop(sound);sound=null;requested="";retries=0;}
}
