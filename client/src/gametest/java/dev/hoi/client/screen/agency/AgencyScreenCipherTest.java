package dev.hoi.client.screen.agency;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.commander.CommanderConfirmation;
import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.hud.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.nio.file.*;
import java.util.*;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

public final class AgencyScreenCipherTest {
    private static Button button(Screen screen, String id) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(value -> value.getMessage().getString().equals(id)).findFirst().orElseThrow(() -> new AssertionError("Missing cipher control " + id));
    }
    private static void click(Screen screen, String id, int mouse) {
        var button = button(screen, id);
        var event = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(mouse, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    public static void run(ClientGameTestContext context, String directory) {
        Path path = Path.of(directory, "kor-agency-cryptology.json");
        if (!Files.isRegularFile(path)) return;
        AgencyView view;
        try { view = new AgencyPayloads.Response(Files.readString(path)).view(); }
        catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        if (view.ciphers().isEmpty()) return;
        var previous = context.computeOnClient(client -> ClientCampaignHud.country().isEmpty() ? HudPayloads.State.HIDDEN : HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()));
        context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of(view.country(), ClientCampaignHud.hud())));
        try {
            for (int width : new int[]{2560, 1280}) {
                context.getInput().resizeWindow(width, width * 9 / 16);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var sent = new ArrayList<AgencyPayloads.Request>();
                context.getInput().setCursorPos(width - 2, 80);
                context.setScreen(() -> new AgencyScreen(null, view.session(), view, sent::add));
                context.waitTicks(3);
                context.runOnClient(client -> click(client.gui.screen(), "암호학", MOUSE_BUTTON_LEFT));
                context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
                context.takeScreenshot("hoi-cipher-four-states-" + width + "-gui2");
                if (width == 1280) {
                    context.runOnClient(client -> client.gui.screen().mouseScrolled(20, client.gui.screen().height - 10, 0, -1));
                    context.waitTicks(2);
                    context.takeScreenshot("hoi-cipher-active-visible-" + width + "-gui2");
                }
                context.runOnClient(client -> {
                    var screen = (AgencyScreen)client.gui.screen();
                    if (width == 1280) screen.mouseScrolled(20, screen.height - 10, 0, -100);
                    click(screen, "pause:PRK", MOUSE_BUTTON_RIGHT);
                    if (!sent.isEmpty()) throw new AssertionError("Right click mutated cipher state");
                    click(screen, "pause:PRK", MOUSE_BUTTON_LEFT); click(screen, "pause:PRK", MOUSE_BUTTON_LEFT);
                    if (sent.size() != 1 || !sent.getFirst().item().equals("pause:PRK") || sent.getFirst().revision() != view.revision()) throw new AssertionError("Cipher pause lost issued revision or request limit");
                });
                for (String id : List.of("crypto_delete:PRK", "reveal:PRC")) {
                    sent.clear();
                    context.getInput().setCursorPos(width - 2, 80);
                    context.setScreen(() -> new AgencyScreen(null, view.session(), view, sent::add));
                    context.waitTicks(3);
                    context.runOnClient(client -> {
                        click(client.gui.screen(), "암호학", MOUSE_BUTTON_LEFT);
                        if (width == 1280) client.gui.screen().mouseScrolled(20, client.gui.screen().height - 10, 0, -100);
                    });
                    context.waitTicks(2);
                    context.takeScreenshot("hoi-cipher-" + id.replace(':', '-') + "-row-" + width + "-gui2");
                    context.runOnClient(client -> {
                        click(client.gui.screen(), id, MOUSE_BUTTON_LEFT);
                        if (!(client.gui.screen() instanceof CommanderConfirmation) || !sent.isEmpty()) throw new AssertionError("Cipher action bypassed source confirmation");
                    });
                    context.waitTicks(2);
                    context.takeScreenshot("hoi-cipher-" + id.replace(':', '-') + "-confirm-" + width + "-gui2");
                    context.runOnClient(client -> {
                        var confirmation=(CommanderConfirmation)client.gui.screen();
                        var agency=(AgencyScreen)confirmation.backdrop();
                        var dated=new dev.hoi.protocol.agency.AgencyView(view.session(),view.revision()+1,view.country(),view.name(),view.status(),view.items(),view.message(),view.candidates(),view.capitals(),view.operatives(),view.operations(),view.networks(),view.offers(),view.operativeCapacity(),view.confirms(),view.ciphers());
                        agency.update(dated);
                        if(client.gui.screen()!=confirmation)throw new AssertionError("Date-only agency update must preserve the issued confirmation");
                        click(client.gui.screen(), "OK", MOUSE_BUTTON_LEFT);
                        if (sent.size() != 1 || !sent.getFirst().item().equals(id) || sent.getFirst().revision()!=dated.revision()) throw new AssertionError("Cipher confirmation did not send exact action with the latest received revision");
                    });
                }
            }
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> ClientCampaignHud.accept(previous));
        }
    }
}
