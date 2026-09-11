package dev.hoi.client.ui;

import dev.hoi.client.audio.UiSounds;

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


public final class HoiMenuBar {
    private static CountryHud cachedHud;
    private static List<Indicator> cachedIndicators = List.of();
    private HoiMenuBar() {}
    public static void clear() { cachedHud = null; cachedIndicators = List.of(); HudTooltip.clear(); }

    public static int height(int width) { return width >= 600 ? 36 : 34; }

    private static int tabWidth(int width) {
        return Math.min(Math.min(28, Math.max(20, width * 7 / 200)), (width - 12) / MenuTab.ORDER.size());
    }

    public static int dockWidth(int width) { return statsLeft(width) + tabWidth(width) * (MenuTab.ORDER.size() - 1); }

    public static final int STATS_HEIGHT = 14;
    public static int flagWidth(int width) { return (height(width) - 6) * 98 / 67; }
    static int statsLeft(int width) { return flagWidth(width) + 10; }
    private static final int TENSION_DOCK_WIDTH = 38;
    public static boolean showingTensionTooltip;
    public static int tensionX(int width) { return width - 34; }
    public static int statsRight(int width) { return width - TENSION_DOCK_WIDTH; }

    public enum Format { NUMBER, POLITICAL_POWER, COMMAND_POWER, EXPERIENCE, NUCLEAR, PERCENT, MONEY }
    public record Indicator(String icon, String label, Number raw, Format format, String barLabel, Double ratio, String value) {
        public Indicator(String icon, String label, Number raw, Format format, String barLabel, Double ratio) { this(icon, label, raw, format, barLabel, ratio, value(icon, raw, format)); }
        public Indicator(String icon, String label, Number raw, Format format) { this(icon, label, raw, format, null, null); }
        public int width() { return format == Format.MONEY ? 67 : format == Format.POLITICAL_POWER || format == Format.PERCENT ? 54 : format == Format.EXPERIENCE || format == Format.COMMAND_POWER ? 48 : icon.equals("manpower") || icon.equals("fuel") || icon.equals("convoys") || icon.equals("factories") ? 52 : 40; }
        private static String value(String icon, Number raw, Format format) {
            if (raw == null) return "—";
            return switch (format) {
                case NUMBER -> icon.equals("factories") ? Long.toString(raw.longValue()) : number(raw.doubleValue());
                case POLITICAL_POWER -> politicalPower(raw.doubleValue());
                case COMMAND_POWER -> Long.toString(raw.longValue());
                case EXPERIENCE -> Long.toString(Math.clamp(raw.longValue(), 0, 1000));
                case NUCLEAR -> Long.toString(Math.clamp(raw.longValue(), 0, 999));
                case PERCENT -> percent(raw.doubleValue());
                case MONEY -> money(raw.doubleValue());
            };
        }
    }

    public static List<Indicator> indicators(CountryHud hud) {
        if (hud == cachedHud) return cachedIndicators;
        var n = hud.national();
        var items = new ArrayList<>(List.of(
                new Indicator("political_power", "정치력", n.politicalPower(), Format.POLITICAL_POWER),
                new Indicator("stability", "안정도", n.stability(), Format.PERCENT),
                new Indicator("war_support", "전쟁 지지도", n.warSupport(), Format.PERCENT),
                new Indicator("manpower", "인력", n.manpower(), Format.NUMBER),
                new Indicator("factories", "공장", n.factories(), Format.NUMBER, "에너지", n.energyRatio()),
                new Indicator("fuel", "연료", n.fuel(), Format.NUMBER),
                new Indicator("supplies", "보급", n.supplyEfficiency(), Format.PERCENT, "보급 효율", n.supplyEfficiency()),
                new Indicator("convoys", "수송", n.convoys(), Format.NUMBER, "수송 효율", n.transportEfficiency()),
                new Indicator("command_power", "지휘력", n.commandPower(), Format.COMMAND_POWER),
                new Indicator("army_experience", "육군 경험치", hud.armyExperience(), Format.EXPERIENCE),
                new Indicator("air_experience", "공군 경험치", hud.airExperience(), Format.EXPERIENCE),
                new Indicator("navy_experience", "해군 경험치", hud.navyExperience(), Format.EXPERIENCE),
                new Indicator("party_support", "집권 정당 지지도", n.rulingPartySupport(), Format.PERCENT)));
        items.add(new Indicator("nuclear", "핵폭탄", hud.nuclearStockpile(), Format.NUCLEAR));
        items.add(new Indicator("gdp", "실질 GDP (십억 달러)", hud.gdpBillions(), Format.MONEY));
        items.add(new Indicator("debt", "국가부채 (십억 달러)", hud.debtBillions(), Format.MONEY));
        cachedHud = hud; cachedIndicators = List.copyOf(items);
        return cachedIndicators;
    }

    public static int maximumScroll(int width, CountryHud hud) {
        return Math.max(0, indicators(hud).stream().mapToInt(Indicator::width).sum() - (statsRight(width) - statsLeft(width) - 3));
    }

