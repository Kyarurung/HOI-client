package dev.hoi.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class UiStoryAssets {
    private static JsonObject assets;
    private UiStoryAssets() {}
    public static void clear() { assets = null; }
    private static void load() {
        if (assets == null) {
            assets = new JsonObject();
            var resource = Minecraft.getInstance().getResourceManager().getResource(Identifier.parse("hoi:ui/story_assets.json"));
            if (resource.isPresent()) try (var input = resource.get().open()) {
                var bytes = input.readNBytes(524289);
                if (bytes.length <= 524288) assets = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (java.io.IOException | RuntimeException invalid) { assets = new JsonObject(); }
        }
    }
    public static String flag(String country, String key) {
        load();
        String fallback = country.matches("[A-Z][A-Z0-9]{2}") ? "country/" + country.toLowerCase(Locale.ROOT) + "/flag" : "";
        String path = flag(assets, country, key);
        return !path.isEmpty() && UiAssets.imageHeight(path, 20) > 0 ? path : fallback;
    }
    public static String asset(String key) {
        if (!key.startsWith("GFX_") && !key.startsWith("gfx/") && !key.startsWith("politics/art/gfx_")) return key;
        load();
        String path = asset(assets, dev.hoi.client.ClientCampaignHud.country(), key);
        return path.isEmpty() ? key : path;
    }
    static String asset(JsonObject root, String country, String key) {
        try {
            if (root.get("version").getAsInt() != 1) return "";
            var dynamic = root.getAsJsonObject("dynamic_modifiers");
            if (dynamic != null && dynamic.has(key)) return path(dynamic.get(key));
            if (dynamic != null && key.startsWith("politics/art/")) for (var entry : dynamic.entrySet()) {
                if (key.equals("politics/art/" + entry.getKey().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"))) return path(entry.getValue());
            }
            var countries = root.getAsJsonObject("countries");
            var own = countries.getAsJsonObject(country);
            if (own != null && own.getAsJsonObject("portraits").has(key)) return path(own.getAsJsonObject("portraits").get(key));
            String candidate = "";
            for (var entry : countries.entrySet()) {
                var portraits = entry.getValue().getAsJsonObject().getAsJsonObject("portraits");
                if (portraits == null || !portraits.has(key)) continue;
                String next = path(portraits.get(key));
                if (!candidate.isEmpty() && !candidate.equals(next)) return "";
                candidate = next;
            }
            return candidate;
        } catch (RuntimeException invalid) { return ""; }
    }
    private static String path(com.google.gson.JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return "";
        String path = value.getAsString();
        return path.matches("[a-z0-9_/.-]+") && !path.contains("..") ? path : "";
    }
    static String flag(JsonObject root, String country, String key) {
        try {
            if (root.get("version").getAsInt() != 1) return "";
            var value = root.getAsJsonObject("countries").getAsJsonObject(country).getAsJsonObject("flags").get(key);
            return path(value);
        } catch (RuntimeException invalid) { return ""; }
    }
}
