package dev.hoi.protocol;

import java.util.Objects;

public final class ProtocolTextBounds {
    private ProtocolTextBounds() {}

    public static String clip(String text, int limit) {
        Objects.requireNonNull(text);
        if (limit < 0) throw new IllegalArgumentException("Negative text limit");
        if (text.length() <= limit) return text;
        return text.substring(0, boundary(text, limit));
    }

    public static String ellipsize(String text, int limit) {
        Objects.requireNonNull(text);
        if (limit < 0) throw new IllegalArgumentException("Negative text limit");
        if (text.length() <= limit) return text;
        return limit == 0 ? "" : text.substring(0, boundary(text, limit - 1)) + "…";
    }

    private static int boundary(String text, int end) {
        return end > 0 && Character.isHighSurrogate(text.charAt(end - 1))
                && Character.isLowSurrogate(text.charAt(end)) ? end - 1 : end;
    }
}
