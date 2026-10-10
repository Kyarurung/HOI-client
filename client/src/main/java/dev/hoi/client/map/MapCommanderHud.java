package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
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
    static final MapCommanderGroups groups=new MapCommanderGroups();
    record Band(String owner,int x,int y,int width,int height,int color) {}
    private static List<Band> bands=List.of();
    static List<Band> bands(){return bands;}
    static java.util.function.Consumer<Open> requestObserver;
    private static int page;
    private static String paletteGroup="";
    private static String selectedCard="",hovered="",renderedTooltip="";
    static String renderedTooltip(){return renderedTooltip;}
    record TooltipBounds(int x,int y,int width,int height) {}
    private static TooltipBounds tooltipBounds;
    static TooltipBounds tooltipBounds(){return tooltipBounds;}
    private static org.joml.Vector2ic tooltipPosition(int width,int height,int mouseX,int mouseY,int textWidth,int textHeight){
        var original=net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE.positionTooltip(width,height,mouseX,mouseY,textWidth,textHeight);
        int x=Math.clamp(original.x(),7,Math.max(7,width-textWidth-7)),y=Math.clamp(original.y(),7,Math.max(7,height-textHeight-7));
        var protectedHud=protectedHud(width,height);
        if(x-7<protectedHud.x()+protectedHud.width()&&x+textWidth+7>protectedHud.x()&&y+textHeight+7>protectedHud.y()){
            int right=protectedHud.x()+protectedHud.width()+9,left=protectedHud.x()-textWidth-9;
            if(right+textWidth+7<=width)x=right;
            else if(left>=7)x=left;
            else if(protectedHud.y()-textHeight-9>=7)y=protectedHud.y()-textHeight-9;
        }
        tooltipBounds=new TooltipBounds(x-7,y-7,textWidth+14,textHeight+14);return new org.joml.Vector2i(x,y);
    }
    static TooltipBounds protectedHud(int width,int height){
        var units=new UiUnits(width,height);int left=Math.min((width-182)/2,(width-units.size(320.5))/2),right=width-left;
        for(var box:boxes){left=Math.min(left,box.x());right=Math.max(right,box.x()+box.width());}
        int top=MapCommanderLayout.popupBottom(units,false);return new TooltipBounds(left,top,right-left,height-top);
    }
    static record Box(int x,int y,int width,int height,Open request,int pageDelta,String tip,Runnable action){
        Box { tip=UiCommandLabels.withoutShortcuts(tip); }
        Box(int x,int y,int width,int height,Open request,int pageDelta,String tip){this(x,y,width,height,request,pageDelta,tip,null);}
        Box(int x,int y,int width,int height,Open request,String tip){this(x,y,width,height,request,0,tip);}
        boolean contains(double mx,double my){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}
    }
    static List<Box> boxes(){return boxes;}
    private static List<Box> boxes=List.of();
    private MapCommanderHud() {}
    public static void register(){
        ProtocolCommanderHud.registerPayloadTypes();MapCommanderOrders.register();
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE,(packet,context)->{try{accept(packet.view());}catch(IllegalArgumentException error){reset();}});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(MapCommanderHud::reset));
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(view!=null&&(client.level==null||!view.dimension().equals(client.level.dimension().identifier().toString())||!view.country().equals(ClientCampaignHud.country())))reset();});
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,Identifier.parse("hoi:commander_hud"),(g,delta)->{if(Minecraft.getInstance().gui.screen()==null)draw(g,0,g.guiWidth()/2,g.guiHeight()/2);});
        ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{if(screen instanceof InputSidebarMovement.InputMovementScreen sidebar)ScreenEvents.afterExtract(screen).register((s,g,mx,my,delta)->{if(sidebar.allowsMovement())draw(g,sidebar.unitHudLeft(),mx,my);});});
    }
    public static void accept(View next){var client=Minecraft.getInstance();if(next.country().isEmpty()||client.level==null||!next.dimension().equals(client.level.dimension().identifier().toString())||!next.country().equals(ClientCampaignHud.country())){reset();return;}if(view==null||!view.country().equals(next.country())||!view.dimension().equals(next.dimension())||view.branch()!=next.branch()){page=0;paletteGroup="";}if(next.cards().stream().noneMatch(c->c.id().equals(paletteGroup)))paletteGroup="";groups.accept(next);view=next;MapCommanderPanels.accept(next);if(next.cards().stream().noneMatch(card->MapCommanderPanels.selected(card.id())))selectedCard="";boxes=List.of();}
    public static void reset(){paletteGroup="";groups.clear();bands=List.of();tooltipBounds=null;renderedTooltip="";hovered="";MapCommanderOrders.reset();MapCommanderPanels.reset();selectedCard="";view=null;boxes=List.of();page=0;}
    public static void open(Open request){if(requestObserver!=null){requestObserver.accept(request);return;}if(MapCommanderOrders.route(request))return;if(ClientPlayNetworking.canSend(Open.TYPE))ClientPlayNetworking.send(request);}
    public static boolean closeSelection(){boolean closed=MapCommanderPanels.closeSelection();if(closed)dev.hoi.client.audio.AudioUiSounds.play("ui.close");return closed;}
    public static boolean scrollAt(double x,double y,double amount){
        if(MapCommanderPanels.scrollAt(x,y,amount))return true;
        if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden()||boxes.stream().noneMatch(box->box.contains(x,y)))return false;
        int delta=amount>0?-1:amount<0?1:0;
        if(delta!=0&&boxes.stream().anyMatch(box->box.pageDelta()==delta)){page+=delta;boxes=List.of();}
        return true;
    }
    public static boolean clickAt(double x,double y,int button,boolean twice){if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden())return false;if(MapCommanderPanels.click(x,y,button,twice))return true;if(button==InputConstants.MOUSE_BUTTON_RIGHT){if(boxes.stream().anyMatch(box->box.action()!=null&&box.contains(x,y)))return true;if(formationAt(x,y))return true;var wings=MapCommanderPanels.attachmentWings();if(!wings.isEmpty())for(var box:boxes)if(box.contains(x,y)&&box.request()!=null&&box.request().mode()==Mode.ASSIGN_ARMY&&view.cards().stream().anyMatch(c->c.id().equals(box.request().target())&&c.role()==Mode.ASSIGN_ARMY)){var values=new ArrayList<String>();values.add("army/"+box.request().target());values.addAll(wings);dev.hoi.client.audio.AudioUiSounds.play("ui.click");open(new Open(Mode.AIR_ATTACH,"",values));return true;}}return button==InputConstants.MOUSE_BUTTON_LEFT?clickAt(x,y):boxes.stream().anyMatch(box->box.contains(x,y));}
    public static boolean clickAt(double x,double y){if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden())return false;if(MapCommanderPanels.click(x,y))return true;for(var box:boxes)if(box.contains(x,y)){dev.hoi.client.audio.AudioUiSounds.play(box.request()!=null&&(box.request().mode()==Mode.ASSIGN_ARMY||box.request().mode()==Mode.ASSIGN_GROUP_MARSHAL||box.request().mode()==Mode.NAVY_FLEET)?"ui.division.select":box.request()!=null&&box.request().mode()==Mode.ARMY_TOOLBAR?"ui.battleplan":"ui.click");if(box.action()!=null){box.action().run();}else if(box.pageDelta()!=0){page+=box.pageDelta();boxes=List.of();}else {selectedCard=box.request().target();if(view.cards().stream().anyMatch(c->c.id().equals(selectedCard))&&(box.request().mode()==Mode.ASSIGN_ARMY||box.request().mode()==Mode.ASSIGN_GROUP_MARSHAL||box.request().mode()==Mode.NAVY_FLEET))MapCommanderPanels.select(selectedCard);else open(box.request());}return true;}return false;}
    static String countLabel(Card card){boolean assigned=armyCount(card)&&card.panel()!=null?!card.panel().commander().isEmpty():!card.portrait().equals("politics/empty/leader");return assigned?card.count()+" / "+card.capacity():Integer.toString(card.count());}
    private static boolean armyCount(Card card){return card.role()==Mode.ASSIGN_ARMY||card.role()==Mode.ARMY_GROUP;}
    private static int countColor(Card card){return armyCount(card)?0xFFFFFFFF:card.portrait().equals("politics/empty/leader")?UiMenuStyle.TEXT:card.count()>card.capacity()?0xFFE15B5B:0xFF83C64F;}
    private static boolean formationAt(double x,double y){
        if(view.branch()!=Mode.ARMY||!MapCommanderPanels.attachmentWings().isEmpty())return false;
        for(var box:boxes)if(box.contains(x,y)&&box.request()!=null){
            var request=box.request();var card=view.cards().stream().filter(c->c.id().equals(request.target())).findFirst().orElse(null);
            if(card!=null&&card.role()==Mode.ASSIGN_ARMY){MapCommanderOrders.formation("MOVE_DIVISIONS",card.id(),MapCommanderPanels.commandSelection());return true;}
            if(card!=null&&card.role()==Mode.ARMY_GROUP){MapCommanderOrders.formation("MOVE_ARMIES",card.id(),MapCommanderPanels.formationArmies());return true;}
            if(request.mode()==Mode.ARMY_GROUPS){MapCommanderOrders.formation("CREATE_GROUP","",MapCommanderPanels.formationArmies());return true;}
            if(request.mode()==Mode.CREATE_ARMY){MapCommanderOrders.formation("CREATE_ARMY","",MapCommanderPanels.commandSelection());return true;}
        }
        return false;
    }
    static boolean updateHover(String key){boolean entered=!key.isEmpty()&&!key.equals(hovered);hovered=key;return entered;}
    private static void draw(GuiGraphicsExtractor g,int left,int mx,int my){
        tooltipBounds=null;renderedTooltip="";int sidebar=left;left=0;
        boxes=List.of();bands=List.of();var client=Minecraft.getInstance();if(view==null||client.level==null||client.gui.hud.isHidden()||!view.country().equals(ClientCampaignHud.country())||!view.dimension().equals(client.level.dimension().identifier().toString())||left>=g.guiWidth()-65){updateHover("");return;}
        var units=new UiUnits(g.guiWidth(),g.guiHeight());
        boolean navalBranch=view.branch()==Mode.NAVY_HQ;
        int cardWidth=units.size(42),cardHeight=units.size(58.5),footHeight=units.size(14),pitch=units.size(50);
        int creationSlots=navalBranch?1:2,airSpace=view.branch()==Mode.AIR?units.size(23):0;
        int columns=Math.max(2,(g.guiWidth()-units.size(12)-airSpace)/pitch-creationSlots);
        boolean paged=groups.pages(view,columns).size()>1;
        if(paged)columns=Math.max(2,(g.guiWidth()-units.size(72)-airSpace)/pitch-creationSlots);
        int capacity=columns,contentWidth=g.guiWidth()-(paged?units.size(60):0)-airSpace;
        var pages=groups.pages(view,capacity);int lastPage=pages.size()-1;page=Math.clamp(page,0,lastPage);var visible=pages.get(page);
        var selected=MapUnitHud.selectedKeys().stream().sorted().toList();
        int shown=visible.size();
        int bottom=MapCommanderLayout.cardsBottom(units),y=bottom-cardHeight;
        int stripX=Math.max(units.size(6),(contentWidth-(shown+creationSlots)*pitch)/2),ordinaryX=stripX+pitch;
        var list=new ArrayList<Box>();var drawnBands=new ArrayList<Band>();
        for(int first=0;first<shown;){String owner=groups.owner(visible.get(first),view);int end=first+1;if(owner.isEmpty()){first=end;continue;}while(end<shown&&groups.owner(visible.get(end),view).equals(owner))end++;
            int sx=ordinaryX+first*pitch-units.size(2),sy=bottom-units.size(76),sw=(end-first)*pitch-units.size(3),sh=units.size(76),color=groups.color(owner,view);g.fill(sx,sy,sx+sw,sy+sh,0xb0000000|(color&0xffffff));
            for(int n=first;n<end;n++)UiAssets.drawTinted(g,"commander/commands/lg_army_mid_bg/frame_0",ordinaryX+n*pitch-units.size(2),sy,pitch,sh,color);
            drawnBands.add(new Band(owner,sx,sy,sw,sh,color));first=end;
        }
        bands=List.copyOf(drawnBands);
        for(int i=0;i<shown;i++) {
            var card=visible.get(i);boolean group=card.role()==Mode.ARMY_GROUP;
            boolean naval=view.branch()==Mode.NAVY_HQ,wing=view.branch()==Mode.AIR&&!group&&card.role()!=Mode.ASSIGN_ARMY;
            int cw=units.size(group?45:42),ch=units.size(group?63:58.5),bx=ordinaryX+i*pitch,by=bottom-ch;
            String art="commander/hud/"+(group?"group":"army");
            boolean hover=mx>=bx&&mx<bx+cw&&my>=by&&my<by+ch;
            int innerX=bx+units.size(group?4.5:4),innerY=by+units.size(3),innerWidth=units.size(group?35:30.5),innerHeight=units.size(group?48:41.5);
            if(!wing)UiAssets.draw(g,art+(hover?"_hover/frame_0":"_background/frame_0"),innerX,innerY,innerWidth,innerHeight);
            String portrait=card.portrait().equals("politics/empty/leader")?(wing?"unit_hud/air_fighter":art+"_unknown/frame_0"):card.portrait();
            UiAssets.drawContent(g,portrait,innerX,innerY,innerWidth,innerHeight);
            if(!wing)UiAssets.draw(g,(naval?"commander/hud/navy":art)+"_frame/frame_"+(MapCommanderPanels.selected(card.id())||naval&&hover?1:0),bx,by,cw,ch);
            if(!naval&&!wing&&card.panel()!=null){
                var armyCards=view.cards().stream().filter(c->c.panel()!=null&&c.role()!=Mode.ARMY_GROUP&&(c.id().equals(card.id())||c.panel().parent().equals(card.id()))).toList();var execution=armyCards.stream().flatMap(c->c.panel().controls().stream()).filter(c->c.id().equals("ACTIVATE")||c.id().equals("DEACTIVATE")).toList();
                if(!execution.isEmpty()){boolean active=execution.stream().anyMatch(c->c.id().equals("DEACTIVATE"));int ex=bx+units.size(group?20:18),ey=by-units.size(group?21:16),ew=units.size(21.5),eh=units.size(11.5);String icon="commander/commands/execute_button_"+(active?"stop":"play")+"_small"+(group?"_ag":"")+"/frame_0";UiAssets.draw(g,icon,ex,ey,ew,eh);String reason=MapCommanderAvailability.reason(active?"ARMY/DEACTIVATE":"ARMY/ACTIVATE",armyCards.stream().map(Card::id).toList());boolean enabled=reason.isEmpty()&&execution.stream().anyMatch(Control::enabled);String tip=active?"전투 계획 실행 중지":execution.stream().map(Control::tooltip).distinct().reduce((a,b)->a+"\n\n"+b).orElse("전투 계획 실행");tip=MapCommanderAvailability.explain(tip,reason);list.add(new Box(ex,ey,ew,eh,enabled?new Open(Mode.ARMY_TOOLBAR,active?"DEACTIVATE":"ACTIVATE",armyCards.stream().map(Card::id).toList()):null,0,tip,enabled?null:()->{}));if(!enabled)g.fill(ex,ey,ex+ew,ey+eh,0x66000000);}
            }
            var attached=view.wings().stream().filter(wingMember->wingMember.data()!=null&&wingMember.data().attachedArmy().equals(card.id())).toList();
            if(!attached.isEmpty()){
                int ay=by-units.size(32),ah=units.size(14);UiAssets.draw(g,"commander/commands/army_select_wings_btn/frame_0",bx,ay,cw,ah);UiAssets.drawContent(g,"unit_hud/air_fighter",bx+units.size(2),ay+units.size(2),units.size(16),units.size(10));
                UiText.centeredSingleLine(g,client.font,net.minecraft.network.chat.Component.literal(Integer.toString(attached.stream().mapToInt(wingMember->wingMember.data().quantity()).sum())),bx+units.size(18),ay,cw-units.size(19),ah,UiMenuStyle.TEXT);
                list.add(new Box(bx,ay,cw,ah,null,0,UiCommandLabels.text("SELECT_ATTACHED_AIRWINGS","부속된 비행단 선택"),()->MapCommanderPanels.showWings(attached.stream().map(Member::id).toList())));
            }
            UiText.centeredSingleLine(g,client.font,net.minecraft.network.chat.Component.literal(countLabel(card)),bx+units.size(1),by+ch-footHeight-units.size(2),cw-units.size(2),footHeight,countColor(card));
            if(group&&card.panel()!=null){
                int sx=bx+units.size(2),sy=by-units.size(6),size=units.size(9),color=groups.color(card.id(),view);g.fill(sx,sy,sx+size,sy+size,color);
                String tip="집단군 색상 · "+(card.panel().color()==null?"미지정":String.format(Locale.ROOT,"#%06X",card.panel().color()));
                if(!card.panel().palette().isEmpty())list.add(new Box(sx,sy,size,size,null,0,tip+"\n"+UiCommandLabels.text("SELECT_COLOR","지도 색상 선택"),()->{paletteGroup=paletteGroup.equals(card.id())?"":card.id();boxes=List.of();}));
            }
            if(!wing){
                int controlX=bx+cw-units.size(10),closeY=by-units.size(6),controlWidth=units.size(9),controlHeight=units.size(9);
                UiAssets.draw(g,"commander/commands/lg_disband_group_btn/frame_0",controlX,closeY,controlWidth,controlHeight);
                list.add(new Box(controlX,closeY,controlWidth,controlHeight,null,0,"선택 해제 · "+card.name(),()->MapCommanderPanels.closeCard(card)));
                boolean expanded=group?groups.expanded(card.id()):groups.rowsVisible(card.id());int foldY=by+units.size(7);
                UiAssets.draw(g,"commander/commands/lg_expand_group_btn/frame_"+(expanded?0:1),controlX,foldY,controlWidth,units.size(12));
                String tip=group?UiCommandLabels.text(expanded?"CLICK_TO_HIDE_ARMIES":"CLICK_TO_SHOW_ARMIES",expanded?"소속 군 접기":"소속 군 펼치기"):(expanded?"소속 부대 목록 접기":"소속 부대 목록 펼치기");
                list.add(new Box(controlX,foldY,controlWidth,units.size(12),null,0,tip,()->{groups.toggle(card);boxes=List.of();}));
            }
            var mode=card.role()==Mode.ARMY_GROUP?Mode.ASSIGN_GROUP_MARSHAL:(view.branch()==Mode.ARMY||card.role()==Mode.ASSIGN_ARMY)?Mode.ASSIGN_ARMY:view.branch()==Mode.NAVY_HQ?Mode.NAVY_FLEET:view.branch();
            list.add(new Box(bx,by,cw,ch-footHeight,new Open(mode,card.id()),card.name()+"\n"+card.leader()+"\n"+card.detail()+(view.branch()==Mode.ARMY?"\n\n"+(card.role()==Mode.ARMY_GROUP?"우클릭: 선택한 군을 이 집단군에 편입":"우클릭: 선택한 사단을 이 군으로 이동"):"")+(mode==Mode.ASSIGN_ARMY&&!MapCommanderPanels.attachmentWings().isEmpty()?"\n\n"+UiCommandLabels.text("ATTACH_AIRWING_TO_GROUP_TOOLTIP","우클릭하여 비행단을 군에 배치"):"")));
            var membership=card.role()==Mode.ARMY_GROUP?new Open(Mode.ARMY_GROUP,card.id()):(view.branch()==Mode.ARMY||card.role()==Mode.ASSIGN_ARMY)&&!selected.isEmpty()?new Open(Mode.MOVE_DIVISIONS,card.id(),selected)
                    :view.branch()==Mode.NAVY_HQ?new Open(Mode.NAVAL_TASK_FORCES,card.id())
                    :view.branch()==Mode.AIR&&card.role()!=Mode.ASSIGN_ARMY?new Open(Mode.AIR_MISSION,card.id()):new Open(Mode.ARMY_PLANS,card.id());
            list.add(new Box(bx,by+ch-footHeight,cw,footHeight,membership,membership.mode()==Mode.MOVE_DIVISIONS?"선택한 자국 사단을 이 군으로 이동":view.branch()==Mode.NAVY_HQ?"태스크 포스 임무 선택":view.branch()==Mode.AIR?"비행단 임무 선택":"전투 계획 · "+card.name()+"\n"+card.leader()+"\n"+card.detail()));
        }
        int addX=ordinaryX+shown*pitch;
        if(!navalBranch) {
            int groupWidth=units.size(45),groupHeight=units.size(63),groupY=bottom-groupHeight;
            UiAssets.draw(g,"commander/hud/group_background/frame_0",stripX+units.size(4.5),groupY+units.size(3),units.size(35),units.size(48));
            UiAssets.draw(g,"commander/hud/group_frame/frame_0",stripX,groupY,groupWidth,groupHeight);
            list.add(new Box(stripX,groupY,groupWidth,groupHeight,new Open(Mode.ARMY_GROUPS,""),"좌클릭 또는 우클릭: 선택한 군으로 새 집단군 생성\n기존 집단군 편입: 해당 집단군 카드 우클릭"));
            UiAssets.draw(g,"commander/hud/army_background/frame_0",addX+units.size(4),y+units.size(3),units.size(30.5),units.size(41.5));
            UiAssets.draw(g,"commander/hud/army_frame/frame_0",addX,y,cardWidth,cardHeight);
            list.add(new Box(addX,y,cardWidth,cardHeight,new Open(Mode.CREATE_ARMY,"",selected),"선택한 자국 사단으로 새 군 생성"));
        } else {
            addX=stripX;
            UiAssets.draw(g,"commander/hud/army_background/frame_0",addX+units.size(4),y+units.size(3),units.size(30.5),units.size(41.5));
            UiAssets.draw(g,"commander/hud/army_border/frame_0",addX,y,units.size(39.5),units.size(55.5));
            list.add(new Box(addX,y,cardWidth,cardHeight,new Open(Mode.NAVY_HQ,""),"함대 생성·태스크 포스 편입"));
        }
        if(view.branch()==Mode.AIR){int cx=addX+pitch,cy=bottom-units.size(19);UiAssets.draw(g,"commander/commands/air_base_create_new_btn/frame_0",cx,cy,units.size(19),units.size(19));list.add(new Box(cx,cy,units.size(19),units.size(19),new Open(Mode.AIR_ORGANIZE,"",List.of("action/CREATE")),UiCommandLabels.text("AIRWING_NEW","신규 비행단 창설")));}
        if(lastPage>0) {
            int px=g.guiWidth()-units.size(56),py=y+cardHeight-units.size(20);
            UiText.centeredSingleLine(g,client.font,net.minecraft.network.chat.Component.literal((page+1)+" / "+(lastPage+1)),px,py-units.size(16),units.size(50),units.size(14),UiMenuStyle.TEXT);
            for(int delta:new int[]{-1,1}) {
                int ax=px+(delta<0?0:units.size(26));boolean enabled=delta<0?page>0:page<lastPage;
                UiMenuStyle.control(g,ax,py,units.size(24),units.size(20),false,false);
                UiText.centeredSingleLine(g,client.font,net.minecraft.network.chat.Component.literal(delta<0?"‹":"›"),ax,py,units.size(24),units.size(20),enabled?UiMenuStyle.TEXT:UiMenuStyle.MUTED);
                if(enabled)list.add(new Box(ax,py,units.size(24),units.size(20),null,delta,delta<0?"이전 지휘관":"다음 지휘관"));
            }
        }
        var colorCard=view.cards().stream().filter(c->c.id().equals(paletteGroup)&&c.panel()!=null).findFirst().orElse(null);
        if(colorCard!=null&&!colorCard.panel().palette().isEmpty()){
            int cell=units.size(14),pw=colorCard.panel().palette().size()*cell,px=Math.max(units.size(4),(g.guiWidth()-pw)/2),py=MapCommanderLayout.popupBottom(units,false)-cell-units.size(5);g.fill(px-units.size(3),py-units.size(3),px+pw+units.size(3),py+cell+units.size(3),0xee101418);
            for(int index=0;index<colorCard.panel().palette().size();index++){int rgb=colorCard.panel().palette().get(index),cx=px+index*cell;g.fill(cx+1,py+1,cx+cell-1,py+cell-1,0xff000000|rgb);list.add(0,new Box(cx,py,cell,cell,null,0,"집단군 색상 #"+String.format(Locale.ROOT,"%06X",rgb),()->{MapCommanderOrders.groupColor(colorCard.id(),rgb);paletteGroup="";boxes=List.of();}));}
        }
        boxes=List.copyOf(list);
        MapCommanderPanels.draw(g,sidebar,mx,my,list.stream().mapToInt(Box::y).min().orElse(y));
        MapCommanderOrders.drawMessage(g);
        String tooltip=MapCommanderPanels.tooltipAt(mx,my);int tooltipWidth=new UiUnits(g.guiWidth(),g.guiHeight()).size(320);
        if(tooltip.isEmpty()){tooltipWidth=240;for(var box:boxes)if(box.contains(mx,my)){tooltip=box.tip();break;}}
        if(!tooltip.isEmpty()){
            var lines=UiTooltips.sourceLines(client.font,tooltip,Math.min(g.guiWidth(),tooltipWidth+24));int textWidth=lines.stream().mapToInt(client.font::width).max().orElse(0),lineHeight=client.font.lineHeight+1,textHeight=lines.size()*lineHeight;var position=tooltipPosition(g.guiWidth(),g.guiHeight(),mx,my,textWidth,textHeight);
            g.nextStratum();UiTooltips.background(g,position.x()-7,position.y()-7,textWidth+14,textHeight+14);for(int n=0;n<lines.size();n++)g.text(client.font,lines.get(n),position.x(),position.y()+n*lineHeight,0xFFE0E0E0);renderedTooltip=tooltip;
        }
        String hover=MapCommanderPanels.hoverKey(mx,my);if(hover.isEmpty())for(var box:boxes)if(box.contains(mx,my)){hover="hud/"+box.x()+"/"+box.y()+"/"+box.width()+"/"+box.height();break;}if(updateHover(hover))dev.hoi.client.audio.AudioUiSounds.play("ui.menu_over");
    }
}
