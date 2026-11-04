package dev.hoi.protocol.country;

import dev.hoi.protocol.agency.AgencyIntelDomain;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CountryJustificationTest {
    @Test void justificationDetailsRoundTripAndLegacySnapshotsRemainCompatible() {
        var relation = new CountryView.Relation("전쟁 명분 정당화", "diplomacy/relations/timed_wargoal_receiver",
                List.of(new CountryView.Choice("PRK", "조선민주주의인민공화국")), "북한 → 남한\n126일 남음");
        var view = new CountryView("PRK", CountryHud.UNKNOWN, "KOR", "대한민국", false,
                List.of(), List.of(), List.of(), Arrays.stream(AgencyIntelDomain.values())
                .map(d -> new CountryView.Report(d, 0, List.of())).toList(), null, List.of(relation));
        var packet = CountryPayloads.Response.of(UUID.randomUUID().toString(), view);
        assertEquals(view, packet.view());
        var gson = new Gson();
        var json = gson.toJsonTree(view).getAsJsonObject();
        var row = json.getAsJsonArray("relations").get(0).getAsJsonObject();
        var oldReader = gson.fromJson(row, LegacyRelation.class);
        assertEquals(relation.name(), oldReader.name());
        assertEquals(relation.countries(), oldReader.countries());
        row.remove("detail");
        var legacy = new CountryPayloads.Response(packet.token(), gson.toJson(json)).view();
        assertEquals("", legacy.relations().getFirst().detail());
        assertEquals(relation.countries(), legacy.relations().getFirst().countries());
        assertEquals("", new CountryView.Relation("관계", "diplomacy/relations/faction", List.of()).detail());
    }
    @Test void detailAndExistingFlagBoundsAreEnforced() {
        assertThrows(IllegalArgumentException.class, () -> new CountryView.Relation("", "diplomacy/relations/wargoal", List.of(), "x".repeat(1001)));
        assertThrows(IllegalArgumentException.class, () -> new CountryView.Relation("", "../wargoal", List.of(), ""));
        assertThrows(IllegalArgumentException.class, () -> new CountryView.Relation("", "diplomacy/relations/wargoal",
                List.of(new CountryView.Choice("PRK", "북한"), new CountryView.Choice("PRK", "북한")), ""));
    }
    private record LegacyRelation(String name, String icon, List<CountryView.Choice> countries) {}
}
