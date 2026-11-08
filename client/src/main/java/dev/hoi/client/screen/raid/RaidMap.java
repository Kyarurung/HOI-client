package dev.hoi.client.screen.raid;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.screen.decision.DecisionText;
import dev.hoi.protocol.raid.RaidView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.*;

final class RaidMap {
    private static final Map<String, net.minecraft.resources.Identifier> textures = new HashMap<>();
    private static boolean missing;
    static void resourcesReloaded() {
        textures.values().forEach(Minecraft.getInstance().getTextureManager()::release);
        textures.clear(); missing = false;
    }
    static net.minecraft.resources.Identifier texture(String state) {
        String key = state.equals("BLOCKED") ? "PREPARING" : state;
        if (textures.containsKey(key) || missing) return textures.get(key);
        var client = Minecraft.getInstance();
        try (var maskInput = client.getResourceManager().open(net.minecraft.resources.Identifier.parse("hoi:textures/gui/raids/air_arrow_mask.png"));
                var mask = com.mojang.blaze3d.platform.NativeImage.read(maskInput);
                var patternInput = client.getResourceManager().open(net.minecraft.resources.Identifier.parse("hoi:textures/gui/raids/air_arrow.png"));
                var pattern = com.mojang.blaze3d.platform.NativeImage.read(patternInput)) {
            if (pattern.getWidth() != mask.getWidth() || pattern.getHeight() != mask.getHeight()) throw new java.io.IOException("Raid arrow pattern/mask size mismatch");
            var image = new com.mojang.blaze3d.platform.NativeImage(mask.getWidth(), mask.getHeight(), false);
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int pixel = mask.getPixel(x, y), color = pattern.getPixel(x, y);
                double inner = Math.min(1, ((pixel >> 16 & 255) * .5 + (pixel >> 8 & 255) * .9) / 255) * (pixel >>> 24) / 255;
                double outer = (pixel >> 16 & 255) / 255.0 * (pixel >>> 24) / 255;
                double fade = x < image.getWidth() / 2 ? Math.min(1, (x + .5) / (image.getWidth() * .5) / .3) : 1;
                int alpha = (int)Math.round((color >>> 24) * (1 - (1 - inner) * (1 - outer)) * fade);
                int tint = switch (key) { case "READY" -> 0xE6B3B300; case "ACTIVE" -> 0xE6FF0000; default -> 0xFFB3B3B3; };
                image.setPixel(x, y, alpha * (tint >>> 24) / 255 << 24
                    | (color >> 16 & 255) * (tint >> 16 & 255) / 255 << 16
                    | (color >> 8 & 255) * (tint >> 8 & 255) / 255 << 8 | (color & 255) * (tint & 255) / 255);
            }
            var dynamic = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "HOI original raid arrow mask", image);
            var id = net.minecraft.resources.Identifier.parse("hoi:raids/air_arrow_" + key.toLowerCase(Locale.ROOT));
            try { client.getTextureManager().register(id, dynamic); }
            catch (RuntimeException | Error failure) { dynamic.close(); throw failure; }
            textures.put(key, id);
        } catch (java.io.IOException failure) { missing = true; }
        return textures.get(key);
    }
    record Hit(int x, int y, RaidView.Target target) {
        boolean contains(double px, double py) { return px >= x - 10 && px < x + 10 && py >= y - 10 && py < y + 10; }
    }
    private List<Hit> hits = List.of();
    record Interaction(double x, double y, double endX, double endY, double body, double halfWidth, String tooltip, String open, String right) {
        boolean contains(double px, double py) {
            double dx = endX - x, dy = endY - y, length = Math.hypot(dx, dy);
            if (length < .001) return Math.abs(px - x) <= halfWidth && Math.abs(py - y) <= halfWidth;
            double along = ((px - x) * dx + (py - y) * dy) / length;
            double across = Math.abs((px - x) * dy - (py - y) * dx) / length;
            return along >= 0 && along <= length && across <= Math.max(3, along < body ? halfWidth * .3 : halfWidth);
        }
    }
    private final List<Interaction> interactions = new ArrayList<>();
    private String activePlan = "";
    private double mouseX = -1, mouseY = -1;
    private int planX, planY, planHeight;
    void hover(double x, double y) { mouseX = x; mouseY = y; }
    List<Hit> hits() { return hits; }
    List<Interaction> interactions() { return List.copyOf(interactions); }
    void clear() { hits = List.of(); interactions.clear(); activePlan = ""; }
    Hit at(double x, double y) { return hits.reversed().stream().filter(hit -> hit.contains(x, y)).findFirst().orElse(null); }
    Interaction interaction(double x, double y) { return interactions.reversed().stream().filter(hit -> hit.contains(x, y)).findFirst().orElse(null); }
    boolean planContains(double x, double y) { return !activePlan.isEmpty() && x >= planX && x < planX + 100 && y >= planY && y < planY + planHeight; }
    String tooltip(double x, double y) {
        var hit = at(x, y);
        if (planContains(x, y)) return hit == null ? "" : hit.target().tooltip();
        var action = interaction(x, y);
        return action != null ? action.tooltip() : hit == null || !activePlan.isEmpty() ? "" : hit.target().tooltip();
    }
    private org.joml.Vector3f project(GuiGraphicsExtractor g, double x, double z) {
        var client = Minecraft.getInstance();
        int bx = (int)Math.floor(x), bz = (int)Math.floor(z);
        if (client.level == null || !client.level.hasChunk(bx >> 4, bz >> 4)) return null;
        var camera = client.gameRenderer.mainCamera();
        var relative = new Vec3(x, client.level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz) + 1, z).subtract(camera.position());
        var forward = camera.forwardVector();
        if (relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z() <= 0) return null;
        var point = camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f()).transformProject(relative.toVector3f());
        return point.isFinite() ? new org.joml.Vector3f((point.x + 1) * g.guiWidth() / 2, (1 - point.y) * g.guiHeight() / 2, point.z) : null;
    }
    void draw(GuiGraphicsExtractor g, RaidView view, int left, int top, DecisionText text) {
        hits = List.of(); interactions.clear(); var client = Minecraft.getInstance();
        if (view == null || client.level == null || !client.level.dimension().identifier().toString().startsWith("hoi:atlas_")) { activePlan = ""; return; }
        g.enableScissor(left, top, g.guiWidth(), g.guiHeight());
        for (var line : view.lines()) {
            if (!line.arrow().equals("air")) continue;
            var from = project(g, line.fromX(), line.fromZ()); var to = project(g, line.toX(), line.toZ());
            if (from == null || to == null) continue;
            double length = Math.hypot(to.x - from.x, to.y - from.y);
            if (length < 1) continue;
            double dx = line.toX() - line.fromX(), dz = line.toZ() - line.fromZ(), worldLength = Math.hypot(dx, dz);
            if (worldLength < .001) continue;
            var side = project(g, line.toX() - dz / worldLength * 2, line.toZ() + dx / worldLength * 2);
            float headWidth = side == null ? 16 : (float)Math.clamp(Math.hypot(side.x - to.x, side.y - to.y), 4, 44);
            float head = Math.min((float)length, headWidth * 1.75f), body = (float)length - head;
            var texture = texture(line.state());
            if (texture == null) continue;
            g.pose().pushMatrix(); g.pose().translate(from.x, from.y); g.pose().rotate((float)Math.atan2(to.y - from.y, to.x - from.x));
            g.blit(texture, 0, -Math.round(headWidth * .15f), Math.round(body), Math.round(headWidth * .15f), 0, .5f, .3f, .7f);
            g.blit(texture, Math.round(body), -Math.round(headWidth / 2), (int)Math.round(length), Math.round(headWidth / 2), .5f, 1, 0, 1);
            g.pose().popMatrix();
            interactions.add(new Interaction(from.x, from.y, to.x, to.y, body, headWidth / 2, line.tooltip(), line.open(), line.locate()));
        }
        var visible = new ArrayList<Hit>();
        var positions = new HashSet<String>();
        String hoveredPlan = planContains(mouseX, mouseY) ? activePlan : "";
        for (var target : view.targets()) {
            if (!positions.add(target.x() + ":" + target.z())) continue;
            var point = project(g, target.x(), target.z());
            if (point == null || point.x < left + 10 || point.x >= g.guiWidth() - 10 || point.y < top + 10 || point.y >= g.guiHeight() - 10) continue;
            int x = Math.round(point.x), y = Math.round(point.y);
            String asset = RaidScreen.asset(target.icon(), 0);
            UiAssets.draw(g, RaidScreen.asset("GFX_raid_target_icon_generic", 0), x - 12, y - 12, 24, 24);
            if (target.allowed()) UiAssets.draw(g, asset, x - 9, y - 9, 18, 18);
            else UiAssets.drawGrayscale(g, asset, x - 9, y - 9, 18, 18);
            visible.add(new Hit(x, y, target));
            if (Math.abs(mouseX - x) < 12 && Math.abs(mouseY - y) < 12) hoveredPlan = view.plans().stream()
                .filter(plan -> plan.rows().stream().anyMatch(row -> row.targets().contains(target.id()))).map(RaidView.Plan::id).findFirst().orElse("");
        }
        for (var marker : view.markers()) {
            var point = project(g, marker.x(), marker.z());
            if (point == null || point.x < left + 8 || point.y < top + 8 || point.x >= g.guiWidth() - 8 || point.y >= g.guiHeight() - 8) continue;
            UiAssets.draw(g, RaidScreen.asset("GFX_raid_target_icon_generic", 0), Math.round(point.x) - 8, Math.round(point.y) - 8, 16, 16);
            interactions.add(new Interaction(point.x, point.y, point.x, point.y, 0, 8, marker.tooltip(), marker.open(), marker.dismiss()));
        }
        activePlan = view.window() == null && interaction(mouseX, mouseY) == null ? hoveredPlan : "";
        var plan = view.plans().stream().filter(value -> value.id().equals(activePlan)).findFirst().orElse(null);
        if (plan != null) {
            var anchor = project(g, plan.x(), plan.z());
            if (anchor != null) {
                planHeight = 13 + plan.rows().stream().mapToInt(row -> 12 + ((row.targets().size() + 4) / 5) * 19).sum();
                planX = Math.clamp(Math.round(anchor.x) + 10, left, Math.max(left, g.guiWidth() - 100));
                planY = Math.clamp(Math.round(anchor.y) - 35, top, Math.max(top, g.guiHeight() - planHeight));
                UiAssets.nineSlice(g, RaidScreen.asset("GFX_construction_province_background", 0), planX, planY, 100, planHeight, 4, .5);
                UiText.centered(g, client.font, text.component(view.planTitle()), planX, planY + 1, 100, 11, UiMenuStyle.TEXT);
                int rowY = planY + 13;
                for (var row : plan.rows()) {
                    UiText.scrollingCell(g, client.font, text.component(row.title()), planX + 5, rowY, 90, 10, UiMenuStyle.TEXT, false);
                    for (int i = 0; i < row.targets().size(); i++) {
                        String id = row.targets().get(i);
                        var target = view.targets().stream().filter(value -> value.id().equals(id)).findFirst().orElseThrow();
                        int x = planX + 14 + i % 5 * 19, y = rowY + 20 + i / 5 * 19;
                        if (target.allowed()) UiAssets.draw(g, RaidScreen.asset(target.icon(), 0), x - 9, y - 9, 18, 18);
                        else UiAssets.drawGrayscale(g, RaidScreen.asset(target.icon(), 0), x - 9, y - 9, 18, 18);
                        visible.add(new Hit(x, y, target));
                    }
                    rowY += 12 + ((row.targets().size() + 4) / 5) * 19;
                }
            } else activePlan = "";
        }
        g.disableScissor(); hits = List.copyOf(visible);
    }
}
