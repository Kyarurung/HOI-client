package dev.hoi.protocol;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;


public final class CampaignStyle {
    private CampaignStyle() {}
    public static final int SEPARATOR = 0x555555;
    public static String speedName(String speed) {
        return switch(speed) { case "X1" -> "1배속"; case "X2" -> "2배속"; case "X3" -> "3배속"; case "X4" -> "4배속"; case "X5" -> "5배속"; default -> "일시정지"; };
    }
    public static int speedColor(String speed) {
        return switch(speed) { case "X1" -> 0x55FF55; case "X2" -> 0xFFFF55; case "X3" -> 0xFFAA00; case "X4" -> 0xFF5555; case "X5" -> 0xAA0000; default -> 0xAAAAAA; };
    }
    public static MutableComponent separator() { return Component.literal(" | ").withStyle(style -> style.withColor(SEPARATOR)); }
    public static MutableComponent speed(String speed) { return Component.literal(speedName(speed)).withStyle(style -> style.withColor(speedColor(speed))); }
    public static MutableComponent clock(String date, String speed) { return Component.literal(date).append(separator()).append(speed(speed)); }
}
