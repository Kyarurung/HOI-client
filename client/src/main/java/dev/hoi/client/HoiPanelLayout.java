package dev.hoi.client;

import dev.hoi.protocol.MenuTab;

/** TFR interface container widths scaled to the user's 2560-pixel reference captures. */
final class HoiPanelLayout {
    private HoiPanelLayout() {}

    static int width(MenuTab tab, int screenWidth) {
        // country{politics,trade,logistics}view.gui; all other main containers use 550.
        int original = switch (tab) {
            case POLITICS -> 726;
            case TRADE -> 620;
            case LOGISTICS -> 560;
            default -> 550;
        };
        int minimum = tab == MenuTab.POLITICS ? 248 : tab == MenuTab.TRADE ? 184 : 168;
        // At high GUI scales retain legible controls instead of shrinking images or overflowing text.
        return Math.min(screenWidth - 16, Math.max(minimum, screenWidth * original / 2560));
    }
}
