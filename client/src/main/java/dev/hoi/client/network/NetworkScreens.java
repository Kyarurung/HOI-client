package dev.hoi.client.network;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.client.screen.ScreenAgency;
import dev.hoi.client.screen.ScreenConstruction;
import dev.hoi.client.screen.ScreenCountry;
import dev.hoi.client.screen.ScreenDialogCoordinator;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.client.screen.ScreenIndustry;

import dev.hoi.protocol.ResearchProtocol;
import dev.hoi.protocol.MenuProtocol;
import dev.hoi.protocol.AgencyProtocol;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class NetworkScreens {
    private static final NetworkPendingScreen pendingResearch = new NetworkPendingScreen();
    private static final NetworkPendingScreen pendingMenu = new NetworkPendingScreen();
    private static dev.hoi.protocol.MenuTab pendingTab;
    private static net.minecraft.client.gui.screens.Screen menuOrigin, researchOrigin;
    private static String researchCountry = "";
    private static String menuCountry = "", menuToken = "";
    private static int menuRetryTicks;
    public static void register() {
        ResearchProtocol.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.FocusProtocol.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.ScreenFocus screen) {
                try { screen.update(packet); } catch (IllegalArgumentException e) { context.client().gui.setScreen(null); message("중점 정보를 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.DecisionProtocol.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.ScreenDecision screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { context.client().gui.setScreen(null); message("결정 정보를 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.WorldTensionProtocol.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.ScreenWorldTension screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { context.client().gui.setScreen(null); message("세계 긴장도 정보를 읽을 수 없습니다."); }
            }
        });
        dev.hoi.protocol.IndustryProtocol.registerPayloadTypes();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingMenu.waiting()) {
                if (!menuContextValid()) cancelOpen();
                else if (pendingMenu.tick()) { cancelOpen(); message("국가 메뉴 응답이 없습니다. 다시 열어주세요."); }
                else if (++menuRetryTicks >= 20 && ClientPlayNetworking.canSend(MenuProtocol.Refresh.TYPE)) {
                    menuRetryTicks = 0;
                    ClientPlayNetworking.send(new MenuProtocol.Refresh(menuToken));
                }
            }
            if (pendingResearch.waiting() && !researchContextValid()) cancelOpen();
            if (pendingResearch.tick()) message("연구 화면 응답이 없습니다. 다시 열어주세요.");
        });
        ClientPlayNetworking.registerGlobalReceiver(ResearchProtocol.OpenScreen.TYPE, (packet, context) -> open());
        ClientPlayNetworking.registerGlobalReceiver(MenuProtocol.SelectionPreview.TYPE, (packet, context) -> {
            if (packet.json().isEmpty()) {
                if (ScreenDialogCoordinator.contentScreen() instanceof ScreenMenu screen && screen.selectionPreview()) context.client().gui.setScreen(null);
                return;
            }
            try {
                cancelOpen();
                context.client().gui.setScreen(ScreenMenu.selection(packet.view()));
            } catch (IllegalArgumentException error) { message("국가 선택 정보를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(MenuProtocol.OpenScreen.TYPE, (packet, context) -> {
            try {
                var view = packet.view();
                cancelOpen();
                context.client().gui.setScreen(new ScreenMenu(view));
            } catch (RuntimeException e) { message("HOI 메뉴 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.CountryProtocol.OpenScreen.TYPE, (packet, context) -> openCountry(packet.target()));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.CountryProtocol.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof ScreenCountry screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { context.client().gui.setScreen(null); message("국가 정보를 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(MenuProtocol.Update.TYPE, (packet, context) -> receiveMenu(packet));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ConstructionProtocol.Response.TYPE, (packet, context) -> {
            if(ScreenDialogCoordinator.contentScreen() instanceof ScreenConstruction screen) {
                try {screen.update(packet.view());} catch(IllegalArgumentException e){message("건설 데이터를 읽을 수 없습니다.");}
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.IndustryProtocol.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof ScreenIndustry screen) {
                try { screen.update(packet.view()); }
                catch (IllegalArgumentException error) { context.client().gui.setScreen(null); message("산업 현황을 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AgencyProtocol.Response.TYPE, (packet, context) -> {
            if (!(ScreenDialogCoordinator.contentScreen() instanceof ScreenAgency agency)) return;
            try { agency.update(packet.view()); }
            catch (IllegalArgumentException error) { message("정보기관 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(ResearchProtocol.Response.TYPE, (packet, context) -> receiveResearch(packet));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            cancelOpen();
            if (client.gui.screen() instanceof dev.hoi.client.screen.ScreenWorldTension) client.gui.setScreen(null);
            if (client.gui.screen() instanceof dev.hoi.client.screen.ScreenFocus || client.gui.screen() instanceof dev.hoi.client.screen.ScreenDecision || client.gui.screen() instanceof ResearchScreen || client.gui.screen() instanceof ScreenMenu || client.gui.screen() instanceof ScreenAgency || client.gui.screen() instanceof ScreenConstruction || client.gui.screen() instanceof ScreenCountry || client.gui.screen() instanceof ScreenIndustry) client.gui.setScreen(null);
        });
    }
    static void receiveResearch(ResearchProtocol.Response packet) {
            try {
                var view = packet.view();
                var current = ScreenDialogCoordinator.contentScreen();
                if (view.country().isEmpty()) {
                    boolean active = current instanceof ResearchScreen screen && (view.session().isEmpty() || screen.session().equals(view.session()));
                    if (active || pendingResearch.matches(view.session())) {
                        cancelOpen();
                        if (active) Minecraft.getInstance().gui.setScreen(null);
                        message(view.message());
                    }
                } else if (pendingResearch.matches(view.session())) {
                    if (!researchContextValid() || !researchCountry.isEmpty() && !researchCountry.equals(view.country())) {
                        cancelOpen();
                        send(new ResearchProtocol.Request(ResearchProtocol.Action.CLOSE, view.session(), 0, -1, ""));
                        return;
                    }
                    String target = pendingResearch.target();
                    cancelOpen();
                    var screen = new ResearchScreen(view);
                    Minecraft.getInstance().gui.setScreen(screen);
                    if (target != null && view.technology(target) != null) screen.showDetail(target);
                } else if (current instanceof ResearchScreen screen && screen.session().equals(view.session())) {
                    screen.update(view);
                } else {

                    send(new ResearchProtocol.Request(ResearchProtocol.Action.CLOSE, view.session(), 0, -1, ""));
                }
            } catch (IllegalArgumentException e) { cancelOpen(); message("HOI 연구 화면 데이터를 읽을 수 없습니다."); }
    }
    static ResearchProtocol.Request beginResearch() {
        cancelOpen();
        researchOrigin = ScreenDialogCoordinator.contentScreen(); researchCountry = ClientCampaignHud.country();
        return new ResearchProtocol.Request(ResearchProtocol.Action.OPEN, pendingResearch.begin(), 0, -1, "");
    }
    private static boolean researchContextValid() {
        return researchCountry.equals(ClientCampaignHud.country()) && researchOrigin == ScreenDialogCoordinator.contentScreen();
    }
    public static void open() {
        if (pendingResearch.waiting() || ScreenDialogCoordinator.contentScreen() instanceof ResearchScreen) return;
        if (!ClientPlayNetworking.canSend(ResearchProtocol.Request.TYPE)) { message("이 서버는 HOI 연구 UI를 지원하지 않습니다."); return; }
        send(beginResearch());
    }
    public static void openResearchDetail(String id) {
        cancelOpen();
        if (ScreenDialogCoordinator.contentScreen() instanceof ResearchScreen screen) { screen.showDetail(id); return; }
        open();
        if (pendingResearch.waiting()) pendingResearch.target(id);
    }
    public static void cancelOpen() {
        pendingResearch.clear(); pendingMenu.clear(); pendingTab = null; menuOrigin = null; researchOrigin = null; researchCountry = "";
        menuCountry = ""; menuToken = ""; menuRetryTicks = 0;
    }
    static MenuProtocol.Refresh beginMenu(dev.hoi.protocol.MenuTab tab) {
        cancelOpen();
        pendingTab = tab; menuOrigin = ScreenDialogCoordinator.contentScreen(); menuCountry = ClientCampaignHud.country();
        menuToken = pendingMenu.begin();
        return new MenuProtocol.Refresh(menuToken);
    }
    private static boolean menuContextValid() {
        return ClientCampaignHud.visible() && menuCountry.equals(ClientCampaignHud.country()) && menuOrigin == ScreenDialogCoordinator.contentScreen();
    }
    static void receiveMenu(MenuProtocol.Update packet) {
        if (pendingMenu.matches(packet.screen())) {
            boolean valid = menuContextValid();
            var tab = pendingTab;
            String country = menuCountry, target = pendingMenu.target();
            cancelOpen();
            if (!valid) return;
            if (packet.json().isEmpty()) { Minecraft.getInstance().gui.setScreen(null); return; }
            try {
                var view = new MenuProtocol.OpenScreen(packet.json()).view();
                if (!country.equals(view.country())) return;
                var screen = target == null ? new ScreenMenu(view, tab, dev.hoi.client.HoiClient::open) : ScreenMenu.forFocus(view, target);
                Minecraft.getInstance().gui.setScreen(screen);
            } catch (IllegalArgumentException error) { message("HOI 메뉴 데이터를 읽을 수 없습니다."); }
            return;
        }
        if (!(ScreenDialogCoordinator.contentScreen() instanceof ScreenMenu menu) || menu.selectionPreview() || !menu.token().equals(packet.screen())) return;
        if (packet.json().isEmpty()) { cancelOpen(); Minecraft.getInstance().gui.setScreen(null); return; }
        try { menu.update(new MenuProtocol.OpenScreen(packet.json()).view()); }
        catch (RuntimeException error) { message("HOI 메뉴 갱신 데이터를 읽을 수 없습니다."); }
    }
    public static void openSourceFocusDetail(String id) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.FocusProtocol.Request.TYPE)) { message("이 서버는 중점 계통도 UI를 지원하지 않습니다."); return; }
        cancelOpen();Minecraft.getInstance().gui.setScreen(new dev.hoi.client.screen.ScreenFocus(id));
    }
    public static void openFocusDetail(String id) {
        openMenu(dev.hoi.protocol.MenuTab.POLITICS);
        if (pendingMenu.waiting()) pendingMenu.target(id);
    }
    public static void openCountry(String target) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.CountryProtocol.Request.TYPE)) { message("이 서버는 외국 정보 UI를 지원하지 않습니다."); return; }
        cancelOpen(); Minecraft.getInstance().gui.setScreen(new ScreenCountry(target));
        dev.hoi.client.audio.AudioUiSounds.play("ui.menu_tab");
    }
    public static void openWorldTension() {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.WorldTensionProtocol.Request.TYPE)) { message("이 서버는 세계 긴장도 이력 UI를 지원하지 않습니다."); return; }
        cancelOpen(); Minecraft.getInstance().gui.setScreen(new dev.hoi.client.screen.ScreenWorldTension());
    }
    public static void openConstruction() {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.ConstructionProtocol.Request.TYPE)) { message("이 서버는 건설 UI를 지원하지 않습니다."); return; }
        cancelOpen();var screen=new ScreenConstruction();Minecraft.getInstance().gui.setScreen(screen);screen.open();
    }
    public static void openIndustry(dev.hoi.protocol.MenuTab tab) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.IndustryProtocol.Request.TYPE)) { message("이 서버는 산업 UI를 지원하지 않습니다."); return; }
        cancelOpen(); var screen = new ScreenIndustry(tab); Minecraft.getInstance().gui.setScreen(screen); screen.open();
    }
    public static void openAgency() {
        if (!ClientPlayNetworking.canSend(AgencyProtocol.Request.TYPE)) { message("이 서버는 HOI 정보기관을 지원하지 않습니다."); return; }
        cancelOpen(); var screen = new ScreenAgency(null); Minecraft.getInstance().gui.setScreen(screen); screen.open();
    }
    public static void openMenu(dev.hoi.protocol.MenuTab tab) {
        if (!ClientCampaignHud.visible()) return;
        if (tab == dev.hoi.protocol.MenuTab.RESEARCH) { open(); return; }
        if (tab == dev.hoi.protocol.MenuTab.INTELLIGENCE) { openAgency(); return; }
        if (tab == dev.hoi.protocol.MenuTab.DECISIONS && ClientPlayNetworking.canSend(dev.hoi.protocol.DecisionProtocol.Request.TYPE)) {
            cancelOpen(); Minecraft.getInstance().gui.setScreen(new dev.hoi.client.screen.ScreenDecision()); return;
        }
        if (ScreenIndustry.supports(tab)) { openIndustry(tab); return; }
        if(tab==dev.hoi.protocol.MenuTab.CONSTRUCTION){openConstruction();return;}
        if (!ClientPlayNetworking.canSend(MenuProtocol.Refresh.TYPE)) { message("이 서버는 HOI 국가 메뉴를 지원하지 않습니다."); return; }
        if (pendingMenu.waiting() && pendingTab == tab && menuContextValid()) return;
        ClientPlayNetworking.send(beginMenu(tab));
    }
    public static void send(ResearchProtocol.Request request) {
        if (Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(ResearchProtocol.Request.TYPE)) ClientPlayNetworking.send(request);
    }
    private static void message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal(text));
    }
}
