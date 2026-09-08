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
    private int pane, top, scroll, total, detailScroll, tradeTab, countryTab, hudScroll;
    private static final String[] COUNTRY_TABS = {"국가", "민간", "육군", "해군", "공군"};
    private static final String[] COUNTRY_ICONS = {"politics", "civilian", "army", "navy", "air"};

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
        if (detail != null) {
            var old = detail;
            detail = next.page(selected).sections().stream().filter(s -> s.icon().equals(detailIcon))
                    .flatMap(s -> s.entries().stream()).filter(e -> e.name().equals(old.name())).findFirst().orElse(null);
        }
        view = next; rebuildWidgets();
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
            for (int i = 0; i < COUNTRY_TABS.length; i++) {
                final int next = i;
                addRenderableWidget(new HoiMenuButton(COUNTRY_TABS[i], texture(COUNTRY_ICONS[i]), countryTab == i,
                        8 + i * (pane - 16) / 5, countryTabsY(), (pane - 20) / 5, countryTabsHeight(),
                        () -> { countryTab = next; collapsed.clear(); scroll = 0; rebuildWidgets(); }));
            }
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
    private int countryTabsY() { return top + (height >= 360 ? 103 : 80); }
    private int countryTabsHeight() { return height >= 360 ? 29 : 21; }
    private int contentTop() { return selected == MenuTab.POLITICS ? countryTabsY() + countryTabsHeight() + 8
            : top + (selected == MenuTab.TRADE ? 62 : selected == MenuTab.INTELLIGENCE ? 82 : 32); }
    private boolean hasFooterActions() { return selected == MenuTab.RESEARCH || selected == MenuTab.INTELLIGENCE; }
    private int contentBottom() { return height - (hasFooterActions() ? 36 : 8); }
    private int detailX() { return width - pane >= 240 ? pane + 6 : Math.max(8, (width - detailWidth()) / 2); }
    private int detailWidth() { return Math.min(310, width - (width - pane >= 240 ? pane + 14 : 16)); }
    private int rowHeight() { return selected == MenuTab.PRODUCTION || selected == MenuTab.RECRUITMENT ? 49 : 37; }
    private void layoutRows(boolean widgets, GuiGraphicsExtractor g) {
        int y = contentTop() - scroll;
        var sections = view.page(selected).sections();
        for (int index = 0; index < sections.size(); index++) {
            if (selected == MenuTab.TRADE && (tradeTab == 0 ? index != 0 : index == 0)) continue;
            var section = sections.get(index); final int sectionIndex = index;
            if (selected == MenuTab.POLITICS && !(countryTab == 0
                    ? section.icon().equals("politics") || section.icon().equals("research")
                    : section.icon().equals(COUNTRY_ICONS[countryTab]))) continue;
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
    private void requestRefresh() {
        if (ClientPlayNetworking.canSend(MenuProtocol.Refresh.TYPE)) ClientPlayNetworking.send(new MenuProtocol.Refresh(token));
    }
    @Override public void tick() {
        if (refreshTicks++ % 40 == 0) requestRefresh();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        HoiMenuBar.draw(g, width, view == null ? dev.hoi.protocol.CountryHud.UNKNOWN : view.hud(), mx, my, hudScroll);
        HoiMenuStyle.panel(g, 0, top, pane, height - top);
        g.text(font, trim(selected.label(), pane - 44), 10, top + 9, TEXT);
        if (view == null) {
            g.text(font, trim("서버 정보 불러오는 중…", pane - 20), 10, top + 40, MUTED);
            super.extractRenderState(g, mx, my, delta);
            return;
        }
        if (selected == MenuTab.POLITICS) {
            int bannerHeight = countryTabsY() - top - 36;
            HoiMenuStyle.recess(g, 8, top + 30, pane - 16, bannerHeight);
            int flagWidth = Math.min(62, pane / 4), flagHeight = Math.min(48, bannerHeight - 10);
            int flagY = top + 30 + (bannerHeight - flagHeight) / 2;
            HoiMenuStyle.metal(g, 13, flagY, flagWidth, flagHeight);
            UiAssets.draw(g, "country/" + view.country().toLowerCase(Locale.ROOT) + "/flag", 17, flagY + 4, flagWidth - 8, flagHeight - 8);
            int textX = flagWidth + 23, textY = top + 30 + (bannerHeight - 26) / 2;
            g.text(font, trim(view.countryName(), pane - textX - 16), textX, textY, TEXT);
            g.horizontalLine(textX, pane - 18, textY + 12, 0xFF46474A);
            g.text(font, trim(view.country(), pane - textX - 16), textX, textY + 18, MUTED);
        }
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
        UiAssets.draw(g, texture(detailIcon), x + 12, top + 36, 34, 26);
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
