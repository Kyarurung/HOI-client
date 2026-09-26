package dev.hoi.protocol;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolIndustryViewTest {
    @Test void productionResourceShortagesRoundTripWithoutGuessingLegacyValues() {
        var gson=new com.google.gson.Gson();
        var line=new ProtocolIndustryView.Line("line","equipment",15,150,.5,10,.2,.8,null,10,2,.00003,.0001,.7,java.util.Set.of("RUBBER"));
        assertEquals(line,gson.fromJson(gson.toJson(line),ProtocolIndustryView.Line.class));
        var legacy=gson.toJsonTree(line).getAsJsonObject();legacy.remove("missingResources");
        assertTrue(gson.fromJson(legacy,ProtocolIndustryView.Line.class).missingResources().isEmpty());
        assertThrows(UnsupportedOperationException.class,()->line.missingResources().add("STEEL"));
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.Line("line","equipment",15,150,.5,10,.2,.8,null,10,2,.00003,.0001,.7,java.util.Set.of("x".repeat(33))));
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.Line("line","equipment",15,150,.5,10,.2,.8,null,10,2,.00003,.0001,.7,java.util.stream.IntStream.range(0,9).mapToObj(Integer::toString).collect(java.util.stream.Collectors.toSet())));
    }
    @Test void designerContributionsPreserveLegacyStatsAndBoundedDeltas() {
        var gson=new com.google.gson.Gson();
        var stat=new ProtocolTankDesigner.Stat("hard_attack","대물 공격",46,false,List.of(new ProtocolTankDesigner.Contribution("강선포",40)),46);
        assertEquals(stat,gson.fromJson(gson.toJson(stat),ProtocolTankDesigner.Stat.class));
        var legacy=gson.toJsonTree(stat).getAsJsonObject();legacy.remove("contributions");legacy.remove("change");
        var restored=gson.fromJson(legacy,ProtocolTankDesigner.Stat.class);
        assertEquals(46,restored.value());assertEquals(0,restored.change());assertTrue(restored.contributions().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new ProtocolTankDesigner.Stat("hard_attack","",46,false,Collections.nCopies(65,new ProtocolTankDesigner.Contribution("part",1)),0));
        assertThrows(IllegalArgumentException.class,()->new ProtocolTankDesigner.Contribution("part",Double.NaN));
        assertThrows(IllegalArgumentException.class,()->new ProtocolTankDesigner.Stat("hard_attack","",46,false,List.of(),Double.POSITIVE_INFINITY));
    }
    @Test void designerAdjustmentsPreserveSignsAndLegacyAbsence() {
        var gson=new com.google.gson.Gson();
        var adjustment=new ProtocolTankDesigner.Adjustment("forest",-.2,0,.1);
        var design=new ProtocolTankDesigner("chassis","name","texture",List.of(),List.of(),List.of(),Map.of(),1,0,List.of(),List.of(),List.of(adjustment));
        assertEquals(design,gson.fromJson(gson.toJson(design),ProtocolTankDesigner.class));
        var legacy=gson.toJsonTree(design).getAsJsonObject();legacy.remove("adjustments");
        assertTrue(gson.fromJson(legacy,ProtocolTankDesigner.class).adjustments().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new ProtocolTankDesigner.Adjustment("forest",Double.NaN,0,0));
        assertThrows(IllegalArgumentException.class,()->new ProtocolTankDesigner("chassis","name","texture",List.of(),List.of(),List.of(),Map.of(),1,0,List.of(),List.of(),Collections.nCopies(17,adjustment)));
    }
    @Test void equipmentSourceReferenceIsOptionalAndBounded() {
        var gson=new com.google.gson.Gson();
        var old=new ProtocolIndustryView.Equipment("old","Old","old","infantry",false,true,1,4,Map.of(),5,2,1,0);
        var json=gson.toJsonTree(old).getAsJsonObject();
        assertNull(gson.fromJson(json,ProtocolIndustryView.Equipment.class).sourceEquipment());
        json.addProperty("sourceEquipment","infantry_equipment_3");
        var equipment=gson.fromJson(json,ProtocolIndustryView.Equipment.class);
        assertEquals("infantry_equipment_3",equipment.sourceEquipment());
        assertEquals(equipment,gson.fromJson(gson.toJson(equipment),ProtocolIndustryView.Equipment.class));
        json.addProperty("sourceEquipment","x".repeat(129));
        assertThrows(RuntimeException.class,()->gson.fromJson(json,ProtocolIndustryView.Equipment.class));
    }
    @Test void productionEfficiencyGainPreservesLegacyUnknownAndStationaryZero() {
        var gson=new com.google.gson.Gson();
        var line=new ProtocolIndustryView.Line("line","equipment",15,150,.5,10,.2,.8,null,10,2,.00003,.0001,.7);
        assertEquals(line,gson.fromJson(gson.toJson(line),ProtocolIndustryView.Line.class));
        var legacy=gson.toJsonTree(line).getAsJsonObject();legacy.remove("efficiencyGain");
        assertNull(gson.fromJson(legacy,ProtocolIndustryView.Line.class).efficiencyGain());
        legacy.remove("efficiencyBaseGain");legacy.remove("efficiencyResourcePenalty");
        var old=gson.fromJson(legacy,ProtocolIndustryView.Line.class);
        assertNull(old.efficiencyBaseGain());assertNull(old.efficiencyResourcePenalty());
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.Line("line","equipment",15,150,.5,10,.2,.8,null,10,2,.00003,-.1,.7));
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.Line("line","equipment",15,150,.5,10,.2,.8,null,10,2,.00003,.1,1.1));
        assertEquals(0.0,new ProtocolIndustryView.Line("line","equipment",15,150,.5,0,.2,0,null,0,2,0.0).efficiencyGain());
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.Line("line","equipment",15,150,.5,10,.2,.8,null,10,2,Double.NaN));
    }
    @Test void equipmentRatiosPreserveUnknownAndValidateServerValues() {
        var gson=new com.google.gson.Gson();
        var policy=new ProtocolIndustryView.RecruitmentPolicy(java.util.Set.of(),Map.of(),.5,Map.of(),null,Map.of("supply",0.0,"garrison",1.0));
        assertEquals(policy,gson.fromJson(gson.toJson(policy),ProtocolIndustryView.RecruitmentPolicy.class));
        var legacy=gson.toJsonTree(policy).getAsJsonObject();legacy.remove("equipmentRatios");
        assertTrue(gson.fromJson(legacy,ProtocolIndustryView.RecruitmentPolicy.class).equipmentRatios().isEmpty());
        for(double ratio:new double[]{-.01,1.01,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.RecruitmentPolicy(java.util.Set.of(),Map.of(),null,Map.of(),null,Map.of("upgrade",ratio)));
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.RecruitmentPolicy(java.util.Set.of(),Map.of(),null,Map.of(),null,Map.of("unknown",.5)));
    }
    @Test void regimentColumnsPreserveHolesBetweenIndependentColumns() {
        var gson=new com.google.gson.Gson();
        var line=List.of("INFANTRY","INFANTRY","INFANTRY","INFANTRY");
        var template=new ProtocolIndustryView.Template("draft","편제",line,List.of(),List.of(),Map.of(),0,0,List.of(1,0,3,0,0),Map.of(2,"ENGINEER"));
        assertEquals(template,gson.fromJson(gson.toJson(template),ProtocolIndustryView.Template.class));
        assertEquals("",template.lineUnit(5));
        assertEquals("INFANTRY",template.lineUnit(12));
        assertEquals(1,template.lineIndex(10));
        var json=gson.toJsonTree(template).getAsJsonObject();json.remove("columns");json.remove("regimentSupport");
        var legacy=gson.fromJson(json,ProtocolIndustryView.Template.class);
        assertNull(legacy.columns()); assertTrue(legacy.regimentSupport().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.Template("d","d",line,List.of(),List.of(),Map.of(),0,0,List.of(1,0,2,0,0),Map.of()));
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.Template("d","d",line,List.of(),List.of(),Map.of(),0,0,List.of(1,0,3,0,0),Map.of(0,"ENGINEER")));
    }
    @Test void equipmentGenerationsAreOptionalAndNeverInferredForLegacyServers() {
        var gson = new com.google.gson.Gson();
        var old = new ProtocolIndustryView.Equipment("old","Old","old","infantry",false,true,1,4,Map.of(),5,2,1,0);
        var legacy = gson.fromJson(gson.toJson(old),ProtocolIndustryView.Equipment.class);
        assertNull(legacy.demand()); assertNull(legacy.replacement()); assertNull(legacy.family()); assertFalse(legacy.outdated());
        var versioned = new ProtocolIndustryView.Equipment("old","Old","old","infantry",false,true,1,4,Map.of(),5,2,1,0,"new","rifles");
        assertEquals(versioned,gson.fromJson(gson.toJson(versioned),ProtocolIndustryView.Equipment.class));
        assertTrue(versioned.outdated());
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.Equipment("old","Old","old","infantry",false,true,1,4,Map.of(),5,2,1,0,"old","rifles"));
    }
    @Test void outstandingDemandPreservesZeroAndRejectsInvalidAmounts() {
        var gson = new com.google.gson.Gson();
        var equipment = new ProtocolIndustryView.Equipment("rifle","Rifle","rifle","infantry",false,true,1,4,Map.of(),5,2,1,3,null,"infantry","보병 장비",8L);
        assertEquals(equipment, gson.fromJson(gson.toJson(equipment),ProtocolIndustryView.Equipment.class));
        var json = gson.toJsonTree(equipment).getAsJsonObject();
        json.addProperty("demand",-1);
        assertThrows(RuntimeException.class, () -> gson.fromJson(json,ProtocolIndustryView.Equipment.class));
        json.addProperty("demand",0);
        assertEquals(0L,gson.fromJson(json,ProtocolIndustryView.Equipment.class).demand());
    }
    @Test void optionalRepairReportDistinguishesLegacyUnknownFromAnEmptyQueue() {
        var old = ProtocolIndustryView.revoked("session", "");
        assertNull(ProtocolIndustryPayloads.Response.of(old).view().navalRepairs());
        String legacy = ProtocolIndustryPayloads.Response.of(old).json();
        var json = com.google.gson.JsonParser.parseString(legacy).getAsJsonObject();
        json.add("navalRepairs", com.google.gson.JsonParser.parseString("{\"availableDockyards\":3,\"ships\":[]}"));
        var report = new ProtocolIndustryPayloads.Response(json.toString()).view().navalRepairs();
        assertNotNull(report);
        assertEquals(3, report.availableDockyards());
        assertNull(report.usedDockyards());
        assertTrue(report.ships().isEmpty());
        json.getAsJsonObject("navalRepairs").addProperty("usedDockyards", 2);
        assertEquals(2, new ProtocolIndustryPayloads.Response(json.toString()).view().navalRepairs().usedDockyards());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryView.NavalRepairs(3, List.of(), 4));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryView.NavalRepair("s","s","ship","port",60,50,"waiting"));
    }
    @Test void repairProgressKeepsOrganizationSeparateAndLegacyUnknown() {
        var gson=new com.google.gson.Gson();
        var current=new ProtocolIndustryView.NavalRepair("s","s","ship","port",30,50,"수리 중",12.0,40.0,36L);
        assertEquals(current,gson.fromJson(gson.toJson(current),ProtocolIndustryView.NavalRepair.class));
        var json=gson.toJsonTree(current).getAsJsonObject();
        json.remove("organization");json.remove("maxOrganization");json.remove("remainingHours");
        var old=gson.fromJson(json,ProtocolIndustryView.NavalRepair.class);
        assertNull(old.organization());assertNull(old.maxOrganization());assertNull(old.remainingHours());
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.NavalRepair("s","s","ship","port",30,50,"수리 중",41.0,40.0,36L));
        assertThrows(IllegalArgumentException.class,()->new ProtocolIndustryView.NavalRepair("s","s","ship","port",30,50,"수리 중",12.0,40.0,-1L));
    }
    @Test void requestsRejectMalformedAmountsAndIdentifiersWithoutAnArbitraryFactoryCap() {
        assertEquals(Integer.MAX_VALUE, new ProtocolIndustryPayloads.Request(ProtocolIndustryPayloads.Action.ASSIGN, "session", 1, "line", "", Integer.MAX_VALUE).amount());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryPayloads.Request(ProtocolIndustryPayloads.Action.ASSIGN, "session", 1, "line", "", -1));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryPayloads.Request(null, "session", 1, "", "", 0));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryPayloads.Request(ProtocolIndustryPayloads.Action.OPEN, "s".repeat(37), 0, "", "", 0));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryPayloads.Request(ProtocolIndustryPayloads.Action.ADD, "s", -1, "", "", 0));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryPayloads.Request(ProtocolIndustryPayloads.Action.ADD, "s", 0, "x".repeat(129), "", 0));
    }

    @Test void privateSnapshotsRemainBoundedAndRevocationRoundTrips() {
        var revoked = ProtocolIndustryView.revoked("session", "권한이 변경되었습니다.");
        assertEquals(revoked, ProtocolIndustryPayloads.Response.of(revoked).view());
        assertNull(revoked.economy());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryPayloads.Response("x".repeat(800001)));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryPayloads.Response("null").view());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryView.Template("t", "t", Collections.nCopies(26, "INFANTRY"), List.of(), List.of(), Map.of(), 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryView.Template("t", "t", List.of("INFANTRY"), Collections.nCopies(6, "ENGINEER"), List.of(), Map.of(), 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolIndustryView.Template("t", "t", List.of("INFANTRY"), List.of(), List.of(), Map.of(), Double.NaN, 1));
        var supplied = new java.util.ArrayList<>(List.of("INFANTRY"));
        var template = new ProtocolIndustryView.Template("t", "보병사단", supplied, List.of(), List.of(), Map.of("gear", 900L), 180, 1000);
        supplied.clear();
        assertEquals(List.of("INFANTRY"), template.line());
        assertThrows(UnsupportedOperationException.class, () -> template.equipment().put("gear", 0L));
    }
}
