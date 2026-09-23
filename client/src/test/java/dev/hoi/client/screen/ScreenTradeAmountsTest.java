package dev.hoi.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenTradeAmountsTest {
    @Test void keepsWholeFactoryStepsAndThePartialLastImport() {
        for (int export = 1; export <= 128; export++) {
            int maximum = (export + 7) / 8;
            for (int factories = 0; factories <= maximum; factories++) {
                double position = ScreenTradeAmounts.tradeSliderPosition(factories, export, 8);
                assertEquals(Math.min(export, factories * 8) / (double)export, position);
                assertEquals(factories, ScreenTradeAmounts.tradeSliderFactories(position, export, 8));
            }
        }
        assertEquals(0, ScreenTradeAmounts.tradeSliderFactories(-1, 41, 8));
        assertEquals(6, ScreenTradeAmounts.tradeSliderFactories(2, 41, 8));
        assertEquals(0, ScreenTradeAmounts.tradeSliderPosition(0, 0, 8));
    }
    @Test void replacesCurrentImportsAndRespectsFactoryLimit() {
        assertEquals(2, ScreenTradeAmounts.neededTradeFactories(20, 24, 16, 8, 10));
        assertEquals(0, ScreenTradeAmounts.neededTradeFactories(20, 40, 16, 8, 10));
        assertEquals(3, ScreenTradeAmounts.neededTradeFactories(21, 4, 0, 8, 10));
        assertEquals(2, ScreenTradeAmounts.neededTradeFactories(100, 0, 0, 8, 2));
    }
    @Test void distinguishesLandSeaAndUnknownRoutes() {
        assertEquals("—", ScreenTradeAmounts.convoyCell(null));
        assertEquals("—", ScreenTradeAmounts.convoyCell(-1));
        assertEquals("-", ScreenTradeAmounts.convoyCell(0));
        assertEquals("9", ScreenTradeAmounts.convoyCell(9));
    }
}
