package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.hoi.protocol.commander.CommanderHudPayloads.*;
import static org.junit.jupiter.api.Assertions.*;

class CommanderTargetClassesTest {
    @Test void exactNineClassesSurvivePayloadAndLegacyConvoyRemainsCompatible() {
        var gson=new Gson();var all=Set.of("SUBMARINE","DESTROYER","LIGHT_CRUISER","HEAVY_CRUISER","CONVOY","BATTLE_CRUISER","BATTLESHIP","SH_BATTLESHIP","CARRIER");
        var saved=new NavalData(2,true,false,"LOW",Map.of(),false,all);assertEquals(all,gson.fromJson(gson.toJson(saved),NavalData.class).resolvedExcludedTargets());
        var member=new Member("force/owned","Owned task force","","","",1,new Open(Mode.NAVAL_MISSION,"force/owned"),"",1.0,Map.of(),"","HOLD",new MemberData("","",4,null,saved,null));var card=new Card("fleet/owned","Fleet","","politics/empty/leader","","",1,10,"",Mode.NAVY_FLEET,new Panel("","",null,List.of(member),1));assertEquals(all,Snapshot.of(new View("KOR","hoi:atlas_navy_kor",Mode.NAVY_HQ,List.of(card))).view().cards().getFirst().panel().members().getFirst().data().navy().resolvedExcludedTargets());
        assertEquals(Set.of("CONVOY"),new NavalData(null,null,null,"MEDIUM",Map.of(),true).resolvedExcludedTargets());assertEquals(Set.of(),new NavalData(null,null,null,"MEDIUM",Map.of()).resolvedExcludedTargets());assertEquals(Set.of(),new NavalData(null,null,null,"MEDIUM",Map.of(),true,Set.of()).resolvedExcludedTargets());assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"MEDIUM",Map.of(),null,Set.of("TRANSPORT")));
    }
    @Test void newRoutesPreserveExactOwnedIdentityAndSelection() {
        var gson=new Gson();for(var request:List.of(new Open(Mode.NAVAL_AREAS,"fleet/owned",List.of("clear")),new Open(Mode.NAVAL_REINFORCEMENT,"force/owned"),new Open(Mode.ARMY_ABILITIES,"extra_suplies",List.of("army/owned"))))assertEquals(request,gson.fromJson(gson.toJson(request),Open.class));
    }
}
