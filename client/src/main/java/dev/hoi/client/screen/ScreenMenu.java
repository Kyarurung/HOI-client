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
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){if(balanceOpen&&selected==MenuTab.POLITICS&&view.balance()!=null&&balanceWindow.contains(balanceFrame(),event.x(),event.y())){if(super.mouseClicked(event,twice))return true;if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)balanceWindow.click(event.x(),event.y());return true;}if(manufacturerGroup!=null&&detail==null&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&manufacturerScrollbar().contains(event.x(),event.y())){if(manufacturerScrollbar().arrow(event.y())){manufacturerScroll=manufacturerScrollbar().step(event.y(),manufacturerScroll,IndustryOrganizationList.ENTRY_HEIGHT);rebuildWidgets();}else{manufacturerDragging=true;dragManufacturer(event.y());}return true;}if(!selectionPreview&&selected==MenuTab.OFFICER_CORPS&&capturedButton!=null&&capturedButton.active&&capturedButton.isMouseOver(event.x(),event.y())&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){AudioUiSounds.play("ui.click");openCaptivity(false);return true;}if(!selectionPreview&&detail==null&&(selected==MenuTab.POLITICS||selected==MenuTab.OFFICER_CORPS)&&listScrollbar().contains(event.x(),event.y())){if(event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return true;var bar=listScrollbar();if(bar.arrow(event.y()))scroll=bar.step(event.y(),scroll,40);else{listDragging=true;scroll=bar.offset(event.y());}rebuildWidgets();return true;}return super.mouseClicked(event,twice);}
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
    private int spiritScroll, tooltipScroll, partyScroll;
    private boolean balanceOpen;
    private final dev.hoi.client.screen.decision.DecisionBalanceWindow balanceWindow = new dev.hoi.client.screen.decision.DecisionBalanceWindow();
    public dev.hoi.client.screen.decision.DecisionBalanceWindow balanceWindow() { return balanceWindow; }
    public boolean decisionResponse(dev.hoi.protocol.decision.DecisionPayloads.Response packet) { return balanceOpen && balanceWindow.update(packet); }
    public UiMenuStyle.Frame balanceFrame() { return dev.hoi.client.screen.decision.DecisionBalanceWindow.frame(politicsLayout(), width); }
    private MenuView.Entry hoveredPolitics;
    private List<MenuView.Entry> spirits = List.of();
    private record ScreenPolicyHover(PoliticsLayout.PoliticsBox box, MenuView.Entry entry) {}
    private final List<ScreenPolicyHover> policies = new ArrayList<>();
    private UiMenuStyle.Frame frame;
    private int pane, top, scroll, total, detailScroll, tradeTab, hudScroll;
    private List<MenuView.Entry> parties = List.of();
    private final List<int[]> partySpans = new ArrayList<>();
    private static final String[] POLITICS_IDS = {"government", "economic_laws", "military_laws", "social_laws", "development", "military_staff", "research_production"};
    private static final String[] POLITICS_NAMES = {"정부", "경제법", "군법", "사회법", "발전도", "군사 참모", "연구 & 생산"};
    private static final int POLITICS_COLUMNS = 7;
    private static final UiMenuStyle.Window IDEAS = new UiMenuStyle.Window("research/source/gfx_tiled_window_2b_border/frame_0", PoliticsLayout.WIDTH, UiMenuStyle.NO_HEADER, 0);
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
        if (!sameCountry || next.balance() == null) { balanceOpen = false; balanceWindow.close(); }
        else if (balanceOpen && !next.balance().category().equals(balanceWindow.category())) balanceWindow.open(next.balance().category(), minecraft.getResourceManager());
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
        frame = UiMenuStyle.frame(UiMenuStyle.window(selected), width, pane);
        top = Math.round(frame.y());
        if (selectionPreview) {
            pane = Math.min(width - 16, 726 / Math.max(1, minecraft.getWindow().getGuiScale()));
            while (pane > 160 && new PoliticsLayout(pane, 0).contentTop() > height - 16) pane--;
            top = (height - (new PoliticsLayout(pane, 0).contentTop())) / 2;
            preparePartyChart();
            spirits = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.title().equals("국가 정신"))
                    .flatMap(s -> s.entries().stream()).filter(e -> !e.name().equals("없음")).toList();
            spiritScroll = Math.clamp(spiritScroll, 0, maximumSpiritScroll());
            addRenderableWidget(UiMenuStyle.close(selectionLeft() + pane - 21.5f, top + 3, this::onClose));
            return;
        }
        HudMenuBar.buttons(width, view.country(), selected, tab -> {
            if (tab == MenuTab.INTELLIGENCE) { HoiClient.openAgency(); return; }
            if (tab == MenuTab.DECISIONS && ClientPlayNetworking.canSend(dev.hoi.protocol.decision.DecisionPayloads.Request.TYPE)) { HoiClient.openMenu(tab); return; }
            if (tab == MenuTab.RESEARCH) { researchOpen.run(); return; }
            if (tab == MenuTab.CONSTRUCTION && ClientPlayNetworking.canSend(dev.hoi.protocol.industry.IndustryConstructionPayloads.Request.TYPE)) { HoiClient.openConstruction(); return; }
            if (IndustryScreen.supports(tab) && ClientPlayNetworking.canSend(dev.hoi.protocol.industry.IndustryPayloads.Request.TYPE)) { HoiClient.openIndustry(tab); return; }
            HoiClient.cancelOpen();
            selected = tab; balanceOpen = false; balanceWindow.close(); scroll = 0; collapsed.clear(); detail = null; manufacturerGroup = null; manufacturerDetailId = null; rebuildWidgets();
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(UiMenuStyle.close(UiMenuStyle.window(selected), frame, this::onClose));
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
                addRenderableWidget(new ScreenInvisibleButton("권력의 균형", box.x(), box.y(), box.width(), box.height(), () -> {
                    balanceOpen = !balanceOpen;
                    if (balanceOpen) balanceWindow.open(view.balance().category(), minecraft.getResourceManager()); else balanceWindow.close();
                    rebuildWidgets();
                }));
                if (balanceOpen) {
                    var at = balanceFrame();
                    addRenderableWidget(UiMenuStyle.close("menu/close", at.x(510), at.y(7), at.size(32), at.size(33), () -> {balanceOpen = false; balanceWindow.close(); rebuildWidgets();}));
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
        if (selected == MenuTab.OFFICER_CORPS) officerHeader(true, null);
        if (selected == MenuTab.RESEARCH) addRenderableWidget(new UiMenuButton("연구 선택", 8, height - 27, 88, 20, researchOpen));
        if (manufacturerGroup != null && detail == null) layoutManufacturers(true, null);
        if (detail != null) {
            InputSidebarMovement.release(minecraft);
            int x = detailX(), w = detailWidth();
            addRenderableWidget(new UiMenuButton("×", x + w - 25, top + 3, 19, 19, () -> { detail = null; rebuildWidgets(); }));
        }
    }
    private int contentTop() { return selected == MenuTab.POLITICS ? politicsLayout().contentTop()
            : selected == MenuTab.OFFICER_CORPS ? frame.iy(186)
            : top + (selected == MenuTab.TRADE ? 62 : selected == MenuTab.INTELLIGENCE ? 82 : 32); }
    private boolean hasFooterActions() { return selected == MenuTab.RESEARCH || selected == MenuTab.INTELLIGENCE; }
    private int windowHeight() { return (int)Math.ceil((height - frame.y()) / frame.scale()); }
    private int contentBottom() { return selected == MenuTab.OFFICER_CORPS ? frame.iy(windowHeight() - 10) : selected == MenuTab.POLITICS ? frame.iy(windowHeight() - 10) : height - (hasFooterActions() ? 36 : 8); }
    private dev.hoi.client.ui.UiScrollBar listScrollbar() {
        int x = selected == MenuTab.POLITICS ? frame.ix(PoliticsLayout.WIDTH - 23) : frame.ix(527);
        return new dev.hoi.client.ui.UiScrollBar(x, contentTop(), contentBottom(), contentBottom() - contentTop(), total);
    }
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
                dev.hoi.client.ui.UiText.marquee(g, font, dev.hoi.client.ui.tooltip.Tooltips.officerText(section.title(), true), 37, y + 6, pane - 68, dev.hoi.client.ui.UiText.scale(font), TEXT, true, -1);
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
    private float officerX(double px) { return frame.x(px); }
    private float officerY(double py) { return frame.y(py) - scroll; }
    private void officerArt(GuiGraphicsExtractor g, String path, double x, double y, double w, double h) {
        UiAssets.drawExact(g, path, officerX(x), officerY(y), frame.size(w), frame.size(h));
    }
    private void officerHeader(boolean widgets, GuiGraphicsExtractor g) {
        var sections = view.page(MenuTab.OFFICER_CORPS).sections();
        var commanders = sections.stream().filter(v -> v.icon().equals("high_command")).flatMap(v -> v.entries().stream()).toList();
        if (g != null) frame.art(g, "officer/ui/advisor_theorist_entry", 11, 45);
        int count = Math.min(3, commanders.size()), by = frame.iy(59.5), size = frame.len(55);
        if (g != null) for (int i = 0; i < count; i++) if (commanders.get(i).alert()) alertGlow(g, frame.ix(33.5 + 60 * i), by, size, size);
        for (int i = 0; i < count; i++) {
            var entry = commanders.get(i);
            int bx = frame.ix(33.5 + 60 * i);
            if (g != null) UiAssets.drawContent(g, entry.icon().isEmpty() ? "politics/high_command" : entry.icon(), bx, by, size, size);
            officerControl(widgets, entry, "high_command", i, bx, by, size, size, false);
        }
        var tactic = sections.stream().filter(v -> v.icon().equals("preferred_tactic")).flatMap(v -> v.entries().stream()).findFirst().orElse(null);
        if (tactic != null) {
            if (g != null) frame.art(g, "officer/preferred_tactic", 405, 50);
            officerControl(widgets, tactic, "preferred_tactic", 0, frame.ix(405), frame.iy(50), frame.len(101), frame.len(60), false);
        }
        var captured=sections.stream().filter(v->v.icon().equals("captivity")).flatMap(v->v.entries().stream()).toList();String missing=captivityCounts!=null?Integer.toString(captivityCounts[0]):captured.isEmpty()?"—":captured.getFirst().value(),held=captivityCounts!=null?Integer.toString(captivityCounts[1]):captured.size()<2?"—":captured.get(1).value();
        String capturedTip="§e포획된 지휘관§r\n§a클릭§r하여 우리의 실종된 지휘관 확인\n§a우클릭§r하여 우리가 포획한 지휘관 확인\n\n우리의 실종된 지휘관: §e"+missing+"§r\n우리에게 포획된 지휘관: §e"+held+"§r\n\n"+dev.hoi.client.ui.UiCommandLabels.text("CAPTURED_GENERALS_DELAYED_OPERATIVES","전투 중에 지휘관이 포로로 잡힐 수 있습니다. 포획된 지휘관은 습격이나 첩보 작전을 통해 구출할 수 있습니다.");
        var deployed=sections.stream().filter(v->v.icon().equals("army_hq")).flatMap(v->v.entries().stream()).findFirst().orElse(null);
        if (g != null) {
            g.enableScissor(frame.ix(13), frame.iy(116), frame.ix(536), frame.iy(201));
            frame.art(g, "officer/hq/ribbon", 13, 116);
            g.disableScissor();
            frame.art(g, "officer/hq/hq_button", 23, 121);
            frame.art(g, "officer/hq/captured_button", 306, 116);
            dev.hoi.client.ui.UiText.marquee(g, font, Tooltips.officerText("육군 사령부 편제"), frame.x(35), frame.y(145.5) - font.lineHeight / 2f, frame.size(176), 1, TEXT, true, 0);
            if (deployed != null) {
                frame.art(g, "officer/hq/badge", 223, 135);
                dev.hoi.client.ui.UiText.marquee(g, font, Component.literal(deployed.value()), frame.x(243), frame.y(148.5) - font.lineHeight / 2f, frame.size(60), 1, TEXT, true, -1);
            }
            dev.hoi.client.ui.UiText.marquee(g, font, Tooltips.officerText("포획된 지휘관"), frame.x(361), frame.y(147) - font.lineHeight / 2f, frame.size(155), 1, TEXT, true, 0);
            dev.hoi.client.ui.UiText.marquee(g, font, Component.literal(held), frame.x(346), dev.hoi.client.ui.UiText.centeredY(font, held, frame.y(161.5) - font.lineHeight / 2f, font.lineHeight, 1), frame.size(19), 1, GOLD, false, 0);
        }
        if (widgets) {
            var headquarters=new UiMenuButton("육군 사령부 편제",frame.ix(23),frame.iy(121),frame.len(200),frame.len(49),()->{var screen=new ArmyHeadquarters(this);DialogCoordinator.setContent(screen);screen.open();}).caption("").background("none");
            headquarters.setTooltip(null);officerTooltips.put(headquarters,dev.hoi.client.ui.UiCommandLabels.text("ARMY_HQ_TEMPLATES_TOGGLE_TOOLTIP","§G클릭§!하여 §Y육군 사령부 편제§! 목록을 전환합니다. 이 목록에서는 육군 사령부 편제를 검토 및 관리할 수 있습니다."));addRenderableWidget(headquarters);
            if(deployed!=null){var counter=new dev.hoi.client.ui.UiHoverRegion(deployed.name(),frame.ix(223),frame.iy(135),frame.len(80),frame.len(27));officerTooltips.put(counter,deployed.detail());counter.active=detail==null;addRenderableWidget(counter);}
            var button=new UiMenuButton("포획된 지휘관",frame.ix(306),frame.iy(116),frame.len(218),frame.len(62),()->openCaptivity(true)).caption("").background("none");button.setTooltip(null);capturedButton=button;officerTooltips.put(button,capturedTip);addRenderableWidget(button);
        }
    }
    private static final int[] OFFICER_TOPS = {191, 451, 712, 973};
    private void layoutOfficers(boolean widgets, GuiGraphicsExtractor g) {
        var sections = view.page(MenuTab.OFFICER_CORPS).sections();
        String[] branches = {"army", "navy", "air", "special"};
        int last = 186;
        for (int b = 0; b < branches.length; b++) {
            String branch = branches[b];
            var section = sections.stream().filter(v -> v.icon().equals(branch)).findFirst().orElse(null);
            if (section == null) continue;
            int t = OFFICER_TOPS[b];
            String artBranch = branch.equals("special") ? "army" : branch;
            var entries = section.entries(); boolean special = branch.equals("special"), air = branch.equals("air");
            int h = special ? 143 : 254;
            if (g != null) {
                g.pose().pushMatrix(); g.pose().translate(officerX(13), officerY(t)); g.pose().scale(frame.scale());
                UiAssets.tiledNineSlice(g, "officer/" + artBranch + "/background", 0, 0, 516, h, 6, 1);
                g.pose().popMatrix();
                officerArt(g, "officer/" + artBranch + "/header", 13, t - 12, 516, 242);
                officerArt(g, "officer/" + branch + "/banner", air ? 13 : 9, t - (special ? 4 : air ? 6 : 7), 119, 66);
                dev.hoi.client.ui.UiText.marquee(g, font, Component.literal(section.title()).withStyle(net.minecraft.ChatFormatting.BOLD), officerX(123), officerY(t + 14.5) - font.lineHeight / 2f, frame.size(240), 1, TEXT, true, -1);
            }
            if (!special && !entries.isEmpty()) {
                var chief = entries.getFirst();
                double cx = branch.equals("army") ? 118 : branch.equals("navy") ? 120 : 122, cy = t + (branch.equals("navy") ? 38 : 39);
                int bx = Math.round(officerX(cx)), by = Math.round(officerY(cy)), bw = frame.len(61), bh = frame.len(83);
                if (g != null) {
                    if (chief.alert()) alertGlow(g, bx, by, bw, bw);
                    UiAssets.drawContent(g, chief.icon(), bx, by, bw, bh);
                    if (!chief.value().equals("미지정")) dev.hoi.client.ui.UiText.marquee(g, font, Component.literal(chief.value()), officerX(183), officerY(t + 57) - font.lineHeight / 2f, frame.size(110), 1, TEXT, true, -1);
                }
                officerControl(widgets, chief, branch, 0, bx, by, frame.len(176), bh, true);
            }
            int doctrineIndex = special ? 0 : 1;
            if (entries.size() > doctrineIndex) {
                var doctrine = entries.get(doctrineIndex);
                double dx = special ? 401 : 301, dy = t + (special ? 45 : branch.equals("army") ? 34 : 35);
                if (g != null) {
                    officerArt(g, "officer/" + branch + "/doctrine_frame", dx, dy, 89, 77);
                    UiAssets.drawContent(g, doctrine.icon(), Math.round(officerX(dx + 14)), Math.round(officerY(dy + 10)), frame.len(61), frame.len(52));
                    officerArt(g, special ? "officer/ui/number_container_short" : "officer/ui/number_container", special ? dx + 26 : dx + 10, t + 94 - 10.5, special ? 37 : 69, 21);
                }
                officerControl(widgets, doctrine, branch, -1, Math.round(officerX(dx)), Math.round(officerY(dy)), frame.len(89), frame.len(77), true);
            }
            int spirits = Math.max(0, entries.size() - 2);
            int[] spiritX = air ? new int[]{112, 278} : new int[]{29, 195, 361};
            for (int i = 0; !special && i < spirits && i < spiritX.length; i++) {
                var entry = entries.get(i + 2);
                double sx = spiritX[i], sy = t + 124;
                int bx = Math.round(officerX(sx)), by = Math.round(officerY(sy)), bw = frame.len(151), bh = frame.len(63);
                if (g != null) {
                    boolean selectedSpirit = entry.progress() > 0;
                    officerArt(g, "officer/spirit/ui/background_" + (selectedSpirit ? "enabled" : "disabled"), sx, sy, 151.2, 63);
                    if (entry.alert()) officerArt(g, "officer/spirit/ui/glow/frame_0", sx + 3.95, sy - 20.25, 143.1, 103.5);
                    int[] size = UiAssets.imageSize(entry.icon());
                    double iconScale = selectedSpirit ? .9 : .72, centerX = sx + 75.6 + (selectedSpirit ? .9 : 1.8), centerY = sy + 31.5 + (selectedSpirit ? 2.7 : -7.2);
                    if (size[0] > 0) officerArt(g, entry.icon(), centerX - size[0] * iconScale / 2, centerY - size[1] * iconScale / 2, size[0] * iconScale, size[1] * iconScale);
                }
                officerControl(widgets, entry, branch, i + 1, bx, by, bw, bh, true);
            }
            var statistics = sections.stream().filter(v -> v.icon().equals(branch + "_stats"))
                    .flatMap(v -> v.entries().stream()).toList();
            int columns = special ? 5 : 7;
            double statsX = special ? 25 : branch.equals("army") ? 17 : 20, statsPitch = special || branch.equals("army") ? 72 : 71;
            for (int i = 0; i < statistics.size(); i++) {
                var statistic = statistics.get(i);
                double sx = statsX + i % columns * statsPitch, sy = t + (special ? 67 : 189) + i / columns * 33;
                int left = Math.round(officerX(sx)), upper = Math.round(officerY(sy)), w = frame.len(74), cellHeight = frame.len(32);
                if (g != null) {
                    officerArt(g, "officer/stats/background", sx, sy, 74, 32);
                    UiAssets.drawContent(g, statistic.icon(), left + frame.len(4), upper + frame.len(6), frame.len(24), frame.len(20));
                    officerStatText(g, statistic.value(), left + frame.len(28), upper, w - frame.len(32), cellHeight);
                }
                if (widgets && upper >= contentTop() && upper + cellHeight <= contentBottom()) {
                    var button = new dev.hoi.client.ui.UiHoverRegion(statistic.name(), left, upper, w, cellHeight);
                    officerTooltips.put(button, statistic.detail());
                    button.active = detail == null;
                    addRenderableWidget(button);
                }
            }
            last = t + h;
        }
        total = Math.round(frame.y(last + 10)) - contentTop();
    }
    private void officerControl(boolean widgets, MenuView.Entry entry, String section, int slot, int x, int y, int w, int h, boolean scrolled) {
        if (!widgets || scrolled && (y < contentTop() || y + h > contentBottom())) return;
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
    private void officerStatText(GuiGraphicsExtractor g, String value, int x, int y, int available, int height) {
        var text = Tooltips.styled(Component.literal(value), false);
        dev.hoi.client.ui.UiText.marquee(g, font, text.getVisualOrderText(), x, dev.hoi.client.ui.UiText.centeredY(font, text.getVisualOrderText(), y, height, 1), available, 1, GOLD, true, 1);
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
    private void politicsArt(GuiGraphicsExtractor g, PoliticsLayout layout, String art, double x, double y, double w, double h) {
        UiAssets.drawExact(g, art, layout.x(x), layout.y(y), (float)(w * layout.scale()), (float)(h * layout.scale()));
    }
    private void politicsText(GuiGraphicsExtractor g, PoliticsLayout layout, String text, double x, double center, double width, int color, int align) {
        dev.hoi.client.ui.UiText.marquee(g, font, dev.hoi.client.ui.tooltip.Tooltips.officerText(text), layout.x(x), layout.y(center) - font.lineHeight / 2f, (float)(width * layout.scale()), 1, color, true, align);
    }
    private void drawPoliticsBanner(GuiGraphicsExtractor g) {
        var layout = politicsLayout();
        politicsArt(g, layout, "politics/view_background", PoliticsLayout.ART_X, PoliticsLayout.ART_Y, PoliticsLayout.ART_WIDTH, PoliticsLayout.ART_HEIGHT);
        var person = politicsEntry("지도자");
        politicsArt(g, layout, person.icon().isEmpty() ? "politics/empty/leader" : person.icon(), 13, 55, 156, 210);
        politicsArt(g, layout, "politics/leader_frame", 6, 49, 172, 258);
        politicsText(g, layout, person.value(), 15, 285, 155, TEXT, 0);
        if (!selectionPreview) {
            politicsArt(g, layout, "politics/focus_background", 187, 52, 359, 107);
            politicsArt(g, layout, "politics/focus_select", 280, 64, 258, 83);
            var focus = politicsFocus();
            if (focus.progress() >= 0) {
                politicsArt(g, layout, "politics/focus_progress_bg", 302, 122, 212, 6);
                double fraction = Math.clamp(focus.progress(), 0, 1);
                if (fraction > 0) {
                    g.pose().pushMatrix(); g.pose().translate(layout.x(302), layout.y(122)); g.pose().scale((float)layout.scale());
                    g.blit(net.minecraft.resources.Identifier.fromNamespaceAndPath("hoi", "textures/gui/politics/focus_progress.png"),
                            0, 0, (int)Math.round(212 * fraction), 6, 0, (float)fraction, 3f / 18, 15f / 18);
                    g.pose().popMatrix();
                }
            }
            String art = focus.icon().isEmpty() ? "politics/empty/focus" : focus.icon();
            int[] size = UiAssets.imageSize(art);
            if (size[0] > 0 && size[1] > 0) politicsArt(g, layout, art, 237 - size[0] / 2.0, 105 - size[1] / 2.0, size[0], size[1]);
            boolean chosen = !focus.value().equals("국가 중점 선택");
            if (chosen) politicsText(g, layout, focus.value(), 307, 102, 200, GOLD, 0);
            else politicsText(g, layout, focus.value(), 280, 105.5, 258, 0xFFFFFFFF, 0);
        }
        politicsCell(g, "경제-정치 연합", layout.union());
        var ideology = politicsEntry("세부 이념");
        String ideologyArt = ideology.icon().isEmpty() ? politicsEntry("이념").icon() : ideology.icon();
        politicsArt(g, layout, ideologyArt.isEmpty() ? "politics/empty/ideology" : ideologyArt, 193, 174, 66, 68);
        if (!ideology.value().equals("정보 없음")) politicsText(g, layout, ideology.value(), 192, 270, 175, TEXT, -1);
        var strip = layout.spirits();
        g.enableScissor(Math.round(layout.x(281)), Math.round(layout.y(165)), Math.round(layout.x(709)), Math.round(layout.y(238)));
        for (int i = 0; i < spirits.size(); i++) {
            double sx = 291 + 68 * i - spiritScroll / layout.scale();
            if (sx + 68 > 281 && sx < 709) {
                var spirit = spirits.get(i);
                politicsArt(g, layout, spirit.icon().isEmpty() ? "menu/politics" : spirit.icon(), sx, 170, 68, 68);
            }
        }
        g.disableScissor();
        if (maximumSpiritScroll() > 0) spiritScrollbar(g, layout);
        politicsCell(g, "정치 체제", layout.government());
        var government = politicsEntry("정치 체제");
        if (!government.value().equals("정보 없음")) politicsText(g, layout, government.value(), 253, 359, 125, TEXT, 0);
        String election = politicsEntry("다음 선거").value();
        politicsText(g, layout, election.equals("선거 없음") ? election : "다음 선거: " + election, 192, 305, 140, election.equals("선거 없음") ? MUTED : TEXT, -1);
        if (!selectionPreview && view.balance() != null) {
            var balance = view.balance();
            politicsArt(g, layout, "politics/bop/button", 379, 253, 129, 57);
            String side = balance.value() < 0 ? balance.leftIcon() : balance.rightIcon();
            int[] size = UiAssets.imageSize(side);
            if (size[0] > 0) politicsArt(g, layout, side, 391, 248, size[0] * .7, size[1] * .7);
            politicsText(g, layout, Math.round(Math.abs(balance.value()) * 100) + "%", 420, 272, 100, TEXT, 0);
        }
        politicsCell(g, "경제 모델", layout.economy());
        String economy = politicsEntry("경제 모델").value();
        if (!economy.equals("정보 없음")) politicsText(g, layout, economy, 9, 415, 175, TEXT, 0);
        politicsCell(g, "세력", layout.faction());
        for (int i = 0; !selectionPreview && i < 2; i++) {
            var entry = politicsEntry(i == 0 ? "점령지" : "협력정부");
            drawPoliticalArt(g, "politics/" + (i == 0 ? "occupied" : "collaboration") + (entry.progress() > 0 ? "_active" : "_inactive"), layout.status(i));
        }
        if (!selectionPreview) politicsArt(g, layout, "politics/ui/rules", 625, 7, 38, 42);
        var chart = layout.partyChart();
        for (int[] span : partySpans) g.fill(chart.x() + span[0], chart.y() + span[1], chart.x() + span[2], chart.y() + 1 + span[1], span[3]);
        politicsArt(g, layout, "politics/pie_overlay", 398, 327, 90, 87);
        drawPartyRows(g, layout);
    }
    static final int PARTY_PITCH = 16;
    private int visibleParties() { return Math.max(1, 173 / PARTY_PITCH); }
    private void drawPartyRows(GuiGraphicsExtractor g, PoliticsLayout layout) {
        if (parties.isEmpty()) { politicsText(g, layout, "정보 없음", 530, 264, 170, MUTED, -1); return; }
        partyScroll = Math.clamp(partyScroll, 0, Math.max(0, parties.size() - visibleParties()));
        g.enableScissor(Math.round(layout.x(500)), Math.round(layout.y(251)), Math.round(layout.x(710)), Math.round(layout.y(424)));
        var leading = parties.stream().max(java.util.Comparator.comparingDouble(MenuView.Entry::progress)).orElse(null);
        for (int row = 0; row < visibleParties() + 1 && partyScroll + row < parties.size(); row++) {
            var party = parties.get(partyScroll + row);
            double y = 251 + row * PARTY_PITCH;
            if (party == leading) politicsArt(g, layout, "politics/party_leading", 510, y + 5, 196, 15);
            politicsArt(g, layout, "commander/commands/pol_party_colour_bg/frame_0", 507, y + 2, 21, 21);
            UiAssets.drawTinted(g, "commander/commands/pol_party_colour/frame_0", Math.round(layout.x(511)), Math.round(layout.y(y + 6)), (int)Math.round(13 * layout.scale()), (int)Math.round(13 * layout.scale()), partyColor(party.icon()));
            String label = party.name() + " (" + Math.round(party.progress() * 100) + ")";
            politicsText(g, layout, label, 530, y + 12.5, 180, TEXT, -1);
        }
        g.disableScissor();
        if (parties.size() > visibleParties()) {
            var bar = partyScrollbar(layout);
            bar.draw(g, partyScroll * PARTY_PITCH);
        }
    }
    private dev.hoi.client.ui.UiScrollBar partyScrollbar(PoliticsLayout layout) {
        return new dev.hoi.client.ui.UiScrollBar(Math.round(layout.x(687)), Math.round(layout.y(251)), Math.round(layout.y(424)), visibleParties() * PARTY_PITCH, parties.size() * PARTY_PITCH);
    }
    private void spiritScrollbar(GuiGraphicsExtractor g, PoliticsLayout layout) {
        g.pose().pushMatrix(); g.pose().translate(layout.x(281), layout.y(233)); g.pose().scale((float)layout.scale());
        UiAssets.tiledNineSlice(g, "commander/commands/scroll_track_horizontal_2/frame_0", 0, 0, 428, 18, 5, 1);
        g.pose().popMatrix();
        politicsArt(g, layout, "commander/commands/scroll_left_2/frame_0", 284, 232, 18, 21);
        politicsArt(g, layout, "commander/commands/scroll_right_2/frame_0", 690, 232, 18, 21);
        double travel = 428 - 2 * 21 - 22;
        double thumb = 302 + travel * spiritScroll / Math.max(1, maximumSpiritScroll());
        politicsArt(g, layout, "commander/commands/scroll_drager_2/frame_0", thumb, 231, 22, 20);
    }
    private static PoliticsLayout.PoliticsBox square(PoliticsLayout.PoliticsBox box) {
        int size = Math.min(box.width(), box.height());
        return new PoliticsLayout.PoliticsBox(box.x() + (box.width() - size) / 2, box.y() + (box.height() - size) / 2, size, size);
    }
    private void drawPoliticalArt(GuiGraphicsExtractor g, String art, PoliticsLayout.PoliticsBox box) {
        UiAssets.draw(g, art, box.x(), box.y(), box.width(), box.height());
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
        String kind = switch (label) {
            case "경제 모델" -> "economy";
            case "세력" -> "faction";
            case "정치 체제" -> "government";
            default -> "union";
        };
        String icon = politicsEntry(label).icon();
        if (icon.isEmpty()) icon = "politics/empty/" + kind;
        int inset = icon.equals("politics/empty/union") ? 0 : 3;
        var cell = square(new PoliticsLayout.PoliticsBox(x, y, w, h));
        UiAssets.draw(g, icon, cell.x() + inset - (label.equals("경제-정치 연합") ? 1 : 0), cell.y() + inset, cell.width() - inset * 2, cell.height() - inset * 2);
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
        var layout = politicsLayout(); double scale = layout.scale();
        int iconSize = (int)Math.round(63 * scale);
        float origin = layout.y(PoliticsLayout.CATEGORY_Y) - scroll;
        for (int i = 0; i < POLITICS_IDS.length; i++) {
            final int index = i;
            var section = view.page(MenuTab.POLITICS).sections().stream().filter(s -> s.icon().equals(POLITICS_IDS[index])).findFirst()
                    .orElse(new MenuView.Section(POLITICS_NAMES[i], POLITICS_IDS[i], List.of()));
            float y = origin + (float)(PoliticsLayout.CATEGORY_PITCH * i * scale);
            if (g != null) {
                UiAssets.drawExact(g, "politics/category_header", layout.x(27), y, (float)(660 * scale), (float)(52 * scale));
                String icon = "politics/header/" + new int[]{2, 1, 4, 5, 6, 7, 8}[i];
                int[] size = UiAssets.imageSize(icon);
                UiAssets.drawExact(g, icon, layout.x(36), y + (float)(8 * scale), (float)(size[0] * scale), (float)(size[1] * scale));
                UiMenuStyle.heading(g, font, section.title(), Math.round(layout.x(102)), Math.round(y + (float)(20.5 * scale) - 4.5f * (font.lineHeight + 2f) / font.lineHeight) + 1, (int)Math.round(300 * scale), TEXT);
            }
            var entries = section.entries();
            int count = Math.max(1, Math.min(POLITICS_COLUMNS, entries.size())), firstColumn = section.icon().equals("research_production") ? 1 : 0;
            if (g != null) for (int j = 0; j < Math.min(count, entries.size()); j++) if (entries.get(j).alert())
                alertGlow(g, Math.round(layout.x(104 + 80 * (j + firstColumn))), Math.round(y + (float)(44 * scale)), iconSize, iconSize);
            for (int j = 0; j < count; j++) {
                int x = Math.round(layout.x(104 + 80 * (j + firstColumn))), cy = Math.round(y + (float)(44 * scale));
                var entry = entries.isEmpty() ? new MenuView.Entry(section.title(), "정보 없음", "서버가 이 구역의 상태를 제공하지 않습니다.") : entries.get(j);
                if (g != null) {
                    String icon = entry.icon().isEmpty() ? "menu/politics" : entry.icon();
                    UiAssets.draw(g, icon, x, cy, iconSize, iconSize);
                }
                if (widgets && cy >= contentTop() && cy + iconSize <= contentBottom()) {
                    final int position = j;
                    var button = new ScreenInvisibleButton(entry.name() + " · " + entry.value(), x, cy, iconSize, iconSize, () -> {
                        if (section.icon().equals("research_production") && position < 4) {
                            manufacturerGroup = List.of("armor", "navy", "air", "materiel").get(position);
                            manufacturerScroll = 0; detail = null; manufacturerDetailId = null; rebuildWidgets();
                        } else if (index < 6 && ClientPlayNetworking.canSend(dev.hoi.protocol.dialog.DialogPayloads.PoliticsOpen.TYPE)) {
                            ClientPlayNetworking.send(new dev.hoi.protocol.dialog.DialogPayloads.PoliticsOpen(section.icon(), position));
                        } else showDetail(entry, section.icon());
                    });
                    if (!entry.value().equals("공석") && !entry.value().equals("미지정") && !entry.icon().equals("politics/vacant"))
                        policies.add(new ScreenPolicyHover(new PoliticsLayout.PoliticsBox(x, cy, iconSize, iconSize), entry));
                    button.active = true; addRenderableWidget(button);
                }
            }
        }
        total = Math.round(layout.y(PoliticsLayout.CATEGORY_Y + PoliticsLayout.CATEGORY_PITCH * POLITICS_IDS.length)) - contentTop();
    }
    private static void alertGlow(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        float unit = Math.min(w, h) / 63f;
        int glowW = Math.round(91.65f * unit), glowH = Math.round(74.75f * unit);
        UiAssets.draw(g, "doctrine/gfx_ongoing_generic_glow_yellow/frame_0", x + (w - glowW) / 2, y + (h - glowH) / 2, glowW, glowH);
    }
    private void requestRefresh() {
        if (ClientPlayNetworking.canSend(MenuPayloads.Refresh.TYPE)) ClientPlayNetworking.send(new MenuPayloads.Refresh(token));
    }
    @Override public void tick() {
        if (!selectionPreview && ++refreshTicks % 40 == 0) requestRefresh();
        if (balanceOpen) balanceWindow.tick();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (selectionPreview) {
            g.pose().pushMatrix();
            g.pose().translate(selectionLeft(), 0);
            UiAssets.tiledNineSlice(g, UiMenuStyle.window(MenuTab.POLITICS).tile(), 0, top, pane, selectionHeight(), 64, .5);
            UiMenuStyle.heading(g, font, CountryDisplays.name(view.country(), view.countryName()), 10, top + 9, pane - 44, TEXT);
            drawPoliticsBanner(g);
            g.pose().popMatrix();
            super.extractRenderState(g, mx, my, delta);
            drawPoliticsTooltip(g, mx - selectionLeft(), my, mx);
            return;
        }
        HudMenuBar.draw(g, width, view.hud(), mx, my, hudScroll);
        var window = UiMenuStyle.window(selected);
        UiMenuStyle.window(g, window, frame, height);
        UiMenuStyle.title(g, font, window, frame, selected == MenuTab.POLITICS ? "정치" : selected.label());
        if (selected == MenuTab.POLITICS) drawPoliticsBanner(g);
        if (selected == MenuTab.INTELLIGENCE) {
            UiMenuStyle.recess(g, 8, top + 30, pane - 16, 46);
            if (!view.country().equals("KOR") || !UiAssets.draw(g, "country/kor/intelligence", 18, top + 33, 42, 40))
                UiAssets.draw(g, "menu/intelligence", 18, top + 33, 42, 40);
            dev.hoi.client.ui.UiText.marquee(g, font, dev.hoi.client.ui.tooltip.Tooltips.officerText("정보기관"), 76, top + 43, pane - 86, dev.hoi.client.ui.UiText.scale(font), TEXT, true, -1);
            dev.hoi.client.ui.UiText.marquee(g, font, dev.hoi.client.ui.tooltip.Tooltips.officerText(CountryDisplays.name(view.country(), view.countryName())), 76, top + 58, pane - 86, dev.hoi.client.ui.UiText.scale(font), MUTED, true, -1);
        }
        if (selected == MenuTab.POLITICS) frame.tile(g, IDEAS.tile(), 0, PoliticsLayout.IDEAS_Y, PoliticsLayout.WIDTH, windowHeight() - PoliticsLayout.IDEAS_Y, 64, UiMenuStyle.TILE);
        else if (selected == MenuTab.OFFICER_CORPS) { frame.tile(g, "diplomacy/dialog/panel_dark", 0, 173, 550, windowHeight() - 173, 64, UiMenuStyle.TILE); officerHeader(false, g); }
        else UiMenuStyle.recess(g, 5, contentTop() - 3, pane - 10, Math.max(6, contentBottom() - contentTop() + 5));
        boolean original = selected == MenuTab.POLITICS || selected == MenuTab.OFFICER_CORPS;
        g.enableScissor(original ? 0 : 6, contentTop(), original ? frame.ix(selected == MenuTab.POLITICS ? PoliticsLayout.WIDTH - 23 : 527) : pane - 6, contentBottom()); layoutRows(false, g); g.disableScissor();
        int visible = contentBottom() - contentTop();
        if (original) { if (total > visible) listScrollbar().draw(g, scroll); }
        else if (total > visible && visible > 0) {
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
        if (balanceOpen && selected == MenuTab.POLITICS && view.balance() != null) balanceWindow.drawTooltip(g, font, mx, my);
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
        return politicsLayout().balance();
    }
    private void drawBalance(GuiGraphicsExtractor g, int mx, int my) {
        var balance = view.balance();
        String tooltip = UiCommandLabels.text("BOP_TOOLTIP", "").replace("[root.GetPowerBalanceName]", balance.name())
                .replace("[?.root.power_balance_value|1%H]", "§H" + String.format(Locale.ROOT, "%.1f%%", Math.abs(balance.value()) * 100) + "§!")
                .replace("[root.GetActiveSideName]", balance.value() < 0 ? balance.left() : balance.right());
        balanceWindow.draw(g, font, balanceFrame(), balance, tooltip, mx, my);
    }
    public PoliticsLayout politicsLayout() { return new PoliticsLayout(pane, top, selectionPreview || frame == null ? 0 : frame.x()); }
    int spiritPitch() { return Math.max(1, (int)Math.round(68 * politicsLayout().scale())); }
    private int maximumSpiritScroll() { return Math.max(0, (int)Math.round((spirits.size() * 68 - 418) * politicsLayout().scale())); }
    private int spiritStart() { return Math.round(politicsLayout().x(291)); }
    public MenuView.Entry politicsHover(double x, double y) {
        if (selected != MenuTab.POLITICS || detail != null || manufacturerGroup != null) return null;
        var layout = politicsLayout();
        if (layout.leader().contains(x, y)) {
            var leader = politicsEntry("지도자");
            return leader;
        }
        if (x >= layout.x(281) && x < layout.x(709) && y >= layout.y(170) && y < layout.y(231)) {
            int position = (int)x - spiritStart() + spiritScroll, index = position / spiritPitch();
            if (position >= 0 && index < spirits.size()) return spirits.get(index);
        }
        if (layout.ideology().contains(x, y)) return politicsEntry("세부 이념");
        if (layout.economy().contains(x, y)) return politicsEntry("경제 모델");
        if (layout.government().contains(x, y)) return politicsEntry("정치 체제");
        if (!selectionPreview) for (int i=0;i<2;i++) if (layout.status(i).contains(x,y)) return politicsEntry(i==0?"점령지":"협력정부");
        var chart=layout.partyChart();
        var party=PoliticsPartyChart.at(parties,x-chart.x()-PoliticsPartyChart.centerX(chart.width()),y-chart.y()-PoliticsPartyChart.centerY(chart.width()),PoliticsPartyChart.radius(chart.width()));
        if(party!=null)return party;
        var partyList = layout.parties();
        if (partyList.contains(x, y) && x < layout.x(687)) {
            int index = partyScroll + (int)((y - layout.y(251)) / (PARTY_PITCH * layout.scale()));
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
        UiMenuStyle.heading(g, font, detail.name(), x + 10, top + 9, w - 44, TEXT);
        UiMenuStyle.recess(g, x + 8, top + 31, w - 16, 38);
        UiAssets.draw(g, detail.icon().isEmpty() ? texture(detailIcon) : detail.icon(), x + 12, top + 36, 34, 26);
        dev.hoi.client.ui.UiText.marquee(g, font, dev.hoi.client.ui.tooltip.Tooltips.officerText(detail.value()), x + 55, top + 44, w - 64, dev.hoi.client.ui.UiText.scale(font), TEXT, true, -1);
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
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){if(manufacturerDragging){dragManufacturer(event.y());return true;}if(listDragging){scroll=listScrollbar().offset(event.y());rebuildWidgets();return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){if(manufacturerDragging){manufacturerDragging=false;return true;}if(listDragging){listDragging=false;return true;}return super.mouseReleased(event);}
    private boolean listDragging;
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
        if (balanceOpen && selected == MenuTab.POLITICS && view.balance() != null && balanceWindow.scroll(balanceFrame(), x, y, vertical)) return true;
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
            partyScroll = Math.clamp(partyScroll - (int)Math.signum(vertical), 0, Math.max(0, parties.size() - visibleParties()));
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
