package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.*;
import java.util.function.Consumer;

final class ScreenIntelLedger {
    private static final Map<String, String> RESEARCH = Map.of(
        "보병", "infantry", "지상 & 항공 지원", "support", "포", "artillery", "기갑", "armour",
        "해군", "naval", "공군", "air_techs", "산업", "industry", "공학", "secret_weapons");
    private static final List<String> RESOURCES = List.of("석유", "알루미늄", "고무", "텅스텐", "강철", "크롬", "에너지");
    private final ProtocolCountryView.Report report;
    private final ProtocolIntelDomain domain;
    private final float scale;
    private final int top, bottom;
    private final long openedAt;
    private final List<Cell> cells = new ArrayList<>();
    private String selectedSection;
    private final Map<String, Integer> offsets = new HashMap<>();
    private ProtocolCountryView.Section dragging;
    private record Cell(ProtocolMenuView.Entry entry, String icon, int x, int y, int width, int height, boolean research) {}
    ScreenIntelLedger(ProtocolCountryView.Report report, ProtocolIntelDomain domain, int pane, int top, int bottom, long openedAt) {
        this.report = report;
        this.domain = domain;
        this.scale = pane / 550f;
        this.top = top;
        this.bottom = bottom;
        this.openedAt = openedAt;
        selectedSection = domain == ProtocolIntelDomain.ARMY ? "templates" : "graph_industry";
        if (domain == ProtocolIntelDomain.CIVILIAN) {
            for (int i = 0; i < RESOURCES.size(); i++) add(RESOURCES.get(i), source("resources_strip", i), 30 + i * 69, 5, 66, 70, false);
            add("총 공장", "hud/factories", 30, 100, 50, 60, false);
            add("가용 인력", "hud/manpower", 80, 100, 50, 60, false);
            add("사용 가능한 연료", "hud/fuel", 130, 100, 50, 60, false);
            add("총 수송선", "hud/convoys", 180, 100, 50, 60, false);
        } else if (domain == ProtocolIntelDomain.ARMY) {
            add("총 사단 수", source("endgame_army_button", 0), 30, 25, 140, 55, false);
            add("총 특수부대 수", source("special_forces_icon", 2), 200, 37, 88, 27, false);
            add("배치한 병력", "hud/manpower", 300, 37, 108, 27, false);
            add("배치한 육군 장비", source("equipment_icon", 0), 420, 37, 88, 27, false);
        } else if (domain == ProtocolIntelDomain.NAVY) {
            add("함대 수", "", 90, 37, 200, 20, false);
            add("태스크 포스 수", "", 90, 57, 200, 20, false);
            add("배치한 인력", "hud/manpower", 280, 43, 108, 27, false);
            add("총 함선 수", "", 400, 43, 108, 27, false);
        } else {
            add("총 항공기 수", source("endgame_air_button", 0), 30, 25, 250, 55, false);
            add("배치한 공군 인력", "hud/manpower", 380, 43, 108, 27, false);
            add("할당 임무 수", "", 280, 43, 88, 27, false);
        }
        int i = 0;
        for (var entry : report.entries()) if (RESEARCH.containsKey(entry.name())) {
            int y = Math.round((bottom - top) / scale) - 65;
            cells.add(new Cell(entry, source(RESEARCH.get(entry.name()) + "_folder_tab", 0), 70 + i++ * 80, y, 91, 61, true));
        }
    }
    static String source(String name, int frame) { return "intel/source/gfx_" + name + "/frame_" + frame; }
    private int at(int value) { return Math.round(value * scale); }
    private void add(String name, String icon, int x, int y, int width, int height, boolean research) {
        report.entries().stream().filter(entry -> entry.name().equals(name)).findFirst()
            .ifPresent(entry -> cells.add(new Cell(entry, icon, x, y, width, height, research)));
    }
    void addHoverRegions(Consumer<AbstractWidget> add) {
        if (domain == ProtocolIntelDomain.ARMY) {
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
        } else if (domain == ProtocolIntelDomain.CIVILIAN) {
            int i = 0;
            String[] icons = {"hud/factories", "hud/convoys", source("wing_types_own", 1), source("intel_ledger_truck_icon", 0), source("intel_ledger_train_icon", 0)};
            for (var section : report.sections()) {
                String icon = i < icons.length ? icons[i] : "";
                var button = new UiPanelButton(section.id(), at(34), top + at(180 + i++ * 35), at(40), at(32), () -> selectedSection = section.id()) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        UiAssets.draw(g, source("strategicair_mission_btn_bg", selectedSection.equals(section.id()) ? 1 : 0), getX(), getY(), getWidth(), getHeight());
                        if (!icon.isEmpty()) UiAssets.draw(g, icon, getX() + at(6), getY() + at(4), at(28), at(24));
                    }
                };
                button.setTooltip(UiTooltips.createExact(section.tooltip())); add.accept(button);
            }
        }
        for (var cell : cells) {
            var hover = new UiHoverRegion(cell.entry().name(), at(cell.x()), top + at(cell.y()), at(cell.width()), at(cell.height()));
            hover.setTooltip(UiTooltips.createExact(cell.entry().detail()));
            add.accept(hover);
        }
    }
    String detailAt(double x, double y) {
        for (var cell : cells) if (x >= at(cell.x()) && x < at(cell.x() + cell.width())
                && y >= top + at(cell.y()) && y < top + at(cell.y() + cell.height())) return cell.entry().detail();
        for (var section : report.sections()) {
            if (!sectionVisible(section)) continue;
            int sy = sectionY(section), sh = sectionHeight(section);
            if (x >= at(20) && x < at(520) && y >= sy && y < sy + sh) {
                if (!section.rows().isEmpty()) {
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
    private boolean sectionVisible(ProtocolCountryView.Section section) {
        return domain != ProtocolIntelDomain.CIVILIAN && domain != ProtocolIntelDomain.ARMY || section.id().equals(selectedSection);
    }
    private int sectionY(ProtocolCountryView.Section section) {
        return top + at(switch (section.id()) {
            case "templates", "stockpiles" -> 130;
            case "ship_types", "airwing_types" -> 94;
            case "ships", "airwings" -> 172;
            default -> 175;
        });
    }
    private int sectionHeight(ProtocolCountryView.Section section) {
        return section.id().equals("ship_types") || section.id().equals("airwing_types") ? at(70)
                : Math.max(at(94), bottom - at(75) - sectionY(section));
    }
    private boolean types(ProtocolCountryView.Section section) { return section.id().equals("ship_types") || section.id().equals("airwing_types"); }
    private int rowHeight(ProtocolCountryView.Section section, ProtocolCountryView.Row row) {
        return at(section.id().equals("templates") || section.id().equals("stockpiles") ? 50 : row.header() ? 40 : 65);
    }
    private UiScrollBar bar(ProtocolCountryView.Section section) {
        int y = sectionY(section), height = Math.min(sectionHeight(section), bottom - at(75) - y);
        return new UiScrollBar(at(520) - 9, y, y + height, height, at(16) + section.rows().stream().mapToInt(row -> rowHeight(section, row)).sum());
    }
    boolean scroll(double x, double y, double amount) {
        for (var section : report.sections()) if (sectionVisible(section) && !types(section) && x >= at(20) && x < at(520)
                && y >= sectionY(section) && y < sectionY(section) + sectionHeight(section)) {
            offsets.put(section.id(), Math.clamp(offsets.getOrDefault(section.id(), 0) - (int)(amount * at(50)), 0, bar(section).maximum()));
            return true;
        }
        return false;
    }
    boolean click(double x, double y) {
        for (var section : report.sections()) if (sectionVisible(section) && !types(section) && bar(section).contains(x, y)) {
            var bar = bar(section);
            dragging = bar.arrow(y) ? null : section;
            offsets.put(section.id(), dragging == null ? bar.step(y, offsets.getOrDefault(section.id(), 0), at(50)) : bar.offset(y));
            return true;
        }
        return false;
    }
    boolean drag(double y) { if (dragging == null) return false; offsets.put(dragging.id(), bar(dragging).offset(y)); return true; }
    boolean release() { boolean active = dragging != null; dragging = null; return active; }
    private String rowIcon(ProtocolCountryView.Section section, ProtocolCountryView.Row row) {
        String icon = row.icon();
        if (icon.isEmpty() || icon.contains(":")) return "";
        if (icon.contains("/")) return icon;
        return "unit_hud/" + (section.id().startsWith("ship") ? "navy_" : section.id().startsWith("air") ? "air_" : "") + icon;
    }
    private void value(GuiGraphicsExtractor g, Font font, String value, int x, int y, int width, int height) {
        if (font.width(value) <= width) UiText.centered(g, font, value, x, y, width, height, UiMenuStyle.TEXT);
        else UiText.scrollingCell(g, font, net.minecraft.network.chat.Component.literal(value), x, y, width, height, UiMenuStyle.TEXT, true, net.minecraft.util.Util.getMillis() - openedAt);
    }
    private void rows(GuiGraphicsExtractor g, Font font, ProtocolCountryView.Section section, int y) {
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
                UiAssets.draw(g, icon, at(iconX), rowY + at(5), at(iconWidth), height - at(10));
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
            UiAssets.nineSlice(g, source("mini_bg_grey", 0), at(20), y, at(500), h, 12, scale);
            if (section.state().equals("no_intel")) {
                int x = domain == ProtocolIntelDomain.CIVILIAN ? 100 : 25;
                int w = domain == ProtocolIntelDomain.CIVILIAN ? 410 : 482;
                UiAssets.draw(g, source("no_intel", 0), at(x), y, at(w), Math.min(h, at(94)));
                UiText.centered(g, font, section.text(), at(x), y, at(w), Math.min(h, at(94)), UiMenuStyle.TEXT);
            } else {
                rows(g, font, section, y);
            }
            g.disableScissor();
            if (!types(section) && section.state().equals("shown")) bar(section).draw(g, offsets.getOrDefault(section.id(), 0));
        }
    }
    void draw(GuiGraphicsExtractor g, Font font) {
        int headerHeight = domain == ProtocolIntelDomain.CIVILIAN ? 170 : 90;
        UiAssets.nineSlice(g, source("mini_bg_grey", 0), at(20), top + at(domain == ProtocolIntelDomain.CIVILIAN ? 5 : 25), at(500), at(headerHeight - 25), 12, scale);
        if (!report.sections().isEmpty()) drawSections(g, font);
        int bodyY = top + at(headerHeight + 40), footerY = bottom - at(220);
        if (report.sections().isEmpty()) {
        UiAssets.nineSlice(g, source("mini_bg_grey", 0), at(20), bodyY, at(500), Math.max(at(30), footerY - bodyY), 12, scale);
        var insufficient = report.entries().stream().filter(entry -> entry.name().equals("정보가 충분하지 않음")).findFirst();
        if (insufficient.isPresent()) {
            UiAssets.draw(g, source("no_intel", 0), at(25), bodyY + at(5), at(490), at(100));
            UiText.centered(g, font, insufficient.get().name(), at(25), bodyY + at(25), at(490), at(40), UiMenuStyle.TEXT);
        }
        }
        UiAssets.draw(g, source("intel_ledger_tech", 0), at(26), bottom - at(70), at(490), at(69));
        if (domain == ProtocolIntelDomain.NAVY) UiAssets.draw(g, source("endgame_navy_button", 0), at(30), top + at(33), at(55), at(55));
        for (var cell : cells) {
            int x = at(cell.x()), y = top + at(cell.y()), w = at(cell.width()), h = at(cell.height());
            if (cell.research()) {
                UiAssets.draw(g, cell.icon(), x, y, w, h);
                continue;
            }
            if (domain == ProtocolIntelDomain.NAVY && (cell.entry().name().equals("함대 수") || cell.entry().name().equals("태스크 포스 수"))) {
                UiText.scrollingCell(g, font, net.minecraft.network.chat.Component.literal(cell.entry().name() + ": " + cell.entry().value()), x, y, w, h, UiMenuStyle.TEXT, false, net.minecraft.util.Util.getMillis() - openedAt);
                continue;
            }
            if (domain == ProtocolIntelDomain.CIVILIAN) {
                UiAssets.draw(g, cell.icon(), x + w / 4, y, w / 2, h / 2);
                value(g, font, cell.entry().value(), x, y + h / 2, w, h / 2);
            } else {
                int icon = cell.icon().isEmpty() ? 0 : Math.min(h, at(51));
                UiAssets.nineSlice(g, source("generic_text_bg_108", 0), x + icon / 2, y + (h - at(27)) / 2, w - icon / 2, at(27), 3, scale);
                if (icon > 0) UiAssets.draw(g, cell.icon(), x, y, icon, h);
                value(g, font, cell.entry().value(), x + icon, y, w - icon, h);
            }
        }
    }
}
