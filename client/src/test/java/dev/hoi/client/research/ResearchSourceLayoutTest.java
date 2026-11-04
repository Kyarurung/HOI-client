package dev.hoi.client.research;

import dev.hoi.protocol.research.ResearchPayloads;
import dev.hoi.protocol.research.ResearchView;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchSourceLayoutTest {
    private List<ResearchView.Tech> catalog() throws Exception {
        try (var in = getClass().getResourceAsStream("/tfr-research-view.json")) {
            return new ResearchPayloads.Response(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).view().technologies();
        }
    }
    private ResearchLayout.ResearchNode node(ResearchLayout layout, String id) {
        return layout.nodes().stream().filter(n -> n.tech().source().id().equals(id)).findFirst().orElseThrow();
    }
    @Test void electronicsAndIndustryKeepOriginalHeadingAnchors() throws Exception {
        for (String category : List.of("ENGINEERING", "INDUSTRY")) {
            var layout = ResearchLayout.create(catalog(), category, "");
            var labels = ResearchTreeLabels.create(layout, String::length, 9);
            for (var anchor : ResearchSourceLayout.headings(layout)) {
                var actual = labels.stream().filter(l -> !l.year() && l.text().equals(anchor.text())).findFirst().orElseThrow();
                assertEquals(Math.max(2, anchor.x() - anchor.text().length() / 2), actual.x(), category + ": " + anchor.text());
            }
        }
    }
    @Test void constructionKeepsItsOwnLaneAndSearchDoesNotReflowTheSource() throws Exception {
        var catalog = catalog();
        var layout = ResearchLayout.create(catalog, "INDUSTRY", "");
        assertEquals(60, layout.nodes().size());
        var construction = node(layout, "construction1");
        var rubber = node(layout, "rubber_processing");
        assertEquals(construction.tech().source().x(), rubber.tech().source().x());
        assertEquals(construction.tech().source().y(), rubber.tech().source().y());
        assertTrue(construction.x() < node(layout, "excavation1").x());
        assertTrue(node(layout, "excavation1").x() < node(layout, "oil_processing").x());
        assertEquals(construction.x(), node(layout, "construction9").x());
        assertEquals(node(layout, "concentrated_industry").y(), construction.y());
        var filtered = ResearchLayout.create(catalog, "INDUSTRY", "construction1");
        assertEquals(construction, filtered.nodes().getFirst());
        assertEquals(layout.width(), filtered.width());
        assertEquals(layout.height(), filtered.height());
    }
    @Test void allReferenceCardsRemainDistinctAndConnectionsDoNotCutThroughCards() throws Exception {
        var catalog = catalog();
        for (var category : List.of("INFANTRY", "SUPPORT", "ARMOR", "ARTILLERY", "NAVY", "NAVAL_SUPPORT", "AIR", "ENGINEERING", "INDUSTRY")) for (int viewport : List.of(407, 780, 1100, 1260)) {
            var layout = ResearchSourceLayout.fit(ResearchLayout.create(catalog, category, ""), viewport);
            assertTrue(ResearchSourceLayout.applies(layout));
            assertEquals(catalog.stream().filter(t -> t.source().parentTechnology().isEmpty() && ResearchLayout.category(t).equals(category)).count(), layout.nodes().size());
            for (var a : layout.nodes()) {
                assertNotNull(ResearchSourceLayout.position(a.tech()), a.tech().id());
                assertSame(a, layout.at(a.x() + a.width() / 2.0, a.y() + a.height() / 2.0));
                for (var b : layout.nodes()) if (a != b) assertFalse(a.x() < b.x() + b.width() && a.x() + a.width() > b.x()
                        && a.y() < b.y() + b.height() && a.y() + a.height() > b.y(), a.tech().id() + " overlaps " + b.tech().id());
            }
            var ids = layout.nodes().stream().map(n -> n.tech().id()).collect(java.util.stream.Collectors.toSet());
            int expected = layout.nodes().stream().mapToInt(n -> (int)ResearchLayout.parents(n.tech()).stream().filter(ids::contains).count()).sum();
            var connections = ResearchSourceLayout.connections(layout);
            assertEquals(expected, connections.size());
            for (var connection : connections) for (var s : connection.segments()) {
                assertTrue(s.x1() == s.x2() || s.y1() == s.y2());
                for (var n : layout.nodes()) {
                    boolean cuts = s.x1() == s.x2()
                            ? s.x1() > n.x() && s.x1() < n.x() + n.width() && Math.max(s.y1(), s.y2()) > n.y() && Math.min(s.y1(), s.y2()) < n.y() + n.height()
                            : s.y1() > n.y() && s.y1() < n.y() + n.height() && Math.max(s.x1(), s.x2()) > n.x() && Math.min(s.x1(), s.x2()) < n.x() + n.width();
                    assertFalse(cuts, connection.parent().id() + " line cuts " + n.tech().id() + ": " + s);
                }
            }
        }
    }
    @Test void armorUsesSideConnectionsAndSeparateHullAndAmmunitionTimelines() throws Exception {
        var layout = ResearchLayout.create(catalog(), "ARMOR", "");
        var hull = node(layout, "gwtank_chassis");
        var module = node(layout, "legacy_armor_designs");
        assertTrue(module.x() < hull.x());
        assertEquals(module.y() + module.height() / 2, hull.y() + hull.height() / 2);
        assertTrue(hull.width() > module.width());
        assertTrue(ResearchSourceLayout.iconOnly(module.tech()));
        var sameRow = ResearchSourceLayout.connections(layout).stream().filter(c -> c.parent().equals(hull.tech()))
                .filter(c -> c.segments().size() == 1 && c.segments().getFirst().y1() == c.segments().getFirst().y2()).toList();
        assertEquals(1, sameRow.size());
        var labels = ResearchSourceLayout.yearLabels(layout);
        assertTrue(labels.stream().anyMatch(l -> l.text().equals("2018년") && l.x() == 4));
        assertTrue(labels.stream().anyMatch(l -> l.text().equals("2000년") && l.x() > 700));
        var reversed = new ArrayList<>(catalog()); Collections.reverse(reversed);
        assertEquals(layout, ResearchLayout.create(reversed, "ARMOR", ""));
    }
}
