package dev.hoi.client.screen;

import dev.hoi.protocol.ProtocolDialogView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

final class ScreenDialogStack extends Screen {
    static boolean contentPass;
    private final List<ScreenDialog> windows = new ArrayList<>();
    private Screen backdrop;
    private Screen pressedTarget;
    private final java.util.function.BiConsumer<ProtocolDialogView,String> send;

    ScreenDialogStack(Screen backdrop) {this(backdrop,ScreenDialogCoordinator::choose);}
    ScreenDialogStack(Screen backdrop,java.util.function.BiConsumer<ProtocolDialogView,String> send) { super(Component.literal("알림")); this.backdrop = backdrop;this.send=send; }
    Screen backdrop() { return backdrop; }
    void backdrop(Screen screen) { backdrop = screen; }
    Screen detachBackdrop() { var parent = backdrop; backdrop = null; return parent; }
    List<ProtocolDialogView> views() { return windows.stream().map(ScreenDialog::view).toList(); }
    ScreenDialog find(String token) { return windows.stream().filter(w -> w.view().token().equals(token)).findFirst().orElse(null); }
    void add(ProtocolDialogView view) {
        var old = find(view.token());
        if (old != null) { old.update(view); return; }
        if (windows.size() >= 64) return;
        var window = view.completion() ? new ScreenCompletion(view,send) : new ScreenDialog(view);
        window.stacked = true;
        windows.add(window);
        if (width > 0) init();
    }
    void close(String token) {
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
        if (backdrop != null && !ScreenDialogCoordinator.suspending()) { backdrop.removed(); backdrop = null; }
    }
    List<Screen> layers() {
        ScreenDialog event = null, superEvent = null, completion = null;
        for (var window : windows) {
            if (window.view().completion()) completion = window;
            else if (window.view().kind() == ProtocolDialogView.Kind.SUPER_EVENT) superEvent = window;
            else event = window;
        }
        var layers = new ArrayList<Screen>(4);
        if (event != null) layers.add(event);
        if (superEvent != null) layers.add(superEvent);
        if (backdrop != null) layers.add(backdrop);
        if (completion != null) layers.add(completion);
        return layers;
    }
    Screen inputAt(double x, double y) {
        for (var screen : layers().reversed()) {
            if (screen == backdrop) {
                if (y < dev.hoi.client.ui.UiMenuBar.height(width)) continue;
                if (!(screen instanceof dev.hoi.client.input.InputSidebarMovement.InputMovementScreen sidebar) || !sidebar.allowsMovement()
                        || x < sidebar.unitHudLeft()) return screen;
            } else if (screen instanceof ScreenDialog window && x >= window.panelLeft() && x < window.panelLeft() + window.panelWidth()
                    && y >= window.panelTop() && y < window.panelTop() + window.panelHeight()) return window;
        }
        return backdrop != null && y < dev.hoi.client.ui.UiMenuBar.height(width) ? backdrop : null;
    }
    private Screen keyboardTarget() { var layers = layers(); return layers.isEmpty() ? null : layers.getLast(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        var target = inputAt(mx, my);
        dev.hoi.client.ClientCampaignHud.draw(g);
        g.nextStratum();
        contentPass = dev.hoi.client.ClientCampaignHud.visible();
        try {
            for (var screen : layers()) {
                screen.extractRenderState(g, screen == target ? mx : -1, screen == target ? my : -1, delta);
                g.nextStratum();
            }
        } finally { contentPass = false; }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        pressedTarget = inputAt(event.x(), event.y());
        return pressedTarget == null ? super.mouseClicked(event, twice) : pressedTarget.mouseClicked(event, twice);
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
