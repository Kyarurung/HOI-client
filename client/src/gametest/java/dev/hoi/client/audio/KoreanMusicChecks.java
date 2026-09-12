package dev.hoi.client.audio;

import dev.hoi.client.screen.KoreanMusicScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;

public final class KoreanMusicChecks {
    public static void run(ClientGameTestContext context, dev.hoi.protocol.MenuView menu) {
        double[] previousMusic=new double[1];
        context.runOnClient(client -> {
            previousMusic[0]=client.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC);
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(1.0);
            KoreanMusic.reload(client.getResourceManager());
            var tracks = KoreanMusic.PLAYBACK.tracks();
            if (tracks.size() != 81) throw new AssertionError("All original Korean playlist tracks must load");
            for (var track : tracks) {
                if (!track.cover().isEmpty() && client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+track.cover()+".png")).isEmpty()) throw new AssertionError("Missing album art: "+track.title());
            }
            for (var track : tracks) if (client.getSoundManager().getSoundEvent(UiSounds.id(track.event())) == null)
                throw new AssertionError("Missing playlist sound: " + track.event());
        });
        context.runOnClient(client -> { if(!KoreanMusic.stationEnabled())KoreanMusic.toggleStation(); });
        for (int[] size : new int[][]{{1600, 1000}, {854, 480}}) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.setScreen(() -> new KoreanMusicScreen(null));
            context.waitTicks(3);
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                for (var widget : screen.children()) if (widget instanceof Button button
                        && (button.getX() < 0 || button.getY() < 0 || button.getX() + button.getWidth() > screen.width
                        || button.getY() + button.getHeight() > screen.height))
                    throw new AssertionError("Playlist control outside viewport: " + button.getMessage().getString());
            });
            context.takeScreenshot("hoi-korean-music-" + size[0]);
            if(size[0]==1600){
                context.runOnClient(client -> {
                    var screen=client.gui.screen();
                    var button=screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("TFR Korea Plus ON/OFF")).map(w -> (Button)w).findFirst().orElseThrow();
                    screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(button.getX()+10,button.getY()+10,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
                    if(KoreanMusic.stationEnabled()||KoreanMusic.PLAYBACK.requested())throw new AssertionError("Station OFF stops playlist");
                    KoreanMusic.mapChanged();
                    if(KoreanMusic.stationEnabled()||KoreanMusic.PLAYBACK.requested())throw new AssertionError("Map switch preserves station OFF");
                });
                context.getInput().setCursorPos(1500,950);context.waitTicks(3);
                context.takeScreenshot("hoi-korean-music-off-1600");
                context.runOnClient(client -> KoreanMusic.toggleStation());context.waitTicks(3);
                context.runOnClient(client -> {if(!KoreanMusic.stationEnabled()||!KoreanMusic.PLAYBACK.requested())throw new AssertionError("Station ON starts selected track");});
                context.waitTicks(25);
                context.takeScreenshot("hoi-korean-music-on-1600");
                context.runOnClient(client -> {
                    var slider=client.gui.screen().children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.AbstractSliderButton).map(w -> (net.minecraft.client.gui.components.AbstractSliderButton)w).findFirst().orElseThrow();
                    slider.onClick(new net.minecraft.client.input.MouseButtonEvent(slider.getX()+4,slider.getY()+5,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
                    if(KoreanMusic.volume()!=0)throw new AssertionError("Volume click reaches mute");
                });
                context.waitTicks(3);
                context.runOnClient(client -> {if(KoreanMusic.playing()==null||KoreanMusic.playing().getVolume()!=0)throw new AssertionError("Active stream updates to mute");});
                context.runOnClient(client -> {
                    var slider=client.gui.screen().children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.AbstractSliderButton).map(w -> (net.minecraft.client.gui.components.AbstractSliderButton)w).findFirst().orElseThrow();
                    slider.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(slider.getX()+4+(slider.getWidth()-8)*.25,slider.getY()+5,new net.minecraft.client.input.MouseButtonInfo(0,0)),0,0);
                    if(Math.abs(KoreanMusic.volume()-.25)>.001)throw new AssertionError("Volume drag reaches 25 percent");
                });
                context.waitTicks(3);
                context.runOnClient(client -> {if(Math.abs(KoreanMusic.playing().getVolume()-.25)>.001)throw new AssertionError("Active stream volume updates without restart");});
                context.takeScreenshot("hoi-korean-music-volume-25-1600");
                context.runOnClient(client -> KoreanMusic.volume(1));
            }
        }
        for (int[] size : new int[][]{{1600, 1000}, {854, 480}}) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.setScreen(() -> new dev.hoi.client.screen.HoiMenuScreen(menu));
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/music/toggle.png")).isEmpty())
                    throw new AssertionError("Original TFR music icon must load");
                var screen = client.gui.screen();
                var button = screen.children().stream().filter(b -> b instanceof dev.hoi.client.ui.MusicButton)
                        .map(b -> (dev.hoi.client.ui.MusicButton)b).findFirst().orElseThrow();
                int right = dev.hoi.client.ui.HoiMenuBar.statsRight(screen.width);
                if (button.getX() < right - 67 || button.getRight() > right || button.getY() <= dev.hoi.client.ui.HoiMenuBar.STATS_HEIGHT)
                    throw new AssertionError("Music icon must sit below the right debt indicator");
                for (var child : screen.children()) if (child instanceof Button other && other != button
                        && other.getX() < button.getRight() && other.getRight() > button.getX()
                        && other.getY() < button.getBottom() && other.getBottom() > button.getY())
                    throw new AssertionError("Music button overlaps " + other.getMessage().getString());
            });
            context.takeScreenshot("hoi-music-button-below-debt-" + size[0]);context.waitTicks(3);
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                var button = screen.children().stream().filter(b -> b instanceof dev.hoi.client.ui.MusicButton)
                        .map(b -> (dev.hoi.client.ui.MusicButton)b).findFirst().orElseThrow();
                screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(button.getX() + 9, button.getY() + 9,
                        new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
                if (!(client.gui.screen() instanceof KoreanMusicScreen)) throw new AssertionError("Music icon click opens playlist");
                client.gui.screen().onClose();
                if (client.gui.screen() != screen) throw new AssertionError("Playlist returns to previous menu");
            });
        }
        context.runOnClient(client -> KoreanMusic.play(0));
        context.waitTicks(30);
        var stopped = new net.minecraft.client.resources.sounds.SoundInstance[1];
        context.runOnClient(client -> {
            if (!KoreanMusic.PLAYBACK.requested() || KoreanMusic.PLAYBACK.selected() != 0)
                throw new AssertionError("Playlist starts its selected song");
            try {
                var field = KoreanMusic.class.getDeclaredField("sound"); field.setAccessible(true);
                var sound = (net.minecraft.client.resources.sounds.SoundInstance)field.get(null);
                if (sound == null || !client.getSoundManager().isActive(sound)) throw new AssertionError("Original Korean stream is active");
                stopped[0] = sound;
                KoreanMusic.stop();
            } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        });
        context.waitTicks(5);
        context.runOnClient(client -> {
            if (client.getSoundManager().isActive(stopped[0])) throw new AssertionError("Playlist stop releases sound");
        });
        context.runOnClient(client -> StoryMusicAudio.play("music.korea.extinction"));
        context.waitTicks(30);
        context.runOnClient(client -> {
            try {
                var field=StoryMusicAudio.class.getDeclaredField("sound");field.setAccessible(true);
                var sound=(net.minecraft.client.resources.sounds.SoundInstance)field.get(null);
                if(sound==null || sound.getSource()!=net.minecraft.sounds.SoundSource.MASTER || !client.getSoundManager().isActive(sound))
                    throw new AssertionError("Source focus music must play through Master volume");
                StoryMusicAudio.reset();
                if(StoryMusicAudio.active())throw new AssertionError("Source music reset retains pending cue");
            }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(previousMusic[0]);
        });
        context.getInput().resizeWindow(1600, 1000);
        context.setScreen(() -> null);
    }
}
