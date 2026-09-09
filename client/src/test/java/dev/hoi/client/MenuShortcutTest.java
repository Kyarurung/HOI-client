package dev.hoi.client;

import net.minecraft.client.input.KeyEvent;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import static org.junit.jupiter.api.Assertions.*;

class MenuShortcutTest {
    @Test void onlyShiftFMatchesAndLockKeysDoNotInterfere() {
        assertTrue(MenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_SHIFT)));
        assertTrue(MenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CAPS_LOCK)));
        assertFalse(MenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, 0)));
        assertFalse(MenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_R, 0, 0)));
        assertFalse(MenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_R, 0, GLFW.GLFW_MOD_SHIFT)));
        assertFalse(MenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL)));
        assertFalse(MenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT)));
    }
}
