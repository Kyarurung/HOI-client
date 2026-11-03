package dev.hoi.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.ProtocolRaidPayloads;
import dev.hoi.protocol.ProtocolRaidView;
import dev.hoi.protocol.ProtocolRaidView.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class ScreenRaid extends Screen implements InputSidebarMovement.InputMovementScreen {
    private final String token;
    private final Consumer<ProtocolRaidPayloads.Request> transport;
    private final ScreenDecisionText text = new ScreenDecisionText();
    private final ScreenRaidMap map = new ScreenRaidMap();
    private final Set<String> hidden = new HashSet<>(), collapsed = new HashSet<>();
    private final List<Hit> hits = new ArrayList<>();
    private ProtocolRaidView view;
    private int pending, refresh, scroll, equipmentScroll, windowX, windowY;
    private final Map<String, Integer> categoryOffsets = new HashMap<>();
    private final Map<String, UiScrollBar> categoryBars = new LinkedHashMap<>();
    private String scrollingCategory;
    private boolean sourceTab, autoExpanded, closed, dragging, scrolling, overlay;
    private double dragX, dragY;
    private String tooltipText = "";
    private int tooltipScroll;
    private UiTooltips.ScrolledTooltip tooltipBounds;
    private record Hit(int x, int y, int w, int h, String tooltip) {
        boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }
    public ScreenRaid() {
        this(UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(ProtocolRaidPayloads.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    public ScreenRaid(String token, ProtocolRaidView fixture, Consumer<ProtocolRaidPayloads.Request> transport) {
        super(Component.literal("군사 습격")); this.token = token; this.view = fixture; this.transport = transport;
    }
    public ProtocolRaidView snapshot() { return view; }
    public void overlay() { overlay = true; hidden.clear(); map.clear(); }
    public static void resourcesReloaded() { ScreenRaidMap.resourcesReloaded(); }
    List<ScreenRaidMap.Hit> mapHits() { return map.hits(); }
    public void open() { send(ProtocolRaidPayloads.Kind.OPEN, "", List.of()); }
    public void update(ProtocolRaidView next) {
        if (closed || !token.equals(next.session())) return;
        if (next.country().isEmpty() || view != null && !view.country().equals(next.country())
                || !next.country().equals(ClientCampaignHud.country())) { revoke(); return; }
        if (view != null && next.revision() <= view.revision()) return;
        boolean changed = view == null || !Objects.equals(view.window() == null ? null : view.window().raid(), next.window() == null ? null : next.window().raid());
        view = next; pending = 0; autoExpanded = false; map.clear();
        if (changed) { centerWindow(); equipmentScroll = 0; }
        rebuildWidgets();
    }
    public void revoke() { closed = true; view = null; map.clear(); clearWidgets(); if (!overlay) minecraft.gui.setScreen(null); }
    private void send(ProtocolRaidPayloads.Kind kind, String item, List<String> args) {
        if (closed || kind != ProtocolRaidPayloads.Kind.CLOSE && pending > 0) return;
        pending = 100; refresh = 0;
        transport.accept(new ProtocolRaidPayloads.Request(kind, token, view == null ? 0 : view.revision(), item, args));
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
        send(ProtocolRaidPayloads.Kind.CALL, item, args);
    }
    private void centerWindow() {
        windowX = Math.max(0, (width - 320) / 2);
        windowY = Math.clamp(height / 2 - 200, UiMenuBar.height(width) + 2, Math.max(UiMenuBar.height(width) + 2, height - 299));
    }
    @Override protected void init() {
        if (windowX == 0 && windowY == 0) centerWindow();
        windowX = Math.clamp(windowX, 0, Math.max(0, width - 320));
        windowY = Math.clamp(windowY, UiMenuBar.height(width), Math.max(UiMenuBar.height(width), height - 299));
        var flags = minecraft.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:ui/decision_flags.json"));
        if (flags.isPresent()) try (var reader = flags.get().openAsReader()) { text.flags(com.google.gson.JsonParser.parseReader(reader).getAsJsonObject()); }
        catch (java.io.IOException | RuntimeException invalid) { text.flags(new com.google.gson.JsonObject()); }
        if (!overlay) UiMenuBar.buttons(width, ClientCampaignHud.country(), null, HoiClient::openMenu).forEach(this::addRenderableWidget);
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
    private int setupY() { return Math.clamp(height / 2 - 100, UiMenuBar.height(width) + 2, Math.max(UiMenuBar.height(width) + 2, height - 225)); }
    private UiScrollBar setupBar() {
        int count = view == null || view.setup() == null ? 0 : sourceTab ? view.setup().sources().size() : view.setup().units().size();
        return new UiScrollBar(108, setupY() + 70, setupY() + 215, 145, count * 31);
    }
    private void layout(GuiGraphicsExtractor g) {
        if (view == null) return;
        categoryBars.clear();
        if (view.window() == null) categories(g);
        if (view.setup() != null && view.window() == null) setup(g);
        if (view.window() != null) {
            if (view.window().feedback() != null) feedback(g); else window(g);
        }
    }
    private void categories(GuiGraphicsExtractor g) {
        categoryBars.clear();
        int x = width - 160, y = UiMenuBar.height(width) + 20;
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
        if (!overlay) filters(g);
    }
    private void filters(GuiGraphicsExtractor g) {
        int rows = (view.categories().size() + 1) / 2, x = width - 38, y = height - 118;
        if (g != null) UiAssets.nineSlice(g, asset("GFX_raids_filter_tiled_bg", 0), x, y, 39, rows * 18 + 10, 4, .5);
        original(g, "GFX_raids_filter_header_button", 0, x - 1, y - 19, .5);
        original(g, "GFX_raid_target_icon_generic", 0, x + 12, y - 17, .5);
        tip(g, view.filterTooltip(), x, y - 19, 39, 19);
        for (int i = 0; i < view.categories().size(); i++) {
            var category = view.categories().get(i);
            int bx = x + 3 + i % 2 * 17, by = y + 5 + i / 2 * 18;
            original(g, "GFX_raid_type_background_round", 0, bx, by, .5);
            String icon = category.icon().isEmpty() ? "GFX_raid_category_small_" + category.id() : category.icon();
            if (UiAssets.imageSize(asset(icon, 0))[0] == 0) icon = "GFX_raid_category_small_unknown";
            button(g, "show:" + category.id(), "", hidden.contains(category.id()) ? category.showTooltip() : category.hideTooltip(), icon, 0, bx, by, 16, 18, true, () -> {
                if (!hidden.add(category.id())) hidden.remove(category.id()); map.clear(); rebuildWidgets();
            });
            original(g, "GFX_deploy_priority", hidden.contains(category.id()) ? 2 : 3, bx + 8, by + 12, .325);
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
        button(g, "units", setup.unitTab(), "", "GFX_sort_button_83x29", 0, x + 16, y + 39, 42, 15, true, () -> { sourceTab = false; scroll = 0; rebuildWidgets(); });
        button(g, "sources", setup.sourceTab(), "", "GFX_sort_button_83x29", 0, x + 62, y + 39, 42, 15, true, () -> { sourceTab = true; scroll = 0; rebuildWidgets(); });
        label(g, sourceTab ? setup.sourcePrompt() : setup.unitPrompt(), x + 10, y + 56, 100, 10, UiMenuStyle.TEXT);
        scroll = Math.clamp(scroll, 0, setupBar().maximum());
        int count = sourceTab ? setup.sources().size() : setup.units().size();
        if (g != null) g.enableScissor(x + 9, y + 70, x + 109, y + 215);
        for (int i = 0; i < count; i++) {
            int row = y + 70 + i * 31 - scroll;
            if (row < y + 70 || row + 31 > y + 215) continue;
            if (sourceTab) {
                var source = setup.sources().get(i);
                button(g, source.select(), "", source.tooltip(), "GFX_tiled_window_small", 0, x + 10, row, 100, 31, pending == 0, () -> call(source.select(), List.of()));
                label(g, source.name(), x + 20, row + 5, 80, 12, UiMenuStyle.TEXT);
                label(g, source.distance(), x + 20, row + 18, 70, 10, UiMenuStyle.TEXT);
            } else {
                var unit = setup.units().get(i);
                button(g, unit.select(), "", unit.tooltip(), "GFX_tiled_window_small", 0, x + 10, row, 100, 31, pending == 0, () -> call(unit.select(), List.of()));
                if (g != null) UiText.centeredSingleLine(g, font, text.component(unit.name()), x + 13, row + 2, 93, 12, UiMenuStyle.TEXT);
                original(g, "GFX_unit_level", unit.level(), x + 20, row + 10, .5);
                if (g != null) UiAssets.draw(g, unit.icon(), x + 41, row + 12, 39, 18);
                label(g, unit.modifier(), x + 82, row + 12, 25, 10, UiMenuStyle.TEXT);
            }
        }
        if (g != null) { g.disableScissor(); setupBar().draw(g, scroll); }
    }
    private void window(GuiGraphicsExtractor g) {
        var w = view.window(); int x = windowX, y = windowY;
        image(g, "GFX_raids_launch_bg", 0, x, y, 320, 299);
        action(g, w.close(), "GFX_closebutton", 0, x + 296, y + 13, 16, 16);
        label(g, w.title(), x + 80, y + 9, 160, 15, UiMenuStyle.TEXT);
        label(g, w.type(), x + 60, y + 35, 200, 19, UiMenuStyle.TEXT);
        if (g != null) UiText.leftSingleLine(g, font, text.component(w.unit()), x + 10, y + 48, 150, 15, UiMenuStyle.TEXT);
        label(g, w.target(), x + 160, y + 48, 150, 15, UiMenuStyle.TEXT);
        original(g, "GFX_raids_terrain_" + w.terrain(), 0, x + 8, y + 60, .5);
        if (g != null) UiAssets.draw(g, w.unitIcon(), x + 17, y + 69, 55, 35);
        original(g, w.targetIcon(), 0, x + 265, y + 62, .5);
        label(g, w.chanceHeader(), x + 12, y + 106, 144, 15, UiMenuStyle.TEXT);
        label(g, w.equipmentHeader(), x + 164, y + 106, 144, 15, UiMenuStyle.TEXT);
        original(g, "GFX_success_chance_display", 0, x + 30, y + 129, .5);
        label(g, w.chanceLevel(), x + 32, y + 136, 100, 15, UiMenuStyle.TEXT);
        label(g, w.chance(), x + 32, y + 149, 100, 15, UiMenuStyle.TEXT);
        tip(g, w.chanceTooltip(), x + 12, y + 122, 140, 42);
        original(g, "GFX_raid_disaster_icon", 0, x + 125, y + 169, .2);
        label(g, w.disaster(), x + 132, y + 169, 26, 13, UiMenuStyle.TEXT);
        tip(g, w.disasterTooltip(), x + 122, y + 165, 37, 18);
        tip(g, w.equipmentTooltip(), x + 164, y + 106, 144, 15);
        int[] columns = {165, 230, 271}, widths = {65, 40, 38};
        for (int i = 0; i < 3; i++) label(g, w.equipmentColumns().get(i), x + columns[i], y + 123, widths[i], 10, 0xFF202020);
        equipmentScroll = Math.clamp(equipmentScroll, 0, Math.max(0, w.equipment().size() - 5));
        for (int i = equipmentScroll; i < Math.min(w.equipment().size(), equipmentScroll + 5); i++) {
            var equipment = w.equipment().get(i); int row = y + 134 + (i - equipmentScroll) * 10;
            if (g != null) {
                UiText.left(g, font, text.component(equipment.name()), x + 165, row, 65, 10, 0xFF202020);
                UiText.cell(g, font, equipment.collected(), x + 230, row, 38, 10, 0xFF202020, true, false);
                UiText.cell(g, font, equipment.required(), x + 271, row, 36, 10, 0xFF202020, true, false);
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
        label(g, w.phase().equals("CREATE") ? w.preparation() : w.status(), x + 164, y + 207, 150, 15, UiMenuStyle.TEXT);
        if (!w.progressText().isEmpty()) {
            original(g, "GFX_raids_loading_bar_fill_background", 0, x + 170, y + 226, .5);
            if (g != null) {
                g.enableScissor(x + 170, y + 226, x + 170 + (int)(137 * w.progress()), y + 237);
                original(g, "GFX_raids_loading_bar_fill_fill", 0, x + 170, y + 226, .5); g.disableScissor();
            }
            original(g, "GFX_raids_loading_bar_frame", 0, x + 168, y + 223, .5);
            label(g, w.progressText(), x + 164, y + 225, 150, 15, UiMenuStyle.TEXT);
            tip(g, w.remaining(), x + 164, y + 207, 150, 32);
        }
        label(g, w.intel(), x + 164, y + 242, 150, 15, UiMenuStyle.TEXT);
        tip(g, w.intelTooltip(), x + 164, y + 240, 150, 17);
        if (w.phase().equals("CREATE")) action(g, w.create(), "GFX_play_button", 0, x + 100, y + 263, 120, 30);
        else {
            original(g, "GFX_raids_launch_keys", 0, x + 164, y + 261, .5);
            action(g, w.start(), "GFX_raids_launch_button_single", 0, x + 211, y + 267, 24, 24);
            action(g, w.cancel(), "GFX_button_123x34", 0, x + 242, y + 269, 62, 17);
            label(g, w.autoLabel(), x + 90, y + 272, 45, 15, UiMenuStyle.TEXT);
            var choice = w.autoOptions().stream().filter(value -> value.value().equals(w.autoSelected())).findFirst().orElse(null);
            button(g, "auto-options", choice == null ? "" : choice.label(), w.auto().tooltip(), "GFX_generic_background", 0, x + 12, y + 270, 75, 15,
                pending == 0 && w.auto().enabled(), () -> { autoExpanded = !autoExpanded; rebuildWidgets(); });
            original(g, "GFX_expand_button", 0, x + 75, y + 273, .5);
            if (autoExpanded) {
                if (g != null) UiAssets.nineSlice(g, asset("GFX_tiled_window_1b_thin_border", 0), x + 10, y + 190, 115, 84, 4, .5);
                for (int i = 0; i < w.autoOptions().size(); i++) {
                    var option = w.autoOptions().get(i);
                    button(g, "auto:" + option.value(), option.label(), option.tooltip(), "GFX_mini_tooltip", 0, x + 14, y + 190 + i * 15, 98, 15,
                        pending == 0 && w.auto().enabled(), () -> call(w.auto().item(), List.of(option.value())));
                }
            }
        }
    }
    private void feedback(GuiGraphicsExtractor g) {
        var w = view.window(); var f = w.feedback(); int x = Math.max(0, (width - 350) / 2), y = Math.max(UiMenuBar.height(width), (height - 263) / 2);
        image(g, "GFX_raids_outcome_bg", 0, x, y, 350, 263);
        original(g, "GFX_raids_terrain_" + w.terrain(), 0, x + 7, y + 34, .5);
        original(g, f.outcome().equals("FAILURE") ? "GFX_province_view_frame_fail" : "GFX_province_view_frame_success", 0, x + 4, y + 28, .5);
        label(g, f.name(), x + 60, y + 12, 200, 15, UiMenuStyle.TEXT);
        label(g, f.title(), x + 65, y + 80, 190, 25, UiMenuStyle.TEXT);
        original(g, "GFX_raid_outcome_" + f.outcome().toLowerCase(Locale.ROOT), 0, x + 45, y + 15, .25);
        original(g, "GFX_raid_outcome_" + f.outcome().toLowerCase(Locale.ROOT), 0, x + 262, y + 15, .25);
        if (g != null) {
            UiAssets.draw(g, w.unitIcon(), x + 17, y + 44, 55, 35);
            UiAssets.draw(g, "country/" + f.actor().toLowerCase(Locale.ROOT) + "/flag", x + 22, y + 82, 35, 23);
            UiAssets.draw(g, "country/" + f.victim().toLowerCase(Locale.ROOT) + "/flag", x + 270, y + 82, 35, 23);
        }
        original(g, w.targetIcon(), 0, x + 265, y + 37, .5);
        if (g != null) UiText.leftSingleLine(g, font, text.component(f.unit()), x + 14, y + 114, 140, 15, UiMenuStyle.TEXT);
        label(g, f.target(), x + 165, y + 114, 140, 15, UiMenuStyle.TEXT);
        label(g, f.actorEffects(), x + 20, y + 140, 126, 85, UiMenuStyle.TEXT);
        label(g, f.victimEffects(), x + 173, y + 140, 126, 85, UiMenuStyle.TEXT);
        action(g, w.close(), "GFX_closebutton", 0, x + 297, y + 12, 16, 16);
        action(g, f.dismiss(), "GFX_button_reset_123x34", 0, x + 128, y + 234, 62, 17);
    }
    public void extractMapState(GuiGraphicsExtractor g) {
        map.draw(g, view, hidden, view != null && view.setup() != null && view.window() == null ? 123 : 0, UiMenuBar.height(width));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        hits.clear();
        if (!overlay) extractMapState(g);
        layout(g);
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable renderable) renderable.extractRenderState(g, mx, my, delta);
        if (!overlay) UiMenuBar.draw(g, width, ClientCampaignHud.hud(), mx, my, 0);
        String tooltip = hits.reversed().stream().filter(hit -> hit.contains(mx, my)).map(Hit::tooltip).findFirst().orElse("");
        if (tooltip.isEmpty() && (view == null || view.window() == null)
                && (!overlay || !dev.hoi.client.map.MapAgencyHud.containsControl(mx, my))) tooltip = map.tooltip(mx, my);
        if (!tooltip.equals(tooltipText)) { tooltipText = tooltip; tooltipScroll = 0; }
        tooltipBounds = tooltip.isEmpty() ? null : text.tooltip(g, font, tooltip, mx, my, width, height, tooltipScroll);
        if (tooltipBounds != null) tooltipScroll = tooltipBounds.scroll();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.y() < UiMenuBar.height(width)) return false;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return !overlay || occupies(event.x(), event.y());
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
        if (overlay && dev.hoi.client.map.MapAgencyHud.containsControl(event.x(), event.y())) return false;
        var hit = map.at(event.x(), event.y());
        if (hit == null) return false;
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) call(event.hasShiftDown() ? hit.target().quick() : hit.target().select(), List.of());
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) { windowX = Math.clamp((int)(event.x() - dragX), 0, Math.max(0, width - 320)); windowY = Math.clamp((int)(event.y() - dragY), UiMenuBar.height(width), Math.max(UiMenuBar.height(width), height - 299)); rebuildWidgets(); return true; }
        if (scrollingCategory != null) {
            var bar = categoryBars.get(scrollingCategory);
            if (bar != null) categoryOffsets.put(scrollingCategory, bar.offset(event.y()));
            rebuildWidgets(); return true;
        }
        if (scrolling) { scroll = setupBar().offset(event.y()); rebuildWidgets(); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return !overlay; boolean active = dragging || scrolling || scrollingCategory != null; dragging = false; scrolling = false; scrollingCategory = null; return active || super.mouseReleased(event); }
    private boolean occupies(double x, double y) {
        return view != null && view.window() != null || hits.stream().anyMatch(hit -> hit.contains(x, y))
                || map.at(x, y) != null && (!overlay || !dev.hoi.client.map.MapAgencyHud.containsControl(x, y));
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (overlay && !occupies(x, y) && (tooltipBounds == null || tooltipBounds.maxScroll() == 0)) return false;
        if (tooltipBounds != null && tooltipBounds.maxScroll() > 0) {
            tooltipScroll = Math.clamp(tooltipScroll - (int)(vertical * (font.lineHeight + 1) * 3), 0, tooltipBounds.maxScroll());
            return true;
        }
        if (view != null && view.window() != null) equipmentScroll = Math.clamp(equipmentScroll - (int)vertical, 0, Math.max(0, view.window().equipment().size() - 5));
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
        else if (++refresh >= 40) send(ProtocolRaidPayloads.Kind.REFRESH, "", List.of());
    }
    @Override public void onClose() {
        if (view != null && view.window() != null) { call(view.window().close().item(), List.of()); return; }
        if (view != null && view.setup() != null) { call(view.setup().close().item(), List.of()); return; }
        if (overlay) return;
        send(ProtocolRaidPayloads.Kind.CLOSE, "", List.of()); closed = true; minecraft.gui.setScreen(new ScreenMapCursor());
    }
    @Override public void removed() {
        if (!overlay) InputSidebarMovement.release(minecraft);
        if (!closed) {
            closed = true;
            transport.accept(new ProtocolRaidPayloads.Request(ProtocolRaidPayloads.Kind.CLOSE, token, view == null ? 0 : view.revision(), "", List.of()));
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean allowsMovement() { return view == null || view.window() == null; }
    @Override public int unitHudLeft() { return width; }
    @Override public boolean keyPressed(KeyEvent event) { return allowsMovement() && InputSidebarMovement.consumes(minecraft, event) || super.keyPressed(event); }
    @Override public boolean keyReleased(KeyEvent event) { return allowsMovement() && InputSidebarMovement.consumes(minecraft, event) || super.keyReleased(event); }
}
