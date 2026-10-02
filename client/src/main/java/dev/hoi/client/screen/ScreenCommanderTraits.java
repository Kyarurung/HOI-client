package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;
import java.util.function.Consumer;

final class ScreenCommanderTraits extends ScreenDialog implements dev.hoi.client.input.InputSidebarMovement.InputMovementScreen {
    private final Screen parent;
    private final Consumer<ProtocolDialogPayloads.Action> sender;
    private boolean pending;
    private int left,top,scroll,terrainScroll,ownedScroll,ownedMax;
    private UiUnits units;
    private final Map<String,AbstractWidget> choices=new HashMap<>();
    private final Set<AbstractWidget> nodeButtons=new HashSet<>();
    ScreenCommanderTraits(ProtocolDialogView view,Screen parent) {this(view,parent,null);}
    ScreenCommanderTraits(ProtocolDialogView view,Screen parent,Consumer<ProtocolDialogPayloads.Action> sender) {super(view);this.parent=parent;this.sender=sender;}
    Screen backdrop() {return parent;}
    void dismiss() {ScreenDialogCoordinator.setContent(parent);}
    AbstractWidget choiceButton(String id) {return choices.get(id);}
    @Override public boolean allowsMovement() {return true;}
    @Override public int unitHudLeft() {return parent instanceof dev.hoi.client.input.InputSidebarMovement.InputMovementScreen sidebar?sidebar.unitHudLeft():0;}
    @Override void update(ProtocolDialogView next) {if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;pending=false;super.update(next);}
    private int x(double value) {return left+Math.round(units.length(value));}
    private int y(double value) {return top+Math.round(units.length(value));}
    private int size(double value) {return units.size(value);}
    private void sourceArt(GuiGraphicsExtractor g,String asset,float px,float py,int w,int h,float nativeScale) {g.pose().pushMatrix();g.pose().translate(px,py);g.pose().scale(nativeScale*units.scale());UiAssets.draw(g,asset,0,0,w,h);g.pose().popMatrix();}
    private boolean confirming() {return view().image().equals("commander/trait-confirm");}
    private void submit(String id) {
        if(pending||view().token().isEmpty()||sender==null&&!ClientPlayNetworking.canSend(ProtocolDialogPayloads.Action.TYPE))return;
        pending=true;children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).forEach(w->w.active=false);
        var action=new ProtocolDialogPayloads.Action(view().token(),view().revision(),id);if(sender==null)ClientPlayNetworking.send(action);else sender.accept(action);
    }
    private int nodeX(ProtocolDialogView.TraitNode n) {return x((n.terrain()?469.5:147)+104*n.column());}
    private int nodeY(ProtocolDialogView.TraitNode n) {return y(55+27.5*n.row())-(n.terrain()?terrainScroll:scroll);}
    private boolean visible(ProtocolDialogView.TraitNode n) {int py=nodeY(n);return n.row()>=0&&py>=y(55)&&py+size(19.5)<=y(375);}
    private void button(String id,String text,double px,double py,double w,double h) {
        var button=new UiMenuButton(text,x(px),y(py),size(w),size(h),()->submit(id)).caption("");button.active=!pending;choices.put(id,button);addRenderableWidget(button);
    }
    @Override protected void init() {
        units=new UiUnits(width,height);left=width/2-Math.round(units.length(335));top=height/2-Math.round(units.length(175));choices.clear();nodeButtons.clear();
        addRenderableWidget(new UiMenuButton("×",x(648),y(5.5),size(15),size(15),this::onClose));
        if(confirming()) {
            if(view().choices().stream().anyMatch(c->c.id().equals("confirm")))button("confirm","확인",310,238,60,17);
            button("cancel","취소",380,238,60,17);
        } else {
            for(var tile:view().tiles()) {
                var n=tile.traitNode();if(n==null||!visible(n))continue;
                if(n.available()&&view().choices().stream().anyMatch(c->c.id().equals(tile.section()))) {
                    var button=new UiMenuButton(tile.name(),nodeX(n),nodeY(n),size(83),size(19.5),()->submit(tile.section())).caption("");button.active=!pending;choices.put(tile.section(),button);nodeButtons.add(button);addRenderableWidget(button);
                }
            }
            if(view().choices().stream().anyMatch(c->c.id().equals("back")))button("back","돌아가기",12,365,116,17);
        }
        if(pending)children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).forEach(w->w.active=false);
    }
    private void label(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,int color,boolean centered) {
        float scale=8*units.scale()/font.lineHeight;var component=UiKeywordIcons.decorateLines(UiTooltips.officerText(value),false);float horizontal=Math.min(1,w/Math.max(1,font.width(component)*scale));g.pose().pushMatrix();g.pose().translate(px+(centered?(w-font.width(component)*scale*horizontal)/2:0),py+(h-font.lineHeight*scale)/2);g.pose().scale(scale*horizontal,scale);g.text(font,component,0,0,color);g.pose().popMatrix();
    }
    private void drawButton(GuiGraphicsExtractor g,AbstractWidget button,int mx,int my,float delta) {button.extractRenderState(g,mx,my,delta);label(g,button.getMessage().getString(),button.getX()+size(2),button.getY(),Math.max(1,button.getWidth()-size(4)),button.getHeight(),button.active?UiMenuStyle.TEXT:UiMenuStyle.MUTED,true);}
    private void text(GuiGraphicsExtractor g,String value,double px,double py,double w,double h,int color) {label(g,value,x(px),y(py),size(w),size(h),color,true);}
    private void line(GuiGraphicsExtractor g,int ax,int ay,int bx,int by,int color,boolean dashed) {
        int thick=Math.max(1,size(.5));if(ax==bx){g.fill(ax,Math.min(ay,by),ax+thick,Math.max(ay,by)+thick,color);return;}
        int lo=Math.min(ax,bx),hi=Math.max(ax,bx);if(!dashed){g.fill(lo,ay,hi+thick,ay+thick,color);return;}
        int dash=Math.max(2,size(3));for(int px=lo;px<hi;px+=dash*2)g.fill(px,ay,Math.min(hi,px+dash),ay+thick,color);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        sourceArt(g,"commander/tree/tree_bg",left,top,1343,783,.5f);
        text(g,"지휘관 세부 사항",17.5,3.5,235,16,UiMenuStyle.TEXT);
        text(g,"특성",185,9,120,13,UiMenuStyle.TEXT);
        var hero=view().tiles().stream().filter(t->t.leader()!=null).findFirst().orElseThrow();var leader=hero.leader();
        UiAssets.draw(g,hero.icon(),x(5.5),y(32.5),size(33),size(44));
        text(g,Integer.toString(leader.skill()),38,40,25,18,0xFFFFD34B);text(g,hero.value(),62.5,34.5,75,10,0xFF161616);text(g,hero.name(),62.5,43.5,75,10,0xFF161616);
        for(int i=0;i<4;i++) {String key=List.of("attack","defense",leader.role().equals("제독")?"maneuver":"planning",leader.role().equals("제독")?"coordination":"logistics").get(i);UiAssets.draw(g,"commander/ui/"+key,x(66+i*12.5),y(56),size(12),size(12));text(g,Integer.toString(leader.abilities().get(i)),65.5+i*12.5,68,15,13,0xFF161616);}
        String hovered="";int ownedY=88-ownedScroll;g.enableScissor(x(8),y(87.5),x(140),y(337.5));
        for(boolean personal:List.of(true,false)) {
            text(g,personal?"뒷배경":"특성",8,ownedY,127,12,0xFF161616);ownedY+=15;
            for(var trait:leader.traitDetails()) {
                boolean personality=view().tiles().stream().filter(t->t.traitNode()!=null&&t.traitNode().owned()&&t.name().equals(trait.name())).findFirst().map(t->Set.of("personality_trait","status_trait").contains(t.traitNode().kind())).orElse(true);if(personality!=personal)continue;
                if(!trait.icon().isEmpty())sourceArt(g,trait.icon(),x(10),y(ownedY),23,33,.5f);label(g,trait.name(),x(28),y(ownedY),size(104),size(9),0xFF161616,false);
                var parts=trait.tooltip().split("\n\n",-1);int rowY=ownedY+10;
                if(parts.length>1)for(String effect:parts[1].split("\n")) {if(effect.contains("역할 해금")||!effect.contains(": "))break;label(g,effect,x(28),y(rowY),size(104),size(9),0xFF161616,false);rowY+=9;}
                if(mx>=x(8)&&mx<x(135)&&my>=y(87.5)&&my<y(337.5)&&my>=y(ownedY)&&my<y(rowY))hovered=trait.tooltip();ownedY=Math.max(ownedY+20,rowY+6);
            }
        }
        g.disableScissor();ownedMax=Math.max(0,ownedY+ownedScroll-337);
        int bodyY=342;for(String line:view().body().split("\n")) {text(g,line,8,bodyY,125,10,0xFF161616);bodyY+=10;}
        boolean navy=leader.role().equals("제독");text(g,"획득 특성",122.5,35,150,13,UiMenuStyle.TEXT);
        text(g,navy?"제독 특성":"장군 특성",navy?228.5:219,35,150,13,UiMenuStyle.TEXT);
        if(!navy)text(g,"원수 특성",325,35,150,13,UiMenuStyle.TEXT);text(g,"지형 특성",navy?515:487.5,35,150,13,UiMenuStyle.TEXT);
        var nodes=new HashMap<String,ProtocolDialogView.Tile>();for(var tile:view().tiles())if(tile.traitNode()!=null)nodes.put(tile.section(),tile);
        var highlight=new HashSet<String>();
        for(var tile:nodes.values()) {var n=tile.traitNode();if(visible(n)&&mx>=nodeX(n)&&mx<nodeX(n)+size(83)&&my>=nodeY(n)&&my<nodeY(n)+size(19.5)) {var queue=new ArrayDeque<String>();queue.add(tile.section());while(!queue.isEmpty()){String current=queue.removeFirst();if(!highlight.add(current))continue;var next=nodes.get(current);if(next!=null)queue.addAll(next.traitNode().parents());}}}
        for(var tile:nodes.values()) {var n=tile.traitNode();if(!visible(n))continue;for(String parentId:n.parents()) {var parentTile=nodes.get(parentId);if(parentTile==null||!visible(parentTile.traitNode()))continue;var p=parentTile.traitNode();int ax=nodeX(p)+size(83),ay=nodeY(p)+size(7.5),bx=nodeX(n),by=nodeY(n)+size(7.5);if(ax>bx)continue;int middle=(ax+bx)/2,color=highlight.contains(tile.section())?0xFFFFFF00:p.owned()?0xFF78EDA6:0xFFABBFED;line(g,ax,ay,middle,ay,color,n.parents().size()>1);line(g,middle,ay,middle,by,color,false);line(g,middle,by,bx,by,color,n.parents().size()>1);}}
        var exclusions=new HashSet<String>();
        for(var tile:nodes.values()) {var n=tile.traitNode();if(!visible(n))continue;for(String other:n.exclusive()) {var paired=nodes.get(other);if(paired==null||!visible(paired.traitNode()))continue;var p=paired.traitNode();String pair=tile.section().compareTo(other)<0?tile.section()+"/"+other:other+"/"+tile.section();if(!exclusions.add(pair)||n.terrain()!=p.terrain()||n.column()!=p.column())continue;int markerX=nodeX(n)+size(40.5),markerY=(nodeY(n)+nodeY(p))/2+size(7.5);sourceArt(g,"commander/tree/exclusive",markerX,markerY,20,25,.3f);}}
        for(var tile:view().tiles()) {var n=tile.traitNode();if(n==null||!visible(n))continue;int px=nodeX(n),py=nodeY(n);boolean hover=mx>=px&&mx<px+size(83)&&my>=py&&my<py+size(19.5);String state=n.owned()?"picked":hover||n.available()?"pickable":"disabled";
            sourceArt(g,"commander/tree/"+state,px,py,166,39,.5f);if(!tile.icon().isEmpty())sourceArt(g,tile.icon(),px+units.length(5),py+units.length(2),23,33,.5f);label(g,tile.name(),px+size(20),py+size(6),size(61.5),size(10),UiMenuStyle.TEXT,false);if(hover)hovered=tile.detail();
        }
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable widget&&!nodeButtons.contains(child)){if(child instanceof AbstractWidget button&&choices.containsValue(button))drawButton(g,button,mx,my,delta);else widget.extractRenderState(g,mx,my,delta);}
        if(confirming()) {UiMenuStyle.panel(g,x(230),y(170),size(225),size(93));text(g,view().title(),238,178,209,19,UiMenuStyle.TEXT);text(g,view().body(),238,202,209,24,UiMenuStyle.TEXT);for(String key:List.of("confirm","cancel")){var button=choices.get(key);if(button!=null)drawButton(g,button,mx,my,delta);}}
        else if(!hovered.isEmpty())UiTooltips.drawOfficerScrollable(g,font,hovered,mx,my,width,height,0,size(240));
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(confirming())return false;
        if(mx>=x(8)&&mx<x(140)&&my>=y(87.5)&&my<y(337.5)) {ownedScroll=Math.clamp(ownedScroll-(int)(vertical*20),0,ownedMax);return true;}
        if(mx<x(147)||mx>=x(669)||my<y(55)||my>=y(375))return false;
        boolean terrain=mx>=x(469.5);int max=view().tiles().stream().filter(t->t.traitNode()!=null&&t.traitNode().terrain()==terrain).mapToInt(t->Math.max(0,t.traitNode().row())*size(27.5)+size(19.5)-size(320)).max().orElse(0);
        if(terrain)terrainScroll=Math.clamp(terrainScroll-(int)(vertical*size(20)),0,Math.max(0,max));else scroll=Math.clamp(scroll-(int)(vertical*size(20)),0,Math.max(0,max));rebuildWidgets();return true;
    }
    @Override public void onClose() {submit("");dismiss();}
}
