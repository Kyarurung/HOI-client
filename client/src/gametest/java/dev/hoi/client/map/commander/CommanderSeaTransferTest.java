package dev.hoi.client.map.commander;

import com.google.gson.Gson;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.unit.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class CommanderSeaTransferTest {
    record Position(double x, double y, double z) {}
    record Phase(String phase, long gameMinute, int leg, double fraction, boolean atSea, String model, Position position, Double yaw, UnitHud.Counter counter) {}
    record Fixture(String country, String signal, List<Phase> sea) {}
    private CommanderSeaTransferTest() {}
    public static void run(ClientGameTestContext context, Consumer<String> command) {
        int scale = context.computeOnClient(c -> c.options.guiScale().get());
        boolean hidden = context.computeOnClient(c -> c.gui.hud.isHidden());
        var hud = context.computeOnClient(c -> ClientCampaignHud.country().isEmpty() ? HudPayloads.State.HIDDEN : HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()));
        try {
            var source = Path.of(System.getenv("HOI_MISSION_OVERLAY_FIXTURES"), "army-sea-transfer.json");
            var fixture = new Gson().fromJson(Files.readString(source), Fixture.class);
            if (!fixture.signal().equals("ORDER_CONVOY_TRANSPORT") || fixture.sea().size() != 3) throw new AssertionError("Actual cue and three producer phases required");
            if (!fixture.sea().stream().map(Phase::model).toList().equals(List.of("hoi:army/self_propelled_artillery", "hoi:army/transport", "hoi:army/self_propelled_artillery"))) throw new AssertionError("Models must switch only at the completed sea/arrival legs");
            context.setScreen(() -> null);
            context.runOnClient(c -> { c.options.guiScale().set(2); if (c.gui.hud.isHidden()) c.gui.hud.toggle(); ClientCampaignHud.accept(HudPayloads.State.of(fixture.country(), CountryHud.UNKNOWN)); CommanderHud.reset(); CommanderUnitHud.reset(); });
            command.accept("gamemode spectator @a"); command.accept("time set noon"); command.accept("weather clear");
            command.accept("fill -4 249 -4 12 249 12 minecraft:light_blue_concrete strict");
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                for (var phase : fixture.sea()) {
                    command.accept("kill @e[tag=hoi_sea_phase_preview]");
                    double yaw = phase.yaw() == null ? 0 : phase.yaw();
                    command.accept("summon minecraft:item_display 4 252 4 {Tags:[\"hoi_sea_phase_preview\"],item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"" + phase.model() + "\"}},item_display:\"fixed\",transformation:{translation:[0f,0f,0f],left_rotation:[0f," + Math.sin(yaw / 2) + "f,0f," + Math.cos(yaw / 2) + "f],right_rotation:[0f,0f,0f,1f],scale:[3f,3f,3f]},brightness:{block:15,sky:15}}");
                    command.accept("tp @a 7 256 9 149 40");
                    var c = phase.counter();
                    if (c.x() != phase.position().x() || c.z() != phase.position().z() || phase.atSea() != phase.phase().equals("first_sea_zone")) throw new AssertionError("Producer model and counter must share a phase position");
                    var counter = new UnitHud.Counter(c.stack(), c.country(), c.countryName(), c.type(), c.name(), c.count(), 4, 252, 4, c.organization(), c.strength(), c.supply(), c.battalions(), c.capitals(), c.status(), c.allegiance(), c.battalionMaximums());
                    context.runOnClient(client -> CommanderUnitHud.accept(new UnitHud.Snapshot("minecraft:overworld", List.of(counter)))); context.waitTicks(8);
                    context.runOnClient(client -> {
                        var box = CommanderUnitHud.boxes().stream().filter(b -> b.counter().stack().equals(c.stack())).findFirst().orElseThrow(() -> new AssertionError("Sea counter disappeared"));
                        if (!CommanderUnitHud.selectAt(box.x() + 2, box.y() + 2) || !CommanderUnitHud.selectedKeys().contains(counter.key())) throw new AssertionError("Army counter must remain selectable at sea and on land");
                    });
                    context.waitTicks(2); var shot = context.takeScreenshot("hoi-sea-producer-" + phase.phase() + "-" + size[0] + "-gui2");
                    Files.writeString(shot.resolveSibling(shot.getFileName().toString().replace(".png", ".json")), new Gson().toJson(phase));
                }
            }
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        finally {
            command.accept("kill @e[tag=hoi_sea_phase_preview]");
            context.runOnClient(c -> { CommanderUnitHud.reset(); CommanderHud.reset(); ClientCampaignHud.accept(hud); c.options.guiScale().set(scale); if (c.gui.hud.isHidden() != hidden) c.gui.hud.toggle(); });
        }
    }
}
