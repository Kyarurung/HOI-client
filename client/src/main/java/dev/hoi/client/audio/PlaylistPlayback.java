package dev.hoi.client.audio;

import java.util.*;

public final class PlaylistPlayback {
    public record Track(String event, String title, double seconds) {
        public Track {
            if (event == null || !event.matches("music\\.korea\\.[a-z0-9_.]+") || title == null || title.isBlank()
                    || title.length() > 256 || !Double.isFinite(seconds) || seconds <= 0 || seconds > 3600)
                throw new IllegalArgumentException("Invalid Korean music track");
        }
    }
    public interface Output {
        boolean start(Track track);
        boolean active();
        void stop();
    }
    private final Output output;
    private List<Track> tracks = List.of();
    private int selected, delay;
    private boolean requested, streaming;
    public PlaylistPlayback(Output output) { this.output = Objects.requireNonNull(output); }
    public List<Track> tracks() { return tracks; }
    public int selected() { return selected; }
    public boolean requested() { return requested; }
    public static List<Track> validate(List<Track> replacement) {
        replacement = List.copyOf(replacement);
        if (replacement.size() > 128 || replacement.stream().map(Track::event).distinct().count() != replacement.size())
            throw new IllegalArgumentException("Invalid Korean playlist");
        return replacement;
    }
    public void replace(List<Track> replacement) {
        replacement = validate(replacement);
        String previous = tracks.isEmpty() ? "" : tracks.get(selected).event();
        output.stop(); streaming = false; delay = 0;
        tracks = replacement; selected = 0;
        for (int i = 0; i < tracks.size(); i++) if (tracks.get(i).event().equals(previous)) selected = i;
        if (tracks.isEmpty()) requested = false;
    }
    public void play(int index) {
        Objects.checkIndex(index, tracks.size());
        output.stop(); selected = index; requested = true; streaming = false; delay = 0;
    }
    public void step(int direction) {
        if (!tracks.isEmpty()) play(Math.floorMod(selected + direction, tracks.size()));
    }
    public void stop() { output.stop(); requested = false; streaming = false; delay = 0; }
    public void tick(boolean allowed) {
        if (!requested || tracks.isEmpty()) return;
        if (!allowed) {
            if (streaming) output.stop();
            streaming = false; delay = 0; return;
        }
        if (delay > 0) { delay--; return; }
        if (!streaming) {
            streaming = output.start(tracks.get(selected)); delay = 20;
            if (!streaming) selected = (selected + 1) % tracks.size();
        } else if (!output.active()) {
            output.stop(); selected = (selected + 1) % tracks.size(); streaming = false;
        }
    }
}
