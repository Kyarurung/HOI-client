package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static dev.hoi.protocol.ProtocolFocusPayloads.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolFocusDetailsTest {
    private static final String SESSION=UUID.randomUUID().toString();
    private static Detail detail(String id,List<DetailLine> requirements) {
        return new Detail(id,"§Y연기§!","첫째\n\n§G둘째§! £pol_power $CHARACTER$ [?amount|.2]",84,12,24,12,1,
                "tfr/common/national_focus/TFR_national_focus_JAP.txt",337,requirements,List.of(),false,"지원되지 않는 효과",false);
    }
    private static View view(Detail detail,Ack ack) {
        var node=new Node("focus","중점","","",0,0,List.of(),List.of(),"조건 미충족",0,false);
        return new View("JAP","일본","tree",SESSION,3,"",ProtocolCountryHud.UNKNOWN,List.of(node),false,detail,ack);
    }
    @Test void detailsAndInputAcknowledgementRoundTripWithoutNormalizingSourceText() {
        var request=new Request(Action.START,UUID.randomUUID().toString(),SESSION,2,"focus");
        var original=view(detail("focus",List.of(new DetailLine("§G이벤트가 완료됨§!",1,Assessment.UNMET,"completed_by_event_tooltip",true))),Ack.of(request));
        var restored=Response.of(request.token(),original).view();
        assertEquals(original,restored);
        assertEquals(2,restored.ack().revision());
        assertEquals(84,restored.detail().durationHours());
        assertEquals("§G이벤트가 완료됨§!",restored.detail().requirements().getFirst().text());
        assertEquals(Assessment.UNMET,restored.detail().requirements().getFirst().assessment());
    }
    @Test void legacyViewWithoutOptionalFieldsRemainsReadable() {
        var gson=new Gson();var data=gson.toJsonTree(view(null,null)).getAsJsonObject();
        data.remove("detail");data.remove("ack");
        var restored=new Response(UUID.randomUUID().toString(),data.toString()).view();
        assertNull(restored.detail());assertNull(restored.ack());assertEquals("focus",restored.nodes().getFirst().id());
    }
    @Test void selectedDetailCannotReferenceAnInvisibleFocusOrExceedBounds() {
        assertThrows(IllegalArgumentException.class,()->view(detail("foreign",List.of()),null));
        assertThrows(IllegalArgumentException.class,()->new DetailLine("text",17,Assessment.UNKNOWN,"",false));
        assertThrows(IllegalArgumentException.class,()->new DetailLine("x".repeat(4097),0,Assessment.UNKNOWN,"",false));
        var large=new DetailLine("x".repeat(4096),0,Assessment.UNKNOWN,"",false);
        assertThrows(IllegalArgumentException.class,()->detail("focus",java.util.Collections.nCopies(17,large)));
        assertThrows(IllegalArgumentException.class,()->detail("focus",java.util.Collections.nCopies(129,new DetailLine("",0,Assessment.UNKNOWN,"",false))));
    }
    @Test void malformedAssessmentCannotBePresentedAsAFailedCondition() {
        var gson=new Gson();var data=gson.toJsonTree(view(detail("focus",List.of(new DetailLine("",0,Assessment.UNKNOWN,"",false))),null)).getAsJsonObject();
        data.getAsJsonObject("detail").getAsJsonArray("requirements").get(0).getAsJsonObject().addProperty("assessment","INVALID");
        assertThrows(IllegalArgumentException.class,()->new Response(UUID.randomUUID().toString(),data.toString()).view());
    }
    @Test void anUnknownDurationIsDistinctFromAFabricatedPositiveValue() {
        var unknown=new Detail("focus","중점","",0,0,0,0,1,"source",1,List.of(),List.of(),false,"원본 기간을 확인할 수 없습니다.",false);
        assertEquals(0,Response.of(UUID.randomUUID().toString(),view(unknown,null)).view().detail().durationHours());
        assertThrows(IllegalArgumentException.class,()->new Detail("focus","중점","",0,1,0,0,1,"source",1,List.of(),List.of(),false,"",false));
    }
}
