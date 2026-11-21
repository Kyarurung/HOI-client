package dev.hoi.client.screen.commander;

import dev.hoi.client.ui.*;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.dialog.DialogScreen;
import dev.hoi.protocol.dialog.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;
import java.util.function.Consumer;

public final class CommanderTraits extends DialogScreen implements dev.hoi.client.input.InputSidebarMovement.InputMovementScreen {
    private final Screen parent;
    private final Consumer<DialogPayloads.Action> sender;
    private boolean pending;
    private Tooltips.ScrolledTooltip tooltipBounds;
    public Tooltips.ScrolledTooltip tooltipBounds() {return tooltipBounds;}
    private int left,top,scroll,terrainScroll,ownedScroll,ownedMax;
    private int dragging=-1;
    private float scale;
    private String clippedTooltip="";
    private int mouseX,mouseY;
    private final Map<String,AbstractWidget> choices=new HashMap<>();
    private final Set<AbstractWidget> nodeButtons=new HashSet<>();
    public CommanderTraits(DialogView view,Screen parent) {this(view,parent,null);}
    public CommanderTraits(DialogView view,Screen parent,Consumer<DialogPayloads.Action> sender) {super(view);this.parent=parent;this.sender=sender;}
    public Screen backdrop() {return parent;}
    public void dismiss() {DialogCoordinator.setContent(parent);}
    AbstractWidget choiceButton(String id) {return choices.get(id);}
    @Override public boolean allowsMovement() {return true;}
    @Override public int unitHudLeft() {return parent instanceof dev.hoi.client.input.InputSidebarMovement.InputMovementScreen sidebar?sidebar.unitHudLeft():0;}
    @Override public void update(DialogView next) {if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;pending=false;super.update(next);}
    public int x(double value) {return left+Math.round(length(value));}
    public int y(double value) {return top+Math.round(length(value));}
    private float length(double value) {return (float)(value*scale);}
    private int size(double value) {return (int)Math.ceil(length(value));}
    private void sourceArt(GuiGraphicsExtractor g,String asset,float px,float py,int w,int h,float nativeScale) {g.pose().pushMatrix();g.pose().translate(px,py);g.pose().scale(nativeScale*scale);UiAssets.draw(g,asset,0,0,w,h);g.pose().popMatrix();}
    private boolean confirming() {return view().image().equals("commander/trait-confirm");}
    private boolean advisorRoles() {return view().image().equals("commander/advisor-roles");}
    private List<DialogView.Tile> roleTiles() {return view().tiles().stream().filter(t->t.traitNode()!=null?t.traitNode().kind().equals("advisor_role"):t.leader()==null&&!t.section().equals("preferred-tactic-info")).toList();}
    private boolean chief(DialogView.Tile tile) {return tile.traitNode()!=null?tile.traitNode().column()==1:tile.section().startsWith("ARMY_CHIEF/")||tile.section().startsWith("NAVY_CHIEF/");}
    private int roleY(DialogView.Tile tile) {return y(55+26*(tile.traitNode()!=null?tile.traitNode().row():roleTiles().stream().takeWhile(t->t!=tile).filter(t->chief(t)==chief(tile)).count()))-(chief(tile)?terrainScroll:scroll);}
    private void submit(String id) {
        if(pending||view().token().isEmpty()||sender==null&&!ClientPlayNetworking.canSend(DialogPayloads.Action.TYPE))return;
        pending=true;children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).forEach(w->w.active=false);
        var action=new DialogPayloads.Action(view().token(),view().revision(),id);if(sender==null)ClientPlayNetworking.send(action);else sender.accept(action);
    }
    private int nodeX(DialogView.TraitNode n) {return x((n.terrain()?469.5:147)+104*n.column());}
    private int nodeY(DialogView.TraitNode n) {return y(55+27.5*n.row())-(n.terrain()?terrainScroll:scroll);}
    public boolean visible(DialogView.TraitNode n) {int py=nodeY(n);return n.row()>=0&&py>=y(55)&&py+size(19.5)<=y(380);}
    private int maximum(boolean terrain){if(advisorRoles())return Math.max(0,(int)roleTiles().stream().filter(t->chief(t)==terrain).count()*size(26)-size(325));return Math.max(0,view().tiles().stream().filter(t->t.traitNode()!=null&&t.traitNode().terrain()==terrain).mapToInt(t->Math.max(0,t.traitNode().row())*size(27.5)+size(19.5)-size(325)).max().orElse(0));}
    private UiScrollBar scrollbar(boolean terrain){return new UiScrollBar(x(terrain?661:452),y(47),y(388),size(325),size(325)+maximum(terrain));}
    private void scrollbar(GuiGraphicsExtractor g,boolean terrain){scrollbar(terrain).draw(g,terrain?terrainScroll:scroll);}
    private void dragScrollbar(double mouseY){int next=scrollbar(dragging==1).offset(mouseY);if(dragging==1)terrainScroll=next;else scroll=next;rebuildWidgets();}
    private void button(String id,String text,double px,double py,double w,double h) {
        var button=new UiMenuButton(text,x(px),y(py),size(w),size(h),()->submit(id)).caption("");if(id.equals("advisor-roles")||id.equals("traits"))button.background("officer/hq/captured_tab/frame_1");button.active=!pending;choices.put(id,button);addRenderableWidget(button);
    }
    @Override protected void init() {
        scale=Math.min(1,Math.min((width-12)/671.5f,(height-HudMenuBar.height(width)-12)/391.5f));left=(width-size(671.5))/2;top=Math.max(HudMenuBar.height(width)+6,(height-size(391.5))/2);choices.clear();nodeButtons.clear();
        addRenderableWidget(UiMenuStyle.close("menu/close",x(648),y(5.5),size(16),size(16.5),this::onClose));
        if(confirming()) {
            if(view().choices().stream().anyMatch(c->c.id().equals("confirm")))button("confirm","확인",310,238,60,17);
            button("cancel","취소",380,238,60,17);
        } else {
            if(advisorRoles()) {
                view().choices().stream().filter(c->c.id().equals("traits")).findFirst().ifPresent(c->button(c.id(),c.label(),259,6.5,120,19.5));
                for(var tile:roleTiles())if(roleY(tile)>=y(55)&&roleY(tile)+size(26)<=y(380)&&view().choices().stream().anyMatch(c->c.id().equals(tile.section()))){var button=new UiMenuButton(tile.name(),x(chief(tile)?469.5:147),roleY(tile),size(122),size(26),()->submit(tile.section())).caption("");button.active=!pending;choices.put(tile.section(),button);nodeButtons.add(button);addRenderableWidget(button);}
            } else {
            button("advisor-roles","장교단 역할",387,6.5,120,19.5);
            choices.get("advisor-roles").active=!pending&&view().choices().stream().anyMatch(c->c.id().equals("advisor-roles"));
            for(var tile:view().tiles()) {
                var n=tile.traitNode();if(n==null||!visible(n))continue;
                if(n.available()&&view().choices().stream().anyMatch(c->c.id().equals(tile.section()))) {
                    var button=new UiMenuButton(tile.name(),nodeX(n),nodeY(n),size(83),size(19.5),()->submit(tile.section())).caption("");button.active=!pending;choices.put(tile.section(),button);nodeButtons.add(button);addRenderableWidget(button);
                }
            }
            }
            view().tiles().stream().filter(t->t.section().equals("preferred-tactic-info")).findFirst().ifPresent(tile->{
                button("preferred-tactic",tile.value(),5,337.5,135,46.5);
                ((UiMenuButton)choices.get("preferred-tactic")).background("none");
                choices.get("preferred-tactic").active=!pending&&view().choices().stream().anyMatch(c->c.id().equals("preferred-tactic"));
                choices.get("preferred-tactic").setTooltip(null);
            });
        }
        if(pending)children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).forEach(w->w.active=false);
    }
    private void label(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,int color,boolean centered) {
        var component=TooltipKeywordIcons.decorateLines(value.startsWith("§l")?Tooltips.officerText(value,true):Tooltips.officerText(value),false);boolean clipped=font.width(component)>w;
        UiText.marquee(g,font,component,px,py+Math.max(0,(h-font.lineHeight)/2),w,1,color,false,centered?0:-1);
        if(clipped&&mouseX>=px&&mouseX<px+w&&mouseY>=py&&mouseY<py+Math.max(h,font.lineHeight))clippedTooltip=value;
    }
    private void drawButton(GuiGraphicsExtractor g,AbstractWidget button,int mx,int my,float delta) {button.extractRenderState(g,mx,my,delta);label(g,button.getMessage().getString(),button.getX()+size(2),button.getY(),Math.max(1,button.getWidth()-size(4)),button.getHeight(),button.active?UiMenuStyle.TEXT:UiMenuStyle.MUTED,true);}
    private void text(GuiGraphicsExtractor g,String value,double px,double py,double w,double h,int color) {label(g,value,x(px),y(py),size(w),size(h),color,true);}
    private void ownedLabel(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,int color,boolean centered) {if(py>=y(87.5)&&py+Math.max(h,font.lineHeight)<=y(331.5))label(g,value,px,py,w,h,color,centered);}
    private void line(GuiGraphicsExtractor g,int ax,int ay,int bx,int by,int color,boolean dashed) {
        int thick=Math.max(1,size(.5));if(ax==bx){g.fill(ax,Math.min(ay,by),ax+thick,Math.max(ay,by)+thick,color);return;}
        int lo=Math.min(ax,bx),hi=Math.max(ax,bx);if(!dashed){g.fill(lo,ay,hi+thick,ay+thick,color);return;}
        int dash=Math.max(2,size(3));for(int px=lo;px<hi;px+=dash*2)g.fill(px,ay,Math.min(hi,px+dash),ay+thick,color);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        mouseX=mx;mouseY=my;clippedTooltip="";
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        g.fill(x(3),y(3),x(668),y(388),0xFF191B20);
        sourceArt(g,"commander/tree/tree_bg",left,top,1343,783,.5f);
        g.pose().pushMatrix();g.pose().translate(x(17.5),y(5.5));g.pose().scale(Math.max(1,12*scale/font.lineHeight));g.text(font,net.minecraft.network.chat.Component.literal("지휘관 세부 사항"),0,0,0xFFFFFFFF,false);g.pose().popMatrix();
        UiAssets.draw(g,"officer/hq/captured_tab/frame_0",x(advisorRoles()?387:259),y(6.5),size(120),size(19.5));
        text(g,advisorRoles()?"장교단 역할":"특성",advisorRoles()?387:259,6.5,120,19.5,UiMenuStyle.TEXT);
        var hero=view().tiles().stream().filter(t->t.leader()!=null).findFirst().orElse(null);
        tooltipBounds=null;ownedMax=0;String hovered="";
        if(hero!=null) {
            var leader=hero.leader();
            sourceArt(g,hero.icon(),x(5.5),y(32.5),156,210,.195f);
            sourceArt(g,"commander/commands/unit_leader_portrait_frame/frame_0",x(2.25),y(28.5),71,104,.5f);
            sourceArt(g,"commander/commands/land_battle_leader_skill_bg/frame_0",x(40),y(36.5),35,50,.6f);
            var skill=net.minecraft.network.chat.Component.literal(Integer.toString(leader.skill())).withStyle(net.minecraft.ChatFormatting.BOLD);float skillScale=Math.max(1,1.2f*scale);g.pose().pushMatrix();g.pose().translate(x(50.5)-font.width(skill)*skillScale/2,y(41));g.pose().scale(skillScale);g.text(font,skill,0,0,0xFFFFFFFF,false);g.pose().popMatrix();label(g,"§l"+leader.role(),x(62.5),y(34.5),size(75),size(10),0xFFFFFFFF,false);label(g,"§l"+hero.name(),x(62.5),y(43.5),size(75),size(10),0xFFFFFFFF,false);
            for(int i=0;i<4;i++) {String key=List.of("attack","defense",leader.role().equals("제독")?"maneuver":"planning",leader.role().equals("제독")?"coordination":"logistics").get(i);UiAssets.draw(g,"commander/ui/"+key,x(new double[]{66.75,78.75,91.75,104.25}[i]),y(new double[]{52.75,52.75,53.25,53.75}[i]),size(13.5),size(12.5));text(g,Integer.toString(leader.abilities().get(i)),65.5+i*12.5,63,15,10,0xFFFFFFFF);}
            sourceArt(g,"commander/commands/leader_exp_bg/frame_0",x(42),y(74),171,11,.5f);
            UiAssets.draw(g,"commander/commands/leader_exp_fg_mid/background/frame_0",x(42),y(74),size(86),size(5));
            if(hero.gauge()!=null&&hero.gauge().fraction()>0){g.enableScissor(x(42),y(74),x(42)+Math.round(size(86)*(float)hero.gauge().fraction()),y(79));UiAssets.draw(g,"commander/commands/leader_exp_fg_mid/fill/frame_0",x(42),y(74),size(86),size(5));g.disableScissor();}
            hovered=mx>=x(42)&&mx<x(128)&&my>=y(74)&&my<y(79)?hero.gauge()==null?"경험치: 미확인":hero.gauge().tooltip():"";
            for(int i=0;i<leader.abilityTooltips().size();i++)if(mx>=x(66+i*12.5)&&mx<x(78.5+i*12.5)&&my>=y(56)&&my<y(74))hovered=leader.abilityTooltips().get(i);
            int ownedY=88-ownedScroll;g.enableScissor(x(8),y(87.5),x(140),y(331.5));
            int textStep=Math.max(10,(int)Math.ceil((font.lineHeight+1)/scale));
            for(String category:List.of("advisor","background","trait")) {
                var owned=view().tiles().stream().filter(t->{var n=t.traitNode();if(n==null||!n.owned()||n.kind().equals("advisor_role"))return false;return category.equals("advisor")?n.kind().equals("advisor"):!n.kind().equals("advisor")&&Set.of("personality_trait","status_trait").contains(n.kind())==category.equals("background");}).toList();
                if(owned.isEmpty())continue;
                UiAssets.draw(g,"commander/tree/separator",x(4),y(ownedY+5),size(136),size(2.5));ownedLabel(g,"§l"+switch(category){case "advisor"->"장교단";case "background"->"뒷배경";default->"특성";},x(8),y(ownedY),size(127),size(textStep),0xFF000000,true);ownedY+=textStep+4;
                for(var trait:owned) {
                    if(category.equals("advisor"))UiAssets.drawLeaderControl(g,trait.icon(),x(9),y(ownedY),size(18),size(20),true);
                    else if(!trait.icon().isEmpty())sourceArt(g,trait.icon(),x(10),y(ownedY),23,33,.5f);
                    ownedLabel(g,"§l"+trait.name(),x(28),y(ownedY),size(104),size(textStep),0xFF000000,false);
                    var lines=trait.detail().split("\n",-1);int rowY=ownedY+textStep;
                    for(int index=1;index<lines.length;index++) {String effect=lines[index];if(effect.isBlank()){if(rowY==ownedY+textStep)continue;break;}if(effect.contains("역할 해금")||!effect.contains(": "))break;var content=TooltipKeywordIcons.decorateLines(Tooltips.officerText(effect.replaceAll("§(?:#[0-9a-fA-F]{6}|.)","")),false);if(y(rowY)>=y(87.5)&&y(rowY)+font.lineHeight<=y(331.5))UiText.marquee(g,font,content,x(28),y(rowY),size(104),1,0xFF000000,false,-1);rowY+=textStep;}
                    if(mx>=x(8)&&mx<x(135)&&my>=y(87.5)&&my<y(331.5)&&my>=y(ownedY)&&my<y(rowY))hovered=trait.detail();ownedY=Math.max(ownedY+20,rowY+6);
                }
            }
            sourceArt(g,"commander/tree/new",x(10),y(ownedY),33,40,.45f);ownedLabel(g,"특성 선택",x(28),y(ownedY+2),size(104),size(12),0xFFB3A578,false);
            if(mx>=x(8)&&mx<x(135)&&my>=y(87.5)&&my>=y(ownedY)&&my<y(ownedY+20)&&my<y(331.5))hovered=view().body();
            ownedY+=24;g.disableScissor();ownedMax=Math.max(0,ownedY+ownedScroll-331);
            if(ownedMax>0){int h=y(331.5)-y(87.5),thumb=8;UiAssets.nineSlice(g,"commander/commands/scroll_track/frame_0",x(136),y(87.5),size(4),h,3,scale*.5);UiAssets.draw(g,"commander/commands/scroll_drager/frame_0",x(138)-thumb/2,y(87.5)+(h-thumb)*ownedScroll/ownedMax,thumb,thumb);}
        }
        var preferred=view().tiles().stream().filter(t->t.section().equals("preferred-tactic-info")).findFirst().orElse(null);
        if(preferred!=null) {
            UiAssets.nineSlice(g,"commander/commands/tiled_plain_bg/frame_0",x(5),y(337.5),size(135),size(46.5),4,.5*scale);
            text(g,preferred.name(),10,341,125,12,0xFFFFFFFF);
            var button=choices.get("preferred-tactic");if(button!=null&&Tooltips.contains(button,mx,my))hovered=preferred.detail();
        }
        boolean navy=hero!=null?hero.leader().role().equals("제독"):roleTiles().stream().anyMatch(t->t.section().contains("NAVY_CHIEF/"));
        if(advisorRoles()) {
            text(g,"§l최고사령부",147,35,305,13,0xFFFFFFFF);text(g,navy?"§l해군참모총장":"§l육군참모총장",469.5,35,192,13,0xFFFFFFFF);
            for(var tile:roleTiles()) {
                int px=x(chief(tile)?469.5:147),py=roleY(tile);if(py<y(55)||py+size(26)>y(380))continue;
                boolean owned=tile.traitNode()!=null?tile.traitNode().owned():tile.section().endsWith("/dismiss"),hover=mx>=px&&mx<px+size(122)&&my>=py&&my<py+size(26);
                sourceArt(g,"commander/commands/trait_adviser_"+(owned?"picked":"disabled")+"/frame_0",px,py,244,52,.5f);
                if(!tile.icon().isEmpty())UiAssets.draw(g,tile.icon(),px+size(3),py+size(3),size(20),size(20));
                String[] lines=dev.hoi.client.ui.tooltip.TooltipText.sourceSpacing(tile.name()).split("\n",-1);int textHeight=Math.max(font.lineHeight,size(10)),textTop=py+size(2);
                for(int index=0;index<lines.length;index++)label(g,lines[index],px+size(25),textTop+index*textHeight,size(94),textHeight,0xFFFFFFFF,false);
                if(hover)hovered=tile.detail();
            }
        } else {
        text(g,"획득 특성",122.5,35,150,13,UiMenuStyle.TEXT);
        text(g,navy?"제독 특성":"장군 특성",navy?228.5:219,35,150,13,UiMenuStyle.TEXT);
        if(!navy)text(g,"원수 특성",325,35,150,13,UiMenuStyle.TEXT);text(g,"지형 특성",navy?515:487.5,35,150,13,UiMenuStyle.TEXT);
        var nodes=new HashMap<String,DialogView.Tile>();for(var tile:view().tiles())if(tile.traitNode()!=null)nodes.put(tile.section(),tile);
        for(var tile:nodes.values()) {var n=tile.traitNode();if(!visible(n))continue;for(String parentId:n.parents()) {var parentTile=nodes.get(parentId);if(parentTile==null||!visible(parentTile.traitNode()))continue;var p=parentTile.traitNode();int ax=nodeX(p)+size(83),ay=nodeY(p)+size(7.5),bx=nodeX(n),by=nodeY(n)+size(7.5);if(ax>bx)continue;int middle=(ax+bx)/2,color=0xFFBDC2CA;line(g,ax,ay,middle,ay,color,n.parents().size()>1);line(g,middle,ay,middle,by,color,false);line(g,middle,by,bx,by,color,n.parents().size()>1);}}
        var exclusions=new HashSet<String>();
        for(var tile:nodes.values()) {var n=tile.traitNode();if(!visible(n))continue;for(String other:n.exclusive()) {var paired=nodes.get(other);if(paired==null||!visible(paired.traitNode()))continue;var p=paired.traitNode();String pair=tile.section().compareTo(other)<0?tile.section()+"/"+other:other+"/"+tile.section();if(!exclusions.add(pair)||n.terrain()!=p.terrain()||n.column()!=p.column())continue;int markerX=nodeX(n)+size(40.5),markerY=(nodeY(n)+nodeY(p))/2+size(7.5);sourceArt(g,"commander/tree/exclusive",markerX,markerY,20,25,.3f);}}
        for(var tile:view().tiles()) {var n=tile.traitNode();if(n==null||!visible(n))continue;int px=nodeX(n),py=nodeY(n);boolean hover=mx>=px&&mx<px+size(83)&&my>=py&&my<py+size(19.5);String state=n.owned()?"picked":hover||n.available()?"pickable":"disabled";
            sourceArt(g,"commander/tree/"+state,px,py,166,39,.5f);if(!tile.icon().isEmpty())sourceArt(g,tile.icon(),px+length(5),py+length(2),23,33,.5f);label(g,tile.name(),px+size(20),py+size(6),size(61.5),size(10),UiMenuStyle.TEXT,false);if(hover)hovered=tile.detail();
        }
        }
        if(!advisorRoles()||maximum(false)>0)scrollbar(g,false);if(maximum(true)>0)scrollbar(g,true);
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable widget&&!nodeButtons.contains(child)){if(child instanceof AbstractWidget button&&choices.containsValue(button)){if(child==choices.get("preferred-tactic")){button.extractRenderState(g,mx,my,delta);UiAssets.draw(g,preferred.icon(),x(11.5),y(350),size(45),size(25));UiText.marquee(g,font,Tooltips.officerText(preferred.value()),x(65),y(350),size(75),1,0xFFFFFFFF,false,-1);var required=java.util.regex.Pattern.compile("기술 레벨 (\\d+)").matcher(preferred.detail());if(required.find())label(g,"요구 기술 §e"+required.group(1),x(65),y(350)+2*font.lineHeight,size(75),size(10),0xFFFFFFFF,false);}else drawButton(g,button,mx,my,delta);}else widget.extractRenderState(g,mx,my,delta);}
        if(confirming()) {UiMenuStyle.panel(g,x(230),y(170),size(225),size(93));text(g,view().title(),238,178,209,19,UiMenuStyle.TEXT);text(g,view().body(),238,202,209,24,UiMenuStyle.TEXT);for(String key:List.of("confirm","cancel")){var button=choices.get(key);if(button!=null)drawButton(g,button,mx,my,delta);}}
        else if(!hovered.isEmpty()||!clippedTooltip.isEmpty())tooltipBounds=Tooltips.drawOfficerScrollable(g,font,hovered.isEmpty()?clippedTooltip:hovered,mx,my,width,height,0);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(confirming())return false;
        if(mx>=x(8)&&mx<x(140)&&my>=y(87.5)&&my<y(337.5)) {ownedScroll=Math.clamp(ownedScroll-(int)(vertical*20),0,ownedMax);return true;}
        if(mx<x(147)||mx>=x(669)||my<y(55)||my>=y(380))return false;
        boolean terrain=mx>=x(469.5);int max=maximum(terrain);
        if(terrain)terrainScroll=Math.clamp(terrainScroll-(int)(vertical*size(20)),0,Math.max(0,max));else scroll=Math.clamp(scroll-(int)(vertical*size(20)),0,Math.max(0,max));rebuildWidgets();return true;
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){
        if(!confirming()&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)for(int index=0;index<2;index++){var bar=scrollbar(index==1);if(bar.contains(event.x(),event.y())){if(bar.arrow(event.y())){if(index==1)terrainScroll=bar.step(event.y(),terrainScroll,size(26));else scroll=bar.step(event.y(),scroll,size(26));rebuildWidgets();}else{dragging=index;dragScrollbar(event.y());}return true;}}
        return super.mouseClicked(event,twice)||event.x()>=left&&event.x()<x(671.5)&&event.y()>=top&&event.y()<y(391.5);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){if(dragging<0)return super.mouseDragged(event,dx,dy);dragScrollbar(event.y());return true;}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){if(dragging<0)return super.mouseReleased(event);dragging=-1;return true;}
    @Override public void onClose() {submit("");dismiss();}
}
