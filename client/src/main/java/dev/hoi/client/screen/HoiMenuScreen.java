package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.UiSounds;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.PoliticsLayout;
import dev.hoi.client.ui.HoiMenuButton;
import dev.hoi.client.ui.HoiMenuStyle;
import dev.hoi.client.ui.HoiPanelLayout;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.HoiTooltips;

import dev.hoi.protocol.MenuTab;
import dev.hoi.protocol.MenuView;
import dev.hoi.protocol.CountryHud;
import dev.hoi.protocol.MenuProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;


public final class HoiMenuScreen extends Screen implements SidebarMovement.Screen {
    private static final int TEXT = HoiMenuStyle.TEXT, GOLD = 0xFFFFAA00, MUTED = HoiMenuStyle.MUTED;
    private MenuView view;
    private final Runnable researchOpen;
    private final String token = UUID.randomUUID().toString();
    private int refreshTicks;
    private MenuTab selected = MenuTab.POLITICS;
    private final Set<Integer> collapsed = new HashSet<>();
    private MenuView.Entry detail;
    private String focusTarget;
    private String detailIcon;
    private String manufacturerGroup, manufacturerDetailId;
    private int manufacturerScroll;
    private int spiritScroll, tooltipScroll;
    private MenuView.Entry hoveredPolitics;
    private List<MenuView.Entry> spirits = List.of();
    private int pane, top, scroll, total, detailScroll, tradeTab, hudScroll;
    private List<MenuView.Entry> parties = List.of();
    private int partyScroll;
    private final List<int[]> partySpans = new ArrayList<>();
    private static final String[] POLITICS_IDS = {"government", "economic_laws", "military_laws", "social_laws", "development", "military_staff", "research_production"};
    private static final String[] POLITICS_NAMES = {"정부", "경제법", "군법", "사회법", "발전도", "군사 참모", "연구 & 생산"};

