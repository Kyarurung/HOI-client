package dev.hoi.client;

import dev.hoi.protocol.CountryHud;
import dev.hoi.protocol.HudDetail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

/** Uses Minecraft's final tooltip layer, with a viewport-sized scroll window. */
final class HudTooltip {
    private static Object screen;
    private static String key;
    private static int offset, maximum;
    private HudTooltip() {}

    static boolean scroll(double amount) {
        var client = Minecraft.getInstance();
        if (screen != client.gui.screen() || key == null || maximum == 0) return false;
        var window = client.getWindow();
        if (!com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                && !com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT)) return false;
        offset = Math.clamp(offset - (int)(amount * 3), 0, maximum);
        return true;
    }

    static List<FormattedCharSequence> body(Font font, int width, CountryHud hud, HoiMenuBar.Indicator item) {
        int wrap = Math.max(60, Math.min(310, width - 36));
        var lines = new ArrayList<FormattedCharSequence>();
        String current = item.raw() == null ? "—" : item.format() == HoiMenuBar.Format.PERCENT
                ? HoiMenuBar.rawNumber(item.raw().doubleValue() * 100) + "%" : HoiMenuBar.rawNumber(item.raw());
        lines.addAll(font.split(Component.literal("현재: " + current), wrap));
        var detail = hud.details().get(item.icon());
        if (detail != null) for (var row : detail.rows()) {
            int color = switch (row.tone()) {
                case GOOD -> 0x94BA8A; case BAD -> 0xCB9292; case MUTED -> 0x9BABB4;
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
        return List.copyOf(lines);
    }

    static int visibleRows(int height, Font font) { return Math.max(1, (height - 52) / (font.lineHeight + 1)); }

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
        // The vanilla positioner starts twelve pixels above the pointer and does not clamp the top edge.
        g.setTooltipForNextFrame(font, visible, mx, Math.max(16, my));
    }
}
