package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolAtlasTerrain;
import dev.hoi.protocol.ProtocolAtlasScene;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class MapDistantTerrainRenderer {
    private record MapTerrainTile(int x, int z, int state, MapSceneMesh.MapTile mesh) {}
    private record MapTerrainDraw(MapSceneMesh.MapTile mesh, TextureAtlasSprite sprite, int color) {}
    private static final RenderStateDataKey<List<MapTerrainDraw>> FRAME = RenderStateDataKey.create(() -> "hoi:distant_terrain");
    private static final MapDistantTerrainAssembler ASSEMBLER = new MapDistantTerrainAssembler();
    private static final Map<Integer, TextureAtlasSprite> SPRITES = new HashMap<>();
    private static ClientLevel owner;
    private static List<MapTerrainTile> tiles = List.of();
    private static boolean complete;
    private static int visible;

    public static void register() {
        ProtocolAtlasTerrain.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(ProtocolAtlasTerrain.Page.TYPE, (page, context) -> receive(page));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientTickEvents.START_CLIENT_TICK.register(client -> { if (owner != null && client.level != owner) clear(); });
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            var client = Minecraft.getInstance();
            if (!active()) { context.levelState().setData(FRAME, null); visible = 0; return; }
            var camera = context.levelState().cameraRenderState;
            double range = client.options.renderDistance().get() * 16.0;
            var draws = new ArrayList<MapTerrainDraw>();
            for (var tile : tiles) {
                if (tile.mesh.bounds().distanceToSqr(camera.pos) > range * range || !camera.cullFrustum.isVisible(tile.mesh.bounds())) continue;
                if (owner.getChunkSource().getChunk(tile.x, tile.z, ChunkStatus.FULL, false) != null) continue;
                var state = Block.stateById(tile.state);
                var sprite = SPRITES.computeIfAbsent(tile.state, unused -> state.is(Blocks.WATER)
                        ? client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Identifier.parse("minecraft:block/water_still"))
                        : client.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite());
                draws.add(new MapTerrainDraw(tile.mesh, sprite, state.is(Blocks.WATER) ? 0xff3f76e4 : 0xffffffff));
            }
            visible = draws.size(); context.levelState().setData(FRAME, List.copyOf(draws));
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            var draws = context.levelState().getData(FRAME); if (draws == null || draws.isEmpty()) return;
            var camera = context.levelState().cameraRenderState.pos;
            var pose = context.poseStack(); pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
            for (var draw : draws) context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.solidMovingBlock(), (matrix, consumer) -> {
                var v = draw.mesh.vertices();
                for (int i = 0; i < v.length; i += 8) consumer.addVertex(matrix, v[i], v[i+1], v[i+2]).setColor(draw.color)
                        .setUv(draw.sprite.getU(v[i+3]), draw.sprite.getV(v[i+4])).setLight(15728880)
                        .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(matrix, v[i+5], v[i+6], v[i+7]);
            });
            pose.popPose();
        });
    }

    static void receive(ProtocolAtlasTerrain.Page page) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().identifier().toString().equals(page.dimension())) return;
        if (owner != level) clear();
        owner = level;
        if (page.index() == 0) { complete = false; tiles = List.of(); }
        var strips = ASSEMBLER.accept(page);
        if (strips == null) return;
        var groups = new LinkedHashMap<Integer, List<ProtocolAtlasScene.Box>>();
        for (var s : strips) {
            var state = Block.stateById(s.state());
            if (Block.getId(state) != s.state() || state.isAir()) { clear(); throw new IllegalArgumentException("Unknown atlas terrain block"); }
            float top = s.top() - (state.is(Blocks.WATER) ? .125f : 0);
            groups.computeIfAbsent(s.state(), unused -> new ArrayList<>()).add(new ProtocolAtlasScene.Box(s.x(), s.bottom(), s.z(),
                    s.width(), top - s.bottom(), 1, 0, ProtocolAtlasScene.Material.GRAY));
        }
        var result = new ArrayList<MapTerrainTile>();
        groups.forEach((state, boxes) -> {
            for (var tile : MapSceneMesh.build(page.dimension(), boxes).tiles()) {
                var v = tile.vertices();
                result.add(new MapTerrainTile((int)Math.floor(v[0] / 16), (int)Math.floor(v[2] / 16), state, tile));
            }
        });
        tiles = List.copyOf(result); complete = true;
    }
    public static boolean active() { return complete && owner != null && owner == Minecraft.getInstance().level; }
    public static void resourcesReloaded() { SPRITES.clear(); }
    static int visibleTiles() { return visible; }
    static void clear() { owner = null; tiles = List.of(); complete = false; visible = 0; ASSEMBLER.clear(); SPRITES.clear(); }
    private MapDistantTerrainRenderer() {}
}
