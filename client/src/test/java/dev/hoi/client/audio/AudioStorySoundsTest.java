package dev.hoi.client.audio;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioStorySoundsTest {
    @Test void sourceBindingsRespectCountryOverlayAndMissingSamples() {
        var root = JsonParser.parseString("""
                {"version":1,"effects":{"tfr":{"TFR_se_Xi":"hoi:ui.story.tfr.xi","same":"hoi:ui.story.tfr.same"},
                "korea":{"same":"hoi:ui.story.korea.same","unsafe":"hoi:../secret"}}}
                """).getAsJsonObject();
        assertEquals("ui.story.tfr.xi", AudioStorySounds.resolve(root, "KOR", "sound.story.tfr_se_xi"));
        assertEquals("ui.story.korea.same", AudioStorySounds.resolve(root, "PRK", "sound.story.same"));
        assertEquals("ui.story.tfr.same", AudioStorySounds.resolve(root, "JAP", "sound.story.same"));
        assertEquals("", AudioStorySounds.resolve(root, "KOR", "sound.story.unsafe"));
        assertEquals("", AudioStorySounds.resolve(root, "JAP", "sound.story.unknown"));
        assertEquals("", AudioStorySounds.resolve(root, "JAP", "ui.click"));
        root.addProperty("version", 2);
        assertEquals("", AudioStorySounds.resolve(root, "JAP", "sound.story.same"));
    }
}
