package dev.hoi.protocol;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryViewTest {
    @Test void requestsRejectMalformedAmountsAndIdentifiersWithoutAnArbitraryFactoryCap() {
        assertEquals(Integer.MAX_VALUE, new IndustryProtocol.Request(IndustryProtocol.Action.ASSIGN, "session", 1, "line", "", Integer.MAX_VALUE).amount());
        assertThrows(IllegalArgumentException.class, () -> new IndustryProtocol.Request(IndustryProtocol.Action.ASSIGN, "session", 1, "line", "", -1));
        assertThrows(IllegalArgumentException.class, () -> new IndustryProtocol.Request(null, "session", 1, "", "", 0));
        assertThrows(IllegalArgumentException.class, () -> new IndustryProtocol.Request(IndustryProtocol.Action.OPEN, "s".repeat(37), 0, "", "", 0));
        assertThrows(IllegalArgumentException.class, () -> new IndustryProtocol.Request(IndustryProtocol.Action.ADD, "s", -1, "", "", 0));
        assertThrows(IllegalArgumentException.class, () -> new IndustryProtocol.Request(IndustryProtocol.Action.ADD, "s", 0, "x".repeat(129), "", 0));
    }

    @Test void privateSnapshotsRemainBoundedAndRevocationRoundTrips() {
        var revoked = IndustryView.revoked("session", "권한이 변경되었습니다.");
        assertEquals(revoked, IndustryProtocol.Response.of(revoked).view());
        assertNull(revoked.economy());
        assertThrows(IllegalArgumentException.class, () -> new IndustryProtocol.Response("x".repeat(800001)));
        assertThrows(IllegalArgumentException.class, () -> new IndustryProtocol.Response("null").view());
        assertThrows(IllegalArgumentException.class, () -> new IndustryView.Template("t", "t", Collections.nCopies(26, "INFANTRY"), List.of(), List.of(), Map.of(), 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new IndustryView.Template("t", "t", List.of("INFANTRY"), Collections.nCopies(6, "ENGINEER"), List.of(), Map.of(), 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new IndustryView.Template("t", "t", List.of("INFANTRY"), List.of(), List.of(), Map.of(), Double.NaN, 1));
        var supplied = new java.util.ArrayList<>(List.of("INFANTRY"));
        var template = new IndustryView.Template("t", "보병사단", supplied, List.of(), List.of(), Map.of("gear", 900L), 180, 1000);
        supplied.clear();
        assertEquals(List.of("INFANTRY"), template.line());
        assertThrows(UnsupportedOperationException.class, () -> template.equipment().put("gear", 0L));
    }
}
