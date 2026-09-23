package dev.hoi.client.mixin;

import dev.hoi.client.input.InputMenuShortcut;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardShortcut {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void hoi$menuShortcut(long window, int action, KeyEvent event, CallbackInfo callback) {
        if (InputMenuShortcut.handle(Minecraft.getInstance(), window, action, event)) callback.cancel();
    }
}
