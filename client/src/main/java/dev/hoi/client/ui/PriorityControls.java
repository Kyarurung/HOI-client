package dev.hoi.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class PriorityControls {
    private PriorityControls() {}
    public static boolean shifted() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
    public static Component tooltip(boolean up) {
        return Component.literal("우선순위를 " + (up ? "높이려면 " : "낮추려면 ")).withStyle(ChatFormatting.WHITE)
                .append(Component.literal("클릭하십시오.\nShift + 클릭하면 해당 생산에 ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(up ? "최우선 순위" : "최하위 우선순위").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("를 부여합니다.\n\n").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("우선순위가 높은 생산 라인은 우선순위가 낮은 생산 라인보다 먼저 자원을 할당 받습니다.").withStyle(ChatFormatting.WHITE));
    }
}
