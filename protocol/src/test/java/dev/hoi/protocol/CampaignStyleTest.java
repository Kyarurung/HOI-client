package dev.hoi.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CampaignStyleTest {
    @Test void tenSpeedsUseFiveColorPairsAndStopHasItsOwnLabel() {
        int[] colors = {0x55FF55, 0xFFFF55, 0xFFAA00, 0xFF5555, 0xAA0000};
        for (int speed = 1; speed <= 10; speed++) {
            assertEquals(speed + "배속", CampaignStyle.speedName("X" + speed));
            assertEquals(colors[(speed - 1) / 2], CampaignStyle.speedColor("X" + speed));
        }
        assertEquals("일시 정지", CampaignStyle.speedName("PAUSED"));
        assertEquals(0xAAAAAA, CampaignStyle.speedColor("PAUSED"));
    }
}
