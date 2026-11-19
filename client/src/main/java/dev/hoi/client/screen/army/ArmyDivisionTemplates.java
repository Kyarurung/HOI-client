package dev.hoi.client.screen.army;

import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.client.screen.dialog.DialogScreen;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;

public final class ArmyDivisionTemplates extends DialogScreen {
    private int left,top,pane,rowsVisible,scroll;
    private boolean pending,dragging;
    private final MapCursor backdrop=new MapCursor();
    private final Map<String,UiMenuButton> buttons=new LinkedHashMap<>();
    public ArmyDivisionTemplates(DialogView view){super(view);}
    ArmyDivisionTemplates(DialogView view,BiConsumer<DialogView,String> send){super(view,send);}
    public static boolean matches(DialogView view){return view.kind()==DialogView.Kind.COMMANDER&&!view.tiles().isEmpty()&&view.tiles().stream().allMatch(t->t.section().startsWith("template/"));}
    AbstractWidget choiceButton(String id){return buttons.get(id);}
    @Override public void update(DialogView next){if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;pending=false;super.update(next);}
    @Override protected void init(){
        backdrop.init(width,height);
        left=18;top=Math.min(height-65,HudMenuBar.height(width)+90);pane=108;rowsVisible=Math.max(1,Math.min(5,(height-top-35)/25));scroll=Math.clamp(scroll,0,Math.max(0,view().tiles().size()-rowsVisible));buttons.clear();
        for(int i=scroll;i<Math.min(view().tiles().size(),scroll+rowsVisible);i++){
            var tile=view().tiles().get(i);var button=new UiMenuButton(tile.name(),left+3,top+4+(i-scroll)*25,pane-12,25,()->choose(tile.section())).caption("").background("none");
            button.active=!pending&&view().choices().stream().anyMatch(c->c.id().equals(tile.section()));button.setTooltip(null);buttons.put(tile.section(),button);addRenderableWidget(button);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        backdrop.extractRenderState(g,-1,-1,delta);CommanderHud.drawBackdrop(g);g.nextStratum();
        UiAssets.nineSlice(g,"commander/commands/tiled_window_1b_thin_border/frame_0",left,top,pane,rowsVisible*25+8,4,.5);
        for(var tile:view().tiles()){
            var b=buttons.get(tile.section());if(b==null)continue;
            UiAssets.nineSlice(g,"commander/commands/mini_tooltip/frame_0",b.getX(),b.getY(),b.getWidth(),25,6,.5);
            UiAssets.drawContent(g,dev.hoi.client.map.commander.CommanderPanels.armyUnitIcon(tile.icon()),b.getX()+1,b.getY()+6,33,13);
            var lines=font.split(Tooltips.officerText(tile.name()),b.getWidth()-35);int shown=Math.min(2,lines.size());for(int i=0;i<shown;i++){var line=lines.get(i);if(i==1&&lines.size()>2){var first=new StringBuilder();lines.getFirst().accept((index,style,code)->{first.appendCodePoint(code);return true;});String full=Tooltips.officerText(tile.name()).getString();line=net.minecraft.network.chat.Component.literal(full.substring(Math.min(full.length(),first.length())).strip()).getVisualOrderText();}UiText.marquee(g,font,line,b.getX()+35,b.getY()+(25-shown*10)/2+i*10,b.getWidth()-35,1,b.active?UiMenuStyle.TEXT:0xFFACAAAD,false,-1);}
        }
        scrollbar().draw(g,scroll);
        for(var b:buttons.values())b.extractRenderState(g,mx,my,delta);

        for(var tile:view().tiles()){var b=buttons.get(tile.section());if(b!=null&&Tooltips.contains(b,mx,my))Tooltips.drawSourceScrollable(g,font,tile.detail(),left+pane-42,b.getY()-14,width,height,0);}
    }
    @Override protected void choose(String id){if(pending||view().choices().stream().noneMatch(c->c.id().equals(id)))return;pending=true;buttons.values().forEach(b->b.active=false);super.choose(id);}
    @Override public void onClose(){choose("cancel");}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x<left||x>=left+pane||y<top||y>=top+rowsVisible*25+8)return false;scroll=Math.clamp(scroll-(int)vertical,0,Math.max(0,view().tiles().size()-rowsVisible));rebuildWidgets();return true;}
    private UiScrollBar scrollbar(){return new UiScrollBar(left+pane-11,top+3,top+rowsVisible*25+5,rowsVisible,view().tiles().size());}
    private void drag(double y){scroll=scrollbar().offset(y);rebuildWidgets();}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&scrollbar().contains(event.x(),event.y())){if(scrollbar().arrow(event.y())){scroll=scrollbar().step(event.y(),scroll,1);rebuildWidgets();}else{dragging=true;drag(event.y());}return true;}return super.mouseClicked(event,twice);}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(dragging){drag(event.y());return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(dragging){dragging=false;return true;}return super.mouseReleased(event);}
}
