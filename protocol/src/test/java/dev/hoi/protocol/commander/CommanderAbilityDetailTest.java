package dev.hoi.protocol.commander;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderAbilityDetailTest {
    @Test void sourceAbilityIdsKeepTheirCountryPrefixCase() {
        for (String id : new String[]{"force_attack", "PRC_force_attack", "GER_force_attack"})
            assertEquals(id, new CommanderHudPayloads.AbilityDetail(id, true, "").id());
        for (String id : new String[]{"", "bad-id", "PRC force", "a".repeat(65)})
            assertThrows(IllegalArgumentException.class, () -> new CommanderHudPayloads.AbilityDetail(id, true, ""));
    }
}
