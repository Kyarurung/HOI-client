package dev.hoi.client.ui.country;

import dev.hoi.client.ui.UiAssets;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CountryFlags {
    private CountryFlags() {}
    public static boolean draw(GuiGraphicsExtractor g, String path, String style, int x, int y, int w, int h) {
        if (!java.util.Set.of("small2", "small", "smallest").contains(style)) throw new IllegalArgumentException("Unknown flag style");
        return UiAssets.draw(g, path, x, y, w, h);
    }
}
