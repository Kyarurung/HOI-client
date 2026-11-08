package dev.hoi.client.screen.agency;

import dev.hoi.protocol.country.CountryView;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgencyIntelChartTest {
    @Test void periodsKeepDatesValuesAndVisibleMaximumTogether() {
        String days = String.join(" ", IntStream.range(0, 79).mapToObj(i -> LocalDate.of(2020, 1, 2).plusDays(i * 14L).toString()).toList());
        String values = String.join(" ", IntStream.range(0, 79).mapToObj(i -> i == 0 ? "999" : String.valueOf(i)).toList());
        var section = section(days, values);
        var shortHistory = AgencyIntelChart.read(section, 0);
        assertEquals(7, shortHistory.days().size());
        assertEquals(72, shortHistory.series().getFirst().values().getFirst());
        assertEquals(78, shortHistory.maximum());
        assertEquals(27, AgencyIntelChart.read(section, 1).days().size());
        assertEquals(79, AgencyIntelChart.read(section, 2).days().size());
        assertEquals(999, AgencyIntelChart.read(section, 2).maximum());
    }
    @Test void missingMismatchedAndNonFiniteHistoryStaysEmpty() {
        assertTrue(AgencyIntelChart.read(section("2020-01-02 2020-01-16", "1"), 0).days().isEmpty());
        assertTrue(AgencyIntelChart.read(section("2020-01-02", "NaN"), 0).days().isEmpty());
        assertTrue(AgencyIntelChart.read(section("invalid", "1"), 0).days().isEmpty());
        assertTrue(AgencyIntelChart.read(new CountryView.Section("graph_trucks", "no_intel", "", "", List.of()), 0).series().isEmpty());
    }
    private static CountryView.Section section(String days, String values) {
        return new CountryView.Section("graph_trucks", "shown", "", "", List.of(
                new CountryView.Row("days", true, "", "2020-01-02", days, ""),
                new CountryView.Row("trucks", false, "", "0", values, "")));
    }
}
