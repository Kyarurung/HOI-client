package dev.hoi.client.map.agency;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.screen.raid.RaidScreen;
import dev.hoi.protocol.raid.RaidPayloads;
import dev.hoi.protocol.raid.RaidView;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public final class AgencyRaidHud {
    private static RaidScreen screen;
    private static String country = "", dimension = "";
    private static java.util.function.Consumer<RaidPayloads.Request> transport = request -> {
        if (ClientPlayNetworking.canSend(RaidPayloads.Request.TYPE)) ClientPlayNetworking.send(request);
    };
    private AgencyRaidHud() {}
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        HudElementRegistry.attachElementAfter(Identifier.parse("hoi:agency_hud"), Identifier.parse("hoi:raid_hud"), (g, delta) -> {
            if (Minecraft.getInstance().gui.screen() == null) draw(g, g.guiWidth() / 2, g.guiHeight() / 2, 0);
        });
        ScreenEvents.AFTER_INIT.register((client, current, width, height) -> {
            if (current instanceof MapCursor) ScreenEvents.afterExtract(current).register((s, g, mx, my, delta) -> draw(g, mx, my, delta));
        });
    }
    private static boolean current() {
        var client = Minecraft.getInstance();
        return screen != null && AgencyHud.active() && country.equals(ClientCampaignHud.country())
                && dimension.equals(client.level.dimension().identifier().toString());
    }
    private static boolean visible() {
        var client = Minecraft.getInstance();
        return current() && !client.gui.hud.isHidden() && (client.gui.screen() == null || client.gui.screen() instanceof MapCursor);
    }
    private static void tick() {
        if (!AgencyHud.active()) { reset(); return; }
        if (screen != null && !current()) reset();
        if (screen == null) {
            var client = Minecraft.getInstance();
            country = ClientCampaignHud.country(); dimension = client.level.dimension().identifier().toString();
            screen = new RaidScreen(java.util.UUID.randomUUID().toString(), null, transport);
            screen.overlay(); resize(); screen.open();
        } else { resize(); screen.tick(); }
    }
    private static void resize() {
        var client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth(), height = client.getWindow().getGuiScaledHeight();
        if (screen.width != width || screen.height != height) screen.init(width, height);
    }
    public static void reset() {
        if (screen != null) screen.removed();
        screen = null; country = ""; dimension = "";
    }
    public static void accept(RaidView view) { if (current()) screen.update(view); }
    public static void accept(RaidPayloads.Highlight packet) { if (current()) screen.accept(packet); }
    public static dev.hoi.client.screen.raid.RaidSources sources() { return visible() && screen.showingSources() ? screen.sources() : null; }
    public static void drawMap(GuiGraphicsExtractor g) { if (visible()) { resize(); screen.extractMapState(g); } }
    public static void draw(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (visible()) { resize(); screen.extractRenderState(g, mx, my, delta); }
    }
    public static boolean click(MouseButtonEvent event, boolean twice) {
        return visible() && screen.mouseClicked(event, twice);
    }
    public static boolean drag(MouseButtonEvent event, double dx, double dy) {
        return visible() && screen.mouseDragged(event, dx, dy);
    }
    public static boolean release(MouseButtonEvent event) { return visible() && screen.mouseReleased(event); }
    public static boolean scroll(double x, double y, double horizontal, double vertical) {
        return visible() && screen.mouseScrolled(x, y, horizontal, vertical);
    }
    public static boolean closePanel() {
        if (!visible() || screen.snapshot() == null || screen.snapshot().setup() == null && screen.snapshot().window() == null) return false;
        screen.onClose(); return true;
    }
    public static boolean allowsMovement() { return !visible() || screen.allowsMovement(); }
}
