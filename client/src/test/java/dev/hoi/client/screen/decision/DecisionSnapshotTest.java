package dev.hoi.client.screen.decision;

import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.decision.DecisionPayloads;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static dev.hoi.client.screen.decision.DecisionSnapshot.Acceptance.*;
import static org.junit.jupiter.api.Assertions.*;

class DecisionSnapshotTest {
    private static final String SESSION = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_SESSION = "22222222-2222-2222-2222-222222222222";
    @Test void initialSnapshotWaitsForPageZeroAndAllPages() {
        var snapshot = new DecisionSnapshot(null);
        assertNull(snapshot.view());
        assertNull(snapshot.authority());
        assertFalse(snapshot.loading());
        assertEquals(0, snapshot.nextPage());
        assertTrue(snapshot.rows().isEmpty());
        assertTrue(snapshot.categories().isEmpty());
        var last = view(1, 1, 2, 2, List.of(row("last", "category")), List.of());
        assertEquals(IGNORED, snapshot.accept(last));
        assertNull(snapshot.authority());
        var first = view(1, 0, 2, 2, List.of(row("first", "category")), List.of(category("category")));
        assertEquals(STAGED, snapshot.accept(first));
        assertNull(snapshot.view());
        assertSame(first, snapshot.authority());
        assertTrue(snapshot.loading());
        assertTrue(snapshot.rows().isEmpty());
        assertTrue(snapshot.categories().isEmpty());
        assertEquals(1, snapshot.nextPage());
        assertEquals(APPLIED, snapshot.accept(last));
        assertSame(first, snapshot.view());
        assertSame(first, snapshot.authority());
        assertFalse(snapshot.loading());
        assertEquals(List.of("first", "last"), rowIds(snapshot));
    }
    @Test void reverseOrderPagesReplaceRowsAndCategoriesOnlyWhenComplete() {
        var original = initial(1);
        var snapshot = new DecisionSnapshot(original);
        var first = view(2, 0, 3, 3, List.of(row("first", "first_category")), List.of(category("first_category")));
        var middle = view(2, 1, 3, 3, List.of(row("middle", "middle_category")), List.of(category("middle_category")));
        var last = view(2, 2, 3, 3, List.of(row("last", "last_category")), List.of(category("last_category")));
        assertEquals(STAGED, snapshot.accept(first));
        assertEquals(STAGED, snapshot.accept(last));
        assertSame(original, snapshot.view());
        assertSame(first, snapshot.authority());
        assertEquals(original.rows(), snapshot.rows());
        assertEquals(original.categories(), snapshot.categories());
        assertEquals(1, snapshot.nextPage());
        assertEquals(APPLIED, snapshot.accept(middle));
        assertEquals(List.of("first", "middle", "last"), rowIds(snapshot));
        assertEquals(List.of("first_category", "middle_category", "last_category"), categoryIds(snapshot));
        assertSame(first, snapshot.view());
        assertSame(first, snapshot.authority());
        assertFalse(snapshot.loading());
        assertEquals(0, snapshot.nextPage());
    }
    @Test void equalAndOlderRevisionsCannotReplaceAnAppliedSnapshot() {
        var original = initial(5);
        var snapshot = new DecisionSnapshot(original);
        for (long revision : new long[]{4, 5}) {
            assertEquals(IGNORED, snapshot.accept(view(revision, 0, 1, 1,
                    List.of(row("stale", "stale_category")), List.of(category("stale_category")))));
            assertSame(original, snapshot.view());
            assertSame(original, snapshot.authority());
            assertEquals(original.rows(), snapshot.rows());
            assertEquals(original.categories(), snapshot.categories());
            assertFalse(snapshot.loading());
        }
    }
    @Test void aNewRevisionDiscardsPendingPagesAndRejectsDelayedOlderPages() {
        var original = initial(1);
        var snapshot = new DecisionSnapshot(original);
        var superseded = view(2, 0, 2, 2, List.of(row("superseded_first", "category")), List.of(category("category")));
        var delayed = view(2, 1, 2, 2, List.of(row("superseded_last", "category")), List.of());
        var first = view(3, 0, 2, 2, List.of(row("current_first", "next_leader_category")), List.of(category("next_leader_category")));
        var last = view(3, 1, 2, 2, List.of(row("current_last", "next_leader_category")), List.of());
        assertEquals(STAGED, snapshot.accept(superseded));
        assertEquals(IGNORED, snapshot.accept(last));
        assertSame(superseded, snapshot.authority());
        assertEquals(STAGED, snapshot.accept(first));
        assertEquals(IGNORED, snapshot.accept(delayed));
        assertEquals(IGNORED, snapshot.accept(superseded));
        assertSame(first, snapshot.authority());
        assertSame(original, snapshot.view());
        assertEquals(1, snapshot.nextPage());
        assertEquals(List.of("original_category"), categoryIds(snapshot));
        assertEquals(APPLIED, snapshot.accept(last));
        assertEquals(List.of("current_first", "current_last"), rowIds(snapshot));
        assertEquals(List.of("next_leader_category"), categoryIds(snapshot));
        assertEquals(IGNORED, snapshot.accept(delayed));
        assertEquals(List.of("next_leader_category"), categoryIds(snapshot));
    }
    @Test void repeatedPageCannotReplaceAlreadyStagedContent() {
        var snapshot = new DecisionSnapshot(initial(1));
        var first = view(2, 0, 2, 2, List.of(row("first", "category")), List.of(category("category")));
        assertEquals(STAGED, snapshot.accept(first));
        assertEquals(IGNORED, snapshot.accept(first));
        assertEquals(IGNORED, snapshot.accept(view(2, 0, 2, 2,
                List.of(row("replacement", "other_category")), List.of(category("other_category")))));
        assertSame(first, snapshot.authority());
        assertEquals(1, snapshot.nextPage());
        assertEquals(APPLIED, snapshot.accept(view(2, 1, 2, 2, List.of(row("last", "category")), List.of())));
        assertEquals(List.of("first", "last"), rowIds(snapshot));
        assertEquals(List.of("category"), categoryIds(snapshot));
    }
    @Test void anotherSessionCannotReplaceAppliedOrPendingAuthority() {
        var original = initial(1);
        var snapshot = new DecisionSnapshot(original);
        var another = new DecisionPayloads.View("KOR", OTHER_SESSION, 9, "", 0, 0,
                CountryHud.UNKNOWN, List.of(), List.of(), 1);
        assertEquals(IGNORED, snapshot.accept(another));
        assertSame(original, snapshot.authority());
        var first = view(2, 0, 2, 2, List.of(row("first", "category")), List.of(category("category")));
        assertEquals(STAGED, snapshot.accept(first));
        assertEquals(IGNORED, snapshot.accept(another));
        assertSame(first, snapshot.authority());
        assertSame(original, snapshot.view());
        assertEquals(1, snapshot.nextPage());
    }
    @Test void countryChangeRequestsRevocationWithoutPublishingForeignContent() {
        var original = initial(1);
        var snapshot = new DecisionSnapshot(original);
        var foreign = new DecisionPayloads.View("JAP", SESSION, 2, "", 0, 1,
                CountryHud.UNKNOWN, List.of(row("foreign", "foreign_category")), List.of(category("foreign_category")), 1);
        assertEquals(REVOKED, snapshot.accept(foreign));
        assertSame(original, snapshot.view());
        assertEquals(original.rows(), snapshot.rows());
        assertEquals(original.categories(), snapshot.categories());
        var first = view(2, 0, 2, 2, List.of(row("first", "category")), List.of(category("category")));
        assertEquals(STAGED, snapshot.accept(first));
        assertEquals(REVOKED, snapshot.accept(foreign));
        assertSame(first, snapshot.authority());
        assertSame(original, snapshot.view());
        assertEquals(original.rows(), snapshot.rows());
    }
    @Test void inconsistentPageMetadataDoesNotContaminateThePendingSnapshot() {
        var original = initial(1);
        var snapshot = new DecisionSnapshot(original);
        var first = view(2, 0, 2, 2, List.of(row("first", "category")), List.of(category("category")));
        var lastRow = List.of(row("last", "category"));
        assertEquals(STAGED, snapshot.accept(first));
        var changedHud = new CountryHud(1.0, null, null, null, null, null, false, null);
        var mismatches = List.of(
                view(2, 1, 3, 2, lastRow, List.of()),
                view(2, 1, 2, 3, lastRow, List.of()),
                new DecisionPayloads.View("KOR", SESSION, 2, "different", 1, 2,
                        CountryHud.UNKNOWN, lastRow, List.of(), 2),
                new DecisionPayloads.View("KOR", SESSION, 2, "", 1, 2,
                        changedHud, lastRow, List.of(), 2));
        for (var mismatch : mismatches) {
            assertEquals(IGNORED, snapshot.accept(mismatch));
            assertSame(first, snapshot.authority());
            assertSame(original, snapshot.view());
            assertEquals(original.rows(), snapshot.rows());
            assertEquals(1, snapshot.nextPage());
        }
        var equivalentHud = new CountryHud(null, null, null, null, null, null, false, null);
        assertNotSame(CountryHud.UNKNOWN, equivalentHud);
        assertEquals(CountryHud.UNKNOWN, equivalentHud);
        assertEquals(APPLIED, snapshot.accept(new DecisionPayloads.View("KOR", SESSION, 2, "", 1, 2,
                equivalentHud, lastRow, List.of(), 2)));
        assertEquals(List.of("first", "last"), rowIds(snapshot));
    }
    @Test void duplicateRowIdsAcrossPagesRejectTheWholeRevision() {
        var original = initial(1);
        var snapshot = new DecisionSnapshot(original);
        assertEquals(STAGED, snapshot.accept(view(2, 0, 2, 2,
                List.of(row("duplicate", "category")), List.of(category("category")))));
        assertEquals(IGNORED, snapshot.accept(view(2, 1, 2, 2,
                List.of(row("duplicate", "other_category")), List.of(category("other_category")))));
        assertRejected(snapshot, original);
        var recovered = initial(3);
        assertEquals(APPLIED, snapshot.accept(recovered));
        assertSame(recovered, snapshot.view());
    }
    @Test void duplicateCategoryIdsAcrossPagesRejectTheWholeRevision() {
        var original = initial(1);
        var snapshot = new DecisionSnapshot(original);
        assertEquals(STAGED, snapshot.accept(view(2, 0, 2, 2,
                List.of(row("first", "category")), List.of(category("category")))));
        assertEquals(IGNORED, snapshot.accept(view(2, 1, 2, 2,
                List.of(row("last", "category")), List.of(category("category")))));
        assertRejected(snapshot, original);
    }
    @Test void finalRowCountMustMatchTheDeclaredTotal() {
        for (int total : new int[]{1, 3}) {
            var original = initial(1);
            var snapshot = new DecisionSnapshot(original);
            assertEquals(STAGED, snapshot.accept(view(2, 0, 2, total,
                    List.of(row("first", "category")), List.of(category("category")))));
            assertEquals(IGNORED, snapshot.accept(view(2, 1, 2, total,
                    List.of(row("last", "category")), List.of())));
            assertRejected(snapshot, original);
        }
    }
    @Test void emptyCategoriesHaveTheirOwnPagesWithoutInventingDecisionRows() {
        var snapshot = new DecisionSnapshot(initial(1));
        var parliament = category("KOR_parliament");
        var cabinet = category("KOR_cabinet");
        var first = view(2, 0, 2, 0, List.of(), List.of(parliament));
        assertEquals(STAGED, snapshot.accept(first));
        assertEquals(1, snapshot.nextPage());
        assertEquals(APPLIED, snapshot.accept(view(2, 1, 2, 0, List.of(), List.of(cabinet))));
        assertTrue(snapshot.rows().isEmpty());
        assertEquals(List.of(parliament, cabinet), snapshot.categories());
        assertTrue(snapshot.categories().getFirst().visibleWhenEmpty());
        assertSame(first, snapshot.view());
    }
    @Test void legacyViewsWithoutCategoriesStillAssembleInferredRowPages() {
        var allRows = IntStream.range(0, 49).mapToObj(i -> row("legacy_" + i, "legacy_category")).toList();
        var first = new DecisionPayloads.View("KOR", SESSION, 1, "", 0, 49,
                CountryHud.UNKNOWN, allRows.subList(0, 48));
        var last = new DecisionPayloads.View("KOR", SESSION, 1, "", 1, 49,
                CountryHud.UNKNOWN, allRows.subList(48, 49), null, 0);
        var snapshot = new DecisionSnapshot(first);
        assertTrue(snapshot.loading());
        assertNull(snapshot.view());
        assertEquals(2, snapshot.authority().pageCount());
        assertEquals(1, snapshot.nextPage());
        assertEquals(APPLIED, snapshot.accept(last));
        assertEquals(allRows, snapshot.rows());
        assertTrue(snapshot.categories().isEmpty());
        assertFalse(snapshot.loading());
    }
    private static void assertRejected(DecisionSnapshot snapshot, DecisionPayloads.View original) {
        assertFalse(snapshot.loading());
        assertSame(original, snapshot.view());
        assertSame(original, snapshot.authority());
        assertEquals(original.rows(), snapshot.rows());
        assertEquals(original.categories(), snapshot.categories());
        assertEquals(0, snapshot.nextPage());
    }
    private static DecisionPayloads.View initial(long revision) {
        return view(revision, 0, 1, 1, List.of(row("original", "original_category")), List.of(category("original_category")));
    }
    private static DecisionPayloads.View view(long revision, int page, int pages, int total,
            List<DecisionPayloads.Row> rows, List<DecisionPayloads.Category> categories) {
        return new DecisionPayloads.View("KOR", SESSION, revision, "", page, total,
                CountryHud.UNKNOWN, rows, categories, pages);
    }
    private static DecisionPayloads.Row row(String id, String category) {
        return new DecisionPayloads.Row(id, category, category, "", id, "", "", 0, 0, "", true);
    }
    private static DecisionPayloads.Category category(String id) {
        return new DecisionPayloads.Category(id, id, "§Y원문§!\n\n설명", "", "", "", true);
    }
    private static List<String> rowIds(DecisionSnapshot snapshot) {
        return snapshot.rows().stream().map(DecisionPayloads.Row::id).toList();
    }
    private static List<String> categoryIds(DecisionSnapshot snapshot) {
        return snapshot.categories().stream().map(DecisionPayloads.Category::id).toList();
    }
}
