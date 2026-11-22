package dev.hoi.client.screen.diplomacy;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.country.CountryFlags;
import dev.hoi.protocol.*;
import dev.hoi.client.screen.agency.AgencyIntelLedger;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.diplomacy.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class DiplomacyCountryScreen extends Screen implements InputSidebarMovement.InputMovementScreen {
    private CountryView view;
    private java.util.function.Consumer<CountryPayloads.Action> actionTransport = ClientPlayNetworking::send;
    private java.util.function.BooleanSupplier actionAvailable = () -> ClientPlayNetworking.canSend(CountryPayloads.Action.TYPE);
    private DiplomacyHeader header;
    private AgencyIntelLedger intelLedger;
    private final dev.hoi.client.ui.UiDiplomacyAssets assets = new dev.hoi.client.ui.UiDiplomacyAssets();
    private final DiplomacyArt art = new DiplomacyArt();
    private final DiplomacyCountryList list = new DiplomacyCountryList(assets);
    private long ledgerOpenedAt;
    private String target, token = UUID.randomUUID().toString();
    private AgencyIntelDomain domain = AgencyIntelDomain.CIVILIAN;
    private boolean ledger, choosing, pending;
    private CountryView.Action confirmation;
    private String openingAction = "";
    private String amountText = "1";
    private int pane, top, scroll, actionScroll, total, ticks, hudScroll;
    private MenuView.Entry detail;
    private int detailScroll, confirmationScroll, leaderTooltipScroll, spiritTooltipScroll;
    private MenuView.Entry hoveredSpirit;
    public DiplomacyCountryScreen(String target) { super(Component.literal("외교")); this.target = target; choosing = target.isEmpty(); }
    public String target() { return target; }
    public DiplomacyCountryScreen(CountryView fixture) { this(fixture.target()); view = fixture; }
    public DiplomacyCountryScreen(CountryView fixture, java.util.function.Consumer<CountryPayloads.Action> transport) {
        this(fixture);
        actionTransport = Objects.requireNonNull(transport);
        actionAvailable = () -> true;
    }
    public DiplomacyCountryScreen(String target, String action) { this(target); openingAction = action; }
    private static boolean leaseAction(String id) { return id.equals("lend_lease") || id.equals("request_lend_lease") || id.startsWith("answer_lease:"); }
    String token() { return token; }
    CountryView view() { return view; }
    int panelWidth() { return pane; }
    boolean choosing() { return choosing; }
    List<String> listed() { return view == null ? List.of() : list.rows(view).stream().map(CountryView.Choice::tag).toList(); }
    DiplomacyCountryList list() { return list; }
    void select(String tag) {
        ledgerOpenedAt = net.minecraft.util.Util.getMillis();
        target = tag; view = null; token = UUID.randomUUID().toString(); scroll = 0; actionScroll = 0; detail = null; confirmation = null; pending = false; choosing = false;
        ticks = 0; rebuildWidgets();
    }
    public void requestRefresh() {
        token = UUID.randomUUID().toString();
        ticks = 0;
        pending = false;
        confirmation = null;
    }
    public void update(DiplomacyPayloads.Response packet) {
        if (packet.status() != DiplomacyPayloads.Status.OPEN || !token.equals(packet.token())
                || !pending || confirmation == null || !leaseAction(confirmation.id()) || view == null) return;
        var editor = packet.view();
        if (!editor.viewer().equals(view.viewer()) || !editor.target().equals(target)) return;
        pending = false;
        boolean answer = confirmation.id().startsWith("answer_lease:");
        boolean request = confirmation.id().equals("request_lend_lease");
        confirmation = null;
        DialogCoordinator.setContent(new DiplomacyLease(this, editor, answer ? DiplomacyLease.Presentation.ANSWER : request ? DiplomacyLease.Presentation.REQUEST : DiplomacyLease.Presentation.OFFER));
    }
    public void update(DiplomacyVolunteerPayloads.Response packet) {
        if (packet.status() != DiplomacyVolunteerPayloads.Status.OPEN || !token.equals(packet.token())
                || !pending || confirmation == null || !confirmation.id().equals("volunteers") || view == null) return;
        var editor = packet.view();
        if (!editor.viewer().equals(view.viewer()) || !editor.target().equals(target)) return;
        pending = false;
        confirmation = null;
        DialogCoordinator.setContent(new DiplomacyVolunteers(this, editor));
    }
    public void update(CountryPayloads.Response packet) {
        if (!token.equals(packet.token())) return;
        if (packet.json().isEmpty()) { view = null; detail = null; DialogCoordinator.setContent(null); return; }
        var next = packet.view();
        if (!target.isEmpty() && !next.target().equals(target)) return;
        if (view != null && view.actions() != null && next.actions() != null
                && view.actions().session().equals(next.actions().session()) && next.actions().revision() < view.actions().revision()) return;
        if (next.equals(view)) return;
        pending = false; confirmation = null;
        var oldDetail = detail;
        boolean sameCountry = view != null && view.viewer().equals(next.viewer()) && view.target().equals(next.target());
        view = next; target = next.target();
        if (!openingAction.isEmpty()) {
            String action = openingAction; openingAction = "";
            confirmation = next.actions() == null ? null : next.actions().entries().stream().filter(entry -> entry.id().equals(action) && entry.enabled()).findFirst().orElse(null);
            if (confirmation != null) { sendAction(); return; }
        }
        detail = sameCountry && oldDetail != null ? entries().stream().filter(e -> e.name().equals(oldDetail.name())).findFirst().orElse(null) : null;
        rebuildWidgets();
    }
    @Override protected void init() {
        pane = Math.min(width - 20, Math.max(210, width * 550 / 2560)); top = HudMenuBar.height(width);
        HudMenuBar.buttons(width, view == null ? "" : view.viewer(), null, tab -> {
            if (tab == MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(UiMenuStyle.close(pane - 21.5f, top + 4.5f, this::onClose));
        list.layout(pane, top, height);
        if (view == null) return;
        header = new DiplomacyHeader(view, pane);
        intelLedger = null;
        if (choosing) return;
        int half = (pane - 18) / 2;
        var dip = new UiMenuButton("외교", 7, DiplomacyHeader.tabs(top), half, 20, () -> { ledger = false; choosing = false; scroll = 0; detail = null; rebuildWidgets(); });
        var intel = new UiMenuButton("첩보 장부", 11 + half, DiplomacyHeader.tabs(top), half, 20, () -> { ledgerOpenedAt = net.minecraft.util.Util.getMillis(); ledger = true; choosing = false; scroll = 0; detail = null; rebuildWidgets(); });
        dip.background("none"); intel.background("none");
        addRenderableWidget(dip); addRenderableWidget(intel);
        if (ledger) {
            int cell = (pane - 16) / 4;
            for (var d : AgencyIntelDomain.values()) {
                var report = view.report(d);
                var button = new UiMenuButton(d.label + " 정보", 8 + d.ordinal() * cell, DiplomacyHeader.tabs(top) + 25, cell, 30,
                        () -> { ledgerOpenedAt = net.minecraft.util.Util.getMillis(); domain = d; scroll = 0; detail = null; rebuildWidgets(); }).caption("").background("none");
                addRenderableWidget(button);
            }
            intelLedger = new AgencyIntelLedger(view.report(domain), domain, pane, DiplomacyHeader.tabs(top) + 55, height - 33, ledgerOpenedAt, assets);
            intelLedger.addHoverRegions(this::addRenderableWidget);
        }
        addRenderableWidget(new UiMenuButton("국가 목록 열기", DiplomacyHeader.scaled(50, pane), height - DiplomacyHeader.scaled(48, pane), DiplomacyHeader.scaled(221, pane), DiplomacyHeader.scaled(34, pane), () -> { choosing = true; list.reset(); detail = null; confirmation = null; rebuildWidgets(); }).background("agency/operations/gfx_button_221x34/frame_0"));
        total = entries().size() * relationPitch();
        scroll = Math.clamp(scroll, 0, Math.max(0, total - (height - 33 - bodyTop())));
        if (view.actions() != null && !ledger) {
            view.actions().entries().stream().filter(action -> action.id().equals("open_logistics")).findFirst().ifPresent(action -> {
                var logistics = new UiMenuButton(action.name(), DiplomacyHeader.scaled(296, pane), height - DiplomacyHeader.scaled(48, pane), DiplomacyHeader.scaled(221, pane), DiplomacyHeader.scaled(34, pane),
                        () -> dev.hoi.client.network.NetworkScreens.openForeignLogistics(target)).background("agency/operations/gfx_button_221x34/frame_0").sound("ui.close");
                logistics.active = action.enabled() && !pending && confirmation == null && detail == null;
                logistics.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.createExact(action.detail()));
                addRenderableWidget(logistics);
            });
        }
        if (actionsVisible()) {
            int pitch = Math.max(14, DiplomacyHeader.scaled(29, pane));
            actionScroll = Math.clamp(actionScroll, 0, Math.max(0, (int)view.actions().entries().stream().filter(action -> !action.id().equals("open_logistics")).count() * pitch - (height - 33 - bodyTop())));
            int y = bodyTop() - actionScroll;
            for (var action : view.actions().entries()) {
                if (action.id().equals("open_logistics")) continue;
                int actionY = y;
                y += pitch;
                if (actionY < bodyTop() || actionY + pitch > height - 33) continue;
                var button = new DiplomacyActionButton(action, pane / 2 + 3, actionY, pane / 2 - 12, pitch - 1,
                        () -> { confirmation = action; amountText = "1"; confirmationScroll = 0; if (action.id().startsWith("page_") || leaseAction(action.id()) || action.id().equals("volunteers")) { actionScroll = 0; sendAction(); } else rebuildWidgets(); });
                button.active = action.enabled() && !pending && confirmation == null && detail == null;
                button.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal(action.name() + "\n" + action.detail())));
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
        actionTransport.accept(new CountryPayloads.Action(token, action.session(), action.revision(), confirmation.id(), amount));
        rebuildWidgets();
    }
    private List<MenuView.Entry> entries() {
        if (ledger) return view.report(domain).entries();
        return view.relations().stream().map(r -> new MenuView.Entry(r.name(), "",
                relationTooltip(r))).toList();
    }
    private static String relationTooltip(CountryView.Relation relation) {
        return relation.detail().isEmpty() ? relation.name() + "\n" + relation.countries().stream().map(CountryView.Choice::name).collect(java.util.stream.Collectors.joining("\n"))
                : relation.detail();
    }
    String relationTooltipAt(double x, double y) {
        if (view == null || ledger || choosing || detail != null || confirmation != null
                || x < 9 || x >= pane / 2 - 3 || y < bodyTop() || y >= height - 33) return "";
        int row = (int)(y - bodyTop() + scroll) / relationPitch();
        int rowY = bodyTop() + row * relationPitch() - scroll;
        return row >= 0 && row < view.relations().size() && y < rowY + relationPitch() - 2
                ? relationTooltip(view.relations().get(row)) : "";
    }
    private int bodyTop() { return choosing ? DiplomacyHeader.tabs(top) + 25 : ledger ? DiplomacyHeader.tabs(top) + 75 : DiplomacyHeader.body(top, pane); }
    private int relationPitch() { return DiplomacyHeader.scaled(50, pane); }
    String ledgerDetailAt(double x, double y) { return intelLedger == null ? "" : intelLedger.detailAt(x, y); }
    private int detailX() { return width - pane >= 200 ? pane + 5 : 5; }
    private int detailWidth() { return Math.min(330, width - detailX() - 6); }
    @Override public void tick() {
        if (pending && ++ticks >= 100) {
            pending = false;
            confirmation = null;
            ticks = 0;
            rebuildWidgets();
        }
        if (!pending && confirmation == null && ticks++ % 40 == 0 && ClientPlayNetworking.canSend(CountryPayloads.Request.TYPE)) {
            token = UUID.randomUUID().toString(); ClientPlayNetworking.send(new CountryPayloads.Request(token, target));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        assets.beginFrame();
        HudMenuBar.draw(g, width, view == null ? CountryHud.UNKNOWN : view.hud(), mx, my, hudScroll);
        UiMenuStyle.window(g, UiMenuStyle.DIPLOMACY, 0, top, pane, height - top);
        UiMenuStyle.title(g, font, UiMenuStyle.DIPLOMACY, "외교", 0, top, pane);
        if (view == null) dev.hoi.client.ui.UiText.text(g, font, "서버 정보 불러오는 중…", 12, top + 38, UiMenuStyle.MUTED);
        else if (choosing) list.draw(g, font, view, mx, my);
        else {
            String[] targetStats = {"안정도", "전쟁 지지도"};
            float s = pane / 550f;
            if (!view.player().isEmpty()) {
                UiAssets.drawExact(g, "production/picker/text_bg", 173 * s, top + 9 * s, 168 * s, 33 * s);
                UiAssets.drawExact(g, "diplomacy/players_icon", 183 * s, top + 15 * s, 20 * s, 20 * s);
                headerText(g, view.player(), 206 * s, top + 25.5f * s, 120 * s, -1, UiMenuStyle.TEXT);
            } else if (view.opinion() != null) {
                CountryFlags.draw(g, "country/" + view.viewer().toLowerCase(Locale.ROOT) + "/flag", "small", 261 * s, top + 9 * s, 20 * s, 17 * s);
                CountryFlags.draw(g, "country/" + view.target().toLowerCase(Locale.ROOT) + "/flag", "small", 305 * s, top + 21 * s, 20 * s, 17 * s);
                int[] opinions = {view.opinion().ours(), view.opinion().theirs()};
                for (int i = 0; i < opinions.length; i++) {
                    int opinion = opinions[i];
                    headerText(g, (opinion > 0 ? "+" : "") + opinion, (i == 0 ? 282 : 252) * s, top + (i == 0 ? 12.5f : 37.5f) * s, 50 * s, 0, opinion < 0 ? 0xFFFF4444 : opinion > 0 ? 0xFF44FF44 : UiMenuStyle.TEXT);
                }
            }
            UiAssets.drawExact(g, "diplomacy/unity_bg", 330 * s, top + 10 * s, 142 * s, 33 * s);
            for (int i = 0; i < targetStats.length; i++) {
                var stat = DiplomacyHeader.entry(view, targetStats[i]);
                String value = stat.value().endsWith("%") ? ProtocolNumberBounds.percent(stat.progress(), 0) : stat.value();
                float x = (i == 0 ? 331 : 402) * s, icon = (i == 0 ? 375 : 443) * s;
                UiAssets.drawExact(g, i == 0 ? "hud/stability" : "hud/war_support", icon, top + 14 * s, (i == 0 ? 27 : 26) * s, (i == 0 ? 25 : 24) * s);
                headerText(g, value.equals("정보 없음") ? "0" : value, x, top + 26.5f * s, icon - x - s, 1, UiMenuStyle.TEXT);
                if (mx >= x && mx < icon + 27 * s && my >= top + 10 * s && my < top + 43 * s)
                    dev.hoi.client.ui.tooltip.Tooltips.drawExact(g, font, dev.hoi.client.ui.country.CountryDisplays.name(view.target(), view.name()) + " · " + stat.name() + ": " + (stat.value().endsWith("%") ? ProtocolNumberBounds.percent(stat.progress(), 2) : stat.value()), mx, my);
            }
            int headerX = DiplomacyHeader.scaled(139, pane), headerW = pane - headerX - 7;
            UiAssets.nineSlice(g, "diplomacy/header", headerX, top + 25, headerW, 32, 5, pane / 550.0);
            int flagH = 32, flagW = flagH * 98 / 67;
            HudMenuBar.drawFlag(g, view.target(), (headerX - flagW) / 2, top + 25, flagW, flagH);
            int headerEdge = Math.max(1, DiplomacyHeader.scaled(5, pane));
            int ideologyCell = headerEdge + Math.round((headerW - headerEdge * 2) * 91f / 388);
            int ideologyX = headerX + headerEdge, textX = headerX + ideologyCell + 5;
            var ideology = DiplomacyHeader.entry(view, "이념");
            UiAssets.draw(g, ideology.icon().isEmpty() ? "politics/empty/ideology" : ideology.icon(), ideologyX, top + 25, Math.max(1, ideologyCell - headerEdge), 32);
            String faction = view.diplomacy().stream().filter(e -> e.name().equals("세력")).map(MenuView.Entry::value).findFirst().orElse("세력 없음");
            var factionEntry = DiplomacyHeader.entry(view, "세력");
            boolean factionIcon = !factionEntry.icon().isEmpty() && !factionEntry.value().isBlank()
                    && !Set.of("세력 없음", "미가입", "없음", "정보 없음", "—").contains(factionEntry.value().strip());
            String[] info = {dev.hoi.client.ui.country.CountryDisplays.name(view.target(), view.name()), faction, DiplomacyHeader.entry(view, "지도자").value()};
            for (int i = 0; i < info.length; i++)
                dev.hoi.client.ui.UiText.scrollingCell(g, font, Component.literal(info[i]), textX, top + 25 + i * 10, pane - textX - (factionIcon ? 47 : 12), 10, UiMenuStyle.TEXT, false);
            if (factionIcon) {
                UiAssets.draw(g, factionEntry.icon(), pane - 44, top + 25, 32, 32);
                if (mx >= pane - 44 && mx < pane - 12 && my >= top + 25 && my < top + 57)
                    dev.hoi.client.ui.tooltip.Tooltips.draw(g, font, factionEntry.detail().isEmpty() ? factionEntry.value() : factionEntry.detail(), mx, my);
            }
            int half = (pane - 18) / 2;
            UiAssets.nineSlice(g, AgencyIntelLedger.source("tab_intel_ledger", ledger ? 0 : 1), 7, DiplomacyHeader.tabs(top), half, 20, 5, pane / 550.0);
            UiAssets.nineSlice(g, AgencyIntelLedger.source("tab_intel_ledger", ledger ? 1 : 0), 11 + half, DiplomacyHeader.tabs(top), half, 20, 5, pane / 550.0);
            if (ledger) for (var d : AgencyIntelDomain.values()) {
                int cell = (pane - 16) / 4, x = 8 + d.ordinal() * cell;
                int y = DiplomacyHeader.tabs(top) + 25;
                UiAssets.nineSlice(g, AgencyIntelLedger.source("intel_ledger_tab_btn", domain == d ? 1 : 0), x, y, cell, 30, 5, pane / 550.0);
                UiAssets.draw(g, d.icon, x + (cell - 22) / 2, y + 1, 22, 16);
                dev.hoi.client.ui.UiText.centered(g, font, Math.round(view.report(d).percent()) + "%", x, y + 17, cell, 12, UiMenuStyle.TEXT);
            }
            if (!ledger) header.draw(g, font, view, pane, top, assets);
            if (!ledger) {
                assets.tile(g, "commander/commands/tiled_window_1b_thin_border/frame_0", 5, bodyTop() - 4, pane / 2 - 8, Math.max(6, height - 27 - bodyTop()), 64, 64, pane / 550.0);
                assets.tile(g, "commander/commands/tiled_window_1b_thin_border/frame_0", pane / 2, bodyTop() - 4, pane / 2 - 5, Math.max(6, height - 27 - bodyTop()), 64, 64, pane / 550.0);
            }
            if (intelLedger != null) intelLedger.draw(g, font);
            g.enableScissor(6, bodyTop(), pane - 6, height - 33);
            for (int i = 0; !ledger && i < entries().size(); i++) {
                int y = bodyTop() + i * relationPitch() - scroll;
                if (y + relationPitch() - 2 < bodyTop() || y > height - 33) continue;
                UiAssets.draw(g, "diplomacy/relations", 9, y + DiplomacyHeader.scaled(3, pane), DiplomacyHeader.scaled(235, pane), DiplomacyHeader.scaled(44, pane));
                if (entries().get(i).name().equals("정보가 충분하지 않습니다"))
                    UiAssets.draw(g, "intel/no_intel", 9, y, pane / 2 - 12, 36);
                var relation=view.relations().get(i);
                int[] iconSize=UiAssets.imageSize(relation.icon());
                UiAssets.draw(g,relation.icon(),9-DiplomacyHeader.scaled(3,pane),y+DiplomacyHeader.scaled(12,pane),DiplomacyHeader.scaled(iconSize[0],pane),DiplomacyHeader.scaled(iconSize[1],pane));
                int fw=DiplomacyHeader.scaled(47,pane),fh=DiplomacyHeader.scaled(32,pane);
                g.enableScissor(9 + DiplomacyHeader.scaled(40, pane), y + DiplomacyHeader.scaled(3, pane), 9 + DiplomacyHeader.scaled(200, pane), y + DiplomacyHeader.scaled(47, pane));
                for(int j=0;j<relation.countries().size();j++) {
                    var country=relation.countries().get(j);
                    int fx=dev.hoi.client.ui.country.CountryFlagStack.left(j,relation.countries().size(),9+DiplomacyHeader.scaled(40,pane),DiplomacyHeader.scaled(160,pane),fw);
                    assets.relationFlag(g,"country/"+country.tag().toLowerCase(Locale.ROOT)+"/flag",fx,DiplomacyHeader.flagTop(y,pane),fw,fh);
                }
                g.disableScissor();
            }
            g.disableScissor();
            if (confirmation != null) {
                int x = detailX(), w = detailWidth();
                g.nextStratum(); UiMenuStyle.panel(g, x, top, w, 207);
                UiMenuStyle.heading(g, font, confirmation.name(), x + 10, top + 9, w - 20, UiMenuStyle.TEXT);
                var lines = dev.hoi.client.ui.tooltip.Tooltips.split(font,Component.literal(dev.hoi.client.ui.country.CountryDisplays.name(view.target(), view.name()) + "\n\n" + confirmation.detail()), w - 24);
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
                UiMenuStyle.heading(g, font, detail.name(), x + 9, top + 9, w - 44, UiMenuStyle.TEXT);
                var lines = dev.hoi.client.ui.tooltip.Tooltips.split(font,Component.literal(detail.value() + "\n\n" + (detail.detail().isEmpty() ? "추가 정보 없음" : detail.detail())), w - 22);
                detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - (height - top - 78)));
                g.enableScissor(x + 7, top + 32, x + w - 7, height - 37);
                int y = top + 34 - detailScroll;
                for (var line : lines) { dev.hoi.client.ui.UiText.text(g, font, line, x + 11, y, UiMenuStyle.TEXT); y += 13; }
                g.disableScissor();
            }
        }
        if (!choosing || view == null) UiAssets.draw(g, AgencyIntelLedger.source("main_screens_bottom", 0), 0, height - DiplomacyHeader.scaled(55, pane), pane, DiplomacyHeader.scaled(55, pane));
        super.extractRenderState(g, mx, my, delta);
        String relationTooltip = relationTooltipAt(mx, my);
        if (!relationTooltip.isEmpty()) dev.hoi.client.ui.tooltip.Tooltips.drawExact(g, font, relationTooltip, mx, my);
        if (intelLedger != null && detail == null && confirmation == null) {
            int cell = (pane - 16) / 4, tabs = DiplomacyHeader.tabs(top) + 25;
            String tooltip = my >= tabs && my < tabs + 30 && mx >= 8 && mx < 8 + 4 * cell ? view.report(AgencyIntelDomain.values()[(int)((mx - 8) / cell)]).tooltip() : intelLedger.detailAt(mx, my);
            if (!tooltip.isEmpty()) dev.hoi.client.ui.tooltip.Tooltips.drawWide(g, font, tooltip, mx, my);
        }
        if (view != null && !ledger && !choosing && detail == null && confirmation == null) {
            art.draw(g, view, pane, top);
            art.tooltip(g, font, view, pane, top, mx, my);
            var focus = header.focusAt(view, mx, my, pane, top);
            if (focus != null) dev.hoi.client.ui.tooltip.Tooltips.drawExact(g, font, focus.detail(), mx, my);
            var spirit = header.spiritAt(mx, my, pane, top);
            var party = header.partyAt(view,mx,my,pane,top);
            if(party!=null) dev.hoi.client.ui.tooltip.Tooltips.drawExact(g,font,party.detail(),mx,my);
            if (header.leaderAt(mx, my, pane, top))
                leaderTooltipScroll = dev.hoi.client.ui.politics.PoliticsTooltip.drawLeader(g, font, DiplomacyHeader.entry(view, "지도자"),
                        view.target(), dev.hoi.client.ui.country.CountryDisplays.name(view.target(), view.name()), mx, my, width, height, leaderTooltipScroll);
            else leaderTooltipScroll = 0;
            if (!java.util.Objects.equals(spirit, hoveredSpirit)) spiritTooltipScroll = 0;
            hoveredSpirit = spirit;
            if (spirit != null) spiritTooltipScroll = dev.hoi.client.ui.politics.PoliticsTooltip.draw(g, font,
                    new MenuView.Entry(spirit.name(), "", spirit.detail().substring("국가 정신\n".length()), spirit.progress(), spirit.icon()),
                    mx, my, width, height, spiritTooltipScroll);
        } else art.leave();
    }
    CountryView.Choice flagAt(double x,double y) {
        if(view==null||ledger||choosing||detail!=null||confirmation!=null
                ||x<9||x>=pane/2-3||y<bodyTop()||y>=height-33||view.relations().isEmpty())return null;
        int row=(int)(y-bodyTop()+scroll)/relationPitch();
        if(row<0||row>=view.relations().size())return null;
        var countries=view.relations().get(row).countries();
        int fw=DiplomacyHeader.scaled(47,pane),fh=DiplomacyHeader.scaled(32,pane);
        float ry=DiplomacyHeader.flagTop(bodyTop()+row*relationPitch()-scroll,pane);
        if (x < 9 + DiplomacyHeader.scaled(40, pane) || x >= 9 + DiplomacyHeader.scaled(200, pane)) return null;
        for(int i=0;i<countries.size();i++) {
            int fx=dev.hoi.client.ui.country.CountryFlagStack.left(i,countries.size(),9+DiplomacyHeader.scaled(40,pane),DiplomacyHeader.scaled(160,pane),fw);
            int visible=dev.hoi.client.ui.country.CountryFlagStack.visibleWidth(i,countries.size(),9+DiplomacyHeader.scaled(40,pane),DiplomacyHeader.scaled(160,pane),fw);
            if(x>=fx&&x<fx+visible&&y>=ry&&y<ry+fh)return countries.get(i);
        }
        return null;
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice) {
        if (intelLedger != null && detail == null && confirmation == null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            String folder = intelLedger.doctrineFolder(event.x(), event.y());
            if (!folder.isEmpty()) {
                dev.hoi.client.audio.AudioUiSounds.play("ui.click");
                dev.hoi.client.screen.doctrine.DoctrineScreen.open(this, folder, view.target());
                return true;
            }
            if (intelLedger.click(event.x(), event.y())) return true;
        }
        if (choosing && view != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (super.mouseClicked(event, twice)) return true;
            String tag = list.click(view, event.x(), event.y());
            if (tag != null && !tag.isEmpty()) select(tag);
            return tag != null;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && view != null && !ledger && !choosing && detail == null && confirmation == null
                && DiplomacyHeader.focusButton(event.x(), event.y(), pane, top)) {
            dev.hoi.client.audio.AudioUiSounds.play("ui.click");
            dev.hoi.client.network.NetworkScreens.openForeignFocus(this, view.target());
            return true;
        }
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            var country=flagAt(event.x(),event.y());
            if(country!=null){select(country.tag());return true;}
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        return event.button() == InputConstants.MOUSE_BUTTON_LEFT && (intelLedger != null && intelLedger.drag(event.x(), event.y()) || choosing && view != null && list.drag(view, event.y())) || super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return list.release() || intelLedger != null && intelLedger.release() || super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (view != null && !ledger && !choosing && detail == null && confirmation == null && art.scroll(view, pane, top, x, y, vertical)) return true;
        if (intelLedger != null && detail == null && confirmation == null && intelLedger.scroll(x, y, vertical)) return true;
        if (choosing && view != null && list.scroll(view, x, y, vertical)) return true;
        if (y < HudMenuBar.STATS_HEIGHT && x >= 40 && x < HudMenuBar.statsRight(width)) {
            hudScroll = HudMenuBar.scroll(width, view == null ? CountryHud.UNKNOWN : view.hud(), hudScroll, horizontal, vertical); return true;
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
            if (actionsVisible() && x >= pane / 2) actionScroll -= (int)(vertical * Math.max(14, DiplomacyHeader.scaled(29, pane)));
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
    @Override public void removed() { assets.close(); InputSidebarMovement.release(minecraft); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private void headerText(GuiGraphicsExtractor g, String value, float x, float center, float width, int align, int color) {
        var line = Component.literal(value).getVisualOrderText(); float scale = dev.hoi.client.ui.UiText.scale(font);
        dev.hoi.client.ui.UiText.marquee(g, font, line, x, dev.hoi.client.ui.UiText.centeredY(font, line, center - 10, 20, scale), width, scale, color, true, align);
    }
    private final class DiplomacyActionButton extends Button {
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { dev.hoi.client.audio.AudioUiSounds.play("ui.click"); }
        private final CountryView.Action definition;
        DiplomacyActionButton(CountryView.Action definition, int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.literal(definition.name()), b -> action.run(), DEFAULT_NARRATION);
            this.definition = definition;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            UiAssets.nineSlice(g, "diplomacy/" + (active ? "action" : "action_disabled"), getX(), getY(), getWidth(), getHeight(), 3, pane / 550.0);
            int iconSize = DiplomacyHeader.scaled(20, pane);
            int costWidth = definition.costs().stream().mapToInt(cost -> font.width(cost.amount()) + iconSize + 2).sum();
            int right = getX() + getWidth() - 4;
            if (definition.declined() || definition.human()) {
                float s = pane / 550f, iconX = getX() + getWidth() - 26 * s;
                UiAssets.drawExact(g, "commander/commands/accept_decline_icon/frame_" + (definition.human() ? 2 : 0), iconX, getY() + (getHeight() - 21 * s) / 2, 24 * s, 21 * s);
                right = Math.min(right, (int)Math.floor(iconX) - 1);
            }
            int costX = right - costWidth;
            for (var cost : definition.costs()) {
                UiAssets.draw(g, cost.icon().equals("command_power") ? "keywords/modifier_command_power_gain" : "politics/choice/political_power/frame_0", costX, getY() + (getHeight() - iconSize) / 2, iconSize, iconSize);
                dev.hoi.client.ui.UiText.centered(g, font, cost.amount(), costX + iconSize, getY(), font.width(cost.amount()), getHeight(), UiMenuStyle.TEXT);
                costX += iconSize + font.width(cost.amount()) + 2;
            }
            int textRight = definition.costs().isEmpty() ? right : right - costWidth - 2;
            dev.hoi.client.ui.UiText.scrollingCell(g, font, getMessage(), getX() + 4, getY(), Math.max(1, textRight - getX() - 4), getHeight(), active ? UiMenuStyle.TEXT : UiMenuStyle.MUTED, false);
        }
    }

}
