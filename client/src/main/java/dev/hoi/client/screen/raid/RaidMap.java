package dev.hoi.client.screen.raid;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.raid.RaidView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.*;

final class RaidMap {
    private static net.minecraft.resources.Identifier texture;
    private static boolean missing;
    static void resourcesReloaded() {
        if (texture != null) Minecraft.getInstance().getTextureManager().release(texture);
        texture = null; missing = false;
    }
    static net.minecraft.resources.Identifier texture() {
        if (texture != null || missing) return texture;
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
                image.setPixel(x, y, alpha << 24 | color & 0xFFFFFF);
            }
            var dynamic = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "HOI original raid arrow mask", image);
            var id = net.minecraft.resources.Identifier.parse("hoi:raids/air_arrow_masked");
            try { client.getTextureManager().register(id, dynamic); }
            catch (RuntimeException | Error failure) { dynamic.close(); throw failure; }
            texture = id;
        } catch (java.io.IOException failure) { missing = true; }
        return texture;
    }
    record Hit(int x, int y, RaidView.Target target) {
        boolean contains(double px, double py) { return px >= x - 10 && px < x + 10 && py >= y - 10 && py < y + 10; }
    }
    private List<Hit> hits = List.of();
    List<Hit> hits() { return hits; }
    void clear() { hits = List.of(); }
    Hit at(double x, double y) { return hits.reversed().stream().filter(hit -> hit.contains(x, y)).findFirst().orElse(null); }
    String tooltip(double x, double y) { var hit = at(x, y); return hit == null ? "" : hit.target().tooltip(); }
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
    void draw(GuiGraphicsExtractor g, RaidView view, Set<String> hidden, int left, int top) {
        clear(); var client = Minecraft.getInstance();
        if (view == null || client.level == null || !client.level.dimension().identifier().toString().startsWith("hoi:atlas_")) return;
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
            var texture = texture();
            if (texture == null) continue;
            g.pose().pushMatrix(); g.pose().translate(from.x, from.y); g.pose().rotate((float)Math.atan2(to.y - from.y, to.x - from.x));
            g.blit(texture, 0, -Math.round(headWidth * .15f), Math.round(body), Math.round(headWidth * .15f), 0, .5f, .3f, .7f);
            g.blit(texture, Math.round(body), -Math.round(headWidth / 2), (int)Math.round(length), Math.round(headWidth / 2), .5f, 1, 0, 1);
            g.pose().popMatrix();
        }
        var visible = new ArrayList<Hit>();
        for (var target : view.targets()) {
            if (hidden.contains(target.category())) continue;
            var point = project(g, target.x(), target.z());
            if (point == null || point.x < left + 10 || point.x >= g.guiWidth() - 10 || point.y < top + 10 || point.y >= g.guiHeight() - 10) continue;
            int x = Math.round(point.x), y = Math.round(point.y);
            String asset = RaidScreen.asset(target.icon(), 0);
            UiAssets.draw(g, RaidScreen.asset("GFX_raid_target_icon_generic", 0), x - 12, y - 12, 24, 24);
            if (target.allowed()) UiAssets.draw(g, asset, x - 9, y - 9, 18, 18);
            else UiAssets.drawGrayscale(g, asset, x - 9, y - 9, 18, 18);
            visible.add(new Hit(x, y, target));
        }
        g.disableScissor(); hits = List.copyOf(visible);
    }
}
