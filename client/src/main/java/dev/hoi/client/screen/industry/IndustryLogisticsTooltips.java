package dev.hoi.client.screen.industry;

import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.protocol.ProtocolNumberBounds;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class IndustryLogisticsTooltips {
    private static final List<String> KEYS = List.of("EFFICIENCY", "EQUIPMENT_TYPE", "PRODUCED", "STATUS", "NEEDS", "BALANCE", "IN_STOCK", "RESOURCES");
    private static final Map<String,String> SOURCE = Map.ofEntries(
            Map.entry("TOPBAR_ARMY", "육군"), Map.entry("TOPBAR_NAVY", "해군"), Map.entry("TOPBAR_AIR", "공군"),
            Map.entry("LOGISTICS_FUEL_PRIORITIZATION", "연료 우선순위"),
            Map.entry("FUEL_LOW_PRIORITY_DESC", "연료 우선순위 낮음"), Map.entry("FUEL_MEDIUM_PRIORITY_DESC", "연료 우선순위 중간"),
            Map.entry("FUEL_HIGH_PRIORITY_DESC", "연료 우선순위 높음"),
            Map.entry("LOGISTICS_FUEL_PERCENTAGE", "§H$VALUE|0$%§!의 필요한 연료가 충족되었습니다."),
            Map.entry("PRODUCTION_OUTPUT", "비축량"), Map.entry("LOGISTICS_MILITARY_FACTORIES_LABEL", "군수공장"),
            Map.entry("LOGISTICS_NAVAL_FACTORIES_LABEL", "해군 조선소"), Map.entry("NUCLEAR_BOMB", "원자폭탄"),
            Map.entry("LOGISTICS_FUEL_STORED", "비축량"), Map.entry("LOGISTICS_FUEL_CONSUMED", "소모된 양"),
            Map.entry("LINECHART_DETAILS_DAYS", "일"), Map.entry("LINECHART_DETAILS_MONTHS", "월"), Map.entry("LINECHART_DETAILS_YEARS", "년"));
    private IndustryLogisticsTooltips() {}
    static String text(String key) { return UiCommandLabels.text(key, Objects.requireNonNull(SOURCE.get(key), key)); }
    static String header(int column) {
        String key = "LOGISTICS_" + KEYS.get(column);
        String title = "§e" + IndustryEquipmentDesigns.tooltip(key + "_LABEL") + "§f";
        if (column == 1) return title;
        return title + "\n\n" + IndustryEquipmentDesigns.tooltip(key + (column == 2 ? "_LABEL_DESC" : "_DESC"));
    }
    static String row(IndustryLogisticsRows.IndustryLogisticsRow row, int column) {
        var equipment = row.representative();
        String name = equipment.familyName() == null || equipment.familyName().isBlank() ? equipment.name() : equipment.familyName();
        return switch (column) {
            case 0 -> row.efficiency() == null
                    ? IndustryEquipmentDesigns.tooltip("LOGISTICS_NO_EQUIPMENT_PRODUCTION").replace("$EQUIPMENT|Y$", "§e" + name + "§f")
                    : IndustryEquipmentDesigns.tooltip("LOGISTICS_AVERAGE_EFFICIENCY")
                            .replace("$EQUIPMENT|H$", "§e" + name + "§f")
                            .replace("$EFFICIENCY|Y%$", "§e" + ProtocolNumberBounds.percent(row.efficiency(), 1) + "§f")
                            + "\n\n" + IndustryEquipmentDesigns.tooltip("LOGISTICS_AVERAGE_EFFICIENCY_DELAYED");
            case 1 -> "§e" + name + "§f\n\n" + row.models().stream().map(model -> "§e" + model.name() + "§f"
                    + "\n보관 재고: §e" + model.stockpile() + "§f\n훈련에 지급: §e" + model.reserved()
                    + "§f\n배치 부대 보유: §e" + model.deployed() + "§f").collect(java.util.stream.Collectors.joining("\n\n"));
            case 2 -> header(column) + "\n\n" + name + "\n일일 생산량: §e" + ProtocolNumberBounds.fixed(row.daily(), 1) + "§f";
            case 3 -> header(column) + "\n\n" + row.supplyDetail();
            case 4 -> header(column) + "\n\n현재 미지급 충원 수요: §e" + (row.demand() == null ? "—" : row.demand()) + "§f";
            case 5 -> header(column) + "\n\n현재 비축량 − 미지급 충원 수요: "
                    + (row.balance() == null ? "—" : (row.balance() < 0 ? "§c" : "§a") + row.balance()) + "§f";
            case 6 -> header(column) + "\n\n보관 재고: §e" + row.stockpile() + "§f\n충원 후 잉여/부족: "
                    + (row.balance() == null ? "—" : (row.balance() < 0 ? "§c" : "§a") + row.balance()) + "§f";
            default -> throw new IllegalArgumentException("Unknown logistics column");
        };
    }
}
