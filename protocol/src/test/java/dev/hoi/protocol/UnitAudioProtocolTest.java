package dev.hoi.protocol;

import dev.hoi.protocol.UnitAudioProtocol.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UnitAudioProtocolTest {
    @Test void boundsRejectInvalidPositionsDuplicatesAndExcessVoices() {
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,30_000_001})
            assertThrows(IllegalArgumentException.class,()->new Emitter("a",Sound.FOOT,invalid,0,0));
        var emitter=new Emitter("a",Sound.RIFLE,0,64,0);
        assertThrows(IllegalArgumentException.class,()->new Snapshot("minecraft:overworld",List.of(emitter,emitter)));
        assertThrows(IllegalArgumentException.class,()->new Snapshot("minecraft:overworld",java.util.stream.IntStream.range(0,4).mapToObj(i->new Emitter("u"+i,Sound.FOOT,0,64,0)).toList()));
        assertThrows(IllegalArgumentException.class,()->new Snapshot("bad dimension",List.of()));
        assertEquals(49,emitter.distanceSquared(7,64,0));
    }
}
