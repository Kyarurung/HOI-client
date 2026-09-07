package dev.hoi.client;

import dev.hoi.protocol.ResearchView;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchLayoutTest {
    private ResearchView.Tech tech(String id, int year) {
        return new ResearchView.Tech(id, "INFANTRY", "보병 " + id, year, 1, 100, 0, 1,
                List.of(), List.of(), List.of(), ResearchView.Status.AVAILABLE);
    }
    @Test void denseSameYearTreesNeverOverlapAndPickingUsesContentCoordinates() {
        var list = new ArrayList<ResearchView.Tech>();
        for (int i = 0; i < 300; i++) list.add(tech("hoi:tech/" + i, 2000 + i % 10));
        var layout = ResearchLayout.create(list, "INFANTRY", "");
        assertEquals(300, layout.nodes().size());
        for (var node : layout.nodes()) {
            assertSame(node, layout.at(node.x() + 20, node.y() + 20));
            assertEquals(1, layout.nodes().stream().filter(n -> n.contains(node.x() + 20, node.y() + 20)).count());
        }
        Collections.reverse(list);
        assertEquals(layout, ResearchLayout.create(list, "INFANTRY", ""));
        assertEquals(0, ResearchLayout.clampScroll(-40, 1000, 400));
        assertEquals(600, ResearchLayout.clampScroll(9999, 1000, 400));
        assertEquals(0, ResearchLayout.clampScroll(100, 100, 400));
    }
    @Test void filtersKeepCanonicalTechnologyIdentityAndEmptyTreesRemainScrollable() {
        var technologies = List.of(tech("hoi:a", 2020), tech("hoi:b", 2030));
        var filtered = ResearchLayout.create(technologies, "INFANTRY", "HOI:B");
        assertEquals(List.of(2030), filtered.years());
        assertSame(technologies.get(1), filtered.nodes().getFirst().tech());
        assertTrue(ResearchLayout.create(technologies, "NAVY", "").nodes().isEmpty());
        assertNull(filtered.at(0, 0));
    }
    @Test void screenshotTimelinesKeepEmptyYearsAndIncludeCustomCatalogYears() {
        var infantry = ResearchLayout.create(List.of(tech("hoi:modern", 2006)), "INFANTRY", "");
        assertEquals(List.of(2000, 2006, 2012, 2018, 2021, 2024, 2027, 2030, 2033, 2036), infantry.years());
        assertEquals(20 + ResearchLayout.COLUMN, infantry.nodes().getFirst().x());
        var armor = ResearchLayout.create(List.of(), "ARMOR", "");
        assertEquals(List.of(1980, 2018, 2020, 2022, 2024, 2028, 2032), armor.years());
        assertTrue(armor.nodes().isEmpty());
        var custom = ResearchLayout.create(List.of(tech("hoi:future", 2045)), "INFANTRY", "");
        assertEquals(2045, custom.years().getLast());
        assertEquals(20 + 10 * ResearchLayout.COLUMN, custom.nodes().getFirst().x());
        assertEquals(armor, ResearchLayout.create(List.of(tech("hoi:foreign_category", 2020)), "ARMOR", ""));
    }
}
