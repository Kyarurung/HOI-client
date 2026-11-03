package dev.hoi.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import dev.hoi.protocol.ProtocolAgencyView;
import dev.hoi.protocol.ProtocolAtlasScene.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

final class MapAgencyNetworks {
    private static Identifier texture;
    private static boolean missing;
    private static ProtocolAgencyView cached;
    private static long cachedTick = Long.MIN_VALUE;
    private static List<MapSceneMesh.MapTile> meshes = List.of();
    private MapAgencyNetworks() {}
    static Identifier texture() {
        if (texture != null || missing) return texture;
        var client = Minecraft.getInstance();
        try (var input = client.getResourceManager().open(Identifier.parse("hoi:textures/gui/agency/map/network_mask.png"));
                var mask = NativeImage.read(input);
                var patternInput = client.getResourceManager().open(Identifier.parse("hoi:textures/gui/agency/map/network_pattern.png"));
                var pattern = NativeImage.read(patternInput)) {
            if (pattern.getWidth() != mask.getWidth() || pattern.getHeight() != mask.getHeight())
                throw new IOException("Intel network pattern/mask size mismatch");
            var image = new NativeImage(mask.getWidth(), mask.getHeight(), false);
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int pixel = mask.getPixel(x, y);
                int color = pattern.getPixel(x, y);
                int white = Math.max(color & 255, Math.max(color >> 8 & 255, color >> 16 & 255));
                int alpha = (pixel >>> 24) * (pixel >> 8 & 255) / 255 * (color >>> 24) / 255;
                image.setPixel(x, y, alpha << 24 | white << 16 | white << 8 | white);
            }
            var dynamic = new DynamicTexture(() -> "HOI original intel network mask", image);
            var id = Identifier.parse("hoi:agency/network_edge");
            try { client.getTextureManager().register(id, dynamic); }
            catch (RuntimeException | Error failure) { dynamic.close(); throw failure; }
            texture = id;
        } catch (IOException failure) { missing = true; }
        return texture;
    }
    static void invalidate() { cached = null; cachedTick = Long.MIN_VALUE; meshes = List.of(); }
    static void clear() {
        if (texture != null) Minecraft.getInstance().getTextureManager().release(texture);
        texture = null; missing = false; invalidate();
    }
    private static double surface(double x, double z) {
        var level = Minecraft.getInstance().level;
        double height = level.getMinY();
        for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2) {
            int bx = (int)Math.floor(x + dx * .01), bz = (int)Math.floor(z + dz * .01);
            if (!level.hasChunk(bx >> 4, bz >> 4)) return Double.NaN;
            height = Math.max(height, level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz));
        }
        return height + .06;
    }
    static List<MapSceneMesh.MapTile> extract() {
        var client = Minecraft.getInstance();
        var view = MapAgencyHud.active() ? MapAgencyHud.snapshot() : null;
        if (view == null || !view.country().equals(dev.hoi.client.ClientCampaignHud.country())) { invalidate(); return List.of(); }
        long tick = client.level.getGameTime();
        if (view == cached && cachedTick == tick) return meshes;
        double phase = (tick / 20.0 * .2) % 1;
        var result = new ArrayList<MapSceneMesh.MapTile>();
        for (var network : view.networks()) for (var node : network.nodes()) {
            if (node.parent().isEmpty()) continue;
            var parent = network.nodes().stream().filter(value -> value.state().equals(node.parent())).findFirst().orElse(null);
            if (parent == null) continue;
            double fromX = parent.x(), fromZ = parent.z();
            if (!client.level.hasChunk((int)Math.floor(fromX) >> 4, (int)Math.floor(fromZ) >> 4)
                    || !client.level.hasChunk((int)Math.floor(node.x()) >> 4, (int)Math.floor(node.z()) >> 4)) continue;
            double dx = node.x() - fromX, dz = node.z() - fromZ;
            double length = Math.hypot(dx, dz), half = .25 * Math.clamp(length * 4 / 40, .2, 1);
            if (length == 0) continue;
            double nx = -dz / length * half, nz = dx / length * half;
            var vertices = new FloatArrayList(8 * 4 * 8);
            double start = 0;
            while (start < 1) {
                double u = (start * 7 - phase) % 1;
                if (u < 0) u++;
                double endAt = Math.min(1, start + (1 - u) / 7);
                if (endAt - start < .000001) { u = 0; endAt = Math.min(1, start + 1.0 / 7); }
                endAt = Math.min(endAt, start + 1 / Math.min(64, Math.max(1, length)));
                double y0 = surface(fromX + dx * start, fromZ + dz * start);
                double y1 = surface(fromX + dx * endAt, fromZ + dz * endAt);
                if (!Double.isFinite(y0) || !Double.isFinite(y1)) { start = endAt; continue; }
                for (int corner = 0; corner < 4; corner++) {
                    boolean end = corner >= 2, right = corner == 1 || corner == 2;
                    double t = end ? endAt : start;
                    vertices.add((float)(fromX + dx * t + (right ? nx : -nx)));
                    vertices.add((float)Math.max(y0, y1));
                    vertices.add((float)(fromZ + dz * t + (right ? nz : -nz)));
                    vertices.add((float)(end ? Math.min(1, u + (endAt - start) * 7) : u));
                    vertices.add(right ? .95f : .05f);
                    vertices.add(0); vertices.add(1); vertices.add(0);
                }
                start = endAt;
            }
            if (vertices.isEmpty()) continue;
            float[] points = vertices.toFloatArray();
            var bounds = new AABB(points[0], points[1], points[2], points[0], points[1], points[2]);
            for (int i = 8; i < points.length; i += 8) bounds = bounds.minmax(new AABB(points[i], points[i + 1], points[i + 2], points[i], points[i + 1], points[i + 2]));
            result.add(new MapSceneMesh.MapTile(Material.DEPLOYMENT, bounds.inflate(.01), points, List.of()));
        }
        cached = view; cachedTick = tick; meshes = List.copyOf(result); return meshes;
    }
}
