package dev.hoi.client.mixin;

import dev.hoi.client.map.MapDistantTerrainRenderer;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class MixinAtlasRenderDistance {
    @Inject(method = "getEffectiveRenderDistance", at = @At("HEAD"), cancellable = true)
    private void hoi$atlasDistance(CallbackInfoReturnable<Integer> result) {
        if (MapDistantTerrainRenderer.active()) result.setReturnValue(((Options)(Object)this).renderDistance().get());
    }
}
