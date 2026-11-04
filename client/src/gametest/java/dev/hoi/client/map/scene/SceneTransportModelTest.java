package dev.hoi.client.map.scene;

import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class SceneTransportModelTest {
    private SceneTransportModelTest() {}
    public static void run(ClientGameTestContext context, Consumer<String> command) {
        int scale = context.computeOnClient(client -> client.options.guiScale().get());
        boolean hidden = context.computeOnClient(client -> client.gui.hud.isHidden());
        var heard = new java.util.ArrayList<net.minecraft.client.resources.sounds.SoundInstance>();
        net.minecraft.client.sounds.SoundEventListener listener = (sound,event,range) -> {
            if (sound.getIdentifier().toString().equals("hoi:ui.order.convoy_transport")) heard.add(sound);
        };
        try {
            context.runOnClient(client -> {
                client.getSoundManager().addListener(listener);
                dev.hoi.client.audio.AudioUiSounds.receive(dev.hoi.protocol.audio.AudioPayloads.Cue.ORDER_CONVOY_TRANSPORT);
                if (heard.size()!=1 || heard.getFirst().getSource()!=net.minecraft.sounds.SoundSource.MASTER) throw new AssertionError("Approved convoy cue must play the original sound once on Master");
            });
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (!client.getSoundManager().isActive(heard.getFirst())) throw new AssertionError("Convoy audio must load and play");
            });
            context.setScreen(() -> null);
            command.accept("gamerule minecraft:send_command_feedback false");
            command.accept("time set noon");
            command.accept("weather clear");
            command.accept("gamemode spectator @a");
            context.setScreen(() -> null);
            command.accept("tp @a 18 69.8 28 127 28");
            atlas(context,command);
            command.accept("summon minecraft:item_display 14 67.3 25 {Tags:[\"hoi_transport_preview\"],item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:army/transport\"}},item_display:\"fixed\",transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[3f,3f,3f]},brightness:{block:15,sky:15}}");
            command.accept("tp @a 18 69.8 28 127 28");
            context.runOnClient(client -> {
                client.options.guiScale().set(2);
                if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
                for (String suffix : List.of("", "_moving_0", "_moving_1", "_moving_2", "_moving_3", "_combat_0", "_combat_1", "_combat_2", "_combat_3")) {
                    var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("hoi", "models/army/transport" + suffix + ".json");
                    if (client.getResourceManager().getResource(id).isEmpty()) throw new AssertionError("Missing transport model " + id);
                }
            });
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.waitTicks(15);
                context.takeScreenshot("hoi-transport-source-model-fixture-" + size[0] + "-gui2");
            }
        } finally {
            command.accept("kill @e[tag=hoi_transport_preview]");
            context.runOnClient(client -> {
                SceneRenderer.clear();
                client.getSoundManager().removeListener(listener);
                heard.forEach(client.getSoundManager()::stop);
                client.options.guiScale().set(scale);
                if (client.gui.hud.isHidden() != hidden) client.gui.hud.toggle();
            });
        }
    }
    private static void atlas(ClientGameTestContext context,Consumer<String> command) {
        try(var input=SceneTransportModelTest.class.getResourceAsStream("/atlas-client-scene.json")) {
            if(input==null)throw new AssertionError("Production Korean atlas fixture missing");
            var scene=com.google.gson.JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var data=scene.getAsJsonObject("scenes").getAsJsonObject("ARMY");
            int size=scene.get("size").getAsInt();
            var blocks=new String[size*size];
            for(var value:data.getAsJsonArray("cells")){var cell=value.getAsJsonArray();blocks[cell.get(1).getAsInt()*size+cell.get(0).getAsInt()]=cell.get(2).getAsString();}
            if(!blocks[25*size+14].startsWith("minecraft:water"))throw new AssertionError("Transport must be placed on actual atlas sea");
            command.accept("fill 0 64 0 "+(size-1)+" 64 "+(size-1)+" minecraft:stone strict");
            for(int z=0;z<size;z++)for(int x=0;x<size;){int end=x+1;while(end<size&&blocks[z*size+end].equals(blocks[z*size+x]))end++;command.accept("fill "+x+" 65 "+z+" "+(end-1)+" 65 "+z+" "+blocks[z*size+x]+" strict");x=end;}
            for(var value:data.getAsJsonArray("relief")){var cell=value.getAsJsonArray();command.accept("setblock "+cell.get(0).getAsInt()+" "+cell.get(3).getAsInt()+" "+cell.get(1).getAsInt()+" "+cell.get(2).getAsString()+" strict");}
            var boxes=new java.util.ArrayList<dev.hoi.protocol.map.MapAtlasScene.Box>();
            for(var value:data.getAsJsonArray("meshes")){var b=value.getAsJsonArray();boxes.add(new dev.hoi.protocol.map.MapAtlasScene.Box(b.get(0).getAsFloat(),b.get(1).getAsFloat(),b.get(2).getAsFloat(),b.get(3).getAsFloat(),b.get(4).getAsFloat(),b.get(5).getAsFloat(),b.get(6).getAsFloat(),dev.hoi.protocol.map.MapAtlasScene.Material.valueOf(b.get(7).getAsString())));}
            context.runOnClient(client->{SceneRenderer.clear();var id=java.util.UUID.randomUUID();int count=(boxes.size()+1023)/1024;for(int i=0;i<count;i++)SceneRenderer.receive(new dev.hoi.protocol.map.MapAtlasScene.Page(id,"minecraft:overworld",i,count,boxes.subList(i*1024,Math.min((i+1)*1024,boxes.size()))));});
            context.waitFor(client->SceneRenderer.visibleTileCount()>0);
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
}
