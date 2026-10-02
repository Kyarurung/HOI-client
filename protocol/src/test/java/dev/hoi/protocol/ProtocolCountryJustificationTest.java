package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCountryJustificationTest {
    @Test void justificationDetailsRoundTripAndLegacySnapshotsRemainCompatible() {
        var relation = new ProtocolCountryView.Relation("전쟁 명분 정당화", "diplomacy/relations/timed_wargoal_receiver",
                List.of(new ProtocolCountryView.Choice("PRK", "조선민주주의인민공화국")), "북한 → 남한\n126일 남음");
        var view = new ProtocolCountryView("PRK", ProtocolCountryHud.UNKNOWN, "KOR", "대한민국", false,
                List.of(), List.of(), List.of(), Arrays.stream(ProtocolIntelDomain.values())
                .map(d -> new ProtocolCountryView.Report(d, 0, List.of())).toList(), null, List.of(relation));
        var packet = ProtocolCountryPayloads.Response.of(UUID.randomUUID().toString(), view);
        assertEquals(view, packet.view());
        var gson = new Gson();
        var json = gson.toJsonTree(view).getAsJsonObject();
        var row = json.getAsJsonArray("relations").get(0).getAsJsonObject();
        var oldReader = gson.fromJson(row, LegacyRelation.class);
        assertEquals(relation.name(), oldReader.name());
        assertEquals(relation.countries(), oldReader.countries());
        row.remove("detail");
        var legacy = new ProtocolCountryPayloads.Response(packet.token(), gson.toJson(json)).view();
        assertEquals("", legacy.relations().getFirst().detail());
        assertEquals(relation.countries(), legacy.relations().getFirst().countries());
        assertEquals("", new ProtocolCountryView.Relation("관계", "diplomacy/relations/faction", List.of()).detail());
    }
    @Test void detailAndExistingFlagBoundsAreEnforced() {
        assertThrows(IllegalArgumentException.class, () -> new ProtocolCountryView.Relation("", "diplomacy/relations/wargoal", List.of(), "x".repeat(1001)));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolCountryView.Relation("", "../wargoal", List.of(), ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolCountryView.Relation("", "diplomacy/relations/wargoal",
                List.of(new ProtocolCountryView.Choice("PRK", "북한"), new ProtocolCountryView.Choice("PRK", "북한")), ""));
    }
    private record LegacyRelation(String name, String icon, List<ProtocolCountryView.Choice> countries) {}
}
