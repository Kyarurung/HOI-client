package dev.hoi.client.map;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class MapCountryNameVisibilityTest {
    public static void run(ClientGameTestContext context) {
        int distance = context.computeOnClient(c -> c.options.renderDistance().get());
        double entityDistance = context.computeOnClient(c -> c.options.entityDistanceScaling().get());
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(c -> { c.options.renderDistance().set(2); c.options.entityDistanceScaling().set(0.5); });
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("execute as @a in hoi:atlas_distant_test run tp @s 32 128 0 0 0");
            world.getServer().runCommand("execute in hoi:atlas_distant_test run worldborder center 1024 1024");
            context.waitFor(c -> c.level != null && MapCountryNameVisibility.atlas(c.level));
            context.runOnClient(c -> { label(c, 900001, true); label(c, 900002, false); });
            context.waitTicks(15);
            context.runOnClient(c -> {
                var name = (Display.TextDisplay)c.level.getEntity(900001);
                var ordinary = (Display.TextDisplay)c.level.getEntity(900002);
                if (name.textRenderState() == null) throw new AssertionError("Unloaded-chunk name metadata must be ticked");
                if (c.level.getChunkSource().getChunk(2, 93, ChunkStatus.FULL, false) != null) throw new AssertionError("Far label chunk must stay unloaded");
                if (!name.shouldRenderAtSqrDistance(1.0e12) || ordinary.shouldRenderAtSqrDistance(1.0e12)) throw new AssertionError("Only atlas names ignore entity distance");
                var camera = c.gameRenderer.mainCamera(); var pos = camera.position();
                if (!c.levelExtractor.isEntityVisible(name, camera.getCullFrustum(), pos.x, pos.y, pos.z, 1, 0)) throw new AssertionError("Far atlas name must pass chunk and frustum checks");
                if (c.levelExtractor.isEntityVisible(ordinary, camera.getCullFrustum(), pos.x, pos.y, pos.z, 1, 0)) throw new AssertionError("Ordinary displays keep normal culling");
                if (c.options.renderDistance().get() != 2 || c.options.entityDistanceScaling().get() != 0.5) throw new AssertionError("Viewing preferences changed");
            });
            whitePixels(context.takeScreenshot("hoi-country-name-unloaded-chunk-distance-2"));
            context.runOnClient(c -> c.options.renderDistance().set(16));
            context.waitTicks(10);
            whitePixels(context.takeScreenshot("hoi-country-name-unloaded-chunk-distance-16"));
            world.getServer().runCommand("execute as @a in minecraft:overworld run tp @s 32 128 0 0 0");
            context.waitFor(c -> !MapCountryNameVisibility.atlas(c.level));
            context.runOnClient(c -> label(c, 900003, true));
            context.waitTicks(3);
            context.runOnClient(c -> {
                var name = c.level.getEntity(900003);
                if (MapCountryNameVisibility.independent(name) || name.shouldRenderAtSqrDistance(1.0e12)) throw new AssertionError("Ordinary worlds keep text distance rules");
            });
        } finally {
            context.runOnClient(c -> { c.options.renderDistance().set(distance); c.options.entityDistanceScaling().set(entityDistance); });
        }
    }
    private static void label(Minecraft client, int id, boolean country) {
        var text = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, client.level);
        text.setId(id); text.setPos(32, 128, 1500);
        var name = Component.literal("대한민국");
        if (country) name.withStyle(s -> s.withFont(new FontDescription.Resource(Identifier.parse("hoi:country_names"))));
        set(text, Display.TextDisplay.class, "setText", Component.class, name);
        set(text, Display.TextDisplay.class, "setLineWidth", int.class, 1024);
        set(text, Display.TextDisplay.class, "setBackgroundColor", int.class, 0);
        set(text, Display.TextDisplay.class, "setFlags", byte.class, Display.TextDisplay.FLAG_SEE_THROUGH);
        set(text, Display.class, "setBillboardConstraints", Display.BillboardConstraints.class, Display.BillboardConstraints.CENTER);
        set(text, Display.class, "setViewRange", float.class, 64f);
        set(text, Display.class, "setWidth", float.class, 64f);
        set(text, Display.class, "setHeight", float.class, 64f);
        set(text, Display.class, "setBrightnessOverride", net.minecraft.util.Brightness.class, new net.minecraft.util.Brightness(15, 15));
        set(text, Display.class, "setTransformation", com.mojang.math.Transformation.class,
                new com.mojang.math.Transformation(new org.joml.Vector3f(-180, 0, 0), null, new org.joml.Vector3f(512), null));
        client.level.addEntity(text);
    }
    private static void set(Object target, Class<?> owner, String name, Class<?> type, Object value) {
        try { var method = owner.getDeclaredMethod(name, type); method.setAccessible(true); method.invoke(target, value); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void whitePixels(java.nio.file.Path path) {
        try {
            var image = javax.imageio.ImageIO.read(path.toFile()); int count = 0;
            for (int y = image.getHeight() / 4; y < image.getHeight() * 3 / 4; y++) for (int x = image.getWidth() / 4; x < image.getWidth() * 3 / 4; x++) {
                int rgb = image.getRGB(x, y);
                if ((rgb >> 16 & 255) > 230 && (rgb >> 8 & 255) > 230 && (rgb & 255) > 230) count++;
            }
            if (count < 100) throw new AssertionError("Far country name must produce visible white glyphs: " + count);
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    private MapCountryNameVisibilityTest() {}
}
