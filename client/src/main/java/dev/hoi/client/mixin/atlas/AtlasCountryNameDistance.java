package dev.hoi.client.mixin.atlas;

import dev.hoi.client.map.MapCountryNameVisibility;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Display.class)
public abstract class AtlasCountryNameDistance {
    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void hoi$countryDistance(double distance, CallbackInfoReturnable<Boolean> result) {
        if (MapCountryNameVisibility.independent((Entity)(Object)this)) result.setReturnValue(true);
    }
}
