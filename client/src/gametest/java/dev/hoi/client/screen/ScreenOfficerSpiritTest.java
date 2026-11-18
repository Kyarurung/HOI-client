package dev.hoi.client.screen;

import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import java.awt.image.BufferedImage;
import java.util.*;

public final class ScreenOfficerSpiritTest {
    private static final String ICON = "politics/appointments/academy_scholarships_spirit";
    public static void run(ClientGameTestContext context, MenuView fixture) {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2));
        context.getInput().setCursorPos(2, 2);
        var empty = capture(context, fixture, "politics/appointments/missing_spirit_icon", "empty");
        var adopted = capture(context, fixture, ICON, "adopted");
        int[] slot = context.computeOnClient(c -> c.gui.screen().children().stream()
                .filter(widget -> widget instanceof AbstractWidget button && button.getMessage().getString().equals("정신 · 사관학교 장학금"))
                .map(widget -> (AbstractWidget) widget).findFirst()
                .map(button -> new int[]{button.getX() * 2, button.getY() * 2, button.getWidth() * 2, button.getHeight() * 2})
                .orElseThrow(() -> new AssertionError("Adopted spirit slot must stay clickable")));
        int[] size = context.computeOnClient(c -> dev.hoi.client.ui.UiAssets.imageSize(ICON));
        int left = Integer.MAX_VALUE, right = -1, top = Integer.MAX_VALUE, bottom = -1;
        for (int y = Math.max(0, slot[1] - slot[3]); y < Math.min(empty.getHeight(), slot[1] + 2 * slot[3]); y++)
            for (int x = slot[0]; x < slot[0] + slot[2]; x++) if (empty.getRGB(x, y) != adopted.getRGB(x, y)) {
                left = Math.min(left, x); right = Math.max(right, x + 1); top = Math.min(top, y); bottom = Math.max(bottom, y + 1);
            }
        double unit = slot[2] / 168.0;
        if (right < 0) throw new AssertionError("Adopted spirit icon must be drawn");
        if (right - left > size[0] * unit + 2 || bottom - top > size[1] * unit + 2)
            throw new AssertionError("Adopted spirit icon keeps the source size on the 168px spirit background: " + (right - left) + "x" + (bottom - top) + " limit " + size[0] * unit + "x" + size[1] * unit);
        double centerX = slot[0] + slot[2] / 2.0 + unit, centerY = slot[1] + slot[3] / 2.0 + 3 * unit;
        if (Math.abs((left + right) / 2.0 - centerX) > 3 || Math.abs((top + bottom) / 2.0 - centerY) > 3)
            throw new AssertionError("Adopted spirit icon follows the source spirit_icon offset: " + left + "," + top + "-" + right + "," + bottom + " center " + centerX + "," + centerY);
        context.setScreen(() -> null);
        context.getInput().resizeWindow(1600, 1000);
    }
    private static BufferedImage capture(ClientGameTestContext context, MenuView fixture, String icon, String stage) {
        var pages = fixture.pages().stream().map(page -> page.tab() != MenuTab.OFFICER_CORPS ? page : new MenuView.Page(page.tab(), page.sections().stream().map(section -> {
            if (!section.icon().equals("army")) return section;
            var entries = new ArrayList<>(section.entries());
            var spirit = entries.get(2);
            entries.set(2, new MenuView.Entry(spirit.name(), "사관학교 장학금", spirit.detail(), 1, icon));
            return new MenuView.Section(section.title(), section.icon(), entries);
        }).toList())).toList();
        var view = new MenuView(fixture.country(), fixture.countryName(), fixture.date(), fixture.speed(), pages, fixture.hud());
        context.setScreen(() -> new ScreenMenu(view, MenuTab.OFFICER_CORPS, () -> {}));
        context.waitTicks(3);
        try { return javax.imageio.ImageIO.read(context.takeScreenshot("hoi-officer-spirit-" + stage + "-gui2").toFile()); }
        catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
}
