package dev.hoi.client.audio;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AudioPlaylistSettingsTest {
    @TempDir Path directory;
    @Test void defaultsToFullVolumeWithoutCreatingAConfigDuringLoad() throws Exception {
        var file = directory.resolve("hoi-audio.properties");
        var settings = new AudioPlaylistSettings(file);
        settings.load();
        assertEquals(1.0, settings.value());
        assertFalse(Files.exists(file));
    }
    @Test void muteIntermediateAndMaximumPersistAcrossNewInstances() throws Exception {
        var file = directory.resolve("nested/hoi-audio.properties");
        var settings = new AudioPlaylistSettings(file);
        for (double volume : new double[]{0, .35, 1}) {
            settings.set(volume);
            var reconnected = new AudioPlaylistSettings(file);
            reconnected.load();
            assertEquals(volume, reconnected.value());
        }
        try (var files = Files.list(file.getParent())) {
            assertEquals(1, files.count());
        }
    }
    @Test void clampsFiniteValuesAndRejectsNonFiniteUpdatesWithoutTouchingSavedBytes() throws Exception {
        var file = directory.resolve("hoi-audio.properties");
        var settings = new AudioPlaylistSettings(file);
        settings.set(-4);
        assertEquals(0, settings.value());
        settings.set(4);
        assertEquals(1, settings.value());
        settings.set(.25);
        var bytes = Files.readAllBytes(file);
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) settings.set(invalid);
        assertEquals(.25, settings.value());
        assertArrayEquals(bytes, Files.readAllBytes(file));
    }
    @Test void invalidSavedValuesUseDefaultAndUnknownKeysSurviveAnUpdate() throws Exception {
        var file = directory.resolve("hoi-audio.properties");
        for (String invalid : new String[]{"not-a-number", "NaN", "Infinity"}) {
            Files.writeString(file, "playlistVolume=" + invalid + "\nfutureSetting=preserved\n");
            var settings = new AudioPlaylistSettings(file);
            settings.load();
            assertEquals(1, settings.value());
            settings.set(.5);
            assertTrue(Files.readString(file).contains("futureSetting=preserved"));
        }
    }
    @Test void koreanStationStartsOffAndKeepsItsChoiceBesideTheVolume() throws Exception {
        var file = directory.resolve("hoi-audio.properties");
        var settings = new AudioPlaylistSettings(file);
        settings.load();
        assertFalse(settings.station());
        settings.set(.4);
        settings.station(true);
        var reconnected = new AudioPlaylistSettings(file);
        reconnected.load();
        assertTrue(reconnected.station());
        assertEquals(.4, reconnected.value());
        reconnected.station(false);
        var again = new AudioPlaylistSettings(file);
        again.load();
        assertFalse(again.station());
        assertEquals(.4, again.value());
        Files.writeString(file, "koreanStation=maybe\n");
        var invalid = new AudioPlaylistSettings(file);
        invalid.load();
        assertFalse(invalid.station());
    }
    @Test void failedWriteKeepsTheRuntimeVolumeAndExistingBytes() throws Exception {
        var parent = directory.resolve("not-a-directory");
        Files.writeString(parent, "preserve");
        var settings = new AudioPlaylistSettings(parent.resolve("hoi-audio.properties"));
        assertThrows(java.io.IOException.class, () -> settings.set(.25));
        assertEquals(.25, settings.value());
        assertEquals("preserve", Files.readString(parent));
    }
}
