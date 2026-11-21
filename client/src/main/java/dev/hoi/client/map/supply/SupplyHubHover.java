package dev.hoi.client.map.supply;

import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.map.MapSupplyHover;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

public final class SupplyHubHover {
    public static final long DELAY_MILLIS = 1000;
    public static Consumer<MapSupplyHover.Request> queryObserver;
    private static boolean pointing;
    private static double x, y, z;
    private static long sequence, awaiting = -1, since;
    private static int delay;
    private static String target = "", text = "", delayed = "";
    private SupplyHubHover() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MapSupplyHover.Tooltip.TYPE, (packet, context) -> accept(packet));
    }
    public static boolean supplyMap(Minecraft client) {
        return client.level != null && client.level.dimension().identifier().toString().startsWith("hoi:atlas_supply_");
    }
    public static void reset() {
        pointing = false; awaiting = -1; delay = 0; target = ""; text = ""; delayed = "";
    }
    public static void update(Minecraft client, int mouseX, int mouseY, int width, int height, boolean available) {
        if (!available || !supplyMap(client) || !client.options.getCameraType().isFirstPerson()) { reset(); return; }
        var matrix = client.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
        var ray = matrix.transformProject(new org.joml.Vector3f((float)(mouseX / (double)width * 2 - 1), (float)(1 - mouseY / (double)height * 2), 1)).normalize();
        if (ray.y >= -.00001) { reset(); return; }
        point(ray.x, ray.y, ray.z);
    }
    public static void point(double nextX, double nextY, double nextZ) {
        if (!pointing || Math.abs(nextX - x) + Math.abs(nextY - y) + Math.abs(nextZ - z) > 1e-5) { awaiting = -1; delay = Math.min(delay, 2); }
        x = nextX; y = nextY; z = nextZ; pointing = true;
    }
    public static void tick() {
        if (!pointing || --delay > 0) return;
        if (queryObserver == null && !ClientPlayNetworking.canSend(MapSupplyHover.Request.TYPE)) { reset(); return; }
        var packet = new MapSupplyHover.Request(++sequence, x, y, z);
        awaiting = packet.sequence(); delay = 3;
        if (queryObserver != null) queryObserver.accept(packet); else ClientPlayNetworking.send(packet);
    }
    public static void accept(MapSupplyHover.Tooltip packet) {
        if (!pointing || packet.sequence() != awaiting) return;
        if (!packet.target().equals(target)) since = Util.getMillis();
        target = packet.target(); text = packet.text(); delayed = packet.delayed(); delay = 10;
    }
    public static String target() { return target; }
    public static String text() {
        if (text.isEmpty()) return "";
        return delayed.isEmpty() || Util.getMillis() - since < DELAY_MILLIS ? text : text + "\n\n" + delayed;
    }
    public static void draw(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, int width, int height) {
        String value = text();
        if (!value.isEmpty()) Tooltips.drawSourceScrollable(graphics, font, value, mouseX, mouseY, width, height, 0);
    }
}
