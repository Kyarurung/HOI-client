package dev.hoi.client;

import java.util.UUID;

final class PendingResearch {
    private String token = "", target;
    private int ticks;
    String begin() { clear(); token = UUID.randomUUID().toString(); ticks = 100; return token; }
    boolean waiting() { return ticks > 0; }
    boolean matches(String session) { return waiting() && token.equals(session); }
    String target() { return target; }
    void target(String value) { target = value; }
    boolean tick() { if (ticks == 0 || --ticks > 0) return false; clear(); return true; }
    void clear() { token = ""; target = null; ticks = 0; }
}
