package dev.hoi.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.hoi.protocol.ProtocolCommanderHud.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolLandPlanMapTest {
    @Test void boundedMapRaysRoundTripWithPrivateIssuedIdentity() {
        var points=new ArrayList<Ray>();for(int i=0;i<64;i++)points.add(new Ray(0,-1,0));
        var request=new PlanMapPick(UUID.randomUUID().toString(),42,false,points);points.clear();assertEquals(64,request.rays().size());
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{PlanMapPick.CODEC.encode(buffer,request);assertEquals(request,PlanMapPick.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
        assertEquals(0,Mode.ARMY.ordinal());assertEquals(12,Mode.AIR_MISSION.ordinal());
    }
    @Test void invalidRayAndUnboundedWirePayloadFailBeforeAllocation() {
        String token=UUID.randomUUID().toString();var ray=new Ray(0,-1,0);
        assertThrows(IllegalArgumentException.class,()->new Ray(Double.NaN,-1,0));assertThrows(IllegalArgumentException.class,()->new Ray(0,1,0));assertThrows(IllegalArgumentException.class,()->new Ray(0,-2,0));
        assertThrows(IllegalArgumentException.class,()->new PlanMapPick(token,0,false,List.of()));assertThrows(IllegalArgumentException.class,()->new PlanMapPick(token,0,true,List.of(ray,ray)));assertThrows(IllegalArgumentException.class,()->new PlanMapPick(token,-1,true,List.of(ray)));
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{buffer.writeUtf(token,36);buffer.writeVarLong(0);buffer.writeBoolean(false);buffer.writeVarInt(65);assertThrows(IllegalArgumentException.class,()->PlanMapPick.CODEC.decode(buffer));}finally{buffer.release();}
    }
}
