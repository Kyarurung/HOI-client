package dev.hoi.protocol;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolResearchViewTest {
    @Test void localizedNamesAreOptionalBoundedPresentationMetadata() {
        var source = new ProtocolResearchView.Source("infantry_weapons2", "infantry_folder", 0, 0, false,
                List.of(), List.of(), List.of(), List.of("현대 보병장비"), "", List.of(), java.util.Map.of("현대 보병장비", "K2 돌격소총"));
        var gson = new com.google.gson.Gson();
        assertEquals(source, gson.fromJson(gson.toJson(source), ProtocolResearchView.Source.class));
        var json = com.google.gson.JsonParser.parseString(gson.toJson(source)).getAsJsonObject();
        json.remove("localizedNames");
        assertTrue(gson.fromJson(json, ProtocolResearchView.Source.class).localizedNames().isEmpty());
    }
    @Test void privateSnapshotRoundTripsAndEstimatesAccountForBankedDays() {
        var tech = new ProtocolResearchView.Tech("hoi:tech", "INFANTRY", "보병 연구", 2020, 1, 100, 20, 2,
                List.of("hoi:prerequisite"), List.of("연구 속도 +5%"), List.of("장비"), ProtocolResearchView.Status.ACTIVE);
        var view = new ProtocolResearchView("test-session", 2, "KOR", "대한민국", 42, "2020-02-12", "X5",
                List.of(new ProtocolResearchView.Slot(0, tech.id(), 10)), List.of(tech), "");
        assertEquals(view, ProtocolResearchPayloads.Response.of(view).view());
        assertEquals(30, tech.remainingDays(10));
        assertEquals(.2, tech.fraction(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> new ProtocolResearchPayloads.Response("x".repeat(ProtocolResearchPayloads.MAX_JSON + 1)));
        var hud = new ProtocolCountryHud(1.0, 2.0, 3.0, null, null, .5, true, 0L);
        var withHud = new ProtocolResearchView(view.session(), view.revision(), view.country(), view.countryName(), view.day(), view.date(),
                view.speed(), view.slots(), view.technologies(), view.message(), hud);
        assertEquals(hud, ProtocolResearchPayloads.Response.of(withHud).view().hud());
        var legacy = com.google.gson.JsonParser.parseString(ProtocolResearchPayloads.Response.of(view).json()).getAsJsonObject(); legacy.remove("hud");
        assertEquals(ProtocolCountryHud.UNKNOWN, new ProtocolResearchPayloads.Response(legacy.toString()).view().hud());
    }
}
