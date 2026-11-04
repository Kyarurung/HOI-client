package dev.hoi.client.screen.politics;

import dev.hoi.protocol.dialog.DialogPayloads;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.protocol.menu.MenuPayloads;
import dev.hoi.protocol.menu.MenuTab;
import dev.hoi.client.screen.ScreenMenu;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;

public final class PoliticsChoiceTest {
    private PoliticsChoiceTest() {}
    public static void run(ClientGameTestContext context) {
        String directory = System.getenv("HOI_POLITICS_CHOICE_PREVIEW");
        if (directory == null) throw new AssertionError("Authoritative PRC choice fixtures are required");
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        try {
            var menu = new MenuPayloads.OpenScreen(Files.readString(Path.of(directory, "prc-menu.json"))).view();
            if (System.getProperty("hoi.testSuite", "all").equals("politics-choice-missing")) {
                var view = new DialogPayloads.Show(Files.readString(Path.of(directory, "army_chief-ready.json")), true).view();
                context.runOnClient(client -> {
                    if (client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/politics/choice/header/frame_0.png")).isPresent())
                        throw new AssertionError("Missing-pack reproduction must remove the real choice assets");
                });
                context.setScreen(() -> new PoliticsChoice(view, new ScreenMenu(menu, MenuTab.POLITICS, () -> {})));
                context.getInput().setCursorPos(2400, 1300);
                context.waitTicks(3);
                context.takeScreenshot("hoi-prc-choice-missing-pack-gui2");
                return;
            }
            for (String name : List.of("army_chief", "navy_chief", "air_chief", "high_command", "law", "minister", "foreign", "finance", "interior", "intelligence", "defense")) {
                var view = new DialogPayloads.Show(Files.readString(Path.of(directory, name + "-ready.json")), true).view();
                for (var tab : List.of(MenuTab.POLITICS, MenuTab.OFFICER_CORPS)) {
                    if ((name.equals("law") || view.image().startsWith("minister/")) && tab == MenuTab.OFFICER_CORPS) continue;
                    var sent = new ArrayList<DialogPayloads.Action>();
                    context.setScreen(() -> new PoliticsChoice(view, new ScreenMenu(menu, tab, () -> {}), sent::add));
                    context.getInput().setCursorPos(2400, 1300);
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-prc-choice-" + name + "-" + tab.name().toLowerCase(java.util.Locale.ROOT) + "-ready-gui2");
                    context.runOnClient(client -> {
                        var screen = (PoliticsChoice)client.gui.screen();
                        var buttons = screen.children().stream().filter(c -> c instanceof Button).map(c -> (Button)c).toList();
                        var first = buttons.get(1);
                        if (first.getX() != 360 || first.getY() != (view.image().startsWith("advisor/") ? 97 : 68))
                            throw new AssertionError("Both parent tabs must use the native choice anchor: " + first.getX() + "," + first.getY());
                        if (view.image().startsWith("minister/") && buttons.size() > 2) {
                            var second = buttons.get(2);
                            if (second.getX() != first.getX() + 115 || second.getY() != first.getY())
                                throw new AssertionError("Every minister must use two columns: " + name);
                        }
                        for (var tile : view.tiles()) {
                            if (client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + tile.icon() + ".png")).isEmpty())
                                throw new AssertionError("Missing actual PRC portrait: " + tile.icon());
                            if (view.image().startsWith("minister/") && !tile.overlay().equals("politics/choice/locked"))
                                throw new AssertionError("All six 2020 PRC minister slots must be locked: " + name);
                            if (PoliticsChoice.grayscale(tile)) {
                                verifyGrayscale(client, tile.icon());
                                if (tile.icon().startsWith("politics/appointments/") && dev.hoi.client.ui.UiAssets.imageHeight(tile.icon(), 156) == 210)
                                    verifyGrayscale(client, "politics/vacant");
                            }
                        }
                        var open = buttons.stream().skip(1).filter(b -> b.active).findFirst().orElse(null);
                        for (var disabled : buttons.stream().skip(1).filter(b -> !b.active).toList()) {
                            screen.mouseClicked(new MouseButtonEvent(disabled.getX() + 1, disabled.getY() + 1, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                            if (!sent.isEmpty()) throw new AssertionError("Disabled candidates must not submit a choice");
                        }
                        if (open != null) {
                            var right = new MouseButtonEvent(open.getX() + 1, open.getY() + 1, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0));
                            screen.mouseClicked(right, false);
                            if (!sent.isEmpty()) throw new AssertionError("Right click must not submit a choice");
                            screen.mouseDragged(right, 10, 10);
                            if (!sent.isEmpty()) throw new AssertionError("Dragging must not submit a choice");
                            var left = new MouseButtonEvent(open.getX() + 1, open.getY() + 1, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                            screen.mouseClicked(left, false);
                            screen.mouseClicked(left, true);
                            if (sent.size() != 1) throw new AssertionError("Repeated click must submit exactly once");
                            screen.update(view);
                            if (screen.children().stream().filter(c -> c instanceof Button).map(c -> (Button)c).anyMatch(b -> b.active))
                                throw new AssertionError("Same revision must not unlock pending input");
                            screen.update(view.issued(view.token(), view.revision() + 1));
                            if (screen.children().stream().filter(c -> c instanceof Button).map(c -> (Button)c).skip(1).noneMatch(b -> b.active))
                                throw new AssertionError("Authoritative new revision must unlock input");
                        }
                    });
                    if (name.equals("army_chief") && tab == MenuTab.POLITICS) {
                        String grayscalePath = view.tiles().stream().filter(PoliticsChoice::grayscale).findFirst().orElseThrow().icon();
                        context.runOnClient(client -> {
                            var id = net.minecraft.resources.Identifier.parse("hoi:grayscale/textures/gui/" + grayscalePath + ".png");
                            var image = ((net.minecraft.client.renderer.texture.DynamicTexture)client.getTextureManager().getTexture(id)).getPixels();
                            dev.hoi.client.ui.UiAssets.clear();
                            if (!image.isClosed()) throw new AssertionError("Resource reload must release grayscale native images");
                        });
                        context.waitTicks(3);
                        context.takeScreenshot("hoi-prc-choice-army_chief-politics-reloaded-gui2");
                        context.runOnClient(client -> verifyGrayscale(client, grayscalePath));
                    }
                }
                var poor = new DialogPayloads.Show(Files.readString(Path.of(directory, name + "-poor.json")), true).view();
                context.setScreen(() -> new PoliticsChoice(poor, new ScreenMenu(menu, MenuTab.POLITICS, () -> {})));
                context.getInput().setCursorPos(2400, 1300);
                context.waitTicks(3);
                context.takeScreenshot("hoi-prc-choice-" + name + "-politics-poor-gui2");
                var point = context.computeOnClient(client -> {
                    var button = (Button)client.gui.screen().children().get(1);
                    return new int[]{button.getX() * 2 + 10, button.getY() * 2 + 10};
                });
                context.getInput().setCursorPos(point[0], point[1]);
                context.waitTicks(18);
                context.takeScreenshot("hoi-prc-choice-" + name + "-tooltip-gui2");
            }
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        } finally {
            context.setScreen(() -> null);
        }
    }
    private static void verifyGrayscale(net.minecraft.client.Minecraft client, String path) {
        var source = net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + path + ".png");
        var id = net.minecraft.resources.Identifier.parse("hoi:grayscale/textures/gui/" + path + ".png");
        if (!(client.getTextureManager().getTexture(id) instanceof net.minecraft.client.renderer.texture.DynamicTexture texture))
            throw new AssertionError("Disabled portrait must use a grayscale runtime texture: " + path);
        try (var stream = client.getResourceManager().getResource(source).orElseThrow().open();
                var original = com.mojang.blaze3d.platform.NativeImage.read(stream)) {
            var image = texture.getPixels();
            if (image.getWidth() != original.getWidth() || image.getHeight() != original.getHeight())
                throw new AssertionError("Grayscale must retain every source pixel: " + path);
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixel(x, y);
                if ((pixel >>> 24) != (original.getPixel(x, y) >>> 24)
                        || (pixel >>> 16 & 255) != (pixel >>> 8 & 255) || (pixel & 255) != (pixel >>> 8 & 255))
                    throw new AssertionError("Portrait, frame and alpha must retain the original grayscale rule: " + path);
            }
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }
}
