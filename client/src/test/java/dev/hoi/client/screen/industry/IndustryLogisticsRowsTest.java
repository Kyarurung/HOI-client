package dev.hoi.client.screen.industry;

import dev.hoi.protocol.industry.IndustryView;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryLogisticsRowsTest {
    @org.junit.jupiter.api.Test void stockLabelsKeepFourCharactersAcrossUnitBoundaries() {
        org.junit.jupiter.api.Assertions.assertEquals("3.3K", IndustryLogisticsRows.stockLabel(3300));
        org.junit.jupiter.api.Assertions.assertEquals("3.3M", IndustryLogisticsRows.stockLabel(3300000));
        org.junit.jupiter.api.Assertions.assertEquals("10K", IndustryLogisticsRows.stockLabel(9950));
        org.junit.jupiter.api.Assertions.assertEquals("1M", IndustryLogisticsRows.stockLabel(999500));
        org.junit.jupiter.api.Assertions.assertEquals("999", IndustryLogisticsRows.stockLabel(999));
        for (long value : new long[]{0, 1000, 9999, 10000, 99949, 999499, 999500, 999999, 1000000, Long.MAX_VALUE})
            org.junit.jupiter.api.Assertions.assertTrue(IndustryLogisticsRows.stockLabel(value).length() <= 4, Long.toString(value));
    }
    @Test void productionUsesOneDecimalAndCompactUnits() {
        double[] values = {0, 1, 1.24, 1.26, 999.9, 999.99, 1000, 1100, 10100, 999949, 999950, 1000000};
        String[] labels = {"0", "1", "1.2", "1.3", "999.9", "1K", "1K", "1.1K", "10.1K", "999.9K", "1M", "1M"};
        for (int i = 0; i < values.length; i++) assertEquals(labels[i], IndustryLogisticsRows.productionLabel(values[i]), Double.toString(values[i]));
    }
    @Test void onlyAcquiredFamiliesAppearWithLatestResearchedImageAndCombinedAmounts() {
        var old = equipment("old", true, 20, "new", "rifles");
        var current = equipment("new", true, 0, "", "rifles");
        var future = equipment("future", false, 5, "", "rifles");
        var hidden = equipment("hidden", false, 0, "", "other");
        var imported = equipment("imported", false, 7, "", "foreign");
        var legacy = equipment("legacy", true, 0, null, null);
        var input = List.of(future, hidden, old, current, imported, legacy);
        var lines = List.of(new IndustryView.Line("line", old.id(), 1, 1, 1, 3.5, 0, 0));
        var rows = IndustryLogisticsRows.create(input, lines, "all");
        assertEquals(1, rows.size());
        var rifles = rows.getFirst();
        assertEquals("image/new", rifles.representative().texture());
        assertEquals(25, rifles.stockpile());
        assertEquals(3.5, rifles.daily());
        assertEquals(List.of(future, old, current), rifles.models());
        assertTrue(IndustryLogisticsRows.create(input, lines, "navy").isEmpty());
        assertEquals(20, old.stockpile(), "Grouping does not convert existing stock");
    }
    @Test void unacquiredFamilyWithOnlyDemandRemainsHidden() {
        var demand = new IndustryView.Equipment("old", "Old model", "image/old", "infantry", false, false, 1, 1,
                Map.of(), 0, 0, 0, 30, "", "rifles", "보병 장비");
        var rows = IndustryLogisticsRows.create(List.of(demand), List.of(), "all");
        assertTrue(rows.isEmpty());
    }
    @Test void currentDemandDrivesStatusAndSignedBalanceAcrossGenerations() {
        var old = new IndustryView.Equipment("old", "old", "image/old", "infantry", false, false, 1, 1,
                Map.of(), 10, 0, 0, 0, "new", "rifles", "보병 장비", 60L);
        var modern = new IndustryView.Equipment("new", "new", "image/new", "infantry", false, true, 1, 1,
                Map.of(), 20, 0, 0, 0, "", "rifles", "보병 장비", 0L);
        var rows = IndustryLogisticsRows.create(List.of(old, modern), List.of(), "all");
        assertEquals(60L, rows.getFirst().demand());
        assertEquals(-30L, rows.getFirst().balance());
        assertEquals("부족", rows.getFirst().status());
        var line = new IndustryView.Line("line", "new", 1, 1, 1, 10, 0, 0);
        var producing = IndustryLogisticsRows.create(List.of(old, modern), List.of(line), "all").getFirst();
        assertEquals("생산", producing.status());
        assertTrue(producing.supplyDetail().contains("3일"));
        assertNull(IndustryLogisticsRows.create(List.of(equipment("legacy", true, 10, "", "rifles")), List.of(), "all").getFirst().balance());
        var covered = IndustryLogisticsRows.create(List.of(modern), List.of(), "all").getFirst();
        assertEquals("충족", covered.status());
        assertEquals(20L, covered.balance());
    }
    @Test void acquisitionHistoryKeepsAnExhaustedFamilyVisibleButAnEmptyLineDoesNot() {
        var never = equipment("new", true, 0, "", "rifles");
        var line = new IndustryView.Line("line", "new", 1, 1, 1, 10, 0, 0);
        assertTrue(IndustryLogisticsRows.create(List.of(never),List.of(line),"all").isEmpty());
        var acquired = new IndustryView.Equipment("old","old","image/old","infantry",false,false,1,1,
                Map.of(),0,0,0,0,"new","rifles","보병 장비",0L,true);
        var rows = IndustryLogisticsRows.create(List.of(acquired,never),List.of(line),"all");
        assertEquals(1,rows.size());
        assertEquals("image/new",rows.getFirst().representative().texture());
        assertEquals(0,rows.getFirst().stockpile());
    }
    @Test void combatShipRowsStayHiddenWhileSupplyLandAndAirQuantitiesArePreserved() {
        var convoy = branchEquipment("convoy", "navy", true, "convoy", 580, 20, 0, 30);
        var harbor = branchEquipment("harbor", "navy", true, "floating_harbor_equipment", 3, 1, 0, 2);
        var train = branchEquipment("train", "infantry", false, "train", 200, 0, 0, 0);
        var truck = branchEquipment("truck", "infantry", false, "truck", 1500, 10, 90, 360);
        var rifle = branchEquipment("rifle", "infantry", false, "infantry", 100, 5, 25, 80);
        var plane = branchEquipment("plane", "air", false, "naval_bomber", 50, 0, 100, 20);
        var input = new ArrayList<>(List.of(convoy, harbor, train, truck, rifle, plane));
        for (String family : List.of("destroyer", "light_cruiser", "heavy_cruiser", "battlecruiser", "battleship", "carrier", "submarine"))
            input.add(branchEquipment("custom/" + family, "navy", true, family, 12, 3, 7, 4));
        var snapshot = List.copyOf(input);
        var lines = List.of(new IndustryView.Line("convoy-line", "convoy", 2, 2, .5, 1.25, 0, 0),
                new IndustryView.Line("ship-line", "custom/destroyer", 3, 3, .7, 5, 0, 0));
        var rows = IndustryLogisticsRows.create(snapshot, lines, "all");
        assertEquals(List.of("convoy", "harbor", "train", "truck", "rifle", "plane"), rows.stream().map(r -> r.representative().id()).toList());
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i); var item = snapshot.get(i);
            assertEquals(item.stockpile(), row.stockpile());
            assertEquals(item.reserved(), row.reserved());
            assertEquals(item.deployed(), row.deployed());
            assertEquals(item.demand(), row.demand());
            assertEquals(item.stockpile() - item.demand(), row.balance().longValue());
        }
        assertEquals(1.25, rows.getFirst().daily());
        assertEquals(List.of("convoy", "harbor"), IndustryLogisticsRows.create(snapshot, lines, "navy").stream().map(r -> r.representative().id()).toList());
        assertEquals(List.of(plane), IndustryLogisticsRows.create(snapshot, lines, "air").getFirst().models());
        assertEquals(input, snapshot);
        assertEquals(12, snapshot.get(6).stockpile());
        assertEquals(7, snapshot.get(6).deployed());
        assertEquals(2, lines.size());
    }
    @Test void unknownNavalFamiliesAndLegacyMissingFamiliesAreNotGuessedFromNames() {
        var legacy = branchEquipment("legacy", "navy", true, null, 10, 0, 0, 0);
        var unknown = branchEquipment("destroyer", "navy", true, "unknown_support", 5, 0, 0, 0);
        assertEquals(List.of(legacy, unknown), IndustryLogisticsRows.create(List.of(legacy, unknown), List.of(), "all")
                .stream().map(IndustryLogisticsRows.IndustryLogisticsRow::representative).toList());
    }
    private static IndustryView.Equipment branchEquipment(String id, String group, boolean naval, String family, long stock, long reserved, long deployed, long demand) {
        return new IndustryView.Equipment(id, id, "image/" + id, group, naval, true, 1, 10,
                Map.of(), stock, reserved, deployed, 0, "", family, id, demand, true);
    }
    private static IndustryView.Equipment equipment(String id, boolean unlocked, long stock, String replacement, String family) {
        return new IndustryView.Equipment(id, id, "image/" + id, "infantry", false, unlocked, 1, 1,
                Map.of(), stock, 0, 0, 0, replacement, family);
    }
}
