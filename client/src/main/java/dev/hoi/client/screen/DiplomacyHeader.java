package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;

final class DiplomacyHeader {
    private final List<int[]> chart;
    DiplomacyHeader(CountryView view, int pane) { chart = chart(view, scaled(94, pane)); }
    static boolean summary(MenuView.Entry entry) {
        return Set.of("집권 정당", "TFR 원본 시작 이념", "지도자", "다음 선거").contains(entry.name())
                || entry.detail().equals("정당 지지도") || entry.detail().startsWith("국가 정신\n");
    }
    static int scaled(int value, int pane) { return Math.round(value * pane / 550f); }
    static int body(int top, int pane) { return top + scaled(425, pane); }
    private static MenuView.Entry entry(CountryView view, String name) {
        return view.diplomacy().stream().filter(e -> e.name().equals(name)).findFirst().orElse(new MenuView.Entry(name, "정보 없음", ""));
    }
    void draw(GuiGraphicsExtractor g, Font font, CountryView view, int pane, int top) {
        int y = top + scaled(160, pane), leaderX = scaled(13, pane), leaderW = scaled(134, pane), leaderH = scaled(175, pane);
        var leader = entry(view, "지도자");
        UiAssets.draw(g, leader.icon().isEmpty() ? "politics/empty/leader" : leader.icon(), leaderX + 3, y + 3, leaderW - 6, leaderH - 6);
        UiAssets.nineSlice(g, "diplomacy/leader_frame", leaderX, y, leaderW, leaderH, 5, pane / 550.0);
        int right = scaled(147, pane), rightW = pane - right - scaled(12, pane);
        UiAssets.nineSlice(g, "diplomacy/politics", right, y, rightW, scaled(167, pane), 5, pane / 550.0);
        int chart = scaled(94, pane), chartX = right + scaled(6, pane), chartY = y + scaled(9, pane);
        for (var span : this.chart) g.fill(chartX + span[0], chartY + span[1], chartX + span[2], chartY + span[1] + 1, span[3]);
        int textX = right + scaled(110, pane), textW = pane - textX - 12;
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "집권 정당").value(), Math.max(1, (int)(textW / UiText.scale(font)))), textX, y + scaled(17, pane), HoiMenuStyle.TEXT);
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "TFR 원본 시작 이념").value(), Math.max(1, (int)(textW / UiText.scale(font)))), textX, y + scaled(43, pane), HoiMenuStyle.TEXT);
        UiText.text(g, font, entry(view, "다음 선거").value(), textX, y + scaled(70, pane), 0xFFFFBB22);
        int focusY = y + scaled(93, pane), focusX = right + scaled(100, pane);
        UiAssets.draw(g, "politics/empty/focus", right + 4, focusY, scaled(90, pane), scaled(67, pane));
        UiAssets.nineSlice(g, "diplomacy/focus", focusX, focusY, pane - focusX - 12, scaled(64, pane), 5, pane / 550.0);
        var focus = view.focuses().stream().filter(e -> !e.value().equals("완료")).findFirst().orElse(new MenuView.Entry("선택된 중점 없음", "", ""));
        UiText.centered(g, font, focus.name(), focusX + 4, focusY + 3, pane - focusX - 20, scaled(58, pane), HoiMenuStyle.TEXT);
        int spiritY = top + scaled(337, pane), spiritH = scaled(80, pane);
        UiAssets.nineSlice(g, "diplomacy/spirits", scaled(15, pane), spiritY, pane - scaled(30, pane), spiritH, 5, pane / 550.0);
        UiText.text(g, font, "국민정신", scaled(20, pane), spiritY + 4, HoiMenuStyle.TEXT);
        var spirits = view.diplomacy().stream().filter(e -> e.detail().startsWith("국가 정신\n")).toList();
        int iconSize = Math.max(16, scaled(54, pane)), x = scaled(140, pane);
        for (var spirit : spirits) {
            if (x + iconSize > pane - 12) break;
            UiAssets.draw(g, spirit.icon(), x, spiritY + 5, iconSize, Math.max(12, spiritH - 10)); x += iconSize;
        }
    }
    private static List<int[]> chart(CountryView view, int size) {
        var result = new ArrayList<int[]>();
        var parties = view.diplomacy().stream().filter(e -> e.detail().equals("정당 지지도")).toList();
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
