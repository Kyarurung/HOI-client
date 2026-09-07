package dev.hoi.client;

import dev.hoi.protocol.ResearchView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;

/** Vertical research slots styled as illustrated progress cards. */
final class ResearchSlotButton extends Button {
    private final ResearchView.Slot slot;
    private final ResearchView.Tech technology;

    ResearchSlotButton(ResearchView.Slot slot, ResearchView.Tech technology, int x, int y, int width, int height, Runnable action) {
        super(x, y, width, height, Component.literal("슬롯 " + (slot.index() + 1) + " · " + (technology == null ? "연구 선택" : technology.name())),
                b -> action.run(), DEFAULT_NARRATION);
        this.slot = slot; this.technology = technology;
        setTooltip(Tooltip.create(Component.literal(getMessage().getString()
                + (technology == null ? "" : " · " + technology.remainingDays(slot.savedDays()) + "일"))));
    }

    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        var font = Minecraft.getInstance().font;
        int color = isHoveredOrFocused() ? 0xFFE6C779 : 0xFF94B4C4;
        g.fillGradient(x, y, x + w, y + h, 0xFF3E5A70, 0xFF172731);
        // Keep the original slot artwork underneath the researched equipment image.
        UiAssets.draw(g, "panel/research_slot", x, y, w, h);
        g.outline(x, y, w, h, color);
        boolean compactActive = technology != null && h < 40;
        String title = (slot.index() + 1) + "." + (compactActive ? "" : " " + (technology == null ? "연구 선택" : technology.name()));
        if (font.width(title) > w - 20) title = font.plainSubstrByWidth(title, Math.max(1, w - 29)) + "…";
        int titleY = compactActive || h < 24 ? (h - 8) / 2 : technology == null ? 7 : 4;
        if (technology != null) {
            int imageWidth = Math.min(88, w / 2);
            int imageHeight = h >= 40 ? Math.min(30, h - 28) : Math.min(24, Math.max(6, h - (h >= 20 ? 12 : 4)));
            UiAssets.technology(g, technology, x + (w - imageWidth) / 2, y + (h - imageHeight) / 2,
                    imageWidth, imageHeight, color);
            String status = technology.remainingDays(slot.savedDays()) + "일";
            int statusX = compactActive ? x + w - 10 - font.width(status) : x + 10;
            g.text(font, status, statusX, y + (compactActive ? titleY : h - 13), color);
        }
        g.text(font, title, x + 10, y + titleY, 0xFFE0E6E8);
        if (h >= 20) {
            g.fill(x + 4, y + h - 5, x + w - 4, y + h - 2, 0xFF0C1217);
            if (technology != null) g.fill(x + 4, y + h - 5, x + 4 + (int)((w - 8) * technology.fraction()), y + h - 2, color);
        }
    }
}
