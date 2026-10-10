package dev.hoi.protocol;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProtocolDiplomacyView(String session, long revision, String viewer, String target, String title,
        String subtitle, String currentStatus, String acceptanceText, String acceptanceDetail, boolean submitEnabled,
        String submitReason, List<EquipmentChoice> equipment, List<LeaseLine> lines) {
    public enum Category { INFANTRY, ARMOR, AIR, NAVAL }
    public enum Mode { ONCE, MONTHLY, PERCENT }
    public ProtocolDiplomacyView {
        UUID.fromString(session);
        if (revision < 0) throw new IllegalArgumentException("Invalid diplomacy revision");
        ProtocolCountryView.tag(viewer);
        ProtocolCountryView.tag(target);
        ProtocolCountryView.text(title, 128);
        ProtocolCountryView.text(subtitle, 1000);
        ProtocolCountryView.text(currentStatus, 1000);
        ProtocolCountryView.text(acceptanceText, 1000);
        ProtocolCountryView.text(acceptanceDetail, 4000);
        ProtocolCountryView.text(submitReason, 4000);
        equipment = ProtocolCountryView.bounded(equipment, 256);
        lines = leaseLines(lines);
        if (equipment.stream().map(EquipmentChoice::key).distinct().count() != equipment.size())
            throw new IllegalArgumentException("Duplicate diplomacy equipment choice");
    }
    public record EquipmentChoice(String key, String name, String icon, Category category, String stockText,
            String productionText, String detail, boolean available, String reason, List<ModeChoice> modes) {
        public EquipmentChoice {
            ProtocolCountryView.text(key, 64);
            ProtocolCountryView.text(name, 128);
            ProtocolCountryView.text(icon, 160);
            if (!icon.isEmpty() && !icon.matches("[a-z0-9_/]+")) throw new IllegalArgumentException("Invalid equipment icon");
            Objects.requireNonNull(category);
            ProtocolCountryView.text(stockText, 128);
            ProtocolCountryView.text(productionText, 128);
            ProtocolCountryView.text(detail, 4000);
            ProtocolCountryView.text(reason, 1000);
            modes = ProtocolCountryView.bounded(modes, 3);
            if (modes.stream().map(ModeChoice::mode).distinct().count() != modes.size())
                throw new IllegalArgumentException("Duplicate lease mode");
        }
    }
    public record ModeChoice(Mode mode, String label, boolean supported, String reason, long defaultAmount, String unitLabel) {
        public ModeChoice {
            Objects.requireNonNull(mode);
            ProtocolCountryView.text(label, 128);
            ProtocolCountryView.text(reason, 1000);
            ProtocolCountryView.text(unitLabel, 128);
            if (defaultAmount < 0 || mode == Mode.PERCENT && defaultAmount > 100)
                throw new IllegalArgumentException("Invalid lease default amount");
        }
    }
    public record LeaseLine(String key, Mode mode, long amount) {
        public LeaseLine {
            ProtocolCountryView.text(key, 64);
            Objects.requireNonNull(mode);
            if (amount <= 0 || mode == Mode.PERCENT && amount > 100)
                throw new IllegalArgumentException("Invalid lease quantity");
        }
    }
    static List<LeaseLine> leaseLines(List<LeaseLine> lines) {
        var result = ProtocolCountryView.bounded(lines, 32);
        if (result.stream().map(LeaseLine::key).distinct().count() != result.size())
            throw new IllegalArgumentException("Duplicate lease equipment");
        return result;
    }
}
