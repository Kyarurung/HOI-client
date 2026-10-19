package dev.hoi.client.audio;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class AudioStorySignalsTest {
    private AudioStorySignalsTest() {}
    public static void run(ClientGameTestContext context) {
        var heard = new ArrayList<String>();
        net.minecraft.client.sounds.SoundEventListener listener = (sound, event, range) -> {
            if (sound.getIdentifier().getNamespace().equals("hoi")) heard.add(sound.getIdentifier().getPath());
        };
        try {
            context.runOnClient(client -> {
                AudioUiSounds.reset();
                client.getSoundManager().addListener(listener);
                AudioStorySounds.play("sound.story.siren_sound_effect");
                AudioStorySounds.play("sound.story.unavailable_source_key");
                if (!heard.equals(List.of("ui.story.tfr.siren_sound_effect"))) throw new AssertionError("Play only the registered raw source effect once: " + heard);
                AudioStoryMusic.play("music.story.one_minute_of_silence");
            });
            context.waitTicks(5);
            context.runOnClient(client -> {
                var playing = AudioStoryMusic.playing();
                if (playing == null || !client.getSoundManager().isActive(playing)) throw new AssertionError("Original silent music must load and play");
                if (playing.getSource() != net.minecraft.sounds.SoundSource.MASTER) throw new AssertionError("Story music follows Master");
                AudioStoryMusic.play("music.story.unavailable_source_key");
                if (AudioStoryMusic.playing() != playing) throw new AssertionError("Unregistered source does not replace the current music");
                AudioSuperEvent.play("story-signal-fixture", "super_event/eurasian_chinese_war_super_event");
                AudioStoryMusic.play("music.story.one_minute_of_silence");
            });
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (!AudioSuperEvent.active() || AudioStoryMusic.playing() != null || !AudioStoryMusic.active()) throw new AssertionError("Super event keeps priority over pending story music");
                AudioSuperEvent.stop("story-signal-fixture");
            });
            context.waitTicks(4);
            context.runOnClient(client -> {
                if (AudioStoryMusic.playing() == null) throw new AssertionError("Pending story music resumes after the super event ends");
                AudioUiSounds.reset();
                if (AudioStoryMusic.active() || AudioSuperEvent.active()) throw new AssertionError("Stop clears pending and active story audio");
            });
        } finally {
            context.runOnClient(client -> { client.getSoundManager().removeListener(listener); AudioUiSounds.reset(); AudioStorySounds.clear(); });
        }
    }
}
