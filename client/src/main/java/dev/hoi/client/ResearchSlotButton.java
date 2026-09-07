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
        setTooltip(Tooltip.create(Component.literal(getMessage().getString() + " · 저장된 연구 " + slot.savedDays() + "일")));
    }

    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        var font = Minecraft.getInstance().font;
        int color = isHoveredOrFocused() ? 0xFFE6C779 : 0xFF94B4C4;
        g.fillGradient(x, y, x + w, y + h, 0xFF3E5A70, 0xFF172731);
        UiAssets.draw(g, "panel/research_slot", x, y, w, h);
        g.outline(x, y, w, h, color);
        String title = (slot.index() + 1) + ". " + (technology == null ? "연구 선택" : technology.name());
        if (font.width(title) > w - 8) title = font.plainSubstrByWidth(title, Math.max(1, w - 17)) + "…";
        g.text(font, title, x + 4, y + 3, 0xFFE0E6E8);
        if (h >= 30) {
            String status = technology == null ? "저장 " + slot.savedDays() + "일" : technology.remainingDays(slot.savedDays()) + "일";
            g.text(font, status, x + 5, y + h - 18, color);
        }
        if (h >= 46 && technology != null) UiAssets.technology(g, technology, x + w - 51, y + 17, 44, h - 25, color);
        g.fill(x + 4, y + h - 5, x + w - 4, y + h - 2, 0xFF0C1217);
        if (technology != null) g.fill(x + 4, y + h - 5, x + 4 + (int)((w - 8) * technology.fraction()), y + h - 2, color);
    }
}
