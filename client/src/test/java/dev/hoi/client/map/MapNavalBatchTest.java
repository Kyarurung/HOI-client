package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapNavalBatchTest {
    private static Member force(String id,Integer policy,Boolean reinforce){return new Member(id,id,"","","",1,new Open(Mode.NAVAL_MISSION,id),"",null,Map.of("unit_hud/navy_destroyer",3),"","HOLD",new MemberData("","",3,null,new NavalData(policy,false,reinforce,"LOW",Map.of("unit_hud/navy_destroyer",5)),null,null));}
    private static Member carrier(String id,Integer stance){return new Member(id,id,"","","",1,new Open(Mode.NAVAL_MISSION,id),"",null,Map.of(),"","HOLD",new MemberData("","",1,null,new NavalData(0,false,false,"LOW",Map.of(),null,null,"","",false,"",1,null,false,null,"",Map.of(),Map.of(),stance,Map.of()),null,null));}
    @Test void carrierStanceUsesMajorityAndFirstSelectionForTiesWithoutDroppingNonCarriers(){
        var first=carrier("first",3);var second=carrier("second",2);var third=carrier("third",2);var ordinary=carrier("ordinary",null);
        assertNull(MapCommanderPanels.navalStance(List.of(ordinary)));
        assertEquals(3,MapCommanderPanels.navalStance(List.of(ordinary,first,second)));
        assertEquals(2,MapCommanderPanels.navalStance(List.of(second,first)));
        assertEquals(2,MapCommanderPanels.navalStance(List.of(first,ordinary,second,third)));
        assertEquals(List.of("ordinary","0/stance/none","first","1/stance/3","second","2/stance/2"),MapCommanderPanels.navalBatch(List.of(ordinary,first,second),"STANCE_PREVIOUS"));
    }
    @Test void batchPreservesEachExpectedStateAndPublishedOrder(){
        var forces=List.of(force("second",0,true),force("first",2,null));
        assertEquals(List.of("second","0/state/true","first","1/state/unset"),MapCommanderPanels.navalBatch(forces,"REINFORCE"));
        assertEquals(List.of("second","0/policy/0","first","1/policy/2"),MapCommanderPanels.navalBatch(forces,"REPAIR_PREVIOUS"));
        assertEquals(List.of("second","0/engagement/LOW","first","1/engagement/LOW"),MapCommanderPanels.navalBatch(forces,"ENGAGEMENT_NEXT"));
    }
    @Test void unknownPolicyAndOversizedSelectionCannotProducePartialBatches(){
        assertTrue(MapCommanderPanels.navalBatch(List.of(force("a",0,true),force("b",null,false)),"REPAIR_NEXT").isEmpty());
        assertTrue(MapCommanderPanels.navalBatch(Collections.nCopies(161,force("a",0,true)),"REINFORCE").isEmpty());
    }
    @Test void compositionTargetsUseAuthoritativeAssetKeysAndMissingTargetsUseTheCurrentCount(){
        var member=force("a",0,true);
        assertEquals(5,MapCommanderPanels.hullTarget(member,"unit_hud/navy_destroyer",3));
        assertEquals(2,MapCommanderPanels.hullTarget(member,"unit_hud/navy_submarine",2));
    }
    @Test void shortcutPresentationUsesNativeKeysWithoutChangingOtherSourceLines(){
        assertEquals("§Y초계§!\n\n단축키: £z£",MapCommanderHud.navalTooltipText("§Y초계§!\n\n단축키: §YZ§!"));
        assertEquals("단축키: £shift£ + £b£",MapCommanderHud.navalTooltipText("단축키: §YShift+B§!"));
    }
}
