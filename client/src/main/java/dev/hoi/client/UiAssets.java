package dev.hoi.client;

import dev.hoi.protocol.ResearchView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.io.DataInputStream;
import java.io.IOException;

/** Resource locations only. TFR/TFRKP pixels live in HOI-resourcepack, never this JAR. */
final class UiAssets {
    private static final Map<Identifier, int[]> DIMENSIONS = new HashMap<>();
    private UiAssets() {}
    static void clear() { DIMENSIONS.clear(); }
    /** Banner-only crop-to-fill: original aspect ratio, centered source UVs. */
    static boolean cover(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return false;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        if (size[0] <= 0 || size[1] <= 0) return false;
        double scale = Math.max((double)w / size[0], (double)h / size[1]);
        float u = (float)(w / (size[0] * scale)), v = (float)(h / (size[1] * scale));
        g.blit(id, x, y, x + w, y + h, (1 - u) / 2, (1 + u) / 2, (1 - v) / 2, (1 + v) / 2);
        return true;
    }
    static boolean draw(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return false;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        if (size[0] <= 0 || size[1] <= 0) return false;
        // Caller dimensions are a maximum box, never a stretched output size.
        double scale = Math.min((double) w / size[0], (double) h / size[1]);
        int fitW = Math.max(1, (int)Math.round(size[0] * scale)), fitH = Math.max(1, (int)Math.round(size[1] * scale));
        int left = x + (w - fitW) / 2, top = y + (h - fitH) / 2;
        g.blit(id, left, top, left + fitW, top + fitH, 0, 1, 0, 1);
        return true;
    }
    private static int[] dimensions(Identifier id) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isPresent()) try (var input = new DataInputStream(resource.get().open())) {
            // Read only the PNG IHDR; texture upload remains Minecraft's responsibility.
            if (input.readLong() == 0x89504E470D0A1A0AL && input.readInt() == 13 && input.readInt() == 0x49484452) {
                int w = input.readInt(), h = input.readInt();
                if (w > 0 && h > 0 && w <= 32768 && h <= 32768) return new int[]{w, h};
            }
        } catch (IOException ignored) { }
        return new int[]{0, 0};
    }
    static void technology(GuiGraphicsExtractor g, ResearchView.Tech tech, int x, int y, int w, int h, int color) {
        String id = tech.id();
        if (id.startsWith("hoi:") && draw(g, "technology/" + id.substring(4), x, y, w, h)) return;
        if (draw(g, "category/" + tech.category().toLowerCase(Locale.ROOT), x, y, w, h)) return;
        ResearchIcons.fallback(g, tech.category(), x + w / 2 - 10, y + h / 2 - 8, color, 2);
    }
}
