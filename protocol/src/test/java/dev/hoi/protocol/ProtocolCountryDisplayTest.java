package dev.hoi.protocol;

import dev.hoi.protocol.ProtocolCountryDisplay.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCountryDisplayTest {
    @Test void tableCarriesServerChosenNamesAndFlagKeys() {
        var table=new Table(List.of(new Entry("KOR","고려민주연방공화국","KOR_federation"),new Entry("PRK","조선민주주의인민공화국","PRK")));
        assertEquals("KOR_federation",table.country("KOR").orElseThrow().flag());
        assertTrue(table.country("JAP").isEmpty());
        assertThrows(IllegalArgumentException.class,()->new Table(List.of(new Entry("KOR","a","KOR"),new Entry("KOR","b","KOR"))));
        for(String flag:new String[]{"","bad flag","x".repeat(129)})assertThrows(IllegalArgumentException.class,()->new Entry("KOR","a",flag));
        assertThrows(IllegalArgumentException.class,()->new Entry("kor","a","KOR"));
        assertThrows(IllegalArgumentException.class,()->new Entry("KOR"," ","KOR"));
    }
}