    public HoiMenuScreen(MenuView view) { this(view, MenuTab.POLITICS, HoiClient::open); }
    public HoiMenuScreen(MenuTab selected) { this(null, selected, HoiClient::open); }
    static HoiMenuScreen forFocus(String id) {
        HoiClient.cancelOpen();
        var screen = new HoiMenuScreen(MenuTab.POLITICS);
        screen.focusTarget = id;
        return screen;
    }
    public HoiMenuScreen(MenuView view, MenuTab selected, Runnable researchOpen) {
        super(Component.literal("HOI · 국가 메뉴"));
        this.view = view; this.selected = selected; this.researchOpen = researchOpen;
    }
    public String token() { return token; }
    public void update(MenuView next) {
        if (next.equals(view)) return;
        if (view != null && !view.country().equals(next.country())) { selected = MenuTab.POLITICS; collapsed.clear(); manufacturerGroup = null; manufacturerDetailId = null; }
        var old = detail;
        boolean sameCountry = view != null && view.country().equals(next.country());
        view = next;
        if (focusTarget != null) {
            detail = next.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 중점"))
                    .flatMap(s -> s.entries().stream()).filter(e -> e.detail().lines().findFirst().orElse("").equals(focusTarget))
                    .findFirst().orElse(null);
            detailIcon = "research"; detailScroll = 0; focusTarget = null;
        }
        if (old != null) {
            if (!sameCountry) detail = null;
            else if (manufacturerDetailId != null) detail = next.manufacturers().stream().filter(m -> m.id().equals(manufacturerDetailId)).map(MenuView.Manufacturer::entry).findFirst().orElse(null);
            else if (old.name().equals("국가 중점")) detail = politicsFocus();
            else detail = next.page(selected).sections().stream().filter(s -> s.icon().equals(detailIcon))
                    .flatMap(s -> s.entries().stream()).filter(e -> e.name().equals(old.name())).findFirst().orElse(null);
        }
        rebuildWidgets();
    }
    public MenuTab selectedTab() { return selected; }
    public int panelWidth() { return pane; }
    @Override public boolean allowsMovement() { return detail == null && manufacturerGroup == null; }
    @Override protected void init() {
        pane = HoiPanelLayout.width(selected, width);
        top = HoiMenuBar.height(width);
        HoiMenuBar.buttons(width, view == null ? "" : view.country(), selected, tab -> {
            if (tab == MenuTab.RESEARCH) { researchOpen.run(); return; }
            if (tab == MenuTab.CONSTRUCTION && ClientPlayNetworking.canSend(dev.hoi.protocol.ConstructionProtocol.Request.TYPE)) { HoiClient.openConstruction(); return; }
            if (IndustryScreen.supports(tab) && ClientPlayNetworking.canSend(dev.hoi.protocol.IndustryProtocol.Request.TYPE)) { HoiClient.openIndustry(tab); return; }
            HoiClient.cancelOpen();
            selected = tab; scroll = 0; collapsed.clear(); detail = null; manufacturerGroup = null; manufacturerDetailId = null; rebuildWidgets();
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new HoiMenuButton("×", pane - 25, top + 3, 19, 19, this::onClose));
        if (view == null) return;
        if (selected == MenuTab.POLITICS) {
            preparePartyChart();
            spirits = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 정신"))
                    .flatMap(s -> s.entries().stream()).filter(e -> !e.name().equals("없음")).toList();
            spiritScroll = Math.clamp(spiritScroll, 0, maximumSpiritScroll());
            var focusBox = politicsLayout().focusTitle();
            addRenderableWidget(new InvisibleButton("국가 중점", focusBox.x(), focusBox.y(), focusBox.width(), focusBox.height(),
                    () -> showDetail(politicsFocus(), "research")));
            var factionBox = politicsLayout().faction();
            addRenderableWidget(new InvisibleButton("세력", factionBox.x(), factionBox.y(), factionBox.width(), factionBox.height(),
                    () -> HoiClient.openCountry("")));
        }
        if (selected == MenuTab.TRADE) {
            addRenderableWidget(new HoiMenuButton("경제", "category/industry", tradeTab == 0, 8, top + 30, (pane - 20) / 2, 24,
                    () -> { tradeTab = 0; scroll = 0; rebuildWidgets(); }));
            addRenderableWidget(new HoiMenuButton("무역", "menu/trade", tradeTab == 1, pane / 2, top + 30, (pane - 20) / 2, 24,
                    () -> { tradeTab = 1; scroll = 0; rebuildWidgets(); }));
        }
        layoutRows(false, null);
        scroll = Math.clamp(scroll, 0, Math.max(0, total - (contentBottom() - contentTop())));
        layoutRows(true, null);
        if (selected == MenuTab.RESEARCH) addRenderableWidget(new HoiMenuButton("연구 선택", 8, height - 27, 88, 20, researchOpen));
        if (selected == MenuTab.INTELLIGENCE) addRenderableWidget(new HoiMenuButton("기관 관리", 8, height - 27, Math.min(80, pane - 16), 20, () -> {
            if (ClientPlayNetworking.canSend(dev.hoi.protocol.AgencyProtocol.Request.TYPE)) {
                var screen = new AgencyScreen(this); minecraft.gui.setScreen(screen); screen.open();
            }
        }));
        if (manufacturerGroup != null && detail == null) layoutManufacturers(true, null);
        if (detail != null) {
            SidebarMovement.release(minecraft);
            int x = detailX(), w = detailWidth();
            addRenderableWidget(new HoiMenuButton("×", x + w - 25, top + 3, 19, 19, () -> { detail = null; rebuildWidgets(); }));
        }
    }
    private int contentTop() { return selected == MenuTab.POLITICS ? politicsLayout().contentTop()
            : top + (selected == MenuTab.TRADE ? 62 : selected == MenuTab.INTELLIGENCE ? 82 : 32); }
    private boolean hasFooterActions() { return selected == MenuTab.RESEARCH || selected == MenuTab.INTELLIGENCE; }
    private int contentBottom() { return height - (hasFooterActions() ? 36 : 8); }
    private int detailX() { return width - pane >= 240 ? pane + 6 : Math.max(8, (width - detailWidth()) / 2); }
    private int detailWidth() { return Math.min(310, width - (width - pane >= 240 ? pane + 14 : 16)); }
    private int rowHeight() { return selected == MenuTab.PRODUCTION || selected == MenuTab.RECRUITMENT ? 49 : 37; }
    private void layoutRows(boolean widgets, GuiGraphicsExtractor g) {
        if (selected == MenuTab.POLITICS) { layoutPolitics(widgets, g); return; }
        if (selected == MenuTab.OFFICER_CORPS) { layoutOfficers(widgets, g); return; }
        int y = contentTop() - scroll;
        var sections = view.page(selected).sections();
        for (int index = 0; index < sections.size(); index++) {
            if (selected == MenuTab.TRADE && (tradeTab == 0 ? index != 0 : index == 0)) continue;
            var section = sections.get(index); final int sectionIndex = index;
            if (g != null) {
                HoiMenuStyle.metal(g, 8, y, pane - 16, 21);
                UiAssets.draw(g, texture(section.icon()), 13, y + 3, 19, 15);
                g.text(font, trim(section.title(), pane - 68), 37, y + 6, TEXT);
                g.text(font, collapsed.contains(index) ? "+" : "−", pane - 25, y + 6, GOLD);
            }
            if (widgets && y >= contentTop() && y + 21 <= contentBottom()) {
                var header = new InvisibleButton(section.title(), 8, y, pane - 16, 21, () -> {
                    if (!collapsed.add(sectionIndex)) collapsed.remove(sectionIndex); rebuildWidgets();
                });
                header.active = detail == null; addRenderableWidget(header);
            }
            y += 25;
            if (!collapsed.contains(index)) for (var entry : section.entries()) {
                int h = rowHeight();
                if (g != null) {
                    g.fillGradient(10, y, pane - 10, y + h - 3, 0xFF303136, 0xFF1B1C21);
                    HoiMenuStyle.bevel(g, 10, y, pane - 20, h - 3, false);
                    String icon = section.icon();
                    String texture = texture(icon);
                    HoiMenuStyle.recess(g, 13, y + 3, 32, h - 9);
                    UiAssets.draw(g, texture, 16, y + 5, 26, h - 13);
                    g.text(font, trim(entry.name(), pane - 67), 51, y + 6, TEXT);
                    g.text(font, trim(entry.value(), pane - 67), 51, y + 19, GOLD);
                    if (entry.progress() >= 0) {
                        g.fill(50, y + h - 8, pane - 17, y + h - 5, 0xFF090C08);
                        g.fillGradient(50, y + h - 8, 50 + (int)((pane - 67) * entry.progress()), y + h - 5, 0xFFADB872, 0xFF52602F);
                    }
                }
                if (widgets && y >= contentTop() && y + h <= contentBottom()) {
                    var row = new InvisibleButton(entry.name() + " · " + entry.value(), 10, y, pane - 20, h - 3,
                            () -> { detail = entry; detailIcon = section.icon(); detailScroll = 0; rebuildWidgets(); });
                    row.active = detail == null; addRenderableWidget(row);
                }
                y += h;
            }
            y += 6;
        }
        total = y - contentTop() + scroll;
    }
    private void layoutOfficers(boolean widgets, GuiGraphicsExtractor g) {
        int y = contentTop() - scroll;
        var sections = view.page(MenuTab.OFFICER_CORPS).sections();
        var commanders = sections.stream().filter(v -> v.icon().equals("high_command")).flatMap(v -> v.entries().stream()).toList();
        if (g != null) HoiMenuStyle.metal(g, 8, y, pane - 16, 36);
        int count = Math.min(3, commanders.size()), cell = Math.min(31, (pane - 65) / 3);
        for (int i = 0; i < count; i++) {
            var entry = commanders.get(i); int bx = 11 + i * cell;
            if (g != null) {
                UiAssets.draw(g, entry.icon().isEmpty() ? "politics/high_command" : entry.icon(), bx, y + 3, cell - 2, 30);
            }
            officerControl(widgets, entry, "high_command", bx, y, cell - 2, 36);
        }
        var tactic = sections.stream().filter(v -> v.icon().equals("preferred_tactic")).flatMap(v -> v.entries().stream()).findFirst().orElse(null);
        if (tactic != null) {
            if (g != null) UiAssets.draw(g, "officer/preferred_tactic", pane - 65, y + 4, 48, 28);
            officerControl(widgets, tactic, "preferred_tactic", pane - 65, y + 4, 48, 28);
        }
        y += 44;
        for (String branch : List.of("army", "navy", "air", "special")) {
            var section = sections.stream().filter(v -> v.icon().equals(branch)).findFirst().orElse(null);
            if (section == null) continue;
            String artBranch = branch.equals("special") ? "army" : branch;
            var entries = section.entries(); boolean special = branch.equals("special");
            int h = special ? 68 : 122;
            if (g != null) {
                HoiMenuStyle.recess(g, 8, y, pane - 16, h);
                UiAssets.cover(g, "officer/" + artBranch + "/background", 9, y + 17, pane - 18, h - 18);
                UiAssets.cover(g, "officer/" + artBranch + "/header", 9, y, pane - 18, 15);
                UiAssets.draw(g, "officer/" + branch + "/banner", 8, y - 4, 49, 35);
                officerText(g, section.title(), 60, y + 4, pane - 72, TEXT);
            }
            if (!special && !entries.isEmpty()) {
                var chief = entries.getFirst();
                if (g != null) {
                    UiAssets.draw(g, chief.icon(), 57, y + 23, 29, 37);
                    if (!chief.value().equals("미지정")) officerText(g, chief.value(), 89, y + 28, pane / 2 - 83, TEXT);
                }
                officerControl(widgets, chief, branch, 54, y + 22, Math.max(29, pane / 2 - 48), 39);
            }
            int doctrineIndex = special ? 0 : 1;
            if (entries.size() > doctrineIndex) {
                var doctrine = entries.get(doctrineIndex); int dx = special ? pane - 61 : pane * 57 / 100;
                if (g != null) {
                    UiAssets.draw(g, "officer/" + branch + "/doctrine_frame", dx, y + 19, 43, 44);
                    UiAssets.draw(g, doctrine.icon(), dx + 6, y + 24, 31, 31);
                    if (doctrine.progress() > 0) g.fill(dx + 9, y + 59, dx + 34, y + 61, 0xFFFFAA00);
                }
                officerControl(widgets, doctrine, branch, dx, y + 19, 43, 44);
            }
            int spirits = Math.max(0, entries.size() - 2);
            for (int i = 0; !special && i < spirits; i++) {
                var entry = entries.get(i + 2); int bw = (pane - 32) / 3;
                int bx = (pane - (spirits * bw + (spirits - 1) * 10)) / 2 + i * (bw + 10), by = y + 79;
                if (g != null) {
                    HoiMenuStyle.recess(g, bx, by, bw, 28);
                    if (entry.progress() > 0) g.fillGradient(bx + 1, by + 1, bx + bw - 1, by + 27, 0xFF6C8035, 0xFF293515);
                    UiAssets.draw(g, entry.icon(), bx + 2, by - 9, bw - 4, 38);
                }
                officerControl(widgets, entry, branch, bx, by, bw, 28);
            }
            y += h + 4;
        }
        total = y - contentTop() + scroll;
    }
    private void officerControl(boolean widgets, MenuView.Entry entry, String section, int x, int y, int w, int h) {
        if (!widgets || y < contentTop() || y + h > contentBottom()) return;
        var button = new InvisibleButton(entry.name() + " · " + entry.value(), x, y, w, h, () -> showDetail(entry, section));
        if (!entry.value().equals("미지정") && !section.equals("preferred_tactic") && !entry.name().endsWith("교리"))
            button.setTooltip(Tooltip.create(Component.literal(entry.name() + "\n" + entry.value())));
        else button.setTooltip(null);
        button.active = detail == null;
        addRenderableWidget(button);
    }
    private void officerText(GuiGraphicsExtractor g, String value, int x, int y, int available, int color) {
        float scale = Math.min(.75f, available / (float)Math.max(1, font.width(value)));
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale);
        g.text(font, value, 0, 0, color); g.pose().popMatrix();
    }
    private void showDetail(MenuView.Entry entry, String icon) {
        detail = entry; detailIcon = icon; detailScroll = 0; manufacturerDetailId = null; rebuildWidgets();
    }
    private String fontSafe(String text) { return text.length() <= 2000 ? text : text.substring(0, 1999) + "…"; }
    private MenuView.Entry politicsFocus() {
        var entries = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 중점")).flatMap(s -> s.entries().stream()).toList();
        var active = entries.stream().filter(e -> e.progress() >= 0 || e.value().contains("진행")).findFirst();
        String label = active.map(MenuView.Entry::name).orElse("국가 중점 선택");
        return new MenuView.Entry("국가 중점", label, fontSafe(entries.stream().map(e -> e.name() + " · " + e.value() + "\n" + e.detail()).collect(java.util.stream.Collectors.joining("\n\n"))), -1, active.map(MenuView.Entry::icon).orElse(""));
    }
    private void drawPoliticsBanner(GuiGraphicsExtractor g) {
        var layout = politicsLayout(); var leader = layout.leader();
        var person = politicsEntry("지도자");
        UiAssets.draw(g, person.icon().isEmpty() ? "politics/empty/leader" : person.icon(),
                leader.x() + leader.width() * 7 / 172, leader.y() + leader.height() * 6 / 258,
                leader.width() * 158 / 172, leader.height() * 210 / 258);
        drawPoliticalArt(g, "politics/leader_frame", leader);
        centeredPoliticsText(g, person.value(), new PoliticsLayout.Box(leader.x() + 4, leader.y() + leader.height() * 218 / 258,
                leader.width() - 8, leader.height() * 34 / 258), TEXT);
        drawPoliticalArt(g, "politics/focus_background", layout.focus());
        drawPoliticalArt(g, "politics/focus_select", layout.focusTitle());
        var focus = politicsFocus();
        drawPoliticalArt(g, focus.icon().isEmpty() ? "politics/empty/focus" : focus.icon(), layout.focusImage());
        centeredPoliticsText(g, focus.value(), layout.focusTitle(), focus.value().equals("국가 중점 선택") ? 0xFFFFFFFF : GOLD);
        politicsCell(g, "경제-정치 연합", layout.union());
        var ideology = politicsEntry("세부 이념"); var ideologyBox = layout.ideology();
        HoiMenuStyle.recess(g, ideologyBox.x(), ideologyBox.y(), ideologyBox.width(), ideologyBox.height());
        String ideologyArt = ideology.icon().isEmpty() ? politicsEntry("이념").icon() : ideology.icon();
        drawPoliticalArt(g, ideologyArt.isEmpty() ? "politics/empty/ideology" : ideologyArt, ideologyBox);
        var strip = layout.spirits();
        HoiMenuStyle.recess(g, strip.x(), strip.y(), strip.width(), strip.height());
        g.enableScissor(strip.x() + 4, strip.y() + 2, strip.x() + strip.width() - 4, strip.y() + strip.height() - 4);
        int sx = spiritStart() - spiritScroll;
        for (var spirit : spirits) {
            if (sx + spiritPitch() - 3 > strip.x() + 4 && sx < strip.x() + strip.width() - 4)
                UiAssets.draw(g, spirit.icon().isEmpty() ? "menu/politics" : spirit.icon(), sx + 1, strip.y() + 3, spiritPitch() - 3, spiritPitch() - 3);
            sx += spiritPitch();
        }
        g.disableScissor();
        if (maximumSpiritScroll() > 0) {
            int track = strip.width() - 8, thumb = Math.max(8, track * track / (spirits.size() * spiritPitch()));
            int x = spiritStart() + spiritScroll * (track - thumb) / maximumSpiritScroll();
            g.fill(x, strip.y() + strip.height() - 4, x + thumb, strip.y() + strip.height() - 2, 0xFF929497);
        }
        politicsCell(g, "정치 체제", layout.government());
        var electionBox = layout.election();
        HoiMenuStyle.recess(g, electionBox.x(), electionBox.y(), electionBox.width(), electionBox.height());
        String election = politicsEntry("다음 선거").value();
        int textY = electionBox.y() + (electionBox.height() - 7) / 2;
        if (election.equals("선거 없음")) centeredPoliticsText(g, election, electionBox, MUTED);
        else {
            String label = "다음 선거 "; int labelWidth = (int)Math.ceil(font.width(label) * .75f);
            officerText(g, label, electionBox.x() + 4, textY, labelWidth, TEXT);
            officerText(g, election, electionBox.x() + 4 + labelWidth, textY, electionBox.width() - 8 - labelWidth, GOLD);
        }
        politicsCell(g, "경제 모델", layout.economy());
        politicsCell(g, "세력", layout.faction());
        for (int i = 0; i < 2; i++) {
            var entry = politicsEntry(i == 0 ? "점령지" : "순응도");
            drawPoliticalArt(g, "politics/" + (i == 0 ? "occupied" : "collaboration") + (entry.progress() > 0 ? "_active" : "_inactive"), layout.status(i));
        }
        var chart = layout.partyChart(); int cy = chart.y() + chart.height() / 2;
        HoiMenuStyle.recess(g, chart.x(), chart.y(), chart.width(), chart.height());
        for (int[] span : partySpans) g.fill(chart.x() + chart.width() / 2 + span[0], cy + span[1], chart.x() + chart.width() / 2 + span[2], cy + 1 + span[1], span[3]);
        var list = layout.parties();
        HoiMenuStyle.recess(g, list.x(), list.y(), list.width(), list.height());
        partyScroll = Math.clamp(partyScroll, 0, Math.max(0, parties.size() * 9 - list.height() + 8));
        g.enableScissor(list.x() + 3, list.y() + 3, list.x() + list.width() - 3, list.y() + list.height() - 3);
        int py = list.y() + 4 - partyScroll;
        for (var party : parties) {
            g.fill(list.x() + 4, py, list.x() + 9, py + 6, partyColor(party.icon()));
            officerText(g, party.name() + " (" + party.value() + ")", list.x() + 12, py, list.width() - 17, TEXT);
            py += 9;
        }
        if (parties.isEmpty()) officerText(g, "정보 없음", list.x() + 5, list.y() + 5, list.width() - 10, MUTED);
        g.disableScissor();
        if (parties.size() * 9 > list.height() - 8) {
            int track = list.height() - 6, thumb = Math.max(6, track * (list.height() - 8) / (parties.size() * 9));
            int y = list.y() + 3 + partyScroll * (track - thumb) / Math.max(1, parties.size() * 9 - list.height() + 8);
            g.fill(list.x() + list.width() - 3, y, list.x() + list.width() - 2, y + thumb, 0xFF929497);
        }
        g.fill(8, layout.contentTop() - 5, pane - 8, layout.contentTop() - 4, 0xFF68686B);
    }
    private void drawPoliticalArt(GuiGraphicsExtractor g, String art, PoliticsLayout.Box box) {
        UiAssets.draw(g, art, box.x(), box.y(), box.width(), box.height());
    }
    private void centeredPoliticsText(GuiGraphicsExtractor g, String text, PoliticsLayout.Box box, int color) {
        int textWidth = Math.max(1, Math.min(box.width() - 8, (int)Math.ceil(font.width(text) * .75f)));
        officerText(g, text, box.x() + (box.width() - textWidth) / 2, box.y() + (box.height() - 7) / 2, textWidth, color);
    }
    private void politicsCell(GuiGraphicsExtractor g, String label, PoliticsLayout.Box box) {
        politicsCell(g, label, box.x(), box.y(), box.width(), box.height());
    }
    private MenuView.Entry politicsEntry(String name) {
        return view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 현황"))
                .flatMap(s -> s.entries().stream()).filter(e -> e.name().equals(name)).findFirst()
                .orElse(new MenuView.Entry(name, "정보 없음", ""));
    }
    private void politicsCell(GuiGraphicsExtractor g, String label, int x, int y, int w, int h) {
        HoiMenuStyle.recess(g, x, y, w, h);
        String kind = switch (label) {
            case "경제 모델" -> "economy";
            case "세력" -> "faction";
            case "정치 체제" -> "government";
            default -> "union";
        };
        UiAssets.draw(g, politicsEntry(label).icon().isEmpty() ? "politics/empty/" + kind : politicsEntry(label).icon(), x + 3, y + 3, w - 6, h - 6);
    }
    private static int partyColor(String icon) {
        if (icon.matches("politics/party/[0-9a-fA-F]{6}")) return 0xFF000000 | Integer.parseInt(icon.substring(icon.lastIndexOf('/') + 1), 16);
        return switch (icon) {
            case "politics/ideology/democratic" -> 0xFF4267CE;
            case "politics/ideology/communist" -> 0xFFB52C39;
            case "politics/ideology/fascist" -> 0xFF97652F;
            default -> 0xFF939393;
        };
    }
    private void preparePartyChart() {
        parties = view.page(MenuTab.POLITICS).sections().stream().flatMap(s -> s.entries().stream())
                .filter(e -> (e.icon().startsWith("politics/ideology/") || e.icon().startsWith("politics/party/")) && e.progress() >= 0).toList();
        partySpans.clear();
        for (int y = -15; y <= 15; y++) {
            int edge = (int)Math.sqrt(225 - y * y), start = -edge, previous = 0;
            for (int x = -edge; x <= edge + 1; x++) {
                int color = 0xFF34363A;
                double angle = (Math.atan2(y, x) + Math.PI * 2.5) % (Math.PI * 2) / (Math.PI * 2), sum = 0;
                for (var party : parties) { sum += party.progress(); if (angle < sum) { color = partyColor(party.icon()); break; } }
                if (x == -edge) previous = color;
                if (color != previous || x > edge) { partySpans.add(new int[]{start, y, x, previous}); start = x; previous = color; }
            }
        }
    }
    private void layoutPolitics(boolean widgets, GuiGraphicsExtractor g) {
        int row = Math.clamp((contentBottom() - contentTop()) / 7, 37, 68);
        int y = contentTop() - scroll;
        for (int i = 0; i < POLITICS_IDS.length; i++) {
            final int index = i;
            var section = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.icon().equals(POLITICS_IDS[index])).findFirst()
                    .orElse(new MenuView.Section(POLITICS_NAMES[i], POLITICS_IDS[i], List.of()));
            if (g != null) {
                HoiMenuStyle.metal(g, 9, y, pane - 22, 15);
                UiAssets.draw(g, "politics/header/" + new int[]{2, 1, 4, 5, 6, 7, 8}[i], 13, y + 1, 21, 13);
                g.text(font, section.title(), 39, y + 4, TEXT);
            }
            int columns = 6, cell = (pane - 36) / columns, iconSize = Math.min(cell - 4, row - 21);
            var entries = section.entries();
            int count = Math.max(1, entries.size());
            for (int j = 0; j < count; j++) {
                int x = 16 + Math.max(0, columns - count) * cell / 2 + (j % columns) * cell, cy = y + 17 + (j / columns) * (row - 17);
                var entry = entries.isEmpty() ? new MenuView.Entry(section.title(), "정보 없음", "서버가 이 구역의 상태를 제공하지 않습니다.") : entries.get(j);
                if (g != null) {
                    String icon = entry.icon().isEmpty() ? "menu/politics" : entry.icon();
                    UiAssets.draw(g, icon, x, cy, cell - 4, iconSize);
                    if (!icon.equals("politics/vacant") && (entry.value().equals("정보 없음") || entry.value().equals("미지정")))
                        g.fill(x, cy + iconSize - 2, x + cell - 4, cy + iconSize, 0xA0404246);
                }
                if (widgets && cy >= contentTop() && cy + iconSize <= contentBottom()) {
                    final int position = j;
                    var button = new InvisibleButton(entry.name() + " · " + entry.value(), x, cy, cell - 4, iconSize, () -> {
                        if (section.icon().equals("research_production") && position < 4) {
                            manufacturerGroup = List.of("armor", "navy", "air", "materiel").get(position);
                            manufacturerScroll = 0; detail = null; manufacturerDetailId = null; rebuildWidgets();
                        } else showDetail(entry, section.icon());
                    });
                    button.active = detail == null; addRenderableWidget(button);
                }
            }
            y += row + (count - 1) / columns * (row - 17);
        }
        total = y - contentTop() + scroll;
    }
    private void requestRefresh() {
        if (ClientPlayNetworking.canSend(MenuProtocol.Refresh.TYPE)) ClientPlayNetworking.send(new MenuProtocol.Refresh(token));
    }
    @Override public void tick() {
        if (refreshTicks++ % 40 == 0) requestRefresh();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        HoiMenuBar.draw(g, width, view == null ? dev.hoi.protocol.CountryHud.UNKNOWN : view.hud(), mx, my, hudScroll);
        HoiMenuStyle.panel(g, 0, top, pane, height - top);
        g.text(font, trim(selected == MenuTab.POLITICS ? "정치" : selected.label(), pane - 44), 10, top + 9, TEXT);
        if (view == null) {
            g.text(font, trim("서버 정보 불러오는 중…", pane - 20), 10, top + 40, MUTED);
            super.extractRenderState(g, mx, my, delta);
            return;
        }
        if (selected == MenuTab.POLITICS) drawPoliticsBanner(g);
        if (selected == MenuTab.INTELLIGENCE) {
            HoiMenuStyle.recess(g, 8, top + 30, pane - 16, 46);
            if (!view.country().equals("KOR") || !UiAssets.draw(g, "country/kor/intelligence", 18, top + 33, 42, 40))
                UiAssets.draw(g, "menu/intelligence", 18, top + 33, 42, 40);
            g.text(font, trim("정보기관", pane - 86), 76, top + 43, TEXT);
            g.text(font, trim(view.countryName(), pane - 86), 76, top + 58, MUTED);
        }
        HoiMenuStyle.recess(g, 5, contentTop() - 3, pane - 10, Math.max(6, contentBottom() - contentTop() + 5));
        g.enableScissor(6, contentTop(), pane - 6, contentBottom()); layoutRows(false, g); g.disableScissor();
        int visible = contentBottom() - contentTop();
        if (total > visible && visible > 0) {
            int thumb = Math.max(10, visible * visible / total);
            int y = contentTop() + scroll * (visible - thumb) / Math.max(1, total - visible);
            g.fill(pane - 7, contentTop(), pane - 3, contentBottom(), 0xFF08090B);
            HoiMenuStyle.metal(g, pane - 7, y, 4, thumb);
        }
        if (hasFooterActions()) HoiMenuStyle.metal(g, 0, height - 33, pane, 33);
        if (manufacturerGroup != null) layoutManufacturers(false, g);
        if (detail != null) drawDetail(g);
        super.extractRenderState(g, mx, my, delta);
        var hovered = politicsHover(mx, my);
        if (!Objects.equals(hovered, hoveredPolitics)) tooltipScroll = 0;
        hoveredPolitics = hovered;
        if (hovered != null) {
            String title = hovered.name().equals("지도자") || hovered.name().equals("세부 이념") ? hovered.value() : hovered.name();
            var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
            lines.add(Component.literal(title).withStyle(net.minecraft.ChatFormatting.GOLD).getVisualOrderText());
            lines.addAll(HoiTooltips.lines(font, hovered.detail(), width));
            int count = Math.max(1, (height - 48) / (font.lineHeight + 1));
            tooltipScroll = Math.clamp(tooltipScroll, 0, Math.max(0, lines.size() - count));
            g.setTooltipForNextFrame(font, lines.subList(tooltipScroll, Math.min(lines.size(), tooltipScroll + count)), mx, my);
        }
    }
    PoliticsLayout politicsLayout() { return new PoliticsLayout(pane, top); }
    CountryHud hud() { return view == null ? CountryHud.UNKNOWN : view.hud(); }
    int spiritPitch() { return Math.max(18, politicsLayout().spirits().height() - 5); }
    private int maximumSpiritScroll() { return Math.max(0, spirits.size() * spiritPitch() - (politicsLayout().spirits().width() - 8)); }
    private int spiritStart() { return politicsLayout().spirits().x() + 4; }
    MenuView.Entry politicsHover(double x, double y) {
        if (view == null || selected != MenuTab.POLITICS || detail != null || manufacturerGroup != null) return null;
        var layout = politicsLayout();
        if (layout.leader().contains(x, y)) {
            var leader = politicsEntry("지도자");
            return leader.detail().isBlank() ? null : leader;
        }
        var strip = layout.spirits();
        if (x >= strip.x() + 4 && x < strip.x() + strip.width() - 4 && y >= strip.y() + 2 && y < strip.y() + strip.height() - 4) {
            int position = (int)x - spiritStart() + spiritScroll, index = position / spiritPitch();
            if (position >= 0 && position % spiritPitch() >= 1 && position % spiritPitch() < spiritPitch() - 2 && index < spirits.size()) return spirits.get(index);
        }
        if (layout.ideology().contains(x, y)) {
            var ideology = politicsEntry("세부 이념");
            return ideology.detail().isBlank() ? null : ideology;
        }
        return null;
    }
    private void drawDetail(GuiGraphicsExtractor g) {
        int x = detailX(), w = detailWidth();
        g.nextStratum();
        HoiMenuStyle.panel(g, x, top, w, height - top - 34);
        g.text(font, trim(detail.name(), w - 48), x + 10, top + 9, TEXT);
        HoiMenuStyle.recess(g, x + 8, top + 31, w - 16, 38);
        UiAssets.draw(g, detail.icon().isEmpty() ? texture(detailIcon) : detail.icon(), x + 12, top + 36, 34, 26);
        g.text(font, trim(detail.value(), w - 64), x + 55, top + 44, TEXT);
        g.horizontalLine(x + 10, x + w - 10, top + 74, 0xFF4E4F52);
        var lines = font.split(Component.literal(detail.detail().isBlank() ? "추가 정보 없음" : detail.detail()), w - 28);
        int visible = Math.max(1, height - top - 128);
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - visible));
        g.enableScissor(x + 10, top + 84, x + w - 10, height - 44);
        int y = top + 84 - detailScroll;
        for (var line : lines) { g.text(font, line, x + 14, y, TEXT); y += 13; }
        g.disableScissor();
    }
    private void layoutManufacturers(boolean widgets, GuiGraphicsExtractor g) {
        int x = detailX(), w = detailWidth(), bottom = height - 34;
        var groups = List.of("all", "materiel", "armor", "air", "navy");
        var names = List.of("전체", "군수품", "전차", "비행기", "함선");
        var icons = List.of("menu/production", "politics/materiel_manufacturer", "politics/tank_manufacturer", "politics/aircraft_manufacturer", "politics/naval_manufacturer");
        var rows = view.manufacturers().stream().filter(m -> manufacturerGroup.equals("all") || m.groups().contains(manufacturerGroup)).toList();
        int available = Math.max(1, bottom - top - 62);
        manufacturerScroll = Math.clamp(manufacturerScroll, 0, Math.max(0, rows.size() * 49 - available));
        if (g != null) {
            g.nextStratum(); HoiMenuStyle.panel(g, x, top, w, bottom - top);
            g.text(font, "군수산업체", x + 10, top + 9, TEXT);
        }
        if (widgets) {
            addRenderableWidget(new HoiMenuButton("×", x + w - 25, top + 3, 19, 19, () -> { manufacturerGroup = null; rebuildWidgets(); }));
            for (int i = 0; i < groups.size(); i++) {
                final String group = groups.get(i);
                addRenderableWidget(new HoiMenuButton(names.get(i) + " 산업체", icons.get(i), manufacturerGroup.equals(group),
                        x + 9 + i * 30, top + 30, 27, 23, () -> { manufacturerGroup = group; manufacturerScroll = 0; rebuildWidgets(); }));
            }
        }
        if (g != null) g.enableScissor(x + 8, top + 59, x + w - 8, bottom - 4);
        int y = top + 59 - manufacturerScroll;
        for (var manufacturer : rows) {
            var entry = manufacturer.entry();
            if (g != null) {
                HoiMenuStyle.metal(g, x + 8, y, w - 16, 45);
                UiAssets.draw(g, entry.icon(), x + 12, y + 9, 25, 28);
                officerText(g, entry.name(), x + 43, y + 7, w - 55, TEXT);
                officerText(g, entry.value(), x + 43, y + 20, w - 116, GOLD);
            }
            if (widgets && y >= top + 59 && y + 45 <= bottom - 4) {
                addRenderableWidget(new HoiMenuButton("세부 사항", x + w - 70, y + 25, 56, 16, () -> {
                    detail = entry; detailIcon = "research_production"; manufacturerDetailId = manufacturer.id(); detailScroll = 0; rebuildWidgets();
                }));
            }
            y += 49;
        }
        if (g != null) {
            if (rows.isEmpty()) officerText(g, "등록된 군수산업체 없음", x + 14, top + 69, w - 28, MUTED);
            g.disableScissor();
        }
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (view != null && selected == MenuTab.POLITICS && detail == null && manufacturerGroup == null && politicsLayout().parties().contains(x, y)) {
            partyScroll = Math.clamp(partyScroll - (int)(vertical * 18), 0, Math.max(0, parties.size() * 9 - politicsLayout().parties().height() + 8));
            return true;
        }
        if (politicsHover(x, y) != null && (com.mojang.blaze3d.platform.InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT))) {
            tooltipScroll -= (int)(vertical * 3); return true;
        }
        if (view != null && selected == MenuTab.POLITICS && detail == null && manufacturerGroup == null
                && politicsLayout().spirits().contains(x, y)) {
            spiritScroll = Math.clamp(spiritScroll - (int)((horizontal != 0 ? horizontal : vertical) * spiritPitch()), 0, maximumSpiritScroll());
            return true;
        }
        if (x >= 40 && x < HoiMenuBar.statsRight(width) && y >= 0 && y < HoiMenuBar.STATS_HEIGHT) {
            hudScroll = HoiMenuBar.scroll(width, view == null ? dev.hoi.protocol.CountryHud.UNKNOWN : view.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        if (detail != null) { detailScroll -= (int)(vertical * 26); return true; }
        if (manufacturerGroup != null && x >= detailX()) { manufacturerScroll -= (int)(vertical * 35); rebuildWidgets(); return true; }
        if (x < pane && y >= contentTop()) { scroll -= (int)(vertical * 35); rebuildWidgets(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (detail != null && event.key() == GLFW.GLFW_KEY_ESCAPE) { detail = null; rebuildWidgets(); return true; }
        if (manufacturerGroup != null && event.key() == GLFW.GLFW_KEY_ESCAPE) { manufacturerGroup = null; rebuildWidgets(); return true; }
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        if (event.key() == GLFW.GLFW_KEY_PAGE_DOWN || event.key() == GLFW.GLFW_KEY_PAGE_UP) {
            int amount = (event.key() == GLFW.GLFW_KEY_PAGE_DOWN ? 1 : -1) * 100;
            if (detail != null) detailScroll += amount; else { scroll += amount; rebuildWidgets(); } return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        return super.keyReleased(event);
    }
    @Override public void removed() { SidebarMovement.release(minecraft); HoiClient.cancelOpen(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private String trim(String text, int max) { return font.width(text) <= max ? text : font.plainSubstrByWidth(text, Math.max(1, max - 9)) + "…"; }
    private String texture(String icon) {
        return switch (icon) {
            case "politics" -> "country/" + view.country().toLowerCase(Locale.ROOT) + "/flag";
            case "civilian" -> "menu/trade";
            case "army" -> "category/infantry";
            case "navy" -> "category/navy";
            case "air" -> "category/air";
            case "special" -> "category/support";
            case "commander" -> "agency/commander";
            default -> "menu/" + icon;
        };
    }
    private final class InvisibleButton extends Button {
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) {
            UiSounds.play(getMessage().getString().equals("국가 중점") ? "ui.focus.select" : "ui.click");
        }
        InvisibleButton(String label, int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.literal(label)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {

        }
    }
}
