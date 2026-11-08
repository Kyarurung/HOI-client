package dev.hoi.client.mixin.atlas;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class AtlasChunkFade {
    @Inject(method = "getVisibility", at = @At("HEAD"), cancellable = true)
    private void hoi$visibleImmediately(long now, long duration, CallbackInfoReturnable<Float> result) {
        result.setReturnValue(1.0f);
    }
}
