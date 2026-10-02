package dev.hoi.client.map;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;

public final class MapChunkFadeTest {
    public static void run(net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(MapChunkFadeTest::verify);
            world.getServer().runCommand("execute as @a in hoi:atlas_distant_test run tp @s 0 100 0");
            context.waitFor(client -> client.level.dimension().identifier().toString().equals("hoi:atlas_distant_test"));
            context.runOnClient(MapChunkFadeTest::verify);
        }
    }
    private static void verify(Minecraft client) {
        double original = client.options.chunkSectionFadeInTime().get();
        try {
            for (double preference : new double[] {0.0, 0.75, 2.0}) {
                client.options.chunkSectionFadeInTime().set(preference);
                if (FabricLoader.getInstance().isModLoaded("sodium")) {
                    Class<?> type = Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.RenderSection");
                    Class<?> region = Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion");
                    Object section = type.getConstructor(region, int.class, int.class, int.class).newInstance(null, 0, 4, 0);
                    for (int draw = 0; draw < 2; draw++) {
                        if ((boolean) type.getMethod("consumeFade").invoke(section)) throw new AssertionError("Sodium must not schedule chunk fade");
                    }
                } else {
                    var field = client.levelRenderer.getClass().getDeclaredField("sectionRenderDispatcher");
                    field.setAccessible(true);
                    var dispatcher = (SectionRenderDispatcher) field.get(client.levelRenderer);
                    var section = dispatcher.new RenderSection(0, SectionPos.asLong(0, 4, 0));
                    try {
                        for (long age : new long[] {0, 1, 375, 2001}) {
                            if (section.getVisibility(age, (long) (preference * 1000)) != 1.0f) throw new AssertionError("New native sections must be fully visible immediately");
                        }
                    } finally { section.reset(); }
                }
                client.options.save();
                var saved = java.nio.file.Files.readAllLines(client.gameDirectory.toPath().resolve("options.txt"));
                if (!saved.contains("chunkSectionFadeInTime:" + preference)) throw new AssertionError("Saved fade preference changed");
                if (client.options.chunkSectionFadeInTime().get() != preference) throw new AssertionError("In-memory fade preference changed");
            }
        } catch (ReflectiveOperationException | java.io.IOException e) {
            throw new AssertionError("Chunk fade fixture failed", e);
        } finally {
            client.options.chunkSectionFadeInTime().set(original);
            client.options.save();
        }
    }
    private MapChunkFadeTest() {}
}
