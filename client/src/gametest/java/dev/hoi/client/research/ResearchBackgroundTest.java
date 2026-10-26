package dev.hoi.client.research;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.ProtocolResearchView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.List;

public final class ResearchBackgroundTest {
    private ResearchBackgroundTest() {}
    public static void run(ClientGameTestContext context) {
        ProtocolResearchView source;
        try (var input = ResearchBackgroundTest.class.getResourceAsStream("/tfr-research-view.json")) {
            source = new dev.hoi.protocol.ProtocolResearchPayloads.Response(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).view();
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        var view = new ProtocolResearchView(source.session(), source.revision(), source.country(), source.countryName(), source.day(), source.date(), source.speed(),
                List.of(new ProtocolResearchView.Slot(0, "", 0)), source.technologies(), source.message(), source.hud(), source.benefits());
        int scale = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.waitTicks(5);
                context.setScreen(() -> new ResearchScreen(view, request -> {
                    if (request.action() == dev.hoi.protocol.ProtocolResearchPayloads.Action.START || request.action() == dev.hoi.protocol.ProtocolResearchPayloads.Action.CANCEL)
                        throw new AssertionError("Background navigation sent a research mutation");
                }));
                context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = (ResearchScreen)client.gui.screen();
                    var slot = screen.children().stream().filter(ResearchSlotButton.class::isInstance)
                            .map(ResearchSlotButton.class::cast).filter(button -> button.getMessage().getString().contains("연구 선택")).findFirst().orElseThrow();
                    activate(slot);
                    if (screen.allowsMovement()) throw new AssertionError("Slot did not open tree: " + slot.getX() + "," + slot.getY() + " in " + screen.width + "x" + screen.height);
                });
                context.waitTicks(3);
                var sprites = List.of("infantry_techtree_bg", "support_techtree_bg", "armortech_bg", "artillery_techtree_bg",
                        "naval_techtree_bg", "", "air_techtree_bg", "engineering_techtree_bg", "industry_techtree_bg");
                for (int index = 0; index < sprites.size(); index++) {
                    int tab = index;
                    context.runOnClient(client -> {
                        var screen = (ResearchScreen)client.gui.screen();
                        activate(screen.children().stream().filter(ResearchTabButton.class::isInstance).map(ResearchTabButton.class::cast).toList().get(tab));
                        if (!sprites.get(tab).isEmpty() && UiAssets.imageSize("research/source/gfx_" + sprites.get(tab) + "/atlas")[0] == 0)
                            throw new AssertionError("Missing source background " + sprites.get(tab));
                    });
                    context.getInput().setCursorPos(size[0] - 10, size[1] - 10);
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-research-source-background-" + index + "-" + size[0] + "-gui2");
                }
            }
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> client.options.guiScale().set(scale));
        }
    }
    private static void activate(net.minecraft.client.gui.components.Button button) {
        if (!button.active) throw new AssertionError("Research navigation disabled " + button.getMessage().getString());
        button.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
    }
}
