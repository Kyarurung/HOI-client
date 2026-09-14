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
    public static Component decorateLines(Component text) {
        var result = Component.empty();
        var line = new Component[]{Component.empty()};
        text.visit((style, part) -> {
            String[] parts = part.replaceAll("£[A-Za-z0-9_]+\\h*", "").split("\\R", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    result.append(decorate(line[0])).append("\n");
                    line[0] = Component.empty();
                }
                line[0] = line[0].copy().append(Component.literal(parts[i]).withStyle(style));
            }
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return result.append(decorate(line[0]));
    }

    public static Component decorate(Component text) {
        if (bindings == null) bindings = load();
        String label = text.getString().replaceAll("§.", "").stripLeading();
        if (label.matches("에너지 농장[:：]?")) return text;
        for (var binding : bindings) {
            if (!label.startsWith(binding.label())) continue;
            int end = binding.label().length();
            if (label.length() > end && !Character.isWhitespace(label.charAt(end)) && label.charAt(end) != ':' && label.charAt(end) != '：') continue;
            // Source building labels contain indentation after their icon token.
            // Remove it only on matched effect rows, preserving colours and paragraph spacing.
            var trimmed = Component.empty();
            boolean[] leading = {true};
            text.visit((style, part) -> {
                String value = leading[0] ? part.stripLeading() : part;
                if (!value.isEmpty()) leading[0] = false;
                trimmed.append(Component.literal(value).withStyle(style));
                return Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
            return Component.empty().append(Component.literal(binding.glyph()).withStyle(s -> s.withFont(FONT).withColor(0xFFFFFF)))
                    .append(Component.literal(" ")).append(trimmed);
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
