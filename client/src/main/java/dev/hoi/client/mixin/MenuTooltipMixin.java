package dev.hoi.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MenuTooltipMixin {
    @Inject(method = "setTooltipForNextFrame", at = @At("HEAD"), cancellable = true)
    private void hideMenuTooltip(CallbackInfo callback) {
        var screen = Minecraft.getInstance().gui.screen();
        if (dev.hoi.client.ui.HoiMenuBar.showingTensionTooltip) return;
        if (screen instanceof dev.hoi.client.screen.ConstructionScreen construction && construction.showingConsumerTooltip()) return;
        if (screen != null && screen.getClass().getPackageName().startsWith("dev.hoi.client.")) callback.cancel();
    }
}
