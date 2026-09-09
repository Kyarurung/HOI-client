package dev.hoi.client.screen;

import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.ui.PanelButton;
import dev.hoi.client.ui.HoiMenuButton;
import dev.hoi.client.ui.HoiMenuStyle;
import dev.hoi.client.ui.HoiPanelLayout;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.*;
import dev.hoi.protocol.AgencyView.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;


public final class AgencyScreen extends Screen implements SidebarMovement.Screen {
    private static final int TEXT = 0xFFE0E3DD, GOLD = 0xFFFFAA00, MUTED = 0xFF9AABA8;
    public static final List<String> UPGRADE_ORDER = List.of("foreign_intelligence", "domestic_intelligence", "military_intelligence", "planning_and_direction", "collection", "processing_and_exploitation", "analysis", "dissemination", "humint", "sigint", "masint", "osint", "geoint", "cell_system", "communication_security", "support_services", "enhanced_interrogation_techniques", "termination", "elint", "comint", "advanced_cryptoanalytical_attack_models", "cryptosystem_algorithm_upgrade", "quantum_cryptography");
    private static final String[] GROUPS = {"agents", "upgrades", "cryptology", "operations"};
    private static final String[] LABELS = {"첩보원", "기관 개선", "암호학", "작전"};
    private final Screen parent;
    private final Consumer<AgencyProtocol.Request> transport;
    private final String token;
    private AgencyView view;
    private String group = "operations", selectedId;
    private final List<String> arguments = new ArrayList<>();
    private int pane, scroll, detailScroll, choosing = -1, choiceScroll, pendingTicks, refreshTicks, overlayStart;
    private String searchText = "";
    private EditBox search;

