package dev.hoi.client.map.air;

import com.mojang.blaze3d.platform.NativeImage;
import dev.hoi.protocol.map.MapAtlasScene.Material;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.client.map.scene.SceneMesh;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import java.io.IOException;
import java.util.*;

public final class AirArrowOverlay {
    public record Mesh(SceneMesh.SceneTile tile,AirArrowKind kind,AirArrowState state) {}
    private static final Map<AirArrowKind,Identifier> TEXTURES=new EnumMap<>(AirArrowKind.class);
    private static final Set<AirArrowKind> MISSING=EnumSet.noneOf(AirArrowKind.class);
    private static List<AirArrow> cachedArrows=List.of();
    private static List<Mesh> cachedMeshes=List.of();
    private static long cachedTick=Long.MIN_VALUE;
    private AirArrowOverlay() {}
    public static int color(AirArrowState state){return switch(state){case SELECTED->0xccffff00;case ACTIVE->0x8000ff00;case NONACTIVE->0x33ffffff;};}
    public static Identifier texture(AirArrowKind kind){
        if(TEXTURES.containsKey(kind))return TEXTURES.get(kind);
        if(MISSING.contains(kind))return null;
        String name=kind==AirArrowKind.MISSION?"air_mission":"air_transfer";
        var client=Minecraft.getInstance();var resources=client.getResourceManager();
        try(var patternInput=resources.open(Identifier.parse("hoi:textures/gui/map/"+name+".png"));var maskInput=resources.open(Identifier.parse("hoi:textures/gui/map/"+name+"_mask.png"));var mask=NativeImage.read(maskInput)){
            var pattern=NativeImage.read(patternInput);
            DynamicTexture texture;
            try{
                if(pattern.getWidth()!=mask.getWidth()||pattern.getHeight()!=mask.getHeight())throw new IOException("Air arrow pattern/mask size mismatch: "+name);
                for(int y=0;y<pattern.getHeight();y++)for(int x=0;x<pattern.getWidth();x++){
                    int pixel=pattern.getPixel(x,y),m=mask.getPixel(x,y),alpha=(pixel>>>24)*(m>>>24)/255*(m>>8&255)/255;
                    pattern.setPixel(x,y,alpha<<24|pixel&0xffffff);
                }
                texture=new DynamicTexture(()->"HOI source air arrow "+name,pattern);
            }catch(IOException|RuntimeException|Error failure){pattern.close();throw failure;}
            var id=Identifier.parse("hoi:map/"+name+"_masked");
            try{client.getTextureManager().register(id,texture);}catch(RuntimeException|Error failure){texture.close();throw failure;}
            TEXTURES.put(kind,id);return id;
        }catch(IOException failure){MISSING.add(kind);return null;}
    }
    public static void clear(){TEXTURES.values().forEach(id->Minecraft.getInstance().getTextureManager().release(id));TEXTURES.clear();MISSING.clear();cachedArrows=List.of();cachedMeshes=List.of();cachedTick=Long.MIN_VALUE;}
    public static List<Mesh> build(List<AirArrow> arrows,double seconds){
        long tick=(long)Math.floor(seconds*20);
        if(arrows.equals(cachedArrows)&&(tick==cachedTick||arrows.stream().noneMatch(a->a.kind()==AirArrowKind.TRANSFER)))return cachedMeshes;
        var meshes=new ArrayList<Mesh>();
        for(var arrow:arrows){
            double dx=arrow.to().x()-arrow.from().x(),dz=arrow.to().z()-arrow.from().z(),length=Math.hypot(dx,dz);
            if(length<.0001)continue;
            boolean mission=arrow.kind()==AirArrowKind.MISSION;double scale=Math.min(1,Math.max(mission?.1:.2,length*4/40));
            double width=(mission?13:.8)/4*scale,head=mission?Math.min(length*.4,14./4*scale):0,body=length-head;
            var vertices=new FloatArrayList();
            if(mission){
                strip(vertices,arrow,0,body/length,.001,.499,.3,.7,width*7/13,length);
                strip(vertices,arrow,body/length,1,.501,.998,.001,.998,width,length);
            }else{
                double loop=1.5,phase=(seconds*.2)%1,start=0;
                while(start<1){double u=(start*loop-phase)%1;if(u<0)u++;double end=Math.min(1,start+(1-u)/loop);if(end-start<.000001)end=Math.min(1,start+1/loop);
                    strip(vertices,arrow,start,end,u,Math.min(1,u+(end-start)*loop),.295,.705,width,length);start=end;
                }
            }
            var v=vertices.toFloatArray();AABB bounds=new AABB(v[0],v[1],v[2],v[0],v[1],v[2]);for(int i=8;i<v.length;i+=8)bounds=bounds.minmax(new AABB(v[i],v[i+1],v[i+2],v[i],v[i+1],v[i+2]));
            meshes.add(new Mesh(new SceneMesh.SceneTile(Material.DEPLOYMENT,bounds.inflate(.01),v,List.of()),arrow.kind(),arrow.state()));
        }
        cachedArrows=List.copyOf(arrows);cachedTick=tick;cachedMeshes=List.copyOf(meshes);return cachedMeshes;
    }
    private static void strip(FloatArrayList vertices,AirArrow arrow,double start,double end,double u0,double u1,double v0,double v1,double width,double length){
        int segments=Math.max(1,(int)Math.ceil((end-start)*32));
        for(int i=0;i<segments;i++){
            double a=(double)i/segments,b=(double)(i+1)/segments;
            vertex(vertices,arrow,start+(end-start)*a,-width/2,u0+(u1-u0)*a,v0,length);
            vertex(vertices,arrow,start+(end-start)*a,width/2,u0+(u1-u0)*a,v1,length);
            vertex(vertices,arrow,start+(end-start)*b,width/2,u0+(u1-u0)*b,v1,length);
            vertex(vertices,arrow,start+(end-start)*b,-width/2,u0+(u1-u0)*b,v0,length);
        }
    }
    private static void vertex(FloatArrayList vertices,AirArrow arrow,double t,double side,double u,double v,double length){
        double dx=arrow.to().x()-arrow.from().x(),dz=arrow.to().z()-arrow.from().z(),rise=Math.min(3,length*.12)*4*t*(1-t);
        vertices.add((float)(arrow.from().x()+dx*t-dz/length*side));vertices.add((float)(arrow.from().y()+(arrow.to().y()-arrow.from().y())*t+rise));vertices.add((float)(arrow.from().z()+dz*t+dx/length*side));
        vertices.add((float)u);vertices.add((float)v);vertices.add(0);vertices.add(1);vertices.add(0);
    }
}
