package dev.hoi.client.input;

import dev.hoi.client.HoiClient;

import dev.hoi.protocol.MenuTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;


public final class MenuShortcut {
    private MenuShortcut() {}
    public static boolean matches(KeyEvent event) {
        int modifiers = event.modifiers();
        return event.key() == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0
                && (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) == 0;
    }
    public static boolean handle(Minecraft client, long window, int action, KeyEvent event) {
        if (window != client.getWindow().handle() || client.player == null || client.gui.screen() != null
                || client.gui.overlay() != null || !client.isWindowActive() || !matches(event)) return false;
        if (action == GLFW.GLFW_PRESS) {
            if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(dev.hoi.protocol.MenuProtocol.Refresh.TYPE))
                HoiClient.openMenu(MenuTab.POLITICS);
            return true;
        }
        return action == GLFW.GLFW_REPEAT;
    }
}
