package dev.hoi.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudNumbersTest {
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
