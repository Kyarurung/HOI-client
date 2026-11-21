package dev.hoi.client.ui.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudMenuBarTest {
    @Test void fuelTrendUsesOriginalIconsDaysColorsAndStockGauge() {
        var gson=new com.google.gson.Gson();
        var up=gson.fromJson("{\"fuel\":500,\"fuelRatio\":0.25,\"fuelDailyChange\":100,\"fuelDays\":15}",dev.hoi.protocol.country.CountryHud.NationalIndicators.class);
        var down=gson.fromJson("{\"fuel\":500,\"fuelRatio\":0.25,\"fuelDailyChange\":-100,\"fuelDays\":5}",dev.hoi.protocol.country.CountryHud.NationalIndicators.class);
        for(String change:new String[]{"100","0","-100","null"}) {
            var full=gson.fromJson("{\"fuel\":2000,\"fuelRatio\":1,\"fuelDailyChange\":"+change+",\"fuelDays\":0}",dev.hoi.protocol.country.CountryHud.NationalIndicators.class);
            assertEquals("가득 참",HudMenuBar.fuelIndicator(full).value());
            assertEquals(0xFF009F03,HudMenuBar.indicatorColor(HudMenuBar.fuelIndicator(full)));
        }
        var rising=HudMenuBar.fuelIndicator(up);var falling=HudMenuBar.fuelIndicator(down);
        assertEquals("15일",rising.value());assertEquals("5일",falling.value());
        assertEquals("fuel_increasing",HudMenuBar.icon(rising));assertEquals("fuel_decreasing",HudMenuBar.icon(falling));
        assertEquals(0xFF009F03,HudMenuBar.indicatorColor(rising));assertEquals(0xFFFF3232,HudMenuBar.indicatorColor(falling));
        assertEquals(.25,rising.ratio());assertEquals(.25,falling.ratio());
        assertEquals("fuel",HudMenuBar.icon(HudMenuBar.fuelIndicator(dev.hoi.protocol.country.CountryHud.NationalIndicators.UNKNOWN)));
    }
    @Test void suppliesUseTheAuthoritativeRatioAndFactoriesKeepFiveDigits() {
        assertEquals("0%", new HudMenuBar.HudIndicator("supplies", "보급", null, HudMenuBar.HudFormat.PERCENT).value());
        var full = new HudMenuBar.HudIndicator("supplies", "보급", 1.0, HudMenuBar.HudFormat.PERCENT);
        var shortfall = new HudMenuBar.HudIndicator("supplies", "보급", .99, HudMenuBar.HudFormat.PERCENT);
        assertEquals("100%", full.value());
        assertEquals(0xFFFFFFFF, HudMenuBar.indicatorColor(full));
        assertEquals("99%", shortfall.value());
        assertEquals(0xFFFF3232, HudMenuBar.indicatorColor(shortfall));
        for(double ratio:new double[]{0,.5,.99,.9999,1}) {
            var item=new HudMenuBar.HudIndicator("supplies","보급",ratio,HudMenuBar.HudFormat.PERCENT,"보급 효율",ratio);
            assertEquals(ratio<1?0xFFFF3232:0xFFFFFFFF,HudMenuBar.indicatorColor(item));
        }
        assertEquals("99%",new HudMenuBar.HudIndicator("supplies","보급",.9999,HudMenuBar.HudFormat.PERCENT).value());
        assertEquals("0%",new HudMenuBar.HudIndicator("supplies","보급",0,HudMenuBar.HudFormat.PERCENT).value());
        assertEquals("99999", new HudMenuBar.HudIndicator("factories", "공장", 99999, HudMenuBar.HudFormat.NUMBER).value());
    }
    @Test void signedRatiosAndLimitedCountersKeepFullDigits() {
        assertEquals("-100%", new HudMenuBar.HudIndicator("stability", "안정도", -1.0, HudMenuBar.HudFormat.PERCENT).value());
        assertEquals("100%", new HudMenuBar.HudIndicator("party", "정당 인기도", 1.0, HudMenuBar.HudFormat.PERCENT).value());
        assertEquals("1000", new HudMenuBar.HudIndicator("army_xp", "육군 경험치", 1000.0, HudMenuBar.HudFormat.EXPERIENCE).value());
        assertEquals("999", new HudMenuBar.HudIndicator("nuclear", "핵", 1200.0, HudMenuBar.HudFormat.NUCLEAR).value());
        assertEquals("0", new HudMenuBar.HudIndicator("nuclear", "핵", null, HudMenuBar.HudFormat.NUCLEAR).value());
    }
    @Test void politicalPowerTruncatesAtTheRequestedBoundaries() {
        assertEquals("0",HudMenuBar.politicalPower(null));
        assertEquals("-2000",HudMenuBar.politicalPower(-2000.0));
        assertEquals("0",HudMenuBar.politicalPower(-0.5));
        assertEquals("73",HudMenuBar.politicalPower(73.99));
        assertEquals("999",HudMenuBar.politicalPower(999.99));
        assertEquals("1000",HudMenuBar.politicalPower(1000.0));
        assertEquals("1099",HudMenuBar.politicalPower(1099.99));
        assertEquals("1350",HudMenuBar.politicalPower(1350.0));
        assertEquals("1999",HudMenuBar.politicalPower(1999.99));
        assertEquals("2000",HudMenuBar.politicalPower(2000.0));
    }
    @Test void commandPowerDropsDecimalsAndMissingIndicatorsDisplayZero() {
        assertEquals("0",new HudMenuBar.HudIndicator("command_power","지휘력",null,HudMenuBar.HudFormat.COMMAND_POWER).value());
        assertEquals("0",new HudMenuBar.HudIndicator("command_power","지휘력",0.99,HudMenuBar.HudFormat.COMMAND_POWER).value());
        assertEquals("150",new HudMenuBar.HudIndicator("command_power","지휘력",150.75,HudMenuBar.HudFormat.COMMAND_POWER).value());
        assertEquals("999",new HudMenuBar.HudIndicator("command_power","지휘력",999.99,HudMenuBar.HudFormat.COMMAND_POWER).value());
    }
    @Test void unknownCountryUsesNoDefaultNationalFlag() {
        assertEquals("",HudMenuBar.flagTexture(""));
        assertEquals("",HudMenuBar.flagTexture(null));
        assertEquals("country/kor/flag",HudMenuBar.flagTexture("KOR"));
    }
    @Test void moneyUsesTrillionsWithAtMostThreeDecimalsAndRawNumbersKeepTwo() {
        assertEquals("123.46",HudMenuBar.number(123.456789));
        assertEquals("7,043억",HudMenuBar.money(704.314));
        assertEquals("1.707조",HudMenuBar.money(1707.123456));
        assertEquals("10조",HudMenuBar.money(10000.0));
        assertEquals("0억",HudMenuBar.money(0.0));
        assertEquals("1조",HudMenuBar.money(999.96));
        assertEquals("9,999억",HudMenuBar.money(999.94));
        assertEquals("15억",HudMenuBar.money(1.5));
        assertEquals("0",HudMenuBar.money(null));
        assertEquals("9223372036854775807",HudMenuBar.rawNumber(Long.MAX_VALUE));
        assertEquals("-2.35",HudMenuBar.rawNumber(-2.345));
        assertEquals("0",HudMenuBar.rawNumber(-0.00001));
        assertEquals("11.6K",HudMenuBar.number(11600.0));
        assertEquals("281.2K",HudMenuBar.number(281200.0));
        assertEquals("21.86",HudMenuBar.rawNumber(21.862500000000004));
    }
}
