package dev.hoi.client.ui.hud;

import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.protocol.menu.MenuTab;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.client.ui.*;
import dev.hoi.client.ui.country.CountryDisplays;
import dev.hoi.client.ui.tooltip.Tooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;

public final class HudMenuBar {
    private static CountryHud cachedHud;
    private static List<HudIndicator> cachedIndicators = List.of();
    private HudMenuBar() {}
    public static void clear() { cachedHud = null; cachedIndicators = List.of(); HudTooltip.clear(); }
    private static final int[] TAB_X = {109, 164, 220, 275, 331, 387, 442, 497, 553};
    private static final int TAB_Y = 35, TAB_WIDTH = 55, TAB_HEIGHT = 41, FLAG_X = 1, FLAG_Y = 6, FLAG_WIDTH = 98, FLAG_HEIGHT = 67;
    private static final int TENSION_WIDTH = 73, TENSION_HEIGHT = 93;
    static float scale(int width) { return Math.min(1f, (width - 23f) * 2 / (TAB_X[TAB_X.length - 1] + TAB_WIDTH + TENSION_WIDTH)); }
    public static int height(int width) { return (int)Math.ceil(41 * scale(width)); }
    public static float windowTop(int width) { return 39 * scale(width); }
    public static int dockWidth(int width) { return Math.round(338 * scale(width)); }
    public static final int STATS_TOP = 2, STATS_HEIGHT = 15;
    public static int flagWidth(int width) { return Math.round(FLAG_WIDTH * scale(width) / 2); }
    public static int statsLeft(int width) { return Math.round(48 * scale(width)); }
    public static boolean showingTensionTooltip;
    private static float tensionLeft(int width) { return width - TENSION_WIDTH * scale(width) / 2; }
    public static int tensionX(int width) { return (int)Math.floor(tensionLeft(width)); }
    public static int statsRight(int width) { return tensionX(width); }
    public enum HudFormat { NUMBER, POLITICAL_POWER, COMMAND_POWER, EXPERIENCE, NUCLEAR, PERCENT, MONEY, FUEL_INCREASING, FUEL_DECREASING }
    public record HudIndicator(String icon, String label, Number raw, HudFormat format, String barLabel, Double ratio, String value) {
        public HudIndicator(String icon, String label, Number raw, HudFormat format, String barLabel, Double ratio) { this(icon, label, raw, format, barLabel, ratio, value(icon, raw, format)); }
        public HudIndicator(String icon, String label, Number raw, HudFormat format) { this(icon, label, raw, format, null, null); }
        public int width() { return format == HudFormat.MONEY ? 67 : format == HudFormat.POLITICAL_POWER || format == HudFormat.PERCENT ? 54 : format == HudFormat.EXPERIENCE || format == HudFormat.COMMAND_POWER ? 48 : icon.equals("manpower") || icon.equals("fuel") || icon.equals("convoys") || icon.equals("factories") ? 52 : 40; }
        private static String value(String icon, Number raw, HudFormat format) {
            if (icon.equals("party_support")) return dev.hoi.protocol.ProtocolNumberBounds.percent(raw == null ? 0.0 : raw.doubleValue(), 0);
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
        var current = dev.hoi.client.ClientCampaignHud.hud();
        if (dev.hoi.client.ClientCampaignHud.visible() && !current.equals(CountryHud.UNKNOWN)) return current;
        return supplied == null || supplied.equals(CountryHud.UNKNOWN) ? dev.hoi.client.ClientCampaignHud.hud() : supplied;
    }
    public static List<HudIndicator> indicators(CountryHud hud) {
        hud = snapshot(hud);
        if (hud == cachedHud) return cachedIndicators;
        var n = hud.national();
        var items = new ArrayList<>(List.of(
                new HudIndicator("political_power", "정치력", n.politicalPower(), HudFormat.POLITICAL_POWER),
                new HudIndicator("stability", "안정도", n.stability(), HudFormat.PERCENT),
                new HudIndicator("war_support", "전쟁 지지도", n.warSupport(), HudFormat.PERCENT),
                new HudIndicator("manpower", "인력", n.manpower(), HudFormat.NUMBER),
                new HudIndicator("factories", "공장", n.factories(), HudFormat.NUMBER, "에너지", n.energyRatio()),
                fuelIndicator(n),
                new HudIndicator("supplies", "병참 상황", n.supplyEfficiency(), HudFormat.PERCENT, "병참 상황", n.supplyEfficiency()),
                new HudIndicator("convoys", "수송", n.convoys(), HudFormat.NUMBER, "수송 효율", n.transportEfficiency()),
                new HudIndicator("command_power", "지휘력", n.commandPower(), HudFormat.COMMAND_POWER),
                new HudIndicator("army_experience", "육군 경험치", hud.armyExperience(), HudFormat.EXPERIENCE),
                new HudIndicator("navy_experience", "해군 경험치", hud.navyExperience(), HudFormat.EXPERIENCE),
                new HudIndicator("air_experience", "공군 경험치", hud.airExperience(), HudFormat.EXPERIENCE),
                new HudIndicator("party_support", "정당 인기도", n.rulingPartySupport(), HudFormat.PERCENT)));
        items.add(new HudIndicator("nuclear", "핵폭탄", hud.nuclearStockpile(), HudFormat.NUCLEAR));
        items.add(new HudIndicator("gdp", "실질 GDP (십억 달러)", hud.gdpBillions(), HudFormat.MONEY));
        items.add(new HudIndicator("debt", "국가부채 (십억 달러)", hud.debtBillions(), HudFormat.MONEY));
        cachedHud = hud; cachedIndicators = List.copyOf(items);
        return cachedIndicators;
    }
    public static int maximumScroll(int width, CountryHud hud) {
        return Math.max(0, indicators(hud).stream().mapToInt(HudIndicator::width).sum() - (statsRight(width) - statsLeft(width) - 3));
    }
    public static int scroll(int width, CountryHud hud, int offset, double horizontal, double vertical) {
        if (horizontal == 0 && HudTooltip.scroll(snapshot(hud), vertical)) return offset;
        return Math.clamp(offset - (int)((horizontal != 0 ? horizontal : vertical) * 42), 0, maximumScroll(width, hud));
    }
    public static void draw(GuiGraphicsExtractor g, int width, CountryHud hud, int mx, int my, int offset) {
        if (dev.hoi.client.screen.dialog.DialogCoordinator.renderingContent()) return;
        drawBackground(g, width, hud, offset);
        drawTooltip(g, width, hud, mx, my, offset);
    }
    public static void drawBackground(GuiGraphicsExtractor g, int width, CountryHud hud, int offset) {
        if (dev.hoi.client.screen.dialog.DialogCoordinator.renderingContent()) return;
        hud = snapshot(hud);
        topbar(g, width);
        var items = indicators(hud);
        int maxScroll = maximumScroll(width, hud), left = statsLeft(width) - Math.clamp(offset, 0, maxScroll);
        int contentWidth = items.stream().mapToInt(HudIndicator::width).sum();
        int right = statsRight(width);
        g.enableScissor(statsLeft(width), 0, right - 2, STATS_HEIGHT + 1);
        for (var item : items) {
            if (item.icon().equals("gdp") && maxScroll == 0) left += Math.max(0, right - statsLeft(width) - contentWidth - 3);
            if (left + item.width() > statsLeft(width) && left < right - 2) stat(g, item, left);
            left += item.width();
        }
        g.disableScissor();
        drawTension(g, width, hud);
    }
    private static final Identifier TOPBAR = Identifier.parse("hoi:textures/gui/hud/topbar_background.png");
    private static void topbar(GuiGraphicsExtractor g, int width) {
        float k = scale(width) / 2;
        int band = (int)Math.ceil(width / k) - 724;
        g.pose().pushMatrix();
        g.pose().scale(k, k);
        art(g, 0, 0, 724, 82, 4, 1, 728, 83);
        if (band > 0) art(g, 724, 0, 724 + band, 38, 728, 1, 728 + Math.min(1612, band), 39);
        g.pose().popMatrix();
    }
    private static void art(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int u0, int v0, int u1, int v1) {
        g.blit(TOPBAR, x0, y0, x1, y1, u0 / 2346f, u1 / 2346f, v0 / 87f, v1 / 87f);
    }
    public static void drawTooltip(GuiGraphicsExtractor g, int width, CountryHud hud, int mx, int my, int offset) {
        hud = snapshot(hud);
        HudIndicator hovered = null;
        int maxScroll = maximumScroll(width, hud), left = statsLeft(width) - Math.clamp(offset, 0, maxScroll);
        int contentWidth = indicators(hud).stream().mapToInt(HudIndicator::width).sum();
        for (var item : indicators(hud)) {
            if (item.icon().equals("gdp") && maxScroll == 0) left += Math.max(0, statsRight(width) - statsLeft(width) - contentWidth - 3);
            if (mx >= Math.max(statsLeft(width), left) && mx < Math.min(statsRight(width) - 2, left + item.width()) && my >= STATS_TOP && my < STATS_HEIGHT) hovered = item;
            left += item.width();
        }
        showingTensionTooltip = mx >= tensionX(width) && mx < width && my >= 0 && my < TENSION_HEIGHT * scale(width) / 2;
        if (showingTensionTooltip) hovered = new HudIndicator("world_tension", "세계 긴장도", hud.worldTension(), HudFormat.PERCENT);
        try { HudTooltip.draw(g, width, hud, hovered, mx, my); }
        finally { showingTensionTooltip = false; }
    }
    public static void drawPassive(GuiGraphicsExtractor g, int width, CountryHud hud, String country) {
        HudMusicButton.draw(g, width);
        draw(g, width, hud, -1, -1, 0);
        for (var tab : MenuTab.ORDER) drawTab(g, tab, country, false, width, snapshot(hud));
    }
    private static void drawTension(GuiGraphicsExtractor g, int width, CountryHud hud) {
        float s = scale(width), k = s / 2, left = tensionLeft(width);
        var font = Minecraft.getInstance().font;
        int frame = defconFrame(hud.worldTension());
        g.pose().pushMatrix();
        g.pose().translate(left, 0);
        g.pose().scale(k, k);
        UiAssets.draw(g, "hud/defcon/background", 0, 0, TENSION_WIDTH, TENSION_HEIGHT);
        if (frame >= 0) UiAssets.draw(g, "hud/defcon/" + frame, 16, 25, 44, 34);
        g.pose().popMatrix();
        if (frame < 0) UiText.centered(g, font, "0", Math.round(left + 2 * s), Math.round(4.5f * s), Math.round(34 * s), Math.round(30 * s), UiMenuStyle.TEXT, false);
        UiText.centered(g, font, hud.worldTension() == null ? "0" : percent(hud.worldTension()), Math.round(left + s), Math.round(36.5f * s), Math.round(36.5f * s), Math.round(9 * s), UiMenuStyle.TEXT, false);
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
    private static String percent(double value) { return dev.hoi.protocol.ProtocolNumberBounds.percent(value, 0); }
    static String money(Double value) {
        if (value == null) return "0";
        var billions = java.math.BigDecimal.valueOf(value);
        long eok = billions.movePointRight(1).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        if (Math.abs(eok) < 10000) return String.format(Locale.ROOT, "%,d억", eok);
        return billions.movePointLeft(3).setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "조";
    }
    private static void stat(GuiGraphicsExtractor g, HudIndicator item, int x) {
        int w = item.width(), h = STATS_HEIGHT - STATS_TOP;
        UiAssets.nineSlice(g, "commander/commands/generic_box_smallest/frame_0", x, STATS_TOP, w - 1, h, 6, .5);
        UiAssets.draw(g, "hud/" + icon(item), x + 2, STATS_TOP + (h - 10) / 2, 11, 10);
        var font = Minecraft.getInstance().font;
        String value = item.value();
        int color = indicatorColor(item);
        UiText.cell(g, font, value, x + 15, STATS_TOP, w - 18, item.barLabel() == null ? h : 10, color, false);
        if (item.barLabel() != null) {
            g.fill(x + 15, STATS_TOP + 10, x + w - 3, STATS_TOP + 12, 0xFF18201A);
            if (item.ratio() != null) g.fill(x + 15, STATS_TOP + 10, x + 15 + (int)Math.round((w - 18) * Math.clamp(item.ratio(),0,1)), STATS_TOP + 12, 0xFF6D9C61);
        }
    }
    static HudIndicator fuelIndicator(CountryHud.NationalIndicators n) {
        if(n.fuelRatio()!=null&&n.fuelRatio()>=1)return new HudIndicator("fuel","연료",n.fuel(),HudFormat.NUMBER,"연료 비축",n.fuelRatio(),"가득 참");
        if(n.fuelDailyChange()==null||n.fuelDays()==null||Math.abs(n.fuelDailyChange())<1e-9)
            return new HudIndicator("fuel","연료",n.fuel(),HudFormat.NUMBER,"연료 비축",n.fuelRatio());
        return new HudIndicator("fuel","연료",n.fuel(),n.fuelDailyChange()>0?HudFormat.FUEL_INCREASING:HudFormat.FUEL_DECREASING,
                "연료 비축",n.fuelRatio(),(long)Math.ceil(n.fuelDays())+"일");
    }
    static String icon(HudIndicator item) {
        return item.format()==HudFormat.FUEL_INCREASING?"fuel_increasing":item.format()==HudFormat.FUEL_DECREASING?"fuel_decreasing":item.icon();
    }
    static int indicatorColor(HudIndicator item) {
        if(item.icon().equals("fuel")&&item.ratio()!=null&&item.ratio()>=1)return 0xFF000000|Tooltips.GOOD;
        if(item.format()==HudFormat.FUEL_INCREASING)return 0xFF000000|Tooltips.GOOD;
        if(item.format()==HudFormat.FUEL_DECREASING)return 0xFF000000|Tooltips.BAD;
        if (item.icon().equals("supplies")) return item.raw() != null && item.raw().doubleValue() < 1 ? 0xFF000000|Tooltips.BAD : 0xFFFFFFFF;
        return item.icon().equals("gdp") ? UiEffectColors.GOOD : item.icon().equals("debt") ? UiEffectColors.BAD : UiMenuStyle.TEXT;
    }
    public static List<Button> buttons(int width, String country, MenuTab selected, Consumer<MenuTab> select) {
        return buttons(width, country, selected, select, Integer.MAX_VALUE);
    }
    public static List<Button> buttons(int width, String country, MenuTab selected, Consumer<MenuTab> select, int bottom) {
        var buttons = new ArrayList<Button>(MenuTab.ORDER.stream().map(tab -> new HudTabButton(tab, country, selected == tab, width, () -> select.accept(tab))).toList());
        float s = scale(width), left = tensionLeft(width);
        for (int part = 0; part < 2; part++) {
            int buttonX = (int)Math.floor(left + (part == 0 ? 4 : 2) * s / 2), buttonY = (int)Math.floor((part == 0 ? 9 : 71) * s / 2);
            int buttonWidth = (int)Math.ceil(left + (part == 0 ? 72 : 71) * s / 2) - buttonX, buttonHeight = (int)Math.ceil((part == 0 ? 69 : 92) * s / 2) - buttonY;
            buttons.add(new Button(buttonX, buttonY, buttonWidth, Math.max(0, Math.min(buttonHeight, bottom - buttonY)), Component.literal(part == 0 ? "데프콘" : "세계 긴장도 %"),
                    b -> dev.hoi.client.HoiClient.openWorldTension(), message -> message.get()) {
                @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play("ui.click"); }
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {}
            });
        }
        buttons.add(new HudMusicButton(width));
        return List.copyOf(buttons);
    }
    public static String flagTexture(String country) {
        return CountryDisplays.flag(country);
    }
    public static final class HudTabButton extends Button {
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play("ui.menu_tab"); }
        private final MenuTab tab;
        private final String country;
        private final boolean selected;
        private final int screenWidth;
        private HudTabButton(MenuTab tab, String country, boolean selected, int screenWidth, Runnable action) {
            this(tab, country, selected, screenWidth, bounds(tab, scale(screenWidth)), action);
        }
        private HudTabButton(MenuTab tab, String country, boolean selected, int screenWidth, int[] bounds, Runnable action) {
            super(bounds[0], bounds[1], bounds[2], bounds[3], Component.literal(tab.label() + " 메뉴"), b -> action.run(), DEFAULT_NARRATION);
            this.tab = tab; this.country = country; this.selected = selected; this.screenWidth = screenWidth;
            if (tab != MenuTab.POLITICS) setTooltip(dev.hoi.client.ui.tooltip.Tooltips.create(Component.literal(tab.label())));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            drawTab(g, tab, country, selected, screenWidth, snapshot(null));
            if (tab == MenuTab.POLITICS && Tooltips.contains(this, mx, my)) {
                String name = CountryDisplays.name(country, "");
                if (!name.isBlank()) Tooltips.drawExact(g, Minecraft.getInstance().font, flagTooltip(name), mx, my);
            }
        }
        @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            if (tab != MenuTab.POLITICS || event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT || !active || !visible || !isMouseOver(event.x(), event.y()))
                return super.mouseClicked(event, doubleClick);
            toggleOwnDiplomacy(country);
            return true;
        }
    }
    public static String flagTooltip(String name) {
        return name + "\n" + dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("TOPBAR_FLAG_DESC");
    }
    public static void toggleOwnDiplomacy(String country) {
        if (country == null || country.isBlank()) return;
        if (dev.hoi.client.screen.dialog.DialogCoordinator.contentScreen() instanceof dev.hoi.client.screen.diplomacy.DiplomacyCountryScreen screen && country.equals(screen.target())) {
            AudioUiSounds.play("ui.menu_tab"); screen.onClose(); return;
        }
        dev.hoi.client.network.NetworkScreens.openCountry(country);
    }
    private static int[] bounds(MenuTab tab, float s) {
        boolean flag = tab == MenuTab.POLITICS;
        int x = flag ? FLAG_X : TAB_X[tab.ordinal() - 1], y = flag ? FLAG_Y : TAB_Y, w = flag ? FLAG_WIDTH : TAB_WIDTH, h = flag ? FLAG_HEIGHT : TAB_HEIGHT;
        int left = Math.round(x * s / 2), top = Math.round(y * s / 2);
        return new int[]{left, top, Math.round((x + w) * s / 2) - left, Math.round((y + h) * s / 2) - top};
    }
    public static void drawFlag(GuiGraphicsExtractor g, String country, int x, int y, int w, int h) {
        UiMenuStyle.metal(g, x, y, w, h);
        flag(g, country, x, y, w, h);
    }
    private static void flag(GuiGraphicsExtractor g, String country, int x, int y, int w, int h) {
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
    private static void drawTab(GuiGraphicsExtractor g, MenuTab tab, String country, boolean selected, int width, CountryHud hud) {
        if (dev.hoi.client.screen.dialog.DialogCoordinator.renderingContent()) return;
        float s = scale(width), k = s / 2;
        boolean drawn = true;
        g.pose().pushMatrix();
        g.pose().scale(k, k);
        if (tab == MenuTab.POLITICS) flag(g, country, FLAG_X, FLAG_Y, FLAG_WIDTH, FLAG_HEIGHT);
        else drawn = UiAssets.draw(g, "menu/" + tab.id() + (selected ? "_selected" : ""), TAB_X[tab.ordinal() - 1], TAB_Y, TAB_WIDTH, TAB_HEIGHT);
        g.pose().popMatrix();
        var font = Minecraft.getInstance().font;
        int[] bounds = bounds(tab, s);
        if (!drawn) g.centeredText(font, "◇", bounds[0] + bounds[2] / 2, bounds[1] + (bounds[3] - 8) / 2, UiMenuStyle.ACCENT);
        Integer count = hud.availableDecisions();
        if (tab == MenuTab.DECISIONS && count != null && count > 0) {
            int x = Math.round(57 * s), y = Math.round(36 * s) - 13;
            UiAssets.draw(g, "mio/ui/gfx_topbar_alert_bg/frame_0", x, y, 13, 13);
            UiText.centered(g, font, Integer.toString(count), x - 4, y, 21, 13, 0xFFFFFFFF, false);
        }
    }
}
