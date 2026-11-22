package dev.hoi.client.map.commander;

import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import dev.hoi.protocol.commander.CommanderShipPayloads;
import dev.hoi.protocol.commander.CommanderShipPayloads.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public final class CommanderShipHud {
    public static Consumer<Request> requestObserver;
    private static CommanderShipWindow window;
    private CommanderShipHud() {}
    static void register() {
        CommanderShipPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE, (packet, context) -> accept(packet.view()));
        ClientPlayNetworking.registerGlobalReceiver(ForceSnapshot.TYPE, (packet, context) -> CommanderShipRows.accept(packet.force()));
        HudElementRegistry.attachElementAfter(Identifier.parse("hoi:commander_hud"), Identifier.parse("hoi:ship_window"), (g, delta) -> {
            if (Minecraft.getInstance().gui.screen() == null) draw(g, g.guiWidth() / 2, g.guiHeight() / 2);
        });
    }
    static void send(Request request) {
        if (requestObserver != null) requestObserver.accept(request);
        else if (ClientPlayNetworking.canSend(Request.TYPE)) ClientPlayNetworking.send(request);
    }
    public static CommanderShipWindow window() { return window; }
    public static void refresh() { if (window != null) window.request(); }
    public static void open(String ship) {
        if (window != null && window.ship().equals(ship)) return;
        var next = new CommanderShipWindow(ship);
        if (window != null) { next.place(window); window.close(); }
        window = next;
        layout();
        window.open();
    }
    public static void accept(View view) { if (window != null && !window.accept(view)) close(); }
    private static void layout() {
        var client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth(), height = client.getWindow().getGuiScaledHeight();
        window.layout(width, height, CommanderLayout.units(width, height).scale() * .5f);
    }
    private static boolean visible() {
        var client = Minecraft.getInstance();
        return window != null && client.level != null && !client.gui.hud.isHidden() && !AgencyHud.active() && (client.gui.screen() == null || client.gui.screen() instanceof MapCursor);
    }
    public static boolean covers(double x, double y) { return visible() && window.contains(x, y); }
    public static void draw(GuiGraphicsExtractor g, int mx, int my) {
        if (!visible()) return;
        render(g);
        String tooltip = window.tooltip(mx, my);
        if (!tooltip.isEmpty()) CommanderHud.drawTooltip(g, mx, my, TooltipDelay.visible(tooltip));
    }
    public static void drawBackdrop(GuiGraphicsExtractor g) { if (window != null) render(g); }
    private static void render(GuiGraphicsExtractor g) { layout(); window.draw(g, Minecraft.getInstance().font); }
    public static boolean click(MouseButtonEvent event) { return visible() && window.mouseClicked(event, CommanderShipHud::close); }
    public static boolean drag(MouseButtonEvent event) {
        if (!visible() || !window.mouseDragged(event)) return false;
        layout();
        return true;
    }
    public static boolean release() { return window != null && window.mouseReleased() && visible(); }
    public static boolean scroll(double x, double y, double vertical) { return visible() && window.mouseScrolled(x, y, vertical); }
    static boolean closeWindow() {
        if (!visible()) return false;
        close();
        return true;
    }
    private static void close() { var old = window; window = null; old.close(); }
    public static void reset() { if (window != null) close(); }
}
