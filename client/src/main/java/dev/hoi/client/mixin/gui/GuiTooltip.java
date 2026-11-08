package dev.hoi.client.mixin.gui;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.ui.tooltip.Tooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Tooltip.class)
public abstract class GuiTooltip {
    @ModifyConstant(method = "splitTooltip", constant = @Constant(intValue = 170))
    private static int hoiWidth(int original) {
        var client = Minecraft.getInstance();
        var screen = client.gui.screen();
        boolean campaign = screen == null ? ClientCampaignHud.visible() : screen.getClass().getPackageName().startsWith("dev.hoi.client.");
        return campaign ? Tooltips.width(client.getWindow().getGuiScaledWidth(), 14) : original;
    }
}
