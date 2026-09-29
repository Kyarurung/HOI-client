package dev.hoi.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public abstract class MixinCountryNameWater {
    private static final FontDescription HOI_COUNTRY_FONT = new FontDescription.Resource(Identifier.parse("hoi:country_names"));

    @Redirect(method = "submitInner(Lnet/minecraft/client/renderer/entity/state/TextDisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitText(Lcom/mojang/blaze3d/vertex/PoseStack;FFLnet/minecraft/util/FormattedCharSequence;ZLnet/minecraft/client/gui/Font$DisplayMode;IIII)V"))
    private void hoi$keepCountryNamesAboveWater(SubmitNodeCollector collector, PoseStack pose, float x, float y,
            FormattedCharSequence text, boolean shadow, Font.DisplayMode mode, int light, int color, int background, int outline) {
        collector.submitText(pose, x, y, text, shadow, mode, light, color, background, outline);
        if (mode == Font.DisplayMode.SEE_THROUGH && !text.accept((index, style, code) -> !HOI_COUNTRY_FONT.equals(style.getFont()))) {
            collector.submitText(pose, x, y, text, shadow, Font.DisplayMode.POLYGON_OFFSET, light, color, background, outline);
        }
    }
}
