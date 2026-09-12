package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.SuperEventAudio;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

public final class DialogClient {
    private static final LinkedHashMap<String, DialogView> pending = new LinkedHashMap<>();
    private static DialogStackScreen notifications;
    private static boolean suspending;
    public static boolean renderingContent() { return DialogStackScreen.contentPass; }
    public static boolean suspending() { return suspending; }
    public static Screen contentScreen() {
        var current = Minecraft.getInstance().gui.screen();
        if (current instanceof DialogStackScreen stack) current = stack.backdrop();
        if (current instanceof AgencyOperationScreen operation) return operation.backdrop();
        return current instanceof CompletionScreen completion && completion.backdrop() != null ? completion.backdrop() : current;
    }
    static boolean menuScreen(Screen screen) {
        return screen != null && screen.getClass().getPackageName().startsWith("dev.hoi.client")
                && !(screen instanceof DialogScreen) && !(screen instanceof DialogStackScreen);
    }
    private static boolean canOverlay(Screen screen) { return screen == null || screen instanceof DialogScreen || screen instanceof DialogStackScreen || menuScreen(screen); }
    private static boolean notification(DialogView view) {
        return view.completion() || switch (view.kind()) {
            case EVENT, GLOBAL_EVENT, SUPER_EVENT, DIPLOMACY -> true;
            default -> false;
        };
    }
    private DialogClient() {}
    public static void register() {
        DialogProtocol.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(DialogProtocol.Show.TYPE, (p, c) -> receive(p));
        ClientPlayNetworking.registerGlobalReceiver(DialogProtocol.Close.TYPE, (p, c) -> close(p.token()));
        ClientPlayNetworking.registerGlobalReceiver(DialogProtocol.Details.TYPE, (p, c) -> details(p));
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (canOverlay(c.gui.screen())) {
                for (var view : List.copyOf(pending.values())) show(view);
                showNotifications();
            }
            SuperEventAudio.tick();
        });
    }
    public static void reset() {
        pending.clear(); notifications = null; suspending = false; HoiClient.cancelOpen(); SuperEventAudio.reset();
        var client = Minecraft.getInstance();
        if (client.gui.screen() instanceof DialogScreen || client.gui.screen() instanceof DialogStackScreen) client.gui.setScreen(null);
    }
    private static DialogScreen find(String token) {
        if (notifications != null && notifications.find(token) != null) return notifications.find(token);
        var current = contentScreen();
        return current instanceof DialogScreen screen && screen.view().token().equals(token) ? screen : null;
    }
    static void receive(DialogProtocol.Show packet) {
        DialogView view;
        try { view = packet.view(); } catch (IllegalArgumentException error) { return; }
        var existing = find(view.token());
        if (existing != null) { existing.update(view); return; }
        var old = pending.get(view.token());
        if (old != null && old.revision() > view.revision()) return;
        if (!packet.open()) { if (old != null) pending.put(view.token(), view); return; }
        if (pending.size() >= 64) return;
        pending.put(view.token(), view);
        var client = Minecraft.getInstance();
        if (canOverlay(client.gui.screen()) || !notification(view) || view.completion() && client.level != null) show(view);
    }
    static void details(DialogProtocol.Details packet) {
        var window = find(packet.token());
        if (window == null || !window.view().completion()) return;
        close(packet.token());
        String id = packet.target().substring(packet.target().indexOf('/') + 1);
        if (packet.target().startsWith("research/")) HoiClient.openResearchDetail(id);
        else Minecraft.getInstance().gui.setScreen(HoiMenuScreen.forFocus(id));
    }
    private static void show(DialogView view) {
        pending.remove(view.token());
        var client = Minecraft.getInstance();
        if (notification(view)) {
            if (notifications == null) notifications = new DialogStackScreen(null);
            notifications.add(view);
            showNotifications();
            if (view.kind() == DialogView.Kind.SUPER_EVENT) SuperEventAudio.play(view.token(), view.sound());
        } else {
            HoiClient.cancelOpen();
            var parent = contentScreen();
            if (parent instanceof PoliticsChoiceScreen politics) parent = politics.backdrop();
            var screen = view.kind() == DialogView.Kind.POLITICS ? new PoliticsChoiceScreen(view, parent) : new DialogScreen(view);
            if (client.gui.screen() == notifications && notifications != null) {
                notifications.backdrop(screen); screen.init(notifications.width, notifications.height);
            } else client.gui.setScreen(screen);
        }
    }
    private static void showNotifications() {
        var client = Minecraft.getInstance();
        if (notifications == null || client.gui.screen() == notifications) return;
        var parent = client.gui.screen();
        notifications.backdrop(menuScreen(parent) || parent instanceof DialogScreen ? parent : null);
        suspending = true;
        try { client.gui.setScreen(notifications); } finally { suspending = false; }
    }
    static void close(String token) {
        pending.remove(token);
        var client = Minecraft.getInstance();
        if (notifications != null && notifications.find(token) != null) {
            var stack = notifications;
            stack.close(token);
            if (stack.views().isEmpty()) {
                notifications = null;
                if (client.gui.screen() == stack) client.gui.setScreen(stack.detachBackdrop());
            }
            return;
        }
        if (notifications != null && notifications.backdrop() instanceof DialogScreen screen && screen.view().token().equals(token)) {
            notifications.backdrop(screen instanceof PoliticsChoiceScreen politics ? politics.backdrop() : null);
            return;
        }
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
