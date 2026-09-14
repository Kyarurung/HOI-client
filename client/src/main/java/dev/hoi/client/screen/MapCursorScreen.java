package dev.hoi.client.screen;

import dev.hoi.client.CampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.MapInfoInput;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.ui.HoiMenuBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class MapCursorScreen extends Screen implements SidebarMovement.Screen {
    private int hudScroll;
    private String country;
    public MapCursorScreen() { super(Component.literal("지도")); }
    @Override protected void init() {
        country = CampaignHud.country();
        HoiMenuBar.buttons(width, country, null, HoiClient::openMenu).forEach(this::addRenderableWidget);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        HoiMenuBar.draw(graphics, width, CampaignHud.hud(), mouseX, mouseY, hudScroll);
        for (var child : children()) if (child instanceof Renderable renderable)
            renderable.extractRenderState(graphics, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (super.mouseClicked(event, twice)) return true;
        return event.button() == 0 && event.y() >= HoiMenuBar.height(width)
                && MapInfoInput.clickAt(minecraft, event.x(), event.y(), width, height);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (dev.hoi.client.ui.HudTooltip.scroll(vertical)) return true;
        if (y < HoiMenuBar.STATS_HEIGHT) {
            hudScroll = HoiMenuBar.scroll(width, CampaignHud.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void tick() {
        if (!CampaignHud.visible()) onClose();
        else if (!java.util.Objects.equals(country, CampaignHud.country())) rebuildWidgets();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean allowsMovement() { return true; }
    @Override public int unitHudLeft() { return 0; }
    @Override public boolean keyPressed(KeyEvent event) { return SidebarMovement.consumes(minecraft, event) || super.keyPressed(event); }
    @Override public boolean keyReleased(KeyEvent event) { return SidebarMovement.consumes(minecraft, event) || super.keyReleased(event); }
    @Override public void removed() { SidebarMovement.release(minecraft); }
}
