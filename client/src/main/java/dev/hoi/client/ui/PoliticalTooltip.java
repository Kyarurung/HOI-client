package dev.hoi.client.ui;

import dev.hoi.protocol.MenuView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;

/** Scrollable original description images and text in one bounded tooltip. */
public final class PoliticalTooltip {
    private PoliticalTooltip() {}
    private record Row(net.minecraft.util.FormattedCharSequence text, String image, int height) {}
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
        entry = new MenuView.Entry(entry.name(), entry.value(), TooltipText.format(entry.detail()), entry.progress(), entry.icon());
        int width = Math.max(80, Math.min(270, viewportWidth - 24));
        if (compact) {
            int measured = 80;
            for (String line : entry.detail().split("\\R")) {
                var value = HoiTooltips.sentences(Component.literal(line));
                measured = Math.max(measured, line.startsWith("[image:") ? 170
                        : font.width(icons ? KeywordIcons.decorate(value) : value));
            }
            width = Math.min(width, measured + (flag.isEmpty() ? 0 : 15));
        }
        String detail = flag.isEmpty() ? entry.detail() : entry.detail().strip();
        var rows = new ArrayList<Row>();
        if (!compact) rows.add(new Row(Component.literal(entry.value().isBlank() ? entry.name() : entry.value())
                .withColor(HoiTooltips.GOLD).getVisualOrderText(), "", font.lineHeight + 3));
        if (!flag.isEmpty() && compact) {
            var lines = font.split(Component.literal(detail), width - 15);
            for (int i = 0; i < lines.size(); i++) rows.add(new Row(lines.get(i), i == 0 ? flag : "", font.lineHeight + 1));
            detail = "";
        } else if (!flag.isEmpty()) {
            var role = Component.literal(countryName).withColor(HoiTooltips.GOLD)
                    .append(Component.literal("의 국가원수").withStyle(net.minecraft.ChatFormatting.WHITE));
            var roleLines = font.split(role, width - 15);
            for (int i = 0; i < roleLines.size(); i++) rows.add(new Row(roleLines.get(i), i == 0 ? flag : "", font.lineHeight + 1));
            if (!detail.isEmpty()) rows.add(new Row(Component.empty().getVisualOrderText(), "", font.lineHeight + 1));
        }
        for (String line : !flag.isEmpty() && detail.isEmpty() ? new String[0] : detail.split("\\R", -1)) {
            if (line.matches("\\[image:[a-z0-9_/]+\\]")) {
                String image = line.substring(7, line.length() - 1);
                int imageHeight = UiAssets.imageHeight(image, width);
                if (imageHeight > 0) rows.add(new Row(null, image, imageHeight));
            } else for (var text : HoiTooltips.formattedLines(font, line, width + 24, icons))
                rows.add(new Row(text, "", font.lineHeight + 1));
        }
        int total = rows.stream().mapToInt(Row::height).sum();
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
                else if (row.text() != null) {
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
