package dev.hoi.client.audio;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistPlaybackTest {
    private static final List<PlaylistPlayback.Track> TRACKS = List.of(
            new PlaylistPlayback.Track("music.korea.first", "첫 곡", 100),
            new PlaylistPlayback.Track("music.korea.second", "다음 곡", 120));
    private static class Output implements PlaylistPlayback.Output {
        int starts; boolean playing, ready = true; String track;
        public boolean start(PlaylistPlayback.Track selected) {
            assertFalse(playing); starts++; track = selected.event(); playing = ready; return ready;
        }
        public boolean active() { return playing; }
        public void stop() { playing = false; }
    }
    @Test void advancesOnceAfterStreamEndsAndWrapsWithoutOverlapping() {
        var out = new Output(); var player = new PlaylistPlayback(out); player.replace(TRACKS);
        player.play(0); player.tick(true); assertEquals(1, out.starts);
        for (int i = 0; i < 30; i++) player.tick(true);
        assertEquals(1, out.starts);
        out.playing = false; player.tick(true); player.tick(true);
        assertEquals(1, player.selected()); assertEquals(2, out.starts);
        player.step(1); player.tick(true);
        assertEquals(0, player.selected()); assertEquals(3, out.starts);
    }
    @Test void muteSuperEventAndReloadDoNotSkipTrackOrRestartStoppedPlayback() {
        var out = new Output(); var player = new PlaylistPlayback(out); player.replace(TRACKS);
        player.play(1); player.tick(true); player.tick(false);
        assertFalse(out.playing); assertTrue(player.requested()); assertEquals(1, player.selected());
        player.tick(true); assertEquals(TRACKS.get(1).event(), out.track);
        player.replace(TRACKS.reversed()); player.tick(true);
        assertEquals(0, player.selected()); assertEquals(TRACKS.get(1).event(), out.track);
        player.stop(); int starts = out.starts; player.replace(TRACKS);
        for (int i = 0; i < 200; i++) player.tick(true);
        assertEquals(starts, out.starts); assertFalse(player.requested());
    }
    @Test void unavailableSoundsRetryAtABoundedRateAndMalformedCatalogKeepsExistingState() {
        var out = new Output(); out.ready = false;
        var player = new PlaylistPlayback(out); player.replace(TRACKS); player.play(0);
        for (int i = 0; i < 100; i++) player.tick(true);
        assertEquals(5, out.starts);
        assertThrows(IllegalArgumentException.class, () -> player.replace(List.of(TRACKS.get(0), TRACKS.get(0))));
        assertEquals(TRACKS, player.tracks());
        assertThrows(IndexOutOfBoundsException.class, () -> player.play(2));
        player.replace(List.of()); player.step(1); player.tick(true); assertFalse(player.requested());
    }
}
