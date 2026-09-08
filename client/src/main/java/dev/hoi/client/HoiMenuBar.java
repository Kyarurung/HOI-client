package dev.hoi.client;

import dev.hoi.protocol.MenuTab;
import dev.hoi.protocol.CountryHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;

/** Country flag spans both rows: roughly 40% indicators and 60% icon navigation. */
final class HoiMenuBar {
    private HoiMenuBar() {}

    static int height(int width) { return width >= 600 ? 36 : 34; }

    private static int tabWidth(int width) {
        return Math.min(Math.min(28, Math.max(20, width * 7 / 200)), (width - 12) / MenuTab.ORDER.size());
    }

    static int dockWidth(int width) { return 40 + tabWidth(width) * (MenuTab.ORDER.size() - 1); }

    static final int STATS_HEIGHT = 14;
    private static final int STATS_LEFT = 40;
    private static final int TENSION_DOCK_WIDTH = 38;
    static int tensionX(int width) { return width - 34; }
    static int statsRight(int width) { return width - TENSION_DOCK_WIDTH; }

    enum Format { NUMBER, PERCENT, MONEY }
    record Indicator(String icon, String label, Number raw, Format format, String barLabel, Double ratio) {
        Indicator(String icon, String label, Number raw, Format format) { this(icon, label, raw, format, null, null); }
        int width() { return format == Format.MONEY ? 66 : icon.equals("manpower") || icon.equals("fuel") ? 52 : 40; }
        String value() {
            if (raw == null) return "—";
            return switch (format) {
                case NUMBER -> number(raw.doubleValue());
                case PERCENT -> percent(raw.doubleValue());
                case MONEY -> money(raw.doubleValue());
            };
        }
    }

    static List<Indicator> indicators(CountryHud hud) {
        var n = hud.national();
        var items = new ArrayList<>(List.of(
                new Indicator("political_power", "정치력", n.politicalPower(), Format.NUMBER),
                new Indicator("stability", "안정도", n.stability(), Format.PERCENT),
                new Indicator("war_support", "전쟁 지지도", n.warSupport(), Format.PERCENT),
                new Indicator("manpower", "인력", n.manpower(), Format.NUMBER),
                new Indicator("factories", "공장", n.factories(), Format.NUMBER, "에너지", n.energyRatio()),
                new Indicator("fuel", "연료", n.fuel(), Format.NUMBER),
                new Indicator("supplies", "병참 상황", n.supplies(), Format.NUMBER, "보급 효율", n.supplyEfficiency()),
                new Indicator("convoys", "수송", n.convoys(), Format.NUMBER, "수송 효율", n.transportEfficiency()),
                new Indicator("command_power", "지휘력", n.commandPower(), Format.NUMBER),
                new Indicator("army_experience", "육군 경험치", hud.armyExperience(), Format.NUMBER),
                new Indicator("air_experience", "공군 경험치", hud.airExperience(), Format.NUMBER),
                new Indicator("navy_experience", "해군 경험치", hud.navyExperience(), Format.NUMBER),
                new Indicator("party_support", "집권 정당 지지도", n.rulingPartySupport(), Format.PERCENT)));
        items.add(new Indicator("nuclear", "핵폭탄", hud.nuclearStockpile(), Format.NUMBER));
        items.add(new Indicator("gdp", "실질 GDP (십억 달러)", hud.gdpBillions(), Format.MONEY));
        items.add(new Indicator("debt", "국가부채 (십억 달러)", hud.debtBillions(), Format.MONEY));
        return List.copyOf(items);
    }

    static int maximumScroll(int width, CountryHud hud) {
        return Math.max(0, indicators(hud).stream().mapToInt(Indicator::width).sum() - (statsRight(width) - STATS_LEFT - 3));
    }

    static int scroll(int width, CountryHud hud, int offset, double horizontal, double vertical) {
        return Math.clamp(offset - (int)((horizontal != 0 ? horizontal : vertical) * 42), 0, maximumScroll(width, hud));
    }

