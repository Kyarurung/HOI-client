package dev.hoi.client.audio;

import dev.hoi.client.screen.KoreanMusicScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;

public final class KoreanMusicChecks {
    public static void run(ClientGameTestContext context) {
        double[] previousMusic=new double[1];
        context.runOnClient(client -> {
            previousMusic[0]=client.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC);
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(1.0);
            KoreanMusic.reload(client.getResourceManager());
            var tracks = KoreanMusic.PLAYBACK.tracks();
            if (tracks.size() != 26) throw new AssertionError("All original Korean playlist tracks must load");
            for (var track : tracks) if (client.getSoundManager().getSoundEvent(UiSounds.id(track.event())) == null)
                throw new AssertionError("Missing playlist sound: " + track.event());
        });
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
                KoreanMusic.PLAYBACK.stop();
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
                if(sound==null || sound.getSource()!=net.minecraft.sounds.SoundSource.MUSIC || !client.getSoundManager().isActive(sound))
                    throw new AssertionError("Source focus music must play through Music volume");
                StoryMusicAudio.reset();
                if(StoryMusicAudio.active())throw new AssertionError("Source music reset retains pending cue");
            }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(previousMusic[0]);
        });
        context.getInput().resizeWindow(1600, 1000);
        context.setScreen(() -> null);
    }
}
