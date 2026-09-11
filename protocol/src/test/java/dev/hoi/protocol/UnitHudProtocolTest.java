package dev.hoi.protocol;

import dev.hoi.protocol.UnitHudProtocol.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UnitHudProtocolTest {
    private Counter row(String id,double ratio,Map<String,Integer> battalions) {
        return new Counter(id,"KOR","대한민국","infantry","보병 사단",2,-32,64,0,ratio,.5,.75,battalions);
    }
    @Test void invalidPrivateCountersAreRejectedBeforePublication() {
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-.1,1.001})
            assertThrows(IllegalArgumentException.class,()->row("a",invalid,Map.of()));
        assertThrows(IllegalArgumentException.class,()->row("a",1,Map.of("infantry",51)));
        assertThrows(IllegalArgumentException.class,()->row("a",1,Map.of("unknown",1)));
        assertThrows(IllegalArgumentException.class,()->row("a",1,Map.of("infantry",0)));
        var counter=row("a",1,Map.of("infantry",14));
        assertThrows(IllegalArgumentException.class,()->new Snapshot("bad dimension",List.of(counter)));
        assertThrows(IllegalArgumentException.class,()->new Snapshot("minecraft:overworld",List.of(counter,counter)));
        assertThrows(IllegalArgumentException.class,()->new Snapshot("minecraft:overworld",java.util.stream.IntStream.range(0,193).mapToObj(i->row("u"+i,1,Map.of())).toList()));
        var mutable=new HashMap<String,Integer>();mutable.put("infantry",14);
        var saved=row("a",1,mutable);mutable.clear();assertEquals(14,saved.battalions().get("infantry"));
        assertEquals(4096,saved.distanceSquared(-96,64,0));
    }
}
