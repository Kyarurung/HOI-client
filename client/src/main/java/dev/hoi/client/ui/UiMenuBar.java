package dev.hoi.client.ui;

import dev.hoi.client.audio.AudioUiSounds;

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


public final class UiMenuBar {
    private static CountryHud cachedHud;
    private static List<UiIndicator> cachedIndicators = List.of();
    private UiMenuBar() {}
    public static void clear() { cachedHud = null; cachedIndicators = List.of(); UiHudTooltip.clear(); }

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

    public enum UiFormat { NUMBER, POLITICAL_POWER, COMMAND_POWER, EXPERIENCE, NUCLEAR, PERCENT, MONEY, FUEL_INCREASING, FUEL_DECREASING }
    public record UiIndicator(String icon, String label, Number raw, UiFormat format, String barLabel, Double ratio, String value) {
        public UiIndicator(String icon, String label, Number raw, UiFormat format, String barLabel, Double ratio) { this(icon, label, raw, format, barLabel, ratio, value(icon, raw, format)); }
        public UiIndicator(String icon, String label, Number raw, UiFormat format) { this(icon, label, raw, format, null, null); }
        public int width() { return format == UiFormat.MONEY ? 67 : format == UiFormat.POLITICAL_POWER || format == UiFormat.PERCENT ? 54 : format == UiFormat.EXPERIENCE || format == UiFormat.COMMAND_POWER ? 48 : icon.equals("manpower") || icon.equals("fuel") || icon.equals("convoys") || icon.equals("factories") ? 52 : 40; }
        private static String value(String icon, Number raw, UiFormat format) {
            if (icon.equals("party_support")) return dev.hoi.protocol.ProtocolNumbers.percent(raw == null ? 0.0 : raw.doubleValue(), 0);
            if (raw == null) return icon.equals("supplies") ? "0%" : "0";
            if (icon.equals("supplies")) return (int)Math.floor(Math.clamp(raw.doubleValue(),0,1)*100)+"%";
            return switch (format) {
                case NUMBER, FUEL_INCREASING, FUEL_DECREASING -> icon.equals("factories") ? Long.toString(raw.longValue()) : number(raw.doubleValue());
                case POLITICAL_POWER -> politicalPower(raw.doubleValue());
                case COMMAND_POWER -> Long.toString(raw.longValue());
                case EXPERIENCE -> Long.toString(Math.clamp(raw.longValue(), 0, 1000));
                case NUCLEAR -> Long.toString(Math.clamp(raw.longValue(), 0, 999));
                case PERCENT -> percent(raw.doubleValue());
                case MONEY -> money(raw.doubleValue());
            };
        }
    }

    public static CountryHud snapshot(CountryHud supplied) {
        return supplied == null || supplied.equals(CountryHud.UNKNOWN) ? dev.hoi.client.ClientCampaignHud.hud() : supplied;
    }

    public static List<UiIndicator> indicators(CountryHud hud) {
        hud = snapshot(hud);
        if (hud == cachedHud) return cachedIndicators;
        var n = hud.national();
        var items = new ArrayList<>(List.of(
                new UiIndicator("political_power", "정치력", n.politicalPower(), UiFormat.POLITICAL_POWER),
                new UiIndicator("stability", "안정도", n.stability(), UiFormat.PERCENT),
                new UiIndicator("war_support", "전쟁 지지도", n.warSupport(), UiFormat.PERCENT),
                new UiIndicator("manpower", "인력", n.manpower(), UiFormat.NUMBER),
                new UiIndicator("factories", "공장", n.factories(), UiFormat.NUMBER, "에너지", n.energyRatio()),
                fuelIndicator(n),
                new UiIndicator("supplies", "병참 상황", n.supplyEfficiency(), UiFormat.PERCENT, "병참 상황", n.supplyEfficiency()),
                new UiIndicator("convoys", "수송", n.convoys(), UiFormat.NUMBER, "수송 효율", n.transportEfficiency()),
                new UiIndicator("command_power", "지휘력", n.commandPower(), UiFormat.COMMAND_POWER),
                new UiIndicator("army_experience", "육군 경험치", hud.armyExperience(), UiFormat.EXPERIENCE),
                new UiIndicator("navy_experience", "해군 경험치", hud.navyExperience(), UiFormat.EXPERIENCE),
                new UiIndicator("air_experience", "공군 경험치", hud.airExperience(), UiFormat.EXPERIENCE),
                new UiIndicator("party_support", "정당 인기도", n.rulingPartySupport(), UiFormat.PERCENT)));
        items.add(new UiIndicator("nuclear", "핵폭탄", hud.nuclearStockpile(), UiFormat.NUCLEAR));
        items.add(new UiIndicator("gdp", "실질 GDP (십억 달러)", hud.gdpBillions(), UiFormat.MONEY));
        items.add(new UiIndicator("debt", "국가부채 (십억 달러)", hud.debtBillions(), UiFormat.MONEY));
        cachedHud = hud; cachedIndicators = List.copyOf(items);
        return cachedIndicators;
    }

