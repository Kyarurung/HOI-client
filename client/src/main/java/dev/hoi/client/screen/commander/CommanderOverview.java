package dev.hoi.client.screen.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.client.map.commander.CommanderPanels;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiStatCell;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderOverviewPayloads.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CommanderOverview extends Screen implements InputSidebarMovement.InputMovementScreen {
    static final UiMenuStyle.Window WINDOW = new UiMenuStyle.Window("commander/commands/tiled_window_1b_border/frame_0", 550, -6, 5, 7, 45, 370, 507, 10, true);
    static final int ROW_X = 13, ROW_WIDTH = 511, ROW_HEIGHT = 35, LIST_MARGIN = 40;
    record Layout(int strip, int typesX, int typesY, int slotHeight, int sortY, int panelY, int rowY, List<Column> columns) {}
    record Column(String label, String sort, int x, int width) {}
    record Box(double x, double y, double w, double h) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    static final Map<Tab, Layout> LAYOUTS = Map.of(
            Tab.ARMY, new Layout(0, 29, 175, 29, 238, 257, 275, List.of(new Column("ARMY_SORT_TYPE", "TYPE", 14, 126), new Column("ARMY_SORT_NAME", "NAME", 138, 179), new Column("ARMY_SORT_THEATRE", "LOCATION", 315, 148), new Column("ARMY_SORT_GROUP", "GROUP", 460, 64))),
            Tab.NAVY, new Layout(1, 15, 148, 27, 213, 232, 250, List.of(new Column("ARMY_SORT_TYPE", "TYPE", 14, 164), new Column("ARMY_SORT_NAME", "NAME", 176, 158), new Column("ARMY_SORT_REGION", "LOCATION", 332, 128), new Column("ARMY_SORT_SIZE", "SIZE", 458, 65))),
            Tab.AIR, new Layout(2, 15, 183, 27, 273, 292, 310, List.of(new Column("ARMY_SORT_TYPE", "TYPE", 15, 135), new Column("ARMY_SORT_NAME", "NAME", 151, 133), new Column("ARMY_SORT_BASE", "LOCATION", 283, 128), new Column("ARMY_SORT_REGION", "GROUP", 409, 116))));
    private static final Identifier BAR_BACKGROUND = Identifier.parse("hoi:textures/gui/commander/commands/navieslist_bar_org/background/frame_0.png"),
            ORGANIZATION = Identifier.parse("hoi:textures/gui/commander/commands/navieslist_bar_org/fill/frame_0.png"),
            STRENGTH = Identifier.parse("hoi:textures/gui/commander/commands/navieslist_bar_str/fill/frame_0.png"),
            SORT = Identifier.parse("hoi:textures/gui/commander/overview/sort_button.png");
    public static Consumer<Request> requestObserver;
    private final Tab tab;
    private final String token = UUID.randomUUID().toString();
    private final Consumer<Request> transport;
    private View view;
    private List<Row> rows = List.of();
    private UiMenuStyle.Frame frame;
    private EditBox filter;
    private String query = "", typeFilter = "", sort = "TYPE", hovered = "";
    private boolean descending, dragging, suspended;
    private int scroll;
    private CommanderDivisionWindow details;
    private CommanderNavalLossWindow losses;
    public CommanderOverview(Tab tab) { this(tab, request -> { if (ClientPlayNetworking.canSend(Request.TYPE)) ClientPlayNetworking.send(request); }); }
    CommanderOverview(Tab tab, Consumer<Request> transport) {
        super(Component.literal(tab == Tab.ARMY ? "육군 개요" : tab == Tab.NAVY ? "해군 개요" : "공군 개요"));
        this.tab = tab; this.transport = transport;
    }
    public Tab tab() { return tab; }
    String token() { return token; }
    String hoveredTooltip() { return hovered; }
    public List<Row> rows() { return rows; }
    CommanderDivisionWindow details() { return details; }
    UiMenuStyle.Frame frame() { return frame; }
    int scroll() { return scroll; }
    public void open() { AudioUiSounds.play("ui.window_open"); send(Action.OPEN, ""); }
    public void accept(View next) {
        if (!next.token().equals(token) || next.tab() != tab) return;
        view = next; refresh();
    }
    public void accept(dev.hoi.protocol.commander.CommanderDivisionPayloads.View next) {
        if (details == null) return;
        boolean shown = details.view() != null && details.view().error().isEmpty();
        details.accept(next);
        if (shown && !details.view().error().isEmpty()) closeDetails();
    }
    private void openDetails(String division) {
        closeDetails();
        details = new CommanderDivisionWindow(division, new CommanderDivisionWindow.Host() {
            @Override public void edit(EditBox box) { addWidget(box); setFocused(box); }
            @Override public void release(EditBox box) { if (getFocused() == box) setFocused(null); removeWidget(box); }
        });
        details.layout(width, height, frame.scale());
        details.open();
    }
    private void closeDetails() { if (details != null) { details.close(); details = null; } }
    CommanderNavalLossWindow losses() { return losses; }
    public void accept(dev.hoi.protocol.commander.CommanderRecordPayloads.Losses next) { if (losses != null && !losses.accept(next)) closeLosses(); }
    private void toggleLosses() {
        if (losses != null) { closeLosses(); return; }
        losses = new CommanderNavalLossWindow();
        losses.layout(frame, width, height);
        losses.open();
    }
    private void closeLosses() { if (losses != null) { losses.close(); losses = null; } }
    static Box lossesBox() { return new Box(419, 8, 37, 36); }
    private boolean lossesAt(double x, double y) { return tab == Tab.NAVY && view != null && view.error().isEmpty() && gui(lossesBox()).contains(x, y); }
    private void send(Action action, String target) {
        var request = new Request(token, action, tab, target);
        if (requestObserver != null) requestObserver.accept(request); else transport.accept(request);
    }
    private Layout layout() { return LAYOUTS.get(tab); }
    private String label(String key) { return view == null ? "" : view.label(key); }
    @Override protected void init() {
        int pane = Math.max(180, dev.hoi.client.ui.UiPanelLayout.width(dev.hoi.protocol.menu.MenuTab.CONSTRUCTION, width));
        frame = UiMenuStyle.frame(WINDOW, width, pane);
        HudMenuBar.buttons(width, dev.hoi.client.ClientCampaignHud.country(), null, HoiClient::openMenu).forEach(this::addRenderableWidget);
        addRenderableWidget(UiMenuStyle.close(WINDOW, frame, this::onClose));
        filter = new EditBox(font, frame.ix(343), frame.iy(85), frame.len(177), frame.len(20), Component.literal("필터"));
        filter.setBordered(false); filter.setMaxLength(64); filter.setValue(query);
        filter.setResponder(value -> { query = value; scroll = 0; refresh(); });
        if (!label(filterTooltipKey()).isEmpty()) filter.setTooltip(Tooltips.createOfficer(label(filterTooltipKey())));
        addRenderableWidget(filter);
        if (details != null) details.layout(width, height, frame.scale());
        if (losses != null) losses.layout(frame, width, height);
        if (suspended) { suspended = false; send(Action.OPEN, ""); if (details != null) details.request(); if (losses != null) losses.request(); }
        refresh();
    }
    private String filterTooltipKey() { return tab == Tab.ARMY ? "ARMY_DIVISONS_FILTER_DESC" : tab == Tab.NAVY ? "ARMY_NAVIES_FILTER_DESC" : "ARMY_AIR_FILTER_DESC"; }
    @Override public void removed() { InputSidebarMovement.release(minecraft); if (details != null) details.close(); if (losses != null) losses.close(); send(Action.CLOSE, ""); suspended = true; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    @Override public boolean allowsMovement() { return !(getFocused() instanceof EditBox); }
    @Override public int unitHudLeft() { return frame == null ? 0 : frame.ix(550); }
    @Override public boolean covers(double x, double y) { return details != null && details.contains(x, y) || losses != null && losses.contains(x, y); }
    @Override public void drawOver(GuiGraphicsExtractor g, int mx, int my) {
        if (losses != null) {
            losses.draw(g, font, mx, my);
            String text = losses.tooltip(mx, my);
            if (!text.isEmpty()) { g.nextStratum(); Tooltips.drawSourceScrollable(g, font, text, mx, my, g.guiWidth(), g.guiHeight(), 0); }
        }
        if (details == null) return;
        details.draw(g, font, mx, my);
        String text = details.contains(mx, my) ? details.tooltip(mx, my) : "";
        if (!text.isEmpty()) { g.nextStratum(); Tooltips.drawSourceScrollable(g, font, text, mx, my, g.guiWidth(), g.guiHeight(), 0); }
    }
    void refresh() {
        if (view == null) { rows = List.of(); return; }
        String text = query.strip().toLowerCase(Locale.ROOT);
        Comparator<Row> order = switch (sort) {
            case "NAME" -> Comparator.comparing(Row::name);
            case "LOCATION" -> Comparator.comparing(Row::location).thenComparing(Row::name);
            case "GROUP" -> Comparator.comparing(Row::group).thenComparing(Row::name);
            case "SIZE" -> Comparator.<Row>comparingInt(row -> amount(row.amount())).reversed().thenComparing(Row::name);
            default -> Comparator.comparing(Row::type).thenComparing(Row::name);
        };
        if (descending) order = order.reversed();
        rows = view.rows().stream().filter(row -> typeFilter.isEmpty() || row.type().equals(typeFilter))
                .filter(row -> text.isEmpty() || (row.name() + "\n" + row.location() + "\n" + row.group() + "\n" + typeName(row.type())).toLowerCase(Locale.ROOT).contains(text))
                .sorted(order.thenComparing(Row::id)).toList();
        scroll = Math.clamp(scroll, 0, maxScroll());
    }
    private String typeName(String key) {
        if (view == null) return "";
        return view.types().stream().filter(count -> count.key().equals(key)).findFirst().map(count -> Tooltips.officerText(count.tooltip()).getString()).orElse("");
    }
    private static int amount(String value) { int slash = value.indexOf('/'); try { return Integer.parseInt(slash < 0 ? value : value.substring(0, slash)); } catch (NumberFormatException e) { return 0; } }
    private double bottomUnits() { return (height - frame.y(0)) / frame.scale(); }
    private double listBottom() { return bottomUnits() - LIST_MARGIN; }
    private int visibleUnits() { return (int)Math.max(0, listBottom() - layout().rowY()); }
    private int maxScroll() { return frame == null ? 0 : Math.max(0, rows.size() * ROW_HEIGHT - visibleUnits()); }
    private Box rowBox(int index) { return new Box(ROW_X, layout().rowY() + index * ROW_HEIGHT - scroll, ROW_WIDTH, ROW_HEIGHT); }
    private Box gui(Box box) { return new Box(frame.x(box.x()), frame.y(box.y()), frame.size(box.w()), frame.size(box.h())); }
    private Box typeBox(int index) {
        var l = layout(); double x = l.typesX() + index % 6 * 86, y = l.typesY() + index / 6 * l.slotHeight();
        return tab == Tab.ARMY ? new Box(x - 14, y + 6, 86, 29) : new Box(x, y, 86, 27);
    }
    private static Box modifierBox(int index) { return new Box(15 + index % 7 * 74, 114 + index / 7 * 33, UiStatCell.WIDTH, UiStatCell.HEIGHT); }
    private UiScrollBar bar() {
        int top = Math.round(frame.y(layout().rowY())), bottom = Math.round(frame.y(listBottom()));
        return new UiScrollBar(frame.ix(527), top, bottom, Math.round(frame.size(visibleUnits())), Math.round(frame.size(rows.size() * ROW_HEIGHT)));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        double bottom = bottomUnits(); var l = layout();
        boolean covered = covers(mx, my);
        HudMenuBar.draw(g, width, dev.hoi.client.ClientCampaignHud.hud(), covered ? -1 : mx, covered ? -1 : my, 0);
        UiMenuStyle.window(g, WINDOW, frame, height);
        frame.tile(g, "commander/commands/tiled_bg/frame_0", 0, 40, 550, (int)Math.ceil(bottom - 40), 64, 192);
        frame.tile(g, WINDOW.tile(), 0, l.panelY(), 550, (int)Math.ceil(bottom - l.panelY()), 64, 190);
        UiMenuStyle.title(g, font, WINDOW, frame, label(tab == Tab.ARMY ? "ARMY_ARMY_LABEL" : tab == Tab.NAVY ? "ARMY_NAVY_LABEL" : "ARMY_AIR_LABEL"));
        if (tab == Tab.NAVY) frame.art(g, "commander/losses/button", lossesBox().x(), lossesBox().y());
        frame.art(g, "commander/overview/top_strip/frame_" + l.strip(), 14, 55);
        frame.art(g, "commander/overview/edit_box", 333, 80);
        text(g, label("ARMY_FILTER_LABEL"), 343, 62, 180, 20, false);
        if (view != null) {
            text(g, view.total(), 183, 66, 140, 20, false);
            if (!view.secondary().isEmpty()) text(g, view.secondary(), 183, 83, 140, 20, false);
            if (!view.special().isEmpty()) { frame.art(g, "recruitment/special", 287, 82); text(g, view.special(), 311, 83, 25, 20, false); }
            for (int i = 0; i < view.modifiers().size(); i++) { var box = modifierBox(i); UiStatCell.draw(g, font, view.modifiers().get(i).icon(), view.modifiers().get(i).value(), frame.x(box.x()), frame.y(box.y()), frame.scale()); }
            for (int i = 0; i < Math.min(12, view.types().size()); i++) drawType(g, view.types().get(i), typeBox(i));
            int listTop = Math.round(frame.y(l.rowY())), listBottom = Math.round(frame.y(listBottom()));
            g.enableScissor(frame.ix(ROW_X), listTop, frame.ix(ROW_X + ROW_WIDTH), listBottom);
            for (int i = 0; i < rows.size(); i++) {
                var box = rowBox(i);
                if (box.y() + ROW_HEIGHT < l.rowY() || box.y() > listBottom()) continue;
                drawRow(g, rows.get(i), box.x(), box.y());
            }
            g.disableScissor();
            if (!view.error().isEmpty()) text(g, view.error(), ROW_X + 8, l.rowY() + 8, ROW_WIDTH - 16, 20, false);
            bar().draw(g, Math.round(frame.size(scroll)));
            frame.art(g, "commander/commands/trade_header/frame_0", 11, bottom - 44);
            text(g, view.footer(), 26, bottom - 36, 490, 20, true);
        }
        for (var column : l.columns()) {
            sortButton(g, column.x(), l.sortY(), column.width());
            text(g, label(column.label()), column.x(), l.sortY(), column.width(), 23, true);
        }
        super.extractRenderState(g, covered ? -1 : mx, covered ? -1 : my, delta);
        if (!covered) tooltip(g, mx, my);
    }
    private void text(GuiGraphicsExtractor g, String value, double x, double y, double w, double h, boolean centered) {
        if (value == null || value.isEmpty()) return;
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.officerText(value));
        float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(x), UiText.centeredY(font, line, frame.y(y), frame.size(h), scale), frame.size(w), scale, UiMenuStyle.TEXT, false, centered ? 0 : -1);
    }
    private void drawType(GuiGraphicsExtractor g, Count count, Box box) {
        art(g, "production/picker/filter_bg", box.x(), box.y(), 90, 29);
        if (count.key().equals(typeFilter)) art(g, "production/picker/filter_highlight", box.x(), box.y(), 90, 29);
        String icon = tab == Tab.ARMY ? CommanderPanels.armyUnitIcon(count.icon()) : count.icon();
        int[] size = UiAssets.imageSize(icon); double scale = tab == Tab.ARMY ? .7 : .9, w = size[0] * scale, h = size[1] * scale;
        double cx = box.x() + (tab == Tab.ARMY ? 43 : tab == Tab.NAVY ? 44 : 58), cy = box.y() + 16;
        if (size[0] > 0) UiAssets.drawExact(g, icon, frame.x(cx - w / 2), frame.y(cy - h / 2), frame.size(w), frame.size(h));
        text(g, Integer.toString(count.count()), box.x() + (tab == Tab.ARMY ? 10 : tab == Tab.NAVY ? 4 : 6), box.y(), 40, 22, false);
    }
    private void sortButton(GuiGraphicsExtractor g, double x, double y, double w) {
        int width = (int)Math.round(w);
        g.pose().pushMatrix();
        g.pose().translate(frame.x(x), frame.y(y));
        g.pose().scale(frame.scale());
        g.blit(SORT, 0, 0, 20, 23, 0, 20 / 60f, 0, 1);
        g.blit(SORT, 20, 0, width - 20, 23, 20 / 60f, 40 / 60f, 0, 1);
        g.blit(SORT, width - 20, 0, width, 23, 40 / 60f, 1, 0, 1);
        g.pose().popMatrix();
    }
    private void art(GuiGraphicsExtractor g, String path, double x, double y, double w, double h) { UiAssets.drawExact(g, path, frame.x(x), frame.y(y), frame.size(w), frame.size(h)); }
    private void drawRow(GuiGraphicsExtractor g, Row row, double x, double y) {
        switch (tab) {
            case ARMY -> {
                art(g, "commander/overview/army_entry", x, y, 509, 35);
                art(g, "commander/commands/unitlist_unitinfo_button/frame_0", x + 1, y + 2, 76, 31);
                String icon = CommanderPanels.armyUnitIcon(row.icon());
                if (UiAssets.imageSize(icon)[0] > 0) art(g, icon, x + 6, y + 1, 60.8, 33.6);
                if (row.obsolete()) art(g, "commander/commands/obsolete_template_warning/frame_0", x + 16, y + 6, 16, 14);
                if (row.fighting()) art(g, "diplomacy/relations/war_relation", x, y + 3, 20.3, 18.2);
                bar(g, ORGANIZATION, x + 79, y + 30, row.organization());
                bar(g, STRENGTH, x + 86, y + 30, row.strength());
                art(g, "commander/commands/unit_level/frame_" + row.level(), x + 93, y + 4, 27, 27);
                text(g, row.name(), x + 127, y + 2, 170, 23, false);
                text(g, row.location(), x + 309, y + 1, 140, 16, false);
                text(g, row.group(), x + 309, y + 17, 140, 16, false);
                if (row.color() != null) UiAssets.drawExact(g, "commander/commands/theatre_army_shield/frame_" + Objects.requireNonNullElse(row.insignia(), 0), frame.x(x + 461), frame.y(y + 2), frame.size(33), frame.size(32), 0xFF000000 | row.color());
            }
            case NAVY -> {
                art(g, "commander/overview/navy_entry", x, y, 509, 35);
                int[] size = UiAssets.imageSize(row.icon());
                art(g, row.icon(), x + 9, y + 9, size[0], size[1]);
                if (row.fighting()) art(g, "diplomacy/relations/war_relation", x + 3, y + 3, 20.3, 18.2);
                bar(g, ORGANIZATION, x + 119, y + 30, row.organization());
                bar(g, STRENGTH, x + 127, y + 30, row.strength());
                art(g, row.leader().isEmpty() ? "politics/empty/leader" : row.leader(), x + 139, y + 3, 22.9, 30.9);
                art(g, "commander/overview/leader_frame", x + 138, y + 1, 26, 33);
                text(g, row.name(), x + 169, y + 7, 148, 18, false);
                text(g, row.location(), x + 328, y + 7, 122, 18, false);
                if (!row.missions().isEmpty()) art(g, CommanderHud.overviewMissionIcon(tab, row.missions().getFirst()), x + 455, y + 1, 43, 34);
                text(g, row.amount(), x + 450, y, 40, 16, false);
            }
            case AIR -> {
                art(g, "commander/overview/air_entry", x, y, 509, 35);
                if (!row.icon().isEmpty()) UiAssets.drawContent(g, row.icon(), frame.ix(x + 4), frame.iy(y + 1), frame.len(82), frame.len(33));
                bar(g, STRENGTH, x + 88, y + 30, row.strength());
                int slash = row.amount().indexOf('/');
                textRight(g, slash < 0 ? row.amount() : row.amount().substring(0, slash), x + 51, y + 1, 35, 16);
                textRight(g, slash < 0 ? "" : row.amount().substring(slash + 1), x + 51, y + 17, 35, 16);
                text(g, row.name(), x + 142, y + 6, 130, 23, false);
                text(g, row.location(), x + 268, y + 6, 124, 23, true);
                text(g, row.group(), x + 397, y, 120, 16, false);
                if (!row.missions().isEmpty()) { String mission = CommanderHud.overviewMissionIcon(tab, row.missions().getFirst()); if (!mission.isEmpty()) art(g, mission, x + 395, y + 15, 29.4, 20.3); }
            }
        }
    }
    private void textRight(GuiGraphicsExtractor g, String value, double x, double y, double w, double h) {
        if (value.isEmpty()) return;
        var line = Component.literal(value).getVisualOrderText(); float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(x), UiText.centeredY(font, line, frame.y(y), frame.size(h), scale), frame.size(w), scale, UiMenuStyle.TEXT, false, 1);
    }
    private void bar(GuiGraphicsExtractor g, Identifier fill, double left, double bottom, double ratio) {
        g.pose().pushMatrix();
        g.pose().translate(frame.x(left), frame.y(bottom));
        g.pose().rotate((float)(-Math.PI / 2));
        g.pose().scale(frame.scale());
        g.blit(BAR_BACKGROUND, 0, 0, 28, 6, 0, 1, 0, 1);
        int length = (int)Math.round(28 * Math.clamp(ratio, 0, 1));
        if (length > 0) g.blit(fill, 0, 0, length, 6, 0, length / 28f, 0, 1);
        g.pose().popMatrix();
    }
    private void tooltip(GuiGraphicsExtractor g, int mx, int my) {
        hovered = "";
        if (view == null || mx >= frame.x(550)) return;
        String text = lossesAt(mx, my) ? label("SUNK_SHIP_DETAILS") : "";
        if (!view.special().isEmpty() && gui(new Box(287, 82, 50, 23)).contains(mx, my)) text = view.specialTooltip();
        for (int i = 0; i < view.modifiers().size(); i++) if (gui(modifierBox(i)).contains(mx, my)) text = view.modifiers().get(i).tooltip();
        for (int i = 0; i < Math.min(12, view.types().size()); i++) if (gui(typeBox(i)).contains(mx, my)) text = view.types().get(i).tooltip();
        var l = layout();
        if (my >= frame.y(l.rowY()) && my < frame.y(listBottom())) for (int i = 0; i < rows.size(); i++) {
            var box = rowBox(i); if (!gui(box).contains(mx, my)) continue;
            var row = rows.get(i);
            if (row.fighting() && gui(new Box(box.x() + (tab == Tab.NAVY ? 3 : 0), box.y() + 3, 20.3, 18.2)).contains(mx, my)) text = label("IN_COMBAT");
            else if (row.obsolete() && gui(new Box(box.x() + 16, box.y() + 6, 16, 14)).contains(mx, my)) text = label("DECOMMISSIONED_TEMPLATE_WARNING_TOOLTIP");
            else text = label(tab == Tab.ARMY ? "ARMY_DIVISIONS_BUTTON" : tab == Tab.NAVY ? "ARMY_NAVIES_BUTTON" : "ARMY_AIR_BUTTON");
            break;
        }
        hovered = text;
        if (!text.isEmpty()) { g.nextStratum(); Tooltips.drawSourceScrollable(g, font, text, mx, my, g.guiWidth(), g.guiHeight(), 0); }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (details != null && details.mouseClicked(event, twice, font, this::closeDetails)) return true;
        if (losses != null && losses.mouseClicked(event, this::closeLosses)) return true;
        if (super.mouseClicked(event, twice)) return true;
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && lossesAt(event.x(), event.y())) { AudioUiSounds.play("ui.click"); toggleLosses(); return true; }
        if (view == null || event.x() >= frame.x(550)) return false;
        var l = layout();
        if (bar().contains(event.x(), event.y())) {
            if (bar().arrow(event.y())) scroll = Math.clamp(scroll + (event.y() < frame.y(l.rowY()) + 8 ? -ROW_HEIGHT : ROW_HEIGHT), 0, maxScroll());
            else { dragging = true; scroll = Math.round(bar().offset(event.y()) / frame.scale()); }
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (var column : l.columns()) if (gui(new Box(column.x(), l.sortY(), column.width(), 23)).contains(event.x(), event.y())) {
                AudioUiSounds.play("ui.click");
                if (sort.equals(column.sort())) descending = !descending; else { sort = column.sort(); descending = false; }
                refresh(); return true;
            }
            for (int i = 0; i < Math.min(12, view.types().size()); i++) if (gui(typeBox(i)).contains(event.x(), event.y())) {
                AudioUiSounds.play("ui.click");
                String key = view.types().get(i).key(); typeFilter = typeFilter.equals(key) ? "" : key; scroll = 0; refresh(); return true;
            }
        }
        if (event.y() < frame.y(l.rowY()) || event.y() >= frame.y(listBottom())) return true;
        for (int i = 0; i < rows.size(); i++) {
            if (!gui(rowBox(i)).contains(event.x(), event.y())) continue;
            var row = rows.get(i);
            if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) { send(Action.FOCUS, row.id()); return true; }
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
            if (tab == Tab.ARMY && gui(new Box(ROW_X + 1, rowBox(i).y() + 2, 76, 31)).contains(event.x(), event.y())) { openDetails(row.id()); return true; }
            AudioUiSounds.play(tab == Tab.ARMY ? "ui.division.select" : tab == Tab.NAVY ? "ui.fleet.select" : "ui.aircraft.select");
            CommanderHud.selectOverview(tab, row.id(), event.hasShiftDown());
            return true;
        }
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (details != null && details.mouseDragged(event) || losses != null && losses.mouseDragged(event)) return true;
        if (dragging) { scroll = Math.round(bar().offset(event.y()) / frame.scale()); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { dragging = false; if (details != null) details.mouseReleased(); if (losses != null) losses.mouseReleased(); return super.mouseReleased(event); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (details != null && details.keyPressed(event)) return true;
        if (details != null && event.key() == InputConstants.KEY_ESCAPE) { closeDetails(); return true; }
        if (losses != null && event.key() == InputConstants.KEY_ESCAPE) { if (!losses.closeMenu()) closeLosses(); return true; }
        return super.keyPressed(event);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (details != null && details.mouseScrolled(x, y, vertical) || losses != null && losses.mouseScrolled(x, y, vertical)) return true;
        if (frame != null && x < frame.x(550) && y >= frame.y(layout().rowY()) && y < frame.y(listBottom())) { scroll = Math.clamp(scroll - (int)Math.round(vertical * ROW_HEIGHT * 2), 0, maxScroll()); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
}
