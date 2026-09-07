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
    private static final int GOLD = 0xFFE6C779;

    private HoiMenuBar() {}

    static int height(int width) { return width >= 600 ? 42 : 32; }

    static void draw(GuiGraphicsExtractor g, int width) {
        g.fillGradient(0, 0, width, height(width), 0xFF343B41, 0xFF0B0E11);
    }

    static List<TabButton> buttons(int width, String country, MenuTab selected, Consumer<MenuTab> select) {
        int tabWidth = Math.min(78, (width - 12) / MenuTab.ORDER.size());
        return MenuTab.ORDER.stream().map(tab -> new TabButton(tab, country, selected == tab,
                6 + tab.ordinal() * tabWidth, tabWidth - 3, height(width) - 6, () -> select.accept(tab))).toList();
    }

    static final class TabButton extends Button {
        private final MenuTab tab;
        private final String country;
        private final boolean selected;

        private TabButton(MenuTab tab, String country, boolean selected, int x, int width, int height, Runnable action) {
            super(x, 3, width, height, Component.literal(tab.label() + " 메뉴"), b -> action.run(), DEFAULT_NARRATION);
            this.tab = tab; this.country = country; this.selected = selected;
            setTooltip(Tooltip.create(Component.literal(tab.label())));
        }

        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            var font = Minecraft.getInstance().font;
            g.fillGradient(x, y, x + w, y + h, selected ? 0xFF526341 : 0xFF2B3227, 0xFF0B100C);
            g.outline(x, y, w, h, isHoveredOrFocused() || selected ? GOLD : 0xFF596052);
            String texture = tab == MenuTab.POLITICS && !country.isEmpty()
                    ? "country/" + country.toLowerCase(Locale.ROOT) + "/flag" : "menu/" + tab.id();
            int iconWidth = Math.min(54, w - 8);
            if (!UiAssets.draw(g, texture, x + (w - iconWidth) / 2, y + 3, iconWidth, h - 6))
                g.centeredText(font, "◇", x + w / 2, y + (h - 8) / 2, GOLD);
        }
    }
}
