package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.network.NetworkScreens;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.commander.CommanderShipPayloads.*;
import dev.hoi.protocol.industry.IndustryTankDesigner;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public final class CommanderShipWindow {
    static final int WIDTH = 856, HEIGHT = 433, VALUE_RIGHT = 162, WING_X = 562, WING_Y = 160, WING_W = 290, WING_H = 236, WING = 46;
    private static final String PROMOTE = "commander/commands/leader_selection_button/frame_0";
    static final String[] TABS = {"NAVAL_STATS_LABEL", "NAVAL_HISTORY_LABEL", "NAVAL_DESIGN_LABEL"}, TITLES = {"DESIGNER_BASE_STATS_LABEL", "DESIGNER_COMBAT_STATS_LABEL", "FE_MISC_STATS"};
    static final int[] TAB_X = {10, 120, 340}, TITLE_X = {18, 192, 367}, COLUMN_X = {18, 190, 364};
    static final double[] ROWS = {110.5, 130, 150, 170, 190, 209.5, 229, 248};
    static Consumer<String> designer = NetworkScreens::openShipDesigner;
    record Box(double x, double y, double w, double h) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    private final String ship, token = UUID.randomUUID().toString();
    private View view;
    private UiMenuStyle.Frame frame;
    private int tab, scroll;
    private float left = Float.NaN, top;
    private double grabX, grabY;
    private boolean dragging, moving;
    CommanderShipWindow(String ship) { this.ship = ship; }
    String ship() { return ship; }
    View view() { return view; }
    int tab() { return tab; }
    UiMenuStyle.Frame frame() { return frame; }
    void open() { AudioUiSounds.play("ui.pop_up"); request(); }
    void request() { CommanderShipHud.send(new Request(token, Action.OPEN, ship)); }
    void close() { CommanderShipHud.send(new Request(token, Action.CLOSE, ship)); }
    boolean accept(View next) {
        if (!next.token().equals(token)) return true;
        view = next; scroll = Math.clamp(scroll, 0, maxScroll());
        return next.error().isEmpty();
    }
    void place(CommanderShipWindow previous) { left = previous.left; top = previous.top; }
    void layout(int width, int height, float base) {
        float scale = Math.min(base, Math.min((width - 8f) / WIDTH, (height - 8f) / HEIGHT));
        if (Float.isNaN(left)) { left = 500 * base; top = Math.max(100 * base, HudMenuBar.height(width)); }
        left = Math.clamp(left, 0, Math.max(0, width - WIDTH * scale)); top = Math.clamp(top, 0, Math.max(0, height - HEIGHT * scale));
        frame = new UiMenuStyle.Frame(left, top, scale);
    }
    boolean contains(double x, double y) { return frame != null && gui(new Box(0, 0, WIDTH, HEIGHT)).contains(x, y); }
    private Box gui(Box box) { return new Box(frame.x(box.x()), frame.y(box.y()), frame.size(box.w()), frame.size(box.h())); }
    private String label(String key) { return view == null ? "" : view.label(key); }
    private List<IndustryTankDesigner.Stat> stats(int column) { return view == null || column >= view.columns().size() ? List.of() : view.columns().get(column).stats(); }
    private int maxScroll() { return view == null ? 0 : Math.max(0, view.wings().size() * WING - WING_H); }
    private UiScrollBar bar() { return new UiScrollBar(frame.ix(WING_X + WING_W - 20), frame.iy(WING_Y), frame.iy(WING_Y + WING_H), Math.round(frame.size(WING_H)), Math.round(frame.size(WING_H + maxScroll()))); }
    private static Box closeBox() { return new Box(822, 3, 31, 31); }
    private static Box deleteBox() { return new Box(560, 3, 31, 31); }
    private static Box promoteBox() { return new Box(740, 34, 90, 31); }
    private static Box tabBox(int i) { return new Box(TAB_X[i], 397, 113, 36); }
    private static Box imageBox() { return new Box(148, 311, 244, 66); }
    static Box row(int column, int row) { return new Box(COLUMN_X[column], ROWS[row] - 9.5, VALUE_RIGHT + 2, 19); }
    void draw(GuiGraphicsExtractor g, Font font) {
        if (frame == null) return;
        g.nextStratum();
        frame.art(g, "commander/commands/naval_unit_stats_bg/frame_0", 0, 0, WIDTH, 409);
        for (int i = 0; i < TABS.length; i++) {
            var box = tabBox(i);
            frame.art(g, "commander/commands/unit_stats_view_tabs/frame_" + (tab == i ? 0 : 1), box.x(), box.y(), box.w(), box.h());
            text(g, font, label(TABS[i]), box.x(), box.y() + 2, box.w(), box.h() - 4, 0, UiMenuStyle.TEXT);
        }
        UiMenuStyle.title(g, font, frame, label("SHIP_UNIT_STATS"), 35, 17.5, 330, -1, UiMenuStyle.TEXT);
        frame.art(g, "commander/commands/nv_btn_delete/frame_0", 560, 3, 31, 31);
        frame.art(g, "commander/commands/nv_btn_close/frame_0", 822, 3, 31, 31);
        if (view == null || !view.error().isEmpty()) return;
        frame.art(g, view.type(), 17, 43);
        text(g, font, view.name(), 114, 42, 262, 20, -1, UiMenuStyle.TEXT);
        text(g, font, label("UNIT_VIEW_NAVAL_EXPERIENCE"), 385, 42, 108, 20, -1, UiMenuStyle.TEXT);
        frame.art(g, "commander/commands/unit_level/frame_" + view.level(), 496, 38, 27, 27);
        frame.art(g, "commander/commands/fuel/frame_0", 735, 6, 24, 24);
        text(g, font, view.fuel(), 762, 8, 47, 19, 0, UiMenuStyle.TEXT);
        var officer = view.officer();
        if (officer != null) {
            frame.art(g, "commander/commands/unit_stats_list_entry_bg2/frame_0", 562, 34, 268, 32);
            text(g, font, label("CAPTAIN_TITLE_TEXT"), 574, 39, 160, 20, -1, UiMenuStyle.TEXT);
            if (officer.promotable()) frame.art(g, PROMOTE, 740, 34, 90, 31);
            else UiAssets.drawGrayscale(g, PROMOTE, frame.ix(740), frame.iy(34), frame.len(90), frame.len(31));
            text(g, font, label("PROMOTE"), 740, 34, 90, 31, 0, officer.promotable() ? UiMenuStyle.TEXT : UiMenuStyle.MUTED);
            frame.art(g, officer.portrait(), 574, 68, 46, 62);
            text(g, font, officer.title() + " " + officer.name(), 632, 72, 190, 20, -1, UiMenuStyle.TEXT);
        }
        wings(g, font);
        if (tab == 0) stats(g, font); else history(g, font);
    }
    private void stats(GuiGraphicsExtractor g, Font font) {
        for (int c = 0; c < COLUMN_X.length; c++) {
            int column = c;
            outlined((dx, dy, color) -> UiMenuStyle.title(g, font, frame, label(TITLES[column]), TITLE_X[column] + dx, 86.5 + dy, 160, -1, color), UiMenuStyle.TEXT);
            var stats = stats(c);
            for (int r = 0; r < Math.min(ROWS.length, stats.size()); r++) {
                var stat = stats.get(r);
                double x = COLUMN_X[c], y = ROWS[r] - 10;
                String icon = "production/design/stats/" + stat.id(), value = IndustryEquipmentDesigns.statValue(stat, stat.value());
                int indent = UiAssets.imageSize(icon)[0] > 0 ? 20 : 0;
                if (indent > 0) UiAssets.drawAlignedContent(g, icon, icon, frame.x(x), frame.y(y + 1), frame.size(18), frame.size(18));
                double width = Math.min(90, font.width(value) * UiText.scale(font) / frame.scale() + 4);
                outlined((dx, dy, color) -> text(g, font, IndustryEquipmentDesigns.statName(stat.id()).strip(), x + indent + dx, y + dy, VALUE_RIGHT - indent - width, 20, -1, color), UiMenuStyle.TEXT);
                outlined((dx, dy, color) -> text(g, font, value, x + VALUE_RIGHT - width + dx, y + dy, width, 20, 1, color), stat.id().equals("hit_profile") ? UiMenuStyle.TEXT : Tooltips.GOLD | 0xFF000000);
            }
        }
        text(g, font, view.designName(), 148, 279, 245, 20, 0, UiMenuStyle.TEXT);
        int[] size = UiAssets.contentSize(view.image());
        if (size[0] > 0 && size[1] > 0) {
            var box = imageBox();
            double scale = Math.min(box.w() / size[0], box.h() / size[1]);
            UiAssets.drawContent(g, view.image(), Math.round(frame.x(box.x() + (box.w() - size[0] * scale) / 2)), Math.round(frame.y(box.y() + (box.h() - size[1] * scale) / 2)), Math.round(frame.size(size[0] * scale)), Math.round(frame.size(size[1] * scale)));
        }
        frame.art(g, "commander/commands/manpower_icon/frame_0", 20, 364, 27, 27);
        text(g, font, view.manpower(), 50, 367.5, 61, 20, 0, UiMenuStyle.TEXT);
    }
    private void history(GuiGraphicsExtractor g, Font font) {
        frame.tile(g, "commander/commands/tiled_paper_w_frame_one_border_2/frame_0", 0, 98, 557, 310, 50, 192);
        frame.art(g, "commander/commands/deployment_binding/frame_0", 6, 70, 533, 35);
        text(g, font, label("INSTANCED_MEDALS"), 25, 76.5, 100, 20, -1, UiMenuStyle.TEXT);
        text(g, font, view.tooltip("history"), 236, 76.5, 250, 20, 1, UiMenuStyle.TEXT);
    }
    private void wings(GuiGraphicsExtractor g, Font font) {
        if (view.wingTotal().isEmpty()) return;
        frame.art(g, "commander/commands/unit_stats_list_entry_bg2/frame_0", 562, 124, 268, 32);
        text(g, font, label("AIR_WINGS") + " " + view.wingTotal(), 574, 129, 244, 20, 0, UiMenuStyle.TEXT);
        g.enableScissor(frame.ix(WING_X), frame.iy(WING_Y), frame.ix(WING_X + WING_W - 20), frame.iy(WING_Y + WING_H));
        for (int i = 0; i < view.wings().size(); i++) {
            double y = WING_Y + i * WING - scroll;
            if (y + WING <= WING_Y || y >= WING_Y + WING_H) continue;
            var wing = view.wings().get(i);
            frame.art(g, "commander/commands/air_unit_stats_equip_entry/frame_0", WING_X, y, 268, 45);
            int[] size = UiAssets.contentSize(wing.icon());
            if (size[0] > 0 && size[1] > 0) {
                double scale = Math.min(64.0 / size[0], 36.0 / size[1]);
                UiAssets.drawContent(g, wing.icon(), Math.round(frame.x(WING_X + 42 - size[0] * scale / 2)), Math.round(frame.y(y + 22.5 - size[1] * scale / 2)), Math.round(frame.size(size[0] * scale)), Math.round(frame.size(size[1] * scale)));
            }
            text(g, font, wing.name(), WING_X + 80, y + 2, 182, 20, -1, UiMenuStyle.TEXT);
            text(g, font, wing.amount(), WING_X + 80, y + 23, 100, 20, -1, UiMenuStyle.TEXT);
        }
        g.disableScissor();
        if (maxScroll() > 0) bar().draw(g, Math.round(frame.size(scroll)));
    }
    private interface Pen { void draw(int dx, int dy, int color); }
    private static void outlined(Pen pen, int color) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (dx != 0 || dy != 0) pen.draw(dx, dy, 0xFF000000);
        pen.draw(0, 0, color);
    }
    private void text(GuiGraphicsExtractor g, Font font, String value, double x, double y, double w, double h, int align, int color) {
        if (value == null || value.isEmpty()) return;
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.officerText(value));
        float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(x), UiText.centeredY(font, line, frame.y(y), frame.size(h), scale), frame.size(w), scale, color, false, align);
    }
    String tooltip(double mx, double my) {
        if (frame == null || view == null || !view.error().isEmpty()) return "";
        if (gui(closeBox()).contains(mx, my)) return label("CLOSE");
        if (gui(deleteBox()).contains(mx, my)) return view.tooltip("delete");
        if (gui(new Box(735, 6, 74, 24)).contains(mx, my)) return stats(2).stream().filter(stat -> stat.id().equals("fuel_consumption")).findFirst().map(IndustryEquipmentDesigns::statTooltip).orElse("");
        if (gui(new Box(385, 38, 138, 27)).contains(mx, my)) return view.tooltip("experience");
        if (view.officer() != null && gui(promoteBox()).contains(mx, my)) return view.officer().promotion();
        if (tab != 0) return "";
        if (gui(new Box(20, 364, 91, 27)).contains(mx, my)) return view.tooltip("manpower");
        if (gui(imageBox()).contains(mx, my)) {
            String description = IndustryEquipmentDesigns.equipmentDescription(view.country(), view.tooltip("equipment"));
            return "§e" + view.designName() + "§r" + (description.isEmpty() ? "" : "\n" + description);
        }
        for (int c = 0; c < COLUMN_X.length; c++) for (int r = 0; r < Math.min(ROWS.length, stats(c).size()); r++) if (gui(row(c, r)).contains(mx, my)) return IndustryEquipmentDesigns.statTooltip(stats(c).get(r));
        return "";
    }
    boolean mouseClicked(MouseButtonEvent event, Runnable closer) {
        if (!contains(event.x(), event.y())) return false;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (gui(closeBox()).contains(event.x(), event.y())) { AudioUiSounds.play("ui.close"); closer.run(); return true; }
        if (view == null || !view.error().isEmpty()) { grab(event); return true; }
        if (gui(deleteBox()).contains(event.x(), event.y())) {
            AudioUiSounds.play("ui.disband");
            CommanderHud.open(new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.NAVAL_ORGANIZE, "DELETE_SHIP", List.of("ship/" + ship)));
            return true;
        }
        if (view.officer() != null && gui(promoteBox()).contains(event.x(), event.y())) {
            if (view.officer().promotable()) CommanderHud.open(new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.SHIP_CAPTAINS, "", List.of("unit", "promote/" + ship)));
            return true;
        }
        for (int i = 0; i < TABS.length; i++) if (gui(tabBox(i)).contains(event.x(), event.y())) { if (i < 2) tab = i; else if (view.designer()) designer.accept(view.design()); return true; }
        if (maxScroll() > 0 && bar().contains(event.x(), event.y())) {
            if (bar().arrow(event.y())) scroll = Math.clamp(scroll + (event.y() < frame.y(WING_Y) + 8 ? -WING : WING), 0, maxScroll());
            else { dragging = true; scroll = Math.clamp(Math.round(bar().offset(event.y()) / frame.scale()), 0, maxScroll()); }
            return true;
        }
        grab(event);
        return true;
    }
    private void grab(MouseButtonEvent event) { moving = true; grabX = event.x() - left; grabY = event.y() - top; }
    boolean mouseDragged(MouseButtonEvent event) {
        if (moving) { left = (float)(event.x() - grabX); top = (float)(event.y() - grabY); return true; }
        if (!dragging) return false;
        scroll = Math.clamp(Math.round(bar().offset(event.y()) / frame.scale()), 0, maxScroll());
        return true;
    }
    boolean mouseReleased() { boolean held = dragging || moving; dragging = false; moving = false; return held; }
    boolean mouseScrolled(double x, double y, double vertical) {
        if (!contains(x, y)) return false;
        if (gui(new Box(WING_X, WING_Y, WING_W, WING_H)).contains(x, y)) scroll = Math.clamp(scroll - (int)Math.round(vertical * WING), 0, maxScroll());
        return true;
    }
}
