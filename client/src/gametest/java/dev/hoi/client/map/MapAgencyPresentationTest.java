package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenAgency;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class MapAgencyPresentationTest {
    private static Object field(String name) {
        try { var field = MapAgencyHud.class.getDeclaredField(name); field.setAccessible(true); return field.get(null); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static void field(String name, Object value) {
        try { var field = MapAgencyHud.class.getDeclaredField(name); field.setAccessible(true); field.set(null, value); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static void install(ProtocolAgencyView source, ProtocolMenuView menu) {
        var client = net.minecraft.client.Minecraft.getInstance();
        ClientCampaignHud.accept(ProtocolHudPayloads.State.of(source.country(), menu.hud()));
        var selector = new ItemStack(Items.PAPER);
        selector.set(DataComponents.ITEM_MODEL, Identifier.parse("hoi:map/selector/operatives"));
        client.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, selector);
        MapAgencyHud.reset();
        field("session", source.session()); field("country", source.country());
        field("dimension", client.level.dimension().identifier().toString());
        MapAgencyHud.accept(source);
    }
    public static void run(ClientGameTestContext context, ProtocolMenuView menu, java.util.function.Consumer<String> serverCommand) {
        String directory = System.getenv("HOI_AGENCY_OPERATION_PREVIEW");
        if (directory == null) throw new AssertionError("Authoritative operative fixture required");
        ProtocolAgencyView source;
        try { source = new ProtocolAgencyPayloads.Response(Files.readString(Path.of(directory, "kor-agency-operatives.json"))).view(); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        boolean before = "1".equals(System.getenv("HOI_CAPTURE_BEFORE"));
        com.google.gson.JsonObject atlas;
        try (var input = MapAgencyPresentationTest.class.getResourceAsStream("/atlas-client-scene.json")) {
            if (input == null) throw new AssertionError("Production atlas fixture required");
            atlas = com.google.gson.JsonParser.parseString(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        var network = source.networks().getFirst();
        var provinceCells = new java.util.ArrayList<com.google.gson.JsonArray>();
        for (var cell : atlas.getAsJsonArray("provinces")) if (network.provinces().contains(cell.getAsJsonArray().get(2).getAsInt())) provinceCells.add(cell.getAsJsonArray());
        if (provinceCells.isEmpty()) throw new AssertionError("Network province must exist in the production atlas fixture");
        int originX = network.segments().stream().mapToInt(segment -> Math.min(segment.fromX(), segment.toX())).min().orElseThrow()
                - provinceCells.stream().mapToInt(cell -> cell.get(0).getAsInt()).min().orElseThrow();
        int originZ = network.segments().stream().mapToInt(segment -> Math.min(segment.fromZ(), segment.toZ())).min().orElseThrow()
                - provinceCells.stream().mapToInt(cell -> cell.get(1).getAsInt()).min().orElseThrow();
        int atlasSize = atlas.get("size").getAsInt();
        var server = context.computeOnClient(client -> client.getSingleplayerServer());
        var playerTask = server.submit(() -> server.getPlayerList().getPlayers().getFirst());
        context.waitFor(client -> playerTask.isDone()); var player = playerTask.join();
        var gameMode = player.gameMode.getGameModeForPlayer();
        var position = context.computeOnClient(client -> client.player.position());
        var rotation = context.computeOnClient(client -> new float[]{client.player.getYRot(), client.player.getXRot()});
        var dimension = context.computeOnClient(client -> client.level.dimension());
        var hand = context.computeOnClient(client -> client.player.getMainHandItem().copy());
        int scale = context.computeOnClient(client -> client.options.guiScale().get());
        var move = server.submit(() -> {
            var map = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse("hoi:atlas_ui_test")));
            var blocks = new java.util.LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
            for (int x = originX; x < originX + atlasSize; x++) for (int z = originZ; z < originZ + atlasSize; z++) for (int y = 64; y <= 68; y++) {
                var at = new BlockPos(x, y, z); blocks.put(at, map.getBlockState(at));
            }
            player.teleportTo(map, 257, 81, 80, java.util.Set.of(), 0, 55, false);
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            return blocks;
        });
        context.waitFor(client -> move.isDone()); var blocks = move.join();
        java.util.function.Consumer<String> command = value -> serverCommand.accept("execute in hoi:atlas_ui_test run " + value);
        command.accept("fill " + originX + " 64 " + originZ + " " + (originX + atlasSize - 1) + " 68 " + (originZ + atlasSize - 1) + " minecraft:air strict");
        var data = atlas.getAsJsonObject("scenes").getAsJsonObject("ARMY");
        String[] cells = new String[atlasSize * atlasSize];
        for (var entry : data.getAsJsonArray("cells")) {
            var cell = entry.getAsJsonArray(); cells[cell.get(1).getAsInt() * atlasSize + cell.get(0).getAsInt()] = cell.get(2).getAsString();
        }
        for (int z = 0; z < atlasSize; z++) for (int x = 0; x < atlasSize;) {
            int end = x + 1;
            while (end < atlasSize && cells[z * atlasSize + end].equals(cells[z * atlasSize + x])) end++;
            command.accept("fill " + (x + originX) + " 65 " + (z + originZ) + " " + (end - 1 + originX) + " 65 " + (z + originZ) + " " + cells[z * atlasSize + x] + " strict");
            x = end;
        }
        for (var entry : data.getAsJsonArray("relief")) {
            var cell = entry.getAsJsonArray();
            command.accept("setblock " + (originX + cell.get(0).getAsInt()) + " " + cell.get(3).getAsInt() + " " + (originZ + cell.get(1).getAsInt()) + " " + cell.get(2).getAsString() + " strict");
        }
        var atlasBoxes = new java.util.ArrayList<ProtocolAtlasScene.Box>();
        for (var entry : data.getAsJsonArray("meshes")) {
            var box = entry.getAsJsonArray();
            atlasBoxes.add(new ProtocolAtlasScene.Box(originX + box.get(0).getAsFloat(), box.get(1).getAsFloat(), originZ + box.get(2).getAsFloat(),
                    box.get(3).getAsFloat(), box.get(4).getAsFloat(), box.get(5).getAsFloat(), box.get(6).getAsFloat(), ProtocolAtlasScene.Material.valueOf(box.get(7).getAsString())));
        }
        context.runOnClient(client -> {
            var scene = java.util.UUID.randomUUID(); int pages = (atlasBoxes.size() + 1023) / 1024;
            for (int page = 0; page < pages; page++) MapSceneRenderer.receive(new ProtocolAtlasScene.Page(scene, "hoi:atlas_ui_test", page, pages,
                    atlasBoxes.subList(page * 1024, Math.min((page + 1) * 1024, atlasBoxes.size()))));
        });
        context.waitFor(client -> client.level.dimension().identifier().toString().equals("hoi:atlas_ui_test") && client.level.hasChunk(16, 5));
        context.waitTicks(5);
        try {
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> { client.options.guiScale().set(2); install(source, menu); });
                context.setScreen(ScreenMapCursor::new); context.waitTicks(3);
                context.getInput().setCursorPos(size[0] - 3, size[1] - 3); context.waitTicks(2);
                context.takeScreenshot("hoi-agency-map-source-" + size[0] + "-gui2");
                dev.hoi.client.screen.ScreenRaidHudTest.source(context, directory, size[0]);
                var initialNetwork = context.computeOnClient(client -> MapAgencyNetworks.extract().getFirst().vertices().clone());
                context.waitTicks(10);
                context.runOnClient(client -> {
                    var animated = MapAgencyNetworks.extract().getFirst().vertices();
                    if (java.util.Arrays.equals(initialNetwork, animated)) throw new AssertionError("Network UV must animate without a new snapshot");
                    for (int i = 4; i < animated.length; i += 8) if (Math.abs(animated[i] - .05f) > .0001 && Math.abs(animated[i] - .95f) > .0001)
                        throw new AssertionError("Original network body UV scale must remain 0.9");
                    if (MapAgencyNetworks.texture() == null) throw new AssertionError("Original network pattern and mask must load");
                });
                context.takeScreenshot("hoi-agency-network-animated-atlas-" + size[0] + "-gui2");
                int count = Math.max(source.operatives().size(), source.operativeCapacity() == null ? 0 : source.operativeCapacity());
                int cardX = (size[0] / 2 - count * 40) / 2 + 20, cardY = size[1] / 2 - 35;
                context.runOnClient(client -> {
                    if (!MapAgencyHud.active()) throw new AssertionError("Operative selector must activate private HUD");
                    if (!before && MapAgencyNetworks.extract().size() != source.networks().getFirst().nodes().stream().filter(node -> !node.parent().isEmpty()).count()) throw new AssertionError("Issued parent-to-state network edges missing");
                    if (!before && MapAgencyNetworks.texture() == null) throw new AssertionError("Original network mask missing");
                    if (!before) for (String sprite : List.of("operative_frame/atlas", "operative_leader_badge/frame_0", "nv_btn_close/frame_0", "operative_unknown/frame_0", "lg_disband_group_btn/frame_0", "leader_group_frame_unselected/frame_0", "execute_stop_operatives_small_bg/frame_0", "active_operative_operation_anim/frame_0", "leader_portrait_captured/frame_0")) {
                        if (dev.hoi.client.ui.UiAssets.imageHeight("agency/operations/gfx_" + sprite, 100) <= 0) throw new AssertionError("Missing original operative card art: " + sprite);
                    }
                    MapCommanderHud.clickAt(cardX, cardY, InputConstants.MOUSE_BUTTON_RIGHT, false);
                    if (!field("selected").equals("")) throw new AssertionError("Right click selected operative");
                    MapCommanderHud.clickAt(cardX, cardY, InputConstants.MOUSE_BUTTON_LEFT, false);
                    if (before) field("selected", source.operatives().getFirst().id());
                    else if (!field("selected").equals(source.operatives().getFirst().id())) throw new AssertionError("Real left input did not select operative");
                });
                context.waitTicks(3); context.takeScreenshot("hoi-agency-card-selected-source-" + size[0] + "-gui2");
                if (!before) {
                    context.runOnClient(client -> field("selectedAt", net.minecraft.util.Util.getMillis() - 9000));
                    context.waitTicks(2); context.takeScreenshot("hoi-agency-badge-scrolled-" + size[0] + "-gui2");
                }
                context.runOnClient(client -> {
                    if (!before) {
                        var missions = ((List<?>)field("hits")).stream().filter(hit -> hitNumber(hit, "width") == 24 && hitNumber(hit, "height") == 24).toList();
                        if (missions.size() != 8) throw new AssertionError("Selected operative must expose all eight original mission positions");
                        var screen = client.gui.screen();
                        for (var mission : missions) {
                            MapAgencyHud.clickAt(hitNumber(mission, "x") + 12, hitNumber(mission, "y") + 12, InputConstants.MOUSE_BUTTON_LEFT);
                            MapAgencyHud.clickAt(hitNumber(mission, "x") + 12, hitNumber(mission, "y") + 12, InputConstants.MOUSE_BUTTON_RIGHT);
                        }
                        if (client.gui.screen() != screen || !field("selected").equals(source.operatives().getFirst().id()) || !field("targeting").equals("")) throw new AssertionError("Busy operative mission controls must remain disabled");
                        for (int frame = 0; frame < 8; frame++) if (dev.hoi.client.ui.UiAssets.imageHeight("agency/operations/gfx_operative_mission_icons/frame_" + frame, 17) <= 0)
                            throw new AssertionError("Missing original mission frame " + frame);
                        MapCommanderHud.clickAt(cardX, cardY, InputConstants.MOUSE_BUTTON_LEFT, false);
                        if (!field("selected").equals("")) throw new AssertionError("Repeated card click must deselect");
                    }
                    MapAgencyHud.accept(source);
                    if (MapAgencyHud.snapshot() != source) throw new AssertionError("Same revision replaced authoritative snapshot");
                });
                var requests = new java.util.ArrayList<ProtocolAgencyPayloads.Request>();
                context.setScreen(() -> new ScreenAgency(null, source.session(), source, requests::add));
                context.waitTicks(3); context.getInput().setCursorPos(size[0] - 3, size[1] - 3); context.waitTicks(2);
                context.takeScreenshot("hoi-agency-operation-map-source-" + size[0] + "-gui2");
                if (!before) {
                    var issued = source.operations().getFirst();
                    if (issued.progress() == null) throw new AssertionError("Current server must issue phase progress");
                    for (int phaseIndex = 0; phaseIndex < issued.phases().size(); phaseIndex++) {
                        var gson = new com.google.gson.Gson();
                        var json = gson.toJsonTree(source).getAsJsonObject();
                        var operation = json.getAsJsonArray("operations").get(0).getAsJsonObject();
                        operation.add("progress", gson.toJsonTree(new ProtocolAgencyView.Progress(phaseIndex, 30, 90)));
                        var sample = gson.fromJson(json, ProtocolAgencyView.class);
                        context.setScreen(() -> new ScreenAgency(null, sample.session(), sample, requests::add));
                        context.waitTicks(3);
                        context.runOnClient(client -> {
                            String icon = "agency/operations/" + sample.operations().getFirst().phases().get(sample.operations().getFirst().progress().index()).icon().toLowerCase(java.util.Locale.ROOT) + "/frame_0";
                            if (dev.hoi.client.ui.UiAssets.imageHeight(icon, 21) <= 0) throw new AssertionError("Missing issued phase picture");
                            if (dev.hoi.client.ui.UiAssets.imageHeight("agency/map/progress_fill/frame_0", 25) <= 0) throw new AssertionError("Missing source progress texture");
                        });
                        context.takeScreenshot("hoi-agency-operation-phase-synthetic-" + phaseIndex + "-" + size[0] + "-gui2");
                    }
                    context.setScreen(() -> new ScreenAgency(null, source.session(), source, requests::add));
                    context.waitTicks(3);
                }
                if (!before) {
                    int[] hover = context.computeOnClient(client -> {
                        try {
                            var field = ScreenAgency.class.getDeclaredField("operationMap"); field.setAccessible(true);
                            var hit = ((MapAgencyOperations)field.get(client.gui.screen())).hits().getFirst();
                            return new int[]{hit.x() * 2, hit.y() * 2};
                        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                    });
                    context.getInput().setCursorPos(hover[0], hover[1]); context.waitTicks(2);
                    context.takeScreenshot("hoi-agency-operation-tooltip-source-" + size[0] + "-gui2");
                }
                if (!before) context.runOnClient(client -> {
                    MapAgencyOperations overlay;
                    try {
                        var field = ScreenAgency.class.getDeclaredField("operationMap"); field.setAccessible(true);
                        overlay = (MapAgencyOperations)field.get(client.gui.screen());
                    } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                    if (overlay.hits().size() != 1) throw new AssertionError("Actual operation location must have one map icon");
                    var hit = overlay.hits().getFirst();
                    if (!source.operations().getFirst().detail().isEmpty() && !hit.tooltip().equals(source.operations().getFirst().detail())) throw new AssertionError("Server operation detail must be preserved exactly");
                    if (!hit.item().equals(source.operations().getFirst().item())) throw new AssertionError("Operation icon lost issued id");
                    if (!hit.tooltip().equals("§Y" + source.operations().getFirst().name() + "§!\n작전이 §H" + source.operations().getFirst().completes() + "§!에 완료될 것입니다\n\n세부사항을 보려면 §G클릭§!.")) throw new AssertionError("Original operation tooltip or authoritative completion date changed");
                    var screen = (ScreenAgency)client.gui.screen();
                    var right = new net.minecraft.client.input.MouseButtonEvent(hit.x(), hit.y(), new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0));
                    screen.mouseClicked(right, false);
                    if (client.gui.screen() != screen) throw new AssertionError("Right click opened operation");
                    var left = new net.minecraft.client.input.MouseButtonEvent(hit.x(), hit.y(), new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                    screen.mouseClicked(left, false);
                    if (client.gui.screen() == screen || !client.gui.screen().getTitle().getString().equals(source.operations().getFirst().name())) throw new AssertionError("Map operation did not open exact detail");
                    var frame = MapAgencyNetworks.extract();
                    if (frame.isEmpty()) throw new AssertionError("Agency screen lost network snapshot");
                    ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);
                    if (!MapAgencyNetworks.extract().isEmpty() || MapAgencyHud.clickAt(cardX, cardY, InputConstants.MOUSE_BUTTON_LEFT)) throw new AssertionError("Revoked country retained agency map input");
                    if (!requests.isEmpty()) throw new AssertionError("Viewing map issued an agency mutation");
                });
                context.runOnClient(client -> install(source, menu));
                for (int slots : List.of(5, 1, 0)) {
                    var gson = new com.google.gson.Gson();
                    var json = gson.toJsonTree(source).getAsJsonObject();
                    json.addProperty("operativeCapacity", slots);
                    var sample = gson.fromJson(json, ProtocolAgencyView.class);
                    context.setScreen(() -> new ScreenAgency(null, sample.session(), sample, requests::add));
                    context.waitTicks(3);
                    context.runOnClient(client -> {
                        long cards = ((List<?>)field("hits")).stream().filter(hit -> !hitText(hit, "operative").isEmpty() || hitText(hit, "item").equals("recruit")).count();
                        if (cards != Math.max(slots, source.operatives().size())) throw new AssertionError("Slot layout hides existing operatives or adds an extra card: " + cards);
                        if (((ScreenAgency)client.gui.screen()).snapshot().operatives().size() != source.operatives().size()) throw new AssertionError("Reduced slots must preserve every operative");
                    });
                    context.takeScreenshot("hoi-agency-slot-capacity-" + slots + "-" + size[0] + "-gui2");
                }
                requests.clear();
                context.setScreen(() -> new ScreenAgency(null, source.session(), source, requests::add));
                context.waitTicks(3);
                context.runOnClient(client -> {
                    var screen = (ScreenAgency)client.gui.screen();
                    var disabled = source.items().stream().filter(item -> item.id().startsWith("dismiss:") && !item.enabled()).findFirst().orElseThrow();
                    screen.showMapItem(disabled.id());
                    if (!requests.isEmpty()) throw new AssertionError("Busy operative dismissal must be disabled");
                    var enabled = source.items().stream().filter(item -> item.id().startsWith("dismiss:") && item.enabled()).findFirst().orElseThrow();
                    var hit = ((List<?>)field("hits")).stream().filter(value -> hitText(value, "item").equals(enabled.id())).findFirst().orElseThrow();
                    int x = hitNumber(hit, "x") + 2, y = hitNumber(hit, "y") + 2;
                    MapAgencyHud.clickAt(x, y, InputConstants.MOUSE_BUTTON_RIGHT);
                    if (!requests.isEmpty()) throw new AssertionError("Right dismiss click sends no request");
                    MapAgencyHud.clickAt(x, y, InputConstants.MOUSE_BUTTON_LEFT);
                    screen.showMapItem(enabled.id());
                    if (!requests.isEmpty() || client.gui.screen() == screen) throw new AssertionError("Dismiss must await confirmation");
                    client.gui.screen().onClose();
                    if (!requests.isEmpty() || client.gui.screen() != screen) throw new AssertionError("Cancel must return without dismissing");
                    screen.showMapItem(enabled.id());
                    confirmDismiss();
                    if (requests.size() != 1 || requests.getFirst().kind() != ProtocolAgencyPayloads.Kind.CALL || !requests.getFirst().item().equals(enabled.id())) throw new AssertionError("OK must send one issued dismiss request");
                    screen.update(source);
                    screen.showMapItem(enabled.id());
                    if (requests.size() != 1) throw new AssertionError("Stale reply must retain pending lock");
                    var gson = new com.google.gson.Gson();
                    var failure = gson.toJsonTree(source).getAsJsonObject();
                    failure.addProperty("revision", source.revision() + 1);
                    failure.addProperty("message", "해임 거절");
                    screen.update(gson.fromJson(failure, ProtocolAgencyView.class));
                    if (!screen.snapshot().operatives().equals(source.operatives())) throw new AssertionError("Failed dismiss must preserve server operatives");
                    screen.showMapItem(enabled.id());
                    confirmDismiss();
                    if (requests.size() != 2 || requests.getLast().revision() != source.revision() + 1) throw new AssertionError("New response permits authenticated retry");
                    ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);
                    if (MapAgencyHud.clickAt(x, y, InputConstants.MOUSE_BUTTON_LEFT) || requests.size() != 2) throw new AssertionError("Revoked country must block dismiss");
                });
                context.setScreen(() -> new ScreenAgency(new dev.hoi.client.screen.ScreenMapCursor(), source.session(), source, requests::add));
                context.runOnClient(client -> {
                    install(source, menu);
                    var enabled = source.items().stream().filter(item -> item.id().startsWith("dismiss:") && item.enabled()).findFirst().orElseThrow();
                    ((ScreenAgency)client.gui.screen()).showMapItem(enabled.id());
                    client.gui.screen().onClose();
                    if (!(client.gui.screen() instanceof dev.hoi.client.screen.ScreenMapCursor)) throw new AssertionError("Map dismissal cancel must preserve map backdrop");
                });
                context.setScreen(() -> new ScreenAgency(new dev.hoi.client.screen.ScreenMapCursor(), source.session(), source, requests::add));
                context.runOnClient(client -> {
                    install(source, menu);
                    var enabled = source.items().stream().filter(item -> item.id().startsWith("dismiss:") && item.enabled()).findFirst().orElseThrow();
                    ((ScreenAgency)client.gui.screen()).showMapItem(enabled.id());
                });
                context.waitTicks(2);
                context.takeScreenshot("hoi-agency-dismiss-confirmation-" + size[0] + "-gui2");
                context.runOnClient(client -> {
                    var dialog = client.gui.screen();
                    var ok = dialog.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance).map(net.minecraft.client.gui.components.Button.class::cast)
                            .filter(value -> value.getMessage().getString().equals("OK")).findFirst().orElseThrow();
                    int bx = ok.getX(), by = ok.getY();
                    var drag = new net.minecraft.client.input.MouseButtonEvent(bx - 90, by - 90, new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                    dialog.mouseClicked(drag, false); dialog.mouseDragged(drag, 8, 6); dialog.mouseReleased(drag);
                    var moved = dialog.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance).map(net.minecraft.client.gui.components.Button.class::cast)
                            .filter(value -> value.getMessage().getString().equals("OK")).findFirst().orElseThrow();
                    if (moved.getX() != bx + 8 || moved.getY() != by + 6) throw new AssertionError("Dismissal title must move the original window");
                    dialog.mouseDragged(drag, 8, 6);
                    if (moved.getX() != bx + 8) throw new AssertionError("Released dismissal title must stop dragging");
                    int countBefore = requests.size();
                    ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);
                    confirmDismiss();
                    if (requests.size() != countBefore || client.gui.screen() != null) throw new AssertionError("Revocation must block confirmation OK");
                });
                if (!before) missions(context, source, menu, size[0]);
                if (!before) { ranks(context, source, menu, size[0]); vacancies(context, directory, menu, size[0]); }
                context.setScreen(() -> null);
            }
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> { MapAgencyHud.reset(); MapSceneRenderer.clear(); ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN); client.options.guiScale().set(scale); client.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, hand); });
            var restore = server.submit(() -> {
                blocks.forEach((at, state) -> player.level().setBlock(at, state, 3));
                player.teleportTo(server.getLevel(dimension), position.x, position.y, position.z, java.util.Set.of(), rotation[0], rotation[1], false);
                player.setGameMode(gameMode);
            });
            context.waitFor(client -> restore.isDone()); restore.join();
            context.waitFor(client -> client.level != null && client.level.dimension().equals(dimension)
                    && client.player != null && client.player.position().distanceToSqr(position) < 1);
            context.waitTicks(3);
        }
    }
    private static void vacancies(ClientGameTestContext context, String directory, ProtocolMenuView menu, int width) {
        for (String name : List.of("kor-agency-recruit", "prc-agency-recruit-candidates")) {
            ProtocolAgencyView view;
            try { view = new ProtocolAgencyPayloads.Response(Files.readString(Path.of(directory, name + ".json"))).view(); }
            catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            var vacant = view.items().stream().filter(item -> item.id().startsWith("vacant:")).findFirst().orElseThrow();
            var sent = new java.util.ArrayList<ProtocolAgencyPayloads.Request>();
            context.runOnClient(client -> install(view, menu));
            context.setScreen(() -> new ScreenAgency(null, view.session(), view, sent::add));
            context.waitTicks(2);
            context.takeScreenshot("hoi-agency-vacant-" + (vacant.enabled() ? "ready" : "waiting-" + vacant.value()) + "-" + width + "-gui2");
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                var hit = ((List<?>)field("hits")).stream().filter(value -> hitText(value, "tooltip").equals(vacant.detail())).findFirst().orElseThrow();
                double x = hitNumber(hit, "x") + hitNumber(hit, "width") / 2.0, y = hitNumber(hit, "y") + hitNumber(hit, "height") / 2.0;
                MapAgencyHud.clickAt(x, y, InputConstants.MOUSE_BUTTON_RIGHT);
                if (client.gui.screen() != screen || !sent.isEmpty()) throw new AssertionError("Vacant slot right click must not recruit");
                MapAgencyHud.clickAt(x, y, InputConstants.MOUSE_BUTTON_LEFT);
                if ((client.gui.screen() != screen) != vacant.enabled() || !sent.isEmpty()) throw new AssertionError("Vacant slot must open candidates only when the server enables it");
            });
        }
    }
    private static void ranks(ClientGameTestContext context, ProtocolAgencyView source, ProtocolMenuView menu, int width) {
        var names = List.of("신병", "자산", "정보원", "비밀 요원", "전설");
        for (int level = 1; level <= names.size(); level++) {
            var json = new com.google.gson.Gson().toJsonTree(source).getAsJsonObject();
            var operative = json.getAsJsonArray("operatives").get(1).getAsJsonObject();
            operative.addProperty("level", level);
            operative.addProperty("rank", names.get(level - 1));
            operative.addProperty("progress", level == 5 ? -1 : 0.3);
            var boundary = new ProtocolAgencyPayloads.Response(json.toString()).view();
            context.runOnClient(client -> {
                install(boundary, menu); field("selected", boundary.operatives().get(1).id());
                var value = boundary.operatives().get(1);
                if (client.font.width(Integer.toString(value.level())) > 12 || client.font.width(value.rank()) > 96)
                    throw new AssertionError("Issued skill or rank exceeds badge bounds");
            });
            context.setScreen(ScreenMapCursor::new);
            context.waitTicks(2);
            context.takeScreenshot("hoi-agency-rank-boundary-" + level + "-" + width + "-gui2");
        }
    }
    private static void clickMission(String id, int button) {
        var hit = ((List<?>)field("hits")).stream().filter(value -> hitText(value, "item").equals(id)).findFirst().orElseThrow();
        MapAgencyHud.clickAt(hitNumber(hit, "x") + hitNumber(hit, "width") / 2.0, hitNumber(hit, "y") + hitNumber(hit, "height") / 2.0, button);
    }
    private static void confirmDismiss() {
        var screen = net.minecraft.client.Minecraft.getInstance().gui.screen();
        var button = screen.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance)
                .map(net.minecraft.client.gui.components.Button.class::cast).filter(value -> value.getMessage().getString().equals("OK")).findFirst().orElseThrow();
        var event = new net.minecraft.client.input.MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0,
                new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false); screen.mouseClicked(event, false);
    }
    private static void missions(ClientGameTestContext context, ProtocolAgencyView source, ProtocolMenuView menu, int width) {
        Object previous = field("transport");
        var sent = new java.util.ArrayList<ProtocolAgencyPayloads.Request>();
        var highlights = new java.util.ArrayList<ProtocolAgencyPayloads.Request>();
        String operative = source.operatives().get(1).id();
        String prefix = "assign:" + operative + ":";
        try {
            context.runOnClient(client -> { install(source, menu); field("transport", (java.util.function.Consumer<ProtocolAgencyPayloads.Request>)(request -> { if (request.kind() == ProtocolAgencyPayloads.Kind.HIGHLIGHT) highlights.add(request); else sent.add(request); })); field("selected", operative); });
            context.setScreen(ScreenMapCursor::new);
            context.waitTicks(3);
            context.takeScreenshot("hoi-agency-missions-enabled-" + width + "-gui2");
            for (String item : List.of("operative", "dismiss:" + operative, prefix + "COUNTER_INTELLIGENCE", prefix + "BUILD_NETWORK", prefix + "QUIET_NETWORK", prefix + "PROPAGANDA", prefix + "BOOST_IDEOLOGY", prefix + "CONTROL_TRADE", prefix + "DIPLOMATIC_PRESSURE", prefix + "ROOT_OUT_RESISTANCE")) {
                var point = context.computeOnClient(client -> {
                    var hit = ((List<?>)field("hits")).stream().filter(value -> item.equals("operative") ? hitText(value, "operative").equals(operative) : hitText(value, "item").equals(item)).findFirst().orElseThrow();
                    String text = hitText(hit, "tooltip");
                    if (item.startsWith(prefix) && !text.contains("§!\n\n")) throw new AssertionError("Mission title must have one blank separator");
                    dev.hoi.client.ui.UiTooltips.officerText(text).visit((style, part) -> {
                        if (style.isBold()) throw new AssertionError("Source §L prose must not become Minecraft bold");
                        return java.util.Optional.empty();
                    }, net.minecraft.network.chat.Style.EMPTY);
                    return new int[]{(hitNumber(hit, "x") + hitNumber(hit, "width") / 2) * 2, (hitNumber(hit, "y") + hitNumber(hit, "height") - 3) * 2, hitNumber(hit, "y")};
                });
                context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(2);
                var bounds = context.computeOnClient(client -> (dev.hoi.client.ui.UiTooltips.ScrolledTooltip)field("tooltip"));
                if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) throw new AssertionError("Hovered agency tooltip must actually render");
                if (bounds.y() - 4 < 0 || bounds.y() + bounds.height() + 4 > point[2]) throw new AssertionError("Bottom HUD tooltip must fit above its complete control");
                var capture = context.takeScreenshot("hoi-agency-tooltip-" + (item.equals("operative") ? item : item.startsWith("dismiss:") ? "dismiss" : item.substring(item.lastIndexOf(':') + 1).toLowerCase(java.util.Locale.ROOT)) + "-" + width + "-gui2");
                try {
                    var image = javax.imageio.ImageIO.read(capture.toFile());
                    if ((image.getRGB((bounds.x() - 4) * 2, (bounds.y() - 4) * 2) & 0xFFFFFF) != 0x9A45AB) throw new AssertionError("Agency tooltip border must reach the actual framebuffer");
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            }
            context.getInput().setCursorPos(width - 3, 3);
            for (String mission : List.of("COUNTER_INTELLIGENCE", "BUILD_NETWORK")) {
                ProtocolAgencyPayloads.Highlight[] packets;
                String filename = mission.equals("COUNTER_INTELLIGENCE") ? "kor-counter-intelligence.json" : "kor-build-network.json";
                try {
                    packets = new com.google.gson.Gson().fromJson(Files.readString(Path.of(System.getenv("HOI_AGENCY_OPERATION_PREVIEW"))
                            .resolveSibling("agency-highlight-source-fixtures").resolve(filename)), ProtocolAgencyPayloads.Highlight[].class);
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                int expected = mission.equals("COUNTER_INTELLIGENCE") ? 59 : 2632;
                if (java.util.Arrays.stream(packets).mapToInt(packet -> packet.page().boxes().size()).sum() != expected) throw new AssertionError("Authoritative highlight fixture changed");
                context.runOnClient(client -> {
                    clickMission(prefix + mission, InputConstants.MOUSE_BUTTON_LEFT);
                    for (var packet : packets) {
                        if (!packet.session().equals("fixture") || packet.revision() != 1 || !packet.item().equals(prefix + mission)) throw new AssertionError("Highlight fixture authorization differs");
                        var page = packet.page();
                        MapAgencyHud.accept(new ProtocolAgencyPayloads.Highlight(source.session(), source.revision(), packet.item(),
                                new ProtocolAtlasScene.Page(page.scene(), client.level.dimension().identifier().toString(), page.index(), page.count(), page.boxes())));
                    }
                    var mesh = MapAgencyHud.highlight();
                    if (mesh == null || mesh.tiles().stream().mapToInt(tile -> tile.vertices().length).sum() != expected * 6 * 4 * 8) throw new AssertionError("Actual highlight pages lost source geometry");
                });
                context.waitTicks(3);
                context.takeScreenshot("hoi-agency-highlight-" + mission.toLowerCase(java.util.Locale.ROOT) + "-" + width + "-gui2");
                context.runOnClient(client -> {
                    MapAgencyHud.deselect();
                    if (MapAgencyHud.highlight() != null) throw new AssertionError("Cancelling actual highlight must clear geometry");
                });
            }
            context.runOnClient(client -> {
                clickMission("deselect", InputConstants.MOUSE_BUTTON_RIGHT);
                if (!field("selected").equals(operative)) throw new AssertionError("Right badge X must not dismiss or deselect");
                clickMission("deselect", InputConstants.MOUSE_BUTTON_LEFT);
                if (!field("selected").equals("") || !sent.isEmpty()) throw new AssertionError("Badge X must deselect without a mutation");
                field("selected", operative);
            });
            context.waitTicks(2);
            context.runOnClient(client -> {
                clickMission(prefix + "BUILD_NETWORK", InputConstants.MOUSE_BUTTON_LEFT);
                if (!field("targeting").equals(prefix + "BUILD_NETWORK") || !sent.isEmpty()) throw new AssertionError("Mission selection must wait for map target");
                var highlightRequest = highlights.getLast();
                if (!highlightRequest.item().equals(prefix + "BUILD_NETWORK") || !highlightRequest.session().equals(source.session()) || highlightRequest.revision() != source.revision() || !highlightRequest.arguments().isEmpty()) throw new AssertionError("Highlight request must retain exact authorization and have no action arguments");
                highlightPages(source, prefix + "BUILD_NETWORK");
                MapAgencyHud.clickAt(client.getWindow().getGuiScaledWidth() / 2, client.getWindow().getGuiScaledHeight() / 2, InputConstants.MOUSE_BUTTON_RIGHT);
                if (!field("targeting").equals("") || !sent.isEmpty() || MapAgencyHud.highlight() != null) throw new AssertionError("Right map click must cancel target selection and clear geometry");
                clickMission(prefix + "BUILD_NETWORK", InputConstants.MOUSE_BUTTON_LEFT);
                MapAgencyHud.clickAt(client.getWindow().getGuiScaledWidth() / 2, client.getWindow().getGuiScaledHeight() / 2, InputConstants.MOUSE_BUTTON_LEFT);
                if (sent.size() != 1 || !sent.getFirst().arguments().getFirst().startsWith("ray:")) throw new AssertionError("Map target must send camera ray");
                var request = sent.getFirst();
                if (!request.item().equals(prefix + "BUILD_NETWORK") || request.revision() != source.revision() || !request.session().equals(source.session())) throw new AssertionError("Mission request lost authorization context");
                String[] values = request.arguments().getFirst().substring(4).split(",");
                new ProtocolCommanderHud.Ray(Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]));
                MapAgencyHud.accept(source);
                clickMission(prefix + "COUNTER_INTELLIGENCE", InputConstants.MOUSE_BUTTON_RIGHT);
                if (sent.size() != 1) throw new AssertionError("Pending mission and stale reply must not allow a second request");
                var gson = new com.google.gson.Gson();
                var json = gson.toJsonTree(source).getAsJsonObject(); json.addProperty("revision", source.revision() + 1); json.addProperty("message", "대상 거절");
                MapAgencyHud.accept(gson.fromJson(json, ProtocolAgencyView.class));
                if (!MapAgencyHud.snapshot().operatives().equals(source.operatives())) throw new AssertionError("Rejected mission must preserve server operatives");
            });
            context.waitTicks(2);
            context.runOnClient(client -> {
                clickMission(prefix + "COUNTER_INTELLIGENCE", InputConstants.MOUSE_BUTTON_RIGHT);
                if (sent.size() != 2 || !sent.getLast().arguments().equals(List.of(source.country()))) throw new AssertionError("Counterintelligence right click must send issued home country");
                install(source, menu); field("selected", operative); sent.clear();
            });
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (dev.hoi.client.ui.UiAssets.imageHeight("agency/operations/gfx_button_123x34/frame_0", 92) <= 0) throw new AssertionError("Missing original ideology button background");
                clickMission(prefix + "BOOST_IDEOLOGY", InputConstants.MOUSE_BUTTON_LEFT);
                MapAgencyHud.clickAt(client.getWindow().getGuiScaledWidth() / 2, client.getWindow().getGuiScaledHeight() / 2, InputConstants.MOUSE_BUTTON_LEFT);
                if (field("targetRay").equals("") || !sent.isEmpty()) throw new AssertionError("Ideology mission must await issued ideology choice");
            });
            context.waitTicks(2);
            context.takeScreenshot("hoi-agency-mission-ideology-" + width + "-gui2");
            context.runOnClient(client -> {
                String ideology = source.items().stream().filter(item -> item.id().equals(prefix + "BOOST_IDEOLOGY")).findFirst().orElseThrow().parameters().get(1).choices().getFirst().value();
                clickMission("ideology:" + ideology, InputConstants.MOUSE_BUTTON_LEFT);
                clickMission("ideology:" + ideology, InputConstants.MOUSE_BUTTON_LEFT);
                if (sent.size() != 1 || sent.getFirst().arguments().size() != 2 || !sent.getFirst().arguments().get(1).equals(ideology)) throw new AssertionError("Ideology selection must send one issued two-argument request");
                ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);
                if (MapAgencyHud.clickAt(320, 180, InputConstants.MOUSE_BUTTON_LEFT) || sent.size() != 1) throw new AssertionError("Revocation must block mission input");
            });
        } finally { context.runOnClient(client -> { field("transport", previous); MapAgencyHud.reset(); }); }
    }
    private static void highlightPages(ProtocolAgencyView source, String item) {
        String dimension = net.minecraft.client.Minecraft.getInstance().level.dimension().identifier().toString();
        var scene = java.util.UUID.randomUUID();
        var box = new ProtocolAtlasScene.Box(0, 70, 0, 1, .1f, 1, 0, ProtocolAtlasScene.Material.DEPLOYMENT);
        var first = new ProtocolAtlasScene.Page(scene, dimension, 0, 2, List.of(box));
        var last = new ProtocolAtlasScene.Page(scene, dimension, 1, 2, List.of(box));
        MapAgencyHud.accept(new ProtocolAgencyPayloads.Highlight("wrong", source.revision(), item, first));
        MapAgencyHud.accept(new ProtocolAgencyPayloads.Highlight(source.session(), source.revision() + 1, item, last));
        if (MapAgencyHud.highlight() != null) throw new AssertionError("Wrong authorization must not publish geometry");
        MapAgencyHud.accept(new ProtocolAgencyPayloads.Highlight(source.session(), source.revision(), item, last));
        if (MapAgencyHud.highlight() != null) throw new AssertionError("Missing first page must not publish geometry");
        MapAgencyHud.accept(new ProtocolAgencyPayloads.Highlight(source.session(), source.revision(), item, first));
        if (MapAgencyHud.highlight() != null) throw new AssertionError("Partial highlight must not publish geometry");
        MapAgencyHud.accept(new ProtocolAgencyPayloads.Highlight(source.session(), source.revision(), item + "wrong", last));
        if (MapAgencyHud.highlight() != null) throw new AssertionError("Wrong item must not publish geometry");
        MapAgencyHud.accept(new ProtocolAgencyPayloads.Highlight(source.session(), source.revision(), item, last));
        if (MapAgencyHud.highlight() == null || MapAgencyHud.highlight().tiles().isEmpty()) throw new AssertionError("Complete highlight must publish geometry");
        MapAgencyHud.accept(new ProtocolAgencyPayloads.Highlight(source.session(), source.revision(), item, new ProtocolAtlasScene.Page(java.util.UUID.randomUUID(), dimension, 0, 1, List.of())));
        if (MapAgencyHud.highlight() == null || !MapAgencyHud.highlight().tiles().isEmpty()) throw new AssertionError("Empty highlight must clear prior country geometry");
    }
    private static Object hitValue(Object hit, String name) {
        try { var method = hit.getClass().getDeclaredMethod(name); method.setAccessible(true); return method.invoke(hit); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static String hitText(Object hit, String name) { return (String)hitValue(hit, name); }
    private static int hitNumber(Object hit, String name) { return (Integer)hitValue(hit, name); }
}
