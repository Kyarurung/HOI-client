package dev.hoi.client.mixin.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiTooltipLayer {
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void hoiTooltipLayer(DeltaTracker delta,boolean renderLevel,boolean renderHud,CallbackInfo callback) {
        dev.hoi.client.ui.tooltip.TooltipLayer.flush();
    }
}
