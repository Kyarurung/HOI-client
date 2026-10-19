package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapCommanderGroupsTest {
    private static Card card(String id,Mode role,String parent){return new Card(id,id,"","politics/empty/leader","","",1,24,"",role,new Panel(parent,"",null,List.of(),0,List.of(),List.of()));}
    private static View view(List<Card> cards){return new View("KOR","hoi:army",Mode.ARMY,cards);}
    @Test void explicitCollapseSurvivesSelectionRefreshAndIndependentGroupChanges(){
        var a=card("A",Mode.ARMY_GROUP,"");var b=card("B",Mode.ARMY_GROUP,"");var child=card("a",Mode.ASSIGN_ARMY,"A");var state=new MapCommanderGroups();var view=view(List.of(a,b,child));state.accept(view);assertFalse(state.expanded("A"));state.reveal(a);state.reveal(b);state.toggle(a);state.accept(view);state.reveal(child);assertFalse(state.expanded("A"));assertTrue(state.expanded("B"));assertEquals(List.of(a,b),state.pages(view,5).getFirst());state.accept(view(List.of(b,card("a",Mode.ASSIGN_ARMY,"B"))));assertFalse(state.expanded("A"));assertEquals(List.of("B","a"),state.pages(view(List.of(b,card("a",Mode.ASSIGN_ARMY,"B"))),5).getFirst().stream().map(Card::id).toList());
    }
    @Test void oversizedGroupKeepsItsParentOnEveryPageAndChildrenImmediatelyToItsRight(){
        var parent=card("G",Mode.ARMY_GROUP,"");var cards=new ArrayList<Card>();cards.add(parent);for(int i=0;i<8;i++)cards.add(card("a"+i,Mode.ASSIGN_ARMY,"G"));var state=new MapCommanderGroups();var view=view(cards);state.accept(view);state.reveal(parent);var pages=state.pages(view,4);assertEquals(3,pages.size());var seen=new HashSet<String>();for(var page:pages){assertTrue(page.size()<=4);assertEquals(parent,page.getFirst());for(var child:page.subList(1,page.size())){assertTrue(seen.add(child.id()));assertEquals("G",state.owner(child,view));}}assertEquals(8,seen.size());
    }
    @Test void foldedMemberRowsAndGroupColorRemainStableWithoutMutatingSnapshots(){
        var army=card("fleet",Mode.NAVY_FLEET,"");var state=new MapCommanderGroups();var view=view(List.of(army));state.accept(view);state.toggle(army);int color=state.color("G",view);state.accept(view);assertFalse(state.rowsVisible("fleet"));assertEquals(color,state.color("G",view));assertEquals(List.of(army),state.pages(view,4).getFirst());state.accept(new View("PRK","hoi:army",Mode.ARMY,List.of(army)));assertTrue(state.rowsVisible("fleet"));
    }
    @Test void armyAndDivisionRowsCannotBeFolded(){
        var army=card("army",Mode.ASSIGN_ARMY,"");var state=new MapCommanderGroups();state.accept(view(List.of(army)));state.toggle(army);assertTrue(state.rowsVisible("army"));assertEquals(List.of(army),state.pages(view(List.of(army)),4).getFirst());
    }
}
