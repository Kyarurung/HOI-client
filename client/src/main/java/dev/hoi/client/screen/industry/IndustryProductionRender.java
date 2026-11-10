package dev.hoi.client.screen.industry;

import dev.hoi.client.ui.UiText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

final class IndustryProductionRender {
    private IndustryProductionRender() {}
    static void bar(GuiGraphicsExtractor g, String name, int x, int y, int w, int h, double value) {
        float ratio = (float)Math.clamp(value, 0, 1);
        var background = Identifier.parse("hoi:textures/gui/production/line/" + name + "_bg.png");
        var fill = Identifier.parse("hoi:textures/gui/production/line/" + name + "_fill.png");
        g.blit(background, x, y, x + w, y + h, 0, 1, 0, 1);
        int filled = Math.round(w * ratio);
        if (filled > 0) g.blit(fill, x, y, x + filled, y + h, 0, ratio, 1f / 3, 2f / 3);
    }
    static void text(GuiGraphicsExtractor g, Font font, String value, int x, int y, int w, int h, int color) {
        if (g == null) return;
        int textWidth = font.width(value);
        float scale = Math.min(UiText.scale(font), Math.min((w - 2f) / Math.max(1, textWidth), h / (float)font.lineHeight));
        g.pose().pushMatrix();
        g.pose().translate(x + (w - textWidth * scale) / 2, UiText.centeredY(font, value, y, h, scale));
        g.pose().scale(scale);
        g.text(font, value, 0, 0, color);
        g.pose().popMatrix();
    }
}
