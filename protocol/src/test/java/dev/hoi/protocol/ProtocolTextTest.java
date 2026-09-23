package dev.hoi.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolTextTest {
    @Test
    void preservesEmptyExactAndShortText() {
        assertEquals("", ProtocolText.clip("", 0));
        assertEquals("", ProtocolText.ellipsize("", 0));
        assertEquals("국가", ProtocolText.clip("국가", 2));
        assertEquals("국가", ProtocolText.ellipsize("국가", 2));
        assertEquals("국가", ProtocolText.ellipsize("국가", 20));
        assertEquals("", ProtocolText.clip("국가", 0));
        assertEquals("", ProtocolText.ellipsize("국가", 0));
        assertEquals("…", ProtocolText.ellipsize("국가", 1));
    }

    @Test
    void retainsExistingBmpClippingAndEllipsisBudgets() {
        assertEquals("국가", ProtocolText.clip("국가 정보", 2));
        assertEquals("국…", ProtocolText.ellipsize("국가 정보", 2));
        assertEquals("x".repeat(239) + "…", ProtocolText.ellipsize("x".repeat(20000), 240));
    }

    @Test
    void neverCutsBetweenTheSurrogatesOfASupplementaryCharacter() {
        String text = "가\uD83D\uDE80나";
        assertEquals("가", ProtocolText.clip(text, 2));
        assertEquals("가\uD83D\uDE80", ProtocolText.clip(text, 3));
        assertEquals("가…", ProtocolText.ellipsize(text, 3));
        assertEquals("가\uD83D\uDE80…", ProtocolText.ellipsize(text + "다", 4));
        assertEquals("", ProtocolText.clip("\uD83D\uDE80", 1));
        assertEquals("…", ProtocolText.ellipsize("\uD83D\uDE80x", 2));
    }

    @Test
    void rejectsInvalidBudgetsAndNullText() {
        assertThrows(IllegalArgumentException.class, () -> ProtocolText.clip("", -1));
        assertThrows(IllegalArgumentException.class, () -> ProtocolText.ellipsize("", -1));
        assertThrows(NullPointerException.class, () -> ProtocolText.clip(null, 1));
    }
}