    public static int maximumScroll(int width, CountryHud hud) {
        return Math.max(0, indicators(hud).stream().mapToInt(UiIndicator::width).sum() - (statsRight(width) - statsLeft(width) - 3));
    }

    public static int scroll(int width, CountryHud hud, int offset, double horizontal, double vertical) {
        return Math.clamp(offset - (int)((horizontal != 0 ? horizontal : vertical) * 42), 0, maximumScroll(width, hud));
    }

    public static void draw(GuiGraphicsExtractor g, int width, CountryHud hud, int mx, int my, int offset) {
        if (dev.hoi.client.screen.ScreenDialogCoordinator.renderingContent()) return;
        hud = snapshot(hud);
        int dock = dockWidth(width), h = height(width);
        UiMenuStyle.metal(g, 0, 0, width, STATS_HEIGHT + 1);
        UiMenuStyle.metal(g, 0, STATS_HEIGHT, dock, h - STATS_HEIGHT);
        UiMenuStyle.recess(g, statsLeft(width) - 3, 15, dock - statsLeft(width), h - 17);
        var items = indicators(hud);
        int maxScroll = maximumScroll(width, hud), left = statsLeft(width) - Math.clamp(offset, 0, maxScroll);
        int contentWidth = items.stream().mapToInt(UiIndicator::width).sum();
        UiIndicator hovered = null;
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
        UiHudTooltip.draw(g, width, hud, hovered, mx, my);
        drawTension(g, width, hud, mx, my);
    }

