package dev.hoi.client.screen.army;

import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.industry.*;
import dev.hoi.client.ui.tooltip.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import static dev.hoi.protocol.industry.IndustryPayloads.Action.*;

public final class ArmyHeadquarters extends Screen implements InputSidebarMovement.InputMovementScreen {
    private final Screen parent;
    private final String token;
    private final Consumer<IndustryPayloads.Request> transport;
    private IndustryView view;
    private EditBox name;
    private String draftName="";
    private boolean pending,closed,closeAfterDiscard,templatePicker,listDragging;
    private IndustryPayloads.Action pendingAction;
    private int templateScroll;
    private String pickerGroup="";
    private int left,listLeft,listWidth,designerWidth,designerTop,designerHeight,top,bottom,scroll,picker=-1,pickerScroll;
    private double scale;
    private final Map<net.minecraft.client.gui.components.AbstractWidget,String> scrollingTips=new LinkedHashMap<>();
    private final List<UiPanelButton> slotButtons=new ArrayList<>();
    private final List<UiMenuButton> listButtons=new ArrayList<>();
    private Tooltips.ScrolledTooltip tooltip;
    private String tooltipText="";
    private int tooltipScroll;
    Tooltips.ScrolledTooltip detailTooltip(){return tooltip;}
    private boolean overTooltip(double x,double y){return tooltip!=null&&x>=tooltip.x()-4&&x<tooltip.x()+tooltip.width()+4&&y>=tooltip.y()-4&&y<tooltip.y()+tooltip.height()+4;}
    private void clearTooltip(){tooltip=null;tooltipText="";tooltipScroll=0;}
    private void scrollingTip(net.minecraft.client.gui.components.AbstractWidget widget,String text){widget.setTooltip(null);scrollingTips.put(widget,text);}
    public ArmyHeadquarters(Screen parent){this(parent,UUID.randomUUID().toString(),null,request->{if(ClientPlayNetworking.canSend(IndustryPayloads.Request.TYPE))ClientPlayNetworking.send(request);});}
    public ArmyHeadquarters(Screen parent,String token,IndustryView fixture,Consumer<IndustryPayloads.Request> transport){super(Component.literal("육군 사령부 편제"));this.parent=parent;this.token=token;this.view=fixture;this.transport=transport;if(fixture!=null&&fixture.headquarters()!=null&&fixture.headquarters().draft()!=null)draftName=fixture.headquarters().draft().name();}
    public void open(){send(HQ_OPEN,"","",0);}
    public void update(IndustryView next){
        if(closed||!next.session().equals(token))return;
        if(next.country().isEmpty()||view!=null&&!view.country().equals(next.country())){closed=true;DialogCoordinator.setContent(null);return;}
        if(view!=null&&next.revision()<=view.revision())return;
        var previous=draft();if(name!=null)draftName=name.getValue();view=next;pending=false;
        if(draft()!=null&&(previous==null||!previous.id().equals(draft().id())||pendingAction==HQ_EDIT||pendingAction==HQ_NEW||pendingAction==HQ_COPY||pendingAction==HQ_RESET)){draftName=draft().name();picker=-1;pickerGroup="";}
        pendingAction=null;
        if(draft()==null){picker=-1;if(closeAfterDiscard){finishClose();return;}}
        rebuildWidgets();
    }
    private CommanderArmyHeadquarters hq(){return view==null?null:view.headquarters();}
    private CommanderArmyHeadquarters.Template draft(){return hq()==null?null:hq().draft();}
    private void send(IndustryPayloads.Action action,String item,String other,int amount){if(closed||pending)return;pending=true;pendingAction=action;templatePicker=false;transport.accept(new IndustryPayloads.Request(action,token,view==null?0:view.revision(),item,other,amount));if(minecraft!=null)rebuildWidgets();}
    private UiMenuButton button(String caption,int x,int y,int w,int h,Runnable action){var button=new UiMenuButton(caption,x,y,w,h,action);button.active=!pending;addRenderableWidget(button);return button;}
    private int s(int value){return (int)Math.round(value*scale);}
    private int x(int value){return left+s(value);}
    private int y(int value){return designerTop+s(value);}
    private UiMenuButton designerButton(String caption,int x,int y,int w,int h,Runnable action){return button(caption,x(x),y(y),s(w),Math.max(13,s(h)),action);}
    @Override protected void init(){
        scrollingTips.clear();slotButtons.clear();listButtons.clear();clearTooltip();
        if(parent!=null&&(parent.width!=width||parent.height!=height))parent.resize(width,height);
        top=HudMenuBar.height(width)+2;bottom=Math.min(height-4,top+Math.max(100,Math.round(height*.7f)));listWidth=Math.min(188,Math.max(100,width-8));listLeft=Math.min(Math.max(4,width-listWidth-4),parent==null?4:UiPanelLayout.width(MenuTab.OFFICER_CORPS,width)+3);
        scale=Math.min(.5,Math.min((width-12)/1200.0,(height-top-8)/720.0));designerWidth=s(1200);designerHeight=s(720);left=(width-designerWidth)/2;designerTop=Math.max(top,(height-designerHeight)/2);name=null;
        listWidgets();
        if(templatePicker&&draft()!=null){templateWidgets();return;}
        if(picker>=0&&draft()!=null){pickerWidgets();statsWidgets();adjustmentWidgets();return;}
        if(draft()==null)return;
        designerButton("HQ 설계 닫기",1156,6,32,33,()->requestClose(false)).background("menu/close").caption("").sound("ui.close");
        name=new EditBox(font,x(195),y(92),s(180),Math.max(10,s(20)),Component.literal("HQ 편제 이름"));name.setBordered(false);name.setMaxLength(80);name.setValue(draftName);name.setResponder(value->draftName=value);name.active=!pending;addRenderableWidget(name);
        for(int index=0;index<hq().slots().size();index++){
            var slot=hq().slots().get(index);int selected=index;int sx=slot.support()?50+82*slot.index():slot.index()==0?156:50+106*(slot.index()-1);int sy=slot.support()?541:slot.index()==0?311:415;
            String texture=slot.current().isEmpty()?"production/design/division/"+(slot.reason().isEmpty()?"add":"locked"):unitIcon(slot.current());
            var button=new UiPanelButton(slot.current().isEmpty()?(slot.reason().isEmpty()?"+":"잠김"):label(slot.current()),x(sx),y(sy),s(slot.support()?64:88),Math.max(13,s(slot.support()?32:42)),()->{
                if(!slot.support()&&slot.index()>0){send(HQ_SLOT,slot.current().isEmpty()?draft().battalions().getFirst():"","line",slot.current().isEmpty()?draft().battalions().size():slot.index());return;}
                picker=selected;pickerGroup="";pickerScroll=0;rebuildWidgets();
            }){
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
                    if(slot.support())UiAssets.draw(g,"production/design/division/support_slot",getX(),getY(),getWidth(),getHeight());
                    else g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/design/division/slot_atlas.png"),getX(),getY(),getX()+getWidth(),getY()+getHeight(),0,.5f,0,1);
                    int inset=slot.support()?0:s(12),iw=Math.min(s(76),getWidth()),ih=s(slot.support()?30:40);
                    String shown=active&&isHoveredOrFocused()&&!slot.current().isEmpty()?(slot.support()||slot.index()==0?"officer/hq/replace":"production/designer/remove"):texture;
                    UiAssets.drawAlignedContent(g,shown,shown,getX()+inset+(getWidth()-inset-iw)/2f-1,getY()+(getHeight()-ih)/2f-.5f,iw,ih);
                }
            };button.active=!pending;addRenderableWidget(button);slotButtons.add(button);
            button.active&=slot.reason().isEmpty();scrollingTip(button,slot.tooltip());
        }
        headerWidgets();
        statsWidgets();
        summary(null);
        costs(null);
        adjustmentWidgets();
        var reset=designerButton("초기화",20,620,123,34,()->{draftName=draft().name();send(HQ_RESET,"","",0);}).background("production/design/division/reset").textShadow(false);scrollingTip(reset,IndustryEquipmentDesigns.tooltip("DESIGNER_RESET_DESC"));
        var copy=designerButton("복제",150,620,123,36,()->send(HQ_COPY,draft().id(),"",0)).background("production/design/division/duplicate").textShadow(false);scrollingTip(copy,IndustryEquipmentDesigns.tooltip("DESIGNER_DUPLICATE_DESC"));copy.active&=hq().templates().stream().anyMatch(t->t.id().equals(draft().id()));
        var save=new UiPanelButton("저장",x(487),y(667),s(221),s(36),()->send(HQ_SAVE,draftName,"",0)){
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
                UiAssets.draw(g,"production/design/division/save",getX(),getY(),getWidth(),getHeight());
                var text=TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal(experienceLabel("DESIGNER_ARMY_SAVE",hq().experience())),false),false);
                UiText.centeredSingleLine(g,font,text,getX(),getY(),getWidth(),getHeight(),active?UiMenuStyle.TEXT:UiMenuStyle.MUTED);
            }
        };save.active=!pending&&hq().saveReason().isEmpty()&&!draftName.isBlank();addRenderableWidget(save);
        scrollingTip(save,hq().saveReason().isEmpty()?experienceLabel("DESIGNER_ARMY_SAVE",hq().experience()):hq().saveReason());
    }
    private void listWidgets(){
        var closeList=addRenderableWidget(UiMenuStyle.close("HQ 목록 닫기","menu/close",listLeft+listWidth-21.5f,top+5,16,16.5f,()->requestClose(true)));closeList.active=!pending;listButtons.add(closeList);
        if(hq()==null)return;
        var fresh=button("새 설계",listLeft+94,top+28,83,17,()->send(HQ_NEW,"","",0)).background("officer/hq/open_designer").caption("육군 사령부 설계");listButtons.add(fresh);
        scroll=Math.clamp(scroll,0,listScrollBar().maximum());
        for(int i=0;i<hq().templates().size();i++){
            var row=hq().templates().get(i);int ry=listRow(i);if(ry<top+52||ry+48>bottom-4)continue;
            listButtons.add(button("편집",listLeft+120,ry+29,35,13,()->send(HQ_EDIT,row.id(),"",0)).background("recruitment/button"));
            listButtons.add(button("HQ 편제 삭제 · "+row.name(),listLeft+155,ry+29,15,13,()->send(HQ_DELETE,row.id(),"",0)).background("recruitment/delete_template").caption(""));
        }
        for(var control:listButtons)control.active&=!templatePicker&&picker<0;
    }
    private void headerWidgets(){
        button("HQ 편제 목록",x(381),y(91),s(22),s(22),()->{templatePicker=true;templateScroll=0;rebuildWidgets();}).background("production/design/division/expand").caption("");
        for(int priority=0;priority<3;priority++){
            int value=priority;String key="TEMPLATE_PRIO_"+value;
            var control=button("HQ 우선순위 · "+value,x(499+36*value),y(value==2?103:104),s(36),s(37),()->send(HQ_PRIORITY,"","",value)).background("production/design/division/priority_"+(value+(draft().priority()==value?3:0))).caption("");
            control.setTooltip(Tooltips.createExact("§e"+IndustryEquipmentDesigns.tooltip(key)+"§f\n"+IndustryEquipmentDesigns.tooltip(key+"_DESC")));
        }
        var paradrop=new UiHoverRegion("HQ 공수 가능",x(405),y(120),s(26),s(24));paradrop.setTooltip(Tooltips.createExact(draft().paradropTooltip()));addRenderableWidget(paradrop);
        var special=new UiHoverRegion("HQ 특수부대",x(430),y(120),s(26),s(24));special.setTooltip(Tooltips.createExact(draft().specialForcesTooltip()));addRenderableWidget(special);
    }
    private void templateWidgets(){
        button("HQ 편제 목록",x(381),y(91),s(22),s(22),()->{templatePicker=false;rebuildWidgets();}).background("production/design/division/expand").caption("");
        templateScroll=Math.clamp(templateScroll,0,Math.max(0,hq().templates().size()-6));
        for(int i=templateScroll;i<Math.min(hq().templates().size(),templateScroll+6);i++){
            var row=hq().templates().get(i);
            button(row.name(),x(188),y(121+(i-templateScroll)*50),s(200),s(46),()->send(HQ_EDIT,row.id(),"",0));
        }
    }
    private static int statColumn(String id){return Set.of("MANPOWER","TRAINING_TIME","FUEL_CAPACITY","FUEL_USAGE").contains(id)?2:Set.of("SOFT_ATTACK","HARD_ATTACK","AIR_ATTACK","DEFENSE","BREAKTHROUGH","ARMOR","PIERCING","HARDNESS","INITIATIVE","ENTRENCHMENT","EQUIPMENT_CAPTURE_RATE","COMBAT_WIDTH").contains(id)?1:0;}
    private static String statValue(IndustryTankDesigner.Stat stat,boolean signed){return (signed&&stat.value()>0?"+":"")+IndustryEquipmentDesigns.divisionStatValue(new IndustryView.Stat(stat.name(),"",stat.value(),stat.percent()?"PERCENT":stat.id().equals("SPEED")?"KM_PER_HOUR":"",stat.id()));}
    private void summary(GuiGraphicsExtractor g){
        String[] ids={"HARDNESS","ORGANIZATION","HP","SPEED"};int[] positions={86,216,326,436},widths={100,80,80,115};
        for(int i=0;i<ids.length;i++){
            String id=ids[i];var stat=hq().details()==null?null:hq().details().stats().stream().filter(value->id.equals(value.id())).findFirst().orElse(null);
            String value=stat==null?"—":statValue(stat,false);int bx=x(positions[i]),by=y(192),w=s(widths[i]);
            if(g==null){var region=new UiHoverRegion("HQ 요약 · "+id,bx,by,w,s(24));region.setTooltip(Tooltips.createOfficer((stat==null?id:stat.name())+": "+value+"\n\n"+IndustryEquipmentDesigns.divisionStatField(id,"description")));addRenderableWidget(region);}
            else{UiAssets.nineSlice(g,"commander/commands/generic_background/frame_0",bx,by,w,s(24),4,.5);UiText.iconText(g,font,IndustryEquipmentDesigns.divisionStatField(id,"texture"),value,bx+1,by,w-2,s(24),s(20),Tooltips.GOLD|0xFF000000);}
        }
        if(g==null){var region=new UiHoverRegion("육군 경험치",x(297),y(628),s(70),s(24));region.setTooltip(Tooltips.createOfficer(IndustryEquipmentDesigns.tooltip("CURRENT_EXPERIENCE")));addRenderableWidget(region);}
        else UiText.iconText(g,font,"hud/army_experience",view.hud().armyExperience()==null?"—":ProtocolNumberBounds.fixed(view.hud().armyExperience(),0),x(297),y(628),s(70),s(24),s(20),UiMenuStyle.TEXT);
    }
    private void statsWidgets(){
        if(hq().details()==null)return;int[] rows={0,0,0};
        for(var stat:hq().details().stats()){if(stat.id().equals("HARDNESS"))continue;int col=statColumn(stat.id()),row=rows[col]++;var region=new UiHoverRegion(stat.name(),x(672+col*174),y(74+row*20),s(166),s(20));region.setTooltip(Tooltips.createOfficer(stat.name()+": "+statValue(stat,false)+"\n\n"+IndustryEquipmentDesigns.divisionStatField(stat.id(),"description")));addRenderableWidget(region);}
    }
    private void costs(GuiGraphicsExtractor g){
        var template=draft();String days=template.deployDays()>0?template.deployDays()+"일":"—",power=ProtocolNumberBounds.fixed(template.baseCommandPower(),0);
        String cost=IndustryEquipmentDesigns.tooltip(template.costMinimum()==template.costMaximum()?"DIVISION_PRODUCTION_COST_SINGLE_VAL":"DIVISION_PRODUCTION_COST_VAL")
                .replace("$VALUE|0$",java.math.BigDecimal.valueOf(template.costMinimum()).setScale(0,java.math.RoundingMode.DOWN).toPlainString()).replace("$MINVALUE|0$",java.math.BigDecimal.valueOf(template.costMinimum()).setScale(0,java.math.RoundingMode.DOWN).toPlainString()).replace("$MAXVALUE|0$",java.math.BigDecimal.valueOf(template.costMaximum()).setScale(0,java.math.RoundingMode.DOWN).toPlainString());
        if(g==null){
            String durationKey="HQ_DEPLOY_AND_WITHDRAW_DAYS"+(template.deployDays()==1?"_SINGULAR":"");if(!hq().labels().containsKey(durationKey))durationKey="HQ_DEPLOY_DAYS"+(template.deployDays()==1&&hq().labels().containsKey("HQ_DEPLOY_DAYS_SINGULAR")?"_SINGULAR":"");
            var time=new UiHoverRegion("HQ 배치 일수",x(380),y(623),s(90),s(29));time.setTooltip(Tooltips.createExact(template.deployDays()>0?label(durationKey).replace("$DAYS$",Integer.toString(template.deployDays())):"—"));addRenderableWidget(time);
            var command=new UiHoverRegion("HQ 배치 지휘력",x(480),y(623),s(60),s(29));command.setTooltip(Tooltips.createExact(label("HQ_DEPLOY_CP_COST").replace("$COST|H0$","§H"+power+"§!")));addRenderableWidget(command);
            var resources=new UiHoverRegion("HQ 생산 자원",x(548),y(628),Math.max(s(24),template.resources().size()*s(26)),s(24));resources.setTooltip(Tooltips.createExact(IndustryEquipmentDesigns.tooltip("DIVISION_PRODUCTION_RESOURCE_DESC")));addRenderableWidget(resources);
            var production=new UiHoverRegion("HQ 예상 생산 비용",x(700),y(628),s(470),s(25));production.setTooltip(Tooltips.createExact(IndustryEquipmentDesigns.tooltip("DIVISION_PRODUCTION_COST_DESC")));addRenderableWidget(production);
        }else{
            UiAssets.nineSlice(g,"commander/commands/generic_background/frame_0",x(380),y(623),s(90),s(29),4,.5);
            UiAssets.nineSlice(g,"commander/commands/generic_background/frame_0",x(480),y(623),s(60),s(29),4,.5);
            UiText.iconText(g,font,"commander/commands/leader_en_route/frame_0",days,x(380),y(623),s(90),s(29),s(24),UiMenuStyle.TEXT);
            UiText.iconText(g,font,"hud/command_power",power,x(480),y(623),s(60),s(29),s(24),UiMenuStyle.TEXT);
            for(int i=0;i<template.resources().size();i++)UiAssets.draw(g,"production/resources/"+template.resources().get(i).toLowerCase(Locale.ROOT),x(548+i*26),y(628),s(24),s(24));
            var line=TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal(cost),false),false);
            UiText.text(g,font,line,x(1170)-Math.round(font.width(line)*UiText.scale(font)),y(630),UiMenuStyle.TEXT);
        }
    }
    private void drawStats(GuiGraphicsExtractor g){
        String[] headers={"기본 능력치","전투 능력치","장비 비용"};for(int col=0;col<3;col++)UiText.centeredSingleLine(g,font,Tooltips.officerText(headers[col],true),x(672+col*174),y(47),s(166),s(24),0xFF000000);
        if(hq().details()==null)return;int[] rows={0,0,0};
        for(var stat:hq().details().stats()){if(stat.id().equals("HARDNESS"))continue;int col=statColumn(stat.id()),row=rows[col]++,bx=x(673+col*174),by=y(74+row*20),w=s(166);String value=statValue(stat,false),icon=IndustryEquipmentDesigns.divisionStatField(stat.id(),"texture");int valueWidth=Math.round(font.width(value)*UiText.scale(font)),iconWidth=icon.isEmpty()?0:s(20);if(!icon.isEmpty())UiAssets.draw(g,icon,bx,by+s(2),s(18),s(16));UiText.marquee(g,font,stat.name()+":",bx+iconWidth,by,w-valueWidth-iconWidth-3,UiText.scale(font),0xFF000000,false,-1);UiText.text(g,font,Component.literal(value).getVisualOrderText(),bx+w-valueWidth,by,0xFF000000,false);}
        int row=rows[2];for(var equipment:new TreeMap<>(draft().equipment()).entrySet()){
            if(equipment.getValue()<=0)continue;
            int bx=x(1021),by=y(74+row++*20),w=s(166);String value=Long.toString(equipment.getValue());int valueWidth=Math.round(font.width(value)*UiText.scale(font));
            UiText.marquee(g,font,label(equipment.getKey()),bx,by,w-valueWidth-3,UiText.scale(font),0xFF000000,false,-1);
            UiText.text(g,font,Component.literal(value).getVisualOrderText(),bx+w-valueWidth,by,0xFF000000,false);
        }
    }
    private void adjustmentWidgets(){
        if(hq().details()==null)return;int index=0;
        for(var value:hq().details().adjustments()){
            int n=index++,row=n/5,columns=Math.min(5,hq().details().adjustments().size()-row*5),bx=x(662+(530-((columns-1)*100+78))/2+n%5*100),by=y(370+row*72);
            var region=new UiHoverRegion("HQ 지역 변동치 · "+value.id(),bx,by,s(78),s(72));region.setTooltip(Tooltips.createExact(IndustryTerrainAdjustment.tooltip(value)));addRenderableWidget(region);
        }
    }
    private void drawAdjustments(GuiGraphicsExtractor g){
        UiAssets.nineSlice(g,"production/design/division/adjustment_panel",x(662),y(356),s(530),s(258),8,.5);if(hq().details()==null)return;int index=0;
        for(var value:hq().details().adjustments()){
            int n=index++,row=n/5,columns=Math.min(5,hq().details().adjustments().size()-row*5),bx=x(662+(530-((columns-1)*100+78))/2+n%5*100),by=y(370+row*72);
            IndustryTerrainAdjustment.draw(g,value,bx+s(4),by+s(4),this::s);
        }
    }
    private void pickerWidgets(){
        if(picker>=hq().slots().size()){picker=-1;rebuildWidgets();return;}var slot=hq().slots().get(picker);int px=pickerX(),py=pickerY();
        button("뒤로",px+s(506),py+s(2),s(123),Math.max(13,s(34)),this::pickerBack).background("commander/commands/button_123x34/frame_0");
        if(!slot.support()&&pickerGroup.isEmpty()){
            int index=0;
            var remove=slot.choices().stream().filter(c->c.id().isEmpty()).findFirst().orElse(null);
            if(remove!=null)pickerChoice(slot,remove,index++);
            for(String group:List.of("infantry","mobile","armor")){
                var choices=slot.choices().stream().filter(c->!c.id().isEmpty()&&c.group().equals(group)).toList();
                if(choices.isEmpty())continue;
                var representative=choices.stream().filter(c->c.id().equals(slot.current())).findFirst().orElse(choices.getFirst());
                String caption=label("group_"+group+"_title"),cost=slot.groups().containsKey(group)?experienceLabel("NEW_GROUP_ADDITIONAL_COST",slot.groups().get(group)):"";
                pickerCard(caption,unitIcon(representative.id()),cost,index++,true,()->{pickerGroup=group;pickerScroll=0;rebuildWidgets();});
            }
            return;
        }
        var choices=slot.choices().stream().filter(c->c.id().isEmpty()||slot.support()||c.group().equals(pickerGroup)).sorted(Comparator.comparing(c->!c.id().isEmpty())).toList();
        int visible=Math.max(1,s(453)/Math.max(1,s(69)));pickerScroll=Math.clamp(pickerScroll,0,Math.max(0,(choices.size()+1)/2-visible));
        for(int i=pickerScroll*2;i<Math.min(choices.size(),(pickerScroll+visible)*2);i++)pickerChoice(slot,choices.get(i),i-pickerScroll*2);
    }
    private void pickerChoice(CommanderArmyHeadquarters.Slot slot,CommanderArmyHeadquarters.Choice choice,int index){
        boolean selected=!choice.id().isEmpty()&&choice.id().equals(slot.current());
        String costKey=choice.id().isEmpty()?(choice.experience()==0?"DESIGNER_REMOVE_NO_COST":"DESIGNER_REMOVE_DESC"):slot.current().isEmpty()?"DESIGNER_ADD_DESC":choice.experience()==0?"DESIGNER_NO_COST":"DESIGNER_REPLACE_DESC";
        var button=pickerCard(choice.id().isEmpty()?"제거":label(choice.id()),choice.id().isEmpty()?"production/designer/remove":unitIcon(choice.id()),selected?"§G(현재 선택됨)§!":experienceLabel(costKey,choice.experience()),index,!selected&&choice.reason().isEmpty(),()->{picker=-1;send(HQ_SLOT,choice.id(),slot.support()?"support":"line",slot.index());});
        scrollingTip(button,choice.tooltip());
    }
    private UiPanelButton pickerCard(String caption,String icon,String detail,int index,boolean enabled,Runnable action){
        var button=new UiPanelButton(caption,pickerX()+s(72+index%2*256),pickerY()+s(54+index/2*69),s(250),s(66),action){
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
                UiAssets.nineSlice(g,"commander/commands/mini_tooltip/frame_0",getX(),getY(),getWidth(),getHeight(),8,.5);
                UiAssets.drawAlignedContent(g,icon,icon,getX()+s(9),getY()+s(17),s(76),s(42));
                UiText.marquee(g,font,Tooltips.officerText(caption),getX()+s(85),getY()+s(5)+(s(22)-UiText.MIN_HEIGHT)/2,s(158),UiText.scale(font),enabled?UiMenuStyle.TEXT:UiMenuStyle.MUTED,true,-1);
                UiText.leftSingleLine(g,font,TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal(detail),false),false),getX()+s(85),getY()+s(35),s(160),s(22),UiMenuStyle.TEXT);
            }
        };button.active=!pending&&enabled;addRenderableWidget(button);return button;
    }
    private void pickerBack(){if(!pickerGroup.isEmpty())pickerGroup="";else picker=-1;pickerScroll=0;rebuildWidgets();}
    private int pickerWidth(){return s(660);}
    private int listRow(int index){return top+52+(int)Math.round(index*47.5)-scroll;}
    private UiScrollBar listScrollBar(){return new UiScrollBar(listLeft+listWidth-10,top+52,bottom-4,bottom-top-56,hq()==null||hq().templates().isEmpty()?0:(int)Math.round((hq().templates().size()-1)*47.5)+48);}
    private int pickerHeight(){return s(580);}
    private int pickerX(){return x(0);}
    private int pickerY(){return y(40);}
    private void requestClose(boolean whole){if(pending)return;if(templatePicker){templatePicker=false;rebuildWidgets();return;}if(picker>=0){pickerBack();return;}if(draft()!=null){closeAfterDiscard=whole;send(HQ_DISCARD,"","",0);}else finishClose();}
    private void finishClose(){if(closed)return;transport.accept(new IndustryPayloads.Request(CLOSE,token,view==null?0:view.revision(),"","",0));closed=true;DialogCoordinator.setContent(parent);}
    @Override public void onClose(){requestClose(draft()==null);}
    @Override public boolean allowsMovement(){return draft()==null&&!pending;}
    @Override public int unitHudLeft(){return parent instanceof InputSidebarMovement.InputMovementScreen sidebar?sidebar.unitHudLeft():0;}
    @Override public boolean keyPressed(KeyEvent event){if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE){onClose();return true;}return super.keyPressed(event);}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){if(overTooltip(event.x(),event.y()))return true;if(event.modifiers()!=0){for(int index=0;index<slotButtons.size();index++){var button=slotButtons.get(index);if(!Tooltips.contains(button,event.x(),event.y()))continue;var slot=hq().slots().get(index);if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&button.active&&!slot.current().isEmpty()){int modifiers=event.modifiers();if((modifiers&com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL)!=0&&!slot.support()&&slot.index()==0)send(HQ_SLOT,"","line_all",0);else if((modifiers&com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT)!=0&&(slot.support()||slot.index()==0))send(HQ_SLOT,"",slot.support()?"support":"line",slot.index());}return true;}}if(templatePicker&&(event.x()<x(180)||event.x()>x(410)||event.y()<y(87)||event.y()>y(439))){templatePicker=false;rebuildWidgets();return true;}if((draft()==null||!insideDesigner(event.x(),event.y()))&&!pending&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&listScrollBar().maximum()>0&&listScrollBar().contains(event.x(),event.y())){if(listScrollBar().arrow(event.y()))scroll=listScrollBar().step(event.y(),scroll,20);else{listDragging=true;scroll=listScrollBar().offset(event.y());}rebuildWidgets();return true;}if(allowsMovement()&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT){if(parent!=null&&(event.x()<listLeft||event.y()<top)){finishClose();return parent.mouseClicked(event,twice);}if(event.x()>listLeft+listWidth||event.y()>=bottom)return dev.hoi.client.input.InputMapInfo.clickAt(minecraft,event.x(),event.y(),width,height);}if(draft()!=null&&insideDesigner(event.x(),event.y())){for(var child:children())if(!listButtons.contains(child)&&child.mouseClicked(event,twice)){setFocused(child);if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)setDragging(true);break;}return true;}super.mouseClicked(event,twice);return true;}
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){if(listDragging){scroll=listScrollBar().offset(event.y());rebuildWidgets();return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){if(listDragging){listDragging=false;return true;}return super.mouseReleased(event);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(overTooltip(mx,my)){tooltipScroll=Math.clamp(tooltipScroll-(int)Math.round(vertical*3*(font.lineHeight+1)),0,tooltip.maxScroll());return true;}if(templatePicker){templateScroll=Math.max(0,templateScroll-(int)Math.round(vertical));rebuildWidgets();}else if(picker>=0){if(mx>=pickerX()&&mx<pickerX()+pickerWidth()&&my>=pickerY()&&my<pickerY()+pickerHeight()){pickerScroll=Math.max(0,pickerScroll-(int)Math.round(vertical));rebuildWidgets();}}else if((draft()==null||!insideDesigner(mx,my))&&mx>=listLeft&&mx<listLeft+listWidth){scroll=Math.max(0,scroll-(int)Math.round(vertical*24));rebuildWidgets();}return true;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        UiAssets.tiledNineSlice(g,"officer/hq/list_background",listLeft,top,listWidth,bottom-top,64,.5);UiAssets.draw(g,"officer/hq/list_header",listLeft+1,top,188,51);UiText.centered(g,font,Tooltips.officerText("육군 사령부 편제",true),listLeft+10,top+4,158,18,UiMenuStyle.TEXT);
        if(hq()==null&&pending)g.text(font,"불러오는 중…",listLeft+6,top+54,UiMenuStyle.MUTED,false);
        else for(int i=0;i<hq().templates().size();i++){var row=hq().templates().get(i);int ry=listRow(i);if(ry<top+52||ry+48>bottom-4)continue;UiAssets.draw(g,"officer/hq/template",listLeft+5,ry,171,48);UiText.centered(g,font,row.name(),listLeft+61,ry+11,99,10,UiMenuStyle.TEXT);UiAssets.draw(g,icon(row),listLeft+11,ry+12,38,21);UiAssets.draw(g,"officer/hq/allowed/frame_2",listLeft+6,ry+7,13,12);UiAssets.draw(g,"production/design/division/deployment_priority_"+row.priority(),listLeft+41,ry+8,8,26);UiAssets.draw(g,"commander/commands/leader_en_route/frame_0",listLeft+56,ry+27,12,14);UiText.left(g,font,row.deployDays()>0?row.deployDays()+"일":"—",listLeft+67,ry+30,28,10,UiMenuStyle.TEXT);UiAssets.draw(g,"hud/command_power",listLeft+94,ry+29,11,11);UiText.left(g,font,ProtocolNumberBounds.fixed(row.baseCommandPower(),0),listLeft+105,ry+30,14,10,UiMenuStyle.TEXT);}
        if(listScrollBar().maximum()>0)listScrollBar().draw(g,scroll);
        for(var control:listButtons)control.extractRenderState(g,draft()!=null&&insideDesigner(mx,my)?-1:mx,draft()!=null&&insideDesigner(mx,my)?-1:my,delta);
        if(draft()!=null){g.nextStratum();drawDesigner(g);}
        if(picker>=0&&draft()!=null){g.nextStratum();UiAssets.nineSlice(g,"commander/commands/tiled_window_1b_border/frame_0",pickerX(),pickerY(),pickerWidth(),pickerHeight(),32,.5);UiText.left(g,font,IndustryEquipmentDesigns.tooltip("REGIMENT_WINDOW_DESC"),pickerX()+s(30),pickerY()+s(13),s(460),s(34),UiMenuStyle.TEXT);}
        if(templatePicker){g.nextStratum();UiAssets.nineSlice(g,"officer/hq/list_background",x(180),y(114),s(230),s(325),8,scale);}
        boolean over=overTooltip(mx,my);for(var child:children())if(!listButtons.contains(child)&&child instanceof net.minecraft.client.gui.components.Renderable widget)widget.extractRenderState(g,over?-1:mx,over?-1:my,delta);
        String hovered=over?tooltipText:scrollingTips.entrySet().stream().filter(entry->Tooltips.contains(entry.getKey(),mx,my)).map(Map.Entry::getValue).findFirst().orElse("");
        if(hovered.isEmpty())clearTooltip();else{if(!hovered.equals(tooltipText)){tooltipText=hovered;tooltipScroll=0;}tooltip=Tooltips.drawOfficerScrollable(g,font,tooltipText,mx,my,width,height,tooltipScroll);tooltipScroll=tooltip.scroll();}
    }
    private void drawDesigner(GuiGraphicsExtractor g){
        UiAssets.draw(g,"officer/hq/designer",left,designerTop,designerWidth,designerHeight);UiAssets.draw(g,"officer/hq/header",x(12),y(45),s(637),s(110));UiAssets.draw(g,"officer/hq/composition",x(12),y(255),s(386),s(354));UiAssets.draw(g,"officer/hq/modifiers",x(396),y(255),s(252),s(354));
        UiAssets.nineSlice(g,"commander/commands/tiled_header_1/frame_0",x(12),y(224),s(637),s(32),4,scale);
        UiText.centered(g,font,Tooltips.officerText("육군 사령부 설계",true),x(340),y(3),s(490),s(35),UiMenuStyle.TEXT);UiAssets.drawAlignedContent(g,icon(draft()),icon(draft()),x(38),y(80),s(76),s(42));if(name==null)UiText.left(g,font,draftName,x(195),y(92),s(180),s(20),UiMenuStyle.TEXT);
        UiAssets.draw(g,"production/design/division/priority_icon_"+draft().priority(),x(93),y(70),s(16),s(35));
        UiAssets.draw(g,"production/design/division/priority_bg",x(490),y(101),s(128),s(44));
        UiAssets.draw(g,"production/design/division/paradrop_"+(draft().paradrop()?1:0),x(405),y(120),s(26),s(24));
        UiAssets.draw(g,"production/design/division/special_"+(draft().specialForces()>0?1:0),x(430),y(120),s(26),s(24));
        UiText.centered(g,font,Tooltips.officerText("육군 사령부 구성",true),x(30),y(226),s(340),s(28),UiMenuStyle.TEXT);UiText.centered(g,font,Tooltips.officerText("지휘관 혜택",true),x(401),y(226),s(235),s(28),UiMenuStyle.TEXT);
        UiText.centered(g,font,Tooltips.officerText("대대",true),x(141),y(359),s(130),s(27),UiMenuStyle.TEXT);UiText.centered(g,font,Tooltips.officerText("사령부 지원",true),x(40),y(479),s(340),s(27),UiMenuStyle.TEXT);
        var combatWidth=hq().details()==null?null:hq().details().stats().stream().filter(stat->stat.id().equals("COMBAT_WIDTH")).findFirst().orElse(null);
        UiText.centered(g,font,combatWidth==null?"—":statValue(combatWidth,false),x(188),y(274),s(40),s(20),UiMenuStyle.TEXT);
        paragraph(g,hq().details()==null?"혜택 정보 없음":hq().details().leaderBenefits(),x(418),y(270),s(226),s(333),UiMenuStyle.TEXT);
        drawStats(g);
        summary(g);
        costs(g);
        drawAdjustments(g);
    }
    private boolean insideDesigner(double mx,double my){return mx>=left&&mx<left+designerWidth&&my>=designerTop&&my<designerTop+designerHeight;}
    private void paragraph(GuiGraphicsExtractor g,String text,int x,int y,int w,int h,int color){for(var line:Tooltips.split(font,TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal(text),false),false),Math.max(1,w))){if(h<font.lineHeight)break;g.text(font,line,x,y,color,false);y+=font.lineHeight+2;h-=font.lineHeight+2;}}
    private static String icon(CommanderArmyHeadquarters.Template template){return template.battalions().isEmpty()?"":unitIcon(template.battalions().getFirst());}
    private static String unitIcon(String id){return "officer/hq/units/"+id+"/frame_0";}
    private String label(String id){return hq()==null?id:hq().labels().getOrDefault(id,id);}
    private String experienceLabel(String key,int amount){return label(key).replace("$VALUE|H0$","§H"+amount+"§!").replace("$VALUE|Y0$","§Y"+amount+"§!").replace("$VALUE$",Integer.toString(amount));}
}
