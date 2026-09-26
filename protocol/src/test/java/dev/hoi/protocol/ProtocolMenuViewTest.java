package dev.hoi.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolMenuViewTest {
    @Test void manufacturerGroupsOverlapWithoutChangingIndividualEffects() {
        var entry = new ProtocolMenuView.Entry("포 제조사", "", "기갑 연구 +10% · 포 연구 +5%");
        var manufacturer = new ProtocolMenuView.Manufacturer("hoi:artillery", List.of("armor", "materiel"), entry);
        var pages = ProtocolMenuTab.ORDER.stream().map(tab -> new ProtocolMenuView.Page(tab,
                List.of(new ProtocolMenuView.Section("현황", "", List.of())))).toList();
        var view = new ProtocolMenuView("KOR", "대한민국", "2020-01-01", "PAUSED", pages, ProtocolCountryHud.UNKNOWN, List.of(manufacturer));
        var gson = new Gson();
        assertEquals(view, gson.fromJson(gson.toJson(view), ProtocolMenuView.class));
        var legacy = JsonParser.parseString(gson.toJson(view)).getAsJsonObject();
        legacy.remove("manufacturers");
        assertTrue(gson.fromJson(legacy, ProtocolMenuView.class).manufacturers().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolMenuView.Manufacturer("hoi:bad", List.of("unknown"), entry));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolMenuView("KOR", "대한민국", "2020-01-01", "PAUSED",
                pages, ProtocolCountryHud.UNKNOWN, List.of(manufacturer, manufacturer)));
    }
}
