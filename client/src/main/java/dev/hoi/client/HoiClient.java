package dev.hoi.client;

import dev.hoi.client.network.NetworkScreens;

import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.map.MapSceneRenderer;
import dev.hoi.client.screen.ScreenDialogCoordinator;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.ProtocolResearchPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

public final class HoiClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ProtocolResearchPayloads.registerPayloadTypes();
        ClientCampaignHud.register();
        dev.hoi.client.map.MapUnitHud.register();
        dev.hoi.client.map.MapCommanderHud.register();
        dev.hoi.client.map.MapOccupationHud.register();
        ScreenDialogCoordinator.register();
        dev.hoi.protocol.ProtocolAudioPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolAudioPayloads.Signal.TYPE, (packet, context) -> AudioUiSounds.receive(packet.cue()));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolAudioPayloads.Music.TYPE, (packet, context) -> dev.hoi.client.audio.AudioStoryMusic.play(packet.event()));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolUnitAudio.Snapshot.TYPE, (packet, context) -> dev.hoi.client.audio.AudioUnitVoices.receive(packet));
        MapSceneRenderer.register();
        dev.hoi.protocol.ProtocolConstructionPayloads.registerPayloadTypes();
        dev.hoi.protocol.ProtocolCountryPayloads.registerPayloadTypes();
        dev.hoi.protocol.ProtocolWorldTension.registerPayloadTypes();
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(Identifier.fromNamespaceAndPath("hoi", "ui_image_dimensions"),
                        (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> { UiAssets.clear(); net.minecraft.client.Minecraft.getInstance().execute(dev.hoi.client.audio.AudioUnitVoices::reset); MapSceneRenderer.resourcesReloaded(); dev.hoi.client.audio.AudioKoreanMusic.reload(manager); });
        ClientTickEvents.START_CLIENT_TICK.register(InputSidebarMovement::tick);
        NetworkScreens.register();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> registerCommands(dispatcher));
        ClientTickEvents.END_CLIENT_TICK.register(client -> { dev.hoi.client.audio.AudioUnitVoices.tick(); dev.hoi.client.audio.AudioStoryMusic.tick(); dev.hoi.client.audio.AudioKoreanMusic.tick(); });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            dev.hoi.client.screen.ScreenIndustry.clearPreferences();
            dev.hoi.client.input.InputMenuShortcut.reset(); AudioUiSounds.reset(); dev.hoi.client.audio.AudioUnitVoices.reset(); dev.hoi.client.audio.AudioStoryMusic.reset(); dev.hoi.client.audio.AudioKoreanMusic.PLAYBACK.stop(); InputSidebarMovement.release(client); dev.hoi.client.ui.UiMenuBar.clear();
        }));
    }
    static void registerCommands(com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("hoi-music").executes(context -> { var client = net.minecraft.client.Minecraft.getInstance(); client.gui.setScreen(new dev.hoi.client.screen.ScreenKoreanMusic(client.gui.screen())); return 1; }));
    }
    public static void open() { NetworkScreens.open(); }
    public static void openResearchDetail(String id) { NetworkScreens.openResearchDetail(id); }
    public static void cancelOpen() { NetworkScreens.cancelOpen(); }
    public static void openCountry(String target) { NetworkScreens.openCountry(target); }
    public static void openWorldTension() { NetworkScreens.openWorldTension(); }
    public static void openConstruction() { NetworkScreens.openConstruction(); }
    public static void openIndustry(dev.hoi.protocol.ProtocolMenuTab tab) { NetworkScreens.openIndustry(tab); }
    public static void openAgency() { NetworkScreens.openAgency(); }
    public static void openMenu(dev.hoi.protocol.ProtocolMenuTab tab) { NetworkScreens.openMenu(tab); }
    public static void send(ProtocolResearchPayloads.Request request) { NetworkScreens.send(request); }
}
