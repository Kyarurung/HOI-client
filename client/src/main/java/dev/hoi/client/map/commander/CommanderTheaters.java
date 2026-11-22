package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class CommanderTheaters {
    private static final float[] SUCCESSFUL={.043f,.86f,.043f},MIN_FAILED={.3153f,.86f,.043f},MID_FAILED={.86f,.7238f,.043f},MAX_FAILED={.86f,.043f,.043f};
    private static int scroll,maximum,left,top,right,bottom;
    private static String settings="";
    private CommanderTheaters() {}
    static void reset(){settings="";scroll=maximum=left=top=right=bottom=0;}
    static boolean closeSettings(){if(settings.isEmpty())return false;settings="";return true;}
    static boolean scrollAt(double x,double y,double amount){if(amount==0||x<left||x>=right||y<top||y>=bottom)return false;scroll=Math.clamp(scroll-(int)Math.signum(amount)*20,0,maximum);return true;}
    private static void choose(View view,String id){if(CommanderHud.theaters.choose(view,id))CommanderHud.theaterChanged();}
    private static String art(String name){return "commander/commands/"+name+"/frame_0";}
    private static Control control(Theater theater,String id){return theater.controls().stream().filter(c->c.id().equals(id)||c.id().startsWith(id+"/")).findFirst().orElse(null);}
    private static List<String> selection(View view){return view.cards().stream().filter(c->CommanderPanels.selected(c.id())&&(c.role()==Mode.ASSIGN_ARMY||c.role()==Mode.ARMY_GROUP)).map(Card::id).toList();}
    static void draw(GuiGraphicsExtractor g,UiUnits u,View view,List<CommanderPanels.Hit> hits,int limit){
        if(view.branch()==Mode.NAVY_HQ||view.branch()==Mode.AIR){drawBranch(g,u,view,hits,limit);return;}
        if(view.branch()!=Mode.ARMY||view.theaters().isEmpty()){reset();return;}
        left=g.guiWidth()-u.size(164);right=left+u.size(152.5);top=u.size(55.5);bottom=Math.max(top,limit-u.size(4));int y=top-scroll;var out=new ArrayList<CommanderPanels.Hit>();
        if(view.theaters().stream().noneMatch(t->t.id().equals(settings)))settings="";
        int settingsY=top;Theater settingsTheater=null;
        var cards=new HashMap<String,Card>();view.cards().forEach(c->cards.put(c.id(),c));g.enableScissor(left,top,right,bottom);
        for(var theater:view.theaters()){
            int header=y;UiAssets.draw(g,art("theater_group_bg"),left,y,u.size(152.5),u.size(38.5));
            if(theater.id().equals(settings)){settingsY=y;settingsTheater=theater;}
            label(g,theater.name(),left+u.size(19),y+u.size(3.5),u.size(84),u.size(16),false);
            if(view.branch()!=Mode.AIR)out.add(new CommanderPanels.Hit(left+u.size(19),y+u.size(3.5),u.size(84),u.size(16),theater.name(),()->choose(view,theater.id()),null,dev.hoi.client.audio.AudioUiSounds.selection(view.branch()),"theater_title/"+theater.id()));
            icon(g,u,out,theater,"THEATER_SETTINGS","theater_group_settings_btn",0,left+u.size(4.5),y+u.size(5.5),11.5,11.5);
            icon(g,u,out,theater,"THEATER_LOG","theatre_combat_log_icon",CommanderCombatLogHud.shows(theater.id())?1:0,left+u.size(104.5),y+u.size(2.5),13.05,17.1);
            battle(g,u,out,theater,"THEATER_OFFENSIVE",0,left+u.size(116),y+u.size(6.5));
            battle(g,u,out,theater,"THEATER_DEFENSIVE",1,left+u.size(134.5),y+u.size(6.5));
            var assign=control(theater,"THEATER_ASSIGN");
            g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/commander/commands/assign_group_button/atlas.png"),left+u.size(1.5),y+u.size(19),left+u.size(151),y+u.size(36.5),0,1f/3,0,1);
            if(assign!=null)out.add(new CommanderPanels.Hit(left+u.size(1.5),y+u.size(19),u.size(148),u.size(18),assign.tooltip(),()->{},(button,twice)->{if(!assign.enabled())return;if(button==InputConstants.MOUSE_BUTTON_RIGHT)CommanderOrders.formation("THEATER_ASSIGN",theater.id(),selection(view));else if(button==InputConstants.MOUSE_BUTTON_LEFT){choose(view,theater.id());boolean first=true;var covered=new HashSet<String>();for(String id:theater.groups()){if(!cards.containsKey(id))continue;CommanderPanels.select(id,!first);first=false;covered.add(id);}for(String id:theater.armies()){var card=cards.get(id);if(card==null||card.panel()!=null&&covered.contains(card.panel().parent()))continue;CommanderPanels.select(id,!first);first=false;}}},dev.hoi.client.audio.AudioUiSounds.selection(view.branch()),"theater_assign"));
            int x=left+u.size(5);for(String id:theater.groups()){var card=cards.get(id);if(card!=null&&x+u.size(19)<right-u.size(24)){badge(g,u,out,card,theater,x,y+u.size(21.5));x+=u.size(19);}}
            for(String id:theater.armies()){var card=cards.get(id);if(card!=null&&(card.panel()==null||card.panel().parent().isEmpty())&&x+u.size(19)<right-u.size(24)){badge(g,u,out,card,theater,x,y+u.size(21.5));x+=u.size(19);}}
            label(g,Integer.toString(theater.divisions()),left+u.size(128),y+u.size(24),u.size(20),u.size(16),true);y+=u.size(36.5);
            for(String groupId:theater.groups()){
                var members=new ArrayList<Card>();for(String id:theater.armies()){var card=cards.get(id);if(card!=null&&card.panel()!=null&&card.panel().parent().equals(groupId))members.add(card);}
                var group=cards.get(groupId);if(group!=null)members.addFirst(group);
                for(int start=0;start<members.size();start+=7){UiAssets.nineSlice(g,art("theatre_dropdown_bottom").replace("frame_0","frame_"+(CommanderPanels.selected(groupId)?1:0)),left,y,u.size(150.5),u.size(16.5),2,u.scale()*.5);for(int n=start;n<Math.min(start+7,members.size());n++)badge(g,u,out,members.get(n),theater,left+u.size(5+(n-start)*19),y);y+=u.size(16.5);}
            }
            if(CommanderHud.theaters.selected(view).equals(theater.id()))UiAssets.draw(g,art("selected_theatre_frame"),left,header+u.size(1),u.size(151.5),u.size(37));
            y+=u.size(6);
        }
        var create=view.theaters().stream().map(t->control(t,"THEATER_CREATE")).filter(Objects::nonNull).findFirst().orElse(null);
        if(create!=null&&view.cards().stream().anyMatch(c->CommanderPanels.selected(c.id())&&c.role()==Mode.ASSIGN_ARMY&&c.panel()!=null&&!c.panel().parent().isEmpty())){int createY=y-u.size(2.5);g.pose().pushMatrix();g.pose().translate(0,u.length(.5));UiAssets.nineSlice(g,art("theater_group_assign_btn"),left,createY,u.size(151.5),u.size(18.5),3,u.scale()*.5);label(g,UiCommandLabels.text("NEW_THEATER_GROUP",""),left+u.size(5),createY+u.size(2.5),u.size(140),u.size(14),true);g.pose().popMatrix();out.add(new CommanderPanels.Hit(left,createY,u.size(151.5),u.size(18.5),create.tooltip(),()->{if(create.enabled())CommanderOrders.formation("THEATER_CREATE","",selection(view));},null,create.enabled()?"ui.click":"","theater_create"));y+=u.size(17);}
        g.disableScissor();maximum=Math.max(0,y+scroll-bottom);scroll=Math.clamp(scroll,0,maximum);out.stream().filter(h->h.y()>=top&&h.y()+h.h()<=bottom).sorted(Comparator.comparingInt(h->h.icon().equals("theater_assign")?1:0)).forEach(hits::add);
        if(settingsTheater!=null&&settingsY>=top&&settingsY<bottom)settings(g,u,hits,settingsTheater,Math.max(0,left-u.size(153)),Math.clamp(settingsY,top,Math.max(top,bottom-u.size(38.5))));
    }
    private static String branchPrefix(View view){return view.branch()==Mode.NAVY_HQ?"NAVAL_THEATER":"AIR_THEATER";}
    private static List<String> branchSelection(View view){return view.branch()==Mode.AIR?view.wings().stream().filter(w->CommanderPanels.selectedWingIds().contains(w.id())).map(Member::id).toList():view.cards().stream().filter(c->c.role()==Mode.NAVY_FLEET&&CommanderPanels.selected(c.id())).map(Card::id).toList();}
    private static void selectFleets(List<String> ids){CommanderPanels.closeSelection();for(int n=0;n<ids.size();n++)CommanderPanels.select(ids.get(n),n>0);}
    private static void branchCommand(View view,Theater theater,String action){
        var c=control(theater,branchPrefix(view)+"_"+(action.startsWith("ALERT/")?"ALERT":action));if(c==null||!c.enabled())return;
        var ids=new ArrayList<String>();if(action.equals("ASSIGN")){ids.add("target/"+theater.id());ids.addAll(branchSelection(view));if(ids.size()==1)return;}else if(action.equals("CREATE")){ids.addAll(branchSelection(view));if(ids.isEmpty())return;}else ids.add(theater.id());
        CommanderOrders.send(branchPrefix(view)+"/"+action,ids,List.of());
    }
    private static void drawBranch(GuiGraphicsExtractor g,UiUnits u,View view,List<CommanderPanels.Hit> hits,int limit){
        if(view.theaters().isEmpty()){reset();return;}
        boolean navy=view.branch()==Mode.NAVY_HQ;String prefix=branchPrefix(view);var ids=view.theaters().stream().map(Theater::id).toList();if(!ids.contains(settings))settings="";
        left=g.guiWidth()-u.size(164);right=left+u.size(152.5);top=u.size(55.5);bottom=Math.max(top,limit-u.size(4));int y=top-scroll,settingsY=top;Theater settingsTheater=null;
        var out=new ArrayList<CommanderPanels.Hit>();var cards=new LinkedHashMap<String,Card>();view.cards().forEach(c->cards.put(c.id(),c));g.enableScissor(left,top,right,bottom);
        for(var theater:view.theaters()){
            int header=y;var assign=control(theater,prefix+"_ASSIGN");var fleets=theater.fleets().stream().map(cards::get).filter(Objects::nonNull).toList();var wings=view.wings().stream().filter(w->theater.wings().contains(w.id())).toList();
            UiAssets.draw(g,art("theater_group_bg").replace("frame_0",navy?"frame_2":"frame_4"),left,y,u.size(152.5),u.size(38.5));
            label(g,theater.name(),left+u.size(19),y+u.size(3.5),u.size(84),u.size(16),false);
            if(view.branch()!=Mode.AIR)out.add(new CommanderPanels.Hit(left+u.size(19),y+u.size(3.5),u.size(84),u.size(16),theater.name(),()->choose(view,theater.id()),null,dev.hoi.client.audio.AudioUiSounds.selection(view.branch()),"theater_title/"+theater.id()));
            int titleY=y;
            var disband=control(theater,prefix+"_DISBAND");if(disband!=null){UiAssets.draw(g,art("theater_group_settings_btn"),left+u.size(4.5),y+u.size(5.5),u.size(11.5),u.size(11.5));out.add(new CommanderPanels.Hit(left+u.size(4.5),y+u.size(5.5),u.size(11.5),u.size(11.5),theater.name(),()->settings=settings.equals(theater.id())?"":theater.id(),null,"ui.click","theater_settings/"+theater.id()));}
            if(navy){
                var c=control(theater,"NAVAL_THEATER_ALERT");boolean high="HIGH".equals(theater.priority());int bx=left+u.size(132.5),by=y+u.size(2);String asset=art("fleet_theatre_priority_strip").replace("frame_0",high?"frame_1":"frame_0");
                if(c!=null&&c.enabled())UiAssets.draw(g,asset,bx,by,u.size(18),u.size(18.5));else UiAssets.drawGrayscale(g,asset,bx,by,u.size(18),u.size(18.5));
                out.add(new CommanderPanels.Hit(bx,by,u.size(18),u.size(18.5),c==null?"":c.tooltip(),()->branchCommand(view,theater,"ALERT/"+(high?"NORMAL":"HIGH")),null,c!=null&&c.enabled()?"ui.click":"","theater_alert/"+theater.id()));
            }else for(int n=0;n<3;n++){
                String priority=List.of("LOW","NORMAL","HIGH").get(n);var c=control(theater,"AIR_THEATER_PRIORITY/"+priority);int frame=priority.equals(theater.priority())?n+1:0,bx=left+u.size(112.5+n*11),by=y+u.size(6.5);
                UiAssets.draw(g,art("deploy_priority").replace("frame_0","frame_"+frame),bx,by,u.size(10),u.size(10.5));out.add(new CommanderPanels.Hit(bx,by,u.size(10),u.size(10.5),c==null?"":c.tooltip(),()->branchCommand(view,theater,"PRIORITY/"+priority),null,c!=null&&c.enabled()?"ui.click":"","theater_priority/"+theater.id()+"/"+priority));
            }
            g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/commander/commands/assign_group_button/atlas.png"),left+u.size(1.5),y+u.size(19),left+u.size(113.625),y+u.size(36.5),navy?.5f:.75f,navy?.75f:1,0,1);
            if(navy){int count=fleets.size();double step=count<2?19:Math.min(19,108.0/(count-1));for(int n=count-1;n>=0;n--)fleetBadge(g,u,out,fleets.get(n),left+u.size(5+n*step),y+u.size(20.5));UiText.cell(g,Minecraft.getInstance().font,theater.ships()==null?"0":theater.ships().toString(),left+u.size(128),y+u.size(20),u.size(20),u.size(16),UiMenuStyle.TEXT,true);}else airSummary(g,u,out,wings,left+u.size(5),y+u.size(21.5),u.size(125));
            boolean selected=navy?CommanderHud.theaters.selected(view).equals(theater.id()):theater.wings().stream().anyMatch(CommanderPanels.selectedWingIds()::contains);
            if(selected)UiAssets.draw(g,art("selected_theatre_frame"),left,header+u.size(1),u.size(151.5),u.size(37));
            if(assign!=null)out.add(new CommanderPanels.Hit(left+u.size(1.5),y+u.size(19),u.size(149.5),u.size(18),assign.tooltip(),()->{},(button,twice)->{if(!assign.enabled())return;if(button==InputConstants.MOUSE_BUTTON_RIGHT)branchCommand(view,theater,"ASSIGN");else if(button==InputConstants.MOUSE_BUTTON_LEFT){if(navy){choose(view,theater.id());selectFleets(java.util.stream.Stream.concat(theater.fleets().stream(),theater.reserves().stream()).toList());}else CommanderPanels.showWings(theater.wings());}},assign.enabled()?dev.hoi.client.audio.AudioUiSounds.selection(view.branch()):"","theater_assign/"+theater.id()));
            y+=u.size(40);
            if(navy){
                for(var fleet:fleets){var forces=fleet.panel()==null?List.<Member>of():fleet.panel().members();var row=navalBadges(u,forces.size()+1,u.size(150.5),forces.isEmpty()?"0":Integer.toString(CommanderPanels.shipCount(forces.getLast())),3);
                    branchRow(g,u,CommanderPanels.selected(fleet.id())?2:0,y);var badges=new ArrayList<CommanderPanels.Hit>();
                    for(int n=forces.size()-1;n>=0;n--)forceBadge(g,u,badges,fleet,forces.get(n),left+row.x(n+1),y-u.size(3));
                    fleetBadge(g,u,badges,fleet,left+row.x(0),y-u.size(3));Collections.reverse(badges);out.addAll(badges);y+=u.size(16.5);
                }
                if(!theater.reserves().isEmpty()){
                    var reserves=theater.reserves().stream().map(cards::get).filter(Objects::nonNull).toList();var forces=reserves.stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream().map(force->Map.entry(c,force))).toList();branchRow(g,u,reserves.stream().anyMatch(c->CommanderPanels.selected(c.id()))?2:0,y);
                    var row=navalBadges(u,forces.size()+1,u.size(150.5),forces.isEmpty()?"0":Integer.toString(CommanderPanels.shipCount(forces.getLast().getValue())),5);g.pose().pushMatrix();g.pose().translate(row.x(0)-(int)row.x(0),0);UiAssets.draw(g,art("nv_icon_reserve_fleet"),left+(int)row.x(0),y-u.size(3.5),u.size(15.5),u.size(15.5));g.pose().popMatrix();var badges=new ArrayList<CommanderPanels.Hit>();for(int n=forces.size()-1;n>=0;n--){var entry=forces.get(n);forceBadge(g,u,badges,entry.getKey(),entry.getValue(),left+row.x(n+1),y-u.size(3));}Collections.reverse(badges);out.addAll(badges);
                    out.add(new CommanderPanels.Hit(left,y-u.size(3.5),u.size(150.5),u.size(16.5),dev.hoi.protocol.ProtocolTooltip.delayed(UiCommandLabels.text("RESERVE_FLEETS_FOR_THIS_THEATER",""),"\n",UiCommandLabels.text("RESERVE_FLEETS_FOR_THIS_THEATER_DESC","")),()->selectFleets(theater.reserves()),null,dev.hoi.client.audio.AudioUiSounds.selection(view.branch()),"theater_reserves/"+theater.id()));y+=u.size(16.5);
                }
            }else{
                var groups=wingGroups(wings);
                for(var group:groups.values()){branchRow(g,u,group.stream().anyMatch(w->CommanderPanels.selectedWingIds().contains(w.id()))?1:0,y);airGroupBadge(g,u,out,group,left+u.size(3),y-u.size(3));airTypes(g,u,out,group,left+u.size(22),y-u.size(3),u.size(116));y+=u.size(16.5);}
            }
            if(theater.id().equals(settings)){settingsTheater=theater;settingsY=titleY;}y+=u.size(6);
        }
        var create=view.theaters().stream().filter(t->control(t,prefix+"_CREATE")!=null).findFirst().orElse(null);
        if(create!=null){var c=control(create,prefix+"_CREATE");boolean enabled=c.enabled()&&!branchSelection(view).isEmpty();String asset=art("theater_group_assign_btn");if(enabled)UiAssets.draw(g,asset,left,y,u.size(151.5),u.size(18.5));else UiAssets.drawGrayscale(g,asset,left,y,u.size(151.5),u.size(18.5));label(g,UiCommandLabels.text("NEW_THEATER_GROUP",""),left+u.size(5),y+u.size(2.5),u.size(140),u.size(14),true);out.add(new CommanderPanels.Hit(left,y,u.size(151.5),u.size(18.5),c.tooltip(),()->branchCommand(view,create,"CREATE"),null,enabled?"ui.click":"","theater_create"));y+=u.size(20);}
        g.disableScissor();maximum=Math.max(0,y+scroll-bottom);scroll=Math.clamp(scroll,0,maximum);out.stream().filter(h->h.y()>=top&&h.y()+h.h()<=bottom).sorted(Comparator.comparingInt(h->h.icon().startsWith("theater_assign/")?0:1)).forEach(hits::add);
        if(settingsTheater!=null&&settingsY>=top&&settingsY<bottom)branchSettings(g,u,view,hits,settingsTheater,Math.max(0,left-u.size(153)),Math.clamp(settingsY,top,Math.max(top,bottom-u.size(38.5))));
    }
    private static void branchRow(GuiGraphicsExtractor g,UiUnits u,int frame,int y){UiAssets.nineSlice(g,art("theatre_dropdown_bottom").replace("frame_0","frame_"+frame),left,y-u.size(4),u.size(150.5),u.size(16.5),2,u.scale()*.5);}
    private static void fleetBadge(GuiGraphicsExtractor g,UiUnits u,List<CommanderPanels.Hit> out,Card fleet,float exact,int y){int x=(int)Math.floor(exact);float shift=exact-x;g.pose().pushMatrix();g.pose().translate(shift,0);
        int frame=fleet.panel()==null||fleet.panel().insignia()==null?3:fleet.panel().insignia();UiAssets.drawTinted(g,art("theatre_army_shield").replace("frame_0","frame_"+frame),x,y-u.size(1),u.size(16.5),u.size(16),CommanderPanels.fleetColor(fleet));if(CommanderPanels.selected(fleet.id()))UiAssets.draw(g,art("theatre_army_shield_selected").replace("frame_0","frame_"+frame),x-u.size(.5),y-u.size(1),u.size(17.5),u.size(17));int count=CommanderPanels.fleetShipCount(fleet);label(g,count<0?"0":Integer.toString(count),x+u.size(3),y+u.size(5),u.size(18),u.size(11.5),true);out.add(new CommanderPanels.Hit(x,y,u.size(21),u.size(16.5),CommanderHud.cardTooltip(fleet),()->CommanderPanels.select(fleet.id()),null,"ui.fleet.select","theater_fleet/"+fleet.id()).shiftedX(shift));g.pose().popMatrix();
    }
    static CommanderLayout.BadgeRow navalBadges(UiUnits u,int slots,int width,String lastCount,float first){var font=Minecraft.getInstance().font;float right=u.size(3)+u.size(18)/2f+font.width(lastCount)*UiText.scale(font)/2-1.5f;return CommanderLayout.navalBadges(u,slots,width,right,first,Minecraft.getInstance().getWindow().getGuiScale());}
    static void navalCount(GuiGraphicsExtractor g,UiUnits u,String count,int x,int y){label(g,count,x+u.size(3),y+u.size(5),u.size(18),u.size(11.5),true);}
    private static void forceBadge(GuiGraphicsExtractor g,UiUnits u,List<CommanderPanels.Hit> out,Card fleet,Member force,float exact,int y){int x=(int)Math.floor(exact);float shift=exact-x;g.pose().pushMatrix();g.pose().translate(shift,0);
        UiAssets.drawTinted(g,force.icon(),x,y,u.size(16.5),u.size(16),CommanderPanels.fleetColor(fleet));navalCount(g,u,Integer.toString(CommanderPanels.shipCount(force)),x,y);out.add(new CommanderPanels.Hit(x,y,u.size(21),u.size(16.5),force.name()+"\n"+force.status()+"\n\n"+force.detail(),()->CommanderPanels.selectTheaterForce(fleet,force),null,"ui.fleet.select","theater_force/"+force.id()).shiftedX(shift));g.pose().popMatrix();
    }
    private static Map<String,List<Member>> wingGroups(List<Member> wings){var groups=new LinkedHashMap<String,List<Member>>();for(var wing:wings)if(wing.data()!=null&&wing.data().air()!=null&&!wing.data().air().airGroup().isEmpty())groups.computeIfAbsent(wing.data().air().airGroup(),ignored->new ArrayList<>()).add(wing);return groups;}
    private static void airGroupBadge(GuiGraphicsExtractor g,UiUnits u,List<CommanderPanels.Hit> out,List<Member> wings,int x,int y){
        UiAssets.draw(g,art("theatre_army_shield"),x,y-u.size(1),u.size(16.5),u.size(16));if(wings.stream().anyMatch(w->CommanderPanels.selectedWingIds().contains(w.id())))UiAssets.draw(g,art("theatre_army_shield_selected"),x-u.size(.5),y-u.size(1),u.size(17.5),u.size(17));label(g,Integer.toString(wings.size()),x+u.size(3),y+u.size(4),u.size(18),u.size(11.5),true);var air=wings.getFirst().data().air();out.add(new CommanderPanels.Hit(x,y-u.size(1),u.size(21),u.size(17.5),air.airGroupName(),()->CommanderPanels.showWings(wings.stream().map(Member::id).toList()),null,"ui.aircraft.select","theater_air_group/"+air.airGroup()));
    }
    private static void airSummary(GuiGraphicsExtractor g,UiUnits u,List<CommanderPanels.Hit> out,List<Member> wings,int x,int y,int width){
        var groups=wingGroups(wings);var grouped=groups.values().stream().flatMap(Collection::stream).map(Member::id).collect(java.util.stream.Collectors.toSet());var ungrouped=wings.stream().filter(w->!grouped.contains(w.id())).toList();int types=(int)ungrouped.stream().map(Member::icon).distinct().count(),slots=groups.size()+types;double step=slots<2?u.size(19):Math.min(u.size(19),(width-u.size(21))/(double)(slots-1));int n=0;for(var group:groups.values())airGroupBadge(g,u,out,group,x+(int)Math.round(n++*step),y);int offset=(int)Math.round(n*step);airTypes(g,u,out,ungrouped,x+offset,y,Math.max(u.size(21),width-offset));
    }
    private static void airTypes(GuiGraphicsExtractor g,UiUnits u,List<CommanderPanels.Hit> out,List<Member> wings,int x,int y,int width){
        var types=new LinkedHashMap<String,List<Member>>();for(var wing:wings)types.computeIfAbsent(wing.icon(),ignored->new ArrayList<>()).add(wing);double step=types.size()<2?u.size(19):Math.min(u.size(19),(width-u.size(21))/(double)(types.size()-1));int n=0;
        for(var entry:types.entrySet()){int bx=x+(int)Math.round(n++*step);String role=entry.getKey().replace("unit_hud/air_","");if(!UiAssets.draw(g,"production/design/roles/"+role,bx,y,u.size(15),u.size(6)))UiAssets.draw(g,entry.getKey(),bx,y,u.size(15),u.size(6));label(g,Integer.toString(entry.getValue().size()),bx+u.size(3),y+u.size(4),u.size(18),u.size(11.5),true);String tip=entry.getValue().stream().map(w->w.name()+"\n"+w.status()).collect(java.util.stream.Collectors.joining("\n\n"));out.add(new CommanderPanels.Hit(bx,y,u.size(21),u.size(16.5),tip,()->CommanderPanels.showWings(entry.getValue().stream().map(Member::id).toList()),null,"ui.aircraft.select","theater_air_type/"+role));}
    }
    private static void branchSettings(GuiGraphicsExtractor g,UiUnits u,View view,List<CommanderPanels.Hit> hits,Theater theater,int x,int y){
        var out=new ArrayList<CommanderPanels.Hit>();UiAssets.draw(g,art("theatre_settings_bg"),x,y,u.size(152.5),u.size(38.5));label(g,theater.name(),x+u.size(31),y+u.size(6.5),u.size(90),u.size(10),false);var disband=control(theater,branchPrefix(view)+"_DISBAND");boolean enabled=disband!=null&&disband.enabled();
        if(enabled)UiAssets.draw(g,art("nv_btn_delete"),x+u.size(3),y+u.size(3),u.size(15.5),u.size(15.5));else UiAssets.drawGrayscale(g,art("nv_btn_delete"),x+u.size(3),y+u.size(3),u.size(15.5),u.size(15.5));out.add(new CommanderPanels.Hit(x+u.size(3),y+u.size(3),u.size(15.5),u.size(15.5),disband==null?"":disband.tooltip(),()->branchCommand(view,theater,"DISBAND"),null,enabled?"ui.disband":"","theater_disband"));
        UiAssets.draw(g,art("nv_btn_close"),x+u.size(134.5),y+u.size(3),u.size(15.5),u.size(15.5));out.add(new CommanderPanels.Hit(x+u.size(134.5),y+u.size(3),u.size(15.5),u.size(15.5),UiCommandLabels.text("CLOSE",""),()->settings="",null,"ui.close","theater_settings_close"));out.add(new CommanderPanels.Hit(x,y,u.size(152.5),u.size(38.5),"",()->{},null,"","theater_settings_background"));hits.addAll(0,out);
    }
    private static void icon(GuiGraphicsExtractor g,UiUnits u,List<CommanderPanels.Hit> out,Theater theater,String id,String name,int frame,int x,int y,double width,double height){
        var control=control(theater,id);String asset=art(name).replace("frame_0","frame_"+frame);if(id.equals("THEATER_SETTINGS")&&(control==null||!control.enabled()))UiAssets.drawGrayscale(g,asset,x,y,u.size(width),u.size(height));else UiAssets.draw(g,asset,x,y,u.size(width),u.size(height));
        out.add(new CommanderPanels.Hit(x,y,u.size(width),u.size(height),control==null?"":control.tooltip(),()->{if(control==null||!control.enabled())return;if(id.equals("THEATER_LOG"))CommanderCombatLogHud.open(theater.id());else settings=settings.equals(theater.id())?"":theater.id();},null,control!=null&&control.enabled()?"ui.click":"",id.toLowerCase(Locale.ROOT)));
    }
    static int combatColor(int total,int problems){
        float[] color={.8f,.8f,.8f};
        if(total>0&&problems<=0)color=SUCCESSFUL;
        else if(total>0){float ratio=Math.min(1,problems/(float)total);float[] from=ratio<.5f?MIN_FAILED:MID_FAILED,to=ratio<.5f?MID_FAILED:MAX_FAILED;color=new float[3];for(int i=0;i<3;i++)color[i]=from[i]+(to[i]-from[i])*2*(ratio<.5f?ratio:ratio-.5f);}
        int tint=0xFF000000;for(int i=0;i<3;i++)tint|=Math.round((float)Math.pow(color[i],2.2)*255)<<16-8*i;
        return tint;
    }
    private static void battle(GuiGraphicsExtractor g,UiUnits u,List<CommanderPanels.Hit> out,Theater theater,String id,int frame,int x,int y){
        var control=control(theater,id);int total=-1,problems=-1;
        if(control!=null){String[] parts=control.id().split("/");if(parts.length==3)try{total=Integer.parseInt(parts[1]);problems=Integer.parseInt(parts[2]);}catch(NumberFormatException ignored){}}
        String asset=art("theatre_battle_indicator").replace("frame_0","frame_"+frame);
        UiAssets.drawTinted(g,asset,x,y,u.size(15.3),u.size(15.3),combatColor(total,problems));
        if(total>0){var font=Minecraft.getInstance().font;String value=Integer.toString(total);float tx=frame==0?x+u.length(2.5):x+u.length(13.5)-font.width(value)*UiText.scale(font);g.pose().pushMatrix();g.pose().translate(tx,y-u.length(2.5));UiText.text(g,font,value,0,0,UiMenuStyle.TEXT);g.pose().popMatrix();}
        out.add(new CommanderPanels.Hit(x,y-u.size(4),u.size(16),u.size(19.3),control==null?"":control.tooltip(),()->{},null,"",id.toLowerCase(Locale.ROOT)));
    }
    private static void settings(GuiGraphicsExtractor g,UiUnits u,List<CommanderPanels.Hit> hits,Theater theater,int x,int y){
        var out=new ArrayList<CommanderPanels.Hit>();var state=control(theater,"THEATER_SETTINGS");if(state==null||!state.enabled()){settings="";return;}
        UiAssets.draw(g,art("theatre_settings_bg"),x,y,u.size(152.5),u.size(38.5));
        label(g,theater.name(),x+u.size(31),y+u.size(6.5),u.size(90),u.size(10),false);
        var disband=control(theater,"THEATER_DISBAND");boolean enabled=disband!=null&&disband.enabled();
        if(enabled)UiAssets.draw(g,art("nv_btn_delete"),x+u.size(3),y+u.size(3),u.size(15.5),u.size(15.5));else UiAssets.drawGrayscale(g,art("nv_btn_delete"),x+u.size(3),y+u.size(3),u.size(15.5),u.size(15.5));
        out.add(new CommanderPanels.Hit(x+u.size(3),y+u.size(3),u.size(15.5),u.size(15.5),disband==null?"":disband.tooltip(),()->{if(enabled)CommanderHud.open(disband.request());},null,enabled?"ui.click":"","theater_disband"));
        UiAssets.draw(g,art("nv_btn_close"),x+u.size(134.5),y+u.size(3),u.size(15.5),u.size(15.5));out.add(new CommanderPanels.Hit(x+u.size(134.5),y+u.size(3),u.size(15.5),u.size(15.5),UiCommandLabels.text("CLOSE",""),()->settings="",null,"ui.close","theater_settings_close"));
        label(g,UiCommandLabels.text("REINFORCEMENT_PRIORITY",""),x+u.size(4),y+u.size(22),u.size(85),u.size(11.5),true);
        for(int n=0;n<3;n++){String priority=List.of("LOW","NORMAL","HIGH").get(n);boolean on=state.id().endsWith("/"+priority);int bx=x+u.size(98.5+n*17.5),by=y+u.size(n==0?18:17.5);UiAssets.draw(g,art("div_priority_strip").replace("frame_0","frame_"+(n+(on?3:0))),bx,by,u.size(18),u.size(18.5));out.add(new CommanderPanels.Hit(bx,by,u.size(18),u.size(18.5),dev.hoi.protocol.ProtocolTooltip.delayed(UiCommandLabels.text(List.of("LOW_PRIORITY","MEDIUM_PRIORITY","HIGH_PRIORITY").get(n),""),"\n",UiCommandLabels.text("THEATRE_PRIORITY_DESC","")),()->CommanderOrders.formation("THEATER_PRIORITY/"+priority,"",List.of(theater.id())),null,"ui.click","theater_priority_"+priority.toLowerCase(Locale.ROOT)));}
        out.add(new CommanderPanels.Hit(x,y,u.size(152.5),u.size(38.5),"",()->{},null,"","theater_settings_background"));hits.addAll(0,out);
    }
    private static void badge(GuiGraphicsExtractor g,UiUnits u,List<CommanderPanels.Hit> out,Card card,Theater theater,int x,int y){
        y+=u.size(1);
        boolean group=card.role()==Mode.ARMY_GROUP;int frame=group||card.panel()==null||card.panel().insignia()==null?0:card.panel().insignia();String name=group?"theatre_army_group_shield":"theatre_army_shield";int tint=group?0xffb29854:card.panel()==null||card.panel().armyColor()==null?0xffffffff:0xff000000|card.panel().armyColor();
        UiAssets.drawTinted(g,art(name).replace("frame_0","frame_"+frame),x,y-u.size(1),u.size(16.5),u.size(16),tint);if(CommanderPanels.selected(card.id())||card.panel()!=null&&CommanderPanels.selected(card.panel().parent()))UiAssets.draw(g,art(name+"_selected").replace("frame_0","frame_"+frame),x-u.size(.5),y-u.size(1),u.size(group?16.5:17.5),u.size(group?16:17));
        label(g,Integer.toString(card.count()),x+u.size(5),y+u.size(5),u.size(14),u.size(11.5),true);String warning=theater.warnings().getOrDefault(card.id(),"");if(!warning.isEmpty())UiAssets.draw(g,art("warning_icon_l"),x,y,u.size(5),u.size(12));out.add(new CommanderPanels.Hit(x,y-u.size(1),u.size(19),u.size(16.5),CommanderHud.cardTooltip(card),()->{},(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT)CommanderPanels.select(card.id());else if(button==InputConstants.MOUSE_BUTTON_RIGHT&&!group)CommanderOrders.formation("MOVE_DIVISIONS",card.id(),CommanderPanels.commandSelection().stream().filter(id->id.startsWith("unit/")).toList());},dev.hoi.client.audio.AudioUiSounds.selection(card.role()),"theater_card"));
    }
    private static void label(GuiGraphicsExtractor g,String text,int x,int y,int width,int height,boolean centered){var font=Minecraft.getInstance().font;if(centered)UiText.centeredSingleLine(g,font,Component.literal(text),x,y,width,height,UiMenuStyle.TEXT);else UiText.marquee(g,font,dev.hoi.client.ui.tooltip.Tooltips.officerText(text),x,y+(height-font.lineHeight)/2,width,UiText.scale(font),UiMenuStyle.TEXT,true,-1);}
}
