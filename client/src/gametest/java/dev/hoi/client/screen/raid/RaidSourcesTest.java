package dev.hoi.client.screen.raid;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.map.MapAtlasScene;
import dev.hoi.protocol.raid.RaidPayloads;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class RaidSourcesTest {
    public static void run(ClientGameTestContext context, String directory, int width) throws java.io.IOException {
        var path = Path.of(directory).getParent().resolve("raid-highlight-source-fixtures/prc-raids-setup-highlight.json");
        if (!Files.isRegularFile(path)) throw new AssertionError("Raid source highlight input required");
        var packets = new Gson().fromJson(Files.readString(path), RaidPayloads.Highlight[].class);
        var json = JsonParser.parseString(Files.readString(Path.of(directory, "prc-raids-setup.json"))).getAsJsonObject();
        var view = new RaidPayloads.Response(json.toString()).view();
        var previous = context.computeOnClient(client -> HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()));
        context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of("PRC", previous.hud())));
        try {
            context.setScreen(() -> new RaidScreen(view.session(), view, request -> {}));
            context.runOnClient(client -> {
                var screen = (RaidScreen)client.gui.screen();
                for (var packet : packets) {
                    var page = packet.page();
                    screen.accept(new RaidPayloads.Highlight(view.session(), view.revision(), packet.state(),
                            new MapAtlasScene.Page(page.scene(), client.level.dimension().identifier().toString(), page.index(), page.count(), page.boxes())));
                }
                if (screen.sources().meshes().size() != 50) throw new AssertionError("Expected 50 server candidate states, received " + screen.sources().meshes().size());
                screen.sources().clear();
                var original = packets[0];
                var boxes = original.page().boxes().stream().map(box -> new MapAtlasScene.Box(box.x() - 228, 64.05f, box.z() - 140,
                        box.sx(), box.sy(), box.sz(), box.yaw(), box.material())).toList();
                var page = new MapAtlasScene.Page(UUID.randomUUID(), client.level.dimension().identifier().toString(), 0, 1, boxes);
                for (var invalid : List.of(
                        new RaidPayloads.Highlight(UUID.randomUUID().toString(), view.revision(), original.state(), page),
                        new RaidPayloads.Highlight(view.session(), view.revision() - 1, original.state(), page),
                        new RaidPayloads.Highlight(view.session(), view.revision(), "hoi:unknown", page),
                        new RaidPayloads.Highlight(view.session(), view.revision(), original.state(), original.page()))) screen.accept(invalid);
                if (!screen.sources().meshes().isEmpty()) throw new AssertionError("Unauthorized raid highlight accepted");
                var id = UUID.randomUUID();
                screen.accept(new RaidPayloads.Highlight(view.session(), view.revision(), original.state(), new MapAtlasScene.Page(id, page.dimension(), 1, 2, boxes)));
                if (!screen.sources().meshes().isEmpty()) throw new AssertionError("Out of order page published");
                screen.accept(new RaidPayloads.Highlight(view.session(), view.revision(), original.state(), new MapAtlasScene.Page(id, page.dimension(), 0, 2, boxes.subList(0, 5))));
                if (!screen.sources().meshes().isEmpty()) throw new AssertionError("Partial state published");
                screen.accept(new RaidPayloads.Highlight(view.session(), view.revision(), original.state(), new MapAtlasScene.Page(id, page.dimension(), 1, 2, boxes.subList(5, boxes.size()))));
                if (screen.sources().meshes().size() != 1) throw new AssertionError("Paged state missing");
                var tab = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(button -> button.getMessage().getString().equals("sources")).findFirst().orElseThrow();
                var event = new MouseButtonEvent(tab.getX() + 5, tab.getY() + 5, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                screen.mouseClicked(event, false); screen.mouseReleased(event);
                if (!screen.showingSources()) throw new AssertionError("Source tab not selected");
                if (RaidSources.mask(0, 0) != 1 || RaidSources.mask(Math.PI, 0) != 0) throw new AssertionError("Source stripe mask lost opaque/gap phases");
            });
            context.getInput().setCursorPos(width - 2, 80); context.waitTicks(4);
            context.takeScreenshot("hoi-raids-source-stripes-translated-" + width + "-gui2");
            var point = context.computeOnClient(client -> {
                var camera = client.gameRenderer.mainCamera();
                var relative = new net.minecraft.world.phys.Vec3(6.5, 64.065, 2.5).subtract(camera.position());
                var projected = camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f()).transformProject(relative.toVector3f());
                return new int[]{Math.round((projected.x + 1) * width / 2), Math.round((1 - projected.y) * (width * 9 / 16) / 2)};
            });
            context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(4);
            context.runOnClient(client -> {
                var sources = ((RaidScreen)client.gui.screen()).sources();
                if (sources.color(packets[0].state()) != 0xB34DFFFF) throw new AssertionError("Map hover did not highlight candidate state");
            });
            context.takeScreenshot("hoi-raids-source-stripes-hover-translated-" + width + "-gui2");
            context.runOnClient(client -> {
                var screen = (RaidScreen)client.gui.screen();
                var next = json.deepCopy(); next.addProperty("revision", view.revision() + 1); next.remove("setup");
                screen.update(new RaidPayloads.Response(next.toString()).view());
                if (!screen.sources().meshes().isEmpty() || screen.showingSources()) throw new AssertionError("Closed setup kept private highlights");
                screen.accept(packets[0]);
                if (!screen.sources().meshes().isEmpty()) throw new AssertionError("Late highlight reopened setup");
            });
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> ClientCampaignHud.accept(previous));
        }
    }
}
