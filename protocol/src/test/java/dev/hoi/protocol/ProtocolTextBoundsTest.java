package dev.hoi.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolTextBoundsTest {
    @Test
    void preservesEmptyExactAndShortText() {
        assertEquals("", ProtocolTextBounds.clip("", 0));
        assertEquals("", ProtocolTextBounds.ellipsize("", 0));
        assertEquals("국가", ProtocolTextBounds.clip("국가", 2));
        assertEquals("국가", ProtocolTextBounds.ellipsize("국가", 2));
        assertEquals("국가", ProtocolTextBounds.ellipsize("국가", 20));
        assertEquals("", ProtocolTextBounds.clip("국가", 0));
        assertEquals("", ProtocolTextBounds.ellipsize("국가", 0));
        assertEquals("…", ProtocolTextBounds.ellipsize("국가", 1));
    }
    @Test
    void retainsExistingBmpClippingAndEllipsisBudgets() {
        assertEquals("국가", ProtocolTextBounds.clip("국가 정보", 2));
        assertEquals("국…", ProtocolTextBounds.ellipsize("국가 정보", 2));
        assertEquals("x".repeat(239) + "…", ProtocolTextBounds.ellipsize("x".repeat(20000), 240));
    }
    @Test
    void neverCutsBetweenTheSurrogatesOfASupplementaryCharacter() {
        String text = "가\uD83D\uDE80나";
        assertEquals("가", ProtocolTextBounds.clip(text, 2));
        assertEquals("가\uD83D\uDE80", ProtocolTextBounds.clip(text, 3));
        assertEquals("가…", ProtocolTextBounds.ellipsize(text, 3));
        assertEquals("가\uD83D\uDE80…", ProtocolTextBounds.ellipsize(text + "다", 4));
        assertEquals("", ProtocolTextBounds.clip("\uD83D\uDE80", 1));
        assertEquals("…", ProtocolTextBounds.ellipsize("\uD83D\uDE80x", 2));
    }
    @Test
    void rejectsInvalidBudgetsAndNullText() {
        assertThrows(IllegalArgumentException.class, () -> ProtocolTextBounds.clip("", -1));
        assertThrows(IllegalArgumentException.class, () -> ProtocolTextBounds.ellipsize("", -1));
        assertThrows(NullPointerException.class, () -> ProtocolTextBounds.clip(null, 1));
    }
}
