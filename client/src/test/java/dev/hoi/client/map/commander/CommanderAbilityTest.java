package dev.hoi.client.map.commander;

import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderAbilityTest {
    private static Card card(Mode role,List<String> ids){
        var gson=new com.google.gson.Gson();var card=new Card(role.name(),"","","","","",0,0,"",role,new Panel("","commander",null,List.of(),0,List.of(),List.of()));
        var json=gson.toJsonTree(card).getAsJsonObject();json.getAsJsonObject("panel").add("abilities",gson.toJsonTree(ids));
        return gson.fromJson(json,Card.class);
    }
    @Test void bothRanksKeepFourSkillsIncludingBasicsWithoutBackfillingHiddenSkills(){
        var ids=List.of("force_attack","last_stand","staff_office_plan","probing_attack","glider_planes","makeshift_bridges","siege_artillery","faster_naval_invasion_planning");
        for(var role:List.of(Mode.ASSIGN_ARMY,Mode.ARMY_GROUP)){
            assertEquals(ids.subList(0,4),CommanderPanels.armyAbilityIds(List.of(card(role,ids))));
            assertEquals(ids.subList(0,3),CommanderPanels.armyAbilityIds(List.of(card(role,ids.subList(0,3)))));
            assertEquals(List.of(),CommanderPanels.armyAbilityIds(List.of(card(role,null))));
            var other=List.of("force_attack","last_stand","staff_office_plan","faster_naval_invasion_planning","glider_planes","makeshift_bridges","siege_artillery","probing_attack");
            assertEquals(ids.subList(0,3),CommanderPanels.armyAbilityIds(List.of(card(role,ids),card(role,other))));
        }
    }
    @Test void unavailableAndUnaffordableSkillsAreGrayWithoutGrayingAffordableCooldowns(){
        for(boolean enabled:new boolean[]{false,true}){
            assertTrue(CommanderPanels.abilityGrayscale(new AbilityDetail("force_attack",enabled,"",false)));
            assertFalse(CommanderPanels.abilityGrayscale(new AbilityDetail("force_attack",enabled,"",true)));
            assertEquals(!enabled,CommanderPanels.abilityGrayscale(new AbilityDetail("glider_planes",enabled,"",null)));
        }
    }
}
