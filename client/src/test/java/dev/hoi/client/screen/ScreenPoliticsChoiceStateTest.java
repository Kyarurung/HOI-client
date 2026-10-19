package dev.hoi.client.screen;

import dev.hoi.protocol.ProtocolDialogView;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenPoliticsChoiceStateTest {
    @Test void onlyCandidateConditionsDrawACrossAndLockedPortraitsAreGrayscale() {
        for (String cost : java.util.List.of("165 정치력", "§c165 정치력", "적용 중")) {
            var unavailable = new ProtocolDialogView.Tile("candidate", "후보", cost, "politics/vacant", "", null, "politics/choice/unavailable");
            var locked = new ProtocolDialogView.Tile("candidate", "후보", cost, "politics/vacant", "", null, "politics/choice/locked");
            var ordinary = new ProtocolDialogView.Tile("candidate", "후보", cost, "politics/vacant", "");
            assertTrue(ScreenPoliticsChoice.blocked(unavailable));
            assertTrue(ScreenPoliticsChoice.grayscale(unavailable));
            assertFalse(ScreenPoliticsChoice.blocked(locked));
            assertTrue(ScreenPoliticsChoice.grayscale(locked));
            assertFalse(ScreenPoliticsChoice.blocked(ordinary));
            assertFalse(ScreenPoliticsChoice.grayscale(ordinary));
        }
    }
}
