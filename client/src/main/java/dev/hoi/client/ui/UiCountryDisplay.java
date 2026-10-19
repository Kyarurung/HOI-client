package dev.hoi.client.ui;

import dev.hoi.protocol.ProtocolCountryDisplay;
import java.util.List;

public final class UiCountryDisplay {
    private static ProtocolCountryDisplay.Table table = new ProtocolCountryDisplay.Table(List.of());
    private UiCountryDisplay() {}
    public static void accept(ProtocolCountryDisplay.Table next) { table = next; }
    public static void clear() { table = new ProtocolCountryDisplay.Table(List.of()); }
    public static String name(String country, String fallback) {
        return table.country(country).map(ProtocolCountryDisplay.Entry::name).orElse(fallback);
    }
    public static String flag(String country) {
        if (country == null || country.isBlank()) return "";
        var display = table.country(country);
        return display.isPresent() ? UiStoryAssets.flag(country, display.get().flag())
                : "country/" + country.toLowerCase(java.util.Locale.ROOT) + "/flag";
    }
    static String texture(String path) {
        if (path.matches("country/[a-z0-9]{3}/flag")) return flag(path.substring(8, 11).toUpperCase(java.util.Locale.ROOT));
        return UiStoryAssets.asset(path);
    }
}
