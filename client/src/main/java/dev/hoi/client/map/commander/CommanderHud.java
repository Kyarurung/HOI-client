package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.map.naval.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class CommanderHud {
    private static View view;
    static final CommanderGroups groups=new CommanderGroups();
    static final CommanderTheaterSelection theaters=new CommanderTheaterSelection();
    static void theaterChanged(){page=0;boxes=List.of();CommanderCardDrag.cancel();}
    record Band(String owner,int x,int y,int width,int height,int color) {}
    private static List<Band> bands=List.of();
    static List<Band> bands(){return bands;}
    static java.util.function.Consumer<Open> requestObserver;
    private static int page;
    private static String paletteGroup="";
    private static String selectedCard="",hovered="",renderedTooltip="";
    public static String renderedTooltip(){return renderedTooltip;}
    public record TooltipBounds(int x,int y,int width,int height) {}
    private static TooltipBounds tooltipBounds;
    public static TooltipBounds tooltipBounds(){return tooltipBounds;}
    private static org.joml.Vector2ic tooltipPosition(int width,int height,int mouseX,int mouseY,int textWidth,int textHeight){
        var original=Tooltips.position(width,height,mouseX,mouseY,textWidth,textHeight);
        int x=original.x(),y=original.y();
        tooltipBounds=new TooltipBounds(x-7,y-7,textWidth+14,textHeight+14);return new org.joml.Vector2i(x,y);
    }
    public static record Box(int x,int y,int width,int height,Open request,int pageDelta,String tip,Runnable action,int horizontalInset,float pixelShift){
        Box(int x,int y,int width,int height,Open request,int pageDelta,String tip,Runnable action,int horizontalInset){this(x,y,width,height,request,pageDelta,tip,action,horizontalInset,0);}
        Box(int x,int y,int width,int height,Open request,int pageDelta,String tip,Runnable action){this(x,y,width,height,request,pageDelta,tip,action,0);}
        Box(int x,int y,int width,int height,Open request,int pageDelta,String tip){this(x,y,width,height,request,pageDelta,tip,null);}
        Box(int x,int y,int width,int height,Open request,String tip){this(x,y,width,height,request,0,tip);}
        boolean contains(double mx,double my){return mx>=x+horizontalInset-pixelShift&&mx<x+width-horizontalInset-pixelShift&&my>=y+pixelShift&&my<y+height+pixelShift;}
    }
    static String cardTooltip(Card card){
        String tip=UiCommandLabels.text("SELECT_GROUP_TOOLTIP","");
        if(CommanderUnitHud.selectedKeys().stream().anyMatch(id->id.startsWith("unit/")))tip+="\n"+UiCommandLabels.text("SELECT_GROUP_WITH_UNITS_SELECTED_TOOLTIP","");
        tip+="\n§H"+card.name()+"§!";
        if(card.panel()!=null&&!card.panel().divisionTypes().isEmpty()){tip+="\n";for(var entry:card.panel().divisionTypes().entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).toList()){String asset=CommanderPanels.armyUnitIcon(entry.getKey());String sprite=asset.replace("commander/commands/","").replace("/frame_0","").replace('/','_');tip+="£"+sprite+"£ "+entry.getValue()+"  ";}}
        tip+="\n\n"+UiCommandLabels.text(card.role()==Mode.ARMY_GROUP?"COMMAND_ARMY_GROUP_DESC":"COMMAND_GROUP_DESC","");
        if(!card.tooltip().isEmpty())tip+="\n"+card.tooltip();
        return tip;
    }
    private static void cardOrders(GuiGraphicsExtractor g,UiUnits units,List<Box> list,Card card,int bx,int by,boolean group){
        var controls=card.panel().controls();var defense=controls.stream().filter(c->c.id().equals("DEFENDING_AREA")).findFirst().orElse(null);
        var training=controls.stream().filter(c->c.id().equals("STOP_EXERCISE")).findFirst().orElse(null);
        int ey=by-units.size(group?9:8);
        if(defense!=null){int x=bx+units.size(30.5);UiAssets.draw(g,"commander/commands/area_defense_active/frame_0",x,ey+units.size(1),units.size(8),units.size(9.5));list.add(new Box(x,ey,units.size(8),units.size(11.5),null,0,defense.tooltip(),()->{}));return;}
        boolean execution=controls.stream().anyMatch(c->c.id().equals("ACTIVATE")||c.id().equals("DEACTIVATE"));
        if(training==null&&!execution&&!NavalInvasionOrders.available(card.id()))return;
        for(boolean stop:List.of(true,false)){
            int ex=bx+units.size(group?(stop?5.5:20.5):(stop?3.5:18.5)),ew=units.size(stop?15:21.5),eh=units.size(13);
            String action=training!=null?"STOP_EXERCISE":stop?"DEACTIVATE":"ACTIVATE";
            var control=controls.stream().filter(c->c.id().equals(action)).findFirst().orElse(null);
            if(training!=null&&!stop){UiAssets.draw(g,"commander/commands/exercise_icon_anim_strip/frame_0",ex,ey,units.size(12.5),units.size(12.5));list.add(new Box(ex,ey,ew,eh,null,0,UiCommandLabels.text("UNITS_VIEW_EXERCISING",""),()->{}));continue;}
            String icon="commander/commands/execute_button_"+(stop?"stop":"play")+"_small"+(group?"_ag":"")+"/frame_0";
            String reason=CommanderAvailability.reason("ARMY/"+action,List.of(card.id()));boolean enabled=control!=null&&control.enabled()&&reason.isEmpty();
            boolean pointable=training==null&&CommanderPanels.shift()&&NavalInvasionOrders.available(card.id());
            if(pointable){enabled=true;reason="";}
            if(enabled)UiAssets.draw(g,icon,ex,ey,ew,eh);else UiAssets.drawGrayscale(g,icon,ex,ey,ew,eh);
            list.add(new Box(ex,ey,ew,eh,enabled?new Open(Mode.ARMY_TOOLBAR,action,List.of(card.id())):null,0,CommanderAvailability.explain(pointable?UiCommandLabels.text("UNITS_VIEW_BTN_EXECUTE_POINT_OUT",""):control==null?"":control.tooltip(),reason),enabled?null:()->{}));
        }
        if(training==null)UiAssets.draw(g,"commander/commands/plan_value_strip/frame_"+(card.panel().planEvaluation()==null?0:card.panel().planEvaluation()+1),bx+units.size(group?22:19),ey+units.size(.5),units.size(8),units.size(11.5));
    }
    public static List<Box> boxes(){return boxes;}
    static void togglePalette(String id){paletteGroup=paletteGroup.equals(id)?"":id;boxes=List.of();}
    private static List<Box> boxes=List.of();
    private CommanderHud() {}
    static boolean running(){return view!=null&&Boolean.TRUE.equals(view.running());}
    static boolean hidesVanillaHud(){var client=Minecraft.getInstance();return view!=null&&client.level!=null&&!client.gui.hud.isHidden()&&view.country().equals(ClientCampaignHud.country())&&view.dimension().equals(client.level.dimension().identifier().toString());}
    public static boolean cardsVisible(){var client=Minecraft.getInstance();var screen=client.gui.screen();return view!=null&&client.level!=null&&!client.gui.hud.isHidden()&&view.country().equals(ClientCampaignHud.country())&&view.dimension().equals(client.level.dimension().identifier().toString())&&(screen==null||screen instanceof InputSidebarMovement.InputMovementScreen sidebar&&sidebar.allowsMovement());}
    public static void register(){
        CommanderHudPayloads.registerPayloadTypes();CommanderOrders.register();NavalSeaHover.register();
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE,(packet,context)->{try{accept(packet.view());}catch(IllegalArgumentException error){reset();}});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(CommanderHud::reset));
        ClientTickEvents.END_CLIENT_TICK.register(client->{NavalSeaHover.tick();if(view!=null&&(client.level==null||!view.dimension().equals(client.level.dimension().identifier().toString())||!view.country().equals(ClientCampaignHud.country())))reset();});
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,Identifier.parse("hoi:commander_hud"),(g,delta)->{if(Minecraft.getInstance().gui.screen()==null)draw(g,0,g.guiWidth()/2,g.guiHeight()/2);});
        ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{if(screen instanceof InputSidebarMovement.InputMovementScreen sidebar){if(screen instanceof dev.hoi.client.screen.commander.CommanderTraits)ScreenEvents.beforeExtract(screen).register((s,g,mx,my,delta)->{if(sidebar.allowsMovement()){draw(g,sidebar.unitHudLeft(),-1,-1);g.nextStratum();}});else ScreenEvents.afterExtract(screen).register((s,g,mx,my,delta)->{if(sidebar.allowsMovement())draw(g,sidebar.unitHudLeft(),mx,my);});}});
    }
    public static void accept(View received){var next=visibleCards(received);var client=Minecraft.getInstance();if(next.country().isEmpty()||client.level==null||!next.dimension().equals(client.level.dimension().identifier().toString())||!next.country().equals(ClientCampaignHud.country())){reset();return;}if(view==null||!view.country().equals(next.country())||!view.dimension().equals(next.dimension())||view.branch()!=next.branch()){NavalSeaHover.reset();CommanderTheaters.reset();page=0;paletteGroup="";}if(next.cards().stream().noneMatch(c->c.id().equals(paletteGroup)))paletteGroup="";CommanderCardDrag.accept(next);groups.accept(next);theaters.selected(next);view=next;CommanderPanels.accept(next);if(next.cards().stream().noneMatch(card->CommanderPanels.selected(card.id())))selectedCard="";boxes=List.of();}
    static View visibleCards(View view){var cards=view.cards().stream().filter(card->card.role()!=Mode.ASSIGN_ARMY||card.count()>0).toList();return new View(view.country(),view.dimension(),view.branch(),cards,view.wings(),view.running(),view.theaters(),view.armyTheaters(),view.orderToken(),view.orderRevision());}
    public static void reset(){theaters.clear();NavalSeaHover.reset();CommanderTheaters.reset();CommanderCardDrag.reset();paletteGroup="";groups.clear();bands=List.of();tooltipBounds=null;renderedTooltip="";hovered="";CommanderOrders.reset();CommanderPanels.reset();selectedCard="";view=null;boxes=List.of();page=0;}
    public static void open(Open request){if(request.mode()==Mode.AIR_ATTACH){CommanderOrders.route(request);return;}if(requestObserver!=null){requestObserver.accept(request);return;}if(CommanderOrders.route(request))return;if(ClientPlayNetworking.canSend(Open.TYPE))ClientPlayNetworking.send(request);}
    public static boolean pressAt(double x,double y,int button,boolean twice){if(AgencyHud.active())return AgencyHud.clickAt(x,y,button);if(button==InputConstants.MOUSE_BUTTON_LEFT&&view!=null&&!Minecraft.getInstance().gui.hud.isHidden()&&view.country().equals(ClientCampaignHud.country())&&(CommanderPanels.pressScroll(x,y)||CommanderCardDrag.begin(view,boxes,x,y)))return true;return clickAt(x,y,button,twice);}
    public static boolean dragAt(double x,double y){return CommanderPanels.dragScroll(y)||CommanderCardDrag.move(x);}
    public static boolean dragAt(double x){return CommanderCardDrag.move(x);}
    public static boolean releaseAt(double x,int button){return button==InputConstants.MOUSE_BUTTON_LEFT&&(CommanderPanels.releaseScroll()||CommanderCardDrag.release(x));}
    public static void cancelDrag(){CommanderCardDrag.cancel();}
    static void reconcileCardOrder(){if(view!=null)CommanderCardDrag.reconcile(view);}
    public static boolean closeSelection(){if(AgencyHud.active())return AgencyHud.deselect();boolean closed=CommanderTheaters.closeSettings()||CommanderPanels.closeSelection();if(closed)dev.hoi.client.audio.AudioUiSounds.play("ui.close");return closed;}
    public static boolean scrollAt(double x,double y,double amount){
        if(AgencyHud.active())return AgencyHud.scrollAt(x,y,amount);
        CommanderCardDrag.cancel();
        if(CommanderPanels.scrollAt(x,y,amount))return true;
        if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden()||boxes.stream().noneMatch(box->box.contains(x,y)))return false;
        int delta=amount>0?-1:amount<0?1:0;
        if(delta!=0&&boxes.stream().anyMatch(box->box.pageDelta()==delta)){page+=delta;boxes=List.of();}
        return true;
    }
    public static boolean clickAt(double x,double y,int button,boolean twice){if(AgencyHud.active())return AgencyHud.clickAt(x,y,button);if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden())return false;if(CommanderPanels.click(x,y,button,twice))return true;if(button==InputConstants.MOUSE_BUTTON_RIGHT){for(var box:boxes)if(box.contains(x,y)&&box.request()!=null&&box.request().mode()==Mode.AIR_ATTACH&&box.request().selection().contains("action/detach")){dev.hoi.client.audio.AudioUiSounds.play("ui.checkbox");open(box.request());return true;}if(boxes.stream().anyMatch(box->box.action()!=null&&box.contains(x,y)))return true;if(formationAt(x,y))return true;var wings=CommanderPanels.attachmentWings();if(!wings.isEmpty())for(var box:boxes)if(box.contains(x,y)&&box.request()!=null&&box.request().mode()==Mode.ASSIGN_ARMY&&view.cards().stream().anyMatch(c->c.id().equals(box.request().target())&&c.role()==Mode.ASSIGN_ARMY)){var values=new ArrayList<String>();values.add("army/"+box.request().target());values.addAll(wings);dev.hoi.client.audio.AudioUiSounds.play("ui.click");open(new Open(Mode.AIR_ATTACH,"",values));return true;}}return button==InputConstants.MOUSE_BUTTON_LEFT?clickAt(x,y):boxes.stream().anyMatch(box->box.contains(x,y));}
    public static boolean clickAt(double x,double y){if(AgencyHud.active())return AgencyHud.clickAt(x,y,InputConstants.MOUSE_BUTTON_LEFT);if(view==null||!view.country().equals(ClientCampaignHud.country())||Minecraft.getInstance().gui.hud.isHidden())return false;if(CommanderPanels.click(x,y))return true;for(var box:boxes)if(box.contains(x,y)){dev.hoi.client.audio.AudioUiSounds.play(box.request()!=null&&box.request().mode()==Mode.AIR_ATTACH||box.tip().equals(UiCommandLabels.text("ATTACH_AIR_WING_DESC",""))||box.tip().equals(UiCommandLabels.text("ATTACH_AIR_WING_SELECT_SOME",""))?"ui.checkbox":box.request()!=null&&(box.request().mode()==Mode.ASSIGN_ARMY||box.request().mode()==Mode.ASSIGN_GROUP_MARSHAL||box.request().mode()==Mode.NAVY_FLEET)?dev.hoi.client.audio.AudioUiSounds.selection(box.request().mode()):box.request()!=null&&box.request().mode()==Mode.ARMY_TOOLBAR?"ui.battleplan":"ui.click");if(box.action()!=null){box.action().run();}else if(box.pageDelta()!=0){page+=box.pageDelta();boxes=List.of();}else {selectedCard=box.request().target();if(view.cards().stream().anyMatch(c->c.id().equals(selectedCard))&&(box.request().mode()==Mode.ASSIGN_ARMY||box.request().mode()==Mode.ASSIGN_GROUP_MARSHAL||box.request().mode()==Mode.NAVY_FLEET))CommanderPanels.select(selectedCard);else open(box.request());}return true;}return false;}
    static String countLabel(Card card){if(card.role()==Mode.NAVY_HQ)return Integer.toString(card.count());return card.role()==Mode.ARMY_GROUP||hasCommander(card)?card.count()+"/"+card.capacity():Integer.toString(card.count());}
    private static boolean hasCommander(Card card){return card.panel()!=null?!card.panel().commander().isEmpty():!card.portrait().equals("politics/empty/leader");}
    static String divisionCountLabel(Card card){if(card.role()!=Mode.ARMY_GROUP)return Integer.toString(card.count());var panel=card.panel();if(panel==null)return "0";if(!panel.divisionTypes().isEmpty())return Integer.toString(panel.divisionTypes().values().stream().mapToInt(Integer::intValue).sum());return panel.divisionIds()==null?"0":Integer.toString(panel.divisionIds().size());}
    private static boolean armyCount(Card card){return card.role()==Mode.ASSIGN_ARMY||card.role()==Mode.ARMY_GROUP;}
    static int countColor(Card card){return (card.role()==Mode.ARMY_GROUP||hasCommander(card))&&card.count()>card.capacity()?0xFFE15B5B:armyCount(card)||card.role()==Mode.NAVY_FLEET||card.role()==Mode.NAVY_HQ||!hasCommander(card)?UiMenuStyle.TEXT:0xFF83C64F;}
    private static boolean formationAt(double x,double y){
        if(view.branch()!=Mode.ARMY||!CommanderPanels.attachmentWings().isEmpty())return false;
        for(var box:boxes)if(box.contains(x,y)&&box.request()!=null){
            var request=box.request();var card=view.cards().stream().filter(c->c.id().equals(request.target())).findFirst().orElse(null);
            if(card!=null&&card.role()==Mode.ASSIGN_ARMY){CommanderOrders.formation("MOVE_DIVISIONS",card.id(),CommanderPanels.commandSelection());return true;}
            if(card!=null&&card.role()==Mode.ARMY_GROUP){CommanderOrders.formation("MOVE_ARMIES",card.id(),CommanderPanels.formationArmies());return true;}
            if(request.mode()==Mode.ARMY_GROUPS){CommanderOrders.formation("CREATE_GROUP","",CommanderPanels.formationArmies());return true;}
            if(request.mode()==Mode.CREATE_ARMY){CommanderOrders.formation("CREATE_ARMY","",CommanderPanels.commandSelection());return true;}
        }
        return false;
    }
    static boolean updateHover(String key){boolean entered=!key.isEmpty()&&!key.equals(hovered);hovered=key;return entered;}
    private static boolean backdropPass;
    static boolean backdropPass(){return backdropPass;}
    public static void drawBackdrop(GuiGraphicsExtractor g){backdropPass=true;try{draw(g,0,-1,-1);}finally{backdropPass=false;}}
    private static void draw(GuiGraphicsExtractor g,int left,int mx,int my){
        if(AgencyHud.active()){boxes=List.of();CommanderCardDrag.cancel();return;}
        tooltipBounds=null;renderedTooltip="";int sidebar=left;left=0;
        boxes=List.of();bands=List.of();var client=Minecraft.getInstance();if(view==null||client.level==null||client.gui.hud.isHidden()||!view.country().equals(ClientCampaignHud.country())||!view.dimension().equals(client.level.dimension().identifier().toString())||left>=g.guiWidth()-65){updateHover("");return;}
        var units=CommanderLayout.units(g.guiWidth(),g.guiHeight());
        boolean navalBranch=view.branch()==Mode.NAVY_HQ;
        int cardWidth=units.size(42),cardHeight=units.size(58.5),footHeight=units.size(14),pitch=units.size(45);
        int creationSlots=navalBranch?1:2;
        int columns=Math.max(2,(g.guiWidth()-units.size(12))/pitch-creationSlots);
        boolean paged=groups.pages(view,columns).size()>1;
        if(paged)columns=Math.max(2,(g.guiWidth()-units.size(72))/pitch-creationSlots);
        int capacity=columns,contentWidth=g.guiWidth()-(paged?units.size(60):0);
        var pages=groups.pages(view,capacity,theaters.cards(view));int lastPage=pages.size()-1;page=Math.clamp(page,0,lastPage);var visible=CommanderCardDrag.preview(pages.get(page));
        var selected=CommanderUnitHud.selectedKeys().stream().sorted().toList();
        int shown=visible.size();
        int[] offsets=new int[shown+1];for(int i=0;i<shown;i++){boolean group=visible.get(i).role()==Mode.ARMY_GROUP,nextGroup=i+1<shown&&visible.get(i+1).role()==Mode.ARMY_GROUP;offsets[i+1]=offsets[i]+units.size(group?45:42)-units.size(group?2:.5)-units.size(nextGroup?2:.5);}
        int bottom=CommanderLayout.cardsBottom(units),navalOffset=navalBranch?units.size(3.5):0,y=bottom-cardHeight+navalOffset;
        int stripX=Math.max(units.size(6),(contentWidth-offsets[shown]-creationSlots*pitch)/2),ordinaryX=stripX+pitch-units.size(2)-units.size(shown>0&&visible.getFirst().role()==Mode.ARMY_GROUP?2:.5);
        var list=new ArrayList<Box>();var drawnBands=new ArrayList<Band>();
        if(navalBranch){g.enableScissor(0,0,g.guiWidth(),bottom);g.pose().pushMatrix();g.pose().translate(0,(float)(3.5*units.scale()-navalOffset));}
        for(int first=0;first<shown;){String owner=groups.owner(visible.get(first),view);int end=first+1;if(owner.isEmpty()){first=end;continue;}while(end<shown&&groups.owner(visible.get(end),view).equals(owner))end++;
            int sx=ordinaryX+offsets[first],sy=bottom-units.size(76),sw=offsets[end]-offsets[first],sh=units.size(76),color=groups.color(owner,view);
            drawnBands.add(new Band(owner,sx,sy,sw,sh,color));first=end;
        }
        bands=List.copyOf(drawnBands);
        if(view.branch()==Mode.ARMY)for(int i=0;i<shown;i++){
            var card=visible.get(i);String owner=groups.owner(card,view);if(owner.isEmpty())continue;
            boolean group=card.role()==Mode.ARMY_GROUP,last=i+1==shown||!groups.owner(visible.get(i+1),view).equals(owner);
            int bx=CommanderCardDrag.x(card,ordinaryX+offsets[i]),by=bottom-units.size(79),width=group?units.size(43.75):last?units.size(42):offsets[i+1]-offsets[i];
            String asset="commander/commands/lg_army_"+(group?"group":last?"end":"mid")+"_bg/";
            if(group)g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+asset+"atlas.png"),bx,by,0,0,width,units.size(75.5),175,151,350,151,0xffb29854);
            else UiAssets.drawTinted(g,asset+"frame_0",bx,by,width,units.size(last?75:74.5),0xffb29854);
        }
        int addX=ordinaryX+offsets[shown];var creationBoxes=new ArrayList<Box>();
        if(!navalBranch) {
            int groupWidth=units.size(45),groupHeight=units.size(63),groupY=bottom-groupHeight;
            UiAssets.draw(g,"commander/hud/group_background/frame_0",stripX+units.size(4.5),groupY+units.size(3),units.size(35),units.size(48));
            UiAssets.draw(g,"commander/hud/group_frame/frame_0",stripX,groupY,groupWidth,groupHeight);
            creationBoxes.add(new Box(stripX,groupY,groupWidth,groupHeight,new Open(Mode.ARMY_GROUPS,""),creationTooltip(true)));
            int armyY=bottom-units.size(62.5);
            g.pose().pushMatrix();g.pose().translate(units.length(4.5)-units.size(4.5),0);
            UiAssets.draw(g,"commander/hud/army_background/frame_0",addX+units.size(4.5),armyY+units.size(5),units.size(30.5),units.size(41.5));
            g.pose().popMatrix();
            UiAssets.draw(g,"commander/hud/army_frame/frame_0",addX,armyY,cardWidth,units.size(62.5));
            creationBoxes.add(new Box(addX,armyY,cardWidth,units.size(62.5),new Open(Mode.CREATE_ARMY,"",selected),creationTooltip(false)));
        } else {
            addX=stripX;
            g.pose().pushMatrix();g.pose().translate(addX,y);g.pose().scale(units.scale()*.5f);UiAssets.draw(g,"commander/hud/army_background/frame_0",9,7,61,83);UiAssets.draw(g,"commander/hud/army_border/frame_0",0,0,79,111);g.pose().popMatrix();
            creationBoxes.add(new Box(addX,y,cardWidth,cardHeight,new Open(Mode.NAVY_HQ,""),"함대 생성·태스크 포스 편입"));
        }
        for(int i:CommanderCardDrag.drawOrder(visible)) {
            var card=visible.get(i);boolean group=card.role()==Mode.ARMY_GROUP;
            boolean naval=view.branch()==Mode.NAVY_HQ,wing=view.branch()==Mode.AIR&&!group&&card.role()!=Mode.ASSIGN_ARMY;
            int cw=units.size(group?45:42),ch=units.size(group?67:naval?58.5:62.5),bx=CommanderCardDrag.x(card,ordinaryX+offsets[i]),by=bottom-ch+navalOffset;
            String art="commander/hud/"+(group?"group":"army");
            boolean hover=mx>=bx&&mx<bx+cw&&my>=by&&my<by+ch;
            int innerX=bx+units.size(naval||wing?(group?4.5:4):group?5:4.5),innerY=by+units.size(naval||wing?3:group?6:5),innerWidth=units.size(group?35:30.5),innerHeight=units.size(naval||wing?(group?48:41.5):group?42.5:41.5);
            g.pose().pushMatrix();
            if(!naval&&!wing&&!group)g.pose().translate(units.length(4.5)-units.size(4.5),0);
            if(!wing)UiAssets.draw(g,art+(hover?"_hover/frame_0":"_background/frame_0"),innerX,innerY,innerWidth,innerHeight);
            String portrait=naval&&card.role()==Mode.NAVY_HQ?"commander/commands/reserve_portrait/frame_0":card.portrait().equals("politics/empty/leader")?(wing?"unit_hud/air_fighter":art+"_unknown/frame_0"):card.portrait();
            if(naval&&card.role()==Mode.NAVY_HQ){g.pose().pushMatrix();g.pose().translate(bx+units.length(4.5),by+units.length(3.5));g.pose().scale(units.scale()*.5f);UiAssets.draw(g,portrait,0,0,68,91);g.pose().popMatrix();}else if(naval||wing)UiAssets.drawContent(g,portrait,innerX,innerY,innerWidth,innerHeight);else if(card.portrait().equals("politics/empty/leader"))UiAssets.cover(g,portrait,innerX,innerY,innerWidth,innerHeight);else UiAssets.drawCropped(g,portrait,innerX,innerY,innerWidth,innerHeight,units.scale()*(group?.225f:.195f));
            g.pose().popMatrix();
            if(!wing)UiAssets.draw(g,naval?(card.role()==Mode.NAVY_HQ?"commander/commands/reserve_frame/frame_":"commander/hud/navy_frame/frame_")+(CommanderPanels.selected(card.id())?1:0):CommanderPanels.selected(card.id())?"commander/commands/leader_"+(group?"army_group":"group")+"_frame_selected/frame_1":art+"_frame/frame_0",bx,by,cw,ch);
            if(!naval&&!wing&&!group&&card.panel()!=null){Integer color=card.panel().armyColor();if(color!=null)UiAssets.drawTinted(g,"commander/commands/group_marker2/frame_0",bx+units.size(1.5),by+units.size(18),units.size(36.5),units.size(11.5),0xff000000|color);}
            if(!naval&&!wing)UiAssets.draw(g,"commander/commands/execute_stop_small_bg/frame_0",bx+units.size(group?4.5:2.5),by-units.size(group?10:9),units.size(38),units.size(13));
            if(!naval&&!wing&&card.panel()!=null)cardOrders(g,units,list,card,bx,by,group);
            var attached=view.wings().stream().filter(wingMember->wingMember.data()!=null&&wingMember.data().attachedArmy().equals(card.id())).toList();
            if(view.branch()==Mode.AIR&&!group&&!wing)airAttachments(g,units,list,card,attached,bx,by);
            UiText.cell(g,client.font,countLabel(card),bx+units.size(naval?3.5:group?7:4),naval?by+units.size(44.5):by+ch-units.size(16),naval?units.size(33):cw-units.size(group?14:8),Math.max(client.font.lineHeight,units.size(naval?10.5:10)),countColor(card),false);
            if(!wing&&card.panel()!=null)for(var control:card.panel().controls())if(Set.of("LEADER_TRAITS","LEADER_ADVISOR").contains(control.id())&&(!control.id().equals("LEADER_TRAITS")||hasCommander(card))){boolean traits=control.id().equals("LEADER_TRAITS");int width=units.size(traits?(control.icon().contains("available_trait")?11:12):14),height=units.size(traits?12.5:14),cx=naval?bx+units.size(traits?5:22.5):traits?innerX:innerX+innerWidth-width,cy=naval?by+units.size(31.5):innerY+innerHeight-height;if(naval)UiAssets.drawLeaderControl(g,control.icon(),cx,cy,width,height,control.enabled());else {int[] bounds=UiAssets.drawLeaderControlAnchored(g,control.icon(),traits?innerX:innerX+innerWidth,innerY+innerHeight,width,height,!traits,control.enabled());cx=bounds[0];cy=bounds[1];width=bounds[2];height=bounds[3];}list.add(new Box(cx,cy,width,height,null,0,control.tooltip(),()->{if(control.enabled())open(control.request());}));}
            if(!naval&&!wing&&card.panel()!=null)CommanderPanels.progress(g,"plan_unit_power",bx+units.size(group?8:5),by+ch-units.size(6.5),units.size(29.5),units.size(3),card.panel().forces(),0xff008000,0xff020305);
            if(!naval&&!wing)UiAssets.draw(g,"commander/commands/plan_frame/frame_0",bx+units.size(group?6.5:3.5),by+ch-units.size(6.5),units.size(32.5),units.size(4.5));
            if(!naval&&!wing&&card.panel()!=null){
                int sx=bx+units.size(group?.5:1.5),sy=by+units.size(group?19.5:18),sh=units.size(group?14.5:11.5);
                String tip="집단군 색상 · "+(card.panel().color()==null?"미지정":String.format(Locale.ROOT,"#%06X",card.panel().color()));
                if(group&&CommanderPanels.selected(card.id())&&!card.panel().palette().isEmpty())list.add(new Box(sx,sy,units.size(3.5),sh,null,0,tip+"\n"+UiCommandLabels.text("SELECT_COLOR","지도 색상 선택"),()->{paletteGroup=paletteGroup.equals(card.id())?"":card.id();boxes=List.of();}));
            }
            if(!wing&&!group&&card.role()!=Mode.NAVY_HQ){
                int controlX=bx+units.size(31.5),closeY=by+units.size(3.5),controlWidth=units.size(naval?9:8),controlHeight=units.size(naval?9:8);
                float pixelShift=1f/(float)client.getWindow().getGuiScale();g.pose().pushMatrix();g.pose().translate(-pixelShift,pixelShift);UiAssets.draw(g,"commander/commands/lg_disband_group_btn/frame_0",controlX,closeY,units.size(8),units.size(8));g.pose().popMatrix();
                String closeTip=naval?"선택 해제 · "+card.name():UiCommandLabels.text("UNITS_VIEW_BTN_DISBAND_GROUP","")+"\n"+UiCommandLabels.text("UNITS_VIEW_BTN_DISBAND_GROUP_TOOLTIP","");
                list.add(new Box(controlX,closeY,controlWidth,controlHeight,null,0,closeTip,()->{if(naval)CommanderPanels.closeCard(card);else CommanderOrders.formation("DISSOLVE_ARMY","",List.of(card.id()));},0,pixelShift));
            }
            if(group){
                boolean expanded=groups.expanded(card.id());int controlX=bx+units.size(36),foldY=by+units.size(4),controlWidth=units.size(7),controlHeight=units.size(10);
                UiAssets.draw(g,"commander/commands/lg_expand_group_btn/frame_"+(expanded?1:0),controlX,foldY,controlWidth,controlHeight);
                String tip=group?UiCommandLabels.text(expanded?"CLICK_TO_HIDE_ARMIES":"CLICK_TO_SHOW_ARMIES",expanded?"소속 군 접기":"소속 군 펼치기"):(expanded?"소속 부대 목록 접기":"소속 부대 목록 펼치기");
                list.add(new Box(controlX,foldY,controlWidth,controlHeight,null,0,tip,()->{groups.toggle(card);boxes=List.of();}));
            }
            var mode=card.role()==Mode.ARMY_GROUP?Mode.ASSIGN_GROUP_MARSHAL:(view.branch()==Mode.ARMY||card.role()==Mode.ASSIGN_ARMY)?Mode.ASSIGN_ARMY:view.branch()==Mode.NAVY_HQ?Mode.NAVY_FLEET:view.branch();
            list.add(new Box(bx,by,cw,ch-footHeight,new Open(mode,card.id()),0,(view.branch()==Mode.ARMY?cardTooltip(card):(card.panel()!=null&&card.panel().fleet()!=null&&card.panel().fleet().reserve()?card.panel().fleet().reserveTooltip():card.name()+"\n"+card.leader()+"\n"+card.detail()))+(mode==Mode.ASSIGN_ARMY&&!CommanderPanels.attachmentWings().isEmpty()?"\n\n"+UiCommandLabels.text("ATTACH_AIRWING_TO_GROUP_TOOLTIP","우클릭하여 비행단을 군에 배치"):""),null,units.size(group?2:.5)));
            var membership=view.branch()==Mode.AIR&&!wing?new Open(mode,card.id()):card.role()==Mode.ARMY_GROUP?new Open(Mode.ARMY_GROUP,card.id()):(view.branch()==Mode.ARMY||card.role()==Mode.ASSIGN_ARMY)&&!selected.isEmpty()?new Open(Mode.MOVE_DIVISIONS,card.id(),selected)
                    :view.branch()==Mode.NAVY_HQ?new Open(Mode.NAVAL_TASK_FORCES,card.id())
                    :view.branch()==Mode.AIR&&card.role()!=Mode.ASSIGN_ARMY?new Open(Mode.AIR_MISSION,card.id()):new Open(Mode.ARMY_PLANS,card.id());
            list.add(new Box(bx,by+ch-footHeight,cw,footHeight,membership,0,!naval&&!wing&&card.panel()!=null&&mx>=bx+units.size(group?8:5)&&mx<bx+units.size(group?8:5)+units.size(29.5)&&my>=by+ch-units.size(6.5)&&my<by+ch-units.size(2)?card.panel().tooltips().getOrDefault("forces",""):view.branch()==Mode.ARMY?cardTooltip(card):membership.mode()==Mode.MOVE_DIVISIONS?"선택한 자국 사단을 이 군으로 이동":view.branch()==Mode.NAVY_HQ?"태스크 포스 임무 선택":view.branch()==Mode.AIR?cardTooltip(card):"전투 계획 · "+card.name()+"\n"+card.leader()+"\n"+card.detail(),null,units.size(group?2:.5)));
        }
        list.addAll(creationBoxes);
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
        if(navalBranch){g.pose().popMatrix();g.disableScissor();list.replaceAll(box->new Box(box.x(),box.y(),box.width(),Math.min(box.height(),bottom-box.y()),box.request(),box.pageDelta(),box.tip(),box.action(),box.horizontalInset(),box.pixelShift()));}
        var colorCard=view.cards().stream().filter(c->c.id().equals(paletteGroup)&&c.panel()!=null&&CommanderPanels.selected(c.id())).findFirst().orElse(null);
        if(colorCard!=null&&!colorCard.panel().palette().isEmpty()){
            int cell=units.size(14),pw=colorCard.panel().palette().size()*cell,px=Math.max(units.size(4),(g.guiWidth()-pw)/2),py=CommanderLayout.popupBottom(units,false)-cell-units.size(5);g.fill(px-units.size(3),py-units.size(3),px+pw+units.size(3),py+cell+units.size(3),0xee101418);
            for(int index=0;index<colorCard.panel().palette().size();index++){int rgb=colorCard.panel().palette().get(index),cx=px+index*cell;g.fill(cx+1,py+1,cx+cell-1,py+cell-1,0xff000000|rgb);list.add(0,new Box(cx,py,cell,cell,null,0,(navalBranch?"함대":colorCard.role()==Mode.ASSIGN_ARMY?"군":"집단군")+" 색상 #"+String.format(Locale.ROOT,"%06X",rgb),()->{CommanderOrders.send(navalBranch?"FLEET_COLOR":colorCard.role()==Mode.ASSIGN_ARMY?"ARMY_COLOR":"GROUP_COLOR",List.of(colorCard.id(),"rgb/"+rgb),List.of());paletteGroup="";boxes=List.of();}));}
        }
        boxes=List.copyOf(list);
        CommanderPanels.draw(g,sidebar,mx,my,list.stream().mapToInt(Box::y).min().orElse(y));
        String tooltip=CommanderPanels.tooltipAt(mx,my);
        if(tooltip.isEmpty()){for(var box:boxes)if(box.contains(mx,my)){tooltip=box.tip();break;}}
        NavalSeaHover.update(view,mx,my,g.guiWidth(),g.guiHeight(),mx>=sidebar&&my>=HudMenuBar.height(g.guiWidth())&&boxes.stream().noneMatch(b->b.contains(mx,my))&&CommanderPanels.hits().stream().noneMatch(h->h.contains(mx,my)));
        if(tooltip.isEmpty())tooltip=NavalInvasionOverlay.tooltip(mx,my);
        if(tooltip.isEmpty())tooltip=NavalSeaHover.text();
        if(!tooltip.isEmpty())drawTooltip(g,mx,my,tooltip);
        String hover=CommanderPanels.hoverKey(mx,my);if(hover.isEmpty())for(var box:boxes)if(box.contains(mx,my)){hover="hud/"+box.x()+"/"+box.y()+"/"+box.width()+"/"+box.height();break;}if(updateHover(hover))dev.hoi.client.audio.AudioUiSounds.play("ui.menu_over");
    }
    private static void drawTooltip(GuiGraphicsExtractor g,int mx,int my,String tooltip){
        if(dev.hoi.client.ui.tooltip.TooltipLayer.defer(g,()->drawTooltip(g,mx,my,tooltip)))return;
        var client=Minecraft.getInstance();
        var lines=Tooltips.tooltipLines(client.font,tooltip,g.guiWidth());int textWidth=lines.stream().mapToInt(client.font::width).max().orElse(0),lineHeight=client.font.lineHeight,textHeight=lines.size()*lineHeight;var position=tooltipPosition(g.guiWidth(),g.guiHeight(),mx,my,textWidth,textHeight);
        g.nextStratum();Tooltips.background(g,position.x()-7,position.y()-7,textWidth+14,textHeight+14);var attachment=boxes.stream().filter(box->box.contains(mx,my)&&box.request()!=null&&box.request().mode()==Mode.AIR_ATTACH&&box.request().selection().contains("action/detach")).findFirst().orElse(null);var tooltipWings=attachment==null?List.<Member>of():view.wings().stream().filter(wing->attachment.request().selection().contains(wing.id())).toList();int plane=0;for(int n=0;n<lines.size();n++){g.text(client.font,lines.get(n),position.x(),position.y()+n*lineHeight,0xFFE0E0E0);if(plane<tooltipWings.size()){var plain=new StringBuilder();lines.get(n).accept((index,style,codepoint)->{plain.appendCodePoint(codepoint);return true;});if(plain.toString().startsWith("    "))UiAssets.drawContent(g,tooltipWings.get(plane++).icon(),position.x(),position.y()+n*lineHeight,14,lineHeight);}}renderedTooltip=tooltip;
    }
    private static void airAttachments(GuiGraphicsExtractor g,UiUnits u,List<Box> boxes,Card card,List<Member> attached,int x,int y){
        int top=y-u.size(8)-u.size(18),origin=x+u.size(2.5),width=u.size(38);var selected=CommanderPanels.attachmentWings();int[] frames={0,2,3};int plus=selected.isEmpty()?-1:2;
        for(int index=0;index<3;index++){int frame=frames[index];if(index!=plus&&attached.stream().noneMatch(wing->attachmentFrame(CommanderPanels.airRole(wing))==frame))continue;int left=origin+width*index/3;airAttachmentImage(g,u,left,top,origin+width*(index+1)/3-left,frame);}
        for(int index=0;index<3;index++){int frame=frames[index];
            var members=attached.stream().filter(wing->attachmentFrame(CommanderPanels.airRole(wing))==frame).toList();
            if(members.isEmpty()&&index!=plus)continue;
            int left=origin+width*index/3,right=origin+width*(index+1)/3,cellWidth=right-left;
            if(members.isEmpty()){
                String tip=UiCommandLabels.text(selected.isEmpty()?"ATTACH_AIR_WING_SELECT_SOME":CommanderPanels.attachmentAllowed()?"ATTACH_AIR_WING_DESC":"CANNOT_ATTACH_AIR_WING_DESC","");
                boxes.add(new Box(left,top,cellWidth,u.size(18),null,0,tip,()->{if(!selected.isEmpty()){var values=new ArrayList<String>();values.add("army/"+card.id());values.addAll(selected);open(new Open(Mode.AIR_ATTACH,"",values));}}));
                continue;
            }
            String tip=UiCommandLabels.text("AIRWING_ATTACHED","")+"\n\n"+members.stream().map(wing->{String amount="0";if(wing.data()!=null&&wing.data().air()!=null&&wing.data().air().wing()!=null){int count=wing.data().quantity(),target=wing.data().air().wing().target();amount=UiCommandLabels.text("AIRWING_G_STR","").replace("$COUNT|G0$","§G"+count+"§!").replace("$LIMIT|0$",Integer.toString(target)).replace("$VALUE|%0G$","§G"+Math.round(count*100.0/target)+"%§!");}return wing.name()+"\n    "+amount;}).collect(java.util.stream.Collectors.joining("\n\n"))+"\n\n"+UiCommandLabels.text("ATTACH_AIR_WING_SELECT_DESC","");
            var font=Minecraft.getInstance().font;String count=Integer.toString(members.stream().mapToInt(wing->wing.data().quantity()).sum());count=count.substring(0,Math.min(3,count.length()));float scale=Math.min(UiText.scale(font),u.size(6)/(float)font.lineHeight);g.pose().pushMatrix();g.pose().translate(left+(cellWidth-font.width(count)*scale)/2,top+u.size(11.5)+(u.size(6)-font.lineHeight*scale)/2);g.pose().scale(scale);g.text(font,count,0,0,UiMenuStyle.TEXT);g.pose().popMatrix();var values=new ArrayList<String>();values.add("action/detach");members.stream().map(Member::id).forEach(values::add);boxes.add(new Box(left,top,cellWidth,u.size(18),new Open(Mode.AIR_ATTACH,"",values),0,tip,()->CommanderPanels.showWings(members.stream().map(Member::id).toList())));
        }
    }
    static int attachmentFrame(String role){return switch(Objects.toString(role,"")){case "FIGHTER","HEAVY_FIGHTER"->0;case "CAS","TACTICAL_BOMBER","NAVAL_BOMBER"->2;default->-1;};}
    private static void airAttachmentImage(GuiGraphicsExtractor g,UiUnits u,int x,int y,int width,int frame){
        UiAssets.nineSlice(g,"commander/commands/attached_air_bg/frame_0",x,y,width,u.size(18),3,u.scale()*.4);
        g.pose().pushMatrix();g.pose().translate(x+(width-u.length(11.44))/2,y+u.length(1));g.pose().scale(u.scale()*.4f/5,u.scale()*.4f);g.blit(Identifier.parse("hoi:textures/gui/commander/commands/attached_air_types/atlas.png"),0,0,143,25,frame/5f,(frame+1)/5f,0,1);g.pose().popMatrix();
    }
    private static String creationTooltip(boolean group){String prefix=group?"CREATE_ARMY_GROUP":"CREATE_GROUP";boolean empty=group?CommanderPanels.formationArmies().isEmpty():CommanderPanels.commandSelection().isEmpty();return UiCommandLabels.text(prefix+(empty?"_WITH_NO_UNIT_SELECTED_TOOLTIP":"_WITH_UNITS_SELECTED_TOOLTIP"),"")+"\n\n§Y"+UiCommandLabels.text(group?"OPERATION_NEW_ARMY_GROUP":"OPERATION_NEW_GROUP","")+"§!\n"+UiCommandLabels.text(group?"COMMAND_ARMY_GROUP_DESC":"COMMAND_GROUP_DESC","");}
}