    public AgencyScreen(Screen parent) {
        this(parent, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(AgencyProtocol.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    public AgencyScreen(Screen parent, String token, AgencyView fixture, Consumer<AgencyProtocol.Request> transport) {
        super(Component.literal("정보기관")); this.parent = parent; this.token = token; this.view = fixture; this.transport = transport;
    }
    String session() { return token; }
    void open() { send(AgencyProtocol.Kind.OPEN, "", List.of()); }
    public void update(AgencyView next) {
        if (!next.session().equals(token)) return;
        if (next.country().isEmpty()) { minecraft.gui.setScreen(null); return; }
        if (view != null && next.revision() < view.revision()) return;
        pendingTicks = 0;
        view = next;
        var item = selected();
        if (item == null) { selectedId = null; choosing = -1; }
        else normalizeArguments(item);
        rebuildWidgets();
    }
    private void normalizeArguments(Item item) {
        while (arguments.size() > item.parameters().size()) arguments.removeLast();
        for (int i = 0; i < item.parameters().size(); i++) {
            var choices = item.parameters().get(i).choices();
            if (arguments.size() <= i) arguments.add(choices.isEmpty() ? "" : choices.getFirst().value());
            String value = arguments.get(i);
            if (choices.stream().noneMatch(c -> c.value().equals(value))) arguments.set(i, choices.isEmpty() ? "" : choices.getFirst().value());
        }
    }
    @Override protected void init() {
        overlayStart = 0;
        SidebarMovement.release(minecraft);
        pane = HoiPanelLayout.width(MenuTab.INTELLIGENCE, width);
        addRenderableWidget(new HoiMenuButton("×", pane - 25, 3, 19, 19, this::onClose));
        if (view == null) return;
        if (view.items().stream().anyMatch(i -> i.id().equals("create"))) {
            var create = view.items().stream().filter(i -> i.id().equals("create")).findFirst().orElseThrow();
            var button = new HoiMenuButton("정보기관 창설", 8, 99, pane - 16, 26,
                    () -> send(AgencyProtocol.Kind.CALL, create.id(), List.of()));
            button.active = create.enabled() && pendingTicks == 0; addRenderableWidget(button); return;
        }
        if (view.items().stream().noneMatch(i -> i.id().equals("recruit"))) return;
        view.items().stream().filter(i -> i.id().equals("spy_master")).findFirst().ifPresent(master ->
                addRenderableWidget(new HoiMenuButton("세력 첩보장", "agency/spy_master", false, pane - 39, 40, 29, 29, () -> selectItem(master))));
        int actionWidth = (pane - 20) / 2;
        var improveButton = new HoiMenuButton("첩보기관 개선", 8, 96, actionWidth, 23, () -> changeGroup("upgrades"));
        improveButton.textScale(.8f); addRenderableWidget(improveButton);
        for (int i = 0; i < 2; i++) {
            String id = i == 0 ? "operations" : "cryptology", label = i == 0 ? "작전" : "암호학";
            addRenderableWidget(new HoiMenuButton(label, null, group.equals(id), 8 + i * (pane - 16) / 2, 149, (pane - 20) / 2, 23, () -> changeGroup(id)));
        }
        addRenderableWidget(new HoiMenuButton("모집된 요원", "agency/total_operatives", group.equals("agents"), 8, 122, 28, 23, () -> changeGroup("agents")));
        addRenderableWidget(new HoiMenuButton("적에게 포획된 정보원", "agency/arrested_operatives", false, 43, 122, 28, 23, () -> changeGroup("agents")));
        addRenderableWidget(new HoiMenuButton("적에게 사살당한 정보원", "agency/dead_operatives", false, 78, 122, 28, 23, () -> changeGroup("agents")));
        var recruit = view.items().stream().filter(i -> i.id().equals("recruit")).findFirst().orElseThrow();
        var recruitButton = new HoiMenuButton("정보원 모집", 12 + actionWidth, 96, actionWidth, 23, () -> selectItem(recruit));
        recruitButton.textScale(.8f); addRenderableWidget(recruitButton);
        overlayStart = children().size();
        if (group.equals("upgrades")) {
            for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget button) button.active = false;
            addRenderableWidget(new HoiMenuButton("개선 창 닫기", upgradeX() + upgradeWidth() - 26, upgradeY() + 4, 20, 19, () -> changeGroup("operations")).caption("×"));
        }
        var items = visibleItems();
        boolean grid = group.equals("upgrades");
        int x = grid ? upgradeX() + 8 : 8, y = grid ? upgradeY() + 39 : 180;
        int areaWidth = grid ? upgradeWidth() - 16 : pane - 16;
        int columns = grid ? Math.max(1, Math.min(5, areaWidth / 100)) : 1;
        int pitch = grid ? 48 : 51;
        int contentHeight = grid ? (items.isEmpty() ? 0 : upgradeOffset(items, items.size() - 1, columns) + pitch) : items.size() * pitch;
        scroll = Math.clamp(scroll, 0, Math.max(0, contentHeight - (height - y - 40)));
        if (selectedId == null || !grid) for (int i = 0; i < items.size(); i++) {
            var item = items.get(i); int position = grid ? upgradePosition(items, i, columns) : i;
            int bx = x + (position % columns) * areaWidth / columns, by = y + (grid ? upgradeOffset(items, i, columns) : i * pitch) - scroll;
            if (by < y || by + pitch > height - 37) continue;
            var button = new PanelButton(item.title(), bx, by, areaWidth / columns - 4, pitch - 5, () -> {
                selectItem(item);
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    int w = getWidth(), h = getHeight();
                    g.fillGradient(bx, by, bx + w, by + h, item.enabled() ? 0xFF354036 : 0xFF292C31, 0xFF101519);
                    g.outline(bx, by, w, h, grid ? upgradeColor(item) : isHoveredOrFocused() || item.id().equals(selectedId) ? GOLD : 0xFF62696F);
                    int art = grid ? 24 : 34;
                    int ax = grid ? bx + (w - art) / 2 : bx + 5;
                    if (!UiAssets.draw(g, item.texture(), ax, by + 2, art, art)) UiAssets.draw(g, "menu/intelligence", ax, by + 2, art, art);
                    int tx = grid ? bx + 5 : bx + 44, ty = grid ? by + 29 : by + 7;
                    if (grid) {
                        float scale = Math.min(.8f, (w - 10) / (float)Math.max(1, font.width(item.title())));
                        g.pose().pushMatrix(); g.pose().translate(tx, ty); g.pose().scale(scale);
                        g.text(font, item.title(), 0, 0, TEXT); g.pose().popMatrix();
                        int stages = upgradeStages(item);
                        int completed = (int)Math.round(Math.max(0, item.progress()) * stages);
                        for (int stage = 0; stage < stages; stage++)
                            UiAssets.draw(g, stage < completed ? "agency/stage_complete" : "agency/stage_empty",
                                    bx + (w - stages * 7) / 2 + stage * 7, by + h - 7, 7, 7);
                    } else {
                        g.text(font, trim(item.title(), w - 49), tx, ty, TEXT);
                        g.text(font, trim(item.value(), w - 49), tx, ty + 13, GOLD);
                    }
                    if (!grid && item.progress() >= 0) {
                        g.fill(bx + 3, by + h - 4, bx + w - 3, by + h - 2, 0xFF151A13);
                        g.fill(bx + 3, by + h - 4, bx + 3 + (int)((w - 6) * item.progress()), by + h - 2, 0xFF91AB70);
                    }
                }
            };
            button.active = selectedId == null; addRenderableWidget(button);
        }
        var item = selected();
        if (item != null) {
            int dx = detailX(), dw = detailWidth();
            addRenderableWidget(new PanelButton("닫기", dx + dw - 52, 44, 44, 20, () -> { selectedId = null; choosing = -1; rebuildWidgets(); }));
            if (choosing >= 0) { initChoices(item); return; }
            int bottom = height - 70;
            for (int i = 0; i < item.parameters().size(); i++) {
                final int index = i; var parameter = item.parameters().get(i); String value = arguments.get(i);
                String name = parameter.choices().stream().filter(c -> c.value().equals(value)).map(Choice::label).findFirst().orElse("선택 없음");
                addRenderableWidget(new PanelButton(parameter.label() + ": " + trim(name, dw - 116), dx + 10, bottom - (item.parameters().size() - i) * 25, dw - 20, 22,
                        () -> { choosing = index; searchText = ""; choiceScroll = 0; rebuildWidgets(); }));
            }
            if (!item.button().isBlank()) {
                var action = new PanelButton(pendingTicks > 0 ? "응답 대기…" : item.button(), dx + dw / 2 - 65, bottom, 130, 24,
                        () -> send(AgencyProtocol.Kind.CALL, item.id(), List.copyOf(arguments)));
                action.active = item.enabled() && pendingTicks == 0 && arguments.stream().noneMatch(String::isEmpty);
                addRenderableWidget(action);
            }
        }
    }
    private void initChoices(Item item) {
        int dx = detailX(), dw = detailWidth();
        search = new EditBox(font, dx + 12, 84, dw - 92, 20, Component.literal("검색"));
        search.setMaxLength(128); search.setValue(searchText);
        search.setResponder(value -> { searchText = value; choiceScroll = 0; rebuildWidgets(); });
        addRenderableWidget(search); setInitialFocus(search);
        addRenderableWidget(new PanelButton("뒤로", dx + dw - 74, 84, 62, 20, () -> { choosing = -1; rebuildWidgets(); }));
        var choices = matchingChoices(item);
        int count = Math.max(1, (height - 163) / 25);
        choiceScroll = Math.clamp(choiceScroll, 0, Math.max(0, choices.size() - count));
        for (int i = choiceScroll; i < Math.min(choices.size(), choiceScroll + count); i++) {
            var choice = choices.get(i);
            addRenderableWidget(new PanelButton(trim(choice.label(), dw - 38), dx + 12, 113 + (i - choiceScroll) * 25, dw - 24, 22,
                    () -> { arguments.set(choosing, choice.value()); choosing = -1; rebuildWidgets(); }));
        }
    }
    private List<Choice> matchingChoices(Item item) {
        String query = searchText.toLowerCase(Locale.ROOT);
        return item.parameters().get(choosing).choices().stream().filter(c -> (c.label() + " " + c.value()).toLowerCase(Locale.ROOT).contains(query)).toList();
    }
    private Item selected() { return view == null ? null : view.items().stream().filter(i -> i.id().equals(selectedId)).findFirst().orElse(null); }
    private List<Item> visibleItems() {
        return view.items().stream().filter(i -> i.group().equals(group) && !i.id().equals("spy_master")).sorted(group.equals("upgrades") ? Comparator.comparingInt(AgencyScreen::upgradeOrder) : Comparator.comparingInt(i -> 0)).toList();
    }
    private void changeGroup(String next) { group = next; selectedId = null; choosing = -1; scroll = 0; rebuildWidgets(); }
    private void selectItem(Item item) { selectedId = item.id(); arguments.clear(); normalizeArguments(item); detailScroll = 0; rebuildWidgets(); }
    private int upgradeWidth() { return Math.min(650, width - 16); }
    private int upgradeX() { return (width - upgradeWidth()) / 2; }
    private int upgradeY() { return 38; }
    static int upgradeOrder(Item item) {
        int index = UPGRADE_ORDER.indexOf(item.id().replace("upgrade:", "").toLowerCase(Locale.ROOT));
        return index < 0 ? UPGRADE_ORDER.size() : index;
    }
    static int upgradeColor(Item item) {
        return item.progress() >= 1 ? GOLD : item.enabled() ? 0xFF55FF55 : 0xFFB53543;
    }
    static int upgradeStages(Item item) {
        if (!item.id().startsWith("upgrade:")) return 0;
        int slash = item.value().indexOf('/');
        if (slash < 0) return 0;
        try {
            int count = Integer.parseInt(item.value().substring(slash + 1).trim());
            return count > 0 && count <= 8 ? count : 0;
        } catch (NumberFormatException ignored) { return 0; }
    }
    private static int upgradeCategory(Item item) {
        int index = upgradeOrder(item);
        return index < 3 ? 0 : index < 8 ? 1 : index < 13 ? 2 : index < 18 ? 3 : 4;
    }
    private static int upgradePosition(List<Item> items, int index, int columns) {
        int pos = 0;
        for (int i = 0; i < index; i++) {
            pos++;
            if (upgradeCategory(items.get(i)) != upgradeCategory(items.get(i + 1))) pos = (pos + columns - 1) / columns * columns;
        }
        return pos;
    }
    private static int upgradeOffset(List<Item> items, int index, int columns) {
        int headers = 1;
        for (int i = 1; i <= index; i++) if (upgradeCategory(items.get(i - 1)) != upgradeCategory(items.get(i))) headers++;
        return upgradePosition(items, index, columns) / columns * 48 + headers * 16;
    }
    private int detailX() { return Math.min(pane + 12, width - 260); }
    private int detailWidth() { return Math.min(540, width - detailX() - 12); }
    private void send(AgencyProtocol.Kind kind, String item, List<String> args) {
        if (pendingTicks > 0 && kind == AgencyProtocol.Kind.CALL) return;
        pendingTicks = 100;
        transport.accept(new AgencyProtocol.Request(kind, token, view == null ? 0 : view.revision(), item, args));
        rebuildWidgets();
    }
    @Override public void tick() {
        if (pendingTicks > 0 && --pendingTicks == 0) {

            send(view == null ? AgencyProtocol.Kind.OPEN : AgencyProtocol.Kind.REFRESH, "", List.of());
        } else if (++refreshTicks % 60 == 0 && pendingTicks == 0 && choosing < 0 && selectedId == null) send(AgencyProtocol.Kind.REFRESH, "", List.of());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        HoiMenuStyle.panel(g, 0, 0, pane, height);
        g.text(font, "정보기관", 10, 9, HoiMenuStyle.TEXT);
        String flag = view == null ? "menu/intelligence" : view.country().equals("KOR") ? "country/kor/intelligence" : "menu/intelligence";
        UiAssets.cover(g, "agency/ui/header", 6, 29, pane - 12, 62);
        UiAssets.draw(g, flag, 12, 40, 38, 39);
        g.text(font, trim(view == null ? "불러오는 중…" : view.name(), pane - 98), 56, 46, TEXT);
        g.text(font, trim(view == null ? "" : view.status(), pane - 16), 8, 81, MUTED);
        boolean overlay = view != null && group.equals("upgrades");
        if (overlay) {
            for (int i = 0; i < overlayStart; i++) if (children().get(i) instanceof net.minecraft.client.gui.components.Renderable widget)
                widget.extractRenderState(g, mx, my, delta);
            g.nextStratum();
        }
        if (view != null && group.equals("upgrades") && selectedId == null) {
            int ux = upgradeX(), uw = upgradeWidth();
            HoiMenuStyle.panel(g, ux, upgradeY(), uw, height - upgradeY() - 36);
            g.text(font, "첩보기관 개선", ux + 12, upgradeY() + 10, TEXT);
            var items = visibleItems(); int columns = Math.max(1, Math.min(5, (uw - 16) / 100));
            String[] titles = {"정보공동체", "정보 순환", "정보 수집 분야", "휴민트", "신호 정보"};
            for (int i = 0; i < items.size(); i++) if (i == 0 || upgradeCategory(items.get(i - 1)) != upgradeCategory(items.get(i))) {
                int by = upgradeY() + 39 + upgradeOffset(items, i, columns) - scroll - 16;
                if (by < upgradeY() + 34 || by + 17 > height - 37) continue;
                HoiMenuStyle.metal(g, ux + 8, by, uw - 16, 17);
                g.text(font, titles[upgradeCategory(items.get(i))], ux + 13, by + 4, TEXT);
            }
        }
        var item = selected();
        if (item != null) {
            int dx = detailX(), dw = detailWidth();
            g.fillGradient(dx, 38, dx + dw, height - 36, 0xFF343D46, 0xFF10151B); g.outline(dx, 38, dw, height - 74, 0xFF88919A);
            g.text(font, trim(item.title(), dw - 73), dx + 10, 51, GOLD);
            if (choosing < 0) {
                if (!UiAssets.draw(g, item.texture(), dx + 12, 77, 72, 58)) UiAssets.draw(g, "menu/intelligence", dx + 12, 77, 72, 58);
                g.text(font, trim(item.value(), dw - 109), dx + 97, 86, TEXT);
                if (item.group().equals("operations") && dw >= 360) {
                    String[] phases = {"phase_border", "phase_bribe", "phase_escape"};
                    int artWidth = Math.min(70, (dw - 124) / 3);
                    for (int i = 0; i < phases.length; i++) UiAssets.draw(g, "agency/" + phases[i], dx + 98 + i * (artWidth + 5), 104, artWidth, 37);
                }
                int bottom = height - 82 - item.parameters().size() * 25;
                var lines = font.split(Component.literal(item.detail()), dw - 26);
                detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - Math.max(0, bottom - 148)));
                g.enableScissor(dx + 10, 146, dx + dw - 10, Math.max(146, bottom));
                int y = 148 - detailScroll;
                for (var line : lines) { g.text(font, line, dx + 13, y, TEXT); y += 13; }
                g.disableScissor();
            }
        }
        if (overlay) {
            for (int i = overlayStart; i < children().size(); i++) if (children().get(i) instanceof net.minecraft.client.gui.components.Renderable widget)
                widget.extractRenderState(g, mx, my, delta);
        } else super.extractRenderState(g, mx, my, delta);
        if (!overlay && view != null) {
            String[] ids = {"recruit", "captured_count", "killed_count"};
            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];
                var value = view.items().stream().filter(v -> v.id().equals(id)).map(AgencyView.Item::value).findFirst().orElse("—");
                g.pose().pushMatrix(); g.pose().translate(10 + 35 * i, 139); g.pose().scale(.65f);
                g.text(font, value, 0, 0, TEXT); g.pose().popMatrix();
            }
        }
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (choosing >= 0) choiceScroll -= (int)(vertical * 3);
        else if (selectedId != null) { detailScroll -= (int)(vertical * 26); return true; }
        else scroll -= (int)(vertical * 51);
        rebuildWidgets(); return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && selectedId != null) {
            if (choosing >= 0) choosing = -1; else selectedId = null;
            rebuildWidgets(); return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && group.equals("upgrades")) { changeGroup("operations"); return true; }
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        return super.keyReleased(event);
    }
    @Override public void onClose() {
        transport.accept(new AgencyProtocol.Request(AgencyProtocol.Kind.CLOSE, token, view == null ? 0 : view.revision(), "", List.of()));
        minecraft.gui.setScreen(parent);
    }
    @Override public void removed() { SidebarMovement.release(minecraft); }
    @Override public boolean allowsMovement() { return selectedId == null && !group.equals("upgrades"); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private String trim(String s, int max) { return font.width(s) <= max ? s : font.plainSubstrByWidth(s, Math.max(1, max - 9)) + "…"; }
}
