package dev.hoi.client.input;

import net.minecraft.client.input.KeyEvent;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import static org.junit.jupiter.api.Assertions.*;

class InputMenuShortcutTest {
    @Test void onlyFMatchesAndLockKeysDoNotInterfere() {
        assertFalse(InputMenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_SHIFT)));
        assertTrue(InputMenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_CAPS_LOCK)));
        assertTrue(InputMenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, 0)));
        assertFalse(InputMenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_R, 0, 0)));
        assertFalse(InputMenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_R, 0, GLFW.GLFW_MOD_SHIFT)));
        assertFalse(InputMenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL)));
        assertFalse(InputMenuShortcut.matches(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT)));
    }
}