    public static int scroll(int width, CountryHud hud, int offset, double horizontal, double vertical) {
        if (HudTooltip.scroll(vertical)) return offset;
        return Math.clamp(offset - (int)((horizontal != 0 ? horizontal : vertical) * 42), 0, maximumScroll(width, hud));
    }

    public static void draw(GuiGraphicsExtractor g, int width, CountryHud hud, int mx, int my, int offset) {
        int dock = dockWidth(width), h = height(width);
        HoiMenuStyle.metal(g, 0, 0, width, STATS_HEIGHT + 1);
        HoiMenuStyle.metal(g, 0, STATS_HEIGHT, dock, h - STATS_HEIGHT);
        HoiMenuStyle.recess(g, statsLeft(width) - 3, 15, dock - statsLeft(width), h - 17);
        var items = indicators(hud);
        int maxScroll = maximumScroll(width, hud), left = statsLeft(width) - Math.clamp(offset, 0, maxScroll);
        int contentWidth = items.stream().mapToInt(Indicator::width).sum();
        Indicator hovered = null;
        int right = statsRight(width);
        g.enableScissor(statsLeft(width), 0, right - 2, STATS_HEIGHT + 1);
        for (var item : items) {
            if (item.icon().equals("gdp") && maxScroll == 0) left += Math.max(0, right - statsLeft(width) - contentWidth - 3);
            if (left + item.width() > statsLeft(width) && left < right - 2) stat(g, item, left);
            if (mx >= Math.max(statsLeft(width), left) && mx < Math.min(right - 2, left + item.width()) && my >= 1 && my < STATS_HEIGHT)
                hovered = item;
            left += item.width();
        }
        g.disableScissor();
        HudTooltip.draw(g, width, hud, hovered, mx, my);
        drawTension(g, width, hud, mx, my);
    }

    public static void drawPassive(GuiGraphicsExtractor g, int width, CountryHud hud, String country) {
        MusicButton.draw(g, width);
        draw(g, width, hud, -1, -1, 0);
        int tabWidth = tabWidth(width);
        for (var tab : MenuTab.ORDER) drawTab(g, tab, country, false,
                tab == MenuTab.POLITICS ? 4 : statsLeft(width) + (tab.ordinal() - 1) * tabWidth,
                tab == MenuTab.POLITICS ? 3 : 17, tab == MenuTab.POLITICS ? flagWidth(width) : tabWidth - 2,
                height(width) - (tab == MenuTab.POLITICS ? 6 : 20));
    }

    private static void drawTension(GuiGraphicsExtractor g, int width, CountryHud hud, int mx, int my) {
        int x = tensionX(width), h = height(width) - 6;
        var font = Minecraft.getInstance().font;
        int percentY = 3 + h + 2, percentHeight = font.lineHeight + 4;
        HoiMenuStyle.metal(g, statsRight(width), 0, TENSION_DOCK_WIDTH, percentY + percentHeight + 3);
        HoiMenuStyle.recess(g, x, 3, 30, h);
        UiAssets.draw(g, "hud/defcon/frame", x, 3, 30, h);
        int frame = defconFrame(hud.worldTension());
        if (frame >= 0) UiAssets.draw(g, "hud/defcon/" + frame, x + 2, 5, 26, h - 4);
        else g.centeredText(font, "—", x + 15, 3 + (h - font.lineHeight) / 2, HoiMenuStyle.TEXT);
        HoiMenuStyle.recess(g, x, percentY, 30, percentHeight);
        g.centeredText(font, hud.worldTension() == null ? "—" : percent(hud.worldTension()), x + 15, percentY + 2, HoiMenuStyle.TEXT);
        if (mx >= x && mx < x + 30 && my >= 3 && my < percentY + percentHeight) {
            g.outline(x, my >= percentY ? percentY : 3, 30, my >= percentY ? percentHeight : h, HoiMenuStyle.ACCENT);
            showingTensionTooltip = true;
            try { HudTooltip.draw(g, width, hud, new Indicator("world_tension", "세계 긴장도", hud.worldTension(), Format.PERCENT), mx, my); }
            finally { showingTensionTooltip = false; }
        }
    }


    public static int defconFrame(Double tension) {
        if (tension == null) return -1;
        for (int frame = 0; frame < 9; frame++)
            if (tension < (frame + 1) / 10.0) return frame;
        return 9;
    }

    static String politicalPower(Double value) {
        if (value == null) return "—";
        return Long.toString(value.longValue());
    }

    public static String number(Double value) {
        if (value == null) return "—";
        double magnitude = Math.abs(value);
        if (magnitude >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000);
        return magnitude >= 1000 ? String.format(Locale.ROOT, "%.1fK", value / 1000) : rawNumber(value);
    }

