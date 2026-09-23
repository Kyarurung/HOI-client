package dev.hoi.client.ui;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class UiFlagStack {
    private final List<String> order = new ArrayList<>();
    public List<String> update(List<String> incoming) {
        var remaining = new LinkedHashSet<>(incoming);
        order.removeIf(id -> !remaining.remove(id));
        order.addAll(remaining);
        return List.copyOf(order);
    }
    public static int left(int index, int count, int x, int width, int flagWidth) {
        double step = count <= 1 ? 0 : Math.min(flagWidth, Math.max(0, width - flagWidth) / (double)(count - 1));
        return x + (int)Math.round(index * step);
    }
    public static int visibleWidth(int index,int count,int x,int width,int flagWidth) {
        return index+1<count?left(index+1,count,x,width,flagWidth)-left(index,count,x,width,flagWidth):flagWidth;
    }
    public static void draw(GuiGraphicsExtractor g, String icon, int x, int y, int width, int height) {
        int frameX = x + Math.round(width * 2f / 52), frameY = y + Math.round(height * 3f / 38);
        int frameW = Math.round(width * 47f / 52), frameH = Math.round(height * 32f / 38);
        double scale = Math.min(frameW / 47.0, frameH / 32.0);
        int fitW = (int)Math.round(47 * scale), fitH = (int)Math.round(32 * scale);
        g.pose().pushMatrix();
        g.pose().translate(frameX + (frameW - fitW) / 2, frameY + (frameH - fitH) / 2);
        g.pose().scale(fitW / 47f, fitH / 32f);
        UiAssets.cover(g, icon, 5, 5, 37, 22);
        g.pose().popMatrix();
        UiAssets.draw(g, "map_detail/flag_frame_thin", frameX, frameY, frameW, frameH);
        UiAssets.draw(g, "map_detail/flag_frame", x, y, width, height);
    }
}
