package dev.hoi.client.screen;

import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiPanelButton;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.UiTooltips;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiText;

import dev.hoi.protocol.*;
import dev.hoi.protocol.IndustryView.*;
import static dev.hoi.protocol.IndustryProtocol.Action.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;


public final class ScreenIndustry extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final int TEXT = UiMenuStyle.TEXT, MUTED = UiMenuStyle.MUTED;
    private static final int GOOD = 0xFF000000 | UiTooltips.GOOD, BAD = 0xFF000000 | UiTooltips.BAD, GOLD = 0xFF000000 | UiTooltips.GOLD;
    private final String token;
    private final Consumer<IndustryProtocol.Request> transport;
    private MenuTab tab;
    private IndustryView view;
    private final Map<String,Equipment> equipmentById=new HashMap<>();
    private final Map<String,Equipment> equipmentBySource=new HashMap<>();
    private final Map<String,String> navalFamilyByTexture=new HashMap<>();
    private final Map<String,TankDesignerView.Slot> designSlots=new HashMap<>();
    private final Map<String,Template> templatesById=new HashMap<>();
    private final Map<String,NavalRepair> repairShipsById=new HashMap<>();
    private List<Equipment> productionItems=List.of(),navalTypes=List.of();
    private final Map<String,Long> navalCounts=new HashMap<>();
    private int militaryFactories,navalFactories;
    private int pane, top, scroll, secondScroll, pickerScroll, hudScroll, pending, refresh;
    private int designerScroll;
    private static final Map<String, Map<String, Integer>> factoryScales = new HashMap<>();
    public static void clearPreferences() { factoryScales.clear(); }
    private Map<String, Integer> factoryScales() { return factoryScales.computeIfAbsent(view.country(), key -> new HashMap<>()); }
    private String unitGroup = "", logisticsEquipment = "";
    private int tradeTab = 1, tradeAmount = 1, selectedSlot = -1;
    private boolean supportSlot, designer, closed, background, showOutdated;
    private final Set<String> hiddenResources = new HashSet<>(), expandedRepairPorts = new HashSet<>();
    private String navalFamily = "";
    private String group = "all", picker = "", switchLine = "", resource = "IRON", partner = "";
    private boolean openingDesigner;
    private String location = "", locationRecruit = "", draftName = "";
    private record ScreenDeploymentLocation(int x,int y,int width,int height,String recruit,String label) {}
    private final List<ScreenDeploymentLocation> deploymentLocations = new ArrayList<>();
    private boolean selectingDeployment;
    public boolean selectingDeployment() {return selectingDeployment && !closed;}
    public boolean acceptsDeploymentOverlay(String session,long revision) {return selectingDeployment() && token.equals(session) && view != null && revision >= view.revision();}
    private EditBox nameBox, tankNameBox;
    private String tankName="",tankSlot="",tankCategory="";
    private int tankScroll;
    private boolean tankRolesOpen;
    private boolean designPresetsOpen,presetApplying;
    private String presetMessage="";
    private List<EquipmentDesignPreset> designPresets=List.of();
    private final dev.hoi.client.ui.EquipmentDesignPresets presetStore=new dev.hoi.client.ui.EquipmentDesignPresets(
            net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("hoi/equipment-design-presets.json"));
    private final Set<String> collapsedRecruitment = new HashSet<>();
    private GuiGraphicsExtractor graphics;
    private int mouseX, mouseY;

    public ScreenIndustry(MenuTab tab) {
        this(tab, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(IndustryProtocol.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    public ScreenIndustry(MenuTab tab, String token, IndustryView fixture, Consumer<IndustryProtocol.Request> transport) {
        super(Component.literal(title(tab)));
        this.tab = tab; this.token = token; this.view = fixture; this.transport = transport;
        indexView();
        if(fixture!=null&&fixture.tankDesigner()!=null)tankName=fixture.tankDesigner().name();

    }
    public static boolean supports(MenuTab tab) {
        return tab == MenuTab.PRODUCTION || tab == MenuTab.TRADE || tab == MenuTab.LOGISTICS || tab == MenuTab.RECRUITMENT;
    }
    private static String title(MenuTab tab) {
        return switch (tab) { case PRODUCTION -> "생산"; case TRADE -> "경제 · 무역";
            case LOGISTICS -> "군수"; case RECRUITMENT -> "모병 및 배치"; default -> "산업"; };
    }
    public void open() { send(OPEN, "", "", 0); }
    public void update(IndustryView next) {
        if (closed || !next.session().equals(token)) return;
        if (next.country().isEmpty() || view != null && !next.country().equals(view.country())) {
            closed = true; minecraft.gui.setScreen(null);
            if (minecraft.player != null && !next.message().isBlank()) minecraft.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(next.message()));
            return;
        }
        if (view != null && next.revision() <= view.revision()) return;
        if (nameBox != null) draftName = nameBox.getValue();
        if (next.draft() != null && (view == null || view.draft() == null)) draftName = next.draft().name();
        if (designer && next.draft() == null) designer = false;
        if(next.tankDesigner()!=null&&!tankDesigner()) {tankName=next.tankDesigner().name();tankSlot="";tankScroll=0;}
        if(presetApplying) {
            if(next.tankDesigner()!=null&&next.message().equals("저장된 설계를 불러왔습니다."))tankName=next.tankDesigner().name();
            presetApplying=false;
        }
        view = next; pending = 0; indexView();
        if (selectingDeployment && (next.message().equals("배치 장소를 지정했습니다.") || next.recruits().stream().noneMatch(r -> r.id().equals(locationRecruit)))) {
            selectingDeployment = false; dev.hoi.client.map.MapSceneRenderer.clearDeployment();
        }
        if (view.locations().stream().noneMatch(l -> l.id().equals(location)))
            location = "";
        rebuildWidgets();
    }
    int panelWidth() { return pane; }
    private boolean compact() { return height - top < 300; }
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
        boolean naval=group.equals("navy");
        productionItems=view.equipment().stream().filter(e->e.unlocked()&&(group.equals("all")||e.group().equals(group)))
                .filter(e->!naval||!"convoy".equals(e.family()))
                .filter(e->showOutdated||!e.outdated()).filter(e->navalFamily.isEmpty()||Objects.equals(navalFamily,e.family()))
                .filter(e->e.resources().keySet().stream().noneMatch(hiddenResources::contains)).toList();
        var models=new LinkedHashMap<String,Equipment>();navalCounts.clear();
        for(var e:view.equipment())if(e.unlocked()&&e.family()!=null) {
            if(e.naval()&&!"convoy".equals(e.family()))models.putIfAbsent(e.family(),e);
            if(showOutdated||!e.outdated())navalCounts.merge(e.family(),1L,Long::sum);
        }
        var order=List.of("carrier","heavy_cruiser","light_cruiser","destroyer","submarine","battleship","battlecruiser");
        navalTypes=models.values().stream().sorted(Comparator.comparingInt(e->{int index=order.indexOf(e.family());return index<0?order.size():index;})).toList();
    }
    private int bottom() { return height - 25; }
    private int rightWidth() { return Math.min(Math.max(176, width * 380 / 2560), width - pane - 8); }
    private boolean modal() { return tankDesigner() || designer || !picker.isEmpty() || !partner.isEmpty(); }
    private int productionPickerWidth() { return Math.min(width-16,Math.max(196,Math.round(pane*(picker.equals("repairs")?444f:495f)/550))); }
    private boolean dockedProductionPicker() { return !tankDesigner() && tab==MenuTab.PRODUCTION && (picker.equals("equipment")||picker.equals("repairs")) && width-pane>=productionPickerWidth()+4; }
    @Override protected void rebuildWidgets() {
        var focused = getFocused();
        super.rebuildWidgets();
        if (focused != null && children().contains(focused)) setFocused(focused);
    }
    @Override protected void init() {
        pane = UiPanelLayout.width(tab, width); top = UiMenuBar.height(width);
        filterProduction();
        graphics = null; if (!designer) nameBox = null; deploymentLocations.clear();
        if (selectingDeployment) return;
        UiMenuBar.buttons(width, view == null ? "" : view.country(), tab, next -> {
            if (next == tab) return;
            if (next == MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(next);
        }).forEach(this::addRenderableWidget);
        if (!modal() || dockedProductionPicker()) button("닫기", "×", pane - 25, top + 3, 19, 19, true, this::onClose);
        if (view != null) {
            if (!modal() || dockedProductionPicker()) layout();
            if (tankDesigner()) layoutEquipmentDesigner();
            else if (designer) layoutDesigner();
            else if (!partner.isEmpty()) layoutContract();
            else if (!picker.isEmpty()) layoutPicker();
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (selectingDeployment && view != null) {
            UiMenuBar.drawPassive(g, width, view.hud(), view.country());
            String instruction = pending > 0 ? "배치 가능한 장소를 불러오는 중…" : "좌클릭: 배치 장소 지정  ·  우클릭: 취소";
            g.centeredText(font, instruction, width / 2, height - 24, TEXT); return;
        }
        graphics = g; mouseX = mx; mouseY = my;
        UiMenuStyle.panel(g, 0, top, pane, height - top);
        heading(title(tab), 9, top + 8, pane - 40, TEXT);
        if (view == null) {
            UiMenuBar.draw(g, width, dev.hoi.client.ClientCampaignHud.hud(), mx, my, hudScroll);
            text("국가 현황을 불러오는 중…", 9, top + 37, pane - 18, MUTED);
        }
        else {
            UiMenuBar.draw(g, width, view.hud(), mx, my, hudScroll);
            background = modal() && !dockedProductionPicker();
            layout();
            background = false;
            if (tankDesigner()) layoutEquipmentDesigner();
            else if (designer) layoutDesigner();
            else if (!partner.isEmpty()) layoutContract();
            else if (!picker.isEmpty()) layoutPicker();
        }
        super.extractRenderState(g, mx, my, delta); graphics = null;
    }
    private void layout() {
        switch (tab) { case PRODUCTION -> production(); case TRADE -> trade();
            case LOGISTICS -> logistics(); case RECRUITMENT -> recruitment(); default -> {} }
    }
    private void production() {
        var economy = view.economy();
        var lines = view.lines();
        int start = top + 135, h = productionRowHeight(), bodyTop = top + 27;
        if (compact()) scroll = Math.clamp(scroll, 0, Math.max(0, start + lines.size() * h - bottom()));
        else scroll = clampScroll(scroll, lines.size(), h, start);
        int offset = compact() ? scroll : 0;
        if (graphics != null) graphics.enableScissor(0, bodyTop, pane, bottom());
        resourceStrip(top + 27 - offset, 32);
        var modifiers = productionIndicators();
        int cell = (pane - 12) / 6;
        for (int i = 0; i < modifiers.size(); i++) {
            var m = modifiers.get(i); int x = 6 + i * cell, y = top + 62 - offset;
            recess(x, y, cell - 1, 19);
            iconText("production/modifiers/" + m.id(), percent(m.value()), x + 2, y + 1, cell - 5, 17, 14, m.value()==0?TEXT:(m.id().equals("damage")?m.value()<0:m.value()>0)?0xFF009F03:0xFFFF3232);
            productionTip(ScreenProductionTooltips.modifier(m),x,y,cell,19);
        }
        cell = (pane - 12) / 3;
        int used = militaryFactories;
        int naval = navalFactories;
        String[] icons = {"military_factory_icon", "dockyard_icon", "dockyard_icon_with_wrench"};
        String[] names = {"군수공장 사용 / 전체", "조선소 사용 / 전체", "해군 수리용 조선소 사용 / 전체"};
        String[] counts = {used + "/" + economy.military(), naval + "/" + economy.dockyards(), repairDockyards()};
        int summaryY = top + 84 - offset;
        for (int i = 0; i < 3; i++) {
            int x = 6 + i * cell;
            rail(x, summaryY, cell - 1, 22);
            iconText("production/summary/" + icons[i], counts[i], x + 2, summaryY + 1, cell - 5, 20, 15, TEXT);
            tip(names[i] + ": " + counts[i] + (i == 2 ? "\n함선 건조에 배정한 조선소를 제외한 수리 배정 현황" : ""), x, summaryY, cell, 22);
        }
        int filterY = top + 109 - offset;
        if (filterY >= bodyTop && filterY + 22 <= bottom()) productionActions(filterY);
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i); var e = equipment(line.equipment()); int y = start + i * h - scroll;
            if (!visible(y, h, compact() ? bodyTop : start)) continue;
            productionLine(line, e, i, y);

        }
        if (graphics != null) graphics.disableScissor();

    }
    private int productionRowHeight() {
        return Math.max(62, Math.round((pane - 12) * 116f / 511)) + 2;
    }
    private void productionLine(Line line, Equipment e, int index, int y) {
        float scale = (pane - 12) / 511f;
        java.util.function.IntUnaryOperator x = n -> 6 + Math.round(n * scale);
        java.util.function.IntUnaryOperator dy = n -> y + Math.round(n * scale);
        int header = Math.max(1, Math.round(24 * scale));
        int step = e.naval() ? 1 : factoryScales().getOrDefault(line.id(), 1);
        int limit = Math.min(e.factoryLimit(), line.availableFactories());
        art("production/line/" + (e.naval() ? (e.factoryLimit() < 15 ? "naval_" + e.factoryLimit() : "naval") : "land"), 6, y, pane - 12, Math.round(108 * scale));
        cellText(Integer.toString(index + 1), x.applyAsInt(3), y + 3, x.applyAsInt(30) - x.applyAsInt(3), header, TEXT, false);
        tip(e.name(),x.applyAsInt(35),y,x.applyAsInt(219)-x.applyAsInt(35),header);
        cellText(UiText.ellipsize(font,e.name(),(int)((x.applyAsInt(219)-x.applyAsInt(35))/UiText.scale(font))), x.applyAsInt(35), y + 1, x.applyAsInt(219) - x.applyAsInt(35), header, TEXT, false);
        if (!e.naval()) {
            if (graphics != null) {
                graphics.pose().pushMatrix(); graphics.pose().translate(6, y); graphics.pose().scale(scale);
                UiAssets.draw(graphics, "production/line/efficiency", 225, 4, 24, 21);
                productionBar(graphics, "efficiency", 251, 10, 93, 9, line.efficiency());
                graphics.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/line/efficiency_frame.png"), 248, 7, 347, 22, 0, 1, 0, 1);
                graphics.pose().popMatrix();
            }
            productionTip(ScreenProductionTooltips.efficiency(view,line), x.applyAsInt(248), y, Math.round(99 * scale), header);
        }
        if (graphics != null) UiAssets.drawContent(graphics, "production/line/" + (e.naval() ? "dockyard_0" : "factory_0"),
                x.applyAsInt(354) + 2, y + 2, x.applyAsInt(379) - x.applyAsInt(354), header);
        originalButton("공장 줄이기", "production/line/minus", x.applyAsInt(379) + 1, y + 2, header, header, line.factories() > 0,
                () -> send(ASSIGN, line.id(), "", Math.max(0, line.factories() - factoryStep())));
        productionText(graphics, (line.reservedFactories() > 0 ? (line.operatingFactories() + line.damagedFactories()) + "/" : "") + line.factories(), x.applyAsInt(403) + 1, y + 3, x.applyAsInt(453) - x.applyAsInt(403), header, TEXT);
        originalButton("공장 늘리기 · 부족분 예약", "production/line/plus", x.applyAsInt(453) + 1, y + 2, header, header, line.factories() < limit,
                () -> send(ASSIGN, line.id(), "", Math.min(limit, line.factories() + factoryStep())));
        originalButton("생산 라인 삭제", "production/line/delete", x.applyAsInt(481), y + 2, header, header, true, () -> send(REMOVE, line.id(), "", 0));
        productionEquipment(line, e, x.applyAsInt(3), dy.applyAsInt(30), scale);
        int controlsX = x.applyAsInt(206), infoX = x.applyAsInt(233);
        originalButton("생산 라인 한 칸 위로", "production/line/up", controlsX, dy.applyAsInt(50), Math.round(24 * scale), Math.round(24 * scale), index > 0,
                () -> send(UP, line.id(), "", dev.hoi.client.ui.UiPriorityControls.shifted() ? 1 : 0));
        originalButton("생산 라인 한 칸 아래로", "production/line/down", controlsX, dy.applyAsInt(77), Math.round(24 * scale), Math.round(24 * scale), index < view.lines().size() - 1,
                () -> send(DOWN, line.id(), "", dev.hoi.client.ui.UiPriorityControls.shifted() ? 1 : 0));
        productionStock(e, infoX, dy.applyAsInt(78), x.applyAsInt(275) - infoX, Math.round(22 * scale));
        var conversion = line.conversion();
        if (conversion != null) {
            boolean converting = conversion.source() != null;
            if (converting) art("production/modifiers/retention", infoX, dy.applyAsInt(50), x.applyAsInt(275) - infoX, Math.round(27 * scale));
            tip((converting ? equipment(conversion.source()).name() + " → " + e.name() + "\n전환용 재고 " + conversion.stock() : "신규 장비 생산")
                    + "\n1개당 " + ic(conversion.cost()) + " IC · 전환 속도 혜택 " + percent(conversion.speed() - 1)
                    + "\n장비 이미지를 눌러 변경합니다.", infoX, dy.applyAsInt(50), x.applyAsInt(275) - infoX, 16);
        }
        if (!e.naval()) for (int multiplier : new int[]{10, 5}) {
            String label = line.id() + " · ×" + multiplier;
            originalButton(label, "production/line/x" + multiplier + (step == multiplier ? "_selected" : "_normal"),
                    x.applyAsInt(278), dy.applyAsInt(multiplier == 10 ? 30 : 68), Math.round(36 * scale), Math.round(36 * scale), () -> {
                        factoryScales().put(line.id(), step == multiplier ? 1 : multiplier); rebuildWidgets();
                    });
            tip("공장 " + multiplier + "개씩 표시 · 다시 누르면 ×1", x.applyAsInt(278), dy.applyAsInt(multiplier == 10 ? 30 : 68), Math.round(36 * scale), Math.round(36 * scale));
        }
        for (int cell = 0; cell < 15; cell++) {
            int fx = x.applyAsInt(317 + cell % 5 * 29), fy = dy.applyAsInt(33 + cell / 5 * 23);
            factoryCell(line, e, cell, step, limit, y, fx, fy, Math.round(28 * scale), Math.round(22 * scale));
        }
        int resourceRow = 0;
        for (var resource : (conversion != null && conversion.source() != null ? conversion.resources() : e.resources()).entrySet()) {
            if (resourceRow == 3) break;
            int ry = dy.applyAsInt(33 + resourceRow++ * 23);
            boolean missing = line.missingResources().contains(resource.getKey());
            productionResource(resourceArt((missing ? "missing/" : "") + resource.getKey()), integer(resource.getValue() * line.operatingFactories()), x.applyAsInt(464), ry + 1,
                    pane - 9 - x.applyAsInt(464), Math.round(22 * scale), missing ? BAD : TEXT);
        }
    }
    private static void productionBar(GuiGraphicsExtractor g, String name, int x, int y, int w, int h, double value) {
        float ratio=(float)Math.clamp(value,0,1);
        var background=net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/line/"+name+"_bg.png");
        var fill=net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/line/"+name+"_fill.png");
        g.blit(background,x,y,x+w,y+h,0,1,0,1);
        int filled=Math.round(w*ratio);
        if(filled>0)g.blit(fill,x,y,x+filled,y+h,0,ratio,1f/3,2f/3);
    }
    private void productionStock(Equipment equipment, int x, int y, int w, int h) {
        if(graphics!=null)return;
        var button=new UiPanelButton(equipment.name()+" · 재고량 · 군수 보기",x,y,w,h,()->{
            tab=MenuTab.LOGISTICS; logisticsEquipment=equipment.id(); scroll=0;
            var rows=ScreenLogisticsRows.create(view.equipment(),view.lines(),"all");
            for(int i=0;i<rows.size();i++)if(rows.get(i).models().stream().anyMatch(e->e.id().equals(logisticsEquipment))) {scroll=i*39;break;}
            rebuildWidgets();
        }) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                productionText(g,ScreenLogisticsRows.stockLabel(equipment.stockpile()),x,y,w,h,GOOD);
            }
        };
        button.active=pending==0;
        button.setTooltip(UiTooltips.create(Component.literal(equipment.name()+" 비축량: "+equipment.stockpile()+"\n클릭하여 군수 현황 보기")));
        addRenderableWidget(button);
    }
    @Override public Component getTitle() { return Component.literal(title(tab)); }
    private void productionResource(String texture, String value, int x, int y, int w, int h, int color) {
        if (graphics == null) return;
        int size=Math.min(7,h-2),textWidth=Math.min(w-size-3,Math.round(font.width(value)*UiText.scale(font)));
        int left=x+(w-size-1-textWidth)/2;
        UiAssets.drawContent(graphics,texture,left,y+(h-size)/2,size,size);
        productionText(graphics,value,left+size+1,y,textWidth,h,color);
    }
    private void productionText(GuiGraphicsExtractor g, String value, int x, int y, int w, int h, int color) {
        productionText(g,value,x,y,w,h,color,"");
    }
    private void productionText(GuiGraphicsExtractor g, String value, int x, int y, int w, int h, int color, String raisedUnit) {
        if (g == null) return;
        int textWidth=font.width(value+raisedUnit);
        float scale=Math.min(UiText.scale(font),Math.min((w-2f)/Math.max(1,textWidth),h/(float)font.lineHeight));
        g.pose().pushMatrix();
        g.pose().translate(x+(w-textWidth*scale)/2,y+(h-font.lineHeight*scale)/2);
        g.pose().scale(scale);g.text(font,value,0,0,color);
        if(!raisedUnit.isEmpty()) {
            g.pose().translate(font.width(value),-1/scale);
            g.text(font,raisedUnit,0,0,color);
        }
        g.pose().popMatrix();
    }
    private void productionEquipment(Line line, Equipment equipment, int x, int y, float scale) {
        if (graphics != null) return;
        int w=Math.round(201*scale),h=Math.round(70*scale);
        String name=equipment.name()+" · 장비 교체";
        var button=new UiPanelButton(name,x,y,w,h,()->{
            picker="equipment";group=equipment.group();switchLine=line.id();navalFamily="";hiddenResources.clear();pickerScroll=0;rebuildWidgets();
        }) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiAssets.draw(g,equipment.texture(),x,y,w,h);
                if(equipment.outdated())UiAssets.draw(g,"production/outdated_overlay",x,y,w,h);
                int labelY=y+Math.round(53*scale);
                g.fill(x,labelY,x+w,labelY+Math.round(20*scale),0x88000000);
                UiText.cell(g,font,familyName(equipment),x+2,labelY,w-4,Math.round(20*scale),TEXT,false);
                int rateX=x+Math.round(155*scale),rateY=y+Math.round(scale),rateWidth=Math.round(118*scale),rateHeight=Math.round(21*scale);
                productionBar(g,"progress",rateX,rateY-1,rateWidth,rateHeight,line.progress());
                productionText(g,line.daily()>0?decimal(line.daily())+" / ":"생산량 없음",rateX,rateY-1,rateWidth,rateHeight,line.daily()>0?TEXT:BAD,line.daily()>0?"일":"");
            }
        };
        button.active=pending==0;button.setTooltip(Tooltip.create(UiTooltips.productionText(Component.literal(ScreenProductionTooltips.equipment(view,line,equipment)))));addRenderableWidget(button);
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
                float scale=(pane-12)/511f;
                double column=((mx-6)/scale-317)/29,row=((my-lineY)/scale-33)/23;
                int preview=pending==0&&column>=0&&column<5&&row>=0&&row<3
                        ?Math.min(((int)row*5+(int)column+1)*step,limit):-1;
                int status=productionFactoryState(start,step,normal,damaged,reserved,line.factories(),preview);
                if(status>=0)UiAssets.drawContent(g,"production/line/"+(e.naval()?"dockyard_":"factory_")+(frame+status),x+1,y+1,w-2,h-2);

            }
        };
        button.active = pending == 0 && start < limit;
        button.setTooltip(UiTooltips.create(Component.literal((e.naval() ? "조선소" : "군수공장") + " " + target + "개 배정\n정상 " + normal + " · 손상 " + damaged + " · 예약 " + reserved + " · ×" + step)));
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
    private void productionActions(int y) {
        String[] ids = {"infantry", "armor", "air", "navy", "repair"};
        String[] names = {"보병 및 포병 장비 제작", "기갑 차량 제작", "항공기 제작", "함선 건조", "해군 수리 대기열"};
        int cell = (pane - 12) / ids.length;
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            originalButton(names[i], "production/actions/" + id, 6 + i * cell, y, cell - 1, 22, () -> {
                group = id; picker = id.equals("repair") ? "repairs" : "equipment";
                switchLine = ""; navalFamily=""; hiddenResources.clear(); pickerScroll = 0; rebuildWidgets();
            });
        }
    }
    private void resourceStrip(int y, int h) {
        int cell = (pane - 12) / Math.max(1, view.resources().size());
        for (int i = 0; i < view.resources().size(); i++) {
            var r = view.resources().get(i); int x = 6 + i * cell;
            recess(x, y, cell - 1, h);
            int contentY = y + (h - 27) / 2;
            art("production/resources/" + (r.available() < r.demand() ? "missing/" : "") + r.id().toLowerCase(Locale.ROOT), x + (cell - 1 - 16) / 2, contentY, 16, 16);
            cellText(integer(r.available() - r.demand()), x + 2, contentY + 18, cell - 5, 9, r.available() < r.demand() ? BAD : GOOD, false);
            tip(r.name() + "\n점유지 추출 " + decimal(r.extracted()) + "\n수입 " + decimal(r.imported())
                    + " · 수출 배정 " + decimal(r.exported()) + "\n생산용 " + decimal(r.available()) + " / 수요 " + decimal(r.demand()), x, y, cell, h);
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
    static final List<String> LOGISTICS_HEADERS = List.of("평균 생산 효율", "장비 유형", "생산", "상태", "수요", "균형", "비축량", "자원");
    private void logistics() {
        int[] fractions = {0, 5, 29, 41, 55, 64, 74, 83, 100};
        int[] cols = new int[9];
        for (int i = 0; i < cols.length; i++) cols[i] = 6 + (pane - 12) * fractions[i] / 100;
        String[] icons = {"efficiency", "", "production", "", "need", "balance", "stockpile", ""};
        String[] descriptions = {"가동 중인 공장 수로 가중한 평균 생산 효율", "장비 유형", "각 장비의 하루 생산량", "현재 충원 수요 대비 비축 장비의 충족률", "훈련·배치 사단과 자동 보충 비행단의 현재 미지급 장비", "비축량에서 현재 충원 수요를 뺀 수량", "현재 보관 중인 장비 총량", "배정된 생산 공장이 요구하는 자원"};
        int heading = top + 29, start = heading + 22, h = 39;
        rail(6, heading, pane - 12, 20);
        for (int i = 0; i < LOGISTICS_HEADERS.size(); i++) {
            int w = cols[i + 1] - cols[i];
            if (icons[i].isEmpty()) cellText(LOGISTICS_HEADERS.get(i), cols[i] + 1, heading, w - 2, 20, TEXT, false);
            else art("logistics/" + icons[i], cols[i] + 1, heading + 3, w - 2, 14);
            tip(LOGISTICS_HEADERS.get(i) + "\n" + descriptions[i], cols[i], heading, w, 20);
        }
        var items = ScreenLogisticsRows.create(view.equipment(), view.lines(), "all");
        scroll = clampScroll(scroll, items.size(), h, start);
        for (int i = 0; i < items.size(); i++) {
            var row = items.get(i); var e = row.representative(); int y = start + i * h - scroll;
            if (!visible(y, h, start)) continue;
            if (graphics != null) {
                if(row.models().stream().anyMatch(model->model.id().equals(logisticsEquipment))) graphics.fill(6,y,pane-6,y+h-2,0x443D6B3D);
                UiAssets.nineSlice(graphics, "logistics/efficiency_frame", cols[0], y, cols[1] - cols[0], h - 2, 3, 0.65);
                UiAssets.nineSlice(graphics, "logistics/equipment_frame", cols[1], y, cols[2] - cols[1], h - 2, 5, 0.65);
                UiAssets.nineSlice(graphics, "logistics/resource_frame", cols[7], y, cols[8] - cols[7], h - 2, 5, 0.65);
            }
            efficiencyBar(cols[0] + 2, y + 3, Math.max(2, cols[1] - cols[0] - 4), h - 8, row.efficiency() == null ? 0 : row.efficiency());
            int equipmentY = y + (h - 2 - 31) / 2;
            art(e.texture(), cols[1] + 1, equipmentY, cols[2] - cols[1] - 2, 20);
            cellText(familyName(e), cols[1] + 1, equipmentY + 21, cols[2] - cols[1] - 2, 10, GOLD, false);
            String[] values = {ScreenLogisticsRows.productionLabel(row.daily()), "", row.demandLabel(), row.balanceLabel(), ScreenLogisticsRows.stockLabel(row.stockpile())};
            for (int j = 0; j < values.length; j++) {
                int col = j + 2;
                if (graphics != null) UiAssets.nineSlice(graphics, "logistics/number_frame", cols[col], y + 7, cols[col + 1] - cols[col], 22, 5, 0.65);
                if (j == 1) {
                    int x = cols[col] + 3, width = cols[col + 1] - cols[col] - 6;
                    if (graphics != null) graphics.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/logistics/status_bg.png"),
                            x, y + 13, x + width, y + 23, 0, 1, 0, 1);
                    if (graphics != null && row.demand() != null) {
                        double ratio = row.demand() == 0 ? 1 : Math.clamp(row.stockpile() / (double)row.demand(), 0, 1);
                        int filled = (int)Math.round(width * ratio);
                        if (filled > 0) graphics.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/logistics/status_bar.png"),
                                x, y + 13, x + filled, y + 23, 0, (float)ratio, 0, 1);
                    }
                } else {
                    int color = j == 0 || j == 4 ? GOOD : row.balance() == null ? MUTED : row.balance() >= 0 ? GOOD : BAD;
                    cellText(values[j], cols[col] + 2, y + 7, cols[col + 1] - cols[col] - 4, 22, color, false);
                }
            }
            int n = 0, size = Math.min(16, Math.max(1, (cols[8] - cols[7] - 6) / 3));
            int resourceX = cols[7] + (cols[8] - cols[7] - 3 * size) / 2;
            int resourceY = y + (h - 2 - 2 * size) / 2;
            for (var resource : row.resources().entrySet()) {
                art(resourceArt(resource.getKey()), resourceX + n % 3 * size, resourceY + n / 3 * size, size, size);
                tip(resourceName(resource.getKey()) + ": " + decimal(resource.getValue()), resourceX + n % 3 * size, resourceY + n / 3 * size, size, size);
                n++;
            }
            tip("평균 생산 효율: " + (row.efficiency() == null ? "—" : ProtocolNumbers.percent(row.efficiency(), 1)), cols[0], y, cols[1] - cols[0], h);
            tip(row.supplyDetail(), cols[3], y, cols[6] - cols[3], h);
            tip(familyName(e) + "\n충원에 추가로 필요한 수량: " + row.deficit() + "\n\n" + row.models().stream().map(model -> model.name() + "\n보관 재고: " + model.stockpile()
                    + "\n훈련에 지급: " + model.reserved() + "\n배치 부대 보유: " + model.deployed()
                    ).collect(java.util.stream.Collectors.joining("\n\n")), cols[1], y, cols[2] - cols[1], h);
            tip("비축량: " + row.stockpile(), cols[6], y, cols[7] - cols[6], h);
        }
        if (items.isEmpty()) text("연구 완료 또는 비축된 장비가 없습니다.", 10, start + 6, pane - 20, MUTED);
    }
    private void trade() {
        int half = (pane - 14) / 2;
        button("경제", "경제", 6, top + 27, half, 22, true, () -> { tradeTab = 0; scroll = 0; rebuildWidgets(); });
        button("무역", "무역", 8 + half, top + 27, half, 22, true, () -> { tradeTab = 1; scroll = 0; rebuildWidgets(); });
        var e = view.economy();
        if (tradeTab == 0) {
            String[][] rows = {{"실질 GDP", amount(e.gdp())}, {"부채", amount(e.debt())},
                    {"민간공장", "" + e.civilian()}, {"무역 가용 공장", "" + e.freeCivilian()},
                    {"소비재 공장", "" + e.consumer()}, {"군수공장", "" + e.military()}, {"조선소", "" + e.dockyards()},
                    {"전력 공급 / 수요", decimal(e.energy()) + " / " + decimal(e.energyDemand())},
                    {"연료 비축", decimal(e.fuel())}, {"인력", manpower(e.manpower())}};
            int start = top + 55; scroll = clampScroll(scroll, rows.length, 35, start);
            for (int i = 0; i < rows.length; i++) {
                int y = start + i * 35 - scroll; if (!visible(y, 35, start)) continue;
                recess(6, y, pane - 12, 32); text(rows[i][0], 11, y + 4, pane - 22, MUTED);
                text(rows[i][1], 11, y + 18, pane - 22, TEXT);
            }
            return;
        }
        rail(6, top + 54, pane - 12, 21);
        art("construction/civilian_factory", 9, top + 57, 15, 15);
        leftText("민간공장", 28, top + 61, 45, TEXT); leftText("" + e.civilian(), 74, top + 61, pane/2-78, GOLD);
        leftText("이용 가능:", pane/2+4, top + 61, pane/2-34, TEXT); leftText("" + e.freeCivilian(), pane - 28, top + 61, 21, GOLD);
        int labelWidth = compact() ? 0 : 36, cell = Math.max(16, (pane - 12 - labelWidth) / Math.max(1, view.resources().size()));
        String[] labels = {"추출", "수입", "수출", "생산", "잔여"};
        for (int i = 0; i < view.resources().size(); i++) {
            var r = view.resources().get(i); int x = 6 + labelWidth + i * cell;
            imageButton(r.name(), resourceArt(r.id()), x, top + 79, cell - 1, 20, true,
                    () -> { resource = r.id(); scroll = 0; rebuildWidgets(); });
            double[] values = {r.extracted(), r.imported(), -r.exported(), -r.demand(), r.available() - r.demand()};
            for (int j = 0; !compact() && j < values.length; j++) {
                recess(x, top + 102 + j * 13, cell - 1, 12);
                cellText(integer(values[j]), x + 2, top + 102 + j * 13, cell - 5, 12, values[j] < 0 ? BAD : GOOD, true);
            }
            tip(r.name() + "\n추출 " + decimal(r.extracted()) + " · 수입 " + decimal(r.imported()) + "\n수출 배정 " + decimal(r.exported())
                    + "\n생산 수요 " + decimal(r.demand()) + " · 잔여 " + decimal(r.available() - r.demand()), x, top + 79, cell, compact() ? 20 : 88);
        }
        for (int j = 0; !compact() && j < labels.length; j++) cellText(labels[j], 8, top + 102 + j * 13, labelWidth - 4, 12, MUTED, true);
        int heading = top + (compact() ? 104 : 172);
        var contracts = view.trades().stream().filter(t -> t.importing() && t.resource().equals(resource))
                .collect(java.util.stream.Collectors.groupingBy(IndustryView.Trade::partner));
        int rowX = 6, rowWidth = pane - 12;
        double rowScale = rowWidth / 576.0;
        int start = heading + 18, rowHeight = Math.max(20, (int)Math.round(45 * rowScale)), h = rowHeight + 1;
        int[] cols = {rowX + (int)Math.round(4 * rowScale), rowX + (int)Math.round(250 * rowScale), rowX + (int)Math.round(341 * rowScale), rowX + (int)Math.round(450 * rowScale)};
        int[] ends = {rowX + (int)Math.round(248 * rowScale), rowX + (int)Math.round(328 * rowScale), rowX + (int)Math.round(438 * rowScale), rowX + (int)Math.round(571 * rowScale)};
        String[] headers = {"국가", "수출", "운송됨", "수송선"};
        for (int i = 0; i < 4; i++) cellText(headers[i], cols[i], heading, ends[i] - cols[i], 16, TEXT, false);
        var partners=view.partners().stream().filter(p->Math.round(p.exports().getOrDefault(resource,0.0))>0).toList();
        scroll = clampScroll(scroll, partners.size(), h, start);
        for (int i = 0; i < partners.size(); i++) {
            var p = partners.get(i); int y = start + i * h - scroll; if (!visible(y, h, start)) continue;
            var active = contracts.getOrDefault(p.id(), List.of());
            double delivered = active.stream().mapToDouble(IndustryView.Trade::delivered).sum();
            var detail=p.detail()==null?null:p.detail().resources().get(resource);
            double total=p.exports().getOrDefault(resource,0.0),requested=active.stream().mapToInt(IndustryView.Trade::factories).sum()*(p.resourcesPerFactory()==null?8:p.resourcesPerFactory());
            if(detail!=null)requested=detail.requested();
            double remaining=detail==null?Math.max(0,total-delivered):detail.remaining();
            art(!active.isEmpty() ? "trade/row_active" : !p.route() ? "trade/row_blocked" : "trade/row", rowX, y, rowWidth, rowHeight);
            int flagX = rowX + (int)Math.round(7 * rowScale), flagY = y + (int)Math.round(8 * rowScale);
            int flagWidth = Math.max(1, (int)Math.round(41 * rowScale)), flagHeight = Math.max(1, (int)Math.round(26 * rowScale));
            if (graphics != null) UiAssets.cover(graphics, UiMenuBar.flagTexture(p.id()), flagX, flagY, flagWidth, flagHeight);
            art("trade/flag_overlay", flagX, flagY, flagWidth, flagHeight);
            art("trade/flag_small_frame", rowX + (int)Math.round(4 * rowScale), y + (int)Math.round(6 * rowScale), (int)Math.round(47 * rowScale), (int)Math.round(32 * rowScale));
            int nameX = rowX + (int)Math.round(53 * rowScale);
            leftText(p.name(), nameX, y + (rowHeight - Math.round(font.lineHeight * UiText.scale(font))) / 2, cols[1] - nameX - 3, TEXT);
            tradeValue(Component.literal(integer(remaining)+"/"+integer(total)),cols[1],y,ends[1]-cols[1],rowHeight,GOLD);
            String shipped=active.isEmpty()?"§e0":"§"+(delivered+1e-9<requested?"c":"e")+integer(delivered)+"§r/§e"+integer(requested);
            tradeValue(UiTooltips.styled(Component.literal(shipped),false),cols[2],y,ends[2]-cols[2],rowHeight,TEXT);
            String convoy=detail!=null&&!active.isEmpty()&&detail.convoyNeeded()>0?""+detail.convoyUsed():ScreenTradeAmounts.convoyCell(p.convoys());
            tradeValue(Component.literal(convoy),cols[3],y,ends[3]-cols[3],rowHeight,active.isEmpty()?TEXT:GOOD);
            if (graphics == null) {
                var row = new UiPanelButton(p.name() + "에서 수입", 6, y, pane - 12, rowHeight, () -> { partner = p.id(); tradeAmount = active.stream().mapToInt(IndustryView.Trade::factories).sum(); rebuildWidgets(); }) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        if (isHoveredOrFocused()) g.outline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
                    }
                };
                row.active = !background && pending == 0; addRenderableWidget(row);
            }
            if(detail!=null) {
                String influence=p.detail().influence(),law=p.detail().law();
                productionTip(detail.status()+"\n\n"+influence+(law.isBlank()?"":"\n\n"+law),cols[0],y,ends[0]-cols[0],rowHeight);
                productionTip(detail.exports(),cols[1],y,ends[1]-cols[1],rowHeight);
                productionTip(detail.transported(),cols[2],y,ends[2]-cols[2],rowHeight);
                productionTip(detail.convoys(),cols[3],y,ends[3]-cols[3],rowHeight);
            }
        }
    }

    private void tradeValue(Component value,int x,int y,int width,int height,int color) {
        if(graphics==null)return;
        float scale=Math.min(UiText.scale(font),(width-2f)/Math.max(1,font.width(value)));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x+(width-font.width(value)*scale)/2,y+(height-font.lineHeight*scale)/2);
        graphics.pose().scale(scale);graphics.text(font,value,0,0,color);graphics.pose().popMatrix();
    }

    private double minimumTraining() { return view.recruitmentPolicy() == null || view.recruitmentPolicy().minimumTraining() == null ? .2 : view.recruitmentPolicy().minimumTraining(); }
    private void recruitment() {
        int secondX = pane + 3, secondW = rightWidth(), start = top + 28;
        if (graphics != null) UiMenuStyle.panel(graphics, secondX, top, secondW, height - top);
        heading("사단 편제", secondX + (secondW - (int)(font.width("사단 편제") * (font.lineHeight + 2f) / font.lineHeight)) / 2, top + 8, secondW - 16, TEXT);
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
                String needs = policy.reinforcementNeeds().entrySet().stream().map(e -> familyName(equipment(e.getKey())) + " · 부족 " + e.getValue()).collect(java.util.stream.Collectors.joining("\n"));
                tip("증원 · 장비 충족률 " + percent(policy.reinforcementRatio()) + (needs.isEmpty() ? "\n부족한 장비 없음" : "\n" + needs), 7, y, (int)(400 * scale), rowHeight);
            } else tip(names[i] + (ratio == null ? " · 장비 배정 현황 미제공" : " · 장비 충족률 " + percent(ratio)), 7, y, (int)(400 * scale), rowHeight);
        }
        int cursor = start + prioritiesHeight - scroll;
        for (var entry : groups.entrySet()) {
            var rows = entry.getValue(); var first = rows.getFirst(); var t = template(first.template());
            int y = cursor; cursor += groupHeight;
            if (visible(y, groupHeight, start)) {
                art("recruitment/conveyor", groupX, y, (int)Math.round(490 * gs), headerHeight);
                if (!t.line().isEmpty()) art(unitTexture(t.line().getFirst(), false), groupX + 2 + (int)(10 * gs), y + (int)(17 * gs), (int)(70 * gs), (int)(50 * gs));
                tip(t.name(),groupX+(int)(95*gs),y+(int)(17*gs),(int)(214*gs),(int)(29*gs));
                centeredCompactText(UiText.ellipsize(font,t.name(),(int)(214*gs/UiText.scale(font))), groupX + (int)(95 * gs), y + (int)(29 * gs) - 5, (int)(214 * gs), TEXT);
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
                if (graphics == null) deploymentLocations.add(new ScreenDeploymentLocation(groupX + (int)(91 * gs), y + (int)(46 * gs), (int)(223 * gs), (int)(31 * gs), first.id(), locationName(first.location())));
                tip("좌클릭하여 배치 프로빈스 선택 · 우클릭하여 장소 초기화", groupX + (int)(91 * gs), y + (int)(46 * gs), (int)(223 * gs), (int)(31 * gs));
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
                compactText(UiText.ellipsize(font,t.name()+" "+number,(int)(190*gs/UiText.scale(font))), lineX + (int)(9 * gs), y + (int)(20 * gs) - 3, (int)(190 * gs), TEXT);
                double manpowerRatio = t.manpower() == 0 ? 1 : (double) r.manpower() / t.manpower();
                double equipped = fillRatio(t.equipment(), r.equipment());
                art("recruitment/equipment_state", lineX + 1 + (int)(211 * gs), y + 1 + (int)(7 * gs), (int)(27 * gs), (int)(27 * gs));
                art("recruitment/training", lineX + (int)(307 * gs), y + (int)(7 * gs), (int)(27 * gs), (int)(27 * gs));
                bar(lineX + (int)(237 * gs), y + (int)(14 * gs), (int)(64 * gs), Math.max(3, (int)(11 * gs)), equipped, 0xFF7EAF69);
                bar(lineX + (int)(333 * gs), y + (int)(14 * gs), (int)(64 * gs), Math.max(3, (int)(11 * gs)), r.progress(), 0xFF7EAF69);
                tip(t.name()+" "+number+"\n훈련 " + percent(r.progress()) + " · 인력 " + percent(manpowerRatio) + " · 장비 " + percent(equipped) + "\n" + equipmentList(t.equipment(), r.equipment()), 8, y, (int)(397 * gs), lineHeight);
                cellText(r.seriesLabel(), lineX + (int)(398 * gs) + 2, y + (int)(7 * gs) + 1, (int)(57 * gs), (int)(26 * gs), r.seriesLimit() == 0 ? GOLD : TEXT, false);
                tip("라인의 현재 연속 훈련 수 · " + r.seriesLabel(), lineX + (int)(398 * gs), y + (int)(7 * gs), (int)(57 * gs), (int)(26 * gs));
                textureButton("즉시 배치 · 훈련 " + Math.round(minimumTraining()*100) + "% 이상", "", "recruitment/deploy_line", lineX + (int)(458 * gs), y + (int)(7 * gs), (int)(26 * gs), (int)(26 * gs), !r.location().isEmpty() && r.progress() >= minimumTraining(), () -> send(DEPLOY, r.id(), "", 0));
                textureButton("훈련 취소 · 지급 장비와 인력 반환", "", "recruitment/cancel_line", lineX + (int)(484 * gs), y + (int)(7 * gs), (int)(26 * gs), (int)(26 * gs), true, () -> send(CANCEL_RECRUIT, r.id(), "", 0));
            }
        }
        var special = view.specialForces();
        art("recruitment/special", secondX + 11, top + 30, 20, 20);
        text(special == null ? "—/—" : special.used() + "/" + special.capacity(), secondX + 35, top + 35, secondW - 128, TEXT);
        tip("특수부대 대대 · 사용량 / 한도" + (special == null ? " · 정보 없음" : "\n" + special.used() + " / " + special.capacity() + " · 훈련 대기 포함"), secondX + 7, top + 28, secondW - 100, 23);
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
            centeredCompactText(UiText.ellipsize(font,t.name(),(int)(nameWidth/UiText.scale(font))), nameX, y + (int)(32 * cardScale) - 5, nameWidth, TEXT);
            int buttonWidth = (int)(71 * cardScale), buttonY = y + (int)(49 * cardScale), buttonHeight = Math.max(10, (int)(26 * cardScale));
            textureButton(t.name() + " 훈련", "훈련", "recruitment/button", secondX + 5 + (int)(158 * cardScale), buttonY, buttonWidth, buttonHeight, !retired.contains(t.id()), () -> send(RECRUIT, t.id(), "", 0));
            textureButton(t.name() + " 편제", "편제", "recruitment/button", secondX + 5 + (int)(229 * cardScale), buttonY, buttonWidth, buttonHeight, true, () -> loadDesigner(t.id()));
            textureButton(t.name() + " 편제를 목록에서 삭제", "", "recruitment/delete_template", secondX + 5 + (int)(300 * cardScale), y + (int)(48 * cardScale), (int)(26 * cardScale), (int)(26 * cardScale), true, () -> send(RETIRE_TEMPLATE, t.id(), "", 1));
            tip(t.name() + " · " + manpower(t.manpower()) + "명 · " + integer(t.days()) + "일\n" + equipmentList(t.equipment(), Map.of()), nameX, y, nameWidth, th - 14);
        }
    }
    private static String priorityName(int priority) {return switch(priority) {case 0 -> "낮음"; case 1 -> "보통"; default -> "높음";};}
    private static String priorityTexture(int priority, boolean selected) {return "recruitment/priority" + (selected ? switch(priority) {case 0 -> "_low"; case 1 -> "_normal"; default -> "_high";} : "");}
    private void textureButton(String name, String label, String background, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var control = new dev.hoi.client.ui.UiMenuButton(name, x, y, w, h, action).caption(label).background(background);
        if (name.startsWith("클릭하여")) control.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal("클릭").withStyle(net.minecraft.ChatFormatting.GREEN).append(Component.literal(name.substring(2)).withStyle(net.minecraft.ChatFormatting.WHITE))));
        if (background.equals("recruitment/delete_template")) control.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal("편제를 목록에서 ").withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal("삭제").withStyle(net.minecraft.ChatFormatting.DARK_RED)).append(Component.literal("합니다.").withStyle(net.minecraft.ChatFormatting.WHITE))));
        if (name.contains("우선순위") || name.contains("연속 훈련 횟수")) control.setTooltip(null);
        control.textScale(dev.hoi.client.ui.UiText.scale(font)); control.active = enabled && pending == 0; addRenderableWidget(control);
    }
    private void centeredCompactText(String value, int x, int y, int width, int color) {
        String clipped = font.plainSubstrByWidth(value, Math.max(1, (int)(width / UiText.scale(font))));
        compactText(clipped, x + Math.max(0, (width - (int)Math.ceil(font.width(clipped) * dev.hoi.client.ui.UiText.scale(font))) / 2), y, width, color);
    }
    private void compactText(String value, int x, int y, int width, int color) {
        if (graphics == null) return;
        graphics.pose().pushMatrix(); graphics.pose().translate(x, y); graphics.pose().scale(dev.hoi.client.ui.UiText.scale(font));
        graphics.text(font, font.plainSubstrByWidth(value, Math.max(1, (int)(width / UiText.scale(font)))), 0, 0, color); graphics.pose().popMatrix();
    }
    private void loadDesigner(String id) {
        selectedSlot = -1; designerScroll = 0; picker = ""; openingDesigner = true;
        send(LOAD_TEMPLATE, id, "", 0);

    }
    private int modalWidth() { return Math.min(820, width - 16); }
    private int modalX() { return (width - modalWidth()) / 2; }
    private int modalY() { return Math.max(top + 18, (height - 350) / 2); }
    private int modalHeight() { return Math.min(350, height - modalY() - 8); }
    private void layoutDesigner() {
        var t = view.draft(); if (t == null) return;
        int x = modalX(), y = modalY(), w = modalWidth(), h = modalHeight(), left = Math.min(290, w / 2 - 8);
        panel(x, y, w, h); heading("사단 설계", x + 8, y + 8, w - 40, TEXT);
        button("편제 창 닫기", "×", x + w - 25, y + 3, 19, 19, true, () -> { designer = false; selectedSlot = -1; rebuildWidgets(); });
        if (selectedSlot < 0 && graphics == null) {
            if (nameBox == null) {
                nameBox = new EditBox(font, x + 9, y + 29, left - 8, 20, Component.literal("편제 이름"));
                nameBox.setMaxLength(80); nameBox.setValue(draftName); nameBox.setResponder(value -> draftName = value);
            }
            nameBox.setPosition(x + 9, y + 29); nameBox.setWidth(left - 8);
            if (!nameBox.getValue().equals(draftName)) nameBox.setValue(draftName);
            addRenderableWidget(nameBox);
        }
        if (selectedSlot < 0) {
        art("production/designer/composition", x + 10, y + 53, 12, 11);
        text("사단 구성", x + 27, y + 54, left - 29, TEXT);
        String[] summary = {"HARDNESS", "ORGANIZATION", "HP", "SPEED"};
        for (int i = 0; i < summary.length; i++) {
            String id = summary[i]; int bx = x + 10 + i * (left - 12) / 4;
            var stat = t.stats().stream().filter(v -> id.equals(v.id())).findFirst().orElse(null);
            recess(bx, y + 67, (left - 16) / 4, 17);
            iconText("stats/" + id.toLowerCase(Locale.ROOT), stat == null ? "—" : statValue(stat), bx + 2, y + 68, (left - 16) / 4 - 4, 15, 12, GOLD);
            tip(stat == null ? id : stat.name() + ": " + statValue(stat), bx, y + 67, (left - 16) / 4, 17);
        }
        }
        if (selectedSlot < 0) {
            int regimentHeight = t.columns() == null ? 0 : 42;
            int requiredHeight = 197 + regimentHeight;
            designerScroll = Math.clamp(designerScroll, 0, Math.max(0, requiredHeight - (h - 106)));
            int cell = Math.min(40, (left - 45) / 5), sy = y + 107 - designerScroll;
            for (int i = 0; i < 25; i++) {
                int slot = i, bx = x + 10 + i / 5 * cell, by = sy + i % 5 * 25;
                if (by < y + 90 || by + 22 > y + h - 33) continue;
                String unit = t.lineUnit(i);
                if (t.columns() == null ? i > t.line().size() : i % 5 > t.columnSize(i / 5)) {
                    divisionSlot("이 열의 앞선 빈 대대 칸부터 추가", "production/designer/locked", bx, by, cell - 3, false, () -> {}); continue;
                }
                if (unit.isEmpty()) divisionSlot("대대 추가", "production/designer/add", bx, by, cell - 3, true, () -> selectSlot(slot, false));
                else divisionSlot(unitName(unit, false), unitTexture(unit, false), bx, by, cell - 3, true, () -> selectSlot(slot, false));
            }
            if (designerScroll == 0) text("지원", x + 14 + 5 * cell, sy - 9, 32, MUTED);
            for (int i = 0; i < 5; i++) {
                int slot = i, bx = x + 12 + 5 * cell, by = sy + i * 25;
                if (by < y + 90 || by + 22 > y + h - 33) continue;
                if (i > t.support().size()) divisionSlot("앞선 빈 지원중대 칸부터 추가", "production/designer/locked", bx, by, 30, false, () -> {});
                else if (i == t.support().size()) divisionSlot("지원중대 추가", "production/designer/add", bx, by, 30, true, () -> selectSlot(slot, true));
                else divisionSlot(unitName(t.support().get(i), true), unitTexture(t.support().get(i), true), bx, by, 30, true, () -> selectSlot(slot, true));
            }
            if (t.columns() != null) {
                if (sy + 131 >= y + 90 && sy + 142 < y + h - 33) text("연대 지원", x + 10, sy + 131, left - 12, TEXT);
                for (int column = 0; column < 5; column++) {
                    int slot = column, bx = x + 10 + column * cell, by = sy + 145;
                    if (by < y + 90 || by + 22 > y + h - 33) continue;
                    boolean unlocked = t.columnSize(column) >= 3;
                    divisionSlot(unlocked ? "연대 지원" : "연대 지원 · 같은 열에 대대 3개 필요",
                            unlocked ? "production/designer/add" : "production/designer/locked", bx, by, cell - 3, false, () -> {});
                }
            }
        } else {
            recess(x + 7, y + 28, left - 3, h - 83);
            text(supportSlot ? "지원중대 추가 / 변경" : selectedSlot % 5 == 0 && unitGroup.isEmpty() ? "연대 계열 교체" : "대대 추가 / 변경", x + 13, y + 38, left - 70, TEXT);
            button("대대 목록 닫기", "뒤로", x + left - 49, y + 34, 43, 18, true, () -> {
                if (!supportSlot && selectedSlot % 5 == 0 && !unitGroup.isEmpty()) unitGroup = "";
                else selectedSlot = -1;
                pickerScroll = 0; rebuildWidgets();
            });
            int start = y + 62, cardWidth = (left - 22) / 2, pitch = 40;
            String current = supportSlot ? (selectedSlot < t.support().size() ? t.support().get(selectedSlot) : "") : t.lineUnit(selectedSlot);
            if (!supportSlot && unitGroup.isEmpty()) {
                var groups = List.of("ARMOR", "MOBILE", "INFANTRY").stream()
                        .filter(g -> view.battalions().stream().anyMatch(c -> battalionGroup(c.id()).equals(g))).toList();
                for (int i = 0; i < groups.size(); i++) {
                    String group = groups.get(i), label = switch (group) { case "ARMOR" -> "기갑 대대"; case "MOBILE" -> "기동 대대"; default -> "보병 대대"; };
                    String preferred = switch (group) { case "ARMOR" -> "TANK"; case "MOBILE" -> "MOTORIZED_INFANTRY"; default -> "INFANTRY"; };
                    var candidates = view.battalions().stream().filter(c -> battalionGroup(c.id()).equals(group)).toList();
                    var choice = candidates.stream().filter(c -> c.id().equals(preferred)).findFirst().orElse(candidates.getFirst());
                    int column = selectedSlot / 5;
                    boolean selected = !current.isEmpty() && battalionGroup(current).equals(group);
                    String detail = selected ? "(현재 선택됨)" : (current.isEmpty() ? "추가 비용 " : "교체 비용 ") + 5 * Math.max(1, t.columnSize(column)) + " ★";
                    unitChoice(label, choice.texture(), detail, x + 12 + i % 2 * (cardWidth + 6), start + i / 2 * pitch, cardWidth, true,
                            () -> { unitGroup = group; pickerScroll = 0; rebuildWidgets(); });
                }
            } else {
                var choices = supportSlot ? view.companies().stream().filter(c -> c.id().equals(current) || !t.support().contains(c.id())).toList()
                        : view.battalions().stream().filter(c -> battalionGroup(c.id()).equals(unitGroup)).toList();
                int remove = current.isEmpty() ? 0 : 1, total = choices.size() + remove;
                int rows = Math.max(1, (y + h - 34 - start) / pitch);
                pickerScroll = Math.clamp(pickerScroll, 0, Math.max(0, (total + 1) / 2 - rows));
                for (int i = 0; i < total; i++) {
                    int row = i / 2 - pickerScroll, bx = x + 12 + i % 2 * (cardWidth + 6), by = start + row * pitch;
                    if (row < 0 || row >= rows) continue;
                    var choice = i < remove ? null : choices.get(i - remove);
                    String id = choice == null ? "" : choice.id(), label = choice == null ? "제거" : choice.name();
                    boolean selected = !id.isEmpty() && id.equals(current);
                    boolean enabled = !selected && (choice != null || supportSlot || t.line().size() > 1);
                    String detail = selected ? "(현재 선택됨)" : choice == null ? "제거 비용 5 ★" : current.isEmpty() ? "추가 비용 5 ★" : "교체 비용 " + (!supportSlot && selectedSlot % 5 == 0 && !battalionGroup(current).equals(battalionGroup(id)) ? 5*t.columnSize(selectedSlot/5) : 5) + " ★";

                    unitChoice(label, choice == null ? "production/designer/remove" : choice.texture(), detail, bx, by, cardWidth, enabled, () -> {
                        int slot = selectedSlot; selectedSlot = -1;
                        send(supportSlot ? SUPPORT_SLOT : t.columns() == null ? LINE_SLOT : LINE_COLUMN, id, "", slot);
                    });
                }
            }
        }

        int statsX = x + left + 12, statsWidth = w - left - 21, colWidth = statsWidth / 3;
        String[] groups = {"BASE", "COMBAT", "EQUIPMENT_COST"}, titles = {"기본 능력치", "전투 능력치", "장비 비용"};
        for (int col = 0; col < 3; col++) {
            int sx = statsX + col * colWidth;
            recess(sx, y + 28, colWidth - 1, h - 81);
            rail(sx, y + 28, colWidth - 1, 20);
            if(graphics!=null) {
                float scale=14f/font.lineHeight;
                graphics.pose().pushMatrix(); graphics.pose().translate(sx+3,y+31); graphics.pose().scale(scale);
                graphics.text(font,titles[col],0,0,TEXT);graphics.pose().popMatrix();
            }
            String groupId = groups[col]; int row = 0;
            for (var stat : t.stats().stream().filter(v -> v.group().equals(groupId)).toList()) {
                int sy = y + 51 + row++ * 15 - designerScroll;
                if (sy < y + 48 || sy + 12 > y + h - 58) continue;
                art("stats/" + (stat.id() == null ? "" : stat.id().toLowerCase(Locale.ROOT)), sx + 2, sy - 2, 11, 11);
                int valueWidth = Math.min(colWidth / 2, Math.max(35, font.width(statValue(stat))));
                leftText(stat.name(), sx + 15, sy, colWidth - valueWidth - 19, MUTED);
                cellText(statValue(stat), sx + colWidth - valueWidth - 3, sy, valueWidth, font.lineHeight, TEXT, true);
                tip(stat.name() + ": " + statValue(stat), sx, sy - 2, colWidth - 1, 15);
            }
            if (col == 2) for (var need : t.equipment().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                int sy = y + 51 + row++ * 15 - designerScroll;
                if (sy < y + 48 || sy + 15 > y + h - 58) continue;
                var e = equipment(need.getKey());
                leftText(familyName(e), sx + 3, sy, colWidth - 41, MUTED); cellText(integer(need.getValue()), sx + colWidth - 37, sy, 34, font.lineHeight, TEXT, true);
                tip(familyName(e) + ": " + need.getValue(), sx, sy - 2, colWidth - 1, 18);
            }
        }
        double cost = 0; boolean known = true;
        for (var need : t.equipment().entrySet()) {
            var e = equipmentById.get(need.getKey());
            if (e == null) known = false; else cost += e.cost() * need.getValue();
        }
        art("keywords/stat_common_build_cost_ic", statsX, y + h - 49, 14, 14);
        leftText("예상 생산 비용: " + (known ? ic(cost) + " IC" : "—"), statsX + 18, y + h - 45, statsWidth - 20, TEXT);
        art("hud/army_experience", x + 10, y + h - 46, 14, 14);
        leftText(integer(view.economy().armyXp()), x + 28, y + h - 44, left - 30, 0xFFFFFFFF);
        tip("보유 육군 경험치", x + 10, y + h - 47, left - 12, 17);
        if (graphics == null) {
            int bx = x + w / 2 - 65, by = y + h - 27;
            var save = new UiPanelButton("새 편제로 저장 · 기존 사단과 훈련 편제 유지", bx, by, 130, 21,
                    () -> send(SAVE_TEMPLATE, draftName, "", 0)) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    UiMenuStyle.control(g, bx, by, 130, 21, false, false);
                    var label = Component.literal(integer(t.experienceCost())).withStyle(style -> style.withColor(UiTooltips.GOLD))
                            .append(Component.literal(" 저장").withStyle(style -> style.withColor(0xFFFFFF)));
                    int labelWidth = Math.round(font.width(label) * UiText.scale(font));
                    int start = bx + (130 - 17 - labelWidth) / 2;
                    UiAssets.draw(g, "hud/army_experience", start, by + 3, 14, 14);
                    UiText.text(g, font, label, start + 17, by + 5, 0xFFFFFFFF);
                }
            };
            save.active = pending == 0 && !draftName.isBlank() && !t.line().isEmpty() && view.economy().armyXp() >= t.experienceCost();
            save.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal("새 편제로 저장 · 기존 사단과 훈련 편제 유지")));
            addRenderableWidget(save);
        }
    }
    private static String statValue(IndustryView.Stat stat) {
        String value = stat.unit().equals("PEOPLE") ? manpower(stat.value()) : stat.unit().equals("PERCENT") ? percent(stat.value()) : (stat.unit().equals("DAYS") || Set.of("WEIGHT", "COMBAT_WIDTH").contains(stat.id())) ? integer(stat.value()) : decimal(stat.value());
        return value + (stat.unit().equals("KM_PER_HOUR") ? "km/h" : stat.unit().equals("DAYS") ? "일" : "");
    }
    private void selectSlot(int slot, boolean support) {
        selectedSlot = slot; supportSlot = support; pickerScroll = 0;
        String head = support ? "" : view.draft().lineUnit(slot / 5 * 5);
        unitGroup = support || slot % 5 == 0 ? "" : battalionGroup(head); rebuildWidgets();
    }
    private static String battalionGroup(String id) {
        return switch (id) {
            case "TANK", "LIGHT_TANK", "SELF_PROPELLED_ARTILLERY", "SELF_PROPELLED_AIR_DEFENSE", "AMPHIBIOUS_ARMOR", "MOTORIZED_ROCKET_ARTILLERY" -> "ARMOR";
            case "MOTORIZED_MILITIA", "MOTORIZED_INFANTRY", "MECHANIZED_INFANTRY", "LIGHT_MECHANIZED", "AMPHIBIOUS_MECHANIZED", "HEADQUARTERS" -> "MOBILE";
            default -> "INFANTRY";
        };
    }
    private void layoutPicker() {
        if (picker.equals("equipment") || picker.equals("repairs")) { layoutProductionPicker(); return; }
        int w = Math.min(270, width - 16), x = width - pane >= w + 8 ? pane + 4 : (width - w) / 2, y = top + 3;
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
    private void layoutProductionPicker() {
        boolean repair=picker.equals("repairs"), naval=group.equals("navy");
        int w=productionPickerWidth();
        int x=width-pane>=w+4?pane+2:(width-w)/2,y=top+3;
        int end=repair?Math.min(bottom(),y+Math.round(w*600f/444)):bottom();
        if(graphics!=null)UiAssets.nineSlice(graphics,"production/picker/panel",x,y,w,end-y,64,w/(repair?444.0:495.0));
        if(repair) {
            art("production/picker/repair_title",x+3,y+2,w-6,Math.round((w-6)*41f/430));
            heading("수리 대기열",x+18,y+7,w-48,TEXT);
        }
        button("목록 닫기","×",x+w-23,y+2,19,19,true,()->{picker="";rebuildWidgets();});
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
            toggle.active=pending==0;addRenderableWidget(toggle);
        }
        recess(x+7,y+5,45,16);
        art("production/picker/manufacturer",x+11,y+7,12,12);
        cellText("0",x+26,y+5,24,16,GOLD,false);
        tip("군수산업조직 현황 미제공",x+7,y+5,45,16);
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
                    filter.active=pending==0;filter.setTooltip(UiTooltips.create(Component.literal(familyName(model))));addRenderableWidget(filter);
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
        int start=y+49;
        if(naval)start=y+29+Math.max(2,(navalTypes.size()+3)/4)*Math.max(11,Math.round(((w-58)/4)/3f));
        var current=view.lines().stream().filter(l->l.id().equals(switchLine)).findFirst().orElse(null);
        int row=Math.max(34,Math.round((w-12)*80f/460));
        pickerScroll=clampScroll(pickerScroll,items.size(),row,start);
        if(graphics!=null)UiAssets.nineSlice(graphics,"production/picker/list",x+3,start-2,w-6,end-start+1,64,w/495.0);
        for(int i=0;i<items.size();i++) {
            var equipment=items.get(i);int by=start+i*row-pickerScroll;
            if(by<start||by+row>end-3)continue;
            boolean selected=current!=null&&current.equipment().equals(equipment.id());
            pickerEquipment(equipment,x+6,by,w-16,row-1,selected);
        }
        pickerScrollbar(x+w-7,start,5,end-start-4,items.size()*row,pickerScroll);
        if(items.isEmpty())text("생산 가능한 장비가 없습니다.",x+9,start+8,w-18,MUTED);
    }
    private void pickerEquipment(Equipment equipment,int x,int y,int w,int h,boolean selected) {
        var design=dev.hoi.client.ui.UiEquipmentDesigns.productionDesign(equipment);
        if(graphics!=null) {
            productionTip(dev.hoi.client.ui.UiEquipmentDesigns.productionObsoleteTooltip(equipment.outdated()),x+w-20,y+1,17,16);
            productionTip(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("PRODUCTION_CREATE_VARIANT"),x+w-23,y+h-23,19,19);
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
                int labelX=x+Math.round((equipment.naval()?62:206)*scale),right=x+Math.round(365*scale)-3;
                if(equipment.naval()) {
                    pickerRightText(g,UiText.ellipsize(font,equipment.name(),(int)((right-labelX)/UiText.scale(font))),labelX,y+Math.round(6*scale),right-labelX);
                    pickerRightText(g,familyName(equipment),labelX,y+Math.round(22*scale),right-labelX);
                } else {
                    UiText.text(g,font,UiText.ellipsize(font,equipment.name(),Math.max(0,(int)((right-labelX)/UiText.scale(font)))),labelX,y+Math.round(6*scale),TEXT);
                    var lines=font.split(Component.literal(familyName(equipment)),Math.max(1,(int)((right-labelX)/UiText.scale(font))));
                    for(int i=0;i<Math.min(3,lines.size());i++) {
                        UiText.text(g,font,lines.get(i),labelX,y+Math.round(22*scale)+i*UiText.MIN_HEIGHT,TEXT);
                    }
                }
                int costX=x+5,costY=y+h-13,iconSize=10;
                UiAssets.draw(g,equipment.naval()?"production/picker/naval_cost":"keywords/stat_common_build_cost_ic",costX,costY,iconSize,iconSize);
                UiText.cell(g,font,ic(equipment.cost()),costX+iconSize+1,costY+1,Math.round(font.width(ic(equipment.cost()))*UiText.scale(font)),iconSize,GOLD,false);
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
                if(tank||blueprint)UiAssets.draw(g,"production/tank/empty_blueprint",x+imageWidth-13,costY+(iconSize-12)/2,14,12);
                UiAssets.draw(g,"production/picker/obsolete",x+w-16,y+4,9,9);
                UiAssets.draw(g,design.icon(),x+w-23,y+h-23,19,19);
            }
        };
        button.active=pending==0&&!selected&&!blueprint;
        button.setTooltip(UiTooltips.create(Component.literal(equipment.name()+"\n"+familyName(equipment)+"\n"+ic(equipment.cost())+" IC\n"+resources(equipment.resources(),1))));
        if(unfinished)button.setTooltip(UiTooltips.create(Component.literal((equipment.designWarning()==null||equipment.designWarning().isEmpty())?"설계를 열어 모듈을 구성하세요.":equipment.designWarning())));
        if(blueprint)button.setTooltip(UiTooltips.create(Component.literal(Objects.toString(equipment.designWarning(),""))));
        addRenderableWidget(button);
        if(tank)originalButton(design.title(),design.icon(),x+w-23,y+h-23,19,19,true,()->openTank(equipment))
                .setTooltip(UiTooltips.createExact(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("PRODUCTION_CREATE_VARIANT")));
    }
    private static String tankErrors(TankDesignerView view) {
        var missing=view.slots().stream().filter(s->s.required()&&s.selected().isEmpty()).map(TankDesignerView.Slot::name).toList();
        var errors=new ArrayList<String>();
        if(!missing.isEmpty())errors.add(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("REQUIRED_MODULE_EMPTY")+"\n§e"+String.join("\n",missing));
        view.errors().stream().filter(e->!missing.contains(e)).forEach(e->errors.add("§c"+e));
        return String.join("\n\n",errors);
    }
    private boolean tankDesigner() {return view!=null&&view.tankDesigner()!=null;}
    private void openTank(Equipment equipment) {tankSlot="";tankScroll=0;designPresetsOpen=false;presetMessage="";send(TANK_OPEN,equipment.id(),"",0);}
    static String tankValue(TankDesignerView.Stat stat) {
        return dev.hoi.client.ui.UiEquipmentDesigns.statValue(stat,stat.value());
    }
    private static String tankChanges(List<TankDesignerView.Stat> stats) {
        var order=List.of("breakthrough","maximum_speed","armor_value","reliability","fuel_consumption","build_cost_ic");
        return stats.stream().sorted(Comparator.comparingInt(s->order.contains(s.id())?order.indexOf(s.id()):order.size()))
                .map(s->"§f"+dev.hoi.client.ui.UiEquipmentDesigns.statCaption(s.id(),s.name()).strip()+" "+(s.id().equals("reliability")?(s.value()>0?"§a":"§c"):"§e")+tankValue(s)).collect(java.util.stream.Collectors.joining("\n"));
    }
    static String tankUpgradeChanges(TankDesignerView.Upgrade upgrade,int levels) {
        return tankChanges(upgrade.perLevel().stream().map(stat->new TankDesignerView.Stat(stat.id(),stat.name(),stat.value()*levels,stat.percent())).toList());
    }
    private TankDesignerView.UpgradeStep tankUpgradeStep(TankDesignerView.Upgrade upgrade,boolean increase) {
        int amount=(increase?1:-1)*tankStep();
        return upgrade.steps().stream().filter(step->step.amount()==amount).findFirst().orElse(null);
    }
    private String tankUpgradeTooltip(TankDesignerView.Upgrade upgrade,boolean increase) {
        var step=tankUpgradeStep(upgrade,increase);
        if(increase&&upgrade.level()>=upgrade.maximum()&&upgrade.maximum()<20)
            return dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_NEXT_LEVEL_BLOCKED_BY")+"\n"+String.join("\n",upgrade.requirements());
        if(increase&&upgrade.level()>=upgrade.maximum()||!increase&&upgrade.level()==0)
            return dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_ALREADY_AT_"+(increase?"HIGHEST":"LOWEST")+"_LEVEL");
        int levels=step==null?(increase?1:-1):step.level()-upgrade.level();
        var resources=step==null?(increase?upgrade.resourceIncrease():upgrade.resourceDecrease()):step.resources();
        String result=dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_"+(increase?"NEXT":"PREV")+"_UPGRADE_GRANTS")+"\n"+tankUpgradeChanges(upgrade,levels);
        if(!resources.isEmpty())result+="\n\n"+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_RESOURCE_CHANGE")+"\n"+resources.entrySet().stream().map(r->"§f"+resourceName(r.getKey())+": §e"+(r.getValue()>0?"+":"")+integer(r.getValue())).collect(java.util.stream.Collectors.joining("\n"));
        int cost=step==null?1:step.experience();
        String key=cost<0?"EQUIPMENT_UPGRADE_PREV_UPGRADE_REFUND":"EQUIPMENT_UPGRADE_"+(increase?"NEXT":"PREV")+"_UPGRADE_COST";
        String costText=dev.hoi.client.ui.UiEquipmentDesigns.tooltip(key).replace("$XPICON$","£army_experience ").replace("$COST|H0$","§e"+Math.abs(cost)+"§f");
        String sign=increase?"+":"-";
        return result+"\n\n§f"+costText+"\n\n§a클릭§f하여 §e"+sign+"1\n§aCtrl + 클릭§f하여 §e"+sign+"5\n§aShift + 클릭§f하여 §e"+sign+"10";
    }
    private void tankUpgradeButton(TankDesignerView.Upgrade upgrade,boolean increase,int x,int y,int size) {
        if(graphics!=null)return;
        var button=new UiPanelButton(upgrade.name()+(increase?" 증가":" 감소"),x,y,size,size,()->{
            var step=tankUpgradeStep(upgrade,increase);
            send(TANK_UPGRADE,upgrade.id(),"",step==null?Math.clamp(upgrade.level()+(increase?1:-1)*tankStep(),0,upgrade.maximum()):step.level());
        }) {
            private int previousStep;
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiAssets.draw(g,"production/line/"+(increase?"plus":"minus"),x,y,size,size);
                if(previousStep!=tankStep()) {previousStep=tankStep();setTooltip(UiTooltips.createExact(tankUpgradeTooltip(upgrade,increase)));}
            }
        };
        button.active=pending==0&&(increase?upgrade.level()<upgrade.maximum():upgrade.level()>0);
        button.setTooltip(UiTooltips.createExact(tankUpgradeTooltip(upgrade,increase)));addRenderableWidget(button);
    }
    private static String tankStatIcon(String id) {
        return "production/design/stats/"+id;
    }
    private int tankStep() {
        if(factoryStep()==10)return 10;
        long window=minecraft.getWindow().handle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(window,org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)==1||org.lwjgl.glfw.GLFW.glfwGetKey(window,org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL)==1?5:1;
    }
    private static String designerTitle(String branch) {return switch(branch) {case "navy"->"함선 설계사";case "air"->"항공기 설계사";default->"전차 설계사";};}
    private String designBranch() {return !tankDesigner()||view.tankDesigner().chassis().startsWith("modern_tank_")?"army":view.tankDesigner().chassis().startsWith("ship_")?"navy":"air";}
    private String designMission="";
    private void layoutEquipmentDesigner() {
        var d=view.tankDesigner();
        var displayedStats=d.missions().stream().filter(m->m.id().equals(designMission)).map(TankDesignerView.Mission::stats).findFirst().orElse(d.stats());
        var statsById=new HashMap<String,TankDesignerView.Stat>();
        displayedStats.forEach(stat->statsById.putIfAbsent(stat.id(),stat));
        String branch=designBranch(),title=designerTitle(branch);
        boolean armor=branch.equals("army"),navy=branch.equals("navy");
        Double experience=armor?view.economy().armyXp():navy?view.hud().navyExperience():view.hud().airExperience();
        int minY=top+font.lineHeight+8;
        int w=Math.min(Math.min(width-24,Math.max(780,width*5/8)),(height-minY-12)*1092/553);
        int h=w*553/1092,x=(width-w)/2,y=Math.max(minY,(height-h)/2);
        java.util.function.IntUnaryOperator size=n->Math.max(1,n*w/1092);
        java.util.function.IntUnaryOperator px=n->x+n*w/1092,py=n->y+n*w/1092;
        var preview=new HashMap<String,Double>();
        if(tankSlot.isEmpty()&&mouseY>=py.applyAsInt(466)&&mouseY<py.applyAsInt(492))for(int i=0;i<d.upgrades().size();i++) {
            var upgrade=d.upgrades().get(i);int bx=309+i*106;
            if(mouseX>=px.applyAsInt(bx)&&mouseX<px.applyAsInt(bx)+size.applyAsInt(26)&&upgrade.level()>0)(tankUpgradeStep(upgrade,false)==null?upgrade.decrease():tankUpgradeStep(upgrade,false).changes()).forEach(stat->preview.put(stat.id(),stat.value()));
            if(mouseX>=px.applyAsInt(bx)+size.applyAsInt(99)-size.applyAsInt(26)&&mouseX<px.applyAsInt(bx)+size.applyAsInt(99)&&upgrade.level()<upgrade.maximum())(tankUpgradeStep(upgrade,true)==null?upgrade.increase():tankUpgradeStep(upgrade,true).changes()).forEach(stat->preview.put(stat.id(),stat.value()));
        }
        art(armor?"production/tank/panel":"production/design/"+branch+"_panel",x,y,w,h);
        if(graphics!=null) {
            graphics.pose().pushMatrix();graphics.pose().translate(x+w/2,py.applyAsInt(11)+2);graphics.pose().scale(1.7f);
            graphics.text(font,title,-font.width(title)/2,0,TEXT,false);graphics.pose().popMatrix();
        }
        int closeWidth=size.applyAsInt(31),closeHeight=size.applyAsInt(29);
        int closeX=px.applyAsInt(navy?1048:1044)+(size.applyAsInt(navy?37:38)-closeWidth)/2;
        int closeY=py.applyAsInt(navy?4:8)+(size.applyAsInt(navy?38:34)-closeHeight)/2;
        button(title+" 창 닫기","×",closeX,closeY,closeWidth,closeHeight,true,()->send(TANK_CLOSE,"","",0));
        if(tankSlot.isEmpty()&&!designPresetsOpen) {
            if(graphics!=null)UiAssets.nineSlice(graphics,"production/tank/name",px.applyAsInt(24),py.applyAsInt(59),size.applyAsInt(305),size.applyAsInt(29),4,.5);
            art(armor?"production/tank/blueprint_icon":"production/design/controls/"+branch+"_name",px.applyAsInt(25),py.applyAsInt(61),size.applyAsInt(27),size.applyAsInt(26));
            if(graphics==null) {
                tankNameBox=new EditBox(font,px.applyAsInt(54),py.applyAsInt(64)+3,size.applyAsInt(270),size.applyAsInt(23),Component.literal(title+" 이름"));
                tankNameBox.setBordered(false);tankNameBox.setCentered(false);
                tankNameBox.setMaxLength(80);tankNameBox.setValue(tankName);tankNameBox.moveCursorToStart(false);tankNameBox.setResponder(value->tankName=value);addRenderableWidget(tankNameBox);
            }
            String chassis=Optional.ofNullable(equipmentBySource.get(d.chassis())).map(ScreenIndustry::familyName).orElse(d.name());
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
                    tip(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_MODULE_SLOT_NOT_AVAILABLE"),bx,by,bw,bh);
                }
            }
            cellText(chassis,px.applyAsInt(31),py.applyAsInt(201)+4,size.applyAsInt(478),size.applyAsInt(22),TEXT,false);
            for(int i=0;i<d.upgrades().size();i++) {
                var u=d.upgrades().get(i);int bx=309+i*106;
                cellText(u.name(),px.applyAsInt(bx),py.applyAsInt(445),size.applyAsInt(99),size.applyAsInt(20),TEXT,false);
                int controlX=px.applyAsInt(bx),controlY=py.applyAsInt(466),controlWidth=size.applyAsInt(99),buttonSize=size.applyAsInt(26);
                cellText(Integer.toString(u.level()),controlX+buttonSize,controlY,controlWidth-2*buttonSize,buttonSize,TEXT,false);
                String requirements="§f"+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_CURRENT_MAX_LEVEL").replace("$LEVEL|H0$","§e"+u.maximum())+"\n"+String.join("\n",u.requirements());
                tankUpgradeButton(u,false,controlX,controlY,buttonSize);
                tankUpgradeButton(u,true,controlX+controlWidth-buttonSize,controlY,buttonSize);
                productionTip(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_UPGRADE_EACH_LEVEL_STAT_HEADER")+"\n"+tankUpgradeChanges(u,1)+"\n\n"+requirements,controlX+buttonSize,controlY,controlWidth-2*buttonSize,buttonSize);
            }
            }
            originalButton("파일로 저장","production/design/controls/save_preset",px.applyAsInt(24),py.applyAsInt(396),size.applyAsInt(22),size.applyAsInt(22),!tankName.isBlank(),()->saveDesignPreset());
            originalButton("저장된 설계 보기","production/design/controls/open_preset",px.applyAsInt(24),py.applyAsInt(419),size.applyAsInt(22),size.applyAsInt(22),true,()->openDesignPresets());
            if(!presetMessage.isEmpty())tip(presetMessage,px.applyAsInt(24),py.applyAsInt(396),size.applyAsInt(22),size.applyAsInt(45));
            int designButtonY=py.applyAsInt(372),designButtonSize=size.applyAsInt(70);
            String upgradeTexture=navy?"production/design/controls/upgrade":"production/tank/refit";
            String manufacturerTexture=armor?"production/tank/manufacturer":"production/design/controls/"+branch+"_manufacturer";
            var refit=originalButton("최신 특성으로 개량 · 군수산업체 특성 개량은 아직 지원하지 않습니다.",upgradeTexture,px.applyAsInt(408),designButtonY,size.applyAsInt(40),designButtonSize,false,()->{},upgradeTexture+"_disabled");
            var manufacturer=originalButton(title+" · 군수산업체 선택은 아직 지원하지 않습니다.",manufacturerTexture,px.applyAsInt(454),designButtonY,designButtonSize,designButtonSize,false,()->{},manufacturerTexture+"_disabled");
            if(refit!=null)refit.setTooltip(UiTooltips.createExact(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("INDUSTRIAL_ORG_TOOLTIP_UPGRADE_ALREADY_ASSIGNED_MIO_HEADER")+"\n"+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("INDUSTRIAL_ORG_TOOLTIP_NO_UPGRADE_ALREADY_ASSIGNED_MIO")+"\n\n"+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("INDUSTRIAL_ORG_TOOLTIP_UPGRADE_ALREADY_ASSIGNED_MIO_DELAYED")+"\n\n§7군수산업체 특성 개량은 아직 지원하지 않습니다."));
            if(manufacturer!=null)manufacturer.setTooltip(UiTooltips.createExact(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("INDUSTRIAL_ORG_TOOLTIP_HEADER")+"\n\n§7군수산업체 선택은 아직 지원하지 않습니다."));
        } else if(designPresetsOpen) {
            recess(px.applyAsInt(16),py.applyAsInt(48),size.applyAsInt(508),size.applyAsInt(446));
            leftText("저장된 설계",px.applyAsInt(30),py.applyAsInt(65),size.applyAsInt(330),TEXT);
            button("저장된 설계 닫기","뒤로",px.applyAsInt(414),py.applyAsInt(58),size.applyAsInt(94),size.applyAsInt(34),true,()->{designPresetsOpen=false;tankScroll=0;rebuildWidgets();});
            if(!presetMessage.isEmpty())leftText(presetMessage,px.applyAsInt(30),py.applyAsInt(101),size.applyAsInt(478),MUTED);
            int rowHeight=31,rows=Math.max(1,(py.applyAsInt(490)-py.applyAsInt(140))/rowHeight);
            tankScroll=Math.clamp(tankScroll,0,Math.max(0,designPresets.size()-rows));
            for(int i=tankScroll;i<Math.min(designPresets.size(),tankScroll+rows);i++) {
                var preset=designPresets.get(i);
                button("저장된 설계 불러오기: "+preset.name(),preset.name(),px.applyAsInt(30),py.applyAsInt(140)+(i-tankScroll)*rowHeight,size.applyAsInt(478),27,true,()->{
                    presetApplying=true;designPresetsOpen=false;tankScroll=0;send(TANK_PRESET,"",preset.json(),0);
                });
            }
        } else {
            int left=size.applyAsInt(508),footer=py.applyAsInt(496);
            recess(px.applyAsInt(16),py.applyAsInt(48),left,footer-py.applyAsInt(48));
            if(graphics!=null&&armor)graphics.fill(px.applyAsInt(18),py.applyAsInt(50),px.applyAsInt(522),footer-2,0xFF030803);
            var slot=Objects.requireNonNull(designSlots.get(tankSlot));
            String selectedTexture=slot.choices().stream().filter(part->part.id().equals(slot.selected())).map(TankDesignerView.Part::texture).findFirst().orElse("production/designer/add");
            recess(px.applyAsInt(30),py.applyAsInt(61),size.applyAsInt(73),size.applyAsInt(45));
            float headerCenter=py.applyAsInt(61)+size.applyAsInt(45)/2f;
            int card=size.applyAsInt(241),gap=size.applyAsInt(6),rowHeight=size.applyAsInt(69),cardHeight=size.applyAsInt(63);
            int backHeight=size.applyAsInt(40),backWidth=backHeight*118/29;
            int backX=px.applyAsInt(30)+2*card+gap-backWidth;
            moduleBackButton(backX,Math.round(headerCenter-backHeight/2f),backWidth,backHeight);
            int removeSize=size.applyAsInt(27),removeX=backX-gap-removeSize;
            if(graphics!=null) {
                float labelScale=Math.min(1.5f,((armor?backX:removeX)-gap-px.applyAsInt(112))/(float)font.width("모듈 선택"));
                graphics.pose().pushMatrix();graphics.pose().translate(px.applyAsInt(112),headerCenter-font.lineHeight*labelScale/2);graphics.pose().scale(labelScale);
                graphics.text(font,"모듈 선택",0,0,TEXT);graphics.pose().popMatrix();
            }
            if(!armor&&!slot.selected().isEmpty())originalButton("장착 모듈 제거","production/designer/remove",removeX,Math.round(headerCenter-removeSize/2f),removeSize,removeSize,true,()->{send(TANK_MODULE,slot.id(),"",0);tankSlot="";});
            var categories=dev.hoi.client.ui.UiEquipmentDesigns.categoryIds(d.chassis(),slot.id()).stream()
                    .filter(category->slot.choices().stream().anyMatch(part->dev.hoi.client.ui.UiEquipmentDesigns.moduleCategory(part.id()).equals(category))).toList();
            boolean categorized=armor&&!Set.of("suspension_type_slot","armor_type_slot","engine_type_slot","special_type_slot_5","turret_type_slot").contains(slot.id());
            var parts=slot.choices().stream().filter(part->!categorized||dev.hoi.client.ui.UiEquipmentDesigns.moduleCategory(part.id()).equals(tankCategory)).toList();
            if(armor)parts=parts.stream().sorted(Comparator.comparing((TankDesignerView.Part part)->switch(part.id()) {
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
                List.of("lg_attack","lg_armor_piercing","hg_attack","hg_armor_piercing","torpedo_attack","anti_air_attack","armor_value","sub_attack","carrier_size"),
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
                    if(stat==null)stat=new TankDesignerView.Stat(key,dev.hoi.client.ui.UiEquipmentDesigns.statName(key),0,false);
                }
                int rowTop=py.applyAsInt(73+row*20),rowHeight=py.applyAsInt(93+row*20)-rowTop,by=rowTop+1;
                int valueWidth=size.applyAsInt(key.equals("maximum_speed")?82:57);
                String icon=armor?tankStatIcon(key):"production/design/stats/"+key;
                if(graphics!=null)UiAssets.drawAlignedContent(graphics,icon,icon,px.applyAsInt(sx+2),by+(rowHeight-size.applyAsInt(18))/2f,size.applyAsInt(19),size.applyAsInt(18));
                if(stat==null&&!armor) {
                    String label=dev.hoi.client.ui.UiEquipmentDesigns.statName(key).replaceAll("£[^ ]+\\s*","").replace(":","").strip();
                    tankStatText(label,px.applyAsInt(sx+24),by,column-valueWidth-size.applyAsInt(25),rowHeight,false);
                    tankStatText("—",px.applyAsInt(sx)+column-valueWidth,by,valueWidth,rowHeight,true);
                    tip(label+": 현재 설계 데이터에 없는 수치입니다.",px.applyAsInt(sx),by,column,rowHeight);
                    continue;
                }
                if(stat==null)stat=new TankDesignerView.Stat(key,dev.hoi.client.ui.UiEquipmentDesigns.statName(key),0,false);
                String label=stat.name().replaceAll("£[^ ]+\\s*","").replace(":","").strip();
                tankStatText(label,px.applyAsInt(sx+24),by,column-valueWidth-size.applyAsInt(25),rowHeight,false);
                double change=preview.getOrDefault(key,0.0);
                var displayed=new TankDesignerView.Stat(stat.id(),stat.name(),stat.value()+change,stat.percent());
                tankStatText(tankValue(displayed),px.applyAsInt(sx)+column-valueWidth,by,valueWidth-(change==0?0:size.applyAsInt(10)),rowHeight,true);
                if(graphics!=null&&change!=0) {
                    boolean beneficial=dev.hoi.client.ui.UiEquipmentDesigns.beneficial(key,change);
                    UiText.cell(graphics,font,change>0?"▲":"▼",px.applyAsInt(sx)+column-size.applyAsInt(10),by,size.applyAsInt(10),rowHeight,beneficial?0xFF009F03:0xFFFF3232,false);
                }
                productionTip(dev.hoi.client.ui.UiEquipmentDesigns.statTooltip(stat),px.applyAsInt(sx),by,column,rowHeight);
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
            var environments=navy?List.of("water_shallow_sea","water_deep_ocean","water_fjords"):
                    List.of("plains","forest","hills","mountain","urban","jungle","marsh","desert","fort","river","amphibious");
            for(int i=0;i<environments.size();i++) {
                String id=environments.get(i),label=dev.hoi.client.ui.UiEquipmentDesigns.adjusterName(id);
                int columns=armor?Math.min(5,environments.size()-i/5*5):environments.size();
                int rowWidth=columns*84+(columns-1)*16;
                int adjusterX=539+(536-rowWidth)/2+i%5*100;
                int adjusterY=armor?288+i/5*70:adjustmentY;
                designerAdjustment(id,label,adjusterX,adjusterY,px,n->py.applyAsInt(n)-(armor?2:0),size,false,()->{});
            }
        } else {
            int rows=Math.max(1,(d.missions().size()+4)/5),rowHeight=Math.min(76,(488-adjustmentY)/rows);
            java.util.function.IntUnaryOperator adjustmentSize=n->Math.max(1,size.applyAsInt(n)*Math.min(72,rowHeight)/72);
            for(int i=0;i<d.missions().size();i++) {
                var mission=d.missions().get(i);
                String tooltip=mission.name()+"\n"+tankChanges(mission.stats())+"\n클릭하여 이 임무의 능력치 보기";
                designerAdjustment(mission.id(),tooltip,562+i%5*100,adjustmentY+i/5*rowHeight,px,py,adjustmentSize,true,()->{designMission=designMission.equals(mission.id())?"":mission.id();rebuildWidgets();});
            }
            if(!designMission.isEmpty())tip("선택한 임무의 능력치입니다. 같은 임무를 다시 누르면 기본 능력치로 돌아갑니다.",px.applyAsInt(540),py.applyAsInt(47),size.applyAsInt(520),size.applyAsInt(202));
        }
        int footerHeight=size.applyAsInt(30),footerCenter=py.applyAsInt(509)+size.applyAsInt(27)/2,footerY=footerCenter-footerHeight/2;
        var reset=button("설계 초기화","초기화",px.applyAsInt(20),footerY,size.applyAsInt(112),footerHeight,true,()->send(TANK_RESET,"","",0));
        if(reset!=null)reset.setTooltip(UiTooltips.createExact(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_RESET_DESC")));
        productionTip(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("CURRENT_EXPERIENCE"),px.applyAsInt(286),footerY,size.applyAsInt(90),footerHeight);
        if(graphics!=null)UiText.iconText(graphics,font,"hud/"+branch+"_experience",experience==null?"—":integer(experience),px.applyAsInt(286),footerY+1,size.applyAsInt(90),footerHeight,size.applyAsInt(20),TEXT);
        if(graphics==null) {
            int bx=px.applyAsInt(395),by=footerY,bw=size.applyAsInt(122),bh=footerHeight;
            String label=d.errors().isEmpty()?"설계 저장 · "+(armor?"육군":navy?"해군":"공군")+" 경험치 "+d.experience():tankErrors(d);
            var save=new UiPanelButton(label,bx,by,bw,bh,()->send(TANK_SAVE,tankName,"",0)) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    UiMenuStyle.control(g,bx,by,bw,bh,false,false);
                    tankExperienceCaption(g,"저장",d.experience(),bx+bw/2,by+(bh-UiText.MIN_HEIGHT)/2);
                }
            };
            save.active=pending==0&&d.errors().isEmpty()&&experience!=null&&experience>=d.experience();
            String saveTip=dev.hoi.client.ui.UiTooltipText.sections(
                    dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SAVE_DESC").replace("$XPICON$",branch+"_experience").replace("$XPCOST|Y0$","§e"+d.experience()+"§f"),
                    dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SAVE_DESC_NO_CHANGE_TO_PARENT"));
            if(!d.errors().isEmpty())saveTip=tankErrors(d);
            else if(experience==null||experience<d.experience())saveTip=dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_NOT_ENOUGH_XP");
            save.setTooltip(UiTooltips.createExact(saveTip));addRenderableWidget(save);
        }
        int resourceX=px.applyAsInt(540),resourceWidth=size.applyAsInt(97);
        if(graphics!=null)UiAssets.drawContent(graphics,"production/tank/resources",resourceX,footerY,resourceWidth,footerHeight);
        var designResources=List.of("OIL","ALUMINUM","RUBBER","TUNGSTEN","IRON","CHROMIUM","COAL").stream().filter(id->d.resources().getOrDefault(id,0.0)>0).toList();
        int resourceStep=Math.min(26,82/Math.max(1,designResources.size())),resourceIcon=size.applyAsInt(Math.min(22,resourceStep));
        int resourceLeft=resourceX+(resourceWidth-(Math.max(0,designResources.size()-1)*size.applyAsInt(resourceStep)+resourceIcon))/2;
        for(int i=0;i<designResources.size();i++)art(resourceArt(designResources.get(i)),resourceLeft+i*size.applyAsInt(resourceStep),footerCenter-resourceIcon/2,resourceIcon,resourceIcon);
        tip("이 설계를 생산하는 데 사용되는 §e자원",resourceX,footerY,resourceWidth,footerHeight);
        if(graphics!=null) {
            String label="생산 비용:",cost=ProtocolNumbers.fixed(d.cost()+preview.getOrDefault("build_cost_ic",0.0),2);
            float textScale=UiText.scale(font);
            int labelWidth=Math.round(font.width(label)*textScale),costWidth=Math.round(font.width(cost)*textScale),iconSize=size.applyAsInt(17),gap=2;
            int costLeft=px.applyAsInt(1060)-labelWidth-iconSize-costWidth-2*gap,textY=footerCenter-Math.round(font.lineHeight*textScale/2);
            UiText.text(graphics,font,label,costLeft,textY,TEXT);
            UiAssets.draw(graphics,"keywords/stat_common_build_cost_ic",costLeft+labelWidth+gap,footerCenter-iconSize/2,iconSize,iconSize);
            UiText.text(graphics,font,cost,costLeft+labelWidth+iconSize+2*gap,textY+1,TEXT);
            var costStat=statsById.get("build_cost_ic");
            if(costStat!=null)productionTip(dev.hoi.client.ui.UiEquipmentDesigns.statTooltip(costStat),costLeft,footerY,px.applyAsInt(1060)-costLeft,footerHeight);
        }
        if(!view.message().isEmpty())tip(view.message(),px.applyAsInt(140),py.applyAsInt(508),size.applyAsInt(137),size.applyAsInt(30));
        if(armor&&tankSlot.isEmpty()&&!designPresetsOpen)tankRoleMenu(px.applyAsInt(24),py.applyAsInt(95),size.applyAsInt(305),size.applyAsInt(35));
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
        toggle.active=pending==0;addRenderableWidget(toggle);
        if(!tankRolesOpen)return;
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget!=toggle
                &&widget.getX()<x+w&&widget.getX()+widget.getWidth()>x&&widget.getY()<y+4*(h+3)&&widget.getY()+widget.getHeight()>y+h)widget.active=false;
        var roles=List.of("modern","anti_air","flame");
        for(int i=0;i<roles.size();i++) {
            String role=roles.get(i),key="tank_designer_"+(i==0?"modern_tank_chassis":role);
            String name=dev.hoi.client.ui.UiEquipmentDesigns.tooltip(key);
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
            entry.setTooltip(UiTooltips.createExact(selected?"§e"+name:dev.hoi.client.ui.UiEquipmentDesigns.tooltip(key+"_role_disallowed")+"\n\n§7현재 전차 설계에서는 이 역할로 저장할 수 없습니다."));
            addRenderableWidget(entry);
        }
    }
    private void saveDesignPreset() {
        try {presetStore.save(EquipmentDesignPreset.from(view.tankDesigner(),tankName));presetMessage="파일로 저장했습니다.";}
        catch(java.io.IOException|IllegalArgumentException error){presetMessage=error.getMessage();}
        if(minecraft.player!=null)minecraft.player.sendSystemMessage(Component.literal(presetMessage));
        rebuildWidgets();
    }
    private void openDesignPresets() {
        try {
            designPresets=presetStore.load().stream().filter(p->p.chassis().equals(view.tankDesigner().chassis())).toList();
            presetMessage=designPresets.isEmpty()?"이 차체·함체·기체의 저장된 설계가 없습니다.":"같은 이름으로 저장하면 기존 설계를 교체합니다.";
        } catch(java.io.IOException error){designPresets=List.of();presetMessage=error.getMessage();}
        tankSlot="";tankScroll=0;designPresetsOpen=true;rebuildWidgets();
    }
    private void branchDesignContents(TankDesignerView d,boolean navy,java.util.function.IntUnaryOperator px,java.util.function.IntUnaryOperator py,java.util.function.IntUnaryOperator size) {
        String branch=navy?"navy":"air";
        var equipment=equipmentBySource.get(d.chassis());
        String chassis=equipment==null?d.name():equipment.familyName();
        recess(px.applyAsInt(24),py.applyAsInt(95),size.applyAsInt(305),size.applyAsInt(35));
        if(navy) {
            art("production/design/controls/navy_role",px.applyAsInt(29),py.applyAsInt(97),size.applyAsInt(32),size.applyAsInt(31));
            leftText(chassis,px.applyAsInt(68),py.applyAsInt(98)+Math.round((size.applyAsInt(28)-font.lineHeight*UiText.scale(font))/2),size.applyAsInt(256),TEXT);
        } else {
            String role=dev.hoi.client.ui.UiEquipmentDesigns.role(d),label=dev.hoi.client.ui.UiEquipmentDesigns.roleName(role);
            int rx=px.applyAsInt(24),ry=py.applyAsInt(95),rw=size.applyAsInt(305),rh=size.applyAsInt(35);
            if(graphics==null) {
                var roleButton=new UiPanelButton("항공기 역할 · 주무장 선택",rx,ry,rw,rh,()->{tankSlot="fixed_main_weapon_slot";tankScroll=0;rebuildWidgets();}) {
                    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                        UiMenuStyle.recess(g,rx,ry,rw,rh);
                        UiAssets.draw(g,"production/design/roles/"+role,rx+size.applyAsInt(5),ry+size.applyAsInt(4),size.applyAsInt(32),rh-size.applyAsInt(8));
                        var lines=font.split(Component.literal(label),Math.max(1,(int)((rw-size.applyAsInt(46))/UiText.scale(font))));
                        int lineHeight=UiText.MIN_HEIGHT;
                        for(int i=0;i<Math.min(2,lines.size());i++)UiText.text(g,font,lines.get(i),rx+size.applyAsInt(43),ry+(rh-Math.min(2,lines.size())*lineHeight)/2+i*lineHeight,TEXT);
                    }
                };
                roleButton.active=pending==0&&d.slots().stream().anyMatch(slot->slot.id().equals("fixed_main_weapon_slot"));
                roleButton.setTooltip(UiTooltips.create(Component.literal(label+"\n주무장을 선택하면 항공기 역할이 변경됩니다.")));addRenderableWidget(roleButton);
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
            tip(category.equals("capital")?"주력함":category.equals("submarine")?"잠수함":"보조함",cx,cy,cw,ch);
        }
        int bx=px.applyAsInt(16),by=py.applyAsInt(196),bw=size.applyAsInt(508),bh=size.applyAsInt(248);
        art("production/design/"+branch+"_blueprint_bg",bx,by,bw,bh);
        if(graphics!=null) {
            String texture="production/design/blueprints/"+d.chassis();
            if(!UiAssets.draw(graphics,texture+"_"+view.country().toLowerCase(Locale.ROOT),bx,by,bw,bh))UiAssets.draw(graphics,texture,bx,by,bw,bh);
        }
        cellText(chassis,px.applyAsInt(31),py.applyAsInt(201)+4,size.applyAsInt(478),size.applyAsInt(22),TEXT,false);
        List<String> upper=navy?d.slots().stream().map(TankDesignerView.Slot::id).filter(id->id.endsWith("_custom_slot")).toList():
                List.of("fixed_main_weapon_slot","fixed_auxiliary_weapon_slot_1","fixed_auxiliary_weapon_slot_2","fixed_auxiliary_weapon_slot_3","fixed_auxiliary_weapon_slot_4","fixed_auxiliary_weapon_slot_5");
        List<String> lower=navy?d.slots().stream().map(TankDesignerView.Slot::id).filter(id->!id.endsWith("_custom_slot")).toList():
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
        if(graphics!=null)dev.hoi.client.ui.UiEquipmentDesigns.overlays(graphics,d,hovered,bx,by,bw,bh);
        if(!navy) {
            art("production/design/controls/silhouette_bg",px.applyAsInt(451),py.applyAsInt(146),size.applyAsInt(69),size.applyAsInt(46));
            art("production/design/roles/"+dev.hoi.client.ui.UiEquipmentDesigns.role(d),px.applyAsInt(456),py.applyAsInt(150),size.applyAsInt(59),size.applyAsInt(38));
        }

    }
    private void designerAdjustment(String id,String label,int x,int y,java.util.function.IntUnaryOperator px,java.util.function.IntUnaryOperator py,java.util.function.IntUnaryOperator size,boolean enabled,Runnable action) {
        int bx=px.applyAsInt(x),by=py.applyAsInt(y),bw=size.applyAsInt(84),bh=size.applyAsInt(67);
        if(graphics!=null)return;
        int centering=designBranch().equals("army")?3:0;
        int frameW=size.applyAsInt(64),frameH=size.applyAsInt(57);
        double frameScale=Math.min(frameW/70.0,frameH/62.0);
        int fitW=(int)Math.round(70*frameScale),fitH=(int)Math.round(62*frameScale);
        int frameX=bx+size.applyAsInt(7+centering)+(frameW-fitW)/2,frameY=by+size.applyAsInt(5)+(frameH-fitH)/2;
        var adjustment=view.tankDesigner().adjustments().stream().filter(a->a.id().equals(id)).findFirst().orElse(null);
        double[] values=adjustment==null?null:new double[]{adjustment.movement(),adjustment.attack(),adjustment.defense()};
        var button=new UiPanelButton("설계 조정치 · "+id,bx,by,bw,bh,action) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiAssets.draw(g,"production/design/adjusters/"+id,bx+size.applyAsInt(19+centering),by+size.applyAsInt(10),size.applyAsInt(40),size.applyAsInt(40));
                UiAssets.draw(g,"production/design/controls/adjuster_frame",bx+size.applyAsInt(7+centering),by+size.applyAsInt(5),size.applyAsInt(64),size.applyAsInt(57));
                String hovered="";
                if(values!=null)for(int j=0;j<3;j++) {
                    String icon=j==0?(designBranch().equals("navy")?"ship_movement":"movement"):j==1?"attack":"defense";
                    String path="production/design/controls/"+icon,state=values[j]>1e-8?"positive":values[j]<-1e-8?"negative":"neutral";
                    float iconSize=21/2f;
                    float inset=(size.applyAsInt(15)-iconSize)/2f;
                    float iconX=j<2?frameX+fitW*(j==0?12.5f:35f)/70f-iconSize/2f+(j==0&&!designBranch().equals("army")?1:0):bx+size.applyAsInt(52+centering)+inset;
                    float iconY=j<2?frameY+fitH*49.5f/62f-iconSize/2f:by+size.applyAsInt(42)+inset+1;
                    UiAssets.drawAlignedContent(g,path+"_"+state,path+"_neutral",iconX,iconY,iconSize,iconSize);
                    if(mx>=iconX&&mx<iconX+iconSize&&my>=iconY&&my<iconY+iconSize)hovered=designerAdjustmentLine(j,values[j],false);
                }
                if(isHovered())setTooltip(UiTooltips.createExact(hovered.isEmpty()?designerAdjustmentTooltip(label,values):hovered));
            }
        };
        button.active=enabled&&pending==0;button.setTooltip(UiTooltips.createExact(designerAdjustmentTooltip(label,values)));addRenderableWidget(button);
    }
    private String designerAdjustmentLine(int index,double value,boolean total) {
        String key=switch(index){case 0->"MOVEMENT";case 1->"ATTACK";default->"DEFENCE";};
        String token=switch(index){case 0->"movement_texticon";case 1->"attack_texticon";default->"defence_texticon";};
        String amount=String.format(java.util.Locale.ROOT,"%.1f%%",(total?1+value:value)*100);
        return "§f£"+token+" "+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("STAT_ADJUSTER_"+key)+" "+(total?"§e":value<0?"§c":"§a")+(!total&&value>0?"+":"")+amount;
    }
    private String designerAdjustmentTooltip(String label,double[] values) {
        if(values==null||designBranch().equals("air"))return label;
        var lines=new ArrayList<String>();lines.add("§e"+label);
        for(int i=0;i<3;i++)lines.add(designerAdjustmentLine(i,values[i],true)+"\n§f"+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("BASE")+" §e"+String.format(java.util.Locale.ROOT,"%.1f%%",(1+values[i])*100));
        return String.join("\n\n",lines);
    }

    private void tankExperienceCaption(GuiGraphicsExtractor g,String label,int amount,int centerX,int y) {
        var cost=dev.hoi.client.ui.UiKeywordIcons.decorateLines(UiTooltips.styled(Component.literal("§f£"+designBranch()+"_experience §e"+amount),false),false);
        String prefix=label+" ";float scale=UiText.scale(font);
        int left=centerX-Math.round((font.width(prefix)+font.width(cost))*scale/2);
        UiText.text(g,font,prefix,left,y,TEXT);
        UiText.text(g,font,cost,left+Math.round(font.width(prefix)*scale),y+1,TEXT);
    }
    private void tankStatText(String value,int x,int y,int width,int height,boolean right) {
        tankStatText(value,x,y,width,height,right,1);
    }
    private void tankStatText(String value,int x,int y,int width,int height,boolean right,float multiplier) {
        if(graphics==null)return;
        var caption=Component.literal(value);
        if(!right)caption.withStyle(net.minecraft.ChatFormatting.BOLD);
        float scale=Math.min(Math.min(1.1f,Math.max(.8f,this.width/1100f))*multiplier,(float)width/Math.max(1,font.width(caption)));
        scale=Math.min(scale,(float)Math.max(1,height-2)/font.lineHeight);
        graphics.pose().pushMatrix();graphics.pose().translate(x+(right?width-font.width(caption)*scale:0),y+(height-(font.lineHeight-(right?2:0))*scale)/2);graphics.pose().scale(scale);
        graphics.text(font,caption,0,0,0xFF13170E,false);graphics.pose().popMatrix();
    }
    private void tankSlotButton(TankDesignerView.Slot slot,int x,int y,int w,int h) {
        String texture=slot.choices().stream().filter(p->p.id().equals(slot.selected())).map(TankDesignerView.Part::texture).findFirst().orElse("production/designer/add");
        String label=dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_NAME_TOOLTIP").replace("$NAME$",slot.name())+"\n";
        var selected=slot.choices().stream().filter(p->p.id().equals(slot.selected())).findFirst();
        label+=selected.isPresent()?dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_MODULE_TOOLTIP").replace("$NAME$",selected.get().name()):dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_EMPTY_TOOLTIP");
        String description=dev.hoi.client.ui.UiEquipmentDesigns.moduleDescription(slot.selected());
        if(!description.isEmpty())label+="\n"+description;
        var moduleStats=dev.hoi.client.ui.UiEquipmentDesigns.moduleStats(slot.selected());
        String moduleEffects=tankModuleStatLines(moduleStats.stream().filter(stat->!stat.id().equals("build_cost_ic")).toList());
        if(!moduleEffects.isEmpty())label+="\n\n"+moduleEffects;
        String moduleCost=tankModuleStatLines(moduleStats.stream().filter(stat->stat.id().equals("build_cost_ic")).toList());
        if(!moduleCost.isEmpty())label+="\n\n"+moduleCost;
        label+="\n\n"+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_ALLOWED_CATEGORIES_TOOLTIP")+"\n§f"+dev.hoi.client.ui.UiEquipmentDesigns.slotCategories(view.tankDesigner().chassis(),slot.id());
        if(slot.required())label+="\n\n"+(slot.selected().isEmpty()?"§c":"§f")+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_SLOT_REQUIRED_TOOLTIP");
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
        button.setTooltip(UiTooltips.createExact(label));addRenderableWidget(button);
    }
    private void moduleBackButton(int x,int y,int w,int h) {
        if(graphics!=null)return;
        var button=new UiPanelButton("모듈 선택 뒤로",x,y,w,h,()->{
            if(!tankCategory.isEmpty())tankCategory="";else tankSlot="";
            tankScroll=0;rebuildWidgets();
        }) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                UiAssets.drawContent(g,"production/design/controls/module_back",x,y,w,h);
                float labelScale=Math.min(1.5f,h/(float)font.lineHeight);
                g.pose().pushMatrix();g.pose().translate(x+(w-font.width("뒤로")*labelScale)/2,y+(h-font.lineHeight*labelScale)/2);g.pose().scale(labelScale);
                g.text(font,"뒤로",0,0,TEXT);g.pose().popMatrix();
            }
        };
        button.active=pending==0;addRenderableWidget(button);
    }
    private void tankCategoryButton(String category,int x,int y,int w,int h) {
        if(graphics!=null)return;
        String label=dev.hoi.client.ui.UiEquipmentDesigns.categoryName(category);
        var button=new UiPanelButton("모듈 분류: "+label,x,y,w,h,()->{tankCategory=category;tankScroll=0;rebuildWidgets();}) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                moduleCard(g,dev.hoi.client.ui.UiEquipmentDesigns.categoryTexture(category),label,x,y,w,h,false);
            }
        };
        button.active=pending==0;button.setTooltip(UiTooltips.createExact("§e"+label));addRenderableWidget(button);
    }
    private void moduleCard(GuiGraphicsExtractor g,String texture,String label,int x,int y,int w,int h,boolean cost) {
        UiAssets.nineSlice(g,"production/design/controls/module_card",x,y,w,h,12,w/244.0);
        int iconWidth=w*72/244,iconHeight=h*44/66,textX=x+w*85/244,textWidth=w*153/244;
        UiAssets.drawContent(g,texture,x+w*6/244,y+(h-iconHeight)/2,iconWidth,iconHeight);
        var lines=font.split(Component.literal(label),Math.max(1,(int)(textWidth/UiText.scale(font))));
        int count=Math.min(2,lines.size()),areaHeight=cost?h*38/66:h;
        for(int i=0;i<count;i++)UiText.text(g,font,lines.get(i),textX,y+(areaHeight-count*UiText.MIN_HEIGHT)/2+i*UiText.MIN_HEIGHT,TEXT);
    }
    private static String tankModuleStatLines(List<TankDesignerView.Stat> stats) {
        return stats.stream().map(stat->"§f"+dev.hoi.client.ui.UiEquipmentDesigns.statCaption(stat.id(),stat.name()).strip()+" §e"+tankValue(stat)).collect(java.util.stream.Collectors.joining("\n"));
    }
    private String tankModuleTooltip(TankDesignerView.Part part) {
        String label="§e"+part.name(),description=dev.hoi.client.ui.UiEquipmentDesigns.moduleDescription(part.id());
        if(!description.isEmpty())label+="\n\n§f"+description;
        var stats=dev.hoi.client.ui.UiEquipmentDesigns.moduleStats(part.id());
        var effects=stats.stream().filter(stat->!stat.id().equals("build_cost_ic")).toList();
        if(!effects.isEmpty())label+="\n\n"+tankModuleStatLines(effects);
        var costs=stats.stream().filter(stat->stat.id().equals("build_cost_ic")).toList();
        if(!costs.isEmpty())label+="\n\n"+tankChanges(costs);
        var resources=dev.hoi.client.ui.UiEquipmentDesigns.moduleResources(part.id());
        if(!resources.isEmpty())label+="\n\n"+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("PRODUCTION_RESOURCES_PER_FACTORY")+"\n"+resources.entrySet().stream().map(entry->"  §f"+resourceName(entry.getKey())+": §e"+integer(entry.getValue())).collect(java.util.stream.Collectors.joining("\n"));
        return label+"\n\n§f£"+designBranch()+"_experience "+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("EQUIPMENT_DESIGNER_"+designBranch().toUpperCase(Locale.ROOT)+"_XP_COST").replace("$COST|Y0$","§e"+(dev.hoi.client.ui.UiEquipmentDesigns.moduleExperience(part.id())>0?dev.hoi.client.ui.UiEquipmentDesigns.moduleExperience(part.id()):part.experience()));
    }
    private void tankPartButton(TankDesignerView.Part part,TankDesignerView.Slot slot,int x,int y,int w,int h) {
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
        button.setTooltip(UiTooltips.createExact(tankModuleTooltip(part)));addRenderableWidget(button);
    }

    private void pickerRightText(GuiGraphicsExtractor g,String value,int x,int y,int width) {
        String clipped=font.plainSubstrByWidth(value,Math.max(1,(int)(width/UiText.scale(font))));
        UiText.text(g,font,clipped,x+width-Math.round(font.width(clipped)*UiText.scale(font)),y,TEXT);
    }
    private void pickerScrollbar(int x,int y,int w,int h,int total,int offset) {
        if(graphics==null||h<=0)return;
        graphics.fill(x,y,x+w,y+h,0xFF151515);
        if(total>h) {
            int size=Math.max(12,h*h/total),position=(h-size)*offset/Math.max(1,total-h);
            rail(x,y+Math.min(h-size,position),w,size);
        }
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
    private void layoutRepairPorts(int x,int y,int w,int end) {
        var repairs=view.navalRepairs();
        if(repairs==null){text("수리 현황을 확인할 수 없습니다.",x+9,y+36,w-18,MUTED);return;}
        art("production/summary/dockyard_icon_with_wrench",x+8,y+28,16,16);
        var decrease=originalButton("수리 조선소 배정 감소 · 자동 배정 중","recruitment/decrease",x+26,y+29,13,13,false,()->{});
        cellText(Integer.toString(repairs.availableDockyards()),x+39,y+28,22,16,TEXT,false);
        var increase=originalButton("수리 조선소 배정 증가 · 자동 배정 중","recruitment/increase",x+61,y+29,13,13,false,()->{});
        var assignmentTip=UiTooltips.create(Component.literal("함선 수리에 배정할 수 있는 §e조선소§r의 최대 숫자입니다.\n\n§7현재 서버는 수리 조선소를 자동 배정합니다."));
        if(decrease!=null)decrease.setTooltip(assignmentTip);
        if(increase!=null)increase.setTooltip(assignmentTip);
        tip("함선 수리에 배정할 수 있는 §e조선소§r의 최대 숫자입니다.\n\n§7현재 서버는 수리 조선소를 자동 배정합니다.",x+8,y+27,67,17);
        var represented=new HashSet<String>();repairs.ports().forEach(port->represented.addAll(port.ships()));
        var expand=originalButton("수리 목록 펼치기/접기","production/picker/"+(expandedRepairPorts.isEmpty()?"collapse":"expand"),x+81,y+28,38,15,true,()->{
            if(expandedRepairPorts.isEmpty()) {
                repairs.ports().forEach(port->expandedRepairPorts.add(port.id()));
                repairs.ships().stream().filter(ship->!represented.contains(ship.id()))
                        .forEach(ship->expandedRepairPorts.add(ship.id()));
            } else expandedRepairPorts.clear();
            pickerScroll=0;rebuildWidgets();
        });
        if(expand!=null)expand.setTooltip(UiTooltips.create(Component.literal("§a클릭§r하여 모든 대기열 펴기/접기")));
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
            long returning=portShips.stream().filter(ship->repairState(ship).equals("귀항 중")).count();
            String counts="수리 대기 §e"+waiting+"§r척\n수리 중 §e"+port.activeShips()+"§r척 · 귀항 중 §e"+returning+"§r척";
            int py=by;by+=row;
            if(py>=start&&py+row<=end-3) {
                if(graphics==null) {
                    var listTip=UiTooltips.create(Component.literal(counts+"\n\n§a클릭§r하여 함선 대기열 목록을 전환합니다."));
                    var policyTip=UiTooltips.create(Component.literal("§e수리 우선순위 선택§r\n\n우선순위가 더 높은 해군기지는 수리를 위한 조선소를 먼저 할당받습니다.\n\n§a클릭§r하여 우선순위를 바꾸십시오. §a우클릭§r하여 조선소의 수리 여부를 활성화하거나 비활성화하십시오.\n\n§7현재 서버는 자동 배정만 지원하며 위 우선순위 조작은 지원하지 않습니다."));
                    var shipsTip=UiTooltips.create(Component.literal("이 해군기지에서 함선 §e"+waiting+"§r척이 수리를 위해 대기 중입니다.\n\n"+counts));
                    var capacityTip=UiTooltips.create(Component.literal("§e"+waiting+"§r척의 함정이 수리 대기 중입니다.\n현재 수리 중: §e"+port.activeShips()+"§r척\n이 §e해군기지§r는 동시에 최대 §e"+(port.capacity()==0?"—":port.capacity())+"§r척의 수리 작업을 수행할 수 있습니다."));
                    var button=new UiPanelButton("수리 항구: "+port.name(),x+7,py,w-18,Math.round((w-18)*33f/400),()->{if(!expandedRepairPorts.add(port.id()))expandedRepairPorts.remove(port.id());rebuildWidgets();}) {
                        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                            UiAssets.draw(g,"production/picker/repair_port",getX(),py,getWidth(),getHeight());
                            int column=(mx-getX())*400/Math.max(1,getWidth());
                            setTooltip(column>=35&&column<60?policyTip:column>=200&&column<270?shipsTip:column>=270?capacityTip:listTip);
                            UiAssets.draw(g,"production/picker/repair_state",getX()+Math.round(getWidth()*35f/400),py+2,Math.round(getWidth()*22f/400),getHeight()-4);
                            UiText.text(g,font,font.plainSubstrByWidth(port.name(),Math.max(1,(int)((getWidth()*140f/400)/UiText.scale(font)))),getX()+Math.round(getWidth()*60f/400),py+4,TEXT);
                            UiAssets.draw(g,"production/picker/ships",getX()+getWidth()/2,py+3,15,10);
                            productionText(g,Long.toString(waiting),getX()+getWidth()*59/100,py,w*9/100,row-2,TEXT);
                            UiAssets.draw(g,"production/summary/dockyard_icon",getX()+getWidth()*68/100,py+2,12,12);
                            productionText(g,port.capacity()==0?"—":port.activeShips()+" / "+port.capacity(),getX()+getWidth()*75/100,py,w*18/100,row-2,TEXT);
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
                leftText(UiText.ellipsize(font,ship.name(),(int)(labelWidth/UiText.scale(font))),label,sy+2,labelWidth,TEXT);
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
        pickerScrollbar(x+w-7,start,4,end-start-4,total,pickerScroll);
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
        int w=Math.min(316,Math.min(width-16,(height-top-8)*527/437)),h=w*437/527;
        int x=(width-w)/2,y=Math.max(top+4,(height-h)/2);
        java.util.function.IntUnaryOperator size=n->Math.max(1,n*w/527);
        java.util.function.IntUnaryOperator px=n->x+n*w/527,py=n->y+n*w/527;
        art("trade/offer",x,y,w,h);
        int titleX=px.applyAsInt(105),titleY=py.applyAsInt(22);
        int titleWidth=px.applyAsInt(424)-titleX,titleHeight=py.applyAsInt(64)-titleY;
        cellText(p.name(),titleX,titleY,titleWidth,titleHeight,TEXT,false);
        int flagX=px.applyAsInt(425),frameWidth=size.applyAsInt(78),frameHeight=size.applyAsInt(49);
        int flagWidth=size.applyAsInt(70),flagHeight=size.applyAsInt(44);
        art(UiMenuBar.flagTexture(p.id()),flagX+(frameWidth-flagWidth)/2,titleY+(titleHeight-flagHeight)/2,flagWidth,flagHeight);
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
            art(UiMenuBar.flagTexture(buyer.id()),bx,by,size.applyAsInt(40),size.applyAsInt(27));
            tip(buyer.name(),bx,by,size.applyAsInt(40),size.applyAsInt(27));
        }
        art("construction/summary/civilian_factory",px.applyAsInt(70),py.applyAsInt(275)+1,size.applyAsInt(26),size.applyAsInt(22));
        paragraph(Component.literal("수입량: ").withColor(TEXT).append(Component.literal(integer(Math.min(export,tradeAmount*perFactory))).withColor(GOLD))
                .append(Component.literal("\n할당될 민간공장: ").withColor(TEXT)).append(Component.literal(Integer.toString(tradeAmount)).withColor(GOLD)),
                px.applyAsInt(104),py.applyAsInt(265)+1,size.applyAsInt(235),size.applyAsInt(40));
        var neededButton=originalButton("필요한 수입량 설정","trade/need",px.applyAsInt(350),py.applyAsInt(262)+1,size.applyAsInt(38),size.applyAsInt(38),p.route(),()->{
            double delivered=view.trades().stream().filter(v->v.importing()&&v.partner().equals(partner)&&v.resource().equals(resource)).mapToDouble(Trade::delivered).sum();
            tradeAmount=view.resources().stream().filter(v->v.id().equals(resource)).mapToInt(v->ScreenTradeAmounts.neededTradeFactories(v.demand(),v.available(),delivered,perFactory,limit)).findFirst().orElse(0);
            rebuildWidgets();
        });
        if(neededButton!=null)neededButton.setTooltip(UiTooltips.create(Component.literal("현재 자원 수요를 충족하기 위해 구매해야 할 양을 설정하십시오.")));
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
            var slider=new net.minecraft.client.gui.components.AbstractSliderButton(sx.applyAsInt(71),sy.applyAsInt(8),sx.applyAsInt(333)-sx.applyAsInt(71),sy.applyAsInt(34)-sy.applyAsInt(8),Component.literal("수입량 조절"),ScreenTradeAmounts.tradeSliderPosition(tradeAmount,maxImport,perFactory)) {
                @Override protected void updateMessage(){setMessage(Component.literal("수입량 조절"));}
                @Override protected void applyValue(){
                    tradeAmount=Math.clamp(ScreenTradeAmounts.tradeSliderFactories(value,maxImport,perFactory),0,limit);
                    value=ScreenTradeAmounts.tradeSliderPosition(tradeAmount,maxImport,perFactory);
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
                    if(active&&(event.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT||event.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT)) {
                        value=ScreenTradeAmounts.tradeSliderPosition(Math.clamp(tradeAmount+(event.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT?1:-1),0,limit),maxImport,perFactory);
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
        var lines=font.split(value,Math.max(1,(int)(width/UiText.scale(font))));
        int count=Math.min(lines.size(),height/(UiText.MIN_HEIGHT+1));
        for(int i=0;i<count;i++)UiText.text(graphics,font,lines.get(i),x,y+i*(UiText.MIN_HEIGHT+1),TEXT);
        if(count<lines.size())tip(value.getString(),x,y,width,height);
    }

    private void send(IndustryProtocol.Action action, String item, String other, int amount) {
        if (closed || pending > 0 && action != CLOSE) return;
        boolean passive = action == REFRESH;
        if (!passive) pending = 100;
        transport.accept(new IndustryProtocol.Request(action, token, view == null ? 0 : view.revision(), item, other, amount));
        if (!passive) rebuildWidgets();
    }
    @Override public void tick() {
        if (pending == 0 && view != null && view.draft() != null && openingDesigner) {
            InputSidebarMovement.release(minecraft); designer = true; openingDesigner = false; draftName = view.draft().name(); rebuildWidgets();
        }
        if (pending > 0 && --pending == 0) {
            send(view == null ? OPEN : REFRESH, "", "", 0);
        } else if (++refresh % 60 == 0 && pending == 0) send(view == null ? OPEN : REFRESH, "", "", 0);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (y < top && view != null) { hudScroll = UiMenuBar.scroll(width, view.hud(), hudScroll, horizontal, vertical); return true; }
        if (tankDesigner()) tankScroll -= (int) vertical;
        else if (designer && selectedSlot >= 0) pickerScroll -= (int) vertical;
        else if (designer) designerScroll -= (int) (vertical * 25);
        else if (dockedProductionPicker() && x < pane) scroll -= (int)(vertical*productionRowHeight());
        else if (!picker.isEmpty()) pickerScroll -= (int) (vertical * 55);
        else if (modal()) return super.mouseScrolled(x, y, horizontal, vertical);
        else if (tab == MenuTab.RECRUITMENT && x >= pane) secondScroll -= (int) (vertical * 40);
        else scroll -= (int) (vertical * (tab == MenuTab.PRODUCTION ? productionRowHeight() : tab == MenuTab.RECRUITMENT ? (compact() ? 85 : 93) : tab == MenuTab.LOGISTICS ? 68 : tradeTab == 0 ? 35 : 34));
        rebuildWidgets(); return true;
    }
    @Override public void removed() {
        InputSidebarMovement.release(minecraft);
        if (ScreenDialogCoordinator.suspending()) return;
        closed = true; selectingDeployment = false; dev.hoi.client.map.MapSceneRenderer.clearDeployment();
        transport.accept(new IndustryProtocol.Request(CLOSE, token, view == null ? 0 : view.revision(), "", "", 0));
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (!modal() && (event.button() == 0 || event.button() == 1) && view != null && pending == 0) {
            for (var slot : deploymentLocations) if (event.x() >= slot.x() && event.x() < slot.x() + slot.width() && event.y() >= slot.y() && event.y() < slot.y() + slot.height()) {
                locationRecruit = slot.recruit();
                selectingDeployment = event.button() == 0;
                dev.hoi.client.map.MapSceneRenderer.clearDeployment();
                send(selectingDeployment ? SELECT_LOCATION : LOCATION, slot.recruit(), "", 0);
                return true;
            }
            if (selectingDeployment && event.y() > top && minecraft.player != null) {
                if (event.button() == 1) {selectingDeployment = false; dev.hoi.client.map.MapSceneRenderer.clearDeployment(); rebuildWidgets(); return true;}
                if (!minecraft.options.getCameraType().isFirstPerson()) return true;
                var matrix = minecraft.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
                var ray = matrix.transformProject(new org.joml.Vector3f((float)(event.x() / width * 2 - 1), (float)(1 - event.y() / height * 2), 1)).normalize();
                selectingDeployment = false; dev.hoi.client.map.MapSceneRenderer.clearDeployment();
                pending = 100;
                transport.accept(new IndustryProtocol.Request(LOCATION_AT, token, view.revision(), locationRecruit, "", 0, ray.x, ray.y, ray.z));
                rebuildWidgets();return true;
            }
        }
        return super.mouseClicked(event, twice);
    }
    @Override public int unitHudLeft(){return tab==MenuTab.RECRUITMENT?pane+3+rightWidth():pane;}
    @Override public boolean allowsMovement() { return !modal(); }
    @Override public boolean keyPressed(KeyEvent e) { return !modal() && InputSidebarMovement.consumes(minecraft, e) || super.keyPressed(e); }
    @Override public boolean keyReleased(KeyEvent e) { return !modal() && InputSidebarMovement.consumes(minecraft, e) || super.keyReleased(e); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private Equipment equipment(String id) { return Objects.requireNonNull(equipmentById.get(id)); }
    private int factoryStep() {
        long window = minecraft.getWindow().handle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == 1
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == 1 ? 10 : 1;
    }
    private static String familyName(Equipment e) { return e.familyName() == null ? e.name() : e.familyName(); }
    private Template template(String id) { return Objects.requireNonNull(templatesById.get(id)); }
    private String locationName(String id) { if (id.isEmpty()) return "장소 미지정"; return view.locations().stream().filter(l -> l.id().equals(id)).map(Choice::name).map(n -> n.replace("수도 · ", "").split(" · ", 2)[0]).findFirst().orElse(id); }
    private String resourceName(String id) { return view.resources().stream().filter(r -> r.id().equals(id)).map(Resource::name).findFirst().orElse(id); }
    private String unitName(String id, boolean support) { return (support ? view.companies() : view.battalions()).stream().filter(c -> c.id().equals(id)).map(Choice::name).map(n -> n.replace("수도 · ", "").split(" · ", 2)[0]).findFirst().orElse(id); }
    private String unitTexture(String id, boolean support) { return (support ? view.companies() : view.battalions()).stream().filter(c -> c.id().equals(id)).map(Choice::texture).findFirst().orElse("menu/recruitment"); }
    private String resources(Map<String,Double> resources, int count) {
        return resources.entrySet().stream().map(e -> resourceName(e.getKey()) + " " + decimal(e.getValue() * count)).reduce((a, b) -> a + " · " + b).orElse("자원 소모 없음");
    }
    private String equipmentList(Map<String,Long> need, Map<String,Long> current) {
        return need.entrySet().stream().map(e -> familyName(equipment(e.getKey())) + ": " + current.getOrDefault(e.getKey(), 0L) + " / " + e.getValue()).reduce((a, b) -> a + "\n" + b).orElse("필요 장비 없음");
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
    private static String integer(double n) { return ProtocolNumbers.fixed(n == 0 ? 0 : n, 0); }
    private static String decimal(double n) { return String.format(Locale.ROOT, "%,.1f", n); }
    private static String percent(double n) { return integer(n * 100) + "%"; }
    private static String manpower(double n) { return UiMenuBar.number(n); }
    private static String amount(Double n) { return n == null ? "알 수 없음" : decimal(n) + " 십억"; }
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
        String display = font.plainSubstrByWidth(value, Math.max(1, (int)(max / UiText.scale(font))));
        UiText.text(graphics, font, display, x, y, color);
        if (!display.equals(value)) tip(value, x, y - 1, max, 11);
    }
    private void text(String value, int x, int y, int max, int color) {
        if (graphics == null) return;
        float scale=dev.hoi.client.ui.UiText.scale(font);
        String display=font.plainSubstrByWidth(value,Math.max(1,(int)(max/scale)));
        UiText.cell(graphics, font, display, x, y, max, UiText.MIN_HEIGHT, color, false);
        if(!display.equals(value))tip(value,x,y-1,max,11);
    }

    private void productionTip(String value,int x,int y,int w,int h) {
        if(graphics!=null&&!background&&mouseX>=x&&mouseX<x+w&&mouseY>=y&&mouseY<y+h)
            UiTooltips.drawExact(graphics,font,value,mouseX,mouseY);
    }
    private void tip(String value, int x, int y, int w, int h) {
        if (graphics != null && !background && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h)
            UiTooltips.draw(graphics, font, value, mouseX, mouseY);
    }
    private void bar(int x, int y, int w, int h, double value, int color) {
        if (graphics == null) return;
        graphics.fill(x, y, x + w, y + h, 0xFF0B100D);
        graphics.fill(x, y, x + (int) (w * Math.clamp(value, 0, 1)), y + h, color);
    }
    private UiMenuButton button(String accessible, String label, int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (graphics != null) return null;
        var button = new UiMenuButton(accessible, x, y, w, h, action).caption(label);
        button.active = enabled && pending == 0; button.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(label.equals("×") || label.equals("X") ? "닫기" : accessible)));
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
    private UiPanelButton originalButton(String name, String texture, int x, int y, int w, int h, boolean enabled, Runnable action, String disabledTexture) {
        if (graphics != null) return null;
        var button = new UiPanelButton(name, x, y, w, h, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                UiAssets.draw(g,!active&&disabledTexture!=null?disabledTexture:texture,x,y,w,h);
            }
        };
        button.active = enabled && pending == 0;
        button.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(name.contains(" · ×") ? name.substring(name.indexOf(" · ×") + 3) + " · 다시 누르면 ×1" : name)));
        addRenderableWidget(button);
        return button;
    }
    private void divisionSlot(String name, String texture, int x, int y, int w, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new UiPanelButton(name, x, y, w, 22, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                UiMenuStyle.recess(g, x, y, w, 22);
                UiAssets.draw(g, "production/designer/slot", x + 1, y + 1, w - 2, 20);
                UiAssets.draw(g, texture, x + 2, y + 2, w - 4, 18);
            }
        };
        button.active = enabled && pending == 0;
        button.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(name)));
        addRenderableWidget(button);
    }
    private void unitChoice(String name, String texture, String detail, int x, int y, int w, boolean enabled, Runnable action) {
        if (graphics != null) return;
        var button = new UiPanelButton(name, x, y, w, 36, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                UiMenuStyle.control(g, x, y, w, 36, isHoveredOrFocused(), !active);
                if (!texture.isEmpty()) UiAssets.draw(g, texture, x + 4, y + 9, 28, 21);

                int tw = Math.max(1, w - 40), center = x + 35 + tw / 2;
                g.centeredText(font, font.plainSubstrByWidth(name, tw), center, y + 6, TEXT);
                if (detail.endsWith(" ★")) {
                    int split = detail.lastIndexOf(' ', detail.length() - 3);
                    String amount = detail.substring(split + 1, detail.length() - 2);
                    var costLabel = Component.literal(amount).withColor(UiTooltips.GOLD);
                    int start = center - (font.width(costLabel) + 13) / 2;
                    g.text(font, costLabel, start, y + 21, 0xFFFFFFFF);
                    UiAssets.draw(g, "hud/army_experience", start + font.width(costLabel) + 2, y + 19, 11, 11);
                } else g.centeredText(font, font.plainSubstrByWidth(detail, tw), center, y + 21, 0xFF000000 | UiTooltips.GOOD);
            }
        };
        button.active = enabled && pending == 0;
        button.setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(name)));
        addRenderableWidget(button);
    }
}
