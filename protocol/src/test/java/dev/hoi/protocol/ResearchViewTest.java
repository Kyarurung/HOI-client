package dev.hoi.protocol;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchViewTest {
    @Test void privateSnapshotRoundTripsAndEstimatesAccountForBankedDays() {
        var tech = new ResearchView.Tech("hoi:tech", "INFANTRY", "보병 연구", 2020, 1, 100, 20, 2,
                List.of("hoi:prerequisite"), List.of("연구 속도 +5%"), List.of("장비"), ResearchView.Status.ACTIVE);
        var view = new ResearchView("test-session", 2, "KOR", "대한민국", 42, "2020-02-12", "X5",
                List.of(new ResearchView.Slot(0, tech.id(), 10)), List.of(tech), "");
        assertEquals(view, ResearchProtocol.Response.of(view).view());
        assertEquals(30, tech.remainingDays(10));
        assertEquals(.2, tech.fraction(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> new ResearchProtocol.Response("x".repeat(ResearchProtocol.MAX_JSON + 1)));
    }
}
