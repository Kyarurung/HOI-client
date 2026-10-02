package dev.hoi.client.network;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.client.screen.ScreenAgency;
import dev.hoi.client.screen.ScreenConstruction;
import dev.hoi.client.screen.ScreenCountry;
import dev.hoi.client.screen.ScreenDialogCoordinator;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.client.screen.ScreenIndustry;

import dev.hoi.protocol.ProtocolResearchPayloads;
import dev.hoi.protocol.ProtocolMenuPayloads;
import dev.hoi.protocol.ProtocolAgencyPayloads;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class NetworkScreens {
    private static final NetworkPendingScreen pendingResearch = new NetworkPendingScreen();
    private static final NetworkPendingScreen pendingMenu = new NetworkPendingScreen();
    private static dev.hoi.protocol.ProtocolMenuTab pendingTab;
    private static net.minecraft.client.gui.screens.Screen menuOrigin, researchOrigin;
    private static String researchCountry = "";
    private static String menuCountry = "", menuToken = "";
    private static int menuRetryTicks;
    public static void register() {
        ProtocolResearchPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolFocusPayloads.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.ScreenFocus screen) {
                try { screen.update(packet); } catch (IllegalArgumentException e) { ScreenDialogCoordinator.setContent(null); message("중점 정보를 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolDecisionPayloads.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.ScreenDecision screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { ScreenDialogCoordinator.setContent(null); message("결정 정보를 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolWorldTension.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.ScreenWorldTension screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { ScreenDialogCoordinator.setContent(null); message("세계 긴장도 정보를 읽을 수 없습니다."); }
            }
        });
        dev.hoi.protocol.ProtocolIndustryPayloads.registerPayloadTypes();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingMenu.waiting()) {
                if (!menuContextValid()) cancelOpen();
                else if (pendingMenu.tick()) { cancelOpen(); message("국가 메뉴 응답이 없습니다. 다시 열어주세요."); }
                else if (++menuRetryTicks >= 20 && ClientPlayNetworking.canSend(ProtocolMenuPayloads.Refresh.TYPE)) {
                    menuRetryTicks = 0;
                    ClientPlayNetworking.send(new ProtocolMenuPayloads.Refresh(menuToken));
                }
            }
            if (pendingResearch.waiting() && !researchContextValid()) cancelOpen();
            if (pendingResearch.tick()) message("연구 화면 응답이 없습니다. 다시 열어주세요.");
        });
        ClientPlayNetworking.registerGlobalReceiver(ProtocolResearchPayloads.OpenScreen.TYPE, (packet, context) -> open());
        ClientPlayNetworking.registerGlobalReceiver(ProtocolMenuPayloads.SelectionPreview.TYPE, (packet, context) -> {
            if (packet.json().isEmpty()) {
                if (ScreenDialogCoordinator.contentScreen() instanceof ScreenMenu screen && screen.selectionPreview()) ScreenDialogCoordinator.setContent(null);
                return;
            }
            try {
                cancelOpen();
                ScreenDialogCoordinator.setContent(ScreenMenu.selection(packet.view()));
            } catch (IllegalArgumentException error) { message("국가 선택 정보를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(ProtocolMenuPayloads.OpenScreen.TYPE, (packet, context) -> {
            try {
                var view = packet.view();
                cancelOpen();
                ScreenDialogCoordinator.setContent(new ScreenMenu(view));
            } catch (RuntimeException e) { message("HOI 메뉴 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolCountryPayloads.OpenScreen.TYPE, (packet, context) -> openCountry(packet.target()));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolCountryPayloads.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof ScreenCountry screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { ScreenDialogCoordinator.setContent(null); message("국가 정보를 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(ProtocolMenuPayloads.Update.TYPE, (packet, context) -> receiveMenu(packet));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolConstructionPayloads.Response.TYPE, (packet, context) -> {
            if(ScreenDialogCoordinator.contentScreen() instanceof ScreenConstruction screen) {
                try {screen.update(packet.view());} catch(IllegalArgumentException e){message("건설 데이터를 읽을 수 없습니다.");}
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolIndustryPayloads.Response.TYPE, (packet, context) -> {
            if (ScreenDialogCoordinator.contentScreen() instanceof ScreenIndustry screen) {
                try { screen.update(packet.view()); }
                catch (IllegalArgumentException error) { ScreenDialogCoordinator.setContent(null); message("산업 현황을 읽을 수 없습니다."); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.ProtocolDoctrinePayloads.Response.TYPE, (packet, context) -> {
            if (!(ScreenDialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.ScreenDoctrine screen)) return;
            try { screen.update(packet.view()); }
            catch (IllegalArgumentException invalid) { ScreenDialogCoordinator.setContent(null); message("교리 화면을 다시 열어주세요."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(ProtocolAgencyPayloads.Response.TYPE, (packet, context) -> {
            if (!(ScreenDialogCoordinator.contentScreen() instanceof ScreenAgency agency)) return;
            try { agency.update(packet.view()); }
            catch (IllegalArgumentException error) { message("정보기관 데이터를 읽을 수 없습니다."); }
        });
        ClientPlayNetworking.registerGlobalReceiver(ProtocolResearchPayloads.Response.TYPE, (packet, context) -> receiveResearch(packet));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            cancelOpen();
            if (client.gui.screen() instanceof dev.hoi.client.screen.ScreenWorldTension) client.gui.setScreen(null);
            if (client.gui.screen() instanceof dev.hoi.client.screen.ScreenFocus || client.gui.screen() instanceof dev.hoi.client.screen.ScreenDecision || client.gui.screen() instanceof ResearchScreen || client.gui.screen() instanceof ScreenMenu || client.gui.screen() instanceof ScreenAgency || client.gui.screen() instanceof ScreenConstruction || client.gui.screen() instanceof ScreenCountry || client.gui.screen() instanceof ScreenIndustry) client.gui.setScreen(null);
        }));
    }
    static void receiveResearch(ProtocolResearchPayloads.Response packet) {
            try {
                var view = packet.view();
                var current = ScreenDialogCoordinator.contentScreen();
                if (view.country().isEmpty()) {
                    boolean active = current instanceof ResearchScreen screen && (view.session().isEmpty() || screen.session().equals(view.session()));
                    if (active || pendingResearch.matches(view.session())) {
                        cancelOpen();
                        if (active) ScreenDialogCoordinator.setContent(null);
                        message(view.message());
                    }
                } else if (pendingResearch.matches(view.session())) {
                    if (!researchContextValid() || !researchCountry.isEmpty() && !researchCountry.equals(view.country())) {
                        cancelOpen();
                        send(new ProtocolResearchPayloads.Request(ProtocolResearchPayloads.Action.CLOSE, view.session(), 0, -1, ""));
                        return;
                    }
                    String target = pendingResearch.target();
                    cancelOpen();
                    var screen = new ResearchScreen(view);
                    ScreenDialogCoordinator.setContent(screen);
                    if (target != null && view.technology(target) != null) screen.showDetail(target);
                } else if (current instanceof ResearchScreen screen && screen.session().equals(view.session())) {
                    screen.update(view);
                } else {
                    send(new ProtocolResearchPayloads.Request(ProtocolResearchPayloads.Action.CLOSE, view.session(), 0, -1, ""));
                }
            } catch (IllegalArgumentException e) { cancelOpen(); message("HOI 연구 화면 데이터를 읽을 수 없습니다."); }
    }
    static ProtocolResearchPayloads.Request beginResearch() {
        cancelOpen();
        researchOrigin = ScreenDialogCoordinator.contentScreen(); researchCountry = ClientCampaignHud.country();
        return new ProtocolResearchPayloads.Request(ProtocolResearchPayloads.Action.OPEN, pendingResearch.begin(), 0, -1, "");
    }
    private static boolean researchContextValid() {
        return researchCountry.equals(ClientCampaignHud.country()) && researchOrigin == ScreenDialogCoordinator.contentScreen();
    }
    public static void open() {
        if (pendingResearch.waiting() || ScreenDialogCoordinator.contentScreen() instanceof ResearchScreen) return;
        if (!ClientPlayNetworking.canSend(ProtocolResearchPayloads.Request.TYPE)) { message("이 서버는 HOI 연구 UI를 지원하지 않습니다."); return; }
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
    static ProtocolMenuPayloads.Refresh beginMenu(dev.hoi.protocol.ProtocolMenuTab tab) {
        cancelOpen();
        pendingTab = tab; menuOrigin = ScreenDialogCoordinator.contentScreen(); menuCountry = ClientCampaignHud.country();
        menuToken = pendingMenu.begin();
        return new ProtocolMenuPayloads.Refresh(menuToken);
    }
    private static boolean menuContextValid() {
        return ClientCampaignHud.visible() && menuCountry.equals(ClientCampaignHud.country()) && menuOrigin == ScreenDialogCoordinator.contentScreen();
    }
    static void receiveMenu(ProtocolMenuPayloads.Update packet) {
        if (pendingMenu.matches(packet.screen())) {
            boolean valid = menuContextValid();
            var tab = pendingTab;
            String country = menuCountry, target = pendingMenu.target();
            cancelOpen();
            if (!valid) return;
            if (packet.json().isEmpty()) { ScreenDialogCoordinator.setContent(null); return; }
            try {
                var view = new ProtocolMenuPayloads.OpenScreen(packet.json()).view();
                if (!country.equals(view.country())) return;
                var screen = target == null ? new ScreenMenu(view, tab, dev.hoi.client.HoiClient::open) : ScreenMenu.forFocus(view, target);
                ScreenDialogCoordinator.setContent(screen);
            } catch (IllegalArgumentException error) { message("HOI 메뉴 데이터를 읽을 수 없습니다."); }
            return;
        }
        if (!(ScreenDialogCoordinator.contentScreen() instanceof ScreenMenu menu) || menu.selectionPreview() || !menu.token().equals(packet.screen())) return;
        if (packet.json().isEmpty()) { cancelOpen(); ScreenDialogCoordinator.setContent(null); return; }
        try { menu.update(new ProtocolMenuPayloads.OpenScreen(packet.json()).view()); }
        catch (RuntimeException error) { message("HOI 메뉴 갱신 데이터를 읽을 수 없습니다."); }
    }
    public static void openSourceFocusDetail(String id) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolFocusPayloads.Request.TYPE)) { message("이 서버는 중점 계통도 UI를 지원하지 않습니다."); return; }
        cancelOpen();ScreenDialogCoordinator.setContent(new dev.hoi.client.screen.ScreenFocus(id));
    }
    public static void openFocusDetail(String id) {
        openMenu(dev.hoi.protocol.ProtocolMenuTab.POLITICS);
        if (pendingMenu.waiting()) pendingMenu.target(id);
    }
    public static void openCountry(String target) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolCountryPayloads.Request.TYPE)) { message("이 서버는 외국 정보 UI를 지원하지 않습니다."); return; }
        cancelOpen(); ScreenDialogCoordinator.setContent(new ScreenCountry(target));
        dev.hoi.client.audio.AudioUiSounds.play("ui.menu_tab");
    }
    public static void openWorldTension() {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolWorldTension.Request.TYPE)) { message("이 서버는 세계 긴장도 이력 UI를 지원하지 않습니다."); return; }
        cancelOpen(); ScreenDialogCoordinator.setContent(new dev.hoi.client.screen.ScreenWorldTension());
    }
    public static void openConstruction() {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolConstructionPayloads.Request.TYPE)) { message("이 서버는 건설 UI를 지원하지 않습니다."); return; }
        cancelOpen();var screen=new ScreenConstruction();ScreenDialogCoordinator.setContent(screen);screen.open();
    }
    public static void openIndustry(dev.hoi.protocol.ProtocolMenuTab tab) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolIndustryPayloads.Request.TYPE)) { message("이 서버는 산업 UI를 지원하지 않습니다."); return; }
        cancelOpen(); var screen = new ScreenIndustry(tab); ScreenDialogCoordinator.setContent(screen); screen.open();
    }
    public static void openAgency() {
        if (!ClientPlayNetworking.canSend(ProtocolAgencyPayloads.Request.TYPE)) { message("이 서버는 HOI 정보기관을 지원하지 않습니다."); return; }
        cancelOpen(); var screen = new ScreenAgency(null); ScreenDialogCoordinator.setContent(screen); screen.open();
    }
    public static void openMenu(dev.hoi.protocol.ProtocolMenuTab tab) {
        if (!ClientCampaignHud.visible()) return;
        if (tab == dev.hoi.protocol.ProtocolMenuTab.RESEARCH) { open(); return; }
        if (tab == dev.hoi.protocol.ProtocolMenuTab.INTELLIGENCE) { openAgency(); return; }
        if (tab == dev.hoi.protocol.ProtocolMenuTab.DECISIONS && ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolDecisionPayloads.Request.TYPE)) {
            cancelOpen(); ScreenDialogCoordinator.setContent(new dev.hoi.client.screen.ScreenDecision()); return;
        }
        if (ScreenIndustry.supports(tab)) { openIndustry(tab); return; }
        if(tab==dev.hoi.protocol.ProtocolMenuTab.CONSTRUCTION){openConstruction();return;}
        if (!ClientPlayNetworking.canSend(ProtocolMenuPayloads.Refresh.TYPE)) { message("이 서버는 HOI 국가 메뉴를 지원하지 않습니다."); return; }
        if (pendingMenu.waiting() && pendingTab == tab && menuContextValid()) return;
        ClientPlayNetworking.send(beginMenu(tab));
    }
    public static void send(ProtocolResearchPayloads.Request request) {
        if (Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(ProtocolResearchPayloads.Request.TYPE)) ClientPlayNetworking.send(request);
    }
    private static void message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal(text));
    }
}
