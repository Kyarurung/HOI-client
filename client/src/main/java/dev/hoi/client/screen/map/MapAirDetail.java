package dev.hoi.client.screen.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.dialog.DialogScreen;
import dev.hoi.client.ui.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.politics.*;
import dev.hoi.client.ui.tooltip.*;
import dev.hoi.protocol.dialog.DialogView;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class MapAirDetail extends DialogScreen implements InputSidebarMovement.InputMovementScreen {
    private static final String CHOICE = "air/", ART = "commander/commands/";
    private static final int WIDTH = 552, HEIGHT = 436, ORIGIN_X = 4, ORIGIN_Y = 5, DETAILS_X = 540, DETAILS_Y = -6, DETAILS_WIDTH = 513, DETAILS_HEIGHT = 495;
    private static final int CHART_X = 54, CHART_Y = 25, CHART_WIDTH = 380, CHART_HEIGHT = 150, CHANNELS = 12, CHANNEL_ROW = 22, BLACK = 0xFF000000;
    private static final List<String> SPANS = List.of("DAY", "MONTH", "YEAR");
    private static final int[] SPAN_X = {128, 218, 308};
    private static final String[] GROUPS = {"FIGHTER_SUPPORT", "NAVAL", "BOMBING_LOGISTICS"};
    private record Hover(String tooltip, float x, float y, float w, float h) {}
    private final List<Hover> hovers = new ArrayList<>();
    private final Set<Integer> channels = new HashSet<>();
    private UiMenuStyle.Frame frame = UiMenuStyle.SCREEN, details = UiMenuStyle.SCREEN;
    private int left, top, pane, tall, dragX, dragY, centered, tooltipScroll;
    private double scale;
    private boolean dragging;
    private String hovered = "";
    public MapAirDetail(DialogView view) { super(view); }
    MapAirDetail(DialogView view, java.util.function.BiConsumer<DialogView, String> send) { super(view, send); }
    @Override public int panelLeft() { return left; }
    @Override public int panelTop() { return top; }
    @Override public int panelWidth() { return pane; }
    @Override public int panelHeight() { return tall; }
    UiMenuStyle.Frame frame() { return frame; }
    UiMenuStyle.Frame details() { return details; }
    boolean detailsOpen() { return on("details"); }
    Set<Integer> channels() { return Set.copyOf(channels); }
    String tooltipText() { return hovered; }
    @Override protected void init() {
        boolean open = detailsOpen();
        int header = HudMenuBar.height(width) + 2;
        scale = Math.min(Math.max(.75, width * 1.5 / 2560), (height - header - 6) / (double)(open ? DETAILS_HEIGHT + 2 : HEIGHT));
        scale = Math.min(scale, (width - 8) / (double)WIDTH);
        pane = size(WIDTH); tall = size(HEIGHT);
        int wide = open ? size(ORIGIN_X + DETAILS_X + DETAILS_WIDTH) : pane, high = open ? size(ORIGIN_Y + DETAILS_Y + DETAILS_HEIGHT) : tall;
        left = Math.clamp((width - pane) / 2 + dragX, 0, Math.max(0, width - Math.min(width, wide)));
        centered = Math.max(header, (height - high) / 2) + dragY;
        top = Math.clamp(centered, header, Math.max(header, height - high));
        frame = new UiMenuStyle.Frame((float)(left + ORIGIN_X * scale), (float)(top + ORIGIN_Y * scale), (float)scale);
        details = new UiMenuStyle.Frame(Math.min(frame.x(DETAILS_X), width - (float)(DETAILS_WIDTH * scale)), frame.y(DETAILS_Y), (float)scale);
        addRenderableWidget(UiMenuStyle.close("raids/gfx_closebutton_small/frame_0", frame.x(511), frame.y(1), frame.size(26), frame.size(26), this::onClose));
        button(frame, 202, 112, 140, 29, "ui.checkbox", () -> choose(CHOICE + "details"));
        button(frame, 10, 227, 140, 29, "ui.checkbox", () -> choose(CHOICE + "missions/own"));
        button(frame, 394, 227, 140, 29, "ui.checkbox", () -> choose(CHOICE + "missions/enemy"));
        for (int i = 0; i < GROUPS.length; i++) {
            String group = GROUPS[i];
            button(frame, 205 + 45 * i, 223, 45, 34, "ui.air.filter", () -> choose(CHOICE + (UiPriorityControls.controlled() ? "all" : (UiPriorityControls.shifted() ? "only/" : "group/") + group)));
        }
        if (open) {
            for (int i = 0; i < SPANS.size(); i++) {
                String span = SPANS.get(i);
                button(details, SPAN_X[i], 200, 76, 31, "ui.click", () -> choose(CHOICE + "span/" + span));
            }
            for (int i = 0; i < tiles("chart_channel").size(); i++) {
                int channel = i;
                button(details, channelX(i), channelY(i), 247, 21, "ui.checkbox", () -> { if (!channels.remove(channel)) channels.add(channel); });
            }
        }
        if (dev.hoi.client.ClientCampaignHud.visible()) HudMenuBar.buttons(width, dev.hoi.client.ClientCampaignHud.country(), null, dev.hoi.client.HoiClient::openMenu).forEach(this::addRenderableWidget);
    }
    private void button(UiMenuStyle.Frame area, double px, double py, double pw, double ph, String sound, Runnable action) {
        var button = new UiMenuButton("", area.ix(px), area.iy(py), area.len(pw), area.len(ph), action);
        button.background("none").caption("").sound(sound);
        button.setTooltip(null);
        addRenderableWidget(button);
    }
    private int size(double value) { return (int)Math.round(value * scale); }
    private List<DialogView.Tile> tiles(String section) { return view().tiles().stream().filter(tile -> tile.section().equals(section)).toList(); }
    private DialogView.Tile tile(String section) { return tiles(section).stream().findFirst().orElse(null); }
    private String name(String section) { var tile = tile(section); return tile == null ? "" : tile.name(); }
    private boolean on(String section) { var tile = tile(section); return tile != null && tile.value().equals("1"); }
    private static int channelX(int channel) { return channel < CHANNELS ? 10 : 256; }
    private static int channelY(int channel) { return 235 + CHANNEL_ROW * (channel % CHANNELS); }
    private static int channelColor(int channel) {
        int step = channel % CHANNELS;
        return channel < CHANNELS ? color(1, .05f * step, 0) : color(1 - .05f * step, 1, 0);
    }
    private static int color(float red, float green, float blue) { return BLACK | Math.round(red * 255) << 16 | Math.round(green * 255) << 8 | Math.round(blue * 255); }
    private void text(GuiGraphicsExtractor g, UiMenuStyle.Frame area, String value, double cx, double cy) { text(g, area, value, cx, cy, 1, 0); }
    private float label(double source) { return Math.max(1, (float)Math.ceil(source * scale / font.lineHeight * 2 - .01) / 2); }
    private void text(GuiGraphicsExtractor g, UiMenuStyle.Frame area, String value, double cx, double cy, float factor, double limit) {
        if (value == null || value.isEmpty()) return;
        var line = Tooltips.sentences(Component.literal(value)).getVisualOrderText();
        if (limit > 0 && font.width(line) * factor > area.len(limit)) { UiText.marquee(g, font, line, area.ix(cx - limit / 2), area.y(cy) - 4 * factor, area.len(limit), factor, UiMenuStyle.TEXT, true, -1); return; }
        g.pose().pushMatrix();
        g.pose().translate(area.x(cx) - font.width(line) * factor / 2, area.y(cy) - 4 * factor);
        g.pose().scale(factor);
        g.text(font, line, 0, 0, UiMenuStyle.TEXT);
        g.pose().popMatrix();
    }
    private void edge(GuiGraphicsExtractor g, String value, double px, double cy, boolean right) {
        var line = Tooltips.sentences(Component.literal(value)).getVisualOrderText();
        g.pose().pushMatrix();
        g.pose().translate(details.x(px) - (right ? font.width(line) : 0), details.y(cy) - 4);
        g.text(font, line, 0, 0, UiMenuStyle.TEXT);
        g.pose().popMatrix();
    }
    private void hover(DialogView.Tile tile, UiMenuStyle.Frame area, double px, double py, double pw, double ph) {
        if (tile != null) hovers.add(new Hover(tile.detail().isBlank() ? tile.name() : tile.detail(), area.x(px), area.y(py), area.size(pw), area.size(ph)));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        hovers.clear();
        if (!DialogCoordinator.renderingContent()) dev.hoi.client.ClientCampaignHud.draw(g);
        frame.art(g, "air_detail/background", -ORIGIN_X, -ORIGIN_Y, WIDTH, HEIGHT);
        hover(tile("fighter_area"), frame, 10, 151, 525, 80);
        hover(tile("mission_area"), frame, 10, 255, 525, 100);
        presence(g);
        weather(g);
        indicator(g, "detection", 14, 11, 112, 62);
        indicator(g, "efficiency", 90, 90, 114, 134);
        indicator(g, "enemy_efficiency", 383, 430, 114, 411);
        indicator(g, "enemy_detection", 459, 499, 112, 483);
        toggle(g, "details", 202, 112, false);
        text(g, frame, name("own_label"), 31, 161, label(18), 0);
        text(g, frame, name("enemy_label"), 504, 161, label(18), 0);
        fighters(g, "own", 11, 130, 170);
        fighters(g, "enemy", 294, 420, 300);
        toggle(g, "missions_own", 10, 227, true);
        toggle(g, "missions_enemy", 394, 227, true);
        for (int i = 0; i < GROUPS.length; i++) {
            String section = "mission_filter_" + i;
            frame.art(g, "air_detail/category_bg_" + (on(section) ? 0 : 1), 205 + 45 * i, 223);
            frame.art(g, "air_detail/category_" + i, 205 + 47 * i, 226);
            hover(tile(section), frame, 205 + 45 * i, 223, 45, 34);
        }
        missions(g, "own", 11, 100, 170, false);
        missions(g, "enemy", 352, 445, 300, true);
        g.nextStratum();
        text(g, frame, view().title(), 267, 14, label(16), 420);
        if (detailsOpen()) chart(g);
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable renderable) renderable.extractRenderState(g, mx, my, delta);
        tooltip(g, mx, my);
    }
    private void presence(GuiGraphicsExtractor g) {
        for (boolean enemy : new boolean[]{false, true}) {
            var entries = tiles(enemy ? "enemy" : "own");
            int[] ix = enemy ? new int[]{500, 500, 428, 429} : new int[]{8, 8, 79, 79}, iy = enemy ? new int[]{34, 70, 34, 71} : new int[]{32, 70, 34, 71};
            int[] cx = enemy ? new int[]{484, 484, 413, 413} : new int[]{56, 56, 127, 127}, cy = {47, 82, 47, 82};
            for (int i = 0; i < Math.min(4, entries.size()); i++) {
                frame.art(g, entries.get(i).icon(), ix[i], iy[i]);
                text(g, frame, entries.get(i).value(), cx[i], cy[i]);
                hover(entries.get(i), frame, Math.min(ix[i], cx[i] - 16), iy[i], 66, 24);
            }
        }
        for (var friendly : tiles("friendly")) {
            int type = Integer.parseInt(friendly.value()), px = type < 2 ? 8 : 79, py = type % 2 == 0 ? 33 : 69;
            frame.art(g, friendly.icon(), px, py, 22 * .7, 22 * .7);
            hover(friendly, frame, px, py, 16, 16);
        }
    }
    private void weather(GuiGraphicsExtractor g) {
        var weather = tile("weather");
        if (weather != null) { frame.art(g, weather.icon(), 147, 30); hover(weather, frame, 147, 30, 248, 47); }
        frame.art(g, "air_detail/air_weather_frame", 147, 30);
        var balance = tile("balance");
        if (balance != null) {
            boolean disabled = balance.icon().endsWith("disabled");
            frame.art(g, disabled ? balance.icon() : "air_detail/progress_bad", 147, 80);
            if (!disabled && balance.gauge() != null && balance.gauge().fraction() > 0) {
                g.enableScissor(frame.ix(147), frame.iy(80), frame.ix(147 + 250 * balance.gauge().fraction()), frame.iy(94));
                frame.art(g, "air_detail/progress_good", 147, 80);
                g.disableScissor();
            }
            hover(balance, frame, 144, 78, 256, 20);
        }
        frame.art(g, "air_detail/air_progress_frame", 144, 78);
    }
    private void indicator(GuiGraphicsExtractor g, String section, int bgX, int iconX, int iconY, int cx) {
        var tile = tile(section);
        if (tile == null) return;
        frame.art(g, "air_detail/mini_bg", bgX, 115);
        frame.art(g, tile.icon(), iconX, iconY);
        text(g, frame, tile.value(), cx, 126);
        hover(tile, frame, Math.min(bgX, iconX), iconY, 72, 28);
    }
    private void toggle(GuiGraphicsExtractor g, String section, int px, int py, boolean selectable) {
        var tile = tile(section);
        if (tile == null) return;
        frame.art(g, ART + "sort_button_140x29/frame_" + (selectable && on(section) ? 1 : 0), px, py);
        text(g, frame, tile.name(), px + 70, py + 14.5, label(14), 136);
        hover(tile, frame, px, py, 140, 29);
    }
    private void picture(GuiGraphicsExtractor g, DialogView.Tile tile, double px, double py, double pw, double ph) { frame.art(g, tile.icon(), px, py, pw, ph); }
    private void damage(GuiGraphicsExtractor g, String section, int px, int py, int row) {
        var rows = tiles(section);
        for (int i = 0; i < rows.size(); i++) {
            int y = py + row * i;
            frame.art(g, rows.get(i).icon(), px + 3, y + 3);
            text(g, frame, rows.get(i).value(), px + 55, y + 14);
            hover(rows.get(i), frame, px, y, 75, row);
        }
    }
    private void fighters(GuiGraphicsExtractor g, String side, int px, int cx, int list) {
        var count = tile(side + "_fighters");
        if (count == null) return;
        text(g, frame, count.value(), cx, 162);
        hover(count, frame, cx - 90, 155, 180, 14);
        for (var art : tiles(side + "_fighter_art")) for (int copy = 0; copy < 2; copy++) picture(g, art, px + 122 * copy, 176, 181 * .65, 65 * .65);
        damage(g, side + "_fighter_damage", list, 177, 25);
    }
    private void missions(GuiGraphicsExtractor g, String side, int px, int cx, int list, boolean right) {
        var count = tile(side + "_planes");
        if (count == null) return;
        text(g, frame, count.value(), cx, 277);
        hover(count, frame, cx - 90, 270, 180, 14);
        var art = tiles(side + "_plane_art");
        for (int i = 0; i < Math.min(3, art.size()); i++) {
            if (i == 0) picture(g, art.get(i), px, 288, 181, 65);
            else picture(g, art.get(i), px + (i == 1 ? (right ? 91 : 0) : right ? 0 : 91), 358, 90.5, 32.5);
        }
        damage(g, side + "_damage", list, 265, right ? 23 : 25);
    }
    private static double[] values(DialogView.Tile tile) { return Arrays.stream(tile.detail().split(",")).mapToDouble(Double::parseDouble).toArray(); }
    private void chart(GuiGraphicsExtractor g) {
        details.tile(g, ART + "tiled_bg/frame_0", 0, 0, DETAILS_WIDTH, DETAILS_HEIGHT, 64, UiMenuStyle.TILE);
        details.tile(g, ART + "tiled_plain_bg/frame_0", 12, 13, 489, 225, 64, UiMenuStyle.TILE);
        details.art(g, ART + "intel_ledger_chart_grid_bg/frame_0", CHART_X, CHART_Y, CHART_WIDTH, CHART_HEIGHT);
        var entries = tiles("chart_channel");
        double max = 0;
        for (int i = 0; i < entries.size(); i++) if (channels.contains(i)) max = Math.max(max, Arrays.stream(values(entries.get(i))).max().orElse(0));
        if (max > 0) for (int i = 0; i < entries.size(); i++) if (channels.contains(i)) line(g, values(entries.get(i)), max * 1.05, channelColor(i));
        if (max > 0) { edge(g, decimal(max), 439, 32, false); edge(g, decimal(max / 2), 439, 96, false); }
        edge(g, decimal(0), 439, 164, false);
        var axis = tile("chart_axis");
        if (axis != null) {
            edge(g, "0", 54, 182, false);
            edge(g, axis.value(), 434, 182, true);
            text(g, details, axis.name(), 245, 182);
        }
        var spans = tiles("chart_span");
        for (int i = 0; i < Math.min(SPANS.size(), spans.size()); i++) {
            details.art(g, ART + "chart_history_level_button/frame_" + (spans.get(i).value().equals("1") ? 1 : 0), SPAN_X[i], 200);
            text(g, details, spans.get(i).name(), SPAN_X[i] + 38, 215.5);
        }
        for (int i = 0; i < entries.size(); i++) {
            boolean enemy = i >= CHANNELS, shown = channels.contains(i);
            int px = channelX(i), py = channelY(i);
            details.art(g, ART + "generic_entry_bg_small/frame_" + (shown ? 1 : 0), px + (enemy ? 0 : 25), py);
            text(g, details, entries.get(i).name(), px + (enemy ? 0 : 25) + 111, py + 12, 1, 214);
            details.art(g, ART + "pol_party_colour_bg/frame_0", px + (enemy ? 221 : 5), py + 1);
            UiAssets.drawExact(g, ART + "pol_party_colour/frame_0", details.x(px + (enemy ? 225 : 9)), details.y(py + 5), details.size(13), details.size(13), shown ? channelColor(i) : BLACK);
        }
    }
    private static String decimal(double value) { return new java.math.BigDecimal(value).setScale(5, java.math.RoundingMode.HALF_UP).setScale(1, java.math.RoundingMode.DOWN).toPlainString(); }
    private void line(GuiGraphicsExtractor g, double[] points, double top, int color) {
        int x = details.ix(CHART_X), y = details.iy(CHART_Y), w = details.len(CHART_WIDTH), h = details.len(CHART_HEIGHT), before = -1;
        for (int column = 0; column < w; column++) {
            double at = points.length < 2 ? 0 : (points.length - 1.0) * column / Math.max(1, w - 1);
            int index = (int)Math.min(at, points.length - 1);
            double value = points[index] + (index + 1 < points.length ? (points[index + 1] - points[index]) * (at - index) : 0);
            int row = y + (int)Math.round((h - 1) * (1 - Math.clamp(value / top, 0, 1)));
            g.fill(x + column, before < 0 ? row : Math.min(row, before), x + column + 1, (before < 0 ? row : Math.max(row, before)) + 1, color);
            before = row;
        }
    }
    private void tooltip(GuiGraphicsExtractor g, int mx, int my) {
        String current = hovers.reversed().stream().filter(area -> mx >= area.x() && mx < area.x() + area.w() && my >= area.y() && my < area.y() + area.h()).map(Hover::tooltip).findFirst().orElse("");
        if (detailsOpen() && mx >= details.x(0) && my >= details.y(0) && mx < details.x(DETAILS_WIDTH) && my < details.y(DETAILS_HEIGHT)) current = "";
        if (!current.equals(hovered)) tooltipScroll = 0;
        hovered = current;
        if (current.isEmpty()) return;
        float factor = (float)Math.max(12, 24 * Math.min(Math.max(.5, width / 2560.0), scale)) / font.lineHeight;
        g.pose().pushMatrix();
        g.pose().scale(factor);
        tooltipScroll = PoliticsTooltip.drawTextDetail(g, font, current, (int)(mx / factor), (int)(my / factor), (int)(width / factor), (int)(height / factor), tooltipScroll);
        g.pose().popMatrix();
    }
    private boolean inside(double x, double y) {
        return x >= left && x < left + pane && y >= top && y < top + tall
                || detailsOpen() && x >= details.x(0) && x < details.x(DETAILS_WIDTH) && y >= details.y(0) && y < details.y(DETAILS_HEIGHT);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (!inside(event.x(), event.y())) {
            if (event.y() < HudMenuBar.height(width)) return super.mouseClicked(event, twice);
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) return dev.hoi.client.input.InputMapInfo.clickAt(minecraft, event.x(), event.y(), width, height);
        }
        dragging = !super.mouseClicked(event, twice) && event.button() == InputConstants.MOUSE_BUTTON_LEFT;
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        dragX += (int)Math.round(dx); dragY += (int)Math.round(dy);
        rebuildWidgets();
        dragX = left - (width - pane) / 2; dragY += top - centered;
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (hovered.isEmpty()) return false;
        tooltipScroll -= (int)vertical;
        return true;
    }
    @Override public boolean allowsMovement() { return true; }
    @Override public int unitHudLeft() { return 0; }
    @Override public boolean keyPressed(KeyEvent event) { return InputSidebarMovement.consumes(minecraft, event) || super.keyPressed(event); }
    @Override public boolean keyReleased(KeyEvent event) { return InputSidebarMovement.consumes(minecraft, event) || super.keyReleased(event); }
    @Override public void removed() { InputSidebarMovement.release(minecraft); }
}
