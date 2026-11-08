package dev.hoi.client.mixin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiMenuTooltip {
    @org.spongepowered.asm.mixin.injection.Inject(method = "extractDeferredElements", at = @At("TAIL"))
    private void hoiHudTooltip(int mouseX, int mouseY, float delta, org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        dev.hoi.client.ui.hud.HudTooltip.extractDeferred((GuiGraphicsExtractor)(Object)this);
    }
    private static boolean hoiText() {
        var screen = Minecraft.getInstance().gui.screen();
        return screen == null ? dev.hoi.client.ClientCampaignHud.visible()
                : screen.getClass().getPackageName().startsWith("dev.hoi.client.");
    }
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "requestCursor", at = @At("HEAD"), argsOnly = true)
    private com.mojang.blaze3d.platform.cursor.CursorType hoiCursor(com.mojang.blaze3d.platform.cursor.CursorType value) {
        return hoiText() && value == com.mojang.blaze3d.platform.cursor.CursorTypes.NOT_ALLOWED
                ? com.mojang.blaze3d.platform.cursor.CursorTypes.ARROW : value;
    }
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private net.minecraft.util.FormattedCharSequence hoiEffectText(net.minecraft.util.FormattedCharSequence value) {
        return hoiText() ? dev.hoi.client.ui.UiEffectColors.text(value) : value;
    }
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int hoiEffectColor(int value) {
        return hoiText() ? dev.hoi.client.ui.UiEffectColors.color(value) : value;
    }
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean hoiTextShadow(boolean value) {
        return !hoiText() && value;
    }
    @Redirect(method = "tooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"))
    private org.joml.Vector2ic menuTooltipPosition(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner,int width,int height,int mouseX,int mouseY,int textWidth,int textHeight) {
        return hoiText()?dev.hoi.client.ui.tooltip.Tooltips.position(width,height,mouseX,mouseY,textWidth,textHeight):positioner.positionTooltip(width,height,mouseX,mouseY,textWidth,textHeight);
    }
    @Redirect(method = "tooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;extractTooltipBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIILnet/minecraft/resources/Identifier;)V"))
    private void menuTooltipBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Identifier style) {
        if (hoiText()) {
            dev.hoi.client.ui.tooltip.Tooltips.background(graphics,x-7,y-7,width+14,height+14);
        } else {
            TooltipRenderUtil.extractTooltipBackground(graphics, x, y, width, height, style);
        }
    }
}
