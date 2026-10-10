package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class MapCommanderPanels {
    private static final LinkedHashSet<String> selected=new LinkedHashSet<>();
    private static final LinkedHashSet<String> selectedForces=new LinkedHashSet<>();
    private static final LinkedHashSet<String> selectedWings=new LinkedHashSet<>();
    private static List<Hit> hits=List.of();
    private static View current;
    private static List<Member> divisionDetails=List.of();
    private static final Set<String> fullArmyCards=new HashSet<>(),manualArmyKeys=new HashSet<>();
    private static int scroll,forcePage;
    private static boolean wingList,postureMenu,navalTargetMenu,specificForces;
    static record Hit(int x,int y,int w,int h,String tip,Runnable action,java.util.function.BiConsumer<Integer,Boolean> gesture,String sound,String icon){Hit(int x,int y,int w,int h,String tip,Runnable action,java.util.function.BiConsumer<Integer,Boolean> gesture,String sound){this(x,y,w,h,tip,action,gesture,sound,"");}Hit {tip=UiCommandLabels.withoutShortcuts(tip);}Hit(int x,int y,int w,int h,String tip,Runnable action){this(x,y,w,h,tip,action,null,"ui.click");}Hit(int x,int y,int w,int h,String tip,Runnable action,java.util.function.BiConsumer<Integer,Boolean> gesture){this(x,y,w,h,tip,action,gesture,"");}boolean contains(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}}
    static List<Hit> hits(){return hits;}
    static String tooltipAt(double x,double y){for(var hit:hits)if(hit.contains(x,y))return hit.tip();return "";}
    static boolean scrollAt(double x,double y,double amount){if(amount==0||hits.stream().noneMatch(h->h.contains(x,y)))return false;scroll=Math.max(0,scroll-(int)Math.signum(amount)*3);return true;}
    private MapCommanderPanels() {}
    static void accept(View view) {
        if(current==null||!current.country().equals(view.country())||!current.dimension().equals(view.dimension())||current.branch()!=view.branch()){if(current!=null){MapUnitHud.clearSelection();MapCommanderOrders.scopeChanged(current,view);}reset();}
        var previousMembers=knownArmyMembers(current);current=view;selectedForces.removeIf(id->view.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).noneMatch(m->m.id().equals(id)));selected.removeIf(id->view.cards().stream().noneMatch(c->c.id().equals(id)));if(view.branch()==Mode.NAVY_HQ&&specificForces&&selectedForces.isEmpty())selected.clear();selectedWings.removeIf(id->view.wings().stream().noneMatch(w->w.id().equals(id)));hits=List.of();fullArmyCards.retainAll(selected);if(view.branch()==Mode.ARMY&&view.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY||c.role()==Mode.ARMY_GROUP).allMatch(c->c.panel()!=null&&c.panel().divisionIds()!=null)){previousMembers.removeAll(knownArmyMembers(view));manualArmyKeys.removeIf(key->key.startsWith("unit/")&&previousMembers.contains(key.substring(5)));}if(view.branch()==Mode.ARMY)syncArmySelection();MapCommanderOrders.selectionChanged();
    }
    static void reset(){if(current!=null)MapUnitHud.clearSelection();fullArmyCards.clear();manualArmyKeys.clear();current=null;divisionDetails=List.of();selected.clear();selectedForces.clear();specificForces=false;selectedWings.clear();hits=List.of();scroll=0;forcePage=0;wingList=false;postureMenu=false;navalTargetMenu=false;}
    static boolean selected(String id){return selected.contains(id);}
    static Set<String> selectedCards(){return Set.copyOf(selected);}
    static Set<String> selectedWingIds(){return Set.copyOf(selectedWings);}
    private static boolean wingSelected(Member wing){return selectedWings.contains(wing.id())||current!=null&&current.branch()!=Mode.AIR&&!wing.selectionKey().isEmpty()&&MapUnitHud.selectedKeys().contains(wing.selectionKey());}
    static List<String> attachmentWings(){return current==null?List.of():current.wings().stream().filter(MapCommanderPanels::wingSelected).map(Member::id).toList();}
    static boolean openMission(Open request){
        if(current==null)return false;
        if(request.mode()==Mode.AIR_MISSION){if(current.wings().stream().noneMatch(wing->wing.id().equals(request.target())))return false;showWings(List.of(request.target()));return true;}
        if(current.branch()!=Mode.NAVY_HQ)return false;
        if(request.mode()==Mode.NAVAL_MISSION){
            var card=current.cards().stream().filter(c->c.panel()!=null&&c.panel().members().stream().anyMatch(force->force.id().equals(request.target()))).findFirst().orElse(null);if(card==null)return false;
            MapUnitHud.clearSelection();selected.clear();selected.add(card.id());selectedForces.clear();selectedForces.add(request.target());specificForces=true;scroll=0;hits=List.of();MapCommanderOrders.selectionChanged();return true;
        }
        if(request.mode()!=Mode.NAVAL_TASK_FORCES)return false;var card=current.cards().stream().filter(c->c.id().equals(request.target())).findFirst().orElse(null);
        if(card==null||card.panel()==null||card.panel().total()!=card.panel().members().size())return false;
        if(!selected.contains(card.id()))select(card.id(),false);else {selected.retainAll(Set.of(card.id()));selectedForces.removeIf(id->card.panel().members().stream().noneMatch(force->force.id().equals(id)));hits=List.of();MapCommanderOrders.selectionChanged();}
        openMissionPanel();return true;
    }
    static void openMissionPanel(){if(current==null)return;if(current.branch()==Mode.NAVY_HQ){if(selected.isEmpty())MapCommanderOrders.message("임무를 지정할 함대를 먼저 선택하세요.");else {wingList=false;hits=List.of();}}else {var wings=attachmentWings();if(wings.isEmpty())MapCommanderOrders.message("임무를 지정할 비행단을 먼저 선택하세요.");else showWings(wings);}}
    static void showWings(Collection<String> ids){MapUnitHud.clearSelection();selectWings(ids);wingList=true;}
    static void selectWings(Collection<String> ids){fullArmyCards.clear();manualArmyKeys.clear();selectedWings.clear();if(current!=null)current.wings().stream().filter(w->ids.contains(w.id())).map(Member::id).forEach(selectedWings::add);scroll=0;hits=List.of();postureMenu=false;MapCommanderOrders.selectionChanged();}
    static void select(String id){select(id,shift());}
    static void select(String id,boolean additive){
        if(current==null||current.cards().stream().noneMatch(c->c.id().equals(id)))return;
        current.cards().stream().filter(c->c.id().equals(id)).findFirst().ifPresent(MapCommanderHud.groups::reveal);var previous=Set.copyOf(selected);selectedForces.clear();specificForces=false;if(current.branch()==Mode.ARMY)selectedWings.clear();if(!additive){selected.clear();selected.add(id);}else if(!selected.remove(id))selected.add(id);
        if(current.branch()==Mode.ARMY){if(!additive){fullArmyCards.clear();manualArmyKeys.clear();fullArmyCards.add(id);}else if(selected.contains(id))fullArmyCards.add(id);else {fullArmyCards.remove(id);retainManualArmySelection();}syncArmySelection();}else MapUnitHud.clearSelection();scroll=0;hits=List.of();MapCommanderOrders.selectionChanged();if(!previous.equals(selected))MapCommanderOrders.selectionScopeChanged();
    }
    static void acceptDivisionDetails(List<Member> members){
        if(current==null||current.branch()!=Mode.ARMY)return;divisionDetails=List.copyOf(members);
        if(fullArmyCards.isEmpty()){MapUnitHud.replaceDivisions(members.stream().map(Member::id).toList());manualArmyKeys.clear();manualArmyKeys.addAll(MapUnitHud.selectedKeys());alignArmyCards();}
        hits=List.of();
    }
    private static void alignArmyCards(){
        if(current==null||current.branch()!=Mode.ARMY)return;var ids=MapUnitHud.selectedKeys();
        selected.removeIf(id->current.cards().stream().filter(c->c.id().equals(id)&&c.panel()!=null&&c.panel().divisionIds()!=null).anyMatch(c->c.panel().divisionIds().stream().noneMatch(d->ids.contains("unit/"+d))));
        var covered=new HashSet<String>();current.cards().stream().filter(c->selected.contains(c.id())&&c.panel()!=null&&c.panel().divisionIds()!=null).forEach(c->covered.addAll(c.panel().divisionIds()));
        for(var card:current.cards())if(card.role()==Mode.ASSIGN_ARMY&&card.panel()!=null&&card.panel().divisionIds()!=null&&card.panel().divisionIds().stream().anyMatch(id->ids.contains("unit/"+id)&&!covered.contains(id))){selected.add(card.id());covered.addAll(card.panel().divisionIds());}
    }
    static List<Member> selectedDivisionRows(){
        var rows=new LinkedHashMap<String,Member>();if(current!=null)current.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).filter(m->m.request().mode()==Mode.ARMY_UNITS).forEach(m->rows.putIfAbsent(m.id(),m));divisionDetails.forEach(m->rows.put(m.id(),m));
        return rows.values().stream().filter(m->MapUnitHud.selectedKeys().contains("unit/"+m.id())).sorted(Comparator.comparing(Member::id)).toList();
    }
    private static List<Member> armyRows(Card card){
        var panel=card.panel();if(panel==null)return List.of();var ids=panel.divisionIds();return selectedDivisionRows().stream().filter(m->ids!=null?ids.contains(m.id()):panel.members().stream().anyMatch(row->row.id().equals(m.id()))).toList();
    }
    static void manualDivisionSelection(){fullArmyCards.clear();manualArmyKeys.clear();}
    private static Set<String> knownArmyMembers(View view){var result=new HashSet<String>();if(view!=null&&view.branch()==Mode.ARMY)for(var card:view.cards())if(card.panel()!=null&&card.panel().divisionIds()!=null)result.addAll(card.panel().divisionIds());return result;}
    private static void retainManualArmySelection(){var allowed=new HashSet<String>();for(var card:current.cards())if(selected.contains(card.id())&&card.panel()!=null&&card.panel().divisionIds()!=null)card.panel().divisionIds().forEach(id->allowed.add("unit/"+id));manualArmyKeys.removeIf(key->key.startsWith("unit/")&&!allowed.contains(key));}
    static void counterSelectionRefreshed(){var keys=MapUnitHud.selectedKeys();manualArmyKeys.removeIf(key->!key.startsWith("unit/")&&!keys.contains(key));}
    private static void syncArmySelection(){
        var ids=new TreeSet<String>(manualArmyKeys);boolean missing=false,oversized=false;
        for(var card:current.cards())if(fullArmyCards.contains(card.id())&&(card.role()==Mode.ASSIGN_ARMY||card.role()==Mode.ARMY_GROUP)){if(card.panel()==null||card.panel().divisionIds()==null){missing=true;oversized|=card.panel()!=null&&card.panel().total()>2048;}else card.panel().divisionIds().forEach(id->ids.add("unit/"+id));}
        if(missing||ids.size()>2048){MapUnitHud.replaceDivisions(List.of());MapCommanderOrders.message(oversized||ids.size()>2048?"선택한 사단이 명령 입력 한도 2048개를 초과했습니다.":"군 구성원 정보를 기다리고 있습니다.");return;}
        MapUnitHud.replaceKeys(ids);
    }
    static List<String> commandSelection(){
        var map=MapUnitHud.selectedKeys();if(current!=null&&current.branch()==Mode.NAVY_HQ)return navalSelection().stream().map(id->"navy/"+id).sorted().toList();
        if(current!=null&&current.branch()==Mode.AIR)return attachmentWings().stream().map(id->"air/"+id).sorted().toList();
        if(!map.isEmpty())return map.stream().sorted().toList();return current==null?List.of():attachmentWings().stream().map(id->"air/"+id).sorted().toList();
    }
    static void mapSelectionChanged(){fullArmyCards.clear();manualArmyKeys.clear();manualArmyKeys.addAll(MapUnitHud.selectedKeys());alignArmyCards();selectedWings.clear();selectedForces.clear();specificForces=false;if(current!=null&&current.branch()==Mode.NAVY_HQ){selected.clear();for(var card:current.cards())if(card.panel()!=null)for(var member:card.panel().members())if(MapUnitHud.selectedKeys().stream().anyMatch(key->key.startsWith(member.id()+"|navy_"))){selected.add(card.id());selectedForces.add(member.id());specificForces=true;}}if(current!=null&&current.branch()==Mode.AIR)current.wings().stream().filter(w->!w.selectionKey().isEmpty()&&MapUnitHud.selectedKeys().contains(w.selectionKey())).map(Member::id).forEach(selectedWings::add);MapCommanderOrders.selectionChanged();}
    static boolean counterSelected(String key){
        if(current!=null&&current.branch()==Mode.AIR)return current.wings().stream().anyMatch(w->selectedWings.contains(w.id())&&w.selectionKey().equals(key));
        if(current!=null&&current.branch()==Mode.NAVY_HQ)return navalSelection().stream().anyMatch(id->key.startsWith(id+"|navy_"));
        if(MapUnitHud.selectedKeys().contains(key))return true;if(current==null)return false;
        return selectedDivisionRows().stream().anyMatch(m->!m.selectionKey().isEmpty()&&m.selectionKey().equals(key));
    }
    static String hoverKey(double x,double y){for(var hit:hits)if(hit.contains(x,y))return hit.sound().isEmpty()&&hit.gesture()==null?"":"panel/"+hit.x()+"/"+hit.y()+"/"+hit.w()+"/"+hit.h();return "";}
    static boolean shift(){return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)||InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);}
    static boolean click(double x,double y){return click(x,y,InputConstants.MOUSE_BUTTON_LEFT,false);}
    static boolean click(double x,double y,int button,boolean twice){for(var hit:hits)if(hit.contains(x,y)){if(hit.gesture()!=null){if(button==InputConstants.MOUSE_BUTTON_LEFT||button==InputConstants.MOUSE_BUTTON_RIGHT)dev.hoi.client.audio.AudioUiSounds.play(hit.sound());hit.gesture().accept(button,twice);}else if(button==InputConstants.MOUSE_BUTTON_LEFT){dev.hoi.client.audio.AudioUiSounds.play(hit.sound());hit.action().run();}MapCommanderOrders.selectionChanged();return true;}return false;}
    private static boolean control(){return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)||InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);}
    static boolean armyInput(){return current!=null&&current.branch()==Mode.ARMY&&attachmentWings().isEmpty();}
    static List<String> formationArmies(){return current==null?List.of():current.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&fullArmyCards.contains(c.id())).map(Card::id).sorted().toList();}
    static List<String> armyIds(){return selectedArmies().stream().map(Card::id).toList();}
    private static void navalAreas(boolean clear){var ids=navalSelection();if(!ids.isEmpty())MapCommanderOrders.send(clear?"NAVAL_AREAS/CLEAR":"NAVAL_AREAS/REMOVE",ids,List.of());}
    private static boolean marshalSkills(){return current!=null&&current.cards().stream().anyMatch(c->c.role()==Mode.ARMY_GROUP&&selected.contains(c.id()));}
    private static String abilityAction(String ability){return "ABILITY/"+(marshalSkills()?"MARSHAL/":"")+ability.toUpperCase(Locale.ROOT);}
    private static void armyAbility(String ability){MapCommanderOrders.send(abilityAction(ability),selectedDivisions(),List.of());}
    private static void armyAbilities(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y,int w){int left=x+(w-u.size(99))/2,top=y-u.size(24);UiAssets.draw(g,source("ability_list_begin"),left,top,u.size(49.5),u.size(22.5));UiAssets.draw(g,source("ability_list_end"),left+u.size(49.5),top,u.size(49.5),u.size(22.5));String[] ids=marshalSkills()?new String[]{"staff_office_plan","extra_suplies"}:new String[]{"force_attack","last_stand","staff_office_plan","extra_suplies"};double start=(99-((ids.length-1)*18+17))/2.0;for(int n=0;n<ids.length;n++){String id=ids[n],key="ABILITY_"+id.toUpperCase(Locale.ROOT);int width=id.equals("force_attack")||id.equals("extra_suplies")?34:33,height=id.equals("staff_office_plan")?32:33;String original=UiCommandLabels.text(key,id)+"\n\n"+UiCommandLabels.text(key+"_DESC","");button(g,out,"","ability_"+id,left+u.size(start+n*18),top+u.size(4),u.size(width/2.0),u.size(height/2.0),MapCommanderAvailability.tooltip(abilityAction(id),original),()->armyAbility(id));if(!MapCommanderAvailability.reason(abilityAction(id)).isEmpty()){var hit=out.getLast();g.fill(hit.x(),hit.y(),hit.x()+hit.w(),hit.y()+hit.h(),0x55000000);}}}
    private static List<String> navalSelection(){return current==null?List.of():current.cards().stream().filter(c->selected.contains(c.id())&&c.panel()!=null).flatMap(c->c.panel().members().stream()).map(Member::id).filter(id->!specificForces||selectedForces.contains(id)).distinct().toList();}
    private static void navalOrganization(String action,boolean half){var ids=navalSelection();if(ids.isEmpty())return;var values=new ArrayList<String>();ids.forEach(id->values.add("force/"+id));if(half)values.add("half");MapCommanderHud.open(new Open(Mode.NAVAL_ORGANIZE,action,values));}
    private static void navalTools(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y,int w){
        background(g,"task_force_actions_bg",x,y,w,u.size(21));
        button(g,out,"","nv_btn_create_new_from_selected",x+u.size(32),y+u.size(3),u.size(15.5),u.size(15.5),UiCommandLabels.text("CREATE_NEW_TASK_FORCE_FROM_SELECTION","새 태스크 포스 생성"),()->navalOrganization("SHIPS",false));
        button(g,out,"","nv_btn_split_in_half",x+u.size(47),y+u.size(3),u.size(15.5),u.size(15.5),UiCommandLabels.text("NAVY_SPLIT_IN_HALF","태스크 포스를 둘로 분할")+"\n\n분리될 실제 함선을 확인한 뒤 적용합니다.\nHOI는 함선 ID순으로 나누며 홀수의 남는 한 척은 원래 태스크 포스에 남깁니다.",()->navalOrganization("SPLIT",false));
        button(g,out,"","nv_btn_select_half",x+u.size(62),y+u.size(3),u.size(15.5),u.size(15.5),UiCommandLabels.text("NAVY_SELECT_HALF","현재 함선 선택을 절반으로 줄임")+"\n\n태스크 포스를 선택한 뒤 실제 함선 선택 화면을 엽니다. 선택만 바꾸며 함선을 분리하지 않습니다.",()->navalOrganization("SHIPS",true));
        button(g,out,"","nv_btn_merge",x+u.size(92),y+u.size(3),u.size(15.5),u.size(15.5),UiCommandLabels.text("NAVAL_GUI_BTN_MERGE","태스크 포스 병합"),()->navalOrganization("MERGE",false));
    }
    private static List<String> selectedDivisions(){return MapUnitHud.selectedKeys().stream().filter(id->id.startsWith("unit/")).sorted().toList();}
    private static void landSettings(String action){if(!MapCommanderAvailability.allow("ARMY/"+action))return;var ids=selectedDivisions();if(!ids.isEmpty())MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,action,ids));}
    private static void haltSelected(){if(!MapCommanderAvailability.allow("ARMY/HALT_UNITS"))return;var ids=selectedDivisions();if(!ids.isEmpty())MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,"HALT_UNITS",ids));}
    private static void missionShortcut(boolean navy,List<String> ids,String mission){if(!MapCommanderAvailability.allow((navy?"NAVY/":"AIR/")+mission))return;dev.hoi.client.audio.AudioUiSounds.play("ui.click");var values=new ArrayList<String>();values.add("mission/"+mission);values.addAll(ids);MapCommanderHud.open(new Open(navy?Mode.NAVAL_TOOLBAR:Mode.AIR_TOOLBAR,"",values));}
    private static List<Card> selectedArmies(){return current==null?List.of():current.cards().stream().filter(c->c.panel()!=null&&c.role()!=Mode.ARMY_GROUP&&(selected.contains(c.id())||selected.contains(c.panel().parent()))).toList();}
    private static void openCohesion(){var armies=selectedArmies().stream().map(Card::id).toList();if(!armies.isEmpty())MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,"COHESION_ARMIES",armies));}
    static void openPlans(String army){if(current!=null&&current.cards().stream().anyMatch(c->c.id().equals(army)&&(c.role()==Mode.ASSIGN_ARMY||c.role()==Mode.ARMY_GROUP)))select(army,false);}
    private static void deletePlan(){if(!MapCommanderAvailability.allow("LAND/DELETE_PLAN"))return;MapCommanderOrders.land("DELETE_PLAN",selectedArmies().stream().map(Card::id).toList());}
    private static String armyControlReason(String type) {
        String reason=MapCommanderAvailability.reason("LAND/"+type);if(!reason.isEmpty())return reason;
        var armies=selectedArmies();var controls=armies.stream().flatMap(c->c.panel().controls().stream()).filter(c->c.id().equals(type)).toList();
        if(armies.isEmpty()||controls.size()!=armies.size()||controls.stream().anyMatch(c->!c.enabled()))return "선택한 군 중 이 명령을 지정할 수 없는 군이 있습니다.";
        return type.equals("INVASION")&&armies.size()!=1?"상륙 계획 초안을 편집할 군 하나를 선택하세요.":"";
    }
    private static void openArmyControl(String type){String reason=armyControlReason(type);if(!reason.isEmpty()){MapCommanderOrders.message(reason);return;}var armies=selectedArmies();MapCommanderOrders.land(type,armies.stream().map(Card::id).toList());}
    private static String source(String id){return "commander/commands/"+id+"/frame_0";}
    private static void label(GuiGraphicsExtractor g,String text,int x,int y,int w,int h,int color){UiText.centeredSingleLine(g,Minecraft.getInstance().font,UiTooltips.officerText(text),x,y,w,h,color);}
    private static void background(GuiGraphicsExtractor g,String id,int x,int y,int w,int h){if(id.equals("air_priorities_tiled_bg")){UiAssets.nineSlice(g,source(id),x,y,w,h,32,new UiUnits(g.guiWidth(),g.guiHeight()).scale()*.5);return;}if(!UiAssets.draw(g,source(id),x,y,w,h))UiMenuStyle.control(g,x,y,w,h,false,false);}
    private static void button(GuiGraphicsExtractor g,List<Hit> out,String text,String icon,int x,int y,int w,int h,String tip,Runnable action){UiAssets.nineSlice(g,source("nv_btn_select"),x,y,w,h,4,new UiUnits(g.guiWidth(),g.guiHeight()).scale()*.5);if(icon.isEmpty())label(g,text,x,y,w,h,UiMenuStyle.TEXT);else UiAssets.draw(g,source(icon),x,y,w,h);out.add(new Hit(x,y,w,h,tip,action,null,icon.equals("nv_btn_close")?"ui.close":icon.equals("nv_btn_delete")?"ui.disband":icon.startsWith("bpt_order_")||icon.equals("exercise_icon")?"ui.battleplan":"ui.click",icon));}
    private static String commanderName(Card card){return card.panel()!=null&&card.panel().commander().isEmpty()?UiCommandLabels.text("UNIT_LEADER_NO_LEADER","§R지휘관 없음§!"):card.leader();}
    private static Open units(Card card){return new Open(card.role()==Mode.ARMY_GROUP?Mode.ARMY_GROUP:current.branch()==Mode.NAVY_HQ?Mode.NAVAL_TASK_FORCES:Mode.ARMY_UNITS,card.id());}
    static boolean closeSelection(){if(selected.isEmpty()&&selectedWings.isEmpty()&&!wingList&&MapUnitHud.selectedKeys().isEmpty())return false;close();return true;}
    private static void close(){selected.clear();selectedForces.clear();specificForces=false;selectedWings.clear();MapUnitHud.clearSelection();wingList=false;postureMenu=false;navalTargetMenu=false;scroll=0;hits=List.of();MapCommanderOrders.selectionChanged();}
    static int actionbarTop(UiUnits units,boolean navy){return MapCommanderLayout.commandTop(units,navy);}
    static void draw(GuiGraphicsExtractor g,int sidebar,int mouseX,int mouseY,int portraitsTop) {
        hits=List.of();if(current==null)return;var u=new UiUnits(g.guiWidth(),g.guiHeight());var out=new ArrayList<Hit>();
        var chosen=current.cards().stream().filter(c->selected.contains(c.id())).toList();
        int bottom=MapCommanderLayout.popupBottom(u,current.branch()==Mode.NAVY_HQ);
        if(current.branch()==Mode.ARMY||!chosen.isEmpty())actionbar(g,u,out,chosen,actionbarTop(u,current.branch()==Mode.NAVY_HQ),bottom);
        if(sidebar==0&&!(Minecraft.getInstance().gui.screen() instanceof dev.hoi.client.screen.ScreenDialog)) {
            if(current.branch()==Mode.ARMY&&fullArmyCards.isEmpty()&&!selectedDivisionRows().isEmpty())armySelection(g,u,out,bottom);
            else if(current.branch()==Mode.AIR||wingList) {
                var keys=MapUnitHud.selectedKeys();
                var wings=current.wings().stream().filter(MapCommanderPanels::wingSelected).sorted(Comparator.comparing(MapCommanderAirBases::identity).thenComparing(Member::id)).toList();
                if(!wings.isEmpty())air(g,u,out,wings);
                else if(!chosen.isEmpty())command(g,u,out,chosen,bottom);
            } else if(!chosen.isEmpty())command(g,u,out,chosen,bottom);
            else if(current.branch()==Mode.ARMY&&!selectedDivisionRows().isEmpty())armySelection(g,u,out,bottom);
        }

        hits=List.copyOf(out);
    }
    private static void command(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> chosen,int bottom) {
        boolean navy=current.branch()==Mode.NAVY_HQ;if(!navy&&chosen.size()>1){armyStack(g,u,out,chosen,bottom);return;}if(navy&&chosen.size()>1){navyStack(g,u,out,chosen,bottom);return;}int x=u.size(3),top=UiMenuBar.height(g.guiWidth())+u.size(2),w=u.size(navy?246.5:218.5),header=u.size(navy?83:55);
        int visible=Math.max(1,(Math.max(top+header,bottom)-top)/header),start=chosen.size()>1?Math.clamp(scroll,0,Math.max(0,chosen.size()-visible)):0;
        int y=top;
        for(var card:chosen.subList(start,Math.min(chosen.size(),start+visible))) {
            if(navy){navyHeader(g,u,out,card,x,y,w,header);y+=header;continue;}
            background(g,navy?"fleet_bg":card.role()==Mode.ARMY_GROUP?"army_group_bg":"army_bg",x,y,w,header);
            label(g,card.name(),x+u.size(20),y+u.size(2),w-u.size(62),u.size(12),UiMenuStyle.TEXT);
            button(g,out,"×","nv_btn_close",x+w-u.size(15),y+u.size(2),u.size(13),u.size(13),"닫기",()->closeCard(card));
            int py=y+u.size(16);String portrait=card.portrait().equals("politics/empty/leader")?"commander/hud/army_unknown/frame_0":card.portrait();
            UiAssets.drawContent(g,portrait,x+u.size(3),py,u.size(31),u.size(36));
            var appointment=new Open(card.role()==Mode.ARMY_GROUP?Mode.ASSIGN_GROUP_MARSHAL:navy?Mode.NAVY_FLEET:Mode.ASSIGN_ARMY,card.id());
            out.add(new Hit(x+u.size(3),py,u.size(31),u.size(36),card.detail(),()->MapCommanderHud.open(appointment)));
            var p=card.panel();String role=p!=null&&p.leader()!=null?p.leader().role():navy?"제독":card.role()==Mode.ARMY_GROUP?"원수":"장군";
            label(g,role,x+u.size(36),py,u.size(64),u.size(9),UiMenuStyle.TEXT);label(g,commanderName(card),x+u.size(36),py+u.size(9),u.size(64),u.size(11),UiMenuStyle.TEXT);
            if(p!=null&&p.leader()!=null) {
                label(g,Integer.toString(p.leader().skill()),x+u.size(101),py,u.size(13),u.size(18),UiMenuStyle.TEXT);
                for(int i=0;i<4;i++) {int bx=x+u.size(115+i*13);String icon="commander/ui/"+List.of("attack","defense",navy?"maneuver":"planning",navy?"coordination":"logistics").get(i);UiAssets.drawContent(g,icon,bx,py,u.size(12),u.size(10));label(g,Integer.toString(p.leader().abilities().get(i)),bx,py+u.size(10),u.size(12),u.size(12),UiMenuStyle.TEXT);String tip=p.leader().abilityTooltips().size()>i?p.leader().abilityTooltips().get(i):card.detail();out.add(new Hit(bx,py,u.size(12),u.size(22),tip,()->{},null,""));}
                int tx=x+u.size(166);for(var trait:p.leader().traitDetails().stream().limit(5).toList()){UiAssets.drawContent(g,trait.icon(),tx,py,u.size(9),u.size(14));out.add(new Hit(tx,py,u.size(9),u.size(14),trait.tooltip(),()->{},null,""));tx+=u.size(9);}
                if(!p.commander().isEmpty())out.add(new Hit(x+u.size(102),py+u.size(23),w-u.size(110),u.size(12),"특성",()->MapCommanderHud.open(new Open(Mode.COMMANDER_TRAITS,(navy?"navy/":"army/")+p.commander()))));
            }
            label(g,MapCommanderHud.countLabel(card),x+u.size(37),y+header-u.size(14),u.size(66),u.size(12),0xFFFFFFFF);
            y+=header;
        }
        if(chosen.size()>1){pager(g,u,out,x,y,w,scroll>0,start+visible<chosen.size(),1);out.add(new Hit(x,top,w,y-top,"",()->{},null,""));return;}
        var card=chosen.getFirst();var panel=card.panel();if(panel==null)return;
        int tools=u.size(navy?22:21);if(navy)navalTools(g,u,out,x,y,w);else {background(g,"air_priorities_tiled_bg",x,y,w,tools);
        button(g,out,navy?"태스크 포스":"소속 사단","",x+u.size(3),y+u.size(2),u.size(28),u.size(17),navy?"태스크 포스 임무 선택":"소속 사단",()->MapCommanderHud.open(units(card)));
        button(g,out,navy?"편입":"전투 계획","",x+u.size(34),y+u.size(2),u.size(42),u.size(17),navy?"태스크 포스 편입":"전투 계획",()->MapCommanderHud.open(new Open(navy?Mode.NAVY_FLEET_MEMBERS:card.role()==Mode.ARMY_GROUP?Mode.ARMY_GROUP_MEMBERS:Mode.ARMY_PLANS,card.id())));
        }
        if(!navy&&card.role()!=Mode.ARMY_GROUP)button(g,out,UiCommandLabels.text("UNITS_VIEW_BTN_DISBAND_GROUP","제거"),"",x+w-u.size(43),y+u.size(2),u.size(40),u.size(17),UiCommandLabels.text("UNITS_VIEW_BTN_DISBAND_GROUP_TOOLTIP","")+"\n\n사단은 미배속으로 보존합니다. 계획·지휘관·부속 비행단 처리와 비용을 확인합니다.",()->MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,"DISSOLVE_ARMY",List.of(card.id()))));
        if(!navy)divisionActions(g,u,out,x,y);
        List<Member> rows=MapCommanderHud.groups.rowsVisible(card.id())?(navy?panel.members():armyRows(card)):List.of();y+=tools;int row=u.size(navy?62.5:16.5);int end=Math.max(y+row,Math.min(g.guiHeight()-u.size(82),bottom));int count=Math.max(1,(end-y-u.size(18))/row);scroll=Math.clamp(scroll,0,Math.max(0,rows.size()-count));
        for(var member:rows.subList(scroll,Math.min(rows.size(),scroll+count))) {member(g,u,out,member,x,y,navy?w:u.size(195),row,navy?"task_force_bg":"uv_unit_entry_bg");y+=row;}
        pager(g,u,out,x,y,w,scroll>0,scroll+count<rows.size(),count);
        if((navy||fullArmyCards.contains(card.id()))&&MapCommanderHud.groups.rowsVisible(card.id())&&panel.total()>panel.members().size())button(g,out,"전체 목록","",x,y+u.size(18),w,u.size(17),"소속 부대 전체 목록",()->MapCommanderHud.open(units(card)));out.add(new Hit(x,top,w,y-top,"",()->{},null,""));
    }
    private record ArmyEntry(Card card,Member member) {}
    private static void armyStack(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> chosen,int bottom){
        var entries=new ArrayList<ArmyEntry>();var shown=new HashSet<String>();for(var card:chosen){entries.add(new ArmyEntry(card,null));if(MapCommanderHud.groups.rowsVisible(card.id()))armyRows(card).stream().filter(m->shown.add(m.id())).forEach(m->entries.add(new ArmyEntry(card,m)));}
        int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),top=y,w=u.size(218.5),row=u.size(16.5);int count=Math.max(1,(bottom-y-u.size(18))/row);scroll=Math.clamp(scroll,0,Math.max(0,entries.size()-count));
        for(var entry:entries.subList(scroll,Math.min(entries.size(),scroll+count))){if(entry.member()!=null)member(g,u,out,entry.member(),x,y,w,row,"uv_unit_entry_bg");else{background(g,"army_bg",x,y,w,row);label(g,entry.card().name(),x+u.size(4),y,w-u.size(24),row,UiMenuStyle.TEXT);button(g,out,"×","nv_btn_close",x+w-u.size(15),y,u.size(13),row,"닫기",()->closeCard(entry.card()));}y+=row;}
        pager(g,u,out,x,y,w,scroll>0,scroll+count<entries.size(),count);out.add(new Hit(x,top,w,y-top,"",()->{},null,""));
    }
    private static void divisionActions(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y){
        if(!selectedDivisions().isEmpty()){
            var unitIds=new HashSet<>(selectedDivisions());var rows=current.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).filter(m->unitIds.contains("unit/"+m.id())).toList();var states=rows.stream().filter(m->m.data()!=null&&m.data().strategic()!=null).map(m->m.data().strategic()).distinct().toList();boolean known=rows.stream().map(Member::id).distinct().count()==unitIds.size()&&rows.stream().allMatch(m->m.data()!=null&&m.data().strategic()!=null);Boolean state=known&&states.size()==1?states.getFirst():null;
            String title=state==null?"전략적 재배치 모드 선택":UiCommandLabels.text(state?"SWITCH_STRATEGIC_REDEPLOYMENT_MODE_OFF":"SWITCH_STRATEGIC_REDEPLOYMENT_MODE_ON","");String tip=title+"\n\n"+UiCommandLabels.text("SWITCH_STRATEGIC_REDEPLOYMENT_MODE_DETAILED","")+"\n"+UiCommandLabels.text("SWITCH_STRATEGIC_REDEPLOYMENT_MODE_DETAILED_2","");
            int sx=x+u.size(80),sy=y+u.size(3.5);UiAssets.draw(g,source("redeployment_type_icon").replace("frame_0",Boolean.TRUE.equals(state)?"frame_1":"frame_0"),sx,sy,u.size(15.5),u.size(15.5));if(state==null)label(g,"—",sx,sy,u.size(15.5),u.size(15.5),UiMenuStyle.TEXT);out.add(new Hit(sx,sy,u.size(15.5),u.size(15.5),tip,()->landSettings("STRATEGIC_UNITS")));
        }
        if(!selectedDivisions().isEmpty())button(g,out,"","hold",x+u.size(165),y+u.size(3.5),u.size(15.5),u.size(15.5),UiCommandLabels.text("HALT_UNIT","선택한 모든 부대 정지"),MapCommanderPanels::haltSelected);
    }
    private static void armySelection(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int bottom){
        var rows=selectedDivisionRows();int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),top=y,w=u.size(218.5),row=u.size(16.5);
        background(g,"army_bg",x,y,w,row);label(g,rows.size()+"개 사단 선택됨",x,y,w-u.size(18),row,UiMenuStyle.TEXT);button(g,out,"×","nv_btn_close",x+w-u.size(15),y,u.size(13),row,"선택 해제",MapCommanderPanels::close);y+=row;
        background(g,"air_priorities_tiled_bg",x,y,w,u.size(21));divisionActions(g,u,out,x,y);y+=u.size(21);
        int count=Math.max(1,(bottom-y-u.size(18))/row);scroll=Math.clamp(scroll,0,Math.max(0,rows.size()-count));for(var member:rows.subList(scroll,Math.min(rows.size(),scroll+count))){member(g,u,out,member,x,y,w,row,"uv_unit_entry_bg");y+=row;}pager(g,u,out,x,y,w,scroll>0,scroll+count<rows.size(),count);out.add(new Hit(x,top,w,y-top,"",()->{},null,""));
    }
    private record FleetEntry(Card card,Member member) {}
    private static void navyStack(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> chosen,int bottom) {
        var entries=new ArrayList<FleetEntry>();for(var card:chosen){entries.add(new FleetEntry(card,null));if(card.panel()!=null&&MapCommanderHud.groups.rowsVisible(card.id()))card.panel().members().forEach(m->entries.add(new FleetEntry(card,m)));}
        int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),w=u.size(246.5),top=y;scroll=Math.clamp(scroll,0,Math.max(0,entries.size()-1));int index=scroll;
        for(;index<entries.size();index++) {
            var entry=entries.get(index);int h=u.size(entry.member()==null?83:62.5);if(entry.member()==null&&index==scroll)h+=u.size(22);if(y+h>bottom-u.size(20))break;
            if(entry.member()==null){navyHeader(g,u,out,entry.card(),x,y,w,u.size(83));if(index==scroll)navalTools(g,u,out,x,y+u.size(83),w);}
            else member(g,u,out,entry.member(),x,y,w,h,"task_force_bg");y+=h;
        }
        pager(g,u,out,x,y,w,scroll>0,index<entries.size(),Math.max(1,index-scroll));out.add(new Hit(x,top,w,Math.max(1,y-top),"",()->{},null,""));
    }
    static void closeCard(Card card){if(!selected.remove(card.id()))return;if(card.panel()!=null)card.panel().members().forEach(member->selectedForces.remove(member.id()));if(current!=null&&current.branch()==Mode.ARMY){fullArmyCards.remove(card.id());retainManualArmySelection();syncArmySelection();}scroll=0;hits=List.of();MapCommanderOrders.selectionChanged();MapCommanderOrders.selectionScopeChanged();}
    private static void navyHeader(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Card card,int x,int y,int w,int h){
        background(g,"fleet_bg",x,y,w,h);label(g,card.name(),x+u.size(35),y+u.size(6),u.size(106),u.size(11.5),UiMenuStyle.TEXT);label(g,Integer.toString(card.count()),x+u.size(159),y+u.size(5),u.size(45),u.size(11.5),UiMenuStyle.TEXT);
        button(g,out,"×","nv_btn_close",x+u.size(227),y+u.size(2.5),u.size(13.5),u.size(13.5),UiCommandLabels.text("CLOSE","닫기"),()->closeCard(card));
        String portrait=card.portrait().equals("politics/empty/leader")?"commander/hud/army_unknown/frame_0":card.portrait();UiAssets.drawContent(g,portrait,x+u.size(4),y+u.size(23.5),u.size(30),u.size(41.5));if(card.panel()!=null&&!card.panel().commander().isEmpty())label(g,"제독",x+u.size(40.5),y+u.size(25),u.size(37.5),u.size(8),UiMenuStyle.TEXT);label(g,commanderName(card),x+u.size(40.5),y+u.size(33),u.size(70),u.size(11.5),UiMenuStyle.TEXT);out.add(new Hit(x+u.size(4),y+u.size(23.5),u.size(30),u.size(41.5),card.panel()!=null&&card.panel().commander().isEmpty()?"배치하려면 클릭하십시오.":card.detail(),()->MapCommanderHud.open(new Open(Mode.NAVY_FLEET,card.id()))));
        var panel=card.panel();if(panel==null)return;
        if(!panel.commander().isEmpty()){UiAssets.draw(g,source("leader_details_icon"),x+u.size(4),y+u.size(55),u.size(12),u.size(12));out.add(new Hit(x+u.size(4),y+u.size(55),u.size(12),u.size(12),"특성",()->MapCommanderHud.open(new Open(Mode.COMMANDER_TRAITS,"navy/"+panel.commander()))));}
        if(panel.leader()!=null){label(g,Integer.toString(panel.leader().skill()),x+u.size(98),y+u.size(25.5),u.size(15),u.size(11.5),UiMenuStyle.TEXT);for(int n=0;n<4;n++){int bx=x+u.size(109+n*9);UiAssets.draw(g,"commander/ui/"+List.of("attack","defense","maneuver","coordination").get(n),bx,y+u.size(23.5),u.size(9),u.size(10));label(g,Integer.toString(panel.leader().abilities().get(n)),bx,y+u.size(34.5),u.size(15),u.size(11.5),UiMenuStyle.TEXT);String tip=panel.leader().abilityTooltips().size()>n?panel.leader().abilityTooltips().get(n):card.detail();out.add(new Hit(bx,y+u.size(23.5),u.size(15),u.size(23),tip,()->{},null,""));}int tx=x+u.size(151);for(var trait:panel.leader().traitDetails().stream().limit(5).toList()){UiAssets.drawContent(g,trait.icon(),tx,y+u.size(30),u.size(9),u.size(14));out.add(new Hit(tx,y+u.size(30),u.size(9),u.size(14),trait.tooltip(),()->{},null,""));tx+=u.size(9);}}
        if(panel.total()==panel.members().size()){var counts=new TreeMap<String,Integer>();panel.members().forEach(m->m.composition().forEach((icon,count)->counts.merge(icon,count,Math::addExact)));int bx=x+u.size(37.5);for(var entry:counts.entrySet()){UiAssets.draw(g,entry.getKey(),bx,y+u.size(48),u.size(18),u.size(9));label(g,entry.getValue().toString(),bx,y+u.size(57),u.size(18),u.size(9),UiMenuStyle.TEXT);out.add(new Hit(bx,y+u.size(47.5),u.size(23),u.size(20),hullTooltip(entry.getKey(),entry.getValue()),()->MapCommanderHud.open(units(card))));bx+=u.size(23);}}
    }
    private static void selectWing(Member member){if(shift()){var keys=MapUnitHud.selectedKeys();current.wings().stream().filter(wing->keys.contains(wing.selectionKey())).map(Member::id).forEach(selectedWings::add);}MapUnitHud.clearSelection();if(!shift())selectedWings.clear();if(!selectedWings.remove(member.id()))selectedWings.add(member.id());wingList=true;scroll=0;hits=List.of();postureMenu=false;MapCommanderOrders.selectionChanged();}
    private static void deselectWing(String id){showWings(current.wings().stream().filter(MapCommanderPanels::wingSelected).filter(member->!member.id().equals(id)).map(Member::id).toList());}
    private static void airMember(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Member member,int x,int y,int w,int h){
        var data=member.data().air();var wing=data.wing();background(g,"av_wing_entry_bg",x,y,w,h);if(selectedWings.contains(member.id()))UiAssets.draw(g,source("av_wing_entry_bg").replace("frame_0","frame_1"),x,y,w,h);
        String detail=member.name()+"\n"+member.status()+"\n\n"+member.detail()+"\n\n"+data.airGroupName()+"\n"+airPolicyTooltip(data.reinforcementPolicy());
        button(g,out,"","unitlist_unitinfo_button",x+u.size(5.5),y+u.size(3.5),u.size(31),u.size(15),detail,()->MapCommanderHud.open(member.request()));if(!UiAssets.drawContent(g,wing.equipmentIcon(),x+u.size(8),y+u.size(6),u.size(26),u.size(12)))UiAssets.drawContent(g,member.icon(),x+u.size(8),y+u.size(6),u.size(26),u.size(12));
        if(wing.carrier())UiAssets.draw(g,source("airwing_carrier_icon"),x+u.size(29),y+u.size(9.5),u.size(9),u.size(8));if(member.strength()>=0)nativeBar(g,"repair_hp",x+u.size(3),y+u.size(3),u.size(2),h-u.size(6),member.strength(),true);
        label(g,Integer.toString(member.data().quantity()),x+u.size(61),y+u.size(1.5),u.size(15),u.size(10),UiMenuStyle.TEXT);label(g,Integer.toString(wing.target()),x+u.size(61),y+u.size(11.5),u.size(15),u.size(10),UiMenuStyle.TEXT);
        String policy=airPolicyTooltip(data.reinforcementPolicy());int frame=List.of("OBSOLETE_ONLY","ANY","ONLY_NEW","ONLY_SAME").indexOf(data.reinforcementPolicy());int px=x+u.size(76.5),py=y+u.size(10.5),ps=u.size(9);UiAssets.draw(g,source("wing_prio_strip").replace("frame_0","frame_"+frame),px,py,ps,ps);out.add(new Hit(px,py,ps,ps,member.name()+"\n"+policy,()->MapCommanderHud.open(new Open(Mode.AIR_REINFORCEMENT,"policy",List.of(member.id()))),null,"ui.click","wing_prio_strip"));
        button(g,out,"","ace_none",x+u.size(91),y+u.size(2),u.size(20),u.size(20),member.detail()+"\n\n"+UiCommandLabels.text("ACE_PILOT_ASSIGN_ACE","에이스 선택"),()->MapCommanderHud.open(new Open(Mode.AIR_REINFORCEMENT,"aces",List.of(member.id()))));
        label(g,member.name(),x+u.size(116.5),y+u.size(3.5),u.size(42),u.size(9),UiMenuStyle.TEXT);label(g,wing.missionName(),x+u.size(116.5),y+u.size(12),u.size(51),u.size(10),0xFFD5CA7D);
        if(!member.data().attachedArmy().isEmpty()){int ax=x+u.size(160),ay=y+u.size(5);UiAssets.draw(g,source("theatre_army_shield"),ax,ay,u.size(13.2),u.size(12.8));out.add(new Hit(ax,ay,u.size(15.5),u.size(15.5),UiCommandLabels.text("ATTACHED_AIR_WING_DELAYED_DESC","부속된 군 선택 · 부속 취소"),()->{},(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT){selectedWings.clear();wingList=false;MapUnitHud.clearSelection();select(member.data().attachedArmy(),false);}else if(button==InputConstants.MOUSE_BUTTON_RIGHT)MapCommanderHud.open(new Open(Mode.AIR_ATTACH,member.id(),List.of("action/detach")));},"ui.click"));}
        for(var mission:AIR)if(mission[0].equals(member.mission())){UiAssets.drawContent(g,"commander/commands/strategicair_mission_types/frame_"+mission[1],x+u.size(176),y+u.size(3),u.size(12.5),u.size(8.5));break;}
        String timeKey="AIRWING_MISSION_"+(wing.sortieTime().equals("BOTH")?"DAY_NIGHT":wing.sortieTime());int tx=x+u.size(241.5),ty=y+u.size(3.5),ts=u.size(14.5);button(g,out,"","strategicair_day_night",tx,ty,ts,ts,member.name()+"\n"+UiCommandLabels.text(timeKey,"")+"\n\n클릭: 다음 출격 시간대",()->MapCommanderOrders.send("AIR_SETTING/SORTIE_NEXT",List.of(member.id(),"sortie/"+wing.sortieTime()),List.of()));UiAssets.drawContent(g,source("strategicair_day_night_icons").replace("frame_0","frame_"+List.of("DAY","NIGHT","BOTH").indexOf(wing.sortieTime())),tx,ty,ts,ts);
        button(g,out,"","nv_btn_close",x+u.size(258),y+u.size(3.5),u.size(14.5),u.size(14.5),"선택 해제 · "+member.name(),()->deselectWing(member.id()));out.add(new Hit(x,y,w,h,detail,()->selectWing(member),null,"ui.division.select"));
    }
    private static void member(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Member member,int x,int y,int w,int h,String asset) {
        if(member.request().mode()==Mode.AIR_MISSION&&member.data()!=null&&member.data().air()!=null&&member.data().air().wing()!=null){airMember(g,u,out,member,x,y,w,h);return;}
        background(g,asset,x,y,w,h);if(member.request().mode()==Mode.NAVAL_MISSION&&selectedForces.contains(member.id()))UiAssets.draw(g,source("task_force_bg_hl"),x,y,w,h);if(member.request().mode()==Mode.ARMY_UNITS&&MapUnitHud.selectedKeys().contains("unit/"+member.id()))UiAssets.draw(g,source(asset).replace("frame_0","frame_1"),x,y,w,h);if(member.request().mode()==Mode.AIR_MISSION&&selectedWings.contains(member.id()))UiAssets.draw(g,source(asset).replace("frame_0","frame_1"),x,y,w,h);UiAssets.drawContent(g,member.icon(),x+u.size(6),y+u.size(2),u.size(24),Math.min(h-u.size(4),u.size(24)));
        if(member.strength()>=0)nativeBar(g,"repair_hp",x+u.size(2),y+u.size(2),u.size(3),h-u.size(4),member.strength(),true);
        boolean air=member.request().mode()==Mode.AIR_MISSION;
        label(g,member.name(),x+u.size(33),y+u.size(1),w-u.size(member.request().mode()==Mode.NAVAL_MISSION?125:48),Math.min(h,u.size(air?9:14)),UiMenuStyle.TEXT);
        if(h>u.size(20))label(g,member.status(),x+u.size(33),y+u.size(air?13:19),w-u.size(48),u.size(air?9:14),0xFFD5CA7D);
        if(member.organization()!=null&&(member.request().mode()==Mode.ARMY_UNITS||member.request().mode()==Mode.NAVAL_MISSION))nativeBar(g,"repair_org",x+u.size(5),y+u.size(2),u.size(3),h-u.size(4),member.organization(),true);
        if(member.organization()!=null&&member.request().mode()!=Mode.ARMY_UNITS&&member.request().mode()!=Mode.NAVAL_MISSION)nativeBar(g,"repair_org",x+u.size(30),y+h-u.size(5),u.size(75),u.size(3),member.organization(),false);
        if(!member.composition().isEmpty()) {int cx=x+u.size(6);for(var entry:new TreeMap<>(member.composition()).entrySet()){UiAssets.draw(g,entry.getKey(),cx,y+h-u.size(21),u.size(18),u.size(9));label(g,entry.getValue().toString(),cx,y+h-u.size(12),u.size(18),u.size(9),UiMenuStyle.TEXT);out.add(new Hit(cx,y+h-u.size(21),u.size(18),u.size(18),hullTooltip(entry.getKey(),entry.getValue()),()->selectForce(member.id()),null,"ui.division.select"));cx+=u.size(23);}}
        if(air&&member.data()!=null&&!member.data().attachedArmy().isEmpty()){
            int ax=x+w-u.size(17),ay=y+u.size(3);UiAssets.draw(g,source("theatre_army_shield"),ax,ay,u.size(13.2),u.size(12.8));out.add(new Hit(ax,ay,u.size(15.5),u.size(15.5),UiCommandLabels.text("ATTACHED_AIR_WING_DELAYED_DESC","부속된 군 선택 · 부속 취소"),()->{},(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT){selectedWings.clear();wingList=false;MapUnitHud.clearSelection();select(member.data().attachedArmy(),false);}else if(button==InputConstants.MOUSE_BUTTON_RIGHT)MapCommanderHud.open(new Open(Mode.AIR_ATTACH,member.id(),List.of("action/detach")));},"ui.click"));
        }
        if(member.request().mode()==Mode.NAVAL_MISSION){
            var state=member.data()==null?null:member.data().navy();String[] icons={"nv_btn_repair_priority","nv_btn_toggle_split_for_repair","nv_btn_aggressiveness_small","nv_btn_composition_editor","nv_btn_reinforce"},keys={"NAVY_REPAIR_POLICY_DESC","NAVY_REPAIR_POLICY_SPLIT_DESC","SHIP_ENGAGEMENT_BUTTON_AGGRESSIVENESS_GENERAL_DESC","TASK_FORCE_COMPOSITION_EDITOR_DESC","TASK_FORCE_AUTO_REINFORCEMENT_DESC"},tabs={"repair","","engagement","composition",""};
            for(int n=0;n<icons.length;n++){String tab=tabs[n];int frame=state==null?0:n==0&&state.repairPolicy()!=null?state.repairPolicy():n==2?List.of("NEVER","LOW","MEDIUM","HIGH","ALWAYS").indexOf(state.engagement()):0;String icon=n==0&&(state==null||state.repairPolicy()==null)?source("nv_btn_repair_symbol"):source(icons[n]).replace("frame_0","frame_"+frame);int bx=x+w-u.size(17+n*17);UiAssets.draw(g,icon,bx,y+u.size(2),u.size(15.5),u.size(15.5));String tip=UiCommandLabels.text(keys[n],List.of("수리 우선순위","자동 분리 수리","교전 규칙","태스크 포스 편성","자동 증원").get(n));if(state!=null){if(n==0)tip=state.repairPolicy()==null?"수리 우선순위 미설정\n\n"+tip:UiCommandLabels.text("NAVY_REPAIR_POLICY_"+state.repairPolicy(),tip);if(n==2)tip=UiCommandLabels.text("SHIP_ENGAGEMENT_BUTTON_AGGRESSIVENESS_"+(frame+1),state.engagement())+"\n\n"+UiCommandLabels.text("SHIP_ENGAGEMENT_BUTTON_AGGRESSIVENESS_"+(frame+1)+"_DESC","")+"\n\n"+tip;if(n==1||n==4){Boolean enabled=n==1?state.autoSplit():state.reinforce();tip+=(enabled==null?"\n\n미설정":enabled?"\n\n활성화":"\n\n비활성화");}}if(n==0){Integer policy=state==null?null:state.repairPolicy();if(policy!=null){String next=UiCommandLabels.text("NAVY_REPAIR_POLICY_"+((policy+1)%4),"").split("\n",2)[0],previous=UiCommandLabels.text("NAVY_REPAIR_POLICY_"+((policy+3)%4),"").split("\n",2)[0];tip+="\n"+UiCommandLabels.text("NAVY_REPAIR_POLICY_CLICK_TO_CHANGE_TO","").replace("$LEVEL|$",next)+"\n"+UiCommandLabels.text("NAVY_REPAIR_POLICY_RIGHT_CLICK_TO_CHANGE_TO","").replace("$LEVEL|$",previous);}else tip+="\n\n수리 우선순위를 먼저 선택하세요.";out.add(new Hit(bx,y+u.size(2),u.size(15.5),u.size(15.5),tip,()->MapCommanderOrders.repairPolicy(member.id(),policy,false),(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT||button==InputConstants.MOUSE_BUTTON_RIGHT)MapCommanderOrders.repairPolicy(member.id(),policy,button==InputConstants.MOUSE_BUTTON_RIGHT);},"ui.click"));}else if(n==1||n==2||n==4){int setting=n;if(state==null)tip+="\n\n태스크 포스 설정을 불러오는 중입니다.";if(n==2)tip+="\n\n클릭: 다음 교전 규칙\n우클릭: 이전 교전 규칙";out.add(new Hit(bx,y+u.size(2),u.size(15.5),u.size(15.5),tip,()->navalSetting(member,setting,false),(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT||setting==2&&button==InputConstants.MOUSE_BUTTON_RIGHT)navalSetting(member,setting,button==InputConstants.MOUSE_BUTTON_RIGHT);},"ui.click"));}else out.add(new Hit(bx,y+u.size(2),u.size(15.5),u.size(15.5),tip,()->MapCommanderHud.open(new Open(Mode.NAVAL_SETTINGS,member.id(),tab.isEmpty()?List.of():List.of(tab)))));}
            out.add(new Hit(x+u.size(6),y+u.size(2),u.size(24),u.size(24),"태스크 포스 표식 변경",()->MapCommanderHud.open(new Open(Mode.NAVAL_SETTINGS,member.id(),List.of("insignia")))));
        }
        if(member.request().mode()==Mode.ARMY_UNITS){
            out.add(new Hit(x,y,w,h,member.name()+"\n"+member.status()+"\n\n"+member.detail()+"\n\n§G클릭§!하여 선택\n§GCtrl+클릭§!하여 선택 전환\n§G더블클릭§!하여 같은 편제 선택\n§G우클릭§!하여 사단 위치로 이동",()->{},(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_RIGHT){dev.hoi.client.audio.AudioUiSounds.play("ui.click");MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,"FOCUS_UNIT",List.of("unit/"+member.id())));return;}if(button!=InputConstants.MOUSE_BUTTON_LEFT)return;dev.hoi.client.audio.AudioUiSounds.play("ui.division.select");var matching=twice&&member.data()!=null?current.cards().stream().filter(c->c.panel()!=null&&selected.contains(c.id())).flatMap(c->c.panel().members().stream()).filter(m->m.data()!=null&&m.request().mode()==Mode.ARMY_UNITS&&m.data().template().equals(member.data().template())).map(Member::id).distinct().toList():List.of(member.id());MapUnitHud.selectDivisions(matching,control());}));return;
        }
        String airDetail=air&&member.data()!=null&&member.data().air()!=null?"\n\n"+member.data().air().airGroupName()+"\n"+airPolicyTooltip(member.data().air().reinforcementPolicy()):"";
        out.add(new Hit(x,y,w,h,member.name()+"\n"+member.status()+"\n\n"+member.detail()+airDetail,()->{
            if(member.request().mode()==Mode.NAVAL_MISSION)selectForce(member.id());
            else if(member.request().mode()==Mode.AIR_MISSION)selectWing(member);
            else MapCommanderHud.open(member.request());
        },null,"ui.division.select"));
    }
    private static void nativeBar(GuiGraphicsExtractor g,String asset,int x,int y,int w,int h,double ratio,boolean vertical){
        nativeBarImage(g,"repair_stat_bg",x,y,w,h,vertical);int fill=(int)Math.round((vertical?h:w)*Math.clamp(ratio,0,1));if(fill<=0)return;
        g.enableScissor(x,vertical?y+h-fill:y,vertical?x+w:x+fill,y+h);nativeBarImage(g,asset,x,y,w,h,vertical);g.disableScissor();
    }
    private static void nativeBarImage(GuiGraphicsExtractor g,String asset,int x,int y,int w,int h,boolean vertical){
        g.pose().pushMatrix();g.pose().translate(x,vertical?y+h:y);if(vertical)g.pose().rotate(-(float)Math.PI/2);g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/picker/"+asset+".png"),0,0,vertical?h:w,vertical?w:h,0,1,0,1);g.pose().popMatrix();
    }
    private static void pager(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y,int w,boolean previous,boolean next,int step){if(!previous&&!next)return;if(previous)button(g,out,"‹","",x,y,u.size(20),u.size(17),"이전",()->scroll=Math.max(0,scroll-step));if(next)button(g,out,"›","",x+w-u.size(20),y,u.size(20),u.size(17),"다음",()->scroll+=step);}
    private static void commandIcon(GuiGraphicsExtractor g,UiUnits u,String icon,int x,int y,int w,int h){int inset=Math.max(1,u.size(1));UiAssets.drawContent(g,icon,x+inset,y+inset,Math.max(1,w-2*inset),Math.max(1,h-2*inset));}
    private static void armyButton(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,String icon,int x,int y,int slot,String tip,Runnable action){
        String command=switch(icon){case "bpt_order_unassign_units_btn"->"ARMY/UNASSIGN_UNITS";case "bpt_order_assign_units_btn"->"LAND/ASSIGN_UNITS";case "bpt_order_reshape_btn"->"LAND/EDIT_PLAN";case "exercise_icon"->"ARMY/"+(selectedArmies().stream().flatMap(c->c.panel().controls().stream()).anyMatch(c->c.id().equals("STOP_EXERCISE"))?"STOP_EXERCISE":shift()?"EXERCISE_UNTIL_TRAINED":"EXERCISE");default->"";};
        var cell=MapCommanderLayout.armyCell(u,x,y,slot);commandIcon(g,u,source(icon),cell.x(),cell.y(),cell.width(),cell.height());if(!MapCommanderAvailability.reason(command).isEmpty())g.fill(cell.x(),cell.y(),cell.x()+cell.width(),cell.y()+cell.height(),0x88000000);
        out.add(new Hit(cell.x(),cell.y(),cell.width(),cell.height(),MapCommanderAvailability.tooltip(command,tip),()->{if(MapCommanderAvailability.allow(command))action.run();},null,"ui.battleplan"));
    }
    private static void actionbar(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> cards,int y,int popupBottom) {
        boolean navy=current.branch()==Mode.NAVY_HQ;int w=u.size(navy?266:320.5),h=u.size(navy?50:39.5),x=(g.guiWidth()-w)/2;
        background(g,navy?"air_priorities_tiled_bg":"bpt_bg",x,y,w,h);
        label(g,cards.isEmpty()?"군을 선택하세요":navy?navalCaption(cards):cards.size()==1?cards.getFirst().name():cards.size()+" 선택됨",x+u.size(7),y+u.size(3),w-u.size(30),u.size(11),UiMenuStyle.TEXT);
        if(navy) {
            var ids=cards.stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).map(Member::id).filter(id->!specificForces||selectedForces.contains(id)).distinct().toList();
            branchButtons(g,u,out,true,ids,x,y);int rx=x+u.size(248),ry=y+u.size(27);UiAssets.draw(g,source("nv_btn_remove_regions"),rx,ry,u.size(13.5),u.size(13.5));out.add(new Hit(rx,ry,u.size(13.5),u.size(13.5),UiCommandLabels.text("NAVAL_BTN_REMOVE_REGIONS", "배정 해역 제거"),()->{},(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT||button==InputConstants.MOUSE_BUTTON_RIGHT){dev.hoi.client.audio.AudioUiSounds.play("ui.click");navalAreas(button==InputConstants.MOUSE_BUTTON_RIGHT);}}));forceBand(g,u,out,cards,x,y+h+u.size(4),w);navalTargets(g,u,out,x,y,popupBottom);
        } else {
            armyAbilities(g,u,out,x,y,w);areaDefense(g,u,out,x,y,w,popupBottom);var controls=current.cards().stream().filter(c->c.panel()!=null&&(cards.stream().anyMatch(selected->selected.id().equals(c.id())||selected.id().equals(c.panel().parent())))).flatMap(c->c.panel().controls().stream()).toList();
            int bx=x+u.size(8);for(String type:List.of("INVASION","FRONT","OFFENSIVE","SPEARHEAD","FALLBACK","AREA_DEFENSE")) {
                var matching=controls.stream().filter(c->c.id().equals(type)).toList();
                if(matching.isEmpty()){
                    String icon=Map.of("INVASION","bpt_order_draw_naval_inv_btn","FRONT","bpt_order_draw_defence_btn","OFFENSIVE","bpt_order_draw_offence_btn","SPEARHEAD","bpt_order_draw_blitz_btn","FALLBACK","bpt_order_draw_fallback_btn","AREA_DEFENSE","bpt_order_area_defense_btn").get(type);
                    String key=Map.of("INVASION","UNITS_VIEW_BTN_DRAW_NAVAL_INV","FRONT","UNITS_VIEW_BTN_DRAW_DEFENCE","OFFENSIVE","UNITS_VIEW_BTN_DRAW_OFFENCE","SPEARHEAD","UNITS_VIEW_BTN_DRAW_BLITZ","FALLBACK","UNITS_VIEW_BTN_DRAW_FALLBACK","AREA_DEFENSE","UNITS_VIEW_BTN_AREA_DEFENSE").get(type);
                    var cell=MapCommanderLayout.armyCell(u,x,y,Map.of("INVASION",4,"FRONT",6,"OFFENSIVE",7,"SPEARHEAD",8,"FALLBACK",9,"AREA_DEFENSE",10).get(type));commandIcon(g,u,source(icon),cell.x(),cell.y(),cell.width(),cell.height());g.fill(cell.x(),cell.y(),cell.x()+cell.width(),cell.y()+cell.height(),0x66000000);out.add(new Hit(cell.x(),cell.y(),cell.width(),cell.height(),UiCommandLabels.text(key,type)+"\n\n명령을 지정할 군을 선택하세요.",()->{}));continue;
                }
                var first=matching.getFirst();
                var cell=MapCommanderLayout.armyCell(u,x,y,Map.of("INVASION",4,"FRONT",6,"OFFENSIVE",7,"SPEARHEAD",8,"FALLBACK",9,"AREA_DEFENSE",10).get(type));commandIcon(g,u,first.icon(),cell.x(),cell.y(),cell.width(),cell.height());
                String reason=armyControlReason(type);if(!reason.isEmpty())g.fill(cell.x(),cell.y(),cell.x()+cell.width(),cell.y()+cell.height(),0x88000000);
                out.add(new Hit(cell.x(),cell.y(),cell.width(),cell.height(),MapCommanderAvailability.explain(UiCommandLabels.withoutShortcuts(type.equals("FRONT")&&cards.stream().anyMatch(c->c.role()==Mode.ARMY_GROUP)?UiCommandLabels.text("UNITS_VIEW_BTN_DRAW_DEFENCE_ARMY_GROUP",first.tooltip()):first.tooltip()),reason),()->{openArmyControl(type);},null,"ui.battleplan"));bx+=u.size(21);
            }
            armyButton(g,u,out,"bpt_order_unassign_units_btn",x,y,12,UiCommandLabels.text("UNITS_VIEW_BTN_UNITS_UNASSIGN",""),()->landSettings("UNASSIGN_UNITS"));
            armyButton(g,u,out,"bpt_order_assign_units_btn",x,y,11,UiCommandLabels.text("UNITS_VIEW_BTN_UNITS_ASSIGN", "사단 배정 모드"),MapCommanderOrders::assignSelected);
            var armies=current.cards().stream().filter(c->c.panel()!=null&&c.role()!=Mode.ARMY_GROUP&&cards.stream().anyMatch(chosen->chosen.id().equals(c.id())||chosen.id().equals(c.panel().parent()))).map(Card::id).distinct().toList();
            boolean training=controls.stream().anyMatch(c->c.id().equals("STOP_EXERCISE"));
            armyButton(g,u,out,"exercise_icon",x,y,0,training?UiCommandLabels.text("UNITS_VIEW_BTN_STOP_TRAINING_TOOLTIP","훈련 중지"):controls.stream().filter(c->c.id().equals("EXERCISE")).map(Control::tooltip).findFirst().orElse(UiCommandLabels.text("UNITS_VIEW_BTN_START_TRAINING","훈련")),()->{if(!armies.isEmpty())MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,training?"STOP_EXERCISE":shift()?"EXERCISE_UNTIL_TRAINED":"EXERCISE",armies));});
            var cohesion=controls.stream().filter(c->c.id().equals("COHESION_IMMOBILE")||c.id().equals("COHESION_CURRENT")).toList();if(!cohesion.isEmpty())armyButton(g,u,out,"bpt_cohesion_immobile",x,y,2,UiCommandLabels.text("PLAN_COHESION_MODE","")+"\n\n"+cohesion.getFirst().tooltip(),MapCommanderPanels::openCohesion);
            armyButton(g,u,out,"bpt_execute_mid",x,y,1,UiCommandLabels.text("PLAN_EXECUTE_MODE","전투 계획 실행 모드"),()->postureMenu=!postureMenu);
            if(postureMenu){String[] actions={"CAREFUL","BALANCED","AGGRESSIVE"},icons={"slow","mid","fast"},keys={"CAREFUL","BALANCED","RUSH"};for(int n=0;n<3;n++){String action=actions[n];String key="PLAN_EXECUTE_"+keys[n];button(g,out,"","bpt_execute_"+icons[n],x+u.size(3.5+n*20),popupBottom-u.size(20),u.size(17.5),u.size(17),MapCommanderAvailability.tooltip("ARMY/"+action,UiCommandLabels.text(key+"_TITLE",action)+"\n\n"+UiCommandLabels.text(key+"_DESC","")),()->{if(!MapCommanderAvailability.allow("ARMY/"+action))return;postureMenu=false;if(!armies.isEmpty())MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,action,armies));});}}
            armyButton(g,u,out,"bpt_order_reshape_btn",x,y,13,UiCommandLabels.text("UNITS_VIEW_BTN_RESHAPE", "편집 모드"),()->MapCommanderOrders.land("EDIT_PLAN",armies));
            String deleteReason=MapCommanderAvailability.reason("LAND/DELETE_PLAN"),removeReason=MapCommanderAvailability.reason("ARMY/REMOVE");String deleteTip=UiCommandLabels.text("UNITS_VIEW_BTN_DELETE_ORDER","명령 삭제");if(!deleteReason.isEmpty())deleteTip=MapCommanderAvailability.explain(deleteTip,"클릭: "+deleteReason);if(!removeReason.isEmpty())deleteTip=MapCommanderAvailability.explain(deleteTip,"우클릭: "+removeReason);var deleteCell=MapCommanderLayout.armyCell(u,x,y,14);commandIcon(g,u,source("bpt_order_delete_btn"),deleteCell.x(),deleteCell.y(),deleteCell.width(),deleteCell.height());out.add(new Hit(deleteCell.x(),deleteCell.y(),deleteCell.width(),deleteCell.height(),deleteTip,()->{},(button,twice)->{if(armies.isEmpty())return;if(button==InputConstants.MOUSE_BUTTON_RIGHT){if(!MapCommanderAvailability.allow("ARMY/REMOVE",armies))return;dev.hoi.client.audio.AudioUiSounds.play("ui.battleplan");MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,"REMOVE",armies));}else if(button==InputConstants.MOUSE_BUTTON_LEFT){dev.hoi.client.audio.AudioUiSounds.play("ui.battleplan");deletePlan();}}));
        }
        String branchReason=navy?"":MapCommanderAvailability.branchReason("ARMY/");
        if(!branchReason.isEmpty()||cards.isEmpty()&&selectedDivisions().isEmpty()){
            String reason=branchReason.isEmpty()?"군을 선택하세요.":branchReason;
            for(int i=0;i<out.size();i++){var hit=out.get(i);g.fill(hit.x(),hit.y(),hit.x()+hit.w(),hit.y()+hit.h(),0x44000000);out.set(i,new Hit(hit.x(),hit.y(),hit.w(),hit.h(),branchReason.isEmpty()?hit.tip()+"\n\n군을 선택하세요.":hit.tip().contains(reason)?hit.tip():MapCommanderAvailability.explain(hit.tip(),reason),()->{},null,"",hit.icon()));}
        }
        if(!cards.isEmpty()||!selectedDivisions().isEmpty())button(g,out,"×","nv_btn_close",x+w-u.size(19),y+u.size(2),u.size(15),u.size(14),UiCommandLabels.text("CLOSE","닫기"),MapCommanderPanels::close);
    }
    private static void areaDefense(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y,int w,int popupBottom){
        var armies=selectedArmies();if(armies.size()!=1)return;var controls=armies.getFirst().panel().controls();var area=controls.stream().filter(c->c.id().equals("AREA_STATES")).findFirst();if(area.isEmpty())return;
        int width=u.size(179),height=u.size(39),left=Math.clamp(x+w-width,0,Math.max(0,g.guiWidth()-width)),top=Math.max(UiMenuBar.height(g.guiWidth())+u.size(2),popupBottom-height-u.size(2));background(g,"air_priorities_tiled_bg",left,top,width,height);
        var command=area.get();String caption=command.tooltip().split("\n",2)[0];button(g,out,caption,"",left+u.size(3),top+u.size(2),width-u.size(6),u.size(14),command.tooltip(),()->MapCommanderHud.open(command.request()));
        int column=0;for(var control:controls)if(control.id().startsWith("AREA_OPTION/")){int bx=left+u.size(4+column++*24);int by=top+u.size(18);boolean on=control.id().endsWith("/ON");if(on)g.fill(bx,by,bx+u.size(21),by+u.size(18),0x9962943F);UiAssets.draw(g,control.icon(),bx+u.size(2),by+u.size(1),u.size(17),u.size(16));out.add(new Hit(bx,by,u.size(21),u.size(18),control.tooltip(),()->MapCommanderHud.open(control.request())));}
        out.add(new Hit(left,top,width,height,"",()->{},null,""));
    }
    static String navalCaption(List<Card> cards){
        var all=cards.stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).toList();var chosen=all.stream().filter(m->!specificForces||selectedForces.contains(m.id())).toList();
        if(chosen.size()==1)return UiCommandLabels.text("ONE_TASKFORCE_SELECTED","임무 - $TASKFORCE$").replace("$TASKFORCE$",chosen.getFirst().name());
        return chosen.size()==all.size()?UiCommandLabels.text("ALL_TASKFORCES_SELECTED","임무 - 모든 태스크 포스"):UiCommandLabels.text("MULTIPLE_TASKFORCES_SELECTED","임무 - 다수 태스크 포스");
    }
    private static void navalSetting(Member member,int setting,boolean previous){
        var state=member.data()==null?null:member.data().navy();if(state==null){MapCommanderOrders.message("태스크 포스 설정을 불러오는 중입니다.");return;}
        String action,expected;if(setting==2){action=previous?"ENGAGEMENT_PREVIOUS":"ENGAGEMENT_NEXT";expected="engagement/"+state.engagement();}
        else {action=setting==1?"AUTO_SPLIT":"REINFORCE";Boolean value=setting==1?state.autoSplit():state.reinforce();expected="state/"+(value==null?"unset":value);}
        MapCommanderOrders.send("NAVAL_SETTING/"+action,List.of(member.id(),expected),List.of());
    }
    private static List<Member> navalTargetCandidates(){var ids=new HashSet<>(navalSelection());return current.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).filter(m->ids.contains(m.id())).toList();}
    private static final String[][] NAVAL_TARGETS={{"SUBMARINE","submarine","잠수함"},{"DESTROYER","destroyer","초계함"},{"LIGHT_CRUISER","light_cruiser","호위함"},{"HEAVY_CRUISER","heavy_cruiser","구축함"},{"CONVOY","convoy","수송선"},{"BATTLE_CRUISER","battle_cruiser","순양전함"},{"BATTLESHIP","battleship","미사일 순양함"},{"SH_BATTLESHIP","sh_battleship","순양전함"},{"CARRIER","carrier","항공모함"}};
    private static boolean targetExcluded(Member force,String type){return force.data()!=null&&force.data().navy()!=null&&force.data().navy().resolvedExcludedTargets().contains(type);}
    private static void navalTarget(List<Member> forces,String type){
        if(forces.stream().anyMatch(force->force.data()==null||force.data().navy()==null)){MapCommanderOrders.message("태스크 포스 설정을 불러오는 중입니다.");return;}
        var selection=new ArrayList<String>();for(int i=0;i<forces.size();i++){var force=forces.get(i);selection.add(force.id());selection.add("excluded/"+i+"/"+targetExcluded(force,type));}MapCommanderOrders.send("NAVAL_SETTING/TARGET_BATCH/"+type,selection,List.of());
    }
    private static String navalTargetTooltip(List<Member> forces,String[] type){if(forces.stream().anyMatch(force->force.data()==null||force.data().navy()==null))return type[2]+"\n\n태스크 포스 설정을 불러오는 중입니다.";return String.join("\n",forces.stream().map(force->UiCommandLabels.text(targetExcluded(force,type[0])?"NAVAL_MISSION_TARGET_IS_SELECTED":"NAVAL_MISSION_TARGET_IS_DESELECTED","").replace("$TYPE$",type[2])).distinct().toList());}
    private static void navalTargets(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y,int popupBottom){
        var candidates=navalTargetCandidates();int bx=x+u.size(217),by=y+u.size(26),button=u.size(15.5);UiAssets.draw(g,source("air_target_icon"),bx,by,button,button);String title=UiCommandLabels.text(candidates.isEmpty()?"NAVAL_BTN_SET_TARGET_NO_TASKFORCE":"NAVAL_BTN_SET_TARGET","");String tip=title+"\n\n"+UiCommandLabels.text("NAVAL_BTN_SET_TARGET_DELAYED_DESCRIPTION","");
        out.add(0,new Hit(bx,by,button,button,tip,()->{if(!candidates.isEmpty())navalTargetMenu=!navalTargetMenu;}));
        if(!navalTargetMenu)return;if(candidates.isEmpty()){navalTargetMenu=false;return;}
        int width=u.size(293),height=u.size(38),left=Math.clamp(x-u.size(14.5),0,Math.max(0,g.guiWidth()-width)),top=Math.max(u.size(4),popupBottom-height);UiAssets.nineSlice(g,source("tiled_window_thin_border2"),left,top,width,height,64,u.scale()*.5);
        int[] iconX={38,152,266,370,494,18,132,246,370},iconY={13,13,13,13,13,40,42,42,42},iconW={61,46,53,60,52,90,66,96,79},iconH={18,18,18,18,17,20,18,19,19};
        for(int n=0;n<NAVAL_TARGETS.length;n++){var type=NAVAL_TARGETS[n];int column=n<5?n:n-5,cx=left+u.size(4+column*57),cy=top+u.size(n<5?4:18.5),cw=u.size(57),ch=u.size(16);UiAssets.draw(g,source("naval_target_btn").replace("frame_0",candidates.stream().allMatch(force->targetExcluded(force,type[0]))?"frame_0":"frame_1"),cx,cy,cw,ch);UiAssets.draw(g,source("navalcombat_ship_icon_"+type[1]),left+u.size(iconX[n]/2.0),top+u.size(iconY[n]/2.0),u.size(iconW[n]/2.0),u.size(iconH[n]/2.0));if(candidates.stream().anyMatch(force->force.data()==null||force.data().navy()==null))g.fill(cx,cy,cx+cw,cy+ch,0x99000000);out.add(new Hit(cx,cy,cw,ch,navalTargetTooltip(candidates,type),()->navalTarget(candidates,type[0])));}
        button(g,out,"×","nv_btn_close",left+width-u.size(17),top+u.size(21),u.size(13),u.size(13),UiCommandLabels.text("CLOSE",""),()->navalTargetMenu=false);
    }
    private static void selectForce(String id){specificForces=true;MapUnitHud.clearSelection();if(!shift())selectedForces.clear();if(!selectedForces.remove(id))selectedForces.add(id);MapCommanderOrders.selectionChanged();}
    private static String hullTooltip(String icon,int count){String hull=icon.substring(icon.lastIndexOf('/')+1).replace("navy_","");String name=UiCommandLabels.text("HOI_HULL_"+hull,switch(hull){case "carrier"->"항공모함";case "heavy_cruiser"->"구축함";case "battlecruiser"->"순양전함";case "destroyer"->"초계함";case "light_cruiser"->"호위함";case "battleship"->"미사일 순양함";case "submarine"->"잠수함";default->hull;});return "§Y"+name+"§! 유형의 함정 §Y"+count+"§!척";}
    private static void forceBand(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> cards,int x,int y,int w){
        var forces=cards.stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).toList();int bandWidth=u.size(150.5),left=x+(w-bandWidth)/2,perPage=7;forcePage=Math.clamp(forcePage,0,Math.max(0,(forces.size()-1)/perPage));background(g,"theatre_naval_control_panel",left,y,bandWidth,u.size(21.5));int bx=left+u.size(2.5);
        for(var force:forces.subList(Math.min(forces.size(),forcePage*perPage),Math.min(forces.size(),(forcePage+1)*perPage))){int cell=bx;UiAssets.draw(g,source("nv_btn_base"),cell,y+u.size(1.5),u.size(15.5),u.size(15.5));UiAssets.draw(g,source("theatre_army_shield").replace("frame_0","frame_"+(force.data()==null||force.data().insignia()==null?0:force.data().insignia())),cell+u.size(1.5),y+u.size(3),u.size(11.5),u.size(11.5));if(selectedForces.contains(force.id()))UiAssets.draw(g,source("theatre_army_shield_selected"),cell+u.size(1.5),y+u.size(3),u.size(11.5),u.size(11.5));label(g,Integer.toString(force.composition().values().stream().mapToInt(Integer::intValue).sum()),cell-u.size(2),y+u.size(8.5),u.size(15),u.size(8),UiMenuStyle.TEXT);String tip=force.name()+"\n"+force.status()+"\n\n"+force.detail();for(var entry:new TreeMap<>(force.composition()).entrySet())tip+="\n"+hullTooltip(entry.getKey(),entry.getValue());out.add(new Hit(cell,y,u.size(15.5),u.size(21.5),tip,()->selectForce(force.id()),null,"ui.division.select"));bx+=u.size(19.5);}
        if(forcePage>0)button(g,out,"‹","",left-u.size(15),y,u.size(14),u.size(21.5),"이전",()->forcePage--);if((forcePage+1)*perPage<forces.size())button(g,out,"›","",left+bandWidth+u.size(1),y,u.size(14),u.size(21.5),"다음",()->forcePage++);
    }
    private static final String[][] NAVAL={{"PATROL","patrol","PATROL"},{"STRIKE_FORCE","strike_force","STRIKE_FORCE"},{"CONVOY_RAIDING","convoy_raid","CONVOY_RAIDING"},{"CONVOY_ESCORT","convoy_escort","CONVOY_ESCORT"},{"MINELAYING","mines_planting","MINES_PLANTING"},{"MINESWEEPING","mines_sweeping","MINES_SWEEPING"},{"NAVAL_INVASION_SUPPORT","naval_invasion_support","NAVAL_INVASION_SUPPORT"},{"HOLD","hold","HOLD"},{"EXERCISE","training","TRAINING"}};
    private static final String[][] AIR={{"EXERCISE","11","TRAINING"},{"SUPERIORITY","0","AIR_SUPERIORITY"},{"CLOSE_AIR_SUPPORT","1","CAS"},{"INTERCEPTION","2","INTERCEPTION"},{"STRATEGIC_BOMBING","3","STRATEGIC_BOMBER"},{"NAVAL_STRIKE","4","NAVAL_BOMBER"},{"PORT_STRIKE","8","PORT_STRIKE"},{"LOGISTICS_STRIKE","9","ATTACK_LOGISTICS"},{"AIR_SUPPLY","10","AIR_SUPPLY"},{"RECON","14","RECON"},{"NAVAL_PATROL","15","NAVAL_PATROL"},{"MINELAYING","12","NAVAL_MINE_PLANTING"},{"MINESWEEPING","13","NAVAL_MINE_SWEEPING"}};
    private static void branchButtons(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,boolean navy,List<String> ids,int x,int y) {
        var selectedIds=new HashSet<>(ids);var currentMembers=navy?current.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).filter(m->selectedIds.contains(m.id())).toList():current.wings().stream().filter(m->selectedIds.contains(m.id())).toList();
        int index=0;for(var mission:navy?NAVAL:AIR) {
            int offset=navy?Map.of("PATROL",10,"STRIKE_FORCE",60,"CONVOY_RAIDING",110,"EXERCISE",185,"CONVOY_ESCORT",235,"MINELAYING",285,"MINESWEEPING",335,"NAVAL_INVASION_SUPPORT",385,"HOLD",466).get(mission[0]):0;int bx=x+u.size(navy?offset/2.0:index++*23.5),by=y+(navy?u.size(mission[0].equals("HOLD")?27:24):0),w=navy?u.size(mission[0].equals("HOLD")?13.5:24):Math.max(1,u.size(index*23.5)-u.size((index-1)*23.5)-1),h=u.size(navy?(mission[0].equals("HOLD")?13.5:20):17);String key=(navy?"NAVAL_MISSION_NAME_":"AIRWING_MISSION_TYPE_")+mission[2];String name=UiCommandLabels.text(key,mission[0]);if(!navy&&mission[0].equals("HOLD"))name=UiCommandLabels.text("AIRWING_MISSION_NONE","임무 없음");
            String detail=UiCommandLabels.text(key+"_DESC","");if(navy&&mission[0].equals("STRIKE_FORCE"))detail=current.cards().stream().filter(c->selected.contains(c.id())&&c.panel()!=null).flatMap(c->c.panel().controls().stream()).filter(c->c.id().equals("STRIKE_FORCE")).map(Control::tooltip).findFirst().orElse(detail);if(mission[0].equals("EXERCISE"))detail+=navy?"":"\n\n"+UiCommandLabels.text("AIRWING_MISSION_TYPE_TRAINING_DESC_SHIFT_CLICK","");
            boolean active=!currentMembers.isEmpty()&&currentMembers.stream().allMatch(m->m.mission().equals(mission[0]));
            String icon=navy?source("nv_btn_"+mission[1]).replace("frame_0",active?"frame_1":"frame_0"):mission[0].equals("HOLD")?source("uv_btn_unassign"):"commander/commands/strategicair_mission_types/frame_"+mission[1];
            if(!navy)UiAssets.draw(g,source(Set.of("CLOSE_AIR_SUPPORT","STRATEGIC_BOMBING","NAVAL_STRIKE","PORT_STRIKE","LOGISTICS_STRIKE","AIR_SUPPLY","RECON","NAVAL_PATROL","MINELAYING","MINESWEEPING").contains(mission[0])?"strategicair_mission_btn_checkbox_bg":"strategicair_mission_btn_bg").replace("frame_0",active?"frame_0":"frame_1"),bx,by,w,h);if(navy)UiAssets.draw(g,icon,bx,by,w,h);else commandIcon(g,u,icon,bx,by,w,h);
            String chosen=navy&&mission[0].equals("HOLD")&&control()?"HOLD_IN_PLACE":mission[0].equals("EXERCISE")&&shift()?"EXERCISE_UNTIL_TRAINED":mission[0];String command=(navy?"NAVY/":"AIR/")+chosen;if(!MapCommanderAvailability.reason(command).isEmpty())g.fill(bx,by,bx+w,by+h,0x88000000);String tip=MapCommanderAvailability.tooltip(command,name+"\n\n"+detail);out.add(new Hit(bx,by,w,h,tip,()->{if(ids.isEmpty()||!MapCommanderAvailability.allow(command))return;var values=new ArrayList<String>();values.add("mission/"+chosen);values.addAll(ids);MapCommanderHud.open(new Open(navy?Mode.NAVAL_TOOLBAR:Mode.AIR_TOOLBAR,"",values));}));
        }
    }
    private static void airOrganizationButton(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,String action,String icon,String key,List<String> ids,int x,int y){
        button(g,out,"",icon,x,y,u.size(14.5),u.size(14.5),UiCommandLabels.text(key,action.equals("CREATE")?"비행단 창설":action),()->{var selection=new ArrayList<String>();selection.add("action/"+action);selection.addAll(ids);MapCommanderHud.open(new Open(Mode.AIR_ORGANIZE,"",selection));});
    }
    private static List<String> airGroupIds(List<Member> wings){return wings.stream().filter(w->w.data()!=null&&w.data().air()!=null&&!w.data().air().airGroup().isEmpty()).map(w->w.data().air().airGroup()).distinct().toList();}
    private static void airGroups(String action,List<Member> wings){var groups=airGroupIds(wings);if(action.equals("merge")&&groups.size()<2||action.equals("remove")&&groups.isEmpty())return;var ids=action.equals("merge")?groups:wings.stream().map(Member::id).toList();if(!ids.isEmpty())MapCommanderHud.open(new Open(Mode.AIR_GROUPS,action,ids));}
    private static String airPolicyTooltip(String policy){String key="AIR_REINFORCE_"+policy;return UiCommandLabels.text(key+"_SHORT",policy)+"\n"+UiCommandLabels.text(key,policy)+"\n"+UiCommandLabels.text("AIR_REINFORCE_SELECTION","")+"\n\n"+UiCommandLabels.text("AIR_REINFORCE_DESC","");}
    private static void airPolicy(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Member> wings,int x,int y){
        var policies=wings.stream().filter(w->w.data()!=null&&w.data().air()!=null).map(w->w.data().air().reinforcementPolicy()).distinct().toList();
        String tip=policies.isEmpty()?UiCommandLabels.text("AIR_REINFORCE_SELECTION",""):String.join("\n\n",policies.stream().map(MapCommanderPanels::airPolicyTooltip).toList());
        int w=u.size(14.5),h=u.size(14.8);if(policies.size()==1){int frame=List.of("OBSOLETE_ONLY","ANY","ONLY_NEW","ONLY_SAME").indexOf(policies.getFirst());var texture=net.minecraft.resources.Identifier.parse("hoi:textures/gui/commander/commands/wing_prio_strip/atlas.png");g.blit(texture,x,y,x+w,y+h,frame/8f,(frame+1)/8f,0,1);}else label(g,"—",x,y,w,h,UiMenuStyle.TEXT);
        var request=new Open(Mode.AIR_REINFORCEMENT,"policy",wings.stream().map(Member::id).toList());out.add(new Hit(x,y,w,h,tip,()->MapCommanderHud.open(request)));
        button(g,out,"","nv_btn_base_partial_toggle",x+u.size(38),y-u.size(1),u.size(12),u.size(16),tip,()->MapCommanderHud.open(request));
    }
    private static void airBaseHeader(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Member wing,int x,int y,int w,int h) {
        background(g,"av_base_entry_bg",x,y,w,h);var owner=MapCommanderAirBases.state(wing);if(owner!=null&&!owner.controller().isEmpty())UiAssets.drawContent(g,UiMenuBar.flagTexture(owner.controller()),x+u.size(5),y+u.size(4),u.size(13),u.size(9));label(g,wing.group(),x+u.size(33.5),y+u.size(5.5),u.size(60),u.size(11.5),UiMenuStyle.TEXT);
        UiAssets.draw(g,source("fuel_strip"),x+u.size(19),y+u.size(4.5),u.size(10),u.size(13));out.add(new Hit(x+u.size(19),y+u.size(4.5),u.size(10),u.size(13),MapCommanderAirBases.fuelTooltip(current.country()),()->{},null,""));
        var base=MapCommanderAirBases.state(wing);String tip=MapCommanderAirBases.tooltip(wing);int color=base==null?UiMenuStyle.MUTED:base.totalPlanes()>base.capacity()?0xffe15b5b:0xff83c64f;label(g,MapCommanderAirBases.capacity(wing),x+u.size(104),y+u.size(6.5),u.size(35),u.size(11.5),color);out.add(new Hit(x+u.size(104),y,u.size(35),h,tip,()->{},null,""));
        airOrganizationButton(g,u,out,"CREATE","air_base_create_new_btn","AIRWING_NEW",base==null?List.of():List.of("base/"+base.id()),x+u.size(244.5),y+u.size(4));
        button(g,out,"×","nv_btn_close",x+u.size(259.5),y+u.size(4),u.size(14.5),u.size(14.5),"선택 해제 · "+wing.group(),()->{String id=MapCommanderAirBases.identity(wing);showWings(current.wings().stream().filter(MapCommanderPanels::wingSelected).filter(member->!MapCommanderAirBases.identity(member).equals(id)).map(Member::id).toList());});
        out.add(new Hit(x,y,w,h,tip,()->{},null,""));
    }
    private static void air(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Member> wings) {
        int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),w=u.size(274),h=u.size(24);
        branchButtons(g,u,out,false,wings.stream().map(Member::id).toList(),x,y);y+=u.size(22);
        int toolbar=u.size(314);background(g,"air_priorities_tiled_bg",x,y,toolbar,h);label(g,UiCommandLabels.text("UNITS_VIEW_WINGS_COUNT","$WINGS|G$개 비행단 선택됨").replace("$WINGS|G$","§G"+wings.size()+"§!"),x+u.size(23.5),y+u.size(6.5),u.size(54),u.size(11.5),UiMenuStyle.TEXT);
        var ids=wings.stream().map(Member::id).toList();
        airOrganizationButton(g,u,out,"DISBAND","nv_btn_delete","AIRWING_DISBAND",ids,x+u.size(3.5),y+u.size(4));
        button(g,out,"","add_to_air_group",x+u.size(77.5),y+u.size(4),u.size(14.5),u.size(14.5),UiCommandLabels.text("CREATE_AIR_GROUP",""),()->airGroups("create",wings));
        airOrganizationButton(g,u,out,"SPLIT","nv_btn_split_in_half","AIRWING_SPLIT",ids,x+u.size(92.5),y+u.size(4));
        airOrganizationButton(g,u,out,"MERGE","consolidate","AIRWING_MERGE",ids,x+u.size(107.5),y+u.size(4));
        var groups=airGroupIds(wings);String merge=UiCommandLabels.text("AIRWING_MERGE_GROUPS","")+(groups.size()<2?"\n"+UiCommandLabels.text("AIRWING_MERGE_GROUPS_BLOCKED",""):"");
        button(g,out,"","nv_btn_merge",x+u.size(122.5),y+u.size(4),u.size(14.5),u.size(14.5),merge,()->airGroups("merge",wings));
        airPolicy(g,u,out,wings,x+u.size(140),y+u.size(4));
        button(g,out,"","uv_btn_unassign",x+u.size(282.5),y+u.size(4),u.size(14.5),u.size(14.5),UiCommandLabels.text("REMOVE_FROM_AIR_GROUP","")+(groups.isEmpty()?"\n"+UiCommandLabels.text("AIRWING_NEED_ONE_GROUPED_SELECTED",""):""),()->airGroups("remove",wings));
        button(g,out,"","select_half",x+u.size(252.5),y+u.size(4),u.size(14.5),u.size(14.5),UiCommandLabels.text(wings.size()>1?"SELECT_HALF_WINGS":"SELECT_HALF_WINGS_BLOCKED","절반 선택"),()->{if(wings.size()>1){MapUnitHud.clearSelection();selectWings(ids.subList(0,(ids.size()+1)/2));wingList=true;}});
        button(g,out,"","hold",x+u.size(267.5),y+u.size(4),u.size(14.5),u.size(14.5),UiCommandLabels.text("HOLD_WINGS",""),()->{var values=new ArrayList<String>();values.add("mission/HOLD");values.addAll(ids);MapCommanderHud.open(new Open(Mode.AIR_TOOLBAR,"",values));});
        button(g,out,"×","nv_btn_close",x+u.size(297.5),y+u.size(4),u.size(14.5),u.size(14.5),"선택 해제",MapCommanderPanels::close);
        y+=h;int count=Math.max(1,(MapCommanderLayout.popupBottom(u,false)-y-u.size(15))/h);scroll=Math.clamp(scroll,0,Math.max(0,wings.size()-1));
        int shown=0;String group="";for(var wing:wings.subList(scroll,Math.min(wings.size(),scroll+count))){String base=MapCommanderAirBases.identity(wing);boolean header=!group.equals(base);if(y+h*(header?2:1)>MapCommanderLayout.popupBottom(u,false))break;if(header){group=base;airBaseHeader(g,u,out,wing,x,y,w,h);y+=h;}member(g,u,out,wing,x,y,w,h,"av_wing_entry_bg");y+=h;shown++;}
        pager(g,u,out,x,y,w,scroll>0,scroll+shown<wings.size(),Math.max(1,shown));
    }
}
