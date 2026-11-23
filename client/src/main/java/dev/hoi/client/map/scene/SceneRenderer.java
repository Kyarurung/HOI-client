package dev.hoi.client.map.scene;

import dev.hoi.protocol.map.MapAtlasScene;
import dev.hoi.protocol.map.MapAtlasVisibility;
import dev.hoi.protocol.map.MapAtlasScene.Material;
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
import dev.hoi.client.map.scene.SceneMesh.SceneTile;
import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.map.agency.AgencyNetworks;
import dev.hoi.client.map.air.AirArrowOverlay;
import dev.hoi.client.map.air.AirRangeOverlay;
import dev.hoi.client.map.commander.CommanderPanels;
import dev.hoi.client.map.naval.NavalInvasionOverlay;
import dev.hoi.client.map.agency.AgencyRaidHud;
import dev.hoi.client.screen.raid.RaidScreen;
import dev.hoi.client.screen.raid.RaidSources;

public final class SceneRenderer {
    private static final RenderStateDataKey<SceneRenderFrame> FRAME = RenderStateDataKey.create(() -> "hoi:atlas_scene");
    private static final SceneAssembler ASSEMBLER = new SceneAssembler();
    private static SceneMesh active, deployment, occupation, plans;
    private static SceneMesh decision;
    private static int decisionColor=0xffffffff;
    private static String planCountry="";
    private static List<dev.hoi.protocol.commander.CommanderHudPayloads.AirArrow> air=List.of();
    private static final SceneAssembler PLANS=new SceneAssembler();
    private static final SceneAssembler DEPLOYMENT = new SceneAssembler();
    private static final SceneAssembler OCCUPATION = new SceneAssembler();
    private static ItemStackRenderState city;
    private static Map<Material, TextureAtlasSprite> sprites;
    private static int visibleTiles, examinedTiles;
    private static final Map<Material, Identifier> TEXTURES = Map.ofEntries(
            Map.entry(Material.RESISTANCE, Identifier.parse("minecraft:block/white_concrete")),
            Map.entry(Material.COMPLIANCE, Identifier.parse("minecraft:block/white_concrete")),
            Map.entry(Material.DMZ, Identifier.parse("minecraft:block/white_concrete")),
            Map.entry(Material.BLACK, Identifier.parse("minecraft:block/black_concrete")), Map.entry(Material.RED, Identifier.parse("minecraft:block/red_concrete")),
            Map.entry(Material.WATER, Identifier.parse("minecraft:block/water_still")), Map.entry(Material.FOREST, Identifier.parse("minecraft:block/oak_sapling")),
            Map.entry(Material.JUNGLE, Identifier.parse("minecraft:block/jungle_sapling")), Map.entry(Material.MARSH, Identifier.parse("minecraft:block/mud")),
            Map.entry(Material.DESERT, Identifier.parse("minecraft:block/sand")),
            Map.entry(Material.NAVY_RIVER, Identifier.parse("minecraft:block/gray_concrete")),
            Map.entry(Material.AIR_RIVER, Identifier.parse("minecraft:block/light_gray_concrete")),
            Map.entry(Material.GRAY, Identifier.parse("minecraft:block/gray_concrete")), Map.entry(Material.DEPLOYMENT, Identifier.parse("minecraft:block/lime_concrete")));
    private record SceneRaidTile(SceneTile tile, int color) {}
    private record SceneRenderFrame(List<SceneTile> tiles, Map<Material, TextureAtlasSprite> sprites, ItemStackRenderState city,List<SceneTile> decision,int decisionColor,AirRangeOverlay.Frame ranges,List<AirArrowOverlay.Mesh> air,List<SceneTile> networks,List<NavalInvasionOverlay.Mesh> invasions,List<dev.hoi.client.map.air.AirParadropOverlay.Mesh> paradrops,List<SceneRaidTile> raids) {}
    public static void register() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register((world,chunk)->{AirRangeOverlay.clear();AgencyNetworks.invalidate();});
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_UNLOAD.register((world,chunk)->{AirRangeOverlay.clear();AgencyNetworks.invalidate();});
        MapAtlasScene.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasScene.Page.TYPE, (page, context) -> {
            if (receive(page)) ClientPlayNetworking.send(new MapAtlasScene.Ready(page.scene()));
        });
        dev.hoi.protocol.industry.IndustryPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.industry.IndustryPayloads.DeploymentOverlay.TYPE, (packet, context) -> {
            if (!(context.client().gui.screen() instanceof dev.hoi.client.screen.industry.IndustryScreen screen) || !screen.acceptsDeploymentOverlay(packet.session(), packet.revision())) return;
            var boxes = DEPLOYMENT.accept(packet.page());
            if (boxes != null) deployment = SceneMesh.build(packet.page().dimension(), boxes);
        });
        dev.hoi.protocol.occupation.OccupationPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.occupation.OccupationPayloads.Overlay.TYPE,(packet,context)->{
            receiveOccupation(packet.page());
        });
        dev.hoi.protocol.commander.CommanderHudPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.commander.CommanderHudPayloads.PlanOverlay.TYPE,(packet,context)->{
            receivePlans(packet.country(),packet.page(),packet.movement(),packet.air());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(SceneRenderer::clear));
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            if(!planCountry.equals(dev.hoi.client.ClientCampaignHud.country()))clearPlans();
            var scene = active;
            String dimension = context.level().dimension().identifier().toString();
            var ranges=AirRangeOverlay.extract(context.level(),CommanderPanels.airRanges());
            var networks = AgencyNetworks.extract();
            var agency = AgencyHud.highlight();
            var raidSources = dev.hoi.client.screen.dialog.DialogCoordinator.contentScreen() instanceof RaidScreen raid && raid.showingSources()
                    ? raid.sources() : AgencyRaidHud.sources();
            var raidMeshes = raidSources == null ? Map.<String, SceneMesh>of() : raidSources.meshes();
            var invasions=NavalInvasionOverlay.extract();NavalInvasionOverlay.project(invasions);
            var paradrops=dev.hoi.client.map.air.AirParadropOverlay.extract();dev.hoi.client.map.air.AirParadropOverlay.project(paradrops);
            if(!(dev.hoi.client.screen.dialog.DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.decision.DecisionScreen screen)
                    || !screen.mapDimension().equals(dimension))decision=null;
            if (!(Minecraft.getInstance().gui.screen() instanceof dev.hoi.client.screen.industry.IndustryScreen screen) || !screen.selectingDeployment()) clearDeployment();
            if ((scene == null || !dimension.equals(scene.dimension())) && (deployment == null || !dimension.equals(deployment.dimension())) && (occupation == null || !dimension.equals(occupation.dimension())) && (plans == null || !dimension.equals(plans.dimension())) && decision==null && agency==null && ranges.outline().isEmpty() && networks.isEmpty() && invasions.isEmpty() && paradrops.isEmpty() && raidMeshes.isEmpty()) {
                visibleTiles=0;context.levelState().setData(FRAME, null); return;
            }
            var camera = context.levelState().cameraRenderState;
            var tiles = new ArrayList<SceneTile>();examinedTiles=0;
            if (scene != null && dimension.equals(scene.dimension())) examinedTiles+=scene.collect(camera.pos, tiles);
            if (deployment != null && dimension.equals(deployment.dimension())) examinedTiles+=deployment.collect(camera.pos, tiles);
            if (occupation != null && dimension.equals(occupation.dimension())) examinedTiles+=occupation.collect(camera.pos, tiles);
            if(plans!=null&&dimension.equals(plans.dimension()))examinedTiles+=plans.collect(camera.pos,tiles);
            if (agency != null && dimension.equals(agency.dimension())) examinedTiles += agency.collect(camera.pos, tiles);
            tiles.removeIf(tile -> !camera.cullFrustum.isVisible(tile.bounds()));
            var airMeshes=plans!=null&&dimension.equals(plans.dimension())?AirArrowOverlay.build(air,context.level().getGameTime()/20.0).stream().filter(mesh->camera.cullFrustum.isVisible(mesh.tile().bounds())&&mesh.tile().bounds().distanceToSqr(camera.pos)<=MapAtlasVisibility.BOUNDARY_RANGE*MapAtlasVisibility.BOUNDARY_RANGE&&AirArrowOverlay.texture(mesh.kind())!=null).toList():List.<AirArrowOverlay.Mesh>of();
            visibleTiles=tiles.size()+airMeshes.size();
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
            var highlighted=new ArrayList<SceneTile>();
            if(decision!=null)decision.collect(camera.pos,highlighted);
            highlighted.removeIf(tile->!camera.cullFrustum.isVisible(tile.bounds()));
            var networkMeshes = networks.stream().filter(tile -> camera.cullFrustum.isVisible(tile.bounds())
                && tile.bounds().distanceToSqr(camera.pos) <= MapAtlasVisibility.BOUNDARY_RANGE * MapAtlasVisibility.BOUNDARY_RANGE).toList();
            var raidTiles = new ArrayList<SceneRaidTile>();
            raidMeshes.forEach((state, mesh) -> {
                var candidates = new ArrayList<SceneTile>();
                mesh.collect(camera.pos, candidates);
                for (var tile : candidates) if (camera.cullFrustum.isVisible(tile.bounds())) raidTiles.add(new SceneRaidTile(tile, raidSources.color(state)));
            });
            context.levelState().setData(FRAME, new SceneRenderFrame(tiles, sprites, city,highlighted,decisionColor,ranges,airMeshes,networkMeshes,invasions,paradrops,raidTiles));
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(SceneRenderer::draw);
    }
    public static boolean receive(MapAtlasScene.Page page) {
        var complete = ASSEMBLER.accept(page);
        if (complete == null) return false;
        active = SceneMesh.build(page.dimension(), complete); return true;
    }
    public static void clearDeployment() {deployment = null; DEPLOYMENT.clear();}
    public static void decision(SceneMesh mesh,int color) {decision=mesh;decisionColor=color;}
    static void receiveOccupation(MapAtlasScene.Page page) {
        var boxes=OCCUPATION.accept(page);
        if(boxes!=null)occupation=SceneMesh.build(page.dimension(),boxes);
    }
    public static void clearOccupation() {occupation=null;OCCUPATION.clear();}
    static void receivePlans(String country,MapAtlasScene.Page page) {receivePlans(country,page,List.of());}
    public static void receivePlans(String country,MapAtlasScene.Page page,List<dev.hoi.protocol.commander.CommanderHudPayloads.MovementQuad> movement) {
        receivePlans(country,page,movement,List.of());
    }
    static void receivePlans(String country,MapAtlasScene.Page page,List<dev.hoi.protocol.commander.CommanderHudPayloads.MovementQuad> movement,List<dev.hoi.protocol.commander.CommanderHudPayloads.AirArrow> arrows) {
        var client=Minecraft.getInstance();
        if(!country.equals(dev.hoi.client.ClientCampaignHud.country())||client.level==null||!page.dimension().equals(client.level.dimension().identifier().toString()))return;
        if(!planCountry.equals(country))clearPlans();
        planCountry=country;var complete=PLANS.accept(page,movement,arrows);if(complete!=null){plans=SceneMesh.build(page.dimension(),complete.boxes(),complete.movement());air=complete.air();}
    }
    public static void clearPlans(){air=List.of();plans=null;planCountry="";PLANS.clear();}
    public static void clear() { clearDeployment(); clearOccupation(); clearPlans(); decision=null;active = null; resourcesReloaded(); visibleTiles=0;examinedTiles=0;ASSEMBLER.clear(); }
    public static void resourcesReloaded() { city = null; sprites = null; AirRangeOverlay.clear(); AirArrowOverlay.clear(); AgencyNetworks.clear(); NavalInvasionOverlay.clear(); }
    static int tileCount() { return active == null ? 0 : active.tiles().size(); }
    public static int visibleTileCount() { return visibleTiles; }
    static int examinedTileCount() { return examinedTiles; }
    private static void draw(LevelRenderContext context) {
        var frame = context.levelState().getData(FRAME); if (frame == null) return;
        var camera = context.levelState().cameraRenderState.pos;
        var pose = context.poseStack(); pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        for(var tile:frame.ranges().fill())if(context.levelState().cameraRenderState.cullFrustum.isVisible(tile.bounds()))context.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(Identifier.parse("minecraft:textures/atlas/blocks.png")),(matrix,consumer)->emit(tile,frame.sprites().get(Material.COMPLIANCE),matrix,consumer,0x20ffffff));
        for(var tile:frame.ranges().outline())if(context.levelState().cameraRenderState.cullFrustum.isVisible(tile.bounds()))context.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(Identifier.parse("hoi:textures/gui/map/air_range.png")),(matrix,consumer)->emit(tile,null,matrix,consumer,frame.ranges().color()));
        for (var tile : frame.tiles()) {
            if (tile.material() == Material.CITY) {
                for (var box : tile.cities()) {
                    if(camera.distanceToSqr(box.x()+box.sx()/2,box.y()+box.sy()/2,box.z()+box.sz()/2)>MapAtlasVisibility.DETAIL_RANGE*MapAtlasVisibility.DETAIL_RANGE)continue;
                    pose.pushPose(); pose.translate(box.x()+box.sx()/2,box.y()+box.sy()/2,box.z()+box.sz()/2); pose.scale(box.sx(),box.sy(),box.sz());
                    frame.city().submit(pose,context.submitNodeCollector(),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);
                    pose.popPose();
                }
                continue;
            }
            if(tile.movement()){context.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(movementTexture(tile.arrow())),(matrix,consumer)->emit(tile,null,matrix,consumer));continue;}
            var sprite = frame.sprites().get(tile.material());
            var type = tile.material() == Material.DMZ ? RenderTypes.entityTranslucentEmissive(Identifier.parse("minecraft:textures/atlas/blocks.png")) : tile.material() == Material.FOREST || tile.material() == Material.JUNGLE ? RenderTypes.cutoutMovingBlock() : RenderTypes.solidMovingBlock();
            context.submitNodeCollector().submitCustomGeometry(pose, type, (matrix, consumer) -> emit(tile, sprite, matrix, consumer));
        }
        for(var tile:frame.decision())context.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(Identifier.parse("minecraft:textures/atlas/blocks.png")),
                (matrix,consumer)->emit(tile,frame.sprites().get(Material.COMPLIANCE),matrix,consumer,frame.decisionColor()));
        for (var raid : frame.raids()) context.submitNodeCollector().submitCustomGeometry(pose,
                RenderTypes.entityTranslucentEmissive(Identifier.parse("minecraft:textures/atlas/blocks.png")), (matrix, consumer) -> {
            var vertices = raid.tile().vertices(); var sprite = frame.sprites().get(Material.COMPLIANCE);
            for (int i = 0; i < vertices.length; i += 8) {
                int alpha = (int)Math.round((raid.color() >>> 24) * RaidSources.mask(vertices[i + 3], camera.y - vertices[i + 1]));
                consumer.addVertex(matrix, vertices[i], vertices[i + 1], vertices[i + 2]).setColor(alpha << 24 | raid.color() & 0xFFFFFF)
                        .setUv(sprite.getU(.5f), sprite.getV(.5f)).setLight(15728880).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(matrix, 0, 1, 0);
            }
        });
        for(var mesh:frame.air()){var texture=AirArrowOverlay.texture(mesh.kind());if(texture!=null)context.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(texture),(matrix,consumer)->emit(mesh.tile(),null,matrix,consumer,AirArrowOverlay.color(mesh.state())));}
        var invasionTexture=frame.invasions().isEmpty()?null:NavalInvasionOverlay.texture();
        if(invasionTexture!=null)for(var mesh:frame.invasions())context.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(invasionTexture),(matrix,consumer)->emit(mesh.tile(),null,matrix,consumer,NavalInvasionOverlay.color(mesh.plan())));
        var paradropTexture=frame.paradrops().isEmpty()?null:NavalInvasionOverlay.texture();
        if(paradropTexture!=null)for(var mesh:frame.paradrops())context.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(paradropTexture),(matrix,consumer)->emit(mesh.tile(),null,matrix,consumer,dev.hoi.client.map.air.AirParadropOverlay.color(mesh.plan())));
        var networkTexture = frame.networks().isEmpty() ? null : AgencyNetworks.texture();
        if (networkTexture != null) for (var tile : frame.networks()) context.submitNodeCollector().submitCustomGeometry(pose,
            RenderTypes.entityTranslucentEmissive(networkTexture), (matrix, consumer) -> emit(tile, null, matrix, consumer, 0xFFFFFFFF));
        pose.popPose();
    }
    static Identifier movementTexture(dev.hoi.protocol.commander.CommanderHudPayloads.Arrow arrow){return Identifier.parse("hoi:textures/gui/map/"+switch(arrow){case MOVE->"movement_arrow";case MISSION->"mission_arrow";case RETREAT->"retreat_arrow";}+".png");}
    private static void emit(SceneTile tile, TextureAtlasSprite sprite, PoseStack.Pose pose, VertexConsumer consumer) {
        int color = tile.movement()?0xbfffffff:tile.material() == Material.RESISTANCE ? 0xff9b6363 : tile.material() == Material.COMPLIANCE ? 0xff5b668f : tile.material() == Material.DMZ ? 0xfeff0000 : tile.material() == Material.WATER ? 0xff3f76e4 : 0xffffffff;
        emit(tile,sprite,pose,consumer,color);
    }
    private static void emit(SceneTile tile,TextureAtlasSprite sprite,PoseStack.Pose pose,VertexConsumer consumer,int color) {
        var v = tile.vertices();
        for (int i=0;i<v.length;i+=8) consumer.addVertex(pose,v[i],v[i+1],v[i+2]).setColor(color)
                .setUv(sprite==null?v[i+3]:sprite.getU(v[i+3]),sprite==null?v[i+4]:sprite.getV(v[i+4])).setLight(15728880).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(pose,v[i+5],v[i+6],v[i+7]);
    }
    private SceneRenderer() {}
}
