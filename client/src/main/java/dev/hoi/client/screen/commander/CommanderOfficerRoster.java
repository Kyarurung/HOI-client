package dev.hoi.client.screen.commander;

import dev.hoi.client.ui.*;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.dialog.DialogScreen;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class CommanderOfficerRoster extends DialogScreen {
    private final Screen parent;
    private final Map<String,AbstractWidget> buttons=new LinkedHashMap<>();
    private List<DialogView.Tile> rows=List.of();
    private int left,top,pane,bottom,scroll,sort;
    private boolean pending,descending=true,dragging;
    private String query="";
    private EditBox search;
    public CommanderOfficerRoster(DialogView view,Screen parent){super(view);this.parent=parent;}
    CommanderOfficerRoster(DialogView view,Screen parent,BiConsumer<DialogView,String> sender){super(view,sender);this.parent=parent;}
    public static boolean matches(DialogView view){return view.kind()==DialogView.Kind.COMMANDER&&Set.of("navy/captains","commander/field_officers").contains(view.image())&&!CommanderConfirmation.matches(view);}
    public Screen backdrop(){return parent;}
    private void label(GuiGraphicsExtractor g,String value,int x,int y,int width){dev.hoi.client.ui.UiText.marquee(g,font,Tooltips.officerText(value),x,y,width,1,UiMenuStyle.TEXT,false,-1);}
    AbstractWidget choiceButton(String id){return buttons.get(id);}
    public void dismiss(){DialogCoordinator.setContent(parent);}
    @Override public void update(DialogView next){if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;pending=false;super.update(next);}
    private UiScrollBar scrollbar(){return new UiScrollBar(left+pane-11,top+37,bottom-25,bottom-top-62,rows.size()*49);}
    private int maximumScroll(){return scrollbar().maximum();}
    private void sorted(int column){descending=sort==column?!descending:column!=1;sort=column;scroll=0;rebuildWidgets();}
    @Override protected void init(){
        pane=Math.min(275,width-12);left=Math.min(UiPanelLayout.width(MenuTab.OFFICER_CORPS,width)+4,width-pane-6);top=HudMenuBar.height(width);bottom=Math.min(height-8,top+295);buttons.clear();
        addRenderableWidget(new UiMenuButton("×",left+pane-19,top+2,15,14,this::onClose));
        String[] keys={"DIVISION_COMMANDER_EXPERIENCE_SORT","DIVISION_COMMANDER_TYPE_SORT","DIVISION_COMMANDER_MEDALS_SORT","DIVISION_COMMANDER_TOTAL_EXPERIENCE_SORT"};
        int[] xs={7,48,167,219},ws={41,41,52,48};
        for(int i=0;i<4;i++){int index=i;addRenderableWidget(new UiMenuButton(UiCommandLabels.text(keys[i],""),left+xs[i],top+20,ws[i],14,()->sorted(index)).caption(i==0?"":UiCommandLabels.text(keys[i],"")).background("mio/ui/gfx_sort_up_down_button_83x29/frame_0").sound("ui.scroll"));}
        search=new EditBox(font,left+99,top+22,64,11,Component.literal("검색"));search.setMaxLength(128);search.setBordered(false);search.setHint(Component.literal("검색"));search.setValue(query);search.setResponder(v->{query=v;scroll=0;rebuildWidgets();setFocused(search);});addRenderableWidget(search);
        Comparator<DialogView.Tile> order=switch(sort){case 1->Comparator.comparing(t->t.officer().type());case 2->Comparator.comparingInt(t->t.officer().medals());case 3->Comparator.comparingDouble(t->t.officer().experience());default->Comparator.comparingDouble(t->t.officer().unitExperience());};
        if(descending)order=order.reversed();String lower=query.toLowerCase(Locale.ROOT);
        rows=view().tiles().stream().filter(t->t.officer()!=null&&(t.name()+" "+t.officer().unit()).toLowerCase(Locale.ROOT).contains(lower)).sorted(order).toList();scroll=Math.clamp(scroll,0,maximumScroll());
        for(int i=0;i<rows.size();i++){
            var tile=rows.get(i);int y=top+38+i*49-scroll;if(y>=bottom-25)break;if(y+49<=top+38)continue;
            button("details/"+tile.section(),UiCommandLabels.text("DIVISION_DETAILS","세부 사항"),UiCommandLabels.text("OPEN_LEADER_DETAILS",""),left+165,y+28,48,false);
            button("promote/"+tile.section(),UiCommandLabels.text("UNIT_LEADER_PROMOTE","진급"),tile.officer().promotion(),left+213,y+28,48,true);
        }
        int pageX=left+6;
        for(var choice:view().choices())if(choice.id().startsWith("page/")){button(choice.id(),choice.label(),choice.tooltip(),pageX,bottom-14,62,true);pageX+=65;}
        if(pending)for(var child:children())if(child instanceof AbstractWidget widget)widget.active=false;
    }
    private void button(String id,String title,String tooltip,int x,int y,int w,boolean supported){
        var button=new UiMenuButton(title,x,y,w,14,()->choose(id)).background("none");
        button.setTooltip(null);button.textScale(.9f);button.textShadow(false);button.active=supported&&!pending&&(id.startsWith("page/")||y>=top+38&&y+14<=bottom-25)&&view().choices().stream().anyMatch(c->c.id().equals(id));buttons.put(id,button);addRenderableWidget(button);
    }
    @Override protected void choose(String id){if(pending||!id.isEmpty()&&view().choices().stream().noneMatch(c->c.id().equals(id)))return;pending=true;for(var child:children())if(child instanceof AbstractWidget widget)widget.active=false;super.choose(id);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        UiAssets.draw(g,"commander/officers/gfx_leader_selection_bg/frame_0",left,top,pane,bottom-top);
        UiText.leftSingleLine(g,font,Tooltips.officerText(view().title(),true),left+10,top+4,pane-33,14,UiMenuStyle.TEXT);
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable renderable&&(!buttons.containsValue(child)||((AbstractWidget)child).getY()>=bottom-14))renderable.extractRenderState(g,mx,my,delta);
        UiAssets.draw(g,"commander/commands/unit_level/frame_0",left+20,top+20,14,14);
        int[] sortX={7,48,167,219};UiText.centered(g,font,descending?"▲":"▼",left+sortX[sort]+30,top+21,8,12,0xFFFFD060,false);
        g.enableScissor(left+5,top+38,left+pane-12,bottom-25);
        String tooltip="";
        for(int i=0;i<rows.size();i++){
            var tile=rows.get(i);var officer=tile.officer();int y=top+38+i*49-scroll;if(y>=bottom-25)break;if(y+49<=top+38)continue;
            UiAssets.draw(g,"commander/officers/gfx_leader_selection_entry_bg/frame_0",left+6,y,255,49);
            String unitIcon=officer.unitIcon().equals("officer/ship_unknown")?"commander/officers/gfx_navalcombat_ship_icon_unknown/frame_0":dev.hoi.client.map.commander.CommanderPanels.armyUnitIcon(officer.unitIcon());
            UiAssets.drawContent(g,unitIcon,left+3,y+2,33,27);
            UiAssets.draw(g,"commander/commands/unit_level/frame_"+officer.unitLevel(),left+13,y+30,14,14);
            UiAssets.draw(g,tile.icon(),left+34,y+3,33,44);
            UiAssets.draw(g,"commander/officers/gfx_clip/frame_0",left+33,y,9,5);
            label(g,officer.title()+" "+tile.name(),left+70,y+4,122);
            label(g,officer.unit(),left+70,y+14,122);
            if(officer.insignia()!=null)UiAssets.draw(g,"commander/officers/gfx_theatre_army_shield/frame_"+officer.insignia(),left+203,y+8,14,14);
            UiText.centered(g,font,String.format(Locale.ROOT,"%.0f",officer.experience()),left+222,y+7,33,14,UiMenuStyle.TEXT,false);
            if(mx>=left+6&&mx<left+261&&my>=y&&my<y+49)tooltip=tile.detail();
            if(officer.insignia()!=null&&mx>=left+203&&mx<left+217&&my>=y+8&&my<y+22)tooltip=officer.type();
            if(mx>=left+220&&mx<left+261&&my>=y+5&&my<y+25)tooltip=officer.experienceTooltip();
            for(String prefix:List.of("details/","promote/")){
                var b=buttons.get(prefix+tile.section());if(b.active)UiAssets.draw(g,"commander/commands/leader_selection_button/frame_0",b.getX(),b.getY(),b.getWidth(),b.getHeight());else UiAssets.drawGrayscale(g,"commander/commands/leader_selection_button/frame_0",b.getX(),b.getY(),b.getWidth(),b.getHeight());b.extractRenderState(g,mx,my,delta);
                if(Tooltips.contains(b,mx,my))tooltip=prefix.equals("promote/")?officer.promotion():UiCommandLabels.text("OPEN_LEADER_DETAILS","");
            }
        }
        g.disableScissor();
        scrollbar().draw(g,scroll);
        if(my>=top+38&&my<bottom-25&&!tooltip.isEmpty())Tooltips.drawSourceScrollable(g,font,tooltip,mx,my,width,height,0);
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(x<left||x>=left+pane)return false;scroll=Math.clamp(scroll-(int)(v*49),0,maximumScroll());rebuildWidgets();return true;}
    private void drag(double y){scroll=scrollbar().offset(y);rebuildWidgets();}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&scrollbar().contains(event.x(),event.y())){
            if(scrollbar().arrow(event.y())){scroll=scrollbar().step(event.y(),scroll,49);rebuildWidgets();}else{dragging=true;drag(event.y());}return true;
        }
        if(event.y()>=top+34&&event.y()<top+38||event.y()>=bottom-25&&event.y()<bottom-15)return true;
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(dragging){drag(event.y());return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(dragging){dragging=false;return true;}return super.mouseReleased(event);}
    @Override public void onClose(){choose("");dismiss();}
}
