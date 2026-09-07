package dev.hoi.client;

import dev.hoi.protocol.MenuTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Shared navigation only: country statistics and campaign clock belong to their own views. */
final class HoiMenuBar {
    private static final int GOLD = 0xFFE6C779, TEXT = 0xFFE0E3DD;

    private HoiMenuBar() {}

    static int height(int width) { return width >= 600 ? 42 : 32; }

    static void draw(GuiGraphicsExtractor g, int width) {
        g.fillGradient(0, 0, width, height(width), 0xFF343B41, 0xFF0B0E11);
    }

    static List<TabButton> buttons(int width, String country, MenuTab selected, Consumer<MenuTab> select) {
        int tabWidth = Math.min(78, (width - 12) / MenuTab.ORDER.size());
        return MenuTab.ORDER.stream().map(tab -> new TabButton(tab, country, selected == tab,
                6 + tab.ordinal() * tabWidth, tabWidth - 3, width >= 600, () -> select.accept(tab))).toList();
    }

    static final class TabButton extends Button {
        private final MenuTab tab;
        private final String country;
        private final boolean selected, labels;

        private TabButton(MenuTab tab, String country, boolean selected, int x, int width, boolean labels, Runnable action) {
            super(x, 3, width, labels ? 36 : 26, Component.literal(tab.label() + " 메뉴"), b -> action.run(), DEFAULT_NARRATION);
            this.tab = tab; this.country = country; this.selected = selected; this.labels = labels;
            setTooltip(Tooltip.create(Component.literal(tab.label())));
        }

        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            var font = Minecraft.getInstance().font;
            g.fillGradient(x, y, x + w, y + h, selected ? 0xFF526341 : 0xFF2B3227, 0xFF0B100C);
            g.outline(x, y, w, h, isHoveredOrFocused() || selected ? GOLD : 0xFF596052);
            String texture = tab == MenuTab.POLITICS && !country.isEmpty()
                    ? "country/" + country.toLowerCase(Locale.ROOT) + "/flag" : "menu/" + tab.id();
            int iconWidth = Math.min(30, w - 4);
            if (!UiAssets.draw(g, texture, x + (w - iconWidth) / 2, y + (labels ? 2 : 3), iconWidth, 20))
                g.centeredText(font, "◇", x + w / 2, y + 5, GOLD);
            if (labels) {
                String label = tab.label();
                if (font.width(label) > w - 6) label = font.plainSubstrByWidth(label, Math.max(1, w - 15)) + "…";
                g.centeredText(font, label, x + w / 2, y + 25, TEXT);
            }
        }
    }
}
