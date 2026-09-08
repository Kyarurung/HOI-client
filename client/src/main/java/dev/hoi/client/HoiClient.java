package dev.hoi.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.ResearchProtocol;
import dev.hoi.protocol.MenuProtocol;
import dev.hoi.protocol.AgencyProtocol;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class HoiClient implements ClientModInitializer {
    private static int awaiting;
    @Override public void onInitializeClient() {
        ResearchProtocol.registerPayloadTypes();
        dev.hoi.protocol.ConstructionProtocol.registerPayloadTypes();
        dev.hoi.protocol.CountryProtocol.registerPayloadTypes();
        dev.hoi.protocol.IndustryProtocol.registerPayloadTypes();
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(Identifier.fromNamespaceAndPath("hoi", "ui_image_dimensions"),
                        (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> UiAssets.clear());
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("hoi", "strategy"));
        var key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hoi.research", InputConstants.Type.KEYSYM, InputConstants.KEY_R, category));
        ClientTickEvents.START_CLIENT_TICK.register(SidebarMovement::tick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (awaiting > 0 && --awaiting == 0) message("연구 화면 응답이 없습니다. 다시 열어주세요.");
            while (key.consumeClick()) if (client.player != null && client.gui.screen() == null) open();
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> registerCommands(dispatcher));
        ClientPlayNetworking.registerGlobalReceiver(ResearchProtocol.OpenScreen.TYPE, (packet, context) -> open());
        ClientPlayNetworking.registerGlobalReceiver(MenuProtocol.OpenScreen.TYPE, (packet, context) -> {
            try {
                var view = packet.view();
                awaiting = 0;
                if (context.client().gui.screen() instanceof HoiMenuScreen menu) menu.update(view);
                else context.client().gui.setScreen(new HoiMenuScreen(view));
            } catch (RuntimeException e) { message("HOI 메뉴 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.CountryProtocol.OpenScreen.TYPE, (packet, context) -> openCountry(packet.target()));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.CountryProtocol.Response.TYPE, (packet, context) -> {
            if (context.client().gui.screen() instanceof CountryScreen screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { context.client().gui.setScreen(null); message("국가 정보를 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(MenuProtocol.Update.TYPE, (packet, context) -> {
            if (!(context.client().gui.screen() instanceof HoiMenuScreen menu) || !menu.token().equals(packet.screen())) return;
            if (packet.json().isEmpty()) { context.client().gui.setScreen(null); return; }
            try { menu.update(new MenuProtocol.OpenScreen(packet.json()).view()); }
            catch (RuntimeException error) { message("HOI 메뉴 갱신 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ConstructionProtocol.Response.TYPE, (packet, context) -> {
            if(context.client().gui.screen() instanceof ConstructionScreen screen) {
                try {screen.update(packet.view());} catch(IllegalArgumentException e){message("건설 데이터를 읽을 수 없습니다.");}
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.IndustryProtocol.Response.TYPE, (packet, context) -> {
            if (context.client().gui.screen() instanceof IndustryScreen screen) {
                try { screen.update(packet.view()); }
                catch (IllegalArgumentException error) { context.client().gui.setScreen(null); message("산업 현황을 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AgencyProtocol.Response.TYPE, (packet, context) -> {
            if (!(context.client().gui.screen() instanceof AgencyScreen agency)) return;
            try { agency.update(packet.view()); }
            catch (IllegalArgumentException error) { message("첩보기관 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(ResearchProtocol.Response.TYPE, (packet, context) -> {
            try {
                var view = packet.view();
                var current = context.client().gui.screen();
                if (view.country().isEmpty()) {
                    awaiting = 0;
                    if (current instanceof ResearchScreen) context.client().gui.setScreen(null);
                    message(view.message());
                } else if (awaiting > 0) {
                    awaiting = 0;
                    context.client().gui.setScreen(new ResearchScreen(view));
                } else if (current instanceof ResearchScreen screen && screen.session().equals(view.session())) {
                    screen.update(view);
                } else {
                    // A response may arrive after Esc, disconnect or another screen supersedes the request.
                    send(new ResearchProtocol.Request(ResearchProtocol.Action.CLOSE, view.session(), 0, -1, ""));
                }
            } catch (IllegalArgumentException e) { awaiting = 0; message("HOI 연구 화면 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SidebarMovement.release(client);
            awaiting = 0;
            if (client.gui.screen() instanceof ResearchScreen || client.gui.screen() instanceof HoiMenuScreen || client.gui.screen() instanceof AgencyScreen || client.gui.screen() instanceof ConstructionScreen || client.gui.screen() instanceof CountryScreen || client.gui.screen() instanceof IndustryScreen) client.gui.setScreen(null);
        });
    }
    static void registerCommands(com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dispatcher) {
        // A client root shadows ALL server subcommands. Keep /hoi exclusively server-owned.
        dispatcher.register(ClientCommands.literal("hoi-research").executes(context -> { open(); return 1; }));
    }
    public static void open() {
        if (awaiting > 0 || Minecraft.getInstance().gui.screen() instanceof ResearchScreen) return;
        if (!ClientPlayNetworking.canSend(ResearchProtocol.Request.TYPE)) { message("이 서버는 HOI 연구 UI를 지원하지 않습니다."); return; }
        awaiting = 100;
        send(new ResearchProtocol.Request(ResearchProtocol.Action.OPEN, "", 0, -1, ""));
    }
    static void cancelOpen() { awaiting = 0; }
    static void openCountry(String target) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.CountryProtocol.Request.TYPE)) { message("이 서버는 외국 정보 UI를 지원하지 않습니다."); return; }
        cancelOpen(); Minecraft.getInstance().gui.setScreen(new CountryScreen(target));
    }
    static void openConstruction() {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.ConstructionProtocol.Request.TYPE)) { message("이 서버는 건설 UI를 지원하지 않습니다."); return; }
        cancelOpen();var screen=new ConstructionScreen();Minecraft.getInstance().gui.setScreen(screen);screen.open();
    }
    static void openIndustry(dev.hoi.protocol.MenuTab tab) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.IndustryProtocol.Request.TYPE)) { message("이 서버는 산업 UI를 지원하지 않습니다."); return; }
        cancelOpen(); var screen = new IndustryScreen(tab); Minecraft.getInstance().gui.setScreen(screen); screen.open();
    }
    static void openMenu(dev.hoi.protocol.MenuTab tab) {
        if (IndustryScreen.supports(tab)) { openIndustry(tab); return; }
        if(tab==dev.hoi.protocol.MenuTab.CONSTRUCTION){openConstruction();return;}
        if (!ClientPlayNetworking.canSend(MenuProtocol.Refresh.TYPE)) { message("이 서버는 HOI 국가 메뉴를 지원하지 않습니다."); return; }
        cancelOpen();
        Minecraft.getInstance().gui.setScreen(new HoiMenuScreen(tab));
    }
    static void send(ResearchProtocol.Request request) {
        if (Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(ResearchProtocol.Request.TYPE)) ClientPlayNetworking.send(request);
    }
    private static void message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal(text));
    }
}
