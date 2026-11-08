package dev.hoi.client.mixin.atlas;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSection", remap = false)
public abstract class AtlasSodiumChunkFade {
    @Inject(method = "consumeFade", at = @At("RETURN"), cancellable = true)
    private void hoi$skipFade(CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(false);
    }
}
