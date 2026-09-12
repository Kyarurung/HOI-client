package dev.hoi.client.ui;

import dev.hoi.client.research.ResearchDetails;
import dev.hoi.client.research.ResearchIcons;

import dev.hoi.protocol.ResearchView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.io.DataInputStream;
import java.io.IOException;


public final class UiAssets {
    private static final Map<Identifier, int[]> DIMENSIONS = new HashMap<>();
    private UiAssets() {}
    public static void clear() { DIMENSIONS.clear(); ResearchDetails.clear(); KeywordIcons.clear(); HoiMenuBar.clear(); }

    public static boolean cover(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return false;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        if (size[0] <= 0 || size[1] <= 0) return false;
        double scale = Math.max((double)w / size[0], (double)h / size[1]);
        float u = (float)(w / (size[0] * scale)), v = (float)(h / (size[1] * scale));
        g.blit(id, x, y, x + w, y + h, (1 - u) / 2, (1 + u) / 2, (1 - v) / 2, (1 + v) / 2);
        return true;
    }
    public static boolean draw(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) {
        return draw(g, path, x, y, w, h, 0, 0);
    }

    public static boolean draw(GuiGraphicsExtractor g, String path, int x, int y, int w, int h, int trimLeft, int trimRight) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return false;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        if (size[0] <= 0 || size[1] <= 0) return false;

        if (path.startsWith("politics/appointments/") && size[0] == 156 && size[1] == 210) {
            float scale = Math.min(w / 62f, h / 67f);
            g.pose().pushMatrix();
            g.pose().translate(x + (w - 62 * scale) / 2, y + (h - 67 * scale) / 2);
            g.pose().scale(scale);
            draw(g, "politics/vacant", 0, 0, 62, 67);
            g.pose().translate(30, 33);
            g.pose().rotate(-.07f);
            cover(g, path, -16, -23, 32, 46);
            g.pose().popMatrix();
            return true;
        }
        int sourceW = size[0] - trimLeft - trimRight;
        if (sourceW <= 0) return false;
        double scale = Math.min((double) w / sourceW, (double) h / size[1]);
        int fitW = Math.max(1, (int)Math.round(sourceW * scale)), fitH = Math.max(1, (int)Math.round(size[1] * scale));
        int left = x + (w - fitW) / 2, top = y + (h - fitH) / 2;
        g.blit(id, left, top, left + fitW, top + fitH, (float)trimLeft / size[0], 1 - (float)trimRight / size[0], 0, 1);
        return true;
    }
    public static void nineSlice(GuiGraphicsExtractor g, String path, int x, int y, int w, int h, int border, double scale) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        if (size[0] <= 2 * border || size[1] <= 2 * border) {draw(g, path, x, y, w, h); return;}
        int edge = Math.min(Math.min(w, h) / 2, Math.max(1, (int)Math.round(border * scale)));
        int[] xs = {x, x + edge, x + w - edge, x + w}, ys = {y, y + edge, y + h - edge, y + h};
        float[] us = {0, (float)border / size[0], 1 - (float)border / size[0], 1}, vs = {0, (float)border / size[1], 1 - (float)border / size[1], 1};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            g.blit(id, xs[col], ys[row], xs[col + 1], ys[row + 1], us[col], us[col + 1], vs[row], vs[row + 1]);
    }
    private static int[] dimensions(Identifier id) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isPresent()) try (var input = new DataInputStream(resource.get().open())) {

            if (input.readLong() == 0x89504E470D0A1A0AL && input.readInt() == 13 && input.readInt() == 0x49484452) {
                int w = input.readInt(), h = input.readInt();
                if (w > 0 && h > 0 && w <= 32768 && h <= 32768) return new int[]{w, h};
            }
        } catch (IOException ignored) { }
        return new int[]{0, 0};
    }

    public static String technologyPath(String country, ResearchView.Tech tech) {
        if (!tech.id().startsWith("hoi:")) return "";
        String common = "technology/" + tech.id().substring(4);
        if (country != null && country.matches("[A-Za-z0-9_]{1,16}")) {
            String national = "technology/country/" + country.toLowerCase(Locale.ROOT) + "/" + tech.id().substring(4);
            if (hasTexture(national)) return national;
        }
        return hasTexture(common) ? common : "";
    }
    private static boolean hasTexture(String path) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        return id != null && DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions)[0] > 0;
    }
    public static void technology(GuiGraphicsExtractor g, String country, ResearchView.Tech tech, int x, int y, int w, int h, int color) {
        String path = technologyPath(country, tech);
        if (!path.isEmpty() && draw(g, path, x, y, w, h)) return;
        if (draw(g, "category/" + tech.category().toLowerCase(Locale.ROOT), x, y, w, h)) return;
        ResearchIcons.fallback(g, tech.category(), x + w / 2 - 10, y + h / 2 - 8, color, 2);
    }
}
