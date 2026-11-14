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
import java.util.Objects;

final class DiplomacyArt {
    private record Row(FormattedCharSequence text, String image, int imageX) {}
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
        scroll = Math.max(0, scroll - (int)(amount * 3));
        return true;
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
        String text = art.title() + (Util.getMillis() - enteredAt >= 1000 ? "\n\n" + art.tooltip() : "");
        text = text.replace("§A", "§#CD3737").replace("§L", "§#A2B9CF").replace("§H", "§e").replace("§!", "§r");
        int width = Tooltips.width(g.guiWidth(), 14), lineHeight = font.lineHeight + 1;
        var rows = new ArrayList<Row>();
        for (String line : TooltipText.colorLines(text)) {
            String plain = line.replaceAll("§(?:#[0-9a-fA-F]{6}|.)", "");
            if (plain.strip().matches("£GFX_PRK_(?:cabinet_premier_(?:kim_jae_ryong|kim_tok_hun|pak_tae_song)|eternal_leader_(?:kim_il_sung|kim_jong_il))")) {
                int marker = plain.indexOf('£');
                rows.add(new Row(FormattedCharSequence.EMPTY, "diplomacy/tfrkp/" + plain.strip().substring(1).toLowerCase(java.util.Locale.ROOT), font.width(plain.substring(0, marker))));
            } else if (line.isEmpty()) rows.add(new Row(FormattedCharSequence.EMPTY, "", 0));
            else for (var wrapped : font.split(TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal(line), false), false), width)) rows.add(new Row(wrapped, "", 0));
        }
        int total = rows.size() * lineHeight, height = Math.min(total, g.guiHeight() - 20);
        scroll = Math.clamp(scroll, 0, Math.max(0, (total - height + lineHeight - 1) / lineHeight));
        var position = Tooltips.position(g.guiWidth(), g.guiHeight(), mx, my, width, height);
        int x = position.x(), y = position.y();
        g.nextStratum();
        Tooltips.background(g, x - 4, y - 4, width + 8, height + 8);
        g.enableScissor(x, y, x + width, y + height);
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            int rowY = y + (index - scroll) * lineHeight;
            if (row.image().isEmpty()) g.text(font, row.text(), x, rowY, 0xFFE0E0E0);
            else UiAssets.draw(g, row.image(), x + row.imageX(), rowY - (129 - lineHeight) / 2, 86, 129);
        }
        g.disableScissor();
    }
}
