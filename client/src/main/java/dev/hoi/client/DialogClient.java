package dev.hoi.client;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;

final class DialogClient {
    private static DialogView pending;
    private DialogClient() {}
    static void register() {
        DialogProtocol.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(DialogProtocol.Show.TYPE, (p, c) -> receive(p));
        ClientPlayNetworking.registerGlobalReceiver(DialogProtocol.Close.TYPE, (p, c) -> close(p.token()));
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {
            pending = null; SuperEventAudio.reset();
            if (c.gui.screen() instanceof DialogScreen) c.gui.setScreen(null);
        });
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (pending != null && c.gui.screen() == null) show(pending);
            SuperEventAudio.tick();
        });
    }
    static void receive(DialogProtocol.Show packet) {
        var view = packet.view(); var client = Minecraft.getInstance();
        if (client.gui.screen() instanceof DialogScreen screen && screen.view().token().equals(view.token())) { screen.update(view); return; }
        if (!packet.open()) { if (pending != null && pending.token().equals(view.token())) pending = view; return; }
        pending = view;
        if (client.gui.screen() == null || client.gui.screen() instanceof DialogScreen
                || client.gui.screen() instanceof ResearchScreen || client.gui.screen() instanceof HoiMenuScreen
                || client.gui.screen() instanceof IndustryScreen || client.gui.screen() instanceof ConstructionScreen
                || client.gui.screen() instanceof CountryScreen || client.gui.screen() instanceof AgencyScreen
                || view.kind() == DialogView.Kind.STATE || view.kind() == DialogView.Kind.IDEOLOGIES) show(view);
    }
    private static void show(DialogView view) {
        pending = null; HoiClient.cancelOpen(); Minecraft.getInstance().gui.setScreen(view.completion() ? new CompletionScreen(view) : new DialogScreen(view));
        if (view.kind() == DialogView.Kind.SUPER_EVENT) SuperEventAudio.play(view.token(), view.sound());
    }
    static void close(String token) {
        if (pending != null && pending.token().equals(token)) pending = null;
        var client = Minecraft.getInstance();
        if (client.gui.screen() instanceof DialogScreen screen && screen.view().token().equals(token)) client.gui.setScreen(null);
    }
    static void choose(DialogView view, String choice) {
        if (!view.token().isEmpty() && ClientPlayNetworking.canSend(DialogProtocol.Action.TYPE))
            ClientPlayNetworking.send(new DialogProtocol.Action(view.token(), view.revision(), choice));
    }
}
