package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class DecisionScreen extends Screen implements SidebarMovement.Screen {
    private final String token = UUID.randomUUID().toString();
    private final Consumer<DecisionProtocol.Request> sender;
    private final boolean fixture;
    private final Set<String> collapsed = new HashSet<>();
    private DecisionProtocol.View view;
    private int pane, top, scroll, total, page, ticks = 99, waiting;
    private record Hit(DecisionProtocol.Row row, int y, int height) {}
    private final List<Hit> hits = new ArrayList<>();
    public DecisionScreen() { this(null, ClientPlayNetworking::send, false); }
    public DecisionScreen(DecisionProtocol.View view, Consumer<DecisionProtocol.Request> sender) { this(view, sender, true); }
    private DecisionScreen(DecisionProtocol.View view, Consumer<DecisionProtocol.Request> sender, boolean fixture) {
        super(Component.literal("사건과 결정")); this.view=view;this.sender=sender;this.fixture=fixture;
    }
    public String token() { return token; }
    int scrollOffset() { return scroll; }
    boolean collapsed(String id) { return collapsed.contains(id); }
    public void update(DecisionProtocol.Response packet) {
        if (!packet.token().equals(token)) return;
        if (packet.json().isEmpty()) { minecraft.gui.setScreen(null); return; }
        var next=packet.view();
        if (view!=null && !view.country().equals(next.country())) { minecraft.gui.setScreen(null); return; }
        if (view!=null && view.session().equals(next.session()) && next.revision()<view.revision()) return;
        view=next;page=view.page();waiting=0;rebuildWidgets();
    }
    private void request(DecisionProtocol.Action action, String id) {
        if (!fixture && !ClientPlayNetworking.canSend(DecisionProtocol.Request.TYPE)) return;
        sender.accept(new DecisionProtocol.Request(action,token,view==null?"":view.session(),view==null?0:view.revision(),id,page));
        ticks=0;if(action==DecisionProtocol.Action.TAKE)waiting=100;
    }
    @Override public void tick() {
        if (waiting>0) waiting--;
        if (!fixture && ++ticks>=80 && waiting==0) request(view==null?DecisionProtocol.Action.OPEN:DecisionProtocol.Action.REFRESH,"");
    }
    @Override protected void init() {
        pane=HoiPanelLayout.width(MenuTab.DECISIONS,width);top=HoiMenuBar.height(width);
        HoiMenuBar.buttons(width,view==null?"":view.country(),MenuTab.DECISIONS,tab -> {
            if(tab==MenuTab.RESEARCH)HoiClient.open();else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new HoiMenuButton("×",pane-25,top+3,19,19,this::onClose));
        layout(null,false);
        scroll=Math.clamp(scroll,0,Math.max(0,total-contentBottom()+contentTop()));
        layout(null,true);
        if(view!=null && view.total()>DecisionProtocol.PAGE_SIZE) {
            var back=new HoiMenuButton("←",8,height-27,36,21,() -> changePage(-1));back.active=page>0;addRenderableWidget(back);
            var next=new HoiMenuButton("→",pane-44,height-27,36,21,() -> changePage(1));next.active=(page+1)*DecisionProtocol.PAGE_SIZE<view.total();addRenderableWidget(next);
        }
    }
    private void changePage(int delta) { page+=delta;scroll=0;request(DecisionProtocol.Action.REFRESH,""); }
    private int contentTop() { return top+52; }
    private int contentBottom() { return height-32; }
    private void layout(GuiGraphicsExtractor g, boolean widgets) {
        if(view==null)return;
        if(widgets)hits.clear();
        var groups=new LinkedHashMap<String,List<DecisionProtocol.Row>>();
        for(var row:view.rows())groups.computeIfAbsent(row.category(),k -> new ArrayList<>()).add(row);
        int y=contentTop()-scroll, header=Math.max(26,pane*52/550),rowHeight=Math.max(25,pane*40/550);
        for(var group:groups.entrySet()) {
            String id=group.getKey(),name=group.getValue().getFirst().categoryName();
            if(g!=null) {
                UiAssets.nineSlice(g,"decision/category",5,y,pane-10,header,5,1);
                if (!UiAssets.draw(g,icon(group.getValue().getFirst().categoryIcon()),10,y+3,header-6,header-6)) UiAssets.draw(g,"decision/category_generic",10,y+3,header-6,header-6);
                g.centeredText(font,font.plainSubstrByWidth(name,pane-header-39),pane/2,y+(header-font.lineHeight)/2,HoiMenuStyle.TEXT);
                g.text(font,collapsed.contains(id)?"+":"−",pane-22,y+(header-font.lineHeight)/2,HoiMenuStyle.TEXT);
            }
            if(widgets && y>=contentTop() && y+header<=contentBottom()) {
                control(name,6,y,pane-12,header,true,() -> {if(!collapsed.add(id))collapsed.remove(id);rebuildWidgets();});
            }
            y+=header+3;
            if(collapsed.contains(id))continue;
            for(var row:group.getValue()) {
                if(g!=null) {
                    UiAssets.nineSlice(g,row.enabled()?"decision/row":"decision/row_disabled",7,y,pane-14,rowHeight,4,1);
                    String icon=icon(row.icon());
                    if(icon.isEmpty() || !UiAssets.draw(g,icon,10,y+3,20,rowHeight-6))UiAssets.draw(g,"menu/decisions",10,y+3,20,rowHeight-6);
                    String cost=row.remainingHours()>0?(row.remainingHours()+23)/24+"일":format(row.cost());
                    int right=pane-30, costWidth=font.width(cost);
                    if(row.remainingHours()==0)UiAssets.draw(g,"hud/political_power",right-costWidth-16,y+6,12,12);
                    g.text(font,cost,right-costWidth,y+(rowHeight-font.lineHeight)/2,0xFFFFCC33);
                    g.text(font,font.plainSubstrByWidth(row.name(),Math.max(15,pane-costWidth-93)),35,y+(rowHeight-font.lineHeight)/2,row.enabled()?HoiMenuStyle.TEXT:HoiMenuStyle.MUTED);
                    UiAssets.draw(g,row.remainingHours()>0?"decision/timer":"decision/select",pane-26,y+4,15,rowHeight-8);
                }
                if(widgets && y>=contentTop() && y+rowHeight<=contentBottom()) {
                    hits.add(new Hit(row,y,rowHeight));
                    control(row.name(),7,y,pane-14,rowHeight,row.enabled() && waiting==0,() -> {request(DecisionProtocol.Action.TAKE,row.id());rebuildWidgets();});
                }
                y+=rowHeight+3;
            }
            y+=7;
        }
        total=y-contentTop()+scroll;
    }
    private void control(String name,int x,int y,int w,int h,boolean enabled,Runnable action) {
        var button=new Button(x,y,w,h,Component.literal(name),b -> action.run(),message -> message.get()) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {}
        };
        button.active=enabled;addRenderableWidget(button);
    }
    private static String icon(String name) { return name.matches("[A-Za-z0-9_]+")?"decision/icon/"+name.toLowerCase(Locale.ROOT):""; }
    private static String format(double n) { return java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        HoiMenuBar.draw(g,width,view==null?CountryHud.UNKNOWN:view.hud(),mx,my,0);
        HoiMenuStyle.panel(g,0,top,pane,height-top);
        g.text(font,"사건과 결정",10,top+9,HoiMenuStyle.TEXT);
        String message=view==null?"서버 정보 불러오는 중…":view.message().isEmpty()?"국가중점과 사건에 따른 결정":view.message();
        g.text(font,font.plainSubstrByWidth(message,pane-20),10,top+34,HoiMenuStyle.MUTED);
        g.enableScissor(3,contentTop(),pane-3,contentBottom());layout(g,false);g.disableScissor();
        if(view!=null && view.rows().isEmpty())g.text(font,"선택 가능한 결정 없음",12,contentTop()+12,HoiMenuStyle.MUTED);
        if(view!=null && view.total()>DecisionProtocol.PAGE_SIZE)g.centeredText(font,(page+1)+" / "+((view.total()+DecisionProtocol.PAGE_SIZE-1)/DecisionProtocol.PAGE_SIZE),pane/2,height-21,HoiMenuStyle.TEXT);
        super.extractRenderState(g,mx,my,delta);
        if(mx>=7 && mx<pane-7)for(var hit:hits)if(my>=hit.y() && my<hit.y()+hit.height()) {
            var r=hit.row();HoiTooltips.draw(g,font,r.name()+"\n"+r.description()+"\n정치력: "+format(r.cost())+"\n"+r.status(),mx,my);break;
        }
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(x<pane && y>=contentTop()) { scroll=Math.clamp(scroll-(int)(vertical*28),0,Math.max(0,total-contentBottom()+contentTop()));rebuildWidgets();return true; }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean allowsMovement() { return true; }
}
