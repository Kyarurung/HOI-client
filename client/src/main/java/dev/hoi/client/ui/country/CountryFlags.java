package dev.hoi.client.ui.country;

import com.mojang.blaze3d.platform.NativeImage;
import dev.hoi.client.ui.UiAssets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class CountryFlags {
    private static final Set<String> STYLES = Set.of("small2", "small", "smallest");
    private static final Map<String, Identifier> MASKED = new HashMap<>();
    private static int generation;
    private CountryFlags() {}
    public static boolean draw(GuiGraphicsExtractor g, String path, String style, int x, int y, int w, int h) {
        return draw(g, path, style, (float) x, (float) y, w, h);
    }
    public static boolean draw(GuiGraphicsExtractor g, String path, String style, float x, float y, float w, float h) {
        if (!STYLES.contains(style)) throw new IllegalArgumentException("Unknown flag style");
        var texture = masked(CountryDisplays.texture(path), style);
        if (texture == null) return false;
        int[] mask = UiAssets.imageSize("flags/" + style + "/mask"), overlay = UiAssets.imageSize("flags/" + style + "/overlay");
        if (mask[0] <= 0 || mask[1] <= 0) return false;
        float fit = Math.min(w / mask[0], h / mask[1]);
        g.pose().pushMatrix();
        g.pose().translate(x + (w - mask[0] * fit) / 2, y + (h - mask[1] * fit) / 2);
        g.pose().scale(fit, fit);
        g.blit(texture, 0, 0, mask[0], mask[1], 0, 1, 0, 1);
        g.pose().translate((mask[0] - overlay[0]) / 2f, (mask[1] - overlay[1]) / 2f);
        UiAssets.draw(g, "flags/" + style + "/overlay", 0, 0, overlay[0], overlay[1]);
        g.pose().popMatrix();
        return true;
    }
    public static boolean framed(GuiGraphicsExtractor g, String path, String frame, int frameX, int frameY, int flagW, int flagH) {
        int[] size = UiAssets.imageSize(frame);
        boolean drawn = draw(g, path, "small2", frameX + (size[0] - flagW) / 2f, frameY + (size[1] - flagH) / 2f, flagW, flagH);
        UiAssets.draw(g, frame, frameX, frameY, size[0], size[1]);
        return drawn;
    }
    public static void clear() {
        MASKED.values().forEach(id -> { if (id != null) Minecraft.getInstance().getTextureManager().release(id); });
        MASKED.clear();
    }
    private static Identifier masked(String path, String style) {
        String key = style + "|" + path;
        if (MASKED.containsKey(key)) return MASKED.get(key);
        Identifier id = null;
        try (var flag = read(path); var mask = read("flags/" + style + "/mask")) {
            if (flag != null && mask != null) {
                var image = new NativeImage(flag.getWidth(), flag.getHeight(), false);
                DynamicTexture texture;
                try {
                    for (int row = 0; row < flag.getHeight(); row++) for (int column = 0; column < flag.getWidth(); column++) {
                        int alpha = mask.getPixel(column * mask.getWidth() / flag.getWidth(), row * mask.getHeight() / flag.getHeight()) >>> 24;
                        int color = flag.getPixel(column, row);
                        image.setPixel(column, row, color & 0xFFFFFF | (alpha * (color >>> 24) / 255) << 24);
                    }
                    texture = new DynamicTexture(() -> "HOI masked flag " + key, image);
                } catch (RuntimeException | Error failure) {
                    image.close();
                    throw failure;
                }
                id = Identifier.fromNamespaceAndPath("hoi", "masked_flag/" + style + "/" + generation++);
                try { Minecraft.getInstance().getTextureManager().register(id, texture); }
                catch (RuntimeException | Error failure) { texture.close(); throw failure; }
            }
        } catch (java.io.IOException ignored) {
        }
        MASKED.put(key, id);
        return id;
    }
    private static NativeImage read(String path) throws java.io.IOException {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        var resource = id == null ? java.util.Optional.<net.minecraft.server.packs.resources.Resource>empty() : Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isEmpty()) return null;
        try (var input = resource.get().open()) { return NativeImage.read(input); }
    }
}
