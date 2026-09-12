package dev.hoi.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;


public final class HoiTooltips {
    // TFR interface/fonts.gfx: textcolors Y, G and R.
    public static final int GOLD = 0xEEC923, GOOD = 0x56AC5B, BAD = 0xDE5646;
    private HoiTooltips() {}

    private static net.minecraft.network.chat.Style sourceColor(net.minecraft.network.chat.Style style) {
        var color = style.getColor();
        if (color == null) return style;
        return switch (color.getValue()) {
            case 0xFFFF55, 0xFFAA00 -> style.withColor(GOLD);
            case 0x55FF55, 0x00AA00 -> style.withColor(GOOD);
            case 0xFF5555, 0xAA0000 -> style.withColor(BAD);
            default -> style;
        };
    }

    public static String sentences(String text) {
        return text.replaceAll("([.!?。！？](?:§.)*)\\h+(?=\\S)", "$1\n");
    }

    public static Component sentences(Component text) {
        var result = Component.empty();
        text.visit((style, part) -> {
            String prose = sentences(part);
            var codes = java.util.regex.Pattern.compile("(?i)§([0-9a-fk-or])").matcher(prose);
            var current = style;
            int start = 0;
            while (codes.find()) {
                if (start < codes.start()) result.append(Component.literal(prose.substring(start, codes.start())).withStyle(sourceColor(current)));
                var format = net.minecraft.ChatFormatting.getByCode(codes.group(1).toLowerCase(java.util.Locale.ROOT).charAt(0));
                current = format == net.minecraft.ChatFormatting.RESET ? style : current.applyLegacyFormat(format);
                start = codes.end();
            }
            if (start < prose.length()) result.append(Component.literal(prose.substring(start)).withStyle(sourceColor(current)));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return result;
    }

    public static net.minecraft.client.gui.components.Tooltip create(Component text) {
        return net.minecraft.client.gui.components.Tooltip.create(KeywordIcons.decorateLines(sentences(text)));
    }

    public static List<FormattedCharSequence> lines(Font font, String text, int viewportWidth) {
        int width = Math.max(80, Math.min(300, viewportWidth - 24));
        var lines = new ArrayList<FormattedCharSequence>();
        for (String line : sentences(text).split("\\R", -1)) {
            if (line.isEmpty()) lines.add(Component.empty().getVisualOrderText());
            else lines.addAll(font.split(KeywordIcons.decorate(sentences(Component.literal(line.replaceAll("£[A-Za-z0-9_]+\\s*", "")))), width));
        }
        return List.copyOf(lines);
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        graphics.setTooltipForNextFrame(font, lines(font, text, graphics.guiWidth()), x, y);
    }
}
