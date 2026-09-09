package dev.hoi.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PresentationViewTest {
    @Test void hudDetailsAreOptionalBoundedAndKeepRecordedZero() {
        var json = new Gson();
        var detail = new HudDetail(List.of(new HudDetail.Row("매일 획득", "0", HudDetail.Tone.NORMAL),
                new HudDetail.Row("금리", "—", HudDetail.Tone.MUTED)));
        var hud = new CountryHud(null, null, null, null, null, null, false, null,
                CountryHud.NationalIndicators.UNKNOWN, Map.of("political_power", detail));
        assertEquals(hud, json.fromJson(json.toJson(hud), CountryHud.class));
        var legacy = JsonParser.parseString(json.toJson(hud)).getAsJsonObject(); legacy.remove("details");
        assertTrue(json.fromJson(legacy, CountryHud.class).details().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> detail.rows().clear());
        assertThrows(IllegalArgumentException.class, () -> new HudDetail(Collections.nCopies(65, detail.rows().getFirst())));
        assertThrows(IllegalArgumentException.class, () -> new HudDetail.Row("줄\n바꿈", "0", HudDetail.Tone.NORMAL));
        assertThrows(IllegalArgumentException.class, () -> new HudDetail.Row("값", "x".repeat(81), HudDetail.Tone.NORMAL));
    }
    @Test void completionPresentationSurvivesIssuingAndLegacyJson() {
        var view = new DialogView("", 0, DialogView.Kind.DIPLOMACY, "발전된 광학장비", "", "효과\n해금",
                "", "technology/tfr/technology/example", "", List.of(), List.of(new DialogView.Choice("ack", "확인")), "research_complete");
        var issued = view.issued(UUID.randomUUID().toString(), 2);
        assertTrue(issued.completion());
        assertEquals(issued, DialogProtocol.Show.of(issued, true).view());
        var legacy = JsonParser.parseString(DialogProtocol.Show.of(issued, true).json()).getAsJsonObject(); legacy.remove("presentation");
        assertFalse(new DialogProtocol.Show(legacy.toString(), true).view().completion());
        legacy.addProperty("presentation", "invented");
        assertThrows(RuntimeException.class, () -> new DialogProtocol.Show(legacy.toString(), true).view());
    }
}
