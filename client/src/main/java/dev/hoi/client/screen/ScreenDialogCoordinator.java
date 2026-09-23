package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.AudioSuperEvent;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

public final class ScreenDialogCoordinator {
    private static final LinkedHashMap<String, DialogView> pending = new LinkedHashMap<>();
    private static ScreenDialogStack notifications;
    private static boolean suspending;
    public static boolean renderingContent() { return ScreenDialogStack.contentPass; }
    public static boolean suspending() { return suspending; }
    public static Screen contentScreen() {
        var current = Minecraft.getInstance().gui.screen();
        if (current instanceof ScreenDialogStack stack) current = stack.backdrop();
        if (current instanceof ScreenAgencyOperation operation) return operation.backdrop();
        return current instanceof ScreenCompletion completion && completion.backdrop() != null ? completion.backdrop() : current;
    }
    static boolean menuScreen(Screen screen) {
        return screen != null && screen.getClass().getPackageName().startsWith("dev.hoi.client")
                && !(screen instanceof ScreenDialog) && !(screen instanceof ScreenDialogStack);
    }
    private static boolean canOverlay(Screen screen) { return screen == null || screen instanceof ScreenDialog || screen instanceof ScreenDialogStack || menuScreen(screen); }
    private static boolean notification(DialogView view) {
        return view.completion() || switch (view.kind()) {
            case EVENT, GLOBAL_EVENT, SUPER_EVENT, DIPLOMACY -> true;
            default -> false;
        };
    }
    private ScreenDialogCoordinator() {}
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
            AudioSuperEvent.tick();
        });
    }
    public static void reset() {
        pending.clear(); notifications = null; suspending = false; HoiClient.cancelOpen(); AudioSuperEvent.reset();
        var client = Minecraft.getInstance();
        if (client.gui.screen() instanceof ScreenDialog || client.gui.screen() instanceof ScreenDialogStack) client.gui.setScreen(null);
    }
    private static ScreenDialog find(String token) {
        if (notifications != null && notifications.find(token) != null) return notifications.find(token);
        var current = contentScreen();
        return current instanceof ScreenDialog screen && screen.view().token().equals(token) ? screen : null;
    }
    static void receive(DialogProtocol.Show packet) {
        DialogView view;
        try { view = packet.view(); } catch (IllegalArgumentException error) { return; }
        var existing = find(view.token());
        if (existing != null) {
            if(view.revision()<existing.view().revision())return;
            if(view.kind()==DialogView.Kind.STATE&&(existing instanceof ScreenAirDetail||existing instanceof ScreenMapDetail)
                    &&(existing instanceof ScreenAirDetail)!=view.image().equals("air_detail/background"))show(view);
            else existing.update(view);
            return;
        }
        var old = pending.get(view.token());
        if (old != null && old.revision() > view.revision()) return;
        if (!packet.open()) { if (old != null) pending.put(view.token(), view); return; }
        if (pending.size() >= 64) return;
        pending.put(view.token(), view);
        if(view.kind()==DialogView.Kind.SUPER_EVENT)AudioSuperEvent.play(view.token(),view.sound());
        var client = Minecraft.getInstance();
        if (canOverlay(client.gui.screen()) || !notification(view) || view.completion() && client.level != null) show(view);
    }
    static void details(DialogProtocol.Details packet) {
        var window = find(packet.token());
        if (window == null || !window.view().completion()) return;
        close(packet.token());
        String id = packet.target().substring(packet.target().indexOf('/') + 1);
        if (packet.target().startsWith("research/")) HoiClient.openResearchDetail(id);
        else if (packet.target().startsWith("source-focus/")) dev.hoi.client.network.NetworkScreens.openSourceFocusDetail(id);
        else dev.hoi.client.network.NetworkScreens.openFocusDetail(id);
    }
    private static void show(DialogView view) {
        pending.remove(view.token());
        var client = Minecraft.getInstance();
        if (notification(view)) {
            if (notifications == null) notifications = new ScreenDialogStack(null);
            notifications.add(view);
            showNotifications();
            if (view.kind() == DialogView.Kind.SUPER_EVENT) AudioSuperEvent.play(view.token(), view.sound());
            else if (view.completion()) dev.hoi.client.audio.AudioUiSounds.play(
                    view.presentation().equals("research_complete") ? "ui.research.complete" : "ui.focus.complete");
        } else {
            HoiClient.cancelOpen();
            var parent = contentScreen();
            if (parent instanceof ScreenPoliticsChoice politics) parent = politics.backdrop();
            var screen = view.kind() == DialogView.Kind.POLITICS ? new ScreenPoliticsChoice(view, parent) : view.kind() == DialogView.Kind.STATE ? (view.image().equals("air_detail/background") ? new ScreenAirDetail(view) : new ScreenMapDetail(view)) : new ScreenDialog(view);
            if (client.gui.screen() == notifications && notifications != null) {
                notifications.backdrop(screen); screen.init(notifications.width, notifications.height);
            } else client.gui.setScreen(screen);
        }
    }
    private static void showNotifications() {
        var client = Minecraft.getInstance();
        if (notifications == null || client.gui.screen() == notifications) return;
        var parent = client.gui.screen();
        notifications.backdrop(menuScreen(parent) || parent instanceof ScreenDialog ? parent : null);
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
        if (notifications != null && notifications.backdrop() instanceof ScreenDialog screen && screen.view().token().equals(token)) {
            notifications.backdrop(screen instanceof ScreenPoliticsChoice politics ? politics.backdrop() : null);
            return;
        }
        if (client.gui.screen() instanceof ScreenDialog screen && screen.view().token().equals(token)) {
            if (screen instanceof ScreenCompletion completion) completion.dismiss();
            else if (screen instanceof ScreenPoliticsChoice politics) politics.dismiss();
            else client.gui.setScreen(null);
        }
    }
    static void choose(DialogView view, String choice) {
        if (!view.token().isEmpty() && ClientPlayNetworking.canSend(DialogProtocol.Action.TYPE))
            ClientPlayNetworking.send(new DialogProtocol.Action(view.token(), view.revision(), choice));
    }
}
