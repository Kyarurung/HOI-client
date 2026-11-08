package dev.hoi.client.screen.raid;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.protocol.hud.HudPayloads;
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
            for (int x = -8; x <= 12; x++) for (int z = -4; z <= 16; z++) map.setBlock(new net.minecraft.core.BlockPos(x, 63, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
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
            json.add("plans", new com.google.gson.JsonArray());
            var lines = new com.google.gson.JsonArray();
            var line = new com.google.gson.JsonObject();
            line.addProperty("raid", "fixture-raid-projection"); line.addProperty("arrow", "air");
            line.addProperty("state", "PREPARING"); line.addProperty("tooltip", "Synthetic preparing raid"); line.addProperty("open", "open:fixture-raid-projection"); line.addProperty("locate", "locate:fixture-raid-projection");
            var prc = JsonParser.parseString(Files.readString(Path.of(directory, "prc-raids-preparing.json"))).getAsJsonObject();
            line.add("tooltip", prc.getAsJsonArray("lines").get(0).getAsJsonObject().get("tooltip"));
            line.addProperty("fromX", 5.5); line.addProperty("fromZ", 4.5); line.addProperty("toX", 3.5); line.addProperty("toZ", 8.5); line.addProperty("progress", .5); lines.add(line); json.add("lines", lines);
            var view = new RaidPayloads.Response(json.toString()).view();
            for (int width : new int[]{2560, 1280}) {
                context.getInput().resizeWindow(width, width * 9 / 16);
                context.runOnClient(client -> { client.options.guiScale().set(2); client.player.setYRot(0); client.player.setXRot(70); });
                var sent = new ArrayList<RaidPayloads.Request>();
                context.setScreen(() -> new RaidScreen(view.session(), view, sent::add)); context.waitTicks(4);
                context.runOnClient(client -> { if (((RaidScreen)client.gui.screen()).mapHits().size() != 1) throw new AssertionError("Raid target not projected on loaded atlas"); });
                context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
                context.takeScreenshot("hoi-raids-map-projection-synthetic-" + width + "-gui2");
                {
                    context.runOnClient(client -> {
                        var screen = client.gui.screen();
                        var button = screen.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance)
                            .map(net.minecraft.client.gui.components.Button.class::cast)
                            .filter(value -> value.getMessage().getString().equals("expand:" + target.get("category").getAsString())).findFirst().orElseThrow();
                        var event = new MouseButtonEvent(button.getX() + 3, button.getY() + 3, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                        screen.mouseClicked(event, false); screen.mouseReleased(event);
                    });
                    context.waitTicks(2);
                    context.runOnClient(client -> {
                        if (((RaidScreen)client.gui.screen()).mapHits().size() != 1) throw new AssertionError("Raid category collapse hid a target");
                        if (sent.stream().anyMatch(value -> value.kind() == RaidPayloads.Kind.CALL)) throw new AssertionError("Local raid category collapse emitted request");
                        if (client.gui.screen().children().stream().anyMatch(child -> child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().startsWith("show:"))) throw new AssertionError("Removed raid filter remains");
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
                interactions(context, json, directory, width);
                RaidSourcesTest.run(context, directory, width);
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
    private static void interactions(ClientGameTestContext context, com.google.gson.JsonObject source, String directory, int width) throws java.io.IOException {
        var sent = new ArrayList<RaidPayloads.Request>();
        var previous = context.computeOnClient(client -> HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()));
        context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of("PRC", previous.hud())));
        try {
            for (String state : dev.hoi.protocol.raid.RaidView.LINE_STATES) {
                String fixture = switch (state) { case "PREPARING" -> "preparing"; case "BLOCKED" -> "prepared"; case "READY" -> "launchable"; case "ACTIVE" -> "in-progress"; default -> throw new AssertionError(state); };
                var json = JsonParser.parseString(Files.readString(Path.of(directory, "prc-raids-" + fixture + ".json"))).getAsJsonObject();
                json.add("window", com.google.gson.JsonNull.INSTANCE);
                var line = json.getAsJsonArray("lines").get(0).getAsJsonObject();
                if (!line.get("state").getAsString().equals(state)) throw new AssertionError("Raid source state mismatch: " + fixture);
                line.addProperty("fromX", 5.5); line.addProperty("fromZ", 4.5); line.addProperty("toX", 3.5); line.addProperty("toZ", 8.5);
                var view = new RaidPayloads.Response(json.toString()).view();
                for (int button : new int[]{InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT}) {
                    context.setScreen(() -> new RaidScreen(view.session(), view, sent::add)); context.waitTicks(2);
                    var hit = context.computeOnClient(client -> ((RaidScreen)client.gui.screen()).mapInteractions().getFirst());
                    int x = (int)Math.round((hit.x() + hit.endX()) / 2), y = (int)Math.round((hit.y() + hit.endY()) / 2);
                    context.getInput().setCursorPos(x * 2, y * 2); context.waitTicks(2);
                    if (button == InputConstants.MOUSE_BUTTON_LEFT) context.takeScreenshot("hoi-raids-line-" + state.toLowerCase(Locale.ROOT) + "-prc-projected-" + width + "-gui2");
                    context.runOnClient(client -> {
                        if (!hit.contains(x, y) || !hit.contains(hit.endX(), hit.endY()) || hit.contains(x + 1000, y + 1000)) throw new AssertionError("Raid arrow body/head hit bounds");
                        sent.clear();
                        var screen = client.gui.screen(); var event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
                        screen.mouseClicked(event, false); screen.mouseReleased(event); screen.mouseClicked(event, false);
                        String expected = button == InputConstants.MOUSE_BUTTON_LEFT ? hit.open() : hit.right();
                        if (sent.size() != 1 || !sent.getFirst().item().equals(expected) || sent.getFirst().revision() != view.revision()) throw new AssertionError("Raid line click scope/lock: " + sent);
                    });
                }
            }
        } finally {
            context.runOnClient(client -> ClientCampaignHud.accept(previous));
        }
        var zoomView = new RaidPayloads.Response(source.toString()).view();
        double nearWidth = 0;
        for (int altitude : new int[]{76, 110, 90}) {
            var server = context.computeOnClient(client -> client.getSingleplayerServer());
            var move = server.submit(() -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.teleportTo(player.level(), .5, altitude, .5, Set.of(), 0, 70, false);
            });
            context.waitFor(client -> move.isDone()); move.join();
            context.waitFor(client -> Math.abs(client.player.getY() - altitude) < .01);
            context.setScreen(() -> new RaidScreen(zoomView.session(), zoomView, sent::add)); context.waitTicks(4);
            context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
            var hit = context.computeOnClient(client -> ((RaidScreen)client.gui.screen()).mapInteractions().getFirst());
            if (altitude == 76) nearWidth = hit.halfWidth();
            if (altitude == 110 && hit.halfWidth() >= nearWidth) throw new AssertionError("Raid zoom did not reduce projected width");
            if (altitude != 90) context.takeScreenshot("hoi-raids-line-" + (altitude == 76 ? "near" : "far") + "-synthetic-" + width + "-gui2");
        }
        var json = source.deepCopy(); json.add("lines", new com.google.gson.JsonArray());
        var markers = new com.google.gson.JsonArray(); var marker = new com.google.gson.JsonObject();
        marker.addProperty("raid", "ended-synthetic"); marker.addProperty("x", 3.5); marker.addProperty("z", 8.5);
        marker.addProperty("tooltip", "Synthetic completed raid"); marker.addProperty("open", "open:ended-synthetic"); marker.addProperty("dismiss", "dismiss:ended-synthetic");
        markers.add(marker); json.add("markers", markers);
        var ended = new RaidPayloads.Response(json.toString()).view();
        for (int button : new int[]{InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT}) {
            context.setScreen(() -> new RaidScreen(ended.session(), ended, sent::add)); context.waitTicks(2);
            var hit = context.computeOnClient(client -> ((RaidScreen)client.gui.screen()).mapInteractions().getFirst());
            context.getInput().setCursorPos((int)hit.x() * 2, (int)hit.y() * 2); context.waitTicks(2);
            if (button == InputConstants.MOUSE_BUTTON_LEFT) context.takeScreenshot("hoi-raids-marker-synthetic-" + width + "-gui2");
            context.runOnClient(client -> {
                sent.clear(); var event = new MouseButtonEvent(hit.x(), hit.y(), new MouseButtonInfo(button, 0));
                client.gui.screen().mouseClicked(event, false); client.gui.screen().mouseReleased(event); client.gui.screen().mouseClicked(event, false);
                if (sent.size() != 1 || !sent.getFirst().item().equals(button == InputConstants.MOUSE_BUTTON_LEFT ? hit.open() : hit.right())) throw new AssertionError("Completed marker request/lock");
            });
        }
        json.add("markers", new com.google.gson.JsonArray());
        var target = json.getAsJsonArray("targets").get(0).getAsJsonObject();
        var plans = new com.google.gson.JsonArray(); var plan = new com.google.gson.JsonObject();
        plan.addProperty("id", "synthetic-plan"); plan.addProperty("x", .5); plan.addProperty("z", 8.5);
        var rows = new com.google.gson.JsonArray(); var row = new com.google.gson.JsonObject(); var ids = new com.google.gson.JsonArray();
        ids.add(target.get("id").getAsString()); row.addProperty("title", target.get("name").getAsString()); row.add("targets", ids); rows.add(row); plan.add("rows", rows); plans.add(plan); json.add("plans", plans);
        var planned = new RaidPayloads.Response(json.toString()).view();
        context.setScreen(() -> new RaidScreen(planned.session(), planned, sent::add)); context.waitTicks(2);
        var anchor = context.computeOnClient(client -> ((RaidScreen)client.gui.screen()).mapHits().getFirst());
        context.getInput().setCursorPos(anchor.x() * 2, anchor.y() * 2); context.waitTicks(3);
        context.takeScreenshot("hoi-raids-plan-synthetic-" + width + "-gui2");
        var option = context.computeOnClient(client -> {
            var screen = (RaidScreen)client.gui.screen();
            if (screen.mapHits().size() != 2) throw new AssertionError("Hovered raid plan missing type options");
            return screen.mapHits().getLast();
        });
        context.getInput().setCursorPos(option.x() * 2, option.y() * 2); context.waitTicks(3);
        context.runOnClient(client -> {
            sent.clear(); var event = new MouseButtonEvent(option.x(), option.y(), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            client.gui.screen().mouseClicked(event, false);
            if (sent.size() != 1 || !sent.getFirst().item().equals(option.target().select())) throw new AssertionError("Raid plan option not connected");
        });
    }
}
