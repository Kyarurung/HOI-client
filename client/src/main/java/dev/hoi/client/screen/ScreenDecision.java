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
    final ScreenDecisionPreferences preferences;
    private final dev.hoi.client.map.MapDecisionOverlay mapOverlay=new dev.hoi.client.map.MapDecisionOverlay();
    private final Set<String> highlighted=new HashSet<>();
    private Map<Integer,Integer> highlightColors=Map.of();
    private String mapRow="",mapDimension="";
    private int highlightTick=-100,gotoTick=-100,mapTicks;
    public String mapDimension() {
        var authority=snapshot.authority();
        return !fixture && authority!=null && !authority.country().equals(dev.hoi.client.ClientCampaignHud.country())?"":mapDimension;
    }
    private final Set<String> collapsed = new HashSet<>();
    private final ScreenDecisionSnapshot snapshot;
    private final ScreenDecisionText text = new ScreenDecisionText();
    private final ScreenDecisionPanel panels = new ScreenDecisionPanel();
    private Map<String,String> assets = Map.of();
    private int pane, top, scroll, total, ticks = 99, tooltipScroll;
    private long pendingRevision = -1;
    private boolean legacy;
    private boolean draggingScroll;
    private int scrollGrab;
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
    private ScreenDialogStack eventStack;
    private List<ProtocolDialogView> eventRows = List.of();
    void eventStack(ScreenDialogStack stack) { eventStack = stack; if (width > 0) rebuildWidgets(); }
    private ScreenDialogStack dialogs() { return eventStack == null ? ScreenDialogCoordinator.eventStack() : eventStack; }
    int eventCount() { return eventRows.size(); }
    public ScreenDecision() { this(null,ClientPlayNetworking::send,false); }
    public ScreenDecision(ProtocolDecisionPayloads.View view,Consumer<ProtocolDecisionPayloads.Request> sender) { this(view,sender,true); }
    private ScreenDecision(ProtocolDecisionPayloads.View view,Consumer<ProtocolDecisionPayloads.Request> sender,boolean fixture) {
        super(Component.literal("사건과 결정"));snapshot=new ScreenDecisionSnapshot(view);this.sender=sender;this.fixture=fixture;if(view!=null)modern=true;
        try { preferences=fixture?new ScreenDecisionPreferences(null):ScreenDecisionPreferences.current(); }
        catch(java.io.IOException impossible) { throw new IllegalStateException(impossible); }
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
        if(packet.json().isEmpty()) {clearMap();ScreenDialogCoordinator.setContent(null);return;}
        ProtocolDecisionPayloads.View next;
        try {next=packet.view();} catch(IllegalArgumentException invalid) {return;}
        boolean nextModern=JsonParser.parseString(packet.json()).getAsJsonObject().has("pageCount");
        if(modern!=null && modern!=nextModern)return;
        modern=nextModern;legacy=!modern;
        var result=legacy?snapshot.acceptLegacy(next):snapshot.accept(next);
        if(result==ScreenDecisionSnapshot.Acceptance.REVOKED) {ScreenDialogCoordinator.setContent(null);return;}
        if(result==ScreenDecisionSnapshot.Acceptance.STAGED) {
            clearMap();
            request(ProtocolDecisionPayloads.Action.REFRESH,"",snapshot.nextPage());
            rebuildWidgets();
        } else if(result==ScreenDecisionSnapshot.Acceptance.APPLIED) {
            clearMap();
            draggingScroll=false;
            details.clear();detailRequests.clear();
            if(legacy || next.revision()>pendingRevision)pendingRevision=-1;
            if(legacy)legacyPage=next.page();
            text.clear();ticks=0;rebuildWidgets();
        }
    }
    public void update(ProtocolDecisionPayloads.Highlight packet) {
        var authority=snapshot.authority();
        if(authority==null || snapshot.loading() || pending() || !packet.token().equals(token)
                || !packet.session().equals(authority.session()) || packet.revision()!=authority.revision()
                || !highlighted.contains(packet.row()) || !mapDimension().equals(dimension()))return;
        var row=snapshot.rows().stream().filter(value->value.id().equals(packet.row())).findFirst().orElse(null);
        if(row!=null && !row.highlightStates().isEmpty())mapOverlay.receive(packet,mapDimension,mapColor(row));
    }
    private String dimension() { return minecraft.level==null?"":minecraft.level.dimension().identifier().toString(); }
    private int mapColor(ProtocolDecisionPayloads.Row row) { return row.highlightColor()==null?0xffffffff:highlightColors.getOrDefault(row.highlightColor(),0xffffffff); }
    private void clearMap() { mapOverlay.clear();highlighted.clear();mapRow="";mapDimension=""; }
    void mapTick() {
        mapTicks++;
        String dimension=dimension();
        if(!mapDimension.equals(dimension)) { clearMap();mapDimension=dimension; }
        var row=snapshot.rows().stream().filter(value->value.id().equals(hover)).findFirst().orElse(null);
        String id=row==null || row.highlightStates().isEmpty() || snapshot.loading() || pending()?"":row.id();
        if(!id.equals(mapRow)) {mapRow=id;mapOverlay.select(id,dimension,row==null?0xffffffff:mapColor(row));}
        if(!id.isEmpty() && !dimension.isEmpty() && !highlighted.contains(id) && mapTicks-highlightTick>=4
                && (fixture || ClientPlayNetworking.canSend(ProtocolDecisionPayloads.Request.TYPE))) {
            highlighted.add(id);highlightTick=mapTicks;request(ProtocolDecisionPayloads.Action.HIGHLIGHT,id,0);
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
            clearMap();
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
        mapTick();
        if(snapshot.authority()!=null && !snapshot.authority().country().equals(dev.hoi.client.ClientCampaignHud.country())) {
            onClose();return;
        }
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
        eventRows = dialogs() == null ? List.of() : dialogs().events();
        pane=UiPanelLayout.width(ProtocolMenuTab.DECISIONS,width);top=UiMenuBar.height(width);
        assets=loadAssets();text.clear();panels.clear();
        pane=preferredWidth();
        var view=snapshot.view();
        UiMenuBar.buttons(width,view==null?"":view.country(),ProtocolMenuTab.DECISIONS,tab -> {
            if(tab==ProtocolMenuTab.RESEARCH)HoiClient.open();else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new UiMenuButton("×",pane-25,top+3,19,19,this::onClose));
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
    private int preferredWidth() {
        int required=pane;
        for(var category:categories())required=Math.max(required,font.width(text.component(category.name()))+76);
        for(var row:snapshot.rows()) {
            int nameWidth=text.rows(rowLabel(row)).stream().mapToInt(font::width).max().orElse(0);
            int costWidth=text.rows(rowCost(row)).stream().mapToInt(font::width).max().orElse(0);
            required=Math.max(required,nameWidth+costWidth+110+(political(row)?14:0)+(row.highlightStates().isEmpty()?0:14)
                    +(row.target()!=null && row.target().kind().equals("country")?24:0)+outlineWidth(row));
        }
        return Math.max(1,Math.min(width-16,required));
    }
    private List<ProtocolDecisionPayloads.Category> categories() {
        var groups=new LinkedHashMap<String,ProtocolDecisionPayloads.Category>();
        for(var category:snapshot.categories())groups.put(category.id(),category);
        for(var row:snapshot.rows())groups.computeIfAbsent(row.category(),id ->
                new ProtocolDecisionPayloads.Category(id,row.categoryName(),"",row.categoryIcon(),"","",false));
        return List.copyOf(groups.values());
    }
    private void layout(GuiGraphicsExtractor g,boolean widgets) {
        int pane=this.pane-10;
        if(widgets)hits.clear();
        var groups=new LinkedHashMap<String,List<ProtocolDecisionPayloads.Row>>();
        for(var row:snapshot.rows())groups.computeIfAbsent(row.category(),k -> new ArrayList<>()).add(row);
        int y=contentTop()-scroll,header=Math.max(26,pane*52/550);
        {
            int eventHeader=36;
            if (g != null) {
                UiAssets.nineSlice(g, "decision/ui/event_header", 5, y, pane - 10, eventHeader, 5, 1);
                g.text(font, "사건", 16, y + 6, UiMenuStyle.TEXT,false);
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
                    g.text(font, UiText.ellipsize(font, event.title(), pane - textLeft - 80), textLeft, y + (eventHeight - font.lineHeight) / 2, UiMenuStyle.TEXT, false);
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
                if(g!=null)g.text(font,line,12,y,UiMenuStyle.MUTED,false);
                y+=font.lineHeight+1;
            }
            y+=4;
        }
        for(var category:categories()) {
            String id=category.id(),name=category.name();
            int categoryHeight=Math.max(header,text.lines(font,name,pane-header-43).size()*(font.lineHeight+1)+8);
            if(g!=null) {
                UiAssets.nineSlice(g,"decision/category",5,y,pane-10,categoryHeight,5,1);
                if(!UiAssets.draw(g,asset(category.icon(),"icon"),10,y+(categoryHeight-header+6)/2,header-6,header-6))
                    UiAssets.draw(g,"decision/category_generic",10,y+(categoryHeight-header+6)/2,header-6,header-6);
                label(g,name,header+8,y,pane-header-43,categoryHeight,UiMenuStyle.TEXT,0);
                UiAssets.draw(g,collapsed.contains(id)?"recruitment/expand":"recruitment/collapse",pane-27,y+(categoryHeight-12)/2,12,12);
                if(!groups.getOrDefault(id,List.of()).isEmpty())UiAssets.draw(g,"decision/ui/track_category/frame_"+categoryTracking(groups.get(id)),pane-42,y+(categoryHeight-10)/2,10,10);
            }
            if(widgets && visible(y,categoryHeight)) {
                control(name,6,y,pane-50,categoryHeight,true,"ui.decision.tab",() -> {if(!collapsed.add(id))collapsed.remove(id);rebuildWidgets();});
                control("분류 접기: "+name,pane-28,y,17,categoryHeight,true,"ui.decision.tab",() -> {if(!collapsed.add(id))collapsed.remove(id);rebuildWidgets();});
                hits.add(new ScreenDecisionHit("category/"+id,name+(category.description().isEmpty()?"":"\n"+category.description()),y,categoryHeight));
                var rows=groups.getOrDefault(id,List.of());
                if(!rows.isEmpty()) {
                    control("분류 추적: "+text.component(name).getString(),pane-43,y,13,categoryHeight,true,"ui.decision.tab",()->{
                        boolean enabled=categoryTracking(rows)!=1;
                        preferences.tracking(server(),viewCountry(),rows.stream().map(ProtocolDecisionPayloads.Row::id).toList(),enabled);
                        rebuildWidgets();
                    });
                    hits.add(new ScreenDecisionHit("track-category/"+id,"분류 추적 설정 (상단 경보는 미지원)",pane-43,y,13,categoryHeight));
                }
            }
            y+=categoryHeight+3;
            if(collapsed.contains(id))continue;
            y=body(g,category,y,widgets);
            for(var row:groups.getOrDefault(id,List.of())) {
                int rowHeight=rowHeight(row);
                if(g!=null)row(g,row,y,rowHeight);
                if(widgets && visible(y,rowHeight)) {
                    hits.add(new ScreenDecisionHit(row.id(),tooltip(row),y,rowHeight));
                    control(rowName(row),7,y,pane-52-(row.highlightStates().isEmpty()?0:14),rowHeight,row.enabled() && !pending() && !snapshot.loading(),"ui.decision.take",
                            () -> {request(ProtocolDecisionPayloads.Action.TAKE,row.id(),legacy?legacyPage:0);rebuildWidgets();});
                    control("결정 실행: "+text.component(rowName(row)).getString(),pane-30,y,20,rowHeight,row.enabled() && !pending() && !snapshot.loading(),"ui.decision.take",
                            () -> {request(ProtocolDecisionPayloads.Action.TAKE,row.id(),legacy?legacyPage:0);rebuildWidgets();});
                    control("추적: "+text.component(rowName(row)).getString(),pane-44,y,12,rowHeight,true,"ui.decision.tab",()->{
                        preferences.tracking(server(),viewCountry(),row.id(),!tracking(row));rebuildWidgets();
                    });
                    hits.add(new ScreenDecisionHit("track/"+row.id(),"결정 추적 설정 (상단 경보는 미지원)",pane-44,y,12,rowHeight));
                    if(!row.highlightStates().isEmpty()) {
                        control("지도에서 보기: "+text.component(rowName(row)).getString(),pane-59,y,13,rowHeight,!snapshot.loading() && !pending(),"ui.click",()->{
                            if(mapTicks-gotoTick<10)return;
                            gotoTick=mapTicks;request(ProtocolDecisionPayloads.Action.GOTO,row.id(),0);
                        });
                        hits.add(new ScreenDecisionHit(row.id(),tooltip(row),pane-59,y,13,rowHeight));
                    }
                }
                y+=rowHeight+3;
            }
            y+=7;
        }
        total=y-contentTop()+scroll;
    }
    private String server() { return fixture?"fixture":ScreenDecisionPreferences.server(); }
    private String viewCountry() { var view=snapshot.view();return view==null?"":view.country(); }
    private boolean tracking(ProtocolDecisionPayloads.Row row) { return preferences.tracking(server(),viewCountry(),row.id()); }
    private int categoryTracking(List<ProtocolDecisionPayloads.Row> rows) {
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
            hits.add(new ScreenDecisionHit("popup/"+id,tooltip,x,y,width,height));
        }
    }
    private boolean visible(int y,int height) { return y>=contentTop() && y+height<=contentBottom(); }
    private int body(GuiGraphicsExtractor g,ProtocolDecisionPayloads.Category category,int y,boolean widgets) {
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
            var rows=text.rows(category.description());
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
                }
                String path=bodyImage(line);
                int imageWidth=Math.min(bodyWidth,(int)Math.round(UiAssets.imageSize(path)[0]*pane/550.0));
                int imageHeight=UiAssets.imageHeight(path,imageWidth);
                if(imageHeight>lineHeight*2) {
                    imageHeight=Math.min(imageHeight,pane);
                    if(g!=null)UiAssets.draw(g,path,11+(bodyWidth-imageWidth)/2,y,imageWidth,imageHeight);
                    y+=imageHeight+3;
                } else for(var part:font.split(line,bodyWidth).isEmpty()?List.of(net.minecraft.util.FormattedCharSequence.EMPTY):font.split(line,bodyWidth)) {
                    if(g!=null)g.text(font,part,11,y,UiMenuStyle.TEXT,false);
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
                hits.add(new ScreenDecisionHit("panel/"+category.id()+"/"+hit.id(),tooltip,hit.x(),hit.y(),hit.width(),hit.height()));
            }
            y+=result.height();
        }
        return y==start?y:y+5;
    }
    private static boolean negativePanel(ProtocolDecisionPanel.Element element) {
        return element.layout().y()!=null && element.layout().y()<0 || element.children().stream().anyMatch(ScreenDecision::negativePanel);
    }
    private String bodyImage(Component line) {
        var matcher=BODY_IMAGE.matcher(line.getString());
        return matcher.find() && line.getString().replace(matcher.group(),"").isBlank()
                ?assets.getOrDefault(matcher.group(1),assets.getOrDefault("GFX_"+matcher.group(1),"")):"";
    }
    private void row(GuiGraphicsExtractor g,ProtocolDecisionPayloads.Row row,int y,int height) {
        int pane=this.pane-10;
        String background="decision/ui/row/frame_1";
        if(UiAssets.imageHeight(background,100)==0)background=row.enabled()?"decision/row":"decision/row_disabled";
        UiAssets.nineSlice(g,background,7,y,pane-14,height,4,1);
        if(!UiAssets.draw(g,asset(row.icon(),"icon"),10,y+3,20,height-6))
            UiAssets.draw(g,"menu/decisions",10,y+3,20,height-6);
        String cost=rowCost(row);
        int costWidth=costWidth(row);
        boolean political=political(row);
        int right=pane-49-(row.highlightStates().isEmpty()?0:14),costLeft=right-costWidth;
        if(political)UiAssets.draw(g,"hud/political_power",costLeft-14,y+5,12,height-10);
        label(g,cost,costLeft,y,costWidth,height,political?(Boolean.FALSE.equals(row.costMet())?0xFFFF3232:0xFFFFBD00):0xFFFFFFFF,1);
        int nameLeft=34;
        if(row.target()!=null && row.target().kind().equals("country")) {
            UiAssets.draw(g,UiStoryAssets.flag(row.target().id(),row.target().flag()),34,y+(height-14)/2,20,14);
            nameLeft+=24;
        }
        int nameWidth=Math.max(10,costLeft-nameLeft-4-(political?14:0)-outlineWidth(row));
        label(g,rowLabel(row),nameLeft,y,nameWidth,height,0xFFFFFFFF,-1);
        String outline=rowOutline(row);
        if(!outline.isEmpty()) {
            int labelWidth=text.lines(font,rowLabel(row),nameWidth).stream().mapToInt(font::width).max().orElse(0);
            g.pose().pushMatrix();g.pose().translate(nameLeft+labelWidth+2,y+(height-font.lineHeight*2)/2);
            g.pose().scale(2f);g.text(font,text.component(outline),0,0,0xFFFFFFFF,false);g.pose().popMatrix();
        }
        String action=row.remainingHours()>0?"decision/timer":"decision/select";
        int actionHeight=UiAssets.imageHeight(action,20);
        UiAssets.draw(g,action,pane-30,y+(height-actionHeight)/2,20,actionHeight);
        UiAssets.draw(g,"decision/ui/track_decision/frame_"+(tracking(row)?1:0),pane-44,y+(height-10)/2,10,10);
        if(!row.highlightStates().isEmpty())UiAssets.draw(g,"decision/reference/gfx_locator_item_icon",pane-59,y+(height-12)/2,12,12);
        if(!row.progress().isEmpty() && row.totalHours()!=null && row.totalHours()>0) {
            String path="decision/ui/progress_"+row.progress().toLowerCase(Locale.ROOT)+"/";
            double progress=Math.clamp(1-row.remainingHours()/(double)row.totalHours(),0,1);
            int left=10,barWidth=pane-24,barY=y+height-5;
            UiAssets.draw(g,path+"texturefile2",left,barY,barWidth,4);
            int fill=(int)Math.round(barWidth*progress);
            if(fill>0)g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+path+"texturefile1.png"),left,barY,left+fill,barY+4,0,(float)progress,0,1);
        }
    }
    private String rowOutline(ProtocolDecisionPayloads.Row row) {
        var matcher=BODY_IMAGE.matcher(rowName(row));
        String token="";
        while(matcher.find())if(matcher.group(1).endsWith("_texticon") && rowName(row).substring(matcher.end()).isBlank()
                && !text.component(matcher.group()).getString().contains("£"))token=matcher.group();
        return token;
    }
    private String rowLabel(ProtocolDecisionPayloads.Row row) {
        String outline=rowOutline(row);
        String name=rowName(row);
        return outline.isEmpty()?name:name.substring(0,name.lastIndexOf(outline)).stripTrailing();
    }
    private int outlineWidth(ProtocolDecisionPayloads.Row row) {
        String outline=rowOutline(row);
        return outline.isEmpty()?0:font.width(text.component(outline))*2+4;
    }
    private String rowCost(ProtocolDecisionPayloads.Row row) {
        return row.remainingHours()>0?(row.remainingHours()+23)/24+"일":row.costText().isEmpty()?format(row.cost()):row.costText();
    }
    private boolean political(ProtocolDecisionPayloads.Row row) {
        return row.remainingHours()==0 && (row.costText().isEmpty() || row.costText().matches("[0-9]+(?:\\.[0-9]+)?"));
    }
    private int costWidth(ProtocolDecisionPayloads.Row row) {
        return Math.min((pane-10)/2,Math.max(22,text.rows(rowCost(row)).stream().mapToInt(font::width).max().orElse(0)+6));
    }
    private int rowHeight(ProtocolDecisionPayloads.Row row) {
        int nameLeft=row.target()!=null && row.target().kind().equals("country")?58:34;
        int nameWidth=Math.max(10,pane-59-costWidth(row)-nameLeft-4-(political(row)?14:0)-outlineWidth(row)-(row.highlightStates().isEmpty()?0:14));
        int lines=Math.max(text.lines(font,rowLabel(row),nameWidth).size(),text.lines(font,rowCost(row),costWidth(row)).size());
        return Math.max(rowOutline(row).isEmpty()?20:font.lineHeight*2+4,lines*(font.lineHeight+1)+8);
    }
    private String tooltip(ProtocolDecisionPayloads.Row row) {
        String body=row.tooltip().isEmpty()?row.description():row.tooltip();
        if(!row.costText().isEmpty() && text.component(body).getString().equals(text.component(row.costText()).getString()))body=row.costText();
        var result=new StringBuilder(rowName(row));
        if(!body.isEmpty())result.append("\n").append(body);
        appendState(result,row,body);
        return result.toString();
    }
    private void appendState(StringBuilder result,ProtocolDecisionPayloads.Row row,String body) {
        if(row.costText().isEmpty() || row.costText().matches("[0-9]+(?:\\.[0-9]+)?"))
            result.append("\n정치력: ").append(row.costText().isEmpty()?format(row.cost()):row.costText());
        else if(!text.component(body).getString().equals(text.component(row.costText()).getString())
                && !text.component(body).getString().endsWith("\n"+text.component(row.costText()).getString()))result.append("\n").append(row.costText());
        if(!row.status().isEmpty())result.append("\n").append(row.status());
        if(row.remainingHours()>0)result.append("\n").append(row.phase().equals("COOLDOWN")?"재사용 대기: ":"남은 기간: ")
                .append((row.remainingHours()+23)/24).append("일");
    }
    static String rowName(ProtocolDecisionPayloads.Row row) {
        return row.target()==null?row.name():row.name()+" ("+row.target().name()+")";
    }
    private void label(GuiGraphicsExtractor g,String value,int x,int y,int width,int height,int color,int alignment) {
        var lines=text.lines(font,value,width);
        int lineHeight=font.lineHeight+1;
        int top=y+(height-lines.size()*lineHeight)/2;
        for(var line:lines) {
            int offset=alignment<0?0:alignment==0?(width-font.width(line))/2:width-font.width(line);
            g.text(font,line,x+offset,top,color,false);
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
        return mapped!=null?mapped:name.matches("[A-Za-z0-9_]+")?"decision/"+kind+"/"+name.toLowerCase(Locale.ROOT):"";
    }
    private static String format(Double value) { return value==null?"?":java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        var events = dialogs() == null ? List.<ProtocolDialogView>of() : dialogs().events();
        if (!events.equals(eventRows)) rebuildWidgets();
        var view=snapshot.view();
        UiMenuBar.draw(g,width,view==null?ProtocolCountryHud.UNKNOWN:view.hud(),mx,my,0);
        UiAssets.tiledNineSlice(g,"diplomacy/dialog/panel_dark",0,top,pane,height-top,64,.5);
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/decision/ui/window_header.png"),0,top,pane,top+21,248f/794,1,0,1);
        g.pose().pushMatrix();g.pose().translate(20,top+4);g.pose().scale(1.25f);
        g.text(font,"사건과 결정",0,0,UiMenuStyle.TEXT,false);g.pose().popMatrix();
        g.enableScissor(3,contentTop(),pane-13,contentBottom());layout(g,false);g.disableScissor();
        drawScrollbar(g);
        if(view!=null && categories().isEmpty())g.text(font,"선택 가능한 결정 없음",12,contentTop()+48,UiMenuStyle.MUTED);
        super.extractRenderState(g,mx,my,delta);
        ScreenDecisionHit selected=null;
        if(mx>=7 && mx<pane-13 && my>=contentTop() && my<contentBottom())
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
        if(row!=null && row.target()!=null)result.insert(detail.name().length()," ("+row.target().name()+")");
        if(row!=null) {result.append('\n');appendState(result,row,detail.description());}
        return result.toString();
    }
    private int maximumScroll() { return Math.max(0,total-contentBottom()+contentTop()); }
    private int scrollThumbHeight() {
        int track=Math.max(1,contentBottom()-contentTop()-20);
        return Math.min(track,Math.max(12,track*(contentBottom()-contentTop())/Math.max(1,total)));
    }
    private int scrollThumbTop() {
        int travel=Math.max(1,contentBottom()-contentTop()-20-scrollThumbHeight());
        return contentTop()+10+(int)Math.round(travel*scroll/(double)Math.max(1,maximumScroll()));
    }
    private void drawScrollbar(GuiGraphicsExtractor g) {
        String path="commander/commands/";
        int x=pane-11,top=contentTop(),height=contentBottom()-top;
        UiAssets.nineSlice(g,path+"scrollbar_vertical_bg/frame_0",x,top,9,height,3,.5);
        UiAssets.nineSlice(g,path+"scroll_track/frame_0",x+2,top+10,5,height-20,2,.5);
        UiAssets.draw(g,path+"scroll_up/frame_0",x,top,8,8);
        UiAssets.draw(g,path+"scroll_down/frame_0",x,contentBottom()-8,8,8);
        UiAssets.nineSlice(g,path+"scroll_drager/frame_0",x,scrollThumbTop(),8,scrollThumbHeight(),3,.5);
    }
    private void scrollTo(int value) {
        scroll=Math.clamp(value,0,maximumScroll());rebuildWidgets();
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice) {
        if(event.x()>=pane-12 && event.x()<pane && event.y()>=contentTop() && event.y()<contentBottom()) {
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
    @Override public void onClose() { ScreenDialogCoordinator.setContent(null); }
    @Override public void removed() { draggingScroll=false;clearMap();panels.close();super.removed(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public int unitHudLeft() { return pane; }
    @Override public boolean allowsMovement() { return true; }
}
