package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.ProtocolCommanderHud;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class MapCommanderHud {
    private static View view;
    private static int age,page;
    static record Box(int x,int y,int width,int height,Open request,int pageDelta,String tip){
        Box(int x,int y,int width,int height,Open request,String tip){this(x,y,width,height,request,0,tip);}
        boolean contains(double mx,double my){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}
    }
    static List<Box> boxes(){return boxes;}
    private static List<Box> boxes=List.of();
    private MapCommanderHud() {}
    public static void register(){
        ProtocolCommanderHud.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE,(packet,context)->{try{accept(packet.view());}catch(IllegalArgumentException error){reset();}});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(MapCommanderHud::reset));
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(view!=null&&(client.level==null||!view.dimension().equals(client.level.dimension().identifier().toString())||!view.country().equals(ClientCampaignHud.country())||++age>=40))reset();});
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,Identifier.parse("hoi:commander_hud"),(g,delta)->{if(Minecraft.getInstance().gui.screen()==null)draw(g,0,g.guiWidth()/2,g.guiHeight()/2);});
        ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{if(screen instanceof InputSidebarMovement.InputMovementScreen sidebar)ScreenEvents.afterExtract(screen).register((s,g,mx,my,delta)->{if(sidebar.allowsMovement())draw(g,sidebar.unitHudLeft(),mx,my);});});
    }
    public static void accept(View next){var client=Minecraft.getInstance();if(client.level==null||!next.dimension().equals(client.level.dimension().identifier().toString())||!next.country().equals(ClientCampaignHud.country())){reset();return;}if(view==null||!view.country().equals(next.country())||!view.dimension().equals(next.dimension()))page=0;view=next;age=0;boxes=List.of();}
    public static void reset(){view=null;boxes=List.of();age=0;page=0;}
    public static void open(Open request){if(ClientPlayNetworking.canSend(Open.TYPE))ClientPlayNetworking.send(request);}
    public static boolean clickAt(double x,double y){if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden())return false;for(var box:boxes)if(box.contains(x,y)){if(box.pageDelta()!=0){page+=box.pageDelta();boxes=List.of();}else open(box.request());return true;}return false;}
    private static void draw(GuiGraphicsExtractor g,int left,int mx,int my){
        boxes=List.of();var client=Minecraft.getInstance();if(view==null||client.level==null||client.gui.hud.isHidden()||!view.country().equals(ClientCampaignHud.country())||!view.dimension().equals(client.level.dimension().identifier().toString())||left>=g.guiWidth()-65)return;
        int columns=Math.max(1,(g.guiWidth()-left-72)/56),capacity=columns*2;
        int lastPage=Math.max(0,(view.cards().size()-1)/capacity);page=Math.clamp(page,0,lastPage);
        var selected=MapUnitHud.selectedKeys().stream().sorted().toList();
        int shown=Math.min(capacity,Math.max(0,view.cards().size()-page*capacity));int stride=view.branch()==Mode.ARMY||view.cards().stream().anyMatch(c->c.role()==Mode.ASSIGN_ARMY||c.role()==Mode.ARMY_GROUP)?74:52;int y=g.guiHeight()-stride-56;int x=Math.max(left+6,(g.guiWidth()+left-Math.min(columns,Math.max(1,shown))*56-62)/2);var list=new ArrayList<Box>();
        for(int i=0;i<shown;i++) {
            var card=view.cards().get(page*capacity+i);int bx=x+i%columns*56,by=y+i/columns*stride;
            UiMenuStyle.control(g,bx,by,52,49,false,false);UiAssets.draw(g,card.portrait(),bx+4,by+2,44,33);
            boolean assigned=!card.portrait().equals("politics/empty/leader");
            UiText.centered(g,client.font,assigned?card.count()+" / "+card.capacity():Integer.toString(card.count()),bx+1,by+35,50,12,!assigned?UiMenuStyle.TEXT:card.count()>card.capacity()?0xFFE15B5B:0xFF83C64F);
            var mode=card.role()==Mode.ARMY_GROUP?Mode.ASSIGN_GROUP_MARSHAL:(view.branch()==Mode.ARMY||card.role()==Mode.ASSIGN_ARMY)?Mode.ASSIGN_ARMY:view.branch()==Mode.NAVY_HQ?Mode.NAVY_FLEET:view.branch();
            list.add(new Box(bx,by,52,35,new Open(mode,card.id()),card.name()+"\n"+card.leader()+"\n"+card.detail()));
            var membership=card.role()==Mode.ARMY_GROUP?new Open(Mode.ARMY_GROUP,card.id()):(view.branch()==Mode.ARMY||card.role()==Mode.ASSIGN_ARMY)&&!selected.isEmpty()?new Open(Mode.MOVE_DIVISIONS,card.id(),selected)
                    :view.branch()==Mode.NAVY_HQ?new Open(Mode.NAVAL_TASK_FORCES,card.id())
                    :view.branch()==Mode.AIR&&card.role()!=Mode.ASSIGN_ARMY?new Open(Mode.AIR_MISSION,card.id()):new Open(Mode.ARMY_PLANS,card.id());
            list.add(new Box(bx,by+35,52,14,membership,membership.mode()==Mode.MOVE_DIVISIONS?"선택한 자국 사단을 이 군으로 이동":view.branch()==Mode.NAVY_HQ?"태스크 포스 임무 선택":view.branch()==Mode.AIR?"비행단 임무 선택":"전투 계획 · "+card.name()+"\n"+card.leader()+"\n"+card.detail()));
            if((view.branch()==Mode.ARMY||card.role()==Mode.ASSIGN_ARMY)&&card.role()!=Mode.ARMY_GROUP) {
                UiMenuStyle.control(g,bx,by-21,52,19,false,false);if(!card.marshalPortrait().isEmpty())UiAssets.draw(g,card.marshalPortrait(),bx+2,by-20,17,17);
                UiText.centered(g,client.font,card.marshal().isEmpty()?"집단군":card.marshal(),bx+20,by-20,31,17,UiMenuStyle.TEXT);
                list.add(new Box(bx,by-21,52,19,new Open(Mode.ARMY_GROUPS,"",List.of(card.id())),card.marshal().isEmpty()?"집단군":card.marshal()));
            }
        }
        int addX=Math.min(g.guiWidth()-58,x+Math.min(columns,Math.max(1,shown))*56);
        if(view.branch()==Mode.ARMY) {
            UiMenuStyle.control(g,addX,y-21,50,19,false,false);UiText.centered(g,client.font,"집단군 +",addX,y-21,50,19,UiMenuStyle.TEXT);
            list.add(new Box(addX,y-21,50,19,new Open(Mode.ARMY_GROUPS,""),"집단군 생성·편입·원수 임명"));
            UiMenuStyle.control(g,addX,y+2,50,45,false,false);UiText.centered(g,client.font,"+",addX,y+3,50,30,UiMenuStyle.TEXT);
            list.add(new Box(addX,y+2,50,45,new Open(Mode.CREATE_ARMY,"",selected),"선택한 자국 사단으로 새 군 생성"));
        }
        if(view.branch()==Mode.NAVY_HQ) {
            UiMenuStyle.control(g,addX,y+2,50,45,false,false);UiText.centered(g,client.font,"+",addX,y+3,50,30,UiMenuStyle.TEXT);
            list.add(new Box(addX,y+2,50,45,new Open(Mode.NAVY_HQ,""),"함대 생성·태스크 포스 편입"));
        }
        if(view.branch()==Mode.NAVY_HQ||view.branch()==Mode.AIR) {
            UiMenuStyle.control(g,addX,y+50,50,19,false,false);UiText.centered(g,client.font,"임무",addX,y+50,50,19,UiMenuStyle.TEXT);
            list.add(new Box(addX,y+50,50,19,new Open(view.branch()==Mode.NAVY_HQ?Mode.NAVAL_ORDERS:Mode.AIR_ORDERS,""),"여러 부대의 임무·목표 지역 배정"));
        }
        if(lastPage>0) {
            int px=g.guiWidth()-56,py=g.guiHeight()-28;
            UiText.centered(g,client.font,(page+1)+" / "+(lastPage+1),px,py-16,50,14,UiMenuStyle.TEXT);
            for(int delta:new int[]{-1,1}) {
                int ax=px+(delta<0?0:26);boolean enabled=delta<0?page>0:page<lastPage;
                UiMenuStyle.control(g,ax,py,24,20,false,false);
                UiText.centered(g,client.font,delta<0?"‹":"›",ax,py,24,20,enabled?UiMenuStyle.TEXT:UiMenuStyle.MUTED);
                if(enabled)list.add(new Box(ax,py,24,20,null,delta,delta<0?"이전 지휘관":"다음 지휘관"));
            }
        }
        boxes=List.copyOf(list);
        for(var box:boxes)if(box.contains(mx,my)){UiTooltips.drawExact(g,client.font,box.tip(),mx,my,240);break;}
    }
}
