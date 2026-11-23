package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.ui.*;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderAirWingPayloads.*;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.industry.IndustryTankDesigner;
import java.util.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public final class CommanderAirWingWindow {
    static final int WIDTH=856,HEIGHT=409,VALUE_RIGHT=162,LIST_X=561,LIST_Y=30,LIST_W=268,LIST_H=360,ENTRY=45;
    static final String[] TITLES={"DESIGNER_BASE_STATS_LABEL","DESIGNER_COMBAT_STATS_LABEL","FE_MISC_STATS"};
    static final int[] TITLE_X={18,192,367},COLUMN_X={18,190,364};
    static final double TITLE_Y=88;
    static final double[] ROWS={110.5,130.5,150.5,170.5,190.5,210,229.5,248.5};
    record Box(double x,double y,double w,double h){boolean contains(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}}
    private final String wing,token=UUID.randomUUID().toString();
    private View view;
    private UiMenuStyle.Frame frame;
    private float left=Float.NaN,top;
    private double grabX,grabY;
    private boolean moving;
    CommanderAirWingWindow(String wing){this.wing=wing;}
    String wing(){return wing;}
    View view(){return view;}
    UiMenuStyle.Frame frame(){return frame;}
    void open(){AudioUiSounds.play("ui.pop_up");request();}
    void request(){CommanderAirWingHud.send(new Request(token,Action.OPEN,wing));}
    void close(){CommanderAirWingHud.send(new Request(token,Action.CLOSE,wing));}
    boolean accept(View next){if(!next.token().equals(token))return true;view=next;return next.error().isEmpty();}
    void place(CommanderAirWingWindow previous){left=previous.left;top=previous.top;}
    void layout(int width,int height,float base) {
        float scale=Math.min(base,Math.min((width-8f)/WIDTH,(height-8f)/HEIGHT));
        if(Float.isNaN(left)){left=width/2f-275*scale;top=height/2f-235*scale;}
        left=Math.clamp(left,0,Math.max(0,width-WIDTH*scale));top=Math.clamp(top,0,Math.max(0,height-HEIGHT*scale));
        frame=new UiMenuStyle.Frame(left,top,scale);
    }
    boolean contains(double x,double y){return frame!=null&&gui(new Box(0,0,WIDTH,HEIGHT)).contains(x,y);}
    private Box gui(Box box){return new Box(frame.x(box.x()),frame.y(box.y()),frame.size(box.w()),frame.size(box.h()));}
    private String label(String key){return view==null?"":view.label(key);}
    private List<IndustryTankDesigner.Stat> stats(int column){return view==null||column>=view.columns().size()?List.of():view.columns().get(column).stats();}
    private static Box closeBox(){return new Box(821,3,31,31);}
    private static Box deleteBox(){return new Box(561,3,31,31);}
    private static Box typeBox(){return new Box(12,38,96,27);}
    private static Box carrierBox(){return new Box(14.5,42,25,22);}
    static Box row(int column,int row){return new Box(COLUMN_X[column],ROWS[row]-9.5,VALUE_RIGHT+2,19);}
    void draw(GuiGraphicsExtractor g,Font font) {
        if(frame==null)return;
        g.nextStratum();
        frame.art(g,"commander/commands/air_unit_stats_bg/frame_0",0,0,WIDTH,HEIGHT);
        UiMenuStyle.title(g,font,frame,label("AIR_WING_STATS"),35,17.5,300,-1,UiMenuStyle.TEXT,true);
        frame.art(g,"commander/commands/nv_btn_delete/frame_0",561,3,31,31);
        frame.art(g,"commander/commands/nv_btn_close/frame_0",821,3,31,31);
        if(view==null||!view.error().isEmpty())return;
        text(g,font,view.name(),113,41.5,267,20,-1,UiMenuStyle.TEXT);
        picture(g,view.icon(),typeBox());
        if(view.carrier())frame.art(g,"commander/commands/airwing_carrier_icon/frame_0",carrierBox().x(),carrierBox().y());
        frame.art(g,"commander/commands/equipment_icon/frame_0",638,5.5);
        text(g,font,view.ratio(),690,7,60,20,-1,UiMenuStyle.TEXT);
        for(int c=0;c<COLUMN_X.length;c++) {
            UiMenuStyle.title(g,font,frame,label(TITLES[c]),TITLE_X[c],TITLE_Y,160,-1,UiMenuStyle.TEXT,true);
            var stats=stats(c);
            for(int r=0;r<Math.min(ROWS.length,stats.size());r++) {
                var stat=stats.get(r);double x=COLUMN_X[c],y=ROWS[r]-10;
                String icon="production/design/stats/"+stat.id(),value=IndustryEquipmentDesigns.statValue(stat,stat.value());
                int indent=UiAssets.imageSize(icon)[0]>0?20:0;
                if(indent>0)UiAssets.drawAlignedContent(g,icon,icon,frame.x(x),frame.y(y+1),frame.size(18),frame.size(18));
                double width=Math.min(90,font.width(value)*UiText.scale(font)/frame.scale()+4);
                text(g,font,IndustryEquipmentDesigns.statName(stat.id()).strip(),x+indent,y,VALUE_RIGHT-indent-width,20,-1,UiMenuStyle.TEXT);
                text(g,font,value,x+VALUE_RIGHT-width,y,width,20,1,Tooltips.GOLD|0xFF000000);
            }
        }
        text(g,font,view.equipmentName(),85,285,350,24,0,UiMenuStyle.TEXT);
        picture(g,view.icon(),new Box(185,318,140,50));
        for(int i=0;i<Math.min(12,view.missions().size());i++){String icon=CommanderPanels.airMissionIcon(view.missions().get(i));if(!icon.isEmpty())UiAssets.drawContent(g,icon,frame.ix(20+i%6*25),frame.iy(316.5+i/6*20),frame.len(25),frame.len(17));}
        frame.art(g,"commander/commands/manpower_icon/frame_0",20,364,27,27);
        text(g,font,view.manpower(),50,367.5,60,20,0,UiMenuStyle.TEXT);
        if(!view.aceIcon().isEmpty())frame.art(g,view.aceIcon(),480,350);
        text(g,font,view.ace(),225,370,250,20,1,UiMenuStyle.TEXT);
        for(int i=0;i<Math.min(view.equipment().size(),LIST_H/ENTRY);i++) {
            var item=view.equipment().get(i);double y=LIST_Y+i*ENTRY;
            frame.art(g,"commander/commands/air_unit_stats_equip_entry/frame_0",LIST_X,y,LIST_W,ENTRY);
            picture(g,item.icon(),new Box(LIST_X+10,y+8,70,32));
            if(item.carrier())frame.art(g,"commander/commands/airwing_carrier_icon/frame_0",LIST_X+12,y+24);
            text(g,font,Integer.toString(item.count()),LIST_X+7,y+4,60,16,-1,UiMenuStyle.TEXT);
            text(g,font,item.name(),LIST_X+90,y+5,175,16,-1,UiMenuStyle.TEXT);
            text(g,font,item.type(),LIST_X+90,y+23,150,16,-1,UiMenuStyle.TEXT);
        }
    }
    private void picture(GuiGraphicsExtractor g,String asset,Box box){UiAssets.drawAlignedContent(g,asset,asset,frame.x(box.x()),frame.y(box.y()),frame.size(box.w()),frame.size(box.h()));}
    private void text(GuiGraphicsExtractor g,Font font,String value,double x,double y,double w,double h,int align,int color) {
        if(value==null||value.isEmpty())return;
        var line=net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.officerText(value));float scale=UiText.scale(font);
        UiText.outlined(g,font,line,frame.x(x),UiText.centeredY(font,line,frame.y(y),frame.size(h),scale),frame.size(w),scale,color,false,align);
    }
    String tooltip(double mx,double my) {
        if(frame==null||view==null||!view.error().isEmpty())return "";
        if(gui(closeBox()).contains(mx,my))return label("CLOSE");
        if(gui(deleteBox()).contains(mx,my))return view.tooltip("delete");
        if(gui(new Box(20,364,91,27)).contains(mx,my))return view.tooltip("manpower");
        if(view.carrier()&&gui(carrierBox()).contains(mx,my))return view.tooltip("carrier");
        for(int c=0;c<COLUMN_X.length;c++)for(int r=0;r<Math.min(ROWS.length,stats(c).size());r++)if(gui(row(c,r)).contains(mx,my))return IndustryEquipmentDesigns.statTooltip(stats(c).get(r));
        return "";
    }
    boolean mouseClicked(MouseButtonEvent event,Runnable closer) {
        if(!contains(event.x(),event.y()))return false;
        if(event.button()!=InputConstants.MOUSE_BUTTON_LEFT)return true;
        if(gui(closeBox()).contains(event.x(),event.y())){AudioUiSounds.play("ui.close");closer.run();return true;}
        if(view!=null&&view.error().isEmpty()&&gui(deleteBox()).contains(event.x(),event.y())){AudioUiSounds.play("ui.disband");CommanderHud.open(new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.AIR_ORGANIZE,"",List.of("action/DISBAND",wing)));return true;}
        moving=true;grabX=event.x()-left;grabY=event.y()-top;return true;
    }
    boolean mouseDragged(MouseButtonEvent event){if(!moving)return false;left=(float)(event.x()-grabX);top=(float)(event.y()-grabY);return true;}
    boolean mouseReleased(){boolean held=moving;moving=false;return held;}
}
