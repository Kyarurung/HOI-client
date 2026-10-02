package dev.hoi.client.ui;

import dev.hoi.protocol.ProtocolMenuTab;

public final class UiPanelLayout {
    private UiPanelLayout() {}
    public static int width(ProtocolMenuTab tab, int screenWidth) {
        if (tab == ProtocolMenuTab.INTELLIGENCE) return Math.max(1, Math.min(270, screenWidth - 16));
        if (tab == ProtocolMenuTab.PRODUCTION || tab == ProtocolMenuTab.CONSTRUCTION) return Math.max(1, Math.min(screenWidth - 16, Math.max(275, screenWidth * 550 / 2560)));
        int reference = switch (tab) {
            case POLITICS -> 726;
            case INTELLIGENCE -> 540;
            case TRADE -> 620;
            case LOGISTICS -> 720;
            default -> 550;
        };
        int minimum = tab == ProtocolMenuTab.POLITICS ? 248 : tab == ProtocolMenuTab.TRADE ? 244 : tab == ProtocolMenuTab.LOGISTICS ? 300 : 184;
        if (screenWidth >= 800) return tab == ProtocolMenuTab.LOGISTICS ? Math.max(minimum, screenWidth * reference / 2560) : screenWidth * reference / 2560;
        int limit = Math.min(screenWidth - 16, Math.max(minimum, screenWidth * (tab == ProtocolMenuTab.POLITICS ? 40 : 35) / 100));
        return Math.max(1, Math.min(Math.max(minimum, screenWidth * reference / 2560), limit));
    }
}
