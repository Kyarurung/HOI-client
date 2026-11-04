package dev.hoi.client.map.air;

import dev.hoi.protocol.commander.CommanderHudPayloads.AirRange;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirRangeOverlayTest {
    @Test void sameCenterAndRadiusDeduplicateWithoutMergingDifferentRangesOrBases(){
        var circles=AirRangeOverlay.circles(List.of(new AirRange(650,12,-4,22),new AirRange(651,12,-4,22),new AirRange(650,13,-4,22),new AirRange(850,12,-4,30)));
        assertEquals(List.of(new AirRangeOverlay.Circle(12,-4,22),new AirRangeOverlay.Circle(13,-4,22),new AirRangeOverlay.Circle(12,-4,30)),circles);
        assertTrue(AirRangeOverlay.circles(List.of()).isEmpty());
    }
}
