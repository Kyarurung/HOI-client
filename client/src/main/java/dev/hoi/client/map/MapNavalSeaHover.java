package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

final class MapNavalSeaHover {
    static Consumer<SeaHover> queryObserver;
    private static Ray ray;
    private static Vec3 origin;
    private static String scope="",text="";
    private static List<String> selection=List.of();
    private static long sequence,awaiting=-1;
    private static int delay;
    private MapNavalSeaHover() {}
    static void register(){ClientPlayNetworking.registerGlobalReceiver(SeaHoverTooltip.TYPE,(packet,context)->accept(packet));}
    static void reset(){ray=null;origin=null;scope="";text="";selection=List.of();awaiting=-1;delay=0;}
    static void update(View view,int x,int y,int width,int height,boolean available){
        var selected=MapCommanderPanels.commandSelection();boolean air=!selected.isEmpty()&&selected.stream().allMatch(id->id.startsWith("air/"));
        var client=Minecraft.getInstance();if(!available||view==null||!(view.branch()==Mode.NAVY_HQ||air&&(view.branch()==Mode.AIR||view.branch()==Mode.ARMY))||client.level==null||!client.options.getCameraType().isFirstPerson()||!view.country().equals(ClientCampaignHud.country())||!view.dimension().equals(client.level.dimension().identifier().toString())){reset();return;}
        var matrix=client.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();var vector=matrix.transformProject(new org.joml.Vector3f((float)(x/(double)width*2-1),(float)(1-y/(double)height*2),1)).normalize();
        try{point(new Ray(vector.x,vector.y,vector.z),client.gameRenderer.mainCamera().position(),view.country()+"/"+view.dimension()+"/"+view.branch());}catch(IllegalArgumentException sky){reset();}
    }
    static void point(Ray next,Vec3 position,String nextScope){
        var selected=MapCommanderPanels.commandSelection();if(!next.equals(ray)||!position.equals(origin)||!nextScope.equals(scope)||!selected.equals(selection)){text="";awaiting=-1;delay=Math.min(delay,2);}
        ray=next;origin=position;scope=nextScope;selection=selected;
    }
    static void tick(){
        if(!MapCommanderHud.cardsVisible()||!selection.equals(MapCommanderPanels.commandSelection())){reset();return;}if(ray==null||--delay>0)return;
        if(queryObserver==null&&!ClientPlayNetworking.canSend(SeaHover.TYPE)){reset();return;}
        var packet=new SeaHover(++sequence,ray);awaiting=packet.sequence();delay=3;
        if(queryObserver!=null)queryObserver.accept(packet);else ClientPlayNetworking.send(packet);
    }
    static void accept(SeaHoverTooltip packet){if(ray==null||packet.sequence()!=awaiting||!MapCommanderHud.cardsVisible()||!selection.equals(MapCommanderPanels.commandSelection()))return;boolean air=!selection.isEmpty()&&selection.stream().allMatch(id->id.startsWith("air/"));boolean airTarget=packet.region().startsWith("AIR_BASE/")||packet.region().startsWith("AIR_REGION/");text=packet.region().isEmpty()||air!=airTarget?"":packet.text();delay=20;}
    static String text(){return selection.equals(MapCommanderPanels.commandSelection())?text:"";}
}
