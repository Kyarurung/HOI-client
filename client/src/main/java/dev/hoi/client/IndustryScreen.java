package dev.hoi.client;

import dev.hoi.protocol.*;
import dev.hoi.protocol.IndustryView.*;
import static dev.hoi.protocol.IndustryProtocol.Action.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Private country workflows. Controls send choices; production, inventory and training never run here. */
public final class IndustryScreen extends Screen implements SidebarMovement.Screen {
    private static final int TEXT = HoiMenuStyle.TEXT, MUTED = HoiMenuStyle.MUTED;
    private static final int GOOD = 0xFF9EB990, BAD = 0xFFD58C81;
    private final String token;
    private final Consumer<IndustryProtocol.Request> transport;
    private final MenuTab tab;
    private IndustryView view;
    private int pane, top, scroll, secondScroll, pickerScroll, hudScroll, pending, refresh;
    private int designerScroll;
    private int tradeTab = 1, tradeAmount = 1, selectedSlot = -1;
    private boolean supportSlot, designer, closed, background;
    private String group = "all", picker = "", switchLine = "", resource = "IRON", partner = "";
    private String location = "", recruitTemplate = "", draftName = "", localMessage = "";
    private EditBox nameBox;
    private GuiGraphicsExtractor graphics;
    private int mouseX, mouseY;

