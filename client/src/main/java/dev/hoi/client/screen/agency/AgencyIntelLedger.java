package dev.hoi.client.screen.agency;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.client.ui.tooltip.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.*;
import java.util.function.Consumer;

public final class AgencyIntelLedger {
    private static final Map<String, String> RESEARCH = Map.of(
        "보병", "infantry", "지상 & 항공 지원", "support", "포", "artillery", "기갑", "armour",
        "해군", "naval", "공군", "air_techs", "산업", "industry", "공학", "electronics", "해군 지원 장비", "mtgnavalsupport");
    private static final List<String> RESEARCH_ORDER = List.of("산업", "공학", "보병", "지상 & 항공 지원", "기갑", "포", "해군", "해군 지원 장비", "공군");
    private static final List<String> RESOURCES = List.of("석유", "알루미늄", "고무", "텅스텐", "강철", "크롬", "에너지");
    private final CountryView.Report report;
    private final AgencyIntelDomain domain;
    private final float scale;
    private int top, bottom;
    private final int viewportTop, viewportBottom;
    private final long openedAt;
    private final List<Cell> cells = new ArrayList<>();
    private final List<Art> art = new ArrayList<>();
    private final UiDiplomacyAssets assets;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final Map<AbstractWidget, String> tips = new LinkedHashMap<>();
    private String selectedSection;
    private final Map<String, Integer> offsets = new HashMap<>();
    private CountryView.Section dragging;
    private boolean draggingBody;
    private int history;
    private record Cell(MenuView.Entry entry, int x, int y, int width, int height, boolean left) {}
    private record Art(String icon, int x, int y, int width, int height, boolean disabled) {}
    public AgencyIntelLedger(CountryView.Report report, AgencyIntelDomain domain, int pane, int top, int bottom, long openedAt, UiDiplomacyAssets assets) {
        this.report = report;
        this.domain = domain;
        this.scale = pane / 550f;
        this.top = top;
        this.viewportTop = top;
        this.viewportBottom = bottom;
        this.bottom = Math.max(bottom, top + at(domain == AgencyIntelDomain.CIVILIAN ? 775 : 675));
        this.openedAt = openedAt;
        this.assets = assets;
        selectedSection = domain == AgencyIntelDomain.ARMY ? "templates" : "graph_industry";
        if (domain == AgencyIntelDomain.CIVILIAN) {
            for (int i = 0; i < RESOURCES.size(); i++) {
                image(source("resources_strip", i), 46 + i * 69, 10, 27, 27);
                add(RESOURCES.get(i), 31 + i * 69, 36, 56, 24, false);
            }
            String[] names = {"총 공장", "가용 인력", "사용 가능한 연료", "총 수송선", "트럭 비축량", "열차 비축량", "보급 충족"};
            String[] icons = {source("industrial_capacity_icon", 0), source("manpower_icon", 0), source("fuel_state", 0), source("topbar_convoys", 0), source("intel_ledger_truck_icon", 0), source("intel_ledger_train_icon", 0), source("equipment_icon", 0)};
            for (int i = 0; i < names.length; i++) {
                int x = 30 + i * 50;
                image(source("vanilla_diplo_unity_bg", 0), x, 101, 42, 62);
                image(icons[i], x + 3, 105, 36, 25);
                add(names[i], x + 1, 130, 40, 31, false);
            }
            String[] factories = {"민간공장", "군수공장", "조선소"};
            String[] factoryIcons = {"endgame_civ_industry_button", "endgame_industry_button", "endgame_naval_industry_button"};
            for (int i = 0; i < factories.length; i++) {
                int y = 190 + i * 60;
                image(source("generic_text_bg_108", 0), 40, y, 108, 27);
                image(source(factoryIcons[i], 0), 25, y - 10, 51, 52);
                add(factories[i], 80, y, 65, 27, false);
                field("손상된 " + factories[i] + " 비율", source("building_status_overlay", 2), 150, y, 60, 20);
            }
            field("무역에 사용 중인 민간공장", "", 415, 190, 88, 0);
            String[] production = {"군사 장비 생산에 사용 중인 군수공장", "기갑 장비에 생산에 사용 중인 군수공장", "비행기 생산에 사용 중인 군수공장", "주력함 생산에 사용 중인 조선소", "보조함 생산에 사용 중인 조선소", "다른 함선 생산에 사용 중인 조선소"};
            String[] productionIcons = {"eq", "armor", "air", "capital", "navy", "other"};
            for (int i = 0; i < production.length; i++) field(production[i], source("intel_ledger_" + productionIcons[i] + "_icon", 0), 225 + i % 3 * 95, 250 + i / 3 * 60, 88, 25);
        } else if (domain == AgencyIntelDomain.ARMY) {
            image(source("generic_text_bg_108", 0), 50, 37, 108, 27);
            image(source("endgame_army_button", 0), 30, 25, 51, 52);
            add("총 사단 수", 85, 37, 70, 27, false);
            field("총 특수부대 수", source("special_forces_icon", 2), 200, 37, 88, 25);
            field("배치한 병력", source("manpower_icon", 0), 300, 37, 108, 25);
            field("배치한 육군 장비", source("equipment_icon", 0), 420, 37, 88, 25);
        } else if (domain == AgencyIntelDomain.NAVY) {
            image(source("endgame_navy_button", 0), 30, 33, 51, 52);
            add("함대 수", 90, 37, 190, 20, true);
            add("태스크 포스 수", 90, 57, 190, 20, true);
            field("배치한 인력", source("manpower_icon", 0), 280, 43, 108, 25);
            field("총 함선 수", source("repair_ships_icon", 0), 400, 43, 88, 25);
        } else {
            image(source("endgame_air_button", 0), 30, 33, 51, 52);
            add("총 항공기 수", 90, 47, 240, 27, true);
            field("배치한 공군 인력", source("manpower_icon", 0), 380, 43, 108, 25);
        }
        int i = 0;
        for (String name : RESEARCH_ORDER) for (var entry : report.entries()) if (entry.name().equals(name)) {
            int y = Math.round((this.bottom - top) / scale) - 65, x = 70 + i++ * 80;
            String icon = name.equals("해군 지원 장비") ? "diplomacy/tfrkp/gfx_mtgnavalsupportfolder_icon" : source(RESEARCH.get(name) + "_folder_icon", 0);
            art.add(new Art(icon, x, y, 91, 61, entry.value().equals("정보가 충분하지 않음")));
            cells.add(new Cell(entry, x, y, 91, 61, false));
        }
    }
    public static String source(String name, int frame) { return "intel/source/gfx_" + name + "/frame_" + frame; }
    private int at(int value) { return Math.round(value * scale); }
    private void add(String name, int x, int y, int width, int height, boolean left) {
        report.entries().stream().filter(entry -> entry.name().equals(name)).findFirst()
            .ifPresent(entry -> {
                var header = report.sections().stream().filter(section -> section.id().equals("resources")).flatMap(section -> section.rows().stream()).filter(row -> row.header() && row.name().equals(name)).findFirst();
                var displayed = header.isEmpty() ? entry : new MenuView.Entry(entry.name(), header.get().value(), entry.detail(), entry.progress(), entry.icon());
                cells.add(new Cell(displayed, x, y, width, height, left));
            });
    }
    private void image(String icon, int x, int y, int width, int height) { art.add(new Art(icon, x, y, width, height, false)); }
    private void field(String name, String icon, int x, int y, int width, int iconWidth) {
        if (report.entries().stream().noneMatch(entry -> entry.name().equals(name))) return;
        image(source("generic_text_bg_" + width, 0), x, y, width, 27);
        if (!icon.isEmpty()) image(icon, x + 2, y, iconWidth, 27);
        add(name, x + iconWidth + 2, y, width - iconWidth - 5, 27, false);
    }
    public void addHoverRegions(Consumer<AbstractWidget> consumer) {
        Consumer<AbstractWidget> add = widget -> { widgets.add(widget); widget.visible = widget.getY() >= viewportTop && widget.getBottom() <= viewportBottom; consumer.accept(widget); };
        if (domain == AgencyIntelDomain.ARMY) {
            for (int i = 0; i < 2; i++) {
                String id = i == 0 ? "templates" : "stockpiles";
                String label = UiCommandLabels.text(i == 0 ? "ARMY_LEDGER_ARMIES" : "ARMY_LEDGER_EQUIPMENT", "");
                var button = new UiPanelButton(label, at(15 + 250 * i), top + at(82), at(250), at(43), () -> selectedSection = id) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        UiAssets.draw(g, source("tab_intel_ledger_2", selectedSection.equals(id) ? 0 : 1), getX(), getY(), getWidth(), getHeight());
                        UiText.centered(g, net.minecraft.client.Minecraft.getInstance().font, label, getX(), getY(), getWidth(), getHeight(), UiMenuStyle.TEXT);
                    }
                };
                button.setTooltip(null); add.accept(button);
            }
        } else if (domain == AgencyIntelDomain.CIVILIAN) {
            int i = 0;
            String[] icons = {"hud/factories", "hud/convoys", source("wing_types_own", 1), source("intel_ledger_truck_icon", 0), source("intel_ledger_train_icon", 0)};
            for (var section : report.sections()) {
                if (!section.id().startsWith("graph_")) continue;
                String icon = i < icons.length ? icons[i] : "";
                var button = new UiPanelButton(section.id(), at(34), top + at(360 + i++ * 35), at(40), at(32), () -> selectedSection = section.id()) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        UiAssets.draw(g, source("strategicair_mission_btn_bg", selectedSection.equals(section.id()) ? 1 : 0), getX(), getY(), getWidth(), getHeight());
                        if (!icon.isEmpty()) UiAssets.draw(g, icon, getX() + at(6), getY() + at(4), at(28), at(24));
                    }
                };
                tips.put(button, section.tooltip()); add.accept(button);
            }
            String[] periods = {"3_MONTHS", "1_YEAR", "3_YEARS"};
            String[] labels = {"3개월", "1년", "3년"};
            for (int index = 0; index < periods.length; index++) {
                int period = index;
                String key = "LEDGER_CIV_LINECHART_DETAILS_" + periods[index];
                String label = UiCommandLabels.text(key, labels[index]);
                var button = new UiPanelButton(label, at(172 + index * 80), top + at(520), at(76), at(31), () -> history = period) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        UiAssets.draw(g, source("chart_history_level_button", history == period ? 1 : 0), getX(), getY(), getWidth(), getHeight());
                        UiText.centered(g, net.minecraft.client.Minecraft.getInstance().font, label, getX(), getY(), getWidth(), getHeight(), UiMenuStyle.TEXT);
                    }
                };
                add.accept(button);
            }
        }
        for (var cell : cells) {
            var hover = new UiHoverRegion(cell.entry().name(), at(cell.x()), top + at(cell.y()), at(cell.width()), at(cell.height()));
            tips.put(hover, cell.entry().detail());
            add.accept(hover);
        }
    }
    public String detailAt(double x, double y) {
        if (y < viewportTop || y >= viewportBottom) return "";
        for (var tip : tips.entrySet()) if (Tooltips.contains(tip.getKey(), x, y)) return tip.getValue();
        for (var section : report.sections()) {
            if (section.id().equals("ideas") && x >= at(20) && x < at(20 + ideaWidth()) && y >= ideaY() && y < ideaY() + at(59 + (ideaRows() - 1) * 50)) {
                int column = (int)(x - at(25) + offsets.getOrDefault("ideas", 0)) / Math.max(1, at(60));
                int row = (int)(y - ideaY() - at(5)) / Math.max(1, at(50));
                int index = column * ideaRows() + row;
                if (x >= at(25) && y >= ideaY() + at(5) && index >= 0 && index < section.rows().size()) {
                    var idea = section.rows().get(index);
                    return idea.name() + (idea.detail().isBlank() ? "" : "\n" + idea.detail()) + "\n\n" + section.tooltip();
                }
                return section.tooltip();
            }
            if (section.id().equals("doctrine") && !section.rows().isEmpty() && x >= at(400) && x < at(520) && y >= ideaY() && y < ideaY() + at(109)) return section.tooltip();
        }
        for (var cell : cells) if (x >= at(cell.x()) && x < at(cell.x() + cell.width())
                && y >= top + at(cell.y()) && y < top + at(cell.y() + cell.height())) return cell.entry().detail();
        for (var section : report.sections()) {
            if (!sectionVisible(section)) continue;
            int sy = sectionY(section), sh = sectionHeight(section);
            if (x >= at(20) && x < at(520) && y >= sy && y < sy + sh) {
                if (!section.rows().isEmpty() && !section.id().startsWith("graph_")) {
                    if (types(section)) {
                        int row = (int)((y - sy - at(8)) / Math.max(1, at(28))) * 5 + (int)((x - at(30)) / Math.max(1, at(95)));
                        if (x >= at(30) && y >= sy + at(8) && row >= 0 && row < section.rows().size()) return section.rows().get(row).detail();
                    } else {
                        int rowY = sy + at(8) - offsets.getOrDefault(section.id(), 0);
                        for (var row : section.rows()) {
                            int height = rowHeight(section, row);
                            if (y >= rowY && y < rowY + height) return row.detail();
                            rowY += height;
                        }
                    }
                }
                return section.tooltip();
            }
        }
        return "";
    }
    public String doctrineFolder(double x, double y) {
        if (y < viewportTop || y >= viewportBottom || x < at(415) || x >= at(506) || y < ideaY() + at(25) || y >= ideaY() + at(86)) return "";
        for (var section : report.sections()) if (section.id().equals("doctrine") && section.state().equals("shown") && !section.rows().isEmpty())
            return switch (domain) { case NAVY -> "naval"; case AIR -> "air"; case ARMY -> "land"; default -> ""; };
        return "";
    }
    private boolean sectionVisible(CountryView.Section section) {
        if (Set.of("ideas", "resources", "doctrine").contains(section.id())) return false;
        return domain != AgencyIntelDomain.CIVILIAN && domain != AgencyIntelDomain.ARMY || section.id().equals(selectedSection);
    }
    private int ideaY() { return bottom - at(domain == AgencyIntelDomain.CIVILIAN ? 135 : 175); }
    private int ideaWidth() { return domain == AgencyIntelDomain.CIVILIAN ? 500 : 380; }
    private int ideaRows() { return domain == AgencyIntelDomain.CIVILIAN ? 1 : 2; }
    private int ideaBottom() { return ideaY() + at(domain == AgencyIntelDomain.CIVILIAN ? 74 : 124); }
    private UiScrollBar ideaBar(CountryView.Section section) {
        int width = at(ideaWidth());
        return new UiScrollBar(0, 0, width, width, at(10 + 60 * ((section.rows().size() + ideaRows() - 1) / ideaRows())));
    }
    private int sectionY(CountryView.Section section) {
        return top + at(switch (section.id()) {
            case "templates", "stockpiles" -> 130;
            case "ship_types", "airwing_types" -> 94;
            case "ships", "airwings" -> 172;
            default -> 355;
        });
    }
    private int sectionHeight(CountryView.Section section) {
        return domain == AgencyIntelDomain.CIVILIAN ? at(240) : types(section) ? at(70)
                : Math.max(at(94), bottom - at(225) - sectionY(section));
    }
    private boolean types(CountryView.Section section) { return section.id().equals("ship_types") || section.id().equals("airwing_types"); }
    private int rowHeight(CountryView.Section section, CountryView.Row row) {
        return at(section.id().equals("templates") || section.id().equals("stockpiles") ? 50 : row.header() ? 40 : 65);
    }
    private UiScrollBar bar(CountryView.Section section) {
        int y = sectionY(section), height = sectionHeight(section);
        return new UiScrollBar(at(520) - 9, y, y + height, height, at(16) + section.rows().stream().mapToInt(row -> rowHeight(section, row)).sum());
    }
    public boolean scroll(double x, double y, double amount) {
        for (var section : report.sections()) if (section.id().equals("ideas") && x >= at(20) && x < at(20 + ideaWidth()) && y >= Math.max(viewportTop, ideaY()) && y < Math.min(viewportBottom, ideaBottom()) && ideaBar(section).maximum() > 0) {
            offsets.put("ideas", Math.clamp(offsets.getOrDefault("ideas", 0) - (int)(amount * at(60)), 0, ideaBar(section).maximum()));
            return true;
        }
        if (bodyBar().maximum() > 0 && x >= 0 && x < at(550) && y >= viewportTop && y < viewportBottom) {
            scrollBody(Math.clamp(viewportTop - top - (int)(amount * at(50)), 0, bodyBar().maximum()));
            return true;
        }
        for (var section : report.sections()) if (sectionVisible(section) && !types(section) && x >= at(20) && x < at(520)
                && y >= sectionY(section) && y < sectionY(section) + sectionHeight(section)) {
            offsets.put(section.id(), Math.clamp(offsets.getOrDefault(section.id(), 0) - (int)(amount * at(50)), 0, bar(section).maximum()));
            return true;
        }
        return false;
    }
    public boolean click(double x, double y) {
        if (y < viewportTop || y >= viewportBottom) return false;
        for (var section : report.sections()) if (section.id().equals("ideas") && ideaBar(section).contains(ideaBottom() - y, x - at(20))) {
            var bar = ideaBar(section);
            dragging = bar.arrow(x - at(20)) ? null : section;
            offsets.put("ideas", dragging == null ? bar.step(x - at(20), offsets.getOrDefault("ideas", 0), at(60)) : bar.offset(x - at(20)));
            return true;
        }
        if (bodyBar().contains(x, y)) {
            var bar = bodyBar();
            draggingBody = !bar.arrow(y);
            scrollBody(draggingBody ? bar.offset(y) : bar.step(y, viewportTop - top, at(50)));
            return true;
        }
        for (var section : report.sections()) if (sectionVisible(section) && !types(section) && bar(section).contains(x, y)) {
            var bar = bar(section);
            dragging = bar.arrow(y) ? null : section;
            offsets.put(section.id(), dragging == null ? bar.step(y, offsets.getOrDefault(section.id(), 0), at(50)) : bar.offset(y));
            return true;
        }
        return false;
    }
    private UiScrollBar bodyBar() { return new UiScrollBar(at(532), viewportTop, viewportBottom, viewportBottom - viewportTop, bottom - top); }
    private void scrollBody(int offset) {
        int delta = viewportTop - offset - top;
        top += delta;
        bottom += delta;
        for (var widget : widgets) {
            widget.setY(widget.getY() + delta);
            widget.visible = widget.getY() >= viewportTop && widget.getBottom() <= viewportBottom;
        }
    }
    public boolean drag(double x, double y) {
        if (draggingBody) { scrollBody(bodyBar().offset(y)); return true; }
        if (dragging == null) return false;
        if (dragging.id().equals("ideas")) { offsets.put("ideas", ideaBar(dragging).offset(x - at(20))); return true; }
        offsets.put(dragging.id(), bar(dragging).offset(y)); return true;
    }
    public boolean release() { boolean active = dragging != null || draggingBody; dragging = null; draggingBody = false; return active; }
    private String rowIcon(CountryView.Section section, CountryView.Row row) {
        String icon = row.icon();
        if (icon.isEmpty() || icon.contains(":")) return "";
        if (icon.contains("/")) return icon;
        if (section.id().equals("templates")) return dev.hoi.client.map.commander.CommanderPanels.armyUnitIcon("unit_hud/" + icon);
        return "unit_hud/" + (section.id().startsWith("ship") ? "navy_" : section.id().startsWith("air") ? "air_" : "") + icon;
    }
    private void value(GuiGraphicsExtractor g, Font font, String value, int x, int y, int width, int height) {
        UiText.scrollingCell(g, font, net.minecraft.network.chat.Component.literal(value), x, y, width, height, UiMenuStyle.TEXT, true, net.minecraft.util.Util.getMillis() - openedAt);
    }
    private void drawFooter(GuiGraphicsExtractor g) {
        for (var section : report.sections()) {
            if (section.id().equals("ideas")) {
                int x = at(20), y = ideaY(), width = at(ideaWidth()), height = at(domain == AgencyIntelDomain.CIVILIAN ? 59 : 109);
                UiAssets.nineSlice(g, source("mini_bg", 0), x, y, width, height, 12, scale);
                g.enableScissor(x, y, x + width, y + height);
                for (int index = 0; index < section.rows().size(); index++) {
                    var row = section.rows().get(index);
                    UiAssets.draw(g, row.icon(), at(25 + index / ideaRows() * 60) - offsets.getOrDefault("ideas", 0), y + at(5 + index % ideaRows() * 50), at(48), at(48));
                }
                g.disableScissor();
                g.pose().pushMatrix();
                g.pose().translate(x, ideaBottom());
                g.pose().rotate(-(float)Math.PI / 2);
                ideaBar(section).draw(g, offsets.getOrDefault("ideas", 0));
                g.pose().popMatrix();
            } else if (section.id().equals("doctrine") && !section.rows().isEmpty()) {
                UiAssets.nineSlice(g, source("mini_bg", 0), at(400), ideaY(), at(120), at(109), 12, scale);
                var folder = section.rows().getFirst();
                if (section.state().equals("no_intel")) UiAssets.drawGrayscale(g, folder.icon(), at(415), ideaY() + at(25), at(91), at(61));
                else UiAssets.draw(g, folder.icon(), at(415), ideaY() + at(25), at(91), at(61));
            }
        }
    }
    private void rows(GuiGraphicsExtractor g, Font font, CountryView.Section section, int y) {
        int rowY = y + at(8) - offsets.getOrDefault(section.id(), 0), index = 0;
        for (var row : section.rows()) {
            String icon = rowIcon(section, row);
            if (types(section)) {
                int x = at(30 + index % 5 * 95), ry = y + at(8 + index / 5 * 28);
                if (!icon.isEmpty()) UiAssets.draw(g, icon, x + at(27), ry + at(3), at(47), at(25));
                value(g, font, row.value(), x + at(6), ry + at(2), at(28), at(24));
                index++; continue;
            }
            int height = rowHeight(section, row);
            boolean army = section.id().equals("templates") || section.id().equals("stockpiles");
            int nameX = army ? 90 : row.header() ? section.id().equals("ships") ? 111 : 41 : section.id().equals("ships") ? 255 : 145;
            UiAssets.nineSlice(g, source(row.header() ? "intel_ledger_entry" : "mini_bg", 0), at(25), rowY, at(480), height, 4, scale);
            if (!icon.isEmpty() && !(row.header() && section.id().equals("airwings"))) {
                int iconX = army ? 30 : row.header() ? 41 : 35;
                int iconWidth = army || row.header() ? 55 : section.id().equals("ships") ? 200 : 100;
                if (section.id().equals("templates")) UiAssets.drawContent(g, icon, at(iconX), rowY + at(5), at(iconWidth), height - at(10));
                else UiAssets.draw(g, icon, at(iconX), rowY + at(5), at(iconWidth), height - at(10));
            }
            if (army) UiAssets.draw(g, source("generic_text_bg_108", 0), at(395), rowY + at(12), at(108), at(27));
            UiText.scrollingCell(g, font, net.minecraft.network.chat.Component.literal(row.name()), at(nameX), rowY + at(4), at(385 - nameX), height - at(8), UiMenuStyle.TEXT, false, net.minecraft.util.Util.getMillis() - openedAt);
            value(g, font, row.value(), at(395), rowY, at(105), height);
            rowY += height;
        }
    }
    private void drawSections(GuiGraphicsExtractor g, Font font) {
        for (var section : report.sections()) {
            if (!sectionVisible(section)) continue;
            int y = sectionY(section), h = sectionHeight(section);
            g.enableScissor(at(20), y, at(520), Math.min(bottom - at(75), y + h));
            UiAssets.nineSlice(g, source(domain == AgencyIntelDomain.CIVILIAN || types(section) ? "mini_bg" : "mini_bg_grey", 0), at(20), y, at(500), h, 12, scale);
            if (domain == AgencyIntelDomain.CIVILIAN) {
                UiAssets.draw(g, source("intel_ledger_chart_grid_bg", 0), at(90), y + at(10), at(378), at(151));
                AgencyIntelChart.read(section, history).draw(g, font, at(89), y + at(30), scale);
                if (!section.text().isBlank()) UiText.centered(g, font, section.text(), at(150), y + at(70), at(260), at(20), UiMenuStyle.TEXT);
                g.disableScissor();
                continue;
            }
            if (section.state().equals("no_intel")) {
                int x = domain == AgencyIntelDomain.CIVILIAN ? 100 : 25;
                int w = domain == AgencyIntelDomain.CIVILIAN ? 410 : 482;
                UiAssets.draw(g, source("no_intel", 0), at(x), y, at(w), Math.min(h, at(94)));
                UiText.centered(g, font, section.text(), at(x), y, at(w), Math.min(h, at(94)), UiMenuStyle.TEXT);
            } else {
                rows(g, font, section, y);
            }
            g.disableScissor();
            if (!types(section) && section.state().equals("shown")) bar(section).draw(g, offsets.getOrDefault(section.id(), 0));
        }
    }
    private void drawResources(GuiGraphicsExtractor g) {
        var section = report.sections().stream().filter(value -> value.id().equals("resources") && value.state().equals("shown")).findFirst();
        if (section.isEmpty()) return;
        int index = 0;
        for (var header : section.get().rows()) {
            if (!header.header()) continue;
            double[] values = new double[3];
            boolean[] present = new boolean[3];
            for (var row : section.get().rows()) if (!row.header() && row.group().equals(header.group())) {
                int part = List.of("domestic", "exported", "imported").indexOf(row.name());
                if (part < 0) continue;
                try {
                    double value = Double.parseDouble(row.value());
                    if (Double.isFinite(value) && value >= 0) { values[part] = value; present[part] = true; }
                } catch (NumberFormatException ignored) {}
            }
            if (index >= RESOURCES.size()) break;
            if (present[0] && present[1] && present[2]) {
                double total = values[0] + values[1] + values[2];
                int[] colors = {0xFF00B300, 0xFFFFFF00, 0xFF0000FF};
                int x = 49 + index * 69;
                for (int row = 0; row < 20; row++) for (int col = 0; col < 20; col++) {
                    double dx = col + .5 - 10, dy = row + .5 - 10;
                    if (dx * dx + dy * dy >= 100) continue;
                    double angle = (Math.atan2(dy, dx) + Math.PI * 2.5) % (Math.PI * 2) / (Math.PI * 2);
                    int part = total == 0 ? 2 : angle < values[0] / total ? 0 : angle < (values[0] + values[1]) / total ? 1 : 2;
                    g.fill(at(x + col), top + at(58 + row), at(x + col + 1), top + at(59 + row), colors[part]);
                }
                UiAssets.draw(g, source("mini_piechart_overlay", 0), at(47 + index * 69), top + at(56), at(25), at(25));
            }
            index++;
        }
    }
    public void draw(GuiGraphicsExtractor g, Font font) {
        g.enableScissor(0, viewportTop, at(550), viewportBottom);
        assets.tile(g, source("tiled_window2_1b_border", 0), 0, top, at(550), bottom - top, 64, 64, scale);
        if (domain == AgencyIntelDomain.CIVILIAN) {
            UiAssets.nineSlice(g, source("mini_bg_grey", 0), at(20), top + at(5), at(500), at(85), 12, scale);
            UiAssets.nineSlice(g, source("mini_bg", 0), at(20), top + at(95), at(500), at(75), 12, scale);
            UiAssets.nineSlice(g, source("mini_bg_grey", 0), at(20), top + at(175), at(500), at(180), 12, scale);
        } else UiAssets.nineSlice(g, source("mini_bg_grey", 0), at(20), top + at(25), at(500), at(domain == AgencyIntelDomain.ARMY ? 55 : 65), 12, scale);
        if (domain == AgencyIntelDomain.CIVILIAN) drawResources(g);
        if (!report.sections().isEmpty()) drawSections(g, font);
        int bodyY = top + at(domain == AgencyIntelDomain.CIVILIAN ? 355 : 130), footerY = bottom - at(220);
        if (report.sections().isEmpty()) {
        UiAssets.nineSlice(g, source("mini_bg_grey", 0), at(20), bodyY, at(500), Math.max(at(30), footerY - bodyY), 12, scale);
        var insufficient = report.entries().stream().filter(entry -> entry.name().equals("정보가 충분하지 않음")).findFirst();
        if (insufficient.isPresent()) {
            UiAssets.draw(g, source("no_intel", 0), at(25), bodyY + at(5), at(490), at(100));
            UiText.centered(g, font, insufficient.get().name(), at(25), bodyY + at(25), at(490), at(40), UiMenuStyle.TEXT);
        }
        }
        if (domain != AgencyIntelDomain.CIVILIAN) {
            UiAssets.draw(g, source("trade_header", 0), at(12), footerY + at(5), at(516), at(35));
            UiText.centered(g, font, UiCommandLabels.text("ARMY_LEDGER_THRESHOLD_STAFF_AND_RESEARCH", ""), at(12), footerY + at(5), at(500), at(35), UiMenuStyle.TEXT);
        }
        drawFooter(g);
        UiAssets.draw(g, source("intel_ledger_tech", 0), at(26), bottom - at(70), at(490), at(69));
        for (var image : art) {
            if (image.disabled()) UiAssets.drawGrayscale(g, image.icon(), at(image.x()), top + at(image.y()), at(image.width()), at(image.height()));
            else UiAssets.draw(g, image.icon(), at(image.x()), top + at(image.y()), at(image.width()), at(image.height()));
        }
        for (var cell : cells) {
            int x = at(cell.x()), y = top + at(cell.y()), w = at(cell.width()), h = at(cell.height());
            if (RESEARCH.containsKey(cell.entry().name())) continue;
            if (cell.left()) UiText.scrollingCell(g, font, net.minecraft.network.chat.Component.literal(cell.entry().name() + ": " + cell.entry().value()), x, y, w, h, UiMenuStyle.TEXT, false, net.minecraft.util.Util.getMillis() - openedAt);
            else value(g, font, cell.entry().value(), x, y, w, h);
        }
        g.disableScissor();
        bodyBar().draw(g, viewportTop - top);
    }
}
