package dev.hoi.client.screen.raid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.map.agency.AgencyRaidHud;
import dev.hoi.protocol.agency.AgencyPayloads;
import dev.hoi.protocol.raid.RaidPayloads;
import dev.hoi.client.screen.map.MapCursor;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;

public final class RaidScreenHudTest {
    private static Object field(Class<?> type, String name, Object... value) {
        try {
            var field = type.getDeclaredField(name); field.setAccessible(true);
            if (value.length > 0) field.set(null, value[0]);
            return field.get(null);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static RaidScreen screen() { return (RaidScreen)field(AgencyRaidHud.class, "screen"); }
    private static JsonObject read(String directory, String name) {
        try { return JsonParser.parseString(Files.readString(Path.of(directory, name + ".json"))).getAsJsonObject(); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }
    private static void accept(JsonObject source, String token, long revision) {
        var copy = source.deepCopy(); copy.addProperty("session", token); copy.addProperty("revision", revision);
        AgencyRaidHud.accept(new RaidPayloads.Response(copy.toString()).view());
    }
    private static MouseButtonEvent event(double x, double y, int button, boolean shift) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, shift ? InputConstants.MOD_SHIFT : 0));
    }
    public static void source(ClientGameTestContext context, String directory, int width) {
        var originalTransport = context.computeOnClient(client -> field(AgencyRaidHud.class, "transport"));
        var sent = new ArrayList<RaidPayloads.Request>();
        var agency = context.computeOnClient(client -> AgencyHud.snapshot());
        try {
            context.runOnClient(client -> {
                AgencyRaidHud.reset();
                field(AgencyRaidHud.class, "transport", (Consumer<RaidPayloads.Request>)sent::add);
            });
            context.waitTicks(2);
            String token = context.computeOnClient(client -> sent.getFirst().session());
            context.runOnClient(client -> accept(read(directory, "kor-raids-map"), token, 100));
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (screen().mapHits().isEmpty() || AgencyHud.snapshot() != agency) throw new AssertionError("Source atlas overlay must retain actual targets and operative snapshot");
                for (var hit : screen().mapHits()) if (AgencyHud.containsControl(hit.x(), hit.y())) {
                    if (AgencyRaidHud.click(event(hit.x(), hit.y(), InputConstants.MOUSE_BUTTON_LEFT, false), false)) throw new AssertionError("Raid target behind operative control consumed its click");
                }
            });
            context.takeScreenshot("hoi-raids-operative-source-atlas-" + width + "-gui2");
            context.runOnClient(client -> {
                var rows = new com.google.gson.JsonArray();
                for (var target : screen().mapHits()) {
                    var row = new JsonObject();
                    row.addProperty("id", target.target().id()); row.addProperty("state", target.target().state());
                    row.addProperty("province", target.target().province());
                    row.addProperty("screenX", target.x() * 2); row.addProperty("screenY", target.y() * 2);
                    row.addProperty("worldX", target.target().x()); row.addProperty("worldZ", target.target().z());
                    rows.add(row);
                }
                try { Files.writeString(Path.of("raid-target-hits-" + width + ".json"), rows.toString()); }
                catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            });
            var hit = context.computeOnClient(client -> screen().mapHits().getFirst());
            context.getInput().setCursorPos(hit.x() * 2, hit.y() * 2); context.waitTicks(2);
            context.takeScreenshot("hoi-raids-operative-source-tooltip-" + width + "-gui2");
            context.runOnClient(client -> {
                var closed = read(directory, "kor-raids-in-progress");
                closed.remove("window"); closed.remove("setup");
                accept(closed, token, 101);
            });
            context.getInput().setCursorPos(width - 2, 80); context.waitTicks(3);
            context.runOnClient(client -> {
                if (RaidMap.texture("PREPARING") == null) throw new AssertionError("Original raid arrow mask must load");
                RaidScreen.resourcesReloaded();
                if (RaidMap.texture("PREPARING") == null) throw new AssertionError("Raid arrow mask must survive resource reload");
            });
            context.takeScreenshot("hoi-raids-operative-source-line-" + width + "-gui2");
        } finally {
            context.runOnClient(client -> { AgencyRaidHud.reset(); field(AgencyRaidHud.class, "transport", originalTransport); });
        }
    }
    public static void run(ClientGameTestContext context, String directory, JsonObject map, int width) {
        var hand = context.computeOnClient(client -> client.player.getMainHandItem().copy());
        var originalTransport = context.computeOnClient(client -> field(AgencyRaidHud.class, "transport"));
        var sent = new ArrayList<RaidPayloads.Request>();
        var agency = new AgencyPayloads.Response(read(directory, "kor-agency-operatives").toString()).view();
        try {
            context.runOnClient(client -> {
                AgencyRaidHud.reset();
                field(AgencyRaidHud.class, "transport", (Consumer<RaidPayloads.Request>)sent::add);
                var selector = new ItemStack(Items.PAPER);
                selector.set(DataComponents.ITEM_MODEL, Identifier.parse("hoi:map/selector/operatives"));
                client.player.setItemInHand(InteractionHand.MAIN_HAND, selector);
                AgencyHud.reset();
                field(AgencyHud.class, "session", agency.session()); field(AgencyHud.class, "country", agency.country());
                field(AgencyHud.class, "dimension", client.level.dimension().identifier().toString());
                AgencyHud.accept(agency);
                client.gui.setScreen(new MapCursor());
            });
            context.waitTicks(3);
            String token = context.computeOnClient(client -> {
                if (sent.stream().filter(request -> request.kind() == RaidPayloads.Kind.OPEN).count() != 1) throw new AssertionError("Raid mode must open one independent session");
                return sent.getFirst().session();
            });
            context.runOnClient(client -> accept(map, token, 100));
            context.getInput().setCursorPos(width - 2, 80); context.waitTicks(3);
            context.runOnClient(client -> {
                if (screen().mapHits().size() != 1 || AgencyHud.snapshot() != agency) throw new AssertionError("Raid overlay lost target or operative snapshot");
                if (AgencyRaidHud.click(event(300, 70, InputConstants.MOUSE_BUTTON_LEFT, false), false)) throw new AssertionError("Empty operative map click consumed by raids");
                var raid = screen();
                if (raid.children().stream().anyMatch(child -> child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().startsWith("show:"))) throw new AssertionError("Removed raid filter still has widgets");
                double filterX = raid.width - 29, filterY = raid.height - 110;
                if (AgencyRaidHud.click(event(filterX, filterY, InputConstants.MOUSE_BUTTON_LEFT, false), false)
                        || AgencyRaidHud.click(event(filterX, filterY, InputConstants.MOUSE_BUTTON_RIGHT, false), false)
                        || AgencyRaidHud.scroll(filterX, filterY, 0, -1)) throw new AssertionError("Removed filter consumes operative map input");
                client.gui.screen().mouseClicked(event(filterX, filterY, InputConstants.MOUSE_BUTTON_LEFT, false), false);
                try {
                    var selecting = MapCursor.class.getDeclaredField("selecting"); selecting.setAccessible(true);
                    if (!selecting.getBoolean(client.gui.screen())) throw new AssertionError("Removed filter does not reach map selection");
                } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                client.gui.screen().mouseReleased(event(filterX, filterY, InputConstants.MOUSE_BUTTON_LEFT, false));
                accept(map, token, 99);
                accept(map, "stale-session", 102);
                if (screen().snapshot().revision() != 100) throw new AssertionError("Stale raid response accepted");
            });
            context.takeScreenshot("hoi-raids-operative-overlay-synthetic-" + width + "-gui2");
            context.runOnClient(client -> client.gui.setScreen(null)); context.waitTicks(2);
            context.takeScreenshot("hoi-raids-operative-native-synthetic-" + width + "-gui2");
            context.setScreen(MapCursor::new); context.waitTicks(2);
            var hit = context.computeOnClient(client -> screen().mapHits().getFirst());
            context.getInput().setCursorPos(hit.x() * 2, hit.y() * 2); context.waitTicks(2);
            context.takeScreenshot("hoi-raids-operative-tooltip-synthetic-" + width + "-gui2");
            context.runOnClient(client -> {
                sent.clear();
                var right = event(hit.x(), hit.y(), InputConstants.MOUSE_BUTTON_RIGHT, false);
                client.gui.screen().mouseClicked(right, false); client.gui.screen().mouseReleased(right);
                if (!sent.isEmpty()) throw new AssertionError("Overlay right click issued raid request");
                var left = event(hit.x(), hit.y(), InputConstants.MOUSE_BUTTON_LEFT, false);
                client.gui.screen().mouseClicked(left, false); client.gui.screen().mouseReleased(left);
                client.gui.screen().mouseClicked(left, false);
                if (sent.size() != 1 || !sent.getFirst().item().equals(hit.target().select())) throw new AssertionError("Overlay target request/duplicate lock failed");
                accept(read(directory, "kor-raids-setup"), token, 101);
            });
            context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
            context.takeScreenshot("hoi-raids-operative-setup-synthetic-" + width + "-gui2");
            context.runOnClient(client -> {
                sent.clear();
                var raid = screen(); var unit = raid.snapshot().setup().units().getFirst().select();
                var button = raid.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance)
                        .map(net.minecraft.client.gui.components.Button.class::cast).filter(b -> b.getMessage().getString().equals(unit)).findFirst().orElseThrow();
                var click = event(button.getX() + 3, button.getY() + 3, InputConstants.MOUSE_BUTTON_LEFT, false);
                client.gui.screen().mouseClicked(click, false); client.gui.screen().mouseReleased(click);
                if (sent.size() != 1 || !sent.getFirst().item().equals(unit)) throw new AssertionError("Overlay unit selection failed");
                accept(read(directory, "kor-raids-create"), token, 102);
            });
            context.waitTicks(2); context.takeScreenshot("hoi-raids-operative-create-synthetic-" + width + "-gui2");
            context.runOnClient(client -> {
                sent.clear(); String close = screen().snapshot().window().close().item();
                if (!AgencyRaidHud.closePanel() || sent.size() != 1 || !sent.getFirst().item().equals(close)) throw new AssertionError("Overlay close lost server action");
                accept(map, token, 103);
                if (AgencyHud.snapshot() != agency || !(client.gui.screen() instanceof MapCursor)) throw new AssertionError("Raid close lost operative map");
            });
            context.waitTicks(2); context.takeScreenshot("hoi-raids-operative-return-synthetic-" + width + "-gui2");
            context.runOnClient(client -> {
                sent.clear(); var current = screen().mapHits().getFirst();
                var quick = event(current.x(), current.y(), InputConstants.MOUSE_BUTTON_LEFT, true);
                client.gui.screen().mouseClicked(quick, false); client.gui.screen().mouseReleased(quick);
                if (sent.size() != 1 || !sent.getFirst().item().equals(current.target().quick())) throw new AssertionError("Overlay Shift click failed");
                client.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            });
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (screen() != null || sent.stream().filter(request -> request.kind() == RaidPayloads.Kind.CLOSE).count() != 1) throw new AssertionError("Leaving operative mode did not close exactly once");
                accept(map, token, 104);
                if (screen() != null) throw new AssertionError("Late response reopened raid overlay");
            });
            context.takeScreenshot("hoi-raids-main-no-entry-" + width + "-gui2");
            context.runOnClient(client -> {
                sent.clear();
                var selector = new ItemStack(Items.PAPER);
                selector.set(DataComponents.ITEM_MODEL, Identifier.parse("hoi:map/selector/operatives"));
                client.player.setItemInHand(InteractionHand.MAIN_HAND, selector);
            });
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (sent.stream().filter(request -> request.kind() == RaidPayloads.Kind.OPEN).count() != 1
                        || sent.getFirst().session().equals(token)) throw new AssertionError("Re-entering operative mode must open one new raid session");
                accept(map, token, 105);
                if (screen().snapshot() != null) throw new AssertionError("Prior mode session accepted after re-entry");
            });
        } finally {
            context.runOnClient(client -> {
                AgencyRaidHud.reset(); field(AgencyRaidHud.class, "transport", originalTransport);
                client.player.setItemInHand(InteractionHand.MAIN_HAND, hand); AgencyHud.reset();
            });
        }
    }
}
