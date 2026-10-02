package dev.hoi.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class UiPriorityControls {
    private UiPriorityControls() {}
    public static boolean shifted() {
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT);
    }
    public static boolean controlled() {
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RCONTROL);
    }
    public static Component tooltip(boolean up) {
        return Component.literal("우선순위를 " + (up ? "높이려면 " : "낮추려면 ")).withStyle(ChatFormatting.WHITE)
                .append(Component.literal("클릭하십시오.\nShift + 클릭하면 해당 생산에 ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(up ? "최우선 순위" : "최하위 우선순위").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("를 부여합니다.\n\n").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("우선순위가 높은 생산 라인은 우선순위가 낮은 생산 라인보다 먼저 자원을 할당 받습니다.").withStyle(ChatFormatting.WHITE));
    }
}
