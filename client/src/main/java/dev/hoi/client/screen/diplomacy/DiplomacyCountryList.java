package dev.hoi.client.screen.diplomacy;

import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiDiplomacyAssets;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.country.CountryDisplays;
import dev.hoi.client.ui.country.CountryFlags;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.country.CountryView;
import java.util.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class DiplomacyCountryList {
    enum Sort { NAME, OURS, THEIRS }
    record Cell(CountryView.Filter filter, int column, int row) {}
    private static final int FILTER_ROWS = 4, POSITIVE = 0xFF009F03, ZERO = 0xFFFFBD00, NEGATIVE = 0xFFFF3232, TEXT = 0xFFFFFFFF;
    private final UiDiplomacyAssets assets;
    private final Set<String> selected = new LinkedHashSet<>();
    private Sort sort = Sort.NAME;
    private boolean descending, filterDragging, listDragging;
    private int filterScroll, listScroll, pane, top, height;
    private float s;
    DiplomacyCountryList(UiDiplomacyAssets assets) { this.assets = assets; }
    void layout(int pane, int top, int height) { this.pane = pane; this.top = top; this.height = height; s = pane / 550f; }
    int filterScroll() { return filterScroll; }
    private float x(float value) { return value * s; }
    private float y(float value) { return top + value * s; }
    private int filterTop() { return Math.round(y(40)); }
    private int filterBottom() { return Math.round(y(40 + 35 * FILTER_ROWS + 33)); }
    private int filterViewTop() { return Math.round(y(54)); }
    private int filterViewBottom() { return Math.round(y(54 + 35 * FILTER_ROWS)); }
    private int headerTop() { return filterBottom(); }
    private int listTop() { return Math.round(headerTop() + x(30)); }
    private int listBottom() { return height - Math.max(1, Math.round(x(5))); }
    private int rowsTop() { return Math.round(listTop() + x(8)); }
    private int rowsBottom() { return listBottom() - Math.max(1, Math.round(x(8))); }
    private float rowPitch() { return x(45); }
    private float rowHeight() { return x(44); }
    List<Cell> cells(CountryView view) {
        var cells = new ArrayList<Cell>();
        if (view.list() == null) return cells;
        int row = 0, column = 0; String group = null;
        for (var filter : view.list().filters()) {
            if (group != null && !group.equals(filter.group()) && column > 0 || column == 4) { row++; column = 0; }
            cells.add(new Cell(filter, column++, row)); group = filter.group();
        }
        return cells;
    }
    private float filterContent(CountryView view) {
        var cells = cells(view);
        return cells.isEmpty() ? 0 : (cells.getLast().row() + 1) * 35 + 1;
    }
    private UiScrollBar filterBar(CountryView view) {
        int visible = filterViewBottom() - filterViewTop();
        return new UiScrollBar(Math.round(x(548)) - 10, filterTop() + 2, filterBottom() - 2, visible, Math.round(x(filterContent(view))));
    }
    List<CountryView.Choice> rows(CountryView view) {
        var groups = new HashMap<String, String>();
        if (view.list() != null) for (var filter : view.list().filters()) groups.put(filter.id(), filter.group());
        selected.retainAll(groups.keySet());
        var rows = new ArrayList<CountryView.Choice>();
        for (var row : view.countries()) if (row.listing() != null && matches(row.listing().filters(), groups)) rows.add(row);
        Comparator<CountryView.Choice> order = switch (sort) {
            case NAME -> Comparator.comparing(row -> CountryDisplays.name(row.tag(), row.name()));
            case OURS -> Comparator.comparing(row -> row.listing().ours(), Comparator.nullsFirst(Comparator.naturalOrder()));
            case THEIRS -> Comparator.comparing(row -> row.listing().theirs(), Comparator.nullsFirst(Comparator.naturalOrder()));
        };
        if (sort != Sort.NAME) order = order.reversed();
        rows.sort(descending ? order.reversed() : order);
        return rows;
    }
    private boolean matches(List<String> filters, Map<String, String> groups) {
        for (String group : CountryView.Filter.GROUPS) {
            var wanted = selected.stream().filter(id -> group.equals(groups.get(id))).toList();
            if (!wanted.isEmpty() && wanted.stream().noneMatch(filters::contains)) return false;
        }
        return true;
    }
    private UiScrollBar listBar(CountryView view) {
        int visible = rowsBottom() - rowsTop();
        return new UiScrollBar(Math.round(x(545)) - 10, listTop() + 2, listBottom() - 2, visible, Math.round(x(5) + rows(view).size() * rowPitch()));
    }
    void draw(GuiGraphicsExtractor g, Font font, CountryView view, int mx, int my) {
        assets.tile(g, "occupation/header_frame", Math.round(x(3)), filterTop(), Math.round(x(545)), filterBottom() - filterTop(), 64, 64, s);
        var filterBar = filterBar(view);
        filterScroll = Math.clamp(filterScroll, 0, filterBar.maximum());
        g.enableScissor(Math.round(x(3)), filterViewTop(), filterBar.x(), filterViewBottom());
        float contentTop = filterViewTop() - filterScroll;
        UiAssets.drawExact(g, "diplomacy/list/filter_area", x(18), contentTop, x(521), x(136));
        float scale = UiText.scale(font);
        for (var cell : cells(view)) {
            float cx = x(108 + cell.column() * 105), cy = contentTop + x(1 + cell.row() * 35);
            boolean on = selected.contains(cell.filter().id());
            UiAssets.drawExact(g, "tactic/ui/filter/frame_" + (on ? 0 : 1), cx, cy, x(100), x(29));
            String label = fit(font, cell.filter().name(), x(100) - 2, scale);
            UiText.marquee(g, font, label, cx, UiText.centeredY(font, label, cy, x(29), scale), x(100), scale, TEXT, true, 0);
        }
        g.disableScissor();
        filterBar.draw(g, filterScroll);
        int header = headerTop();
        UiAssets.drawExact(g, "diplomacy/list/binding", x(3), header, x(533), x(35));
        UiAssets.drawExact(g, "diplomacy/list/sort", x(13), header + x(2), x(202), x(29));
        String label = view.list() == null ? "" : view.list().texts().getOrDefault("COUNTRYLIST_LABEL", "");
        UiText.marquee(g, font, label, x(13), UiText.centeredY(font, label, header + x(2), x(29), scale), x(202), scale, TEXT, true, 0);
        String flag = "country/" + view.viewer().toLowerCase(Locale.ROOT) + "/flag";
        CountryFlags.draw(g, flag, "small", x(248), header + x(4), x(20), x(17));
        CountryFlags.draw(g, flag, "small", x(310), header + x(4), x(20), x(17));
        UiAssets.drawExact(g, "commander/commands/opinion_arrow_right/frame_0", x(274), header + x(8), x(17), x(17));
        UiAssets.drawExact(g, "diplomacy/list/arrow_left", x(335), header + x(8), x(17), x(17));
        assets.tile(g, "commander/commands/tiled_window_1b_thin_border/frame_0", 0, listTop(), Math.round(x(545)), listBottom() - listTop(), 64, 64, s);
        var listBar = listBar(view);
        listScroll = Math.clamp(listScroll, 0, listBar.maximum());
        var rows = rows(view);
        g.enableScissor(0, rowsTop(), listBar.x(), rowsBottom());
        for (int i = 0; i < rows.size(); i++) {
            float rowY = rowsTop() + x(5) + i * rowPitch() - listScroll;
            if (rowY + rowHeight() < rowsTop() || rowY > rowsBottom()) continue;
            drawRow(g, font, rows.get(i), rowY, scale);
        }
        g.disableScissor();
        listBar.draw(g, listScroll);
        String tooltip = tooltip(view, mx, my);
        if (!tooltip.isEmpty()) Tooltips.drawExact(g, font, tooltip, mx, my);
    }
    private void drawRow(GuiGraphicsExtractor g, Font font, CountryView.Choice row, float rowY, float scale) {
        float left = x(12);
        UiAssets.drawExact(g, "diplomacy/list/entry", left, rowY, x(505), rowHeight());
        CountryFlags.draw(g, "country/" + row.tag().toLowerCase(Locale.ROOT) + "/flag", "small2", left + x(9), rowY + x(10), x(41), x(26));
        UiAssets.drawExact(g, "map_detail/flag_frame", left + x(5), rowY + x(6), x(52), x(38));
        float lineHeight = font.lineHeight * scale;
        int lines = Math.max(1, (int) Math.floor((rowHeight() - x(6)) / lineHeight));
        float nameWidth = Math.min(x(174), left + x(256) - font.width("-100") * scale / 2 - 1 - (left + x(56)));
        var wrapped = nameLines(font, CountryDisplays.name(row.tag(), row.name()), nameWidth, scale, Math.min(2, lines));
        for (int line = 0; line < wrapped.size(); line++)
            UiText.marquee(g, font, wrapped.get(line), left + x(56), rowY + x(6) + line * lineHeight, nameWidth, scale, TEXT, true, -1);
        var listing = row.listing();
        if (listing.ours() == null) return;
        int[] values = {listing.ours(), listing.theirs()};
        for (int i = 0; i < 2; i++) {
            float box = left + x(i == 0 ? 238 : 300);
            UiAssets.drawExact(g, "diplomacy/list/opinion", box, rowY + x(9), x(36), x(29));
            String value = (values[i] > 0 ? "+" : "") + values[i];
            float width = font.width(value) * scale;
            UiText.marquee(g, font, value, box + x(18) - width / 2, UiText.centeredY(font, value, rowY + x(9), x(29), scale), width + 1, scale,
                    values[i] > 0 ? POSITIVE : values[i] < 0 ? NEGATIVE : ZERO, true, -1);
        }
        var marks = listing.marks();
        for (int i = 0; i < marks.size(); i++) {
            int[] size = UiAssets.imageSize(marks.get(i).icon());
            UiAssets.drawExact(g, marks.get(i).icon(), markLeft(left, i, marks.size()) + x(1), rowY + x(9), x(size[0]), x(size[1]));
        }
    }
    private float markLeft(float left, int index, int count) {
        float step = count <= 1 ? 0 : Math.min(35, (80 - 34) / (float) (count - 1));
        return left + x(420 + index * step);
    }
    static List<String> nameLines(Font font, String name, float width, float scale, int lines) {
        var result = new ArrayList<String>();
        String rest = name.strip();
        while (!rest.isEmpty() && result.size() < lines) {
            if (font.width(rest) * scale <= width) { result.add(rest); rest = ""; break; }
            if (result.size() == lines - 1) { result.add(fit(font, rest, width, scale)); rest = ""; break; }
            int end = rest.length();
            while (end > 1 && font.width(rest.substring(0, end)) * scale > width) end--;
            int space = rest.lastIndexOf(' ', end);
            if (space > 0 && space < end) end = space;
            result.add(rest.substring(0, end).stripTrailing());
            rest = rest.substring(end).stripLeading();
        }
        return result;
    }
    static String fit(Font font, String text, float width, float scale) {
        if (font.width(text) * scale <= width) return text;
        for (int end = text.length() - 1; end > 0; end--) {
            String candidate = text.substring(0, end).stripTrailing() + "...";
            if (font.width(candidate) * scale <= width) return candidate;
        }
        return "...";
    }
    private CountryView.Choice rowAt(CountryView view, double mx, double my) {
        if (mx < x(12) || mx >= x(12 + 500) || mx >= listBar(view).x() || my < rowsTop() || my >= rowsBottom()) return null;
        var rows = rows(view);
        int index = (int) Math.floor((my - rowsTop() - x(5) + listScroll) / rowPitch());
        if (index < 0 || index >= rows.size()) return null;
        float rowY = rowsTop() + x(5) + index * rowPitch() - listScroll;
        return my < rowY + rowHeight() ? rows.get(index) : null;
    }
    private float rowTop(CountryView view, CountryView.Choice row) {
        return rowsTop() + x(5) + rows(view).indexOf(row) * rowPitch() - listScroll;
    }
    private Cell cellAt(CountryView view, double mx, double my) {
        if (my < filterViewTop() || my >= filterViewBottom() || mx >= filterBar(view).x()) return null;
        float contentTop = filterViewTop() - filterScroll;
        for (var cell : cells(view)) {
            float cx = x(108 + cell.column() * 105), cy = contentTop + x(1 + cell.row() * 35);
            if (mx >= cx && mx < cx + x(100) && my >= cy && my < cy + x(29)) return cell;
        }
        return null;
    }
    private Sort sortAt(double mx, double my) {
        int header = headerTop();
        if (my >= header + x(2) && my < header + x(31) && mx >= x(13) && mx < x(215)) return Sort.NAME;
        if (my >= header + x(2) && my < header + x(25) && mx >= x(245) && mx < x(271)) return Sort.OURS;
        if (my >= header + x(2) && my < header + x(25) && mx >= x(307) && mx < x(333)) return Sort.THEIRS;
        return null;
    }
    String tooltip(CountryView view, double mx, double my) {
        if (view.list() == null) return "";
        var sortKey = sortAt(mx, my);
        if (sortKey != null) return view.list().texts().getOrDefault(switch (sortKey) {
            case NAME -> "DIPLOMACY_SORT_ALPHABETICAL";
            case OURS -> "DIPLOMACY_SORT_OPINION_OURS";
            case THEIRS -> "DIPLOMACY_SORT_OPINION_THEIRS";
        }, "");
        var row = rowAt(view, mx, my);
        if (row == null) return "";
        var listing = row.listing();
        float left = x(12), rowY = rowTop(view, row);
        if (listing.ours() != null) for (int i = 0; i < 2; i++) {
            float box = left + x(i == 0 ? 238 : 300);
            if (mx >= box && mx < box + x(36) && my >= rowY + x(9) && my < rowY + x(38)) return i == 0 ? listing.oursDetail() : listing.theirsDetail();
        }
        var marks = listing.marks();
        for (int i = marks.size() - 1; i >= 0; i--) {
            float markX = markLeft(left, i, marks.size());
            if (mx >= markX && mx < markX + x(34) && my >= rowY + x(8) && my < rowY + x(42)) return marks.get(i).detail();
        }
        return listing.open();
    }
    String click(CountryView view, double mx, double my) {
        var filterBar = filterBar(view);
        if (filterBar.contains(mx, my)) {
            filterScroll = filterBar.arrow(my) ? filterBar.step(my, filterScroll, Math.round(x(35))) : filterBar.offset(my);
            filterDragging = !filterBar.arrow(my);
            return "";
        }
        var listBar = listBar(view);
        if (listBar.contains(mx, my)) {
            listScroll = listBar.arrow(my) ? listBar.step(my, listScroll, Math.round(rowPitch())) : listBar.offset(my);
            listDragging = !listBar.arrow(my);
            return "";
        }
        var cell = cellAt(view, mx, my);
        if (cell != null) {
            if (!selected.remove(cell.filter().id())) selected.add(cell.filter().id());
            listScroll = 0;
            AudioUiSounds.play("ui.scroll");
            return "";
        }
        var sortKey = sortAt(mx, my);
        if (sortKey != null) {
            descending = sortKey == sort && !descending;
            sort = sortKey;
            AudioUiSounds.play("ui.click");
            return "";
        }
        var row = rowAt(view, mx, my);
        if (row == null) return null;
        AudioUiSounds.play("ui.diplomacy.select");
        return row.tag();
    }
    boolean drag(CountryView view, double my) {
        if (filterDragging) { filterScroll = filterBar(view).offset(my); return true; }
        if (listDragging) { listScroll = listBar(view).offset(my); return true; }
        return false;
    }
    boolean release() {
        boolean dragging = filterDragging || listDragging;
        filterDragging = listDragging = false;
        return dragging;
    }
    boolean scroll(CountryView view, double mx, double my, double amount) {
        if (mx < 0 || mx >= pane) return false;
        if (my >= filterTop() && my < filterBottom()) {
            filterScroll = Math.clamp(filterScroll - Math.round(amount * x(35)), 0, filterBar(view).maximum());
            return true;
        }
        if (my >= listTop() && my < listBottom()) {
            listScroll = Math.clamp(listScroll - Math.round(amount * rowPitch()), 0, listBar(view).maximum());
            return true;
        }
        return false;
    }
    void reset() { listScroll = 0; }
}
