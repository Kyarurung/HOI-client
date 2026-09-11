package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.HoiMenuButton;
import dev.hoi.client.ui.HoiMenuStyle;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import org.lwjgl.glfw.GLFW;


public final class CountryScreen extends Screen implements SidebarMovement.Screen {
    private CountryView view;
    private DiplomacyHeader header;
    private String target, token = UUID.randomUUID().toString();
    private IntelDomain domain = IntelDomain.CIVILIAN;
    private boolean ledger, choosing, pending;
    private CountryView.Action confirmation;
    private int pane, top, scroll, total, ticks, hudScroll;
    private MenuView.Entry detail;
    private int detailScroll;
    public CountryScreen(String target) { super(Component.literal("외교")); this.target = target; choosing = target.isEmpty(); }
    public CountryScreen(CountryView fixture) { this(fixture.target()); view = fixture; }
    String token() { return token; }
    CountryView view() { return view; }
    int panelWidth() { return pane; }
    void select(String tag) {
        target = tag; view = null; token = UUID.randomUUID().toString(); scroll = 0; detail = null; confirmation = null; pending = false; choosing = false;
        ticks = 0; rebuildWidgets();
    }
    public void update(CountryProtocol.Response packet) {
        if (!token.equals(packet.token())) return;
        if (packet.json().isEmpty()) { view = null; detail = null; minecraft.gui.setScreen(null); return; }
        var next = packet.view();
        if (!target.isEmpty() && !next.target().equals(target)) return;
        if (next.equals(view)) return;
        pending = false; confirmation = null;
        var oldDetail = detail;
        boolean sameCountry = view != null && view.viewer().equals(next.viewer()) && view.target().equals(next.target());
        view = next; target = next.target();
        detail = sameCountry && oldDetail != null ? entries().stream().filter(e -> e.name().equals(oldDetail.name())).findFirst().orElse(null) : null;
        rebuildWidgets();
    }
    @Override protected void init() {
        pane = Math.min(width - 20, Math.max(210, width * 550 / 2560)); top = HoiMenuBar.height(width);
        HoiMenuBar.buttons(width, view == null ? "" : view.viewer(), null, tab -> {
            if (tab == MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new HoiMenuButton("×", pane - 25, top + 3, 19, 19, this::onClose));
        if (view == null) return;
        header = new DiplomacyHeader(view, pane);
        int half = (pane - 18) / 2;
        var dip = new HoiMenuButton("외교", 7, top + DiplomacyHeader.scaled(120, pane), half, 20, () -> { ledger = false; choosing = false; scroll = 0; detail = null; rebuildWidgets(); });
        var intel = new HoiMenuButton("첩보 장부", 11 + half, top + DiplomacyHeader.scaled(120, pane), half, 20, () -> { ledger = true; choosing = false; scroll = 0; detail = null; rebuildWidgets(); });
        addRenderableWidget(dip); addRenderableWidget(intel);
        if (ledger && !choosing) {
            int cell = (pane - 16) / 4;
            for (var d : IntelDomain.values()) {
                var report = view.report(d);
                var button = new HoiMenuButton(d.label + " 정보", d.icon, domain == d, 8 + d.ordinal() * cell, top + 95, cell - 2, 30,
                        () -> { domain = d; scroll = 0; detail = null; rebuildWidgets(); });
                String tips = "총 " + d.label + " 정보: " + String.format(Locale.ROOT, "%.1f%%", report.percent())
                        + (view.shared() ? "\n동맹 정보 공유" : "") + "\n\n" + d.tiers.stream()
                        .map(t -> (IntelDomain.permits(report.percent(), view.shared(), t.percent()) ? "✓ " : "× ") + t.percent() + "% · " + t.description())
                        .collect(java.util.stream.Collectors.joining("\n"));
                button.setTooltip(Tooltip.create(Component.literal(tips))); addRenderableWidget(button);
            }
        }
        addRenderableWidget(new HoiMenuButton("국가 목록 열기", 8, height - 26, pane - 16, 19, () -> { choosing = !choosing; scroll = 0; detail = null; rebuildWidgets(); }));
        int count = choosing ? view.countries().size() : entries().size(); total = count * 39;
        int rowsWidth = actionsVisible() ? pane / 2 - 12 : pane - 22;
        scroll = Math.clamp(scroll, 0, Math.max(0, total - (height - 33 - bodyTop())));
        for (int i = 0; i < count; i++) {
            int y = bodyTop() + i * 39 - scroll;
            if (y < bodyTop() || y + 36 > height - 33) continue;
            final int index = i;
            String label = choosing ? view.countries().get(i).name() : entries().get(i).name() + " · " + entries().get(i).value();
            var button = new Row(label, 9, y, rowsWidth, 36, () -> {
                if (choosing) select(view.countries().get(index).tag());
                else { detail = entries().get(index); detailScroll = 0; rebuildWidgets(); }
            });
            button.active = detail == null && confirmation == null; addRenderableWidget(button);
        }
        if (actionsVisible()) {
            int y = bodyTop();
            for (var action : view.actions().entries()) {
                var button = new DiplomacyActionButton(action.name(), pane / 2 + 3, y, pane / 2 - 12, Math.max(15, DiplomacyHeader.scaled(28, pane)),
                        () -> { confirmation = action; rebuildWidgets(); });
                button.active = action.enabled() && !pending && confirmation == null && detail == null;
                button.setTooltip(Tooltip.create(Component.literal(action.name() + "\n" + action.detail())));
                addRenderableWidget(button); y += Math.max(16, DiplomacyHeader.scaled(29, pane));
            }
        }
        if (confirmation != null) {
            int x = detailX(), w = detailWidth();
            addRenderableWidget(new HoiMenuButton("취소", x + 10, top + 175, (w - 25) / 2, 22,
                    () -> { confirmation = null; rebuildWidgets(); }));
            var confirm = new HoiMenuButton("확인", x + 15 + (w - 25) / 2, top + 175, (w - 25) / 2, 22, this::sendAction);
            confirm.active = !pending;
            addRenderableWidget(confirm);
        }
        if (detail != null) addRenderableWidget(new HoiMenuButton("닫기", detailX() + detailWidth() - 43, top + 4, 36, 18, () -> { detail = null; rebuildWidgets(); }));
    }
    private boolean actionsVisible() { return view != null && !ledger && !choosing && view.actions() != null && !view.actions().entries().isEmpty(); }
    private void sendAction() {
        if (pending || confirmation == null || view == null || view.actions() == null || !ClientPlayNetworking.canSend(CountryProtocol.Action.TYPE)) return;
        var action = view.actions();
        pending = true; ticks = 1;
        ClientPlayNetworking.send(new CountryProtocol.Action(token, action.session(), action.revision(), confirmation.id()));
        rebuildWidgets();
    }
    private List<MenuView.Entry> entries() {
        if (ledger) return view.report(domain).entries();
        return view.diplomacy().stream().filter(e -> !DiplomacyHeader.summary(e)).toList();
    }
    private int bodyTop() { return choosing ? top + 97 : ledger ? top + 143 : DiplomacyHeader.body(top, pane); }
    private int detailX() { return width - pane >= 200 ? pane + 5 : 5; }
    private int detailWidth() { return Math.min(330, width - detailX() - 6); }
    @Override public void tick() {
        if (!pending && confirmation == null && ticks++ % 40 == 0 && ClientPlayNetworking.canSend(CountryProtocol.Request.TYPE)) {
            token = UUID.randomUUID().toString(); ClientPlayNetworking.send(new CountryProtocol.Request(token, target));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        HoiMenuBar.draw(g, width, view == null ? CountryHud.UNKNOWN : view.hud(), mx, my, hudScroll);
        HoiMenuStyle.panel(g, 0, top, pane, height - top);
        HoiMenuStyle.heading(g, font, "외교", 10, top + 9, pane - 44, HoiMenuStyle.TEXT);
        if (view == null) dev.hoi.client.ui.UiText.text(g, font, "서버 정보 불러오는 중…", 12, top + 38, HoiMenuStyle.MUTED);
        else {
            UiAssets.nineSlice(g, "diplomacy/header", 7, top + 30, pane - 14, 38, 5, pane / 550.0);
            if (!UiAssets.draw(g, "country/" + view.target().toLowerCase(Locale.ROOT) + "/flag", 11, top + 34, 39, 29))
                dev.hoi.client.ui.UiText.text(g, font, view.target(), 12, top + 43, HoiMenuStyle.TEXT);
            dev.hoi.client.ui.UiText.text(g, font, trim(view.name(), pane - 69), 58, top + 37, HoiMenuStyle.TEXT);
            dev.hoi.client.ui.UiText.text(g, font, view.viewer().equals(view.target()) ? "통제 국가" : view.shared() ? "동맹 정보 공유" : "외국 정보", 58, top + 52, HoiMenuStyle.MUTED);
            if (ledger && !choosing) for (var d : IntelDomain.values()) {
                int cell = (pane - 16) / 4, x = 8 + d.ordinal() * cell;
                dev.hoi.client.ui.UiText.text(g, font, (int)view.report(d).percent() + "%", x + 10, top + 129, HoiMenuStyle.TEXT);
            }
            if (!ledger && !choosing) header.draw(g, font, view, pane, top);
            HoiMenuStyle.recess(g, 5, bodyTop() - 3, pane - 10, Math.max(6, height - 30 - bodyTop()));
            g.enableScissor(6, bodyTop(), pane - 6, height - 33);
            int count = choosing ? view.countries().size() : entries().size();
            int rowsWidth = actionsVisible() ? pane / 2 - 12 : pane - 22;
            for (int i = 0; i < count; i++) {
                int y = bodyTop() + i * 39 - scroll;
                if (y + 36 < bodyTop() || y > height - 33) continue;
                if (!ledger && !choosing) UiAssets.nineSlice(g, "diplomacy/relations", 9, y, rowsWidth, 36, 4, pane / 550.0);
                else HoiMenuStyle.control(g, 9, y, rowsWidth, 36, false, false);
                if (!choosing && entries().get(i).name().equals("정보가 충분하지 않습니다"))
                    UiAssets.draw(g, "intel/no_intel", 9, y, rowsWidth, 36);
                String icon = choosing ? "country/" + view.countries().get(i).tag().toLowerCase(Locale.ROOT) + "/flag" : ledger ? domain.icon : "country/" + view.target().toLowerCase(Locale.ROOT) + "/flag";
                UiAssets.draw(g, icon, 12, y + 5, 27, 25);
                String name = choosing ? view.countries().get(i).name() : entries().get(i).name();
                String value = choosing ? view.countries().get(i).tag() : entries().get(i).value();
                dev.hoi.client.ui.UiText.text(g, font, trim(name, rowsWidth - 42), 46, y + 6, HoiMenuStyle.TEXT);
                dev.hoi.client.ui.UiText.text(g, font, trim(value, rowsWidth - 42), 46, y + 21, HoiMenuStyle.MUTED);
            }
            g.disableScissor();
            if (view.actions() != null && !view.actions().message().isBlank()) {
                var lines = font.split(Component.literal(view.actions().message()), pane - 24);
                int messageY = height - 42 - Math.min(3, lines.size()) * 12;
                for (var line : lines.stream().limit(3).toList()) { dev.hoi.client.ui.UiText.text(g, font, line, 12, messageY, 0xFFFFBB22); messageY += 12; }
            }
            if (confirmation != null) {
                int x = detailX(), w = detailWidth();
                g.nextStratum(); HoiMenuStyle.panel(g, x, top, w, 207);
                HoiMenuStyle.heading(g, font, confirmation.name(), x + 10, top + 9, w - 20, HoiMenuStyle.TEXT);
                int y = top + 38;
                for (var line : font.split(Component.literal(view.name() + "\n\n" + confirmation.detail()), w - 24)) {
                    dev.hoi.client.ui.UiText.text(g, font, line, x + 12, y, HoiMenuStyle.TEXT); y += 13;
                }
            }
            if (detail != null) {
                int x = detailX(), w = detailWidth();
                g.nextStratum(); HoiMenuStyle.panel(g, x, top, w, height - top - 31);
                HoiMenuStyle.heading(g, font, trim(detail.name(), w - 55), x + 9, top + 9, w - 44, HoiMenuStyle.TEXT);
                var lines = font.split(Component.literal(detail.value() + "\n\n" + (detail.detail().isEmpty() ? "추가 정보 없음" : detail.detail())), w - 22);
                detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - (height - top - 78)));
                g.enableScissor(x + 7, top + 32, x + w - 7, height - 37);
                int y = top + 34 - detailScroll;
                for (var line : lines) { dev.hoi.client.ui.UiText.text(g, font, line, x + 11, y, HoiMenuStyle.TEXT); y += 13; }
                g.disableScissor();
            }
        }
        super.extractRenderState(g, mx, my, delta);
    }
    private String trim(String text, int w) { return font.width(text) <= w ? text : font.plainSubstrByWidth(text, Math.max(1, w - 9)) + "…"; }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (y < HoiMenuBar.STATS_HEIGHT && x >= 40 && x < HoiMenuBar.statsRight(width)) {
            hudScroll = HoiMenuBar.scroll(width, view == null ? CountryHud.UNKNOWN : view.hud(), hudScroll, horizontal, vertical); return true;
        }
        if (detail != null) { detailScroll -= (int)(vertical * 26); return true; }
        if (x < pane && y >= bodyTop()) { scroll -= (int)(vertical * 39); rebuildWidgets(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (confirmation != null && event.key() == GLFW.GLFW_KEY_ESCAPE && !pending) { confirmation = null; rebuildWidgets(); return true; }
        if (detail != null && event.key() == GLFW.GLFW_KEY_ESCAPE) { detail = null; rebuildWidgets(); return true; }
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) { return allowsMovement() && SidebarMovement.consumes(minecraft, event) || super.keyReleased(event); }
    @Override public boolean allowsMovement() { return detail == null && confirmation == null; }
    @Override public void removed() { SidebarMovement.release(minecraft); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private final class DiplomacyActionButton extends Button {
        DiplomacyActionButton(String label, int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            UiAssets.nineSlice(g, "diplomacy/" + (active ? "action" : "action_disabled"), getX(), getY(), getWidth(), getHeight(), 3, pane / 550.0);
            dev.hoi.client.ui.UiText.text(g, font, font.plainSubstrByWidth(getMessage().getString(), Math.max(1, (int)((getWidth() - 9) / dev.hoi.client.ui.UiText.scale(font)))),
                    getX() + 4, getY() + (getHeight() - 10) / 2, active ? HoiMenuStyle.TEXT : HoiMenuStyle.MUTED);
        }
    }
    private final class Row extends Button {
        Row(String label, int x, int y, int w, int h, Runnable action) {
            super(x,y,w,h,Component.literal(label),b -> action.run(),DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.literal(label)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (active && isHoveredOrFocused()) g.outline(getX(),getY(),getWidth(),getHeight(),HoiMenuStyle.ACCENT);
        }
    }
}
