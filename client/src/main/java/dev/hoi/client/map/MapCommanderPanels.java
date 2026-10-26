package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class MapCommanderPanels {
    private static final LinkedHashSet<String> selected=new LinkedHashSet<>();
    private static final LinkedHashSet<String> selectedForces=new LinkedHashSet<>();
    private static final LinkedHashSet<String> selectedWings=new LinkedHashSet<>();
    private static final Set<String> armyListGroups=new HashSet<>();
    private static List<Hit> hits=List.of();
    private static View current;
    private static String templateCard="";
    private static List<Member> divisionDetails=List.of();
    private static final Set<String> fullArmyCards=new HashSet<>(),manualArmyKeys=new HashSet<>();
    private static Set<String> pendingArmyDivisions=Set.of(),previousArmyCards=Set.of();
    private static int scroll,forcePage;
    private static Scrollbar scrollbar;
    private static double scrollGrab=-1;
    private record Scrollbar(int x,int y,int width,int height,int thumb,int maximum) {}
    private static boolean wingList,postureMenu,navalTargetMenu,specificForces,dragArmySelection,airCardSelection;
    static record Hit(int x,int y,int w,int h,String tip,Runnable action,java.util.function.BiConsumer<Integer,Boolean> gesture,String sound,String icon,float pixelShift,float horizontalShift){Hit(int x,int y,int w,int h,String tip,Runnable action,java.util.function.BiConsumer<Integer,Boolean> gesture,String sound,String icon,float pixelShift){this(x,y,w,h,tip,action,gesture,sound,icon,pixelShift,0);}Hit(int x,int y,int w,int h,String tip,Runnable action,java.util.function.BiConsumer<Integer,Boolean> gesture,String sound,String icon){this(x,y,w,h,tip,action,gesture,sound,icon,0);}Hit(int x,int y,int w,int h,String tip,Runnable action,java.util.function.BiConsumer<Integer,Boolean> gesture,String sound){this(x,y,w,h,tip,action,gesture,sound,"");}Hit {if(!preservesSourceTooltip(icon))tip=UiCommandLabels.withoutShortcuts(tip);}Hit(int x,int y,int w,int h,String tip,Runnable action){this(x,y,w,h,tip,action,null,"ui.click");}Hit(int x,int y,int w,int h,String tip,Runnable action,java.util.function.BiConsumer<Integer,Boolean> gesture){this(x,y,w,h,tip,action,gesture,"");}Hit shiftedX(float shift){return new Hit(x,y,w,h,tip,action,gesture,sound,icon,pixelShift,shift);}float visualX(){return x+horizontalShift;}boolean contains(double px,double py){return px>=x+horizontalShift-pixelShift&&px<x+w+horizontalShift-pixelShift&&py>=y+pixelShift&&py<y+h+pixelShift;}}
    static boolean preservesSourceTooltip(String icon){return Set.of("change_template_button","template_summary","army_order_warning","army_portrait","army_trait","army_group_disband","deploy_hq_button","select_all_armylist_short_btn","motorization_level_toggle_btn","army_group_toggle_armies_divisions_btn","railway_gun_toggle_btn","army_select_wings_btn").contains(icon)||icon.startsWith("theater_")||icon.startsWith("ability_")||icon.startsWith("naval_");}
    static List<Hit> hits(){return hits;}
    private static String detailHover="";
    private static long detailHoverStarted;
    private static final Map<String,String> detailedTooltips=new HashMap<>();
    static String tooltipAt(double x,double y){
        for(var hit:hits)if(hit.contains(x,y)){
            String key=detailedTooltips.containsKey(hit.icon())?hit.icon():"";
            if(!key.equals(detailHover)){detailHover=key;detailHoverStarted=net.minecraft.util.Util.getMillis();}
            return !key.isEmpty()&&net.minecraft.util.Util.getMillis()-detailHoverStarted>=1000?detailedTooltips.get(key):hit.tip();
        }
        detailHover="";return "";
    }
    private static String panelTooltip(Panel panel,String key){return panel==null?"":panel.tooltips().getOrDefault(key,"");}
    private static String memberTooltip(Member member,String key){return member.data()==null?"":member.data().tooltips().getOrDefault(key,"");}
    static boolean scrollAt(double x,double y,double amount){if(MapCommanderTheaters.scrollAt(x,y,amount))return true;if(amount==0||hits.stream().noneMatch(h->h.contains(x,y)))return false;scroll=Math.max(0,scroll-(int)Math.signum(amount)*(current!=null&&current.branch()==Mode.NAVY_HQ?1:3));return true;}
    static boolean pressScroll(double x,double y){var bar=scrollbar;if(bar==null||x<bar.x()||x>=bar.x()+bar.width()||y<bar.y()||y>=bar.y()+bar.height())return false;int top=bar.y()+(int)Math.round((bar.height()-bar.thumb())*scroll/(double)bar.maximum());scrollGrab=y>=top&&y<top+bar.thumb()?y-top:bar.thumb()/2.;return dragScroll(y);}
    static boolean dragScroll(double y){var bar=scrollbar;if(scrollGrab<0||bar==null)return false;scroll=(int)Math.round(Math.clamp((y-bar.y()-scrollGrab)/Math.max(1,bar.height()-bar.thumb()),0,1)*bar.maximum());return true;}
    static boolean releaseScroll(){boolean active=scrollGrab>=0;scrollGrab=-1;return active;}
    private MapCommanderPanels() {}
    static void accept(View view) {
        if(current==null||!current.country().equals(view.country())||!current.dimension().equals(view.dimension())||current.branch()!=view.branch()){if(current!=null){MapUnitHud.clearSelection();MapCommanderOrders.scopeChanged(current,view);}reset();}
        if(view.cards().stream().noneMatch(c->c.id().equals(templateCard)))templateCard="";
        if(current!=null&&view.branch()==Mode.NAVY_HQ&&view.cards().stream().allMatch(c->c.panel()!=null&&c.panel().members().size()==c.panel().total())){
            var removed=new HashSet<String>();current.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).map(Member::id).forEach(removed::add);
            view.cards().stream().flatMap(c->c.panel().members().stream()).map(Member::id).forEach(removed::remove);
            if(!removed.isEmpty())MapUnitHud.replaceKeys(MapUnitHud.selectedKeys().stream().filter(key->!removed.contains(key.startsWith("navy/")?key.substring(5):key.split("\\|",2)[0])).toList());
        }
        var previousMembers=knownArmyMembers(current);current=view;armyListGroups.retainAll(view.cards().stream().map(Card::id).toList());selectedForces.removeIf(id->view.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).noneMatch(m->m.id().equals(id)));selected.removeIf(id->view.cards().stream().noneMatch(c->c.id().equals(id)));if(view.branch()==Mode.NAVY_HQ&&specificForces&&selectedForces.isEmpty())selected.clear();selectedWings.removeIf(id->view.wings().stream().noneMatch(w->w.id().equals(id)));hits=List.of();fullArmyCards.retainAll(selected);if(view.branch()==Mode.ARMY&&view.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY||c.role()==Mode.ARMY_GROUP).allMatch(c->c.panel()!=null&&c.panel().divisionIds()!=null)){previousMembers.removeAll(knownArmyMembers(view));manualArmyKeys.removeIf(key->key.startsWith("unit/")&&previousMembers.contains(key.substring(5)));}if(view.branch()==Mode.ARMY)syncArmySelection();if(view.branch()==Mode.AIR&&airCardSelection)syncAirCardSelection();selectCreatedArmy();MapCommanderOrders.selectionChanged();
    }
    static void awaitCreatedArmy(List<String> selection){
        pendingArmyDivisions=selection.stream().filter(id->id.startsWith("unit/")).map(id->id.substring(5)).collect(java.util.stream.Collectors.toSet());
        previousArmyCards=current==null?Set.of():current.cards().stream().map(Card::id).collect(java.util.stream.Collectors.toSet());
    }
    private static void selectCreatedArmy(){
        if(pendingArmyDivisions.isEmpty())return;
        var created=current.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&!previousArmyCards.contains(c.id())&&c.panel()!=null&&c.panel().divisionIds()!=null&&new HashSet<>(c.panel().divisionIds()).equals(pendingArmyDivisions)).toList();
        pendingArmyDivisions=Set.of();previousArmyCards=Set.of();
        if(created.size()==1)select(created.getFirst().id(),false);
    }
    static void reset(){detailHover="";detailedTooltips.clear();pendingArmyDivisions=Set.of();previousArmyCards=Set.of();airCardSelection=false;dragArmySelection=false;templateCard="";armyListGroups.clear();if(current!=null)MapUnitHud.clearSelection();fullArmyCards.clear();manualArmyKeys.clear();current=null;divisionDetails=List.of();selected.clear();selectedForces.clear();specificForces=false;selectedWings.clear();hits=List.of();scrollbar=null;scrollGrab=-1;scroll=0;forcePage=0;wingList=false;postureMenu=false;navalTargetMenu=false;}
    static boolean selected(String id){return selected.contains(id);}
    static Set<String> selectedCards(){return Set.copyOf(selected);}
    static Set<String> selectedWingIds(){return Set.copyOf(selectedWings);}
    private static boolean wingSelected(Member wing){return selectedWings.contains(wing.id())||current!=null&&current.branch()!=Mode.AIR&&!wing.selectionKey().isEmpty()&&MapUnitHud.selectedKeys().contains(wing.selectionKey());}
    static List<String> attachmentWings(){return current==null?List.of():current.wings().stream().filter(MapCommanderPanels::wingSelected).map(Member::id).toList();}
    static List<AirRange> airRanges(){return current==null||current.branch()!=Mode.AIR||!MapCommanderHud.cardsVisible()?List.of():current.wings().stream().filter(MapCommanderPanels::wingSelected).filter(w->w.data()!=null&&w.data().air()!=null&&w.data().air().wing()!=null).map(w->w.data().air().wing().range()).filter(Objects::nonNull).toList();}
    static boolean attachmentAllowed(){return current!=null&&current.wings().stream().filter(MapCommanderPanels::wingSelected).allMatch(wing->Set.of("FIGHTER","HEAVY_FIGHTER","CAS","TACTICAL_BOMBER","NAVAL_BOMBER").contains(Objects.toString(airRole(wing),"")));}
    static String airRole(Member wing){return wing.data()==null||wing.data().air()==null||wing.data().air().wing()==null?null:wing.data().air().wing().role();}
    static boolean openMission(Open request){
        if(current==null)return false;
        if(request.mode()==Mode.AIR_MISSION){if(current.wings().stream().noneMatch(wing->wing.id().equals(request.target())))return false;showWings(List.of(request.target()));return true;}
        if(current.branch()!=Mode.NAVY_HQ)return false;
        if(request.mode()==Mode.NAVAL_MISSION){
            var card=current.cards().stream().filter(c->c.panel()!=null&&c.panel().members().stream().anyMatch(force->force.id().equals(request.target()))).findFirst().orElse(null);if(card==null)return false;
            MapUnitHud.clearSelection();selected.clear();selected.add(card.id());selectedForces.clear();selectedForces.add(request.target());if(MapCommanderHud.theaters.selectCard(current,card.id()))MapCommanderHud.theaterChanged();specificForces=true;scroll=0;hits=List.of();MapCommanderOrders.selectionChanged();return true;
        }
        if(request.mode()!=Mode.NAVAL_TASK_FORCES)return false;var card=current.cards().stream().filter(c->c.id().equals(request.target())).findFirst().orElse(null);
        if(card==null||card.panel()==null||card.panel().total()!=card.panel().members().size())return false;
        if(!selected.contains(card.id()))select(card.id(),false);else {selected.retainAll(Set.of(card.id()));selectedForces.removeIf(id->card.panel().members().stream().noneMatch(force->force.id().equals(id)));hits=List.of();MapCommanderOrders.selectionChanged();}
        openMissionPanel();return true;
    }
    static void openMissionPanel(){if(current==null)return;if(current.branch()==Mode.NAVY_HQ){if(selected.isEmpty())MapCommanderOrders.message("임무를 지정할 함대를 먼저 선택하세요.");else {wingList=false;hits=List.of();}}else {var wings=attachmentWings();if(wings.isEmpty())MapCommanderOrders.message("임무를 지정할 비행단을 먼저 선택하세요.");else showWings(wings);}}
    static void showWings(Collection<String> ids){MapUnitHud.clearSelection();selectWings(ids);wingList=true;}
    static void selectWings(Collection<String> ids){airCardSelection=false;if(current!=null&&current.branch()==Mode.AIR)selected.clear();templateCard="";fullArmyCards.clear();manualArmyKeys.clear();selectedWings.clear();if(current!=null)current.wings().stream().filter(w->ids.contains(w.id())).map(Member::id).forEach(selectedWings::add);scroll=0;hits=List.of();postureMenu=false;MapCommanderOrders.selectionChanged();}
    static void select(String id){select(id,shift());}
    static void select(String id,boolean additive){
        if(current==null||current.cards().stream().noneMatch(c->c.id().equals(id)))return;
        if(MapCommanderHud.theaters.selectCard(current,id))MapCommanderHud.theaterChanged();
        dragArmySelection=false;templateCard="";armyListGroups.remove(id);
        current.cards().stream().filter(c->c.id().equals(id)).findFirst().ifPresent(MapCommanderHud.groups::reveal);var previous=Set.copyOf(selected);selectedForces.clear();specificForces=false;if(current.branch()==Mode.ARMY)selectedWings.clear();if(!additive){selected.clear();selected.add(id);}else if(!selected.remove(id))selected.add(id);
        if(current.branch()==Mode.ARMY){if(!additive){fullArmyCards.clear();manualArmyKeys.clear();fullArmyCards.add(id);}else if(selected.contains(id))fullArmyCards.add(id);else {fullArmyCards.remove(id);retainManualArmySelection();}syncArmySelection();}else {MapUnitHud.clearSelection();if(current.branch()==Mode.AIR){airCardSelection=true;syncAirCardSelection();}}scroll=0;hits=List.of();MapCommanderOrders.selectionChanged();if(!previous.equals(selected))MapCommanderOrders.selectionScopeChanged();
    }
    static List<String> attachedWings(View view,Set<String> selection){
        var armies=new HashSet<String>();
        for(var card:view.cards())if(card.role()==Mode.ASSIGN_ARMY&&(selection.contains(card.id())||card.panel()!=null&&selection.contains(card.panel().parent())))armies.add(card.id());
        return view.wings().stream().filter(w->w.data()!=null&&armies.contains(w.data().attachedArmy())).map(Member::id).toList();
    }
    private static void syncAirCardSelection(){selectedWings.clear();selectedWings.addAll(attachedWings(current,selected));}
    static void acceptDivisionDetails(List<Member> members){
        if(current==null||current.branch()!=Mode.ARMY)return;divisionDetails=List.copyOf(members);
        if(fullArmyCards.isEmpty()){MapUnitHud.replaceDivisions(members.stream().map(Member::id).toList());manualArmyKeys.clear();manualArmyKeys.addAll(MapUnitHud.selectedKeys());if(current!=null&&current.branch()==Mode.ARMY)selected.clear();alignArmyCards();}
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
        return armyRows(card,current.cards(),selectedDivisionRows());
    }
    static List<Member> armyRows(Card card,List<Card> cards,List<Member> selectedRows){
        var panel=card.panel();if(panel==null)return List.of();var ids=panel.divisionIds();var rows=selectedRows.stream().filter(m->ids!=null?ids.contains(m.id()):panel.members().stream().anyMatch(row->row.id().equals(m.id()))).toList();
        if(card.role()!=Mode.ARMY_GROUP)return rows;
        var order=new HashMap<String,Integer>();
        for(int index=0;index<cards.size();index++){
            var child=cards.get(index);var childPanel=child.panel();
            if(child.role()!=Mode.ASSIGN_ARMY||childPanel==null||!childPanel.parent().equals(card.id()))continue;
            var childIds=childPanel.divisionIds()!=null?childPanel.divisionIds():childPanel.members().stream().map(Member::id).toList();
            for(var id:childIds)order.putIfAbsent(id,index);
        }
        return rows.stream().sorted(Comparator.comparingInt(m->order.getOrDefault(m.id(),Integer.MAX_VALUE))).toList();
    }
    static void manualDivisionSelection(){dragArmySelection=false;fullArmyCards.clear();manualArmyKeys.clear();}
    static List<Card> dragPanels(List<Card> cards,Set<String> keys){
        var armies=cards.stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&c.panel()!=null&&c.panel().divisionIds()!=null&&c.panel().divisionIds().stream().anyMatch(id->keys.contains("unit/"+id))).toList();
        if(armies.size()<2)return armies;
        String parent=armies.getFirst().panel().parent();var result=new ArrayList<Card>();
        if(!parent.isEmpty()&&armies.stream().allMatch(c->c.panel().parent().equals(parent)))cards.stream().filter(c->c.role()==Mode.ARMY_GROUP&&c.id().equals(parent)).findFirst().ifPresent(result::add);
        result.addAll(armies);return List.copyOf(result);
    }
    static void mapSelectionChanged(boolean dragged){mapSelectionChanged();dragArmySelection=dragged;}
    private static Set<String> knownArmyMembers(View view){var result=new HashSet<String>();if(view!=null&&view.branch()==Mode.ARMY)for(var card:view.cards())if(card.panel()!=null&&card.panel().divisionIds()!=null)result.addAll(card.panel().divisionIds());return result;}
    private static void retainManualArmySelection(){var allowed=new HashSet<String>();for(var card:current.cards())if(selected.contains(card.id())&&card.panel()!=null&&card.panel().divisionIds()!=null)card.panel().divisionIds().forEach(id->allowed.add("unit/"+id));manualArmyKeys.removeIf(key->key.startsWith("unit/")&&!allowed.contains(key));}
    static void counterSelectionRefreshed(){var keys=MapUnitHud.selectedKeys();manualArmyKeys.removeIf(key->!key.startsWith("unit/")&&!keys.contains(key));}
    private static void syncArmySelection(){
        var ids=new TreeSet<String>(manualArmyKeys);boolean missing=false,oversized=false;
        for(var card:current.cards())if(fullArmyCards.contains(card.id())&&(card.role()==Mode.ASSIGN_ARMY||card.role()==Mode.ARMY_GROUP)){if(card.panel()==null||card.panel().divisionIds()==null){missing=true;oversized|=card.panel()!=null&&card.panel().total()>2048;}else card.panel().divisionIds().forEach(id->ids.add("unit/"+id));}
        if(missing&&!manualArmyKeys.isEmpty()&&manualArmyKeys.size()<=2048){MapUnitHud.replaceKeys(manualArmyKeys);return;}
        if(missing||ids.size()>2048){MapUnitHud.replaceDivisions(List.of());MapCommanderOrders.message(oversized||ids.size()>2048?"선택한 사단이 명령 입력 한도 2048개를 초과했습니다.":"군 구성원 정보를 기다리고 있습니다.");return;}
        MapUnitHud.replaceKeys(ids);
    }
    static List<String> commandSelection(){
        var map=MapUnitHud.selectedKeys();if(!map.isEmpty()&&(current==null||current.branch()!=Mode.AIR))return map.stream().map(key->key.contains("|navy_")?"navy/"+key.substring(0,key.indexOf('|')):key).distinct().sorted().toList();
        if(current!=null&&current.branch()==Mode.NAVY_HQ)return navalSelection().stream().map(id->"navy/"+id).sorted().toList();
        if(current!=null&&current.branch()==Mode.AIR)return attachmentWings().stream().map(id->"air/"+id).sorted().toList();
        return current==null?List.of():attachmentWings().stream().map(id->"air/"+id).sorted().toList();
    }
    static void mapSelectionChanged(){var previous=Set.copyOf(selected);var previousWings=Set.copyOf(selectedWings);airCardSelection=false;if(current!=null&&current.branch()==Mode.AIR)selected.clear();dragArmySelection=false;if(current!=null&&current.branch()==Mode.ARMY)hits=List.of();templateCard="";fullArmyCards.clear();manualArmyKeys.clear();manualArmyKeys.addAll(MapUnitHud.selectedKeys());if(current!=null&&current.branch()==Mode.ARMY)selected.clear();alignArmyCards();selectedWings.clear();selectedForces.clear();specificForces=false;if(current!=null&&current.branch()==Mode.NAVY_HQ){selected.clear();for(var card:current.cards())if(card.panel()!=null)for(var member:card.panel().members())if(MapUnitHud.selectedKeys().stream().anyMatch(key->key.startsWith(member.id()+"|navy_"))){selected.add(card.id());selectedForces.add(member.id());specificForces=true;}}if(current!=null&&current.branch()==Mode.AIR)current.wings().stream().filter(w->!w.selectionKey().isEmpty()&&MapUnitHud.selectedKeys().contains(w.selectionKey())).map(Member::id).forEach(selectedWings::add);if(current!=null&&current.branch()==Mode.AIR){var wings=current.wings().stream().filter(w->selectedWings.contains(w.id())&&w.data()!=null&&!w.data().attachedArmy().isEmpty()).sorted(Comparator.comparing(w->previousWings.contains(w.id()))).toList();if(!wings.isEmpty()&&MapCommanderHud.theaters.selectCard(current,wings.getFirst().data().attachedArmy()))MapCommanderHud.theaterChanged();}if(current!=null){var changed=selected.stream().filter(id->!previous.contains(id)).findFirst().or(()->selected.stream().findFirst());changed.ifPresent(id->{if(MapCommanderHud.theaters.selectCard(current,id))MapCommanderHud.theaterChanged();});}MapCommanderOrders.selectionChanged();}
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
    static boolean armyCommandVisible(){return armyInput()&&!fullArmyCards.isEmpty();}
    static void armyAbility(String ability){var divisions=selectedDivisions();if(!armyCommandVisible()||divisions.isEmpty()){MapCommanderOrders.message("스킬을 사용할 군이나 사단을 선택하세요.");return;}MapCommanderOrders.send(abilityAction(ability),divisions,List.of());}
    static List<String> armyAbilityIds(){
        if(!armyCommandVisible())return List.of();var cards=current.cards().stream().filter(c->selected.contains(c.id())&&c.panel()!=null&&(marshalSkills()?c.role()==Mode.ARMY_GROUP:c.role()==Mode.ASSIGN_ARMY)).toList();
        return armyAbilityIds(cards);
    }
    static List<String> armyAbilityIds(List<Card> cards){
        if(cards.isEmpty()||cards.stream().anyMatch(c->c.panel().commander().isEmpty()||c.panel().abilities()==null))return List.of();
        return cards.getFirst().panel().abilities().stream().limit(4).filter(id->cards.stream().allMatch(c->c.panel().abilities().stream().limit(4).anyMatch(id::equals))).toList();
    }
    private static void armyAbilities(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y,int w){
        var ids=armyAbilityIds();if(ids.isEmpty())return;int span=u.size(99+Math.max(0,ids.size()-4)*18),left=x+(w-u.size(99))/2,top=y-u.size(21),end=u.size(49.5);
        UiAssets.draw(g,source("ability_list_begin"),left,top,end,u.size(22.5));
        if(span>2*end)UiAssets.draw(g,source("ability_list_mid"),left+end,top,span-2*end,u.size(22.5));
        UiAssets.draw(g,source("ability_list_end"),left+span-end,top,end,u.size(22.5));
        for(int n=0;n<ids.size();n++){
            String id=ids.get(n),key="ABILITY_"+id.toUpperCase(Locale.ROOT),icon="ability_"+id;var size=UiAssets.imageSize(source(icon));if(size[0]<=0||size[1]<=0){icon="ability_icon_default";size=UiAssets.imageSize(source(icon));}int width=size[0],height=size[1];
            var details=selectedAbilityDetails(id);String original=details.stream().map(AbilityDetail::tooltip).filter(t->!t.isEmpty()).findFirst().orElseGet(()->MapCommanderAvailability.tooltip(abilityAction(id),UiCommandLabels.text(key,id)+"\n\n"+UiCommandLabels.text(key+"_DESC","")));
            String reason=MapCommanderAvailability.reason(abilityAction(id));if(!reason.isEmpty()&&!original.contains(reason))original=MapCommanderAvailability.explain(original,reason);
            nativeButton(g,out,icon,left+u.size(14.5+n*18),top+u.size(4),u.size(width/2.0),u.size(height/2.0),original,()->armyAbility(id),details.stream().anyMatch(MapCommanderPanels::abilityGrayscale));
            if(!reason.isEmpty()){var hit=out.getLast();g.fill(hit.x(),hit.y(),hit.x()+hit.w(),hit.y()+hit.h(),0x55000000);}
        }
    }
    private static List<AbilityDetail> selectedAbilityDetails(String id){return current.cards().stream().filter(c->selected.contains(c.id())&&c.panel()!=null&&(marshalSkills()?c.role()==Mode.ARMY_GROUP:c.role()==Mode.ASSIGN_ARMY)).flatMap(c->c.panel().abilityDetails().stream()).filter(a->a.id().equals(id)).toList();}
    static boolean abilityGrayscale(AbilityDetail ability){return Boolean.FALSE.equals(ability.affordable())||!ability.enabled()&&ability.affordable()==null;}
    private static List<String> navalSelection(){return current==null?List.of():current.cards().stream().filter(c->selected.contains(c.id())&&c.panel()!=null).flatMap(c->c.panel().members().stream()).map(Member::id).filter(id->!specificForces||selectedForces.contains(id)).distinct().toList();}
    private static void navalOrganization(String action,boolean half){var ids=navalSelection();if(ids.isEmpty())return;var values=new ArrayList<String>();ids.forEach(id->values.add("force/"+id));if(half)values.add("half");MapCommanderHud.open(new Open(Mode.NAVAL_ORGANIZE,action,values));}
    private static final String[] NAVAL_TOOLS={"DELETE","CREATE","SPLIT","SELECT_HALF","RESERVE","MERGE","AUTO_ORGANIZE","UNDERWAY","UPGRADE","REPAIR_NOW","STANCE","REINFORCE","SPLIT_REPAIR","REPAIR_PRIORITY","AGGRESSIVENESS"};
    private static final String[] NAVAL_TOOL_ICONS={"nv_btn_delete","nv_btn_create_new_from_selected","nv_btn_split_in_half","nv_btn_select_half","nv_btn_set_as_reserve","nv_btn_merge","nv_btn_taskforce_o_matic","nv_btn_underway_replenishment","nv_btn_upgrade","nv_btn_repair_now_small","defensive_stance_strip","nv_btn_reinforce","nv_btn_toggle_split_for_repair","nv_btn_repair_priority","nv_btn_aggressiveness_small"};
    private static void navalTool(String tool,boolean previous){
        if(!MapCommanderAvailability.allow("NAVAL_TB/"+tool))return;
        var forces=navalTargetCandidates();if(forces.isEmpty())return;
        switch(tool){
            case "CREATE" -> navalOrganization("SHIPS",false);
            case "AUTO_ORGANIZE" -> navalOrganization("BALANCE",false);
            case "DELETE","SPLIT","RESERVE","MERGE" -> navalOrganization(tool,false);
            case "SELECT_HALF" -> {specificForces=true;selectedForces.clear();forces.stream().limit((forces.size()+1)/2).forEach(f->selectedForces.add(f.id()));MapUnitHud.clearSelection();MapCommanderOrders.selectionChanged();}
            case "REPAIR_NOW","REPAIR_CANCEL" -> MapCommanderOrders.send("NAVAL_SETTING/"+tool+"_BATCH",forces.stream().map(Member::id).toList(),List.of());
            case "REINFORCE","SPLIT_REPAIR","REPAIR_PRIORITY","AGGRESSIVENESS","STANCE" -> {
                String action=switch(tool){case "REINFORCE"->"REINFORCE";case "SPLIT_REPAIR"->"AUTO_SPLIT";case "REPAIR_PRIORITY"->previous?"REPAIR_PREVIOUS":"REPAIR_NEXT";case "STANCE"->previous?"STANCE_PREVIOUS":"STANCE_NEXT";default->previous?"ENGAGEMENT_PREVIOUS":"ENGAGEMENT_NEXT";};
                var values=navalBatch(tool.equals("STANCE")?forces.stream().sorted(Comparator.comparing(Member::id)).toList():forces,action);if(values.isEmpty()){MapCommanderOrders.message("태스크 포스 설정을 먼저 선택하세요.");return;}
                MapCommanderOrders.send("NAVAL_SETTING/BATCH/"+action,values,List.of());
            }
        }
    }
    static List<String> navalBatch(List<Member> forces,String action){
        if(forces.isEmpty()||forces.size()>160)return List.of();var values=new ArrayList<String>();
        for(int i=0;i<forces.size();i++){var force=forces.get(i);var state=force.data()==null?null:force.data().navy();if(state==null)return List.of();String expected;
            if(action.startsWith("REPAIR_")){if(state.repairPolicy()==null)return List.of();expected="policy/"+state.repairPolicy();}
            else if(action.startsWith("ENGAGEMENT_"))expected="engagement/"+state.engagement();
            else if(action.startsWith("STANCE_"))expected="stance/"+(state.carrierStance()==null?"none":state.carrierStance());
            else {Boolean enabled=action.equals("REINFORCE")?state.reinforce():state.autoSplit();expected="state/"+(enabled==null?"unset":enabled);}
            values.add(force.id());values.add(i+"/"+expected);
        }return List.copyOf(values);
    }
    static Integer navalStance(List<Member> forces){
        var counts=new LinkedHashMap<Integer,Integer>();
        for(var force:forces){var state=force.data()==null?null:force.data().navy();if(state!=null&&state.carrierStance()!=null)counts.merge(state.carrierStance(),1,Integer::sum);}
        Integer result=null;int maximum=0;
        for(var entry:counts.entrySet())if(entry.getValue()>maximum){result=entry.getKey();maximum=entry.getValue();}
        return result;
    }
    private static void navalTools(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y,int w){
        background(g,"task_force_actions_bg",x,y,w,u.size(21));var forces=navalTargetCandidates();var state=forces.isEmpty()||forces.getFirst().data()==null?null:forces.getFirst().data().navy();
        Integer stance=navalStance(forces.stream().sorted(Comparator.comparing(Member::id)).toList());
        for(int n=0;n<NAVAL_TOOLS.length;n++){String tool=NAVAL_TOOLS[n].equals("STANCE")&&stance==null?"REPAIR_CANCEL":NAVAL_TOOLS[n],action="NAVAL_TB/"+tool,icon=tool.equals("REPAIR_CANCEL")?"nv_btn_repair_now_small":NAVAL_TOOL_ICONS[n];if(Set.of("CREATE","SELECT_HALF","UPGRADE").contains(tool))continue;int bx=x+u.size(n==0?3:32+(n-1)*15),by=y+u.size(3),size=u.size(15.5);int frame=tool.equals("STANCE")?stance:state==null?0:tool.equals("REPAIR_PRIORITY")&&state.repairPolicy()!=null?state.repairPolicy():tool.equals("AGGRESSIVENESS")?Math.max(0,List.of("NEVER","LOW","MEDIUM","HIGH","ALWAYS").indexOf(state.engagement())):0;
            boolean disabled=!MapCommanderAvailability.reason(action).isEmpty();String path=source(icon).replace("frame_0","frame_"+frame);int trimLeft=tool.equals("REPAIR_CANCEL")?32:0,trimRight=tool.equals("REPAIR_NOW")?32:0;
            if(n>=7)UiAssets.draw(g,source("nv_btn_base_partial_toggle"),bx,by,size,size);
            if(disabled)UiAssets.drawGrayscale(g,path,bx,by,size,size,trimLeft,trimRight);else UiAssets.draw(g,path,bx,by,size,size,trimLeft,trimRight);
            out.add(new Hit(bx,by,size,size,MapCommanderAvailability.tooltip(action,""),()->navalTool(tool,false),(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT||button==InputConstants.MOUSE_BUTTON_RIGHT&&Set.of("REPAIR_PRIORITY","AGGRESSIVENESS","STANCE").contains(tool))navalTool(tool,button==InputConstants.MOUSE_BUTTON_RIGHT);},tool.equals("DELETE")?"ui.disband":"ui.click","naval_tool_"+tool));
        }
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
    private static void leaderSkill(GuiGraphicsExtractor g,UiUnits u,int value,int x,float y){
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(u.scale()*.5f);g.fill(0,0,23,1,0xff584d51);g.fill(0,1,1,34,0xff584d51);g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+source("land_battle_leader_skill_bg")+".png"),1,1,23,34,1/35f,34/35f,1/50f,49/50f);g.pose().popMatrix();
        var font=Minecraft.getInstance().font;String text=Integer.toString(value);float scale=UiText.scale(font),pixel=(float)Minecraft.getInstance().getWindow().getGuiScale();float left=Math.round((x+(u.length(12.25)-(font.width(text)-1)*scale)/2)*pixel)/pixel,top=Math.round((y+u.length(5.75)-3.5f*scale)*pixel)/pixel;
        g.pose().pushMatrix();g.pose().translate(left,top);g.pose().scale(scale);g.text(font,text,0,0,UiMenuStyle.TEXT,true);g.pose().popMatrix();
    }
    private static void leaderAbility(GuiGraphicsExtractor g,UiUnits u,int value,float center,float top){
        var font=Minecraft.getInstance().font;String text=Integer.toString(value);float pixel=(float)Minecraft.getInstance().getWindow().getGuiScale(),scale=u.scale()*9f/14f;
        g.pose().pushMatrix();g.pose().translate(Math.round((center-font.width(text)*scale/2)*pixel)/pixel,Math.round(top*pixel)/pixel);g.pose().scale(scale);g.text(font,text,0,0,UiMenuStyle.TEXT,true);g.pose().popMatrix();
    }
    private static void label(GuiGraphicsExtractor g,String text,int x,int y,int w,int h,int color){UiText.centeredSingleLine(g,Minecraft.getInstance().font,UiTooltips.officerText(text),x,y,w,h,color);}
    private static void leftLabel(GuiGraphicsExtractor g,String text,int x,int y,int w,int h,int color){leftLabel(g,text,x,y,w,h,color,UiText.scale(Minecraft.getInstance().font));}
    private static void airLabel(GuiGraphicsExtractor g,UiUnits u,String text,int x,int y,int w,int h){leftLabel(g,text,x,y,w,h,UiMenuStyle.TEXT,u.scale());}
    private static void leftLabel(GuiGraphicsExtractor g,String text,int x,int y,int w,int h,int color,float scale){var font=Minecraft.getInstance().font;var value=UiKeywordIcons.decorateLines(UiTooltips.officerText(text),false);if(font.width(value)*scale>w){var clipped=Component.empty();font.substrByWidth(value,Math.max(0,(int)(w/scale)-font.width("…"))).visit((style,part)->{clipped.append(Component.literal(part).setStyle(style));return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);value=clipped.append("…");}g.pose().pushMatrix();g.pose().translate(x,y+(h-font.lineHeight*scale)/2);g.pose().scale(scale);g.text(font,value,0,0,color);g.pose().popMatrix();}
    private static void wrappedLabel(GuiGraphicsExtractor g,UiUnits u,String text,int x,int y,int w,int h,boolean centered){var font=Minecraft.getInstance().font;float scale=UiText.scale(font),lineHeight=font.lineHeight*scale;var lines=UiTooltips.split(font,UiTooltips.officerText(text),Math.max(1,(int)(w/scale)));float top=y+(h-lines.size()*lineHeight)/2;for(var line:lines){g.pose().pushMatrix();g.pose().translate(centered?x+(w-font.width(line)*scale)/2:x,top);g.pose().scale(scale);g.text(font,line,0,0,UiMenuStyle.TEXT);g.pose().popMatrix();top+=lineHeight;}}
    private static void background(GuiGraphicsExtractor g,String id,int x,int y,int w,int h){if(id.equals("tiled_plain_bg_small")){UiAssets.tiledNineSlice(g,source(id),x,y,w,h,16,MapCommanderLayout.units(g.guiWidth(),g.guiHeight()).scale()*.5);return;}if(id.equals("air_priorities_tiled_bg")){UiAssets.nineSlice(g,source(id),x,y,w,h,32,MapCommanderLayout.units(g.guiWidth(),g.guiHeight()).scale()*.5);return;}if(!UiAssets.draw(g,source(id),x,y,w,h))UiMenuStyle.control(g,x,y,w,h,false,false);}
    private static void button(GuiGraphicsExtractor g,List<Hit> out,String text,String icon,int x,int y,int w,int h,String tip,Runnable action){button(g,out,text,icon,x,y,w,h,tip,action,false);}
    private static void button(GuiGraphicsExtractor g,List<Hit> out,String text,String icon,int x,int y,int w,int h,String tip,Runnable action,boolean grayscale){UiAssets.nineSlice(g,source("nv_btn_select"),x,y,w,h,4,MapCommanderLayout.units(g.guiWidth(),g.guiHeight()).scale()*.5);if(icon.isEmpty())label(g,text,x,y,w,h,UiMenuStyle.TEXT);else if(grayscale)UiAssets.drawGrayscale(g,source(icon),x,y,w,h);else UiAssets.draw(g,source(icon),x,y,w,h);out.add(new Hit(x,y,w,h,tip,action,null,icon.equals("nv_btn_close")?"ui.close":icon.equals("nv_btn_delete")?"ui.disband":icon.startsWith("bpt_order_")||icon.equals("exercise_icon")?"ui.battleplan":"ui.click",icon));}
    private static void nativeButton(GuiGraphicsExtractor g,List<Hit> out,String icon,int x,int y,int w,int h,String tip,Runnable action){nativeButton(g,out,icon,x,y,w,h,tip,action,false);}
    private static void nativeButton(GuiGraphicsExtractor g,List<Hit> out,String icon,int x,int y,int w,int h,String tip,Runnable action,boolean grayscale){if(grayscale)UiAssets.drawGrayscale(g,source(icon),x,y,w,h);else UiAssets.draw(g,source(icon),x,y,w,h);out.add(new Hit(x,y,w,h,tip,action,null,"ui.click",icon));}
    private static String commanderName(Card card){return card.panel()!=null&&card.panel().commander().isEmpty()?UiCommandLabels.text("UNIT_LEADER_NO_LEADER","§R지휘관 없음§!"):card.leader();}
    private static Open units(Card card){return new Open(card.role()==Mode.ARMY_GROUP?Mode.ARMY_GROUP:current.branch()==Mode.NAVY_HQ?Mode.NAVAL_TASK_FORCES:Mode.ARMY_UNITS,card.id());}
    static boolean closeSelection(){if(templateCard.isEmpty()&&selected.isEmpty()&&selectedWings.isEmpty()&&!wingList&&MapUnitHud.selectedKeys().isEmpty())return false;close();return true;}
    private static void close(){airCardSelection=false;templateCard="";selected.clear();selectedForces.clear();specificForces=false;selectedWings.clear();MapUnitHud.clearSelection();wingList=false;postureMenu=false;navalTargetMenu=false;scroll=0;hits=List.of();MapCommanderOrders.selectionChanged();}
    static int actionbarTop(UiUnits units,boolean navy){return MapCommanderLayout.commandTop(units,navy);}
    static void draw(GuiGraphicsExtractor g,int sidebar,int mouseX,int mouseY,int portraitsTop) {
        detailedTooltips.clear();
        hits=List.of();scrollbar=null;if(current==null)return;var u=MapCommanderLayout.units(g.guiWidth(),g.guiHeight());var out=new ArrayList<Hit>();
        var chosen=current.cards().stream().filter(c->selected.contains(c.id())||c.id().equals(templateCard)).toList();
        int bottom=MapCommanderLayout.popupBottom(u,current.branch()==Mode.NAVY_HQ);
        var selectedRows=selectedDivisionRows();boolean unassignedOnly=current.branch()==Mode.ARMY&&chosen.isEmpty()&&!selectedRows.isEmpty()&&selectedRows.stream().allMatch(m->m.data()!=null&&m.data().attachedArmy().isEmpty());
        int panelBottom=!unassignedOnly&&(current.branch()!=Mode.ARMY||armyCommandVisible())?actionbarTop(u,current.branch()==Mode.NAVY_HQ)-u.size(armyAbilityIds().isEmpty()?2:26):portraitsTop-u.size(4);
        if(current.branch()==Mode.NAVY_HQ&&u.size(250)<MapCommanderLayout.commandLeft(u,true))panelBottom=portraitsTop-u.size(4);
        if(current.branch()==Mode.ARMY&&!wingList&&attachmentWings().isEmpty()){int panelRight=u.size(221.5)+chosen.stream().mapToInt(c->summaryExtra(c,u)).max().orElse(0);if(panelRight<MapCommanderLayout.commandLeft(u,false))panelBottom=Math.min(portraitsTop-u.size(4),g.guiHeight()-u.size(85));}
        if(current.branch()==Mode.ARMY?armyCommandVisible():current.branch()==Mode.NAVY_HQ&&!chosen.isEmpty())actionbar(g,u,out,chosen,actionbarTop(u,current.branch()==Mode.NAVY_HQ),bottom);
        if(sidebar==0&&(!(Minecraft.getInstance().gui.screen() instanceof dev.hoi.client.screen.ScreenDialog)||MapCommanderHud.backdropPass())) {
            if(current.branch()==Mode.ARMY&&dragArmySelection&&!dragPanels(current.cards(),MapUnitHud.selectedKeys()).isEmpty()){var panels=dragPanels(current.cards(),MapUnitHud.selectedKeys());if(panels.size()>1)dragStack(g,u,out,panels,panelBottom);else command(g,u,out,panels,panelBottom);}
            else if(current.branch()==Mode.ARMY&&templateCard.isEmpty()&&fullArmyCards.isEmpty()&&!selectedDivisionRows().isEmpty())armySelection(g,u,out,panelBottom);
            else if(current.branch()==Mode.AIR||wingList) {
                var wings=current.wings().stream().filter(MapCommanderPanels::wingSelected).sorted(Comparator.comparing(MapCommanderAirBases::identity).thenComparing(Member::id)).toList();
                if(!wings.isEmpty())air(g,u,out,wings,panelBottom);
                else if(current.branch()!=Mode.AIR&&!chosen.isEmpty())command(g,u,out,chosen,panelBottom);
            } else if(!chosen.isEmpty())command(g,u,out,chosen,panelBottom);
            else if(current.branch()==Mode.ARMY&&!selectedDivisionRows().isEmpty())armySelection(g,u,out,panelBottom);
        }
        MapCommanderTheaters.draw(g,u,current,out,portraitsTop);
        hits=List.copyOf(out);
    }
    private static void command(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> chosen,int bottom) {
        if(current.branch()==Mode.ARMY&&chosen.size()==1&&chosen.getFirst().role()==Mode.ARMY_GROUP&&armyListGroups.contains(chosen.getFirst().id())){armyOverview(g,u,out,chosen.getFirst(),bottom);return;}
        boolean navy=current.branch()==Mode.NAVY_HQ;if(!navy&&chosen.size()>1){armyStack(g,u,out,chosen,bottom);return;}if(navy&&chosen.size()>1){navyStack(g,u,out,chosen,bottom);return;}int x=u.size(3),top=UiMenuBar.height(g.guiWidth())+u.size(2),w=u.size(navy?246.5:218.5)+(navy?0:summaryExtra(chosen.getFirst(),u)),header=u.size(navy?83:55);
        int visible=Math.max(1,(Math.max(top+header,bottom)-top)/header),start=chosen.size()>1?Math.clamp(scroll,0,Math.max(0,chosen.size()-visible)):0;
        int y=top;
        for(var card:chosen.subList(start,Math.min(chosen.size(),start+visible))) {
            if(navy){navyHeader(g,u,out,card,x,y,w,header);y+=header;continue;}
            armyHeader(g,u,out,card,x,y,w,header);
            y+=header-u.size(3);
            if(card.role()!=Mode.ARMY_GROUP){armyHeadquarters(g,u,out,card,x,y,w);y+=u.size(21.5);}
        }
        if(chosen.size()>1){pager(g,u,out,x,y,w,scroll>0,start+visible<chosen.size(),1);out.add(new Hit(x,top,w,y-top,"",()->{},null,""));return;}
        var card=chosen.getFirst();var panel=card.panel();if(panel==null)return;
        int toolbarExtra=w-u.size(218.5),tools=u.size(navy?22:21);if(navy)navalTools(g,u,out,x,y,w);else {int visibleRows=Math.min(MapCommanderHud.groups.rowsVisible(card.id())?armyRows(card).size():0,Math.max(1,(bottom-y-tools-u.size(13.5))/u.size(16.5)));armyListBackground(g,u,x-u.size(2),y-u.size(1.5),w-u.size(1),tools+u.size(18)+visibleRows*u.size(16.5));selectionBackground(g,u,x,y,w);
        g.enableScissor(x+u.size(36.5),y,x+toolbarExtra+u.size(103),y+tools);UiText.text(g,Minecraft.getInstance().font,UiTooltips.officerText(UiCommandLabels.text("UNITS_VIEW_SELECTED_DIVISIONS_COUNT","$DIV|G$개 사단 선택됨").replace("$DIV|G$","§G"+selectedDivisions().size()+"§!")),x+u.size(36.5),y+(tools-Minecraft.getInstance().font.lineHeight)/2+u.size(2),UiMenuStyle.TEXT);g.disableScissor();
        templateButton(g,u,out,x-u.size(4),y);
        var selectedRows=selectedDivisionRows();boolean weightKnown=selectedRows.size()==selectedDivisions().size()&&!selectedRows.isEmpty()&&selectedRows.stream().allMatch(m->m.data()!=null&&m.data().transportWeight()!=null);String weight=weightKnown?Long.toString(Math.round(selectedRows.stream().mapToDouble(m->m.data().transportWeight()).sum())):"0";
        UiAssets.draw(g,source("generic_mini_bg"),x+toolbarExtra+u.size(134.5),y+u.size(5.5),u.size(34),u.size(13));UiAssets.drawContent(g,source("topbar_convoys"),x+toolbarExtra+u.size(135.5),y+u.size(7),u.size(9),u.size(9));leftLabel(g,weight,x+toolbarExtra+u.size(145),y+u.size(6),u.size(18.5),u.size(11.5),UiMenuStyle.TEXT);out.add(new Hit(x+toolbarExtra+u.size(134.5),y+u.size(5.5),u.size(34),u.size(13),UiCommandLabels.text("TRANSPORT_WEIGHT_DESC",""),()->{},null,""));
        button(g,out,"","uv_btn_unassign",x+toolbarExtra+u.size(197),y+u.size(3.5),u.size(15.5),u.size(15.5),UiCommandLabels.text(selectedDivisions().size()==1?"CONFIRMUNASSIGNUNIT":"CONFIRMUNASSIGNUNITS",""),()->landSettings("UNASSIGN_UNITS"));
        }
        if(!navy)disbandButton(g,u,out,x-u.size(4),y);
        if(!navy)divisionActions(g,u,out,x+w-u.size(218.5),y);
        List<Member> rows=MapCommanderHud.groups.rowsVisible(card.id())?(navy?panel.members():armyRows(card)):List.of();y+=tools;if(!navy){armyColumns(g,u,x,y,w);y+=u.size(10.5);}int row=u.size(navy?62:16.5);int end=Math.max(y+row,Math.min(g.guiHeight(),bottom));int count=Math.max(1,(end-y-u.size(navy?0:3))/row);scroll=Math.clamp(scroll,0,Math.max(0,rows.size()-count));
        for(var member:rows.subList(scroll,Math.min(rows.size(),scroll+count))) {member(g,u,out,member,x,y,navy?w:w-u.size(15),row,navy?"task_force_bg":"uv_unit_entry_bg");y+=row;}
        if(navy)armyScrollbar(g,u,out,x+w-u.size(8),y-Math.min(count,rows.size()-scroll)*row,u.size(8),count*row,rows.size(),count);else armyScrollbar(g,u,out,x+w-u.size(13.5),y-Math.min(count,rows.size()-scroll)*row,u.size(8.5),count*row,rows.size(),count);
        if((navy||fullArmyCards.contains(card.id()))&&MapCommanderHud.groups.rowsVisible(card.id())&&panel.total()>panel.members().size())button(g,out,"전체 목록","",x,y+u.size(18),w,u.size(17),"소속 부대 전체 목록",()->MapCommanderHud.open(units(card)));out.add(new Hit(x,top,w,y-top,"",()->{},null,""));
    }
    private static void armyHeader(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Card card,int x,int y,int w,int h){
        Integer color=card.role()==Mode.ARMY_GROUP?Integer.valueOf(0x806000):card.panel()==null?null:card.panel().armyColor();if(color!=null)UiAssets.drawTinted(g,source("group_marker3"),x+w-u.size(16),y+u.size(7),u.size(22),u.size(44),0xff000000|color);
        boolean group=card.role()==Mode.ARMY_GROUP;var panel=card.panel();int extra=summaryExtra(card,u);var headerTexture=net.minecraft.resources.Identifier.parse("hoi:textures/gui/commander/commands/"+(group?"army_group_bg":"army_bg")+"/frame_0.png");int sourceWidth=(group?436:437),sourceHeight=(group?110:109);g.blit(headerTexture,x-u.size(2.5),y,x+u.size(148.5),y+h,0,297f/sourceWidth,0,1);g.blit(headerTexture,x+u.size(148.5),y,x+w-u.size(group?3:2),y+h,297f/sourceWidth,(sourceWidth-4f)/sourceWidth,0,1);g.blit(headerTexture,x+w-u.size(group?3:2),y,x+w-u.size(group?1:0),y+h,(sourceWidth-4f)/sourceWidth,1,0,1);g.blit(headerTexture,x+u.size(99),y+u.size(17.5),x+u.size(153),y+u.size(38),300f/sourceWidth,408f/sourceWidth,35f/sourceHeight,76f/sourceHeight);g.blit(headerTexture,x+u.size(99),y+u.size(18.5),x+u.size(148),y+u.size(36),220f/sourceWidth,297f/sourceWidth,37f/sourceHeight,72f/sourceHeight);
        UiAssets.drawTinted(g,source(group?"theatre_army_group_shield":"theatre_army_shield").replace("frame_0","frame_"+(group||panel==null||panel.insignia()==null?0:panel.insignia())),x+u.size(7.5),y,u.size(16.5),u.size(16),group?0xffb29854:panel==null||panel.armyColor()==null?0xffffffff:0xff000000|panel.armyColor());label(g,Integer.toString(card.count()),x+u.size(12.5),y+u.size(4.5),u.size(14),u.size(11.5),UiMenuStyle.TEXT);
        if(!group&&panel!=null&&!panel.palette().isEmpty())out.add(new Hit(x+u.size(7.5),y,u.size(19),u.size(16),UiCommandLabels.text("CHOOSE_INSIGNIA",""),()->MapCommanderHud.togglePalette(card.id()),null,"ui.click","army_color_button"));
        leftLabel(g,card.name(),x+u.size(33),y+u.size(group?5:4),u.size(67.5),u.size(11.5),UiMenuStyle.TEXT);
        UiAssets.drawContent(g,source("supply_strip"),x+u.size(98),y+u.size(3.5),u.size(11),u.size(11));UiAssets.drawContent(g,source("fuel_strip"),x+u.size(110),y+u.size(5),u.size(10),u.size(11));
        out.add(new Hit(x+u.size(98),y+u.size(3.5),u.size(11),u.size(11),panelTooltip(panel,"supply"),()->{},null,"","army_supply"));
        out.add(new Hit(x+u.size(110),y+u.size(5),u.size(10),u.size(11),panelTooltip(panel,"fuel"),()->{},null,"","army_fuel"));
        if(group){int gx=x+extra+u.size(139),gy=y+u.size(1.5),gs=u.size(15.5);var dissolve=panel==null?null:panel.controls().stream().filter(c->c.id().equals("GROUP_DISSOLVE")).findFirst().orElse(null);boolean enabled=dissolve!=null&&dissolve.enabled();if(enabled)UiAssets.draw(g,source("uv_btn_unassign"),gx,gy,gs,gs);else UiAssets.drawGrayscale(g,source("uv_btn_unassign"),gx,gy,gs,gs);out.add(new Hit(gx,gy,gs,gs,dissolve==null?UiCommandLabels.text("UNASSIGN_ARMY_GROUP",""):dissolve.tooltip(),()->{if(enabled)MapCommanderOrders.formation(shift()?"DISSOLVE_GROUP_ALL":"DISSOLVE_GROUP","",List.of(card.id()));},null,enabled?"ui.click":"","army_group_disband"));}
        else if(panel!=null&&!panel.parent().isEmpty()){
            var parent=current.cards().stream().filter(c->c.role()==Mode.ARMY_GROUP&&c.id().equals(panel.parent())).findFirst().orElse(null);
            UiAssets.drawTinted(g,source("theatre_army_group_shield"),x+extra+u.size(122.5),y+u.size(3),u.size(14.85),u.size(14.4),0xffb29854);
            if(parent!=null){String number=parent.name().split("\\s",2)[0];if(number.matches("[0-9]+"))label(g,number,x+extra+u.size(126.5),y+u.size(6.5),u.size(14),u.size(11.5),UiMenuStyle.TEXT);}
            button(g,out,"","uv_btn_unassign",x+extra+u.size(139),y+u.size(1.5),u.size(15.5),u.size(15.5),UiCommandLabels.text("UNASSIGN","배정 해제"),()->MapCommanderOrders.formation("DETACH_ARMIES","",List.of(card.id())));
        }
        int countX=x+extra+u.size(group?154.5:155.5),countY=y+u.size(group?1.5:3);g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+source(group?"group_div_count_bg":"div_count_bg")+".png"),countX,countY,countX+u.size(group?47:45),countY+u.size(13),0,1,0,1);label(g,MapCommanderHud.countLabel(card),x+extra+u.size(156),y+u.size(group?3:4),u.size(45),u.size(11.5),MapCommanderHud.countColor(card));
        float pixelShift=1f/(float)Minecraft.getInstance().getWindow().getGuiScale();g.pose().pushMatrix();g.pose().translate(-pixelShift,pixelShift);
        button(g,out,"×","nv_btn_close",x+extra+u.size(200.5),y+u.size(1.5),u.size(13),u.size(13),"닫기",()->closeCard(card));
        g.pose().popMatrix();var close=out.removeLast();out.add(new Hit(close.x(),close.y(),close.w(),close.h(),close.tip(),close.action(),close.gesture(),close.sound(),close.icon(),pixelShift));
        int px=x+u.size(4.5),py=y+u.size(16),pw=u.size(28),ph=u.size(36);String portrait=card.portrait().equals("politics/empty/leader")?"commander/hud/army_unknown/frame_0":card.portrait();UiAssets.drawContent(g,portrait,px,py,pw,ph);UiAssets.draw(g,source(group?"group_leader_portrait_frame_badge":"unit_leader_portrait_frame_badge"),x+u.size(2.5),y+u.size(13.5),u.size(33),u.size(40));
        String assign=UiCommandLabels.text("CLICK_TO_ASSIGN","");out.add(new Hit(px,py,pw,ph,panel==null||panel.commander().isEmpty()?panelTooltip(panel,"commander"):leaderTooltip(card),()->MapCommanderHud.open(new Open(group?Mode.ASSIGN_GROUP_MARSHAL:Mode.ASSIGN_ARMY,card.id())),null,"ui.click","army_portrait"));
        if(panel!=null&&panel.leader()!=null){var leader=panel.leader();leftLabel(g,leader.role(),x+u.size(35),y+u.size(19),u.size(66.5),u.size(9),UiMenuStyle.TEXT);leftLabel(g,card.leader(),x+u.size(35),y+u.size(27),u.size(66.5),u.size(9),UiMenuStyle.TEXT);
            leaderSkill(g,u,leader.skill(),x+u.size(99),y+u.size(18.5));
            for(int n=0;n<4;n++){float sx=x+u.length(new double[]{109,117.5,126.5,135}[n]);g.pose().pushMatrix();g.pose().translate(sx,y+u.size(19.5));UiAssets.draw(g,source("trait_"+List.of("attack","defense","planning","logistics").get(n)),0,0,u.size(13.5),u.size(12.5));g.pose().popMatrix();leaderAbility(g,u,leader.abilities().get(n),sx+u.length(6.75),y+u.length(30));out.add(new Hit((int)sx,y+u.size(19),u.size(9),u.size(20),leader.abilityTooltips().size()>n?leader.abilityTooltips().get(n):card.detail(),()->{},null,""));}
            int traitsX=x+u.size(150.5),traitsWidth=w-u.size(group?10:12)-u.size(157.5),traitWidth=u.size(10.5);for(int n=0;n<leader.traitDetails().size();n++){var trait=leader.traitDetails().get(n);int tx=dev.hoi.client.ui.UiFlagStack.left(n,leader.traitDetails().size(),traitsX,traitsWidth,traitWidth),visible=dev.hoi.client.ui.UiFlagStack.visibleWidth(n,leader.traitDetails().size(),traitsX,traitsWidth,traitWidth);UiAssets.drawTinted(g,trait.icon(),tx,y+u.size(21),traitWidth,u.size(16),Boolean.FALSE.equals(trait.active())?0xffbf5050:0xffffffff);out.add(new Hit(tx,y+u.size(21),visible,u.size(16),trait.tooltip(),()->MapCommanderHud.open(new Open(Mode.COMMANDER_TRAITS,"army/"+panel.commander())),null,"ui.click","army_trait"));}
        }else {leftLabel(g,commanderName(card),x+u.size(35),y+u.size(19),u.size(66.5),u.size(19),0xffe15b5b);if(!assign.isEmpty())wrappedLabel(g,u,assign,x+u.size(2.5),y+u.size(13.5),u.size(33),u.size(40),true);}
        UiAssets.draw(g,source("leader_exp_bg"),px,py+ph-u.size(2),pw,u.size(2));
        progress(g,"leader_exp_fg_mid",px,py+ph-u.size(2),pw,u.size(2),panel==null?null:panel.experience(),0xffffffff,0xffffffff);
        if(panel==null||panel.controls().stream().noneMatch(control->control.id().equals("LEADER_TRAITS"))){int cx=x+u.size(2),cy=y+u.size(40.5),cw=u.size(12),ch=u.size(12.5);UiAssets.drawLeaderControl(g,"commander/commands/leader_details_icon/frame_0",cx,cy,cw,ch,false);out.add(0,new Hit(cx,cy,cw,ch,UiCommandLabels.text("OPEN_LEADER_DETAILS",""),()->{},null,"","army_leader_details"));}
        if(panel!=null)for(var control:panel.controls())if(Set.of("LEADER_TRAITS","LEADER_ADVISOR").contains(control.id())){boolean traits=control.id().equals("LEADER_TRAITS");int cx=x+u.size(traits?2:19),cy=y+u.size(40.5),cw=u.size(traits?(control.icon().contains("available_trait")?11:12):14),ch=u.size(traits?12.5:14);UiAssets.drawLeaderControl(g,control.icon(),cx,cy,cw,ch,control.enabled());out.add(0,new Hit(cx,cy,cw,ch,traits?UiCommandLabels.text("OPEN_LEADER_DETAILS",""):control.tooltip(),()->{if(control.enabled())MapCommanderHud.open(control.request());},null,control.enabled()?"ui.click":"",traits?"army_leader_details":"army_leader_advisor"));}
        UiAssets.nineSlice(g,source("tiled_header_1"),x+u.size(31),y+u.size(35),u.size(10.5),u.size(18),2,u.scale()*.5);
        verticalProgress(g,"army_badge_bar_org",x+u.size(33),y+u.size(37.5),u.size(3),u.size(13),panel==null?null:panel.organization());
        verticalProgress(g,"army_badge_bar_str",x+u.size(36.5),y+u.size(37.5),u.size(3),u.size(13),panel==null?null:panel.strength());
        out.add(new Hit(x+u.size(33),y+u.size(37.5),u.size(6.5),u.size(13),panelTooltip(panel,"averages"),()->{},null,"","army_averages"));
        if(panel!=null){double cx=x+u.size(39.5);var font=Minecraft.getInstance().font;int index=0;for(var entry:summaryTypes(card)){double cell=summaryCell(entry.getValue(),u);UiAssets.draw(g,armyUnitIcon(entry.getKey()),(int)Math.round(cx+(cell-u.size(13.5))/2.),y+u.size(37.5),u.size(13.5),u.size(7.5));UiText.centeredAt(g,font,entry.getValue().toString(),(int)Math.round(cx+cell/2),y+u.size(45),UiMenuStyle.TEXT);if(!panel.templateSummaries().isEmpty()){var summary=panel.templateSummaries().get(index);out.add(new Hit((int)Math.round(cx),y+u.size(37.5),(int)Math.ceil(cell),u.size(16),summary.tooltip(),()->{String context=templateCard.isEmpty()?(selected.size()==1?selected.iterator().next():card.id()):templateCard;MapUnitHud.selectDivisions(summary.divisionIds(),shift());templateCard=context;},null,"ui.division.select","template_summary"));}index++;cx+=cell;}}
        int buttonsStart=out.size();
        nativeButton(g,out,"select_all_armylist_short_btn",x+extra+u.size(group?152.5:132),y+u.size(38.5),u.size(26),u.size(11.5),UiCommandLabels.text("SELECT_ALL",""),()->select(card.id(),false));
        nativeButton(g,out,"motorization_level_toggle_btn",x+extra+u.size(group?175:154.5),y+u.size(38.5),u.size(22),u.size(11.5),UiCommandLabels.text("ARMY_MOTORIZATION_PRIORITY","")+(panel==null||panel.motorizationPriority()==null?"\n\n차량 수송 우선순위 자료가 없습니다.":""),()->{if(!group&&panel!=null&&panel.motorizationPriority()!=null)MapCommanderOrders.send("ARMY_MOTORIZATION",List.of(card.id()),List.of());});
        UiAssets.draw(g,source("motorization_level_toggle_strip").replace("frame_0","frame_"+(panel==null||panel.motorizationPriority()==null?0:panel.motorizationPriority())),x+extra+u.size(group?178.5:158),y+u.size(38.5),u.size(15),u.size(11.5));
        nativeButton(g,out,group?"army_group_toggle_armies_divisions_btn":"railway_gun_toggle_btn",x+extra+u.size(group?193.5:174),y+u.size(38.5),u.size(group?20.5:22),u.size(11.5),group?UiCommandLabels.text("TOGGLE_ARMY_GROUP_VIEW_ARMIES_DIVISIONS",""):UiCommandLabels.text("RAILWAY_GUN_CLICK_TO_SELECT_ASSIGNED",""),()->{if(group){if(!armyListGroups.remove(card.id()))armyListGroups.add(card.id());scroll=0;hits=List.of();}else MapCommanderOrders.message("철도포 배속은 아직 지원하지 않습니다.");});
        if(!group)nativeButton(g,out,"army_select_wings_btn",x+extra+u.size(193.5),y+u.size(38.5),u.size(22),u.size(11.5),UiCommandLabels.text("SELECT_ATTACHED_AIRWINGS","부속된 비행단 선택"),()->showWings(current.wings().stream().filter(wing->wing.data()!=null&&wing.data().attachedArmy().equals(card.id())).map(Member::id).toList()));
        for(int n=buttonsStart;n<out.size()-1;n++){var hit=out.get(n);out.set(n,new Hit(hit.x(),hit.y(),Math.min(hit.w(),out.get(n+1).x()-hit.x()),hit.h(),hit.tip(),hit.action(),hit.gesture(),hit.sound(),hit.icon()));}
    }
    private static List<Map.Entry<String,Integer>> summaryTypes(Card card){return card.panel()==null?List.of():!card.panel().templateSummaries().isEmpty()?card.panel().templateSummaries().stream().limit(11).map(t->Map.entry(t.icon(),t.count())).toList():card.panel().divisionTypes().entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).limit(11).toList();}
    private static void dragStack(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> cards,int bottom){
        int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),top=y,h=u.size(55),step=u.size(52),count=Math.max(1,(bottom-y-u.size(3))/step);scroll=Math.clamp(scroll,0,Math.max(0,cards.size()-count));
        for(var card:cards.subList(scroll,Math.min(cards.size(),scroll+count))){int width=u.size(218.5)+summaryExtra(card,u);armyHeader(g,u,out,card,x,y,width,h);y+=step;}
        armyScrollbar(g,u,out,x+u.size(219)+cards.stream().mapToInt(c->summaryExtra(c,u)).max().orElse(0),top,u.size(8),count*step,cards.size(),count);out.add(new Hit(x,top,u.size(218.5),y-top,"",()->{},null,""));
    }
    private static void armyOverview(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Card group,int bottom){
        var armies=new ArrayList<Card>();armies.add(group);current.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&c.panel()!=null&&c.panel().parent().equals(group.id())).forEach(armies::add);
        int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),top=y,h=u.size(55),count=Math.max(1,(bottom-y)/h);scroll=Math.clamp(scroll,0,Math.max(0,armies.size()-count));
        for(var army:armies.subList(scroll,Math.min(armies.size(),scroll+count))){armyHeader(g,u,out,army,x,y,u.size(218.5)+summaryExtra(army,u),h);y+=h;}
        armyScrollbar(g,u,out,x+u.size(219),top,u.size(9),count*h,armies.size(),count);
    }
    private static String leaderTooltip(Card card){
        var panel=card.panel();if(panel==null||panel.leaderEnRouteDays()==null||panel.leaderEnRouteDays()==0)return card.detail();
        return UiCommandLabels.text(panel.leaderEnRouteDays()==1?"UNITLEADER_IS_EN_ROUTE_SINGULAR":"UNITLEADER_IS_EN_ROUTE","").replace("$NAME|H$","§H"+card.leader()+"§!").replace("$DAYS|H$","§H"+panel.leaderEnRouteDays()+"§!")+"\n\n"+card.detail();
    }
    private static void armyHeadquarters(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Card card,int x,int y,int w){
        UiAssets.nineSlice(g,source("army_hq_bg"),x-u.size(.5),y,w-u.size(2),u.size(22.5),4,u.scale()*.5);
        var hq=card.panel()==null?null:card.panel().headquarters();boolean absent=hq==null?card.panel()!=null&&card.panel().commander().isEmpty():hq.state().equals("NO_COMMANDER");
        int bh=u.size(21);var font=Minecraft.getInstance().font;
        String label=hq==null?UiCommandLabels.text("DEPLOY_HQ_BUTTON_LABEL",""):hq.label(),text=hq==null?(absent?UiCommandLabels.text("DEPLOY_HQ_BUTTON_NO_COMMANDER",""):""):hq.text();
        int textWidth=(int)Math.ceil(font.width(absent?UiTooltips.officerText(text).copy().withStyle(net.minecraft.ChatFormatting.BOLD):UiTooltips.officerText(text))*UiText.scale(font));
        int bw=Math.max(u.size(155),u.size(absent?60:123)+textWidth);
        int darkLeft=x-u.size(.5)+u.size(2)+(int)Math.round((w-u.size(6))/425.0),darkRight=x-u.size(.5)+u.size(2)+(int)Math.round((w-u.size(6))*393.0/425);
        int bx=darkLeft+(darkRight-darkLeft-bw)/2,by=y+(u.size(22.5)-bh)/2,frameY=by+u.size(2);
        if(absent)UiAssets.drawGrayscale(g,source("deploy_hq_button").replace("frame_0","frame_1"),bx,frameY,bw,bh);
        else{var texture=net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+source("deploy_hq_button").replace("frame_0","frame_1")+".png");int left=u.size(55),right=u.size(4);g.blit(texture,bx,frameY,bx+left,frameY+bh,0,110f/310,0,1);g.blit(texture,bx+left,frameY,bx+bw-right,frameY+bh,110f/310,302f/310,0,1);g.blit(texture,bx+bw-right,frameY,bx+bw,frameY+bh,302f/310,1,0,1);}
        UiText.centeredSingleLine(g,font,UiTooltips.officerText(label).copy().withStyle(net.minecraft.ChatFormatting.BOLD),bx,by,u.size(55),bh,UiMenuStyle.TEXT);
        if(absent)UiText.centeredSingleLine(g,font,UiTooltips.officerText(text).copy().withStyle(net.minecraft.ChatFormatting.BOLD),bx+u.size(55),by,bw-u.size(55),bh,0xffdf4545);
        else if(hq!=null){
            String cost=hq.cost()==null?"":java.math.BigDecimal.valueOf(hq.cost()).stripTrailingZeros().toPlainString();
            int costWidth=(int)Math.ceil(font.width(cost)*UiText.scale(font)),contentWidth=u.size(21)+textWidth+(cost.isEmpty()?0:u.size(14)+costWidth),contentX=bx+u.size(55)+(bw-u.size(59)-contentWidth)/2;
            UiAssets.draw(g,armyUnitIcon("unit_hud/infantry"),contentX-u.size(10.5),by,u.size(38),bh);
            leftLabel(g,text,contentX+u.size(21),by,textWidth,bh,UiMenuStyle.TEXT);
            if(hq.cost()!=null){int cx=contentX+u.size(24)+textWidth;UiAssets.drawTinted(g,"hud/command_power",cx,by+u.size(5),u.size(11),u.size(11),Boolean.FALSE.equals(hq.affordable())?0xffdf4545:0xffffffff);leftLabel(g,cost,cx+u.size(11),by,costWidth,bh,Boolean.FALSE.equals(hq.affordable())?0xffdf4545:UiMenuStyle.TEXT);}
        }
        out.add(new Hit(bx,frameY,bw,bh,hq==null?(absent?text:"육군 사령부 배치 정보를 기다리고 있습니다."):hq.tooltip(),()->{},null,"","deploy_hq_button"));
    }
    private static void templateButton(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y){
        int bx=x+u.size(18.5),by=y+u.size(3.5),size=u.size(15.5);var selection=selectedDivisions();String tip=UiCommandLabels.text("CHANGE_TEMPLATE_UNIT","");
        String block=selectedDivisionRows().stream().filter(m->m.data()!=null).map(m->m.data().templateBlock()).filter(s->!s.isEmpty()).findFirst().orElse("");
        if(selection.isEmpty()||!block.isEmpty()){UiAssets.drawGrayscale(g,source("change_template_button"),bx,by,size,size);out.add(new Hit(bx,by,size,size,block.isEmpty()?tip:block,()->{},null,"","change_template_button"));return;}
        nativeButton(g,out,"change_template_button",bx,by,size,size,tip,()->MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,"DIVISIONS_TEMPLATE",selection)));
    }
    private static void disbandButton(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y){
        int bx=x+u.size(4.5),by=y+u.size(3.5),size=u.size(15.5);var selection=selectedDivisions();String tip=UiCommandLabels.text("DISBAND_ALL_UNIT","");
        if(selection.isEmpty()){UiAssets.drawGrayscale(g,source("nv_btn_delete"),bx,by,size,size);out.add(new Hit(bx,by,size,size,tip,()->{},null,"","nv_btn_delete"));return;}
        UiAssets.draw(g,source("nv_btn_delete"),bx,by,size,size);out.add(new Hit(bx,by,size,size,tip,()->MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,"DIVISIONS_DISBAND",selection)),null,"ui.disband","nv_btn_delete"));
    }
    private static double summaryCell(int count,UiUnits u){var font=Minecraft.getInstance().font;return Math.max(u.size(13.5),font.width(Integer.toString(count))*UiText.scale(font)+4);}
    private static int summaryExtra(Card card,UiUnits u){return current==null?0:current.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY||c.role()==Mode.ARMY_GROUP).mapToInt(c->Math.max(0,(int)Math.ceil(summaryTypes(c).stream().mapToDouble(e->summaryCell(e.getValue(),u)).sum())-u.size(c.role()==Mode.ARMY_GROUP?110:90))).max().orElse(0);}
    private static void selectionBackground(GuiGraphicsExtractor g,UiUnits u,int x,int y,int w){UiAssets.nineSlice(g,source("unit_list_header"),x-u.size(.5),y+u.size(3.5),w-u.size(2.5),u.size(17.5),4,u.scale()*.5);}
    private static void armyColumns(GuiGraphicsExtractor g,UiUnits u,int x,int y,int w){int extra=w-u.size(209.5);int[] edges={0,19,38,95,121,141,171,333,357,388};for(int n=0;n<edges.length-1;n++){int bx=x+u.size(.5+edges[n]/2.0)+(n>=7?extra:0),ex=x+u.size(.5+edges[n+1]/2.0)+(n>=6?extra:0),iw=u.size(9.5);UiAssets.nineSlice(g,source("tiling_sort_button_thin"),bx,y,ex-bx,u.size(10.5),2,u.scale()*.5);UiAssets.draw(g,source("division_list_column_button").replace("frame_0","frame_"+(n==0?2:0)),bx+(ex-bx-iw)/2,y,iw,u.size(10.5));}}
    private record ArmyEntry(Card card,Member member) {}
    private static void armyStack(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> chosen,int bottom){
        var entries=new ArrayList<ArmyEntry>();var shown=new HashSet<String>();for(var card:chosen){entries.add(new ArmyEntry(card,null));if(MapCommanderHud.groups.rowsVisible(card.id()))armyRows(card).stream().filter(m->shown.add(m.id())).forEach(m->entries.add(new ArmyEntry(card,m)));}
        int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),top=y,w=u.size(218.5),row=u.size(16.5);int count=Math.max(1,(bottom-y-u.size(18))/row);scroll=Math.clamp(scroll,0,Math.max(0,entries.size()-count));
        for(var entry:entries.subList(scroll,Math.min(entries.size(),scroll+count))){if(entry.member()!=null)member(g,u,out,entry.member(),x,y,w,row,"uv_unit_entry_bg");else{background(g,"army_bg",x,y,w,row);label(g,entry.card().name(),x+u.size(4),y,w-u.size(24),row,UiMenuStyle.TEXT);button(g,out,"×","nv_btn_close",x+w-u.size(15),y,u.size(13),row,"닫기",()->closeCard(entry.card()));}y+=row;}
        pager(g,u,out,x,y,w,scroll>0,scroll+count<entries.size(),count);out.add(new Hit(x,top,w,y-top,"",()->{},null,""));
    }
    private static void divisionActions(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y){
        int consolidateX=x+u.size(118.5),consolidateY=y+u.size(3.5),consolidateSize=u.size(15.5);UiAssets.drawGrayscale(g,source("consolidate"),consolidateX,consolidateY,consolidateSize,consolidateSize);out.add(new Hit(consolidateX,consolidateY,consolidateSize,consolidateSize,UiCommandLabels.text("CONSOLIDATE_NOT_POSSIBLE","")+"\n\n"+UiCommandLabels.text("CONSOLIDATE_NOT_POSSIBLE_TOOLTIP","")+"\n\n"+UiCommandLabels.text(selectedDivisions().size()<2?"CONSOLIDATE_NOT_POSSIBLE_NOT_MULTIPLE_UNITS":"CONSOLIDATE_NOT_POSSIBLE_FOR_SOME_UNITS",""),()->{},null,""));
        {
            var unitIds=new HashSet<>(selectedDivisions());var rows=current.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).filter(m->unitIds.contains("unit/"+m.id())).toList();var states=rows.stream().filter(m->m.data()!=null&&m.data().strategic()!=null).map(m->m.data().strategic()).distinct().toList();boolean known=rows.stream().map(Member::id).distinct().count()==unitIds.size()&&rows.stream().allMatch(m->m.data()!=null&&m.data().strategic()!=null);Boolean state=known&&states.size()==1?states.getFirst():null;
            String title=state==null?"전략적 재배치 모드 선택":UiCommandLabels.text(state?"SWITCH_STRATEGIC_REDEPLOYMENT_MODE_OFF":"SWITCH_STRATEGIC_REDEPLOYMENT_MODE_ON","");String tip=title+"\n\n"+UiCommandLabels.text("SWITCH_STRATEGIC_REDEPLOYMENT_MODE_DETAILED","")+"\n"+UiCommandLabels.text("SWITCH_STRATEGIC_REDEPLOYMENT_MODE_DETAILED_2","");
            if(state!=null){String key=state?"strategic_on":"strategic_off";tip=current.cards().stream().filter(c->c.panel()!=null&&c.panel().members().stream().anyMatch(m->unitIds.contains("unit/"+m.id()))).map(c->panelTooltip(c.panel(),key)).filter(t->!t.isEmpty()).distinct().collect(java.util.stream.Collectors.joining("\n\n"));}
            int sx=x+u.size(103),sy=y+u.size(3.5);UiAssets.draw(g,source("redeployment_type_icon").replace("frame_0",Boolean.TRUE.equals(state)?"frame_1":"frame_0"),sx,sy,u.size(15.5),u.size(15.5));out.add(new Hit(sx,sy,u.size(15.5),u.size(15.5),tip,()->landSettings("STRATEGIC_UNITS"),null,"ui.click","division_strategic"));
        }
        button(g,out,"","select_half",x+u.size(164.5),y+u.size(3.5),u.size(15.5),u.size(15.5),UiCommandLabels.text("SELECT_HALF_DIVISIONS","절반 선택"),()->{var ids=selectedDivisions();if(ids.size()>1)MapUnitHud.selectDivisions(ids.subList(0,(ids.size()+1)/2).stream().map(id->id.substring(5)).toList(),false);});
        button(g,out,"","hold",x+u.size(180),y+u.size(3.5),u.size(15.5),u.size(15.5),UiCommandLabels.text("HALT_UNIT","선택한 모든 부대 정지"),MapCommanderPanels::haltSelected);
    }
    private static void armySelection(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int bottom){
        var rows=selectedDivisionRows();int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),top=y,w=u.size(218.5),row=u.size(16.5);
        boolean unassigned=!rows.isEmpty()&&rows.stream().allMatch(m->m.data()!=null&&m.data().attachedArmy().isEmpty());
        if(!unassigned){background(g,"army_bg",x,y,w,row);label(g,rows.size()+"개 사단 선택됨",x,y,w-u.size(18),row,UiMenuStyle.TEXT);button(g,out,"×","nv_btn_close",x+w-u.size(15),y,u.size(13),row,"선택 해제",MapCommanderPanels::close);y+=row;}
        String description=unassigned?UiCommandLabels.text("UNASSIGNED_UNIT_DESCRIPTION",""):"";int footer=description.isEmpty()?0:UiTooltips.split(Minecraft.getInstance().font,UiTooltips.officerText(description),u.size(185)).size()*Minecraft.getInstance().font.lineHeight+u.size(12);
        int count=Math.min(rows.stream().anyMatch(m->m.data()!=null&&m.data().attachedArmy().isEmpty())?11:Integer.MAX_VALUE,Math.max(1,(bottom-y-u.size(34.5)-footer)/row));
        background(g,"air_priorities_tiled_bg",x,y,w,u.size(31.5)+Math.min(count,rows.size())*row+footer+u.size(4));
        if(unassigned){
            selectionBackground(g,u,x,y,w);
            leftLabel(g,UiCommandLabels.text("UNITS_VIEW_DIVISIONS_COUNT","").replace("$DIV|G$","§G"+rows.size()+"§!"),x+u.size(37.5),y,u.size(65),u.size(21),UiMenuStyle.TEXT);
            disbandButton(g,u,out,x-u.size(4),y);
            templateButton(g,u,out,x-u.size(4),y);
            String weight=rows.stream().allMatch(m->m.data()!=null&&m.data().transportWeight()!=null)?Long.toString(Math.round(rows.stream().mapToDouble(m->m.data().transportWeight()).sum())):"0";
            UiAssets.draw(g,source("generic_mini_bg"),x+u.size(134.5),y+u.size(5.5),u.size(34),u.size(13));UiAssets.drawContent(g,source("topbar_convoys"),x+u.size(135.5),y+u.size(7),u.size(9),u.size(9));leftLabel(g,weight,x+u.size(145),y+u.size(6),u.size(18.5),u.size(11.5),UiMenuStyle.TEXT);
            nativeButton(g,out,"nv_btn_close",x+w-u.size(18),y+u.size(3.5),u.size(15.5),u.size(15.5),UiCommandLabels.text("CLOSE","닫기"),MapCommanderPanels::close);
        }
        divisionActions(g,u,out,x+w-u.size(218.5),y);y+=u.size(21);armyColumns(g,u,x,y,w);y+=u.size(10.5);
        int listTop=y;scroll=Math.clamp(scroll,0,Math.max(0,rows.size()-count));for(var member:rows.subList(scroll,Math.min(rows.size(),scroll+count))){member(g,u,out,member,x,y,w-u.size(15),row,"uv_unit_entry_bg");y+=row;}armyScrollbar(g,u,out,x+w-u.size(14),listTop,u.size(9),count*row,rows.size(),count);
        g.pose().pushMatrix();g.pose().translate(0,-u.length(.5));g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+source("tiled_window_1b_thin_border")+".png"),x+u.size(1),y,x+w-u.size(2),y+u.size(3),95/190f,96/190f,179/190f,185/190f);g.pose().popMatrix();y+=u.size(3);
        if(!description.isEmpty()){UiAssets.nineSlice(g,source("unitlist_unassigned_bg"),x+u.size(2.5),y,w-u.size(7.5),footer,4,u.scale()*.5);wrappedLabel(g,u,description,x+u.size(9),y+u.size(2),u.size(185),footer-u.size(4),true);y+=footer;}
        out.add(new Hit(x,top,w,y-top,"",()->{},null,""));
    }
    private record FleetEntry(Card card,Member member) {}
    private static void navyStack(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> chosen,int bottom) {
        var entries=new ArrayList<FleetEntry>();for(var card:chosen){entries.add(new FleetEntry(card,null));if(card.panel()!=null&&MapCommanderHud.groups.rowsVisible(card.id()))card.panel().members().forEach(m->entries.add(new FleetEntry(card,m)));}
        int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),w=u.size(246.5),top=y;scroll=Math.clamp(scroll,0,Math.max(0,entries.size()-1));int index=scroll;
        for(;index<entries.size();index++) {
            var entry=entries.get(index);int h=u.size(entry.member()==null?83:62);if(entry.member()==null&&index==scroll)h+=u.size(22);if(y+h>bottom)break;
            if(entry.member()==null){navyHeader(g,u,out,entry.card(),x,y,w,u.size(83));if(index==scroll)navalTools(g,u,out,x,y+u.size(83),w);}
            else member(g,u,out,entry.member(),x,y,w,h,"task_force_bg");y+=h;
        }
        armyScrollbar(g,u,out,x+w-u.size(8),top,u.size(8),y-top,entries.size(),Math.max(1,index-scroll));out.add(new Hit(x,top,w,Math.max(1,y-top),"",()->{},null,""));
    }
    static void closeCard(Card card){if(dragArmySelection&&current!=null&&current.branch()==Mode.ARMY){var ids=new HashSet<String>();for(var army:current.cards())if(army.role()==Mode.ASSIGN_ARMY&&army.panel()!=null&&(army.id().equals(card.id())||card.role()==Mode.ARMY_GROUP&&army.panel().parent().equals(card.id()))&&army.panel().divisionIds()!=null)ids.addAll(army.panel().divisionIds());MapUnitHud.removeDivisions(ids);mapSelectionChanged(true);scroll=0;MapCommanderOrders.selectionScopeChanged();return;}if(templateCard.equals(card.id())){templateCard="";hits=List.of();}if(!selected.remove(card.id()))return;if(card.panel()!=null)card.panel().members().forEach(member->selectedForces.remove(member.id()));if(current!=null&&current.branch()==Mode.ARMY){fullArmyCards.remove(card.id());retainManualArmySelection();syncArmySelection();}scroll=0;hits=List.of();MapCommanderOrders.selectionChanged();MapCommanderOrders.selectionScopeChanged();}
    private static void navyHeader(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Card card,int x,int y,int w,int h){
        background(g,"fleet_bg",x,y,w,h);if(selected.contains(card.id())){g.pose().pushMatrix();g.pose().translate(x-u.size(1),y-u.size(1));g.pose().scale(u.scale()*.5f);UiAssets.draw(g,source("fleet_bg_hl"),0,0,497,170);g.pose().popMatrix();}UiAssets.drawTinted(g,source("theatre_army_shield").replace("frame_0","frame_"+(card.panel()==null||card.panel().insignia()==null?3:card.panel().insignia())),x+u.size(3),y+u.size(3),u.size(16.5),u.size(16),fleetColor(card));label(g,UiText.ellipsize(Minecraft.getInstance().font,card.name(),(int)(u.size(113)/UiText.scale(Minecraft.getInstance().font))),x+u.size(30),y+u.size(4),u.size(113),u.size(11.5),UiMenuStyle.TEXT);label(g,MapCommanderHud.countLabel(card),x+u.size(160),y+u.size(4),u.size(45),u.size(11.5),UiMenuStyle.TEXT);
        button(g,out,"×","nv_btn_close",x+u.size(227),y+u.size(2.5),u.size(13.5),u.size(13.5),UiCommandLabels.text("CLOSE","닫기"),()->closeCard(card));
        var panel=card.panel();String portrait=card.portrait().equals("politics/empty/leader")?"commander/hud/army_unknown/frame_0":card.portrait();int px=x+u.size(1),py=y+u.size(25.5),pw=u.size(30),ph=u.size(41.5);g.pose().pushMatrix();g.pose().translate(px,py);g.pose().scale(u.scale()*.195f);UiAssets.drawContent(g,portrait,0,0,156,210);g.pose().popMatrix();g.pose().pushMatrix();g.pose().translate(x-u.size(1),y+u.size(22));g.pose().scale(u.scale()*.5f);UiAssets.draw(g,source("naval_unit_leader_frame"),0,0,69,95);g.pose().popMatrix();
        String assign=UiCommandLabels.text("CLICK_TO_ASSIGN","");out.add(new Hit(px,py,pw,ph,panel==null||panel.commander().isEmpty()?assign:leaderTooltip(card),()->MapCommanderHud.open(new Open(Mode.NAVY_FLEET,card.id())),null,"ui.click","navy_portrait"));
        if(panel==null)return;
        button(g,out,UiCommandLabels.text("SELECT_ALL","모두 선택"),"select_all_armylist_btn",x+u.size(177),y+u.size(71),u.size(65),u.size(12),UiCommandLabels.text("SELECT_ALL","모두 선택"),()->selectFleetForces(card));label(g,UiCommandLabels.text("SELECT_ALL","모두 선택"),x+u.size(177),y+u.size(71),u.size(65),u.size(12),UiMenuStyle.TEXT);
        if(panel.leader()!=null){var leader=panel.leader();leftLabel(g,leader.role(),x+u.size(37.5),y+u.size(25),u.size(57),u.size(9),UiMenuStyle.TEXT);leftLabel(g,card.leader(),x+u.size(37.5),y+u.size(33),u.size(57),u.size(9),UiMenuStyle.TEXT);
            int lineY=y+u.size(23);float statsTop=lineY-u.length(.5);
            var statsTexture=net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+source("fleet_bg")+".png");g.blit(statsTexture,x+u.size(96.5),lineY,x+u.size(148.5),y+u.size(44),300/493f,404/493f,46/166f,88/166f);g.pose().pushMatrix();g.pose().translate(x+u.size(96.5),statsTop);g.blit(statsTexture,0,0,u.size(48),u.size(17),226/493f,296/493f,47/166f,82/166f);g.pose().popMatrix();leaderSkill(g,u,leader.skill(),x+u.size(96.5),statsTop);
            for(int n=0;n<4;n++){int sx=x+u.size(106+n*9);g.pose().pushMatrix();g.pose().translate(sx,statsTop);g.pose().scale(u.scale()*.5f);UiAssets.draw(g,source("trait_"+List.of("attack","defense","maneuvering","coordination").get(n)),0,0,27,25);g.pose().popMatrix();leaderAbility(g,u,leader.abilities().get(n),sx+u.length(6.75),statsTop+u.length(12));out.add(new Hit(sx,(int)Math.floor(statsTop),u.size(9),u.size(20),leader.abilityTooltips().size()>n?leader.abilityTooltips().get(n):card.detail(),()->{},null,"","navy_ability"));}
            int count=leader.traitDetails().size();float tx=x+u.length(148.5),ty=lineY+u.length(2.5),tw=u.length(11.5),pixel=(float)Minecraft.getInstance().getWindow().getGuiScale(),step=count<2?0:Math.min(u.length(7.5),(u.length(57)-tw)/(count-1));step=(float)Math.floor(step*pixel)/pixel;
            for(int n=0;n<count;n++){var trait=leader.traitDetails().get(n);float left=tx+n*step;int hitLeft=(int)Math.ceil(left),right=(int)Math.ceil(n+1<count?tx+(n+1)*step:left+tw);g.pose().pushMatrix();g.pose().translate(left,ty);g.pose().scale(u.scale()*.5f);UiAssets.drawTinted(g,trait.icon(),0,0,23,33,Boolean.FALSE.equals(trait.active())?0xffbf5050:0xffffffff);g.pose().popMatrix();out.add(new Hit(hitLeft,(int)Math.ceil(ty),Math.max(1,right-hitLeft),u.size(16.5),trait.tooltip(),()->MapCommanderHud.open(new Open(Mode.COMMANDER_TRAITS,"navy/"+panel.commander())),null,"ui.click","navy_trait"));}
        }else{leftLabel(g,commanderName(card),x+u.size(37.5),y+u.size(25),u.size(70),u.size(19),0xffe15b5b);wrappedLabel(g,u,assign,px,py,pw,ph,true);}
        for(var control:panel.controls())if(Set.of("LEADER_TRAITS","LEADER_ADVISOR").contains(control.id())){boolean traits=control.id().equals("LEADER_TRAITS");int cx=x+u.size(traits?1:16),cy=y+u.size(57),cw=u.size(traits?(control.icon().contains("available_trait")?11:12):14),ch=u.size(traits?12.5:14);UiAssets.drawLeaderControl(g,control.icon(),cx,cy,cw,ch,control.enabled());out.add(0,new Hit(cx,cy,cw,ch,traits?UiCommandLabels.text("OPEN_LEADER_DETAILS",""):control.tooltip(),()->{if(control.enabled())MapCommanderHud.open(control.request());},null,"ui.click",traits?"navy_leader_details":"navy_leader_advisor"));}
        var fleet=panel.fleet();int hullIndex=0,bx=x+u.size(37.5);
        for(var entry:orderedHulls(fleetComposition(card))){bx=x+u.size(37.5+hullIndex++*23);String icon=entry.getKey();navalHullBars(g,u,bx+u.size(3.5),y+u.size(49.5),fleet==null?null:fleet.hullStrength().get(icon),fleet==null?null:fleet.hullOrganization().get(icon),fleet==null?null:fleet.hullAir().get(icon));UiAssets.draw(g,navalHullIcon(icon),bx+u.size(3.5),y+u.size(53.5),u.size(15),u.size(6));label(g,entry.getValue().toString(),bx+u.size(17),y+u.size(48.5),u.size(8),u.size(9),UiMenuStyle.TEXT);String tip=fleet==null?hullTooltip(icon,entry.getValue()):fleet.hullTooltips().getOrDefault(icon,hullTooltip(icon,entry.getValue()));out.add(new Hit(bx,y+u.size(45),u.size(23),u.size(20),tip,()->MapCommanderHud.open(units(card)),null,"ui.click","naval_hull"));bx+=u.size(23);}
        UiAssets.draw(g,source("fuel_extended_strip"),x+u.size(227.5),y+u.size(37.5),u.size(10),u.size(13));UiAssets.draw(g,source("mapicon_air_base").replace("frame_0","frame_1"),x+u.size(226.5),y+u.size(52.5),u.size(13),u.size(13));
        var insignia=panel.controls().stream().filter(c->c.id().equals("FLEET_INSIGNIA")).findFirst().orElse(null);int ix=x+u.size(242.5),iy=y+u.size(22.5),size=u.size(5.5),stripeHeight=u.size(42.5);g.enableScissor(ix,iy,ix+size,iy+stripeHeight);UiAssets.drawTinted(g,source("group_marker3"),ix-u.size(16.5),iy,u.size(22),u.size(44),fleetColor(card));g.disableScissor();
        if(insignia!=null)out.add(new Hit(ix,iy,size,stripeHeight,insignia.tooltip(),()->{if(insignia.enabled())MapCommanderHud.togglePalette(card.id());},null,"ui.click","naval_insignia"));
        if(fleet!=null){leftLabel(g,fleet.activeRegionsText(),x+u.size(8),y+u.size(70),u.size(86),u.size(11.5),UiMenuStyle.TEXT);UiAssets.draw(g,source("naval_automation_base"),x+u.size(100),y+u.size(67.5),u.size(15),u.size(15));out.add(new Hit(x+u.size(100),y+u.size(67.5),u.size(15),u.size(15),fleet.automateTooltip(),()->{},null,"","naval_automate"));UiAssets.draw(g,source("naval_deploy_button"),x+u.size(115),y+u.size(67.5),u.size(60),u.size(15));UiAssets.draw(g,source("deploy_port_fleet_icons"),x+u.size(115),y+u.size(67),u.size(15),u.size(15));leftLabel(g,fleet.homeBase(),x+u.size(130),y+u.size(70),u.size(45),u.size(12.5),UiMenuStyle.TEXT);out.add(new Hit(x+u.size(115),y+u.size(67.5),u.size(60),u.size(15),fleet.homeBaseTooltip(),()->{},null,"","naval_home_base"));}
    }
    private static void selectWing(Member member){if(shift()){var keys=MapUnitHud.selectedKeys();current.wings().stream().filter(wing->keys.contains(wing.selectionKey())).map(Member::id).forEach(selectedWings::add);}MapUnitHud.clearSelection();if(!shift())selectedWings.clear();if(!selectedWings.remove(member.id()))selectedWings.add(member.id());wingList=true;scroll=0;hits=List.of();postureMenu=false;MapCommanderOrders.selectionChanged();}
    private static void deselectWing(String id){showWings(current.wings().stream().filter(MapCommanderPanels::wingSelected).filter(member->!member.id().equals(id)).map(Member::id).toList());}
    private static void airMember(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Member member,int x,int y,int w,int h){
        var data=member.data().air();var wing=data.wing();background(g,"av_wing_entry_bg",x,y,w,h);if(selectedWings.contains(member.id()))UiAssets.draw(g,source("av_wing_entry_bg").replace("frame_0","frame_1"),x,y,w,h);
        String detail="§e"+member.name()+"§r\n"+member.detail();
        button(g,out,"","unitlist_unitinfo_button",x+u.size(5.5),y+u.size(3.5),u.size(31),u.size(15),detail,()->MapCommanderHud.open(member.request()));if(!UiAssets.drawContent(g,wing.equipmentIcon(),x+u.size(8),y+u.size(6),u.size(26),u.size(12)))UiAssets.drawContent(g,member.icon(),x+u.size(8),y+u.size(6),u.size(26),u.size(12));
        if(wing.carrier())UiAssets.draw(g,source("airwing_carrier_icon"),x+u.size(29),y+u.size(9.5),u.size(9),u.size(8));if(member.strength()>=0)nativeBar(g,"repair_hp",x+u.size(3),y+u.size(3),u.size(2),h-u.size(6),member.strength(),true);
        if(member.data().experienceLevel()!=null)UiAssets.draw(g,source("unit_level").replace("frame_0","frame_"+member.data().experienceLevel()),x+u.size(42.5),y+u.size(5),u.size(13.5),u.size(13.5));if(member.data().experienceProgress()!=null)nativeBar(g,"unit_xp_bar",x+u.size(56.5),y+u.size(3.5),u.size(1),u.size(15),member.data().experienceProgress(),true);
        UiAssets.draw(g,source("wing_reinforce_small_btn"),x+u.size(59),y+u.size(3.5),u.size(26),u.size(15));label(g,Integer.toString(member.data().quantity()),x+u.size(61),y+u.size(1.5),u.size(15),u.size(10),UiMenuStyle.TEXT);label(g,Integer.toString(wing.target()),x+u.size(61),y+u.size(11.5),u.size(15),u.size(10),UiMenuStyle.TEXT);
        current.theaters().stream().filter(theater->theater.wings().contains(member.id())).findFirst().ifPresent(theater->{int frame=List.of("LOW","NORMAL","HIGH").indexOf(theater.priority())+1;if(frame>0)UiAssets.draw(g,source("deploy_priority").replace("frame_0","frame_"+frame),x+u.size(77.5),y+u.size(3),u.size(6.5),u.size(6.5));});
        if(wing.efficiency()!=null){int frame=Math.min(2,(int)(wing.efficiency()*3));UiAssets.draw(g,source("air_mission_efficiency_strip").replace("frame_0","frame_"+frame),x+u.size(103),y+u.size(5),u.size(12),u.size(13));out.add(new Hit(x+u.size(103),y+u.size(5),u.size(12),u.size(13),member.detail(),()->{},null,"","air_mission_efficiency"));}
        String policy=airPolicyTooltip(data.reinforcementPolicy());int px=x+u.size(76.5),py=y+u.size(10.5),ps=u.size(11);airPolicyIcon(g,data.reinforcementPolicy(),px,py,ps,ps);out.add(new Hit(px,py,ps,ps,member.name()+"\n"+policy,()->MapCommanderHud.open(new Open(Mode.AIR_REINFORCEMENT,"policy",List.of(member.id()))),null,"ui.click","wing_prio_strip"));
        nativeButton(g,out,"ace_none",x+u.size(82.5),y-u.size(1.5),u.size(21),u.size(23),member.detail()+"\n\n"+UiCommandLabels.text("ACE_PILOT_ASSIGN_ACE","에이스 선택"),()->MapCommanderHud.open(new Open(Mode.AIR_REINFORCEMENT,"aces",List.of(member.id()))));
        airLabel(g,u,member.name(),x+u.size(116.5),y+u.size(3.5),u.size(60),u.size(9));airLabel(g,u,wing.missionState(),x+u.size(116.5),y+u.size(12),u.size(60),u.size(10));out.add(new Hit(x+u.size(116.5),y+u.size(12),u.size(60),u.size(10),wing.missionStateTooltip().isEmpty()?wing.missionState():wing.missionStateTooltip(),()->selectWing(member),null,"ui.division.select","air_mission_state"));
        for(var mission:AIR)if(mission[0].equals(member.mission())){UiAssets.drawContent(g,"commander/commands/strategicair_mission_types/frame_"+mission[1],x+u.size(170),y+u.size(2.5),u.size(12.5),u.size(8.5));break;}
        button(g,out,"","aggressivness_btn",x+u.size(226.5),y+u.size(3.5),u.size(15.5),u.size(15.5),UiCommandLabels.text("AGGRESSIVE_CLICK_TO","§G클릭§!하여 공격성 변경")+"\n\n공군 공격성 설정은 아직 지원하지 않습니다.",()->{},true);
        String timeKey="AIRWING_MISSION_"+(wing.sortieTime().equals("BOTH")?"DAY_NIGHT":wing.sortieTime());int tx=x+u.size(241.5),ty=y+u.size(3.5),ts=u.size(14.5);button(g,out,"","strategicair_day_night",tx,ty,ts,ts,member.name()+"\n"+UiCommandLabels.text(timeKey,"")+"\n\n클릭: 다음 출격 시간대",()->MapCommanderOrders.send("AIR_SETTING/SORTIE_NEXT",List.of(member.id(),"sortie/"+wing.sortieTime()),List.of()));UiAssets.drawContent(g,source("strategicair_day_night_icons").replace("frame_0","frame_"+List.of("DAY","NIGHT","BOTH").indexOf(wing.sortieTime())),tx,ty,ts,ts);
        button(g,out,"","nv_btn_close",x+u.size(258),y+u.size(3.5),u.size(14.5),u.size(14.5),"선택 해제 · "+member.name(),()->deselectWing(member.id()));out.add(new Hit(x,y,w,h,detail,()->selectWing(member),null,"ui.division.select"));
    }
    private static void member(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Member member,int x,int y,int w,int h,String asset) {
        if(member.request().mode()==Mode.AIR_MISSION&&member.data()!=null&&member.data().air()!=null&&member.data().air().wing()!=null){airMember(g,u,out,member,x,y,w,h);return;}
        if(member.request().mode()==Mode.ARMY_UNITS){armyMember(g,u,out,member,x,y,w,h,asset);return;}
        boolean naval=member.request().mode()==Mode.NAVAL_MISSION;
        background(g,asset,x,y,w,h);if(member.request().mode()==Mode.NAVAL_MISSION&&selectedForces.contains(member.id()))UiAssets.draw(g,source("task_force_bg_hl"),x,y,w,h);if(member.request().mode()==Mode.AIR_MISSION&&selectedWings.contains(member.id()))UiAssets.draw(g,source(asset).replace("frame_0","frame_1"),x,y,w,h);if(naval)UiAssets.drawTinted(g,member.icon(),x+u.size(4),y+u.size(2),u.size(16.5),u.size(16),fleetColor(fleetFor(member)));else UiAssets.drawContent(g,member.icon(),x+u.size(6),y+u.size(2),u.size(24),Math.min(h-u.size(4),u.size(24)));
        if(naval){label(g,Integer.toString(shipCount(member)),x+u.size(5),y+u.size(8),u.size(16),u.size(9),UiMenuStyle.TEXT);UiAssets.draw(g,source("task_force_mission_bg"),x+u.size(4),y+u.size(19),u.size(20),u.size(20));var navy=member.data()==null?null:member.data().navy();String activity=navalActivityIcon(member);UiAssets.draw(g,activity,x+u.size(3.5),y+u.size(21),u.size(21.5),u.size(17));}
        if(!naval&&member.strength()>=0)nativeBar(g,"repair_hp",x+u.size(2),y+u.size(2),u.size(3),h-u.size(4),member.strength(),true);
        boolean air=member.request().mode()==Mode.AIR_MISSION;
        if(naval)leftLabel(g,member.name(),x+u.size(35),y+u.size(4),u.size(139),u.size(11.5),UiMenuStyle.TEXT);else label(g,member.name(),x+u.size(33),y+u.size(1),w-u.size(48),Math.min(h,u.size(air?9:14)),UiMenuStyle.TEXT);
        if(naval){var navy=member.data()==null?null:member.data().navy();navalActivityLabel(g,u,navy==null||navy.activity().isEmpty()?member.status():navy.activity(),x+u.size(30),y+u.size(19),u.size(131),u.size(20),navy!=null&&navy.activityWarning()?0xffe15b5b:UiMenuStyle.TEXT);if(navy!=null)out.add(new Hit(x+u.size(3.5),y+u.size(19),u.size(157),u.size(20),navy.activityTooltip(),()->selectForce(member.id()),null,"ui.division.select","naval_activity"));}else if(h>u.size(20))label(g,member.status(),x+u.size(33),y+u.size(13),w-u.size(48),u.size(9),0xFFD5CA7D);
        if(member.organization()!=null&&member.request().mode()!=Mode.NAVAL_MISSION)nativeBar(g,"repair_org",x+u.size(30),y+h-u.size(5),u.size(75),u.size(3),member.organization(),false);
        if(naval&&!member.composition().isEmpty()){var navy=member.data()==null?null:member.data().navy();int cx=x+u.size(6);for(var entry:orderedHulls(member.composition())){navalHullBars(g,u,cx,y+h-u.size(23),navy==null?null:navy.hullStrength().get(entry.getKey()),navy==null?null:navy.hullOrganization().get(entry.getKey()),navy==null?null:navy.hullAir().get(entry.getKey()));UiAssets.draw(g,navalHullIcon(entry.getKey()),cx,y+h-u.size(18),u.size(15),u.size(6));label(g,entry.getValue().toString(),cx+u.size(14),y+h-u.size(23),u.size(10),u.size(9),UiMenuStyle.TEXT);UiAssets.draw(g,source("nv_h_divider"),cx+u.size(15),y+h-u.size(14),u.size(8),u.size(1.5));label(g,Integer.toString(hullTarget(member,entry.getKey(),entry.getValue())),cx+u.size(14),y+h-u.size(12),u.size(10),u.size(9),UiMenuStyle.TEXT);out.add(new Hit(cx,y+h-u.size(23),u.size(23),u.size(23),hullTooltip(entry.getKey(),entry.getValue()),()->selectForce(member.id()),null,"ui.division.select","naval_hull"));cx+=u.size(23);}}
        if(!naval&&!member.composition().isEmpty()){int cx=x+u.size(6);for(var entry:new TreeMap<>(member.composition()).entrySet()){UiAssets.draw(g,entry.getKey(),cx,y+h-u.size(21),u.size(18),u.size(9));label(g,entry.getValue().toString(),cx,y+h-u.size(12),u.size(18),u.size(9),UiMenuStyle.TEXT);out.add(new Hit(cx,y+h-u.size(21),u.size(18),u.size(18),hullTooltip(entry.getKey(),entry.getValue()),()->selectForce(member.id()),null,"ui.division.select"));cx+=u.size(23);}}
        if(air&&member.data()!=null&&!member.data().attachedArmy().isEmpty()){
            int ax=x+w-u.size(17),ay=y+u.size(3);UiAssets.draw(g,source("theatre_army_shield"),ax,ay,u.size(13.2),u.size(12.8));out.add(new Hit(ax,ay,u.size(15.5),u.size(15.5),UiCommandLabels.text("ATTACHED_AIR_WING_DELAYED_DESC","부속된 군 선택 · 부속 취소"),()->{},(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT){selectedWings.clear();wingList=false;MapUnitHud.clearSelection();select(member.data().attachedArmy(),false);}else if(button==InputConstants.MOUSE_BUTTON_RIGHT)MapCommanderHud.open(new Open(Mode.AIR_ATTACH,member.id(),List.of("action/detach")));},"ui.click"));
        }
        if(member.request().mode()==Mode.NAVAL_MISSION){
            var state=member.data()==null?null:member.data().navy();int dx=x+u.size(162.5),dy=y+u.size(25.5);UiAssets.draw(g,source("naval_dominance_support_strip").replace("frame_0",state!=null&&state.dominance()!=null&&state.dominance()>0?"frame_1":"frame_0"),dx,dy,u.size(12),u.size(7));out.add(new Hit(dx,dy,u.size(12),u.size(7),state==null?"":state.dominanceTooltip(),()->{},null,"","naval_dominance"));int fuelX=x+u.size(177.5),fuelY=y+u.size(23.5);UiAssets.draw(g,source("fuel_extended_strip"),fuelX,fuelY,u.size(10),u.size(13));out.add(new Hit(fuelX,fuelY,u.size(10),u.size(13),MapCommanderAirBases.fuelTooltip(current.country()),()->{},null,"","naval_fuel"));String[] icons={"nv_btn_repair_priority","nv_btn_toggle_split_for_repair","nv_btn_aggressiveness_small","nv_btn_composition_editor"},keys={"NAVY_REPAIR_POLICY_DESC","NAVY_REPAIR_POLICY_SPLIT_DESC","SHIP_ENGAGEMENT_BUTTON_AGGRESSIVENESS_GENERAL_DESC","TASK_FORCE_COMPOSITION_EDITOR_DESC"},tabs={"repair","","engagement","composition"};
            for(int n=0;n<icons.length;n++){String tab=tabs[n];int frame=n==1?(state!=null&&Boolean.TRUE.equals(state.autoSplit())?0:1):state==null?0:n==0&&state.repairPolicy()!=null?state.repairPolicy():n==2?List.of("NEVER","LOW","MEDIUM","HIGH","ALWAYS").indexOf(state.engagement()):0;String icon=source(icons[n]).replace("frame_0","frame_"+frame);int bx=x+u.size(n==0?209.5:n==1?194.5:n==2?224.5:225),by=y+u.size(n>=3?40:21.5);if(n<3)UiAssets.draw(g,source("nv_btn_base_partial_toggle"),bx,by,u.size(15.5),u.size(15.5));UiAssets.draw(g,icon,bx,by,u.size(15.5),u.size(15.5));String tip=UiCommandLabels.text(keys[n],List.of("수리 우선순위","자동 분리 수리","교전 규칙","태스크 포스 편성").get(n));if(state!=null){if(n==0)tip=state.repairPolicy()==null?"수리 우선순위 미설정\n\n"+tip:UiCommandLabels.text("NAVY_REPAIR_POLICY_"+state.repairPolicy(),tip);if(n==2)tip=UiCommandLabels.text("SHIP_ENGAGEMENT_BUTTON_AGGRESSIVENESS_"+(frame+1),state.engagement())+"\n\n"+UiCommandLabels.text("SHIP_ENGAGEMENT_BUTTON_AGGRESSIVENESS_"+(frame+1)+"_DESC","")+"\n\n"+tip;if(n==1){Boolean enabled=state.autoSplit();tip+=(enabled==null?"\n\n미설정":enabled?"\n\n활성화":"\n\n비활성화");}}if(n==0){Integer policy=state==null?null:state.repairPolicy();if(policy!=null){String next=UiCommandLabels.text("NAVY_REPAIR_POLICY_"+((policy+1)%4),"").split("\n",2)[0],previous=UiCommandLabels.text("NAVY_REPAIR_POLICY_"+((policy+3)%4),"").split("\n",2)[0];tip+="\n"+UiCommandLabels.text("NAVY_REPAIR_POLICY_CLICK_TO_CHANGE_TO","").replace("$LEVEL|$",next)+"\n"+UiCommandLabels.text("NAVY_REPAIR_POLICY_RIGHT_CLICK_TO_CHANGE_TO","").replace("$LEVEL|$",previous);}else tip+="\n\n수리 우선순위를 먼저 선택하세요.";out.add(new Hit(bx,by,u.size(15.5),u.size(15.5),tip,()->MapCommanderOrders.repairPolicy(member.id(),policy,false),(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT||button==InputConstants.MOUSE_BUTTON_RIGHT)MapCommanderOrders.repairPolicy(member.id(),policy,button==InputConstants.MOUSE_BUTTON_RIGHT);},"ui.click"));}else if(n==1||n==2){int setting=n;if(state==null)tip+="\n\n태스크 포스 설정을 불러오는 중입니다.";if(n==2)tip+="\n\n클릭: 다음 교전 규칙\n우클릭: 이전 교전 규칙";out.add(new Hit(bx,by,u.size(15.5),u.size(15.5),tip,()->navalSetting(member,setting,false),(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT||setting==2&&button==InputConstants.MOUSE_BUTTON_RIGHT)navalSetting(member,setting,button==InputConstants.MOUSE_BUTTON_RIGHT);},"ui.click"));}else out.add(new Hit(bx,by,u.size(15.5),u.size(15.5),tip,()->MapCommanderHud.open(new Open(Mode.NAVAL_SETTINGS,member.id(),tab.isEmpty()?List.of():List.of(tab)))));}
            button(g,out,"선택","nv_btn_select",x+u.size(194.5),y+u.size(1.5),u.size(33.5),u.size(15.5),"선택 · "+member.name(),()->selectForce(member.id()));label(g,"선택",x+u.size(194.5),y+u.size(1.5),u.size(33.5),u.size(15.5),UiMenuStyle.TEXT);
            button(g,out,"×","nv_btn_close",x+u.size(227),y+u.size(1.5),u.size(13.5),u.size(13.5),"선택 해제 · "+member.name(),()->deselectForce(member.id()));
            var fleet=current.cards().stream().filter(c->c.panel()!=null&&c.panel().members().stream().anyMatch(m->m.id().equals(member.id()))).findFirst().orElse(null);if(fleet!=null){int fx=x+u.size(178);UiAssets.draw(g,source("army_shield_bg"),fx,y+u.size(2.5),u.size(13.2),u.size(12.8));UiAssets.drawTinted(g,source(state!=null&&state.fleetInsignia()!=null&&state.fleetInsignia()>30?"theatre_army_shield_selected":"theatre_task_force_shield_selected").replace("frame_0","frame_"+(state==null||state.fleetInsignia()==null?3:state.fleetInsignia())),fx,y+u.size(2.5),u.size(13.2),u.size(12.8),0xffd8c97a);label(g,fleetShipCount(fleet)<0?"?":Integer.toString(fleetShipCount(fleet)),fx,y+u.size(8),u.size(13.2),u.size(8),UiMenuStyle.TEXT);}
            out.add(new Hit(x+u.size(4),y+u.size(2),u.size(16.5),u.size(16),"태스크 포스 표식 변경",()->MapCommanderHud.open(new Open(Mode.NAVAL_SETTINGS,member.id(),List.of("insignia")))));
        }
        out.add(new Hit(x,y,w,h,"§e"+member.name()+"§r\n"+member.detail(),()->{
            if(member.request().mode()==Mode.NAVAL_MISSION)selectForce(member.id());
            else if(member.request().mode()==Mode.AIR_MISSION)selectWing(member);
            else MapCommanderHud.open(member.request());
        },null,"ui.division.select"));
    }
    public static String armyUnitIcon(String icon){
        String type=icon.substring(icon.lastIndexOf('/')+1);
        String sprite=switch(type){case "motorized_infantry"->"motorized";case "mechanized_infantry"->"mechanized";case "airborne"->"paratrooper";case "mountaineer"->"mountaineers";case "tank"->"modern_armor";case "light_tank"->"light_armor";case "artillery"->"artillery_brigade";case "self_propelled_artillery"->"modern_sp_artillery_brigade";case "anti_tank"->"anti_tank_brigade";case "recce"->"recce";case "headquarters"->"bus";case "motorized_rocket_artillery"->"motorized_rocket_brigade";case "motorized_militia"->"mot_militia";case "exoskeleton_infantry"->"exoskeleton_unit";default->type;};
        String asset=source("unit_"+sprite+"_icon_medium");return UiAssets.imageHeight(asset,76)>0?asset:icon;
    }
    private static void armyMember(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Member member,int x,int y,int w,int h,String asset){
        int extra=w-u.size(194.5);UiAssets.nineSlice(g,source(asset),x,y,w,h,4,u.scale()*.5);if(MapUnitHud.selectedKeys().contains("unit/"+member.id()))UiAssets.nineSlice(g,source(asset).replace("frame_0","frame_1"),x,y,w,h,4,u.scale()*.5);
        verticalProgress(g,"navieslist_bar_org",x+u.size(1.5),y+u.size(2),u.size(3),h-u.size(4),member.organization());
        verticalProgress(g,"navieslist_bar_str",x+u.size(5.5),y+u.size(2),u.size(3),h-u.size(4),member.strength()<0?null:member.strength());
        out.add(new Hit(x+u.size(1.5),y+u.size(2),u.size(3),h-u.size(4),memberTooltip(member,"organization"),()->{},(button,twice)->divisionGesture(member,button,twice),"","division_organization"));
        out.add(new Hit(x+u.size(5.5),y+u.size(2),u.size(3),h-u.size(4),memberTooltip(member,"strength"),()->{},(button,twice)->divisionGesture(member,button,twice),"","division_strength"));
        UiAssets.draw(g,source("unitlist_unitinfo_button"),x+u.size(10),y+u.size(.5),u.size(38),u.size(15.5));
        g.enableScissor(x,y,x+w,y+h);UiAssets.draw(g,armyUnitIcon(member.icon()),x+u.size(9.5),y-u.size(3),u.size(38),u.size(21));g.disableScissor();
        if(member.data()!=null&&!member.data().orderWarning().isEmpty()){String warning=member.data().orderWarning().equals("INVALID_ORDERS")?"mapicon_unit_invalid_orders":"mapicon_unit_no_orders";UiAssets.draw(g,source(warning),x+u.size(12),y+u.size(2),u.size(6),u.size(7));out.add(new Hit(x+u.size(12),y+u.size(2),u.size(6),u.size(7),member.data().orderTooltip(),()->{},null,"","army_order_warning"));}
        out.add(new Hit(x+u.size(10),y+u.size(.5),u.size(38),u.size(15.5),memberTooltip(member,"template"),()->{},(button,twice)->divisionGesture(member,button,twice),"","division_template"));
        float experienceX=x+(u.size(48)+u.size(61)-u.scale()*16.5f)/2f,planX=x+(u.size(61)+u.size(71)-u.size(2.5))/2f;
        if(member.data()!=null&&member.data().experienceLevel()!=null){g.pose().pushMatrix();g.pose().translate(experienceX,y+u.scale()*2.5f);g.pose().scale(u.scale()*.5f);UiAssets.draw(g,source("unit_level").replace("frame_0","frame_"+member.data().experienceLevel()),0,0,27,27);g.pose().popMatrix();}
        verticalProgress(g,"unit_xp_bar",experienceX+u.scale()*13.5f,y+u.size(1.5),u.size(3),u.size(14),member.data()==null?null:member.data().experienceProgress());
        int activityX=x+(u.size(71)+u.size(86)-u.size(11)-u.size(2.5))/2;
        verticalProgress(g,"unitlist_bar_prepare",planX,y+u.size(2),u.size(2.5),u.size(12),member.data()==null?null:member.data().planPower());
        String activity=member.data()==null?"":member.data().activity();String status=switch(activity){case "MOVING","TRANSIT"->"unitlist_movement_icon";case "RETREATING"->"unitlist_movement_icon/frame_1";case "TRAINING"->"";case "FIGHTING"->"land_unit_combat_icon";default->"unitlist_dig_in_icon";};
        if(!status.isEmpty())UiAssets.draw(g,status.contains("/")?"commander/commands/"+status:source(status),activityX,y+u.size(2),u.size(11),u.size(13));
        if(status.equals("unitlist_dig_in_icon")){String key="division_entrenchment/"+member.id();out.add(new Hit(activityX,y+u.size(2),u.size(11),u.size(13),memberTooltip(member,"entrenchment"),()->{},(button,twice)->divisionGesture(member,button,twice),"",key));String detail=memberTooltip(member,"entrenchment_detail");if(!detail.isEmpty())detailedTooltips.put(key,detail);}
        int climate=acclimatizationFrame(member.data());if(climate>=0)UiAssets.draw(g,source("unitlist_acclimatization").replace("frame_0","frame_"+climate),x+u.size(70),y+u.size(8),u.size(6),u.size(6));
        if(climate>=0)out.add(0,new Hit(x+u.size(70),y+u.size(8),u.size(6),u.size(6),memberTooltip(member,"acclimatization"),()->{},(button,twice)->divisionGesture(member,button,twice),"","division_acclimatization"));
        verticalProgress(g,"unitlist_bar_prepare",activityX+u.size(11),y+u.size(2),u.size(2.5),u.size(12),member.data()==null?null:member.data().entrenchmentProgress());
        leftLabel(g,member.name(),x+u.size(87),y+u.size(1),u.size(79)+extra,h-u.size(2),UiMenuStyle.TEXT);
        UiAssets.draw(g,source("fuel_strip"),x+extra+u.size(168),y+u.size(3),u.size(9),u.size(11));
        out.add(new Hit(x+extra+u.size(168),y+u.size(3),u.size(9),u.size(11),memberTooltip(member,"fuel"),()->{},null,""));
        boolean unassigned=member.data()!=null&&member.data().attachedArmy().isEmpty();
        if(unassigned)nativeButton(g,out,"nv_btn_close",x+extra+u.size(178.5),y+u.size(.5),u.size(15.5),u.size(15.5),UiCommandLabels.text("DESELECT_UNIT","선택 해제"),()->MapUnitHud.selectDivisions(selectedDivisionRows().stream().map(Member::id).filter(id->!id.equals(member.id())).toList(),false));
        else UiAssets.drawTinted(g,source("theatre_army_shield").replace("frame_0","frame_"+(member.data()==null||member.data().insignia()==null?0:member.data().insignia())),x+extra+u.size(178),y+u.size(.5),u.size(16.5),u.size(16),member.data()==null||member.data().armyColor()==null?0xffffffff:0xff000000|member.data().armyColor());
        var army=current.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&c.panel()!=null&&member.data()!=null&&c.id().equals(member.data().attachedArmy())).findFirst().orElse(null);
        if(!unassigned)out.add(new Hit(x+extra+u.size(178),y+u.size(.5),u.size(16.5),u.size(16),UiCommandLabels.text("DIVISION_VIEW_SELECT_ARMY",""),()->{if(army!=null)select(army.id(),false);},null,"ui.click","division_army"));
            out.add(new Hit(x,y,w,h,String.join("\n",UiCommandLabels.text("ARMY_LIST_CLICK_SELECT_DIVISION",""),UiCommandLabels.text("ARMY_LIST_RIGHT_CLICK_PAN",""),UiCommandLabels.text("ARMY_LIST_DOUBLE_CLICK_TEMPLATE",""),UiCommandLabels.text("ARMY_LIST_CTRL_CLICK_TOGGLE",""))+"\n\n§Y"+member.name()+"§!\n"+member.detail(),()->{},(button,twice)->divisionGesture(member,button,twice)));return;
    }
    private static void divisionGesture(Member member,int button,boolean twice){if(button==InputConstants.MOUSE_BUTTON_RIGHT){dev.hoi.client.audio.AudioUiSounds.play("ui.click");MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,"FOCUS_UNIT",List.of("unit/"+member.id())));return;}if(button!=InputConstants.MOUSE_BUTTON_LEFT)return;dev.hoi.client.audio.AudioUiSounds.play("ui.division.select");var matching=twice&&member.data()!=null?current.cards().stream().filter(c->c.panel()!=null&&selected.contains(c.id())).flatMap(c->c.panel().members().stream()).filter(m->m.data()!=null&&m.request().mode()==Mode.ARMY_UNITS&&m.data().template().equals(member.data().template())).map(Member::id).distinct().toList():List.of(member.id());MapUnitHud.selectDivisions(matching,control());}
    static int acclimatizationFrame(MemberData data){if(data==null)return -1;double cold=data.coldAcclimatization()==null?0:data.coldAcclimatization(),hot=data.hotAcclimatization()==null?0:data.hotAcclimatization();if(cold<=0&&hot<=0)return -1;return (hot>cold?2:0)+(Math.max(cold,hot)>=1?1:0);}
    private static void verticalProgress(GuiGraphicsExtractor g,String asset,float x,int y,int w,int h,Double ratio){
        for(boolean fill:List.of(false,true)){if(fill){if(ratio==null)return;int extent=(int)Math.round(h*Math.clamp(ratio,0,1));if(extent<=0)return;g.enableScissor((int)Math.floor(x),y+h-extent,(int)Math.ceil(x+w),y+h);}g.pose().pushMatrix();g.pose().translate(x,y+h);g.pose().rotate(-(float)Math.PI/2);g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/commander/commands/"+asset+"/"+(fill?"fill":"background")+"/frame_0.png"),0,0,h,w,0,1,0,1);g.pose().popMatrix();if(fill)g.disableScissor();}
    }
    private static void nativeBar(GuiGraphicsExtractor g,String asset,int x,int y,int w,int h,double ratio,boolean vertical){
        nativeBarImage(g,"repair_stat_bg",x,y,w,h,vertical);int fill=(int)Math.round((vertical?h:w)*Math.clamp(ratio,0,1));if(fill<=0)return;
        g.enableScissor(x,vertical?y+h-fill:y,vertical?x+w:x+fill,y+h);nativeBarImage(g,asset,x,y,w,h,vertical);g.disableScissor();
    }
    static void progress(GuiGraphicsExtractor g,String asset,int x,int y,int w,int h,Double ratio,int fillColor,int backgroundColor){
        String base="commander/commands/"+asset;if(asset.equals("plan_unit_power"))g.fill(x,y,x+w,y+h,backgroundColor);else UiAssets.drawTinted(g,base+"/background/frame_0",x,y,w,h,backgroundColor);if(ratio==null)return;int fill=(int)Math.round(w*Math.clamp(ratio,0,1));if(fill<=0)return;g.enableScissor(x,y,x+fill,y+h);UiAssets.drawTinted(g,base+"/fill/frame_0",x,y,w,h,fillColor);g.disableScissor();
    }
    private static void nativeBarImage(GuiGraphicsExtractor g,String asset,int x,int y,int w,int h,boolean vertical){
        g.pose().pushMatrix();g.pose().translate(x,vertical?y+h:y);if(vertical)g.pose().rotate(-(float)Math.PI/2);g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/picker/"+asset+".png"),0,0,vertical?h:w,vertical?w:h,0,1,0,1);g.pose().popMatrix();
    }
    private static void armyListBackground(GuiGraphicsExtractor g,UiUnits u,int x,int y,int w,int h){
        int bottom=y+h-u.size(4);g.enableScissor(x,y,x+w,bottom);
        UiAssets.tiledNineSlice(g,source("tiled_window_1b_thin_border"),x,y,w,h,64,u.scale()*.5);g.disableScissor();
        var texture=net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+source("tiled_window_1b_thin_border")+".png");int width=Math.round(w/(u.scale()*.5f));
        g.pose().pushMatrix();g.pose().translate(x,bottom);g.pose().scale(u.scale()*.5f);
        g.blit(texture,0,0,64,3,0,64/190f,182/190f,185/190f);g.blit(texture,64,0,width-64,3,64/190f,126/190f,182/190f,185/190f);g.blit(texture,width-64,0,width,3,126/190f,1,182/190f,185/190f);g.pose().popMatrix();
    }
    private static void armyScrollbar(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y,int w,int h,int total,int visible){
        if(total<=visible||h<=u.size(18))return;
        int arrow=u.size(8),track=h-2*arrow,thumb=u.size(8),maximum=total-visible;
        x+=(w-arrow)/2;w=arrow;
        var background=net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+source("scrollbar_vertical_bg")+".png");int edge=Math.min(h/2,u.size(26)),bx=x-u.size(.5),bw=u.size(9);
        g.blit(background,bx,y,bx+bw,y+edge,0,1,0,1/3f);g.blit(background,bx,y+edge,bx+bw,y+h-edge,0,1,1/3f,2/3f);g.blit(background,bx,y+h-edge,bx+bw,y+h,0,1,2/3f,1);
        UiAssets.nineSlice(g,source("scroll_track"),x+u.size(1),y+arrow,u.size(6),track,4,u.scale()*.5);
        nativeButton(g,out,"scroll_up",x,y,w,arrow,"이전 사단",()->scroll=Math.max(0,scroll-1));
        nativeButton(g,out,"scroll_down",x,y+h-arrow,w,arrow,"다음 사단",()->scroll=Math.min(maximum,scroll+1));
        int thumbY=y+arrow+(int)Math.round((track-thumb)*scroll/(double)maximum);
        UiAssets.draw(g,source("scroll_drager"),x,thumbY,w,thumb);
        scrollbar=new Scrollbar(x,y+arrow,w,track,thumb,maximum);
        out.add(new Hit(x,y+arrow,w,track,"",()->{},null,"","army_scrollbar"));
    }
    private static void pager(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,int x,int y,int w,boolean previous,boolean next,int step){if(!previous&&!next)return;int bottom=MapCommanderHud.boxes().stream().mapToInt(MapCommanderHud.Box::y).min().orElse(u.height()),height=Math.min(u.size(17),bottom-y);if(height<Minecraft.getInstance().font.lineHeight)return;if(previous)button(g,out,"‹","",x,y,u.size(20),height,"이전",()->scroll=Math.max(0,scroll-step));if(next)button(g,out,"›","",x+w-u.size(20),y,u.size(20),height,"다음",()->scroll+=step);}
    private static void commandIcon(GuiGraphicsExtractor g,UiUnits u,String icon,int x,int y,int w,int h){int inset=Math.max(1,u.size(1));if(icon.equals(source("bpt_order_draw_naval_inv_with_harbor_btn"))){int width=Math.max(1,w-2*inset),height=Math.min(Math.max(1,h-2*inset),Math.max(1,(int)Math.round(width*32/35.5)));g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/commander/commands/bpt_order_draw_naval_inv_with_harbor_btn/atlas.png"),x+inset,y+(h-height)/2,x+inset+width,y+(h-height)/2+height,0,.5f,0,1);return;}UiAssets.drawContent(g,icon,x+inset,y+inset,Math.max(1,w-2*inset),Math.max(1,h-2*inset));}
    private static void armyButton(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,String icon,int x,int y,int slot,String tip,Runnable action){
        String command=switch(icon){case "bpt_order_unassign_units_btn"->"ARMY/UNASSIGN_UNITS";case "bpt_order_assign_units_btn"->"LAND/ASSIGN_UNITS";case "bpt_order_reshape_btn"->"LAND/EDIT_PLAN";case "exercise_icon"->"ARMY/"+(selectedArmies().stream().flatMap(c->c.panel().controls().stream()).anyMatch(c->c.id().equals("STOP_EXERCISE"))?"STOP_EXERCISE":shift()?"EXERCISE_UNTIL_TRAINED":"EXERCISE");default->"";};
        var cell=MapCommanderLayout.armyCell(u,x,y,slot);commandIcon(g,u,source(icon).replace("frame_0",icon.startsWith("bpt_execute_")||icon.startsWith("bpt_cohesion_")?"frame_1":"frame_0"),cell.x(),cell.y(),cell.width(),cell.height());
        out.add(new Hit(cell.x(),cell.y(),cell.width(),cell.height(),MapCommanderAvailability.tooltip(command,tip),()->{if(MapCommanderAvailability.allow(command))action.run();},null,"ui.battleplan",icon));
    }
    private static void actionbar(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> cards,int y,int popupBottom) {
        boolean navy=current.branch()==Mode.NAVY_HQ;int w=u.size(navy?266:320.5),h=u.size(navy?50:39.5),x=MapCommanderLayout.commandLeft(u,navy);
        if(navy)UiAssets.tiledNineSlice(g,source("air_priorities_tiled_bg"),x,y,w,h,32,u.scale()*.5);else background(g,"bpt_bg",x,y,w,h);
        leftLabel(g,navy?navalCaption(cards):cards.stream().filter(c->c.panel()!=null).map(c->c.panel().commandCaption()).filter(c->!c.isEmpty()).distinct().reduce((a,b)->a+" · "+b).orElse(""),x+u.size(7),y+u.size(3),w-u.size(navy?30:14),u.size(11),UiMenuStyle.TEXT);
        if(navy) {
            var ids=cards.stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).map(Member::id).filter(id->!specificForces||selectedForces.contains(id)).distinct().toList();
            for(int n=0;n<3;n++){String icon=List.of("increase","multiplication","reduction").get(n);UiAssets.draw(g,source("naval_dominance_"+icon+"_texticon"),x+u.size(10+n*25),y+u.size(15),u.size(20.5),u.size(10.5));}
            branchButtons(g,u,out,true,ids,x,y);int rx=x+u.size(248),ry=y+u.size(27);UiAssets.draw(g,source("nv_btn_remove_regions"),rx,ry,u.size(13.5),u.size(13.5));out.add(new Hit(rx,ry,u.size(13.5),u.size(13.5),UiCommandLabels.text("NAVAL_BTN_REMOVE_REGIONS", "배정 해역 제거"),()->{},(button,twice)->{if(button==InputConstants.MOUSE_BUTTON_LEFT||button==InputConstants.MOUSE_BUTTON_RIGHT){dev.hoi.client.audio.AudioUiSounds.play("ui.click");navalAreas(button==InputConstants.MOUSE_BUTTON_RIGHT);}}));forceBand(g,u,out,cards,x,y+u.size(47.5),w);navalTargets(g,u,out,x,y,popupBottom);
        } else {
            armyAbilities(g,u,out,x,y,w);areaDefense(g,u,out,x,y,w,popupBottom);var controls=current.cards().stream().filter(c->c.panel()!=null&&(cards.stream().anyMatch(selected->selected.id().equals(c.id())||selected.id().equals(c.panel().parent())))).flatMap(c->c.panel().controls().stream()).toList();
            int bx=x+u.size(8);for(String type:List.of("INVASION","FRONT","OFFENSIVE","SPEARHEAD","FALLBACK","AREA_DEFENSE")) {
                var matching=controls.stream().filter(c->c.id().equals(type)).toList();
                if(matching.isEmpty()){
                    String icon=Map.of("INVASION","bpt_order_draw_naval_inv_btn","FRONT","bpt_order_draw_defence_btn","OFFENSIVE","bpt_order_draw_offence_btn","SPEARHEAD","bpt_order_draw_blitz_btn","FALLBACK","bpt_order_draw_fallback_btn","AREA_DEFENSE","bpt_order_area_defense_btn").get(type);
                    String key=Map.of("INVASION","UNITS_VIEW_BTN_DRAW_NAVAL_INV","FRONT","UNITS_VIEW_BTN_DRAW_DEFENCE","OFFENSIVE","UNITS_VIEW_BTN_DRAW_OFFENCE","SPEARHEAD","UNITS_VIEW_BTN_DRAW_BLITZ","FALLBACK","UNITS_VIEW_BTN_DRAW_FALLBACK","AREA_DEFENSE","UNITS_VIEW_BTN_AREA_DEFENSE").get(type);
                    var cell=MapCommanderLayout.armyCell(u,x,y,Map.of("INVASION",5,"FRONT",7,"OFFENSIVE",8,"SPEARHEAD",9,"FALLBACK",10,"AREA_DEFENSE",11).get(type));commandIcon(g,u,source(icon),cell.x(),cell.y(),cell.width(),cell.height());out.add(new Hit(cell.x(),cell.y(),cell.width(),cell.height(),UiCommandLabels.text(key,type)+"\n\n명령을 지정할 군을 선택하세요.",()->{}));continue;
                }
                var first=matching.getFirst();
                var cell=MapCommanderLayout.armyCell(u,x,y,Map.of("INVASION",5,"FRONT",7,"OFFENSIVE",8,"SPEARHEAD",9,"FALLBACK",10,"AREA_DEFENSE",11).get(type));commandIcon(g,u,first.icon(),cell.x(),cell.y(),cell.width(),cell.height());
                String reason=armyControlReason(type);
                out.add(new Hit(cell.x(),cell.y(),cell.width(),cell.height(),MapCommanderAvailability.explain(UiCommandLabels.withoutShortcuts(type.equals("FRONT")&&cards.stream().anyMatch(c->c.role()==Mode.ARMY_GROUP)?UiCommandLabels.text("UNITS_VIEW_BTN_DRAW_DEFENCE_ARMY_GROUP",first.tooltip()):first.tooltip()),reason),()->{openArmyControl(type);},null,"ui.battleplan"));bx+=u.size(21);
            }
            var airborne=MapCommanderLayout.armyCell(u,x,y,4);commandIcon(g,u,source("bpt_order_draw_paradrop_btn"),airborne.x(),airborne.y(),airborne.width(),airborne.height());out.add(new Hit(airborne.x(),airborne.y(),airborne.width(),airborne.height(),UiCommandLabels.text("UNITS_VIEW_BTN_DRAW_PARADROP","")+"\n\n공수 작전 실행은 아직 지원하지 않습니다.",()->{}));
            var harbor=MapCommanderLayout.armyCell(u,x,y,6);commandIcon(g,u,source("bpt_order_draw_naval_inv_with_harbor_btn"),harbor.x(),harbor.y(),harbor.width(),harbor.height());out.add(new Hit(harbor.x(),harbor.y(),harbor.width(),harbor.height(),UiCommandLabels.text("UNITS_VIEW_BTN_DRAW_NAVAL_INV_WITH_HARBOR","")+"\n\n부유항을 이용한 해상 침공은 아직 지원하지 않습니다.",()->{}));
            armyButton(g,u,out,"bpt_order_unassign_units_btn",x,y,13,UiCommandLabels.text(selectedDivisions().size()==1?"CONFIRMUNASSIGNUNIT":"CONFIRMUNASSIGNUNITS",""),()->landSettings("UNASSIGN_UNITS"));
            armyButton(g,u,out,"bpt_order_assign_units_btn",x,y,12,UiCommandLabels.text("UNITS_VIEW_BTN_UNITS_ASSIGN", "사단 배정 모드"),MapCommanderOrders::assignSelected);
            var armies=current.cards().stream().filter(c->c.panel()!=null&&c.role()!=Mode.ARMY_GROUP&&cards.stream().anyMatch(chosen->chosen.id().equals(c.id())||chosen.id().equals(c.panel().parent()))).map(Card::id).distinct().toList();
            boolean training=controls.stream().anyMatch(c->c.id().equals("STOP_EXERCISE"));
            armyButton(g,u,out,"exercise_icon",x,y,0,training?UiCommandLabels.text("UNITS_VIEW_BTN_STOP_TRAINING_TOOLTIP","훈련 중지"):controls.stream().filter(c->c.id().equals("EXERCISE")).map(Control::tooltip).findFirst().orElse(UiCommandLabels.text("UNITS_VIEW_BTN_START_TRAINING","훈련")),()->{if(!armies.isEmpty())MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,training?"STOP_EXERCISE":shift()?"EXERCISE_UNTIL_TRAINED":"EXERCISE",armies));});
            var cohesion=controls.stream().filter(c->c.id().equals("COHESION_IMMOBILE")||c.id().equals("COHESION_CURRENT")).toList();armyButton(g,u,out,cohesion.stream().anyMatch(c->c.id().equals("COHESION_IMMOBILE"))?"bpt_cohesion_immobile":"bpt_cohesion_balanced",x,y,2,UiCommandLabels.text("PLAN_COHESION_MODE","")+"\n\n"+(cohesion.isEmpty()?"군을 선택하세요.":cohesion.getFirst().tooltip()),()->{if(!cohesion.isEmpty())openCohesion();});
            var modes=cards.stream().filter(c->c.panel()!=null).map(c->c.panel().executionMode()).distinct().toList();String mode=modes.size()==1?modes.getFirst():"";String executeIcon=switch(mode){case "CAREFUL"->"bpt_execute_slow";case "AGGRESSIVE"->"bpt_execute_fast";default->"bpt_execute_mid";};
            armyButton(g,u,out,executeIcon,x,y,1,UiCommandLabels.text("PLAN_EXECUTE_MODE","전투 계획 실행 모드")+(mode.isEmpty()?"\n\n표시 자료가 없거나 선택한 계획의 자세가 다릅니다.":""),()->postureMenu=!postureMenu);
            var offensive=MapCommanderLayout.armyCell(u,x,y,8);UiAssets.draw(g,source("bpt_execute_strip").replace("frame_0","frame_"+switch(mode){case "CAREFUL"->0;case "AGGRESSIVE"->2;default->1;}),offensive.x(),offensive.y()+u.size(7),u.size(13.5),u.size(13.5));
            if(postureMenu){String[] actions={"CAREFUL","BALANCED","AGGRESSIVE"},icons={"slow","mid","fast"},keys={"CAREFUL","BALANCED","RUSH"};for(int n=0;n<3;n++){String action=actions[n];String key="PLAN_EXECUTE_"+keys[n];button(g,out,"","bpt_execute_"+icons[n],x+u.size(3.5+n*20),popupBottom-u.size(20),u.size(17.5),u.size(17),MapCommanderAvailability.tooltip("ARMY/"+action,UiCommandLabels.text(key+"_TITLE",action)+"\n\n"+UiCommandLabels.text(key+"_DESC","")),()->{if(!MapCommanderAvailability.allow("ARMY/"+action))return;postureMenu=false;if(!armies.isEmpty())MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,action,armies));});}}
            armyButton(g,u,out,"bpt_order_reshape_btn",x,y,14,UiCommandLabels.text("UNITS_VIEW_BTN_RESHAPE", "편집 모드"),()->MapCommanderOrders.land("EDIT_PLAN",armies));
            String deleteReason=MapCommanderAvailability.reason("LAND/DELETE_PLAN"),removeReason=MapCommanderAvailability.reason("ARMY/REMOVE");String deleteTip=UiCommandLabels.text("UNITS_VIEW_BTN_DELETE_ORDER","명령 삭제");if(!deleteReason.isEmpty())deleteTip=MapCommanderAvailability.explain(deleteTip,"클릭: "+deleteReason);if(!removeReason.isEmpty())deleteTip=MapCommanderAvailability.explain(deleteTip,"우클릭: "+removeReason);var deleteCell=MapCommanderLayout.armyCell(u,x,y,15);commandIcon(g,u,source("bpt_order_delete_btn"),deleteCell.x(),deleteCell.y(),deleteCell.width(),deleteCell.height());out.add(new Hit(deleteCell.x(),deleteCell.y(),deleteCell.width(),deleteCell.height(),deleteTip,()->{},(button,twice)->{if(armies.isEmpty())return;if(button==InputConstants.MOUSE_BUTTON_RIGHT){if(!MapCommanderAvailability.allow("ARMY/REMOVE",armies))return;dev.hoi.client.audio.AudioUiSounds.play("ui.battleplan");MapCommanderHud.open(new Open(Mode.ARMY_TOOLBAR,"REMOVE",armies));}else if(button==InputConstants.MOUSE_BUTTON_LEFT){dev.hoi.client.audio.AudioUiSounds.play("ui.battleplan");deletePlan();}}));
        }
        String branchReason=navy?"":MapCommanderAvailability.branchReason("ARMY/");
        if(!branchReason.isEmpty()||cards.isEmpty()&&selectedDivisions().isEmpty()){
            String reason=branchReason.isEmpty()?"군을 선택하세요.":branchReason;
            for(int i=0;i<out.size();i++){var hit=out.get(i);if(navy)g.fill(hit.x(),hit.y(),hit.x()+hit.w(),hit.y()+hit.h(),0x44000000);out.set(i,new Hit(hit.x(),hit.y(),hit.w(),hit.h(),branchReason.isEmpty()?hit.tip()+"\n\n군을 선택하세요.":hit.tip().contains(reason)?hit.tip():MapCommanderAvailability.explain(hit.tip(),reason),()->{},null,"",hit.icon(),hit.pixelShift(),hit.horizontalShift()));}
        }
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
    static void selectTheaterForce(Card card,Member force){select(card.id(),false);selectForce(force.id());}
    private static String hullTooltip(String icon,int count){String hull=icon.substring(icon.lastIndexOf('/')+1).replace("navy_","");String name=UiCommandLabels.text("HOI_HULL_"+hull,switch(hull){case "carrier"->"항공모함";case "heavy_cruiser"->"구축함";case "battlecruiser"->"순양전함";case "destroyer"->"초계함";case "light_cruiser"->"호위함";case "battleship"->"미사일 순양함";case "submarine"->"잠수함";default->hull;});return "§Y"+name+"§! 유형의 함정 §Y"+count+"§!척";}
    private static List<Map.Entry<String,Integer>> orderedHulls(Map<String,Integer> hulls){var order=List.of("carrier","battleship","battlecruiser","heavy_cruiser","light_cruiser","destroyer","submarine");return hulls.entrySet().stream().sorted(Comparator.comparingInt(e->{int index=order.indexOf(e.getKey().replace("unit_hud/navy_",""));return index<0?order.size():index;})).toList();}
    static int hullTarget(Member member,String icon,int count){var navy=member.data()==null?null:member.data().navy();return navy==null?count:navy.targets().getOrDefault(icon,count);}
    private static Card fleetFor(Member member){return current.cards().stream().filter(c->c.panel()!=null&&c.panel().members().stream().anyMatch(m->m.id().equals(member.id()))).findFirst().orElse(null);}
    static int fleetColor(Card card){return card==null||card.panel()==null||card.panel().color()==null?0xffffffff:0xff000000|card.panel().color();}
    static int shipCount(Member member){return member.composition().values().stream().mapToInt(Integer::intValue).sum();}
    static Map<String,Integer> fleetComposition(Card card){var panel=card.panel();if(panel==null)return Map.of();if(!panel.divisionTypes().isEmpty())return panel.divisionTypes();if(panel.total()!=panel.members().size())return Map.of();var counts=new TreeMap<String,Integer>();panel.members().forEach(member->member.composition().forEach((icon,count)->counts.merge(icon,count,Math::addExact)));return counts;}
    static int fleetShipCount(Card card){if(card.panel()!=null&&card.panel().fleet()!=null)return card.panel().fleet().ships();return card.panel()==null||card.panel().divisionTypes().isEmpty()&&card.panel().total()!=card.panel().members().size()?-1:fleetComposition(card).values().stream().mapToInt(Integer::intValue).sum();}
    private static String navalHullIcon(String icon){return source("unit_"+icon.replace("unit_hud/navy_","")+"_icon_medium_white");}
    private static void navalHullBars(GuiGraphicsExtractor g,UiUnits u,int x,int y,Double strength,Double organization,Double air){g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(u.scale()*.5f);navalHullBar(g,"navieslist_bar_str_compact",0,23,strength);navalHullBar(g,"navieslist_bar_org_compact",4,23,organization);if(air!=null){navalHullBar(g,"navieslist_bar_planes",20,30,air);UiAssets.draw(g,source("nv_xs_plane_icon"),2,24,15,6);}g.pose().popMatrix();}
    private static void navalHullBar(GuiGraphicsExtractor g,String asset,int y,int width,Double ratio){
        var path="hoi:textures/gui/commander/commands/"+asset;g.blit(net.minecraft.resources.Identifier.parse(path+"/background/frame_0.png"),0,y,width,y+3,0,1,0,1);
        if(ratio==null)return;int filled=(int)Math.round(width*Math.clamp(ratio,0,1));if(filled>0)g.blit(net.minecraft.resources.Identifier.parse(path+"/fill/frame_0.png"),0,y,filled,y+3,0,(float)filled/width,0,1);
    }
    static String navalActivityIcon(Member member){var state=member.data()==null?null:member.data().navy();String activity=state==null?"":state.activityIcon();boolean mission=activity.isEmpty()||activity.equals("ship_activity_docked")||activity.equals("ship_activity_holding")||activity.startsWith("ship_activity_mission_");return mission?source("nv_all_missions").replace("frame_0","frame_"+navalMissionFrame(activity.equals("ship_activity_docked")?"HOLD":member.mission())):source(activity);}
    private static void navalActivityLabel(GuiGraphicsExtractor g,UiUnits u,String text,int x,int y,int w,int h,int color){var flag=java.util.regex.Pattern.compile("£flag_([A-Z]{3})£?").matcher(text);if(!flag.find()){leftLabel(g,text,x,y,w,h,color);return;}String before=text.substring(0,flag.start()),after=text.substring(flag.end());var font=Minecraft.getInstance().font;int prefix=(int)Math.ceil(font.width(UiKeywordIcons.decorateLines(UiTooltips.officerText(before),false))*UiText.scale(font));leftLabel(g,before,x,y,Math.min(w,prefix),h,color);int fw=u.size(4.5),fh=u.size(3);UiAssets.draw(g,UiMenuBar.flagTexture(flag.group(1)),x+prefix,y+(h-fh)/2,fw,fh);leftLabel(g,after,x+prefix+fw,y,Math.max(1,w-prefix-fw),h,color);}
    static int navalMissionFrame(String mission){return switch(mission){case "PATROL"->1;case "STRIKE_FORCE"->2;case "CONVOY_RAIDING"->3;case "CONVOY_ESCORT"->4;case "MINELAYING"->5;case "MINESWEEPING"->6;case "EXERCISE","EXERCISE_UNTIL_TRAINED"->7;case "NAVAL_INVASION_SUPPORT"->9;default->0;};}
    private static void selectFleetForces(Card card){MapUnitHud.clearSelection();specificForces=true;selectedForces.clear();if(card.panel()!=null)card.panel().members().forEach(m->selectedForces.add(m.id()));MapCommanderOrders.selectionChanged();}
    private static void deselectForce(String id){var retained=new LinkedHashSet<>(navalSelection());retained.remove(id);MapUnitHud.clearSelection();specificForces=true;selectedForces.clear();selectedForces.addAll(retained);MapCommanderOrders.selectionChanged();}
    private static void forceBand(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Card> cards,int x,int y,int w){
        var forces=cards.stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).toList();int perPage=Math.max(1,(w-u.size(36))/u.size(15)),pages=Math.max(1,(forces.size()+perPage-1)/perPage);forcePage=Math.clamp(forcePage,0,pages-1);int shown=Math.min(perPage,Math.max(0,forces.size()-forcePage*perPage)),bandWidth=Math.max(u.size(150.5),u.size(22+shown*15)),left=x+(w-bandWidth)/2;
        UiAssets.nineSlice(g,source("theatre_naval_control_panel"),left,y,bandWidth,u.size(21.5),8,u.scale()*.5);var visible=forces.subList(Math.min(forces.size(),forcePage*perPage),Math.min(forces.size(),(forcePage+1)*perPage));var badges=new ArrayList<Hit>();var row=MapCommanderTheaters.navalBadges(u,shown+1,bandWidth,visible.isEmpty()?"0":Integer.toString(shipCount(visible.getLast())),3);
        for(int n=visible.size()-1;n>=0;n--){var force=visible.get(n);float exact=left+row.x(n+1);int cell=(int)Math.floor(exact);float shift=exact-cell;g.pose().pushMatrix();g.pose().translate(shift,0);String sprite=selectedForces.contains(force.id())?"theatre_army_shield_selected":"theatre_army_shield";UiAssets.drawTinted(g,source(sprite).replace("frame_0","frame_"+(force.data()==null||force.data().insignia()==null?0:force.data().insignia())),cell,y+u.size(2),u.size(16.5),u.size(16),fleetColor(fleetFor(force)));MapCommanderTheaters.navalCount(g,u,Integer.toString(shipCount(force)),cell,y+u.size(2));String tip=force.name()+"\n"+force.status()+"\n\n"+force.detail();for(var entry:new TreeMap<>(force.composition()).entrySet())tip+="\n"+hullTooltip(entry.getKey(),entry.getValue());badges.add(new Hit(cell,y+u.size(3),u.size(21),u.size(17),tip,()->selectForce(force.id()),null,"ui.division.select","naval_force_band").shiftedX(shift));g.pose().popMatrix();}Collections.reverse(badges);out.addAll(badges);
        float anchor=left+row.x(0);int anchorX=(int)Math.floor(anchor);g.pose().pushMatrix();g.pose().translate(anchor-anchorX,0);UiAssets.drawTinted(g,source("theatre_task_force_shield_selected").replace("frame_0","frame_3"),anchorX,y+u.size(1),u.size(16.5),u.size(16),0xffd8c97a);MapCommanderTheaters.navalCount(g,u,cards.stream().anyMatch(card->fleetShipCount(card)<0)?"?":Integer.toString(cards.stream().mapToInt(MapCommanderPanels::fleetShipCount).sum()),anchorX,y+u.size(2));g.pose().popMatrix();
        if(forcePage>0)button(g,out,"‹","",left-u.size(15),y+u.size(3),u.size(14),u.size(17),"이전",()->forcePage--);if(forcePage+1<pages)button(g,out,"›","",left+bandWidth+u.size(1),y+u.size(3),u.size(14),u.size(17),"다음",()->forcePage++);
    }
    private static final String[][] NAVAL={{"PATROL","patrol","PATROL"},{"STRIKE_FORCE","strike_force","STRIKE_FORCE"},{"CONVOY_RAIDING","convoy_raid","CONVOY_RAIDING"},{"CONVOY_ESCORT","convoy_escort","CONVOY_ESCORT"},{"MINELAYING","mines_planting","MINES_PLANTING"},{"MINESWEEPING","mines_sweeping","MINES_SWEEPING"},{"NAVAL_INVASION_SUPPORT","naval_invasion_support","NAVAL_INVASION_SUPPORT"},{"HOLD","hold","HOLD"},{"EXERCISE","training","TRAINING"}};
    private static final String[][] AIR={{"EXERCISE","11","TRAINING"},{"SUPERIORITY","0","AIR_SUPERIORITY"},{"CLOSE_AIR_SUPPORT","1","CAS"},{"INTERCEPTION","2","INTERCEPTION"},{"STRATEGIC_BOMBING","3","STRATEGIC_BOMBER"},{"NAVAL_STRIKE","4","NAVAL_BOMBER"},{"PORT_STRIKE","8","PORT_STRIKE"},{"LOGISTICS_STRIKE","9","ATTACK_LOGISTICS"},{"AIR_SUPPLY","10","AIR_SUPPLY"},{"MINELAYING","12","NAVAL_MINE_PLANTING"},{"MINESWEEPING","13","NAVAL_MINE_SWEEPING"},{"RECON","14","RECON"},{"NAVAL_PATROL","15","NAVAL_PATROL"}};
    private static void branchButtons(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,boolean navy,List<String> ids,int x,int y) {
        var selectedIds=new HashSet<>(ids);var currentMembers=navy?current.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).filter(m->selectedIds.contains(m.id())).toList():current.wings().stream().filter(m->selectedIds.contains(m.id())).toList();
        int index=0;for(var mission:navy?NAVAL:AIR) {
            int offset=navy?Map.of("PATROL",10,"STRIKE_FORCE",60,"CONVOY_RAIDING",110,"EXERCISE",185,"CONVOY_ESCORT",235,"MINELAYING",285,"MINESWEEPING",335,"NAVAL_INVASION_SUPPORT",385,"HOLD",466).get(mission[0]):0;int slot=index++;if(!navy&&slot>=5)slot+=slot==5?1:2;int bx=x+u.size(navy?offset/2.0:7+slot*22.5),by=y+(navy?u.size(mission[0].equals("HOLD")?27:24):u.size(4)),w=navy?u.size(mission[0].equals("HOLD")?13.5:24):u.size(23.5),h=u.size(navy?(mission[0].equals("HOLD")?13.5:20):17);String key=(navy?"NAVAL_MISSION_NAME_":"AIRWING_MISSION_TYPE_")+mission[2];String name=UiCommandLabels.text(key,mission[0]);if(!navy&&mission[0].equals("HOLD"))name=UiCommandLabels.text("AIRWING_MISSION_NONE","임무 없음");
            String detail=UiCommandLabels.text(key+"_DESC","");if(navy&&mission[0].equals("STRIKE_FORCE"))detail=current.cards().stream().filter(c->selected.contains(c.id())&&c.panel()!=null).flatMap(c->c.panel().controls().stream()).filter(c->c.id().equals("STRIKE_FORCE")).map(Control::tooltip).findFirst().orElse(detail);if(mission[0].equals("EXERCISE"))detail+=navy?"":"\n\n"+UiCommandLabels.text("AIRWING_MISSION_TYPE_TRAINING_DESC_SHIFT_CLICK","");
            boolean active=!currentMembers.isEmpty()&&currentMembers.stream().allMatch(m->m.mission().equals(mission[0]));
            String icon=navy?source("nv_btn_"+mission[1]).replace("frame_0",currentMembers.stream().anyMatch(m->m.mission().equals(mission[0]))?"frame_2":"frame_0"):mission[0].equals("HOLD")?source("uv_btn_unassign"):"commander/commands/strategicair_mission_types/frame_"+mission[1];
            String chosen=navy&&mission[0].equals("HOLD")&&control()?"HOLD_IN_PLACE":mission[0].equals("EXERCISE")&&shift()?"EXERCISE_UNTIL_TRAINED":mission[0];String command=(navy?"NAVY/":"AIR/")+chosen;boolean disabled=!MapCommanderAvailability.reason(command).isEmpty();
            if(navy){if(disabled)UiAssets.drawGrayscale(g,source("nv_btn_"+mission[1]),bx,by,w,h);else UiAssets.draw(g,icon,bx,by,w,h);if(!disabled&&MapCommanderAvailability.warning(command))UiAssets.draw(g,source("btn_yellow_glow"),bx,by,w,h);}else {String background=source(Set.of("CLOSE_AIR_SUPPORT","STRATEGIC_BOMBING","NAVAL_STRIKE","PORT_STRIKE","LOGISTICS_STRIKE","AIR_SUPPLY","RECON","NAVAL_PATROL","MINELAYING","MINESWEEPING").contains(mission[0])?"strategicair_mission_btn_checkbox_bg":"strategicair_mission_btn_bg").replace("frame_0",active?"frame_0":"frame_1");if(disabled){UiAssets.drawGrayscale(g,background,bx,by,w,h);g.pose().pushMatrix();g.pose().translate(bx,by);g.pose().scale(u.scale()*.5f);UiAssets.drawGrayscale(g,icon,1,4,42,29);g.pose().popMatrix();}else{UiAssets.draw(g,background,bx,by,w,h);g.pose().pushMatrix();g.pose().translate(bx,by);g.pose().scale(u.scale()*.5f);UiAssets.draw(g,icon,1,4,42,29);g.pose().popMatrix();}}
            String tip=MapCommanderAvailability.tooltip(command,name+"\n\n"+detail);out.add(new Hit(bx,by,w,h,tip,()->{if(ids.isEmpty()||!MapCommanderAvailability.allow(command))return;var values=new ArrayList<String>();values.add("mission/"+chosen);values.addAll(ids);MapCommanderHud.open(new Open(navy?Mode.NAVAL_TOOLBAR:Mode.AIR_TOOLBAR,"",values));},null,"ui.click",navy?"naval_mission_"+chosen:"air_mission_"+chosen));
        }
    }
    private static void airOrganizationButton(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,String action,String icon,String key,List<String> ids,int x,int y){
        button(g,out,"",icon,x,y,u.size(14.5),u.size(14.5),UiCommandLabels.text(key,action.equals("CREATE")?"비행단 창설":action),()->{var selection=new ArrayList<String>();selection.add("action/"+action);selection.addAll(ids);MapCommanderHud.open(new Open(Mode.AIR_ORGANIZE,"",selection));});
    }
    private static List<String> airGroupIds(List<Member> wings){return wings.stream().filter(w->w.data()!=null&&w.data().air()!=null&&!w.data().air().airGroup().isEmpty()).map(w->w.data().air().airGroup()).distinct().toList();}
    private static void airGroups(String action,List<Member> wings){var groups=airGroupIds(wings);if(action.equals("merge")&&groups.size()<2||action.equals("remove")&&groups.isEmpty())return;var ids=action.equals("merge")?groups:wings.stream().map(Member::id).toList();if(!ids.isEmpty())MapCommanderHud.open(new Open(Mode.AIR_GROUPS,action,ids));}
    private static String airPolicyTooltip(String policy){String key="AIR_REINFORCE_"+policy;return UiCommandLabels.text(key+"_SHORT",policy)+"\n"+UiCommandLabels.text(key,policy)+"\n"+UiCommandLabels.text("AIR_REINFORCE_SELECTION","")+"\n\n"+UiCommandLabels.text("AIR_REINFORCE_DESC","");}
    private static void airPolicyIcon(GuiGraphicsExtractor g,String policy,int x,int y,int w,int h){int frame=List.of("OBSOLETE_ONLY","ANY","ONLY_NEW","ONLY_SAME").indexOf(policy);if(frame>=0)g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/commander/commands/wing_prio_strip/atlas.png"),x,y,x+w,y+h,frame/8f,(frame+1)/8f,0,1);}
    private static void airPolicy(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Member> wings,int x,int y){
        var policies=wings.stream().filter(w->w.data()!=null&&w.data().air()!=null).map(w->w.data().air().reinforcementPolicy()).distinct().toList();
        String tip=policies.isEmpty()?UiCommandLabels.text("AIR_REINFORCE_SELECTION",""):String.join("\n\n",policies.stream().map(MapCommanderPanels::airPolicyTooltip).toList());
        int w=u.size(14.5),h=u.size(14.8);
        var request=new Open(Mode.AIR_REINFORCEMENT,"policy",wings.stream().map(Member::id).toList());out.add(new Hit(x,y,w,h,tip,()->MapCommanderHud.open(request)));
        button(g,out,"","nv_btn_base_partial_toggle",x-u.size(1),y-u.size(1),u.size(15.5),u.size(15.5),tip,()->MapCommanderHud.open(request));if(policies.size()==1)airPolicyIcon(g,policies.getFirst(),x,y,w,h);else label(g,"—",x,y,w,h,UiMenuStyle.TEXT);
    }
    private static void airBaseHeader(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,Member wing,int x,int y,int w,int h) {
        background(g,"av_base_entry_bg",x,y,w,h);var owner=MapCommanderAirBases.state(wing);if(owner!=null&&!owner.controller().isEmpty())UiAssets.cover(g,UiMenuBar.flagTexture(owner.controller()),x+u.size(8),y+u.size(8),u.size(9),u.size(7));airLabel(g,u,wing.group(),x+u.size(33.5),y+u.size(5.5),u.size(60),u.size(11.5));
        UiAssets.draw(g,source("fuel_strip"),x+u.size(19),y+u.size(4.5),u.size(10),u.size(13));out.add(new Hit(x+u.size(19),y+u.size(4.5),u.size(10),u.size(13),MapCommanderAirBases.fuelTooltip(current.country()),()->{},null,""));
        var base=MapCommanderAirBases.state(wing);String tip=MapCommanderAirBases.tooltip(wing),capacity=MapCommanderAirBases.capacity(wing);if(base!=null)capacity=(base.totalPlanes()>base.capacity()?"§R":"§G")+base.totalPlanes()+"§!"+capacity.substring(Integer.toString(base.totalPlanes()).length());airLabel(g,u,capacity,x+u.size(104),y+u.size(6.5),u.size(70),u.size(11.5));out.add(new Hit(x+u.size(104),y,u.size(70),h,tip,()->{},null,""));
        airOrganizationButton(g,u,out,"CREATE","air_base_create_new_btn","AIRWING_NEW",base==null?List.of():List.of("base/"+base.id()),x+u.size(244.5),y+u.size(4));
        button(g,out,"","edit_shortcuts_button",x+u.size(225),y+u.size(4),u.size(16),u.size(16),UiCommandLabels.text("QUICK_DEPLOY_SELECTION_WINDOW_OPEN_DESC","빠른 배치\n\n특정 유형의 §H비행단§!을 배치하기 위한 단축 버튼을 추가합니다.")+"\n\n빠른 배치 단축 버튼 설정은 아직 지원하지 않습니다.",()->{},true);
        button(g,out,"×","nv_btn_close",x+u.size(259.5),y+u.size(4),u.size(14.5),u.size(14.5),"선택 해제 · "+wing.group(),()->{String id=MapCommanderAirBases.identity(wing);showWings(current.wings().stream().filter(MapCommanderPanels::wingSelected).filter(member->!MapCommanderAirBases.identity(member).equals(id)).map(Member::id).toList());});
        out.add(new Hit(x,y,w,h,tip,()->{},null,""));
    }
    private static void air(GuiGraphicsExtractor g,UiUnits u,List<Hit> out,List<Member> wings,int bottom) {
        int x=u.size(3),y=UiMenuBar.height(g.guiWidth())+u.size(2),w=u.size(274),h=u.size(24);
        background(g,"tiled_plain_bg_small",x,y,u.size(351.5),u.size(26));branchButtons(g,u,out,false,wings.stream().map(Member::id).toList(),x,y);button(g,out,"","air_target_icon",x+u.size(119.5),y+u.size(4),u.size(15.5),u.size(15.5),UiCommandLabels.text("AIR_STRATEGIC_PRIORITIES","전략폭격 우선순위")+"\n\n전략 폭격 목표 우선순위 설정은 아직 지원하지 않습니다.",()->{},true);UiAssets.drawGrayscale(g,source("strategicair_mission_btn_bg").replace("frame_0","frame_1"),x+u.size(164.5),y+u.size(4),u.size(23.5),u.size(17));UiAssets.drawGrayscale(g,"commander/commands/strategicair_mission_types/frame_7",x+u.size(168),y+u.size(6),u.size(21),u.size(14.5));out.add(new Hit(x+u.size(164.5),y+u.size(4),u.size(23.5),u.size(17),UiCommandLabels.text("AIRWING_MISSION_TYPE_NAVAL_KAMIKAZE","")+"\n\n이 임무는 아직 지원하지 않습니다.",()->{},null,"","air_mission_unsupported"));y+=u.size(27.5);
        int toolbar=u.size(326);background(g,"tiled_plain_bg_small",x,y,toolbar,h);g.pose().pushMatrix();g.pose().translate(x+u.length(13.5),y+u.length(3.5));g.pose().scale(u.scale()*.5f);UiAssets.draw(g,source("unit_list_header_air"),0,0,471,35);g.pose().popMatrix();String selectedText=UiCommandLabels.text("UNITS_VIEW_WINGS_COUNT","$WINGS|G$개 비행단 선택됨").replace("$WINGS|G$","§G"+wings.size()+"§!");int textWidth=(int)Math.ceil(Minecraft.getInstance().font.width(UiTooltips.officerText(selectedText))*u.scale()),leftButtons=x+u.size(23.5)+textWidth+u.size(3);airLabel(g,u,selectedText,x+u.size(23.5),y+u.size(6.5),textWidth,u.size(11.5));out.add(new Hit(x+u.size(23.5),y,textWidth,h,selectedText,()->{},null,"","air_selected_count"));
        var ids=wings.stream().map(Member::id).toList();
        airOrganizationButton(g,u,out,"DISBAND","nv_btn_delete","AIRWING_DISBAND",ids,x+u.size(3.5),y+u.size(4));
        button(g,out,"","add_to_air_group",leftButtons,y+u.size(4),u.size(14.5),u.size(14.5),UiCommandLabels.text("CREATE_AIR_GROUP",""),()->airGroups("create",wings));
        airOrganizationButton(g,u,out,"SPLIT","nv_btn_split_in_half","AIRWING_SPLIT",ids,leftButtons+u.size(15),y+u.size(4));
        airOrganizationButton(g,u,out,"MERGE","consolidate","AIRWING_MERGE",ids,leftButtons+u.size(30),y+u.size(4));
        var groups=airGroupIds(wings);String merge=UiCommandLabels.text("AIRWING_MERGE_GROUPS","")+(groups.size()<2?"\n"+UiCommandLabels.text("AIRWING_MERGE_GROUPS_BLOCKED",""):"");
        button(g,out,"","nv_btn_merge",leftButtons+u.size(45),y+u.size(4),u.size(14.5),u.size(14.5),merge,()->airGroups("merge",wings));
        airPolicy(g,u,out,wings,x+u.size(178),y+u.size(4));
        button(g,out,"","role_icon_bg",x+u.size(193.5),y+u.size(4),u.size(15.5),u.size(15.5),"비행단 특화 설정은 아직 지원하지 않습니다.",()->{},true);
        button(g,out,"","aggressivness_btn",x+u.size(207.5),y+u.size(4),u.size(15.5),u.size(15.5),UiCommandLabels.text("AGGRESSIVE_CLICK_TO","§G클릭§!하여 공격성 변경")+"\n\n공군 공격성 설정은 아직 지원하지 않습니다.",()->{},true);
        var times=wings.stream().filter(wing->wing.data()!=null&&wing.data().air()!=null&&wing.data().air().wing()!=null).map(wing->wing.data().air().wing().sortieTime()).distinct().toList();String timeTip=times.stream().map(time->UiCommandLabels.text("AIRWING_MISSION_"+(time.equals("BOTH")?"DAY_NIGHT":time),"")).collect(java.util.stream.Collectors.joining("\n"));button(g,out,"","strategicair_day_night",x+u.size(222.5),y+u.size(4),u.size(15.5),u.size(15.5),timeTip+"\n\n"+(wings.size()==1?UiCommandLabels.text("DAY_NIGHT_CLICK_TO","§G클릭§!하여 기간 변경"):"여러 비행단의 출격 시간 동시 변경은 아직 지원하지 않습니다."),()->{if(wings.size()==1&&times.size()==1)MapCommanderOrders.send("AIR_SETTING/SORTIE_NEXT",List.of(wings.getFirst().id(),"sortie/"+times.getFirst()),List.of());});if(times.size()==1)UiAssets.draw(g,source("strategicair_day_night_icons").replace("frame_0","frame_"+List.of("DAY","NIGHT","BOTH").indexOf(times.getFirst())),x+u.size(222.5),y+u.size(4),u.size(15.5),u.size(15.5));
        button(g,out,"","air_base_select_all_btn",x+u.size(237.5),y+u.size(4),u.size(16),u.size(16),UiCommandLabels.text("AIR_SELECT_ALL_REGION","모두 선택")+"\n\n지역의 전체 비행단 선택은 아직 지원하지 않습니다.",()->{},true);
        button(g,out,"","uv_btn_unassign",x+u.size(282.5),y+u.size(4),u.size(14.5),u.size(14.5),UiCommandLabels.text("REMOVE_FROM_AIR_GROUP","")+(groups.isEmpty()?"\n"+UiCommandLabels.text("AIRWING_NEED_ONE_GROUPED_SELECTED",""):""),()->airGroups("remove",wings));
        button(g,out,"","select_half",x+u.size(252.5),y+u.size(4),u.size(14.5),u.size(14.5),UiCommandLabels.text(wings.size()>1?"SELECT_HALF_WINGS":"SELECT_HALF_WINGS_BLOCKED","절반 선택"),()->{if(wings.size()>1){MapUnitHud.clearSelection();selectWings(ids.subList(0,(ids.size()+1)/2));wingList=true;}});
        button(g,out,"","hold",x+u.size(267.5),y+u.size(4),u.size(14.5),u.size(14.5),UiCommandLabels.text("HOLD_WINGS",""),()->{var values=new ArrayList<String>();values.add("mission/HOLD");values.addAll(ids);MapCommanderHud.open(new Open(Mode.AIR_TOOLBAR,"",values));});
        button(g,out,"×","nv_btn_close",x+u.size(297.5),y+u.size(4),u.size(14.5),u.size(14.5),"선택 해제",MapCommanderPanels::close);
        y+=h+u.size(10);int count=Math.max(1,(bottom-y-u.size(15))/h);scroll=Math.clamp(scroll,0,Math.max(0,wings.size()-1));
        int shown=0;String group="";for(var wing:wings.subList(scroll,Math.min(wings.size(),scroll+count))){String base=MapCommanderAirBases.identity(wing);boolean header=!group.equals(base);if(y+h*(header?2:1)>bottom)break;if(header){group=base;airBaseHeader(g,u,out,wing,x,y,w,h);y+=h;}member(g,u,out,wing,x,y,w,h,"av_wing_entry_bg");y+=h;shown++;}
        pager(g,u,out,x,y,w,scroll>0,scroll+shown<wings.size(),Math.max(1,shown));
    }
}
