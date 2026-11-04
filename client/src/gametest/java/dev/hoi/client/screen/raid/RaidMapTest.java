package dev.hoi.client.screen.raid;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.raid.RaidPayloads;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.nio.file.*;
import java.util.*;

public final class RaidMapTest {
    public static void run(ClientGameTestContext context, String directory) {
        var server = context.computeOnClient(client -> client.getSingleplayerServer());
        var previous = context.computeOnClient(client -> client.player.position());
        var dimension = context.computeOnClient(client -> client.level.dimension());
        var rotation = context.computeOnClient(client -> new float[]{client.player.getYRot(), client.player.getXRot()});
        var move = server.submit(() -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            var map = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse("hoi:atlas_ui_test")));
            if (map == null) throw new AssertionError("Raid atlas test dimension missing");
            map.setBlock(new net.minecraft.core.BlockPos(0, 63, 8), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            map.setBlock(new net.minecraft.core.BlockPos(5, 63, 4), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            player.teleportTo(map, .5, 90, .5, Set.of(), 0, 70, false);
        });
        context.waitFor(client -> move.isDone()); move.join();
        context.waitFor(client -> client.level.dimension().identifier().toString().equals("hoi:atlas_ui_test") && client.level.hasChunk(0, 0));
        try {
            var json = JsonParser.parseString(Files.readString(Path.of(directory, "kor-raids-map.json"))).getAsJsonObject();
            var target = json.getAsJsonArray("targets").asList().stream().map(value -> value.getAsJsonObject()).filter(value -> value.get("allowed").getAsBoolean()).findFirst().orElseThrow().deepCopy();
            target.addProperty("x", .5); target.addProperty("z", 8.5);
            var targets = new com.google.gson.JsonArray(); targets.add(target); json.add("targets", targets);
            var lines = new com.google.gson.JsonArray();
            var line = new com.google.gson.JsonObject();
            line.addProperty("raid", "fixture-raid-projection"); line.addProperty("arrow", "air");
            line.addProperty("fromX", 5.5); line.addProperty("fromZ", 4.5); line.addProperty("toX", .5); line.addProperty("toZ", 8.5); line.addProperty("progress", .5); lines.add(line); json.add("lines", lines);
            var view = new RaidPayloads.Response(json.toString()).view();
            for (int width : new int[]{2560, 1280}) {
                context.getInput().resizeWindow(width, width * 9 / 16);
                context.runOnClient(client -> { client.options.guiScale().set(2); client.player.setYRot(0); client.player.setXRot(70); });
                var sent = new ArrayList<RaidPayloads.Request>();
                context.setScreen(() -> new RaidScreen(view.session(), view, sent::add)); context.waitTicks(4);
                context.runOnClient(client -> { if (((RaidScreen)client.gui.screen()).mapHits().size() != 1) throw new AssertionError("Raid target not projected on loaded atlas"); });
                context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
                context.takeScreenshot("hoi-raids-map-projection-synthetic-" + width + "-gui2");
                for (String id : List.of("expand:", "show:")) {
                    context.runOnClient(client -> {
                        var screen = client.gui.screen();
                        var button = screen.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance)
                            .map(net.minecraft.client.gui.components.Button.class::cast)
                            .filter(value -> value.getMessage().getString().equals(id + target.get("category").getAsString())).findFirst().orElseThrow();
                        var event = new MouseButtonEvent(button.getX() + 3, button.getY() + 3, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                        screen.mouseClicked(event, false); screen.mouseReleased(event);
                    });
                    context.waitTicks(2);
                    context.runOnClient(client -> {
                        int expected = id.equals("expand:") ? 1 : 0;
                        if (((RaidScreen)client.gui.screen()).mapHits().size() != expected) throw new AssertionError("Raid category list/filter affected wrong target visibility");
                        if (sent.stream().anyMatch(value -> value.kind() == RaidPayloads.Kind.CALL)) throw new AssertionError("Local raid filter emitted request");
                    });
                }
                context.setScreen(() -> new RaidScreen(view.session(), view, sent::add)); context.waitTicks(2);
                var hit = context.computeOnClient(client -> ((RaidScreen)client.gui.screen()).mapHits().getFirst());
                context.getInput().setCursorPos(hit.x() * 2, hit.y() * 2); context.waitTicks(2);
                context.takeScreenshot("hoi-raids-map-tooltip-synthetic-" + width + "-gui2");
                context.runOnClient(client -> {
                    var screen = (RaidScreen)client.gui.screen();
                    var right = new MouseButtonEvent(hit.x(), hit.y(), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0));
                    screen.mouseClicked(right, false); screen.mouseReleased(right);
                    long before = sent.stream().filter(value -> value.kind() == RaidPayloads.Kind.CALL).count();
                    if (before != 0) throw new AssertionError("Raid target right-click mutated state");
                });
                context.setScreen(() -> new RaidScreen(view.session(), view, sent::add)); context.waitTicks(2);
                context.runOnClient(client -> {
                    sent.clear(); var screen = (RaidScreen)client.gui.screen(); var current = screen.mapHits().getFirst();
                    var left = new MouseButtonEvent(current.x(), current.y(), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOD_SHIFT));
                    screen.mouseClicked(left, false); screen.mouseReleased(left); screen.mouseClicked(left, false);
                    if (sent.size() != 1 || !sent.getFirst().item().equals(target.get("quick").getAsString())) throw new AssertionError("Raid quick action or duplicate lock lost: " + sent);
                });
                RaidScreenHudTest.run(context, directory, json, width);
            }
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        finally {
            context.setScreen(() -> null);
            var restore = server.submit(() -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.teleportTo(server.getLevel(dimension), previous.x, previous.y, previous.z, Set.of(), rotation[0], rotation[1], false);
            });
            context.waitFor(client -> restore.isDone()); restore.join();
            context.waitFor(client -> client.level.dimension().equals(dimension));
        }
    }
}
