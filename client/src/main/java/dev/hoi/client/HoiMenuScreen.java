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
    private static final int TEXT = 0xFFE0E3DD, GOLD = 0xFFE6C779, MUTED = 0xFF9AABA8;
    private MenuView view;
    private final Runnable researchOpen;
    private final String token = UUID.randomUUID().toString();
    private int refreshTicks;
    private MenuTab selected = MenuTab.POLITICS;
    private final Set<Integer> collapsed = new HashSet<>();
    private MenuView.Entry detail;
    private String detailIcon;
    private int pane, top, scroll, total, detailScroll, refreshCooldown, tradeTab, countryTab;
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
        pane = selected == MenuTab.POLITICS || selected == MenuTab.RECRUITMENT ? width * 4 / 10
                : selected == MenuTab.TRADE ? width * 35 / 100 : width * 3 / 10;
        top = HoiMenuBar.height(width);
        HoiMenuBar.buttons(width, view == null ? "" : view.country(), selected, tab -> {
            if (tab == MenuTab.RESEARCH) { researchOpen.run(); return; }
            HoiClient.cancelOpen();
            selected = tab; scroll = 0; collapsed.clear(); detail = null; rebuildWidgets();
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new ResearchButton("×", pane - 25, top + 3, 19, 19, this::onClose));
        if (view == null) return;
        if (selected == MenuTab.POLITICS) {
            for (int i = 0; i < COUNTRY_TABS.length; i++) {
                final int next = i;
                addRenderableWidget(new ResearchButton((countryTab == i ? "• " : "") + COUNTRY_TABS[i],
                        8 + i * (pane - 16) / 5, top + 80, (pane - 20) / 5, 21,
                        () -> { countryTab = next; collapsed.clear(); scroll = 0; rebuildWidgets(); }));
            }
        }
        if (selected == MenuTab.TRADE) {
            addRenderableWidget(new ResearchButton((tradeTab == 0 ? "• " : "") + "경제", 8, top + 30, (pane - 20) / 2, 20,
                    () -> { tradeTab = 0; scroll = 0; rebuildWidgets(); }));
            addRenderableWidget(new ResearchButton((tradeTab == 1 ? "• " : "") + "무역", pane / 2, top + 30, (pane - 20) / 2, 20,
                    () -> { tradeTab = 1; scroll = 0; rebuildWidgets(); }));
        }
        layoutRows(false, null);
        scroll = Math.clamp(scroll, 0, Math.max(0, total - (height - 38 - contentTop())));
        layoutRows(true, null);
        addRenderableWidget(new ResearchButton("새로고침", 8, height - 28, 76, 20, () -> {
            if (refreshCooldown == 0 && minecraft.getConnection() != null) {
                refreshCooldown = 20; requestRefresh();
            }
        }));
        if (selected == MenuTab.RESEARCH) addRenderableWidget(new ResearchButton("연구 선택", 90, height - 28, 88, 20, researchOpen));
        if (selected == MenuTab.INTELLIGENCE) addRenderableWidget(new ResearchButton("기관 관리", 90, height - 28, 80, 20, () -> {
            if (ClientPlayNetworking.canSend(dev.hoi.protocol.AgencyProtocol.Request.TYPE)) {
                var screen = new AgencyScreen(this); minecraft.gui.setScreen(screen); screen.open();
            }
        }));
        if (detail != null) {
            SidebarMovement.release(minecraft);
            int x = detailX(), w = detailWidth();
            addRenderableWidget(new ResearchButton("×", x + w - 26, top + 5, 20, 20, () -> { detail = null; rebuildWidgets(); }));
        }
    }
    private int contentTop() { return top + (selected == MenuTab.POLITICS ? 109 : selected == MenuTab.TRADE ? 58 : selected == MenuTab.INTELLIGENCE ? 82 : 32); }
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
                int color = switch (section.icon()) { case "army" -> 0xFF465F35; case "navy" -> 0xFF344B72; case "air" -> 0xFF69353D; case "special" -> 0xFF756638; default -> 0xFF343F47; };
                g.fillGradient(8, y, pane - 8, y + 21, color, 0xFF15191D);
                g.outline(8, y, pane - 16, 21, 0xFF59666A);
                g.text(font, section.title(), 17, y + 6, TEXT);
                g.text(font, collapsed.contains(index) ? "+" : "−", pane - 25, y + 6, GOLD);
            }
            if (widgets && y >= contentTop() && y + 21 <= height - 36) {
                var header = new InvisibleButton(section.title(), 8, y, pane - 16, 21, () -> {
                    if (!collapsed.add(sectionIndex)) collapsed.remove(sectionIndex); rebuildWidgets();
                });
                header.active = detail == null; addRenderableWidget(header);
            }
            y += 25;
            if (!collapsed.contains(index)) for (var entry : section.entries()) {
                int h = rowHeight();
                if (g != null) {
                    g.fillGradient(10, y, pane - 10, y + h - 3, 0xFF292F32, 0xFF0D1114);
                    g.outline(10, y, pane - 20, h - 3, 0xFF47524D);
                    String icon = section.icon();
                    String texture = texture(icon);
                    UiAssets.draw(g, texture, 15, y + 5, 28, h - 13);
                    g.text(font, trim(entry.name(), pane - 65), 50, y + 5, TEXT);
                    g.text(font, trim(entry.value(), pane - 65), 50, y + 19, GOLD);
                    if (entry.progress() >= 0) {
                        g.fill(50, y + h - 8, pane - 17, y + h - 5, 0xFF151E14);
                        g.fill(50, y + h - 8, 50 + (int)((pane - 67) * entry.progress()), y + h - 5, 0xFF82A467);
                    }
                }
                if (widgets && y >= contentTop() && y + h <= height - 36) {
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
        if (refreshCooldown > 0) refreshCooldown--;
        if (refreshTicks++ % 40 == 0) requestRefresh();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        HoiMenuBar.draw(g, width);
        g.fillGradient(0, top, pane, height, 0xFF23272C, 0xFF0E1114);
        g.outline(0, top, pane, height - top, 0xFF657078);
        g.text(font, selected.label(), 10, top + 8, TEXT);
        g.horizontalLine(8, pane - 8, top + 25, 0xFF657078);
        if (view == null) {
            g.text(font, "서버 정보 불러오는 중…", 10, top + 40, MUTED);
            super.extractRenderState(g, mx, my, delta);
            return;
        }
        if (selected == MenuTab.POLITICS) {
            g.fillGradient(9, top + 30, pane - 9, top + 75, 0xFF353D45, 0xFF14181D);
            UiAssets.draw(g, "country/" + view.country().toLowerCase(Locale.ROOT) + "/flag", 16, top + 38, 51, 30);
            g.text(font, trim(view.countryName(), pane - 86), 77, top + 39, TEXT);
            g.text(font, "자국 정보 · " + view.country(), 77, top + 57, MUTED);
        }
        if (selected == MenuTab.INTELLIGENCE) {
            g.fillGradient(9, top + 30, pane - 9, top + 76, 0xFF383B43, 0xFF131820);
            if (!view.country().equals("KOR") || !UiAssets.draw(g, "country/kor/intelligence", 18, top + 33, 42, 40))
                UiAssets.draw(g, "menu/intelligence", 18, top + 33, 42, 40);
            g.text(font, "정보기관", 76, top + 43, TEXT);
            g.text(font, view.countryName(), 76, top + 58, MUTED);
        }
        g.enableScissor(6, contentTop(), pane - 6, height - 36); layoutRows(false, g); g.disableScissor();
        int visible = height - 38 - contentTop();
        if (total > visible && visible > 0) {
            int thumb = Math.max(10, visible * visible / total);
            int y = contentTop() + scroll * (visible - thumb) / Math.max(1, total - visible);
            g.fill(pane - 6, y, pane - 3, y + thumb, MUTED);
        }
        g.text(font, "서버 현황", pane - 66, height - 22, MUTED);
        if (detail != null) drawDetail(g);
        super.extractRenderState(g, mx, my, delta);
    }
    private void drawDetail(GuiGraphicsExtractor g) {
        int x = detailX(), w = detailWidth();
        g.nextStratum();
        g.fillGradient(x, top, x + w, height - 34, 0xFF353C44, 0xFF101418);
        g.outline(x, top, w, height - top - 34, 0xFF79858A);
        g.text(font, trim(detail.name(), w - 48), x + 10, top + 12, GOLD);
        UiAssets.draw(g, texture(detailIcon), x + 12, top + 36, 34, 26);
        g.text(font, trim(detail.value(), w - 64), x + 55, top + 44, TEXT);
        g.horizontalLine(x + 10, x + w - 10, top + 72, 0xFF657078);
        var lines = font.split(Component.literal(detail.detail().isBlank() ? "추가 정보 없음" : detail.detail()), w - 28);
        int visible = Math.max(1, height - top - 128);
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - visible));
        g.enableScissor(x + 10, top + 84, x + w - 10, height - 44);
        int y = top + 84 - detailScroll;
        for (var line : lines) { g.text(font, line, x + 14, y, TEXT); y += 13; }
        g.disableScissor();
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
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
