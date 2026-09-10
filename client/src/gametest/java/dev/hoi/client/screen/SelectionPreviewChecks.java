package dev.hoi.client.screen;

import dev.hoi.protocol.MenuView;
import dev.hoi.client.ui.HoiMenuBar;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class SelectionPreviewChecks {
    public static void run(ClientGameTestContext context, MenuView view) {
        for (int[] size : new int[][]{{1600, 1000, 1}, {1600, 1000, 2}, {1600, 1000, 3}, {854, 480, 2}}) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.runOnClient(client -> client.options.guiScale().set(size[2]));
            context.setScreen(() -> HoiMenuScreen.selection(view));
            context.waitTicks(3);
            context.runOnClient(client -> {
                var screen = (HoiMenuScreen) client.gui.screen();
                if (Math.abs(screen.selectionLeft() * 2 + screen.panelWidth() - screen.width) > 1
                        || Math.abs(screen.selectionTop() * 2 + screen.selectionHeight() - screen.height) > 1)
                    throw new AssertionError("Country overview must be centered at every supported viewport");
                if (size[0] == 1600 && Math.abs(screen.panelWidth() * client.getWindow().getGuiScale() - 726) > 3)
                    throw new AssertionError("Original 726-pixel width must not multiply with GUI scale");
                if (screen.children().size() != 1 || screen.allowsMovement())
                    throw new AssertionError("Selection contains only the dismiss control, with no gameplay actions");
                for (var child : screen.children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget
                        && !widget.getMessage().getString().equals("×")) throw new AssertionError("Unexpected selection control");
                var indicators = HoiMenuBar.indicators(view.hud());
                if (indicators != HoiMenuBar.indicators(view.hud())) throw new AssertionError("Unchanged HUD must reuse formatted indicators");
                HoiMenuBar.clear();
                if (indicators == HoiMenuBar.indicators(view.hud())) throw new AssertionError("Resource reload must invalidate HUD presentation caches");
            });
            context.takeScreenshot("hoi-country-selection-" + size[0] + "-gui" + size[2]);
        }
        context.setScreen(() -> null);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.getInput().resizeWindow(1600, 1000);
    }
}
