package dev.hoi.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Original category emblems. Rendering does not depend on item registries, models or a resource pack. */
final class ResearchIcons {
    private ResearchIcons() {}
    static void draw(GuiGraphicsExtractor g, String category, int x, int y, int color) {
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
            if (glyph[row].charAt(col) == '#') g.fill(x + col * 2, y + row * 2, x + col * 2 + 2, y + row * 2 + 2, color);
    }
}
