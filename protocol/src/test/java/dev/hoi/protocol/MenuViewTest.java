package dev.hoi.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuViewTest {
    @Test void manufacturerGroupsOverlapWithoutChangingIndividualEffects() {
        var entry = new MenuView.Entry("포 제조사", "", "기갑 연구 +10% · 포 연구 +5%");
        var manufacturer = new MenuView.Manufacturer("hoi:artillery", List.of("armor", "materiel"), entry);
        var pages = MenuTab.ORDER.stream().map(tab -> new MenuView.Page(tab,
                List.of(new MenuView.Section("현황", "", List.of())))).toList();
        var view = new MenuView("KOR", "대한민국", "2020-01-01", "PAUSED", pages, CountryHud.UNKNOWN, List.of(manufacturer));
        var gson = new Gson();
        assertEquals(view, gson.fromJson(gson.toJson(view), MenuView.class));
        var legacy = JsonParser.parseString(gson.toJson(view)).getAsJsonObject();
        legacy.remove("manufacturers");
        assertTrue(gson.fromJson(legacy, MenuView.class).manufacturers().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new MenuView.Manufacturer("hoi:bad", List.of("unknown"), entry));
        assertThrows(IllegalArgumentException.class, () -> new MenuView("KOR", "대한민국", "2020-01-01", "PAUSED",
                pages, CountryHud.UNKNOWN, List.of(manufacturer, manufacturer)));
    }
}
