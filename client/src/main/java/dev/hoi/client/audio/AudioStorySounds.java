package dev.hoi.client.audio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoi.client.ClientCampaignHud;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class AudioStorySounds {
    private static JsonObject bindings;
    private AudioStorySounds() {}
    public static void clear() { bindings = null; }
    public static void play(String event) {
        if (bindings == null) {
            bindings = new JsonObject();
            var resource = Minecraft.getInstance().getResourceManager().getResource(Identifier.parse("hoi:ui/story_audio.json"));
            if (resource.isPresent()) try (var input = resource.get().open()) {
                var bytes = input.readNBytes(524289);
                if (bytes.length <= 524288) bindings = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (java.io.IOException | RuntimeException invalid) { bindings = new JsonObject(); }
        }
        AudioUiSounds.play(resolve(bindings, ClientCampaignHud.country(), event));
    }
    static String resolve(JsonObject root, String country, String event) {
        if (!event.matches("sound\\.story\\.[a-z0-9_.]+")) return "";
        try {
            if (root.get("version").getAsInt() != 1) return "";
            String raw = event.substring("sound.story.".length());
            var effects = root.getAsJsonObject("effects");
            for (String context : country.equals("KOR") || country.equals("PRK") ? List.of("korea", "tfr") : List.of("tfr")) {
                var rows = effects.getAsJsonObject(context);
                if (rows == null) continue;
                for (var entry : rows.entrySet()) {
                    if (!entry.getKey().toLowerCase(Locale.ROOT).replace('-', '_').equals(raw)) continue;
                    String path = entry.getValue().getAsString();
                    return path.matches("hoi:[a-z0-9_./-]+") && !path.contains("..") ? path.substring(4) : "";
                }
            }
        } catch (RuntimeException invalid) { return ""; }
        return "";
    }
}
