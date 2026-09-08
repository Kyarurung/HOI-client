package dev.hoi.client;

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

/** Operatives, upgrades, ciphers and operation details using server-issued choices. */
public final class AgencyScreen extends Screen implements SidebarMovement.Screen {
    private static final int TEXT = 0xFFE0E3DD, GOLD = 0xFFE6C779, MUTED = 0xFF9AABA8;
    private static final String[] GROUPS = {"agents", "upgrades", "cryptology", "operations"};
    private static final String[] LABELS = {"첩보원", "기관 개선", "암호학", "작전"};
    private final Screen parent;
    private final Consumer<AgencyProtocol.Request> transport;
    private final String token;
    private AgencyView view;
    private String group = "agents", selectedId;
    private final List<String> arguments = new ArrayList<>();
    private int pane, scroll, detailScroll, choosing = -1, choiceScroll, pendingTicks, refreshTicks;
    private String searchText = "", localMessage = "";
    private EditBox search;

    public AgencyScreen(Screen parent) {
        this(parent, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(AgencyProtocol.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    AgencyScreen(Screen parent, String token, AgencyView fixture, Consumer<AgencyProtocol.Request> transport) {
        super(Component.literal("정보기관")); this.parent = parent; this.token = token; this.view = fixture; this.transport = transport;
    }
    String session() { return token; }
    void open() { send(AgencyProtocol.Kind.OPEN, "", List.of()); }
    void update(AgencyView next) {
        if (!next.session().equals(token)) return;
        if (next.country().isEmpty()) { minecraft.gui.setScreen(null); return; }
        if (view != null && next.revision() < view.revision()) return;
        pendingTicks = 0; localMessage = "";
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
        SidebarMovement.release(minecraft);
        pane = HoiPanelLayout.width(MenuTab.INTELLIGENCE, width);
        for (int i = 0; i < GROUPS.length; i++) {
            final int index = i;
            addRenderableWidget(new HoiMenuButton(LABELS[i], null, group.equals(GROUPS[i]), 8 + (i % 2) * (pane - 16) / 2, 96 + (i / 2) * 24, (pane - 20) / 2, 21,
                    () -> { group = GROUPS[index]; selectedId = null; choosing = -1; scroll = 0; rebuildWidgets(); }));
        }
        addRenderableWidget(new HoiMenuButton("×", pane - 25, 3, 19, 19, this::onClose));
        if (view == null) return;
        var items = visibleItems();
        boolean grid = group.equals("upgrades");
        int x = grid ? pane + 12 : 8, y = grid ? 70 : 153;
        int areaWidth = grid ? width - x - 14 : pane - 16;
        int columns = grid ? Math.max(1, areaWidth / 115) : 1;
        int pitch = grid ? 74 : 51, rows = (items.size() + columns - 1) / columns;
        scroll = Math.clamp(scroll, 0, Math.max(0, rows * pitch - (height - y - 40)));
        if (selectedId == null || !grid) for (int i = 0; i < items.size(); i++) {
            var item = items.get(i); int bx = x + (i % columns) * areaWidth / columns, by = y + (i / columns) * pitch - scroll;
            if (by < y || by + pitch > height - 37) continue;
            var button = new ResearchButton(item.title(), bx, by, areaWidth / columns - 4, pitch - 5, () -> {
                selectedId = item.id(); arguments.clear(); normalizeArguments(item); detailScroll = 0; rebuildWidgets();
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    int w = getWidth(), h = getHeight();
                    g.fillGradient(bx, by, bx + w, by + h, item.enabled() ? 0xFF354036 : 0xFF292C31, 0xFF101519);
                    g.outline(bx, by, w, h, isHoveredOrFocused() || item.id().equals(selectedId) ? GOLD : 0xFF62696F);
                    int art = grid ? 32 : 26;
                    if (!UiAssets.draw(g, item.texture(), bx + 5, by + 5, art, art)) UiAssets.draw(g, "menu/intelligence", bx + 5, by + 5, art, art);
                    int tx = grid ? bx + 5 : bx + 36, ty = grid ? by + 41 : by + 7;
                    g.text(font, trim(item.title(), w - (grid ? 10 : 41)), tx, ty, TEXT);
                    g.text(font, trim(item.value(), w - (grid ? 10 : 41)), tx, ty + 13, GOLD);
                    if (item.progress() >= 0) {
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
            addRenderableWidget(new ResearchButton("닫기", dx + dw - 52, 44, 44, 20, () -> { selectedId = null; choosing = -1; rebuildWidgets(); }));
            if (choosing >= 0) { initChoices(item); return; }
            int bottom = height - 70;
            for (int i = 0; i < item.parameters().size(); i++) {
                final int index = i; var parameter = item.parameters().get(i); String value = arguments.get(i);
                String name = parameter.choices().stream().filter(c -> c.value().equals(value)).map(Choice::label).findFirst().orElse("선택 없음");
                addRenderableWidget(new ResearchButton(parameter.label() + ": " + trim(name, dw - 116), dx + 10, bottom - (item.parameters().size() - i) * 25, dw - 20, 22,
                        () -> { choosing = index; searchText = ""; choiceScroll = 0; rebuildWidgets(); }));
            }
            if (!item.button().isBlank()) {
                var action = new ResearchButton(pendingTicks > 0 ? "응답 대기…" : item.button(), dx + dw / 2 - 65, bottom, 130, 24,
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
        addRenderableWidget(new ResearchButton("뒤로", dx + dw - 74, 84, 62, 20, () -> { choosing = -1; rebuildWidgets(); }));
        var choices = matchingChoices(item);
        int count = Math.max(1, (height - 163) / 25);
        choiceScroll = Math.clamp(choiceScroll, 0, Math.max(0, choices.size() - count));
        for (int i = choiceScroll; i < Math.min(choices.size(), choiceScroll + count); i++) {
            var choice = choices.get(i);
            addRenderableWidget(new ResearchButton(trim(choice.label(), dw - 38), dx + 12, 113 + (i - choiceScroll) * 25, dw - 24, 22,
                    () -> { arguments.set(choosing, choice.value()); choosing = -1; rebuildWidgets(); }));
        }
    }
    private List<Choice> matchingChoices(Item item) {
        String query = searchText.toLowerCase(Locale.ROOT);
        return item.parameters().get(choosing).choices().stream().filter(c -> (c.label() + " " + c.value()).toLowerCase(Locale.ROOT).contains(query)).toList();
    }
    private Item selected() { return view == null ? null : view.items().stream().filter(i -> i.id().equals(selectedId)).findFirst().orElse(null); }
    private List<Item> visibleItems() { return view.items().stream().filter(i -> i.group().equals(group)).toList(); }
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
            localMessage = "응답 대기 시간이 지났습니다. 현황을 갱신합니다.";
            // Never repeat a mutation after a timeout.
            send(view == null ? AgencyProtocol.Kind.OPEN : AgencyProtocol.Kind.REFRESH, "", List.of());
        } else if (++refreshTicks % 60 == 0 && pendingTicks == 0 && choosing < 0 && selectedId == null) send(AgencyProtocol.Kind.REFRESH, "", List.of());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        HoiMenuStyle.panel(g, 0, 0, pane, height);
        g.text(font, "정보기관", 10, 9, HoiMenuStyle.TEXT);
        String flag = view == null ? "menu/intelligence" : view.country().equals("KOR") ? "country/kor/intelligence" : "menu/intelligence";
        UiAssets.draw(g, flag, 12, 40, 38, 39);
        g.text(font, trim(view == null ? "불러오는 중…" : view.name(), pane - 62), 56, 46, GOLD);
        g.text(font, trim(view == null ? "" : view.status(), pane - 16), 8, 81, MUTED);
        if (view != null && group.equals("upgrades") && selectedId == null) {
            g.fillGradient(pane + 8, 40, width - 8, height - 36, 0xFF2B3139, 0xFF0F1319);
            g.text(font, "첩보기관 개선", pane + 18, 51, TEXT);
        }
        String message = !localMessage.isBlank() ? localMessage : view == null ? "" : view.message();
        g.fill(0, height - 32, width, height, 0xDD11171C);
        g.text(font, trim(message, width - 16), 8, height - 20, GOLD);
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
        super.extractRenderState(g, mx, my, delta);
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
