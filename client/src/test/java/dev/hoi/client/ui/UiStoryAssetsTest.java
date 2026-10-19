package dev.hoi.client.ui;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiStoryAssetsTest {
    @Test void rawPortraitsAndDynamicIconsUseExactProvenanceBindings() {
        var root = JsonParser.parseString("""
                {"version":1,"dynamic_modifiers":{"GFX_dynamic":"story/icons/dynamic"},
                "countries":{"KOR":{"portraits":{"GFX_own":"portraits/kor/own","GFX_shared":"portraits/kor/shared"}},
                "PRK":{"portraits":{"GFX_foreign":"portraits/prk/foreign","GFX_shared":"portraits/prk/shared"}}}}
                """).getAsJsonObject();
        assertEquals("story/icons/dynamic", UiStoryAssets.asset(root, "KOR", "GFX_dynamic"));
        assertEquals("story/icons/dynamic", UiStoryAssets.asset(root, "KOR", "politics/art/gfx_dynamic"));
        assertEquals("portraits/prk/foreign", UiStoryAssets.asset(root, "KOR", "GFX_foreign"));
        assertEquals("portraits/kor/shared", UiStoryAssets.asset(root, "KOR", "GFX_shared"));
        assertEquals("", UiStoryAssets.asset(root, "JAP", "GFX_shared"));
        assertEquals("", UiStoryAssets.asset(root, "KOR", "GFX_unknown"));
    }
    @Test void flagsUseExactCountryAndServerKeyWithoutConstructingVariants() {
        var root=JsonParser.parseString("""
                {"version":1,"countries":{"KOR":{"flags":{"KOR_communism":"story/flags/kor/test","KOR":"country/kor/flag"}},
                "PRK":{"flags":{"KOR_communism":"story/flags/prk/test","invalid":"../private","missing":{"status":"missing_file"}}}}}
                """).getAsJsonObject();
        assertEquals("story/flags/kor/test",UiStoryAssets.flag(root,"KOR","KOR_communism"));
        assertEquals("story/flags/prk/test",UiStoryAssets.flag(root,"PRK","KOR_communism"));
        assertEquals("",UiStoryAssets.flag(root,"KOR","KOR_democratic"));
        assertEquals("",UiStoryAssets.flag(root,"JAP","KOR_communism"));
        assertEquals("",UiStoryAssets.flag(root,"PRK","invalid"));
        assertEquals("",UiStoryAssets.flag(root,"PRK","missing"));
        root.addProperty("version",2);
        assertEquals("",UiStoryAssets.flag(root,"KOR","KOR_communism"));
    }
}
