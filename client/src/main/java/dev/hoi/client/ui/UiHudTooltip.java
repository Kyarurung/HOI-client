package dev.hoi.client.ui;

import dev.hoi.protocol.country.CountryHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

public final class UiHudTooltip {
    private static Font cachedFont;
    private static CountryHud cachedHud;
    private static UiMenuBar.UiIndicator cachedItem;
    private static int cachedWidth;
    private static List<FormattedCharSequence> cachedBody = List.of();
    private UiHudTooltip() {}
    public static void clear() {
        cachedFont = null; cachedHud = null; cachedItem = null; cachedBody = List.of();
    }
    public static List<FormattedCharSequence> body(Font font, int width, CountryHud hud, UiMenuBar.UiIndicator item) {
        if (font == cachedFont && hud == cachedHud && item.equals(cachedItem) && width == cachedWidth) return cachedBody;
        int wrap = Math.max(60, (int)(Math.min(item.icon().equals("gdp") ? 360 : 310, width - 36) / UiText.scale(font)));
        var lines = new ArrayList<FormattedCharSequence>();
        var original = hud.details().get(item.icon());
        if (original != null && original.sourceText() != null) {
            for (String line : UiTooltipText.colorLines(UiCommandLabels.withoutShortcuts(original.sourceText()))) {
                if (UiTooltipText.blank(line)) lines.add(FormattedCharSequence.EMPTY);
                else lines.addAll(UiTooltips.split(font,UiKeywordIcons.decorateLines(UiTooltips.styled(Component.literal(line).withColor(0xFFFFFF), false), false), item.icon().equals("gdp") ? wrap : Math.min(175, wrap)));
            }
            cachedFont = font; cachedHud = hud; cachedItem = item; cachedWidth = width;
            cachedBody = UiTooltips.collapseBlankLines(lines); return cachedBody;
        }
        if (item.icon().equals("fuel")) lines.add(FormattedCharSequence.EMPTY);
        String current = (item.icon().equals("party_support") || item.icon().equals("supplies")) ? item.value() : item.raw() == null ? "0" : item.format() == UiMenuBar.UiFormat.PERCENT
                ? UiMenuBar.rawNumber(item.raw().doubleValue() * 100) + "%" : UiMenuBar.rawNumber(item.raw());
        if (!item.icon().equals("fuel") && !item.icon().equals("world_tension") && !item.icon().equals("convoys") && !item.icon().equals("supplies") && UiTooltipText.visible(current)) lines.addAll(UiTooltips.split(font,Component.literal("현재: " + current).withColor(UiMenuBar.indicatorColor(item)), wrap));
        var detail = hud.details().get(item.icon());
        if (detail != null) for (var row : detail.rows()) {
            if (UiTooltipText.blank(row.label()) && row.value().isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            int color = switch (row.tone()) {
                case GOOD -> UiTooltips.GOOD; case BAD -> UiTooltips.BAD; case MUTED -> 0x9BABB4;
                case HEADING -> UiTooltips.GOLD; default -> item.icon().equals("fuel") || item.icon().equals("supplies") ? 0xFFFFFF : 0xE0E6E8;
            };
            String value = row.value();
            var text = Component.literal(row.label() + (value.isEmpty() ? "" : ": " + value))
                    .withStyle(style -> style.withColor(color));
            lines.addAll(UiTooltips.split(font,item.icon().equals("fuel") ? fuelNumbers(text.getString(), color) : item.icon().equals("convoys") ? convoyNumbers(text.getString(), color) : UiTooltips.styled(text, false), wrap));
        } else {
            if (item.barLabel() != null && item.ratio() != null && item.ratio() != 0) lines.addAll(UiTooltips.split(font,Component.literal(item.barLabel() + ": "
                    + (item.ratio() == null ? "0" : UiMenuBar.rawNumber(item.ratio() * 100) + "%")), wrap));
            lines.addAll(UiTooltips.split(font,Component.literal("세부 내역 없음").withStyle(s -> s.withColor(0x9BABB4)), wrap));
        }
        cachedFont = font; cachedHud = hud; cachedItem = item; cachedWidth = width;
        cachedBody = UiTooltips.collapseBlankLines(lines); return cachedBody;
    }
    private static Component fuelNumbers(String text,int color) {
        var result=Component.empty();
        var matcher=java.util.regex.Pattern.compile("£resources_strip\\|[0-9]+|[0-9]+(?:\\.[0-9]+)?[KM]?(?:일)?|연료|저장고|보급선").matcher(text);
        int start=0;
        while(matcher.find()) {
            result.append(Component.literal(text.substring(start,matcher.start())).withColor(color));
            String part=matcher.group();
            int tint=part.startsWith("£")?0xFFFFFF:part.endsWith("일")?color:UiTooltips.GOLD;
            result.append(Component.literal(part).withColor(tint));start=matcher.end();
        }
        result.append(Component.literal(text.substring(start)).withColor(color));
        return UiKeywordIcons.decorateLines(result);
    }
    private static Component convoyNumbers(String text,int color) {
        var result=Component.empty();
        var matcher=java.util.regex.Pattern.compile("[0-9]+").matcher(text);int start=0;
        while(matcher.find()) {
            result.append(Component.literal(text.substring(start,matcher.start())).withColor(color));
            result.append(Component.literal(matcher.group()).withColor(UiTooltips.GOLD));start=matcher.end();
        }
        return result.append(Component.literal(text.substring(start)).withColor(color));
    }
    static void draw(GuiGraphicsExtractor g, int width, CountryHud hud, UiMenuBar.UiIndicator item, int mx, int my) {
        var client = Minecraft.getInstance();
        if (item == null) return;
        var font = client.font;
        var content = body(font, width, hud, item);
        var visible = new ArrayList<FormattedCharSequence>();
        if (!item.icon().equals("supplies") && !(hud.details().containsKey(item.icon()) && hud.details().get(item.icon()).sourceText() != null && List.of("gdp", "debt").contains(item.icon()))) {
            Component title = Component.literal(item.label()).withColor(UiTooltips.GOLD);
            if (item.icon().equals("manpower")) title = UiKeywordIcons.decorateLines(Component.literal("£manpower_texticon ").append(title));
            visible.add(title.getVisualOrderText());
        }
        visible.addAll(content);
        g.setTooltipForNextFrame(font, UiTooltips.collapseBlankLines(visible), mx, my);
    }
}
