package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapCommanderTheaterSelectionTest {
    private static Card card(String id,Mode role){return new Card(id,id,"","politics/empty/leader","","",1,24,"",role,null);}
    private static Theater army(String id,String card){return new Theater(id,id,1,List.of(),List.of(card),Map.of(),List.of());}
    @Test void remembersSeparateBranchesAndFallsBackAfterTheaterRemoval(){
        var state=new MapCommanderTheaterSelection();var a=card("a",Mode.ASSIGN_ARMY);var b=card("b",Mode.ASSIGN_ARMY);
        var army=new View("KOR","hoi:atlas_army",Mode.ARMY,List.of(a,b),List.of(),true,List.of(army("one","a"),army("two","b")));
        assertEquals("one",state.selected(army));assertEquals(List.of(a),state.cards(army));
        assertTrue(state.selectCard(army,"b"));assertEquals(List.of(b),state.cards(army));
        var air=new View("KOR","hoi:atlas_air",Mode.AIR,List.of(a,b),List.of(),true,List.of(army("air","a")),army.theaters());
        assertEquals("two",state.selected(air));assertEquals(List.of(b),state.cards(air));
        assertFalse(state.choose(air,"air"));assertTrue(state.selectCard(air,"a"));assertEquals("one",state.selected(army));state.choose(army,"two");
        var f=card("fleet",Mode.NAVY_FLEET);var r=card("reserve",Mode.NAVY_HQ);
        var navy=new View("KOR","hoi:atlas_navy",Mode.NAVY_HQ,List.of(f,r),List.of(),true,List.of(new Theater("sea","sea",0,List.of(),List.of(),Map.of(),List.of(),List.of("fleet"),List.of("reserve"),List.of(),"NORMAL",0)));
        assertEquals("sea",state.selected(navy));assertEquals(List.of(f,r),state.cards(navy));assertEquals("two",state.selected(army));
        var removed=new View("KOR",army.dimension(),army.branch(),army.cards(),List.of(),true,List.of(army.theaters().getFirst()));
        assertEquals("one",state.selected(removed));assertEquals(List.of(a),state.cards(removed));
        state.clear();assertEquals("one",state.selected(army));
    }
}
