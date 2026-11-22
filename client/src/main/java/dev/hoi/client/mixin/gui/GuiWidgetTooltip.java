package dev.hoi.client.mixin.gui;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WidgetTooltipHolder.class)
public abstract class GuiWidgetTooltip {
    @Shadow private Tooltip tooltip;
    @Unique private Tooltip hoiSummary, hoiFull;
    @Unique private long hoiHoverStart = -1;
    @ModifyVariable(method = "refreshTooltipForNextRenderPass", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean hoiDelayedTooltip(boolean hovered) {
        Tooltip summary = hoiFull != null && tooltip == hoiFull ? hoiSummary : tooltip;
        Tooltip full = TooltipDelay.full(summary);
        if (full == null) { hoiSummary = hoiFull = null; hoiHoverStart = -1; return hovered; }
        hoiSummary = summary; hoiFull = full;
        long now = Util.getMillis();
        if (!hovered) hoiHoverStart = -1;
        else if (hoiHoverStart < 0) hoiHoverStart = now;
        tooltip = hovered && now - hoiHoverStart >= TooltipDelay.DELAY ? full : summary;
        return hovered;
    }
    @ModifyVariable(method = "refreshTooltipForNextRenderPass", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean hoiHoverOnly(boolean focused) {
        var screen = Minecraft.getInstance().gui.screen();
        boolean campaign = screen == null ? ClientCampaignHud.visible() : screen.getClass().getPackageName().startsWith("dev.hoi.client.");
        return focused && !campaign;
    }
}
