package dev.hoi.protocol;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolNumberBoundsTest {
    @Test
    @ResourceLock("java.util.Locale.default")
    void displayUsesStableDecimalPointsAcrossLocales() {
        Locale original = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            assertEquals("1234.50", ProtocolNumberBounds.fixed(1234.5, 2));
            assertEquals("12.5%", ProtocolNumberBounds.percent(0.125, 1));
            assertEquals("+12.50%", ProtocolNumberBounds.signedPercent(0.125, 2));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original);
        }
    }

    @Test
    void preservesRoundingSignsAndLargeValuesWithoutClamping() {
        assertEquals("0", ProtocolNumberBounds.fixed(0, 0));
        assertEquals("-0.00", ProtocolNumberBounds.fixed(-0.0, 2));
        assertEquals("1.13", ProtocolNumberBounds.fixed(1.125, 2));
        assertEquals("-1.13", ProtocolNumberBounds.fixed(-1.125, 2));
        assertEquals("1000000000000.00", ProtocolNumberBounds.fixed(1e12, 2));
        assertEquals("125.0%", ProtocolNumberBounds.percent(1.25, 1));
        assertEquals("-12.50%", ProtocolNumberBounds.signedPercent(-0.125, 2));
        assertEquals("+0.00%", ProtocolNumberBounds.signedPercent(0, 2));
        assertEquals("-0.00%", ProtocolNumberBounds.signedPercent(-0.0, 2));
        assertEquals("NaN", ProtocolNumberBounds.fixed(Double.NaN, 1));
        assertEquals("Infinity", ProtocolNumberBounds.fixed(Double.POSITIVE_INFINITY, 1));
    }

    @Test
    void rejectsUnboundedPrecision() {
        assertThrows(IllegalArgumentException.class, () -> ProtocolNumberBounds.fixed(1, -1));
        assertThrows(IllegalArgumentException.class, () -> ProtocolNumberBounds.fixed(1, 10));
        assertThrows(IllegalArgumentException.class, () -> ProtocolNumberBounds.percent(1, Integer.MAX_VALUE));
    }
}
