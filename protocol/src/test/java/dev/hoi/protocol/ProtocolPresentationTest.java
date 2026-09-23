package dev.hoi.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolPresentationTest {
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
        var source = new HudDetail(detail.rows(), "§e현재 경험치: §f0\n\n£pol_idea §a+0.1");
        assertEquals(source, json.fromJson(json.toJson(source), HudDetail.class));
        assertNull(detail.sourceText());
        assertThrows(IllegalArgumentException.class, () -> new HudDetail(List.of(), "x".repeat(12001)));
        assertThrows(IllegalArgumentException.class, () -> new HudDetail(List.of(), "숨김\u0000문자"));
        assertThrows(IllegalArgumentException.class, () -> new CountryHud(null, null, null, null, null, null, false, null,
                CountryHud.NationalIndicators.UNKNOWN, Map.of("a", new HudDetail(List.of(), "x".repeat(12000)),
                "b", new HudDetail(List.of(), "x".repeat(12000)), "c", new HudDetail(List.of(), "x"))));
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
    @Test void buildingDamageIsOptionalBoundedAndSurvivesTransport() {
        var gson=new Gson();
        var tile=new DialogView.Tile("shared","민간공장","3/25","construction/civilian_factory","",new DialogView.Gauge(.35,"레벨 내구도: §e65.00%§r"),"map_detail/repairing");
        assertEquals(tile,gson.fromJson(gson.toJson(tile),DialogView.Tile.class));
        var legacy=gson.toJsonTree(tile).getAsJsonObject();legacy.remove("gauge");
        assertNull(gson.fromJson(legacy,DialogView.Tile.class).gauge());
        legacy.remove("overlay");
        assertEquals("",gson.fromJson(legacy,DialogView.Tile.class).overlay());
        for(double invalid:new double[]{-.01,1.01,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->new DialogView.Gauge(invalid,""));
    }
    @Test void relationFlagsKeepOrderAndLegacyResponsesRemainReadable() {
        var gson=new Gson();
        var relation=new CountryView.Relation("세력","diplomacy/relations/faction",List.of(new CountryView.Choice("JAP","일본"),new CountryView.Choice("KOR","대한민국")));
        var reports=Arrays.stream(IntelDomain.values()).map(d->new CountryView.Report(d,0,List.of())).toList();
        var view=new CountryView("KOR",CountryHud.UNKNOWN,"JAP","일본",false,List.of(),List.of(),List.of(),reports,null,List.of(relation));
        assertEquals(view,gson.fromJson(gson.toJson(view),CountryView.class));
        var legacy=gson.toJsonTree(view).getAsJsonObject();legacy.remove("relations");
        assertTrue(gson.fromJson(legacy,CountryView.class).relations().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new CountryView.Relation("", "../invalid",List.of()));
        assertThrows(IllegalArgumentException.class,()->new CountryView.Relation("", "faction",List.of(relation.countries().getFirst(),relation.countries().getFirst())));
    }

}
