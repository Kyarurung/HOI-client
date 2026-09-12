package dev.hoi.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MenuBackgroundMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void hoi$clearMenuBackground(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo callback) {
        if (getClass().getPackageName().startsWith("dev.hoi.client.")) callback.cancel();
    }
}
