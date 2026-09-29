package dev.hoi.client.input;

import net.minecraft.client.input.KeyEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputMenuShortcutTest {
    @Test void onlyFMatchesAndLockKeysDoNotInterfere() {
        assertFalse(InputMenuShortcut.matches(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F, 0, com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT)));
        assertTrue(InputMenuShortcut.matches(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F, 0, com.mojang.blaze3d.platform.InputConstants.MOD_CAPS_LOCK)));
        assertTrue(InputMenuShortcut.matches(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F, 0, 0)));
        assertFalse(InputMenuShortcut.matches(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_R, 0, 0)));
        assertFalse(InputMenuShortcut.matches(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_R, 0, com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT)));
        assertFalse(InputMenuShortcut.matches(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F, 0, com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT | com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL)));
        assertFalse(InputMenuShortcut.matches(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F, 0, com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT | com.mojang.blaze3d.platform.InputConstants.MOD_ALT)));
    }
}
