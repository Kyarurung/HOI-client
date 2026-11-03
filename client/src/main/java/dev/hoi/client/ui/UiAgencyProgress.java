package dev.hoi.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class UiAgencyProgress {
    private UiAgencyProgress() {}
    public static void draw(GuiGraphicsExtractor g, int x, int y, double progress) {
        int size = 25;
        UiAssets.draw(g, "agency/map/progress_background/frame_0", x, y, size, size);
        var texture = Identifier.parse("hoi:textures/gui/agency/map/progress_fill/frame_0.png");
        for (int row = 0; row < size; row++) {
            int start = -1;
            for (int col = 0; col <= size; col++) {
                double angle = col == size ? 0 : (Math.atan2(col + .5 - size / 2.0, size / 2.0 - row - .5) + Math.PI * 2) % (Math.PI * 2);
                boolean fill = col < size && angle < Math.clamp(progress, 0, 1) * Math.PI * 2;
                if (fill && start < 0) start = col;
                if (!fill && start >= 0) {
                    g.blit(texture, x + start, y + row, x + col, y + row + 1,
                            start / (float)size, col / (float)size, row / (float)size, (row + 1) / (float)size);
                    start = -1;
                }
            }
        }
    }
}
