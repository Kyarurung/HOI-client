package dev.hoi.client;

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

/** A narrow construction queue beside the live atlas. Mouse rays never contain a trusted state or country. */
public final class ConstructionScreen extends Screen implements SidebarMovement.Screen {
    private final String token;
    private final Consumer<ConstructionProtocol.Request> transport;
    private ConstructionView view;
    private int pane,top,scroll,paletteScroll,pending,refresh,hudScroll;
    private String localMessage="";
    public ConstructionScreen() {
        this(UUID.randomUUID().toString(),null,r->{if(ClientPlayNetworking.canSend(ConstructionProtocol.Request.TYPE))ClientPlayNetworking.send(r);});
    }
    ConstructionScreen(String token,ConstructionView fixture,Consumer<ConstructionProtocol.Request> transport) {
        super(Component.literal("건설"));this.token=token;this.view=fixture;this.transport=transport;
    }
    void open(){send(ConstructionProtocol.Action.OPEN,"",0,null);}
    void update(ConstructionView next) {
        if(!next.session().equals(token))return;
        if(next.country().isEmpty()){minecraft.gui.setScreen(null);return;}
        if(view!=null&&next.revision()<view.revision())return;
        view=next;pending=0;localMessage="";rebuildWidgets();
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
        int icon=paletteSize(),x=paletteX(),y=top+96;
        int paletteRows=Math.max(1,(height-y-30)/(icon+3));
        paletteScroll=Math.clamp(paletteScroll/(icon+3),0,Math.max(0,(view.buildings().size()+1)/2-paletteRows))*(icon+3);
        for(int i=0;i<view.buildings().size();i++) {
            var b=view.buildings().get(i);int bx=x+(i%2)*(icon+2),by=y+(i/2)*(icon+3)-paletteScroll;
            if(by<y||by+icon>height-30)continue;
            var button=new ResearchButton(b.name(),bx,by,icon,icon,()->send(ConstructionProtocol.Action.SELECT,b.id(),0,null)) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    HoiMenuStyle.control(g,bx,by,icon,icon,view.selected().equals(b.id()),isHoveredOrFocused());
                    UiAssets.draw(g,b.texture(),bx+2,by+2,icon-4,icon-4);
                    if(!b.enabled())g.fill(bx+1,by+1,bx+icon-1,by+icon-1,0x88000000);
                }
            };
            button.sound(UiSounds.construction(b.id()));
            button.active=b.enabled()&&pending==0;
            button.setTooltip(Tooltip.create(Component.literal(b.name()+"\n"+b.reason())));addRenderableWidget(button);
        }
        int repair=view.summary().repairPriority();
        for(int direction:new int[]{-1,1}) {
            var button=new HoiMenuButton(direction<0?"−":"+",direction<0?pane-89:pane-29,bodyTop()+27,21,19,
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
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        HoiMenuStyle.panel(g,0,top,pane,height-top);
        g.text(font,"건설",9,top+8,HoiMenuStyle.TEXT);
        if(view!=null) {
            HoiMenuBar.draw(g,width,view.hud(),mx,my,hudScroll);
            var s=view.summary();int q=paletteX()-5;
            HoiMenuStyle.metal(g,6,bodyTop()+24,pane-12,26);
            g.text(font,"수리 우선",10,bodyTop()+32,HoiMenuStyle.TEXT);
            HoiMenuStyle.recess(g,pane-66,bodyTop()+27,35,19);
            String repairCount=Integer.toString(s.repairPriority());
            g.text(font,repairCount,pane-48-font.width(repairCount)/2,bodyTop()+32,HoiMenuStyle.TEXT);
            HoiMenuStyle.recess(g,6,bodyTop()+52,pane-12,35);
            UiAssets.draw(g,"construction/civilian_factory",10,bodyTop()+54,19,19);
            g.text(font,(s.used()+s.repair()+s.consumer()+s.reserved())+" / "+s.total(),33,bodyTop()+57,HoiMenuStyle.TEXT);
            g.text(font,"미사용 "+s.idle(),pane-65,bodyTop()+57,HoiMenuStyle.MUTED);
            g.text(font,String.format(Locale.ROOT,"속도 %+.0f%%",(s.speed()-1)*100),10,top+78,HoiMenuStyle.TEXT);
            String energy=String.format(Locale.ROOT,"%.0f/%.0f",s.energy(),s.demand());
            int ex=pane-10-font.width(energy);UiAssets.draw(g,"construction/energy",ex-14,top+76,12,12);
            g.text(font,energy,ex,top+78,s.energy()<s.demand()?0xFFD59183:0xFF91B48A);
            HoiMenuStyle.metal(g,6,top+96,q-6,21);
            UiAssets.draw(g,"construction/consumer_goods",10,top+97,19,19);
            g.text(font,"소비재",33,top+102,HoiMenuStyle.TEXT);
            g.text(font,Integer.toString(s.consumer()),q-22,top+102,HoiMenuStyle.TEXT);
            for(int i=0;i<view.projects().size();i++) {
                var p=view.projects().get(i);int y=queueY()+i*rowHeight()-scroll;
                if(y<queueY()||y+rowHeight()>height-30)continue;
                HoiMenuStyle.metal(g,6,y,q-6,rowHeight()-3);
                UiAssets.draw(g,"construction/"+p.building(),10,y+7,23,23);
                g.text(font,trim(p.name(),q-46),38,y+7,HoiMenuStyle.TEXT);
                g.text(font,trim(p.factories()+" / 15",q-44),38,y+20,HoiMenuStyle.MUTED);
                int bw=Math.max(15,q-79);g.fill(10,y+38,10+bw,y+43,0xFF101511);
                g.fill(10,y+38,10+(int)(bw*Math.clamp(p.cost()==0?0:p.progress()/p.cost(),0,1)),y+43,0xFF79945A);
                if(mx>=6&&mx<q&&my>=y&&my<y+31)HoiTooltips.draw(g,font,p.name()+" · "+p.building()+"\n"+String.format(Locale.ROOT,"진행 %.0f / %.0f · 하루 %.1f\n완료 예상: %s",p.progress(),p.cost(),p.daily(),p.daily()>0?(long)Math.ceil(Math.max(0,p.cost()-p.progress())/p.daily())+"일":"공장 배정 대기"),mx,my);
            }
            if(mx>=6&&mx<pane-6&&my>=bodyTop()+24&&my<bodyTop()+50)HoiTooltips.draw(g,font,"건설보다 건물 수리를 우선하는 공장 수\n우선 지정 "+s.repairPriority()+" · 실제 수리 배정 "+s.repair()+"\n수리할 건물이 없으면 건설에 배정됩니다.",mx,my);
            if(mx>=6&&mx<pane-6&&my>=bodyTop()+52&&my<top+92)HoiTooltips.draw(g,font,"민간공장 "+s.total()+"\n소비재 "+s.consumer()+" · 무역/기관 예약 "+s.reserved()+"\n건설 "+s.used()+" · 수리 "+s.repair()+" · 미사용 "+s.idle()+"\n사용 가능한 에너지 "+HoiMenuBar.rawNumber(s.energy())+" / 필요량 "+HoiMenuBar.rawNumber(s.demand())+"\n공장별 배정은 서버가 계산합니다.",mx,my);
            if(view.projects().isEmpty())g.text(font,trim("우클릭 건설 · 좌클릭 취소",q-16),10,queueY()+12,HoiMenuStyle.MUTED);
        } else g.text(font,"건설 현황을 불러오는 중…",10,top+40,HoiMenuStyle.MUTED);
        String msg=!localMessage.isEmpty()?localMessage:view==null?"":view.message();
        if(!msg.isEmpty())g.text(font,trim(msg,pane-16),8,height-20,HoiMenuStyle.TEXT);
        else {
            g.text(font,trim("우클릭 건설 · 좌클릭 취소",pane-16),8,height-20,HoiMenuStyle.MUTED);
            if(mx<pane&&my>=height-24)HoiTooltips.draw(g,font,"건물 선택 후 자국 주 우클릭: 건설\n좌클릭: 해당 주에서 선택한 건물의 마지막 건설 예약 하나 취소",mx,my);
        }
        super.extractRenderState(g,mx,my,delta);
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
                    localMessage="1인칭 시점에서 지도 위의 주를 선택하세요.";return true;
                }
                var matrix=minecraft.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new Matrix4f()).invert();
                var ray=matrix.transformProject(new Vector3f((float)(event.x()/width*2-1),(float)(1-event.y()/height*2),1)).normalize();
                send(event.button()==1?ConstructionProtocol.Action.PLACE:ConstructionProtocol.Action.CANCEL_AT,"",0,ray);
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
        if(pending>0&&--pending==0){localMessage="응답을 기다리는 중입니다. 현황을 다시 확인합니다.";send(view==null?ConstructionProtocol.Action.OPEN:ConstructionProtocol.Action.REFRESH,"",0,null);}
        else if(++refresh%60==0&&pending==0)send(ConstructionProtocol.Action.REFRESH,"",0,null);
    }
    @Override public boolean keyPressed(KeyEvent e){return SidebarMovement.consumes(minecraft,e)||super.keyPressed(e);}
    @Override public boolean keyReleased(KeyEvent e){return SidebarMovement.consumes(minecraft,e)||super.keyReleased(e);}
    @Override public void removed(){SidebarMovement.release(minecraft);transport.accept(new ConstructionProtocol.Request(ConstructionProtocol.Action.CLOSE,token,view==null?0:view.revision(),"",0,0,0,0));}
    @Override public boolean allowsMovement(){return true;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}
