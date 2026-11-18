package dev.hoi.client.screen;

import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import java.awt.image.BufferedImage;
import java.util.*;

public final class ScreenAlertGlowTest {
    public static void run(ClientGameTestContext context, MenuView fixture) {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2));
        context.getInput().setCursorPos(2, 2);
        var plain = capture(context, fixture, MenuTab.POLITICS, Map.of(), "politics-off");
        var glowing = capture(context, fixture, MenuTab.POLITICS, Map.of("economic_laws", 1, "military_laws", 2), "politics-on");
        for (String slot : List.of("경제법 선택 1 · 미지정", "군법 선택 2 · 미지정")) {
            int[] box = box(context, slot);
            glowing(slot, box, glow(box), plain, glowing, true);
        }
        quiet("경제법 선택 4 · 미지정", box(context, "경제법 선택 4 · 미지정"), plain, glowing);
        plain = capture(context, fixture, MenuTab.OFFICER_CORPS, Map.of(), "officer-off");
        glowing = capture(context, fixture, MenuTab.OFFICER_CORPS, Map.of("high_command", 1, "army", 2), "officer-on");
        int[] commander = box(context, "최고사령부 2 · 미지정"), spirit = box(context, "정신 · 미지정");
        glowing("최고사령부 2", commander, glow(commander), plain, glowing, true);
        glowing("육군 정신 1", spirit, new int[]{spirit[0] - spirit[2], spirit[1] - spirit[3], spirit[0] + 2 * spirit[2], spirit[1] + 2 * spirit[3]}, plain, glowing, false);
        quiet("해군 참모총장 · 미지정", box(context, "해군 참모총장 · 미지정"), plain, glowing);
        context.setScreen(() -> null);
        context.getInput().resizeWindow(1600, 1000);
    }
    private static void glowing(String slot, int[] box, int[] area, BufferedImage plain, BufferedImage glowing, boolean vertical) {
        int[] changes = changes(plain, glowing, area);
        if (changes[0] < 100) throw new AssertionError("Changeable slot lost its glow: " + slot + " " + changes[0]);
        if (changes[1] * 10 < changes[0] * 9) throw new AssertionError("Glow must brighten its slot: " + slot + " " + Arrays.toString(changes));
        if (Math.abs(changes[2] + changes[3] - 2 * box[0] - box[2]) > 6 || vertical && Math.abs(changes[4] + changes[5] - 2 * box[1] - box[3]) > 6)
            throw new AssertionError("Glow must be centered on its slot: " + slot + " " + Arrays.toString(box) + " " + Arrays.toString(changes));
    }
    private static void quiet(String slot, int[] box, BufferedImage plain, BufferedImage glowing) {
        int changed = changes(plain, glowing, glow(box))[0];
        if (changed != 0) throw new AssertionError("Slot without a change glowed: " + slot + " " + changed);
    }
    private static BufferedImage capture(ClientGameTestContext context, MenuView fixture, MenuTab tab, Map<String, Integer> alerts, String stage) {
        var pages = fixture.pages().stream().map(page -> page.tab() != tab ? page : new MenuView.Page(page.tab(), page.sections().stream().map(section -> {
            int target = alerts.getOrDefault(section.icon(), -1);
            var entries = new ArrayList<MenuView.Entry>();
            for (int i = 0; i < section.entries().size(); i++) {
                var entry = section.entries().get(i);
                entries.add(new MenuView.Entry(entry.name(), entry.value(), entry.detail(), entry.progress(), entry.icon(), i == target));
            }
            return new MenuView.Section(section.title(), section.icon(), entries);
        }).toList())).toList();
        var view = new MenuView(fixture.country(), fixture.countryName(), fixture.date(), fixture.speed(), pages, fixture.hud());
        context.setScreen(() -> new ScreenMenu(view, tab, () -> {}));
        context.waitTicks(3);
        try { return javax.imageio.ImageIO.read(context.takeScreenshot("hoi-alert-glow-" + stage + "-gui2").toFile()); }
        catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private static int[] box(ClientGameTestContext context, String message) {
        return context.computeOnClient(c -> c.gui.screen().children().stream()
                .filter(widget -> widget instanceof AbstractWidget button && button.getMessage().getString().equals(message))
                .map(widget -> (AbstractWidget) widget).findFirst()
                .map(button -> new int[]{button.getX() * 2, button.getY() * 2, button.getWidth() * 2, button.getHeight() * 2})
                .orElseThrow(() -> new AssertionError("Slot missing: " + message)));
    }
    private static int[] glow(int[] box) {
        float unit = Math.min(box[2], box[3]) / 63f;
        int width = Math.round(91.65f * unit), height = Math.round(74.75f * unit);
        int x = box[0] + (box[2] - width) / 2, y = box[1] + (box[3] - height) / 2;
        return new int[]{x, y, x + width, y + height};
    }
    private static int[] changes(BufferedImage before, BufferedImage after, int[] area) {
        int changed = 0, brighter = 0, left = Integer.MAX_VALUE, right = Integer.MIN_VALUE, top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;
        for (int y = Math.max(0, area[1]); y < Math.min(before.getHeight(), area[3]); y++) for (int x = Math.max(0, area[0]); x < Math.min(before.getWidth(), area[2]); x++) {
            int a = before.getRGB(x, y), b = after.getRGB(x, y);
            int difference = Math.abs((a >> 16 & 255) - (b >> 16 & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255));
            if (difference <= 24) continue;
            changed++;
            if ((b >> 16 & 255) + (b >> 8 & 255) > (a >> 16 & 255) + (a >> 8 & 255)) brighter++;
            left = Math.min(left, x); right = Math.max(right, x + 1); top = Math.min(top, y); bottom = Math.max(bottom, y + 1);
        }
        return new int[]{changed, brighter, left, right, top, bottom};
    }
}
