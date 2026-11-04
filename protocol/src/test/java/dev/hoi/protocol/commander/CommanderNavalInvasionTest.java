package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderNavalInvasionTest {
    private static CommanderNavalInvasion.Plan plan(String id){return new CommanderNavalInvasion.Plan(id,"army","1".repeat(64),"DRAFT",new CommanderNavalInvasion.Point(-1,2),new CommanderNavalInvasion.Point(3,4),List.of(CommanderNavalInvasion.Action.RESERVE),false,"§Y원문§!\n\n설명");}
    @Test void sourceControlsRoundTripWithoutChangingExistingContextWireFormat(){
        var view=new CommanderNavalInvasion.View(UUID.randomUUID().toString(),9,"KOR","hoi:atlas_army_kor",List.of(plan("plan")));var snapshot=CommanderNavalInvasion.Snapshot.of(view);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);try{CommanderNavalInvasion.Snapshot.CODEC.encode(buffer,snapshot);assertEquals(view,CommanderNavalInvasion.Snapshot.CODEC.decode(buffer).view());assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
    }
    @Test void malformedControlsAndOversizedCollectionsFail(){
        assertThrows(IllegalArgumentException.class,()->new CommanderNavalInvasion.Point(Double.NaN,0));
        var valid=plan("plan");var json=new Gson().toJsonTree(new CommanderNavalInvasion.View(UUID.randomUUID().toString(),0,"KOR","hoi:atlas_army_kor",List.of(valid))).getAsJsonObject();
        json.getAsJsonArray("plans").get(0).getAsJsonObject().addProperty("phase","TRANSIT");assertThrows(RuntimeException.class,()->new CommanderNavalInvasion.Snapshot(json.toString()).view());
        assertThrows(IllegalArgumentException.class,()->new CommanderNavalInvasion.View(UUID.randomUUID().toString(),0,"KOR","x",List.of(valid,valid)));
        assertThrows(IllegalArgumentException.class,()->new CommanderNavalInvasion.View(UUID.randomUUID().toString(),0,"KOR","x",java.util.stream.IntStream.range(0,129).mapToObj(i->plan("plan/"+i)).toList()));
        assertThrows(IllegalArgumentException.class,()->new CommanderNavalInvasion.Snapshot("한".repeat(90000)));
    }
}
