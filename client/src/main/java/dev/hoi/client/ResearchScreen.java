package dev.hoi.client;

import dev.hoi.protocol.ResearchProtocol;
import dev.hoi.protocol.ResearchView;
import dev.hoi.protocol.ResearchView.Tech;
import dev.hoi.protocol.MenuTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Full-screen research workbench; all strategic state is received from the authoritative server. */
public final class ResearchScreen extends Screen implements SidebarMovement.Screen {
    // NAVAL_SUPPORT is an empty presentation tab until the server provides classified technologies.
    // The authoritative research model still has its original eight categories.
    private static final String[] CATEGORIES = {"INFANTRY", "SUPPORT", "ARMOR", "ARTILLERY", "NAVY", "NAVAL_SUPPORT", "AIR", "ENGINEERING", "INDUSTRY"};
    private static final String[] LABELS = {"보병", "지상 & 항공 지원", "기갑", "포", "해군", "해군 지원 장비", "공군", "공학", "산업"};
    private static final int GOLD = 0xFFE6C779, TEXT = 0xFFE0E6E8, MUTED = 0xFF9BABB4;
    private ResearchView view;
    private ResearchLayout layout;
    private String category = CATEGORIES[0], query = "", detail, focusedTech;
    private int selectedSlot, slotOffset, treeTop, treeBottom, panelX, panelY, panelW, panelH, detailScroll, pending, hudScroll;
    private double scrollX, scrollY;
    private boolean panning, overview = true;
    private final java.util.function.Consumer<ResearchProtocol.Request> requests;
    private final java.util.function.Consumer<MenuTab> menus;
    private EditBox search;
    private final List<Button> baseButtons = new ArrayList<>();

