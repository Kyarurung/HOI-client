package dev.hoi.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiUnitsTest {
    @Test void gui2ReferencePreservesExistingCoordinates() {
        var units = new UiUnits(1280, 720);
        assertEquals(1, units.scale());
        assertEquals(33, units.size(33));
        assertEquals(-1, units.length(-1));
    }

    @Test void aspectRatiosFitWithoutStretching() {
        assertEquals(.75f, new UiUnits(960, 540).scale());
        assertEquals(.75f, new UiUnits(1280, 540).scale());
        assertEquals(.75f, new UiUnits(960, 720).scale());
        assertEquals(.421875f, new UiUnits(540, 960).scale());
        assertEquals(1.5f, new UiUnits(1920, 1080).scale());
    }

    @Test void roundedBoundsContainFractionalDrawing() {
        var units = new UiUnits(960, 540);
        assertEquals(24.75f, units.length(33));
        assertEquals(25, units.size(33));
        assertEquals(11, units.size(14));
        assertEquals(0, units.size(0));
    }

    @Test void guiScaleConversionKeepsPhysicalProportions() {
        assertEquals(new UiUnits(2560, 1440).length(33), new UiUnits(1280, 720).length(33) * 2);
        assertEquals(new UiUnits(1920, 1080).length(33), new UiUnits(640, 360).length(33) * 3);
        assertThrows(IllegalArgumentException.class, () -> new UiUnits(0, 720));
    }
}
