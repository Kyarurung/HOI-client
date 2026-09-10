package dev.hoi.client.ui;

import dev.hoi.protocol.CountryHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;


public final class HudTooltip {
    private static Object screen;
    private static String key;
    private static int offset, maximum;
    private static Font cachedFont;
    private static CountryHud cachedHud;
    private static HoiMenuBar.Indicator cachedItem;
    private static int cachedWidth;
    private static List<FormattedCharSequence> cachedBody = List.of();
    private HudTooltip() {}
    public static void clear() {
        screen = null; key = null; offset = 0; maximum = 0;
        cachedFont = null; cachedHud = null; cachedItem = null; cachedBody = List.of();
    }

    public static boolean scroll(double amount) {
        var client = Minecraft.getInstance();
        if (screen != client.gui.screen() || key == null || maximum == 0) return false;
        var window = client.getWindow();
        if (!com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                && !com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT)) return false;
        offset = Math.clamp(offset - (int)(amount * 3), 0, maximum);
        return true;
    }

    public static List<FormattedCharSequence> body(Font font, int width, CountryHud hud, HoiMenuBar.Indicator item) {
        if (font == cachedFont && hud == cachedHud && item.equals(cachedItem) && width == cachedWidth) return cachedBody;
        int wrap = Math.max(60, (int)(Math.min(310, width - 36) / UiText.scale(font)));
        var lines = new ArrayList<FormattedCharSequence>();
        String current = item.raw() == null ? "—" : item.format() == HoiMenuBar.Format.PERCENT
                ? HoiMenuBar.rawNumber(item.raw().doubleValue() * 100) + "%" : HoiMenuBar.rawNumber(item.raw());
        if (!item.icon().equals("world_tension")) lines.addAll(font.split(Component.literal("현재: " + current), wrap));
        var detail = hud.details().get(item.icon());
        if (detail != null) for (var row : detail.rows()) {
            int color = switch (row.tone()) {
                case GOOD -> 0x55AA33; case BAD -> 0xDD4444; case MUTED -> 0x9BABB4;
                case HEADING -> 0xE6C779; default -> 0xE0E6E8;
            };
            var text = Component.literal(row.label() + (row.value().isEmpty() ? "" : ": " + row.value()))
                    .withStyle(style -> style.withColor(color));
            lines.addAll(font.split(text, wrap));
        } else {
            if (item.barLabel() != null) lines.addAll(font.split(Component.literal(item.barLabel() + ": "
                    + (item.ratio() == null ? "—" : HoiMenuBar.rawNumber(item.ratio() * 100) + "%")), wrap));
            lines.addAll(font.split(Component.literal("세부 내역 없음").withStyle(s -> s.withColor(0x9BABB4)), wrap));
        }
        cachedFont = font; cachedHud = hud; cachedItem = item; cachedWidth = width;
        cachedBody = List.copyOf(lines); return cachedBody;
    }

    public static int visibleRows(int height, Font font) { return Math.max(1, (height - 76) / 12); }

    static void draw(GuiGraphicsExtractor g, int width, CountryHud hud, HoiMenuBar.Indicator item, int mx, int my) {
        var client = Minecraft.getInstance();
        if (item == null) { key = null; maximum = 0; return; }
        if (screen != client.gui.screen() || !item.icon().equals(key)) offset = 0;
        screen = client.gui.screen(); key = item.icon();
        var font = client.font;
        var content = body(font, width, hud, item);
        int count = visibleRows(client.getWindow().getGuiScaledHeight(), font);
        maximum = Math.max(0, content.size() - count);
        offset = Math.clamp(offset, 0, maximum);
        var visible = new ArrayList<FormattedCharSequence>();
        visible.add(Component.literal(item.label()).withStyle(s -> s.withColor(0xE6C779)).getVisualOrderText());
        visible.addAll(content.subList(offset, Math.min(content.size(), offset + count)));
        if (maximum > 0) visible.add(Component.literal("Shift + 휠 · 설명 스크롤").withStyle(s -> s.withColor(0x9BABB4)).getVisualOrderText());

        if (!item.icon().equals("world_tension")) {
            g.setTooltipForNextFrame(font, visible, mx, Math.max(16, my)); return;
        }
        int boxWidth = Math.min(width - 12, Math.max(220, (int)Math.ceil(visible.stream().mapToInt(font::width).max().orElse(0) * UiText.scale(font)) + 12));
        int boxHeight = visible.size() * 12 + 10;
        int left = Math.max(6, width - boxWidth - 6);
        int top = Math.max(6, Math.min(HoiMenuBar.height(width) + 18, client.getWindow().getGuiScaledHeight() - boxHeight - 6));
        g.nextStratum();
        g.fill(left, top, left + boxWidth, top + boxHeight, 0xF0100812);
        g.outline(left, top, boxWidth, boxHeight, 0xFF873B98);
        for (int i=0; i<visible.size(); i++) UiText.text(g, font, visible.get(i), left + 6, top + 5 + i * 12, 0xFFFFFFFF);
    }
}
