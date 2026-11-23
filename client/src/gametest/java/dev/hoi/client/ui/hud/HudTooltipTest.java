package dev.hoi.client.ui.hud;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.menu.MenuView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class HudTooltipTest {
    private HudTooltipTest() {}
    public static void run(ClientGameTestContext context, MenuView menu) {
        decisions(context, menu);
        boolean before = "1".equals(System.getenv("HOI_CAPTURE_BEFORE"));
        for (int width : new int[]{2560, 1920, 1280}) {
            context.getInput().resizeWindow(width, width * 9 / 16);
            context.runOnClient(client -> { client.options.guiScale().set(2); HudTooltip.clear(); });
            context.setScreen(() -> new ScreenMenu(menu));
            context.runOnClient(client -> client.gui.screen().mouseScrolled(50, 5, -100, 0));
            int x = HudMenuBar.statsRight(width / 2) - 3 - 134 + 20;
            context.getInput().setCursorPos(x * 2, 12); context.waitTicks(4);
            context.takeScreenshot("hoi-hud-gdp-scroll-top-" + width + "-gui2");
            if (!before) context.runOnClient(client -> {
                var bounds = bounds();
                var gdp = HudMenuBar.indicators(menu.hud()).stream().filter(item -> item.icon().equals("gdp")).findFirst().orElseThrow();
                int content = HudTooltip.body(client.font, width / 2, menu.hud(), gdp).size() * (client.font.lineHeight + 1);
                if (bounds == null && content + 8 > client.gui.screen().height) throw new AssertionError("A GDP tooltip taller than the screen must have a bounded scroll viewport");
                if (bounds != null && (bounds.width() + 8 > dev.hoi.client.ui.tooltip.Tooltips.MAX_WIDTH || bounds.y() < 4 || bounds.y() + bounds.height() + 4 > client.gui.screen().height || bounds.scroll() != 0))
                    throw new AssertionError("HUD tooltip initial width, height or offset: " + bounds);
            });
            context.runOnClient(client -> client.gui.screen().mouseScrolled(x, 6, 0, -1000));
            context.waitTicks(3);
            context.takeScreenshot("hoi-hud-gdp-scroll-bottom-" + width + "-gui2");
            if (!before) context.runOnClient(client -> {
                var bounds = bounds();
                if (bounds != null && (bounds.maxScroll() <= 0 || bounds.scroll() != bounds.maxScroll())) throw new AssertionError("HUD wheel must reach the final GDP line: " + bounds);
                var indicator = HudMenuBar.indicators(menu.hud()).stream().filter(item -> item.icon().equals("gdp")).findFirst().orElseThrow();
                var lines = HudTooltip.body(client.font, width / 2, menu.hud(), indicator);
                if (bounds != null && lines.size() * (client.font.lineHeight + 1) - bounds.scroll() != bounds.height()) throw new AssertionError("GDP final line must remain inside the viewport");
                client.gui.screen().mouseScrolled(x, 6, 0, 1000);
            });
            context.waitTicks(2);
            if (!before) context.runOnClient(client -> { if (bounds() != null && bounds().scroll() != 0) throw new AssertionError("HUD wheel must return to the first GDP line"); });
            context.getInput().setCursorPos(width - 2, width * 9 / 16 - 2); context.waitTicks(2);
            if (!before) context.runOnClient(client -> { if (bounds() != null) throw new AssertionError("Leaving the indicator must clear the HUD tooltip scroll state"); });
        }
        context.getInput().resizeWindow(2560, 1440);
    }
    private static void decisions(ClientGameTestContext context, MenuView menu) {
        context.getInput().resizeWindow(2560, 1440);
        var previous = context.computeOnClient(client -> { client.options.guiScale().set(2); return HudPayloads.State.of(ClientCampaignHud.country().isEmpty() ? menu.country() : ClientCampaignHud.country(), ClientCampaignHud.hud()); });
        boolean hidden = context.computeOnClient(client -> !ClientCampaignHud.visible());
        for (int count : new int[]{18, 0}) {
            context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of(menu.country(), menu.hud().withAvailableDecisions(count))));
            context.setScreen(() -> new ScreenMenu(menu)); context.waitTicks(3);
            try {
                var image = javax.imageio.ImageIO.read(context.takeScreenshot("hoi-hud-decisions-badge-" + count + "-gui2").toFile());
                int white = 0;
                for (int y = 46; y < 72; y++) for (int x = 114; x < 140; x++) if ((image.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) white++;
                if ((count > 0) != (white > 0)) throw new AssertionError("Decisions badge shows only an available count: " + count + " white=" + white);
            } catch (java.io.IOException failure) { throw new AssertionError(failure); }
        }
        context.runOnClient(client -> ClientCampaignHud.accept(hidden ? HudPayloads.State.HIDDEN : previous));
    }
    private static Tooltips.ScrolledTooltip bounds() {
        try {
            var field = HudTooltip.class.getDeclaredField("bounds");
            field.setAccessible(true);
            return (Tooltips.ScrolledTooltip)field.get(null);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
}
