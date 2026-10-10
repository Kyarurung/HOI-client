package dev.hoi.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class UiTooltips {
    public static final int GOLD = 0xFFBD00, GOOD = 0x009F03, BAD = 0xFF3232;
    private record LayoutKey(Font font,String text,int width,boolean icons,boolean inferIcons) {}
    private static final java.util.LinkedHashMap<LayoutKey,List<FormattedCharSequence>> LAYOUTS = new java.util.LinkedHashMap<>(64,.75f,true);
    private static final java.util.regex.Pattern SENTENCES = java.util.regex.Pattern.compile("([.!?。！？](?:§.)*)\\h+(?=\\S)");
    private UiTooltips() {}
    public static boolean contains(net.minecraft.client.gui.components.AbstractWidget widget,double x,double y) {
        return widget.visible && x>=widget.getX() && y>=widget.getY()
                && x<widget.getX()+widget.getWidth() && y<widget.getY()+widget.getHeight();
    }
    public static void clear() {LAYOUTS.clear();}
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
        return SENTENCES.matcher(text).replaceAll("$1\n");
    }
    public static Component sentences(Component text) {
        return styled(text,true);
    }
    public static Component styled(Component text,boolean wrapSentences) {
        var result = Component.empty();
        text.visit((style, part) -> {
            String prose = wrapSentences?sentences(part):part;
            var codes = UiTooltipText.COLORS.matcher(prose);
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
        return net.minecraft.client.gui.components.Tooltip.create(UiKeywordIcons.decorateLines(styled(format(text),false)));
    }
    public static net.minecraft.client.gui.components.Tooltip createExact(String text) {
        return net.minecraft.client.gui.components.Tooltip.create(UiKeywordIcons.decorateLines(styled(Component.literal(UiTooltipText.sourceSpacing(text)),false),false));
    }
    public static net.minecraft.client.gui.components.Tooltip createOfficer(String text) {
        return createExact(officerCodes(text));
    }
    public record ScrolledTooltip(int x, int y, int width, int height, int scroll, int maxScroll) {}
    public static ScrolledTooltip drawOfficerScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return drawOfficerScrollable(graphics,font,text,mouseX,mouseY,viewportWidth,viewportHeight,scroll,324);
    }
    public static ScrolledTooltip drawOfficerScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll, int maxWidth) {
        return drawOfficerScrollable(graphics,font,text,mouseX,mouseY,viewportWidth,viewportHeight,scroll,maxWidth,false);
    }
    public static ScrolledTooltip drawSourceScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return drawSourceScrollable(graphics,font,text,mouseX,mouseY,viewportWidth,viewportHeight,scroll,324);
    }
    public static ScrolledTooltip drawSourceScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll, int maxWidth) {
        return drawOfficerScrollable(graphics,font,text,mouseX,mouseY,viewportWidth,viewportHeight,scroll,maxWidth,true);
    }
    private static ScrolledTooltip drawOfficerScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll, int maxWidth, boolean sourceLayout) {
        String content = officerCodes(text);
        var lines = exactLines(font, sourceLayout ? content : content.replaceAll("(?m)^-{3,}$", "\u200B"), Math.min(viewportWidth, maxWidth));
        int width = Math.min(Math.max(1, viewportWidth - 20), Math.max(80, lines.stream().mapToInt(font::width).max().orElse(80)));
        int lineHeight = font.lineHeight + 1;
        int total = lines.size() * lineHeight;
        int height = Math.min(total, Math.max(1, viewportHeight - 20));
        int maxScroll = Math.max(0, total - height);
        scroll = Math.clamp(scroll, 0, maxScroll);
        int x = Math.clamp(mouseX + 12, 5, Math.max(5, viewportWidth - width - 5));
        int y = Math.clamp(mouseY - 12, 5, Math.max(5, viewportHeight - height - 5));
        graphics.nextStratum();
        graphics.fill(x - 4, y - 4, x + width + 4, y + height + 4, 0xFF9A45AB);
        graphics.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0xF0100914);
        graphics.enableScissor(x, y, x + width, y + height);
        for (int i = Math.max(0, scroll / lineHeight); i < lines.size(); i++) {
            int top = y + i * lineHeight - scroll;
            if (top >= y + height) break;
            var separator=new boolean[1];
            lines.get(i).accept((index,style,codepoint)->{if(codepoint==0x200B)separator[0]=true;return true;});
            if(!sourceLayout && separator[0]) {
                for(int offset=0;offset<width;offset+=4)graphics.fill(x+offset,top+lineHeight/2,x+Math.min(width,offset+2),top+lineHeight/2+1,0xFF78828F);
            } else graphics.text(font, lines.get(i), sourceLayout ? x : x + (width - font.width(lines.get(i))) / 2, top, 0xFFE0E0E0);
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int thumb = Math.max(8, height * height / total);
            int thumbY = y + (height - thumb) * scroll / maxScroll;
            graphics.fill(x + width + 1, thumbY, x + width + 3, thumbY + thumb, GOLD | 0xFF000000);
        }
        return new ScrolledTooltip(x, y, width, height, scroll, maxScroll);
    }
    public static List<FormattedCharSequence> sourceLines(Font font,String text,int viewportWidth){return exactLines(font,officerCodes(text),viewportWidth);}
    public static Component officerText(String text) {
        return styled(Component.literal(officerCodes(text)), false);
    }
    private static String officerCodes(String text) {
        return text.replace("§g", "§#8893A5").replace("§G", "§a").replace("§R", "§4")
                .replace("§Y", "§e").replace("§H", "§e").replace("§W", "§f")
                .replace("§B", "§9").replace("§C", "§b").replace("§O", "§6").replace("§!", "§r");
    }
    public static Component productionText(Component text) {
        return UiKeywordIcons.decorateLines(sentences(text));
    }
    public static void background(GuiGraphicsExtractor graphics,int x,int y,int width,int height) {
        UiAssets.nineSlice(graphics,"production/tooltip",x,y,width,height,6,.5);
    }
    public static void drawExact(GuiGraphicsExtractor graphics,Font font,String text,int x,int y) {
        graphics.setTooltipForNextFrame(font,exactLines(font,text,graphics.guiWidth()),x,y);
    }
    public static void drawExact(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int maxWidth) {
        graphics.setTooltipForNextFrame(font, exactLines(font, text, Math.min(graphics.guiWidth(), maxWidth + 24)), x, y);
    }
    public static List<FormattedCharSequence> exactLines(Font font,String text,int viewportWidth) {
        return formattedLines(font,text,viewportWidth,true,false);
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
        boolean space = false, added = false;
        for (var row : rows) {
            if (UiTooltipText.blank(row.getString())) { space = added; continue; }
            if (!UiTooltipText.visible(row.getString())) continue;
            if (added) result.append(space ? "\n\n" : "\n");
            result.append(row); space = false; added = true;
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
        return formattedLines(font,text,viewportWidth,icons,true);
    }
    private static List<FormattedCharSequence> formattedLines(Font font,String text,int viewportWidth,boolean icons,boolean inferIcons) {
        int width = Math.max(80, Math.min(300, viewportWidth - 24));
        var key = new LayoutKey(font,text,width,icons,inferIcons);
        var cached = LAYOUTS.get(key);
        if(cached!=null)return cached;
        var lines = new ArrayList<FormattedCharSequence>();
        for (String line : UiTooltipText.colorLines(UiTooltipText.sourceSpacing(text))) {
            if (line.isEmpty()) lines.add(Component.empty().getVisualOrderText());
            else {
                var value = styled(Component.literal(line),false);
                lines.addAll(font.split(icons ? UiKeywordIcons.decorateLines(value,inferIcons) : value, width));
            }
        }
        var result = List.copyOf(lines);
        if(text.length()<=16384) {
            if(LAYOUTS.size()>=64)LAYOUTS.pollFirstEntry();
            LAYOUTS.put(key,result);
        }
        return result;
    }
    public static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        graphics.setTooltipForNextFrame(font, lines(font, text, graphics.guiWidth()), x, y);
    }
}
