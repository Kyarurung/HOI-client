package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.ProtocolDialogView;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.Identifier;

final class ScreenCommanderConfirmation extends ScreenDialog {
    private int left,top,pane,panelHeight,scroll,offsetX,offsetY;
    private boolean pending,dragging,moving;
    private net.minecraft.client.gui.screens.Screen parent;
    void backdrop(net.minecraft.client.gui.screens.Screen value){parent=value;}
    net.minecraft.client.gui.screens.Screen backdrop(){return parent;}
    @Override public void tick(){if(parent instanceof ScreenAgency agency)agency.tick();}
    private List<net.minecraft.util.FormattedCharSequence> lines=List.of();
    private final Map<String,UiMenuButton> buttons=new HashMap<>();
    ScreenCommanderConfirmation(ProtocolDialogView view){super(view);}
    ScreenCommanderConfirmation(ProtocolDialogView view,BiConsumer<ProtocolDialogView,String> send){super(view,send);}
    static boolean division(ProtocolDialogView view){return view.image().isEmpty()&&view.tiles().isEmpty()&&List.of("CONFIRMDELETEUNIT","CONFIRMDELETEUNITS","CONFIRMCHANGE_UNIT_TEMPLATE").stream().anyMatch(key->view.title().equals(UiCommandLabels.text(key,"")));}
    static boolean matches(ProtocolDialogView view){return view.kind()==ProtocolDialogView.Kind.COMMANDER&&(division(view)||Set.of("commander/advisor-confirm","commander/advisor-promote").contains(view.image())||view.tiles().size()==1&&view.tiles().getFirst().section().equals("promote")&&view.choices().stream().anyMatch(c->c.id().equals("cancel")));}
    AbstractWidget choiceButton(String id){return buttons.get(id);}
    @Override net.minecraft.network.chat.Component bodyText(){return UiKeywordIcons.decorateLines(UiTooltips.sourceSpacing(parent instanceof ScreenAgency?UiTooltips.officerText(view().body(),false):confirmationText(view().body())),false);}
    private net.minecraft.network.chat.Component confirmationText(String text){
        var result=net.minecraft.network.chat.Component.empty();
        UiTooltips.officerText(text,false).visit((style,part)->{result.append(net.minecraft.network.chat.Component.literal(part).withStyle(style.getColor()!=null&&style.getColor().getValue()==UiTooltips.BAD?style.withColor(division(view())?0xE8343F:0xC04040):style));return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        return result;
    }
    @Override void update(ProtocolDialogView next){if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;pending=false;super.update(next);}
    @Override protected void init(){
        if(division(view())&&parent==null){parent=new ScreenMapCursor();parent.init(width,height);}
        boolean agency=parent instanceof ScreenAgency;
        if(agency&&((ScreenAgency)parent).dismissalBackdrop()!=parent)((ScreenAgency)parent).dismissalBackdrop().init(width,height);
        buttons.clear();pane=Math.min(agency?250:233,width-16);lines=font.split(bodyText(),pane-(agency?50:40));panelHeight=Math.min(height-16,agency?130:154);left=Math.clamp((width-pane)/2+offsetX,0,width-pane);top=Math.clamp((height-panelHeight)/2+offsetY,0,height-panelHeight);
        scroll=Math.clamp(scroll,0,Math.max(0,lines.size()*10-bodyHeight()));
        for(var choice:view().choices()){
            boolean cancel=choice.id().equals("cancel");var button=new UiMenuButton(cancel?choice.label():"OK",left+(cancel?agency?45:43:agency?130:128),top+panelHeight-(agency?28:30),agency?74:70,agency?17:13,()->choose(choice.id())).background("commander/commands/button_148x34/frame_0").textShadow(true).sound(cancel?"ui.close":"ui.click");
            button.caption("");button.active=!pending;button.setTooltip(UiTooltips.createOfficer(choice.tooltip()));buttons.put(choice.id(),button);addRenderableWidget(button);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(division(view())&&!(parent instanceof ScreenDivisionTemplates)){dev.hoi.client.map.MapCommanderHud.drawBackdrop(g);g.nextStratum();}
        if(parent!=null){var background=parent instanceof ScreenAgency agency?agency.dismissalBackdrop():parent;background.extractRenderState(g,-1,-1,delta);if(background instanceof ScreenMapCursor&&parent instanceof ScreenAgency)dev.hoi.client.map.MapAgencyHud.drawBackdrop(g);g.nextStratum();}
        if(parent instanceof ScreenAgency){
            UiAssets.nineSlice(g,"occupation/header_frame",left,top,pane,panelHeight,5,.5);
            UiAssets.draw(g,"completion/header",left+1,top+4,pane-2,43);
            UiAssets.draw(g,"completion/research",left+7,top+38,pane-13,61);
            UiAssets.draw(g,"completion/bottom",left+2,top+panelHeight-35,pane-4,36);
            UiText.centered(g,font,UiTooltips.officerText(view().title(),true),left+36,top+14,175,12,UiMenuStyle.TEXT,false);
            g.enableScissor(left+12,top+38,left+pane-12,top+99);
            int bodyY=top+60-scroll;
            for(var line:lines){g.text(font,line,left+(pane-font.width(line))/2,bodyY,UiMenuStyle.TEXT,false);bodyY+=10;}
            g.disableScissor();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable widget)widget.extractRenderState(g,mx,my,delta);
            for(var button:buttons.values())UiText.centered(g,font,button.getMessage(),button.getX(),button.getY(),button.getWidth(),button.getHeight(),button.active?UiMenuStyle.TEXT:UiMenuStyle.MUTED,false);
            if(lines.size()*10>61)scrollbar().draw(g,scroll);
            return;
        }
        var texture=Identifier.parse("hoi:textures/gui/mio/ui/gfx_tiled_window_pol_goal/frame_0.png");int[] xs={left,left+90,left+pane-90,left+pane},ys={top,top+43,top+panelHeight-43,top+panelHeight};float[] us={0,180f/382,202f/382,1},vs={0,85f/382,297f/382,1};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)g.blit(texture,xs[col],ys[row],xs[col+1],ys[row+1],us[col],us[col+1],vs[row],vs[row+1]);
        g.pose().pushMatrix();g.pose().translate(left+pane/2f,top+12);g.pose().scale(1.25f);g.centeredText(font,dev.hoi.client.ui.UiTooltips.officerText(view().title(),true),0,0,0xFFFFFFFF);g.pose().popMatrix();
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
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x<left||x>=left+pane||y<top||y>=top+panelHeight)return false;scroll=Math.clamp(scroll-(int)(vertical*33),0,Math.max(0,lines.size()*10-bodyHeight()));return true;}
    private int bodyHeight(){return parent instanceof ScreenAgency?61:panelHeight-89;}
    private UiScrollBar scrollbar(){return new UiScrollBar(left+pane-13,top+37,top+panelHeight-36,bodyHeight(),lines.size()*10);}
    private void dragBody(double y){scroll=scrollbar().offset(y);}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e,boolean twice){if(e.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT){if(parent instanceof ScreenAgency&&e.x()>=left&&e.x()<left+pane&&e.y()>=top&&e.y()<top+35){moving=true;return true;}if(scrollbar().contains(e.x(),e.y())){if(scrollbar().arrow(e.y()))scroll=scrollbar().step(e.y(),scroll,22);else{dragging=true;dragBody(e.y());}return true;}}return super.mouseClicked(e,twice);}
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent e,double dx,double dy){if(moving){offsetX+=(int)Math.round(dx);offsetY+=(int)Math.round(dy);rebuildWidgets();return true;}if(dragging){dragBody(e.y());return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent e){if(dragging||moving){dragging=false;moving=false;return true;}return super.mouseReleased(e);}
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN||event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER){view().choices().stream().filter(c->!c.id().equals("cancel")).findFirst().ifPresent(c->choose(c.id()));return true;}return super.keyPressed(event);}
}