    public static String rawNumber(Number value) {
        if(value==null)return "—";
        try {return new java.math.BigDecimal(value.toString()).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
        catch(NumberFormatException e){return value.toString();}
    }

    private static String percent(double value) { return String.format(Locale.ROOT, "%.0f%%", value * 100); }

    static String money(Double value) {
        if (value == null) return "—";
        return java.math.BigDecimal.valueOf(value).movePointLeft(3).setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "조";
    }

    private static void stat(GuiGraphicsExtractor g, Indicator item, int x) {
        int w = item.width();
        HoiMenuStyle.recess(g, x, 1, w - 1, STATS_HEIGHT - 1);
        UiAssets.draw(g, "hud/" + item.icon(), x + 2, 2, 11, 10);
        var font = Minecraft.getInstance().font;
        String value = item.value();
        String fit = font.width(value) <= w - 18 ? value : font.plainSubstrByWidth(value, Math.max(1, w - 27)) + "…";
        int color = indicatorColor(item);
        g.text(font, fit, x + 15, 2, color);
        if (item.barLabel() != null) {
            g.fill(x + 15, 11, x + w - 3, 13, 0xFF18201A);
            if (item.ratio() != null) g.fill(x + 15, 11, x + 15 + (int)Math.round((w - 18) * item.ratio()), 13, 0xFF6D9C61);
        }
    }

    static int indicatorColor(Indicator item) {
        if (item.icon().equals("supplies")) return item.raw() != null && item.raw().doubleValue() < 1 ? 0xFFAA0000 : 0xFFFFFFFF;
        return item.icon().equals("gdp") ? 0xFF94BA8A : item.icon().equals("debt") ? 0xFFCB9292 : HoiMenuStyle.TEXT;
    }

    public static List<Button> buttons(int width, String country, MenuTab selected, Consumer<MenuTab> select) {
        int tabWidth = tabWidth(width);
        var buttons = new ArrayList<Button>(MenuTab.ORDER.stream().map(tab -> new TabButton(tab, country, selected == tab,
                tab == MenuTab.POLITICS ? 4 : statsLeft(width) + (tab.ordinal() - 1) * tabWidth,
                tab == MenuTab.POLITICS ? flagWidth(width) : tabWidth - 2, tab == MenuTab.POLITICS ? 3 : 17,
                height(width) - (tab == MenuTab.POLITICS ? 6 : 20), () -> select.accept(tab))).toList());
        int defconHeight = height(width) - 6;
        for (int part = 0; part < 2; part++) {
            int buttonY = part == 0 ? 3 : 3 + defconHeight + 2;
            int buttonHeight = part == 0 ? defconHeight : Minecraft.getInstance().font.lineHeight + 4;
            buttons.add(new Button(tensionX(width), buttonY, 30, buttonHeight, Component.literal(part == 0 ? "데프콘" : "세계 긴장도 %"),
                    b -> dev.hoi.client.HoiClient.openWorldTension(), message -> message.get()) {
                @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { UiSounds.play("ui.click"); }
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {}
            });
        }
        buttons.add(new MusicButton(width));
        return List.copyOf(buttons);
    }

    public static String flagTexture(String country) {
        return country == null || country.isBlank() ? "" : "country/" + country.toLowerCase(Locale.ROOT) + "/flag";
    }

    public static final class TabButton extends Button {
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { UiSounds.play("ui.menu_tab"); }
        private final MenuTab tab;
        private final String country;
        private final boolean selected;

        private TabButton(MenuTab tab, String country, boolean selected, int x, int width, int y, int height, Runnable action) {
            super(x, y, width, height, Component.literal(tab.label() + " 메뉴"), b -> action.run(), DEFAULT_NARRATION);
            this.tab = tab; this.country = country; this.selected = selected;
            if (tab != MenuTab.POLITICS) setTooltip(Tooltip.create(Component.literal(tab.label())));
        }

        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            drawTab(g, tab, country, selected, x, y, w, h);
        }
    }
    private static void drawTab(GuiGraphicsExtractor g, MenuTab tab, String country, boolean selected, int x, int y, int w, int h) {
            var font = Minecraft.getInstance().font;

            if (selected) g.fillGradient(x, y, x + w, y + h, 0xFF454B4C, 0xFF202526);
            String texture = tab == MenuTab.POLITICS ? flagTexture(country) : "menu/" + tab.id();
            boolean flag = tab == MenuTab.POLITICS;
            if (flag) HoiMenuStyle.metal(g, x, y, w, h);
            int artX = flag ? x + w * 8 / 98 : x, artY = flag ? y + h * 7 / 67 : y;
            int artW = flag ? w * 82 / 98 : w, artH = flag ? h * 52 / 67 : h;
            if (texture.isEmpty()) g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF4B5257);
            else if (!UiAssets.draw(g, texture, artX, artY, artW, artH) && !flag)
                g.centeredText(font, "◇", x + w / 2, y + (h - 8) / 2, HoiMenuStyle.ACCENT);
            if (flag) UiAssets.draw(g, "menu/flag_overlay", x, y, w, h);
            if (selected) {
                g.horizontalLine(x + 2, x + w - 3, y + h - 1, HoiMenuStyle.ACCENT);
            }
    }
}
