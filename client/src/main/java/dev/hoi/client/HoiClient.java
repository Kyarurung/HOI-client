package dev.hoi.client;

import dev.hoi.client.network.ScreenNetworking;

import dev.hoi.client.audio.UiSounds;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.map.AtlasSceneClient;
import dev.hoi.client.screen.DialogClient;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.ResearchProtocol;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

public final class HoiClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ResearchProtocol.registerPayloadTypes();
        CampaignHud.register();
        dev.hoi.client.map.UnitHud.register();
        DialogClient.register();
        dev.hoi.protocol.AudioProtocol.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.AudioProtocol.Signal.TYPE, (packet, context) -> UiSounds.receive(packet.cue()));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.AudioProtocol.Music.TYPE, (packet, context) -> dev.hoi.client.audio.StoryMusicAudio.play(packet.event()));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.UnitAudioProtocol.Snapshot.TYPE, (packet, context) -> dev.hoi.client.audio.UnitAudio.receive(packet));
        AtlasSceneClient.register();
        dev.hoi.protocol.ConstructionProtocol.registerPayloadTypes();
        dev.hoi.protocol.CountryProtocol.registerPayloadTypes();
        dev.hoi.protocol.WorldTensionProtocol.registerPayloadTypes();
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(Identifier.fromNamespaceAndPath("hoi", "ui_image_dimensions"),
                        (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> { UiAssets.clear(); net.minecraft.client.Minecraft.getInstance().execute(dev.hoi.client.audio.UnitAudio::reset); AtlasSceneClient.resourcesReloaded(); dev.hoi.client.audio.KoreanMusic.reload(manager); });
        ClientTickEvents.START_CLIENT_TICK.register(SidebarMovement::tick);
        ScreenNetworking.register();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> registerCommands(dispatcher));
        ClientTickEvents.END_CLIENT_TICK.register(client -> { dev.hoi.client.audio.UnitAudio.tick(); dev.hoi.client.audio.StoryMusicAudio.tick(); dev.hoi.client.audio.KoreanMusic.tick(); });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            dev.hoi.client.input.MenuShortcut.reset(); UiSounds.reset(); dev.hoi.client.audio.UnitAudio.reset(); dev.hoi.client.audio.StoryMusicAudio.reset(); dev.hoi.client.audio.KoreanMusic.PLAYBACK.stop(); SidebarMovement.release(client); dev.hoi.client.ui.HoiMenuBar.clear();
        });
    }
    static void registerCommands(com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dispatcher) {

        dispatcher.register(ClientCommands.literal("hoi-music").executes(context -> { var client = net.minecraft.client.Minecraft.getInstance(); client.gui.setScreen(new dev.hoi.client.screen.KoreanMusicScreen(client.gui.screen())); return 1; }));
        dispatcher.register(ClientCommands.literal("hoi-research").executes(context -> { open(); return 1; }));
    }
    public static void open() { ScreenNetworking.open(); }
    public static void openResearchDetail(String id) { ScreenNetworking.openResearchDetail(id); }
    public static void cancelOpen() { ScreenNetworking.cancelOpen(); }
    public static void openCountry(String target) { ScreenNetworking.openCountry(target); }
    public static void openWorldTension() { ScreenNetworking.openWorldTension(); }
    public static void openConstruction() { ScreenNetworking.openConstruction(); }
    public static void openIndustry(dev.hoi.protocol.MenuTab tab) { ScreenNetworking.openIndustry(tab); }
    public static void openAgency() { ScreenNetworking.openAgency(); }
    public static void openMenu(dev.hoi.protocol.MenuTab tab) { ScreenNetworking.openMenu(tab); }
    public static void send(ResearchProtocol.Request request) { ScreenNetworking.send(request); }
}
