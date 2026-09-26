package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiPanelButton;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.UiTooltips;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolConstructionView.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.Consumer;


public final class ScreenConstruction extends Screen implements InputSidebarMovement.InputMovementScreen {

    private static final List<List<String>> BUILDING_GROUPS=List.of(
            List.of("infrastructure","air_base","anti_air","radar"),
            List.of("military_factory","civilian_factory","dockyard","office_park","refinery","fuel_silo","rocket_site","nuclear_reactor","power_plant","energy_farm","conversion"),
            List.of("hub","railway","port","fort","coastal_fort"));
    private record ScreenPaletteEntry(Building building,int column,int offset) {}
    private final String token;
    private final Consumer<ProtocolConstructionPayloads.Request> transport;
    private ProtocolConstructionView view;
    private int pane,top,scroll,paletteScroll,pending,refresh,hudScroll;
    private List<Integer> paletteDividers=List.of();
    private boolean consumerTooltip, loadingFailed;
    private record ScreenConversionBox(ConversionState state,int x,int y) {}
    private List<ScreenConversionBox> conversionBoxes=List.of();
    public boolean showingConsumerTooltip() { return consumerTooltip; }
    public ScreenConstruction() {
        this(UUID.randomUUID().toString(),null,r->{if(ClientPlayNetworking.canSend(ProtocolConstructionPayloads.Request.TYPE))ClientPlayNetworking.send(r);});
    }
    ScreenConstruction(String token,ProtocolConstructionView fixture,Consumer<ProtocolConstructionPayloads.Request> transport) {
        super(Component.literal("건설"));this.token=token;this.view=fixture;this.transport=transport;
    }
    public void open(){send(ProtocolConstructionPayloads.Action.OPEN,"",0,null);}
    public void update(ProtocolConstructionView next) {
        if(!next.session().equals(token))return;
        if(next.country().isEmpty() || view!=null&&!next.country().equals(view.country())){
            if(!next.message().isBlank()&&minecraft.player!=null)minecraft.player.sendSystemMessage(Component.literal(next.message()));
            minecraft.gui.setScreen(null);return;
        }
        if(view!=null&&next.revision()<=view.revision())return;
        view=next;pending=0;rebuildWidgets();
    }
    int panelWidth(){return pane;}
    @Override protected void init() {
        pane=UiPanelLayout.width(ProtocolMenuTab.CONSTRUCTION,width);top=UiMenuBar.height(width);
        UiMenuBar.buttons(width,view==null?"":view.country(),ProtocolMenuTab.CONSTRUCTION,t->{
            if(t==ProtocolMenuTab.CONSTRUCTION)return;
            if(t==ProtocolMenuTab.RESEARCH)HoiClient.open();else HoiClient.openMenu(t);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new UiMenuButton("×",pane-25,top+3,19,19,this::onClose));
        if(view==null){
            return;
        }
        int icon=paletteSize(),x=paletteX(),y=top+54;
        var entries=new ArrayList<ScreenPaletteEntry>();
        var dividers=new ArrayList<Integer>();
        int offset=0;
        for(var ids:BUILDING_GROUPS) {
            var group=ids.stream().flatMap(id->view.buildings().stream().filter(b->b.id().equals(id))).toList();
            if(group.isEmpty())continue;
            if(!entries.isEmpty()){dividers.add(offset+2);offset+=7;}
            for(int i=0;i<group.size();i++)entries.add(new ScreenPaletteEntry(group.get(i),i%2,offset+(i/2)*(icon+3)));
            offset+=((group.size()+1)/2)*(icon+3);
        }
        paletteDividers=List.copyOf(dividers);
        paletteScroll=Math.clamp(paletteScroll,0,Math.max(0,offset-3-Math.max(0,height-y-30)));
        for(var entry:entries) {
            var b=entry.building();int bx=x+entry.column()*(icon+2),by=y+entry.offset()-paletteScroll;
            if(by<y||by+icon>height-30)continue;
            var button=new UiPanelButton(b.name(),bx,by,icon,icon,()->send(ProtocolConstructionPayloads.Action.SELECT,b.id(),0,null)) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    UiMenuStyle.control(g,bx,by,icon,icon,view.selected().equals(b.id()),isHoveredOrFocused());
                    if(!b.texture().isBlank())UiAssets.draw(g,b.texture(),bx+2,by+2,icon-4,icon-4);
                    else dev.hoi.client.ui.UiText.cell(g,font,b.name(),bx+2,by+2,icon-4,icon-4,UiMenuStyle.TEXT,false);
                    if(!b.enabled())g.fill(bx+1,by+1,bx+icon-1,by+icon-1,0x88000000);
                }
            };
            button.active=b.enabled()&&pending==0;
            button.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(b.name()+"\n"+b.reason())));addRenderableWidget(button);
        }
        int repair=view.summary().repairPriority();
        for(int direction:new int[]{-1,1}) {
            var button=new UiMenuButton(direction<0?"−":"+",direction<0?pane-101:pane-47,top+3,21,19,
                    ()->send(ProtocolConstructionPayloads.Action.REPAIR,"",Math.clamp(repair+direction,0,10000),null));
            button.active=pending==0&&(direction>0||repair>0);button.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal("건설보다 수리를 우선하는 공장 수")));addRenderableWidget(button);
        }
        int visibleRows=Math.max(1,(height-queueY()-30)/rowHeight());
        scroll=Math.clamp(scroll/rowHeight(),0,Math.max(0,view.projects().size()-visibleRows))*rowHeight();
        for(int i=0;i<view.projects().size();i++) {
            var p=view.projects().get(i);int by=queueY()+i*rowHeight()-scroll;
            if(by<queueY()||by+rowHeight()>height-30)continue;
            for(int action=0;action<3;action++) {
                var kind=action==0?ProtocolConstructionPayloads.Action.UP:action==1?ProtocolConstructionPayloads.Action.DOWN:ProtocolConstructionPayloads.Action.CANCEL;
                var button=new UiMenuButton(action==0?"↑":action==1?"↓":"×",x-63+action*19,by+rowHeight()-23,18,18,()->send(kind,p.id(),kind!=ProtocolConstructionPayloads.Action.CANCEL && dev.hoi.client.ui.UiPriorityControls.shifted()?1:0,null));
                button.active=pending==0&&(action!=0||i>0)&&(action!=1||i<view.projects().size()-1);
                button.setTooltip(dev.hoi.client.ui.UiTooltips.create(action<2?dev.hoi.client.ui.UiPriorityControls.tooltip(action==0):Component.literal("건설 취소")));addRenderableWidget(button);
            }
        }
    }

    private int paletteSize(){return Math.min(29,Math.max(19,(height-top-137)/10-3));}
    private int paletteX(){return pane-2*(paletteSize()+2)-6;}
    private int queueY(){return top+98;}
    private int rowHeight(){return 29;}
    private String buildingName(String id) {
        return view.buildings().stream().filter(b->b.id().equals(id)).map(Building::name).findFirst().orElse(id);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        consumerTooltip=false;
        UiMenuStyle.panel(g,0,top,pane,height-top);
        UiMenuStyle.heading(g, font,"건설",9,top+8, pane - 44,UiMenuStyle.TEXT);
        UiMenuBar.draw(g,width,view == null ? dev.hoi.client.ClientCampaignHud.hud() : view.hud(),mx,my,hudScroll);
        if(view!=null) {
            var s=view.summary();int q=paletteX()-5;
            UiAssets.draw(g,"construction/summary/civilian_factory_repair",pane-123,top+5,18,16);
            UiMenuStyle.recess(g,pane-78,top+3,29,19);
            String repairCount=Integer.toString(s.repairPriority());
            dev.hoi.client.ui.UiText.cell(g,font,repairCount,pane-76,top+4,25,17,UiMenuStyle.TEXT,false);
            int cell=(q-6)/3;
            UiMenuStyle.recess(g,6,top+27,q-6,25);
            int assigned=s.used()+s.repair()+s.consumer()+s.reserved();
            dev.hoi.client.ui.UiText.iconText(g,font,"construction/summary/civilian_factory",assigned+"/"+s.total(),7,top+28,cell-1,22,13,assigned==s.total()?0xFF55FF55:0xFFFFAA00);
            long speedPercent=Math.round((s.speed()-1)*100);
            dev.hoi.client.ui.UiText.iconText(g,font,"construction/speed",String.format(Locale.ROOT,"%+d%%",speedPercent),6+cell,top+28,cell,22,12,speedPercent>=0?0xFF55FF55:0xFFAA0000);
            double energyBalance=s.energy()-s.demand();
            dev.hoi.client.ui.UiText.iconText(g,font,energyBalance<0?"construction/energy_deficit":"construction/energy",UiMenuBar.rawNumber(energyBalance),6+cell*2,top+28,q-6-cell*2,22,12,energyBalance>=0?0xFF55FF55:0xFFAA0000);
            UiMenuStyle.metal(g,6,top+54,q-6,18);
            dev.hoi.client.ui.UiText.cell(g,font,"위치",8,top+55,Math.max(24,q-133),16,UiMenuStyle.TEXT,false);
            dev.hoi.client.ui.UiText.cell(g,font,"작업",q-123,top+55,24,16,UiMenuStyle.TEXT,false);
            UiAssets.draw(g,"construction/summary/civilian_factory",q-87,top+56,13,13);
            dev.hoi.client.ui.UiText.cell(g,font,"우선순위",q-58,top+55,55,16,UiMenuStyle.TEXT,false);
            UiMenuStyle.metal(g,6,top+75,q-6,21);
            UiAssets.draw(g,"construction/consumer_goods",10,top+76,19,19);
            dev.hoi.client.ui.UiText.text(g,font,"소비재",33,top+81,UiMenuStyle.TEXT);
            dev.hoi.client.ui.UiText.cell(g,font,Integer.toString(s.consumer()),q-94,top+77,35,17,UiMenuStyle.TEXT,false);
            for(int divider:paletteDividers) {
                int y=top+54+divider-paletteScroll;
                if(y>=top+54&&y+1<height-30) {
                    g.horizontalLine(paletteX(),pane-7,y,0xFF0B0D0F);
                    g.horizontalLine(paletteX(),pane-7,y+1,0xFF55595D);
                }
            }
            for(int i=0;i<view.projects().size();i++) {
                var p=view.projects().get(i);int y=queueY()+i*rowHeight()-scroll;
                if(y<queueY()||y+rowHeight()>height-30)continue;
                UiMenuStyle.metal(g,6,y,q-6,rowHeight()-3);
                dev.hoi.client.ui.UiText.cell(g,font,p.name(),10,y+4,Math.max(24,q-139),20,UiMenuStyle.TEXT,false);
                UiAssets.draw(g,"construction/"+p.building(),q-124,y+2,23,23);
                dev.hoi.client.ui.UiText.cell(g,font,Integer.toString(p.levels()),q-113,y+14,12,11,UiMenuStyle.TEXT,true);
                dev.hoi.client.ui.UiText.cell(g,font,p.factories()+"/15",q-96,y+1,31,17,UiMenuStyle.TEXT,false);
                g.fill(q-96,y+19,q-65,y+23,0xFF101511);
                g.fill(q-96,y+19,q-96+(int)(31*Math.clamp(p.cost()==0?0:p.progress()/p.cost(),0,1)),y+23,0xFF79945A);
                if(mx>=6&&mx<q&&my>=y&&my<y+rowHeight()-3)UiTooltips.draw(g,font,p.name()+" · "+buildingName(p.building())+"\n"+String.format(Locale.ROOT,"진행 %.0f / %.0f · 하루 %.1f\n완료 예상: %s",p.progress(),p.cost(),p.daily(),p.daily()>0?(long)Math.ceil(Math.max(0,p.cost()-p.progress())/p.daily())+"일":"공장 배정 대기"),mx,my);
            }
            if(mx>=pane-123&&mx<pane-27&&my>=top+3&&my<top+23)UiTooltips.draw(g,font,"건설보다 건물 수리를 우선하는 공장 수\n우선 지정 "+s.repairPriority()+" · 실제 수리 배정 "+s.repair()+"\n수리할 건물이 없으면 건설에 배정됩니다.",mx,my);
            if(mx>=6&&mx<q&&my>=top+27&&my<top+52) {
                int column=Math.min(2,(mx-6)/cell);
                String detail=column==0?"민간공장 "+assigned+"/"+s.total()+"\n소비재 "+s.consumer()+" · 무역/기관 예약 "+s.reserved()+"\n건설 "+s.used()+" · 수리 "+s.repair()+" · 미사용 "+s.idle()+"\n무역 상품 "+(s.tradeGoods()==null?"—":s.tradeGoods()):column==1?calculation(s.speedCalculation(),"건설 속도"):calculation(s.energyCalculation(),"사용 가능한 에너지");
                UiTooltips.draw(g,font,detail,mx,my);
            }
            if(mx>=6&&mx<q&&my>=top+75&&my<top+96) {
                consumerTooltip=true;
                UiTooltips.draw(g,font,calculation(s.consumerCalculation(),"소비재"),mx,my);
                consumerTooltip=false;
            }
        } else dev.hoi.client.ui.UiText.text(g, font,loadingFailed?"서버 응답이 없습니다.":"건설 현황을 불러오는 중…",10,top+40,UiMenuStyle.MUTED);
        drawConversions(g,mx,my);
        super.extractRenderState(g,mx,my,delta);
    }
    private void drawConversions(GuiGraphicsExtractor g,int mx,int my) {
        conversionBoxes=List.of();
        if(view==null||!view.selected().equals("conversion")||minecraft.player==null)return;
        var camera=minecraft.gameRenderer.mainCamera();var matrix=camera.getViewRotationProjectionMatrix(new Matrix4f());
        var boxes=new ArrayList<ScreenConversionBox>();
        for(var state:view.conversions()) {
            var relative=new net.minecraft.world.phys.Vec3(state.x(),state.y(),state.z()).subtract(camera.position());
            var forward=camera.forwardVector();
            if(relative.x*forward.x()+relative.y*forward.y()+relative.z*forward.z()<=0)continue;
            var point=matrix.transformProject(relative.toVector3f());
            if(!point.isFinite()||Math.abs(point.x)>1||Math.abs(point.y)>1)continue;
            int x=Math.round((point.x+1)*width/2)-53,y=Math.round((1-point.y)*height/2)-13;
            if(x<pane||x+106>UiMenuBar.statsRight(width)||y<top+24||y+26>height-24)continue;
            boxes.add(new ScreenConversionBox(state,x,y));
            UiAssets.draw(g,"construction/conversion_background",x,y,106,26);
            UiAssets.draw(g,"construction/conversion_civilian",x+6,y+3,20,20);
            dev.hoi.client.ui.UiText.cell(g,font,Integer.toString(state.civilian()),x+26,y+3,18,20,0xFFFFFFFF,false);
            dev.hoi.client.ui.UiText.cell(g,font,Integer.toString(state.military()),x+62,y+3,16,20,0xFFFFFFFF,false);
            UiAssets.draw(g,"construction/conversion_military",x+78,y+3,20,20);
            if(mx>=x&&mx<x+106&&my>=y&&my<y+26)UiTooltips.draw(g,font,state.name()+"\n민간공장 클릭: 민간공장으로 전환\n군수공장 클릭: 군수공장으로 전환\n반대 방향 예약이 있으면 하나 취소합니다.\n숫자는 전환 예약을 포함합니다.",mx,my);
        }
        conversionBoxes=List.copyOf(boxes);
    }
    private String calculation(String explanation,String label) {
        return explanation==null||explanation.isBlank()?label+"\n계산 내역이 제공되지 않았습니다.":explanation;
    }
    private void send(ProtocolConstructionPayloads.Action action,String item,int amount,Vector3f ray) {
        if(pending>0&&action!=ProtocolConstructionPayloads.Action.CLOSE)return;
        boolean passive=action==ProtocolConstructionPayloads.Action.REFRESH;
        if(action==ProtocolConstructionPayloads.Action.OPEN)loadingFailed=false;
        if(!passive)pending=view==null?200:100;transport.accept(new ProtocolConstructionPayloads.Request(action,token,view==null?0:view.revision(),item,amount,ray==null?0:ray.x,ray==null?0:ray.y,ray==null?0:ray.z));
        if(!passive)rebuildWidgets();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(view!=null&&view.selected().equals("conversion")&&event.x()>=pane) {
            if(event.button()==0&&pending==0)for(var box:conversionBoxes.reversed()) {
                if(event.y()<box.y()||event.y()>=box.y()+26)continue;
                if(event.x()>=box.x()+6&&event.x()<box.x()+26) {dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(ProtocolConstructionPayloads.Action.CONVERT,box.state().state(),0,null);return true;}
                if(event.x()>=box.x()+78&&event.x()<box.x()+98) {dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(ProtocolConstructionPayloads.Action.CONVERT,box.state().state(),1,null);return true;}
            }
            return true;
        }
        if((event.button()==0||event.button()==1)&&event.x()>=pane&&event.y()>top+22&&event.x()<UiMenuBar.statsRight(width)) {
            if(view!=null&&pending==0&&minecraft.player!=null) {
                if(!minecraft.options.getCameraType().isFirstPerson()) {
                    return true;
                }
                var matrix=minecraft.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new Matrix4f()).invert();
                var ray=matrix.transformProject(new Vector3f((float)(event.x()/width*2-1),(float)(1-event.y()/height*2),1)).normalize();
                send(event.button()==0?ProtocolConstructionPayloads.Action.PLACE:ProtocolConstructionPayloads.Action.CANCEL_AT,"",dev.hoi.client.ui.UiPriorityControls.shifted()?1:0,ray);
            }
            return true;
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(y<top&&view!=null){hudScroll=UiMenuBar.scroll(width,view.hud(),hudScroll,horizontal,vertical);return true;}
        if(x<pane){if(x>=paletteX())paletteScroll-=(int)(vertical*(paletteSize()+3));else scroll-=(int)(vertical*rowHeight());rebuildWidgets();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void tick() {
        if(pending>0&&--pending==0){
            if(view==null){loadingFailed=true;rebuildWidgets();}
            else send(ProtocolConstructionPayloads.Action.REFRESH,"",0,null);
        }
        else if(view!=null&&++refresh%60==0&&pending==0)send(ProtocolConstructionPayloads.Action.REFRESH,"",0,null);
    }
    @Override public boolean keyPressed(KeyEvent e){return InputSidebarMovement.consumes(minecraft,e)||super.keyPressed(e);}
    @Override public boolean keyReleased(KeyEvent e){return InputSidebarMovement.consumes(minecraft,e)||super.keyReleased(e);}
    @Override public void removed(){InputSidebarMovement.release(minecraft);if(ScreenDialogCoordinator.suspending())return;transport.accept(new ProtocolConstructionPayloads.Request(ProtocolConstructionPayloads.Action.CLOSE,token,view==null?0:view.revision(),"",0,0,0,0));}
    @Override public int unitHudLeft(){return pane;}
    @Override public boolean allowsMovement(){return true;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}
