package dev.hoi.client.screen.diplomacy;

import dev.hoi.client.HoiClient;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.diplomacy.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.time.LocalDate;
import java.util.*;

public final class DiplomacyWorldTension extends Screen {
    static final int LIST_X = 46, LIST_Y = 134, ROW = 42, ROWS = 13;
    private static final int[][] HEADERS = {{24, 180, 244, 303, 362, 440, 504}, {592, 745, 815, 868, 930, 1005, 1071}};
    private static final int[] HEADER_Y = {155, 158, 159, 158, 159, 159, 159};
    private static final String[] HEADER_ICONS = {"tension/participants", "tension/surrender", "tension/wargoal", "tension/contribution", "tension/divisions", "hud/factories", "tension/casualties"};
    private static final String[][] HEADER_TIPS = {{"PARTICIPANTS", "참전국"}, {"WAR_OVERVIEW_CAPITULATION_PROGRESS", "항복 현황"}, {"WARGOAL", "전쟁 명분"},
            {"WARSCORE_TITLE", "§H전쟁 기여도§!\n적을 항복시키는 데 한 국가가 얼마나 기여했는지를 나타내는 지표입니다. 기여도가 높을수록 종전 시 열릴 평화 회담에서 더 많은 요구를 할 수 있습니다."},
            {"DIVISIONS", "사단"}, {"INDUSTRY", "산업"}, {"CASUALTY", "사상자"}};
    private DiplomacyWorldTensionView view;
    private DiplomacyWorldTensionView.Sort sort = DiplomacyWorldTensionView.Sort.DATE;
    private String token = UUID.randomUUID().toString(), warId;
    private boolean descending = true, pending = true, minors = true, capitulated = true, callable = true;
    private int offset, ticks = 39, x, y, panelWidth, panelHeight, leftScroll, rightScroll;
    private float scale;
    private final boolean fixture;
    public DiplomacyWorldTension() { super(Component.literal("세계 긴장도 이력")); fixture = false; }
    public DiplomacyWorldTension(DiplomacyWorldTensionView view) { super(Component.literal("세계 긴장도 이력")); this.view = view; fixture = true; pending = false; }
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
            String[] labels = {"날짜","국가","세계 긴장도","현재 전쟁"}; int[] positions = {40,149,258,385};
            for (var tab : DiplomacyWorldTensionView.Sort.values()) button(labels[tab.ordinal()],positions[tab.ordinal()],87,100,29,"tension/sort"+(sort==tab?"_selected":""),() -> select(tab));
            button("닫기",204,475,123,34,null,this::onClose);
            if (view != null && !pending && sort == DiplomacyWorldTensionView.Sort.WARS) {
                for (int i=0;i<Math.min(7,view.wars().size());i++) {
                    var w = view.wars().get(i); button(w.name(),LIST_X,LIST_Y+i*44,449,41,"",() -> openWar(w.id()));
                }
            }
        } else {
            button("×",1124,18,30,30,null,this::onClose);
            button("약소국",374,113,140,29,"tension/sort"+(minors?"_selected":""),() -> { minors=!minors; rebuildWidgets(); });
            button("항복함",520,113,140,29,"tension/sort"+(capitulated?"_selected":""),() -> { capitulated=!capitulated; rebuildWidgets(); });
            button("호출 가능",665,113,140,29,"tension/sort"+(callable?"_selected":""),() -> { callable=!callable; rebuildWidgets(); });
            sideButtons(war().attackers(),25,leftScroll); sideButtons(war().defenders(),593,rightScroll);
        }
    }
    private List<DiplomacyWorldTensionView.Participant> filtered(List<DiplomacyWorldTensionView.Participant> side) {
        return side.stream().filter(p -> (minors || p.leader()) && (capitulated || !p.capitulated()) && (callable || !p.callable())).toList();
    }
    private void sideButtons(List<DiplomacyWorldTensionView.Participant> side,int px,int scroll) {
        var rows=filtered(side); int start=Math.clamp(scroll,0,Math.max(0,rows.size()-ROWS));
        for(int i=start;i<Math.min(rows.size(),start+ROWS);i++) {
            var p=rows.get(i); button(p.name(),px,193+(i-start)*ROW,531,41,"",() -> HoiClient.openCountry(p.country()));
        }
    }
    private void button(String label,int px,int py,int w,int h,String art,Runnable action) {
        addRenderableWidget(new Button(x+s(px),y+s(py),s(w),s(h),Component.literal(label),b -> action.run(),message -> message.get()) {
            @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { dev.hoi.client.audio.AudioUiSounds.play("ui.click"); }
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                if (art != null && art.isEmpty()) return;
                if (art == null) UiMenuStyle.control(g,getX(),getY(),getWidth(),getHeight(),false,false);
                else UiAssets.draw(g,art.equals("tension/sort") ? "tension/sort_selected" : art.equals("tension/sort_selected") ? "tension/sort" : art,getX(),getY(),getWidth(),getHeight());
                if(label.equals("×")) UiMenuStyle.close(g,getX(),getY(),getWidth(),getHeight(),UiMenuStyle.TEXT);
                else {
                    int lift = label.equals("닫기") ? Math.clamp(Math.round((Math.max(1.6f * font.lineHeight * scale, 10f) - 12 * scale) / 2), 1, 3) : 0;
                    begin(g); text(g,label,px,py+(h-12)/2-Math.round(lift/scale),w,0xFFE0E0DA,true); end(g);
                }
            }
        });
    }
    private int s(int value) { return Math.round(value*scale); }
    private void begin(GuiGraphicsExtractor g) { g.pose().pushMatrix(); g.pose().translate(x,y); g.pose().scale(scale); }
    private void end(GuiGraphicsExtractor g) { g.pose().popMatrix(); }
    private void text(GuiGraphicsExtractor g,String value,int px,int py,int w,int color,boolean centered) {
        text(g,value,px,py,w,color,centered,false);
    }
    private void text(GuiGraphicsExtractor g,String value,int px,int py,int w,int color,boolean centered,boolean bold) {
        float fontScale=Math.max(1.6f, 10f / (font.lineHeight * scale)); var line=net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(Tooltips.officerText(value,bold),Math.max(1,(int)(w/fontScale))));
        g.pose().pushMatrix(); g.pose().translate(px+(centered?w/2f:0),py); g.pose().scale(fontScale);
        if(centered) g.centeredText(font,line,0,0,color); else g.text(font,line,0,0,color);
        g.pose().popMatrix();
    }
    private void rightText(GuiGraphicsExtractor g,String value,int px,int py,int w,int color) {
        float fontScale=Math.max(1.6f, 10f / (font.lineHeight * scale)); var line=net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(Component.literal(value),Math.max(1,(int)(w/fontScale))));
        g.pose().pushMatrix(); g.pose().translate(px+w-font.width(line)*fontScale,py); g.pose().scale(fontScale);
        g.text(font,line,0,0,color); g.pose().popMatrix();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        HudMenuBar.draw(g,width,view==null?CountryHud.UNKNOWN:view.hud(),mx,my,0);
        begin(g);
        if(war()==null) history(g); else detail(g,war());
        end(g); super.extractRenderState(g,mx,my,delta);
        String tip=war()==null?"":headerTooltip((mx-x)/scale,(my-y)/scale);
        if(!tip.isEmpty()) Tooltips.drawExact(g,font,tip,mx,my);
    }
    String headerTooltip(double px,double py) {
        for(int side=0;side<2;side++) for(int i=0;i<HEADER_ICONS.length;i++) {
            int[] size=UiAssets.imageSize(HEADER_ICONS[i]); int hx=HEADERS[side][i], hy=HEADER_Y[i];
            if(px>=hx&&px<hx+size[0]&&py>=hy&&py<hy+size[1]) return UiCommandLabels.text(HEADER_TIPS[i][0],HEADER_TIPS[i][1]);
        }
        return "";
    }
    private void history(GuiGraphicsExtractor g) {
        UiAssets.draw(g,"tension/background",0,0,527,534);
        text(g,"세계 긴장도 이력",20,28,500,0xFFE0E0DA,true,true);
        if(view==null || pending) { text(g,"서버 정보 불러오는 중…",30,176,467,0xFFA8ABA9,true); return; }
        int count=sort==DiplomacyWorldTensionView.Sort.WARS?view.wars().size():view.entries().size();
        if(count==0) text(g,sort==DiplomacyWorldTensionView.Sort.WARS?"현재 진행 중인 전쟁이 없습니다.":"세계 긴장도 발생 기록이 없습니다.",30,176,467,0xFFA8ABA9,true);
        for(int i=0;i<Math.min(7,count);i++) {
            int px=LIST_X, py=LIST_Y+i*44;
            if(sort==DiplomacyWorldTensionView.Sort.WARS) {
                var w=view.wars().get(i); UiAssets.nineSlice(g,"tension/war_entry",px,py,449,41,6,1);
                text(g,w.name(),px+45,py+4,250,0xFFE0E0DA,false); score(g,"tension/score_small",px+45,py+20,260,12,w.balance());
                if(!w.attackers().isEmpty()) flag(g,w.attackers().getFirst().country(),px+326,py+7,"map_detail/flag_frame",px+321,py+3);
                if(!w.defenders().isEmpty()) flag(g,w.defenders().getFirst().country(),px+401,py+7,"map_detail/flag_frame",px+396,py+3);
            } else {
                var e=view.entries().get(i); UiAssets.draw(g,"tension/entry",px,py,449,41);
                flag(g,e.country(),px+7,py+6,"map_detail/flag_frame",px+2,py+2); text(g,e.name(),px+55,py+4,150,0xFFE0E0DA,false);
                var date=LocalDate.of(2020,1,1).plusDays(e.day());
                text(g,date.getDayOfMonth()+" "+date.getMonthValue()+"월, "+date.getYear(),px+55,py+20,180,0xFFE0E0DA,false);
                UiAssets.draw(g,"tension/clock",px+172,py+7,22,28);
                rightText(g,percent(e.initial()),px+185,py+4,50,e.initial()!=null&&e.initial()>0?0xFFCF4545:0xFF6DAD64);
                rightText(g,percent(e.current()),px+185,py+22,50,0xFFC5C7BF);
                var lines=Tooltips.split(font,Component.literal(e.reason()),145);
                for(int j=0;j<Math.min(2,lines.size());j++) { g.pose().pushMatrix();g.pose().translate(px+240,py+4+j*17);g.pose().scale(Math.max(1.25f, 10f / (font.lineHeight * scale)));g.text(font,lines.get(j),0,0,0xFFE0E0DA);g.pose().popMatrix(); }
            }
        }
        scrollbar(g,503,133,328,offset,view.total(),7);
    }
    private void detail(GuiGraphicsExtractor g,DiplomacyWorldTensionView.War war) {
        var id=Identifier.parse("hoi:textures/gui/tension/war_background.png");
        g.blit(id,0,0,1167,240,0,1,0,240f/506);
        g.blit(id,0,240,1167,514,0,1,239f/506,240f/506);
        g.blit(id,0,514,1167,780,0,1,240f/506,1);
        text(g,war.name(),340,24,487,0xFFE0E0DA,true,true);
        score(g,"tension/score",294,71,579,26,war.balance()); UiAssets.draw(g,"tension/score_frame",287,66,591,38);
        text(g,"야전 인력: "+sum(war.attackers()),20,68,250,0xFFE7C34C,true);
        text(g,"야전 인력: "+sum(war.defenders()),900,68,250,0xFFE7C34C,true);
        text(g,"손실: —",20,86,250,0xFFE7C34C,true); text(g,"손실: —",900,86,250,0xFFE7C34C,true);
        drawSide(g,war.attackers(),25,0,leftScroll); drawSide(g,war.defenders(),593,1,rightScroll);
    }
    private void drawSide(GuiGraphicsExtractor g,List<DiplomacyWorldTensionView.Participant> side,int px,int header,int scroll) {
        for(int i=0;i<HEADER_ICONS.length;i++) { int[] size=UiAssets.imageSize(HEADER_ICONS[i]); UiAssets.draw(g,HEADER_ICONS[i],HEADERS[header][i],HEADER_Y[i],size[0],size[1]); }
        var rows=filtered(side); int start=Math.clamp(scroll,0,Math.max(0,rows.size()-ROWS));
        for(int i=start;i<Math.min(rows.size(),start+ROWS);i++) {
            var p=rows.get(i); int py=193+(i-start)*ROW;
            g.blit(Identifier.parse("hoi:textures/gui/"+(p.callable()?"tension/ally":"tension/participant")+".png"),px,py,px+531,py+41,0,1,0,1);
            flag(g,p.country(),px+9,py+7,"tension/flag_frame",px+4,py+3);
            if(p.capitulated()) UiAssets.draw(g,"tension/surrender_flag",px+32,py+17,16,16);
            text(g,p.name(),px+56,py+(p.callable()?8:4),p.callable()?150:180,p.callable()?0xFF999C96:0xFFE0E0DA,false);
            UiAssets.draw(g,"tension/surrender_bg",px+55,py+26,142,6);
            if(p.surrender()!=null&&p.surrender()>0) g.blit(Identifier.parse("hoi:textures/gui/tension/surrender_fill.png"),px+55,py+26,px+55+(int)Math.round(142*Math.min(1,p.surrender())),py+32,0,(float)Math.min(1,p.surrender()),0,1);
            UiAssets.draw(g,"tension/surrender_frame",px+54,py+26,144,8);
            if(p.callable()) continue;
            text(g,p.contribution()==null?"—":Math.round(p.contribution()*100)+"%",px+269,py+11,50,0xFFE0E0DA,true);
            text(g,p.divisions(),px+317,py+11,80,0xFFE0E0DA,true);
            text(g,p.factories(),px+393,py+11,80,0xFFE0E0DA,true);
            text(g,p.casualties()==null?"—":p.casualties().toString(),px+466,py+11,60,0xFFE0E0DA,true);
        }
        scrollbar(g,px+541,193,ROWS*ROW,start,rows.size(),ROWS);
    }
    private String sum(List<DiplomacyWorldTensionView.Participant> side) {
        long total=0;
        for(var p:side) if(!p.callable()) { try {total=Math.addExact(total,Long.parseLong(p.manpower()));} catch(NumberFormatException|ArithmeticException e) {return "0";} }
        return HudMenuBar.number((double)total);
    }
    private static String percent(Double value) { return value==null?"—":String.format(Locale.ROOT,"%.1f%%",value); }
    private void flag(GuiGraphicsExtractor g,String tag,int px,int py,String frame,int fx,int fy) {
        if(!UiAssets.draw(g,HudMenuBar.flagTexture(tag),px,py,41,26)) text(g,tag,px,py+5,41,0xFFE0E0DA,true);
        int[] size=UiAssets.imageSize(frame); UiAssets.draw(g,frame,fx,fy,size[0],size[1]);
    }
    private void score(GuiGraphicsExtractor g,String art,int px,int py,int w,int h,Double amount) {
        UiAssets.draw(g,art+"_blue",px,py,w,h);
        if(amount==null) { text(g,"0",px,py,w,0xFFE0E0DA,true);return; }
        int filled=(int)Math.round(w*amount);
        if(filled>0) g.blit(Identifier.parse("hoi:textures/gui/"+art+"_red.png"),px,py,px+filled,py+h,0,amount.floatValue(),0,1);
    }
    private void scrollbar(GuiGraphicsExtractor g,int px,int py,int h,int start,int total,int visible) {
        if(total<=visible)return;
        g.fill(px,py,px+5,py+h,0xFF202325); int thumb=Math.max(12,h*visible/total), top=py+(h-thumb)*start/Math.max(1,total-visible);
        g.fill(px,top,px+5,top+thumb,0xFF7F8277);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(view==null||mx<x||mx>x+s(panelWidth)||my<y||my>y+s(panelHeight)) return super.mouseScrolled(mx,my,horizontal,vertical);
        if(war()!=null) {
            if(mx<x+s(586)) leftScroll=Math.clamp(leftScroll-(int)vertical,0,Math.max(0,filtered(war().attackers()).size()-ROWS));
            else rightScroll=Math.clamp(rightScroll-(int)vertical,0,Math.max(0,filtered(war().defenders()).size()-ROWS));
            rebuildWidgets();return true;
        }
        int next=Math.clamp(offset-(int)(vertical*3),0,Math.max(0,view.total()-7));
        if(next!=offset) {offset=next;requestChanged();} return true;
    }
    @Override public void onClose() { if(warId!=null) {warId=null;rebuildWidgets();} else super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
