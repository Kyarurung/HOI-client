package dev.hoi.client.screen;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputMapInfo;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiMenuBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ScreenMapCursor extends Screen implements InputSidebarMovement.InputMovementScreen {
    private int hudScroll;
    private String country;
    private String dragDimension="";
    private boolean selecting,windowActive=true;
    private final ScreenLandPlanGesture commandGesture=new ScreenLandPlanGesture();
    private String commandContext="";
    private boolean commandLine;
    private int commandButton;
    private double startX,startY,endX,endY;
    private boolean dragged() {return Math.hypot(endX-startX,endY-startY)>=new dev.hoi.client.ui.UiUnits(width,height).size(3);}
    public ScreenMapCursor() { super(Component.literal("지도")); }
    @Override protected void init() {
        selecting=false;commandGesture.clear();commandContext="";
        country = ClientCampaignHud.country();
        UiMenuBar.buttons(width, country, null, HoiClient::openMenu).forEach(this::addRenderableWidget);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if(commandGesture.active()&&commandLine)commandGesture.draw(graphics);
        if(selecting&&dragged()) {
            int left=(int)Math.min(startX,endX),right=(int)Math.max(startX,endX);
            int top=(int)Math.min(startY,endY),bottom=(int)Math.max(startY,endY);
            graphics.fill(left,top,right,top+1,0xFFFFFFFF);
            graphics.fill(left,bottom-1,right,bottom,0xFFFFFFFF);
            graphics.fill(left,top,left+1,bottom,0xFFFFFFFF);
            graphics.fill(right-1,top,right,bottom,0xFFFFFFFF);
        }
        UiMenuBar.draw(graphics, width, ClientCampaignHud.hud(), mouseX, mouseY, hudScroll);
        for (var child : children()) if (child instanceof Renderable renderable)
            renderable.extractRenderState(graphics, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (super.mouseClicked(event, twice)) return true;
        if(event.y()<UiMenuBar.height(width)||minecraft.level==null)return false;
        if(dev.hoi.client.map.MapCommanderHud.clickAt(event.x(),event.y(),event.button(),twice))return true;
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT||event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&(dev.hoi.client.map.MapCommanderOrders.pointMode()||dev.hoi.client.map.MapCommanderOrders.clicksBorder()||dev.hoi.client.map.MapCommanderOrders.editsPoint())){
            if(!dev.hoi.client.map.MapCommanderOrders.ready())return false;
            commandButton=event.button();commandContext=dev.hoi.client.map.MapCommanderOrders.gestureContext();commandLine=dev.hoi.client.map.MapCommanderOrders.drawsPlanLine();
            return commandGesture.begin(minecraft,event.x(),event.y(),width,height);
        }
        if(event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return false;
        selecting=true;
        startX=endX=event.x();startY=endY=event.y();
        dragDimension=minecraft.level.dimension().identifier().toString();
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if(commandGesture.active()&&event.button()==commandButton){if(commandLine)commandGesture.sample(minecraft,Math.clamp(event.x(),0,width),Math.clamp(event.y(),UiMenuBar.height(width),height),width,height,false);return true;}
        if(!selecting||event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return super.mouseDragged(event,dx,dy);
        endX=Math.clamp(event.x(),0,width);endY=Math.clamp(event.y(),UiMenuBar.height(width),height);
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if(commandGesture.active()&&event.button()==commandButton){
            if(!commandContext.equals(dev.hoi.client.map.MapCommanderOrders.gestureContext())){commandGesture.clear();return true;}
            commandGesture.sample(minecraft,Math.clamp(event.x(),0,width),Math.clamp(event.y(),UiMenuBar.height(width),height),width,height,true);dev.hoi.client.map.MapCommanderOrders.map(commandGesture.finish(minecraft),commandButton==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);return true;
        }
        if(!selecting||event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return super.mouseReleased(event);
        mouseDragged(event,0,0);selecting=false;
        if(minecraft.level==null||!dragDimension.equals(minecraft.level.dimension().identifier().toString()))return true;
        if(dragged())dev.hoi.client.map.MapUnitHud.selectArea(startX,startY,endX,endY);
        else InputMapInfo.clickAt(minecraft,event.x(),event.y(),width,height);
        return true;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if(dev.hoi.client.map.MapCommanderHud.scrollAt(x,y,vertical))return true;
        if (y < UiMenuBar.STATS_HEIGHT) {
            hudScroll = UiMenuBar.scroll(width, ClientCampaignHud.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void tick() {
        if(windowActive&&!minecraft.isWindowActive()){selecting=false;commandGesture.clear();dev.hoi.client.map.MapCommanderOrders.cancelTarget();}windowActive=minecraft.isWindowActive();
        if(commandGesture.active()&&!commandContext.equals(dev.hoi.client.map.MapCommanderOrders.gestureContext()))commandGesture.clear();
        if(selecting&&(minecraft.level==null||!dragDimension.equals(minecraft.level.dimension().identifier().toString())))selecting=false;
        if (!ClientCampaignHud.visible()) onClose();
        else if (!java.util.Objects.equals(country, ClientCampaignHud.country())) rebuildWidgets();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean allowsMovement() { return true; }
    @Override public int unitHudLeft() { return 0; }
    @Override public boolean keyPressed(KeyEvent event) { if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN&&dev.hoi.client.map.MapCommanderOrders.confirmDraft())return true;if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE){commandGesture.clear();if(dev.hoi.client.map.MapCommanderOrders.cancelTarget()||dev.hoi.client.map.MapCommanderHud.closeSelection())return true;}return InputSidebarMovement.consumes(minecraft, event) || super.keyPressed(event); }
    @Override public boolean keyReleased(KeyEvent event) { return InputSidebarMovement.consumes(minecraft, event) || super.keyReleased(event); }
    @Override public void removed() { dev.hoi.client.map.MapCommanderOrders.cancelTarget();selecting=false;commandGesture.clear();InputSidebarMovement.release(minecraft); }
}
