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
    @Test void targetedRowsCarryTheirTargetInTheRowIdAndRoundTrip() {
        var country = new ProtocolDecisionPayloads.Target("country","PRK","북한","PRK_communism");
        var state = new ProtocolDecisionPayloads.Target("state","530","경기",null);
        var toCountry = new ProtocolDecisionPayloads.Row("demand@c:PRK","category","국회","","요구","","",(Double)25.0,0,"",true,"25","","READY",null,country,List.of(530,531));
        var toState = new ProtocolDecisionPayloads.Row("build@s:530","category","국회","","건설","","",(Double)null,0,"",false,"","","",null,state,null);
        var view = new ProtocolDecisionPayloads.View("KOR",SESSION,3,"",0,3,ProtocolCountryHud.UNKNOWN,List.of(toCountry,toState,row()));
        var read = ProtocolDecisionPayloads.Response.of(TOKEN,view).view();
        assertEquals(view,read);
        assertEquals(List.of(),read.rows().get(1).highlightStates());
        assertEquals("",read.rows().get(1).target().flag());
        assertNull(read.rows().get(2).target());
        assertEquals(List.of(),read.rows().get(2).highlightStates());
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Row("demand@c:CHI","category","","","","","",(Double)0.0,0,"",true,"","","",null,country,List.of()));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Row("demand@c:PRK","category","","","","","",(Double)0.0,0,"",true,"","","",null,country,List.of(0)));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Row("demand@c:PRK","category","","","","","",(Double)0.0,0,"",true,"","","",null,country,List.of(5,5)));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Target("state","0","",""));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Target("state","530","","KOR"));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Target("province","530","",""));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Target("country","kor","",""));
    }
    @Test void costProgressHighlightAndMapRequestsAreBounded() {
        var blocked = new ProtocolDecisionPayloads.Row("export","category","","","수출","","",(Double)20.0,0,"",false,"§A§Y자주포§! 250대 필요§!","","READY",0L,null,List.of(1410),false,"",2);
        var mission = new ProtocolDecisionPayloads.Row("mission","category","","","임무","","",(Double)0.0,144,"",false,"","","ACTIVE",240L,null,List.of(),null,"BAD",null);
        var view = new ProtocolDecisionPayloads.View("KOR",SESSION,1,"",0,2,ProtocolCountryHud.UNKNOWN,List.of(blocked,mission));
        var read = ProtocolDecisionPayloads.Response.of(TOKEN,view).view();
        assertEquals(view,read); assertEquals(Boolean.FALSE,read.rows().getFirst().costMet()); assertEquals(2,read.rows().getFirst().highlightColor());
        assertNull(read.rows().get(1).costMet()); assertEquals("BAD",read.rows().get(1).progress());
        var json = JsonParser.parseString(ProtocolDecisionPayloads.Response.of(TOKEN,new ProtocolDecisionPayloads.View("KOR",SESSION,0,"",0,1,ProtocolCountryHud.UNKNOWN,List.of(row()))).json()).getAsJsonObject();
        var legacy = json.getAsJsonArray("rows").get(0).getAsJsonObject();
        legacy.remove("costMet"); legacy.remove("progress"); legacy.remove("highlightColor");
        var restored = new ProtocolDecisionPayloads.Response(TOKEN,json.toString()).view().rows().getFirst();
        assertNull(restored.costMet()); assertEquals("",restored.progress()); assertNull(restored.highlightColor());
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Row("x","c","","","","","",(Double)0.0,0,"",false,"","","READY",0L,null,List.of(),null,"GOOD",null));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Row("x","c","","","","","",(Double)0.0,0,"",false,"","","ACTIVE",0L,null,List.of(),null,"FAST",null));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Row("x","c","","","","","",(Double)0.0,0,"",false,"","","",0L,null,List.of(),null,"",64));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Request(ProtocolDecisionPayloads.Action.GOTO,TOKEN,"",0,"x",0));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Request(ProtocolDecisionPayloads.Action.HIGHLIGHT,TOKEN,SESSION,0,"",0));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Request(ProtocolDecisionPayloads.Action.HIGHLIGHT,TOKEN,SESSION,0,"x",1));
        var page = new ProtocolAtlasScene.Page(UUID.randomUUID(),"hoi:atlas_army",0,1,List.of(new ProtocolAtlasScene.Box(1,2,3,1,.015f,1,0,ProtocolAtlasScene.Material.DEPLOYMENT)));
        var highlight = new ProtocolDecisionPayloads.Highlight(TOKEN,SESSION,1,"KOR_arms_factory_ulsan",page);
        assertEquals("KOR_arms_factory_ulsan",highlight.row());
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Highlight(TOKEN,SESSION,1,"",page));
        assertThrows(IllegalArgumentException.class,() -> new ProtocolDecisionPayloads.Highlight(TOKEN,SESSION,-1,"x",page));
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
