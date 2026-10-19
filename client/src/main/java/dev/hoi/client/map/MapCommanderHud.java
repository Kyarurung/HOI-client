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
            if(protectedHud.y()-textHeight-9>=7)y=protectedHud.y()-textHeight-9;
            else if(right+textWidth+7<=width)x=right;
            else if(left>=7)x=left;
        }
        tooltipBounds=new TooltipBounds(x-7,y-7,textWidth+14,textHeight+14);return new org.joml.Vector2i(x,y);
    }
    static TooltipBounds protectedHud(int width,int height){
        var units=MapCommanderLayout.units(width,height);int left=Math.min((width-182)/2,(width-units.size(320.5))/2),right=Math.max(width-left,MapCommanderLayout.commandLeft(units,false)+units.size(320.5));
        for(var box:boxes){left=Math.min(left,box.x());right=Math.max(right,box.x()+box.width());}
        int top=MapCommanderLayout.popupBottom(units,false);
        if(view!=null&&view.branch()==Mode.ARMY&&MapCommanderPanels.hits().stream().noneMatch(hit->hit.icon().startsWith("bpt_")||hit.icon().equals("exercise_icon")))top=boxes.stream().mapToInt(Box::y).min().orElse(MapCommanderLayout.cardsBottom(units));
        return new TooltipBounds(left,top,right-left,height-top);
    }
    static record Box(int x,int y,int width,int height,Open request,int pageDelta,String tip,Runnable action,int horizontalInset,float pixelShift){
        Box(int x,int y,int width,int height,Open request,int pageDelta,String tip,Runnable action,int horizontalInset){this(x,y,width,height,request,pageDelta,tip,action,horizontalInset,0);}
        Box(int x,int y,int width,int height,Open request,int pageDelta,String tip,Runnable action){this(x,y,width,height,request,pageDelta,tip,action,0);}
        Box(int x,int y,int width,int height,Open request,int pageDelta,String tip){this(x,y,width,height,request,pageDelta,tip,null);}
        Box(int x,int y,int width,int height,Open request,String tip){this(x,y,width,height,request,0,tip);}
        boolean contains(double mx,double my){return mx>=x+horizontalInset-pixelShift&&mx<x+width-horizontalInset-pixelShift&&my>=y+pixelShift&&my<y+height+pixelShift;}
    }
    static String cardTooltip(Card card){
        String tip=UiCommandLabels.text("SELECT_GROUP_TOOLTIP","");
        if(MapUnitHud.selectedKeys().stream().anyMatch(id->id.startsWith("unit/")))tip+="\n"+UiCommandLabels.text("SELECT_GROUP_WITH_UNITS_SELECTED_TOOLTIP","");
        tip+="\n§H"+card.name()+"§!";
        if(card.panel()!=null&&!card.panel().divisionTypes().isEmpty()){tip+="\n";for(var entry:card.panel().divisionTypes().entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).toList()){String asset=MapCommanderPanels.armyUnitIcon(entry.getKey());String sprite=asset.replace("commander/commands/","").replace("/frame_0","").replace('/','_');tip+="£"+sprite+"£ "+entry.getValue()+"  ";}}
        tip+="\n\n"+UiCommandLabels.text(card.role()==Mode.ARMY_GROUP?"COMMAND_ARMY_GROUP_DESC":"COMMAND_GROUP_DESC","");
        if(!card.tooltip().isEmpty())tip+="\n"+card.tooltip();
        return tip;
    }
    private static void cardOrders(GuiGraphicsExtractor g,UiUnits units,List<Box> list,Card card,int bx,int by,boolean group){
        var controls=card.panel().controls();var defense=controls.stream().filter(c->c.id().equals("DEFENDING_AREA")).findFirst().orElse(null);
        var training=controls.stream().filter(c->c.id().equals("STOP_EXERCISE")).findFirst().orElse(null);
        int ey=by-units.size(group?10:8);
        if(defense!=null){int x=bx+units.size(30.5);UiAssets.draw(g,"commander/commands/area_defense_active/frame_0",x,ey+units.size(1),units.size(8),units.size(9.5));list.add(new Box(x,ey,units.size(8),units.size(11.5),null,0,defense.tooltip(),()->{}));return;}
        boolean execution=controls.stream().anyMatch(c->c.id().equals("ACTIVATE")||c.id().equals("DEACTIVATE"));
        if(training==null&&!execution)return;
        for(boolean stop:List.of(true,false)){
            int ex=bx+units.size(group?(stop?5.5:20.5):(stop?3.5:18.5)),ew=units.size(stop?15:21.5),eh=units.size(13);
            String action=training!=null?"STOP_EXERCISE":stop?"DEACTIVATE":"ACTIVATE";
            var control=controls.stream().filter(c->c.id().equals(action)).findFirst().orElse(null);
            if(training!=null&&!stop){UiAssets.draw(g,"commander/commands/exercise_icon_anim_strip/frame_0",ex,ey,units.size(12.5),units.size(12.5));list.add(new Box(ex,ey,ew,eh,null,0,UiCommandLabels.text("UNITS_VIEW_EXERCISING",""),()->{}));continue;}
            String icon="commander/commands/execute_button_"+(stop?"stop":"play")+"_small"+(group?"_ag":"")+"/frame_0";
            String reason=MapCommanderAvailability.reason("ARMY/"+action,List.of(card.id()));boolean enabled=control!=null&&control.enabled()&&reason.isEmpty();
            if(enabled)UiAssets.draw(g,icon,ex,ey,ew,eh);else UiAssets.drawGrayscale(g,icon,ex,ey,ew,eh);
            list.add(new Box(ex,ey,ew,eh,enabled?new Open(Mode.ARMY_TOOLBAR,action,List.of(card.id())):null,0,MapCommanderAvailability.explain(control==null?"":control.tooltip(),reason),enabled?null:()->{}));
        }
        if(training==null)UiAssets.draw(g,"commander/commands/plan_value_strip/frame_"+(card.panel().planEvaluation()==null?0:card.panel().planEvaluation()+1),bx+units.size(group?22:19),ey+units.size(.5),units.size(8),units.size(11.5));
    }
    static List<Box> boxes(){return boxes;}
    static void togglePalette(String id){paletteGroup=paletteGroup.equals(id)?"":id;boxes=List.of();}
    private static List<Box> boxes=List.of();
    private MapCommanderHud() {}
    static boolean running(){return view!=null&&Boolean.TRUE.equals(view.running());}
    static boolean hidesVanillaHud(){return running()&&cardsVisible();}
    static boolean cardsVisible(){var client=Minecraft.getInstance();var screen=client.gui.screen();return view!=null&&client.level!=null&&!client.gui.hud.isHidden()&&view.country().equals(ClientCampaignHud.country())&&view.dimension().equals(client.level.dimension().identifier().toString())&&(screen==null||screen instanceof InputSidebarMovement.InputMovementScreen sidebar&&sidebar.allowsMovement());}
    public static void register(){
        ProtocolCommanderHud.registerPayloadTypes();MapCommanderOrders.register();
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE,(packet,context)->{try{accept(packet.view());}catch(IllegalArgumentException error){reset();}});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(MapCommanderHud::reset));
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(view!=null&&(client.level==null||!view.dimension().equals(client.level.dimension().identifier().toString())||!view.country().equals(ClientCampaignHud.country())))reset();});
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,Identifier.parse("hoi:commander_hud"),(g,delta)->{if(Minecraft.getInstance().gui.screen()==null)draw(g,0,g.guiWidth()/2,g.guiHeight()/2);});
        ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{if(screen instanceof InputSidebarMovement.InputMovementScreen sidebar)ScreenEvents.afterExtract(screen).register((s,g,mx,my,delta)->{if(sidebar.allowsMovement())draw(g,sidebar.unitHudLeft(),mx,my);});});
    }
    public static void accept(View next){var client=Minecraft.getInstance();if(next.country().isEmpty()||client.level==null||!next.dimension().equals(client.level.dimension().identifier().toString())||!next.country().equals(ClientCampaignHud.country())){reset();return;}if(view==null||!view.country().equals(next.country())||!view.dimension().equals(next.dimension())||view.branch()!=next.branch()){MapCommanderTheaters.reset();page=0;paletteGroup="";}if(next.cards().stream().noneMatch(c->c.id().equals(paletteGroup)))paletteGroup="";MapCommanderCardDrag.accept(next);groups.accept(next);view=next;MapCommanderPanels.accept(next);if(next.cards().stream().noneMatch(card->MapCommanderPanels.selected(card.id())))selectedCard="";boxes=List.of();}
    public static void reset(){MapCommanderTheaters.reset();MapCommanderCardDrag.reset();paletteGroup="";groups.clear();bands=List.of();tooltipBounds=null;renderedTooltip="";hovered="";MapCommanderOrders.reset();MapCommanderPanels.reset();selectedCard="";view=null;boxes=List.of();page=0;}
    public static void open(Open request){if(requestObserver!=null){requestObserver.accept(request);return;}if(MapCommanderOrders.route(request))return;if(ClientPlayNetworking.canSend(Open.TYPE))ClientPlayNetworking.send(request);}
    public static boolean pressAt(double x,double y,int button,boolean twice){if(button==InputConstants.MOUSE_BUTTON_LEFT&&view!=null&&!Minecraft.getInstance().gui.hud.isHidden()&&view.country().equals(ClientCampaignHud.country())&&(MapCommanderPanels.pressScroll(x,y)||MapCommanderCardDrag.begin(view,boxes,x,y)))return true;return clickAt(x,y,button,twice);}
    public static boolean dragAt(double x,double y){return MapCommanderPanels.dragScroll(y)||MapCommanderCardDrag.move(x);}
    public static boolean dragAt(double x){return MapCommanderCardDrag.move(x);}
    public static boolean releaseAt(double x,int button){return button==InputConstants.MOUSE_BUTTON_LEFT&&(MapCommanderPanels.releaseScroll()||MapCommanderCardDrag.release(x));}
    public static void cancelDrag(){MapCommanderCardDrag.cancel();}
    static void reconcileCardOrder(){if(view!=null)MapCommanderCardDrag.reconcile(view);}
    public static boolean closeSelection(){boolean closed=MapCommanderTheaters.closeSettings()||MapCommanderPanels.closeSelection();if(closed)dev.hoi.client.audio.AudioUiSounds.play("ui.close");return closed;}
    public static boolean scrollAt(double x,double y,double amount){
        MapCommanderCardDrag.cancel();
        if(MapCommanderPanels.scrollAt(x,y,amount))return true;
        if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden()||boxes.stream().noneMatch(box->box.contains(x,y)))return false;
        int delta=amount>0?-1:amount<0?1:0;
        if(delta!=0&&boxes.stream().anyMatch(box->box.pageDelta()==delta)){page+=delta;boxes=List.of();}
        return true;
    }
    public static boolean clickAt(double x,double y,int button,boolean twice){if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden())return false;if(MapCommanderPanels.click(x,y,button,twice))return true;if(button==InputConstants.MOUSE_BUTTON_RIGHT){if(boxes.stream().anyMatch(box->box.action()!=null&&box.contains(x,y)))return true;if(formationAt(x,y))return true;var wings=MapCommanderPanels.attachmentWings();if(!wings.isEmpty())for(var box:boxes)if(box.contains(x,y)&&box.request()!=null&&box.request().mode()==Mode.ASSIGN_ARMY&&view.cards().stream().anyMatch(c->c.id().equals(box.request().target())&&c.role()==Mode.ASSIGN_ARMY)){var values=new ArrayList<String>();values.add("army/"+box.request().target());values.addAll(wings);dev.hoi.client.audio.AudioUiSounds.play("ui.click");open(new Open(Mode.AIR_ATTACH,"",values));return true;}}return button==InputConstants.MOUSE_BUTTON_LEFT?clickAt(x,y):boxes.stream().anyMatch(box->box.contains(x,y));}
    public static boolean clickAt(double x,double y){if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden())return false;if(MapCommanderPanels.click(x,y))return true;for(var box:boxes)if(box.contains(x,y)){dev.hoi.client.audio.AudioUiSounds.play(box.request()!=null&&(box.request().mode()==Mode.ASSIGN_ARMY||box.request().mode()==Mode.ASSIGN_GROUP_MARSHAL||box.request().mode()==Mode.NAVY_FLEET)?"ui.division.select":box.request()!=null&&box.request().mode()==Mode.ARMY_TOOLBAR?"ui.battleplan":"ui.click");if(box.action()!=null){box.action().run();}else if(box.pageDelta()!=0){page+=box.pageDelta();boxes=List.of();}else {selectedCard=box.request().target();if(view.cards().stream().anyMatch(c->c.id().equals(selectedCard))&&(box.request().mode()==Mode.ASSIGN_ARMY||box.request().mode()==Mode.ASSIGN_GROUP_MARSHAL||box.request().mode()==Mode.NAVY_FLEET))MapCommanderPanels.select(selectedCard);else open(box.request());}return true;}return false;}
    static String countLabel(Card card){return card.role()==Mode.ARMY_GROUP||hasCommander(card)?card.count()+"/"+card.capacity():Integer.toString(card.count());}
    private static boolean hasCommander(Card card){return card.panel()!=null?!card.panel().commander().isEmpty():!card.portrait().equals("politics/empty/leader");}
    static String divisionCountLabel(Card card){if(card.role()!=Mode.ARMY_GROUP)return Integer.toString(card.count());var panel=card.panel();if(panel==null)return "?";if(!panel.divisionTypes().isEmpty())return Integer.toString(panel.divisionTypes().values().stream().mapToInt(Integer::intValue).sum());return panel.divisionIds()==null?"?":Integer.toString(panel.divisionIds().size());}
    private static boolean armyCount(Card card){return card.role()==Mode.ASSIGN_ARMY||card.role()==Mode.ARMY_GROUP;}
    static int countColor(Card card){return (card.role()==Mode.ARMY_GROUP||hasCommander(card))&&card.count()>card.capacity()?0xFFE15B5B:armyCount(card)||!hasCommander(card)?UiMenuStyle.TEXT:0xFF83C64F;}
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
        var units=MapCommanderLayout.units(g.guiWidth(),g.guiHeight());
        boolean navalBranch=view.branch()==Mode.NAVY_HQ;
        int cardWidth=units.size(42),cardHeight=units.size(58.5),footHeight=units.size(14),pitch=units.size(45);
        int creationSlots=navalBranch?1:2,airSpace=view.branch()==Mode.AIR?units.size(23):0;
        int columns=Math.max(2,(g.guiWidth()-units.size(12)-airSpace)/pitch-creationSlots);
        boolean paged=groups.pages(view,columns).size()>1;
        if(paged)columns=Math.max(2,(g.guiWidth()-units.size(72)-airSpace)/pitch-creationSlots);
        int capacity=columns,contentWidth=g.guiWidth()-(paged?units.size(60):0)-airSpace;
        var pages=groups.pages(view,capacity);int lastPage=pages.size()-1;page=Math.clamp(page,0,lastPage);var visible=MapCommanderCardDrag.preview(pages.get(page));
        var selected=MapUnitHud.selectedKeys().stream().sorted().toList();
        int shown=visible.size();
        int[] offsets=new int[shown+1];for(int i=0;i<shown;i++){boolean group=visible.get(i).role()==Mode.ARMY_GROUP,nextGroup=i+1<shown&&visible.get(i+1).role()==Mode.ARMY_GROUP;offsets[i+1]=offsets[i]+units.size(group?45:42)-units.size(group?2:.5)-units.size(nextGroup?2:.5);}
        int bottom=MapCommanderLayout.cardsBottom(units),y=bottom-cardHeight;
        int stripX=Math.max(units.size(6),(contentWidth-offsets[shown]-creationSlots*pitch)/2),ordinaryX=stripX+pitch-units.size(2)-units.size(shown>0&&visible.getFirst().role()==Mode.ARMY_GROUP?2:.5);
        var list=new ArrayList<Box>();var drawnBands=new ArrayList<Band>();
        for(int first=0;first<shown;){String owner=groups.owner(visible.get(first),view);int end=first+1;if(owner.isEmpty()){first=end;continue;}while(end<shown&&groups.owner(visible.get(end),view).equals(owner))end++;
            int sx=ordinaryX+offsets[first],sy=bottom-units.size(76),sw=offsets[end]-offsets[first],sh=units.size(76),color=groups.color(owner,view);
            drawnBands.add(new Band(owner,sx,sy,sw,sh,color));first=end;
        }
        bands=List.copyOf(drawnBands);
        if(view.branch()==Mode.ARMY)for(int i=0;i<shown;i++){
            var card=visible.get(i);String owner=groups.owner(card,view);if(owner.isEmpty())continue;
            boolean group=card.role()==Mode.ARMY_GROUP,last=i+1==shown||!groups.owner(visible.get(i+1),view).equals(owner);
            int bx=MapCommanderCardDrag.x(card,ordinaryX+offsets[i]),by=bottom-units.size(79),width=group?units.size(43.75):last?units.size(42):offsets[i+1]-offsets[i];
            String asset="commander/commands/lg_army_"+(group?"group":last?"end":"mid")+"_bg/";
            if(group)g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+asset+"atlas.png"),bx,by,0,0,width,units.size(75.5),175,151,350,151,0xffb29854);
            else UiAssets.drawTinted(g,asset+"frame_0",bx,by,width,units.size(last?75:74.5),0xffb29854);
        }
        for(int i:MapCommanderCardDrag.drawOrder(visible)) {
            var card=visible.get(i);boolean group=card.role()==Mode.ARMY_GROUP;
            boolean naval=view.branch()==Mode.NAVY_HQ,wing=view.branch()==Mode.AIR&&!group&&card.role()!=Mode.ASSIGN_ARMY;
            int cw=units.size(group?45:42),ch=units.size(group?67:62.5),bx=MapCommanderCardDrag.x(card,ordinaryX+offsets[i]),by=bottom-ch;
            String art="commander/hud/"+(group?"group":"army");
            boolean hover=mx>=bx&&mx<bx+cw&&my>=by&&my<by+ch;
            int innerX=bx+units.size(group?4.5:4),innerY=by+units.size(3),innerWidth=units.size(group?35:30.5),innerHeight=units.size(group?48:41.5);
            if(!wing)UiAssets.draw(g,art+(hover?"_hover/frame_0":"_background/frame_0"),innerX,innerY,innerWidth,innerHeight);
            String portrait=card.portrait().equals("politics/empty/leader")?(wing?"unit_hud/air_fighter":art+"_unknown/frame_0"):card.portrait();
            UiAssets.drawContent(g,portrait,innerX,innerY,innerWidth,innerHeight);
            if(!wing)UiAssets.draw(g,naval?"commander/hud/navy_frame/frame_"+(MapCommanderPanels.selected(card.id())||hover?1:0):MapCommanderPanels.selected(card.id())?"commander/commands/leader_"+(group?"army_group":"group")+"_frame_selected/frame_1":art+"_frame/frame_0",bx,by,cw,ch);
            if(!naval&&!wing&&!group&&card.panel()!=null){Integer color=card.panel().armyColor();if(color!=null)UiAssets.drawTinted(g,"commander/commands/group_marker2/frame_0",bx+units.size(1.5),by+units.size(18),units.size(36.5),units.size(11.5),0xff000000|color);}
            if(!naval&&!wing)UiAssets.draw(g,"commander/commands/execute_stop_small_bg/frame_0",bx+units.size(group?4.5:2.5),by-units.size(group?11:9),units.size(38),units.size(13));
            if(!naval&&!wing&&card.panel()!=null)cardOrders(g,units,list,card,bx,by,group);
            var attached=view.wings().stream().filter(wingMember->wingMember.data()!=null&&wingMember.data().attachedArmy().equals(card.id())).toList();
            if(!attached.isEmpty()){
                int ay=by-units.size(32),ah=units.size(14);UiAssets.draw(g,"commander/commands/army_select_wings_btn/frame_0",bx,ay,cw,ah);UiAssets.drawContent(g,"unit_hud/air_fighter",bx+units.size(2),ay+units.size(2),units.size(16),units.size(10));
                UiText.centeredSingleLine(g,client.font,net.minecraft.network.chat.Component.literal(Integer.toString(attached.stream().mapToInt(wingMember->wingMember.data().quantity()).sum())),bx+units.size(18),ay,cw-units.size(19),ah,UiMenuStyle.TEXT);
                list.add(new Box(bx,ay,cw,ah,null,0,UiCommandLabels.text("SELECT_ATTACHED_AIRWINGS","부속된 비행단 선택"),()->MapCommanderPanels.showWings(attached.stream().map(Member::id).toList())));
            }
            UiText.cell(g,client.font,countLabel(card),bx+units.size(group?7:4),by+ch-units.size(16),cw-units.size(group?14:8),Math.max(client.font.lineHeight,units.size(10)),countColor(card),false);
            if(!naval&&!wing&&card.panel()!=null)MapCommanderPanels.progress(g,"plan_unit_power",bx+units.size(group?8:5),by+ch-units.size(6.5),units.size(29.5),units.size(3),card.panel().planPower()!=null&&card.panel().planPower()>0?card.panel().planPower():null,0xff008000,0xff020305);
            if(!naval&&!wing)UiAssets.draw(g,"commander/commands/plan_frame/frame_0",bx+units.size(group?6.5:3.5),by+ch-units.size(6.5),units.size(32.5),units.size(4.5));
            if(!naval&&!wing&&card.panel()!=null){
                int sx=bx+units.size(group?.5:1.5),sy=by+units.size(group?19.5:18),sw=units.size(group?42.5:36.5),sh=units.size(group?14.5:11.5);
                String tip="집단군 색상 · "+(card.panel().color()==null?"미지정":String.format(Locale.ROOT,"#%06X",card.panel().color()));
                if(group&&MapCommanderPanels.selected(card.id())&&!card.panel().palette().isEmpty())list.add(new Box(sx,sy,units.size(3.5),sh,null,0,tip+"\n"+UiCommandLabels.text("SELECT_COLOR","지도 색상 선택"),()->{paletteGroup=paletteGroup.equals(card.id())?"":card.id();boxes=List.of();}));
            }
            if(!wing&&!group){
                int controlX=bx+units.size(31.5),closeY=by+units.size(2.5),controlWidth=units.size(naval?9:8),controlHeight=units.size(naval?9:8);
                float pixelShift=1f/(float)client.getWindow().getGuiScale();g.pose().pushMatrix();g.pose().translate(-pixelShift,pixelShift);UiAssets.draw(g,"commander/commands/lg_disband_group_btn/frame_0",controlX,closeY,controlWidth,controlHeight);g.pose().popMatrix();
                String closeTip=naval?"선택 해제 · "+card.name():UiCommandLabels.text("UNITS_VIEW_BTN_DISBAND_GROUP","")+"\n"+UiCommandLabels.text("UNITS_VIEW_BTN_DISBAND_GROUP_TOOLTIP","");
                list.add(new Box(controlX,closeY,controlWidth,controlHeight,null,0,closeTip,()->{if(naval)MapCommanderPanels.closeCard(card);else open(new Open(Mode.ARMY_TOOLBAR,"DISSOLVE_ARMY",List.of(card.id())));},0,pixelShift));
            }
            if(group){
                boolean expanded=groups.expanded(card.id());int controlX=bx+units.size(36),foldY=by+units.size(3),controlWidth=units.size(7),controlHeight=units.size(10);
                UiAssets.draw(g,"commander/commands/lg_expand_group_btn/frame_"+(expanded?1:0),controlX,foldY,controlWidth,controlHeight);
                String tip=group?UiCommandLabels.text(expanded?"CLICK_TO_HIDE_ARMIES":"CLICK_TO_SHOW_ARMIES",expanded?"소속 군 접기":"소속 군 펼치기"):(expanded?"소속 부대 목록 접기":"소속 부대 목록 펼치기");
                list.add(new Box(controlX,foldY,controlWidth,controlHeight,null,0,tip,()->{groups.toggle(card);boxes=List.of();}));
            }
            var mode=card.role()==Mode.ARMY_GROUP?Mode.ASSIGN_GROUP_MARSHAL:(view.branch()==Mode.ARMY||card.role()==Mode.ASSIGN_ARMY)?Mode.ASSIGN_ARMY:view.branch()==Mode.NAVY_HQ?Mode.NAVY_FLEET:view.branch();
            list.add(new Box(bx,by,cw,ch-footHeight,new Open(mode,card.id()),0,(view.branch()==Mode.ARMY?cardTooltip(card):card.name()+"\n"+card.leader()+"\n"+card.detail())+(mode==Mode.ASSIGN_ARMY&&!MapCommanderPanels.attachmentWings().isEmpty()?"\n\n"+UiCommandLabels.text("ATTACH_AIRWING_TO_GROUP_TOOLTIP","우클릭하여 비행단을 군에 배치"):""),null,units.size(group?2:.5)));
            var membership=card.role()==Mode.ARMY_GROUP?new Open(Mode.ARMY_GROUP,card.id()):(view.branch()==Mode.ARMY||card.role()==Mode.ASSIGN_ARMY)&&!selected.isEmpty()?new Open(Mode.MOVE_DIVISIONS,card.id(),selected)
                    :view.branch()==Mode.NAVY_HQ?new Open(Mode.NAVAL_TASK_FORCES,card.id())
                    :view.branch()==Mode.AIR&&card.role()!=Mode.ASSIGN_ARMY?new Open(Mode.AIR_MISSION,card.id()):new Open(Mode.ARMY_PLANS,card.id());
            list.add(new Box(bx,by+ch-footHeight,cw,footHeight,membership,0,view.branch()==Mode.ARMY?cardTooltip(card):membership.mode()==Mode.MOVE_DIVISIONS?"선택한 자국 사단을 이 군으로 이동":view.branch()==Mode.NAVY_HQ?"태스크 포스 임무 선택":view.branch()==Mode.AIR?"비행단 임무 선택":"전투 계획 · "+card.name()+"\n"+card.leader()+"\n"+card.detail(),null,units.size(group?2:.5)));
        }
        int addX=ordinaryX+offsets[shown];
        if(!navalBranch) {
            int groupWidth=units.size(45),groupHeight=units.size(63),groupY=bottom-groupHeight;
            UiAssets.draw(g,"commander/hud/group_background/frame_0",stripX+units.size(4.5),groupY+units.size(3),units.size(35),units.size(48));
            UiAssets.draw(g,"commander/hud/group_frame/frame_0",stripX,groupY,groupWidth,groupHeight);
            list.add(new Box(stripX,groupY,groupWidth,groupHeight,new Open(Mode.ARMY_GROUPS,""),creationTooltip(true)));
            UiAssets.draw(g,"commander/hud/army_background/frame_0",addX+units.size(4),y+units.size(3),units.size(30.5),units.size(41.5));
            UiAssets.draw(g,"commander/hud/army_border/frame_0",addX,y,units.size(39.5),units.size(55.5));
            list.add(new Box(addX,y,cardWidth,cardHeight,new Open(Mode.CREATE_ARMY,"",selected),creationTooltip(false)));
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
        var colorCard=view.cards().stream().filter(c->c.id().equals(paletteGroup)&&c.panel()!=null&&MapCommanderPanels.selected(c.id())).findFirst().orElse(null);
        if(colorCard!=null&&!colorCard.panel().palette().isEmpty()){
            int cell=units.size(14),pw=colorCard.panel().palette().size()*cell,px=Math.max(units.size(4),(g.guiWidth()-pw)/2),py=MapCommanderLayout.popupBottom(units,false)-cell-units.size(5);g.fill(px-units.size(3),py-units.size(3),px+pw+units.size(3),py+cell+units.size(3),0xee101418);
            for(int index=0;index<colorCard.panel().palette().size();index++){int rgb=colorCard.panel().palette().get(index),cx=px+index*cell;g.fill(cx+1,py+1,cx+cell-1,py+cell-1,0xff000000|rgb);list.add(0,new Box(cx,py,cell,cell,null,0,(colorCard.role()==Mode.ASSIGN_ARMY?"군":"집단군")+" 색상 #"+String.format(Locale.ROOT,"%06X",rgb),()->{MapCommanderOrders.send(colorCard.role()==Mode.ASSIGN_ARMY?"ARMY_COLOR":"GROUP_COLOR",List.of(colorCard.id(),"rgb/"+rgb),List.of());paletteGroup="";boxes=List.of();}));}
        }
        boxes=List.copyOf(list);
        MapCommanderPanels.draw(g,sidebar,mx,my,list.stream().mapToInt(Box::y).min().orElse(y));
        MapCommanderOrders.drawMessage(g);
        String tooltip=MapCommanderPanels.tooltipAt(mx,my);int tooltipWidth=MapCommanderLayout.units(g.guiWidth(),g.guiHeight()).size(320);
        if(tooltip.isEmpty()){tooltipWidth=240;for(var box:boxes)if(box.contains(mx,my)){tooltip=box.tip();break;}}
        if(!tooltip.isEmpty()){
            var lines=UiTooltips.sourceLines(client.font,tooltip,Math.min(g.guiWidth(),tooltipWidth+24));int textWidth=lines.stream().mapToInt(client.font::width).max().orElse(0),lineHeight=client.font.lineHeight+1,textHeight=lines.size()*lineHeight;var position=tooltipPosition(g.guiWidth(),g.guiHeight(),mx,my,textWidth,textHeight);
            g.nextStratum();UiTooltips.background(g,position.x()-7,position.y()-7,textWidth+14,textHeight+14);for(int n=0;n<lines.size();n++)g.text(client.font,lines.get(n),position.x(),position.y()+n*lineHeight,0xFFE0E0E0);renderedTooltip=tooltip;
        }
        String hover=MapCommanderPanels.hoverKey(mx,my);if(hover.isEmpty())for(var box:boxes)if(box.contains(mx,my)){hover="hud/"+box.x()+"/"+box.y()+"/"+box.width()+"/"+box.height();break;}if(updateHover(hover))dev.hoi.client.audio.AudioUiSounds.play("ui.menu_over");
    }
    private static String creationTooltip(boolean group){String prefix=group?"CREATE_ARMY_GROUP":"CREATE_GROUP";boolean empty=group?MapCommanderPanels.formationArmies().isEmpty():MapCommanderPanels.commandSelection().isEmpty();return UiCommandLabels.text(prefix+(empty?"_WITH_NO_UNIT_SELECTED_TOOLTIP":"_WITH_UNITS_SELECTED_TOOLTIP"),"")+"\n\n§Y"+UiCommandLabels.text(group?"OPERATION_NEW_ARMY_GROUP":"OPERATION_NEW_GROUP","")+"§!\n"+UiCommandLabels.text(group?"COMMAND_ARMY_GROUP_DESC":"COMMAND_GROUP_DESC","");}
}
