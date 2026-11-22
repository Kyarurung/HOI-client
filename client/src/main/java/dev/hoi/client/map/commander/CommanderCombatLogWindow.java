package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.network.NetworkScreens;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.country.CountryFlags;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderHudPayloads.Card;
import dev.hoi.protocol.commander.CommanderRecordPayloads.*;
import dev.hoi.protocol.commander.CommanderRecordPayloads;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public final class CommanderCombatLogWindow {
    static final int WIDTH = 670, PAGE = 50, BODY = 165, SLOT = 105, LOSS = 46, TEMPLATE = 35, MARGIN = 13, INNER = WIDTH - 2 * MARGIN;
    static final String[] TABS = {"COMBAT_LOG_TAB_COMBAT", "COMBAT_LOG_TAB_EQUIPMENT", "COMBAT_LOG_TAB_TEMPLATE"};
    static final int[] TAB_X = {5, 215, 425};
    private static final int BATTLES = 0, OWN = 1, ENEMY = 2, TEMPLATES = 3, ARMY = 33, LOCATION = 140, LOCATION_WIDTH = 328;
    static Consumer<String> designer = NetworkScreens::openDesigner;
    static Consumer<String> country = HoiClient::openCountry;
    record Box(double x, double y, double w, double h) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    private final String theater, token = UUID.randomUUID().toString();
    private final Set<String> excluded = new TreeSet<>();
    private Log log;
    private UiMenuStyle.Frame frame;
    private int height, tab, months, dragging = -1;
    private final int[] scrolls = new int[4];
    private boolean detailed, slider, moving, dirty;
    private long sent;
    private float left = Float.NaN, top;
    private double grabX, grabY;
    CommanderCombatLogWindow(String theater) { this.theater = theater; }
    String theater() { return theater; }
    Log log() { return log; }
    int tab() { return tab; }
    UiMenuStyle.Frame frame() { return frame; }
    double left() { return frame.x(-MARGIN); }
    double right() { return frame.x(WIDTH - MARGIN); }
    double bottom() { return frame.y(height); }
    int firstRow() { return first(); }
    int visibleRows() { return rows(); }
    int enemyTop() { return listTop(ENEMY); }
    int armyCount() { return armies().size(); }
    void open() { AudioUiSounds.play("ui.pop_up"); request(); }
    void request() { dirty = true; flush(); }
    void close() { CommanderCombatLogHud.send(new Request(token, Action.CLOSE_LOG, theater, 0, 0, List.of())); }
    private void flush() {
        long now = System.currentTimeMillis();
        if (!dirty || now - sent < 100) return;
        dirty = false; sent = now;
        CommanderCombatLogHud.send(new Request(token, Action.LOG, theater, months, Math.max(0, first() - 6), List.copyOf(excluded)));
    }
    boolean accept(Log next) {
        if (!next.token().equals(token)) return true;
        log = next;
        if (months == 0) months = next.months();
        for (int list = 0; list < scrolls.length; list++) scrolls[list] = Math.clamp(scrolls[list], 0, maxScroll(list));
        return next.error().isEmpty();
    }
    void place(CommanderCombatLogWindow previous) { left = previous.left; top = previous.top; }
    void layout(int width, int screenHeight, float scale) {
        scale = Math.min(scale, (width - 8f) / WIDTH);
        if (Float.isNaN(left)) { left = width - 995 * scale; top = 95 * scale; }
        top = Math.max(top, HudMenuBar.height(width));
        height = Math.max(BODY + SLOT + 40, (int)((screenHeight - top) / scale) - 165);
        left = Math.clamp(left, 0, Math.max(0, width - WIDTH * scale));
        top = Math.clamp(top, 0, Math.max(0, screenHeight - height * scale));
        frame = new UiMenuStyle.Frame(left + MARGIN * scale, top, scale);
    }
    boolean contains(double x, double y) { return frame != null && gui(new Box(-MARGIN, 0, WIDTH, height)).contains(x, y); }
    private Box gui(Box box) { return new Box(frame.x(box.x()), frame.y(box.y()), frame.size(box.w()), frame.size(box.h())); }
    private String label(String key) { return log == null ? "" : log.label(key); }
    private boolean shown() { return log != null && log.error().isEmpty(); }
    private int bodyHeight() { return height - BODY - MARGIN; }
    private int rows() { return Math.max(1, (bodyHeight() - 10) / SLOT); }
    private int first() { return scrolls[BATTLES]; }
    private int half() { return (bodyHeight() - 75) / 2; }
    private int listTop(int list) { return switch (list) { case BATTLES -> BODY + 10; case OWN -> BODY + 75; case ENEMY -> BODY + 75 + half() + 30; default -> BODY + 45; }; }
    private int listHeight(int list) { return switch (list) { case BATTLES -> rows() * SLOT; case OWN -> half(); case ENEMY -> half() - 30; default -> bodyHeight() - 45; }; }
    private List<Loss> losses(int list) { return list == OWN ? detailed ? log.detailed() : log.losses() : detailed ? log.enemyDetailed() : log.enemy(); }
    private int total(int list) {
        if (!shown()) return 0;
        return switch (list) { case BATTLES -> log.total(); case OWN -> log.losses().isEmpty() ? 0 : losses(OWN).size() * LOSS; case ENEMY -> losses(ENEMY).size() * LOSS; default -> log.templates().size() * TEMPLATE; };
    }
    private int maxScroll(int list) { return Math.max(0, list == BATTLES ? total(list) - rows() : total(list) - listHeight(list)); }
    private UiScrollBar bar(int list) {
        int unit = list == BATTLES ? SLOT : 1, barTop = listTop(list);
        return new UiScrollBar(frame.ix(list == BATTLES ? 620 : 608), frame.iy(barTop), frame.iy(barTop + listHeight(list)), Math.round(frame.size(listHeight(list))), Math.round(frame.size((double)total(list) * unit)));
    }
    private List<Integer> lists() { return tab == 0 ? List.of(BATTLES) : tab == 1 ? List.of(OWN, ENEMY) : List.of(TEMPLATES); }
    private static Box closeBox() { return new Box(INNER - 35, 13, 32, 33); }
    private static Box tabBox(int i) { return new Box(TAB_X[i], PAGE + 85, 210, 34); }
    private static Box filterBox() { return new Box(65, BODY + 41, 202, 29); }
    private static Box trackBox() { return new Box(90, PAGE + 26, 160, 16); }
    private List<Card> armies() {
        var view = CommanderHud.view();
        if (view == null) return List.of();
        var ids = view.theaters().stream().filter(t -> t.id().equals(theater)).flatMap(t -> t.armies().stream()).toList();
        return view.cards().stream().filter(card -> ids.contains(card.id())).sorted(Comparator.comparingInt(card -> ids.indexOf(card.id()))).toList();
    }
    private Box armyBox(int index, int count) { return new Box(340 + index * (count < 2 ? 0 : Math.min(37, (262.0 - ARMY) / (count - 1))), PAGE + 10, ARMY, 32); }
    private Battle battle(int index) {
        int loaded = index - log.offset();
        return loaded < 0 || loaded >= log.battles().size() ? null : log.battles().get(loaded);
    }
    private Box slot(int index) { return new Box(10, listTop(BATTLES) + (index - first()) * SLOT, 610, 100); }
    void draw(GuiGraphicsExtractor g, Font font) {
        if (frame == null) return;
        flush();
        g.nextStratum();
        frame.tile(g, "commander/commands/tiled_plain_bg/frame_0", -MARGIN, 0, WIDTH, height, 64, 192);
        frame.art(g, "commander/log/top", 0, 5);
        frame.art(g, "menu/close", closeBox().x(), closeBox().y());
        UiMenuStyle.title(g, font, frame, label("COMBAT_LOG"), 0, 28, WIDTH, 0, UiMenuStyle.TEXT);
        if (!shown()) return;
        text(g, font, log.period(), 30, PAGE + 4, 280, 20, 0);
        slider(g, font);
        groups(g, font);
        frame.art(g, "tension/casualties", 25, PAGE + 55);
        text(g, font, log.manpower(), 55, PAGE + 55, 280, 25, -1);
        text(g, font, log.ratio(), 340, PAGE + 55, 280, 25, 0);
        frame.tile(g, "agency/operations/gfx_tiled_paper_bg2/frame_0", 0, BODY, INNER, bodyHeight(), 64, 192);
        g.enableScissor(frame.ix(0), frame.iy(BODY), frame.ix(INNER), frame.iy(BODY + bodyHeight()));
        frame.art(g, "commander/log/info_bg", -8, BODY);
        if (tab == 0) battles(g, font); else if (tab == 1) equipment(g, font); else templates(g, font);
        g.disableScissor();
        for (int i = 0; i < TABS.length; i++) {
            frame.art(g, "commander/log/tab/frame_" + (tab == i ? 0 : 1), TAB_X[i], PAGE + 85, 210, 34);
            text(g, font, label(TABS[i]), TAB_X[i], PAGE + 85, 210, 34, 0);
        }
        for (int list : lists()) bar(list).draw(g, Math.round(frame.size((double)scrolls[list] * (list == BATTLES ? SLOT : 1))));
    }
    private void slider(GuiGraphicsExtractor g, Font font) {
        text(g, font, "1", 38, PAGE + 24, 35, 18, 0);
        text(g, font, Integer.toString(log.maximum()), 267, PAGE + 24, 35, 18, 0);
        frame.art(g, "commander/log/scroll_left", 73, PAGE + 26);
        frame.art(g, "commander/log/scroll_right", 251, PAGE + 26);
        var track = trackBox();
        frame.tile(g, "commander/log/track", track.x(), track.y() + 2, (int)track.w(), 12, 4, 12);
        frame.art(g, "commander/commands/scroll_drager/frame_0", track.x() + (track.w() - 16) * (months - 1) / Math.max(1, log.maximum() - 1), track.y());
    }
    private void groups(GuiGraphicsExtractor g, Font font) {
        var armies = armies();
        for (int i = 0; i < armies.size(); i++) {
            var card = armies.get(i); var box = armyBox(i, armies.size());
            int shield = card.panel() == null || card.panel().insignia() == null ? 0 : card.panel().insignia(), tint = card.panel() == null || card.panel().armyColor() == null ? 0xFFFFFFFF : 0xFF000000 | card.panel().armyColor();
            UiAssets.drawExact(g, "commander/commands/theatre_army_shield/frame_" + shield, frame.x(box.x()), frame.y(box.y()), frame.size(33), frame.size(32), tint);
            if (!excluded.contains(card.id())) frame.art(g, "commander/commands/theatre_army_shield_selected/frame_" + shield, box.x() - 1, box.y() - 1, 35, 34);
            if (i == armies.size() - 1 || armyBox(i + 1, armies.size()).x() - box.x() >= 30) text(g, font, Integer.toString(card.count()), box.x() + 8, box.y() + 12, 28, 20, 0);
        }
    }
    private void battles(GuiGraphicsExtractor g, Font font) {
        if (log.total() == 0) { text(g, font, label("COMBAT_LOG_EMPTY_COMBATS"), 30, BODY + 100, 590, 25, 0); return; }
        boolean missing = false;
        for (int i = first(); i < Math.min(log.total(), first() + rows()); i++) {
            var battle = battle(i);
            if (battle == null) { missing = true; continue; }
            var box = slot(i); double x = box.x(), y = box.y();
            frame.art(g, "commander/log/entry", x, y);
            frame.art(g, battle.icon(), x + 100, y + 3);
            frame.art(g, "commander/log/location", x + LOCATION, y + 5, LOCATION_WIDTH, 23);
            text(g, font, battle.location(), x + 149, y + 5, 170, 23, -1);
            text(g, font, battle.date(), x + 320, y + 5, 140, 23, 1);
            frame.art(g, battle.image(), x + 98, y + 31, 413, 70);
            frame.art(g, "commander/log/terrain_overlay", x + 98, y + 31);
            party(g, font, battle.own(), x, y, true);
            party(g, font, battle.enemy(), x, y, false);
        }
        if (missing) request();
    }
    private void party(GuiGraphicsExtractor g, Font font, Party party, double x, double y, boolean own) {
        double flag = x + (own ? 12 : 562), face = x + (own ? 8 : 558), line = x + (own ? 105 : 305);
        CountryFlags.draw(g, HudMenuBar.flagTexture(party.country()), "small2", frame.x(flag), frame.y(y + 6), frame.size(41), frame.size(26));
        frame.art(g, "tension/flag_frame", flag - 5, y + 2);
        frame.art(g, party.portrait().isEmpty() ? "politics/empty/leader" : party.portrait(), face, y + 36, 46.8, 63);
        String[] values = {party.manpower(), party.armor(), party.air(), party.other()};
        for (int i = 0; i < values.length; i++) text(g, font, values[i], line, y + 30 + i * 18, 200, 20, own ? -1 : 1, true);
    }
    private void equipment(GuiGraphicsExtractor g, Font font) {
        frame.art(g, "commander/log/info_header", 10, BODY + 10);
        text(g, font, label("COMBAT_LOG_EQ_LOST"), 30, BODY + 12, 280, 25, 0);
        text(g, font, label("COMBAT_LOG_LOSS_REASON_COMBAT"), 285, BODY + 12, 130, 25, 0);
        text(g, font, label("COMBAT_LOG_LOSS_REASON_ATTRITION"), 390, BODY + 12, 140, 25, 0);
        text(g, font, label("COMBAT_LOG_RECOVERED"), 495, BODY + 12, 140, 25, 0);
        text(g, font, log.lost().combat(), 285, BODY + 43, 130, 25, 0);
        text(g, font, log.lost().attrition(), 390, BODY + 43, 130, 25, 0);
        text(g, font, log.lost().recovered(), 495, BODY + 43, 130, 25, 0);
        frame.art(g, "commander/log/filter/frame_" + (detailed ? 1 : 0), 65, BODY + 41);
        text(g, font, label("COMBAT_LOG_FILTER_BTN"), 65, BODY + 41, 202, 29, 0);
        frame.art(g, "commander/log/info_overlay", 25, BODY + 72);
        frame.art(g, "commander/log/info_overlay", 25, BODY + 75 + half() + 28);
        frame.art(g, "commander/log/info_header", 15, BODY + 75 + half() + 1);
        text(g, font, label("COMBAT_LOG_ENEMY_EQ_LOST"), 30, BODY + 75 + half() + 4, 280, 25, 0);
        text(g, font, log.enemyLost().combat(), 285, BODY + 75 + half() + 4, 130, 25, 0);
        losses(g, font, OWN, "COMBAT_LOG_EMPTY_EQ");
        losses(g, font, ENEMY, "COMBAT_LOG_EMPTY_ENEMY_EQ");
    }
    private Box lossBox(int list, int index) { return new Box(25, listTop(list) + index * LOSS - scrolls[list], 580, LOSS); }
    private void losses(GuiGraphicsExtractor g, Font font, int list, String empty) {
        var rows = losses(list);
        if (rows.isEmpty()) { text(g, font, label(empty), 30, listTop(list) + (list == OWN ? 100 : 70), 590, 25, 0); return; }
        clip(g, list);
        for (int i = 0; i < rows.size(); i++) {
            var box = lossBox(list, i); var row = rows.get(i);
            if (box.y() + LOSS < listTop(list) || box.y() > listTop(list) + listHeight(list)) continue;
            nine(g, box.x(), box.y(), box.w(), box.h());
            int[] size = UiAssets.imageSize(row.icon());
            if (size[0] > 0) { double scale = Math.min(.6, Math.min(110.0 / size[0], 46.0 / size[1])); frame.art(g, row.icon(), box.x() + 55 - size[0] * scale / 2, box.y() + 23 - size[1] * scale / 2, size[0] * scale, size[1] * scale); }
            text(g, font, row.name(), box.x() + 110, box.y() + 10, list == OWN ? 150 : 180, 25, -1);
            text(g, font, row.combat(), box.x() + 260, box.y() + 10, 130, 25, 0);
            if (list == OWN) { text(g, font, row.attrition(), box.x() + 365, box.y() + 10, 130, 25, 0); text(g, font, row.recovered(), box.x() + 470, box.y() + 10, 130, 25, 0); }
        }
        g.disableScissor();
    }
    private Box templateBox(int index) { return new Box(25, listTop(TEMPLATES) + index * TEMPLATE - scrolls[TEMPLATES], 580, TEMPLATE); }
    private void templates(GuiGraphicsExtractor g, Font font) {
        frame.art(g, "commander/log/info_overlay", 24, BODY + 40);
        frame.art(g, "commander/log/info_header", 10, BODY + 10);
        text(g, font, label("COMBAT_LOG_TEMPLATES"), 32, BODY + 12, 210, 25, 0);
        text(g, font, label("COMBAT_LOG_COMBATS_WON"), 325, BODY + 12, 140, 25, 0);
        text(g, font, label("COMBAT_LOG_COMBATS_TOTAL"), 460, BODY + 12, 140, 25, 0);
        if (log.templates().isEmpty()) { text(g, font, label("COMBAT_LOG_EMPTY_TEMPLATES"), 30, BODY + 100, 590, 25, 0); return; }
        clip(g, TEMPLATES);
        for (int i = 0; i < log.templates().size(); i++) {
            var box = templateBox(i); var row = log.templates().get(i);
            if (box.y() + TEMPLATE < listTop(TEMPLATES) || box.y() > listTop(TEMPLATES) + listHeight(TEMPLATES)) continue;
            nine(g, box.x(), box.y(), box.w(), box.h());
            String icon = CommanderPanels.armyUnitIcon(row.icon()); int[] size = UiAssets.imageSize(icon);
            if (size[0] > 0) { double scale = Math.min(.7, Math.min(48.0 / size[0], 31.0 / size[1])); frame.art(g, icon, box.x() + 24 - size[0] * scale / 2, box.y() + 17.5 - size[1] * scale / 2, size[0] * scale, size[1] * scale); }
            text(g, font, row.name(), box.x() + 50, box.y() + 5, 250, 25, -1);
            text(g, font, row.ratio(), box.x() + 305, box.y() + 5, 140, 25, 0);
            text(g, font, row.total(), box.x() + 438, box.y() + 5, 140, 25, 0);
        }
        g.disableScissor();
    }
    private void clip(GuiGraphicsExtractor g, int list) { g.enableScissor(frame.ix(20), frame.iy(listTop(list)), frame.ix(607), frame.iy(listTop(list) + listHeight(list))); }
    private void nine(GuiGraphicsExtractor g, double x, double y, double w, double h) {
        g.pose().pushMatrix();
        g.pose().translate(frame.x(x), frame.y(y));
        g.pose().scale(frame.scale());
        UiAssets.nineSlice(g, "commander/commands/mini_tooltip/frame_0", 0, 0, (int)w, (int)h, 12, 1);
        g.pose().popMatrix();
    }
    private void text(GuiGraphicsExtractor g, Font font, String value, double x, double y, double w, double h, int align) { text(g, font, value, x, y, w, h, align, false); }
    private void text(GuiGraphicsExtractor g, Font font, String value, double x, double y, double w, double h, int align, boolean shadow) {
        if (value == null || value.isEmpty()) return;
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.panelText(value));
        float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(x), UiText.centeredY(font, line, frame.y(y), frame.size(h), scale), frame.size(w), scale, UiMenuStyle.TEXT, shadow, align);
    }
    String tooltip(double mx, double my) {
        if (frame == null) return "";
        if (gui(closeBox()).contains(mx, my)) return label("CLOSE");
        if (!shown()) return "";
        if (gui(new Box(25, PAGE + 55, 305, 25)).contains(mx, my)) return log.manpowerTooltip();
        if (gui(new Box(400, PAGE + 55, 160, 25)).contains(mx, my)) return label("RATIO_TOOLTIP");
        if (my < frame.y(BODY)) return "";
        return tab == 0 ? battleTooltip(mx, my) : tab == 1 ? equipmentTooltip(mx, my) : templateTooltip(mx, my);
    }
    private String battleTooltip(double mx, double my) {
        for (int i = first(); i < Math.min(log.total(), first() + rows()); i++) {
            var battle = battle(i); var box = slot(i);
            if (battle == null || !gui(box).contains(mx, my)) continue;
            double x = box.x(), y = box.y();
            if (gui(new Box(x + 100, y + 3, 39, 26)).contains(mx, my)) return battle.iconTooltip();
            if (gui(new Box(x + 380, y + 5, 80, 23)).contains(mx, my)) return battle.dateTooltip();
            if (gui(new Box(x + LOCATION, y + 5, LOCATION_WIDTH, 23)).contains(mx, my)) return battle.locationTooltip();
            if (gui(new Box(x + 98, y + 31, 413, 70)).contains(mx, my)) return battle.imageTooltip();
            for (boolean own : new boolean[]{true, false}) {
                var party = own ? battle.own() : battle.enemy(); double side = x + (own ? 8 : 558);
                if (gui(new Box(side + 4, y + 6, 41, 26)).contains(mx, my)) return party.countries();
                if (gui(new Box(side, y + 36, 46.8, 63)).contains(mx, my)) return party.leader();
            }
        }
        return "";
    }
    private String equipmentTooltip(double mx, double my) {
        if (gui(new Box(285, BODY + 43, 130, 25)).contains(mx, my)) return log.lost().combatTooltip();
        if (gui(new Box(285, BODY + 75 + half() + 4, 130, 25)).contains(mx, my)) return log.enemyLost().combatTooltip();
        for (int list : List.of(OWN, ENEMY)) {
            if (!gui(new Box(25, listTop(list), 580, listHeight(list))).contains(mx, my)) continue;
            var rows = losses(list);
            for (int i = 0; i < rows.size(); i++) {
                var box = lossBox(list, i);
                if (!gui(box).contains(mx, my)) continue;
                if (gui(new Box(box.x() + 260, box.y(), 105, LOSS)).contains(mx, my)) return rows.get(i).combatTooltip();
                if (gui(new Box(box.x() + 365, box.y(), 105, LOSS)).contains(mx, my)) return rows.get(i).attritionTooltip();
                return gui(new Box(box.x() + 470, box.y(), 110, LOSS)).contains(mx, my) ? rows.get(i).recoveredTooltip() : "";
            }
        }
        return "";
    }
    private String templateTooltip(double mx, double my) {
        if (!gui(new Box(25, listTop(TEMPLATES), 580, listHeight(TEMPLATES))).contains(mx, my)) return "";
        for (int i = 0; i < log.templates().size(); i++) {
            var box = templateBox(i);
            if (!gui(box).contains(mx, my)) continue;
            if (gui(new Box(box.x(), box.y(), 48, TEMPLATE)).contains(mx, my)) return label("DEPLOYMENT_TEMPLATE_ITEM_EDIT");
            return gui(new Box(box.x() + 305, box.y(), 140, TEMPLATE)).contains(mx, my) ? log.templates().get(i).tooltip() : "";
        }
        return "";
    }
    boolean mouseClicked(MouseButtonEvent event, Runnable closer) {
        double mx = event.x(), my = event.y();
        if (!contains(mx, my)) return false;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (gui(closeBox()).contains(mx, my)) { AudioUiSounds.play("ui.close"); closer.run(); return true; }
        if (!shown()) { grab(event); return true; }
        for (int i = 0; i < TABS.length; i++) if (gui(tabBox(i)).contains(mx, my)) { AudioUiSounds.play("ui.click"); tab = i; return true; }
        if (gui(new Box(73, PAGE + 26, 16, 16)).contains(mx, my)) { period(months - 1); return true; }
        if (gui(new Box(251, PAGE + 26, 16, 16)).contains(mx, my)) { period(months + 1); return true; }
        if (gui(trackBox()).contains(mx, my)) { slider = true; slide(mx); return true; }
        var armies = armies();
        for (int i = armies.size() - 1; i >= 0; i--) if (gui(armyBox(i, armies.size())).contains(mx, my)) {
            AudioUiSounds.play("ui.click");
            String id = armies.get(i).id();
            if (!excluded.remove(id)) excluded.add(id);
            while (excluded.size() > CommanderRecordPayloads.FILTERS) excluded.remove(excluded.iterator().next());
            Arrays.fill(scrolls, 0); request();
            return true;
        }
        for (int list : lists()) if (maxScroll(list) > 0 && bar(list).contains(mx, my)) {
            if (bar(list).arrow(my)) scroll(list, my < frame.y(listTop(list)) + 8 ? -step(list) : step(list));
            else { dragging = list; drag(my); }
            return true;
        }
        if (tab == 0) { if (battleClicked(mx, my)) return true; }
        else if (tab == 1 && gui(filterBox()).contains(mx, my)) { AudioUiSounds.play("ui.click"); detailed = !detailed; scrolls[OWN] = scrolls[ENEMY] = 0; return true; }
        else if (tab == 2 && gui(new Box(25, listTop(TEMPLATES), 580, listHeight(TEMPLATES))).contains(mx, my)) for (int i = 0; i < log.templates().size(); i++) {
            var box = templateBox(i);
            if (!gui(new Box(box.x(), box.y(), 48, TEMPLATE)).contains(mx, my)) continue;
            AudioUiSounds.play("ui.click");
            designer.accept(log.templates().get(i).id());
            return true;
        }
        grab(event);
        return true;
    }
    private boolean battleClicked(double mx, double my) {
        for (int i = first(); i < Math.min(log.total(), first() + rows()); i++) {
            var battle = battle(i); var box = slot(i);
            if (battle == null || !gui(box).contains(mx, my)) continue;
            double x = box.x(), y = box.y();
            if (gui(new Box(x + LOCATION, y + 5, LOCATION_WIDTH, 23)).contains(mx, my)) { AudioUiSounds.play("ui.click"); CommanderCombatLogHud.send(new Request(token, Action.FOCUS, battle.province(), 0, 0, List.of())); return true; }
            for (boolean own : new boolean[]{true, false}) if (gui(new Box(x + (own ? 12 : 562), y + 6, 41, 26)).contains(mx, my)) { AudioUiSounds.play("ui.click"); country.accept((own ? battle.own() : battle.enemy()).country()); return true; }
        }
        return false;
    }
    private void period(int value) {
        value = Math.clamp(value, 1, log.maximum());
        if (value == months) return;
        months = value; Arrays.fill(scrolls, 0);
        AudioUiSounds.play("ui.scroll"); request();
    }
    private void slide(double mx) { var track = gui(trackBox()); period(1 + (int)Math.round(Math.clamp((mx - track.x() - frame.size(8)) / Math.max(1, track.w() - frame.size(16)), 0, 1) * (log.maximum() - 1))); }
    private static int step(int list) { return list == BATTLES ? 1 : list == TEMPLATES ? TEMPLATE : LOSS; }
    private void scroll(int list, int amount) { scrolls[list] = Math.clamp(scrolls[list] + amount, 0, maxScroll(list)); }
    private void drag(double my) { scrolls[dragging] = Math.clamp(Math.round(bar(dragging).offset(my) / frame.scale() / (dragging == BATTLES ? SLOT : 1)), 0, maxScroll(dragging)); }
    private void grab(MouseButtonEvent event) { moving = true; grabX = event.x() - left; grabY = event.y() - top; }
    boolean mouseDragged(MouseButtonEvent event) {
        if (moving) { left = (float)(event.x() - grabX); top = (float)(event.y() - grabY); return true; }
        if (slider) { slide(event.x()); return true; }
        if (dragging < 0) return false;
        drag(event.y());
        return true;
    }
    boolean mouseReleased() { boolean held = dragging >= 0 || slider || moving; dragging = -1; slider = false; moving = false; return held; }
    boolean mouseScrolled(double x, double y, double vertical) {
        if (!contains(x, y)) return false;
        if (shown()) for (int list : lists()) if (gui(new Box(0, listTop(list), INNER, listHeight(list))).contains(x, y)) scroll(list, -(int)Math.round(vertical * step(list)));
        return true;
    }
}
