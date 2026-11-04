package dev.hoi.client.screen.occupation;

import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.occupation.OccupationPayloads.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class OccupationScreen extends Screen implements InputSidebarMovement.InputMovementScreen {
    private record Entry(Row row,boolean country,int count,int offset) {}
    private record Control(UiMenuButton button,String texture,String icon,String caption,boolean left) {}
    private final String token;
    private final Consumer<Request> transport;
    private final Set<String> expanded=new HashSet<>();
    private final List<Control> controls=new ArrayList<>();
    private List<Entry> entries=List.of();
    private View view;
    private int pane,top,scroll,sort,pending,refresh,optionScroll;
    private boolean compliance,templates,showPeaceful=true;
    private String target;
    private Row returnTarget;
    private int returnScroll;
    private long returnRevision;
    private List<net.minecraft.util.FormattedCharSequence> returnLines=List.of();
    public OccupationScreen(){this(UUID.randomUUID().toString(),null,r->{if(ClientPlayNetworking.canSend(Request.TYPE))ClientPlayNetworking.send(r);});}
    OccupationScreen(String token,View view,Consumer<Request> transport){super(Component.literal("점령지"));this.token=token;this.view=view;this.transport=transport;}
    public void open(){send(Action.OPEN,"","");}
    public void update(View next){
        if(!token.equals(next.token())||view!=null&&next.revision()<=view.revision())return;
        if(next.country().isEmpty()||view!=null&&!view.country().equals(next.country())){minecraft.gui.setScreen(null);return;}
        view=next;pending=0;returnTarget=null;rebuildWidgets();
    }
    private int s(int value){return Math.round(value*pane/550f);}
    private float textScale(){return Math.max(1f,pane/550f*2);}
    private int headerLabelY(){return top+s(48)+(s(74)-Math.round(font.lineHeight*textScale())-s(4)-s(32))/2;}
    private int headerControlsY(){return headerLabelY()+Math.round(font.lineHeight*textScale())+s(4);}
    @Override protected void init(){
        pane=Math.max(180,UiPanelLayout.width(MenuTab.CONSTRUCTION,width));top=UiMenuBar.height(width);controls.clear();
        UiMenuBar.buttons(width,view==null?"":view.country(),MenuTab.POLITICS,t->{if(t==MenuTab.RESEARCH)HoiClient.open();else HoiClient.openMenu(t);}).forEach(this::addRenderableWidget);
        addRenderableWidget(new UiMenuButton("×",pane-s(43),top+s(7),s(34),s(34),this::onClose));
        if(view==null)return;
        rebuildEntries();
        add("주둔 사단 편제","occupation/template_button",templateIcon(view.defaultTemplate()),"",s(13),headerControlsY(),s(76),s(31),false,()->select("*",true)).setTooltip(UiTooltips.createExact(templateTooltip(null)));
        add(law(view.defaultLaw()),"occupation/select_law_button",lawIcon(view.defaultLaw()),law(view.defaultLaw()),s(93),headerControlsY(),s(169),s(32),false,()->select("*",false)).setTooltip(UiTooltips.createExact(currentLawTooltip(null,view.defaultLaw())));
        control("저항도 지도",compliance?"occupation/resistance_tab_inactive":"occupation/resistance_tab",null,"",446,68,47,54,()->{compliance=false;send(Action.RESISTANCE,"","");}).setTooltip(UiTooltips.createExact(label("OCCUPATION_RESISTANCE_TAB_TOOLTIP")));
        control("순응도 지도",compliance?"occupation/compliance_tab_active":"occupation/compliance_tab",null,"",493,79,47,43,()->{compliance=true;send(Action.COMPLIANCE,"","");}).setTooltip(UiTooltips.createExact(label("OCCUPATION_COMPLIANCE_TAB_TOOLTIP")));
        String[] labels={"이름","주둔군","경제","저항도","순응도"};
        String[] sorts={"NAME","GARRISON","ECONOMY","RESISTANCE","COMPLIANCE"};
        for(int i=0;i<5;i++){int index=i;control(labels[i],sort==i?"tension/sort":"tension/sort_selected",null,labels[i],14+i*106,128,100,29,()->{sort=index;scroll=0;rebuildWidgets();}).setTooltip(UiTooltips.createExact(label("OCCUPATION_SORT_"+sorts[i]+"_TOOLTIP")));}
        control("저항하지 않는 국가 보기",null,null,"",17,159,510,31,()->{showPeaceful=!showPeaceful;scroll=0;rebuildWidgets();}).sound("ui.checkbox");
        for(var entry:entries){
            int y=rowY(entry);if(!visible(entry,y))continue;var row=entry.row();
            if(entry.country()){
                add((expanded.contains(row.country())?"▾ ":"▸ ")+row.countryName(),null,null,"",s(12),y,s(282),s(45),false,()->{if(!expanded.add(row.country()))expanded.remove(row.country());rebuildWidgets();}).setTooltip(null);
                add((expanded.contains(row.country())?"점령지 접기 · ":"점령지 펼치기 · ")+row.countryName(),"occupation/"+(expanded.contains(row.country())?"collapse":"expand"),null,"",s(9),y+s(38),s(19),s(19),false,()->{if(!expanded.add(row.country()))expanded.remove(row.country());rebuildWidgets();});
                if(row.hasResistance())add("주둔 사단 편제","occupation/template_button",templateIcon(row.template()),"",s(18),y+s(84),s(76),s(31),false,()->select(row.state(),true)).setTooltip(UiTooltips.createExact(templateTooltip(entry)));
                var admin=row.administration();boolean returning=admin!=null&&admin.returnTerritory();String caption=returning?"영토 반환":"국가 해방";
                var release=add(caption,"occupation/release",null,caption,s(190),y+s(84),s(149),s(27),false,()->openReturn(row.country()));release.active=pending==0&&returning&&admin.enabled();
                release.setTooltip(UiTooltips.createExact(admin==null?"현재 게임 조건에서는 국가를 해방할 수 없습니다.":admin.tooltip()));
                if(row.hasResistance())add(law(row.law()),"occupation/select_law_button",lawIcon(row.law()),law(row.law()),s(346),y+s(84),s(169),s(32),false,()->select(row.state(),false)).setTooltip(UiTooltips.createExact(currentLawTooltip(entry,row.law())));
            }else{
                if(row.hasResistance())add("주둔 사단 편제","occupation/template_button",templateIcon(row.template()),"",s(18),y+s(28),s(76),s(31),false,()->select(row.state(),true)).setTooltip(UiTooltips.createExact(templateTooltip(entry)));
                if(row.hasResistance())add(law(row.law()),"occupation/state_law",lawIcon(row.law()),"",s(477),y+s(24),s(40),s(34),false,()->select(row.state(),false)).setTooltip(UiTooltips.createExact(currentLawTooltip(entry,row.law())));
            }
        }
        if(target!=null)buildPopup();
        if(returnTarget!=null)buildReturn();
    }
    private void rebuildEntries(){
        Comparator<Row> order=switch(sort){case 1->Comparator.comparingDouble(Row::garrison).reversed();case 2->Comparator.comparingInt(Row::factories).reversed();case 3->Comparator.comparingDouble(Row::resistance).reversed();case 4->Comparator.comparingDouble(Row::compliance).reversed();default->Comparator.comparing(Row::name);};
        var groups=new LinkedHashMap<String,List<Row>>();
        for(var row:view.rows())groups.computeIfAbsent(row.country(),key->new ArrayList<>()).add(row);
        var parents=new ArrayList<Row>();
        for(var group:groups.values()){
            var first=group.getFirst();if(!showPeaceful&&group.stream().allMatch(r->r.resistance()==0))continue;
            double resistance=group.stream().filter(Row::hasResistance).mapToDouble(Row::resistance).average().orElse(0),compliance=group.stream().filter(Row::hasResistance).mapToDouble(Row::compliance).average().orElse(0);
            parents.add(new Row("country:"+first.country(),first.countryName(),first.country(),first.countryName(),resistance,compliance,
                    group.stream().mapToDouble(Row::garrison).sum(),group.stream().mapToInt(Row::factories).sum(),
                    group.stream().allMatch(r->r.template().equals(first.template()))?first.template():"",group.stream().allMatch(r->r.law().equals(first.law()))?first.law():"",
                    format("OCCUPATION_BAR_CORE_RESISTANCE_TOOLTIP",Map.of("COUNTRY",first.countryName(),"RESISTANCE",decimal(resistance*100,1)+"%")),
                    format("OCCUPATION_BAR_CORE_COMPLIANCE_TOOLTIP",Map.of("COUNTRY",first.countryName(),"COMPLIANCE",decimal(compliance*100,1)+"%")),first.resistanceEffects(),first.complianceEffects(),0,0,0,groupDetails(group),groupAdministration(group)));
        }
        parents.sort(order.thenComparing(Row::country));var result=new ArrayList<Entry>();int offset=0;
        for(var parent:parents){var group=groups.get(parent.country());result.add(new Entry(parent,true,group.size(),offset));offset+=124;if(expanded.contains(parent.country()))for(var row:group.stream().sorted(order.thenComparing(Row::state)).toList()){result.add(new Entry(row,false,1,offset));offset+=row.hasResistance()?63:33;}}
        entries=List.copyOf(result);scroll=Math.clamp(scroll,0,Math.max(0,s(offset)-(height-top-s(196))));
    }
    private Administration groupAdministration(List<Row> group){
        if(group.stream().anyMatch(r->r.administration()==null))return null;
        return new Administration(group.stream().anyMatch(Row::hasResistance),group.getFirst().administration().returnTerritory(),group.stream().anyMatch(r->r.administration().enabled()),group.stream().map(r->r.administration().tooltip()).distinct().collect(java.util.stream.Collectors.joining("\n")),group.getFirst().administration().confirm());
    }
    private Details groupDetails(List<Row> group){
        if(group.stream().anyMatch(r->r.details()==null))return null;
        double factories=group.stream().mapToDouble(Row::factories).sum();
        Economy economy=null;
        if(group.stream().allMatch(r->r.details().economy()!=null)) {
            var all=group.stream().map(r->r.details().economy()).toList();
            var resourceAmounts=new TreeMap<String,Double>();
            all.forEach(e->e.resourceAmounts().forEach((resource,amount)->resourceAmounts.merge(resource,amount,Double::sum)));
            economy=new Economy(all.stream().anyMatch(e->e.population()==null)?null:all.stream().mapToDouble(Economy::population).sum(),all.stream().anyMatch(e->e.manpower()==null)?null:all.stream().mapToDouble(Economy::manpower).sum(),all.stream().mapToDouble(Economy::resources).sum(),all.stream().mapToInt(Economy::civilian).sum(),all.stream().mapToInt(Economy::military).sum(),resourceAmounts);
        }
        double resources=economy==null?0:economy.resources();
        return new Details(group.getFirst().details().totalStates(),factories==0?0:group.stream().mapToDouble(r->r.factories()*r.details().factoryShare()).sum()/factories,
                resources==0?0:group.stream().mapToDouble(r->r.details().economy().resources()*r.details().resourceShare()).sum()/resources,
                group.stream().anyMatch(r->r.details().divisions()==null)?null:group.stream().mapToDouble(r->r.details().divisions()).sum(),group.stream().mapToDouble(r->r.details().coverage()).average().orElseThrow(),economy);
    }
    private void openReturn(String country){
        if(pending>0)return;
        returnTarget=view.rows().stream().filter(r->r.country().equals(country)&&r.administration()!=null&&r.administration().returnTerritory()&&r.administration().enabled()).findFirst().orElse(null);
        if(returnTarget==null)return;
        returnRevision=view.revision();returnScroll=0;target=null;rebuildWidgets();
    }
    private void buildReturn(){
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        returnLines=font.split(UiKeywordIcons.decorateLines(UiTooltips.officerText(returnTarget.administration().confirm()),false),183);
        int x=(width-233)/2,y=(height-154)/2;
        addRenderableWidget(new UiMenuButton(UiCommandLabels.text("CANCEL","취소"),x+43,y+124,70,13,()->{returnTarget=null;rebuildWidgets();}).background("commander/commands/button_148x34/frame_0").sound("ui.close"));
        addRenderableWidget(new UiMenuButton("OK",x+128,y+124,70,13,this::confirmReturn).background("commander/commands/button_148x34/frame_0"));
    }
    private void confirmReturn(){
        if(returnTarget==null||pending>0||view.revision()!=returnRevision)return;
        var selected=returnTarget;returnTarget=null;send(Action.RETURN,selected.state(),selected.country());
    }
    private void drawReturn(GuiGraphicsExtractor g){
        int x=(width-233)/2,y=(height-154)/2;
        g.nextStratum();
        UiAssets.nineSlice(g,"mio/ui/gfx_tiled_window_pol_goal/frame_0",x,y,233,154,43,.5);
        UiText.centered(g,font,Component.literal(UiCommandLabels.text("RETURN_TERRITORY_TITLE","영토 반환")).withStyle(net.minecraft.ChatFormatting.BOLD),x+8,y+9,217,22,UiMenuStyle.TEXT,false);
        int available=71;returnScroll=Math.clamp(returnScroll,0,Math.max(0,returnLines.size()*10-available));
        g.enableScissor(x+12,y+40,x+221,y+111);
        for(int i=0;i<returnLines.size();i++)g.text(font,returnLines.get(i),x+(233-font.width(returnLines.get(i)))/2,y+40+i*10-returnScroll,UiMenuStyle.TEXT,false);
        g.disableScissor();
    }
    private void buildPopup(){
        for(var control:controls)control.button().active=false;
        int x=popupX(),y=popupY(),w=popupWidth(),h=popupHeight();
        addRenderableWidget(new UiMenuButton("×",x+w-s(42),y+s(7),s(34),s(34),()->{target=null;rebuildWidgets();}));
        var options=options();int available=h-s(52);optionScroll=Math.clamp(optionScroll,0,Math.max(0,s(options.size()*38+(target.equals("*")?0:38))-available));
        if(!target.equals("*")){
            String label="기본 ("+(templates?templateName(view.defaultTemplate()):law(view.defaultLaw()))+") 사용";
            add(label,selected().equals(templates?view.defaultTemplate():view.defaultLaw())?"occupation/select_entry_selected":"occupation/select_entry",null,"",x+s(15),y+s(44)-optionScroll,w-s(29),s(38),true,()->{send(Action.RESET,target,templates?"template":"law");target=null;});
        }
        for(int i=0;i<options.size();i++){
            var option=options.get(i);int by=optionY(i);if(by<y+s(44)||by+s(38)>y+h-s(8))continue;
            var b=add(option.name(),option.id().equals(selected())?"occupation/select_entry_selected":"occupation/select_entry",option.icon(),"",x+s(15),by,w-s(29),s(38),true,()->{send(templates?Action.TEMPLATE:Action.LAW,target,option.id());target=null;});
            b.active=option.enabled()&&pending==0;b.setTooltip(UiTooltips.createExact(templates?option.tooltip():lawTooltip(targetEntry(),option.id())));
        }
    }
    private UiMenuButton control(String name,String texture,String icon,String caption,int x,int y,int w,int h,Runnable action){return add(name,texture,icon,caption,s(x),top+s(y),s(w),s(h),false,action);}
    private UiMenuButton add(String name,String texture,String icon,String caption,int x,int y,int w,int h,boolean left,Runnable action){
        var button=new UiMenuButton(name,x,y,w,h,action).background("none").caption("");button.active=pending==0;addRenderableWidget(button);controls.add(new Control(button,texture,icon,caption,left));return button;
    }
    private void select(String state,boolean template){target=state;templates=template;optionScroll=0;rebuildWidgets();}
    private String law(String id){return view.laws().stream().filter(o->o.id().equals(id)).map(Option::name).findFirst().orElse("혼합");}
    private String lawIcon(String id){return view.laws().stream().filter(o->o.id().equals(id)).map(Option::icon).findFirst().orElse("");}
    private String templateName(String id){return view.templates().stream().filter(o->o.id().equals(id)).map(Option::name).findFirst().orElse("—");}
    private String templateIcon(String id){return view.templates().stream().filter(o->o.id().equals(id)).map(Option::icon).findFirst().orElse("production/units/infantry");}
    private String selected(){if(target.equals("*"))return templates?view.defaultTemplate():view.defaultLaw();return entries.stream().filter(e->e.row().state().equals(target)).map(e->templates?e.row().template():e.row().law()).findFirst().orElse("");}
    private List<Option> options(){return templates?view.templates():view.laws().stream().filter(Option::enabled).toList();}
    private int popupWidth(){return Math.min(s(540),Math.max(160,width-pane-8));}
    private int popupX(){return Math.min(pane+s(6),width-popupWidth());}
    private int popupY(){return top+s(42);}
    private int popupHeight(){return Math.min(s(488),height-popupY()-3);}
    private int optionY(int i){return popupY()+s(44+(i+(target.equals("*")?0:1))*38)-optionScroll;}
    private int rowY(Entry e){return top+s(190+e.offset())-scroll;}
    private boolean visible(Entry e,int y){return y>=top+s(190)&&y+s(e.country()?120:e.row().hasResistance()?60:30)<=height-3;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        UiMenuStyle.panel(g,0,top,pane,height-top);UiAssets.nineSlice(g,"construction/ui/header",0,top,pane,s(43),4,.5);
        UiText.left(g,font,Component.literal("점령지").withStyle(net.minecraft.ChatFormatting.BOLD),s(28),top+s(7),s(350),s(34),UiMenuStyle.TEXT);
        UiAssets.draw(g,"occupation/source/gfx_sunk_ships_button/frame_0",s(410),top+s(7),s(28),s(29));
        UiAssets.draw(g,"occupation/source/gfx_equip_overview_button/frame_0",s(450),top+s(7),s(28),s(29));
        UiMenuBar.draw(g,width,dev.hoi.client.ClientCampaignHud.hud(),mx,my,0);
        if(view!=null){
            float headerPixel=1f/minecraft.getWindow().getGuiScale();
            g.pose().pushMatrix();g.pose().translate(0,top+s(40));g.pose().scale(1,(s(90)+headerPixel)/s(90));
            UiAssets.nineSlice(g,"occupation/header_frame",s(3),0,s(545),s(90),8,pane/550.0);g.pose().popMatrix();
            g.pose().pushMatrix();g.pose().translate(0,top+s(48));g.pose().scale(1,(s(74)+headerPixel)/s(74));
            UiAssets.nineSlice(g,"occupation/header",s(11),0,s(529),s(74),2,pane/550.0);g.pose().popMatrix();
            text(g,"영토 관리",s(13),headerLabelY(),s(230),UiMenuStyle.TEXT,false);
            text(g,"외국의 지원",s(290),headerLabelY(),s(150),UiMenuStyle.TEXT,false);
            UiAssets.nineSlice(g,"occupation/foreign_support",s(290),headerControlsY(),s(119),s(31),8,pane/550.0);
            int supportTextHeight=Math.round(font.lineHeight*textScale());
            int supportY=headerControlsY()+(s(31)-supportTextHeight)/2;
            UiAssets.drawContent(g,"hud/manpower",s(293),supportY,s(27),supportTextHeight);text(g,"0",s(324),supportY,s(75),UiMenuStyle.TEXT,true);
            art(g,showPeaceful?"occupation/checkbox_checked":"occupation/checkbox",17,159,30,30);
            text(g,"저항하지 않는 국가 보기 ("+view.rows().stream().filter(r->r.resistance()==0).map(Row::country).distinct().count()+")",s(53),top+s(169),s(450),UiMenuStyle.TEXT,false);
            for(var entry:entries){int y=rowY(entry);if(visible(entry,y))drawRow(g,entry,y);}
            if(entries.isEmpty())text(g,"관리 중인 비핵심 점령지가 없습니다.",s(20),top+s(207),pane-s(35),UiMenuStyle.MUTED,false);
            if(target!=null){int x=popupX(),y=popupY(),w=popupWidth();UiMenuStyle.panel(g,x,y,w,popupHeight());UiAssets.draw(g,"occupation/select_header",x+s(7),y+s(5),w-s(14),s(40));String title=target.equals("*")?(templates?label("OCCUPATION_PRIMARY_GARRISON_TITLE"):"기본 법률"):entries.stream().filter(e->e.row().state().equals(target)).map(e->e.row().name()).findFirst().orElse("");text(g,title,x+s(25),y+s(13),w-s(75),UiMenuStyle.TEXT,false);}
            for(var control:controls){drawControl(g,control,mx,my);if(target!=null&&!templates&&control.button().isMouseOver(mx,my)){var option=options().stream().filter(o->o.name().equals(control.button().getMessage().getString())).findFirst().orElse(null);if(option!=null)control.button().setTooltip(UiTooltips.createExact(UiPriorityControls.controlled()?option.comparisons().getOrDefault(selected(),option.tooltip()):lawTooltip(targetEntry(),option.id())));}}
            if(target!=null)drawOptions(g);
        }
        if(returnTarget!=null)drawReturn(g);
        super.extractRenderState(g,mx,my,delta);
        if(view!=null&&target==null&&returnTarget==null){
            if(mx>=s(288)&&mx<s(408)&&my>=headerControlsY()&&my<headerControlsY()+s(31))UiTooltips.drawExact(g,font,label("FOREIGN_SUPPORT_TOOLTIP"),mx,my);
            for(var entry:entries){int y=rowY(entry);if(!visible(entry,y))continue;if(entry.country()&&entry.row().details()!=null&&mx>=s(69)&&mx<s(158)&&my>=y+s(21)&&my<y+s(42))UiTooltips.drawExact(g,font,format("OCCUPIED_COUNTRY_CORE_COUNT_TOOLTIP",Map.of("NUM_CONTROLLED_CORES",Integer.toString(entry.count()),"NUM_CORES",Integer.toString(entry.row().details().totalStates()))),mx,my);if(!entry.row().hasResistance())continue;drawDetailsTooltip(g,entry,y,mx,my);if(drawResistanceEffectTooltip(g,entry,y,mx,my))continue;int bx=s(entry.country()?292:280),by=y+s(entry.country()?8:35);if(mx>=bx&&mx<s(516)&&my>=by&&my<by+s(entry.country()?70:25)){boolean comp=entry.country()?my>=by+s(35):mx>=s(397);UiTooltips.drawExact(g,font,comp?entry.row().complianceTooltip():entry.row().resistanceTooltip(),mx,my);}}
        }
    }
    private void drawRow(GuiGraphicsExtractor g,Entry entry,int y){
        var row=entry.row();
        UiAssets.draw(g,entry.country()?(row.hasResistance()?"occupation/entry_top":"occupation/source/gfx_occupation_country_entry_bg_top_locked/frame_0"):row.hasResistance()?"occupation/state_entry":"occupation/state_entry_no_resistance",s(7),y+(entry.country()?1:0),s(519),s(entry.country()?121:row.hasResistance()?63:30));
        if(entry.country()){
            int textHeight=Math.round(font.lineHeight*textScale());
            int flagY=(y+s(4)-1+y+s(23)-1+textHeight-s(31))/2;
            g.pose().pushMatrix();g.pose().translate(0,1f/minecraft.getWindow().getGuiScale());
            UiMenuBar.drawFlag(g,row.country(),s(20),flagY,s(45),s(31));g.pose().popMatrix();
            UiText.left(g,font,Component.literal(row.countryName()).withStyle(net.minecraft.ChatFormatting.BOLD),s(72),y+s(8),s(205),s(18),UiMenuStyle.TEXT);
            int stateIconHeight=Math.round(font.lineHeight*textScale());
            UiAssets.drawContent(g,"occupation/garrison",s(69),y+s(23)-1,stateIconHeight,stateIconHeight);text(g,entry.count()+"/"+(row.details()==null?"—":row.details().totalStates()),s(92),y+s(23)-1,s(65),UiMenuStyle.TEXT,false);
            int resourceY=y+s(46)+1;
            int economyTextY=resourceY+(s(30)-textHeight)/2;
            UiAssets.drawContent(g,"hud/manpower",s(36),economyTextY-1,s(29),textHeight+2);text(g,row.details()==null||row.details().economy()==null||row.details().economy().manpower()==null?"—":number(row.details().economy().manpower()),s(66),economyTextY,s(61),UiMenuStyle.TEXT,false);
            UiAssets.drawContent(g,"hud/factories",s(106),economyTextY-1,s(31),textHeight+2);text(g,row.factories()==0?"---":row.details()==null?"—":percent(row.details().factoryShare()),s(140),economyTextY,s(56),UiMenuStyle.TEXT,false);
            UiAssets.draw(g,"occupation/resources",s(199),resourceY,s(32),s(30));text(g,row.details()==null?"—":row.details().economy()!=null&&row.details().economy().resources()==0?"---":percent(row.details().resourceShare()),s(234),economyTextY,s(52),UiMenuStyle.TEXT,false);
            if(row.hasResistance()){drawRebellion(g,row,y);
            drawBar(g,row.resistance(),false,s(299),y+s(12),s(200),false);drawBar(g,row.compliance(),true,s(299),y+s(43),s(200),false);}
        }else{
            UiAssets.draw(g,"occupation/garrison",s(16),y+s(3),s(16),s(21));text(g,row.name(),s(35),y+s(8),s(227),UiMenuStyle.TEXT,false);
            if(row.hasResistance()){drawBar(g,row.resistance(),false,s(282),y+s(34),s(110),true);drawBar(g,row.compliance(),true,s(397),y+s(34),s(65),true);
            drawResistanceEffects(g,row,y);}
        }
        if(row.hasResistance())text(g,row.details()==null||row.details().divisions()==null?"—":String.format(Locale.ROOT,"%.2f",row.details().divisions()),s(102),y+s(entry.country()?91:35),s(65),UiMenuStyle.TEXT,false);
    }
    private void drawRebellion(GuiGraphicsExtractor g,Row row,int y){
        row.resistanceEffects().stream().filter(e->e.id().equals("resistance_90")).findFirst().ifPresent(effect->{
            drawResistanceEffect(g,effect,"disabled",s(482),y+s(3));
        });
    }
    private List<Effect> stateResistanceEffects(Row row){return row.resistanceEffects().stream().filter(e->e.threshold()<.9).toList();}
    private void drawResistanceEffects(GuiGraphicsExtractor g,Row row,int y){
        var effects=stateResistanceEffects(row);
        for(int i=0;i<effects.size();i++){
            var effect=effects.get(i);int x=s(300+i*27),ey=y+s(13);
            drawResistanceEffect(g,effect,effect.active()?"active":"inactive",x,ey);
        }
    }
    private void drawResistanceEffect(GuiGraphicsExtractor g,Effect effect,String background,int x,int y){
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(pane/550f);
        UiAssets.draw(g,"occupation/modifier_"+background,0,0,23,23);
        UiAssets.draw(g,effect.icon()+"_small",2,2,19,19);
        g.pose().popMatrix();
    }
    private boolean drawResistanceEffectTooltip(GuiGraphicsExtractor g,Entry entry,int y,int mx,int my){
        if(entry.country()){
            if(mx<s(482)||mx>=s(505)||my<y+s(3)||my>=y+s(26))return false;
            var effect=entry.row().resistanceEffects().stream().filter(e->e.id().equals("resistance_90")).findFirst();
            effect.ifPresent(e->UiTooltips.drawExact(g,font,e.tooltip(),mx,my));
            return effect.isPresent();
        }
        if(my<y+s(13)||my>=y+s(36))return false;
        var effects=stateResistanceEffects(entry.row());
        for(int i=0;i<effects.size();i++)if(mx>=s(300+i*27)&&mx<s(323+i*27)){
            UiTooltips.drawExact(g,font,effects.get(i).tooltip(),mx,my);return true;
        }
        return false;
    }
    private String currentLawTooltip(Entry entry,String id){
        return lawTooltip(entry,id).replace("\n\n"+label("OCCUPATION_POLICY_HOLD_CONTROL_TO_SHOW_DELTAS"),"");
    }
    private Entry targetEntry(){return target==null||target.equals("*")?null:entries.stream().filter(e->e.row().state().equals(target)).findFirst().orElse(null);}
    private String label(String key){return view.labels().getOrDefault(key,"");}
    private String format(String key,Map<String,String> values){return java.util.regex.Pattern.compile("\\$([A-Z_]+)(?:\\|([^$]*))?\\$").matcher(label(key)).replaceAll(m->java.util.regex.Matcher.quoteReplacement(m.group(2)!=null&&m.group(2).contains("H")?"§e"+values.getOrDefault(m.group(1),"—")+"§r":values.getOrDefault(m.group(1),"—")));}
    private static String decimal(double n,int places){return String.format(Locale.ROOT,"%."+places+"f",n);}
    private String templateTooltip(Entry entry){return format("OCCUPATION_CHANGE_GARRISON_"+(entry==null?"DEFAULT":entry.country()?"COUNTRY":"STATE")+"_TEMPLATE_TOOLTIP",Map.of("SELECTED_TEMPLATE",templateName(entry==null?view.defaultTemplate():entry.row().template()),"OCCUPIED_COUNTRY",entry==null?"":entry.row().countryName(),"OCCUPIED_STATE",entry==null?"":entry.row().name()));}
    private String lawTooltip(Entry entry,String id){
        var text=new StringBuilder(view.laws().stream().filter(o->o.id().equals(id)).map(Option::tooltip).findFirst().orElse(""));
        var rows=view.rows().stream().filter(r->entry==null||entry.country()&&r.country().equals(entry.row().country())||r.state().equals(entry.row().state())).toList();
        if(rows.stream().anyMatch(r->r.details()!=null&&r.details().coverage()<1))text.append("\n\n").append(label("LAW_BONUS_SCALED"));
        var overrides=rows.stream().filter(r->r.details()!=null&&r.details().lawOverride()&&!r.law().equals(id)).toList();
        if(entry==null||entry.country())if(!overrides.isEmpty()){text.append("\n\n").append(label("LAW_IS_OVERRIDDEN_TOOLTIP"));for(var row:overrides)text.append("\n§e").append(row.name()).append("§r: ").append(law(row.law()));}
        return text.toString();
    }
    private void drawDetailsTooltip(GuiGraphicsExtractor g,Entry entry,int y,int mx,int my){
        var row=entry.row();var details=row.details();String tooltip=null;
        if(details==null)return;
        if(mx>=s(96)&&mx<s(173)&&my>=y+s(entry.country()?84:28)&&my<y+s(entry.country()?116:59))tooltip=format(row.template().isEmpty()?"OCCUPATION_GARRISON_STRENGTH_VARIOUS_TOOLTIP":"OCCUPATION_GARRISON_STRENGTH_TOOLTIP",Map.of("SELECTED_TEMPLATE",templateName(row.template()),"NUMBER",details.divisions()==null?"—":decimal(details.divisions(),2),"PERCENTAGE",percent(details.coverage())));
        var economy=details.economy();
        if(entry.country()&&economy!=null&&my>=y+s(46)&&my<y+s(78)){
            if(mx>=s(32)&&mx<s(107))tooltip=economy.population()!=null&&economy.population()==0?label("OCCUPIED_COUNTRY_NO_MANPOWER_TOOLTIP"):format("OCCUPIED_COUNTRY_MANPOWER_TOOLTIP",Map.of("NUM_TOTAL_MANPOWER",economy.population()==null?"—":number(economy.population()),"NUM_ACCESSED_MANPOWER",economy.manpower()==null?"—":number(economy.manpower()),"RATIO",economy.manpower()==null||economy.population()==null||economy.population()==0?"—":decimal(economy.manpower()/economy.population()*100,1)+"%"));
            else if(mx>=s(107)&&mx<s(198))tooltip=row.factories()==0?label("OCCUPIED_COUNTRY_NO_INDUSTRIAL_CAPACITY_TOOLTIP"):format("OCCUPIED_COUNTRY_INDUSTRIAL_CAPACITY_TOOLTIP",Map.of("NUM_TOTAL_FACTORIES",Integer.toString(row.factories()),"NUM_TOTAL_CIV_FACTORIES",Integer.toString(economy.civilian()),"NUM_TOTAL_MIL_FACTORIES",Integer.toString(economy.military()),"RATIO",decimal(details.factoryShare()*100,1)+"%","NUM_ACCESSED_FACTORIES",decimal(row.factories()*details.factoryShare(),1)));
            else if(mx>=s(198)&&mx<s(285))tooltip=economy.resources()==0?label("OCCUPIED_COUNTRY_NO_RESOURCES_TOOLTIP"):format("OCCUPIED_COUNTRY_RESOURCES_TOOLTIP",Map.of("NUM_TOTAL_RESOURCES",decimal(economy.resources(),0),"RESOURCES",economy.resourceAmounts().isEmpty()?"—":new TreeMap<>(economy.resourceAmounts()).entrySet().stream().map(e->"§e"+decimal(e.getValue(),0)+"§r "+e.getKey()).collect(java.util.stream.Collectors.joining(", ")),"RATIO",decimal(details.resourceShare()*100,1)+"%","NUM_ACCESSED_RESOURCES",decimal(economy.resources()*details.resourceShare(),1)));
        }
        if(tooltip!=null&&!tooltip.isEmpty())UiTooltips.drawExact(g,font,tooltip,mx,my);
    }
    private void drawBar(GuiGraphicsExtractor g,double value,boolean comp,int x,int y,int w,boolean compact){
        String kind=comp?"compliance":"resistance";
        int sourceWidth=compact?(comp?65:110):200;
        float scale=w/(float)sourceWidth;
        g.pose().pushMatrix();g.pose().translate(x-(compact?0:1),y);g.pose().scale(scale);
        int bx=compact?2:26,by=compact?2:comp?6:17,bw=compact?sourceWidth-4:173,bh=compact?15:9;
        if(value>0){
            var texture=net.minecraft.resources.Identifier.fromNamespaceAndPath("hoi","textures/gui/occupation/"+kind+(compact?"_compact":"_bar")+".png");
            float portion=(float)Math.clamp(value,0,1),u0=compact?0:7f/173;
            g.blit(texture,bx,by,bx+Math.round(bw*portion),by+bh,u0,u0+(1-u0)*portion,0,1);
        }
        UiAssets.draw(g,"occupation/"+kind+(compact?"_compact_frame":"_frame"),0,0,sourceWidth,compact?19:31);
        g.pose().popMatrix();
        if(compact)text(g,percent(value),x,y+(19*scale-font.lineHeight*textScale())/2f,w,UiMenuStyle.TEXT,true);
        else text(g,percent(value),x-1+s(27),y+s(comp?21:2),w-s(27),UiMenuStyle.TEXT,false);
    }
    private void drawControl(GuiGraphicsExtractor g,Control c,int mx,int my){
        var b=c.button();int x=b.getX(),y=b.getY(),w=b.getWidth(),h=b.getHeight();
        boolean mapMode=c.texture()!=null&&(c.texture().startsWith("occupation/resistance_tab")||c.texture().startsWith("occupation/compliance_tab"));
        if(mapMode){g.pose().pushMatrix();g.pose().translate(0,1f/minecraft.getWindow().getGuiScale());}
        if(c.texture()!=null){if(c.texture().startsWith("occupation/select_entry"))UiAssets.nineSlice(g,c.texture(),x,y,w,h,5,pane/550.0);else UiAssets.draw(g,c.texture(),x,y,w,h);}
        if(mapMode)g.pose().popMatrix();
        if(c.icon()!=null&&!c.icon().isEmpty()){int size=Math.min(h-s(2),s(30));if(c.icon().startsWith("production/units/"))UiAssets.drawContent(g,c.icon(),x+s(7),y+(h-size)/2,s(47),size);else UiAssets.draw(g,c.icon(),x+s(4),y+(h-size)/2,size,size);if(c.texture()!=null&&c.texture().equals("occupation/template_button"))UiAssets.draw(g,"occupation/template_change",x+w-s(16),y+s(6),s(12),s(18));}
        if("occupation/release".equals(c.texture())){UiText.centered(g,font,Component.literal(c.caption()).withStyle(net.minecraft.ChatFormatting.BOLD),x,y,w,h,UiMenuStyle.TEXT,false);return;}
        if(!c.caption().isEmpty()){int inset=c.icon()==null?(c.left()?s(4):0):s(32);text(g,c.caption(),x+inset,y+(h-font.lineHeight*textScale())/2f,w-inset-(c.icon()==null?0:s(5)),b.active||"occupation/release".equals(c.texture())?UiMenuStyle.TEXT:UiMenuStyle.MUTED,!c.left());}
    }
    private void drawOptions(GuiGraphicsExtractor g){
        var options=options();int x=popupX(),w=popupWidth();
        int[] starts=new int[3],widths=new int[3];int right=x+w-s(30);
        for(int c=2;c>=0;c--){int column=c; widths[c]=options.stream().filter(o->o.columns().size()>column).mapToInt(o->Math.round(font.width(o.columns().get(column).replaceAll("§.",""))*textScale())).max().orElse(0);starts[c]=right-widths[c]-s(27);right=starts[c]-s(8);}
        for(int i=target.equals("*")?0:-1;i<options.size();i++){int y=optionY(i);if(y<popupY()+s(44)||y+s(38)>popupY()+popupHeight()-s(8))continue;var option=i<0?options.stream().filter(o->o.id().equals(templates?view.defaultTemplate():view.defaultLaw())).findFirst().orElse(null):options.get(i);if(option==null)continue;int color=option.id().equals(selected())?0xFFD9BE38:UiMenuStyle.TEXT;int nameX=x+s(templates?78:57),nameWidth=starts[0]-nameX-s(8);String caption=UiText.ellipsize(font,option.name(),Math.max(1,(int)(nameWidth/textScale())-(i<0?font.width("기본 () 사용"):0)),"...");if(i<0)caption="기본 ("+caption+") 사용";text(g,caption,nameX,y+(s(38)-Math.round(font.lineHeight*textScale()))/2,nameWidth,color,false);
            String[] icons=templates?new String[]{"occupation/manpower_cost","occupation/production_cost","stats/hardness"}:new String[]{"map_detail/resistance","map_detail/compliance","hud/manpower"};
            for(int c=0;c<option.columns().size();c++){int cx=starts[c];UiAssets.draw(g,icons[c],cx,y+(s(38)-s(21))/2,s(22),s(21));text(g,option.columns().get(c),cx+s(27),y+(s(38)-Math.round(font.lineHeight*textScale()))/2,widths[c]+1,UiMenuStyle.TEXT,false);}
        }
    }
    private void art(GuiGraphicsExtractor g,String texture,int x,int y,int w,int h){UiAssets.draw(g,texture,s(x),top+s(y),s(w),s(h));}
    private void text(GuiGraphicsExtractor g,String value,int x,float y,int w,int color,boolean center){float scale=textScale();String clipped=UiText.ellipsize(font,value,Math.max(1,(int)(w/scale)));g.pose().pushMatrix();g.pose().translate(center?x+(w-font.width(clipped)*scale)/2:x,y);g.pose().scale(scale);g.text(font,clipped,0,0,color);g.pose().popMatrix();}
    private static String percent(double value){return String.format(Locale.ROOT,"%.0f%%",value*100);}
    private static String number(double value){return value>=1000?String.format(Locale.ROOT,"%.1fK",value/1000):String.format(Locale.ROOT,"%.0f",value);}
    private void send(Action action,String state,String value){if(pending>0&&action!=Action.CLOSE)return;pending=100;transport.accept(new Request(action,token,view==null?0:view.revision(),state,value));rebuildWidgets();}
    @Override public void tick(){if(pending>0)--pending;else if(++refresh%60==0&&view!=null&&returnTarget==null)send(Action.REFRESH,"","");}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(returnTarget!=null){returnScroll-=(int)(v*22);return true;}if(target!=null){optionScroll-=(int)(v*s(38));rebuildWidgets();return true;}if(x<pane){scroll-=(int)(v*s(40));rebuildWidgets();return true;}return super.mouseScrolled(x,y,h,v);}
    @Override public boolean keyPressed(KeyEvent e){if(returnTarget!=null){if(e.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE){returnTarget=null;rebuildWidgets();}else if(e.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN||e.key()==com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER)confirmReturn();return true;}if(e.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE&&target!=null){target=null;rebuildWidgets();return true;}return InputSidebarMovement.consumes(minecraft,e)||super.keyPressed(e);}
    @Override public boolean keyReleased(KeyEvent e){return InputSidebarMovement.consumes(minecraft,e)||super.keyReleased(e);}
    @Override public void removed(){InputSidebarMovement.release(minecraft);transport.accept(new Request(Action.CLOSE,token,view==null?0:view.revision(),"",""));}
    @Override public boolean allowsMovement(){return true;}
    @Override public int unitHudLeft(){return target==null?pane:popupX()+popupWidth();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}
