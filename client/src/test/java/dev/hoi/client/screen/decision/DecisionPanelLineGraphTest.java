package dev.hoi.client.screen.decision;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecisionPanelLineGraphTest {
    @Test void halfStateDrawsTheSourceLineThroughTheMiddleOfEveryColumn() {
        int[] runs = DecisionPanel.lineRuns(20, 200, .5f);
        assertEquals(20 * 3, runs.length);
        for (int index = 0; index < runs.length; index += 3) {
            assertEquals(index / 3, runs[index]);
            assertEquals(98, runs[index + 1]);
            assertEquals(102, runs[index + 2]);
        }
    }
    @Test void higherStateRaisesTheRightEndFromTheLeftMiddle() {
        int[] runs = DecisionPanel.lineRuns(10, 200, 1f);
        assertArrayEquals(new int[]{0, 85, 101}, java.util.Arrays.copyOfRange(runs, 0, 3));
        assertArrayEquals(new int[]{9, 0, 11}, java.util.Arrays.copyOfRange(runs, runs.length - 3, runs.length));
        int[] falling = DecisionPanel.lineRuns(10, 200, 0f);
        assertArrayEquals(new int[]{9, 189, 200}, java.util.Arrays.copyOfRange(falling, falling.length - 3, falling.length));
    }
}
