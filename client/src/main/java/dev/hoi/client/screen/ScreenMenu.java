package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiCountryDisplay;
import dev.hoi.client.ui.UiPoliticsLayout;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiTooltips;

import dev.hoi.protocol.ProtocolMenuTab;
import dev.hoi.protocol.ProtocolMenuView;
import dev.hoi.client.ui.UiPoliticalTooltip;
import dev.hoi.protocol.ProtocolCountryHud;
import dev.hoi.protocol.ProtocolMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class ScreenMenu extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final int TEXT = UiMenuStyle.TEXT, GOLD = 0xFFFFAA00, MUTED = UiMenuStyle.MUTED;
    private final Map<net.minecraft.client.gui.components.AbstractWidget, String> officerTooltips = new IdentityHashMap<>();
    private String hoveredOfficer = "";
    String hoveredOfficerTooltip() { return hoveredOfficer; }
    private Button capturedButton;
    private int[] captivityCounts;
    public void captivityCounts(int missing,int held){captivityCounts=new int[]{missing,held};}
    private void openCaptivity(boolean ours){var screen=new ScreenCommanderCaptivity(this,ours);ScreenDialogCoordinator.setContent(screen);screen.open();}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){if(manufacturerGroup!=null&&detail==null&&manufacturerMaximum>0&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&event.x()>=detailX()+detailWidth()-8&&event.x()<detailX()+detailWidth()&&event.y()>=top+59&&event.y()<Math.min(height-8,top+328)){manufacturerDragging=true;dragManufacturer(event.y());return true;}if(!selectionPreview&&selected==ProtocolMenuTab.OFFICER_CORPS&&capturedButton!=null&&capturedButton.active&&capturedButton.isMouseOver(event.x(),event.y())&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){AudioUiSounds.play("ui.click");openCaptivity(false);return true;}return super.mouseClicked(event,twice);}
    private ProtocolMenuView view;
    private boolean selectionPreview;
    private final Runnable researchOpen;
    private final java.util.function.Consumer<dev.hoi.protocol.ProtocolDialogPayloads.PoliticsOpen> officerOpen;
    private final String token = UUID.randomUUID().toString();
    private int refreshTicks;
    private ProtocolMenuTab selected = ProtocolMenuTab.POLITICS;
    private final Set<Integer> collapsed = new HashSet<>();
    private ProtocolMenuView.Entry detail;
    private String focusTarget;
    private String detailIcon;
    private String manufacturerGroup, manufacturerDetailId;
    private int manufacturerScroll,manufacturerMaximum;
    private boolean manufacturerDragging;
    private boolean manufacturerByName,manufacturerDescending,manufacturerSorted;
    private final Map<net.minecraft.client.gui.components.AbstractWidget,String> manufacturerTooltips=new LinkedHashMap<>();
    private int spiritScroll, tooltipScroll;
    private boolean balanceOpen;
    private ProtocolMenuView.Entry hoveredPolitics;
    private List<ProtocolMenuView.Entry> spirits = List.of();
    private record ScreenPolicyHover(UiPoliticsLayout.UiPoliticsBox box, ProtocolMenuView.Entry entry) {}
    private final List<ScreenPolicyHover> policies = new ArrayList<>();
    private int pane, top, scroll, total, detailScroll, tradeTab, hudScroll;
    private List<ProtocolMenuView.Entry> parties = List.of();
    private final List<int[]> partySpans = new ArrayList<>();
    private static final String[] POLITICS_IDS = {"government", "economic_laws", "military_laws", "social_laws", "development", "military_staff", "research_production"};
    private static final String[] POLITICS_NAMES = {"정부", "경제법", "군법", "사회법", "발전도", "군사 참모", "연구 & 생산"};
    public ScreenMenu(ProtocolMenuView view) { this(view, ProtocolMenuTab.POLITICS, HoiClient::open); }
    public static ScreenMenu forFocus(ProtocolMenuView view, String id) {
        var screen = new ScreenMenu(view);
        screen.focusTarget = id;
        return screen;
    }
    public ScreenMenu(ProtocolMenuView view, ProtocolMenuTab selected, Runnable researchOpen) {
        this(view,selected,researchOpen,null);
    }
    ScreenMenu(ProtocolMenuView view,ProtocolMenuTab selected,Runnable researchOpen,
               java.util.function.Consumer<dev.hoi.protocol.ProtocolDialogPayloads.PoliticsOpen> officerOpen) {
        super(Component.literal("HOI · 국가 메뉴"));
        this.view = Objects.requireNonNull(view); this.selected = selected; this.researchOpen = researchOpen;this.officerOpen=officerOpen;
    }
    public static ScreenMenu selection(ProtocolMenuView view) {
        var screen = new ScreenMenu(view);
        screen.selectionPreview = true;
        return screen;
    }
    public boolean selectionPreview() { return selectionPreview; }
    public int selectionLeft() { return (width - pane) / 2; }
    public int selectionTop() { return top; }
    public int selectionHeight() { return politicsLayout().contentTop() - top; }
    public String token() { return token; }
    public void update(ProtocolMenuView next) {
        if (selectionPreview || next.equals(view)) return;
        if (!view.country().equals(next.country())) { selected = ProtocolMenuTab.POLITICS; collapsed.clear(); manufacturerGroup = null; manufacturerDetailId = null; }
        var old = detail;
        boolean sameCountry = view.country().equals(next.country());
        view = next;captivityCounts=null;
        if (!sameCountry || next.balance() == null) balanceOpen = false;
        if (old != null) {
            if (!sameCountry) detail = null;
            else if (manufacturerDetailId != null) detail = next.manufacturers().stream().filter(m -> m.id().equals(manufacturerDetailId)).map(ProtocolMenuView.Manufacturer::entry).findFirst().orElse(null);
            else if (old.name().equals("국가 중점")) detail = politicsFocus();
            else detail = next.page(selected).sections().stream().filter(s -> s.icon().equals(detailIcon))
                    .flatMap(s -> s.entries().stream()).filter(e -> e.name().equals(old.name())).findFirst().orElse(null);
        }
        rebuildWidgets();
    }
    public ProtocolMenuTab selectedTab() { return selected; }
    public int panelWidth() { return pane; }
    @Override public int unitHudLeft(){return pane;}
    @Override public boolean allowsMovement() { return !selectionPreview && detail == null && manufacturerGroup == null; }
    @Override protected void init() {
        officerTooltips.clear();capturedButton=null;
        pane = UiPanelLayout.width(selected, width);
        top = UiMenuBar.height(width);
        if (selectionPreview) {
            pane = Math.min(width - 16, 726 / Math.max(1, minecraft.getWindow().getGuiScale()));
            while (pane > 160 && new UiPoliticsLayout(pane, 0).contentTop() > height - 16) pane--;
            top = (height - (new UiPoliticsLayout(pane, 0).contentTop())) / 2;
            preparePartyChart();
            spirits = view.page(ProtocolMenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 정신"))
                    .flatMap(s -> s.entries().stream()).filter(e -> !e.name().equals("없음")).toList();
            spiritScroll = Math.clamp(spiritScroll, 0, maximumSpiritScroll());
            addRenderableWidget(new UiMenuButton("×", selectionLeft() + pane - 25, top + 3, 19, 19, this::onClose));
            return;
        }
        UiMenuBar.buttons(width, view.country(), selected, tab -> {
            if (tab == ProtocolMenuTab.INTELLIGENCE) { HoiClient.openAgency(); return; }
            if (tab == ProtocolMenuTab.DECISIONS && ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolDecisionPayloads.Request.TYPE)) { HoiClient.openMenu(tab); return; }
            if (tab == ProtocolMenuTab.RESEARCH) { researchOpen.run(); return; }
            if (tab == ProtocolMenuTab.CONSTRUCTION && ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolConstructionPayloads.Request.TYPE)) { HoiClient.openConstruction(); return; }
            if (ScreenIndustry.supports(tab) && ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolIndustryPayloads.Request.TYPE)) { HoiClient.openIndustry(tab); return; }
            HoiClient.cancelOpen();
            selected = tab; balanceOpen = false; scroll = 0; collapsed.clear(); detail = null; manufacturerGroup = null; manufacturerDetailId = null; rebuildWidgets();
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new UiMenuButton("×", pane - 25, top + 3, 19, 19, this::onClose));
        if (focusTarget != null) {
            detail = view.page(ProtocolMenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 중점"))
                    .flatMap(s -> s.entries().stream()).filter(e -> e.detail().lines().findFirst().orElse("").equals(focusTarget))
                    .findFirst().orElse(null);
            detailIcon = "research"; detailScroll = 0; focusTarget = null;
        }
        if (selected == ProtocolMenuTab.POLITICS) {
            for (int i = 0; i < 2; i++) {
                var box = politicsLayout().status(i);
                var entry = politicsEntry(i == 0 ? "점령지" : "협력정부");
                var button = new ScreenInvisibleButton(entry.name(), box.x(), box.y(), box.width(), box.height(),
                        () -> { if(entry.name().equals("점령지"))dev.hoi.client.map.MapOccupationHud.open();else showDetail(entry, "politics"); });
                button.setTooltip(null);
                button.active = entry.progress() > 0;
                addRenderableWidget(button);
            }
            if (view.balance() != null) {
                var box = balanceButton();
                addRenderableWidget(new ScreenInvisibleButton("권력의 균형", box.x(), box.y(), box.width(), box.height(), () -> {balanceOpen = !balanceOpen; rebuildWidgets();}));
                if (balanceOpen) {
                    int x = balancePanelX(), w = balancePanelWidth();
                    addRenderableWidget(new UiMenuButton("×", x + w - 24, top + 3, 19, 19, () -> {balanceOpen = false; rebuildWidgets();}));
                }
            }
            preparePartyChart();
            spirits = view.page(ProtocolMenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 정신"))
                    .flatMap(s -> s.entries().stream()).filter(e -> !e.name().equals("없음")).toList();
            spiritScroll = Math.clamp(spiritScroll, 0, maximumSpiritScroll());
            var focusBox = politicsLayout().focusTitle();
            addRenderableWidget(new ScreenInvisibleButton("국가 중점", focusBox.x(), focusBox.y(), focusBox.width(), focusBox.height(),
                    () -> minecraft.gui.setScreen(new ScreenFocus())));
        }
        if (selected == ProtocolMenuTab.TRADE) {
            addRenderableWidget(new UiMenuButton("경제", "category/industry", tradeTab == 0, 8, top + 30, (pane - 20) / 2, 24,
                    () -> { tradeTab = 0; scroll = 0; rebuildWidgets(); }));
            addRenderableWidget(new UiMenuButton("무역", "menu/trade", tradeTab == 1, pane / 2, top + 30, (pane - 20) / 2, 24,
                    () -> { tradeTab = 1; scroll = 0; rebuildWidgets(); }));
        }
        layoutRows(false, null);
        scroll = Math.clamp(scroll, 0, Math.max(0, total - (contentBottom() - contentTop())));
        layoutRows(true, null);
        if (selected == ProtocolMenuTab.RESEARCH) addRenderableWidget(new UiMenuButton("연구 선택", 8, height - 27, 88, 20, researchOpen));
        if (manufacturerGroup != null && detail == null) layoutManufacturers(true, null);
        if (detail != null) {
            InputSidebarMovement.release(minecraft);
            int x = detailX(), w = detailWidth();
            addRenderableWidget(new UiMenuButton("×", x + w - 25, top + 3, 19, 19, () -> { detail = null; rebuildWidgets(); }));
        }
    }
    private int contentTop() { return selected == ProtocolMenuTab.POLITICS ? politicsLayout().contentTop()
            : top + (selected == ProtocolMenuTab.TRADE ? 62 : selected == ProtocolMenuTab.INTELLIGENCE ? 82 : 32); }
    private boolean hasFooterActions() { return selected == ProtocolMenuTab.RESEARCH || selected == ProtocolMenuTab.INTELLIGENCE; }
    private int contentBottom() { return height - (hasFooterActions() ? 36 : 8); }
    private int detailX() { return width - pane >= 240 ? pane + 6 : Math.max(8, (width - detailWidth()) / 2); }
    private int detailWidth() { return Math.min(manufacturerGroup!=null?268:310, width - (width - pane >= 240 ? pane + 14 : 16)); }
    private int rowHeight() { return selected == ProtocolMenuTab.PRODUCTION || selected == ProtocolMenuTab.RECRUITMENT ? 49 : 37; }
    private void layoutRows(boolean widgets, GuiGraphicsExtractor g) {
        if (selected == ProtocolMenuTab.POLITICS) { layoutPolitics(widgets, g); return; }
        if (selected == ProtocolMenuTab.OFFICER_CORPS) { layoutOfficers(widgets, g); return; }
        int y = contentTop() - scroll;
        var sections = view.page(selected).sections();
        for (int index = 0; index < sections.size(); index++) {
            if (selected == ProtocolMenuTab.TRADE && (tradeTab == 0 ? index != 0 : index == 0)) continue;
            var section = sections.get(index); final int sectionIndex = index;
            if (g != null) {
                UiMenuStyle.metal(g, 8, y, pane - 16, 21);
                UiAssets.draw(g, texture(section.icon()), 13, y + 3, 19, 15);
                dev.hoi.client.ui.UiText.text(g, font, trim(section.title(), pane - 68), 37, y + 6, TEXT);
                dev.hoi.client.ui.UiText.text(g, font, collapsed.contains(index) ? "+" : "−", pane - 25, y + 6, GOLD);
            }
            if (widgets && y >= contentTop() && y + 21 <= contentBottom()) {
                var header = new ScreenInvisibleButton(section.title(), 8, y, pane - 16, 21, () -> {
                    if (!collapsed.add(sectionIndex)) collapsed.remove(sectionIndex); rebuildWidgets();
                });
                header.active = detail == null; addRenderableWidget(header);
            }
            y += 25;
            if (!collapsed.contains(index)) for (var entry : section.entries()) {
                int h = rowHeight();
                if (g != null) {
                    g.fillGradient(10, y, pane - 10, y + h - 3, 0xFF303136, 0xFF1B1C21);
                    UiMenuStyle.bevel(g, 10, y, pane - 20, h - 3, false);
                    String icon = section.icon();
                    String texture = texture(icon);
                    UiMenuStyle.recess(g, 13, y + 3, 32, h - 9);
                    UiAssets.draw(g, texture, 16, y + 5, 26, h - 13);
                    dev.hoi.client.ui.UiText.cell(g, font, entry.name(), 51, y + 3, pane - 67, 14, TEXT, false);
                    dev.hoi.client.ui.UiText.cell(g, font, entry.value(), 51, y + 17, pane - 67, 14, GOLD, false);
                    if (entry.progress() >= 0) {
                        g.fill(50, y + h - 8, pane - 17, y + h - 5, 0xFF090C08);
                        g.fillGradient(50, y + h - 8, 50 + (int)((pane - 67) * entry.progress()), y + h - 5, 0xFFADB872, 0xFF52602F);
                    }
                }
                if (widgets && y >= contentTop() && y + h <= contentBottom()) {
                    var row = new ScreenInvisibleButton(entry.name() + " · " + entry.value(), 10, y, pane - 20, h - 3,
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
        var sections = view.page(ProtocolMenuTab.OFFICER_CORPS).sections();
        var commanders = sections.stream().filter(v -> v.icon().equals("high_command")).flatMap(v -> v.entries().stream()).toList();
        if (g != null) { UiMenuStyle.metal(g, 8, y, pane - 110, 36); UiMenuStyle.metal(g, pane - 98, y, 90, 36); }
        int count = Math.min(3, commanders.size()), cell = Math.min(31, (pane - 65) / 3);
        for (int i = 0; i < count; i++) {
            var entry = commanders.get(i); int bx = 11 + i * cell;
            if (g != null) {
                UiAssets.draw(g, entry.icon().isEmpty() ? "politics/high_command" : entry.icon(), bx, y + 3, cell - 2, 30);
            }
            officerControl(widgets, entry, "high_command", i, bx, y, cell - 2, 36);
        }
        var tactic = sections.stream().filter(v -> v.icon().equals("preferred_tactic")).flatMap(v -> v.entries().stream()).findFirst().orElse(null);
        if (tactic != null) {
            if (g != null) UiAssets.draw(g, "officer/preferred_tactic", pane - 65, y + 4, 48, 28);
            officerControl(widgets, tactic, "preferred_tactic", 0, pane - 65, y + 4, 48, 28);
        }
        y += 38;
        double commandScale=Math.min(.5,(pane-22)/418.0);int hqWidth=(int)Math.round(200*commandScale),captureWidth=(int)Math.round(218*commandScale),commandHeight=(int)Math.round(62*commandScale),captureX=pane-11-captureWidth;
        var captured=sections.stream().filter(v->v.icon().equals("captivity")).flatMap(v->v.entries().stream()).toList();String missing=captivityCounts!=null?Integer.toString(captivityCounts[0]):captured.isEmpty()?"—":captured.getFirst().value(),held=captivityCounts!=null?Integer.toString(captivityCounts[1]):captured.size()<2?"—":captured.get(1).value();
        String capturedTip="§e포획된 지휘관§r\n§a클릭§r하여 우리의 실종된 지휘관 확인\n§a우클릭§r하여 우리가 포획한 지휘관 확인\n\n우리의 실종된 지휘관: §e"+missing+"§r\n우리에게 포획된 지휘관: §e"+held+"§r\n\n"+dev.hoi.client.ui.UiCommandLabels.text("CAPTURED_GENERALS_DELAYED_OPERATIVES","전투 중에 지휘관이 포로로 잡힐 수 있습니다. 포획된 지휘관은 습격이나 첩보 작전을 통해 구출할 수 있습니다.");
        var deployed=sections.stream().filter(v->v.icon().equals("army_hq")).flatMap(v->v.entries().stream()).findFirst().orElse(null);int badgeX=11+hqWidth,badgeY=y+(int)Math.round(19*commandScale),badgeWidth=(int)Math.round(18*commandScale),badgeHeight=(int)Math.round(27*commandScale),counterX=11+(int)Math.round(220*commandScale),counterWidth=Math.max(1,captureX-counterX-1);
        if(g!=null){g.fill(8,y,pane-8,y+commandHeight+4,0xFF342E2B);UiAssets.draw(g,"officer/hq/hq_button",11,y+3,hqWidth,(int)Math.round(49*commandScale));UiAssets.draw(g,"officer/hq/captured_button",captureX,y+2,captureWidth,commandHeight);dev.hoi.client.ui.UiText.centered(g,font,"육군 사령부 편제",25,y+4,hqWidth-28,commandHeight-5,TEXT);if(deployed!=null){UiAssets.draw(g,"officer/hq/badge",badgeX,badgeY,badgeWidth,badgeHeight);dev.hoi.client.ui.UiText.leftSingleLine(g,font,Component.literal(deployed.value()),counterX,badgeY,counterWidth,badgeHeight,TEXT);}dev.hoi.client.ui.UiText.centered(g,font,"포획된 지휘관",captureX+(int)Math.round(69*commandScale),y+3,captureWidth-(int)Math.round(75*commandScale),commandHeight-7,TEXT);dev.hoi.client.ui.UiText.centered(g,font,held,captureX+18,y+19,10,9,GOLD,false);}
        if(widgets){int rowY=y;var headquarters=new UiMenuButton("육군 사령부 편제",11,rowY+2,hqWidth,commandHeight,()->{var screen=new ScreenArmyHeadquarters(this);ScreenDialogCoordinator.setContent(screen);screen.open();}).caption("").background("none");headquarters.setTooltip(null);officerTooltips.put(headquarters,dev.hoi.client.ui.UiCommandLabels.text("ARMY_HQ_TEMPLATES_TOGGLE_TOOLTIP","§G클릭§!하여 §Y육군 사령부 편제§! 목록을 전환합니다. 이 목록에서는 육군 사령부 편제를 검토 및 관리할 수 있습니다."));addRenderableWidget(headquarters);if(deployed!=null){var counter=new dev.hoi.client.ui.UiHoverRegion(deployed.name(),badgeX,badgeY,counterX+counterWidth-badgeX,badgeHeight);officerTooltips.put(counter,deployed.detail());counter.active=detail==null;addRenderableWidget(counter);}var button=new UiMenuButton("포획된 지휘관",captureX,rowY+2,captureWidth,commandHeight,()->openCaptivity(true)).caption("").background("none");button.setTooltip(null);capturedButton=button;officerTooltips.put(button,capturedTip);addRenderableWidget(button);}
        y += commandHeight+3;
        for (String branch : List.of("army", "navy", "air", "special")) {
            var section = sections.stream().filter(v -> v.icon().equals(branch)).findFirst().orElse(null);
            if (section == null) continue;
            String artBranch = branch.equals("special") ? "army" : branch;
            var entries = section.entries(); boolean special = branch.equals("special");
            int h = special ? 69 : branch.equals("air") ? 125 : 129;
            if (g != null) {
                UiMenuStyle.recess(g, 8, y, pane - 16, h);
                UiAssets.cover(g, "officer/" + artBranch + "/background", 9, y + 17, pane - 18, h - 18);
                UiAssets.cover(g, "officer/" + artBranch + "/header", 9, y - 8, pane - 18, h);
                UiAssets.draw(g, "officer/" + branch + "/banner", 8, y - 4, 49, 35);
                if(special)g.fill(8,y,pane-8,y+17,0xFF4A4A31);
                dev.hoi.client.ui.UiText.left(g, font, Component.literal(section.title()).withStyle(net.minecraft.ChatFormatting.BOLD), 60, y - 1, pane - 72, 17, TEXT);
            }
            if (!special && !entries.isEmpty()) {
                var chief = entries.getFirst();
                if (g != null) {
                    UiAssets.drawContent(g, chief.icon(), 54, y + 25, 32, 32);
                    if (!chief.value().equals("미지정")) dev.hoi.client.ui.UiText.centeredSingleLine(g, font, Component.literal(chief.value()), 92, y + 22, pane / 2 - 87, 38, TEXT);
                }
                officerControl(widgets, chief, branch, 0, 48, y + 19, Math.max(44, pane / 2 - 42), 44);
            }
            int doctrineIndex = special ? 0 : 1;
            if (entries.size() > doctrineIndex) {
                var doctrine = entries.get(doctrineIndex); int dx = special ? pane - 61 : pane * 340 / 550;
                if (g != null) {
                    UiAssets.draw(g, "officer/" + branch + "/doctrine_frame", dx, y + 19, 44, 44);
                    UiAssets.drawContent(g, doctrine.icon(), dx + 6, y + 25, 32, 32);
                }
                officerControl(widgets, doctrine, branch, -1, dx, y + 19, 44, 44);
            }
            int spirits = Math.max(0, entries.size() - 2);
            for (int i = 0; !special && i < spirits; i++) {
                var entry = entries.get(i + 2); int bw = (pane - 44) / 3;
                int bx = (pane - (spirits * bw + (spirits - 1) * 10)) / 2 + i * (bw + 10), by = y + 61;
                if (g != null) {
                    boolean selectedSpirit=entry.progress()>0;
                    int backgroundHeight=Math.max(1,(int)Math.round(bw*70.0/168));
                    UiAssets.draw(g,"officer/spirit/ui/background_"+(selectedSpirit?"enabled":"disabled"),bx,by+(28-backgroundHeight)/2,bw,backgroundHeight);
                    UiAssets.draw(g, entry.icon(), bx + 2, by - (selectedSpirit?5:9), bw - 4, 38);
                }
                officerControl(widgets, entry, branch, i + 1, bx, by, bw, 28);
            }
            var statistics = sections.stream().filter(v -> v.icon().equals(branch + "_stats"))
                    .flatMap(v -> v.entries().stream()).toList();
            int columns = special ? 5 : 7;
            int cellWidth = (pane - 20) / (special ? 7 : columns);
            int statsY = y + (special ? 33 : branch.equals("air") ? 95 : 96);
            for (int i = 0; i < statistics.size(); i++) {
                var statistic = statistics.get(i);
                int sx = 10 + i % columns * cellWidth, sy = statsY + i / columns * 16;
                if (g != null) {
                    UiAssets.cover(g, "officer/stats/background", sx, sy, cellWidth - 1, 15);
                    UiAssets.drawContent(g, statistic.icon(), sx + 1, sy + 1, 12, 13);
                    officerStatText(g, statistic.value(), sx + 13, sy, cellWidth - 14);
                }
                if (widgets && sy >= contentTop() && sy + 15 <= contentBottom()) {
                    var button = new dev.hoi.client.ui.UiHoverRegion(statistic.name(), sx, sy, cellWidth - 1, 15);
                    officerTooltips.put(button, statistic.detail());
                    button.active = detail == null;
                    addRenderableWidget(button);
                }
            }
            y += h + 4;
        }
        total = y - contentTop() + scroll;
    }
    private void officerControl(boolean widgets, ProtocolMenuView.Entry entry, String section, int slot, int x, int y, int w, int h) {
        if (!widgets || y < contentTop() || y + h > contentBottom()) return;
        var button = new ScreenInvisibleButton(entry.name() + " · " + entry.value(), x, y, w, h, () -> {
            if (slot >= 0 && (officerOpen!=null || ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolDialogPayloads.PoliticsOpen.TYPE))) {
                var request=new dev.hoi.protocol.ProtocolDialogPayloads.PoliticsOpen("officer_"+section,slot);
                if(officerOpen!=null)officerOpen.accept(request);else ClientPlayNetworking.send(request);
            } else if (slot < 0 && java.util.Set.of("army", "navy", "air", "special").contains(section)
                    && ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolDoctrinePayloads.Request.TYPE)) {
                String folder = switch (section) { case "army" -> "land"; case "navy" -> "naval"; case "special" -> "special_forces"; default -> "air"; };
                ScreenDoctrine.open(this, folder);
            } else showDetail(entry, section);
        });
        boolean vacant = entry.value().equals("미지정") || entry.value().isBlank();
        boolean advisor = section.equals("high_command") || slot == 0 && java.util.Set.of("army", "navy", "air").contains(section);
        String title = vacant ? entry.name() : entry.value();
        button.setTooltip(null);
        officerTooltips.put(button, slot < 0 || advisor && vacant ? entry.detail() : "§e" + title + "§r\n" + entry.detail());
        button.active = detail == null;
        addRenderableWidget(button);
    }
    static float officerStatScale(net.minecraft.client.gui.Font font, Component styled, int available) {
        return Math.min(.75f, available / (float)Math.max(1, font.width(styled)));
    }
    private void officerStatText(GuiGraphicsExtractor g, String value, int x, int y, int available) {
        var styled = UiTooltips.styled(Component.literal(value), false);
        float scale = officerStatScale(font, styled, available);
        g.pose().pushMatrix();
        g.pose().translate(x + available - font.width(styled) * scale, y + (15 - font.lineHeight * scale) / 2f);
        g.pose().scale(scale);
        g.text(font, styled.getVisualOrderText(), 0, 0, GOLD); g.pose().popMatrix();
    }
    private void officerText(GuiGraphicsExtractor g, String value, int x, int y, int available, int color) {
        float scale = dev.hoi.client.ui.UiText.scale(font);
        var styled = UiTooltips.styled(Component.literal(value), false);
        var lines = UiTooltips.split(font,styled, Math.max(1, (int)(available / scale)));
        if (lines.isEmpty()) return;
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale);
        g.text(font, lines.getFirst(), 0, 0, color); g.pose().popMatrix();
    }
    private void showDetail(ProtocolMenuView.Entry entry, String icon) {
        detail = entry; detailIcon = icon; detailScroll = 0; manufacturerDetailId = null; rebuildWidgets();
    }
    private String fontSafe(String text) { return dev.hoi.protocol.ProtocolTextBounds.ellipsize(text, 2000); }
    private ProtocolMenuView.Entry politicsFocus() {
        var entries = view.page(ProtocolMenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 중점")).flatMap(s -> s.entries().stream()).toList();
        var active = entries.stream().filter(e -> e.progress() >= 0 || e.value().contains("진행")).findFirst();
        String label = active.map(ProtocolMenuView.Entry::name).orElse("국가 중점 선택");
        return new ProtocolMenuView.Entry("국가 중점", label, fontSafe(entries.stream().map(e -> e.name() + " · " + e.value() + "\n" + e.detail()).collect(java.util.stream.Collectors.joining("\n\n"))), active.map(ProtocolMenuView.Entry::progress).orElse(-1.0), active.map(ProtocolMenuView.Entry::icon).orElse(""));
    }
    private void drawPoliticsBanner(GuiGraphicsExtractor g) {
        var layout = politicsLayout(); var leader = layout.leader();
        var person = politicsEntry("지도자");
        UiAssets.draw(g, person.icon().isEmpty() ? "politics/empty/leader" : person.icon(),
                leader.x() + leader.width() * 7 / 172, leader.y() + leader.height() * 6 / 258,
                leader.width() * 158 / 172, leader.height() * 210 / 258);
        drawPoliticalArt(g, "politics/leader_frame", leader);
        centeredPoliticsText(g, person.value(), new UiPoliticsLayout.UiPoliticsBox(leader.x() + 4, leader.y() + leader.height() * 218 / 258 + 2,
                leader.width() - 8, leader.height() * 34 / 258), TEXT);
        if (!selectionPreview) {
            drawPoliticalArt(g, "politics/focus_background", layout.focus());
            drawPoliticalArt(g, "politics/focus_select", layout.focusTitle());
            var focus = politicsFocus();
            if (focus.progress() >= 0) {
                var bar = layout.focusProgress();
                g.fill(bar.x(), bar.y(), bar.x() + bar.width(), bar.y() + bar.height(), 0xFF17101B);
                int filled = (int)Math.round(bar.width() * Math.clamp(focus.progress(), 0, 1));
                if (filled > 0) {
                    g.enableScissor(bar.x(), bar.y(), bar.x() + filled, bar.y() + bar.height());
                    g.blit(net.minecraft.resources.Identifier.fromNamespaceAndPath("hoi", "textures/gui/politics/focus_progress.png"),
                            bar.x(), bar.y(), bar.x() + bar.width(), bar.y() + bar.height(), 0, 1, 3f / 18, 15f / 18);
                    g.disableScissor();
                }
            }
            drawPoliticalArt(g, focus.icon().isEmpty() ? "politics/empty/focus" : focus.icon(),
                    focus.icon().isEmpty() ? layout.focusImage() : layout.focusArtwork());
            centeredPoliticsText(g, focus.value(), layout.focusTitle(), focus.value().equals("국가 중점 선택") ? 0xFFFFFFFF : GOLD);
        }
        politicsCell(g, "경제-정치 연합", layout.union());
        var ideology = politicsEntry("세부 이념"); var ideologyBox = layout.ideology();
        UiMenuStyle.recess(g, ideologyBox.x(), ideologyBox.y(), ideologyBox.width(), ideologyBox.height());
        String ideologyArt = ideology.icon().isEmpty() ? politicsEntry("이념").icon() : ideology.icon();
        drawPoliticalArt(g, ideologyArt.isEmpty() ? "politics/empty/ideology" : ideologyArt, ideologyBox);
        var strip = layout.spirits();
        UiMenuStyle.recess(g, strip.x(), strip.y(), strip.width(), strip.height());
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
        UiMenuStyle.recess(g, electionBox.x(), electionBox.y(), electionBox.width(), electionBox.height());
        String election = politicsEntry("다음 선거").value();
        if (election.equals("선거 없음")) centeredPoliticsText(g, election, electionBox, MUTED);
        else {
            int middle = electionBox.y() + electionBox.height() / 2;
            centeredPoliticsText(g, "다음 선거", new UiPoliticsLayout.UiPoliticsBox(electionBox.x(), middle - 12, electionBox.width(), 10), TEXT);
            centeredPoliticsText(g, election, new UiPoliticsLayout.UiPoliticsBox(electionBox.x(), middle + 2, electionBox.width(), 10), GOLD);
        }
        if (!selectionPreview && view.balance() != null) {
            var b = balanceButton(); var balance = view.balance();
            UiMenuStyle.recess(g, b.x(), b.y(), b.width(), b.height());
            UiAssets.draw(g, balance.value() < 0 ? balance.leftIcon() : balance.rightIcon(), b.x() + 2, b.y() + 2, Math.min(24, b.width()/2), b.height() - 4);
            officerText(g, Math.round(Math.abs(balance.value()) * 100) + "%", b.x() + b.width()/2, b.y() + (b.height() - 7)/2, b.width()/2 - 3, TEXT);
        }
        politicsCell(g, "경제 모델", layout.economy());
        politicsCell(g, "세력", layout.faction());
        for (int i = 0; !selectionPreview && i < 2; i++) {
            var entry = politicsEntry(i == 0 ? "점령지" : "협력정부");
            drawPoliticalArt(g, "politics/" + (i == 0 ? "occupied" : "collaboration") + (entry.progress() > 0 ? "_active" : "_inactive"), layout.status(i));
        }
        var chart = layout.partyChart();
        UiMenuStyle.recess(g, chart.x(), chart.y(), chart.width(), chart.height());
        for (int[] span : partySpans) g.fill(chart.x() + span[0], chart.y() + span[1], chart.x() + span[2], chart.y() + 1 + span[1], span[3]);
        var list = layout.parties();
        UiMenuStyle.recess(g, list.x(), list.y(), list.width(), list.height());
        double pitch = 11;
        float scale = dev.hoi.client.ui.UiText.scale(font);
        int index = 0;
        for (var party : parties) {
            int py = list.y() + 4 + (int)(index++ * pitch);
            if (py + 10 > list.y() + list.height()) break;
            int swatch = Math.max(2, (int)(pitch - 2));
            g.fill(list.x() + 4, py, list.x() + 4 + swatch, py + swatch, partyColor(party.icon()));
            String label = party.name() + " (" + dev.hoi.protocol.ProtocolNumberBounds.percent(party.progress(), 0) + ")";
            float rowScale = Math.min(scale, (list.width() - 21f) / Math.max(1, font.width(label)));
            g.pose().pushMatrix(); g.pose().translate(list.x() + 17, py); g.pose().scale(rowScale);
            g.text(font, label, 0, 0, TEXT);
            g.pose().popMatrix();
        }
        if (parties.isEmpty()) officerText(g, "정보 없음", list.x() + 5, list.y() + 5, list.width() - 10, MUTED);
        g.fill(8, layout.contentTop() - 5, pane - 8, layout.contentTop() - 4, 0xFF68686B);
    }
    private void drawPoliticalArt(GuiGraphicsExtractor g, String art, UiPoliticsLayout.UiPoliticsBox box) {
        UiAssets.draw(g, art, box.x(), box.y(), box.width(), box.height());
    }
    private void centeredPoliticsText(GuiGraphicsExtractor g, String text, UiPoliticsLayout.UiPoliticsBox box, int color) {
        if (selectionPreview) {
            float scale = Math.min(dev.hoi.client.ui.UiText.scale(font), (box.width() - 8f) / Math.max(1, font.width(text)));
            scale = Math.min(scale, box.height() / (float)font.lineHeight);
            g.pose().pushMatrix();
            g.pose().translate(box.x() + (box.width() - font.width(text) * scale) / 2, box.y() + (box.height() - font.lineHeight * scale) / 2);
            g.pose().scale(scale); g.text(font, text, 0, 0, color); g.pose().popMatrix();
        } else dev.hoi.client.ui.UiText.centered(g, font, text, box.x() + 4, box.y(), box.width() - 8, Math.max(10, box.height()), color);
    }
    private void politicsCell(GuiGraphicsExtractor g, String label, UiPoliticsLayout.UiPoliticsBox box) {
        politicsCell(g, label, box.x(), box.y(), box.width(), box.height());
    }
    private ProtocolMenuView.Entry politicsEntry(String name) {
        return view.page(ProtocolMenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 현황"))
                .flatMap(s -> s.entries().stream()).filter(e -> e.name().equals(name) || name.equals("세부 이념") && e.name().equals("이념")).findFirst()
                .orElse(new ProtocolMenuView.Entry(name, "정보 없음", ""));
    }
    private void politicsCell(GuiGraphicsExtractor g, String label, int x, int y, int w, int h) {
        UiMenuStyle.recess(g, x, y, w, h);
        String kind = switch (label) {
            case "경제 모델" -> "economy";
            case "세력" -> "faction";
            case "정치 체제" -> "government";
            default -> "union";
        };
        String icon = politicsEntry(label).icon();
        if (icon.isEmpty()) icon = "politics/empty/" + kind;
        int inset = icon.equals("politics/empty/union") ? 0 : 3;
        UiAssets.draw(g, icon, x + inset - (label.equals("경제-정치 연합") ? 1 : 0), y + inset, w - inset * 2, h - inset * 2);
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
        parties = view.page(ProtocolMenuTab.POLITICS).sections().stream().flatMap(s -> s.entries().stream())
                .filter(e -> (e.icon().startsWith("politics/ideology/") || e.icon().startsWith("politics/party/")) && e.progress() >= 0).toList();
        partySpans.clear();
        int size = politicsLayout().partyChart().width();
        for (int y = 0; y < size; y++) {
            int start = 0, previous = 0;
            for (int x = 0; x <= size; x++) {
                int color = x == size ? 0 : partyPixel(x, y, size);
                if (color != previous) {
                    if (previous != 0) partySpans.add(new int[]{start, y, x, previous});
                    start = x; previous = color;
                }
            }
        }
    }
    private int partyPixel(int x, int y, int size) {
        int samples = 0, red = 0, green = 0, blue = 0;
        double radius = size / 2.0 - 3;
        for (int sy = 0; sy < 4; sy++) for (int sx = 0; sx < 4; sx++) {
            double dx = x + (sx + .5) / 4 - size / 2.0, dy = y + (sy + .5) / 4 - size / 2.0;
            double distance = Math.hypot(dx, dy);
            if (distance > radius) continue;
            int color = 0xFF34363A;
            if (distance > radius - .8) color = dy < 0 ? 0xFFA5ABB0 : 0xFF454B51;
            else {
                double angle = (Math.atan2(dy, dx) + Math.PI * 2.5) % (Math.PI * 2) / (Math.PI * 2), sum = 0;
                for (var party : parties) {
                    sum += party.progress();
                    if (angle < sum) { color = partyColor(party.icon()); break; }
                }
            }
            samples++; red += (color >> 16) & 255; green += (color >> 8) & 255; blue += color & 255;
        }
        return samples == 0 ? 0 : (samples * 255 / 16 << 24) | (red / samples << 16) | (green / samples << 8) | blue / samples;
    }
    private void layoutPolitics(boolean widgets, GuiGraphicsExtractor g) {
        if (widgets) policies.clear();
        int row = Math.clamp((contentBottom() - contentTop()) / 7, 37, 68);
        int y = contentTop() - scroll;
        for (int i = 0; i < POLITICS_IDS.length; i++) {
            final int index = i;
            var section = view.page(ProtocolMenuTab.POLITICS).sections().stream().filter(s -> s.icon().equals(POLITICS_IDS[index])).findFirst()
                    .orElse(new ProtocolMenuView.Section(POLITICS_NAMES[i], POLITICS_IDS[i], List.of()));
            if (g != null) {
                UiMenuStyle.metal(g, 9, y, pane - 22, 15);
                UiAssets.draw(g, "politics/header/" + new int[]{2, 1, 4, 5, 6, 7, 8}[i], 13, y + 1, 21, 13);
                UiMenuStyle.heading(g, font, section.title(), 39, y + 2, pane - 50, TEXT);
            }
            int columns = 6, cell = (pane - 36) / columns, iconSize = Math.min(cell - 4, row - 21);
            var entries = section.entries();
            int count = Math.max(1, entries.size());
            for (int j = 0; j < count; j++) {
                int x = 16 + Math.max(0, columns - count) * cell / 2 + (j % columns) * cell, cy = y + 17 + (j / columns) * (row - 17);
                var entry = entries.isEmpty() ? new ProtocolMenuView.Entry(section.title(), "정보 없음", "서버가 이 구역의 상태를 제공하지 않습니다.") : entries.get(j);
                if (g != null) {
                    String icon = entry.icon().isEmpty() ? "menu/politics" : entry.icon();
                    UiAssets.draw(g, icon, x, cy, cell - 4, iconSize);
                    if (!icon.equals("politics/vacant") && (entry.value().equals("정보 없음") || entry.value().equals("미지정")))
                        g.fill(x, cy + iconSize - 2, x + cell - 4, cy + iconSize, 0xA0404246);
                }
                if (widgets && cy >= contentTop() && cy + iconSize <= contentBottom()) {
                    final int position = j;
                    var button = new ScreenInvisibleButton(entry.name() + " · " + entry.value(), x, cy, cell - 4, iconSize, () -> {
                        if (section.icon().equals("research_production") && position < 4) {
                            manufacturerGroup = List.of("armor", "navy", "air", "materiel").get(position);
                            manufacturerScroll = 0; detail = null; manufacturerDetailId = null; rebuildWidgets();
                        } else if (index < 6 && ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolDialogPayloads.PoliticsOpen.TYPE)) {
                            ClientPlayNetworking.send(new dev.hoi.protocol.ProtocolDialogPayloads.PoliticsOpen(section.icon(), position));
                        } else showDetail(entry, section.icon());
                    });
                    button.setTooltip(null);
                    if (!entry.value().equals("공석") && !entry.value().equals("미지정") && !entry.icon().equals("politics/vacant"))
                        policies.add(new ScreenPolicyHover(new UiPoliticsLayout.UiPoliticsBox(x, cy, cell - 4, iconSize), entry));
                    button.active = true; addRenderableWidget(button);
                }
            }
            y += row + (count - 1) / columns * (row - 17);
        }
        total = y - contentTop() + scroll;
    }
    private void requestRefresh() {
        if (ClientPlayNetworking.canSend(ProtocolMenuPayloads.Refresh.TYPE)) ClientPlayNetworking.send(new ProtocolMenuPayloads.Refresh(token));
    }
    @Override public void tick() {
        if (!selectionPreview && ++refreshTicks % 40 == 0) requestRefresh();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (selectionPreview) {
            g.pose().pushMatrix();
            g.pose().translate(selectionLeft(), 0);
            UiMenuStyle.panel(g, 0, top, pane, selectionHeight());
            UiMenuStyle.heading(g, font, trim(UiCountryDisplay.name(view.country(), view.countryName()), pane - 44), 10, top + 9, pane - 44, TEXT);
            drawPoliticsBanner(g);
            g.pose().popMatrix();
            super.extractRenderState(g, mx, my, delta);
            drawPoliticsTooltip(g, mx - selectionLeft(), my, mx);
            return;
        }
        UiMenuBar.draw(g, width, view.hud(), mx, my, hudScroll);
        UiMenuStyle.panel(g, 0, top, pane, height - top);
        String heading=trim(selected == ProtocolMenuTab.POLITICS ? "정치" : selected.label(),pane-44);int headingX=10;

        UiMenuStyle.heading(g, font, selected==ProtocolMenuTab.OFFICER_CORPS?"§l"+heading:heading, headingX, top + 9, pane - 44, TEXT);
        if (selected == ProtocolMenuTab.POLITICS) drawPoliticsBanner(g);
        if (selected == ProtocolMenuTab.INTELLIGENCE) {
            UiMenuStyle.recess(g, 8, top + 30, pane - 16, 46);
            if (!view.country().equals("KOR") || !UiAssets.draw(g, "country/kor/intelligence", 18, top + 33, 42, 40))
                UiAssets.draw(g, "menu/intelligence", 18, top + 33, 42, 40);
            dev.hoi.client.ui.UiText.text(g, font, trim("정보기관", pane - 86), 76, top + 43, TEXT);
            dev.hoi.client.ui.UiText.text(g, font, trim(UiCountryDisplay.name(view.country(), view.countryName()), pane - 86), 76, top + 58, MUTED);
        }
        UiMenuStyle.recess(g, 5, contentTop() - 3, pane - 10, Math.max(6, contentBottom() - contentTop() + 5));
        g.enableScissor(6, contentTop(), pane - 6, contentBottom()); layoutRows(false, g); g.disableScissor();
        int visible = contentBottom() - contentTop();
        if (total > visible && visible > 0) {
            int thumb = Math.max(10, visible * visible / total);
            int y = contentTop() + scroll * (visible - thumb) / Math.max(1, total - visible);
            g.fill(pane - 7, contentTop(), pane - 3, contentBottom(), 0xFF08090B);
            UiMenuStyle.metal(g, pane - 7, y, 4, thumb);
        }
        if (hasFooterActions()) UiMenuStyle.metal(g, 0, height - 33, pane, 33);
        if (manufacturerGroup != null) layoutManufacturers(false, g);
        if (detail != null) drawDetail(g);
        if (balanceOpen && selected == ProtocolMenuTab.POLITICS && view.balance() != null) drawBalance(g, mx, my);
        super.extractRenderState(g, mx, my, delta);
        drawPoliticsTooltip(g, mx, my, mx);
        if(manufacturerGroup!=null&&detail==null)manufacturerTooltips.entrySet().stream().filter(e->UiTooltips.contains(e.getKey(),mx,my)).findFirst().ifPresent(e->UiTooltips.drawSourceScrollable(g,font,e.getValue(),mx,my,width,height,0));
        if (selected == ProtocolMenuTab.OFFICER_CORPS && detail == null) {
            String text = officerTooltips.entrySet().stream().filter(entry -> entry.getKey().isMouseOver(mx, my))
                    .map(Map.Entry::getValue).findFirst().orElse("");
            if (!text.equals(hoveredOfficer)) { hoveredOfficer = text; tooltipScroll = 0; }
            if (!text.isEmpty()) tooltipScroll = UiTooltips.drawSourceScrollable(g, font, text, mx, my, width, height, tooltipScroll).scroll();
        }
    }
    private void drawPoliticsTooltip(GuiGraphicsExtractor g, int mx, int my, int screenX) {
        var hovered = politicsHover(mx, my);
        if (!Objects.equals(hovered, hoveredPolitics)) tooltipScroll = 0;
        hoveredPolitics = hovered;
        if (hovered != null && hovered.name().equals("지도자")) {
            tooltipScroll = UiPoliticalTooltip.drawLeader(g, font, hovered, view.country(), UiCountryDisplay.name(view.country(), view.countryName()), screenX, my, width, height, tooltipScroll);
        } else if (hovered != null && hovered.name().equals("세력") && hovered.value().equals("미가입")) {
            tooltipScroll = UiPoliticalTooltip.drawCountryDetail(g, font, hovered, view.country(), screenX, my, width, height, tooltipScroll);
        } else if (hovered != null && (hovered.name().equals("점령지") || hovered.name().equals("협력정부") || hovered.icon().startsWith("politics/party/") || hovered.icon().startsWith("politics/ideology/"))) {
            UiTooltips.drawExact(g, font, hovered.detail(), screenX, my);
        } else if (hovered != null && hovered.detail().contains("[image:")) {
            tooltipScroll = UiPoliticalTooltip.draw(g, font, hovered, screenX, my, width, height, tooltipScroll);
        } else if (hovered != null) {
            boolean party = hovered.icon().startsWith("politics/party/") || hovered.icon().startsWith("politics/ideology/");
            String title = party || hovered.value().isBlank() ? hovered.name() : hovered.value();
            var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
            lines.add(Component.literal(title).withColor(UiTooltips.GOLD).getVisualOrderText());
            if (party) lines.add(Component.literal(hovered.value()).getVisualOrderText());
            if (!hovered.detail().isBlank()) lines.addAll(spirits.contains(hovered) ? UiTooltips.sourceLines(font, hovered.detail(), width) : UiTooltips.lines(font, hovered.detail(), width));
            lines=new ArrayList<>(UiTooltips.collapseBlankLines(lines));
            int count = Math.max(1, (height - 48) / (font.lineHeight + 1));
            tooltipScroll = Math.clamp(tooltipScroll, 0, Math.max(0, lines.size() - count));
            g.setTooltipForNextFrame(font, lines.subList(tooltipScroll, Math.min(lines.size(), tooltipScroll + count)), screenX, my);
        }
    }
    private UiPoliticsLayout.UiPoliticsBox balanceButton() {
        var layout = politicsLayout(); var first = layout.status(0); var last = layout.status(1);
        return new UiPoliticsLayout.UiPoliticsBox(first.x(), layout.summaryY(), last.x() + last.width() - first.x(), first.y() - layout.summaryY() - 2);
    }
    private int balancePanelWidth() { return Math.min(UiPanelLayout.width(ProtocolMenuTab.INTELLIGENCE, width), width - 8); }
    private int balancePanelX() { return Math.min(pane + 3, width - balancePanelWidth()); }
    private void drawBalance(GuiGraphicsExtractor g, int mx, int my) {
        var balance = view.balance(); int x = balancePanelX(), w = balancePanelWidth(), y = top + 30;
        UiMenuStyle.panel(g, x, top, w, Math.min(height - top, 300));
        UiMenuStyle.heading(g, font, "권력의 균형", x + 9, top + 9, w - 44, TEXT);
        officerText(g, balance.name(), x + 10, y + 4, w - 20, TEXT);
        UiAssets.draw(g, balance.leftIcon(), x + 8, y + 22, 36, 36);
        UiAssets.draw(g, balance.rightIcon(), x + w - 44, y + 22, 36, 36);
        int bx = x + 48, bw = w - 96, by = y + 43;
        UiAssets.draw(g, "bop/bar_bg", bx, by, bw, 12);
        int center = bx + bw / 2, length = (int)Math.round(Math.abs(balance.value()) * bw / 2);
        g.enableScissor(balance.value() < 0 ? center - length : center, by, balance.value() < 0 ? center : center + length, by + 12);
        UiAssets.draw(g, balance.value() < 0 ? "bop/bar_orange_light" : "bop/bar_blue_light", bx, by, bw, 12);
        g.disableScissor();
        UiAssets.draw(g, "bop/bar_frame", bx - 2, by - 2, bw + 4, 16);
        for (var range : balance.ranges()) {
            int px = bx + (int)Math.round((range.progress() + 1) * bw / 2);
            UiAssets.draw(g, balance.value() >= range.progress() ? "bop/bulb_active" : "bop/bulb", px - 4, by - 11, 8, 10);
        }
        var active = balance.ranges().stream().filter(r -> r.progress() <= balance.value()).reduce((a,b) -> b).orElse(null);
        officerText(g, active == null ? "" : active.name(), x + 12, y + 66, w - 24, TEXT);
        if (mx >= bx && mx <= bx + bw && my >= by - 14 && my <= by + 16) {
            var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
            lines.add(Component.literal(balance.left() + " ↔ " + balance.right()).getVisualOrderText());
            for (var range : balance.ranges()) lines.add(Component.literal(range.name() + " · " + range.value()).getVisualOrderText());
            g.setTooltipForNextFrame(font, lines, mx, my);
        }
    }
    UiPoliticsLayout politicsLayout() { return new UiPoliticsLayout(pane, top); }
    ProtocolCountryHud hud() { return view.hud(); }
    int spiritPitch() { return Math.max(18, politicsLayout().spirits().height() - 5); }
    private int maximumSpiritScroll() { return Math.max(0, spirits.size() * spiritPitch() - Math.max(0, politicsLayout().spirits().width() - 8)); }
    private int spiritStart() { return politicsLayout().spirits().x() + 4; }
    ProtocolMenuView.Entry politicsHover(double x, double y) {
        if (selected != ProtocolMenuTab.POLITICS || detail != null || manufacturerGroup != null) return null;
        var layout = politicsLayout();
        if (layout.leader().contains(x, y)) {
            var leader = politicsEntry("지도자");
            return leader;
        }
        var strip = layout.spirits();
        if (x >= strip.x() + 4 && x < strip.x() + strip.width() - 4 && y >= strip.y() + 2 && y < strip.y() + strip.height() - 4) {
            int position = (int)x - spiritStart() + spiritScroll, index = position / spiritPitch();
            if (position >= 0 && position % spiritPitch() >= 1 && position % spiritPitch() < spiritPitch() - 2 && index < spirits.size()) return spirits.get(index);
        }
        if (layout.ideology().contains(x, y)) return politicsEntry("세부 이념");
        if (layout.economy().contains(x, y)) return politicsEntry("경제 모델");
        if (layout.government().contains(x, y)) return politicsEntry("정치 체제");
        if (!selectionPreview) for (int i=0;i<2;i++) if (layout.status(i).contains(x,y)) return politicsEntry(i==0?"점령지":"협력정부");
        var chart=layout.partyChart();
        var party=ScreenPartyChart.at(parties,x-chart.x()-chart.width()/2.0,y-chart.y()-chart.height()/2.0,chart.width()/2.0-3.8);
        if(party!=null)return party;
        var partyList = layout.parties();
        if (partyList.contains(x, y)) {
            int index = (int)(y - partyList.y() - 4) / 11;
            if (index >= 0 && index < parties.size()) return parties.get(index);
        }
        if (layout.faction().contains(x, y)) return politicsEntry("세력");
        if (!selectionPreview && y >= contentTop() && y < contentBottom())
            for (var policy : policies) if (policy.box().contains(x, y)) return policy.entry();
        return null;
    }
    private void drawDetail(GuiGraphicsExtractor g) {
        int x = detailX(), w = detailWidth();
        g.nextStratum();
        UiMenuStyle.panel(g, x, top, w, height - top - 34);
        UiMenuStyle.heading(g, font, trim(detail.name(), w - 48), x + 10, top + 9, w - 44, TEXT);
        UiMenuStyle.recess(g, x + 8, top + 31, w - 16, 38);
        UiAssets.draw(g, detail.icon().isEmpty() ? texture(detailIcon) : detail.icon(), x + 12, top + 36, 34, 26);
        dev.hoi.client.ui.UiText.text(g, font, trim(detail.value(), w - 64), x + 55, top + 44, TEXT);
        g.horizontalLine(x + 10, x + w - 10, top + 74, 0xFF4E4F52);
        var lines = UiTooltips.split(font,Component.literal(detail.detail().isBlank() ? "추가 정보 없음" : detail.detail()), w - 28);
        int visible = Math.max(1, height - top - 128);
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - visible));
        g.enableScissor(x + 10, top + 84, x + w - 10, height - 44);
        int y = top + 84 - detailScroll;
        for (var line : lines) { dev.hoi.client.ui.UiText.text(g, font, line, x + 14, y, TEXT); y += 13; }
        g.disableScissor();
    }
    private void dragManufacturer(double y){int end=Math.min(height-8,top+328)-4;manufacturerScroll=(int)(Math.clamp((y-top-59)/Math.max(1,end-top-59),0,1)*manufacturerMaximum);rebuildWidgets();}
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){if(manufacturerDragging){dragManufacturer(event.y());return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){if(manufacturerDragging){manufacturerDragging=false;return true;}return super.mouseReleased(event);}
    private void layoutManufacturers(boolean widgets, GuiGraphicsExtractor g) {
        if(widgets)manufacturerTooltips.clear();
        int x = detailX(), w = detailWidth(), bottom = Math.min(height-8,top+328);
        var groups = List.of("points", "materiel", "armor", "air", "navy");
        var names = List.of("특성 점수 보유", "군수품", "전차", "비행기", "함선");
        var icons = List.of("mio/ui/gfx_refitting/frame_0", "mio/ui/gfx_inf_art_checkbox/frame_0", "mio/ui/gfx_armour_checkbox/frame_0", "mio/ui/gfx_air_checkbox/frame_0", "mio/ui/gfx_screen_ships_checkbox/frame_0");
        Comparator<ProtocolMenuView.Manufacturer> order=manufacturerByName?Comparator.comparing(m->m.entry().name()):Comparator.comparingInt(ProtocolMenuView.Manufacturer::size);
        if(manufacturerDescending)order=order.reversed();
        var filtered=view.manufacturers().stream().filter(m -> manufacturerGroup.equals("points") ? m.points()>0 : m.groups().contains(manufacturerGroup));
        var rows=(manufacturerSorted?filtered.sorted(order):filtered).toList();
        int available = Math.max(1, bottom - top - 62);
        manufacturerMaximum=Math.max(0,rows.size()*49-available);manufacturerScroll = Math.clamp(manufacturerScroll, 0, manufacturerMaximum);
        if (g != null) {
            g.nextStratum(); UiMenuStyle.panel(g, x, top, w, bottom - top);
            dev.hoi.client.ui.UiText.centered(g,font,"군수산업체",x+10,top+5,w-35,20,TEXT,false);
        }
        if (widgets) {
            addRenderableWidget(new UiMenuButton("규모",x+8,top+32,24,18,()->{manufacturerDescending=manufacturerSorted&&!manufacturerByName&&!manufacturerDescending;manufacturerByName=false;manufacturerSorted=true;rebuildWidgets();}).caption("").background(ScreenOrganization.art("sort_skill")));
            addRenderableWidget(new UiMenuButton("이름",x+34,top+32,45,18,()->{manufacturerDescending=manufacturerSorted&&manufacturerByName&&!manufacturerDescending;manufacturerByName=true;manufacturerSorted=true;rebuildWidgets();}));
            addRenderableWidget(new UiMenuButton("×", x + w - 25, top + 3, 19, 19, () -> { manufacturerGroup = null; rebuildWidgets(); }));
            for (int i = 0; i < groups.size(); i++) {
                final String group = groups.get(i);
                String icon=i>0&&manufacturerGroup.equals(group)?icons.get(i).replace("frame_0","frame_1"):icons.get(i);
                addRenderableWidget(new UiMenuButton(names.get(i), x+w-122+i*23,top+30,23,18,()->{manufacturerGroup=group;manufacturerScroll=0;rebuildWidgets();}).caption("").background(icon).sound("ui.checkbox"));
            }
        }
        if (g != null) g.enableScissor(x + 8, top + 59, x + w - 8, bottom - 4);
        int y = top + 59 - manufacturerScroll;
        for (var manufacturer : rows) {
            var entry = manufacturer.entry();
            if (g != null) {
                dev.hoi.client.ui.UiOrganizationRow.draw(g,entry.icon(),manufacturer.size(),entry.name(),manufacturer.type(),manufacturer.equipment(),x+8,y,w-16);
            }
            if (widgets && y >= top + 59 && y + 45 <= bottom - 4) {
                var hover=new dev.hoi.client.ui.UiHoverRegion(entry.name(),x+8,y,w-16,45);manufacturerTooltips.put(hover,entry.detail());
                addRenderableWidget(new UiMenuButton("세부 사항", x + w - 67, y + 31, 54, 16, () -> dev.hoi.client.map.MapCommanderHud.open(new dev.hoi.protocol.ProtocolCommanderHud.Open(dev.hoi.protocol.ProtocolCommanderHud.Mode.ORGANIZATION,manufacturer.id(),List.of()))).background(ScreenOrganization.art("leader_selection_button")));addRenderableWidget(hover);
            }
            y += 49;
        }
        if (g != null) {

            g.disableScissor();
            if(manufacturerMaximum>0){int thumb=Math.max(12,available*available/(available+manufacturerMaximum));int sy=top+59+(available-thumb)*manufacturerScroll/manufacturerMaximum;g.fill(x+w-7,top+59,x+w-3,bottom-4,0xFF181B20);g.fill(x+w-7,sy,x+w-3,sy+thumb,0xFF89929C);}
        }
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        final double hoverX = x, hoverY = y;
        if (selected == ProtocolMenuTab.OFFICER_CORPS && officerTooltips.keySet().stream().anyMatch(widget -> widget.isMouseOver(hoverX, hoverY))
                && com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)) {
            tooltipScroll -= (int)(vertical * 30); return true;
        }
        if (selectionPreview) {
            x -= selectionLeft();
            if (politicsLayout().spirits().contains(x, y)) {
                spiritScroll = Math.clamp(spiritScroll - (int)((horizontal != 0 ? horizontal : vertical) * spiritPitch()), 0, maximumSpiritScroll());
            } else if (politicsHover(x, y) != null) tooltipScroll -= (int)(vertical * 3);
            return true;
        }
        if (selected == ProtocolMenuTab.POLITICS && detail == null && manufacturerGroup == null && politicsLayout().parties().contains(x, y)) {
            return true;
        }
        if (politicsHover(x, y) != null && (com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT))) {
            tooltipScroll -= (int)(vertical * 3); return true;
        }
        if (selected == ProtocolMenuTab.POLITICS && detail == null && manufacturerGroup == null
                && politicsLayout().spirits().contains(x, y)) {
            spiritScroll = Math.clamp(spiritScroll - (int)((horizontal != 0 ? horizontal : vertical) * spiritPitch()), 0, maximumSpiritScroll());
            return true;
        }
        if (x >= 40 && x < UiMenuBar.statsRight(width) && y >= 0 && y < UiMenuBar.STATS_HEIGHT) {
            hudScroll = UiMenuBar.scroll(width, view.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        if (detail != null) { detailScroll -= (int)(vertical * 26); return true; }
        if (manufacturerGroup != null && x >= detailX()) { manufacturerScroll -= (int)(vertical * 35); rebuildWidgets(); return true; }
        if (x < pane && y >= contentTop()) { scroll -= (int)(vertical * 35); rebuildWidgets(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (detail != null && event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { detail = null; rebuildWidgets(); return true; }
        if (manufacturerGroup != null && event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { manufacturerGroup = null; rebuildWidgets(); return true; }
        if (allowsMovement() && InputSidebarMovement.consumes(minecraft, event)) return true;
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP) {
            int amount = (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN ? 1 : -1) * 100;
            if (detail != null) detailScroll += amount; else { scroll += amount; rebuildWidgets(); } return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (allowsMovement() && InputSidebarMovement.consumes(minecraft, event)) return true;
        return super.keyReleased(event);
    }
    @Override public void removed() { InputSidebarMovement.release(minecraft); if (!ScreenDialogCoordinator.suspending()) HoiClient.cancelOpen(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private String trim(String text, int max) { return dev.hoi.client.ui.UiText.ellipsize(font, text, max); }
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
    private final class ScreenInvisibleButton extends Button {
        @Override protected void handleCursor(GuiGraphicsExtractor g) {
            String label = getMessage().getString();
            if (active || !(label.equals("점령지") || label.equals("협력정부"))) super.handleCursor(g);
        }
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) {
            AudioUiSounds.play(getMessage().getString().equals("국가 중점") ? "ui.focus.select" : "ui.click");
        }
        ScreenInvisibleButton(String label, int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
            setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(label)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        }
    }
}
