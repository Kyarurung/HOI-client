package dev.hoi.protocol;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryViewTest {
    @Test void regimentColumnsPreserveHolesBetweenIndependentColumns() {
        var gson=new com.google.gson.Gson();
        var line=List.of("INFANTRY","INFANTRY","INFANTRY","INFANTRY");
        var template=new IndustryView.Template("draft","편제",line,List.of(),List.of(),Map.of(),0,0,List.of(1,0,3,0,0),Map.of(2,"ENGINEER"));
        assertEquals(template,gson.fromJson(gson.toJson(template),IndustryView.Template.class));
        assertEquals("",template.lineUnit(5));
        assertEquals("INFANTRY",template.lineUnit(12));
        assertEquals(1,template.lineIndex(10));
        var json=gson.toJsonTree(template).getAsJsonObject();json.remove("columns");json.remove("regimentSupport");
        var legacy=gson.fromJson(json,IndustryView.Template.class);
        assertNull(legacy.columns()); assertTrue(legacy.regimentSupport().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new IndustryView.Template("d","d",line,List.of(),List.of(),Map.of(),0,0,List.of(1,0,2,0,0),Map.of()));
        assertThrows(IllegalArgumentException.class,()->new IndustryView.Template("d","d",line,List.of(),List.of(),Map.of(),0,0,List.of(1,0,3,0,0),Map.of(0,"ENGINEER")));
    }
    @Test void equipmentGenerationsAreOptionalAndNeverInferredForLegacyServers() {
        var gson = new com.google.gson.Gson();
        var old = new IndustryView.Equipment("old","Old","old","infantry",false,true,1,4,Map.of(),5,2,1,0);
        var legacy = gson.fromJson(gson.toJson(old),IndustryView.Equipment.class);
        assertNull(legacy.demand()); assertNull(legacy.replacement()); assertNull(legacy.family()); assertFalse(legacy.outdated());
        var versioned = new IndustryView.Equipment("old","Old","old","infantry",false,true,1,4,Map.of(),5,2,1,0,"new","rifles");
        assertEquals(versioned,gson.fromJson(gson.toJson(versioned),IndustryView.Equipment.class));
        assertTrue(versioned.outdated());
        assertThrows(IllegalArgumentException.class,()->new IndustryView.Equipment("old","Old","old","infantry",false,true,1,4,Map.of(),5,2,1,0,"old","rifles"));
    }
    @Test void outstandingDemandPreservesZeroAndRejectsInvalidAmounts() {
        var gson = new com.google.gson.Gson();
        var equipment = new IndustryView.Equipment("rifle","Rifle","rifle","infantry",false,true,1,4,Map.of(),5,2,1,3,null,"infantry","보병 장비",8L);
        assertEquals(equipment, gson.fromJson(gson.toJson(equipment),IndustryView.Equipment.class));
        var json = gson.toJsonTree(equipment).getAsJsonObject();
        json.addProperty("demand",-1);
        assertThrows(RuntimeException.class, () -> gson.fromJson(json,IndustryView.Equipment.class));
        json.addProperty("demand",0);
        assertEquals(0L,gson.fromJson(json,IndustryView.Equipment.class).demand());
    }
    @Test void optionalRepairReportDistinguishesLegacyUnknownFromAnEmptyQueue() {
        var old = IndustryView.revoked("session", "");
        assertNull(IndustryProtocol.Response.of(old).view().navalRepairs());
        String legacy = IndustryProtocol.Response.of(old).json();
        var json = com.google.gson.JsonParser.parseString(legacy).getAsJsonObject();
        json.add("navalRepairs", com.google.gson.JsonParser.parseString("{\"availableDockyards\":3,\"ships\":[]}"));
        var report = new IndustryProtocol.Response(json.toString()).view().navalRepairs();
        assertNotNull(report);
        assertEquals(3, report.availableDockyards());
        assertNull(report.usedDockyards());
        assertTrue(report.ships().isEmpty());
        json.getAsJsonObject("navalRepairs").addProperty("usedDockyards", 2);
        assertEquals(2, new IndustryProtocol.Response(json.toString()).view().navalRepairs().usedDockyards());
        assertThrows(IllegalArgumentException.class, () -> new IndustryView.NavalRepairs(3, List.of(), 4));
        assertThrows(IllegalArgumentException.class, () -> new IndustryView.NavalRepair("s","s","ship","port",60,50,"waiting"));
    }
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
