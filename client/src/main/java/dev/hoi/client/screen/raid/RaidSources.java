package dev.hoi.client.screen.raid;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.map.scene.SceneAssembler;
import dev.hoi.client.map.scene.SceneMesh;
import dev.hoi.protocol.map.MapAtlasScene;
import dev.hoi.protocol.raid.RaidPayloads;
import dev.hoi.protocol.raid.RaidView;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RaidSources {
    private final Map<String, SceneAssembler> staging = new LinkedHashMap<>();
    private final Map<String, SceneMesh> meshes = new LinkedHashMap<>();
    private String dimension = "", hovered = "";
    public void clear() { staging.clear(); meshes.clear(); dimension = ""; hovered = ""; }
    public void accept(RaidView view, RaidPayloads.Highlight packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || view == null || view.setup() == null || !view.country().equals(ClientCampaignHud.country())
                || !view.session().equals(packet.session()) || view.revision() != packet.revision()
                || !level.dimension().identifier().toString().equals(packet.page().dimension())
                || view.setup().sources().stream().noneMatch(source -> source.state().equals(packet.state()))) return;
        if (!dimension.equals(packet.page().dimension())) { clear(); dimension = packet.page().dimension(); }
        if (packet.page().boxes().stream().anyMatch(box -> box.material() != MapAtlasScene.Material.DEPLOYMENT)) return;
        var boxes = staging.computeIfAbsent(packet.state(), unused -> new SceneAssembler()).accept(packet.page());
        if (boxes != null) meshes.put(packet.state(), build(dimension, boxes));
    }
    public Map<String, SceneMesh> meshes() {
        var level = Minecraft.getInstance().level;
        if (level == null || !dimension.equals(level.dimension().identifier().toString())) clear();
        return Map.copyOf(meshes);
    }
    public void hover(String state) { hovered = state; }
    public int color(String state) { return state.equals(hovered) ? 0xB34DFFFF : 0x6600FFFF; }
    public void hoverAt(double x, double y, int width, int height) {
        var camera = Minecraft.getInstance().gameRenderer.mainCamera();
        var ray = camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert()
                .transformProject(new org.joml.Vector3f((float)(x / width * 2 - 1), (float)(1 - y / height * 2), 1)).normalize();
        var from = camera.position();
        var to = from.add(ray.x * 2048, ray.y * 2048, ray.z * 2048);
        double closest = Double.POSITIVE_INFINITY;
        for (var entry : meshes.entrySet()) for (var tile : entry.getValue().tiles()) {
            if (tile.bounds().clip(from, to).isEmpty()) continue;
            var vertices = tile.vertices();
            for (int i = 0; i < vertices.length; i += 32) {
                if (Math.abs(ray.y) < .00001) continue;
                double t = (vertices[i + 1] - from.y) / ray.y;
                if (t < 0 || t >= closest) continue;
                double px = from.x + ray.x * t, pz = from.z + ray.z * t;
                double ax = vertices[i + 24] - vertices[i], az = vertices[i + 26] - vertices[i + 2];
                double bx = vertices[i + 8] - vertices[i], bz = vertices[i + 10] - vertices[i + 2];
                double dx = px - vertices[i], dz = pz - vertices[i + 2], determinant = ax * bz - az * bx;
                if (Math.abs(determinant) < .000001) continue;
                double u = (dx * bz - dz * bx) / determinant, v = (ax * dz - az * dx) / determinant;
                if (u >= 0 && u <= 1 && v >= 0 && v <= 1) { closest = t; hovered = entry.getKey(); }
            }
        }
    }
    public static double mask(double phase, double height) {
        double far = Math.clamp((height * 4 - 300) / 900, 0, 1);
        double value = Math.clamp((Math.cos(phase) + far * 1.5) * 1.7, 0, 1);
        return value * value * (3 - 2 * value) * (1 - .7 * far);
    }
    private static SceneMesh build(String dimension, List<MapAtlasScene.Box> boxes) {
        var base = SceneMesh.build(dimension, boxes);
        var tiles = new ArrayList<SceneMesh.SceneTile>();
        for (var tile : base.tiles()) {
            var points = new FloatArrayList();
            var vertices = tile.vertices();
            for (int offset = 0; offset < vertices.length; offset += 192) {
                for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) for (int corner = 0; corner < 4; corner++) {
                    float u = (x + (corner >= 2 ? 1 : 0)) / 4f, v = (z + (corner == 1 || corner == 2 ? 1 : 0)) / 4f;
                    float px = vertices[offset] + u * (vertices[offset + 24] - vertices[offset]) + v * (vertices[offset + 8] - vertices[offset]);
                    float pz = vertices[offset + 2] + u * (vertices[offset + 26] - vertices[offset + 2]) + v * (vertices[offset + 10] - vertices[offset + 2]);
                    points.add(px); points.add(vertices[offset + 1]); points.add(pz);
                    points.add((float)(6000 * ((3744 + px * 4 + .5) / 8192 * Math.cos(Math.PI / 8)
                            + (384 + pz * 4 + .5) / 2048 * Math.sin(Math.PI / 8))));
                    points.add(0); points.add(0); points.add(1); points.add(0);
                }
            }
            tiles.add(new SceneMesh.SceneTile(tile.material(), tile.bounds(), points.toFloatArray(), List.of()));
        }
        return new SceneMesh(dimension, List.copyOf(tiles), base.cells(), base.large());
    }
}
