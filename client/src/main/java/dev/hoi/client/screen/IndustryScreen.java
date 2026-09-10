package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.ui.PanelButton;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.HoiMenuButton;
import dev.hoi.client.ui.HoiMenuStyle;
import dev.hoi.client.ui.HoiPanelLayout;
import dev.hoi.client.ui.HoiTooltips;
import dev.hoi.client.ui.UiAssets;

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


public final class IndustryScreen extends Screen implements SidebarMovement.Screen {
    private static final int TEXT = HoiMenuStyle.TEXT, MUTED = HoiMenuStyle.MUTED;
    private static final int GOOD = 0xFF55FF55, BAD = 0xFFAA3333, GOLD = 0xFFFFAA00;
    private final String token;
    private final Consumer<IndustryProtocol.Request> transport;
    private final MenuTab tab;
    private IndustryView view;
    private int pane, top, scroll, secondScroll, pickerScroll, hudScroll, pending, refresh;
    private int designerScroll;
    private int tradeTab = 1, tradeAmount = 1, selectedSlot = -1;
    private boolean supportSlot, designer, closed, background, showOutdated;
    private String group = "all", picker = "", switchLine = "", resource = "IRON", partner = "";
    private String location = "", locationRecruit = "", recruitTemplate = "", draftName = "";
    private record DeploymentLocation(int x,int y,int width,int height,String recruit,String label) {}
    private final List<DeploymentLocation> deploymentLocations = new ArrayList<>();
    private boolean selectingDeployment;
    public boolean selectingDeployment() {return selectingDeployment && !closed;}
    public boolean acceptsDeploymentOverlay(String session,long revision) {return selectingDeployment() && token.equals(session) && view != null && revision >= view.revision();}
    private EditBox nameBox;
    private final Set<String> collapsedRecruitment = new HashSet<>();
    private GuiGraphicsExtractor graphics;
    private int mouseX, mouseY;

