package dev.hoi.client.screen;

import dev.hoi.client.map.MapCommanderHud;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.ProtocolDialogView;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;

final class ScreenDivisionTemplates extends ScreenDialog {
    private int left,top,pane,rowsVisible,scroll;
    private boolean pending,dragging;
    private final ScreenMapCursor backdrop=new ScreenMapCursor();
    private final Map<String,UiMenuButton> buttons=new LinkedHashMap<>();
    ScreenDivisionTemplates(ProtocolDialogView view){super(view);}
    ScreenDivisionTemplates(ProtocolDialogView view,BiConsumer<ProtocolDialogView,String> send){super(view,send);}
    static boolean matches(ProtocolDialogView view){return view.kind()==ProtocolDialogView.Kind.COMMANDER&&!view.tiles().isEmpty()&&view.tiles().stream().allMatch(t->t.section().startsWith("template/"));}
    AbstractWidget choiceButton(String id){return buttons.get(id);}
    @Override void update(ProtocolDialogView next){if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;pending=false;super.update(next);}
    @Override protected void init(){
        backdrop.init(width,height);
        left=18;top=Math.min(height-65,UiMenuBar.height(width)+90);pane=108;rowsVisible=Math.max(1,Math.min(5,(height-top-35)/25));scroll=Math.clamp(scroll,0,Math.max(0,view().tiles().size()-rowsVisible));buttons.clear();
        for(int i=scroll;i<Math.min(view().tiles().size(),scroll+rowsVisible);i++){
            var tile=view().tiles().get(i);var button=new UiMenuButton(tile.name(),left+3,top+4+(i-scroll)*25,pane-12,25,()->choose(tile.section())).caption("").background("none");
            button.active=!pending&&view().choices().stream().anyMatch(c->c.id().equals(tile.section()));button.setTooltip(null);buttons.put(tile.section(),button);addRenderableWidget(button);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        backdrop.extractRenderState(g,-1,-1,delta);MapCommanderHud.drawBackdrop(g);g.nextStratum();
        UiAssets.nineSlice(g,"commander/commands/tiled_window_1b_thin_border/frame_0",left,top,pane,rowsVisible*25+8,4,.5);
        for(var tile:view().tiles()){
            var b=buttons.get(tile.section());if(b==null)continue;
            UiAssets.nineSlice(g,"commander/commands/mini_tooltip/frame_0",b.getX(),b.getY(),b.getWidth(),25,6,.5);
            UiAssets.drawContent(g,dev.hoi.client.map.MapCommanderPanels.armyUnitIcon(tile.icon()),b.getX()+1,b.getY()+6,33,13);
            var lines=font.split(UiTooltips.officerText(tile.name()),b.getWidth()-35);int shown=Math.min(2,lines.size());for(int i=0;i<shown;i++){var line=lines.get(i);if(i==1&&lines.size()>2){var text=new StringBuilder();line.accept((index,style,code)->{text.appendCodePoint(code);return true;});line=net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(text.toString(),Math.max(1,b.getWidth()-35-font.width("…")))+"…").getVisualOrderText();}g.text(font,line,b.getX()+35,b.getY()+(25-shown*10)/2+i*10,b.active?UiMenuStyle.TEXT:0xFFACAAAD,false);}
        }
        if(view().tiles().size()>rowsVisible){int h=rowsVisible*25,thumb=Math.max(10,h*rowsVisible/view().tiles().size()),y=top+4+scroll*(h-thumb)/(view().tiles().size()-rowsVisible);g.fill(left+pane-7,top+4,left+pane-3,top+4+h,0xFF282735);g.fill(left+pane-7,y,left+pane-3,y+thumb,0xFFAAA3B7);}
        for(var b:buttons.values())b.extractRenderState(g,mx,my,delta);
        UiText.centered(g,font,"▲",left+pane-9,top-2,8,9,UiMenuStyle.TEXT,false);UiText.centered(g,font,"▼",left+pane-9,top+rowsVisible*25,8,9,UiMenuStyle.TEXT,false);
        for(var tile:view().tiles()){var b=buttons.get(tile.section());if(b!=null&&UiTooltips.contains(b,mx,my))UiTooltips.drawSourceScrollable(g,font,tile.detail(),left+pane-42,b.getY()-14,width,height,0,184);}
    }
    @Override protected void choose(String id){if(pending||view().choices().stream().noneMatch(c->c.id().equals(id)))return;pending=true;buttons.values().forEach(b->b.active=false);super.choose(id);}
    @Override public void onClose(){choose("cancel");}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x<left||x>=left+pane||y<top||y>=top+rowsVisible*25+8)return false;scroll=Math.clamp(scroll-(int)vertical,0,Math.max(0,view().tiles().size()-rowsVisible));rebuildWidgets();return true;}
    private void drag(double y){int h=rowsVisible*25,thumb=Math.max(10,h*rowsVisible/view().tiles().size());scroll=(int)Math.round(Math.clamp((y-top-4-thumb/2.)/Math.max(1,h-thumb),0,1)*Math.max(0,view().tiles().size()-rowsVisible));rebuildWidgets();}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&event.x()>=left+pane-8&&event.x()<left+pane&&event.y()>=top&&event.y()<top+rowsVisible*25+8&&view().tiles().size()>rowsVisible){if(event.y()<top+6){scroll=Math.max(0,scroll-1);rebuildWidgets();}else if(event.y()>=top+rowsVisible*25){scroll=Math.min(view().tiles().size()-rowsVisible,scroll+1);rebuildWidgets();}else{dragging=true;drag(event.y());}return true;}return super.mouseClicked(event,twice);}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(dragging){drag(event.y());return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(dragging){dragging=false;return true;}return super.mouseReleased(event);}
}
