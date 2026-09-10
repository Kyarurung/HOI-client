package dev.hoi.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class PayloadDecodeTest {
    @ParameterizedTest
    @ValueSource(strings = {"null", "", "[", "[]", "\"invalid\""})
    void invalidSnapshotsFailThroughTheHandledExceptionContract(String json) {
        List<Function<String, Object>> readers = List.of(
                value -> new ResearchProtocol.Response(value).view(),
                value -> new AgencyProtocol.Response(value).view(),
                value -> new DialogProtocol.Show(value, true).view(),
                value -> new ConstructionProtocol.Response(value).view(),
                value -> new IndustryProtocol.Response(value).view(),
                value -> new MenuProtocol.OpenScreen(value).view(),
                value -> new MenuProtocol.SelectionPreview(value).view(),
                value -> new CountryProtocol.Response("test", value).view(),
                value -> new WorldTensionProtocol.Response("test", value).view(),
                value -> new HudProtocol.State("KOR", value).hud());
        for (var reader : readers) assertThrows(IllegalArgumentException.class, () -> reader.apply(json));
    }
    @Test void invalidResearchEnvelopesAreRejectedBeforeTheyReachTheServer() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchProtocol.Request(null, "", 0, -1, ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchProtocol.Request(ResearchProtocol.Action.OPEN, "x".repeat(37), 0, -1, ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchProtocol.Request(ResearchProtocol.Action.START, "", -1, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchProtocol.Request(ResearchProtocol.Action.START, "", 0, -2, ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchProtocol.Request(ResearchProtocol.Action.START, "", 0, 0, "x".repeat(161)));
        assertEquals(CountryHud.UNKNOWN, HudProtocol.State.HIDDEN.hud());
    }
}
