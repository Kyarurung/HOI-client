package dev.hoi.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;


public final class HoiTooltips {
    private HoiTooltips() {}

    public static List<FormattedCharSequence> lines(Font font, String text, int viewportWidth) {
        int width = Math.max(80, Math.min(300, viewportWidth - 24));
        var lines = new ArrayList<FormattedCharSequence>();
        for (String line : text.split("\\R", -1)) {
            if (line.isEmpty()) lines.add(Component.empty().getVisualOrderText());
            else lines.addAll(font.split(KeywordIcons.decorate(Component.literal(line.replaceAll("£[A-Za-z0-9_]+\\s*", ""))), width));
        }
        return List.copyOf(lines);
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        graphics.setTooltipForNextFrame(font, lines(font, text, graphics.guiWidth()), x, y);
    }
}
