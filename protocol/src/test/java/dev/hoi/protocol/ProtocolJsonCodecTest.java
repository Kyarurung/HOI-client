package dev.hoi.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolJsonCodecTest {
    @ParameterizedTest
    @ValueSource(strings = {"null", "", "[", "[]", "\"invalid\""})
    void invalidSnapshotsFailThroughTheHandledExceptionContract(String json) {
        String token = "00000000-0000-0000-0000-000000000001";
        List<Function<String, Object>> readers = List.of(
                value -> new ProtocolResearchPayloads.Response(value).view(),
                value -> new ProtocolAgencyPayloads.Response(value).view(),
                value -> new ProtocolDialogPayloads.Show(value, true).view(),
                value -> new ProtocolConstructionPayloads.Response(value).view(),
                value -> new ProtocolIndustryPayloads.Response(value).view(),
                value -> new ProtocolMenuPayloads.OpenScreen(value).view(),
                value -> new ProtocolMenuPayloads.SelectionPreview(value).view(),
                value -> new ProtocolCountryPayloads.Response(token, value).view(),
                value -> new ProtocolWorldTension.Response(token, value).view(),
                value -> new ProtocolFocusPayloads.Response(token, value).view(),
                value -> new ProtocolDecisionPayloads.Response(token, value).view(),
                value -> new ProtocolHudPayloads.State("KOR", value).hud());
        for (var reader : readers) assertThrows(IllegalArgumentException.class, () -> reader.apply(json));
    }
    @Test void invalidResearchEnvelopesAreRejectedBeforeTheyReachTheServer() {
        assertThrows(IllegalArgumentException.class, () -> new ProtocolResearchPayloads.Request(null, "", 0, -1, ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolResearchPayloads.Request(ProtocolResearchPayloads.Action.OPEN, "x".repeat(37), 0, -1, ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolResearchPayloads.Request(ProtocolResearchPayloads.Action.START, "", -1, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolResearchPayloads.Request(ProtocolResearchPayloads.Action.START, "", 0, -2, ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolResearchPayloads.Request(ProtocolResearchPayloads.Action.START, "", 0, 0, "x".repeat(161)));
        assertEquals(ProtocolCountryHud.UNKNOWN, ProtocolHudPayloads.State.HIDDEN.hud());
    }
}
