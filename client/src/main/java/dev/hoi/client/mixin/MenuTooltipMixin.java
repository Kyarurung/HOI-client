package dev.hoi.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MenuTooltipMixin {
    private static boolean hoiText() {
        var screen = Minecraft.getInstance().gui.screen();
        return screen == null ? dev.hoi.client.CampaignHud.visible()
                : screen.getClass().getPackageName().startsWith("dev.hoi.client.");
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private net.minecraft.util.FormattedCharSequence hoiEffectText(net.minecraft.util.FormattedCharSequence value) {
        return hoiText() ? dev.hoi.client.ui.EffectColors.text(value) : value;
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int hoiEffectColor(int value) {
        return hoiText() ? dev.hoi.client.ui.EffectColors.color(value) : value;
    }

    @Redirect(method = "tooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;extractTooltipBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIILnet/minecraft/resources/Identifier;)V"))
    private void menuTooltipBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Identifier style) {
        var screen = Minecraft.getInstance().gui.screen();
        if (screen != null && screen.getClass().getPackageName().startsWith("dev.hoi.client.")) {
            graphics.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0xF0100812);
            graphics.outline(x - 3, y - 3, width + 6, height + 6, 0xFF873B98);
        } else {
            TooltipRenderUtil.extractTooltipBackground(graphics, x, y, width, height, style);
        }
    }
}