    public static void drawPassive(GuiGraphicsExtractor g, int width, CountryHud hud, String country) {
        UiMusicButton.draw(g, width);
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
        UiMenuStyle.metal(g, statsRight(width), 0, TENSION_DOCK_WIDTH, percentY + percentHeight + 3);
        UiMenuStyle.recess(g, x, 3, 30, h);
        UiAssets.draw(g, "hud/defcon/frame", x, 3, 30, h);
        int frame = defconFrame(hud.worldTension());
        if (frame >= 0) UiAssets.draw(g, "hud/defcon/" + frame, x + 2, 5, 26, h - 4);
        else g.centeredText(font, "0", x + 15, 3 + (h - font.lineHeight) / 2, UiMenuStyle.TEXT);
        UiMenuStyle.recess(g, x, percentY, 30, percentHeight);
        g.centeredText(font, hud.worldTension() == null ? "0" : percent(hud.worldTension()), x + 15, percentY + 2, UiMenuStyle.TEXT);
        if (mx >= x && mx < x + 30 && my >= 3 && my < percentY + percentHeight) {
            g.outline(x, my >= percentY ? percentY : 3, 30, my >= percentY ? percentHeight : h, UiMenuStyle.ACCENT);
            showingTensionTooltip = true;
            try { UiHudTooltip.draw(g, width, hud, new UiIndicator("world_tension", "세계 긴장도", hud.worldTension(), UiFormat.PERCENT), mx, my); }
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
        if (value == null) return "0";
        return Long.toString(value.longValue());
    }

    public static String number(Double value) {
        if (value == null) return "0";
        double magnitude = Math.abs(value);
        if (magnitude >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000);
        return magnitude >= 1000 ? String.format(Locale.ROOT, "%.1fK", value / 1000) : rawNumber(value);
    }

    public static String rawNumber(Number value) {
        if(value==null)return "0";
        try {return new java.math.BigDecimal(value.toString()).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
        catch(NumberFormatException e){return value.toString();}
    }

    private static String percent(double value) { return dev.hoi.protocol.ProtocolNumbers.percent(value, 0); }

    static String money(Double value) {
        if (value == null) return "0";
        return java.math.BigDecimal.valueOf(value).movePointLeft(3).setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "조";
    }

    private static void stat(GuiGraphicsExtractor g, UiIndicator item, int x) {
        int w = item.width();
        UiMenuStyle.recess(g, x, 1, w - 1, STATS_HEIGHT - 1);
        UiAssets.draw(g, "hud/" + icon(item), x + 2, 1 + (STATS_HEIGHT - 1 - 10) / 2, 11, 10);
        var font = Minecraft.getInstance().font;
        String value = item.value();
        String fit = font.width(value) <= w - 18 ? value : font.plainSubstrByWidth(value, Math.max(1, w - 27)) + "…";
        int color = indicatorColor(item);
        UiText.cell(g, font, fit, x + 15, 1, w - 18, item.barLabel() == null ? STATS_HEIGHT - 1 : 10, color, false);
        if (item.barLabel() != null) {
            g.fill(x + 15, 11, x + w - 3, 13, 0xFF18201A);
            if (item.ratio() != null) g.fill(x + 15, 11, x + 15 + (int)Math.round((w - 18) * Math.clamp(item.ratio(),0,1)), 13, 0xFF6D9C61);
        }
    }

    static UiIndicator fuelIndicator(CountryHud.NationalIndicators n) {
        if(n.fuelRatio()!=null&&n.fuelRatio()>=1)return new UiIndicator("fuel","연료",n.fuel(),UiFormat.NUMBER,"연료 비축",n.fuelRatio(),"가득 참");
        if(n.fuelDailyChange()==null||n.fuelDays()==null||Math.abs(n.fuelDailyChange())<1e-9)
            return new UiIndicator("fuel","연료",n.fuel(),UiFormat.NUMBER,"연료 비축",n.fuelRatio());
        return new UiIndicator("fuel","연료",n.fuel(),n.fuelDailyChange()>0?UiFormat.FUEL_INCREASING:UiFormat.FUEL_DECREASING,
                "연료 비축",n.fuelRatio(),(long)Math.ceil(n.fuelDays())+"일");
    }
    static String icon(UiIndicator item) {
        return item.format()==UiFormat.FUEL_INCREASING?"fuel_increasing":item.format()==UiFormat.FUEL_DECREASING?"fuel_decreasing":item.icon();
    }

    static int indicatorColor(UiIndicator item) {
        if(item.icon().equals("fuel")&&item.ratio()!=null&&item.ratio()>=1)return 0xFF000000|UiTooltips.GOOD;
        if(item.format()==UiFormat.FUEL_INCREASING)return 0xFF000000|UiTooltips.GOOD;
        if(item.format()==UiFormat.FUEL_DECREASING)return 0xFF000000|UiTooltips.BAD;
        if (item.icon().equals("supplies")) return item.raw() != null && item.raw().doubleValue() < 1 ? 0xFF000000|UiTooltips.BAD : 0xFFFFFFFF;
        return item.icon().equals("gdp") ? UiEffectColors.GOOD : item.icon().equals("debt") ? UiEffectColors.BAD : UiMenuStyle.TEXT;
    }

    public static List<Button> buttons(int width, String country, MenuTab selected, Consumer<MenuTab> select) {
        int tabWidth = tabWidth(width);
        var buttons = new ArrayList<Button>(MenuTab.ORDER.stream().map(tab -> new UiTabButton(tab, country, selected == tab,
                tab == MenuTab.POLITICS ? 4 : statsLeft(width) + (tab.ordinal() - 1) * tabWidth,
                tab == MenuTab.POLITICS ? flagWidth(width) : tabWidth - 2, tab == MenuTab.POLITICS ? 3 : 17,
                height(width) - (tab == MenuTab.POLITICS ? 6 : 20), () -> select.accept(tab))).toList());
        int defconHeight = height(width) - 6;
        for (int part = 0; part < 2; part++) {
            int buttonY = part == 0 ? 3 : 3 + defconHeight + 2;
            int buttonHeight = part == 0 ? defconHeight : Minecraft.getInstance().font.lineHeight + 4;
            buttons.add(new Button(tensionX(width), buttonY, 30, buttonHeight, Component.literal(part == 0 ? "데프콘" : "세계 긴장도 %"),
                    b -> dev.hoi.client.HoiClient.openWorldTension(), message -> message.get()) {
                @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play("ui.click"); }
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {}
            });
        }
        buttons.add(new UiMusicButton(width));
        return List.copyOf(buttons);
    }

    public static String flagTexture(String country) {
        return country == null || country.isBlank() ? "" : "country/" + country.toLowerCase(Locale.ROOT) + "/flag";
    }

    public static final class UiTabButton extends Button {
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play("ui.menu_tab"); }
        private final MenuTab tab;
        private final String country;
        private final boolean selected;

        private UiTabButton(MenuTab tab, String country, boolean selected, int x, int width, int y, int height, Runnable action) {
            super(x, y, width, height, Component.literal(tab.label() + " 메뉴"), b -> action.run(), DEFAULT_NARRATION);
            this.tab = tab; this.country = country; this.selected = selected;
            if (tab != MenuTab.POLITICS) setTooltip(dev.hoi.client.ui.UiTooltips.create(Component.literal(tab.label())));
        }

        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            drawTab(g, tab, country, selected, x, y, w, h);
        }
    }
    public static void drawFlag(GuiGraphicsExtractor g, String country, int x, int y, int w, int h) {
        UiMenuStyle.metal(g, x, y, w, h);
        String texture = flagTexture(country);
        if (texture.isEmpty()) g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF4B5257);
        else {
            double scale = Math.min(w / 98.0, h / 67.0);
            int frameW = (int)Math.round(98 * scale), frameH = (int)Math.round(67 * scale);
            g.pose().pushMatrix();
            g.pose().translate(x + (w - frameW) / 2, y + (h - frameH) / 2);
            g.pose().scale(frameW / 98f, frameH / 67f);
            UiAssets.cover(g, texture, 8, 9, 82, 50);
            g.pose().popMatrix();
        }
        UiAssets.draw(g, "menu/flag_overlay", x, y, w, h);
    }
    private static void drawTab(GuiGraphicsExtractor g, MenuTab tab, String country, boolean selected, int x, int y, int w, int h) {
        if (dev.hoi.client.screen.ScreenDialogCoordinator.renderingContent()) return;
            var font = Minecraft.getInstance().font;

            if (selected) g.fillGradient(x, y, x + w, y + h, 0xFF454B4C, 0xFF202526);
            String texture = tab == MenuTab.POLITICS ? flagTexture(country) : "menu/" + tab.id();
            boolean flag = tab == MenuTab.POLITICS;
            if (flag) drawFlag(g, country, x, y, w, h);
            else if (!UiAssets.draw(g, texture, x, y, w, h))
                g.centeredText(font, "◇", x + w / 2, y + (h - 8) / 2, UiMenuStyle.ACCENT);
            if (selected) {
                g.horizontalLine(x + 2, x + w - 3, y + h - 1, UiMenuStyle.ACCENT);
            }
    }
}
