package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;

final class DiplomacyHeader {
    private final List<int[]> chart;
    private final List<MenuView.Entry> spirits;
    private int spiritScroll;
    DiplomacyHeader(CountryView view, int pane) {
        chart = chart(view, scaled(80, pane));
        spirits = view.diplomacy().stream().filter(e -> e.detail().startsWith("국가 정신\n")).toList();
    }
    static boolean summary(MenuView.Entry entry) {
        return Set.of("안정도", "전쟁 지지도", "집권 정당", "이념", "TFR 원본 시작 이념", "지도자", "다음 선거").contains(entry.name())
                || entry.detail().equals("정당 지지도") || entry.detail().startsWith("국가 정신\n");
    }
    static int scaled(int value, int pane) { return Math.round(value * pane / 550f); }
    static int tabs(int top) { return top + 70; }
    static int politicsTop(int top) { return tabs(top) + 24; }
    static int leaderHeight(int pane) { return Math.max(80, scaled(175, pane)); }
    static int spiritsTop(int top, int pane) { return politicsTop(top) + leaderHeight(pane) + 4; }
    static int body(int top, int pane) { return spiritsTop(top, pane) + Math.max(32, scaled(72, pane)) + 8; }
    static MenuView.Entry entry(CountryView view, String name) {
        return view.diplomacy().stream().filter(e -> e.name().equals(name)).findFirst().orElse(new MenuView.Entry(name, "정보 없음", ""));
    }
    void draw(GuiGraphicsExtractor g, Font font, CountryView view, int pane, int top) {
        int y = politicsTop(top), leaderX = scaled(13, pane), leaderW = scaled(134, pane), leaderH = leaderHeight(pane);
        var leader = entry(view, "지도자");
        UiAssets.draw(g, leader.icon().isEmpty() ? "politics/empty/leader" : leader.icon(), leaderX + 3, y + 3, leaderW - 6, leaderH - 6);
        UiAssets.nineSlice(g, "diplomacy/leader_frame", leaderX, y, leaderW, leaderH, 5, pane / 550.0);
        int right = scaled(147, pane), rightW = pane - right - scaled(12, pane);
        UiAssets.nineSlice(g, "diplomacy/politics", right, y, rightW, leaderH, 5, pane / 550.0);
        int chart = scaled(80, pane), chartX = right + scaled(10, pane), chartY = y + 5;
        for (var span : this.chart) g.fill(chartX + span[0], chartY + span[1], chartX + span[2], chartY + span[1] + 1, span[3]);
        int textX = right + scaled(110, pane), textW = pane - textX - 12;
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "집권 정당").value(), Math.max(1, (int)(textW / UiText.scale(font)))), textX, y + 5, HoiMenuStyle.TEXT);
        UiText.text(g, font, font.plainSubstrByWidth(entry(view, "이념").value(), Math.max(1, (int)(textW / UiText.scale(font)))), textX, y + 17, HoiMenuStyle.TEXT);
        UiText.text(g, font, entry(view, "다음 선거").value(), textX, y + 29, 0xFFFFBB22);
        int focusH = Math.max(28, scaled(64, pane));
        int focusY = y + leaderH - focusH - 3, focusX = right + scaled(100, pane);
        UiAssets.draw(g, "politics/empty/focus", right + 4, focusY, scaled(90, pane), focusH);
        UiAssets.nineSlice(g, "diplomacy/focus", focusX, focusY, pane - focusX - 12, focusH, 5, pane / 550.0);
        var focus = view.focuses().stream().filter(e -> !e.value().equals("완료")).findFirst().orElse(new MenuView.Entry("선택된 중점 없음", "", ""));
        UiText.centered(g, font, focus.name(), focusX + 4, focusY + 3, pane - focusX - 20, focusH - 6, HoiMenuStyle.TEXT);
        int spiritY = spiritsTop(top, pane), spiritH = Math.max(32, scaled(72, pane));
        UiAssets.nineSlice(g, "diplomacy/spirits", scaled(15, pane), spiritY, pane - scaled(30, pane), spiritH, 5, pane / 550.0);
        UiText.text(g, font, "국민정신", scaled(20, pane), spiritY + 4, HoiMenuStyle.TEXT);
        int iconSize = spiritSize(pane), start = spiritStart(pane);
        spiritScroll = Math.clamp(spiritScroll, 0, Math.max(0, spirits.size() * iconSize - (pane - 12 - start)));
        g.enableScissor(start, spiritY + 2, pane - 12, spiritY + spiritH - 2);
        for (int i = spiritScroll / iconSize; i < spirits.size(); i++) {
            int x = start + i * iconSize - spiritScroll;
            if (x >= pane - 12) break;
            var spirit = spirits.get(i);
            UiAssets.draw(g, spirit.icon().isEmpty() ? "menu/politics" : spirit.icon(), x, spiritY + 5, iconSize, Math.max(12, spiritH - 10));
        }
        g.disableScissor();
    }
    private static int spiritStart(int pane) { return scaled(140, pane); }
    private static int spiritSize(int pane) { return Math.max(16, scaled(54, pane)); }
    MenuView.Entry spiritAt(double x, double y, int pane, int top) {
        int start = spiritStart(pane), sy = spiritsTop(top, pane);
        if (x < start || x >= pane - 12 || y < sy + 2 || y >= sy + Math.max(32, scaled(72, pane)) - 2) return null;
        int index = (int)(x - start + spiritScroll) / spiritSize(pane);
        return index < spirits.size() ? spirits.get(index) : null;
    }
    boolean scrollSpirits(double x, double y, double amount, int pane, int top) {
        int sy = spiritsTop(top, pane);
        if (x < spiritStart(pane) || x >= pane - 12 || y < sy || y >= sy + Math.max(32, scaled(72, pane))) return false;
        spiritScroll = Math.clamp(spiritScroll - (int)(amount * spiritSize(pane)), 0,
                Math.max(0, spirits.size() * spiritSize(pane) - (pane - 12 - spiritStart(pane))));
        return true;
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
