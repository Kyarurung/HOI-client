package dev.hoi.client.map;

import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.UiUnits;
import dev.hoi.protocol.UnitHudProtocol;
import dev.hoi.protocol.UnitHudProtocol.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public final class MapUnitHud {
    private static int width(Counter counter){return counter.navy()?37:33;}
    private static int height(Counter counter){return counter.navy()?18:14;}
    private static List<List<Counter>> groups=List.of();
    private static String dimension="";
    private static int age;
    private static final Set<String> selected=new LinkedHashSet<>();
    private static List<MapHudBox> boxes=List.of();
    record MapHudBox(Counter counter,int x,int y,int width,int height,float scale) {
        boolean contains(double mx,double my){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}
    }
    private MapUnitHud() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE,(packet,context)->accept(packet));
        ClientTickEvents.END_CLIENT_TICK.register(client->tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset());
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,Identifier.parse("hoi:unit_hud"),(g,delta)-> {
            if(Minecraft.getInstance().gui.screen()==null) {
                draw(g,0);tooltip(g,g.guiWidth()/2,g.guiHeight()/2);
            }
        });
        ScreenEvents.AFTER_INIT.register((client,screen,w,h)-> {
            if(!(screen instanceof InputSidebarMovement.InputMovementScreen))return;
            ScreenEvents.afterBackground(screen).register((s,g,mx,my,delta)->draw(g,worldLeft()));
            ScreenEvents.afterExtract(screen).register((s,g,mx,my,delta)-> {
                if(mx>=worldLeft())tooltip(g,mx,my);
            });
        });
    }
    private static int worldLeft() {
        var screen=Minecraft.getInstance().gui.screen();
        return screen instanceof InputSidebarMovement.InputMovementScreen sidebar&&sidebar.allowsMovement()?sidebar.unitHudLeft():Integer.MAX_VALUE;
    }
    public static void accept(Snapshot snapshot) {
        var previous=dimension.equals(snapshot.dimension())?Set.copyOf(selected):Set.<String>of();
        reset();
        var client=Minecraft.getInstance();
        if(client.level==null||!snapshot.dimension().equals(client.level.dimension().identifier().toString()))return;
        snapshot.counters().stream().filter(c->previous.contains(c.key())&&selectable(c,snapshot.dimension())).forEach(c->selected.add(c.key()));
        dimension=snapshot.dimension();
        var grouped=new LinkedHashMap<String,List<Counter>>();
        for(var counter:snapshot.counters())grouped.computeIfAbsent(counter.country()+"|"+counter.x()+"|"+counter.y()+"|"+counter.z(),key->new ArrayList<>()).add(counter);
        groups=grouped.values().stream().map(List::copyOf).toList();
    }
    public static void reset(){groups=List.of();boxes=List.of();dimension="";age=0;selected.clear();}
    public static boolean selectAt(double x,double y) {
        var client=Minecraft.getInstance();
        if(client.level==null||client.gui.hud.isHidden()||!dimension.equals(client.level.dimension().identifier().toString()))return false;
        if(client.gui.screen()!=null&&x<worldLeft())return false;
        selected.clear();
        for(int i=boxes.size()-1;i>=0;i--) {
            var box=boxes.get(i);
            if(box.contains(x,y)&&selectable(box.counter(),dimension)){selected.add(box.counter().key());return true;}
        }
        return false;
    }
    public static void selectArea(double startX,double startY,double endX,double endY) {
        var client=Minecraft.getInstance();
        selected.clear();
        if(client.level==null||client.gui.hud.isHidden()||!dimension.equals(client.level.dimension().identifier().toString()))return;
        double left=Math.max(Math.min(startX,endX),client.gui.screen()==null?0:worldLeft());
        double top=Math.max(Math.min(startY,endY),UiMenuBar.height(client.getWindow().getGuiScaledWidth()));
        double right=Math.max(startX,endX),bottom=Math.max(startY,endY);
        for(var box:boxes)if(selectable(box.counter(),dimension)&&box.x()<right&&box.x()+box.width()>left
                &&box.y()<bottom&&box.y()+box.height()>top)selected.add(box.counter().key());
    }
    static boolean selectable(Counter counter,String dimension) {
        if(dimension.startsWith("hoi:atlas_army_"))return !counter.navy()&&!counter.air();
        if(dimension.startsWith("hoi:atlas_navy_"))return counter.navy();
        if(dimension.startsWith("hoi:atlas_air_"))return counter.air();
        return !dimension.startsWith("hoi:atlas_");
    }
    static String selected(){return selected.stream().findFirst().orElse("");}
    static Set<String> selectedKeys(){return Set.copyOf(selected);}
    static List<MapHudBox> boxes(){return boxes;}
    static void tick() {
        var client=Minecraft.getInstance();
        if(client.level==null||!dimension.equals(client.level.dimension().identifier().toString())||++age>=40)reset();
    }
    private static void draw(GuiGraphicsExtractor g,int left) {
        boxes=List.of();
        var client=Minecraft.getInstance();
        if(groups.isEmpty()||client.level==null||client.player==null||client.gui.hud.isHidden()||left>=g.guiWidth()
                ||!dimension.equals(client.level.dimension().identifier().toString()))return;
        var camera=client.gameRenderer.mainCamera();
        var matrix=camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());
        var forward=camera.forwardVector();
        var result=new ArrayList<MapHudBox>();
        var units=new UiUnits(g.guiWidth(),g.guiHeight());
        int top=UiMenuBar.height(g.guiWidth())+4;
        g.enableScissor(left,top,g.guiWidth(),g.guiHeight());
        for(var rows:groups) {
            var c=rows.getFirst();
            if(c.distanceSquared(client.player.getX(),client.player.getY(),client.player.getZ())>UnitHudProtocol.RANGE*UnitHudProtocol.RANGE)continue;
            var relative=new Vec3(c.x(),c.y()+3.1,c.z()).subtract(camera.position());
            if(relative.x*forward.x()+relative.y*forward.y()+relative.z*forward.z()<=0)continue;
            var projected=matrix.transformProject(relative.toVector3f());
            if(!projected.isFinite()||Math.abs(projected.x)>1||Math.abs(projected.y)>1)continue;
            int centerX=Math.round((projected.x+1)*g.guiWidth()/2);
            int y=Math.round((1-projected.y)*g.guiHeight()/2)-rows.stream().mapToInt(row->units.size(height(row))).sum()/2;
            for(var row:rows) {
                int rowWidth=units.size(width(row)),rowHeight=units.size(height(row));
                int x=centerX-rowWidth/2;
                if(x>=left&&x+rowWidth<=g.guiWidth()&&y>=top&&y+rowHeight<=g.guiHeight()) {
                    var box=new MapHudBox(row,x,y,rowWidth,rowHeight,units.scale());result.add(box);counter(g,box);
                }
                y+=rowHeight;
            }
        }
        g.disableScissor();boxes=List.copyOf(result);
    }
    private static void counter(GuiGraphicsExtractor g,MapHudBox box) {
        var c=box.counter();var font=Minecraft.getInstance().font;
        int w=c.navy()?74:66,h=c.navy()?36:27;
        g.pose().pushMatrix();g.pose().translate(box.x(),box.y());g.pose().scale(.5f*box.scale());
        var background=Identifier.parse("hoi:textures/gui/unit_hud/"+(c.navy()?"navy":"army")+"_background.png");
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,background,0,0,0,0,w,h,w,h,tint(c.allegiance()));
        UiAssets.draw(g,"unit_hud/"+(c.navy()?"navy":"army")+"_overlay",3,3,30,c.navy()?29:20);
        UiAssets.drawTinted(g,"unit_hud/"+c.type(),3,3,30,12,c.navy()||c.air()?0xFFFFFFFF:tint(c.allegiance()));
        int flagY=c.navy()?22:15;
        int barX=c.navy()||c.air()?14:15;
        if(!c.air())bar(g,"organization",barX,flagY,c.organization());
        bar(g,"strength",barX,flagY+4,c.strength());
        if(!UiAssets.draw(g,"unit_hud/flags/"+c.country().toLowerCase(Locale.ROOT),c.navy()||c.air()?4:5,flagY,10,7))
            UiAssets.draw(g,UiMenuBar.flagTexture(c.country()),c.navy()||c.air()?4:5,flagY,10,7);
        if(c.navy()) {
            UiText.centered(g,font,Integer.toString(c.capitals()),32,5,34,14,0xFFFFFFFF);
            UiText.centered(g,font,count(c.count()-c.capitals()),32,20,34,14,0xFFFFFFFF);
            if(c.capitals()>0)UiAssets.draw(g,"unit_hud/capital",59,7,7,7);
        } else UiText.centered(g,font,c.unknown()?"?":count(c.count()),c.air()?21:36,c.air()?8:7,c.air()?50:20,14,0xFFFFFFFF);
        if(c.status()==Status.TRAINING)UiAssets.draw(g,"unit_hud/"+(c.air()?"air_training":"training"),c.air()?-14:c.navy()?63:54,c.air()?7:8,c.air()?14:12,c.air()?14:12);
        if(c.air()&&c.status()==Status.IDLE)UiAssets.draw(g,"unit_hud/no_orders",-10,8,12,14);
        if(selected.contains(c.key()))UiAssets.draw(g,"unit_hud/"+(c.navy()?"navy":"army")+"_selected",0,0,c.navy()?74:68,c.navy()?36:28);
        g.pose().popMatrix();
    }
    private static int tint(Allegiance allegiance) {
        return switch(allegiance) {
            case OURS -> 0xFF52B563;
            case ALLIED -> 0xFF4FA6F0;
            case ENEMY -> 0xFFE84D4D;
            case OTHER -> 0xFFCCCCCC;
        };
    }
    private static void bar(GuiGraphicsExtractor g,String name,int x,int y,double ratio) {
        g.fill(x,y,x+17,y+3,0xFF172319);
        g.pose().pushMatrix();g.pose().translate(x,y+3);g.pose().rotate(-(float)Math.PI/2);
        g.blit(Identifier.parse("hoi:textures/gui/unit_hud/"+name+".png"),0,0,3,(int)Math.round(17*ratio),0,1,0,(float)ratio);
        g.pose().popMatrix();
    }
    private static String count(int value) {
        return value<1000?Integer.toString(value):decimal(value/(value<1_000_000?1000.0:1_000_000.0))+(value<1_000_000?"K":"M");
    }
    private static String decimal(double value){return BigDecimal.valueOf(value).setScale(1,RoundingMode.DOWN).stripTrailingZeros().toPlainString();}
    private static void tooltip(GuiGraphicsExtractor g,int mx,int my) {
        if(Minecraft.getInstance().gui.hud.isHidden())return;
        Counter hovered=null;
        for(var box:boxes)if(box.contains(mx,my))hovered=box.counter();
        if(hovered==null||hovered.navy()||hovered.air())return;
        var c=hovered;var font=Minecraft.getInstance().font;
        int w=Math.min(286,g.guiWidth()-12);
        var title=font.split(Component.literal(c.countryName()+" · "+(c.unknown()?"?":c.name()+" "+c.count()+"개")),Math.max(1,(int)((w-12)/UiText.scale(font))));
        int cellWidth=c.unknown()?80:44;
        int titleCount=Math.min(3,title.size()),columns=Math.max(1,(w-12)/cellWidth);
        int h=12+titleCount*12+40+((c.battalions().size()+columns-1)/columns)*16;
        int x=Math.clamp(mx+12,6,Math.max(6,g.guiWidth()-w-6)),y=Math.clamp(my+12,6,Math.max(6,g.guiHeight()-h-6));
        g.nextStratum();dev.hoi.client.ui.UiTooltips.background(g,x,y,w,h);
        for(int i=0;i<titleCount;i++)UiText.text(g,font,title.get(i),x+6,y+6+i*12,0xFFFFFFFF);
        int line=y+8+titleCount*12;
        String[] labels={"평균 조직력","평균 내구도","보급 상황"};
        double[] ratios={c.organization(),c.strength(),c.supply()};
        for(int i=0;i<labels.length;i++)UiText.text(g,font,Component.literal(labels[i]+": ").append(Component.literal(c.unknown()&&i==2?"?":decimal(ratios[i]*100)+"%").withColor(dev.hoi.client.ui.UiTooltips.GOLD)),x+6,line+i*12,0xFFFFFFFF);
        int i=0;
        for(var entry:new TreeMap<>(c.battalions()).entrySet()) {
            int bx=x+6+i%columns*cellWidth,by=line+40+i/columns*16;
            UiAssets.drawTinted(g,"unit_hud/"+entry.getKey(),bx,by,16,12,0xFFFFFFFF);
            UiText.text(g,font,(c.unknown()?entry.getValue()+"–"+c.battalionMaximums().get(entry.getKey()):Integer.toString(entry.getValue())),bx+18,by+1,0xFFFFFFFF);i++;
        }
    }
}
