package dev.hoi.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiMenuBarTest {
    @Test void fuelTrendUsesOriginalIconsDaysColorsAndStockGauge() {
        var gson=new com.google.gson.Gson();
        var up=gson.fromJson("{\"fuel\":500,\"fuelRatio\":0.25,\"fuelDailyChange\":100,\"fuelDays\":15}",dev.hoi.protocol.ProtocolCountryHud.NationalIndicators.class);
        var down=gson.fromJson("{\"fuel\":500,\"fuelRatio\":0.25,\"fuelDailyChange\":-100,\"fuelDays\":5}",dev.hoi.protocol.ProtocolCountryHud.NationalIndicators.class);
        for(String change:new String[]{"100","0","-100","null"}) {
            var full=gson.fromJson("{\"fuel\":2000,\"fuelRatio\":1,\"fuelDailyChange\":"+change+",\"fuelDays\":0}",dev.hoi.protocol.ProtocolCountryHud.NationalIndicators.class);
            assertEquals("가득 참",UiMenuBar.fuelIndicator(full).value());
            assertEquals(0xFF009F03,UiMenuBar.indicatorColor(UiMenuBar.fuelIndicator(full)));
        }
        var rising=UiMenuBar.fuelIndicator(up);var falling=UiMenuBar.fuelIndicator(down);
        assertEquals("15일",rising.value());assertEquals("5일",falling.value());
        assertEquals("fuel_increasing",UiMenuBar.icon(rising));assertEquals("fuel_decreasing",UiMenuBar.icon(falling));
        assertEquals(0xFF009F03,UiMenuBar.indicatorColor(rising));assertEquals(0xFFFF3232,UiMenuBar.indicatorColor(falling));
        assertEquals(.25,rising.ratio());assertEquals(.25,falling.ratio());
        assertEquals("fuel",UiMenuBar.icon(UiMenuBar.fuelIndicator(dev.hoi.protocol.ProtocolCountryHud.NationalIndicators.UNKNOWN)));
    }
    @Test void suppliesUseTheAuthoritativeRatioAndFactoriesKeepFiveDigits() {
        assertEquals("0%", new UiMenuBar.UiIndicator("supplies", "보급", null, UiMenuBar.UiFormat.PERCENT).value());
        var full = new UiMenuBar.UiIndicator("supplies", "보급", 1.0, UiMenuBar.UiFormat.PERCENT);
        var shortfall = new UiMenuBar.UiIndicator("supplies", "보급", .99, UiMenuBar.UiFormat.PERCENT);
        assertEquals("100%", full.value());
        assertEquals(0xFFFFFFFF, UiMenuBar.indicatorColor(full));
        assertEquals("99%", shortfall.value());
        assertEquals(0xFFFF3232, UiMenuBar.indicatorColor(shortfall));
        for(double ratio:new double[]{0,.5,.99,.9999,1}) {
            var item=new UiMenuBar.UiIndicator("supplies","보급",ratio,UiMenuBar.UiFormat.PERCENT,"보급 효율",ratio);
            assertEquals(ratio<1?0xFFFF3232:0xFFFFFFFF,UiMenuBar.indicatorColor(item));
        }
        assertEquals("99%",new UiMenuBar.UiIndicator("supplies","보급",.9999,UiMenuBar.UiFormat.PERCENT).value());
        assertEquals("0%",new UiMenuBar.UiIndicator("supplies","보급",0,UiMenuBar.UiFormat.PERCENT).value());
        assertEquals("99999", new UiMenuBar.UiIndicator("factories", "공장", 99999, UiMenuBar.UiFormat.NUMBER).value());
    }
    @Test void signedRatiosAndLimitedCountersKeepFullDigits() {
        assertEquals("-100%", new UiMenuBar.UiIndicator("stability", "안정도", -1.0, UiMenuBar.UiFormat.PERCENT).value());
        assertEquals("100%", new UiMenuBar.UiIndicator("party", "정당 인기도", 1.0, UiMenuBar.UiFormat.PERCENT).value());
        assertEquals("1000", new UiMenuBar.UiIndicator("army_xp", "육군 경험치", 1000.0, UiMenuBar.UiFormat.EXPERIENCE).value());
        assertEquals("999", new UiMenuBar.UiIndicator("nuclear", "핵", 1200.0, UiMenuBar.UiFormat.NUCLEAR).value());
        assertEquals("0", new UiMenuBar.UiIndicator("nuclear", "핵", null, UiMenuBar.UiFormat.NUCLEAR).value());
    }
    @Test void politicalPowerTruncatesAtTheRequestedBoundaries() {
        assertEquals("0",UiMenuBar.politicalPower(null));
        assertEquals("-2000",UiMenuBar.politicalPower(-2000.0));
        assertEquals("0",UiMenuBar.politicalPower(-0.5));
        assertEquals("73",UiMenuBar.politicalPower(73.99));
        assertEquals("999",UiMenuBar.politicalPower(999.99));
        assertEquals("1000",UiMenuBar.politicalPower(1000.0));
        assertEquals("1099",UiMenuBar.politicalPower(1099.99));
        assertEquals("1350",UiMenuBar.politicalPower(1350.0));
        assertEquals("1999",UiMenuBar.politicalPower(1999.99));
        assertEquals("2000",UiMenuBar.politicalPower(2000.0));
    }
    @Test void commandPowerDropsDecimalsAndMissingIndicatorsDisplayZero() {
        assertEquals("0",new UiMenuBar.UiIndicator("command_power","지휘력",null,UiMenuBar.UiFormat.COMMAND_POWER).value());
        assertEquals("0",new UiMenuBar.UiIndicator("command_power","지휘력",0.99,UiMenuBar.UiFormat.COMMAND_POWER).value());
        assertEquals("150",new UiMenuBar.UiIndicator("command_power","지휘력",150.75,UiMenuBar.UiFormat.COMMAND_POWER).value());
        assertEquals("999",new UiMenuBar.UiIndicator("command_power","지휘력",999.99,UiMenuBar.UiFormat.COMMAND_POWER).value());
    }
    @Test void unknownCountryUsesNoDefaultNationalFlag() {
        assertEquals("",UiMenuBar.flagTexture(""));
        assertEquals("",UiMenuBar.flagTexture(null));
        assertEquals("country/kor/flag",UiMenuBar.flagTexture("KOR"));
    }
    @Test void moneyUsesTrillionsWithAtMostThreeDecimalsAndRawNumbersKeepTwo() {
        assertEquals("123.46",UiMenuBar.number(123.456789));
        assertEquals("0.704조",UiMenuBar.money(704.314));
        assertEquals("1.707조",UiMenuBar.money(1707.123456));
        assertEquals("10조",UiMenuBar.money(10000.0));
        assertEquals("0조",UiMenuBar.money(0.0));
        assertEquals("0",UiMenuBar.money(null));
        assertEquals("9223372036854775807",UiMenuBar.rawNumber(Long.MAX_VALUE));
        assertEquals("-2.35",UiMenuBar.rawNumber(-2.345));
        assertEquals("0",UiMenuBar.rawNumber(-0.00001));
        assertEquals("11.6K",UiMenuBar.number(11600.0));
        assertEquals("281.2K",UiMenuBar.number(281200.0));
        assertEquals("21.86",UiMenuBar.rawNumber(21.862500000000004));
    }
}
