package dev.hoi.client.network;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.research.ResearchScreen;
import dev.hoi.client.screen.agency.AgencyScreen;
import dev.hoi.client.screen.industry.IndustryConstruction;
import dev.hoi.client.screen.diplomacy.DiplomacyCountryScreen;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.client.screen.industry.IndustryScreen;
import dev.hoi.protocol.research.ResearchPayloads;
import dev.hoi.protocol.menu.MenuPayloads;
import dev.hoi.protocol.agency.AgencyPayloads;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class NetworkScreens {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NetworkScreens.class);
    private static final NetworkPendingScreen pendingResearch = new NetworkPendingScreen();
    private static final NetworkPendingScreen pendingMenu = new NetworkPendingScreen();
    private static dev.hoi.protocol.menu.MenuTab pendingTab;
    private static net.minecraft.client.gui.screens.Screen menuOrigin, researchOrigin;
    private static String researchCountry = "";
    private static String menuCountry = "", menuToken = "";
    private static int menuRetryTicks;
    public static void register() {
        ResearchPayloads.registerPayloadTypes();
        dev.hoi.protocol.diplomacy.DiplomacyVolunteerPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.diplomacy.DiplomacyVolunteerPayloads.Response.TYPE, (packet, context) -> {
            try {
                var screen = DialogCoordinator.contentScreen();
                if (screen instanceof dev.hoi.client.screen.diplomacy.DiplomacyVolunteers editor) editor.update(packet);
                else if (screen instanceof DiplomacyCountryScreen country) country.update(packet);
            } catch (IllegalArgumentException error) {
                DialogCoordinator.setContent(null);
                LOGGER.warn("의용군 정보를 읽을 수 없습니다.", error);
            }
        });
        dev.hoi.protocol.diplomacy.DiplomacyPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.diplomacy.DiplomacyPayloads.Response.TYPE, (packet, context) -> {
            try {
                var screen = DialogCoordinator.contentScreen();
                if (screen instanceof dev.hoi.client.screen.diplomacy.DiplomacyLease editor) editor.update(packet);
                else if (screen instanceof DiplomacyCountryScreen country) country.update(packet);
            } catch (IllegalArgumentException error) {
                DialogCoordinator.setContent(null);
                LOGGER.warn("무기대여 정보를 읽을 수 없습니다.", error);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.focus.FocusPayloads.Response.TYPE, (packet, context) -> {
            if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.focus.FocusScreen screen) {
                try { screen.update(packet); } catch (IllegalArgumentException e) { DialogCoordinator.setContent(null); LOGGER.warn("중점 정보를 읽을 수 없습니다.", e); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.decision.DecisionPayloads.Response.TYPE, (packet, context) -> {
            if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.decision.DecisionScreen screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { DialogCoordinator.setContent(null); LOGGER.warn("결정 정보를 읽을 수 없습니다.", error); }
            } else if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.ScreenMenu menu) menu.decisionResponse(packet);
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.decision.DecisionDetailPayloads.Response.TYPE, (packet, context) -> {
            if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.decision.DecisionScreen screen) screen.update(packet);
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.decision.DecisionPayloads.Highlight.TYPE,(packet,context)->{
            if(DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.decision.DecisionScreen screen)screen.update(packet);
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.diplomacy.DiplomacyWorldTensionPayloads.Response.TYPE, (packet, context) -> {
            if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.diplomacy.DiplomacyWorldTension screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { DialogCoordinator.setContent(null); LOGGER.warn("세계 긴장도 정보를 읽을 수 없습니다.", error); }
            }
        });
        dev.hoi.protocol.industry.IndustryPayloads.registerPayloadTypes();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingMenu.waiting()) {
                if (!menuContextValid()) cancelOpen();
                else if (pendingMenu.tick()) cancelOpen();
                else if (++menuRetryTicks >= 20 && ClientPlayNetworking.canSend(MenuPayloads.Refresh.TYPE)) {
                    menuRetryTicks = 0;
                    ClientPlayNetworking.send(new MenuPayloads.Refresh(menuToken));
                }
            }
            if (pendingResearch.waiting() && !researchContextValid()) cancelOpen();
            pendingResearch.tick();
        });
        ClientPlayNetworking.registerGlobalReceiver(ResearchPayloads.OpenScreen.TYPE, (packet, context) -> open());
        ClientPlayNetworking.registerGlobalReceiver(MenuPayloads.SelectionPreview.TYPE, (packet, context) -> {
            if (packet.json().isEmpty()) {
                if (DialogCoordinator.contentScreen() instanceof ScreenMenu screen && screen.selectionPreview()) DialogCoordinator.setContent(null);
                return;
            }
            try {
                cancelOpen();
                DialogCoordinator.setContent(ScreenMenu.selection(packet.view()));
            } catch (IllegalArgumentException error) { LOGGER.warn("국가 선택 정보를 읽을 수 없습니다.", error); }
        });
        ClientPlayNetworking.registerGlobalReceiver(MenuPayloads.OpenScreen.TYPE, (packet, context) -> {
            try {
                var view = packet.view();
                cancelOpen();
                DialogCoordinator.setContent(new ScreenMenu(view));
            } catch (RuntimeException e) { LOGGER.warn("HOI 메뉴 데이터를 읽을 수 없습니다.", e); }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.country.CountryPayloads.OpenScreen.TYPE, (packet, context) -> openCountry(packet.target()));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.country.CountryPayloads.Response.TYPE, (packet, context) -> {
            if (DialogCoordinator.contentScreen() instanceof DiplomacyCountryScreen screen) {
                try { screen.update(packet); }
                catch (IllegalArgumentException error) { DialogCoordinator.setContent(null); LOGGER.warn("국가 정보를 읽을 수 없습니다.", error); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(MenuPayloads.Update.TYPE, (packet, context) -> receiveMenu(packet));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.industry.IndustryConstructionPayloads.Response.TYPE, (packet, context) -> {
            if(DialogCoordinator.contentScreen() instanceof IndustryConstruction screen) {
                try {screen.update(packet.view());} catch(IllegalArgumentException e){LOGGER.warn("건설 데이터를 읽을 수 없습니다.",e);}
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.industry.IndustryPayloads.Response.TYPE, (packet, context) -> {
            if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.army.ArmyHeadquarters headquarters) {try{headquarters.update(packet.view());}catch(IllegalArgumentException error){DialogCoordinator.setContent(null);LOGGER.warn("HQ 편제 정보를 읽을 수 없습니다.",error);}}
            else if(DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.commander.CommanderCaptivity captured){try{captured.update(packet.view());}catch(IllegalArgumentException error){captured.failed();}}
            else if (DialogCoordinator.contentScreen() instanceof IndustryScreen screen) {
                try { screen.update(packet.view()); }
                catch (IllegalArgumentException error) { DialogCoordinator.setContent(null); LOGGER.warn("산업 현황을 읽을 수 없습니다.", error); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.doctrine.DoctrinePayloads.Response.TYPE, (packet, context) -> {
            if (!(DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.doctrine.DoctrineScreen screen)) return;
            try { screen.update(packet.view()); }
            catch (IllegalArgumentException invalid) { DialogCoordinator.setContent(null); LOGGER.warn("교리 화면을 다시 열어주세요.", invalid); }
        });
        ClientPlayNetworking.registerGlobalReceiver(AgencyPayloads.Highlight.TYPE, (packet, context) -> dev.hoi.client.map.agency.AgencyHud.accept(packet));
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.raid.RaidPayloads.Highlight.TYPE, (packet, context) -> {
            if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.raid.RaidScreen raid) raid.accept(packet);
            else dev.hoi.client.map.agency.AgencyRaidHud.accept(packet);
        });
        ClientPlayNetworking.registerGlobalReceiver(dev.hoi.protocol.raid.RaidPayloads.Response.TYPE, (packet, context) -> {
            try {
                if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.raid.RaidScreen raid) raid.update(packet.view());
                else dev.hoi.client.map.agency.AgencyRaidHud.accept(packet.view());
            }
            catch (IllegalArgumentException invalid) {
                if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.raid.RaidScreen raid) raid.revoke();
                else dev.hoi.client.map.agency.AgencyRaidHud.reset();
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AgencyPayloads.Response.TYPE, (packet, context) -> {
            try {
                var view = packet.view();
                if (DialogCoordinator.contentScreen() instanceof AgencyScreen agency) agency.update(view);
                else dev.hoi.client.map.agency.AgencyHud.accept(view);
            }
            catch (IllegalArgumentException error) { LOGGER.warn("정보기관 데이터를 읽을 수 없습니다.", error); }
        });
        ClientPlayNetworking.registerGlobalReceiver(ResearchPayloads.Response.TYPE, (packet, context) -> receiveResearch(packet));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            cancelOpen();
            if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.raid.RaidScreen raid) raid.revoke();
            if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.diplomacy.DiplomacyLease editor) editor.revoke();
            if (DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.diplomacy.DiplomacyVolunteers editor) editor.revoke();
            if (client.gui.screen() instanceof dev.hoi.client.screen.diplomacy.DiplomacyWorldTension) client.gui.setScreen(null);
            if (client.gui.screen() instanceof dev.hoi.client.screen.focus.FocusScreen || client.gui.screen() instanceof dev.hoi.client.screen.decision.DecisionScreen || client.gui.screen() instanceof ResearchScreen || client.gui.screen() instanceof ScreenMenu || client.gui.screen() instanceof AgencyScreen || client.gui.screen() instanceof IndustryConstruction || client.gui.screen() instanceof DiplomacyCountryScreen || client.gui.screen() instanceof IndustryScreen || client.gui.screen() instanceof dev.hoi.client.screen.army.ArmyHeadquarters || client.gui.screen() instanceof dev.hoi.client.screen.commander.CommanderCaptivity) client.gui.setScreen(null);
        }));
    }
    static void receiveResearch(ResearchPayloads.Response packet) {
            try {
                var view = packet.view();
                var current = DialogCoordinator.contentScreen();
                if (view.country().isEmpty()) {
                    boolean active = current instanceof ResearchScreen screen && (view.session().isEmpty() || screen.session().equals(view.session()));
                    if (active || pendingResearch.matches(view.session())) {
                        cancelOpen();
                        if (active) DialogCoordinator.setContent(null);
                    }
                } else if (pendingResearch.matches(view.session())) {
                    if (!researchContextValid() || !researchCountry.isEmpty() && !researchCountry.equals(view.country())) {
                        cancelOpen();
                        send(new ResearchPayloads.Request(ResearchPayloads.Action.CLOSE, view.session(), 0, -1, ""));
                        return;
                    }
                    String target = pendingResearch.target();
                    cancelOpen();
                    var screen = new ResearchScreen(view);
                    DialogCoordinator.setContent(screen);
                    if (target != null && view.technology(target) != null) screen.showDetail(target);
                } else if (current instanceof ResearchScreen screen && screen.session().equals(view.session())) {
                    screen.update(view);
                } else {
                    send(new ResearchPayloads.Request(ResearchPayloads.Action.CLOSE, view.session(), 0, -1, ""));
                }
            } catch (IllegalArgumentException e) { cancelOpen(); LOGGER.warn("HOI 연구 화면 데이터를 읽을 수 없습니다.", e); }
    }
    static ResearchPayloads.Request beginResearch() {
        cancelOpen();
        researchOrigin = DialogCoordinator.contentScreen(); researchCountry = ClientCampaignHud.country();
        return new ResearchPayloads.Request(ResearchPayloads.Action.OPEN, pendingResearch.begin(), 0, -1, "");
    }
    private static boolean researchContextValid() {
        return researchCountry.equals(ClientCampaignHud.country()) && researchOrigin == DialogCoordinator.contentScreen();
    }
    public static void open() {
        if (pendingResearch.waiting() || DialogCoordinator.contentScreen() instanceof ResearchScreen) return;
        if (!ClientPlayNetworking.canSend(ResearchPayloads.Request.TYPE)) return;
        send(beginResearch());
    }
    public static void openResearchDetail(String id) {
        cancelOpen();
        if (DialogCoordinator.contentScreen() instanceof ResearchScreen screen) { screen.showDetail(id); return; }
        open();
        if (pendingResearch.waiting()) pendingResearch.target(id);
    }
    public static void cancelOpen() {
        pendingResearch.clear(); pendingMenu.clear(); pendingTab = null; menuOrigin = null; researchOrigin = null; researchCountry = "";
        menuCountry = ""; menuToken = ""; menuRetryTicks = 0;
    }
    static MenuPayloads.Refresh beginMenu(dev.hoi.protocol.menu.MenuTab tab) {
        cancelOpen();
        pendingTab = tab; menuOrigin = DialogCoordinator.contentScreen(); menuCountry = ClientCampaignHud.country();
        menuToken = pendingMenu.begin();
        return new MenuPayloads.Refresh(menuToken);
    }
    private static boolean menuContextValid() {
        return ClientCampaignHud.visible() && menuCountry.equals(ClientCampaignHud.country()) && menuOrigin == DialogCoordinator.contentScreen();
    }
    static void receiveMenu(MenuPayloads.Update packet) {
        if (pendingMenu.matches(packet.screen())) {
            boolean valid = menuContextValid();
            var tab = pendingTab;
            String country = menuCountry, target = pendingMenu.target();
            cancelOpen();
            if (!valid) return;
            if (packet.json().isEmpty()) { DialogCoordinator.setContent(null); return; }
            try {
                var view = new MenuPayloads.OpenScreen(packet.json()).view();
                if (!country.equals(view.country())) return;
                var screen = target == null ? new ScreenMenu(view, tab, dev.hoi.client.HoiClient::open) : ScreenMenu.forFocus(view, target);
                DialogCoordinator.setContent(screen);
            } catch (IllegalArgumentException error) { LOGGER.warn("HOI 메뉴 데이터를 읽을 수 없습니다.", error); }
            return;
        }
        if (!(DialogCoordinator.contentScreen() instanceof ScreenMenu menu) || menu.selectionPreview() || !menu.token().equals(packet.screen())) return;
        if (packet.json().isEmpty()) { cancelOpen(); DialogCoordinator.setContent(null); return; }
        try { menu.update(new MenuPayloads.OpenScreen(packet.json()).view()); }
        catch (RuntimeException error) { LOGGER.warn("HOI 메뉴 갱신 데이터를 읽을 수 없습니다.", error); }
    }
    public static void openSourceFocusDetail(String id) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.focus.FocusPayloads.Request.TYPE)) return;
        cancelOpen();DialogCoordinator.setContent(new dev.hoi.client.screen.focus.FocusScreen(id));
    }
    public static void openForeignFocus(net.minecraft.client.gui.screens.Screen parent, String target) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.focus.FocusPayloads.Request.TYPE)) return;
        cancelOpen(); DialogCoordinator.setContent(new dev.hoi.client.screen.focus.FocusScreen(parent, target));
    }
    public static void openFocusDetail(String id) {
        openMenu(dev.hoi.protocol.menu.MenuTab.POLITICS);
        if (pendingMenu.waiting()) pendingMenu.target(id);
    }
    public static void openCountry(String target) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.country.CountryPayloads.Request.TYPE)) return;
        cancelOpen(); DialogCoordinator.setContent(new DiplomacyCountryScreen(target));
        dev.hoi.client.audio.AudioUiSounds.play("ui.menu_tab");
    }
    public static void openLeaseAnswer(String target, String action) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.country.CountryPayloads.Request.TYPE)) return;
        cancelOpen(); DialogCoordinator.setContent(new DiplomacyCountryScreen(target, action));
    }
    public static void openForeignLogistics(String target) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.industry.IndustryPayloads.Request.TYPE)) return;
        cancelOpen(); var screen = new IndustryScreen(target); DialogCoordinator.setContent(screen); screen.open();
    }
    public static void openWorldTension() {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.diplomacy.DiplomacyWorldTensionPayloads.Request.TYPE)) return;
        cancelOpen(); DialogCoordinator.setContent(new dev.hoi.client.screen.diplomacy.DiplomacyWorldTension());
    }
    public static void openConstruction() {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.industry.IndustryConstructionPayloads.Request.TYPE)) return;
        cancelOpen();var screen=new IndustryConstruction();DialogCoordinator.setContent(screen);screen.open();
    }
    public static void openIndustry(dev.hoi.protocol.menu.MenuTab tab) {
        if (!ClientPlayNetworking.canSend(dev.hoi.protocol.industry.IndustryPayloads.Request.TYPE)) return;
        cancelOpen(); var screen = new IndustryScreen(tab); DialogCoordinator.setContent(screen); screen.open();
    }
    public static void openAgency() {
        dev.hoi.client.map.agency.AgencyHud.reset();
        if (!ClientPlayNetworking.canSend(AgencyPayloads.Request.TYPE)) return;
        cancelOpen(); var screen = new AgencyScreen(null); DialogCoordinator.setContent(screen); screen.open();
    }
    public static void openMenu(dev.hoi.protocol.menu.MenuTab tab) {
        if (!ClientCampaignHud.visible()) return;
        if (tab == dev.hoi.protocol.menu.MenuTab.RESEARCH) { open(); return; }
        if (tab == dev.hoi.protocol.menu.MenuTab.INTELLIGENCE) { openAgency(); return; }
        if (tab == dev.hoi.protocol.menu.MenuTab.DECISIONS && ClientPlayNetworking.canSend(dev.hoi.protocol.decision.DecisionPayloads.Request.TYPE)) {
            cancelOpen(); DialogCoordinator.setContent(new dev.hoi.client.screen.decision.DecisionScreen()); return;
        }
        if (IndustryScreen.supports(tab)) { openIndustry(tab); return; }
        if(tab==dev.hoi.protocol.menu.MenuTab.CONSTRUCTION){openConstruction();return;}
        if (!ClientPlayNetworking.canSend(MenuPayloads.Refresh.TYPE)) return;
        if (pendingMenu.waiting() && pendingTab == tab && menuContextValid()) return;
        ClientPlayNetworking.send(beginMenu(tab));
    }
    public static void send(ResearchPayloads.Request request) {
        if (Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(ResearchPayloads.Request.TYPE)) ClientPlayNetworking.send(request);
    }
}
