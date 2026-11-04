package dev.hoi.protocol.diplomacy;

import dev.hoi.protocol.country.CountryView;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public record DiplomacyVolunteerView(String session, long revision, String viewer, String target, String title,
        String sourceDescription, String transferText, String unassignedText, List<ArmyRow> armies,
        List<String> selectedArmyKeys, String summaryText, String capText, String threatText,
        boolean airEnabled, boolean airSelected, String airReason, boolean submitEnabled, String submitReason) {
    public static final int MAX_ARMIES = 128;
    public static final int MAX_UNIT_COUNTS = 11;
    public DiplomacyVolunteerView {
        token(session);
        if (revision < 0) throw new IllegalArgumentException("Invalid volunteer revision");
        CountryView.tag(viewer);
        CountryView.tag(target);
        CountryView.text(title, 128);
        CountryView.text(sourceDescription, 4000);
        CountryView.text(transferText, 128);
        CountryView.text(unassignedText, 1000);
        CountryView.text(summaryText, 4000);
        CountryView.text(capText, 4000);
        CountryView.text(threatText, 1000);
        CountryView.text(airReason, 4000);
        CountryView.text(submitReason, 4000);
        armies = CountryView.bounded(armies, MAX_ARMIES);
        selectedArmyKeys = CountryView.bounded(selectedArmyKeys, MAX_ARMIES);
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
            DiplomacyVolunteerView.key(key);
            CountryView.text(name, 128);
            texture(icon);
            CountryView.text(divisionText, 128);
            CountryView.text(commanderName, 128);
            texture(commanderPortrait);
            CountryView.text(commanderSkillText, 128);
            CountryView.text(commanderTooltip, 4000);
            unitCounts = CountryView.bounded(unitCounts, MAX_UNIT_COUNTS);
            CountryView.text(reason, 1000);
        }
    }
    public record UnitCount(String icon, String text) {
        public UnitCount {
            texture(icon);
            CountryView.text(text, 128);
        }
    }
    static void token(String token) {
        CountryView.text(token, 36);
        UUID.fromString(token);
    }
    static void key(String key) {
        CountryView.text(key, 64);
        if (key.isBlank()) throw new IllegalArgumentException("Missing volunteer army key");
    }
    private static void texture(String texture) {
        CountryView.text(texture, 160);
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
