package dev.hoi.protocol;

import dev.hoi.protocol.agency.AgencyPayloads;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.country.CountryPayloads;
import dev.hoi.protocol.decision.DecisionPayloads;
import dev.hoi.protocol.dialog.DialogPayloads;
import dev.hoi.protocol.diplomacy.DiplomacyWorldTensionPayloads;
import dev.hoi.protocol.focus.FocusPayloads;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.industry.IndustryConstructionPayloads;
import dev.hoi.protocol.industry.IndustryPayloads;
import dev.hoi.protocol.menu.MenuPayloads;
import dev.hoi.protocol.research.ResearchPayloads;
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
                value -> new ResearchPayloads.Response(value).view(),
                value -> new AgencyPayloads.Response(value).view(),
                value -> new DialogPayloads.Show(value, true).view(),
                value -> new IndustryConstructionPayloads.Response(value).view(),
                value -> new IndustryPayloads.Response(value).view(),
                value -> new MenuPayloads.OpenScreen(value).view(),
                value -> new MenuPayloads.SelectionPreview(value).view(),
                value -> new CountryPayloads.Response(token, value).view(),
                value -> new DiplomacyWorldTensionPayloads.Response(token, value).view(),
                value -> new FocusPayloads.Response(token, value).view(),
                value -> new DecisionPayloads.Response(token, value).view(),
                value -> new HudPayloads.State("KOR", value).hud());
        for (var reader : readers) assertThrows(IllegalArgumentException.class, () -> reader.apply(json));
    }
    @Test void invalidResearchEnvelopesAreRejectedBeforeTheyReachTheServer() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchPayloads.Request(null, "", 0, -1, ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchPayloads.Request(ResearchPayloads.Action.OPEN, "x".repeat(37), 0, -1, ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchPayloads.Request(ResearchPayloads.Action.START, "", -1, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchPayloads.Request(ResearchPayloads.Action.START, "", 0, -2, ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchPayloads.Request(ResearchPayloads.Action.START, "", 0, 0, "x".repeat(161)));
        assertEquals(CountryHud.UNKNOWN, HudPayloads.State.HIDDEN.hud());
    }
}
