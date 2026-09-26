package dev.hoi.protocol;

import java.util.Locale;

public final class ProtocolNumberBounds {
    private ProtocolNumberBounds() {}

    public static String fixed(double value, int decimals) {
        return String.format(Locale.ROOT, pattern(decimals, false), value);
    }

    public static String percent(double ratio, int decimals) {
        return fixed(ratio * 100, decimals) + "%";
    }

    public static String signedPercent(double ratio, int decimals) {
        return String.format(Locale.ROOT, pattern(decimals, true), ratio * 100) + "%";
    }

    private static String pattern(int decimals, boolean signed) {
        if (decimals < 0 || decimals > 9) throw new IllegalArgumentException("Invalid display precision");
        return (signed ? "%+." : "%.") + decimals + "f";
    }
}
