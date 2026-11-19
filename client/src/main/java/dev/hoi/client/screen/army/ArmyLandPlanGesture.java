package dev.hoi.client.screen.army;

import dev.hoi.protocol.commander.CommanderHudPayloads.Ray;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

public final class ArmyLandPlanGesture {
    private record Point(double x,double y,Ray ray) {}
    private final List<Point> points=new ArrayList<>();
    private Vec3 origin;
    private String dimension="";
    private boolean fixed;
    public boolean active(){return !points.isEmpty();}
    public void clear(){points.clear();origin=null;dimension="";}
    private boolean compatible(Minecraft client){return client.level!=null&&client.options.getCameraType().isFirstPerson()&&dimension.equals(client.level.dimension().identifier().toString())&&origin!=null&&(fixed||origin.distanceToSqr(client.gameRenderer.mainCamera().position())<.0001);}
    public boolean begin(Minecraft client,double x,double y,int width,int height,boolean fixed) {
        clear();this.fixed=fixed;if(client.level==null||!client.options.getCameraType().isFirstPerson())return false;
        dimension=client.level.dimension().identifier().toString();origin=client.gameRenderer.mainCamera().position();sample(client,x,y,width,height,true);return active();
    }
    public void sample(Minecraft client,double x,double y,int width,int height,boolean force) {
        if(!compatible(client)){clear();return;}
        if(fixed&&!points.isEmpty())return;
        if(!points.isEmpty()&&!force&&Math.hypot(x-points.getLast().x(),y-points.getLast().y())<6)return;
        var matrix=client.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
        var vector=matrix.transformProject(new org.joml.Vector3f((float)(x/width*2-1),(float)(1-y/height*2),1)).normalize();
        try {
            var point=new Point(x,y,new Ray(vector.x,vector.y,vector.z));
            if(points.size()==64)points.set(63,point);else points.add(point);
        }catch(IllegalArgumentException sky){}
    }
    public List<Ray> finish(Minecraft client){var result=compatible(client)?points.stream().map(Point::ray).toList():List.<Ray>of();clear();return result;}
    public void draw(GuiGraphicsExtractor graphics) {
        for(int i=1;i<points.size();i++) {
            var from=points.get(i-1);var to=points.get(i);int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(to.x()-from.x()),Math.abs(to.y()-from.y()))));
            for(int j=0;j<=steps;j++){int x=(int)Math.round(from.x()+(to.x()-from.x())*j/steps),y=(int)Math.round(from.y()+(to.y()-from.y())*j/steps);graphics.fill(x-1,y-1,x+2,y+2,0xFF6BCE50);}
        }
    }
}
