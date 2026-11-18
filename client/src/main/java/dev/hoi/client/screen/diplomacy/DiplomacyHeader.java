package dev.hoi.client.screen.diplomacy;

import dev.hoi.client.ui.*;
import dev.hoi.client.screen.agency.AgencyIntelLedger;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.menu.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;

final class DiplomacyHeader {
    private final List<MenuView.Entry> parties;
    private final List<MenuView.Entry> spirits;
    private int spiritScroll;
    DiplomacyHeader(CountryView view, int pane) {
        parties = view.diplomacy().stream().filter(e -> e.icon().startsWith("politics/party/") || e.icon().startsWith("politics/ideology/")).toList();
        spirits = view.diplomacy().stream().filter(e -> e.detail().startsWith("국가 정신\n")).toList();
    }
    static int scaled(int value, int pane) { return Math.round(value * pane / 550f); }
    static float flagTop(int rowY, int pane) { return rowY + scaled(3, pane) + (scaled(44, pane) - scaled(32, pane)) / 2f; }
    static int tabs(int top) { return top + 60; }
    static int politicsTop(int top) { return tabs(top) + 24; }
    static int leaderHeight(int pane) { return Math.max(80, scaled(175, pane)); }
    static int spiritsTop(int top, int pane) { return politicsTop(top) + leaderHeight(pane) - scaled(14, pane); }
    static int body(int top, int pane) { return spiritsTop(top, pane) + Math.max(32, scaled(84, pane)); }
    static MenuView.Entry entry(CountryView view, String name) {
        return view.diplomacy().stream().filter(e -> e.name().equals(name)).findFirst().orElse(new MenuView.Entry(name, "정보 없음", ""));
    }
    static int[] chartCell(int pane, int top) {
        int right = scaled(147, pane), width = pane - right - scaled(12, pane), edge = Math.max(1, scaled(5, pane));
        return new int[]{right + edge, politicsTop(top) + edge, Math.round((width - 2 * edge) * 99f / 389), Math.round((leaderHeight(pane) - 2 * edge) * 85f / 157)};
    }
    static float[] chartBox(int pane, int top) {
        var cell = chartCell(pane, top);
        int size = scaled(71, pane);
        return new float[]{cell[0] + (cell[2] - size) / 2f, cell[1] + (cell[3] - size) / 2f, size};
    }
    void draw(GuiGraphicsExtractor g, Font font, CountryView view, int pane, int top, UiDiplomacyAssets assets) {
        int y = politicsTop(top), leaderX = scaled(13, pane), leaderW = scaled(134, pane), leaderH = leaderHeight(pane);
        var leader = entry(view, "지도자");
        UiAssets.draw(g, leader.icon().isEmpty() ? "politics/empty/leader" : leader.icon(), leaderX + 3, y + 3, leaderW - 6, leaderH - 6);
        UiAssets.nineSlice(g, "diplomacy/leader_frame", leaderX, y, leaderW, leaderH, 5, pane / 550.0);
        int right = scaled(147, pane), rightW = pane - right - scaled(12, pane);
        UiAssets.nineSlice(g, "diplomacy/politics", right, y, rightW, leaderH, 5, pane / 550.0);
        int edge = Math.max(1, scaled(5, pane));
        int chartCellH = edge + Math.round((leaderH - edge * 2) * 85f / 157);
        var chart = chartBox(pane, top);
        assets.politicalChart(g, parties, chart[0], chart[1], (int)chart[2]);
        int textX = scaled(263, pane), textW = pane - textX - scaled(20, pane);
        int textY = y + (chartCellH - 36) / 2 + 1;
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "집권 정당").value(), (int)(textW / UiText.scale(font))), textX, textY + 1, UiMenuStyle.TEXT);
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "이념").value(), (int)(textW / UiText.scale(font))), textX, textY + 13, UiMenuStyle.TEXT);
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "다음 선거").value(), (int)(textW / UiText.scale(font))), textX, textY + 25, UiMenuStyle.TEXT);
        int focusH = Math.max(28, scaled(64, pane));
        int focusY = y + leaderH - focusH - 3, focusX = right + scaled(100, pane);
        var focus = view.focuses().stream().filter(e -> !e.value().equals("완료")).findFirst().orElse(new MenuView.Entry("선택된 중점 없음", "", ""));
        UiAssets.draw(g, focus.name().equals("알려지지 않은 중점") ? AgencyIntelLedger.source("goal_unknown", 0)
            : focus.icon().isEmpty() ? "politics/empty/focus" : focus.icon(), right + 4, focusY, scaled(90, pane), focusH);
        UiAssets.nineSlice(g, "diplomacy/focus", focusX, focusY, pane - focusX - 12, focusH, 5, pane / 550.0);
        UiText.centered(g, font, focus.name(), focusX + 4, focusY + 3, pane - focusX - 20, focusH - 6, UiMenuStyle.TEXT);
        int spiritY = spiritsTop(top, pane), spiritH = Math.max(32, scaled(92, pane)), spiritInset = scaled(14, pane);
        UiAssets.nineSlice(g, "diplomacy/spirits", scaled(15, pane), spiritY, pane - scaled(30, pane), spiritH, 5, pane / 550.0);
        UiText.text(g, font, "국민정신", scaled(20, pane), spiritY + spiritInset, UiMenuStyle.TEXT);
        int start = spiritStart(pane), rowHeight = spiritRowHeight(pane), gridWidth = spiritWidth(pane);
        spiritScroll = Math.clamp(spiritScroll, 0, maxSpiritScroll(pane));
        g.enableScissor(start, spiritY + spiritInset, start + gridWidth, spiritY + spiritInset + spiritViewportHeight(pane));
        for (int i = spiritScroll / rowHeight * 6; i < spirits.size(); i++) {
            int column = i % 6, rowY = i / 6 * rowHeight - spiritScroll;
            if (rowY >= spiritViewportHeight(pane)) break;
            int x = start + column * gridWidth / 6, rightEdge = start + (column + 1) * gridWidth / 6;
            var spirit = spirits.get(i);
            UiAssets.draw(g, spirit.icon().isEmpty() ? "menu/politics" : spirit.icon(), x, spiritY + spiritInset + rowY, rightEdge - x, rowHeight);
        }
        g.disableScissor();
        if (maxSpiritScroll(pane) > 0) {
            int trackX = start + gridWidth + 2, trackY = spiritY + spiritInset, trackH = spiritViewportHeight(pane);
            int thumbH = Math.max(4, trackH * trackH / (maxSpiritScroll(pane) + trackH));
            int thumbY = trackY + spiritScroll * (trackH - thumbH) / maxSpiritScroll(pane);
            g.fill(trackX, trackY, trackX + 2, trackY + trackH, 0xFF32363A);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFFD5D5D0);
        }
    }
    MenuView.Entry partyAt(CountryView view, double x, double mouseY, int pane, int top) {
        var chart = chartBox(pane, top);
        return dev.hoi.client.screen.politics.PoliticsPartyChart.at(parties, x - chart[0] - chart[2] / 2, mouseY - chart[1] - chart[2] / 2, chart[2] * 30 / 71);
    }
    MenuView.Entry focusAt(CountryView view, double x, double mouseY, int pane, int top) {
        int height = Math.max(28, scaled(64, pane)), y = politicsTop(top) + leaderHeight(pane) - height - 3;
        if (x < scaled(147, pane) || x >= pane - 12 || mouseY < y || mouseY >= y + height) return null;
        return view.focuses().stream().filter(entry -> !entry.value().equals("완료")).findFirst().orElse(null);
    }
    boolean leaderAt(double x, double y, int pane, int top) {
        int left = scaled(13, pane), upper = politicsTop(top);
        return x >= left && x < left + scaled(134, pane) && y >= upper && y < upper + leaderHeight(pane);
    }
    private static int spiritStart(int pane) { return scaled(140, pane); }
    private static int spiritWidth(int pane) { return pane - 18 - spiritStart(pane); }
    private static int spiritRowHeight(int pane) { return Math.max(24, scaled(68, pane)); }
    private static int spiritViewportHeight(int pane) { return Math.max(24, scaled(68, pane)); }
    private int maxSpiritScroll(int pane) { return Math.max(0, (spirits.size() + 5) / 6 * spiritRowHeight(pane) - spiritViewportHeight(pane)); }
    MenuView.Entry spiritAt(double x, double y, int pane, int top) {
        int start = spiritStart(pane), sy = spiritsTop(top, pane) + scaled(14, pane), gridWidth = spiritWidth(pane), rowHeight = spiritRowHeight(pane);
        if (x < start || x >= start + gridWidth || y < sy || y >= sy + spiritViewportHeight(pane)) return null;
        int column = Math.min(5, ((int)(x - start) * 6 + 5) / gridWidth);
        int index = ((int)(y - sy) + spiritScroll) / rowHeight * 6 + column;
        return index < spirits.size() ? spirits.get(index) : null;
    }
    boolean scrollSpirits(double x, double y, double amount, int pane, int top) {
        int sy = spiritsTop(top, pane);
        if (x < spiritStart(pane) || x >= pane - 12 || y < sy || y >= sy + Math.max(32, scaled(92, pane))) return false;
        int steps = (int)Math.copySign(Math.ceil(Math.abs(amount)), amount);
        spiritScroll = Math.clamp(spiritScroll - steps * spiritRowHeight(pane), 0, maxSpiritScroll(pane));
        return true;
    }
}
