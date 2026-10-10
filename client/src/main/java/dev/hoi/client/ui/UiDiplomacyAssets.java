package dev.hoi.client.ui;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class UiDiplomacyAssets implements AutoCloseable {
    static final String BUTTON_STATE_SHA256 = "914fa2f451e2fe744d753f266c071b32a7afb5d2866085d14f0ed20ac5bf7f44";
    static final String NO_DOWN_SHA256 = "13901e2dc58659b84e01a84aedf64e8a9f3d45765dd013f6ff97c133622f1d05";
    private static final Set<UiDiplomacyAssets> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<String, BufferedImage> sources = new HashMap<>();
    private final Map<String, Animation> animations = new HashMap<>();
    private final Map<Variant, Cached> textures = new HashMap<>();
    private final String namespace = UUID.randomUUID().toString();
    private long frame, generation;
    public enum Effect { NONE, BUTTON_STATE, NO_DOWN }
    public enum State { UP, OVER, DOWN, DISABLED }
    public UiDiplomacyAssets() { INSTANCES.add(this); }
    public void beginFrame() {
        frame++;
        textures.values().removeIf(cached -> {
            if (cached.lastFrame >= frame - 2) return false;
            Minecraft.getInstance().getTextureManager().release(cached.id);
            return true;
        });
        animations.values().removeIf(animation -> animation.lastFrame < frame - 2);
    }
    public static void resourcesReloaded() {
        List.copyOf(INSTANCES).forEach(UiDiplomacyAssets::clear);
    }
    public void clear() {
        if (!textures.isEmpty()) {
            var manager = Minecraft.getInstance().getTextureManager();
            textures.values().forEach(cached -> manager.release(cached.id));
        }
        textures.clear();
        animations.clear();
        sources.clear();
    }
    @Override public void close() { clear(); }
    public boolean button(GuiGraphicsExtractor graphics, String controlKey, String path, int x, int y, int width, int height, Effect effect, State state) {
        if (graphics == null || path == null || width <= 0 || height <= 0) return false;
        if (effect == Effect.NONE) return UiAssets.draw(graphics, path, x, y, width, height);
        BufferedImage source = source(path);
        if (source == null) return false;
        float elapsed = animation(controlKey, path, effect, state, System.nanoTime());
        double scale = Math.min((double)width / source.getWidth(), (double)height / source.getHeight());
        int fitWidth = Math.max(1, (int)Math.round(source.getWidth() * scale));
        int fitHeight = Math.max(1, (int)Math.round(source.getHeight() * scale));
        float guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int rasterWidth = Math.max(1, Math.round(fitWidth * guiScale)), rasterHeight = Math.max(1, Math.round(fitHeight * guiScale));
        if (rasterWidth > 4096 || rasterHeight > 4096 || (long)rasterWidth * rasterHeight > 4_194_304) return false;
        var variant = new Variant(path, rasterWidth, rasterHeight, effect, state, mix(effect, state, elapsed));
        Cached cached = textures.get(variant);
        if (cached == null) {
            var image = new NativeImage(rasterWidth, rasterHeight, false);
            for (int row = 0; row < rasterHeight; row++) {
                for (int column = 0; column < rasterWidth; column++) {
                    image.setPixel(column, row, sample(source, (column + .5f) / rasterWidth, (row + .5f) / rasterHeight, effect, state, variant.mix));
                }
            }
            DynamicTexture texture;
            try {
                texture = new DynamicTexture(() -> "HOI diplomacy source effect " + path, image);
            } catch (RuntimeException | Error failure) {
                image.close();
                throw failure;
            }
            Identifier id = Identifier.tryParse("hoi:diplomacy_effect/" + namespace + "/" + generation++);
            try {
                Minecraft.getInstance().getTextureManager().register(id, texture);
            } catch (RuntimeException | Error failure) {
                texture.close();
                throw failure;
            }
            cached = new Cached(id, frame);
            textures.put(variant, cached);
        }
        cached.lastFrame = frame;
        int left = x + (width - fitWidth) / 2, top = y + (height - fitHeight) / 2;
        graphics.blit(cached.id, left, top, left + fitWidth, top + fitHeight, 0, 1, 0, 1);
        return true;
    }
    float animation(String controlKey, String path, Effect effect, State state, long now) {
        Animation animation = animations.get(controlKey);
        if (animation == null || animation.state != state || animation.effect != effect || !animation.path.equals(path)) {
            animation = new Animation(path, effect, state, now, frame);
            animations.put(controlKey, animation);
        }
        animation.lastFrame = frame;
        return Math.max(0f, (now - animation.started) / 1_000_000_000f);
    }
    public boolean tile(GuiGraphicsExtractor graphics, String path, int x, int y, int width, int height, int borderX, int borderY, double scale) {
        if (graphics == null || width <= 0 || height <= 0) return false;
        BufferedImage image = source(path);
        if (image == null) return false;
        Identifier id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        for (Tile tile : tiles(width, height, image.getWidth(), image.getHeight(), borderX, borderY, scale)) {
            graphics.blit(id, x + tile.x0, y + tile.y0, x + tile.x1, y + tile.y1, tile.u0, tile.u1, tile.v0, tile.v1);
        }
        return true;
    }
    static List<Tile> tiles(int width, int height, int sourceWidth, int sourceHeight, int borderX, int borderY, double scale) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0 || borderX < 0 || borderY < 0
                || borderX * 2 >= sourceWidth || borderY * 2 >= sourceHeight || !Double.isFinite(scale) || scale <= 0) return List.of();
        int edgeX = Math.min(width / 2, Math.max(0, (int)Math.round(borderX * scale)));
        int edgeY = Math.min(height / 2, Math.max(0, (int)Math.round(borderY * scale)));
        int[] xs = {0, edgeX, width - edgeX, width}, ys = {0, edgeY, height - edgeY, height};
        int[] us = {0, borderX, sourceWidth - borderX, sourceWidth}, vs = {0, borderY, sourceHeight - borderY, sourceHeight};
        List<Tile> result = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int cellWidth = xs[column + 1] - xs[column], cellHeight = ys[row + 1] - ys[row];
                if (cellWidth <= 0 || cellHeight <= 0) continue;
                int stepX = column == 1 ? Math.max(1, (int)Math.round((us[2] - us[1]) * scale)) : cellWidth;
                int stepY = row == 1 ? Math.max(1, (int)Math.round((vs[2] - vs[1]) * scale)) : cellHeight;
                for (int top = ys[row]; top < ys[row + 1]; top += stepY) {
                    for (int left = xs[column]; left < xs[column + 1]; left += stepX) {
                        int right = Math.min(left + stepX, xs[column + 1]), bottom = Math.min(top + stepY, ys[row + 1]);
                        float u0 = (float)us[column] / sourceWidth, v0 = (float)vs[row] / sourceHeight;
                        float u1 = (us[column] + (us[column + 1] - us[column]) * ((float)(right - left) / stepX)) / sourceWidth;
                        float v1 = (vs[row] + (vs[row + 1] - vs[row]) * ((float)(bottom - top) / stepY)) / sourceHeight;
                        result.add(new Tile(left, top, right, bottom, u0, u1, v0, v1));
                    }
                }
            }
        }
        return result;
    }
    private BufferedImage source(String path) {
        if (sources.containsKey(path)) return sources.get(path);
        BufferedImage image = null;
        Identifier id = path == null ? null : Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id != null) {
            var resource = Minecraft.getInstance().getResourceManager().getResource(id);
            if (resource.isPresent()) {
                try (var input = resource.get().open(); var stream = ImageIO.createImageInputStream(input)) {
                    if (stream != null) {
                        var readers = ImageIO.getImageReaders(stream);
                        if (readers.hasNext()) {
                            var reader = readers.next();
                            try {
                                reader.setInput(stream);
                                int width = reader.getWidth(0), height = reader.getHeight(0);
                                if (width > 0 && height > 0 && width <= 4096 && height <= 4096 && (long)width * height <= 4_194_304) image = reader.read(0);
                            } finally { reader.dispose(); }
                        }
                    }
                } catch (IOException ignored) { }
            }
        }
        sources.put(path, image);
        return image;
    }
    static float mix(Effect effect, State state, float elapsed) {
        if (effect == Effect.NONE || state == State.UP || state == State.DISABLED) return 0;
        if (effect == Effect.NO_DOWN) return state == State.OVER ? .1f : 0;
        float progress = Math.clamp(elapsed * (state == State.DOWN ? 16f : 4f), 0f, 1f);
        float time = .9f - progress;
        return .15f * (.9f * .9f - time * time) * (state == State.DOWN ? -1f : 1f);
    }
    static int shade(int argb, Effect effect, State state, float elapsed) {
        return shade((argb >>> 16 & 255) / 255f, (argb >>> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f, effect, state, mix(effect, state, elapsed));
    }
    private static int shade(float red, float green, float blue, float alpha, Effect effect, State state, float mix) {
        if (effect != Effect.NONE && state == State.DISABLED) {
            float grey = red * .212671f + green * .715160f + blue * .072169f;
            red = grey; green = grey; blue = grey;
        } else if (effect == Effect.BUTTON_STATE) {
            red += (.5f + red) * mix; green += (.5f + green) * mix; blue += (.5f + blue) * mix;
        } else if (effect == Effect.NO_DOWN) {
            red += mix; green += mix; blue += mix;
        }
        return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }
    static int sample(BufferedImage image, float u, float v, Effect effect, State state, float mix) {
        if (effect != Effect.BUTTON_STATE) {
            int column = Math.clamp((int)Math.floor(u * image.getWidth()), 0, image.getWidth() - 1);
            int row = Math.clamp((int)Math.floor(v * image.getHeight()), 0, image.getHeight() - 1);
            int argb = image.getRGB(column, row);
            return shade((argb >>> 16 & 255) / 255f, (argb >>> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f, effect, state, mix);
        }
        float x = u * image.getWidth() - .5f, y = v * image.getHeight() - .5f;
        int ix = (int)Math.floor(x), iy = (int)Math.floor(y);
        float fx = x - ix, fy = y - iy;
        int a = image.getRGB(Math.clamp(ix, 0, image.getWidth() - 1), Math.clamp(iy, 0, image.getHeight() - 1));
        int b = image.getRGB(Math.clamp(ix + 1, 0, image.getWidth() - 1), Math.clamp(iy, 0, image.getHeight() - 1));
        int c = image.getRGB(Math.clamp(ix, 0, image.getWidth() - 1), Math.clamp(iy + 1, 0, image.getHeight() - 1));
        int d = image.getRGB(Math.clamp(ix + 1, 0, image.getWidth() - 1), Math.clamp(iy + 1, 0, image.getHeight() - 1));
        return shade(interpolate(a, b, c, d, 16, fx, fy), interpolate(a, b, c, d, 8, fx, fy), interpolate(a, b, c, d, 0, fx, fy), interpolate(a, b, c, d, 24, fx, fy), effect, state, mix);
    }
    private static float interpolate(int a, int b, int c, int d, int shift, float x, float y) {
        float top = (a >>> shift & 255) * (1 - x) + (b >>> shift & 255) * x;
        float bottom = (c >>> shift & 255) * (1 - x) + (d >>> shift & 255) * x;
        return (top * (1 - y) + bottom * y) / 255f;
    }
    private static int channel(float value) { return Math.clamp(Math.round(value * 255f), 0, 255); }
    record Tile(int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {}
    private record Variant(String path, int width, int height, Effect effect, State state, float mix) {}
    private static final class Cached {
        final Identifier id;
        long lastFrame;
        Cached(Identifier id, long frame) { this.id = id; lastFrame = frame; }
    }
    private static final class Animation {
        final String path;
        final Effect effect;
        final State state;
        final long started;
        long lastFrame;
        Animation(String path, Effect effect, State state, long started, long frame) {
            this.path = path; this.effect = effect; this.state = state; this.started = started; lastFrame = frame;
        }
    }
}
