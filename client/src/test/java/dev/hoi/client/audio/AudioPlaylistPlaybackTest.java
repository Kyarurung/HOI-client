package dev.hoi.client.audio;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioPlaylistPlaybackTest {
    private static final List<AudioPlaylistPlayback.AudioTrack> TRACKS = List.of(
            new AudioPlaylistPlayback.AudioTrack("music.korea.first", "첫 곡", 100),
            new AudioPlaylistPlayback.AudioTrack("music.korea.second", "다음 곡", 120));
    private static class AudioTestOutput implements AudioPlaylistPlayback.AudioOutput {
        int starts; boolean playing, paused, ready = true; String track;
        public boolean start(AudioPlaylistPlayback.AudioTrack selected) {
            assertFalse(playing); starts++; track = selected.event(); playing = ready; return ready;
        }
        public boolean active() { return playing; }
        public void stop() { playing = false; paused = false; }
        public void pause(boolean value) { paused = value; }
    }
    @Test void pauseResumesTheSameStreamAndSelectionWithoutAdvancingOrRestarting() {
        var out=new AudioTestOutput();var player=new AudioPlaylistPlayback(out);player.replace(TRACKS);
        player.togglePause();player.tick(true);player.togglePause();
        assertTrue(player.paused());assertTrue(out.paused);
        for(int i=0;i<200;i++)player.tick(true);
        assertEquals(1,out.starts);assertEquals(0,player.selected());
        player.togglePause();player.tick(true);
        assertFalse(out.paused);assertEquals(1,out.starts);
        player.togglePause();player.tick(false);player.tick(true);
        assertEquals(1,out.starts);assertTrue(player.paused());
        player.togglePause();player.tick(true);assertEquals(2,out.starts);
        player.togglePause();player.stop();assertFalse(player.paused());assertFalse(player.requested());
    }
    @Test void twoPlayersKeepIndependentSelectionsAndStopStates() {
        var a=new AudioTestOutput();var b=new AudioTestOutput();var first=new AudioPlaylistPlayback(a);var second=new AudioPlaylistPlayback(b);
        first.replace(TRACKS);second.replace(TRACKS);first.play(0);second.play(1);
        first.tick(true);second.tick(true);first.stop();
        assertTrue(b.playing);assertEquals(1,second.selected());assertTrue(second.requested());
        first.play(0);first.tick(true);first.tick(false);second.tick(false);
        assertFalse(a.playing);assertFalse(b.playing);
        first.tick(true);second.tick(true);assertEquals(TRACKS.get(0).event(),a.track);assertEquals(TRACKS.get(1).event(),b.track);
    }
    @Test void advancesOnceAfterStreamEndsAndWrapsWithoutOverlapping() {
        var out = new AudioTestOutput(); var player = new AudioPlaylistPlayback(out); player.replace(TRACKS);
        player.play(0); player.tick(true); assertEquals(1, out.starts);
        for (int i = 0; i < 30; i++) player.tick(true);
        assertEquals(1, out.starts);
        out.playing = false; player.tick(true); player.tick(true);
        assertEquals(1, player.selected()); assertEquals(2, out.starts);
        player.step(1); player.tick(true);
        assertEquals(0, player.selected()); assertEquals(3, out.starts);
    }
    @Test void muteSuperEventAndReloadDoNotSkipTrackOrRestartStoppedPlayback() {
        var out = new AudioTestOutput(); var player = new AudioPlaylistPlayback(out); player.replace(TRACKS);
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
        var out = new AudioTestOutput(); out.ready = false;
        var player = new AudioPlaylistPlayback(out); player.replace(TRACKS); player.play(0);
        for (int i = 0; i < 100; i++) player.tick(true);
        assertEquals(5, out.starts);
        assertThrows(IllegalArgumentException.class, () -> player.replace(List.of(TRACKS.get(0), TRACKS.get(0))));
        assertEquals(TRACKS, player.tracks());
        assertThrows(IndexOutOfBoundsException.class, () -> player.play(2));
        player.replace(List.of()); player.step(1); player.tick(true); assertFalse(player.requested());
    }
}
