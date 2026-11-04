package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.hoi.protocol.commander.CommanderHudPayloads.*;

class CommanderAirControlsTest {
    @Test void airGroupsRemainIndependentFromArmyAttachmentAndBooleanReinforcement() {
        var gson=new Gson();
        for(String policy:List.of("ANY","OBSOLETE_ONLY","ONLY_NEW","ONLY_SAME")){
            var data=new MemberData("","army/actual",42,null,null,false,new AirData("airgroup/actual","Actual group",policy));
            assertEquals(data,gson.fromJson(gson.toJson(data),MemberData.class));
            var wing=new Member("wing/actual","Actual wing","Ready","","",1,new Open(Mode.AIR_MISSION,"wing/actual"),"",null,Map.of(),"Actual airbase","HOLD",data);
            assertEquals(data,Snapshot.of(new View("KOR","hoi:atlas_air_kor",Mode.AIR,List.of(),List.of(wing))).view().wings().getFirst().data());
        }
        var legacy=gson.toJsonTree(new MemberData("","army/legacy",17,null,null,true));legacy.getAsJsonObject().remove("air");
        var parsed=gson.fromJson(legacy,MemberData.class);assertNull(parsed.air());assertEquals("army/legacy",parsed.attachedArmy());assertTrue(parsed.reinforce());
    }
    @Test void strategicModeIsOptionalAndIndependentFromAirPolicy() {
        var gson=new Gson();var legacy=gson.toJsonTree(new MemberData("template/actual","",1,null));legacy.getAsJsonObject().remove("strategic");assertNull(gson.fromJson(legacy,MemberData.class).strategic());
        for(boolean strategic:List.of(false,true)){var data=new MemberData("template/actual","",1,null,null,null,null,strategic);var restored=gson.fromJson(gson.toJson(data),MemberData.class);assertEquals(Boolean.valueOf(strategic),restored.strategic());assertNull(restored.air());assertEquals("template/actual",restored.template());}
    }
    @Test void malformedOrGuessedAirStateIsRejected() {
        assertThrows(IllegalArgumentException.class,()->new AirData("","","HIGH"));
        assertThrows(IllegalArgumentException.class,()->new AirData("group/actual","","ANY"));
        assertThrows(IllegalArgumentException.class,()->new AirData("","Invented group","ANY"));
        assertThrows(IllegalArgumentException.class,()->new AirData("bad group","Invalid","ANY"));
        assertEquals("merge",new Open(Mode.AIR_GROUPS,"merge",List.of("group/a","group/b")).target());
        assertEquals(Mode.AIR_REINFORCEMENT,new Open(Mode.AIR_REINFORCEMENT,"policy",List.of("wing/a")).mode());
    }
}
