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
    @Test void hoverShowsTheSampledDateAndTheSumUntilToday() {
        var section = new CountryView.Section("graph_convoys", "shown", "", "", List.of(new CountryView.Row("days", true, "", "2020-02-13", "2020-01-02 2020-01-16 2020-01-30 2020-02-13", ""),
                new CountryView.Row("convoys_produced", false, "§G생산한 수송선:§! $NUM|H$ (오늘까지 $UNTIL_TODAY|H$)", "4", "1 2 3 4", ""),
                new CountryView.Row("convoys_lost", false, "§R손실된 수송선:§! $NUM|H$ (오늘까지 $UNTIL_TODAY|H$)", "0", "5 0 0 0", "")));
        var chart = AgencyIntelChart.read(section, 2);
        assertEquals("§H2020년 1월 16일§!\n\n§G생산한 수송선:§! §H2§! (오늘까지 §H9§!)\n§R손실된 수송선:§! §H0§! (오늘까지 §H0§!)", chart.tooltip(.34));
        assertEquals("§H2020년 2월 13일§!\n\n§G생산한 수송선:§! §H4§! (오늘까지 §H4§!)\n§R손실된 수송선:§! §H0§! (오늘까지 §H0§!)", chart.tooltip(1));
        assertTrue(chart.tooltip(-.01).isEmpty() && chart.tooltip(1.01).isEmpty());
        assertTrue(AgencyIntelChart.read(new CountryView.Section("graph_trucks", "no_intel", "", "", List.of()), 0).tooltip(.5).isEmpty());
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
