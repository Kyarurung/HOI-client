package dev.hoi.protocol;

import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCommanderMembershipTest {
    @Test void completeMembershipSurvivesTruncatedRowsAndJsonRoundTrip(){
        var ids=java.util.stream.IntStream.range(0,150).mapToObj(i->"division-"+i).toList();var rows=ids.stream().limit(120).map(id->new Member(id,id,"","unit_hud/infantry","",1,new Open(Mode.ARMY_UNITS,"army"))).toList();
        var panel=new Panel("group","",null,rows,150,List.of(),ids);var card=new Card("army","Army","","politics/empty/leader","","",150,18,"",Mode.ASSIGN_ARMY,panel);var view=new View("KOR","hoi:atlas_army_kor",Mode.ARMY,List.of(card));
        assertEquals(ids,Snapshot.of(view).view().cards().getFirst().panel().divisionIds());assertEquals(120,panel.members().size());
        var compact=new Panel("group","",null,List.of(),150,List.of(),ids);assertEquals(ids,compact.divisionIds());
        assertThrows(IllegalArgumentException.class,()->new Panel("","",null,List.of(),150,List.of(),ids.subList(0,120)));
        assertThrows(IllegalArgumentException.class,()->new Panel("","",null,List.of(),2,List.of(),List.of("same","same")));
    }
    @Test void legacyCompleteRowsRecoverMembershipButPartialRowsNeverPretendToBeComplete(){
        var member=new Member("D1","Unit","","unit_hud/infantry","",1,new Open(Mode.ARMY_UNITS,"army"));assertEquals(List.of("D1"),new Panel("","",null,List.of(member),1).divisionIds());assertNull(new Panel("","",null,List.of(member),2).divisionIds());
        var ids=java.util.stream.IntStream.range(0,2048).mapToObj(i->"D"+i).toList();assertEquals(2048,new Panel("","",null,List.of(),2048,List.of(),ids).divisionIds().size());
        var excessive=new ArrayList<>(ids);excessive.add("overflow");assertThrows(IllegalArgumentException.class,()->new Panel("","",null,List.of(),2049,List.of(),excessive));
    }
}
