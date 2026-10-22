package dev.hoi.client.mixin;

import dev.hoi.client.input.InputMapInfo;

import dev.hoi.client.input.InputConstructionMap;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinConstructionClick {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void hoi$placeConstruction(CallbackInfoReturnable<Boolean> callback) {
        if (InputConstructionMap.place((Minecraft) (Object) this) || dev.hoi.client.input.InputMapInfo.click((Minecraft) (Object) this)) callback.setReturnValue(false);
    }
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void hoi$cancelConstruction(CallbackInfo callback) {
        if (InputConstructionMap.cancel((Minecraft) (Object) this) || dev.hoi.client.map.MapCommanderOrders.useMap((Minecraft) (Object) this)) callback.cancel();
    }
}
