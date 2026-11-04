package dev.hoi.client.ui;

import dev.hoi.protocol.country.CountryDisplay;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiCountryDisplayTest {
    @Test void authoritativeReplacementAndDisconnectDiscardEarlierNames() {
        try {
            UiCountryDisplay.accept(new CountryDisplay.Table(List.of(new CountryDisplay.Entry("KOR", "서버 표시명", "KOR_empire"))));
            assertEquals("서버 표시명", UiCountryDisplay.name("KOR", "기존 스냅샷"));
            assertEquals("기존 스냅샷", UiCountryDisplay.name("JAP", "기존 스냅샷"));
            UiCountryDisplay.accept(new CountryDisplay.Table(List.of()));
            assertEquals("기존 스냅샷", UiCountryDisplay.name("KOR", "기존 스냅샷"));
            UiCountryDisplay.accept(new CountryDisplay.Table(List.of(new CountryDisplay.Entry("KOR", "새 표시명", "KOR"))));
            UiCountryDisplay.clear();
            assertEquals("재접속 스냅샷", UiCountryDisplay.name("KOR", "재접속 스냅샷"));
        } finally { UiCountryDisplay.clear(); }
    }
}
