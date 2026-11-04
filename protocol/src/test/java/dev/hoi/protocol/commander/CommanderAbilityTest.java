package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import dev.hoi.protocol.commander.CommanderHudPayloads.AbilityDetail;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderAbilityTest {
    @Test void affordabilityIsOptionalAndIndependentOfEnabled(){
        var gson=new Gson();
        for(Boolean affordable:new Boolean[]{null,false,true}){
            var detail=new AbilityDetail("force_attack",false,"원문\n\n비용",affordable);
            assertEquals(detail,gson.fromJson(gson.toJson(detail),AbilityDetail.class));
        }
        assertNull(new AbilityDetail("force_attack",true,"").affordable());
        assertNull(gson.fromJson("{\"id\":\"force_attack\",\"enabled\":true,\"tooltip\":\"\"}",AbilityDetail.class).affordable());
    }
}
