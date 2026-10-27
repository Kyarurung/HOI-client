package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.ProtocolDialogView;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.Identifier;

final class ScreenCommanderConfirmation extends ScreenDialog {
    private int left,top,pane,panelHeight,scroll;
    private boolean pending,dragging;
    private net.minecraft.client.gui.screens.Screen parent;
    void backdrop(net.minecraft.client.gui.screens.Screen value){parent=value;}
    net.minecraft.client.gui.screens.Screen backdrop(){return parent;}
    private List<net.minecraft.util.FormattedCharSequence> lines=List.of();
    private final Map<String,UiMenuButton> buttons=new HashMap<>();
    ScreenCommanderConfirmation(ProtocolDialogView view){super(view);}
    ScreenCommanderConfirmation(ProtocolDialogView view,BiConsumer<ProtocolDialogView,String> send){super(view,send);}
    static boolean division(ProtocolDialogView view){return view.image().isEmpty()&&view.tiles().isEmpty()&&List.of("CONFIRMDELETEUNIT","CONFIRMDELETEUNITS","CONFIRMCHANGE_UNIT_TEMPLATE").stream().anyMatch(key->view.title().equals(UiCommandLabels.text(key,"")));}
    static boolean matches(ProtocolDialogView view){return view.kind()==ProtocolDialogView.Kind.COMMANDER&&(division(view)||Set.of("commander/advisor-confirm","commander/advisor-promote").contains(view.image())||view.tiles().size()==1&&view.tiles().getFirst().section().equals("promote")&&view.choices().stream().anyMatch(c->c.id().equals("cancel")));}
    AbstractWidget choiceButton(String id){return buttons.get(id);}
    @Override net.minecraft.network.chat.Component bodyText(){return UiKeywordIcons.decorateLines(UiTooltips.sourceSpacing(confirmationText(view().body())),false);}
    private net.minecraft.network.chat.Component confirmationText(String text){
        var result=net.minecraft.network.chat.Component.empty();
        UiTooltips.officerText(text,false).visit((style,part)->{result.append(net.minecraft.network.chat.Component.literal(part).withStyle(style.getColor()!=null&&style.getColor().getValue()==UiTooltips.BAD?style.withColor(division(view())?0xE8343F:0xC04040):style));return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        return result;
    }
    @Override void update(ProtocolDialogView next){if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;pending=false;super.update(next);}
    @Override protected void init(){
        if(division(view())&&parent==null){parent=new ScreenMapCursor();parent.init(width,height);}
        buttons.clear();pane=Math.min(233,width-16);lines=font.split(bodyText(),pane-40);panelHeight=Math.min(height-16,154);left=(width-pane)/2;top=(height-panelHeight)/2;
        scroll=Math.clamp(scroll,0,Math.max(0,lines.size()*10-(panelHeight-89)));
        for(var choice:view().choices()){
            boolean cancel=choice.id().equals("cancel");var button=new UiMenuButton(cancel?choice.label():"OK",left+(cancel?43:128),top+panelHeight-30,70,13,()->choose(choice.id())).background("commander/commands/button_148x34/frame_0").textShadow(true).sound(cancel?"ui.close":"ui.click");
            button.caption("");button.active=!pending;button.setTooltip(UiTooltips.createOfficer(choice.tooltip()));buttons.put(choice.id(),button);addRenderableWidget(button);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(division(view())&&!(parent instanceof ScreenDivisionTemplates)){dev.hoi.client.map.MapCommanderHud.drawBackdrop(g);g.nextStratum();}
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}

        var texture=Identifier.parse("hoi:textures/gui/mio/ui/gfx_tiled_window_pol_goal/frame_0.png");int[] xs={left,left+90,left+pane-90,left+pane},ys={top,top+43,top+panelHeight-43,top+panelHeight};float[] us={0,180f/382,202f/382,1},vs={0,85f/382,297f/382,1};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)g.blit(texture,xs[col],ys[row],xs[col+1],ys[row+1],us[col],us[col+1],vs[row],vs[row+1]);
        g.pose().pushMatrix();g.pose().translate(left+pane/2f,top+12);g.pose().scale(1.25f);g.centeredText(font,view().title(),0,0,0xFFFFFFFF);g.pose().popMatrix();
        g.enableScissor(left+12,top+42,left+pane-12,top+panelHeight-44);
        int y=top+45-scroll;
        for(var line:lines){g.text(font,line,left+(pane-font.width(line))/2,y,UiMenuStyle.TEXT,false);y+=10;}g.disableScissor();
        if(division(view())||lines.size()*10>panelHeight-89){
            scrollbar().draw(g,scroll);
        }
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable widget)widget.extractRenderState(g,mx,my,delta);
        for(var button:buttons.values()){var label=button.getMessage().copy().withStyle(net.minecraft.ChatFormatting.BOLD);g.text(font,label,button.getX()+(button.getWidth()-font.width(label))/2,button.getY()+(button.getHeight()-font.lineHeight)/2,button.active?0xFFFFFFFF:UiMenuStyle.MUTED,true);}
    }
    @Override protected void choose(String id){if(pending||view().choices().stream().noneMatch(c->c.id().equals(id)))return;pending=true;buttons.values().forEach(b->b.active=false);super.choose(id);}
    @Override public void onClose(){choose("cancel");}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x<left||x>=left+pane||y<top||y>=top+panelHeight)return false;scroll=Math.clamp(scroll-(int)(vertical*33),0,Math.max(0,lines.size()*10-(panelHeight-89)));return true;}
    private UiScrollBar scrollbar(){return new UiScrollBar(left+pane-13,top+37,top+panelHeight-36,panelHeight-89,lines.size()*10);}
    private void dragBody(double y){scroll=scrollbar().offset(y);}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e,boolean twice){if(e.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&scrollbar().contains(e.x(),e.y())){if(scrollbar().arrow(e.y()))scroll=scrollbar().step(e.y(),scroll,22);else{dragging=true;dragBody(e.y());}return true;}return super.mouseClicked(e,twice);}
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent e,double dx,double dy){if(dragging){dragBody(e.y());return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent e){if(dragging){dragging=false;return true;}return super.mouseReleased(e);}
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN||event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER){view().choices().stream().filter(c->!c.id().equals("cancel")).findFirst().ifPresent(c->choose(c.id()));return true;}return super.keyPressed(event);}
}
