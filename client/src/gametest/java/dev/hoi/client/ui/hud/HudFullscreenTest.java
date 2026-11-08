package dev.hoi.client.ui.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.dialog.DialogStack;
import dev.hoi.client.screen.diplomacy.DiplomacyWorldTension;
import dev.hoi.protocol.diplomacy.DiplomacyWorldTensionPayloads;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class HudFullscreenTest {
    private HudFullscreenTest() {}
    public static void run(ClientGameTestContext context, String name, int width) {
        var screen = context.computeOnClient(client -> client.gui.screen());
        var requests = new AtomicInteger();
        context.runOnClient(client -> {
            DiplomacyWorldTensionPayloads.registerPayloadTypes();
            if (!ServerPlayNetworking.registerGlobalReceiver(DiplomacyWorldTensionPayloads.Request.TYPE, (packet, connection) -> requests.incrementAndGet()))
                throw new AssertionError("The fullscreen fixture must own its temporary tension receiver");
        });
        context.waitTicks(4);
        var previous = context.computeOnClient(client -> dev.hoi.client.ClientCampaignHud.visible()
                ? dev.hoi.protocol.hud.HudPayloads.State.of(dev.hoi.client.ClientCampaignHud.country(), dev.hoi.client.ClientCampaignHud.hud())
                : dev.hoi.protocol.hud.HudPayloads.State.HIDDEN);
        try {
            var menu = new dev.hoi.protocol.menu.MenuPayloads.OpenScreen(java.nio.file.Files.readString(java.nio.file.Path.of(System.getenv("HOI_COMMANDER_PREVIEW"), "kor-selected-menu.json"))).view();
            context.runOnClient(client -> dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.hud.HudPayloads.State.of(menu.country(), menu.hud())));
            context.runOnClient(client -> {
                if (!ClientPlayNetworking.canSend(DiplomacyWorldTensionPayloads.Request.TYPE)) throw new AssertionError("Temporary tension receiver was not advertised");
            });
            check(context, name, width, requests);
            context.runOnClient(client -> dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.hud.HudPayloads.State.of("KOR", dev.hoi.client.ClientCampaignHud.hud())));
            context.setScreen(() -> new DialogStack(screen));
            check(context, name + "-stacked", width, requests);
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        finally {
            context.setScreen(() -> screen);
            context.runOnClient(client -> {
                dev.hoi.client.ClientCampaignHud.accept(previous);
                ServerPlayNetworking.unregisterGlobalReceiver(DiplomacyWorldTensionPayloads.Request.TYPE.id());
            });
        }
    }
    private static void check(ClientGameTestContext context, String name, int width, AtomicInteger requests) {
        var screen = context.computeOnClient(client -> client.gui.screen());
        var content = context.computeOnClient(client -> DialogCoordinator.contentScreen());
        int boundary = name.startsWith("doctrine") ? 41 : HudMenuBar.height(screen.width);
        int x = HudMenuBar.tensionX(screen.width) + 4;
        context.runOnClient(client -> {
            var buttons = content.children().stream().filter(child -> child instanceof AbstractWidget widget
                    && List.of("데프콘", "세계 긴장도 %").contains(widget.getMessage().getString())).map(child -> (AbstractWidget)child).toList();
            if (buttons.size() != 2 || !buttons.getFirst().isMouseOver(x, 8)) throw new AssertionError("Fullscreen must retain the exposed tension buttons: " + name);
            for (var button : buttons) if (button.getHeight() > 0 && button.getY() + button.getHeight() > boundary)
                throw new AssertionError("Tension input extends into the fullscreen window: " + name);
        });
        context.getInput().setCursorPos(0, 0); context.waitTicks(3);
        context.takeScreenshot("hoi-fullscreen-header-" + name + "-" + width + "-gui2");
        context.getInput().setCursorPos(x * 2, 16); context.waitTicks(4);
        context.runOnClient(client -> requireTooltip("world_tension", name));
        context.takeScreenshot("hoi-fullscreen-tension-hover-" + name + "-" + width + "-gui2");
        int before = requests.get();
        context.runOnClient(client -> {
            click(screen, x, 8, InputConstants.MOUSE_BUTTON_LEFT, false);
            if (!(DialogCoordinator.contentScreen() instanceof DiplomacyWorldTension)) throw new AssertionError("Exposed DEFCON click must open world tension: " + name);
        });
        context.waitTicks(3);
        if (requests.get() <= before) throw new AssertionError("World tension must send its request: " + name);
        context.takeScreenshot("hoi-fullscreen-tension-open-" + name + "-" + width + "-gui2");
        restore(context, screen, content);
        context.getInput().setCursorPos(x * 2, 88); context.waitTicks(4);
        context.runOnClient(client -> {
            if (activeTooltip() != null && activeTooltip().icon().equals("world_tension")) throw new AssertionError("Covered tension must not hover: " + name);
            for (int button : List.of(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT)) {
                click(screen, x, 44, button, false);
                click(screen, x, 44, button, true);
                if (DialogCoordinator.contentScreen() != content) throw new AssertionError("Covered tension stole a window click: " + name);
            }
        });
        context.takeScreenshot("hoi-fullscreen-covered-input-" + name + "-" + width + "-gui2");
        context.getInput().setCursorPos((HudMenuBar.flagWidth(screen.width) + 14) * 2, 12); context.waitTicks(4);
        context.runOnClient(client -> requireTooltip("political_power", name));
        context.takeScreenshot("hoi-fullscreen-indicator-tooltip-" + name + "-" + width + "-gui2");
        if (!name.startsWith("doctrine")) {
            context.runOnClient(client -> {
                if (!click(screen, screen.width - 15, 44, InputConstants.MOUSE_BUTTON_LEFT, false))
                    throw new AssertionError("The window close button must own the overlap: " + name);
                if (DialogCoordinator.contentScreen() == content) throw new AssertionError("The covered close button did not close the window: " + name);
            });
            restore(context, screen, content);
        }
        context.getInput().setCursorPos(0, 0); context.waitTicks(2);
    }
    private static void restore(ClientGameTestContext context, Screen screen, Screen content) {
        context.runOnClient(client -> {
            if (screen instanceof DialogStack) {
                try {
                    var field = DialogStack.class.getDeclaredField("backdrop"); field.setAccessible(true); field.set(screen, content);
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            }
        });
        context.setScreen(() -> screen);
        context.waitTicks(2);
    }
    private static boolean click(Screen screen, int x, int y, int button, boolean twice) {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
        boolean handled = screen.mouseClicked(event, twice);
        screen.mouseReleased(event);
        return handled;
    }
    private static HudMenuBar.HudIndicator activeTooltip() {
        try {
            var field = HudTooltip.class.getDeclaredField("activeItem"); field.setAccessible(true);
            return (HudMenuBar.HudIndicator)field.get(null);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static void requireTooltip(String icon, String name) {
        if (activeTooltip() == null || !activeTooltip().icon().equals(icon)) throw new AssertionError("Missing " + icon + " tooltip: " + name);
    }
}
