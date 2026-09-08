package dev.hoi.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Split explicit lines and wrap to the viewport before Minecraft converts text into tooltip components. */
final class HoiTooltips {
    private HoiTooltips() {}

    static List<FormattedCharSequence> lines(Font font, String text, int viewportWidth) {
        int width = Math.max(80, Math.min(300, viewportWidth - 24));
        var lines = new ArrayList<FormattedCharSequence>();
        for (String line : text.split("\\R", -1)) lines.addAll(font.split(Component.literal(line), width));
        return List.copyOf(lines);
    }

    static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        graphics.setTooltipForNextFrame(font, lines(font, text, graphics.guiWidth()), x, y);
    }
}
