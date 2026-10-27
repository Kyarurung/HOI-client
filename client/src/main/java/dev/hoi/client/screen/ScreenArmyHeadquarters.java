package dev.hoi.client.screen;

import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import static dev.hoi.protocol.ProtocolIndustryPayloads.Action.*;

public final class ScreenArmyHeadquarters extends Screen implements InputSidebarMovement.InputMovementScreen {
    private final Screen parent;
    private final String token;
    private final Consumer<ProtocolIndustryPayloads.Request> transport;
    private ProtocolIndustryView view;
    private EditBox name;
    private String draftName="";
    private boolean pending,closed,confirmDiscard,closeAfterDiscard;
    private int left,listLeft,listWidth,designerWidth,designerTop,designerHeight,top,bottom,scroll,picker=-1,pickerScroll;
    private double scale;
    private final Map<net.minecraft.client.gui.components.AbstractWidget,String> scrollingTips=new LinkedHashMap<>();
    private UiTooltips.ScrolledTooltip tooltip;
    private String tooltipText="";
    private int tooltipScroll;
    UiTooltips.ScrolledTooltip detailTooltip(){return tooltip;}
    private boolean overTooltip(double x,double y){return tooltip!=null&&x>=tooltip.x()-4&&x<tooltip.x()+tooltip.width()+4&&y>=tooltip.y()-4&&y<tooltip.y()+tooltip.height()+4;}
    private void clearTooltip(){tooltip=null;tooltipText="";tooltipScroll=0;}
    private void scrollingTip(net.minecraft.client.gui.components.AbstractWidget widget,String text){widget.setTooltip(null);scrollingTips.put(widget,text);}
    public ScreenArmyHeadquarters(Screen parent){this(parent,UUID.randomUUID().toString(),null,request->{if(ClientPlayNetworking.canSend(ProtocolIndustryPayloads.Request.TYPE))ClientPlayNetworking.send(request);});}
    public ScreenArmyHeadquarters(Screen parent,String token,ProtocolIndustryView fixture,Consumer<ProtocolIndustryPayloads.Request> transport){super(Component.literal("육군 사령부 편제"));this.parent=parent;this.token=token;this.view=fixture;this.transport=transport;if(fixture!=null&&fixture.headquarters()!=null&&fixture.headquarters().draft()!=null)draftName=fixture.headquarters().draft().name();}
    public void open(){send(HQ_OPEN,"","",0);}
    public void update(ProtocolIndustryView next){
        if(closed||!next.session().equals(token))return;
        if(next.country().isEmpty()||view!=null&&!view.country().equals(next.country())){closed=true;ScreenDialogCoordinator.setContent(null);return;}
        if(view!=null&&next.revision()<=view.revision())return;
        var previous=draft();if(name!=null)draftName=name.getValue();view=next;pending=false;
        if(draft()!=null&&(previous==null||!previous.id().equals(draft().id())))draftName=draft().name();
        if(draft()==null){picker=-1;confirmDiscard=false;if(closeAfterDiscard){finishClose();return;}}
        rebuildWidgets();
    }
    private ProtocolArmyHeadquarters hq(){return view==null?null:view.headquarters();}
    private ProtocolArmyHeadquarters.Template draft(){return hq()==null?null:hq().draft();}
    private void send(ProtocolIndustryPayloads.Action action,String item,String other,int amount){if(closed||pending)return;pending=true;transport.accept(new ProtocolIndustryPayloads.Request(action,token,view==null?0:view.revision(),item,other,amount));if(minecraft!=null)rebuildWidgets();}
    private UiMenuButton button(String caption,int x,int y,int w,int h,Runnable action){var button=new UiMenuButton(caption,x,y,w,h,action);button.active=!pending;addRenderableWidget(button);return button;}
    private UiMenuButton button(String caption,int x,int y,int w,Runnable action){return button(caption,x,y,w,18,action);}
    private int s(int value){return (int)Math.round(value*scale);}
    private int x(int value){return left+s(value);}
    private int y(int value){return designerTop+s(value);}
    private UiMenuButton designerButton(String caption,int x,int y,int w,int h,Runnable action){return button(caption,x(x),y(y),s(w),Math.max(13,s(h)),action);}
    @Override protected void init(){
        scrollingTips.clear();clearTooltip();
        top=UiMenuBar.height(width)+2;bottom=Math.max(top+80,height-10);listWidth=Math.min(188,Math.max(100,width-8));listLeft=Math.min(Math.max(4,width-listWidth-4),parent==null?4:UiPanelLayout.width(ProtocolMenuTab.OFFICER_CORPS,width)+3);
        scale=Math.min(.5,Math.min((width-12)/1200.0,(height-top-8)/720.0));designerWidth=s(1200);designerHeight=s(720);left=(width-designerWidth)/2;designerTop=Math.max(top,(height-designerHeight)/2);name=null;
        if(confirmDiscard){int w=Math.min(300,designerWidth-16),cx=left+(designerWidth-w)/2,cy=designerTop+designerHeight/2;button("계속 편집",cx+8,cy+10,(w-24)/2,()->{confirmDiscard=false;closeAfterDiscard=false;rebuildWidgets();});button("초안 버리기",cx+w/2+4,cy+10,(w-24)/2,()->{confirmDiscard=false;send(HQ_DISCARD,"","",0);});return;}
        if(picker>=0&&draft()!=null){pickerWidgets();return;}
        if(draft()==null){
        var closeList=button("×",listLeft+listWidth-21,top+3,17,()->requestClose(true));closeList.active&=draft()==null;
        if(hq()==null)return;
        var fresh=button("새 설계",listLeft+listWidth/2,top+28,listWidth/2-6,()->send(HQ_NEW,"","",0));fresh.active&=draft()==null;
        var refresh=button("목록 갱신",listLeft+5,bottom-22,listWidth-10,()->send(REFRESH,"","",0));refresh.active&=draft()==null;
        int available=Math.max(0,bottom-top-110);scroll=Math.clamp(scroll,0,Math.max(0,hq().templates().size()*52-available));
        for(int i=0;i<hq().templates().size();i++){var row=hq().templates().get(i);int ry=top+54+i*52-scroll;if(ry<top+54||ry+48>bottom-50)continue;var edit=button("편집",listLeft+listWidth-76,ry+27,34,()->send(HQ_EDIT,row.id(),"",0));edit.active&=draft()==null;var copy=button("복제",listLeft+listWidth-39,ry+27,34,()->send(HQ_COPY,row.id(),"",0));copy.active&=draft()==null;copy.setTooltip(UiTooltips.createOfficer("원본 편제를 유지하고 새 ID의 초안을 만듭니다. 저장 전에는 실제 편제가 바뀌지 않습니다."));}
        }
        if(draft()==null)return;
        designerButton("×",1160,5,33,33,()->requestClose(false));
        name=new EditBox(font,x(150),y(81),s(300),Math.max(15,s(35)),Component.literal("HQ 편제 이름"));name.setMaxLength(80);name.setValue(draftName);name.setResponder(value->draftName=value);name.active=!pending;addRenderableWidget(name);
        for(int index=0;index<hq().slots().size();index++){
            var slot=hq().slots().get(index);int selected=index;int sx=slot.support()?50+82*slot.index():slot.index()==0?170:62+106*(slot.index()-1);int sy=slot.support()?520:slot.index()==0?307:411;
            var button=new UiMenuButton(slot.current().isEmpty()?(slot.reason().isEmpty()?"+":"잠김"):label(slot.current()),slot.current().isEmpty()?null:unitIcon(slot.current()),false,x(sx),y(sy),s(slot.support()?77:96),Math.max(13,s(36)),()->{picker=selected;pickerScroll=0;rebuildWidgets();});button.active=!pending;addRenderableWidget(button);
            button.active&=slot.reason().isEmpty();scrollingTip(button,slot.reason().isEmpty()?(slot.current().isEmpty()?"클릭하여 HQ 부대 추가":unitDetail(slot.current())+"\n\n클릭하여 편성 후보 선택"):slot.reason());
        }
        statsWidgets();
        adjustmentWidgets();
        designerButton("초기화",20,626,115,33,()->{draftName=draft().name();send(HQ_RESET,"","",0);});
        var save=designerButton("저장",487,677,225,31,()->send(HQ_SAVE,draftName,"",0));save.active&=hq().saveReason().isEmpty();save.setTooltip(UiTooltips.createOfficer(hq().saveReason().isEmpty()?"원본 편제 복제·이름 변경: 경험치 0. 저장은 HQ 부대를 생성하거나 배치하지 않습니다.":hq().saveReason()));
        designerButton("닫기",1048,677,126,31,()->requestClose(false));
    }
    private static int statColumn(String id){return Set.of("MANPOWER","TRAINING_TIME","FUEL_CAPACITY","FUEL_USAGE").contains(id)?2:Set.of("SOFT_ATTACK","HARD_ATTACK","AIR_ATTACK","DEFENSE","BREAKTHROUGH","ARMOR","PIERCING","HARDNESS","INITIATIVE","ENTRENCHMENT","EQUIPMENT_CAPTURE_RATE","COMBAT_WIDTH").contains(id)?1:0;}
    private static String statValue(ProtocolTankDesigner.Stat stat,boolean signed){return String.format(Locale.ROOT,signed?"%+.2f":"%.2f",stat.value()*(stat.percent()?100:1))+(stat.percent()?"%":stat.id().equals("SPEED")?" km/h":stat.id().equals("TRAINING_TIME")?"일":"");}
    private void statsWidgets(){
        if(hq().details()==null)return;int[] rows={0,0,0};
        for(var stat:hq().details().stats()){int col=statColumn(stat.id()),row=rows[col]++;var region=new UiHoverRegion(stat.name(),x(672+col*174),y(74+row*22),s(166),s(22));region.setTooltip(UiTooltips.createOfficer(stat.name()+": "+statValue(stat,false)+"\n\n현재 편성·연구·교리를 반영한 능력치입니다. 장비는 원본 기본 프로필 기준입니다."));addRenderableWidget(region);}
    }
    private void statText(GuiGraphicsExtractor g,String text,int x,int y,int width,int height){UiText.centeredSingleLine(g,font,Component.literal(text),x,y,width,height,UiMenuStyle.TEXT);}
    private void drawStats(GuiGraphicsExtractor g){
        String[] headers={"기본 능력치","전투 능력치","장비 비용"};for(int col=0;col<3;col++)statText(g,headers[col],x(672+col*174),y(47),s(166),s(24));
        if(hq().details()==null)return;int[] rows={0,0,0};
        for(var stat:hq().details().stats()){int col=statColumn(stat.id()),row=rows[col]++;statText(g,stat.name(),x(673+col*174),y(74+row*22),s(102),s(22));statText(g,statValue(stat,false),x(775+col*174),y(74+row*22),s(63),s(22));}
        int row=5;for(var equipment:new TreeMap<>(draft().equipment()).entrySet()){statText(g,label(equipment.getKey())+": "+equipment.getValue(),x(1020),y(74+row++*24),s(166),s(24));}
        statText(g,"기본 지휘력: "+number(draft().baseCommandPower()),x(1020),y(74+row*24),s(166),s(24));
    }
    private static String changeTooltip(List<ProtocolTankDesigner.Stat> changes){var lines=new ArrayList<String>();for(var stat:changes){boolean lower=Set.of("SUPPLY_USE","EXPERIENCE_LOSS","WEIGHT","MANPOWER","TRAINING_TIME","FUEL_USAGE","COMBAT_WIDTH").contains(stat.id());lines.add("§f"+stat.name()+": "+((stat.value()<0)==lower?"§a":"§c")+statValue(stat,true)+"§r");}return lines.isEmpty()?"":"\n\n편성 변경 시\n"+String.join("\n",lines);}
    private void adjustmentWidgets(){
        if(hq().details()==null)return;int index=0;
        for(var value:hq().details().adjustments()){
            int n=index++,row=n/4,columns=Math.min(4,hq().details().adjustments().size()-row*4),bx=x(686+(480-columns*112)/2+n%4*112),by=y(370+row*72);
            var region=new UiHoverRegion("HQ 지역 변동치 · "+value.id(),bx,by,s(105),s(66));region.setTooltip(UiTooltips.createExact(UiTerrainAdjustment.tooltip(value)));addRenderableWidget(region);
        }
    }
    private void drawAdjustments(GuiGraphicsExtractor g){
        g.fill(x(662),y(356),x(1192),y(614),0xFF101012);if(hq().details()==null)return;int index=0;
        for(var value:hq().details().adjustments()){
            int n=index++,row=n/4,columns=Math.min(4,hq().details().adjustments().size()-row*4),bx=x(686+(480-columns*112)/2+n%4*112),by=y(370+row*72);
            UiTerrainAdjustment.draw(g,value,bx+s(18),by+s(5),this::s);
        }
    }
    private void pickerWidgets(){
        if(picker>=hq().slots().size()){picker=-1;rebuildWidgets();return;}var slot=hq().slots().get(picker);int px=pickerX(),py=pickerY(),pw=pickerWidth(),ph=pickerHeight();
        button("편성 선택 취소",px+pw-108,py+5,102,()->{picker=-1;rebuildWidgets();});int visible=Math.max(1,(ph-38)/35);pickerScroll=Math.clamp(pickerScroll,0,Math.max(0,slot.choices().size()-visible));
        for(int i=pickerScroll;i<Math.min(slot.choices().size(),pickerScroll+visible);i++){var choice=slot.choices().get(i);var b=button(choice.id().isEmpty()?"제거":label(choice.id()),px+8,py+30+(i-pickerScroll)*35,pw-16,29,()->{picker=-1;send(HQ_SLOT,choice.id(),slot.support()?"support":"line",slot.index());});b.active&=choice.reason().isEmpty();var tooltip=new StringBuilder(choice.id().isEmpty()?"선택한 HQ 부대를 초안에서 제거합니다. 저장 전 실제 편제는 유지됩니다.":unitDetail(choice.id()));tooltip.append(changeTooltip(choice.changes()));if(!choice.reason().isEmpty())tooltip.append("\n\n").append(choice.reason());scrollingTip(b,tooltip.toString());}
    }
    private int pickerWidth(){return Math.min(300,designerWidth-16);}
    private int pickerHeight(){return Math.min(300,designerHeight-20);}
    private int pickerX(){return left+(designerWidth-pickerWidth())/2;}
    private int pickerY(){return designerTop+(designerHeight-pickerHeight())/2;}
    private void requestClose(boolean whole){if(pending)return;if(picker>=0){picker=-1;rebuildWidgets();return;}if(draft()!=null&&(hq().copy()||hq().dirty()||!draftName.equals(draft().name()))){confirmDiscard=true;closeAfterDiscard=whole;rebuildWidgets();}else if(draft()!=null){closeAfterDiscard=whole;send(HQ_DISCARD,"","",0);}else finishClose();}
    private void finishClose(){if(closed)return;transport.accept(new ProtocolIndustryPayloads.Request(CLOSE,token,view==null?0:view.revision(),"","",0));closed=true;ScreenDialogCoordinator.setContent(parent);}
    @Override public void onClose(){requestClose(draft()==null);}
    @Override public boolean allowsMovement(){return draft()==null&&!confirmDiscard;}
    @Override public int unitHudLeft(){return parent instanceof InputSidebarMovement.InputMovementScreen sidebar?sidebar.unitHudLeft():0;}
    @Override public boolean keyPressed(KeyEvent event){if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE){if(confirmDiscard){confirmDiscard=false;closeAfterDiscard=false;rebuildWidgets();}else onClose();return true;}return super.keyPressed(event);}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){if(overTooltip(event.x(),event.y()))return true;if(draft()==null&&!confirmDiscard&&event.x()>listLeft+listWidth)return dev.hoi.client.input.InputMapInfo.clickAt(minecraft,event.x(),event.y(),width,height);super.mouseClicked(event,twice);return true;}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(overTooltip(mx,my)){tooltipScroll=Math.clamp(tooltipScroll-(int)Math.round(vertical*3*(font.lineHeight+1)),0,tooltip.maxScroll());return true;}if(picker>=0){if(mx>=pickerX()&&mx<pickerX()+pickerWidth()&&my>=pickerY()&&my<pickerY()+pickerHeight()){pickerScroll=Math.max(0,pickerScroll-(int)Math.round(vertical));rebuildWidgets();}}else if(draft()==null&&!confirmDiscard&&mx>=listLeft&&mx<listLeft+listWidth){scroll=Math.max(0,scroll-(int)Math.round(vertical*24));rebuildWidgets();}return true;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        UiAssets.nineSlice(g,"officer/hq/list_background",listLeft,top,listWidth,bottom-top,8,1);UiAssets.draw(g,"officer/hq/list_header",listLeft,top,listWidth,51);UiText.centered(g,font,"육군 사령부 편제",listLeft+4,top+4,listWidth-29,18,UiMenuStyle.TEXT);
        if(hq()==null)g.text(font,pending?"불러오는 중…":"HQ 정보를 받지 못했습니다.",listLeft+6,top+54,UiMenuStyle.MUTED,false);
        else for(int i=0;i<hq().templates().size();i++){var row=hq().templates().get(i);int ry=top+54+i*52-scroll;if(ry<top+54||ry+48>bottom-50)continue;UiAssets.draw(g,"officer/hq/template",listLeft+4,ry,listWidth-8,48);UiText.centered(g,font,row.name(),listLeft+42,ry+7,listWidth-48,18,UiMenuStyle.TEXT);UiAssets.draw(g,icon(row),listLeft+8,ry+5,29,26);}
        if(view!=null&&!view.message().isBlank())paragraph(g,view.message(),listLeft+6,bottom-48,listWidth-12,22,UiMenuStyle.TEXT);
        if(draft()!=null){g.nextStratum();drawDesigner(g);}
        if(picker>=0&&draft()!=null){g.nextStratum();UiMenuStyle.panel(g,pickerX(),pickerY(),pickerWidth(),pickerHeight());g.text(font,"편성 선택",pickerX()+8,pickerY()+10,UiMenuStyle.TEXT,false);}
        if(confirmDiscard){g.nextStratum();int w=Math.min(300,designerWidth-16),cx=left+(designerWidth-w)/2,cy=designerTop+designerHeight/2;UiMenuStyle.panel(g,cx,cy-25,w,62);UiText.centered(g,font,"저장하지 않은 초안을 버리시겠습니까?",cx+7,cy-22,w-14,26,UiMenuStyle.TEXT);}
        boolean over=overTooltip(mx,my);for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable widget)widget.extractRenderState(g,over?-1:mx,over?-1:my,delta);
        String hovered=over?tooltipText:scrollingTips.entrySet().stream().filter(entry->UiTooltips.contains(entry.getKey(),mx,my)).map(Map.Entry::getValue).findFirst().orElse("");
        if(hovered.isEmpty())clearTooltip();else{if(!hovered.equals(tooltipText)){tooltipText=hovered;tooltipScroll=0;}tooltip=UiTooltips.drawOfficerScrollable(g,font,tooltipText,mx,my,width,height,tooltipScroll);tooltipScroll=tooltip.scroll();}
    }
    private void drawDesigner(GuiGraphicsExtractor g){
        UiAssets.draw(g,"officer/hq/designer",left,designerTop,designerWidth,designerHeight);UiAssets.draw(g,"officer/hq/header",x(12),y(45),s(637),s(110));UiAssets.draw(g,"officer/hq/composition",x(12),y(255),s(386),s(354));UiAssets.draw(g,"officer/hq/modifiers",x(396),y(255),s(252),s(354));
        UiText.centered(g,font,"육군 사령부 설계",x(340),y(3),s(490),s(35),UiMenuStyle.TEXT);UiAssets.draw(g,icon(draft()),x(24),y(60),s(86),s(66));if(name==null)UiText.centered(g,font,draftName,x(150),y(81),s(300),s(35),UiMenuStyle.TEXT);
        UiText.centered(g,font,"육군 사령부 구성",x(30),y(226),s(340),s(28),UiMenuStyle.TEXT);UiText.centered(g,font,"지휘관 혜택",x(401),y(226),s(235),s(28),UiMenuStyle.TEXT);
        UiText.centered(g,font,"대대",x(141),y(365),s(130),s(27),UiMenuStyle.TEXT);UiText.centered(g,font,"사령부 지원",x(40),y(483),s(340),s(27),UiMenuStyle.TEXT);
        paragraph(g,hq().details()==null?"혜택 정보 없음":hq().details().leaderBenefits().isEmpty()?"지휘관 혜택 없음":hq().details().leaderBenefits(),x(416),y(280),s(214),s(285),UiMenuStyle.TEXT);
        drawStats(g);
        drawAdjustments(g);
        paragraph(g,hq().saveReason().isEmpty()?"원본 편제 복제·이름 변경 경험치: 0":hq().saveReason(),x(156),y(626),s(870),s(40),UiMenuStyle.TEXT);
    }
    private void paragraph(GuiGraphicsExtractor g,String text,int x,int y,int w,int h,int color){for(var line:UiTooltips.split(font,Component.literal(text),Math.max(1,w))){if(h<font.lineHeight)break;g.text(font,line,x,y,color,false);y+=font.lineHeight+2;h-=font.lineHeight+2;}}
    private static String icon(ProtocolArmyHeadquarters.Template template){return template.battalions().isEmpty()?"":unitIcon(template.battalions().getFirst());}
    private static String unitIcon(String id){return "officer/hq/units/"+id+"/frame_0";}
    private String unitDetail(String id){return hq().details()==null?label(id):hq().details().units().getOrDefault(id,label(id));}
    private static String number(double value){return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();}
    private String label(String id){return hq()==null?id:hq().labels().getOrDefault(id,id);}
}
