package dev.hoi.client.mixin.audio;

import dev.hoi.client.audio.AudioKoreanMusic;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class AudioSoundResume {
    @Inject(method = "resume", at = @At("RETURN"))
    private void hoiRestorePlaylistPause(CallbackInfo ci) {
        AudioKoreanMusic.restorePause();
    }
}