    public IndustryScreen(MenuTab tab) {
        this(tab, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(IndustryProtocol.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    public IndustryScreen(MenuTab tab, String token, IndustryView fixture, Consumer<IndustryProtocol.Request> transport) {
        super(Component.literal(title(tab)));
        this.tab = tab; this.token = token; this.view = fixture; this.transport = transport;

    }
    public static boolean supports(MenuTab tab) {
        return tab == MenuTab.PRODUCTION || tab == MenuTab.TRADE || tab == MenuTab.LOGISTICS || tab == MenuTab.RECRUITMENT;
    }
    private static String title(MenuTab tab) {
        return switch (tab) { case PRODUCTION -> "생산"; case TRADE -> "경제 · 무역";
            case LOGISTICS -> "군수"; case RECRUITMENT -> "모병 및 배치"; default -> "산업"; };
    }
    public void open() { send(OPEN, "", "", 0); }
    public void update(IndustryView next) {
        if (closed || !next.session().equals(token)) return;
        if (next.country().isEmpty() || view != null && !next.country().equals(view.country())) {
            closed = true; minecraft.gui.setScreen(null);
            if (minecraft.player != null && !next.message().isBlank()) minecraft.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(next.message()));
            return;
        }
        if (view != null && next.revision() <= view.revision()) return;
        if (nameBox != null) draftName = nameBox.getValue();
        if (next.draft() != null && (view == null || view.draft() == null)) draftName = next.draft().name();
        if (designer && next.draft() == null) designer = false;
        view = next; pending = 0;
        if (selectingDeployment && (next.message().equals("배치 장소를 지정했습니다.") || next.recruits().stream().noneMatch(r -> r.id().equals(locationRecruit)))) {
            selectingDeployment = false; dev.hoi.client.map.AtlasSceneClient.clearDeployment();
        }
        if (view.locations().stream().noneMatch(l -> l.id().equals(location)))
            location = "";
        rebuildWidgets();
    }
    int panelWidth() { return pane; }
    private boolean compact() { return height - top < 300; }
    private int bottom() { return height - 25; }
    private int rightWidth() { return Math.min(Math.max(176, width * 380 / 2560), width - pane - 8); }
    private boolean modal() { return designer || !picker.isEmpty() || !partner.isEmpty(); }
    @Override protected void init() {
        pane = HoiPanelLayout.width(tab, width); top = HoiMenuBar.height(width);
        graphics = null; nameBox = null; deploymentLocations.clear();
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
        super.extractRenderState(g, mx, my, delta); graphics = null;
    }
    private void layout() {
        switch (tab) { case PRODUCTION -> production(); case TRADE -> trade();
            case LOGISTICS -> logistics(); case RECRUITMENT -> recruitment(); default -> {} }
    }
    private void production() {
        var economy = view.economy();
        var lines = view.lines();
        int start = top + 155, h = productionRowHeight(start), bodyTop = top + 27;
        if (compact()) scroll = Math.clamp(scroll, 0, Math.max(0, start + lines.size() * h - bottom()));
        else scroll = clampScroll(scroll, lines.size(), h, start);
        int offset = compact() ? scroll : 0;
        if (graphics != null) graphics.enableScissor(0, bodyTop, pane, bottom());
        resourceStrip(top + 27 - offset, 32);
        var modifiers = productionIndicators();
        int cell = (pane - 12) / 3;
        for (int i = 0; i < modifiers.size(); i++) {
            var m = modifiers.get(i); int x = 6 + i % 3 * cell, y = top + 62 + i / 3 * 20 - offset;
            recess(x, y, cell - 1, 19);
            art("production/modifiers/" + m.id(), x + 2, y + 3, 14, 12);
            text(percent(m.value()), x + 18, y + 6, cell - 20, TEXT);
            tip(m.name() + ": " + percent(m.value()) + "\n" + m.detail(), x, y, cell, 19);
        }
        int used = lines.stream().filter(l -> !equipment(l.equipment()).naval()).mapToInt(Line::factories).sum();
        int naval = lines.stream().filter(l -> equipment(l.equipment()).naval()).mapToInt(Line::factories).sum();
        String[] icons = {"military_factory_icon", "dockyard_icon", "dockyard_icon_with_wrench"};
        String[] names = {"군수공장 사용 / 전체", "조선소 사용 / 전체", "해군 수리용 조선소 사용 / 전체"};
        String[] counts = {used + "/" + economy.military(), naval + "/" + economy.dockyards(), repairDockyards()};
        int summaryY = top + 104 - offset;
        for (int i = 0; i < 3; i++) {
            int x = 6 + i * cell;
            rail(x, summaryY, cell - 1, 22);
            art("production/summary/" + icons[i], x + 2, summaryY + 3, 15, 15);
            text(counts[i], x + 19, summaryY + 7, cell - 22, TEXT);
            tip(names[i] + ": " + counts[i] + (i == 2 ? "\n함선 건조에 배정한 조선소를 제외한 수리 배정 현황" : ""), x, summaryY, cell, 22);
        }
        int filterY = top + 129 - offset;
        if (filterY >= bodyTop && filterY + 22 <= bottom()) productionActions(filterY);
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i); var e = equipment(line.equipment()); int y = start + i * h - scroll;
            if (!visible(y, h, compact() ? bodyTop : start)) continue;
            rail(6, y, pane - 12, h - 3);
            equipmentButton(e, e.name() + " · 장비 교체", 10, y + 4, 48, Math.min(30, h - 32), true, () -> {
                picker = "equipment"; group = e.group(); switchLine = line.id(); pickerScroll = 0; rebuildWidgets();
            });
            text(equipmentName(e), 62, y + 6, pane - 72, TEXT);
            text(decimal(line.daily()) + " / 일", 62, y + (h >= 65 ? 20 : 15), pane - 72, GOOD);
            int controlsY = y + (h >= 79 ? 38 : h - 27), barY = y + (h >= 79 ? 62 : h - 7);
            button("공장 줄이기", "−", 10, controlsY, 18, 18, line.factories() > 0,
                    () -> send(ASSIGN, line.id(), "", Math.max(0, line.factories() - factoryStep())));
            recess(30, controlsY, 23, 18); text(Integer.toString(line.factories()), 32, controlsY + 5, 19, TEXT);
            button("공장 늘리기 · 보유량 안에서 배정", "+", 55, controlsY, 18, 18, line.factories() < line.availableFactories(),
                    () -> send(ASSIGN, line.id(), "", Math.min(line.availableFactories(), line.factories() + factoryStep())));
            if (e.outdated()) {
                tip("구형 장비 · " + e.name() + "\n장비 이미지를 눌러 신형으로 교체할 수 있습니다.\n교체 시 생산 효율이 변경되며 기존 재고는 유지됩니다.", 60, y + 3, pane - 65, 12);
            }
            button("생산 우선순위 맨 위로", "⇈", pane - 93, controlsY, 19, 18, i > 0, () -> send(FIRST, line.id(), "", 0));
            button("생산 라인 한 칸 위로", "↑", pane - 72, controlsY, 19, 18, view.lines().indexOf(line) > 0, () -> send(UP, line.id(), "", 0));
            button("생산 라인 한 칸 아래로", "↓", pane - 51, controlsY, 19, 18, view.lines().indexOf(line) < view.lines().size() - 1, () -> send(DOWN, line.id(), "", 0));
            button("생산 라인 삭제", "×", pane - 30, controlsY, 19, 18, true, () -> send(REMOVE, line.id(), "", 0));
            bar(10, barY, pane - 20, 5, line.efficiency(), line.shortage() > 0 ? BAD : GOOD);
            tip("생산 효율 " + percent(line.efficiency()) + "\n자원 부족으로 인한 감소 " + percent(line.shortage())
                    + "\n누적 생산 " + decimal(line.progress()) + " · 단가 " + decimal(e.cost()) + " IC\n"
                    + resources(e.resources(), line.factories()) + "\n배정 가능 " + line.availableFactories(), 9, barY - 3, pane - 18, 9);
        }
        if (graphics != null) graphics.disableScissor();

    }
    int productionRowHeight(int start) {
        return compact() ? 79 : Math.max(52, Math.min(79, (bottom() - start) / 3));
    }
    String repairDockyards() {
        var repairs = view.navalRepairs();
        if (repairs == null) return "—/—";
        return (repairs.usedDockyards() == null ? "—" : repairs.usedDockyards()) + "/" + repairs.availableDockyards();
    }
    List<Modifier> productionIndicators() {
        var result = new ArrayList<Modifier>();
        for (String id : List.of("dockyard", "factory", "cap", "retention", "growth", "damage"))
            view.modifiers().stream().filter(m -> m.id().equals(id)).findFirst().ifPresent(result::add);
        return List.copyOf(result);
    }
    private void productionActions(int y) {
        String[] ids = {"infantry", "armor", "air", "navy", "repair"};
        String[] names = {"보병 및 포병 장비 제작", "기갑 차량 제작", "항공기 제작", "함선 건조", "해군 수리 대기열"};
        int cell = (pane - 12) / ids.length;
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            originalButton(names[i], "production/actions/" + id, 6 + i * cell, y, cell - 1, 22, () -> {
                group = id; picker = id.equals("repair") ? "repairs" : "equipment";
                switchLine = ""; pickerScroll = 0; rebuildWidgets();
            });
        }
    }
    private void resourceStrip(int y) { resourceStrip(y, 36); }
    private void resourceStrip(int y, int h) {
        int cell = (pane - 12) / Math.max(1, view.resources().size());
        for (int i = 0; i < view.resources().size(); i++) {
            var r = view.resources().get(i); int x = 6 + i * cell;
            recess(x, y, cell - 1, h); art(resourceArt(r.id()), x + (cell - 16) / 2, y + 2, 16, 16);
            text(integer(r.available() - r.demand()), x + 2, y + h - 11, cell - 4, r.available() < r.demand() ? BAD : GOOD);
            tip(r.name() + "\n점유지 추출 " + decimal(r.extracted()) + "\n수입 " + decimal(r.imported())
                    + " · 수출 배정 " + decimal(r.exported()) + "\n생산용 " + decimal(r.available()) + " / 수요 " + decimal(r.demand()), x, y, cell, h);
        }
    }
    static final List<String> LOGISTICS_HEADERS = List.of("평균 생산 효율", "장비 유형", "생산", "상태", "수요", "균형", "비축량", "자원");
    private void logistics() {
        int[] fractions = {0, 7, 32, 42, 52, 62, 73, 85, 100};
        int[] cols = new int[9];
        for (int i = 0; i < cols.length; i++) cols[i] = 6 + (pane - 12) * fractions[i] / 100;
        String[] icons = {"efficiency", "", "production", "", "need", "balance", "stockpile", ""};
        String[] descriptions = {"가동 중인 공장 수로 가중한 평균 생산 효율", "장비 유형", "각 장비의 하루 생산량", "일일 필요량 중 생산으로 충당되는 비율", "장비 유형별 일일 수요", "매일 장비 균형", "현재 보관 중인 장비 총량", "배정된 생산 공장이 요구하는 자원"};
        int heading = top + 29, start = heading + 22, h = 39;
        rail(6, heading, pane - 12, 20);
        for (int i = 0; i < LOGISTICS_HEADERS.size(); i++) {
            int w = cols[i + 1] - cols[i];
            if (icons[i].isEmpty()) fitted(LOGISTICS_HEADERS.get(i), cols[i] + 1, heading + 6, w - 2, TEXT);
            else art("logistics/" + icons[i], cols[i] + 1, heading + 3, w - 2, 14);
            tip(LOGISTICS_HEADERS.get(i) + "\n" + descriptions[i], cols[i], heading, w, 20);
        }
        var items = LogisticsRows.create(view.equipment(), view.lines(), "all");
        scroll = clampScroll(scroll, items.size(), h, start);
        for (int i = 0; i < items.size(); i++) {
            var row = items.get(i); var e = row.representative(); int y = start + i * h - scroll;
            if (!visible(y, h, start)) continue;
            rail(6, y, pane - 12, h - 2);
            bar(cols[0] + 2, y + 3, Math.max(2, cols[1] - cols[0] - 4), h - 8, row.efficiency() == null ? 0 : row.efficiency(), GOLD);
            art(e.texture(), cols[1] + 1, y + 2, cols[2] - cols[1] - 2, 23);
            fitted(equipmentName(e), cols[1] + 1, y + 27, cols[2] - cols[1] - 2, GOLD);
            String[] values = {decimal(row.daily()), "—", "—", "—", HoiMenuBar.rawNumber(row.stockpile())};
            for (int j = 0; j < values.length; j++) {
                int col = j + 2; recess(cols[col] + 1, y + 8, cols[col + 1] - cols[col] - 2, 17);
                fitted(values[j], cols[col] + 2, y + 13, cols[col + 1] - cols[col] - 4, j == 0 || j == 4 ? GOOD : MUTED);
            }
            int n = 0, size = Math.min(12, Math.max(7, (cols[8] - cols[7] - 2) / 2));
            for (var resource : row.resources().entrySet()) {
                art(resourceArt(resource.getKey()), cols[7] + 1 + n % 2 * size, y + 3 + n / 2 * size, size, size); n++;
            }
            tip("평균 생산 효율: " + (row.efficiency() == null ? "—" : String.format(Locale.ROOT, "%.1f%%", row.efficiency() * 100)), cols[0], y, cols[1] - cols[0], h);
            tip("일일 수요·충당률·균형 정보가 제공되지 않았습니다.", cols[3], y, cols[6] - cols[3], h);
            tip(row.models().stream().map(model -> model.name() + "\n보관 재고: " + model.stockpile()
                    + "\n훈련에 지급: " + model.reserved() + "\n배치 사단 휴대: " + model.deployed()
                    + "\n충원에 추가로 필요한 수량: " + model.deficit()).collect(java.util.stream.Collectors.joining("\n\n")), cols[1], y, cols[2] - cols[1], h);
            tip("비축량: " + row.stockpile(), cols[6], y, cols[7] - cols[6], h);
            tip(row.resources().entrySet().stream().map(r -> resourceName(r.getKey()) + ": " + decimal(r.getValue())).collect(java.util.stream.Collectors.joining("\n")), cols[7], y, cols[8] - cols[7], h);
        }
        if (items.isEmpty()) text("연구 완료 또는 비축된 장비가 없습니다.", 10, start + 6, pane - 20, MUTED);
    }
    private void fitted(String value, int x, int y, int max, int color) {
        if (graphics == null) return;
        float scale = Math.min(1f, Math.max(1, max) / (float)Math.max(1, font.width(value)));
        graphics.pose().pushMatrix(); graphics.pose().translate(x, y); graphics.pose().scale(scale);
        graphics.text(font, value, 0, 0, color); graphics.pose().popMatrix();
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
                    {"연료 비축", decimal(e.fuel())}, {"인력", manpower(e.manpower())}};
            int start = top + 55; scroll = clampScroll(scroll, rows.length, 35, start);
            for (int i = 0; i < rows.length; i++) {
                int y = start + i * 35 - scroll; if (!visible(y, 35, start)) continue;
                recess(6, y, pane - 12, 32); text(rows[i][0], 11, y + 4, pane - 22, MUTED);
                text(rows[i][1], 11, y + 18, pane - 22, TEXT);
            }
            return;
        }
        rail(6, top + 54, pane - 12, 21);
        art("construction/civilian_factory", 9, top + 57, 15, 15);
        text("민간공장", 28, top + 61, 45, TEXT); text("" + e.civilian(), 74, top + 61, pane/2-78, GOLD);
        text("이용 가능:", pane/2+4, top + 61, pane/2-34, TEXT); text("" + e.freeCivilian(), pane - 28, top + 61, 21, GOLD);
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
        int start = heading + 34, h = 29;
        int[] cols = {8, pane * 44 / 100, pane * 65 / 100, pane * 83 / 100};
        String[] headers = {"국가", "수출", "운송됨", "수송선"};
        for (int i = 0; i < 4; i++) text(headers[i], cols[i], heading + 18, (i == 3 ? pane - 7 : cols[i + 1]) - cols[i] - 3, TEXT);
        scroll = clampScroll(scroll, view.partners().size(), h, start);
        for (int i = 0; i < view.partners().size(); i++) {
            var p = view.partners().get(i); int y = start + i * h - scroll; if (!visible(y, h, start)) continue;
            var active = contracts.stream().filter(t -> t.partner().equals(p.id())).toList();
            int color = !p.route() ? BAD : active.isEmpty() ? GOLD : GOOD;
            double delivered = active.stream().mapToDouble(IndustryView.Trade::delivered).sum();
            recess(6, y, pane - 12, h - 3);
            if (graphics != null) graphics.outline(6, y, pane - 12, h - 3, color);
            art(HoiMenuBar.flagTexture(p.id()), 9, y + 4, 20, 15);
            text(p.name(), 33, y + 9, cols[1] - 38, TEXT);
            text(decimal(p.exports().getOrDefault(resource, 0.0)), cols[1], y + 9, cols[2] - cols[1] - 3, color);
            text(decimal(delivered), cols[2], y + 9, cols[3] - cols[2] - 3, color);
            text(p.convoys() == null || p.convoys() < 0 ? "—" : Integer.toString(p.convoys()), cols[3], y + 9, pane - cols[3] - 8, color);
            if (graphics == null) {
                var row = new PanelButton(p.name() + "에서 수입", 6, y, pane - 12, h - 3, () -> { partner = p.id(); tradeAmount = 1; rebuildWidgets(); }) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        if (isHoveredOrFocused()) g.outline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
                    }
                };
                row.active = !background && pending == 0; addRenderableWidget(row);
            }
            tip(p.name() + (p.route() ? active.isEmpty() ? " · 거래 없음" : " · 거래 중" : " · 경로 없음"), 6, y, pane - 12, h - 3);
        }
    }

    private void recruitment() {
        int secondX = pane + 3, secondW = rightWidth(), start = top + 28;
        if (graphics != null) HoiMenuStyle.panel(graphics, secondX, top, secondW, height - top);
        text("사단 편제", secondX + (secondW - font.width("사단 편제")) / 2, top + 8, secondW - 16, TEXT);
        var groups = new LinkedHashMap<String, List<Recruit>>();
        for (var r : view.recruits()) groups.computeIfAbsent(r.template() + "|" + r.location(), key -> new ArrayList<>()).add(r);
        double scale = (pane - 14) / 493.0;
        int rowHeight = Math.max(23, (int)Math.round(47 * scale)), pitch = rowHeight + 2, prioritiesHeight = 6 * pitch + 4;
        double gs = (pane - 14) / 518.0;
        int headerHeight = (int)Math.round(84 * gs), lineHeight = (int)Math.round(40 * gs);
        int groupHeight = headerHeight + lineHeight + 3, linePitch = lineHeight + 1;
        int groupX = 7 + (int)Math.round(14 * gs);
        int content = prioritiesHeight + groups.entrySet().stream().mapToInt(e -> groupHeight + (collapsedRecruitment.contains(e.getKey()) ? 0 : e.getValue().size() * linePitch)).sum();
        scroll = Math.clamp(scroll, 0, Math.max(0, content - (bottom() - start)));
        String[] names = {"증원", "업그레이드", "보급 트럭", "작전", "주둔군", "습격"};
        String[] icons = {"reinforcement", "upgrade", "supply", "operation", "garrison", "raid"};
        String[] entries = {"reinforcements", "upgrades", "reinforcements", "operations", "garrisons", "upgrades"};
        int backgroundHeight = (int)Math.round(57 * scale);
        double backgroundScale = Math.min(scale, backgroundHeight / 57.0);
        int backgroundLeft = 7 + (pane - 14 - (int)Math.round(493 * backgroundScale)) / 2;
        for (int i = 0; i < names.length; i++) {
            int y = start + i * pitch - scroll;
            if (!visible(y, rowHeight, start)) continue;
            art("recruitment/" + entries[i], 7, y - (int)(5 * scale), pane - 14, backgroundHeight);
            int iconCenterX = backgroundLeft + (int)Math.round(48 * backgroundScale);
            int iconCenterY = y - (int)(5 * scale) + (int)Math.round(29.5 * backgroundScale) - 2;
            int iconBgWidth = (int)(77 * scale), iconBgHeight = (int)(40 * scale), iconWidth = (int)(54 * scale);
            int iconBgX = iconCenterX - iconBgWidth / 2, iconBgY = iconCenterY - iconBgHeight / 2;
            if (graphics != null) UiAssets.nineSlice(graphics, "recruitment/icon_bg", iconBgX, iconBgY, iconBgWidth, iconBgHeight, 16, scale);
            art("recruitment/" + icons[i] + "_icon", iconCenterX - iconWidth / 2, iconBgY, iconWidth, iconBgHeight);
            int labelX = 7 + (int)(110 * scale), labelWidth = (int)(159 * scale);
            art("recruitment/title", labelX, y + 1 + (int)(12 * scale), labelWidth, (int)(26 * scale));
            centeredCompactText(names[i], labelX + 2, y + (int)(25 * scale) - 3, labelWidth - 4, TEXT);
            art("recruitment/meter", 7 + (int)(280 * scale), y + (int)(10 * scale), (int)(108 * scale), (int)(33 * scale));
            art("recruitment/equipment", 7 + (int)(285 * scale), y + (int)(15 * scale), (int)(23 * scale), (int)(23 * scale));
            var policy = view.recruitmentPolicy();
            String category = icons[i];
            for (int dot = 0; dot < 3; dot++) {
                int priority = dot;
                boolean selected = policy != null && Objects.equals(policy.priorities().get(category), dot);
                textureButton(names[i] + " · " + priorityName(dot) + " 우선순위", "", priorityTexture(dot, selected), 7 + (int)((415 + dot * 20) * scale), y + (int)(15 * scale), (int)(20 * scale), (int)(21 * scale), policy != null,
                        () -> send(ALLOCATION_PRIORITY, category, "", priority));
            }
            if (i == 0 && policy != null && policy.reinforcementRatio() != null) {
                bar(7 + (int)(310 * scale), y + (int)(19 * scale), (int)(72 * scale), Math.max(2, (int)(12 * scale)), policy.reinforcementRatio(), 0xFF7EAF69);
                String needs = policy.reinforcementNeeds().entrySet().stream().map(e -> equipment(e.getKey()).name() + " · 부족 " + e.getValue()).collect(java.util.stream.Collectors.joining("\n"));
                tip("증원 · 장비 충족률 " + percent(policy.reinforcementRatio()) + (needs.isEmpty() ? "\n부족한 장비 없음" : "\n" + needs), 7, y, (int)(400 * scale), rowHeight);
            } else tip(names[i] + " · 배정 현황이 제공되지 않았습니다.", 7, y, (int)(400 * scale), rowHeight);
        }
        int cursor = start + prioritiesHeight - scroll;
        for (var entry : groups.entrySet()) {
            var rows = entry.getValue(); var first = rows.getFirst(); var t = template(first.template());
            int y = cursor; cursor += groupHeight;
            if (visible(y, groupHeight, start)) {
                art("recruitment/conveyor", groupX, y, (int)Math.round(490 * gs), headerHeight);
                if (!t.line().isEmpty()) art(unitTexture(t.line().getFirst(), false), groupX + 2 + (int)(10 * gs), y + (int)(17 * gs), (int)(70 * gs), (int)(50 * gs));
                centeredCompactText(t.name(), groupX + (int)(95 * gs), y + (int)(29 * gs) - 3, (int)(214 * gs), TEXT);
                int amountSize = Math.max(8, (int)Math.round(26 * gs)), amountY = y + 2 + (int)Math.round(10 * gs);
                int minusX = groupX + 2 + (int)Math.round(312 * gs), plusX = groupX + 2 + (int)Math.round(383 * gs);
                textureButton(t.name() + " 연속 훈련 횟수 증가", "", "recruitment/increase", plusX, amountY, amountSize, amountSize, first.seriesLimit() < 999, () -> send(SERIES, first.id(), "", first.seriesLimit() + 1));
                textureButton(t.name() + " 연속 훈련 횟수 감소 · 0은 무한대", "", "recruitment/decrease", minusX, amountY, amountSize, amountSize, first.seriesLimit() > 0, () -> send(SERIES, first.id(), "", first.seriesLimit() - 1));
                if (graphics != null) {
                    String amount = first.seriesLimit() == 0 ? "∞" : first.seriesLimit().toString();
                    graphics.pose().pushMatrix();
                    graphics.pose().translate((minusX + amountSize + plusX - font.width(amount) * .75f) / 2, amountY + (amountSize - font.lineHeight * .75f) / 2);
                    graphics.pose().scale(.75f);
                    graphics.text(font, amount, 0, 0, first.seriesLimit() == 0 ? GOLD : TEXT);
                    graphics.pose().popMatrix();
                }
                tip("연속 훈련 횟수 · 0은 무한대", minusX + amountSize, amountY, plusX - minusX - amountSize, amountSize);
                for (int dot = 0; dot < 3; dot++) {
                    int priority = dot;
                    textureButton(t.name() + " · " + priorityName(dot) + " 우선순위", "", priorityTexture(dot, first.priority() == dot), groupX + (int)((420 + 20 * dot) * gs), y + (int)(17 * gs), (int)(20 * gs), (int)(21 * gs), true, () -> send(GROUP_PRIORITY, first.id(), "", priority));
                }
                art("recruitment/location", groupX + (int)(91 * gs), y + (int)(46 * gs), (int)(223 * gs), (int)(31 * gs));
                art("recruitment/location_frame", groupX + (int)(94 * gs), y + (int)(48 * gs), (int)(27 * gs), (int)(27 * gs));
                centeredCompactText(locationName(first.location()), groupX + (int)(119 * gs), y + (int)(61 * gs) - 3, (int)(188 * gs), first.location().isEmpty() ? BAD : TEXT);
                if (graphics == null) deploymentLocations.add(new DeploymentLocation(groupX + (int)(91 * gs), y + (int)(46 * gs), (int)(223 * gs), (int)(31 * gs), first.id(), locationName(first.location())));
                tip("좌클릭하여 배치 프로빈스 선택 · 우클릭하여 장소 초기화", groupX + (int)(91 * gs), y + (int)(46 * gs), (int)(223 * gs), (int)(31 * gs));
                textureButton(t.name() + " 부대 추가", "부대 추가", "recruitment/add", groupX + (int)(315 * gs), y + (int)(50 * gs), (int)(105 * gs), (int)(23 * gs), true, () -> send(RECRUIT, t.id(), first.location(), 0));
                textureButton(t.name() + " 모든 라인 즉시 배치", "", "recruitment/deploy", groupX + (int)(421 * gs), y + (int)(47 * gs), (int)(27 * gs), (int)(27 * gs), !first.location().isEmpty() && rows.stream().anyMatch(r -> r.progress() >= .2), () -> send(DEPLOY_GROUP, first.id(), "", 0));
                textureButton(t.name() + " 모든 라인 생산 취소", "", "recruitment/cancel", groupX + (int)(450 * gs), y + (int)(47 * gs), (int)(27 * gs), (int)(27 * gs), true, () -> send(CANCEL_GROUP, first.id(), "", 0));
                int summaryY = y + headerHeight;
                art("recruitment/summary", 7, summaryY, pane - 14, lineHeight);
                art("recruitment/equipment_state", 8 + (int)(212 * gs), summaryY + 1 + (int)(3 * gs), (int)(27 * gs), (int)(27 * gs));
                bar(7 + (int)(238 * gs), summaryY + (int)(10 * gs), (int)(65 * gs), Math.max(3, (int)(11 * gs)), rows.stream().mapToDouble(r -> fillRatio(t.equipment(), r.equipment())).average().orElse(0), 0xFF7EAF69);
                centeredCompactText(rows.size() + "개 사단", 7 + (int)(315 * gs), summaryY + (int)(18 * gs) - 3, (int)(150 * gs), TEXT);
                textureButton(collapsedRecruitment.contains(entry.getKey()) ? "클릭하여 펼치기" : "클릭하여 접기", "", collapsedRecruitment.contains(entry.getKey()) ? "recruitment/expand" : "recruitment/collapse", 7 + (int)(481 * gs), summaryY + (int)(4 * gs), (int)(26 * gs), (int)(26 * gs), true,
                        () -> { if (!collapsedRecruitment.add(entry.getKey())) collapsedRecruitment.remove(entry.getKey()); rebuildWidgets(); });
            }
            if (collapsedRecruitment.contains(entry.getKey())) continue;
            int number = 0;
            for (var r : rows) {
                y = cursor; cursor += linePitch; number++;
                if (!visible(y, lineHeight, start)) continue;
                int lineX = 7 + (int)Math.round(2 * gs);
                art("recruitment/line", lineX, y, (int)Math.round(514 * gs), lineHeight);
                compactText(t.name() + " " + number, lineX + (int)(9 * gs), y + (int)(20 * gs) - 3, (int)(190 * gs), TEXT);
                double manpowerRatio = t.manpower() == 0 ? 1 : (double) r.manpower() / t.manpower();
                double equipped = fillRatio(t.equipment(), r.equipment());
                art("recruitment/equipment_state", lineX + 1 + (int)(211 * gs), y + 1 + (int)(7 * gs), (int)(27 * gs), (int)(27 * gs));
                art("recruitment/training", lineX + 1 + (int)(307 * gs), y + (int)(7 * gs), (int)(27 * gs), (int)(27 * gs));
                bar(lineX + (int)(237 * gs), y + (int)(14 * gs), (int)(64 * gs), Math.max(3, (int)(11 * gs)), equipped, 0xFF7EAF69);
                bar(lineX + (int)(333 * gs), y + (int)(14 * gs), (int)(64 * gs), Math.max(3, (int)(11 * gs)), r.progress(), 0xFF7EAF69);
                tip("훈련 " + percent(r.progress()) + " · 인력 " + percent(manpowerRatio) + " · 장비 " + percent(equipped) + "\n" + equipmentList(t.equipment(), r.equipment()), 8, y, (int)(397 * gs), lineHeight);
                centeredCompactText(r.seriesLabel(), lineX + (int)(398 * gs), y + (int)(20 * gs) - 3, (int)(55 * gs), r.seriesLimit() == 0 ? GOLD : TEXT);
                tip("라인의 현재 연속 훈련 수 · " + r.seriesLabel(), lineX + (int)(398 * gs), y + (int)(7 * gs), (int)(57 * gs), (int)(26 * gs));
                textureButton("즉시 배치 · 훈련 20% 이상", "", "recruitment/deploy_line", lineX + (int)(458 * gs), y + (int)(7 * gs), (int)(26 * gs), (int)(26 * gs), !r.location().isEmpty() && r.progress() >= .2, () -> send(DEPLOY, r.id(), "", 0));
                textureButton("훈련 취소 · 지급 장비와 인력 반환", "", "recruitment/cancel_line", lineX + (int)(484 * gs), y + (int)(7 * gs), (int)(26 * gs), (int)(26 * gs), true, () -> send(CANCEL_RECRUIT, r.id(), "", 0));
            }
        }
        var special = view.specialForces();
        art("recruitment/special", secondX + 11, top + 30, 20, 20);
        text(special == null ? "—/—" : special.used() + "/" + special.capacity(), secondX + 35, top + 35, secondW - 128, TEXT);
        tip("특수부대 대대 · 사용량 / 한도" + (special == null ? " · 정보 없음" : "\n" + special.used() + " / " + special.capacity() + " · 훈련 대기 포함"), secondX + 7, top + 28, secondW - 100, 23);
        button("새 사단 편제 설계", "사단 설계", secondX + secondW - 90, top + 29, 83, 22, !view.templates().isEmpty(),
                () -> loadDesigner(view.templates().getFirst().id()));
        var retired = view.recruitmentPolicy() == null ? Set.<String>of() : view.recruitmentPolicy().retiredTemplates();
        var templates = view.templates().stream().filter(t -> !retired.contains(t.id())).toList();
        int ts = top + 55, th = Math.max(36, (secondW - 10) * 78 / 344); secondScroll = clampScroll(secondScroll, templates.size(), th + 3, ts);
        for (int i = 0; i < templates.size(); i++) {
            var t = templates.get(i); int y = ts + i * (th + 3) - secondScroll;
            if (!visible(y, th, ts)) continue;
            art("recruitment/queue", secondX + 5, y, secondW - 10, th);
            double cardScale = (secondW - 10) / 346.0;
            int iconWidth = (int)(72 * cardScale), nameX = secondX + 5 + (int)(97 * cardScale), nameWidth = (int)(234 * cardScale);
            if (!t.line().isEmpty()) art(unitTexture(t.line().getFirst(), false), secondX + 7 + (int)(9 * cardScale), y + (int)(12 * cardScale), iconWidth, (int)(52 * cardScale));
            centeredCompactText(t.name(), nameX, y + (int)(32 * cardScale) - 3, nameWidth, TEXT);
            int buttonWidth = (int)(71 * cardScale), buttonY = y + (int)(49 * cardScale), buttonHeight = Math.max(10, (int)(26 * cardScale));
            textureButton(t.name() + " 훈련", "훈련", "recruitment/button", secondX + 5 + (int)(158 * cardScale), buttonY, buttonWidth, buttonHeight, !retired.contains(t.id()), () -> send(RECRUIT, t.id(), "", 0));
            textureButton(t.name() + " 편제", "편제", "recruitment/button", secondX + 5 + (int)(229 * cardScale), buttonY, buttonWidth, buttonHeight, true, () -> loadDesigner(t.id()));
            textureButton(t.name() + " 편제를 목록에서 삭제", "", "recruitment/delete_template", secondX + 5 + (int)(300 * cardScale), y + (int)(48 * cardScale), (int)(26 * cardScale), (int)(26 * cardScale), true, () -> send(RETIRE_TEMPLATE, t.id(), "", 1));
            tip(t.name() + " · " + manpower(t.manpower()) + "명 · " + integer(t.days()) + "일\n" + equipmentList(t.equipment(), Map.of()), nameX, y, nameWidth, th - 14);
        }
    }
    private static String priorityName(int priority) {return switch(priority) {case 0 -> "낮음"; case 1 -> "보통"; default -> "높음";};}
    private static String priorityTexture(int priority, boolean selected) {return "recruitment/priority" + (selected ? switch(priority) {case 0 -> "_low"; case 1 -> "_normal"; default -> "_high";} : "");}
    private void textureButton(String name, String label, String background, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var control = new dev.hoi.client.ui.HoiMenuButton(name, x, y, w, h, action).caption(label).background(background);
        if (name.startsWith("클릭하여")) control.setTooltip(Tooltip.create(Component.literal("클릭").withStyle(net.minecraft.ChatFormatting.GREEN).append(Component.literal(name.substring(2)).withStyle(net.minecraft.ChatFormatting.WHITE))));
        control.textScale(.75f); control.active = enabled && pending == 0; addRenderableWidget(control);
    }
    private void centeredCompactText(String value, int x, int y, int width, int color) {
        String clipped = font.plainSubstrByWidth(value, Math.max(1, (int)(width / .75)));
        compactText(clipped, x + Math.max(0, (width - (int)Math.ceil(font.width(clipped) * .75)) / 2), y, width, color);
    }
    private void compactText(String value, int x, int y, int width, int color) {
        if (graphics == null) return;
        graphics.pose().pushMatrix(); graphics.pose().translate(x, y); graphics.pose().scale(.75f);
        graphics.text(font, font.plainSubstrByWidth(value, Math.max(1, (int)(width / .75))), 0, 0, color); graphics.pose().popMatrix();
    }
    private void smallText(String value, int x, int y, int color) {
        if (graphics == null) return;
        int edge = x < pane ? pane - 10 : pane + 3 + rightWidth() - 10;
        String clipped = font.plainSubstrByWidth(value, Math.max(1, (int)((edge - x) / .75)));
        graphics.pose().pushMatrix(); graphics.pose().translate(x, y); graphics.pose().scale(.75f);
        graphics.text(font, clipped, 0, 0, color); graphics.pose().popMatrix();
    }
    private void loadDesigner(String id) {
        selectedSlot = -1; designerScroll = 0; picker = ""; recruitTemplate = id;
        send(LOAD_TEMPLATE, id, "", 0);

    }
    private int modalWidth() { return Math.min(820, width - 16); }
    private int modalX() { return (width - modalWidth()) / 2; }
    private int modalY() { return Math.max(top + 18, (height - 350) / 2); }
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
        text("사단 구성", x + 10, y + 54, left - 12, TEXT);
        String[] summary = {"HARDNESS", "ORGANIZATION", "HP", "SPEED"};
        for (int i = 0; i < summary.length; i++) {
            String id = summary[i]; int bx = x + 10 + i * (left - 12) / 4;
            var stat = t.stats().stream().filter(v -> id.equals(v.id())).findFirst().orElse(null);
            recess(bx, y + 67, (left - 16) / 4, 17);
            art("stats/" + id.toLowerCase(Locale.ROOT), bx + 2, y + 69, 12, 12);
            text(stat == null ? "—" : statValue(stat), bx + 16, y + 72, (left - 16) / 4 - 18, GOLD);
            tip(stat == null ? id : stat.name() + ": " + statValue(stat), bx, y + 67, (left - 16) / 4, 17);
        }
        if (selectedSlot < 0) {
            int regimentHeight = t.columns() == null ? 0 : 42;
            int requiredHeight = 184 + regimentHeight;
            designerScroll = Math.clamp(designerScroll, 0, Math.max(0, requiredHeight - (h - 106)));
            int cell = Math.min(40, (left - 45) / 5), sy = y + 94 - designerScroll;
            for (int i = 0; i < 25; i++) {
                int slot = i, bx = x + 10 + i / 5 * cell, by = sy + i % 5 * 25;
                if (by < y + 90 || by + 22 > y + h - 33) continue;
                String unit = t.lineUnit(i);
                if (t.columns() == null ? i > t.line().size() : i % 5 > t.columnSize(i / 5)) {
                    divisionSlot("이 열의 앞선 빈 대대 칸부터 추가", "production/designer/locked", bx, by, cell - 3, false, () -> {}); continue;
                }
                if (unit.isEmpty()) divisionSlot("대대 추가", "production/designer/add", bx, by, cell - 3, true, () -> selectSlot(slot, false));
                else divisionSlot(unitName(unit, false), unitTexture(unit, false), bx, by, cell - 3, true, () -> selectSlot(slot, false));
            }
            if (designerScroll == 0) text("지원", x + 14 + 5 * cell, sy - 9, 32, MUTED);
            for (int i = 0; i < 5; i++) {
                int slot = i, bx = x + 12 + 5 * cell, by = sy + i * 25;
                if (by < y + 90 || by + 22 > y + h - 33) continue;
                if (i > t.support().size()) divisionSlot("앞선 빈 지원중대 칸부터 추가", "production/designer/locked", bx, by, 30, false, () -> {});
                else if (i == t.support().size()) divisionSlot("지원중대 추가", "production/designer/add", bx, by, 30, true, () -> selectSlot(slot, true));
                else divisionSlot(unitName(t.support().get(i), true), unitTexture(t.support().get(i), true), bx, by, 30, true, () -> selectSlot(slot, true));
            }
            if (t.columns() != null) {
                if (sy + 131 >= y + 90 && sy + 142 < y + h - 33) text("연대 지원", x + 10, sy + 131, left - 12, TEXT);
                for (int column = 0; column < 5; column++) {
                    int slot = column, bx = x + 10 + column * cell, by = sy + 145;
                    if (by < y + 90 || by + 22 > y + h - 33) continue;
                    boolean unlocked = t.columnSize(column) >= 3;
                    divisionSlot(unlocked ? "연대 지원 · 장식용" : "연대 지원 · 같은 열에 대대 3개 필요",
                            unlocked ? "production/designer/add" : "production/designer/locked", bx, by, cell - 3, false, () -> {});
                }
            }
        } else {
            var choices = supportSlot ? view.companies().stream().filter(c -> !t.support().contains(c.id())).toList() : view.battalions();
            button("대대 목록 닫기", "뒤로", x + left - 45, y + 53, 43, 18, true, () -> { selectedSlot = -1; rebuildWidgets(); });
            int start = y + 77, available = Math.max(1, (h - 112) / 29) * 2;
            pickerScroll = Math.clamp(pickerScroll, 0, Math.max(0, (choices.size() + 2 - available + 1) / 2));
            for (int i = 0; i < choices.size() + 1; i++) {
                int row = i / 2 - pickerScroll, bx = x + 9 + i % 2 * (left / 2), by = start + row * 29;
                if (row < 0 || by + 27 > y + h - 32) continue;
                String id = i == 0 ? "" : choices.get(i - 1).id(); String label = i == 0 ? "제거" : choices.get(i - 1).name();
                boolean occupied = supportSlot ? selectedSlot < t.support().size() : !t.lineUnit(selectedSlot).isEmpty();
                boolean enabled = i != 0 || occupied && (supportSlot || t.line().size() > 1);
                Runnable choose = () -> {
                    send(supportSlot ? SUPPORT_SLOT : t.columns() == null ? LINE_SLOT : LINE_COLUMN,
                            id, "", selectedSlot); selectedSlot = -1;
                };
                if (i == 0) button(label, label, bx, by, left / 2 - 5, 25, enabled, choose);
                else unitChoice(label, choices.get(i - 1).texture(), bx, by, left / 2 - 5, enabled, choose);
            }
        }
        int statsX = x + left + 12, statsWidth = w - left - 21, colWidth = statsWidth / 3;
        String[] groups = {"BASE", "COMBAT", "EQUIPMENT_COST"}, titles = {"기본 능력치", "전투 능력치", "장비 비용"};
        for (int col = 0; col < 3; col++) {
            int sx = statsX + col * colWidth;
            recess(sx, y + 28, colWidth - 1, h - 81);
            rail(sx, y + 28, colWidth - 1, 18); text(titles[col], sx + 3, y + 33, colWidth - 6, TEXT);
            String groupId = groups[col]; int row = 0;
            for (var stat : t.stats().stream().filter(v -> v.group().equals(groupId)).toList()) {
                int sy = y + 51 + row++ * 15 - designerScroll;
                if (sy < y + 48 || sy + 12 > y + h - 58) continue;
                art("stats/" + (stat.id() == null ? "" : stat.id().toLowerCase(Locale.ROOT)), sx + 2, sy - 2, 11, 11);
                int valueWidth = Math.min(colWidth / 2, Math.max(35, font.width(statValue(stat))));
                text(stat.name(), sx + 15, sy, colWidth - valueWidth - 19, MUTED);
                text(statValue(stat), sx + colWidth - valueWidth - 3, sy, valueWidth, TEXT);
                tip(stat.name() + ": " + statValue(stat), sx, sy - 2, colWidth - 1, 15);
            }
            if (col == 2) for (var need : t.equipment().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                int sy = y + 51 + row++ * 18 - designerScroll;
                if (sy < y + 48 || sy + 15 > y + h - 58) continue;
                var e = equipment(need.getKey());
                text(e.name(), sx + 3, sy, colWidth - 41, MUTED); text(integer(need.getValue()), sx + colWidth - 37, sy, 34, TEXT);
                tip(e.name() + ": " + need.getValue(), sx, sy - 2, colWidth - 1, 18);
            }
        }
        double cost = 0; boolean known = true;
        for (var need : t.equipment().entrySet()) {
            var e = view.equipment().stream().filter(v -> v.id().equals(need.getKey())).findFirst().orElse(null);
            if (e == null) known = false; else cost += e.cost() * need.getValue();
        }
        art("stats/production_cost", statsX, y + h - 49, 14, 14);
        text("예상 생산 비용: " + (known ? decimal(cost) + " IC" : "—"), statsX + 18, y + h - 45, statsWidth - 20, GOLD);
        button("새 편제로 저장 · 기존 사단과 훈련 편제 유지", "편제 저장", x + w / 2 - 65, y + h - 27, 130, 21, !draftName.isBlank(),
                () -> send(SAVE_TEMPLATE, draftName, "", 0));
    }
    private static String statValue(IndustryView.Stat stat) {
        String value = stat.unit().equals("PEOPLE") ? manpower(stat.value()) : stat.unit().equals("PERCENT") ? percent(stat.value()) : stat.unit().equals("DAYS") ? integer(stat.value()) : decimal(stat.value());
        return value + (stat.unit().equals("KM_PER_HOUR") ? "km/h" : stat.unit().equals("DAYS") ? "일" : "");
    }
    private void selectSlot(int slot, boolean support) { selectedSlot = slot; supportSlot = support; pickerScroll = 0; rebuildWidgets(); }
    private void layoutPicker() {
        int w = Math.min(270, width - 16), x = width - pane >= w + 8 ? pane + 4 : (width - w) / 2, y = top + 3;
        panel(x, y, w, height - y - 27);
        text(picker.equals("locations") ? "배치 장소 지정" : picker.equals("repairs") ? "해군 수리 대기열" : "생산 장비 선택", x + 9, y + 8, w - 40, TEXT);
        button("목록 닫기", "×", x + w - 25, y + 3, 19, 19, true, () -> { picker = ""; rebuildWidgets(); });
        int start = y + 29;
        if (picker.equals("repairs")) {
            var repairs = view.navalRepairs();
            if (repairs == null) {
                text("수리 현황을 확인할 수 없습니다.", x + 9, start + 8, w - 18, MUTED);
                return;
            }
            art("production/summary/dockyard_icon_with_wrench", x + 9, start, 20, 20);
            text("수리용 조선소 " + repairDockyards(), x + 34, start + 6, w - 43, TEXT);
            start += 28;
            pickerScroll = clampScroll(pickerScroll, repairs.ships().size(), 58, start);
            for (int i = 0; i < repairs.ships().size(); i++) {
                var ship = repairs.ships().get(i); int by = start + i * 58 - pickerScroll;
                if (!visible(by, 58, start)) continue;
                recess(x + 6, by, w - 12, 55);
                art(ship.texture(), x + 10, by + 4, 45, 24);
                text(ship.name(), x + 60, by + 5, w - 70, TEXT);
                text(ship.status(), x + 60, by + 19, w - 70, MUTED);
                text(ship.port(), x + 10, by + 33, w - 20, MUTED);
                bar(x + 10, by + 47, w - 20, 4, ship.hp() / ship.maxHp(), GOOD);
                tip(ship.name() + "\n내구도 " + decimal(ship.hp()) + " / " + decimal(ship.maxHp()), x + 6, by, w - 12, 55);
            }
            if (repairs.ships().isEmpty()) text("수리가 필요한 함선이 없습니다.", x + 9, start + 10, w - 18, MUTED);
        } else if (picker.equals("equipment")) {
            button("구형 장비 표시", (showOutdated ? "[✓] " : "[ ] ") + "구형 장비 표시", x + 8, start, w - 16, 22, true,
                    () -> { showOutdated = !showOutdated; pickerScroll = 0; rebuildWidgets(); });
            start += 27;
            var current = view.lines().stream().filter(l -> l.id().equals(switchLine)).map(l -> equipment(l.equipment())).findFirst().orElse(null);
            var items = view.equipment().stream().filter(e -> e.unlocked() && (group.equals("all") || e.group().equals(group)))
                    .filter(e -> showOutdated || !e.outdated())
                    .filter(e -> switchLine.isEmpty() || current != null && current.naval() == e.naval()
                            && (current.family() == null || e.family() == null || current.family().equals(e.family()))).toList();
            pickerScroll = clampScroll(pickerScroll, items.size(), 55, start);
            for (int i = 0; i < items.size(); i++) {
                var e = items.get(i); int by = start + i * 55 - pickerScroll; if (!visible(by, 55, start)) continue;
                boolean selected = current != null && current.id().equals(e.id());
                equipmentButton(e, e.name(), x + 7, by, 55, 45, !selected, () -> chooseEquipment(e.id()));
                text(equipmentName(e), x + 67, by + 4, w - 76, TEXT);
                text(decimal(e.cost()) + " IC · 재고 " + e.stockpile(), x + 67, by + 18, w - 76, MUTED);
                button("생산: " + e.name(), selected ? "생산 중" : switchLine.isEmpty() ? "생산" : "교체", x + 67, by + 30, w - 76, 19, !selected, () -> chooseEquipment(e.id()));
                tip(resources(e.resources(), 1), x + 6, by, w - 12, 28);
            }
            if (items.isEmpty()) text("생산 가능한 장비가 없습니다.", x + 9, start + 9, w - 18, MUTED);
        } else {
            pickerScroll = clampScroll(pickerScroll, view.locations().size(), 25, start);
            for (int i = 0; i < view.locations().size(); i++) {
                var l = view.locations().get(i); int by = start + i * 25 - pickerScroll;
                if (!visible(by, 25, start)) continue;
                button(l.name(), l.name(), x + 7, by, w - 14, 22, true, () -> { location = l.id(); picker = ""; send(LOCATION, locationRecruit, l.id(), 0); rebuildWidgets(); });
            }
        }
    }
    private void chooseEquipment(String id) {
        if (switchLine.isEmpty()) send(ADD, id, "", 0); else send(SWITCH, switchLine, id, 0);
        picker = "";
    }
    private void layoutContract() {
        var p = view.partners().stream().filter(v -> v.id().equals(partner)).findFirst().orElse(null);
        if (p == null) { partner = ""; return; }
        var contracts = view.trades().stream().filter(v -> v.importing() && v.partner().equals(partner) && v.resource().equals(resource)).toList();
        int w = Math.min(340, width - 16), h = Math.min(225, height - top - 12), x = (width - w) / 2, y = Math.max(top + 4, (height - h) / 2);
        panel(x, y, w, h); text(p.name(), x + 10, y + 9, w - 67, TEXT);
        art(HoiMenuBar.flagTexture(p.id()), x + w - 60, y + 5, 25, 16);
        button("계약 창 닫기", "×", x + w - 25, y + 3, 19, 19, true, () -> { partner = ""; rebuildWidgets(); });
        art(resourceArt(resource), x + 10, y + 35, 20, 20);
        text(resourceName(resource) + " 수출: " + decimal(p.exports().getOrDefault(resource, 0.0)), x + 38, y + 39, w - 48, GOLD);
        text(p.route() ? "무역 경로 이용 가능" : "무역 경로 없음", x + 10, y + 65, w - 20, p.route() ? GOOD : BAD);
        text("수송선: " + (p.convoys() == null || p.convoys() < 0 ? "—" : p.convoys() == 0 ? "필요 없음" : p.convoys()), x + 10, y + 83, w - 20, TEXT);
        art("construction/civilian_factory", x + 10, y + 101, 15, 15);
        text("이용 가능: " + view.economy().freeCivilian(), x + 31, y + 105, w - 41, GOLD);
        button("무역 공장 줄이기", "−", x + 10, y + 126, 24, 23, tradeAmount > 1, () -> { tradeAmount--; rebuildWidgets(); });
        text("배정할 민간공장: " + tradeAmount, x + 43, y + 133, w - 86, GOLD);
        button("무역 공장 늘리기", "+", x + w - 34, y + 126, 24, 23, tradeAmount < view.economy().freeCivilian(), () -> { tradeAmount++; rebuildWidgets(); });
        if (!contracts.isEmpty()) button("수입 계약 취소", "기존 계약 취소", x + 10, y + h - 58, w - 20, 21, true, () -> send(CANCEL_TRADE, contracts.getFirst().id(), "", 0));
        button("수입 계약 체결", "수입 계약", x + 10, y + h - 30, w - 20, 23,
                p.route() && p.exports().getOrDefault(resource, 0.0) > 0 && tradeAmount <= view.economy().freeCivilian(), () -> {
            String target = partner; partner = ""; send(TRADE, target, resource, tradeAmount);
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
            send(view == null ? OPEN : REFRESH, "", "", 0);
        } else if (++refresh % 60 == 0 && pending == 0) send(view == null ? OPEN : REFRESH, "", "", 0);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (y < top && view != null) { hudScroll = HoiMenuBar.scroll(width, view.hud(), hudScroll, horizontal, vertical); return true; }
        if (designer && selectedSlot >= 0) pickerScroll -= (int) vertical;
        else if (designer) designerScroll -= (int) (vertical * 25);
        else if (!picker.isEmpty()) pickerScroll -= (int) (vertical * 55);
        else if (modal()) return super.mouseScrolled(x, y, horizontal, vertical);
        else if (tab == MenuTab.RECRUITMENT && x >= pane) secondScroll -= (int) (vertical * 40);
        else scroll -= (int) (vertical * (tab == MenuTab.PRODUCTION ? productionRowHeight(top + 155) : tab == MenuTab.RECRUITMENT ? (compact() ? 85 : 93) : tab == MenuTab.LOGISTICS ? 68 : tradeTab == 0 ? 35 : 34));
        rebuildWidgets(); return true;
    }
    @Override public void removed() {
        SidebarMovement.release(minecraft);
        if (DialogClient.suspending()) return;
        closed = true; selectingDeployment = false; dev.hoi.client.map.AtlasSceneClient.clearDeployment();
        transport.accept(new IndustryProtocol.Request(CLOSE, token, view == null ? 0 : view.revision(), "", "", 0));
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (!modal() && (event.button() == 0 || event.button() == 1) && view != null && pending == 0) {
            for (var slot : deploymentLocations) if (event.x() >= slot.x() && event.x() < slot.x() + slot.width() && event.y() >= slot.y() && event.y() < slot.y() + slot.height()) {
                locationRecruit = slot.recruit();
                selectingDeployment = event.button() == 0;
                dev.hoi.client.map.AtlasSceneClient.clearDeployment();
                send(selectingDeployment ? SELECT_LOCATION : LOCATION, slot.recruit(), "", 0);
                return true;
            }
            if (selectingDeployment && event.x() >= pane + 3 + rightWidth() && event.y() > top + 22 && minecraft.player != null) {
                if (event.button() == 1) {selectingDeployment = false; dev.hoi.client.map.AtlasSceneClient.clearDeployment(); return true;}
                if (!minecraft.options.getCameraType().isFirstPerson()) return true;
                var matrix = minecraft.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
                var ray = matrix.transformProject(new org.joml.Vector3f((float)(event.x() / width * 2 - 1), (float)(1 - event.y() / height * 2), 1)).normalize();
                pending = 100;
                transport.accept(new IndustryProtocol.Request(LOCATION_AT, token, view.revision(), locationRecruit, "", 0, ray.x, ray.y, ray.z));
                rebuildWidgets();return true;
            }
        }
        return super.mouseClicked(event, twice);
    }
    @Override public boolean allowsMovement() { return !modal(); }
    @Override public boolean keyPressed(KeyEvent e) { return !modal() && SidebarMovement.consumes(minecraft, e) || super.keyPressed(e); }
    @Override public boolean keyReleased(KeyEvent e) { return !modal() && SidebarMovement.consumes(minecraft, e) || super.keyReleased(e); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private Equipment equipment(String id) { return view.equipment().stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow(); }
    private int factoryStep() {
        long window = minecraft.getWindow().handle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == 1
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == 1 ? 10 : 1;
    }
    private static String equipmentName(Equipment e) { return e.name(); }
    private Template template(String id) { return view.templates().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow(); }
    private String locationName(String id) { if (id.isEmpty()) return "장소 미지정"; return view.locations().stream().filter(l -> l.id().equals(id)).map(Choice::name).map(n -> n.replace("수도 · ", "").split(" · ", 2)[0]).findFirst().orElse(id); }
    private String resourceName(String id) { return view.resources().stream().filter(r -> r.id().equals(id)).map(Resource::name).findFirst().orElse(id); }
    private String unitName(String id, boolean support) { return (support ? view.companies() : view.battalions()).stream().filter(c -> c.id().equals(id)).map(Choice::name).map(n -> n.replace("수도 · ", "").split(" · ", 2)[0]).findFirst().orElse(id); }
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
        if (designer && !background) {
            float scale = Math.min(1f, Math.max(1, max) / (float)Math.max(1, font.width(value)));
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y + (1 - scale) * 4);
            graphics.pose().scale(scale);
            graphics.text(font, value, 0, 0, color);
            graphics.pose().popMatrix();
            return;
        }
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
        var button = new PanelButton(accessible, x, y, w, h, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                HoiMenuStyle.control(g, x, y, w, h, false, false);
                String caption = font.plainSubstrByWidth(label, Math.max(1, w - 6));
                g.centeredText(font, caption, x + w / 2, y + (h - font.lineHeight) / 2, active ? TEXT : MUTED);
            }
        };
        button.active = enabled && pending == 0; button.setTooltip(Tooltip.create(Component.literal(accessible)));
        addRenderableWidget(button);
    }
    private void equipmentButton(Equipment equipment, String name, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new PanelButton(name, x, y, w, h, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                UiAssets.draw(g, equipment.texture(), x, y, w, h);
                if (equipment.outdated()) UiAssets.draw(g, "production/outdated_overlay", x, y, w, h);
            }
        };
        button.active = enabled && pending == 0; addRenderableWidget(button);
    }
    private void imageButton(String name, String texture, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new HoiMenuButton(name, texture, false, x, y, w, h, action);
        button.active = enabled && pending == 0; addRenderableWidget(button);
    }
    private void originalButton(String name, String texture, int x, int y, int w, int h, Runnable action) {
        if (graphics != null) return;
        var button = new PanelButton(name, x, y, w, h, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                UiAssets.draw(g, texture, x, y, w, h);
            }
        };
        button.active = pending == 0;
        button.setTooltip(Tooltip.create(Component.literal(name)));
        addRenderableWidget(button);
    }
    private void divisionSlot(String name, String texture, int x, int y, int w, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new PanelButton(name, x, y, w, 22, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                HoiMenuStyle.recess(g, x, y, w, 22);
                UiAssets.draw(g, "production/designer/slot", x + 1, y + 1, w - 2, 20);
                UiAssets.draw(g, texture, x + 2, y + 2, w - 4, 18);
            }
        };
        button.active = enabled && pending == 0;
        button.setTooltip(Tooltip.create(Component.literal(name)));
        addRenderableWidget(button);
    }
    private void unitChoice(String name, String texture, int x, int y, int w, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new PanelButton(name, x, y, w, 25, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                HoiMenuStyle.control(g, x, y, w, 25, false, false);
                UiAssets.draw(g, texture, x + 4, y + 3, 26, 19);
                g.centeredText(font, font.plainSubstrByWidth(name, Math.max(1, w - 38)), x + 34 + (w - 38) / 2, y + (25 - font.lineHeight) / 2, active ? TEXT : MUTED);
            }
        };
        button.active = enabled && pending == 0;
        button.setTooltip(Tooltip.create(Component.literal(name)));
        addRenderableWidget(button);
    }
}
