package dev.hoi.client.research;

import dev.hoi.client.ui.PanelButton;

import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.UiSounds;
import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.screen.DialogClient;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.HoiMenuStyle;
import dev.hoi.client.ui.HoiPanelLayout;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.ResearchProtocol;
import dev.hoi.protocol.ResearchView;
import dev.hoi.protocol.ResearchView.Tech;
import dev.hoi.protocol.MenuTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;


public final class ResearchScreen extends Screen implements SidebarMovement.Screen {


    private static final String[] CATEGORIES = {"INFANTRY", "SUPPORT", "ARMOR", "ARTILLERY", "NAVY", "NAVAL_SUPPORT", "AIR", "ENGINEERING", "INDUSTRY"};
    private static final String[] LABELS = {"보병", "지상 & 항공 지원", "기갑", "포", "해군", "해군 지원 장비", "공군", "공학", "산업"};
    private static final int GOLD = 0xFFFFAA00, TEXT = 0xFFE0E6E8, MUTED = 0xFF9BABB4;
    private ResearchView view;
    private ResearchLayout layout;
    private List<TfrResearchLayout.Connection> referenceConnections = List.of();
    private List<ResearchTreeLabels.Label> referenceLabels = List.of();
    private String category = CATEGORIES[0], detail, focusedTech;
    private int selectedSlot, slotOffset, treeTop, treeBottom, treePadding, panelX, panelY, panelW, panelH, detailScroll, pending, hudScroll;
    private double scrollX, scrollY;
    private boolean panning, overview = true;
    private final java.util.function.Consumer<ResearchProtocol.Request> requests;
    private final java.util.function.Consumer<MenuTab> menus;
    private final List<Button> baseButtons = new ArrayList<>();

