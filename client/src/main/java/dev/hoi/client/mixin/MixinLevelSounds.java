package dev.hoi.client.mixin;

import dev.hoi.client.audio.AudioUiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MixinLevelSounds {
    @Redirect(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"))
    private void hoiPreserveMusic(SoundManager sounds, ClientLevel level, boolean stopSounds) {
        if (level == null) sounds.stop();
        else AudioUiSounds.stopWorldSounds(sounds);
    }
}
