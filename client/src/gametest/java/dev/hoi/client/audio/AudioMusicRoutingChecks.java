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
        for (var cue : new AudioProtocol.Cue[]{AudioProtocol.Cue.MAP_ARMY, AudioProtocol.Cue.MAP_NAVY,
                AudioProtocol.Cue.MAP_AIR, AudioProtocol.Cue.MAP_SUPPLY, AudioProtocol.Cue.MAP_CONSTRUCTION}) {
            var before = new Object[3];
            context.runOnClient(client -> {
                var engine = ((dev.hoi.client.mixin.MixinManagerAccess)client.getSoundManager()).hoiSoundEngine();
                before[0] = AudioKoreanMusic.playing();
                before[1] = AudioKoreanMusic.PLAYBACK.selected();
                before[2] = ((dev.hoi.client.mixin.MixinEngineAccess)engine).hoiChannels().get(before[0]);
                if (before[0] == null || before[2] == null) throw new AssertionError("Playlist channel must be active before " + cue);
                AudioUiSounds.receive(cue);
            });
            context.waitTicks(5);
            context.runOnClient(client -> {
                var engine = ((dev.hoi.client.mixin.MixinManagerAccess)client.getSoundManager()).hoiSoundEngine();
                var channel = ((dev.hoi.client.mixin.MixinEngineAccess)engine).hoiChannels().get(AudioKoreanMusic.playing());
                if (AudioKoreanMusic.playing() != before[0] || !before[1].equals(AudioKoreanMusic.PLAYBACK.selected())
                        || channel != before[2] || !client.getSoundManager().isActive(AudioKoreanMusic.playing()))
                    throw new AssertionError("Map switch must retain the current track and playing channel: " + cue);
            });
        }
        for (boolean chooseTrack : new boolean[]{false, true}) {
            context.runOnClient(client -> AudioStoryMusic.play("music.korea.extinction"));
            context.waitTicks(25);
            context.setScreen(() -> new dev.hoi.client.screen.ScreenKoreanMusic(null));
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (!AudioStoryMusic.active() || AudioKoreanMusic.playing() != null)
                    throw new AssertionError("Focus music suspends the playlist before manual replacement");
                var screen = client.gui.screen();
                String label = chooseTrack ? AudioKoreanMusic.PLAYBACK.tracks().get(1).title() : "재생";
                var button = screen.children().stream()
                        .filter(widget -> widget instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(label))
                        .map(widget -> (net.minecraft.client.gui.components.Button)widget).findFirst().orElseThrow();
                button.onPress(new net.minecraft.client.input.MouseButtonEvent(button.getX() + 2, button.getY() + 2,
                        new net.minecraft.client.input.MouseButtonInfo(0, 0)));
                if (AudioStoryMusic.active() || !AudioKoreanMusic.PLAYBACK.requested() || AudioKoreanMusic.PLAYBACK.paused())
                    throw new AssertionError("Playlist control replaces focus music immediately");
                if (chooseTrack && AudioKoreanMusic.PLAYBACK.selected() != 1)
                    throw new AssertionError("Track selection replaces focus music with the chosen song");
            });
            context.waitTicks(25);
            context.runOnClient(client -> {
                if (AudioStoryMusic.active() || AudioKoreanMusic.playing() == null
                        || !client.getSoundManager().isActive(AudioKoreanMusic.playing()))
                    throw new AssertionError("Manual playlist replacement stays active after streaming starts");
                client.gui.screen().onClose();
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
            if(AudioKoreanMusic.playing()==null||!client.getSoundManager().isActive(AudioKoreanMusic.playing()))throw new AssertionError("Finished super event returns to Korean playlist");
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
                AudioUiSounds.receive(AudioProtocol.Cue.MAP_AIR);
                if (!AudioStoryMusic.active() || f.get(null) != sound || !client.getSoundManager().isActive(sound))
                    throw new AssertionError("Map switch preserves the active focus stream");
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
