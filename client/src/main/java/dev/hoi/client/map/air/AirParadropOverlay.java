package dev.hoi.client.map.air;

import dev.hoi.protocol.map.MapAtlasScene.Material;
import dev.hoi.protocol.commander.CommanderParadrop;
import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.map.commander.CommanderOrders;
import dev.hoi.client.map.commander.CommanderPanels;
import dev.hoi.client.map.scene.SceneMesh;
import dev.hoi.client.ui.tooltip.Tooltips;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AirParadropOverlay {
    public record Mesh(SceneMesh.SceneTile tile,CommanderParadrop.Plan plan) {}
    record Hit(String tooltip,float[] corners) {
        boolean contains(double x,double y){
            boolean inside=false;int count=corners.length/2;
            for(int i=0,j=count-1;i<count;j=i++)if((corners[i*2+1]>y)!=(corners[j*2+1]>y)&&x<(corners[j*2]-corners[i*2])*(y-corners[i*2+1])/(corners[j*2+1]-corners[i*2+1])+corners[i*2])inside=!inside;
            return inside;
        }
    }
    record Label(String text,float x,float y,float angle,float scale) {}
    private static CommanderParadrop.View view;
    static List<Hit> hits=List.of();
    static List<Label> labels=List.of();
    private AirParadropOverlay() {}
    public static void register(){ClientPlayNetworking.registerGlobalReceiver(CommanderParadrop.Snapshot.TYPE,(packet,context)->{try{accept(packet.view());}catch(IllegalArgumentException ignored){}});}
    static void accept(CommanderParadrop.View next){
        var context=CommanderOrders.currentContext();
        if(!CommanderOrders.valid()||!next.token().equals(context.token())||next.revision()!=context.revision()||!next.country().equals(context.country())||!next.dimension().equals(context.dimension()))return;
        view=next;
    }
    static List<CommanderParadrop.Plan> plans(){
        var context=CommanderOrders.currentContext();
        if(view==null||!CommanderOrders.valid()||!view.token().equals(context.token())||view.revision()!=context.revision()||!CommanderPanels.armyInput()||AgencyHud.active())return List.of();
        return view.plans();
    }
    public static void reset(){view=null;hits=List.of();labels=List.of();}
    public static int color(CommanderParadrop.Plan plan){
        var color=CommanderPanels.armyColor(plan.army());if(color==null)return 0xffffffff;
        float[] hsv=java.awt.Color.RGBtoHSB(color>>16&255,color>>8&255,color&255,null);
        return 0xff000000|java.awt.Color.HSBtoRGB(hsv[0],Math.min(1,hsv[1]*1.5f),hsv[2])&0xffffff;
    }
    static double rise(double length,double t){return Math.min(3,length*.12)*4*t*(1-t);}
    public static List<Mesh> extract(){
        var client=Minecraft.getInstance();var result=new ArrayList<Mesh>();
        if(client.level==null||client.gui.hud.isHidden()||AgencyHud.active())return List.of();
        for(var plan:plans()){
            var area=new AABB(Math.min(plan.source().x(),plan.target().x()),-64,Math.min(plan.source().z(),plan.target().z()),Math.max(plan.source().x(),plan.target().x()),512,Math.max(plan.source().z(),plan.target().z()));
            if(area.distanceToSqr(client.gameRenderer.mainCamera().position())>dev.hoi.protocol.map.MapAtlasVisibility.BOUNDARY_RANGE*dev.hoi.protocol.map.MapAtlasVisibility.BOUNDARY_RANGE)continue;
            double dx=plan.target().x()-plan.source().x(),dz=plan.target().z()-plan.source().z(),length=Math.hypot(dx,dz);
            if(length<.0001)continue;
            double scale=Math.clamp(length*4/50,.2,1),width=8.5/4*scale,head=Math.min(length,9./4*scale),body=length-head;
            var vertices=new FloatArrayList();
            if(!strip(vertices,plan,0,body/length,.001,.499,.295,.705,width,length)||!strip(vertices,plan,body/length,1,.501,.998,.001,.998,width,length))continue;
            var v=vertices.toFloatArray();var bounds=new AABB(v[0],v[1],v[2],v[0],v[1],v[2]);
            for(int i=8;i<v.length;i+=8)bounds=bounds.minmax(new AABB(v[i],v[i+1],v[i+2],v[i],v[i+1],v[i+2]));
            result.add(new Mesh(new SceneMesh.SceneTile(Material.DEPLOYMENT,bounds.inflate(.01),v,List.of()),plan));
        }
        return List.copyOf(result);
    }
    private static double ground(CommanderParadrop.Point point){var level=Minecraft.getInstance().level;int x=(int)Math.floor(point.x()),z=(int)Math.floor(point.z());return level.hasChunk(x>>4,z>>4)?level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)+.04:Double.NaN;}
    private static boolean strip(FloatArrayList vertices,CommanderParadrop.Plan plan,double start,double end,double u0,double u1,double v0,double v1,double width,double length){
        double from=ground(plan.source()),to=ground(plan.target());if(Double.isNaN(from)||Double.isNaN(to))return false;
        int segments=Math.clamp((int)Math.ceil((end-start)*length),1,128);double dx=plan.target().x()-plan.source().x(),dz=plan.target().z()-plan.source().z();
        for(int i=0;i<segments;i++){
            double a=start+(end-start)*i/segments,b=start+(end-start)*(i+1)/segments;
            for(int corner=0;corner<4;corner++){
                double t=corner>=2?b:a,side=(corner==1||corner==2?1:-1)*width/2;
                vertices.add((float)(plan.source().x()+dx*t-dz/length*side));vertices.add((float)(from+(to-from)*t+rise(length,t)));vertices.add((float)(plan.source().z()+dz*t+dx/length*side));
                vertices.add((float)(u0+(u1-u0)*(i+(corner>=2?1.:0))/segments));vertices.add((float)(corner==1||corner==2?v1:v0));vertices.add(0);vertices.add(1);vertices.add(0);
            }
        }
        return true;
    }
    public static void project(List<Mesh> meshes){
        var client=Minecraft.getInstance();var camera=client.gameRenderer.mainCamera();var projection=camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());var forward=camera.forwardVector();
        var nextHits=new ArrayList<Hit>();var nextLabels=new ArrayList<Label>();int width=client.getWindow().getGuiScaledWidth(),height=client.getWindow().getGuiScaledHeight();
        for(var mesh:meshes){
            String tooltip=mesh.plan().detail().isEmpty()?mesh.plan().label():mesh.plan().label()+"\n"+mesh.plan().detail();
            var vertices=mesh.tile().vertices();
            for(int offset=0;offset<vertices.length;offset+=32){
                var corners=new float[8];boolean visible=true;
                for(int corner=0;corner<4;corner++){
                    var point=screen(projection,camera.position(),forward,vertices[offset+corner*8],vertices[offset+corner*8+1],vertices[offset+corner*8+2],width,height);
                    if(point==null){visible=false;break;}corners[corner*2]=point[0];corners[corner*2+1]=point[1];
                }
                if(visible)nextHits.add(new Hit(tooltip,corners));
            }
            var plan=mesh.plan();double from=ground(plan.source()),to=ground(plan.target()),length=Math.hypot(plan.target().x()-plan.source().x(),plan.target().z()-plan.source().z());
            if(Double.isNaN(from)||Double.isNaN(to))continue;
            float[] a=at(projection,camera.position(),forward,plan,.42,from,to,length,width,height),b=at(projection,camera.position(),forward,plan,.58,from,to,length,width,height);
            if(a==null||b==null)continue;
            float angle=(float)Math.atan2(b[1]-a[1],b[0]-a[0]);if(angle>Math.PI/2)angle-=(float)Math.PI;else if(angle<-Math.PI/2)angle+=(float)Math.PI;
            nextLabels.add(new Label(plan.label(),(a[0]+b[0])/2,(a[1]+b[1])/2,angle,dev.hoi.client.ui.UiText.scale(client.font)));
        }
        hits=List.copyOf(nextHits);labels=List.copyOf(nextLabels);
    }
    private static float[] at(org.joml.Matrix4f projection,Vec3 camera,org.joml.Vector3fc forward,CommanderParadrop.Plan plan,double t,double from,double to,double length,int width,int height){
        double x=plan.source().x()+(plan.target().x()-plan.source().x())*t,z=plan.source().z()+(plan.target().z()-plan.source().z())*t,y=from+(to-from)*t+rise(length,t);
        return screen(projection,camera,forward,(float)x,(float)y,(float)z,width,height);
    }
    private static float[] screen(org.joml.Matrix4f projection,Vec3 camera,org.joml.Vector3fc forward,float x,float y,float z,int width,int height){
        var relative=new Vec3(x,y,z).subtract(camera);
        if(relative.x*forward.x()+relative.y*forward.y()+relative.z*forward.z()<=0)return null;
        var point=projection.transformProject(relative.toVector3f());if(!point.isFinite())return null;
        return new float[]{(point.x+1)*width/2,(1-point.y)*height/2};
    }
    public static String tooltip(double x,double y){if(plans().isEmpty())return "";return hits.reversed().stream().filter(hit->hit.contains(x,y)).map(Hit::tooltip).findFirst().orElse("");}
    public static void labels(GuiGraphicsExtractor g){
        if(plans().isEmpty())return;var font=Minecraft.getInstance().font;
        for(var label:labels){
            var text=Tooltips.officerText(label.text());int textWidth=font.width(text);
            g.pose().pushMatrix();g.pose().translate(label.x(),label.y());g.pose().rotate(label.angle());g.pose().scale(label.scale());
            g.text(font,text,-textWidth/2,-font.lineHeight/2,0xffffffff,true);g.pose().popMatrix();
        }
    }
}
