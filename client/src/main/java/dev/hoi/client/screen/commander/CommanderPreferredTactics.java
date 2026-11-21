package dev.hoi.client.screen.commander;

import dev.hoi.client.ui.*;
import dev.hoi.client.screen.dialog.DialogScreen;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;
import java.util.function.Consumer;

public final class CommanderPreferredTactics extends DialogScreen {
    private final Screen parent;
    private final Consumer<DialogPayloads.Action> sender;
    private final Map<String,AbstractWidget> buttons=new HashMap<>();
    private UiUnits units;
    private int left,top,scroll,filter,tooltipScroll,rowsTop;
    private boolean dragging;
    private boolean pending;
    private int bodyLineCount;
    private float bodyTextScale;
    int bodyLineCount() {return bodyLineCount;}
    float bodyTextScale() {return bodyTextScale;}
    int bodyAreaHeight() {return rowsTop-y(33)-3;}
    private String hovered="";
    private Tooltips.ScrolledTooltip tooltipBounds;
    public Tooltips.ScrolledTooltip tooltipBounds() {return tooltipBounds;}
    public CommanderPreferredTactics(DialogView view,Screen parent) {this(view,parent,null);}
    public CommanderPreferredTactics(DialogView view,Screen parent,Consumer<DialogPayloads.Action> sender) {super(view);this.parent=parent;this.sender=sender;}
    public Screen backdrop() {return parent;}
    public void dismiss() {minecraft.gui.setScreen(parent);}
    public AbstractWidget choiceButton(String id) {return buttons.get(id);}
    private int x(double value) {return left+Math.round(units.length(value));}
    private int y(double value) {return top+Math.round(units.length(value));}
    private int size(double value) {return units.size(value);}
    private List<DialogView.Tile> rows() {return view().tiles().stream().filter(t->t.tactic()!=null&&(filter==0||t.tactic().attacking()==(filter==1))).toList();}
    private int rowY(int index) {return rowsTop+index*size(47)-scroll;}
    @Override public void update(DialogView next) {
        if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;
        pending=false;super.update(next);
    }
    private void submit(String id) {
        if(pending||view().token().isEmpty()||sender==null&&!ClientPlayNetworking.canSend(DialogPayloads.Action.TYPE))return;
        pending=true;buttons.values().forEach(button->button.active=false);
        var action=new DialogPayloads.Action(view().token(),view().revision(),id);
        if(sender!=null)sender.accept(action);else ClientPlayNetworking.send(action);
    }
    @Override protected void init() {
        units=new UiUnits(width,height,Math.min(1f,Math.max(.1f,(height-HudMenuBar.height(width)-6)/306.5f)));left=UiPanelLayout.width(MenuTab.OFFICER_CORPS,width);top=HudMenuBar.height(width);
        if(left+size(196)>width-size(3))left=Math.max(size(3),width-size(199));
        if(top+size(306.5)>height-size(3))top=Math.max(size(3),height-size(309.5));
        bodyLineCount=Tooltips.split(font,TooltipKeywordIcons.decorateLines(Tooltips.officerText(view().body()),false),size(174)).size();rowsTop=Math.max(y(62.5),y(33)+bodyLineCount*(font.lineHeight+1)+3);
        buttons.clear();tooltipBounds=null;addRenderableWidget(UiMenuStyle.close("raids/gfx_closebutton_small/frame_0",x(180.5),y(2.5),size(13),size(13),this::onClose));
        for(int i=0;i<3;i++) {
            int next=i;var tab=new UiMenuButton(List.of("모두","공격","방어").get(i),x(7.5+52.5*i),y(17.5),size(50),size(14.5),()->{filter=next;scroll=0;rebuildWidgets();}).caption("");
            buttons.put("filter/"+i,tab);addRenderableWidget(tab);
        }
        var rows=rows();scroll=Math.clamp(scroll,0,Math.max(0,rows.size()*size(47)-(y(299.5)-rowsTop)));
        for(int i=0;i<rows.size();i++) {
            var row=rows.get(i);int py=rowY(i),minimum=rowsTop,maximum=y(299.5);
            if(py+size(47)<=minimum||py>=maximum)continue;
            var button=new UiMenuButton(row.name(),x(6.5),Math.max(minimum,py),size(175),Math.min(maximum,py+size(47))-Math.max(minimum,py),()->submit(row.section())).caption("");
            button.setTooltip(null);button.active=!pending&&view().choices().stream().anyMatch(choice->choice.id().equals(row.section()));
            buttons.put(row.section(),button);addRenderableWidget(button);
        }
    }
    private void text(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,int color) {
        var component=TooltipKeywordIcons.decorateLines(Tooltips.officerText(value),false);
        if(value.contains("\n"))UiText.left(g,font,component,px,py,w,h,color);
        else UiText.leftSingleLine(g,font,component,px,py,w,h,color);
    }
    private void title(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,int color) {
        UiText.centeredSingleLine(g,font,TooltipKeywordIcons.decorateLines(Tooltips.officerText(value),false),px,py,w,h,color);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        UiMenuStyle.panel(g,left,top,size(196),size(306.5));
        UiText.centeredSingleLine(g,font,Tooltips.officerText(view().title(),true),x(2),y(1),size(176),size(14),UiMenuStyle.TEXT);
        for(var child:children())if(child instanceof AbstractWidget button&&button.getMessage().getString().equals("×"))button.extractRenderState(g,mx,my,delta);
        for(int i=0;i<3;i++) {
            var button=buttons.get("filter/"+i);
            UiAssets.draw(g,"tactic/ui/filter/frame_"+(filter==i?0:1),button.getX(),button.getY(),button.getWidth(),button.getHeight());
            title(g,button.getMessage().getString(),button.getX()+size(9),button.getY(),button.getWidth()-size(18),button.getHeight(),UiMenuStyle.TEXT);
        }
        var body=TooltipKeywordIcons.decorateLines(Tooltips.officerText(view().body()),false);
        int bodyWidth=size(174);float bodyScale=1f;
        var bodyLines=Tooltips.split(font,body,bodyWidth);
        bodyLineCount=bodyLines.size();bodyTextScale=bodyScale;
        g.enableScissor(x(9),y(32),x(184),rowsTop-3);
        for(int i=0;i<bodyLineCount;i++)g.text(font,bodyLines.get(i),x(10),y(33)+i*(font.lineHeight+1),UiMenuStyle.TEXT,false);
        g.disableScissor();
        String tooltip="";var rows=rows();g.enableScissor(x(6.5),rowsTop,x(181.5),y(299.5));
        for(int i=0;i<rows.size();i++) {
            var row=rows.get(i);var tactic=row.tactic();int py=rowY(i);if(py+size(47)<=rowsTop||py>=y(299.5))continue;
            var button=buttons.get(row.section());boolean hover=button!=null&&Tooltips.contains(button,mx,my);
            int frame=2;
            UiAssets.draw(g,"tactic/ui/background/frame_"+frame,x(6.5),py,size(175),size(47));
            if(tactic.selected())g.fill(x(7),py+size(.5),x(181),py+size(46.5),0x303A8A42);
            text(g,"§H"+row.name()+"§!",x(13.5),py+size(1),size(tactic.phase().isEmpty()?155:77),size(13),UiMenuStyle.TEXT);
            if(!tactic.phase().isEmpty()){var phase=Tooltips.officerText(tactic.phase());g.text(font,phase,x(177)-font.width(phase),py+size(3),UiMenuStyle.TEXT,false);}
            UiAssets.drawContent(g,row.icon(),x(14),py+size(16.5),size(43),size(23));
            for(int n=0;n<tactic.effects().size();n++) {
                double px=switch(n){case 0,4->58;case 1->100;case 2->107;case 3->129;case 5->83;default->58;};
                int ey=py+size(n<2?16.5:30.5),icon=size(n<2?11:8),value=size(n<2?22:15);
                UiAssets.drawContent(g,"tactic/ui/"+List.of("attack","defend","width","speed","attacker_org","defender_org").get(n)+"/frame_0",x(px),ey,icon,size(11));
                text(g,tactic.effects().get(n),x(px)+icon+size(.5),ey,value,size(11),UiMenuStyle.TEXT);
            }
            if(!row.value().isEmpty()&&!row.value().equals("적용 중")) {
                UiAssets.drawContent(g,"hud/command_power",x(155),py+size(17.5),size(9),size(9));
                text(g,row.value(),x(143),py+size(16.5),size(11),size(11),UiMenuStyle.TEXT);
            }
            if(!tactic.unlocked())UiAssets.drawContent(g,"tactic/ui/locked/frame_0",x(154),py+size(31),size(8),size(11));
            if(hover)tooltip=tactic.unlocked()?"§H"+row.name()+"§!\n"+row.detail():row.detail();
        }
        g.disableScissor();
        scrollbar().draw(g,scroll);
        if(!tooltip.equals(hovered)){hovered=tooltip;tooltipScroll=0;}
        tooltipBounds=tooltip.isEmpty()?null:Tooltips.drawSourceScrollable(g,font,tooltip,mx,my,width,height,tooltipScroll);
        if(tooltipBounds!=null)tooltipScroll=tooltipBounds.scroll();
    }
    private UiScrollBar scrollbar(){return new UiScrollBar(x(183),rowsTop-size(8),y(307.5),y(299.5)-rowsTop,rows().size()*size(47));}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&scrollbar().contains(event.x(),event.y())){if(scrollbar().arrow(event.y()))scroll=scrollbar().step(event.y(),scroll,size(47));else{dragging=true;scroll=scrollbar().offset(event.y());}rebuildWidgets();return true;}return super.mouseClicked(event,twice);}
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){if(dragging){scroll=scrollbar().offset(event.y());rebuildWidgets();return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){if(dragging){dragging=false;return true;}return super.mouseReleased(event);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(!hovered.isEmpty()&&com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)){tooltipScroll-=(int)(vertical*size(15));return true;}
        if(mx<left||mx>x(196)||my<rowsTop||my>y(299.5))return false;
        scroll-=(int)(vertical*size(23.5));rebuildWidgets();return true;
    }
    @Override protected void choose(String choice) {submit(choice);}
    @Override public void onClose() {submit("");dismiss();}
}
