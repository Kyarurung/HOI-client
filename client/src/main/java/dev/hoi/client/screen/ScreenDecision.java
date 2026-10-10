package dev.hoi.client.screen;

import com.google.gson.JsonParser;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class ScreenDecision extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static net.minecraft.client.resources.sounds.SoundInstance tabSound;
    private static final Pattern BODY_IMAGE = Pattern.compile("£([A-Za-z0-9_]+)£?");
    private String token = UUID.randomUUID().toString();
    private final Consumer<ProtocolDecisionPayloads.Request> sender;
    private final boolean fixture;
    private final Set<String> collapsed = new HashSet<>();
    private final ScreenDecisionSnapshot snapshot;
    private final ScreenDecisionText text = new ScreenDecisionText();
    private final ScreenDecisionPanel panels = new ScreenDecisionPanel();
    private Map<String,String> assets = Map.of();
    private int pane, top, scroll, total, ticks = 99, tooltipScroll;
    private long pendingRevision = -1;
    private boolean legacy;
    private Boolean modern;
    private int legacyPage;
    private String hover = "";
    private UiTooltips.ScrolledTooltip tooltipBounds;
    private record ScreenDecisionHit(String id,String tooltip,int x,int y,int width,int height) {
        ScreenDecisionHit(String id,String tooltip,int y,int height) { this(id,tooltip,7,y,Integer.MAX_VALUE,height); }
    }
    private final List<ScreenDecisionHit> hits = new ArrayList<>();
    private final Map<String,ProtocolDecisionDetails.View> details = new HashMap<>();
    private final Map<String,Integer> detailRequests = new HashMap<>();
    private int detailTicks;
    public ScreenDecision() { this(null,ClientPlayNetworking::send,false); }
    public ScreenDecision(ProtocolDecisionPayloads.View view,Consumer<ProtocolDecisionPayloads.Request> sender) { this(view,sender,true); }
    private ScreenDecision(ProtocolDecisionPayloads.View view,Consumer<ProtocolDecisionPayloads.Request> sender,boolean fixture) {
        super(Component.literal("사건과 결정"));snapshot=new ScreenDecisionSnapshot(view);this.sender=sender;this.fixture=fixture;if(view!=null)modern=true;
    }
    public String token() { return token; }
    public static void resourcesReloaded() {
        net.minecraft.client.Minecraft.getInstance().execute(()->{
            if(ScreenDialogCoordinator.contentScreen() instanceof ScreenDecision screen) {
                screen.panels.close();screen.text.assets(new com.google.gson.JsonObject());screen.rebuildWidgets();
            }
        });
    }
    int scrollOffset() { return scroll; }
    boolean collapsed(String id) { return collapsed.contains(id); }
    int rowCount() { return snapshot.rows().size(); }
    boolean pending() { return pendingRevision>=0; }
    public void update(ProtocolDecisionPayloads.Response packet) {
        if(!packet.token().equals(token))return;
        if(packet.json().isEmpty()) {ScreenDialogCoordinator.setContent(null);return;}
        ProtocolDecisionPayloads.View next;
        try {next=packet.view();} catch(IllegalArgumentException invalid) {return;}
        boolean nextModern=JsonParser.parseString(packet.json()).getAsJsonObject().has("pageCount");
        if(modern!=null && modern!=nextModern)return;
        modern=nextModern;legacy=!modern;
        var result=legacy?snapshot.acceptLegacy(next):snapshot.accept(next);
        if(result==ScreenDecisionSnapshot.Acceptance.REVOKED) {ScreenDialogCoordinator.setContent(null);return;}
        if(result==ScreenDecisionSnapshot.Acceptance.STAGED) {
            request(ProtocolDecisionPayloads.Action.REFRESH,"",snapshot.nextPage());
            rebuildWidgets();
        } else if(result==ScreenDecisionSnapshot.Acceptance.APPLIED) {
            details.clear();detailRequests.clear();
            if(legacy || next.revision()>pendingRevision)pendingRevision=-1;
            if(legacy)legacyPage=next.page();
            text.clear();ticks=0;rebuildWidgets();
        }
    }
    public void update(ProtocolDecisionDetails.Response packet) {
        if(!packet.token().equals(token) || snapshot.loading())return;
        ProtocolDecisionDetails.View next;
        try {next=packet.view();} catch(IllegalArgumentException invalid) {return;}
        var authority=snapshot.authority();
        if(authority==null || !authority.country().equals(next.country()) || !authority.session().equals(next.session())
                || authority.revision()!=next.revision() || !detailRequests.containsKey(next.id())
                || snapshot.rows().stream().noneMatch(row->row.id().equals(next.id())))return;
        details.put(next.id(),next);text.clear();
    }
    private void request(ProtocolDecisionPayloads.Action action,String id,int page) {
        if(!fixture && !ClientPlayNetworking.canSend(ProtocolDecisionPayloads.Request.TYPE))return;
        var authority=snapshot.authority();
        if(action==ProtocolDecisionPayloads.Action.TAKE) {
            if(pending() || snapshot.loading() || authority==null)return;
            pendingRevision=authority.revision();
        }
        if(legacy) {
            token=UUID.randomUUID().toString();
            if(action==ProtocolDecisionPayloads.Action.REFRESH)legacyPage=page;
        }
        sender.accept(new ProtocolDecisionPayloads.Request(action,token,authority==null?"":authority.session(),
                authority==null?0:authority.revision(),id,page));
        ticks=0;
    }
    @Override public void tick() {
        if(fixture)return;
        detailTicks++;
        var authority=snapshot.authority();
        if(!hover.isEmpty() && !snapshot.loading() && !pending() && authority!=null && !details.containsKey(hover)
                && detailTicks-detailRequests.getOrDefault(hover,-120)>=120
                && snapshot.rows().stream().anyMatch(row->row.id().equals(hover))
                && ClientPlayNetworking.canSend(ProtocolDecisionDetails.Request.TYPE)) {
            detailRequests.put(hover,detailTicks);
            ClientPlayNetworking.send(new ProtocolDecisionDetails.Request(token,authority.session(),authority.revision(),hover));
        }
        if(++ticks>=(snapshot.loading()?20:80))
            request(snapshot.authority()==null?ProtocolDecisionPayloads.Action.OPEN:ProtocolDecisionPayloads.Action.REFRESH,"",legacy?legacyPage:snapshot.nextPage());
    }
    @Override protected void init() {
        pane=UiPanelLayout.width(ProtocolMenuTab.DECISIONS,width);top=UiMenuBar.height(width);
        var view=snapshot.view();
        UiMenuBar.buttons(width,view==null?"":view.country(),ProtocolMenuTab.DECISIONS,tab -> {
            if(tab==ProtocolMenuTab.RESEARCH)HoiClient.open();else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new UiMenuButton("×",pane-25,top+3,19,19,this::onClose));
        assets=loadAssets();text.clear();panels.clear();
        layout(null,false);
        scroll=Math.clamp(scroll,0,Math.max(0,total-contentBottom()+contentTop()));
        layout(null,true);
        if(legacy && view!=null && view.total()>ProtocolDecisionPayloads.PAGE_SIZE) {
            var back=new UiMenuButton("←",8,height-27,36,21,() -> request(ProtocolDecisionPayloads.Action.REFRESH,"",legacyPage-1));
            back.active=legacyPage>0 && !pending();addRenderableWidget(back);
            var next=new UiMenuButton("→",pane-44,height-27,36,21,() -> request(ProtocolDecisionPayloads.Action.REFRESH,"",legacyPage+1));
            next.active=(legacyPage+1)*ProtocolDecisionPayloads.PAGE_SIZE<view.total() && !pending();addRenderableWidget(next);
        }
    }
    private Map<String,String> loadAssets() {
        var resource=minecraft.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:ui/decision_assets.json"));
        if(resource.isEmpty())return Map.of();
        try(var input=resource.get().open()) {
            byte[] bytes=input.readNBytes(262145);
            if(bytes.length>262144)return Map.of();
            var root=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("version").getAsInt()!=1)return Map.of();
            text.assets(root);
            panels.assets(root);
            var result=new HashMap<String,String>();
            for(var entry:root.getAsJsonObject("sprites").entrySet()) {
                String path=entry.getValue().getAsString();
                if(path.matches("[a-z0-9_/.-]+") && !path.contains(".."))result.put(entry.getKey(),path);
            }
            return Map.copyOf(result);
        } catch(Exception invalid) {return Map.of();}
    }
    private int contentTop() { return top+52; }
    private int contentBottom() { return height-(legacy?32:10); }
    private List<ProtocolDecisionPayloads.Category> categories() {
        var groups=new LinkedHashMap<String,ProtocolDecisionPayloads.Category>();
        for(var category:snapshot.categories())groups.put(category.id(),category);
        for(var row:snapshot.rows())groups.computeIfAbsent(row.category(),id ->
                new ProtocolDecisionPayloads.Category(id,row.categoryName(),"",row.categoryIcon(),"","",false));
        return List.copyOf(groups.values());
    }
    private void layout(GuiGraphicsExtractor g,boolean widgets) {
        if(widgets)hits.clear();
        var groups=new LinkedHashMap<String,List<ProtocolDecisionPayloads.Row>>();
        for(var row:snapshot.rows())groups.computeIfAbsent(row.category(),k -> new ArrayList<>()).add(row);
        int y=contentTop()-scroll,header=Math.max(26,pane*52/550),rowHeight=Math.max(25,pane*40/550);
        for(var category:categories()) {
            String id=category.id(),name=category.name();
            if(g!=null) {
                UiAssets.nineSlice(g,"decision/category",5,y,pane-10,header,5,1);
                if(!UiAssets.draw(g,asset(category.icon(),"icon"),10,y+3,header-6,header-6))
                    UiAssets.draw(g,"decision/category_generic",10,y+3,header-6,header-6);
                fitted(g,name,header+8,y,pane-header-43,header,UiMenuStyle.TEXT);
                UiAssets.draw(g,collapsed.contains(id)?"recruitment/expand":"recruitment/collapse",pane-27,y+7,17,header-14);
            }
            if(widgets && visible(y,header)) {
                control(name,6,y,pane-12,header,true,"ui.decision.tab",() -> {if(!collapsed.add(id))collapsed.remove(id);rebuildWidgets();});
                hits.add(new ScreenDecisionHit("category/"+id,name+(category.description().isEmpty()?"":"\n"+category.description()),y,header));
            }
            y+=header+3;
            if(collapsed.contains(id))continue;
            y=body(g,category,y,widgets);
            for(var row:groups.getOrDefault(id,List.of())) {
                if(g!=null)row(g,row,y,rowHeight);
                if(widgets && visible(y,rowHeight)) {
                    hits.add(new ScreenDecisionHit(row.id(),tooltip(row),y,rowHeight));
                    control(row.name(),7,y,pane-14,rowHeight,row.enabled() && !pending() && !snapshot.loading(),"ui.decision.take",
                            () -> {request(ProtocolDecisionPayloads.Action.TAKE,row.id(),legacy?legacyPage:0);rebuildWidgets();});
                }
                y+=rowHeight+3;
            }
            y+=7;
        }
        total=y-contentTop()+scroll;
    }
    private boolean visible(int y,int height) { return y>=contentTop() && y+height<=contentBottom(); }
    private int body(GuiGraphicsExtractor g,ProtocolDecisionPayloads.Category category,int y,boolean widgets) {
        int start=y,bodyWidth=pane-22;
        if(g!=null) {
            int bodyHeight=body(null,category,y,false)-y;
            if(bodyHeight>0)UiAssets.nineSlice(g,assets.getOrDefault("GFX_tiled_decisions_bg_small","doctrine/gfx_tiled_decisions_bg_small"),7,y,pane-14,bodyHeight,4,1);
        }
        String picture=asset(category.picture(),"picture");
        int pictureHeight=UiAssets.imageHeight(picture,bodyWidth);
        if(pictureHeight>0) {
            pictureHeight=Math.min(pictureHeight,pane);
            if(g!=null)UiAssets.draw(g,picture,11,y,bodyWidth,pictureHeight);
            y+=pictureHeight+4;
        }
        if(!category.description().isEmpty()) {
            int lineHeight=font.lineHeight+2;
            for(var line:text.rows(category.description())) {
                var matcher=BODY_IMAGE.matcher(line.getString());
                String path="";
                if(matcher.find() && line.getString().replace(matcher.group(),"").isBlank())
                    path=assets.getOrDefault(matcher.group(1),assets.getOrDefault("GFX_"+matcher.group(1),""));
                int imageHeight=UiAssets.imageHeight(path,bodyWidth);
                if(imageHeight>lineHeight*2) {
                    imageHeight=Math.min(imageHeight,pane);
                    if(g!=null)UiAssets.draw(g,path,11,y,bodyWidth,imageHeight);
                    y+=imageHeight+3;
                } else for(var part:font.split(line,bodyWidth).isEmpty()?List.of(net.minecraft.util.FormattedCharSequence.EMPTY):font.split(line,bodyWidth)) {
                    if(g!=null)g.text(font,part,11+(bodyWidth-font.width(part))/2,y,UiMenuStyle.TEXT);
                    y+=lineHeight;
                }
            }
        }
        if(category.panel()!=null) {
            var result=panels.render(g,font,category.panel(),assets,text,11,y,bodyWidth);
            if(widgets)for(var hit:result.hits()) {
                String tooltip=hit.immediate()+hit.delayed();
                hits.add(new ScreenDecisionHit("panel/"+category.id()+"/"+hit.id(),tooltip,hit.x(),hit.y(),hit.width(),hit.height()));
            }
            y+=result.height();
        }
        return y==start?y:y+5;
    }
    private void row(GuiGraphicsExtractor g,ProtocolDecisionPayloads.Row row,int y,int height) {
        int frame=row.phase().equals("ACTIVE")?1:row.enabled()?2:0;
        String background="decision/ui/row/frame_"+frame;
        if(UiAssets.imageHeight(background,100)==0)background=row.enabled()?"decision/row":"decision/row_disabled";
        UiAssets.nineSlice(g,background,7,y,pane-14,height,4,1);
        if(!UiAssets.draw(g,asset(row.icon(),"icon"),10,y+3,20,height-6))
            UiAssets.draw(g,"menu/decisions",10,y+3,20,height-6);
        String cost=row.remainingHours()>0?(row.remainingHours()+23)/24+"일":
                row.costText().isEmpty()?format(row.cost()):row.costText();
        int costWidth=Math.min(pane/3,Math.max(22,font.width(text.component(cost))+6));
        boolean political=row.remainingHours()==0 && (row.costText().isEmpty() || row.costText().matches("[0-9]+(?:\\.[0-9]+)?"));
        int right=pane-31,costLeft=right-costWidth;
        if(political)UiAssets.draw(g,"hud/political_power",costLeft-14,y+5,12,height-10);
        fitted(g,cost,costLeft,y,costWidth,height,0xFFFFCC33);
        fitted(g,row.name(),34,y,Math.max(10,costLeft-38-(political?14:0)),height,row.enabled()?UiMenuStyle.TEXT:UiMenuStyle.MUTED);
        UiAssets.draw(g,row.remainingHours()>0?"decision/timer":"decision/select",pane-26,y+4,15,height-8);
    }
    private String tooltip(ProtocolDecisionPayloads.Row row) {
        String body=row.tooltip().isEmpty()?row.description():row.tooltip();
        var result=new StringBuilder(row.name());
        if(!body.isEmpty())result.append("\n").append(body);
        appendState(result,row,body);
        return result.toString();
    }
    private void appendState(StringBuilder result,ProtocolDecisionPayloads.Row row,String body) {
        if(row.costText().isEmpty() || row.costText().matches("[0-9]+(?:\\.[0-9]+)?"))
            result.append("\n정치력: ").append(row.costText().isEmpty()?format(row.cost()):row.costText());
        else if(!body.equals(row.costText()) && !body.endsWith("\n"+row.costText()))result.append("\n").append(row.costText());
        if(!row.status().isEmpty())result.append("\n").append(row.status());
        if(row.remainingHours()>0)result.append("\n").append(row.phase().equals("COOLDOWN")?"재사용 대기: ":"남은 기간: ")
                .append((row.remainingHours()+23)/24).append("일");
    }
    private void fitted(GuiGraphicsExtractor g,String value,int x,int y,int width,int height,int color) {
        var component=text.component(value.replace("\n"," "));
        int actual=font.width(component);
        float scale=actual==0?1:Math.min(1,(float)width/actual);
        g.pose().pushMatrix();
        g.pose().translate(x+(width-actual*scale)/2f,y+(height-font.lineHeight*scale)/2f);
        g.pose().scale(scale);
        g.text(font,component,0,0,color);
        g.pose().popMatrix();
    }
    private void control(String name,int x,int y,int width,int height,boolean enabled,String sound,Runnable action) {
        var button=new Button(x,y,width,height,text.component(name),b -> action.run(),message -> message.get()) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {}
            @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) {
                if(sound.equals("ui.decision.tab")) {
                    if(tabSound!=null)manager.stop(tabSound);
                    tabSound=dev.hoi.client.audio.AudioUiSounds.instance(sound);
                    manager.play(tabSound);
                } else dev.hoi.client.audio.AudioUiSounds.play(sound);
            }
        };
        button.active=enabled;addRenderableWidget(button);
    }
    private String asset(String name,String kind) {
        if(name.isEmpty())return "";
        String mapped=assets.get(name);
        return mapped!=null?mapped:name.matches("[A-Za-z0-9_]+")?"decision/"+kind+"/"+name.toLowerCase(Locale.ROOT):"";
    }
    private static String format(Double value) { return value==null?"?":java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        var view=snapshot.view();
        UiMenuBar.draw(g,width,view==null?ProtocolCountryHud.UNKNOWN:view.hud(),mx,my,0);
        UiMenuStyle.panel(g,0,top,pane,height-top);
        g.text(font,"사건과 결정",10,top+9,UiMenuStyle.TEXT);
        String message=snapshot.loading()?"서버 정보 불러오는 중…":view==null?"서버 정보 불러오는 중…":
                view.message().isEmpty()?"국가중점과 사건에 따른 결정":view.message();
        g.text(font,font.plainSubstrByWidth(message,pane-20),10,top+34,UiMenuStyle.MUTED);
        g.enableScissor(3,contentTop(),pane-3,contentBottom());layout(g,false);g.disableScissor();
        if(view!=null && categories().isEmpty())g.text(font,"선택 가능한 결정 없음",12,contentTop()+12,UiMenuStyle.MUTED);
        super.extractRenderState(g,mx,my,delta);
        ScreenDecisionHit selected=null;
        if(mx>=7 && mx<pane-7 && my>=contentTop() && my<contentBottom())
            for(var hit:hits)if(mx>=hit.x() && (long)mx<hit.x()+(long)hit.width() && my>=hit.y() && my<hit.y()+hit.height())selected=hit;
        String id=selected==null?"":selected.id();
        if(!id.equals(hover)) {hover=id;tooltipScroll=0;}
        tooltipBounds=selected==null?null:text.tooltip(g,font,detailText(selected),mx,my,width,height,tooltipScroll);
        if(tooltipBounds!=null)tooltipScroll=tooltipBounds.scroll();
    }
    private String detailText(ScreenDecisionHit hit) {
        var detail=details.get(hit.id());
        if(detail==null)return hit.tooltip();
        var result=new StringBuilder(detail.name());
        if(!detail.description().isEmpty())result.append('\n').append(detail.description());
        for(var section:detail.sections()) {
            if(section.lines().isEmpty())continue;
            String title=switch(section.phase()) {
                case READY_REQUIREMENTS -> "선택 조건:";
                case COMPLETE -> "선택 시 효과:";
                case MODIFIER -> "활성 중 효과:";
                case REMOVE -> "기간 만료 시 효과:";
                case CANCEL -> "취소 시 효과:";
                case TIMEOUT -> "기한 만료 시 효과:";
            };
            result.append("\n\n§Y").append(title).append("§!");
            for(var line:section.lines()) {
                result.append('\n').append("  ".repeat(line.depth()));
                if(section.phase()==ProtocolDecisionDetails.Phase.READY_REQUIREMENTS)result.append(switch(line.assessment()) {
                    case MET -> "§G✓§! ";case UNMET -> "§R×§! ";case UNKNOWN,NOT_EVALUATED -> "§g?§! ";
                });
                result.append(line.text());
            }
        }
        if(!detail.limitation().isEmpty())result.append("\n\n§g").append(detail.limitation()).append("§!");
        var row=snapshot.rows().stream().filter(value->value.id().equals(hit.id())).findFirst().orElse(null);
        if(row!=null) {result.append('\n');appendState(result,row,detail.description());}
        return result.toString();
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        boolean overTooltip=tooltipBounds!=null && x>=tooltipBounds.x()-4 && x<tooltipBounds.x()+tooltipBounds.width()+4
                && y>=tooltipBounds.y()-4 && y<tooltipBounds.y()+tooltipBounds.height()+4;
        boolean overSource=x>=7 && x<pane-7 && hits.stream().anyMatch(hit->hit.id().equals(hover) && y>=hit.y() && y<hit.y()+hit.height());
        if(tooltipBounds!=null && tooltipBounds.maxScroll()>0 && !hover.isEmpty() && (overTooltip || overSource)) {
            tooltipScroll=Math.clamp(tooltipScroll-(int)(vertical*(font.lineHeight+1)*3),0,tooltipBounds.maxScroll());return true;
        }
        if(x>=0 && x<pane && y>=contentTop() && y<contentBottom()) {
            scroll=Math.clamp(scroll-(int)(vertical*28),0,Math.max(0,total-contentBottom()+contentTop()));rebuildWidgets();return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void onClose() { ScreenDialogCoordinator.setContent(null); }
    @Override public void removed() { panels.close();super.removed(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public int unitHudLeft() { return pane; }
    @Override public boolean allowsMovement() { return true; }
}
