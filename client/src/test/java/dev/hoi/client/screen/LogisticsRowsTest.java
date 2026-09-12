package dev.hoi.client.screen;

import dev.hoi.protocol.IndustryView;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogisticsRowsTest {
    @org.junit.jupiter.api.Test void stockLabelsKeepFourCharactersAcrossUnitBoundaries() {
        org.junit.jupiter.api.Assertions.assertEquals("3.3K", LogisticsRows.stockLabel(3300));
        org.junit.jupiter.api.Assertions.assertEquals("3.3M", LogisticsRows.stockLabel(3300000));
        org.junit.jupiter.api.Assertions.assertEquals("10K", LogisticsRows.stockLabel(9950));
        org.junit.jupiter.api.Assertions.assertEquals("1M", LogisticsRows.stockLabel(999500));
        org.junit.jupiter.api.Assertions.assertEquals("999", LogisticsRows.stockLabel(999));
        for (long value : new long[]{0, 1000, 9999, 10000, 99949, 999499, 999500, 999999, 1000000, Long.MAX_VALUE})
            org.junit.jupiter.api.Assertions.assertTrue(LogisticsRows.stockLabel(value).length() <= 4, Long.toString(value));
    }

    @Test void productionUsesOneDecimalAndCompactUnits() {
        double[] values = {0, 1, 1.24, 1.26, 999.9, 999.99, 1000, 1100, 10100, 999949, 999950, 1000000};
        String[] labels = {"0", "1", "1.2", "1.3", "999.9", "1K", "1K", "1.1K", "10.1K", "999.9K", "1M", "1M"};
        for (int i = 0; i < values.length; i++) assertEquals(labels[i], LogisticsRows.productionLabel(values[i]), Double.toString(values[i]));
    }
    @Test void onlyResearchedOrStockedModelsAppearWithLatestResearchedImageAndCombinedAmounts() {
        var old = equipment("old", true, 20, "new", "rifles");
        var current = equipment("new", true, 0, "", "rifles");
        var future = equipment("future", false, 5, "", "rifles");
        var hidden = equipment("hidden", false, 0, "", "other");
        var imported = equipment("imported", false, 7, "", "foreign");
        var legacy = equipment("legacy", true, 0, null, null);
        var input = List.of(future, hidden, old, current, imported, legacy);
        var lines = List.of(new IndustryView.Line("line", old.id(), 1, 1, 1, 3.5, 0, 0));
        var rows = LogisticsRows.create(input, lines, "all");
        assertEquals(3, rows.size());
        var rifles = rows.getFirst();
        assertEquals("image/new", rifles.representative().texture());
        assertEquals(25, rifles.stockpile());
        assertEquals(3.5, rifles.daily());
        assertEquals(List.of(future, old, current), rifles.models());
        assertEquals(imported, rows.get(1).representative());
        assertEquals(legacy, rows.get(2).representative());
        assertTrue(LogisticsRows.create(input, lines, "navy").isEmpty());
        assertEquals(20, old.stockpile(), "Grouping does not convert existing stock");
    }
    @Test void lockedFamilyWithARealDeficitStillAppears() {
        var demand = new IndustryView.Equipment("old", "Old model", "image/old", "infantry", false, false, 1, 1,
                Map.of(), 0, 0, 0, 30, "", "rifles", "보병 장비");
        var rows = LogisticsRows.create(List.of(demand), List.of(), "all");
        assertEquals(1, rows.size());
        assertEquals(30, rows.getFirst().deficit());
        assertEquals("보병 장비", rows.getFirst().representative().familyName());
    }
    @Test void currentDemandDrivesStatusAndSignedBalanceAcrossGenerations() {
        var old = new IndustryView.Equipment("old", "old", "image/old", "infantry", false, false, 1, 1,
                Map.of(), 10, 0, 0, 0, "new", "rifles", "보병 장비", 60L);
        var modern = new IndustryView.Equipment("new", "new", "image/new", "infantry", false, true, 1, 1,
                Map.of(), 20, 0, 0, 0, "", "rifles", "보병 장비", 0L);
        var rows = LogisticsRows.create(List.of(old, modern), List.of(), "all");
        assertEquals(60L, rows.getFirst().demand());
        assertEquals(-30L, rows.getFirst().balance());
        assertEquals("부족", rows.getFirst().status());
        var line = new IndustryView.Line("line", "new", 1, 1, 1, 10, 0, 0);
        var producing = LogisticsRows.create(List.of(old, modern), List.of(line), "all").getFirst();
        assertEquals("생산", producing.status());
        assertTrue(producing.supplyDetail().contains("3일"));
        assertNull(LogisticsRows.create(List.of(equipment("legacy", true, 10, "", "rifles")), List.of(), "all").getFirst().balance());
        var covered = LogisticsRows.create(List.of(modern), List.of(), "all").getFirst();
        assertEquals("충족", covered.status());
        assertEquals(20L, covered.balance());
    }
    private static IndustryView.Equipment equipment(String id, boolean unlocked, long stock, String replacement, String family) {
        return new IndustryView.Equipment(id, id, "image/" + id, "infantry", false, unlocked, 1, 1,
                Map.of(), stock, 0, 0, 0, replacement, family);
    }
}
