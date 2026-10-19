package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class ScreenCountry extends Screen implements InputSidebarMovement.InputMovementScreen {
    private ProtocolCountryView view;
    private java.util.function.Consumer<ProtocolCountryPayloads.Action> actionTransport = ClientPlayNetworking::send;
    private java.util.function.BooleanSupplier actionAvailable = () -> ClientPlayNetworking.canSend(ProtocolCountryPayloads.Action.TYPE);
    private ScreenDiplomacyHeader header;
    private String target, token = UUID.randomUUID().toString();
    private ProtocolIntelDomain domain = ProtocolIntelDomain.CIVILIAN;
    private boolean ledger, choosing, pending;
    private ProtocolCountryView.Action confirmation;
    private String amountText = "1";
    private int pane, top, scroll, actionScroll, total, ticks, hudScroll;
    private ProtocolMenuView.Entry detail;
    private int detailScroll, confirmationScroll, leaderTooltipScroll, spiritTooltipScroll;
    private ProtocolMenuView.Entry hoveredSpirit;
    public ScreenCountry(String target) { super(Component.literal("외교")); this.target = target; choosing = target.isEmpty(); }
    public ScreenCountry(ProtocolCountryView fixture) { this(fixture.target()); view = fixture; }
    ScreenCountry(ProtocolCountryView fixture, java.util.function.Consumer<ProtocolCountryPayloads.Action> transport) {
        this(fixture);
        actionTransport = Objects.requireNonNull(transport);
        actionAvailable = () -> true;
    }
    String token() { return token; }
    ProtocolCountryView view() { return view; }
    int panelWidth() { return pane; }
    void select(String tag) {
        target = tag; view = null; token = UUID.randomUUID().toString(); scroll = 0; actionScroll = 0; detail = null; confirmation = null; pending = false; choosing = false;
        ticks = 0; rebuildWidgets();
    }
    public void requestRefresh() {
        token = UUID.randomUUID().toString();
        ticks = 0;
        pending = false;
        confirmation = null;
    }
    public void update(ProtocolDiplomacyPayloads.Response packet) {
        if (packet.status() != ProtocolDiplomacyPayloads.Status.OPEN || !token.equals(packet.token())
                || !pending || confirmation == null || !confirmation.id().equals("lend_lease") || view == null) return;
        var editor = packet.view();
        if (!editor.viewer().equals(view.viewer()) || !editor.target().equals(target)) return;
        pending = false;
        confirmation = null;
        ScreenDialogCoordinator.setContent(new ScreenDiplomacyLease(this, editor));
    }
    public void update(ProtocolDiplomacyVolunteerPayloads.Response packet) {
        if (packet.status() != ProtocolDiplomacyVolunteerPayloads.Status.OPEN || !token.equals(packet.token())
                || !pending || confirmation == null || !confirmation.id().equals("volunteers") || view == null) return;
        var editor = packet.view();
        if (!editor.viewer().equals(view.viewer()) || !editor.target().equals(target)) return;
        pending = false;
        confirmation = null;
        ScreenDialogCoordinator.setContent(new ScreenDiplomacyVolunteers(this, editor));
    }
    public void update(ProtocolCountryPayloads.Response packet) {
        if (!token.equals(packet.token())) return;
        if (packet.json().isEmpty()) { view = null; detail = null; ScreenDialogCoordinator.setContent(null); return; }
        var next = packet.view();
        if (!target.isEmpty() && !next.target().equals(target)) return;
        if (view != null && view.actions() != null && next.actions() != null
                && view.actions().session().equals(next.actions().session()) && next.actions().revision() < view.actions().revision()) return;
        if (next.equals(view)) return;
        pending = false; confirmation = null;
        var oldDetail = detail;
        boolean sameCountry = view != null && view.viewer().equals(next.viewer()) && view.target().equals(next.target());
        view = next; target = next.target();
        detail = sameCountry && oldDetail != null ? entries().stream().filter(e -> e.name().equals(oldDetail.name())).findFirst().orElse(null) : null;
        rebuildWidgets();
    }
    @Override protected void init() {
        pane = Math.min(width - 20, Math.max(210, width * 550 / 2560)); top = UiMenuBar.height(width);
        UiMenuBar.buttons(width, view == null ? "" : view.viewer(), null, tab -> {
            if (tab == ProtocolMenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new UiMenuButton("×", pane - 25, top + 3, 19, 19, this::onClose));
        if (view == null) return;
        header = new ScreenDiplomacyHeader(view, pane);
        int half = (pane - 18) / 2;
        var dip = new UiMenuButton("외교", 7, ScreenDiplomacyHeader.tabs(top), half, 20, () -> { ledger = false; choosing = false; scroll = 0; detail = null; rebuildWidgets(); });
        var intel = new UiMenuButton("첩보 장부", 11 + half, ScreenDiplomacyHeader.tabs(top), half, 20, () -> { ledger = true; choosing = false; scroll = 0; detail = null; rebuildWidgets(); });
        addRenderableWidget(dip); addRenderableWidget(intel);
        if (ledger && !choosing) {
            int cell = (pane - 16) / 4;
            for (var d : ProtocolIntelDomain.values()) {
                var report = view.report(d);
                var button = new UiMenuButton(d.label + " 정보", d.icon, domain == d, 8 + d.ordinal() * cell, ScreenDiplomacyHeader.tabs(top) + 25, cell - 2, 30,
                        () -> { domain = d; scroll = 0; detail = null; rebuildWidgets(); });
                String tips = "총 " + d.label + " 정보: " + String.format(Locale.ROOT, "%.1f%%", report.percent())
                        + (view.shared() ? "\n동맹 정보 공유" : "") + "\n\n" + d.tiers.stream()
                        .map(t -> (ProtocolIntelDomain.permits(report.percent(), view.shared(), t.percent()) ? "✓ " : "× ") + t.percent() + "% · " + t.description())
                        .collect(java.util.stream.Collectors.joining("\n"));
                button.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(tips))); addRenderableWidget(button);
            }
        }
        addRenderableWidget(new UiMenuButton("국가 목록 열기", 8, height - 26, pane - 16, 19, () -> { choosing = !choosing; scroll = 0; detail = null; rebuildWidgets(); }));
        int count = choosing ? view.countries().size() : entries().size(); total = count * 39;
        int rowsWidth = !ledger && !choosing ? pane / 2 - 12 : pane - 22;
        scroll = Math.clamp(scroll, 0, Math.max(0, total - (height - 33 - bodyTop())));
        for (int i = 0; (ledger || choosing) && i < count; i++) {
            int y = bodyTop() + i * 39 - scroll;
            if (y < bodyTop() || y + 36 > height - 33) continue;
            final int index = i;
            String label = choosing ? dev.hoi.client.ui.UiCountryDisplay.name(view.countries().get(i).tag(), view.countries().get(i).name()) : entries().get(i).name() + " · " + entries().get(i).value();
            var button = new ScreenCountryRow(label, 9, y, rowsWidth, 36, () -> {
                if (choosing) select(view.countries().get(index).tag());
                else { detail = entries().get(index); detailScroll = 0; rebuildWidgets(); }
            });
            button.active = detail == null && confirmation == null; addRenderableWidget(button);
        }
        if (actionsVisible()) {
            int pitch = Math.max(16, ScreenDiplomacyHeader.scaled(29, pane));
            actionScroll = Math.clamp(actionScroll, 0, Math.max(0, view.actions().entries().size() * pitch - (height - 33 - bodyTop())));
            int y = bodyTop() - actionScroll;
            for (var action : view.actions().entries()) {
                int actionY = y;
                y += pitch;
                if (actionY < bodyTop() || actionY + pitch > height - 33) continue;
                var button = new ScreenActionButton(action.name(), pane / 2 + 3, actionY, pane / 2 - 12, Math.max(15, ScreenDiplomacyHeader.scaled(28, pane)),
                        () -> { confirmation = action; amountText = "1"; confirmationScroll = 0; if (action.id().startsWith("page_") || action.id().equals("lend_lease") || action.id().equals("volunteers")) { actionScroll = 0; sendAction(); } else rebuildWidgets(); });
                button.active = action.enabled() && !pending && confirmation == null && detail == null;
                button.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(action.name() + "\n" + action.detail())));
                addRenderableWidget(button);
            }
        }
        if (confirmation != null) {
            int x = detailX(), w = detailWidth();
            addRenderableWidget(new UiMenuButton("취소", x + 10, top + 175, (w - 25) / 2, 22,
                    () -> { confirmation = null; rebuildWidgets(); }));
            var confirm = new UiMenuButton("확인", x + 15 + (w - 25) / 2, top + 175, (w - 25) / 2, 22, this::sendAction);
            confirm.active = !pending && (confirmation.amountInput() == null || inputAmount() > 0);
            addRenderableWidget(confirm);
            if (confirmation.amountInput() != null) {
                var amount = new net.minecraft.client.gui.components.EditBox(font,x+12,top+144,w-24,20,Component.literal("수량"));
                amount.setMaxLength(19);amount.setValue(amountText);amount.setEditable(!pending);
                amount.setResponder(value -> {amountText=value;confirm.active=!pending&&inputAmount()>0;});
                addRenderableWidget(amount);
                if(!pending)setInitialFocus(amount);
            }
        }
        if (detail != null) addRenderableWidget(new UiMenuButton("닫기", detailX() + detailWidth() - 43, top + 4, 36, 18, () -> { detail = null; rebuildWidgets(); }));
    }
    private boolean actionsVisible() { return view != null && !ledger && !choosing && view.actions() != null && !view.actions().entries().isEmpty(); }
    private long inputAmount() {
        if(!amountText.matches("[0-9]+"))return -1;
        try {return Long.parseLong(amountText);}catch(NumberFormatException ignored){return -1;}
    }
    private void sendAction() {
        if (pending || confirmation == null || view == null || view.actions() == null || !actionAvailable.getAsBoolean()) return;
        long amount = confirmation.amountInput() == null ? 0 : inputAmount();
        if (confirmation.amountInput() != null && amount <= 0) return;
        var action = view.actions();
        pending = true; ticks = 1;
        actionTransport.accept(new ProtocolCountryPayloads.Action(token, action.session(), action.revision(), confirmation.id(), amount));
        rebuildWidgets();
    }
    private List<ProtocolMenuView.Entry> entries() {
        if (ledger) return view.report(domain).entries();
        return view.relations().stream().map(r -> new ProtocolMenuView.Entry(r.name(), "",
                relationTooltip(r))).toList();
    }
    private static String relationTooltip(ProtocolCountryView.Relation relation) {
        return relation.name() + "\n" + (relation.detail().isEmpty()
                ? relation.countries().stream().map(ProtocolCountryView.Choice::name).collect(java.util.stream.Collectors.joining(" · "))
                : relation.detail());
    }
    String relationTooltipAt(double x, double y) {
        if (view == null || ledger || choosing || detail != null || confirmation != null
                || x < 9 || x >= pane / 2 - 3 || y < bodyTop() || y >= height - 33) return "";
        int row = (int)(y - bodyTop() + scroll) / 39;
        int rowY = bodyTop() + row * 39 - scroll;
        return row >= 0 && row < view.relations().size() && y < rowY + 36
                ? relationTooltip(view.relations().get(row)) : "";
    }
    private int bodyTop() { return choosing ? ScreenDiplomacyHeader.tabs(top) + 25 : ledger ? ScreenDiplomacyHeader.tabs(top) + 75 : ScreenDiplomacyHeader.body(top, pane); }
    private int detailX() { return width - pane >= 200 ? pane + 5 : 5; }
    private int detailWidth() { return Math.min(330, width - detailX() - 6); }
    @Override public void tick() {
        if (pending && ++ticks >= 100) {
            pending = false;
            confirmation = null;
            ticks = 0;
            rebuildWidgets();
        }
        if (!pending && confirmation == null && ticks++ % 40 == 0 && ClientPlayNetworking.canSend(ProtocolCountryPayloads.Request.TYPE)) {
            token = UUID.randomUUID().toString(); ClientPlayNetworking.send(new ProtocolCountryPayloads.Request(token, target));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        UiMenuBar.draw(g, width, view == null ? ProtocolCountryHud.UNKNOWN : view.hud(), mx, my, hudScroll);
        UiMenuStyle.panel(g, 0, top, pane, height - top);
        UiMenuStyle.heading(g, font, "외교", 10, top + 9, pane - 44, UiMenuStyle.TEXT);
        if (view == null) dev.hoi.client.ui.UiText.text(g, font, "서버 정보 불러오는 중…", 12, top + 38, UiMenuStyle.MUTED);
        else {
            String[] targetStats = {"안정도", "전쟁 지지도"};
            int statWidth = Math.min(58, (pane - 76) / 2), statsX = pane - 30 - statWidth * 2;
            UiMenuStyle.recess(g, statsX, top + 4, statWidth * 2, 18);
            for (int i = 0; i < targetStats.length; i++) {
                var stat = ScreenDiplomacyHeader.entry(view, targetStats[i]);
                String value = stat.value().endsWith("%") ? ProtocolNumberBounds.percent(stat.progress(), 0) : stat.value();
                int x = statsX + i * statWidth;
                dev.hoi.client.ui.UiText.iconText(g, font, i == 0 ? "hud/stability" : "hud/war_support",
                        value.equals("정보 없음") ? "?" : value, x, top + 4, statWidth, 18, 11, UiMenuStyle.TEXT);
                if (mx >= x && mx < x + statWidth && my >= top + 4 && my < top + 22)
                    dev.hoi.client.ui.UiTooltips.drawExact(g, font, dev.hoi.client.ui.UiCountryDisplay.name(view.target(), view.name()) + " · " + stat.name() + ": " + (stat.value().endsWith("%") ? ProtocolNumberBounds.percent(stat.progress(), 2) : stat.value()), mx, my);
            }
            int headerX = ScreenDiplomacyHeader.scaled(139, pane), headerW = pane - headerX - 7;
            UiAssets.nineSlice(g, "diplomacy/header", headerX, top + 30, headerW, 38, 5, pane / 550.0);
            int flagH = 32, flagW = flagH * 98 / 67;
            UiMenuBar.drawFlag(g, view.target(), (headerX - flagW) / 2, top + 33, flagW, flagH);
            int headerEdge = Math.max(1, ScreenDiplomacyHeader.scaled(5, pane));
            int ideologyCell = headerEdge + Math.round((headerW - headerEdge * 2) * 91f / 388);
            int ideologyX = headerX + headerEdge, textX = headerX + ideologyCell + 5;
            var ideology = ScreenDiplomacyHeader.entry(view, "이념");
            UiAssets.draw(g, ideology.icon().isEmpty() ? "politics/empty/ideology" : ideology.icon(), ideologyX, top + 33, Math.max(1, ideologyCell - headerEdge), 32);
            String faction = view.diplomacy().stream().filter(e -> e.name().equals("세력")).map(ProtocolMenuView.Entry::value).findFirst().orElse("세력 없음");
            String[] info = {dev.hoi.client.ui.UiCountryDisplay.name(view.target(), view.name()), faction, ScreenDiplomacyHeader.entry(view, "지도자").value()};
            for (int i = 0; i < info.length; i++)
                dev.hoi.client.ui.UiText.text(g, font, trim(info[i], pane - textX - 47), textX, top + 34 + i * 10, UiMenuStyle.TEXT);
            var factionEntry = ScreenDiplomacyHeader.entry(view, "세력");
            if (!factionEntry.icon().isEmpty() && !factionEntry.value().isBlank()
                    && !Set.of("세력 없음", "미가입", "없음", "정보 없음", "—").contains(factionEntry.value().strip())) {
                UiAssets.draw(g, factionEntry.icon(), pane - 44, top + 33, 32, 32);
                if (mx >= pane - 44 && mx < pane - 12 && my >= top + 33 && my < top + 65)
                    dev.hoi.client.ui.UiTooltips.draw(g, font, factionEntry.value() + "\n" + factionEntry.detail(), mx, my);
            }
            if (ledger && !choosing) for (var d : ProtocolIntelDomain.values()) {
                int cell = (pane - 16) / 4, x = 8 + d.ordinal() * cell;
                dev.hoi.client.ui.UiText.text(g, font, (int)view.report(d).percent() + "%", x + 10, ScreenDiplomacyHeader.tabs(top) + 58, UiMenuStyle.TEXT);
            }
            if (!ledger && !choosing) header.draw(g, font, view, pane, top);
            if (!ledger && !choosing) {
                UiMenuStyle.recess(g, 5, bodyTop() - 4, pane / 2 - 8, Math.max(6, height - 27 - bodyTop()));
                UiMenuStyle.recess(g, pane / 2, bodyTop() - 4, pane / 2 - 5, Math.max(6, height - 27 - bodyTop()));
            } else UiMenuStyle.recess(g, 5, bodyTop() - 3, pane - 10, Math.max(6, height - 30 - bodyTop()));
            g.enableScissor(6, bodyTop(), pane - 6, height - 33);
            int count = choosing ? view.countries().size() : entries().size();
            int rowsWidth = !ledger && !choosing ? pane / 2 - 12 : pane - 22;
            for (int i = 0; i < count; i++) {
                int y = bodyTop() + i * 39 - scroll;
                if (y + 36 < bodyTop() || y > height - 33) continue;
                if (!ledger && !choosing) UiAssets.nineSlice(g, "diplomacy/relations", 9, y, rowsWidth, 36, 4, pane / 550.0);
                else UiMenuStyle.control(g, 9, y, rowsWidth, 36, false, false);
                if (!choosing && entries().get(i).name().equals("정보가 충분하지 않습니다"))
                    UiAssets.draw(g, "intel/no_intel", 9, y, rowsWidth, 36);
                if (!ledger && !choosing) {
                    var relation=view.relations().get(i);
                    UiAssets.draw(g,relation.icon(),11,y+7,22,22);
                    int fw=Math.max(20,ScreenDiplomacyHeader.scaled(44,pane))+2,fh=Math.round(fw*38f/52);
                    for(int j=0;j<relation.countries().size();j++) {
                        var country=relation.countries().get(j);
                        int fx=dev.hoi.client.ui.UiFlagStack.left(j,relation.countries().size(),34,rowsWidth-26,fw),fy=y+(36-fh)/2;
                        dev.hoi.client.ui.UiFlagStack.draw(g,"country/"+country.tag().toLowerCase(Locale.ROOT)+"/flag",fx,fy,fw,fh);
                    }
                    continue;
                }
                String icon = choosing ? "country/" + view.countries().get(i).tag().toLowerCase(Locale.ROOT) + "/flag" : ledger ? domain.icon : "country/" + view.target().toLowerCase(Locale.ROOT) + "/flag";
                UiAssets.draw(g, icon, 12, y + 5, 27, 25);
                String name = choosing ? dev.hoi.client.ui.UiCountryDisplay.name(view.countries().get(i).tag(), view.countries().get(i).name()) : entries().get(i).name();
                String value = choosing ? view.countries().get(i).tag() : entries().get(i).value();
                dev.hoi.client.ui.UiText.text(g, font, trim(name, rowsWidth - 42), 46, y + 6, UiMenuStyle.TEXT);
                dev.hoi.client.ui.UiText.text(g, font, trim(value, rowsWidth - 42), 46, y + 21, UiMenuStyle.MUTED);
            }
            g.disableScissor();
            if (view.actions() != null && !view.actions().message().isBlank()) {
                var lines = font.split(Component.literal(view.actions().message()), pane - 24);
                int messageY = height - 42 - Math.min(3, lines.size()) * 12;
                for (var line : lines.stream().limit(3).toList()) { dev.hoi.client.ui.UiText.text(g, font, line, 12, messageY, 0xFFFFBB22); messageY += 12; }
            }
            if (confirmation != null) {
                int x = detailX(), w = detailWidth();
                g.nextStratum(); UiMenuStyle.panel(g, x, top, w, 207);
                UiMenuStyle.heading(g, font, confirmation.name(), x + 10, top + 9, w - 20, UiMenuStyle.TEXT);
                var lines = font.split(Component.literal(dev.hoi.client.ui.UiCountryDisplay.name(view.target(), view.name()) + "\n\n" + confirmation.detail()), w - 24);
                confirmationScroll = Math.clamp(confirmationScroll, 0, Math.max(0, lines.size() * 13 - (confirmation.amountInput() == null ? 127 : 84)));
                g.enableScissor(x + 7, top + 36, x + w - 7, top + (confirmation.amountInput() == null ? 165 : 122));
                int y = top + 38 - confirmationScroll;
                for (var line : lines) {
                    dev.hoi.client.ui.UiText.text(g, font, line, x + 12, y, UiMenuStyle.TEXT); y += 13;
                }
                g.disableScissor();
            }
            if (confirmation != null && confirmation.amountInput() != null)
                dev.hoi.client.ui.UiText.text(g,font,"수량",detailX()+12,top+130,UiMenuStyle.TEXT);
            if (detail != null) {
                int x = detailX(), w = detailWidth();
                g.nextStratum(); UiMenuStyle.panel(g, x, top, w, height - top - 31);
                UiMenuStyle.heading(g, font, trim(detail.name(), w - 55), x + 9, top + 9, w - 44, UiMenuStyle.TEXT);
                var lines = font.split(Component.literal(detail.value() + "\n\n" + (detail.detail().isEmpty() ? "추가 정보 없음" : detail.detail())), w - 22);
                detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - (height - top - 78)));
                g.enableScissor(x + 7, top + 32, x + w - 7, height - 37);
                int y = top + 34 - detailScroll;
                for (var line : lines) { dev.hoi.client.ui.UiText.text(g, font, line, x + 11, y, UiMenuStyle.TEXT); y += 13; }
                g.disableScissor();
            }
        }
        super.extractRenderState(g, mx, my, delta);
        String relationTooltip = relationTooltipAt(mx, my);
        if (!relationTooltip.isEmpty()) dev.hoi.client.ui.UiTooltips.drawExact(g, font, relationTooltip, mx, my);
        if (view != null && !ledger && !choosing && detail == null && confirmation == null) {
            var spirit = header.spiritAt(mx, my, pane, top);
            var party = header.partyAt(view,mx,my,pane,top);
            if(party!=null) dev.hoi.client.ui.UiTooltips.drawExact(g,font,party.detail(),mx,my);
            if (header.leaderAt(mx, my, pane, top))
                leaderTooltipScroll = dev.hoi.client.ui.UiPoliticalTooltip.drawLeader(g, font, ScreenDiplomacyHeader.entry(view, "지도자"),
                        view.target(), dev.hoi.client.ui.UiCountryDisplay.name(view.target(), view.name()), mx, my, width, height, leaderTooltipScroll);
            else leaderTooltipScroll = 0;
            if (!java.util.Objects.equals(spirit, hoveredSpirit)) spiritTooltipScroll = 0;
            hoveredSpirit = spirit;
            if (spirit != null) spiritTooltipScroll = dev.hoi.client.ui.UiPoliticalTooltip.draw(g, font,
                    new ProtocolMenuView.Entry(spirit.name(), "", spirit.detail().substring("국가 정신\n".length()), spirit.progress(), spirit.icon()),
                    mx, my, width, height, spiritTooltipScroll);
        }
    }
    ProtocolCountryView.Choice flagAt(double x,double y) {
        if(view==null||ledger||choosing||detail!=null||confirmation!=null
                ||x<9||x>=pane/2-3||y<bodyTop()||y>=height-33||view.relations().isEmpty())return null;
        int row=(int)(y-bodyTop()+scroll)/39;
        if(row<0||row>=view.relations().size())return null;
        var countries=view.relations().get(row).countries();
        int fw=Math.max(20,ScreenDiplomacyHeader.scaled(44,pane))+2,fh=Math.round(fw*38f/52),ry=bodyTop()+row*39-scroll;
        for(int i=0;i<countries.size();i++) {
            int fx=dev.hoi.client.ui.UiFlagStack.left(i,countries.size(),34,pane/2-38,fw);
            int visible=dev.hoi.client.ui.UiFlagStack.visibleWidth(i,countries.size(),34,pane/2-38,fw);
            if(x>=fx&&x<fx+visible&&y>=ry+(36-fh)/2&&y<ry+(36+fh)/2)return countries.get(i);
        }
        return null;
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice) {
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            var country=flagAt(event.x(),event.y());
            if(country!=null){select(country.tag());return true;}
        }
        return super.mouseClicked(event,twice);
    }
    private String trim(String text, int w) { return dev.hoi.client.ui.UiText.ellipsize(font, text, w); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (y < UiMenuBar.STATS_HEIGHT && x >= 40 && x < UiMenuBar.statsRight(width)) {
            hudScroll = UiMenuBar.scroll(width, view == null ? ProtocolCountryHud.UNKNOWN : view.hud(), hudScroll, horizontal, vertical); return true;
        }
        if (view != null && !ledger && !choosing && detail == null && confirmation == null
                && header.spiritAt(x, y, pane, top) != null
                && (com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT))) {
            spiritTooltipScroll -= (int)(vertical * 3); return true;
        }
        if (view != null && !ledger && !choosing && detail == null && confirmation == null
                && header.scrollSpirits(x, y, horizontal != 0 ? horizontal : vertical, pane, top)) return true;
        if (view != null && !ledger && !choosing && detail == null && confirmation == null
                && header.leaderAt(x, y, pane, top) && (com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT))) {
            leaderTooltipScroll -= (int)(vertical * 3); return true;
        }
        if (confirmation != null) { confirmationScroll -= (int)(vertical * 26); return true; }
        if (detail != null) { detailScroll -= (int)(vertical * 26); return true; }
        if (x < pane && y >= bodyTop()) {
            if (actionsVisible() && x >= pane / 2) actionScroll -= (int)(vertical * Math.max(16, ScreenDiplomacyHeader.scaled(29, pane)));
            else scroll -= (int)(vertical * 39);
            rebuildWidgets(); return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (confirmation != null && event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && !pending) { confirmation = null; rebuildWidgets(); return true; }
        if (detail != null && event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { detail = null; rebuildWidgets(); return true; }
        if (allowsMovement() && InputSidebarMovement.consumes(minecraft, event)) return true;
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) { return allowsMovement() && InputSidebarMovement.consumes(minecraft, event) || super.keyReleased(event); }
    @Override public int unitHudLeft(){return pane;}
    @Override public boolean allowsMovement() { return detail == null && confirmation == null; }
    @Override public void removed() { InputSidebarMovement.release(minecraft); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private final class ScreenActionButton extends Button {
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { dev.hoi.client.audio.AudioUiSounds.play("ui.click"); }
        ScreenActionButton(String label, int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            UiAssets.nineSlice(g, "diplomacy/" + (active ? "action" : "action_disabled"), getX(), getY(), getWidth(), getHeight(), 3, pane / 550.0);
            dev.hoi.client.ui.UiText.cell(g, font, getMessage().getString(), getX() + 4, getY(), getWidth() - 8, getHeight(),
                    active ? UiMenuStyle.TEXT : UiMenuStyle.MUTED, false);
        }
    }
    private final class ScreenCountryRow extends Button {
        ScreenCountryRow(String label, int x, int y, int w, int h, Runnable action) {
            super(x,y,w,h,Component.literal(label),b -> action.run(),DEFAULT_NARRATION);
            setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(label)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (active && isHoveredOrFocused()) g.outline(getX(),getY(),getWidth(),getHeight(),UiMenuStyle.ACCENT);
        }
    }
}
