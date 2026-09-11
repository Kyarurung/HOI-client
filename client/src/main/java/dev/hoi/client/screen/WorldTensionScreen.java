package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.time.LocalDate;
import java.util.*;

public final class WorldTensionScreen extends Screen {
    private WorldTensionView view;
    private WorldTensionView.Sort sort = WorldTensionView.Sort.DATE;
    private String token = UUID.randomUUID().toString(), warId;
    private boolean descending = true, pending = true, minors = true, capitulated = true, callable = true;
    private int offset, ticks = 39, x, y, panelWidth, panelHeight, leftScroll, rightScroll;
    private float scale;
    private final boolean fixture;
    public WorldTensionScreen() { super(Component.literal("세계 긴장도 이력")); fixture = false; }
    public WorldTensionScreen(WorldTensionView view) { super(Component.literal("세계 긴장도 이력")); this.view = view; fixture = true; pending = false; }
    String token() { return token; }
    WorldTensionView view() { return view; }
    WorldTensionView.Sort sort() { return sort; }
    String warId() { return warId; }
    void select(WorldTensionView.Sort next) {
        descending = sort == next ? !descending : next != WorldTensionView.Sort.COUNTRY;
        sort = next; offset = 0; warId = null; requestChanged();
    }
    private void requestChanged() {
        token = UUID.randomUUID().toString(); pending = !fixture; ticks = 30;
        if (fixture && view != null && sort != WorldTensionView.Sort.WARS) {
            Comparator<WorldTensionView.Entry> order = switch (sort) {
                case COUNTRY -> Comparator.comparing(WorldTensionView.Entry::name);
                case TENSION -> Comparator.comparingDouble(WorldTensionView.Entry::current);
                default -> Comparator.comparingLong(WorldTensionView.Entry::day);
            };
            if (descending) order = order.reversed();
            view = new WorldTensionView(view.viewer(),view.hud(),view.day(),view.offset(),view.total(),view.entries().stream().sorted(order).toList(),view.wars());
        }
        rebuildWidgets();
    }
    public void update(WorldTensionProtocol.Response packet) {
        if (!token.equals(packet.token())) return;
        if (packet.json().isEmpty()) { view = null; warId = null; minecraft.gui.setScreen(null); return; }
        var next = packet.view();
        if (view != null && !view.viewer().equals(next.viewer())) { view = null; warId = null; minecraft.gui.setScreen(null); return; }
        view = next; offset = view.offset(); pending = false;
        if (warId != null && war() == null) warId = null;
        rebuildWidgets();
    }
    private WorldTensionView.War war() { return view == null || warId == null ? null : view.wars().stream().filter(w -> w.id().equals(warId)).findFirst().orElse(null); }
    void openWar(String id) { if (view != null && view.wars().stream().anyMatch(w -> w.id().equals(id))) { warId = id; leftScroll = 0; rightScroll = 0; rebuildWidgets(); } }
    @Override public void tick() {
        if (fixture) return;
        if (++ticks >= 40 && ClientPlayNetworking.canSend(WorldTensionProtocol.Request.TYPE)) {
            ticks = 0; token = UUID.randomUUID().toString();
            ClientPlayNetworking.send(new WorldTensionProtocol.Request(token,sort,descending,offset));
        }
    }
    @Override protected void init() {
        boolean detail = war() != null;
        scale = Math.min(Math.max(.5f, width / 2560f), Math.min((width-12f)/(detail?1167:527),(height-HoiMenuBar.height(width)-12f)/(detail?780:534)));
        panelWidth = detail ? 1167 : 527; panelHeight = detail ? 780 : 534;
        x = (width - s(panelWidth))/2; y = Math.max(HoiMenuBar.height(width)+5,(height-s(panelHeight))/2);
        HoiMenuBar.buttons(width,view==null?"":view.viewer(),null,tab -> { if (tab==MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(tab); }).forEach(this::addRenderableWidget);
        if (!detail) {
            String[] labels = {"날짜","국가","세계 긴장도","현재 전쟁"}; int[] positions = {40,149,258,385};
            for (var tab : WorldTensionView.Sort.values()) button(labels[tab.ordinal()],positions[tab.ordinal()],87,100,29,"tension/sort"+(sort==tab?"_selected":""),() -> select(tab));
            button("닫기",204,484,123,28,null,this::onClose);
            if (view != null && !pending && sort == WorldTensionView.Sort.WARS) {
                for (int i=0;i<Math.min(7,view.wars().size());i++) {
                    var w = view.wars().get(i); button(w.name(),35,132+i*44,460,41,"",() -> openWar(w.id()));
                }
            }
        } else {
            button("×",1124,18,30,30,null,this::onClose);
            button("약소국",374,113,140,29,"tension/sort"+(minors?"_selected":""),() -> { minors=!minors; rebuildWidgets(); });
            button("항복함",520,113,140,29,"tension/sort"+(capitulated?"_selected":""),() -> { capitulated=!capitulated; rebuildWidgets(); });
            button("호출 가능",665,113,140,29,"tension/sort"+(callable?"_selected":""),() -> { callable=!callable; rebuildWidgets(); });
            sideButtons(war().attackers(),18,leftScroll); sideButtons(war().defenders(),589,rightScroll);
        }
    }
    private List<WorldTensionView.Participant> filtered(List<WorldTensionView.Participant> side) {
        return side.stream().filter(p -> (minors || p.leader()) && (capitulated || !p.capitulated()) && (callable || !p.callable())).toList();
    }
    private void sideButtons(List<WorldTensionView.Participant> side,int px,int scroll) {
        var rows=filtered(side); int start=Math.clamp(scroll,0,Math.max(0,rows.size()-12));
        for(int i=start;i<Math.min(rows.size(),start+12);i++) {
            var p=rows.get(i); button(p.name(),px,193+(i-start)*44,531,41,"",() -> HoiClient.openCountry(p.country()));
        }
    }
    private void button(String label,int px,int py,int w,int h,String art,Runnable action) {
        addRenderableWidget(new Button(x+s(px),y+s(py),s(w),s(h),Component.literal(label),b -> action.run(),message -> message.get()) {
            @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { dev.hoi.client.audio.UiSounds.play("ui.click"); }
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                if (art != null && art.isEmpty()) return;
                if (art == null) HoiMenuStyle.control(g,getX(),getY(),getWidth(),getHeight(),false,false);
                else UiAssets.draw(g,art.equals("tension/sort") ? "tension/sort_selected" : art.equals("tension/sort_selected") ? "tension/sort" : art,getX(),getY(),getWidth(),getHeight());
                if(label.equals("×")) HoiMenuStyle.close(g,getX(),getY(),getWidth(),getHeight(),HoiMenuStyle.TEXT);
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
        float fontScale=Math.max(1.6f, 10f / (font.lineHeight * scale)); String line=font.plainSubstrByWidth(value,Math.max(1,(int)(w/fontScale)));
        g.pose().pushMatrix(); g.pose().translate(px+(centered?w/2f:0),py); g.pose().scale(fontScale);
        if(centered) g.centeredText(font,line,0,0,color); else g.text(font,line,0,0,color);
        g.pose().popMatrix();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        HoiMenuBar.draw(g,width,view==null?CountryHud.UNKNOWN:view.hud(),mx,my,0);
        begin(g);
        if(war()==null) history(g); else detail(g,war());
        end(g); super.extractRenderState(g,mx,my,delta);
    }
    private void history(GuiGraphicsExtractor g) {
        UiAssets.draw(g,"tension/background",0,0,527,534);
        text(g,"세계 긴장도 이력",20,28,500,0xFFE0E0DA,true);
        if(view==null || pending) { text(g,"서버 정보 불러오는 중…",30,176,467,0xFFA8ABA9,true); return; }
        int count=sort==WorldTensionView.Sort.WARS?view.wars().size():view.entries().size();
        if(count==0) text(g,sort==WorldTensionView.Sort.WARS?"현재 진행 중인 전쟁이 없습니다.":"세계 긴장도 발생 기록이 없습니다.",30,176,467,0xFFA8ABA9,true);
        for(int i=0;i<Math.min(7,count);i++) {
            int py=132+i*44;
            if(sort==WorldTensionView.Sort.WARS) {
                var w=view.wars().get(i); UiAssets.draw(g,"tension/war_entry",35,py,460,41);
                text(g,w.name(),77,py+4,250,0xFFE0E0DA,false); score(g,77,py+22,260,12,w.balance());
                if(!w.attackers().isEmpty()) flag(g,w.attackers().getFirst().country(),359,py+7,36,24);
                if(!w.defenders().isEmpty()) flag(g,w.defenders().getFirst().country(),434,py+7,36,24);
            } else {
                var e=view.entries().get(i); UiAssets.draw(g,"tension/entry",35,py,460,41);
                flag(g,e.country(),42,py+6,36,24); text(g,e.name(),88,py+4,122,0xFFE0E0DA,false);
                var date=LocalDate.of(2020,1,1).plusDays(e.day());
                text(g,date.getDayOfMonth()+" "+date.getMonthValue()+"월, "+date.getYear(),88,py+22,122,0xFFE0E0DA,false);
                UiAssets.draw(g,"tension/clock",213,py+8,20,25);
                text(g,percent(e.initial()),244,py+4,49,e.initial()!=null&&e.initial()>0?0xFFCF4545:0xFF6DAD64,false);
                text(g,percent(e.current()),244,py+22,49,0xFFC5C7BF,false);
                var lines=font.split(Component.literal(e.reason()),145);
                for(int j=0;j<Math.min(2,lines.size());j++) { g.pose().pushMatrix();g.pose().translate(295,py+4+j*17);g.pose().scale(Math.max(1.25f, 10f / (font.lineHeight * scale)));g.text(font,lines.get(j),0,0,0xFFE0E0DA);g.pose().popMatrix(); }
            }
        }
        scrollbar(g,503,133,328,offset,view.total(),7);
    }
    private void detail(GuiGraphicsExtractor g,WorldTensionView.War war) {
        var id=Identifier.parse("hoi:textures/gui/tension/war_background.png");
        g.blit(id,0,0,1167,240,0,1,0,240f/506);
        g.blit(id,0,240,1167,514,0,1,239f/506,240f/506);
        g.blit(id,0,514,1167,780,0,1,240f/506,1);
        text(g,war.name(),340,24,487,0xFFE0E0DA,true);
        score(g,294,71,579,26,war.balance()); UiAssets.draw(g,"tension/score_frame",287,66,593,36);
        text(g,"야전 인력: "+sum(war.attackers()),20,72,264,0xFFE7C34C,true);
        text(g,"야전 인력: "+sum(war.defenders()),889,72,258,0xFFE7C34C,true);
        text(g,"손실: —",20,90,264,0xFFE7C34C,true); text(g,"손실: —",889,90,258,0xFFE7C34C,true);
        drawSide(g,war.attackers(),18,leftScroll); drawSide(g,war.defenders(),589,rightScroll);
    }
    private void drawSide(GuiGraphicsExtractor g,List<WorldTensionView.Participant> side,int px,int scroll) {
        String[] icons={"tension/surrender","tension/contribution","tension/divisions","hud/factories","tension/casualties"};
        int[] columns={233,283,346,422,485};
        for(int i=0;i<icons.length;i++) UiAssets.draw(g,icons[i],px+columns[i],157,22,22);
        var rows=filtered(side); int start=Math.clamp(scroll,0,Math.max(0,rows.size()-12));
        for(int i=start;i<Math.min(rows.size(),start+12);i++) {
            var p=rows.get(i); int py=193+(i-start)*44;
            g.blit(Identifier.parse("hoi:textures/gui/tension/participant.png"),px,py,px+531,py+41,0,1,0,1);
            flag(g,p.country(),px+8,py+5,30,20);
            text(g,p.name(),px+55,py+9,174,p.callable()?0xFF999C96:0xFFE0E0DA,false);
            g.fill(px+55,py+24,px+197,py+27,0xFF28443F);
            if(p.surrender()!=null) g.fill(px+55,py+24,px+55+(int)(142*p.surrender()),py+27,0xFFA13939);
            text(g,p.capitulated()?"항복":p.callable()?"대기":"",px+225,py+8,38,0xFFC6C6B7,true);
            text(g,p.contribution()==null?"—":Math.round(p.contribution()*100)+"%",px+269,py+12,50,0xFFE0E0DA,true);
            text(g,p.divisions(),px+317,py+12,80,0xFFE0E0DA,true);
            text(g,p.factories(),px+393,py+12,80,0xFFE0E0DA,true);
            text(g,p.casualties()==null?"—":p.casualties().toString(),px+466,py+12,60,0xFFE0E0DA,true);
        }
        scrollbar(g,px+553,193,548,start,rows.size(),12);
    }
    private String sum(List<WorldTensionView.Participant> side) {
        long total=0;
        for(var p:side) if(!p.callable()) { try {total=Math.addExact(total,Long.parseLong(p.manpower()));} catch(NumberFormatException|ArithmeticException e) {return "?";} }
        return HoiMenuBar.number((double)total);
    }
    private static String percent(Double value) { return value==null?"—":String.format(Locale.ROOT,"%.1f%%",value); }
    private void flag(GuiGraphicsExtractor g,String tag,int px,int py,int w,int h) {
        if(!UiAssets.draw(g,HoiMenuBar.flagTexture(tag),px,py,w,h)) text(g,tag,px,py+5,w,0xFFE0E0DA,true);
    }
    private void score(GuiGraphicsExtractor g,int px,int py,int w,int h,Double amount) {
        UiAssets.draw(g,"tension/score_blue",px,py,w,h);
        if(amount==null) { text(g,"?",px,py,w,0xFFE0E0DA,true);return; }
        int filled=(int)Math.round(w*amount);
        if(filled>0) g.blit(Identifier.parse("hoi:textures/gui/tension/score_red.png"),px,py,px+filled,py+h,0,amount.floatValue(),0,1);
    }
    private void scrollbar(GuiGraphicsExtractor g,int px,int py,int h,int start,int total,int visible) {
        if(total<=visible)return;
        g.fill(px,py,px+5,py+h,0xFF202325); int thumb=Math.max(12,h*visible/total), top=py+(h-thumb)*start/Math.max(1,total-visible);
        g.fill(px,top,px+5,top+thumb,0xFF7F8277);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(view==null||mx<x||mx>x+s(panelWidth)||my<y||my>y+s(panelHeight)) return super.mouseScrolled(mx,my,horizontal,vertical);
        if(war()!=null) {
            if(mx<x+s(583)) leftScroll=Math.clamp(leftScroll-(int)vertical,0,Math.max(0,filtered(war().attackers()).size()-12));
            else rightScroll=Math.clamp(rightScroll-(int)vertical,0,Math.max(0,filtered(war().defenders()).size()-12));
            rebuildWidgets();return true;
        }
        int next=Math.clamp(offset-(int)(vertical*3),0,Math.max(0,view.total()-7));
        if(next!=offset) {offset=next;requestChanged();} return true;
    }
    @Override public void onClose() { if(warId!=null) {warId=null;rebuildWidgets();} else super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
