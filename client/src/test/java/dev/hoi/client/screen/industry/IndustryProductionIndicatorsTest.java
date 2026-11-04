package dev.hoi.client.screen.industry;

import dev.hoi.protocol.industry.IndustryView;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryProductionIndicatorsTest {
    @Test void navalCompletionRoundsUpPartialDaysAndHandlesStoppedProduction() {
        assertEquals("30일",IndustryProductionIndicators.navalDays(.025,.25));
        assertEquals("4일",IndustryProductionIndicators.navalDays(.2,.25));
        assertEquals("1일",IndustryProductionIndicators.navalDays(2,.5));
        assertEquals("0일",IndustryProductionIndicators.navalDays(.1,1));
        assertEquals("생산량 없음",IndustryProductionIndicators.navalDays(0,.5));
    }
    private IndustryView.Equipment equipment(String id,long stock,long deployed,long demand,String replacement) {
        return new IndustryView.Equipment(id,id,"","armor",false,true,1,10,Map.of(),stock,0,deployed,0,replacement,"tank","전차",demand,false,id);
    }
    @Test void unmetFamilyDemandPrecedesUpgradesAndSharedStockCoversOlderModels() {
        var old=equipment("old",5,10,8,"new");
        var next=equipment("new",0,0,0,"");
        assertEquals(2,IndustryProductionIndicators.demand(List.of(old,next)).targets().get("new"));
        old=equipment("old",8,10,8,"new");
        assertEquals(1,IndustryProductionIndicators.demand(List.of(old,next)).targets().get("new"));
        assertEquals(0,IndustryProductionIndicators.demand(List.of(equipment("new",0,0,0,""))).targets().get("new"));
    }
    @Test void sourceRolesDistinguishTankDestroyersArtilleryAndAirDefense() {
        assertEquals("production/roles/land_0",IndustryProductionIndicators.landRole(equipment("modern_tank_destroyer_equipment_1",0,0,0,"")));
        assertEquals("production/roles/land_1",IndustryProductionIndicators.landRole(equipment("modern_tank_artillery_equipment_1",0,0,0,"")));
        assertEquals("production/roles/land_2",IndustryProductionIndicators.landRole(equipment("modern_tank_anti_air_equipment_1",0,0,0,"")));
        assertEquals("production/roles/land_1",IndustryProductionIndicators.landRole(equipment("motorized_rocket_equipment_1",0,0,0,"")));
        for(String role:List.of("infantry_equipment_1","mechanized_equipment_1","modern_tank_equipment_1","hoi:chassis/modern_tank_anti_air"))
            assertEquals("",IndustryProductionIndicators.landRole(equipment(role,0,0,0,"")));
    }
}
