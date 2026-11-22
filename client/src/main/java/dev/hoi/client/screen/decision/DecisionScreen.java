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
    private static final UiMenuStyle.Window WINDOW=UiMenuStyle.window(MenuTab.DECISIONS);
    private UiMenuStyle.Frame frame;
    private int pane, scroll, total, ticks = 99, tooltipScroll;
    private long marqueeTicks;
    private long pendingRevision = -1;
    private boolean legacy;
    private boolean draggingScroll;
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
    int scrollOffset() { return Math.round(scroll*frame.scale()); }
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
        if(action==DecisionPayloads.Action.TAKE || action==DecisionPayloads.Action.PANEL_CLICK) {
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
        assets=loadAssets();text.clear();panels.clear();
        pane=Math.max(1,Math.min(275,width-16));frame=UiMenuStyle.frame(WINDOW,width,pane);
        var view=snapshot.view();
        HudMenuBar.buttons(width,view==null?"":view.country(),MenuTab.DECISIONS,tab -> {
            if(tab==MenuTab.RESEARCH)HoiClient.open();else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(UiMenuStyle.close(WINDOW,frame,this::onClose));
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
        var root=text.load(minecraft.getResourceManager());
        if(root==null)return Map.of();
        try {
            panels.assets(root);
            var colors=new HashMap<Integer,Integer>();
            if(root.get("highlightColors") instanceof com.google.gson.JsonArray palette)
                for(int i=0;i<Math.min(64,palette.size());i++)colors.put(i,(int)Long.parseLong(palette.get(i).getAsString(),16));
            highlightColors=Collections.unmodifiableMap(colors);
            return DecisionPanel.sprites(root);
        } catch(Exception invalid) {return Map.of();}
    }
    private static final int LIST_X=5,LIST_Y=45,LIST_WIDTH=544,GRID_X=14,SCROLL_X=526,EVENT_HEADER=65,EVENT_ROW=33,CATEGORY_HEADER=60,CATEGORY_END=20,DESC_WIDTH=510,TEXT_X=28,TEXT_WIDTH=470,PANEL_WIDTH=486,WHEEL=40;
    private int listBottom() { return (int)Math.ceil((height-frame.y())/frame.scale())+2; }
    int contentTop() { return frame.iy(LIST_Y); }
    int contentBottom() { return legacy?height-32:Math.min(height,frame.iy(listBottom())); }
    private int listLeft() { return frame.ix(LIST_X); }
    private int listRight() { return frame.ix(LIST_X+LIST_WIDTH); }
    private void line(GuiGraphicsExtractor g,Component value,double x,double center,double width,int color,boolean centered,boolean bold) {
        if(width<=0)return;
        int left=frame.ix(x),right=frame.ix(x+width),top=Math.round(frame.y(center)-font.lineHeight/2f);
        text.scrollingCell(g,font,bold?value.copy().withStyle(net.minecraft.ChatFormatting.BOLD):value,left,top,Math.max(1,right-left),font.lineHeight,color,centered,marqueeTicks*50);
    }
    private List<DecisionPayloads.Category> categories() {
        var groups=new LinkedHashMap<String,DecisionPayloads.Category>();
        for(var category:snapshot.categories())groups.put(category.id(),category);
        for(var row:snapshot.rows())groups.computeIfAbsent(row.category(),id ->
                new DecisionPayloads.Category(id,row.categoryName(),"",row.categoryIcon(),"","",false));
        return List.copyOf(groups.values());
    }
    private void layout(GuiGraphicsExtractor g,boolean widgets) {
        if(widgets)hits.clear();
        var groups=new LinkedHashMap<String,List<DecisionPayloads.Row>>();
        for(var row:snapshot.rows())groups.computeIfAbsent(row.category(),k -> new ArrayList<>()).add(row);
        int y=LIST_Y-scroll;
        if(g!=null) {
            frame.art(g,"decision/ui/event_header",GRID_X,y);
            UiMenuStyle.title(g,font,frame,"사건",34,y+40,78,-1,UiMenuStyle.TEXT);
        }
        popup(g,widgets,"minor","중요하지 않은 알림",116,y+10,104);
        popup(g,widgets,"events","사건 팝업",256,y+10,110);
        popup(g,widgets,"news","뉴스 팝업",256,y+38,110);
        y+=EVENT_HEADER;
        for(var event:eventRows) {
            if(g!=null) {
                frame.art(g,"decision/ui/event_row",GRID_X-1,y);
                frame.art(g,"decision/ui/event_type/frame_1",GRID_X+15,y+3);
                String remaining=event.remainingDays()==null?"":event.remainingDays()+"일";
                double timer=remaining.isEmpty()?0:font.width(remaining)/frame.scale()+8;
                line(g,text.component(event.title()),GRID_X+50,y+16,450-timer,UiMenuStyle.TEXT,false,false);
                if(!remaining.isEmpty())text.draw(g,font,Component.literal(remaining).getVisualOrderText(),frame.ix(GRID_X+500)-font.width(remaining),Math.round(UiText.centeredY(font,remaining,frame.y(y+16)-font.lineHeight/2f,font.lineHeight,1)),UiMenuStyle.TEXT,false);
            }
            if(widgets && visible(frame.iy(y),frame.len(EVENT_ROW)))control(event.title(),frame.ix(GRID_X-1),frame.iy(y),frame.len(512),frame.len(EVENT_ROW),true,"ui.click",() -> {
                var stack=dialogs();if(stack!=null)stack.reopen(event.token());
            });
            y+=EVENT_ROW;
        }
        var view=snapshot.view();
        String notice=view==null || snapshot.loading()?"서버 정보 불러오는 중…":"";
        if(!notice.isEmpty()) {
            var lines=text.lines(font,notice,frame.len(TEXT_WIDTH));
            for(var line:lines) {
                if(g!=null)text.draw(g,font,line,frame.ix(TEXT_X),frame.iy(y+4),UiMenuStyle.MUTED,false);
                y+=Math.round((font.lineHeight+1)/frame.scale());
            }
            y+=8;
        }
        for(var category:categories()) {
            String id=category.id(),name=category.name();
            var rows=groups.getOrDefault(id,List.of());
            if(g!=null) {
                frame.art(g,"decision/ui/category",GRID_X-4,y+8);
                String icon=asset(category.icon(),"icon");
                int[] iconSize=UiAssets.imageSize(icon);
                if(iconSize[0]>0)frame.art(g,icon,GRID_X+10,y+16,Math.min(iconSize[0],51),Math.min(iconSize[1],40));
                line(g,text.component(name),GRID_X+74,y+28.5,365,UiMenuStyle.TEXT,true,true);
                frame.art(g,collapsed.contains(id)?"recruitment/expand":"recruitment/collapse",GRID_X+470,y+23);
                if(!rows.isEmpty())frame.art(g,"decision/ui/track_category/frame_"+categoryTracking(rows),GRID_X+435,y+24);
            }
            if(widgets && visible(frame.iy(y+8),frame.len(53))) {
                Runnable fold=() -> {if(!collapsed.add(id))collapsed.remove(id);rebuildWidgets();};
                control(name,frame.ix(GRID_X-4),frame.iy(y+8),frame.len(GRID_X+435-(GRID_X-4)),frame.len(53),true,"ui.decision.tab",fold);
                control("분류 접기: "+name,frame.ix(GRID_X+470),frame.iy(y+23),frame.len(24),frame.len(25),true,"ui.decision.tab",fold);
                hits.add(new DecisionHit("category/"+id,"§Y"+text.component(name).getString()+"§!",frame.ix(GRID_X-4),frame.iy(y+8),frame.len(516),frame.len(53)));
                if(!rows.isEmpty()) {
                    control("분류 추적: "+text.component(name).getString(),frame.ix(GRID_X+435),frame.iy(y+24),frame.len(23),frame.len(23),true,"ui.decision.tab",()->{
                        boolean enabled=categoryTracking(rows)!=1;
                        preferences.tracking(server(),viewCountry(),rows.stream().map(DecisionPayloads.Row::id).toList(),enabled);
                        rebuildWidgets();
                    });
                    hits.add(new DecisionHit("track-category/"+id,dev.hoi.client.ui.UiCommandLabels.text("CLICK_TO_TOGGLE_ALL_CATEGORY_DECISION_TRACKING",""),frame.ix(GRID_X+435),frame.iy(y+24),frame.len(23),frame.len(23)));
                }
            }
            y+=CATEGORY_HEADER;
            if(!collapsed.contains(id)) {
                y=body(g,category,y,widgets);
                for(var row:rows) {
                    int rowHeight=rowHeight(row);
                    if(g!=null)row(g,row,y);
                    int top=frame.iy(y),height=frame.len(rowHeight);
                    if(widgets && visible(top,height)) {
                        boolean active=row.enabled() && !pending() && !snapshot.loading();
                        hits.add(new DecisionHit(row.id(),tooltip(row),frame.ix(GRID_X),top,frame.len(512),height));
                        control(rowName(row),frame.ix(GRID_X),top,frame.len(row.remainingHours()>0?512:437),height,active,"ui.decision.take",
                                () -> {request(DecisionPayloads.Action.TAKE,row.id(),legacy?legacyPage:0);rebuildWidgets();});
                        if(row.remainingHours()<=0) {
                            int selectY=row.target()==null?6:4;
                            control("결정 실행: "+text.component(rowName(row)).getString(),frame.ix(GRID_X+465),frame.iy(y+selectY),frame.len(40),frame.len(28),active,"ui.decision.take",
                                    () -> {request(DecisionPayloads.Action.TAKE,row.id(),legacy?legacyPage:0);rebuildWidgets();});
                            control("추적: "+text.component(rowName(row)).getString(),frame.ix(GRID_X+437),frame.iy(y+7),frame.len(23),frame.len(23),true,"ui.decision.tab",()->{
                                preferences.tracking(server(),viewCountry(),row.id(),!tracking(row));rebuildWidgets();
                            });
                            hits.add(new DecisionHit("track/"+row.id(),dev.hoi.client.ui.UiCommandLabels.text(tracking(row)?"DECISION_KEEP_NOTIFIED":"DECISION_NOT_KEEP_NOTIFIED",""),frame.ix(GRID_X+437),frame.iy(y+7),frame.len(23),frame.len(23)));
                        }
                    }
                    y+=rowHeight;
                }
            }
            y+=CATEGORY_END;
        }
        total=y-(LIST_Y-scroll);
    }
    private static boolean timed(DecisionPayloads.Row row) { return row.remainingHours()>0 && row.target()==null; }
    private String server() { return fixture?"fixture":DecisionPreferences.server(); }
    private String viewCountry() { var view=snapshot.view();return view==null?"":view.country(); }
    private boolean tracking(DecisionPayloads.Row row) { return preferences.tracking(server(),viewCountry(),row.id()); }
    private int categoryTracking(List<DecisionPayloads.Row> rows) {
        long tracked=rows.stream().filter(this::tracking).count();return tracked==rows.size()?1:tracked==0?0:2;
    }
    private void popup(GuiGraphicsExtractor g,boolean widgets,String id,String label,int x,int y,int labelWidth) {
        int box=y+2;
        if(g!=null) {
            frame.art(g,"decision/ui/checkbox/frame_"+(preferences.popup(id)?1:0),x,box);
            line(g,text.component(label),x+36,box+14.5,labelWidth,UiMenuStyle.TEXT,false,false);
        }
        int left=frame.ix(x),top=frame.iy(box),width=frame.ix(x+36+labelWidth)-left,height=frame.len(30);
        if(widgets && visible(top,height)) {
            control(label,left,top,width,height,true,"ui.decision.tab",()->{preferences.popup(id,!preferences.popup(id));rebuildWidgets();});
            String tooltip=switch(id) {
                case "minor" -> "에이스 조종사 사건 같이 작은 사건 알림을 전환하려면 §G클릭§!하십시오.";
                case "news" -> "뉴스 알림을 전환하려면 §G클릭§!하십시오";
                default -> "사건 알림을 전환하려면 §G클릭§!하십시오";
            };
            hits.add(new DecisionHit("popup/"+id,tooltip,left,top,width,height));
        }
    }
    private boolean visible(int y,int height) { return y>=contentTop() && y+height<=contentBottom(); }
    private int body(GuiGraphicsExtractor g,DecisionPayloads.Category category,int y,boolean widgets) {
        int start=y;
        float scale=frame.scale();
        if(g!=null) {
            int bodyHeight=body(null,category,y,false)-y;
            if(bodyHeight>0)frame.tile(g,assets.getOrDefault("GFX_tiled_decisions_bg_small","doctrine/gfx_tiled_decisions_bg_small"),GRID_X,y-1,DESC_WIDTH,bodyHeight,16,48);
        }
        int lineHeight=Math.round((font.lineHeight+2)/scale);
        String picture=asset(category.picture(),"picture");
        int[] pictureSize=UiAssets.imageSize(picture);
        if(pictureSize[0]>0 && pictureSize[1]>0) {
            int pictureWidth=Math.min(TEXT_WIDTH,pictureSize[0]),pictureHeight=Math.min(DESC_WIDTH,Math.round(pictureSize[1]*pictureWidth/(float)pictureSize[0]));
            if(g!=null)frame.art(g,picture,TEXT_X+(TEXT_WIDTH-pictureWidth)/2.0,y+6,pictureWidth,pictureHeight);
            y+=pictureHeight+14;
        }
        if(!category.description().isEmpty()) {
            if(y==start)y+=6;
            int bodyWidth=frame.len(TEXT_WIDTH);
            var rows=text.sourceRows(category.description());
            for(int index=0;index<rows.size();index++) {
                var line=rows.get(index);
                if(line.getString().isBlank() && (index==0 || !rows.get(index-1).getString().isBlank())) {
                    int image=index;
                    while(image<rows.size() && rows.get(image).getString().isBlank())image++;
                    String path=image<rows.size()?bodyImage(rows.get(image)):"";
                    int[] size=UiAssets.imageSize(path);
                    int imageWidth=Math.min(TEXT_WIDTH,size[0]),imageHeight=size[0]>0?Math.round(size[1]*imageWidth/(float)size[0]):0;
                    int after=image+1;
                    while(after<rows.size() && rows.get(after).getString().isBlank())after++;
                    if(image-index>=3 && after-image-1>=3 && imageHeight>lineHeight*2) {
                        y+=10;
                        if(g!=null)frame.art(g,path,TEXT_X+(TEXT_WIDTH-imageWidth)/2.0,y,imageWidth,imageHeight);
                        y+=imageHeight+10;
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
                int[] size=UiAssets.imageSize(path);
                int imageWidth=Math.min(TEXT_WIDTH,size[0]),imageHeight=size[0]>0?Math.min(DESC_WIDTH,Math.round(size[1]*imageWidth/(float)size[0])):0;
                if(imageHeight>lineHeight*2) {
                    if(g!=null)frame.art(g,path,TEXT_X+(TEXT_WIDTH-imageWidth)/2.0,y,imageWidth,imageHeight);
                    y+=imageHeight+6;
                } else for(var part:font.split(TooltipKeywordIcons.decorateLines(line,false),bodyWidth).isEmpty()?List.of(net.minecraft.util.FormattedCharSequence.EMPTY):font.split(TooltipKeywordIcons.decorateLines(line,false),bodyWidth)) {
                    if(g!=null)text.draw(g,font,part,frame.ix(TEXT_X),frame.iy(y),UiMenuStyle.TEXT,false);
                    y+=lineHeight;
                }
            }
        }
        if(category.panel()!=null) {
            int reserved=0;
            if(category.description().endsWith("\n") && category.panel().elements().stream().anyMatch(element->negativePanel(element))) {
                var rows=text.rows(category.description());
                for(int index=rows.size()-1;index>=0 && rows.get(index).getString().isBlank();index--)reserved+=lineHeight;
                if(reserved<3*lineHeight)reserved=0;
            }
            y-=reserved;
            var result=panels.render(g,font,category.panel(),assets,text,frame.ix(TEXT_X),frame.iy(y),frame.len(PANEL_WIDTH));
            if(widgets)for(var hit:result.hits()) {
                String tooltip=hit.immediate()+hit.delayed();
                if(!tooltip.isEmpty())hits.add(new DecisionHit("panel/"+category.id()+"/"+hit.id(),tooltip,hit.x(),hit.y(),hit.width(),hit.height()));
            }
            if(widgets)for(var hit:result.hits().reversed()) {
                if(!hit.button().isEmpty()) {
                    String click=category.panel().scriptedGui()+"/"+hit.button();
                    control(hit.button(),hit.x(),hit.y(),hit.width(),hit.height(),hit.enabled() && !pending() && !snapshot.loading(),"ui.click",
                            () -> {request(DecisionPayloads.Action.PANEL_CLICK,click,0);rebuildWidgets();});
                }
            }
            y+=Math.round(result.height()/scale);
        }
        return y==start?y:y+10;
    }
    private static boolean negativePanel(DecisionPanelView.Element element) {
        return element.layout().y()!=null && element.layout().y()<0 || element.children().stream().anyMatch(DecisionScreen::negativePanel);
    }
    private String bodyImage(Component line) {
        var matcher=BODY_IMAGE.matcher(line.getString());
        return matcher.find() && line.getString().replace(matcher.group(),"").isBlank()
                ?assets.getOrDefault(matcher.group(1),assets.getOrDefault("GFX_"+matcher.group(1),"")):"";
    }
    private void row(GuiGraphicsExtractor g,DecisionPayloads.Row row,int y) {
        boolean targeted=row.target()!=null,timed=timed(row),counting=row.remainingHours()>0;
        int left=targeted?GRID_X-1:GRID_X,top=targeted?y-1:y;
        frame.art(g,"decision/ui/row/frame_"+(row.phase().equals("ACTIVE")?0:1),left,top);
        if(!row.progress().isEmpty() && row.totalHours()!=null && row.totalHours()>0) {
            String path="hoi:textures/gui/decision/ui/progress_"+row.progress().toLowerCase(Locale.ROOT)+"/texturefile1.png";
            double progress=Math.clamp(1-row.remainingHours()/(double)row.totalHours(),0,1);
            int fill=(int)Math.round(502*progress);
            if(fill>0) {
                g.pose().pushMatrix();g.pose().translate(frame.x(GRID_X+(targeted?9:timed?2:5)),frame.y(y+28));g.pose().scale(frame.scale());
                g.blit(net.minecraft.resources.Identifier.parse(path),0,0,fill,10,0,(float)progress,0,1);
                g.pose().popMatrix();
            }
        }
        String image=asset(row.icon(),"icon");
        if(UiAssets.imageHeight(image,20)<=0)image=asset("GFX_decision_unknown","icon");
        int[] imageSize=UiAssets.imageSize(image);
        double imageRight=0;
        if(imageSize[0]>0 && imageSize[1]>0) {
            double x=targeted?GRID_X+15:GRID_X+(timed?32:30)-imageSize[0]/2.0,iy=targeted?y:y+(timed?20:19)-imageSize[1]/2.0;
            frame.art(g,image,x,iy);
            imageRight=x+imageSize[0];
        }
        double cost=GRID_X+(timed?455:432);
        String costText=rowCost(row);
        double costWidth=0;
        if(counting) {
            String days=(row.remainingHours()+23)/24+"일";
            double timerScale=targeted?.85:.95;
            int[] timer=UiAssets.imageSize("decision/timer");
            frame.art(g,"decision/timer",GRID_X+492-timer[0]*timerScale/2,y+15-timer[1]*timerScale/2,timer[0]*timerScale,timer[1]*timerScale);
            int right=frame.ix(GRID_X+475);
            var sequence=Component.literal(days).withStyle(style->style.withColor(Tooltips.GOLD)).getVisualOrderText();
            text.draw(g,font,sequence,right-font.width(days),Math.round(UiText.centeredY(font,sequence,frame.y(y+19.5)-font.lineHeight/2f,font.lineHeight,1)),0xFFFFFFFF,false);
            costWidth=timed?0:(font.width(days)/frame.scale()+10);
            cost=GRID_X+405-8;
        }
        if(!timed && !counting)costWidth=drawCost(g,row,costText,cost,y);
        double nameX=GRID_X+(targeted||timed?62:55);
        String flag=targeted&&row.target().kind().equals("country")?UiStoryAssets.flag(row.target().id(),row.target().flag()):"";
        int[] flagSize=flag.isEmpty()?new int[]{0,0}:UiAssets.contentSize(flag);
        if(flagSize[1]>0) {
            double flagHeight=font.lineHeight/frame.scale(),flagWidth=flagSize[0]*flagHeight/flagSize[1],flagX=Math.max(GRID_X+30,imageRight+2);
            UiAssets.drawContent(g,flag,frame.ix(flagX),frame.iy(y+16.5-flagHeight/2),Math.max(1,frame.len(flagWidth)),font.lineHeight);
            nameX=Math.max(nameX,flagX+flagWidth+6);
        }
        nameX=Math.max(nameX,imageRight+2);
        double nameRight=Math.min(nameX+(targeted?280:timed?300:330),(counting?GRID_X+475-(font.width((row.remainingHours()+23)/24+"일")/frame.scale()):cost-costWidth)-6);
        var nameRows=text.rows(rowName(row)).stream().map(line->TooltipKeywordIcons.decorateLines(line,false)).toList();
        int rowsHeight=nameRows.size()*(font.lineHeight+1)-1;
        int nameTop=Math.round(frame.y(y+16.5)-rowsHeight/2f);
        for(var line:nameRows) {
            line(g,line,nameX,(nameTop-frame.y())/frame.scale()+font.lineHeight/2f/frame.scale(),nameRight-nameX,0xFFFFFFFF,false,false);
            nameTop+=font.lineHeight+1;
        }
        if(!counting) {
            frame.art(g,"decision/ui/track_decision/frame_"+(tracking(row)?1:0),GRID_X+437,y+7);
            frame.art(g,"decision/ui/select/frame_"+(row.enabled() && !pending() && !snapshot.loading()?1:0),GRID_X+465,y+(targeted?4:6));
        }
    }
    private double drawCost(GuiGraphicsExtractor g,DecisionPayloads.Row row,String cost,double right,int y) {
        int costColor=Boolean.FALSE.equals(row.costMet())?Tooltips.BAD:Tooltips.GOLD;
        var costRows=text.rows(cost).stream().map(line->TooltipKeywordIcons.decorateLines(line,false)).toList();
        int widest=costRows.stream().mapToInt(font::width).max().orElse(0);
        double width=Math.min(320,widest/frame.scale());
        int edge=frame.ix(right),costTop=Math.round(frame.y(y+18)-(costRows.size()*(font.lineHeight+1)-1)/2f);
        g.enableScissor(edge-frame.len(320),frame.iy(y),edge,frame.iy(y+39));
        for(var line:costRows) {
            var coloredCost=Component.empty();
            line.visit((style,part)->{
                part.codePoints().forEach(character->coloredCost.append(Component.literal(new String(Character.toChars(character))).withStyle(Character.isDigit(character)||".,%+-".indexOf(character)>=0?style.withColor(costColor):style)));
                return Optional.empty();
            },net.minecraft.network.chat.Style.EMPTY);
            var sequence=coloredCost.getVisualOrderText();
            text.draw(g,font,sequence,edge-font.width(coloredCost),Math.round(UiText.centeredY(font,sequence,costTop,font.lineHeight+1,1)),0xFFFFFFFF,false);
            costTop+=font.lineHeight+1;
        }
        g.disableScissor();
        return width;
    }
    static String rowCost(DecisionPayloads.Row row) {
        String cost=row.remainingHours()>0?(row.remainingHours()+23)/24+"일":row.costText().isEmpty()?format(row.cost()):row.costText();
        return political(row)?"£pol_power "+cost:cost;
    }
    private static boolean political(DecisionPayloads.Row row) {
        return row.remainingHours()==0 && (row.costText().isEmpty() || row.costText().matches("[0-9]+(?:\\.[0-9]+)?"));
    }
    private static int rowHeight(DecisionPayloads.Row row) { return row.target()!=null?40:39; }
    private String tooltip(DecisionPayloads.Row row) { return tooltip(text,row); }
    static String tooltip(DecisionText text,DecisionPayloads.Row row) {
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
    private String asset(String name,String kind) { return asset(assets,name,kind); }
    static String asset(Map<String,String> assets,String name,String kind) {
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
        UiMenuStyle.window(g,WINDOW,frame,height);
        UiMenuStyle.title(g,font,WINDOW,frame,"사건과 결정");
        frame.tile(g,"decision/ui/list_frame",LIST_X,LIST_Y,LIST_WIDTH,listBottom()-LIST_Y,64,UiMenuStyle.TILE);
        g.enableScissor(listLeft(),contentTop(),listRight(),contentBottom());layout(g,false);g.disableScissor();
        scrollbar().draw(g,scroll);
        if(view!=null && categories().isEmpty())g.text(font,"선택 가능한 결정 없음",frame.ix(TEXT_X),contentTop()+48,UiMenuStyle.MUTED);
        super.extractRenderState(g,mx,my,delta);
        DecisionHit selected=null;
        if(mx>=listLeft() && mx<frame.ix(SCROLL_X) && my>=contentTop() && my<contentBottom())
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
    private int visibleList() { return Math.max(1,Math.round((contentBottom()-contentTop())/frame.scale())); }
    private int maximumScroll() { return Math.max(0,total-visibleList()); }
    dev.hoi.client.ui.UiScrollBar scrollbar() { return new dev.hoi.client.ui.UiScrollBar(frame.ix(SCROLL_X),contentTop(),contentBottom(),visibleList(),total); }
    private void scrollTo(int value) {
        scroll=Math.clamp(value,0,maximumScroll());rebuildWidgets();
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice) {
        var bar=scrollbar();
        if(bar.contains(event.x(),event.y())) {
            if(event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return true;
            if(bar.arrow(event.y()))scrollTo(bar.step(event.y(),scroll,WHEEL));
            else {draggingScroll=true;scrollTo(bar.offset(event.y()));}
            return true;
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy) {
        if(draggingScroll && event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            scrollTo(scrollbar().offset(event.y()));return true;
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
        boolean overSource=x>=listLeft() && x<frame.ix(SCROLL_X) && hits.stream().anyMatch(hit->hit.id().equals(hover) && y>=hit.y() && y<hit.y()+hit.height());
        if(tooltipBounds!=null && tooltipBounds.maxScroll()>0 && !hover.isEmpty() && (overTooltip || overSource)) {
            tooltipScroll=Math.clamp(tooltipScroll-(int)(vertical*(font.lineHeight+1)*3),0,tooltipBounds.maxScroll());return true;
        }
        if(x>=0 && x<listRight() && y>=contentTop() && y<contentBottom()) {
            scrollTo(scroll-(int)(vertical*WHEEL));return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void onClose() { DialogCoordinator.setContent(null); }
    @Override public void removed() { draggingScroll=false;clearMap();panels.close();super.removed(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public int unitHudLeft() { return pane; }
    @Override public boolean allowsMovement() { return true; }
}
