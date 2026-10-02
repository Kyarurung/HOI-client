package dev.hoi.client.input;

import dev.hoi.client.screen.ScreenMapCursor;


import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public final class InputMenuShortcut {
    private static boolean held;
    private InputMenuShortcut() {}
    public static void reset() { held = false; }
    public static boolean matches(KeyEvent event) {
        int modifiers = event.modifiers();
        return event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_F && (modifiers & (com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT | com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL | com.mojang.blaze3d.platform.InputConstants.MOD_ALT | com.mojang.blaze3d.platform.InputConstants.MOD_SUPER)) == 0;
    }
    public static boolean handle(Minecraft client, long window, int action, KeyEvent event) {
        if (window != client.getWindow().handle()) return false;
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_F && held) {
            if (action == com.mojang.blaze3d.platform.InputConstants.RELEASE) held = false;
            return true;
        }
        if (client.player == null || !dev.hoi.client.ClientCampaignHud.visible()
                || (client.gui.screen() != null && !(client.gui.screen() instanceof ScreenMapCursor))
                || client.gui.overlay() != null || !client.isWindowActive() || !matches(event)) return false;
        if (action == com.mojang.blaze3d.platform.InputConstants.PRESS) {
            held = true;
            client.gui.setScreen(client.gui.screen() instanceof ScreenMapCursor ? null : new ScreenMapCursor());
            return true;
        }
        return action == com.mojang.blaze3d.platform.InputConstants.REPEAT;
    }
}
