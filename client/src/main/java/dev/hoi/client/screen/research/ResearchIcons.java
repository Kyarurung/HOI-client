package dev.hoi.client.screen.research;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ResearchIcons {
    private ResearchIcons() {}
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
