package dev.hoi.protocol;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public record ProtocolDiplomacyVolunteerView(String session, long revision, String viewer, String target, String title,
        String sourceDescription, String transferText, String unassignedText, List<ArmyRow> armies,
        List<String> selectedArmyKeys, String summaryText, String capText, String threatText,
        boolean airEnabled, boolean airSelected, String airReason, boolean submitEnabled, String submitReason) {
    public static final int MAX_ARMIES = 128;
    public static final int MAX_UNIT_COUNTS = 11;
    public ProtocolDiplomacyVolunteerView {
        token(session);
        if (revision < 0) throw new IllegalArgumentException("Invalid volunteer revision");
        ProtocolCountryView.tag(viewer);
        ProtocolCountryView.tag(target);
        ProtocolCountryView.text(title, 128);
        ProtocolCountryView.text(sourceDescription, 4000);
        ProtocolCountryView.text(transferText, 128);
        ProtocolCountryView.text(unassignedText, 1000);
        ProtocolCountryView.text(summaryText, 4000);
        ProtocolCountryView.text(capText, 4000);
        ProtocolCountryView.text(threatText, 1000);
        ProtocolCountryView.text(airReason, 4000);
        ProtocolCountryView.text(submitReason, 4000);
        armies = ProtocolCountryView.bounded(armies, MAX_ARMIES);
        selectedArmyKeys = ProtocolCountryView.bounded(selectedArmyKeys, MAX_ARMIES);
        var issued = new HashSet<String>();
        for (var army : armies) {
            if (!issued.add(army.key())) throw new IllegalArgumentException("Duplicate volunteer army");
        }
        var selected = new HashSet<String>();
        for (var armyKey : selectedArmyKeys) {
            key(armyKey);
            if (!selected.add(armyKey) || !issued.contains(armyKey))
                throw new IllegalArgumentException("Invalid volunteer selection");
        }
    }
    public record ArmyRow(String key, String name, String icon, String divisionText, String commanderName,
            String commanderPortrait, String commanderSkillText, String commanderTooltip,
            List<UnitCount> unitCounts, boolean allowed, String reason) {
        public ArmyRow {
            ProtocolDiplomacyVolunteerView.key(key);
            ProtocolCountryView.text(name, 128);
            texture(icon);
            ProtocolCountryView.text(divisionText, 128);
            ProtocolCountryView.text(commanderName, 128);
            texture(commanderPortrait);
            ProtocolCountryView.text(commanderSkillText, 128);
            ProtocolCountryView.text(commanderTooltip, 4000);
            unitCounts = ProtocolCountryView.bounded(unitCounts, MAX_UNIT_COUNTS);
            ProtocolCountryView.text(reason, 1000);
        }
    }
    public record UnitCount(String icon, String text) {
        public UnitCount {
            texture(icon);
            ProtocolCountryView.text(text, 128);
        }
    }
    static void token(String token) {
        ProtocolCountryView.text(token, 36);
        UUID.fromString(token);
    }
    static void key(String key) {
        ProtocolCountryView.text(key, 64);
        if (key.isBlank()) throw new IllegalArgumentException("Missing volunteer army key");
    }
    private static void texture(String texture) {
        ProtocolCountryView.text(texture, 160);
        if (texture.isEmpty()) return;
        if (!texture.matches("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*")
                && !texture.matches("hoi:textures/gui/[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*\\.png"))
            throw new IllegalArgumentException("Invalid volunteer texture");
        String path = texture.substring(texture.indexOf(':') + 1);
        for (String segment : path.split("/")) {
            if (segment.equals(".") || segment.equals(".."))
                throw new IllegalArgumentException("Invalid volunteer texture path");
        }
    }
}
