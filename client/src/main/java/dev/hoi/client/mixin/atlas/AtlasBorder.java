package dev.hoi.client.mixin.atlas;

import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldBorder.class)
public abstract class AtlasBorder {
    @Unique private VoxelShape hoi$collision;
    @Unique private double hoi$width,hoi$depth;
    @Unique private boolean hoi$atlas() { var level=net.minecraft.client.Minecraft.getInstance().level;return level!=null&&level.getWorldBorder()==(Object)this&&level.dimension().identifier().toString().startsWith("hoi:atlas_"); }
    @Inject(method={"getMinX(F)D","getMinZ(F)D"},at=@At("HEAD"),cancellable=true)
    private void hoi$minimum(float partialTick,CallbackInfoReturnable<Double> result) {
        if(hoi$atlas())result.setReturnValue(0.0);
    }
    @Inject(method="getMaxX(F)D",at=@At("HEAD"),cancellable=true)
    private void hoi$maximumX(float partialTick,CallbackInfoReturnable<Double> result) {
        if(hoi$atlas())result.setReturnValue(((WorldBorder)(Object)this).getCenterX()*2);
    }
    @Inject(method="getMaxZ(F)D",at=@At("HEAD"),cancellable=true)
    private void hoi$maximumZ(float partialTick,CallbackInfoReturnable<Double> result) {
        if(hoi$atlas())result.setReturnValue(((WorldBorder)(Object)this).getCenterZ()*2);
    }
    @Inject(method="getCollisionShape",at=@At("HEAD"),cancellable=true)
    private void hoi$collision(CallbackInfoReturnable<VoxelShape> result) {
        if(!hoi$atlas())return;
        var border=(WorldBorder)(Object)this;
        double width=border.getCenterX()*2,depth=border.getCenterZ()*2;
        if(hoi$collision==null||hoi$width!=width||hoi$depth!=depth) {
            hoi$width=width;hoi$depth=depth;
            hoi$collision=Shapes.join(Shapes.INFINITY,Shapes.box(0,Double.NEGATIVE_INFINITY,0,width,Double.POSITIVE_INFINITY,depth),BooleanOp.ONLY_FIRST);
        }
        result.setReturnValue(hoi$collision);
    }
}
