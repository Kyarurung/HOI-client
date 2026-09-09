package dev.hoi.protocol;

import java.util.List;
import java.util.Objects;


public record HudDetail(List<Row> rows) {
    public HudDetail {
        rows = List.copyOf(rows);
        if (rows.size() > 64) throw new IllegalArgumentException("Too many HUD detail rows");
    }

    public enum Tone { NORMAL, GOOD, BAD, MUTED, HEADING }

    public record Row(String label, String value, Tone tone) {
        public Row {
            literal(label, 200); literal(value, 80); Objects.requireNonNull(tone);
        }
    }

    static void literal(String text, int max) {
        if (text == null || text.length() > max || text.codePoints().anyMatch(c -> Character.isISOControl(c) || c == 0x00a7))
            throw new IllegalArgumentException("Invalid HUD detail text");
    }
}
