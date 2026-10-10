package dev.hoi.protocol;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolDecisionPanelTest {
    private static final String TOKEN = "cd7fdc43-1d1e-47bd-8f79-2c7c9b7152bd";
    private static final String SESSION = "ea5f15ab-6f5a-4208-b814-59b55762d194";
    private static final String SOURCE = "korea/interface/TFRKP/TFR_Korea_NatAsm.gui";
    private static ProtocolDecisionPanel.Layout layout() {
        return new ProtocolDecisionPanel.Layout(null,null,null,null,null,null,null,null,null,null,null,null);
    }
    private static ProtocolDecisionPanel.Element element(String name, String kind, Double frame, ProtocolDecisionPanel.Text text,
            Boolean visible, List<ProtocolDecisionPanel.Element> children) {
        return new ProtocolDecisionPanel.Element(name,kind,SOURCE,2,layout(),null,frame,null,text,null,visible,null,children);
    }
    private static ProtocolDecisionPanel panel(List<ProtocolDecisionPanel.Element> elements) {
        return new ProtocolDecisionPanel("TFR_KOR_national_assembly","TFR_KOR_national_assembly_container",
                "korea/common/scripted_guis/TFR_scripted_guis_NatAsm.txt",2,elements,List.of());
    }
    private static ProtocolDecisionPanel sample() {
        return panel(List.of(element("caption","text",null,new ProtocolDecisionPanel.Text("원문",false),null,List.of())));
    }
    private static ProtocolDecisionPayloads.Response response(ProtocolDecisionPanel panel) {
        var category = new ProtocolDecisionPayloads.Category("category","국회","","","","TFR_KOR_national_assembly",true,panel);
        return ProtocolDecisionPayloads.Response.of(TOKEN,new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,0,
                ProtocolCountryHud.UNKNOWN,List.of(),List.of(category),1));
    }
    private static JsonObject json(ProtocolDecisionPanel panel) { return JsonParser.parseString(response(panel).json()).getAsJsonObject(); }
    private static JsonObject panel(JsonObject json) { return json.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonObject("panel"); }
    private static JsonObject element(JsonObject json) { return panel(json).getAsJsonArray("elements").get(0).getAsJsonObject(); }
    private static void rejects(Consumer<JsonObject> mutation) {
        var json = json(sample()); mutation.accept(json);
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
    }
    @Test void originalTreeGeometryFormattingTooltipsAndUnknownValuesRoundTrip() {
        var sourceLayout = new ProtocolDecisionPanel.Layout(-0.0,-230.0,new ProtocolDecisionPanel.Measure(100.0,true),
                new ProtocolDecisionPanel.Measure(0.0,false),"center_down",true,0.8,"center","VCR02_12",false,null,true);
        String sourceText = "§Y원문§!\n\n---\n£GFX_KOR_NatAsm_Logo [?KOR_DP_right|1%] $CHARACTER$ +2.50%";
        var tooltip = new ProtocolDecisionPanel.Tooltip(new ProtocolDecisionPanel.Text("§Y즉시§!",true),
                new ProtocolDecisionPanel.Text("지연\n\n0.0%",false));
        var unknown = new ProtocolDecisionPanel.Element("NatAsm_DP_bar","radial",SOURCE,12,sourceLayout,"GFX_KOR_DP_Bar",null,
                "KOR_DP_right",new ProtocolDecisionPanel.Text(sourceText,false),tooltip,null,null,List.of());
        var known = element("known","icon",0.0,null,false,List.of());
        var root = element("root","container",null,null,true,List.of(unknown,known));
        var expected = new ProtocolDecisionPanel("TFR_KOR_national_assembly","TFR_KOR_national_assembly_container",
                "korea/common/scripted_guis/TFR_scripted_guis_NatAsm.txt",2,List.of(root),List.of("frame:NatAsm_DP_bar/KOR_DP_right"));
        var actual = response(expected).view().categories().getFirst().panel();
        assertEquals(expected,actual);
        var child = actual.elements().getFirst().children().getFirst();
        assertNull(child.frame()); assertNull(child.visible()); assertEquals(sourceText,child.text().value());
        assertFalse(child.text().complete()); assertEquals("KOR_DP_right",child.frameVariable());
        assertEquals(Double.doubleToRawLongBits(-0.0),Double.doubleToRawLongBits(child.layout().x()));
        assertEquals(0.0,actual.elements().getFirst().children().get(1).frame());
        assertFalse(actual.elements().getFirst().children().get(1).visible());
        assertNull(response(null).view().categories().getFirst().panel());
    }
    @Test void gridKeepsRelativeSlotsAndUnresolvedArrayWithoutInventingChildren() {
        var repeat = new ProtocolDecisionPanel.Repeat("KOR_NatAsm_factions","KOR_NatAsm_faction_idx","KOR_NatAsm_faction","NatAsm_member_entry");
        var grid = new ProtocolDecisionPanel.Grid(new ProtocolDecisionPanel.Measure(200.0,false),new ProtocolDecisionPanel.Measure(25.0,false),10,false,repeat);
        var node = new ProtocolDecisionPanel.Element("NatAsm_member_grid","grid",SOURCE,85,layout(),null,null,null,null,null,true,grid,List.of());
        var expected = panel(List.of(node));
        assertEquals(expected,response(expected).view().categories().getFirst().panel());
        assertTrue(expected.elements().getFirst().children().isEmpty());
        assertThrows(IllegalArgumentException.class,() -> element("grid","grid",null,null,true,List.of()));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPanel.Element("icon","icon",SOURCE,2,layout(),null,null,null,null,null,true,grid,List.of()));
    }
    @Test void mutableCollectionsAreCopiedAndMissingRequiredFieldsFailClosed() {
        var children = new ArrayList<>(List.of(element("child","icon",null,null,null,List.of())));
        var root = element("root","container",null,null,null,children);
        var elements = new ArrayList<>(List.of(root)); var unresolved = new ArrayList<>(List.of("text:missing"));
        var panel = new ProtocolDecisionPanel("gui","window","tfr/common/scripted_guis/panel.txt",1,elements,unresolved);
        children.clear(); elements.clear(); unresolved.clear();
        assertEquals(1,panel.elements().size()); assertEquals(1,root.children().size()); assertEquals(1,panel.unresolved().size());
        assertThrows(UnsupportedOperationException.class,() -> panel.elements().clear());
        assertThrows(UnsupportedOperationException.class,() -> root.children().clear());
        assertThrows(UnsupportedOperationException.class,() -> panel.unresolved().clear());
        for (String key : List.of("scriptedGui","windowName","source","line","elements","unresolved")) {
            rejects(json -> panel(json).remove(key)); rejects(json -> panel(json).add(key,JsonNull.INSTANCE));
        }
        for (String key : List.of("name","kind","source","line","layout","children")) {
            rejects(json -> element(json).remove(key)); rejects(json -> element(json).add(key,JsonNull.INSTANCE));
        }
        for (String key : List.of("value","complete")) rejects(json -> element(json).getAsJsonObject("text").remove(key));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPanel.Text("x",null));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPanel.Measure(null,false));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPanel.Measure(1.0,null));
    }
    @Test void nonfiniteUnsafeAndUnsupportedValuesAreRejectedWithoutClamping() {
        for (double value : new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1,1_000_001})
            assertThrows(IllegalArgumentException.class,() -> element("icon","icon",value,null,null,List.of()));
        for (double value : new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,16_385})
            assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPanel.Measure(value,false));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPanel.Measure(101.0,true));
        assertThrows(IllegalArgumentException.class,() -> element("button","button",null,null,null,List.of()));
        assertThrows(IllegalArgumentException.class,() -> element("leaf","icon",null,null,null,List.of(element("child","icon",null,null,null,List.of()))));
        for (String source : List.of("../panel.gui","korea/interface/../panel.gui","file:///panel.gui","korea//panel.gui","korea/interface/panel.exe"))
            assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPanel("gui","window",source,1,sample().elements(),List.of()));
        rejects(json -> element(json).addProperty("kind","button"));
        rejects(json -> element(json).addProperty("frame","NaN"));
        rejects(json -> element(json).addProperty("frame","Infinity"));
        rejects(json -> element(json).addProperty("frame",-1));
        rejects(json -> element(json).addProperty("frame",1_000_001));
        rejects(json -> element(json).getAsJsonObject("layout").addProperty("x",16_385));
        rejects(json -> element(json).getAsJsonObject("layout").addProperty("scale",0));
        rejects(json -> element(json).getAsJsonObject("layout").addProperty("scale",17));
        rejects(json -> element(json).getAsJsonObject("layout").addProperty("orientation","invented"));
        rejects(json -> element(json).getAsJsonObject("layout").addProperty("alignment","invented"));
        rejects(json -> element(json).addProperty("line",0));
        rejects(json -> element(json).addProperty("line",1.5));
        rejects(json -> element(json).addProperty("line",1_000_001));
        rejects(json -> element(json).addProperty("line",new java.math.BigDecimal("1e999999999")));
    }
    @Test void wireTypesRejectCoercionDuplicatesAndMalformedNestedValues() {
        rejects(json -> element(json).addProperty("name",123));
        rejects(json -> element(json).addProperty("visible","false"));
        rejects(json -> element(json).addProperty("frame","1"));
        rejects(json -> element(json).getAsJsonObject("text").addProperty("complete","true"));
        rejects(json -> element(json).getAsJsonObject("text").addProperty("value",123));
        rejects(json -> panel(json).getAsJsonArray("unresolved").add(123));
        rejects(json -> element(json).getAsJsonArray("children").add(JsonNull.INSTANCE));
        rejects(json -> {
            var measure = new JsonObject(); measure.addProperty("value",100); measure.addProperty("relative","true");
            element(json).getAsJsonObject("layout").add("width",measure);
        });
        rejects(json -> {
            var measure = new JsonObject(); measure.addProperty("value",100);
            element(json).getAsJsonObject("layout").add("width",measure);
        });
        rejects(json -> element(json).add("tooltip",new JsonObject()));
        String source = response(sample()).json().replace("\"scriptedGui\":\"TFR_KOR_national_assembly\"",
                "\"scriptedGui\":\"TFR_KOR_national_assembly\",\"scriptedGui\":\"duplicate\"");
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,source).view());
    }
    @Test void exactTreeAndTextBoundsAreAcceptedAndNextValueIsRejected() {
        var elements = new ArrayList<ProtocolDecisionPanel.Element>();
        for (int i = 0; i < 128; i++) elements.add(element("icon" + i,"icon",null,null,null,List.of()));
        assertEquals(128,response(panel(elements)).view().categories().getFirst().panel().elements().size());
        var excessive = json(panel(elements));
        panel(excessive).getAsJsonArray("elements").add(element(excessive).deepCopy());
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,excessive.toString()).view());
        elements.add(element("excess","icon",null,null,null,List.of()));
        assertThrows(IllegalArgumentException.class,() -> panel(elements));
        var text = new ProtocolDecisionPanel.Text("x".repeat(4_096),true);
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPanel.Text("x".repeat(4_097),true));
        var textElements = new ArrayList<ProtocolDecisionPanel.Element>();
        for (int i = 0; i < 8; i++) textElements.add(element("text" + i,"text",null,text,null,List.of()));
        assertEquals(8,response(panel(textElements)).view().categories().getFirst().panel().elements().size());
        textElements.add(element("excess","text",null,new ProtocolDecisionPanel.Text("x",true),null,List.of()));
        assertThrows(IllegalArgumentException.class,() -> panel(textElements));
        rejects(json -> element(json).getAsJsonObject("text").addProperty("value","x".repeat(4_097)));
        var json = json(panel(textElements.subList(0,8)));
        var ninth = element(json).deepCopy(); ninth.addProperty("name","excess"); ninth.getAsJsonObject("text").addProperty("value","x");
        panel(json).getAsJsonArray("elements").add(ninth);
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
    }
    @Test void immediateAndDelayedTooltipsShareThePanelTextBudget() {
        var text = new ProtocolDecisionPanel.Text("x".repeat(4_096),true);
        var full = new ProtocolDecisionPanel.Element("full","text",SOURCE,2,layout(),null,null,null,text,
                new ProtocolDecisionPanel.Tooltip(text,text),true,null,List.of());
        var rest = new ProtocolDecisionPanel.Element("rest","text",SOURCE,2,layout(),null,null,null,text,
                new ProtocolDecisionPanel.Tooltip(text,null),true,null,List.of());
        var exact = panel(List.of(full,full,rest));
        assertEquals(exact,response(exact).view().categories().getFirst().panel());
        var json = json(exact);
        var tip = panel(json).getAsJsonArray("elements").get(2).getAsJsonObject().getAsJsonObject("tooltip");
        var excess = new JsonObject(); excess.addProperty("value","x"); excess.addProperty("complete",true);
        tip.add("delayed",excess);
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
    }
    @Test void depthIncludesRootAndExcessiveWireNestingFailsBeforeUnboundedRecursion() {
        var chain = element("leaf","icon",null,null,null,List.of());
        for (int depth = 2; depth <= 8; depth++) chain = element("level" + depth,"container",null,null,null,List.of(chain));
        assertEquals(panel(List.of(chain)),response(panel(List.of(chain))).view().categories().getFirst().panel());
        var deepest = chain;
        assertThrows(IllegalArgumentException.class,() -> element("ninth","container",null,null,null,List.of(deepest)));
        var json = json(sample()); var child = element(json).deepCopy();
        for (int depth = 0; depth < 64; depth++) {
            var parent = element(json).deepCopy(); parent.addProperty("kind","container");
            parent.getAsJsonArray("children").add(child); child = parent;
        }
        panel(json).getAsJsonArray("elements").set(0,child);
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
    }
}
