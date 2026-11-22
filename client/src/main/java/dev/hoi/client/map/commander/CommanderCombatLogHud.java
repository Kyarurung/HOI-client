package dev.hoi.client.map.commander;

import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.screen.commander.CommanderOverview;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import dev.hoi.protocol.commander.CommanderRecordPayloads;
import dev.hoi.protocol.commander.CommanderRecordPayloads.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public final class CommanderCombatLogHud {
    public static Consumer<Request> requestObserver;
    private static CommanderCombatLogWindow window;
    private CommanderCombatLogHud() {}
    static void register() {
        CommanderRecordPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(LogSnapshot.TYPE, (packet, context) -> accept(packet.log()));
        ClientPlayNetworking.registerGlobalReceiver(LossesSnapshot.TYPE, (packet, context) -> { if (DialogCoordinator.contentScreen() instanceof CommanderOverview overview) overview.accept(packet.losses()); });
        HudElementRegistry.attachElementAfter(Identifier.parse("hoi:ship_window"), Identifier.parse("hoi:combat_log"), (g, delta) -> {
            if (Minecraft.getInstance().gui.screen() == null) draw(g, g.guiWidth() / 2, g.guiHeight() / 2);
        });
    }
    public static void send(Request request) {
        if (requestObserver != null) requestObserver.accept(request);
        else if (ClientPlayNetworking.canSend(Request.TYPE)) ClientPlayNetworking.send(request);
    }
    public static CommanderCombatLogWindow window() { return window; }
    static boolean shows(String theater) { return window != null && window.theater().equals(theater); }
    public static void refresh() { if (window != null) window.request(); }
    public static void open(String theater) {
        if (shows(theater)) { close(); return; }
        var next = new CommanderCombatLogWindow(theater);
        if (window != null) { next.place(window); window.close(); }
        window = next;
        layout();
        window.open();
    }
    public static void accept(Log log) { if (window != null && !window.accept(log)) close(); }
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
    public static boolean click(MouseButtonEvent event) { return visible() && window.mouseClicked(event, CommanderCombatLogHud::close); }
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
