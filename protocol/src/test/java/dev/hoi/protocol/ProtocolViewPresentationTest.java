package dev.hoi.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolViewPresentationTest {
    @Test void hudDetailsAreOptionalBoundedAndKeepRecordedZero() {
        var json = new Gson();
        var detail = new ProtocolHudDetail(List.of(new ProtocolHudDetail.Row("매일 획득", "0", ProtocolHudDetail.Tone.NORMAL),
                new ProtocolHudDetail.Row("금리", "—", ProtocolHudDetail.Tone.MUTED)));
        var hud = new ProtocolCountryHud(null, null, null, null, null, null, false, null,
                ProtocolCountryHud.NationalIndicators.UNKNOWN, Map.of("political_power", detail));
        assertEquals(hud, json.fromJson(json.toJson(hud), ProtocolCountryHud.class));
        var legacy = JsonParser.parseString(json.toJson(hud)).getAsJsonObject(); legacy.remove("details");
        assertTrue(json.fromJson(legacy, ProtocolCountryHud.class).details().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> detail.rows().clear());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolHudDetail(Collections.nCopies(65, detail.rows().getFirst())));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolHudDetail.Row("줄\n바꿈", "0", ProtocolHudDetail.Tone.NORMAL));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolHudDetail.Row("값", "x".repeat(81), ProtocolHudDetail.Tone.NORMAL));
        var source = new ProtocolHudDetail(detail.rows(), "§e현재 경험치: §f0\n\n£pol_idea §a+0.1");
        assertEquals(source, json.fromJson(json.toJson(source), ProtocolHudDetail.class));
        assertNull(detail.sourceText());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolHudDetail(List.of(), "x".repeat(12001)));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolHudDetail(List.of(), "숨김\u0000문자"));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolCountryHud(null, null, null, null, null, null, false, null,
                ProtocolCountryHud.NationalIndicators.UNKNOWN, Map.of("a", new ProtocolHudDetail(List.of(), "x".repeat(12000)),
                "b", new ProtocolHudDetail(List.of(), "x".repeat(12000)), "c", new ProtocolHudDetail(List.of(), "x"))));
    }
    @Test void completionPresentationSurvivesIssuingAndLegacyJson() {
        var view = new ProtocolDialogView("", 0, ProtocolDialogView.Kind.DIPLOMACY, "발전된 광학장비", "", "효과\n해금",
                "", "technology/tfr/technology/example", "", List.of(), List.of(new ProtocolDialogView.Choice("ack", "확인")), "research_complete");
        var issued = view.issued(UUID.randomUUID().toString(), 2);
        assertTrue(issued.completion());
        assertEquals(issued, ProtocolDialogPayloads.Show.of(issued, true).view());
        var legacy = JsonParser.parseString(ProtocolDialogPayloads.Show.of(issued, true).json()).getAsJsonObject(); legacy.remove("presentation");
        assertFalse(new ProtocolDialogPayloads.Show(legacy.toString(), true).view().completion());
        legacy.addProperty("presentation", "invented");
        assertThrows(RuntimeException.class, () -> new ProtocolDialogPayloads.Show(legacy.toString(), true).view());
    }
    @Test void dialogChoicesCarryAnOptionalBoundedTooltip() {
        var gson = new Gson();
        var plain = new ProtocolDialogView.Choice("korea.21.a", "북쪽을 바라본다.");
        assertEquals("", plain.tooltip());
        assertEquals(plain, new ProtocolDialogView.Choice("korea.21.a", "북쪽을 바라본다.", null));
        var described = new ProtocolDialogView.Choice("korea.21.a", "북쪽을 바라본다.", "'§Y북쪽을 바라보다§!' 사건 발생\n£pol_power 정치력 획득: §G+60§!.");
        var view = new ProtocolDialogView("", 0, ProtocolDialogView.Kind.EVENT, "동쪽을 바라보다", "대한민국", "본문", "", "", "", List.of(), List.of(described))
                .issued(UUID.randomUUID().toString(), 1);
        assertEquals(view, ProtocolDialogPayloads.Show.of(view, true).view());
        assertEquals(described, ProtocolDialogPayloads.Show.of(view, true).view().choices().getFirst());
        var legacy = gson.toJsonTree(described).getAsJsonObject(); legacy.remove("tooltip");
        assertEquals(plain, gson.fromJson(legacy, ProtocolDialogView.Choice.class));
        assertEquals(3000, new ProtocolDialogView.Choice("a", "b", "x".repeat(3000)).tooltip().length());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDialogView.Choice("a", "b", "x".repeat(3001)));
    }
    @Test void buildingDamageIsOptionalBoundedAndSurvivesTransport() {
        var gson=new Gson();
        var tile=new ProtocolDialogView.Tile("shared","민간공장","3/25","construction/civilian_factory","",new ProtocolDialogView.Gauge(.35,"레벨 내구도: §e65.00%§r"),"map_detail/repairing");
        assertEquals(tile,gson.fromJson(gson.toJson(tile),ProtocolDialogView.Tile.class));
        var legacy=gson.toJsonTree(tile).getAsJsonObject();legacy.remove("gauge");
        assertNull(gson.fromJson(legacy,ProtocolDialogView.Tile.class).gauge());
        legacy.remove("overlay");
        assertEquals("",gson.fromJson(legacy,ProtocolDialogView.Tile.class).overlay());
        for(double invalid:new double[]{-.01,1.01,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->new ProtocolDialogView.Gauge(invalid,""));
    }
    @Test void relationFlagsKeepOrderAndLegacyResponsesRemainReadable() {
        var gson=new Gson();
        var relation=new ProtocolCountryView.Relation("세력","diplomacy/relations/faction",List.of(new ProtocolCountryView.Choice("JAP","일본"),new ProtocolCountryView.Choice("KOR","대한민국")));
        var reports=Arrays.stream(ProtocolIntelDomain.values()).map(d->new ProtocolCountryView.Report(d,0,List.of())).toList();
        var view=new ProtocolCountryView("KOR",ProtocolCountryHud.UNKNOWN,"JAP","일본",false,List.of(),List.of(),List.of(),reports,null,List.of(relation));
        assertEquals(view,gson.fromJson(gson.toJson(view),ProtocolCountryView.class));
        var legacy=gson.toJsonTree(view).getAsJsonObject();legacy.remove("relations");
        assertTrue(gson.fromJson(legacy,ProtocolCountryView.class).relations().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new ProtocolCountryView.Relation("", "../invalid",List.of()));
        assertThrows(IllegalArgumentException.class,()->new ProtocolCountryView.Relation("", "faction",List.of(relation.countries().getFirst(),relation.countries().getFirst())));
    }
}
