package dev.hoi.client.screen.industry;

import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.industry.IndustryTerrainAdjustment;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.client.ui.industry.IndustryOrganizationList;
import dev.hoi.client.ui.industry.IndustryOrganizationRow;
import dev.hoi.client.ui.UiHoverRegion;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiPanelButton;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.client.ui.UiText;
import dev.hoi.protocol.*;
import dev.hoi.protocol.industry.IndustryView.*;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import static dev.hoi.protocol.industry.IndustryPayloads.Action.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class IndustryScreen extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final int TEXT = UiMenuStyle.TEXT, MUTED = UiMenuStyle.MUTED;
    private static final int GOOD = 0xFF000000 | Tooltips.GOOD, BAD = 0xFF000000 | Tooltips.BAD, GOLD = 0xFF000000 | Tooltips.GOLD;
    private final String token;
    private final Consumer<IndustryPayloads.Request> transport;
    private final dev.hoi.client.ui.UiDiplomacyAssets flagAssets = new dev.hoi.client.ui.UiDiplomacyAssets();
    private final dev.hoi.client.screen.decision.DecisionText economyText = new dev.hoi.client.screen.decision.DecisionText();
    private final dev.hoi.client.screen.decision.DecisionPanel economyPanel = new dev.hoi.client.screen.decision.DecisionPanel();
    private Map<String, String> economyAssets;
    private boolean economyOpen, fuelWindow, fuelConsumed;
    private int fuelScale;
    private MenuTab tab;
    private String foreignTarget = "";
    private boolean foreign() { return !foreignTarget.isEmpty() || view != null && view.viewing() != null; }
    private IndustryView view;
    private final List<net.minecraft.client.gui.components.Renderable> passiveWidgets = new ArrayList<>();
    private final Map<String,Equipment> equipmentById=new HashMap<>();
    private final Map<String,Equipment> equipmentBySource=new HashMap<>();
    private final Map<String,String> navalFamilyByTexture=new HashMap<>();
    private final Map<String,IndustryTankDesigner.Slot> designSlots=new HashMap<>();
    private final Map<String,Template> templatesById=new HashMap<>();
    private final Map<String,NavalRepair> repairShipsById=new HashMap<>();
    private List<Equipment> productionItems=List.of(),navalTypes=List.of();
    private final Map<String,Long> navalCounts=new HashMap<>();
    private int militaryFactories,navalFactories;
    private UiMenuStyle.Frame frame;
    private int pane, top, scroll, secondScroll, pickerScroll, hudScroll, pending, refresh;
    private int designerScroll;
    private String divisionPicker = "";
    private int divisionPickerScroll;
    private final List<UiPanelButton> divisionOptions = new ArrayList<>();
    private UiScrollBar divisionPickerBar;
    private boolean divisionPickerDragging;
    private UiScrollBar productionScrollbar, productionPickerScrollbar;
    private int productionPickerStep;
    private boolean productionDragging, productionPickerDragging;
    private final int[] divisionStatScroll = new int[3];
    private final UiScrollBar[] divisionStatBars = new UiScrollBar[3];
    private static final Map<String, Map<String, Integer>> factoryScales = new HashMap<>();
    public static void clearPreferences() { factoryScales.clear(); }
    private Map<String, Integer> factoryScales() { return factoryScales.computeIfAbsent(view.country(), key -> new HashMap<>()); }
    private boolean allRepairPortsExpanded;
    private String unitGroup = "", logisticsEquipment = "", manufacturerLine = "";
    private boolean manufacturerName, manufacturerReverse, manufacturerSorted, designerManufacturer;
    private String manufacturerEquipment = "";
    private int manufacturerScroll, manufacturerAnchorX, manufacturerAnchorY, manufacturerX, manufacturerY, manufacturerWidth, manufacturerBottom;
    private final List<net.minecraft.client.gui.components.AbstractWidget> manufacturerWidgets = new ArrayList<>();
    private int tradeTab = 1, tradeAmount = 1, selectedSlot = -1;
    private boolean supportSlot, designer, closed, background, showOutdated;
    private final Set<String> hiddenResources = new HashSet<>(), expandedRepairPorts = new HashSet<>();
    private String navalFamily = "";
    private String navalProductionLine = "";
    private String group = "all", picker = "", switchLine = "", resource = "IRON", partner = "";
    private final Set<String> tradeRegions = new HashSet<>();
    private String tradeSort = "export", draggedImport = "";
    private boolean tradeSortReverse, autoImportWindow, autoImportMoving, listDragging, importMoved;
    private double autoImportX, autoImportY, autoImportGrabX, autoImportGrabY, importGrabY, importPointerY, importStartY;
    private int autoImportBudget = -1, tradeClipTop, tradeClipBottom = Integer.MAX_VALUE;
    private UiScrollBar listScrollbar;
    private boolean openingDesigner;
    private String location = "", locationRecruit = "", draftName = "";
    private record IndustryDeploymentLocation(int x,int y,int width,int height,String recruit,String label) {}
    private final List<IndustryDeploymentLocation> deploymentLocations = new ArrayList<>();
    private record IndustryProductionLine(Line line, int y, int height) {}
    private final List<IndustryProductionLine> productionLines = new ArrayList<>();
    private final Set<String> collapsedProduction = new HashSet<>();
    private String draggedProduction = "";
    private double productionDragX, productionDragY, productionPointerY, productionOffsetY;
    private final Map<String,List<net.minecraft.client.gui.components.AbstractWidget>> productionWidgets = new HashMap<>();
    private boolean productionMoved;
    private boolean selectingDeployment;
    public boolean selectingDeployment() {return selectingDeployment && !closed;}
    public boolean acceptsDeploymentOverlay(String session,long revision) {return selectingDeployment() && token.equals(session) && view != null && revision >= view.revision();}
    private EditBox nameBox, tankNameBox;
    private String tankName="",tankSlot="",tankCategory="";
    private int tankScroll;
    private boolean tankRolesOpen;
    private final Set<String> collapsedRecruitment = new HashSet<>();
    private GuiGraphicsExtractor graphics;
    private int mouseX, mouseY;
    public IndustryScreen(MenuTab tab) {
        this(tab, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(IndustryPayloads.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    public IndustryScreen(MenuTab tab, String token, IndustryView fixture, Consumer<IndustryPayloads.Request> transport) {
        super(Component.literal(title(tab)));
        this.tab = tab; this.token = token; this.view = fixture; this.transport = transport;
        indexView();
        if(fixture!=null&&fixture.tankDesigner()!=null)tankName=fixture.tankDesigner().name();
    }
    public IndustryScreen(String target) {
        this(MenuTab.LOGISTICS);
        foreignTarget = Objects.requireNonNull(target);
    }
    public static boolean supports(MenuTab tab) {
        return tab == MenuTab.PRODUCTION || tab == MenuTab.TRADE || tab == MenuTab.LOGISTICS || tab == MenuTab.RECRUITMENT;
    }
    private static String title(MenuTab tab) {
        return switch (tab) { case PRODUCTION -> "생산"; case TRADE -> MenuTab.TRADE.label();
            case LOGISTICS -> "군수"; case RECRUITMENT -> "모병 및 배치"; default -> "산업"; };
    }
    public void open() { send(foreign() ? FOREIGN_LOGISTICS : OPEN, foreignTarget, "", 0); }
    public void update(IndustryView next) {
        if (closed || !next.session().equals(token)) return;
        if (next.country().isEmpty() || view != null && !next.country().equals(view.country())) {
            closed = true; minecraft.gui.setScreen(null);
            return;
        }
        if (view != null && next.revision() <= view.revision()) return;
        if (foreign() && (next.viewing() == null || !foreignTarget.isEmpty() && !foreignTarget.equals(next.viewing().country()))) return;
        if (next.viewing() != null) { tab = MenuTab.LOGISTICS; foreignTarget = next.viewing().country(); }
        if (nameBox != null) draftName = nameBox.getValue();
        if (next.draft() != null && (view == null || view.draft() == null)) draftName = next.draft().name();
        if (designer && next.draft() == null) designer = false;
        if(next.tankDesigner()!=null&&!tankDesigner()) {tankName=next.tankDesigner().name();tankSlot="";tankScroll=0;}
        draggedProduction = "";
        autoImportBudget = -1; draggedImport = ""; importMoved = false;
        view = next; pending = 0; indexView();
        if (!tankDesigner() || view.tankDesigner().designTeam() == null) designerManufacturer = false;
        if (!manufacturerEquipment.isEmpty() && (equipmentById.get(manufacturerEquipment) == null || equipment(manufacturerEquipment).manufacturers().isEmpty())) manufacturerEquipment = "";
        if (selectingDeployment && (next.message().equals("배치 장소를 지정했습니다.") || next.recruits().stream().noneMatch(r -> r.id().equals(locationRecruit)))) {
            selectingDeployment = false; dev.hoi.client.map.scene.SceneRenderer.clearDeployment();
        }
        if (view.locations().stream().noneMatch(l -> l.id().equals(location)))
            location = "";
        rebuildWidgets();
    }
    int panelWidth() { return pane; }
    private boolean compact() { return height - top < 300; }
    private int hudBottom() { return HudMenuBar.height(width); }
    private void indexView() {
        equipmentById.clear();equipmentBySource.clear();navalFamilyByTexture.clear();designSlots.clear();templatesById.clear();repairShipsById.clear();
        militaryFactories=0;navalFactories=0;
        if(view==null)return;
        for(var e:view.equipment()) {
            equipmentById.putIfAbsent(e.id(),e);
            if(e.sourceEquipment()!=null)equipmentBySource.putIfAbsent(e.sourceEquipment(),e);
            if(e.naval()&&e.family()!=null)navalFamilyByTexture.putIfAbsent(e.texture(),e.family());
        }
        if(view.tankDesigner()!=null)view.tankDesigner().slots().forEach(slot->designSlots.putIfAbsent(slot.id(),slot));
        view.templates().forEach(t->templatesById.putIfAbsent(t.id(),t));
        if(view.navalRepairs()!=null)view.navalRepairs().ships().forEach(ship->repairShipsById.putIfAbsent(ship.id(),ship));
        for(var line:view.lines()) {
            if(equipment(line.equipment()).naval())navalFactories+=line.operatingFactories();
            else militaryFactories+=line.operatingFactories();
        }
    }
    private void filterProduction() {
        if(view==null)return;
        productionItems=view.equipment().stream().filter(e->e.unlocked()&&(group.equals("all")||e.group().equals(group)))
                .filter(e->showOutdated||!e.outdated()).filter(e->navalFamily.isEmpty()||Objects.equals(navalFamily,e.family()))
                .filter(e->e.resources().keySet().stream().noneMatch(hiddenResources::contains))
                .sorted(Comparator.comparingInt(e -> "convoy".equals(e.family()) ? 0 : 1)).toList();
        var models=new LinkedHashMap<String,Equipment>();navalCounts.clear();
        for(var e:view.equipment())if(e.unlocked()&&e.family()!=null) {
            if(e.naval()&&!Set.of("convoy", "floating_harbor_equipment").contains(e.family()))models.putIfAbsent(e.family(),e);
            if(showOutdated||!e.outdated())navalCounts.merge(e.family(),1L,Long::sum);
        }
        var order=List.of("carrier","heavy_cruiser","light_cruiser","destroyer","submarine","battleship","battlecruiser");
        navalTypes=models.values().stream().sorted(Comparator.comparingInt(e->{int index=order.indexOf(e.family());return index<0?order.size():index;})).toList();
    }
    private int bottom() { return height - 25; }
    private int rightWidth() { return Math.min(Math.max(176, width * 380 / 2560), width - pane - 8); }
    private boolean modal() { return tankDesigner() || designer || !picker.isEmpty() || !partner.isEmpty(); }
    private int productionPickerWidth() { return Math.min(width-16,Math.round(pane*(picker.equals("repairs")?444f:picker.equals("naval_ports")?375f:495f)/550)); }
    private boolean dockedProductionPicker() { return !tankDesigner() && tab==MenuTab.PRODUCTION && (picker.equals("equipment")||picker.equals("repairs")||picker.equals("naval_ports")) && width-pane>=productionPickerWidth()+4; }
    @Override protected void rebuildWidgets() {
        var focused = getFocused();
        super.rebuildWidgets();
        if (focused != null && children().contains(focused)) setFocused(focused);
    }
    @Override protected void init() {
        confirming = false;
        passiveWidgets.clear();
        productionScrollbar = null;
        productionPickerScrollbar = null;
        listScrollbar = null;
        manufacturerScrollbar = null; manufacturerWidgets.clear();
        pane = UiPanelLayout.width(tab, width); frame = UiMenuStyle.frame(UiMenuStyle.window(tab), width, pane); top = Math.round(frame.y());
        filterProduction();
        graphics = null; if (!designer) nameBox = null; deploymentLocations.clear(); productionLines.clear(); productionWidgets.clear();
        if (selectingDeployment) return;
        background = designer;
        HudMenuBar.buttons(width, view == null ? "" : view.country(), tab, next -> {
            if (next == tab) return;
            if (next == MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(next);
        }).stream().filter(button -> !foreign() || button.getMessage().getString().equals(MenuTab.LOGISTICS.label() + " 메뉴")).forEach(this::addRenderableWidget);
        background = false;
        if (!modal() || dockedProductionPicker()) { var close = UiMenuStyle.close(UiMenuStyle.window(tab), frame, this::onClose); close.active = pending == 0; addRenderableWidget(close); }
        if (view != null) {
            background = modal() && !dockedProductionPicker();
            layout();
            background = false;
            if (tankDesigner()) layoutEquipmentDesigner();
            else if (designer) layoutDesigner();
            else if (!partner.isEmpty()) layoutContract();
            else if (!picker.isEmpty()) layoutPicker();
        }
        if (designerManufacturer) layoutManufacturers();
        if (!manufacturerEquipment.isEmpty() || designerManufacturer) for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && !manufacturerWidgets.contains(widget)) { widget.active = false; widget.setTooltip(null); }
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (selectingDeployment && view != null) {
            HudMenuBar.drawPassive(g, width, view.hud(), view.country());
            return;
        }
        graphics = g; mouseX = mx; mouseY = my;
        UiMenuStyle.window(g, UiMenuStyle.window(tab), frame, height);
        if (tab == MenuTab.TRADE) {
            frame.art(g, "trade/ui/econ_switch/frame_" + (tradeTab == 0 ? 0 : 1), 70, 4);
            UiMenuStyle.title(g, font, frame, UiCommandLabels.text("TRADE_ECON", "경제"), 78, 21, 210, 0, UiMenuStyle.TEXT);
            UiMenuStyle.title(g, font, frame, UiCommandLabels.text("TRADE_TRADE", "무역"), 335, 21, 210, 0, UiMenuStyle.TEXT);
        } else UiMenuStyle.title(g, font, UiMenuStyle.window(tab), frame, title(tab));
        if (view == null) {
            HudMenuBar.draw(g, width, dev.hoi.client.ClientCampaignHud.hud(), mx, my, hudScroll);
            text("국가 현황을 불러오는 중…", 9, top + 37, pane - 18, MUTED);
        }
        else {
            HudMenuBar.draw(g, width, view.hud(), mx, my, hudScroll);
            background = modal() && !dockedProductionPicker();
            layout();
            if (background) for (var widget : passiveWidgets) widget.extractRenderState(g, -1, -1, delta);
            background = false;
            if (tankDesigner()) layoutEquipmentDesigner();
            else if (designer) layoutDesigner();
            else if (!partner.isEmpty()) layoutContract();
            else if (!picker.isEmpty()) layoutPicker();
        }
        var movingWidgets = productionMoved && !draggedProduction.isEmpty() ? productionWidgets.getOrDefault(draggedProduction,List.of()).stream().filter(widget->widget.visible).toList() : List.<net.minecraft.client.gui.components.AbstractWidget>of();
        for (var widget : movingWidgets) widget.visible = false;
        for (var widget : divisionOptions) widget.visible = false;
        super.extractRenderState(g, mx, my, delta);
        for (var widget : divisionOptions) widget.visible = true;
        for (var widget : movingWidgets) widget.visible = true;
        if (!draggedProduction.isEmpty() && productionMoved && !modal()) {
            var row = productionLines.stream().filter(r -> r.line().id().equals(draggedProduction)).findFirst().orElse(null);
            if (row != null) {
                int y = Math.clamp((int)Math.round(productionPointerY - productionOffsetY),productionScrollbar.top(),Math.max(productionScrollbar.top(),productionScrollbar.bottom()-row.height()));
                g.nextStratum();
                g.enableScissor(productionLeft(),y,productionLeft()+frame.len(512),y+row.height());UiMenuStyle.window(g,UiMenuStyle.window(tab),frame,height);productionListBackground(g,compact()?scroll:0);g.disableScissor();
                g.pose().pushMatrix();g.pose().translate(0,y-row.y());
                int savedX=mouseX,savedY=mouseY;mouseX=mouseY=-1;
                productionLine(row.line(),equipment(row.line().equipment()),view.lines().indexOf(row.line()),row.y());
                for (var widget : movingWidgets) widget.extractRenderState(g,-1,-1,delta);
                mouseX=savedX;mouseY=savedY;
                g.pose().popMatrix();
            }
        }
        if (designerManufacturer || !manufacturerEquipment.isEmpty() && !equipment(manufacturerEquipment).manufacturers().isEmpty()) {
            layoutManufacturers();
            for (var widget : manufacturerWidgets) widget.extractRenderState(g, mx, my, delta);
        }
        if (designer && !divisionPicker.isEmpty() && view.draft() != null) {
            int w = Math.min(600, width), h = Math.min(360, height);
            g.nextStratum();
            layoutDivisionPicker((width - w) / 2, (height - h) / 2, h, view.draft());
            for (var widget : divisionOptions) widget.extractRenderState(g, mx, my, delta);
        }
        graphics = null;
    }
    private void layout() {
        switch (tab) { case PRODUCTION -> production(); case TRADE -> { trade(); autoImportPriority(); }
            case LOGISTICS -> logistics(); case RECRUITMENT -> recruitment(); default -> {} }
    }
    @Override protected <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addRenderableWidget(T widget) {
        if (!background) return super.addRenderableWidget(widget);
        if (widget instanceof net.minecraft.client.gui.components.AbstractWidget button) {
            button.active = false;
            button.setTooltip(null);
        }
        passiveWidgets.add(widget);
        return widget;
    }
    private static final String[] FILTERS = {"inf_art", "armour", "air", "capital_ships", "screen_ships", "other_ships", "refit", "resources", "outdated", "stockpiling", "reinforcing", "upgrading"};
    private static final String[][] FILTER_TEXT = {{"PRODUCTION_EQUIPMENT_FILTER", "보병 및 포병 장비"}, {"PRODUCTION_ARMOR_FILTER", "기갑 차량"}, {"PRODUCTION_AIR_FILTER", "항공기"},
            {"PRODUCTION_CAPITAL_SHIPS_FILTER", "주력함"}, {"PRODUCTION_SCREEN_SHIPS_FILTER", "보조함"}, {"PRODUCTION_OTHER_SHIPS_FILTER", "기타 함선"}, {"PRODUCTION_REFITTING_FILTER", "개장"},
            {"PRODUCTION_LACKING_RESOURCES_FILTER", "충족된 자원"}, {"PRODUCTION_OUTDATED_FILTER", "최신 장비"}, {"PRODUCTION_STOCKPILING_FILTER", "비축"}, {"PRODUCTION_REINFORCING_FILTER", "증원"}, {"PRODUCTION_UPGRADING_FILTER", "개선"}};
    private static final int[][] FILTER_POSITIONS = {{13, 223, 51, 234}, {98, 223, 137, 234}, {183, 223, 221, 234}, {268, 223, 306, 234}, {353, 223, 391, 234}, {439, 223, 476, 234},
            {14, 252, 67, 262}, {98, 255, 152, 262}, {183, 255, 236, 262}, {268, 255, 322, 262}, {353, 255, 408, 262}, {440, 255, 492, 262}};
    private static final Set<String> hiddenFilters = new HashSet<>();
    private static final Set<String> CAPITAL_SHIPS = Set.of("battleship", "battlecruiser", "heavy_cruiser", "carrier"), SCREEN_SHIPS = Set.of("destroyer", "light_cruiser");
    int productionLeft() { return frame.ix(14); }
    float productionScale() { return frame.scale(); }
    int productionRowTop(int index) {
        double sourceY = 294;
        for (int i = 0; i < index; i++) sourceY += productionSourceHeight(view.lines().get(i));
        return Math.round(frame.y(sourceY)) - scroll;
    }
    private int windowHeight() { return (int)Math.ceil((height - frame.y()) / frame.scale()); }
    private boolean productionFilter(String filter, Line line) {
        var e = equipment(line.equipment());
        return switch (filter) {
            case "inf_art" -> !e.naval() && !e.group().equals("armor") && !e.group().equals("air");
            case "armour" -> !e.naval() && e.group().equals("armor");
            case "air" -> !e.naval() && e.group().equals("air");
            case "capital_ships" -> e.naval() && CAPITAL_SHIPS.contains(e.family());
            case "screen_ships" -> e.naval() && SCREEN_SHIPS.contains(e.family());
            case "other_ships" -> e.naval() && !CAPITAL_SHIPS.contains(e.family()) && !SCREEN_SHIPS.contains(e.family());
            case "refit" -> e.naval() && line.conversion() != null;
            case "resources" -> line.missingResources() == null || line.missingResources().isEmpty();
            case "outdated" -> !e.outdated();
            case "stockpiling" -> Objects.equals(line.needState(), 0);
            case "reinforcing" -> Objects.equals(line.needState(), 2);
            case "upgrading" -> Objects.equals(line.needState(), 1);
            default -> false;
        };
    }
    private boolean productionShown(Line line) {
        for (String filter : hiddenFilters) if (productionFilter(filter, line)) return false;
        return true;
    }
    private double productionSourceHeight(Line line) { return !productionShown(line) ? 0 : collapsedProduction.contains(line.id()) ? 63 : 111; }
    private void production() {
        var lines = new ArrayList<>(view.lines());
        if (productionMoved && !draggedProduction.isEmpty()) {
            int target=productionTarget(productionPointerY);
            var moving=lines.stream().filter(line->line.id().equals(draggedProduction)).findFirst().orElse(null);
            if(target>=0&&moving!=null){lines.remove(moving);lines.add(target,moving);}
        }
        int bodyTop = frame.iy(44), start = frame.iy(289), end = Math.min(height, frame.iy(windowHeight() - 13));
        int viewportTop = compact() ? bodyTop : start;
        int content = Math.round(frame.y(294 + lines.stream().mapToDouble(this::productionSourceHeight).sum())) - start;
        productionScrollbar = new UiScrollBar(frame.ix(527), viewportTop, end, end - viewportTop, content + (compact() ? start - bodyTop : 0));
        scroll = Math.clamp(scroll, 0, productionScrollbar.maximum());
        int offset = compact() ? scroll : 0;
        if (graphics != null) {
            graphics.enableScissor(0, bodyTop, width, end);
            productionListBackground(graphics, offset);
        }
        productionHeader(offset);
        double sourceY = 294;
        for (var line : lines) {
            double sourceHeight = productionSourceHeight(line);
            int y = Math.round(frame.y(sourceY)) - scroll, h = Math.round(frame.y(sourceY + sourceHeight)) - scroll - y;
            sourceY += sourceHeight;
            if (sourceHeight == 0) continue;
            if ((y < viewportTop || y + h > end) && !line.id().equals(draggedProduction)) continue;
            boolean moving=productionMoved&&line.id().equals(draggedProduction);
            if(graphics==null||!moving) {
                int first=children().size();
                productionLine(line, equipment(line.equipment()), view.lines().indexOf(line), y);
                if(graphics==null)productionWidgets.put(line.id(),children().subList(first,children().size()).stream().filter(child->child instanceof net.minecraft.client.gui.components.AbstractWidget).map(child->(net.minecraft.client.gui.components.AbstractWidget)child).toList());
            }
        }
        if (graphics != null) graphics.disableScissor();
        if (graphics != null && productionScrollbar.maximum() > 0) productionScrollbar.draw(graphics, scroll);
    }
    private void productionListBackground(GuiGraphicsExtractor g, int offset) {
        frame.tile(g, "diplomacy/dialog/panel_dark", 0, 253 - offset / frame.scale(), 550, windowHeight() - 253 + (int)Math.ceil(offset / frame.scale()), 64, UiMenuStyle.TILE);
    }
    private void productionHeader(int offset) {
        var economy = view.economy();
        float lift = offset;
        if (graphics != null) UiAssets.drawExact(graphics, "production/ui/win_top", frame.x(0), frame.y(44) - lift, frame.size(550), frame.size(253));
        for (int i = 0; i < Math.min(7, view.resources().size()); i++) {
            var r = view.resources().get(i); double rx = 25 + 70 * i;
            if (graphics != null) {
                graphics.pose().pushMatrix(); graphics.pose().translate(frame.x(rx), frame.y(61) - lift); graphics.pose().scale(frame.scale());
                UiAssets.tiledNineSlice(graphics, "commander/commands/mini_tooltip/frame_0", 0, 0, 70, 30, 12, 1);
                graphics.pose().popMatrix();
                UiAssets.drawExact(graphics, "production/resources/" + (r.available() < r.demand() ? "missing/" : "") + r.id().toLowerCase(Locale.ROOT), frame.x(rx + 4), frame.y(63) - lift, frame.size(27), frame.size(27));
                String value = integer(r.available() - r.demand());
                UiText.marquee(graphics, font, Component.literal(value), frame.x(rx + 35), UiText.centeredY(font, value, frame.y(76) - lift - font.lineHeight / 2f, font.lineHeight, 1), frame.size(33), 1, r.available() < r.demand() ? BAD : GOOD, true, -1);
            }
            tip(r.name() + "\n점유지 추출 " + decimal(r.extracted()) + "\n수입 " + decimal(r.imported())
                    + " · 수출 배정 " + decimal(r.exported()) + "\n생산용 " + decimal(r.available()) + " / 수요 " + decimal(r.demand()), frame.ix(rx), frame.iy(61) - offset, frame.len(70), frame.len(30));
        }
        String[][] modifiers = {{"dockyard", "production/line/dockyard_0", "22", "102", "50"}, {"factory", "production/ui/output_icon", "108", "97", "137"}, {"cap", "production/ui/efficiency_cap_icon", "199", "101", "227"},
                {"retention", "production/ui/efficiency_loss_icon", "281", "101", "309"}, {"growth", "production/line/efficiency", "366", "102", "396"}, {"damage", "production/ui/bombing_defence_icon", "446", "97", "475"}};
        for (var m : productionIndicators()) for (var spec : modifiers) if (spec[0].equals(m.id())) {
            double ix = Double.parseDouble(spec[2]), iy = Double.parseDouble(spec[3]), vx = Double.parseDouble(spec[4]);
            int[] size = UiAssets.imageSize(spec[1]);
            long rounded = Math.round(m.value() * 100);
            String value = (rounded >= 0 && !m.id().equals("cap") ? "+" : "") + rounded + "%";
            int color = m.id().equals("damage") ? rounded < 0 ? GOOD : rounded == 0 ? GOLD : BAD : rounded >= 0 ? GOOD : BAD;
            if (graphics != null) {
                UiAssets.drawExact(graphics, spec[1], frame.x(ix), frame.y(iy) - lift, frame.size(size[0]), frame.size(size[1]));
                UiText.marquee(graphics, font, Component.literal(value), frame.x(vx), UiText.centeredY(font, value, frame.y(111) - lift - font.lineHeight / 2f, font.lineHeight, 1), frame.size(50), 1, color, true, -1);
            }
            productionTip(IndustryProductionTooltips.modifier(m), frame.ix(ix), frame.iy(97) - offset, frame.len(vx + 50 - ix), frame.len(30));
        }
        String[] icons = {"military_factory_icon", "dockyard_icon", "dockyard_icon_with_wrench"};
        String[] counts = {militaryFactories + "/" + economy.military(), navalFactories + "/" + economy.dockyards(), repairDockyards()};
        double[][] summary = {{15, 134, 45, 135, 180}, {258, 136, 290, 80, 375}, {440, 136, -1, 0, 473}};
        String[] labels = {UiCommandLabels.text("PRODUCTION_OPERATIONAL_ARMS_FACTORIES_LABEL", "군수공장"), UiCommandLabels.text("PRODUCTION_OPERATIONAL_DOCKYARDS_LABEL_SHORT", "조선소"), ""};
        for (int i = 0; i < 3; i++) {
            var row = summary[i];
            int[] size = UiAssets.imageSize("production/summary/" + icons[i]);
            float center = frame.y(144) - lift - font.lineHeight / 2f;
            if (graphics != null) {
                UiAssets.drawExact(graphics, "production/summary/" + icons[i], frame.x(row[0]), frame.y(row[1]) - lift, frame.size(size[0]), frame.size(size[1]));
                if (row[2] >= 0) UiText.marquee(graphics, font, Tooltips.officerText(labels[i]), frame.x(row[2]), center, frame.size(row[3]), 1, TEXT, true, -1);
                UiText.marquee(graphics, font, Component.literal(counts[i]), frame.x(row[4]), UiText.centeredY(font, counts[i], center, font.lineHeight, 1), frame.size(i == 2 ? 70 : 75), 1, i == 0 ? militaryFactories == economy.military() ? GOOD : GOLD : i == 1 && navalFactories != economy.dockyards() ? BAD : GOOD, true, -1);
            }
            String factoryDesc = IndustryEquipmentDesigns.sourceText(UiCommandLabels.text("arms_factory_desc", "이 건물은 §R군수공장 유지비§!를 대가로 주에 신규 군사 장비 생산을 위한 산업을 제공합니다. 필요한 경우 §Y민간공장§!으로 전환될 수 있습니다."));
            productionTip(factoryDesc, frame.ix(row[0]), frame.iy(row[1]) - offset, frame.len(size[0]), frame.len(size[1]));
            if (i == 0) productionTip(factoryDesc, frame.ix(row[2]), frame.iy(137) - offset, frame.len(row[3]), frame.len(20));
            if (i == 1) productionTip(IndustryEquipmentDesigns.sourceText(UiCommandLabels.text("dockyard_desc", "이 건물은 §R조선소 유지비§!를 대가로 새 함선을 건조하기 위한 산업력을 제공합니다.")), frame.ix(row[2]), frame.iy(137) - offset, frame.len(row[3]), frame.len(20));
            String working = i == 1 ? UiCommandLabels.text("PRODUCTION_WORKING_FACTORIES", "현재 생산 라인 중 하나에서 작업 중인 공장의 수입니다.") : i == 2 ? UiCommandLabels.text("PRODUCTION_WORKING_DOCKYARDS_FOR_REPAIRS", "현재 함선 수리용으로 사용 중인 조선소의 수입니다.") : null;
            if (working != null) productionTip(IndustryEquipmentDesigns.sourceText(working), frame.ix(row[4]), frame.iy(137) - offset, frame.len(70), frame.len(20));
        }
        if (!compact() || frame.iy(166) - offset >= frame.iy(44)) productionActions(offset);
        productionFilters(offset);
    }
    private void productionFilters(int offset) {
        float lift = offset;
        boolean solo = hiddenFilters.size() == FILTERS.length - 1;
        for (int i = 0; i < FILTERS.length; i++) {
            String filter = FILTERS[i]; int[] at = FILTER_POSITIONS[i];
            boolean shown = !hiddenFilters.contains(filter), inverted = filter.equals("resources") || filter.equals("outdated");
            int used = view.lines().stream().filter(line -> productionFilter(filter, line)).mapToInt(Line::factories).sum();
            String count = Integer.toString(used);
            if (graphics != null) {
                UiAssets.drawExact(graphics, "production/filter/" + filter + "/frame_" + (shown != inverted ? 1 : 0), frame.x(at[0]), frame.y(at[1]) - lift, frame.size(45), frame.size(32));
                float center = frame.y(at[3] + 9) - lift - font.lineHeight / 2f;
                UiText.marquee(graphics, font, Component.literal(count), frame.x(at[2]), UiText.centeredY(font, count, center, font.lineHeight, 1), frame.size(40), 1, TEXT, true, i < 6 ? 1 : 0);
            }
            if (i < 6) tip(UiCommandLabels.text(i >= 3 ? "PRODUCTION_FILTER_DOCKYARDS" : "PRODUCTION_FILTER_ARMS_FACTORIES", i >= 3 ? "사용 중인 해군 조선소" : "사용 중인 군수공장"),
                    frame.ix(at[0] + 45), frame.iy(at[3]) - offset, frame.ix(at[2] + 40) - frame.ix(at[0] + 45), frame.len(20));
            if (graphics == null && (!compact() || frame.iy(at[1]) - offset >= frame.iy(44))) {
                String tooltip = "§Y" + UiCommandLabels.text(FILTER_TEXT[i][0], FILTER_TEXT[i][1]) + "§!\n"
                        + UiCommandLabels.text(shown ? "PRODUCTION_FILTER_CLICK_TO_HIDE" : "PRODUCTION_FILTER_CLICK_TO_SHOW", shown ? "이 유형을 모두 §R숨기려면§! §G클릭§!하십시오" : "이 유형을 모두 §G보려면§! §G클릭§!하십시오")
                        + (solo && shown ? UiCommandLabels.text("PRODUCTION_FILTER_SHIFTCLICK_AGAIN", "\n다시 모든 항목을 §G보려면§! §GShift+클릭§!하십시오.") : UiCommandLabels.text("PRODUCTION_FILTER_SHIFTCLICK", "\n다른 모든 항목을 §R숨기려면§! §GShift+클릭§!하십시오."))
                        + UiCommandLabels.text("PRODUCTION_FILTER_RIGHTCLICK", "\n이 카테고리 전체를 펼치거나 접으려면 §G우클릭§!하십시오.");
                int bottom = i < 6 ? Math.min(frame.iy(at[1] + 32), frame.iy(FILTER_POSITIONS[i + 6][1])) : frame.iy(at[1] + 32);
                var button = new UiPanelButton(UiCommandLabels.text(FILTER_TEXT[i][0], FILTER_TEXT[i][1]), frame.ix(at[0]), frame.iy(at[1]) - offset, frame.ix(at[0] + 45) - frame.ix(at[0]), bottom - frame.iy(at[1]), () -> toggleFilter(filter)) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {}
                    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
                        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && isMouseOver(event.x(), event.y())) {
                            playDownSound(minecraft.getSoundManager()); collapseFilter(filter); return true;
                        }
                        return super.mouseClicked(event, twice);
                    }
                    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { dev.hoi.client.audio.AudioUiSounds.play("ui.checkbox"); }
                };
                button.setTooltip(Tooltips.createExact(IndustryEquipmentDesigns.sourceText(tooltip)));
                addRenderableWidget(button);
            }
        }
    }
    private void toggleFilter(String filter) {
        if (dev.hoi.client.ui.UiPriorityControls.shifted()) {
            boolean solo = hiddenFilters.size() == FILTERS.length - 1 && !hiddenFilters.contains(filter);
            hiddenFilters.clear();
            if (!solo) for (String other : FILTERS) if (!other.equals(filter)) hiddenFilters.add(other);
        } else if (!hiddenFilters.remove(filter)) hiddenFilters.add(filter);
        scroll = 0; rebuildWidgets();
    }
    private void collapseFilter(String filter) {
        var matching = view.lines().stream().filter(line -> productionFilter(filter, line)).map(Line::id).toList();
        if (matching.isEmpty()) return;
        boolean collapse = !collapsedProduction.containsAll(matching);
        if (collapse) collapsedProduction.addAll(matching); else matching.forEach(collapsedProduction::remove);
        rebuildWidgets();
    }
    private int productionTarget(double pointerY) {
        double sourceY = 294;
        for(int i=0;i<view.lines().size();i++){
            double sourceHeight=productionSourceHeight(view.lines().get(i));
            int y=Math.round(frame.y(sourceY))-scroll,end=Math.round(frame.y(sourceY+sourceHeight))-scroll;
            sourceY+=sourceHeight;
            if(sourceHeight>0&&pointerY>=y&&pointerY<end)return i;
        }
        return -1;
    }
    private void toggleProduction(Line line) {
        boolean collapse = !collapsedProduction.contains(line.id());
        String category = equipment(line.equipment()).group();
        for (var candidate : view.lines()) {
            if (!candidate.id().equals(line.id()) && !dev.hoi.client.ui.UiPriorityControls.shifted()
                    && !(dev.hoi.client.ui.UiPriorityControls.controlled() && equipment(candidate.equipment()).group().equals(category))) continue;
            if (collapse) collapsedProduction.add(candidate.id()); else collapsedProduction.remove(candidate.id());
        }
        rebuildWidgets();
    }
    private void productionLine(Line line, Equipment e, int index, int y) {
        float scale = frame.scale();
        boolean collapsed = collapsedProduction.contains(line.id());
        int lineHeight = Math.round((collapsed ? 60 : e.naval() ? 116 : 108) * scale);
        if (graphics == null) productionLines.add(new IndustryProductionLine(line, y, lineHeight));
        java.util.function.IntUnaryOperator x = n -> productionLeft() + Math.round(n * scale);
        java.util.function.IntUnaryOperator dy = n -> y + Math.round(n * scale);
        int header = Math.max(1, Math.round(24 * scale));
        int step = e.naval() ? 1 : factoryScales().getOrDefault(line.id(), 1);
        int limit = Math.min(e.factoryLimit(), line.availableFactories());
        art("production/line/" + (collapsed ? "collapsed" : e.naval() ? (e.factoryLimit() < 15 ? "naval_" + e.factoryLimit() : "naval") : "land"), productionLeft(), y, Math.round(511 * scale), collapsed ? lineHeight : Math.round(108 * scale));
        art("mio/ui/gfx_production_item_has_mio/frame_0", productionLeft(), y, Math.round(512 * scale), Math.round(30 * scale));
        art("recruitment/location_frame", x.applyAsInt(2), dy.applyAsInt(1), Math.round(27 * scale), Math.round(27 * scale));
        if(graphics!=null) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0,-1f/minecraft.getWindow().getGuiScale());
            cellText(Integer.toString(index + 1), x.applyAsInt(3), y + 3, x.applyAsInt(30) - x.applyAsInt(3), header, TEXT, false);
            graphics.pose().popMatrix();
            var name = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.officerText(e.name()));
            UiText.marquee(graphics, font, name, productionLeft() + 40 * scale, UiText.centeredY(font, name, y + 4 * scale, 19 * scale, UiText.scale(font)), 154 * scale, UiText.scale(font), TEXT, true, -1);
        }
        if (children().stream().noneMatch(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.isMouseOver(mouseX, mouseY)))
            productionTip(IndustryEquipmentDesigns.tooltip("PRODUCTION_DRAG_N_DROP"),productionLeft(),y,Math.round(511*scale),lineHeight);
        var expand = originalButton(collapsed ? "생산 라인 펼치기" : "생산 라인 접기", "commander/commands/division_list_column_button/frame_" + (collapsed ? 1 : 7),
                x.applyAsInt(20), dy.applyAsInt(8), Math.round(19 * 1.4f * scale), Math.round(21 * 1.4f * scale), true, () -> toggleProduction(line));
        if (expand != null) {
            expand.setHeight(Math.min(expand.getHeight(), dy.applyAsInt(30) - expand.getY()));
            expand.setTooltip(Tooltips.createExact(IndustryEquipmentDesigns.tooltip(collapsed ? "PRODUCTION_EXPAND" : "PRODUCTION_COLLAPSE")));
        }
        if (graphics == null) {
            var manufacturer = new UiPanelButton("군수산업체 · " + line.id(), x.applyAsInt(195), y + 1, Math.round(29 * scale), header,
                    () -> { manufacturerLine = line.id(); picker = "manufacturer"; manufacturerScroll = 0; rebuildWidgets(); }) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    if (line.manufacturer() != null) UiAssets.drawContent(g, line.manufacturer().icon(), getX(), getY(), getWidth(), getHeight());
                    else if (line.manufacturers().isEmpty()) UiAssets.drawGrayscale(g, dev.hoi.client.ui.industry.IndustryOrganizationRow.art("attach_manufacturer"), getX(), getY(), getWidth(), getHeight());
                    else UiAssets.draw(g, dev.hoi.client.ui.industry.IndustryOrganizationRow.art("attach_manufacturer"), getX(), getY(), getWidth(), getHeight());
                }
                @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
                    if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && isMouseOver(event.x(), event.y())) {
                        if (active && line.manufacturer() != null) { playDownSound(minecraft.getSoundManager()); send(MANUFACTURER, line.id(), "", 0); }
                        return true;
                    }
                    return super.mouseClicked(event, twice);
                }
            };
            manufacturer.active = pending == 0 && (line.manufacturer() != null || !line.manufacturers().isEmpty());
            manufacturer.setTooltip(Tooltips.createExact(line.manufacturer() == null ? line.manufacturerTooltip() : line.manufacturer().tooltip()));
            addRenderableWidget(manufacturer);
        }
        if (!e.naval()) {
            if (graphics != null) {
                double cap = view.modifiers().stream().filter(m -> m.id().equals("cap")).findFirst().map(m -> m.value()).orElse(0.0);
                graphics.pose().pushMatrix(); graphics.pose().translate(productionLeft(), y); graphics.pose().scale(scale);
                IndustryProductionRender.startEnd(graphics, "production/line/efficiency_max", "production/line/efficiency_bg", 252, 10, 93, 9, cap);
                IndustryProductionRender.startEnd(graphics, "production/line/efficiency_fill", null, 252, 10, 93, 9, line.efficiency());
                graphics.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/line/efficiency_frame.png"), 248, 7, 347, 22, 0, 1, 0, 1);
                UiAssets.draw(graphics, "production/line/efficiency", 225, 4, 24, 21);
                graphics.pose().popMatrix();
            }
            productionTip(IndustryProductionTooltips.efficiency(view,line), x.applyAsInt(248), y, Math.round(99 * scale), header);
        }
        if(view.navalDeployment()!=null&&view.navalDeployment().destinations().containsKey(line.id())) {
            String destination=view.navalDeployment().destinations().get(line.id());
            String caption=destination.isEmpty()?"자동":view.navalDeployment().targets().stream().filter(p->p.id().equals(destination)).map(IndustryView.NavalTarget::name).findFirst().orElse("배치할 수 없음");
            int deployX=x.applyAsInt(230),deployWidth=Math.round(124*scale),deployHeight=Math.round(32*scale);
            if(graphics==null){
                var deploy=new UiPanelButton("생산 배치 대상 · "+line.id(),deployX,y-1,deployWidth,deployHeight,
                        ()->{navalProductionLine=line.id();picker="naval_ports";pickerScroll=0;rebuildWidgets();}) {
                    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                        UiAssets.draw(g,"production/deployment/button",getX(),getY(),getWidth(),getHeight());
                        int icon=Math.round(31*scale);
                        UiAssets.draw(g,"production/deployment/port",getX(),getY()+(getHeight()-icon)/2,icon,icon);
                        UiText.marquee(g,font,Tooltips.officerText(caption),getX()+icon,getY()+Math.round((getHeight()-font.lineHeight*UiText.scale(font))/2),getWidth()-icon-2,UiText.scale(font),TEXT,true,-1);
                    }
                };
                deploy.active=pending==0;
                deploy.setTooltip(Tooltips.createExact(destination.isEmpty()?dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("PRODUCTION_NAVAL_DEPLOYMENT_AUTO_DEPLOYMENT")
                        :dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("PRODUCTION_NAVAL_DEPLOYMENT_BASE").replace("$STATE|H$","§e"+caption+"§f")));
                addRenderableWidget(deploy);
            }
        }
        if (graphics != null) UiAssets.drawContent(graphics, "production/line/" + (e.naval() ? "dockyard_0" : "factory_0"),
                x.applyAsInt(354) + 2, y + 2, x.applyAsInt(379) - x.applyAsInt(354), header);
        originalButton("공장 줄이기", "production/line/minus", x.applyAsInt(379) + 1, y + 2, header, header, line.factories() > 0,
                () -> send(ASSIGN, line.id(), "", Math.max(0, line.factories() - factoryStep())));
        IndustryProductionRender.text(graphics,font, (collapsed || line.reservedFactories() > 0 ? (line.operatingFactories() + line.damagedFactories()) + "/" : "") + line.factories(), x.applyAsInt(403) + 1, y + 2, x.applyAsInt(453) - x.applyAsInt(403), header, TEXT);
        var add = originalButton("공장 늘리기", "production/line/plus", x.applyAsInt(453) + 1, y + 2, header, header, line.factories() < limit,
                () -> send(ASSIGN, line.id(), "", Math.min(limit, line.factories() + factoryStep())));
        if (add != null) add.setTooltip(Tooltips.create(Component.literal(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip(e.naval() ? "PRODUCTION_ADD_NAVAL_FACTORIES_CLICK" : "PRODUCTION_ADD_FACTORIES_CLICK"))));
        originalButton("생산 라인 삭제", "production/line/delete", x.applyAsInt(481), y + 2, header, header, true, () -> send(REMOVE, line.id(), "", 0));
        if (collapsed) {
            productionCollapsed(line, e, y, scale);
            return;
        }
        int controlsX = productionEquipment(line, e, x.applyAsInt(3), dy.applyAsInt(30), dy.applyAsInt(104), scale);
        int infoX = x.applyAsInt(233);
        int demandOffset=Math.round(new dev.hoi.client.ui.UiUnits(width,height).length(1));
        int priorityWidth=Math.round(22*scale),priorityBaseHeight=Math.round(28*scale);
        int priorityDownY=dy.applyAsInt(76);
        int priorityHeight=dy.applyAsInt(104)-priorityDownY;
        var priorityUp = originalButton("생산 라인 한 칸 위로", "construction/ui/up", controlsX, priorityDownY-priorityBaseHeight, priorityWidth, priorityBaseHeight, index > 0,
                () -> send(UP, line.id(), "", dev.hoi.client.ui.UiPriorityControls.shifted() ? 1 : 0), "construction/ui/up_disabled");
        if (priorityUp != null) priorityUp.setTooltip(Tooltips.create(dev.hoi.client.ui.UiPriorityControls.tooltip(true)));
        var priorityDown = originalButton("생산 라인 한 칸 아래로", "construction/ui/down", controlsX, priorityDownY, priorityWidth, priorityHeight, index < view.lines().size() - 1,
                () -> send(DOWN, line.id(), "", dev.hoi.client.ui.UiPriorityControls.shifted() ? 1 : 0), "construction/ui/down_disabled");
        if (priorityDown != null) priorityDown.setTooltip(Tooltips.create(dev.hoi.client.ui.UiPriorityControls.tooltip(false)));
        if(!e.naval())productionStock(line, e, infoX, dy.applyAsInt(78)+demandOffset, x.applyAsInt(275) - infoX, Math.round(22 * scale));
        if(!e.naval()&&line.needState()!=null) {
            int target=line.needState();
            int offset=2*demandOffset;
            art("production/line/target_"+target,x.applyAsInt(248),dy.applyAsInt(50)+offset,Math.round(27*scale),Math.round(24*scale));
            productionTip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip(IndustryProductionIndicators.targetKey(target)),x.applyAsInt(248),dy.applyAsInt(50)+offset,Math.round(27*scale),Math.round(24*scale));
        }
        var conversion = line.conversion();
        if (!e.naval()) for (int multiplier : new int[]{10, 5}) {
            var scaleButton = originalButton(line.id() + " ×" + multiplier, "production/line/x" + multiplier + (step == multiplier ? "_selected" : "_normal"),
                    x.applyAsInt(278), dy.applyAsInt(multiplier == 10 ? 30 : 68), Math.round(36 * scale), Math.round(36 * scale), true, () -> {
                        factoryScales().put(line.id(), step == multiplier ? 1 : multiplier); rebuildWidgets();
                    });
            if (scaleButton != null) scaleButton.setTooltip(Tooltips.create(Tooltips.officerText(step == multiplier ? UiCommandLabels.text("PRODUCTION_FACTORY_SCALE_DOWN", "") : UiCommandLabels.text("PRODUCTION_FACTORY_SCALE_UP", "").replace("$SCALE$", Integer.toString(multiplier)))));
        }
        for (int cell = 0; cell < 15; cell++) {
            int fx = x.applyAsInt(317 + cell % 5 * 29), fy = dy.applyAsInt(33 + cell / 5 * 23);
            factoryCell(line, e, cell, step, limit, y, fx, fy, Math.round(28 * scale), Math.round(22 * scale));
        }
        int resourceRow = 0;
        for (var resource : (conversion != null && conversion.source() != null ? conversion.resources() : e.resources()).entrySet()) {
            if (resourceRow == 3) break;
            int ry = dy.applyAsInt(31 + resourceRow++ * 24);
            boolean missing = line.missingResources().contains(resource.getKey());
            productionResource(resourceArt((missing ? "missing/" : "") + resource.getKey()), integer(resource.getValue() * line.operatingFactories()), x.applyAsInt(462), ry,
                    scale, missing ? BAD : TEXT);
        }
    }
    private void productionCollapsed(Line line, Equipment equipment, int y, float scale) {
        int imageX = productionLeft(), imageY = y + Math.round(33 * scale), imageWidth = Math.round(278 * scale), imageHeight = Math.round(26 * scale);
        art("production/line/equipment_collapsed", imageX, imageY, imageWidth, imageHeight);
        if (graphics == null) {
            var button = new UiPanelButton(equipment.name() + " · 장비 교체", imageX, imageY, imageWidth, imageHeight, () -> {
                picker="equipment";group=equipment.group();switchLine=line.id();navalFamily="";hiddenResources.clear();pickerScroll=0;rebuildWidgets();
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    int[] size = UiAssets.imageSize(equipment.texture());
                    int w = Math.min(Math.round(size[0] * .37f * scale), Math.round(154 * scale));
                    int h = Math.min(Math.round(size[1] * .37f * scale), Math.round(23 * scale));
                    UiAssets.draw(g,equipment.texture(),imageX+(Math.round(157*scale)-w)/2,imageY+(Math.round(23*scale)-h)/2,w,h);
                }
            };
            button.active = pending == 0;
            button.setTooltip(Tooltips.createExact(IndustryProductionTooltips.equipment(view,line,equipment,IndustryProductionTooltips.shortage(line))));
            addRenderableWidget(button);
        }
        if (graphics != null) {
            graphics.pose().pushMatrix();graphics.pose().translate(productionLeft(),y);graphics.pose().scale(scale);
            IndustryProductionRender.startEnd(graphics,"production/line/progress_fill","production/line/progress_bg",157,34,118,21,line.progress());
            graphics.pose().popMatrix();
            IndustryProductionRender.text(graphics,font,equipment.naval()?IndustryProductionIndicators.navalDays(line.daily(),line.progress()):productionDaily(line.daily()),productionLeft()+157*scale,y+34*scale,120*scale,21*scale,TEXT,false);
        }
        if (!equipment.naval()) {
            productionStock(line,equipment,productionLeft()+Math.round(281*scale),y+Math.round(34*scale),Math.round(39*scale),Math.round(23*scale));
            if (line.needState()!=null) art("production/line/target_"+line.needState(),productionLeft()+Math.round(320*scale),y+Math.round(32*scale),Math.round(27*scale),Math.round(24*scale));
        }
        int index=0;
        for(var resource:(line.conversion()!=null&&line.conversion().source()!=null?line.conversion().resources():equipment.resources()).entrySet()) {
            if(index==3)break;
            boolean missing=line.missingResources().contains(resource.getKey());
            productionResource(resourceArt((missing?"missing/":"")+resource.getKey()),integer(resource.getValue()*line.operatingFactories()),productionLeft()+Math.round((360+index++*46)*scale),y+Math.round(32*scale),scale,missing?BAD:TEXT);
        }
    }
    private static String productionDaily(double daily) {
        return IndustryEquipmentDesigns.tooltip("PRODUCTION_PRODUCING_DAILY").replace("$AMOUNT$", String.format(Locale.ROOT, "%.2f", daily));
    }
    private void productionStock(IndustryView.Line line, Equipment equipment, int x, int y, int w, int h) {
        if(graphics!=null)return;
        int offset=Math.round(new dev.hoi.client.ui.UiUnits(width,height).length(1));
        var button=new UiPanelButton(equipment.name()+" · 재고량 · 군수 보기",x,y,w,h,()->{
            tab=MenuTab.LOGISTICS; logisticsEquipment=equipment.id(); scroll=0;
            var rows=IndustryLogisticsRows.create(view.equipment(),view.lines(),"all");
            for(int i=0;i<rows.size();i++)if(rows.get(i).models().stream().anyMatch(e->e.id().equals(logisticsEquipment))) {scroll=frame.len(LOGISTICS_ROW*i);break;}
            rebuildWidgets();
        }) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiText.cell(g,font,IndustryLogisticsRows.stockLabel(line.needValue()==null?equipment.stockpile():line.needValue()),x-offset,y,w,h,line.needState()!=null&&line.needState()>0?GOLD:GOOD,true);
            }
        };
        button.active=pending==0;
        button.setTooltip(Tooltips.create(Component.literal(equipment.name()+" 비축량: "+equipment.stockpile()+"\n클릭하여 군수 현황 보기")));
        addRenderableWidget(button);
    }
    @Override public Component getTitle() { return Component.literal(title(tab)); }
    private void productionResource(String texture, String value, int x, int y, float scale, int color) {
        if (graphics == null) return;
        int size=Math.round(27*.7f*scale);
        UiAssets.draw(graphics,texture,x+Math.round(5*scale),y+Math.round(4*scale),size,size);
        IndustryProductionRender.text(graphics,font,value,x+Math.round(18*scale),y+Math.round(5*scale),Math.round(30*scale),Math.round(17*scale),color);
    }
    private int productionEquipment(Line line, Equipment equipment, int slotX, int y, int bottom, float scale) {
        int slotWidth=Math.round(201*scale);
        var units=new dev.hoi.client.ui.UiUnits(width,height);
        int down=Math.round(units.length(1)),left=Math.round(units.length(3));
        int h=bottom-y-down;
        int[] image=UiAssets.imageSize(equipment.texture());
        int w=image[0]>0&&image[1]>0?Math.max(1,(int)Math.round(image[0]*Math.min(slotWidth/(double)image[0],h/(double)image[1]))):slotWidth;
        int x=slotX+(slotWidth-w)/2;
        if (graphics != null) return x+w;
        boolean waiting=view.navalDeployment()!=null&&view.navalDeployment().waiting().containsKey(line.id());
        String name=equipment.name()+" · 장비 교체";
        var button=new UiPanelButton(name,x,y+down,w,h,()->{
            picker="equipment";group=equipment.group();switchLine=line.id();navalFamily="";hiddenResources.clear();pickerScroll=0;rebuildWidgets();
        }) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                int labelY=y+Math.round(53*scale)+down;
                UiAssets.draw(g,equipment.texture(),x,y+down,w,h);
                String role=IndustryProductionIndicators.landRole(equipment);
                if(!role.isEmpty())UiAssets.draw(g,role,x+w-Math.round(36*scale)-left,y+Math.round(25*scale)+down,Math.round(36*scale),Math.round(35*scale));
                if(equipment.outdated())UiAssets.draw(g,"production/outdated_overlay",x,y+down,w,h);
                g.fill(x,labelY,x+w,bottom,0x88000000);
                IndustryProductionRender.text(g,font,familyName(equipment),x+2,labelY,w-4,bottom-labelY,TEXT);
                int lineY=y-Math.round(30*scale),barX=equipment.naval()?158:157,barY=equipment.naval()?31:26;
                g.pose().pushMatrix();g.pose().translate(productionLeft(),lineY);g.pose().scale(scale);
                IndustryProductionRender.startEnd(g,"production/line/progress_fill","production/line/progress_bg",barX,barY,118,21,line.progress());
                g.pose().popMatrix();
                IndustryProductionRender.text(g,font,waiting?dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("PRODUCTION_NAVAL_DEPLOYMENT_NOT_DEPLOYABLE"):equipment.naval()?IndustryProductionIndicators.navalDays(line.daily(),line.progress()):productionDaily(line.daily()),productionLeft()+160*scale,lineY+barY*scale,115*scale,21*scale,!waiting&&line.daily()>0?TEXT:BAD,true);
            }
        };
        button.active=pending==0;button.setTooltip(Tooltips.createExact(waiting?IndustryProductionTooltips.navalWaiting(view,line):IndustryProductionTooltips.equipment(view,line,equipment,IndustryProductionTooltips.shortage(line))));addRenderableWidget(button);
        return x+w;
    }
    static int productionFactoryState(int start,int step,int normal,int damaged,int reserved,int assigned,int preview) {
        if(preview>=0&&preview<assigned&&start>=preview&&start<assigned)return 4;
        if(preview>assigned&&start+step>assigned&&start<preview)return 1;
        return damaged>0?2:normal>0?0:reserved>0?3:-1;
    }
    private void factoryCell(Line line, Equipment e, int cell, int step, int limit, int lineY, int x, int y, int w, int h) {
        if (graphics != null) return;
        int start = cell * step, target = Math.min((cell + 1) * step, limit);
        int used = Math.clamp(line.factories() - start, 0, step);
        int normal = Math.clamp(line.operatingFactories() - start, 0, step);
        int damaged = Math.clamp(line.operatingFactories() + line.damagedFactories() - start, 0, step) - normal;
        int reserved = used - normal - damaged;
        int frame = step == 10 ? 10 : step == 5 ? 5 : 0;
        String name = line.id() + " · " + (e.naval() ? "조선소" : "공장") + " " + (cell + 1) + "칸";
        var button = new UiPanelButton(name, x, y, w, h, () -> send(ASSIGN, line.id(), "", target)) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                if (start >= e.factoryLimit()) return;
                UiAssets.draw(g, "production/line/factory_bg", x, y, w, h);
                float scale=IndustryScreen.this.frame.scale();
                double column=((mx-6)/scale-317)/29,row=((my-lineY)/scale-33)/23;
                int preview=pending==0&&column>=0&&column<5&&row>=0&&row<3
                        ?Math.min(((int)row*5+(int)column+1)*step,limit):-1;
                int status=productionFactoryState(start,step,normal,damaged,reserved,line.factories(),preview);
                if (status >= 0) {
                    int dockW = Math.round(26 * scale), dockH = Math.round(18 * scale);
                    if (e.naval()) UiAssets.draw(g, "production/line/dockyard_" + (frame + status), x + (w - dockW) / 2, y + (h - dockH) / 2, dockW, dockH);
                    else UiAssets.drawContent(g, "production/line/factory_" + (frame + status), x + 1, y + 1, w - 2, h - 2);
                }
            }
        };
        button.sound("ui.scroll");
        button.active = pending == 0 && start < limit;
        button.setTooltip(null);
        addRenderableWidget(button);
    }
    String repairDockyards() {
        var repairs = view.navalRepairs();
        if (repairs == null) return "—/—";
        return (repairs.usedDockyards() == null ? "—" : repairs.usedDockyards()) + "/" + repairs.availableDockyards();
    }
    List<Modifier> productionIndicators() {
        var result = new ArrayList<Modifier>();
        for (String id : List.of("dockyard", "factory", "cap", "retention", "growth", "damage"))
            view.modifiers().stream().filter(m -> m.id().equals(id)).findFirst().ifPresent(result::add);
        return List.copyOf(result);
    }
    private void productionActions(int offset) {
        String[] ids = {"infantry", "armor", "air", "navy", "repair"};
        String[] names = {"보병 및 포병 장비 제작", "기갑 차량 제작", "항공기 제작", "함선 건조", "해군 수리 대기열"};
        int[] xs = {25, 105, 185, 350, 430};
        if (graphics != null) return;
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i]; int sourceX = xs[i], x = frame.ix(sourceX), y = frame.iy(166) - offset;
            int right = i + 1 < xs.length ? Math.min(frame.ix(sourceX + 81), frame.ix(xs[i + 1])) : frame.ix(sourceX + 81);
            var button = new UiPanelButton(names[i], x, y, right - x, frame.iy(207) - frame.iy(166), () -> {
                group = id; picker = id.equals("repair") ? "repairs" : "equipment";
                switchLine = ""; navalFamily=""; hiddenResources.clear(); pickerScroll = 0; rebuildWidgets();
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    UiAssets.drawExact(g, "production/actions/" + id, frame.x(sourceX), frame.y(166) - offset, frame.size(81), frame.size(41));
                }
            };
            button.sound("ui.checkbox");
            button.active = pending == 0;
            addRenderableWidget(button);
        }
    }
    private void efficiencyBar(int x, int y, int width, int height, double value) {
        if (graphics == null) return;
        int barWidth = Math.min(width, Math.max(2, Math.round(height * 6f / 28)));
        int filled = (int)Math.round(height * Math.clamp(value, 0, 1));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + (width - barWidth) / 2f, y + height);
        graphics.pose().rotate(-(float)Math.PI / 2);
        UiAssets.draw(graphics, "logistics/efficiency_bg", 0, 0, height, barWidth);
        if (filled > 0) graphics.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/logistics/efficiency_bar.png"),
                0, 0, filled, barWidth, 0, filled / (float)height, 0, 1);
        graphics.pose().popMatrix();
    }
    private static final String[] LOGISTICS_KEYS = {"EFFICIENCY", "EQUIPMENT_TYPE", "PRODUCED", "STATUS", "NEEDS", "BALANCE", "IN_STOCK", "RESOURCES"};
    static String logisticsHeader(int column) { return IndustryLogisticsTooltips.text("LOGISTICS_" + LOGISTICS_KEYS[column] + "_LABEL"); }
    private static final int LOGISTICS_ROW_Y = 77, LOGISTICS_ROW = 58, LOGISTICS_FUEL = 210, VARIANT_ROW = 38, FUEL_LINE = 30, EQUIPMENT_LINE = 0xFF952523;
    private static final String[] FUEL_BRANCHES = {"fuel_army", "fuel_navy", "fuel_air"}, FUEL_TITLES = {"TOPBAR_ARMY", "TOPBAR_NAVY", "TOPBAR_AIR"},
            FUEL_LEVELS = {"FUEL_LOW_PRIORITY_DESC", "FUEL_MEDIUM_PRIORITY_DESC", "FUEL_HIGH_PRIORITY_DESC"}, FUEL_DOTS = {"recruitment/priority_low", "recruitment/priority_normal", "recruitment/priority_high"};
    private static final String[][] FUEL_LINES = {{"fuel_line_grid_entry_6", "produced"}, {"fuel_line_grid_entry_1", "consumed"}, {"fuel_line_grid_entry_3", "navy"},
            {"fuel_line_grid_entry_4", "air"}, {"fuel_line_grid_entry_2", "army"}, {"fuel_line_grid_entry_5", "other"}};
    private static final int[] FUEL_COLORS = {0xFFCCCCCC, 0xFFCC0000, 0xFF0000CC, 0xFF00CCCC, 0xFF00CC00, 0xFFCCCC00};
    private static final double[] RESOURCE_CELL_X = {1, 52, 102}, RESOURCE_CELL_W = {49, 48, 48}, RESOURCE_CELL_Y = {1, 33}, RESOURCE_CELL_H = {28, 30};
    private int logisticsSort = -1, variantScroll, equipmentScale;
    private boolean logisticsSortReverse, confirming;
    private String logisticsInfo = "";
    private final Set<String> hiddenFuelLines = new HashSet<>();
    int logisticsWindowHeight() { return windowHeight(); }
    int logisticsListTop() { return frame.iy(LOGISTICS_ROW_Y); }
    int logisticsListBottom() { return Math.min(height, frame.iy(windowHeight() - LOGISTICS_FUEL)); }
    int logisticsRowTop(int row) { return Math.round(frame.y(LOGISTICS_ROW_Y + LOGISTICS_ROW * row) - scroll); }
    String logisticsInfo() { return logisticsInfo; }
    List<IndustryLogisticsRows.IndustryLogisticsRow> logisticsRows() {
        var rows = new ArrayList<>(IndustryLogisticsRows.create(view.equipment(), view.lines(), "all", view.economy().equipmentLedger()));
        Comparator<IndustryLogisticsRows.IndustryLogisticsRow> order = switch (logisticsSort) {
            case 0 -> Comparator.comparingDouble(row -> row.efficiency() == null ? -1 : row.efficiency());
            case 1 -> Comparator.comparing(IndustryLogisticsRows.IndustryLogisticsRow::name);
            case 3 -> Comparator.comparingDouble(IndustryLogisticsRows.IndustryLogisticsRow::coverage);
            case 4 -> Comparator.comparingDouble(IndustryLogisticsRows.IndustryLogisticsRow::need);
            case 5 -> Comparator.comparingDouble(IndustryLogisticsRows.IndustryLogisticsRow::balance);
            case 6 -> Comparator.comparingLong(IndustryLogisticsRows.IndustryLogisticsRow::inStock);
            case 7 -> Comparator.comparingInt(row -> row.resources().size());
            default -> null;
        };
        if (order != null) rows.sort(logisticsSort == 1 ^ logisticsSortReverse ? order : order.reversed());
        return rows;
    }
    private void logistics() {
        tradeClipTop = 0; tradeClipBottom = Integer.MAX_VALUE;
        var items = logisticsRows();
        int listTop = logisticsListTop(), listBottom = logisticsListBottom(), bottom = windowHeight();
        listScrollbar = new UiScrollBar(frame.ix(527), listTop, listBottom, listBottom - listTop, frame.len(LOGISTICS_ROW * items.size()));
        scroll = Math.clamp(scroll, 0, listScrollbar.maximum());
        String country = foreign() && view.viewing() != null ? view.viewing().country() : view.country();
        if (graphics != null) {
            flagAssets.smallFlag(graphics, HudMenuBar.flagTexture(country), frame.ix(445), frame.iy(13), frame.len(41), frame.len(26));
            frame.art(graphics, "map_detail/flag_frame", 440, 8);
            frame.art(graphics, "commander/commands/deployment_binding/frame_0", 5, 47);
        }
        if (foreign() && view.viewing() != null) productionTip(view.viewing().title(), frame.ix(445), frame.iy(13), frame.len(41), frame.len(26));
        String[] icons = {"logistics/efficiency", "", "logistics/production", "", "logistics/need", "logistics/balance", "logistics/stockpile", ""};
        double[][] header = {{18, 53, 24, 21}, {48, 55, 125, 20}, {174, 53, 32, 21}, {201, 55, 70, 20}, {272, 56, 27, 16}, {313, 52, 30, 23}, {373, 52, 24, 23}, {423, 55, 100, 20}};
        for (int i = 0; i < header.length; i++) {
            final int column = i;
            if (graphics != null && !icons[i].isEmpty()) frame.art(graphics, icons[i], header[i][0], header[i][1]);
            else if (graphics != null) tradeText(graphics, frame, Tooltips.officerText(logisticsHeader(i)), header[i][0], 64.5, header[i][2], TEXT, i == 1 ? -1 : 0);
            productionTip(IndustryLogisticsTooltips.header(i), frame.ix(header[i][0]), frame.iy(header[i][1]), frame.len(header[i][2]), frame.len(header[i][3]));
            if (column == 2) continue;
            var sort = sourceButton(IndustryLogisticsTooltips.plain("LOGISTICS_" + LOGISTICS_KEYS[i] + "_SORT_BY"), frame, header[i][0], header[i][1], header[i][2], header[i][3], () -> {
                if (logisticsSort == column) logisticsSortReverse = !logisticsSortReverse; else { logisticsSort = column; logisticsSortReverse = false; }
                rebuildWidgets();
            }, (g, b) -> {});
            if (sort != null) sort.active = pending == 0;
        }
        var at = new UiMenuStyle.Frame(frame.x(), frame.y() - scroll, frame.scale());
        if (graphics != null) graphics.enableScissor(0, listTop, width, listBottom);
        tradeClipTop = listTop; tradeClipBottom = listBottom;
        for (int i = 0; i < items.size(); i++) {
            var row = items.get(i); var e = row.representative(); double ry = LOGISTICS_ROW_Y + LOGISTICS_ROW * i;
            if (at.iy(ry + 57) <= listTop || at.iy(ry) >= listBottom) continue;
            String rowId = e.id();
            var select = sourceButton(row.name(), at, 13, ry, 510, 57, () -> { logisticsInfo = logisticsInfo.equals(rowId) ? "" : rowId; fuelWindow = false; variantScroll = 0; rebuildWidgets(); }, (g, b) -> {});
            if (select != null) select.active = pending == 0;
            if (graphics != null) {
                at.art(graphics, "logistics/ui/equipment_entry", 13, ry);
                efficiencyBar(at.ix(29), at.iy(ry + 12), at.len(6), at.len(28), row.efficiency() == null ? 0 : row.efficiency());
                int[] picture = UiAssets.imageSize(e.texture()); double scale = e.naval() ? .4 : .55;
                if (picture[0] > 0) UiAssets.drawExact(graphics, e.texture(), at.x(105 - picture[0] * scale / 2), at.y(ry + 23 - picture[1] * scale / 2), at.size(picture[0] * scale), at.size(picture[1] * scale));
                tradeText(graphics, at, Component.literal(familyName(e)), 50, ry + 45, 115, GOLD, -1);
                double coverage = row.coverage();
                graphics.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/logistics/status_bg.png"), at.ix(205), at.iy(ry + 19), at.ix(267), at.iy(ry + 33), 0, 1, 0, 1);
                if (coverage > 0) graphics.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/logistics/status_bar.png"), at.ix(205), at.iy(ry + 19), at.ix(205 + IndustryLogisticsRows.BAR * coverage), at.iy(ry + 33), 0, (float) coverage, 0, 1);
                at.art(graphics, "logistics/ui/progress_marker", 205 + row.marker(), ry + 18);
                tradeText(graphics, at, Component.literal(IndustryLogisticsRows.amount(row.daily())), 166, ry + 27, 36, GOLD, 0);
                tradeText(graphics, at, Component.literal(IndustryLogisticsRows.amount(row.need())), 269, ry + 27, 36, GOLD, 0);
                String balance = IndustryLogisticsRows.signedAmount(row.balance());
                tradeText(graphics, at, Component.literal(balance), 303, ry + 27, 50, balance.startsWith("+") ? GOOD : balance.startsWith("-") ? BAD : GOLD, 0);
                long stock = row.inStock();
                tradeText(graphics, at, Component.literal(IndustryLogisticsRows.signedStock(stock)), 360, ry + 27, 50, stock > 0 ? GOOD : stock < 0 ? BAD : GOLD, 0);
            }
            int n = 0;
            for (var resource : row.resources().keySet()) {
                if (n == 6) break;
                double cell = 89 / 3.0, cx = 430 + cell * (n % 3 + .5), cy = ry + 3 + 25 * (n / 3 + .5);
                if (graphics != null) at.art(graphics, resourceArt(resource), cx - 13.5, cy - 13.5, 27, 27);
                if (mouseY >= listTop && mouseY < listBottom) productionTip(IndustryLogisticsTooltips.resource(row, resource, resourceName(resource), view.lines(), equipmentById), at.ix(cx - cell / 2), at.iy(cy - 12.5), at.len(cell), at.len(25));
                n++;
            }
            double[] columns = {21, 43, 164, 205, 257, 303, 360, 410};
            if (mouseY >= listTop && mouseY < listBottom) for (int column = 0; column < 7; column++)
                productionTip(IndustryLogisticsTooltips.row(row, column, view.lines()), at.ix(columns[column]), at.iy(ry), at.ix(columns[column + 1]) - at.ix(columns[column]), at.len(57));
        }
        tradeClipTop = 0; tradeClipBottom = Integer.MAX_VALUE;
        if (graphics != null) { graphics.disableScissor(); if (listScrollbar.maximum() > 0) listScrollbar.draw(graphics, scroll); }
        if (items.isEmpty()) text("연구 완료 또는 비축된 장비가 없습니다.", frame.ix(20), frame.iy(LOGISTICS_ROW_Y + 10), frame.len(500), MUTED);
        double fuelTop = bottom - LOGISTICS_FUEL, boxTop = fuelTop + 33, factoryTop = bottom - 94;
        if (graphics != null) {
            frame.art(graphics, "commander/commands/deployment_binding/frame_0", 7, fuelTop);
            frame.art(graphics, "logistics/ui/fuel_icon", 23, fuelTop + 4);
            tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text("LOGISTICS_FUEL_PRIORITIZATION")), 60, fuelTop + 17.5, 300, TEXT, -1);
        }
        var policy = view.recruitmentPolicy();
        for (int box = 0; box < 4; box++) {
            double bx = 20 + 127 * box;
            if (graphics != null) {
                frame.tile(graphics, "commander/commands/mini_tooltip/frame_0", bx, boxTop + 3, 120, 68, 12, 32);
                if (box < 3) frame.art(graphics, "logistics/ui/fuel_types/frame_" + box, bx + 78, boxTop + 10);
            }
            double ratio = box < 3 ? policy == null ? 0 : policy.equipmentRatios().getOrDefault(FUEL_BRANCHES[box], 0.0) : fuelRatio();
            fuelBar(bx + 10, boxTop + 46, ratio);
            if (box < 3) {
                productionTip(IndustryLogisticsTooltips.text("LOGISTICS_FUEL_PERCENTAGE").replace("$VALUE|0$", Long.toString(Math.round(ratio * 100))), frame.ix(bx + 10), frame.iy(boxTop + 46), frame.len(100), frame.len(14));
                if (graphics != null) tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text(FUEL_TITLES[box])), bx + 10, boxTop + 18, 80, TEXT, -1);
                int selected = policy == null ? 1 : policy.priorities().getOrDefault(FUEL_BRANCHES[box], 1);
                for (int level = 0; level < 3; level++) {
                    final int value = level; final String branch = FUEL_BRANCHES[box]; final double dx = bx + 7 + 20 * level;
                    String art = level == selected ? FUEL_DOTS[level] : "recruitment/priority";
                    var dot = sourceButton(IndustryLogisticsTooltips.text(FUEL_LEVELS[level]), frame, dx, boxTop + 25, 20, 21, () -> send(ALLOCATION_PRIORITY, branch, "", value),
                            (g, b) -> frame.art(g, art, dx, boxTop + 25));
                    if (dot != null) { dot.active &= policy != null && level != selected && !foreign(); dot.setTooltip(Tooltips.createOfficer(IndustryLogisticsTooltips.text(FUEL_LEVELS[level]))); }
                }
            } else {
                var stored = sourceButton(IndustryLogisticsTooltips.text("PRODUCTION_OUTPUT"), frame, bx + 7, boxTop + 13, 105, 23, () -> { fuelWindow = !fuelWindow; logisticsInfo = ""; rebuildWidgets(); },
                        (g, b) -> { frame.art(g, "recruitment/add", bx + 7, boxTop + 13); tradeText(g, frame, Tooltips.officerText(IndustryLogisticsTooltips.text("PRODUCTION_OUTPUT")), bx + 3, boxTop + 24.5, 75, TEXT, 0); frame.art(g, "logistics/ui/fuel_types/frame_3", bx + 78, boxTop + 10); });
                if (stored != null) stored.active = true;
            }
        }
        int military = 0, naval = 0;
        for (var line : view.lines()) { var equipment = equipmentById.get(line.equipment()); if (equipment != null && equipment.naval()) naval += line.factories(); else military += line.factories(); }
        String[] labels = {"LOGISTICS_MILITARY_FACTORIES_LABEL", "LOGISTICS_NAVAL_FACTORIES_LABEL"};
        String[] factoryIcons = {"production/line/factory_0", "production/line/dockyard_0"};
        double[] usage = {view.economy().military() == 0 ? 0 : (double) military / view.economy().military(), view.economy().dockyards() == 0 ? 0 : (double) naval / view.economy().dockyards()};
        for (int box = 0; box < 2; box++) {
            double bx = 20 + 150 * box;
            if (graphics != null) {
                frame.tile(graphics, "commander/commands/mini_tooltip/frame_0", bx, factoryTop + 3, 130, 70, 12, 32);
                tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text(labels[box])), bx + 15, factoryTop + 20, 74, TEXT, -1);
                frame.art(graphics, factoryIcons[box], bx + 89, factoryTop + 17);
            }
            fuelBar(bx + 15, factoryTop + 48, usage[box]);
        }
        resourceShortages(bottom);
        if (fuelWindow) fuelHistory();
        else if (!logisticsInfo.isEmpty()) items.stream().filter(row -> row.representative().id().equals(logisticsInfo)).findFirst().ifPresentOrElse(this::equipmentHistory, () -> logisticsInfo = "");
    }
    private void resourceShortages(int bottom) {
        double bgX = 366, bgY = bottom - 90;
        if (graphics != null) frame.art(graphics, "logistics/ui/resources_bg", bgX, bgY);
        var shortages = view.resources().stream().filter(r -> r.demand() > r.available() + 1e-9).limit(6).toList();
        for (int i = 0; i < shortages.size(); i++) {
            var r = shortages.get(i); int col = i / 2, row = i % 2;
            double left = bgX + RESOURCE_CELL_X[col], top = bgY + RESOURCE_CELL_Y[row], w = RESOURCE_CELL_W[col], h = RESOURCE_CELL_H[row];
            if (graphics != null) {
                frame.art(graphics, resourceArt(r.id()), left + 2, top + (h - 27) / 2, 27, 27);
                tradeText(graphics, frame, Component.literal(integer(r.demand() - r.available())), left + 29, top + h / 2, w - 31, BAD, 1);
            }
            productionTip(IndustryLogisticsTooltips.shortage(r.name(), r.available(), r.demand(), resourceBranches(r.id())), frame.ix(left), frame.iy(top), frame.len(w), frame.len(h));
        }
    }
    private Map<String, Double> resourceBranches(String resource) {
        var result = new HashMap<String, Double>();
        for (var line : view.lines()) {
            var equipment = equipmentById.get(line.equipment()); if (equipment == null) continue;
            var resources = line.conversion() != null && line.conversion().source() != null ? line.conversion().resources() : equipment.resources();
            double amount = resources.getOrDefault(resource, 0.0) * line.factories();
            if (amount > 0) result.merge(equipment.naval() ? "NAVAL" : equipment.group().equals("air") ? "AIR" : "LAND", amount, Double::sum);
        }
        return result;
    }
    private static final double FUEL_WINDOW = 545;
    private static final String[] FUEL_SCALES = {"LINECHART_DETAILS_DAYS", "LINECHART_DETAILS_MONTHS", "LINECHART_DETAILS_YEARS"};
    List<Double> fuelSeries() { return fuelSeries(fuelConsumed ? "consumed" : "stored"); }
    List<Double> fuelSeries(String line) {
        var economy = view.economy();
        if (fuelScale == 0) return economy.fuelDays().stream().map(point -> fuelValue(point, line, 1)).toList();
        var months = economy.fuelMonths();
        if (fuelScale == 1) return months.subList(Math.max(0, months.size() - 13), months.size()).stream().map(point -> fuelValue(point, line, monthDays(point.day()))).toList();
        var years = new TreeMap<Integer, double[]>();
        for (var point : months) {
            var year = years.computeIfAbsent(java.time.LocalDate.of(2020, 1, 1).plusDays(point.day() - 1).getYear(), key -> new double[3]);
            year[0] = point.stored(); year[1] += fuelRaw(point, line); year[2] += monthDays(point.day());
        }
        return years.values().stream().map(year -> line.equals("stored") ? year[0] : year[1] / Math.max(1, year[2])).toList();
    }
    private static double fuelRaw(IndustryView.FuelPoint point, String line) {
        return switch (line) {
            case "stored" -> point.stored(); case "consumed" -> point.consumed(); case "army" -> point.army(); case "navy" -> point.navy(); case "air" -> point.air(); case "produced" -> point.produced();
            default -> Math.max(0, point.consumed() - point.army() - point.navy() - point.air());
        };
    }
    private static double fuelValue(IndustryView.FuelPoint point, String line, int days) { return line.equals("stored") ? point.stored() : fuelRaw(point, line) / days; }
    private static int monthDays(long day) { return java.time.LocalDate.of(2020, 1, 1).plusDays(day).minusDays(1).lengthOfMonth(); }
    private void lineChart(List<Double> values, double sx, double sy, double min, double max, int color) {
        if (graphics == null || values.isEmpty()) return;
        double range = max - min;
        java.util.function.DoubleUnaryOperator place = value -> range <= 0 ? 147 : 3 + 144 * (1 - (value - min) / range);
        graphics.pose().pushMatrix();
        graphics.pose().translate(frame.x(sx), frame.y(sy));
        graphics.pose().scale(frame.scale());
        for (int i = 0; i < values.size(); i++) {
            double x = values.size() == 1 ? 112 : 224.0 * i / (values.size() - 1), y = place.applyAsDouble(values.get(i));
            if (i == 0) { graphics.fill((int) Math.round(x), (int) Math.round(y), (int) Math.round(x) + 1, (int) Math.round(y) + 1, color); continue; }
            double px = 224.0 * (i - 1) / (values.size() - 1), py = place.applyAsDouble(values.get(i - 1));
            int steps = (int) Math.ceil(Math.max(Math.abs(x - px), Math.abs(y - py)));
            for (int step = 0; step <= steps; step++) {
                int dx = (int) Math.round(px + (x - px) * step / Math.max(1, steps)), dy = (int) Math.round(py + (y - py) * step / Math.max(1, steps));
                graphics.fill(dx, dy, dx + 1, dy + 1, color);
            }
        }
        graphics.pose().popMatrix();
    }
    private void chartWindow(double wx, int windowHeight, double chartHeight) {
        if (graphics == null) return;
        frame.tile(graphics, "diplomacy/dialog/panel_dark", wx, 0, 395, windowHeight, 64, 192);
        frame.tile(graphics, "production/picker/panel", wx + 14, 33, 356, (int) chartHeight, 64, 192);
        frame.art(graphics, "commander/commands/unit_list_header/frame_0", wx + 1, 7);
        tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text("PRODUCTION_OUTPUT")), wx + 89, 24.5, 200, TEXT, 0);
        frame.tile(graphics, "commander/commands/tiled_window_1b_border/frame_0", wx, 290, 392, Math.max(64, windowHeight - 290), 64, 192);
    }
    private void chartScales(double wx, double sy, int selected, java.util.function.IntConsumer choose) {
        for (int scale = 0; scale < 3; scale++) {
            final int value = scale; final double bx = wx + new double[]{77, 156, 234}[scale]; final String label = IndustryLogisticsTooltips.text(FUEL_SCALES[scale]);
            var button = sourceButton(label, frame, bx, sy, 76, 31, () -> { choose.accept(value); rebuildWidgets(); },
                    (g, b) -> { frame.art(g, "commander/commands/chart_history_level_button/frame_" + (selected == value ? 1 : 0), bx, sy); tradeText(g, frame, Tooltips.officerText(label), bx, sy + 15.5, 76, TEXT, 0); });
            if (button != null) button.active = true;
        }
    }
    private void chartClose(double wx, Runnable close) {
        var button = sourceButton("×", frame, wx + 360, 8, 31, 31, () -> { close.run(); rebuildWidgets(); }, (g, b) -> frame.art(g, "commander/commands/nv_btn_close/frame_0", wx + 360, 8));
        if (button != null) button.active = true;
    }
    private void fuelHistory() {
        double wx = FUEL_WINDOW; int windowHeight = (int) Math.round(height / frame.scale() * .7);
        String[][] lines = fuelConsumed ? FUEL_LINES : new String[][]{{"fuel_line_grid_entry_0", "stored"}};
        var series = new ArrayList<List<Double>>();
        double max = 0;
        for (var line : lines) { var values = fuelSeries(line[1]); series.add(values); if (!hiddenFuelLines.contains(line[0])) for (double value : values) max = Math.max(max, value); }
        chartWindow(wx, windowHeight, 260);
        if (graphics != null) {
            frame.art(graphics, "logistics/fuel_window/linechart_bg", wx + 80, 108);
            for (int i = 0; i < lines.length; i++) if (!hiddenFuelLines.contains(lines[i][0])) lineChart(series.get(i), wx + 85, 105, 0, max, fuelConsumed ? FUEL_COLORS[i] : FUEL_COLORS[0]);
            frame.art(graphics, "logistics/fuel_window/linechart_frame", wx + 76, 103);
            String top = IndustryLogisticsTooltips.format(fuelConsumed ? "LOGISTICS_VIEW_FUEL_CONSUMPTION_VALUE" : "LOGISTICS_VIEW_GRAPH_VALUE", Map.of("VALUE", max));
            tradeText(graphics, frame, Tooltips.officerText(top), wx + 310, 112, 90, TEXT, -1);
            tradeText(graphics, frame, Component.literal("0"), wx + 310, 253, 90, TEXT, -1);
            tradeText(graphics, frame, Component.literal("0"), wx + 79, 271, 30, TEXT, -1);
            tradeText(graphics, frame, Component.literal(Integer.toString(Math.max(0, series.getFirst().size() - 1))), wx + 278, 271, 30, TEXT, 1);
            tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text(FUEL_SCALES[fuelScale])), wx + 104, 271, 180, TEXT, 0);
        }
        String[] types = {"LOGISTICS_FUEL_STORED", "LOGISTICS_FUEL_CONSUMED"};
        for (int type = 0; type < 2; type++) {
            final boolean consumed = type == 1; final double bx = wx + (type == 0 ? 77 : 196); final String label = IndustryLogisticsTooltips.text(types[type]);
            var button = sourceButton(label, frame, bx, 39, 114, 31, () -> { fuelConsumed = consumed; rebuildWidgets(); },
                    (g, b) -> { frame.art(g, "logistics/fuel_window/type_button/frame_" + (fuelConsumed == consumed ? 1 : 0), bx, 39); tradeText(g, frame, Tooltips.officerText(label), bx, 54.5, 114, TEXT, 0); });
            if (button != null) button.active = true;
        }
        chartScales(wx, 69, fuelScale, value -> fuelScale = value);
        chartClose(wx, () -> fuelWindow = false);
        for (int i = 0; i < lines.length; i++) {
            final String key = lines[i][0]; final double ex = wx + 18, ey = 308 + FUEL_LINE * i; final int color = fuelConsumed ? FUEL_COLORS[i] : FUEL_COLORS[0];
            if (ey + FUEL_LINE > windowHeight - 15) break;
            if (graphics != null) {
                frame.art(graphics, "logistics/fuel_window/line_entry", ex, ey);
                tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text(key)), ex + 35, ey + 15, 154, TEXT, -1);
            }
            var toggle = sourceButton(IndustryLogisticsTooltips.text(key), frame, ex + 7, ey + 7, 15, 15, () -> { if (!hiddenFuelLines.remove(key)) hiddenFuelLines.add(key); rebuildWidgets(); },
                    (g, b) -> { UiAssets.drawExact(g, "logistics/fuel_window/line_color", frame.x(ex + 7), frame.y(ey + 7), frame.size(15), frame.size(15), color); if (hiddenFuelLines.contains(key)) frame.art(g, "diplomacy/accept_decline/frame_0", ex + 3, ey + 4); });
            if (toggle != null) toggle.active = true;
        }
    }
    List<Double> equipmentSeries(IndustryLogisticsRows.IndustryLogisticsRow row) {
        var ledger = row.ledger();
        if (ledger == null) return List.of();
        if (equipmentScale == 0) return ledger.days().stream().map(Long::doubleValue).toList();
        var months = ledger.months();
        if (equipmentScale == 1) return months.subList(Math.max(0, months.size() - 13), months.size()).stream().map(Long::doubleValue).toList();
        var boundary = java.time.LocalDate.of(2020, 1, 1).plusDays(ledger.day()).withDayOfMonth(1);
        var years = new TreeMap<Integer, Double>();
        for (int i = 0; i < months.size(); i++) years.put(boundary.minusMonths(months.size() - 1 - i).minusDays(1).getYear(), months.get(i).doubleValue());
        return List.copyOf(years.values());
    }
    private void equipmentHistory(IndustryLogisticsRows.IndustryLogisticsRow row) {
        double wx = FUEL_WINDOW; int windowHeight = (int) Math.round(height / frame.scale() * .7);
        var values = equipmentSeries(row);
        double max = values.stream().mapToDouble(Math::abs).max().orElse(0);
        chartWindow(wx, windowHeight, 230);
        if (graphics != null) {
            frame.art(graphics, "logistics/fuel_window/linechart_bg", wx + 80, 78);
            lineChart(values, wx + 80, 78, -Math.max(1, max), Math.max(1, max), EQUIPMENT_LINE);
            frame.art(graphics, "logistics/fuel_window/linechart_frame", wx + 76, 73);
            tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.format("LOGISTICS_VIEW_GRAPH_VALUE", Map.of("VALUE", max))), wx + 316, 86, 60, TEXT, -1);
            tradeText(graphics, frame, Component.literal("0"), wx + 316, 150, 60, TEXT, -1);
            tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.format("LOGISTICS_VIEW_GRAPH_VALUE", Map.of("VALUE", -max))), wx + 316, 218, 60, TEXT, -1);
            tradeText(graphics, frame, Component.literal("0"), wx + 79, 241, 30, TEXT, -1);
            tradeText(graphics, frame, Component.literal(Integer.toString(Math.max(0, values.size() - 1))), wx + 278, 241, 30, TEXT, 1);
            tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text(FUEL_SCALES[equipmentScale])), wx + 104, 241, 180, TEXT, 0);
            frame.art(graphics, "commander/commands/unit_list_header/frame_0", wx + 1, 261);
            tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text("LOGISTICS_EQUIPMENT_VARIANT_LABEL")), wx + 32, 278.5, 170, TEXT, -1);
            tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text("LOGISTICS_IN_STOCK_LABEL")), wx + 210, 278.5, 50, TEXT, 0);
            tradeText(graphics, frame, Tooltips.officerText(IndustryLogisticsTooltips.text("LOGISTICS_WEEKLY_PRODUCTION_LABEL")), wx + 267, 278.5, 120, TEXT, 0);
        }
        chartScales(wx, 39, equipmentScale, value -> equipmentScale = value);
        chartClose(wx, () -> logisticsInfo = "");
        var variants = row.models().stream().filter(model -> model.stockpile() > 0 || view.lines().stream().anyMatch(line -> line.equipment().equals(model.id()))).toList();
        int listTop = frame.iy(303), listBottom = Math.min(height, frame.iy(windowHeight - 15));
        var bar = new UiScrollBar(frame.ix(wx + 375), listTop, listBottom, listBottom - listTop, frame.len(5 + VARIANT_ROW * variants.size()));
        variantScroll = Math.clamp(variantScroll, 0, bar.maximum());
        var at = new UiMenuStyle.Frame(frame.x(), frame.y() - variantScroll, frame.scale());
        if (graphics != null) graphics.enableScissor(0, listTop, width, listBottom);
        tradeClipTop = listTop; tradeClipBottom = listBottom;
        for (int i = 0; i < variants.size(); i++) {
            var model = variants.get(i); double ex = wx + 13, ey = 308 + VARIANT_ROW * i;
            if (at.iy(ey + 40) <= listTop || at.iy(ey) >= listBottom) continue;
            double weekly = view.lines().stream().filter(line -> line.equipment().equals(model.id())).mapToDouble(Line::daily).sum() * 7;
            if (graphics != null) {
                at.art(graphics, "logistics/info/variant_entry", ex, ey);
                tradeText(graphics, at, Component.literal(model.name()), ex + 22, ey + 18.5, 154, GOLD, -1);
                String stock = IndustryLogisticsTooltips.format("LOGISTICS_IN_STOCK_HIGH_VALUE", Map.of("AMOUNT", model.stockpile()));
                tradeText(graphics, at, Tooltips.officerText(stock), ex + 198, ey + 18.5, 54, TEXT, 0);
                tradeText(graphics, at, Tooltips.officerText(IndustryLogisticsTooltips.format("LOGISTICS_WEEKLY_SPEED_VALUE", Map.of("AMOUNT", weekly))), ex + 262, ey + 18.5, 54, TEXT, 0);
            }
            if (mouseY >= listTop && mouseY < listBottom) productionTip(IndustryLogisticsTooltips.variant(model, view.lines()), at.ix(ex), at.iy(ey), at.len(320), at.len(40));
            var delete = sourceButton(IndustryLogisticsTooltips.plain("LOGISTICS_DELETE_EQUIPMENT"), at, ex + 322, ey + 6, 27.9, 27.9, () -> confirmDelete(model),
                    (g, b) -> at.art(g, "commander/commands/nv_btn_delete/frame_0", ex + 322, ey + 6, 27.9, 27.9));
            if (delete != null) { delete.active = pending == 0 && !foreign() && model.stockpile() > 0; delete.setTooltip(Tooltips.createOfficer(IndustryLogisticsTooltips.text("LOGISTICS_DELETE_EQUIPMENT"))); }
        }
        tradeClipTop = 0; tradeClipBottom = Integer.MAX_VALUE;
        if (graphics != null) { graphics.disableScissor(); if (bar.maximum() > 0) bar.draw(graphics, variantScroll); }
    }
    private void confirmDelete(Equipment model) {
        if (view == null || foreign()) return;
        long revision = view.revision();
        var dialog = new dev.hoi.protocol.dialog.DialogView(token, revision, dev.hoi.protocol.dialog.DialogView.Kind.COMMANDER, IndustryLogisticsTooltips.deleteTitle(), "",
                IndustryLogisticsTooltips.deleteBody(model.name(), 90), "", "", "", List.of(),
                List.of(new dev.hoi.protocol.dialog.DialogView.Choice("cancel", UiCommandLabels.text("PDXO_CANCEL", "취소"), ""), new dev.hoi.protocol.dialog.DialogView.Choice("ok", "OK", "")));
        var screen = new dev.hoi.client.screen.commander.CommanderConfirmation(dialog, (ignored, choice) -> {
            if (choice.equals("ok") && view != null && view.revision() >= revision && !foreign()) send(DELETE_EQUIPMENT, model.id(), "", 0);
            minecraft.gui.setScreen(this);
        });
        screen.backdrop(this); confirming = true; minecraft.gui.setScreen(screen);
    }
    private double fuelRatio() {
        var national = view.hud().national();
        return national == null || national.fuelRatio() == null ? 0 : Math.clamp(national.fuelRatio(), 0, 1);
    }
    private void fuelBar(double sx, double sy, double ratio) {
        if (graphics == null) return;
        frame.art(graphics, "logistics/ui/fuel_bar_bg", sx, sy);
        double fill = Math.clamp(ratio, 0, 1);
        if (fill > 0) graphics.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/logistics/ui/fuel_bar.png"), frame.ix(sx), frame.iy(sy), frame.ix(sx + 100 * fill), frame.iy(sy + 14), 0, (float) fill, 0, 1);
    }
    private static final String[] TRADE_REGIONS = {"europe", "north_america", "south_america", "australia", "africa", "asia", "middle_east", "faction", "subject"};
    private static final String[][] TRADE_SORTS = {{"country", "TRADE_SORT_COUNTRY", "trade/ui/sort_export", "77"}, {"export", "TRADE_SORT_EXPORT", "trade/ui/sort_export", "247"},
            {"request", "TRADE_SORT_REQUEST", "trade/ui/sort_export", "352"}, {"convoys", "TRADE_SORT_CONVOYS", "trade/ui/sort_delivered", "470"}};
    private static final String[] TRADE_ROWS = {"TRADEVIEW_RES_PRODUCED", "TRADEVIEW_RES_IMPORTED", "TRADEVIEW_RES_EXPORTED", "TRADEVIEW_RES_SPECIAL_PROJECT", "TRADEVIEW_RES_PRODUCTION_LINES", "TRADEVIEW_RES_SURPLUS"};
    private static final double[] TRADE_ROW_CENTERS = {176, 195, 214, 233, 252, 282.5};
    private static final int TRADE_ROW = 45, TRADE_LIST_TOP = 529, TRADE_BODY_TOP = 40, IMPORT_ENTRY = 53, IMPORT_LIST_TOP = 158, ECONOMY_Y = 40, ECONOMY_WIDTH = 624;
    UiMenuStyle.Frame tradeFrame() { return frame; }
    int tradeListBottom() { return Math.min(height, frame.iy(windowHeight() - 13)); }
    boolean tradeCompact() { return tradeListBottom() - frame.iy(TRADE_LIST_TOP) < frame.len(TRADE_ROW * 3); }
    int tradeListTop() { return tradeCompact() ? frame.iy(TRADE_BODY_TOP) : frame.iy(TRADE_LIST_TOP); }
    private UiMenuStyle.Frame tradeBody() { return tradeCompact() ? new UiMenuStyle.Frame(frame.x(), frame.y() - scroll, frame.scale()) : frame; }
    private UiMenuStyle.Frame tradeList() { return tradeCompact() ? tradeBody() : new UiMenuStyle.Frame(frame.x(), frame.y() - scroll, frame.scale()); }
    int tradeRowTop(int index) { return Math.round(tradeList().y(TRADE_LIST_TOP + TRADE_ROW * index)); }
    UiMenuStyle.Frame autoImportFrame() {
        float x = Math.max(0, Math.min(frame.x(768) + (float) autoImportX, width - frame.size(295)));
        float y = Math.max(hudBottom(), frame.y(-3) + (float) autoImportY);
        return new UiMenuStyle.Frame(x, y, frame.scale());
    }
    boolean autoImportOpen() { return autoImportWindow && tab == MenuTab.TRADE && tradeTab == 1 && view != null && view.autoImport() != null && partner.isEmpty() && !foreign(); }
    private boolean insideAutoImport(double x, double y) {
        if (!autoImportOpen()) return false;
        var at = autoImportFrame();
        return x >= at.x(0) && x < at.x(295) && y >= at.y(0) && y < at.y(640);
    }
    private double autoImportShift(UiMenuStyle.Frame at) {
        int lines = font.split(Tooltips.officerText(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITY_DESCRIPTION")), Math.max(1, Math.round(at.size(260)))).size();
        return Math.max(0, 42 + lines * font.lineHeight / at.scale() - 104);
    }
    private int importTarget() {
        var at = autoImportFrame();
        double center = (importPointerY - importGrabY - at.y(0)) / at.scale() + IMPORT_ENTRY / 2.0;
        return Math.clamp((int) Math.floor((center - IMPORT_LIST_TOP - autoImportShift(at)) / IMPORT_ENTRY), 0, view.autoImport().order().size() - 1);
    }
    private static int tradeColor(long value) { return value > 0 ? GOOD : value < 0 ? BAD : TEXT; }
    private void tradeText(GuiGraphicsExtractor g, UiMenuStyle.Frame at, Component text, double sx, double center, double sw, int color, int align) {
        var line = text.getVisualOrderText();
        UiText.marquee(g, font, line, at.x(sx), UiText.centeredY(font, line, at.y(center) - font.lineHeight / 2f, font.lineHeight, 1), at.size(sw), 1, color, true, align);
    }
    private void tradeIcon(UiMenuStyle.Frame at, String path, double sx, double sy, double scale) {
        int[] size = UiAssets.imageSize(path);
        UiAssets.drawExact(graphics, path, at.x(sx), at.y(sy), at.size(size[0] * scale), at.size(size[1] * scale));
    }
    private void sourceTip(String value, int x, int y, int w, int h, boolean window) {
        if (graphics == null || background || designerManufacturer || mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) return;
        if (!window && (mouseY < tradeClipTop || mouseY >= tradeClipBottom || insideAutoImport(mouseX, mouseY))) return;
        graphics.setTooltipForNextFrame(font, Tooltips.sourceLines(font, value, Tooltips.width(graphics.guiWidth(), 14) + 24), mouseX, mouseY);
    }
    private UiPanelButton sourceButton(String name, UiMenuStyle.Frame at, double sx, double sy, double sw, double sh, Runnable action, java.util.function.BiConsumer<GuiGraphicsExtractor, UiPanelButton> draw) {
        if (graphics != null) return null;
        int clipTop = tradeClipTop, clipBottom = tradeClipBottom;
        int x = at.ix(sx), y = Math.max(at.iy(sy), clipTop), bottom = Math.min(at.iy(sy + sh), clipBottom);
        if (bottom <= y) return null;
        var button = new UiPanelButton(name, x, y, Math.max(1, at.ix(sx + sw) - x), bottom - y, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                boolean clip = clipTop > 0 || clipBottom < Integer.MAX_VALUE;
                if (clip) g.enableScissor(0, clipTop, IndustryScreen.this.width, Math.min(clipBottom, IndustryScreen.this.height));
                draw.accept(g, this);
                if (clip) g.disableScissor();
            }
        };
        button.active = pending == 0 && !foreign();
        addRenderableWidget(button);
        return button;
    }
    private void commitAutoImportBudget() {
        var auto = view == null ? null : view.autoImport();
        if (auto != null && autoImportBudget >= 0 && autoImportBudget != auto.budget()) send(AUTO_IMPORT_BUDGET, "", "", autoImportBudget);
        else { autoImportBudget = -1; rebuildWidgets(); }
    }
    private void trade() {
        tradeClipTop = 0; tradeClipBottom = Integer.MAX_VALUE;
        var toggle = sourceButton("경제와 무역 화면 간 전환", frame, 70, 4, 482, 35, () -> { tradeTab = 1 - tradeTab; scroll = 0; autoImportWindow = false; rebuildWidgets(); }, (g, b) -> {});
        if (toggle != null) toggle.setTooltip(Tooltips.createOfficer(IndustryTradeTexts.text("open_econ_button_tt")));
        if (tradeTab == 0) { economy(); return; }
        var e = view.economy(); var auto = view.autoImport(); boolean automatic = auto != null && auto.enabled();
        boolean compact = tradeCompact();
        int bodyTop = frame.iy(TRADE_BODY_TOP), listTop = tradeListTop(), listBottom = tradeListBottom();
        var rows = IndustryTradeRows.rows(view, resource, tradeRegions, tradeSort, tradeSortReverse);
        int content = frame.len(compact ? TRADE_LIST_TOP + TRADE_ROW * rows.size() - TRADE_BODY_TOP : TRADE_ROW * rows.size());
        listScrollbar = new UiScrollBar(frame.ix(591), listTop, listBottom, listBottom - listTop, content);
        scroll = Math.clamp(scroll, 0, listScrollbar.maximum());
        final var at = tradeBody();
        tradeClipTop = compact ? bodyTop : 0; tradeClipBottom = compact ? listBottom : Integer.MAX_VALUE;
        if (graphics != null && compact) graphics.enableScissor(0, bodyTop, width, listBottom);
        int traded = view.trades().stream().filter(Trade::importing).mapToInt(Trade::factories).sum();
        if (graphics != null) {
            at.art(graphics, "agency/ui/section", 46, 42);
            tradeText(graphics, at, Component.literal(IndustryTradeTexts.text("TRADE_CIV_FACTORIES")), 71, 63, 150, TEXT, 0);
            at.art(graphics, "construction/summary/civilian_factory", 236, 50);
            tradeText(graphics, at, Component.literal(Integer.toString(e.civilian())), 270, 62, 45, GOOD, -1);
            tradeIcon(at, "construction/consumer_goods", 300, 49.75, .7);
            tradeText(graphics, at, Component.literal(Integer.toString(e.consumer())), 340, 62, 35, GOLD, -1);
            tradeIcon(at, "construction/ui/traded_factories", 370, 46.25, .7);
            tradeText(graphics, at, Component.literal(Integer.toString(traded)), 410, 62, 35, GOLD, -1);
            String available = IndustryTradeTexts.text("TRADE_CIV_FACTORIES_AVAIL"); int mark = available.indexOf("$AVAIL$");
            Component availableText = mark < 0 ? Component.literal(available) : Component.literal(available.substring(0, mark)).withColor(TEXT)
                    .append(Component.literal(Integer.toString(e.freeCivilian())).withColor(GOLD)).append(Component.literal(available.substring(mark + 7)).withColor(TEXT));
            tradeText(graphics, at, availableText, 434, 62, 120, TEXT, 0);
            at.tile(graphics, "doctrine/gfx_tiled_decisions_bg_small", 20, 80, 580, 50, 16, 48);
            tradeText(graphics, at, Tooltips.officerText(IndustryTradeTexts.text("TRADE_AUTO_IMPORT")), 60, 105, 190, TEXT, -1);
            at.art(graphics, "construction/ui/number_box", 250, 92);
            tradeText(graphics, at, Component.literal(Integer.toString(autoImportBudget >= 0 ? autoImportBudget : auto == null ? 0 : auto.budget())), 250, 105, 80, TEXT, 0);
            at.tile(graphics, "trade/ui/slider_track", 340, 99, 145, 12, 4, 12);
        }
        sourceTip(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_DESC"), at.ix(60), at.iy(96), at.ix(250) - at.ix(60), at.len(18), false);
        sourceTip(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_BUDGET_DESC"), at.ix(250), at.iy(92), at.len(80), at.len(26), false);
        var check = sourceButton(IndustryTradeTexts.text("TRADE_AUTO_IMPORT"), at, 28, 89, 32, 32, () -> send(AUTO_IMPORT, "", "", automatic ? 0 : 1),
                (g, b) -> at.art(g, "trade/ui/checkbox/frame_" + (auto == null ? 2 : automatic ? 1 : 0), 28, 89));
        if (check != null) { check.active &= auto != null; check.setTooltip(Tooltips.createOfficer(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_DESC"))); }
        int maximum = auto == null ? 0 : auto.maximum(), budget = autoImportBudget >= 0 ? autoImportBudget : auto == null ? 0 : auto.budget();
        int sliderTop = Math.max(at.iy(97), tradeClipTop), sliderBottom = Math.min(at.iy(113), tradeClipBottom), clipTop = tradeClipTop, clipBottom = tradeClipBottom;
        if (graphics == null && sliderBottom > sliderTop) {
            var slider = new net.minecraft.client.gui.components.AbstractSliderButton(at.ix(340), sliderTop, at.ix(485) - at.ix(340), sliderBottom - sliderTop, Component.literal("자동 수입 예산"), maximum <= 0 ? 0 : Math.min(1, budget / (double) maximum)) {
                @Override protected void updateMessage() {}
                @Override protected void applyValue() { autoImportBudget = (int) Math.round(value * maximum); value = maximum <= 0 ? 0 : autoImportBudget / (double) maximum; }
                @Override public void onRelease(net.minecraft.client.input.MouseButtonEvent event) { super.onRelease(event); commitAutoImportBudget(); }
                @Override public boolean keyPressed(KeyEvent event) {
                    if (active && (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_LEFT || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT)) {
                        autoImportBudget = Math.clamp(budget + (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT ? 1 : -1), 0, maximum);
                        commitAutoImportBudget(); return true;
                    }
                    return super.keyPressed(event);
                }
                @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    boolean clip = clipTop > 0 || clipBottom < Integer.MAX_VALUE;
                    if (clip) g.enableScissor(0, clipTop, IndustryScreen.this.width, Math.min(clipBottom, IndustryScreen.this.height));
                    UiAssets.drawExact(g, "trade/knob", at.x(340) + (float) value * at.size(129), at.y(97), at.size(16), at.size(16));
                    if (clip) g.disableScissor();
                }
            };
            slider.active = !background && pending == 0 && automatic && maximum > 0;
            slider.setTooltip(Tooltips.createOfficer(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_BUDGET_DESC")));
            addRenderableWidget(slider);
        }
        String priorityLabel = IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITY_OPEN");
        var priority = sourceButton(priorityLabel, at, 490, 89, 94, 31, () -> { autoImportWindow = !autoImportWindow; draggedImport = ""; rebuildWidgets(); },
                (g, b) -> { at.art(g, "diplomacy/dialog/clear_button/frame_0", 490, 89); tradeText(g, at, Component.literal(priorityLabel), 490, 105, 94, TEXT, 0); });
        if (priority != null) { priority.active &= auto != null; priority.setTooltip(Tooltips.createOfficer(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITY_OPEN_TOOLTIP"))); }
        var resources = view.resources();
        if (graphics != null) {
            at.art(graphics, "trade/ui/resource_bg", 17, 130);
            for (int k = 0; k < TRADE_ROWS.length; k++) tradeText(graphics, at, Component.literal(IndustryTradeTexts.text(TRADE_ROWS[k])), 22, TRADE_ROW_CENTERS[k], 78, TEXT, 1);
        }
        for (int i = 0; i < Math.min(7, resources.size()); i++) {
            var r = resources.get(i); double ex = 108 + 68 * i;
            long surplus = Math.round(r.available() - r.demand());
            long[] values = {Math.round(r.extracted()), Math.round(r.imported()), -Math.round(r.exported()), 0, -Math.round(r.demand())};
            boolean fuel = r.fuelCurrent() != null && r.fuelMaximum() != null;
            if (graphics != null) {
                at.art(graphics, resourceArt(r.id()), ex + 6, 135.5);
                tradeText(graphics, at, Component.literal(Long.toString(surplus)), ex + 34, 149, 37, fuel ? TEXT : tradeColor(surplus), 1);
                for (int k = 0; k < values.length; k++) tradeText(graphics, at, Component.literal(Long.toString(values[k])), ex + 5, 176 + 19 * k, 65, tradeColor(values[k]), 1);
                if (fuel) {
                    String[] labels = IndustryTradeTexts.text("OIL_NEED_TEXT").split("\n", -1);
                    long[] needs = {Math.round(r.available() - r.demand() - r.fuelCurrent()), Math.round(r.available() - r.demand() - r.fuelMaximum())};
                    double pitch = font.lineHeight / at.scale();
                    for (int k = 0; k < needs.length; k++) {
                        double center = 282.5 + (k - .5) * pitch + 1 / at.scale();
                        if (k < labels.length) tradeText(graphics, at, Component.literal(labels[k]), ex + 5, center, 20, TEXT, -1);
                        tradeText(graphics, at, Component.literal(Long.toString(needs[k])), ex + 22, center, 49, needs[k] < 0 ? BAD : TEXT, 1);
                    }
                } else tradeText(graphics, at, Component.literal(Long.toString(surplus)), ex + 5, 282.5, 65, surplus < 0 ? BAD : TEXT, 1);
                if (mouseX >= at.x(ex + 10) && mouseX < at.x(ex + 78)) {
                    sourceTip(IndustryTradeTexts.balance(r), at.ix(ex + 10), at.iy(131), at.len(68), at.len(33), false);
                    String[] tips = {IndustryTradeTexts.produced(r), IndustryTradeTexts.imported(view, r), IndustryTradeTexts.exported(r), null, IndustryTradeTexts.used(r)};
                    for (int k = 0; k < tips.length; k++) if (tips[k] != null) sourceTip(tips[k], at.ix(ex + 10), at.iy(167 + 19 * k), at.len(68), at.len(19), false);
                    sourceTip(IndustryTradeTexts.surplus(r), at.ix(ex + 10), at.iy(264), at.len(68), at.len(36), false);
                }
            }
            int imports = view.trades().stream().filter(t -> t.importing() && t.resource().equals(r.id())).mapToInt(Trade::factories).sum();
            boolean cancellable = imports > 0 && !automatic;
            var cancel = sourceButton(r.name() + " 수입 취소", at, ex + 30, 303, 31, 31, () -> send(CANCEL_IMPORTS, r.id(), "", 0),
                    (g, b) -> { if (cancellable) at.art(g, "trade/ui/cancel_import", ex + 30, 303); else UiAssets.drawGrayscale(g, "trade/ui/cancel_import", at.ix(ex + 30), at.iy(303), at.len(31), at.len(31)); });
            if (cancel != null) {
                cancel.active &= cancellable;
                cancel.setTooltip(Tooltips.createOfficer(automatic ? IndustryTradeTexts.text("TRADE_AUTO_IMPORT_BLOCKS_CANCEL")
                        : IndustryTradeTexts.fill(imports > 0 ? "CANCEL_RESOURCE_IMPORT" : "NO_IMPORT_TO_CANCEL", Map.of("RESOURCE", r.name()))));
            }
        }
        if (graphics != null) at.art(graphics, "trade/ui/filter_bg", 13, 373);
        for (int i = 0; i < Math.min(7, resources.size()); i++) {
            var r = resources.get(i); double tx = 11 + 85 * i; boolean selected = r.id().equals(resource);
            var filter = sourceButton(r.name(), at, tx, 334, i < 6 ? 85 : 90, 51, () -> { resource = r.id(); scroll = 0; rebuildWidgets(); },
                    (g, b) -> { at.art(g, "trade/ui/resource_btn/frame_" + (selected ? 1 : 0), tx, 334); at.art(g, resourceArt(r.id()), tx + 32, 343); });
            if (filter != null) { filter.sound("ui.checkbox"); filter.setTooltip(Tooltips.createOfficer(IndustryTradeTexts.fill("TRADE_FILTER_BY", Map.of("KEY", r.name())))); }
        }
        for (int k = 0; k < TRADE_REGIONS.length; k++) {
            String id = TRADE_REGIONS[k], name = IndustryTradeTexts.text(id); double fx = 107 + 150 * (k % 3), fy = 385 + 35 * (k / 3); boolean on = tradeRegions.contains(id);
            var region = sourceButton(name, at, fx, fy, 123, 34, () -> { if (!tradeRegions.remove(id)) tradeRegions.add(id); scroll = 0; rebuildWidgets(); },
                    (g, b) -> { at.art(g, "trade/ui/region_filter/frame_" + (on ? 0 : 1), fx, fy); tradeText(g, at, Component.literal(name), fx + 2, fy + 16.5, 117, TEXT, 0); });
            if (region != null) region.sound("ui.scroll");
        }
        for (var sort : TRADE_SORTS) {
            double sx = Double.parseDouble(sort[3]); int[] size = UiAssets.imageSize(sort[2]); String label = IndustryTradeTexts.text(sort[1]);
            sourceButton(label, at, sx, 501, size[0], Math.min(size[1], TRADE_LIST_TOP - 501), () -> {
                if (tradeSort.equals(sort[0])) tradeSortReverse = !tradeSortReverse; else { tradeSort = sort[0]; tradeSortReverse = false; }
                scroll = 0; rebuildWidgets();
            }, (g, b) -> { at.art(g, sort[2], sx, 501); tradeText(g, at, Component.literal(label), sx, 517, size[0], TEXT, 0); });
        }
        var list = tradeList();
        if (graphics != null && !compact) graphics.enableScissor(0, listTop, width, listBottom);
        tradeClipTop = listTop; tradeClipBottom = listBottom;
        String blocked = automatic ? "\n\n" + IndustryEquipmentDesigns.sourceText(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_BLOCKS_PROPOSE")) : "";
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i); var p = row.partner(); double base = TRADE_LIST_TOP + TRADE_ROW * i;
            float y = list.y(base);
            int visibleTop = Math.max(Math.round(y), listTop), visibleBottom = Math.min(Math.round(list.y(base + TRADE_ROW)), listBottom);
            if (visibleBottom <= visibleTop) continue;
            if (graphics != null) {
                UiAssets.drawExact(graphics, row.factories() > 0 ? "trade/row_active" : !p.route() ? "trade/row_blocked" : "trade/row", list.x(15), y, list.size(576), list.size(45));
                UiAssets.cover(graphics, HudMenuBar.flagTexture(p.id()), list.ix(22), list.iy(base + 8), list.len(41), list.len(26));
                list.art(graphics, "trade/flag_overlay", 22, base + 8, 41, 26);
                list.art(graphics, "trade/flag_small_frame", 19, base + 6, 47, 32);
                tradeText(graphics, list, Component.literal(p.name()), 68, base + 21.5, 190, TEXT, -1);
                tradeText(graphics, list, Component.literal(integer(row.remaining()) + "/" + integer(row.total())), 265, base + 22, 77, GOLD, 0);
                String shipped = row.factories() == 0 ? "§e0" : "§" + (row.delivered() + 1e-9 < row.requested() ? "c" : "e") + integer(row.delivered()) + "§r/§e" + integer(row.requested());
                tradeText(graphics, list, Tooltips.styled(Component.literal(shipped), false), 354, base + 22, 100, TEXT, 0);
                String convoy = row.detail() != null && row.factories() > 0 && row.detail().convoyNeeded() > 0 ? Long.toString(row.detail().convoyUsed()) : IndustryTradeAmounts.convoyCell(p.convoys());
                tradeText(graphics, list, Component.literal(convoy), 467, base + 22, 100, row.factories() > 0 ? GOOD : TEXT, 0);
                if (row.detail() != null && mouseY >= visibleTop && mouseY < visibleBottom) {
                    var detail = row.detail(); String law = p.detail().law(); int h = visibleBottom - visibleTop;
                    productionTip(detail.status() + "\n\n" + p.detail().influence() + (law.isBlank() ? "" : "\n\n" + law) + blocked, list.ix(15), visibleTop, list.ix(249) - list.ix(15), h);
                    productionTip(detail.exports() + blocked, list.ix(249), visibleTop, list.ix(342) - list.ix(249), h);
                    productionTip(detail.transported() + blocked, list.ix(342), visibleTop, list.ix(458) - list.ix(342), h);
                    productionTip(detail.convoys() + blocked, list.ix(458), visibleTop, list.ix(591) - list.ix(458), h);
                }
            } else {
                var button = new UiPanelButton(p.name() + "에서 수입", list.ix(15), visibleTop, list.ix(591) - list.ix(15), visibleBottom - visibleTop, () -> { partner = p.id(); tradeAmount = row.factories(); autoImportWindow = false; rebuildWidgets(); }) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {}
                };
                button.active = !background && pending == 0 && !automatic;
                addRenderableWidget(button);
            }
        }
        if (graphics != null) { graphics.disableScissor(); listScrollbar.draw(graphics, scroll); }
    }
    private void autoImportPriority() {
        tradeClipTop = 0; tradeClipBottom = Integer.MAX_VALUE;
        if (!autoImportOpen()) return;
        var auto = view.autoImport(); var at = autoImportFrame(); double shift = autoImportShift(at);
        if (graphics != null) {
            graphics.nextStratum();
            at.tile(graphics, "commander/commands/tiled_plain_bg/frame_0", 0, 0, 295, 640, 64, UiMenuStyle.TILE);
            tradeText(graphics, at, Component.literal(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITY_HEADER")), 0, 22, 295, TEXT, 0);
            int y = at.iy(42);
            for (var line : font.split(Tooltips.officerText(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITY_DESCRIPTION")), Math.max(1, Math.round(at.size(260))))) { UiText.text(graphics, font, line, at.ix(10), y, TEXT); y += font.lineHeight; }
            tradeText(graphics, at, Tooltips.officerText(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITIZE_FUEL")), 44, 124 + shift, 240, TEXT, -1);
        }
        sourceTip(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITIZE_FUEL_DESC"), at.ix(44), at.iy(115 + shift), at.len(240), at.len(18), true);
        sourceButton("×", at, 257, 8, 32, 33, () -> { autoImportWindow = false; draggedImport = ""; rebuildWidgets(); }, (g, b) -> at.art(g, "menu/close", 257, 8, 32, 33));
        boolean fuel = auto.fuel();
        var check = sourceButton(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITIZE_FUEL"), at, 10, 108 + shift, 32, 32, () -> send(AUTO_IMPORT_FUEL, "", "", fuel ? 0 : 1),
                (g, b) -> at.art(g, "trade/ui/checkbox/frame_" + (fuel ? 1 : 0), 10, 108 + shift));
        if (check != null) check.setTooltip(Tooltips.createOfficer(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITIZE_FUEL_DESC")));
        var order = auto.order(); int source = order.indexOf(draggedImport), target = importMoved && source >= 0 ? importTarget() : source;
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < order.size(); i++) {
            boolean dragged = importMoved && i == source;
            if (dragged != (pass == 1)) continue;
            int display = i;
            if (importMoved && source >= 0 && !dragged) {
                if (source < target && i > source && i <= target) display = i - 1;
                else if (source > target && i >= target && i < source) display = i + 1;
            }
            double sy = dragged ? (importPointerY - importGrabY - at.y(0)) / at.scale() : IMPORT_LIST_TOP + shift + IMPORT_ENTRY * display;
            importEntry(at, auto, order.get(i), dragged ? target : display, sy);
        }
        String resetLabel = IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITY_RESET");
        var reset = sourceButton(resetLabel, at, 86, 596, 123, 34, () -> send(AUTO_IMPORT_RESET, "", "", 0),
                (g, b) -> { at.art(g, "production/design/division/reset", 86, 596); tradeText(g, at, Component.literal(resetLabel), 86, 613, 123, TEXT, 0); });
        if (reset != null) reset.setTooltip(Tooltips.createOfficer(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_PRIORITY_RESET_TOOLTIP")));
    }
    private void importEntry(UiMenuStyle.Frame at, IndustryView.AutoImport auto, String id, int index, double sy) {
        String name = view.resources().stream().filter(r -> r.id().equals(id)).map(Resource::name).findFirst().orElse(id);
        int shortage = auto.shortage().getOrDefault(id, 0);
        if (graphics != null) {
            at.tile(graphics, "doctrine/gfx_tiled_decisions_bg_small", 12, sy, 260, 53, 16, 48);
            at.art(graphics, "construction/ui/number_circle", 22, sy + 15.45, 22.1, 22.1);
            tradeText(graphics, at, Component.literal(Integer.toString(index + 1)), 22, sy + 26.5, 22.1, TEXT, 0);
            at.art(graphics, resourceArt(id), 52, sy + 13);
            tradeText(graphics, at, Component.literal(name), 84, sy + 26.5, 82, TEXT, -1);
            tradeText(graphics, at, Component.literal(Integer.toString(shortage)), 205, sy + 26.5, 35, TEXT, 0);
        }
        sourceTip(UiCommandLabels.text("PRODUCTION_DRAG_N_DROP", "§G드래그 앤 드롭§!으로 §Y우선순위§!를 변경할 수 있습니다."), at.ix(12), at.iy(sy), at.len(260), at.len(53), true);
        sourceTip(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_ACCEPTED_SHORTAGE_DESC"), at.ix(205), at.iy(sy + 16.5), at.len(35), at.len(20), true);
        var minus = sourceButton(name + " 허용 부족량 감소", at, 180, sy + 14, 25, 25, () -> send(AUTO_IMPORT_SHORTAGE, id, "", Math.max(0, shortage - factoryStep())),
                (g, b) -> at.art(g, "production/line/minus", 180, sy + 14));
        if (minus != null) { minus.active &= shortage > 0; minus.setTooltip(Tooltips.createOfficer(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_ACCEPTED_SHORTAGE_SUB_DESC"))); }
        var plus = sourceButton(name + " 허용 부족량 증가", at, 240, sy + 13.5, 26, 26, () -> send(AUTO_IMPORT_SHORTAGE, id, "", Math.min(10000, shortage + factoryStep())),
                (g, b) -> at.art(g, "production/line/plus", 240, sy + 13.5));
        if (plus != null) plus.setTooltip(Tooltips.createOfficer(IndustryTradeTexts.text("TRADE_AUTO_IMPORT_ACCEPTED_SHORTAGE_ADD_DESC")));
    }
    private void economySubscription() {
        boolean wanted = tab == MenuTab.TRADE && tradeTab == 0 && view != null && !foreign() && !closed;
        if (wanted == economyOpen || pending > 0 || closed || foreign()) return;
        economyOpen = wanted;
        send(ECONOMY, "", "", wanted ? 1 : 0);
    }
    private Map<String, String> economyAssets() {
        if (economyAssets == null) {
            var root = economyText.load(minecraft.getResourceManager());
            economyAssets = Map.of();
            if (root != null) { economyPanel.assets(root); economyAssets = dev.hoi.client.screen.decision.DecisionPanel.sprites(root); }
        }
        return economyAssets;
    }
    int economyTop() { return frame.iy(ECONOMY_Y); }
    int economyBottom() { return Math.min(height, frame.iy(windowHeight())); }
    private void economy() {
        var panel = view.economyPanel();
        if (panel == null) return;
        int clipTop = economyTop(), clipBottom = economyBottom(), x = frame.ix(0);
        scroll = Math.max(0, scroll);
        if (graphics != null) graphics.enableScissor(x, clipTop, frame.ix(ECONOMY_WIDTH), clipBottom);
        var result = economyPanel.render(graphics, font, panel, economyAssets(), economyText, x, clipTop - scroll, frame.len(ECONOMY_WIDTH));
        if (graphics != null) graphics.disableScissor();
        int content = result.hits().stream().mapToInt(hit -> hit.y() + hit.height() + scroll).max().orElse(clipTop) + frame.len(10);
        int limit = Math.max(0, content - clipBottom);
        if (scroll > limit && graphics == null) { scroll = limit; economy(); return; }
        dev.hoi.client.screen.decision.DecisionPanel.Hit hovered = null;
        var hits = result.hits();
        for (int index = hits.size() - 1; index >= 0; index--) {
            var hit = hits.get(index);
            int x0 = hit.x(), y0 = Math.max(hit.y(), clipTop), x1 = hit.x() + hit.width(), y1 = Math.min(hit.y() + hit.height(), clipBottom);
            if (!hit.button().isEmpty()) for (var later : hits.subList(index + 1, hits.size())) {
                int lx0 = later.x(), ly0 = later.y(), lx1 = lx0 + later.width(), ly1 = ly0 + later.height();
                if (later.button().isEmpty() || lx0 >= x1 || x0 >= lx1 || ly0 >= y1 || y0 >= ly1) continue;
                int[] cost = {ly0 <= y0 ? ly1 - y0 : Integer.MAX_VALUE, ly1 >= y1 ? y1 - ly0 : Integer.MAX_VALUE,
                        lx0 <= x0 ? lx1 - x0 : Integer.MAX_VALUE, lx1 >= x1 ? x1 - lx0 : Integer.MAX_VALUE};
                int side = 0; for (int i = 1; i < 4; i++) if (cost[i] < cost[side]) side = i;
                if (cost[side] == Integer.MAX_VALUE) continue;
                switch (side) { case 0 -> y0 = ly1; case 1 -> y1 = ly0; case 2 -> x0 = lx1; default -> x1 = lx0; }
            }
            if (y1 <= y0 || x1 <= x0) continue;
            if (graphics == null && !hit.button().isEmpty()) {
                var button = new UiPanelButton(hit.button(), x0, y0, x1 - x0, y1 - y0, () -> send(ECONOMY_CLICK, hit.button(), "", 0)) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {}
                };
                button.active = hit.enabled() && pending == 0 && !foreign();
                addRenderableWidget(button);
            }
            if (hovered == null && !(hit.immediate() + hit.delayed()).isEmpty() && mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1) hovered = hit;
        }
        if (graphics != null && !background && hovered != null)
            economyText.tooltip(graphics, font, hovered.immediate() + hovered.delayed(), mouseX, mouseY, width, height, 0);
    }
    private void tradeValue(Component value,int x,int y,int width,int height,int color) {
        if(graphics==null)return;
        float scale=UiText.scale(font);
        UiText.marquee(graphics,font,value,x,y+(height-font.lineHeight*scale)/2,width,scale,color,true,0);
    }
    private double minimumTraining() { return view.recruitmentPolicy() == null || view.recruitmentPolicy().minimumTraining() == null ? .2 : view.recruitmentPolicy().minimumTraining(); }
    private void recruitment() {
        int secondX = pane + 3, secondW = rightWidth(), start = top + 28;
        if (graphics != null) UiAssets.tiledNineSlice(graphics, UiMenuStyle.TEMPLATES.tile(), secondX, top, secondW, height - top, 64, .5);
        heading("사단 편제", secondX + (secondW - (int)(font.width(Tooltips.officerText("사단 편제", true)) * (font.lineHeight + 2f) / font.lineHeight)) / 2, top + 8, secondW - 16, TEXT);
        var groups = new LinkedHashMap<String, List<Recruit>>();
        for (var r : view.recruits()) groups.computeIfAbsent(r.template() + "|" + r.location(), key -> new ArrayList<>()).add(r);
        double scale = (pane - 14) / 493.0;
        int rowHeight = Math.max(23, (int)Math.round(47 * scale)), pitch = rowHeight + 2, prioritiesHeight = 6 * pitch + 4;
        double gs = (pane - 14) / 518.0;
        int headerHeight = (int)Math.round(84 * gs), lineHeight = (int)Math.round(40 * gs);
        int groupHeight = headerHeight + lineHeight + 3, linePitch = lineHeight + 1;
        int groupX = 7 + (int)Math.round(14 * gs);
        int content = prioritiesHeight + groups.entrySet().stream().mapToInt(e -> groupHeight + (collapsedRecruitment.contains(e.getKey()) ? 0 : e.getValue().size() * linePitch)).sum();
        scroll = Math.clamp(scroll, 0, Math.max(0, content - (bottom() - start)));
        String[] names = {"증원", "업그레이드", "보급 트럭", "작전", "주둔군", "습격"};
        String[] icons = {"reinforcement", "upgrade", "supply", "operation", "garrison", "raid"};
        String[] entries = {"reinforcements", "upgrades", "reinforcements", "operations", "garrisons", "upgrades"};
        int backgroundHeight = (int)Math.round(57 * scale);
        double backgroundScale = Math.min(scale, backgroundHeight / 57.0);
        int backgroundLeft = 7 + (pane - 14 - (int)Math.round(493 * backgroundScale)) / 2;
        for (int i = 0; i < names.length; i++) {
            int y = start + i * pitch - scroll;
            if (!visible(y, rowHeight, start)) continue;
            art("recruitment/" + entries[i], 7, y - (int)(5 * scale), pane - 14, backgroundHeight);
            int iconCenterX = backgroundLeft + (int)Math.round(48 * backgroundScale);
            int iconCenterY = y - (int)(5 * scale) + (int)Math.round(29.5 * backgroundScale) - 2;
            int iconBgWidth = (int)(77 * scale), iconBgHeight = (int)(40 * scale), iconWidth = (int)(54 * scale);
            int iconBgX = iconCenterX - iconBgWidth / 2, iconBgY = iconCenterY - iconBgHeight / 2;
            if (graphics != null) UiAssets.nineSlice(graphics, "recruitment/icon_bg", iconBgX, iconBgY, iconBgWidth, iconBgHeight, 16, scale);
            art("recruitment/" + icons[i] + "_icon", iconCenterX - iconWidth / 2, iconBgY, iconWidth, iconBgHeight);
            int labelX = 7 + (int)(110 * scale), labelWidth = (int)(159 * scale);
            art("recruitment/title", labelX, y + 1 + (int)(12 * scale), labelWidth, (int)(26 * scale));
            centeredCompactText(names[i], labelX + 2, y + (int)(25 * scale) - 5, labelWidth - 4, TEXT);
            art("recruitment/meter", 7 + (int)(280 * scale), y + (int)(10 * scale), (int)(108 * scale), (int)(33 * scale));
            art("recruitment/equipment", 7 + (int)(285 * scale), y + (int)(15 * scale), (int)(23 * scale), (int)(23 * scale));
            var policy = view.recruitmentPolicy();
            String category = icons[i];
            for (int dot = 0; dot < 3; dot++) {
                int priority = dot;
                boolean selected = policy != null && Objects.equals(policy.priorities().get(category), dot);
                textureButton(names[i] + " · " + priorityName(dot) + " 우선순위", "", priorityTexture(dot, selected), 7 + (int)((415 + dot * 20) * scale), y + (int)(15 * scale), (int)(20 * scale), (int)(21 * scale), policy != null,
                        () -> send(ALLOCATION_PRIORITY, category, "", priority));
            }
            Double ratio = policy == null ? null : i == 0 ? policy.reinforcementRatio() : policy.equipmentRatios().get(category);
            int meterX = 6 + (int)(310 * scale), meterY = y + (int)(19 * scale), meterW = (int)(72 * scale), meterH = Math.max(2, (int)(12 * scale));
            bar(meterX, meterY, meterW, meterH, ratio == null ? (category.equals("upgrade") ? 1 : 0) : ratio, 0xFF7EAF69);
            if (ratio == null && !category.equals("upgrade")) cellText("—", meterX, meterY, meterW, meterH, MUTED, false);
            if (i == 0 && ratio != null) {
                String needs = policy.reinforcementNeeds().entrySet().stream().map(e -> equipmentLabel(e.getKey()) + " · 부족 " + e.getValue()).collect(java.util.stream.Collectors.joining("\n"));
                tip(UiCommandLabels.text("REINFORCEMENT_DESC", "") + "\n\n" + (needs.isEmpty() ? UiCommandLabels.text("REINFORCEMENT_EMPTY", "") : needs), 7, y, (int)(400 * scale), rowHeight);
            } else if (policy != null && policy.equipmentTooltips().containsKey(category)) {
                tip(policy.equipmentTooltips().get(category), 7, y, (int)(400 * scale), rowHeight);
            } else if (Set.of("upgrade", "garrison").contains(category)) tip(UiCommandLabels.text(category.equals("upgrade") ? "UPGRADES_DESC" : "GARRISONS_DESC", ""), 7, y, (int)(400 * scale), rowHeight);
        }
        int cursor = start + prioritiesHeight - scroll;
        for (var entry : groups.entrySet()) {
            var rows = entry.getValue(); var first = rows.getFirst(); var t = template(first.template());
            int y = cursor; cursor += groupHeight;
            if (visible(y, groupHeight, start)) {
                art("recruitment/conveyor", groupX, y, (int)Math.round(490 * gs), headerHeight);
                if (!t.line().isEmpty()) art(unitTexture(t.line().getFirst(), false), groupX + 2 + (int)(10 * gs), y + (int)(17 * gs), (int)(70 * gs), (int)(50 * gs));
                tip(t.name(),groupX+(int)(95*gs),y+(int)(17*gs),(int)(214*gs),(int)(29*gs));
                centeredCompactText(t.name(), groupX + (int)(95 * gs), y + (int)(29 * gs) - 5, (int)(214 * gs), TEXT);
                int amountSize = Math.max(8, (int)Math.round(26 * gs)), amountY = y + 2 + (int)Math.round(10 * gs);
                int minusX = groupX + 2 + (int)Math.round(312 * gs), plusX = groupX + 2 + (int)Math.round(383 * gs);
                textureButton(t.name() + " 연속 훈련 횟수 증가", "", "recruitment/increase", plusX, amountY, amountSize, amountSize, first.seriesLimit() < 999, () -> send(SERIES, first.id(), "", first.seriesLimit() + 1));
                textureButton(t.name() + " 연속 훈련 횟수 감소 · 0은 무한대", "", "recruitment/decrease", minusX, amountY, amountSize, amountSize, first.seriesLimit() > 0, () -> send(SERIES, first.id(), "", first.seriesLimit() - 1));
                String amount = first.seriesLimit() == 0 ? "∞" : first.seriesLimit().toString();
                cellText(amount, minusX + amountSize, amountY, plusX - minusX - amountSize, amountSize, first.seriesLimit() == 0 ? GOLD : TEXT, false);
                for (int dot = 0; dot < 3; dot++) {
                    int priority = dot;
                    textureButton(t.name() + " · " + priorityName(dot) + " 우선순위", "", priorityTexture(dot, first.priority() == dot), groupX + (int)((420 + 20 * dot) * gs), y + (int)(17 * gs), (int)(20 * gs), (int)(21 * gs), true, () -> send(GROUP_PRIORITY, first.id(), "", priority));
                }
                art("recruitment/location", groupX + (int)(91 * gs), y + (int)(46 * gs), (int)(223 * gs), (int)(31 * gs));
                art("recruitment/location_frame", groupX + (int)(94 * gs), y + (int)(48 * gs), (int)(27 * gs), (int)(27 * gs));
                centeredCompactText(locationName(first.location()), groupX + (int)(119 * gs), y + (int)(61 * gs) - 3, (int)(188 * gs), first.location().isEmpty() ? BAD : TEXT);
                if (graphics == null) deploymentLocations.add(new IndustryDeploymentLocation(groupX + (int)(91 * gs), y + (int)(46 * gs), (int)(223 * gs), (int)(31 * gs), first.id(), locationName(first.location())));
                tip(UiCommandLabels.text("CONVEYOR_SET_LOCATION_DESC", ""), groupX + (int)(91 * gs), y + (int)(46 * gs), (int)(223 * gs), (int)(31 * gs));
                textureButton(t.name() + " 부대 추가", "부대 추가", "recruitment/add", groupX + (int)(315 * gs), y + (int)(50 * gs), (int)(105 * gs), (int)(23 * gs), true, () -> send(RECRUIT, t.id(), first.location(), 0));
                textureButton(t.name() + " 모든 라인 즉시 배치", "", "recruitment/deploy", groupX + (int)(421 * gs), y + (int)(47 * gs), (int)(27 * gs), (int)(27 * gs), !first.location().isEmpty() && rows.stream().anyMatch(r -> r.progress() >= minimumTraining()), () -> send(DEPLOY_GROUP, first.id(), "", 0));
                textureButton(t.name() + " 모든 라인 생산 취소", "", "recruitment/cancel", groupX + (int)(450 * gs), y + (int)(47 * gs), (int)(27 * gs), (int)(27 * gs), true, () -> send(CANCEL_GROUP, first.id(), "", 0));
                int summaryY = y + headerHeight;
                art("recruitment/summary", 7, summaryY, pane - 14, lineHeight);
                art("recruitment/equipment_state", 8 + (int)(212 * gs), summaryY + 1 + (int)(3 * gs), (int)(27 * gs), (int)(27 * gs));
                bar(7 + (int)(238 * gs), summaryY + (int)(10 * gs), (int)(65 * gs), Math.max(3, (int)(11 * gs)), rows.stream().mapToDouble(r -> fillRatio(t.equipment(), r.equipment())).average().orElse(0), 0xFF7EAF69);
                cellText(rows.size() + "개 사단", 7 + (int)(315 * gs), summaryY, (int)(150 * gs), lineHeight, TEXT, false);
                textureButton(collapsedRecruitment.contains(entry.getKey()) ? "클릭하여 펼치기" : "클릭하여 접기", "", collapsedRecruitment.contains(entry.getKey()) ? "recruitment/expand" : "recruitment/collapse", 7 + (int)(481 * gs), summaryY + (int)(4 * gs), (int)(26 * gs), (int)(26 * gs), true,
                        () -> { if (!collapsedRecruitment.add(entry.getKey())) collapsedRecruitment.remove(entry.getKey()); rebuildWidgets(); });
            }
            if (collapsedRecruitment.contains(entry.getKey())) continue;
            int number = 0;
            for (var r : rows) {
                y = cursor; cursor += linePitch; number++;
                if (!visible(y, lineHeight, start)) continue;
                int lineX = 7 + (int)Math.round(2 * gs);
                art("recruitment/line", lineX, y, (int)Math.round(514 * gs), lineHeight);
                compactText(t.name()+" "+number, lineX + (int)(9 * gs), y + (int)(20 * gs) - 3, (int)(190 * gs), TEXT);
                double manpowerRatio = t.manpower() == 0 ? 1 : (double) r.manpower() / t.manpower();
                double equipped = fillRatio(t.equipment(), r.equipment());
                art("recruitment/equipment_state", lineX + 1 + (int)(211 * gs), y + 1 + (int)(7 * gs), (int)(27 * gs), (int)(27 * gs));
                art("recruitment/training", lineX + (int)(307 * gs), y + (int)(7 * gs), (int)(27 * gs), (int)(27 * gs));
                bar(lineX + (int)(237 * gs), y + (int)(14 * gs), (int)(64 * gs), Math.max(3, (int)(11 * gs)), equipped, 0xFF7EAF69);
                bar(lineX + (int)(333 * gs), y + (int)(14 * gs), (int)(64 * gs), Math.max(3, (int)(11 * gs)), r.progress(), 0xFF7EAF69);
                tip(t.name()+" "+number+"\n훈련 " + percent(r.progress()) + " · 인력 " + percent(manpowerRatio) + " · 장비 " + percent(equipped) + "\n" + equipmentList(t.equipment(), r.equipment()), 8, y, (int)(397 * gs), lineHeight);
                cellText(r.seriesLabel(), lineX + (int)(398 * gs) + 2, y + (int)(7 * gs) + 1, (int)(57 * gs), (int)(26 * gs), r.seriesLimit() == 0 ? GOLD : TEXT, false);
                tip(UiCommandLabels.text("CONVEYOR_SERIES_CURRENT_AMOUNT_DESC", ""), lineX + (int)(398 * gs), y + (int)(7 * gs), (int)(57 * gs), (int)(26 * gs));
                String deployTip = r.progress() >= minimumTraining() ? UiCommandLabels.text("CONVEYOR_LINE_DEPLOY_DESC", "") : UiCommandLabels.text("DEPLOYMENT_NOT_ALLOWED_REASON_TRAINING", "")
                        .replace("$VALUE|1H%$", "§H" + String.format(Locale.ROOT, "%.1f%%", minimumTraining() * 100) + "§!").replace("$CURRENT|1R%$", "§R" + String.format(Locale.ROOT, "%.1f%%", r.progress() * 100) + "§!");
                textureButton("즉시 배치", "", "recruitment/deploy_line", lineX + (int)(458 * gs), y + (int)(7 * gs), (int)(26 * gs), (int)(26 * gs), !r.location().isEmpty() && r.progress() >= minimumTraining(), () -> send(DEPLOY, r.id(), "", 0), deployTip);
                textureButton("훈련 취소", "", "recruitment/cancel_line", lineX + (int)(484 * gs), y + (int)(7 * gs), (int)(26 * gs), (int)(26 * gs), true, () -> send(CANCEL_RECRUIT, r.id(), "", 0), UiCommandLabels.text("CONVEYOR_LINE_CANCEL_DESC", ""));
            }
        }
        var special = view.specialForces();
        art("recruitment/special", secondX + 11, top + 30, 20, 20);
        text(special == null ? "—/—" : special.used() + "/" + special.capacity(), secondX + 35, top + 35, secondW - 128, TEXT);
        if (special != null) tip("특수부대 대대 · 사용량 / 한도\n" + special.used() + " / " + special.capacity(), secondX + 7, top + 28, secondW - 100, 23);
        button("새 사단 편제 설계", "사단 설계", secondX + secondW - 90, top + 29, 83, 22, true,
                () -> { selectedSlot=-1; designerScroll=0; picker=""; openingDesigner=true; send(NEW_TEMPLATE,"","",0); });
        var retired = view.recruitmentPolicy() == null ? Set.<String>of() : view.recruitmentPolicy().retiredTemplates();
        var templates = view.templates().stream().filter(t -> !retired.contains(t.id())).toList();
        int ts = top + 55, th = Math.max(36, (secondW - 10) * 78 / 344); secondScroll = clampScroll(secondScroll, templates.size(), th + 3, ts);
        for (int i = 0; i < templates.size(); i++) {
            var t = templates.get(i); int y = ts + i * (th + 3) - secondScroll;
            if (!visible(y, th, ts)) continue;
            art("recruitment/queue", secondX + 5, y, secondW - 10, th);
            double cardScale = (secondW - 10) / 346.0;
            int iconWidth = (int)(72 * cardScale), nameX = secondX + 5 + (int)(97 * cardScale), nameWidth = (int)(234 * cardScale);
            if (!t.line().isEmpty()) art(unitTexture(t.line().getFirst(), false), secondX + 7 + (int)(9 * cardScale), y + (int)(12 * cardScale), iconWidth, (int)(52 * cardScale));
            art("production/design/division/deployment_priority_" + t.priority(), secondX + 5 + (int)(60 * cardScale), y + (int)(16 * cardScale), Math.max(1, (int)(16 * cardScale)), Math.max(1, (int)(52 * cardScale)));
            centeredCompactText(t.name(), nameX, y + (int)(32 * cardScale) - 5, nameWidth, TEXT);
            int buttonWidth = (int)(71 * cardScale), buttonY = y + (int)(49 * cardScale), buttonHeight = Math.max(10, (int)(26 * cardScale));
            textureButton(t.name() + " 훈련", "훈련", "recruitment/button", secondX + 5 + (int)(158 * cardScale), buttonY, buttonWidth, buttonHeight, !retired.contains(t.id()), () -> send(RECRUIT, t.id(), "", 0));
            textureButton(t.name() + " 편제", "편제", "recruitment/button", secondX + 5 + (int)(229 * cardScale), buttonY, buttonWidth, buttonHeight, true, () -> loadDesigner(t.id()));
            textureButton(t.name() + " 편제를 목록에서 삭제", "", "recruitment/delete_template", secondX + 5 + (int)(300 * cardScale), y + (int)(48 * cardScale), (int)(26 * cardScale), (int)(26 * cardScale), true, () -> send(RETIRE_TEMPLATE, t.id(), "", 1));
            tip(t.name() + " · " + manpower(t.manpower()) + "명 · " + integer(t.days()) + "일\n" + equipmentList(t.equipment(), Map.of()), nameX, y, nameWidth, th - 14);
        }
    }
    private static String priorityName(int priority) {return switch(priority) {case 0 -> "낮음"; case 1 -> "보통"; default -> "높음";};}
    private static String priorityTexture(int priority, boolean selected) {return "recruitment/priority" + (selected ? switch(priority) {case 0 -> "_low"; case 1 -> "_normal"; default -> "_high";} : "");}
    private void textureButton(String name, String label, String background, int x, int y, int w, int h, boolean enabled, Runnable action, String tooltip) {
        var control = textureButton(name, label, background, x, y, w, h, enabled, action);
        if (control != null) control.setTooltip(Tooltips.create(Tooltips.officerText(tooltip)));
    }
    private dev.hoi.client.ui.UiMenuButton textureButton(String name, String label, String background, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return null;
        var control = new dev.hoi.client.ui.UiMenuButton(name, x, y, w, h, action).caption(label).background(background);
        if (name.startsWith("클릭하여")) control.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal("클릭").withStyle(net.minecraft.ChatFormatting.GREEN).append(Component.literal(name.substring(2)).withStyle(net.minecraft.ChatFormatting.WHITE))));
        if (background.equals("recruitment/delete_template")) control.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal("편제를 목록에서 ").withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal("삭제").withStyle(net.minecraft.ChatFormatting.DARK_RED)).append(Component.literal("합니다.").withStyle(net.minecraft.ChatFormatting.WHITE))));
        if (name.contains("우선순위") || name.contains("연속 훈련 횟수")) control.setTooltip(null);
        control.textScale(dev.hoi.client.ui.UiText.scale(font)); control.active = enabled && pending == 0; addRenderableWidget(control);
        return control;
    }
    private void centeredCompactText(String value, int x, int y, int width, int color) {
        int inset = Math.max(0, (width - (int)Math.ceil(font.width(value) * UiText.scale(font))) / 2);
        compactText(value, x + inset, y, width - inset, color);
    }
    private void compactText(String value, int x, int y, int width, int color) {
        if (graphics != null) UiText.marquee(graphics, font, value, x, y, width, UiText.scale(font), color, true, -1);
    }
    private void loadDesigner(String id) {
        selectedSlot = -1; designerScroll = 0; picker = ""; divisionPicker = ""; openingDesigner = true;
        send(LOAD_TEMPLATE, id, "", 0);
    }
    private void layoutDesigner() {
        var t = view.draft(); if (t == null) return;
        var design = t.designer();
        if (graphics != null) graphics.nextStratum();
        int w = Math.min(600, width), h = Math.min(360, height), x = (width - w) / 2, y = (height - h) / 2, left = 326;
        boolean compact = w < 600;
        if (compact) {
            if (graphics != null) UiAssets.nineSlice(graphics, "officer/hq/list_background", x, y, w, h, 32, 0.5);
        } else art("officer/hq/designer", x, y, w, h);
        if (graphics != null) {
            var title = Component.literal(IndustryEquipmentDesigns.tooltip("DIVISION_DIVISION")).withStyle(style -> style.withBold(true));
            graphics.pose().pushMatrix(); graphics.pose().translate(x + w / 2f, y + 4); graphics.pose().scale(1.5f);
            graphics.text(font, title, -font.width(title) / 2, 0, TEXT, false); graphics.pose().popMatrix();
        }
        if (selectedSlot < 0) {
            art("production/design/division/icon_bg", x + 14, y + 25, 48, 23);
            if (!t.line().isEmpty()) art(unitTexture(t.line().getFirst(), false), x + 20, y + 26, 38, 21);
            art("production/design/division/name_icon", x + 65, y + 29, 10, 14);
            art("production/design/division/role_bg_" + (design != null && design.armor() ? 0 : 1), x + 26, y + 52, 26, 26);
            art("production/design/division/role_0", x + 30, y + 54, 18, 19);
            if (design != null) {
                if (!design.armor()) art("production/design/division/disabled", x + 30, y + 64, 7, 7);
                if (graphics == null) {
                    var niche = new UiHoverRegion("사단 장비 태그", x + 26, y + 52, 26, 26);
                    niche.setTooltip(Tooltips.createOfficer(design.nicheTooltip())); addRenderableWidget(niche);
                }
            }
            art("production/design/division/nametag", x + 67, y + 58, 12, 12);
            if (graphics != null) {
                UiAssets.nineSlice(graphics, "commander/commands/generic_background/frame_0", x + 82, y + 29, 110, 15, 4, 0.5);
                UiAssets.nineSlice(graphics, "commander/commands/generic_background/frame_0", x + 82, y + 57, 110, 15, 4, 0.5);
            }
            if (graphics != null) {
                graphics.pose().pushMatrix(); graphics.pose().translate(x + 52.5f, y + 28); graphics.pose().scale(0.5f);
                UiAssets.draw(graphics, "production/design/division/priority_icon_" + t.priority(), 0, 0, 16, 35);
                graphics.pose().popMatrix();
            }
            art("production/design/division/priority_bg", x + 254, y + 53, 64, 22);
            for (int priority = 0; priority < 3; priority++) {
                int value = priority;
                if (graphics == null) {
                    var control = new UiPanelButton(IndustryEquipmentDesigns.tooltip("TEMPLATE_PRIO_" + priority), x + 259 + priority * 18, y + (priority == 2 ? 54 : 55), 18, 19, () -> send(TEMPLATE_PRIORITY, "", "", value)) {
                        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                            String texture = "production/design/division/priority_" + (value + (t.priority() == value ? 3 : 0));
                            UiAssets.drawAlignedContent(g, texture, "production/design/division/priority_" + value, getX() - 0.5f, getY(), 18, 18.5f);
                        }
                    };
                    control.active = design != null && pending == 0;
                    control.setTooltip(Tooltips.createExact("§e" + IndustryEquipmentDesigns.tooltip("TEMPLATE_PRIO_" + priority) + "§f\n" + IndustryEquipmentDesigns.tooltip("TEMPLATE_PRIO_" + priority + "_DESC")));
                    addRenderableWidget(control);
                }
            }
            if (design != null) {
                art("production/design/division/paradrop_" + (design.paradrop() ? 1 : 0), x + 213, y + 30, 13, 12);
                art("production/design/division/special_" + (design.specialForces() > 0 ? 1 : 0), x + 226, y + 30, 13, 12);
                productionTip(design.paradropTooltip(), x + 213, y + 30, 13, 12);
                productionTip(design.specialForcesTooltip(), x + 226, y + 30, 13, 12);
            }
            String groupName = design == null ? "—" : design.nameGroups().stream().filter(group -> group.id().equals(t.nameGroup())).map(NameGroup::name).findFirst().orElse(IndustryEquipmentDesigns.tooltip("MODULE_NONE_FIXED"));
            leftText(groupName, x + 85, y + 60, 92, TEXT);
            productionTip(groupName, x + 85, y + 58, 92, 13);
            originalButton("편제 목록", "production/design/division/expand", x + 180, y + 30, 11, 11, selectedSlot < 0, () -> { divisionPicker = divisionPicker.equals("templates") ? "" : "templates"; divisionPickerScroll = 0; rebuildWidgets(); });
            var names = originalButton("사단 이름 그룹 목록", "production/design/division/expand", x + 180, y + 59.5f, 11, 11, selectedSlot < 0 && design != null, () -> { divisionPicker = divisionPicker.equals("names") ? "" : "names"; divisionPickerScroll = 0; rebuildWidgets(); }, null);
            if (names != null) names.setTooltip(Tooltips.createExact(IndustryEquipmentDesigns.tooltip("DIVISION_NAMES_GROUP_DESC")));
        }
        var closeDesigner = button("편제 창 닫기", "", x + w - 22, y + 3, 16, 17, true, () -> { designer = false; selectedSlot = -1; rebuildWidgets(); });
        if (closeDesigner != null) closeDesigner.background("menu/close").sound("ui.close");
        if (selectedSlot < 0 && graphics == null) {
            if (nameBox == null) {
                nameBox = new EditBox(font, x + 85, y + 31, 92, 13, Component.literal("편제 이름"));
                nameBox.setBordered(false);
                nameBox.setMaxLength(80); nameBox.setValue(draftName); nameBox.setResponder(value -> draftName = value);
            }
            nameBox.setPosition(x + 85, y + 31); nameBox.setWidth(92);
            if (!nameBox.getValue().equals(draftName)) nameBox.setValue(draftName);
            addRenderableWidget(nameBox);
        }
        if (selectedSlot < 0) {
            art("production/designer/composition", x + 15, y + 80, 12, 11);
            if (graphics != null) UiText.leftSingleLine(graphics, font, Tooltips.officerText(IndustryEquipmentDesigns.tooltip("DESIGNER_DIVISIONAL_COMPOSITION"), true), x + 30, y + 79, left - 40, 12, TEXT);
            String[] summary = {"HARDNESS", "ORGANIZATION", "HP", "SPEED"};
            for (int i = 0; i < summary.length; i++) {
                String id = summary[i]; int bx = x + new int[]{43, 108, 163, 218}[i];
                var stat = t.stats().stream().filter(v -> id.equals(v.id())).findFirst().orElse(null);
                String value = stat == null ? "—" : statValue(stat);
                int chipWidth = new int[]{49, 42, 42, 57}[i];
                if (graphics != null) UiAssets.nineSlice(graphics, "commander/commands/generic_background/frame_0", bx, y + 96, chipWidth, 14, 4, 0.5);
                iconText(IndustryEquipmentDesigns.divisionStatField(id, "texture"), value, bx + 2, y + 97, chipWidth - 4, 12, 10, GOLD);
                productionTip(stat == null ? id : stat.name() + ": " + value + "\n\n" + IndustryEquipmentDesigns.divisionStatField(id, "description"), bx, y + 96, chipWidth, 14);
            }
            var combatWidth = t.stats().stream().filter(stat -> "COMBAT_WIDTH".equals(stat.id())).findFirst().orElse(null);
            cellText(combatWidth == null ? "—" : integer(combatWidth.value()), x + 145, y + 116, 20, 20, TEXT, false);
            productionTip(IndustryEquipmentDesigns.tooltip("DESIGNER_COMBATWIDTH_TOOLTIP").replace("$VALUE|0$", combatWidth == null ? "—" : integer(combatWidth.value())), x + 145, y + 116, 20, 20);
        }
        if (selectedSlot < 0) {
            int requiredHeight = compact ? 480 + (t.adjustments().size() + 4) / 5 * 36 + 6 : t.columns() == null ? 258 : 297;
            designerScroll = Math.clamp(designerScroll, 0, Math.max(0, requiredHeight - (h - 53)));
            int cell = 51, sy = y + 145 - designerScroll;
            for (int i = 0; i < 25; i++) {
                int slot = i, bx = x + 19 + i / 5 * cell, by = sy + i % 5 * 23;
                if (by < y + 113 || by + 21 > y + h - 53) continue;
                String unit = t.lineUnit(i);
                if (design != null && unit.isEmpty()) {
                    int column = i / 5, row = i % 5, size = t.columnSize(column);
                    boolean columnLocked = column > 0 && t.columnSize(column - 1) == 0;
                    if (row > (columnLocked ? 0 : size + 1)) continue;
                    if (row >= design.brigadeRows()) {
                        divisionSlot(design.rowLocked(), "production/designer/blocked", bx, by, 44, false, () -> {}); continue;
                    }
                    if (columnLocked || row > size) {
                        divisionSlot(design.lineLocked(), "production/designer/locked", bx, by, 44, false, () -> {}); continue;
                    }
                }
                if (t.columns() == null ? i > t.line().size() : i % 5 > t.columnSize(i / 5)) {
                    divisionSlot("이 열의 앞선 빈 대대 칸부터 추가", "production/designer/locked", bx, by, 44, false, () -> {}); continue;
                }
                if (unit.isEmpty()) divisionSlot("대대 추가", "production/designer/add", bx, by, 44, true, () -> selectSlot(slot, false));
                else divisionSlot(unitName(unit, false), unitTexture(unit, false), bx, by, 44, true, () -> selectSlot(slot, false));
            }
            int supportTitleY = y + 149 - designerScroll;
            if (supportTitleY >= y + 113 && supportTitleY + 11 < y + h - 53) {
                if (graphics != null) UiText.centeredSingleLine(graphics, font, Tooltips.officerText(IndustryEquipmentDesigns.tooltip("SUPPORT_HEADER"), true), x + 265, supportTitleY, 55, 10, TEXT);
                productionTip(IndustryEquipmentDesigns.tooltip("DESIGNER_SUPPORT_COLUMN_TITLE"), x + 265, supportTitleY, 55, 10);
            }
            for (int i = 0; i < 5; i++) {
                int slot = i, bx = x + 276, by = y + 160 + Math.round(i * 17.5f) - designerScroll;
                if (by < y + 113 || by + 16 > y + h - 53) continue;
                if (design != null && i > t.support().size() + 1) continue;
                if (i > t.support().size()) divisionSlot("앞선 빈 지원중대 칸부터 추가", "production/designer/locked", bx, by, 32, 16, false, () -> {});
                else if (i == t.support().size()) divisionSlot("지원중대 추가", "production/designer/add", bx, by, 32, 16, true, () -> selectSlot(slot, true));
                else divisionSlot(unitName(t.support().get(i), true), unitTexture(t.support().get(i), true), bx, by, 32, 16, true, () -> selectSlot(slot, true));
            }
            if (t.columns() != null) {
                int regimentTitleY = y + 285 - designerScroll;
                if (regimentTitleY >= y + 113 && regimentTitleY + 11 < y + h - 53) {
                    if (graphics != null) UiText.centeredSingleLine(graphics, font, Tooltips.officerText(IndustryEquipmentDesigns.tooltip("REGIMENTAL_SUPPORT_HEADER"), true), x + 82, regimentTitleY, 125, 10, TEXT);
                    productionTip(IndustryEquipmentDesigns.tooltip("DESIGNER_REGIMENTAL_SUPPORT_COLUMN_TITLE"), x + 82, regimentTitleY, 125, 10);
                }
                for (int column = 0; column < 5; column++) {
                    int bx = x + 27 + column * cell, by = y + 267 - designerScroll;
                    if (by < y + 113 || by + 16 > y + h - 53) continue;
                    boolean unlocked = t.columnSize(column) >= (design == null ? 3 : design.regimentBattalions());
                    divisionSlot(unlocked ? "연대 지원" : design == null ? dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("DESIGNER_BLOCKED_REGIMENTAL_SUPPORT") : design.regimentLocked(),
                            unlocked ? "production/designer/add" : "production/designer/locked", bx, by, 32, 16, false, () -> {});
                }
            }
        } else {
            if (graphics != null) UiAssets.nineSlice(graphics, "commander/commands/tiled_window_1b_border/frame_0", x, y + 20, 330, h - 73, 32, .5);
            if (graphics != null) UiText.centeredSingleLine(graphics, font, Tooltips.officerText(IndustryEquipmentDesigns.tooltip("REGIMENT_WINDOW_DESC"), true), x + 15, y + 25, 235, 13, TEXT);
            var pickerBack = button("대대 목록 닫기", IndustryEquipmentDesigns.tooltip("REGIMENTS_SELECTION_CANCEL"), x + 253, y + 21, 62, 17, true, () -> {
                if (!supportSlot && selectedSlot % 5 == 0 && !unitGroup.isEmpty()) unitGroup = "";
                else selectedSlot = -1;
                pickerScroll = 0; rebuildWidgets();
            });
            if (pickerBack != null) pickerBack.background("commander/commands/button_123x34/frame_0");
            int start = y + 47, cardWidth = 125, pitch = 35;
            String current = supportSlot ? (selectedSlot < t.support().size() ? t.support().get(selectedSlot) : "") : t.lineUnit(selectedSlot);
            if (!supportSlot && unitGroup.isEmpty()) {
                var groups = List.of("ARMOR", "MOBILE", "INFANTRY").stream()
                        .filter(g -> view.battalions().stream().anyMatch(c -> battalionGroup(c.id()).equals(g))).toList();
                for (int i = 0; i < groups.size(); i++) {
                    String group = groups.get(i), label = switch (group) { case "ARMOR" -> "기갑 대대"; case "MOBILE" -> "기동 대대"; default -> "보병 대대"; };
                    String preferred = switch (group) { case "ARMOR" -> "TANK"; case "MOBILE" -> "MOTORIZED_INFANTRY"; default -> "INFANTRY"; };
                    var candidates = view.battalions().stream().filter(c -> battalionGroup(c.id()).equals(group)).toList();
                    var choice = candidates.stream().filter(c -> c.id().equals(preferred)).findFirst().orElse(candidates.getFirst());
                    String detail = "";
                    unitChoice(label, choice.texture(), detail, x + 36 + i % 2 * 128, start + i / 2 * pitch, cardWidth, true,
                            () -> { unitGroup = group; pickerScroll = 0; rebuildWidgets(); });
                }
            } else {
                var choices = supportSlot ? view.companies().stream().filter(c -> c.id().equals(current) || !t.support().contains(c.id())).toList()
                        : view.battalions().stream().filter(c -> battalionGroup(c.id()).equals(unitGroup)).toList();
                int remove = current.isEmpty() ? 0 : 1, total = choices.size() + remove;
                int rows = Math.max(1, (y + h - 56 - start) / pitch);
                pickerScroll = Math.clamp(pickerScroll, 0, Math.max(0, (total + 1) / 2 - rows));
                for (int i = 0; i < total; i++) {
                    int row = i / 2 - pickerScroll, bx = x + 36 + i % 2 * 128, by = start + row * pitch;
                    if (row < 0 || row >= rows) continue;
                    var choice = i < remove ? null : choices.get(i - remove);
                    String id = choice == null ? "" : choice.id(), label = choice == null ? "제거" : choice.name();
                    boolean selected = !id.isEmpty() && id.equals(current);
                    boolean enabled = !selected && (choice != null || supportSlot || t.line().size() > 1);
                    Integer cost = design == null ? null : design.changeCosts().get((supportSlot ? "support:" : "line:") + current + ">" + id);
                    String detail = selected ? "(현재 선택됨)" : cost == null ? "" : cost + " ★";
                    unitChoice(label, choice == null ? "production/designer/remove" : choice.texture(), detail, bx, by, cardWidth, enabled, () -> {
                        int slot = selectedSlot; selectedSlot = -1;
                        send(supportSlot ? SUPPORT_SLOT : t.columns() == null ? LINE_SLOT : LINE_COLUMN, id, "", slot);
                    });
                }
            }
        }
        int statsX = x + (compact ? 19 : 334), statsWidth = 260, colWidth = 87;
        int statsY = compact ? y + 300 - designerScroll : y;
        String[] groups = {"BASE", "COMBAT", "EQUIPMENT_COST"}, titles = {"DESIGNER_BASE_STATS_LABEL", "DESIGNER_COMBAT_STATS_LABEL", "DESIGNER_EQUIPMENT_STATS_LABEL"};
        for (int col = 0; col < 3; col++) {
            int sx = statsX + col * colWidth;
            if (graphics != null && (!compact || statsY + 22 >= y + 113 && statsY + 34 <= y + h - 53)) {
                if (compact) graphics.fill(sx, statsY + 22, sx + colWidth - 1, statsY + 34, 0xFF999999);
                UiText.centeredSingleLine(graphics, font, Tooltips.officerText(IndustryEquipmentDesigns.tooltip(titles[col]), true), sx + 3, statsY + 22, colWidth - 5, 12, 0xFF202020);
            }
            String groupId = groups[col];
            var stats = t.stats().stream().filter(v -> v.group().equals(groupId) && !"HARDNESS".equals(v.id())).toList();
            var needs = col == 2 ? t.equipment().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList() : List.<Map.Entry<String,Long>>of();
            int count = stats.size() + needs.size();
            var bar = new UiScrollBar(sx + colWidth - 9, statsY + 34, statsY + 156, compact ? count : 12, count);
            divisionStatBars[col] = compact ? null : bar;
            divisionStatScroll[col] = Math.clamp(divisionStatScroll[col], 0, bar.maximum());
            for (int row = divisionStatScroll[col]; row < Math.min(count, divisionStatScroll[col] + (compact ? count : 12)); row++) {
                String label, amount, icon = "", detail = "";
                if (row < stats.size()) {
                    var stat = stats.get(row); label = stat.name() + ":"; amount = statValue(stat);
                    icon = IndustryEquipmentDesigns.divisionStatField(stat.id(), "texture");
                    detail = IndustryEquipmentDesigns.divisionStatField(stat.id(), "description");
                } else { var need = needs.get(row - stats.size()); label = familyName(equipment(need.getKey())); amount = integer(need.getValue()); }
                int sy = statsY + 37 + (row - divisionStatScroll[col]) * 10, contentWidth = 84;
                if (compact && (sy < y + 113 || sy + 10 > y + h - 53)) continue;
                int valueWidth = Math.min(contentWidth, Math.round(font.width(amount) * UiText.scale(font)));
                int iconWidth = icon.isEmpty() ? 0 : 10;
                if (graphics != null) {
                    if (compact) graphics.fill(sx, sy, sx + colWidth - 1, sy + 10, row % 2 == 0 ? 0xFF858585 : 0xFF909090);
                    if (!icon.isEmpty()) UiAssets.draw(graphics, icon, sx + 2, sy + 1, 9, 8);
                    UiText.marquee(graphics, font, label, sx + 2 + iconWidth, sy, contentWidth - valueWidth - iconWidth - 5, UiText.scale(font), 0xFF000000, false, -1);
                    UiText.text(graphics, font, Component.literal(amount).getVisualOrderText(), sx + contentWidth - valueWidth, sy, 0xFF000000, false);
                }
                productionTip(label + " " + amount + (detail.isEmpty() ? "" : "\n\n" + detail), sx, sy, contentWidth, 10);
            }
        }
        if (graphics != null) {
            if (compact) graphics.enableScissor(statsX, y + 113, statsX + statsWidth, y + h - 53);
            UiAssets.nineSlice(graphics, "production/design/division/adjustment_panel", statsX, y + (compact ? 476 - designerScroll : 183), statsWidth, 121, 8, 0.5);
            if (compact) graphics.disableScissor();
        }
        for (int i = 0; i < t.adjustments().size(); i++) {
            var value = t.adjustments().get(i);
            int columns = Math.min(5, t.adjustments().size() - i / 5 * 5);
            int bx = statsX + (statsWidth - ((columns - 1) * 50 + 39)) / 2 + i % 5 * 50, by = y + (compact ? 480 - designerScroll : 187) + i / 5 * 36;
            if (compact && (by < y + 113 || by + 36 > y + h - 53)) continue;
            if (graphics != null) IndustryTerrainAdjustment.draw(graphics, value, bx + 2, by + 2, n -> Math.round(n / 2f));
            else {
                var region = new UiHoverRegion("사단 조정치 · " + value.id(), bx, by, 39, 36);
                region.setTooltip(Tooltips.createExact(IndustryTerrainAdjustment.tooltip(value))); addRenderableWidget(region);
            }
        }
        if (design != null) {
            int resourceX = x + (w < 600 ? 195 : 274);
            for (int i = 0; i < design.resources().size(); i++) art(resourceArt(design.resources().get(i)), resourceX + i * 13, y + h - 46, 12, 12);
            productionTip(IndustryEquipmentDesigns.tooltip("DIVISION_PRODUCTION_RESOURCE_DESC"), resourceX, y + h - 46, Math.max(12, design.resources().size() * 13), 12);
        }
        String cost = IndustryEquipmentDesigns.tooltip(design == null || design.costMinimum() == design.costMaximum() ? "DIVISION_PRODUCTION_COST_SINGLE_VAL" : "DIVISION_PRODUCTION_COST_VAL")
                .replace("$VALUE|0$", design == null ? "—" : ic(design.costMinimum()))
                .replace("$MINVALUE|0$", design == null ? "—" : ic(design.costMinimum()))
                .replace("$MAXVALUE|0$", design == null ? "—" : ic(design.costMaximum()));
        int costX = x + (w < 600 ? 274 : 350), costWidth = x + w - 15 - costX;
        if (graphics != null) {
            graphics.enableScissor(costX, y + h - 51, costX + costWidth, y + h - 28);
            int separator = compact ? cost.indexOf(':') : -1;
            if (separator >= 0) {
                String label = cost.substring(0, separator + 1);
                var amount = dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal(cost.substring(separator + 1).strip()), false), false);
                UiText.text(graphics, font, label, costX + costWidth - Math.round(font.width(label) * UiText.scale(font)), y + h - 50, TEXT);
                UiText.text(graphics, font, amount, costX + costWidth - Math.round(font.width(amount) * UiText.scale(font)), y + h - 39, TEXT);
            } else {
                var line = dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal(cost), false), false);
                UiText.text(graphics, font, line, costX + costWidth - Math.round(font.width(line) * UiText.scale(font)), y + h - 44, TEXT);
            }
            graphics.disableScissor();
        }
        productionTip(IndustryEquipmentDesigns.tooltip("DIVISION_PRODUCTION_COST_DESC"), costX, y + h - 47, costWidth, 15);
        divisionTextButton("초기화", IndustryEquipmentDesigns.tooltip("DESIGNER_RESET"), "reset", IndustryEquipmentDesigns.tooltip("DESIGNER_RESET_DESC"), x + 10, y + h - 50, 62, 17, design != null, () -> {
            selectedSlot = -1; openingDesigner = true; divisionPicker = "";
            send(design.baseline().isEmpty() ? NEW_TEMPLATE : LOAD_TEMPLATE, design.baseline(), "", 0);
        });
        divisionTextButton("복제", IndustryEquipmentDesigns.tooltip("DESIGNER_DUPLICATE"), "duplicate", IndustryEquipmentDesigns.tooltip("DESIGNER_DUPLICATE_DESC"), x + 75, y + h - 50, 62, 18, design != null && !design.baseline().isEmpty(), () -> {
            openingDesigner = true; send(DUPLICATE_TEMPLATE, "", "", 0);
        });
        if (graphics != null) UiAssets.nineSlice(graphics, "commander/commands/generic_background/frame_0", x + 150, y + h - 48, 35, 15, 4, 0.5);
        iconText("hud/army_experience", view.hud().armyExperience() == null ? "—" : integer(view.hud().armyExperience()), x + 151, y + h - 47, 33, 13, 10, TEXT);
        productionTip(IndustryEquipmentDesigns.tooltip("CURRENT_EXPERIENCE"), x + 150, y + h - 48, 35, 15);
        if (graphics == null) {
            int bx = x + Math.min(243, (w - 111) / 2), by = y + h - 27;
            var save = new UiPanelButton("편제 저장", bx, by, 111, 18,
                    () -> send(SAVE_TEMPLATE, draftName, "", 0)) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    UiAssets.draw(g, "production/design/division/save", bx, by, 111, 18);
                    var label = Component.literal(integer(t.experienceCost())).withStyle(style -> style.withColor(Tooltips.GOLD))
                            .append(Component.literal(" 저장").withStyle(style -> style.withColor(0xFFFFFF)));
                    int labelWidth = Math.round(font.width(label) * UiText.scale(font));
                    int start = bx + (111 - 12 - labelWidth) / 2;
                    UiAssets.draw(g, "hud/army_experience", start, by + 4, 10, 10);
                    UiText.text(g, font, label, start + 12, by + 4, 0xFFFFFFFF);
                }
            };
            save.active = pending == 0 && !draftName.isBlank() && !t.line().isEmpty() && view.economy().armyXp() >= t.experienceCost();
            save.setTooltip(Tooltips.createExact(IndustryEquipmentDesigns.tooltip("DESIGNER_ARMY_SAVE").replace("$VALUE|Y0$", integer(t.experienceCost()))));
            addRenderableWidget(save);
        }
        if (graphics == null) layoutDivisionPicker(x, y, h, t);
    }
    private void divisionTextButton(String name, String label, String texture, String tooltip, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var control = new UiPanelButton(name, x, y, w, h, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                UiAssets.draw(g, "production/design/division/" + texture, x, y, w, h);
                UiText.cell(g, font, label, x, y, w, h, active ? TEXT : MUTED, false);
            }
        };
        control.active = enabled && pending == 0;
        control.setTooltip(Tooltips.createExact(tooltip)); addRenderableWidget(control);
    }
    private void layoutDivisionPicker(int x, int y, int h, Template template) {
        if (graphics == null) { divisionOptions.clear(); divisionPickerBar = null; }
        if (divisionPicker.isEmpty()) return;
        boolean names = divisionPicker.equals("names");
        var groups = template.designer() == null ? List.<NameGroup>of() : template.designer().nameGroups();
        int count = 1 + (names ? groups.size() : view.templates().size()), rowHeight = names ? 15 : 25;
        int px = x + 80, py = y + (names ? 70 : 43), rows = Math.max(1, Math.min(names ? 8 : 6, (y + h - 56 - py) / rowHeight));
        divisionPickerScroll = Math.clamp(divisionPickerScroll, 0, Math.max(0, count - rows));
        if (graphics != null) {
            graphics.nextStratum();
            graphics.fill(px, py, px + 115, py + rows * rowHeight + 5, 0xFF101012);
            UiAssets.tiledNineSlice(graphics, "officer/hq/list_background", px, py, 115, rows * rowHeight + 5, 64, 0.5);
        }
        var scrollbar = new UiScrollBar(px + 106, py + 2, py + rows * rowHeight + 2, rows, count);
        divisionPickerBar = scrollbar;
        if (graphics != null && scrollbar.maximum() > 0) scrollbar.draw(graphics, divisionPickerScroll);
        if (graphics != null) return;
        for (int index = divisionPickerScroll; index < Math.min(count, divisionPickerScroll + rows); index++) {
            int row = index, by = py + 2 + (index - divisionPickerScroll) * rowHeight;
            String label = names ? row == 0 ? IndustryEquipmentDesigns.tooltip("MODULE_NONE_FIXED") : groups.get(row - 1).name() : row == 0 ? IndustryEquipmentDesigns.tooltip("DESIGNER_CREATE_NEW_DIVISION") : view.templates().get(row - 1).name();
            String icon = names ? "" : row == 0 ? "production/design/division/add" : view.templates().get(row - 1).line().isEmpty() ? "" : unitTexture(view.templates().get(row - 1).line().getFirst(), false);
            var option = new UiPanelButton(label, px + 3, by, 102, rowHeight, () -> {
                divisionPicker = "";
                if (names) send(NAME_GROUP, row == 0 ? "" : groups.get(row - 1).id(), "", 0);
                else if (row == 0) { selectedSlot = -1; openingDesigner = true; send(NEW_TEMPLATE, "", "", 0); }
                else loadDesigner(view.templates().get(row - 1).id());
                rebuildWidgets();
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    g.nextStratum();
                    UiAssets.nineSlice(g, "commander/commands/mini_tooltip/frame_0", getX(), getY(), getWidth(), getHeight(), 4, 0.5);
                    int inset = icon.isEmpty() ? 3 : 28;
                    if (!icon.isEmpty()) UiAssets.draw(g, icon, getX() + 2, getY() + 2, 27, 20);
                    UiText.left(g, font, label, getX() + inset, getY(), getWidth() - inset - 2, getHeight(), TEXT);
                }
            };
            option.active = pending == 0;
            if (names && row > 0) {
                var group = groups.get(row - 1);
                String tooltip = IndustryEquipmentDesigns.tooltip("DIVISION_NAMES_GROUP_LIST_HEADER");
                for (String example : group.examples()) tooltip += IndustryEquipmentDesigns.tooltip("DIVISION_NAMES_GROUP_LIST_ENTRY").replace("$NAME$", example);
                tooltip += IndustryEquipmentDesigns.tooltip("DIVISION_NAMES_GROUP_LIST_ENTRY_FALLBACK").replace("$NAME$", group.fallback());
                option.setTooltip(Tooltips.createExact(tooltip));
            } else option.setTooltip(Tooltips.createExact(label));
            divisionOptions.add(option); addRenderableWidget(option);
        }
    }
    private static String statValue(IndustryView.Stat stat) {
        return IndustryEquipmentDesigns.divisionStatValue(stat);
    }
    private void selectSlot(int slot, boolean support) {
        selectedSlot = slot; supportSlot = support; pickerScroll = 0;
        String head = support ? "" : view.draft().lineUnit(slot / 5 * 5);
        unitGroup = support || slot % 5 == 0 ? "" : battalionGroup(head); rebuildWidgets();
    }
    static String battalionGroup(String id) {
        return switch (id) {
            case "TANK", "LIGHT_TANK", "SELF_PROPELLED_ARTILLERY", "SELF_PROPELLED_AIR_DEFENSE", "AMPHIBIOUS_ARMOR", "MOTORIZED_ROCKET_ARTILLERY", "hq_light_armor", "hq_medium_armor", "hq_heavy_armor" -> "ARMOR";
            case "MOTORIZED_MILITIA", "MOTORIZED_INFANTRY", "MECHANIZED_INFANTRY", "LIGHT_MECHANIZED", "AMPHIBIOUS_MECHANIZED", "HEADQUARTERS", "hq_motorized", "hq_armored_car" -> "MOBILE";
            default -> "INFANTRY";
        };
    }
    private UiScrollBar manufacturerScrollbar;
    private boolean manufacturerDragging;
    private void layoutManufacturers() {
        var line = view.lines().stream().filter(l -> l.id().equals(manufacturerLine)).findFirst().orElse(null);
        var offer = equipmentById.get(manufacturerEquipment);
        var designTeam = designerManufacturer && tankDesigner() ? view.tankDesigner().designTeam() : null;
        if (line == null && offer == null && designTeam == null) return;
        int firstWidget = children().size();
        int w = Math.min(IndustryOrganizationList.WIDTH, width - 16), h = IndustryOrganizationList.HEIGHT, x = Math.min(width - w - 8, pane + 88), y = hudBottom() + 3, end = Math.min(bottom(), y + h);
        if (offer != null || designTeam != null) {
            x = Math.min(width - w - 8, manufacturerAnchorX);
            y = Math.clamp(manufacturerAnchorY, hudBottom() + 3, Math.max(hudBottom() + 3, bottom() - h));
            end = Math.min(bottom(), y + h);
        }
        if (designTeam != null) { x = Math.min(width - w - 8, 133); y = Math.max(hudBottom() + 3, 40); end = Math.min(height - 4, y + h); }
        manufacturerX = x; manufacturerY = y; manufacturerWidth = w; manufacturerBottom = end;
        if (graphics != null) { graphics.nextStratum(); IndustryOrganizationList.frame(graphics, font, x, y, w, end - y); }
        else addRenderableWidget(IndustryOrganizationList.close(x, y, w, this::closeManufacturers));
        var sortSize=originalButton("규모 정렬","mio/ui/gfx_sort_skill/frame_"+(manufacturerSorted&&!manufacturerName?manufacturerReverse?2:1:0),x+8,y+24,15,15,true,()->{manufacturerReverse=!manufacturerSorted||manufacturerName||!manufacturerReverse;manufacturerName=false;manufacturerSorted=true;rebuildWidgets();});
        var sortName=button("이름 정렬","이름",x+23,y+24,41,15,true,()->{manufacturerReverse=manufacturerSorted&&manufacturerName&&!manufacturerReverse;manufacturerName=true;manufacturerSorted=true;rebuildWidgets();});
        if(graphics==null){sortSize.sound("ui.scroll");sortName.background("mio/ui/gfx_sort_up_down_button_83x29/frame_"+(manufacturerSorted&&manufacturerName?manufacturerReverse?2:1:0)).sound("ui.scroll");}
        var rows = new ArrayList<>(designTeam != null ? designTeam.choices().stream().map(IndustryTankDesigner.DesignTeamChoice::manufacturer).toList() : offer == null ? line.manufacturers() : offer.manufacturers());
        if (manufacturerName) rows.sort(Comparator.comparing(LineManufacturer::name));
        else if (manufacturerSorted) rows.sort(Comparator.comparingInt(LineManufacturer::size));
        if (manufacturerReverse) Collections.reverse(rows);
        int entry = IndustryOrganizationList.ENTRY_HEIGHT, entryWidth = IndustryOrganizationList.ENTRY_WIDTH, start = y + IndustryOrganizationList.LIST_TOP, listEnd = end - 10;
        manufacturerScroll = Math.clamp(manufacturerScroll, 0, Math.max(0, entry + rows.size() * entry - (listEnd - start)));
        manufacturerScrollbar=new UiScrollBar(x+w-11,start,listEnd,listEnd-start,entry+rows.size()*entry);
        if(graphics!=null)manufacturerScrollbar.draw(graphics,manufacturerScroll);
        int emptyY = start - manufacturerScroll;
        if(emptyY>=start&&emptyY+entry<=listEnd){
            if(graphics!=null)IndustryOrganizationList.empty(graphics,font,designTeam != null ? designTeam.empty() : offer == null ? line.manufacturerEmpty() : offer.manufacturerEmpty(),x+8,emptyY);
            else addRenderableWidget(new UiPanelButton(designTeam != null ? designTeam.empty() : "군수산업체 없이 생산",x+8,emptyY,entryWidth,entry,()->selectManufacturer("")){@Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){}});
        }
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i); int ry = start + entry + i * entry - manufacturerScroll;
            if (ry < start || ry + entry > listEnd) continue;
            if (graphics != null) IndustryOrganizationRow.draw(graphics, row.icon(), row.size(), row.name(), row.type(), row.equipment(), x + 8, ry, entryWidth);
            if (graphics == null) {
                var details = IndustryOrganizationList.details(font, row.id(), x + 8, ry);
                details.active = pending == 0; addWidget(details);
                var select = new UiPanelButton(row.name(), x + 8, ry, entryWidth, entry, () -> selectManufacturer(row.id())) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {}
                };
                select.active = pending == 0; select.setTooltip(Tooltips.createExact(row.tooltip())); addRenderableWidget(select); addRenderableOnly(details);
            }
        }
        if (graphics == null) {
            manufacturerWidgets.clear();
            children().subList(firstWidget, children().size()).stream().filter(widget -> widget instanceof net.minecraft.client.gui.components.AbstractWidget).map(widget -> (net.minecraft.client.gui.components.AbstractWidget) widget).forEach(manufacturerWidgets::add);
        }
    }
    private void closeManufacturers() {
        if (pending > 0) return;
        if (!manufacturerEquipment.isEmpty()) {
            manufacturerEquipment = "";
            send(OFFER, "", "", 0);
        } else picker = "";
        designerManufacturer = false;
        manufacturerDragging = false; manufacturerScroll = 0; rebuildWidgets();
    }
    private void selectManufacturer(String id) {
        if (pending > 0) return;
        if (designerManufacturer) { designerManufacturer = false; send(TANK_DESIGN_TEAM, "", id, 0); return; }
        if (!manufacturerEquipment.isEmpty()) {
            String item = manufacturerEquipment;
            manufacturerEquipment = ""; picker = ""; send(ADD, item, id, 0); return;
        }
        picker = ""; send(MANUFACTURER, manufacturerLine, id, 0);
    }
    private void layoutPicker() {
        if(picker.equals("manufacturer")){layoutManufacturers();return;}
        if(picker.equals("naval_ports")){layoutNavalPorts();return;}
        if (picker.equals("equipment") || picker.equals("repairs")) { layoutProductionPicker(); return; }
        int w = Math.min(270, width - 16), x = width - pane >= w + 8 ? pane + 4 : (width - w) / 2, y = hudBottom() + 3;
        panel(x, y, w, height - y - 27);
        text("배치 장소 지정", x + 9, y + 8, w - 40, TEXT);
        button("목록 닫기", "×", x + w - 25, y + 3, 19, 19, true, () -> { picker = ""; rebuildWidgets(); });
        int start = y + 29;
        pickerScroll = clampScroll(pickerScroll, view.locations().size(), 25, start);
        for (int i = 0; i < view.locations().size(); i++) {
            var l = view.locations().get(i); int by = start + i * 25 - pickerScroll;
            if (!visible(by, 25, start)) continue;
            button(l.name(), l.name(), x + 7, by, w - 14, 22, true, () -> { location = l.id(); picker = ""; send(LOCATION, locationRecruit, l.id(), 0); rebuildWidgets(); });
        }
    }
    private void selectNavalPort(String port) {
        send(NAVAL_PORT,navalProductionLine,port,0);navalProductionLine="";picker="";
        pickerScroll=0;rebuildWidgets();
    }
    private void layoutNavalPorts() {
        int w=productionPickerWidth(),x=width-pane>=w+4?pane+2:(width-w)/2,y=hudBottom()+3,end=Math.min(bottom(),y+Math.round(height*.7f));
        double scale=w/375.0;
        panel(x,y,w,end-y);
        heading(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("PRODUCTION_NAVAL_DEPLOY_LIST"),x+9,y+7,w-38,TEXT);
        var closeFrame=new UiMenuStyle.Frame(x,y,(float)scale);
        sourceButton("×",closeFrame,w/scale-43,9,32,33,()->{picker="";rebuildWidgets();},(g,b)->closeFrame.art(g,"menu/close",w/scale-43,9,32,33));
        var deployment=view.navalDeployment();
        if(deployment==null)return;
        int start=y+Math.round((float)(57*scale)),row=Math.max(18,Math.round((float)(36*scale)));
        var targets=new ArrayList<IndustryView.NavalTarget>();
        targets.add(new IndustryView.NavalTarget("",dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("PRODUCTION_NAVAL_DEPLOYMENT_AUTO_DEPLOYMENT"),"",0,0));
        targets.addAll(deployment.targets());
        pickerScroll=Math.clamp(pickerScroll,0,Math.max(0,targets.size()*row-(end-start-6)));
        for(int i=0;i<targets.size();i++) {
            var target=targets.get(i);int by=start+i*row-pickerScroll;
            if(by<start||by+row>end-6||graphics!=null)continue;
            int bx=x+(int)Math.round(12*scale),bw=(int)Math.round(335*scale),bh=row-1;
            var entry=new UiPanelButton(target.name(),bx,by,bw,bh,()->selectNavalPort(target.id())) {
                @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice) {
                    if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT&&isMouseOver(event.x(),event.y())&&!target.port().isEmpty()) {
                        if(active){playDownSound(minecraft.getSoundManager());send(NAVAL_LOCATE,target.id(),"",0);}return true;
                    }
                    return super.mouseClicked(event,twice);
                }
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    UiAssets.draw(g,"production/deployment/entry",bx,by,bw,bh);
                    int icon=Math.max(12,Math.round(bh*31f/35)),iconCell=Math.round(bw*34f/335);
                    UiAssets.draw(g,"production/deployment/"+(target.id().isEmpty()?"theater":"port"),bx+(iconCell-icon)/2,by+(bh-icon)/2,icon,icon);
                    if(target.fleet())UiText.cell(g,font,Integer.toString(target.ships()),bx,by+bh/2,iconCell,bh/2,TEXT,false);
                    String caption=target.fleet()||target.id().isEmpty()?target.name():target.name()+" (§e"+target.level()+"§r)";
                    UiText.cell(g,font,caption,bx+iconCell,by,bw-iconCell-4,bh,TEXT,false);
                }
            };
            entry.active=pending==0;
            entry.setTooltip(Tooltips.createExact(IndustryNavalDeployment.tooltip(target)));
            addRenderableWidget(entry);
        }
        pickerScrollbar(x+w-11,y+Math.round((float)(43*scale)),end-6,end-start-6,targets.size()*row,row);
    }
    private void layoutProductionPicker() {
        boolean repair=picker.equals("repairs"), naval=group.equals("navy");
        int w=productionPickerWidth();
        int x=width-pane>=w+4?pane+2:(width-w)/2,y=hudBottom()+3;
        if (!manufacturerEquipment.isEmpty()) x = Math.max(4, Math.min(x, width - w - 278));
        int end=repair?Math.min(bottom(),y+Math.round(w*600f/444)):bottom();
        if(graphics!=null)UiAssets.nineSlice(graphics,"production/picker/panel",x,y,w,end-y,64,w/(repair?444.0:495.0));
        if(repair) {
            art("production/picker/repair_title",x+3,y+2,w-6,Math.round((w-6)*41f/430));
            heading("수리 대기열",x+18,y+7,w-48,TEXT);
        }
        double closeScale=w/(repair?444.0:495.0),closeX=repair?396:w/closeScale-50,closeY=repair?10:5;
        var closeFrame=new UiMenuStyle.Frame(x,y,(float)closeScale);
        sourceButton("×",closeFrame,closeX,closeY,32,33,()->{picker="";rebuildWidgets();},(g,b)->closeFrame.art(g,"menu/close",closeX,closeY,32,33));
        if(repair) { layoutRepairPorts(x,y,w,end); return; }
        if(graphics==null) {
            var toggle=new UiPanelButton("구형 장비 표시",x+63,y+5,w-91,16,()->{showOutdated=!showOutdated;pickerScroll=0;rebuildWidgets();}) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    var label=Component.literal("구형 장비").withColor(GOLD).append(Component.literal(" 표시").withColor(TEXT));
                    int labelWidth=(int)Math.ceil(font.width(label)*UiText.scale(font));
                    int labelAreaWidth=labelWidth+8,groupWidth=13+3+labelAreaWidth;
                    int groupX=getX()+(getWidth()-groupWidth)/2;
                    UiAssets.draw(g,"production/picker/text_bg",groupX+16,getY()+(getHeight()-14)/2,labelAreaWidth,14);
                    g.pose().pushMatrix();g.pose().translate(groupX,getY()+(getHeight()-13)/2);g.pose().scale(1,14f/13);
                    UiAssets.draw(g,"production/picker/check_"+(showOutdated?"on":"off"),0,0,13,13);
                    g.pose().popMatrix();
                    UiText.text(g,font,label,groupX+20,getY()+(getHeight()-Math.round(font.lineHeight*UiText.scale(font)))/2,TEXT);
                }
            };
            toggle.sound("ui.checkbox");toggle.active=pending==0;addRenderableWidget(toggle);
        }
        if (graphics != null) UiAssets.nineSlice(graphics,"commander/commands/mini_bg/frame_0",x+7,y+5,45,16,12,.5);
        art("production/picker/manufacturer",x+11,y+7,12,12);
        var offered = equipmentById.get(manufacturerEquipment);
        if (offered == null) offered = productionItems.stream().filter(item -> children().stream().anyMatch(widget -> widget instanceof UiPanelButton b && b.getMessage().getString().equals("생산: " + item.name()) && b.isMouseOver(mouseX, mouseY))).findFirst().orElse(null);
        cellText(Integer.toString(offered == null ? 0 : offered.manufacturerChoices()),x+26,y+5,24,16,GOLD,false);
        var items=productionItems;
        int filtersY=y+25;
        if(naval) {
            var types=navalTypes;
            int cell=(w-58)/4,filterHeight=Math.max(11,Math.round(cell/3f));
            for(int i=0;i<types.size();i++) {
                var model=types.get(i);String type=model.family();
                int bx=x+6+i%4*cell,by=filtersY+i/4*filterHeight;
                if(graphics==null) {
                    var filter=new UiPanelButton(familyName(model),bx,by,cell,filterHeight,()->{navalFamily=navalFamily.equals(type)?"":type;pickerScroll=0;rebuildWidgets();}) {
                        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                            UiAssets.draw(g,"production/picker/filter_bg",bx,by,getWidth(),getHeight());
                            if(navalFamily.equals(type)||isHoveredOrFocused())UiAssets.draw(g,"production/picker/filter_highlight",bx,by,getWidth(),getHeight());
                            String icon=navalSilhouette(type);
                            UiAssets.draw(g,icon,bx+4,by+3,getWidth()-8,getHeight()-5);
                            long count=navalCounts.getOrDefault(type,0L);
                            UiText.text(g,font,Long.toString(count),bx+1,by,TEXT);
                        }
                    };
                    filter.sound("ui.checkbox");filter.active=pending==0;filter.setTooltip(Tooltips.create(Component.literal(familyName(model))));addRenderableWidget(filter);
                }
            }
        } else {
            var filters=view.resources().stream().sorted(Comparator.comparingInt(r->switch(r.id().toLowerCase(Locale.ROOT)){case "oil" -> 0;case "rubber" -> 1;case "iron","steel" -> 2;case "aluminum" -> 3;case "tungsten" -> 4;case "chromium" -> 5;default -> 6;})).toList();
            int cell=(w-12)/Math.max(1,filters.size());
            for(int i=0;i<filters.size();i++) {
                var resource=filters.get(i);int bx=x+6+i*cell;
                art(resourceArt(resource.id()),bx,filtersY,12,16);
                originalButton(resource.name()+" 사용 장비 표시","production/picker/check_"+(hiddenResources.contains(resource.id())?"off":"on"),bx+13,filtersY,Math.max(10,cell-14),16,()->{
                    if(!hiddenResources.add(resource.id()))hiddenResources.remove(resource.id());pickerScroll=0;rebuildWidgets();
                });
            }
        }
        int listTop=y+Math.round(w*(naval?112f:85f)/495);
        int start=listTop+Math.round(w*(naval?14f:13f)/495);
        int listBottom=end-Math.round(w*(naval?14f:12f)/495);
        var current=view.lines().stream().filter(l->l.id().equals(switchLine)).findFirst().orElse(null);
        int row=Math.max(34,Math.round((w-12)*80f/460));
        pickerScroll=Math.clamp(pickerScroll,0,Math.max(0,items.size()*row-(listBottom-start)));
        if(graphics!=null)UiAssets.nineSlice(graphics,"production/picker/list",x+3,start-2,w-6,end-start+1,64,w/495.0);
        for(int i=0;i<items.size();i++) {
            var equipment=items.get(i);int by=start+i*row-pickerScroll;
            if(by<start||by+row>listBottom)continue;
            boolean selected=current!=null&&current.equipment().equals(equipment.id());
            pickerEquipment(equipment,x+6,by,w-18,row-1,selected);
            if (equipment.id().equals(manufacturerEquipment)) { manufacturerAnchorX = x + w + 2; manufacturerAnchorY = by; }
        }
        pickerScrollbar(x+w-11,listTop,listBottom,listBottom-start,items.size()*row,Math.round(w*41f/495));
        if(items.isEmpty())text("생산 가능한 장비가 없습니다.",x+9,start+8,w-18,MUTED);
        if (!manufacturerEquipment.isEmpty() && !equipment(manufacturerEquipment).manufacturers().isEmpty()) layoutManufacturers();
    }
    private void pickerEquipment(Equipment equipment,int x,int y,int w,int h,boolean selected) {
        var design=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionDesign(equipment);
        if(graphics!=null) {
            productionTip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionObsoleteTooltip(equipment.outdated()),x+w-20,y+1,17,16);
            productionTip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("PRODUCTION_CREATE_VARIANT"),x+w-23,y+h-23,19,19);
            if(equipment.group().equals("air")&&equipment.designer()!=null)for(int i=0;i<equipment.designer().missions().size();i++) {
                float scale=w/458f;
                productionTip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionMissionTooltip(equipment.designer().missions().get(i)),x+Math.round((197+i%6*25)*scale),y+Math.round((60-i/6*20)*scale),Math.round(25*scale),Math.round(20*scale));
            }
            return;
        }
        var rowResources=equipment.resources().entrySet().stream().limit(3).toList();
        boolean blueprint=design.blueprint(),unfinished=design.unfinished(),tank=design.editable();
        var button=new UiPanelButton("생산: "+equipment.name(),x,y,w-25,h,()->{if(unfinished)openTank(equipment);else chooseEquipment(equipment.id());}) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                String background=equipment.naval()?"naval":"land";
                UiAssets.draw(g,unfinished?"production/tank/unfinished":"production/picker/"+background+(selected?"_selected":""),x,y,w,h);
                float scale=w/458f;
                int imageWidth=Math.round((equipment.naval()?263:193)*scale);
                if(!UiAssets.draw(g,equipment.texture()+"_"+view.country().toLowerCase(Locale.ROOT),x+3,y+3,imageWidth,h-6))UiAssets.draw(g,equipment.texture(),x+3,y+3,imageWidth,h-6);
                int resourceX=x+Math.round(366*scale),resourceRight=x+Math.round(403*scale);
                if(equipment.outdated())UiAssets.cover(g,"production/picker/outdated_overlay",x+3,y+3,resourceX-1-(x+3),h-6);
                int labelX=x+Math.round((equipment.naval()?62:206)*scale),right=x+Math.round(337*scale)-3;
                if(equipment.group().equals("air")) {
                    int roleWidth=Math.round(27*scale),roleRight=resourceX-Math.max(3,Math.round(5*scale));
                    right=Math.min(right,roleRight-roleWidth-3);
                    UiAssets.draw(g,dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionRoleIcon(equipment),roleRight-roleWidth,y+Math.round(5*scale),roleWidth,Math.round(14*scale));
                    if(equipment.designer()!=null)for(int i=0;i<equipment.designer().missions().size();i++) {
                        String mission=equipment.designer().missions().get(i);
                        UiAssets.draw(g,dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionMissionIcon(mission),x+Math.round((197+i%6*25)*scale),y+Math.round((60-i/6*20)*scale),Math.round(25*scale),Math.round(20*scale));
                    }
                }
                String landRole=IndustryProductionIndicators.landRole(equipment);
                var units=new dev.hoi.client.ui.UiUnits(width,height);
                if(!unfinished&&!blueprint&&!landRole.isEmpty())UiAssets.draw(g,landRole,x+imageWidth-Math.round(36*scale)-Math.round(units.length(3)),y+h-Math.round(35*scale)+Math.round(units.length(1)),Math.round(36*scale),Math.round(35*scale));
                if(equipment.naval()) {
                    pickerRightText(g,equipment.name(),labelX,y+Math.round(6*scale),right-labelX);
                    pickerRightText(g,familyName(equipment),labelX,y+Math.round(22*scale),right-labelX);
                } else {
                    UiText.marquee(g,font,Tooltips.officerText(equipment.name()),labelX,y+Math.round(6*scale),right-labelX,UiText.scale(font),TEXT,true,-1);
                    var lines=Tooltips.split(font,Component.literal(familyName(equipment)),Math.max(1,(int)((right-labelX)/UiText.scale(font))));
                    for(int i=0;i<Math.min(equipment.group().equals("air")?2:3,lines.size());i++) {
                        UiText.text(g,font,lines.get(i),labelX,y+Math.round(22*scale)+i*UiText.MIN_HEIGHT,TEXT);
                    }
                }
                int costX=x+5,costY=y+h-13,iconSize=10;
                UiAssets.draw(g,equipment.naval()?"production/picker/naval_cost":"keywords/stat_common_build_cost_ic",costX,costY,iconSize,iconSize);
                UiText.cell(g,font,ic(equipment.cost()),costX+iconSize+1,costY,Math.round(font.width(ic(equipment.cost()))*UiText.scale(font)),iconSize,GOLD,false);
                int resourceWidth=resourceRight-resourceX,resourceIcon=Math.min(10,Math.max(6,(resourceWidth-4)/2));
                int resourceTop=y+Math.round(3*h/80f),resourceBottom=y+Math.round(77*h/80f);
                int resourceRow=Math.max(resourceIcon,Math.round(font.lineHeight*UiText.scale(font)));
                int count=rowResources.size(),gap=count<2?0:Math.min(2,Math.max(0,(resourceBottom-resourceTop-count*resourceRow)/(count-1)));
                int groupHeight=count*resourceRow+Math.max(0,count-1)*gap;
                int amountWidth=Math.min(Math.max(1,resourceWidth-resourceIcon-3),rowResources.stream()
                        .mapToInt(resource->Math.round(font.width(integer(resource.getValue()))*UiText.scale(font))).max().orElse(0));
                int groupX=resourceX+(resourceWidth-resourceIcon-2-amountWidth)/2-1;
                int groupY=resourceTop+(resourceBottom-resourceTop-groupHeight)/2;
                for(int i=0;i<count;i++) {
                    var resource=rowResources.get(i);int ry=groupY+i*(resourceRow+gap);
                    UiAssets.draw(g,resourceArt(resource.getKey()),groupX,ry+(resourceRow-resourceIcon)/2,resourceIcon,resourceIcon);
                    UiText.cell(g,font,integer(resource.getValue()),groupX+resourceIcon+2,ry,amountWidth,resourceRow,TEXT,true);
                }
                if(unfinished||blueprint)UiAssets.draw(g,"production/tank/empty_blueprint",x+imageWidth-13,costY+(iconSize-12)/2,14,12);
                UiAssets.draw(g,"production/picker/obsolete",x+w-16,y+4,9,9);
                UiAssets.draw(g,design.icon(),x+w-23,y+h-23,19,19);
            }
        };
        button.active=pending==0&&!selected&&!blueprint;
        button.setTooltip(Tooltips.createExact(IndustryProductionTooltips.equipment(view,equipment)));
        if(unfinished)button.setTooltip(Tooltips.create(Component.literal((equipment.designWarning()==null||equipment.designWarning().isEmpty())?"설계를 열어 모듈을 구성하세요.":equipment.designWarning())));
        if(blueprint)button.setTooltip(Tooltips.create(Component.literal(Objects.toString(equipment.designWarning(),""))));
        addRenderableWidget(button);
        if(tank)originalButton(design.title(),design.icon(),x+w-23,y+h-23,19,19,true,()->openTank(equipment))
                .setTooltip(Tooltips.createExact(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("PRODUCTION_CREATE_VARIANT")));
    }
    private static String tankErrors(IndustryTankDesigner view) {
        var missing=view.slots().stream().filter(s->s.required()&&s.selected().isEmpty()).map(IndustryTankDesigner.Slot::name).toList();
        var errors=new ArrayList<String>();
        if(!missing.isEmpty())errors.add(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("REQUIRED_MODULE_EMPTY")+"\n§e"+String.join("\n",missing));
        view.errors().stream().filter(e->!missing.contains(e)).forEach(e->errors.add("§c"+e));
        return String.join("\n\n",errors);
    }
    private boolean tankDesigner() {return view!=null&&view.tankDesigner()!=null;}
    private void openTank(Equipment equipment) {tankSlot="";tankScroll=0;send(TANK_OPEN,equipment.id(),"",0);}
    static String tankValue(IndustryTankDesigner.Stat stat) {
        return dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statValue(stat,stat.value());
    }
    private static String tankChanges(List<IndustryTankDesigner.Stat> stats) {
        var order=List.of("breakthrough","maximum_speed","armor_value","reliability","fuel_consumption","build_cost_ic");
        return stats.stream().sorted(Comparator.comparingInt(s->order.contains(s.id())?order.indexOf(s.id()):order.size()))
                .map(s->"§f"+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statCaption(s.id(),s.name()).strip()+" "+(s.id().equals("reliability")?(s.value()>0?"§a":"§c"):"§e")+tankValue(s)).collect(java.util.stream.Collectors.joining("\n"));
    }
    static String tankUpgradeChanges(IndustryTankDesigner.Upgrade upgrade,int levels) {
        return tankChanges(upgrade.perLevel().stream().map(stat->new IndustryTankDesigner.Stat(stat.id(),stat.name(),stat.value()*levels,stat.percent())).toList());
    }
    private IndustryTankDesigner.UpgradeStep tankUpgradeStep(IndustryTankDesigner.Upgrade upgrade,boolean increase) {
        int amount=(increase?1:-1)*tankStep();
        return upgrade.steps().stream().filter(step->step.amount()==amount).findFirst().orElse(null);
    }
    private String tankUpgradeTooltip(IndustryTankDesigner.Upgrade upgrade,boolean increase) {
        var step=tankUpgradeStep(upgrade,increase);
        if(increase&&upgrade.level()>=upgrade.maximum()&&upgrade.maximum()<20)
            return dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_NEXT_LEVEL_BLOCKED_BY")+"\n"+String.join("\n",upgrade.requirements());
        if(increase&&upgrade.level()>=upgrade.maximum()||!increase&&upgrade.level()==0)
            return dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_ALREADY_AT_"+(increase?"HIGHEST":"LOWEST")+"_LEVEL");
        int levels=step==null?(increase?1:-1):step.level()-upgrade.level();
        var resources=step==null?(increase?upgrade.resourceIncrease():upgrade.resourceDecrease()):step.resources();
        String result=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_"+(increase?"NEXT":"PREV")+"_UPGRADE_GRANTS")+"\n"+tankUpgradeChanges(upgrade,levels);
        if(!resources.isEmpty())result+="\n\n"+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_RESOURCE_CHANGE")+"\n"+resources.entrySet().stream().map(r->"§f"+resourceName(r.getKey())+": §e"+(r.getValue()>0?"+":"")+integer(r.getValue())).collect(java.util.stream.Collectors.joining("\n"));
        int cost=step==null?1:step.experience();
        String key=cost<0?"EQUIPMENT_UPGRADE_PREV_UPGRADE_REFUND":"EQUIPMENT_UPGRADE_"+(increase?"NEXT":"PREV")+"_UPGRADE_COST";
        String costText=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip(key).replace("$XPICON$","£army_experience ").replace("$COST|H0$","§e"+Math.abs(cost)+"§f");
        String sign=increase?"+":"-";
        return result+"\n\n§f"+costText+"\n\n§a클릭§f하여 §e"+sign+"1\n§aCtrl + 클릭§f하여 §e"+sign+"5\n§aShift + 클릭§f하여 §e"+sign+"10";
    }
    private void tankUpgradeButton(IndustryTankDesigner.Upgrade upgrade,boolean increase,int x,int y,int size) {
        if(graphics!=null)return;
        var button=new UiPanelButton(upgrade.name()+(increase?" 증가":" 감소"),x,y,size,size,()->{
            var step=tankUpgradeStep(upgrade,increase);
            send(TANK_UPGRADE,upgrade.id(),"",step==null?Math.clamp(upgrade.level()+(increase?1:-1)*tankStep(),0,upgrade.maximum()):step.level());
        }) {
            private int previousStep;
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiAssets.draw(g,"production/design/controls/"+(increase?"increase":"decrease"),x,y,size,size);
                if(previousStep!=tankStep()) {previousStep=tankStep();setTooltip(Tooltips.createExact(tankUpgradeTooltip(upgrade,increase)));}
            }
        };
        button.sound("ui.designer.tank."+(increase?"increase":"decrease"));
        button.active=pending==0&&(increase?upgrade.level()<upgrade.maximum():upgrade.level()>0);
        button.setTooltip(Tooltips.createExact(tankUpgradeTooltip(upgrade,increase)));addRenderableWidget(button);
    }
    private static String tankStatIcon(String id) {
        return "production/design/stats/"+id;
    }
    private int tankStep() {
        if(factoryStep()==10)return 10;
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL)||com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RCONTROL)?5:1;
    }
    private static String designerTitle(String branch) {return switch(branch) {case "navy"->"함선 설계사";case "air"->"항공기 설계사";default->"전차 설계사";};}
    private String designBranch() {return !tankDesigner()||view.tankDesigner().chassis().startsWith("modern_tank_")?"army":view.tankDesigner().chassis().startsWith("ship_")?"navy":"air";}
    private String designMission="";
    private void layoutEquipmentDesigner() {
        var d=view.tankDesigner();
        var displayedStats=d.missions().stream().filter(m->m.id().equals(designMission)).map(IndustryTankDesigner.Mission::stats).findFirst().orElse(d.stats());
        var statsById=new HashMap<String,IndustryTankDesigner.Stat>();
        displayedStats.forEach(stat->statsById.putIfAbsent(stat.id(),stat));
        String branch=designBranch(),title=designerTitle(branch);
        boolean armor=branch.equals("army"),navy=branch.equals("navy");
        Double experience=armor?view.economy().armyXp():navy?view.hud().navyExperience():view.hud().airExperience();
        int minY=hudBottom()+font.lineHeight+8;
        int w=Math.min(Math.min(width-24,Math.max(780,width*5/8)),(height-minY-12)*1092/553);
        int h=w*553/1092,x=(width-w)/2,y=Math.max(minY,(height-h)/2);
        java.util.function.IntUnaryOperator size=n->Math.max(1,n*w/1092);
        java.util.function.IntUnaryOperator px=n->x+n*w/1092,py=n->y+n*w/1092;
        var preview=new HashMap<String,Double>();
        if (designerManufacturer && d.designTeam() != null) {
            var rows = new ArrayList<>(d.designTeam().choices());
            if (manufacturerName) rows.sort(Comparator.comparing(choice -> choice.manufacturer().name()));
            else if (manufacturerSorted) rows.sort(Comparator.comparingInt(choice -> choice.manufacturer().size()));
            if (manufacturerReverse) Collections.reverse(rows);
            for (int i = 0; i < rows.size(); i++) {
                int entry = IndustryOrganizationList.ENTRY_HEIGHT, rowY = manufacturerY + IndustryOrganizationList.LIST_TOP + entry + i * entry - manufacturerScroll;
                if (rowY >= manufacturerY + IndustryOrganizationList.LIST_TOP && rowY + entry <= manufacturerBottom - 10 && mouseX >= manufacturerX + 8 && mouseX < manufacturerX + 8 + IndustryOrganizationList.ENTRY_WIDTH && mouseY >= rowY && mouseY < rowY + entry) rows.get(i).changes().forEach(stat -> preview.put(stat.id(), stat.value()));
            }
        }
        if(!designerManufacturer&&tankSlot.isEmpty()&&mouseY>=py.applyAsInt(460)&&mouseY<py.applyAsInt(493))for(int i=0;i<d.upgrades().size();i++) {
            var upgrade=d.upgrades().get(i);int bx=323+i*106;
            if(mouseX>=px.applyAsInt(bx)&&mouseX<px.applyAsInt(bx)+size.applyAsInt(33)&&upgrade.level()>0)(tankUpgradeStep(upgrade,false)==null?upgrade.decrease():tankUpgradeStep(upgrade,false).changes()).forEach(stat->preview.put(stat.id(),stat.value()));
            if(mouseX>=px.applyAsInt(bx)+size.applyAsInt(60)&&mouseX<px.applyAsInt(bx)+size.applyAsInt(93)&&upgrade.level()<upgrade.maximum())(tankUpgradeStep(upgrade,true)==null?upgrade.increase():tankUpgradeStep(upgrade,true).changes()).forEach(stat->preview.put(stat.id(),stat.value()));
        }
        art(armor?"production/tank/panel":"production/design/"+branch+"_panel",x,y,w,h);
        if(graphics!=null) {
            graphics.pose().pushMatrix();graphics.pose().translate(x+w/2,py.applyAsInt(11)+2);graphics.pose().scale(1.7f);
            var caption=Tooltips.officerText(title,true);
            graphics.text(font,caption,-font.width(caption)/2,0,TEXT,false);graphics.pose().popMatrix();
        }
        int closeWidth=size.applyAsInt(32),closeHeight=size.applyAsInt(33);
        int closeX=px.applyAsInt(1048)-(navy?1:0),closeY=py.applyAsInt(6);
        var closeDesigner=button(title+" 창 닫기","",closeX,closeY,closeWidth,closeHeight,true,()->send(TANK_CLOSE,"","",0));
        if(closeDesigner!=null)closeDesigner.background("menu/close").sound("ui.close");
        if(tankSlot.isEmpty()) {
            if(graphics!=null)UiAssets.nineSlice(graphics,"production/tank/name",px.applyAsInt(24),py.applyAsInt(59),size.applyAsInt(305),size.applyAsInt(29),4,.5);
            art(armor?"production/tank/blueprint_icon":"production/design/controls/"+branch+"_name",px.applyAsInt(25),py.applyAsInt(61),size.applyAsInt(27),size.applyAsInt(26));
            if(graphics==null) {
                tankNameBox=new EditBox(font,px.applyAsInt(54),py.applyAsInt(64)+3,size.applyAsInt(270),size.applyAsInt(23),Component.literal(title+" 이름"));
                tankNameBox.setBordered(false);tankNameBox.setCentered(false);
                tankNameBox.setMaxLength(80);tankNameBox.setValue(tankName);tankNameBox.moveCursorToStart(false);tankNameBox.setResponder(value->tankName=value);addRenderableWidget(tankNameBox);
            }
            String chassis=Optional.ofNullable(equipmentBySource.get(d.chassis())).map(IndustryScreen::familyName).orElse(d.name());
            if(!armor)branchDesignContents(d,navy,px,py,size);
            else {
            recess(px.applyAsInt(24),py.applyAsInt(95),size.applyAsInt(305),size.applyAsInt(35));
            int roleHeight=size.applyAsInt(35),roleIcon=size.applyAsInt(29),roleY=py.applyAsInt(95);
            art("production/tank/role_modern",px.applyAsInt(29),roleY+(roleHeight-roleIcon)/2,roleIcon,roleIcon);
            leftText("현대식 전차",px.applyAsInt(68),roleY+Math.round((roleHeight-font.lineHeight*UiText.scale(font))/2),size.applyAsInt(242),TEXT);
            art(d.texture(),px.applyAsInt(340),py.applyAsInt(68),size.applyAsInt(154),size.applyAsInt(56));
            art("production/tank/blueprint_bg",px.applyAsInt(16),py.applyAsInt(196),size.applyAsInt(508),size.applyAsInt(248));
            art("production/tank/blueprint",px.applyAsInt(16),py.applyAsInt(196),size.applyAsInt(508),size.applyAsInt(248));
            var slotIds=List.of("main_armament_slot","turret_type_slot","suspension_type_slot","armor_type_slot","engine_type_slot",
                    "special_type_slot_1","special_type_slot_2","special_type_slot_3","special_type_slot_4","special_type_slot_5","special_type_slot_6","special_type_slot_7");
            int slotAreaX=px.applyAsInt(16),slotAreaWidth=size.applyAsInt(508),slotGap=size.applyAsInt(4),slotHeight=size.applyAsInt(46);
            int slotWidth=(slotAreaWidth-2*size.applyAsInt(3)-6*slotGap)/7;
            int slotLeft=slotAreaX+(slotAreaWidth-7*slotWidth-6*slotGap)/2;
            int topSlotY=py.applyAsInt(142)+(py.applyAsInt(196)-py.applyAsInt(142)-slotHeight)/2;
            int bottomSlotY=py.applyAsInt(444)+(py.applyAsInt(494)-py.applyAsInt(444)-slotHeight)/2;
            int[][] positions={{0,topSlotY},{1,topSlotY},{0,bottomSlotY},{1,bottomSlotY},{2,bottomSlotY},{2,topSlotY},{3,topSlotY},{4,topSlotY},{5,topSlotY},{3,bottomSlotY},{6,topSlotY},{0,py.applyAsInt(196)}};
            for(int i=0;i<slotIds.size();i++) {
                String slotId=slotIds.get(i);
                var slot=designSlots.get(slotId);
                int bx=slotLeft+positions[i][0]*(slotWidth+slotGap),by=positions[i][1],bw=slotWidth,bh=slotHeight;
                if(slot!=null) {
                    tankSlotButton(slot,bx,by,bw,bh);
                    if(mouseX>=bx&&mouseX<bx+bw&&mouseY>=by&&mouseY<by+bh&&!slot.selected().isEmpty()) {
                        String layer=switch(slotId) {case "main_armament_slot"->"gun";case "turret_type_slot"->"turret";case "suspension_type_slot"->"suspension";case "armor_type_slot"->"armor";case "engine_type_slot"->"engine";default->"";};
                        if(!layer.isEmpty())art("production/tank/"+layer,px.applyAsInt(16),py.applyAsInt(196),size.applyAsInt(508),size.applyAsInt(248));
                    }
                } else {
                    recess(bx,by,bw,bh);
                    if(graphics!=null)UiAssets.drawContent(graphics,"production/designer/locked",bx+(bw-size.applyAsInt(36))/2,by+(bh-size.applyAsInt(34))/2,size.applyAsInt(36),size.applyAsInt(34));
                    tip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_MODULE_SLOT_NOT_AVAILABLE"),bx,by,bw,bh);
                }
            }
            cellText(chassis,px.applyAsInt(31),py.applyAsInt(201)+4,size.applyAsInt(478),size.applyAsInt(22),TEXT,false);
            for(int i=0;i<d.upgrades().size();i++) {
                var u=d.upgrades().get(i);int bx=323+i*106;
                cellText(u.name(),px.applyAsInt(bx+1),py.applyAsInt(446),size.applyAsInt(90),size.applyAsInt(20),TEXT,false);
                int controlX=px.applyAsInt(bx),controlY=py.applyAsInt(460),controlWidth=size.applyAsInt(93),buttonSize=size.applyAsInt(33);
                cellText(Integer.toString(u.level()),controlX+buttonSize,controlY,controlWidth-2*buttonSize,buttonSize,TEXT,false);
                String requirements="§f"+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_CURRENT_MAX_LEVEL").replace("$LEVEL|H0$","§e"+u.maximum())+"\n"+String.join("\n",u.requirements());
                tankUpgradeButton(u,false,controlX,controlY,buttonSize);
                tankUpgradeButton(u,true,controlX+controlWidth-buttonSize,controlY,buttonSize);
                productionTip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_EACH_LEVEL_STAT_HEADER")+"\n"+tankUpgradeChanges(u,1)+"\n\n"+requirements,controlX+buttonSize,controlY,controlWidth-2*buttonSize,buttonSize);
            }
            }
            String upgradeTexture=navy?"production/design/controls/upgrade":"production/tank/refit";
            String manufacturerTexture=armor?"production/tank/manufacturer":"production/design/controls/"+branch+"_manufacturer";
            var team = d.designTeam();
            var refit=originalButton("최신 특성으로 개량",upgradeTexture,px.applyAsInt(navy?410:425),py.applyAsInt(navy?400:397),size.applyAsInt(navy?30:38),size.applyAsInt(navy?30:34),team != null && team.outdated(),()->send(TANK_DESIGN_TEAM_UPGRADE,"","",0),upgradeTexture+"_disabled");
            int teamX = px.applyAsInt(armor?461:navy?450:455), teamY = py.applyAsInt(382), teamSize = size.applyAsInt(63);
            UiPanelButton manufacturer = null;
            if (graphics == null) {
                manufacturer = new UiPanelButton(title+" · 군수산업체",teamX,teamY,teamSize,teamSize,()->{
                    if (team != null && !team.choices().isEmpty()) { designerManufacturer = true; manufacturerAnchorX = x; manufacturerAnchorY = y + 24; manufacturerScroll = 0; rebuildWidgets(); }
                }) {
                    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                        UiAssets.draw(g,manufacturerTexture+(team == null || team.choices().isEmpty() ? "_disabled" : ""),getX(),getY(),getWidth(),getHeight());
                        if (team != null && team.assigned() != null) UiAssets.drawContent(g,team.assigned().icon(),getX()+2,getY()+2,getWidth()-4,getHeight()-4);
                    }
                    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice) {
                        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && isMouseOver(event.x(),event.y())) {
                            if (active && team != null && team.assigned() != null) { playDownSound(minecraft.getSoundManager()); send(TANK_DESIGN_TEAM,"","",0); }
                            return true;
                        }
                        return super.mouseClicked(event,twice);
                    }
                };
                manufacturer.active = pending == 0 && team != null && (team.assigned() != null || !team.choices().isEmpty());
                manufacturer.setTooltip(Tooltips.createExact(team == null ? "" : team.assigned() == null ? team.tooltip() : team.assigned().tooltip()));
                addRenderableWidget(manufacturer);
            }
            if (team != null && team.outdated() && !designerManufacturer && mouseX >= px.applyAsInt(navy?410:425) && mouseX < teamX && mouseY >= py.applyAsInt(navy?400:397) && mouseY < py.applyAsInt(navy?400:397)+size.applyAsInt(navy?30:34)) team.upgradeChanges().forEach(stat -> preview.put(stat.id(),stat.value()));
            if(refit!=null&&manufacturer!=null)refit.setWidth(Math.min(refit.getWidth(),manufacturer.getX()-refit.getX()));
            if(refit!=null)refit.setTooltip(Tooltips.createExact(team == null ? "" : team.upgradeTooltip()));
        } else {
            int left=size.applyAsInt(508),footer=py.applyAsInt(496);
            recess(px.applyAsInt(16),py.applyAsInt(48),left,footer-py.applyAsInt(48));
            if(graphics!=null&&armor)graphics.fill(px.applyAsInt(18),py.applyAsInt(50),px.applyAsInt(522),footer-2,0xFF030803);
            var slot=Objects.requireNonNull(designSlots.get(tankSlot));
            String selectedTexture=slot.choices().stream().filter(part->part.id().equals(slot.selected())).map(IndustryTankDesigner.Part::texture).findFirst().orElse("production/designer/add");
            recess(px.applyAsInt(30),py.applyAsInt(61),size.applyAsInt(73),size.applyAsInt(45));
            float headerCenter=py.applyAsInt(61)+size.applyAsInt(45)/2f;
            int card=size.applyAsInt(241),gap=size.applyAsInt(6),rowHeight=size.applyAsInt(69),cardHeight=size.applyAsInt(63);
            int backHeight=size.applyAsInt(36),backWidth=size.applyAsInt(123);
            int backX=px.applyAsInt(navy?404:406);
            moduleBackButton(backX,py.applyAsInt(71),backWidth,backHeight);
            int removeSize=size.applyAsInt(27),removeX=backX-gap-removeSize;
            if(graphics!=null) {
                var label=Tooltips.officerText("모듈 선택",true);
                UiText.marquee(graphics,font,label,px.applyAsInt(112),headerCenter-font.lineHeight*1.5f/2,(navy?removeX:backX)-gap-px.applyAsInt(112),1.5f,TEXT,true,-1);
            }
            if(navy&&!slot.selected().isEmpty())originalButton("장착 모듈 제거","production/designer/remove",removeX,Math.round(headerCenter-removeSize/2f),removeSize,removeSize,true,()->{send(TANK_MODULE,slot.id(),"",0);tankSlot="";});
            var categories=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryIds(d.chassis(),slot.id()).stream()
                    .filter(category->slot.choices().stream().anyMatch(part->dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleCategory(part.id()).equals(category))).toList();
            boolean categorized=!armor?dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryIds(d.chassis(),slot.id()).size()>1:armor&&!Set.of("suspension_type_slot","armor_type_slot","engine_type_slot","special_type_slot_5","turret_type_slot").contains(slot.id());
            var parts=slot.choices().stream().filter(part->!categorized||dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleCategory(part.id()).equals(tankCategory)).toList();
            if(armor)parts=parts.stream().sorted(Comparator.comparing((IndustryTankDesigner.Part part)->switch(part.id()) {
                case "tank_heavy_cannon"->"tank_gun_0";case "tank_high_velocity_cannon"->"tank_gun_1";
                case "tank_high_velocity_cannon_2"->"tank_gun_2";case "tank_heavy_cannon_2"->"tank_gun_3";default->part.id();
            })).toList();
            boolean categoryView=categorized&&tankCategory.isEmpty();
            int rows=Math.max(1,(footer-py.applyAsInt(115))/rowHeight),total=categoryView?categories.size():parts.size();
            tankScroll=Math.clamp(tankScroll,0,Math.max(0,(total+1)/2-rows));
            for(int i=tankScroll*2;i<Math.min(total,(tankScroll+rows)*2);i++) {
                int bx=px.applyAsInt(30)+i%2*(card+gap),by=py.applyAsInt(115)+(i/2-tankScroll)*rowHeight;
                if(categoryView)tankCategoryButton(categories.get(i),bx,by,card,cardHeight);
                else {
                    tankPartButton(parts.get(i),slot,bx,by,card,cardHeight);
                    if(mouseX>=bx&&mouseX<bx+card&&mouseY>=by&&mouseY<by+cardHeight) {
                        if(armor)selectedTexture=parts.get(i).texture();
                        parts.get(i).changes().forEach(stat->preview.put(stat.id(),stat.value()));
                    }
                }
            }
            art(selectedTexture,px.applyAsInt(36),py.applyAsInt(65),size.applyAsInt(61),size.applyAsInt(37));
        }
        List<List<String>> groups=List.of(List.of("maximum_speed","reliability","supply_consumption"),List.of("soft_attack","hard_attack","ap_attack","hardness","armor_value","breakthrough","defense","air_attack"),List.of("fuel_capacity","fuel_consumption","suppression","recon","entrenchment"));
        if(!armor)groups=navy?List.of(List.of("naval_speed","naval_range","max_organisation","max_strength","reliability","supply_consumption","manpower"),
                List.of("lg_attack","lg_armor_piercing","hg_attack","hg_armor_piercing","torpedo_attack","sub_attack","armor_value","anti_air_attack","carrier_size"),
                List.of("fuel_consumption","surface_visibility","surface_detection","sub_visibility","sub_detection","mines_planting","mines_sweeping","naval_weather_penalty_factor","hit_profile")):
                List.of(List.of("maximum_speed","air_range","supply_consumption","weight","thrust"),
                List.of("air_defence","air_attack","air_agility","air_superiority","naval_strike_attack","naval_strike_targetting","air_ground_attack","air_bombing"),
                List.of("reliability","fuel_consumption","surface_detection","sub_detection","night_penalty","weather_penalty","mines_planting","mines_sweeping"));
        String[] headings={"기본 능력치","전투 능력치","기타 정보"};
        for(int col=0;col<3;col++) {
            int sx=543+col*175,column=size.applyAsInt(171);
            tankStatText(headings[col],px.applyAsInt(sx+3),py.applyAsInt(48),column-3,size.applyAsInt(24),false,1.35f);
            for(int row=0;row<groups.get(col).size();row++) {
                String key=groups.get(col).get(row);var stat=statsById.get(key);
                if(key.equals("carrier_size")) {
                    if(!d.chassis().contains("carrier"))continue;
                    if(stat==null)stat=new IndustryTankDesigner.Stat(key,dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statName(key),0,false);
                }
                int rowTop=py.applyAsInt(73+row*20),rowHeight=py.applyAsInt(93+row*20)-rowTop,by=rowTop+1;
                int valueWidth=size.applyAsInt(key.equals("maximum_speed")?82:57);
                String icon=armor?tankStatIcon(key):"production/design/stats/"+key;
                if(graphics!=null)UiAssets.drawAlignedContent(graphics,icon,icon,px.applyAsInt(sx+2),by+(rowHeight-size.applyAsInt(18))/2f,size.applyAsInt(19),size.applyAsInt(18));
                if(stat==null&&!armor) {
                    String label=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statName(key).replaceAll("£[^ ]+\\s*","").replace(":","").strip();
                    tankStatText(label,px.applyAsInt(sx+24),by,column-valueWidth-size.applyAsInt(25),rowHeight,false);
                    tankStatText("—",px.applyAsInt(sx)+column-valueWidth,by,valueWidth,rowHeight,true);
                    tip(label+": 현재 설계 데이터에 없는 수치입니다.",px.applyAsInt(sx),by,column,rowHeight);
                    continue;
                }
                if(stat==null)stat=new IndustryTankDesigner.Stat(key,dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statName(key),0,false);
                String label=stat.name().replaceAll("£[^ ]+\\s*","").replace(":","").strip();
                tankStatText(label,px.applyAsInt(sx+24),by,column-valueWidth-size.applyAsInt(25),rowHeight,false);
                double change=preview.getOrDefault(key,0.0);
                double indicatorChange = preview.containsKey(key) ? change : stat.change();
                var displayed=new IndustryTankDesigner.Stat(stat.id(),stat.name(),stat.value()+change,stat.percent());
                tankStatText(tankValue(displayed),px.applyAsInt(sx)+column-valueWidth,by,valueWidth-(indicatorChange==0?0:size.applyAsInt(10)),rowHeight,true);
                if(graphics!=null&&indicatorChange!=0) {
                    boolean beneficial=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.beneficial(key,indicatorChange);
                    UiText.cell(graphics,font,indicatorChange>0?"▲":"▼",px.applyAsInt(sx)+column-size.applyAsInt(10),by,size.applyAsInt(10),rowHeight,beneficial?0xFF009F03:0xFFFF3232,false);
                }
                productionTip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statTooltip(stat),px.applyAsInt(sx),by,column,rowHeight);
            }
        }
        int adjustmentTop=navy?331:260;
        if(graphics!=null) {
            float captionScale=Math.min(1.55f,size.applyAsInt(24)/(float)font.lineHeight);
            graphics.pose().pushMatrix();graphics.pose().translate(px.applyAsInt(562),py.applyAsInt(adjustmentTop));graphics.pose().scale(captionScale);
            graphics.text(font,Component.literal("조정치").withStyle(net.minecraft.ChatFormatting.BOLD),0,0,0xFFFFFFFF,false);graphics.pose().popMatrix();
        }
        int adjustmentY=adjustmentTop+28;
        if(armor||navy) {
            var environments=navy?List.of("water_fjords","water_shallow_sea","water_deep_ocean"):
                    List.of("plains","forest","hills","mountain","urban","jungle","marsh","desert","fort","river","amphibious");
            for(int i=0;i<environments.size();i++) {
                String id=environments.get(i),label=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.adjusterName(id);
                int columns=armor?Math.min(5,environments.size()-i/5*5):5;
                int rowWidth=columns*84+(columns-1)*16;
                int adjusterX=539+(536-rowWidth)/2+i%5*100;
                int adjusterY=armor?288+i/5*70:adjustmentY;
                designerAdjustment(id,label,adjusterX,adjusterY,px,n->py.applyAsInt(n)-2,size,false,()->{});
            }
        } else {
            int rows=Math.max(1,(d.missions().size()+4)/5),rowHeight=Math.min(76,(488-adjustmentY)/rows);
            java.util.function.IntUnaryOperator adjustmentSize=n->Math.max(1,size.applyAsInt(n)*Math.min(72,rowHeight)/72);
            for(int i=0;i<d.missions().size();i++) {
                var mission=d.missions().get(i);
                String tooltip=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.missionTooltip(d,mission.id());
                designerAdjustment(mission.id(),tooltip,562+i%5*100,adjustmentY+i/5*rowHeight,px,py,adjustmentSize,true,()->{designMission=designMission.equals(mission.id())?"":mission.id();rebuildWidgets();});
            }
            if(!designMission.isEmpty())tip("선택한 임무의 능력치입니다. 같은 임무를 다시 누르면 기본 능력치로 돌아갑니다.",px.applyAsInt(540),py.applyAsInt(47),size.applyAsInt(520),size.applyAsInt(202));
        }
        int footerHeight=size.applyAsInt(30),footerCenter=py.applyAsInt(502)+size.applyAsInt(34)/2,footerY=footerCenter-footerHeight/2;
        var reset=button("설계 초기화","초기화",px.applyAsInt(10),py.applyAsInt(502),size.applyAsInt(123),size.applyAsInt(34),true,()->send(TANK_RESET,"","",0));
        if(reset!=null)reset.background("production/design/division/reset").textShadow(false).sound("ui.designer."+(navy?"ship":"tank")+".reset");
        if(reset!=null)reset.setTooltip(Tooltips.createExact(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_RESET_DESC")));
        productionTip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("CURRENT_EXPERIENCE"),px.applyAsInt(286),footerY,size.applyAsInt(90),footerHeight);
        if(graphics!=null)UiText.iconText(graphics,font,"hud/"+branch+"_experience",experience==null?"—":integer(experience),px.applyAsInt(286),footerY,size.applyAsInt(90),footerHeight,size.applyAsInt(20),TEXT);
        if(graphics==null) {
            int bx=px.applyAsInt(400),by=py.applyAsInt(502),bw=size.applyAsInt(123),bh=size.applyAsInt(36);
            String label=d.errors().isEmpty()?"설계 저장 · "+(armor?"육군":navy?"해군":"공군")+" 경험치 "+d.experience():tankErrors(d);
            var save=new UiPanelButton(label,bx,by,bw,bh,()->send(TANK_SAVE,tankName,"",0)) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    UiAssets.draw(g,"production/design/division/duplicate",bx,by,bw,bh);
                    tankExperienceCaption(g,"저장",d.experience(),bx+bw/2,by+(bh-UiText.MIN_HEIGHT)/2);
                }
            };
            save.sound("ui.designer."+(navy?"ship":"tank")+".save");
            save.active=pending==0&&d.errors().isEmpty()&&experience!=null&&experience>=d.experience();
            String saveTip=dev.hoi.client.ui.tooltip.TooltipText.sections(
                    dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SAVE_DESC").replace("$XPICON$",branch+"_experience").replace("$XPCOST|Y0$","§e"+d.experience()+"§f"),
                    dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SAVE_DESC_NO_CHANGE_TO_PARENT"));
            if(!d.errors().isEmpty())saveTip=tankErrors(d);
            else if(experience==null||experience<d.experience())saveTip=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_NOT_ENOUGH_XP");
            save.setTooltip(Tooltips.createExact(saveTip));addRenderableWidget(save);
        }
        int resourceX=px.applyAsInt(540),resourceWidth=size.applyAsInt(97);
        if(graphics!=null)UiAssets.drawContent(graphics,"production/tank/resources",resourceX,footerY,resourceWidth,footerHeight);
        var designResources=List.of("OIL","ALUMINUM","RUBBER","TUNGSTEN","IRON","CHROMIUM","COAL").stream().filter(id->d.resources().getOrDefault(id,0.0)>0).toList();
        int resourceStep=Math.min(26,82/Math.max(1,designResources.size())),resourceIcon=size.applyAsInt(Math.min(22,resourceStep));
        int resourceLeft=resourceX+(resourceWidth-(Math.max(0,designResources.size()-1)*size.applyAsInt(resourceStep)+resourceIcon))/2;
        for(int i=0;i<designResources.size();i++)art(resourceArt(designResources.get(i)),resourceLeft+i*size.applyAsInt(resourceStep),footerCenter-resourceIcon/2,resourceIcon,resourceIcon);
        tip("이 설계를 생산하는 데 사용되는 §e자원",resourceX,footerY,resourceWidth,footerHeight);
        if(graphics!=null) {
            String label="생산 비용:",cost=ProtocolNumberBounds.fixed(d.cost()+preview.getOrDefault("build_cost_ic",0.0),navy?0:2);
            float textScale=UiText.scale(font);
            int labelWidth=Math.round(font.width(label)*textScale),costWidth=Math.round(font.width(cost)*textScale),iconSize=size.applyAsInt(17),gap=2;
            int costLeft=px.applyAsInt(1060)-labelWidth-iconSize-costWidth-2*gap,textY=footerCenter-Math.round(font.lineHeight*textScale/2);
            UiText.text(graphics,font,label,costLeft,textY,TEXT);
            UiAssets.draw(graphics,navy?"production/design/stats/naval_build_cost_ic":"keywords/stat_common_build_cost_ic",costLeft+labelWidth+gap,footerCenter-iconSize/2,iconSize,iconSize);
            UiText.text(graphics,font,cost,costLeft+labelWidth+iconSize+2*gap,textY,TEXT);
            var costStat=statsById.get("build_cost_ic");
            if(costStat!=null)productionTip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statTooltip(costStat),costLeft,footerY,px.applyAsInt(1060)-costLeft,footerHeight);
        }
        if(armor&&tankSlot.isEmpty())tankRoleMenu(px.applyAsInt(24),py.applyAsInt(95),size.applyAsInt(305),size.applyAsInt(35));
    }
    private void tankRoleMenu(int x,int y,int w,int h) {
        if(graphics!=null)return;
        var toggle=new UiPanelButton("전차 역할 선택",x,y,w,h,()->{tankRolesOpen=!tankRolesOpen;rebuildWidgets();}) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                int side=h-9;
                g.pose().pushMatrix();
                g.pose().translate(x+w-4-(h-6+side)/2f,y+(h-side)/2f-1);
                UiAssets.draw(g,"production/design/controls/role_expand",0,0,side,side);
                g.pose().popMatrix();
                if(tankRolesOpen)g.fill(x,y+h,x+w,y+4*(h+3),0xFF000000);
            }
        };
        toggle.sound("ui.checkbox");toggle.active=pending==0;addRenderableWidget(toggle);
        if(!tankRolesOpen)return;
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget!=toggle
                &&widget.getX()<x+w&&widget.getX()+widget.getWidth()>x&&widget.getY()<y+4*(h+3)&&widget.getY()+widget.getHeight()>y+h)widget.active=false;
        var roles=List.of("modern","anti_air","flame");
        for(int i=0;i<roles.size();i++) {
            String role=roles.get(i),key="tank_designer_"+(i==0?"modern_tank_chassis":role);
            String name=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip(key);
            int by=y+h+3+i*(h+3);boolean selected=i==0;
            var entry=new UiPanelButton(name,x,by,w,h,()->{tankRolesOpen=false;rebuildWidgets();}) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    g.fill(x,by,x+w,by+h,0xFF080B08);
                    UiAssets.nineSlice(g,"production/design/controls/module_card",x,by,w,h,12,w/305.0);
                    int icon=h-6,indicator=h/3;
                    UiAssets.draw(g,"production/design/roles/tank_"+role,x+3,by+(h-icon)/2,icon,icon);
                    UiText.text(g,font,name,x+h,by+Math.round((h-font.lineHeight*UiText.scale(font))/2),selected?0xFFFFBD00:0xFFFF3232);
                    if(!selected)UiAssets.draw(g,"production/design/controls/role_forbidden",x+w-indicator-4,by+(h-indicator)/2,indicator,indicator);
                }
            };
            entry.active=selected&&pending==0;
            entry.setTooltip(Tooltips.createExact(selected?"§e"+name:dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip(key+"_role_disallowed")+"\n\n§7현재 전차 설계에서는 이 역할로 저장할 수 없습니다."));
            addRenderableWidget(entry);
        }
    }
    private void branchDesignContents(IndustryTankDesigner d,boolean navy,java.util.function.IntUnaryOperator px,java.util.function.IntUnaryOperator py,java.util.function.IntUnaryOperator size) {
        String branch=navy?"navy":"air";
        var equipment=equipmentBySource.get(d.chassis());
        String chassis=equipment==null?d.name():equipment.familyName();
        if(!navy) {
            recess(px.applyAsInt(24),py.applyAsInt(95),size.applyAsInt(305),size.applyAsInt(35));
            String role=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.role(d),label=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.roleName(role);
            int rx=px.applyAsInt(24),ry=py.applyAsInt(95),rw=size.applyAsInt(305),rh=size.applyAsInt(35);
            if(graphics==null) {
                var roleButton=new UiPanelButton("항공기 역할 · 주무장 선택",rx,ry,rw,rh,()->{tankSlot="fixed_main_weapon_slot";tankScroll=0;rebuildWidgets();}) {
                    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                        UiMenuStyle.recess(g,rx,ry,rw,rh);
                        UiAssets.draw(g,"production/design/roles/"+role,rx+size.applyAsInt(5),ry+size.applyAsInt(4),size.applyAsInt(32),rh-size.applyAsInt(8));
                        var lines=Tooltips.split(font,Component.literal(label),Math.max(1,(int)((rw-size.applyAsInt(46))/UiText.scale(font))));
                        int lineHeight=UiText.MIN_HEIGHT;
                        for(int i=0;i<Math.min(2,lines.size());i++)UiText.text(g,font,lines.get(i),rx+size.applyAsInt(43),ry+(rh-Math.min(2,lines.size())*lineHeight)/2+i*lineHeight,TEXT);
                    }
                };
                roleButton.active=pending==0&&d.slots().stream().anyMatch(slot->slot.id().equals("fixed_main_weapon_slot"));
                roleButton.setTooltip(Tooltips.createExact(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.roleTooltip(role)));addRenderableWidget(roleButton);
            }
        }
        if(graphics!=null) {
            int imageX=px.applyAsInt(353),imageBottom=navy?py.applyAsInt(95)+size.applyAsInt(35):py.applyAsInt(124),imageWidth=size.applyAsInt(154),imageHeight=size.applyAsInt(60);
            String national=d.texture()+"_"+view.country().toLowerCase(Locale.ROOT);
            if(navy) {
                if(!UiAssets.drawBottom(graphics,national,imageX,imageBottom-imageHeight,imageWidth,imageHeight))UiAssets.drawBottom(graphics,d.texture(),imageX,imageBottom-imageHeight,imageWidth,imageHeight);
            } else if(!UiAssets.draw(graphics,national,imageX,imageBottom-imageHeight,imageWidth,imageHeight))UiAssets.draw(graphics,d.texture(),imageX,imageBottom-imageHeight,imageWidth,imageHeight);
        }
        if(navy&&(!d.family().isEmpty()||equipment!=null)) {
            String family=d.family().isEmpty()?equipment.family():d.family();
            String category=family.equals("submarine")?"submarine":Set.of("destroyer","light_cruiser").contains(family)?"screen":"capital";
            int cx=px.applyAsInt(481),cw=size.applyAsInt(26),ch=size.applyAsInt(23);
            int cy=py.applyAsInt(59)+(size.applyAsInt(29)-ch)/2;
            art("production/design/controls/ship_category_"+category,cx,cy,cw,ch);
            productionTip(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.shipCategoryTooltip(category),cx,cy,cw,ch);
        }
        int bx=px.applyAsInt(16),by=py.applyAsInt(196),bw=size.applyAsInt(508),bh=size.applyAsInt(248);
        art("production/design/"+branch+"_blueprint_bg",bx,by,bw,bh);
        if(graphics!=null) {
            String texture="production/design/blueprints/"+d.chassis();
            if(!UiAssets.draw(graphics,texture+"_"+view.country().toLowerCase(Locale.ROOT),bx,by,bw,bh))UiAssets.draw(graphics,texture,bx,by,bw,bh);
        }
        cellText(chassis,px.applyAsInt(31),py.applyAsInt(201)+4,size.applyAsInt(478),size.applyAsInt(22),TEXT,false);
        List<String> upper=navy?d.slots().stream().map(IndustryTankDesigner.Slot::id).filter(id->id.endsWith("_custom_slot")).toList():
                List.of("fixed_main_weapon_slot","fixed_auxiliary_weapon_slot_1","fixed_auxiliary_weapon_slot_2","fixed_auxiliary_weapon_slot_3","fixed_auxiliary_weapon_slot_4","fixed_auxiliary_weapon_slot_5");
        List<String> lower=navy?d.slots().stream().map(IndustryTankDesigner.Slot::id).filter(id->!id.endsWith("_custom_slot")).toList():
                List.of("engine_type_slot","special_type_slot_1","special_type_slot_2","special_type_slot_3","special_type_slot_4","special_type_slot_5","special_type_slot_6");
        var topSlots=new ArrayList<>(upper);var bottom=new ArrayList<>(lower);
        while(topSlots.size()<(navy?7:6))topSlots.add("locked_"+topSlots.size());
        while(bottom.size()<7)bottom.add("locked_"+bottom.size());
        upper=topSlots;
        String hovered="";
        for(int row=0;row<2;row++) {
            var ids=row==0?upper:bottom;
            int gap=size.applyAsInt(3),sh=size.applyAsInt(46),sw=(bw-2*gap-6*gap)/7;
            int left=bx+(bw-7*sw-6*gap)/2,sy=py.applyAsInt(row==0?146:447);
            for(int i=0;i<ids.size();i++) {
                String id=ids.get(i);int sx=left+i*(sw+gap);
                var slot=designSlots.get(id);
                if(slot!=null) {
                    tankSlotButton(slot,sx,sy,sw,sh);
                    if(mouseX>=sx&&mouseX<sx+sw&&mouseY>=sy&&mouseY<sy+sh)hovered=id;
                }
                else {
                    recess(sx,sy,sw,sh);
                    if(graphics!=null)UiAssets.drawContent(graphics,"production/designer/locked",sx+(sw-size.applyAsInt(36))/2,sy+(sh-size.applyAsInt(34))/2,size.applyAsInt(36),size.applyAsInt(34));
                    tip("이 슬롯에는 이 장비 유형을 사용할 수 없습니다.",sx,sy,sw,sh);
                }
            }
        }
        if(graphics!=null)dev.hoi.client.ui.industry.IndustryEquipmentDesigns.overlays(graphics,d,hovered,bx,by,bw,bh);
        if(!navy) {
            art("production/design/controls/silhouette_bg",px.applyAsInt(451),py.applyAsInt(146),size.applyAsInt(69),size.applyAsInt(46));
        }
    }
    private void designerAdjustment(String id,String label,int x,int y,java.util.function.IntUnaryOperator px,java.util.function.IntUnaryOperator py,java.util.function.IntUnaryOperator size,boolean enabled,Runnable action) {
        int bx=px.applyAsInt(x),by=py.applyAsInt(y),bw=size.applyAsInt(84),bh=size.applyAsInt(67);
        if(graphics!=null)return;
        int centering=3;
        int frameW=size.applyAsInt(64),frameH=size.applyAsInt(57);
        double frameScale=Math.min(frameW/70.0,frameH/62.0);
        int fitW=(int)Math.round(70*frameScale),fitH=(int)Math.round(62*frameScale);
        int frameX=bx+size.applyAsInt(7+centering)+(frameW-fitW)/2,frameY=by+size.applyAsInt(5)+(frameH-fitH)/2;
        var adjustment=view.tankDesigner().adjustments().stream().filter(a->a.id().equals(id)).findFirst().orElse(null);
        double[] values=adjustment==null?null:new double[]{adjustment.movement(),adjustment.attack(),adjustment.defense()};
        boolean navy=designBranch().equals("navy");
        String[] icons=navy?new String[]{"ship_movement","attack_defense","other"}:new String[]{"movement","attack","defense"};
        double[] indicators=values==null?null:values.clone();
        String[] hints=values==null?null:new String[]{designerAdjustmentLine(0,values[0],false),designerAdjustmentLine(1,values[1],false),designerAdjustmentLine(2,values[2],false)};
        if(navy&&values!=null) {
            indicators[1]=Math.min(values[1],values[2])<0?Math.min(values[1],values[2]):Math.max(values[1],values[2]);
            indicators[2]=adjustment.effects().stream().anyMatch(stat->!dev.hoi.client.ui.industry.IndustryEquipmentDesigns.adjusterBeneficial(stat))?-1:adjustment.effects().isEmpty()?0:1;
            hints[1]=java.util.stream.IntStream.range(1,3).filter(index->values[index]!=0).mapToObj(index->designerAdjustmentLine(index,values[index],false)).collect(java.util.stream.Collectors.joining("\n"));
            hints[2]=adjustment.effects().stream().map(dev.hoi.client.ui.industry.IndustryEquipmentDesigns::adjusterEffect).collect(java.util.stream.Collectors.joining("\n"));
        }
        var button=new UiPanelButton("설계 조정치 · "+id,bx,by,bw,bh,action) {
            @Override protected void handleCursor(GuiGraphicsExtractor g) { if(enabled)super.handleCursor(g); }
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiAssets.draw(g,"production/design/adjusters/"+id,bx+size.applyAsInt(19+centering),by+size.applyAsInt(10),size.applyAsInt(40),size.applyAsInt(40));
                UiAssets.draw(g,"production/design/controls/adjuster_frame",bx+size.applyAsInt(7+centering),by+size.applyAsInt(5),size.applyAsInt(64),size.applyAsInt(57));
                String hovered="";
                if(values!=null)for(int j=0;j<3;j++) {
                    String path="production/design/controls/"+icons[j],state=indicators[j]>1e-8?"positive":indicators[j]<-1e-8?"negative":"neutral";
                    float iconSize=21/2f;
                    float inset=(size.applyAsInt(15)-iconSize)/2f;
                    float iconX=j<2?frameX+fitW*(j==0?12.5f:35f)/70f-iconSize/2f:bx+size.applyAsInt(52+centering)+inset;
                    if(j==2)iconX-=1f/minecraft.getWindow().getGuiScale();
                    float iconY=j<2?frameY+fitH*49.5f/62f-iconSize/2f:by+size.applyAsInt(42)+inset+1;
                    UiAssets.drawAlignedContent(g,path+"_"+state,path+"_neutral",iconX,iconY,iconSize,iconSize);
                    if(!designBranch().equals("air")&&mx>=iconX&&mx<iconX+iconSize&&my>=iconY&&my<iconY+iconSize)hovered=hints[j];
                }
                if(isHovered())setTooltip(Tooltips.createExact(hovered.isEmpty()?designerAdjustmentTooltip(label,values,adjustment):hovered));
            }
        };
        button.active=enabled&&pending==0;button.setTooltip(Tooltips.createExact(designerAdjustmentTooltip(label,values,adjustment)));addRenderableWidget(button);
    }
    private String designerAdjustmentLine(int index,double value,boolean total) {
        String key=switch(index){case 0->"MOVEMENT";case 1->"ATTACK";default->"DEFENCE";};
        String token=switch(index){case 0->"movement_texticon";case 1->"attack_texticon";default->"defence_texticon";};
        String amount=String.format(java.util.Locale.ROOT,"%.1f%%",(total?1+value:value)*100);
        return "§f£"+token+" "+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("STAT_ADJUSTER_"+key)+" "+(total?"§e":value<0?"§c":"§a")+(!total&&value>0?"+":"")+amount;
    }
    private String designerAdjustmentTooltip(String label,double[] values,IndustryTankDesigner.Adjustment adjustment) {
        if(values==null||designBranch().equals("air"))return label;
        var lines=new ArrayList<String>();lines.add("§e"+label);
        if(designBranch().equals("navy")) {
            for(int i=0;i<3;i++)if(values[i]!=0)lines.add(designerAdjustmentLine(i,values[i],false));
            for(var stat:adjustment.effects())lines.add(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.adjusterEffect(stat));
            return String.join("\n",lines);
        }
        for(int i=0;i<3;i++)lines.add(designerAdjustmentLine(i,values[i],true)+"\n§f"+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("BASE")+" §e"+String.format(java.util.Locale.ROOT,"%.1f%%",(1+values[i])*100));
        return String.join("\n\n",lines);
    }
    private void tankExperienceCaption(GuiGraphicsExtractor g,String label,int amount,int centerX,int y) {
        var cost=dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal("§f£"+designBranch()+"_experience §e"+amount),false),false);
        String prefix=label+" ";float scale=UiText.scale(font);
        int left=centerX-Math.round((font.width(prefix)+font.width(cost))*scale/2);
        UiText.text(g,font,prefix,left,y,TEXT);
        UiText.text(g,font,cost,left+Math.round(font.width(prefix)*scale),y,TEXT);
    }
    private void tankStatText(String value,int x,int y,int width,int height,boolean right) {
        tankStatText(value,x,y,width,height,right,1);
    }
    private void tankStatText(String value,int x,int y,int width,int height,boolean right,float multiplier) {
        if(graphics==null)return;
        var caption=Component.literal(value);
        if(!right)caption.withStyle(net.minecraft.ChatFormatting.BOLD);
        float scale=Math.min(Math.min(1.1f,Math.max(.8f,this.width/1100f))*multiplier,(float)Math.max(1,height-2)/font.lineHeight);
        UiText.marquee(graphics,font,caption,x,y+(height-(font.lineHeight-(right?2:0))*scale)/2,width,scale,0xFF13170E,false,right?1:-1);
    }
    private void tankSlotButton(IndustryTankDesigner.Slot slot,int x,int y,int w,int h) {
        String texture=slot.choices().stream().filter(p->p.id().equals(slot.selected())).map(IndustryTankDesigner.Part::texture).findFirst().orElse("production/designer/add");
        String label=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_NAME_TOOLTIP").replace("$NAME$",slot.name())+"\n";
        var selected=slot.choices().stream().filter(p->p.id().equals(slot.selected())).findFirst();
        label+=selected.isPresent()?dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_MODULE_TOOLTIP").replace("$NAME$",selected.get().name()):dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_EMPTY_TOOLTIP");
        String description=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleDescription(slot.selected());
        if(!description.isEmpty())label+="\n"+description;
        String effects=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleEffects(slot.selected());
        if(!effects.isEmpty())label+="\n\n"+effects;
        String resources=moduleResourceTooltip(slot.selected());
        if(!resources.isEmpty())label+="\n\n"+resources;
        label+="\n\n"+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_ALLOWED_CATEGORIES_TOOLTIP")+"\n§f"+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.slotCategories(view.tankDesigner().chassis(),slot.id());
        if(slot.required())label+="\n\n"+(slot.selected().isEmpty()?"§c":"§f")+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_REQUIRED_TOOLTIP");
        if(graphics!=null)return;
        String accessible=slot.name()+(slot.required()?" · 필수":"")+selected.map(part->"\n"+part.name()).orElse("");
        var button=new UiPanelButton(accessible,x,y,w,h,()->{tankSlot=slot.id();tankCategory="";tankScroll=0;rebuildWidgets();}) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiMenuStyle.recess(g,x,y,w,h);
                int iconWidth=Math.max(1,w*(slot.selected().isEmpty()?24:56)/69);
                int iconHeight=Math.max(1,h*(slot.selected().isEmpty()?24:42)/46);
                UiAssets.drawContent(g,texture,x+(w-iconWidth)/2,y+(h-iconHeight)/2,iconWidth,iconHeight);
            }
        };
        button.active=pending==0;
        button.setTooltip(Tooltips.createExact(label));addRenderableWidget(button);
    }
    private void moduleBackButton(int x,int y,int w,int h) {
        if(graphics!=null)return;
        var button=new UiPanelButton("모듈 선택 뒤로",x,y,w,h,()->{
            if(!tankCategory.isEmpty())tankCategory="";else tankSlot="";
            tankScroll=0;rebuildWidgets();
        }) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiAssets.draw(g,"production/design/controls/module_back",x,y,w,h);
                UiText.centeredSingleLine(g,font,Component.literal("뒤로"),x,y,w,h,active?TEXT:UiMenuStyle.MUTED);
            }
        };
        button.sound("ui.close");button.active=pending==0;addRenderableWidget(button);
    }
    private void tankCategoryButton(String category,int x,int y,int w,int h) {
        if(graphics!=null)return;
        String label=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryName(category);
        var button=new UiPanelButton("모듈 분류: "+label,x,y,w,h,()->{tankCategory=category;tankScroll=0;rebuildWidgets();}) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                moduleCard(g,dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryTexture(category),label,x,y,w,h,false);
            }
        };
        button.active=pending==0;button.setTooltip(Tooltips.createExact(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryTooltip(category)));addRenderableWidget(button);
    }
    private void moduleCard(GuiGraphicsExtractor g,String texture,String label,int x,int y,int w,int h,boolean cost) {
        UiAssets.nineSlice(g,"production/design/controls/module_card",x,y,w,h,12,w/244.0);
        int iconWidth=w*72/244,iconHeight=h*44/66,textX=x+w*85/244,textWidth=w*153/244;
        UiAssets.drawContent(g,texture,x+w*6/244,y+(h-iconHeight)/2,iconWidth,iconHeight);
        var lines=Tooltips.split(font,Component.literal(label),Math.max(1,(int)(textWidth/UiText.scale(font))));
        int count=Math.min(2,lines.size()),areaHeight=cost?h*38/66:h;
        for(int i=0;i<count;i++)UiText.text(g,font,lines.get(i),textX,y+(areaHeight-count*UiText.MIN_HEIGHT)/2+i*UiText.MIN_HEIGHT,TEXT);
    }
    private String moduleResourceTooltip(String id) {
        var resources=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleResources(id);
        if(resources.isEmpty())return "";
        return "§e"+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("PRODUCTION_RESOURCES_PER_FACTORY")+"\n"+resources.entrySet().stream().map(entry->"  §f"+resourceName(entry.getKey())+": §e"+integer(entry.getValue())).collect(java.util.stream.Collectors.joining("\n"));
    }
    private String tankModuleTooltip(IndustryTankDesigner.Part part) {
        String label="§e"+part.name(),description=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleDescription(part.id());
        if(!description.isEmpty())label+="\n\n§f"+description;
        String effects=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleEffects(part.id());
        if(!effects.isEmpty())label+="\n\n"+effects;
        String resources=moduleResourceTooltip(part.id());
        if(!resources.isEmpty())label+="\n\n"+resources;
        return label+"\n\n§f£"+designBranch()+"_experience "+dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_"+designBranch().toUpperCase(Locale.ROOT)+"_XP_COST").replace("$COST|Y0$","§e"+(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleExperience(part.id())>0?dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleExperience(part.id()):part.experience()));
    }
    private void tankPartButton(IndustryTankDesigner.Part part,IndustryTankDesigner.Slot slot,int x,int y,int w,int h) {
        if(graphics!=null)return;
        boolean selected=part.id().equals(slot.selected());
        var button=new UiPanelButton(part.name(),x,y,w,h,()->{send(TANK_MODULE,slot.id(),part.id(),0);tankSlot="";tankCategory="";}) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                moduleCard(g,part.texture(),part.name(),x,y,w,h,!selected);
                if(!selected)tankExperienceCaption(g,"교체 비용",part.experience(),x+w*85/244+w*153/488,y+h*41/66);
                if(designBranch().equals("army")&&slot.id().equals("main_armament_slot"))UiAssets.draw(g,"production/design/controls/module_valid",x+w*5/244,y+h*5/66,Math.max(8,w*18/244),Math.max(8,h*18/66));
            }
        };
        button.active=pending==0;
        button.setTooltip(Tooltips.createExact(tankModuleTooltip(part)));addRenderableWidget(button);
    }
    private void pickerRightText(GuiGraphicsExtractor g,String value,int x,int y,int width) {
        UiText.marquee(g,font,Tooltips.officerText(value),x,y,width,UiText.scale(font),TEXT,true,1);
    }
    private void pickerScrollbar(int x,int top,int bottom,int visible,int total,int step) {
        productionPickerScrollbar=new UiScrollBar(x,top,bottom,visible,total);
        productionPickerStep=step;
        if(graphics!=null&&productionPickerScrollbar.maximum()>0)productionPickerScrollbar.draw(graphics,pickerScroll);
    }
    private static String navalSilhouette(String type) {
        return switch(type) {
            case "carrier","submarine","destroyer","light_cruiser","heavy_cruiser","battleship","convoy" -> "production/picker/filter_"+type;
            case "battlecruiser" -> "production/picker/filter_battle_cruiser";
            default -> "production/picker/filter_unknown";
        };
    }
    static String repairState(NavalRepair ship) {
        return switch(ship.status()) {
            case "항구 수용량 대기", "조선소 배정 대기" -> ship.hp()<ship.maxHp()?"수리 대기":"정박";
            default -> ship.status();
        };
    }
    static String repairTime(NavalRepair ship) {
        if(!repairState(ship).equals("수리 중")||ship.remainingHours()==null)return "—";
        long hours=ship.remainingHours();
        return hours>=24?(hours/24+(hours%24==0?0:1))+"일":hours+"시간";
    }
    private static int repairAssignmentStep() {
        if(dev.hoi.client.ui.UiPriorityControls.shifted())return 10;
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL)
                ||com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RCONTROL)?5:1;
    }
    private void layoutRepairPorts(int x,int y,int w,int end) {
        var repairs=view.navalRepairs();
        if(repairs==null){text("수리 현황을 확인할 수 없습니다.",x+9,y+36,w-18,MUTED);return;}
        art("production/summary/dockyard_icon_with_wrench",x+8,y+28,16,16);
        var decrease=originalButton("수리 조선소 배정 감소","recruitment/decrease",x+26,y+29,13,13,repairs.assignedDockyards()>0,
                ()->send(REPAIR_DOCKYARDS,"","",Math.max(0,repairs.assignedDockyards()-repairAssignmentStep())));
        cellText(Integer.toString(repairs.assignedDockyards()),x+39,y+28,22,16,TEXT,false);
        var increase=originalButton("수리 조선소 배정 증가","recruitment/increase",x+61,y+29,13,13,repairs.assignedDockyards()<repairs.maximumDockyards(),
                ()->send(REPAIR_DOCKYARDS,"","",Math.min(repairs.maximumDockyards(),repairs.assignedDockyards()+repairAssignmentStep())));
        if(decrease!=null)decrease.setTooltip(Tooltips.create(Component.literal("§a클릭§r하여 §e1§r 감소\n§aCtrl + 클릭§r하여 §e5§r 감소\n§aShift + 클릭§r하여 §e10§r 감소")));
        if(increase!=null)increase.setTooltip(Tooltips.create(Component.literal("§a클릭§r하여 §e1§r 증가\n§aCtrl + 클릭§r하여 §e5§r 증가\n§aShift + 클릭§r하여 §e10§r 증가")));
        tip("함선 수리에 배정할 수 있는 §e조선소§r의 최대 숫자입니다.",x+39,y+27,22,17);
        var represented=new HashSet<String>();repairs.ports().forEach(port->represented.addAll(port.ships()));
        var expand=originalButton("수리 목록 펼치기/접기","production/picker/"+(allRepairPortsExpanded?"expand":"collapse"),x+81,y+28,38,15,true,()->{
            allRepairPortsExpanded = !allRepairPortsExpanded;
            if(allRepairPortsExpanded) {
                repairs.ports().forEach(port->expandedRepairPorts.add(port.id()));
                repairs.ships().stream().filter(ship->!represented.contains(ship.id()))
                        .forEach(ship->expandedRepairPorts.add(ship.id()));
            } else expandedRepairPorts.clear();
            pickerScroll=0;rebuildWidgets();
        });
        if(expand!=null)expand.setTooltip(Tooltips.create(Component.literal("§a클릭§r하여 모든 대기열 펴기/접기")));
        var ports=new ArrayList<>(repairs.ports());
        for(var ship:repairs.ships())if(!represented.contains(ship.id())) {
            ports.add(new RepairPort(ship.id(),ship.port(),0,0,List.of(ship.id())));
        }
        int start=y+50,row=Math.max(17,Math.round((w-18)*35f/400)),shipHeight=Math.max(20,Math.round((w-27)*41f/401)),shipRow=shipHeight+2,total=ports.size()*row;
        for(var port:ports)if(expandedRepairPorts.contains(port.id()))total+=port.ships().size()*shipRow;
        pickerScroll=Math.clamp(pickerScroll,0,Math.max(0,total-(end-start-4)));
        if(graphics!=null)UiAssets.nineSlice(graphics,"production/picker/repair_list",x+3,start-2,w-6,end-start,16,w/444.0);
        int by=start-pickerScroll;
        for(var port:ports) {
            var portShips=port.ships().stream().map(repairShipsById::get).filter(Objects::nonNull).toList();
            long waiting=portShips.stream().filter(ship->repairState(ship).equals("수리 대기")).count();
            int py=by;by+=row;
            if(py>=start&&py+row<=end-3) {
                if(graphics==null) {
                    var listTip=Tooltips.create(Component.literal("§a클릭§r하여 함선 대기열 목록을 전환합니다."));
                    var policyTip=Tooltips.create(Component.literal((port.disabled()?"§c수리하지 않음":port.priority()==0?"§c수리 우선순위 - 낮음":port.priority()==1?"§e수리 우선순위 - 중간":"§a수리 우선순위 - 높음")+"§r\n\n우선순위가 더 높은 해군기지는 수리를 위한 조선소를 먼저 할당받습니다.\n\n§a클릭§r하여 우선순위를 바꾸십시오. §a우클릭§r하여 조선소의 수리 여부를 활성화하거나 비활성화하십시오."));
                    var shipsTip=Tooltips.create(Component.literal("이 해군기지에서 함선 §e"+waiting+"§r척이 수리를 위해 대기 중입니다."));
                    var capacityTip=Tooltips.create(Component.literal("§e"+waiting+"§r척의 함정이 수리 대기 중입니다.\n현재 수리 중: §e"+port.activeShips()+"§r척\n이 §e해군기지§r는 동시에 최대 §e"+(port.capacity()==0?"—":port.capacity())+"§r척의 수리 작업을 수행할 수 있습니다."));
                    var button=new UiPanelButton("수리 항구: "+port.name(),x+7,py,w-18,Math.round((w-18)*33f/400),()->{if(!expandedRepairPorts.add(port.id()))expandedRepairPorts.remove(port.id());rebuildWidgets();}) {
                        @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice) {
                            int column=(int)((event.x()-getX())*400/Math.max(1,getWidth()));
                            if(isMouseOver(event.x(),event.y())&&column>=35&&column<60&&(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT||event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT)) {
                                if(pending==0&&port.capacity()>0) {
                                    playDownSound(minecraft.getSoundManager());
                                    if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT)send(REPAIR_ENABLED,port.id(),"",port.disabled()?1:0);
                                    else send(REPAIR_PRIORITY,port.id(),"",(port.priority()+1)%3);
                                }
                                return true;
                            }
                            return super.mouseClicked(event,twice);
                        }
                        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                            UiAssets.draw(g,"production/picker/"+(expandedRepairPorts.contains(port.id())?"repair_port":"repair_port_collapsed"),getX(),py,getWidth(),getHeight());
                            int column=(mx-getX())*400/Math.max(1,getWidth());
                            setTooltip(column>=35&&column<60?policyTip:column>=200&&column<270?shipsTip:column>=270?capacityTip:listTip);
                            UiAssets.draw(g,"production/picker/repair_state"+(port.disabled()?"_disabled":port.priority()==0?"_low":port.priority()==2?"_high":""),getX()+Math.round(getWidth()*35f/400),py+2,Math.round(getWidth()*22f/400),getHeight()-4);
                            UiText.marquee(g,font,Tooltips.officerText(port.name()),getX()+Math.round(getWidth()*60f/400),py+4,getWidth()*140f/400,UiText.scale(font),TEXT,true,-1);
                            UiAssets.draw(g,"production/picker/ships",getX()+getWidth()/2,py+3,15,10);
                            IndustryProductionRender.text(g,font,Long.toString(waiting),getX()+getWidth()*59/100,py,w*9/100,row-2,TEXT);
                            UiAssets.draw(g,"production/summary/dockyard_icon",getX()+getWidth()*68/100,py+2,12,12);
                            IndustryProductionRender.text(g,font,port.capacity()==0?"—":port.activeShips()+" / "+port.capacity(),getX()+getWidth()*75/100,py,w*18/100,row-2,TEXT);
                        }
                    };
                    button.setTooltip(listTip);addRenderableWidget(button);
                }
            }
            if(expandedRepairPorts.contains(port.id()))for(String id:port.ships()) {
                var ship=repairShipsById.get(id);int sy=by;by+=shipRow;
                if(ship==null||sy<start||sy+shipRow>end-3)continue;
                int sx=x+15,sw=w-27,label=sx+sw*132/401,labelWidth=sw*130/401;
                art("production/picker/repair_ship",sx,sy,sw,shipHeight);
                String family=navalFamilyByTexture.getOrDefault(ship.texture(),ship.texture().substring(ship.texture().lastIndexOf('/')+1));
                art(navalSilhouette(family),sx+Math.round(sw*24f/401),sy+Math.round(sw*8f/401),Math.round(sw*112f/401),Math.round(sw*18f/401));
                leftText(ship.name(),label,sy+2,labelWidth,TEXT);
                leftText(repairState(ship),label,sy+shipHeight-9,labelWidth,repairState(ship).equals("수리 중")?GOOD:MUTED);
                int gaugeHeight=Math.round(sw*28f/401),gaugeWidth=Math.max(2,Math.round(sw*6f/401));
                int gaugeY=sy+Math.round(sw*2f/401),orgX=sx+Math.round(sw*6f/401),hpX=sx+Math.round(sw*14f/401);
                repairStat("repair_org",orgX,gaugeY,gaugeWidth,gaugeHeight,ship.organization()==null?0:ship.organization()/ship.maxOrganization());
                repairStat("repair_hp",hpX,gaugeY,gaugeWidth,gaugeHeight,ship.hp()/ship.maxHp());
                int progressX=sx+sw*270/401,progressWidth=Math.round(sw*90f/401),progressHeight=Math.round(sw*17f/401),progressY=sy+Math.round(sw*10f/401);
                repairProgress(repairState(ship),progressX,progressY,progressWidth,progressHeight,ship.hp()/ship.maxHp());
                cellText(repairTime(ship),progressX,progressY,progressWidth,progressHeight,TEXT,false);
                String organization=ship.organization()==null?"—":decimal(ship.organization())+" / "+decimal(ship.maxOrganization());
                if(mouseX>=orgX&&mouseX<orgX+gaugeWidth)tip("조직력: "+organization,orgX,gaugeY,gaugeWidth,gaugeHeight);
                else if(mouseX>=hpX&&mouseX<hpX+gaugeWidth)tip("내구도: "+decimal(ship.hp())+" / "+decimal(ship.maxHp()),hpX,gaugeY,gaugeWidth,gaugeHeight);
                else tip(ship.name()+"\n"+ship.status()+"\n조직력 "+organization+"\n내구도 "+decimal(ship.hp())+" / "+decimal(ship.maxHp())
                        +"\n수리 완료 예상: "+repairTime(ship)+"\n현재 수리 배정이 유지될 때의 예상 시간입니다.",sx,sy,sw,shipHeight);
            }
        }
        pickerScrollbar(x+w-11,y+Math.round(w*90f/444),end-4,end-start-4,total,row);
    }
    private void repairStat(String name,int x,int y,int w,int h,double value) {
        if(graphics==null)return;
        float ratio=(float)Math.clamp(value,0,1);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x,y+h);
        graphics.pose().rotate(-(float)Math.PI/2);
        UiAssets.draw(graphics,"production/picker/repair_stat_bg",0,0,h,w);
        int filled=Math.round(h*ratio);
        if(filled>0)graphics.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/picker/"+name+".png"),0,0,filled,w,0,ratio,0,1);
        graphics.pose().popMatrix();
    }
    private void repairProgress(String state,int x,int y,int w,int h,double value) {
        if(graphics==null)return;
        float ratio=(float)Math.clamp(value,0,1);
        String name=switch(state) {case "수리 대기"->"repair_progress_waiting";case "귀항 중"->"repair_progress_returning";default->"repair_progress";};
        var texture=net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/picker/"+name+".png");
        UiAssets.draw(graphics,"production/picker/repair_progress_bg",x,y,w,h);
        int filled=Math.round(w*ratio);
        if(filled>0) {
            graphics.blit(texture,x,y,x+filled,y+h,0,ratio,1f/3,2f/3);
            graphics.blit(texture,x,y,x+filled,y+h,0,ratio,0,1f/3);
            graphics.blit(texture,x,y,x+filled,y+h,1-ratio,1,2f/3,1);
        }
    }
    private void chooseEquipment(String id) {
        if (switchLine.isEmpty() && equipment(id).manufacturerChoices() > 0) {
            manufacturerEquipment = id; manufacturerScroll = 0; send(OFFER, id, "", 0); return;
        }
        if (switchLine.isEmpty()) send(ADD, id, "", 0); else send(SWITCH, switchLine, id, 0);
        picker = "";
    }
    private void layoutContract() {
        var p=view.partners().stream().filter(v->v.id().equals(partner)).findFirst().orElse(null);
        if(p==null){partner="";return;}
        int current=view.trades().stream().filter(v->v.importing()&&v.partner().equals(partner)&&v.resource().equals(resource)).mapToInt(Trade::factories).sum();
        double perFactory=p.resourcesPerFactory()==null?8:p.resourcesPerFactory(),export=p.exports().getOrDefault(resource,0.0);
        int limit=Math.max(0,Math.min(view.economy().freeCivilian()+current,Math.max(current,(int)Math.ceil(export/perFactory))));
        tradeAmount=Math.clamp(tradeAmount,0,limit);
        double maxImport=Math.min(export,limit*perFactory);
        long needed=p.convoys()==null||p.convoys()<0?-1:(long)p.convoys()*tradeAmount;
        int w=Math.min(316,Math.min(width-16,(height-hudBottom()-8)*527/437)),h=w*437/527;
        int x=(width-w)/2,y=Math.max(hudBottom()+4,(height-h)/2);
        java.util.function.IntUnaryOperator size=n->Math.max(1,n*w/527);
        java.util.function.IntUnaryOperator px=n->x+n*w/527,py=n->y+n*w/527;
        art("trade/offer",x,y,w,h);
        int titleX=px.applyAsInt(105),titleY=py.applyAsInt(22);
        int titleWidth=px.applyAsInt(424)-titleX,titleHeight=py.applyAsInt(64)-titleY;
        cellText(p.name(),titleX,titleY,titleWidth,titleHeight,TEXT,false);
        int flagX=px.applyAsInt(425),frameWidth=size.applyAsInt(78),frameHeight=size.applyAsInt(49);
        int flagWidth=size.applyAsInt(70),flagHeight=size.applyAsInt(44);
        art(HudMenuBar.flagTexture(p.id()),flagX+(frameWidth-flagWidth)/2,titleY+(titleHeight-flagHeight)/2,flagWidth,flagHeight);
        art("trade/flag_frame",flagX,titleY+(titleHeight-frameHeight)/2,frameWidth,frameHeight);
        art(resourceArt(resource),px.applyAsInt(30),py.applyAsInt(110),size.applyAsInt(22),size.applyAsInt(22));
        var description=Component.literal(p.name()).withColor(GOLD).append(Component.literal(" 측은 ").withColor(TEXT))
                .append(Component.literal(resourceName(resource)).withColor(GOLD)).append(Component.literal("(을)를 ").withColor(TEXT))
                .append(Component.literal(integer(export)).withColor(GOLD)).append(Component.literal("까지 수출할 수 있습니다.\n우리는 ").withColor(TEXT))
                .append(Component.literal(integer(Math.min(export,limit*perFactory))).withColor(GOLD)).append(Component.literal("까지만 수입할 수 있습니다.").withColor(TEXT));
        paragraph(description,px.applyAsInt(69),py.applyAsInt(90),size.applyAsInt(215),size.applyAsInt(105));
        art("hud/convoys",px.applyAsInt(30),py.applyAsInt(211),size.applyAsInt(22),size.applyAsInt(22));
        var convoy=Component.literal("우리에게는 사용 가능한 수송선 ").withColor(TEXT)
                .append(Component.literal(p.freeConvoys()==null?"—":Long.toString(p.freeConvoys())).withColor(GOLD))
                .append(Component.literal("척이 있으며, 이 무역에는 ").withColor(TEXT))
                .append(Component.literal(needed<0?"—":Long.toString(needed)).withColor(needed>0&&p.freeConvoys()!=null&&needed>p.freeConvoys()?BAD:GOOD))
                .append(Component.literal("척이 필요합니다.").withColor(TEXT));
        paragraph(convoy,px.applyAsInt(69),py.applyAsInt(200),size.applyAsInt(215),size.applyAsInt(59));
        recess(px.applyAsInt(294),py.applyAsInt(80),size.applyAsInt(210),size.applyAsInt(176));
        leftText("무역 상대국",px.applyAsInt(309),py.applyAsInt(90),size.applyAsInt(185),TEXT);
        var buyers=p.buyers().stream().filter(v->v.resource().equals(resource)).toList();
        for(int i=0;i<Math.min(16,buyers.size());i++) {
            var buyer=buyers.get(i);int bx=px.applyAsInt(304+i%4*46),by=py.applyAsInt(112+i/4*31);
            art(HudMenuBar.flagTexture(buyer.id()),bx,by,size.applyAsInt(40),size.applyAsInt(27));
            tip(buyer.name(),bx,by,size.applyAsInt(40),size.applyAsInt(27));
        }
        art("construction/summary/civilian_factory",px.applyAsInt(70),py.applyAsInt(275)+1,size.applyAsInt(26),size.applyAsInt(22));
        paragraph(Component.literal("수입량: ").withColor(TEXT).append(Component.literal(integer(Math.min(export,tradeAmount*perFactory))).withColor(GOLD))
                .append(Component.literal("\n할당될 민간공장: ").withColor(TEXT)).append(Component.literal(Integer.toString(tradeAmount)).withColor(GOLD)),
                px.applyAsInt(104),py.applyAsInt(265)+1,size.applyAsInt(235),size.applyAsInt(40));
        var neededButton=originalButton("필요한 수입량 설정","trade/need",px.applyAsInt(350),py.applyAsInt(262)+1,size.applyAsInt(38),size.applyAsInt(38),p.route(),()->{
            double delivered=view.trades().stream().filter(v->v.importing()&&v.partner().equals(partner)&&v.resource().equals(resource)).mapToDouble(Trade::delivered).sum();
            tradeAmount=view.resources().stream().filter(v->v.id().equals(resource)).mapToInt(v->IndustryTradeAmounts.neededTradeFactories(v.demand(),v.available(),delivered,perFactory,limit)).findFirst().orElse(0);
            rebuildWidgets();
        });
        if(neededButton!=null)neededButton.setTooltip(Tooltips.create(Component.literal("현재 자원 수요를 충족하기 위해 구매해야 할 양을 설정하십시오.")));
        int sliderWidth=size.applyAsInt(404),sliderHeight=Math.max(1,Math.round(sliderWidth*42f/404));
        int sliderX=x+(w-sliderWidth)/2,sliderY=py.applyAsInt(307);
        java.util.function.IntUnaryOperator sx=n->sliderX+Math.round(n*sliderWidth/404f),sy=n->sliderY+Math.round(n*sliderHeight/42f);
        art("trade/slider",sliderX,sliderY,sliderWidth,sliderHeight);
        int numberHeight=sy.applyAsInt(34)-sy.applyAsInt(8),centerY=sliderY+sliderHeight/2;
        int arrowWidth=sx.applyAsInt(67)-sx.applyAsInt(53),arrowHeight=sy.applyAsInt(29)-sy.applyAsInt(13);
        tradeValue(Component.literal(integer(Math.min(export,tradeAmount*perFactory))),sx.applyAsInt(7),centerY-numberHeight/2,sx.applyAsInt(47)-sx.applyAsInt(7),numberHeight,TEXT);
        tradeValue(Component.literal(integer(maxImport)),sx.applyAsInt(357),centerY-numberHeight/2,sx.applyAsInt(397)-sx.applyAsInt(357),numberHeight,TEXT);
        originalButton("무역 공장 줄이기","trade/left",(sx.applyAsInt(51)+sx.applyAsInt(71)-arrowWidth)/2,centerY-arrowHeight/2,arrowWidth,arrowHeight,tradeAmount>0,()->{tradeAmount=Math.max(0,tradeAmount-1);rebuildWidgets();});
        originalButton("무역 공장 늘리기","trade/right",(sx.applyAsInt(333)+sx.applyAsInt(353)-arrowWidth)/2,centerY-arrowHeight/2,arrowWidth,arrowHeight,tradeAmount<limit,()->{tradeAmount=Math.min(limit,tradeAmount+1);rebuildWidgets();});
        if(graphics==null) {
            var slider=new net.minecraft.client.gui.components.AbstractSliderButton(sx.applyAsInt(71),sy.applyAsInt(8),sx.applyAsInt(333)-sx.applyAsInt(71),sy.applyAsInt(34)-sy.applyAsInt(8),Component.literal("수입량 조절"),IndustryTradeAmounts.tradeSliderPosition(tradeAmount,maxImport,perFactory)) {
                @Override protected void updateMessage(){setMessage(Component.literal("수입량 조절"));}
                @Override protected void applyValue(){
                    tradeAmount=Math.clamp(IndustryTradeAmounts.tradeSliderFactories(value,maxImport,perFactory),0,limit);
                    value=IndustryTradeAmounts.tradeSliderPosition(tradeAmount,maxImport,perFactory);
                    for(var child:children())if(child instanceof net.minecraft.client.gui.components.Button button) {
                        boolean enabled=switch(button.getMessage().getString()) {
                            case "무역 공장 줄이기" -> tradeAmount>0;
                            case "무역 공장 늘리기" -> tradeAmount<limit;
                            case "수입 계약 체결" -> tradeAmount==0?current>0:p.route();
                            default -> button.active;
                        };
                        button.active=enabled&&pending==0;
                    }
                }
                @Override public boolean keyPressed(KeyEvent event) {
                    if(active&&(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_LEFT||event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT)) {
                        value=IndustryTradeAmounts.tradeSliderPosition(Math.clamp(tradeAmount+(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT?1:-1),0,limit),maxImport,perFactory);
                        applyValue();return true;
                    }
                    return super.keyPressed(event);
                }
                @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    g.fill(getX(),getY()+getHeight()/2-1,getRight(),getY()+getHeight()/2+1,0xFF101010);
                    UiAssets.draw(g,"trade/knob",getX()+(int)((getWidth()-8)*value),getY(),8,getHeight());
                }
            };
            slider.active=!background&&pending==0&&limit>0;addRenderableWidget(slider);
        }
        cellText("(슬라이더를 0에 두면 거래 취소)",x,py.applyAsInt(348),w,size.applyAsInt(18),MUTED,false);
        tradeButton("계약 창 닫기","닫기",px.applyAsInt(70),py.applyAsInt(382),size.applyAsInt(123),size.applyAsInt(34),true,()->{partner="";rebuildWidgets();});
        tradeButton("수입 계약 체결","보내기",px.applyAsInt(350),py.applyAsInt(382),size.applyAsInt(123),size.applyAsInt(34),tradeAmount==0?current>0:p.route(),()->{
            String target=partner;partner="";send(TRADE,target,resource,tradeAmount);
        });
    }
    private void tradeButton(String accessible,String caption,int x,int y,int width,int height,boolean enabled,Runnable action) {
        if(graphics!=null)return;
        var button=new UiMenuButton(accessible,x,y,width,height,action).caption(caption).background("completion/button");
        button.active=enabled&&pending==0;addRenderableWidget(button);
    }
    private void paragraph(Component value,int x,int y,int width,int height) {
        if(graphics==null)return;
        var lines=Tooltips.split(font,value,Math.max(1,(int)(width/UiText.scale(font))));
        int count=Math.min(lines.size(),height/(UiText.MIN_HEIGHT+1));
        for(int i=0;i<count;i++)UiText.text(graphics,font,lines.get(i),x,y+i*(UiText.MIN_HEIGHT+1),TEXT);
        if(count<lines.size())tip(value.getString(),x,y,width,height);
    }
    private void send(IndustryPayloads.Action action, String item, String other, int amount) {
        if (closed || pending > 0 && action != CLOSE) return;
        if (foreign() && action != FOREIGN_LOGISTICS && action != REFRESH && action != CLOSE) return;
        boolean passive = action == REFRESH || action == ECONOMY;
        if (!passive) draggedProduction = "";
        if (!passive) pending = 100;
        transport.accept(new IndustryPayloads.Request(action, token, view == null ? 0 : view.revision(), item, other, amount));
        if (!passive) rebuildWidgets();
    }
    @Override public void tick() {
        economySubscription();
        if (pending == 0 && view != null && view.draft() != null && openingDesigner) {
            InputSidebarMovement.release(minecraft); designer = true; openingDesigner = false; draftName = view.draft().name(); rebuildWidgets();
        }
        if (pending > 0 && --pending == 0) {
            send(view == null ? foreign() ? FOREIGN_LOGISTICS : OPEN : REFRESH, view == null ? foreignTarget : "", "", 0);
        } else if (++refresh % 60 == 0 && pending == 0) send(view == null ? foreign() ? FOREIGN_LOGISTICS : OPEN : REFRESH, view == null ? foreignTarget : "", "", 0);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (designerManufacturer || picker.equals("manufacturer") || !manufacturerEquipment.isEmpty()) { manufacturerScroll -= (int)(vertical * IndustryOrganizationList.ENTRY_HEIGHT); rebuildWidgets(); return true; }
        if (designer && !divisionPicker.isEmpty()) {
            divisionPickerScroll -= (int) vertical; rebuildWidgets(); return true;
        }
        if (y < hudBottom() && view != null) { hudScroll = HudMenuBar.scroll(width, view.hud(), hudScroll, horizontal, vertical); return true; }
        if (insideAutoImport(x, y)) return true;
        if (tankDesigner()) tankScroll -= (int) vertical;
        else if (designer && selectedSlot >= 0) pickerScroll -= (int) vertical;
        else if (designer) {
            boolean overStats = false;
            for (int i = 0; i < divisionStatBars.length; i++) {
                var bar = divisionStatBars[i];
                if (bar != null && x >= bar.x() - 78 && x < bar.x() + 9 && y >= bar.top() && y < bar.bottom()) {
                    divisionStatScroll[i] = Math.clamp(divisionStatScroll[i] - (int) vertical, 0, bar.maximum());
                    overStats = true;
                }
            }
            if (!overStats) designerScroll -= (int) (vertical * 25);
        }
        else if (tab == MenuTab.PRODUCTION && (!modal() || dockedProductionPicker()) && x < pane) {
            if (productionScrollbar != null && y >= productionScrollbar.top() && y < productionScrollbar.bottom()) scroll -= (int)(vertical*20);
        }
        else if (productionPickerScrollbar != null && !picker.isEmpty() && !picker.equals("manufacturer")) {
            if (x >= productionPickerScrollbar.x() - productionPickerWidth() && x < productionPickerScrollbar.x() + 9
                    && y >= productionPickerScrollbar.top() && y < productionPickerScrollbar.bottom()) pickerScroll -= (int)(vertical*20);
        }
        else if (!picker.isEmpty()) pickerScroll -= (int) (vertical * 55);
        else if (modal()) return super.mouseScrolled(x, y, horizontal, vertical);
        else if (tab == MenuTab.TRADE && tradeTab == 1) { if (listScrollbar != null && x < pane && y >= listScrollbar.top() && y < listScrollbar.bottom()) scroll -= (int) (vertical * frame.size(TRADE_ROW)); }
        else if (tab == MenuTab.LOGISTICS) {
            if (!logisticsInfo.isEmpty() && x >= frame.x(FUEL_WINDOW) && x < frame.x(FUEL_WINDOW + 395) && y >= frame.y(290)) variantScroll -= (int) (vertical * frame.size(VARIANT_ROW));
            else if (listScrollbar != null && x < pane && y >= listScrollbar.top() && y < listScrollbar.bottom()) scroll -= (int) (vertical * frame.size(LOGISTICS_ROW));
        }
        else if (tab == MenuTab.RECRUITMENT && x >= pane) secondScroll -= (int) (vertical * 40);
        else scroll -= (int) (vertical * (tab == MenuTab.PRODUCTION ? frame.len(111) : tab == MenuTab.RECRUITMENT ? (compact() ? 85 : 93) : tab == MenuTab.LOGISTICS ? 68 : tradeTab == 0 ? 35 : 34));
        rebuildWidgets(); return true;
    }
    @Override public void removed() {
        if (confirming) return;
        flagAssets.close();
        economyPanel.close();
        draggedProduction = "";
        InputSidebarMovement.release(minecraft);
        if (DialogCoordinator.suspending()) return;
        closed = true; selectingDeployment = false; dev.hoi.client.map.scene.SceneRenderer.clearDeployment();
        transport.accept(new IndustryPayloads.Request(CLOSE, token, view == null ? 0 : view.revision(), "", "", 0));
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){
        if (!draggedProduction.isEmpty()) {
            productionPointerY=event.y();
            productionMoved |= Math.hypot(event.x() - productionDragX, event.y() - productionDragY) >= 4;
            if (productionMoved && productionScrollbar != null && event.x() >= 6 && event.x() < pane - 16) {
                if (event.y() < productionScrollbar.top() + 12) scroll -= 12;
                else if (event.y() > productionScrollbar.bottom() - 12) scroll += 12;
                rebuildWidgets();
            }
            return true;
        }
        if (!draggedImport.isEmpty()) { importPointerY = event.y(); importMoved |= Math.abs(event.y() - importStartY) >= 2; rebuildWidgets(); return true; }
        if (autoImportMoving) {
            float baseX = frame.x(768), baseY = frame.y(-3);
            autoImportX = Math.clamp(event.x() - autoImportGrabX - baseX, -baseX, Math.max(-baseX, width - frame.size(295) - baseX));
            autoImportY = Math.clamp(event.y() - autoImportGrabY - baseY, hudBottom() - baseY, Math.max(hudBottom() - baseY, height - frame.size(40) - baseY));
            rebuildWidgets(); return true;
        }
        if (listDragging && listScrollbar != null) { scroll = listScrollbar.offset(event.y()); rebuildWidgets(); return true; }
        if(divisionPickerDragging&&divisionPickerBar!=null){divisionPickerScroll=divisionPickerBar.offset(event.y());rebuildWidgets();return true;}if(productionDragging&&productionScrollbar!=null){scroll=productionScrollbar.offset(event.y());rebuildWidgets();return true;}if(productionPickerDragging&&productionPickerScrollbar!=null){pickerScroll=productionPickerScrollbar.offset(event.y());rebuildWidgets();return true;}if(manufacturerDragging){manufacturerScroll=manufacturerScrollbar.offset(event.y());rebuildWidgets();return true;}return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){
        if (!draggedProduction.isEmpty()) {
            String id = draggedProduction; draggedProduction = ""; rebuildWidgets();
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && productionMoved && pending == 0 && !modal() && event.x() >= 6 && event.x() < pane - 16
                    && productionScrollbar != null && event.y() >= productionScrollbar.top() && event.y() < productionScrollbar.bottom()) {
                int target=productionTarget(event.y());
                if(target>=0&&!view.lines().get(target).id().equals(id)){dev.hoi.client.audio.AudioUiSounds.play("ui.click");send(MOVE,id,"",target);}
            }
            return true;
        }
        if (!draggedImport.isEmpty()) {
            String id = draggedImport; boolean moved = importMoved; int target = moved && view != null && view.autoImport() != null ? importTarget() : -1;
            draggedImport = ""; importMoved = false;
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && moved && pending == 0 && target >= 0 && target != view.autoImport().order().indexOf(id)) {
                dev.hoi.client.audio.AudioUiSounds.play("ui.click"); send(AUTO_IMPORT_ORDER, id, "", target); return true;
            }
            rebuildWidgets(); return true;
        }
        if (autoImportMoving) { autoImportMoving = false; return true; }
        if (listDragging) { listDragging = false; return true; }
        if(divisionPickerDragging){divisionPickerDragging=false;return true;}if(productionDragging||productionPickerDragging){productionDragging=false;productionPickerDragging=false;return true;}if(manufacturerDragging){manufacturerDragging=false;return true;}return super.mouseReleased(event);
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (!draggedProduction.isEmpty()) { draggedProduction = ""; rebuildWidgets(); return true; }
        if (!draggedImport.isEmpty()) { draggedImport = ""; importMoved = false; rebuildWidgets(); return true; }
        if (insideAutoImport(event.x(), event.y())) {
            if (super.mouseClicked(event, twice)) return true;
            if (event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return true;
            var at = autoImportFrame(); double shift = autoImportShift(at), sx = (event.x() - at.x(0)) / at.scale(), sy = (event.y() - at.y(0)) / at.scale() - IMPORT_LIST_TOP - shift;
            var order = view.autoImport().order(); int index = (int) Math.floor(sy / IMPORT_ENTRY);
            if (pending == 0 && sx >= 12 && sx < 272 && sy >= 0 && index < order.size()) {
                draggedImport = order.get(index); importGrabY = event.y() - at.y(IMPORT_LIST_TOP + shift + IMPORT_ENTRY * index); importPointerY = importStartY = event.y(); importMoved = false;
                return true;
            }
            autoImportMoving = true; autoImportGrabX = event.x() - at.x(0); autoImportGrabY = event.y() - at.y(0);
            return true;
        }
        if (tab == MenuTab.PRODUCTION && !modal() && view != null && pending == 0 && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            float scale = (pane - 22) / 511f;
            if (event.x() >= 6 + Math.round(48 * scale) && event.x() < 6 + Math.round(193 * scale)) {
                for (var row : productionLines) if (event.y() >= row.y() && event.y() < row.y() + Math.round(24 * scale)) {
                    draggedProduction = row.line().id(); productionDragX = event.x(); productionDragY = productionPointerY = event.y(); productionOffsetY=event.y()-row.y(); productionMoved = false;
                    return true;
                }
            }
        }
        if (designerManufacturer || !manufacturerEquipment.isEmpty() || picker.equals("manufacturer")) {
            if (event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || pending > 0) return true;
            if (manufacturerScrollbar != null && manufacturerScrollbar.contains(event.x(), event.y())) {
                if (manufacturerScrollbar.arrow(event.y())) manufacturerScroll = manufacturerScrollbar.step(event.y(), manufacturerScroll, IndustryOrganizationList.ENTRY_HEIGHT);
                else { manufacturerDragging = true; manufacturerScroll = manufacturerScrollbar.offset(event.y()); }
                rebuildWidgets(); return true;
            }
            for (var widget : List.copyOf(manufacturerWidgets)) if (widget.isMouseOver(event.x(), event.y())) { widget.mouseClicked(event, twice); return true; }
            if (event.x() < manufacturerX || event.x() >= manufacturerX + manufacturerWidth || event.y() < manufacturerY || event.y() >= manufacturerBottom) closeManufacturers();
            return true;
        }
        if (tab == MenuTab.PRODUCTION && !modal() && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT
                && event.x() >= 6 && event.x() < pane - 16) {
            for (var row : productionLines) if (event.y() >= row.y() && event.y() < row.y() + row.height()) {
                if (pending == 0 && row.line().manufacturer() != null) {
                    dev.hoi.client.audio.AudioUiSounds.play("ui.click");
                    send(MANUFACTURER, row.line().id(), "", 0);
                }
                return true;
            }
        }
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            if(tab==MenuTab.PRODUCTION&&(!modal()||dockedProductionPicker())&&productionScrollbar!=null&&productionScrollbar.maximum()>0&&productionScrollbar.contains(event.x(),event.y())) {
                if(productionScrollbar.arrow(event.y()))scroll=productionScrollbar.step(event.y(),scroll,28);
                else {productionDragging=true;scroll=productionScrollbar.offset(event.y());}
                rebuildWidgets();return true;
            }
            if((tab==MenuTab.TRADE&&tradeTab==1||tab==MenuTab.LOGISTICS)&&!modal()&&listScrollbar!=null&&listScrollbar.maximum()>0&&listScrollbar.contains(event.x(),event.y())) {
                if(listScrollbar.arrow(event.y()))scroll=listScrollbar.step(event.y(),scroll,frame.len(tab==MenuTab.TRADE?TRADE_ROW:LOGISTICS_ROW));
                else {listDragging=true;scroll=listScrollbar.offset(event.y());}
                rebuildWidgets();return true;
            }
            if(productionPickerScrollbar!=null&&productionPickerScrollbar.maximum()>0&&productionPickerScrollbar.contains(event.x(),event.y())) {
                if(productionPickerScrollbar.arrow(event.y()))pickerScroll=productionPickerScrollbar.step(event.y(),pickerScroll,productionPickerStep);
                else {productionPickerDragging=true;pickerScroll=productionPickerScrollbar.offset(event.y());}
                rebuildWidgets();return true;
            }
        }
        if (designer && !divisionPicker.isEmpty()) {
            if (event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return true;
            if (divisionPickerBar != null && divisionPickerBar.maximum() > 0 && divisionPickerBar.contains(event.x(), event.y())) {
                if (divisionPickerBar.arrow(event.y())) divisionPickerScroll = divisionPickerBar.step(event.y(), divisionPickerScroll, 1);
                else { divisionPickerDragging = true; divisionPickerScroll = divisionPickerBar.offset(event.y()); }
                rebuildWidgets(); return true;
            }
            for (var option : List.copyOf(divisionOptions)) if (option.isMouseOver(event.x(), event.y())) { option.mouseClicked(event, twice); return true; }
            divisionPicker = ""; rebuildWidgets(); return true;
        }
        if (!modal() && (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) && view != null && pending == 0) {
            for (var slot : deploymentLocations) if (event.x() >= slot.x() && event.x() < slot.x() + slot.width() && event.y() >= slot.y() && event.y() < slot.y() + slot.height()) {
                locationRecruit = slot.recruit();
                selectingDeployment = event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
                dev.hoi.client.map.scene.SceneRenderer.clearDeployment();
                send(selectingDeployment ? SELECT_LOCATION : LOCATION, slot.recruit(), "", 0);
                return true;
            }
            if (selectingDeployment && event.y() > hudBottom() && minecraft.player != null) {
                if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) {selectingDeployment = false; dev.hoi.client.map.scene.SceneRenderer.clearDeployment(); rebuildWidgets(); return true;}
                if (!minecraft.options.getCameraType().isFirstPerson()) return true;
                var matrix = minecraft.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
                var ray = matrix.transformProject(new org.joml.Vector3f((float)(event.x() / width * 2 - 1), (float)(1 - event.y() / height * 2), 1)).normalize();
                selectingDeployment = false; dev.hoi.client.map.scene.SceneRenderer.clearDeployment();
                pending = 100;
                transport.accept(new IndustryPayloads.Request(LOCATION_AT, token, view.revision(), locationRecruit, "", 0, ray.x, ray.y, ray.z));
                rebuildWidgets();return true;
            }
        }
        return super.mouseClicked(event, twice);
    }
    @Override public int unitHudLeft(){return tab==MenuTab.RECRUITMENT?pane+3+rightWidth():pane;}
    @Override public boolean allowsMovement() { return !modal(); }
    @Override public boolean keyPressed(KeyEvent e) {
        if (!draggedProduction.isEmpty() && e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { draggedProduction = ""; rebuildWidgets(); return true; }
        if (autoImportOpen() && e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { autoImportWindow = false; draggedImport = ""; rebuildWidgets(); return true; }
        if ((fuelWindow || !logisticsInfo.isEmpty()) && tab == MenuTab.LOGISTICS && e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { fuelWindow = false; logisticsInfo = ""; rebuildWidgets(); return true; }
        if ((designerManufacturer || !manufacturerEquipment.isEmpty() || picker.equals("manufacturer")) && e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { closeManufacturers(); return true; }
        if (designer && !divisionPicker.isEmpty() && e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            divisionPicker = ""; rebuildWidgets(); return true;
        }
        return !modal() && InputSidebarMovement.consumes(minecraft, e) || super.keyPressed(e);
    }
    @Override public boolean keyReleased(KeyEvent e) { return !modal() && InputSidebarMovement.consumes(minecraft, e) || super.keyReleased(e); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private String equipmentLabel(String id) {var e=equipmentById.get(id);return e==null?id:familyName(e);}
    private Equipment equipment(String id) { return Objects.requireNonNull(equipmentById.get(id)); }
    private int factoryStep() {
        if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT)) return 10;
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RCONTROL) ? 5 : 1;
    }
    private static String familyName(Equipment e) { return e.familyName() == null ? e.name() : e.familyName(); }
    private Template template(String id) { return Objects.requireNonNull(templatesById.get(id)); }
    private String locationName(String id) { if (id.isEmpty()) return "장소 미지정"; return view.locations().stream().filter(l -> l.id().equals(id)).map(Choice::name).map(n -> n.replace("수도 · ", "").split(" · ", 2)[0]).findFirst().orElse(id); }
    private String resourceName(String id) { return view.resources().stream().filter(r -> r.id().equals(id)).map(Resource::name).findFirst().orElse(id); }
    private String unitName(String id, boolean support) { return (support ? view.companies() : view.battalions()).stream().filter(c -> c.id().equals(id)).map(Choice::name).map(n -> n.replace("수도 · ", "").split(" · ", 2)[0]).findFirst().orElse(id); }
    private String unitTexture(String id, boolean support) { return (support ? view.companies() : view.battalions()).stream().filter(c -> c.id().equals(id)).map(Choice::texture).findFirst().orElse("menu/recruitment"); }
    private String equipmentList(Map<String,Long> need, Map<String,Long> current) {
        return need.entrySet().stream().map(e -> equipmentLabel(e.getKey()) + ": " + current.getOrDefault(e.getKey(), 0L) + " / " + e.getValue()).reduce((a, b) -> a + "\n" + b).orElse("필요 장비 없음");
    }
    static double fillRatio(Map<String,Long> need, Map<String,Long> current) {
        double required = 0;
        double supplied = 0;
        for (var entry : need.entrySet()) {
            if (entry.getValue() <= 0) continue;
            required += entry.getValue();
            supplied += Math.clamp(current.getOrDefault(entry.getKey(), 0L), 0L, entry.getValue());
        }
        return required == 0 ? 1 : supplied / required;
    }
    private static String resourceArt(String id) { return "production/resources/" + id.toLowerCase(Locale.ROOT); }
    private static String ic(double n) { return java.math.BigDecimal.valueOf(n).setScale(0, java.math.RoundingMode.DOWN).toPlainString(); }
    private static String integer(double n) { return ProtocolNumberBounds.fixed(n == 0 ? 0 : n, 0); }
    private static String decimal(double n) { return String.format(Locale.ROOT, "%,.1f", n); }
    private static String percent(double n) { return integer(n * 100) + "%"; }
    private static String manpower(double n) { return HudMenuBar.number(n); }
    private int clampScroll(int value, int count, int h, int start) { return Math.clamp(value / h, 0, Math.max(0, count - Math.max(1, (bottom() - start) / h))) * h; }
    private boolean visible(int y, int h, int start) { return y >= start && y + h <= bottom(); }
    private void rail(int x, int y, int w, int h) { if (graphics != null) UiMenuStyle.metal(graphics, x, y, w, h); }
    private void recess(int x, int y, int w, int h) { if (graphics != null) UiMenuStyle.recess(graphics, x, y, w, h); }
    private void panel(int x, int y, int w, int h) { if (graphics != null) UiMenuStyle.panel(graphics, x, y, w, h); }
    private void art(String path, int x, int y, int w, int h) { if (graphics != null) UiAssets.draw(graphics, path, x, y, w, h); }
    private void cellText(String value, int x, int y, int width, int height, int color, boolean right) {
        if (graphics != null) UiText.cell(graphics, font, value, x, y, width, height, color, right);
    }
    private void iconText(String icon, String value, int x, int y, int width, int height, int size, int color) {
        if (graphics != null) UiText.iconText(graphics, font, icon, value, x, y, width, height, size, color);
    }
    private void heading(String value, int x, int y, int max, int color) { if (graphics != null) UiMenuStyle.heading(graphics, font, value, x, y, max, color); }
    private void leftText(String value, int x, int y, int max, int color) {
        if (graphics == null) return;
        var source=Tooltips.officerText(value);
        UiText.marquee(graphics,font,source,x,y,max,UiText.scale(font),color,true,-1);
        if(font.width(source)*UiText.scale(font)>max)tip(value,x,y-1,max,11);
    }
    private void text(String value, int x, int y, int max, int color) {
        if (graphics == null) return;
        float scale=dev.hoi.client.ui.UiText.scale(font);
        UiText.cell(graphics, font, value, x, y, max, UiText.MIN_HEIGHT, color, false);
        if(font.width(Tooltips.officerText(value))*scale>max)tip(value,x,y-1,max,11);
    }
    private void productionTip(String value,int x,int y,int w,int h) {
        if(graphics!=null&&!background&&!designerManufacturer&&mouseX>=x&&mouseX<x+w&&mouseY>=y&&mouseY<y+h&&!insideAutoImport(mouseX,mouseY))
            Tooltips.drawExact(graphics,font,value,mouseX,mouseY);
    }
    private void tip(String value, int x, int y, int w, int h) {
        if (graphics != null && !background && !designerManufacturer && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h)
            Tooltips.draw(graphics, font, value, mouseX, mouseY);
    }
    private void bar(int x, int y, int w, int h, double value, int color) {
        if (graphics == null) return;
        graphics.fill(x, y, x + w, y + h, 0xFF0B100D);
        graphics.fill(x, y, x + (int) (w * Math.clamp(value, 0, 1)), y + h, color);
    }
    private UiMenuButton button(String accessible, String label, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return null;
        var button = new UiMenuButton(accessible, x, y, w, h, action).caption(label);
        button.active = enabled && pending == 0; button.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal(label.equals("×") || label.equals("X") ? "닫기" : accessible)));
        addRenderableWidget(button);return button;
    }
    private void imageButton(String name, String texture, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new UiMenuButton(name, texture, false, x, y, w, h, action);
        button.active = enabled && pending == 0; addRenderableWidget(button);
    }
    private void originalButton(String name, String texture, int x, int y, int w, int h, Runnable action) {
        originalButton(name, texture, x, y, w, h, true, action);
    }
    private UiPanelButton originalButton(String name, String texture, int x, int y, int w, int h, boolean enabled, Runnable action) {
        return originalButton(name,texture,x,y,w,h,enabled,action,null);
    }
    private UiPanelButton originalButton(String name, String texture, int x, float buttonY, int w, int h, boolean enabled, Runnable action, String disabledTexture) {
        if (graphics != null) return null;
        int y = Math.round(buttonY);
        var button = new UiPanelButton(name, x, y, w, h, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                String path=!active&&disabledTexture!=null?disabledTexture:texture;
                g.pose().pushMatrix();
                g.pose().translate(0,buttonY-y);
                if(texture.equals("construction/ui/down"))g.pose().translate(0,-1f/minecraft.getWindow().getGuiScale());
                if(texture.equals("production/design/division/expand")) {
                    g.pose().translate(x,y); g.pose().scale(0.5f);
                    UiAssets.draw(g,path,0,0,19,22);
                }
                else if(texture.startsWith("construction/ui/"))UiAssets.nineSlice(g,path,x,y,w,h,2,w/22d);
                else UiAssets.draw(g,path,x,y,w,h);
                g.pose().popMatrix();
            }
        };
        if(texture.equals("production/picker/expand")||texture.equals("production/picker/collapse")||texture.contains("checkbox")||texture.startsWith("production/picker/check_"))button.sound("ui.checkbox");
        button.active = enabled && pending == 0;
        button.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal(name)));
        addRenderableWidget(button);
        return button;
    }
    private void divisionSlot(String name, String texture, int x, int y, int w, boolean enabled, Runnable action) {
        divisionSlot(name, texture, x, y, w, 21, enabled, action);
    }
    private void divisionSlot(String name, String texture, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new UiPanelButton(name, x, y, w, h, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                if (h == 16) UiAssets.draw(g, "production/design/division/support_slot", x, y, w, h);
                else {
                    float u = active && !texture.equals("production/designer/add") && isHoveredOrFocused() ? 0.5f : 0;
                    g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/design/division/slot_atlas.png"), x, y, x + w, y + h, u, u + 0.5f, 0, 1);
                }
                String icon = texture.startsWith("production/designer/") ? texture.replace("production/designer/", "production/design/division/") : texture;
                if (active && isHoveredOrFocused() && !texture.startsWith("production/designer/")) icon = "officer/hq/replace";
                int inset = h == 16 ? 0 : 6;
                int iconWidth = Math.min(38, w), iconHeight = h == 16 ? 15 : 20;
                UiAssets.drawAlignedContent(g, icon, icon, x + inset + (w - inset - iconWidth) / 2f - 1, y + (h - iconHeight) / 2f - 0.5f, iconWidth, iconHeight);
            }
        };
        button.active = enabled && pending == 0;
        button.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.createExact(name));
        addRenderableWidget(button);
    }
    private void unitChoice(String name, String texture, String detail, int x, int y, int w, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new UiPanelButton(name, x, y, w, 33, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                UiAssets.nineSlice(g, "commander/commands/mini_tooltip/frame_0", x, y, w, 33, 8, .5);
                if (!texture.isEmpty()) UiAssets.drawAlignedContent(g, texture, texture, x + 4.5f, y + 8.5f, 38, 21);
                int tw = Math.max(1, w - 43), center = x + 43 + tw / 2;
                UiText.left(g, font, name, x + 43, y + 2, tw, 11, active ? TEXT : MUTED);
                if (detail.endsWith(" ★")) {
                    String amount = detail.substring(0, detail.length() - 2);
                    var costLabel = Component.literal(amount).withColor(Tooltips.GOLD);
                    int start = center - (font.width(costLabel) + 13) / 2;
                    g.text(font, costLabel, start, y + 21, 0xFFFFFFFF);
                    UiAssets.draw(g, "hud/army_experience", start + font.width(costLabel) + 2, y + 19, 11, 11);
                } else UiText.marquee(g, font, detail, center - tw / 2f, y + 21, tw, 1, 0xFF000000 | Tooltips.GOOD, true, 0);
            }
        };
        button.active = enabled && pending == 0;
        button.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal(name)));
        addRenderableWidget(button);
    }
}
