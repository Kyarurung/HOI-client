package dev.hoi.client.map.commander;

import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderCardDragTest {
    @Test void processedHudRevisionAcknowledgesEqualityAndNewerValuesOnlyWithinItsSession(){
        String token=java.util.UUID.randomUUID().toString();
        for(long revision:new long[]{6,7,8}){
            var view=new View("KOR","hoi:atlas_army",Mode.ARMY,List.of(),List.of(),false,List.of(),List.of(),token,revision);
            assertEquals(revision>=7,CommanderCardDrag.applied(view,token,7));
            assertFalse(CommanderCardDrag.applied(view,java.util.UUID.randomUUID().toString(),7));
            assertEquals(token,CommanderHud.visibleCards(view).orderToken());
            assertEquals(revision,CommanderHud.visibleCards(view).orderRevision());
        }
        assertFalse(CommanderCardDrag.applied(new View("KOR","hoi:atlas_army",Mode.ARMY,List.of()),token,7));
    }
    private static Card card(String id,Mode role,String parent){return new Card(id,id,"","politics/empty/leader","","",1,24,"",role,new Panel(parent,"",null,List.of(),0,List.of(),List.of()));}
    private static final List<Card> CARDS=List.of(card("G1",Mode.ARMY_GROUP,""),card("A",Mode.ASSIGN_ARMY,"G1"),card("B",Mode.ASSIGN_ARMY,"G1"),card("G2",Mode.ARMY_GROUP,""),card("C",Mode.ASSIGN_ARMY,"G2"),card("D",Mode.ASSIGN_ARMY,""),card("E",Mode.ASSIGN_ARMY,""));
    private static final View VIEW=new View("KOR","hoi:atlas_army",Mode.ARMY,CARDS);
    private static List<CommanderCardDrag.Slot> slots(){var slots=new ArrayList<CommanderCardDrag.Slot>();for(int i=0;i<CARDS.size();i++)slots.add(new CommanderCardDrag.Slot(CARDS.get(i),i*42,42));return slots;}
    @Test void ordersCompleteSiblingListsWithoutRenamingOrChangingTheSnapshot(){
        assertEquals(new CommanderCardDrag.Drop("G1",List.of("B","A")),CommanderCardDrag.drop(VIEW,slots(),"A",124));
        assertEquals(new CommanderCardDrag.Drop("",List.of("E","D")),CommanderCardDrag.drop(VIEW,slots(),"D",293));
        assertEquals("A",VIEW.cards().get(1).name());assertEquals("G1",VIEW.cards().get(1).panel().parent());
    }
    @Test void groupMovesKeepTheirWholeBlockAndHiddenSiblingsInTheOrder(){
        assertEquals(List.of("G2","G1"),CommanderCardDrag.drop(VIEW,slots(),"G1",200).order());
        var page=slots().subList(0,2);assertEquals(Set.of("A","B"),new HashSet<>(CommanderCardDrag.drop(VIEW,page,"A",70).order()));
    }
    @Test void crossingExpandedGroupBoundariesChangesMembershipAndBothRowEndsDetach(){
        assertEquals(new CommanderCardDrag.Drop("G2",List.of("A","C")),CommanderCardDrag.drop(VIEW,slots(),"A",145));
        assertEquals(new CommanderCardDrag.Drop("G1",List.of("A","D","B")),CommanderCardDrag.drop(VIEW,slots(),"D",95));
        assertEquals(new CommanderCardDrag.Drop("",List.of("D","E","A")),CommanderCardDrag.drop(VIEW,slots(),"A",294));
        assertEquals(new CommanderCardDrag.Drop("",List.of("A","D","E")),CommanderCardDrag.drop(VIEW,slots(),"A",0));
    }
    @Test void fleetAndArmyOrdersNeverIncludeCreationOrUnassignedCards(){
        var f1=card("F1",Mode.NAVY_FLEET,"");var f2=card("F2",Mode.NAVY_FLEET,"");var other=card("reserve",Mode.NAVY_HQ,"");var view=new View("KOR","hoi:atlas_navy",Mode.NAVY_HQ,List.of(f1,f2,other));
        assertFalse(CommanderCardDrag.movable(other));assertEquals(List.of("F2","F1"),CommanderCardDrag.drop(view,List.of(new CommanderCardDrag.Slot(f1,0,42),new CommanderCardDrag.Slot(f2,42,42)),"F1",84).order());
    }
    @Test void legacyFleetRolesMixWithExplicitFleetsButNeverTheReserveCard(){
        var legacy=card("old",null,"");var modern=card("new",Mode.NAVY_FLEET,"");var reserve=card("reserve",Mode.NAVY_HQ,"");
        var view=new View("KOR","hoi:atlas_navy",Mode.NAVY_HQ,List.of(legacy,modern,reserve));
        assertEquals(List.of("new","old"),CommanderCardDrag.drop(view,List.of(new CommanderCardDrag.Slot(legacy,0,42),new CommanderCardDrag.Slot(modern,42,42)),"old",84).order());
        assertEquals(List.of(legacy,modern),CommanderCardDrag.siblings(view,modern,""));
    }
    @Test void reorderingVisibleCardsPreservesHiddenSiblingSlots(){
        for(var role:List.of(Mode.NAVY_FLEET,Mode.ASSIGN_ARMY,Mode.ARMY_GROUP)){
            var first=card("first",role,"");var hidden=card("hidden",role,"");var last=card("last",role,"");
            var view=new View("KOR","hoi:atlas_navy",role==Mode.NAVY_FLEET?Mode.NAVY_HQ:Mode.ARMY,List.of(first,hidden,last));
            assertEquals(List.of("last","hidden","first"),CommanderCardDrag.drop(view,List.of(new CommanderCardDrag.Slot(first,0,42),new CommanderCardDrag.Slot(last,42,42)),"first",84).order());
        }
    }
    @Test void creationSlotKeepsMembershipAndHiddenSiblingPositions(){
        assertEquals(new CommanderCardDrag.Drop("G1",List.of("B","A")),CommanderCardDrag.edgeDrop(VIEW,slots(),"A",false));
        var a=card("A",Mode.ASSIGN_ARMY,"G1");var hidden=card("hidden",Mode.ASSIGN_ARMY,"G1");var b=card("B",Mode.ASSIGN_ARMY,"G1");
        var view=new View("KOR","hoi:atlas_army",Mode.ARMY,List.of(a,hidden,b));
        var visible=List.of(new CommanderCardDrag.Slot(a,0,42),new CommanderCardDrag.Slot(b,42,42));
        assertEquals(new CommanderCardDrag.Drop("G1",List.of("B","hidden","A")),CommanderCardDrag.edgeDrop(view,visible,"A",false));
        assertEquals(new CommanderCardDrag.Drop("G1",List.of("A","hidden","B")),CommanderCardDrag.edgeDrop(view,visible,"B",false));
    }
    @Test void groupCreationSlotMovesCardsFirstWithoutChangingMembership(){
        assertEquals(new CommanderCardDrag.Drop("G1",List.of("B","A")),CommanderCardDrag.edgeDrop(VIEW,slots(),"B",true));
        assertEquals(new CommanderCardDrag.Drop("",List.of("E","D")),CommanderCardDrag.edgeDrop(VIEW,slots(),"E",true));
        assertEquals(new CommanderCardDrag.Drop("",List.of("G2","G1")),CommanderCardDrag.edgeDrop(VIEW,slots(),"G2",true));
        var a=card("A",Mode.ASSIGN_ARMY,"G1");var hidden=card("hidden",Mode.ASSIGN_ARMY,"G1");var b=card("B",Mode.ASSIGN_ARMY,"G1");
        var view=new View("KOR","hoi:atlas_army",Mode.ARMY,List.of(a,hidden,b));
        var visible=List.of(new CommanderCardDrag.Slot(a,0,42),new CommanderCardDrag.Slot(b,42,42));
        assertEquals(new CommanderCardDrag.Drop("G1",List.of("B","hidden","A")),CommanderCardDrag.edgeDrop(view,visible,"B",true));
        assertEquals(new CommanderCardDrag.Drop("G1",List.of("A","hidden","B")),CommanderCardDrag.edgeDrop(view,visible,"A",true));
    }

}
