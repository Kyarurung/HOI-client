package dev.hoi.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public record UiScrollBar(int x, int top, int bottom, int visible, int total) {
    private static final Identifier BACKGROUND = Identifier.parse("hoi:textures/gui/commander/commands/scrollbar_vertical_bg/frame_0.png");
    public static void background(GuiGraphicsExtractor graphics, int x, int top, int bottom) {
        int edge = Math.min((bottom - top) / 2, 26);
        graphics.blit(BACKGROUND, x, top, x + 9, top + edge, 0, 1, 0, edge / 78f);
        graphics.blit(BACKGROUND, x, top + edge, x + 9, bottom - edge, 0, 1, 1 / 3f, 2 / 3f);
        graphics.blit(BACKGROUND, x, bottom - edge, x + 9, bottom, 0, 1, 1 - edge / 78f, 1);
    }
    public int maximum() { return Math.max(0, total - visible); }
    public int trackHeight() { return Math.max(1, bottom - top - 16); }
    public int thumbHeight() { return 8; }
    public boolean contains(double mouseX, double mouseY) { return maximum() > 0 && mouseX >= x && mouseX < x + 9 && mouseY >= top && mouseY < bottom; }
    public int offset(double mouseY) {
        return (int) Math.round(Math.clamp((mouseY - top - 8 - thumbHeight() / 2.0) / Math.max(1, trackHeight() - thumbHeight()), 0, 1) * maximum());
    }
    public int step(double mouseY, int offset, int amount) {
        return Math.clamp(offset + (mouseY < top + 8 ? -amount : amount), 0, maximum());
    }
    public boolean arrow(double mouseY) { return mouseY < top + 8 || mouseY >= bottom - 8; }
    public void draw(GuiGraphicsExtractor graphics, int offset) {
        if (maximum() == 0) return;
        background(graphics, x, top, bottom);
        graphics.pose().pushMatrix();
        graphics.pose().translate(.5f, 0);
        UiAssets.nineSlice(graphics, "commander/commands/scroll_track/frame_0", x + 1, top + 8, 6, trackHeight(), 4, .5);
        int y = top + 8 + Math.clamp(offset, 0, maximum()) * (trackHeight() - thumbHeight()) / maximum();
        UiAssets.draw(graphics, "commander/commands/scroll_drager/frame_0", x, y, 8, thumbHeight());
        UiAssets.draw(graphics, "commander/commands/scroll_up/frame_0", x, top + 1, 8, 8);
        UiAssets.draw(graphics, "commander/commands/scroll_down/frame_0", x, bottom - 8, 8, 8);
        graphics.pose().popMatrix();
    }
}
