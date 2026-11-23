package dev.hoi.client.ui.tooltip;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiCommandLabels;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class Tooltips {
    public static final int GOLD = 0xFFBD00, GOOD = 0x009F03, BAD = 0xFF3232, MAX_WIDTH = 300;
    private record LayoutKey(Font font,String text,int width,boolean icons,boolean inferIcons) {}
    private static final java.util.LinkedHashMap<LayoutKey,List<FormattedCharSequence>> LAYOUTS = new java.util.LinkedHashMap<>(64,.75f,true);
    private static final java.util.regex.Pattern SENTENCES = java.util.regex.Pattern.compile("([.!?。！？](?:§.)*)\\h+(?=\\S)");
    private Tooltips() {}
    public static boolean contains(net.minecraft.client.gui.components.AbstractWidget widget,double x,double y) {
        return widget.visible && x>=widget.getX() && y>=widget.getY()
                && x<widget.getX()+widget.getWidth() && y<widget.getY()+widget.getHeight();
    }
    public static void clear() {LAYOUTS.clear();}
    public static org.joml.Vector2ic position(int width,int height,int mouseX,int mouseY,int textWidth,int textHeight) {
        return new org.joml.Vector2i(Math.clamp(mouseX+11,7,Math.max(7,width-textWidth-7)),Math.clamp(mouseY+14,7,Math.max(7,height-textHeight-7)));
    }
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
            var codes = TooltipText.COLORS.matcher(prose);
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
        return result.getString().contains("£")?TooltipKeywordIcons.decorateLines(result,false):result;
    }
    public static net.minecraft.client.gui.components.Tooltip create(Component text) {
        String plain = text.getString();
        if (TooltipDelay.delayed(plain)) return TooltipDelay.pair(create(Component.literal(TooltipDelay.summary(plain))), create(Component.literal(TooltipDelay.full(plain))));
        return net.minecraft.client.gui.components.Tooltip.create(TooltipKeywordIcons.decorateLines(styled(format(sourceSpacing(text,true)),false)));
    }
    public static net.minecraft.client.gui.components.Tooltip createExact(String text) {
        if (TooltipDelay.delayed(text)) return TooltipDelay.pair(createExact(TooltipDelay.summary(text)), createExact(TooltipDelay.full(text)));
        return net.minecraft.client.gui.components.Tooltip.create(TooltipKeywordIcons.decorateLines(styled(sourceSpacing(Component.literal(text),true),false),false));
    }
    public static net.minecraft.client.gui.components.Tooltip createOfficer(String text) {
        return createExact(officerCodes(text));
    }
    public static net.minecraft.client.gui.components.Tooltip delayed(Component summary, Component full) {
        return TooltipDelay.pair(create(summary), create(full));
    }
    public static int width(int viewportWidth, int horizontalPadding) {
        return Math.max(1, Math.min(MAX_WIDTH, viewportWidth - 10) - horizontalPadding);
    }
    public static List<FormattedCharSequence> tooltipLines(Font font, String text, int viewportWidth) {
        return sourceLines(font, text, width(viewportWidth, 14) + 24);
    }
    public record ScrolledTooltip(int x, int y, int width, int height, int scroll, int maxScroll) {}
    public static ScrolledTooltip drawOfficerScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return drawSourceScrollable(graphics,font,text,mouseX,mouseY,viewportWidth,viewportHeight,scroll);
    }
    public static ScrolledTooltip drawSourceScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return drawSourceScrollable(graphics,font,text,mouseX,mouseY,viewportWidth,viewportHeight,scroll,false,false);
    }
    public static ScrolledTooltip drawSourceScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll, boolean gradient) {
        return drawSourceScrollable(graphics,font,text,mouseX,mouseY,viewportWidth,viewportHeight,scroll,gradient,gradient);
    }
    public static ScrolledTooltip drawSourceScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll, boolean gradient, boolean inferIcons) {
        return drawSourceScrollable(graphics,font,text,mouseX,mouseY,viewportWidth,viewportHeight,scroll,gradient,inferIcons,false);
    }
    private static ScrolledTooltip drawSourceScrollable(GuiGraphicsExtractor graphics, Font font, String text,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll, boolean gradient, boolean inferIcons, boolean above) {
        String content = officerCodes(TooltipDelay.visible(text));
        var lines = formattedLines(font, content, width(viewportWidth, 8) + 24, true, inferIcons);
        return drawLinesScrollable(graphics, font, lines, mouseX, mouseY, viewportWidth, viewportHeight, scroll, gradient, above);
    }
    public static ScrolledTooltip drawLinesScrollable(GuiGraphicsExtractor graphics, Font font, List<FormattedCharSequence> lines,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll, boolean gradient, boolean above) {
        int width = Math.min(Math.max(1, viewportWidth - 20), Math.max(1, lines.stream().mapToInt(font::width).max().orElse(1)));
        int lineHeight = font.lineHeight + 1;
        int total = lines.size() * lineHeight;
        int height = Math.min(total, Math.max(1, above ? mouseY - 9 : viewportHeight - 20));
        int maxScroll = Math.max(0, total - height);
        scroll = Math.clamp(scroll, 0, maxScroll);
        int x = Math.clamp(mouseX + (above ? 4 : 11), 5, Math.max(5, viewportWidth - width - 5));
        int y = Math.clamp(above ? mouseY - height - 4 : mouseY + 14, 5, Math.max(5, viewportHeight - height - 5));
        int offset=scroll;
        if(TooltipLayer.defer(graphics,()->drawLinesScrollable(graphics,font,lines,mouseX,mouseY,viewportWidth,viewportHeight,offset,gradient,above)))return new ScrolledTooltip(x,y,width,height,scroll,maxScroll);
        graphics.nextStratum();
        if(gradient)graphics.fillGradient(x-4,y-4,x+width+4,y+height+4,0xFF754CB3,0xFFD147A5);else graphics.fill(x - 4, y - 4, x + width + 4, y + height + 4, 0xFF9A45AB);
        graphics.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0xF0100914);
        graphics.enableScissor(x, y, x + width, y + height);
        for (int i = Math.max(0, scroll / lineHeight); i < lines.size(); i++) {
            int top = y + i * lineHeight - scroll;
            if (top >= y + height) break;
            graphics.text(font, lines.get(i), x, top, 0xFFE0E0E0, false);
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
    public static Component officerText(String text,boolean bold) {
        var result=Component.empty();
        officerText(text).visit((style,part)->{result.append(Component.literal(part).withStyle(style.withBold(bold&&!TooltipKeywordIcons.FONT.equals(style.getFont()))));return java.util.Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        return result;
    }
    public static Component panelText(String text) {
        var result = Component.empty();
        officerText(text).visit((style, part) -> {
            int color = style.getColor() == null ? -1 : style.getColor().getValue();
            result.append(Component.literal(part).withStyle(color == GOLD ? style.withColor(0xEEC923) : color == GOOD ? style.withColor(0x56AC5B) : color == BAD ? style.withColor(0xDE5646) : style));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return result;
    }
    private static String officerCodes(String text) {
        return text.replace("§g", "§#8893A5").replace("§G", "§a").replace("§R", "§4")
                .replace("§Y", "§e").replace("§H", "§e").replace("§W", "§f")
                .replace("§B", "§9").replace("§C", "§b").replace("§O", "§6").replace("§L", "").replace("§!", "§r");
    }
    public static Component productionText(Component text) {
        return TooltipKeywordIcons.decorateLines(sentences(text));
    }
    private static boolean sourceBlank(String value) { return TooltipText.blank(value); }
    public static Component sourceSpacing(net.minecraft.network.chat.FormattedText text) {
        return sourceSpacing(text,false);
    }
    private static Component sourceSpacing(net.minecraft.network.chat.FormattedText text,boolean trim) {
        var rows=new ArrayList<net.minecraft.network.chat.MutableComponent>();
        rows.add(Component.empty());
        text.visit((style,part)->{
            String[] parts=part.split("\\R",-1);
            for(int i=0;i<parts.length;i++) {
                if(i>0)rows.add(Component.empty());
                rows.getLast().append(Component.literal(parts[i]).withStyle(style));
            }
            return java.util.Optional.empty();
        },net.minecraft.network.chat.Style.EMPTY);
        var result=Component.empty();
        boolean first=true;
        var cleaned=new ArrayList<Component>();boolean removed=false;
        for(var row:rows) {
            var value=withoutShortcuts(row);
            if(value==null){while(!cleaned.isEmpty()&&sourceBlank(cleaned.getLast().getString()))cleaned.removeLast();removed=true;continue;}
            if(removed&&sourceBlank(value.getString()))continue;
            cleaned.add(value);removed=false;
        }
        var normalized=trim?collapseBlankLines(cleaned,line->sourceBlank(line.getString())):TooltipText.collapseBlankLines(cleaned,line->sourceBlank(line.getString()));
        for(var row:normalized) {
            if(!first)result.append("\n");
            if(!sourceBlank(row.getString()))result.append(row);
            first=false;
        }
        return result;
    }
    private static Component withoutShortcuts(Component row) {
        if(TooltipText.blank(row.getString()))return row;
        String original=row.getString(),cleaned=UiCommandLabels.withoutShortcuts(original);
        if(original.equals(cleaned))return row;
        if(cleaned.isEmpty())return null;
        int prefix=0,suffix=0;
        while(prefix<cleaned.length()&&prefix<original.length()&&cleaned.charAt(prefix)==original.charAt(prefix))prefix++;
        while(suffix<cleaned.length()-prefix&&suffix<original.length()-prefix&&cleaned.charAt(cleaned.length()-suffix-1)==original.charAt(original.length()-suffix-1))suffix++;
        int start=prefix,end=original.length()-suffix;
        String replacement=cleaned.substring(prefix,cleaned.length()-suffix);
        var result=Component.empty();int[] offset={0};
        row.visit((style,part)->{
            int from=offset[0],to=from+part.length();
            if(from<start)result.append(Component.literal(part.substring(0,Math.min(part.length(),start-from))).withStyle(style));
            if(from<=start&&start<to&&!replacement.isEmpty())result.append(Component.literal(replacement).withStyle(style));
            if(to>end)result.append(Component.literal(part.substring(Math.max(0,end-from))).withStyle(style));
            offset[0]=to;return java.util.Optional.empty();
        },net.minecraft.network.chat.Style.EMPTY);
        return result;
    }
    public static List<FormattedCharSequence> split(Font font,net.minecraft.network.chat.FormattedText text,int width) {
        return collapseBlankLines(font.split(TooltipKeywordIcons.decorateLines(sourceSpacing(text),false),width));
    }
    public static boolean blank(FormattedCharSequence line) {
        var text=new StringBuilder();
        line.accept((index,style,code)->{text.appendCodePoint(code);return true;});
        return sourceBlank(text.toString());
    }
    public static List<FormattedCharSequence> collapseBlankLines(List<FormattedCharSequence> lines) {
        return collapseBlankLines(lines,Tooltips::blank);
    }
    public static <T> List<T> collapseBlankLines(List<T> lines,java.util.function.Predicate<T> blank) {
        int first=0,last=lines.size();
        while(first<last&&blank.test(lines.get(first)))first++;
        while(last>first&&blank.test(lines.get(last-1)))last--;
        return TooltipText.collapseBlankLines(lines.subList(first,last),blank);
    }
    public static void background(GuiGraphicsExtractor graphics,int x,int y,int width,int height) {
        UiAssets.nineSlice(graphics,"production/tooltip",x,y,width,height,6,.5);
    }
    public static void drawExact(GuiGraphicsExtractor graphics,Font font,String text,int x,int y) {
        graphics.setTooltipForNextFrame(font,exactLines(font,TooltipDelay.visible(text),width(graphics.guiWidth(),14)+24),x,y);
    }
    public static int wideWidth(int viewportWidth) {
        return Math.max(1, Math.min(MAX_WIDTH, viewportWidth - 24));
    }
    public static void drawWide(GuiGraphicsExtractor graphics,Font font,String text,int x,int y) {
        graphics.setTooltipForNextFrame(font,sourceLines(font,TooltipDelay.visible(text),wideWidth(graphics.guiWidth())+24),x,y);
    }
    public static List<FormattedCharSequence> exactLines(Font font,String text,int viewportWidth) {
        return formattedLines(font,text,viewportWidth,true,false);
    }
    public static Component format(Component text) {
        var rows = new ArrayList<net.minecraft.network.chat.MutableComponent>();
        rows.add(Component.empty());
        boolean precise = TooltipText.precise(text.getString());
        text.visit((style, part) -> {
            String[] parts = part.split("\\R", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) rows.add(Component.empty());
                rows.getLast().append(Component.literal(precise ? parts[i] : TooltipText.numbers(parts[i])).withStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        var result = Component.empty();
        boolean space = false, added = false;
        for (var row : rows) {
            if (TooltipText.blank(row.getString())) { space = added; continue; }
            if (!TooltipText.visible(row.getString())) continue;
            if (added) result.append(space ? "\n\n" : "\n");
            result.append(row); space = false; added = true;
        }
        return result;
    }
    public static List<FormattedCharSequence> lines(Font font, String text, int viewportWidth) {
        return lines(font, text, viewportWidth, true);
    }
    public static List<FormattedCharSequence> lines(Font font, String text, int viewportWidth, boolean icons) {
        return formattedLines(font, TooltipText.format(text), viewportWidth, icons);
    }
    public static List<FormattedCharSequence> formattedLines(Font font, String text, int viewportWidth, boolean icons) {
        return formattedLines(font,text,viewportWidth,icons,true);
    }
    private static List<FormattedCharSequence> formattedLines(Font font,String text,int viewportWidth,boolean icons,boolean inferIcons) {
        text=UiCommandLabels.withoutShortcuts(text);
        int width = Math.max(80, Math.min(MAX_WIDTH, viewportWidth - 24));
        var key = new LayoutKey(font,text,width,icons,inferIcons);
        var cached = LAYOUTS.get(key);
        if(cached!=null)return cached;
        var lines = new ArrayList<FormattedCharSequence>();
        for (String line : TooltipText.colorLines(text)) {
            if (sourceBlank(line)) lines.add(Component.empty().getVisualOrderText());
            else {
                var value = styled(Component.literal(line),false);
                lines.addAll(balanced(font, icons ? TooltipKeywordIcons.decorateLines(value,inferIcons) : value, width));
            }
        }
        var result = collapseBlankLines(lines);
        if(text.length()<=16384) {
            if(LAYOUTS.size()>=64)LAYOUTS.pollFirstEntry();
            LAYOUTS.put(key,result);
        }
        return result;
    }
    static List<FormattedCharSequence> balanced(Font font,net.minecraft.network.chat.FormattedText text,int width) {
        var lines=font.split(text,width);
        if(lines.size()<2||font.width(lines.getLast())*4>=width)return lines;
        var best=lines;int low=Math.max(1,width/2),high=width;
        while(low<high){int middle=(low+high)/2;var trial=font.split(text,middle);if(trial.size()==lines.size()){best=trial;high=middle;}else low=middle+1;}
        return best;
    }
    public static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        graphics.setTooltipForNextFrame(font, lines(font, TooltipDelay.visible(text), width(graphics.guiWidth(),14)+24), x, y);
    }
}
