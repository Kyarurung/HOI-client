package dev.hoi.client.screen.industry;

import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiPanelButton;
import dev.hoi.client.ui.UiPriorityControls;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.protocol.industry.IndustryConstructionView.*;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class IndustryConstruction extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final List<List<String>> BUILDING_GROUPS=List.of(
            List.of("infrastructure","air_base","anti_air","radar"),
            List.of("military_factory","civilian_factory","dockyard","office_park","refinery","fuel_silo","rocket_site","nuclear_reactor","power_plant","energy_farm","conversion"),
            List.of("hub","railway","port","fort","coastal_fort"));
    private static final String[] GROUPS={"state","shared","province"};
    private static final int[] GROUP_BACKGROUND_Y={54,200,516},GROUP_ICON_Y={57,203,521},GROUP_GRID_Y={99,244,558};
    private static final int ROW=52,LIST_TOP=143,ROW_TOP=148,PALETTE_TOP=48,MINIMIZED_OFFSET=420,REPAIR_ENTRY=53,REPAIR_LIST_TOP=206,LINE_FACTORIES=20;
    private record IndustryConstructionRow(String kind,int index) {}
    private final String token;
    private final Consumer<IndustryConstructionPayloads.Request> transport;
    private IndustryConstructionView view;
    private static final UiMenuStyle.Window WINDOW=UiMenuStyle.window(MenuTab.CONSTRUCTION);
    private UiMenuStyle.Frame frame,repairFrame;
    private int pane,top,scroll,paletteScroll,repairScroll,pending,refresh,hudScroll;
    private UiScrollBar queueScrollbar,paletteScrollbar,repairScrollbar;
    private boolean queueDragging,paletteDragging,repairDragging,minimized,repairWindow,repairWindowMoving;
    private String draggedProject="",draggedKind="",draggedOrder="";
    private double projectDragX,projectDragY,projectPointerY,projectOffsetY,orderDragY,orderPointerY,repairWindowX,repairWindowY,repairGrabX,repairGrabY;
    private final Map<String,List<net.minecraft.client.gui.components.AbstractWidget>> projectWidgets=new HashMap<>();
    private boolean projectMoved,orderMoved;
    private boolean loadingFailed;
    private record IndustryConversionBox(ConversionState state,int x,int y) {}
    private List<IndustryConversionBox> conversionBoxes=List.of();
    public IndustryConstruction() {
        this(UUID.randomUUID().toString(),null,r->{if(ClientPlayNetworking.canSend(IndustryConstructionPayloads.Request.TYPE))ClientPlayNetworking.send(r);});
    }
    IndustryConstruction(String token,IndustryConstructionView fixture,Consumer<IndustryConstructionPayloads.Request> transport) {
        super(Component.literal("건설"));this.token=token;this.view=fixture;this.transport=transport;
    }
    public void open(){send(IndustryConstructionPayloads.Action.OPEN,"",0,null);}
    public void update(IndustryConstructionView next) {
        if(!next.session().equals(token))return;
        if(next.country().isEmpty() || view!=null&&!next.country().equals(view.country())){
            minecraft.gui.setScreen(null);return;
        }
        if(view!=null&&next.revision()<=view.revision())return;
        draggedProject="";draggedOrder="";view=next;pending=0;rebuildWidgets();
    }
    int panelWidth(){return pane;}
    int panelRight(){return minimized?Math.max(0,Math.round(frame.x(550))):pane;}
    boolean minimized(){return minimized;}
    boolean repairWindowOpen(){return repairWindow;}
    UiMenuStyle.Frame frame(){return frame;}
    UiMenuStyle.Frame repairFrame(){return repairFrame;}
    int listTop(){return frame.iy(LIST_TOP);}
    int listBottom(){return Math.min(height,frame.iy(windowHeight()-15));}
    int rowTop(int row){return Math.round(frame.y(ROW_TOP+ROW*row))-scroll;}
    int rowBottom(int row){return Math.round(frame.y(ROW_TOP+ROW*row+ROW))-scroll;}
    int projectRow(int index){return rows().indexOf(new IndustryConstructionRow("project",index));}
    int repairRow(int index){return rows().indexOf(new IndustryConstructionRow("repair",index));}
    private int windowHeight(){return (int)Math.ceil((height-frame.y())/frame.scale());}
    private List<IndustryConstructionRow> rows() {
        var rows=new ArrayList<IndustryConstructionRow>();
        if(view==null)return rows;
        var s=view.summary();
        rows.add(new IndustryConstructionRow("consumer",0));
        if(s.tradeGoods()!=null&&s.tradeGoods()>0)rows.add(new IndustryConstructionRow("trade",0));
        if(s.nationalProjects()!=null&&s.nationalProjects()>0)rows.add(new IndustryConstructionRow("projects",0));
        for(int i=0;i<view.repairs().size();i++)rows.add(new IndustryConstructionRow("repair",i));
        for(int i=0;i<view.projects().size();i++)rows.add(new IndustryConstructionRow("project",i));
        return rows;
    }
    private static String source(String key,String fallback){return IndustryEquipmentDesigns.sourceText(UiCommandLabels.text(key,fallback));}
    private Button sourceButton(String name,UiMenuStyle.Frame at,double sx,double sy,double sw,double sh,double offset,Runnable action,BiConsumer<GuiGraphicsExtractor,Button> art) {
        int x=at.ix(sx),y=Math.round(at.y(sy)-(float)offset);
        var button=new UiPanelButton(name,x,y,Math.max(1,at.ix(sx+sw)-x),Math.max(1,Math.round(at.y(sy+sh)-(float)offset)-y),action) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {art.accept(g,this);}
        };
        addRenderableWidget(button);
        return button;
    }
    private static int tint(Button button){return button.active?0xFFFFFFFF:0xFF808080;}
    private void art(GuiGraphicsExtractor g,UiMenuStyle.Frame at,String path,double sx,double sy,double sw,double sh,double offset,int tint) {
        UiAssets.drawExact(g,path,at.x(sx),at.y(sy)-(float)offset,at.size(sw),at.size(sh),tint);
    }
    private void label(GuiGraphicsExtractor g,String value,float x,float center,float width,int color,int align) {
        UiText.marquee(g,font,Tooltips.officerText(value),x,center-font.lineHeight/2f,width,1,color,true,align);
    }
    private static void blend(GuiGraphicsExtractor g,String path,float x,float y,float w,float h) {
        long phase=UiText.marqueeMillis()%1334;
        int alpha=Math.round(255*(phase<667?phase:1334-phase)/667f);
        UiAssets.drawExact(g,path+"/frame_0",x,y,w,h,(255-alpha)<<24|0xFFFFFF);
        if(alpha>0)UiAssets.drawExact(g,path+"/frame_1",x,y,w,h,alpha<<24|0xFFFFFF);
    }
    private static String ongoing(){return "construction/ui/ongoing/frame_"+(UiText.marqueeMillis()*24/1000%12);}
    @Override protected void init() {
        queueScrollbar=null;paletteScrollbar=null;repairScrollbar=null;projectWidgets.clear();
        pane=UiPanelLayout.width(MenuTab.CONSTRUCTION,width);
        var base=UiMenuStyle.frame(WINDOW,width,pane);
        frame=minimized?new UiMenuStyle.Frame(base.x()-MINIMIZED_OFFSET*base.scale(),base.y(),base.scale()):base;
        top=Math.round(frame.y());
        repairFrame=new UiMenuStyle.Frame((float)(530*base.scale()+repairWindowX),(float)(base.y()+repairWindowY),base.scale());
        HudMenuBar.buttons(width,view==null?"":view.country(),MenuTab.CONSTRUCTION,t->{
            if(t==MenuTab.CONSTRUCTION)return;
            if(t==MenuTab.RESEARCH)HoiClient.open();else HoiClient.openMenu(t);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(UiMenuStyle.close(WINDOW,frame,this::onClose));
        var size=sourceButton(minimized?"확장":"최소화",frame,457,12,32,28,0,()->{minimized=!minimized;scroll=0;paletteScroll=0;rebuildWidgets();},
                (g,b)->art(g,frame,minimized?"construction/ui/arrow_right":"construction/ui/arrow_left",457,12,32,28,0,tint(b)));
        size.setTooltip(Tooltips.createExact(source(minimized?"CONSTRUCTION_EXPAND":"CONSTRUCTION_MINIMIZE",minimized?"확장":"최소화")));
        if(view==null)return;
        int repair=view.summary().repairPriority();
        for(int direction:new int[]{-1,1}) {
            double sx=direction<0?361:423,side=direction<0?25:26;
            if(frame.x(sx)<0)continue;
            var button=sourceButton(direction<0?"−":"+",frame,sx,13,side,side,0,()->{
                int amount=UiPriorityControls.shifted()?10:UiPriorityControls.controlled()?5:1;
                send(IndustryConstructionPayloads.Action.REPAIR,"",Math.clamp(repair+direction*amount,0,10000),null);
            },(g,b)->{
                boolean shift=UiPriorityControls.shifted(),control=!shift&&UiPriorityControls.controlled();
                String path=direction<0?shift?"commander/commands/subtract_10/frame_0":control?"commander/commands/subtract_5/frame_0":"production/line/minus"
                        :shift?"commander/commands/add_10/frame_0":control?"commander/commands/add_5/frame_0":"production/line/plus";
                art(g,frame,path,sx,13,side,side,0,tint(b));
            });
            button.active=pending==0&&(direction>0||repair>0);
            button.setTooltip(Tooltips.createExact(source(direction<0?"PRODUCTION_SUB_FACTORIES_CLICK":"PRODUCTION_ADD_FACTORIES_CLICK",direction<0?"§G클릭§!하여 §Y1§! 감소\n§GCtrl + 클릭§!하여 §Y5§! 감소\n§GShift + 클릭§!하여 §Y10§! 감소":"§G클릭§!하여 §Y+1§! 증가\n§GCtrl + 클릭§!하여 §Y+5§! 증가\n§GShift + 클릭§!하여 §Y+10§! 증가")));
        }
        if(frame.x(230)>=0) {
            var order=sourceButton(UiCommandLabels.text("REPAIR_ORDER_OPEN","우선순위"),frame,230,11,94,31,0,()->{repairWindow=!repairWindow;repairScroll=0;rebuildWidgets();},(g,b)->{
                art(g,frame,"diplomacy/dialog/clear_button/frame_0",230,11,94,31,0,tint(b));
                label(g,UiCommandLabels.text("REPAIR_ORDER_OPEN","우선순위"),frame.x(230),frame.y(26.5),frame.size(94),UiMenuStyle.TEXT,0);
            });
            order.setTooltip(Tooltips.createExact(source("REPAIR_ORDER_EXPAND_TOOLTIP","§G클릭§!하여 §Y수리 우선순위 대기열§!을 엽니다.\n목록에 표시된 유형의 손상된 건물은 목록 순서에 따라 다른 모든 건물보다 먼저 수리됩니다.")));
        }
        layoutPalette();
        if(!minimized)layoutRows();
        if(repairWindow)layoutRepairWindow();
    }
    private void layoutPalette() {
        float paletteTop=frame.y(PALETTE_TOP);
        double bottom=PALETTE_TOP;
        for(int group=0;group<BUILDING_GROUPS.size();group++) {
            long count=BUILDING_GROUPS.get(group).stream().filter(id->view.buildings().stream().anyMatch(b->b.id().equals(id))).count();
            if(count>0)bottom=Math.max(bottom,Math.max(GROUP_BACKGROUND_Y[group]+UiAssets.imageSize("construction/ui/group_"+GROUPS[group]+"_background")[1],GROUP_GRID_Y[group]+4+44*((count+1)/2)+1));
        }
        int contentBottom=Math.round(frame.y(bottom));
        paletteScrollbar=new UiScrollBar(Math.min(frame.ix(541),pane-9),Math.round(paletteTop),height-2,height-2-Math.round(paletteTop),contentBottom-Math.round(paletteTop));
        paletteScroll=Math.clamp(paletteScroll,0,paletteScrollbar.maximum());
        var order=view.repairOrder();
        for(int group=0;group<BUILDING_GROUPS.size();group++) {
            var ids=BUILDING_GROUPS.get(group);int slot=0;
            for(String id:ids) {
                var b=view.buildings().stream().filter(x->x.id().equals(id)).findFirst().orElse(null);
                if(b==null)continue;
                double sx=446+46*(slot%2),sy=GROUP_GRID_Y[group]+4+44*(slot/2);slot++;
                if(frame.x(sx)<0||frame.y(sy)-paletteScroll<paletteTop||frame.y(sy+45)-paletteScroll>height-2)continue;
                boolean repairable=!id.equals("conversion");
                var button=new UiPanelButton(b.name(),frame.ix(sx),Math.round(frame.y(sy)-paletteScroll),frame.ix(sx+45)-frame.ix(sx),Math.round(frame.y(sy+45)-paletteScroll)-Math.round(frame.y(sy)-paletteScroll),()->{
                    if(repairWindow){if(repairable&&!order.contains(id))send(IndustryConstructionPayloads.Action.REPAIR_ORDER_ADD,id,0,null);}
                    else send(IndustryConstructionPayloads.Action.SELECT,b.id(),0,null);
                }) {
                    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                        float x=frame.x(sx),y=frame.y(sy)-paletteScroll;
                        if(!b.texture().isBlank())UiAssets.drawExact(g,b.texture()+(b.enabled()?"":"_disabled"),x,y,frame.size(45),frame.size(45),0xFFFFFFFF);
                        else UiText.cell(g,font,b.name(),getX(),getY(),getWidth(),getHeight(),UiMenuStyle.TEXT,false);
                        if(!repairWindow&&b.enabled()&&view.selected().equals(b.id()))blend(g,"construction/ui/glow_overlay",frame.x(sx-1),y,frame.size(48),frame.size(48));
                        int number=order.indexOf(id);
                        if(repairWindow&&number>=0) {
                            UiAssets.drawExact(g,"construction/ui/number_circle",frame.x(sx+23),y,frame.size(22.1),frame.size(22.1),0xFFFFFFFF);
                            label(g,Integer.toString(number+1),frame.x(sx+24),y+frame.size(13),frame.size(20),UiMenuStyle.TEXT,0);
                        }
                    }
                    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
                        if(repairWindow&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT&&isMouseOver(event.x(),event.y())) {
                            if(pending==0&&order.contains(id)){playDownSound(minecraft.getSoundManager());send(IndustryConstructionPayloads.Action.REPAIR_ORDER_REMOVE,id,0,null);}
                            return true;
                        }
                        return super.mouseClicked(event,twice);
                    }
                };
                button.active=pending==0&&(repairWindow?repairable:b.enabled());
                String tooltip=b.tooltip()==null?b.name()+"\n"+b.reason():b.tooltip();
                if(repairWindow&&repairable)tooltip=b.name()+"\n\n"+source("REPAIR_ORDER_CLICK_ADD","§G클릭§!하여 §Y수리 우선순위 대기열에 추가§!")+(order.contains(id)?"\n"+source("REPAIR_ORDER_RIGHT_CLICK_REMOVE","§G우클릭§!하여 §Y수리 우선순위 대기열에서 제거§!"):"");
                button.setTooltip(Tooltips.createExact(tooltip));addRenderableWidget(button);
            }
        }
    }
    private void layoutRows() {
        var rows=rows();
        int viewportTop=listTop(),viewportBottom=listBottom();
        queueScrollbar=new UiScrollBar(frame.ix(417),viewportTop,viewportBottom,viewportBottom-viewportTop,Math.round(frame.y(ROW_TOP+ROW*rows.size()+2))-viewportTop);
        scroll=Math.clamp(scroll,0,queueScrollbar.maximum());
        for(int row=0;row<rows.size();row++) {
            var entry=rows.get(row);
            if(!entry.kind().equals("project"))continue;
            var p=view.projects().get(entry.index());
            if((rowTop(row)<viewportTop||rowBottom(row)>viewportBottom)&&!p.id().equals(draggedProject))continue;
            var widgets=new ArrayList<net.minecraft.client.gui.components.AbstractWidget>();projectWidgets.put(p.id(),widgets);
            double base=ROW_TOP+ROW*(p.id().equals(draggedProject)?row:displayRow(row)),offset=scroll;int index=entry.index();
            for(int action=0;action<3;action++) {
                var kind=action==0?IndustryConstructionPayloads.Action.UP:action==1?IndustryConstructionPayloads.Action.DOWN:IndustryConstructionPayloads.Action.CANCEL;
                String path=action==0?"construction/ui/up":action==1?"construction/ui/down":"construction/ui/cancel";
                double sx=14+(action==0?318:action==1?341:370),sw=action==2?26:22,sh=action==2?26:28;
                var button=sourceButton(action==0?"↑":action==1?"↓":"×",frame,sx,base+14,sw,sh,offset,
                        ()->send(kind,p.id(),(kind==IndustryConstructionPayloads.Action.CANCEL?UiPriorityControls.controlled():UiPriorityControls.shifted())?1:0,null),
                        (g,b)->art(g,frame,path+(b.active?"":"_disabled"),sx,base+14,sw,sh,offset,0xFFFFFFFF));
                button.active=pending==0&&(action!=0||index>0)&&(action!=1||index<view.projects().size()-1);
                button.setTooltip(Tooltips.create(action<2?UiPriorityControls.tooltip(action==0):Component.literal("§a클릭하면 §e건설이 취소§a됩니다.\nCtrl + 클릭하면 §e건설 모두 취소§a됩니다.")));widgets.add(button);
            }
            boolean amount=!p.building().startsWith("convert_")&&!p.building().equals("railway");
            if(amount)for(int step=0;step<2;step++) {
                boolean increase=step==0;
                var button=sourceButton(increase?"수량 증가":"수량 감소",frame,14+214,base+(increase?3:27),24.3,24.3,offset,
                        ()->send(IndustryConstructionPayloads.Action.AMOUNT,p.id(),increase?UiPriorityControls.shifted()?2:1:0,null),
                        (g,b)->art(g,frame,increase?"diplomacy/dialog/increase/frame_0":"diplomacy/dialog/decrease/frame_0",14+214,base+(increase?3:27),24.3,24.3,offset,tint(b)));
                button.active=pending==0;
                button.setTooltip(Tooltips.createExact(increase?source("CONSTRUCTION_INCREASE_AMOUNT_LABEL","§T수량 증가§!")+"\n"+source("CONSTRUCTION_MAXIMIZE","§GShift+클릭§!하면 §H최대화§!됩니다."):source("CONSTRUCTION_DECREASE_AMOUNT_LABEL","§T수량 감소§!")));
                widgets.add(button);
            }
        }
    }
    private void layoutRepairWindow() {
        var at=repairFrame;
        var close=sourceButton("수리 우선순위 닫기",at,258,9,32,33,0,()->{repairWindow=false;rebuildWidgets();},(g,b)->art(g,at,"menu/close",258,9,32,33,0,tint(b)));
        close.setTooltip(Tooltips.createExact(source("CLOSE","닫기")));
        boolean railway=view.repairOrder().contains("railway_gun");
        var gun=sourceButton(UiCommandLabels.text("REPAIR_ORDER_RAILWAY_GUN","열차포"),at,6,152,46,46,0,
                ()->send(railway?IndustryConstructionPayloads.Action.REPAIR_ORDER_REMOVE:IndustryConstructionPayloads.Action.REPAIR_ORDER_ADD,"railway_gun",0,null),
                (g,b)->art(g,at,"construction/ui/railway_gun",6,152,46,46,0,tint(b)));
        gun.active=pending==0;
        gun.setTooltip(Tooltips.createExact(source(railway?"REPAIR_ORDER_RAILWAY_GUN_REMOVE_TOOLTIP":"REPAIR_ORDER_RAILWAY_GUN_ADD_TOOLTIP",railway?"§G클릭§!하여 §Y열차포를 수리 우선순위 대기열에서 제거§!":"§G클릭§!하여 §Y열차포를 수리 우선순위 대기열에 추가§!")));
        int listTop=Math.round(at.y(REPAIR_LIST_TOP)),listBottom=Math.round(at.y(600-12));
        var order=view.repairOrder();
        repairScrollbar=new UiScrollBar(at.ix(277),listTop,listBottom,listBottom-listTop,Math.round(at.y(REPAIR_LIST_TOP+REPAIR_ENTRY*order.size()))-listTop);
        repairScroll=Math.clamp(repairScroll,0,repairScrollbar.maximum());
        for(int i=0;i<order.size();i++) {
            String type=order.get(i);double base=REPAIR_LIST_TOP+REPAIR_ENTRY*i;
            if(Math.round(at.y(base+11)-repairScroll)<listTop||Math.round(at.y(base+44)-repairScroll)>listBottom)continue;
            var remove=sourceButton("수리 우선순위 제거 · "+type,at,20,base+11,32,33,repairScroll,()->send(IndustryConstructionPayloads.Action.REPAIR_ORDER_REMOVE,type,0,null),
                    (g,b)->art(g,at,"menu/close",20,base+11,32,33,repairScroll,tint(b)));
            remove.active=pending==0;
            remove.setTooltip(Tooltips.createExact(source("REPAIR_ORDER_REMOVE_TOOLTIP","수리 우선순위 대기열에서 §G제거§!")));
        }
        var reset=sourceButton(UiCommandLabels.text("REPAIR_ORDER_RESET_QUEUE","대기열 초기화"),at,21,596,123,34,0,()->send(IndustryConstructionPayloads.Action.REPAIR_ORDER_RESET,"",0,null),(g,b)->{
            art(g,at,"production/design/division/reset",21,596,123,34,0,tint(b));
            label(g,UiCommandLabels.text("REPAIR_ORDER_RESET_QUEUE","대기열 초기화"),at.x(21),at.y(613),at.size(123),UiMenuStyle.TEXT,0);
        });
        reset.active=pending==0;reset.setTooltip(Tooltips.createExact(source("REPAIR_ORDER_RESET_QUEUE_TOOLTIP","수리 우선순위 대기열의 모든 항목을 제거합니다. 이미 진행 중인 수리는 현재 위치를 유지합니다.")));
        var clear=sourceButton(UiCommandLabels.text("REPAIR_ORDER_CLEAR_PLACEMENT","수동 배치 해제"),at,151,596,123,36,0,()->send(IndustryConstructionPayloads.Action.REPAIR_PLACEMENT_CLEAR,"",0,null),(g,b)->{
            art(g,at,"completion/button",151,596,123,36,0,tint(b));
            label(g,UiCommandLabels.text("REPAIR_ORDER_CLEAR_PLACEMENT","수동 배치 해제"),at.x(151),at.y(614),at.size(123),UiMenuStyle.TEXT,0);
        });
        clear.active=pending==0;clear.setTooltip(Tooltips.createExact(source("REPAIR_ORDER_CLEAR_PLACEMENT_TOOLTIP","모든 수동 배치를 해제하고 해당 수리 항목을 자동으로 지정된 위치로 되돌립니다.")));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        UiMenuStyle.window(g,WINDOW,frame,height);
        UiMenuStyle.title(g,font,WINDOW,frame,UiCommandLabels.text("CONSTRUCTION_VIEW_LABLE","건설"));
        HudMenuBar.draw(g,width,view == null ? dev.hoi.client.ClientCampaignHud.hud() : view.hud(),mx,my,hudScroll);
        if(view!=null) {
            drawSummary(g,mx,my);
            drawPalette(g,mx,my);
            if(!minimized)drawRows(g,mx,my);
            if(repairWindow)drawRepairWindow(g,mx,my);
        } else if(!loadingFailed) UiText.text(g,font,"건설 현황을 불러오는 중…",frame.ix(14),frame.iy(150),UiMenuStyle.MUTED);
        drawConversions(g,mx,my);
        var movingWidgets=projectMoved&&!draggedProject.isEmpty()?projectWidgets.getOrDefault(draggedProject,List.of()).stream().filter(widget->widget.visible).toList():List.<net.minecraft.client.gui.components.AbstractWidget>of();
        for(var widget:movingWidgets)widget.visible=false;
        super.extractRenderState(g,mx,my,delta);
        for(var widget:movingWidgets)widget.visible=true;
        if(view!=null&&projectMoved&&!draggedProject.isEmpty()) {
            int row=draggedRow();
            if(row>=0) {
                int height=rowBottom(row)-rowTop(row);
                int y=Math.clamp((int)Math.round(projectPointerY-projectOffsetY),listTop(),Math.max(listTop(),listBottom()-height));
                g.nextStratum();
                g.enableScissor(frame.ix(14),y,frame.ix(416),y+height);
                g.pose().pushMatrix();g.pose().translate(0,y-rowTop(row));
                UiMenuStyle.window(g,WINDOW,frame,this.height);frame.tile(g,WINDOW.tile(),0,130,440,windowHeight()-130,64,UiMenuStyle.TILE);
                drawRow(g,rows().get(row),row,-1,-1);
                for(var widget:movingWidgets)widget.extractRenderState(g,-1,-1,delta);
                g.pose().popMatrix();g.disableScissor();
            }
        }
    }
    private int draggedRow() {
        var rows=rows();
        for(int row=0;row<rows.size();row++) {
            var entry=rows.get(row);
            if(entry.kind().equals("project")&&draggedKind.equals("project")&&view.projects().get(entry.index()).id().equals(draggedProject))return row;
            if(entry.kind().equals("repair")&&draggedKind.equals("repair")&&view.repairs().get(entry.index()).id().equals(draggedProject))return row;
        }
        return -1;
    }
    private void drawSummary(GuiGraphicsExtractor g,int mx,int my) {
        var s=view.summary();
        art(g,frame,"construction/ui/summary_background",5,37,433,98,0,0xFFFFFFFF);
        art(g,frame,"construction/ui/number_box",365,13,80,26,0,0xFFFFFFFF);
        art(g,frame,"construction/summary/civilian_factory_repair",333,14,27,23,0,0xFFFFFFFF);
        label(g,Integer.toString(s.repairPriority()),frame.x(380),frame.y(26),frame.size(50),UiMenuStyle.TEXT,0);
        if(hover(mx,my,frame,333,13,112,26,0)&&children().stream().noneMatch(c->c instanceof Button b&&b.isMouseOver(mx,my)))
            Tooltips.drawExact(g,font,source("REPAIR_ASSIGNMENT_INFO_FACTORIES","건설보다 건물 수리를 우선하는 §H공장§! 숫자입니다."),mx,my);
        art(g,frame,"construction/summary/civilian_factory",17,55,27,23,0,0xFFFFFFFF);
        int assigned=s.used()+s.repair()+s.consumer()+s.reserved();
        label(g,assigned+"/"+s.total(),frame.x(38),frame.y(65),frame.size(87),assigned==s.total()?0xFF000000|Tooltips.GOOD:0xFF000000|Tooltips.GOLD,0);
        label(g,UiCommandLabels.text("from_trade","무역에서:"),frame.x(122),frame.y(57),frame.size(200),UiMenuStyle.TEXT,-1);
        if(s.fromTrade()!=null)label(g,Integer.toString(s.fromTrade()),frame.x(187),frame.y(57),frame.size(70),UiMenuStyle.TEXT,1);
        label(g,UiCommandLabels.text("owned","소유:"),frame.x(122),frame.y(77),frame.size(200),UiMenuStyle.TEXT,-1);
        if(s.owned()!=null)label(g,Integer.toString(s.owned()),frame.x(187),frame.y(77),frame.size(70),UiMenuStyle.TEXT,1);
        art(g,frame,"construction/speed",288,61,28,19,0,0xFFFFFFFF);
        long speed=Math.round((s.speed()-1)*100);
        label(g,String.format(Locale.ROOT,"%+d%%",speed),frame.x(278),frame.y(67),frame.size(70),speed>=0?0xFF000000|Tooltips.GOOD:0xFF000000|Tooltips.BAD,1);
        double balance=s.energy()-s.demand();
        g.pose().pushMatrix();g.pose().translate(frame.x(353),frame.y(56));g.pose().scale(frame.scale());
        UiAssets.tiledNineSlice(g,"commander/commands/mini_tooltip/frame_0",0,0,70,30,12,1);
        g.pose().popMatrix();
        art(g,frame,balance<0?"construction/energy_deficit":"construction/energy",357,58,27,27,0,0xFFFFFFFF);
        label(g,HudMenuBar.rawNumber(balance),frame.x(388),frame.y(71),frame.size(33),balance>=0?0xFF000000|Tooltips.GOOD:0xFF000000|Tooltips.BAD,-1);
        if(!minimized) {
            label(g,UiCommandLabels.text("CONSTRUCTION_LOCATION_LABEL","위치"),frame.x(20),frame.y(113),frame.size(150),UiMenuStyle.TEXT,0);
            label(g,UiCommandLabels.text("CONSTRUCTION_TASK_LABEL","작업"),frame.x(180),frame.y(113),frame.size(80),UiMenuStyle.TEXT,0);
            art(g,frame,"construction/summary/civilian_factory",274,106,27,23,0,0xFFFFFFFF);
            label(g,UiCommandLabels.text("CONSTRUCTION_FACTORIES_LABEL","#"),frame.x(250),frame.y(113),frame.size(120),UiMenuStyle.TEXT,0);
            label(g,UiCommandLabels.text("CONSTRUCTION_PRIORITY_LABEL","우선순위"),frame.x(330),frame.y(113),frame.size(110),UiMenuStyle.TEXT,0);
            if(hover(mx,my,frame,14,104,426,20,0)) {
                double source=(mx-frame.x(0))/frame.scale();
                String header=source<175?"location":source<255?"task":source<325?"factories":"priority";
                Tooltips.drawExact(g,font,Component.translatable("hoi.construction.header."+header).getString(),mx,my);
            }
            if(hover(mx,my,frame,17,50,104,30,0))Tooltips.drawExact(g,font,calculation(s.factoryCalculation(),"사용 중인 공장"),mx,my);
            else if(hover(mx,my,frame,278,56,72,30,0))Tooltips.drawExact(g,font,calculation(s.speedCalculation(),"건설 속도"),mx,my);
            else if(hover(mx,my,frame,353,56,70,30,0))Tooltips.drawExact(g,font,calculation(s.energyCalculation(),"사용 가능한 에너지"),mx,my);
        }
    }
    private boolean hover(int mx,int my,UiMenuStyle.Frame at,double sx,double sy,double sw,double sh,double offset) {
        return mx>=at.x(sx)&&mx<at.x(sx+sw)&&my>=at.y(sy)-offset&&my<at.y(sy+sh)-offset;
    }
    private void drawPalette(GuiGraphicsExtractor g,int mx,int my) {
        int top=Math.round(frame.y(PALETTE_TOP));
        g.enableScissor(0,top,width,height-2);
        for(int group=0;group<GROUPS.length;group++) {
            String name=GROUPS[group];
            int[] size=UiAssets.imageSize("construction/ui/group_"+name+"_background");
            art(g,frame,"construction/ui/group_"+name+"_background",438,GROUP_BACKGROUND_Y[group],size[0],size[1],paletteScroll,0xFFFFFFFF);
            int[] icon=UiAssets.imageSize("construction/ui/group_"+name);
            art(g,frame,"construction/ui/group_"+name,443,GROUP_ICON_Y[group],icon[0],icon[1],paletteScroll,0xFFFFFFFF);
            if(my>=top&&hover(mx,my,frame,443,GROUP_ICON_Y[group],icon[0],icon[1],paletteScroll))
                Tooltips.drawExact(g,font,Component.translatable("hoi.construction.group."+name).getString(),mx,my);
        }
        g.disableScissor();
        if(paletteScrollbar!=null&&paletteScrollbar.maximum()>0)paletteScrollbar.draw(g,paletteScroll);
    }
    private void drawRows(GuiGraphicsExtractor g,int mx,int my) {
        frame.tile(g,WINDOW.tile(),0,130,440,windowHeight()-130,64,UiMenuStyle.TILE);
        var rows=rows();
        g.enableScissor(0,listTop(),width,listBottom());
        for(int row=0;row<rows.size();row++) {
            if(rowBottom(row)<listTop()||rowTop(row)>listBottom())continue;
            var entry=rows.get(row);
            boolean moving=projectMoved&&!draggedProject.isEmpty()&&row==draggedRow();
            if(!moving)drawRow(g,entry,displayRow(row),mx,my);
        }
        g.disableScissor();
        if(queueScrollbar!=null&&queueScrollbar.maximum()>0)queueScrollbar.draw(g,scroll);
    }
    private int displayRow(int row) {
        if(!projectMoved||draggedProject.isEmpty())return row;
        int source=draggedRow();if(source<0)return row;
        var rows=rows();String kind=rows.get(source).kind();
        int first=-1,last=-1;
        for(int i=0;i<rows.size();i++)if(rows.get(i).kind().equals(kind)){if(first<0)first=i;last=i;}
        int target=Math.clamp((int)Math.floor((projectPointerY+scroll-frame.y(ROW_TOP))/(ROW*frame.scale())),first,last);
        if(row==source)return target;
        if(source<target&&row>source&&row<=target)return row-1;
        if(source>target&&row>=target&&row<source)return row+1;
        return row;
    }
    private void drawRow(GuiGraphicsExtractor g,IndustryConstructionRow entry,int row,int mx,int my) {
        float s=frame.scale(),y=frame.y(ROW_TOP+ROW*row)-scroll,x=frame.x(14);
        java.util.function.DoubleUnaryOperator rx=n->x+n*s,ry=n->y+n*s;
        var summary=view.summary();
        switch(entry.kind()) {
            case "consumer","trade","projects" -> {
                String name=entry.kind().equals("consumer")?UiCommandLabels.text("PRODUCTION_CONSUMER_GOODS","소비재"):entry.kind().equals("trade")?UiCommandLabels.text("CONSTRUCTION_FACTORIES_TRADE_LABEL","무역 상품"):UiCommandLabels.text("CONSTRUCTION_FACTORIES_PROJECTS_LABEL","국가 계획");
                String icon=entry.kind().equals("consumer")?"construction/consumer_goods":entry.kind().equals("trade")?"construction/ui/traded_factories":"construction/ui/national_projects";
                int count=entry.kind().equals("consumer")?summary.consumer():entry.kind().equals("trade")?summary.tradeGoods():summary.nationalProjects();
                String detail=entry.kind().equals("consumer")?summary.consumerCalculation():entry.kind().equals("trade")?summary.tradeCalculation():summary.projectsCalculation();
                UiAssets.drawExact(g,"construction/ui/special_entry",x,y,402*s,54*s,0xFFFFFFFF);
                label(g,name,(float)rx.applyAsDouble(20),(float)ry.applyAsDouble(26),entry.kind().equals("consumer")?150*s:250*s,UiMenuStyle.TEXT,-1);
                int[] size=UiAssets.imageSize(icon);
                double centerY=entry.kind().equals("trade")?25:26;
                UiAssets.drawExact(g,icon,(float)rx.applyAsDouble(210-size[0]/2.0),(float)ry.applyAsDouble(centerY-size[1]/2.0),size[0]*s,size[1]*s,0xFFFFFFFF);
                label(g,Integer.toString(count),(float)rx.applyAsDouble(254),(float)ry.applyAsDouble(26),50*s,UiMenuStyle.TEXT,0);
                if(mx>=x&&mx<rx.applyAsDouble(402)&&my>=y&&my<ry.applyAsDouble(ROW))Tooltips.drawExact(g,font,detail==null?name:detail,mx,my);
            }
            case "repair" -> {
                var r=view.repairs().get(entry.index());
                UiAssets.drawExact(g,"construction/ui/entry",x,y,402*s,54*s,0xFFFFFFFF);
                label(g,r.name(),(float)rx.applyAsDouble(20),(float)ry.applyAsDouble(24),150*s,UiMenuStyle.TEXT,-1);
                UiAssets.drawExact(g,"construction/"+r.building(),(float)rx.applyAsDouble(172),(float)ry.applyAsDouble(5),45*s,45*s,0xFFFFFFFF);
                blend(g,"construction/ui/repair",(float)rx.applyAsDouble(174),(float)ry.applyAsDouble(7),42*s,42*s);
                if(r.factories()>0)UiAssets.drawExact(g,ongoing(),(float)rx.applyAsDouble(254),(float)ry.applyAsDouble(9),54*s,20*s,0xFFFFFFFF);
                label(g,r.factories()+"/"+LINE_FACTORIES,(float)rx.applyAsDouble(254),(float)ry.applyAsDouble(19),50*s,UiMenuStyle.TEXT,0);
                progress(g,x,y,r.level()<=0?0:Math.clamp((r.level()-r.damage())/r.level(),0,1));
                if(r.pinned())UiAssets.drawExact(g,"production/designer/locked",(float)rx.applyAsDouble(345),(float)ry.applyAsDouble(6),76*s,42*s,0xFFFFFFFF);
                UiAssets.drawExact(g,"construction/ui/number_circle",(float)rx.applyAsDouble(372),(float)ry.applyAsDouble(16),22.1f*s,22.1f*s,0xFFFFFFFF);
                label(g,Integer.toString(r.order()),(float)rx.applyAsDouble(373),(float)ry.applyAsDouble(29),20*s,UiMenuStyle.TEXT,0);
                if(mx>=rx.applyAsDouble(372)&&mx<rx.applyAsDouble(394)&&my>=ry.applyAsDouble(16)&&my<ry.applyAsDouble(38))
                    Tooltips.drawExact(g,font,source("REPAIR_ORDER_LINE_NUMBER_TOOLTIP","§Y수리 우선순위 대기열§!에서 이 수리 항목이 차지하는 위치입니다. 해당 위치는 자동으로 관리됩니다. 수동으로 배치하려면 같은 유형의 수리 항목들과 떨어진 위치로 드래그하십시오."),mx,my);
                else if(r.pinned()&&mx>=rx.applyAsDouble(360)&&mx<rx.applyAsDouble(372)&&my>=ry.applyAsDouble(14)&&my<ry.applyAsDouble(40))
                    Tooltips.drawExact(g,font,source("CONSTRUCTION_MANUALLY_PLACED_TOOLTIP","이 수리 항목은 수동으로 배치되어 지정된 우선순위를 유지합니다. 자동 수리 우선순위 규칙에 맡기려면 같은 유형의 수리 항목들 사이로 다시 드래그하십시오."),mx,my);
                else if(mx>=x&&mx<rx.applyAsDouble(402)&&my>=y&&my<ry.applyAsDouble(ROW))
                    Tooltips.drawExact(g,font,source("PRODUCTION_DRAG_N_DROP","§G드래그 앤 드롭§!으로 §Y우선순위§!를 변경할 수 있습니다."),mx,my);
            }
            default -> {
                var p=view.projects().get(entry.index());
                UiAssets.drawExact(g,"construction/ui/entry",x,y,402*s,54*s,0xFFFFFFFF);
                label(g,p.name(),(float)rx.applyAsDouble(20),(float)ry.applyAsDouble(24),150*s,UiMenuStyle.TEXT,-1);
                UiAssets.drawExact(g,"construction/"+p.building(),(float)rx.applyAsDouble(172),(float)ry.applyAsDouble(5),45*s,45*s,0xFFFFFFFF);
                label(g,Integer.toString(p.levels()),(float)rx.applyAsDouble(162),(float)ry.applyAsDouble(39),50*s,UiMenuStyle.TEXT,1);
                if(p.factories()>0)UiAssets.drawExact(g,ongoing(),(float)rx.applyAsDouble(254),(float)ry.applyAsDouble(9),54*s,20*s,0xFFFFFFFF);
                label(g,p.factories()+"/"+LINE_FACTORIES,(float)rx.applyAsDouble(254),(float)ry.applyAsDouble(19),50*s,UiMenuStyle.TEXT,0);
                progress(g,x,y,p.cost()==0?0:p.progress()/p.cost());
                if(mx>=rx.applyAsDouble(172)&&mx<rx.applyAsDouble(214)&&my>=ry.applyAsDouble(5)&&my<ry.applyAsDouble(50)&&p.taskTooltip()!=null)Tooltips.drawExact(g,font,p.taskTooltip(),mx,my);
                else if(mx>=rx.applyAsDouble(249)&&mx<rx.applyAsDouble(311)&&my>=y&&my<ry.applyAsDouble(ROW)&&p.tooltip()!=null)Tooltips.drawExact(g,font,p.tooltip(),mx,my);
                else if(mx>=x&&mx<rx.applyAsDouble(402)&&my>=y&&my<ry.applyAsDouble(ROW)&&children().stream().noneMatch(child->child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget.isMouseOver(mx,my)))
                    Tooltips.drawExact(g,font,source("PRODUCTION_DRAG_N_DROP","§G드래그 앤 드롭§!으로 §Y우선순위§!를 변경할 수 있습니다."),mx,my);
            }
        }
    }
    private void progress(GuiGraphicsExtractor g,float x,float y,double ratio) {
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(frame.scale());
        IndustryProductionRender.startEnd(g,"construction/ui/progress","construction/ui/progress_background",249,36,62,9,ratio);
        g.pose().popMatrix();
    }
    private void drawRepairWindow(GuiGraphicsExtractor g,int mx,int my) {
        var at=repairFrame;
        g.nextStratum();
        at.tile(g,"commander/commands/tiled_plain_bg/frame_0",0,0,295,640,64,UiMenuStyle.TILE);
        label(g,UiCommandLabels.text("REPAIR_ORDER_HEADER","수리 우선순위"),at.x(0),at.y(19),at.size(295),UiMenuStyle.TEXT,0);
        paragraph(g,Tooltips.officerText(source("REPAIR_ORDER_WINDOW_DESCRIPTION","먼저 수리할 건물의 우선순위를 설정합니다.")),at.ix(10),at.iy(42),Math.round(at.size(260)),UiMenuStyle.TEXT);
        paragraph(g,Tooltips.officerText(source("REPAIR_ORDER_WINDOW_INSTRUCTION","건설 목록의 건물을 §G클릭§!하여 §Y이 대기열에 추가§!\n§G우클릭§!하여 §Y이 대기열에서 제거§!.")),at.ix(10),at.iy(86),Math.round(at.size(260)),UiMenuStyle.TEXT);
        var order=view.repairOrder();
        int listTop=Math.round(at.y(REPAIR_LIST_TOP)),listBottom=Math.round(at.y(600-12));
        g.enableScissor(at.ix(0),listTop,at.ix(296),listBottom);
        for(int i=0;i<order.size();i++) {
            if(orderMoved&&order.get(i).equals(draggedOrder))continue;
            drawOrderEntry(g,order.get(i),displayOrder(i),mx,my);
        }
        g.disableScissor();
        if(repairScrollbar!=null&&repairScrollbar.maximum()>0)repairScrollbar.draw(g,repairScroll);
        if(orderMoved&&!draggedOrder.isEmpty()) {
            float y=(float)Math.clamp(orderPointerY-orderDragY,listTop,Math.max(listTop,listBottom-at.size(REPAIR_ENTRY)));
            g.pose().pushMatrix();g.pose().translate(0,y-(at.y(REPAIR_LIST_TOP+REPAIR_ENTRY*order.indexOf(draggedOrder))-repairScroll));
            drawOrderEntry(g,draggedOrder,order.indexOf(draggedOrder),-1,-1);
            g.pose().popMatrix();
        }
    }
    private void paragraph(GuiGraphicsExtractor g,Component text,int x,int y,int width,int color) {
        for(var line:font.split(text,Math.max(1,width))){UiText.text(g,font,line,x,y,color);y+=font.lineHeight;}
    }
    private int displayOrder(int index) {
        if(!orderMoved||draggedOrder.isEmpty())return index;
        var order=view.repairOrder();int source=order.indexOf(draggedOrder);
        int target=Math.clamp((int)Math.floor((orderPointerY+repairScroll-repairFrame.y(REPAIR_LIST_TOP))/(REPAIR_ENTRY*repairFrame.scale())),0,order.size()-1);
        if(source<target&&index>source&&index<=target)return index-1;
        if(source>target&&index>=target&&index<source)return index+1;
        return index;
    }
    private void drawOrderEntry(GuiGraphicsExtractor g,String type,int index,int mx,int my) {
        var at=repairFrame;double base=REPAIR_LIST_TOP+REPAIR_ENTRY*index;
        g.pose().pushMatrix();g.pose().translate(at.x(12),at.y(base)-repairScroll);g.pose().scale(at.scale());
        UiAssets.tiledNineSlice(g,"doctrine/gfx_tiled_decisions_bg_small",0,0,260,53,16,1,48);
        g.pose().popMatrix();
        UiAssets.drawExact(g,"construction/ui/number_circle",at.x(60),at.y(base+18)-repairScroll,at.size(22.1),at.size(22.1),0xFFFFFFFF);
        label(g,Integer.toString(index+1),at.x(62),at.y(base+29)-repairScroll,at.size(20),UiMenuStyle.TEXT,0);
        String icon=type.equals("railway_gun")?"construction/ui/railway_gun":"construction/"+type;
        UiAssets.drawExact(g,icon,at.x(90),at.y(base+11)-repairScroll,at.size(32.2),at.size(32.2),0xFFFFFFFF);
        String name=type.equals("railway_gun")?UiCommandLabels.text("REPAIR_ORDER_RAILWAY_GUN","열차포"):view.buildings().stream().filter(b->b.id().equals(type)).map(Building::name).findFirst().orElse(type);
        label(g,name,at.x(132),at.y(base+27)-repairScroll,at.size(130),UiMenuStyle.TEXT,-1);
        if(mx>=at.x(12)&&mx<at.x(272)&&my>=at.y(base)-repairScroll&&my<at.y(base+REPAIR_ENTRY)-repairScroll&&children().stream().noneMatch(c->c instanceof Button b&&b.isMouseOver(mx,my)))
            Tooltips.drawExact(g,font,source("PRODUCTION_DRAG_N_DROP","§G드래그 앤 드롭§!으로 §Y우선순위§!를 변경할 수 있습니다."),mx,my);
    }
    private boolean insideRepairWindow(double x,double y) {
        return repairWindow&&x>=repairFrame.x(0)&&x<repairFrame.x(295)&&y>=repairFrame.y(0)&&y<repairFrame.y(640);
    }
    private void drawConversions(GuiGraphicsExtractor g,int mx,int my) {
        conversionBoxes=List.of();
        if(view==null||!view.selected().equals("conversion")||minecraft.player==null)return;
        var camera=minecraft.gameRenderer.mainCamera();var matrix=camera.getViewRotationProjectionMatrix(new Matrix4f());
        var boxes=new ArrayList<IndustryConversionBox>();
        for(var state:view.conversions()) {
            var relative=new net.minecraft.world.phys.Vec3(state.x(),state.y(),state.z()).subtract(camera.position());
            var forward=camera.forwardVector();
            if(relative.x*forward.x()+relative.y*forward.y()+relative.z*forward.z()<=0)continue;
            var point=matrix.transformProject(relative.toVector3f());
            if(!point.isFinite()||Math.abs(point.x)>1||Math.abs(point.y)>1)continue;
            int x=Math.round((point.x+1)*width/2)-53,y=Math.round((1-point.y)*height/2)-13;
            if(x<panelRight()||x+106>HudMenuBar.statsRight(width)||y<top+24||y+26>height-24||insideRepairWindow(x,y)||insideRepairWindow(x+106,y+26))continue;
            boxes.add(new IndustryConversionBox(state,x,y));
            UiAssets.draw(g,"construction/conversion_background",x,y,106,26);
            UiAssets.draw(g,"construction/conversion_civilian",x+6,y+3,20,20);
            UiText.cell(g,font,Integer.toString(state.civilian()),x+26,y+3,18,20,0xFFFFFFFF,false);
            UiText.cell(g,font,Integer.toString(state.military()),x+62,y+3,16,20,0xFFFFFFFF,false);
            UiAssets.draw(g,"construction/conversion_military",x+78,y+3,20,20);
            if(mx>=x&&mx<x+106&&my>=y&&my<y+26)Tooltips.draw(g,font,state.name()+"\n민간공장 클릭: 민간공장으로 전환\n군수공장 클릭: 군수공장으로 전환\n반대 방향 예약이 있으면 하나 취소합니다.\n숫자는 전환 예약을 포함합니다.",mx,my);
        }
        conversionBoxes=List.copyOf(boxes);
    }
    private String calculation(String explanation,String label) {
        return explanation==null||explanation.isBlank()?label+"\n계산 내역이 제공되지 않았습니다.":explanation;
    }
    private void send(IndustryConstructionPayloads.Action action,String item,int amount,Vector3f ray) {
        if(pending>0&&action!=IndustryConstructionPayloads.Action.CLOSE)return;
        boolean passive=action==IndustryConstructionPayloads.Action.REFRESH;
        if(!passive){draggedProject="";draggedOrder="";}
        if(action==IndustryConstructionPayloads.Action.OPEN)loadingFailed=false;
        if(!passive)pending=view==null?200:100;transport.accept(new IndustryConstructionPayloads.Request(action,token,view==null?0:view.revision(),item,amount,ray==null?0:ray.x,ray==null?0:ray.y,ray==null?0:ray.z));
        if(!passive)rebuildWidgets();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(!draggedProject.isEmpty()){draggedProject="";rebuildWidgets();return true;}
        if(!draggedOrder.isEmpty()){draggedOrder="";rebuildWidgets();return true;}
        boolean left=event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
        if(insideRepairWindow(event.x(),event.y())) {
            if(super.mouseClicked(event,twice))return true;
            if(left&&repairScrollbar!=null&&repairScrollbar.maximum()>0&&repairScrollbar.contains(event.x(),event.y())) {
                if(repairScrollbar.arrow(event.y()))repairScroll=repairScrollbar.step(event.y(),repairScroll,Math.round(repairFrame.size(REPAIR_ENTRY)));
                else {repairDragging=true;repairScroll=repairScrollbar.offset(event.y());}
                rebuildWidgets();return true;
            }
            int listTop=Math.round(repairFrame.y(REPAIR_LIST_TOP)),listBottom=Math.round(repairFrame.y(600-12));
            if(left&&pending==0&&event.y()>=listTop&&event.y()<listBottom&&event.x()>=repairFrame.x(12)&&event.x()<repairFrame.x(272)) {
                int index=(int)Math.floor((event.y()+repairScroll-repairFrame.y(REPAIR_LIST_TOP))/repairFrame.size(REPAIR_ENTRY));
                if(index>=0&&index<view.repairOrder().size()) {
                    draggedOrder=view.repairOrder().get(index);orderPointerY=event.y();orderDragY=event.y()-(repairFrame.y(REPAIR_LIST_TOP+REPAIR_ENTRY*index)-repairScroll);orderMoved=false;return true;
                }
            }
            if(left){repairWindowMoving=true;repairGrabX=event.x()-repairFrame.x();repairGrabY=event.y()-repairFrame.y();}
            return true;
        }
        if(view!=null&&!minimized&&pending==0&&left&&event.x()>=frame.x(14)&&event.x()<frame.x(14+210)&&event.y()>=listTop()&&event.y()<listBottom()) {
            var rows=rows();
            for(int row=0;row<rows.size();row++) {
                var entry=rows.get(row);
                if(event.y()<rowTop(row)||event.y()>=rowBottom(row)||rowTop(row)<listTop()||rowBottom(row)>listBottom())continue;
                if(entry.kind().equals("project")){draggedProject=view.projects().get(entry.index()).id();draggedKind="project";}
                else if(entry.kind().equals("repair")){draggedProject=view.repairs().get(entry.index()).id();draggedKind="repair";}
                else break;
                projectDragX=event.x();projectDragY=projectPointerY=event.y();projectOffsetY=event.y()-rowTop(row);projectMoved=false;return true;
            }
        }
        if(left) {
            if(queueScrollbar!=null&&queueScrollbar.maximum()>0&&queueScrollbar.contains(event.x(),event.y())) {
                if(queueScrollbar.arrow(event.y()))scroll=queueScrollbar.step(event.y(),scroll,Math.round(frame.size(ROW)));
                else {queueDragging=true;scroll=queueScrollbar.offset(event.y());}
                rebuildWidgets();return true;
            }
            if(paletteScrollbar!=null&&paletteScrollbar.maximum()>0&&paletteScrollbar.contains(event.x(),event.y())) {
                if(paletteScrollbar.arrow(event.y()))paletteScroll=paletteScrollbar.step(event.y(),paletteScroll,Math.round(frame.size(44)));
                else {paletteDragging=true;paletteScroll=paletteScrollbar.offset(event.y());}
                rebuildWidgets();return true;
            }
        }
        if(view!=null&&left&&!minimized&&hover((int)event.x(),(int)event.y(),frame,353,56,70,30,0)) {
            HoiClient.openMenu(MenuTab.TRADE);return true;
        }
        if(view!=null&&view.selected().equals("conversion")&&event.x()>=panelRight()) {
            if(left&&pending==0)for(var box:conversionBoxes.reversed()) {
                if(event.y()<box.y()||event.y()>=box.y()+26)continue;
                if(event.x()>=box.x()+6&&event.x()<box.x()+26) {dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(IndustryConstructionPayloads.Action.CONVERT,box.state().state(),0,null);return true;}
                if(event.x()>=box.x()+78&&event.x()<box.x()+98) {dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(IndustryConstructionPayloads.Action.CONVERT,box.state().state(),1,null);return true;}
            }
            if(event.y()>top+22&&event.x()<HudMenuBar.statsRight(width))return true;
        }
        if(super.mouseClicked(event,twice))return true;
        if((left||event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT)&&event.x()>=panelRight()&&event.y()>top+22&&event.x()<HudMenuBar.statsRight(width)) {
            if(view!=null&&pending==0&&minecraft.player!=null) {
                if(!minecraft.options.getCameraType().isFirstPerson()) {
                    return true;
                }
                var matrix=minecraft.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new Matrix4f()).invert();
                var ray=matrix.transformProject(new Vector3f((float)(event.x()/width*2-1),(float)(1-event.y()/height*2),1)).normalize();
                send(left?IndustryConstructionPayloads.Action.PLACE:IndustryConstructionPayloads.Action.CANCEL_AT,"",UiPriorityControls.shifted()?1:0,ray);
            }
            return true;
        }
        return false;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(y<HudMenuBar.height(width)&&view!=null){hudScroll=HudMenuBar.scroll(width,view.hud(),hudScroll,horizontal,vertical);return true;}
        if(insideRepairWindow(x,y)) {
            if(repairScrollbar!=null){repairScroll-=(int)(vertical*Math.round(repairFrame.size(REPAIR_ENTRY)));rebuildWidgets();}
            return true;
        }
        if(x>=frame.x(438)&&x<panelRight()&&paletteScrollbar!=null&&y>=paletteScrollbar.top()&&y<paletteScrollbar.bottom()) {paletteScroll-=(int)(vertical*Math.round(frame.size(44)));rebuildWidgets();return true;}
        if(!minimized&&x>=0&&x<frame.x(438)&&queueScrollbar!=null&&y>=queueScrollbar.top()&&y<queueScrollbar.bottom()) {scroll-=(int)(vertical*Math.round(frame.size(ROW)));rebuildWidgets();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if(!draggedOrder.isEmpty()) {
            orderPointerY=event.y();orderMoved|=Math.abs(event.y()-orderDragY-orderPointerY)>=0&&Math.abs(dy)>0;
            return true;
        }
        if(repairWindowMoving) {
            repairWindowX=Math.clamp(event.x()-repairGrabX-530*frame.scale(),-530*frame.scale(),width-repairFrame.size(295)-530*frame.scale());
            repairWindowY=Math.clamp(event.y()-repairGrabY-frame.y(),HudMenuBar.height(width)-frame.y(),height-repairFrame.size(40)-frame.y());
            rebuildWidgets();return true;
        }
        if(!draggedProject.isEmpty()) {
            projectPointerY=event.y();
            projectMoved|=Math.hypot(event.x()-projectDragX,event.y()-projectDragY)>=4;
            if(projectMoved&&event.x()>=frame.x(14)&&event.x()<frame.x(416)) {
                if(event.y()<listTop()+12)scroll-=Math.round(frame.size(ROW));else if(event.y()>listBottom()-12)scroll+=Math.round(frame.size(ROW));
                rebuildWidgets();
            }
            return true;
        }
        if(queueDragging&&queueScrollbar!=null){scroll=queueScrollbar.offset(event.y());rebuildWidgets();return true;}
        if(paletteDragging&&paletteScrollbar!=null){paletteScroll=paletteScrollbar.offset(event.y());rebuildWidgets();return true;}
        if(repairDragging&&repairScrollbar!=null){repairScroll=repairScrollbar.offset(event.y());rebuildWidgets();return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        boolean left=event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
        if(!draggedOrder.isEmpty()) {
            String type=draggedOrder;boolean moved=orderMoved;draggedOrder="";orderMoved=false;
            if(left&&moved&&pending==0&&insideRepairWindow(event.x(),event.y())) {
                int index=Math.clamp((int)Math.floor((event.y()+repairScroll-repairFrame.y(REPAIR_LIST_TOP))/repairFrame.size(REPAIR_ENTRY)),0,view.repairOrder().size()-1);
                if(index!=view.repairOrder().indexOf(type)){dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(IndustryConstructionPayloads.Action.REPAIR_ORDER_MOVE,type,index,null);return true;}
            }
            rebuildWidgets();return true;
        }
        if(repairWindowMoving){repairWindowMoving=false;return true;}
        if(!draggedProject.isEmpty()) {
            String id=draggedProject,kind=draggedKind;draggedProject="";rebuildWidgets();
            if(left&&projectMoved&&pending==0&&event.x()>=frame.x(14)&&event.x()<frame.x(416)&&event.y()>=listTop()&&event.y()<listBottom()) {
                var rows=rows();int first=-1,last=-1;
                for(int i=0;i<rows.size();i++)if(rows.get(i).kind().equals(kind)){if(first<0)first=i;last=i;}
                int row=Math.clamp((int)Math.floor((event.y()+scroll-frame.y(ROW_TOP))/(ROW*frame.scale())),first,last),index=rows.get(row).index();
                if(kind.equals("project")&&!view.projects().get(index).id().equals(id)){dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(IndustryConstructionPayloads.Action.MOVE,id,index,null);}
                if(kind.equals("repair")&&!view.repairs().get(index).id().equals(id)){dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(IndustryConstructionPayloads.Action.REPAIR_PLACE,id,index,null);}
            }
            return true;
        }
        boolean dragging=queueDragging||paletteDragging||repairDragging;queueDragging=false;paletteDragging=false;repairDragging=false;
        return super.mouseReleased(event)||dragging;
    }
    @Override public void tick() {
        if(pending>0&&--pending==0){
            if(view==null){loadingFailed=true;rebuildWidgets();}
            else send(IndustryConstructionPayloads.Action.REFRESH,"",0,null);
        }
        else if(view!=null&&++refresh%60==0&&pending==0)send(IndustryConstructionPayloads.Action.REFRESH,"",0,null);
    }
    @Override public boolean keyPressed(KeyEvent e){
        if(e.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE&&(!draggedProject.isEmpty()||!draggedOrder.isEmpty())){draggedProject="";draggedOrder="";rebuildWidgets();return true;}
        if(e.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE&&repairWindow){repairWindow=false;rebuildWidgets();return true;}
        return InputSidebarMovement.consumes(minecraft,e)||super.keyPressed(e);
    }
    @Override public boolean keyReleased(KeyEvent e){return InputSidebarMovement.consumes(minecraft,e)||super.keyReleased(e);}
    @Override public void removed(){draggedProject="";InputSidebarMovement.release(minecraft);if(DialogCoordinator.suspending())return;transport.accept(new IndustryConstructionPayloads.Request(IndustryConstructionPayloads.Action.CLOSE,token,view==null?0:view.revision(),"",0,0,0,0));}
    @Override public int unitHudLeft(){return panelRight();}
    @Override public boolean allowsMovement(){return true;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}
