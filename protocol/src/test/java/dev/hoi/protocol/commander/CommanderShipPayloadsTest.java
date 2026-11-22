package dev.hoi.protocol.commander;

import dev.hoi.protocol.commander.CommanderShipPayloads.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderShipPayloadsTest {
    private static final String TOKEN=UUID.randomUUID().toString();
    private static Row row(String id,int level,double strength){return new Row(id,"ROKS Sejong","구축함","unit_hud/navy_heavy_cruiser",level,.5,strength,1,"내구도","조직력");}
    @Test void requestsKeepTheirTargetAndRejectMalformedInput(){
        for(var action:Action.values()){
            var request=new Request(TOKEN,action,"hoi:ship/KOR_12");var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
            try{Request.CODEC.encode(buffer,request);assertEquals(request,Request.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
        }
        for(String target:List.of("","bad id","../x y","한글"))assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,Action.OPEN,target));
        assertThrows(IllegalArgumentException.class,()->new Request("not-a-token",Action.OPEN,"ship"));
        assertThrows(NullPointerException.class,()->new Request(TOKEN,null,"ship"));
    }
    @Test void forceListsRoundTripAndStayBounded(){
        var force=new Force(TOKEN,"force-1",List.of(row("ship-1",4,1),row("ship-2",0,0)),Map.of("SELECT","선택"),"");
        assertEquals(force,ForceSnapshot.of(force).force());assertEquals("선택",force.label("SELECT"));assertEquals("",force.label("BACK"));
        assertThrows(IllegalArgumentException.class,()->row("ship-1",5,1));
        assertThrows(IllegalArgumentException.class,()->row("ship-1",0,1.01));
        assertThrows(IllegalArgumentException.class,()->row("ship-1",0,Double.NaN));
        var many=new ArrayList<Row>();for(int i=0;i<=CommanderShipPayloads.ROWS;i++)many.add(row("ship-"+i,0,1));
        assertThrows(IllegalArgumentException.class,()->new Force(TOKEN,"force-1",many,Map.of(),""));
        assertThrows(IllegalArgumentException.class,()->new ForceSnapshot("x".repeat(200001)));
        assertThrows(IllegalArgumentException.class,()->new Snapshot("x".repeat(65537)));
    }
}
