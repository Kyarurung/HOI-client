package dev.hoi.protocol;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuViewTest {
    private static List<MenuView.Page> pages() {
        return MenuTab.ORDER.stream().map(tab -> new MenuView.Page(tab,
                List.of(new MenuView.Section(tab.label(), tab.id(), List.of(new MenuView.Entry("항목", "값", "설명")))))).toList();
    }
    @Test void tenTfrTabsRemainOrderedAndRoundTrip() {
        assertEquals(List.of("국가 정보", "결정", "정보기관", "연구", "무역 & 경제", "건설", "생산", "모병 및 배치", "군수", "장교단"), MenuTab.ORDER.stream().map(MenuTab::label).toList());
        var view = new MenuView("KOR", "대한민국", "2020-01-01", "PAUSED", pages());
        assertEquals(view, MenuProtocol.OpenScreen.of(view).view());
        assertThrows(UnsupportedOperationException.class, () -> view.pages().clear());
        var reversed = new ArrayList<>(pages()); Collections.reverse(reversed);
        assertThrows(IllegalArgumentException.class, () -> new MenuView("KOR", "한국", "", "", reversed));
    }
    @Test void invalidAndUnboundedSnapshotsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MenuProtocol.OpenScreen("x".repeat(MenuProtocol.MAX_JSON + 1)));
        assertThrows(RuntimeException.class, () -> new MenuProtocol.OpenScreen("{}").view());
        assertThrows(RuntimeException.class, () -> new MenuProtocol.OpenScreen("null").view());
        assertThrows(IllegalArgumentException.class, () -> new MenuView.Entry("name", "value", "", Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new MenuView.Entry("name", "value", "", 1.01));
        assertThrows(IllegalArgumentException.class, () -> new MenuView.Section("x", "x", Collections.nCopies(65, new MenuView.Entry("x", "", ""))));
    }
}
