package dev.hoi.client.ui;

import dev.hoi.protocol.menu.MenuView;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UiMenuHeadingTest {
    private UiMenuHeadingTest() {}
    public static void run(ClientGameTestContext context, MenuView menu) {
        int previous = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.setScreen(() -> new Screen(Component.literal("헤더 볼드 래스터 검사")) {
                    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        g.fill(0, 0, width, height, 0xFF000000);
                        UiMenuStyle.heading(g, font, "사단 편제 설계", 10, 21, 200, 0xFFFFFFFF);
                        UiMenuStyle.heading(g, font, "사단 편제 설계", 240, 21, 40, 0xFFFFFFFF);
                        for (int row = 0; row < 2; row++) {
                            boolean bold = row == 1;
                            g.pose().pushMatrix();
                            g.pose().translate(10, 60 + row * 40);
                            g.pose().scale((font.lineHeight + 2f) / font.lineHeight);
                            g.text(font, Component.literal("사단 편제 설계").withStyle(style -> style.withBold(bold)), 0, 0, 0xFFFFFFFF);
                            g.pose().popMatrix();
                        }
                        for (int row = 0; row < 3; row++) {
                            boolean weight = row == 1;
                            g.pose().pushMatrix();
                            g.pose().translate(10, 150 + row * 30);
                            g.pose().scale(row == 2 ? 1.5f : 1f);
                            g.text(font, Component.literal("한글").withStyle(s -> s.withBold(weight)), 0, 0, 0xFFFF0000, false);
                            g.text(font, Component.literal("0123456789").withStyle(s -> s.withBold(weight)), 30, 0, 0xFF00FF00, false);
                            g.text(font, Component.literal("ABCxz").withStyle(s -> s.withBold(weight)), 110, 0, 0xFF0000FF, false);
                            g.text(font, Component.literal("gypq").withStyle(s -> s.withBold(weight)), 170, 0, 0xFFFFFF00, false);
                            g.pose().popMatrix();
                        }
                        for (int row = 0; row < 3; row++) {
                            int y = 250 + row * 30;
                            g.fill(10, y, 130, y + 20, 0xFF303030);
                            g.fill(140, y, 260, y + 20, 0xFF303030);
                            g.fill(270, y, 390, y + 20, 0xFF303030);
                            if (row == 0) {
                                UiText.cell(g, font, "0/0", 10, y, 120, 20, 0xFFFF0000, false, false);
                                UiText.cell(g, font, "17", 140, y, 120, 20, 0xFF00FF00, false, false);
                                UiText.cell(g, font, "공장 17", 270, y, 120, 20, 0xFF0000FF, false, false);
                            } else {
                                UiText.centered(g, font, Component.literal("0/0").withStyle(s -> s.withBold(true)), 10, y, 120, 20, 0xFFFF0000, false);
                                UiText.centeredSingleLine(g, font, Component.literal("17"), 140, y, 120, 20, 0xFF00FF00);
                                UiText.leftSingleLine(g, font, Component.literal("공장 17"), 270, y, 120, 20, 0xFF0000FF);
                            }
                        }
                    }
                });
                context.getInput().setCursorPos(size[0] - 2, size[1] - 2);
                context.waitTicks(3);
                var file = context.takeScreenshot("hoi-menu-header-bold-diagnostic-synthetic-" + size[0] + "-gui2");
                var image = javax.imageio.ImageIO.read(file.toFile());
                int actual = ink(image, 20, 40, 400, 24), plain = ink(image, 20, 120, 400, 24), bold = ink(image, 20, 200, 400, 24);
                if (actual != bold || actual <= plain) throw new AssertionError("Menu heading must rasterize at its enlarged bold weight: " + actual + "/" + plain + "/" + bold);
                if (ink(image, 562, 40, 40, 24) != 0) throw new AssertionError("Bold heading must clip using its styled width");
                for (int row = 0; row < 3; row++) {
                    for (int color : new int[]{0xFF0000, 0x00FF00}) {
                        int first = -1, last = -1;
                        int top = 500 + row * 60;
                        for (int py = top; py < top + 40; py++) for (int px = 20; px < 780; px++) {
                            if ((image.getRGB(px, py) & 0xFFFFFF) == color) {
                                if (first < 0) first = py;
                                last = py;
                            }
                        }
                        if (first < 0 || Math.abs(first + last + 1 - (top * 2 + 40)) > 2)
                            throw new AssertionError("Centered cell ink must match its background center: " + row + "/" + color + "/" + first + "/" + last);
                    }
                }
                for (int row = 0; row < 3; row++) {
                    int korean = bottom(image, 300 + row * 60, 0xFF0000);
                    int digits = bottom(image, 300 + row * 60, 0x00FF00);
                    int latin = bottom(image, 300 + row * 60, 0x0000FF);
                    if (korean < 0 || korean != digits || korean != latin) throw new AssertionError("Hangul baseline mismatch: " + korean + "/" + digits + "/" + latin);
                    int descenders = bottom(image, 300 + row * 60, 0xFFFF00);
                    if (descenders - latin != (row == 2 ? 3 : 2)) throw new AssertionError("Latin descenders must retain their shape below the common baseline");
                }
                context.runOnClient(client -> UiText.holdMarquee(false));
                context.waitTicks(47);
                var scrolled = javax.imageio.ImageIO.read(context.takeScreenshot("hoi-menu-header-marquee-synthetic-" + size[0] + "-gui2").toFile());
                context.runOnClient(client -> UiText.holdMarquee(true));
                int changed = 0;
                for (int py = 40; py < 64; py++) for (int px = 480; px < 560; px++) if (image.getRGB(px, py) != scrolled.getRGB(px, py)) changed++;
                if (changed == 0 || ink(scrolled, 480, 40, 80, 24) == 0) throw new AssertionError("Overflowing heading must scroll horizontally inside its box");
                if (ink(scrolled, 562, 40, 40, 24) != 0 || ink(scrolled, 430, 40, 48, 24) != 0) throw new AssertionError("Scrolling heading must stay inside its box");
            }
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> client.options.guiScale().set(previous));
        }
    }
    private static int bottom(java.awt.image.BufferedImage image, int top, int color) {
        int bottom = -1;
        for (int y = top; y < top + 35; y++) for (int x = 10; x < 620; x++) {
            if ((image.getRGB(x, y) & 0xFFFFFF) == color) bottom = y;
        }
        return bottom;
    }
    private static int ink(java.awt.image.BufferedImage image, int x, int y, int width, int height) {
        int count = 0;
        for (int py = y; py < y + height; py++) for (int px = x; px < x + width; px++) {
            int color = image.getRGB(px, py);
            if ((color >> 16 & 255) > 220 && (color >> 8 & 255) > 220 && (color & 255) > 220) count++;
        }
        return count;
    }
}
