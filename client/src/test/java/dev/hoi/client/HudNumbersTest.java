package dev.hoi.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudNumbersTest {
    @Test void monetaryAndRawIndicatorsKeepAtMostTwoDecimals() {
        assertEquals("123.46",HoiMenuBar.number(123.456789));
        assertEquals("704.31B",HoiMenuBar.money(704.314));
        assertEquals("1.71조",HoiMenuBar.money(1707.123456));
        assertEquals("9223372036854775807",HoiMenuBar.rawNumber(Long.MAX_VALUE));
        assertEquals("-2.35",HoiMenuBar.rawNumber(-2.345));
        assertEquals("0",HoiMenuBar.rawNumber(-0.00001));
    }
}
