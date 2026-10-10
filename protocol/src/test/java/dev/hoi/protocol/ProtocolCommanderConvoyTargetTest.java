package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.hoi.protocol.ProtocolCommanderHud.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCommanderConvoyTargetTest {
    @Test void savedConvoyExclusionRoundTripsIndependentlyOfEngagementAndComposition() {
        var gson=new Gson();
        for(Boolean excluded:new Boolean[]{null,false,true}){var state=new NavalData(2,true,false,"LOW",Map.of("unit_hud/navy_destroyer",4),excluded);assertEquals(state,gson.fromJson(gson.toJson(state),NavalData.class));var member=new Member("force/owned","Owned task force","","","",1,new Open(Mode.NAVAL_MISSION,"force/owned"),"",1.0,Map.of(),"","HOLD",new MemberData("","",4,null,state,null));var card=new Card("fleet/owned","Fleet","","politics/empty/leader","","",1,10,"",Mode.NAVY_FLEET,new Panel("","",null,List.of(member),1));var view=Snapshot.of(new View("KOR","hoi:atlas_navy_kor",Mode.NAVY_HQ,List.of(card))).view();assertEquals(excluded,view.cards().getFirst().panel().members().getFirst().data().navy().convoyExcluded());assertEquals("LOW",view.cards().getFirst().panel().members().getFirst().data().navy().engagement());}
    }
    @Test void absentLegacyFlagStaysUnsetAndExistingConstructorRemainsValid() {
        var gson=new Gson();var previous=new NavalData(null,null,null,"MEDIUM",Map.of());var json=gson.toJsonTree(previous);json.getAsJsonObject().remove("convoyExcluded");var restored=gson.fromJson(json,NavalData.class);assertNull(restored.convoyExcluded());assertEquals(previous,restored);assertEquals(Map.of(),restored.targets());
    }
}