    public ResearchScreen(ResearchView view) { this(view, HoiClient::send); }
    public ResearchScreen(ResearchView view, java.util.function.Consumer<ResearchProtocol.Request> requests) {
        this(view, requests, HoiClient::openMenu);
    }
    public ResearchScreen(ResearchView view, java.util.function.Consumer<ResearchProtocol.Request> requests, java.util.function.Consumer<MenuTab> menus) {
        super(Component.literal("HOI · 연구")); this.view = view; this.requests = requests; this.menus = menus;
    }
    public String session() { return view.session(); }
    public void update(ResearchView next) {
        if (!next.session().equals(view.session())) return;
        if (!next.country().equals(view.country())) { minecraft.gui.setScreen(null); return; }
        if (next.revision() <= view.revision()) return;
        view = next; pending = 0;
        selectedSlot = Math.min(selectedSlot, view.slots().size() - 1);
        if (detail != null && view.technology(detail) == null) detail = null;
        rebuildWidgets();
    }
    @Override protected void init() {
        if (!overview) SidebarMovement.release(minecraft);
        clearWidgets(); baseButtons.clear();
        HoiMenuBar.buttons(width, view.country(), MenuTab.RESEARCH, tab -> {
            if (tab == MenuTab.RESEARCH) { HoiClient.cancelOpen(); overview = true; detail = null; rebuildWidgets(); }
            else menus.accept(tab);
        }).forEach(this::addRenderableWidget);
        if (overview) { initOverview(); return; }
        int tabColumns = 9, tabWidth = Math.min(42, (HoiMenuBar.statsRight(width) - 20) / tabColumns);
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int index = i;
            baseButtons.add(addRenderableWidget(new ResearchTabButton(LABELS[i], CATEGORIES[i], category.equals(CATEGORIES[i]),
                    10 + i * tabWidth, menuBottom() + 1, tabWidth, () -> {
                        category = CATEGORIES[index]; scrollX = 0; scrollY = 0; focusedTech = null; detail = null; rebuildWidgets();
                    })));
        }
        treeTop = menuBottom() + ResearchTabButton.heightFor(tabWidth);
        treeBottom = Math.max(treeTop + 16, height - 36);
        relayout();
        if (detail != null) initDetail();
    }
    public int treeTop() { return treeTop; }
    private int visibleSlots() { return 6; }
    private int menuBottom() { return HoiMenuBar.height(width); }
    private int overviewWidth() { return HoiPanelLayout.width(MenuTab.RESEARCH, width); }
    private int slotPitch() { return Math.min(54, Math.max(17, (height - slotsTop() - 8) / visibleSlots())); }
    private int bannerHeight() { return Math.max(1, Math.min((overviewWidth() - 12) * 138 / 532, height - menuBottom() - 80 - visibleSlots() * 17)); }
    private int slotsTop() { return menuBottom() + 72 + bannerHeight(); }
    private void initOverview() {
        int pane = overviewWidth(), count = visibleSlots();
        slotOffset = Math.clamp(slotOffset, 0, Math.max(0, view.slots().size() - count));
        button("×", pane - 28, menuBottom() + 7, 20, 20, this::onClose);
        var benefitsButton = new PanelButton("제한된 혜택: " + view.benefits().remainingUses(), 12, slotsTop()-30, pane/2-18, 18, () -> {}) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                researchIndicator(g,"제한된 혜택:","research/limited_benefits",Integer.toString(view.benefits().remainingUses()),12,slotsTop()-25,pane/2-18,GOLD,false);
            }
        };
        addRenderableWidget(benefitsButton);
        var benefitLines=new ArrayList<String>();
        for(var benefit:view.benefits().limited()) {
            benefitLines.add(benefit.name()+" · "+benefit.uses()+"회");
            benefitLines.add(String.format(Locale.ROOT,"연구 속도 %+.1f%% · 선행 불이익 %.1f년 감소",benefit.speed()*100,benefit.aheadYears()));
            benefitLines.add(String.join(", ",benefit.scope()));
        }
        if(benefitLines.isEmpty()) benefitLines.add("사용 가능한 제한 혜택이 없습니다.");
        benefitsButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(String.join("\n",benefitLines))));
        for (int n = 0; n < count; n++) {
            int index = slotOffset + n;
            if (index >= view.slots().size()) break;
            var slot = view.slots().get(index); var technology = view.technology(slot.technology());
            addRenderableWidget(new ResearchSlotButton(slot, technology, view.country(), 6, slotsTop() + n * slotPitch(), pane - 12, slotPitch() - 4, () -> {
                selectedSlot = index; overview = false; scrollX = 0; scrollY = 0; focusedTech = null;
                if (technology != null) { category = ResearchLayout.category(technology); showDetail(technology.id()); }
                else rebuildWidgets();
            }));
        }
    }
    private final Map<String, ResearchLayout.Node> nodesById = new HashMap<>();
    private void relayout() {
        layout = ResearchPresentation.apply(ResearchLayout.create(view.technologies(), category, ""), minecraft.getResourceManager(), width - 20);
        referenceLabels = TfrResearchLayout.applies(layout) ? ResearchTreeLabels.create(layout, font::width, font.lineHeight) : List.of();
        treePadding = font.lineHeight + 3;
        layout = layout.withVerticalPadding(treePadding);
        nodesById.clear(); layout.nodes().forEach(n -> nodesById.put(n.tech().id(), n));
        referenceLabels = referenceLabels.stream().map(label -> new ResearchTreeLabels.Label(label.text(), label.x(),
                label.y() + treePadding, label.width(), label.height(), label.year())).toList();
        referenceConnections = TfrResearchLayout.applies(layout) ? TfrResearchLayout.connections(layout) : List.of();
        clampScroll();
    }
    private void clampScroll() {
        scrollX = ResearchLayout.clampScroll(scrollX, layout.width(), width - 20);
        scrollY = ResearchLayout.clampScroll(scrollY, layout.height(), treeBottom - treeTop);
    }
    private Button button(String label, int x, int y, int w, int h, Runnable action) {
        return addRenderableWidget(new PanelButton(label, x, y, w, h, action));
    }
    public void showDetail(String id) {
        var target = view.technology(id);
        if (target == null) return;
        overview = false; category = ResearchLayout.category(target);
        UiSounds.play("ui.research.select");
        var chosen=view.technology(id); int free=view.availableSlot(selectedSlot);
        if(chosen!=null&&chosen.status()==ResearchView.Status.AVAILABLE&&free>=0) selectedSlot=free;
        detail = id; focusedTech = id; detailScroll = 0; rebuildWidgets();
        layout.nodes().stream().filter(n -> n.tech().id().equals(id)).findFirst().ifPresent(n -> {
            scrollX = n.x() - (width - 20 - n.width()) / 2.0;
            scrollY = n.y() - (treeBottom - treeTop - n.height()) / 2.0;
            clampScroll();
        });
    }
    private void initDetail() {
        baseButtons.forEach(b -> { b.active = false; b.visible = false; });
        panelW = Math.min(366, width - 24); panelH = Math.min(350, height - menuBottom() - 24);
        panelX = Math.min(Math.max(12, width / 5), width - panelW - 12);
        panelY = menuBottom() + Math.max(12, (height - menuBottom() - panelH) / 2);
        var tech = view.technology(detail);
        button("×", panelX + panelW - 28, panelY + 8, 20, 20, () -> { detail = null; rebuildWidgets(); });
        var activeSlot = view.slots().stream().filter(s -> s.technology().equals(detail)).findFirst();
        String action = tech.status() == ResearchView.Status.ACTIVE ? "연구 중단"
                : tech.status() == ResearchView.Status.COMPLETED ? "연구 완료"
                : tech.status() == ResearchView.Status.LOCKED ? "선행 연구 필요"
                : view.availableSlot(selectedSlot)>=0 ? "연구" : "빈 슬롯 필요";
        var start = button(pending > 0 ? "응답 대기…" : action, panelX + panelW - 104, panelY + 37, 90, 22, () -> {
            pending = 100;
            requests.accept(new ResearchProtocol.Request(activeSlot.isPresent() ? ResearchProtocol.Action.CANCEL : ResearchProtocol.Action.START,
                    view.session(), view.revision(), activeSlot.map(ResearchView.Slot::index).orElse(view.availableSlot(selectedSlot)), detail));
            rebuildWidgets();
        });
        ((PanelButton)start).sound(activeSlot.isPresent() ? "ui.click" : "ui.research.select");
        start.active = pending == 0 && (activeSlot.isPresent() || tech.status() == ResearchView.Status.AVAILABLE
                && view.availableSlot(selectedSlot)>=0);
    }
    @Override public void tick() { if (pending > 0 && --pending == 0) rebuildWidgets(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (overview) { drawOverview(g); HoiMenuBar.draw(g, width, view.hud(), mouseX, mouseY, hudScroll); super.extractRenderState(g, mouseX, mouseY, delta); return; }
        g.fillGradient(0, 0, width, height, 0xFF1B201E, 0xFF060909);
        g.fill(10, treeTop, width - 10, treeBottom, 0xFF101A21);
        g.outline(10, treeTop, width - 20, treeBottom - treeTop, 0xFF778178);
        drawTree(g, mouseX, mouseY);
        g.text(font, "완료", 12, height - 24, color(ResearchView.Status.COMPLETED));
        g.text(font, "가능", 44, height - 24, color(ResearchView.Status.AVAILABLE));
        g.text(font, "진행", 76, height - 24, color(ResearchView.Status.ACTIVE));
        g.text(font, "잠김", 108, height - 24, color(ResearchView.Status.LOCKED));
        if (detail != null) g.text(font, trim(view.message().isEmpty() ? "휠: 세로  ·  가로 휠/우클릭 드래그: 이동  ·  방향키/Enter: 기술 선택" : view.message(), width - 24), 12, height - 12, MUTED);
        if (detail != null) drawDetail(g);
        HoiMenuBar.draw(g, width, view.hud(), mouseX, mouseY, hudScroll);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    private void drawOverview(GuiGraphicsExtractor g) {
        int pane = overviewWidth(), top = menuBottom(), bannerY = top + 39;
        HoiMenuStyle.panel(g, 0, top, pane, height - top);
        HoiMenuStyle.heading(g, font, "연구", 12, top + 9, pane - 44, HoiMenuStyle.TEXT);
        g.fillGradient(6, bannerY, pane - 6, bannerY + bannerHeight(), 0xFF4E315E, 0xFF172731);
        if (!UiAssets.cover(g, "panel/research_banner", 6, bannerY, pane - 12, bannerHeight())) {
            ResearchIcons.fallback(g, "ENGINEERING", pane / 2 - 10, bannerY + bannerHeight() / 2 - 8, 0xFFBB98CD, 2);
        }
        g.outline(6, bannerY, pane - 12, bannerHeight(), 0xFFA070B5);
        double researchSpeed=view.benefits().researchSpeed();
        researchIndicator(g,"연구 속도:","research/speed",String.format(Locale.ROOT,"%+.0f%%",researchSpeed*100),pane/2+6,slotsTop()-25,pane/2-18,researchSpeed>=0?0xFF55FF55:0xFFAA0000,true);
    }
    private void researchIndicator(GuiGraphicsExtractor g,String label,String icon,String value,int x,int y,int max,int color,boolean right) {
        int natural=font.width(label)+15+font.width(value);
        float scale=Math.min(1f,max/(float)Math.max(1,natural));
        g.pose().pushMatrix();g.pose().translate(right?x+max-natural*scale:x,y);g.pose().scale(scale);
        g.text(font,label,0,0,TEXT);UiAssets.draw(g,icon,font.width(label)+2,-2,11,11);
        g.text(font,value,font.width(label)+15,0,color);g.pose().popMatrix();
    }
    private void drawTree(GuiGraphicsExtractor g, int mx, int my) {
        g.enableScissor(10, treeTop, width - 10, treeTop + treePadding + 16);
        for (int i = 0; !layout.sourceTree() && i < layout.years().size(); i++) {
            int x = (int)(30 + i * ResearchLayout.COLUMN - scrollX);
            g.text(font, String.valueOf(layout.years().get(i)), x, treeTop + treePadding + 3, GOLD);
        }
        g.disableScissor();
        g.enableScissor(10, treeTop, width - 10, treeBottom);
        for (int i = 0; !layout.sourceTree() && i <= layout.years().size(); i++) {
            int x = (int)(20 + i * ResearchLayout.COLUMN - scrollX);
            g.verticalLine(x, treeTop, treeBottom, 0xFF1C2421);
        }
        boolean reference = TfrResearchLayout.applies(layout);
        if (reference) drawReferenceGuides(g);
        if(layout.sourceTree() && !reference) for(var guide:layout.yearGuides().entrySet()) {
            int gx=layout.vertical()?14:(int)(10+guide.getValue()-scrollX);
            int gy=layout.vertical()?(int)(treeTop+guide.getValue()-scrollY):treeTop+treePadding+2;
            if(gy>=treeTop&&gy<treeBottom) g.text(font,guide.getKey()+"년",gx,gy,GOLD);
        }
        var byId = nodesById;
        if (reference) drawReferenceConnections(g);
        if (!reference) for (var node : layout.nodes()) for (var parentId : ResearchLayout.parents(node.tech())) {
            var parent = byId.get(parentId); if (parent == null) continue;
            int x1 = (int)(10 + parent.x() + parent.width() - scrollX), y1 = (int)(treeTop + parent.y() + parent.height()/2 - scrollY);
            int x2 = (int)(10 + node.x() - scrollX), y2 = (int)(treeTop + node.y() + node.height()/2 - scrollY);
            if(layout.vertical()) {
                x1=(int)(10+parent.x()+parent.width()/2-scrollX);y1=(int)(treeTop+parent.y()+parent.height()-scrollY);
                x2=(int)(10+node.x()+node.width()/2-scrollX);y2=(int)(treeTop+node.y()-scrollY);
                int midY=(y1+y2)/2;
                int tint=parent.tech().status()==ResearchView.Status.COMPLETED?0xFF567D57:0xFF49525A;
                g.fill(x1,Math.min(y1,midY),x1+2,Math.max(y1,midY)+2,tint);
                g.fill(Math.min(x1,x2),midY,Math.max(x1,x2)+2,midY+2,tint);
                g.fill(x2,Math.min(midY,y2),x2+2,Math.max(midY,y2)+2,tint);
                continue;
            }
            int mid = Math.max(x1 + 10, (x1 + x2) / 2);
            int line = parent.tech().status() == ResearchView.Status.COMPLETED ? 0xFF567D57 : 0xFF49525A;
            g.fill(Math.min(x1, mid), y1, Math.max(x1, mid) + 2, y1 + 2, line);
            g.fill(mid, Math.min(y1, y2), mid + 2, Math.max(y1, y2) + 2, line);
            g.fill(Math.min(mid, x2), y2, Math.max(mid, x2) + 2, y2 + 2, line);
        }
        ResearchLayout.Node hovered = null;
        for (var n : layout.nodes()) {
            int x = (int)(10 + n.x() - scrollX), y = (int)(treeTop + n.y() - scrollY);
            int cardW=n.width(),cardH=n.height();
            if (x + cardW < 10 || x > width - 10 || y + cardH < treeTop || y > treeBottom) continue;
            var tech = n.tech(); boolean hover = detail == null && insideTree(mx, my) && n.contains(mx - 10 + scrollX, my - treeTop + scrollY);
            if (hover) hovered = n;
            g.fill(x, y, x + cardW, y + cardH, hover ? 0xFF2B3C49 : 0xFF1B2933);
            g.outline(x, y, cardW, cardH, focusedTech != null && focusedTech.equals(tech.id()) ? TEXT : color(tech.status()));
            UiAssets.technology(g, view.country(), tech, x + 2, y + 2, cardW - 4, cardH - 4, color(tech.status()));
            if (tech.status() == ResearchView.Status.ACTIVE) {
                g.fill(x + 2, y + cardH - 4, x + cardW - 2, y + cardH - 1, 0xFF0D151A);
                g.fill(x + 2, y + cardH - 4, x + 2 + (int)((cardW - 4) * tech.fraction()), y + cardH - 1, color(tech.status()));
            }
        }
        for (var label : referenceLabels) {
            int x = (int)(10 + label.x() - scrollX), y = (int)(treeTop + label.y() - scrollY);

            g.fill(x - 2, y - 1, x + label.width() + 2, y + label.height() + 1, 0xFF101A21);
            g.text(font, label.text(), x, y, label.year() ? TEXT : MUTED);
        }
        if (layout.nodes().isEmpty()) g.text(font, "이 분야에 표시할 기술이 없습니다.", 28, treeTop + 28, MUTED);
        g.disableScissor();
        int span = width - 20, visible = treeBottom - treeTop;
        if (layout.width() > span) {
            int thumb = Math.max(12, span * span / layout.width()), pos = (int)(scrollX / (layout.width() - span) * (span - thumb));
            g.fill(10 + pos, treeBottom + 2, 10 + pos + thumb, treeBottom + 5, 0xFF887749);
        }
        if (layout.height() > visible) {
            int thumb = Math.max(10, visible * visible / layout.height()), pos = (int)(scrollY / (layout.height() - visible) * (visible - thumb));
            g.fill(width - 8, treeTop + pos, width - 5, treeTop + pos + thumb, 0xFF887749);
        }
        if (hovered != null) {
            var t = hovered.tech();
            var lines = tooltipLines(t).stream().flatMap(line -> font.split(ResearchText.decorateEffect(line), Math.min(310, width - 24)).stream()).toList();
            g.setTooltipForNextFrame(font, lines, mx, my);
        }
    }
    private void drawReferenceGuides(GuiGraphicsExtractor g) {
        double scale = TfrResearchLayout.scale(layout);
        for (int row = 0; layout.vertical() && row < (int)Math.ceil(layout.height() / (44 * scale)); row++) if (row % 2 == 0) {
            int y = (int)(treeTop + treePadding + (18 + row * 44) * scale - scrollY);
            g.fill(10, y, width - 10, y + (int)Math.round(44 * scale), 0xFF131C22);
        }
    }
    private void drawReferenceConnections(GuiGraphicsExtractor g) {
        for (var group : TfrResearchLayout.groups(layout)) {
            int x=(int)(10+group.x()-scrollX), y=(int)(treeTop+group.y()-scrollY);
            g.fill(x,y,x+group.width(),y+group.height(),0x552E210B);
            g.outline(x,y,group.width(),group.height(),0xFF9F8153);
            if (!group.caption().isEmpty()) g.text(font,group.caption(),x,y+group.height()+4,GOLD);
        }
        for (var connection : referenceConnections) {
            int tint = connection.parent().status() == ResearchView.Status.COMPLETED ? 0xFF567D57 : 0xFF49525A;
            for (var segment : connection.segments()) {
                int x1 = (int)(10 + segment.x1() - scrollX), y1 = (int)(treeTop + segment.y1() - scrollY);
                int x2 = (int)(10 + segment.x2() - scrollX), y2 = (int)(treeTop + segment.y2() - scrollY);
                g.fill(Math.min(x1, x2) - 1, Math.min(y1, y2) - 1, Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, tint);
            }
        }
        for (var choice : TfrResearchLayout.choices(layout)) {
            int x = (int)(10 + choice.x() - scrollX), y = (int)(treeTop + choice.y() - scrollY);
            int size = (int)Math.round(24 * TfrResearchLayout.scale(layout));
            String first = choice.horizontal() ? "left" : "up", second = choice.horizontal() ? "right" : "down";
            UiAssets.draw(g, "research/xor_" + first, x - size/2, y - size/2, size, size);
            UiAssets.draw(g, "research/xor_" + second, x - size/2, y - size/2, size, size);
        }
    }
    private void drawDetail(GuiGraphicsExtractor g) {
        g.nextStratum(); g.fill(0, menuBottom(), width, height, 0xB0000000);
        g.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF343B43, 0xFF101416);
        g.outline(panelX, panelY, panelW, panelH, 0xFF82909B);
        var tech = view.technology(detail);
        g.centeredText(font, ResearchText.gold(trim(tech.name(), panelW - 70)), panelX + panelW / 2 - 10, panelY + 14, TEXT);
        g.horizontalLine(panelX + 8, panelX + panelW - 8, panelY + 30, 0xFF657078);
        UiAssets.technology(g, view.country(), tech, panelX + 12, panelY + 39, 52, 37, GOLD);
        g.fill(panelX + 70, panelY + 37, panelX + panelW - 110, panelY + 59, 0xFF0C1014);
        int bank = view.slots().stream().filter(slot -> slot.technology().equals(tech.id())).findFirst()
                .map(ResearchView.Slot::savedDays).orElse(view.slots().get(selectedSlot).savedDays());
        g.centeredText(font, tech.remainingDays(bank) + "일", panelX + (70 + panelW - 110) / 2, panelY + 44, GOLD);
        g.text(font, status(tech.status()) + " · " + tech.year() + "년", panelX + 70, panelY + 65, MUTED);
        int contentHeight = panelH - 100;
        int total = detailBody(null, tech, 0);
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, total - contentHeight));
        g.enableScissor(panelX + 10, panelY + 88, panelX + panelW - 10, panelY + panelH - 12);
        detailBody(g, tech, panelY + 88 - detailScroll);
        g.disableScissor();
        if (total > contentHeight) {
            int thumb = Math.max(10, contentHeight * contentHeight / total);
            int pos = detailScroll * (contentHeight - thumb) / Math.max(1, total - contentHeight);
            g.fill(panelX + panelW - 7, panelY + 88 + pos, panelX + panelW - 4, panelY + 88 + pos + thumb, GOLD);
        }
    }
    private int detailBody(GuiGraphicsExtractor g, Tech tech, int y) {
        y = detailText(g, detailLines(tech), panelX + 14, y, panelW - 30, TEXT);
        var cards = ResearchDetails.cards(tech, view.country());
        if (!cards.isEmpty()) {
            for (var card : cards) if (!card.kind().equals("부대")) y = detailCard(g, card, y + 4);
        }
        var units = cards.stream().filter(card -> card.kind().equals("부대")).toList();
        var otherUnlocks = unlockNames(tech).stream().filter(label -> cards.stream().noneMatch(card -> card.name().equals(label))).toList();
        if (!units.isEmpty() || !otherUnlocks.isEmpty()) {
            y = detailText(g, List.of(ResearchText.white("잠금 해제")), panelX + 14, y + 5, panelW - 30, TEXT);
            for (var card : units) y = detailCard(g, card, y + 4);
            y = detailText(g, otherUnlocks.stream().map(this::unlockName).toList(), panelX + 14, y + 4, panelW - 30, TEXT);
        }
        if (tech.source() != null && !tech.source().deferredEffects().isEmpty()) {
            y = detailText(g, List.of(Component.empty(), Component.literal("원본 추가 효과 · 적용 대기")), panelX + 14, y, panelW - 30, MUTED);
            y = detailText(g, tech.source().deferredEffects().stream().map(ResearchText::effect).toList(), panelX + 14, y, panelW - 30, TEXT);
        }
        return y;
    }
    private int detailText(GuiGraphicsExtractor g, List<? extends Component> lines, int x, int y, int w, int color) {
        for (var line : lines) {
            var wrapped = font.split(ResearchText.decorateEffect(line), w);
            if (wrapped.isEmpty()) { y += 13; continue; }
            for (var text : wrapped) { if (g != null) g.text(font, text, x, y, color); y += 13; }
        }
        return y;
    }
    private int detailCard(GuiGraphicsExtractor g, ResearchDetails.Card card, int y) {
        int x = panelX + 14, w = panelW - 30;
        boolean image = !card.texture().isEmpty();
        int textX = x + (image ? 90 : 7), textW = w - (image ? 97 : 14);
        var heading = List.of(card.kind().equals("부대") ? ResearchText.white(card.name()) : ResearchText.gold(card.name()), ResearchText.white(card.kind()));
        var effects = card.effects().stream().map(ResearchText::effect).toList();
        int end = detailText(null, heading, textX, y + 7, textW, TEXT);
        int statsY = Math.max(y + (image ? 64 : 34), end + 4);
        int bottom = detailText(null, effects, x + 7, statsY, w - 14, TEXT) + 7;
        if (g != null) {
            g.fill(x, y, x + w, bottom, 0xFF151C21);
            g.outline(x, y, w, bottom - y, 0xFF52616B);
            if (image) UiAssets.draw(g, card.texture(), x + 5, y + 5, 78, 52);
            detailText(g, heading, textX, y + 7, textW, TEXT);
            detailText(g, effects, x + 7, statsY, w - 14, TEXT);
        }
        return bottom;
    }
    public List<Component> detailLines(Tech tech) {
        var result = new ArrayList<Component>();
        if (tech.source() != null && !tech.source().description().isBlank()) {
            result.add(ResearchText.keywords(tech.source().description())); result.add(Component.empty());
        }
        if (tech.source() != null) {
            if (!tech.source().excludes().isEmpty()) {
                result.add(ResearchText.white("택일 연구 · 아래 연구와 동시에 선택할 수 없습니다."));
                for (var id : tech.source().excludes()) result.add(ResearchText.gold(researchName(id)));
                result.add(Component.empty());
            }
            tech.source().conditions().stream().map(ResearchText::white).forEach(result::add);
        }
        if (!tech.effects().isEmpty()) {
            result.add(ResearchText.gold("효과:")); tech.effects().stream().map(ResearchText::effect).forEach(result::add); result.add(Component.empty());
        }
        return result;
    }
    private Component unlockName(String label) {
        return label.matches(".*(?:보병|민병|기병|중대|대대)")
                ? ResearchText.white(label) : ResearchText.gold(label);
    }
    private List<String> unlockNames(Tech tech) {
        var labels = new LinkedHashSet<>(tech.unlocks());
        if (tech.source() != null) for (String label : tech.source().unlockLabels())
            labels.add(tech.source().localizedNames().getOrDefault(label, label));
        return List.copyOf(labels);
    }
    List<Component> tooltipLines(Tech tech) {
        var lines = new ArrayList<Component>();
        lines.add(ResearchText.gold(tech.name()));
        var missing = new ArrayList<String>();
        for (String id : tech.prerequisites()) if (!completed(id)) missing.add(technologyName(id));
        if (tech.source() != null && !tech.source().anyOf().isEmpty()
                && tech.source().anyOf().stream().noneMatch(this::completed))
            missing.add(String.join(" 또는 ", tech.source().anyOf().stream().map(this::technologyName).toList()));
        if (tech.status() != ResearchView.Status.COMPLETED && !missing.isEmpty())
            lines.add(Component.literal("기술 필요 (" + String.join(", ", missing) + ")").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        if (tech.status() == ResearchView.Status.COMPLETED) lines.add(Component.literal("연구됨").withStyle(net.minecraft.ChatFormatting.GREEN));
        else {
            int bank = view.slots().stream().filter(s -> s.technology().equals(tech.id())).findFirst()
                    .map(ResearchView.Slot::savedDays).orElse(view.slots().get(selectedSlot).savedDays());
            lines.add(ResearchText.white("연구 시간: ").append(ResearchText.gold(Long.toString(tech.remainingDays(bank)))).append(ResearchText.white("일")));
            var date = java.time.LocalDate.of(2020, 1, 1).plusDays(view.day());
            double years = Math.max(0, tech.year() - (date.getYear() + (date.getDayOfYear() - 1.0) / date.lengthOfYear()));
            if (years > 0) lines.add(Component.literal(String.format(Locale.ROOT,
                    "%.2f년 앞선 기술입니다. 기술이 앞설수록 시간 불이익이 큽니다.", years)).withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
        lines.add(Component.literal(" "));
        lines.add(ResearchText.gold("효과").append(ResearchText.white(":")));
        tech.effects().stream().map(ResearchText::effect).forEach(lines::add);
        for (String label : unlockNames(tech)) lines.add(ResearchText.white("사용 가능 ").append(unlockName(label)));
        if (tech.source() != null && !tech.source().deferredEffects().isEmpty()) {
            lines.add(Component.literal("원본 추가 효과 · 적용 대기").withStyle(net.minecraft.ChatFormatting.GRAY));
            tech.source().deferredEffects().stream().map(ResearchText::effect).forEach(lines::add);
        }
        return lines;
    }
    private boolean completed(String id) { var tech = view.technology(id); return tech != null && tech.status() == ResearchView.Status.COMPLETED; }
    private String technologyName(String id) { var tech = view.technology(id); return tech == null ? id : tech.name(); }
    private String researchName(String id) {
        var t=view.technology(id);
        return (t!=null&&t.status()==ResearchView.Status.COMPLETED?"✓ ":"○ ")+(t==null?id:t.name());
    }
    private boolean insideTree(double x, double y) { return !overview && x >= 10 && x < width - 10 && y >= treeTop && y < treeBottom; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (event.y() < menuBottom()) return super.mouseClicked(event, twice);
        if (detail == null && insideTree(event.x(), event.y())) {
            clearFocus();
            if (event.button() == 1 || event.button() == 2) { panning = true; return true; }
            if (event.button() == 0) {
                var node = layout.at(event.x() - 10 + scrollX, event.y() - treeTop + scrollY);
                if (node != null) { showDetail(node.tech().id()); return true; }
            }
        }
        if (detail != null && (event.x() < panelX || event.x() > panelX + panelW || event.y() < panelY || event.y() > panelY + panelH)) return true;
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panning && detail == null) { scrollX -= dx; scrollY -= dy; clampScroll(); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { panning = false; return super.mouseReleased(event); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= 40 && x < HoiMenuBar.statsRight(width) && y >= 0 && y < HoiMenuBar.STATS_HEIGHT) {
            hudScroll = HoiMenuBar.scroll(width, view.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        if (overview && x < overviewWidth() && y >= slotsTop() && view.slots().size() > visibleSlots()) {
            slotOffset -= (int)Math.signum(vertical); rebuildWidgets(); return true;
        }
        if (detail != null) { detailScroll -= (int)(vertical * 26); return true; }
        if (insideTree(x, y)) { scrollX -= horizontal * 40; scrollY -= vertical * 40; clampScroll(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && detail != null) { detail = null; rebuildWidgets(); return true; }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && !overview) { overview = true; rebuildWidgets(); return true; }
        if (!overview && detail == null && getFocused() == null && !layout.nodes().isEmpty()) {
            int index = 0;
            for (int i = 0; i < layout.nodes().size(); i++) if (layout.nodes().get(i).tech().id().equals(focusedTech)) index = i;
            if (event.key() == GLFW.GLFW_KEY_ENTER && focusedTech != null) { showDetail(focusedTech); return true; }
            if (event.key() >= GLFW.GLFW_KEY_RIGHT && event.key() <= GLFW.GLFW_KEY_UP) {
                if (focusedTech != null) index = Math.floorMod(index + (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_UP ? -1 : 1), layout.nodes().size());
                var node = layout.nodes().get(index); focusedTech = node.tech().id();
                scrollX = node.x() - 20; scrollY = node.y() - 18; clampScroll(); return true;
            }
        }
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        return super.keyReleased(event);
    }
    @Override public int unitHudLeft(){return overviewWidth();}
    @Override public boolean allowsMovement() { return overview && detail == null; }
    @Override public void removed() { SidebarMovement.release(minecraft); if (DialogClient.suspending()) return; requests.accept(new ResearchProtocol.Request(ResearchProtocol.Action.CLOSE, view.session(), 0, -1, "")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private String trim(String value, int max) { return font.width(value) <= max ? value : font.plainSubstrByWidth(value, Math.max(1, max - font.width("…"))) + "…"; }
    private static String status(ResearchView.Status status) {
        return switch (status) { case LOCKED -> "잠김"; case AVAILABLE -> "연구 가능"; case ACTIVE -> "연구 중"; case COMPLETED -> "연구 완료"; };
    }
    private static int color(ResearchView.Status status) {
        return switch (status) { case LOCKED -> 0xFF77838C; case AVAILABLE -> GOLD; case ACTIVE -> 0xFF74BCD8; case COMPLETED -> 0xFF8DBD81; };
    }
}
