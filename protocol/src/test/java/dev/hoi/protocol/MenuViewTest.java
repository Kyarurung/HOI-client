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
    @Test void optionalHudRetainsLegacySnapshotsAndNuclearInventoryBeforeResearch() {
        var legacy = MenuProtocol.OpenScreen.of(new MenuView("KOR", "한국", "", "", pages())).json();
        var object = com.google.gson.JsonParser.parseString(legacy).getAsJsonObject(); object.remove("hud");
        assertEquals(CountryHud.UNKNOWN, new MenuProtocol.OpenScreen(object.toString()).view().hud());
        var hud = new CountryHud(150.0, 119.0, 209.0, 1787.0, 704.315, .36, true, 0L);
        assertEquals(hud, MenuProtocol.OpenScreen.of(new MenuView("KOR", "한국", "", "", pages(), hud)).view().hud());
        assertEquals(5L, new CountryHud(null, null, null, null, null, null, false, 5L).nuclearStockpile());
        assertThrows(IllegalArgumentException.class, () -> new CountryHud(Double.NaN, null, null, null, null, null, false, null));
        assertThrows(IllegalArgumentException.class, () -> new CountryHud(null, null, null, null, null, 1.01, false, null));
    }
    @Test void nationalIndicatorsKeepUnknownDistinctFromZeroAndRejectInvalidRatios() {
        var national = new CountryHud.NationalIndicators(-2.0, .62, .48, 45L, 0.0, 500.0, null, null, 0L, 1.0, 150.0, .36, 280_000L);
        var hud = new CountryHud(150.0, 119.0, 209.0, null, null, .36, false, null, national);
        var payload = MenuProtocol.OpenScreen.of(new MenuView("KOR", "한국", "", "", pages(), hud));
        assertEquals(hud, payload.view().hud());
        var object = com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();
        object.getAsJsonObject("hud").remove("national");
        assertEquals(CountryHud.NationalIndicators.UNKNOWN, new MenuProtocol.OpenScreen(object.toString()).view().hud().national());
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -.01, 1.01})
            assertThrows(IllegalArgumentException.class, () -> new CountryHud.NationalIndicators(
                    null, null, null, null, null, null, null, invalid, null, null, null, null, null));
    }
}
