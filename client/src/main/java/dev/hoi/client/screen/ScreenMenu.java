package dev.hoi.client.screen;

import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.country.CountryDisplays;
import dev.hoi.client.ui.politics.PoliticsLayout;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.menu.MenuTab;
import dev.hoi.protocol.menu.MenuView;
import dev.hoi.client.ui.politics.PoliticsTooltip;
import dev.hoi.protocol.menu.MenuPayloads;
import dev.hoi.client.screen.army.ArmyHeadquarters;
import dev.hoi.client.screen.commander.CommanderCaptivity;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.doctrine.DoctrineScreen;
import dev.hoi.client.screen.focus.FocusScreen;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.client.ui.industry.IndustryOrganizationList;
import dev.hoi.client.ui.industry.IndustryOrganizationRow;
import dev.hoi.client.screen.industry.IndustryScreen;
import dev.hoi.client.screen.politics.PoliticsPartyChart;
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
    private void openCaptivity(boolean ours){var screen=new CommanderCaptivity(this,ours);DialogCoordinator.setContent(screen);screen.open();}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){if(manufacturerGroup!=null&&detail==null&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&manufacturerScrollbar().contains(event.x(),event.y())){if(manufacturerScrollbar().arrow(event.y())){manufacturerScroll=manufacturerScrollbar().step(event.y(),manufacturerScroll,IndustryOrganizationList.ENTRY_HEIGHT);rebuildWidgets();}else{manufacturerDragging=true;dragManufacturer(event.y());}return true;}if(!selectionPreview&&selected==MenuTab.OFFICER_CORPS&&capturedButton!=null&&capturedButton.active&&capturedButton.isMouseOver(event.x(),event.y())&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){AudioUiSounds.play("ui.click");openCaptivity(false);return true;}return super.mouseClicked(event,twice);}
    private MenuView view;
    private boolean selectionPreview;
    private final Runnable researchOpen;
    private final java.util.function.Consumer<dev.hoi.protocol.dialog.DialogPayloads.PoliticsOpen> officerOpen;
    private final String token = UUID.randomUUID().toString();
    private int refreshTicks;
    private MenuTab selected = MenuTab.POLITICS;
    private final Set<Integer> collapsed = new HashSet<>();
    private MenuView.Entry detail;
    private String focusTarget;
    private String detailIcon;
    private String manufacturerGroup, manufacturerDetailId;
    private int manufacturerScroll,manufacturerMaximum;
    private boolean manufacturerDragging;
    private boolean manufacturerByName,manufacturerDescending,manufacturerSorted;
    private final Map<net.minecraft.client.gui.components.AbstractWidget,String> manufacturerTooltips=new LinkedHashMap<>();
    private int spiritScroll, tooltipScroll;
    private boolean balanceOpen;
    private MenuView.Entry hoveredPolitics;
    private List<MenuView.Entry> spirits = List.of();
    private record ScreenPolicyHover(PoliticsLayout.PoliticsBox box, MenuView.Entry entry) {}
    private final List<ScreenPolicyHover> policies = new ArrayList<>();
    private int pane, top, scroll, total, detailScroll, tradeTab, hudScroll;
    private List<MenuView.Entry> parties = List.of();
    private final List<int[]> partySpans = new ArrayList<>();
    private static final String[] POLITICS_IDS = {"government", "economic_laws", "military_laws", "social_laws", "development", "military_staff", "research_production"};
    private static final String[] POLITICS_NAMES = {"정부", "경제법", "군법", "사회법", "발전도", "군사 참모", "연구 & 생산"};
    private static final int POLITICS_COLUMNS = 6;
    public ScreenMenu(MenuView view) { this(view, MenuTab.POLITICS, HoiClient::open); }
    public static ScreenMenu forFocus(MenuView view, String id) {
        var screen = new ScreenMenu(view);
        screen.focusTarget = id;
        return screen;
    }
    public ScreenMenu(MenuView view, MenuTab selected, Runnable researchOpen) {
        this(view,selected,researchOpen,null);
    }
    ScreenMenu(MenuView view,MenuTab selected,Runnable researchOpen,
               java.util.function.Consumer<dev.hoi.protocol.dialog.DialogPayloads.PoliticsOpen> officerOpen) {
        super(Component.literal("HOI · 국가 메뉴"));
        this.view = Objects.requireNonNull(view); this.selected = selected; this.researchOpen = researchOpen;this.officerOpen=officerOpen;
    }
    public static ScreenMenu selection(MenuView view) {
        var screen = new ScreenMenu(view);
        screen.selectionPreview = true;
        return screen;
    }
    public boolean selectionPreview() { return selectionPreview; }
    public int selectionLeft() { return (width - pane) / 2; }
    public int selectionTop() { return top; }
    public int selectionHeight() { return politicsLayout().contentTop() - top; }
    public String token() { return token; }
    public void update(MenuView next) {
        if (selectionPreview || next.equals(view)) return;
        if (!view.country().equals(next.country())) { selected = MenuTab.POLITICS; collapsed.clear(); manufacturerGroup = null; manufacturerDetailId = null; }
        var old = detail;
        boolean sameCountry = view.country().equals(next.country());
        view = next;captivityCounts=null;
        if (!sameCountry || next.balance() == null) balanceOpen = false;
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
    @Override public int unitHudLeft(){return pane;}
    @Override public boolean allowsMovement() { return !selectionPreview && detail == null && manufacturerGroup == null; }
    @Override protected void init() {
        officerTooltips.clear();capturedButton=null;
        pane = UiPanelLayout.width(selected, width);
        top = HudMenuBar.height(width);
        if (selectionPreview) {
            pane = Math.min(width - 16, 726 / Math.max(1, minecraft.getWindow().getGuiScale()));
            while (pane > 160 && new PoliticsLayout(pane, 0).contentTop() > height - 16) pane--;
            top = (height - (new PoliticsLayout(pane, 0).contentTop())) / 2;
            preparePartyChart();
            spirits = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 정신"))
                    .flatMap(s -> s.entries().stream()).filter(e -> !e.name().equals("없음")).toList();
            spiritScroll = Math.clamp(spiritScroll, 0, maximumSpiritScroll());
            addRenderableWidget(new UiMenuButton("×", selectionLeft() + pane - 25, top + 3, 19, 19, this::onClose));
            return;
        }
        HudMenuBar.buttons(width, view.country(), selected, tab -> {
            if (tab == MenuTab.INTELLIGENCE) { HoiClient.openAgency(); return; }
            if (tab == MenuTab.DECISIONS && ClientPlayNetworking.canSend(dev.hoi.protocol.decision.DecisionPayloads.Request.TYPE)) { HoiClient.openMenu(tab); return; }
            if (tab == MenuTab.RESEARCH) { researchOpen.run(); return; }
            if (tab == MenuTab.CONSTRUCTION && ClientPlayNetworking.canSend(dev.hoi.protocol.industry.IndustryConstructionPayloads.Request.TYPE)) { HoiClient.openConstruction(); return; }
            if (IndustryScreen.supports(tab) && ClientPlayNetworking.canSend(dev.hoi.protocol.industry.IndustryPayloads.Request.TYPE)) { HoiClient.openIndustry(tab); return; }
            HoiClient.cancelOpen();
            selected = tab; balanceOpen = false; scroll = 0; collapsed.clear(); detail = null; manufacturerGroup = null; manufacturerDetailId = null; rebuildWidgets();
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new UiMenuButton("×", pane - 25, top + 3, 19, 19, this::onClose));
        if (focusTarget != null) {
            detail = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 중점"))
                    .flatMap(s -> s.entries().stream()).filter(e -> e.detail().lines().findFirst().orElse("").equals(focusTarget))
                    .findFirst().orElse(null);
            detailIcon = "research"; detailScroll = 0; focusTarget = null;
        }
        if (selected == MenuTab.POLITICS) {
            for (int i = 0; i < 2; i++) {
                var box = politicsLayout().status(i);
                var entry = politicsEntry(i == 0 ? "점령지" : "협력정부");
                var button = new ScreenInvisibleButton(entry.name(), box.x(), box.y(), box.width(), box.height(),
                        () -> { if(entry.name().equals("점령지"))dev.hoi.client.map.MapOccupationHud.open();else showDetail(entry, "politics"); });
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
            spirits = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 정신"))
                    .flatMap(s -> s.entries().stream()).filter(e -> !e.name().equals("없음")).toList();
            spiritScroll = Math.clamp(spiritScroll, 0, maximumSpiritScroll());
            var focusBox = politicsLayout().focusTitle();
            addRenderableWidget(new ScreenInvisibleButton("국가 중점", focusBox.x(), focusBox.y(), focusBox.width(), focusBox.height(),
                    () -> minecraft.gui.setScreen(new FocusScreen())));
        }
        if (selected == MenuTab.TRADE) {
            addRenderableWidget(new UiMenuButton("경제", "category/industry", tradeTab == 0, 8, top + 30, (pane - 20) / 2, 24,
                    () -> { tradeTab = 0; scroll = 0; rebuildWidgets(); }));
            addRenderableWidget(new UiMenuButton("무역", "menu/trade", tradeTab == 1, pane / 2, top + 30, (pane - 20) / 2, 24,
                    () -> { tradeTab = 1; scroll = 0; rebuildWidgets(); }));
        }
        layoutRows(false, null);
        scroll = Math.clamp(scroll, 0, Math.max(0, total - (contentBottom() - contentTop())));
        layoutRows(true, null);
        if (selected == MenuTab.RESEARCH) addRenderableWidget(new UiMenuButton("연구 선택", 8, height - 27, 88, 20, researchOpen));
        if (manufacturerGroup != null && detail == null) layoutManufacturers(true, null);
        if (detail != null) {
            InputSidebarMovement.release(minecraft);
            int x = detailX(), w = detailWidth();
            addRenderableWidget(new UiMenuButton("×", x + w - 25, top + 3, 19, 19, () -> { detail = null; rebuildWidgets(); }));
        }
    }
    private int contentTop() { return selected == MenuTab.POLITICS ? politicsLayout().contentTop()
            : top + (selected == MenuTab.TRADE ? 62 : selected == MenuTab.INTELLIGENCE ? 82 : 32); }
    private boolean hasFooterActions() { return selected == MenuTab.RESEARCH || selected == MenuTab.INTELLIGENCE; }
    private int contentBottom() { return height - (hasFooterActions() ? 36 : 8); }
    private int detailX() { return width - pane >= 240 ? pane + 6 : Math.max(8, (width - detailWidth()) / 2); }
    private int detailWidth() { return Math.min(manufacturerGroup!=null?IndustryOrganizationList.WIDTH:310, width - (width - pane >= 240 ? pane + 14 : 16)); }
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
                UiMenuStyle.metal(g, 8, y, pane - 16, 21);
                UiAssets.draw(g, texture(section.icon()), 13, y + 3, 19, 15);
                dev.hoi.client.ui.UiText.text(g, font, dev.hoi.client.ui.tooltip.Tooltips.officerText(trim(section.title(), pane - 68), true), 37, y + 6, TEXT);
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
        var sections = view.page(MenuTab.OFFICER_CORPS).sections();
        var commanders = sections.stream().filter(v -> v.icon().equals("high_command")).flatMap(v -> v.entries().stream()).toList();
        if (g != null) { UiMenuStyle.metal(g, 8, y, pane - 110, 36); UiMenuStyle.metal(g, pane - 98, y, 90, 36); }
        int count = Math.min(3, commanders.size()), cell = Math.min(31, (pane - 65) / 3);
        if (g != null) for (int i = 0; i < count; i++) if (commanders.get(i).alert()) alertGlow(g, 11 + i * cell, y + 3, cell - 2, 30);
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
        double commandScale=Math.min(.5,(pane-16)/523.0);int hqWidth=(int)Math.round(200*commandScale),captureWidth=(int)Math.round(218*commandScale),commandHeight=(int)Math.round(62*commandScale),commandX=8+(int)Math.round(10*commandScale),captureX=8+(int)Math.round(293*commandScale);
        var captured=sections.stream().filter(v->v.icon().equals("captivity")).flatMap(v->v.entries().stream()).toList();String missing=captivityCounts!=null?Integer.toString(captivityCounts[0]):captured.isEmpty()?"—":captured.getFirst().value(),held=captivityCounts!=null?Integer.toString(captivityCounts[1]):captured.size()<2?"—":captured.get(1).value();
        String capturedTip="§e포획된 지휘관§r\n§a클릭§r하여 우리의 실종된 지휘관 확인\n§a우클릭§r하여 우리가 포획한 지휘관 확인\n\n우리의 실종된 지휘관: §e"+missing+"§r\n우리에게 포획된 지휘관: §e"+held+"§r\n\n"+dev.hoi.client.ui.UiCommandLabels.text("CAPTURED_GENERALS_DELAYED_OPERATIVES","전투 중에 지휘관이 포로로 잡힐 수 있습니다. 포획된 지휘관은 습격이나 첩보 작전을 통해 구출할 수 있습니다.");
        var deployed=sections.stream().filter(v->v.icon().equals("army_hq")).flatMap(v->v.entries().stream()).findFirst().orElse(null);int badgeX=8+(int)Math.round(210*commandScale),badgeY=y+(int)Math.round(19*commandScale),badgeWidth=(int)Math.round(18*commandScale),badgeHeight=(int)Math.round(27*commandScale),counterX=8+(int)Math.round(230*commandScale),counterWidth=Math.max(1,captureX-counterX-1);
        if(g!=null){g.enableScissor(8,y,8+(int)Math.round(523*commandScale),y+(int)Math.round(85*commandScale));UiAssets.draw(g,"officer/hq/ribbon",8,y,(int)Math.round(539*commandScale),(int)Math.round(59*commandScale));g.disableScissor();UiAssets.draw(g,"officer/hq/hq_button",commandX,y+3,hqWidth,(int)Math.round(49*commandScale));UiAssets.draw(g,"officer/hq/captured_button",captureX,y,captureWidth,commandHeight);dev.hoi.client.ui.UiText.centered(g,font,"육군 사령부 편제",commandX+8,y+3,hqWidth-16,(int)Math.round(49*commandScale),TEXT);if(deployed!=null){UiAssets.draw(g,"officer/hq/badge",badgeX,badgeY,badgeWidth,badgeHeight);dev.hoi.client.ui.UiText.leftSingleLine(g,font,Component.literal(deployed.value()),counterX,badgeY,counterWidth,badgeHeight,TEXT);}dev.hoi.client.ui.UiText.centered(g,font,"포획된 지휘관",captureX+(int)Math.round(69*commandScale),y+4,captureWidth-(int)Math.round(75*commandScale),commandHeight-7,TEXT);g.pose().pushMatrix();float pixel=(float)(1.0/minecraft.getWindow().getGuiScale());g.pose().translate(pixel,0);dev.hoi.client.ui.UiText.centered(g,font,held,captureX+19,y+19,10,9,GOLD,false);g.pose().popMatrix();}
        if(widgets){int rowY=y;var headquarters=new UiMenuButton("육군 사령부 편제",commandX,rowY+2,hqWidth,commandHeight,()->{var screen=new ArmyHeadquarters(this);DialogCoordinator.setContent(screen);screen.open();}).caption("").background("none");headquarters.setTooltip(null);officerTooltips.put(headquarters,dev.hoi.client.ui.UiCommandLabels.text("ARMY_HQ_TEMPLATES_TOGGLE_TOOLTIP","§G클릭§!하여 §Y육군 사령부 편제§! 목록을 전환합니다. 이 목록에서는 육군 사령부 편제를 검토 및 관리할 수 있습니다."));addRenderableWidget(headquarters);if(deployed!=null){var counter=new dev.hoi.client.ui.UiHoverRegion(deployed.name(),badgeX,badgeY,counterX+counterWidth-badgeX,badgeHeight);officerTooltips.put(counter,deployed.detail());counter.active=detail==null;addRenderableWidget(counter);}var button=new UiMenuButton("포획된 지휘관",captureX,rowY,captureWidth,commandHeight,()->openCaptivity(true)).caption("").background("none");button.setTooltip(null);capturedButton=button;officerTooltips.put(button,capturedTip);addRenderableWidget(button);}
        y += (int)Math.round(85*commandScale);
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
                if(special)g.fill(8,y,pane-8,y+17,0xFF4A4A31);
                UiAssets.draw(g, "officer/" + branch + "/banner", 8, y - 4, 49, 35);
                dev.hoi.client.ui.UiText.left(g, font, Component.literal(section.title()).withStyle(net.minecraft.ChatFormatting.BOLD), 60, y - 1, pane - 72, 17, TEXT);
            }
            if (!special && !entries.isEmpty()) {
                var chief = entries.getFirst();
                if (g != null) {
                    if (chief.alert()) alertGlow(g, 54, y + 25, 32, 32);
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
                    if(entry.alert()){int glowWidth=bw*159/168,glowHeight=bw*115/168;UiAssets.draw(g,"officer/spirit/ui/glow/frame_0",bx+(bw-glowWidth)/2,by+(28-glowHeight)/2,glowWidth,glowHeight);}
                    if (selectedSpirit) {
                        int[] size = UiAssets.imageSize(entry.icon());
                        int iconWidth = Math.max(1, Math.round(size[0] * bw / 168f)), iconHeight = Math.max(1, Math.round(size[1] * bw / 168f));
                        UiAssets.draw(g, entry.icon(), bx + Math.round((bw + 2 * bw / 168f - iconWidth) / 2), by + Math.round(14 + 3 * bw / 168f - iconHeight / 2f), iconWidth, iconHeight);
                    } else UiAssets.draw(g, entry.icon(), bx + 2, by - 9, bw - 4, 38);
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
    private void officerControl(boolean widgets, MenuView.Entry entry, String section, int slot, int x, int y, int w, int h) {
        if (!widgets || y < contentTop() || y + h > contentBottom()) return;
        var button = new ScreenInvisibleButton(entry.name() + " · " + entry.value(), x, y, w, h, () -> {
            if (slot >= 0 && (officerOpen!=null || ClientPlayNetworking.canSend(dev.hoi.protocol.dialog.DialogPayloads.PoliticsOpen.TYPE))) {
                var request=new dev.hoi.protocol.dialog.DialogPayloads.PoliticsOpen("officer_"+section,slot);
                if(officerOpen!=null)officerOpen.accept(request);else ClientPlayNetworking.send(request);
            } else if (slot < 0 && java.util.Set.of("army", "navy", "air", "special").contains(section)
                    && ClientPlayNetworking.canSend(dev.hoi.protocol.doctrine.DoctrinePayloads.Request.TYPE)) {
                String folder = switch (section) { case "army" -> "land"; case "navy" -> "naval"; case "special" -> "special_forces"; default -> "air"; };
                DoctrineScreen.open(this, folder);
            } else showDetail(entry, section);
        });
        boolean vacant = entry.value().equals("미지정") || entry.value().isBlank();
        boolean advisor = section.equals("high_command") || slot == 0 && java.util.Set.of("army", "navy", "air").contains(section);
        String title = vacant ? entry.name() : entry.value();
        officerTooltips.put(button, slot < 0 || advisor && vacant ? entry.detail() : "§e" + title + "§r\n" + entry.detail());
        button.active = detail == null;
        addRenderableWidget(button);
    }
    static float officerStatScale(net.minecraft.client.gui.Font font, Component styled, int available) {
        return Math.min(.75f, available / (float)Math.max(1, font.width(styled)));
    }
    private void officerStatText(GuiGraphicsExtractor g, String value, int x, int y, int available) {
        var styled = Tooltips.styled(Component.literal(value), false);
        float scale = officerStatScale(font, styled, available);
        g.pose().pushMatrix();
        g.pose().translate(x + available - font.width(styled) * scale, y + (15 - font.lineHeight * scale) / 2f);
        g.pose().scale(scale);
        g.text(font, styled.getVisualOrderText(), 0, 0, GOLD); g.pose().popMatrix();
    }
    private void officerText(GuiGraphicsExtractor g, String value, int x, int y, int available, int color) {
        float scale = dev.hoi.client.ui.UiText.scale(font);
        var styled = Tooltips.styled(Component.literal(value), false);
        var lines = Tooltips.split(font,styled, Math.max(1, (int)(available / scale)));
        if (lines.isEmpty()) return;
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale);
        g.text(font, lines.getFirst(), 0, 0, color); g.pose().popMatrix();
    }
    private void showDetail(MenuView.Entry entry, String icon) {
        detail = entry; detailIcon = icon; detailScroll = 0; manufacturerDetailId = null; rebuildWidgets();
    }
    private String fontSafe(String text) { return dev.hoi.protocol.ProtocolTextBounds.ellipsize(text, 2000); }
    private MenuView.Entry politicsFocus() {
        var entries = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 중점")).flatMap(s -> s.entries().stream()).toList();
        var active = entries.stream().filter(e -> e.progress() >= 0 || e.value().contains("진행")).findFirst();
        String label = active.map(MenuView.Entry::name).orElse("국가 중점 선택");
        return new MenuView.Entry("국가 중점", label, fontSafe(entries.stream().map(e -> e.name() + " · " + e.value() + "\n" + e.detail()).collect(java.util.stream.Collectors.joining("\n\n"))), active.map(MenuView.Entry::progress).orElse(-1.0), active.map(MenuView.Entry::icon).orElse(""));
    }
    private void drawPoliticsBanner(GuiGraphicsExtractor g) {
        var layout = politicsLayout(); var leader = layout.leader();
        var person = politicsEntry("지도자");
        UiAssets.draw(g, person.icon().isEmpty() ? "politics/empty/leader" : person.icon(),
                leader.x() + leader.width() * 7 / 172, leader.y() + leader.height() * 6 / 258,
                leader.width() * 158 / 172, leader.height() * 210 / 258);
        drawPoliticalArt(g, "politics/leader_frame", leader);
        centeredPoliticsText(g, person.value(), new PoliticsLayout.PoliticsBox(leader.x() + 4, leader.y() + leader.height() * 218 / 258 + 2,
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
            centeredPoliticsText(g, "다음 선거", new PoliticsLayout.PoliticsBox(electionBox.x(), middle - 12, electionBox.width(), 10), TEXT);
            centeredPoliticsText(g, election, new PoliticsLayout.PoliticsBox(electionBox.x(), middle + 2, electionBox.width(), 10), GOLD);
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
        int overlayHeight = PoliticsPartyChart.overlayHeight(chart.width());
        UiAssets.draw(g, "politics/pie_overlay", chart.x(), chart.y() + (chart.width() - overlayHeight) / 2, chart.width(), overlayHeight);
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
    private void drawPoliticalArt(GuiGraphicsExtractor g, String art, PoliticsLayout.PoliticsBox box) {
        UiAssets.draw(g, art, box.x(), box.y(), box.width(), box.height());
    }
    private void centeredPoliticsText(GuiGraphicsExtractor g, String text, PoliticsLayout.PoliticsBox box, int color) {
        if (selectionPreview) {
            float scale = Math.min(dev.hoi.client.ui.UiText.scale(font), (box.width() - 8f) / Math.max(1, font.width(text)));
            scale = Math.min(scale, box.height() / (float)font.lineHeight);
            g.pose().pushMatrix();
            g.pose().translate(box.x() + (box.width() - font.width(text) * scale) / 2, box.y() + (box.height() - font.lineHeight * scale) / 2);
            g.pose().scale(scale); g.text(font, text, 0, 0, color); g.pose().popMatrix();
        } else dev.hoi.client.ui.UiText.centered(g, font, text, box.x() + 4, box.y(), box.width() - 8, Math.max(10, box.height()), color);
    }
    private void politicsCell(GuiGraphicsExtractor g, String label, PoliticsLayout.PoliticsBox box) {
        politicsCell(g, label, box.x(), box.y(), box.width(), box.height());
    }
    private MenuView.Entry politicsEntry(String name) {
        return view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 현황"))
                .flatMap(s -> s.entries().stream()).filter(e -> e.name().equals(name) || name.equals("세부 이념") && e.name().equals("이념")).findFirst()
                .orElse(new MenuView.Entry(name, "정보 없음", ""));
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
        parties = view.page(MenuTab.POLITICS).sections().stream().flatMap(s -> s.entries().stream())
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
        double radius = PoliticsPartyChart.radius(size);
        for (int sy = 0; sy < 4; sy++) for (int sx = 0; sx < 4; sx++) {
            double dx = x + (sx + .5) / 4 - PoliticsPartyChart.centerX(size), dy = y + (sy + .5) / 4 - PoliticsPartyChart.centerY(size);
            if (Math.hypot(dx, dy) > radius) continue;
            int color = 0xFF34363A;
            double angle = PoliticsPartyChart.angle(dx, dy), sum = 0;
            for (var party : parties) {
                sum += party.progress();
                if (angle < sum) { color = partyColor(party.icon()); break; }
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
            var section = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.icon().equals(POLITICS_IDS[index])).findFirst()
                    .orElse(new MenuView.Section(POLITICS_NAMES[i], POLITICS_IDS[i], List.of()));
            if (g != null) {
                UiMenuStyle.metal(g, 9, y, pane - 22, 15);
                UiAssets.draw(g, "politics/header/" + new int[]{2, 1, 4, 5, 6, 7, 8}[i], 13, y + 1, 21, 13);
                UiMenuStyle.heading(g, font, section.title(), 39, y + 2, pane - 50, TEXT);
            }
            int cell = (pane - 36) / POLITICS_COLUMNS, iconSize = Math.min(cell - 4, row - 21);
            var entries = section.entries();
            int count = Math.max(1, entries.size());
            if (g != null) for (int j = 0; j < entries.size(); j++) if (entries.get(j).alert()) alertGlow(g, politicsSlotX(j, count, cell), politicsSlotY(j, y, row), cell - 4, iconSize);
            for (int j = 0; j < count; j++) {
                int x = politicsSlotX(j, count, cell), cy = politicsSlotY(j, y, row);
                var entry = entries.isEmpty() ? new MenuView.Entry(section.title(), "정보 없음", "서버가 이 구역의 상태를 제공하지 않습니다.") : entries.get(j);
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
                        } else if (index < 6 && ClientPlayNetworking.canSend(dev.hoi.protocol.dialog.DialogPayloads.PoliticsOpen.TYPE)) {
                            ClientPlayNetworking.send(new dev.hoi.protocol.dialog.DialogPayloads.PoliticsOpen(section.icon(), position));
                        } else showDetail(entry, section.icon());
                    });
                    if (!entry.value().equals("공석") && !entry.value().equals("미지정") && !entry.icon().equals("politics/vacant"))
                        policies.add(new ScreenPolicyHover(new PoliticsLayout.PoliticsBox(x, cy, cell - 4, iconSize), entry));
                    button.active = true; addRenderableWidget(button);
                }
            }
            y += row + (count - 1) / POLITICS_COLUMNS * (row - 17);
        }
        total = y - contentTop() + scroll;
    }
    private static void alertGlow(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        float unit = Math.min(w, h) / 63f;
        int glowW = Math.round(91.65f * unit), glowH = Math.round(74.75f * unit);
        UiAssets.draw(g, "doctrine/gfx_ongoing_generic_glow_yellow/frame_0", x + (w - glowW) / 2, y + (h - glowH) / 2, glowW, glowH);
    }
    private static int politicsSlotX(int index, int count, int cell) { return 16 + Math.max(0, POLITICS_COLUMNS - count) * cell / 2 + (index % POLITICS_COLUMNS) * cell; }
    private static int politicsSlotY(int index, int y, int row) { return y + 17 + (index / POLITICS_COLUMNS) * (row - 17); }
    private void requestRefresh() {
        if (ClientPlayNetworking.canSend(MenuPayloads.Refresh.TYPE)) ClientPlayNetworking.send(new MenuPayloads.Refresh(token));
    }
    @Override public void tick() {
        if (!selectionPreview && ++refreshTicks % 40 == 0) requestRefresh();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (selectionPreview) {
            g.pose().pushMatrix();
            g.pose().translate(selectionLeft(), 0);
            UiMenuStyle.panel(g, 0, top, pane, selectionHeight());
            UiMenuStyle.heading(g, font, CountryDisplays.name(view.country(), view.countryName()), 10, top + 9, pane - 44, TEXT);
            drawPoliticsBanner(g);
            g.pose().popMatrix();
            super.extractRenderState(g, mx, my, delta);
            drawPoliticsTooltip(g, mx - selectionLeft(), my, mx);
            return;
        }
        HudMenuBar.draw(g, width, view.hud(), mx, my, hudScroll);
        UiMenuStyle.panel(g, 0, top, pane, height - top);
        String heading=trim(selected == MenuTab.POLITICS ? "정치" : selected.label(),pane-44);int headingX=10;

        UiMenuStyle.heading(g, font, heading, headingX, top + 9, pane - 44, TEXT);
        if (selected == MenuTab.POLITICS) drawPoliticsBanner(g);
        if (selected == MenuTab.INTELLIGENCE) {
            UiMenuStyle.recess(g, 8, top + 30, pane - 16, 46);
            if (!view.country().equals("KOR") || !UiAssets.draw(g, "country/kor/intelligence", 18, top + 33, 42, 40))
                UiAssets.draw(g, "menu/intelligence", 18, top + 33, 42, 40);
            dev.hoi.client.ui.UiText.text(g, font, trim("정보기관", pane - 86), 76, top + 43, TEXT);
            dev.hoi.client.ui.UiText.text(g, font, trim(CountryDisplays.name(view.country(), view.countryName()), pane - 86), 76, top + 58, MUTED);
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
        if (balanceOpen && selected == MenuTab.POLITICS && view.balance() != null) drawBalance(g, mx, my);
        super.extractRenderState(g, mx, my, delta);
        drawPoliticsTooltip(g, mx, my, mx);
        if(manufacturerGroup!=null&&detail==null&&children().stream().noneMatch(c->c instanceof dev.hoi.client.ui.UiMenuButton b&&b.getMessage().getString().equals("세부 사항")&&b.isMouseOver(mx,my)))manufacturerTooltips.entrySet().stream().filter(e->Tooltips.contains(e.getKey(),mx,my)).findFirst().ifPresent(e->Tooltips.drawSourceScrollable(g,font,e.getValue(),mx,my,width,height,0));
        if (selected == MenuTab.OFFICER_CORPS && detail == null) {
            String text = officerTooltips.entrySet().stream().filter(entry -> entry.getKey().isMouseOver(mx, my))
                    .map(Map.Entry::getValue).findFirst().orElse("");
            if (!text.equals(hoveredOfficer)) { hoveredOfficer = text; tooltipScroll = 0; }
            if (!text.isEmpty()) tooltipScroll = Tooltips.drawSourceScrollable(g, font, text, mx, my, width, height, tooltipScroll).scroll();
        }
    }
    private void drawPoliticsTooltip(GuiGraphicsExtractor g, int mx, int my, int screenX) {
        var hovered = politicsHover(mx, my);
        if (!Objects.equals(hovered, hoveredPolitics)) tooltipScroll = 0;
        hoveredPolitics = hovered;
        if (hovered != null && hovered.name().equals("지도자")) {
            tooltipScroll = PoliticsTooltip.drawLeader(g, font, hovered, view.country(), CountryDisplays.name(view.country(), view.countryName()), screenX, my, width, height, tooltipScroll);
        } else if (hovered != null && hovered.name().equals("세력") && hovered.value().equals("미가입")) {
            tooltipScroll = PoliticsTooltip.drawCountryDetail(g, font, hovered, view.country(), screenX, my, width, height, tooltipScroll);
        } else if (hovered != null && (hovered.name().equals("점령지") || hovered.name().equals("협력정부") || hovered.icon().startsWith("politics/party/") || hovered.icon().startsWith("politics/ideology/"))) {
            Tooltips.drawExact(g, font, hovered.detail(), screenX, my);
        } else if (hovered != null && hovered.detail().contains("[image:")) {
            tooltipScroll = PoliticsTooltip.draw(g, font, hovered, screenX, my, width, height, tooltipScroll);
        } else if (hovered != null) {
            boolean party = hovered.icon().startsWith("politics/party/") || hovered.icon().startsWith("politics/ideology/");
            String title = party || hovered.value().isBlank() ? hovered.name() : hovered.value();
            var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
            lines.addAll(font.split(Component.literal(title).withColor(Tooltips.GOLD),Tooltips.width(width,14)));
            if (party) lines.addAll(font.split(Component.literal(hovered.value()),Tooltips.width(width,14)));
            if (!hovered.detail().isBlank()) lines.addAll(spirits.contains(hovered) ? Tooltips.tooltipLines(font, hovered.detail(), width) : Tooltips.lines(font, hovered.detail(), Tooltips.width(width,14)+24));
            lines=new ArrayList<>(Tooltips.collapseBlankLines(lines));
            int count = Math.max(1, (height - 48) / (font.lineHeight + 1));
            tooltipScroll = Math.clamp(tooltipScroll, 0, Math.max(0, lines.size() - count));
            g.setTooltipForNextFrame(font, lines.subList(tooltipScroll, Math.min(lines.size(), tooltipScroll + count)), screenX, my);
        }
    }
    private PoliticsLayout.PoliticsBox balanceButton() {
        var layout = politicsLayout(); var first = layout.status(0); var last = layout.status(1);
        return new PoliticsLayout.PoliticsBox(first.x(), layout.summaryY(), last.x() + last.width() - first.x(), first.y() - layout.summaryY() - 2);
    }
    private int balancePanelWidth() { return Math.min(UiPanelLayout.width(MenuTab.INTELLIGENCE, width), width - 8); }
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
            lines.addAll(font.split(Component.literal(balance.left() + " ↔ " + balance.right()),Tooltips.width(width,14)));
            for (var range : balance.ranges()) lines.addAll(font.split(Component.literal(range.name() + " · " + range.value()),Tooltips.width(width,14)));
            g.setTooltipForNextFrame(font, lines, mx, my);
        }
    }
    public PoliticsLayout politicsLayout() { return new PoliticsLayout(pane, top); }
    int spiritPitch() { return Math.max(18, politicsLayout().spirits().height() - 5); }
    private int maximumSpiritScroll() { return Math.max(0, spirits.size() * spiritPitch() - Math.max(0, politicsLayout().spirits().width() - 8)); }
    private int spiritStart() { return politicsLayout().spirits().x() + 4; }
    public MenuView.Entry politicsHover(double x, double y) {
        if (selected != MenuTab.POLITICS || detail != null || manufacturerGroup != null) return null;
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
        var party=PoliticsPartyChart.at(parties,x-chart.x()-PoliticsPartyChart.centerX(chart.width()),y-chart.y()-PoliticsPartyChart.centerY(chart.width()),PoliticsPartyChart.radius(chart.width()));
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
        var lines = Tooltips.split(font,Component.literal(detail.detail().isBlank() ? "추가 정보 없음" : detail.detail()), w - 28);
        int visible = Math.max(1, height - top - 128);
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - visible));
        g.enableScissor(x + 10, top + 84, x + w - 10, height - 44);
        int y = top + 84 - detailScroll;
        for (var line : lines) { dev.hoi.client.ui.UiText.text(g, font, line, x + 14, y, TEXT); y += 13; }
        g.disableScissor();
    }
    private UiScrollBar manufacturerScrollbar(){int start=top+IndustryOrganizationList.LIST_TOP,end=Math.min(height-8,top+IndustryOrganizationList.HEIGHT)-10,visible=end-start;return new UiScrollBar(detailX()+detailWidth()-11,start,end,visible,visible+manufacturerMaximum);}
    private void dragManufacturer(double y){manufacturerScroll=manufacturerScrollbar().offset(y);rebuildWidgets();}
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){if(manufacturerDragging){dragManufacturer(event.y());return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){if(manufacturerDragging){manufacturerDragging=false;return true;}return super.mouseReleased(event);}
    private void layoutManufacturers(boolean widgets, GuiGraphicsExtractor g) {
        if(widgets)manufacturerTooltips.clear();
        int x = detailX(), w = detailWidth(), bottom = Math.min(height-8,top+IndustryOrganizationList.HEIGHT), start = top + IndustryOrganizationList.LIST_TOP, end = bottom - 10, entryHeight = IndustryOrganizationList.ENTRY_HEIGHT;
        var groups = List.of("points", "materiel", "armor", "air", "navy");
        var names = List.of("특성 점수 보유", "군수품", "전차", "비행기", "함선");
        var categories = List.of("", "MATERIEL", "TANK", "PLANE", "SHIP");
        var icons = List.of("mio/ui/gfx_icon_button_45x32/frame_0", "mio/ui/gfx_inf_art_checkbox/frame_0", "mio/ui/gfx_armour_checkbox/frame_0", "mio/ui/gfx_air_checkbox/frame_0", "mio/ui/gfx_screen_ships_checkbox/frame_0");
        Comparator<MenuView.Manufacturer> order=manufacturerByName?Comparator.comparing(m->m.entry().name()):Comparator.comparingInt(MenuView.Manufacturer::size);
        if(manufacturerDescending)order=order.reversed();
        var filtered=view.manufacturers().stream().filter(m -> manufacturerGroup.equals("points") ? m.points()>0 : m.groups().contains(manufacturerGroup));
        var rows=(manufacturerSorted?filtered.sorted(order):filtered).toList();
        manufacturerMaximum=Math.max(0,rows.size()*entryHeight-Math.max(1,end-start));manufacturerScroll = Math.clamp(manufacturerScroll, 0, manufacturerMaximum);
        if (g != null) { g.nextStratum(); IndustryOrganizationList.frame(g, font, x, top, w, bottom - top); }
        if (widgets) {
            addRenderableWidget(new UiMenuButton("규모",x+8,top+24,15,15,()->{manufacturerDescending=manufacturerSorted&&!manufacturerByName&&!manufacturerDescending;manufacturerByName=false;manufacturerSorted=true;rebuildWidgets();}).caption("").background("mio/ui/gfx_sort_skill/frame_"+(manufacturerSorted&&!manufacturerByName?manufacturerDescending?2:1:0)).sound("ui.scroll"));
            addRenderableWidget(new UiMenuButton("이름",x+23,top+24,41,15,()->{manufacturerDescending=manufacturerSorted&&manufacturerByName&&!manufacturerDescending;manufacturerByName=true;manufacturerSorted=true;rebuildWidgets();}).background("mio/ui/gfx_sort_up_down_button_83x29/frame_"+(manufacturerSorted&&manufacturerByName?manufacturerDescending?2:1:0)).sound("ui.scroll"));
            addRenderableWidget(IndustryOrganizationList.close(x, top, w, () -> { manufacturerGroup = null; rebuildWidgets(); }));
            for (int i = 0; i < groups.size(); i++) {
                final String group = groups.get(i);
                String icon=manufacturerGroup.equals(group)?icons.get(i).replace("frame_0","frame_1"):icons.get(i);
                String tip=i==0?UiCommandLabels.text("INDUSTRIAL_ORG_LIST_WINDOW_FILTER_CAN_UNLOCK_TOOLTIP","사용할 수 있는 특성 점수를 보유 중인 군수산업체만 보기")
                        :UiCommandLabels.text("INDUSTRIAL_ORG_LIST_WINDOW_FILTER_CATEGORY_TOOLTIP","$CATEGORY|Y$에 해당되는 군수산업체만 보기").replace("$CATEGORY|Y$","§Y"+UiCommandLabels.text("INDUSTRIAL_ORG_LIST_WINDOW_FILTER_CATEGORY_"+categories.get(i),names.get(i))+"§!");
                var filter=new dev.hoi.client.ui.UiPanelButton(names.get(i),x+(i==0?152:152+21*i),top+(i==0?25:24),23,16,()->{manufacturerGroup=group;manufacturerScroll=0;rebuildWidgets();}){
                    @Override protected void extractContents(GuiGraphicsExtractor graphics,int mx,int my,float delta){
                        UiAssets.draw(graphics,icon,getX(),getY(),getWidth(),getHeight());
                        if(icon.startsWith("mio/ui/gfx_icon_button_45x32/")){graphics.pose().pushMatrix();graphics.pose().translate(getX()+6.5f,getY()+1.5f);graphics.pose().scale(.5f);UiAssets.draw(graphics,"mio/ui/gfx_refitting/frame_0",0,0,18,18);graphics.pose().popMatrix();}
                    }
                };
                filter.sound("ui.checkbox");filter.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(dev.hoi.client.ui.tooltip.Tooltips.officerText(tip)));addRenderableWidget(filter);
            }
        }
        if (g != null) g.enableScissor(x + 8, start, x + 8 + IndustryOrganizationList.ENTRY_WIDTH, end);
        int y = start - manufacturerScroll;
        for (var manufacturer : rows) {
            var entry = manufacturer.entry();
            if (g != null) IndustryOrganizationRow.draw(g,entry.icon(),manufacturer.size(),entry.name(),manufacturer.type(),manufacturer.equipment(),x+8,y,IndustryOrganizationList.ENTRY_WIDTH);
            if (widgets && y >= start && y + entryHeight <= end) {
                var hover=new dev.hoi.client.ui.UiHoverRegion(entry.name(),x+8,y,IndustryOrganizationList.ENTRY_WIDTH,entryHeight);manufacturerTooltips.put(hover,entry.detail());
                addRenderableWidget(IndustryOrganizationList.details(font,manufacturer.id(),x+8,y));addRenderableWidget(hover);
            }
            y += entryHeight;
        }
        if (g != null) {
            g.disableScissor();
            manufacturerScrollbar().draw(g,manufacturerScroll);
        }
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        final double hoverX = x, hoverY = y;
        if (selected == MenuTab.OFFICER_CORPS && officerTooltips.keySet().stream().anyMatch(widget -> widget.isMouseOver(hoverX, hoverY))
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
        if (selected == MenuTab.POLITICS && detail == null && manufacturerGroup == null && politicsLayout().parties().contains(x, y)) {
            return true;
        }
        if (politicsHover(x, y) != null && (com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT))) {
            tooltipScroll -= (int)(vertical * 3); return true;
        }
        if (selected == MenuTab.POLITICS && detail == null && manufacturerGroup == null
                && politicsLayout().spirits().contains(x, y)) {
            spiritScroll = Math.clamp(spiritScroll - (int)((horizontal != 0 ? horizontal : vertical) * spiritPitch()), 0, maximumSpiritScroll());
            return true;
        }
        if (x >= 40 && x < HudMenuBar.statsRight(width) && y >= 0 && y < HudMenuBar.STATS_HEIGHT) {
            hudScroll = HudMenuBar.scroll(width, view.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        if (detail != null) { detailScroll -= (int)(vertical * 26); return true; }
        if (manufacturerGroup != null && x >= detailX()) { manufacturerScroll -= (int)(vertical * IndustryOrganizationList.ENTRY_HEIGHT); rebuildWidgets(); return true; }
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
    @Override public void removed() { InputSidebarMovement.release(minecraft); if (!DialogCoordinator.suspending()) HoiClient.cancelOpen(); }
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
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        }
    }
}
