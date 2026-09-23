package dev.hoi.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;


public final class UiTooltips {
    public static final int GOLD = 0xFFBD00, GOOD = 0x009F03, BAD = 0xFF3232;
    private UiTooltips() {}

    private static net.minecraft.network.chat.Style sourceColor(net.minecraft.network.chat.Style style) {
        var color = style.getColor();
        if (color == null) return style;
        return switch (color.getValue()) {
            case 0xFFFF55, 0xFFAA00, 0xEEC923 -> style.withColor(GOLD);
            case 0x55FF55, 0x00AA00, 0x56AC5B -> style.withColor(GOOD);
            case 0xFF5555, 0xAA0000, 0xDE5646 -> style.withColor(BAD);
            default -> style;
        };
    }

    public static String sentences(String text) {
        return text.replaceAll("([.!?。！？](?:§.)*)\\h+(?=\\S)", "$1\n");
    }

    public static Component sentences(Component text) {
        return styled(text,true);
    }

    public static Component styled(Component text,boolean wrapSentences) {
        var result = Component.empty();
        text.visit((style, part) -> {
            String prose = wrapSentences?sentences(part):part;
            var codes = java.util.regex.Pattern.compile("(?i)§(#[0-9a-f]{6}|[0-9a-fk-or])").matcher(prose);
            var current = style;
            int start = 0;
            while (codes.find()) {
                if (start < codes.start()) result.append(Component.literal(prose.substring(start, codes.start())).withStyle(sourceColor(current)));
                if(codes.group(1).startsWith("#"))current=current.withColor(Integer.parseInt(codes.group(1).substring(1),16));
                else {
                    var format = net.minecraft.ChatFormatting.getByCode(codes.group(1).toLowerCase(java.util.Locale.ROOT).charAt(0));
                    current = format == net.minecraft.ChatFormatting.RESET ? style : current.applyLegacyFormat(format);
                }
                start = codes.end();
            }
            if (start < prose.length()) result.append(Component.literal(prose.substring(start)).withStyle(sourceColor(current)));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return result;
    }

    public static net.minecraft.client.gui.components.Tooltip create(Component text) {
        return net.minecraft.client.gui.components.Tooltip.create(UiKeywordIcons.decorateLines(sentences(format(text))));
    }

    public static Component productionText(Component text) {
        return UiKeywordIcons.decorateLines(sentences(text));
    }

    public static void background(GuiGraphicsExtractor graphics,int x,int y,int width,int height) {
        UiAssets.nineSlice(graphics,"production/tooltip",x,y,width,height,6,.5);
    }

    public static void drawExact(GuiGraphicsExtractor graphics,Font font,String text,int x,int y) {
        graphics.setTooltipForNextFrame(font,formattedLines(font,text,graphics.guiWidth(),true),x,y);
    }
    public static Component format(Component text) {
        var rows = new ArrayList<net.minecraft.network.chat.MutableComponent>();
        rows.add(Component.empty());
        boolean precise = UiTooltipText.precise(text.getString());
        text.visit((style, part) -> {
            String[] parts = part.split("\\R", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) rows.add(Component.empty());
                rows.getLast().append(Component.literal(precise ? parts[i] : UiTooltipText.numbers(parts[i])).withStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        var result = Component.empty();
        boolean space = false;
        for (var row : rows) {
            if (UiTooltipText.blank(row.getString())) { space = !result.getString().isEmpty(); continue; }
            if (!UiTooltipText.visible(row.getString())) continue;
            if (!result.getString().isEmpty()) result.append(space ? "\n\n" : "\n");
            result.append(row); space = false;
        }
        return result;
    }

    public static List<FormattedCharSequence> lines(Font font, String text, int viewportWidth) {
        return lines(font, text, viewportWidth, true);
    }

    public static List<FormattedCharSequence> lines(Font font, String text, int viewportWidth, boolean icons) {
        return formattedLines(font, UiTooltipText.format(text), viewportWidth, icons);
    }

    static List<FormattedCharSequence> formattedLines(Font font, String text, int viewportWidth, boolean icons) {
        int width = Math.max(80, Math.min(300, viewportWidth - 24));
        var lines = new ArrayList<FormattedCharSequence>();
        for (String line : UiTooltipText.spacing(sentences(text)).split("\\R", -1)) {
            if (line.isEmpty()) lines.add(Component.empty().getVisualOrderText());
            else {
                var value = sentences(Component.literal(line));
                lines.addAll(font.split(icons ? UiKeywordIcons.decorateLines(value) : value, width));
            }
        }
        return List.copyOf(lines);
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        graphics.setTooltipForNextFrame(font, lines(font, text, graphics.guiWidth()), x, y);
    }
}
