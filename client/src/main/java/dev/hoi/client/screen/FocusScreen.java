package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class FocusScreen extends Screen {
    private static final int NODE_W=84,NODE_H=80,X_PITCH=48,Y_PITCH=84;
    private final String token=UUID.randomUUID().toString();
    private final Consumer<FocusProtocol.Request> sender;
    private final boolean fixture;
    private FocusProtocol.View view;
    private EditBox search;
    private String query="",selected="";
    private float zoom=1,panX,panY;
    private boolean centered,panning;
    private int top,waiting,ticks=999;
    public FocusScreen(){this(null,ClientPlayNetworking::send,false);}
    public FocusScreen(FocusProtocol.View view,Consumer<FocusProtocol.Request> sender){this(view,sender,true);}
    private FocusScreen(FocusProtocol.View view,Consumer<FocusProtocol.Request> sender,boolean fixture){super(Component.literal("중점 계통도"));this.view=view;this.sender=sender;this.fixture=fixture;}
    public String token(){return token;}
    String searchText(){return query;}
    float zoomLevel(){return zoom;}
    public void update(FocusProtocol.Response packet) {
        if(!packet.token().equals(token))return;
        if(packet.json().isEmpty()){minecraft.gui.setScreen(null);return;}
        var next=packet.view();
        if(view!=null && !view.country().equals(next.country())){minecraft.gui.setScreen(null);return;}
        if(view!=null && view.session().equals(next.session()) && next.revision()<view.revision())return;
        if(view==null || !view.tree().equals(next.tree())){centered=false;selected="";}
        view=next;waiting=0;rebuildWidgets();
    }
    private void request(FocusProtocol.Action action,String id) {
        if(!fixture && !ClientPlayNetworking.canSend(FocusProtocol.Request.TYPE))return;
        sender.accept(new FocusProtocol.Request(action,token,view==null?"":view.session(),view==null?0:view.revision(),id));ticks=0;
        if(action==FocusProtocol.Action.START || action==FocusProtocol.Action.CANCEL)waiting=100;
    }
    @Override public void tick(){if(waiting>0)waiting--;if(!fixture && ++ticks>=120 && waiting==0)request(view==null?FocusProtocol.Action.OPEN:FocusProtocol.Action.REFRESH,"");}
    @Override protected void rebuildWidgets() {
        var focused=getFocused();
        super.rebuildWidgets();
        if(focused!=null && children().contains(focused))setFocused(focused);
    }
    @Override protected void init() {
        top=HoiMenuBar.height(width)+29;
        HoiMenuBar.buttons(width,view==null?"":view.country(),null,HoiClient::openMenu).forEach(this::addRenderableWidget);
        addRenderableWidget(new HoiMenuButton("×",width-25,top-26,21,21,this::onClose));
        addRenderableWidget(new HoiMenuButton("+",width-51,top-25,21,20,() -> zoom(1.15,width/2.0,(height+top)/2.0)));
        addRenderableWidget(new HoiMenuButton("−",width-74,top-25,21,20,() -> zoom(1/1.15,width/2.0,(height+top)/2.0)));
        int searchWidth=Math.min(140,Math.max(65,width/5));
        if(search==null) {
            search=new EditBox(font,0,0,searchWidth,18,Component.literal("중점 검색"));
            search.setMaxLength(128);search.setHint(Component.literal("입력하여 검색"));search.setValue(query);search.setResponder(s -> query=s);
        }
        search.setPosition(width-82-searchWidth,top-24);search.setWidth(searchWidth);addRenderableWidget(search);
        if(view!=null && !centered)centerTree();
        if(!selected.isEmpty()) {
            var n=selectedNode();if(n==null){selected="";return;}
            int w=detailWidth(),x=(width-w)/2,y=detailY();
            addRenderableWidget(new HoiMenuButton("×",x+w-25,y+3,19,19,() -> {selected="";rebuildWidgets();}));
            var start=new HoiMenuButton("중점 시작",x+w/2-52,y+detailHeight()-29,104,21,() -> {request(FocusProtocol.Action.START,n.id());rebuildWidgets();});start.active=n.enabled() && waiting==0;addRenderableWidget(start);
        }
        if(view!=null && view.cancelable()) {var cancel=new HoiMenuButton("중점 취소",8,height-28,85,21,() -> {request(FocusProtocol.Action.CANCEL,"");rebuildWidgets();});cancel.active=waiting==0;addRenderableWidget(cancel);}
    }
    private void centerTree() {
        if(view.nodes().isEmpty()){panX=width/2f;panY=top+8;centered=true;return;}
        double minX=view.nodes().stream().mapToDouble(n -> n.x()*X_PITCH).min().orElse(0),maxX=view.nodes().stream().mapToDouble(n -> n.x()*X_PITCH).max().orElse(0);
        double minY=view.nodes().stream().mapToDouble(n -> n.y()*Y_PITCH).min().orElse(0);
        zoom=1;
        panX=(float)(width/2.0-(minX+maxX)*zoom/2);panY=(float)(top+8-minY*zoom);centered=true;
    }
    private float x(FocusProtocol.Node n){return (float)(panX+n.x()*X_PITCH*zoom);}
    private float y(FocusProtocol.Node n){return (float)(panY+n.y()*Y_PITCH*zoom);}
    private FocusProtocol.Node at(double mx,double my) {
        if(view==null || my<top || my>=height-5)return null;
        for(var n:view.nodes())if(mx>=x(n)-NODE_W*zoom/2 && mx<x(n)+NODE_W*zoom/2 && my>=y(n) && my<y(n)+NODE_H*zoom)return n;return null;
    }
    private FocusProtocol.Node selectedNode(){return view==null?null:view.nodes().stream().filter(n -> n.id().equals(selected)).findFirst().orElse(null);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xFF030804);HoiMenuBar.draw(g,width,view==null?CountryHud.UNKNOWN:view.hud(),mx,my,0);
        HoiMenuStyle.panel(g,0,top-29,width,29);
        g.text(font,font.plainSubstrByWidth((view==null?"":view.name()+" ")+"중점 계통도",Math.max(30,width-245)),10,top-19,HoiMenuStyle.TEXT);
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
        } else g.centeredText(font,"중점 계통도 불러오는 중…",width/2,top+36,HoiMenuStyle.TEXT);
        g.disableScissor();
        if(view!=null && view.nodes().isEmpty())g.centeredText(font,"표시할 중점이 없습니다.",width/2,top+36,HoiMenuStyle.MUTED);
        if(!selected.isEmpty())detail(g);
        super.extractRenderState(g,mx,my,delta);
        if(selected.isEmpty()) {var node=at(mx,my);if(node!=null)HoiTooltips.draw(g,font,node.name()+"\n"+node.description()+"\n"+node.status()+(node.remaining()>0?" · "+((node.remaining()+23)/24)+"일 남음":""),mx,my);}
        if(view!=null && !view.message().isEmpty())g.text(font,font.plainSubstrByWidth(view.message(),width-115),105,height-19,HoiMenuStyle.TEXT);
    }
    private void link(GuiGraphicsExtractor g,FocusProtocol.Node parent,FocusProtocol.Node child) {
        int px=(int)x(parent),py=(int)(y(parent)+NODE_H*zoom-2*zoom),cx=(int)x(child),cy=(int)y(child),middle=(py+cy)/2;
        int color=parent.status().equals("완료")?0xFF92A360:0xFF45616D;
        g.fill(px,Math.min(py,middle),px+1,Math.max(py,middle)+1,color);g.fill(Math.min(px,cx),middle,Math.max(px,cx)+1,middle+1,color);g.fill(cx,Math.min(middle,cy),cx+1,Math.max(middle,cy)+1,color);
    }
    private void drawNode(GuiGraphicsExtractor g,FocusProtocol.Node n) {
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
    private void drawLabel(GuiGraphicsExtractor g,FocusProtocol.Node n) {
        float left=x(n)-NODE_W*zoom/2,upper=y(n);
        if(left>width || left+NODE_W*zoom<0 || upper>height || upper+NODE_H*zoom<top)return;
        boolean match=query.isBlank() || n.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
        double guiScale=minecraft.getWindow().getGuiScale();
        float textScale=(float)(Math.max(1,Math.round(zoom*guiScale))/guiScale);
        var lines=font.split(Component.literal(n.name()),Math.max(1,(int)((NODE_W-8)*zoom/textScale)));
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
    private int detailWidth(){return Math.min(350,width-20);}
    private int detailHeight(){return Math.min(245,height-top-12);}
    private int detailY(){return top+(height-top-detailHeight())/2;}
    private void detail(GuiGraphicsExtractor g) {
        var n=selectedNode();if(n==null)return;int w=detailWidth(),h=detailHeight(),left=(width-w)/2,upper=detailY();
        g.fill(0,top,width,height,0x88000000);HoiMenuStyle.panel(g,left,upper,w,h);
        g.text(font,font.plainSubstrByWidth(n.name(),w-45),left+10,upper+9,HoiMenuStyle.TEXT);
        var lines=font.split(Component.literal(n.description()+"\n\n"+n.status()),w-24);int limit=Math.max(1,(h-65)/font.lineHeight);
        for(int i=0;i<Math.min(lines.size(),limit);i++)g.text(font,lines.get(i),left+12,upper+34+i*font.lineHeight,HoiMenuStyle.TEXT,false);
    }
    private void zoom(double factor,double mx,double my) {
        float next=(float)Math.clamp(zoom*factor,.35,1.8);float ratio=next/zoom;panX=(float)(mx-(mx-panX)*ratio);panY=(float)(my-(my-panY)*ratio);zoom=next;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(!selected.isEmpty() || event.y()<top)return super.mouseClicked(event,twice);
        var n=at(event.x(),event.y());
        if(event.button()==0 && n!=null){selected=n.id();rebuildWidgets();return true;}
        if(event.button()==0 || event.button()==1 || event.button()==2){panning=true;return true;}
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {if(panning){panX+=(float)dx;panY+=(float)dy;return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent event){panning=false;return super.mouseReleased(event);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(y>=top && selected.isEmpty()){zoom(Math.pow(1.1,vertical),x,y);return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean isPauseScreen(){return false;}
}
