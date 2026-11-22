package dev.hoi.client.screen.focus;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.focus.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class FocusScreen extends Screen {
    private static final int NODE_W=84,NODE_H=80,X_PITCH=48,Y_PITCH=84;
    private final String token=UUID.randomUUID().toString();
    private final Consumer<FocusPayloads.Request> sender;
    private final boolean fixture;
    private final String target;
    private final Screen parent;
    private final FocusDetail detailPanel=new FocusDetail();
    private FocusPayloads.View view;
    private FocusPayloads.Detail detail;
    private final Map<String,FocusPayloads.Detail> hoverDetails=new HashMap<>();
    private String hoverTarget="",hoverQuery="";
    private EditBox search;
    private UiMenuButton searchClear;
    private Button detailClose,detailStart;
    private String query="",selected="",completionTarget="";
    private float zoom=1,panX,panY;
    private boolean centered,panning,draggingDetail,detailDirty,extended;
    private int top,ticks=999,pendingTicks;
    private FocusPayloads.Request pending,queryRequest,recoveryRequest;
    public FocusScreen(){this(null,ClientPlayNetworking::send,false,"",null);}
    public FocusScreen(Screen parent,String target){this(null,ClientPlayNetworking::send,false,target.equals(ClientCampaignHud.country())?"":target,parent);}
    public FocusScreen(String completedFocus){this();completionTarget=completedFocus;}
    public FocusScreen(FocusPayloads.View view,Consumer<FocusPayloads.Request> sender){this(view,sender,true,"",null);}
    FocusScreen(FocusPayloads.View view,Consumer<FocusPayloads.Request> sender,String target,Screen parent){this(view,sender,true,target,parent);}
    private FocusScreen(FocusPayloads.View view,Consumer<FocusPayloads.Request> sender,boolean fixture,String target,Screen parent){super(Component.literal("중점 계통도"));this.view=view;this.sender=sender;this.fixture=fixture;this.target=target;this.parent=parent;extended=view!=null && (view.ack()!=null || view.detail()!=null);}
    public String token(){return token;}
    public String target(){return target;}
    public static void resourcesReloaded(){FocusInlays.clear();}
    String searchText(){return query;}
    float zoomLevel(){return zoom;}
    float panX(){return panX;}
    float panY(){return panY;}
    String selectedId(){return selected;}
    boolean pendingAction(){return pending!=null;}
    String detailId(){return detail==null?"":detail.id();}
    int detailScroll(){return detailPanel.scroll();}
    int[] detailBounds(){return selected.isEmpty()?null:detailPanel.bounds().array();}
    int[] requirementBounds(){return selected.isEmpty()?null:detailPanel.requirements().array();}
    String sourceDetailText(){if(detail==null)return "";return detail.description()+"\n"+String.join("\n",detail.requirements().stream().map(FocusPayloads.DetailLine::text).toList())+"\n"+String.join("\n",detail.rewards().stream().map(FocusPayloads.DetailLine::text).toList());}
    int[] nodeBounds(String id){var n=view==null?null:view.nodes().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);return n==null?null:new int[]{(int)(x(n)-NODE_W*zoom/2),(int)y(n),(int)(NODE_W*zoom),(int)(NODE_H*zoom)};}
    public void update(FocusPayloads.Response packet) {
        if(!packet.token().equals(token))return;
        if(packet.json().isEmpty()){minecraft.gui.setScreen(null);return;}
        var next=packet.view();
        if(view!=null && !view.country().equals(next.country()) || !target.isEmpty() && !next.country().equals(target)){minecraft.gui.setScreen(null);return;}
        boolean acknowledged=matches(pending,next.ack()),recovered=matches(recoveryRequest,next.ack());
        if(view!=null && view.session().equals(next.session()) && next.revision()<view.revision()) {
            if(acknowledged){pending=null;recoveryRequest=null;rebuildWidgets();}
            return;
        }
        boolean newSession=view!=null && !view.session().equals(next.session());
        if(newSession && extended && !acknowledged && !matches(queryRequest,next.ack()))return;
        boolean treeChanged=view==null || !view.tree().equals(next.tree());
        if(treeChanged){centered=false;clearSelection();detailPanel.resetTree();pending=null;recoveryRequest=null;}
        if(acknowledged || recovered || pending!=null && !extended && (newSession || next.revision()>pending.revision())){pending=null;recoveryRequest=null;}
        if(newSession){queryRequest=null;recoveryRequest=null;}
        extended|=next.ack()!=null || next.detail()!=null;
        if(view==null || newSession || next.revision()!=view.revision()){hoverDetails.clear();hoverQuery="";}
        if(next.detail()!=null){hoverDetails.put(next.detail().id(),next.detail());hoverQuery="";}
        view=next;
        if(!completionTarget.isEmpty()){
            selected=completionTarget;completionTarget="";detailDirty=true;
        }
        if(!selected.isEmpty() && selectedNode()==null){clearSelection();pending=null;recoveryRequest=null;}
        if(pending!=null && !pending.id().isEmpty() && next.nodes().stream().noneMatch(n -> n.id().equals(pending.id()))){pending=null;recoveryRequest=null;}
        if(!selected.isEmpty() && next.detail()!=null && next.detail().id().equals(selected)){detail=next.detail();detailDirty=false;}
        else if(selected.isEmpty())detail=null;
        rebuildWidgets();
    }
    private static boolean matches(FocusPayloads.Request request,FocusPayloads.Ack ack){return request!=null && ack!=null && request.action()==ack.action() && request.session().equals(ack.session()) && request.revision()==ack.revision() && request.id().equals(ack.id());}
    private void clearSelection(){selected="";detail=null;detailDirty=false;panning=false;draggingDetail=false;detailPanel.reset();}
    private void select(String id){if(!selected.equals(id)){selected=id;detail=hoverDetails.get(id);detailDirty=extended && !id.isEmpty() && detail==null;detailPanel.reset();}panning=false;rebuildWidgets();queryDetail();}
    private void queryDetail(){if((detailDirty || extended && !selected.isEmpty() && detail==null && ticks>=40) && pending==null && ticks>=20)request(FocusPayloads.Action.REFRESH,selected);}
    private void request(FocusPayloads.Action action,String id) {
        if(!fixture && !ClientPlayNetworking.canSend(FocusPayloads.Request.TYPE))return;
        boolean mutation=action==FocusPayloads.Action.START || action==FocusPayloads.Action.CANCEL;
        if(mutation && pending!=null)return;
        var packet=new FocusPayloads.Request(action,token,view==null?"":view.session(),view==null?0:view.revision(),id,target);
        if(mutation){pending=packet;pendingTicks=0;}
        else {queryRequest=packet;detailDirty=false;if(pending!=null && pendingTicks>=120)recoveryRequest=packet;}
        ticks=0;sender.accept(packet);
    }
    @Override public void tick(){ticks++;if(pending!=null)pendingTicks++;queryDetail();if(view!=null && pending==null && !detailDirty && !hoverTarget.isEmpty() && !hoverDetails.containsKey(hoverTarget) && ticks>=20 && (!hoverQuery.equals(hoverTarget)||ticks>=120)){hoverQuery=hoverTarget;request(FocusPayloads.Action.REFRESH,hoverTarget);}if((!fixture || pending!=null) && ticks>=120)request(view==null?FocusPayloads.Action.OPEN:FocusPayloads.Action.REFRESH,completionTarget.isEmpty()?selected:completionTarget);}
    @Override protected void rebuildWidgets() {
        var focused=getFocused();
        super.rebuildWidgets();
        if(focused!=null && children().contains(focused))setFocused(focused);
    }
    @Override protected void init() {
        detailClose=null;detailStart=null;
        top=HudMenuBar.height(width)+29;
        HudMenuBar.buttons(width,view==null?"":target.isEmpty()?view.country():ClientCampaignHud.country(),null,HoiClient::openMenu,HudMenuBar.height(width)).forEach(this::addRenderableWidget);
        float unit=HudMenuBar.windowTop(width)/78f,windowTop=HudMenuBar.windowTop(width),center=width-114*unit,middle=windowTop+28.5f*unit;
        int closeLeft=Math.round(width-48*unit),closeTop=Math.round(windowTop+13*unit);
        addRenderableWidget(new UiMenuButton("×",closeLeft,closeTop,Math.round(width-16*unit)-closeLeft,Math.round(windowTop+46*unit)-closeTop,this::onClose)
                .caption("").background("menu/close").art(width-48*unit,windowTop+13*unit,32*unit,33*unit).sound("ui.close"));
        int split=Math.round(center+17.5f*unit),artTop=Math.round(middle-17.5f*unit),artBottom=Math.round(middle+17.5f*unit),minusLeft=Math.round(center-21*unit);
        var zoomTip=Tooltips.createOfficer(UiCommandLabels.text("FOCUS_ZOOM_SLIDER_TOOLTIP","클릭하거나 §G마우스 휠§!을 사용하여 중점 계통도를 확대/축소"));
        var zoomIn=new UiMenuButton("+",split,artTop,Math.round(center+56*unit)-split,artBottom-artTop,() -> zoom(1.15,width/2.0,(height+top)/2.0)).caption("").background("none");
        var zoomOut=new UiMenuButton("−",minusLeft,artTop,split-minusLeft,artBottom-artTop,() -> zoom(1/1.15,width/2.0,(height+top)/2.0)).caption("").background("none");
        zoomIn.setTooltip(zoomTip);zoomOut.setTooltip(zoomTip);
        addRenderableWidget(zoomIn);addRenderableWidget(zoomOut);
        if(search==null) {
            search=new EditBox(font,0,0,1,font.lineHeight,Component.literal("중점 검색"));
            search.setMaxLength(128);search.setHint(Component.literal(UiCommandLabels.text("FOCUS_SEARCH_TYPE_TEXT","입력하여 검색")).withStyle(net.minecraft.ChatFormatting.WHITE));search.setValue(query);search.setResponder(s -> query=s);
        }
        search.setBordered(false);search.setPosition(Math.round(width-306*unit),Math.round(middle-font.lineHeight/2f));search.setWidth(Math.round(150*unit));addRenderableWidget(search);
        searchClear=new UiMenuButton("검색 지우기",Math.round(width-171*unit),Math.round(windowTop+20*unit),Math.round(18*unit),Math.round(18*unit),() -> search.setValue("")).caption("").background("focus/clear_text");
        searchClear.setTooltip(null);searchClear.visible=!query.isEmpty();addRenderableWidget(searchClear);
        if(view!=null && !centered)centerTree();
        if(!selected.isEmpty()) {
            var n=selectedNode();
            detailPanel.layout(width,height,top);
            detailClose=addRenderableWidget(detailPanel.button(true,() -> {clearSelection();rebuildWidgets();}));
            boolean cancel=n.status().equals("진행 중");
            detailStart=detailPanel.button(false,() -> {request(cancel?FocusPayloads.Action.CANCEL:FocusPayloads.Action.START,n.id());rebuildWidgets();});
            detailStart.setMessage(Component.literal(cancel?"중점 취소":"중점 시작"));
            detailStart.setTooltip(Tooltips.createOfficer(cancel?view.cancelTooltip():view.startTooltip()));
            detailStart.visible=target.isEmpty();
            detailStart.active=target.isEmpty() && pending==null && (cancel?view.cancelable():n.enabled() && (!extended || detail!=null && detail.executable()));
            addRenderableWidget(detailStart);
        }
    }
    private void centerTree() {
        if(view.nodes().isEmpty()){panX=width/2f;panY=top+8;centered=true;return;}
        double minX=view.nodes().stream().mapToDouble(n -> n.x()*X_PITCH).min().orElse(0),maxX=view.nodes().stream().mapToDouble(n -> n.x()*X_PITCH).max().orElse(0);
        double minY=view.nodes().stream().mapToDouble(n -> n.y()*Y_PITCH).min().orElse(0);
        zoom=1;
        panX=(float)(width/2.0-(minX+maxX)*zoom/2);panY=(float)(top+8-minY*zoom);centered=true;
    }
    private float x(FocusPayloads.Node n){return (float)(panX+n.x()*X_PITCH*zoom);}
    private float y(FocusPayloads.Node n){return (float)(panY+n.y()*Y_PITCH*zoom);}
    private FocusPayloads.Node at(double mx,double my) {
        if(view==null || my<top || my>=height-5)return null;
        for(var n:view.nodes())if(mx>=x(n)-NODE_W*zoom/2 && mx<x(n)+NODE_W*zoom/2 && my>=y(n) && my<y(n)+NODE_H*zoom)return n;return null;
    }
    private FocusPayloads.Node selectedNode(){return view==null?null:view.nodes().stream().filter(n -> n.id().equals(selected)).findFirst().orElse(null);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.nextStratum();
        HudMenuBar.drawBackground(g,width,view==null?CountryHud.UNKNOWN:view.hud(),0);
        g.nextStratum();
        UiAssets.tiledNineSlice(g,"research/source/gfx_tiled_plain_bg2/frame_0",0,HudMenuBar.height(width),width,height-HudMenuBar.height(width),64,.5);
        UiAssets.tiledNineSlice(g,"doctrine/gfx_tiled_bg",0,top-5,width,height-top+5,64,.5);
        UiAssets.tiledNineSlice(g,"commander/commands/tiled_window_1b_border/frame_0",0,top-5,width,height-top+5,64,.5);
        g.nextStratum();
        UiMenuStyle.title(g,font,UiMenuStyle.SCREEN,(view==null?"":view.name()+" ")+"중점 계통도",10,HudMenuBar.height(width)+11.5,Math.max(30,width-245),-1,UiMenuStyle.TEXT);
        drawHeaderArt(g);
        if(searchClear!=null)searchClear.visible=!query.isEmpty();
        g.enableScissor(4,top,width-4,height-5);
        if(view!=null) {
            FocusInlays.draw(g,view.inlays(),panX,panY,zoom);
            for(int i=0;i<view.nodes().size();i++) {
                var n=view.nodes().get(i);
                for(var group:n.parents())for(int parent:group)link(g,view.nodes().get(parent),n);
            }
            var exclusivePairs=new HashSet<Set<Integer>>();
            for(int i=0;i<view.nodes().size();i++)for(int other:view.nodes().get(i).exclusive()) {
                if(other==i||!exclusivePairs.add(Set.of(i,other)))continue;
                var a=view.nodes().get(i);var b=view.nodes().get(other);
                int left=(int)(Math.min(x(a),x(b))+42*zoom),right=(int)(Math.max(x(a),x(b))-42*zoom),linkY=(int)(Math.min(y(a),y(b))+58.5*zoom),size=Math.max(1,Math.round(16*zoom));
                if(right<=left||linkY+size<top||linkY>=height)continue;
                g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/mio/ui/gfx_focus_exclusive_line1/frame_0.png"),left+size/2,linkY+Math.round(5*zoom),right-size/2,linkY+Math.round(11*zoom),0,1,0,1);
                UiAssets.draw(g,"mio/ui/gfx_focus_link_exclusive/frame_1",left,linkY,size,size);
                UiAssets.draw(g,"mio/ui/gfx_focus_link_exclusive/frame_2",right-size,linkY,size,size);
                UiAssets.draw(g,"mio/ui/gfx_focus_link_exclusive/frame_0",(left+right-size)/2,linkY,size,size);
            }
            for(var n:view.nodes())drawNode(g,n);
            g.nextStratum();
            for(var n:view.nodes())drawLabel(g,n);
        } else g.centeredText(font,"중점 계통도 불러오는 중…",width/2,top+36,UiMenuStyle.TEXT);
        g.disableScissor();
        if(view!=null && view.nodes().isEmpty())g.centeredText(font,"표시할 중점이 없습니다.",width/2,top+36,UiMenuStyle.MUTED);
        boolean overDetail=!selected.isEmpty() && detailPanel.bounds().contains(mx,my);
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable widget && child!=detailClose && child!=detailStart)widget.extractRenderState(g,overDetail?-1:mx,overDetail?-1:my,delta);
        g.nextStratum();
        if(!selected.isEmpty()) {
            detailPanel.draw(g,font,selectedNode(),detail,extended && detail==null,mx,my);
            detailClose.extractRenderState(g,mx,my,delta);detailStart.extractRenderState(g,mx,my,delta);
        }
        hoverTarget="";
        if(!widgetAt(mx,my) && (selected.isEmpty() || !detailPanel.bounds().contains(mx,my))) {var node=at(mx,my);if(node!=null){hoverTarget=node.id();g.setTooltipForNextFrame(font,Tooltips.tooltipLines(font,hoverTooltip(node),width),mx,my);}}
        HudMenuBar.drawTooltip(g,width,view==null?CountryHud.UNKNOWN:view.hud(),my<HudMenuBar.height(width)?mx:-1,my,0);
    }
    String hoverTooltip(FocusPayloads.Node node) {
        var result=new StringBuilder("§Y").append(node.name()).append("§!");
        var source=hoverDetails.get(node.id());
        if(source!=null) {
            String status=node.status().equals("완료")?UiCommandLabels.text("FOCUS_STATUS_COMPLETED_TOOLTIP","§G완료됨§!"):node.remaining()>0?UiCommandLabels.text("FOCUS_STATUS_INPROGRESS_TOOLTIP","완료 시간: $DAYS|0H$일. ($LEFT|0H$일 남음)"):UiCommandLabels.text("FOCUS_STATUS_TOOLTIP","완료 시간: $DAYS|0H$일");
            result.append("\n\n").append(status.replace("$DAYS|0H$","§H"+FocusDetail.days(source.durationHours())+"§!").replace("$LEFT|0H$","§H"+FocusDetail.daysLeft(node.remaining())+"§!"));
            if(!source.requirements().isEmpty())result.append('\n');
            for(var row:source.requirements())result.append('\n').append(" ".repeat(row.depth()*2)).append(row.text());
            if(!source.rewards().isEmpty()) {
                result.append("\n\n").append(UiCommandLabels.text("FOCUS_REWARD_TOOLTIP","§H효과§!:"));
                for(var row:source.rewards())result.append('\n').append(" ".repeat(row.depth()*2)).append(row.text());
            }
        }
        result.append("\n§!").append(UiCommandLabels.text("SEPARATION_LINE","--------------\n").stripTrailing());
        result.append('\n').append(UiCommandLabels.text("CLICK_DETAILS_CLICK","§G클릭§!하여 세부 사항 보기"));
        if(!selected.isEmpty())result.append('\n').append(UiCommandLabels.text("CLICK_DETAILS_CTRL","§GCtrl+클릭§!하여 다른 창을 열어 놓은 채로 세부 사항 보기"));
        return result.toString();
    }
    private void link(GuiGraphicsExtractor g,FocusPayloads.Node parent,FocusPayloads.Node child) {
        int px=(int)x(parent),py=(int)(y(parent)+NODE_H*zoom-2*zoom),cx=(int)x(child),cy=(int)y(child),middle=(py+cy)/2;
        int color=parent.status().equals("완료")?0xFF92A360:0xFF45616D;
        g.fill(px,Math.min(py,middle),px+1,Math.max(py,middle)+1,color);g.fill(Math.min(px,cx),middle,Math.max(px,cx)+1,middle+1,color);g.fill(cx,Math.min(middle,cy),cx+1,Math.max(middle,cy)+1,color);
    }
    private void drawNode(GuiGraphicsExtractor g,FocusPayloads.Node n) {
        float left=x(n)-NODE_W*zoom/2,upper=y(n);
        if(left>width || left+NODE_W*zoom<0 || upper>height || upper+NODE_H*zoom<top)return;
        g.pose().pushMatrix();g.pose().translate(left,upper);g.pose().scale(zoom);
        String state=n.status().equals("완료")?"completed":n.enabled()||n.status().equals("진행 중")||n.status().equals("선택 가능")?"available":"unavailable";
        UiAssets.draw(g,"focus/"+state,0,54,NODE_W,25);
        String art=n.icon().matches("[A-Za-z0-9_]+")?"politics/art/"+n.icon().toLowerCase(Locale.ROOT):"";
        boolean drawn=!art.isEmpty() && UiAssets.draw(g,art,2,0,NODE_W-4,60);
        if(!drawn && !art.isEmpty() && !n.icon().startsWith("GFX_"))drawn=UiAssets.draw(g,"politics/art/gfx_"+n.icon().toLowerCase(Locale.ROOT),2,0,NODE_W-4,60);
        if(!drawn)UiAssets.draw(g,"politics/empty/focus",2,0,NODE_W-4,60);
        boolean match=query.isBlank() || n.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
        if(!match)g.fill(0,0,NODE_W,NODE_H,0xA0000000);
        g.pose().popMatrix();
    }
    private void drawLabel(GuiGraphicsExtractor g,FocusPayloads.Node n) {
        float left=x(n)-NODE_W*zoom/2,upper=y(n);
        if(left>width || left+NODE_W*zoom<0 || upper>height || upper+NODE_H*zoom<top)return;
        boolean match=query.isBlank() || n.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
        double guiScale=minecraft.getWindow().getGuiScale();
        float textScale=(float)(Math.max(1,Math.round(zoom*guiScale))/guiScale);
        var lines=Tooltips.split(font,Tooltips.officerText(n.name()),Math.max(1,(int)((NODE_W-8)*zoom/textScale)));
        int count=Math.min(2,lines.size());
        int baseline=(int)Math.round((upper+66.5f*zoom-count*font.lineHeight*textScale/2)*guiScale);
        for(int i=0;i<count;i++) {
            float tx=(float)(Math.round((x(n)-font.width(lines.get(i))*textScale/2)*guiScale)/guiScale);
            float ty=(float)((baseline+Math.round(i*font.lineHeight*textScale*guiScale))/guiScale);
            g.pose().pushMatrix();g.pose().translate(tx,ty);g.pose().scale(textScale);
            g.text(font,lines.get(i),0,0,match?0xFFF0ECDD:0xFF70706A,false);g.pose().popMatrix();
        }
        if(match && !query.isBlank()) {
            g.fill((int)left,(int)(upper+54*zoom),(int)(left+NODE_W*zoom),(int)(upper+54*zoom)+1,0xFFFFCC33);
            g.fill((int)left,(int)(upper+79*zoom)-1,(int)(left+NODE_W*zoom),(int)(upper+79*zoom),0xFFFFCC33);
        }
    }
    private void drawHeaderArt(GuiGraphicsExtractor g) {
        float unit=HudMenuBar.windowTop(width)/78f,windowTop=HudMenuBar.windowTop(width),center=width-114*unit,middle=windowTop+28.5f*unit;
        UiAssets.drawExact(g,"focus/find_editbox",width-346*unit,windowTop+9*unit,200*unit,39*unit);
        zoomHalf(g,"focus/zoom_in",center+14*unit,middle-17.5f*unit,unit,8,28,8,27,31,3);
        zoomHalf(g,"focus/zoom_out",center-21*unit,middle-17.5f*unit,unit,13,32,14,22,34,-3);
    }
    private static void zoomHalf(GuiGraphicsExtractor g,String art,float x,float y,float unit,int left,int right,int upper,int lower,int column,int shift) {
        if(UiAssets.imageSize(art)[0]!=42)return;
        var id=net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+art+".png");
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(unit);
        g.blit(id,0,0,42,35,0,1,0,1);
        g.blit(id,left,upper,right,lower,column/42f,(column+1)/42f,upper/35f,lower/35f);
        g.blit(id,left+shift,upper,right+shift,lower,left/42f,right/42f,upper/35f,lower/35f);
        g.pose().popMatrix();
    }
    private void zoom(double factor,double mx,double my) {
        float next=(float)Math.clamp(zoom*factor,.35,1.8);float ratio=next/zoom;panX=(float)(mx-(mx-panX)*ratio);panY=(float)(my-(my-panY)*ratio);zoom=next;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        panning=false;draggingDetail=false;detailPanel.release();
        if(!selected.isEmpty() && detailPanel.bounds().contains(event.x(),event.y())) {
            for(var button:List.of(detailClose,detailStart))if(Tooltips.contains(button,event.x(),event.y())){if(button.mouseClicked(event,twice) && children().contains(button))setFocused(button);return true;}
            if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && detailPanel.press(event.x(),event.y()))return true;
            draggingDetail=event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && detailPanel.header(event.x(),event.y());return true;
        }
        if(super.mouseClicked(event,twice))return true;
        if(widgetAt(event.x(),event.y()))return true;
        if(event.y()<top)return false;
        var n=at(event.x(),event.y());
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && n!=null){setFocused(null);select(n.id());return true;}
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT || event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE){panning=true;return true;}
        return super.mouseClicked(event,twice);
    }
    private boolean widgetAt(double x,double y){for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget && Tooltips.contains(widget,x,y))return true;return false;}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {if(!selected.isEmpty() && detailPanel.drag(event.y()))return true;if(draggingDetail){detailPanel.move(dx,dy,width,height,top);rebuildWidgets();return true;}if(panning){panX+=(float)dx;panY+=(float)dy;return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent event){panning=false;draggingDetail=false;detailPanel.release();return super.mouseReleased(event);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(!selected.isEmpty() && detailPanel.scroll(x,y,vertical))return true;if(y>=top){zoom(Math.pow(1.1,vertical),x,y);return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean keyPressed(KeyEvent event){if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && !selected.isEmpty()){clearSelection();rebuildWidgets();return true;}if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP || event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN){zoom(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP?1.15:1/1.15,width/2.0,(height+top)/2.0);return true;}if(!selected.isEmpty() && getFocused()!=search && (event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER)){if(detailStart.active)detailStart.onPress(event);return true;}return super.keyPressed(event);}
    @Override public void onClose(){if(parent==null)super.onClose();else dev.hoi.client.screen.dialog.DialogCoordinator.setContent(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){detailPanel.release();detailPanel.closeResources();super.removed();}
}
