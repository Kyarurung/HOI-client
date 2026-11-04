package dev.hoi.client.screen.industry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryTradeAmountsTest {
    @Test void keepsWholeFactoryStepsAndThePartialLastImport() {
        for (int export = 1; export <= 128; export++) {
            int maximum = (export + 7) / 8;
            for (int factories = 0; factories <= maximum; factories++) {
                double position = IndustryTradeAmounts.tradeSliderPosition(factories, export, 8);
                assertEquals(Math.min(export, factories * 8) / (double)export, position);
                assertEquals(factories, IndustryTradeAmounts.tradeSliderFactories(position, export, 8));
            }
        }
        assertEquals(0, IndustryTradeAmounts.tradeSliderFactories(-1, 41, 8));
        assertEquals(6, IndustryTradeAmounts.tradeSliderFactories(2, 41, 8));
        assertEquals(0, IndustryTradeAmounts.tradeSliderPosition(0, 0, 8));
    }
    @Test void replacesCurrentImportsAndRespectsFactoryLimit() {
        assertEquals(2, IndustryTradeAmounts.neededTradeFactories(20, 24, 16, 8, 10));
        assertEquals(0, IndustryTradeAmounts.neededTradeFactories(20, 40, 16, 8, 10));
        assertEquals(3, IndustryTradeAmounts.neededTradeFactories(21, 4, 0, 8, 10));
        assertEquals(2, IndustryTradeAmounts.neededTradeFactories(100, 0, 0, 8, 2));
    }
    @Test void distinguishesLandSeaAndUnknownRoutes() {
        assertEquals("—", IndustryTradeAmounts.convoyCell(null));
        assertEquals("—", IndustryTradeAmounts.convoyCell(-1));
        assertEquals("-", IndustryTradeAmounts.convoyCell(0));
        assertEquals("9", IndustryTradeAmounts.convoyCell(9));
    }
}
