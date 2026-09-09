package dev.hoi.client.research;

import dev.hoi.client.ui.UiAssets;

import com.google.gson.JsonParser;
import dev.hoi.protocol.ResearchView.Tech;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import java.util.*;


public final class ResearchDetails {
    public record Card(String name, String kind, String texture, boolean representative, List<String> effects) {
        public Card { effects = List.copyOf(effects); }
    }
    private static Map<String, List<Card>> cached;
    private ResearchDetails() {}
    public static void clear() { cached = null; }
    public static List<Card> cards(Tech tech) {
        if (tech.source() == null) return List.of();
        if (cached == null) cached = load();
        return cached.getOrDefault(tech.source().id(), List.of());
    }
    public static List<Card> cards(Tech tech, String country) {
        String art = UiAssets.technologyPath(country, tech);
        return cards(tech).stream().map(card -> {
            boolean useArt = !art.isEmpty() && card.kind().equals("장비") && (card.texture().isEmpty() || card.representative());
            String name = tech.source().localizedNames().getOrDefault(card.name(), card.name());
            return new Card(name, card.kind(), useArt ? art : card.texture(), useArt || card.representative(), card.effects());
        }).toList();
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
