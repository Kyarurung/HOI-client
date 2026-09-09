package dev.hoi.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.input.KeyEvent;
import java.util.List;
import java.util.function.Predicate;


public final class SidebarMovement {
    public interface Screen { boolean allowsMovement(); }
    private static boolean controlled;
    private SidebarMovement() {}
    private static List<KeyMapping> keys(Minecraft client) {
        var o = client.options;
        return List.of(o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keyShift, o.keySprint);
    }
    public static boolean consumes(Minecraft client, KeyEvent event) {
        return keys(client).stream().anyMatch(key -> key.matches(event));
    }
    public static void tick(Minecraft client) {
        boolean allowed = client.player != null && client.isWindowActive() && client.gui.overlay() == null
                && client.gui.screen() instanceof Screen screen && screen.allowsMovement();
        if (allowed) {
            controlled = true;
            apply(client, key -> {
                var bound = InputConstants.getKey(key.saveString());
                return bound.getType() == InputConstants.Type.KEYSYM && bound.getValue() >= 0
                        && InputConstants.isKeyDown(client.getWindow(), bound.getValue());
            });
        } else release(client);
    }

    static void apply(Minecraft client, Predicate<KeyMapping> pressed) {
        for (var key : keys(client)) {
            boolean down = pressed.test(key);
            if (key instanceof ToggleKeyMapping toggle) toggle.shouldRestoreStateOnScreenClosed();
            if (key.isDown() == down) continue;
            key.setDown(down);

            if (!down && key.isDown()) key.setDown(true);
        }
    }
    public static void release(Minecraft client) {
        if (controlled) { apply(client, key -> false); controlled = false; }
    }
}
