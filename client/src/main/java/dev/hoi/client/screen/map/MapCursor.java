package dev.hoi.client.screen.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputMapInfo;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.screen.army.ArmyLandPlanGesture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class MapCursor extends Screen implements InputSidebarMovement.InputMovementScreen {
    private int hudScroll;
    private String country;
    private String dragDimension="";
    private boolean selecting,windowActive=true;
    private final ArmyLandPlanGesture commandGesture=new ArmyLandPlanGesture();
    private String commandContext="";
    private boolean commandLine;
    private int commandButton;
    private double startX,startY,endX,endY;
    private boolean dragged() {return Math.hypot(endX-startX,endY-startY)>=new dev.hoi.client.ui.UiUnits(width,height).size(3);}
    public MapCursor() { super(Component.literal("지도")); }
    @Override protected void init() {
        dev.hoi.client.map.commander.CommanderHud.cancelDrag();
        selecting=false;commandGesture.clear();commandContext="";
        country = ClientCampaignHud.country();
        HudMenuBar.buttons(width, country, null, HoiClient::openMenu).forEach(this::addRenderableWidget);
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
        HudMenuBar.draw(graphics, width, ClientCampaignHud.hud(), mouseX, mouseY, hudScroll);
        for (var child : children()) if (child instanceof Renderable renderable)
            renderable.extractRenderState(graphics, mouseX, mouseY, delta);
        dev.hoi.client.map.supply.SupplyHubHover.update(minecraft, mouseX, mouseY, width, height, !selecting && mouseY >= HudMenuBar.height(width));
        dev.hoi.client.map.supply.SupplyHubHover.draw(graphics, font, mouseX, mouseY, width, height);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (dev.hoi.client.map.agency.AgencyRaidHud.click(event, twice)) return true;
        if (super.mouseClicked(event, twice)) return true;
        if(event.y()<HudMenuBar.height(width)||minecraft.level==null)return false;
        if(dev.hoi.client.map.commander.CommanderHud.pressAt(event.x(),event.y(),event.button(),twice))return true;
        if(dev.hoi.client.map.naval.NavalInvasionOverlay.click(event.x(),event.y(),event.button()))return true;
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT||event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&(dev.hoi.client.map.commander.CommanderOrders.pointMode()||dev.hoi.client.map.commander.CommanderOrders.clicksBorder()||dev.hoi.client.map.commander.CommanderOrders.editsPoint())){
            if(dev.hoi.client.map.commander.CommanderOrders.targetTool()&&!dev.hoi.client.map.commander.CommanderOrders.ready())return true;
            commandButton=event.button();commandContext=dev.hoi.client.map.commander.CommanderOrders.gestureContext();commandLine=dev.hoi.client.map.commander.CommanderOrders.drawsPlanLine();
            return commandGesture.begin(minecraft,event.x(),event.y(),width,height,!dev.hoi.client.map.commander.CommanderOrders.targetTool());
        }
        if(event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return false;
        selecting=true;
        startX=endX=event.x();startY=endY=event.y();
        dragDimension=minecraft.level.dimension().identifier().toString();
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if (dev.hoi.client.map.agency.AgencyRaidHud.drag(event, dx, dy)) return true;
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&dev.hoi.client.map.commander.CommanderHud.dragAt(event.x(),event.y()))return true;
        if(commandGesture.active()&&event.button()==commandButton){if(commandLine)commandGesture.sample(minecraft,Math.clamp(event.x(),0,width),Math.clamp(event.y(),HudMenuBar.height(width),height),width,height,false);return true;}
        if(!selecting||event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return super.mouseDragged(event,dx,dy);
        endX=Math.clamp(event.x(),0,width);endY=Math.clamp(event.y(),HudMenuBar.height(width),height);
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dev.hoi.client.map.agency.AgencyRaidHud.release(event)) return true;
        if(dev.hoi.client.map.commander.CommanderHud.releaseAt(event.x(),event.button()))return true;
        if(commandGesture.active()&&event.button()==commandButton){
            if(!commandContext.equals(dev.hoi.client.map.commander.CommanderOrders.gestureContext())){commandGesture.clear();return true;}
            commandGesture.sample(minecraft,Math.clamp(event.x(),0,width),Math.clamp(event.y(),HudMenuBar.height(width),height),width,height,true);dev.hoi.client.map.commander.CommanderOrders.map(commandGesture.finish(minecraft),commandButton==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);return true;
        }
        if(!selecting||event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return super.mouseReleased(event);
        mouseDragged(event,0,0);selecting=false;
        if(minecraft.level==null||!dragDimension.equals(minecraft.level.dimension().identifier().toString()))return true;
        if(dragged())dev.hoi.client.map.commander.CommanderUnitHud.selectArea(startX,startY,endX,endY);
        else InputMapInfo.clickAt(minecraft,event.x(),event.y(),width,height,InputMapInfo.modifiers(event));
        return true;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (dev.hoi.client.map.agency.AgencyRaidHud.scroll(x, y, horizontal, vertical)) return true;
        if(dev.hoi.client.map.commander.CommanderHud.scrollAt(x,y,vertical))return true;
        if (y < HudMenuBar.STATS_HEIGHT) {
            hudScroll = HudMenuBar.scroll(width, ClientCampaignHud.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void tick() {
        if(windowActive&&!minecraft.isWindowActive()){selecting=false;commandGesture.clear();dev.hoi.client.map.commander.CommanderHud.cancelDrag();dev.hoi.client.map.commander.CommanderOrders.cancelTarget();}windowActive=minecraft.isWindowActive();
        if(commandGesture.active()&&!commandContext.equals(dev.hoi.client.map.commander.CommanderOrders.gestureContext()))commandGesture.clear();
        if(selecting&&(minecraft.level==null||!dragDimension.equals(minecraft.level.dimension().identifier().toString())))selecting=false;
        dev.hoi.client.map.supply.SupplyHubHover.tick();
        if (!ClientCampaignHud.visible()) onClose();
        else if (!java.util.Objects.equals(country, ClientCampaignHud.country())) rebuildWidgets();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean allowsMovement() { return dev.hoi.client.map.agency.AgencyRaidHud.allowsMovement(); }
    @Override public int unitHudLeft() { return 0; }
    @Override public boolean keyPressed(KeyEvent event) { if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN&&dev.hoi.client.map.commander.CommanderOrders.confirmDraft())return true;if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE){if(dev.hoi.client.map.agency.AgencyRaidHud.closePanel())return true;dev.hoi.client.map.commander.CommanderHud.cancelDrag();commandGesture.clear();if(dev.hoi.client.map.commander.CommanderOrders.cancelTarget()||dev.hoi.client.map.commander.CommanderHud.closeSelection())return true;}return InputSidebarMovement.consumes(minecraft, event) || super.keyPressed(event); }
    @Override public boolean keyReleased(KeyEvent event) { return InputSidebarMovement.consumes(minecraft, event) || super.keyReleased(event); }
    @Override public void removed() { dev.hoi.client.map.commander.CommanderHud.cancelDrag(); dev.hoi.client.map.commander.CommanderOrders.cancelTarget();selecting=false;commandGesture.clear();dev.hoi.client.map.supply.SupplyHubHover.reset();InputSidebarMovement.release(minecraft); }
}
