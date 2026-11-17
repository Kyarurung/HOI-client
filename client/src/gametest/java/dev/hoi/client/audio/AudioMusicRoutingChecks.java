package dev.hoi.client.audio;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.protocol.audio.*;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.sounds.SoundSource;

public final class AudioMusicRoutingChecks {
    private static void transfer(ClientGameTestContext context, java.util.function.Consumer<String> command, String dimension) {
        command.accept("execute in " + dimension + " run tp @a 0 100 0");
        context.waitFor(client -> client.level != null && client.level.dimension().identifier().toString().equals(dimension));
        context.waitTicks(25);
    }
    public static void run(ClientGameTestContext context, java.util.function.Consumer<String> command) {
        var volume=new double[1];
        context.runOnClient(client->{volume[0]=client.options.getSoundSourceVolume(SoundSource.MUSIC);client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);});
        context.runOnClient(client -> { AudioUiSounds.reset(); AudioUiSounds.receive(AudioPayloads.Cue.SELECT); });
        context.waitTicks(25);
        context.runOnClient(client -> {
            if (!AudioStoryMusic.active()) throw new AssertionError("Selection starts theme");
            AudioUiSounds.receive(AudioPayloads.Cue.START);
            if (!AudioStoryMusic.active()) throw new AssertionError("Start preserves the theme");
            AudioUiSounds.reset();
        });
        context.runOnClient(client->{if(AudioKoreanMusic.stationEnabled())AudioKoreanMusic.toggleStation();});
        for(String country:new String[]{"KOR","PRK","JAP","PRC"}) {
            context.runOnClient(client->{
                AudioUiSounds.reset();ClientCampaignHud.accept(HudPayloads.State.HIDDEN);
                ClientCampaignHud.accept(HudPayloads.State.of(country,CountryHud.UNKNOWN));
                if(HudMenuBar.buttons(800,country,MenuTab.POLITICS,tab->{}).stream().filter(b->b instanceof dev.hoi.client.ui.hud.HudMusicButton).count()!=1)
                    throw new AssertionError("One original music icon below the debt indicator for "+country);
            });
            context.waitTicks(25);
            context.runOnClient(client->{
                if(AudioKoreanMusic.PLAYBACK.requested()||AudioKoreanMusic.playing()!=null)
                    throw new AssertionError("Korean playlist stays off until the station is turned on: "+country);
            });
        }
        context.runOnClient(client->AudioKoreanMusic.toggleStation());
        context.waitTicks(25);
        context.runOnClient(client->{
            if(!AudioKoreanMusic.stationEnabled()||!AudioKoreanMusic.packReady(client))throw new AssertionError("Station ON with every playlist sound loaded");
            if(AudioKoreanMusic.playing()!=null && AudioKoreanMusic.playing().getSource()!=SoundSource.MASTER) throw new AssertionError("Playlist uses master volume");
            if(!AudioKoreanMusic.PLAYBACK.requested()||AudioKoreanMusic.playing()==null||!client.getSoundManager().isActive(AudioKoreanMusic.playing()))
                throw new AssertionError("Turning the station on starts the Korean playlist");
        });
        int transition = 0;
        for (var cue : new AudioPayloads.Cue[]{AudioPayloads.Cue.MAP_ARMY, AudioPayloads.Cue.MAP_NAVY,
                AudioPayloads.Cue.MAP_AIR, AudioPayloads.Cue.MAP_SUPPLY, AudioPayloads.Cue.MAP_CONSTRUCTION}) {
            var before = new Object[4];
            boolean paused = transition == 2;
            if (paused) context.runOnClient(client -> AudioKoreanMusic.togglePause());
            context.runOnClient(client -> {
                var engine = ((dev.hoi.client.mixin.audio.AudioManagerAccess)client.getSoundManager()).hoiSoundEngine();
                before[0] = AudioKoreanMusic.playing();
                before[1] = AudioKoreanMusic.PLAYBACK.selected();
                before[2] = ((dev.hoi.client.mixin.audio.AudioEngineAccess)engine).hoiChannels().get(before[0]);
                if (before[0] == null || before[2] == null) throw new AssertionError("Playlist channel must be active before " + cue);
                before[3] = new net.minecraft.client.resources.sounds.SimpleSoundInstance(AudioUiSounds.id("music.tfr_theme"),
                        SoundSource.MASTER, 1, 1, net.minecraft.util.RandomSource.create(), true, 40,
                        net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE, 0, 0, 0, true);
                client.getSoundManager().play((net.minecraft.client.resources.sounds.SoundInstance)before[3]);
                client.getSoundManager().playDelayed(AudioUiSounds.instance("ui.click"), 200);
                AudioUiSounds.receive(cue);
            });
            String dimension = transition++ % 2 == 0 ? "minecraft:the_nether" : "minecraft:overworld";
            transfer(context, command, dimension);
            context.runOnClient(client -> {
                var engine = ((dev.hoi.client.mixin.audio.AudioManagerAccess)client.getSoundManager()).hoiSoundEngine();
                var channel = ((dev.hoi.client.mixin.audio.AudioEngineAccess)engine).hoiChannels().get(AudioKoreanMusic.playing());
                if (AudioKoreanMusic.playing() != before[0] || !before[1].equals(AudioKoreanMusic.PLAYBACK.selected())
                        || channel != before[2] || !client.getSoundManager().isActive(AudioKoreanMusic.playing()))
                    throw new AssertionError("Map switch must retain the current track and playing channel: " + cue);
                if (AudioKoreanMusic.PLAYBACK.paused() != paused) throw new AssertionError("Dimension transfer preserves manual pause");
                if (client.getSoundManager().isActive((net.minecraft.client.resources.sounds.SoundInstance)before[3]))
                    throw new AssertionError("Previous dimension sound must stop");
                if (!((dev.hoi.client.mixin.audio.AudioEngineAccess)engine).hoiQueuedSounds().isEmpty())
                    throw new AssertionError("Previous dimension delayed sounds must clear");
                if (paused) AudioKoreanMusic.togglePause();
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
                        new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)));
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
        var superSound = context.computeOnClient(client -> AudioSuperEvent.playing());
        transfer(context, command, "minecraft:overworld");
        context.runOnClient(client->{
            if (AudioSuperEvent.playing() != superSound || !client.getSoundManager().isActive(superSound))
                throw new AssertionError("Dimension transfer preserves the super event stream");
            if(!AudioSuperEvent.active()||AudioKoreanMusic.playing()!=null)throw new AssertionError("Super event lost priority during streaming");
            int before=AudioKoreanMusic.PLAYBACK.selected();AudioUiSounds.receive(AudioPayloads.Cue.MAP_NAVY);
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
        var storySound = context.computeOnClient(client -> AudioStoryMusic.playing());
        transfer(context, command, "minecraft:the_nether");
        context.runOnClient(client->{
            if (AudioStoryMusic.playing() != storySound || !client.getSoundManager().isActive(storySound))
                throw new AssertionError("Dimension transfer preserves the focus music stream");
            try {
                var f=AudioStoryMusic.class.getDeclaredField("sound");f.setAccessible(true);
                var sound=(net.minecraft.client.resources.sounds.SoundInstance)f.get(null);
                if(sound==null||!client.getSoundManager().isActive(sound))throw new AssertionError("Original focus music is actually active");
                AudioUiSounds.receive(AudioPayloads.Cue.MAP_AIR);
                if (!AudioStoryMusic.active() || f.get(null) != sound || !client.getSoundManager().isActive(sound))
                    throw new AssertionError("Map switch preserves the active focus stream");
                client.getSoundManager().stop(sound);
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
        context.waitTicks(50);
        context.runOnClient(client->{
            if(AudioStoryMusic.active()||AudioKoreanMusic.playing()==null)throw new AssertionError("Ended focus track resumes playlist");
            AudioUiSounds.receive(AudioPayloads.Cue.STOP);ClientCampaignHud.accept(HudPayloads.State.HIDDEN);
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(AudioKoreanMusic.PLAYBACK.requested()||AudioKoreanMusic.playing()!=null||AudioStoryMusic.active()||AudioSuperEvent.active())throw new AssertionError("STOP leaves no music request");
            if(AudioKoreanMusic.stationEnabled())AudioKoreanMusic.toggleStation();
            client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(volume[0]);
        });
        context.setScreen(() -> new dev.hoi.client.screen.ScreenKoreanMusic(null));
        context.waitTicks(2);
        context.runOnClient(client->{
            var titles=AudioKoreanMusic.PLAYBACK.tracks().stream().map(track->track.title()).collect(java.util.stream.Collectors.toSet());
            boolean listed=client.gui.screen().children().stream().anyMatch(widget->widget instanceof net.minecraft.client.gui.components.Button b&&titles.contains(b.getMessage().getString()));
            client.gui.screen().onClose();
            if(listed)throw new AssertionError("Playlist rows are listed only while the station is ON");
        });
    }
}
