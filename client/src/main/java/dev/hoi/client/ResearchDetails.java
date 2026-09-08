package dev.hoi.client;

import com.google.gson.JsonParser;
import dev.hoi.protocol.ResearchView.Tech;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Optional resource-pack illustrations and source reference values, never gameplay definitions. */
final class ResearchDetails {
    record Card(String name, String kind, String texture, boolean representative, List<String> effects) {
        Card { effects = List.copyOf(effects); }
    }
    private static Map<String, List<Card>> cached;
    private ResearchDetails() {}
    static void clear() { cached = null; }
    static List<Card> cards(Tech tech) {
        if (tech.source() == null) return List.of();
        if (cached == null) cached = load();
        return cached.getOrDefault(tech.source().id(), List.of());
    }
    private static Map<String, List<Card>> load() {
        var asset = Minecraft.getInstance().getResourceManager().getResource(
                Identifier.fromNamespaceAndPath("hoi", "ui/research_details.json"));
        if (asset.isEmpty()) return Map.of();
        try (var input = asset.get().open()) {
            byte[] bytes = input.readNBytes(4_000_001);
            if (bytes.length > 4_000_000) return Map.of();
            var root = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) return Map.of();
            var technologies = root.getAsJsonObject("technologies");
            if (technologies.size() > 512) return Map.of();
            var result = new HashMap<String, List<Card>>();
            for (var entry : technologies.entrySet()) {
                var values = entry.getValue().getAsJsonArray();
                if (values.size() > 64) return Map.of();
                var cards = new ArrayList<Card>();
                for (var value : values) {
                    var card = value.getAsJsonObject();
                    String name = card.get("name").getAsString(), kind = card.get("kind").getAsString();
                    String texture = card.get("texture").getAsString();
                    if (name.length() > 256 || kind.length() > 32 || texture.length() > 256
                            || !texture.matches("[a-z0-9_/.-]*") || texture.contains("..")) return Map.of();
                    var effects = new ArrayList<String>();
                    for (var effect : card.getAsJsonArray("effects")) {
                        String text = effect.getAsString();
                        if (text.length() > 256 || effects.size() >= 64) return Map.of();
                        effects.add(text);
                    }
                    cards.add(new Card(name, kind, texture, card.get("representative").getAsBoolean(), effects));
                }
                result.put(entry.getKey(), List.copyOf(cards));
            }
            return Map.copyOf(result);
        } catch (Exception invalidPack) { return Map.of(); }
    }
}