    public ResearchScreen(ResearchView view) { this(view, HoiClient::send); }
    ResearchScreen(ResearchView view, java.util.function.Consumer<ResearchProtocol.Request> requests) {
        this(view, requests, HoiClient::openMenu);
    }
    ResearchScreen(ResearchView view, java.util.function.Consumer<ResearchProtocol.Request> requests, java.util.function.Consumer<MenuTab> menus) {
        super(Component.literal("HOI · 연구")); this.view = view; this.requests = requests; this.menus = menus;
    }
    public String session() { return view.session(); }
    public void update(ResearchView next) {
        if (next.revision() < view.revision()) return;
        view = next; pending = 0;
        selectedSlot = Math.min(selectedSlot, view.slots().size() - 1);
        if (detail != null && view.technology(detail) == null) detail = null;
        rebuildWidgets();
    }
    @Override protected void init() {
        if (!overview) SidebarMovement.release(minecraft);
        clearWidgets(); baseButtons.clear();
        HoiMenuBar.buttons(width, view.country(), MenuTab.RESEARCH, tab -> {
            if (tab == MenuTab.RESEARCH) { overview = true; detail = null; rebuildWidgets(); }
            else menus.accept(tab);
        }).forEach(this::addRenderableWidget);
        if (overview) { initOverview(); return; }
        int tabColumns = 9, tabWidth = (HoiMenuBar.statsRight(width) - 20) / tabColumns;
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int index = i;
            baseButtons.add(addRenderableWidget(new ResearchTabButton(LABELS[i], CATEGORIES[i], category.equals(CATEGORIES[i]),
                    10 + i * tabWidth, menuBottom() + 3, tabWidth + 1, () -> {
                        category = CATEGORIES[index]; scrollX = 0; scrollY = 0; focusedTech = null; detail = null; rebuildWidgets();
                    })));
        }
        int controlsY = menuBottom() + 44;
        search = new EditBox(font, 10, controlsY, Math.min(180, width - 186), 18, Component.literal("기술 검색"));
        search.setMaxLength(80); search.setHint(Component.literal("기술 이름 / ID 검색")); search.setValue(query);
        search.setResponder(value -> { query = value; scrollX = 0; scrollY = 0; relayout(); }); addRenderableWidget(search);
        baseButtons.add(button("처음 위치", width - 168, controlsY, 70, 18, () -> { scrollX = 0; scrollY = 0; clearFocus(); }));
        baseButtons.add(button("슬롯 목록", width - 94, controlsY, 84, 18, () -> { overview = true; rebuildWidgets(); }));
        treeTop = controlsY + 46; treeBottom = Math.max(treeTop + 16, height - 36);
        relayout();
        if (detail != null) initDetail();
    }
    private int visibleSlots() { return 6; }
    private int menuBottom() { return HoiMenuBar.height(width); }
    private int overviewWidth() { return HoiPanelLayout.width(MenuTab.RESEARCH, width); }
    private int slotPitch() { return Math.min(54, Math.max(17, (height - menuBottom() - 96) / visibleSlots())); }
    private int bannerHeight() { return Math.max(28, height - menuBottom() - 68 - visibleSlots() * slotPitch()); }
    private int slotsTop() { return menuBottom() + 60 + bannerHeight(); }
    private void initOverview() {
        int pane = overviewWidth(), count = visibleSlots();
        slotOffset = Math.clamp(slotOffset, 0, Math.max(0, view.slots().size() - count));
        button("×", pane - 28, menuBottom() + 7, 20, 20, this::onClose);
        for (int n = 0; n < count; n++) {
            int index = slotOffset + n;
            if (index >= view.slots().size()) break;
            var slot = view.slots().get(index); var technology = view.technology(slot.technology());
            addRenderableWidget(new ResearchSlotButton(slot, technology, 6, slotsTop() + n * slotPitch(), pane - 12, slotPitch() - 4, () -> {
                selectedSlot = index; overview = false; scrollX = 0; scrollY = 0; focusedTech = null;
                if (technology != null) { category = technology.category(); showDetail(technology.id()); }
                else rebuildWidgets();
            }));
        }
    }
    private void relayout() {
        layout = ResearchPresentation.apply(ResearchLayout.create(view.technologies(), category, query), minecraft.getResourceManager(), width - 20); clampScroll();
    }
    private void clampScroll() {
        scrollX = ResearchLayout.clampScroll(scrollX, layout.width(), width - 20);
        scrollY = ResearchLayout.clampScroll(scrollY, layout.height(), treeBottom - treeTop);
    }
    private Button button(String label, int x, int y, int w, int h, Runnable action) {
        return addRenderableWidget(new ResearchButton(label, x, y, w, h, action));
    }
    private void showDetail(String id) { detail = id; focusedTech = id; detailScroll = 0; rebuildWidgets(); }
    private void initDetail() {
        baseButtons.forEach(b -> { b.active = false; b.visible = false; }); search.setVisible(false);
        panelW = Math.min(366, width - 24); panelH = Math.min(350, height - menuBottom() - 40);
        panelX = Math.min(Math.max(12, width / 5), width - panelW - 12);
        panelY = menuBottom() + 28 + Math.max(0, (height - menuBottom() - 40 - panelH) / 2);
        var tech = view.technology(detail);
        button("×", panelX + panelW - 28, panelY + 8, 20, 20, () -> { detail = null; rebuildWidgets(); });
        button("슬롯 " + (selectedSlot + 1) + " ▸", panelX + 12, panelY + panelH - 28, panelW - 24, 18, () -> {
            selectedSlot = (selectedSlot + 1) % view.slots().size(); rebuildWidgets();
        });
        var activeSlot = view.slots().stream().filter(s -> s.technology().equals(detail)).findFirst();
        String action = tech.status() == ResearchView.Status.ACTIVE ? "연구 중단"
                : tech.status() == ResearchView.Status.COMPLETED ? "연구 완료"
                : tech.status() == ResearchView.Status.LOCKED ? "선행 연구 필요"
                : view.slots().get(selectedSlot).technology().isEmpty() ? "연구" : "빈 슬롯 필요";
        var start = button(pending > 0 ? "응답 대기…" : action, panelX + panelW - 104, panelY + 37, 90, 22, () -> {
            pending = 100;
            requests.accept(new ResearchProtocol.Request(activeSlot.isPresent() ? ResearchProtocol.Action.CANCEL : ResearchProtocol.Action.START,
                    view.session(), view.revision(), activeSlot.map(ResearchView.Slot::index).orElse(selectedSlot), detail));
            rebuildWidgets();
        });
        start.active = pending == 0 && (activeSlot.isPresent() || tech.status() == ResearchView.Status.AVAILABLE
                && view.slots().get(selectedSlot).technology().isEmpty());
    }
    @Override public void tick() { if (pending > 0 && --pending == 0) rebuildWidgets(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (overview) { drawOverview(g); HoiMenuBar.draw(g, width, view.hud(), mouseX, mouseY, hudScroll); super.extractRenderState(g, mouseX, mouseY, delta); return; }
        g.fillGradient(0, 0, width, height, 0xFF1B201E, 0xFF060909);
        g.fill(10, menuBottom() + 38, width - 10, treeBottom, 0xFF101A21);
        g.outline(10, menuBottom() + 38, width - 20, treeBottom - menuBottom() - 38, 0xFF778178);
        drawTree(g, mouseX, mouseY);
        g.text(font, "완료", 12, height - 24, color(ResearchView.Status.COMPLETED));
        g.text(font, "가능", 44, height - 24, color(ResearchView.Status.AVAILABLE));
        g.text(font, "진행", 76, height - 24, color(ResearchView.Status.ACTIVE));
        g.text(font, "잠김", 108, height - 24, color(ResearchView.Status.LOCKED));
        g.text(font, trim(view.message().isEmpty() ? "휠: 세로  ·  가로 휠/우클릭 드래그: 이동  ·  방향키/Enter: 기술 선택" : view.message(), width - 24), 12, height - 12, MUTED);
        if (detail != null) drawDetail(g);
        HoiMenuBar.draw(g, width, view.hud(), mouseX, mouseY, hudScroll);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    private void drawOverview(GuiGraphicsExtractor g) {
        int pane = overviewWidth(), top = menuBottom(), bannerY = top + 39;
        HoiMenuStyle.panel(g, 0, top, pane, height - top);
        g.text(font, "연구", 12, top + 9, HoiMenuStyle.TEXT);
        g.fillGradient(6, bannerY, pane - 6, bannerY + bannerHeight(), 0xFF4E315E, 0xFF172731);
        if (!UiAssets.cover(g, "panel/research_banner", 6, bannerY, pane - 12, bannerHeight())) {
            ResearchIcons.fallback(g, "ENGINEERING", pane / 2 - 10, bannerY + bannerHeight() / 2 - 8, 0xFFBB98CD, 2);
        }
        g.outline(6, bannerY, pane - 12, bannerHeight(), 0xFFA070B5);
        g.text(font, trim("연구 슬롯 " + view.slots().size() + "개", pane - 12), 6, slotsTop() - 13, MUTED);
    }
    private void drawTree(GuiGraphicsExtractor g, int mx, int my) {
        g.enableScissor(10, treeTop - 21, width - 10, treeTop - 1);
        for (int i = 0; i < layout.years().size(); i++) {
            int x = (int)(30 + i * ResearchLayout.COLUMN - scrollX);
            g.text(font, String.valueOf(layout.years().get(i)), x, treeTop - 15, GOLD);
        }
        g.disableScissor();
        g.enableScissor(10, treeTop, width - 10, treeBottom);
        for (int i = 0; i <= layout.years().size(); i++) {
            int x = (int)(20 + i * ResearchLayout.COLUMN - scrollX);
            g.verticalLine(x, treeTop, treeBottom, 0xFF1C2421);
        }
        var byId = new HashMap<String,ResearchLayout.Node>(); layout.nodes().forEach(n -> byId.put(n.tech().id(), n));
        for (var node : layout.nodes()) for (var parentId : node.tech().prerequisites()) {
            var parent = byId.get(parentId); if (parent == null) continue;
            int x1 = (int)(10 + parent.x() + ResearchLayout.CARD_WIDTH - scrollX), y1 = (int)(treeTop + parent.y() + 29 - scrollY);
            int x2 = (int)(10 + node.x() - scrollX), y2 = (int)(treeTop + node.y() + 29 - scrollY);
            int mid = Math.max(x1 + 10, (x1 + x2) / 2);
            int line = parent.tech().status() == ResearchView.Status.COMPLETED ? 0xFF567D57 : 0xFF49525A;
            g.fill(Math.min(x1, mid), y1, Math.max(x1, mid) + 2, y1 + 2, line);
            g.fill(mid, Math.min(y1, y2), mid + 2, Math.max(y1, y2) + 2, line);
            g.fill(Math.min(mid, x2), y2, Math.max(mid, x2) + 2, y2 + 2, line);
        }
        ResearchLayout.Node hovered = null;
        for (var n : layout.nodes()) {
            int x = (int)(10 + n.x() - scrollX), y = (int)(treeTop + n.y() - scrollY);
            if (x + ResearchLayout.CARD_WIDTH < 10 || x > width - 10 || y + ResearchLayout.CARD_HEIGHT < treeTop || y > treeBottom) continue;
            var tech = n.tech(); boolean hover = detail == null && insideTree(mx, my) && n.contains(mx - 10 + scrollX, my - treeTop + scrollY);
            if (hover) hovered = n;
            g.fill(x, y, x + ResearchLayout.CARD_WIDTH, y + ResearchLayout.CARD_HEIGHT, hover ? 0xFF2B3C49 : 0xFF1B2933);
            g.outline(x, y, ResearchLayout.CARD_WIDTH, ResearchLayout.CARD_HEIGHT, focusedTech != null && focusedTech.equals(tech.id()) ? TEXT : color(tech.status()));
            UiAssets.technology(g, tech, x + 24, y + 4, ResearchLayout.CARD_WIDTH - 48, 32, color(tech.status()));
            g.fill(x + 2, y + 38, x + ResearchLayout.CARD_WIDTH - 2, y + 53, 0xFF101713);
            g.centeredText(font, trim(tech.name(), ResearchLayout.CARD_WIDTH - 12), x + ResearchLayout.CARD_WIDTH / 2, y + 41, TEXT);
            g.fill(x + 2, y + 54, x + ResearchLayout.CARD_WIDTH - 2, y + 57, 0xFF0D151A);
            g.fill(x + 2, y + 54, x + 2 + (int)((ResearchLayout.CARD_WIDTH - 4) * tech.fraction()), y + 57, color(tech.status()));
        }
        if (layout.nodes().isEmpty()) g.text(font, "이 분야에 표시할 기술이 없습니다.", 28, treeTop + 28, MUTED);
        g.disableScissor();
        int span = width - 20, visible = treeBottom - treeTop;
        if (layout.width() > span) {
            int thumb = Math.max(12, span * span / layout.width()), pos = (int)(scrollX / (layout.width() - span) * (span - thumb));
            g.fill(10 + pos, treeBottom + 2, 10 + pos + thumb, treeBottom + 5, 0xFF887749);
        }
        if (layout.height() > visible) {
            int thumb = Math.max(10, visible * visible / layout.height()), pos = (int)(scrollY / (layout.height() - visible) * (visible - thumb));
            g.fill(width - 8, treeTop + pos, width - 5, treeTop + pos + thumb, 0xFF887749);
        }
        if (hovered != null) {
            var t = hovered.tech();
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal(t.name()), Component.literal(status(t.status())
                    + " · 현재 속도 기준 약 " + t.remainingDays(view.slots().get(selectedSlot).savedDays()) + "일"),
                    Component.literal("클릭하여 효과 · 선행 연구 · 해금 장비 보기")), mx, my);
        }
    }
    private void drawDetail(GuiGraphicsExtractor g) {
        g.nextStratum(); g.fill(0, menuBottom(), width, height, 0xB0000000);
        g.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF343B43, 0xFF101416);
        g.outline(panelX, panelY, panelW, panelH, 0xFF82909B);
        var tech = view.technology(detail);
        if (panelY >= menuBottom() + 26) {
            int tabX = panelX + 10, tabY = panelY - 25;
            g.fillGradient(tabX, tabY, tabX + 59, panelY + 2, 0xFF4D555B, 0xFF343B43);
            g.horizontalLine(tabX, tabX + 58, tabY, 0xFF82909B);
            g.verticalLine(tabX, tabY, panelY, 0xFF82909B);
            g.verticalLine(tabX + 58, tabY, panelY, 0xFF82909B);
            UiAssets.draw(g, "tabs/" + tech.category().toLowerCase(Locale.ROOT), tabX + 4, tabY + 1, 51, 24);
        }
        g.centeredText(font, trim(tech.name(), panelW - 70), panelX + panelW / 2 - 10, panelY + 14, TEXT);
        g.horizontalLine(panelX + 8, panelX + panelW - 8, panelY + 30, 0xFF657078);
        UiAssets.technology(g, tech, panelX + 12, panelY + 39, 52, 37, GOLD);
        g.fill(panelX + 70, panelY + 37, panelX + panelW - 110, panelY + 59, 0xFF0C1014);
        int bank = view.slots().stream().filter(slot -> slot.technology().equals(tech.id())).findFirst()
                .map(ResearchView.Slot::savedDays).orElse(view.slots().get(selectedSlot).savedDays());
        g.centeredText(font, tech.remainingDays(bank) + "일", panelX + (70 + panelW - 110) / 2, panelY + 44, GOLD);
        g.text(font, status(tech.status()) + " · " + tech.year() + "년", panelX + 70, panelY + 65, MUTED);
        int contentHeight = panelH - 126;
        var lines = detailLines(tech);
        var wrapped = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        for (var line : lines) wrapped.addAll(font.split(Component.literal(line), panelW - 30));
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, wrapped.size() * 13 - contentHeight));
        g.enableScissor(panelX + 10, panelY + 88, panelX + panelW - 10, panelY + panelH - 38);
        int y = panelY + 88 - detailScroll;
        for (var line : wrapped) { g.text(font, line, panelX + 14, y, TEXT); y += 13; }
        g.disableScissor();
        if (wrapped.size() * 13 > contentHeight) {
            int thumb = Math.max(10, contentHeight * contentHeight / (wrapped.size() * 13));
            int pos = detailScroll * (contentHeight - thumb) / Math.max(1, wrapped.size() * 13 - contentHeight);
            g.fill(panelX + panelW - 7, panelY + 88 + pos, panelX + panelW - 4, panelY + 88 + pos + thumb, GOLD);
        }
    }
    private List<String> detailLines(Tech tech) {
        var result = new ArrayList<String>();
        result.add("연구 대상 정보 · " + tech.tier() + "단계");
        result.add(String.format(Locale.ROOT, "진행도 %.1f%%  ·  기본 연구 기간 %d일", tech.fraction() * 100, tech.baseDays()));
        int bank = tech.status() == ResearchView.Status.ACTIVE ? view.slots().stream().filter(s -> s.technology().equals(tech.id())).findFirst().map(ResearchView.Slot::savedDays).orElse(0)
                : view.slots().get(selectedSlot).savedDays();
        result.add("현재 속도 기준 남은 기간: 약 " + tech.remainingDays(bank) + "일");
        result.add(String.format(Locale.ROOT, "하루 진행량 %.2f · 선택 슬롯 저장 %d일", tech.dailyRate(), bank));
        result.add("시간 경과·연구 보정 변경에 따라 예상 기간이 달라집니다.");
        result.add(" "); result.add("업그레이드 · 연구 효과"); result.addAll(tech.effects().isEmpty() ? List.of("등록된 국가 보정 없음") : tech.effects());
        result.add(" "); result.add("해금 장비"); result.addAll(tech.unlocks().isEmpty() ? List.of("없음") : tech.unlocks());
        result.add(" "); result.add("선행 연구");
        if (tech.prerequisites().isEmpty()) result.add("없음");
        for (String id : tech.prerequisites()) {
            var prerequisite = view.technology(id);
            result.add((prerequisite != null && prerequisite.status() == ResearchView.Status.COMPLETED ? "✓ " : "○ ") + (prerequisite == null ? id : prerequisite.name()));
        }
        result.add(" "); result.add(tech.id());
        return result;
    }
    private boolean insideTree(double x, double y) { return !overview && x >= 10 && x < width - 10 && y >= treeTop && y < treeBottom; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (event.y() < menuBottom()) return super.mouseClicked(event, twice);
        if (detail == null && insideTree(event.x(), event.y())) {
            clearFocus();
            if (event.button() == 1 || event.button() == 2) { panning = true; return true; }
            if (event.button() == 0) {
                var node = layout.at(event.x() - 10 + scrollX, event.y() - treeTop + scrollY);
                if (node != null) { showDetail(node.tech().id()); return true; }
            }
        }
        if (detail != null && (event.x() < panelX || event.x() > panelX + panelW || event.y() < panelY || event.y() > panelY + panelH)) return true;
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panning && detail == null) { scrollX -= dx; scrollY -= dy; clampScroll(); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { panning = false; return super.mouseReleased(event); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= 40 && x < HoiMenuBar.statsRight(width) && y >= 0 && y < HoiMenuBar.STATS_HEIGHT) {
            hudScroll = HoiMenuBar.scroll(width, view.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        if (overview && x < overviewWidth() && y >= slotsTop() && view.slots().size() > visibleSlots()) {
            slotOffset -= (int)Math.signum(vertical); rebuildWidgets(); return true;
        }
        if (detail != null) { detailScroll -= (int)(vertical * 26); return true; }
        if (insideTree(x, y)) { scrollX -= horizontal * 40; scrollY -= vertical * 40; clampScroll(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && detail != null) { detail = null; rebuildWidgets(); return true; }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && !overview) { overview = true; rebuildWidgets(); return true; }
        if (!overview && detail == null && getFocused() == null && !layout.nodes().isEmpty()) {
            int index = 0;
            for (int i = 0; i < layout.nodes().size(); i++) if (layout.nodes().get(i).tech().id().equals(focusedTech)) index = i;
            if (event.key() == GLFW.GLFW_KEY_ENTER && focusedTech != null) { showDetail(focusedTech); return true; }
            if (event.key() >= GLFW.GLFW_KEY_RIGHT && event.key() <= GLFW.GLFW_KEY_UP) {
                if (focusedTech != null) index = Math.floorMod(index + (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_UP ? -1 : 1), layout.nodes().size());
                var node = layout.nodes().get(index); focusedTech = node.tech().id();
                scrollX = node.x() - 20; scrollY = node.y() - 18; clampScroll(); return true;
            }
        }
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        return super.keyReleased(event);
    }
    @Override public boolean allowsMovement() { return overview && detail == null; }
    @Override public void removed() { SidebarMovement.release(minecraft); requests.accept(new ResearchProtocol.Request(ResearchProtocol.Action.CLOSE, view.session(), 0, -1, "")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private String trim(String value, int max) { return font.width(value) <= max ? value : font.plainSubstrByWidth(value, Math.max(1, max - font.width("…"))) + "…"; }
    private static String status(ResearchView.Status status) {
        return switch (status) { case LOCKED -> "잠김"; case AVAILABLE -> "연구 가능"; case ACTIVE -> "연구 중"; case COMPLETED -> "연구 완료"; };
    }
    private static int color(ResearchView.Status status) {
        return switch (status) { case LOCKED -> 0xFF77838C; case AVAILABLE -> GOLD; case ACTIVE -> 0xFF74BCD8; case COMPLETED -> 0xFF8DBD81; };
    }
}
