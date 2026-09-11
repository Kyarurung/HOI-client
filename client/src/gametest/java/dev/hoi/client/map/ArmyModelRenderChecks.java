package dev.hoi.client.map;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;

public final class ArmyModelRenderChecks {
    private static final String[] TYPES = {"infantry", "marine", "airborne", "mountaineer",
            "motorized_infantry", "mechanized_infantry", "tank", "light_tank",
            "artillery", "self_propelled_artillery", "anti_tank", "air_defense"};

    private ArmyModelRenderChecks() {}

    public static void run(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for (String type : TYPES) {
                for (int variant = -1; variant < 8; variant++) {
                    String suffix = variant < 0 ? "" : "_" + (variant < 4 ? "moving" : "combat") + "_" + (variant % 4);
                    var resource = Identifier.parse("hoi:models/army/" + type + suffix + ".json");
                    try (var reader = client.getResourceManager().getResourceOrThrow(resource).openAsReader()) {
                        var model = JsonParser.parseReader(reader).getAsJsonObject();
                        int count = model.getAsJsonArray("elements").size();
                        if (count < 120 || count > 200) throw new AssertionError(resource + " cube budget");
                        var texture = Identifier.parse(model.getAsJsonObject("textures").get("body").getAsString());
                        if (client.getResourceManager().getResource(texture.withPath("textures/" + texture.getPath() + ".png")).isEmpty())
                            throw new AssertionError("Missing army texture " + texture);
                    } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
                }
            }
        });
        context.getInput().resizeWindow(1600, 1000);
        try (var world = context.worldBuilder().adjustSettings(settings -> settings.setGameMode(
                net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var server = world.getServer();
            server.runCommand("gamerule minecraft:send_command_feedback false");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamemode spectator @a");
            server.runCommand("fill -14 69 -8 14 69 20 minecraft:gray_concrete");
            context.setScreen(() -> null);
            context.runOnClient(client -> client.options.fov().set(55));
            for (int i = 0; i < TYPES.length; i++) {
                int x = (i % 4) * 4 - 6;
                int z = (i / 4) * 5;
                server.runCommand("summon minecraft:item_display " + x + " 71.5 " + z
                        + " {Tags:[\"hoi_army_" + i + "\"],item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:army/" + TYPES[i]
                        + "\"}},item_display:\"fixed\",transformation:{translation:[0f,0f,0f],scale:[3f,3f,3f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,1f,0f,0f]},width:4f,height:4f,view_range:8f,brightness:{block:15,sky:15}}");
            }
            server.runCommand("tp @a 0 78 -14 0 27");
            context.waitTicks(30);
            context.runOnClient(client -> {
                int models = 0;
                for (var entity : client.level.entitiesForRendering()) if (entity instanceof net.minecraft.world.entity.Display.ItemDisplay display) {
                    var scale = display.renderState().transformation().get(1).scale();
                    if (Math.abs(scale.x()-3) > .001 || Math.abs(scale.y()-3) > .001 || Math.abs(scale.z()-3) > .001)
                        throw new AssertionError("Army fixture has incorrect display scale " + scale);
                    models++;
                }
                if (models != TYPES.length) throw new AssertionError("Missing army displays: " + models);
            });
            context.takeScreenshot("hoi-army-all-twelve-idle");
            server.runCommand("tp @a -7.7 71.25 -3.7 -24 24");
            context.waitTicks(8);
            context.takeScreenshot("hoi-army-infantry-face-and-grip");
            server.runCommand("tp @a 4.7 72.1 1.3 24 17");
            context.waitTicks(8);
            context.takeScreenshot("hoi-army-tank-running-gear");
            server.runCommand("tp @a 0 78 -14 0 27");
            for (String state : new String[]{"moving", "combat"}) {
                for (int frame = 0; frame < 4; frame++) {
                    for (int i = 0; i < TYPES.length; i++) {
                        server.runCommand("data modify entity @e[type=minecraft:item_display,tag=hoi_army_" + i
                                + ",limit=1] item.components.\"minecraft:item_model\" set value \"hoi:army/" + TYPES[i] + "_" + state + "_" + frame + "\"");
                    }
                    context.waitTicks(5);
                    context.takeScreenshot("hoi-army-" + state + "-" + frame);
                }
            }
        }
    }
}
