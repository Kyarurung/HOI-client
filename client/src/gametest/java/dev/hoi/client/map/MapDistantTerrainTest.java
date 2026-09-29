package dev.hoi.client.map;

import java.util.*;
import dev.hoi.protocol.ProtocolAtlasTerrain;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class MapDistantTerrainTest {
    public static void run(ClientGameTestContext context) {
        int distance = context.computeOnClient(client -> client.options.renderDistance().get());
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> client.options.renderDistance().set(32));
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("execute as @a in hoi:atlas_distant_test run tp @s 32 100 0 0 5");
            world.getServer().runCommand("execute in hoi:atlas_distant_test run time set noon");
            context.waitFor(client -> client.level != null && client.level.dimension().identifier().toString().equals("hoi:atlas_distant_test"));
            context.runOnClient(client -> {
                client.options.setServerRenderDistance(2);
                if (client.options.getEffectiveRenderDistance() != 2) throw new AssertionError("Uninitialized atlas retains normal server cap");
                receive(Blocks.CONCRETE.red().defaultBlockState());
                if (!MapDistantTerrainRenderer.active() || client.options.getEffectiveRenderDistance() != 32) throw new AssertionError("Atlas enables client viewing distance");
            });
            context.waitFor(client -> MapDistantTerrainRenderer.visibleTiles() > 0);
            context.waitTicks(10);
            context.takeScreenshot("hoi-distant-terrain-red-beyond-server-distance");
            context.runOnClient(client -> receive(Blocks.CONCRETE.blue().defaultBlockState()));
            context.waitTicks(5);
            context.takeScreenshot("hoi-distant-terrain-updated-blue");
            context.runOnClient(client -> {
                MapDistantTerrainRenderer.receive(new ProtocolAtlasTerrain.Page(UUID.randomUUID(), "hoi:atlas_distant_test", 0, 2, List.of()));
                if (MapDistantTerrainRenderer.active()) throw new AssertionError("Pending revision hides outdated colours");
                receive(Blocks.CONCRETE.blue().defaultBlockState());
            });
            world.getServer().runCommand("execute as @a in minecraft:overworld run tp @s 0 100 0");
            context.waitFor(client -> !client.level.dimension().identifier().toString().startsWith("hoi:atlas_"));
            context.waitTicks(2);
            context.runOnClient(client -> {
                client.options.setServerRenderDistance(2);
                if (MapDistantTerrainRenderer.active() || client.options.getEffectiveRenderDistance() != 2) throw new AssertionError("Ordinary worlds retain server distance");
            });
        } finally { context.runOnClient(client -> { MapDistantTerrainRenderer.clear(); client.options.renderDistance().set(distance); }); }
    }
    private static void receive(net.minecraft.world.level.block.state.BlockState state) {
        var strips = new ArrayList<ProtocolAtlasTerrain.Strip>();
        for (int z = 352; z < 416; z++) for (int x = 0; x < 64; x += 16)
            strips.add(new ProtocolAtlasTerrain.Strip(x, z, 16, 65, 68, Block.getId(state)));
        MapDistantTerrainRenderer.receive(new ProtocolAtlasTerrain.Page(UUID.randomUUID(), "hoi:atlas_distant_test", 0, 1, strips));
    }
    private MapDistantTerrainTest() {}
}
