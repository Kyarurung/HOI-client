package dev.hoi.client.ui;

import dev.hoi.protocol.MenuTab;


public final class HoiPanelLayout {
    private HoiPanelLayout() {}

    public static int width(MenuTab tab, int screenWidth) {
        int reference = switch (tab) {
            case POLITICS -> 726;
            case TRADE -> 620;
            case LOGISTICS -> 720;
            default -> 550;
        };
        int minimum = tab == MenuTab.POLITICS ? 248 : tab == MenuTab.TRADE ? 244 : tab == MenuTab.LOGISTICS ? 300 : 184;


        if (screenWidth >= 800) return tab == MenuTab.LOGISTICS ? Math.max(minimum, screenWidth * reference / 2560) : screenWidth * reference / 2560;
        int limit = Math.min(screenWidth - 16, Math.max(minimum, screenWidth * (tab == MenuTab.POLITICS ? 40 : 35) / 100));
        return Math.max(1, Math.min(Math.max(minimum, screenWidth * reference / 2560), limit));
    }
}
