package dev.hoi.client.map.commander;

import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import dev.hoi.protocol.commander.CommanderBattlePayloads;
import dev.hoi.protocol.commander.CommanderBattlePayloads.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public final class CommanderBattleHud {
    public static Consumer<Request> requestObserver;
    private static final List<CommanderBattleWindow> windows = new ArrayList<>();
    private CommanderBattleHud() {}
    static void register() {
        CommanderBattlePayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE, (packet, context) -> accept(packet));
        HudElementRegistry.attachElementAfter(Identifier.parse("hoi:combat_log"), Identifier.parse("hoi:battle_windows"), (g, delta) -> {
            if (Minecraft.getInstance().gui.screen() == null) draw(g, g.guiWidth() / 2, g.guiHeight() / 2);
        });
    }
    public static void send(Request request) {
        if (requestObserver != null) requestObserver.accept(request);
        else if (ClientPlayNetworking.canSend(Request.TYPE)) ClientPlayNetworking.send(request);
    }
    public static Optional<CommanderBattleWindow> window(Kind kind) { return windows.stream().filter(window -> window.kind() == kind && !(window instanceof CommanderBattleTactics)).findFirst(); }
    public static Optional<CommanderBattleTactics> tactics() { return windows.stream().filter(CommanderBattleTactics.class::isInstance).map(CommanderBattleTactics.class::cast).findFirst(); }
    public static void open(Kind kind, String id) {
        var old = window(kind).orElse(null);
        if (old != null && old.id().equals(id)) { front(old); return; }
        var next = switch (kind) {
            case LAND -> new CommanderLandBattleWindow(id);
            case NAVAL -> new CommanderNavalBattleWindow(id);
            case RESULT -> new CommanderNavalResultWindow(id);
        };
        if (old != null) { next.place(old); close(old); }
        windows.add(next);
        layout();
        next.open();
    }
    static void tactics(CommanderLandBattleWindow battle) {
        var old = tactics().orElse(null);
        if (old != null) { close(old); return; }
        windows.add(new CommanderBattleTactics(battle));
        layout();
    }
    private static void front(CommanderBattleWindow window) { windows.remove(window); windows.add(window); }
    static void close(CommanderBattleWindow window) {
        if (!windows.remove(window)) return;
        if (window instanceof CommanderLandBattleWindow) windows.removeIf(CommanderBattleTactics.class::isInstance);
        window.close();
    }
    public static void accept(Snapshot snapshot) {
        for (var window : List.copyOf(windows)) if (window.kind() == snapshot.kind() && !window.accept(snapshot)) close(window);
    }
    private static void layout() {
        var client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth(), height = client.getWindow().getGuiScaledHeight();
        for (var window : windows) window.layout(width, height, CommanderLayout.units(width, height).scale() * .5f);
    }
    private static boolean visible() {
        var client = Minecraft.getInstance();
        return !windows.isEmpty() && client.level != null && !client.gui.hud.isHidden() && !AgencyHud.active() && (client.gui.screen() == null || client.gui.screen() instanceof MapCursor);
    }
    private static CommanderBattleWindow at(double x, double y) {
        for (int i = windows.size() - 1; i >= 0; i--) if (windows.get(i).contains(x, y)) return windows.get(i);
        return null;
    }
    public static boolean covers(double x, double y) { return visible() && at(x, y) != null; }
    public static void draw(GuiGraphicsExtractor g, int mx, int my) {
        if (!visible()) return;
        render(g);
        var window = at(mx, my);
        String tooltip = window == null ? "" : window.tooltip(mx, my);
        if (!tooltip.isEmpty()) CommanderHud.drawTooltip(g, mx, my, TooltipDelay.visible(tooltip));
    }
    public static void drawBackdrop(GuiGraphicsExtractor g) { if (!windows.isEmpty()) render(g); }
    private static void render(GuiGraphicsExtractor g) {
        layout();
        for (var window : windows) window.draw(g, Minecraft.getInstance().font);
    }
    public static boolean click(MouseButtonEvent event) {
        var window = visible() ? at(event.x(), event.y()) : null;
        if (window == null) return false;
        front(window);
        return window.mouseClicked(event);
    }
    public static boolean drag(MouseButtonEvent event) {
        if (!visible() || windows.stream().noneMatch(window -> window.mouseDragged(event))) return false;
        layout();
        return true;
    }
    public static boolean release() {
        boolean held = false;
        for (var window : windows) held |= window.mouseReleased();
        return held && visible();
    }
    public static boolean scroll(double x, double y, double vertical) {
        var window = visible() ? at(x, y) : null;
        return window != null && window.mouseScrolled(x, y, vertical);
    }
    static boolean closeWindow() {
        if (!visible()) return false;
        close(windows.getLast());
        return true;
    }
    public static void reset() { for (var window : List.copyOf(windows)) close(window); }
}
