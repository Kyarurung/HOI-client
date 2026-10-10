package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

final class ScreenCommanderRoster extends ScreenDialog implements dev.hoi.client.input.InputSidebarMovement.InputMovementScreen {
    private final Screen parent;
    private final Consumer<ProtocolDialogPayloads.Action> sender;
    private final Consumer<ProtocolCommanderHud.PlanMapPick> mapSender;
    private final ScreenLandPlanGesture landGesture=new ScreenLandPlanGesture();
    private boolean pending,nativeRows,showAssigned=true,descending=true;
    private int left,top,pane,bottom,scroll,listTop,rowHeight,sort;
    private String query="";
    private final Set<String> longDetails=new HashSet<>();
    private String detailText="";
    private int detailRow=-1,detailScroll,detailX,detailY;
    private UiTooltips.ScrolledTooltip detailTooltip;
    UiTooltips.ScrolledTooltip detailTooltip(){return detailTooltip;}
    String detailText(){return detailText;}
    private void clearDetail(){detailText="";detailRow=-1;detailScroll=0;detailTooltip=null;}
    private boolean overDetail(double x,double y){
        if(detailRow<0)return false;
        int rowY=listTop+detailRow*rowHeight-scroll;
        return x>=left+5&&x<left+pane-10&&y>=rowY&&y<rowY+rowHeight-2
                ||detailTooltip!=null&&x>=detailTooltip.x()-4&&x<detailTooltip.x()+detailTooltip.width()+4&&y>=detailTooltip.y()-4&&y<detailTooltip.y()+detailTooltip.height()+4;
    }
    private EditBox search;
    private final Map<String,AbstractWidget> choiceButtons=new HashMap<>();
    AbstractWidget choiceButton(String id){return choiceButtons.get(id);}
    private List<ProtocolDialogView.Tile> rows=List.of();
    ScreenCommanderRoster(ProtocolDialogView view,Screen parent){this(view,parent,null);}
    ScreenCommanderRoster(ProtocolDialogView view,Screen parent,Consumer<ProtocolDialogPayloads.Action> sender){this(view,parent,sender,null);}
    ScreenCommanderRoster(ProtocolDialogView view,Screen parent,Consumer<ProtocolDialogPayloads.Action> sender,Consumer<ProtocolCommanderHud.PlanMapPick> mapSender){super(view);this.parent=parent;this.sender=sender;this.mapSender=mapSender;}
    Screen backdrop(){return parent;}
    void dismiss(){ScreenDialogCoordinator.setContent(parent);}
    @Override public boolean allowsMovement(){return true;}
    @Override public int unitHudLeft(){return parent instanceof dev.hoi.client.input.InputSidebarMovement.InputMovementScreen sidebar?sidebar.unitHudLeft():0;}
    @Override void update(ProtocolDialogView next){if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;pending=false;landGesture.clear();super.update(next);}
    private void submit(String id){
        if(pending||view().token().isEmpty()||sender==null&&!ClientPlayNetworking.canSend(ProtocolDialogPayloads.Action.TYPE))return;
        pending=true;children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).forEach(w->w.active=false);
        var action=new ProtocolDialogPayloads.Action(view().token(),view().revision(),id);if(sender==null)ClientPlayNetworking.send(action);else sender.accept(action);
    }
    private String rowIcon(ProtocolDialogView.Tile tile) {
        if(!view().image().equals("mission/view"))return tile.icon();
        if(tile.icon().startsWith("unit_hud/navy"))return "map_selector/navy";
        if(!tile.icon().equals("unit_hud/air_fighter"))return tile.icon();
        String source=switch(tile.section()) {
            case "mission/SUPERIORITY" -> "air_superiority";
            case "mission/INTERCEPTION" -> "interception";
            case "mission/CLOSE_AIR_SUPPORT" -> "cas";
            case "mission/NAVAL_STRIKE" -> "naval_bomber";
            case "mission/PORT_STRIKE" -> "port_strike";
            case "mission/STRATEGIC_BOMBING" -> "strategic_bomber";
            case "mission/LOGISTICS_STRIKE" -> "attack_logistics";
            case "mission/AIR_SUPPLY" -> "air_supply";
            case "mission/RECON" -> "recon";
            case "mission/NAVAL_PATROL" -> "naval_patrol";
            case "mission/MINELAYING" -> "naval_mines_planting";
            case "mission/MINESWEEPING" -> "naval_mines_sweeping";
            default -> "";
        };
        String icon=UiEquipmentDesigns.productionMissionIcon(source);
        return icon.isEmpty()?"map_selector/air":icon;
    }
    private void sortBy(int column){if(sort==column)descending=!descending;else{sort=column;descending=column!=1;}scroll=0;rebuildWidgets();}
    private void header(String text,int x,int w,int column){addRenderableWidget(new UiMenuButton(text,left+x,top+20,w,13,()->sortBy(column)));}
    @Override protected void init(){
        choiceButtons.clear();longDetails.clear();clearDetail();
        nativeRows=view().tiles().stream().anyMatch(t->t.leader()!=null);rowHeight=nativeRows?49:62;
        if(nativeRows){pane=Math.min(275,width-12);left=Math.clamp(width/2-113,6,Math.max(6,width-pane-6));top=Math.max(UiMenuBar.height(width),height/2-148);bottom=Math.min(height-8,top+304);listTop=top+38;}
        else{left=parent==null?0:UiPanelLayout.width(ProtocolMenuTab.OFFICER_CORPS,width);top=UiMenuBar.height(width);pane=Math.min(290,width-left-6);if(pane<180){pane=Math.min(290,width-12);left=Math.max(0,width-pane-6);}bottom=Math.min(height-8,top+360);listTop=top+52;}
        addRenderableWidget(new UiMenuButton("×",left+pane-18,top+3,14,14,this::onClose));
        rows=view().tiles().stream().filter(t->!t.section().startsWith("configuration/")||!t.detail().isBlank()).toList();
        if(nativeRows){
            header("능력",5,24,0);header("이름",46,46,1);header("수용량",220,43,6);
            for(int i=0;i<4;i++)header(List.of("공","방","계","군").get(i),161+i*13,12,2+i);
            search=new EditBox(font,left+100,top+22,58,11,Component.literal("검색"));search.setMaxLength(128);search.setBordered(false);search.setHint(Component.literal("검색"));search.setValue(query);
            search.setResponder(v->{query=v;scroll=0;rebuildWidgets();setFocused(search);});addRenderableWidget(search);
            var toggle=new UiMenuButton(showAssigned?"☑":"☐",left+pane-37,top+3,15,14,()->{showAssigned=!showAssigned;scroll=0;rebuildWidgets();});toggle.setTooltip(UiTooltips.createOfficer("임명된 지휘관 보기"));addRenderableWidget(toggle);
            Comparator<ProtocolDialogView.Tile> order=switch(sort){case 1->Comparator.comparing(ProtocolDialogView.Tile::name);case 6->Comparator.comparingInt(t->t.leader().capacity());case 2,3,4,5->Comparator.comparingInt(t->t.leader().abilities().get(sort-2));default->Comparator.comparingInt(t->t.leader().skill());};
            if(descending)order=order.reversed();String lower=query.toLowerCase(Locale.ROOT);
            rows=view().tiles().stream().filter(t->t.leader()!=null&&(showAssigned||!t.leader().assigned())&&t.name().toLowerCase(Locale.ROOT).contains(lower)).sorted(order.thenComparing(ProtocolDialogView.Tile::name).thenComparing(ProtocolDialogView.Tile::section)).toList();
            if(view().choices().stream().anyMatch(c->c.id().equals("remove"))){var tile=view().tiles().stream().filter(t->t.section().equals("remove")).findFirst().orElseThrow();var remove=new UiMenuButton("배정 해제",left+pane-116,bottom-17,109,14,()->submit("remove"));remove.active=!pending;remove.setTooltip(UiTooltips.createOfficer(UiCommandLabels.withoutShortcuts(tile.detail())));addRenderableWidget(remove);}
        }
        int end=nativeRows?bottom-20:bottom;scroll=Math.clamp(scroll,0,Math.max(0,rows.size()*rowHeight-(end-listTop)));
        for(int i=0;i<rows.size();i++){
            var tile=rows.get(i);int y=listTop+i*rowHeight-scroll;if(y<listTop||y+rowHeight-2>end)continue;
            boolean choice=view().choices().stream().anyMatch(c->c.id().equals(tile.section()));
            if(!nativeRows&&!choice&&UiTooltips.exactLines(font,UiCommandLabels.withoutShortcuts(tile.detail()),204).size()>8)longDetails.add(tile.section());
            var button=new UiMenuButton(tile.name(),left+5,y,pane-(nativeRows?20:10),rowHeight-2,()->submit(tile.section())).caption("");button.active=!pending&&choice;button.setTooltip(nativeRows||longDetails.contains(tile.section())?null:UiTooltips.createOfficer(UiCommandLabels.withoutShortcuts(tile.detail())));addRenderableWidget(button);if(choice)choiceButtons.put(tile.section(),button);
        }
        if(view().choices().stream().anyMatch(c->c.id().equals("confirm")))addRenderableWidget(new UiMenuButton("확인",left+pane-70,top+28,63,19,()->submit("confirm")));
        if(pending)children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).forEach(w->w.active=false);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        if(nativeRows)UiAssets.draw(g,"commander/ui/panel",left,top,pane,(int)Math.round(pane*607.0/550));else UiMenuStyle.panel(g,left,top,pane,bottom-top+3);UiText.centered(g,font,view().title(),left+5,top+3,pane-44,nativeRows?14:19,UiMenuStyle.TEXT);
        if(!nativeRows)UiText.centered(g,font,view().body(),left+6,top+27,pane-12,19,UiMenuStyle.MUTED);
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable widget)widget.extractRenderState(g,mx,my,delta);
        if(nativeRows){
            UiAssets.draw(g,"commander/ui/sort_skill",left+8,top+20,15,14);
            var example=rows.stream().filter(t->t.leader()!=null).findFirst();boolean navy=example.isPresent()&&example.get().leader().role().equals("제독");
            for(int i=0;i<4;i++)UiAssets.draw(g,"commander/ui/"+List.of("attack","defense",navy?"maneuver":"planning",navy?"coordination":"logistics").get(i),left+161+i*13,top+20,13,13);
        }
        String hovered="";
        if(!overDetail(mx,my))clearDetail();
        int end=nativeRows?bottom-20:bottom;
        for(int i=0;i<rows.size();i++){
            var tile=rows.get(i);int y=listTop+i*rowHeight-scroll;if(y<listTop||y+rowHeight-2>end)continue;
            if(nativeRows){
                var leader=tile.leader();
                if(mx>=left+5&&mx<left+pane-15&&my>=y&&my<y+rowHeight-2) {
                    hovered=UiCommandLabels.withoutShortcuts(tile.detail());
                    if(!leader.abilityTooltips().isEmpty()&&mx>=left+161&&mx<left+213&&my>=y+3&&my<y+25)hovered=leader.abilityTooltips().get((mx-left-161)/13);
                }
                String background=leader.role().equals("제독")?"navy":leader.role().equals("원수")?"marshal":"army";
                if(leader.assigned())background+="_assigned";else if(background.equals("army")&&tile.value().equals("사용 불가"))background="army_unavailable";
                UiAssets.draw(g,"commander/ui/"+background,left+6,y,255,49);
                if(tile.value().equals("선택 중"))g.fill(left+6,y+1,left+261,y+47,0x303F6D1D);
                UiText.centered(g,font,Integer.toString(leader.skill()),left+7,y+7,18,20,leader.role().equals("원수")?0xFFFFD34B:UiMenuStyle.TEXT);
                UiAssets.draw(g,tile.icon(),left+27,y+2,33,44);
                UiText.centered(g,font,leader.role(),left+62,y+2,93,10,UiMenuStyle.TEXT);UiText.centered(g,font,tile.name(),left+62,y+12,93,12,UiMenuStyle.TEXT);
                for(int a=0;a<4;a++){
                    UiAssets.draw(g,"commander/ui/"+List.of("attack","defense",leader.role().equals("제독")?"maneuver":"planning",leader.role().equals("제독")?"coordination":"logistics").get(a),left+161+a*13,y+3,12,12);
                    UiText.centered(g,font,Integer.toString(leader.abilities().get(a)),left+161+a*13,y+15,12,10,UiMenuStyle.TEXT);
                }
                UiText.centered(g,font,Integer.toString(leader.capacity()),left+220,y+5,43,13,UiMenuStyle.TEXT);
                if(leader.traitDetails().isEmpty())UiText.centered(g,font,String.join(" · ",leader.traits()),left+62,y+28,93,15,UiMenuStyle.MUTED);
                else for(int traitIndex=0;traitIndex<leader.traitDetails().size();traitIndex++) {
                    var trait=leader.traitDetails().get(traitIndex);int traitX=left+63+traitIndex*12;if(traitX+12>left+159)break;
                    if(!trait.icon().isEmpty())UiAssets.draw(g,trait.icon(),traitX,y+30,12,16);
                    else UiText.centered(g,font,trait.name(),traitX,y+30,12,16,UiMenuStyle.MUTED);
                    if(mx>=traitX&&mx<traitX+12&&my>=y+30&&my<y+46)hovered=trait.tooltip();
                }
                UiText.centered(g,font,tile.value(),left+162,y+29,101,14,tile.value().equals("사용 불가")?0xFFE15B5B:0xFFFFAA00);
            }else{
                if(detailRow<0&&longDetails.contains(tile.section())&&mx>=left+5&&mx<left+pane-10&&my>=y&&my<y+rowHeight-2){detailText=UiCommandLabels.withoutShortcuts(tile.detail());detailRow=i;detailX=width-left-pane>=204?left+pane:left>=204?left-216:mx;detailY=my;}
                UiAssets.draw(g,rowIcon(tile),left+8,y+3,43,52);UiText.centered(g,font,tile.name(),left+57,y+5,pane-66,23,UiMenuStyle.TEXT);UiText.centered(g,font,tile.value(),left+57,y+30,pane-66,20,tile.value().equals("사용 불가")||tile.value().equals("승진 불가")?0xFFE15B5B:0xFFFFAA00);}
        }
        landGesture.draw(g);
        if(detailRow>=0){detailTooltip=UiTooltips.drawOfficerScrollable(g,font,detailText,detailX,detailY,width,height,detailScroll,204);detailScroll=detailTooltip.scroll();}
        if(!hovered.isEmpty())UiTooltips.drawExact(g,font,hovered,mx,my,180);
    }
    private boolean mapPicking(){return view().image().startsWith("mission/land-target/")&&!view().token().isEmpty();}
    private void sendMapPick(boolean toggle) {
        var rays=landGesture.finish(minecraft);if(rays.isEmpty())return;
        pending=true;children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).forEach(w->w.active=false);
        var request=new ProtocolCommanderHud.PlanMapPick(view().token(),view().revision(),toggle,rays);
        if(mapSender==null)ClientPlayNetworking.send(request);else mapSender.accept(request);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy) {
        if(landGesture.active()&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){landGesture.sample(minecraft,Math.clamp(event.x(),left+pane,width),Math.clamp(event.y(),top,height),width,height,false);return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if(landGesture.active()&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){landGesture.sample(minecraft,Math.clamp(event.x(),left+pane,width),Math.clamp(event.y(),top,height),width,height,true);sendMapPick(false);return true;}
        return super.mouseReleased(event);
    }
    @Override public void removed(){landGesture.clear();super.removed();}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){
        if(nativeRows&&!pending&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            for(int i=0;i<rows.size();i++) {
                var tile=rows.get(i);int rowY=listTop+i*rowHeight-scroll;
                String choice="traits/"+tile.section();
                if(rowY<listTop||rowY+rowHeight-2>bottom-20)continue;
                if(event.x()>=left+62&&event.x()<left+159&&event.y()>=rowY+28&&event.y()<rowY+47&&view().choices().stream().anyMatch(c->c.id().equals(choice))) {submit(choice);return true;}
            }
        }
        if((event.modifiers()&com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT)!=0
                &&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT
                &&view().choices().stream().anyMatch(c->c.id().equals("mission/EXERCISE_UNTIL_TRAINED"))) {
            var exercise=choiceButtons.get("mission/EXERCISE");
            if(exercise!=null&&exercise.active&&exercise.isMouseOver(event.x(),event.y())){submit("mission/EXERCISE_UNTIL_TRAINED");return true;}
        }
        if(mapPicking()&&event.x()>=left+pane&&event.y()>=top) {
            if(pending)return true;
            if(dev.hoi.client.map.MapCommanderHud.clickAt(event.x(),event.y(),event.button(),twice))return true;
            if(mapSender==null&&!ClientPlayNetworking.canSend(ProtocolCommanderHud.PlanMapPick.TYPE))return true;
            if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT){if(landGesture.begin(minecraft,event.x(),event.y(),width,height))sendMapPick(true);return true;}
            if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){landGesture.begin(minecraft,event.x(),event.y(),width,height);return true;}
        }
if(event.x()<left||event.x()>=left+pane||event.y()<top||event.y()>=bottom){if(parent!=null&&event.x()<unitHudLeft()){onClose();return parent.mouseClicked(event,twice);}return dev.hoi.client.input.InputMapInfo.clickAt(minecraft,event.x(),event.y(),width,height);}return super.mouseClicked(event,twice);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(detailTooltip!=null&&overDetail(x,y)){detailScroll=Math.clamp(detailScroll-(int)(vertical*3*(font.lineHeight+1)),0,detailTooltip.maxScroll());return true;}
        if(x<left||x>=left+pane)return false;scroll-=(int)(vertical*rowHeight);rebuildWidgets();return true;
    }
    @Override public void onClose(){submit("");dismiss();}
}
