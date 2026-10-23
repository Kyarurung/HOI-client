package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
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

public final class ScreenFocus extends Screen {
    private static final int NODE_W=84,NODE_H=80,X_PITCH=48,Y_PITCH=84;
    private final String token=UUID.randomUUID().toString();
    private final Consumer<ProtocolFocusPayloads.Request> sender;
    private final boolean fixture;
    private final ScreenFocusDetail detailPanel=new ScreenFocusDetail();
    private ProtocolFocusPayloads.View view;
    private ProtocolFocusPayloads.Detail detail;
    private EditBox search;
    private Button detailClose,detailStart;
    private String query="",selected="",completionTarget="";
    private float zoom=1,panX,panY;
    private boolean centered,panning,draggingDetail,detailDirty,extended;
    private int top,ticks=999,pendingTicks;
    private ProtocolFocusPayloads.Request pending,queryRequest,recoveryRequest;
    public ScreenFocus(){this(null,ClientPlayNetworking::send,false);}
    public ScreenFocus(String completedFocus){this();completionTarget=completedFocus;}
    public ScreenFocus(ProtocolFocusPayloads.View view,Consumer<ProtocolFocusPayloads.Request> sender){this(view,sender,true);}
    private ScreenFocus(ProtocolFocusPayloads.View view,Consumer<ProtocolFocusPayloads.Request> sender,boolean fixture){super(Component.literal("중점 계통도"));this.view=view;this.sender=sender;this.fixture=fixture;extended=view!=null && (view.ack()!=null || view.detail()!=null);}
    public String token(){return token;}
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
    String sourceDetailText(){if(detail==null)return "";return detail.description()+"\n"+String.join("\n",detail.requirements().stream().map(ProtocolFocusPayloads.DetailLine::text).toList())+"\n"+String.join("\n",detail.rewards().stream().map(ProtocolFocusPayloads.DetailLine::text).toList());}
    int[] nodeBounds(String id){var n=view==null?null:view.nodes().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);return n==null?null:new int[]{(int)(x(n)-NODE_W*zoom/2),(int)y(n),(int)(NODE_W*zoom),(int)(NODE_H*zoom)};}
    public void update(ProtocolFocusPayloads.Response packet) {
        if(!packet.token().equals(token))return;
        if(packet.json().isEmpty()){minecraft.gui.setScreen(null);return;}
        var next=packet.view();
        if(view!=null && !view.country().equals(next.country())){minecraft.gui.setScreen(null);return;}
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
    private static boolean matches(ProtocolFocusPayloads.Request request,ProtocolFocusPayloads.Ack ack){return request!=null && ack!=null && request.action()==ack.action() && request.session().equals(ack.session()) && request.revision()==ack.revision() && request.id().equals(ack.id());}
    private void clearSelection(){selected="";detail=null;detailDirty=false;panning=false;draggingDetail=false;detailPanel.reset();}
    private void select(String id){if(!selected.equals(id)){selected=id;detail=null;detailDirty=extended && !id.isEmpty();detailPanel.reset();}panning=false;rebuildWidgets();queryDetail();}
    private void queryDetail(){if(detailDirty && pending==null && ticks>=20)request(ProtocolFocusPayloads.Action.REFRESH,selected);}
    private void request(ProtocolFocusPayloads.Action action,String id) {
        if(!fixture && !ClientPlayNetworking.canSend(ProtocolFocusPayloads.Request.TYPE))return;
        boolean mutation=action==ProtocolFocusPayloads.Action.START || action==ProtocolFocusPayloads.Action.CANCEL;
        if(mutation && pending!=null)return;
        var packet=new ProtocolFocusPayloads.Request(action,token,view==null?"":view.session(),view==null?0:view.revision(),id);
        if(mutation){pending=packet;pendingTicks=0;}
        else {queryRequest=packet;detailDirty=false;if(pending!=null && pendingTicks>=120)recoveryRequest=packet;}
        ticks=0;sender.accept(packet);
    }
    @Override public void tick(){ticks++;if(pending!=null)pendingTicks++;queryDetail();if((!fixture || pending!=null) && ticks>=120)request(view==null?ProtocolFocusPayloads.Action.OPEN:ProtocolFocusPayloads.Action.REFRESH,completionTarget.isEmpty()?selected:completionTarget);}
    @Override protected void rebuildWidgets() {
        var focused=getFocused();
        super.rebuildWidgets();
        if(focused!=null && children().contains(focused))setFocused(focused);
    }
    @Override protected void init() {
        detailClose=null;detailStart=null;
        top=UiMenuBar.height(width)+29;
        UiMenuBar.buttons(width,view==null?"":view.country(),null,HoiClient::openMenu).forEach(this::addRenderableWidget);
        addRenderableWidget(new UiMenuButton("×",width-25,top-26,21,21,() -> super.onClose()));
        addRenderableWidget(new UiMenuButton("+",width-51,top-25,21,20,() -> zoom(1.15,width/2.0,(height+top)/2.0)));
        addRenderableWidget(new UiMenuButton("−",width-74,top-25,21,20,() -> zoom(1/1.15,width/2.0,(height+top)/2.0)));
        int searchWidth=Math.min(140,Math.max(65,width/5));
        if(search==null) {
            search=new EditBox(font,0,0,searchWidth,18,Component.literal("중점 검색"));
            search.setMaxLength(128);search.setHint(Component.literal("입력하여 검색"));search.setValue(query);search.setResponder(s -> query=s);
        }
        search.setPosition(width-82-searchWidth,top-24);search.setWidth(searchWidth);addRenderableWidget(search);
        if(view!=null && !centered)centerTree();
        if(!selected.isEmpty()) {
            var n=selectedNode();
            detailPanel.layout(width,height,top);
            detailClose=addRenderableWidget(detailPanel.button(true,() -> {clearSelection();rebuildWidgets();}));
            detailStart=detailPanel.button(false,() -> {request(ProtocolFocusPayloads.Action.START,n.id());rebuildWidgets();});
            detailStart.active=n.enabled() && pending==null && (!extended || detail!=null && detail.executable());
            if(detail!=null && !detail.limitation().isEmpty())detailStart.setTooltip(UiTooltips.createOfficer(detail.limitation()));
            addRenderableWidget(detailStart);
        }
        if(view!=null && view.cancelable()) {var cancel=new UiMenuButton("중점 취소",8,height-28,85,21,() -> {request(ProtocolFocusPayloads.Action.CANCEL,selected);rebuildWidgets();});cancel.active=pending==null;addRenderableWidget(cancel);}
    }
    private void centerTree() {
        if(view.nodes().isEmpty()){panX=width/2f;panY=top+8;centered=true;return;}
        double minX=view.nodes().stream().mapToDouble(n -> n.x()*X_PITCH).min().orElse(0),maxX=view.nodes().stream().mapToDouble(n -> n.x()*X_PITCH).max().orElse(0);
        double minY=view.nodes().stream().mapToDouble(n -> n.y()*Y_PITCH).min().orElse(0);
        zoom=1;
        panX=(float)(width/2.0-(minX+maxX)*zoom/2);panY=(float)(top+8-minY*zoom);centered=true;
    }
    private float x(ProtocolFocusPayloads.Node n){return (float)(panX+n.x()*X_PITCH*zoom);}
    private float y(ProtocolFocusPayloads.Node n){return (float)(panY+n.y()*Y_PITCH*zoom);}
    private ProtocolFocusPayloads.Node at(double mx,double my) {
        if(view==null || my<top || my>=height-5)return null;
        for(var n:view.nodes())if(mx>=x(n)-NODE_W*zoom/2 && mx<x(n)+NODE_W*zoom/2 && my>=y(n) && my<y(n)+NODE_H*zoom)return n;return null;
    }
    private ProtocolFocusPayloads.Node selectedNode(){return view==null?null:view.nodes().stream().filter(n -> n.id().equals(selected)).findFirst().orElse(null);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xFF030804);UiMenuBar.draw(g,width,view==null?ProtocolCountryHud.UNKNOWN:view.hud(),mx,my,0);
        UiMenuStyle.panel(g,0,top-29,width,29);
        g.text(font,font.plainSubstrByWidth((view==null?"":view.name()+" ")+"중점 계통도",Math.max(30,width-245)),10,top-19,UiMenuStyle.TEXT);
        g.enableScissor(4,top,width-4,height-5);
        if(view!=null) {
            for(int i=0;i<view.nodes().size();i++) {
                var n=view.nodes().get(i);
                for(var group:n.parents())for(int parent:group)link(g,view.nodes().get(parent),n);
                for(int other:n.exclusive())if(other>i) {
                    var b=view.nodes().get(other);int left=(int)Math.min(x(n),x(b)),right=(int)Math.max(x(n),x(b)),lineY=(int)(y(n)+NODE_H*zoom-5*zoom);
                    if(lineY>=top && lineY<height){g.fill(left,lineY,right,lineY+1,0xFF486858);UiAssets.draw(g,"focus/exclusive",(left+right)/2-9,lineY-7,18,14);}
                }
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
        if(!widgetAt(mx,my) && (selected.isEmpty() || !detailPanel.bounds().contains(mx,my))) {var node=at(mx,my);if(node!=null)g.setTooltipForNextFrame(font,UiTooltips.sourceLines(font,node.name()+"\n"+node.description()+"\n"+node.status()+(node.remaining()>0?" · "+ScreenFocusDetail.days(node.remaining())+"일 남음":""),width),mx,my);}
        if(view!=null && !view.message().isEmpty())g.text(font,font.plainSubstrByWidth(view.message(),width-115),105,height-19,UiMenuStyle.TEXT);
    }
    private void link(GuiGraphicsExtractor g,ProtocolFocusPayloads.Node parent,ProtocolFocusPayloads.Node child) {
        int px=(int)x(parent),py=(int)(y(parent)+NODE_H*zoom-2*zoom),cx=(int)x(child),cy=(int)y(child),middle=(py+cy)/2;
        int color=parent.status().equals("완료")?0xFF92A360:0xFF45616D;
        g.fill(px,Math.min(py,middle),px+1,Math.max(py,middle)+1,color);g.fill(Math.min(px,cx),middle,Math.max(px,cx)+1,middle+1,color);g.fill(cx,Math.min(middle,cy),cx+1,Math.max(middle,cy)+1,color);
    }
    private void drawNode(GuiGraphicsExtractor g,ProtocolFocusPayloads.Node n) {
        float left=x(n)-NODE_W*zoom/2,upper=y(n);
        if(left>width || left+NODE_W*zoom<0 || upper>height || upper+NODE_H*zoom<top)return;
        g.pose().pushMatrix();g.pose().translate(left,upper);g.pose().scale(zoom);
        String art=n.icon().matches("[A-Za-z0-9_]+")?"politics/art/"+n.icon().toLowerCase(Locale.ROOT):"";
        boolean drawn=!art.isEmpty() && UiAssets.draw(g,art,2,0,NODE_W-4,60);
        if(!drawn && !art.isEmpty() && !n.icon().startsWith("GFX_"))drawn=UiAssets.draw(g,"politics/art/gfx_"+n.icon().toLowerCase(Locale.ROOT),2,0,NODE_W-4,60);
        if(!drawn)UiAssets.draw(g,"politics/empty/focus",2,0,NODE_W-4,60);
        String state=n.status().equals("완료")?"completed":n.enabled()||n.status().equals("진행 중")?"available":"unavailable";
        UiAssets.draw(g,"focus/"+state,0,54,NODE_W,25);
        boolean match=query.isBlank() || n.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
        if(!match)g.fill(0,0,NODE_W,NODE_H,0xA0000000);
        g.pose().popMatrix();
    }
    private void drawLabel(GuiGraphicsExtractor g,ProtocolFocusPayloads.Node n) {
        float left=x(n)-NODE_W*zoom/2,upper=y(n);
        if(left>width || left+NODE_W*zoom<0 || upper>height || upper+NODE_H*zoom<top)return;
        boolean match=query.isBlank() || n.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
        double guiScale=minecraft.getWindow().getGuiScale();
        float textScale=(float)(Math.max(1,Math.round(zoom*guiScale))/guiScale);
        var lines=UiTooltips.split(font,UiTooltips.officerText(n.name()),Math.max(1,(int)((NODE_W-8)*zoom/textScale)));
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
    private void zoom(double factor,double mx,double my) {
        float next=(float)Math.clamp(zoom*factor,.35,1.8);float ratio=next/zoom;panX=(float)(mx-(mx-panX)*ratio);panY=(float)(my-(my-panY)*ratio);zoom=next;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        panning=false;draggingDetail=false;detailPanel.release();
        if(!selected.isEmpty() && detailPanel.bounds().contains(event.x(),event.y())) {
            for(var button:List.of(detailClose,detailStart))if(UiTooltips.contains(button,event.x(),event.y())){if(button.mouseClicked(event,twice) && children().contains(button))setFocused(button);return true;}
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
    private boolean widgetAt(double x,double y){for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget && UiTooltips.contains(widget,x,y))return true;return false;}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {if(!selected.isEmpty() && detailPanel.drag(event.y()))return true;if(draggingDetail){detailPanel.move(dx,dy,width,height,top);rebuildWidgets();return true;}if(panning){panX+=(float)dx;panY+=(float)dy;return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent event){panning=false;draggingDetail=false;detailPanel.release();return super.mouseReleased(event);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(!selected.isEmpty() && detailPanel.scroll(x,y,vertical))return true;if(y>=top){zoom(Math.pow(1.1,vertical),x,y);return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean keyPressed(KeyEvent event){if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && !selected.isEmpty()){clearSelection();rebuildWidgets();return true;}if(!selected.isEmpty() && getFocused()!=search && (event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER)){if(detailStart.active)detailStart.onPress(event);return true;}return super.keyPressed(event);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){detailPanel.release();detailPanel.closeResources();super.removed();}
}
