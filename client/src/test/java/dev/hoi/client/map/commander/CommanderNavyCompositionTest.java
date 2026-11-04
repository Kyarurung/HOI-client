package dev.hoi.client.map.commander;

import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderNavyCompositionTest {
    private static Member force(String id,Map<String,Integer> counts){return new Member(id,id,"","","",1,new Open(Mode.NAVAL_MISSION,id),"",null,counts,"","HOLD");}
    @Test void fleetBadgeCountsShipsRatherThanTaskForces(){
        var a=force("a",Map.of("unit_hud/navy_destroyer",8,"unit_hud/navy_submarine",2));
        var b=force("b",Map.of("unit_hud/navy_destroyer",3));
        var card=new Card("f","Fleet","","politics/empty/leader","","",2,10,"",Mode.NAVY_FLEET,new Panel("","",null,List.of(a,b),2,List.of()));
        assertEquals(10,CommanderPanels.shipCount(a));
        assertEquals(13,CommanderPanels.fleetShipCount(card));
        assertEquals(2,card.count());
        assertEquals(8,a.composition().get("unit_hud/navy_destroyer"));
    }
    @Test void trimmedFleetCannotPublishPartialShipCountsAsTheTotal(){
        var member=force("a",Map.of("unit_hud/navy_submarine",2));
        var card=new Card("f","Fleet","","politics/empty/leader","","",2,10,"",Mode.NAVY_FLEET,new Panel("","",null,List.of(member),2,List.of()));
        assertEquals(-1,CommanderPanels.fleetShipCount(card));
        assertEquals(2,CommanderPanels.shipCount(member));
    }
    @Test void authoritativeFleetCompositionSurvivesTrimmedTaskForceDetails(){
        var member=force("a",Map.of("unit_hud/navy_submarine",2));
        var counts=Map.of("unit_hud/navy_submarine",7,"unit_hud/navy_destroyer",8);
        var panel=new Panel("","",null,List.of(member),2,List.of(),null,null,List.of(),null,counts,null,null,"");
        var card=new Card("f","Fleet","","politics/empty/leader","","",2,10,"",Mode.NAVY_FLEET,panel);
        assertEquals(counts,CommanderPanels.fleetComposition(card));
        assertEquals(15,CommanderPanels.fleetShipCount(card));
        assertEquals(2,CommanderPanels.shipCount(member));
    }
}
