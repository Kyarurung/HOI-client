package dev.hoi.client.screen.commander;

import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderDivisionPayloads.View;
import dev.hoi.protocol.menu.MenuTab;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public final class CommanderDivisionHud {
    private static CommanderDivisionWindow window;
    private static int width, height;
    private CommanderDivisionHud() {}
    public static void register() {
        HudElementRegistry.attachElementAfter(Identifier.parse("hoi:commander_hud"), Identifier.parse("hoi:division_window"), (g, delta) -> {
            if (Minecraft.getInstance().gui.screen() == null) draw(g, g.guiWidth() / 2, g.guiHeight() / 2);
        });
    }
    static CommanderDivisionWindow window() { return window; }
    public static void open(String division) {
        if (window != null && window.division().equals(division)) return;
        if (window != null) window.close();
        window = new CommanderDivisionWindow(division, new CommanderDivisionWindow.Host() {
            @Override public void edit(EditBox box) { if (Minecraft.getInstance().gui.screen() instanceof MapCursor cursor) cursor.edit(box); }
            @Override public void release(EditBox box) { if (Minecraft.getInstance().gui.screen() instanceof MapCursor cursor) cursor.release(box); }
        });
        width = 0;
        layout();
        window.open();
    }
    public static void refresh() { if (window != null) { width = 0; window.request(); } }
    public static void accept(View view) {
        if (window == null) return;
        boolean shown = window.view() != null && window.view().error().isEmpty();
        window.accept(view);
        if (shown && !window.view().error().isEmpty()) close();
    }
    private static void layout() {
        var client = Minecraft.getInstance();
        int w = client.getWindow().getGuiScaledWidth(), h = client.getWindow().getGuiScaledHeight();
        if (w == width && h == height) return;
        width = w; height = h;
        window.layout(w, h, Math.max(180, UiPanelLayout.width(MenuTab.CONSTRUCTION, w)) / 550f);
    }
    private static boolean visible() {
        var client = Minecraft.getInstance();
        return window != null && client.level != null && !client.gui.hud.isHidden() && !AgencyHud.active() && (client.gui.screen() == null || client.gui.screen() instanceof MapCursor);
    }
    public static boolean covers(double x, double y) { return visible() && window.contains(x, y); }
    public static void draw(GuiGraphicsExtractor g, int mx, int my) {
        if (!visible()) return;
        render(g, mx, my);
        String text = window.contains(mx, my) ? window.tooltip(mx, my) : "";
        if (!text.isEmpty()) { g.nextStratum(); Tooltips.drawSourceScrollable(g, Minecraft.getInstance().font, text, mx, my, g.guiWidth(), g.guiHeight(), 0); }
    }
    public static void drawBackdrop(GuiGraphicsExtractor g) { if (window != null) render(g, -1, -1); }
    private static void render(GuiGraphicsExtractor g, int mx, int my) { layout(); window.draw(g, Minecraft.getInstance().font, mx, my); }
    public static boolean click(MouseButtonEvent event, boolean twice) { return visible() && window.mouseClicked(event, twice, Minecraft.getInstance().font, CommanderDivisionHud::close); }
    public static boolean drag(MouseButtonEvent event) { return visible() && window.mouseDragged(event); }
    public static boolean release() { if (window != null) window.mouseReleased(); return false; }
    public static boolean scroll(double x, double y, double vertical) { return visible() && window.mouseScrolled(x, y, vertical); }
    public static boolean keyPressed(KeyEvent event) { return visible() && window.keyPressed(event); }
    public static boolean closeWindow() {
        if (!visible()) return false;
        close();
        return true;
    }
    private static void close() { var old = window; window = null; old.close(); }
    public static void reset() { if (window != null) close(); }
}
