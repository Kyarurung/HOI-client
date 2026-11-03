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
    private boolean pending,nativeRows,descending=true,showAssigned=true;
    private boolean promoting(){return view().tiles().size()==1&&view().tiles().getFirst().section().equals("promote")&&view().choices().stream().anyMatch(c->c.id().equals("cancel"));}
    private String leaderChoice(String prefix,ProtocolDialogView.Tile tile){return prefix+(tile.section().startsWith("admiral/")?tile.section().substring(8):tile.section());}
    private int left,top,pane,bottom,scroll,listTop,rowHeight,sort;
    private boolean dragging;
    private UiScrollBar scrollbar(){return new UiScrollBar(left+pane-11,listTop-1,bottom-30,bottom-30-listTop,rows.size()*rowHeight);}
    private int maximumScroll(){return scrollbar().maximum();}
    private void dragScroll(double mouseY){scroll=scrollbar().offset(mouseY);rebuildWidgets();}
    private String query="",renderedTooltip="";
    String renderedTooltip(){return renderedTooltip;}
    private final Set<String> longDetails=new HashSet<>();
    private String detailText="";
    private int detailRow=-1,detailScroll;
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
        if(id.equals("remove")&&view().choices().stream().noneMatch(c->c.id().equals(id)))return;
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
    private void rowButton(String id,String label,String tooltip,int x,int y,int w){
        var button=new UiMenuButton(label,x,y,w,14,()->submit(id)).caption("").background("none").textShadow(false);
        button.active=!pending&&view().choices().stream().anyMatch(c->c.id().equals(id));
        button.setTooltip(UiTooltips.createOfficer(tooltip));choiceButtons.put(id,button);addRenderableWidget(button);
    }
    private void sortBy(int column){if(sort==column)descending=!descending;else{sort=column;descending=column!=1;}scroll=0;rebuildWidgets();}
    private void rowButtonText(GuiGraphicsExtractor g,AbstractWidget button){UiAssets.drawTinted(g,"commander/ui/button",button.getX(),button.getY(),button.getWidth(),button.getHeight(),button.getMessage().getString().equals("진급")&&!view().image().equals("commander/group")?(button.active?0xFF6A2B33:0xFF242424):0xFF8E8E8E);float scale=Math.max(UiText.scale(font),1+1f/font.lineHeight);g.pose().pushMatrix();g.pose().translate(button.getX()+(button.getWidth()-font.width(button.getMessage())*scale)/2,button.getY()+(button.getHeight()-font.lineHeight*scale)/2);g.pose().scale(scale);g.text(font,button.getMessage(),0,0,0xFFFFFFFF,false);g.pose().popMatrix();}
    private void skill(GuiGraphicsExtractor g,int value,int x,int y){var text=Component.literal(Integer.toString(value)).withStyle(net.minecraft.ChatFormatting.BOLD);float scale=2;g.pose().pushMatrix();g.pose().translate(x+(20-font.width(text)*scale)/2,y);g.pose().scale(scale);g.text(font,text,0,0,0xFFFFFFFF,false);g.pose().popMatrix();}
    private void label(GuiGraphicsExtractor g,Component text,int x,int y,int w,int h,int color){var shown=font.width(text)>w?net.minecraft.network.chat.FormattedText.composite(font.substrByWidth(text,Math.max(0,w-font.width("…"))),net.minecraft.network.chat.FormattedText.of("…")):text;g.text(font,net.minecraft.locale.Language.getInstance().getVisualOrder(shown),x,y+Math.max(0,(h-font.lineHeight)/2),color,false);}
    private void header(String text,int x,int w,int column){addRenderableWidget(new UiMenuButton(text,left+x,top+20,w,13,()->sortBy(column)));}
    @Override protected void init(){
        choiceButtons.clear();longDetails.clear();clearDetail();
        nativeRows=view().tiles().stream().anyMatch(t->t.leader()!=null);rowHeight=nativeRows?49:62;
        if(nativeRows){pane=Math.min(275,width-12);left=Math.clamp(width/2-113,6,Math.max(6,width-pane-6));top=Math.max(UiMenuBar.height(width),height/2-148);bottom=Math.min(height-8,top+304);listTop=top+38;}
        else{left=parent==null?0:UiPanelLayout.width(ProtocolMenuTab.OFFICER_CORPS,width);top=UiMenuBar.height(width);pane=Math.min(290,width-left-6);if(pane<180){pane=Math.min(290,width-12);left=Math.max(0,width-pane-6);}bottom=Math.min(height-8,top+360);listTop=top+52;}
        addRenderableWidget(new UiMenuButton("×",left+pane-18,top+3,14,14,this::onClose));
        rows=view().tiles().stream().filter(t->!t.section().startsWith("configuration/")||!t.detail().isBlank()).toList();
        if(nativeRows&&!promoting()){
            var assigned=new UiMenuButton("임명된 지휘관 보기",left+pane-101,top+3,80,14,()->{showAssigned=!showAssigned;scroll=0;rebuildWidgets();}).caption(view().tiles().stream().anyMatch(t->t.leader()!=null&&t.leader().role().equals("제독"))?"":"임명된 지…").background("none").sound("ui.checkbox");
            assigned.setTooltip(UiTooltips.createOfficer("임명된 지휘관 보기"));addRenderableWidget(assigned);
            addRenderableWidget(new UiMenuButton("기술 정렬",left+8,top+20,13,14,()->sortBy(0)).caption("").background("commander/ui/sort_skill").sound("ui.scroll"));
            addRenderableWidget(new UiMenuButton("고문 역할 정렬",left+21,top+20,13,14,()->sortBy(7)).caption("").background("commander/ui/sort_adviser").sound("ui.scroll"));
            header("이름",46,46,1);header("수용량",220,43,6);
            for(int i=0;i<4;i++)header(List.of("공","방","계","군").get(i),161+i*13,12,2+i);
            search=new EditBox(font,left+100,top+22,58,11,Component.literal("검색"));search.setMaxLength(128);search.setBordered(false);search.setHint(Component.literal("검색"));search.setValue(query);
            search.setResponder(v->{query=v;scroll=0;rebuildWidgets();setFocused(search);});addRenderableWidget(search);
            Comparator<ProtocolDialogView.Tile> order=switch(sort){case 1->Comparator.comparing(ProtocolDialogView.Tile::name);case 7->Comparator.<ProtocolDialogView.Tile>comparingInt(t->t.leader().advisorState()).thenComparingInt(t->t.leader().skill());case 6->Comparator.comparingInt(t->t.leader().capacity());case 2,3,4,5->Comparator.comparingInt(t->t.leader().abilities().get(sort-2));default->Comparator.comparingInt(t->t.leader().skill());};
            if(descending)order=order.reversed();String lower=query.toLowerCase(Locale.ROOT);
            if(sort!=7&&(view().image().equals("commander/group")||view().image().equals("commander/view"))) {
                String preferred=view().image().equals("commander/group")?"원수":"장군";
                var rank=Comparator.<ProtocolDialogView.Tile>comparingInt(t->t.leader().role().equals(preferred)?0:1);
                order=view().image().equals("commander/group")?order.thenComparing(rank):rank.thenComparing(order);
            }
            rows=view().tiles().stream().filter(t->t.leader()!=null&&(showAssigned||!t.leader().assigned())&&t.name().toLowerCase(Locale.ROOT).contains(lower)).sorted(order).toList();
            var officers=new UiMenuButton(view().image().equals("navy/admirals")?"함선 지휘관 보기":"야전 장교 보기",left+pane-268,bottom-25,131,17,()->submit(view().image().equals("navy/admirals")?"captains":"officers")).background("mio/ui/gfx_button_261x34/frame_0").textShadow(false);officers.active=!pending&&view().choices().stream().anyMatch(c->c.id().equals(view().image().equals("navy/admirals")?"captains":"officers"));choiceButtons.put(view().image().equals("navy/admirals")?"captains":"officers",officers);addRenderableWidget(officers);
            var remove=new UiMenuButton("배정 해제",left+pane-125,bottom-26,111,17,()->submit("remove")).background("mio/ui/gfx_button_221x34/frame_0").textShadow(false);remove.active=!pending;view().tiles().stream().filter(t->t.section().equals("remove")).findFirst().ifPresent(t->remove.setTooltip(UiTooltips.createOfficer(UiCommandLabels.withoutShortcuts(t.detail()))));choiceButtons.put("remove",remove);addRenderableWidget(remove);
        }
        if(promoting()){
            top=Math.max(UiMenuBar.height(width),height/2-85);bottom=top+170;listTop=top+25;
            clearWidgets();addRenderableWidget(new UiMenuButton("×",left+pane-18,top+3,14,14,this::onClose));
            rowButton("promote","진급",view().body(),left+pane/2-65,bottom-23,60);
            rowButton("cancel","취소","",left+pane/2+5,bottom-23,60);
        }
        int end=nativeRows?bottom-30:bottom;scroll=Math.clamp(scroll,0,nativeRows?maximumScroll():Math.max(0,rows.size()*rowHeight-(end-listTop)));
        for(int i=0;i<rows.size();i++){
            var tile=rows.get(i);int y=listTop+i*rowHeight-scroll;if(nativeRows?y+rowHeight<=listTop||y>=end:y<listTop||y+rowHeight-2>end)continue;
            boolean choice=view().choices().stream().anyMatch(c->c.id().equals(tile.section()));
            if(!nativeRows&&!choice&&UiTooltips.exactLines(font,UiCommandLabels.withoutShortcuts(tile.detail()),204).size()>8)longDetails.add(tile.section());
            var button=new UiMenuButton(tile.name(),left+5,Math.max(listTop,y),pane-(nativeRows?20:10),Math.min(y+rowHeight-2,end)-Math.max(listTop,y),()->submit(tile.section())).caption("");button.active=!pending&&choice&&!promoting();button.setTooltip(nativeRows||longDetails.contains(tile.section())?null:UiTooltips.createOfficer(UiCommandLabels.withoutShortcuts(tile.detail())));addRenderableWidget(button);if(choice&&!promoting())choiceButtons.put(tile.section(),button);
            if(nativeRows&&!promoting()){
                String details=leaderChoice("traits/",tile);
                rowButton(details,"세부 사항",view().choices().stream().filter(c->c.id().equals(details)).map(ProtocolDialogView.Choice::tooltip).findFirst().orElse("지휘관 세부 사항을 봅니다."),left+165,y+28,48);
                if(!tile.leader().promotion().isEmpty())rowButton(leaderChoice("promote/",tile),"진급",tile.leader().promotion(),left+213,y+28,48);
                if(!tile.leader().advisor().isEmpty()){
                    String advisor=leaderChoice("advisor/",tile);
                    boolean appointed=!tile.leader().advisorIcon().isEmpty();
                    var control=new UiMenuButton("장교단 역할",left+(appointed?8:13),y+(appointed?23:33),appointed?24:14,appointed?24:14,()->submit(advisor)).caption("").background("none");
                    control.active=!pending&&view().choices().stream().anyMatch(c->c.id().equals(advisor));control.setTooltip(UiTooltips.createOfficer(tile.leader().advisor()));choiceButtons.put(advisor,control);addRenderableWidget(control);
                }
            }
        }
        if(view().choices().stream().anyMatch(c->c.id().equals("confirm")))addRenderableWidget(new UiMenuButton("확인",left+pane-70,top+28,63,19,()->submit("confirm")));
        if(nativeRows&&!promoting())for(var entry:choiceButtons.entrySet())if(entry.getKey().startsWith("traits/")||entry.getKey().startsWith("promote/")||entry.getKey().startsWith("advisor/")){var b=entry.getValue();b.active&=b.getY()>=listTop&&b.getY()+b.getHeight()<=bottom-30;}
        if(pending)children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).forEach(w->w.active=false);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        if(nativeRows&&!promoting())UiAssets.draw(g,"commander/ui/panel",left,top,pane,(int)Math.round(pane*607.0/550));else UiMenuStyle.panel(g,left,top,pane,bottom-top+3);UiText.leftSingleLine(g,font,UiTooltips.officerText(view().title(),true),left+7,top+3,nativeRows&&!promoting()?pane-110:pane-30,nativeRows?14:19,0xFFFFFFFF);
        if(!nativeRows)UiText.centered(g,font,view().body(),left+6,top+27,pane-12,19,UiMenuStyle.MUTED);
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable widget){if(nativeRows&&!promoting()&&child instanceof AbstractWidget b&&b.getY()>=listTop&&b.getY()<bottom-30){g.enableScissor(left+5,listTop,left+pane-12,bottom-30);widget.extractRenderState(g,my>=listTop&&my<bottom-30?mx:-1,my,delta);g.disableScissor();}else widget.extractRenderState(g,mx,my,delta);}
        if(nativeRows&&!promoting())UiAssets.draw(g,"commander/commands/division_list_column_button/frame_7",left+14,top+20,9,8);
        if(nativeRows&&!promoting()){
            UiAssets.draw(g,"commander/commands/generic_checkbox/frame_"+(showAssigned?1:0),left+pane-36,top+3,15,14);
            var example=rows.stream().filter(t->t.leader()!=null).findFirst();boolean navy=example.isPresent()&&example.get().leader().role().equals("제독");
            if(navy){g.text(font,"임명된 지휘관",left+pane-101,top+1,UiMenuStyle.TEXT,false);g.text(font,"보기",left+pane-101,top+10,UiMenuStyle.TEXT,false);}
            for(int i=0;i<4;i++)UiAssets.draw(g,"commander/ui/"+List.of("attack","defense",navy?"maneuver":"planning",navy?"coordination":"logistics").get(i),left+161+i*13,top+20,13,13);
        }
        String hovered="";
        if(!overDetail(mx,my))clearDetail();
        int end=nativeRows?bottom-30:bottom;
        if(nativeRows&&!promoting())g.enableScissor(left+5,listTop,left+pane-12,end);
        for(int i=0;i<rows.size();i++){
            var tile=rows.get(i);int y=listTop+i*rowHeight-scroll;if(nativeRows?y+rowHeight<=listTop||y>=end:y<listTop||y+rowHeight-2>end)continue;
            if(nativeRows){
                var leader=tile.leader();
                if(mx>=left+5&&mx<left+pane-15&&my>=y&&my<y+rowHeight-2) {
                    hovered=leader.assignTooltip();
                    if(mx>=left+34&&mx<left+155&&my<y+27||mx>=left+34&&mx<left+67)hovered=UiCommandLabels.withoutShortcuts(tile.detail());
                    if(mx>=left+9&&mx<left+30&&my>=y+5&&my<y+29)hovered=leader.skillTooltip();
                    if(mx>=left+220&&mx<left+260&&my>=y+3&&my<y+25)hovered=leader.capacityTooltip();
                    if(!leader.abilityTooltips().isEmpty()&&mx>=left+161&&mx<left+213&&my>=y+3&&my<y+25)hovered=leader.abilityTooltips().get((mx-left-161)/13);
                }
                String background=leader.role().equals("제독")?"navy":leader.role().equals("원수")?"marshal":"army";
                if(leader.assigned())background+="_assigned";else if(background.equals("army")&&tile.value().equals("사용 불가"))background="army_unavailable";
                UiAssets.draw(g,"commander/ui/"+background,left+6,y,255,49);
                if(tile.value().equals("선택 중"))g.fill(left+6,y+1,left+261,y+47,0x303F6D1D);
                skill(g,leader.skill(),left+9,y+9);
                UiAssets.draw(g,tile.icon(),left+34,y+3,33,44);
                label(g,UiTooltips.officerText(leader.role(),true),left+70,y+6,85,9,0xFFFFFFFF);label(g,UiTooltips.officerText(tile.name(),true),left+70,y+15,85,9,0xFFFFFFFF);
                for(int a=0;a<4;a++){
                    UiAssets.draw(g,"commander/ui/"+List.of("attack","defense",leader.role().equals("제독")?"maneuver":"planning",leader.role().equals("제독")?"coordination":"logistics").get(a),left+161+a*13,y+5,14,13);
                    UiText.centered(g,font,Integer.toString(leader.abilities().get(a)),left+160+a*13,y+15,15,9,0xFFFFFFFF,false);
                }
                String capacity=Integer.toString(leader.capacity());
                if(leader.role().equals("제독"))UiText.centered(g,font,capacity,left+224,y+5,34,19,0xFFFFFFFF,false);
                else {g.text(font,capacity,left+245-font.width(capacity),y+10,0xFFFFFFFF,false);g.pose().pushMatrix();g.pose().translate(left+246,y+7);g.pose().scale(.375f);UiAssets.draw(g,"commander/commands/medal_collection/frame_0",0,0,16,28);g.pose().popMatrix();}
                if(leader.traitDetails().isEmpty())UiText.centered(g,font,String.join(" · ",leader.traits()),left+70,y+28,95,16,UiMenuStyle.MUTED);
                else for(int traitIndex=0;traitIndex<leader.traitDetails().size();traitIndex++) {
                    var trait=leader.traitDetails().get(traitIndex);int traitX=UiFlagStack.left(traitIndex,leader.traitDetails().size(),left+70,94,12),visibleWidth=UiFlagStack.visibleWidth(traitIndex,leader.traitDetails().size(),left+70,94,12);
                    if(!trait.icon().isEmpty())UiAssets.draw(g,trait.icon(),traitX,y+28,12,16);
                    else UiText.centered(g,font,trait.name(),traitX,y+28,12,16,UiMenuStyle.MUTED);
                    if(mx>=traitX&&mx<traitX+visibleWidth&&my>=y+28&&my<y+44)hovered=trait.tooltip();
                }
                if(!promoting())for(String prefix:List.of("traits/","promote/","advisor/")){
                    var control=choiceButtons.get(leaderChoice(prefix,tile));
                    if(control!=null){control.extractRenderState(g,mx,my,delta);if(prefix.equals("advisor/"))UiAssets.drawLeaderControl(g,leader.advisorIcon().isEmpty()?"commander/commands/give_role_leader_strip/frame_0":leader.advisorIcon(),control.getX(),control.getY(),control.getWidth(),control.getHeight(),true);else rowButtonText(g,control);if(UiTooltips.contains(control,mx,my))hovered=prefix.equals("advisor/")?leader.advisor():prefix.equals("promote/")?leader.promotion():"지휘관 세부 사항을 봅니다.";}
                }
            }else{
                if(detailRow<0&&longDetails.contains(tile.section())&&mx>=left+5&&mx<left+pane-10&&my>=y&&my<y+rowHeight-2){detailText=UiCommandLabels.withoutShortcuts(tile.detail());detailRow=i;}
                UiAssets.draw(g,rowIcon(tile),left+8,y+3,43,52);UiText.centered(g,font,tile.name(),left+57,y+5,pane-66,23,UiMenuStyle.TEXT);UiText.centered(g,font,tile.value(),left+57,y+30,pane-66,20,tile.value().equals("사용 불가")||tile.value().equals("승진 불가")?0xFFE15B5B:0xFFFFAA00);}
        }
        if(nativeRows&&!promoting())g.disableScissor();
        if(my<listTop||my>=end)hovered="";
        if(promoting()){
            for(String id:List.of("promote","cancel"))rowButtonText(g,choiceButtons.get(id));
            var lines=font.split(UiKeywordIcons.decorateLines(UiTooltips.officerText(view().body()),false),pane-22);int lineY=listTop+rowHeight+8;
            for(var line:lines){g.text(font,line,left+11,lineY,UiMenuStyle.TEXT,false);lineY+=11;}
        }
        if(nativeRows&&!promoting())scrollbar().draw(g,scroll);
        landGesture.draw(g);
        if(detailRow>=0){detailTooltip=UiTooltips.drawOfficerScrollable(g,font,detailText,mx,my,width,height,detailScroll,204);detailScroll=detailTooltip.scroll();}
        renderedTooltip=hovered;
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
        if(dragging){dragScroll(event.y());return true;}
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&dev.hoi.client.map.MapCommanderHud.dragAt(event.x()))return true;
        if(landGesture.active()&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){landGesture.sample(minecraft,Math.clamp(event.x(),left+pane,width),Math.clamp(event.y(),top,height),width,height,false);return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if(dragging){dragging=false;return true;}
        if(dev.hoi.client.map.MapCommanderHud.releaseAt(event.x(),event.button()))return true;
        if(landGesture.active()&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){landGesture.sample(minecraft,Math.clamp(event.x(),left+pane,width),Math.clamp(event.y(),top,height),width,height,true);sendMapPick(false);return true;}
        return super.mouseReleased(event);
    }
    @Override public void removed(){dev.hoi.client.map.MapCommanderHud.cancelDrag();landGesture.clear();super.removed();}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){
        if(nativeRows&&!promoting()&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&scrollbar().contains(event.x(),event.y())){if(scrollbar().arrow(event.y())){scroll=scrollbar().step(event.y(),scroll,rowHeight);rebuildWidgets();}else{dragging=true;dragScroll(event.y());}return true;}
        if(nativeRows&&!promoting()&&event.y()>=bottom-30&&event.y()<bottom-26)return true;
        if(nativeRows&&event.y()>=listTop&&event.y()<bottom-30&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)for(var entry:choiceButtons.entrySet())if((entry.getKey().startsWith("traits/")||entry.getKey().startsWith("promote/")||entry.getKey().startsWith("advisor/"))&&UiTooltips.contains(entry.getValue(),event.x(),event.y())){if(entry.getValue().active){entry.getValue().playDownSound(minecraft.getSoundManager());submit(entry.getKey());}return true;}
        if(nativeRows&&!pending&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            for(int i=0;i<rows.size();i++) {
                var tile=rows.get(i);int rowY=listTop+i*rowHeight-scroll;
                String choice=leaderChoice("traits/",tile);
                if(rowY<listTop||rowY+rowHeight-2>bottom-20)continue;
                if(event.x()>=left+70&&event.x()<left+165&&event.y()>=rowY+28&&event.y()<rowY+47&&view().choices().stream().anyMatch(c->c.id().equals(choice))) {submit(choice);return true;}
            }
        }
        if(nativeRows&&!promoting()&&!pending&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&(event.modifiers()&com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT)!=0) {
            for(var tile:rows){var button=choiceButtons.get(tile.section());String deploy=leaderChoice("deploy/",tile);if(button!=null&&button.active&&UiTooltips.contains(button,event.x(),event.y())&&view().choices().stream().anyMatch(c->c.id().equals(deploy))){button.playDownSound(minecraft.getSoundManager());submit(deploy);return true;}}
        }
        if((event.modifiers()&com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT)!=0
                &&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT
                &&view().choices().stream().anyMatch(c->c.id().equals("mission/EXERCISE_UNTIL_TRAINED"))) {
            var exercise=choiceButtons.get("mission/EXERCISE");
            if(exercise!=null&&exercise.active&&exercise.isMouseOver(event.x(),event.y())){submit("mission/EXERCISE_UNTIL_TRAINED");return true;}
        }
        if(mapPicking()&&event.x()>=left+pane&&event.y()>=top) {
            if(pending)return true;
            if(dev.hoi.client.map.MapCommanderHud.pressAt(event.x(),event.y(),event.button(),twice))return true;
            if(mapSender==null&&!ClientPlayNetworking.canSend(ProtocolCommanderHud.PlanMapPick.TYPE))return true;
            if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT){if(landGesture.begin(minecraft,event.x(),event.y(),width,height,false))sendMapPick(true);return true;}
            if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){landGesture.begin(minecraft,event.x(),event.y(),width,height,false);return true;}
        }
if(event.x()<left||event.x()>=left+pane||event.y()<top||event.y()>=bottom){if(parent!=null&&event.x()<unitHudLeft()){onClose();return parent.mouseClicked(event,twice);}return dev.hoi.client.input.InputMapInfo.clickAt(minecraft,event.x(),event.y(),width,height);}return super.mouseClicked(event,twice);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(detailTooltip!=null&&overDetail(x,y)){detailScroll=Math.clamp(detailScroll-(int)(vertical*3*(font.lineHeight+1)),0,detailTooltip.maxScroll());return true;}
        if(x<left||x>=left+pane)return false;scroll-=(int)(vertical*rowHeight);rebuildWidgets();return true;
    }
    @Override public void onClose(){submit("");dismiss();}
}
