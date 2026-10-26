package dev.hoi.client.map;

import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class MapTransportModelTest {
    private MapTransportModelTest() {}
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
                dev.hoi.client.audio.AudioUiSounds.receive(dev.hoi.protocol.ProtocolAudioPayloads.Cue.ORDER_CONVOY_TRANSPORT);
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
            command.accept("fill -4 249 -4 12 249 12 minecraft:light_blue_concrete strict");
            command.accept("summon minecraft:item_display 4 252 4 {Tags:[\"hoi_transport_preview\"],item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:army/transport\"}},item_display:\"fixed\",transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[3f,3f,3f]},brightness:{block:15,sky:15}}");
            command.accept("tp @a 6.0 252.5 7.0 146 45");
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
                client.getSoundManager().removeListener(listener);
                heard.forEach(client.getSoundManager()::stop);
                client.options.guiScale().set(scale);
                if (client.gui.hud.isHidden() != hidden) client.gui.hud.toggle();
            });
        }
    }
}
