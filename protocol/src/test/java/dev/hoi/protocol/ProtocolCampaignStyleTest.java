package dev.hoi.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCampaignStyleTest {
    @Test void twentySpeedsUseFiveColorGroupsAndStopHasItsOwnLabel() {
        int[] colors = {0x55FF55, 0xFFFF55, 0xFFAA00, 0xFF5555, 0xAA0000};
        for (int speed = 1; speed <= 20; speed++) {
            assertEquals(speed + "배속", ProtocolCampaignStyle.speedName("X" + speed));
            assertEquals(colors[(speed - 1) / 4], ProtocolCampaignStyle.speedColor("X" + speed));
        }
        assertEquals("일시 정지", ProtocolCampaignStyle.speedName("PAUSED"));
        assertEquals(0xAAAAAA, ProtocolCampaignStyle.speedColor("PAUSED"));
    }
}
