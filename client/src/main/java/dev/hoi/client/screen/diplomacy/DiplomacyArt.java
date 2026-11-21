package dev.hoi.client.screen.diplomacy;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.tooltip.TooltipKeywordIcons;
import dev.hoi.client.ui.tooltip.TooltipText;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.country.CountryView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

final class DiplomacyArt {
    record Row(FormattedCharSequence text, String image, int imageX, FormattedCharSequence caption) {}
    static final int PORTRAIT_WIDTH = 86, PORTRAIT_HEIGHT = 129;
    private static final Pattern PORTRAIT = Pattern.compile("£GFX_PRK_(?:cabinet_premier_(?:kim_jae_ryong|kim_tok_hun|pak_tae_song)|eternal_leader_(?:kim_il_sung|kim_jong_il))");
    private CountryView.Art hovered;
    private long enteredAt;
    private int scroll;
    static float[] bounds(CountryView.Art art, int pane, int top) {
        var size = UiAssets.imageSize(art.icon());
        return new float[]{(art.x() - 5) * pane / 550f, DiplomacyHeader.politicsTop(top) + (art.y() - 111) * pane / 550f, size[0] * pane / 1100f, size[1] * pane / 1100f};
    }
    static CountryView.Art at(CountryView view, int pane, int top, double mouseX, double mouseY) {
        for (var art : view.art()) {
            var box = bounds(art, pane, top);
            if (mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3]) return art;
        }
        return null;
    }
    void draw(GuiGraphicsExtractor g, CountryView view, int pane, int top) {
        for (var art : view.art()) {
            var box = bounds(art, pane, top);
            g.pose().pushMatrix();
            g.pose().translate(box[0], box[1]);
            g.pose().scale(pane / 1100f);
            var size = UiAssets.imageSize(art.icon());
            UiAssets.draw(g, art.icon(), 0, 0, size[0], size[1]);
            g.pose().popMatrix();
        }
    }
    void leave() {
        hovered = null;
        scroll = 0;
    }
    boolean scroll(CountryView view, int pane, int top, double x, double y, double amount) {
        if (at(view, pane, top, x, y) == null) return false;
        scroll = Math.max(0, scroll - (int)(amount * 30));
        return true;
    }
    static String text(CountryView.Art art, boolean delayed) {
        String text = art.title() + (delayed ? "\n\n" + art.tooltip() : "");
        return text.replace("§A", "§#CD3737").replace("§L", "§#A2B9CF").replace("§H", "§e").replace("§!", "§r");
    }
    static List<Row> rows(Font font, String text, int width) {
        var rows = new ArrayList<Row>();
        for (String line : TooltipText.colorLines(text)) {
            String plain = line.replaceAll("§(?:#[0-9a-fA-F]{6}|.)", "");
            int portrait = rows.size() - 1;
            while (portrait >= 0 && blank(rows.get(portrait))) portrait--;
            if (PORTRAIT.matcher(plain.strip()).matches()) {
                rows.add(new Row(FormattedCharSequence.EMPTY, "diplomacy/tfrkp/" + plain.strip().substring(1).toLowerCase(java.util.Locale.ROOT), font.width(plain.substring(0, plain.indexOf('£'))), null));
            } else if (TooltipText.blank(line)) rows.add(new Row(FormattedCharSequence.EMPTY, "", 0, null));
            else if (portrait >= 0 && !rows.get(portrait).image().isEmpty() && rows.get(portrait).caption() == null && plain.startsWith(" ")) {
                var image = rows.get(portrait);
                rows.subList(portrait, rows.size()).clear();
                rows.add(new Row(FormattedCharSequence.EMPTY, image.image(), image.imageX(), styled(line.replaceFirst("^((?:§(?:#[0-9a-fA-F]{6}|.))*)\\s+", "$1")).getVisualOrderText()));
            } else for (var wrapped : font.split(styled(line), width)) rows.add(new Row(wrapped, "", 0, null));
        }
        return Tooltips.collapseBlankLines(rows, DiplomacyArt::blank);
    }
    static int height(Row row, int lineHeight) {
        return row.image().isEmpty() ? lineHeight : PORTRAIT_HEIGHT;
    }
    static int captionY(Font font) {
        return Math.round(PORTRAIT_HEIGHT * 235f / 258) - font.lineHeight / 2;
    }
    private static boolean blank(Row row) {
        return row.image().isEmpty() && Tooltips.blank(row.text());
    }
    private static Component styled(String line) {
        return TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal(line.replaceAll("(£[A-Za-z0-9_]+) ", "$1£")), false), false);
    }
    void tooltip(GuiGraphicsExtractor g, Font font, CountryView view, int pane, int top, int mx, int my) {
        if(dev.hoi.client.ui.tooltip.TooltipLayer.defer(g,()->tooltip(g,font,view,pane,top,mx,my)))return;
        var art = at(view, pane, top, mx, my);
        if (!Objects.equals(hovered, art)) {
            hovered = art;
            enteredAt = Util.getMillis();
            scroll = 0;
        }
        if (art == null || Util.getMillis() - enteredAt < 50) return;
        int width = Tooltips.width(g.guiWidth(), 14), lineHeight = font.lineHeight + 1;
        var rows = rows(font, text(art, Util.getMillis() - enteredAt >= 1000), width);
        int total = rows.stream().mapToInt(row -> height(row, lineHeight)).sum(), height = Math.min(total, g.guiHeight() - 20);
        scroll = Math.clamp(scroll, 0, total - height);
        var position = Tooltips.position(g.guiWidth(), g.guiHeight(), mx, my, width, height);
        int x = position.x(), y = position.y(), rowY = y - scroll;
        g.nextStratum();
        Tooltips.background(g, x - 4, y - 4, width + 8, height + 8);
        g.enableScissor(x, y, x + width, y + height);
        for (var row : rows) {
            if (row.image().isEmpty()) g.text(font, row.text(), x, rowY, 0xFFE0E0E0);
            else {
                UiAssets.draw(g, row.image(), x + row.imageX(), rowY, PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
                if (row.caption() != null) g.text(font, row.caption(), x + row.imageX() + (PORTRAIT_WIDTH - font.width(row.caption())) / 2, rowY + captionY(font), 0xFFFFFFFF);
            }
            rowY += height(row, lineHeight);
        }
        g.disableScissor();
    }
}
