package dev.hoi.client.screen;

import dev.hoi.protocol.ProtocolIndustryView;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenProductionIndicatorsTest {
    @Test void navalCompletionRoundsUpPartialDaysAndHandlesStoppedProduction() {
        assertEquals("30일",ScreenProductionIndicators.navalDays(.025,.25));
        assertEquals("4일",ScreenProductionIndicators.navalDays(.2,.25));
        assertEquals("1일",ScreenProductionIndicators.navalDays(2,.5));
        assertEquals("0일",ScreenProductionIndicators.navalDays(.1,1));
        assertEquals("생산량 없음",ScreenProductionIndicators.navalDays(0,.5));
    }
    private ProtocolIndustryView.Equipment equipment(String id,long stock,long deployed,long demand,String replacement) {
        return new ProtocolIndustryView.Equipment(id,id,"","armor",false,true,1,10,Map.of(),stock,0,deployed,0,replacement,"tank","전차",demand,false,id);
    }
    @Test void unmetFamilyDemandPrecedesUpgradesAndSharedStockCoversOlderModels() {
        var old=equipment("old",5,10,8,"new");
        var next=equipment("new",0,0,0,"");
        assertEquals(2,ScreenProductionIndicators.demand(List.of(old,next)).targets().get("new"));
        old=equipment("old",8,10,8,"new");
        assertEquals(1,ScreenProductionIndicators.demand(List.of(old,next)).targets().get("new"));
        assertEquals(0,ScreenProductionIndicators.demand(List.of(equipment("new",0,0,0,""))).targets().get("new"));
    }
    @Test void sourceRolesDistinguishTankDestroyersArtilleryAndAirDefense() {
        assertEquals("production/roles/land_0",ScreenProductionIndicators.landRole(equipment("modern_tank_destroyer_equipment_1",0,0,0,"")));
        assertEquals("production/roles/land_1",ScreenProductionIndicators.landRole(equipment("modern_tank_artillery_equipment_1",0,0,0,"")));
        assertEquals("production/roles/land_2",ScreenProductionIndicators.landRole(equipment("modern_tank_anti_air_equipment_1",0,0,0,"")));
        assertEquals("production/roles/land_1",ScreenProductionIndicators.landRole(equipment("motorized_rocket_equipment_1",0,0,0,"")));
        for(String role:List.of("infantry_equipment_1","mechanized_equipment_1","modern_tank_equipment_1","hoi:chassis/modern_tank_anti_air"))
            assertEquals("",ScreenProductionIndicators.landRole(equipment(role,0,0,0,"")));
    }
}
