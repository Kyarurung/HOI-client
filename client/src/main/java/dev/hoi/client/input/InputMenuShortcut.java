package dev.hoi.client.input;

import dev.hoi.client.screen.ScreenMapCursor;


import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;


public final class InputMenuShortcut {
    private static boolean held;
    private InputMenuShortcut() {}
    public static void reset() { held = false; }
    public static boolean matches(KeyEvent event) {
        int modifiers = event.modifiers();
        return event.key() == GLFW.GLFW_KEY_F && (modifiers & (GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) == 0;
    }
    public static boolean handle(Minecraft client, long window, int action, KeyEvent event) {
        if (window != client.getWindow().handle()) return false;
        if (event.key() == GLFW.GLFW_KEY_F && held) {
            if (action == GLFW.GLFW_RELEASE) held = false;
            return true;
        }
        if (client.player == null || !dev.hoi.client.ClientCampaignHud.visible()
                || (client.gui.screen() != null && !(client.gui.screen() instanceof ScreenMapCursor))
                || client.gui.overlay() != null || !client.isWindowActive() || !matches(event)) return false;
        if (action == GLFW.GLFW_PRESS) {
            held = true;
            client.gui.setScreen(client.gui.screen() instanceof ScreenMapCursor ? null : new ScreenMapCursor());
            return true;
        }
        return action == GLFW.GLFW_REPEAT;
    }
}