    public IndustryScreen(MenuTab tab) {
        this(tab, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(IndustryProtocol.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    IndustryScreen(MenuTab tab, String token, IndustryView fixture, Consumer<IndustryProtocol.Request> transport) {
        super(Component.literal(title(tab)));
        this.tab = tab; this.token = token; this.view = fixture; this.transport = transport;
        if (fixture != null && !fixture.locations().isEmpty()) location = fixture.locations().getFirst().id();
    }
    static boolean supports(MenuTab tab) {
        return tab == MenuTab.PRODUCTION || tab == MenuTab.TRADE || tab == MenuTab.LOGISTICS || tab == MenuTab.RECRUITMENT;
    }
    private static String title(MenuTab tab) {
        return switch (tab) { case PRODUCTION -> "생산"; case TRADE -> "경제 · 무역";
            case LOGISTICS -> "군수"; case RECRUITMENT -> "모병 및 배치"; default -> "산업"; };
    }
    void open() { send(OPEN, "", "", 0); }
    void update(IndustryView next) {
        if (closed || !next.session().equals(token)) return;
        if (next.country().isEmpty() || view != null && !next.country().equals(view.country())) {
            closed = true; minecraft.gui.setScreen(null); return;
        }
        if (view != null && next.revision() <= view.revision()) return;
        if (nameBox != null) draftName = nameBox.getValue();
        if (next.draft() != null && (view == null || view.draft() == null)) draftName = next.draft().name();
        if (designer && next.draft() == null) designer = false;
        view = next; pending = 0; localMessage = "";
        if (view.locations().stream().noneMatch(l -> l.id().equals(location)))
            location = view.locations().isEmpty() ? "" : view.locations().getFirst().id();
        rebuildWidgets();
    }
    int panelWidth() { return pane; }
    private boolean compact() { return height - top < 300; }
    private int bottom() { return height - 25; }
    private int rightWidth() { return Math.min(Math.max(176, width * 380 / 2560), width - pane - 8); }
    private boolean modal() { return designer || !picker.isEmpty() || !partner.isEmpty(); }
    @Override protected void init() {
        pane = HoiPanelLayout.width(tab, width); top = HoiMenuBar.height(width);
        graphics = null; nameBox = null;
        HoiMenuBar.buttons(width, view == null ? "" : view.country(), tab, next -> {
            if (next == tab) return;
            if (next == MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(next);
        }).forEach(this::addRenderableWidget);
        if (!modal()) button("닫기", "×", pane - 25, top + 3, 19, 19, true, this::onClose);
        if (view != null) {
            if (!modal()) layout();
            if (designer) layoutDesigner();
            else if (!partner.isEmpty()) layoutContract();
            else if (!picker.isEmpty()) layoutPicker();
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        graphics = g; mouseX = mx; mouseY = my;
        HoiMenuStyle.panel(g, 0, top, pane, height - top);
        text(title(tab), 9, top + 8, pane - 40, TEXT);
        if (view == null) text("국가 현황을 불러오는 중…", 9, top + 37, pane - 18, MUTED);
        else {
            HoiMenuBar.draw(g, width, view.hud(), mx, my, hudScroll);
            background = modal();
            layout();
            background = false;
            if (designer) layoutDesigner();
            else if (!partner.isEmpty()) layoutContract();
            else if (!picker.isEmpty()) layoutPicker();
        }
        String message = !localMessage.isEmpty() ? localMessage : view == null ? "" : view.message();
        if (!message.isEmpty()) {
            HoiMenuStyle.recess(g, 4, height - 24, pane - 8, 21);
            text(message, 8, height - 18, pane - 16, TEXT);
            tip(message, 4, height - 24, pane - 8, 21);
        }
        super.extractRenderState(g, mx, my, delta); graphics = null;
    }
    private void layout() {
        switch (tab) { case PRODUCTION -> production(); case TRADE -> trade();
            case LOGISTICS -> logistics(); case RECRUITMENT -> recruitment(); default -> {} }
    }
    private void production() {
        var economy = view.economy();
        int used = view.lines().stream().filter(l -> !equipment(l.equipment()).naval()).mapToInt(Line::factories).sum();
        int naval = view.lines().stream().filter(l -> equipment(l.equipment()).naval()).mapToInt(Line::factories).sum();
        rail(6, top + 27, pane - 12, 22);
        art("construction/military_factory", 9, top + 29, 18, 17);
        text(used + " / " + economy.military(), 29, top + 34, (pane - 20) / 2 - 22, TEXT);
        art("construction/dockyard", pane / 2, top + 29, 18, 17);
        text(naval + " / " + economy.dockyards(), pane / 2 + 21, top + 34, pane / 2 - 28, TEXT);
        if (!compact()) resourceStrip(top + 52);
        else tip(view.resources().stream().map(r -> r.name()+": "+decimal(r.available())+" / "+decimal(r.demand())).reduce((a,b)->a+"\n"+b).orElse("")
                +"\n"+view.modifiers().stream().map(m->m.name()+": "+percent(m.value())).reduce((a,b)->a+"\n"+b).orElse(""),6,top+27,pane-12,22);
        int count = Math.min(4, Math.max(1, view.modifiers().size())), mw = (pane - 12) / count;
        for (int i = 0; !compact() && i < view.modifiers().size(); i++) {
            var m = view.modifiers().get(i); int x = 6 + i % count * mw, y = top + 91 + i / count * 20;
            recess(x, y, mw - 1, 19);
            text(percent(m.value()), x + 3, y + 6, mw - 6, m.value() < 0 ? BAD : GOOD);
            tip(m.name() + ": " + percent(m.value()) + "\n" + m.detail(), x, y, mw, 20);
        }
        int filterY = top + (compact() ? 53 : 95 + (view.modifiers().size() + count - 1) / count * 20);
        filters(filterY, false);
        button("생산 라인 추가", "+", pane - 29, filterY, 22, 22, true,
                () -> { picker = "equipment"; switchLine = ""; pickerScroll = 0; rebuildWidgets(); });
        var lines = view.lines().stream().filter(l -> group.equals("all") || equipment(l.equipment()).group().equals(group)).toList();
        int start = filterY + 26, h = 79;
        scroll = clampScroll(scroll, lines.size(), h, start);
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i); var e = equipment(line.equipment()); int y = start + i * h - scroll;
            if (!visible(y, h, start)) continue;
            rail(6, y, pane - 12, h - 3);
            imageButton(e.name() + " · 장비 교체", e.texture(), 10, y + 4, 48, 30, true, () -> {
                picker = "equipment"; switchLine = line.id(); pickerScroll = 0; rebuildWidgets();
            });
            text(e.name(), 62, y + 6, pane - 72, TEXT);
            text(decimal(line.daily()) + " / 일", 62, y + 20, pane - 72, GOOD);
            button("공장 줄이기", "−", 10, y + 38, 20, 18, line.factories() > 0,
                    () -> send(ASSIGN, line.id(), "", line.factories() - 1));
            recess(32, y + 38, 42, 18); text(Integer.toString(line.factories()), 36, y + 43, 35, TEXT);
            button("공장 늘리기 · 보유량 안에서 배정", "+", 76, y + 38, 20, 18, line.factories() < line.availableFactories(),
                    () -> send(ASSIGN, line.id(), "", line.factories() + 1));
            button("생산 우선순위 맨 위로", "↑", pane - 51, y + 38, 19, 18, i > 0, () -> send(FIRST, line.id(), "", 0));
            button("생산 라인 삭제", "×", pane - 30, y + 38, 19, 18, true, () -> send(REMOVE, line.id(), "", 0));
            bar(10, y + 62, pane - 20, 5, line.efficiency(), line.shortage() > 0 ? BAD : GOOD);
            tip("생산 효율 " + percent(line.efficiency()) + "\n자원 부족으로 인한 감소 " + percent(line.shortage())
                    + "\n누적 생산 " + decimal(line.progress()) + " · 단가 " + decimal(e.cost()) + " IC\n"
                    + resources(e.resources(), line.factories()) + "\n배정 가능 " + line.availableFactories(), 9, y + 59, pane - 18, 15);
        }
        if (lines.isEmpty()) text("+ 버튼으로 생산 라인을 추가하세요.", 10, start + 12, pane - 20, MUTED);
    }
    private void resourceStrip(int y) {
        int cell = (pane - 12) / Math.max(1, view.resources().size());
        for (int i = 0; i < view.resources().size(); i++) {
            var r = view.resources().get(i); int x = 6 + i * cell;
            recess(x, y, cell - 1, 36); art(resourceArt(r.id()), x + (cell - 16) / 2, y + 2, 16, 16);
            text(integer(r.available() - r.demand()), x + 2, y + 23, cell - 4, r.available() < r.demand() ? BAD : GOOD);
            tip(r.name() + "\n점유지 추출 " + decimal(r.extracted()) + "\n수입 " + decimal(r.imported())
                    + " · 수출 배정 " + decimal(r.exported()) + "\n생산용 " + decimal(r.available()) + " / 수요 " + decimal(r.demand()), x, y, cell, 36);
        }
    }
    private void filters(int y, boolean full) {
        String[] ids = {"all", "infantry", "armor", "air", "navy"};
        String[] names = {"전체 장비", "보병 장비", "기갑 장비", "항공기", "함선"};
        int cell = (pane - (full ? 12 : 40)) / 5;
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i]; int x = 6 + i * cell;
            imageButton(names[i], i == 0 ? "menu/production" : "category/" + id, x, y, cell - 2, 22, true,
                    () -> { group = id; scroll = 0; pickerScroll = 0; rebuildWidgets(); });
            if (graphics != null && group.equals(id)) graphics.outline(x, y, cell - 2, 22, 0xFFAAAAAA);
        }
    }
    private void logistics() {
        resourceStrip(top + 29); filters(top + 69, true);
        var items = view.equipment().stream().filter(e -> group.equals("all") || e.group().equals(group)).toList();
        int start = top + 98, h = 68; scroll = clampScroll(scroll, items.size(), h, start);
        for (int i = 0; i < items.size(); i++) {
            var e = items.get(i); int y = start + i * h - scroll;
            if (!visible(y, h, start)) continue;
            rail(6, y, pane - 12, h - 3); art(e.texture(), 10, y + 4, 46, 26);
            text(e.name(), 60, y + 5, pane - 69, TEXT);
            double daily = view.lines().stream().filter(l -> l.equipment().equals(e.id())).mapToDouble(Line::daily).sum();
            text("+" + decimal(daily) + " / 일", 60, y + 19, pane - 69, GOOD);
            text("보관 " + e.stockpile() + "   훈련 " + e.reserved(), 10, y + 36, pane - 20, TEXT);
            text("지급 " + e.deployed() + "   부족 " + e.deficit(), 10, y + 50, pane - 20, e.deficit() > 0 ? BAD : MUTED);
            tip(e.name() + "\n보관 재고: " + e.stockpile() + "\n훈련에 지급: " + e.reserved() + "\n배치 사단 휴대: " + e.deployed()
                    + "\n충원에 추가로 필요한 수량: " + e.deficit() + "\n" + resources(e.resources(), 1), 6, y, pane - 12, h - 3);
        }
        text("연료 비축 " + decimal(view.economy().fuel()), 10, height - 17, pane - 20, TEXT);
    }
    private void trade() {
        int half = (pane - 14) / 2;
        button("경제", "경제", 6, top + 27, half, 22, true, () -> { tradeTab = 0; scroll = 0; rebuildWidgets(); });
        button("무역", "무역", 8 + half, top + 27, half, 22, true, () -> { tradeTab = 1; scroll = 0; rebuildWidgets(); });
        var e = view.economy();
        if (tradeTab == 0) {
            String[][] rows = {{"실질 GDP", amount(e.gdp())}, {"부채", amount(e.debt())},
                    {"민간공장", "" + e.civilian()}, {"무역 가용 공장", "" + e.freeCivilian()},
                    {"소비재 공장", "" + e.consumer()}, {"군수공장", "" + e.military()}, {"조선소", "" + e.dockyards()},
                    {"전력 공급 / 수요", decimal(e.energy()) + " / " + decimal(e.energyDemand())},
                    {"연료 비축", decimal(e.fuel())}, {"가용 인력", manpower(e.manpower())}};
            int start = top + 55; scroll = clampScroll(scroll, rows.length, 35, start);
            for (int i = 0; i < rows.length; i++) {
                int y = start + i * 35 - scroll; if (!visible(y, 35, start)) continue;
                recess(6, y, pane - 12, 32); text(rows[i][0], 11, y + 4, pane - 22, MUTED);
                text(rows[i][1], 11, y + 18, pane - 22, TEXT);
            }
            return;
        }
        rail(6, top + 54, pane - 12, 21);
        text("민간공장 " + e.civilian() + " · 가용 " + e.freeCivilian(), 10, top + 61, pane - 20, TEXT);
        int labelWidth = compact() ? 0 : 36, cell = Math.max(16, (pane - 12 - labelWidth) / Math.max(1, view.resources().size()));
        String[] labels = {"추출", "수입", "수출", "생산", "잔여"};
        for (int i = 0; i < view.resources().size(); i++) {
            var r = view.resources().get(i); int x = 6 + labelWidth + i * cell;
            imageButton(r.name(), resourceArt(r.id()), x, top + 79, cell - 1, 20, true,
                    () -> { resource = r.id(); scroll = 0; rebuildWidgets(); });
            double[] values = {r.extracted(), r.imported(), -r.exported(), -r.demand(), r.available() - r.demand()};
            for (int j = 0; !compact() && j < values.length; j++) {
                recess(x, top + 102 + j * 13, cell - 1, 12);
                text(integer(values[j]), x + 1, top + 104 + j * 13, cell - 2, values[j] < 0 ? BAD : GOOD);
            }
            tip(r.name() + "\n추출 " + decimal(r.extracted()) + " · 수입 " + decimal(r.imported()) + "\n수출 배정 " + decimal(r.exported())
                    + "\n생산 수요 " + decimal(r.demand()) + " · 잔여 " + decimal(r.available() - r.demand()), x, top + 79, cell, compact() ? 20 : 88);
        }
        for (int j = 0; !compact() && j < labels.length; j++) text(labels[j], 8, top + 104 + j * 13, labelWidth - 2, MUTED);
        int heading = top + (compact() ? 104 : 172);
        text(resourceName(resource) + " 수입 · 민간공장으로 거래", 9, heading, pane - 18, TEXT);
        var contracts = view.trades().stream().filter(t -> t.importing() && t.resource().equals(resource)).toList();
        int start = heading + 17, h = 34;
        scroll = clampScroll(scroll, contracts.size() + view.partners().size(), h, start);
        int index = 0;
        for (var t : contracts) {
            int y = start + index++ * h - scroll; if (!visible(y, h, start)) continue;
            rail(6, y, pane - 12, 31);
            text(partnerName(t.partner()) + " · 계약", 10, y + 4, pane - 43, TEXT);
            text("공장 " + t.factories() + " → " + decimal(t.delivered()), 10, y + 18, pane - 43, GOOD);
            button("수입 계약 취소", "×", pane - 30, y + 6, 19, 19, true, () -> send(CANCEL_TRADE, t.id(), "", 0));
        }
        for (var p : view.partners()) {
            int y = start + index++ * h - scroll; if (!visible(y, h, start)) continue;
            rail(6, y, pane - 12, 31);
            text(p.name(), 10, y + 4, pane - 46, TEXT);
            text("수출 " + decimal(p.exports().getOrDefault(resource, 0.0)) + (p.route() ? "" : " · 경로 없음"), 10, y + 18, pane - 46, MUTED);
            button(p.name() + "에서 수입", "+", pane - 30, y + 6, 19, 19,
                    p.route() && p.exports().getOrDefault(resource, 0.0) > 0 && e.freeCivilian() > 0,
                    () -> { partner = p.id(); tradeAmount = 1; rebuildWidgets(); });
        }
    }
    private void recruitment() {
        int secondX = pane + 3, secondW = rightWidth(), start = top + 89, h = compact() ? 85 : 93;
        if (graphics != null) HoiMenuStyle.panel(graphics, secondX, top, secondW, height - top);
        text("사단 편제", secondX + 8, top + 8, secondW - 16, TEXT);
        recess(6, top + 28, pane - 12, 27);
        text("가용 인력 " + manpower(view.economy().manpower()), 10, top + 37, pane - 20, TEXT);
        button("배치 장소 선택", locationName(location), 6, top + 60, pane - 12, 22, true,
                () -> { picker = "locations"; pickerScroll = 0; rebuildWidgets(); });
        scroll = clampScroll(scroll, view.recruits().size() + view.deployed().size(), h, start);
        int index = 0;
        for (var r : view.recruits()) {
            int y = start + index++ * h - scroll; if (!visible(y, h, start)) continue;
            var t = template(r.template());
            rail(6, y, pane - 12, h - 3);
            text(t.name(), 10, y + 5, pane - 20, TEXT);
            text(locationName(r.location()), 10, y + 18, pane - 20, MUTED);
            bar(10, y + 34, pane - 20, 5, r.progress(), GOOD);
            double manpower = t.manpower() == 0 ? 1 : (double) r.manpower() / t.manpower();
            double equipped = fillRatio(t.equipment(), r.equipment());
            text("인력 " + percent(manpower) + " · 장비 " + percent(equipped), 10, y + 44, pane - 20, equipped < 1 || manpower < 1 ? BAD : GOOD);
            tip("훈련 " + percent(r.progress()) + "\n인력 " + manpower(r.manpower()) + " / " + manpower(t.manpower()) + "\n" + equipmentList(t.equipment(), r.equipment()), 8, y + 30, pane - 16, 29);
            button("훈련 우선순위", priority(r.priority()), 10, y + 64, 43, 20, true, () -> send(PRIORITY, r.id(), "", (r.priority() + 1) % 3));
            button("조기 배치 · 훈련 20% 이상", "배치", 56, y + 64, 43, 20, r.progress() >= .2, () -> send(DEPLOY, r.id(), "", 0));
            button("훈련 취소 · 지급 장비와 인력 반환", "×", pane - 30, y + 64, 19, 20, true, () -> send(CANCEL_RECRUIT, r.id(), "", 0));
        }
        for (var d : view.deployed()) {
            int y = start + index++ * h - scroll; if (!visible(y, h, start)) continue;
            rail(6, y, pane - 12, h - 3);
            art("menu/recruitment", 10, y + 5, 25, 25); text("배치 완료", 40, y + 13, pane - 50, GOOD);
            text(d.name(), 10, y + 36, pane - 20, TEXT);
            text(locationName(d.location()), 10, y + 50, pane - 20, MUTED);
            text("인력 " + manpower(d.manpower()) + " · 장비 " + d.equipment().values().stream().mapToLong(Long::longValue).sum(), 10, y + 66, pane - 20, TEXT);
            tip(equipmentList(d.equipment(), d.equipment()), 6, y, pane - 12, h - 3);
        }
        if (index == 0) text("편제를 골라 훈련을 시작하세요.", 10, start + 10, pane - 20, MUTED);
        button("새 사단 편제 설계", "사단 설계", secondX + 7, top + 29, secondW - 14, 22, !view.templates().isEmpty(),
                () -> loadDesigner(view.templates().getFirst().id()));
        int ts = top + 59, th = 65; secondScroll = clampScroll(secondScroll, view.templates().size(), th, ts);
        for (int i = 0; i < view.templates().size(); i++) {
            var t = view.templates().get(i); int y = ts + i * th - secondScroll;
            if (!visible(y, th, ts)) continue;
            rail(secondX + 6, y, secondW - 12, th - 3);
            if (!t.line().isEmpty()) art(unitTexture(t.line().getFirst(), false), secondX + 10, y + 4, 30, 26);
            text(t.name(), secondX + 43, y + 5, secondW - 53, TEXT);
            text(manpower(t.manpower()) + "명 · " + integer(t.days()) + "일", secondX + 43, y + 20, secondW - 53, MUTED);
            button(t.name() + " 훈련", "훈련", secondX + 10, y + 38, (secondW - 25) / 2, 19, !location.isEmpty(),
                    () -> send(RECRUIT, t.id(), location, 0));
            button(t.name() + " 편제 편집", "편집", secondX + secondW / 2, y + 38, (secondW - 25) / 2, 19, true, () -> loadDesigner(t.id()));
            tip(equipmentList(t.equipment(), Map.of()), secondX + 7, y, secondW - 14, 33);
        }
    }
    private void loadDesigner(String id) {
        selectedSlot = -1; designerScroll = 0; picker = ""; recruitTemplate = id;
        send(LOAD_TEMPLATE, id, "", 0);
        // The server draft must arrive before the designer can issue edits.
    }
    private int modalWidth() { return Math.min(620, width - 16); }
    private int modalX() { return (width - modalWidth()) / 2; }
    private int modalY() { return Math.max(top + 4, (height - 350) / 2); }
    private int modalHeight() { return Math.min(350, height - modalY() - 8); }
    private void layoutDesigner() {
        var t = view.draft(); if (t == null) return;
        int x = modalX(), y = modalY(), w = modalWidth(), h = modalHeight(), left = Math.min(290, w / 2 - 8);
        panel(x, y, w, h); text("사단 설계", x + 8, y + 8, w - 40, TEXT);
        button("편제 창 닫기", "×", x + w - 25, y + 3, 19, 19, true, () -> { designer = false; selectedSlot = -1; rebuildWidgets(); });
        if (graphics == null) {
            nameBox = new EditBox(font, x + 9, y + 29, left - 8, 20, Component.literal("편제 이름"));
            nameBox.setMaxLength(80); nameBox.setValue(draftName); nameBox.setResponder(value -> draftName = value); addRenderableWidget(nameBox);
        }
        text("사단 구성 · " + t.line().size() + "개 대대", x + 10, y + 56, left - 12, MUTED);
        if (selectedSlot < 0) {
            int requiredHeight = 165 + (t.equipment().size() + 1) / 2 * 22;
            designerScroll = Math.clamp(designerScroll, 0, Math.max(0, requiredHeight - (h - 106)));
            int cell = Math.min(40, (left - 45) / 5), sy = y + 73 - designerScroll;
            for (int i = 0; i < 25; i++) {
                int slot = i, bx = x + 10 + i / 5 * cell, by = sy + i % 5 * 25;
                if (by < y + 71 || by + 22 > y + h - 33) continue;
                String unit = i < t.line().size() ? t.line().get(i) : "";
                if (i > t.line().size()) {
                    recess(bx, by, cell - 3, 22); text("·", bx + 10, by + 7, cell - 10, MUTED); continue;
                }
                if (unit.isEmpty()) button("대대 추가", "+", bx, by, cell - 3, 22, true, () -> selectSlot(slot, false));
                else imageButton(unitName(unit, false), unitTexture(unit, false), bx, by, cell - 3, 22, true, () -> selectSlot(slot, false));
            }
            if (designerScroll == 0) text("지원", x + 14 + 5 * cell, sy - 13, 32, MUTED);
            for (int i = 0; i <= Math.min(4, t.support().size()); i++) {
                int slot = i, bx = x + 12 + 5 * cell, by = sy + i * 25;
                if (by < y + 71 || by + 22 > y + h - 33) continue;
                if (i == t.support().size()) button("지원중대 추가", "+", bx, by, 30, 22, true, () -> selectSlot(slot, true));
                else imageButton(unitName(t.support().get(i), true), unitTexture(t.support().get(i), true), bx, by, 30, 22, true, () -> selectSlot(slot, true));
            }
            if (sy + 133 >= y + 71 && sy + 145 < y + h - 33)
                text("훈련 " + integer(t.days()) + "일 · 인력 " + manpower(t.manpower()), x + 10, sy + 133, left - 12, TEXT);
            var needs = t.equipment().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
            for (int i = 0; i < needs.size(); i++) {
                var need = needs.get(i); int ex = x + 10 + i % 2 * (left / 2), ey = sy + 154 + i / 2 * 22;
                if (ey < y + 71 || ey + 20 > y + h - 33) continue;
                art(equipment(need.getKey()).texture(), ex, ey, 24, 18);
                text(integer(need.getValue()), ex + 28, ey + 5, left / 2 - 34, TEXT);
                tip(equipment(need.getKey()).name() + " · 사단당 " + need.getValue() + "개 필요", ex, ey, left / 2 - 4, 20);
            }
        } else {
            var choices = supportSlot ? view.companies() : view.battalions();
            button("대대 목록 닫기", "뒤로", x + left - 45, y + 53, 43, 18, true, () -> { selectedSlot = -1; rebuildWidgets(); });
            int start = y + 77, available = Math.max(1, (h - 112) / 29) * 2;
            pickerScroll = Math.clamp(pickerScroll, 0, Math.max(0, (choices.size() + 2 - available + 1) / 2));
            for (int i = 0; i < choices.size() + 1; i++) {
                int row = i / 2 - pickerScroll, bx = x + 9 + i % 2 * (left / 2), by = start + row * 29;
                if (row < 0 || by + 27 > y + h - 32) continue;
                String id = i == 0 ? "" : choices.get(i - 1).id(); String label = i == 0 ? "제거" : choices.get(i - 1).name();
                boolean enabled = i != 0 || selectedSlot < (supportSlot ? t.support().size() : t.line().size()) && (supportSlot || t.line().size() > 1);
                button(label, label, bx, by, left / 2 - 5, 25, enabled, () -> {
                    send(supportSlot ? SUPPORT_SLOT : LINE_SLOT, id, "", selectedSlot); selectedSlot = -1;
                });
            }
        }
        int statsX = x + left + 12, statsWidth = w - left - 21, columns = 2, colWidth = statsWidth / columns;
        recess(statsX, y + 28, statsWidth, h - 63);
        for (int i = 0; i < t.stats().size(); i++) {
            var s = t.stats().get(i); int sx = statsX + (i / 14) * colWidth + 4, sy = y + 35 + i % 14 * 14 - designerScroll;
            if (sy < y + 33 || sy + 11 > y + h - 39) continue;
            String value = s.unit().equals("PEOPLE") ? manpower(s.value()) : s.unit().equals("PERCENT") ? percent(s.value()) : decimal(s.value());
            text(s.name(), sx, sy, colWidth - 41, MUTED); text(value, sx + colWidth - 40, sy, 34, TEXT);
            tip(s.name() + ": " + value, sx, sy - 2, colWidth - 4, 14);
        }
        button("새 편제로 저장 · 기존 사단과 훈련 편제 유지", "편제 저장", x + w / 2 - 65, y + h - 27, 130, 21, !draftName.isBlank(),
                () -> send(SAVE_TEMPLATE, draftName, "", 0));
    }
    private void selectSlot(int slot, boolean support) { selectedSlot = slot; supportSlot = support; pickerScroll = 0; rebuildWidgets(); }
    private void layoutPicker() {
        int w = Math.min(270, width - 16), x = width - pane >= w + 8 ? pane + 4 : (width - w) / 2, y = top + 3;
        panel(x, y, w, height - y - 27);
        text(picker.equals("locations") ? "배치 장소 선택" : "생산 장비 선택", x + 9, y + 8, w - 40, TEXT);
        button("목록 닫기", "×", x + w - 25, y + 3, 19, 19, true, () -> { picker = ""; rebuildWidgets(); });
        int start = y + 29;
        if (picker.equals("equipment")) {
            var items = view.equipment().stream().filter(e -> e.unlocked() && (group.equals("all") || e.group().equals(group)))
                    .filter(e -> switchLine.isEmpty() || view.lines().stream().anyMatch(l -> l.id().equals(switchLine) && equipment(l.equipment()).naval() == e.naval())).toList();
            pickerScroll = clampScroll(pickerScroll, items.size(), 55, start);
            for (int i = 0; i < items.size(); i++) {
                var e = items.get(i); int by = start + i * 55 - pickerScroll; if (!visible(by, 55, start)) continue;
                imageButton(e.name(), e.texture(), x + 7, by, 55, 45, true, () -> chooseEquipment(e.id()));
                text(e.name(), x + 67, by + 4, w - 76, TEXT);
                text(decimal(e.cost()) + " IC · 재고 " + e.stockpile(), x + 67, by + 18, w - 76, MUTED);
                button("생산: " + e.name(), switchLine.isEmpty() ? "생산" : "교체", x + 67, by + 30, w - 76, 19, true, () -> chooseEquipment(e.id()));
                tip(resources(e.resources(), 1), x + 6, by, w - 12, 28);
            }
        } else {
            pickerScroll = clampScroll(pickerScroll, view.locations().size(), 25, start);
            for (int i = 0; i < view.locations().size(); i++) {
                var l = view.locations().get(i); int by = start + i * 25 - pickerScroll;
                if (!visible(by, 25, start)) continue;
                button(l.name(), l.name(), x + 7, by, w - 14, 22, true, () -> { location = l.id(); picker = ""; rebuildWidgets(); });
            }
        }
    }
    private void chooseEquipment(String id) {
        if (switchLine.isEmpty()) send(ADD, id, "", 0); else send(SWITCH, switchLine, id, 0);
        picker = "";
    }
    private void layoutContract() {
        int w = Math.min(290, width - 16), x = (width - w) / 2, y = Math.max(top + 4, height / 2 - 65);
        panel(x, y, w, 130); text(partnerName(partner) + " · " + resourceName(resource) + " 수입", x + 9, y + 8, w - 40, TEXT);
        button("계약 창 닫기", "×", x + w - 25, y + 3, 19, 19, true, () -> { partner = ""; rebuildWidgets(); });
        text("배정할 민간공장 · 가용 " + view.economy().freeCivilian(), x + 10, y + 35, w - 20, MUTED);
        button("무역 공장 줄이기", "−", x + 10, y + 54, 24, 23, tradeAmount > 1, () -> { tradeAmount--; rebuildWidgets(); });
        text(Integer.toString(tradeAmount), x + 48, y + 62, w - 96, TEXT);
        button("무역 공장 늘리기", "+", x + w - 34, y + 54, 24, 23, tradeAmount < view.economy().freeCivilian(), () -> { tradeAmount++; rebuildWidgets(); });
        button("수입 계약 체결", "수입 계약", x + 10, y + 96, w - 20, 23, tradeAmount <= view.economy().freeCivilian(), () -> {
            send(TRADE, partner, resource, tradeAmount); partner = "";
        });
    }
    private void send(IndustryProtocol.Action action, String item, String other, int amount) {
        if (closed || pending > 0 && action != CLOSE) return;
        pending = 100;
        transport.accept(new IndustryProtocol.Request(action, token, view == null ? 0 : view.revision(), item, other, amount));
        rebuildWidgets();
    }
    @Override public void tick() {
        if (pending == 0 && view != null && view.draft() != null && !recruitTemplate.isEmpty()) {
            SidebarMovement.release(minecraft); designer = true; recruitTemplate = ""; draftName = view.draft().name(); rebuildWidgets();
        }
        if (pending > 0 && --pending == 0) {
            localMessage = "응답을 확인하고 있습니다."; send(view == null ? OPEN : REFRESH, "", "", 0);
        } else if (++refresh % 60 == 0 && pending == 0) send(view == null ? OPEN : REFRESH, "", "", 0);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (y < top && view != null) { hudScroll = HoiMenuBar.scroll(width, view.hud(), hudScroll, horizontal, vertical); return true; }
        if (designer && selectedSlot >= 0) pickerScroll -= (int) vertical;
        else if (designer) designerScroll -= (int) (vertical * 25);
        else if (!picker.isEmpty()) pickerScroll -= (int) (vertical * 55);
        else if (modal()) return super.mouseScrolled(x, y, horizontal, vertical);
        else if (tab == MenuTab.RECRUITMENT && x >= pane) secondScroll -= (int) (vertical * 65);
        else scroll -= (int) (vertical * (tab == MenuTab.PRODUCTION ? 79 : tab == MenuTab.RECRUITMENT ? (compact() ? 85 : 93) : tab == MenuTab.LOGISTICS ? 68 : tradeTab == 0 ? 35 : 34));
        rebuildWidgets(); return true;
    }
    @Override public void removed() {
        SidebarMovement.release(minecraft); closed = true;
        transport.accept(new IndustryProtocol.Request(CLOSE, token, view == null ? 0 : view.revision(), "", "", 0));
    }
    @Override public boolean allowsMovement() { return !modal(); }
    @Override public boolean keyPressed(KeyEvent e) { return !modal() && SidebarMovement.consumes(minecraft, e) || super.keyPressed(e); }
    @Override public boolean keyReleased(KeyEvent e) { return !modal() && SidebarMovement.consumes(minecraft, e) || super.keyReleased(e); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private Equipment equipment(String id) { return view.equipment().stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow(); }
    private Template template(String id) { return view.templates().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow(); }
    private String partnerName(String id) { return view.partners().stream().filter(p -> p.id().equals(id)).map(Partner::name).findFirst().orElse(id); }
    private String locationName(String id) { return view.locations().stream().filter(l -> l.id().equals(id)).map(Choice::name).findFirst().orElse(id); }
    private String resourceName(String id) { return view.resources().stream().filter(r -> r.id().equals(id)).map(Resource::name).findFirst().orElse(id); }
    private String unitName(String id, boolean support) { return (support ? view.companies() : view.battalions()).stream().filter(c -> c.id().equals(id)).map(Choice::name).findFirst().orElse(id); }
    private String unitTexture(String id, boolean support) { return (support ? view.companies() : view.battalions()).stream().filter(c -> c.id().equals(id)).map(Choice::texture).findFirst().orElse("menu/recruitment"); }
    private String resources(Map<String,Double> resources, int count) {
        return resources.entrySet().stream().map(e -> resourceName(e.getKey()) + " " + decimal(e.getValue() * count)).reduce((a, b) -> a + " · " + b).orElse("자원 소모 없음");
    }
    private String equipmentList(Map<String,Long> need, Map<String,Long> current) {
        return need.entrySet().stream().map(e -> equipment(e.getKey()).name() + ": " + current.getOrDefault(e.getKey(), 0L) + " / " + e.getValue()).reduce((a, b) -> a + "\n" + b).orElse("필요 장비 없음");
    }
    private static double fillRatio(Map<String,Long> need, Map<String,Long> current) {
        return need.entrySet().stream().filter(e -> e.getValue() > 0).mapToDouble(e -> Math.min(1, (double) current.getOrDefault(e.getKey(), 0L) / e.getValue())).min().orElse(1);
    }
    private static String priority(int i) { return switch (i) { case 0 -> "낮음"; case 2 -> "높음"; default -> "보통"; }; }
    private static String resourceArt(String id) { return "production/resources/" + id.toLowerCase(Locale.ROOT); }
    private static String integer(double n) { return String.format(Locale.ROOT, "%.0f", n == 0 ? 0 : n); }
    private static String decimal(double n) { return String.format(Locale.ROOT, "%,.1f", n); }
    private static String percent(double n) { return integer(n * 100) + "%"; }
    private static String manpower(double n) { return HoiMenuBar.number(n); }
    private static String amount(Double n) { return n == null ? "알 수 없음" : decimal(n) + " 십억"; }
    private int clampScroll(int value, int count, int h, int start) { return Math.clamp(value / h, 0, Math.max(0, count - Math.max(1, (bottom() - start) / h))) * h; }
    private boolean visible(int y, int h, int start) { return y >= start && y + h <= bottom(); }
    private void rail(int x, int y, int w, int h) { if (graphics != null) HoiMenuStyle.metal(graphics, x, y, w, h); }
    private void recess(int x, int y, int w, int h) { if (graphics != null) HoiMenuStyle.recess(graphics, x, y, w, h); }
    private void panel(int x, int y, int w, int h) { if (graphics != null) HoiMenuStyle.panel(graphics, x, y, w, h); }
    private void art(String path, int x, int y, int w, int h) { if (graphics != null) UiAssets.draw(graphics, path, x, y, w, h); }
    private void text(String value, int x, int y, int max, int color) {
        if (graphics == null) return;
        String display = font.width(value) <= max ? value : font.plainSubstrByWidth(value, Math.max(1, max - 7)) + "…";
        graphics.text(font, display, x, y, color);
        if (!display.equals(value)) tip(value, x, y - 1, max, 11);
    }
    private void tip(String value, int x, int y, int w, int h) {
        if (graphics != null && !background && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h)
            HoiTooltips.draw(graphics, font, value, mouseX, mouseY);
    }
    private void bar(int x, int y, int w, int h, double value, int color) {
        if (graphics == null) return;
        graphics.fill(x, y, x + w, y + h, 0xFF0B100D);
        graphics.fill(x, y, x + (int) (w * Math.clamp(value, 0, 1)), y + h, color);
    }
    private void button(String accessible, String label, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new ResearchButton(accessible, x, y, w, h, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                HoiMenuStyle.control(g, x, y, w, h, false, active && isHoveredOrFocused());
                String caption = font.plainSubstrByWidth(label, Math.max(1, w - 6));
                g.centeredText(font, caption, x + w / 2, y + (h - 8) / 2, active ? TEXT : MUTED);
            }
        };
        button.active = enabled && pending == 0; button.setTooltip(Tooltip.create(Component.literal(accessible)));
        addRenderableWidget(button);
    }
    private void imageButton(String name, String texture, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new HoiMenuButton(name, texture, false, x, y, w, h, action);
        button.active = enabled && pending == 0; addRenderableWidget(button);
    }
}
