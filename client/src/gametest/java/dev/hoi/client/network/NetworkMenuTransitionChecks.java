package dev.hoi.client.network;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.PauseScreen;

public final class NetworkMenuTransitionChecks {
    public static void run(ClientGameTestContext context, ProtocolMenuView view) {
        String json = ProtocolMenuPayloads.OpenScreen.of(view).json();
        String[] token = new String[1];
        context.setScreen(() -> null);
        context.runOnClient(client -> {
            ClientCampaignHud.accept(ProtocolHudPayloads.State.of(view.country(), view.hud()));
            check(dev.hoi.client.ui.UiMenuBar.snapshot(ProtocolCountryHud.UNKNOWN).equals(view.hud()), "All empty menu snapshots retain the authoritative HUD");
            check(dev.hoi.client.ui.UiMenuBar.snapshot(null).equals(view.hud()), "Loading panels retain the authoritative HUD");
            token[0] = NetworkScreens.beginMenu(ProtocolMenuTab.LOGISTICS).screen();
            check(client.gui.screen() == null, "Waiting for menu data must keep the existing HUD without a dummy screen");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-menu-pending-keeps-hud");
        context.runOnClient(client -> {
            String old = token[0];
            token[0] = NetworkScreens.beginMenu(ProtocolMenuTab.POLITICS).screen();
            NetworkScreens.receiveMenu(new ProtocolMenuPayloads.Update(old, json));
            check(client.gui.screen() == null, "Superseded reply cannot open a screen");
            NetworkScreens.receiveMenu(new ProtocolMenuPayloads.Update(token[0], json));
            check(client.gui.screen() instanceof ScreenMenu screen && screen.selectedTab() == ProtocolMenuTab.POLITICS,
                    "Ready snapshot opens the requested panel");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-menu-ready-without-zero-frame");
        context.runOnClient(client -> {
            var origin = client.gui.screen();
            var request = NetworkScreens.beginMenu(ProtocolMenuTab.TRADE);
            NetworkScreens.receiveMenu(new ProtocolMenuPayloads.Update(request.screen(), ""));
            check(client.gui.screen() == null, "Revoked access closes the previous private screen");
            client.gui.setScreen(origin);
            request = NetworkScreens.beginMenu(ProtocolMenuTab.TRADE);
            NetworkScreens.cancelOpen();
            NetworkScreens.receiveMenu(new ProtocolMenuPayloads.Update(request.screen(), json));
            check(client.gui.screen() == origin, "Canceled reply cannot reopen a screen");
            request = NetworkScreens.beginMenu(ProtocolMenuTab.TRADE);
            ClientCampaignHud.accept(ProtocolHudPayloads.State.of("PRC", view.hud()));
            NetworkScreens.receiveMenu(new ProtocolMenuPayloads.Update(request.screen(), json));
            check(client.gui.screen() == origin, "Country change rejects old private data");
            ClientCampaignHud.accept(ProtocolHudPayloads.State.of(view.country(), view.hud()));
            request = NetworkScreens.beginMenu(ProtocolMenuTab.TRADE);
            client.gui.setScreen(new PauseScreen(true));
            var pause = client.gui.screen();
            NetworkScreens.receiveMenu(new ProtocolMenuPayloads.Update(request.screen(), json));
            check(client.gui.screen() == pause, "Late response cannot replace a different screen");
            client.gui.setScreen(null);
            request = NetworkScreens.beginMenu(ProtocolMenuTab.POLITICS);
            ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);
            check(dev.hoi.client.ui.UiMenuBar.snapshot(ProtocolCountryHud.UNKNOWN).equals(ProtocolCountryHud.UNKNOWN), "Stopping cannot leak cached country statistics");
            NetworkScreens.receiveMenu(new ProtocolMenuPayloads.Update(request.screen(), json));
            check(client.gui.screen() == null, "Stopped campaign cannot reopen a menu");
            ClientCampaignHud.accept(ProtocolHudPayloads.State.of(view.country(), view.hud()));
        });
        context.setScreen(() -> null);
        context.runOnClient(client -> {
            var request = NetworkScreens.beginResearch();
            var pause = new PauseScreen(true); client.gui.setScreen(pause);
            NetworkScreens.receiveResearch(researchResponse(request.session(),view.country()));
            check(client.gui.screen() == pause, "Delayed research response cannot replace the user's new screen");
            client.gui.setScreen(null);
            request = NetworkScreens.beginResearch();
            ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);
            NetworkScreens.receiveResearch(researchResponse(request.session(),view.country()));
            check(client.gui.screen() == null, "Research reply cannot reopen a stopped country");
            ClientCampaignHud.accept(ProtocolHudPayloads.State.of(view.country(),view.hud()));
            String old = NetworkScreens.beginResearch().session();
            request = NetworkScreens.beginResearch();
            NetworkScreens.receiveResearch(researchResponse(old,view.country()));
            check(client.gui.screen() == null, "Superseded research reply cannot open a screen");
            NetworkScreens.receiveResearch(researchResponse(request.session(),view.country()));
            check(client.gui.screen() instanceof dev.hoi.client.research.ResearchScreen, "Current research response opens normally");
            NetworkScreens.cancelOpen();
        });
        context.setScreen(() -> null);
    }
    private static ProtocolResearchPayloads.Response researchResponse(String token,String country) {
        return ProtocolResearchPayloads.Response.of(new ProtocolResearchView(token,1,country,country,0,"2020년 1월 1일","일시 정지",java.util.List.of(new ProtocolResearchView.Slot(0,"",0)),java.util.List.of(),""));
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
