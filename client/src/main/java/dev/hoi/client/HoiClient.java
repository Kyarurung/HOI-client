package dev.hoi.client;

import dev.hoi.client.audio.UiSounds;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.map.AtlasSceneClient;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.client.screen.AgencyScreen;
import dev.hoi.client.screen.ConstructionScreen;
import dev.hoi.client.screen.CountryScreen;
import dev.hoi.client.screen.DialogClient;
import dev.hoi.client.screen.HoiMenuScreen;
import dev.hoi.client.screen.IndustryScreen;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.ResearchProtocol;
import dev.hoi.protocol.MenuProtocol;
import dev.hoi.protocol.AgencyProtocol;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class HoiClient implements ClientModInitializer {
    private static int awaiting;
    private static String researchTarget;
    private static HoiMenuScreen pendingMenu;
    private static int menuWait;
    @Override public void onInitializeClient() {
        ResearchProtocol.registerPayloadTypes();
        DialogClient.register();
        dev.hoi.protocol.AudioProtocol.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.AudioProtocol.Signal.TYPE, (packet, context) -> UiSounds.receive(packet.cue()));
        AtlasSceneClient.register();
        dev.hoi.protocol.ConstructionProtocol.registerPayloadTypes();
        dev.hoi.protocol.CountryProtocol.registerPayloadTypes();
        dev.hoi.protocol.WorldTensionProtocol.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.WorldTensionProtocol.Response.TYPE, (packet, context) -> {
            if (DialogClient.contentScreen() instanceof dev.hoi.client.screen.WorldTensionScreen screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { context.client().gui.setScreen(null); message("세계 긴장도 정보를 읽을 수 없습니다."); }
            }
        });
        dev.hoi.protocol.IndustryProtocol.registerPayloadTypes();
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(Identifier.fromNamespaceAndPath("hoi", "ui_image_dimensions"),
                        (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> { UiAssets.clear(); AtlasSceneClient.resourcesReloaded(); });
        ClientTickEvents.START_CLIENT_TICK.register(SidebarMovement::tick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            UiSounds.tick();
            if (menuWait > 0 && --menuWait == 0) pendingMenu = null;
            if (awaiting > 0 && --awaiting == 0) { researchTarget = null; message("연구 화면 응답이 없습니다. 다시 열어주세요."); }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> registerCommands(dispatcher));
        ClientPlayNetworking.registerGlobalReceiver(ResearchProtocol.OpenScreen.TYPE, (packet, context) -> open());
        ClientPlayNetworking.registerGlobalReceiver(MenuProtocol.OpenScreen.TYPE, (packet, context) -> {
            try {
                var view = packet.view();
                awaiting = 0;
                if (DialogClient.contentScreen() instanceof HoiMenuScreen menu) menu.update(view);
                else context.client().gui.setScreen(new HoiMenuScreen(view));
            } catch (RuntimeException e) { message("HOI 메뉴 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.CountryProtocol.OpenScreen.TYPE, (packet, context) -> openCountry(packet.target()));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.CountryProtocol.Response.TYPE, (packet, context) -> {
            if (DialogClient.contentScreen() instanceof CountryScreen screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { context.client().gui.setScreen(null); message("국가 정보를 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(MenuProtocol.Update.TYPE, (packet, context) -> {
            if (pendingMenu != null && pendingMenu.token().equals(packet.screen())) {
                var requested = pendingMenu;
                pendingMenu = null; menuWait = 0;
                if (packet.json().isEmpty()) return;
                try {
                    context.client().gui.setScreen(new HoiMenuScreen(new MenuProtocol.OpenScreen(packet.json()).view(), requested.selectedTab(), HoiClient::open));
                } catch (RuntimeException error) { message("HOI 메뉴 데이터를 읽을 수 없습니다."); }
                return;
            }
            if (!(DialogClient.contentScreen() instanceof HoiMenuScreen menu) || !menu.token().equals(packet.screen())) return;
            if (packet.json().isEmpty()) { context.client().gui.setScreen(null); return; }
            try { menu.update(new MenuProtocol.OpenScreen(packet.json()).view()); }
            catch (RuntimeException error) { message("HOI 메뉴 갱신 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ConstructionProtocol.Response.TYPE, (packet, context) -> {
            if(DialogClient.contentScreen() instanceof ConstructionScreen screen) {
                try {screen.update(packet.view());} catch(IllegalArgumentException e){message("건설 데이터를 읽을 수 없습니다.");}
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.IndustryProtocol.Response.TYPE, (packet, context) -> {
            if (DialogClient.contentScreen() instanceof IndustryScreen screen) {
                try { screen.update(packet.view()); }
                catch (IllegalArgumentException error) { context.client().gui.setScreen(null); message("산업 현황을 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AgencyProtocol.Response.TYPE, (packet, context) -> {
            if (!(DialogClient.contentScreen() instanceof AgencyScreen agency)) return;
            try { agency.update(packet.view()); }
            catch (IllegalArgumentException error) { message("정보기관 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(ResearchProtocol.Response.TYPE, (packet, context) -> {
            try {
                var view = packet.view();
                var current = DialogClient.contentScreen();
                if (view.country().isEmpty()) {
                    cancelOpen();
                    if (current instanceof ResearchScreen) context.client().gui.setScreen(null);
                    message(view.message());
                } else if (awaiting > 0) {
                    String target = researchTarget;
                    cancelOpen();
                    var screen = new ResearchScreen(view);
                    context.client().gui.setScreen(screen);
                    if (target != null && view.technology(target) != null) screen.showDetail(target);
                } else if (current instanceof ResearchScreen screen && screen.session().equals(view.session())) {
                    screen.update(view);
                } else {

                    send(new ResearchProtocol.Request(ResearchProtocol.Action.CLOSE, view.session(), 0, -1, ""));
                }
            } catch (IllegalArgumentException e) { cancelOpen(); message("HOI 연구 화면 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            UiSounds.reset();
            SidebarMovement.release(client);
            cancelOpen();
            if (client.gui.screen() instanceof dev.hoi.client.screen.WorldTensionScreen) client.gui.setScreen(null);
            if (client.gui.screen() instanceof ResearchScreen || client.gui.screen() instanceof HoiMenuScreen || client.gui.screen() instanceof AgencyScreen || client.gui.screen() instanceof ConstructionScreen || client.gui.screen() instanceof CountryScreen || client.gui.screen() instanceof IndustryScreen) client.gui.setScreen(null);
        });
    }
    static void registerCommands(com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dispatcher) {

        dispatcher.register(ClientCommands.literal("hoi-research").executes(context -> { open(); return 1; }));
    }
    public static void open() {
        if (awaiting > 0 || Minecraft.getInstance().gui.screen() instanceof ResearchScreen) return;
        if (!ClientPlayNetworking.canSend(ResearchProtocol.Request.TYPE)) { message("이 서버는 HOI 연구 UI를 지원하지 않습니다."); return; }
        awaiting = 100;
        send(new ResearchProtocol.Request(ResearchProtocol.Action.OPEN, "", 0, -1, ""));
    }
    public static void openResearchDetail(String id) {
        cancelOpen();
        open();
        if (awaiting > 0) researchTarget = id;
    }
    public static void cancelOpen() { awaiting = 0; researchTarget = null; pendingMenu = null; menuWait = 0; }
    public static void openCountry(String target) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.CountryProtocol.Request.TYPE)) { message("이 서버는 외국 정보 UI를 지원하지 않습니다."); return; }
        cancelOpen(); Minecraft.getInstance().gui.setScreen(new CountryScreen(target));
    }
    public static void openWorldTension() {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.WorldTensionProtocol.Request.TYPE)) { message("이 서버는 세계 긴장도 이력 UI를 지원하지 않습니다."); return; }
        cancelOpen(); Minecraft.getInstance().gui.setScreen(new dev.hoi.client.screen.WorldTensionScreen());
    }
    public static void openConstruction() {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.ConstructionProtocol.Request.TYPE)) { message("이 서버는 건설 UI를 지원하지 않습니다."); return; }
        cancelOpen();var screen=new ConstructionScreen();Minecraft.getInstance().gui.setScreen(screen);screen.open();
    }
    public static void openIndustry(dev.hoi.protocol.MenuTab tab) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.IndustryProtocol.Request.TYPE)) { message("이 서버는 산업 UI를 지원하지 않습니다."); return; }
        cancelOpen(); var screen = new IndustryScreen(tab); Minecraft.getInstance().gui.setScreen(screen); screen.open();
    }
    public static void openMenu(dev.hoi.protocol.MenuTab tab) {
        if (IndustryScreen.supports(tab)) { openIndustry(tab); return; }
        if(tab==dev.hoi.protocol.MenuTab.CONSTRUCTION){openConstruction();return;}
        if (!ClientPlayNetworking.canSend(MenuProtocol.Refresh.TYPE)) { message("이 서버는 HOI 국가 메뉴를 지원하지 않습니다."); return; }
        cancelOpen();
        pendingMenu = new HoiMenuScreen(tab);
        menuWait = 100;
        ClientPlayNetworking.send(new MenuProtocol.Refresh(pendingMenu.token()));
    }
    public static void send(ResearchProtocol.Request request) {
        if (Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(ResearchProtocol.Request.TYPE)) ClientPlayNetworking.send(request);
    }
    private static void message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal(text));
    }
}
