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
    private static IndustryView.Equipment equipment(String id, boolean unlocked, long stock, String replacement, String family) {
        return new IndustryView.Equipment(id, id, "image/" + id, "infantry", false, unlocked, 1, 1,
                Map.of(), stock, 0, 0, 0, replacement, family);
    }
}
