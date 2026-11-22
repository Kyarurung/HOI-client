package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import dev.hoi.protocol.commander.CommanderBattlePayloads;
import dev.hoi.protocol.commander.CommanderBattlePayloads.*;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CommanderBattleMarkers {
    public record Placed(Marker marker, float x, float y, float w, float h) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    private static final String MAP = "commander/battle/map/";
    private static final double LIFT = 3.1, REACH = 8;
    private static final int RESULT_STEP = 24;
    private static Markers markers;
    private static List<Placed> placed = List.of();
    private CommanderBattleMarkers() {}
    static void register() {
        CommanderBattlePayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(Markers.TYPE, (packet, context) -> accept(packet));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> { accept(null); CommanderBattleHud.reset(); }));
        HudElementRegistry.attachElementAfter(Identifier.parse("hoi:unit_hud"), Identifier.parse("hoi:battle_markers"), (g, delta) -> {
            if (Minecraft.getInstance().gui.screen() != null) return;
            draw(g, 0);
            tooltip(g, g.guiWidth() / 2, g.guiHeight() / 2);
        });
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof InputSidebarMovement.InputMovementScreen sidebar)) return;
            ScreenEvents.afterBackground(screen).register((s, g, mx, my, delta) -> draw(g, left()));
            ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> { if (mx >= left() && !sidebar.covers(mx, my)) tooltip(g, mx, my); });
        });
    }
    private static int left() { return Minecraft.getInstance().gui.screen() instanceof InputSidebarMovement.InputMovementScreen sidebar && sidebar.allowsMovement() ? sidebar.unitHudLeft() : Integer.MAX_VALUE; }
    public static void accept(Markers packet) { markers = packet; placed = List.of(); }
    public static List<Placed> placed() { return placed; }
    private static boolean shown() {
        var client = Minecraft.getInstance();
        return markers != null && !markers.markers().isEmpty() && client.level != null && client.player != null && !client.gui.hud.isHidden() && markers.dimension().equals(client.level.dimension().identifier().toString());
    }
    private static float[] project(GuiGraphicsExtractor g, Matrix4f matrix, Vec3 camera, Vector3f forward, Vec3 point) {
        var relative = point.subtract(camera);
        if (relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z() <= 0) return null;
        var projected = matrix.transformProject(relative.toVector3f());
        return projected.isFinite() ? new float[]{(projected.x + 1) * g.guiWidth() / 2, (1 - projected.y) * g.guiHeight() / 2} : null;
    }
    private static void draw(GuiGraphicsExtractor g, int left) {
        placed = List.of();
        if (!shown() || left >= g.guiWidth()) return;
        var client = Minecraft.getInstance();
        var camera = client.gameRenderer.mainCamera();
        var matrix = camera.getViewRotationProjectionMatrix(new Matrix4f());
        var forward = new Vector3f(camera.forwardVector());
        float scale = CommanderLayout.units(g.guiWidth(), g.guiHeight()).scale() * .5f;
        int top = HudMenuBar.height(g.guiWidth());
        var result = new ArrayList<Placed>();
        var stacked = new HashMap<Vec3, Integer>();
        g.enableScissor(left, top, g.guiWidth(), g.guiHeight());
        for (var marker : markers.markers()) {
            var point = new Vec3(marker.x(), marker.y() + LIFT, marker.z());
            var at = project(g, matrix, camera.position(), forward, point);
            if (at == null) continue;
            float x = at[0] + (marker.kind() == Kind.RESULT ? (stacked.merge(point, 1, Integer::sum) - 1) * RESULT_STEP * scale : 0), y = at[1];
            if (x < left - 80 * scale || x > g.guiWidth() + 80 * scale || y < top - 40 * scale || y > g.guiHeight() + 40 * scale) continue;
            switch (marker.kind()) {
                case LAND -> {
                    var heading = marker.headingX() == 0 && marker.headingZ() == 0 ? null : project(g, matrix, camera.position(), forward, point.add(marker.headingX() * REACH, 0, marker.headingZ() * REACH));
                    result.add(land(g, marker, x, y, scale, heading == null ? Float.NaN : (float)Math.atan2(heading[1] - at[1], heading[0] - at[0])));
                }
                case NAVAL -> result.add(naval(g, marker, x, y, scale));
                case RESULT -> {
                    art(g, MAP + "result/frame_" + marker.frame(), x - 17.5f * scale, y - 16.5f * scale, 35 * scale, 33 * scale, -1);
                    result.add(new Placed(marker, x - 17.5f * scale, y - 16.5f * scale, 35 * scale, 33 * scale));
                }
            }
        }
        g.disableScissor();
        placed = List.copyOf(result);
    }
    private static void art(GuiGraphicsExtractor g, String path, float x, float y, float w, float h, int tint) { UiAssets.drawExact(g, path, x, y, w, h, tint); }
    private static int tint(Tone tone) { return switch (tone) { case WINNING -> 0xFF00FF00; case LOSING -> 0xFFFF0000; case BALANCED -> 0xFFFFFF00; }; }
    private static Placed land(GuiGraphicsExtractor g, Marker marker, float x, float y, float scale, float angle) {
        int tint = tint(marker.tone());
        float left = x - 17.5f * scale, top = y - 17 * scale;
        if (!Float.isNaN(angle)) {
            g.pose().pushMatrix();
            g.pose().translate(left + 18 * scale, top + 18 * scale);
            g.pose().rotate(angle + (float)Math.PI / 2);
            art(g, MAP + "direction", -35 * scale, -35 * scale, 70 * scale, 70 * scale, tint);
            g.pose().popMatrix();
        }
        art(g, MAP + (marker.urban() ? "urban" : "land"), left - 2 * scale, top - 2 * scale, 38 * scale, 37 * scale, tint);
        if (marker.frame() > 0) art(g, "commander/battle/side_modifier/frame_" + (marker.frame() - 1), left + 4 * scale, top + 8 * scale, 25 * scale, 25 * scale, -1);
        else {
            var font = Minecraft.getInstance().font;
            float text = UiText.scale(font);
            UiText.marquee(g, font, marker.value(), left, UiText.centeredY(font, marker.value(), top + 23 * scale - 4.5f, 9, text), 36 * scale, text, 0xFFFFFFFF, true, 0);
        }
        return new Placed(marker, left, top, 35 * scale, 34 * scale);
    }
    private static Placed naval(GuiGraphicsExtractor g, Marker marker, float x, float y, float scale) {
        art(g, MAP + "naval/frame_" + marker.frame(), x - 19 * scale, y - 46.5f * scale, 40 * scale, 33 * scale, -1);
        float left = x - 77 * scale, top = y - 7 * scale, width = 154 * scale, height = 14 * scale;
        int split = Math.round(left + width * (marker.value().isEmpty() ? 50 : Integer.parseInt(marker.value())) / 100);
        bar(g, "progress_attack", left, top, width, height, Math.round(left), split);
        bar(g, "progress_defence", left, top, width, height, split, Math.round(left + width));
        return new Placed(marker, left, y - 46.5f * scale, width, 53.5f * scale);
    }
    private static void bar(GuiGraphicsExtractor g, String name, float x, float y, float w, float h, int from, int to) {
        if (to <= from) return;
        g.enableScissor(from, (int)Math.floor(y), to, (int)Math.ceil(y + h));
        art(g, MAP + name, x, y, w, h, -1);
        g.disableScissor();
    }
    private static Marker at(double x, double y) {
        if (!shown() || CommanderBattleHud.covers(x, y) || CommanderHud.covers(x, y)) return null;
        for (int i = placed.size() - 1; i >= 0; i--) if (placed.get(i).contains(x, y)) return placed.get(i).marker();
        return null;
    }
    private static void tooltip(GuiGraphicsExtractor g, int mx, int my) {
        var marker = at(mx, my);
        if (marker != null && !marker.tooltip().isEmpty()) CommanderHud.drawTooltip(g, mx, my, TooltipDelay.visible(marker.tooltip()));
    }
    public static boolean click(double x, double y, int button) {
        var marker = at(x, y);
        if (marker == null) return false;
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (marker.kind() != Kind.RESULT) return false;
            AudioUiSounds.play("ui.close");
            CommanderBattleHud.send(new Request(UUID.randomUUID().toString(), Action.DISMISS_ALL, Kind.RESULT, marker.id()));
            return true;
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        CommanderBattleHud.open(marker.kind(), marker.id());
        var client = Minecraft.getInstance();
        if (client.gui.screen() == null) client.gui.setScreen(new MapCursor());
        return true;
    }
}
