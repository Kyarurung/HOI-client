package dev.hoi.client.screen.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.map.commander.CommanderCombatLogHud;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderOverviewPayloads.Tab;
import dev.hoi.protocol.commander.CommanderRecordPayloads.*;
import java.util.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public final class CommanderFieldEquipmentWindow {
    static final int X = 540, WIDTH = 320, ROW = 30, ENTRY = 270;
    private static final int STEP = 40;
    record Cell(int iconX, int iconY, int textX, int textWidth) {}
    record Layout(int top, int first, int rowX, int itemX, int itemHeight, List<Cell> cells) {}
    private static final Map<Tab, Layout> LAYOUTS = Map.of(
            Tab.ARMY, new Layout(108, 111, 19, 19, 55, List.of(new Cell(40, 65, 80, 115), new Cell(201, 72, 231, 60))),
            Tab.NAVY, new Layout(89, 109, 20, 19, 69, List.of(new Cell(40, 70, 80, 95), new Cell(180, 65, 220, 80))),
            Tab.AIR, new Layout(89, 109, 20, 19, 69, List.of(new Cell(30, 72, 70, 52), new Cell(124, 65, 160, 50), new Cell(211, 72, 241, 60))));
    private record Entry(Archetype row, Item item, int y, int h) {}
    record Box(double x, double y, double w, double h) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    private final String token = UUID.randomUUID().toString();
    private final Tab tab;
    private final Set<String> expanded = new HashSet<>();
    private FieldEquipment equipment;
    private UiMenuStyle.Frame frame;
    private int height, scroll;
    private boolean dragging;
    CommanderFieldEquipmentWindow(Tab tab) { this.tab = tab; }
    void request() { CommanderCombatLogHud.send(new Request(token, Action.EQUIPMENT, tab.name(), 0, 0, List.of())); }
    void close() { CommanderCombatLogHud.send(new Request(token, Action.CLOSE_EQUIPMENT, tab.name(), 0, 0, List.of())); }
    boolean accept(FieldEquipment next) {
        if (!next.token().equals(token)) return true;
        equipment = next; scroll = Math.clamp(scroll, 0, maxScroll());
        return next.error().isEmpty();
    }
    FieldEquipment equipment() { return equipment; }
    UiMenuStyle.Frame frame() { return frame; }
    Layout layout() { return LAYOUTS.get(tab); }
    Set<String> expanded() { return expanded; }
    int scroll() { return scroll; }
    double bottom() { return frame.y(height); }
    int listBottom() { return height - 13; }
    void layout(UiMenuStyle.Frame overview, int width, int screenHeight) {
        float scale = overview.scale(), top = overview.y(0);
        height = Math.max(layout().first() + ROW * 4, (int)((screenHeight * .7f - top) / scale));
        frame = new UiMenuStyle.Frame(Math.max(0, Math.min(X * scale, width - WIDTH * scale)), top, scale);
        scroll = Math.clamp(scroll, 0, maxScroll());
    }
    boolean contains(double x, double y) { return frame != null && gui(new Box(0, 0, WIDTH, height)).contains(x, y); }
    private Box gui(Box box) { return new Box(frame.x(box.x()), frame.y(box.y()), frame.size(box.w()), frame.size(box.h())); }
    private String label(String key) { return equipment == null ? "" : equipment.label(key); }
    private boolean shown() { return equipment != null && equipment.error().isEmpty(); }
    private int listHeight() { return listBottom() - layout().top(); }
    private List<Entry> entries() {
        var result = new ArrayList<Entry>(); int y = 0;
        for (var row : equipment.rows()) {
            result.add(new Entry(row, null, y, ROW)); y += ROW;
            if (expanded.contains(row.id())) for (var item : row.items()) { result.add(new Entry(row, item, y, layout().itemHeight())); y += layout().itemHeight(); }
        }
        return result;
    }
    private int total() { var entries = entries(); return layout().first() - layout().top() + (entries.isEmpty() ? 0 : entries.getLast().y() + entries.getLast().h()); }
    private int maxScroll() { return shown() && height > 0 ? Math.max(0, total() - listHeight()) : 0; }
    private Box box(Entry entry) { return new Box(entry.item() == null ? layout().rowX() : layout().itemX(), layout().first() + entry.y() - scroll, ENTRY, entry.h()); }
    Box rowBox(String id) { return entries().stream().filter(entry -> entry.item() == null && entry.row().id().equals(id)).map(this::box).findFirst().orElseThrow(); }
    private Box list() { return new Box(4, layout().top(), WIDTH - 26, listHeight()); }
    private Box cell(Cell cell) { return new Box(cell.iconX(), 64, cell.textX() + cell.textWidth() - cell.iconX(), 32); }
    private UiScrollBar bar() { return new UiScrollBar(frame.ix(WIDTH - 21), frame.iy(layout().top()), frame.iy(listBottom()), Math.round(frame.size(listHeight())), Math.round(frame.size(total()))); }
    void draw(GuiGraphicsExtractor g, Font font) {
        if (frame == null) return;
        g.nextStratum();
        frame.tile(g, "intel/source/gfx_tiled_window2_1b_border/frame_0", 0, 0, WIDTH, height, 64, 192);
        if (shown()) {
            g.enableScissor(frame.ix(4), frame.iy(layout().top()), frame.ix(WIDTH - 4), frame.iy(listBottom()));
            for (var entry : entries()) if (entry.y() + entry.h() >= scroll - ROW && entry.y() <= scroll + listHeight()) entry(g, font, entry);
            g.disableScissor();
        }
        frame.art(g, "commander/equipment/header", 8, 7);
        UiMenuStyle.title(g, font, frame, label("EQUIPMENT_IN_USE_HEADER"), 14, 33, 288, 0, UiMenuStyle.TEXT);
        if (!shown()) return;
        for (int i = 0; i < Math.min(layout().cells().size(), equipment.totals().size()); i++) {
            var cell = layout().cells().get(i); var total = equipment.totals().get(i);
            frame.art(g, total.icon(), cell.iconX(), cell.iconY());
            text(g, font, total.value(), cell.textX(), 70, cell.textWidth(), 20, -1);
        }
        if (maxScroll() > 0) bar().draw(g, Math.round(frame.size(scroll)));
    }
    private void entry(GuiGraphicsExtractor g, Font font, Entry entry) {
        double x = box(entry).x(), y = box(entry).y();
        if (entry.item() == null) {
            frame.art(g, "commander/commands/unit_stats_list_entry_bg2/frame_0", x, y, ENTRY, ROW);
            double amount = width(font, entry.row().amount(), 190);
            text(g, font, entry.row().name(), x + 12, y, entry.row().carrier() ? 130 : ENTRY - 30 - amount, ROW, -1);
            if (entry.row().carrier()) frame.art(g, "commander/commands/airwing_carrier_icon/frame_0", x + 145, y + 5);
            text(g, font, entry.row().amount(), x + ENTRY - 10 - amount, y, amount, ROW, 1);
            return;
        }
        nine(g, "commander/commands/mini_tooltip/frame_0", x, y, ENTRY, entry.h(), 12);
        double amount = width(font, entry.item().amount(), 90);
        switch (tab) {
            case ARMY -> {
                picture(g, entry.item().image(), x + 50, y + 30, .65, 96, 43);
                text(g, font, entry.item().name(), x + 104, y + 7, 250 - amount - 112, 20, -1);
                text(g, font, entry.item().amount(), x + 250 - amount, y + 15, amount, 20, 1);
            }
            case NAVY, AIR -> {
                picture(g, entry.item().image(), x + (tab == Tab.NAVY ? 145 : 125), y + 42, tab == Tab.NAVY ? .7 : .8, 220, 50);
                text(g, font, entry.item().name(), x + 10, y + 5, 253 - amount - 18, 20, -1);
                text(g, font, entry.item().amount(), x + 253 - amount, y + 5, amount, 20, 1);
            }
        }
    }
    private double width(Font font, String value, double maximum) { return Math.min(maximum, (font.width(value) + 1) * UiText.scale(font) / frame.scale()); }
    private void picture(GuiGraphicsExtractor g, String image, double cx, double cy, double scale, double maxWidth, double maxHeight) {
        int[] size = UiAssets.imageSize(image);
        if (size[0] <= 0 || size[1] <= 0) return;
        double fit = Math.min(scale, Math.min(maxWidth / size[0], maxHeight / size[1])), w = size[0] * fit, h = size[1] * fit;
        frame.art(g, image, cx - w / 2, cy - h / 2, w, h);
    }
    private void nine(GuiGraphicsExtractor g, String path, double x, double y, int w, int h, int border) {
        g.pose().pushMatrix();
        g.pose().translate(frame.x(x), frame.y(y));
        g.pose().scale(frame.scale());
        UiAssets.nineSlice(g, path, 0, 0, w, h, border, 1);
        g.pose().popMatrix();
    }
    private void text(GuiGraphicsExtractor g, Font font, String value, double x, double y, double w, double h, int align) {
        if (value == null || value.isEmpty()) return;
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.panelText(value));
        float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(x), UiText.centeredY(font, line, frame.y(y), frame.size(h), scale), frame.size(w), scale, UiMenuStyle.TEXT, false, align);
    }
    String tooltip(double mx, double my) { return TooltipDelay.visible(source(mx, my)); }
    String source(double mx, double my) {
        if (!shown() || !contains(mx, my)) return "";
        for (int i = 0; i < Math.min(layout().cells().size(), equipment.totals().size()); i++) if (gui(cell(layout().cells().get(i))).contains(mx, my)) return equipment.totals().get(i).tooltip();
        if (!gui(list()).contains(mx, my) || my < frame.y(layout().first() - 1)) return "";
        for (var entry : entries()) if (entry.item() == null && gui(box(entry)).contains(mx, my)) return label(expanded.contains(entry.row().id()) ? "EQUIPMENT_VARIANTS_TOGGLE_OFF" : "EQUIPMENT_VARIANTS_TOGGLE_ON");
        return "";
    }
    boolean mouseClicked(MouseButtonEvent event) {
        double mx = event.x(), my = event.y();
        if (!contains(mx, my)) return false;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !shown()) return true;
        if (maxScroll() > 0 && bar().contains(mx, my)) {
            if (bar().arrow(my)) scroll = Math.clamp(scroll + (my < frame.y(layout().top()) + 8 ? -STEP : STEP), 0, maxScroll());
            else { dragging = true; drag(my); }
            return true;
        }
        if (!gui(list()).contains(mx, my) || my < frame.y(layout().first() - 1)) return true;
        for (var entry : entries()) if (entry.item() == null && gui(box(entry)).contains(mx, my)) {
            AudioUiSounds.play("ui.click");
            if (!expanded.remove(entry.row().id())) expanded.add(entry.row().id());
            scroll = Math.clamp(scroll, 0, maxScroll());
            return true;
        }
        return true;
    }
    private void drag(double my) { scroll = Math.clamp(Math.round(bar().offset(my) / frame.scale()), 0, maxScroll()); }
    boolean mouseDragged(MouseButtonEvent event) {
        if (!dragging) return false;
        drag(event.y());
        return true;
    }
    boolean mouseReleased() { boolean held = dragging; dragging = false; return held; }
    boolean mouseScrolled(double x, double y, double vertical) {
        if (!contains(x, y)) return false;
        scroll = Math.clamp(scroll - (int)Math.round(vertical * STEP), 0, maxScroll());
        return true;
    }
}
