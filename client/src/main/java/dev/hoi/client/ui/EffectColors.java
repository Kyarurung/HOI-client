package dev.hoi.client.ui;

import net.minecraft.util.FormattedCharSequence;

public final class EffectColors {
    public static final int GOOD = 0xFF94BA8A;
    public static final int BAD = 0xFFCB9292;

    private EffectColors() {}

    public static int color(int value) {
        int rgb = switch (value & 0xFFFFFF) {
            case 0x55FF55, 0x00AA00, 0x55AA33 -> GOOD;
            case 0xFF5555, 0xAA0000, 0xAA3333, 0xDD4444, 0xB53543 -> BAD;
            default -> value;
        };
        return (value & 0xFF000000) | (rgb & 0xFFFFFF);
    }

    public static FormattedCharSequence text(FormattedCharSequence value) {
        return sink -> value.accept((index, style, character) -> {
            var tint = style.getColor();
            if (tint != null) {
                int mapped = color(tint.getValue());
                if (mapped != tint.getValue()) style = style.withColor(mapped);
            }
            return sink.accept(index, style, character);
        });
    }
}