    static void draw(GuiGraphicsExtractor g, int width, CountryHud hud, int mx, int my, int offset) {
        int dock = dockWidth(width), h = height(width);
        HoiMenuStyle.metal(g, 0, 0, width, STATS_HEIGHT + 1);
        HoiMenuStyle.metal(g, 0, STATS_HEIGHT, dock, h - STATS_HEIGHT);
        HoiMenuStyle.recess(g, 37, 15, dock - 40, h - 17);
        var items = indicators(hud);
        int maxScroll = maximumScroll(width, hud), left = STATS_LEFT - Math.clamp(offset, 0, maxScroll);
        int contentWidth = items.stream().mapToInt(Indicator::width).sum();
        Indicator hovered = null;
        int right = statsRight(width);
        g.enableScissor(STATS_LEFT, 0, right - 2, STATS_HEIGHT + 1);
        for (var item : items) {
            if (item.icon().equals("gdp") && maxScroll == 0) left += Math.max(0, right - STATS_LEFT - contentWidth - 3);
            if (left + item.width() > STATS_LEFT && left < right - 2) stat(g, item, left);
            if (mx >= Math.max(STATS_LEFT, left) && mx < Math.min(right - 2, left + item.width()) && my >= 1 && my < STATS_HEIGHT)
                hovered = item;
            left += item.width();
        }
        g.disableScissor();
        if (hovered != null) {
            var lines = new ArrayList<Component>();
            lines.add(Component.literal(hovered.label()));
            lines.add(Component.literal(hovered.raw() == null ? "서버에 기록된 값이 없습니다." :
                    hovered.format() == Format.PERCENT ? percent(hovered.raw().doubleValue()) : hovered.raw().toString()));
            if (hovered.barLabel() != null) lines.add(Component.literal(hovered.barLabel() + ": " +
                    (hovered.ratio() == null ? "서버 값 없음" : percent(hovered.ratio()))));
            if (maxScroll > 0) lines.add(Component.literal("휠로 다른 지표 보기"));
            g.setComponentTooltipForNextFrame(Minecraft.getInstance().font, lines, mx, my);
        }
        drawTension(g, width, hud, mx, my);
    }

