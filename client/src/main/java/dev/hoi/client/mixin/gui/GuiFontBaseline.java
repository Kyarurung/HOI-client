package dev.hoi.client.mixin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public abstract class GuiFontBaseline {
    @Shadow private float y;
    @Unique private float hoiBaselineOffset;
    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;I)Z", at = @At("HEAD"))
    private void hoiAlignBaseline(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> callback) {
        var client = Minecraft.getInstance();
        var screen = client.gui.screen();
        boolean active = screen == null ? dev.hoi.client.ClientCampaignHud.visible()
                : screen.getClass().getPackageName().startsWith("dev.hoi.client.");
        hoiBaselineOffset = active && codePoint >= 33 && codePoint <= 126
                && style.getFont().equals(FontDescription.DEFAULT) && !client.options.forceUnicodeFont().get() ? 1 : 0;
        y += hoiBaselineOffset;
    }
    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;I)Z", at = @At("RETURN"))
    private void hoiRestoreBaseline(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> callback) {
        y -= hoiBaselineOffset;
    }
}
