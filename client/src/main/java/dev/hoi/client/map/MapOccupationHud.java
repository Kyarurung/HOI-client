package dev.hoi.client.map;

import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.screen.occupation.OccupationScreen;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.occupation.OccupationPayloads;
import dev.hoi.protocol.occupation.OccupationPayloads.*;
import dev.hoi.client.map.scene.SceneRenderer;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
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
    record Label(int x,int y,int width,int height,String state,String value) {
        boolean overlaps(Label other) {return x<other.x+other.width+2&&x+width+2>other.x&&y<other.y+other.height+2&&y+height+2>other.y;}
    }
    private static List<Label> labels=List.of();
    static List<Label> labels() {return labels;}
    static String percentage(double fraction) {
        return java.math.BigDecimal.valueOf(fraction).movePointRight(2).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()+"%";
    }
    static void accept(MapView next) {view=next;age=0;hits=List.of();labels=List.of();if(next.rows().isEmpty())SceneRenderer.clearOccupation();}
    private static MapView view;
    private static List<Hit> hits=List.of();
    private static int age;
    private MapOccupationHud(){}
    public static void register(){
        OccupationPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(Response.TYPE,(p,c)->{if(c.client().gui.screen() instanceof OccupationScreen screen)screen.update(p.view());});
        ClientPlayNetworking.registerGlobalReceiver(MapResponse.TYPE,(p,c)->{accept(p.view());});
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->c.execute(MapOccupationHud::reset));
        ClientTickEvents.END_CLIENT_TICK.register(c->{if(++age>60&&view!=null)reset();});
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,Identifier.parse("hoi:occupation_hud"),(g,d)->{if(Minecraft.getInstance().gui.screen()==null){draw(g,0);tooltip(g,g.guiWidth()/2,g.guiHeight()/2);}});
        ScreenEvents.AFTER_INIT.register((c,s,w,h)->{if(s instanceof InputSidebarMovement.InputMovementScreen){
            ScreenEvents.afterBackground(s).register((a,g,mx,my,d)->draw(g,left()));
            ScreenEvents.afterExtract(s).register((a,g,mx,my,d)->{if(mx>=left())tooltip(g,mx,my);});
        }});
    }
    public static void open(){var screen=new OccupationScreen();Minecraft.getInstance().gui.setScreen(screen);screen.open();}
    private static int left(){return Minecraft.getInstance().gui.screen() instanceof InputSidebarMovement.InputMovementScreen s?s.unitHudLeft():Integer.MAX_VALUE;}
    static void reset(){SceneRenderer.clearOccupation();view=null;hits=List.of();labels=List.of();age=0;}
    private static void draw(GuiGraphicsExtractor g,int left){
        hits=List.of();labels=List.of();var c=Minecraft.getInstance();
        if(view==null||c.level==null||c.gui.hud.isHidden()||!view.dimension().equals(c.level.dimension().identifier().toString()))return;
        var camera=c.gameRenderer.mainCamera();var matrix=camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());var forward=camera.forwardVector();
        var result=new ArrayList<Hit>();var placed=new ArrayList<Label>();int top=HudMenuBar.height(g.guiWidth());
        for(var row:view.rows()){
            var relative=new Vec3(row.x(),row.y()+3.1,row.z()).subtract(camera.position());
            if(relative.x*forward.x()+relative.y*forward.y()+relative.z*forward.z()<=0)continue;
            var p=matrix.transformProject(relative.toVector3f());if(!p.isFinite()||Math.abs(p.x)>1||Math.abs(p.y)>1)continue;
            int x=Math.round((p.x+1)*g.guiWidth()/2),y=Math.round((1-p.y)*g.guiHeight()/2);
            if(x<left+20||y<top+22||x>g.guiWidth()-20||y>g.guiHeight()-14)continue;
            var effects=(view.compliance()?row.complianceEffects():row.resistanceEffects()).stream().filter(Effect::active).toList();
            String value=percentage(view.compliance()?row.compliance():row.resistance());
            int numberWidth=Math.max(36,c.font.width(value)+6),effectWidth=effects.size()*18;
            int boxWidth=Math.max(numberWidth,effectWidth),boxHeight=effects.isEmpty()?14:35;
            Label position=null;
            for(int offset:new int[]{0,16,-16,32,-32,48,-48,64,-64}) {
                int py=y+offset;
                var candidate=new Label(x-boxWidth/2,py-(effects.isEmpty()?3:21),boxWidth,boxHeight,row.state(),value);
                if(candidate.x()<left+2||candidate.x()+candidate.width()>g.guiWidth()-2
                        ||candidate.y()<top+2||candidate.y()+candidate.height()>g.guiHeight()-2)continue;
                if(placed.stream().noneMatch(candidate::overlaps)){position=candidate;y=py;break;}
            }
            if(position==null)continue;
            placed.add(position);
            int start=x-effects.size()*9;
            for(int i=0;i<effects.size();i++){var e=effects.get(i);int ex=start+i*18;UiAssets.drawContent(g,e.icon(),ex,y-21,17,17);result.add(new Hit(ex,y-21,17,17,e.tooltip()));}
            UiText.centered(g,c.font,value,x-numberWidth/2,y-2,numberWidth,12,view.compliance()?OccupationPayloads.COMPLIANCE_COLOR:OccupationPayloads.RESISTANCE_COLOR);
            result.add(new Hit(x-numberWidth/2,y-3,numberWidth,14,"§e"+row.name()+"§r\n\n"+(view.compliance()?row.complianceTooltip():row.resistanceTooltip())));
        }
        hits=List.copyOf(result);labels=List.copyOf(placed);
    }
    private static void tooltip(GuiGraphicsExtractor g,int x,int y){for(var hit:hits.reversed())if(hit.contains(x,y)){Tooltips.drawExact(g,Minecraft.getInstance().font,hit.tooltip(),x,y);return;}}
}
