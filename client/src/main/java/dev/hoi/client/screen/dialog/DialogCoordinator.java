package dev.hoi.client.screen.dialog;

import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.AudioSuperEvent;
import dev.hoi.client.screen.agency.*;
import dev.hoi.client.screen.army.ArmyDivisionTemplates;
import dev.hoi.client.screen.commander.*;
import dev.hoi.client.screen.decision.DecisionPreferences;
import dev.hoi.client.screen.industry.IndustryOrganization;
import dev.hoi.client.screen.map.MapAirDetail;
import dev.hoi.client.screen.map.MapDetail;
import dev.hoi.client.screen.politics.PoliticsChoice;
import dev.hoi.protocol.dialog.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

public final class DialogCoordinator {
    private static final LinkedHashMap<String, DialogView> pending = new LinkedHashMap<>();
    private static DialogStack notifications;
    private static boolean suspending;
    public static DialogStack eventStack() { return notifications; }
    static void minimized(DialogStack stack) {
        var client = Minecraft.getInstance();
        if (stack == notifications && !stack.hasVisibleWindows() && client.gui.screen() == stack) {
            suspending = true;
            try { client.gui.setScreen(stack.detachBackdrop()); } finally { suspending = false; }
        }
    }
    static void reopened(DialogStack stack) { if (stack == notifications) showNotifications(); }
    public static boolean renderingContent() { return DialogStack.contentPass; }
    public static boolean suspending() { return suspending; }
    public static void setContent(Screen screen) {
        var client=Minecraft.getInstance();
        if(notifications==null||client.gui.screen()!=notifications) {client.gui.setScreen(screen);return;}
        var previous=notifications.backdrop();
        if(previous==screen)return;
        if(previous!=null)previous.removed();
        notifications.backdrop(screen);
        if(screen!=null)screen.init(notifications.width,notifications.height);
    }
    public static Screen contentScreen() {
        var current = Minecraft.getInstance().gui.screen();
        if (current instanceof DialogStack stack) current = stack.backdrop();
        if (current instanceof AgencyOperation operation) return operation.backdrop();
        if (current instanceof AgencyCandidates candidates) return candidates.backdrop();
        if (current instanceof CommanderConfirmation confirmation && confirmation.backdrop() instanceof AgencyScreen agency) return agency;
        return current instanceof DialogCompletion completion && completion.backdrop() != null ? completion.backdrop() : current;
    }
    static boolean menuScreen(Screen screen) {
        return screen != null && screen.getClass().getPackageName().startsWith("dev.hoi.client")
                && !(screen instanceof DialogScreen) && !(screen instanceof DialogStack);
    }
    private static boolean canOverlay(Screen screen) { return screen == null || screen instanceof DialogScreen || screen instanceof DialogStack || menuScreen(screen); }
    private static boolean notification(DialogView view) {
        return view.completion() || switch (view.kind()) {
            case EVENT, GLOBAL_EVENT, SUPER_EVENT, DIPLOMACY -> true;
            default -> false;
        };
    }
    private DialogCoordinator() {}
    public static void register() {
        DialogPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(DialogPayloads.Show.TYPE, (p, c) -> receive(p));
        ClientPlayNetworking.registerGlobalReceiver(DialogPayloads.Close.TYPE, (p, c) -> close(p.token()));
        ClientPlayNetworking.registerGlobalReceiver(DialogPayloads.Details.TYPE, (p, c) -> details(p));
        dev.hoi.client.map.supply.SupplyHubHover.register();
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> c.execute(() -> {reset(); dev.hoi.client.map.supply.SupplyHubHover.reset();}));
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
        if (client.gui.screen() instanceof DialogScreen || client.gui.screen() instanceof DialogStack) client.gui.setScreen(null);
    }
    private static DialogScreen find(String token) {
        if (notifications != null && notifications.find(token) != null) return notifications.find(token);
        var current = contentScreen();
        return current instanceof DialogScreen screen && screen.view().token().equals(token) ? screen : null;
    }
    public static void receive(DialogPayloads.Show packet) {
        DialogView view;
        try { view = packet.view(); } catch (IllegalArgumentException error) { return; }
        var existing = find(view.token());
        if (existing != null) {
            if(view.revision()<existing.view().revision())return;
            if(CommanderOfficerRoster.matches(view)!=(existing instanceof CommanderOfficerRoster)||ArmyDivisionTemplates.matches(view)!=(existing instanceof ArmyDivisionTemplates)||CommanderConfirmation.matches(view)!=(existing instanceof CommanderConfirmation)){if(view.revision()>existing.view().revision())show(view);}
            else if(view.kind()==DialogView.Kind.STATE&&(existing instanceof MapAirDetail||existing instanceof MapDetail)
                    &&(existing instanceof MapAirDetail)!=view.image().equals("air_detail/background"))show(view);
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
    static void details(DialogPayloads.Details packet) {
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
            if (notifications == null) notifications = new DialogStack(null);
            notifications.add(view);
            if(!DecisionPreferences.current().opens(view))notifications.minimize(view.token());
            showNotifications();
            if (view.kind() == DialogView.Kind.SUPER_EVENT) AudioSuperEvent.play(view.token(), view.sound());
            else if (view.completion()) dev.hoi.client.audio.AudioUiSounds.play(
                    view.presentation().equals("research_complete") ? "ui.research.complete" : view.presentation().equals("focus_complete") ? "ui.focus.complete" : "");
        } else {
            HoiClient.cancelOpen();
            var originalParent = contentScreen();
            var parent = originalParent;
            if (parent instanceof CommanderConfirmation confirmation) parent=confirmation.backdrop();
            if (parent instanceof CommanderOfficerRoster officers) parent=officers.backdrop();
            if (parent instanceof CommanderPreferredTactics tactics) parent=tactics.backdrop();
            if (parent instanceof PoliticsChoice politics) parent = politics.backdrop();
            if (parent instanceof CommanderRoster commander) parent = commander.backdrop();
            if (parent instanceof IndustryOrganization organization) parent = organization.backdrop();
            if (parent instanceof CommanderTraits traits) parent = traits.backdrop();
            var screen = CommanderOfficerRoster.matches(view) ? new CommanderOfficerRoster(view,originalParent instanceof CommanderRoster?originalParent:parent) : ArmyDivisionTemplates.matches(view) ? new ArmyDivisionTemplates(view) : view.image().startsWith("organization/") ? new IndustryOrganization(view,parent) : CommanderConfirmation.matches(view) ? new CommanderConfirmation(view) : view.image().equals("commander/tactic-confirm") ? new DialogScreen(view) : view.image().endsWith("preferred_tactic_selection") ? new CommanderPreferredTactics(view,parent) : view.kind() == DialogView.Kind.COMMANDER ? ((view.image().startsWith("commander/trait-")||view.image().equals("commander/advisor-roles"))?new CommanderTraits(view,parent):new CommanderRoster(view,parent)) : view.kind() == DialogView.Kind.POLITICS ? new PoliticsChoice(view, parent) : view.kind() == DialogView.Kind.STATE ? (view.image().equals("air_detail/background") ? new MapAirDetail(view) : new MapDetail(view)) : new DialogScreen(view);
            if(screen instanceof CommanderConfirmation confirmation)confirmation.backdrop(originalParent instanceof CommanderOfficerRoster officers?officers.backdrop():originalParent);
            if (client.gui.screen() == notifications && notifications != null) {
                notifications.backdrop(screen); screen.init(notifications.width, notifications.height);
            } else client.gui.setScreen(screen);
        }
    }
    private static void showNotifications() {
        var client = Minecraft.getInstance();
        if (notifications == null || !notifications.hasVisibleWindows() || client.gui.screen() == notifications) return;
        var parent = client.gui.screen();
        notifications.backdrop(menuScreen(parent) || parent instanceof DialogScreen ? parent : null);
        suspending = true;
        try { client.gui.setScreen(notifications); } finally { suspending = false; }
    }
    public static void close(String token) {
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
            notifications.backdrop(screen instanceof IndustryOrganization organization ? organization.backdrop() : screen instanceof CommanderPreferredTactics tactics ? tactics.backdrop() : screen instanceof PoliticsChoice politics ? politics.backdrop() : screen instanceof CommanderRoster commander ? commander.backdrop() : screen instanceof CommanderTraits traits ? traits.backdrop() : null);
            return;
        }
        if (client.gui.screen() instanceof DialogScreen screen && screen.view().token().equals(token)) {
            if (screen instanceof CommanderOfficerRoster officers) officers.dismiss();
            else if (screen instanceof IndustryOrganization organization) organization.dismiss();
            else if (screen instanceof DialogCompletion completion) completion.dismiss();
            else if (screen instanceof CommanderPreferredTactics tactics) tactics.dismiss();
            else if (screen instanceof PoliticsChoice politics) politics.dismiss();
            else if (screen instanceof CommanderRoster commander) commander.dismiss();
            else if (screen instanceof CommanderTraits traits) traits.dismiss();
            else client.gui.setScreen(null);
        }
    }
    public static void choose(DialogView view, String choice) {
        boolean leaseAnswer = view.presentation().equals("lend_lease_request") && choice.startsWith("answer_lease:");
        if (leaseAnswer) {
            var current = find(view.token());
            if (current == null || !current.view().equals(view) || view.choices().stream().noneMatch(c -> c.id().equals(choice))) return;
        }
        boolean agencyDetails = view.presentation().equals("agency_upgrade_complete") && choice.equals("details");
        if (agencyDetails) {
            var current = find(view.token());
            if (current == null || !current.view().equals(view) || view.choices().stream().noneMatch(c -> c.id().equals(choice))) return;
        }
        if (!view.token().isEmpty() && ClientPlayNetworking.canSend(DialogPayloads.Action.TYPE)) {
            ClientPlayNetworking.send(new DialogPayloads.Action(view.token(), view.revision(), choice));
            if (leaseAnswer) {
                String[] flag = view.flag().split("/");
                close(view.token());
                if (flag.length == 3 && flag[0].equals("country") && flag[2].equals("flag"))
                    dev.hoi.client.network.NetworkScreens.openLeaseAnswer(flag[1].toUpperCase(Locale.ROOT), choice);
            }
            if (agencyDetails) {
                close(view.token());
                dev.hoi.client.network.NetworkScreens.openAgency();
            }
        }
    }
}
