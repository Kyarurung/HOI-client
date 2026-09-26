package dev.hoi.client.screen;

import dev.hoi.protocol.ProtocolIndustryView;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenLogisticsRowsTest {
    @org.junit.jupiter.api.Test void stockLabelsKeepFourCharactersAcrossUnitBoundaries() {
        org.junit.jupiter.api.Assertions.assertEquals("3.3K", ScreenLogisticsRows.stockLabel(3300));
        org.junit.jupiter.api.Assertions.assertEquals("3.3M", ScreenLogisticsRows.stockLabel(3300000));
        org.junit.jupiter.api.Assertions.assertEquals("10K", ScreenLogisticsRows.stockLabel(9950));
        org.junit.jupiter.api.Assertions.assertEquals("1M", ScreenLogisticsRows.stockLabel(999500));
        org.junit.jupiter.api.Assertions.assertEquals("999", ScreenLogisticsRows.stockLabel(999));
        for (long value : new long[]{0, 1000, 9999, 10000, 99949, 999499, 999500, 999999, 1000000, Long.MAX_VALUE})
            org.junit.jupiter.api.Assertions.assertTrue(ScreenLogisticsRows.stockLabel(value).length() <= 4, Long.toString(value));
    }

    @Test void productionUsesOneDecimalAndCompactUnits() {
        double[] values = {0, 1, 1.24, 1.26, 999.9, 999.99, 1000, 1100, 10100, 999949, 999950, 1000000};
        String[] labels = {"0", "1", "1.2", "1.3", "999.9", "1K", "1K", "1.1K", "10.1K", "999.9K", "1M", "1M"};
        for (int i = 0; i < values.length; i++) assertEquals(labels[i], ScreenLogisticsRows.productionLabel(values[i]), Double.toString(values[i]));
    }
    @Test void onlyAcquiredFamiliesAppearWithLatestResearchedImageAndCombinedAmounts() {
        var old = equipment("old", true, 20, "new", "rifles");
        var current = equipment("new", true, 0, "", "rifles");
        var future = equipment("future", false, 5, "", "rifles");
        var hidden = equipment("hidden", false, 0, "", "other");
        var imported = equipment("imported", false, 7, "", "foreign");
        var legacy = equipment("legacy", true, 0, null, null);
        var input = List.of(future, hidden, old, current, imported, legacy);
        var lines = List.of(new ProtocolIndustryView.Line("line", old.id(), 1, 1, 1, 3.5, 0, 0));
        var rows = ScreenLogisticsRows.create(input, lines, "all");
        assertEquals(1, rows.size());
        var rifles = rows.getFirst();
        assertEquals("image/new", rifles.representative().texture());
        assertEquals(25, rifles.stockpile());
        assertEquals(3.5, rifles.daily());
        assertEquals(List.of(future, old, current), rifles.models());
        assertTrue(ScreenLogisticsRows.create(input, lines, "navy").isEmpty());
        assertEquals(20, old.stockpile(), "Grouping does not convert existing stock");
    }
    @Test void unacquiredFamilyWithOnlyDemandRemainsHidden() {
        var demand = new ProtocolIndustryView.Equipment("old", "Old model", "image/old", "infantry", false, false, 1, 1,
                Map.of(), 0, 0, 0, 30, "", "rifles", "보병 장비");
        var rows = ScreenLogisticsRows.create(List.of(demand), List.of(), "all");
        assertTrue(rows.isEmpty());
    }
    @Test void currentDemandDrivesStatusAndSignedBalanceAcrossGenerations() {
        var old = new ProtocolIndustryView.Equipment("old", "old", "image/old", "infantry", false, false, 1, 1,
                Map.of(), 10, 0, 0, 0, "new", "rifles", "보병 장비", 60L);
        var modern = new ProtocolIndustryView.Equipment("new", "new", "image/new", "infantry", false, true, 1, 1,
                Map.of(), 20, 0, 0, 0, "", "rifles", "보병 장비", 0L);
        var rows = ScreenLogisticsRows.create(List.of(old, modern), List.of(), "all");
        assertEquals(60L, rows.getFirst().demand());
        assertEquals(-30L, rows.getFirst().balance());
        assertEquals("부족", rows.getFirst().status());
        var line = new ProtocolIndustryView.Line("line", "new", 1, 1, 1, 10, 0, 0);
        var producing = ScreenLogisticsRows.create(List.of(old, modern), List.of(line), "all").getFirst();
        assertEquals("생산", producing.status());
        assertTrue(producing.supplyDetail().contains("3일"));
        assertNull(ScreenLogisticsRows.create(List.of(equipment("legacy", true, 10, "", "rifles")), List.of(), "all").getFirst().balance());
        var covered = ScreenLogisticsRows.create(List.of(modern), List.of(), "all").getFirst();
        assertEquals("충족", covered.status());
        assertEquals(20L, covered.balance());
    }
    @Test void acquisitionHistoryKeepsAnExhaustedFamilyVisibleButAnEmptyLineDoesNot() {
        var never = equipment("new", true, 0, "", "rifles");
        var line = new ProtocolIndustryView.Line("line", "new", 1, 1, 1, 10, 0, 0);
        assertTrue(ScreenLogisticsRows.create(List.of(never),List.of(line),"all").isEmpty());
        var acquired = new ProtocolIndustryView.Equipment("old","old","image/old","infantry",false,false,1,1,
                Map.of(),0,0,0,0,"new","rifles","보병 장비",0L,true);
        var rows = ScreenLogisticsRows.create(List.of(acquired,never),List.of(line),"all");
        assertEquals(1,rows.size());
        assertEquals("image/new",rows.getFirst().representative().texture());
        assertEquals(0,rows.getFirst().stockpile());
    }
    private static ProtocolIndustryView.Equipment equipment(String id, boolean unlocked, long stock, String replacement, String family) {
        return new ProtocolIndustryView.Equipment(id, id, "image/" + id, "infantry", false, unlocked, 1, 1,
                Map.of(), stock, 0, 0, 0, replacement, family);
    }
}
