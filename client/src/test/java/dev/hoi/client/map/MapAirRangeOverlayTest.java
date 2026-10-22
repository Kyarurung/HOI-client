package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolCommanderHud.AirRange;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapAirRangeOverlayTest {
    @Test void sameCenterAndRadiusDeduplicateWithoutMergingDifferentRangesOrBases(){
        var circles=MapAirRangeOverlay.circles(List.of(new AirRange(650,12,-4,22),new AirRange(651,12,-4,22),new AirRange(650,13,-4,22),new AirRange(850,12,-4,30)));
        assertEquals(List.of(new MapAirRangeOverlay.Circle(12,-4,22),new MapAirRangeOverlay.Circle(13,-4,22),new MapAirRangeOverlay.Circle(12,-4,30)),circles);
        assertTrue(MapAirRangeOverlay.circles(List.of()).isEmpty());
    }
}
