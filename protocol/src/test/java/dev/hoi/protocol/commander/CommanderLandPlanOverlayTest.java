package dev.hoi.protocol.commander;

import dev.hoi.protocol.map.MapAtlasScene;
import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderLandPlanOverlayTest {
    @Test void privatePagedGeometryRoundTripsAndRejectsInvalidCountryOrCoordinates() {
        var gson=new Gson();var page=new MapAtlasScene.Page(UUID.randomUUID(),"hoi:atlas_army_1",0,1,List.of(new MapAtlasScene.Box(1,65,2,.18f,.035f,5,.25f,MapAtlasScene.Material.DEPLOYMENT)));
        var overlay=new CommanderHudPayloads.PlanOverlay("KOR",page);assertEquals(overlay,gson.fromJson(gson.toJson(overlay),CommanderHudPayloads.PlanOverlay.class));
        assertThrows(IllegalArgumentException.class,()->new CommanderHudPayloads.PlanOverlay("KOR/PRK",page));assertThrows(NullPointerException.class,()->new CommanderHudPayloads.PlanOverlay("KOR",null));
        var invalid=gson.toJsonTree(overlay).getAsJsonObject();invalid.getAsJsonObject("page").getAsJsonArray("boxes").get(0).getAsJsonObject().addProperty("sx",0);
        assertThrows(RuntimeException.class,()->gson.fromJson(invalid,CommanderHudPayloads.PlanOverlay.class));
        var clear=new CommanderHudPayloads.PlanOverlay("PRK",new MapAtlasScene.Page(UUID.randomUUID(),"hoi:atlas_army_1",0,1,List.of()));assertEquals(clear,gson.fromJson(gson.toJson(clear),CommanderHudPayloads.PlanOverlay.class));
    }
    @Test void movementUvIsBoundedAndSurvivesPayloadSerialization(){
        var q=new CommanderHudPayloads.MovementQuad(1,65,2,.9f,4,.4f,.125f,.5f,.8f,1);var page=new MapAtlasScene.Page(UUID.randomUUID(),"hoi:army",0,1,List.of());var packet=new CommanderHudPayloads.PlanOverlay("KOR",page,List.of(q));var gson=new Gson();assertEquals(packet,gson.fromJson(gson.toJson(packet),CommanderHudPayloads.PlanOverlay.class));
        assertThrows(IllegalArgumentException.class,()->new CommanderHudPayloads.MovementQuad(0,0,0,1,1,0,.7f,0,.5f,1));assertThrows(IllegalArgumentException.class,()->new CommanderHudPayloads.MovementQuad(0,0,0,1,1,0,0,0,Float.NaN,1));assertThrows(IllegalArgumentException.class,()->new CommanderHudPayloads.PlanOverlay("KOR",page,Collections.nCopies(1025,q)));
    }

    @Test void movementGeometryRoundTripsThroughTheActualBoundedWireCodec(){
        var q=new CommanderHudPayloads.MovementQuad(-12,66,7,.9f,4,.4f,.125f,.5f,.8f,1);var page=new MapAtlasScene.Page(UUID.randomUUID(),"hoi:army",0,1,List.of(new MapAtlasScene.Box(1,65,2,1,.1f,4,0,MapAtlasScene.Material.DEPLOYMENT)));var packet=new CommanderHudPayloads.PlanOverlay("KOR",page,Collections.nCopies(1024,q));var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try{CommanderHudPayloads.PlanOverlay.CODEC.encode(buffer,packet);assertEquals(packet,CommanderHudPayloads.PlanOverlay.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
    }

}
