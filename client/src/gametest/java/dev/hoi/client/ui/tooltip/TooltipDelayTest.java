package dev.hoi.client.ui.tooltip;

import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.protocol.ProtocolTooltip;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;

public final class TooltipDelayTest extends Screen {
    private static final String TEXT = ProtocolTooltip.delayed("요약 설명", "\n\n", "상세 설명");
    private TooltipDelayTest() {super(Component.literal("Tooltip delay regression"));}
    @Override protected void init() {
        var button = new UiMenuButton("Delay", 40, 60, 180, 70, () -> {});
        button.setTooltip(Tooltips.createExact(TEXT));
        addRenderableWidget(button);
    }
    public static void run(ClientGameTestContext context) {
        if (!TooltipDelay.summary(TEXT).equals("요약 설명") || !TooltipDelay.full(TEXT).equals("요약 설명\n\n상세 설명"))
            throw new AssertionError("Delayed tooltip must split into the summary and the full source text");
        context.runOnClient(client -> {
            if (!TooltipDelay.visible(TEXT).equals("요약 설명")) throw new AssertionError("Delayed tooltip must first show only the summary");
        });
        for (int i = 0; i < 12; i++) {
            pause(100);
            context.runOnClient(client -> TooltipDelay.visible(TEXT));
        }
        context.runOnClient(client -> {
            if (!TooltipDelay.visible(TEXT).equals("요약 설명\n\n상세 설명")) throw new AssertionError("Delayed tooltip must show the detail after one second of hover");
        });
        pause(300);
        context.runOnClient(client -> {
            if (!TooltipDelay.visible(TEXT).equals("요약 설명")) throw new AssertionError("A new hover must restart the delayed tooltip from the summary");
        });
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.setScreen(TooltipDelayTest::new);
        context.getInput().setCursorPos(140, 160);
        context.waitTicks(3);
        context.runOnClient(client -> {
            int lines = lines((Screen) client.gui.screen());
            if (lines != 1) throw new AssertionError("Widget tooltip must start with the one-line summary: " + lines);
        });
        pause(1100);
        context.runOnClient(client -> {
            int lines = lines((Screen) client.gui.screen());
            if (lines < 2) throw new AssertionError("Widget tooltip must expand to the detail after one second of hover: " + lines);
        });
        context.setScreen(() -> null);
    }
    private static void pause(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
    private static int lines(Screen screen) {
        var client = Minecraft.getInstance();
        int[] count = {0};
        var g = new GuiGraphicsExtractor(client, new GuiRenderState(), 70, 80) {
            @Override public void tooltip(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, net.minecraft.resources.Identifier style, boolean focused) {
                count[0] = components.size();
                super.tooltip(font, components, x, y, positioner, style, focused);
            }
        };
        screen.extractRenderState(g, 70, 80, 0);
        g.extractDeferredElements(70, 80, 0);
        TooltipLayer.flush();
        return count[0];
    }
}
