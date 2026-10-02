package dev.hoi.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hoi.client.map.MapCountryNameVisibility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class MixinAtlasProjection {
    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private float hoi$atlasProjection(float original) {
        var client = Minecraft.getInstance();
        if (!MapCountryNameVisibility.atlas(client.level) || client.player == null) return original;
        var pos = ((Camera)(Object)this).position();
        var border = client.level.getWorldBorder();
        double x = Math.max(Math.abs(pos.x), Math.abs(pos.x - border.getCenterX() * 2));
        double z = Math.max(Math.abs(pos.z), Math.abs(pos.z - border.getCenterZ() * 2));
        double y = Math.max(Math.abs(pos.y - client.level.getMinY()), Math.abs(pos.y - client.level.getMaxY()));
        return Math.max(original, (float)Math.sqrt(x * x + y * y + z * z) + 64);
    }
}
