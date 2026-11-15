package dev.hoi.client.screen.decision;

import com.google.gson.JsonParser;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.dialog.DialogStack;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.decision.*;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class DecisionScreen extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static net.minecraft.client.resources.sounds.SoundInstance tabSound;
    private static final Pattern BODY_IMAGE = Pattern.compile("£([A-Za-z0-9_]+)£?");
    private String token = UUID.randomUUID().toString();
    private final Consumer<DecisionPayloads.Request> sender;
    private final boolean fixture;
    final DecisionPreferences preferences;
    private final dev.hoi.client.map.MapDecisionOverlay mapOverlay=new dev.hoi.client.map.MapDecisionOverlay();
    private final Set<String> highlighted=new HashSet<>();
    private Map<Integer,Integer> highlightColors=Map.of();
    private String mapRow="",mapDimension="";
    private int highlightTick=-100,mapTicks;
    public String mapDimension() {
        var authority=snapshot.authority();
        return !fixture && authority!=null && !authority.country().equals(dev.hoi.client.ClientCampaignHud.country())?"":mapDimension;
    }
    private final Set<String> collapsed = new HashSet<>();
    private final DecisionSnapshot snapshot;
    private final DecisionText text = new DecisionText();
    private final DecisionPanel panels = new DecisionPanel();
    private Map<String,String> assets = Map.of();
    private int pane, top, scroll, total, ticks = 99, tooltipScroll;
    private long marqueeTicks;
    private long pendingRevision = -1;
    private boolean legacy;
    private boolean draggingScroll;
    private int scrollGrab;
    private Boolean modern;
    private int legacyPage;
    private String hover = "";
    private Tooltips.ScrolledTooltip tooltipBounds;
    private record DecisionHit(String id,String tooltip,int x,int y,int width,int height) {
        DecisionHit(String id,String tooltip,int y,int height) { this(id,tooltip,7,y,Integer.MAX_VALUE,height); }
    }
    private final List<DecisionHit> hits = new ArrayList<>();
    private final Map<String,DecisionDetailPayloads.View> details = new HashMap<>();
    private final Map<String,Integer> detailRequests = new HashMap<>();
    private int detailTicks;
    private DialogStack eventStack;
    private List<DialogView> eventRows = List.of();
    public void eventStack(DialogStack stack) { eventStack = stack; if (width > 0) rebuildWidgets(); }
    private DialogStack dialogs() { return eventStack == null ? DialogCoordinator.eventStack() : eventStack; }
    public int eventCount() { return eventRows.size(); }
    public DecisionScreen() { this(null,ClientPlayNetworking::send,false); }
    public DecisionScreen(DecisionPayloads.View view,Consumer<DecisionPayloads.Request> sender) { this(view,sender,true); }
    private DecisionScreen(DecisionPayloads.View view,Consumer<DecisionPayloads.Request> sender,boolean fixture) {
        super(Component.literal("사건과 결정"));snapshot=new DecisionSnapshot(view);this.sender=sender;this.fixture=fixture;if(view!=null)modern=true;
        try { preferences=fixture?new DecisionPreferences(null):DecisionPreferences.current(); }
        catch(java.io.IOException impossible) { throw new IllegalStateException(impossible); }
    }
    public String token() { return token; }
    public static void resourcesReloaded() {
        net.minecraft.client.Minecraft.getInstance().execute(()->{
            if(DialogCoordinator.contentScreen() instanceof DecisionScreen screen) {
                screen.panels.close();screen.text.assets(new com.google.gson.JsonObject());screen.rebuildWidgets();
            }
        });
    }
    int scrollOffset() { return scroll; }
    boolean collapsed(String id) { return collapsed.contains(id); }
    int rowCount() { return snapshot.rows().size(); }
    boolean pending() { return pendingRevision>=0; }
    public void update(DecisionPayloads.Response packet) {
        if(!packet.token().equals(token))return;
        if(packet.json().isEmpty()) {clearMap();DialogCoordinator.setContent(null);return;}
        DecisionPayloads.View next;
        try {next=packet.view();} catch(IllegalArgumentException invalid) {return;}
        boolean nextModern=JsonParser.parseString(packet.json()).getAsJsonObject().has("pageCount");
        if(modern!=null && modern!=nextModern)return;
        modern=nextModern;legacy=!modern;
        var result=legacy?snapshot.acceptLegacy(next):snapshot.accept(next);
        if(result==DecisionSnapshot.Acceptance.REVOKED) {DialogCoordinator.setContent(null);return;}
        if(result==DecisionSnapshot.Acceptance.STAGED) {
            clearMap();
            request(DecisionPayloads.Action.REFRESH,"",snapshot.nextPage());
            rebuildWidgets();
        } else if(result==DecisionSnapshot.Acceptance.APPLIED) {
            clearMap();
            draggingScroll=false;
            details.entrySet().removeIf(entry->!entry.getValue().country().equals(next.country())||!entry.getValue().session().equals(next.session())||snapshot.rows().stream().noneMatch(row->row.id().equals(entry.getKey())));detailRequests.clear();
            if(legacy || next.revision()>pendingRevision)pendingRevision=-1;
            if(legacy)legacyPage=next.page();
            text.clear();ticks=0;rebuildWidgets();
        }
    }
    public void update(DecisionPayloads.Highlight packet) {
        var authority=snapshot.authority();
        if(authority==null || snapshot.loading() || pending() || !packet.token().equals(token)
                || !packet.session().equals(authority.session()) || packet.revision()!=authority.revision()
                || !highlighted.contains(packet.row()) || !mapDimension().equals(dimension()))return;
        var row=snapshot.rows().stream().filter(value->value.id().equals(packet.row())).findFirst().orElse(null);
        if(row!=null && !row.highlightStates().isEmpty())mapOverlay.receive(packet,mapDimension,mapColor(row));
    }
    private String dimension() { return minecraft.level==null?"":minecraft.level.dimension().identifier().toString(); }
    private int mapColor(DecisionPayloads.Row row) { return row.highlightColor()==null?0xffffffff:highlightColors.getOrDefault(row.highlightColor(),0xffffffff); }
    private void clearMap() { mapOverlay.clear();highlighted.clear();mapRow="";mapDimension=""; }
    void mapTick() {
        mapTicks++;
        String dimension=dimension();
        if(!mapDimension.equals(dimension)) { clearMap();mapDimension=dimension; }
        var row=snapshot.rows().stream().filter(value->value.id().equals(hover)).findFirst().orElse(null);
        String id=row==null || row.highlightStates().isEmpty() || snapshot.loading() || pending()?"":row.id();
        if(!id.equals(mapRow)) {mapRow=id;mapOverlay.select(id,dimension,row==null?0xffffffff:mapColor(row));}
        if(!id.isEmpty() && !dimension.isEmpty() && !highlighted.contains(id) && mapTicks-highlightTick>=4
                && (fixture || ClientPlayNetworking.canSend(DecisionPayloads.Request.TYPE))) {
            highlighted.add(id);highlightTick=mapTicks;request(DecisionPayloads.Action.HIGHLIGHT,id,0);
        }
    }
    public void update(DecisionDetailPayloads.Response packet) {
        if(!packet.token().equals(token) || snapshot.loading())return;
        DecisionDetailPayloads.View next;
        try {next=packet.view();} catch(IllegalArgumentException invalid) {return;}
        var authority=snapshot.authority();
        if(authority==null || !authority.country().equals(next.country()) || !authority.session().equals(next.session())
                || authority.revision()!=next.revision() || !detailRequests.containsKey(next.id())
                || snapshot.rows().stream().noneMatch(row->row.id().equals(next.id())))return;
        details.put(next.id(),next);text.clear();
    }
    private void request(DecisionPayloads.Action action,String id,int page) {
        if(!fixture && !ClientPlayNetworking.canSend(DecisionPayloads.Request.TYPE))return;
        var authority=snapshot.authority();
        if(action==DecisionPayloads.Action.TAKE) {
            if(pending() || snapshot.loading() || authority==null)return;
            pendingRevision=authority.revision();
            clearMap();
        }
        if(legacy) {
            token=UUID.randomUUID().toString();
            if(action==DecisionPayloads.Action.REFRESH)legacyPage=page;
        }
        sender.accept(new DecisionPayloads.Request(action,token,authority==null?"":authority.session(),
                authority==null?0:authority.revision(),id,page));
        ticks=0;
    }
    @Override public void tick() {
        marqueeTicks++;
        if(fixture)return;
        mapTick();
        if(snapshot.authority()!=null && !snapshot.authority().country().equals(dev.hoi.client.ClientCampaignHud.country())) {
            onClose();return;
        }
        detailTicks++;
        var authority=snapshot.authority();
        if(!hover.isEmpty() && !snapshot.loading() && !pending() && authority!=null && (!details.containsKey(hover)||details.get(hover).revision()!=authority.revision())
                && detailTicks-detailRequests.getOrDefault(hover,-120)>=120
                && snapshot.rows().stream().anyMatch(row->row.id().equals(hover))
                && ClientPlayNetworking.canSend(DecisionDetailPayloads.Request.TYPE)) {
            detailRequests.put(hover,detailTicks);
            ClientPlayNetworking.send(new DecisionDetailPayloads.Request(token,authority.session(),authority.revision(),hover));
        }
        if(++ticks>=(snapshot.loading()?20:80))
            request(snapshot.authority()==null?DecisionPayloads.Action.OPEN:DecisionPayloads.Action.REFRESH,"",legacy?legacyPage:snapshot.nextPage());
    }
    @Override protected void init() {
        eventRows = dialogs() == null ? List.of() : dialogs().events();
        pane=UiPanelLayout.width(MenuTab.DECISIONS,width);top=HudMenuBar.height(width);
        assets=loadAssets();text.clear();panels.clear();
        pane=Math.max(1,Math.min(275,width-16));
        var view=snapshot.view();
        HudMenuBar.buttons(width,view==null?"":view.country(),MenuTab.DECISIONS,tab -> {
            if(tab==MenuTab.RESEARCH)HoiClient.open();else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new UiMenuButton("×",pane-25,top+3,19,19,this::onClose));
        layout(null,false);
        scroll=Math.clamp(scroll,0,Math.max(0,total-contentBottom()+contentTop()));
        layout(null,true);
        if(legacy && view!=null && view.total()>DecisionPayloads.PAGE_SIZE) {
            var back=new UiMenuButton("←",8,height-27,36,21,() -> request(DecisionPayloads.Action.REFRESH,"",legacyPage-1));
            back.active=legacyPage>0 && !pending();addRenderableWidget(back);
            var next=new UiMenuButton("→",pane-44,height-27,36,21,() -> request(DecisionPayloads.Action.REFRESH,"",legacyPage+1));
            next.active=(legacyPage+1)*DecisionPayloads.PAGE_SIZE<view.total() && !pending();addRenderableWidget(next);
        }
    }
    private Map<String,String> loadAssets() {
        text.flags(new com.google.gson.JsonObject());
        var flags=minecraft.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:ui/decision_flags.json"));
        if(flags.isPresent())try(var input=flags.get().open()) {
            byte[] bytes=input.readNBytes(262145);
            if(bytes.length<=262144)text.flags(JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
        } catch(Exception invalid) {text.flags(new com.google.gson.JsonObject());}
        var resource=minecraft.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:ui/decision_assets.json"));
        if(resource.isEmpty())return Map.of();
        try(var input=resource.get().open()) {
            byte[] bytes=input.readNBytes(262145);
            if(bytes.length>262144)return Map.of();
            var root=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("version").getAsInt()!=1)return Map.of();
            text.assets(root);
            panels.assets(root);
            var colors=new HashMap<Integer,Integer>();
            if(root.get("highlightColors") instanceof com.google.gson.JsonArray palette)
                for(int i=0;i<Math.min(64,palette.size());i++)colors.put(i,(int)Long.parseLong(palette.get(i).getAsString(),16));
            highlightColors=Collections.unmodifiableMap(colors);
            var result=new HashMap<String,String>();
            for(var entry:root.getAsJsonObject("sprites").entrySet()) {
                String path=entry.getValue().getAsString();
                if(path.matches("[a-z0-9_/.-]+") && !path.contains(".."))result.put(entry.getKey(),path);
            }
            return Map.copyOf(result);
        } catch(Exception invalid) {return Map.of();}
    }
    private int contentTop() { return top + 26; }
    private int contentBottom() { return height-(legacy?32:10); }
    private List<DecisionPayloads.Category> categories() {
        var groups=new LinkedHashMap<String,DecisionPayloads.Category>();
        for(var category:snapshot.categories())groups.put(category.id(),category);
        for(var row:snapshot.rows())groups.computeIfAbsent(row.category(),id ->
                new DecisionPayloads.Category(id,row.categoryName(),"",row.categoryIcon(),"","",false));
        return List.copyOf(groups.values());
    }
    private void layout(GuiGraphicsExtractor g,boolean widgets) {
        int pane=this.pane-10;
        if(widgets)hits.clear();
        var groups=new LinkedHashMap<String,List<DecisionPayloads.Row>>();
        for(var row:snapshot.rows())groups.computeIfAbsent(row.category(),k -> new ArrayList<>()).add(row);
        int y=contentTop()-scroll,header=Math.max(26,pane*52/550);
        {
            int eventHeader=36;
            if (g != null) {
                UiAssets.nineSlice(g, "decision/ui/event_header", 5, y, pane - 10, eventHeader, 5, 1);
                g.text(font, Tooltips.officerText("사건",true), 16, y + 6, UiMenuStyle.TEXT,false);
            }
            popup(g,widgets,"minor","중요하지 않은 알림",58,y+5,76,26);
            popup(g,widgets,"events","사건 팝업",139,y+5,Math.max(65,pane-184),12);
            popup(g,widgets,"news","뉴스 팝업",139,y+19,Math.max(65,pane-184),12);
            y += eventHeader + 3;
            double eventScale = (pane - 14) / 512.0;
            int eventHeight = Math.max(12, (int)Math.round(33 * eventScale));
            for (var event : eventRows) {
                if (g != null) {
                    UiAssets.nineSlice(g, "decision/ui/event_row", 7, y, pane - 14, eventHeight, 4, 1);
                    UiAssets.draw(g, "decision/ui/event_type/frame_1", 7 + (int)Math.round(15 * eventScale), y + (int)Math.round(3 * eventScale), (int)Math.round(30 * eventScale), (int)Math.round(26 * eventScale));
                    int textLeft = 7 + (int)Math.round(50 * eventScale);
                    UiText.scrollingCell(g,font,text.component(event.title()),textLeft,y,pane-textLeft-80,eventHeight,UiMenuStyle.TEXT,false,marqueeTicks*50);
                    if (event.remainingDays() != null) {
                        String remaining = event.remainingDays() + "일";
                        int right = 7 + (int)Math.round(500 * eventScale);
                        g.text(font, remaining, right - font.width(remaining), y + (eventHeight - font.lineHeight) / 2, UiMenuStyle.TEXT, false);
                    }
                }
                if (widgets && visible(y, eventHeight)) control(event.title(), 7, y, pane - 14, eventHeight, true, "ui.click", () -> {
                    var stack = dialogs(); if (stack != null) stack.reopen(event.token());
                });
                y += eventHeight + 3;
            }
            if(!eventRows.isEmpty())y+=7;
        }
        var view=snapshot.view();
        String notice=view==null || snapshot.loading()?"서버 정보 불러오는 중…":view.message();
        if(!notice.isEmpty()) {
            var lines=text.lines(font,notice,pane-24);
            for(var line:lines) {
                if(g!=null)text.draw(g,font,line,12,y,UiMenuStyle.MUTED,false);
                y+=font.lineHeight+1;
            }
            y+=4;
        }
        for(var category:categories()) {
            String id=category.id(),name=category.name();
            int categoryHeight=Math.max(header,text.rows(name).size()*(font.lineHeight+1)+8);
            if(g!=null) {
                UiAssets.nineSlice(g,"decision/category",5,y,pane-10,categoryHeight,5,1);
                UiAssets.draw(g,asset(category.icon(),"icon"),10,y+(categoryHeight-header+6)/2,header-6,header-6);
                label(g,name,header+8,y,pane-header-43,categoryHeight,UiMenuStyle.TEXT,0,true);
                UiAssets.draw(g,collapsed.contains(id)?"recruitment/expand":"recruitment/collapse",pane-27,y+(categoryHeight-12)/2,12,12);
                if(!groups.getOrDefault(id,List.of()).isEmpty())UiAssets.draw(g,"decision/ui/track_category/frame_"+categoryTracking(groups.get(id)),pane-42,y+(categoryHeight-10)/2,10,10);
            }
            if(widgets && visible(y,categoryHeight)) {
                control(name,6,y,pane-50,categoryHeight,true,"ui.decision.tab",() -> {if(!collapsed.add(id))collapsed.remove(id);rebuildWidgets();});
                control("분류 접기: "+name,pane-28,y,17,categoryHeight,true,"ui.decision.tab",() -> {if(!collapsed.add(id))collapsed.remove(id);rebuildWidgets();});
                hits.add(new DecisionHit("category/"+id,"§Y"+text.component(name).getString()+"§!",y,categoryHeight));
                var rows=groups.getOrDefault(id,List.of());
                if(!rows.isEmpty()) {
                    control("분류 추적: "+text.component(name).getString(),pane-43,y,13,categoryHeight,true,"ui.decision.tab",()->{
                        boolean enabled=categoryTracking(rows)!=1;
                        preferences.tracking(server(),viewCountry(),rows.stream().map(DecisionPayloads.Row::id).toList(),enabled);
                        rebuildWidgets();
                    });
                    hits.add(new DecisionHit("track-category/"+id,dev.hoi.client.ui.UiCommandLabels.text("CLICK_TO_TOGGLE_ALL_CATEGORY_DECISION_TRACKING",""),pane-43,y,13,categoryHeight));
                }
            }
            y+=categoryHeight+3;
            if(collapsed.contains(id))continue;
            y=body(g,category,y,widgets);
            for(var row:groups.getOrDefault(id,List.of())) {
                int rowHeight=rowHeight(row);
                if(g!=null)row(g,row,y,rowHeight);
                if(widgets && visible(y,rowHeight)) {
                    hits.add(new DecisionHit(row.id(),tooltip(row),y,rowHeight));
                    control(rowName(row),7,y,pane-52,rowHeight,row.enabled() && !pending() && !snapshot.loading(),"ui.decision.take",
                            () -> {request(DecisionPayloads.Action.TAKE,row.id(),legacy?legacyPage:0);rebuildWidgets();});
                    control("결정 실행: "+text.component(rowName(row)).getString(),pane-30,y,20,rowHeight,row.enabled() && !pending() && !snapshot.loading(),"ui.decision.take",
                            () -> {request(DecisionPayloads.Action.TAKE,row.id(),legacy?legacyPage:0);rebuildWidgets();});
                    control("추적: "+text.component(rowName(row)).getString(),pane-44,y,12,rowHeight,true,"ui.decision.tab",()->{
                        preferences.tracking(server(),viewCountry(),row.id(),!tracking(row));rebuildWidgets();
                    });
                    hits.add(new DecisionHit("track/"+row.id(),dev.hoi.client.ui.UiCommandLabels.text(tracking(row)?"DECISION_KEEP_NOTIFIED":"DECISION_NOT_KEEP_NOTIFIED",""),pane-44,y,12,rowHeight));
                }
                y+=rowHeight+3;
            }
            y+=7;
        }
        total=y-contentTop()+scroll;
    }
    private String server() { return fixture?"fixture":DecisionPreferences.server(); }
    private String viewCountry() { var view=snapshot.view();return view==null?"":view.country(); }
    private boolean tracking(DecisionPayloads.Row row) { return preferences.tracking(server(),viewCountry(),row.id()); }
    private int categoryTracking(List<DecisionPayloads.Row> rows) {
        long tracked=rows.stream().filter(this::tracking).count();return tracked==rows.size()?1:tracked==0?0:2;
    }
    private void popup(GuiGraphicsExtractor g,boolean widgets,String id,String label,int x,int y,int width,int height) {
        if(g!=null) {
            UiAssets.draw(g,"decision/ui/checkbox/frame_"+(preferences.popup(id)?1:0),x,y,11,11);
            label(g,label,x+14,y,width-14,height,UiMenuStyle.TEXT,-1);
        }
        if(widgets && visible(y,height)) {
            control(label,x,y,width,height,true,"ui.decision.tab",()->{preferences.popup(id,!preferences.popup(id));rebuildWidgets();});
            String tooltip=switch(id) {
                case "minor" -> "에이스 조종사 사건 같이 작은 사건 알림을 전환하려면 §G클릭§!하십시오.";
                case "news" -> "뉴스 알림을 전환하려면 §G클릭§!하십시오";
                default -> "사건 알림을 전환하려면 §G클릭§!하십시오";
            };
            hits.add(new DecisionHit("popup/"+id,tooltip,x,y,width,height));
        }
    }
    private boolean visible(int y,int height) { return y>=contentTop() && y+height<=contentBottom(); }
    private int body(GuiGraphicsExtractor g,DecisionPayloads.Category category,int y,boolean widgets) {
        int pane=this.pane-10;
        int start=y,bodyWidth=pane-22;
        if(g!=null) {
            int bodyHeight=body(null,category,y,false)-y;
            if(bodyHeight>0)UiAssets.tiledNineSlice(g,assets.getOrDefault("GFX_tiled_decisions_bg_small","doctrine/gfx_tiled_decisions_bg_small"),7,y,pane-14,bodyHeight,16,.5);
        }
        String picture=asset(category.picture(),"picture");
        int pictureWidth=Math.min(bodyWidth,(int)Math.round(UiAssets.imageSize(picture)[0]*pane/550.0));
        int pictureHeight=UiAssets.imageHeight(picture,pictureWidth);
        if(pictureHeight>0) {
            pictureHeight=Math.min(pictureHeight,pane);
            if(g!=null)UiAssets.draw(g,picture,11+(bodyWidth-pictureWidth)/2,y,pictureWidth,pictureHeight);
            y+=pictureHeight+4;
        }
        if(!category.description().isEmpty()) {
            int lineHeight=font.lineHeight+2;
            var rows=text.sourceRows(category.description());
            for(int index=0;index<rows.size();index++) {
                var line=rows.get(index);
                if(line.getString().isBlank() && (index==0 || !rows.get(index-1).getString().isBlank())) {
                    int image=index;
                    while(image<rows.size() && rows.get(image).getString().isBlank())image++;
                    String path=image<rows.size()?bodyImage(rows.get(image)):"";
                    int imageWidth=Math.min(bodyWidth,(int)Math.round(UiAssets.imageSize(path)[0]*pane/550.0));
                    int imageHeight=UiAssets.imageHeight(path,imageWidth);
                    int after=image+1;
                    while(after<rows.size() && rows.get(after).getString().isBlank())after++;
                    if(image-index>=3 && after-image-1>=3 && imageHeight>lineHeight*2) {
                        int padding=Math.max(4,pane*10/550);
                        y+=padding;
                        if(g!=null)UiAssets.draw(g,path,11+(bodyWidth-imageWidth)/2,y,imageWidth,imageHeight);
                        y+=imageHeight+padding;
                        index=after-1;
                        continue;
                    }
                    if(index>0 && image<rows.size()) {
                        y+=lineHeight;
                        index=image-1;
                        continue;
                    }
                }
                String path=bodyImage(line);
                int imageWidth=Math.min(bodyWidth,(int)Math.round(UiAssets.imageSize(path)[0]*pane/550.0));
                int imageHeight=UiAssets.imageHeight(path,imageWidth);
                if(imageHeight>lineHeight*2) {
                    imageHeight=Math.min(imageHeight,pane);
                    if(g!=null)UiAssets.draw(g,path,11+(bodyWidth-imageWidth)/2,y,imageWidth,imageHeight);
                    y+=imageHeight+3;
                } else for(var part:font.split(TooltipKeywordIcons.decorateLines(line,false),bodyWidth).isEmpty()?List.of(net.minecraft.util.FormattedCharSequence.EMPTY):font.split(TooltipKeywordIcons.decorateLines(line,false),bodyWidth)) {
                    if(g!=null)text.draw(g,font,part,11,y,UiMenuStyle.TEXT,false);
                    y+=lineHeight;
                }
            }
        }
        if(category.panel()!=null) {
            int reserved=0;
            if(category.description().endsWith("\n") && category.panel().elements().stream().anyMatch(element->negativePanel(element))) {
                var rows=text.rows(category.description());
                for(int index=rows.size()-1;index>=0 && rows.get(index).getString().isBlank();index--)reserved+=font.lineHeight+2;
                if(reserved<3*(font.lineHeight+2))reserved=0;
            }
            y-=reserved;
            var result=panels.render(g,font,category.panel(),assets,text,11,y,bodyWidth);
            if(widgets)for(var hit:result.hits()) {
                String tooltip=hit.immediate()+hit.delayed();
                hits.add(new DecisionHit("panel/"+category.id()+"/"+hit.id(),tooltip,hit.x(),hit.y(),hit.width(),hit.height()));
            }
            y+=result.height();
        }
        return y==start?y:y+5;
    }
    private static boolean negativePanel(DecisionPanelView.Element element) {
        return element.layout().y()!=null && element.layout().y()<0 || element.children().stream().anyMatch(DecisionScreen::negativePanel);
    }
    private String bodyImage(Component line) {
        var matcher=BODY_IMAGE.matcher(line.getString());
        return matcher.find() && line.getString().replace(matcher.group(),"").isBlank()
                ?assets.getOrDefault(matcher.group(1),assets.getOrDefault("GFX_"+matcher.group(1),"")):"";
    }
    private void row(GuiGraphicsExtractor g,DecisionPayloads.Row row,int y,int height) {
        int pane=this.pane-10;
        String background="decision/ui/row/frame_"+(row.phase().equals("ACTIVE")?0:1);
        if(UiAssets.imageHeight(background,100)==0)background=row.enabled()?"decision/row":"decision/row_disabled";
        UiAssets.nineSlice(g,background,7,y,pane-14,height,4,1);
        String cost=rowCost(row);
        int costWidth=costWidth(row);
        int right=pane-49,costLeft=right-costWidth;
        int costColor=row.remainingHours()>0?0xFFFFFF:Boolean.FALSE.equals(row.costMet())?Tooltips.BAD:Tooltips.GOLD;
        var costRows=text.rows(cost).stream().map(line->TooltipKeywordIcons.decorateLines(line,false)).toList();
        int costTop=y+(height-costRows.size()*(font.lineHeight+1))/2;
        for(var line:costRows) {
            var coloredCost=Component.empty();
            line.visit((style,part)->{
                part.codePoints().forEach(character->coloredCost.append(Component.literal(new String(Character.toChars(character))).withStyle(Character.isDigit(character)||".,%+-".indexOf(character)>=0?style.withColor(costColor):style)));
                return Optional.empty();
            },net.minecraft.network.chat.Style.EMPTY);
            int padding=Math.max(0,costWidth-font.width(coloredCost));
            text.scrollingCell(g,font,coloredCost,costLeft+padding,costTop,costWidth-padding,font.lineHeight+1,0xFFFFFFFF,false,marqueeTicks*50);
            costTop+=font.lineHeight+1;
        }
        String image=asset(row.icon(),"icon");
        boolean hasImage=UiAssets.imageHeight(image,20)>0;
        String flag=row.target()!=null&&row.target().kind().equals("country")?UiStoryAssets.flag(row.target().id(),row.target().flag()):"";
        int[] flagSize=flag.isEmpty()?new int[]{0,0}:UiAssets.contentSize(flag);
        int flagHeight=flagSize[1]>0?font.lineHeight:0,flagWidth=flagHeight==0?0:Math.max(1,Math.round((float)flagSize[0]*flagHeight/flagSize[1]));
        int nameLeft=hasImage?10:34,prefix=(hasImage?24:0)+(flagWidth==0?0:flagWidth+4),nameWidth=Math.max(10,costLeft-nameLeft-4);
        var nameRows=text.rows(rowName(row)).stream().map(line->TooltipKeywordIcons.decorateLines(line,false)).toList();
        int contentWidth=prefix+nameRows.stream().mapToInt(font::width).max().orElse(0);
        int nameX=nameLeft-UiText.scrollingOffset(Math.max(0,contentWidth-nameWidth),marqueeTicks*50);
        g.enableScissor(nameLeft,y,nameLeft+nameWidth,y+height);
        if(hasImage)UiAssets.draw(g,image,nameX,y+3,20,height-6);
        if(flagWidth>0)UiAssets.drawContent(g,flag,nameX+(hasImage?24:0),y+(height-flagHeight)/2,flagWidth,flagHeight);
        int nameTop=y+(height-nameRows.size()*(font.lineHeight+1))/2;
        for(var line:nameRows) {
            var sequence=line.getVisualOrderText();
            text.draw(g,font,sequence,nameX+prefix,Math.round(UiText.centeredY(font,sequence,nameTop,font.lineHeight+1,1)),0xFFFFFFFF,false);
            nameTop+=font.lineHeight+1;
        }
        g.disableScissor();
        String action=row.remainingHours()>0?"decision/timer":"decision/select";
        int actionHeight=UiAssets.imageHeight(action,20);
        UiAssets.draw(g,action,pane-30,y+(height-actionHeight)/2,20,actionHeight);
        UiAssets.draw(g,"decision/ui/track_decision/frame_"+(tracking(row)?1:0),pane-44,y+(height-10)/2,10,10);
        if(!row.progress().isEmpty() && row.totalHours()!=null && row.totalHours()>0) {
            String path="decision/ui/progress_"+row.progress().toLowerCase(Locale.ROOT)+"/";
            double progress=Math.clamp(1-row.remainingHours()/(double)row.totalHours(),0,1);
            int left=10,barWidth=pane-24,barY=y+height-5;
            UiAssets.draw(g,path+"texturefile2",left,barY,barWidth,4);
            int fill=(int)Math.round(barWidth*progress);
            if(fill>0)g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+path+"texturefile1.png"),left,barY,left+fill,barY+4,0,(float)progress,0,1);
        }
    }
    private String rowOutline(DecisionPayloads.Row row) {
        var matcher=BODY_IMAGE.matcher(rowName(row));
        String token="";
        while(matcher.find())if(matcher.group(1).endsWith("_texticon") && rowName(row).substring(matcher.end()).isBlank()
                && !text.component(matcher.group()).getString().contains("£"))token=matcher.group();
        return token;
    }
    private String rowLabel(DecisionPayloads.Row row) {
        String outline=rowOutline(row);
        String name=rowName(row);
        return outline.isEmpty()?name:name.substring(0,name.lastIndexOf(outline)).stripTrailing();
    }
    private String rowCost(DecisionPayloads.Row row) {
        String cost=row.remainingHours()>0?(row.remainingHours()+23)/24+"일":row.costText().isEmpty()?format(row.cost()):row.costText();
        return political(row)?"£pol_power "+cost:cost;
    }
    private boolean political(DecisionPayloads.Row row) {
        return row.remainingHours()==0 && (row.costText().isEmpty() || row.costText().matches("[0-9]+(?:\\.[0-9]+)?"));
    }
    private int costWidth(DecisionPayloads.Row row) {
        return Math.min((pane-10)/2,Math.max(22,text.rows(rowCost(row)).stream().mapToInt(line->font.width(TooltipKeywordIcons.decorateLines(line,false))).max().orElse(0)+6));
    }
    private int rowHeight(DecisionPayloads.Row row) {
        int lines=Math.max(text.rows(rowLabel(row)).size(),text.rows(rowCost(row)).size());
        return Math.max(rowOutline(row).isEmpty()?20:font.lineHeight*2+4,lines*(font.lineHeight+1)+8);
    }
    private String tooltip(DecisionPayloads.Row row) {
        String body=row.tooltip().isEmpty()?row.description():row.tooltip();
        if(!row.costText().isEmpty() && text.component(body).getString().equals(text.component(row.costText()).getString()))body=row.costText();
        var result=new StringBuilder("§H").append(rowName(row)).append("§!");
        if(!body.isEmpty())result.append("\n").append(body);
        appendCost(result,row);
        return result.toString();
    }
    private static void appendCost(StringBuilder result,DecisionPayloads.Row row) {
        if(row==null || row.cost()==null || row.cost()<=0 || row.phase().equals("ACTIVE") || row.phase().equals("COOLDOWN"))return;
        boolean insufficient=Boolean.FALSE.equals(row.costMet());
        String value="§"+(insufficient?"R":"H")+String.format(Locale.ROOT,"%.0f",row.cost())+"§!";
        String cost=dev.hoi.client.ui.UiCommandLabels.text(insufficient?"DECISION_COST_NOT_ENOUGH":"DECISION_COST_DESC","")
                .replace("$VAL|H0$",value).replace("$VAL|HR0$",value);
        if(!cost.isEmpty())result.append("\n\n").append(cost);
    }
    static String rowName(DecisionPayloads.Row row) {
        return row.target()==null?row.name():row.name()+" ("+row.target().name()+")";
    }
    private void label(GuiGraphicsExtractor g,String value,int x,int y,int width,int height,int color,int alignment) {
        label(g,value,x,y,width,height,color,alignment,false);
    }
    private void label(GuiGraphicsExtractor g,String value,int x,int y,int width,int height,int color,int alignment,boolean bold) {
        var lines=text.rows(value);
        int lineHeight=font.lineHeight+1;
        int top=y+(height-lines.size()*lineHeight)/2;
        for(var line:lines) {
            text.scrollingCell(g,font,bold?line.copy().withStyle(net.minecraft.ChatFormatting.BOLD):line,x,top,width,lineHeight,color,alignment==0,marqueeTicks*50);
            top+=lineHeight;
        }
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
        return mapped!=null?(mapped.equals("menu/decisions")?"":mapped):name.matches("[A-Za-z0-9_]+")?"decision/"+kind+"/"+name.toLowerCase(Locale.ROOT):"";
    }
    private static String format(Double value) { return value==null?"0":java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        var events = dialogs() == null ? List.<DialogView>of() : dialogs().events();
        if (!events.equals(eventRows)) rebuildWidgets();
        var view=snapshot.view();
        HudMenuBar.draw(g,width,view==null?CountryHud.UNKNOWN:view.hud(),mx,my,0);
        g.enableScissor(0,top,pane,height);
        UiAssets.tiledNineSlice(g,"diplomacy/dialog/panel_dark",-32,top,pane+32,height-top+32,64,.5);
        g.disableScissor();
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/decision/ui/window_header.png"),0,top,pane,top+21,248f/794,1,0,1);
        g.pose().pushMatrix();g.pose().translate(20,top+4);g.pose().scale(1.25f);
        g.text(font,Tooltips.officerText("사건과 결정",true),0,0,UiMenuStyle.TEXT,false);g.pose().popMatrix();
        g.enableScissor(3,contentTop(),pane-13,contentBottom());layout(g,false);g.disableScissor();
        drawScrollbar(g);
        if(view!=null && categories().isEmpty())g.text(font,"선택 가능한 결정 없음",12,contentTop()+48,UiMenuStyle.MUTED);
        super.extractRenderState(g,mx,my,delta);
        DecisionHit selected=null;
        if(mx>=7 && mx<pane-13 && my>=contentTop() && my<contentBottom())
            for(var hit:hits)if(mx>=hit.x() && (long)mx<hit.x()+(long)hit.width() && my>=hit.y() && my<hit.y()+hit.height())selected=hit;
        String id=selected==null?"":selected.id();
        if(!id.equals(hover)) {hover=id;tooltipScroll=0;}
        tooltipBounds=selected==null?null:text.tooltip(g,font,detailText(selected),mx,my,width,height,tooltipScroll);
        if(tooltipBounds!=null)tooltipScroll=tooltipBounds.scroll();
    }
    private String detailText(DecisionHit hit) {
        var detail=details.get(hit.id());
        if(detail==null)return hit.tooltip();
        var row=snapshot.rows().stream().filter(value->value.id().equals(hit.id())).findFirst().orElse(null);
        var result=new StringBuilder("§H").append(detail.name());
        if(row!=null && row.target()!=null)result.append(" (").append(row.target().name()).append(")");
        result.append("§!");
        if(!detail.description().isEmpty())result.append('\n').append(detail.description());
        String mission=detail.mission();
        for(var section:detail.sections()) {
            if(section.lines().isEmpty())continue;
            String title=switch(section.phase()) {
                case READY_REQUIREMENTS -> dev.hoi.client.ui.UiCommandLabels.text(mission.isEmpty()?"DECISION_PREREQUISITES_HEADER":"TIMED_DECISION_PREREQUISITES_HEADER_"+mission,"");
                case COMPLETE -> dev.hoi.client.ui.UiCommandLabels.text(mission.isEmpty()?"DECISION_SELECT_HEADER":"TIMED_DECISION_COMPLETE_EFFECT_HEADER_"+mission,"");
                case MODIFIER -> dev.hoi.client.ui.UiCommandLabels.text(mission.isEmpty()?(section.days()>0?"DECISION_FOR_AMOUNT_DAYS_MODIFIER":"DECISION_WHILE_ACTIVE"):"TIMED_DECISION_MODIFIER_HEADER_"+(mission.equals("SELECTABLE")?"SELECTABLE_":"NORMAL_")+(section.days()>0?"DAYS":"INF"),"").replace("$AMOUNT|H$",Long.toString(section.days()));
                case REMOVE -> dev.hoi.client.ui.UiCommandLabels.text("DECISION_REMOVE_HEADER","");
                case CANCEL -> dev.hoi.client.ui.UiCommandLabels.text("DECISION_CANCEL_HEADER","");
                case TIMEOUT -> mission.isEmpty() || detail.missionDays()==0?"":dev.hoi.client.ui.UiCommandLabels.text("TIMED_DECISION_TIMEOUT_EFFECT_HEADER_"+mission,"").replace("$TIMEOUT|0$",Long.toString(detail.missionDays()));
            };
            result.append("\n\n").append(title);
            for(var line:section.lines()) {
                result.append('\n').append("  ".repeat(line.depth()));
                if(section.phase()==DecisionDetailPayloads.Phase.READY_REQUIREMENTS)result.append(switch(line.assessment()) {
                    case MET -> "§G✓§! ";case UNMET -> "§R×§! ";case UNKNOWN,NOT_EVALUATED -> "§g?§! ";
                });
                result.append(line.text());
            }
        }
        appendCost(result,row);
        return result.toString();
    }
    private int maximumScroll() { return Math.max(0,total-contentBottom()+contentTop()); }
    private int scrollThumbHeight() {
        return 8;
    }
    private int scrollThumbTop() {
        int travel=Math.max(1,contentBottom()-contentTop()-20-scrollThumbHeight());
        return contentTop()+10+(int)Math.round(travel*scroll/(double)Math.max(1,maximumScroll()));
    }
    private void drawScrollbar(GuiGraphicsExtractor g) {
        if(maximumScroll()==0)return;
        String path="commander/commands/";
        int x=pane-11,top=contentTop(),height=contentBottom()-top;
        UiAssets.nineSlice(g,path+"scrollbar_vertical_bg/frame_0",x,top,9,height,3,.5);
        UiAssets.nineSlice(g,path+"scroll_track/frame_0",x+2,top+10,5,height-20,2,.5);
        UiAssets.draw(g,path+"scroll_up/frame_0",x,top,8,8);
        UiAssets.draw(g,path+"scroll_down/frame_0",x,contentBottom()-8,8,8);
        UiAssets.draw(g,path+"scroll_drager/frame_0",x,scrollThumbTop(),8,scrollThumbHeight());
    }
    private void scrollTo(int value) {
        scroll=Math.clamp(value,0,maximumScroll());rebuildWidgets();
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice) {
        if(maximumScroll()>0 && event.x()>=pane-12 && event.x()<pane && event.y()>=contentTop() && event.y()<contentBottom()) {
            if(event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return true;
            if(event.y()<contentTop()+10)scrollTo(scroll-28);
            else if(event.y()>=contentBottom()-10)scrollTo(scroll+28);
            else if(event.y()>=scrollThumbTop() && event.y()<scrollThumbTop()+scrollThumbHeight()) {
                draggingScroll=true;scrollGrab=(int)event.y()-scrollThumbTop();
            } else scrollTo(scroll+(event.y()<scrollThumbTop()?-1:1)*(contentBottom()-contentTop()));
            return true;
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy) {
        if(draggingScroll && event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            int travel=Math.max(1,contentBottom()-contentTop()-20-scrollThumbHeight());
            scrollTo((int)Math.round((event.y()-scrollGrab-contentTop()-10)*maximumScroll()/travel));return true;
        }
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && draggingScroll) {
            draggingScroll=false;return true;
        }
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        boolean overTooltip=tooltipBounds!=null && x>=tooltipBounds.x()-4 && x<tooltipBounds.x()+tooltipBounds.width()+4
                && y>=tooltipBounds.y()-4 && y<tooltipBounds.y()+tooltipBounds.height()+4;
        boolean overSource=x>=7 && x<pane-13 && hits.stream().anyMatch(hit->hit.id().equals(hover) && y>=hit.y() && y<hit.y()+hit.height());
        if(tooltipBounds!=null && tooltipBounds.maxScroll()>0 && !hover.isEmpty() && (overTooltip || overSource)) {
            tooltipScroll=Math.clamp(tooltipScroll-(int)(vertical*(font.lineHeight+1)*3),0,tooltipBounds.maxScroll());return true;
        }
        if(x>=0 && x<pane && y>=contentTop() && y<contentBottom()) {
            scroll=Math.clamp(scroll-(int)(vertical*28),0,Math.max(0,total-contentBottom()+contentTop()));rebuildWidgets();return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void onClose() { DialogCoordinator.setContent(null); }
    @Override public void removed() { draggingScroll=false;clearMap();panels.close();super.removed(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public int unitHudLeft() { return pane; }
    @Override public boolean allowsMovement() { return true; }
}
