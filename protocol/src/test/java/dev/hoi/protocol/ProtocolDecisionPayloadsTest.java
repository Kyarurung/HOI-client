package dev.hoi.protocol;

import com.google.gson.JsonParser;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolDecisionPayloadsTest {
    private static final String TOKEN = "cd7fdc43-1d1e-47bd-8f79-2c7c9b7152bd";
    private static final String SESSION = "ea5f15ab-6f5a-4208-b814-59b55762d194";
    private static ProtocolDecisionPayloads.Row row() {
        return new ProtocolDecisionPayloads.Row("decision","category","국회","category_icon","§Y결정§!","원문\n\n설명","icon",25,34 * 24,
                "재사용 대기",false,"£pol_power §R25§!","§Y조건§!\n\n£trigger_no §R미충족§!\n+2.50%","COOLDOWN",90L * 24);
    }
    @Test void sourceFormattingAndIndependentCategoriesRoundTripWithoutInventedUnknownValues() {
        var category = new ProtocolDecisionPayloads.Category("category","§Y국회§!","의석\n\n£GFX_icon 0.0%","icon","picture","scripted_gui",true);
        var view = new ProtocolDecisionPayloads.View("KOR",SESSION,17,"",0,1,ProtocolCountryHud.UNKNOWN,List.of(row()),List.of(category),2);
        assertEquals(view,ProtocolDecisionPayloads.Response.of(TOKEN,view).view());
        assertNull(view.hud().national().politicalPower());
        var independent = new ProtocolDecisionPayloads.View("KOR",SESSION,17,"",1,0,ProtocolCountryHud.UNKNOWN,List.of(),List.of(category),2);
        assertEquals(independent,ProtocolDecisionPayloads.Response.of(TOKEN,independent).view());
    }
    @Test void legacyJsonAndConstructorsKeepUnknownDurationAndDeriveLegacyPageCount() {
        var view = new ProtocolDecisionPayloads.View("KOR",SESSION,2,"",1,49,ProtocolCountryHud.UNKNOWN,List.of(row()));
        var json = JsonParser.parseString(ProtocolDecisionPayloads.Response.of(TOKEN,view).json()).getAsJsonObject();
        json.remove("categories"); json.remove("pageCount");
        var sourceRow = json.getAsJsonArray("rows").get(0).getAsJsonObject();
        for (String key : List.of("costText","tooltip","phase","totalHours")) sourceRow.remove(key);
        var read = new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view();
        assertEquals(2,read.pageCount()); assertTrue(read.categories().isEmpty());
        assertEquals("",read.rows().getFirst().costText()); assertEquals("",read.rows().getFirst().tooltip());
        assertEquals("",read.rows().getFirst().phase()); assertNull(read.rows().getFirst().totalHours());
        var legacy = new ProtocolDecisionPayloads.Row("id","category","국회","","결정","","",0,0,"",true);
        assertNull(legacy.totalHours()); assertEquals("",legacy.phase());
    }
    @Test void malformedFieldsDuplicateIdsAndBoundsFailClosed() {
        var category = new ProtocolDecisionPayloads.Category("category","","","","","",true);
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,2,ProtocolCountryHud.UNKNOWN,List.of(row(),row())));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,0,ProtocolCountryHud.UNKNOWN,List.of(),List.of(category,category),1));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",2,0,ProtocolCountryHud.UNKNOWN,List.of(),List.of(),2));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,0,ProtocolCountryHud.UNKNOWN,List.of(),List.of(),258));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Category("category","","x".repeat(ProtocolDecisionPayloads.MAX_DESCRIPTION + 1),"","","",true));
        var json = JsonParser.parseString(ProtocolDecisionPayloads.Response.of(TOKEN,new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,1,ProtocolCountryHud.UNKNOWN,List.of(row()))).json()).getAsJsonObject();
        var sourceRow = json.getAsJsonArray("rows").get(0).getAsJsonObject();
        sourceRow.addProperty("totalHours",-1);
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
        sourceRow.addProperty("totalHours",1); sourceRow.addProperty("phase","INVENTED");
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
        sourceRow.addProperty("phase","READY"); sourceRow.addProperty("tooltip","x".repeat(ProtocolDecisionPayloads.MAX_TOOLTIP + 1));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,"x".repeat(ProtocolDecisionPayloads.MAX_JSON + 1)));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Request(ProtocolDecisionPayloads.Action.REFRESH,TOKEN,SESSION,0,"",257));
    }
    @Test void malformedJsonAndDecodedRecordValidationUseTheSameRejectionContract() {
        var category = new ProtocolDecisionPayloads.Category("category","","","","","",true);
        var view = new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,1,ProtocolCountryHud.UNKNOWN,List.of(row()),List.of(category),1);
        var json = JsonParser.parseString(ProtocolDecisionPayloads.Response.of(TOKEN,view).json()).getAsJsonObject();
        json.addProperty("revision",-1);
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
        json.addProperty("revision",0);
        var sourceRow = json.getAsJsonArray("rows").get(0).getAsJsonObject();
        sourceRow.remove("name");
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
        sourceRow.addProperty("name","결정");
        json.getAsJsonArray("categories").get(0).getAsJsonObject().addProperty("description","x".repeat(ProtocolDecisionPayloads.MAX_DESCRIPTION + 1));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
        for (String malformed : List.of("null","[]","{"))
            assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Response(TOKEN,malformed).view());
    }
    @Test void viewCopiesMutableCollectionsAndRejectsOversizedPages() {
        var rows = new ArrayList<>(List.of(row()));
        var categories = new ArrayList<>(List.of(new ProtocolDecisionPayloads.Category("category","","","","","",true)));
        var view = new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,1,ProtocolCountryHud.UNKNOWN,rows,categories,1);
        rows.clear(); categories.clear();
        assertEquals(1,view.rows().size()); assertEquals(1,view.categories().size());
        assertThrows(UnsupportedOperationException.class,() -> view.rows().clear());
        for (int i = 0; i < 49; i++) rows.add(new ProtocolDecisionPayloads.Row("row" + i,"category","","","","","",0,0,"",true));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,49,ProtocolCountryHud.UNKNOWN,rows));
    }
    @Test void unknownCostRoundTripsWithoutBecomingZeroOrAnExecutableQuote() {
        var unknown=new ProtocolDecisionPayloads.Row("unknown","category","국회","","미상 비용","","",(Double)null,0,"비용 확인 불가",false,"","","READY",null);
        var view=new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,1,ProtocolCountryHud.UNKNOWN,List.of(unknown));
        var restored=ProtocolDecisionPayloads.Response.of(TOKEN,view).view();
        assertEquals(view,restored);assertNull(restored.rows().getFirst().cost());assertFalse(restored.rows().getFirst().enabled());
        var json=JsonParser.parseString(ProtocolDecisionPayloads.Response.of(TOKEN,view).json()).getAsJsonObject();
        var sourceRow=json.getAsJsonArray("rows").get(0).getAsJsonObject();sourceRow.remove("cost");
        assertNull(new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view().rows().getFirst().cost());
        sourceRow.addProperty("enabled",true);
        assertThrows(IllegalArgumentException.class,()->new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view());
        sourceRow.addProperty("cost",0);
        assertEquals(0.0,new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view().rows().getFirst().cost());
        for(double invalid:List.of(-1.0,1000001.0,Double.NaN,Double.POSITIVE_INFINITY))
            assertThrows(IllegalArgumentException.class,()->new ProtocolDecisionPayloads.Row("bad","category","","","","","",invalid,0,"",false,"","","READY",null));
    }
}
