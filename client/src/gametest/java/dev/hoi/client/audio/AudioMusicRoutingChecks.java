package dev.hoi.client.audio;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.sounds.SoundSource;

public final class AudioMusicRoutingChecks {
    public static void run(ClientGameTestContext context) {
        var volume=new double[1];
        context.runOnClient(client->{volume[0]=client.options.getSoundSourceVolume(SoundSource.MUSIC);client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);});
        context.runOnClient(client -> { AudioUiSounds.reset(); AudioUiSounds.receive(AudioProtocol.Cue.SELECT); });
        context.waitTicks(25);
        context.runOnClient(client -> {
            if (!AudioStoryMusic.active()) throw new AssertionError("Selection starts theme");
            AudioUiSounds.receive(AudioProtocol.Cue.START);
            if (!AudioStoryMusic.active()) throw new AssertionError("Start preserves the theme");
            AudioUiSounds.reset();
        });
        for(String country:new String[]{"KOR","PRK","JAP","PRC"}) {
            context.runOnClient(client->{
                AudioUiSounds.reset();ClientCampaignHud.accept(HudProtocol.State.HIDDEN);
                ClientCampaignHud.accept(HudProtocol.State.of(country,CountryHud.UNKNOWN));
                if(UiMenuBar.buttons(800,country,MenuTab.POLITICS,tab->{}).stream().filter(b->b instanceof dev.hoi.client.ui.UiMusicButton).count()!=1)
                    throw new AssertionError("One original music icon below the debt indicator for "+country);
            });
            context.waitTicks(25);
            context.runOnClient(client->{
                if(AudioKoreanMusic.playing()!=null && AudioKoreanMusic.playing().getSource()!=SoundSource.MASTER) throw new AssertionError("Playlist uses master volume");
                if(!AudioKoreanMusic.PLAYBACK.requested()||AudioKoreanMusic.playing()==null||!client.getSoundManager().isActive(AudioKoreanMusic.playing()))
                    throw new AssertionError("Every country must automatically start Korean playlist: "+country);
            });
        }
        context.runOnClient(client->{
            AudioStoryMusic.play("music.korea.extinction");
            if(AudioKoreanMusic.playing()!=null)throw new AssertionError("Focus music must immediately suspend playlist");
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(!AudioStoryMusic.active()||AudioKoreanMusic.playing()!=null)throw new AssertionError("Source focus priority");
            AudioSuperEvent.play("routing-super","super_event/prc_invasion_of_taiwan_super_event");
            if(!AudioSuperEvent.active()||AudioKoreanMusic.playing()!=null||AudioStoryMusic.active())throw new AssertionError("Super event replaces previous focus music");
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(!AudioSuperEvent.active()||AudioKoreanMusic.playing()!=null)throw new AssertionError("Super event lost priority during streaming");
            int before=AudioKoreanMusic.PLAYBACK.selected();AudioUiSounds.receive(AudioProtocol.Cue.MAP_NAVY);
            if(!AudioSuperEvent.active()||AudioKoreanMusic.playing()!=null)throw new AssertionError("Map switch cannot interrupt global super music");
            if(AudioKoreanMusic.PLAYBACK.selected()!=before)throw new AssertionError("Map switch preserves suspended playlist selection");
            AudioKoreanMusic.play(0);AudioStoryMusic.play("music.korea.extinction");
            if(!AudioSuperEvent.active())throw new AssertionError("Track choice and incoming national focus cannot cancel super event");
            AudioStoryMusic.reset();AudioSuperEvent.stop("routing-super");
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(AudioKoreanMusic.playing()==null||!client.getSoundManager().isActive(AudioKoreanMusic.playing()))throw new AssertionError("Map returns to Korean playlist");
            AudioSuperEvent.play("routing-end","super_event/prc_invasion_of_taiwan_super_event");
        });
        context.waitTicks(5);
        context.runOnClient(client->AudioSuperEvent.stop("routing-end"));
        context.waitTicks(25);
        context.runOnClient(client->{
            if(AudioKoreanMusic.playing()==null||!client.getSoundManager().isActive(AudioKoreanMusic.playing()))throw new AssertionError("Finished super event resumes playlist");
            AudioStoryMusic.play("music.korea.extinction");
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            try {
                var f=AudioStoryMusic.class.getDeclaredField("sound");f.setAccessible(true);
                var sound=(net.minecraft.client.resources.sounds.SoundInstance)f.get(null);
                if(sound==null||!client.getSoundManager().isActive(sound))throw new AssertionError("Original focus music is actually active");
                client.getSoundManager().stop(sound);
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
        context.waitTicks(50);
        context.runOnClient(client->{
            if(AudioStoryMusic.active()||AudioKoreanMusic.playing()==null)throw new AssertionError("Ended focus track resumes playlist");
            AudioUiSounds.receive(AudioProtocol.Cue.STOP);ClientCampaignHud.accept(HudProtocol.State.HIDDEN);
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(AudioKoreanMusic.PLAYBACK.requested()||AudioKoreanMusic.playing()!=null||AudioStoryMusic.active()||AudioSuperEvent.active())throw new AssertionError("STOP leaves no music request");
            client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(volume[0]);
        });
    }
}
