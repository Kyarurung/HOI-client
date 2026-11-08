package dev.hoi.client.mixin.atlas;

import dev.hoi.client.map.MapCountryNameVisibility;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.multiplayer.ClientLevel;

@Mixin(LevelExtractor.class)
public abstract class AtlasCountryNameChunk {
    @Inject(method = "isEntityVisible", at = @At("HEAD"))
    private void hoi$countryMetadata(Entity entity, Frustum frustum, double x, double y, double z,
                                     float partialTick, long duration, CallbackInfoReturnable<Boolean> result) {
        if (MapCountryNameVisibility.independent(entity)
                && entity.level() instanceof ClientLevel level && !level.isTickingEntity(entity)) entity.tick();
    }
    @Redirect(method = "isEntityVisible", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;J)Z"))
    private boolean hoi$countryChunk(LevelRenderer renderer, BlockPos pos, long fade,
            Entity entity, Frustum frustum, double x, double y, double z, float partialTick, long duration) {
        return MapCountryNameVisibility.independent(entity) || renderer.isSectionCompiledAndVisible(pos, fade);
    }
}
