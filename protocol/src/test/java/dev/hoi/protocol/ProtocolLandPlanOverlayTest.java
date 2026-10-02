package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolLandPlanOverlayTest {
    @Test void privatePagedGeometryRoundTripsAndRejectsInvalidCountryOrCoordinates() {
        var gson=new Gson();var page=new ProtocolAtlasScene.Page(UUID.randomUUID(),"hoi:atlas_army_1",0,1,List.of(new ProtocolAtlasScene.Box(1,65,2,.18f,.035f,5,.25f,ProtocolAtlasScene.Material.DEPLOYMENT)));
        var overlay=new ProtocolCommanderHud.PlanOverlay("KOR",page);assertEquals(overlay,gson.fromJson(gson.toJson(overlay),ProtocolCommanderHud.PlanOverlay.class));
        assertThrows(IllegalArgumentException.class,()->new ProtocolCommanderHud.PlanOverlay("KOR/PRK",page));assertThrows(NullPointerException.class,()->new ProtocolCommanderHud.PlanOverlay("KOR",null));
        var invalid=gson.toJsonTree(overlay).getAsJsonObject();invalid.getAsJsonObject("page").getAsJsonArray("boxes").get(0).getAsJsonObject().addProperty("sx",0);
        assertThrows(RuntimeException.class,()->gson.fromJson(invalid,ProtocolCommanderHud.PlanOverlay.class));
        var clear=new ProtocolCommanderHud.PlanOverlay("PRK",new ProtocolAtlasScene.Page(UUID.randomUUID(),"hoi:atlas_army_1",0,1,List.of()));assertEquals(clear,gson.fromJson(gson.toJson(clear),ProtocolCommanderHud.PlanOverlay.class));
    }
}
