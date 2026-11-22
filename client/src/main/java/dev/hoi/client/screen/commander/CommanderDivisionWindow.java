package dev.hoi.client.screen.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.client.map.commander.CommanderPanels;
import dev.hoi.client.network.NetworkScreens;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.client.ui.industry.IndustryTerrainAdjustment;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.ProtocolTooltip;
import dev.hoi.protocol.commander.CommanderDivisionPayloads;
import dev.hoi.protocol.commander.CommanderDivisionPayloads.*;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.industry.IndustryTankDesigner;
import dev.hoi.protocol.menu.MenuTab;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class CommanderDivisionWindow {
    static final int WIDTH = 856, HEIGHT = 522, LIST_X = 560, LIST_Y = 118, LIST_W = 296, LIST_H = 359, HEADER = 30, ENTRY = 50, NAME = 0xFF3FAF3F, ADJUST_X = 10, ADJUST_Y = 353, ADJUST_W = 545, ADJUST_H = 132, SLOT_W = 110, SLOT_H = 62,
            VALUE_RIGHT = 161, DROP_X = 10, DROP_Y = 65, DROP_W = 219, DROP_H = 300, DROP_ENTRY = 50;
    private static final String PROMOTE = "commander/commands/leader_selection_button/frame_0";
    static final String[] TABS = {"UNIT_STATS_STATS_TAB", "UNIT_STATS_TEMPLATE_TAB", "UNIT_STATS_HISTORY_TAB"}, TITLES = {"DESIGNER_BASE_STATS_LABEL", "DESIGNER_COMBAT_STATS_LABEL", "FE_MISC_STATS"}, GROUPS = {"BASE", "COMBAT", "MISC"};
    static final int[][] COLUMNS = {{18, 18}, {192, 192}, {367, 364}};
    static final double[] ROWS = {115.5, 135.5, 155, 175, 195, 215, 235, 254.5, 274, 294, 313};
    public static Consumer<Request> requestObserver;
    interface Host { void edit(EditBox box); void release(EditBox box); }
    record Box(double x, double y, double w, double h) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    record Line(boolean header, String name, String amount, String icon, String tooltip) {}
    private final String division, token = UUID.randomUUID().toString();
    private final Host host;
    private View view;
    private UiMenuStyle.Frame frame;
    private EditBox editor;
    private int tab, scroll, adjustScroll, dropScroll, width, height;
    private float base, movedX, movedY;
    private double grabX, grabY;
    private boolean dragging, adjustDragging, dropDragging, moving, dropdown;
    CommanderDivisionWindow(String division, Host host) { this.division = division; this.host = host; }
    String division() { return division; }
    View view() { return view; }
    int tab() { return tab; }
    int scroll() { return scroll; }
    boolean dropdown() { return dropdown; }
    boolean editing() { return editor != null; }
    EditBox editor() { return editor; }
    UiMenuStyle.Frame frame() { return frame; }
    void open() { AudioUiSounds.play("ui.pop_up"); request(); }
    void request() { send(new Request(token, Action.OPEN, division)); }
    void close() { cancelName(); send(new Request(token, Action.CLOSE, division)); }
    void accept(View next) { if (next.token().equals(token)) { view = next; scroll = Math.clamp(scroll, 0, maxScroll()); adjustScroll = Math.clamp(adjustScroll, 0, maxAdjustScroll()); dropScroll = Math.clamp(dropScroll, 0, maxDropScroll()); if (view.locked()) dropdown = false; } }
    private void send(Request request) {
        if (requestObserver != null) requestObserver.accept(request);
        else if (ClientPlayNetworking.canSend(Request.TYPE)) ClientPlayNetworking.send(request);
    }
    void layout(int width, int height, float base) {
        cancelName();
        this.width = width; this.height = height; this.base = base;
        place();
    }
    private void place() {
        float s = Math.min(base, Math.min((width - 8f) / WIDTH, (height - 8f) / HEIGHT));
        float x = Math.clamp(width / 2f - 275 * s + movedX, 0, Math.max(0, width - WIDTH * s)), y = Math.clamp(height / 2f - 235 * s + movedY, 0, Math.max(0, height - HEIGHT * s));
        movedX = x - (width / 2f - 275 * s); movedY = y - (height / 2f - 235 * s);
        frame = new UiMenuStyle.Frame(x, y, s);
    }
    boolean contains(double x, double y) { return frame != null && gui(new Box(0, 0, WIDTH, HEIGHT)).contains(x, y); }
    private Box gui(Box box) { return new Box(frame.x(box.x()), frame.y(box.y()), frame.size(box.w()), frame.size(box.h())); }
    private String label(String key) { return view == null ? "" : view.label(key); }
    private List<Line> lines() {
        var result = new ArrayList<Line>();
        if (view == null) return result;
        for (var row : view.equipmentRows()) {
            result.add(new Line(true, row.name(), thousands(row.have()) + "/" + thousands(row.need()), "", ""));
            for (var variant : row.variants()) {
                String description = IndustryEquipmentDesigns.equipmentDescription(view.country(), variant.source());
                result.add(new Line(false, variant.name(), Long.toString(variant.amount()), variant.icon(), "§e" + variant.name() + "§r" + (description.isEmpty() ? "" : "\n" + description)));
            }
        }
        return result;
    }
    private static String thousands(long value) { return value < 1000 ? Long.toString(value) : Math.round(value / 1000.0) + "K"; }
    private int contentHeight() { return lines().stream().mapToInt(line -> line.header() ? HEADER : ENTRY).sum(); }
    private int maxScroll() { return Math.max(0, contentHeight() - LIST_H + 6); }
    private UiScrollBar bar() { return new UiScrollBar(frame.ix(LIST_X + LIST_W - 22), frame.iy(LIST_Y), frame.iy(LIST_Y + LIST_H), Math.round(frame.size(LIST_H)), Math.round(frame.size(contentHeight() + 6))); }
    private List<IndustryTankDesigner.Adjustment> terrains() { return view == null ? List.of() : view.terrains().stream().filter(value -> Math.abs(value.movement()) > 1e-8 || Math.abs(value.attack()) > 1e-8 || Math.abs(value.defense()) > 1e-8).toList(); }
    private int maxAdjustScroll() { return Math.max(0, (terrains().size() + 4) / 5 * SLOT_H - ADJUST_H); }
    int adjustScroll() { return adjustScroll; }
    private UiScrollBar adjustBar() { return new UiScrollBar(frame.ix(ADJUST_X + ADJUST_W - 20), frame.iy(ADJUST_Y), frame.iy(ADJUST_Y + ADJUST_H), Math.round(frame.size(ADJUST_H)), Math.round(frame.size(ADJUST_H + maxAdjustScroll()))); }
    private int maxDropScroll() { return view == null ? 0 : Math.max(0, view.templates().size() * DROP_ENTRY - (DROP_H - 16)); }
    private UiScrollBar dropBar() { return new UiScrollBar(frame.ix(DROP_X + DROP_W - 20), frame.iy(DROP_Y + 8), frame.iy(DROP_Y + DROP_H - 8), Math.round(frame.size(DROP_H - 16)), Math.round(frame.size(view.templates().size() * DROP_ENTRY))); }
    private Box dropBox() { return new Box(DROP_X, DROP_Y, DROP_W, DROP_H); }
    private Box dropEntry(int index) { return new Box(17, DROP_Y + 8 + index * DROP_ENTRY - dropScroll, 190, DROP_ENTRY); }
    private static Box closeBox() { return new Box(822, 2, 31, 31); }
    private static Box deleteBox() { return new Box(560, 2, 31, 31); }
    private static Box promoteBox() { return new Box(740, 34, 90, 31); }
    private static Box templateBox() { return new Box(18, 37, 76, 31); }
    private static Box nameBox() { return new Box(114, 42, 238, 20); }
    private static Box resetBox() { return new Box(354, 41, 19, 19); }
    private static Box tabBox(int i) { return new Box(10 + 120 * i, 487, 113, 36); }
    private Box adjustBox(int index) { return new Box(17 + index % 5 * SLOT_W, ADJUST_Y + 5 + index / 5 * SLOT_H - adjustScroll, 70, SLOT_H - 1); }
    void draw(GuiGraphicsExtractor g, Font font, int mx, int my) {
        if (frame == null) return;
        g.nextStratum();
        frame.art(g, "commander/commands/unit_stats_bg/frame_0", 0, 0, WIDTH, 497);
        for (int i = 0; i < TABS.length; i++) {
            var box = tabBox(i);
            frame.art(g, "commander/commands/unit_stats_view_tabs/frame_" + (tab == i ? 0 : 1), box.x(), box.y(), box.w(), box.h());
            text(g, font, label(TABS[i]), box.x(), box.y() + 2, box.w(), box.h() - 4, 0, UiMenuStyle.TEXT);
        }
        UiMenuStyle.title(g, font, frame, label("UNIT_STATS"), 35, 17.5, 330, -1, UiMenuStyle.TEXT);
        frame.art(g, "commander/commands/nv_btn_delete/frame_0", 560, 2, 31, 31);
        frame.art(g, "commander/commands/nv_btn_close/frame_0", 822, 2, 31, 31);
        if (view == null) return;
        if (!view.error().isEmpty()) { text(g, font, view.error(), 20, 80, 520, 20, -1, UiMenuStyle.TEXT); return; }
        frame.art(g, "commander/commands/manpower_icon/frame_0", 600, 3, 27, 27);
        text(g, font, view.manpower(), 629, 6, 87, 20, 0, UiMenuStyle.TEXT);
        frame.art(g, "commander/commands/equipment_icon/frame_0", 735, 6, 22, 23);
        text(g, font, view.equipment(), 760, 6, 50, 20, 0, UiMenuStyle.TEXT);
        frame.art(g, "commander/commands/unitlist_unitinfo_button/frame_0", 18, 37, 76, 31);
        String icon = CommanderPanels.armyUnitIcon(view.icon());
        if (UiAssets.imageSize(icon)[0] > 0) frame.art(g, icon, 20, 35, 60.8, 33.6);
        if (!view.locked()) frame.art(g, "commander/commands/change_template_icon/frame_0", 79, 40, 11.9, 17.5);
        if (view.obsolete()) frame.art(g, "commander/commands/obsolete_template_warning/frame_0", 23, 40, 16, 14);
        frame.art(g, "commander/commands/unitlist_priority_icon_strip/frame_" + view.priority(), 93, 36);
        if (editor == null) text(g, font, view.name(), 114, 42, view.custom() ? 238 : 258, 20, -1, view.custom() ? NAME : UiMenuStyle.TEXT);
        else {
            g.fill(frame.ix(110), frame.iy(40), frame.ix(376), frame.iy(64), 0xC0000000);
            editor.extractRenderState(g, mx, my, 0);
        }
        if (view.custom() && editor == null) frame.art(g, "commander/commands/expand_button/frame_0", 354, 41);
        text(g, font, label("UNIT_VIEW_ARMY_EXPERIENCE"), 385, 42, 78, 20, -1, UiMenuStyle.TEXT);
        frame.art(g, "commander/commands/unit_level/frame_" + view.level(), 464, 38, 27, 27);
        var officer = view.officer();
        if (officer != null) {
            frame.art(g, "commander/commands/unit_stats_list_entry_bg2/frame_0", 562, 34, 268, 32);
            text(g, font, label("COMMANDER_TITLE_TEXT_DIVISION"), 574, 39, 160, 20, -1, UiMenuStyle.TEXT);
            if (officer.promotable()) frame.art(g, PROMOTE, 740, 34, 90, 31);
            else UiAssets.drawGrayscale(g, PROMOTE, frame.ix(740), frame.iy(34), frame.len(90), frame.len(31));
            text(g, font, label("PROMOTE"), 740, 34, 90, 31, 0, officer.promotable() ? UiMenuStyle.TEXT : UiMenuStyle.MUTED);
            frame.art(g, officer.portrait(), 568, 68, 52, 70);
            text(g, font, officer.title() + " " + officer.name(), 632, 72, 190, 20, -1, UiMenuStyle.TEXT);
        }
        switch (tab) { case 0 -> { stats(g, font); adjusters(g, font); } case 1 -> template(g, font); default -> history(g, font); }
        equipment(g, font);
        if (dropdown) templates(g, font);
    }
    private void stats(GuiGraphicsExtractor g, Font font) {
        for (int c = 0; c < GROUPS.length; c++) {
            UiMenuStyle.title(g, font, frame, label(TITLES[c]), COLUMNS[c][0], 87, 160, -1, UiMenuStyle.TEXT);
            int row = 0;
            for (var stat : view.stats()) {
                if (!stat.group().equals(GROUPS[c]) || row >= ROWS.length) continue;
                double x = COLUMNS[c][1], y = ROWS[row++] - 10;
                String icon = IndustryEquipmentDesigns.divisionStatField(stat.id(), "texture");
                if (!icon.isEmpty()) frame.art(g, icon, x, y + 2, 18, 16);
                String value = value(stat);
                double valueWidth = Math.min(110, font.width(value) * UiText.scale(font) / frame.scale() + 4);
                text(g, font, IndustryEquipmentDesigns.divisionStatField(stat.id(), "name"), x + 20, y, VALUE_RIGHT - 20 - valueWidth, 20, -1, UiMenuStyle.TEXT);
                text(g, font, value, x + VALUE_RIGHT - valueWidth, y, valueWidth, 20, 1, Tooltips.GOLD | 0xFF000000);
            }
        }
    }
    private void adjusters(GuiGraphicsExtractor g, Font font) {
        text(g, font, label("PRODUCTION_ADJUSTERS_LABEL"), 20, 334, 150, 20, -1, UiMenuStyle.TEXT);
        frame.art(g, "commander/commands/army_composition_icon/frame_0", 205, 325);
        frame.art(g, "commander/commands/armor_softness_icon/frame_0", 243, 330);
        frame.art(g, "commander/commands/inf_softness_icon/frame_0", 501, 329);
        int[] size = UiAssets.imageSize("commander/commands/softness_ratio/background/frame_0");
        frame.art(g, "commander/commands/softness_ratio/background/frame_0", 287, 330, size[0], size[1]);
        int filled = (int)Math.round(size[0] * view.hardness());
        if (filled > 0) {
            g.enableScissor(frame.ix(287), frame.iy(330), frame.ix(287 + filled), (int)Math.ceil(frame.y(330 + size[1])));
            frame.art(g, "commander/commands/softness_ratio/fill/frame_0", 287, 330, size[0], size[1]);
            g.disableScissor();
        }
        frame.art(g, "commander/commands/softness_ratio_frame/frame_0", 282, 325);
        g.enableScissor(frame.ix(ADJUST_X), frame.iy(ADJUST_Y), frame.ix(ADJUST_X + ADJUST_W - 22), frame.iy(ADJUST_Y + ADJUST_H));
        var terrains = terrains();
        for (int i = 0; i < terrains.size(); i++) {
            var box = adjustBox(i);
            if (box.y() + SLOT_H <= ADJUST_Y || box.y() >= ADJUST_Y + ADJUST_H) continue;
            IndustryTerrainAdjustment.draw(g, terrains.get(i), frame.ix(box.x()), frame.iy(box.y()), n -> Math.round(frame.size(n)));
        }
        g.disableScissor();
        if (maxAdjustScroll() > 0) adjustBar().draw(g, Math.round(frame.size(adjustScroll)));
    }
    private static String value(Stat stat) { return IndustryEquipmentDesigns.divisionStatValue(new dev.hoi.protocol.industry.IndustryView.Stat("", stat.group(), stat.value(), stat.unit(), stat.id())); }
    private static Box lineBox(int column, int row) { return new Box(23 + column * 85, 162 + row * 46, 80, 43); }
    private static Box supportBox(int row) { return new Box(455, 190 + row * 34, 64, 32); }
    private void template(GuiGraphicsExtractor g, Font font) {
        frame.art(g, "commander/commands/unit_stats_template_tab_bg/frame_0", 4, 69, 548, 423);
        text(g, font, label("DESIGNER_DIVISIONAL_COMPOSITION"), 31, 74, 372, 20, -1, UiMenuStyle.TEXT);
        text(g, font, Integer.toString(view.combatWidth()), 234, 115.5, 40, 20, 0, UiMenuStyle.TEXT);
        text(g, font, label("SUPPORT_HEADER"), 431, 169, 110, 20, 0, UiMenuStyle.TEXT);
        text(g, font, label("REGIMENTAL_SUPPORT_HEADER"), 107, 443, 250, 20, 0, UiMenuStyle.TEXT);
        for (int c = 0; c < view.columns().size(); c++) for (int r = 0; r < view.columns().get(c).size(); r++) {
            var box = lineBox(c, r);
            frame.art(g, "commander/commands/div_unit_item_bg/frame_0", box.x(), box.y(), box.w(), box.h());
            frame.art(g, view.columns().get(c).get(r).icon(), box.x() + 2, box.y(), 76, 42);
        }
        for (int r = 0; r < view.support().size(); r++) {
            var box = supportBox(r);
            frame.art(g, "commander/commands/div_subunit_item_bg/frame_0", box.x(), box.y(), box.w(), box.h());
            frame.art(g, view.support().get(r).icon(), box.x() + 2, box.y() - 1, 60, 34);
        }
        for (int c = 0; c < 5; c++) frame.art(g, "commander/commands/div_subunit_item_bg/frame_1", 32 + c * 85, 407, 64, 32);
    }
    private void history(GuiGraphicsExtractor g, Font font) {
        text(g, font, label("HISTORY_ENTRY_LABEL"), 29, 72, 180, 20, -1, UiMenuStyle.TEXT);
    }
    private void equipment(GuiGraphicsExtractor g, Font font) {
        int top = frame.iy(LIST_Y), bottom = frame.iy(LIST_Y + LIST_H);
        g.enableScissor(frame.ix(LIST_X), top, frame.ix(LIST_X + LIST_W), bottom);
        double y = LIST_Y + 3 - scroll;
        for (var line : lines()) {
            int height = line.header() ? HEADER : ENTRY;
            if (y + height >= LIST_Y && y <= LIST_Y + LIST_H) {
                if (line.header()) {
                    frame.art(g, "commander/commands/unit_stats_list_entry_bg2/frame_0", LIST_X + 2, y, 268, 32);
                    double amount = Math.max(50, font.width(line.amount()) * UiText.scale(font) / frame.scale() + 4);
                    text(g, font, line.name(), LIST_X + 12, y + 5, 252 - amount - 4, 20, -1, UiMenuStyle.TEXT);
                    text(g, font, line.amount(), LIST_X + 264 - amount, y + 5, amount, 20, 1, UiMenuStyle.TEXT);
                } else {
                    frame.art(g, "commander/commands/transparent_equipment_bg/frame_0", LIST_X + 2, y, 270, 50);
                    int[] size = UiAssets.contentSize(line.icon());
                    if (size[0] > 0 && size[1] > 0) {
                        double scale = Math.min(110.0 / size[0], 46.0 / size[1]);
                        UiAssets.drawContent(g, line.icon(), Math.round(frame.x(LIST_X + 2 + 50 - size[0] * scale / 2)), Math.round(frame.y(y + 25 - size[1] * scale / 2)), Math.round(frame.size(size[0] * scale)), Math.round(frame.size(size[1] * scale)));
                    }
                    double amount = Math.max(30, font.width(line.amount()) * UiText.scale(font) / frame.scale() + 4);
                    text(g, font, line.name(), LIST_X + 122, y, 138 - amount - 4, 50, -1, UiMenuStyle.TEXT);
                    text(g, font, line.amount(), LIST_X + 260 - amount, y, amount, 50, 1, UiMenuStyle.TEXT);
                }
            }
            y += height;
        }
        g.disableScissor();
        if (maxScroll() > 0) bar().draw(g, Math.round(frame.size(scroll)));
    }
    private void templates(GuiGraphicsExtractor g, Font font) {
        g.nextStratum();
        frame.tile(g, "commander/commands/tiled_window_1b_thin_border/frame_0", DROP_X, DROP_Y, DROP_W, DROP_H, 64, 192);
        g.enableScissor(frame.ix(DROP_X + 2), frame.iy(DROP_Y + 8), frame.ix(DROP_X + DROP_W - 2), frame.iy(DROP_Y + DROP_H - 8));
        for (int i = 0; i < view.templates().size(); i++) {
            var box = dropEntry(i);
            if (box.y() + box.h() <= DROP_Y || box.y() >= DROP_Y + DROP_H) continue;
            var entry = view.templates().get(i);
            UiAssets.nineSlice(g, "commander/commands/mini_tooltip/frame_0", frame.ix(box.x()), frame.iy(box.y()), frame.len(box.w()), frame.len(box.h()), 12, frame.scale());
            String icon = CommanderPanels.armyUnitIcon(entry.icon());
            if (UiAssets.imageSize(icon)[0] > 0) frame.art(g, icon, box.x() - 6, box.y() + 6, 76, 42);
            frame.art(g, "commander/commands/unitlist_priority_icon_strip/frame_" + entry.priority(), box.x() + 46, box.y() + 2);
            text(g, font, entry.name(), box.x() + 66, box.y(), box.w() - 70, box.h(), -1, entry.blocked().isEmpty() ? UiMenuStyle.TEXT : UiMenuStyle.MUTED);
        }
        g.disableScissor();
        if (maxDropScroll() > 0) dropBar().draw(g, Math.round(frame.size(dropScroll)));
    }
    private void text(GuiGraphicsExtractor g, Font font, String value, double x, double y, double w, double h, int align, int color) {
        if (value == null || value.isEmpty()) return;
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.officerText(value));
        float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(x), UiText.centeredY(font, line, frame.y(y), frame.size(h), scale), frame.size(w), scale, color, false, align);
    }
    private static String adjustment(IndustryTankDesigner.Adjustment value, int only) {
        var lines = new ArrayList<String>();
        if (only < 0) lines.add("§e" + IndustryEquipmentDesigns.adjusterName(value.id()) + "§r");
        double[] values = {value.movement(), value.attack(), value.defense()};
        String[] keys = {"MOVEMENT", "ATTACK", "DEFENCE"};
        for (int i = 0; i < 3; i++) if (only == i || only < 0 && Math.abs(values[i]) > 1e-8)
            lines.add(IndustryEquipmentDesigns.tooltip("STAT_ADJUSTER_" + keys[i]).stripTrailing() + " " + (values[i] < 0 ? "§c" : values[i] > 0 ? "§a" : "§e") + String.format(Locale.ROOT, "%+.1f%%", values[i] * 100) + "§r");
        return String.join("\n", lines);
    }
    private static String unit(Unit unit) {
        var lines = new ArrayList<String>();
        for (var terrain : unit.terrains()) lines.add(adjustment(terrain, -1));
        return ProtocolTooltip.delayed(unit.tooltip(), "\n\n", String.join("\n", lines));
    }
    String tooltip(double mx, double my) {
        if (frame == null || view == null || !view.error().isEmpty()) return "";
        if (dropdown && gui(dropBox()).contains(mx, my)) {
            if (gui(new Box(DROP_X, DROP_Y + 8, DROP_W, DROP_H - 16)).contains(mx, my)) for (int i = 0; i < view.templates().size(); i++) if (gui(dropEntry(i)).contains(mx, my)) return view.templates().get(i).blocked();
            return "";
        }
        if (gui(closeBox()).contains(mx, my)) return label("CLOSE");
        if (gui(deleteBox()).contains(mx, my)) return view.tooltip("delete");
        if (gui(new Box(600, 3, 116, 27)).contains(mx, my)) return view.tooltip("manpower");
        if (gui(new Box(735, 3, 75, 27)).contains(mx, my)) return view.tooltip("equipment");
        if (view.obsolete() && gui(new Box(23, 40, 16, 14)).contains(mx, my)) return label("DECOMMISSIONED_TEMPLATE_WARNING_TOOLTIP");
        if (gui(templateBox()).contains(mx, my)) return view.tooltip("template");
        if (view.custom() && editor == null && gui(resetBox()).contains(mx, my)) return view.tooltip("reset");
        if (editor == null && gui(nameBox()).contains(mx, my)) return view.tooltip("name");
        if (gui(new Box(385, 38, 106, 27)).contains(mx, my)) return view.tooltip("experience");
        if (view.officer() != null && gui(promoteBox()).contains(mx, my)) return view.officer().promotion();
        if (gui(new Box(LIST_X, LIST_Y, LIST_W - 22, LIST_H)).contains(mx, my)) {
            double y = LIST_Y + 3 - scroll;
            for (var line : lines()) { int h = line.header() ? HEADER : ENTRY; if (gui(new Box(LIST_X + 2, y, 270, h)).contains(mx, my)) return line.tooltip(); y += h; }
        }
        if (tab == 0) return statTooltip(mx, my);
        if (tab == 1) return templateTooltip(mx, my);
        return "";
    }
    private String statTooltip(double mx, double my) {
        if (gui(new Box(ADJUST_X, ADJUST_Y, ADJUST_W - 22, ADJUST_H)).contains(mx, my)) {
            var terrains = terrains();
            for (int i = 0; i < terrains.size(); i++) {
                var box = adjustBox(i);
                for (int icon = 0; icon < 3; icon++) if (gui(new Box(box.x() + 1 + icon * 23, box.y() + 40, 20, 20)).contains(mx, my)) return adjustment(terrains.get(i), icon);
                if (gui(box).contains(mx, my)) return adjustment(terrains.get(i), -1);
            }
            return "";
        }
        if (gui(new Box(205, 325, 22, 22)).contains(mx, my)) return view.tooltip("composition");
        if (gui(new Box(243, 325, 277, 27)).contains(mx, my)) return view.tooltip("hardness");
        for (int c = 0; c < GROUPS.length; c++) {
            int row = 0;
            for (var stat : view.stats()) if (stat.group().equals(GROUPS[c]) && row < ROWS.length && gui(new Box(COLUMNS[c][1], ROWS[row++] - 10, 163, 20)).contains(mx, my)) return stat.tooltip();
        }
        return "";
    }
    private String templateTooltip(double mx, double my) {
        if (gui(new Box(31, 74, 372, 20)).contains(mx, my)) return view.tooltip("line");
        if (gui(new Box(234, 114, 40, 22)).contains(mx, my)) return view.tooltip("width");
        if (gui(new Box(431, 169, 110, 20)).contains(mx, my)) return view.tooltip("support");
        if (gui(new Box(107, 443, 250, 20)).contains(mx, my)) return view.tooltip("regiment");
        for (int c = 0; c < view.columns().size(); c++) for (int r = 0; r < view.columns().get(c).size(); r++) if (gui(lineBox(c, r)).contains(mx, my)) return unit(view.columns().get(c).get(r));
        for (int r = 0; r < view.support().size(); r++) if (gui(supportBox(r)).contains(mx, my)) return unit(view.support().get(r));
        return "";
    }
    private void editName(Font font) {
        editor = new EditBox(font, frame.ix(114), frame.iy(42) + Math.max(0, (frame.len(20) - 9) / 2), frame.len(258), 10, Component.literal(view.name()));
        editor.setBordered(false); editor.setMaxLength(CommanderDivisionPayloads.NAME_LIMIT);
        editor.setValue(view.name());
        editor.setResponder(value -> { String clean = value.replaceAll("[§£$]", ""); if (editor != null && !clean.equals(value)) editor.setValue(clean); });
        host.edit(editor);
    }
    private void cancelName() { if (editor != null) { var box = editor; editor = null; host.release(box); } }
    private void commitName() {
        String value = editor.getValue().strip();
        cancelName();
        if (!value.equals(view.name())) rename(value);
    }
    private void rename(String value) { send(new Request(token, Action.RENAME, division, value)); }
    boolean keyPressed(KeyEvent event) {
        if (editor == null) return false;
        if (event.key() == InputConstants.KEY_ESCAPE) { cancelName(); return true; }
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) { commitName(); return true; }
        return false;
    }
    boolean mouseClicked(MouseButtonEvent event, boolean twice, Font font, Runnable closer) {
        if (!contains(event.x(), event.y())) { if (editor != null) commitName(); return false; }
        if (editor != null) {
            if (gui(new Box(110, 40, 266, 24)).contains(event.x(), event.y())) { editor.mouseClicked(event, twice); return true; }
            commitName();
        }
        boolean left = event.button() == InputConstants.MOUSE_BUTTON_LEFT, right = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        if (left && gui(closeBox()).contains(event.x(), event.y())) { AudioUiSounds.play("ui.close"); closer.run(); return true; }
        if (view == null || !view.error().isEmpty()) { if (left) grab(event); return true; }
        if (dropdown && gui(dropBox()).contains(event.x(), event.y())) { if (left) choose(event); return true; }
        if (gui(templateBox()).contains(event.x(), event.y())) { if (left || right) template(event, right); return true; }
        if (!left) return true;
        if (gui(deleteBox()).contains(event.x(), event.y())) {
            AudioUiSounds.play("ui.disband");
            CommanderHud.open(new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.ARMY_TOOLBAR, "DIVISIONS_DISBAND", List.of("unit/" + division)));
            return true;
        }
        if (view.custom() && gui(resetBox()).contains(event.x(), event.y())) { AudioUiSounds.play("ui.click"); rename(""); return true; }
        if (gui(nameBox()).contains(event.x(), event.y())) { editName(font); return true; }
        if (view.officer() != null && gui(promoteBox()).contains(event.x(), event.y())) {
            if (view.officer().promotable()) CommanderHud.open(new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.FIELD_OFFICERS, "", List.of("unit", "promote/" + division)));
            return true;
        }
        for (int i = 0; i < TABS.length; i++) if (gui(tabBox(i)).contains(event.x(), event.y())) { tab = i; return true; }
        if (tab == 0 && maxAdjustScroll() > 0 && adjustBar().contains(event.x(), event.y())) {
            if (adjustBar().arrow(event.y())) adjustScroll = Math.clamp(adjustScroll + (event.y() < frame.y(ADJUST_Y) + 8 ? -SLOT_H : SLOT_H), 0, maxAdjustScroll());
            else { adjustDragging = true; adjustScroll = Math.clamp(Math.round(adjustBar().offset(event.y()) / frame.scale()), 0, maxAdjustScroll()); }
            return true;
        }
        if (maxScroll() > 0 && bar().contains(event.x(), event.y())) {
            if (bar().arrow(event.y())) scroll = Math.clamp(scroll + (event.y() < frame.y(LIST_Y) + 8 ? -ENTRY : ENTRY), 0, maxScroll());
            else { dragging = true; scroll = Math.clamp(Math.round(bar().offset(event.y()) / frame.scale()), 0, maxScroll()); }
            return true;
        }
        grab(event);
        return true;
    }
    private void grab(MouseButtonEvent event) { moving = true; grabX = event.x() - movedX; grabY = event.y() - movedY; }
    private void template(MouseButtonEvent event, boolean right) {
        if (right) { NetworkScreens.openDesigner(view.templateId()); return; }
        AudioUiSounds.play("ui.click");
        if (event.hasControlDown()) { HoiClient.openMenu(MenuTab.RECRUITMENT); return; }
        if (!view.locked()) { dropdown = !dropdown; dropScroll = 0; }
    }
    private void choose(MouseButtonEvent event) {
        if (maxDropScroll() > 0 && dropBar().contains(event.x(), event.y())) {
            if (dropBar().arrow(event.y())) dropScroll = Math.clamp(dropScroll + (event.y() < frame.y(DROP_Y + 8) + 8 ? -DROP_ENTRY : DROP_ENTRY), 0, maxDropScroll());
            else { dropDragging = true; dropScroll = Math.clamp(Math.round(dropBar().offset(event.y()) / frame.scale()), 0, maxDropScroll()); }
            return;
        }
        if (!gui(new Box(DROP_X, DROP_Y + 8, DROP_W, DROP_H - 16)).contains(event.x(), event.y())) return;
        for (int i = 0; i < view.templates().size(); i++) if (gui(dropEntry(i)).contains(event.x(), event.y())) {
            var entry = view.templates().get(i);
            if (!entry.blocked().isEmpty()) return;
            dropdown = false;
            CommanderHud.open(new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.ARMY_TOOLBAR, "DIVISIONS_TEMPLATE", List.of("unit/" + division, "template/" + entry.id())));
            return;
        }
    }
    boolean mouseDragged(MouseButtonEvent event) {
        if (moving) { movedX = (float)(event.x() - grabX); movedY = (float)(event.y() - grabY); place(); return true; }
        if (dropDragging) { dropScroll = Math.clamp(Math.round(dropBar().offset(event.y()) / frame.scale()), 0, maxDropScroll()); return true; }
        if (adjustDragging) { adjustScroll = Math.clamp(Math.round(adjustBar().offset(event.y()) / frame.scale()), 0, maxAdjustScroll()); return true; }
        if (!dragging) return false;
        scroll = Math.clamp(Math.round(bar().offset(event.y()) / frame.scale()), 0, maxScroll());
        return true;
    }
    void mouseReleased() { dragging = false; adjustDragging = false; dropDragging = false; moving = false; }
    boolean mouseScrolled(double x, double y, double vertical) {
        if (!contains(x, y)) return false;
        if (dropdown && gui(dropBox()).contains(x, y)) { dropScroll = Math.clamp(dropScroll - (int)Math.round(vertical * DROP_ENTRY), 0, maxDropScroll()); return true; }
        if (tab == 0 && gui(new Box(ADJUST_X, ADJUST_Y, ADJUST_W, ADJUST_H)).contains(x, y)) adjustScroll = Math.clamp(adjustScroll - (int)Math.round(vertical * SLOT_H), 0, maxAdjustScroll());
        if (gui(new Box(LIST_X, LIST_Y, LIST_W, LIST_H)).contains(x, y)) scroll = Math.clamp(scroll - (int)Math.round(vertical * ENTRY), 0, maxScroll());
        return true;
    }
}
