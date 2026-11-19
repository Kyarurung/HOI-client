package dev.hoi.client.screen.research;

import dev.hoi.client.ui.*;
import dev.hoi.client.HoiClient;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.research.ResearchPayloads;
import dev.hoi.protocol.research.ResearchView;
import dev.hoi.protocol.research.ResearchView.Tech;
import dev.hoi.protocol.menu.MenuTab;
import dev.hoi.client.ui.industry.*;
import dev.hoi.client.ui.tooltip.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class ResearchScreen extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final String[] CATEGORIES = {"INFANTRY", "SUPPORT", "ARMOR", "ARTILLERY", "NAVY", "NAVAL_SUPPORT", "AIR", "ENGINEERING", "INDUSTRY"};
    private static final String[] LABELS = {"보병", "지상 & 항공 지원", "기갑", "포", "해군", "해군 지원 장비", "공군", "공학", "산업"};
    private static final int GOLD = 0xFFFFAA00, TEXT = 0xFFE0E6E8, MUTED = 0xFF9BABB4;
    private ResearchView view;
    private ResearchLayout layout;
    private List<ResearchSourceLayout.ResearchConnection> referenceConnections = List.of();
    private List<ResearchTreeLabels.ResearchLabel> referenceLabels = List.of();
    private String category = CATEGORIES[0], detail, focusedTech;
    private int selectedSlot, slotOffset, treeTop, treeBottom, treePadding, panelX, panelY, panelW, panelH, detailHeader, detailScroll, pending, hudScroll;
    private double scrollX, scrollY;
    private int descriptionScroll;
    private int slotTooltipScroll, slotTooltipMaximum;
    private String hoveredSlotTooltip = "";
    private dev.hoi.client.ui.UiScrollBar descriptionBar, detailBar;
    private boolean descriptionDragging, detailDragging;
    private boolean panning, overview = true, teamDragging, teamPicker, teamDescending;
    private String teamSort = "";
    private int teamScroll, teamX, teamY, teamWidth, teamHeight, waitingTeamSlot = -1;
    private String waitingTeam = "", waitingTechnology = "", hoveredTeam = "";
    private boolean selectNextSlot;
    private final List<net.minecraft.client.gui.components.AbstractWidget> teamBackdrop = new ArrayList<>(), teamDetails = new ArrayList<>();
    private final Map<Button, String> teamRows = new LinkedHashMap<>();
    private final java.util.function.Consumer<ResearchPayloads.Request> requests;
    private final java.util.function.Consumer<MenuTab> menus;
    private final List<Button> baseButtons = new ArrayList<>();
    public ResearchScreen(ResearchView view) { this(view, HoiClient::send); }
    public ResearchScreen(ResearchView view, java.util.function.Consumer<ResearchPayloads.Request> requests) {
        this(view, requests, HoiClient::openMenu);
    }
    public ResearchScreen(ResearchView view, java.util.function.Consumer<ResearchPayloads.Request> requests, java.util.function.Consumer<MenuTab> menus) {
        super(Component.literal(UiCommandLabels.text("RESEARCH_RESEARCH", "연구"))); this.view = view; this.requests = requests; this.menus = menus;
    }
    public String session() { return view.session(); }
    public void update(ResearchView next) {
        if (!next.session().equals(view.session())) return;
        if (!next.country().equals(view.country())) { minecraft.gui.setScreen(null); return; }
        if (next.revision() <= view.revision()) return;
        int previousPending=pending;
        view = next; pending = 0;
        if (waitingTeamSlot >= 0) {
            String team = waitingTeam, technology = waitingTechnology;
            int slot = view.slots().stream().filter(s -> s.technology().equals(technology)).mapToInt(ResearchView.Slot::index).findFirst().orElse(-1);
            if (slot >= 0) {
                waitingTeamSlot = -1; waitingTeam = ""; waitingTechnology = "";
                if(!team.isEmpty() && view.designTeams().stream().anyMatch(t -> t.id().equals(team) && t.technologies().contains(technology))) {
                    pending = 100;
                    requests.accept(new ResearchPayloads.Request(ResearchPayloads.Action.DESIGN_TEAM, view.session(), view.revision(), slot, team));
                }
            } else pending = previousPending;
        }
        if (selectNextSlot && (waitingTeamSlot < 0 || view.slots().stream().anyMatch(slot -> slot.technology().equals(waitingTechnology)))) {
            selectedSlot = view.availableSlot(selectedSlot);
            if (selectedSlot < 0) selectedSlot = 0;
            selectNextSlot = false;
        }
        selectedSlot = Math.min(selectedSlot, view.slots().size() - 1);
        if (detail != null && view.technology(detail) == null) detail = null;
        rebuildWidgets();
    }
    @Override protected void init() {
        if (!overview) InputSidebarMovement.release(minecraft);
        clearWidgets(); baseButtons.clear(); teamRows.clear(); teamBackdrop.clear(); teamDetails.clear();
        HudMenuBar.buttons(width, view.country(), MenuTab.RESEARCH, tab -> {
            if (tab == MenuTab.RESEARCH) { HoiClient.cancelOpen(); overview = true; detail = null; rebuildWidgets(); }
            else menus.accept(tab);
        }, overview ? Integer.MAX_VALUE : menuBottom()).forEach(this::addRenderableWidget);
        if (overview) { initOverview(); return; }
        int tabWidth = 43;
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int index = i;
            baseButtons.add(addRenderableWidget(new ResearchTabButton(LABELS[i], CATEGORIES[i], category.equals(CATEGORIES[i]),
                    Math.round((19 + i * 89) / 2f), menuBottom() + 4, tabWidth, () -> {
                        category = CATEGORIES[index]; scrollX = 0; scrollY = 0; focusedTech = null; detail = null; teamPicker = false; rebuildWidgets();
                    })));
        }
        baseButtons.add(button("×", width - 25, menuBottom() + 5, 17, 17, this::onClose));
        treeTop = menuBottom() + 4 + ResearchTabButton.heightFor(tabWidth);
        treeBottom = Math.max(treeTop + 16, height - 6);
        relayout();
        if (detail != null) initDetail();
    }
    public int treeTop() { return treeTop; }
    private int visibleSlots() { return Math.max(1, Math.min(6, (height - slotsTop() - 8) / slotPitch())); }
    private int menuBottom() { return HudMenuBar.height(width); }
    private int overviewWidth() { return UiPanelLayout.width(MenuTab.RESEARCH, width); }
    private int slotPitch() { return 50; }
    private int bannerHeight() { return Math.max(1, Math.min((overviewWidth() - 12) * 138 / 532, height - menuBottom() - 80 - Math.min(6, view.slots().size()) * slotPitch())); }
    private int slotsTop() { return menuBottom() + 72 + bannerHeight(); }
    private void initOverview() {
        int pane = overviewWidth(), count = visibleSlots();
        slotOffset = Math.clamp(slotOffset, 0, Math.max(0, view.slots().size() - count));
        button("×", pane - 28, menuBottom() + 7, 20, 20, this::onClose);
        for (int n = 0; n < count; n++) {
            int index = slotOffset + n;
            if (index >= view.slots().size()) break;
            var slot = view.slots().get(index); var technology = view.technology(slot.technology());
            var slotButton = addRenderableWidget(new ResearchSlotButton(slot, technology, view.country(), 6, slotsTop() + n * slotPitch(), Math.min(254, pane - 12), slotPitch(), () -> {
                selectedSlot = index; overview = false; scrollX = 0; scrollY = 0; focusedTech = null;
                if (technology != null) { category = ResearchLayout.category(technology); showDetail(technology.id()); }
                else rebuildWidgets();
            }));
            var team = view.designTeams().stream().filter(t -> t.id().equals(slot.designTeam())).findFirst().orElse(null);
            if (team != null) {
                int size = Math.round(64 * slotButton.sourceScale()), logoX = slotButton.sourceX(450) - size / 2, logoY = slotButton.sourceY(49) - size / 2;
                var logo = new UiPanelButton(team.name(), logoX, logoY, size, size, () -> {}) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) { UiAssets.draw(g, team.icon(), getX(), getY(), getWidth(), getHeight()); if(isHovered())Tooltips.drawSourceScrollable(g,font,teamTooltip(team,slot.technology(),false),pane+2,getY()-7,ResearchScreen.this.width,ResearchScreen.this.height,0); }
                    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) { return false; }
                };
                logo.setTooltip(null); addRenderableWidget(logo);
            }
        }
    }
    private void overviewTooltip(GuiGraphicsExtractor g,int x,int y) {
        String slotText = slotTooltipAt(x,y);
        if (!slotText.equals(hoveredSlotTooltip)) { hoveredSlotTooltip = slotText; slotTooltipScroll = 0; slotTooltipMaximum = 0; }
        if (!slotText.isEmpty()) {
            var tooltip = Tooltips.drawSourceScrollable(g,font,slotText,x,y,width,height,slotTooltipScroll,false,true);
            slotTooltipScroll = tooltip.scroll();
            slotTooltipMaximum = tooltip.maxScroll();
            return;
        }
        if(y<slotsTop()-33||y>=slotsTop())return;
        int pane=overviewWidth();
        String text="";
        if(x>=12&&x<pane/2-6)text=view.benefits().limitedTooltip().isEmpty()?"제한된 기술 혜택이 여기에 열거됩니다.":view.benefits().limitedTooltip();
        else if(x>=pane/2+6&&x<pane-12)text=view.benefits().speedTooltip().isEmpty()?"§e연구 속도§r\n\n새로운 기술을 연구하는 시간이 감소합니다.":view.benefits().speedTooltip();
        if(!text.isEmpty())dev.hoi.client.ui.tooltip.Tooltips.drawExact(g,font,text,x,y);
    }
    String slotTooltipAt(double x,double y) {
        if (!overview) return "";
        var buttons = children().stream().filter(ResearchSlotButton.class::isInstance).map(ResearchSlotButton.class::cast).toList();
        for (int i = 0; i < buttons.size(); i++) {
            var button = buttons.get(i);
            if (!Tooltips.contains(button,x,y)) continue;
            var slot = view.slots().get(slotOffset+i);
            var tech = view.technology(slot.technology());
            if (tech == null) return "";
            if (x >= button.sourceX(57) && x < button.sourceX(107) && y >= button.sourceY(60) && y < button.sourceY(82)) return slot.yearTooltip();
            if (!slot.designTeam().isEmpty() && x >= button.sourceX(418) && x < button.sourceX(482) && y >= button.sourceY(17) && y < button.sourceY(81)) return "";
            return slotTooltip(tech);
        }
        return "";
    }
    static String slotTooltip(Tech tech) {
        var lines = new ArrayList<String>();
        lines.add("§Y"+tech.name()+"§!");
        lines.add("");
        lines.add("§Y효과:§!");
        if (tech.upgrades().isEmpty() && tech.units().isEmpty()) lines.addAll(tech.effects());
        for (var upgrade : tech.upgrades()) {
            lines.add("£unit_"+upgrade.scopeId()+(upgrade.scopeId().startsWith("category_")?"":"_icon_medium")+"£ §Y"+upgrade.scope()+":§!");
            for (var stat : upgrade.lines()) lines.add("  "+stat.name()+": "+(stat.value().startsWith("+")?"§G":stat.value().startsWith("-")?"§R":"§Y")+stat.value()+"§!");
        }
        for (var unit : tech.units()) {
            lines.add("£unit_"+unit.id()+"_icon_medium£ §Y"+unit.name()+":§!");
            for (var stat : unit.stats()) lines.add("  "+stat.name()+": "+(stat.value().startsWith("+")?"§G":stat.value().startsWith("-")?"§R":"§Y")+stat.value()+"§!");
        }
        if (tech.source()!=null && !tech.source().description().isBlank()) { lines.add(""); lines.add(tech.source().description()); }
        return dev.hoi.client.ui.tooltip.TooltipText.spacing(String.join("\n",lines));
    }
    private final Map<String, ResearchLayout.ResearchNode> nodesById = new HashMap<>();
    private void relayout() {
        layout = ResearchPresentation.apply(ResearchLayout.create(view.technologies(), category, ""), minecraft.getResourceManager(), width);
        referenceLabels = ResearchSourceLayout.applies(layout) ? ResearchTreeLabels.create(layout, font::width, font.lineHeight) : List.of();
        treePadding = layout.sourceTree() ? 0 : font.lineHeight + 3;
        layout = layout.withVerticalPadding(treePadding);
        nodesById.clear(); layout.nodes().forEach(n -> nodesById.put(n.tech().id(), n));
        referenceLabels = referenceLabels.stream().map(label -> new ResearchTreeLabels.ResearchLabel(label.text(), label.x(),
                label.y() + treePadding, label.width(), label.height(), label.year())).toList();
        referenceConnections = ResearchSourceLayout.applies(layout) ? ResearchSourceLayout.connections(layout) : List.of();
        clampScroll();
    }
    private void clampScroll() {
        scrollX = ResearchLayout.clampScroll(scrollX, layout.width(), width);
        scrollY = ResearchLayout.clampScroll(scrollY, layout.height(), treeBottom - treeTop);
    }
    private Button button(String label, int x, int y, int w, int h, Runnable action) {
        return addRenderableWidget(label.equals("×")
                ? new dev.hoi.client.ui.UiMenuButton(label, x, y, w, h, action)
                : new UiPanelButton(label, x, y, w, h, action));
    }
    private Button researchButton(String label, int x, int y, int w, int h, Runnable action) {
        return addRenderableWidget(new UiPanelButton(label,x,y,w,h,action) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiAssets.draw(g,"research/detail/gfx_government_button/frame_0",getX(),getY(),getWidth(),getHeight());
                var text=getMessage().getString();
                boolean hasTeam = !eligibleTeams().isEmpty();
                var bounds=font.prepareText(Component.literal(text).getVisualOrderText(),0,0,0xFFFFFFFF,false,false,0).bounds();
                float textY=bounds==null?(getHeight()-font.lineHeight)/2f:(getHeight()-bounds.height())/2f-bounds.top();
                g.pose().pushMatrix();
                g.pose().translate(getX()+(getWidth()-font.width(text))/2f,getY()+textY);
                g.text(font,text,0,0,0xFFFFFFFF,false);
                g.pose().popMatrix();
                if (hasTeam) UiAssets.drawContent(g,"mio/ui/gfx_design_team_icon/frame_0",getX()+getWidth()-13,getY()+1,11,getHeight()-2);
            }
        });
    }
    public void showDetail(String id) {
        var target = view.technology(id);
        if (target == null) return;
        boolean keepTree = !overview && category.equals(ResearchLayout.category(target));
        overview = false; category = ResearchLayout.category(target);
        AudioUiSounds.play("ui.research.select");
        detail = id; focusedTech = id; detailScroll = 0; descriptionScroll = 0; descriptionDragging = false; detailDragging = false; rebuildWidgets();
        if (!keepTree) layout.nodes().stream().filter(n -> n.tech().id().equals(id)).findFirst().ifPresent(n -> {
            scrollX = n.x() - (width - n.width()) / 2.0;
            scrollY = n.y() - (treeBottom - treeTop - n.height()) / 2.0;
            clampScroll();
        });
    }
    private void initDetail() {
        panelW = Math.min(266, width - 24); panelH = height - menuBottom() - 18;
        panelX = Math.min(254, width - panelW - 12);
        if (teamPicker) panelX = Math.max(8, Math.min(panelX, width - panelW - Math.min(IndustryOrganizationList.WIDTH, width - 16) - 23));
        panelY = menuBottom() + 15;
        var tech = view.technology(detail);
        detailHeader=106;
        panelH = Math.min(panelH, Math.max(267, detailBody(null, tech, 0) + detailHeader + 12));
        button("×", panelX + panelW - 22, panelY + 5, 14, 14, () -> { detail = null; rebuildWidgets(); });
        var activeSlot = view.slots().stream().filter(s -> s.technology().equals(detail)).findFirst();
        String action = tech.status() == ResearchView.Status.ACTIVE ? "연구 중단"
                : tech.status() == ResearchView.Status.COMPLETED ? "연구 완료"
                : tech.status() == ResearchView.Status.LOCKED ? "선행 연구 필요"
                : view.availableSlot(selectedSlot)>=0 ? "연구" : "빈 슬롯 필요";
        String status = pending > 0 ? "응답 대기…" : action;
        var start = researchButton("연구", panelX + 179, panelY + 29, panelW - 184, 17, () -> {
            if (activeSlot.isPresent()) {
                pending = 100;
                requests.accept(new ResearchPayloads.Request(ResearchPayloads.Action.CANCEL, view.session(), view.revision(), activeSlot.get().index(), detail));
                detail = null; overview = true; rebuildWidgets();
            } else if (dev.hoi.client.ui.UiPriorityControls.shifted() || eligibleTeams().isEmpty()) selectTeam("");
            else { teamPicker = true; teamScroll = 0; rebuildWidgets(); }
        });
        String startTooltip = status;
        if (pending == 0 && tech.status() == ResearchView.Status.AVAILABLE && view.availableSlot(selectedSlot) >= 0) startTooltip += "\n" + researchTeamTooltip();
        if (!startTooltip.isEmpty()) start.setTooltip(Tooltips.createExact(startTooltip));
        if (activeSlot.isPresent() && !eligibleTeams().isEmpty()) {
            var teamControl=addRenderableWidget(new dev.hoi.client.ui.UiMenuButton(Tooltips.officerText(teamText("header")).getString(),panelX+79,panelY+30,100,15,()->{teamPicker=true;teamScroll=0;rebuildWidgets();}).caption("").background("none"));teamControl.active=pending==0;
        }
        ((UiPanelButton)start).sound("ui.click");
        var child = subTechnology(tech);
        if (child != null) {
            var tooltip=Component.empty();
            for(var line:tooltipLines(child)) {
                if(!tooltip.getString().isEmpty())tooltip.append("\n");
                tooltip.append(line);
            }
            addRenderableWidget(new UiPanelButton(child.name(),panelX+panelW-32,panelY+63,18,18,()->showDetail(child.id())) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
                    drawSubTechnology(g,child,getX(),getY(),getWidth());
                }
            }).setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(tooltip));
        }
        start.active = pending == 0 && (activeSlot.isPresent() || tech.status() == ResearchView.Status.AVAILABLE
                && view.availableSlot(selectedSlot)>=0);
        if (teamPicker) initTeams();
    }
    private List<ResearchView.DesignTeam> eligibleTeams() {
        return eligibleTeams(detail);
    }
    private List<ResearchView.DesignTeam> eligibleTeams(String technology) {
        return view.designTeams().stream().filter(t -> t.technologies().contains(technology)).toList();
    }
    ResearchView.DesignTeam preferredTeam(String technology) {
        ResearchView.DesignTeam best = null;
        for (var team : eligibleTeams(technology)) {
            double rate = team.rates().get(team.technologies().indexOf(technology));
            double bestRate = best == null ? Double.NEGATIVE_INFINITY : best.rates().get(best.technologies().indexOf(technology));
            if (best == null || rate > bestRate || rate == bestRate && team.size() > best.size()) best = team;
        }
        return best;
    }
    private boolean technologyClick(Tech tech, boolean shift, boolean alt) {
        if (!shift && !alt) { showDetail(tech.id()); return true; }
        if (pending > 0 || tech.status() != ResearchView.Status.AVAILABLE || view.availableSlot(selectedSlot) < 0) return true;
        var team = preferredTeam(tech.id());
        int slot = selectedSlot;
        waitingTeamSlot = slot; waitingTeam = team == null ? "" : team.id(); waitingTechnology = tech.id();
        selectNextSlot = shift && alt;
        detail = null; teamPicker = false; overview = !alt; pending = 100;
        requests.accept(new ResearchPayloads.Request(ResearchPayloads.Action.START, view.session(), view.revision(), slot, tech.id()));
        AudioUiSounds.play("ui.click"); rebuildWidgets(); return true;
    }
    private List<Component> interactionTooltip(Tech tech) {
        var team = preferredTeam(tech.id());
        var lines = new ArrayList<Component>();
        lines.add(Tooltips.officerText(UiCommandLabels.text("CLICK_DETAILS_CLICK", "§G클릭§!하여 세부 사항 보기")));
        String[] keys = {"SHIFT", "ALT", "SHIFT_ALT"};
        String[] plain = {"§GShift+클릭§!하여 연구", "§GAlt+클릭§!하여 연구하고 이 창을 열린 상태로 유지", "§GShift+Alt+클릭§!하여 연구하고 다음 빈 슬롯 선택"};
        String[] mio = {"§GShift+클릭§!하여 $ORG|H$(을)를 이용해 연구(규모 $SIZE|H$)", "§GAlt+클릭§!하여 $ORG|H$(을)를 이용해 연구하고 현재 창 유지", "§GShift+Alt+클릭§!하여 $ORG|H$(을)를 이용해 연구하고 다음 빈 연구 슬롯 선택"};
        for (int i = 0; i < keys.length; i++) {
            String text = UiCommandLabels.text("CLICK_DETAILS_" + keys[i] + "_RESEARCH" + (team == null ? "" : "_WITH_MIO"), team == null ? plain[i] : mio[i]);
            if (team != null) text = text.replace("$ORG|H$", "§H" + team.name() + "§!").replace("$SIZE|H$", "§H" + team.size() + "§!");
            lines.add(Tooltips.officerText(text));
        }
        if (detail != null) lines.add(Tooltips.officerText(UiCommandLabels.text("CLICK_DETAILS_CTRL", "§GCtrl+클릭§!하여 다른 창을 열어 놓은 채로 세부 사항 보기")));
        return lines;
    }
    String availableTeamsTooltip(String technology) {
        var rows = eligibleTeams(technology);
        if (rows.isEmpty()) return "";
        var lines = new ArrayList<String>();
        lines.add(teamText("available_title"));
        for (var team : rows) lines.add(teamText("available_item").replace("$NAME$", team.name()));
        return String.join("\n", lines);
    }
    private String teamText(String key) { return view.designTeamText().getOrDefault(key, ""); }
    private String researchTeamTooltip() {
        var rows = eligibleTeams(); var result = new ArrayList<String>();
        if (!rows.isEmpty()) {
            result.add(availableTeamsTooltip(detail));
            result.add("");
        }
        result.add(teamText(rows.isEmpty() ? "research" : "research_mio"));
        if (!rows.isEmpty()) { result.add(""); result.add(teamText("task_info")); }
        return String.join("\n", result);
    }
    String teamTooltip(ResearchView.DesignTeam team, String technology, boolean assign) {
        int index = team.technologies().indexOf(technology);
        String result = team.tooltip();
        if (index >= 0) result += "\n" + teamText("funds").replace("$VALUE$", Integer.toString(team.funds().get(index)));
        var busy = view.slots().stream().filter(slot -> slot.designTeam().equals(team.id()) && !slot.technology().equals(technology)).map(slot -> technologyName(slot.technology())).toList();
        if (!busy.isEmpty()) result += "\n\n" + teamText("busy").replace("$NAME$", team.name()) + "\n" + String.join("\n", busy);
        return assign ? result + "\n\n" + teamText("assign") : result;
    }
    long teamDays(Tech tech,int savedDays){
        var team=eligibleTeams().stream().filter(t->t.id().equals(hoveredTeam)).findFirst().orElse(null);
        return team==null?tech.remainingDays(savedDays):Math.max(1,(long)Math.floor((tech.baseDays()-tech.progress())/team.rates().get(team.technologies().indexOf(tech.id()))-savedDays));
    }
    private void drawTeamDays(GuiGraphicsExtractor g,Tech tech,int savedDays){
        long original=tech.remainingDays(savedDays),days=teamDays(tech,savedDays);
        boolean faster=days<original;String number=Long.toString(days);int textWidth=font.width(number)+font.width("일")+(faster?10:0),x=panelX+79+(112-textWidth)/2,y=panelY+33;
        g.text(font,number,x,y,faster?0xFF55FF55:GOLD,false);x+=font.width(number);
        if(faster){UiAssets.draw(g,"research/source/gfx_increase_reversed_texticon/frame_0",x+2,y+1,7,7);x+=10;}
        g.text(font,"일",x,y,TEXT,false);
    }
    private int descriptionHeight(Tech tech){return tech.source()==null||tech.source().description().isBlank()?0:detailText(null,List.of(ResearchText.keywords(tech.source().description())),0,0,panelW-28,TEXT);}
    private void selectTeam(String team) {
        if (pending > 0 || detail == null || !team.isEmpty() && eligibleTeams().stream().noneMatch(t -> t.id().equals(team))) return;
        var active = view.slots().stream().filter(slot -> slot.technology().equals(detail)).findFirst();
        int slot = active.map(ResearchView.Slot::index).orElse(selectedSlot);
        if (active.isEmpty() && view.availableSlot(selectedSlot) < 0) return;
        teamPicker = false; pending = 100;
        if (active.isEmpty()) {
            if (!team.isEmpty()) { waitingTeamSlot = slot; waitingTeam = team; waitingTechnology = detail; }
            requests.accept(new ResearchPayloads.Request(ResearchPayloads.Action.START, view.session(), view.revision(), slot, detail));
        } else requests.accept(new ResearchPayloads.Request(ResearchPayloads.Action.DESIGN_TEAM, view.session(), view.revision(), slot, team));
        detail = null; overview = true; rebuildWidgets();
    }
    private List<ResearchView.DesignTeam> sortedTeams() {
        var rows = new ArrayList<>(eligibleTeams());
        switch (teamSort) {
            case "size" -> rows.sort(Comparator.comparingInt(ResearchView.DesignTeam::size));
            case "name" -> rows.sort(Comparator.comparing(ResearchView.DesignTeam::name));
            case "cost" -> rows.sort(Comparator.comparingDouble(ResearchView.DesignTeam::dailyCost));
            default -> {}
        }
        if (teamDescending) Collections.reverse(rows);
        return rows;
    }
    private void initTeams() {
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && !baseButtons.contains(widget)) { widget.active = false; if(widget.visible)teamBackdrop.add(widget); widget.visible=false; }
        teamWidth = Math.min(IndustryOrganizationList.WIDTH, width - 16); teamY = panelY; teamHeight = Math.min(IndustryOrganizationList.HEIGHT, height - teamY - 4);
        teamX = Math.max(8, Math.min(width - teamWidth - 25, panelX + panelW - 2));
        addRenderableWidget(IndustryOrganizationList.close(teamX, teamY, teamWidth, () -> { teamPicker = false; rebuildWidgets(); }));
        String[] sorts = {"size", "name", "cost"};
        String[] labels = {"규모", UiCommandLabels.text("mio_sorting_name", "이름"), UiCommandLabels.text("mio_sorting_task_cost", "비용")};
        int[] sortX = {8, 23, 65}, sortWidth = {15, 41, 41};
        for (int i = 0; i < sorts.length; i++) {
            String key = sorts[i];
            var sort = new dev.hoi.client.ui.UiMenuButton(labels[i], teamX + sortX[i], teamY + 24, sortWidth[i], 15, () -> { teamDescending = teamSort.equals(key) && !teamDescending; teamSort = key; rebuildWidgets(); })
                    .background((i == 0 ? "mio/ui/gfx_sort_skill/frame_" : "mio/ui/gfx_sort_up_down_button_83x29/frame_") + (teamSort.equals(key) ? teamDescending ? 2 : 1 : 0)).sound("ui.scroll");
            if (i == 0) sort.caption("");
            sort.setTooltip(null); addRenderableWidget(sort);
        }
        int entry = IndustryOrganizationList.ENTRY_HEIGHT;
        var rows = sortedTeams(); int start = teamY + IndustryOrganizationList.LIST_TOP, end = teamY + teamHeight - 10;
        teamScroll = Math.clamp(teamScroll, 0, Math.max(0, entry + rows.size() * entry - (end - start)));
        if (teamScroll == 0) addRenderableWidget(new UiPanelButton(Tooltips.officerText(teamText("empty")).getString(),teamX+8,start,IndustryOrganizationList.ENTRY_WIDTH,entry,()->selectTeam("")){
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){IndustryOrganizationList.empty(g,font,teamText("empty"),getX(),getY());}
        });
        for (int i = 0; i < rows.size(); i++) {
            var team = rows.get(i); int y = start + entry + i * entry - teamScroll;
            if (y < start || y + entry > end) continue;
            teamDetails.add(addRenderableWidget(IndustryOrganizationList.details(font, team.id(), teamX + 8, y)));
            var row = addRenderableWidget(new UiPanelButton(team.name(), teamX + 8, y, IndustryOrganizationList.ENTRY_WIDTH, entry, () -> selectTeam(team.id())) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    IndustryOrganizationRow.draw(g,team.icon(),team.size(),team.name(),team.type(),team.equipment(),getX(),getY(),getWidth(),teamText("cost").replace("$VALUE$",String.format(Locale.ROOT,"%.2f",team.dailyCost())));
                }
            });
            row.active = pending == 0; teamRows.put(row, team.id());
        }
    }
    private UiScrollBar teamScrollbar() { int entry = IndustryOrganizationList.ENTRY_HEIGHT, start = teamY + IndustryOrganizationList.LIST_TOP, end = teamY + teamHeight - 10; return new UiScrollBar(teamX+teamWidth-11,start,end,end-start,entry+sortedTeams().size()*entry); }
    private void drawTeams(GuiGraphicsExtractor g) {
        g.nextStratum();
        IndustryOrganizationList.frame(g, font, teamX, teamY, teamWidth, teamHeight);
        teamScrollbar().draw(g,teamScroll);
    }
    @Override public void tick() { if (pending > 0 && --pending == 0) { waitingTeamSlot = -1; waitingTeam = ""; waitingTechnology = ""; selectNextSlot = false; rebuildWidgets(); } }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.nextStratum();
        if (overview) { drawOverview(g); HudMenuBar.draw(g, width, view.hud(), mouseX, mouseY, hudScroll); super.extractRenderState(g, mouseX, mouseY, delta); overviewTooltip(g,mouseX,mouseY); return; }
        HudMenuBar.drawBackground(g, width, view.hud(), hudScroll);
        g.nextStratum();
        g.fill(0, menuBottom(), width, height, 0xFF000000);
        UiAssets.tiledNineSlice(g, "research/source/gfx_tiled_plain_bg2/frame_0", 0, menuBottom(), width, height - menuBottom(), 64, .5);
        UiAssets.tiledNineSlice(g, "research/source/gfx_tiled_window_3b_border/frame_0", 0, menuBottom() + 24, width, height - menuBottom() - 24, 64, .5);
        g.nextStratum();
        drawTree(g, mouseX, mouseY);
        if (detail != null && !view.message().isEmpty()) g.text(font, trim(view.message(), width - 24), 12, height - 12, MUTED);
        terrainMouseX = mouseX; terrainMouseY = mouseY; terrainTooltip = "";
        if (detail != null) {
            boolean covered = overDetailWindows(mouseX, mouseY);
            for (var button : baseButtons) button.extractRenderState(g, covered ? -1 : mouseX, covered ? -1 : mouseY, delta);
            hoveredTeam = teamRows.entrySet().stream().filter(e -> e.getKey().isMouseOver(mouseX, mouseY)).map(Map.Entry::getValue).findFirst().orElse(""); drawDetail(g);
        }
        if (teamPicker) { for(var widget:teamBackdrop){widget.visible=true;widget.extractRenderState(g,-1,-1,delta);widget.visible=false;} drawTeams(g); }
        if (detail == null) super.extractRenderState(g, mouseX, mouseY, delta);
        else for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget && !baseButtons.contains(child)) widget.extractRenderState(g, mouseX, mouseY, delta);
        if (teamPicker) for (var widget : teamDetails) widget.extractRenderState(g,mouseX,mouseY,delta);
        if (teamPicker) for (var entry : teamRows.entrySet()) if (entry.getKey().isMouseOver(mouseX,mouseY) && teamDetails.stream().noneMatch(w -> w.isMouseOver(mouseX,mouseY))) {
            var team = view.designTeams().stream().filter(t -> t.id().equals(entry.getValue())).findFirst().orElseThrow();
            Tooltips.drawSourceScrollable(g,font,teamTooltip(team,detail,true),teamX+teamWidth,mouseY,width,height,0);
        }
        HudMenuBar.drawTooltip(g, width, view.hud(), mouseY < menuBottom() ? mouseX : -1, mouseY, hudScroll);
    }
    private void drawOverview(GuiGraphicsExtractor g) {
        int pane = overviewWidth(), top = menuBottom(), bannerY = top + 39;
        UiMenuStyle.panel(g, 0, top, pane, height - top);
        UiMenuStyle.heading(g, font, "연구", 12, top + 9, pane - 44, UiMenuStyle.TEXT);
        g.fillGradient(6, bannerY, pane - 6, bannerY + bannerHeight(), 0xFF4E315E, 0xFF172731);
        if (!UiAssets.cover(g, "panel/research_banner", 6, bannerY, pane - 12, bannerHeight())) {
            ResearchIcons.fallback(g, "ENGINEERING", pane / 2 - 10, bannerY + bannerHeight() / 2 - 8, 0xFFBB98CD, 2);
        }
        g.outline(6, bannerY, pane - 12, bannerHeight(), 0xFFA070B5);
        researchIndicator(g,"제한된 혜택:","research/limited_benefits",Integer.toString(view.benefits().remainingUses()),12,slotsTop()-33,pane/2-18,GOLD,false);
        double researchSpeed=view.benefits().researchSpeed();
        researchIndicator(g,"연구 속도:","research/speed",dev.hoi.protocol.ProtocolNumberBounds.signedPercent(researchSpeed, 0),pane/2+6,slotsTop()-33,pane/2-18,0xFF000000 | (researchSpeed>=0?dev.hoi.client.ui.tooltip.Tooltips.GOOD:dev.hoi.client.ui.tooltip.Tooltips.BAD),true);
    }
    private void researchIndicator(GuiGraphicsExtractor g,String label,String icon,String value,int x,int y,int max,int color,boolean right) {
        int natural=font.width(label)+15+font.width(value);
        float scale=Math.min(1f,max/(float)Math.max(1,natural));
        g.pose().pushMatrix();g.pose().translate(right?x+max-natural*scale:x,y + 16.5f - 3.5f * scale);g.pose().scale(scale);
        g.text(font,label,0,0,TEXT);UiAssets.draw(g,icon,font.width(label)+2,-2,11,11);
        g.text(font,value,font.width(label)+15,0,color);g.pose().popMatrix();
    }
    private void drawTree(GuiGraphicsExtractor g, int mx, int my) {
        g.enableScissor(0, treeTop, width, treeTop + treePadding + 16);
        for (int i = 0; !layout.sourceTree() && i < layout.years().size(); i++) {
            int x = (int)(30 + i * ResearchLayout.COLUMN - scrollX);
            g.text(font, String.valueOf(layout.years().get(i)), x, treeTop + treePadding + 3, GOLD);
        }
        g.disableScissor();
        g.enableScissor(0, treeTop, width, treeBottom);
        boolean sourceBackground = drawSourceBackground(g);
        for (int i = 0; !layout.sourceTree() && i <= layout.years().size(); i++) {
            int x = (int)(20 + i * ResearchLayout.COLUMN - scrollX);
            g.verticalLine(x, treeTop, treeBottom, 0xFF1C2421);
        }
        boolean reference = ResearchSourceLayout.applies(layout);
        if (reference && !sourceBackground) drawReferenceGuides(g);
        if(layout.sourceTree() && !reference) for(var guide:layout.yearGuides().entrySet()) {
            int gx=layout.vertical()?14:(int)(guide.getValue()-scrollX);
            int gy=layout.vertical()?(int)(treeTop+guide.getValue()-scrollY):treeTop+treePadding+2;
            if(gy>=treeTop&&gy<treeBottom) g.text(font,guide.getKey()+"년",gx,gy,GOLD);
        }
        var byId = nodesById;
        if (reference) drawReferenceConnections(g);
        if (!reference) for (var node : layout.nodes()) for (var parentId : ResearchLayout.parents(node.tech())) {
            var parent = byId.get(parentId); if (parent == null) continue;
            int x1 = (int)(parent.x() + parent.width() - scrollX), y1 = (int)(treeTop + parent.y() + parent.height()/2 - scrollY);
            int x2 = (int)(node.x() - scrollX), y2 = (int)(treeTop + node.y() + node.height()/2 - scrollY);
            if(layout.vertical()) {
                x1=(int)(parent.x()+parent.width()/2-scrollX);y1=(int)(treeTop+parent.y()+parent.height()-scrollY);
                x2=(int)(node.x()+node.width()/2-scrollX);y2=(int)(treeTop+node.y()-scrollY);
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
        ResearchLayout.ResearchNode hovered = null;
        String hoveredTeams = "";
        for (var n : layout.nodes()) {
            int x = (int)(n.x() - scrollX), y = (int)(treeTop + n.y() - scrollY);
            int cardW=n.width(),cardH=n.height();
            if (x + cardW < 0 || x > width || y + cardH < treeTop || y > treeBottom) continue;
            var tech = n.tech(); boolean hover = !teamPicker && (detail == null || mx < panelX || mx >= panelX + panelW || my < panelY || my >= panelY + panelH)
                    && insideTree(mx, my) && n.contains(mx + scrollX, my - treeTop + scrollY);
            if (hover) hovered = n;
            g.fill(x, y, x + cardW, y + cardH, hover ? 0xFF2B3C49 : 0xFF1B2933);
            g.outline(x, y, cardW, cardH, focusedTech != null && focusedTech.equals(tech.id()) ? TEXT : color(tech.status()));
            UiAssets.technology(g, view.country(), tech, x + 2, y + 2, cardW - 4, cardH - 4, color(tech.status()));
            var child = subTechnology(tech);
            if (child != null) {
                int size=subTechnologySize(n);
                drawSubTechnology(g,child,x+cardW-size,y,size);
            }
            if (tech.status() == ResearchView.Status.ACTIVE) {
                g.fill(x + 2, y + cardH - 4, x + cardW - 2, y + cardH - 1, 0xFF0D151A);
                g.fill(x + 2, y + cardH - 4, x + 2 + (int)((cardW - 4) * tech.fraction()), y + cardH - 1, color(tech.status()));
            }
            if (!eligibleTeams(tech.id()).isEmpty()) {
                int iconWidth = Math.max(1, (int)Math.round(10 * n.scale())), iconHeight = iconWidth;
                UiAssets.draw(g, "mio/ui/gfx_design_team_icon/frame_0", x, y + cardH - iconHeight, iconWidth, iconHeight);
                if (hover && mx < x + iconWidth && my >= y + cardH - iconHeight) hoveredTeams = availableTeamsTooltip(tech.id());
            }
        }
        for (var label : referenceLabels) {
            int x = (int)(label.x() - scrollX), y = (int)(treeTop + label.y() - scrollY);
            g.text(font, label.text(), x, y, label.year() ? TEXT : MUTED);
        }
        if (reference) drawReferenceCaptions(g);
        if (layout.nodes().isEmpty()) g.text(font, "이 분야에 표시할 기술이 없습니다.", 28, treeTop + 28, MUTED);
        g.disableScissor();
        if (!hoveredTeams.isEmpty()) {
            Tooltips.drawExact(g, font, hoveredTeams, mx, my);
        } else if (hovered != null) {
            var t = hovered.tech();
            var child=subTechnologyAt(mx,my);
            if(child!=null)t=child;
            g.setTooltipForNextFrame(font, hoverLines(t), mx, my);
        }
    }
    List<net.minecraft.util.FormattedCharSequence> hoverLines(Tech tech) {
        var source = tooltipLines(tech); int instructions = source.size() - interactionTooltip(tech).size();
        var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        for (int i = 0; i < source.size(); i++) {
            var line = i < instructions ? Tooltips.format(source.get(i)) : source.get(i);
            if (line.getString().isBlank()) lines.add(net.minecraft.util.FormattedCharSequence.EMPTY);
            else lines.addAll(Tooltips.split(font, Tooltips.sentences(ResearchText.decorateEffect(line)), Tooltips.width(width, 14)));
        }
        return Tooltips.collapseBlankLines(lines);
    }
    public Tech subTechnology(Tech parent) {
        return view.technologies().stream().filter(t -> t.source()!=null && t.source().parentTechnology().equals(parent.id())).findFirst().orElse(null);
    }
    private int subTechnologySize(ResearchLayout.ResearchNode node) { return Math.max(12,(int)Math.round(12*node.scale())); }
    private Tech subTechnologyAt(double mouseX,double mouseY) {
        double x=mouseX+scrollX,y=mouseY-treeTop+scrollY;
        for(var node:layout.nodes()) {
            int size=subTechnologySize(node);
            if(x>=node.x()+node.width()-size && x<node.x()+node.width() && y>=node.y() && y<node.y()+size) {
                var child=subTechnology(node.tech());
                if(child!=null)return child;
            }
        }
        return null;
    }
    private void drawSubTechnology(GuiGraphicsExtractor g,Tech tech,int x,int y,int size) {
        String state=switch(tech.status()) { case COMPLETED->"researched";case AVAILABLE,ACTIVE->"available";default->"unavailable"; };
        UiAssets.draw(g,"research/subtechnology/"+state,x,y,size,size);
        UiAssets.draw(g,"research/subtechnology/"+(tech.category().equals("NAVY")?"battlecruiser":"rocket"),x,y,size,size);
        if(tech.status()==ResearchView.Status.ACTIVE)g.outline(x,y,size,size,color(tech.status()));
    }
    private void drawReferenceCaptions(GuiGraphicsExtractor g) {
        g.nextStratum();
        for (var group : ResearchSourceLayout.groups(layout)) {
            if (group.caption().isEmpty()) continue;
            int x = (int)(group.x() + group.width() + 5 - scrollX);
            int center = (int)(treeTop + group.y() + group.height() / 2 - scrollY);
            var lines = dev.hoi.client.ui.tooltip.Tooltips.split(font,Component.literal(group.caption()), 90);
            int y = center - lines.size() * font.lineHeight / 2;
            for (var line : lines) {
                g.text(font, line, x, y, TEXT);
                y += font.lineHeight;
            }
        }
    }
    private boolean drawSourceBackground(GuiGraphicsExtractor g) {
        String sprite = switch (category) {
            case "INFANTRY" -> "infantry_techtree_bg";
            case "SUPPORT" -> "support_techtree_bg";
            case "ARMOR" -> "armortech_bg";
            case "ARTILLERY" -> "artillery_techtree_bg";
            case "NAVY", "NAVAL_SUPPORT" -> "naval_techtree_bg";
            case "AIR" -> "air_techtree_bg";
            case "ENGINEERING" -> "engineering_techtree_bg";
            case "INDUSTRY" -> "industry_techtree_bg";
            default -> "";
        };
        if (sprite.isEmpty()) return false;
        String path = "research/source/gfx_" + sprite + "/atlas";
        int[] size = UiAssets.imageSize(path);
        if (size[0] == 0) return false;
        double scale = ResearchSourceLayout.scale(layout) * 0.5;
        int x = (int)(10 - scrollX), y = (int)(treeTop + treePadding - scrollY);
        if (category.equals("ARMOR")) UiAssets.tiledNineSlice(g, "research/source/gfx_techtree_stripes/atlas",
                x, y, Math.max(layout.width(), width), Math.max(layout.height(), treeBottom - treeTop), 0, scale);
        int layers = category.equals("AIR") ? 3 : category.equals("ENGINEERING") ? 1 : 2;
        for (int layer = 0; layer < layers; layer++)
            UiAssets.draw(g, path, x, y, (int)Math.round(size[0] * scale), (int)Math.round(size[1] * scale));
        return true;
    }
    private void drawReferenceGuides(GuiGraphicsExtractor g) {
        double scale = ResearchSourceLayout.scale(layout);
        for (int row = 0; layout.vertical() && row < (int)Math.ceil(layout.height() / (44 * scale)); row++) if (row % 2 == 0) {
            int y = (int)(treeTop + treePadding + (18 + row * 44) * scale - scrollY);
            g.fill(0, y, width, y + (int)Math.round(44 * scale), 0xFF131313);
        }
    }
    private void drawReferenceConnections(GuiGraphicsExtractor g) {
        for (var group : ResearchSourceLayout.groups(layout)) {
            int x=(int)(group.x()-scrollX), y=(int)(treeTop+group.y()-scrollY);
            g.fill(x,y,x+group.width(),y+group.height(),0x552E210B);
            g.outline(x,y,group.width(),group.height(),0xFF9F8153);
        }
        for (var connection : referenceConnections) {
            int tint = connection.parent().status() == ResearchView.Status.COMPLETED ? 0xFF567D57 : 0xFF49525A;
            for (var segment : connection.segments()) {
                int x1 = (int)(segment.x1() - scrollX), y1 = (int)(treeTop + segment.y1() - scrollY);
                int x2 = (int)(segment.x2() - scrollX), y2 = (int)(treeTop + segment.y2() - scrollY);
                g.fill(Math.min(x1, x2) - 1, Math.min(y1, y2) - 1, Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, tint);
            }
        }
        for (var choice : ResearchSourceLayout.choices(layout)) {
            int x = (int)(choice.x() - scrollX), y = (int)(treeTop + choice.y() - scrollY);
            int size = (int)Math.round(24 * ResearchSourceLayout.scale(layout));
            String first = choice.horizontal() ? "left" : "up", second = choice.horizontal() ? "right" : "down";
            UiAssets.draw(g, "research/xor_" + first, x - size/2, y - size/2, size, size);
            UiAssets.draw(g, "research/xor_" + second, x - size/2, y - size/2, size, size);
        }
    }
    private void drawDetail(GuiGraphicsExtractor g) {
        g.nextStratum();
        g.fill(panelX + 3, panelY + 3, panelX + panelW - 3, panelY + panelH - 3, 0xFF000000);
        UiAssets.nineSlice(g, "research/source/gfx_tiled_window_thin_border2/frame_0", panelX, panelY, panelW, panelH, 5, .5);
        UiAssets.nineSlice(g, "research/source/gfx_tiled_paper_bg/frame_0", panelX + 5, panelY + detailHeader, panelW - 10, panelH - detailHeader - 5, 6, .5);
        UiAssets.drawGrayscale(g, "research/source/gfx_tech_info_top_win/frame_0", panelX, panelY, panelW, detailHeader);
        var tech = view.technology(detail);
        UiText.centeredSingleLine(g, font, Tooltips.officerText(tech.name(), true), panelX + 20, panelY + 4, panelW - 40, 13, TEXT);
        String art = UiAssets.technologyPath(view.country(), tech);
        if (!art.isEmpty()) UiAssets.drawContent(g, art, panelX + 18, panelY + 20, 38, 36);
        UiAssets.nineSlice(g,"research/source/days_background",panelX+80,panelY+31,110,12,2,.5);
        UiAssets.draw(g,"research/source/gfx_techtree_details_progress_bar_frame/frame_0",panelX+79,panelY+30,112,15);
        int bank = view.slots().stream().filter(slot -> slot.technology().equals(tech.id())).findFirst()
                .map(ResearchView.Slot::savedDays).orElse(view.slots().get(selectedSlot).savedDays());
        drawTeamDays(g,tech,bank);
        if (tech.source() != null && !tech.source().description().isBlank()) {
            descriptionBar = new dev.hoi.client.ui.UiScrollBar(panelX + panelW - 13, panelY + 57, panelY + detailHeader - 4, detailHeader - 63, descriptionHeight(tech));
            descriptionScroll = Math.clamp(descriptionScroll, 0, descriptionBar.maximum());
            g.enableScissor(panelX + 10, panelY + 57, panelX + panelW - 10, panelY + detailHeader - 4);
            detailText(g, List.of(ResearchText.keywords(tech.source().description())), panelX + 10, panelY + 59 - descriptionScroll, panelW - 28, TEXT);
            g.disableScissor();
            descriptionBar.draw(g, descriptionScroll);
        } else descriptionBar = null;
        int contentHeight = panelH - detailHeader - 12;
        int total = detailBody(null, tech, 0);
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, total - contentHeight));
        g.enableScissor(panelX + 10, panelY + detailHeader + 2, panelX + panelW - 10, panelY + panelH - 10);
        detailBody(g, tech, panelY + detailHeader + 2 - detailScroll);
        g.disableScissor();
        if (!terrainTooltip.isEmpty()) Tooltips.drawSourceScrollable(g, font, terrainTooltip, terrainMouseX, terrainMouseY, width, height, 0);
        detailBar = new dev.hoi.client.ui.UiScrollBar(panelX + panelW - 13, panelY + detailHeader + 2, panelY + panelH - 10, contentHeight, total);
        detailBar.draw(g, detailScroll);
    }
    private int sectionHeading(GuiGraphicsExtractor g, String text, int y) {
        if (g != null) g.centeredText(font, Tooltips.officerText(text, true), panelX + panelW / 2, y + 5, TEXT);
        return y + 22;
    }
    private int detailBody(GuiGraphicsExtractor g, Tech tech, int y) {
        var intro = detailLines(tech);
        if (tech.source() != null && !tech.source().description().isBlank()) intro = intro.subList(2, intro.size());
        y = detailText(g, intro, panelX + 14, y, panelW - 30, TEXT);
        var cards = ResearchDetails.cards(tech, view.country());
        if (!cards.isEmpty()) {
            for (var card : cards) if (!card.kind().equals("부대")) y = detailCard(g, card, y + 4);
        }
        var units = cards.stream().filter(card -> card.kind().equals("부대") && tech.units().stream().noneMatch(unit -> unit.name().equals(card.name()))).toList();
        var otherUnlocks = tech.units().isEmpty() && cards.isEmpty() ? unlockNames(tech) : List.<String>of();
        if (!tech.upgrades().isEmpty()) {
            y = sectionHeading(g, UiCommandLabels.text("RESEARCH_UPGRADES_HEADER", "업그레이드"), y);
            for (var group : upgradeGroups(tech.upgrades())) {
                var upgrade = group.getFirst();
                String scope = upgrade.scopeId();
                int columns = Math.max(1, (panelW - 28) / 35);
                if (g != null) {
                    for (int index = 0; index < group.size(); index++) {
                        String id = group.get(index).scopeId();
                        String art = "research/detail/gfx_unit_" + id + (id.startsWith("category_") ? "" : "_icon_medium") + "/frame_0";
                        UiAssets.drawContent(g, art, panelX + 14 + index % columns * 35, y + 4 + index / columns * 26, 30, 20);
                    }
                    if (scope.startsWith("category_")) detailText(g, List.of(ResearchText.white(upgrade.scope())), panelX + 48, y + 6, panelW - 62, TEXT);
                }
                y += scope.startsWith("category_") ? 32 : (group.size() + columns - 1) / columns * 26;
                y = sectionStats(g, upgrade.lines(), y);
                y = terrainCards(g, upgrade.terrain(), y);
            }
        }
        if (!units.isEmpty() || !otherUnlocks.isEmpty() || !tech.units().isEmpty()) {
            y = sectionHeading(g, UiCommandLabels.text("RESEARCH_UNLOCKS_HEADER", "잠금 해제"), y);
            for (var card : units) y = detailCard(g, card, y + 4);
            for (var unit : tech.units()) {
                if (g != null) UiAssets.drawContent(g, "research/detail/gfx_unit_" + unit.id() + "_icon_medium/frame_0", panelX + 14, y + 4, 35, 22);
                y += 30;
                y = sectionStats(g, unit.stats(), y);
                y = terrainCards(g, unit.terrain(), y);
            }
            y = detailText(g, otherUnlocks.stream().map(this::unlockName).toList(), panelX + 14, y + 4, panelW - 30, TEXT);
        }
        if (tech.source() != null && !tech.source().deferredEffects().isEmpty()) {
            y = detailText(g, List.of(Component.empty()), panelX + 14, y, panelW - 30, MUTED);
            y = detailText(g, tech.source().deferredEffects().stream().map(ResearchText::effect).toList(), panelX + 14, y, panelW - 30, TEXT);
        }
        return y;
    }
    static List<List<ResearchView.Upgrade>> upgradeGroups(List<ResearchView.Upgrade> upgrades) {
        var groups = new ArrayList<List<ResearchView.Upgrade>>();
        for (var upgrade : upgrades) {
            var previous = groups.isEmpty() ? null : groups.getLast().getFirst();
            if (previous == null || previous.scopeId().isEmpty() || upgrade.scopeId().isEmpty()
                    || previous.scopeId().startsWith("category_") || upgrade.scopeId().startsWith("category_")
                    || !previous.lines().equals(upgrade.lines()) || !previous.terrain().equals(upgrade.terrain())) {
                groups.add(new ArrayList<>());
            }
            groups.getLast().add(upgrade);
        }
        return groups;
    }
    private int sectionStats(GuiGraphicsExtractor g, List<ResearchView.StatLine> stats, int y) {
        for (var stat : stats) {
            var label = TooltipKeywordIcons.decorateLines(Tooltips.officerText(stat.name() + ":"));
            var value = Tooltips.officerText((stat.value().startsWith("+") ? "§G" : stat.value().startsWith("-") ? "§R" : "§Y") + stat.value());
            var lines = font.split(label, panelW - 108);
            if (g != null) {
                g.fill(panelX + panelW / 2, y, panelX + panelW / 2 + 1, y + Math.max(10, lines.size() * 10), 0x885B2929);
                g.fill(panelX + panelW - 22, y, panelX + panelW - 21, y + Math.max(10, lines.size() * 10), 0x885B2929);
                for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), panelX + 38, y + i * 10, TEXT, false);
                g.text(font, value, panelX + panelW - 44 - font.width(value), y, TEXT, false);
            }
            y += Math.max(10, lines.size() * 10);
        }
        return y;
    }
    private int terrainCards(GuiGraphicsExtractor g, List<ResearchView.TerrainValue> terrains, int y) {
        int perRow = Math.max(1, (panelW - 28) / 42);
        for (int i = 0; i < terrains.size(); i++) {
            var terrain = terrains.get(i); int x = panelX + 14 + i % perRow * 42, top = y + i / perRow * 43;
            if (g == null) continue;
            UiAssets.draw(g, "production/design/adjusters/" + terrain.id(), x + 9, top + 5, 20, 20);
            UiAssets.draw(g, "production/design/controls/adjuster_frame", x + 3, top + 2, 32, 29);
            Double[] values = {terrain.movement(), terrain.attack(), terrain.defence()};
            boolean navy = terrain.id().startsWith("water_");
            String[] types = navy ? new String[]{"ship_movement", "attack_defense", "other"} : new String[]{"movement", "attack", "defense"};
            Double[] indicators = values.clone();
            if (navy) {
                indicators[1] = values[1] == null ? values[2] : values[2] == null ? values[1] : Math.min(values[1], values[2]) < 0 ? Math.min(values[1], values[2]) : Math.max(values[1], values[2]);
                indicators[2] = null;
            }
            for (int j = 0; j < 3; j++) {
                String key = "production/design/controls/" + types[j];
                String state = indicators[j] == null || Math.abs(indicators[j]) <= 1e-8 ? "neutral" : indicators[j] > 0 ? "positive" : "negative";
                float iconSize = 21 / 2f;
                UiAssets.drawAlignedContent(g, key + "_" + state, key + "_neutral", x + 3 + (12.5f + j * 22.5f) * 32 / 70 - iconSize / 2, top + 2 + 49.5f * 28 / 62 - iconSize / 2, iconSize, iconSize);
            }
            if (terrainMouseX >= x && terrainMouseX < x + 40 && terrainMouseY >= top && terrainMouseY < top + 42 && terrainMouseY >= panelY + 88 && terrainMouseY < panelY + panelH - 12) {
                var tooltip = new StringBuilder(terrain.name()); String[] names = {"이동", "공격", "방어"};
                for (int j = 0; j < 3; j++) if (values[j] != null) tooltip.append("\n").append(names[j]).append(": ").append(values[j] < 0 ? "§R" : "§G").append(String.format(Locale.ROOT, "%+.1f%%", values[j] * 100)).append("§!");
                terrainTooltip = tooltip.toString();
            }
        }
        return y + ((terrains.size() + perRow - 1) / perRow) * 43;
    }
    private int terrainMouseX, terrainMouseY;
    private String terrainTooltip = "";
    private int detailText(GuiGraphicsExtractor g, List<? extends Component> lines, int x, int y, int w, int color) {
        for (var line : dev.hoi.client.ui.tooltip.TooltipText.collapseBlankLines(lines,row->dev.hoi.client.ui.tooltip.TooltipText.blank(row.getString()))) {
            var wrapped = dev.hoi.client.ui.tooltip.Tooltips.split(font,ResearchText.decorateEffect(line), w);
            if (wrapped.isEmpty()) { y += 10; continue; }
            for (var text : wrapped) { if (g != null) g.text(font, text, x, y, color); y += 10; }
        }
        return y;
    }
    private int detailCard(GuiGraphicsExtractor g, ResearchDetails.ResearchCard card, int y) {
        int x=panelX+14,w=panelW-30;
        boolean image=!card.texture().isEmpty();
        if(g!=null){
            if(image)UiAssets.drawContent(g,card.texture(),x+5,y+4,64,38);
            detailText(g,List.of(ResearchText.gold(card.name())),x+(image?76:7),y+7,w-(image?83:14),TEXT);
        }
        y+=image?45:24;
        for(String effect:card.effects()){
            if(ResearchText.zeroEffect(effect))continue;
            var formatted=ResearchText.effect(effect);String value=formatted.getString();int split=value.lastIndexOf(':');
            if(split<0){y=detailText(g,List.of(formatted),x+7,y,w-14,TEXT);continue;}
            var label=TooltipKeywordIcons.decorateLines(ResearchText.white(value.substring(0,split+1)),true);
            var lines=font.split(label,w-95);String number=value.substring(split+1).strip();
            int row=Math.max(10,lines.size()*10);
            if(g!=null){
                for(int i=0;i<lines.size();i++)g.text(font,lines.get(i),x+19,y+i*10,TEXT,false);
                g.fill(panelX+panelW/2,y,panelX+panelW/2+1,y+row,0xFF583639);
                g.fill(panelX+panelW-22,y,panelX+panelW-21,y+row,0xFF583639);
                g.text(font,number,panelX+panelW-44-font.width(number),y,GOLD,false);
            }
            y+=row;
        }
        return y+8;
    }
    public List<Component> detailLines(Tech tech) {
        var result = new ArrayList<Component>();
        if (tech.source() != null && !tech.source().description().isBlank()) {
            result.add(ResearchText.keywords(tech.source().description())); result.add(Component.empty());
        }
        if (tech.source() != null) {
            if (!tech.source().excludes().isEmpty()) {
                result.add(ResearchText.white(UiCommandLabels.text("RESEARCH_EXCLUSIVE_INFO_TITLE_TOOLTIP", "")));
                for (var id : tech.source().excludes()) result.add(ResearchText.white(UiCommandLabels.text("RESEARCH_EXCLUSIVE_INFO_ENTRY", "$TECH|Y$").replace("$TECH|Y$", "§Y" + researchName(id) + "§!")));
                result.add(Component.empty());
            }
            tech.source().conditions().stream().map(ResearchText::white).forEach(result::add);
        }
        var effects = tech.effects().stream().filter(text -> !ResearchText.zeroEffect(text) && !text.endsWith(" 해금") && !tech.tooltipOnlyEffects().contains(text)).toList();
        if (!effects.isEmpty() && tech.upgrades().isEmpty()) {
            result.add(ResearchText.gold("효과:")); effects.stream().map(ResearchText::effect).forEach(result::add); result.add(Component.empty());
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
        var missing = new ArrayList<List<String>>();
        for (String id : tech.prerequisites()) if (!completed(id)) missing.add(List.of(technologyName(id)));
        if (tech.source() != null && !tech.source().anyOf().isEmpty()
                && tech.source().anyOf().stream().noneMatch(this::completed))
            missing.add(tech.source().anyOf().stream().map(this::technologyName).toList());
        if (tech.status() != ResearchView.Status.COMPLETED && !missing.isEmpty()) {
            var requirement = Component.literal("기술 필요 (").withColor(Tooltips.BAD);
            for (int index = 0; index < missing.size(); index++) {
                if (index > 0) requirement.append(Component.literal(", ").withColor(Tooltips.BAD));
                var names = missing.get(index);
                for (int name = 0; name < names.size(); name++) {
                    if (name > 0) requirement.append(Component.literal(" 또는 ").withColor(Tooltips.BAD));
                    requirement.append(Component.literal(names.get(name)).withColor(Tooltips.GOLD));
                }
            }
            lines.add(requirement.append(Component.literal(")").withColor(Tooltips.BAD)));
        }
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
        tech.effects().stream().filter(text -> !ResearchText.zeroEffect(text)).map(ResearchText::effect).forEach(lines::add);
        for (String label : unlockNames(tech)) lines.add(ResearchText.white("사용 가능 ").append(unlockName(label)));
        if (tech.source() != null && !tech.source().deferredEffects().isEmpty()) {
            tech.source().deferredEffects().stream().map(ResearchText::effect).forEach(lines::add);
        }
        lines.add(Component.literal(UiCommandLabels.text("SEPARATION_LINE","--------------\n").stripTrailing()));
        lines.addAll(interactionTooltip(tech));
        return lines;
    }
    private boolean completed(String id) { var tech = view.technology(id); return tech != null && tech.status() == ResearchView.Status.COMPLETED; }
    private String technologyName(String id) { var tech = view.technology(id); return tech == null ? id : tech.name(); }
    private String researchName(String id) {
        var t=view.technology(id);
        return (t!=null&&t.status()==ResearchView.Status.COMPLETED?"✓ ":"○ ")+(t==null?id:t.name());
    }
    private boolean insideTree(double x, double y) { return !overview && x >= 0 && x < width && y >= treeTop && y < treeBottom; }
    boolean overDetailWindows(double x, double y) {
        return detail != null && x >= panelX && x < panelX + panelW && y >= panelY && y < panelY + panelH
                || teamPicker && x >= teamX && x <= teamX + teamWidth && y >= teamY && y <= teamY + teamHeight;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (detail != null && !overDetailWindows(event.x(), event.y()))
            for (var button : baseButtons) if (button.isMouseOver(event.x(), event.y())) return button.mouseClicked(event, twice);
        if (detail != null && !teamPicker && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            for (var bar : new dev.hoi.client.ui.UiScrollBar[]{descriptionBar, detailBar}) if (bar != null && bar.contains(event.x(), event.y())) {
                boolean description = bar == descriptionBar;
                int offset = description ? descriptionScroll : detailScroll;
                if (bar.arrow(event.y())) offset = bar.step(event.y(), offset, 26);
                else { descriptionDragging = description; detailDragging = !description; offset = bar.offset(event.y()); }
                if (description) descriptionScroll = offset; else detailScroll = offset;
                return true;
            }
        }
        if(teamPicker&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&teamScrollbar().contains(event.x(),event.y())){if(teamScrollbar().arrow(event.y()))teamScroll=teamScrollbar().step(event.y(),teamScroll,IndustryOrganizationList.ENTRY_HEIGHT);else{teamDragging=true;teamScroll=teamScrollbar().offset(event.y());}rebuildWidgets();return true;}
        if (teamPicker) { if (event.x() < teamX || event.x() > teamX + teamWidth || event.y() < teamY || event.y() > teamY + teamHeight) return true; return super.mouseClicked(event, twice); }
        if (event.y() < menuBottom()) return super.mouseClicked(event, twice);
        if ((detail == null || event.x() < panelX || event.x() >= panelX + panelW || event.y() < panelY || event.y() >= panelY + panelH) && insideTree(event.x(), event.y())) {
            clearFocus();
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT || event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE) { panning = detail == null; return true; }
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
                var child=subTechnologyAt(event.x(),event.y());
                boolean shift = event.hasShiftDown();
                boolean alt = event.hasAltDown();
                if(child!=null)return technologyClick(child,shift,alt);
                var node = layout.at(event.x() + scrollX, event.y() - treeTop + scrollY);
                if (node != null) return technologyClick(node.tech(),shift,alt);
            }
        }
        if (detail != null && (event.x() < panelX || event.x() > panelX + panelW || event.y() < panelY || event.y() > panelY + panelH)) return true;
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (descriptionDragging && descriptionBar != null) { descriptionScroll = descriptionBar.offset(event.y()); return true; }
        if (detailDragging && detailBar != null) { detailScroll = detailBar.offset(event.y()); return true; }
        if(teamDragging){teamScroll=teamScrollbar().offset(event.y());rebuildWidgets();return true;}
        if (panning && detail == null) { scrollX -= dx; scrollY -= dy; clampScroll(); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { if(descriptionDragging || detailDragging){descriptionDragging=false;detailDragging=false;return true;} if(teamDragging){teamDragging=false;return true;} panning = false; return super.mouseReleased(event); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (slotTooltipMaximum>0 && !slotTooltipAt(x,y).isEmpty()) { slotTooltipScroll = Math.clamp(slotTooltipScroll-(int)(vertical*20),0,slotTooltipMaximum); return true; }
        if (x >= 40 && x < HudMenuBar.statsRight(width) && y >= 0 && y < HudMenuBar.STATS_HEIGHT) {
            hudScroll = HudMenuBar.scroll(width, view.hud(), hudScroll, horizontal, vertical);
            return true;
        }
        if (overview && x < overviewWidth() && y >= slotsTop() && view.slots().size() > visibleSlots()) {
            slotOffset -= (int)Math.signum(vertical); rebuildWidgets(); return true;
        }
        if (teamPicker) { teamScroll -= (int)(vertical * IndustryOrganizationList.ENTRY_HEIGHT); rebuildWidgets(); return true; }
        if (detail != null) {
            if (x >= panelX && x < panelX + panelW && y >= panelY + 57 && y < panelY + detailHeader && descriptionBar != null) descriptionScroll = Math.clamp(descriptionScroll - (int)(vertical * 26), 0, descriptionBar.maximum());
            else if (y >= panelY + detailHeader && detailBar != null) detailScroll = Math.clamp(detailScroll - (int)(vertical * 26), 0, detailBar.maximum());
            return true;
        }
        if (insideTree(x, y)) { scrollX -= horizontal * 40; scrollY -= vertical * 40; clampScroll(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (allowsMovement() && InputSidebarMovement.consumes(minecraft, event)) return true;
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && teamPicker) { teamPicker = false; rebuildWidgets(); return true; }
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && detail != null) { detail = null; rebuildWidgets(); return true; }
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && !overview) { overview = true; rebuildWidgets(); return true; }
        if (!overview && detail == null && getFocused() == null && !layout.nodes().isEmpty()) {
            int index = 0;
            for (int i = 0; i < layout.nodes().size(); i++) if (layout.nodes().get(i).tech().id().equals(focusedTech)) index = i;
            if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN && focusedTech != null) { showDetail(focusedTech); return true; }
            if (event.key() >= com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT && event.key() <= com.mojang.blaze3d.platform.InputConstants.KEY_UP) {
                if (focusedTech != null) index = Math.floorMod(index + (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_LEFT || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_UP ? -1 : 1), layout.nodes().size());
                var node = layout.nodes().get(index); focusedTech = node.tech().id();
                scrollX = node.x() - 20; scrollY = node.y() - 18; clampScroll(); return true;
            }
        }
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (allowsMovement() && InputSidebarMovement.consumes(minecraft, event)) return true;
        return super.keyReleased(event);
    }
    @Override public int unitHudLeft(){return overviewWidth();}
    @Override public boolean allowsMovement() { return overview && detail == null; }
    @Override public void removed() { InputSidebarMovement.release(minecraft); if (DialogCoordinator.suspending()) return; waitingTeamSlot = -1; waitingTeam = ""; waitingTechnology = ""; requests.accept(new ResearchPayloads.Request(ResearchPayloads.Action.CLOSE, view.session(), 0, -1, "")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private String trim(String value, int max) { return font.width(value) <= max ? value : font.plainSubstrByWidth(value, Math.max(1, max - font.width("…"))) + "…"; }
    private static int color(ResearchView.Status status) {
        return switch (status) { case LOCKED -> 0xFF77838C; case AVAILABLE -> GOLD; case ACTIVE -> 0xFF74BCD8; case COMPLETED -> 0xFF8DBD81; };
    }
}
