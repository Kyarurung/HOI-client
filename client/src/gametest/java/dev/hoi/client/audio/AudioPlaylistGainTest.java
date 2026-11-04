package dev.hoi.client.audio;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.openal.AL10;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class AudioPlaylistGainTest {
    private AudioPlaylistGainTest() {}
    public static void run(ClientGameTestContext context) {
        double previous = context.computeOnClient(client -> AudioKoreanMusic.volume());
        double master = context.computeOnClient(client -> client.options.getSoundSourceVolume(SoundSource.MASTER));
        double music = context.computeOnClient(client -> client.options.getSoundSourceVolume(SoundSource.MUSIC));
        try {
            context.runOnClient(client -> {
                AudioUiSounds.reset();
                AudioKoreanMusic.stop();
                client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(.4);
                client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);
            });
            dev.hoi.client.screen.dialog.DialogScreenVolumeTest.run(context);
            context.runOnClient(client -> {
                AudioKoreanMusic.volume(.35);
                AudioUiSounds.reset();
                AudioKoreanMusic.loadVolume();
                if (AudioKoreanMusic.volume() != .35) throw new AssertionError("Disconnect/reset or persisted reload changed playlist volume");
                var click = AudioUiSounds.instance("ui.click");
                click.getOrResolve(client.getSoundManager());
                if (click.getVolume() != .45f) throw new AssertionError("Playlist setting changed unrelated UI effects");
                var research = AudioUiSounds.instance("ui.research.complete");
                research.getOrResolve(client.getSoundManager());
                if (research.getVolume() != .5f) throw new AssertionError("Existing research sample gain was changed");
            });
        } finally {
            context.runOnClient(client -> {
                AudioUiSounds.reset();
                AudioKoreanMusic.volume(previous);
                client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(master);
                client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(music);
            });
        }
    }
    public static void assertGain(ClientGameTestContext context, SoundInstance sound, double expected) {
        var result = new CompletableFuture<Float>();
        context.runOnClient(client -> {
            if (sound == null || sound.getSource() != SoundSource.MASTER) throw new AssertionError("Missing MASTER audio instance");
            var engine = ((dev.hoi.client.mixin.MixinManagerAccess)client.getSoundManager()).hoiSoundEngine();
            var channel = ((dev.hoi.client.mixin.MixinEngineAccess)engine).hoiChannels().get(sound);
            if (channel == null) throw new AssertionError("Missing active audio channel: " + sound.getIdentifier());
            channel.execute(value -> {
                try {
                    var field = value.getClass().getDeclaredField("source");
                    field.setAccessible(true);
                    result.complete(AL10.alGetSourcef(field.getInt(value), AL10.AL_GAIN) * AL10.alGetListenerf(AL10.AL_GAIN));
                } catch (ReflectiveOperationException error) {
                    result.completeExceptionally(error);
                }
            });
        });
        try {
            float actual = result.get(10, TimeUnit.SECONDS);
            if (Math.abs(actual - expected) > .002) throw new AssertionError("Actual OpenAL gain " + actual + " != " + expected + " for " + sound.getIdentifier());
        } catch (Exception error) {
            throw new AssertionError("Audio gain verification failed", error);
        }
    }
}
