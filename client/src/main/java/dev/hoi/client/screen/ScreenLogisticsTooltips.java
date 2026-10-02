package dev.hoi.client.screen;

import dev.hoi.client.ui.UiEquipmentDesigns;
import dev.hoi.protocol.ProtocolNumberBounds;
import java.util.List;

final class ScreenLogisticsTooltips {
    private static final List<String> KEYS = List.of("EFFICIENCY", "EQUIPMENT_TYPE", "PRODUCED", "STATUS", "NEEDS", "BALANCE", "IN_STOCK", "RESOURCES");
    private ScreenLogisticsTooltips() {}
    static String header(int column) {
        String key = "LOGISTICS_" + KEYS.get(column);
        String title = "§e" + UiEquipmentDesigns.tooltip(key + "_LABEL") + "§f";
        if (column == 1) return title;
        return title + "\n\n" + UiEquipmentDesigns.tooltip(key + (column == 2 ? "_LABEL_DESC" : "_DESC"));
    }
    static String row(ScreenLogisticsRows.ScreenLogisticsRow row, int column) {
        var equipment = row.representative();
        String name = equipment.familyName() == null || equipment.familyName().isBlank() ? equipment.name() : equipment.familyName();
        return switch (column) {
            case 0 -> row.efficiency() == null
                    ? UiEquipmentDesigns.tooltip("LOGISTICS_NO_EQUIPMENT_PRODUCTION").replace("$EQUIPMENT|Y$", "§e" + name + "§f")
                    : UiEquipmentDesigns.tooltip("LOGISTICS_AVERAGE_EFFICIENCY")
                            .replace("$EQUIPMENT|H$", "§e" + name + "§f")
                            .replace("$EFFICIENCY|Y%$", "§e" + ProtocolNumberBounds.percent(row.efficiency(), 1) + "§f")
                            + "\n\n" + UiEquipmentDesigns.tooltip("LOGISTICS_AVERAGE_EFFICIENCY_DELAYED");
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
