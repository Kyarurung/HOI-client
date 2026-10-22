package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapCommanderCardDragTest {
    private static Card card(String id,Mode role,String parent){return new Card(id,id,"","politics/empty/leader","","",1,24,"",role,new Panel(parent,"",null,List.of(),0,List.of(),List.of()));}
    private static final List<Card> CARDS=List.of(card("G1",Mode.ARMY_GROUP,""),card("A",Mode.ASSIGN_ARMY,"G1"),card("B",Mode.ASSIGN_ARMY,"G1"),card("G2",Mode.ARMY_GROUP,""),card("C",Mode.ASSIGN_ARMY,"G2"),card("D",Mode.ASSIGN_ARMY,""),card("E",Mode.ASSIGN_ARMY,""));
    private static final View VIEW=new View("KOR","hoi:atlas_army",Mode.ARMY,CARDS);
    private static List<MapCommanderCardDrag.Slot> slots(){var slots=new ArrayList<MapCommanderCardDrag.Slot>();for(int i=0;i<CARDS.size();i++)slots.add(new MapCommanderCardDrag.Slot(CARDS.get(i),i*42,42));return slots;}
    @Test void ordersCompleteSiblingListsWithoutRenamingOrChangingTheSnapshot(){
        assertEquals(new MapCommanderCardDrag.Drop("G1",List.of("B","A")),MapCommanderCardDrag.drop(VIEW,slots(),"A",124));
        assertEquals(new MapCommanderCardDrag.Drop("",List.of("E","D")),MapCommanderCardDrag.drop(VIEW,slots(),"D",293));
        assertEquals("A",VIEW.cards().get(1).name());assertEquals("G1",VIEW.cards().get(1).panel().parent());
    }
    @Test void groupMovesKeepTheirWholeBlockAndHiddenSiblingsInTheOrder(){
        assertEquals(List.of("G2","G1"),MapCommanderCardDrag.drop(VIEW,slots(),"G1",200).order());
        var page=slots().subList(0,2);assertEquals(Set.of("A","B"),new HashSet<>(MapCommanderCardDrag.drop(VIEW,page,"A",70).order()));
    }
    @Test void crossingExpandedGroupBoundariesChangesMembershipAndBothRowEndsDetach(){
        assertEquals(new MapCommanderCardDrag.Drop("G2",List.of("A","C")),MapCommanderCardDrag.drop(VIEW,slots(),"A",145));
        assertEquals(new MapCommanderCardDrag.Drop("G1",List.of("A","D","B")),MapCommanderCardDrag.drop(VIEW,slots(),"D",95));
        assertEquals(new MapCommanderCardDrag.Drop("",List.of("D","E","A")),MapCommanderCardDrag.drop(VIEW,slots(),"A",294));
        assertEquals(new MapCommanderCardDrag.Drop("",List.of("A","D","E")),MapCommanderCardDrag.drop(VIEW,slots(),"A",0));
    }
    @Test void fleetAndArmyOrdersNeverIncludeCreationOrUnassignedCards(){
        var f1=card("F1",Mode.NAVY_FLEET,"");var f2=card("F2",Mode.NAVY_FLEET,"");var other=card("reserve",Mode.NAVY_HQ,"");var view=new View("KOR","hoi:atlas_navy",Mode.NAVY_HQ,List.of(f1,f2,other));
        assertFalse(MapCommanderCardDrag.movable(other));assertEquals(List.of("F2","F1"),MapCommanderCardDrag.drop(view,List.of(new MapCommanderCardDrag.Slot(f1,0,42),new MapCommanderCardDrag.Slot(f2,42,42)),"F1",84).order());
    }
    @Test void legacyFleetRolesMixWithExplicitFleetsButNeverTheReserveCard(){
        var legacy=card("old",null,"");var modern=card("new",Mode.NAVY_FLEET,"");var reserve=card("reserve",Mode.NAVY_HQ,"");
        var view=new View("KOR","hoi:atlas_navy",Mode.NAVY_HQ,List.of(legacy,modern,reserve));
        assertEquals(List.of("new","old"),MapCommanderCardDrag.drop(view,List.of(new MapCommanderCardDrag.Slot(legacy,0,42),new MapCommanderCardDrag.Slot(modern,42,42)),"old",84).order());
        assertEquals(List.of(legacy,modern),MapCommanderCardDrag.siblings(view,modern,""));
    }
    @Test void reorderingVisibleCardsPreservesHiddenSiblingSlots(){
        for(var role:List.of(Mode.NAVY_FLEET,Mode.ASSIGN_ARMY,Mode.ARMY_GROUP)){
            var first=card("first",role,"");var hidden=card("hidden",role,"");var last=card("last",role,"");
            var view=new View("KOR","hoi:atlas_navy",role==Mode.NAVY_FLEET?Mode.NAVY_HQ:Mode.ARMY,List.of(first,hidden,last));
            assertEquals(List.of("last","hidden","first"),MapCommanderCardDrag.drop(view,List.of(new MapCommanderCardDrag.Slot(first,0,42),new MapCommanderCardDrag.Slot(last,42,42)),"first",84).order());
        }
    }
}
