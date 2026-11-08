package dev.hoi.client.screen.raid;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.raid.RaidPayloads;
import dev.hoi.protocol.raid.RaidView;
import dev.hoi.protocol.raid.RaidView.*;
import dev.hoi.client.screen.decision.DecisionText;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class RaidScreen extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final java.util.regex.Pattern TARGET_ICON = java.util.regex.Pattern.compile("^£[A-Za-z0-9_]+(?:\\|[0-9]+)?£?\\s*");
    private final String token;
    private final Consumer<RaidPayloads.Request> transport;
    private final DecisionText text = new DecisionText();
    private final RaidMap map = new RaidMap();
    private final RaidSources sources = new RaidSources();
    private final Set<String> collapsed = new HashSet<>();
    private final List<Hit> hits = new ArrayList<>();
    private RaidView view;
    private int pending, refresh, scroll, equipmentScroll, windowX, windowY;
    private final Map<String, Integer> categoryOffsets = new HashMap<>();
    private final Map<String, UiScrollBar> categoryBars = new LinkedHashMap<>();
    private String scrollingCategory;
    private boolean sourceTab, autoExpanded, closed, dragging, scrolling, overlay;
    private double dragX, dragY;
    private String tooltipText = "";
    private int tooltipScroll;
    private final int[] reportScroll = new int[2];
    private final UiScrollBar[] reportBars = new UiScrollBar[2];
    private int scrollingReport = -1;
    private Tooltips.ScrolledTooltip tooltipBounds;
    private record Hit(int x, int y, int w, int h, String tooltip) {
        boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }
    public RaidScreen(String token, RaidView fixture, Consumer<RaidPayloads.Request> transport) {
        super(Component.literal("군사 습격")); this.token = token; this.view = fixture; this.transport = transport;
    }
    public RaidView snapshot() { return view; }
    public void overlay() { overlay = true; map.clear(); }
    public static void resourcesReloaded() { RaidMap.resourcesReloaded(); }
    List<RaidMap.Hit> mapHits() { return map.hits(); }
    List<RaidMap.Interaction> mapInteractions() { return map.interactions(); }
    public void open() { send(RaidPayloads.Kind.OPEN, "", List.of()); }
    public void update(RaidView next) {
        if (closed || !token.equals(next.session())) return;
        if (next.country().isEmpty() || view != null && !view.country().equals(next.country())
                || !next.country().equals(ClientCampaignHud.country())) { revoke(); return; }
        if (view != null && next.revision() <= view.revision()) return;
        boolean changed = view == null || !Objects.equals(view.window() == null ? null : view.window().raid(), next.window() == null ? null : next.window().raid())
                || report() != (next.window() != null && next.window().feedback() != null);
        view = next; pending = 0; autoExpanded = false; map.clear(); sources.clear();
        if (changed) { centerWindow(); equipmentScroll = 0; Arrays.fill(reportScroll, 0); scrollingReport = -1; }
        rebuildWidgets();
    }
    public void revoke() { closed = true; view = null; map.clear(); sources.clear(); clearWidgets(); if (!overlay) minecraft.gui.setScreen(null); }
    public void accept(RaidPayloads.Highlight packet) { if (!closed) sources.accept(view, packet); }
    public RaidSources sources() { return sources; }
    public boolean showingSources() { return !closed && sourceTab && view != null && view.country().equals(ClientCampaignHud.country()) && view.setup() != null && view.window() == null; }
    private void send(RaidPayloads.Kind kind, String item, List<String> args) {
        if (closed || kind != RaidPayloads.Kind.CLOSE && pending > 0) return;
        pending = 100; refresh = 0;
        transport.accept(new RaidPayloads.Request(kind, token, view == null ? 0 : view.revision(), item, args));
        if (minecraft != null) rebuildWidgets();
    }
    void call(String item, List<String> args) {
        if (view == null || pending > 0 || item.isEmpty() || !view.country().equals(ClientCampaignHud.country()) || !view.issued().contains(item)) return;
        var actions = new ArrayList<Action>();
        if (view.window() != null) actions.addAll(view.window().actions());
        if (view.setup() != null) actions.add(view.setup().close());
        view.categories().forEach(category -> actions.add(category.launchAll()));
        if (actions.stream().anyMatch(action -> action.item().equals(item) && !action.enabled())) return;
        if (view.window() != null && item.equals(view.window().auto().item())) {
            if (args.size() != 1 || view.window().autoOptions().stream().noneMatch(choice -> choice.value().equals(args.getFirst()))) return;
        } else if (!args.isEmpty()) return;
        send(RaidPayloads.Kind.CALL, item, args);
    }
    private boolean report() { return view != null && view.window() != null && view.window().feedback() != null; }
    private int windowWidth() { return report() ? 319 : 320; }
    private int windowHeight() { return report() ? 260 : 299; }
    private void centerWindow() {
        windowX = Math.max(0, (width - windowWidth()) / 2);
        windowY = Math.clamp(report() ? (height - windowHeight()) / 2 : height / 2 - 200, HudMenuBar.height(width) + 2, Math.max(HudMenuBar.height(width) + 2, height - windowHeight()));
    }
    @Override protected void init() {
        if (windowX == 0 && windowY == 0) centerWindow();
        windowX = Math.clamp(windowX, 0, Math.max(0, width - windowWidth()));
        windowY = Math.clamp(windowY, HudMenuBar.height(width), Math.max(HudMenuBar.height(width), height - windowHeight()));
        var flags = minecraft.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:ui/decision_flags.json"));
        if (flags.isPresent()) try (var reader = flags.get().openAsReader()) { text.flags(com.google.gson.JsonParser.parseReader(reader).getAsJsonObject()); }
        catch (java.io.IOException | RuntimeException invalid) { text.flags(new com.google.gson.JsonObject()); }
        if (!overlay) HudMenuBar.buttons(width, ClientCampaignHud.country(), null, HoiClient::openMenu).forEach(this::addRenderableWidget);
        layout(null);
    }
    static String asset(String sprite, int frame) { return "raids/" + sprite.toLowerCase(Locale.ROOT) + "/frame_" + frame; }
    private void image(GuiGraphicsExtractor g, String sprite, int frame, int x, int y, int w, int h) {
        if (g != null) UiAssets.draw(g, asset(sprite, frame), x, y, w, h);
    }
    private void original(GuiGraphicsExtractor g, String sprite, int frame, int x, int y, double scale) {
        if (g == null) return;
        String path = asset(sprite, frame); int[] size = UiAssets.imageSize(path);
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale((float)scale);
        UiAssets.draw(g, path, 0, 0, size[0], size[1]); g.pose().popMatrix();
    }
    private void label(GuiGraphicsExtractor g, String value, int x, int y, int w, int h, int color) {
        if (g != null && !value.isEmpty()) UiText.centered(g, font, text.component(value), x, y, w, h, color);
    }
    private void unitPicture(GuiGraphicsExtractor g, String path, int x, int y, float centerY) {
        if (g == null) return;
        int[] size = UiAssets.imageSize(path);
        float scale = Math.min(.5f, Math.min(91f / Math.max(1, size[0]), 36f / Math.max(1, size[1])));
        float top = y;
        if (path.startsWith("production/source/") || path.startsWith("production/units/")) {
            int[] bounds = UiAssets.visibleBounds(path, 0, 0, size[0], size[1]);
            top = centerY - (bounds[1] + bounds[3] / 2f) * scale;
        }
        g.pose().pushMatrix(); g.pose().translate(x, top); g.pose().scale(scale);
        UiAssets.draw(g, path, 0, 0, size[0], size[1]); g.pose().popMatrix();
    }
    private void targetLabel(GuiGraphicsExtractor g, String value, int x, int y, int w, int h) {
        if (g == null) return;
        var match = TARGET_ICON.matcher(value);
        if (!match.find()) {
            UiText.scrollingCell(g, font, text.component(value), x, y, w, h, UiMenuStyle.TEXT, true);
            return;
        }
        var icon = text.component(match.group().stripTrailing());
        var name = text.component(value.substring(match.end()));
        int iconWidth = Math.round(font.width(icon) * 1.5f), nameWidth = Math.min(font.width(name), w - iconWidth - 3);
        int left = x + (w - iconWidth - 3 - nameWidth) / 2;
        g.pose().pushMatrix(); g.pose().translate(left, UiText.centeredY(font, icon.getVisualOrderText(), y, h, 1.5f)); g.pose().scale(1.5f);
        g.text(font, icon, 0, 0, UiMenuStyle.TEXT, false); g.pose().popMatrix();
        UiText.scrollingCell(g, font, name, left + iconWidth + 3, y, nameWidth, h, UiMenuStyle.TEXT, true);
    }
    private void tip(GuiGraphicsExtractor g, String value, int x, int y, int w, int h) {
        if (g != null && !value.isEmpty()) hits.add(new Hit(x, y, w, h, value));
    }
    private void button(GuiGraphicsExtractor g, String id, String label, String tooltip, String sprite, int frame, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (g != null) {
            if (sprite.equals("GFX_tiled_window_small")) UiAssets.nineSlice(g, asset(sprite, frame), x, y, w, h, 32, .5);
            else if (enabled) UiAssets.draw(g, asset(sprite, frame), x, y, w, h);
            else UiAssets.drawGrayscale(g, asset(sprite, frame), x, y, w, h);
            label(g, label, x, y, w, h, UiMenuStyle.TEXT);
            tip(g, tooltip, x, y, w, h);
            return;
        }
        var button = new UiPanelButton(id, x, y, w, h, action) {
            @Override protected void extractContents(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
            }
        };
        button.active = enabled; button.setTooltip(null); addRenderableWidget(button);
    }
    private void action(GuiGraphicsExtractor g, Action action, String sprite, int frame, int x, int y, int w, int h) {
        button(g, action.item(), action.label(), action.tooltip(), sprite, frame, x, y, w, h, action.enabled() && pending == 0, () -> call(action.item(), List.of()));
    }
    private int setupY() { return Math.clamp(height / 2 - 100, HudMenuBar.height(width) + 2, Math.max(HudMenuBar.height(width) + 2, height - 225)); }
    private UiScrollBar setupBar() {
        int count = view == null || view.setup() == null ? 0 : sourceTab ? view.setup().sources().size() : view.setup().units().size();
        return new UiScrollBar(108, setupY() + 70, setupY() + 215, 145, count * 31);
    }
    private void layout(GuiGraphicsExtractor g) {
        if (view == null) return;
        categoryBars.clear();
        Arrays.fill(reportBars, null);
        if (view.window() == null) categories(g);
        if (view.setup() != null && view.window() == null) setup(g);
        if (view.window() != null) {
            if (view.window().feedback() != null) feedback(g); else window(g);
        }
    }
    private void categories(GuiGraphicsExtractor g) {
        categoryBars.clear();
        int x = width - 160, y = HudMenuBar.height(width) + 20;
        long expanded = view.categories().stream().filter(category -> !collapsed.contains(category.id())
            && view.raids().stream().anyMatch(raid -> raid.category().equals(category.id()))).count();
        int available = Math.max(20, (height - y - 10 - view.categories().size() * 26) / (int)Math.max(1, expanded));
        for (var category : view.categories()) {
            var raids = view.raids().stream().filter(raid -> raid.category().equals(category.id())).toList();
            boolean folded = collapsed.contains(category.id());
            original(g, "GFX_raids_header_bg", 0, x, y, .5);
            if (g != null) UiText.scrollingCell(g, font, Component.literal(category.name()), x + 19, y + 3, 85, 14, UiMenuStyle.TEXT, false);
            tip(g, category.tooltip(), x, y, 116, 24);
            String expandTip = folded ? view.expandTooltip() : view.collapseTooltip();
            if (raids.isEmpty()) expandTip += "\n" + view.emptyTooltip();
            button(g, "expand:" + category.id(), "", expandTip, "GFX_conveyor_collapse_btn", folded ? 1 : 0, x + 120, y + 6, 12, 12, true, () -> {
                if (!collapsed.add(category.id())) collapsed.remove(category.id()); rebuildWidgets();
            });
            action(g, category.launchAll(), "GFX_raids_launch_all_button", 0, x + 135, y + 5, 13, 13);
            int top = y + 24;
            y += 26;
            if (folded || raids.isEmpty()) continue;
            int total = raids.size() * 20, visible = Math.min(total, Math.min(175, available));
            var bar = new UiScrollBar(x + 151, top, top + visible, visible, total);
            categoryBars.put(category.id(), bar);
            int offset = Math.clamp(categoryOffsets.getOrDefault(category.id(), 0), 0, bar.maximum());
            categoryOffsets.put(category.id(), offset);
            if (g != null) g.enableScissor(x + 1, top, x + 151, top + visible);
            for (int i = 0; i < raids.size(); i++) {
                var raid = raids.get(i);
                int row = top + i * 20 - offset;
                int rowHeight = 20;
                if (row < top || row + rowHeight > top + visible) continue;
                int rowWidth = 149;
                button(g, raid.open(), "", raid.tooltip(), "GFX_theatre_dropdown_middle", 0, x + 2, row, rowWidth, rowHeight, pending == 0, () -> call(raid.open(), List.of()));
                original(g, raid.icon(), 0, x + 4, row + 1, .5);
                if (g != null) {
                    UiText.scrollingCell(g, font, text.component(raid.description()), x + 19, row, 96, 9, UiMenuStyle.TEXT, false);
                    UiText.scrollingCell(g, font, text.component(raid.status()), x + 19, row + 9, 96, 9, UiMenuStyle.TEXT, false);
                }
                original(g, "GFX_raid_outcome_success", 0, x + 117, row + 3, .25);
                label(g, raid.chance(), x + 132, row + 3, 15, 10, UiMenuStyle.TEXT);
            }
            if (g != null) { g.disableScissor(); bar.draw(g, offset); }
            y += visible;
        }
    }
    private void setup(GuiGraphicsExtractor g) {
        var setup = view.setup(); int x = 3, y = setupY();
        if (g != null) {
            UiAssets.nineSlice(g, asset("GFX_tiled_bg", 0), x, y, 120, 225, 64, .5);
            UiAssets.nineSlice(g, asset("GFX_tiled_plain_bg", 0), x + 7, y + 67, 105, 150, 4, .5);
        }
        label(g, setup.title(), x + 20, y + 12, 80, 25, UiMenuStyle.TEXT);
        action(g, setup.close(), "GFX_closebutton_small", 0, x + 103, y + 3, 12, 12);
        button(g, "units", setup.unitTab(), "", "GFX_sort_button_83x29", sourceTab ? 0 : 1, x + 16, y + 39, 42, 15, true, () -> { sourceTab = false; scroll = 0; rebuildWidgets(); });
        button(g, "sources", setup.sourceTab(), "", "GFX_sort_button_83x29", sourceTab ? 1 : 0, x + 62, y + 39, 42, 15, true, () -> { sourceTab = true; scroll = 0; rebuildWidgets(); });
        label(g, sourceTab ? setup.sourcePrompt() : setup.unitPrompt(), x + 10, y + 56, 100, 10, UiMenuStyle.TEXT);
        scroll = Math.clamp(scroll, 0, setupBar().maximum());
        int count = sourceTab ? setup.sources().size() : setup.units().size();
        if (g != null) g.enableScissor(x + 9, y + 70, x + 109, y + 215);
        for (int i = 0; i < count; i++) {
            int row = y + 70 + i * 31 - scroll;
            if (row + 31 <= y + 70 || row >= y + 215) continue;
            int top = Math.max(row, y + 70), visible = Math.min(row + 31, y + 215) - top;
            if (sourceTab) {
                var source = setup.sources().get(i);
                button(g, source.select(), "", "", "GFX_tiled_window_small", 0, x + 10, g == null ? top : row, g == null ? 95 : 100, g == null ? visible : 31, pending == 0, () -> call(source.select(), List.of()));
                tip(g, source.tooltip(), x + 10, top, 95, visible);
                if (g != null) {
                    UiText.scrollingCell(g, font, text.component(source.name()), x + 20, row + 5, 80, 12, UiMenuStyle.TEXT, false);
                    UiText.scrollingCell(g, font, text.component(source.distance()), x + 20, row + 18, 70, 10, UiMenuStyle.TEXT, false);
                }
            } else {
                var unit = setup.units().get(i);
                button(g, unit.select(), "", "", "GFX_tiled_window_small", 0, x + 10, g == null ? top : row, g == null ? 95 : 100, g == null ? visible : 31, pending == 0, () -> call(unit.select(), List.of()));
                tip(g, unit.tooltip(), x + 10, top, 95, visible);
                original(g, "GFX_unit_level", unit.level(), x + 20, row + 10, .5);
                if (g != null) UiAssets.draw(g, unit.icon(), x + 36, row + 10, 59, 21);
                if (g != null) UiText.scrollingCell(g, font, text.component(unit.name()), x + 13, row, 93, 10, UiMenuStyle.TEXT, true);
                label(g, unit.modifier(), x + 75, row + 12, 25, 10, UiMenuStyle.TEXT);
            }
        }
        if (g != null) { g.disableScissor(); setupBar().draw(g, scroll); }
    }
    private void window(GuiGraphicsExtractor g) {
        var w = view.window(); int x = windowX, y = windowY;
        image(g, "GFX_raids_launch_bg", 0, x, y, 320, 299);
        action(g, w.close(), "GFX_closebutton", 0, x + 296, y + 13, 16, 16);
        label(g, w.title(), x + 80, y + 9, 160, 15, UiMenuStyle.TEXT);
        label(g, w.type(), x + 60, y + 31, 200, 13, UiMenuStyle.TEXT);
        if (g != null) UiText.scrollingCell(g, font, text.component(w.unit()), x + 10, y + 44, 150, 12, UiMenuStyle.TEXT, true);
        targetLabel(g, w.target(), x + 160, y + 44, 150, 12);
        original(g, "GFX_raids_terrain_" + w.terrain(), 0, x + 8, y + 60, .5);
        unitPicture(g, w.unitIcon(), x + 17, y + 69, y + 60 + UiAssets.imageSize(asset("GFX_raids_terrain_" + w.terrain(), 0))[1] / 4f);
        original(g, w.targetIcon(), 0, x + 265, y + 62, .5);
        label(g, w.chanceHeader(), x + 12, y + 106, 144, 15, UiMenuStyle.TEXT);
        label(g, w.equipmentHeader(), x + 164, y + 106, 144, 15, UiMenuStyle.TEXT);
        original(g, "GFX_success_chance_display", 0, x + 30, y + 129, .5);
        label(g, w.chanceLevel(), x + 32, y + 134, 100, 15, UiMenuStyle.TEXT);
        label(g, w.chance(), x + 32, y + 147, 100, 15, UiMenuStyle.TEXT);
        tip(g, w.chanceTooltip(), x + 12, y + 122, 140, 42);
        original(g, "GFX_raid_disaster_icon", 0, x + 125, y + 169, .2);
        label(g, w.disaster(), x + 132, y + 169, 26, 13, UiMenuStyle.TEXT);
        tip(g, w.disasterTooltip(), x + 122, y + 165, 37, 18);
        tip(g, w.equipmentTooltip(), x + 164, y + 106, 144, 15);
        int[] columns = {169, 232, 271}, widths = {61, 36, 30};
        for (int i = 0; i < 3; i++) label(g, w.equipmentColumns().get(i), x + columns[i], y + 123, widths[i], 10, 0xFF202020);
        equipmentScroll = Math.clamp(equipmentScroll, 0, Math.max(0, w.equipment().size() - 5));
        for (int i = equipmentScroll; i < Math.min(w.equipment().size(), equipmentScroll + 5); i++) {
            var equipment = w.equipment().get(i); int row = y + 134 + (i - equipmentScroll) * 10;
            if (g != null) {
                UiText.scrollingCell(g, font, text.component(equipment.name()), x + 169, row, 61, 10, 0xFF202020, false);
                UiText.cell(g, font, equipment.collected(), x + 232, row, 36, 10, 0xFF202020, true, false);
                UiText.cell(g, font, equipment.required(), x + 271, row, 30, 10, 0xFF202020, true, false);
            }
        }
        label(g, w.riskHeader(), x + 12, y + 184, 144, 15, UiMenuStyle.TEXT);
        label(g, w.preparationHeader(), x + 165, y + 184, 144, 15, UiMenuStyle.TEXT);
        for (int i = 0; i < w.risks().size(); i++) {
            var risk = w.risks().get(i); int rx = x + 32 + i * 38;
            label(g, risk.label(), rx - 10, y + 206, 45, 15, UiMenuStyle.TEXT);
            button(g, risk.select().item(), "", risk.tooltip(), "GFX_raids_toggle_switch", risk.id().equals(w.risk()) ? 1 : 0,
                rx, y + 212, 25, 42, pending == 0 && risk.select().enabled(), () -> call(risk.select().item(), List.of()));
            if (risk.id().equals(w.risk())) image(g, "GFX_raids_toggle_switch", 1, rx, y + 212, 25, 42);
        }
        boolean organizing = w.phase().equals("CREATE") || w.phase().equals("ASSEMBLING");
        label(g, w.remaining().isEmpty() ? w.preparation() : w.remaining(), x + 164, y + 207, 150, 15, UiMenuStyle.TEXT);
        if (organizing) label(g, w.phase().equals("CREATE") ? "" : w.status(), x + 164, y + 225, 150, 15, UiMenuStyle.TEXT);
        else if (!w.progressText().isEmpty()) {
            original(g, "GFX_raids_loading_bar_fill_background", 0, x + 170, y + 226, .5);
            if (g != null) {
                g.enableScissor(x + 170, y + 226, x + 170 + (int)(137 * w.progress()), y + 237);
                original(g, "GFX_raids_loading_bar_fill_fill", 0, x + 170, y + 226, .5); g.disableScissor();
            }
            original(g, "GFX_raids_loading_bar_frame", 0, x + 168, y + 223, .5);
            if (g != null) {
                g.pose().pushMatrix();
                g.pose().translate(0, .5f);
                UiText.scrollingCell(g, font, text.component(w.progressText()), x + 170, y + 223, 137, 14, UiMenuStyle.TEXT, true);
                g.pose().popMatrix();
            }
            tip(g, w.remaining(), x + 164, y + 207, 150, 32);
        }
        label(g, w.intel(), x + 164, y + 242, 150, 15, UiMenuStyle.TEXT);
        tip(g, w.intelTooltip(), x + 164, y + 240, 150, 17);
        if (w.phase().equals("CREATE")) createButton(g, w.create(), x + 100, y + 263);
        else {
            original(g, "GFX_raids_launch_keys", w.keys(), x + 164, y + 261, .5);
            button(g, w.start().item(), "", w.start().tooltip(), "GFX_raids_launch_button_single", 0, x + 211, y + 267, 24, 24,
                pending == 0 && w.start().enabled(), () -> call(w.start().item(), List.of()));
            action(g, w.cancel(), "GFX_button_123x34", 0, x + 242, y + 269, 62, 17);
            if (g != null) UiText.scrollingCell(g, font, text.component(w.autoLabel()), x + 90, y + 270, 45, 15, UiMenuStyle.TEXT, true);
            button(g, w.toggleAuto().item(), "", w.toggleAuto().tooltip(), "GFX_generic_checkbox", w.autoEnabled() ? 1 : 0,
                x + 140, y + 270, 17, 15, pending == 0 && w.toggleAuto().enabled(), () -> call(w.toggleAuto().item(), List.of()));
            var choice = w.autoOptions().stream().filter(value -> value.value().equals(w.autoSelected())).findFirst().orElse(null);
            button(g, "auto-options", "", w.auto().tooltip(), "GFX_generic_background", 0, x + 12, y + 270, 75, 15,
                pending == 0 && w.auto().enabled(), () -> { autoExpanded = !autoExpanded; rebuildWidgets(); });
            if (g != null) UiText.scrollingCell(g, font, text.component(choice == null ? w.autoDisabled() : choice.label()), x + 23, y + 270, 43, 15, UiMenuStyle.TEXT, false);
            original(g, "GFX_expand_button", 0, x + 68, y + 273, .5);
            if (autoExpanded) {
                int dropdownY = Math.min(y + 282, height - 84);
                if (g != null) UiAssets.nineSlice(g, asset("GFX_tiled_window_1b_thin_border", 0), x + 10, dropdownY, 115, 84, 4, .5);
                for (int i = 0; i < w.autoOptions().size(); i++) {
                    var option = w.autoOptions().get(i);
                    button(g, "auto:" + option.value(), "", option.tooltip(), "GFX_mini_tooltip", 0, x + 14, dropdownY + 4 + i * 15, 98, 15,
                        pending == 0 && w.auto().enabled(), () -> call(w.auto().item(), List.of(option.value())));
                    if (g != null) UiText.scrollingCell(g, font, text.component(option.label()), x + 17, dropdownY + 4 + i * 15, 92, 15, UiMenuStyle.TEXT, false);
                }
            }
        }
    }
    private void createButton(GuiGraphicsExtractor g, Action action, int x, int y) {
        button(g, action.item(), "", action.tooltip(), "GFX_play_button", 0, x, y, 120, 30, action.enabled() && pending == 0, () -> call(action.item(), List.of()));
        if (g == null) return;
        var match = TARGET_ICON.matcher(action.label());
        if (!match.find()) { label(g, action.label(), x, y, 120, 30, UiMenuStyle.TEXT); return; }
        var icon = text.component(match.group());
        var caption = text.component(action.label().substring(match.end()));
        float scale = UiText.scale(font);
        int left = x + Math.round((120 - font.width(text.component(action.label())) * scale) / 2);
        int top = y + (30 - UiText.MIN_HEIGHT) / 2;
        var iconBounds = font.prepareText(icon.getVisualOrderText(), 0, 0, UiMenuStyle.TEXT, false, false, 0).bounds();
        var textBounds = font.prepareText(caption.getVisualOrderText(), 0, 0, UiMenuStyle.TEXT, false, false, 0).bounds();
        float offset = iconBounds == null || textBounds == null ? 0 : textBounds.top() + textBounds.height() / 2f - iconBounds.top() - iconBounds.height() / 2f;
        g.pose().pushMatrix(); g.pose().translate(left, top + offset * scale); g.pose().scale(scale);
        g.text(font, icon, 0, 0, UiMenuStyle.TEXT, true); g.pose().popMatrix();
        UiText.text(g, font, caption, left + Math.round(font.width(icon) * scale), top, UiMenuStyle.TEXT);
    }
    private void feedback(GuiGraphicsExtractor g) {
        var w = view.window(); var f = w.feedback(); int x = windowX, y = windowY;
        original(g, "GFX_raids_outcome_bg", 0, x, y, .5);
        original(g, "GFX_raids_terrain_" + w.terrain(), 0, x + 7, y + 34, .5);
        original(g, f.outcome().equals("FAILURE") ? "GFX_province_view_frame_fail" : "GFX_province_view_frame_success", 0, x + 4, y + 28, .5);
        label(g, f.title(), x + 60, y + 12, 200, 15, UiMenuStyle.TEXT);
        label(g, f.header(), x + 65, y + 80, 190, 25, UiMenuStyle.TEXT);
        original(g, "GFX_raid_outcome_" + f.outcome().toLowerCase(Locale.ROOT), 0, x + 45, y + 15, .25);
        original(g, "GFX_raid_outcome_" + f.outcome().toLowerCase(Locale.ROOT), 0, x + 262, y + 15, .25);
        unitPicture(g, w.unitIcon(), x + 17, y + 44, y + 34 + UiAssets.imageSize(asset("GFX_raids_terrain_" + w.terrain(), 0))[1] / 4f);
        if (g != null) {
            UiAssets.draw(g, "country/" + f.actor().toLowerCase(Locale.ROOT) + "/flag", x + 22, y + 82, 31, 20);
            UiAssets.draw(g, "country/" + f.victim().toLowerCase(Locale.ROOT) + "/flag", x + 270, y + 82, 31, 20);
        }
        original(g, "GFX_large_flag_frame", 0, x + 19, y + 81, .4);
        original(g, "GFX_large_flag_frame", 0, x + 267, y + 81, .4);
        original(g, w.targetIcon(), 0, x + 265, y + 37, .5);
        if (g != null) {
            UiText.scrollingCell(g, font, text.component(f.unit()), x + 10, y + 111, 146, 16, UiMenuStyle.TEXT, true);
            targetLabel(g, f.target(), x + 164, y + 111, 144, 16);
        }
        reportBody(g, f.actorEffects(), 0, x + 20, y + 140);
        reportBody(g, f.victimEffects(), 1, x + 173, y + 140);
        action(g, w.close(), "GFX_closebutton", 0, x + 297, y + 12, 16, 16);
        action(g, f.dismiss(), "GFX_button_reset_123x34", 0, x + 128, y + 234, 62, 17);
    }
    private void reportBody(GuiGraphicsExtractor g, String value, int index, int x, int y) {
        var lines = font.split(text.component(value), 126);
        var bar = new UiScrollBar(x + 127, y, y + 85, 85, lines.size() * (font.lineHeight + 1));
        reportBars[index] = bar;
        reportScroll[index] = Math.clamp(reportScroll[index], 0, bar.maximum());
        if (g == null) return;
        g.enableScissor(x, y, x + 126, y + 85);
        for (int i = 0; i < lines.size(); i++) {
            int top = y + i * (font.lineHeight + 1) - reportScroll[index];
            if (top + font.lineHeight > y && top < y + 85) g.text(font, lines.get(i), x, top, UiMenuStyle.TEXT, false);
        }
        g.disableScissor(); bar.draw(g, reportScroll[index]);
    }
    public void extractMapState(GuiGraphicsExtractor g) {
        map.draw(g, view, view != null && view.setup() != null && view.window() == null ? 123 : 0, HudMenuBar.height(width), text);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        hits.clear();
        map.hover(mx, my);
        sources.hover("");
        if (showingSources()) {
            int index = (my - setupY() - 70 + scroll) / 31;
            if (mx >= 13 && mx < 108 && my >= setupY() + 70 && my < setupY() + 215 && index >= 0 && index < view.setup().sources().size())
                sources.hover(view.setup().sources().get(index).state());
            else if (mx >= 123 && my >= HudMenuBar.height(width) && (!overlay || !dev.hoi.client.map.agency.AgencyHud.containsControl(mx, my))) sources.hoverAt(mx, my, width, height);
        }
        if (!overlay) extractMapState(g);
        layout(g);
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable renderable) renderable.extractRenderState(g, mx, my, delta);
        if (!overlay) HudMenuBar.draw(g, width, ClientCampaignHud.hud(), mx, my, 0);
        String tooltip = hits.reversed().stream().filter(hit -> hit.contains(mx, my)).map(Hit::tooltip).findFirst().orElse("");
        if (tooltip.isEmpty() && (view == null || view.window() == null)
                && (!overlay || !dev.hoi.client.map.agency.AgencyHud.containsControl(mx, my))) tooltip = map.tooltip(mx, my);
        if (!tooltip.equals(tooltipText)) { tooltipText = tooltip; tooltipScroll = 0; }
        tooltipBounds = tooltip.isEmpty() ? null : text.tooltip(g, font, tooltip, mx, my, width, height, tooltipScroll);
        if (tooltipBounds != null) tooltipScroll = tooltipBounds.scroll();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.y() < HudMenuBar.height(width)) return false;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && view != null && view.window() == null
                    && hits.stream().noneMatch(hit -> hit.contains(event.x(), event.y()))
                    && (!overlay || !dev.hoi.client.map.agency.AgencyHud.containsControl(event.x(), event.y())) && !map.planContains(event.x(), event.y())) {
                var hit = map.interaction(event.x(), event.y());
                if (hit != null) { call(hit.right(), List.of()); return true; }
            }
            return !overlay || occupies(event.x(), event.y());
        }
        for (int i = 0; i < reportBars.length; i++) {
            var bar = reportBars[i];
            if (bar != null && bar.contains(event.x(), event.y())) {
                scrollingReport = bar.arrow(event.y()) ? -1 : i;
                reportScroll[i] = scrollingReport < 0 ? bar.step(event.y(), reportScroll[i], font.lineHeight + 1) : bar.offset(event.y());
                return true;
            }
        }
        if (view != null && view.window() != null && event.y() >= windowY && event.y() < windowY + 25 && event.x() >= windowX && event.x() < windowX + 290 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = true; dragX = event.x() - windowX; dragY = event.y() - windowY; return true;
        }
        for (var entry : categoryBars.entrySet()) if (entry.getValue().contains(event.x(), event.y())) {
            var bar = entry.getValue();
            scrollingCategory = bar.arrow(event.y()) ? null : entry.getKey();
            categoryOffsets.put(entry.getKey(), scrollingCategory == null ? bar.step(event.y(), categoryOffsets.getOrDefault(entry.getKey(), 0), 20) : bar.offset(event.y()));
            rebuildWidgets(); return true;
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        if (view != null && view.setup() != null && view.window() == null && event.button() == InputConstants.MOUSE_BUTTON_LEFT && setupBar().contains(event.x(), event.y())) {
            scrolling = !setupBar().arrow(event.y()); scroll = scrolling ? setupBar().offset(event.y()) : setupBar().step(event.y(), scroll, 31); rebuildWidgets(); return true;
        }
        if (view != null && view.window() != null) return true;
        if (hits.stream().anyMatch(hit -> hit.contains(event.x(), event.y()))) return true;
        if (overlay && dev.hoi.client.map.agency.AgencyHud.containsControl(event.x(), event.y())) return false;
        if (!map.planContains(event.x(), event.y())) {
            var action = map.interaction(event.x(), event.y());
            if (action != null) { call(action.open(), List.of()); return true; }
        }
        var hit = map.at(event.x(), event.y());
        if (hit == null) return map.planContains(event.x(), event.y());
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) call(event.hasShiftDown() ? hit.target().quick() : hit.target().select(), List.of());
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) { windowX = Math.clamp((int)(event.x() - dragX), 0, Math.max(0, width - windowWidth())); windowY = Math.clamp((int)(event.y() - dragY), HudMenuBar.height(width), Math.max(HudMenuBar.height(width), height - windowHeight())); rebuildWidgets(); return true; }
        if (scrollingReport >= 0) { reportScroll[scrollingReport] = reportBars[scrollingReport].offset(event.y()); return true; }
        if (scrollingCategory != null) {
            var bar = categoryBars.get(scrollingCategory);
            if (bar != null) categoryOffsets.put(scrollingCategory, bar.offset(event.y()));
            rebuildWidgets(); return true;
        }
        if (scrolling) { scroll = setupBar().offset(event.y()); rebuildWidgets(); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return !overlay; boolean active = dragging || scrolling || scrollingCategory != null || scrollingReport >= 0; dragging = false; scrolling = false; scrollingCategory = null; scrollingReport = -1; return active || super.mouseReleased(event); }
    private boolean occupies(double x, double y) {
        return view != null && view.window() != null || hits.stream().anyMatch(hit -> hit.contains(x, y))
                || (map.at(x, y) != null || map.interaction(x, y) != null || map.planContains(x, y)) && (!overlay || !dev.hoi.client.map.agency.AgencyHud.containsControl(x, y));
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (overlay && !occupies(x, y) && (tooltipBounds == null || tooltipBounds.maxScroll() == 0)) return false;
        if (tooltipBounds != null && tooltipBounds.maxScroll() > 0) {
            tooltipScroll = Math.clamp(tooltipScroll - (int)(vertical * (font.lineHeight + 1) * 3), 0, tooltipBounds.maxScroll());
            return true;
        }
        if (report()) {
            for (int i = 0; i < reportBars.length; i++) {
                var bar = reportBars[i];
                if (bar != null && x >= bar.x() - 127 && x < bar.x() + 9 && y >= bar.top() && y < bar.bottom()) {
                    reportScroll[i] = Math.clamp(reportScroll[i] - (int)(vertical * (font.lineHeight + 1) * 3), 0, bar.maximum());
                    return true;
                }
            }
        } else if (view != null && view.window() != null) equipmentScroll = Math.clamp(equipmentScroll - (int)vertical, 0, Math.max(0, view.window().equipment().size() - 5));
        else if (view != null && view.setup() != null && x < 123) scroll = Math.clamp(scroll - (int)(vertical * 31), 0, setupBar().maximum());
        else for (var entry : categoryBars.entrySet()) {
            var bar = entry.getValue();
            if (x >= width - 160 && y >= bar.top() && y < bar.bottom()) {
                categoryOffsets.put(entry.getKey(), Math.clamp(categoryOffsets.getOrDefault(entry.getKey(), 0) - (int)(vertical * 20), 0, bar.maximum()));
                break;
            }
        }
        rebuildWidgets(); return true;
    }
    @Override public void tick() {
        if (closed) return;
        if (!ClientCampaignHud.visible() || view != null && !view.country().equals(ClientCampaignHud.country())) { revoke(); return; }
        if (pending > 0) { if (--pending == 0) rebuildWidgets(); }
        else if (++refresh >= 40) send(RaidPayloads.Kind.REFRESH, "", List.of());
    }
    @Override public void onClose() {
        if (view != null && view.window() != null) { call(view.window().close().item(), List.of()); return; }
        if (view != null && view.setup() != null) { call(view.setup().close().item(), List.of()); return; }
        if (overlay) return;
        send(RaidPayloads.Kind.CLOSE, "", List.of()); closed = true; minecraft.gui.setScreen(new MapCursor());
    }
    @Override public void removed() {
        sources.clear();
        if (!overlay) InputSidebarMovement.release(minecraft);
        if (!closed) {
            closed = true;
            transport.accept(new RaidPayloads.Request(RaidPayloads.Kind.CLOSE, token, view == null ? 0 : view.revision(), "", List.of()));
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean allowsMovement() { return view == null || view.window() == null; }
    @Override public int unitHudLeft() { return width; }
    @Override public boolean keyPressed(KeyEvent event) { return allowsMovement() && InputSidebarMovement.consumes(minecraft, event) || super.keyPressed(event); }
    @Override public boolean keyReleased(KeyEvent event) { return allowsMovement() && InputSidebarMovement.consumes(minecraft, event) || super.keyReleased(event); }
}
