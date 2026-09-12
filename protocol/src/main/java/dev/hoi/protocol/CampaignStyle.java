package dev.hoi.protocol;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;


public final class CampaignStyle {
    private CampaignStyle() {}
    public static final int SEPARATOR = 0x555555;
    public static String speedName(String speed) {
        return switch(speed) {
            case "X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8", "X9", "X10", "X11", "X12", "X13", "X14", "X15", "X16", "X17", "X18", "X19", "X20" -> speed.substring(1) + "배속";
            default -> "일시 정지";
        };
    }
    public static int speedColor(String speed) {
        return switch(speed) {
            case "X1", "X2", "X3", "X4" -> 0x55FF55;
            case "X5", "X6", "X7", "X8" -> 0xFFFF55;
            case "X9", "X10", "X11", "X12" -> 0xFFAA00;
            case "X13", "X14", "X15", "X16" -> 0xFF5555;
            case "X17", "X18", "X19", "X20" -> 0xAA0000;
            default -> 0xAAAAAA;
        };
    }
    public static MutableComponent separator() { return Component.literal(" | ").withStyle(style -> style.withColor(SEPARATOR)); }
    public static MutableComponent speed(String speed) { return Component.literal(speedName(speed)).withStyle(style -> style.withColor(speedColor(speed))); }
    public static MutableComponent speedNotice(String speed) { return Component.literal("게임 시간: ").append(speed(speed)); }
    public static MutableComponent clock(String date, String speed) { return Component.literal(date).append(separator()).append(speed(speed)); }
}
