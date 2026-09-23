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
    private static final Map<Identifier, int[]> CONTENT_BOUNDS = new HashMap<>();
    private UiAssets() {}
    public static void clear() { DIMENSIONS.clear(); CONTENT_BOUNDS.clear(); ResearchDetails.clear(); UiKeywordIcons.clear(); UiMenuBar.clear(); UiEquipmentDesigns.clear(); }

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
    public static boolean drawBottom(GuiGraphicsExtractor g,String path,int x,int y,int width,int height) {
        int fittedHeight=Math.min(height,imageHeight(path,width));
        return fittedHeight>0&&draw(g,path,x,y+height-fittedHeight,width,fittedHeight);
    }
    public static boolean drawContent(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return false;
        int[] bounds = CONTENT_BOUNDS.computeIfAbsent(id, UiAssets::contentBounds);
        if (bounds.length == 0) return false;
        int sourceW = bounds[4] - bounds[2], sourceH = bounds[5] - bounds[3];
        double scale = Math.min((double)w / sourceW, (double)h / sourceH);
        int fitW = Math.max(1, (int)Math.round(sourceW * scale)), fitH = Math.max(1, (int)Math.round(sourceH * scale));
        int left = x + (w - fitW) / 2, top = y + (h - fitH) / 2;
        g.blit(id, left, top, left + fitW, top + fitH, (float)bounds[2] / bounds[0], (float)bounds[4] / bounds[0], (float)bounds[3] / bounds[1], (float)bounds[5] / bounds[1]);
        return true;
    }
    public static boolean drawAlignedContent(GuiGraphicsExtractor g,String path,String reference,float x,float y,float w,float h) {
        var id=Identifier.tryParse("hoi:textures/gui/"+path+".png");
        var anchor=Identifier.tryParse("hoi:textures/gui/"+reference+".png");
        if(id==null||anchor==null||w<=0||h<=0)return false;
        int[] bounds=CONTENT_BOUNDS.computeIfAbsent(anchor,UiAssets::contentBounds);
        int[] imageSize=DIMENSIONS.computeIfAbsent(id,UiAssets::dimensions);
        if(bounds.length==0||imageSize[0]!=bounds[0]||imageSize[1]!=bounds[1])return false;
        int contentW=bounds[4]-bounds[2],contentH=bounds[5]-bounds[3];
        float scale=Math.min(w/imageSize[0],h/imageSize[1]);
        float guiScale=Minecraft.getInstance().getWindow().getGuiScale();
        float left=x+w/2f-(bounds[2]+contentW/2f)*scale,top=y+h/2f-(bounds[3]+contentH/2f)*scale;
        g.pose().pushMatrix();
        g.pose().translate(Math.round(left*guiScale)/guiScale,Math.round(top*guiScale)/guiScale);
        g.pose().scale(scale);
        g.blit(id,0,0,imageSize[0],imageSize[1],0,1,0,1);
        g.pose().popMatrix();
        return true;
    }
    private static int[] contentBounds(Identifier id) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isPresent()) try (var input = resource.get().open()) {
            var image = javax.imageio.ImageIO.read(input);
            if (image == null) return new int[0];
            int width = image.getWidth(), height = image.getHeight(), left = width, top = height, right = 0, bottom = 0;
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                if ((image.getRGB(x, y) >>> 24) == 0) continue;
                left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x + 1); bottom = Math.max(bottom, y + 1);
            }
            return right > left && bottom > top ? new int[]{width, height, left, top, right, bottom} : new int[0];
        } catch (IOException ignored) { }
        return new int[0];
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
    public static int imageHeight(String path, int width) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null) return 0;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        return size[0] > 0 ? Math.max(1, Math.round(width * (float) size[1] / size[0])) : 0;
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
