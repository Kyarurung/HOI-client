package dev.hoi.client.screen;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ScreenDecisionPreferencesTest {
    @TempDir Path directory;
    @Test void preferencesSurviveReconnectAndKeepServerCountryAndDecisionScopes() throws Exception {
        Path path=directory.resolve("decisions.properties");
        var first=new ScreenDecisionPreferences(path);
        assertTrue(first.tracking("a","KOR","export@c:JAP"));
        assertFalse(first.popup("minor"));
        assertTrue(first.popup("events"));
        assertTrue(first.popup("news"));
        first.tracking("a","KOR","export@c:JAP",false);
        first.popup("events",false);
        var loaded=new ScreenDecisionPreferences(path);
        assertFalse(loaded.tracking("a","KOR","export@s:1041"));
        assertTrue(loaded.tracking("b","KOR","export@c:JAP"));
        assertTrue(loaded.tracking("a","JAP","export@c:JAP"));
        assertTrue(loaded.tracking("a","KOR","other@c:JAP"));
        assertFalse(loaded.popup("events"));
        assertTrue(loaded.popup("news"));
        try(var files=java.nio.file.Files.list(directory)) { assertEquals(1,files.count()); }
    }
}
