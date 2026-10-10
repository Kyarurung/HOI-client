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
    @Test void movementUvIsBoundedAndSurvivesPayloadSerialization(){
        var q=new ProtocolCommanderHud.MovementQuad(1,65,2,.9f,4,.4f,.125f,.5f,.8f,1);var page=new ProtocolAtlasScene.Page(UUID.randomUUID(),"hoi:army",0,1,List.of());var packet=new ProtocolCommanderHud.PlanOverlay("KOR",page,List.of(q));var gson=new Gson();assertEquals(packet,gson.fromJson(gson.toJson(packet),ProtocolCommanderHud.PlanOverlay.class));
        assertThrows(IllegalArgumentException.class,()->new ProtocolCommanderHud.MovementQuad(0,0,0,1,1,0,.7f,0,.5f,1));assertThrows(IllegalArgumentException.class,()->new ProtocolCommanderHud.MovementQuad(0,0,0,1,1,0,0,0,Float.NaN,1));assertThrows(IllegalArgumentException.class,()->new ProtocolCommanderHud.PlanOverlay("KOR",page,Collections.nCopies(1025,q)));
    }

    @Test void movementGeometryRoundTripsThroughTheActualBoundedWireCodec(){
        var q=new ProtocolCommanderHud.MovementQuad(-12,66,7,.9f,4,.4f,.125f,.5f,.8f,1);var page=new ProtocolAtlasScene.Page(UUID.randomUUID(),"hoi:army",0,1,List.of(new ProtocolAtlasScene.Box(1,65,2,1,.1f,4,0,ProtocolAtlasScene.Material.DEPLOYMENT)));var packet=new ProtocolCommanderHud.PlanOverlay("KOR",page,Collections.nCopies(1024,q));var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try{ProtocolCommanderHud.PlanOverlay.CODEC.encode(buffer,packet);assertEquals(packet,ProtocolCommanderHud.PlanOverlay.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
    }

}
