package dev.hoi.client.mixin;

import dev.hoi.client.ConstructionMapInput;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class ConstructionClickMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void hoi$placeConstruction(CallbackInfoReturnable<Boolean> callback) {
        if (ConstructionMapInput.place((Minecraft) (Object) this) || dev.hoi.client.MapInfoInput.click((Minecraft) (Object) this)) callback.setReturnValue(false);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void hoi$cancelConstruction(CallbackInfo callback) {
        if (ConstructionMapInput.cancel((Minecraft) (Object) this)) callback.cancel();
    }
}
