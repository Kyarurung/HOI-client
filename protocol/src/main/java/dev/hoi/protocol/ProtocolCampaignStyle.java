package dev.hoi.protocol;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ProtocolCampaignStyle {
    private ProtocolCampaignStyle() {}
    public static void requireSpeed(String speed) {
        if (speed == null || !switch (speed) {
            case "", "PAUSED", "X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8", "X9", "X10" -> true;
            default -> false;
        }) throw new IllegalArgumentException("Unsupported campaign speed");
    }
    public static String speedName(String speed) {
        return switch(speed) {
            case "X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8", "X9", "X10" -> speed.substring(1) + "배속";
            default -> "일시 정지";
        };
    }
    public static int speedColor(String speed) {
        return switch(speed) {
            case "X1", "X2" -> 0x55FF55;
            case "X3", "X4" -> 0xFFFF55;
            case "X5", "X6" -> 0xFFAA00;
            case "X7", "X8" -> 0xFF5555;
            case "X9", "X10" -> 0xAA0000;
            default -> 0xAAAAAA;
        };
    }
    public static MutableComponent speed(String speed) { return Component.literal(speedName(speed)).withStyle(style -> style.withColor(speedColor(speed))); }
    public static MutableComponent speedNotice(String speed) { return Component.literal("게임 시간: ").append(speed(speed)); }
}
