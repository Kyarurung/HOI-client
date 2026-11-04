package dev.hoi.client.ui;

import dev.hoi.protocol.menu.MenuView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;

public final class UiPoliticalTooltip {
    private UiPoliticalTooltip() {}
    private record UiTooltipRow(net.minecraft.util.FormattedCharSequence text, String image, int height, boolean caption) {
        UiTooltipRow(net.minecraft.util.FormattedCharSequence text,String image,int height) {this(text,image,height,false);}
    }
    public static int draw(GuiGraphicsExtractor g, Font font, MenuView.Entry entry,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return draw(g, font, entry, "", "", mouseX, mouseY, viewportWidth, viewportHeight, scroll);
    }
    public static int drawLeader(GuiGraphicsExtractor g, Font font, MenuView.Entry entry, String country, String countryName,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return draw(g, font, entry, "country/" + country.toLowerCase(java.util.Locale.ROOT) + "/flag", countryName,
                mouseX, mouseY, viewportWidth, viewportHeight, scroll);
    }
    public static int drawCountryDetail(GuiGraphicsExtractor g, Font font, MenuView.Entry entry, String country,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return draw(g, font, entry, "country/" + country.toLowerCase(java.util.Locale.ROOT) + "/flag", "",
                mouseX, mouseY, viewportWidth, viewportHeight, scroll, true, false);
    }
    public static int drawDetail(GuiGraphicsExtractor g, Font font, String detail,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return draw(g, font, new MenuView.Entry("", "", detail, -1, ""), "", "",
                mouseX, mouseY, viewportWidth, viewportHeight, scroll, true, true);
    }
    public static int drawTextDetail(GuiGraphicsExtractor g, Font font, String detail,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return draw(g, font, new MenuView.Entry("", "", detail, -1, ""), "", "",
                mouseX, mouseY, viewportWidth, viewportHeight, scroll, true, false);
    }
    private static int draw(GuiGraphicsExtractor g, Font font, MenuView.Entry entry, String flag, String countryName,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll) {
        return draw(g, font, entry, flag, countryName, mouseX, mouseY, viewportWidth, viewportHeight, scroll, false, true);
    }
    private static int draw(GuiGraphicsExtractor g, Font font, MenuView.Entry entry, String flag, String countryName,
            int mouseX, int mouseY, int viewportWidth, int viewportHeight, int scroll, boolean compact, boolean icons) {
        entry = new MenuView.Entry(entry.name(), entry.value(), UiTooltipText.sourceSpacing(UiCommandLabels.withoutShortcuts(entry.detail())), entry.progress(), entry.icon());
        int width = Math.max(80, Math.min(270, viewportWidth - 24));
        if (compact) {
            int measured = 1;
            for (String line : entry.detail().split("\\R")) {
                if(line.startsWith("[caption:"))continue;
                var value = UiTooltips.sentences(Component.literal(line));
                measured = Math.max(measured, line.startsWith("[image:") ? 170
                        : font.width(icons ? UiKeywordIcons.decorateLines(value) : value));
            }
            width = Math.min(width, measured + (flag.isEmpty() ? 0 : 15));
        }
        String detail = flag.isEmpty() ? entry.detail() : entry.detail().strip();
        var rows = new ArrayList<UiTooltipRow>();
        if (!compact) rows.add(new UiTooltipRow(Component.literal(entry.value().isBlank() ? entry.name() : entry.value())
                .withColor(UiTooltips.GOLD).getVisualOrderText(), "", font.lineHeight + 3));
        if (!flag.isEmpty() && compact) {
            var lines = UiTooltips.split(font,Component.literal(detail), width - 15);
            for (int i = 0; i < lines.size(); i++) rows.add(new UiTooltipRow(lines.get(i), i == 0 ? flag : "", font.lineHeight + 1));
            detail = "";
        } else if (!flag.isEmpty()) {
            var role = Component.literal(countryName).withColor(UiTooltips.GOLD)
                    .append(Component.literal("의 국가원수").withStyle(net.minecraft.ChatFormatting.WHITE));
            var roleLines = UiTooltips.split(font,role, width - 15);
            for (int i = 0; i < roleLines.size(); i++) rows.add(new UiTooltipRow(roleLines.get(i), i == 0 ? flag : "", font.lineHeight + 1));
            if (!detail.isEmpty()) rows.add(new UiTooltipRow(Component.empty().getVisualOrderText(), "", font.lineHeight + 1));
        }
        for (String line : !flag.isEmpty() && detail.isEmpty() ? java.util.List.<String>of() : UiTooltipText.colorLines(detail)) {
            if(line.startsWith("[caption:")&&line.endsWith("]")&&!rows.isEmpty()&&rows.getLast().text()==null) {
                var previous=rows.removeLast();
                var caption=UiTooltips.styled(Component.literal(line.substring(9,line.length()-1)),false).getVisualOrderText();
                rows.add(new UiTooltipRow(caption,previous.image(),previous.height(),true));
            } else if (line.matches("\\[image:[a-z0-9_/]+\\]")) {
                String image = line.substring(7, line.length() - 1);
                int imageHeight = UiAssets.imageHeight(image, width);
                if (imageHeight > 0) rows.add(new UiTooltipRow(null, image, imageHeight));
            } else if(UiTooltipText.blank(line))rows.add(new UiTooltipRow(net.minecraft.util.FormattedCharSequence.EMPTY,"",font.lineHeight+1));
            else for (var text : UiTooltips.formattedLines(font, line, width + 24, icons))
                rows.add(new UiTooltipRow(text, "", font.lineHeight + 1));
        }
        rows=new ArrayList<>(UiTooltips.collapseBlankLines(rows,row->row.image().isEmpty()&&row.text()!=null&&UiTooltips.blank(row.text())));
        int imageWidth = width;
        width = Math.min(width, Math.max(1, rows.stream().mapToInt(row -> row.image().isEmpty() ? font.width(row.text())
                : row.text() != null && !row.caption() ? 15 + font.width(row.text()) : imageWidth).max().orElse(1)));
        int total = rows.stream().mapToInt(UiTooltipRow::height).sum();
        int height = Math.min(total, viewportHeight - 30);
        scroll = Math.clamp(scroll, 0, Math.max(0, (total - height + 9) / 10));
        int x = Math.clamp(mouseX + 12, 5, Math.max(5, viewportWidth - width - 10));
        int y = Math.clamp(mouseY - 12, 5, Math.max(5, viewportHeight - height - 10));
        g.nextStratum();
        g.fill(x-4, y-4, x+width+4, y+height+4, 0xFF9A45AB);
        g.fill(x-3, y-3, x+width+3, y+height+3, 0xF0100914);
        g.enableScissor(x, y, x+width, y+height);
        int top = y - scroll * 10;
        for (var row : rows) {
            if (top + row.height() > y && top < y + height) {
                if (row.image().isEmpty()) g.text(font, row.text(), x, top, 0xFFE0E0E0);
                else if(row.caption()) {
                    UiAssets.draw(g,row.image(),x,top,width,row.height());
                    float nameplateLeft=x+width*11f/172f, nameplateRight=x+width*161f/172f;
                    float nameplateTop=top+row.height()*224f/258f, nameplateBottom=top+row.height()*247f/258f;
                    float scale=Math.min(1.4f,(nameplateRight-nameplateLeft-8)/Math.max(1,font.width(row.text())));
                    g.pose().pushMatrix();
                    g.pose().translate((nameplateLeft+nameplateRight-font.width(row.text())*scale)/2,
                            (nameplateTop+nameplateBottom-font.lineHeight*scale)/2);
                    g.pose().scale(scale);
                    g.text(font,row.text(),0,0,0xFFFFFFFF);
                    g.pose().popMatrix();
                } else if (row.text() != null) {
                    UiAssets.draw(g, row.image(), x, top, 12, font.lineHeight);
                    g.text(font, row.text(), x + 15, top, 0xFFE0E0E0);
                } else UiAssets.draw(g, row.image(), x, top, width, row.height());
            }
            top += row.height();
        }
        g.disableScissor();
        return scroll;
    }
}
