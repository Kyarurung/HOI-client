package dev.hoi.client.ui.hud;

import dev.hoi.protocol.country.CountryHud;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.client.ui.tooltip.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

public final class HudTooltip {
    private static Font cachedFont;
    private static CountryHud cachedHud;
    private static HudMenuBar.HudIndicator cachedItem;
    private static int cachedWidth;
    private static List<FormattedCharSequence> cachedBody = List.of();
    private static CountryHud activeHud;
    private static HudMenuBar.HudIndicator activeItem;
    private static net.minecraft.client.gui.screens.Screen activeScreen;
    private static Tooltips.ScrolledTooltip bounds;
    private static int scroll;
    private static GuiGraphicsExtractor pendingGraphics;
    private static Runnable pendingTooltip;
    private HudTooltip() {}
    public static void clear() {
        cachedFont = null; cachedHud = null; cachedItem = null; cachedBody = List.of();
        activeHud = null; activeItem = null; activeScreen = null; bounds = null; scroll = 0;
        pendingGraphics = null; pendingTooltip = null;
    }
    static boolean scroll(CountryHud hud, double vertical) {
        if (vertical == 0 || hud != activeHud || activeScreen == null || activeScreen != Minecraft.getInstance().gui.screen() || bounds == null || bounds.maxScroll() == 0) return false;
        scroll = Math.clamp(scroll - (int)(vertical * 30), 0, bounds.maxScroll());
        return true;
    }
    public static void extractDeferred(GuiGraphicsExtractor graphics) {
        if (pendingGraphics != graphics || pendingTooltip == null) return;
        var draw = pendingTooltip;
        pendingGraphics = null; pendingTooltip = null;
        draw.run();
    }
    public static List<FormattedCharSequence> body(Font font, int width, CountryHud hud, HudMenuBar.HudIndicator item) {
        if (font == cachedFont && hud == cachedHud && item.equals(cachedItem) && width == cachedWidth) return cachedBody;
        int wrap = Tooltips.width(width, 14);
        var lines = new ArrayList<FormattedCharSequence>();
        var original = hud.details().get(item.icon());
        if (original != null && original.sourceText() != null) {
            for (String line : TooltipText.colorLines(UiCommandLabels.withoutShortcuts(original.sourceText()))) {
                if (TooltipText.blank(line)) lines.add(FormattedCharSequence.EMPTY);
                else lines.addAll(Tooltips.split(font,TooltipKeywordIcons.decorateLines(Tooltips.styled(Component.literal(line).withColor(0xFFFFFF), false), false), wrap));
            }
            cachedFont = font; cachedHud = hud; cachedItem = item; cachedWidth = width;
            cachedBody = Tooltips.collapseBlankLines(lines); return cachedBody;
        }
        if (item.icon().equals("fuel")) lines.add(FormattedCharSequence.EMPTY);
        String current = (item.icon().equals("party_support") || item.icon().equals("supplies")) ? item.value() : item.raw() == null ? "0" : item.format() == HudMenuBar.HudFormat.PERCENT
                ? HudMenuBar.rawNumber(item.raw().doubleValue() * 100) + "%" : HudMenuBar.rawNumber(item.raw());
        if (!item.icon().equals("fuel") && !item.icon().equals("world_tension") && !item.icon().equals("convoys") && !item.icon().equals("supplies") && TooltipText.visible(current)) lines.addAll(Tooltips.split(font,Component.literal("현재: " + current).withColor(HudMenuBar.indicatorColor(item)), wrap));
        var detail = hud.details().get(item.icon());
        if (detail != null) for (var row : detail.rows()) {
            if (TooltipText.blank(row.label()) && row.value().isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            int color = switch (row.tone()) {
                case GOOD -> Tooltips.GOOD; case BAD -> Tooltips.BAD; case MUTED -> 0x9BABB4;
                case HEADING -> Tooltips.GOLD; default -> item.icon().equals("fuel") || item.icon().equals("supplies") ? 0xFFFFFF : 0xE0E6E8;
            };
            String value = row.value();
            var text = Component.literal(row.label() + (value.isEmpty() ? "" : ": " + value))
                    .withStyle(style -> style.withColor(color));
            lines.addAll(Tooltips.split(font,item.icon().equals("fuel") ? fuelNumbers(text.getString(), color) : item.icon().equals("convoys") ? convoyNumbers(text.getString(), color) : Tooltips.styled(text, false), wrap));
        } else {
            if (item.barLabel() != null && item.ratio() != null && item.ratio() != 0) lines.addAll(Tooltips.split(font,Component.literal(item.barLabel() + ": "
                    + (item.ratio() == null ? "0" : HudMenuBar.rawNumber(item.ratio() * 100) + "%")), wrap));
            lines.addAll(Tooltips.split(font,Component.literal("세부 내역 없음").withStyle(s -> s.withColor(0x9BABB4)), wrap));
        }
        cachedFont = font; cachedHud = hud; cachedItem = item; cachedWidth = width;
        cachedBody = Tooltips.collapseBlankLines(lines); return cachedBody;
    }
    private static Component fuelNumbers(String text,int color) {
        var result=Component.empty();
        var matcher=java.util.regex.Pattern.compile("£resources_strip\\|[0-9]+|[0-9]+(?:\\.[0-9]+)?[KM]?(?:일)?|연료|저장고|보급선").matcher(text);
        int start=0;
        while(matcher.find()) {
            result.append(Component.literal(text.substring(start,matcher.start())).withColor(color));
            String part=matcher.group();
            int tint=part.startsWith("£")?0xFFFFFF:part.endsWith("일")?color:Tooltips.GOLD;
            result.append(Component.literal(part).withColor(tint));start=matcher.end();
        }
        result.append(Component.literal(text.substring(start)).withColor(color));
        return TooltipKeywordIcons.decorateLines(result);
    }
    private static Component convoyNumbers(String text,int color) {
        var result=Component.empty();
        var matcher=java.util.regex.Pattern.compile("[0-9]+").matcher(text);int start=0;
        while(matcher.find()) {
            result.append(Component.literal(text.substring(start,matcher.start())).withColor(color));
            result.append(Component.literal(matcher.group()).withColor(Tooltips.GOLD));start=matcher.end();
        }
        return result.append(Component.literal(text.substring(start)).withColor(color));
    }
    static void draw(GuiGraphicsExtractor g, int width, CountryHud hud, HudMenuBar.HudIndicator item, int mx, int my) {
        var client = Minecraft.getInstance();
        pendingGraphics = null; pendingTooltip = null;
        if (item == null) { activeItem = null; bounds = null; scroll = 0; return; }
        if (hud != activeHud || !item.equals(activeItem) || activeScreen != client.gui.screen()) scroll = 0;
        activeHud = hud; activeItem = item; activeScreen = client.gui.screen();
        var font = client.font;
        var content = body(font, width, hud, item);
        var visible = new ArrayList<FormattedCharSequence>();
        if (!item.icon().equals("supplies") && !(hud.details().containsKey(item.icon()) && hud.details().get(item.icon()).sourceText() != null && List.of("gdp", "debt").contains(item.icon()))) {
            visible.addAll(font.split(Component.literal(item.label()).withColor(Tooltips.GOLD), Tooltips.width(width,14)));
        }
        visible.addAll(content);
        var lines = Tooltips.collapseBlankLines(visible);
        int height = client.getWindow().getGuiScaledHeight();
        if (lines.size() * (font.lineHeight + 1) + 14 > height) {
            pendingGraphics = g;
            pendingTooltip = () -> {
                bounds = Tooltips.drawLinesScrollable(g, font, lines, mx, my, width, height, scroll, false, false);
                scroll = bounds.scroll();
            };
        } else {
            bounds = null; scroll = 0;
            g.setTooltipForNextFrame(font, lines, mx, my);
        }
    }
}
