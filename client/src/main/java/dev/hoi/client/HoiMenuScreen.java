package dev.hoi.client;

import dev.hoi.protocol.MenuTab;
import dev.hoi.protocol.MenuView;
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

/** TFR toolbar and private country side panels with separate authenticated research/agency workflows. */
public final class HoiMenuScreen extends Screen implements SidebarMovement.Screen {
    private static final int TEXT = HoiMenuStyle.TEXT, GOLD = HoiMenuStyle.ACCENT, MUTED = HoiMenuStyle.MUTED;
    private MenuView view;
    private final Runnable researchOpen;
    private final String token = UUID.randomUUID().toString();
    private int refreshTicks;
    private MenuTab selected = MenuTab.POLITICS;
    private final Set<Integer> collapsed = new HashSet<>();
    private MenuView.Entry detail;
    private String detailIcon;
    private int pane, top, scroll, total, detailScroll, tradeTab, hudScroll;
    private static final String[] POLITICS_IDS = {"government", "economic_laws", "military_laws", "social_laws", "development", "military_staff", "research_production"};
    private static final String[] POLITICS_NAMES = {"정부", "경제법", "군법", "사회법", "발전도", "군사 참모", "연구 & 생산"};

    public HoiMenuScreen(MenuView view) { this(view, MenuTab.POLITICS, HoiClient::open); }
    HoiMenuScreen(MenuTab selected) { this(null, selected, HoiClient::open); }
    HoiMenuScreen(MenuView view, MenuTab selected, Runnable researchOpen) {
        super(Component.literal("HOI · 국가 메뉴"));
        this.view = view; this.selected = selected; this.researchOpen = researchOpen;
    }
    String token() { return token; }
    public void update(MenuView next) {
        if (next.equals(view)) return;
        if (view != null && !view.country().equals(next.country())) { selected = MenuTab.POLITICS; collapsed.clear(); }
        var old = detail;
        boolean sameCountry = view != null && view.country().equals(next.country());
        view = next;
        if (old != null) {
            if (!sameCountry) detail = null;
            else if (old.value().equals("국가 현황")) detail = politicsOverview();
            else if (old.name().equals("국가 중점")) detail = politicsFocus();
            else if (old.name().equals("국가 정신")) detail = politicsSpirits();
            else detail = next.page(selected).sections().stream().filter(s -> s.icon().equals(detailIcon))
                    .flatMap(s -> s.entries().stream()).filter(e -> e.name().equals(old.name())).findFirst().orElse(null);
        }
        rebuildWidgets();
    }
    MenuTab selectedTab() { return selected; }
    int panelWidth() { return pane; }
    @Override public boolean allowsMovement() { return detail == null; }
    @Override protected void init() {
        pane = HoiPanelLayout.width(selected, width);
        top = HoiMenuBar.height(width);
        HoiMenuBar.buttons(width, view == null ? "" : view.country(), selected, tab -> {
            if (tab == MenuTab.RESEARCH) { researchOpen.run(); return; }
            if (tab == MenuTab.CONSTRUCTION && ClientPlayNetworking.canSend(dev.hoi.protocol.ConstructionProtocol.Request.TYPE)) { HoiClient.openConstruction(); return; }
            HoiClient.cancelOpen();
            selected = tab; scroll = 0; collapsed.clear(); detail = null; rebuildWidgets();
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new HoiMenuButton("×", pane - 25, top + 3, 19, 19, this::onClose));
        if (view == null) return;
        if (selected == MenuTab.POLITICS) {
            addRenderableWidget(new InvisibleButton("국가 현황", 8, top + 29, pane - 44, 32,
                    () -> showDetail(politicsOverview(), "politics")));
            addRenderableWidget(new HoiMenuButton("외교", "menu/trade", false, pane - 33, top + 30, 25, 28, () -> HoiClient.openCountry("")));
            addRenderableWidget(new InvisibleButton("국가 중점", 8, top + 64, pane - 16, 24,
                    () -> showDetail(politicsFocus(), "research")));
            addRenderableWidget(new InvisibleButton("국가 정신", 8, top + 91, pane - 16, 19,
                    () -> showDetail(politicsSpirits(), "politics")));
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
        if (detail != null) {
            SidebarMovement.release(minecraft);
            int x = detailX(), w = detailWidth();
            addRenderableWidget(new HoiMenuButton("×", x + w - 25, top + 3, 19, 19, () -> { detail = null; rebuildWidgets(); }));
        }
    }
    private int contentTop() { return selected == MenuTab.POLITICS ? top + 116
            : top + (selected == MenuTab.TRADE ? 62 : selected == MenuTab.INTELLIGENCE ? 82 : 32); }
    private boolean hasFooterActions() { return selected == MenuTab.RESEARCH || selected == MenuTab.INTELLIGENCE; }
    private int contentBottom() { return height - (hasFooterActions() ? 36 : 8); }
    private int detailX() { return width - pane >= 240 ? pane + 6 : Math.max(8, (width - detailWidth()) / 2); }
    private int detailWidth() { return Math.min(310, width - (width - pane >= 240 ? pane + 14 : 16)); }
    private int rowHeight() { return selected == MenuTab.PRODUCTION || selected == MenuTab.RECRUITMENT ? 49 : 37; }
    private void layoutRows(boolean widgets, GuiGraphicsExtractor g) {
        if (selected == MenuTab.POLITICS) { layoutPolitics(widgets, g); return; }
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
    private void showDetail(MenuView.Entry entry, String icon) {
        detail = entry; detailIcon = icon; detailScroll = 0; rebuildWidgets();
    }
    private MenuView.Entry politicsOverview() {
        String text = view.page(MenuTab.POLITICS).sections().stream()
                .filter(s -> !Arrays.asList(POLITICS_IDS).contains(s.icon()))
                .map(s -> s.title() + "\n" + s.entries().stream().map(e -> e.name() + " · " + e.value()).collect(java.util.stream.Collectors.joining("\n")))
                .collect(java.util.stream.Collectors.joining("\n\n"));
        return new MenuView.Entry(view.countryName(), "국가 현황", fontSafe(text));
    }
    private String fontSafe(String text) { return text.length() <= 2000 ? text : text.substring(0, 1999) + "…"; }
    private MenuView.Entry politicsFocus() {
        var entries = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 중점")).flatMap(s -> s.entries().stream()).toList();
        String label = entries.stream().filter(e -> e.progress() >= 0 || e.value().contains("진행")).map(MenuView.Entry::name).findFirst().orElse("선택된 중점 없음");
        return new MenuView.Entry("국가 중점", label, fontSafe(entries.stream().map(e -> e.name() + " · " + e.value() + "\n" + e.detail()).collect(java.util.stream.Collectors.joining("\n\n"))));
    }
    private MenuView.Entry politicsSpirits() {
        String text = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 정신"))
                .flatMap(s -> s.entries().stream()).map(e -> e.name() + " · " + e.value()).collect(java.util.stream.Collectors.joining("\n"));
        return new MenuView.Entry("국가 정신", text.isEmpty() ? "없음" : "현재 국가 정신", fontSafe(text));
    }
    private void drawPoliticsBanner(GuiGraphicsExtractor g) {
        HoiMenuStyle.recess(g, 7, top + 29, pane - 14, 83);
        UiAssets.draw(g, "country/" + view.country().toLowerCase(Locale.ROOT) + "/flag", 12, top + 33, 40, 24);
        g.text(font, trim(view.countryName(), pane - 97), 58, top + 34, TEXT);
        g.text(font, trim(view.page(MenuTab.POLITICS).sections().stream().flatMap(s -> s.entries().stream())
                .filter(e -> e.name().equals("집권 정당")).map(MenuView.Entry::value).findFirst().orElse(view.country()), pane - 97), 58, top + 48, MUTED);
        HoiMenuStyle.control(g, 9, top + 64, pane - 18, 24, false, false);
        UiAssets.draw(g, "menu/research", 13, top + 66, 23, 20);
        g.text(font, trim(politicsFocus().value(), pane - 57), 43, top + 72, TEXT);
        UiAssets.draw(g, "menu/politics", 13, top + 93, 17, 15);
        g.text(font, "국가 정신", 35, top + 96, MUTED);
        String spirits = politicsSpirits().detail().replace('\n', ' ');
        g.text(font, trim(spirits.isBlank() ? "없음" : spirits, pane - 108), 94, top + 96, TEXT);
    }
    private void layoutPolitics(boolean widgets, GuiGraphicsExtractor g) {
        int row = Math.clamp((contentBottom() - contentTop()) / 7, 48, 68);
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
                    var button = new InvisibleButton(entry.name() + " · " + entry.value(), x, cy, cell - 4, iconSize, () -> showDetail(entry, section.icon()));
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
        if (detail != null) drawDetail(g);
        super.extractRenderState(g, mx, my, delta);
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
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= 40 && x < HoiMenuBar.statsRight(width) && y >= 0 && y < HoiMenuBar.STATS_HEIGHT) {
            hudScroll = HoiMenuBar.scroll(width, view == null ? dev.hoi.protocol.CountryHud.UNKNOWN : view.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        if (detail != null) { detailScroll -= (int)(vertical * 26); return true; }
        if (x < pane && y >= contentTop()) { scroll -= (int)(vertical * 35); rebuildWidgets(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (detail != null && event.key() == GLFW.GLFW_KEY_ESCAPE) { detail = null; rebuildWidgets(); return true; }
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
        InvisibleButton(String label, int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.literal(label)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (active && isHoveredOrFocused()) g.outline(getX(), getY(), getWidth(), getHeight(), GOLD);
        }
    }
}
