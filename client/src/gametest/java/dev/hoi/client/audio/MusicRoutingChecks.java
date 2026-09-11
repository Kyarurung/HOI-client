package dev.hoi.client.audio;

import dev.hoi.client.CampaignHud;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.sounds.SoundSource;

public final class MusicRoutingChecks {
    public static void run(ClientGameTestContext context) {
        var volume=new double[1];
        context.runOnClient(client->{volume[0]=client.options.getSoundSourceVolume(SoundSource.MUSIC);client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(1.0);});
        for(String country:new String[]{"KOR","PRK","JAP","PRC"}) {
            context.runOnClient(client->{
                UiSounds.reset();CampaignHud.accept(HudProtocol.State.HIDDEN);
                CampaignHud.accept(HudProtocol.State.of(country,CountryHud.UNKNOWN));
                if(HoiMenuBar.buttons(800,country,MenuTab.POLITICS,tab->{}).stream().filter(b->b instanceof dev.hoi.client.ui.MusicButton).count()!=1)
                    throw new AssertionError("One original music icon below the debt indicator for "+country);
            });
            context.waitTicks(25);
            context.runOnClient(client->{
                if(!KoreanMusic.PLAYBACK.requested()||KoreanMusic.playing()==null||!client.getSoundManager().isActive(KoreanMusic.playing()))
                    throw new AssertionError("Every country must automatically start Korean playlist: "+country);
            });
        }
        context.runOnClient(client->{
            StoryMusicAudio.play("music.korea.extinction");
            if(KoreanMusic.playing()!=null)throw new AssertionError("Focus music must immediately suspend playlist");
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(!StoryMusicAudio.active()||KoreanMusic.playing()!=null)throw new AssertionError("Source focus priority");
            SuperEventAudio.play("routing-super","super_event/prc_invasion_of_taiwan_super_event");
            if(!SuperEventAudio.active()||KoreanMusic.playing()!=null)throw new AssertionError("Source super event priority");
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(!SuperEventAudio.active()||KoreanMusic.playing()!=null)throw new AssertionError("Super event lost priority during streaming");
            int before=KoreanMusic.PLAYBACK.selected();UiSounds.receive(AudioProtocol.Cue.MAP_NAVY);
            if(SuperEventAudio.active()||StoryMusicAudio.active())throw new AssertionError("Map switch must cancel special music");
            if(KoreanMusic.PLAYBACK.selected()!=(before+1)%26)throw new AssertionError("Map switch advances the default playlist");
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(KoreanMusic.playing()==null||!client.getSoundManager().isActive(KoreanMusic.playing()))throw new AssertionError("Map returns to Korean playlist");
            SuperEventAudio.play("routing-end","super_event/prc_invasion_of_taiwan_super_event");
        });
        context.waitTicks(5);
        context.runOnClient(client->SuperEventAudio.stop("routing-end"));
        context.waitTicks(25);
        context.runOnClient(client->{
            if(KoreanMusic.playing()==null||!client.getSoundManager().isActive(KoreanMusic.playing()))throw new AssertionError("Finished super event resumes playlist");
            StoryMusicAudio.play("music.korea.extinction");
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            try {
                var f=StoryMusicAudio.class.getDeclaredField("sound");f.setAccessible(true);
                var sound=(net.minecraft.client.resources.sounds.SoundInstance)f.get(null);
                if(sound==null||!client.getSoundManager().isActive(sound))throw new AssertionError("Original focus music is actually active");
                client.getSoundManager().stop(sound);
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
        context.waitTicks(50);
        context.runOnClient(client->{
            if(StoryMusicAudio.active()||KoreanMusic.playing()==null)throw new AssertionError("Ended focus track resumes playlist");
            UiSounds.receive(AudioProtocol.Cue.STOP);CampaignHud.accept(HudProtocol.State.HIDDEN);
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(KoreanMusic.PLAYBACK.requested()||KoreanMusic.playing()!=null||StoryMusicAudio.active()||SuperEventAudio.active())throw new AssertionError("STOP leaves no music request");
            client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(volume[0]);
        });
    }
}
