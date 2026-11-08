package dev.hoi.client.mixin.audio;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AudioMenuSound {
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void hoiClick(SoundManager manager, CallbackInfo callback) {
        var screen = net.minecraft.client.Minecraft.getInstance().gui.screen();
        if (screen != null && screen.getClass().getPackageName().startsWith("dev.hoi.client")) {
            dev.hoi.client.audio.AudioUiSounds.play("ui.click");
            callback.cancel();
        }
    }
}
