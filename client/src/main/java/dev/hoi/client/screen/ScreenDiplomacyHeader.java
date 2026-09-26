package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;

final class ScreenDiplomacyHeader {
    private final List<int[]> chart;
    private final List<ProtocolMenuView.Entry> spirits;
    private int spiritScroll;
    ScreenDiplomacyHeader(ProtocolCountryView view, int pane) {
        chart = chart(view, scaled(80, pane));
        spirits = view.diplomacy().stream().filter(e -> e.detail().startsWith("국가 정신\n")).toList();
    }
    static int scaled(int value, int pane) { return Math.round(value * pane / 550f); }
    static int tabs(int top) { return top + 70; }
    static int politicsTop(int top) { return tabs(top) + 24; }
    static int leaderHeight(int pane) { return Math.max(80, scaled(175, pane)); }
    static int spiritsTop(int top, int pane) { return politicsTop(top) + leaderHeight(pane) + 5; }
    static int body(int top, int pane) { return spiritsTop(top, pane) + Math.max(32, scaled(72, pane)) + 8; }
    static ProtocolMenuView.Entry entry(ProtocolCountryView view, String name) {
        return view.diplomacy().stream().filter(e -> e.name().equals(name)).findFirst().orElse(new ProtocolMenuView.Entry(name, "정보 없음", ""));
    }
    void draw(GuiGraphicsExtractor g, Font font, ProtocolCountryView view, int pane, int top) {
        int y = politicsTop(top), leaderX = scaled(13, pane), leaderW = scaled(134, pane), leaderH = leaderHeight(pane);
        var leader = entry(view, "지도자");
        UiAssets.draw(g, leader.icon().isEmpty() ? "politics/empty/leader" : leader.icon(), leaderX + 3, y + 3, leaderW - 6, leaderH - 6);
        UiAssets.nineSlice(g, "diplomacy/leader_frame", leaderX, y, leaderW, leaderH, 5, pane / 550.0);
        int right = scaled(147, pane), rightW = pane - right - scaled(12, pane);
        UiAssets.nineSlice(g, "diplomacy/politics", right, y, rightW, leaderH, 5, pane / 550.0);
        int edge = Math.max(1, scaled(5, pane));
        int chartCellW = edge + Math.round((rightW - edge * 2) * 99f / 388);
        int chartCellH = edge + Math.round((leaderH - edge * 2) * 85f / 157);
        int chart = scaled(80, pane), chartX = right + (chartCellW - chart) / 2 + 2, chartY = y + (chartCellH - chart) / 2 + 1;
        for (var span : this.chart) g.fill(chartX + span[0], chartY + span[1], chartX + span[2], chartY + span[1] + 1, span[3]);
        int textX = right + chartCellW + 3, textW = rightW - chartCellW - 6;
        int textY = y + (chartCellH - 36) / 2 + 1;
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "집권 정당").value(), (int)(textW / UiText.scale(font))), textX, textY + 1, UiMenuStyle.TEXT);
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "이념").value(), (int)(textW / UiText.scale(font))), textX, textY + 13, UiMenuStyle.TEXT);
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "다음 선거").value(), (int)(textW / UiText.scale(font))), textX, textY + 25, UiMenuStyle.TEXT);
        int focusH = Math.max(28, scaled(64, pane));
        int focusY = y + leaderH - focusH - 3, focusX = right + scaled(100, pane);
        UiAssets.draw(g, "politics/empty/focus", right + 4, focusY, scaled(90, pane), focusH);
        UiAssets.nineSlice(g, "diplomacy/focus", focusX, focusY, pane - focusX - 12, focusH, 5, pane / 550.0);
        var focus = view.focuses().stream().filter(e -> !e.value().equals("완료")).findFirst().orElse(new ProtocolMenuView.Entry("선택된 중점 없음", "", ""));
        UiText.centered(g, font, focus.name(), focusX + 4, focusY + 3, pane - focusX - 20, focusH - 6, UiMenuStyle.TEXT);
        int spiritY = spiritsTop(top, pane), spiritH = Math.max(32, scaled(72, pane));
        UiAssets.nineSlice(g, "diplomacy/spirits", scaled(15, pane), spiritY, pane - scaled(30, pane), spiritH, 5, pane / 550.0);
        UiText.text(g, font, "국민정신", scaled(20, pane), spiritY + 4, UiMenuStyle.TEXT);
        int start = spiritStart(pane), rowHeight = spiritRowHeight(pane), gridWidth = spiritWidth(pane);
        spiritScroll = Math.clamp(spiritScroll, 0, maxSpiritScroll(pane));
        g.enableScissor(start, spiritY + 4, start + gridWidth, spiritY + 4 + spiritViewportHeight(pane));
        for (int i = spiritScroll / rowHeight * 6; i < spirits.size(); i++) {
            int column = i % 6, rowY = i / 6 * rowHeight - spiritScroll;
            if (rowY >= spiritViewportHeight(pane)) break;
            int x = start + column * gridWidth / 6, rightEdge = start + (column + 1) * gridWidth / 6;
            var spirit = spirits.get(i);
            UiAssets.draw(g, spirit.icon().isEmpty() ? "menu/politics" : spirit.icon(), x, spiritY + 4 + rowY, rightEdge - x, rowHeight);
        }
        g.disableScissor();
        if (maxSpiritScroll(pane) > 0) {
            int trackX = start + gridWidth + 2, trackY = spiritY + 4, trackH = spiritViewportHeight(pane);
            int thumbH = Math.max(4, trackH * trackH / (maxSpiritScroll(pane) + trackH));
            int thumbY = trackY + spiritScroll * (trackH - thumbH) / maxSpiritScroll(pane);
            g.fill(trackX, trackY, trackX + 2, trackY + trackH, 0xFF32363A);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFFD5D5D0);
        }
    }
    ProtocolMenuView.Entry partyAt(ProtocolCountryView view, double x, double mouseY, int pane, int top) {
        int right=scaled(147,pane),rightW=pane-right-scaled(12,pane),edge=Math.max(1,scaled(5,pane));
        int cellW=edge+Math.round((rightW-edge*2)*99f/388),cellH=edge+Math.round((leaderHeight(pane)-edge*2)*85f/157);
        int size=scaled(80,pane),left=right+(cellW-size)/2+2,upper=politicsTop(top)+(cellH-size)/2+1;
        double dx=x-left-size/2.0,dy=mouseY-upper-size/2.0;
        return ScreenPartyChart.at(view.diplomacy().stream().filter(party->party.detail().startsWith("정당 지지도")).toList(),dx,dy,size/2.0);
    }
    boolean leaderAt(double x, double y, int pane, int top) {
        int left = scaled(13, pane), upper = politicsTop(top);
        return x >= left && x < left + scaled(134, pane) && y >= upper && y < upper + leaderHeight(pane);
    }
    private static int spiritStart(int pane) { return scaled(140, pane); }
    private static int spiritWidth(int pane) { return pane - 18 - spiritStart(pane); }
    private static int spiritRowHeight(int pane) { return Math.max(12, Math.max(32, scaled(72, pane)) - 10); }
    private static int spiritViewportHeight(int pane) { return Math.max(32, scaled(72, pane)) - 8; }
    private int maxSpiritScroll(int pane) { return Math.max(0, (spirits.size() + 5) / 6 * spiritRowHeight(pane) - spiritViewportHeight(pane)); }
    ProtocolMenuView.Entry spiritAt(double x, double y, int pane, int top) {
        int start = spiritStart(pane), sy = spiritsTop(top, pane) + 4, gridWidth = spiritWidth(pane), rowHeight = spiritRowHeight(pane);
        if (x < start || x >= start + gridWidth || y < sy || y >= sy + spiritViewportHeight(pane)) return null;
        int column = Math.min(5, ((int)(x - start) * 6 + 5) / gridWidth);
        int index = ((int)(y - sy) + spiritScroll) / rowHeight * 6 + column;
        return index < spirits.size() ? spirits.get(index) : null;
    }
    boolean scrollSpirits(double x, double y, double amount, int pane, int top) {
        int sy = spiritsTop(top, pane);
        if (x < spiritStart(pane) || x >= pane - 12 || y < sy || y >= sy + Math.max(32, scaled(72, pane))) return false;
        int steps = (int)Math.copySign(Math.ceil(Math.abs(amount)), amount);
        spiritScroll = Math.clamp(spiritScroll - steps * spiritRowHeight(pane), 0, maxSpiritScroll(pane));
        return true;
    }
    private static List<int[]> chart(ProtocolCountryView view, int size) {
        var result = new ArrayList<int[]>();
        var parties = view.diplomacy().stream().filter(e -> e.detail().startsWith("정당 지지도")).toList();
        double radius = size / 2.0;
        int[] colors = {0xFF4267CE, 0xFFB52C39, 0xFF97652F, 0xFF939393};
        for (int row = 0; row < size; row++) {
          int start = 0, previous = 0;
          for (int col = 0; col <= size; col++) {
            double dx = col + .5 - radius, dy = row + .5 - radius;
            boolean outside = col == size || dx * dx + dy * dy >= radius * radius;
            double angle = (Math.atan2(dy, dx) + Math.PI * 2.5) % (2 * Math.PI) / (2 * Math.PI), sum = 0;
            int color = outside ? 0 : 0xFF555555;
            if (!outside) for (var party : parties) {
                sum += party.progress();
                if (angle < sum) {
                    color = party.icon().matches("politics/party/[0-9a-fA-F]{6}") ? 0xFF000000 | Integer.parseInt(party.icon().substring(party.icon().lastIndexOf('/') + 1), 16) : party.icon().endsWith("democratic") ? colors[0] : party.icon().endsWith("communist") ? colors[1]
                            : party.icon().endsWith("fascist") ? colors[2] : colors[3]; break;
                }
            }
            if (color != previous) {
                if (previous != 0) result.add(new int[]{start, row, col, previous});
                start = col; previous = color;
            }
          }
        }
        return List.copyOf(result);
    }
}
