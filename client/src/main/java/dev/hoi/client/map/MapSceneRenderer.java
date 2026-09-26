package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolAtlasScene;
import dev.hoi.protocol.ProtocolAtlasVisibility;
import dev.hoi.protocol.ProtocolAtlasScene.Material;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.data.AtlasIds;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;
import dev.hoi.client.map.MapSceneMesh.MapTile;


public final class MapSceneRenderer {
    private static final RenderStateDataKey<MapRenderFrame> FRAME = RenderStateDataKey.create(() -> "hoi:atlas_scene");
    private static final MapSceneAssembler ASSEMBLER = new MapSceneAssembler();
    private static MapSceneMesh active, deployment;
    private static final MapSceneAssembler DEPLOYMENT = new MapSceneAssembler();
    private static ItemStackRenderState city;
    private static Map<Material, TextureAtlasSprite> sprites;
    private static int visibleTiles, examinedTiles;
    private static final Map<Material, Identifier> TEXTURES = Map.ofEntries(
            Map.entry(Material.DMZ, Identifier.parse("minecraft:block/white_concrete")),
            Map.entry(Material.BLACK, Identifier.parse("minecraft:block/black_concrete")), Map.entry(Material.RED, Identifier.parse("minecraft:block/red_concrete")),
            Map.entry(Material.WATER, Identifier.parse("minecraft:block/water_still")), Map.entry(Material.FOREST, Identifier.parse("minecraft:block/oak_sapling")),
            Map.entry(Material.JUNGLE, Identifier.parse("minecraft:block/jungle_sapling")), Map.entry(Material.MARSH, Identifier.parse("minecraft:block/mud")),
            Map.entry(Material.DESERT, Identifier.parse("minecraft:block/sand")),
            Map.entry(Material.NAVY_RIVER, Identifier.parse("minecraft:block/gray_concrete")),
            Map.entry(Material.AIR_RIVER, Identifier.parse("minecraft:block/light_gray_concrete")),
            Map.entry(Material.GRAY, Identifier.parse("minecraft:block/gray_concrete")), Map.entry(Material.DEPLOYMENT, Identifier.parse("minecraft:block/lime_concrete")));
    private record MapRenderFrame(List<MapTile> tiles, Map<Material, TextureAtlasSprite> sprites, ItemStackRenderState city) {}

