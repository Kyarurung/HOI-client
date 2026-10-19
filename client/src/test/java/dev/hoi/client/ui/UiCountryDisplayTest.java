package dev.hoi.client.ui;

import dev.hoi.protocol.ProtocolCountryDisplay;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiCountryDisplayTest {
    @Test void authoritativeReplacementAndDisconnectDiscardEarlierNames() {
        try {
            UiCountryDisplay.accept(new ProtocolCountryDisplay.Table(List.of(new ProtocolCountryDisplay.Entry("KOR", "서버 표시명", "KOR_empire"))));
            assertEquals("서버 표시명", UiCountryDisplay.name("KOR", "기존 스냅샷"));
            assertEquals("기존 스냅샷", UiCountryDisplay.name("JAP", "기존 스냅샷"));
            UiCountryDisplay.accept(new ProtocolCountryDisplay.Table(List.of()));
            assertEquals("기존 스냅샷", UiCountryDisplay.name("KOR", "기존 스냅샷"));
            UiCountryDisplay.accept(new ProtocolCountryDisplay.Table(List.of(new ProtocolCountryDisplay.Entry("KOR", "새 표시명", "KOR"))));
            UiCountryDisplay.clear();
            assertEquals("재접속 스냅샷", UiCountryDisplay.name("KOR", "재접속 스냅샷"));
        } finally { UiCountryDisplay.clear(); }
    }
}
