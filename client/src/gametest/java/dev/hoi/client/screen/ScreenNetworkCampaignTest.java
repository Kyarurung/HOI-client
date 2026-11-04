package dev.hoi.client.screen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.network.NetworkScreens;
import dev.hoi.protocol.menu.MenuTab;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.map.MapCursor;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class ScreenNetworkCampaignTest {
    private static final Gson JSON = new Gson();
    private static final List<Map<String, String>> sounds = new ArrayList<>();
    private static final List<Map<String, String>> eventChoices = new ArrayList<>();
    private ScreenNetworkCampaignTest() {}
    public static void run(ClientGameTestContext game) {
        String player = Objects.requireNonNull(System.getenv("HOI_NETWORK_PLAYER"));
        if (!Set.of("HoiTestA", "HoiTestB").contains(player)) throw new IllegalArgumentException("Expected copied-server test identity");
        Path directory = Path.of(Objects.requireNonNull(System.getenv("HOI_NETWORK_DIRECTORY")), player);
        net.minecraft.client.sounds.SoundEventListener listener = (sound, event, range) -> {
            if (sound.getIdentifier().getNamespace().equals("hoi")) sounds.add(Map.of("at", Instant.now().toString(), "event", sound.getIdentifier().toString(), "source", sound.getSource().toString()));
        };
        try {
            Files.createDirectories(directory);
            game.getInput().resizeWindow(1280, 720);
            game.runOnClient(client -> {
                client.options.guiScale().set(2);
                client.getSoundManager().addListener(listener);
                try {
                    var user = Minecraft.class.getDeclaredField("user"); user.setAccessible(true);
                    user.set(client, new User(player, UUID.nameUUIDFromBytes(("OfflinePlayer:" + player).getBytes(StandardCharsets.UTF_8)), "FabricMC", Optional.empty(), Optional.empty()));
                } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
                var server = new ServerData("HOI copied-server verification", "127.0.0.1:25591", ServerData.Type.OTHER);
                server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                ConnectScreen.startConnecting(new TitleScreen(), client, ServerAddress.parseString(server.ip), server, false, null);
            });
            game.waitFor(client -> client.player != null && client.level != null || client.gui.screen() instanceof net.minecraft.client.gui.screens.DisconnectedScreen, 12000);
            if (game.computeOnClient(client -> client.gui.screen() instanceof net.minecraft.client.gui.screens.DisconnectedScreen)) {
                game.takeScreenshot(player + "-connection-failed");
                save(game, directory.resolve("connection-failed.json"), "Disconnected before campaign entry; inspect network log");
                throw new AssertionError("Copied-server connection rejected");
            }
            game.waitFor(client -> !ClientCampaignHud.country().isEmpty(), 12000);
            String expected = player.equals("HoiTestA") ? "KOR" : System.getenv().getOrDefault("HOI_NETWORK_COUNTRY_B", "JAP");
            game.runOnClient(client -> { if (!ClientCampaignHud.country().equals(expected)) throw new AssertionError("Unexpected assigned country"); });
            save(game, directory.resolve("ready.json"), "connected");
            for (int sequence = 1; sequence <= 500; sequence++) {
                Path command = directory.resolve(String.format(Locale.ROOT, "%03d-command.json", sequence));
                game.waitFor(client -> Files.isRegularFile(command), 18000);
                var action = JSON.fromJson(Files.readString(command), JsonObject.class);
                String operation = action.get("operation").getAsString();
                try {
                    if (operation.equals("stop")) {
                        game.runOnClient(client -> client.disconnectWithSavingScreen());
                        save(game, directory.resolve(String.format(Locale.ROOT, "%03d-result.json", sequence)), "disconnected");
                        return;
                    }
                    game.runOnClient(client -> {
                        var screen = DialogCoordinator.contentScreen();
                        switch (operation) {
                            case "menu" -> NetworkScreens.openMenu(MenuTab.valueOf(action.get("tab").getAsString()));
                            case "close" -> { if (screen != null) screen.onClose(); }
                            case "cursor" -> DialogCoordinator.setContent(new MapCursor());
                            case "event" -> {
                                var stack = DialogCoordinator.eventStack();
                                if (stack != null && !stack.views().isEmpty()) {
                                    var view = stack.views().stream().filter(v -> v.completion()).reduce((first, last) -> last).orElse(stack.views().getLast());
                                    if (!view.choices().isEmpty()) {
                                        stack.reopen(view.token());
                                        var window = stack.find(view.token());
                                        var choice = view.completion() ? view.choices().stream().filter(c -> c.id().equals("ack")).findFirst().orElse(view.choices().getFirst()) : view.choices().getFirst();
                                        var button = window.children().stream().filter(w -> w instanceof AbstractWidget b && b.active && b.getMessage().getString().equals(choice.label())).map(w -> (AbstractWidget) w).findFirst().orElseThrow();
                                        eventChoices.add(Map.of("at", Instant.now().toString(), "token", view.token(), "title", view.title(), "choice", choice.id(), "label", choice.label()));
                                        var event = new MouseButtonEvent(button.getX() + button.getWidth() / 2., button.getY() + button.getHeight() / 2., new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                                        stack.mouseClicked(event, false); stack.mouseReleased(event);
                                    }
                                }
                            }
                            case "click" -> {
                                var widget = screen.children().stream().filter(w -> w instanceof AbstractWidget b && b.getMessage().getString().equals(action.get("label").getAsString())).map(w -> (AbstractWidget)w).skip(action.has("index") ? action.get("index").getAsInt() : 0).findFirst().orElseThrow();
                                if (!widget.active) throw new AssertionError("Disabled control: " + widget.getMessage().getString());
                                var event = new MouseButtonEvent(widget.getX() + widget.getWidth() / 2., widget.getY() + widget.getHeight() / 2., new MouseButtonInfo(action.has("right") && action.get("right").getAsBoolean() ? InputConstants.MOUSE_BUTTON_RIGHT : InputConstants.MOUSE_BUTTON_LEFT, action.has("shift") && action.get("shift").getAsBoolean() ? InputConstants.MOD_SHIFT : 0));
                                var recipient = client.gui.screen(); recipient.mouseClicked(event, false); recipient.mouseReleased(event);
                            }
                            case "scroll" -> screen.mouseScrolled(action.get("x").getAsDouble(), action.get("y").getAsDouble(), 0, action.get("amount").getAsDouble());
                            case "pointer" -> {
                                double x = action.get("x").getAsDouble(), y = action.get("y").getAsDouble();
                                var event = new MouseButtonEvent(x, y, new MouseButtonInfo(action.has("right") && action.get("right").getAsBoolean() ? InputConstants.MOUSE_BUTTON_RIGHT : InputConstants.MOUSE_BUTTON_LEFT, action.has("shift") && action.get("shift").getAsBoolean() ? InputConstants.MOD_SHIFT : 0));
                                if (screen != null) { var recipient = client.gui.screen(); recipient.mouseClicked(event, false); recipient.mouseReleased(event); }
                                else if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) dev.hoi.client.input.InputMapInfo.clickAt(client, x, y, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                            }
                            case "snapshot" -> { }
                            default -> throw new IllegalArgumentException(operation);
                        }
                    });
                    game.waitTicks(action.has("ticks") ? Math.clamp(action.get("ticks").getAsInt(), 1, 400) : 40);
                    game.takeScreenshot(player + "-" + sequence + "-" + operation);
                    save(game, directory.resolve(String.format(Locale.ROOT, "%03d-result.json", sequence)), "ok");
                } catch (Exception | AssertionError failure) {
                    save(game, directory.resolve(String.format(Locale.ROOT, "%03d-result.json", sequence)), failure.toString());
                }
            }
            throw new AssertionError("Network session must explicitly stop");
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        finally { game.runOnClient(client -> { client.getSoundManager().removeListener(listener); if (client.getConnection() != null) client.disconnectWithSavingScreen(); client.gui.setScreen(new TitleScreen()); }); }
    }
    private static void save(ClientGameTestContext game, Path path, String result) throws java.io.IOException {
        String text = game.computeOnClient(client -> {
            var state = new LinkedHashMap<String, Object>();
            state.put("at", Instant.now().toString()); state.put("result", result); state.put("country", ClientCampaignHud.country()); state.put("hud", ClientCampaignHud.hud());
            state.put("sounds", sounds);
            state.put("eventChoices", eventChoices);
            var notifications = DialogCoordinator.eventStack();
            if (notifications != null) state.put("events", notifications.views());
            var displays = new ArrayList<Map<String, Object>>();
            if (client.level != null) for (var entity : client.level.entitiesForRendering()) {
                if (entity instanceof net.minecraft.world.entity.Display.ItemDisplay display) displays.add(Map.of("uuid", entity.getUUID().toString(), "x", entity.getX(), "y", entity.getY(), "z", entity.getZ(), "item", display.getItemStack().toString(), "model", String.valueOf(display.getItemStack().get(net.minecraft.core.component.DataComponents.ITEM_MODEL))));
            }
            state.put("displays", displays);
            if (client.player != null) state.put("position", Map.of("x", client.player.getX(), "y", client.player.getY(), "z", client.player.getZ(), "yaw", client.player.getYRot(), "pitch", client.player.getXRot(), "dimension", client.level.dimension().identifier().toString()));
            for (var item : Map.of("units", "dev.hoi.client.map.commander.CommanderUnitHud#groups", "commanders", "dev.hoi.client.map.commander.CommanderHud#view", "orders", "dev.hoi.client.map.commander.CommanderOrders#context").entrySet()) {
                try { var parts = item.getValue().split("#"); var field = Class.forName(parts[0]).getDeclaredField(parts[1]); field.setAccessible(true); state.put(item.getKey(), field.get(null)); }
                catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            }
            var screen = DialogCoordinator.contentScreen();
            state.put("screen", screen == null ? "none" : screen.getClass().getSimpleName());
            if (screen != null) {
                state.put("widgets", screen.children().stream().filter(w -> w instanceof AbstractWidget).map(w -> { var b = (AbstractWidget)w; return Map.of("label", b.getMessage().getString(), "active", b.active, "x", b.getX(), "y", b.getY()); }).toList());
                for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
                    try { var field = type.getDeclaredField("view"); field.setAccessible(true); state.put("view", field.get(screen)); break; }
                    catch (NoSuchFieldException absent) { }
                    catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
                }
            }
            return JSON.toJson(state);
        });
        Files.writeString(path, text);
    }
}
