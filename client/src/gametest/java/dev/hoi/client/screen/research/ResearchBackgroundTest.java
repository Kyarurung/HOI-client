package dev.hoi.client.screen.research;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.research.ResearchView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.List;

public final class ResearchBackgroundTest {
    private ResearchBackgroundTest() {}
    public static void run(ClientGameTestContext context) {
        ResearchView source;
        try (var input = ResearchBackgroundTest.class.getResourceAsStream("/tfr-research-view.json")) {
            source = new dev.hoi.protocol.research.ResearchPayloads.Response(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).view();
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        var view = new ResearchView(source.session(), source.revision(), source.country(), source.countryName(), source.day(), source.date(), source.speed(),
                List.of(new ResearchView.Slot(0, "", 0)), source.technologies(), source.message(), source.hud(), source.benefits());
        int scale = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.waitTicks(5);
                context.setScreen(() -> new ResearchScreen(view, request -> {
                    if (request.action() == dev.hoi.protocol.research.ResearchPayloads.Action.START || request.action() == dev.hoi.protocol.research.ResearchPayloads.Action.CANCEL)
                        throw new AssertionError("Background navigation sent a research mutation");
                }));
                context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = (ResearchScreen)client.gui.screen();
                    var slot = screen.children().stream().filter(ResearchSlotButton.class::isInstance)
                            .map(ResearchSlotButton.class::cast).filter(button -> button.getMessage().getString().equals(dev.hoi.client.ui.UiCommandLabels.text("RESEARCH_NO_RESEARCH", "사용되지 않음"))).findFirst().orElseThrow();
                    activate(slot);
                    if (screen.allowsMovement()) throw new AssertionError("Slot did not open tree: " + slot.getX() + "," + slot.getY() + " in " + screen.width + "x" + screen.height);
                });
                context.waitTicks(3);
                var sprites = List.of("infantry_techtree_bg", "support_techtree_bg", "armortech_bg", "artillery_techtree_bg",
                        "naval_techtree_bg", "naval_techtree_bg", "air_techtree_bg", "engineering_techtree_bg", "industry_techtree_bg");
                for (int index = 0; index < sprites.size(); index++) {
                    int tab = index;
                    context.runOnClient(client -> {
                        var screen = (ResearchScreen)client.gui.screen();
                        var button = screen.children().stream().filter(ResearchTabButton.class::isInstance).map(ResearchTabButton.class::cast).toList().get(tab);
                        if (button.getX() != Math.round((19 + tab * 89) / 2f) || button.getWidth() != 43 || button.getHeight() != 31)
                            throw new AssertionError("Research tabs lost original source geometry");
                        activate(button);
                        try {
                            var padding = ResearchScreen.class.getDeclaredField("treePadding"); padding.setAccessible(true);
                            if (padding.getInt(screen) != 0) throw new AssertionError("Source tree retained a blank top inset");
                        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
                        for (String asset : List.of("gfx_tiled_plain_bg2", "gfx_tiled_window_3b_border"))
                            if (UiAssets.imageSize("research/source/" + asset + "/frame_0")[0] == 0) throw new AssertionError("Missing original fullscreen frame " + asset);
                        if (!sprites.get(tab).isEmpty() && UiAssets.imageSize("research/source/gfx_" + sprites.get(tab) + "/atlas")[0] == 0)
                            throw new AssertionError("Missing source background " + sprites.get(tab));
                    });
                    context.getInput().setCursorPos(size[0] - 10, size[1] - 10);
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-research-source-background-" + index + "-" + size[0] + "-gui2");
                    if (index == 1) {
                        dev.hoi.client.ui.hud.HudFullscreenTest.run(context, "research", size[0]);
                        treeBehindDetail(context, size[0]);
                    }
                }
            }
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> client.options.guiScale().set(scale));
        }
    }
    private static void treeBehindDetail(ClientGameTestContext context, int width) {
        try {
            var nodes = context.computeOnClient(client -> {
                try {
                    var field = ResearchScreen.class.getDeclaredField("layout"); field.setAccessible(true);
                    return ((ResearchLayout)field.get(client.gui.screen())).nodes();
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            var node = nodes.getFirst();
            context.getInput().setCursorPos(width - 2, width * 9 / 16 - 2); context.waitTicks(2);
            var before = javax.imageio.ImageIO.read(context.takeScreenshot("hoi-research-tree-before-detail-" + width + "-gui2").toFile());
            var area = context.computeOnClient(client -> {
                var screen = (ResearchScreen)client.gui.screen();
                screen.showDetail(node.tech().id());
                if (screen.children().stream().filter(ResearchTabButton.class::isInstance).map(ResearchTabButton.class::cast).anyMatch(button -> !button.visible))
                    throw new AssertionError("Research tabs disappeared behind the detail window");
                try {
                    var field = ResearchScreen.class.getDeclaredField("panelX"); field.setAccessible(true);
                    var panelWidth = ResearchScreen.class.getDeclaredField("panelW"); panelWidth.setAccessible(true);
                    var panelTop = ResearchScreen.class.getDeclaredField("panelY"); panelTop.setAccessible(true);
                    var panelHeight = ResearchScreen.class.getDeclaredField("panelH"); panelHeight.setAccessible(true);
                    var layout = ResearchScreen.class.getDeclaredField("layout"); layout.setAccessible(true);
                    if (((ResearchLayout)layout.get(screen)).nodes().size() != nodes.size()) throw new AssertionError("Opening research detail filtered the tree nodes");
                    return new int[]{field.getInt(screen), screen.treeTop(), screen.height, panelWidth.getInt(screen), screen.width, panelTop.getInt(screen), panelHeight.getInt(screen)};
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            context.waitTicks(3);
            var after = javax.imageio.ImageIO.read(context.takeScreenshot("hoi-research-tree-under-detail-" + width + "-gui2").toFile());
            int visible = 0, compared = 0;
            for (int y = area[1] * 2; y < (area[2] - 10) * 2; y++) for (int x = 0; x < area[4] * 2; x++) {
                if (x >= (area[0] - 2) * 2 && x < (area[0] + area[3] + 2) * 2 && y >= (area[5] - 2) * 2 && y < (area[5] + area[6] + 2) * 2) continue;
                if (x >= (node.x() - 2) * 2 && x < (node.x() + node.width() + 2) * 2
                        && y >= (area[1] + node.y() - 2) * 2 && y < (area[1] + node.y() + node.height() + 2) * 2) continue;
                if (before.getRGB(x, y) != after.getRGB(x, y)) throw new AssertionError("Opening research detail changed the exposed tree at " + x + "," + y);
                compared++;
                if ((after.getRGB(x, y) & 0xFFFFFF) != 0) visible++;
            }
            if (visible < 100) throw new AssertionError("Research tree persistence compared an empty background");
            java.nio.file.Files.writeString(java.nio.file.Path.of("research-tree-preservation-" + width + ".json"), new com.google.gson.Gson().toJson(java.util.Map.of("comparedPixels", compared, "changedPixels", 0, "nodesBefore", nodes.size(), "nodesAfter", nodes.size())));
            var next = context.computeOnClient(client -> {
                try {
                    var screen = (ResearchScreen)client.gui.screen();
                    var field = ResearchScreen.class.getDeclaredField("layout"); field.setAccessible(true);
                    return ((ResearchLayout)field.get(screen)).nodes().stream().filter(candidate -> !candidate.tech().id().equals(node.tech().id())
                            && (candidate.x() + candidate.width() < area[0] - 4 || candidate.x() > area[0] + area[3] + 4)
                            && candidate.x() + candidate.width() < area[4] && candidate.y() + screen.treeTop() + candidate.height() < screen.height - 10).findFirst().orElseThrow();
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            int nextX = next.x() + next.width() / 2, nextY = area[1] + next.y() + next.height() / 2;
            context.getInput().setCursorPos(nextX * 2, nextY * 2); context.waitTicks(4);
            context.takeScreenshot("hoi-research-exposed-tree-tooltip-" + width + "-gui2");
            context.runOnClient(client -> {
                var screen = (ResearchScreen)client.gui.screen();
                screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(nextX, nextY, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                try {
                    var field = ResearchScreen.class.getDeclaredField("detail"); field.setAccessible(true);
                    if (!next.tech().id().equals(field.get(screen))) throw new AssertionError("Exposed research node did not open its detail");
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            context.getInput().setCursorPos(0, 0); context.waitTicks(2);
            context.takeScreenshot("hoi-research-exposed-tree-selected-" + width + "-gui2");
            context.runOnClient(client -> client.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE, 0, 0)));
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    private static void activate(net.minecraft.client.gui.components.Button button) {
        if (!button.active) throw new AssertionError("Research navigation disabled " + button.getMessage().getString());
        button.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
    }
}
