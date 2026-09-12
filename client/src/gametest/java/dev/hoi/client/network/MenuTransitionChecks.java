package dev.hoi.client.network;

import dev.hoi.client.CampaignHud;
import dev.hoi.client.screen.HoiMenuScreen;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.PauseScreen;

public final class MenuTransitionChecks {
    public static void run(ClientGameTestContext context, MenuView view) {
        String json = MenuProtocol.OpenScreen.of(view).json();
        String[] token = new String[1];
        context.setScreen(() -> null);
        context.runOnClient(client -> {
            CampaignHud.accept(HudProtocol.State.of(view.country(), view.hud()));
            check(dev.hoi.client.ui.HoiMenuBar.snapshot(CountryHud.UNKNOWN).equals(view.hud()), "All empty menu snapshots retain the authoritative HUD");
            check(dev.hoi.client.ui.HoiMenuBar.snapshot(null).equals(view.hud()), "Loading panels retain the authoritative HUD");
            token[0] = ScreenNetworking.beginMenu(MenuTab.LOGISTICS).screen();
            check(client.gui.screen() == null, "Waiting for menu data must keep the existing HUD without a dummy screen");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-menu-pending-keeps-hud");
        context.runOnClient(client -> {
            String old = token[0];
            token[0] = ScreenNetworking.beginMenu(MenuTab.POLITICS).screen();
            ScreenNetworking.receiveMenu(new MenuProtocol.Update(old, json));
            check(client.gui.screen() == null, "Superseded reply cannot open a screen");
            ScreenNetworking.receiveMenu(new MenuProtocol.Update(token[0], json));
            check(client.gui.screen() instanceof HoiMenuScreen screen && screen.selectedTab() == MenuTab.POLITICS,
                    "Ready snapshot opens the requested panel");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-menu-ready-without-zero-frame");
        context.runOnClient(client -> {
            var origin = client.gui.screen();
            var request = ScreenNetworking.beginMenu(MenuTab.TRADE);
            ScreenNetworking.receiveMenu(new MenuProtocol.Update(request.screen(), ""));
            check(client.gui.screen() == null, "Revoked access closes the previous private screen");
            client.gui.setScreen(origin);
            request = ScreenNetworking.beginMenu(MenuTab.TRADE);
            ScreenNetworking.cancelOpen();
            ScreenNetworking.receiveMenu(new MenuProtocol.Update(request.screen(), json));
            check(client.gui.screen() == origin, "Canceled reply cannot reopen a screen");
            request = ScreenNetworking.beginMenu(MenuTab.TRADE);
            CampaignHud.accept(HudProtocol.State.of("PRC", view.hud()));
            ScreenNetworking.receiveMenu(new MenuProtocol.Update(request.screen(), json));
            check(client.gui.screen() == origin, "Country change rejects old private data");
            CampaignHud.accept(HudProtocol.State.of(view.country(), view.hud()));
            request = ScreenNetworking.beginMenu(MenuTab.TRADE);
            client.gui.setScreen(new PauseScreen(true));
            var pause = client.gui.screen();
            ScreenNetworking.receiveMenu(new MenuProtocol.Update(request.screen(), json));
            check(client.gui.screen() == pause, "Late response cannot replace a different screen");
            client.gui.setScreen(null);
            request = ScreenNetworking.beginMenu(MenuTab.POLITICS);
            CampaignHud.accept(HudProtocol.State.HIDDEN);
            check(dev.hoi.client.ui.HoiMenuBar.snapshot(CountryHud.UNKNOWN).equals(CountryHud.UNKNOWN), "Stopping cannot leak cached country statistics");
            ScreenNetworking.receiveMenu(new MenuProtocol.Update(request.screen(), json));
            check(client.gui.screen() == null, "Stopped campaign cannot reopen a menu");
            CampaignHud.accept(HudProtocol.State.of(view.country(), view.hud()));
        });
        context.setScreen(() -> null);
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
