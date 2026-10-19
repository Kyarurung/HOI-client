package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class MapCommanderTheaters {
    private static int scroll,maximum,left,top,right,bottom;
    private static String settings="";
    private MapCommanderTheaters() {}
    static void reset(){settings="";scroll=maximum=left=top=right=bottom=0;}
    static boolean closeSettings(){if(settings.isEmpty())return false;settings="";return true;}
    static boolean scrollAt(double x,double y,double amount){if(amount==0||x<left||x>=right||y<top||y>=bottom)return false;scroll=Math.clamp(scroll-(int)Math.signum(amount)*20,0,maximum);return true;}
    private static String art(String name){return "commander/commands/"+name+"/frame_0";}
    private static Control control(Theater theater,String id){return theater.controls().stream().filter(c->c.id().equals(id)||c.id().startsWith(id+"/")).findFirst().orElse(null);}
    private static List<String> selection(View view){return view.cards().stream().filter(c->MapCommanderPanels.selected(c.id())&&(c.role()==Mode.ASSIGN_ARMY||c.role()==Mode.ARMY_GROUP)).map(Card::id).toList();}
    static void draw(GuiGraphicsExtractor g,UiUnits u,View view,List<MapCommanderPanels.Hit> hits,int limit){
        if(view.branch()!=Mode.ARMY||view.theaters().isEmpty()){reset();return;}
        left=g.guiWidth()-u.size(164);right=left+u.size(152.5);top=u.size(55.5);bottom=Math.max(top,limit-u.size(4));int y=top-scroll;var out=new ArrayList<MapCommanderPanels.Hit>();
        if(view.theaters().stream().noneMatch(t->t.id().equals(settings)))settings="";
        int settingsY=top;Theater settingsTheater=null;
        var cards=new HashMap<String,Card>();view.cards().forEach(c->cards.put(c.id(),c));g.enableScissor(left,top,right,bottom);
        for(var theater:view.theaters()){
            int header=y;UiAssets.draw(g,art("theater_group_bg").replace("frame_0","frame_1"),left,y,u.size(152.5),u.size(38.5));
            if(theater.id().equals(settings)){settingsY=y;settingsTheater=theater;}
            label(g,theater.name(),left+u.size(19),y+u.size(3.5),u.size(84),u.size(16),false);
            icon(g,u,out,theater,"THEATER_SETTINGS","theater_group_settings_btn",0,left+u.size(4.5),y+u.size(5.5),11.5,11.5);
            icon(g,u,out,theater,"THEATER_LOG","theatre_combat_log_icon",0,left+u.size(104.5),y+u.size(2.5),13.05,17.1);
            battle(g,u,out,theater,"THEATER_OFFENSIVE",0,left+u.size(116),y+u.size(6.5));
            battle(g,u,out,theater,"THEATER_DEFENSIVE",1,left+u.size(134.5),y+u.size(6.5));
            var assign=control(theater,"THEATER_ASSIGN");
            g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/commander/commands/assign_group_button/atlas.png"),left+u.size(1.5),y+u.size(19),left+u.size(151),y+u.size(36.5),0,1f/3,0,1);
            if(assign!=null)out.add(new MapCommanderPanels.Hit(left+u.size(1.5),y+u.size(19),u.size(148),u.size(18),assign.tooltip(),()->{},(button,twice)->{if(!assign.enabled())return;if(button==InputConstants.MOUSE_BUTTON_RIGHT)MapCommanderOrders.formation("THEATER_ASSIGN",theater.id(),selection(view));else if(button==InputConstants.MOUSE_BUTTON_LEFT){boolean first=true;var covered=new HashSet<String>();for(String id:theater.groups()){if(!cards.containsKey(id))continue;MapCommanderPanels.select(id,!first);first=false;covered.add(id);}for(String id:theater.armies()){var card=cards.get(id);if(card==null||card.panel()!=null&&covered.contains(card.panel().parent()))continue;MapCommanderPanels.select(id,!first);first=false;}}},"ui.division.select","theater_assign"));
            int x=left+u.size(5);for(String id:theater.groups()){var card=cards.get(id);if(card!=null&&x+u.size(19)<right-u.size(24)){badge(g,u,out,card,theater,x,y+u.size(21.5));x+=u.size(19);}}
            for(String id:theater.armies()){var card=cards.get(id);if(card!=null&&(card.panel()==null||card.panel().parent().isEmpty())&&x+u.size(19)<right-u.size(24)){badge(g,u,out,card,theater,x,y+u.size(21.5));x+=u.size(19);}}
            label(g,Integer.toString(theater.divisions()),left+u.size(128),y+u.size(24),u.size(20),u.size(16),true);y+=u.size(36.5);
            for(String groupId:theater.groups()){
                var members=new ArrayList<Card>();for(String id:theater.armies()){var card=cards.get(id);if(card!=null&&card.panel()!=null&&card.panel().parent().equals(groupId))members.add(card);}
                var group=cards.get(groupId);if(group!=null)members.addFirst(group);
                for(int start=0;start<members.size();start+=7){UiAssets.nineSlice(g,art("theatre_dropdown_bottom").replace("frame_0","frame_"+(MapCommanderPanels.selected(groupId)?1:0)),left,y,u.size(150.5),u.size(16.5),2,u.scale()*.5);for(int n=start;n<Math.min(start+7,members.size());n++)badge(g,u,out,members.get(n),theater,left+u.size(5+(n-start)*19),y);y+=u.size(16.5);}
            }
            if(selection(view).stream().anyMatch(id->theater.armies().contains(id)||theater.groups().contains(id)))UiAssets.draw(g,art("selected_theatre_frame"),left,header+u.size(1),u.size(151.5),u.size(37));
            y+=u.size(6);
        }
        var create=view.theaters().stream().map(t->control(t,"THEATER_CREATE")).filter(Objects::nonNull).findFirst().orElse(null);
        if(create!=null&&view.cards().stream().anyMatch(c->MapCommanderPanels.selected(c.id())&&c.role()==Mode.ASSIGN_ARMY&&c.panel()!=null&&!c.panel().parent().isEmpty())){int createY=y-u.size(2.5);g.pose().pushMatrix();g.pose().translate(0,u.length(.5));UiAssets.nineSlice(g,art("theater_group_assign_btn"),left,createY,u.size(151.5),u.size(18.5),3,u.scale()*.5);label(g,UiCommandLabels.text("NEW_THEATER_GROUP",""),left+u.size(5),createY+u.size(2.5),u.size(140),u.size(14),true);g.pose().popMatrix();out.add(new MapCommanderPanels.Hit(left,createY,u.size(151.5),u.size(18.5),create.tooltip(),()->{if(create.enabled())MapCommanderOrders.formation("THEATER_CREATE","",selection(view));},null,create.enabled()?"ui.click":"","theater_create"));y+=u.size(17);}
        g.disableScissor();maximum=Math.max(0,y+scroll-bottom);scroll=Math.clamp(scroll,0,maximum);out.stream().filter(h->h.y()>=top&&h.y()+h.h()<=bottom).sorted(Comparator.comparingInt(h->h.icon().equals("theater_assign")?1:0)).forEach(hits::add);
        if(settingsTheater!=null&&settingsY>=top&&settingsY<bottom)settings(g,u,hits,settingsTheater,Math.max(0,left-u.size(153)),Math.clamp(settingsY,top,Math.max(top,bottom-u.size(38.5))));
    }
    private static void icon(GuiGraphicsExtractor g,UiUnits u,List<MapCommanderPanels.Hit> out,Theater theater,String id,String name,int frame,int x,int y,double width,double height){
        var control=control(theater,id);String asset=art(name).replace("frame_0","frame_"+frame);if(id.equals("THEATER_SETTINGS")&&(control==null||!control.enabled()))UiAssets.drawGrayscale(g,asset,x,y,u.size(width),u.size(height));else UiAssets.draw(g,asset,x,y,u.size(width),u.size(height));
        out.add(new MapCommanderPanels.Hit(x,y,u.size(width),u.size(height),control==null?"":control.tooltip(),()->{if(control!=null&&control.enabled()&&id.equals("THEATER_SETTINGS"))settings=settings.equals(theater.id())?"":theater.id();},null,control!=null&&control.enabled()?"ui.click":"",id.toLowerCase(Locale.ROOT)));
    }
    private static void battle(GuiGraphicsExtractor g,UiUnits u,List<MapCommanderPanels.Hit> out,Theater theater,String id,int frame,int x,int y){
        var control=control(theater,id);int total=-1,problems=-1;
        if(control!=null){String[] parts=control.id().split("/");if(parts.length==3)try{total=Integer.parseInt(parts[1]);problems=Integer.parseInt(parts[2]);}catch(NumberFormatException ignored){}}
        String asset=art("theatre_battle_indicator").replace("frame_0","frame_"+frame);
        if(total<=0)UiAssets.drawGrayscale(g,asset,x,y,u.size(15.3),u.size(15.3));else UiAssets.drawTinted(g,asset,x,y,u.size(15.3),u.size(15.3),problems>0?0xffff3030:0xff30d040);
        if(total>0){var font=Minecraft.getInstance().font;String value=Integer.toString(total);float tx=frame==0?x+u.length(2.5):x+u.length(13.5)-font.width(value)*UiText.scale(font);g.pose().pushMatrix();g.pose().translate(tx,y-u.length(2.5));UiText.text(g,font,value,0,0,UiMenuStyle.TEXT);g.pose().popMatrix();}
        out.add(new MapCommanderPanels.Hit(x,y-u.size(4),u.size(16),u.size(19.3),control==null?"":control.tooltip(),()->{},null,"",id.toLowerCase(Locale.ROOT)));
    }
    private static void settings(GuiGraphicsExtractor g,UiUnits u,List<MapCommanderPanels.Hit> hits,Theater theater,int x,int y){
        var out=new ArrayList<MapCommanderPanels.Hit>();var state=control(theater,"THEATER_SETTINGS");if(state==null||!state.enabled()){settings="";return;}
        UiAssets.draw(g,art("theatre_settings_bg"),x,y,u.size(152.5),u.size(38.5));
        label(g,theater.name(),x+u.size(31),y+u.size(6.5),u.size(90),u.size(10),false);
        var disband=control(theater,"THEATER_DISBAND");boolean enabled=disband!=null&&disband.enabled();
        if(enabled)UiAssets.draw(g,art("nv_btn_delete"),x+u.size(3),y+u.size(3),u.size(15.5),u.size(15.5));else UiAssets.drawGrayscale(g,art("nv_btn_delete"),x+u.size(3),y+u.size(3),u.size(15.5),u.size(15.5));
        out.add(new MapCommanderPanels.Hit(x+u.size(3),y+u.size(3),u.size(15.5),u.size(15.5),disband==null?"":disband.tooltip(),()->{if(enabled)MapCommanderHud.open(disband.request());},null,enabled?"ui.click":"","theater_disband"));
        UiAssets.draw(g,art("nv_btn_close"),x+u.size(134.5),y+u.size(3),u.size(15.5),u.size(15.5));out.add(new MapCommanderPanels.Hit(x+u.size(134.5),y+u.size(3),u.size(15.5),u.size(15.5),UiCommandLabels.text("CLOSE",""),()->settings="",null,"ui.close","theater_settings_close"));
        label(g,UiCommandLabels.text("REINFORCEMENT_PRIORITY",""),x+u.size(4),y+u.size(22),u.size(85),u.size(11.5),true);
        for(int n=0;n<3;n++){String priority=List.of("LOW","NORMAL","HIGH").get(n);boolean on=state.id().endsWith("/"+priority);int bx=x+u.size(98.5+n*17.5),by=y+u.size(n==0?18:17.5);UiAssets.draw(g,art("div_priority_strip").replace("frame_0","frame_"+(n+(on?3:0))),bx,by,u.size(18),u.size(18.5));out.add(new MapCommanderPanels.Hit(bx,by,u.size(18),u.size(18.5),UiCommandLabels.text(List.of("LOW_PRIORITY","MEDIUM_PRIORITY","HIGH_PRIORITY").get(n),""),()->MapCommanderOrders.formation("THEATER_PRIORITY/"+priority,"",List.of(theater.id())),null,"ui.click","theater_priority_"+priority.toLowerCase(Locale.ROOT)));}
        out.add(new MapCommanderPanels.Hit(x,y,u.size(152.5),u.size(38.5),"",()->{},null,"","theater_settings_background"));hits.addAll(0,out);
    }
    private static void badge(GuiGraphicsExtractor g,UiUnits u,List<MapCommanderPanels.Hit> out,Card card,Theater theater,int x,int y){
        y+=u.size(1);
        boolean group=card.role()==Mode.ARMY_GROUP;int frame=group||card.panel()==null||card.panel().insignia()==null?0:card.panel().insignia();String name=group?"theatre_army_group_shield":"theatre_army_shield";int tint=group?0xffb29854:card.panel()==null||card.panel().armyColor()==null?0xffffffff:0xff000000|card.panel().armyColor();
        UiAssets.drawTinted(g,art(name).replace("frame_0","frame_"+frame),x,y-u.size(1),u.size(16.5),u.size(16),tint);if(MapCommanderPanels.selected(card.id())||card.panel()!=null&&MapCommanderPanels.selected(card.panel().parent()))UiAssets.draw(g,art(name+"_selected").replace("frame_0","frame_"+frame),x-u.size(.5),y-u.size(1),u.size(group?16.5:17.5),u.size(group?16:17));
        label(g,Integer.toString(card.count()),x+u.size(5),y+u.size(5),u.size(14),u.size(11.5),true);String warning=theater.warnings().getOrDefault(card.id(),"");if(!warning.isEmpty())UiAssets.draw(g,art("warning_icon_l"),x,y,u.size(5),u.size(12));out.add(new MapCommanderPanels.Hit(x,y-u.size(1),u.size(19),u.size(16.5),MapCommanderHud.cardTooltip(card),()->{},(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT)MapCommanderPanels.select(card.id());else if(button==InputConstants.MOUSE_BUTTON_RIGHT&&!group)MapCommanderOrders.formation("MOVE_DIVISIONS",card.id(),MapCommanderPanels.commandSelection().stream().filter(id->id.startsWith("unit/")).toList());},"ui.division.select","theater_card"));
    }
    private static void label(GuiGraphicsExtractor g,String text,int x,int y,int width,int height,boolean centered){var font=Minecraft.getInstance().font;String value=UiText.ellipsize(font,text,Math.max(0,(int)(width/UiText.scale(font))));if(centered)UiText.centeredSingleLine(g,font,Component.literal(value),x,y,width,height,UiMenuStyle.TEXT);else UiText.text(g,font,value,x,y+(height-font.lineHeight)/2,UiMenuStyle.TEXT);}
}
