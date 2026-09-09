package dev.hoi.client.ui;

import com.google.gson.JsonParser;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;


public final class KeywordIcons {
    private static final FontDescription FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("hoi", "keywords"));
    private record Binding(String label, String glyph) {}
    private static List<Binding> bindings;
    private KeywordIcons() {}
    static void clear() { bindings = null; }
    public static Component decorate(Component text) {
        if (bindings == null) bindings = load();
        String label = text.getString().stripLeading();
        for (var binding : bindings) {
            if (!label.startsWith(binding.label())) continue;
            int end = binding.label().length();
            if (label.length() > end && !Character.isWhitespace(label.charAt(end)) && label.charAt(end) != ':' && label.charAt(end) != '：') continue;
            return Component.empty().append(Component.literal(binding.glyph()).withStyle(s -> s.withFont(FONT).withColor(0xFFFFFF)))
                    .append(Component.literal(" ")).append(text);
        }
        return text;
    }
    private static List<Binding> load() {
        var resource = Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("hoi", "ui/keyword_icons.json"));
        if (resource.isEmpty()) return List.of();
        try (var input = resource.get().open()) {
            byte[] data = input.readNBytes(128001);
            if (data.length > 128000) return List.of();
            var root = JsonParser.parseString(new String(data, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) return List.of();
            var result = new ArrayList<Binding>();
            for (var value : root.getAsJsonArray("bindings")) {
                var row = value.getAsJsonObject(); String glyph = row.get("glyph").getAsString();
                if (glyph.length() != 1 || glyph.charAt(0) < 0xe000 || glyph.charAt(0) > 0xf8ff) return List.of();
                for (var item : row.getAsJsonArray("labels")) {
                    String label = item.getAsString();
                    if (label.isBlank() || label.length() > 80 || result.size() >= 1024) return List.of();
                    result.add(new Binding(label, glyph));
                }
            }
            result.sort(Comparator.comparingInt((Binding b) -> b.label().length()).reversed());
            return List.copyOf(result);
        } catch (Exception invalidPack) { return List.of(); }
    }
}
