package dev.hoi.client;

import dev.hoi.protocol.AtlasSceneProtocol;
import dev.hoi.protocol.AtlasVisibility;
import dev.hoi.protocol.AtlasSceneProtocol.Box;
import dev.hoi.protocol.AtlasSceneProtocol.Material;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Static meshes, no world/display entities. Vanilla atlas animation supplies the river texture. */
final class AtlasSceneClient {
    private static final RenderStateDataKey<Frame> FRAME = RenderStateDataKey.create(() -> "hoi:atlas_scene");
    private static final AtlasSceneAssembler ASSEMBLER = new AtlasSceneAssembler();
    private static Scene active;
    private static ItemStackRenderState city;
    private static int visibleTiles, examinedTiles;
    private static final Map<Material, Identifier> TEXTURES = Map.of(
            Material.BLACK, Identifier.parse("minecraft:block/black_concrete"), Material.RED, Identifier.parse("minecraft:block/red_concrete"),
            Material.WATER, Identifier.parse("minecraft:block/water_still"), Material.FOREST, Identifier.parse("minecraft:block/oak_sapling"),
            Material.JUNGLE, Identifier.parse("minecraft:block/jungle_sapling"), Material.MARSH, Identifier.parse("minecraft:block/mud"),
            Material.DESERT, Identifier.parse("minecraft:block/sand"),
            Material.NAVY_RIVER, Identifier.parse("minecraft:block/gray_concrete"),
            Material.AIR_RIVER, Identifier.parse("minecraft:block/light_gray_concrete"));
    private record Tile(Material material, AABB bounds, float[] vertices, List<Box> cities) {}
    private record Scene(String dimension, List<Tile> tiles, Map<Long,List<Integer>> cells, List<Integer> large) {}
    private record Frame(List<Tile> tiles, Map<Material, TextureAtlasSprite> sprites, ItemStackRenderState city) {}
    private record Key(int x, int z, Material material) {}

