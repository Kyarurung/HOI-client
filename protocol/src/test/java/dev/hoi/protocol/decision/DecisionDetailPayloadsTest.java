package dev.hoi.protocol.decision;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static dev.hoi.protocol.decision.DecisionDetailPayloads.*;
import static org.junit.jupiter.api.Assertions.*;

class DecisionDetailPayloadsTest {
    private static final String TOKEN="cd7fdc43-1d1e-47bd-8f79-2c7c9b7152bd",SESSION="ea5f15ab-6f5a-4208-b814-59b55762d194";
    private static Line line(String text) {return new Line(text,0,Assessment.UNKNOWN,"",false);}
    private static View view(List<Section> sections) {
        return new View("KOR",SESSION,17,"source_decision","§Y원본 결정§!","첫 줄\n\n£pol_power §G0.0100§!","korea/common/decisions/TFR_decisions_KOR.txt",1527,sections,"미지원 원본은 판정 불명",false);
    }
    private static JsonObject json() {return JsonParser.parseString(Response.of(TOKEN,view(List.of(new Section(Phase.READY_REQUIREMENTS,List.of(line("원문")))))).json()).getAsJsonObject();}
    private static void rejectsJson(JsonObject json) {
        var error=assertThrows(IllegalArgumentException.class,()->new Response(TOKEN,json.toString()).view());
        assertNotNull(error.getCause());
    }
    @Test void sourcePhasesBlankLinesAndExactNumbersSurviveJsonAndBothWireCodecs() {
        String source="£research_icon §G+0.0100§!\n\n---\n[?amount|.2] $CHARACTER$ §R-1.50%§!";
        var sections=new ArrayList<Section>();
        for(var phase:Phase.values())sections.add(new Section(phase,List.of(new Line(source,16,Assessment.NOT_EVALUATED,"source_tooltip",true),line(""))));
        var original=view(sections);
        var request=new Request(TOKEN,SESSION,17,original.id());
        var response=Response.of(TOKEN,original);
        assertEquals(original,response.view());
        assertEquals(List.of(Phase.values()),response.view().sections().stream().map(Section::phase).toList());
        assertEquals(source,response.view().sections().getFirst().lines().getFirst().text());
        assertEquals("",response.view().sections().getFirst().lines().getLast().text());
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            Request.CODEC.encode(buffer,request);assertEquals(request,Request.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());
            Response.CODEC.encode(buffer,response);var decoded=Response.CODEC.decode(buffer);
            assertEquals(response,decoded);assertEquals(original,decoded.view());assertEquals(0,buffer.readableBytes());
        } finally {buffer.release();}
    }
    @Test void requestRequiresCanonicalTokenAndSessionAndBoundsItsDecisionAndRevision() {
        for(String invalid:Arrays.asList(null,"","not-a-uuid","1-1-1-1-1")) {
            assertNotNull(assertThrows(IllegalArgumentException.class,()->new Request(invalid,SESSION,0,"decision")).getCause());
            assertNotNull(assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,invalid,0,"decision")).getCause());
            assertThrows(IllegalArgumentException.class,()->new Response(invalid,"{}"));
        }
        assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,SESSION,-1,"decision"));
        for(String id:Arrays.asList(null,""," ","x".repeat(MAX_ID+1)))assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,SESSION,0,id));
        assertEquals(MAX_ID,new Request(TOKEN,SESSION,Long.MAX_VALUE,"가".repeat(MAX_ID)).id().length());
        assertThrows(IllegalArgumentException.class,()->new Response(TOKEN,null));
        assertThrows(IllegalArgumentException.class,()->new Response(TOKEN,"x".repeat(MAX_JSON+1)));
        assertEquals(MAX_JSON,new Response(TOKEN,"x".repeat(MAX_JSON)).json().length());
        assertThrows(IllegalArgumentException.class,()->Response.of(TOKEN,null));
    }
    @Test void recordValidationRejectsMissingValuesAndDepthOrMetadataOverflowWithoutNullPointerLeaks() {
        for(int depth:List.of(-1,MAX_DEPTH+1))assertThrows(IllegalArgumentException.class,()->new Line("",depth,Assessment.UNKNOWN,"",false));
        assertThrows(IllegalArgumentException.class,()->new Line("x".repeat(MAX_LINE_TEXT+1),0,Assessment.UNKNOWN,"",false));
        assertThrows(IllegalArgumentException.class,()->new Line("",0,Assessment.UNKNOWN,"x".repeat(MAX_SOURCE_KEY+1),false));
        assertNotNull(assertThrows(IllegalArgumentException.class,()->new Line("",0,null,"",false)).getCause());
        assertThrows(IllegalArgumentException.class,()->new Line(null,0,Assessment.UNKNOWN,"",false));
        assertThrows(IllegalArgumentException.class,()->new Line("",0,Assessment.UNKNOWN,null,false));
        assertThrows(IllegalArgumentException.class,()->new Section(null,List.of()));
        assertThrows(IllegalArgumentException.class,()->new Section(Phase.COMPLETE,null));
        assertThrows(IllegalArgumentException.class,()->new Section(Phase.COMPLETE,Arrays.asList((Line)null)));
        assertThrows(IllegalArgumentException.class,()->view(null));
        assertThrows(IllegalArgumentException.class,()->view(Arrays.asList((Section)null)));
        var original=json();
        for(String field:List.of("country","session","id","name","description","source","limitation")) {
            var invalid=original.deepCopy();invalid.add(field,null);rejectsJson(invalid);
        }
        var limits=Map.of("id",MAX_ID,"name",MAX_NAME,"description",MAX_DESCRIPTION,"source",MAX_SOURCE,"limitation",MAX_LIMITATION);
        limits.forEach((field,max)->{var invalid=original.deepCopy();invalid.addProperty(field,"x".repeat(max+1));rejectsJson(invalid);});
        var invalid=original.deepCopy();invalid.addProperty("line",0);rejectsJson(invalid);
        invalid=original.deepCopy();invalid.addProperty("revision",-1);rejectsJson(invalid);
        invalid=original.deepCopy();invalid.addProperty("country","lowercase");rejectsJson(invalid);
    }
    @Test void combinedLineSectionAndTextBudgetsApplyAcrossEveryPhase() {
        var empty=line("");
        assertEquals(MAX_LINES,view(List.of(new Section(Phase.COMPLETE,Collections.nCopies(MAX_LINES,empty)))).sections().getFirst().lines().size());
        assertThrows(IllegalArgumentException.class,()->new Section(Phase.COMPLETE,Collections.nCopies(MAX_LINES+1,empty)));
        assertThrows(IllegalArgumentException.class,()->view(List.of(new Section(Phase.COMPLETE,Collections.nCopies(128,empty)),new Section(Phase.REMOVE,Collections.nCopies(129,empty)))));
        assertThrows(IllegalArgumentException.class,()->view(Collections.nCopies(MAX_SECTIONS+1,new Section(Phase.COMPLETE,List.of()))));
        var metadata=view(List.of());
        int remaining=MAX_TEXT-metadata.country().length()-metadata.session().length()-metadata.id().length()-metadata.name().length()
                -metadata.description().length()-metadata.source().length()-metadata.limitation().length();
        var lines=new ArrayList<Line>();
        while(remaining>0) {int length=Math.min(remaining,MAX_LINE_TEXT);lines.add(line("가".repeat(length)));remaining-=length;}
        assertEquals(lines.size(),view(List.of(new Section(Phase.COMPLETE,lines))).sections().getFirst().lines().size());
        lines.add(line("x"));
        assertThrows(IllegalArgumentException.class,()->view(List.of(new Section(Phase.COMPLETE,lines))));
        var keyed=new ArrayList<Line>();
        for(int i=0;i<14;i++)keyed.add(new Line("x".repeat(MAX_LINE_TEXT),0,Assessment.MET,"k".repeat(MAX_SOURCE_KEY),true));
        assertThrows(IllegalArgumentException.class,()->view(List.of(new Section(Phase.COMPLETE,keyed))));
    }
    @Test void requiredNewChannelFieldsCannotFallBackToLegacyPrimitiveDefaults() {
        var original=json();
        for(String field:List.of("country","session","revision","id","name","description","source","line","sections","limitation","complete")) {
            var invalid=original.deepCopy();invalid.remove(field);rejectsJson(invalid);
        }
        for(String field:List.of("phase","lines")) {
            var invalid=original.deepCopy();invalid.getAsJsonArray("sections").get(0).getAsJsonObject().remove(field);rejectsJson(invalid);
        }
        for(String field:List.of("text","depth","assessment","sourceKey","supported")) {
            var invalid=original.deepCopy();invalid.getAsJsonArray("sections").get(0).getAsJsonObject().getAsJsonArray("lines").get(0).getAsJsonObject().remove(field);rejectsJson(invalid);
        }
        var invalid=original.deepCopy();invalid.addProperty("complete","true");rejectsJson(invalid);
        invalid=original.deepCopy();invalid.getAsJsonArray("sections").get(0).getAsJsonObject().getAsJsonArray("lines").get(0).getAsJsonObject().addProperty("supported","false");rejectsJson(invalid);
    }
    @Test void malformedEnumsAndJsonRetainTheRejectionCauseInsteadOfInventingAssessments() {
        var original=json();
        var invalid=original.deepCopy();invalid.getAsJsonArray("sections").get(0).getAsJsonObject().addProperty("phase","EXECUTE");rejectsJson(invalid);
        invalid=original.deepCopy();invalid.getAsJsonArray("sections").get(0).getAsJsonObject().getAsJsonArray("lines").get(0).getAsJsonObject().addProperty("assessment","FALSE");rejectsJson(invalid);
        for(String malformed:List.of("null","[]","{","{}"))assertNotNull(assertThrows(IllegalArgumentException.class,()->new Response(TOKEN,malformed).view()).getCause());
        for(var assessment:Assessment.values()) {
            var expected=view(List.of(new Section(Phase.READY_REQUIREMENTS,List.of(new Line("조건",1,assessment,"source_condition",false)))));
            assertEquals(assessment,Response.of(TOKEN,expected).view().sections().getFirst().lines().getFirst().assessment());
        }
    }
    @Test void immutableCopiesKeepSourceSectionAndLineOrderAfterCallerCollectionsChange() {
        var lines=new ArrayList<>(List.of(line("첫 줄"),line(""),line("마지막 줄")));
        var section=new Section(Phase.REMOVE,lines);var sections=new ArrayList<>(List.of(section));var original=view(sections);
        lines.clear();sections.clear();
        assertEquals(List.of("첫 줄","","마지막 줄"),original.sections().getFirst().lines().stream().map(Line::text).toList());
        assertThrows(UnsupportedOperationException.class,()->original.sections().clear());
        assertThrows(UnsupportedOperationException.class,()->original.sections().getFirst().lines().clear());
    }
    @Test void dedicatedChannelsRegisterIdempotentlyWithoutChangingDecisionActions() {
        assertEquals("hoi:decision_detail_request_v1",Request.TYPE.id().toString());
        assertEquals("hoi:decision_detail_response_v1",Response.TYPE.id().toString());
        assertEquals(List.of("OPEN","REFRESH","TAKE","HIGHLIGHT","GOTO"),Arrays.stream(DecisionPayloads.Action.values()).map(Enum::name).toList());
        assertDoesNotThrow(()->{registerPayloadTypes();registerPayloadTypes();});
    }
}
