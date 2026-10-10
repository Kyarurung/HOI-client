package dev.hoi.client.screen;

import dev.hoi.client.audio.AudioKoreanMusic;
import dev.hoi.client.audio.AudioPlaylistGainTest;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.protocol.ProtocolDialogPayloads;
import dev.hoi.protocol.ProtocolDialogView;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.resources.sounds.SoundInstance;

public final class ScreenDialogVolumeTest {
    private ScreenDialogVolumeTest() {}
    public static void run(ClientGameTestContext context) {
        for (double volume : new double[]{0, .35, 1}) {
            var view = new ProtocolDialogView(UUID.randomUUID().toString(), 1, ProtocolDialogView.Kind.DIPLOMACY,
                    "국가 중점 완료", "", "", "", "menu/focus", "", List.of(),
                    List.of(new ProtocolDialogView.Choice("ack", "확인")), "focus_complete");
            context.runOnClient(client -> {
                ScreenDialogCoordinator.reset();
                client.gui.setScreen(null);
                AudioUiSounds.reset();
                client.getSoundManager().stop(net.minecraft.resources.Identifier.parse("hoi:ui.focus.complete"), net.minecraft.sounds.SoundSource.MASTER);
                AudioKoreanMusic.volume(volume);
            });
            context.waitFor(client -> {
                var engine = ((dev.hoi.client.mixin.MixinManagerAccess)client.getSoundManager()).hoiSoundEngine();
                return ((dev.hoi.client.mixin.MixinEngineAccess)engine).hoiChannels().keySet().stream()
                        .noneMatch(sound -> sound.getIdentifier().toString().equals("hoi:ui.focus.complete"));
            });
            context.runOnClient(client -> ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(view, true)));
            context.waitTicks(2);
            var sound = context.computeOnClient(client -> {
                var engine = ((dev.hoi.client.mixin.MixinManagerAccess)client.getSoundManager()).hoiSoundEngine();
                var sounds = ((dev.hoi.client.mixin.MixinEngineAccess)engine).hoiChannels().keySet().stream()
                        .filter(s -> s.getIdentifier().toString().equals("hoi:ui.focus.complete")).toList();
                if (sounds.size() != 1) throw new AssertionError("Completion must create one audio channel, actual: " + sounds.size());
                return sounds.getFirst();
            });
            AudioPlaylistGainTest.assertGain(context, sound, volume * .4);
            context.runOnClient(client -> {
                ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(view, true));
                ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(view.issued(view.token(), 2), false));
                var engine = ((dev.hoi.client.mixin.MixinManagerAccess)client.getSoundManager()).hoiSoundEngine();
                long count = ((dev.hoi.client.mixin.MixinEngineAccess)engine).hoiChannels().keySet().stream()
                        .filter(s -> s.getIdentifier().toString().equals("hoi:ui.focus.complete")).count();
                if (count != 1) throw new AssertionError("Duplicate/refresh completion replayed its cue");
                AudioKoreanMusic.volume(.25);
            });
            context.waitTicks(2);
            AudioPlaylistGainTest.assertGain(context, sound, .1);
            context.runOnClient(client -> AudioKoreanMusic.volume(0));
            context.waitTicks(2);
            AudioPlaylistGainTest.assertGain(context, sound, 0);
            context.runOnClient(client -> {
                AudioKoreanMusic.volume(1);
                ScreenDialogCoordinator.close(view.token());
                if (client.gui.screen() != null) throw new AssertionError("Completion cancellation retained the popup");
            });
            context.waitTicks(2);
            AudioPlaylistGainTest.assertGain(context, sound, .4);
        }
        context.runOnClient(client -> {
            ScreenDialogCoordinator.reset();
            client.getSoundManager().stop(net.minecraft.resources.Identifier.parse("hoi:ui.focus.complete"), net.minecraft.sounds.SoundSource.MASTER);
        });
    }
}
