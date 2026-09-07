package dev.hoi.client;

import dev.hoi.protocol.ResearchProtocol;
import dev.hoi.protocol.ResearchView;
import dev.hoi.protocol.ResearchView.Tech;
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
public final class ResearchScreen extends Screen {
    private static final String[] CATEGORIES = {"INFANTRY", "SUPPORT", "ARTILLERY", "ARMOR", "NAVY", "AIR", "ENGINEERING", "INDUSTRY"};
    private static final String[] LABELS = {"보병", "부대지원", "포병", "기갑", "해군", "공군", "공학", "산업"};
    private static final int GOLD = 0xFFE6C779, TEXT = 0xFFE0E6E8, MUTED = 0xFF9BABB4;
    private ResearchView view;
    private ResearchLayout layout;
    private String category = CATEGORIES[0], query = "", detail, focusedTech;
    private int selectedSlot, slotPage, treeTop, treeBottom, panelX, panelY, panelW, panelH, detailScroll, pending;
    private double scrollX, scrollY;
    private boolean panning;
    private EditBox search;
    private final List<Button> baseButtons = new ArrayList<>();

    public ResearchScreen(ResearchView view) { super(Component.literal("HOI · 연구")); this.view = view; }
    public String session() { return view.session(); }
    public void update(ResearchView next) {
        if (next.revision() < view.revision()) return;
        view = next; pending = 0;
        selectedSlot = Math.min(selectedSlot, view.slots().size() - 1);
        if (detail != null && view.technology(detail) == null) detail = null;
        rebuildWidgets();
    }
    @Override protected void init() {
        clearWidgets(); baseButtons.clear();
        int tabColumns = width >= 640 ? 8 : 4, tabWidth = (width - 20) / tabColumns;
        for (int i = 0; i < 8; i++) {
            final int index = i;
            baseButtons.add(button((category.equals(CATEGORIES[i]) ? "• " : "") + LABELS[i],
                    10 + (i % tabColumns) * tabWidth, 30 + (i / tabColumns) * 23, tabWidth - 3, 20, () -> {
                        category = CATEGORIES[index]; scrollX = 0; scrollY = 0; focusedTech = null; detail = null; rebuildWidgets();
                    }));
        }
        int slotY = 35 + ((8 + tabColumns - 1) / tabColumns) * 23;
        int count = slotsPerPage(); slotPage = Math.clamp(slotPage, 0, Math.max(0, (view.slots().size() - 1) / count));
        baseButtons.add(button("<", 10, slotY, 18, 30, () -> { slotPage = Math.max(0, slotPage - 1); rebuildWidgets(); }));
        int slotWidth = (width - 64) / count;
        for (int n = 0; n < count; n++) {
            int index = slotPage * count + n;
            if (index >= view.slots().size()) break;
            var slot = view.slots().get(index); var tech = view.technology(slot.technology());
            String label = (selectedSlot == index ? "• " : "") + (index + 1) + "  " + (tech == null ? "빈 슬롯" : tech.name());
            var slotButton = button(trim(label, slotWidth - 8), 32 + n * slotWidth, slotY, slotWidth - 4, 30, () -> {
                selectedSlot = index;
                if (tech != null) showDetail(tech.id()); else rebuildWidgets();
            });
            if (tech != null) ((ResearchButton)slotButton).progress(tech.fraction());
            slotButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(tech == null
                    ? "빈 슬롯 · 저장된 연구 " + slot.savedDays() + "일"
                    : tech.name() + " · " + String.format(Locale.ROOT, "%.1f%%", tech.fraction() * 100)
                    + " · 현재 속도 기준 약 " + tech.remainingDays(slot.savedDays()) + "일")));
            baseButtons.add(slotButton);
        }
        baseButtons.add(button(">", width - 28, slotY, 18, 30, () -> {
            slotPage = Math.min((view.slots().size() - 1) / count, slotPage + 1); rebuildWidgets();
        }));
        search = new EditBox(font, 10, slotY + 36, Math.min(180, width - 144), 18, Component.literal("기술 검색"));
        search.setMaxLength(80); search.setHint(Component.literal("기술 이름 / ID 검색")); search.setValue(query);
        search.setResponder(value -> { query = value; scrollX = 0; scrollY = 0; relayout(); }); addRenderableWidget(search);
        baseButtons.add(button("처음 위치", width - 130, slotY + 36, 74, 18, () -> { scrollX = 0; scrollY = 0; clearFocus(); }));
        baseButtons.add(button("닫기", width - 52, slotY + 36, 42, 18, this::onClose));
        treeTop = slotY + 76; treeBottom = Math.max(treeTop + 16, height - 36);
        relayout();
        if (detail != null) initDetail();
    }
    private int slotsPerPage() { return Math.max(1, Math.min(view.slots().size(), (width - 64) / 120)); }
    private void relayout() {
        layout = ResearchLayout.create(view.technologies(), category, query); clampScroll();
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
        panelW = Math.min(366, width - 24); panelH = Math.min(350, height - 20);
        panelX = (width - panelW) / 2; panelY = (height - panelH) / 2;
        var tech = view.technology(detail);
        button("×", panelX + panelW - 28, panelY + 8, 20, 20, () -> { detail = null; rebuildWidgets(); });
        button("슬롯 " + (selectedSlot + 1) + " ▸", panelX + 12, panelY + panelH - 52, panelW - 24, 18, () -> {
            selectedSlot = (selectedSlot + 1) % view.slots().size(); rebuildWidgets();
        });
        var activeSlot = view.slots().stream().filter(s -> s.technology().equals(detail)).findFirst();
        String action = tech.status() == ResearchView.Status.ACTIVE ? "연구 중단 · 진행도 보존"
                : tech.status() == ResearchView.Status.COMPLETED ? "연구 완료"
                : tech.status() == ResearchView.Status.LOCKED ? "선행 연구 필요"
                : view.slots().get(selectedSlot).technology().isEmpty() ? "선택 슬롯에서 연구 시작" : "빈 연구 슬롯을 선택하세요";
        var start = button(pending > 0 ? "서버 응답 대기…" : action, panelX + 12, panelY + panelH - 29, panelW - 24, 20, () -> {
            pending = 100;
            HoiClient.send(new ResearchProtocol.Request(activeSlot.isPresent() ? ResearchProtocol.Action.CANCEL : ResearchProtocol.Action.START,
                    view.session(), view.revision(), activeSlot.map(ResearchView.Slot::index).orElse(selectedSlot), detail));
            rebuildWidgets();
        });
        start.active = pending == 0 && (activeSlot.isPresent() || tech.status() == ResearchView.Status.AVAILABLE
                && view.slots().get(selectedSlot).technology().isEmpty());
    }
    @Override public void tick() { if (pending > 0 && --pending == 0) rebuildWidgets(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fillGradient(0, 0, width, height, 0xFF19232A, 0xFF0B1116);
        g.fill(0, 0, width, 26, 0xFF0A1015); g.horizontalLine(10, width - 10, 26, 0xFF756745);
        g.text(font, "HOI  /  연구", 12, 10, GOLD);
        var title = Component.literal(view.countryName()).append(dev.hoi.protocol.CampaignStyle.separator())
                .append(dev.hoi.protocol.CampaignStyle.clock(view.date(), view.speed()));
        g.enableScissor(118, 0, width - 8, 25);
        g.text(font, title, 118, 10, TEXT);
        g.disableScissor();
        g.fill(10, treeTop - 21, width - 10, treeBottom, 0xFF101A21);
        drawTree(g, mouseX, mouseY);
        g.text(font, "완료", 12, height - 24, color(ResearchView.Status.COMPLETED));
        g.text(font, "가능", 44, height - 24, color(ResearchView.Status.AVAILABLE));
        g.text(font, "진행", 76, height - 24, color(ResearchView.Status.ACTIVE));
        g.text(font, "잠김", 108, height - 24, color(ResearchView.Status.LOCKED));
        g.text(font, trim(view.message().isEmpty() ? "휠: 세로  ·  가로 휠/우클릭 드래그: 이동  ·  방향키/Enter: 기술 선택" : view.message(), width - 24), 12, height - 12, MUTED);
        if (detail != null) drawDetail(g);
        super.extractRenderState(g, mouseX, mouseY, delta);
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
            g.verticalLine(x, treeTop, treeBottom, 0xFF24333E);
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
            ResearchIcons.draw(g, tech.category(), x + 8, y + 8, color(tech.status()));
            g.text(font, trim(tech.name(), 111), x + 32, y + 9, TEXT);
            g.text(font, status(tech.status()) + " · " + tech.year(), x + 32, y + 23, color(tech.status()));
            g.fill(x + 8, y + 41, x + 144, y + 45, 0xFF0D151A);
            g.fill(x + 8, y + 41, x + 8 + (int)(136 * tech.fraction()), y + 45, color(tech.status()));
            g.text(font, tech.status() == ResearchView.Status.COMPLETED ? "100%" : "기본 " + tech.baseDays() + "일", x + 8, y + 48, MUTED);
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
        g.nextStratum(); g.fill(0, 0, width, height, 0xB0000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF17242E);
        g.outline(panelX, panelY, panelW, panelH, GOLD);
        var tech = view.technology(detail);
        ResearchIcons.draw(g, tech.category(), panelX + 12, panelY + 12, GOLD);
        g.text(font, trim(tech.name(), panelW - 74), panelX + 34, panelY + 15, GOLD);
        int contentHeight = panelH - 92;
        var lines = detailLines(tech);
        var wrapped = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        for (var line : lines) wrapped.addAll(font.split(Component.literal(line), panelW - 30));
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, wrapped.size() * 13 - contentHeight));
        g.enableScissor(panelX + 10, panelY + 35, panelX + panelW - 10, panelY + panelH - 58);
        int y = panelY + 38 - detailScroll;
        for (var line : wrapped) { g.text(font, line, panelX + 14, y, TEXT); y += 13; }
        g.disableScissor();
        if (wrapped.size() * 13 > contentHeight) {
            int thumb = Math.max(10, contentHeight * contentHeight / (wrapped.size() * 13));
            int pos = detailScroll * (contentHeight - thumb) / Math.max(1, wrapped.size() * 13 - contentHeight);
            g.fill(panelX + panelW - 7, panelY + 35 + pos, panelX + panelW - 4, panelY + 35 + pos + thumb, GOLD);
        }
    }
    private List<String> detailLines(Tech tech) {
        var result = new ArrayList<String>();
        result.add(status(tech.status()) + "  |  " + tech.year() + "년  |  " + tech.tier() + "단계");
        result.add(String.format(Locale.ROOT, "진행도 %.1f%%  ·  기본 연구 기간 %d일", tech.fraction() * 100, tech.baseDays()));
        int bank = tech.status() == ResearchView.Status.ACTIVE ? view.slots().stream().filter(s -> s.technology().equals(tech.id())).findFirst().map(ResearchView.Slot::savedDays).orElse(0)
                : view.slots().get(selectedSlot).savedDays();
        result.add("현재 속도 기준 남은 기간: 약 " + tech.remainingDays(bank) + "일");
        result.add(String.format(Locale.ROOT, "하루 진행량 %.2f · 선택 슬롯 저장 %d일", tech.dailyRate(), bank));
        result.add("시간 경과·연구 보정 변경에 따라 예상 기간이 달라집니다.");
        result.add(" "); result.add("선행 연구");
        if (tech.prerequisites().isEmpty()) result.add("없음");
        for (String id : tech.prerequisites()) {
            var prerequisite = view.technology(id);
            result.add((prerequisite != null && prerequisite.status() == ResearchView.Status.COMPLETED ? "✓ " : "○ ") + (prerequisite == null ? id : prerequisite.name()));
        }
        result.add(" "); result.add("연구 효과"); result.addAll(tech.effects().isEmpty() ? List.of("등록된 국가 보정 없음") : tech.effects());
        result.add(" "); result.add("해금 장비"); result.addAll(tech.unlocks().isEmpty() ? List.of("없음") : tech.unlocks());
        result.add(" "); result.add(tech.id());
        return result;
    }
    private boolean insideTree(double x, double y) { return x >= 10 && x < width - 10 && y >= treeTop && y < treeBottom; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
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
        if (detail != null) { detailScroll -= (int)(vertical * 26); return true; }
        if (insideTree(x, y)) { scrollX -= horizontal * 40; scrollY -= vertical * 40; clampScroll(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && detail != null) { detail = null; rebuildWidgets(); return true; }
        if (detail == null && getFocused() == null && !layout.nodes().isEmpty()) {
            int index = 0;
            for (int i = 0; i < layout.nodes().size(); i++) if (layout.nodes().get(i).tech().id().equals(focusedTech)) index = i;
            if (event.key() == GLFW.GLFW_KEY_ENTER && focusedTech != null) { showDetail(focusedTech); return true; }
            if (event.key() >= GLFW.GLFW_KEY_RIGHT && event.key() <= GLFW.GLFW_KEY_UP) {
                index = Math.floorMod(index + (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_UP ? -1 : 1), layout.nodes().size());
                var node = layout.nodes().get(index); focusedTech = node.tech().id();
                scrollX = node.x() - 20; scrollY = node.y() - 18; clampScroll(); return true;
            }
        }
        return super.keyPressed(event);
    }
    @Override public void removed() { HoiClient.send(new ResearchProtocol.Request(ResearchProtocol.Action.CLOSE, view.session(), 0, -1, "")); }
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
