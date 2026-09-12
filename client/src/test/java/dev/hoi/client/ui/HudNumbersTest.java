package dev.hoi.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudNumbersTest {
    @Test void suppliesUseTheAuthoritativeRatioAndFactoriesKeepFiveDigits() {
        assertEquals("0", new HoiMenuBar.Indicator("supplies", "보급", null, HoiMenuBar.Format.PERCENT).value());
        var full = new HoiMenuBar.Indicator("supplies", "보급", 1.0, HoiMenuBar.Format.PERCENT);
        var shortfall = new HoiMenuBar.Indicator("supplies", "보급", .99, HoiMenuBar.Format.PERCENT);
        assertEquals("100%", full.value());
        assertEquals(0xFFFFFFFF, HoiMenuBar.indicatorColor(full));
        assertEquals("99%", shortfall.value());
        assertEquals(0xFFAA0000, HoiMenuBar.indicatorColor(shortfall));
        assertEquals("99999", new HoiMenuBar.Indicator("factories", "공장", 99999, HoiMenuBar.Format.NUMBER).value());
    }

    @Test void signedRatiosAndLimitedCountersKeepFullDigits() {
        assertEquals("-100%", new HoiMenuBar.Indicator("stability", "안정도", -1.0, HoiMenuBar.Format.PERCENT).value());
        assertEquals("100%", new HoiMenuBar.Indicator("party", "정당 인기도", 1.0, HoiMenuBar.Format.PERCENT).value());
        assertEquals("1000", new HoiMenuBar.Indicator("army_xp", "육군 경험치", 1000.0, HoiMenuBar.Format.EXPERIENCE).value());
        assertEquals("999", new HoiMenuBar.Indicator("nuclear", "핵", 1200.0, HoiMenuBar.Format.NUCLEAR).value());
        assertEquals("0", new HoiMenuBar.Indicator("nuclear", "핵", null, HoiMenuBar.Format.NUCLEAR).value());
    }
    @Test void politicalPowerTruncatesAtTheRequestedBoundaries() {
        assertEquals("—",HoiMenuBar.politicalPower(null));
        assertEquals("-2000",HoiMenuBar.politicalPower(-2000.0));
        assertEquals("0",HoiMenuBar.politicalPower(-0.5));
        assertEquals("73",HoiMenuBar.politicalPower(73.99));
        assertEquals("999",HoiMenuBar.politicalPower(999.99));
        assertEquals("1000",HoiMenuBar.politicalPower(1000.0));
        assertEquals("1099",HoiMenuBar.politicalPower(1099.99));
        assertEquals("1350",HoiMenuBar.politicalPower(1350.0));
        assertEquals("1999",HoiMenuBar.politicalPower(1999.99));
        assertEquals("2000",HoiMenuBar.politicalPower(2000.0));
    }

    @Test void commandPowerDropsDecimalsAndMissingIndicatorsDisplayZero() {
        assertEquals("0",new HoiMenuBar.Indicator("command_power","지휘력",null,HoiMenuBar.Format.COMMAND_POWER).value());
        assertEquals("0",new HoiMenuBar.Indicator("command_power","지휘력",0.99,HoiMenuBar.Format.COMMAND_POWER).value());
        assertEquals("150",new HoiMenuBar.Indicator("command_power","지휘력",150.75,HoiMenuBar.Format.COMMAND_POWER).value());
        assertEquals("999",new HoiMenuBar.Indicator("command_power","지휘력",999.99,HoiMenuBar.Format.COMMAND_POWER).value());
    }

    @Test void unknownCountryUsesNoDefaultNationalFlag() {
        assertEquals("",HoiMenuBar.flagTexture(""));
        assertEquals("",HoiMenuBar.flagTexture(null));
        assertEquals("country/kor/flag",HoiMenuBar.flagTexture("KOR"));
    }

    @Test void moneyUsesTrillionsWithAtMostThreeDecimalsAndRawNumbersKeepTwo() {
        assertEquals("123.46",HoiMenuBar.number(123.456789));
        assertEquals("0.704조",HoiMenuBar.money(704.314));
        assertEquals("1.707조",HoiMenuBar.money(1707.123456));
        assertEquals("10조",HoiMenuBar.money(10000.0));
        assertEquals("0조",HoiMenuBar.money(0.0));
        assertEquals("—",HoiMenuBar.money(null));
        assertEquals("9223372036854775807",HoiMenuBar.rawNumber(Long.MAX_VALUE));
        assertEquals("-2.35",HoiMenuBar.rawNumber(-2.345));
        assertEquals("0",HoiMenuBar.rawNumber(-0.00001));
        assertEquals("11.6K",HoiMenuBar.number(11600.0));
        assertEquals("281.2K",HoiMenuBar.number(281200.0));
        assertEquals("21.86",HoiMenuBar.rawNumber(21.862500000000004));
    }
}
