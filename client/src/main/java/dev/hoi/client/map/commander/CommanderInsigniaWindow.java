package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.*;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public final class CommanderInsigniaWindow {
    static final int WIDTH=317,HEIGHT=444,COLUMNS=6,SLOT_W=42,SLOT_H=44,GRID_X=27,GRID_Y=88,GRID_W=252,GRID_H=206,BAR_X=20,KNOB=16,TRAVEL=192;
    static final int[] BARS={316,344,373};
    private static final String ART="commander/commands/",SAT=ART+"sat_slider_background/frame_0";
    private static String target="";
    private static UiMenuStyle.Frame frame;
    private static float left=Float.NaN,top,hue,saturation,value;
    private static int dragging=-1,offset;
    private static boolean moving,listDrag,unsentColor,unsentFrame;
    private static double grabX,grabY;
    private static Integer pendingColor,pendingFrame;
    private static int[] saturationRamp;
    private CommanderInsigniaWindow() {}
    static void register() {
        HudElementRegistry.attachElementAfter(Identifier.parse("hoi:combat_log"),Identifier.parse("hoi:insignia_window"),(g,delta)->{if(Minecraft.getInstance().gui.screen()==null)draw(g,g.guiWidth()/2,g.guiHeight()/2);});
    }
    static String target(){return target;}
    static UiMenuStyle.Frame windowFrame(){return frame;}
    static int listOffset(){return offset;}
    static void open(Card card) {
        if(card.panel()==null)return;
        if(target.equals(card.id())){close();return;}
        target=card.id();left=Float.NaN;offset=0;dragging=-1;moving=listDrag=unsentColor=unsentFrame=false;pendingColor=pendingFrame=null;sync(authoritative(card));
    }
    static Integer color(Card card) {
        if(card.panel()==null)return null;
        if(card.id().equals(target))return rgb();
        return authoritative(card);
    }
    static int frame(Card card) {
        if(card.id().equals(target)&&pendingFrame!=null)return pendingFrame;
        return card.panel()==null||card.panel().insignia()==null?kind(card).equals("fleet")?3:0:card.panel().insignia();
    }
    static String kind(Card card){return switch(card.role()){case ARMY_GROUP->"army_group";case NAVY_FLEET,NAVY_HQ->"fleet";default->"army";};}
    static Insignia set(String kind) {
        var view=CommanderHud.view();var set=view==null?null:view.insignia().get(kind);
        return set!=null?set:new Insignia(ART+(kind.equals("fleet")?"theatre_task_force_shield":"theatre_"+kind+"_shield"),1,List.of(),List.of());
    }
    static String shield(Card card){return set(kind(card)).sprite()+"/frame_"+frame(card);}
    static String selected(Card card){return set(kind(card)).sprite()+"_selected/frame_"+frame(card);}
    static int tint(Card card,int color){return tint(set(kind(card)),frame(card),color);}
    public static String armyShield(int icon){return set("army").sprite()+"/frame_"+icon;}
    public static int armyTint(int icon,int color){return tint(set("army"),icon,color);}
    private static int tint(Insignia set,int icon,int color){return set.plain().contains(icon)?0xffffffff:0xff000000|color;}
    private static Integer authoritative(Card card) {
        var panel=card.panel();if(panel==null)return null;String kind=kind(card);
        if(kind.equals("army"))return panel.armyColor();
        return panel.color()!=null?panel.color():kind.equals("army_group")?Integer.valueOf(0x806000):null;
    }
    private static String command(Card card,String field){return switch(kind(card)){case "army_group"->"GROUP_";case "fleet"->"FLEET_";default->"ARMY_";}+field;}
    private static int rgb(){return java.awt.Color.HSBtoRGB(hue,saturation,value)&0xffffff;}
    private static void sync(Integer color){if(color==null)return;float[] hsb=java.awt.Color.RGBtoHSB(color>>16&255,color>>8&255,color&255,null);hue=hsb[0];saturation=hsb[1];value=hsb[2];}
    private static Card card(){var view=CommanderHud.view();return view==null||target.isEmpty()?null:view.cards().stream().filter(c->c.id().equals(target)&&c.panel()!=null&&Set.of(Mode.ASSIGN_ARMY,Mode.ARMY_GROUP,Mode.NAVY_FLEET,Mode.NAVY_HQ).contains(c.role())).findFirst().orElse(null);}
    private static boolean visible() {
        var client=Minecraft.getInstance();
        return !target.isEmpty()&&client.level!=null&&!client.gui.hud.isHidden()&&!AgencyHud.active()&&(client.gui.screen()==null||client.gui.screen() instanceof MapCursor);
    }
    private static void layout() {
        var client=Minecraft.getInstance();int width=client.getWindow().getGuiScaledWidth(),height=client.getWindow().getGuiScaledHeight(),menu=HudMenuBar.height(width);
        float scale=Math.min(CommanderLayout.units(width,height).scale()*.5f,Math.min((width-8f)/WIDTH,(height-menu-8f)/HEIGHT));
        if(Float.isNaN(left)){left=width/2f-175*scale;top=height/2f-300*scale;}
        left=Math.clamp(left,0,Math.max(0,width-WIDTH*scale));top=Math.clamp(top,menu,Math.max(menu,height-HEIGHT*scale));
        frame=new UiMenuStyle.Frame(left,top,scale);
    }
    public static boolean covers(double x,double y){return visible()&&frame!=null&&inside(x,y,0,0,WIDTH,HEIGHT);}
    private static boolean inside(double x,double y,double px,double py,double pw,double ph){return x>=frame.x(px)&&x<frame.x(px+pw)&&y>=frame.y(py)&&y<frame.y(py+ph);}
    private static int count(Card card){return set(kind(card)).choices().size();}
    private static int maximum(Card card){return Math.max(0,(count(card)+COLUMNS-1)/COLUMNS*SLOT_H-GRID_H);}
    private static UiScrollBar bar(Card card){return new UiScrollBar(frame.ix(279),frame.iy(83),frame.iy(306),GRID_H,maximum(card)+GRID_H);}
    private static void settle(Card card) {
        if(unsentColor&&CommanderOrders.send(command(card,"COLOR"),List.of(target,"rgb/"+pendingColor),List.of()))unsentColor=false;
        if(unsentFrame&&CommanderOrders.send(command(card,"INSIGNIA"),List.of(target,"frame/"+pendingFrame),List.of()))unsentFrame=false;
        var view=CommanderHud.view();Integer current=authoritative(card);boolean answered=CommanderOrders.ready()&&view!=null&&CommanderOrders.currentHud(view);
        if(pendingColor!=null&&!unsentColor&&(pendingColor.equals(current)||answered))pendingColor=null;
        if(pendingFrame!=null&&!unsentFrame&&(pendingFrame.equals(card.panel().insignia())||answered))pendingFrame=null;
        if(dragging<0&&pendingColor==null&&current!=null&&current!=rgb())sync(current);
    }
    public static void draw(GuiGraphicsExtractor g,int mx,int my) {
        if(!visible())return;var card=card();if(card==null){close();return;}
        settle(card);layout();var font=Minecraft.getInstance().font;
        frame.art(g,ART+"army_insignia_selection_bg/frame_0",0,0);
        UiMenuStyle.title(g,font,frame,UiCommandLabels.text("CHOOSE_INSIGNIA",""),18,20,256,-1,UiMenuStyle.TEXT);
        frame.art(g,ART+"nv_btn_close/frame_0",282,4);
        frame.art(g,ART+"group_name_bg/frame_0",25,44);
        int color=rgb();var set=set(kind(card));
        UiAssets.drawExact(g,shield(card),frame.x(18),frame.y(41),frame.size(33),frame.size(32),tint(card,color));
        float scale=UiText.scale(font);UiText.marquee(g,font,card.name(),frame.x(57),frame.y(56.5)-font.lineHeight*scale/2,frame.size(231),scale,UiMenuStyle.TEXT,true,-1);
        frame.art(g,ART+"parch_bg/frame_0",14,73);
        offset=Math.clamp(offset,0,maximum(card));
        g.enableScissor(frame.ix(GRID_X),frame.iy(GRID_Y),frame.ix(GRID_X+GRID_W),frame.iy(GRID_Y+GRID_H));
        for(int i=0;i<set.choices().size();i++){double x=GRID_X+i%COLUMNS*SLOT_W,y=GRID_Y+i/COLUMNS*SLOT_H-offset;if(y+SLOT_H<GRID_Y||y>GRID_Y+GRID_H)continue;int icon=set.choices().get(i);UiAssets.drawExact(g,set.sprite()+"/frame_"+icon,frame.x(x),frame.y(y),frame.size(33),frame.size(32),tint(set,icon,color));}
        g.disableScissor();
        bar(card).draw(g,offset);
        var presets=card.panel().palette();
        for(int i=0;i<Math.min(9,presets.size());i++)UiAssets.drawExact(g,ART+"tiled_window_color_picker/frame_0",frame.x(223+25*(i%3)),frame.y(315+25*(i/3)),frame.size(24),frame.size(24),0xff000000|presets.get(i));
        bars(g);
        if(inside(mx,my,282,4,31,31))CommanderHud.drawTooltip(g,mx,my,TooltipDelay.visible(UiCommandLabels.text("CLOSE","")));
    }
    private static void bars(GuiGraphicsExtractor g) {
        frame.art(g,ART+"hue_slider_background/frame_0",BAR_X,BARS[0]);
        int current=rgb(),ramp[]=ramp();
        g.pose().pushMatrix();g.pose().translate(frame.x(BAR_X),frame.y(BARS[1]));g.pose().scale(frame.scale());
        for(int i=0;i<ramp.length;i++){int t=ramp[i],r=Math.min(255,t+(current>>16&255)),gr=Math.min(255,t+(current>>8&255)),b=Math.min(255,t+(current&255));g.fill(i,0,i+1,15,0xff000000|r<<16|gr<<8|b);}
        g.pose().popMatrix();
        UiAssets.drawExact(g,ART+"val_slider_background/frame_0",frame.x(BAR_X),frame.y(BARS[2]),frame.size(200),frame.size(15),0xff000000|java.awt.Color.HSBtoRGB(hue,saturation,1)&0xffffff);
        float[] values={hue,saturation,value};
        for(int i=0;i<3;i++){
            g.pose().pushMatrix();g.pose().translate(frame.x(22),frame.y(BARS[i]+2));g.pose().scale(frame.scale());UiAssets.nineSlice(g,ART+"color_picker_scroll_track/frame_0",0,0,196,12,4,1);g.pose().popMatrix();
            frame.art(g,ART+"color_picker_scroll_drager/frame_0",16+TRAVEL*values[i],BARS[i],KNOB,KNOB);
        }
    }
    private static int[] ramp() {
        if(saturationRamp!=null)return saturationRamp;
        var resource=Minecraft.getInstance().getResourceManager().getResource(Identifier.parse("hoi:textures/gui/"+SAT+".png"));
        if(resource.isEmpty())return new int[0];
        try(var input=resource.get().open();var image=com.mojang.blaze3d.platform.NativeImage.read(input)){int[] values=new int[image.getWidth()];for(int x=0;x<values.length;x++)values[x]=image.getPixel(x,image.getHeight()/2)>>16&255;return saturationRamp=values;}
        catch(java.io.IOException failure){return new int[0];}
    }
    private static float slide(double x){return Math.round(Math.clamp((x-frame.x(24))/frame.size(TRAVEL),0,1)*100)/100f;}
    private static void set(int bar,double x){float v=slide(x);if(bar==0)hue=v;else if(bar==1)saturation=v;else value=v;}
    private static void send(Card card){pendingColor=rgb();unsentColor=true;settle(card);}
    public static boolean click(MouseButtonEvent event) {
        if(!visible()||frame==null)return false;double mx=event.x(),my=event.y();if(!inside(mx,my,0,0,WIDTH,HEIGHT))return false;
        var card=card();if(card==null){close();return true;}
        if(event.button()!=InputConstants.MOUSE_BUTTON_LEFT)return true;
        if(inside(mx,my,282,4,31,31)){AudioUiSounds.play("ui.close");close();return true;}
        var scroll=bar(card);if(scroll.contains(mx,my)){if(scroll.arrow(my))offset=Math.clamp(offset+(my<frame.y(83)+8?-SLOT_H:SLOT_H),0,maximum(card));else{listDrag=true;drag(card,my);}return true;}
        var choices=set(kind(card)).choices();
        if(inside(mx,my,GRID_X,GRID_Y,GRID_W,GRID_H))for(int i=0;i<choices.size();i++){double x=GRID_X+i%COLUMNS*SLOT_W,y=GRID_Y+i/COLUMNS*SLOT_H-offset;if(!inside(mx,my,x,y,33,32))continue;AudioUiSounds.play("ui.click");int icon=choices.get(i);if(frame(card)!=icon){pendingFrame=icon;unsentFrame=true;settle(card);}return true;}
        for(int i=0;i<3;i++)if(inside(mx,my,4,BARS[i],232,KNOB)){dragging=i;set(i,mx);return true;}
        var presets=card.panel().palette();
        for(int i=0;i<Math.min(9,presets.size());i++)if(inside(mx,my,223+25*(i%3),315+25*(i/3),24,24)){AudioUiSounds.play("ui.click");sync(presets.get(i));send(card);return true;}
        moving=true;grabX=mx-left;grabY=my-top;return true;
    }
    private static void drag(Card card,double my){offset=bar(card).offset(my);}
    public static boolean drag(MouseButtonEvent event) {
        if(!visible())return false;
        if(moving){left=(float)(event.x()-grabX);top=(float)(event.y()-grabY);return true;}
        if(dragging>=0){set(dragging,event.x());return true;}
        var card=card();if(listDrag&&card!=null){drag(card,event.y());return true;}
        return false;
    }
    public static boolean release() {
        boolean held=moving||listDrag||dragging>=0;var card=card();
        if(dragging>=0&&card!=null){dragging=-1;send(card);}
        dragging=-1;moving=listDrag=false;return held&&visible();
    }
    public static boolean scroll(double x,double y,double vertical) {
        if(!covers(x,y))return false;var card=card();
        if(card!=null&&inside(x,y,22,83,280,223))offset=Math.clamp(offset-(int)Math.round(Math.signum(vertical))*SLOT_H,0,maximum(card));
        return true;
    }
    static boolean closeWindow(){if(!visible())return false;close();return true;}
    private static void close(){target="";dragging=-1;moving=listDrag=false;}
    static void reset(){close();pendingColor=pendingFrame=null;unsentColor=unsentFrame=false;}
}