    public static void register() {
        ProtocolAtlasScene.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(ProtocolAtlasScene.Page.TYPE, (page, context) -> {
            if (receive(page)) ClientPlayNetworking.send(new ProtocolAtlasScene.Ready(page.scene()));
        });
        dev.hoi.protocol.ProtocolIndustryPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolIndustryPayloads.DeploymentOverlay.TYPE, (packet, context) -> {
            if (!(context.client().gui.screen() instanceof dev.hoi.client.screen.ScreenIndustry screen) || !screen.acceptsDeploymentOverlay(packet.session(), packet.revision())) return;
            var boxes = DEPLOYMENT.accept(packet.page());
            if (boxes != null) deployment = MapSceneMesh.build(packet.page().dimension(), boxes);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            var scene = active;
            String dimension = context.level().dimension().identifier().toString();
            if (!(Minecraft.getInstance().gui.screen() instanceof dev.hoi.client.screen.ScreenIndustry screen) || !screen.selectingDeployment()) clearDeployment();
            if ((scene == null || !dimension.equals(scene.dimension())) && (deployment == null || !dimension.equals(deployment.dimension()))) {
                visibleTiles=0;context.levelState().setData(FRAME, null); return;
            }
            var camera = context.levelState().cameraRenderState;
            var tiles = new ArrayList<MapTile>();examinedTiles=0;
            if (scene != null && dimension.equals(scene.dimension())) examinedTiles+=scene.collect(camera.pos, tiles);
            if (deployment != null && dimension.equals(deployment.dimension())) examinedTiles+=deployment.collect(camera.pos, tiles);
            tiles.removeIf(tile -> !camera.cullFrustum.isVisible(tile.bounds()));
            visibleTiles=tiles.size();
            var client = Minecraft.getInstance();
            if (sprites == null) {
                var atlas = client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
                var resolved = new EnumMap<Material, TextureAtlasSprite>(Material.class);
                TEXTURES.forEach((material, name) -> resolved.put(material, atlas.getSprite(name)));
                sprites = Map.copyOf(resolved);
            }
            if (city == null) {
                city = new ItemStackRenderState(); var item = new ItemStack(Items.PAPER);
                item.set(DataComponents.ITEM_MODEL, Identifier.parse("hoi:map/terrain/city"));
                client.getItemModelResolver().updateForTopItem(city, item, ItemDisplayContext.FIXED, context.level(), null, 0);
            }
            context.levelState().setData(FRAME, new MapRenderFrame(tiles, sprites, city));
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(MapSceneRenderer::draw);
    }

    static boolean receive(ProtocolAtlasScene.Page page) {
        var complete = ASSEMBLER.accept(page);
        if (complete == null) return false;
        active = MapSceneMesh.build(page.dimension(), complete); return true;
    }
    public static void clearDeployment() {deployment = null; DEPLOYMENT.clear();}
    static void clear() { clearDeployment(); active = null; resourcesReloaded(); visibleTiles=0;examinedTiles=0;ASSEMBLER.clear(); }
    public static void resourcesReloaded() { city = null; sprites = null; }
    static int tileCount() { return active == null ? 0 : active.tiles().size(); }
    static int visibleTileCount() { return visibleTiles; }
    static int examinedTileCount() { return examinedTiles; }

    private static void draw(LevelRenderContext context) {
        var frame = context.levelState().getData(FRAME); if (frame == null) return;
        var camera = context.levelState().cameraRenderState.pos;
        var pose = context.poseStack(); pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        for (var tile : frame.tiles()) {
            if (tile.material() == Material.CITY) {
                for (var box : tile.cities()) {
                    if(camera.distanceToSqr(box.x()+box.sx()/2,box.y()+box.sy()/2,box.z()+box.sz()/2)>ProtocolAtlasVisibility.DETAIL_RANGE*ProtocolAtlasVisibility.DETAIL_RANGE)continue;
                    pose.pushPose(); pose.translate(box.x()+box.sx()/2,box.y()+box.sy()/2,box.z()+box.sz()/2); pose.scale(box.sx(),box.sy(),box.sz());
                    frame.city().submit(pose,context.submitNodeCollector(),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);
                    pose.popPose();
                }
                continue;
            }
            var sprite = frame.sprites().get(tile.material());
            var type = tile.material() == Material.DMZ ? RenderTypes.entityTranslucentEmissive(Identifier.parse("minecraft:textures/atlas/blocks.png")) : tile.material() == Material.FOREST || tile.material() == Material.JUNGLE ? RenderTypes.cutoutMovingBlock() : RenderTypes.solidMovingBlock();
            context.submitNodeCollector().submitCustomGeometry(pose, type, (matrix, consumer) -> emit(tile, sprite, matrix, consumer));
        }
        pose.popPose();
    }
    private static void emit(MapTile tile, TextureAtlasSprite sprite, PoseStack.Pose pose, VertexConsumer consumer) {
        int color = tile.material() == Material.DMZ ? 0xfeff0000 : tile.material() == Material.WATER ? 0xff3f76e4 : 0xffffffff;
        var v = tile.vertices();
        for (int i=0;i<v.length;i+=8) consumer.addVertex(pose,v[i],v[i+1],v[i+2]).setColor(color)
                .setUv(sprite.getU(v[i+3]),sprite.getV(v[i+4])).setLight(15728880).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(pose,v[i+5],v[i+6],v[i+7]);
    }
    private MapSceneRenderer() {}
}
