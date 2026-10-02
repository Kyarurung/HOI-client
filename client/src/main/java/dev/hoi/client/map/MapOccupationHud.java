package dev.hoi.client.map;

import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.screen.ScreenOccupation;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.ProtocolOccupation;
import dev.hoi.protocol.ProtocolOccupation.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class MapOccupationHud {
    private record Hit(int x,int y,int w,int h,String tooltip) {boolean contains(int a,int b){return a>=x&&a<x+w&&b>=y&&b<y+h;}}
    private static MapView view;
    private static List<Hit> hits=List.of();
    private static int age;
    private MapOccupationHud(){}
    public static void register(){
        ProtocolOccupation.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(Response.TYPE,(p,c)->{if(c.client().gui.screen() instanceof ScreenOccupation screen)screen.update(p.view());});
        ClientPlayNetworking.registerGlobalReceiver(MapResponse.TYPE,(p,c)->{view=p.view();age=0;if(view.rows().isEmpty())MapSceneRenderer.clearOccupation();});
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->c.execute(MapOccupationHud::reset));
        ClientTickEvents.END_CLIENT_TICK.register(c->{if(++age>60&&view!=null)reset();});
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,Identifier.parse("hoi:occupation_hud"),(g,d)->{if(Minecraft.getInstance().gui.screen()==null){draw(g,0);tooltip(g,g.guiWidth()/2,g.guiHeight()/2);}});
        ScreenEvents.AFTER_INIT.register((c,s,w,h)->{if(s instanceof InputSidebarMovement.InputMovementScreen){
            ScreenEvents.afterBackground(s).register((a,g,mx,my,d)->draw(g,left()));
            ScreenEvents.afterExtract(s).register((a,g,mx,my,d)->{if(mx>=left())tooltip(g,mx,my);});
        }});
    }
    public static void open(){var screen=new ScreenOccupation();Minecraft.getInstance().gui.setScreen(screen);screen.open();}
    private static int left(){return Minecraft.getInstance().gui.screen() instanceof InputSidebarMovement.InputMovementScreen s?s.unitHudLeft():Integer.MAX_VALUE;}
    private static void reset(){MapSceneRenderer.clearOccupation();view=null;hits=List.of();age=0;}
    private static void draw(GuiGraphicsExtractor g,int left){
        hits=List.of();var c=Minecraft.getInstance();
        if(view==null||c.level==null||c.gui.hud.isHidden()||!view.dimension().equals(c.level.dimension().identifier().toString()))return;
        var camera=c.gameRenderer.mainCamera();var matrix=camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());var forward=camera.forwardVector();
        var result=new ArrayList<Hit>();int top=UiMenuBar.height(g.guiWidth());
        for(var row:view.rows()){
            var relative=new Vec3(row.x(),row.y()+3.1,row.z()).subtract(camera.position());
            if(relative.x*forward.x()+relative.y*forward.y()+relative.z*forward.z()<=0)continue;
            var p=matrix.transformProject(relative.toVector3f());if(!p.isFinite()||Math.abs(p.x)>1||Math.abs(p.y)>1)continue;
            int x=Math.round((p.x+1)*g.guiWidth()/2),y=Math.round((1-p.y)*g.guiHeight()/2);
            if(x<left+20||y<top+22||x>g.guiWidth()-20||y>g.guiHeight()-14)continue;
            var effects=(view.compliance()?row.complianceEffects():row.resistanceEffects()).stream().filter(Effect::active).toList();
            int start=x-effects.size()*9;
            for(int i=0;i<effects.size();i++){var e=effects.get(i);int ex=start+i*18;UiAssets.draw(g,e.icon(),ex,y-21,17,17);result.add(new Hit(ex,y-21,17,17,e.tooltip()));}
            String value=String.format(Locale.ROOT,"%.0f%%",(view.compliance()?row.compliance():row.resistance())*100);
            UiText.centered(g,c.font,value,x-18,y-2,36,12,view.compliance()?ProtocolOccupation.COMPLIANCE_COLOR:ProtocolOccupation.RESISTANCE_COLOR);
            result.add(new Hit(x-21,y-3,42,14,"§e"+row.name()+"§r\n\n"+(view.compliance()?row.complianceTooltip():row.resistanceTooltip())));
        }
        hits=List.copyOf(result);
    }
    private static void tooltip(GuiGraphicsExtractor g,int x,int y){for(var hit:hits.reversed())if(hit.contains(x,y)){UiTooltips.drawExact(g,Minecraft.getInstance().font,hit.tooltip(),x,y);return;}}
}
