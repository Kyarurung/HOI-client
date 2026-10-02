package dev.hoi.client.network;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.PauseScreen;
import java.util.concurrent.atomic.AtomicReference;

public final class NetworkDisconnectTest {
    public static void run(ClientGameTestContext context, ProtocolMenuView view) {
        var client = context.computeOnClient(c -> c);
        var handler = context.computeOnClient(c -> c.getConnection());
        var originalHud = context.computeOnClient(c -> ClientCampaignHud.visible() ? ProtocolHudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()) : ProtocolHudPayloads.State.HIDDEN);
        try {
            {
                var removedOnClient = new AtomicReference<Boolean>();
                String token = context.computeOnClient(c -> {
                    ClientCampaignHud.accept(ProtocolHudPayloads.State.of(view.country(), view.hud()));
                    c.gui.setScreen(new ScreenMenu(view));
                    String pending = NetworkScreens.beginMenu(ProtocolMenuTab.TRADE).screen();
                    ScreenEvents.remove(c.gui.screen()).register(screen -> removedOnClient.set(c.isSameThread()));
                    return pending;
                });
                if (client.isSameThread()) throw new AssertionError("Disconnect regression must originate outside the client thread");
                ClientPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(handler, client);
                context.waitTicks(1);
                context.runOnClient(c -> {
                    if (!Boolean.TRUE.equals(removedOnClient.get())) throw new AssertionError("Private screen removal must run on the client thread");
                    if (c.gui.screen() != null || ClientCampaignHud.visible()) throw new AssertionError("Disconnect retained private screen or HUD");
                    ClientCampaignHud.accept(ProtocolHudPayloads.State.of(view.country(), view.hud()));
                    NetworkScreens.receiveMenu(new ProtocolMenuPayloads.Update(token, ProtocolMenuPayloads.OpenScreen.of(view).json()));
                    if (c.gui.screen() != null) throw new AssertionError("Disconnected session response reopened a private screen");
                });
            }
            context.runOnClient(c -> {
                var pause = new PauseScreen(true);
                c.gui.setScreen(pause);
                ClientPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(handler, c);
                if (c.gui.screen() != pause) throw new AssertionError("HOI disconnect cleanup replaced a vanilla screen");
            });
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(c -> ClientCampaignHud.accept(originalHud));
        }
    }
}
