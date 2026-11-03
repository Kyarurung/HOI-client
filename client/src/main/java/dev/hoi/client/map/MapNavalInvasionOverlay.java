package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.hoi.protocol.ProtocolAtlasScene.Material;
import dev.hoi.protocol.ProtocolNavalInvasion;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MapNavalInvasionOverlay {
    record Mesh(MapSceneMesh.MapTile tile,ProtocolNavalInvasion.Plan plan) {}
    record Hit(String id,String detail,float[] corners) {
        boolean contains(double x,double y){
            boolean inside=false;int count=corners.length/2;
            for(int i=0,j=count-1;i<count;j=i++)if((corners[i*2+1]>y)!=(corners[j*2+1]>y)&&x<(corners[j*2]-corners[i*2])*(y-corners[i*2+1])/(corners[j*2+1]-corners[i*2+1])+corners[i*2])inside=!inside;
            return inside;
        }
    }
    private static Identifier texture;
    private static boolean missing;
    private static List<Hit> hits=List.of();
    private MapNavalInvasionOverlay() {}
    static Identifier texture(){
        if(texture!=null||missing)return texture;
        var client=Minecraft.getInstance();
        try(var input=client.getResourceManager().open(Identifier.parse("hoi:textures/gui/map/invasion_mask_0.png"));var mask=NativeImage.read(input);
                var patternInput=client.getResourceManager().open(Identifier.parse("hoi:textures/gui/map/invasion_pattern_0.png"))){
            var pattern=NativeImage.read(patternInput);
            try{
                if(pattern.getWidth()!=mask.getWidth()||pattern.getHeight()!=mask.getHeight())throw new IOException("Invasion source texture dimensions differ");
                for(int y=0;y<pattern.getHeight();y++)for(int x=0;x<pattern.getWidth();x++){
                    int pixel=pattern.getPixel(x,y),m=mask.getPixel(x,y),alpha=(pixel>>>24)*(m>>>24)/255*Math.min(255,(m>>16&255)+(m>>8&255))/255;
                    pattern.setPixel(x,y,alpha<<24|pixel&0xffffff);
                }
                var dynamic=new DynamicTexture(()->"HOI original invasion mask and pattern",pattern);var id=Identifier.parse("hoi:map/invasion_masked");
                try{client.getTextureManager().register(id,dynamic);}catch(RuntimeException|Error failure){dynamic.close();throw failure;}
                texture=id;
            }catch(IOException|RuntimeException|Error failure){pattern.close();throw failure;}
        }catch(IOException failure){missing=true;}
        return texture;
    }
    static void clear(){if(texture!=null)Minecraft.getInstance().getTextureManager().release(texture);texture=null;missing=false;clearHits();}
    static void clearHits(){hits=List.of();}
    static List<Hit> hits(){return hits;}
    static int color(ProtocolNavalInvasion.Plan plan){
        var color=MapCommanderPanels.armyColor(plan.army());if(color==null)return 0xffffffff;
        float[] hsv=java.awt.Color.RGBtoHSB(color>>16&255,color>>8&255,color&255,null);
        return 0xff000000|java.awt.Color.HSBtoRGB(hsv[0],Math.min(1,hsv[1]*1.5f),hsv[2])&0xffffff;
    }
    static List<Mesh> extract(){
        var client=Minecraft.getInstance();var result=new ArrayList<Mesh>();
        if(client.level==null||client.gui.hud.isHidden()||MapAgencyHud.active())return List.of();
        for(var plan:MapNavalInvasionOrders.plans()){
            if(Set.of("COMPLETED","CANCELLED").contains(plan.phase()))continue;
            var area=new AABB(Math.min(plan.departure().x(),plan.target().x()),-64,Math.min(plan.departure().z(),plan.target().z()),Math.max(plan.departure().x(),plan.target().x()),512,Math.max(plan.departure().z(),plan.target().z()));
            if(area.distanceToSqr(client.gameRenderer.mainCamera().position())>dev.hoi.protocol.ProtocolAtlasVisibility.BOUNDARY_RANGE*dev.hoi.protocol.ProtocolAtlasVisibility.BOUNDARY_RANGE)continue;
            double dx=plan.target().x()-plan.departure().x(),dz=plan.target().z()-plan.departure().z(),length=Math.hypot(dx,dz);
            if(length<.0001)continue;
            double scale=Math.clamp(length*4/50,.2,1),width=8.5/4*scale,head=Math.min(length,9./4*scale),body=length-head;
            var vertices=new FloatArrayList();
            strip(vertices,plan,0,body/length,.001,.499,.295,.705,width,length);
            strip(vertices,plan,body/length,1,.501,.998,.001,.998,width,length);
            if(vertices.isEmpty())continue;
            var v=vertices.toFloatArray();var bounds=new AABB(v[0],v[1],v[2],v[0],v[1],v[2]);
            for(int i=8;i<v.length;i+=8)bounds=bounds.minmax(new AABB(v[i],v[i+1],v[i+2],v[i],v[i+1],v[i+2]));
            result.add(new Mesh(new MapSceneMesh.MapTile(Material.DEPLOYMENT,bounds.inflate(.01),v,List.of()),plan));
        }
        return List.copyOf(result);
    }
    private static void strip(FloatArrayList vertices,ProtocolNavalInvasion.Plan plan,double start,double end,double u0,double u1,double v0,double v1,double width,double length){
        int segments=Math.clamp((int)Math.ceil((end-start)*length),1,128);var level=Minecraft.getInstance().level;
        for(int i=0;i<segments;i++){
            double a=start+(end-start)*i/segments,b=start+(end-start)*(i+1)/segments;
            var corners=new double[4][3];boolean loaded=true;
            for(int corner=0;corner<4;corner++){
                double t=corner>=2?b:a,side=(corner==1||corner==2?1:-1)*width/2,dx=plan.target().x()-plan.departure().x(),dz=plan.target().z()-plan.departure().z();
                double x=plan.departure().x()+dx*t-dz/length*side,z=plan.departure().z()+dz*t+dx/length*side;
                int bx=(int)Math.floor(x),bz=(int)Math.floor(z);if(!level.hasChunk(bx>>4,bz>>4)){loaded=false;break;}
                corners[corner]=new double[]{x,level.getHeight(Heightmap.Types.WORLD_SURFACE,bx,bz)+.04,z};
            }
            if(!loaded)continue;
            for(int corner=0;corner<4;corner++){
                for(double coordinate:corners[corner])vertices.add((float)coordinate);
                vertices.add((float)(u0+(u1-u0)*(i+(corner>=2?1.:0))/segments));vertices.add((float)(corner==1||corner==2?v1:v0));vertices.add(0);vertices.add(1);vertices.add(0);
            }
        }
    }
    static void project(List<Mesh> meshes){
        var client=Minecraft.getInstance();var camera=client.gameRenderer.mainCamera();var projection=camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());var forward=camera.forwardVector();
        var result=new ArrayList<Hit>();int width=client.getWindow().getGuiScaledWidth(),height=client.getWindow().getGuiScaledHeight();
        for(var mesh:meshes){var vertices=mesh.tile().vertices();for(int offset=0;offset<vertices.length;offset+=32){
            var corners=new float[8];boolean visible=true;
            for(int corner=0;corner<4;corner++){
                int v=offset+corner*8;var relative=new Vec3(vertices[v],vertices[v+1],vertices[v+2]).subtract(camera.position());
                if(relative.x*forward.x()+relative.y*forward.y()+relative.z*forward.z()<=0){visible=false;break;}
                var point=projection.transformProject(relative.toVector3f());if(!point.isFinite()){visible=false;break;}
                corners[corner*2]=(point.x+1)*width/2;corners[corner*2+1]=(1-point.y)*height/2;
            }
            if(visible)result.add(new Hit(mesh.plan().id(),mesh.plan().detail(),corners));
        }}
        hits=List.copyOf(result);
    }
    static String tooltip(double x,double y){if(MapNavalInvasionOrders.plans().isEmpty())return "";return hits.reversed().stream().filter(hit->hit.contains(x,y)).map(Hit::detail).findFirst().orElse("");}
    public static boolean click(double x,double y,int button){
        if(!MapNavalInvasionOrders.targeting())return false;
        if(button==InputConstants.MOUSE_BUTTON_RIGHT){MapNavalInvasionOrders.cancel();return true;}
        if(button!=InputConstants.MOUSE_BUTTON_LEFT)return true;
        hits.reversed().stream().filter(hit->hit.contains(x,y)).findFirst().ifPresent(hit->{if(MapNavalInvasionOrders.execute(hit.id()))dev.hoi.client.audio.AudioUiSounds.play("ui.battleplan");});
        return true;
    }
}
