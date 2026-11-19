package dev.hoi.client.screen.industry;

import dev.hoi.client.ui.UiText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

final class IndustryProductionRender {
    private IndustryProductionRender() {}
    static void startEnd(GuiGraphicsExtractor g, String fill, String background, int x, int y, int w, int h, double value) {
        if (background != null) g.blit(texture(background), x, y, x + w, y + h, 0, 1, 0, 1);
        int filled = Math.round(w * (float)Math.clamp(value, 0, 1));
        if (filled <= 0) return;
        var texture = texture(fill);
        float u = filled / (float)w;
        g.blit(texture, x, y, x + filled, y + h, 0, u, 1f / 3, 2f / 3);
        g.blit(texture, x, y, x + filled, y + h, 0, u, 0, 1f / 3);
        g.blit(texture, x, y, x + filled, y + h, 1 - u, 1, 2f / 3, 1);
    }
    private static Identifier texture(String path) {
        return Identifier.parse("hoi:textures/gui/" + path + ".png");
    }
    static void text(GuiGraphicsExtractor g, Font font, String value, int x, int y, int w, int h, int color) {
        text(g, font, value, x, y, w, h, color, false);
    }
    static void text(GuiGraphicsExtractor g, Font font, String value, float x, float y, float w, float h, int color, boolean right) {
        if (g == null) return;
        int textWidth = font.width(value);
        float scale = Math.min(UiText.scale(font), Math.min((w - 2f) / Math.max(1, textWidth), h / (float)font.lineHeight));
        g.pose().pushMatrix();
        g.pose().translate(right ? x + w - textWidth * scale : x + (w - textWidth * scale) / 2, UiText.centeredY(font, value, y, h, scale));
        g.pose().scale(scale);
        g.text(font, value, 0, 0, color);
        g.pose().popMatrix();
    }
}
