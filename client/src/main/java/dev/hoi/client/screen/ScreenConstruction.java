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
import dev.hoi.client.ui.UiScrollBar;

import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolConstructionView.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private UiScrollBar queueScrollbar,paletteScrollbar;
    private boolean queueDragging,paletteDragging;
    private record ScreenPaletteGroup(String name,int offset,int height) {}
    private List<ScreenPaletteGroup> paletteGroups=List.of();
    private boolean loadingFailed;
    private record ScreenConversionBox(ConversionState state,int x,int y) {}
    private List<ScreenConversionBox> conversionBoxes=List.of();
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
        queueScrollbar=null;paletteScrollbar=null;
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
        var groups=new ArrayList<ScreenPaletteGroup>();
        int offset=0;
        for(int groupIndex=0;groupIndex<BUILDING_GROUPS.size();groupIndex++) {
            var ids=BUILDING_GROUPS.get(groupIndex);
            var group=ids.stream().flatMap(id->view.buildings().stream().filter(b->b.id().equals(id))).toList();
            if(group.isEmpty())continue;
            int groupHeight=24+((group.size()+1)/2)*(icon+1);
            groups.add(new ScreenPaletteGroup(List.of("state","shared","province").get(groupIndex),offset,groupHeight));
            offset+=24;
            for(int i=0;i<group.size();i++)entries.add(new ScreenPaletteEntry(group.get(i),i%2,offset+(i/2)*(icon+1)));
            offset+=((group.size()+1)/2)*(icon+1);
        }
        paletteGroups=List.copyOf(groups);
        paletteScrollbar=new UiScrollBar(pane-10,y,height-30,height-y-30,offset-1);
        paletteScroll=Math.clamp(paletteScroll,0,paletteScrollbar.maximum());
        for(var entry:entries) {
            var b=entry.building();int bx=x+entry.column()*(icon+1),by=y+entry.offset()-paletteScroll;
            if(by<y||by+icon>height-30)continue;
            var button=new UiPanelButton(b.name(),bx,by,icon,icon,()->send(ProtocolConstructionPayloads.Action.SELECT,b.id(),0,null)) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    if(!b.texture().isBlank())UiAssets.draw(g,b.texture()+(b.enabled()?"":"_disabled"),bx+2,by+2,icon-4,icon-4);
                    else dev.hoi.client.ui.UiText.cell(g,font,b.name(),bx+2,by+2,icon-4,icon-4,UiMenuStyle.TEXT,false);
                    if(b.enabled()&&view.selected().equals(b.id()))g.outline(bx+2,by+2,icon-4,icon-4,0xFFFFFFFF);
                }
            };
            button.active=b.enabled()&&pending==0;
            button.setTooltip(dev.hoi.client.ui.UiTooltips.createExact(b.tooltip()==null?b.name()+"\n"+b.reason():b.tooltip()));addRenderableWidget(button);
        }
        int repair=view.summary().repairPriority();
        for(int direction:new int[]{-1,1}) {
            var button=new UiMenuButton(direction<0?"−":"+",direction<0?pane-101:pane-47,top+3,21,19,
                    ()->send(ProtocolConstructionPayloads.Action.REPAIR,"",Math.clamp(repair+direction,0,10000),null));
            button.active=pending==0&&(direction>0||repair>0);button.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal("건설보다 수리를 우선하는 공장 수")));addRenderableWidget(button);
        }
        queueScrollbar=new UiScrollBar(paletteX()-11,queueY(),height-30,height-queueY()-30,view.projects().size()*rowHeight());
        scroll=Math.clamp(scroll,0,queueScrollbar.maximum());
        for(int i=0;i<view.projects().size();i++) {
            var p=view.projects().get(i);int by=queueY()+i*rowHeight()-scroll;
            if(by<queueY()||by+rowHeight()>height-30)continue;
            for(int action=0;action<3;action++) {
                var kind=action==0?ProtocolConstructionPayloads.Action.UP:action==1?ProtocolConstructionPayloads.Action.DOWN:ProtocolConstructionPayloads.Action.CANCEL;
                String art="construction/ui/"+(action==0?"up":action==1?"down":"cancel");
                var button=new UiPanelButton(action==0?"↑":action==1?"↓":"×",column(action==0?318:action==1?341:370),by+6,14,16,()->send(kind,p.id(),(kind==ProtocolConstructionPayloads.Action.CANCEL?dev.hoi.client.ui.UiPriorityControls.controlled():dev.hoi.client.ui.UiPriorityControls.shifted())?1:0,null)) {
                    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                        UiAssets.draw(g,art+(active?"":"_disabled"),getX(),getY(),getWidth(),getHeight());
                        if(active&&isHoveredOrFocused())g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0x18FFFFFF);
                    }
                };
                button.active=pending==0&&(action!=0||i>0)&&(action!=1||i<view.projects().size()-1);
                button.setTooltip(dev.hoi.client.ui.UiTooltips.create(action<2?dev.hoi.client.ui.UiPriorityControls.tooltip(action==0):Component.literal("§a클릭하면 §e건설이 취소§a됩니다.\nCtrl + 클릭하면 §e건설 모두 취소§a됩니다.")));addRenderableWidget(button);
            }
        }
    }
    private int paletteSize(){return Math.min(29,Math.max(19,(height-top-156)/11-3));}
    private int paletteX(){return pane-2*(paletteSize()+2)-16;}
    private int queueY(){return top+77+reservationHeight()*ScreenConstructionRows.count(view.summary());}
    private int reservationHeight(){return Math.clamp((height-top-77-30-rowHeight())/ScreenConstructionRows.count(view.summary()),16,27);}
    private int column(int sourceX){return 6+(paletteX()-21)*sourceX/402;}
    private int rowHeight(){return 29;}
    private String buildingName(String id) {
        return view.buildings().stream().filter(b->b.id().equals(id)).map(Building::name).findFirst().orElse(id);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        UiMenuStyle.panel(g,0,top,pane,height-top);
        UiAssets.nineSlice(g,"construction/ui/header",0,top,pane,25,4,.5);
        UiMenuStyle.heading(g, font,"건설",9,top+8, pane - 44,UiMenuStyle.TEXT);
        UiMenuBar.draw(g,width,view == null ? dev.hoi.client.ClientCampaignHud.hud() : view.hud(),mx,my,hudScroll);
        if(view!=null) {
            var s=view.summary();int q=paletteX()-15;
            UiAssets.draw(g,"construction/summary/civilian_factory_repair",pane-123,top+5,18,16);
            UiAssets.nineSlice(g,"construction/ui/number_box",pane-78,top+3,29,19,4,.5);
            String repairCount=Integer.toString(s.repairPriority());
            dev.hoi.client.ui.UiText.cell(g,font,repairCount,pane-76,top+4,25,17,UiMenuStyle.TEXT,false);
            int cell=(q-6)/3;
            int summaryHeight=Math.min(49,UiAssets.imageHeight("construction/ui/summary_background",q-6));
            int summaryY=top+27+(49-summaryHeight)/2;
            int totalsY=summaryY+summaryHeight*3/98,totalsHeight=summaryHeight*58/98;
            int labelsY=summaryY+summaryHeight*62/98,labelsHeight=summaryHeight*32/98;
            UiAssets.draw(g,"construction/ui/summary_background",6,top+27,q-6,49);
            int assigned=s.used()+s.repair()+s.consumer()+s.reserved();
            dev.hoi.client.ui.UiText.iconText(g,font,"construction/summary/civilian_factory",assigned+"/"+s.total(),7,totalsY+1,cell-1,totalsHeight,13,assigned==s.total()?0xFF55FF55:0xFFFFAA00);
            long speedPercent=Math.round((s.speed()-1)*100);
            dev.hoi.client.ui.UiText.iconText(g,font,"construction/speed",String.format(Locale.ROOT,"%+d%%",speedPercent),6+cell,totalsY+1,cell,totalsHeight,12,speedPercent>=0?0xFF55FF55:0xFFAA0000);
            double energyBalance=s.energy()-s.demand();
            dev.hoi.client.ui.UiText.iconText(g,font,energyBalance<0?"construction/energy_deficit":"construction/energy",UiMenuBar.rawNumber(energyBalance),6+cell*2,totalsY+1,q-6-cell*2,totalsHeight,12,energyBalance>=0?0xFF55FF55:0xFFAA0000);
            dev.hoi.client.ui.UiText.cell(g,font,"위치",column(20),labelsY+1,column(162)-column(20),labelsHeight,UiMenuStyle.TEXT,false);
            dev.hoi.client.ui.UiText.cell(g,font,"작업",column(172),labelsY+1,23,labelsHeight,UiMenuStyle.TEXT,false);
            UiAssets.draw(g,"construction/summary/civilian_factory",column(249)+1,labelsY+(labelsHeight-13)/2+1,column(310)-column(249),13);
            dev.hoi.client.ui.UiText.cell(g,font,"우선순위",column(318),labelsY+1,column(370)+14-column(318),labelsHeight,UiMenuStyle.TEXT,false);
            if(my>=labelsY&&my<labelsY+labelsHeight&&mx>=6&&mx<q) {
                String header=mx<column(166)?"location":mx<column(231)?"task":mx<column(314)?"factories":"priority";
                UiTooltips.drawExact(g,font,Component.translatable("hoi.construction.header."+header).getString(),mx,my);
            }
            ScreenConstructionRows.draw(g,font,s,6,top+75,q-6,reservationHeight(),mx,my);
            g.enableScissor(paletteX()-2,top+54,pane-4,height-30);
            for(var group:paletteGroups) {
                int y=top+54+group.offset()-paletteScroll;
                UiAssets.nineSlice(g,"construction/ui/group_"+group.name()+"_background",paletteX()-2,y,pane-paletteX()-3,group.height(),44,.5);
                UiAssets.draw(g,"construction/ui/group_"+group.name(),paletteX(),y+1,22,21);
            }
            g.disableScissor();
            for(var group:paletteGroups) {
                int y=top+54+group.offset()-paletteScroll;
                if(mx>=paletteX()&&mx<paletteX()+24&&my>=Math.max(top+54,y)&&my<Math.min(height-30,y+24))
                    UiTooltips.drawExact(g,font,Component.translatable("hoi.construction.group."+group.name()).getString(),mx,my);
            }
            for(int i=0;i<view.projects().size();i++) {
                var p=view.projects().get(i);int y=queueY()+i*rowHeight()-scroll;
                if(y<queueY()||y+rowHeight()>height-30)continue;
                UiAssets.draw(g,"construction/ui/entry",6,y,q-6,rowHeight()-2);
                dev.hoi.client.ui.UiText.cell(g,font,p.name(),column(20),y+3,column(162)-column(20),20,UiMenuStyle.TEXT,false);
                UiAssets.draw(g,"construction/"+p.building(),column(172),y+2,23,23);
                dev.hoi.client.ui.UiText.cell(g,font,Integer.toString(p.levels()),column(190),y+14,12,11,UiMenuStyle.TEXT,true);
                dev.hoi.client.ui.UiText.cell(g,font,p.factories()+"/15",column(249)+1,y+1,column(310)-column(249),17,UiMenuStyle.TEXT,false);
                int progressX=column(250),progressWidth=column(310)-progressX;
                g.pose().pushMatrix();
                g.pose().translate(0,-1f/minecraft.getWindow().getGuiScale());
                UiAssets.draw(g,"construction/ui/progress_background",progressX,y+18,progressWidth,5);
                int filled=(int)(progressWidth*Math.clamp(p.cost()==0?0:p.progress()/p.cost(),0,1));
                if(filled>0) {
                    g.enableScissor(progressX,y+17,progressX+filled,y+24);
                    UiAssets.draw(g,"construction/ui/progress",progressX,y+18,progressWidth,5);
                    g.disableScissor();
                }
                g.pose().popMatrix();
                if(mx>=column(172)&&mx<column(172)+23&&my>=y+2&&my<y+25&&p.taskTooltip()!=null)UiTooltips.drawExact(g,font,p.taskTooltip(),mx,my);
                else if(mx>=6&&mx<column(318)&&my>=y&&my<y+rowHeight()-3)UiTooltips.drawExact(g,font,p.tooltip()==null?p.name()+" · "+buildingName(p.building())+"\n"+String.format(Locale.ROOT,"진행 %.0f / %.0f · 하루 %.2f\n완료 예상: %s",p.progress(),p.cost(),p.daily(),p.daily()>0?(long)Math.ceil(Math.max(0,p.cost()-p.progress())/p.daily())+"일":"공장 배정 대기"):p.tooltip(),mx,my);
            }
            queueScrollbar.draw(g,scroll);
            paletteScrollbar.draw(g,paletteScroll);
            if(mx>=pane-123&&mx<pane-27&&my>=top+3&&my<top+23)UiTooltips.draw(g,font,"건설보다 건물 수리를 우선하는 공장 수\n우선 지정 "+s.repairPriority()+" · 실제 수리 배정 "+s.repair()+"\n수리할 건물이 없으면 건설에 배정됩니다.",mx,my);
            if(mx>=6&&mx<q&&my>=top+27&&my<top+58) {
                int column=Math.min(2,(mx-6)/cell);
                String detail=column==0?calculation(s.factoryCalculation(),"사용 중인 공장"):column==1?calculation(s.speedCalculation(),"건설 속도"):calculation(s.energyCalculation(),"사용 가능한 에너지");
                UiTooltips.drawExact(g,font,detail,mx,my);
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
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            if(queueScrollbar!=null&&queueScrollbar.contains(event.x(),event.y())) {
                if(queueScrollbar.arrow(event.y()))scroll=queueScrollbar.step(event.y(),scroll,rowHeight());
                else {queueDragging=true;scroll=queueScrollbar.offset(event.y());}
                rebuildWidgets();return true;
            }
            if(paletteScrollbar!=null&&paletteScrollbar.contains(event.x(),event.y())) {
                if(paletteScrollbar.arrow(event.y()))paletteScroll=paletteScrollbar.step(event.y(),paletteScroll,paletteSize()+1);
                else {paletteDragging=true;paletteScroll=paletteScrollbar.offset(event.y());}
                rebuildWidgets();return true;
            }
        }
        int q=paletteX()-15,cell=(q-6)/3;
        if(view!=null&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&event.x()>=6+cell*2&&event.x()<q&&event.y()>=top+27&&event.y()<top+58) {
            HoiClient.openMenu(ProtocolMenuTab.TRADE);return true;
        }
        if(view!=null&&view.selected().equals("conversion")&&event.x()>=pane) {
            if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&pending==0)for(var box:conversionBoxes.reversed()) {
                if(event.y()<box.y()||event.y()>=box.y()+26)continue;
                if(event.x()>=box.x()+6&&event.x()<box.x()+26) {dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(ProtocolConstructionPayloads.Action.CONVERT,box.state().state(),0,null);return true;}
                if(event.x()>=box.x()+78&&event.x()<box.x()+98) {dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(ProtocolConstructionPayloads.Action.CONVERT,box.state().state(),1,null);return true;}
            }
            return true;
        }
        if((event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT||event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT)&&event.x()>=pane&&event.y()>top+22&&event.x()<UiMenuBar.statsRight(width)) {
            if(view!=null&&pending==0&&minecraft.player!=null) {
                if(!minecraft.options.getCameraType().isFirstPerson()) {
                    return true;
                }
                var matrix=minecraft.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new Matrix4f()).invert();
                var ray=matrix.transformProject(new Vector3f((float)(event.x()/width*2-1),(float)(1-event.y()/height*2),1)).normalize();
                send(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT?ProtocolConstructionPayloads.Action.PLACE:ProtocolConstructionPayloads.Action.CANCEL_AT,"",dev.hoi.client.ui.UiPriorityControls.shifted()?1:0,ray);
            }
            return true;
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(y<top&&view!=null){hudScroll=UiMenuBar.scroll(width,view.hud(),hudScroll,horizontal,vertical);return true;}
        if(x>=paletteX()&&x<pane&&paletteScrollbar!=null&&y>=paletteScrollbar.top()&&y<paletteScrollbar.bottom()) {paletteScroll-=(int)(vertical*(paletteSize()+1));rebuildWidgets();return true;}
        if(x>=0&&x<paletteX()&&queueScrollbar!=null&&y>=queueScrollbar.top()&&y<queueScrollbar.bottom()) {scroll-=(int)(vertical*rowHeight());rebuildWidgets();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if(queueDragging&&queueScrollbar!=null){scroll=queueScrollbar.offset(event.y());rebuildWidgets();return true;}
        if(paletteDragging&&paletteScrollbar!=null){paletteScroll=paletteScrollbar.offset(event.y());rebuildWidgets();return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        boolean dragging=queueDragging||paletteDragging;queueDragging=false;paletteDragging=false;
        return super.mouseReleased(event)||dragging;
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
