package dev.hoi.client.map;

import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiText;
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

public final class UnitHud {
    private static final int WIDTH=52, HEIGHT=16;
    private static List<List<Counter>> groups=List.of();
    private static String dimension="";
    private static int age;
    private static List<Box> boxes=List.of();
    record Box(Counter counter,int x,int y) {
        boolean contains(int mx,int my){return mx>=x&&mx<x+WIDTH&&my>=y&&my<y+HEIGHT;}
    }
    private UnitHud() {}
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
            if(!(screen instanceof SidebarMovement.Screen))return;
            ScreenEvents.afterBackground(screen).register((s,g,mx,my,delta)->draw(g,worldLeft()));
            ScreenEvents.afterExtract(screen).register((s,g,mx,my,delta)-> {
                if(mx>=worldLeft())tooltip(g,mx,my);
            });
        });
    }
    private static int worldLeft() {
        var screen=Minecraft.getInstance().gui.screen();
        return screen instanceof SidebarMovement.Screen sidebar&&sidebar.allowsMovement()?sidebar.unitHudLeft():Integer.MAX_VALUE;
    }
    public static void accept(Snapshot snapshot) {
        reset();
        var client=Minecraft.getInstance();
        if(client.level==null||!snapshot.dimension().equals(client.level.dimension().identifier().toString()))return;
        dimension=snapshot.dimension();
        var grouped=new LinkedHashMap<String,List<Counter>>();
        for(var counter:snapshot.counters())grouped.computeIfAbsent(counter.stack(),key->new ArrayList<>()).add(counter);
        groups=grouped.values().stream().map(List::copyOf).toList();
    }
    public static void reset(){groups=List.of();boxes=List.of();dimension="";age=0;}
    static List<Box> boxes(){return boxes;}
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
        var result=new ArrayList<Box>();
        int top=HoiMenuBar.height(g.guiWidth())+4;
        g.enableScissor(left,top,g.guiWidth(),g.guiHeight());
        for(var rows:groups) {
            var c=rows.getFirst();
            if(c.distanceSquared(client.player.getX(),client.player.getY(),client.player.getZ())>UnitHudProtocol.RANGE*UnitHudProtocol.RANGE)continue;
            var relative=new Vec3(c.x(),c.y()+3.1,c.z()).subtract(camera.position());
            if(relative.x*forward.x()+relative.y*forward.y()+relative.z*forward.z()<=0)continue;
            var projected=matrix.transformProject(relative.toVector3f());
            if(!projected.isFinite()||Math.abs(projected.x)>1||Math.abs(projected.y)>1)continue;
            int x=Math.round((projected.x+1)*g.guiWidth()/2)-WIDTH/2;
            int y=Math.round((1-projected.y)*g.guiHeight()/2)-(rows.size()-1)*(HEIGHT+1)/2-HEIGHT/2;
            for(var row:rows) {
                if(x>=left&&x+WIDTH<=g.guiWidth()&&y>=top&&y+HEIGHT<=g.guiHeight()) {
                    var box=new Box(row,x,y);result.add(box);counter(g,box);
                }
                y+=HEIGHT+1;
            }
        }
        g.disableScissor();boxes=List.copyOf(result);
    }
    private static void counter(GuiGraphicsExtractor g,Box box) {
        var c=box.counter();int x=box.x(),y=box.y();
        g.fill(x,y,x+WIDTH,y+HEIGHT,0xED14242C);g.outline(x,y,WIDTH,HEIGHT,0xFF546871);
        g.fill(x+32,y+2,x+WIDTH-2,y+HEIGHT-2,0xEE080C0F);
        UiAssets.draw(g,HoiMenuBar.flagTexture(c.country()),x+2,y+1,14,10);
        UiAssets.draw(g,"production/units/"+c.type(),x+18,y+1,12,10);
        g.fill(x+2,y+11,x+30,y+13,0xFF131910);g.fill(x+2,y+11,x+2+(int)(28*c.organization()),y+13,0xFF65B647);
        g.fill(x+2,y+13,x+30,y+15,0xFF191509);g.fill(x+2,y+13,x+2+(int)(28*c.strength()),y+15,0xFFE0AD39);
        String count=c.count()<1000?Integer.toString(c.count()):decimal(c.count()/(c.count()<1_000_000?1000.0:1_000_000.0))+(c.count()<1_000_000?"K":"M");
        UiText.centered(g,Minecraft.getInstance().font,count,x+32,y+2,WIDTH-34,HEIGHT-4,0xFFFFFFFF);
    }
    private static String decimal(double value){return BigDecimal.valueOf(value).setScale(1,RoundingMode.DOWN).stripTrailingZeros().toPlainString();}
    private static void tooltip(GuiGraphicsExtractor g,int mx,int my) {
        if(Minecraft.getInstance().gui.hud.isHidden())return;
        Counter hovered=null;
        for(var box:boxes)if(box.contains(mx,my))hovered=box.counter();
        if(hovered==null)return;
        var c=hovered;var font=Minecraft.getInstance().font;
        int w=Math.min(286,g.guiWidth()-12);
        var title=font.split(Component.literal(c.countryName()+" · "+c.name()+" "+c.count()+"개").withColor(0xE6C779),Math.max(1,(int)((w-12)/UiText.scale(font))));
        int titleCount=Math.min(3,title.size()),columns=Math.max(1,(w-12)/44);
        int h=12+titleCount*12+40+((c.battalions().size()+columns-1)/columns)*16;
        int x=Math.clamp(mx+12,6,Math.max(6,g.guiWidth()-w-6)),y=Math.clamp(my+12,6,Math.max(6,g.guiHeight()-h-6));
        g.nextStratum();g.fill(x,y,x+w,y+h,0xF0100812);g.outline(x,y,w,h,0xFF873B98);
        for(int i=0;i<titleCount;i++)UiText.text(g,font,title.get(i),x+6,y+6+i*12,0xFFFFFFFF);
        int line=y+8+titleCount*12;
        String[] labels={"평균 조직력", "평균 내구도", "보급 상황"};double[] ratios={c.organization(),c.strength(),c.supply()};
        for(int i=0;i<3;i++)UiText.text(g,font,Component.literal(labels[i]+": ").append(Component.literal(decimal(ratios[i]*100)+"%").withColor(0xE6C779)),x+6,line+i*12,0xFFFFFFFF);
        int i=0;
        for(var entry:new TreeMap<>(c.battalions()).entrySet()) {
            int bx=x+6+i%columns*44,by=line+40+i/columns*16;
            UiAssets.draw(g,"production/units/"+entry.getKey(),bx,by,16,12);
            UiText.text(g,font,Integer.toString(entry.getValue()),bx+18,by+1,0xFFFFFFFF);i++;
        }
    }
}
