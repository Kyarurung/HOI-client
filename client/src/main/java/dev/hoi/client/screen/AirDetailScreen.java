package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.protocol.DialogView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

final class AirDetailScreen extends DialogScreen implements SidebarMovement.Screen {
    private int left,top,pane,tall,category,scroll,tooltipScroll;
    private double scale;
    private DialogView.Tile hovered;
    private record Hover(DialogView.Tile tile,int x,int y,int w,int h) {}
    private final List<Hover> hovers=new ArrayList<>();
    AirDetailScreen(DialogView view){super(view);}
    @Override int panelLeft(){return left;}
    @Override int panelTop(){return top;}
    @Override int panelWidth(){return pane;}
    @Override int panelHeight(){return tall;}
    @Override protected void init() {
        scale=Math.min(Math.max(.75,width*1.5/2560.0),(height-HoiMenuBar.height(width)-8)/436.0);
        scale=Math.min(scale,(width-8)/552.0);
        pane=n(552);tall=n(436);left=(width-pane)/2;top=Math.max(HoiMenuBar.height(width)+2,(height-tall)/2);
        button("×",516,3,28,26,this::onClose);
        for(int i=0;i<3;i++) {
            int index=i;
            var b=new HoiMenuButton(List.of("전투기와 근접항공지원기 임무","해상 임무","폭격 임무").get(i),x(207+i*45),y(236),n(43),n(34),()->{category=index;scroll=0;});
            b.background("none").caption("");addRenderableWidget(b);
        }
        if(dev.hoi.client.CampaignHud.visible())HoiMenuBar.buttons(width,"",null,dev.hoi.client.HoiClient::openMenu).forEach(this::addRenderableWidget);
    }
    private void button(String label,int px,int py,int w,int h,Runnable action){addRenderableWidget(new HoiMenuButton(label,x(px),y(py),n(w),n(h),action));}
    private int x(double v){return left+n(v);}
    private int y(double v){return top+n(v);}
    private int n(double v){return (int)Math.round(v*scale);}
    private List<DialogView.Tile> tiles(String section){return view().tiles().stream().filter(t->t.section().equals(section)).toList();}
    private DialogView.Tile tile(String section){return tiles(section).stream().findFirst().orElse(null);}
    private String title(String section,String fallback){var t=tile(section);return t==null?fallback:t.name();}
    private void art(GuiGraphicsExtractor g,String path,int px,int py,int w,int h){UiAssets.draw(g,path,x(px),y(py),n(w),n(h));}
    private void text(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,boolean center) {
        text(g,value,px,py,w,h,center,14);
    }
    private void text(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,boolean center,int size) {
        float factor=(float)Math.max(7,size*scale)/font.lineHeight;
        var line=font.split(HoiTooltips.sentences(Component.literal(value)),Math.max(1,(int)(n(w)/factor))).stream().findFirst().orElse(null);
        if(line==null)return;
        float offset=center?(n(w)-font.width(line)*factor)/2:0;
        g.pose().pushMatrix();g.pose().translate(x(px)+offset,y(py)+(n(h)-(font.lineHeight-1)*factor)/2);g.pose().scale(factor);
        g.text(font,line,0,0,HoiMenuStyle.TEXT);g.pose().popMatrix();
    }
    private void hover(DialogView.Tile t,int px,int py,int w,int h){if(t!=null)hovers.add(new Hover(t,x(px),y(py),n(w),n(h)));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        hovers.clear();
        if(!DialogClient.renderingContent())dev.hoi.client.CampaignHud.draw(g);
        art(g,"air_detail/background",0,0,552,436);
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/air_detail/background.png"),x(538),y(152),x(546),y(230),14f/552,6f/552,152f/436,230f/436);
        g.pose().pushMatrix();g.pose().translate(0,2);
        text(g,view().title(),12,2,528,26,true,21);
        g.pose().popMatrix();
        int centerX=(148+400)/2;
        var weather=tile("weather");
        if(weather!=null){art(g,weather.icon(),centerX-124,35,248,47);hover(weather,centerX-124,35,248,47);}
        art(g,"air_detail/air_weather_frame",centerX-126,33,252,52);
        var weatherIcon=tile("weather_icon");
        if(weatherIcon!=null){art(g,weatherIcon.icon(),centerX-123,54,24,24);hover(weatherIcon,centerX-123,54,24,24);}
        int gaugeX=centerX-128,gaugeY=83;
        var balance=tile("balance");
        if(balance!=null) {
            boolean disabled=balance.icon().endsWith("disabled");
            g.enableScissor(x(gaugeX+5),y(gaugeY+5),x(gaugeX+251),y(gaugeY+15));
            art(g,disabled?balance.icon():"air_detail/progress_bad",gaugeX+3,gaugeY+3,250,14);
            if(!disabled&&balance.gauge()!=null&&balance.gauge().fraction()>0) {
                g.enableScissor(x(gaugeX+5),y(gaugeY+5),x(gaugeX+5+246*balance.gauge().fraction()),y(gaugeY+15));
                art(g,"air_detail/progress_good",gaugeX+3,gaugeY+3,250,14);g.disableScissor();
            }
            g.disableScissor();
            hover(balance,gaugeX,gaugeY,256,20);
        }
        art(g,"air_detail/air_progress_frame",gaugeX,gaugeY,256,20);
        for(String side:List.of("own","enemy")) {
            var entries=tiles(side);
            for(int i=0;i<entries.size()&&i<4;i++) {
                int col=i/2,row=i%2,ix=side.equals("own")?12+70*col:504-70*col,tx=side.equals("own")?46+70*col:470-70*col,py=40+36*row;
                art(g,entries.get(i).icon(),ix,py,34,24);text(g,entries.get(i).value(),tx,py,30,28,true);
                hover(entries.get(i),Math.min(ix,tx),py,68,28);
            }
        }
        stat(g,tile("detection"),18,119,15);
        stat(g,tile("efficiency"),94,119,94);
        stat(g,tile("enemy_efficiency"),387,119,434);
        stat(g,tile("enemy_detection"),463,119,503);
        g.pose().pushMatrix();g.pose().translate(0,1);
        text(g,title("own_label","아군"),18,155,210,22,true,21);
        text(g,title("enemy_label","적군"),324,155,210,22,true,21);
        g.pose().popMatrix();
        combat(g,tile("air_losses"),false);
        combat(g,tile("air_kills"),true);
        for(int i=0;i<3;i++) {
            art(g,"air_detail/category_bg_"+(category==i?2:0),207+45*i,236,43,34);
            art(g,"air_detail/category_"+i,207+45*i,239,43,28);
        }
        var ownMissions=tiles("mission_"+category);
        var enemyMissions=tiles("enemy_mission_"+category);
        int maximum=Math.max(ownMissions.size(),enemyMissions.size());
        scroll=Math.clamp(scroll,0,Math.max(0,maximum-3));
        for(boolean enemy:List.of(false,true)) {
            var missions=enemy?enemyMissions:ownMissions;
            for(int i=scroll;i<Math.min(missions.size(),scroll+3);i++) {
                var t=missions.get(i);int px=enemy?412:15,py=275+(i-scroll)*50;
                g.fill(x(px),y(py),x(px+125),y(py+48),0x9B10161E);
                text(g,t.value(),px,py,125,14,true);art(g,t.icon(),px,py+14,125,32);hover(t,px,py,125,48);
            }
        }
        if(maximum>3)g.fill(x(282),y(280+scroll*112/Math.max(1,maximum-3)),x(286),y(306+scroll*112/Math.max(1,maximum-3)),0xFF8A8A8A);
        for(boolean enemy:List.of(false,true)) {
            int px=enemy?332:155;
            var results=tiles(enemy?"enemy_result":"own_result");
            for(int i=0;i<Math.min(4,results.size());i++) {
                var result=results.get(i);int py=280+i*27;
                art(g,result.icon(),px,py,28,22);
                text(g,(enemy?"§a":"§c")+result.value()+"§r",px+29,py,44,22,true);
                hover(result,px,py,73,22);
            }
        }
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable r)r.extractRenderState(g,mx,my,delta);
        var current=hovers.reversed().stream().filter(h->mx>=h.x()&&mx<h.x()+h.w()&&my>=h.y()&&my<h.y()+h.h()).map(Hover::tile).findFirst().orElse(null);
        if(!Objects.equals(current,hovered))tooltipScroll=0;hovered=current;
        if(current!=null) {
            String detail=current.detail().isBlank()?current.name():current.detail();
            float factor=(float)Math.max(8,16*Math.min(Math.max(.5,width/2560.0),scale))/font.lineHeight;
            g.pose().pushMatrix();g.pose().scale(factor);
            tooltipScroll=PoliticalTooltip.drawTextDetail(g,font,detail,(int)(mx/factor),(int)(my/factor),(int)(width/factor),(int)(height/factor),tooltipScroll);
            g.pose().popMatrix();
        }
    }
    private void stat(GuiGraphicsExtractor g,DialogView.Tile tile,int px,int py,int iconX) {
        if(tile==null)return;
        art(g,"air_detail/mini_bg",px,py,69,25);art(g,tile.icon(),iconX,py-2,24,26);
        text(g,tile.value(),px+(iconX<=px?24:0),py,43,25,true);hover(tile,Math.min(px,iconX),py-2,74,28);
    }
    private void combat(GuiGraphicsExtractor g,DialogView.Tile result,boolean killed) {
        if(result==null)return;
        if(result.value().equals("0")||result.value().equals("?"))return;
        int px=killed?416:18,iconX=killed?307:177;
        var aircraft=tiles(killed?"air_kill_art":"air_loss_art");
        for(int i=0;i<Math.min(1,aircraft.size());i++) {
            var t=aircraft.get(i);art(g,t.icon(),px+i*122,181,118,46);hover(t,px+i*122,181,118,46);
        }
        art(g,result.icon(),iconX,185,42,22);
        text(g,(killed?"§a":"§c")+result.value()+"§r",iconX+32,185,40,22,true);
        hover(result,iconX-3,182,75,23);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(event.x()<left||event.x()>=left+pane||event.y()<top||event.y()>=top+tall) {
            if(event.y()<HoiMenuBar.height(width))return super.mouseClicked(event,twice);
            if(event.button()==0)return dev.hoi.client.input.MapInfoInput.clickAt(minecraft,event.x(),event.y(),width,height);
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseScrolled(double px,double py,double h,double v) {
        if(px>=x(10)&&px<x(542)&&py>=y(278)&&py<y(428)){scroll-=(int)v;return true;}
        if(hovered!=null){tooltipScroll-=(int)v;return true;}return false;
    }
    @Override public boolean allowsMovement(){return true;}
    @Override public int unitHudLeft(){return 0;}
    @Override public boolean keyPressed(KeyEvent e){return SidebarMovement.consumes(minecraft,e)||super.keyPressed(e);}
    @Override public boolean keyReleased(KeyEvent e){return SidebarMovement.consumes(minecraft,e)||super.keyReleased(e);}
    @Override public void removed(){SidebarMovement.release(minecraft);}
}
