package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.SuperEventAudio;
import dev.hoi.client.research.ResearchScreen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;

public final class DialogClient {
    private static DialogView pending;
    private static boolean suspending;
    public static boolean suspending() { return suspending; }
    public static net.minecraft.client.gui.screens.Screen contentScreen() {
        var current = Minecraft.getInstance().gui.screen();
        return current instanceof CompletionScreen completion && completion.backdrop() != null ? completion.backdrop() : current;
    }
    static boolean menuScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof ResearchScreen || screen instanceof HoiMenuScreen || screen instanceof IndustryScreen
                || screen instanceof ConstructionScreen || screen instanceof CountryScreen || screen instanceof AgencyScreen;
    }
    private DialogClient() {}
    public static void register() {
        DialogProtocol.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(DialogProtocol.Show.TYPE, (p, c) -> receive(p));
        ClientPlayNetworking.registerGlobalReceiver(DialogProtocol.Close.TYPE, (p, c) -> close(p.token()));
        ClientPlayNetworking.registerGlobalReceiver(DialogProtocol.Details.TYPE, (p, c) -> details(p));
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
        if (client.gui.screen() instanceof DialogScreen screen && screen.view().token().equals(view.token())) {
            if (view.revision() >= screen.view().revision()) screen.update(view);
            return;
        }
        if (!packet.open()) { if (pending != null && pending.token().equals(view.token())) pending = view; return; }
        pending = view;
        if (client.gui.screen() == null || client.gui.screen() instanceof DialogScreen
                || client.gui.screen() instanceof ResearchScreen || client.gui.screen() instanceof HoiMenuScreen
                || client.gui.screen() instanceof IndustryScreen || client.gui.screen() instanceof ConstructionScreen
                || client.gui.screen() instanceof CountryScreen || client.gui.screen() instanceof AgencyScreen
                || view.kind() == DialogView.Kind.STATE || view.kind() == DialogView.Kind.IDEOLOGIES
                || view.completion() && client.level != null) show(view);
    }
    static void details(DialogProtocol.Details packet) {
        var client = Minecraft.getInstance();
        if (!(client.gui.screen() instanceof CompletionScreen screen) || !screen.view().token().equals(packet.token())) return;
        String id = packet.target().substring(packet.target().indexOf('/') + 1);
        if (packet.target().startsWith("research/")) HoiClient.openResearchDetail(id);
        else client.gui.setScreen(HoiMenuScreen.forFocus(id));
    }
    private static void show(DialogView view) {
        pending = null; HoiClient.cancelOpen();
        var client = Minecraft.getInstance();
        var parent = contentScreen();
        suspending = view.completion() && menuScreen(parent);
        try {
            client.gui.setScreen(view.completion() ? new CompletionScreen(view, suspending ? parent : null)
                    : view.kind() == DialogView.Kind.POLITICS ? new PoliticsChoiceScreen(view, parent) : new DialogScreen(view));
        } finally { suspending = false; }
        if (view.kind() == DialogView.Kind.SUPER_EVENT) SuperEventAudio.play(view.token(), view.sound());
    }
    static void close(String token) {
        if (pending != null && pending.token().equals(token)) pending = null;
        var client = Minecraft.getInstance();
        if (client.gui.screen() instanceof DialogScreen screen && screen.view().token().equals(token)) {
            if (screen instanceof CompletionScreen completion) completion.dismiss();
            else if (screen instanceof PoliticsChoiceScreen politics) politics.dismiss();
            else client.gui.setScreen(null);
        }
    }
    static void choose(DialogView view, String choice) {
        if (!view.token().isEmpty() && ClientPlayNetworking.canSend(DialogProtocol.Action.TYPE))
            ClientPlayNetworking.send(new DialogProtocol.Action(view.token(), view.revision(), choice));
    }
}