    static void register() {
        AtlasSceneProtocol.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(AtlasSceneProtocol.Page.TYPE, (page, context) -> {
            if (receive(page)) ClientPlayNetworking.send(new AtlasSceneProtocol.Ready(page.scene()));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            var scene = active;
            if (scene == null || !context.level().dimension().identifier().toString().equals(scene.dimension())) {
                visibleTiles=0;context.levelState().setData(FRAME, null); return;
            }
            var camera = context.levelState().cameraRenderState;
            var tiles = nearby(scene, camera.pos).stream().filter(tile -> camera.cullFrustum.isVisible(tile.bounds())).toList();
            visibleTiles=tiles.size();
            var client = Minecraft.getInstance();
            var atlas = client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
            var sprites = new EnumMap<Material, TextureAtlasSprite>(Material.class);
            TEXTURES.forEach((material, name) -> sprites.put(material, atlas.getSprite(name)));
            if (city == null) {
                city = new ItemStackRenderState(); var item = new ItemStack(Items.PAPER);
                item.set(DataComponents.ITEM_MODEL, Identifier.parse("hoi:map/terrain/city"));
                client.getItemModelResolver().updateForTopItem(city, item, ItemDisplayContext.FIXED, context.level(), null, 0);
            }
            context.levelState().setData(FRAME, new Frame(tiles, Map.copyOf(sprites), city));
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(AtlasSceneClient::draw);
    }

    static boolean receive(AtlasSceneProtocol.Page page) {
        var complete = ASSEMBLER.accept(page);
        if (complete == null) return false;
        active = mesh(page.dimension(), complete); return true;
    }
    static void clear() { active = null; city = null; visibleTiles=0;examinedTiles=0;ASSEMBLER.clear(); }
    static void resourcesReloaded() { city = null; }
    static int tileCount() { return active == null ? 0 : active.tiles().size(); }
    static int visibleTileCount() { return visibleTiles; }
    static int examinedTileCount() { return examinedTiles; }

    private static Scene mesh(String dimension, List<Box> boxes) {
        var groups = new LinkedHashMap<Key, List<Box>>();
        for (var box : boxes) groups.computeIfAbsent(new Key((int)Math.floor(box.x()/16), (int)Math.floor(box.z()/16), box.material()), unused -> new ArrayList<>()).add(box);
        var tiles = new ArrayList<Tile>();
        groups.forEach((key, group) -> {
            var vertices = new FloatArrayList();
            AABB bounds = null;
            for (var box : group) {
                double radius = Math.hypot(box.sx(), box.sz());
                var extent = new AABB(box.x()-radius, box.y(), box.z()-radius, box.x()+radius, box.y()+box.sy(), box.z()+radius);
                bounds = bounds == null ? extent : bounds.minmax(extent);
                if (box.material() != Material.CITY) append(vertices, box);
            }
            tiles.add(new Tile(key.material(), bounds, vertices.toFloatArray(), key.material() == Material.CITY ? List.copyOf(group) : List.of()));
        });
        var cells = new HashMap<Long,List<Integer>>();var large=new ArrayList<Integer>();
        for (int i=0;i<tiles.size();i++) {
            var bounds=tiles.get(i).bounds();
            // Keep memory linear even for the largest legal network boxes; long tiles use exact fallback checks.
            if((long)(cell(bounds.maxX)-cell(bounds.minX)+1)*(cell(bounds.maxZ)-cell(bounds.minZ)+1)>64) {
                large.add(i);continue;
            }
            for(int z=cell(bounds.minZ);z<=cell(bounds.maxZ);z++)for(int x=cell(bounds.minX);x<=cell(bounds.maxX);x++)
                cells.computeIfAbsent(cellKey(x,z),unused->new ArrayList<>()).add(i);
        }
        return new Scene(dimension, List.copyOf(tiles), cells, List.copyOf(large));
    }

    private static int cell(double value) { return (int)Math.floor(value/16); }
    private static long cellKey(int x,int z) { return ((long)x<<32)|(z&0xffffffffL); }
    private static List<Tile> nearby(Scene scene,Vec3 camera) {
        double radius=Math.max(AtlasVisibility.BOUNDARY_RANGE,AtlasVisibility.DETAIL_RANGE);
        var seen=new LinkedHashSet<Integer>(scene.large());var result=new ArrayList<Tile>();
        for(int z=cell(camera.z-radius);z<=cell(camera.z+radius);z++)for(int x=cell(camera.x-radius);x<=cell(camera.x+radius);x++) {
            var entries=scene.cells().get(cellKey(x,z));if(entries!=null)seen.addAll(entries);
        }
        for(int id:seen) {
            var tile=scene.tiles().get(id);double range=AtlasVisibility.range(tile.material());
            if(tile.bounds().distanceToSqr(camera)<=range*range)result.add(tile);
        }
        examinedTiles=seen.size();
        return result;
    }

    private static void append(FloatArrayList output, Box box) {
        if (box.material() == Material.FOREST || box.material() == Material.JUNGLE) {
            face(output, box, new float[][]{{0,1,0},{0,0,0},{1,0,1},{1,1,1}}, 0,0,1);
            face(output, box, new float[][]{{1,1,0},{1,0,0},{0,0,1},{0,1,1}}, 0,0,1);
            face(output, box, new float[][]{{1,1,1},{1,0,1},{0,0,0},{0,1,0}}, 0,0,-1);
            face(output, box, new float[][]{{0,1,1},{0,0,1},{1,0,0},{1,1,0}}, 0,0,-1);
            return;
        }
        face(output, box, new float[][]{{0,1,0},{0,1,1},{1,1,1},{1,1,0}}, 0,1,0);
        face(output, box, new float[][]{{0,0,1},{0,0,0},{1,0,0},{1,0,1}}, 0,-1,0);
        face(output, box, new float[][]{{1,1,0},{1,0,0},{0,0,0},{0,1,0}}, 0,0,-1);
        face(output, box, new float[][]{{0,1,1},{0,0,1},{1,0,1},{1,1,1}}, 0,0,1);
        face(output, box, new float[][]{{0,1,0},{0,0,0},{0,0,1},{0,1,1}}, -1,0,0);
        face(output, box, new float[][]{{1,1,1},{1,0,1},{1,0,0},{1,1,0}}, 1,0,0);
    }
    private static void face(FloatArrayList output, Box box, float[][] points, float nx, float ny, float nz) {
        float sin = (float)Math.sin(box.yaw()), cos = (float)Math.cos(box.yaw());
        for (int i = 0; i < 4; i++) {
            float x = points[i][0]*box.sx(), z = points[i][2]*box.sz();
            output.add(box.x()+x*cos+z*sin); output.add(box.y()+points[i][1]*box.sy()); output.add(box.z()-x*sin+z*cos);
            output.add(i<2?0:1); output.add(i==0||i==3?0:1);
            output.add(nx*cos+nz*sin); output.add(ny); output.add(-nx*sin+nz*cos);
        }
    }
    private static void draw(LevelRenderContext context) {
        var frame = context.levelState().getData(FRAME); if (frame == null) return;
        var camera = context.levelState().cameraRenderState.pos;
        var pose = context.poseStack(); pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        for (var tile : frame.tiles()) {
            if (tile.material() == Material.CITY) {
                for (var box : tile.cities()) {
                    if(camera.distanceToSqr(box.x()+box.sx()/2,box.y()+box.sy()/2,box.z()+box.sz()/2)>AtlasVisibility.DETAIL_RANGE*AtlasVisibility.DETAIL_RANGE)continue;
                    pose.pushPose(); pose.translate(box.x()+box.sx()/2,box.y()+box.sy()/2,box.z()+box.sz()/2); pose.scale(box.sx(),box.sy(),box.sz());
                    frame.city().submit(pose,context.submitNodeCollector(),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);
                    pose.popPose();
                }
                continue;
            }
            var sprite = frame.sprites().get(tile.material());
            var type = tile.material() == Material.FOREST || tile.material() == Material.JUNGLE ? RenderTypes.cutoutMovingBlock() : RenderTypes.solidMovingBlock();
            context.submitNodeCollector().submitCustomGeometry(pose, type, (matrix, consumer) -> emit(tile, sprite, matrix, consumer));
        }
        pose.popPose();
    }
    private static void emit(Tile tile, TextureAtlasSprite sprite, PoseStack.Pose pose, VertexConsumer consumer) {
        int color = tile.material() == Material.WATER ? 0xff3f76e4 : 0xffffffff;
        var v = tile.vertices();
        for (int i=0;i<v.length;i+=8) consumer.addVertex(pose,v[i],v[i+1],v[i+2]).setColor(color)
                .setUv(sprite.getU(v[i+3]),sprite.getV(v[i+4])).setLight(15728880).setNormal(pose,v[i+5],v[i+6],v[i+7]);
    }
    private AtlasSceneClient() {}
}
