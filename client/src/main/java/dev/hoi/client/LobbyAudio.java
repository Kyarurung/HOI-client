package dev.hoi.client;

/** Retains select while a pack loads; start/disconnect cancel that pending request too. */
final class LobbyAudio {
    interface Output { void stopTheme(); void playTheme(); void playStart(); }
    private final Output output;
    private boolean requested;
    LobbyAudio(Output output) { this.output = output; }
    void select() { requested = true; }
    void tick(boolean ready, boolean playing) { if (requested && ready && !playing) output.playTheme(); }
    void start() { requested = false; output.stopTheme(); output.playStart(); }
    void reset() { requested = false; output.stopTheme(); }
    boolean requested() { return requested; }
}
