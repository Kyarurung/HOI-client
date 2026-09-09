package dev.hoi.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudNumbersTest {
    @Test void politicalPowerTruncatesAtTheRequestedBoundaries() {
        assertEquals("—",HoiMenuBar.politicalPower(null));
        assertEquals("-500",HoiMenuBar.politicalPower(-500.0));
        assertEquals("0",HoiMenuBar.politicalPower(-0.5));
        assertEquals("73",HoiMenuBar.politicalPower(73.99));
        assertEquals("999",HoiMenuBar.politicalPower(999.99));
        assertEquals("1.0K",HoiMenuBar.politicalPower(1000.0));
        assertEquals("1.0K",HoiMenuBar.politicalPower(1099.99));
        assertEquals("1.3K",HoiMenuBar.politicalPower(1350.0));
        assertEquals("1.9K",HoiMenuBar.politicalPower(1999.99));
        assertEquals("2.0K",HoiMenuBar.politicalPower(2000.0));
    }

    @Test void commandPowerDropsDecimalsWithoutRoundingOrInventingUnknownValues() {
        assertEquals("—",new HoiMenuBar.Indicator("command_power","지휘력",null,HoiMenuBar.Format.COMMAND_POWER).value());
        assertEquals("0",new HoiMenuBar.Indicator("command_power","지휘력",0.99,HoiMenuBar.Format.COMMAND_POWER).value());
        assertEquals("150",new HoiMenuBar.Indicator("command_power","지휘력",150.75,HoiMenuBar.Format.COMMAND_POWER).value());
        assertEquals("999",new HoiMenuBar.Indicator("command_power","지휘력",999.99,HoiMenuBar.Format.COMMAND_POWER).value());
    }

    @Test void unknownCountryUsesNoDefaultNationalFlag() {
        assertEquals("",HoiMenuBar.flagTexture(""));
        assertEquals("",HoiMenuBar.flagTexture(null));
        assertEquals("country/kor/flag",HoiMenuBar.flagTexture("KOR"));
    }

    @Test void monetaryAndRawIndicatorsKeepAtMostTwoDecimals() {
        assertEquals("123.46",HoiMenuBar.number(123.456789));
        assertEquals("704.31B",HoiMenuBar.money(704.314));
        assertEquals("1.71조",HoiMenuBar.money(1707.123456));
        assertEquals("9223372036854775807",HoiMenuBar.rawNumber(Long.MAX_VALUE));
        assertEquals("-2.35",HoiMenuBar.rawNumber(-2.345));
        assertEquals("0",HoiMenuBar.rawNumber(-0.00001));
        assertEquals("11.6K",HoiMenuBar.number(11600.0));
        assertEquals("281.2K",HoiMenuBar.number(281200.0));
        assertEquals("21.86",HoiMenuBar.rawNumber(21.862500000000004));
    }
}
