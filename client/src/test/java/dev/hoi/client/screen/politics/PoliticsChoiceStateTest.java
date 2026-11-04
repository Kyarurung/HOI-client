package dev.hoi.client.screen.politics;

import dev.hoi.protocol.dialog.DialogView;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoliticsChoiceStateTest {
    @Test void onlyCandidateConditionsDrawACrossAndLockedPortraitsAreGrayscale() {
        for (String cost : java.util.List.of("165 정치력", "§c165 정치력", "적용 중")) {
            var unavailable = new DialogView.Tile("candidate", "후보", cost, "politics/vacant", "", null, "politics/choice/unavailable");
            var locked = new DialogView.Tile("candidate", "후보", cost, "politics/vacant", "", null, "politics/choice/locked");
            var ordinary = new DialogView.Tile("candidate", "후보", cost, "politics/vacant", "");
            assertTrue(PoliticsChoice.blocked(unavailable));
            assertTrue(PoliticsChoice.grayscale(unavailable));
            assertFalse(PoliticsChoice.blocked(locked));
            assertTrue(PoliticsChoice.grayscale(locked));
            assertFalse(PoliticsChoice.blocked(ordinary));
            assertFalse(PoliticsChoice.grayscale(ordinary));
        }
    }
}
