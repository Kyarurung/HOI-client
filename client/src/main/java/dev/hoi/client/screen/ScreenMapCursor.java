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
    public ScreenMapCursor() { super(Component.literal("지도")); }
    @Override protected void init() {
        country = ClientCampaignHud.country();
        UiMenuBar.buttons(width, country, null, HoiClient::openMenu).forEach(this::addRenderableWidget);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        UiMenuBar.draw(graphics, width, ClientCampaignHud.hud(), mouseX, mouseY, hudScroll);
        for (var child : children()) if (child instanceof Renderable renderable)
            renderable.extractRenderState(graphics, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (super.mouseClicked(event, twice)) return true;
        return event.button() == 0 && event.y() >= UiMenuBar.height(width)
                && InputMapInfo.clickAt(minecraft, event.x(), event.y(), width, height);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (y < UiMenuBar.STATS_HEIGHT) {
            hudScroll = UiMenuBar.scroll(width, ClientCampaignHud.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void tick() {
        if (!ClientCampaignHud.visible()) onClose();
        else if (!java.util.Objects.equals(country, ClientCampaignHud.country())) rebuildWidgets();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean allowsMovement() { return true; }
    @Override public int unitHudLeft() { return 0; }
    @Override public boolean keyPressed(KeyEvent event) { return InputSidebarMovement.consumes(minecraft, event) || super.keyPressed(event); }
    @Override public boolean keyReleased(KeyEvent event) { return InputSidebarMovement.consumes(minecraft, event) || super.keyReleased(event); }
    @Override public void removed() { InputSidebarMovement.release(minecraft); }
}
