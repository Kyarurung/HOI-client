package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapCommanderPanelOrderTest {
    private static Member member(String id){return new Member(id,id,"","","",1,new Open(Mode.ARMY_UNITS,id,List.of()));}
    private static Card card(String id,Mode role,String parent,String... ids){var members=Arrays.stream(ids).map(MapCommanderPanelOrderTest::member).toList();return new Card(id,id,"","politics/empty/leader","","",members.size(),24,"",role,new Panel(parent,"",null,members,members.size(),List.of(),List.of(ids)));}
    private static final Card GROUP=card("G",Mode.ARMY_GROUP,"","07","03","01","06","00","05","02","04");
    private static final Card FIRST=card("A",Mode.ASSIGN_ARMY,"G","06","01");
    private static final Card SECOND=card("B",Mode.ASSIGN_ARMY,"G","05","02");
    private static final Card THIRD=card("C",Mode.ASSIGN_ARMY,"G","04","03");
    private static final List<Member> ROWS=GROUP.panel().members().stream().sorted(Comparator.comparing(Member::id)).toList();
    private static List<String> ids(List<Member> rows){return rows.stream().map(Member::id).toList();}
    @Test void airCardsSelectOnlyWingsAttachedToTheirArmyOrChildArmies(){
        var wings=new ArrayList<Member>();
        for(String army:List.of("A","B","C","OTHER",""))wings.add(new Member("wing"+wings.size(),"wing","","","",1,new Open(Mode.AIR_MISSION,""),"",null,Map.of(),"","",new MemberData("",army,1,null)));
        var view=new View("KOR","minecraft:overworld",Mode.AIR,List.of(GROUP,FIRST,SECOND,THIRD),wings,true);
        assertEquals(List.of(),MapCommanderPanels.attachedWings(view,Set.of()));
        assertEquals(List.of("wing0"),MapCommanderPanels.attachedWings(view,Set.of("A")));
        assertEquals(List.of("wing1"),MapCommanderPanels.attachedWings(view,Set.of("B")));
        assertEquals(List.of("wing0","wing1","wing2"),MapCommanderPanels.attachedWings(view,Set.of("G","A")));
        var moved=new View("KOR","minecraft:overworld",Mode.AIR,List.of(GROUP,FIRST,card("B",Mode.ASSIGN_ARMY,"OTHER"),THIRD),wings,true);
        assertEquals(List.of("wing0","wing2"),MapCommanderPanels.attachedWings(moved,Set.of("G")));
    }
    @Test void groupConcatenatesChildArmiesAndKeepsUnmatchedRowsLast(){
        var cards=List.of(GROUP,FIRST,SECOND,THIRD);var expected=new ArrayList<String>();
        for(var army:List.of(FIRST,SECOND,THIRD))expected.addAll(ids(MapCommanderPanels.armyRows(army,cards,ROWS)));
        expected.addAll(List.of("00","07"));
        assertEquals(List.of("01","06","02","05","03","04","00","07"),expected);
        assertEquals(expected,ids(MapCommanderPanels.armyRows(GROUP,cards,ROWS)));
        assertEquals(List.of("00","01","02","03","04","05","06","07"),ids(ROWS));
    }
    @Test void groupFollowsNewCardOrderAndCurrentMembership(){
        assertEquals(List.of("03","04","01","06","02","05","00","07"),ids(MapCommanderPanels.armyRows(GROUP,List.of(GROUP,THIRD,FIRST,SECOND),ROWS)));
        var detached=card("A",Mode.ASSIGN_ARMY,"","06","01");var moved=card("B",Mode.ASSIGN_ARMY,"OTHER","05","02");
        assertEquals(List.of("03","04","00","01","02","05","06","07"),ids(MapCommanderPanels.armyRows(GROUP,List.of(GROUP,detached,moved,THIRD),ROWS)));
    }
    @Test void partialSelectionAndArmyRowsKeepTheirExistingOrder(){
        var rows=ROWS.stream().filter(m->Set.of("00","02","04","06").contains(m.id())).toList();var cards=List.of(GROUP,THIRD,SECOND,FIRST);
        assertEquals(List.of("04","02","06","00"),ids(MapCommanderPanels.armyRows(GROUP,cards,rows)));
        assertEquals(List.of("01","06"),ids(MapCommanderPanels.armyRows(FIRST,cards,ROWS)));
        assertEquals(ids(ROWS),ids(MapCommanderPanels.armyRows(card("reserve",Mode.ARMY,"", "07","06","05","04","03","02","01","00"),cards,ROWS)));
    }
    @Test void dragPanelsFollowCardOrderAndRequireOneSharedParent(){
        var keys=Set.of("unit/01","unit/02","unit/03","unit/unassigned");
        assertEquals(List.of(GROUP,THIRD,FIRST,SECOND),MapCommanderPanels.dragPanels(List.of(GROUP,THIRD,FIRST,SECOND),keys));
        var detached=card("A",Mode.ASSIGN_ARMY,"","01");
        var other=card("B",Mode.ASSIGN_ARMY,"OTHER","02");
        assertEquals(List.of(THIRD,detached,SECOND),MapCommanderPanels.dragPanels(List.of(GROUP,THIRD,detached,SECOND),keys));
        assertEquals(List.of(THIRD,FIRST,other),MapCommanderPanels.dragPanels(List.of(GROUP,THIRD,FIRST,other),keys));
        assertEquals(List.of(FIRST),MapCommanderPanels.dragPanels(List.of(GROUP,FIRST,SECOND),Set.of("unit/01")));
        assertEquals(List.of(),MapCommanderPanels.dragPanels(List.of(GROUP,FIRST,SECOND),Set.of("unit/unassigned")));
    }
}
