package dev.hoi.client.map.commander;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderMissionFrameTest {
    @Test void partialMissionAssignmentsUseTheOriginalWhiteTriangleFrame() {
        assertEquals("frame_0", CommanderPanels.missionFrame(false, 10, 10));
        assertEquals("frame_2", CommanderPanels.missionFrame(false, 3, 10));
        assertEquals("frame_1", CommanderPanels.missionFrame(false, 0, 10));
        assertEquals("frame_1", CommanderPanels.missionFrame(false, 0, 0));
        assertEquals("frame_0", CommanderPanels.missionFrame(true, 0, 10));
    }
}
