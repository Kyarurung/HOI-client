package dev.hoi.protocol;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolNavalInvasionTest {
    private static ProtocolNavalInvasion.Plan plan(String id){return new ProtocolNavalInvasion.Plan(id,"army","1".repeat(64),"DRAFT",new ProtocolNavalInvasion.Point(-1,2),new ProtocolNavalInvasion.Point(3,4),List.of(ProtocolNavalInvasion.Action.RESERVE),false,"§Y원문§!\n\n설명");}
    @Test void sourceControlsRoundTripWithoutChangingExistingContextWireFormat(){
        var view=new ProtocolNavalInvasion.View(UUID.randomUUID().toString(),9,"KOR","hoi:atlas_army_kor",List.of(plan("plan")));var snapshot=ProtocolNavalInvasion.Snapshot.of(view);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);try{ProtocolNavalInvasion.Snapshot.CODEC.encode(buffer,snapshot);assertEquals(view,ProtocolNavalInvasion.Snapshot.CODEC.decode(buffer).view());assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
    }
    @Test void malformedControlsAndOversizedCollectionsFail(){
        assertThrows(IllegalArgumentException.class,()->new ProtocolNavalInvasion.Point(Double.NaN,0));
        var valid=plan("plan");var json=new Gson().toJsonTree(new ProtocolNavalInvasion.View(UUID.randomUUID().toString(),0,"KOR","hoi:atlas_army_kor",List.of(valid))).getAsJsonObject();
        json.getAsJsonArray("plans").get(0).getAsJsonObject().addProperty("phase","TRANSIT");assertThrows(RuntimeException.class,()->new ProtocolNavalInvasion.Snapshot(json.toString()).view());
        assertThrows(IllegalArgumentException.class,()->new ProtocolNavalInvasion.View(UUID.randomUUID().toString(),0,"KOR","x",List.of(valid,valid)));
        assertThrows(IllegalArgumentException.class,()->new ProtocolNavalInvasion.View(UUID.randomUUID().toString(),0,"KOR","x",java.util.stream.IntStream.range(0,129).mapToObj(i->plan("plan/"+i)).toList()));
        assertThrows(IllegalArgumentException.class,()->new ProtocolNavalInvasion.Snapshot("한".repeat(90000)));
    }
}