    private static void drawTension(GuiGraphicsExtractor g, int width, CountryHud hud, int mx, int my) {
        int x = tensionX(width), h = height(width) - 6;
        HoiMenuStyle.metal(g, statsRight(width), 0, TENSION_DOCK_WIDTH, height(width));
        HoiMenuStyle.recess(g, x, 3, 30, h);
        UiAssets.draw(g, "hud/defcon/frame", x, 3, 30, h);
        int frame = defconFrame(hud.worldTension());
        var font = Minecraft.getInstance().font;
        if (frame >= 0) UiAssets.draw(g, "hud/defcon/" + frame, x + 2, 4, 26, h - 11);
        else g.centeredText(font, "—", x + 15, 8, HoiMenuStyle.TEXT);
        g.centeredText(font, hud.worldTension() == null ? "—" : percent(hud.worldTension()), x + 15, h - 6, HoiMenuStyle.TEXT);
        if (mx >= x && mx < x + 30 && my >= 3 && my < 3 + h) {
            g.outline(x, 3, 30, h, HoiMenuStyle.ACCENT);
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal("세계 긴장도 · DEFCON"),
                    Component.literal(frame >= 0 ? "DEFCON " + (5 - frame / 2) : "DEFCON 단계 알 수 없음"), Component.literal(
                    hud.worldTension() == null ? "서버에 기록된 값이 없습니다." : percent(hud.worldTension()))), mx, my);
        }
    }

    /** The original ten-frame strip advances every 10%; 90..100% uses its final frame. */
    static int defconFrame(Double tension) {
        if (tension == null) return -1;
        for (int frame = 0; frame < 9; frame++)
            if (tension < (frame + 1) / 10.0) return frame;
        return 9;
    }

    private static String number(Double value) {
        if (value == null) return "—";
        double magnitude = Math.abs(value);
        if (magnitude >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000);
        if (magnitude >= 10_000) return String.format(Locale.ROOT, "%.0fK", value / 1000);
        return magnitude >= 1000 ? String.format(Locale.ROOT, "%.1fK", value / 1000) : String.format(Locale.ROOT, "%.0f", value);
    }

    private static String percent(double value) { return String.format(Locale.ROOT, "%.0f%%", value * 100); }

    private static String money(Double value) {
        if (value == null) return "—";
        return value >= 1000 ? String.format(Locale.ROOT, "%.3f조", value / 1000) : String.format(Locale.ROOT, "%.3fB", value);
    }

    private static void stat(GuiGraphicsExtractor g, Indicator item, int x) {
        int w = item.width();
        HoiMenuStyle.recess(g, x, 1, w - 1, STATS_HEIGHT - 1);
        UiAssets.draw(g, "hud/" + item.icon(), x + 2, 2, 11, 10);
        var font = Minecraft.getInstance().font;
        String value = item.value();
        String fit = font.width(value) <= w - 18 ? value : font.plainSubstrByWidth(value, Math.max(1, w - 27)) + "…";
        int color = item.icon().equals("gdp") ? 0xFF94BA8A : item.icon().equals("debt") ? 0xFFCB9292 : HoiMenuStyle.TEXT;
        g.text(font, fit, x + 15, 2, color);
        if (item.barLabel() != null) {
            g.fill(x + 15, 11, x + w - 3, 13, 0xFF18201A);
            if (item.ratio() != null) g.fill(x + 15, 11, x + 15 + (int)Math.round((w - 18) * item.ratio()), 13, 0xFF6D9C61);
        }
    }

    static List<TabButton> buttons(int width, String country, MenuTab selected, Consumer<MenuTab> select) {
        int tabWidth = tabWidth(width);
        return MenuTab.ORDER.stream().map(tab -> new TabButton(tab, country, selected == tab,
                tab == MenuTab.POLITICS ? 4 : 40 + (tab.ordinal() - 1) * tabWidth,
                tab == MenuTab.POLITICS ? 30 : tabWidth - 2, tab == MenuTab.POLITICS ? 3 : 17,
                height(width) - (tab == MenuTab.POLITICS ? 6 : 20), () -> select.accept(tab))).toList();
    }

    static final class TabButton extends Button {
        private final MenuTab tab;
        private final String country;
        private final boolean selected;

        private TabButton(MenuTab tab, String country, boolean selected, int x, int width, int y, int height, Runnable action) {
            super(x, y, width, height, Component.literal(tab.label() + " 메뉴"), b -> action.run(), DEFAULT_NARRATION);
            this.tab = tab; this.country = country; this.selected = selected;
            setTooltip(Tooltip.create(Component.literal(tab.label())));
        }

        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            var font = Minecraft.getInstance().font;
            // The external toolbar images already contain the original metal button frame.
            if (selected) g.fillGradient(x, y, x + w, y + h, 0xFF454B4C, 0xFF202526);
            String texture = tab == MenuTab.POLITICS && !country.isEmpty()
                    ? "country/" + country.toLowerCase(Locale.ROOT) + "/flag" : "menu/" + tab.id();
            boolean flag = tab == MenuTab.POLITICS;
            if (flag) HoiMenuStyle.metal(g, x, y, w, h);
            int inset = flag ? 2 : 0;
            if (!UiAssets.draw(g, texture, x + inset, y + inset, w - inset * 2, h - inset * 2))
                g.centeredText(font, "◇", x + w / 2, y + (h - 8) / 2, HoiMenuStyle.ACCENT);
            if (selected) {
                g.horizontalLine(x + 2, x + w - 3, y + h - 1, HoiMenuStyle.ACCENT);
            }
            if (isHoveredOrFocused()) g.outline(x, y, w, h, HoiMenuStyle.ACCENT);
        }
    }
}
