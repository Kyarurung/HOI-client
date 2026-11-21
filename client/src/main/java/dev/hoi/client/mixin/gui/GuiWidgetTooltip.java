package dev.hoi.client.mixin.gui;

import dev.hoi.client.ClientCampaignHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WidgetTooltipHolder.class)
public abstract class GuiWidgetTooltip {
    @ModifyVariable(method = "refreshTooltipForNextRenderPass", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean hoiHoverOnly(boolean focused) {
        var screen = Minecraft.getInstance().gui.screen();
        boolean campaign = screen == null ? ClientCampaignHud.visible() : screen.getClass().getPackageName().startsWith("dev.hoi.client.");
        return focused && !campaign;
    }
}
