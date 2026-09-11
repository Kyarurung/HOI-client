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
        int width = Math.max(80, Math.min(270, viewportWidth - 24));
        var rows = new ArrayList<Row>();
        rows.add(new Row(Component.literal(entry.value().isBlank() ? entry.name() : entry.value())
                .withStyle(net.minecraft.ChatFormatting.GOLD).getVisualOrderText(), "", font.lineHeight + 3));
        for (String line : entry.detail().split("\\R", -1)) {
            if (line.matches("\\[image:[a-z0-9_/]+\\]"))
                rows.add(new Row(null, line.substring(7, line.length()-1), width * 9 / 16));
            else for (var text : HoiTooltips.lines(font, line, width + 24))
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
                else UiAssets.draw(g, row.image(), x, top, width, row.height());
            }
            top += row.height();
        }
        g.disableScissor();
        return scroll;
    }
}
