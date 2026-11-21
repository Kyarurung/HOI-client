package dev.hoi.client.ui.hud;

import dev.hoi.protocol.hud.HudDetail;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudTooltipTest {
    @Test void sourceTextThatAlreadyStartsWithTheTitleIsNotTitledTwice() {
        var tension = new HudDetail(List.of(), "§H세계 긴장도\n§! 현재 값: §e12.0%§!");
        assertTrue(HudTooltip.sourceTitled("world_tension", "세계 긴장도", tension));
        assertFalse(HudTooltip.sourceTitled("world_tension", "세계 긴장도", new HudDetail(List.of(), "현재 값: 12.0%")));
        assertFalse(HudTooltip.sourceTitled("world_tension", "세계 긴장도", new HudDetail(List.of())));
        assertFalse(HudTooltip.sourceTitled("world_tension", "세계 긴장도", null));
        assertTrue(HudTooltip.sourceTitled("gdp", "실질 GDP (십억 달러)", new HudDetail(List.of(), "본문")));
    }
}
