package dev.hoi.client.audio;

import java.util.*;

public final class AudioPlaylistPlayback {
    public record AudioTrack(String event, String title, double seconds, String cover) {
        public AudioTrack(String event,String title,double seconds){this(event,title,seconds,"");}
        public AudioTrack {
            cover = cover == null ? "" : cover;
            if (!cover.isEmpty() && !cover.matches("music/album/[a-z0-9_]+")) throw new IllegalArgumentException("Invalid album image");
            if (event == null || !event.matches("music\\.korea\\.[a-z0-9_.]+") || title == null || title.isBlank()
                    || title.length() > 256 || !Double.isFinite(seconds) || seconds <= 0 || seconds > 3600)
                throw new IllegalArgumentException("Invalid Korean music track");
        }
    }
    public interface AudioOutput {
        boolean start(AudioTrack track);
        boolean active();
        void stop();
        void pause(boolean paused);
    }
    private final AudioOutput output;
    private List<AudioTrack> tracks = List.of();
    private int selected, delay;
    private boolean requested, streaming, paused;
    public AudioPlaylistPlayback(AudioOutput output) { this.output = Objects.requireNonNull(output); }
    public List<AudioTrack> tracks() { return tracks; }
    public int selected() { return selected; }
    public boolean requested() { return requested; }
    public boolean paused() { return paused; }
    public void togglePause() {
        if (!requested) { if (!tracks.isEmpty()) play(selected); return; }
        paused = !paused;
        if (streaming) output.pause(paused);
    }
    public static List<AudioTrack> validate(List<AudioTrack> replacement) {
        replacement = List.copyOf(replacement);
        if (replacement.size() > 128 || replacement.stream().map(AudioTrack::event).distinct().count() != replacement.size())
            throw new IllegalArgumentException("Invalid Korean playlist");
        return replacement;
    }
    public void replace(List<AudioTrack> replacement) {
        replacement = validate(replacement);
        String previous = tracks.isEmpty() ? "" : tracks.get(selected).event();
        output.stop(); streaming = false; delay = 0;
        tracks = replacement; selected = 0;
        for (int i = 0; i < tracks.size(); i++) if (tracks.get(i).event().equals(previous)) selected = i;
        if (tracks.isEmpty()) requested = false;
    }
    public void play(int index) {
        Objects.checkIndex(index, tracks.size());
        output.stop(); selected = index; requested = true; streaming = false; paused = false; delay = 0;
    }
    public void step(int direction) {
        if (!tracks.isEmpty()) play(Math.floorMod(selected + direction, tracks.size()));
    }
    public void stop() { output.stop(); requested = false; streaming = false; paused = false; delay = 0; }
    public void tick(boolean allowed) {
        if (!requested || tracks.isEmpty()) return;
        if (!allowed) {
            if (streaming) output.stop();
            streaming = false; delay = 0; return;
        }
        if (paused) return;
        if (delay > 0) { delay--; return; }
        if (!streaming) {
            streaming = output.start(tracks.get(selected)); delay = 20;
            if (!streaming) selected = (selected + 1) % tracks.size();
        } else if (!output.active()) {
            output.stop(); selected = (selected + 1) % tracks.size(); streaming = false;
        }
    }
}
