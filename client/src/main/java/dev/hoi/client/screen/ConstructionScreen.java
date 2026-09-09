package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.ui.PanelButton;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.HoiMenuButton;
import dev.hoi.client.ui.HoiMenuStyle;
import dev.hoi.client.ui.HoiPanelLayout;
import dev.hoi.client.ui.HoiTooltips;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.*;
import dev.hoi.protocol.ConstructionView.*;
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


public final class ConstructionScreen extends Screen implements SidebarMovement.Screen {

    private static final List<List<String>> BUILDING_GROUPS=List.of(
            List.of("infrastructure","air_base","anti_air","radar"),
            List.of("military_factory","civilian_factory","dockyard","office_park","refinery","fuel_silo","nuclear_reactor","power_plant","energy_farm"),
            List.of("hub","railway","port","fort","coastal_fort"));
    private record PaletteEntry(Building building,int column,int offset) {}
    private final String token;
    private final Consumer<ConstructionProtocol.Request> transport;
    private ConstructionView view;
    private int pane,top,scroll,paletteScroll,pending,refresh,hudScroll;
    private List<Integer> paletteDividers=List.of();
    public ConstructionScreen() {
        this(UUID.randomUUID().toString(),null,r->{if(ClientPlayNetworking.canSend(ConstructionProtocol.Request.TYPE))ClientPlayNetworking.send(r);});
    }
    ConstructionScreen(String token,ConstructionView fixture,Consumer<ConstructionProtocol.Request> transport) {
        super(Component.literal("건설"));this.token=token;this.view=fixture;this.transport=transport;
    }
    public void open(){send(ConstructionProtocol.Action.OPEN,"",0,null);}
    public void update(ConstructionView next) {
        if(!next.session().equals(token))return;
        if(next.country().isEmpty()){minecraft.gui.setScreen(null);return;}
        if(view!=null&&next.revision()<view.revision())return;
        view=next;pending=0;rebuildWidgets();
    }
    int panelWidth(){return pane;}
    @Override protected void init() {
        pane=HoiPanelLayout.width(MenuTab.CONSTRUCTION,width);top=HoiMenuBar.height(width);
        HoiMenuBar.buttons(width,view==null?"":view.country(),MenuTab.CONSTRUCTION,t->{
            if(t==MenuTab.CONSTRUCTION)return;
            if(t==MenuTab.RESEARCH)HoiClient.open();else HoiClient.openMenu(t);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new HoiMenuButton("×",pane-25,top+3,19,19,this::onClose));
        if(view==null)return;
        int icon=paletteSize(),x=paletteX(),y=top+72;
        var entries=new ArrayList<PaletteEntry>();
        var dividers=new ArrayList<Integer>();
        int offset=0;
        for(var ids:BUILDING_GROUPS) {
            var group=ids.stream().flatMap(id->view.buildings().stream().filter(b->b.id().equals(id))).toList();
            if(group.isEmpty())continue;
            if(!entries.isEmpty()){dividers.add(offset+2);offset+=7;}
            for(int i=0;i<group.size();i++)entries.add(new PaletteEntry(group.get(i),i%2,offset+(i/2)*(icon+3)));
            offset+=((group.size()+1)/2)*(icon+3);
        }
        paletteDividers=List.copyOf(dividers);
        paletteScroll=Math.clamp(paletteScroll,0,Math.max(0,offset-3-Math.max(0,height-y-30)));
        for(var entry:entries) {
            var b=entry.building();int bx=x+entry.column()*(icon+2),by=y+entry.offset()-paletteScroll;
            if(by<y||by+icon>height-30)continue;
            var button=new PanelButton(b.name(),bx,by,icon,icon,()->send(ConstructionProtocol.Action.SELECT,b.id(),0,null)) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    HoiMenuStyle.control(g,bx,by,icon,icon,view.selected().equals(b.id()),isHoveredOrFocused());
                    UiAssets.draw(g,b.texture(),bx+2,by+2,icon-4,icon-4);
                    if(!b.enabled())g.fill(bx+1,by+1,bx+icon-1,by+icon-1,0x88000000);
                }
            };
            button.active=b.enabled()&&pending==0;
            button.setTooltip(Tooltip.create(Component.literal(b.name()+"\n"+(b.enabled()?b.id().equals("railway")?"인접한 두 프로빈스 좌클릭: 철도 건설·증설 · 우클릭: 예약 취소":"자국 주 좌클릭: 건설 · 우클릭: 선택한 건물의 마지막 건설 예약 취소":b.reason()))));addRenderableWidget(button);
        }
        int repair=view.summary().repairPriority();
        for(int direction:new int[]{-1,1}) {
            var button=new HoiMenuButton(direction<0?"−":"+",direction<0?pane-101:pane-47,top+3,21,19,
                    ()->send(ConstructionProtocol.Action.REPAIR,"",Math.clamp(repair+direction,0,10000),null));
            button.active=pending==0&&(direction>0||repair>0);button.setTooltip(Tooltip.create(Component.literal("건설보다 수리를 우선하는 공장 수")));addRenderableWidget(button);
        }
        int visibleRows=Math.max(1,(height-queueY()-30)/rowHeight());
        scroll=Math.clamp(scroll/rowHeight(),0,Math.max(0,view.projects().size()-visibleRows))*rowHeight();
        for(int i=0;i<view.projects().size();i++) {
            var p=view.projects().get(i);int by=queueY()+i*rowHeight()-scroll;
            if(by<queueY()||by+rowHeight()>height-30)continue;
            for(int action=0;action<3;action++) {
                var kind=action==0?ConstructionProtocol.Action.UP:action==1?ConstructionProtocol.Action.DOWN:ConstructionProtocol.Action.CANCEL;
                var button=new HoiMenuButton(action==0?"↑":action==1?"↓":"×",x-63+action*19,by+rowHeight()-23,18,18,()->send(kind,p.id(),0,null));
                button.active=pending==0&&(action!=0||i>0)&&(action!=1||i<view.projects().size()-1);
                button.setTooltip(Tooltip.create(Component.literal(action==0?"건설 우선순위 올리기":action==1?"건설 우선순위 내리기":"건설 취소")));addRenderableWidget(button);
            }
        }
    }
    private int bodyTop(){return top+5;}
    private int paletteSize(){return Math.min(29,Math.max(19,(height-top-137)/10-3));}
    private int paletteX(){return pane-2*(paletteSize()+2)-6;}
    private int queueY(){return top+120;}
    private int rowHeight(){return 55;}
    private String trim(String s,int width){return font.width(s)<=width?s:font.plainSubstrByWidth(s,Math.max(1,width-8))+"…";}
    private String buildingName(String id) {
        return view.buildings().stream().filter(b->b.id().equals(id)).map(Building::name).findFirst().orElse(id);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        HoiMenuStyle.panel(g,0,top,pane,height-top);
        g.text(font,"건설",9,top+8,HoiMenuStyle.TEXT);
        if(view!=null) {
            HoiMenuBar.draw(g,width,view.hud(),mx,my,hudScroll);
            var s=view.summary();int q=paletteX()-5;
            UiAssets.draw(g,"construction/summary/civilian_factory_repair",pane-123,top+5,18,16);
            HoiMenuStyle.recess(g,pane-78,top+3,29,19);
            String repairCount=Integer.toString(s.repairPriority());
            g.text(font,repairCount,pane-64-font.width(repairCount)/2,top+8,HoiMenuStyle.TEXT);
            HoiMenuStyle.recess(g,6,bodyTop()+24,pane-12,40);
            UiAssets.draw(g,"construction/summary/civilian_factory",10,bodyTop()+26,19,19);
            int assigned=s.used()+s.repair()+s.consumer()+s.reserved();
            g.text(font,assigned+" / "+s.total(),33,bodyTop()+29,assigned==s.total()?0xFF55FF55:0xFFFFAA00);
            UiAssets.draw(g,"construction/speed",10,top+53,12,12);
            long speedPercent=Math.round((s.speed()-1)*100);
            fitted(g,String.format(Locale.ROOT,"%+d%%",speedPercent),24,top+55,pane/2-28,speedPercent>=0?0xFF55FF55:0xFFAA0000);
            double energyBalance=s.energy()-s.demand();
            String energy=HoiMenuBar.rawNumber(energyBalance);
            UiAssets.draw(g,energyBalance<0?"construction/energy_deficit":"construction/energy",pane/2,top+53,12,12);
            fitted(g,energy,pane/2+14,top+55,pane/2-22,energyBalance>=0?0xFF55FF55:0xFFAA0000);
            HoiMenuStyle.metal(g,6,top+72,q-6,21);
            UiAssets.draw(g,"construction/consumer_goods",10,top+73,19,19);
            g.text(font,"소비재",33,top+78,HoiMenuStyle.TEXT);
            g.text(font,Integer.toString(s.consumer()),q-22,top+78,HoiMenuStyle.TEXT);
            HoiMenuStyle.metal(g,6,top+96,q-6,21);
            g.text(font,"무역 상품",10,top+102,HoiMenuStyle.TEXT);
            g.text(font,s.tradeGoods()==null?"—":s.tradeGoods().toString(),q-22,top+102,HoiMenuStyle.TEXT);
            for(int divider:paletteDividers) {
                int y=top+72+divider-paletteScroll;
                if(y>=top+72&&y+1<height-30) {
                    g.horizontalLine(paletteX(),pane-7,y,0xFF0B0D0F);
                    g.horizontalLine(paletteX(),pane-7,y+1,0xFF55595D);
                }
            }
            for(int i=0;i<view.projects().size();i++) {
                var p=view.projects().get(i);int y=queueY()+i*rowHeight()-scroll;
                if(y<queueY()||y+rowHeight()>height-30)continue;
                HoiMenuStyle.metal(g,6,y,q-6,rowHeight()-3);
                UiAssets.draw(g,"construction/"+p.building(),10,y+7,23,23);
                g.text(font,trim(p.name(),q-46),38,y+7,HoiMenuStyle.TEXT);
                g.text(font,trim(p.factories()+" / 15",q-44),38,y+20,HoiMenuStyle.MUTED);
                int bw=Math.max(15,q-79);g.fill(10,y+38,10+bw,y+43,0xFF101511);
                g.fill(10,y+38,10+(int)(bw*Math.clamp(p.cost()==0?0:p.progress()/p.cost(),0,1)),y+43,0xFF79945A);
                if(mx>=6&&mx<q&&my>=y&&my<y+31)HoiTooltips.draw(g,font,p.name()+" · "+buildingName(p.building())+"\n"+String.format(Locale.ROOT,"진행 %.0f / %.0f · 하루 %.1f\n완료 예상: %s",p.progress(),p.cost(),p.daily(),p.daily()>0?(long)Math.ceil(Math.max(0,p.cost()-p.progress())/p.daily())+"일":"공장 배정 대기"),mx,my);
            }
            if(mx>=pane-123&&mx<pane-27&&my>=top+3&&my<top+23)HoiTooltips.draw(g,font,"건설보다 건물 수리를 우선하는 공장 수\n우선 지정 "+s.repairPriority()+" · 실제 수리 배정 "+s.repair()+"\n수리할 건물이 없으면 건설에 배정됩니다.",mx,my);
            if(mx>=6&&mx<pane-6&&my>=bodyTop()+24&&my<top+52)HoiTooltips.draw(g,font,"민간공장 "+s.total()+"\n소비재 "+s.consumer()+" · 무역/기관 예약 "+s.reserved()+"\n건설 "+s.used()+" · 수리 "+s.repair()+" · 미사용 "+s.idle(),mx,my);
            if(my>=top+52&&my<top+68&&mx>=6&&mx<pane-6)HoiTooltips.draw(g,font,mx<pane/2?calculation(s.speedCalculation(),"건설 속도"):calculation(s.energyCalculation(),"사용 가능한 에너지"),mx,my);
            if(mx>=6&&mx<q&&my>=top+72&&my<top+93)HoiTooltips.draw(g,font,calculation(s.consumerCalculation(),"소비재"),mx,my);
            if(mx>=6&&mx<q&&my>=top+96&&my<top+117)HoiTooltips.draw(g,font,"무역 상품\n자원 수입 대가로 배정된 민간공장: "+(s.tradeGoods()==null?"—":s.tradeGoods())+"\n기관 개선·작전 예약 공장은 포함하지 않습니다.",mx,my);
        } else g.text(font,"건설 현황을 불러오는 중…",10,top+40,HoiMenuStyle.MUTED);
        super.extractRenderState(g,mx,my,delta);
    }
    private String calculation(String explanation,String label) {
        return explanation==null||explanation.isBlank()?label+"\n계산 내역이 제공되지 않았습니다.":explanation;
    }
    private void fitted(GuiGraphicsExtractor g,String value,int x,int y,int max,int color) {
        float scale=Math.min(1f,Math.max(1,max)/(float)Math.max(1,font.width(value)));
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale);g.text(font,value,0,0,color);g.pose().popMatrix();
    }
    private void send(ConstructionProtocol.Action action,String item,int amount,Vector3f ray) {
        if(pending>0&&action!=ConstructionProtocol.Action.CLOSE)return;
        pending=100;transport.accept(new ConstructionProtocol.Request(action,token,view==null?0:view.revision(),item,amount,ray==null?0:ray.x,ray==null?0:ray.y,ray==null?0:ray.z));
        rebuildWidgets();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if((event.button()==0||event.button()==1)&&event.x()>=pane&&event.y()>top+22&&event.x()<HoiMenuBar.statsRight(width)) {
            if(view!=null&&pending==0&&minecraft.player!=null) {
                if(!minecraft.options.getCameraType().isFirstPerson()) {
                    return true;
                }
                var matrix=minecraft.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new Matrix4f()).invert();
                var ray=matrix.transformProject(new Vector3f((float)(event.x()/width*2-1),(float)(1-event.y()/height*2),1)).normalize();
                send(event.button()==0?ConstructionProtocol.Action.PLACE:ConstructionProtocol.Action.CANCEL_AT,"",0,ray);
            }
            return true;
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(y<top&&view!=null){hudScroll=HoiMenuBar.scroll(width,view.hud(),hudScroll,horizontal,vertical);return true;}
        if(x<pane){if(x>=paletteX())paletteScroll-=(int)(vertical*(paletteSize()+3));else scroll-=(int)(vertical*rowHeight());rebuildWidgets();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void tick() {
        if(pending>0&&--pending==0){send(view==null?ConstructionProtocol.Action.OPEN:ConstructionProtocol.Action.REFRESH,"",0,null);}
        else if(++refresh%60==0&&pending==0)send(ConstructionProtocol.Action.REFRESH,"",0,null);
    }
    @Override public boolean keyPressed(KeyEvent e){return SidebarMovement.consumes(minecraft,e)||super.keyPressed(e);}
    @Override public boolean keyReleased(KeyEvent e){return SidebarMovement.consumes(minecraft,e)||super.keyReleased(e);}
    @Override public void removed(){SidebarMovement.release(minecraft);if(DialogClient.suspending())return;transport.accept(new ConstructionProtocol.Request(ConstructionProtocol.Action.CLOSE,token,view==null?0:view.revision(),"",0,0,0,0));}
    @Override public boolean allowsMovement(){return true;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}
