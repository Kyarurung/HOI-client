package dev.hoi.client.screen;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

final class ScreenDecisionRadial implements AutoCloseable {
    static final String EFFECT_SHA256 = "bb72c5ae8831120923eb9606b5d42b2497b595f8167a54c554cd39685b9f2a07";
    static final String INCLUDE_SHA256 = "ce36e0c3593f76798bf7a8ddcf1c0882510a14bdc74c418481589e8766a8f6ed";
    private static final float PI = 3.14159265f;
    private static final List<String> SPRITES = List.of("GFX_KOR_Ind_Bar", "GFX_KOR_JP_Bar", "GFX_KOR_PP_Bar", "GFX_KOR_PPP_Bar", "GFX_KOR_DP_Bar");
    private final Map<String, Descriptor> descriptors = new HashMap<>();
    private final Map<String, Cached> cache = new HashMap<>();
    private final String namespace = UUID.randomUUID().toString();
    private long generation;
    void configure(JsonObject decisionAssetsRoot) {
        var next = new HashMap<String, Descriptor>();
        if (decisionAssetsRoot != null && decisionAssetsRoot.get("radial") instanceof JsonObject radial) {
            for (String sprite : SPRITES) {
                if (radial.get(sprite) instanceof JsonObject source) {
                    Descriptor descriptor = descriptor(source);
                    if (descriptor != null) next.put(sprite, descriptor);
                }
            }
        }
        cache.entrySet().removeIf(entry -> {
            if (entry.getValue().descriptor.equals(next.get(entry.getKey()))) return false;
            release(entry.getValue());
            return true;
        });
        descriptors.clear();
        descriptors.putAll(next);
    }
    int[] dimensions(String sprite) {
        var descriptor = descriptors.get(sprite);
        return descriptor == null ? new int[2] : new int[]{descriptor.width(), descriptor.height()};
    }
    boolean draw(GuiGraphicsExtractor graphics, String sprite, Double packedFrame, int x, int y, int width, int height) {
        if (graphics == null || packedFrame == null || !validFrame(packedFrame) || width <= 0 || height <= 0) return false;
        var descriptor = descriptors.get(sprite);
        if (descriptor == null) return false;
        var cached = cache.computeIfAbsent(sprite, ignored -> {
            Mask one = loadMask(descriptor.texture1()), two = loadMask(descriptor.texture2());
            return new Cached(descriptor, one == null || two == null ? null : new Raster(descriptor, one, two));
        });
        if (cached.raster == null) return false;
        int[] pixels = cached.raster.pixels(packedFrame);
        if (pixels != cached.uploaded) {
            var image = new NativeImage(descriptor.width(), descriptor.height(), false);
            for (int row = 0; row < descriptor.height(); row++) {
                for (int column = 0; column < descriptor.width(); column++) {
                    image.setPixel(column, row, pixels[row * descriptor.width() + column]);
                }
            }
            DynamicTexture texture;
            try {
                texture = new DynamicTexture(() -> "HOI decision radial " + sprite, image);
            } catch (RuntimeException | Error failure) {
                image.close();
                throw failure;
            }
            var manager = Minecraft.getInstance().getTextureManager();
            var identifier = Identifier.tryParse("hoi:decision_radial/" + namespace + "/" + generation++);
            release(cached);
            try {
                manager.register(identifier, texture);
            } catch (RuntimeException | Error failure) {
                texture.close();
                throw failure;
            }
            cached.texture = identifier;
            cached.uploaded = pixels;
        }
        graphics.blit(cached.texture, x, y, x + width, y + height, 0, 1, 0, 1);
        return true;
    }
    void clear() {
        cache.values().forEach(ScreenDecisionRadial::release);
        cache.clear();
    }
    @Override public void close() {
        clear();
        descriptors.clear();
    }
    private static void release(Cached cached) {
        if (cached.texture != null) Minecraft.getInstance().getTextureManager().release(cached.texture);
        cached.texture = null;
        cached.uploaded = null;
    }
    private static Mask loadMask(String path) {
        var identifier = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (identifier == null) return null;
        var resource = Minecraft.getInstance().getResourceManager().getResource(identifier);
        if (resource.isEmpty()) return null;
        try (var input = resource.get().open(); var stream = ImageIO.createImageInputStream(input)) {
            if (stream == null) return null;
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) return null;
            var reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                if (reader.getWidth(0) != 500 || reader.getHeight(0) != 500) return null;
                return Mask.from(reader.read(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException failure) {
            return null;
        }
    }
    static Descriptor descriptor(JsonObject source) {
        try {
            if (!source.get("effectFile").getAsString().equals("gfx/FX/progress_radial_legco.shader")
                    || !source.get("effectSha256").getAsString().equalsIgnoreCase(EFFECT_SHA256)
                    || !source.get("source").getAsString().equals("korea/interface/TFRKP/TFR_Korea_NatAsm.gfx")
                    || !source.get("sourceSha256").getAsString().matches("(?i)[a-f0-9]{64}")
                    || source.get("line").getAsInt() <= 0
                    || source.get("width").getAsDouble() != 350 || source.get("height").getAsDouble() != 350
                    || source.get("steps").getAsDouble() != 100000
                    || source.has("scale") && !source.get("scale").isJsonNull()) return null;
            boolean include = false;
            for (var value : source.getAsJsonArray("includes")) {
                var item = value.getAsJsonObject();
                if (item.get("source").getAsString().equals("korea/gfx/FX/the_fire_rises_korea_plus.fxh")
                        && item.get("sha256").getAsString().equalsIgnoreCase(INCLUDE_SHA256)) include = true;
            }
            if (!include) return null;
            String one = source.get("texture1").getAsString(), two = source.get("texture2").getAsString();
            if (!one.matches("decision/panel/[a-z0-9_]+/texturefile1") || !two.matches("decision/panel/[a-z0-9_]+/texturefile2")) return null;
            return new Descriptor(350, 350, 100000, one, two, color(source, "color"), color(source, "colorTwo"), source.toString());
        } catch (RuntimeException invalid) {
            return null;
        }
    }
    private static Color color(JsonObject source, String key) {
        var values = source.getAsJsonArray(key);
        if (values.size() != 4) throw new IllegalArgumentException(key);
        float[] rgba = new float[4];
        for (int index = 0; index < rgba.length; index++) {
            rgba[index] = values.get(index).getAsFloat();
            if (!Float.isFinite(rgba[index]) || rgba[index] < 0 || rgba[index] > 1) throw new IllegalArgumentException(key);
        }
        return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }
    static boolean validFrame(double packedFrame) {
        return Double.isFinite(packedFrame) && Float.isFinite((float)(packedFrame / 100000) * 100000f);
    }
    record Descriptor(int width, int height, int steps, String texture1, String texture2, Color one, Color two, String provenance) {}
    record Color(float red, float green, float blue, float alpha) {
        int argb(boolean forceAlpha, boolean logo) {
            float shade = logo ? .65f : 1f;
            return Math.round((forceAlpha ? 1f : alpha) * 255f) << 24 | Math.round(red * shade * 255f) << 16
                    | Math.round(green * shade * 255f) << 8 | Math.round(blue * shade * 255f);
        }
    }
    record Mask(int width, int height, byte[] alpha) {
        static Mask from(BufferedImage image) {
            if (image == null || !image.getColorModel().hasAlpha()) return null;
            int width = image.getWidth(), height = image.getHeight();
            if (width <= 0 || height <= 0 || width > 1024 || height > 1024) return null;
            byte[] alpha = new byte[width * height];
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) alpha[y * width + x] = (byte)(image.getRGB(x, y) >>> 24);
            return new Mask(width, height, alpha);
        }
        boolean pointWrap(float u, float v) {
            int x = Math.min(width - 1, (int)((u - (float)Math.floor(u)) * width));
            int y = Math.min(height - 1, (int)((v - (float)Math.floor(v)) * height));
            return alpha[y * width + x] != 0;
        }
    }
    record Frame(float start, float end, float angle, float lhsX, float lhsY, float rhsX, float rhsY) {
        static Frame from(double packedFrame, int steps) {
            if (!validFrame(packedFrame) || steps != 100000) throw new IllegalArgumentException("radial frame");
            float currentState = (float)(packedFrame / steps);
            float value = currentState * 100000f;
            float start = (float)Math.floor(value / 1000f + .5f) / 100f;
            float end = (value - 1000f * (float)Math.floor(value / 1000f)) / 100f;
            float midway = (end - start) * .5f + start;
            float lhsAngle = PI * (.48f + midway), rhsAngle = PI * (1.5f - end * .5f);
            return new Frame(start, end, (1.5f - end) * PI, -.335f * (float)Math.sin(lhsAngle),
                    -.335f * (float)Math.cos(lhsAngle), -.335f * (float)Math.cos(rhsAngle), -.335f * (float)Math.sin(rhsAngle));
        }
    }
    static int pixel(float u, float v, Frame frame, Color one, Color two, Mask maskOne, Mask maskTwo) {
        float x = u - .5f, y = .982f - v;
        float radius = (float)Math.sqrt(x * x + y * y);
        if (radius > .5f || radius < .17f) return 0;
        float angle = atan2Tno(.5f - u, .982f - v) + PI;
        boolean first = !(frame.angle() > angle);
        float imageX = ((first ? frame.lhsX() : frame.rhsX()) - x) * 3.2f + .5f;
        float imageY = ((first ? frame.lhsY() : frame.rhsY()) - y) * 3.2f + .5f;
        boolean eligible = first ? frame.end() > frame.start() + .01f : frame.end() < 1f;
        boolean logo = eligible && imageX >= 0 && imageX <= 1 && imageY >= 0 && imageY <= 1
                && (first ? maskOne : maskTwo).pointWrap(1f - imageX, imageY);
        return (first ? one : two).argb(first, logo);
    }
    static float atan2Tno(float x, float y) {
        x *= -1f;
        y *= -1f;
        float degree;
        if (x > 0) {
            degree = (float)Math.atan(x / y) - PI;
            if (degree < -PI) degree += PI;
        } else if (x < 0 && y >= 0) degree = (float)Math.atan(x / y) + PI;
        else if (y < 0 && x < 0) degree = (float)Math.atan(x / y);
        else if (x == 0 && y > 0) degree = -PI / 2f;
        else if (x == 0 && y < 0) degree = PI / 2f;
        else degree = 0;
        if (degree > 2f * PI) degree -= 2f * PI;
        return degree;
    }
    static final class Raster {
        private final Descriptor descriptor;
        private final Mask one;
        private final Mask two;
        private long frameBits;
        private int[] pixels;
        Raster(Descriptor descriptor, Mask one, Mask two) {
            this.descriptor = descriptor;
            this.one = one;
            this.two = two;
        }
        int[] pixels(double packedFrame) {
            if (!validFrame(packedFrame)) throw new IllegalArgumentException("radial frame");
            long bits = Double.doubleToLongBits(packedFrame);
            if (pixels != null && frameBits == bits) return pixels;
            var frame = Frame.from(packedFrame, descriptor.steps());
            int[] next = new int[descriptor.width() * descriptor.height()];
            for (int y = 0; y < descriptor.height(); y++) {
                for (int x = 0; x < descriptor.width(); x++) {
                    next[y * descriptor.width() + x] = pixel((x + .5f) / descriptor.width(), (y + .5f) / descriptor.height(),
                            frame, descriptor.one(), descriptor.two(), one, two);
                }
            }
            frameBits = bits;
            pixels = next;
            return pixels;
        }
    }
    private static final class Cached {
        final Descriptor descriptor;
        final Raster raster;
        Identifier texture;
        int[] uploaded;
        Cached(Descriptor descriptor, Raster raster) {
            this.descriptor = descriptor;
            this.raster = raster;
        }
    }
}
