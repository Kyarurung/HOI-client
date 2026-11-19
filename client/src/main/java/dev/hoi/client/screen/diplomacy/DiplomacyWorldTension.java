package dev.hoi.client.screen.diplomacy;

import dev.hoi.client.HoiClient;
import dev.hoi.client.ui.*;
import dev.hoi.client.ui.country.CountryFlags;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.diplomacy.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

public final class DiplomacyWorldTension extends Screen {
    static final int LIST_X = 37, LIST_Y = 134, ROW = 42, ROWS = 13;
    private static final int[][] HEADERS = {{24, 180, 244, 303, 362, 440, 504}, {592, 745, 815, 868, 930, 1005, 1071}};
    private static final int[] HEADER_Y = {155, 158, 159, 158, 159, 159, 159};
    private static final String[] HEADER_ICONS = {"tension/participants", "tension/surrender", "tension/wargoal", "tension/contribution", "tension/divisions", "hud/factories", "tension/casualties"};
    private static final String[][] HEADER_TIPS = {{"PARTICIPANTS", "참전국"}, {"WAR_OVERVIEW_CAPITULATION_PROGRESS", "항복 현황"}, {"WARGOAL", "전쟁 명분"},
            {"WARSCORE_TITLE", "§H전쟁 기여도§!\n적을 항복시키는 데 한 국가가 얼마나 기여했는지를 나타내는 지표입니다. 기여도가 높을수록 종전 시 열릴 평화 회담에서 더 많은 요구를 할 수 있습니다."},
            {"DIVISIONS", "사단"}, {"INDUSTRY", "산업"}, {"CASUALTY", "사상자"}};
    private static final String[][] SORT_LABELS = {{"DATE_NAME", "날짜", "SORT_BY_DATE", "날짜순으로 정렬"}, {"country", "국가", "SORT_BY_COUNTRY", "국가순으로 정렬"},
            {"WORLD_TENSION_NAME", "세계 긴장도", "SORT_BY_TENSION", "세계 긴장도순으로 정렬"}, {"WORLD_TENSION_WARS", "현재 전쟁", "SHOW_WARS", "현재 진행 중인 모든 전쟁 보기"}};
    private record Hit(int x, int y, int w, int h, String tip) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    private record Line(DiplomacyWorldTensionView.Participant participant, String group, String key, List<DiplomacyWorldTensionView.Participant> members) {}
    private DiplomacyWorldTensionView view;
    private DiplomacyWorldTensionView.Sort sort = DiplomacyWorldTensionView.Sort.DATE;
    private String token = UUID.randomUUID().toString(), warId;
    private boolean descending = true, pending = true, minors = true, capitulated = true, callable = true;
    private int offset, ticks = 39, x, y, panelWidth, panelHeight, leftScroll, rightScroll;
    private float scale;
    private final boolean fixture;
    private final List<Hit> hits = new ArrayList<>();
    private final Set<String> collapsed = new HashSet<>();
    public DiplomacyWorldTension() { super(Component.literal(label("THREAT_WINDOW_NAME", "세계 긴장도 이력"))); fixture = false; }
    public DiplomacyWorldTension(DiplomacyWorldTensionView view) { super(Component.literal(label("THREAT_WINDOW_NAME", "세계 긴장도 이력"))); this.view = view; fixture = true; pending = false; }
    private static String label(String key, String fallback) { return UiCommandLabels.text(key, fallback); }
    String token() { return token; }
    DiplomacyWorldTensionView view() { return view; }
    String warId() { return warId; }
    float scale() { return scale; }
    void select(DiplomacyWorldTensionView.Sort next) {
        descending = sort == next ? !descending : next != DiplomacyWorldTensionView.Sort.COUNTRY;
        sort = next; offset = 0; warId = null; requestChanged();
    }
    private void requestChanged() {
        token = UUID.randomUUID().toString(); pending = !fixture; ticks = 30;
        if (fixture && view != null && sort != DiplomacyWorldTensionView.Sort.WARS) {
            Comparator<DiplomacyWorldTensionView.Entry> order = switch (sort) {
                case COUNTRY -> Comparator.comparing(DiplomacyWorldTensionView.Entry::name);
                case TENSION -> Comparator.comparingDouble(DiplomacyWorldTensionView.Entry::current);
                default -> Comparator.comparingLong(DiplomacyWorldTensionView.Entry::day);
            };
            if (descending) order = order.reversed();
            view = new DiplomacyWorldTensionView(view.viewer(),view.hud(),view.day(),view.offset(),view.total(),view.entries().stream().sorted(order).toList(),view.wars());
        }
        rebuildWidgets();
    }
    public void update(DiplomacyWorldTensionPayloads.Response packet) {
        if (!token.equals(packet.token())) return;
        if (packet.json().isEmpty()) { view = null; warId = null; minecraft.gui.setScreen(null); return; }
        var next = packet.view();
        if (view != null && !view.viewer().equals(next.viewer())) { view = null; warId = null; minecraft.gui.setScreen(null); return; }
        view = next; offset = view.offset(); pending = false;
        if (warId != null && war() == null) warId = null;
        rebuildWidgets();
    }
    private DiplomacyWorldTensionView.War war() { return view == null || warId == null ? null : view.wars().stream().filter(w -> w.id().equals(warId)).findFirst().orElse(null); }
    void openWar(String id) { if (view != null && view.wars().stream().anyMatch(w -> w.id().equals(id))) { warId = id; leftScroll = 0; rightScroll = 0; rebuildWidgets(); } }
    @Override public void tick() {
        if (fixture) return;
        if (++ticks >= 40 && ClientPlayNetworking.canSend(DiplomacyWorldTensionPayloads.Request.TYPE)) {
            ticks = 0; token = UUID.randomUUID().toString();
            ClientPlayNetworking.send(new DiplomacyWorldTensionPayloads.Request(token,sort,descending,offset));
        }
    }
    @Override protected void init() {
        boolean detail = war() != null;
        scale = Math.min(Math.max(.5f, width / 2560f), Math.min((width-12f)/(detail?1167:527),(height-HudMenuBar.height(width)-12f)/(detail?780:534)));
        panelWidth = detail ? 1167 : 527; panelHeight = detail ? 780 : 534;
        x = (width - s(panelWidth))/2; y = Math.max(HudMenuBar.height(width)+5,(height-s(panelHeight))/2);
        HudMenuBar.buttons(width,view==null?"":view.viewer(),null,tab -> { if (tab==MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(tab); }).forEach(this::addRenderableWidget);
        if (!detail) {
            int[] positions = {40,149,258,385};
            for (var tab : DiplomacyWorldTensionView.Sort.values()) button(label(SORT_LABELS[tab.ordinal()][0],SORT_LABELS[tab.ordinal()][1]),positions[tab.ordinal()],87,100,29,"tension/sort"+(sort==tab?"_selected":""),"ui.click",() -> select(tab));
            int[] close = UiAssets.imageSize("commander/commands/button_123x34/frame_0");
            button(label("CLOSE","닫기"),204,475,close[0],close[1],"commander/commands/button_123x34/frame_0","ui.close",this::onClose);
            if (view != null && !pending && sort == DiplomacyWorldTensionView.Sort.WARS) {
                var wars = page(view.wars());
                for (int i=0;i<Math.min(7,wars.size());i++) {
                    var w = wars.get(i); button(w.name(),LIST_X,LIST_Y+i*44,449,41,"","ui.click",() -> openWar(w.id()));
                }
            }
        } else {
            int[] close = UiAssets.imageSize("commander/commands/closebutton/frame_0");
            button("×",1167-44,22,close[0],close[1],"commander/commands/closebutton/frame_0","ui.close",this::onClose);
            button(label("WAR_OVERVIEW_FILTER_MINORS","약소국"),374,113,140,29,filterArt(minors),"ui.click",() -> { minors=!minors; rebuildWidgets(); });
            button(label("WAR_OVERVIEW_FILTER_CAPITULATED","항복함"),520,113,140,29,filterArt(capitulated),"ui.click",() -> { capitulated=!capitulated; rebuildWidgets(); });
            button(label("WAR_OVERVIEW_FILTER_CALLABLE","호출 가능"),665,113,140,29,filterArt(callable),"ui.click",() -> { callable=!callable; rebuildWidgets(); });
            sideButtons(war().attackers(),25,0,leftScroll); sideButtons(war().defenders(),593,1,rightScroll);
        }
    }
    private static String filterArt(boolean active) { return "commander/commands/sort_button_140x29/frame_"+(active?1:0); }
    private List<DiplomacyWorldTensionView.Participant> filtered(List<DiplomacyWorldTensionView.Participant> side) {
        return side.stream().filter(p -> (minors || p.major()) && (capitulated || !p.capitulated()) && (callable || !p.callable())).toList();
    }
    private List<Line> lines(List<DiplomacyWorldTensionView.Participant> side,int index) {
        var rows=filtered(side);
        if(rows.stream().allMatch(p -> p.faction().isEmpty())) return rows.stream().map(p -> new Line(p,null,null,null)).toList();
        var groups=new LinkedHashMap<String,List<DiplomacyWorldTensionView.Participant>>();
        rows.stream().filter(p -> !p.faction().isEmpty()).forEach(p -> groups.computeIfAbsent(p.faction(),k -> new ArrayList<>()).add(p));
        rows.stream().filter(p -> p.faction().isEmpty()).forEach(p -> groups.computeIfAbsent("",k -> new ArrayList<>()).add(p));
        var result=new ArrayList<Line>();
        groups.forEach((faction,members) -> {
            String key=index+"|"+faction;
            result.add(new Line(null,faction.isEmpty()?label("DIPLOMACY_NO_FACTION","§T세력 없음§!"):faction,key,members));
            if(!collapsed.contains(key)) members.forEach(p -> result.add(new Line(p,null,null,null)));
        });
        return result;
    }
    private void sideButtons(List<DiplomacyWorldTensionView.Participant> side,int px,int index,int scroll) {
        var rows=lines(side,index); int start=Math.clamp(scroll,0,Math.max(0,rows.size()-ROWS));
        for(int i=start;i<Math.min(rows.size(),start+ROWS);i++) {
            var line=rows.get(i); int py=193+(i-start)*ROW;
            if(line.participant()==null) button("",px+9,py+11,19,19,"occupation/"+(collapsed.contains(line.key())?"expand":"collapse"),"ui.click",() -> { if(!collapsed.remove(line.key())) collapsed.add(line.key()); rebuildWidgets(); });
            else { var p=line.participant(); button(p.name(),px,py,531,41,"","ui.click",() -> HoiClient.openCountry(p.country())); }
        }
    }
    private void button(String text,int px,int py,int w,int h,String art,String sound,Runnable action) {
        addRenderableWidget(new Button(x+s(px),y+s(py),s(w),s(h),Component.literal(text),b -> action.run(),message -> message.get()) {
            @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { dev.hoi.client.audio.AudioUiSounds.play(sound); }
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                if (art.isEmpty()) return;
                UiAssets.draw(g,art.equals("tension/sort") ? "tension/sort_selected" : art.equals("tension/sort_selected") ? "tension/sort" : art,getX(),getY(),getWidth(),getHeight());
                if(!text.isEmpty()&&!text.equals("×")) { begin(g); text(g,text,px,py+(h-lineHeight())/2,w,0xFFE0E0DA,true); end(g); }
            }
        });
    }
    private int s(int value) { return Math.round(value*scale); }
    private float fontScale() { return 1f/scale; }
    private int lineHeight() { return Math.round(font.lineHeight*fontScale()); }
    private void begin(GuiGraphicsExtractor g) { g.pose().pushMatrix(); g.pose().translate(x,y); g.pose().scale(scale); }
    private void end(GuiGraphicsExtractor g) { g.pose().popMatrix(); }
    private void text(GuiGraphicsExtractor g,String value,int px,int py,int w,int color,boolean centered) {
        text(g,value,px,py,w,color,centered,false);
    }
    private void text(GuiGraphicsExtractor g,String value,int px,int py,int w,int color,boolean centered,boolean bold) {
        UiText.marquee(g,font,Tooltips.officerText(value,bold),px,py,w,fontScale(),color,true,centered?0:-1);
    }
    private void marquee(GuiGraphicsExtractor g,String value,int px,int py,int w,int color,boolean centered) {
        float fontScale=fontScale();
        g.pose().pushMatrix(); g.pose().translate(px,py); g.pose().scale(fontScale);
        UiText.scrollingCell(g,font,Tooltips.officerText(value,false),0,0,Math.max(1,(int)(w/fontScale)),font.lineHeight+1,color,centered);
        g.pose().popMatrix();
    }
    private void rightText(GuiGraphicsExtractor g,String value,int px,int py,int w,int color) {
        UiText.marquee(g,font,value,px,py,w,fontScale(),color,true,1);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        HudMenuBar.draw(g,width,view==null?CountryHud.UNKNOWN:view.hud(),mx,my,0);
        hits.clear();
        begin(g);
        if(war()==null) history(g); else detail(g,war());
        end(g);
        scrollbars(g);
        super.extractRenderState(g,mx,my,delta);
        String tip=tooltip((mx-x)/scale,(my-y)/scale);
        if(!tip.isEmpty()) Tooltips.drawExact(g,font,tip,mx,my);
    }
    String tooltip(double px,double py) {
        String header=war()==null?"":headerTooltip(px,py);
        if(!header.isEmpty()) return header;
        for(var hit:hits) if(hit.contains(px,py)) return hit.tip();
        return "";
    }
    String headerTooltip(double px,double py) {
        for(int side=0;side<2;side++) for(int i=0;i<HEADER_ICONS.length;i++) {
            int[] size=UiAssets.imageSize(HEADER_ICONS[i]); int hx=HEADERS[side][i], hy=HEADER_Y[i];
            if(px>=hx&&px<hx+size[0]&&py>=hy&&py<hy+size[1]) return label(HEADER_TIPS[i][0],HEADER_TIPS[i][1]);
        }
        return "";
    }
    private void history(GuiGraphicsExtractor g) {
        UiAssets.draw(g,"tension/background",0,0,527,534);
        text(g,label("THREAT_WINDOW_NAME","세계 긴장도 이력"),20,28,500,0xFFE0E0DA,true,true);
        int[] positions = {40,149,258,385};
        for(int i=0;i<4;i++) hits.add(new Hit(positions[i],87,100,29,label(SORT_LABELS[i][2],SORT_LABELS[i][3])));
        hits.add(new Hit(204,475,123,34,label("CLOSE","닫기")));
        if(view==null || pending) { text(g,"서버 정보 불러오는 중…",13,176,500,0xFFA8ABA9,true); return; }
        boolean wars=sort==DiplomacyWorldTensionView.Sort.WARS;
        var warPage=page(view.wars()); var entryPage=page(view.entries());
        int count=wars?warPage.size():entryPage.size();
        if(count<=0) text(g,label("NO_THREAT_SOURCES_WINDOW","이력 사항 없음"),13,176,500,0xFFE0E0DA,true);
        for(int i=0;i<Math.min(7,count);i++) {
            int px=LIST_X, py=LIST_Y+i*44;
            if(wars) {
                var w=warPage.get(i); UiAssets.nineSlice(g,"tension/war_entry",px,py,449,41,6,1);
                if(w.major()) { UiAssets.draw(g,"tension/war_major",px+5,py+5,32,32); hits.add(new Hit(px+5,py+5,32,32,label("WAR_HAS_MAJOR","하나 이상의 강대국이 참전한 전쟁"))); }
                else hits.add(new Hit(px,py,449,41,label("CLICK_FOR_WAR_OVERVIEW","클릭하여 $WAR_NAME|Y$의 전쟁 개요 열기").replace("$WAR_NAME|Y$","§Y"+w.name()+"§!")));
                if(w.major()) hits.add(new Hit(px,py,449,41,label("CLICK_FOR_WAR_OVERVIEW","클릭하여 $WAR_NAME|Y$의 전쟁 개요 열기").replace("$WAR_NAME|Y$","§Y"+w.name()+"§!")));
                marquee(g,w.name(),px+45,py+2,250,0xFFE0E0DA,false); score(g,"tension/score_small",px+45,py+20,260,12,w.balance());
                if(!w.attackers().isEmpty()) flag(g,leader(w.attackers()).country(),"map_detail/flag_frame",px+321,py+3);
                if(!w.defenders().isEmpty()) flag(g,leader(w.defenders()).country(),"map_detail/flag_frame",px+396,py+3);
            } else {
                var e=entryPage.get(i); UiAssets.draw(g,"tension/entry",px,py,449,41);
                boolean tracking=e.initial()!=null&&e.initial()!=e.current();
                int valueLeft=tracking?172:235-Math.round(font.width(percent(e.current()))*fontScale());
                flag(g,e.country(),"map_detail/flag_frame",px+2,py+2); marquee(g,e.name(),px+55,py+4,Math.min(150,valueLeft-4-55),0xFFE0E0DA,false);
                var date=LocalDate.of(2020,1,1).plusDays(e.day());
                text(g,date.getDayOfMonth()+" "+date.getMonthValue()+"월, "+date.getYear(),px+55,py+20,180,0xFFE0E0DA,false);
                if(tracking) {
                    UiAssets.draw(g,"tension/clock",px+172,py+7,22,28);
                    rightText(g,percent(e.initial()),px+185,py+4,50,e.initial()>0?0xFFCF4545:0xFF6DAD64);
                    rightText(g,percent(e.current()),px+185,py+22,50,0xFFC5C7BF);
                } else rightText(g,percent(e.current()),px+185,py+4,50,e.current()>0?0xFFCF4545:e.current()<0?0xFF6DAD64:0xFFC5C7BF);
                var lines=Tooltips.split(font,Component.literal(e.reason()),(int)(203/fontScale()));
                for(int j=0;j<Math.min(2,lines.size());j++) { g.pose().pushMatrix();g.pose().translate(px+240,py+4+j*lineHeight());g.pose().scale(fontScale());g.text(font,lines.get(j),0,0,0xFFE0E0DA);g.pose().popMatrix(); }
            }
        }
    }
    private <T> List<T> page(List<T> list) { return fixture ? list.subList(Math.min(offset,list.size()),list.size()) : list; }
    private static DiplomacyWorldTensionView.Participant leader(List<DiplomacyWorldTensionView.Participant> side) {
        return side.stream().filter(p -> !p.callable() && p.leader()).findFirst().orElse(side.getFirst());
    }
    private void detail(GuiGraphicsExtractor g,DiplomacyWorldTensionView.War war) {
        var id=Identifier.parse("hoi:textures/gui/tension/war_background.png");
        g.blit(id,0,0,1167,240,0,1,0,240f/506);
        g.blit(id,0,240,1167,514,0,1,239f/506,240f/506);
        g.blit(id,0,514,1167,780,0,1,240f/506,1);
        text(g,war.name(),406,23,350,0xFFE0E0DA,true,true);
        score(g,"tension/score",294,71,579,26,war.balance()); UiAssets.draw(g,"tension/score_frame",287,66,591,38);
        text(g,fill(label("FIELDER_MANPOWER","야전 인력: $MAN|Y*$"),"MAN",manpower(war.attackers())),20,68,250,0xFFE0E0DA,true);
        text(g,fill(label("FIELDER_MANPOWER","야전 인력: $MAN|Y*$"),"MAN",manpower(war.defenders())),900,68,250,0xFFE0E0DA,true);
        text(g,fill(label("MANPOWER_LOSSES","손실: $LOSS|Y*$"),"LOSS","-"),20,86,250,0xFFE0E0DA,true);
        text(g,fill(label("MANPOWER_LOSSES","손실: $LOSS|Y*$"),"LOSS","-"),900,86,250,0xFFE0E0DA,true);
        hits.add(new Hit(1167-44,22,32,33,label("CLOSE","닫기")));
        String[][] filters={{"WAR_OVERVIEW_FILTER_MINORS_DESC","약소국들을 표시합니다"},{"WAR_OVERVIEW_FILTER_CAPITULATED_DESC","항복한 나라들을 표시합니다"},{"WAR_OVERVIEW_FILTER_CALLABLE_DESC","전쟁에 참여하도록 호출할 수 있는 나라들을 표시합니다"}};
        int[] filterX={374,520,665};
        for(int i=0;i<3;i++) hits.add(new Hit(filterX[i],113,140,29,label(filters[i][0],filters[i][1])));
        drawSide(g,war.attackers(),25,0,leftScroll); drawSide(g,war.defenders(),593,1,rightScroll);
    }
    private static String fill(String template,String key,String value) {
        int start=template.indexOf("$"+key);
        if(start<0) return template;
        int end=template.indexOf('$',start+1);
        return end<0?template:template.substring(0,start)+"§Y"+value+"§!"+template.substring(end+1);
    }
    private void drawSide(GuiGraphicsExtractor g,List<DiplomacyWorldTensionView.Participant> side,int px,int index,int scroll) {
        for(int i=0;i<HEADER_ICONS.length;i++) { int[] size=UiAssets.imageSize(HEADER_ICONS[i]); UiAssets.draw(g,HEADER_ICONS[i],HEADERS[index][i],HEADER_Y[i],size[0],size[1]); }
        var rows=lines(side,index); int start=Math.clamp(scroll,0,Math.max(0,rows.size()-ROWS));
        for(int i=start;i<Math.min(rows.size(),start+ROWS);i++) {
            var line=rows.get(i); int py=193+(i-start)*ROW;
            if(line.participant()==null) { group(g,line,px,py); continue; }
            var p=line.participant();
            g.blit(Identifier.parse("hoi:textures/gui/"+(p.callable()?"tension/ally":"tension/participant")+".png"),px,py,px+531,py+41,0,1,0,1);
            flag(g,p.country(),"tension/flag_frame",px+4,py+3);
            if(p.major()) { int[] frame=UiAssets.imageSize("tension/flag_frame"), overlay=UiAssets.imageSize("tension/major_overlay"); UiAssets.draw(g,"tension/major_overlay",px+4+(frame[0]-overlay[0])/2,py+3+(frame[1]-overlay[1])/2,overlay[0],overlay[1]); }
            if(p.capitulated()) UiAssets.draw(g,"tension/surrender_flag",px+32,py+17,16,16);
            marquee(g,p.name(),px+56,py+(p.callable()?8:4),p.callable()?150:180,p.callable()?0xFF999C96:0xFFE0E0DA,false);
            UiAssets.draw(g,"tension/surrender_bg",px+55,py+26,142,6);
            if(p.surrender()!=null&&p.surrender()>0) g.blit(Identifier.parse("hoi:textures/gui/tension/surrender_fill.png"),px+55,py+26,px+55+(int)Math.round(142*Math.min(1,p.surrender())),py+32,0,(float)Math.min(1,p.surrender()),0,1);
            UiAssets.draw(g,"tension/surrender_frame",px+54,py+26,144,8);
            if(p.callable()) continue;
            if(p.wargoal()) { UiAssets.draw(g,"diplomacy/relations/wargoal",px+237,py+7,29,26); hits.add(new Hit(px+237,py+7,29,26,label("WARGOAL","전쟁 명분"))); }
            marquee(g,p.contribution()==null?"-":Math.round(p.contribution()*100)+"%",px+275,py+11,39,0xFFE0E0DA,true);
            String divisions=range(p.divisions()), factories=range(p.factories());
            marquee(g,divisions,px+321,py+11,71,0xFFE0E0DA,true);
            marquee(g,factories,px+399,py+11,67,0xFFE0E0DA,true);
            marquee(g,p.casualties()==null?"-":kilo(p.casualties()),px+475,py+11,42,0xFFE0E0DA,true);
            hits.add(new Hit(px+319,py+5,75,30,label("DIVISIONS_COUNTRY","§T$COUNTRY|H$ 측은 $VAL|H$개 사단 보유 중§!").replace("$COUNTRY|H$","§H"+p.name()+"§!").replace("$VAL|H$","§H"+divisions+"§!")));
            hits.add(new Hit(px+397,py+5,71,30,label("INDUSTRY_COUNTRY","§T$COUNTRY|H$ 측은 총 $VAL|H$개의 공장 보유 중§!").replace("$COUNTRY|H$","§H"+p.name()+"§!").replace("$VAL|H$","§H"+factories+"§!")));
        }
    }
    private void group(GuiGraphicsExtractor g,Line line,int px,int py) {
        UiAssets.draw(g,"tension/faction_entry",px-2,py+1,537,42);
        marquee(g,line.group(),px+56,py+12,163,0xFFE0E0DA,false);
        var members=line.members().stream().filter(p -> !p.callable()).toList();
        marquee(g,sum(members.stream().map(DiplomacyWorldTensionView.Participant::divisions).toList(),false),px+321,py+11,71,0xFFE0E0DA,true);
        marquee(g,sum(members.stream().map(DiplomacyWorldTensionView.Participant::factories).toList(),false),px+399,py+11,67,0xFFE0E0DA,true);
        text(g,"-",px+475,py+11,42,0xFFE0E0DA,true);
    }
    private static long[] bounds(String value) {
        if(value==null||value.isBlank()||value.equals("?")) return null;
        String[] parts=value.replace(" - ","–").split("–");
        try { long low=Long.parseLong(parts[0].trim()), high=parts.length>1?Long.parseLong(parts[1].trim()):low; return low<0||high<low?null:new long[]{low,high}; }
        catch(NumberFormatException error) { return null; }
    }
    private static String range(String value) {
        var bounds=bounds(value);
        return bounds==null?"?":bounds[0]==bounds[1]?Long.toString(bounds[0]):bounds[0]+" - "+bounds[1];
    }
    static String sum(List<String> values,boolean kilo) {
        long low=0,high=0;
        for(String value:values) {
            var bounds=bounds(value); if(bounds==null) return "?";
            try { low=Math.addExact(low,bounds[0]); high=Math.addExact(high,bounds[1]); } catch(ArithmeticException overflow) { return "?"; }
        }
        String lowText=kilo?kilo(low):Long.toString(low), highText=kilo?kilo(high):Long.toString(high);
        return low==high?lowText:lowText+" - "+highText;
    }
    private static String manpower(List<DiplomacyWorldTensionView.Participant> side) {
        return sum(side.stream().filter(p -> !p.callable()).map(DiplomacyWorldTensionView.Participant::manpower).toList(),true);
    }
    static String kilo(long value) {
        if(value<1000) return Long.toString(value);
        String[] units={"K","M","B"}; var number=BigDecimal.valueOf(value);
        for(int unit=0;unit<units.length;unit++) {
            var scaled=number.movePointLeft(3*(unit+1));
            int digits=scaled.compareTo(BigDecimal.valueOf(100))>=0?0:scaled.compareTo(BigDecimal.TEN)>=0?1:2;
            var rounded=scaled.setScale(digits,RoundingMode.HALF_UP);
            if(rounded.compareTo(BigDecimal.valueOf(1000))<0||unit==units.length-1) return rounded.toPlainString()+units[unit];
        }
        return Long.toString(value);
    }
    private static String percent(Double value) { return value==null?"-":String.format(Locale.ROOT,"%.1f%%",value); }
    private void flag(GuiGraphicsExtractor g,String tag,String frame,int fx,int fy) {
        if(!CountryFlags.framed(g,HudMenuBar.flagTexture(tag),frame,fx,fy,41,26)) { int[] size=UiAssets.imageSize(frame); text(g,tag,fx,fy+(size[1]-lineHeight())/2,size[0],0xFFE0E0DA,true); }
    }
    private void score(GuiGraphicsExtractor g,String art,int px,int py,int w,int h,Double amount) {
        UiAssets.draw(g,art+"_blue",px,py,w,h);
        if(amount==null) { text(g,"0",px,py,w,0xFFE0E0DA,true);return; }
        int filled=(int)Math.round(w*amount);
        if(filled>0) g.blit(Identifier.parse("hoi:textures/gui/"+art+"_red.png"),px,py,px+filled,py+h,0,amount.floatValue(),0,1);
    }
    private List<UiScrollBar> bars() {
        var war=war(); var result=new ArrayList<UiScrollBar>();
        if(view==null) return result;
        if(war==null) result.add(new UiScrollBar(x+s(503),y+s(133),y+s(133+328),7,view.total()));
        else {
            result.add(new UiScrollBar(x+s(25+541),y+s(193),y+s(193+ROWS*ROW),ROWS,lines(war.attackers(),0).size()));
            result.add(new UiScrollBar(x+s(593+541),y+s(193),y+s(193+ROWS*ROW),ROWS,lines(war.defenders(),1).size()));
        }
        return result;
    }
    private int barOffset(int index) { return war()==null?offset:index==0?leftScroll:rightScroll; }
    private void scrollbars(GuiGraphicsExtractor g) { var bars=bars(); for(int i=0;i<bars.size();i++) bars.get(i).draw(g,barOffset(i)); }
    private void scrollTo(int index,int next) {
        if(war()==null) { if(next!=offset) { offset=next; requestChanged(); } }
        else { if(index==0) leftScroll=next; else rightScroll=next; rebuildWidgets(); }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        var bars=bars();
        for(int i=0;i<bars.size();i++) {
            var bar=bars.get(i);
            if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&bar.contains(event.x(),event.y())) {
                scrollTo(i,bar.arrow(event.y())?bar.step(event.y(),barOffset(i),1):bar.offset(event.y()));
                dev.hoi.client.audio.AudioUiSounds.play("ui.click"); return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(view==null||mx<x||mx>x+s(panelWidth)||my<y||my>y+s(panelHeight)) return super.mouseScrolled(mx,my,horizontal,vertical);
        if(war()!=null) {
            if(mx<x+s(586)) leftScroll=Math.clamp(leftScroll-(int)vertical,0,Math.max(0,lines(war().attackers(),0).size()-ROWS));
            else rightScroll=Math.clamp(rightScroll-(int)vertical,0,Math.max(0,lines(war().defenders(),1).size()-ROWS));
            rebuildWidgets();return true;
        }
        scrollTo(0,Math.clamp(offset-(int)(vertical*3),0,Math.max(0,view.total()-7)));
        return true;
    }
    @Override public void onClose() { if(warId!=null) {warId=null;rebuildWidgets();} else super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
