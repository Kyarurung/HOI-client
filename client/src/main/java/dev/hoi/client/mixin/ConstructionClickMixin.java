package dev.hoi.client.mixin;

import dev.hoi.client.ConstructionMapInput;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class ConstructionClickMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void hoi$cancelConstruction(CallbackInfoReturnable<Boolean> callback) {
        if (ConstructionMapInput.cancel((Minecraft) (Object) this)) callback.setReturnValue(false);
    }
}
