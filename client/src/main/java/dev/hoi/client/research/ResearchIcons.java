package dev.hoi.client.research;

import dev.hoi.client.ui.UiAssets;

import net.minecraft.client.gui.GuiGraphicsExtractor;


public final class ResearchIcons {
    private ResearchIcons() {}
    static void draw(GuiGraphicsExtractor g, String category, int x, int y, int color) {
        draw(g, category, x, y, color, 2);
    }
    static void draw(GuiGraphicsExtractor g, String category, int x, int y, int color, int scale) {
        if (UiAssets.draw(g, "category/" + category.toLowerCase(java.util.Locale.ROOT), x, y, 10 * scale, 8 * scale)) return;
        fallback(g, category, x, y, color, scale);
    }
    public static void fallback(GuiGraphicsExtractor g, String category, int x, int y, int color, int scale) {
        String[] glyph = switch (category) {
            case "INFANTRY" -> new String[]{"   ####   ", " ######## ", "##########", "##########", "###    ###", "  #    #  ", "  ######  ", "   ####   "};
            case "SUPPORT" -> new String[]{"   ####   ", "   ####   ", "##########", "##########", "##########", "##########", "   ####   ", "   ####   "};
            case "ARTILLERY" -> new String[]{"        ##", "      ####", "    ####  ", "  ####    ", " #######  ", "## ## ##  ", "#  ##  #  ", " ##  ##   "};
            case "ARMOR" -> new String[]{"    ###   ", "   #######", "   ####   ", " ######## ", "##########", "# # ## # #", "# # ## # #", " ######## "};
            case "NAVY" -> new String[]{"    #     ", "    ###   ", "   ####   ", "##########", " ######## ", "  ######  ", "          ", "## ### ###"};
            case "AIR" -> new String[]{"    ##    ", "    ##    ", "   ####   ", "##########", "##########", "    ##    ", "   ####   ", "  ######  "};
            case "ENGINEERING" -> new String[]{" # # # #  ", " ######## ", "##      ##", " # #### # ", "## #### ##", " #      # ", " ######## ", " # # # #  "};
            default -> new String[]{"        ##", "  #  #  ##", " ## ##  ##", "##########", "##########", "#  #  #  #", "##########", "##########"};
        };
        for (int row = 0; row < glyph.length; row++) for (int col = 0; col < glyph[row].length(); col++)
            if (glyph[row].charAt(col) == '#') g.fill(x + col * scale, y + row * scale, x + (col + 1) * scale, y + (row + 1) * scale, color);
    }
}
