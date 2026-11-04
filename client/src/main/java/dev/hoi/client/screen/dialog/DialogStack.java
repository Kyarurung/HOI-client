package dev.hoi.client.screen.dialog;

import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.client.screen.decision.DecisionScreen;
import dev.hoi.client.screen.doctrine.DoctrineNotification;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class DialogStack extends Screen {
    static boolean contentPass;
    private final List<DialogScreen> windows = new ArrayList<>();
    private final Set<String> minimized = new HashSet<>();
    private String foreground = "";
    private Screen backdrop;
    private Screen pressedTarget;
    private final java.util.function.BiConsumer<DialogView,String> send;
    public DialogStack(Screen backdrop) {this(backdrop,DialogCoordinator::choose);}
    public DialogStack(Screen backdrop,java.util.function.BiConsumer<DialogView,String> send) { super(Component.literal("알림")); this.backdrop = backdrop;this.send=send; }
    public Screen backdrop() { return backdrop; }
    void backdrop(Screen screen) { backdrop = screen; }
    Screen detachBackdrop() { var parent = backdrop; backdrop = null; return parent; }
    public List<DialogView> views() { return windows.stream().map(DialogScreen::view).toList(); }
    public List<DialogView> events() { return windows.stream().filter(DialogScreen::minimizable).map(DialogScreen::view).toList(); }
    public boolean hasVisibleWindows() { return windows.stream().anyMatch(w -> !minimized.contains(w.view().token())); }
    void minimize(String token) {
        var window = find(token);
        if (window == null || !window.minimizable()) return;
        minimized.add(token);
        if (foreground.equals(token)) foreground = "";
        DialogCoordinator.minimized(this);
    }
    public void reopen(String token) {
        var window = find(token);
        if (window == null || !window.minimizable()) return;
        minimized.remove(token); windows.remove(window); windows.add(window); foreground = token;
        DialogCoordinator.reopened(this);
    }
    public DialogScreen find(String token) { return windows.stream().filter(w -> w.view().token().equals(token)).findFirst().orElse(null); }
    public void add(DialogView view) {
        var old = find(view.token());
        if (old != null) { old.update(view); return; }
        if (windows.size() >= 64) return;
        var window = DoctrineNotification.matches(view) ? new DoctrineNotification(view,send) : view.completion() ? new DialogCompletion(view,send) : view.leaderEvent() ? new DialogLeaderEvent(view,send) : new DialogScreen(view,send);
        window.stacked = true;
        window.minimize = () -> minimize(view.token());
        windows.add(window);
        if (width > 0) init();
    }
    void close(String token) {
        minimized.remove(token);
        if (foreground.equals(token)) foreground = "";
        if (windows.removeIf(w -> w.view().token().equals(token)) && width > 0) init();
    }
    @Override protected void init() {
        clearWidgets();
        if (dev.hoi.client.ClientCampaignHud.visible()) for (var button : dev.hoi.client.ui.UiMenuBar.buttons(width, "", null, dev.hoi.client.HoiClient::openMenu)) addRenderableWidget(button);
        if (backdrop != null && (backdrop.width != width || backdrop.height != height)) backdrop.resize(width, height);
        for (int i = 0; i < windows.size(); i++) {
            var window = windows.get(i);
            window.init(width, height);
        }
    }
    @Override public void tick() { if (backdrop != null) backdrop.tick(); }
    @Override public void removed() {
        if (backdrop != null && !DialogCoordinator.suspending()) { backdrop.removed(); backdrop = null; }
    }
    public List<Screen> layers() {
        DialogScreen event = null, superEvent = null, completion = null, doctrineReward = null;
        for (var window : windows) {
            if (minimized.contains(window.view().token())) continue;
            if (window instanceof DoctrineNotification doctrine && doctrine.reward()) doctrineReward = window;
            else if (window.view().completion()) completion = window;
            else if (window.view().kind() == DialogView.Kind.SUPER_EVENT) superEvent = window;
            else event = window;
        }
        var layers = new ArrayList<Screen>(4);
        if (event != null) layers.add(event);
        if (superEvent != null) layers.add(superEvent);
        if (backdrop != null) layers.add(backdrop);
        if (event != null && event.view().token().equals(foreground)) { layers.remove(event); layers.add(event); }
        if (doctrineReward != null) layers.add(doctrineReward);
        if (completion != null) layers.add(completion);
        return layers;
    }
    public Screen inputAt(double x, double y) {
        for (var screen : layers().reversed()) {
            if (screen == backdrop) {
                if (y < dev.hoi.client.ui.UiMenuBar.height(width)) continue;
                if (!(screen instanceof dev.hoi.client.input.InputSidebarMovement.InputMovementScreen sidebar) || !sidebar.allowsMovement()
                        || x < sidebar.unitHudLeft()) return screen;
            } else if (screen instanceof DialogScreen window && x >= window.panelLeft() && x < window.panelLeft() + window.panelWidth()
                    && y >= window.panelTop() && y < window.panelTop() + window.panelHeight()) return window;
        }
        return backdrop;
    }
    private Screen keyboardTarget() {
        var layers = layers();
        if (!layers.isEmpty() && layers.getLast() instanceof DecisionScreen)
            for (var window : windows.reversed()) if (window.minimizable() && !minimized.contains(window.view().token())) return window;
        return layers.isEmpty() ? null : layers.getLast();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        var target = inputAt(mx, my);
        dev.hoi.client.ClientCampaignHud.draw(g);
        g.nextStratum();
        contentPass = dev.hoi.client.ClientCampaignHud.visible();
        try {
            for (var screen : layers()) {
                if (screen == backdrop) net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.beforeExtract(screen).invoker().beforeExtract(screen,g,screen == target ? mx : -1,screen == target ? my : -1,delta);
                screen.extractRenderState(g, screen == target ? mx : -1, screen == target ? my : -1, delta);
                if (screen == backdrop) net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterExtract(screen).invoker().afterExtract(screen,g,screen == target ? mx : -1,screen == target ? my : -1,delta);
                g.nextStratum();
            }
        } finally { contentPass = false; }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        pressedTarget = inputAt(event.x(), event.y());
        boolean consumed = pressedTarget == null ? super.mouseClicked(event, twice) : pressedTarget.mouseClicked(event, twice);
        if (!consumed && (pressedTarget == null || pressedTarget == backdrop) && backdrop instanceof dev.hoi.client.input.InputSidebarMovement.InputMovementScreen sidebar
                && sidebar.allowsMovement() && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT
                && event.x() >= sidebar.unitHudLeft() && event.y() >= dev.hoi.client.ui.UiMenuBar.height(width)) {
            return dev.hoi.client.input.InputMapInfo.clickAt(minecraft, event.x(), event.y(), width, height);
        }
        return consumed;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        var target = pressedTarget; pressedTarget = null;
        return target != null && target.mouseReleased(event);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return pressedTarget != null && pressedTarget.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseScrolled(double x, double y, double h, double v) { var target = inputAt(x, y); return target != null && target.mouseScrolled(x, y, h, v); }
    @Override public boolean keyPressed(KeyEvent event) { var target = keyboardTarget(); return target != null && target.keyPressed(event); }
    @Override public boolean keyReleased(KeyEvent event) { var target = keyboardTarget(); return target != null && target.keyReleased(event); }
    @Override public boolean charTyped(CharacterEvent event) { var target = keyboardTarget(); return target != null && target.charTyped(event); }
    @Override public void onClose() { var target = keyboardTarget(); if (target != null) target.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
