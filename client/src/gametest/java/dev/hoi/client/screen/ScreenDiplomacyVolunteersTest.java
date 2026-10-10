package dev.hoi.client.screen;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.protocol.ProtocolCountryHud;
import dev.hoi.protocol.ProtocolDiplomacyVolunteerPayloads;
import dev.hoi.protocol.ProtocolDiplomacyVolunteerView;
import dev.hoi.protocol.ProtocolHudPayloads;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import static com.mojang.blaze3d.platform.InputConstants.*;
import static dev.hoi.protocol.ProtocolDiplomacyVolunteerPayloads.Operation.*;
import static dev.hoi.protocol.ProtocolDiplomacyVolunteerPayloads.Status.UPDATED;

public final class ScreenDiplomacyVolunteersTest {
    private ScreenDiplomacyVolunteersTest() {}
    public static void run(ClientGameTestContext context) {
        try { runWithFixtures(context); }
        catch (IOException failure) { throw new UncheckedIOException("Cannot load authoritative volunteer fixtures", failure); }
    }
    private static void runWithFixtures(ClientGameTestContext context) throws IOException {
        String directory = System.getenv("HOI_VOLUNTEER_PREVIEW");
        if (directory == null || directory.isBlank()) throw new AssertionError("Authoritative volunteer fixtures are required");
        var initial = read(directory, "unselected");
        var selected = read(directory, "selected");
        var multipleSelected = read(directory, "multiple-selected");
        var air = read(directory, "air-disabled");
        for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.runOnClient(client -> client.options.guiScale().set(2));
            int stage = 0;
            for (var view : List.of(initial, selected, multipleSelected, air)) {
                var requests = new ArrayList<ProtocolDiplomacyVolunteerPayloads.Request>();
                context.runOnClient(client -> ClientCampaignHud.accept(ProtocolHudPayloads.State.of(view.viewer(), ProtocolCountryHud.UNKNOWN)));
                context.setScreen(() -> new ScreenDiplomacyVolunteers(null, view, requests::add));
                context.waitTicks(3);
                context.runOnClient(client -> {
                    var screen = (ScreenDiplomacyVolunteers)client.gui.screen();
                    check(screen.view().equals(view) && requests.isEmpty(), "Rendering changed authoritative volunteer state");
                    check(screen.canSubmit() == view.submitEnabled(), "Submit permission differs from source quote");
                    check(!screen.allowsMovement(), "Volunteer dialog leaked movement input");
                    for (var child : screen.children()) if (child instanceof AbstractWidget widget)
                        check(widget.getX() >= 0 && widget.getY() >= 0 && widget.getX() + widget.getWidth() <= screen.width
                                && widget.getY() + widget.getHeight() <= screen.height, "Volunteer control escaped viewport");
                    for (String path : List.of("hoi:ui/diplomacy_labels.json", "hoi:textures/gui/diplomacy/volunteers/background.png",
                            "hoi:textures/gui/diplomacy/volunteers/row/frame_0.png", "hoi:textures/gui/diplomacy/volunteers/row/frame_1.png"))
                        check(client.getResourceManager().getResource(Identifier.parse(path)).isPresent(), "Missing source asset: " + path);
                });
                context.takeScreenshot("hoi-diplomacy-volunteers-" + List.of("unselected", "selected", "multiple-selected", "air-disabled").get(stage++) + "-" + size[0] + "x" + size[1] + "-gui2");
            }
        }
        context.getInput().resizeWindow(2560, 1440);
        requests(context, initial, selected, air);
        cancelRefresh(context, initial);
        context.runOnClient(client -> ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN));
        context.setScreen(() -> null);
    }
    private static ProtocolDiplomacyVolunteerView read(String directory, String stage) throws IOException {
        return new ProtocolDiplomacyVolunteerPayloads.Response(UUID.randomUUID().toString(), ProtocolDiplomacyVolunteerPayloads.Status.OPEN,
                Files.readString(Path.of(directory, stage + ".json")), "").view();
    }
    private static void cancelRefresh(ClientGameTestContext context, ProtocolDiplomacyVolunteerView initial) {
        var requests = new ArrayList<ProtocolDiplomacyVolunteerPayloads.Request>();
        var parent = context.computeOnClient(client -> new ScreenCountry(initial.target()));
        context.runOnClient(client -> ClientCampaignHud.accept(ProtocolHudPayloads.State.of(initial.viewer(), ProtocolCountryHud.UNKNOWN)));
        context.setScreen(() -> new ScreenDiplomacyVolunteers(parent, initial, requests::add));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (ScreenDiplomacyVolunteers)client.gui.screen();
            String previous = parent.token();
            screen.onClose();
            check(client.gui.screen() == parent && !parent.token().equals(previous), "Cancel restored the consumed country session without requesting refresh");
            String refreshed = parent.token();
            screen.onClose();
            check(requests.size() == 1 && requests.getFirst().operation() == CANCEL && parent.token().equals(refreshed), "Repeated cancellation reopened or refreshed twice");
        });
    }
    private static ProtocolDiplomacyVolunteerView issued(ProtocolDiplomacyVolunteerView source, String session, long revision) {
        return new ProtocolDiplomacyVolunteerView(session, revision, source.viewer(), source.target(), source.title(),
                source.sourceDescription(), source.transferText(), source.unassignedText(), source.armies(), source.selectedArmyKeys(),
                source.summaryText(), source.capText(), source.threatText(), source.airEnabled(), source.airSelected(),
                source.airReason(), source.submitEnabled(), source.submitReason());
    }
    private static void requests(ClientGameTestContext context, ProtocolDiplomacyVolunteerView original,
                                 ProtocolDiplomacyVolunteerView selectedSource, ProtocolDiplomacyVolunteerView airSource) {
        check(original.selectedArmyKeys().isEmpty(), "Unselected source fixture already contains an army");
        check(!selectedSource.selectedArmyKeys().isEmpty(), "Selected source fixture has no actual army");
        String key = selectedSource.selectedArmyKeys().getFirst();
        var row = original.armies().stream().filter(army -> army.key().equals(key) && army.allowed()).findFirst()
                .orElseThrow(() -> new AssertionError("Selected fixture must identify an issued eligible army"));
        String session = UUID.randomUUID().toString();
        var initial = issued(original, session, 1);
        var selected = issued(selectedSource, session, 2);
        var requests = new ArrayList<ProtocolDiplomacyVolunteerPayloads.Request>();
        var parent = context.computeOnClient(client -> new Screen(Component.literal(original.title())) {});
        context.runOnClient(client -> ClientCampaignHud.accept(ProtocolHudPayloads.State.of(initial.viewer(), ProtocolCountryHud.UNKNOWN)));
        context.setScreen(() -> new ScreenDiplomacyVolunteers(parent, initial, requests::add));
        context.waitTicks(3);
        context.runOnClient(client -> {
            var screen = (ScreenDiplomacyVolunteers)client.gui.screen();
            var button = button(screen, row.name());
            click(screen, button, MOUSE_BUTTON_RIGHT);
            screen.setFocused(null);
            screen.keyPressed(new KeyEvent(KEY_RETURN, 0, 0));
            check(requests.isEmpty(), "Right click or a new Enter shortcut changed volunteer state");
            click(screen, button, MOUSE_BUTTON_LEFT);
            screen.select(key); screen.toggleAir(); screen.submit(); screen.onClose();
            check(requests.size() == 1 && requests.getFirst().operation() == SELECT && requests.getFirst().key().equals(key)
                    && requests.getFirst().selected() && requests.getFirst().revision() == 1, "Selection changed or duplicated while pending");
            check(screen.view().selectedArmyKeys().isEmpty() && !screen.canSubmit(), "Local click invented an authoritative selection");
            String token = requests.getFirst().token();
            screen.update(ProtocolDiplomacyVolunteerPayloads.Response.of(UUID.randomUUID().toString(), UPDATED, selected));
            screen.update(ProtocolDiplomacyVolunteerPayloads.Response.of(token, UPDATED, issued(selectedSource, UUID.randomUUID().toString(), 20)));
            check(screen.pending() && screen.view().revision() == 1, "Wrong token or session replaced the quote");
            screen.update(ProtocolDiplomacyVolunteerPayloads.Response.of(token, UPDATED, selected));
            check(!screen.pending() && screen.view().selectedArmyKeys().equals(selected.selectedArmyKeys()), "Matching quote did not apply selection");
            screen.update(initial);
            check(screen.view().revision() == 2, "Older revision replaced the quote");
            screen.select(key);
            check(requests.size() == 2 && !requests.getLast().selected(), "Selected army did not issue deselection");
            screen.update(ProtocolDiplomacyVolunteerPayloads.Response.of(requests.getLast().token(), UPDATED, issued(original, session, 3)));
            check(screen.view().selectedArmyKeys().isEmpty(), "Server deselection was not applied");
            screen.toggleAir();
            if (original.airEnabled()) {
                check(requests.getLast().operation() == AIR, "Enabled air choice did not request a quote");
                screen.update(ProtocolDiplomacyVolunteerPayloads.Response.of(requests.getLast().token(), UPDATED, issued(airSource, session, 4)));
                check(screen.view().airSelected() == airSource.airSelected(), "Air choice differs from server quote");
            } else check(requests.size() == 2, "Disabled air volunteers reached transport");
            screen.select(key);
            screen.update(ProtocolDiplomacyVolunteerPayloads.Response.of(requests.getLast().token(), UPDATED, issued(selectedSource, session, 5)));
            int beforeSubmit = requests.size();
            screen.submit(); screen.submit();
            if (selectedSource.submitEnabled()) check(requests.size() == beforeSubmit + 1 && requests.getLast().operation() == SUBMIT
                    && requests.getLast().revision() == 5, "Submission did not retain its exact latest quote");
            else {
                check(requests.size() == beforeSubmit, "Disabled volunteer deployment reached transport");
                screen.select(key);
            }
            for (int tick = 0; tick < 210; tick++) screen.tick();
            check(requests.stream().filter(value -> value.operation() == SUBMIT).count() == (selectedSource.submitEnabled() ? 1 : 0), "Timeout replayed a deployment");
            check(requests.stream().filter(value -> value.operation() == OPEN).count() == 1, "Timeout replayed its read-only refresh");
            check(!screen.canSubmit() && !screen.pending(), "Unanswered refresh enabled stale submission");
            screen.keyPressed(new KeyEvent(KEY_ESCAPE, 0, 0));
            screen.onClose();
            check(requests.stream().filter(value -> value.operation() == CANCEL).count() == 1 && client.gui.screen() == parent,
                    "Escape did not return once to its parent");
            screen.update(issued(selectedSource, session, 50));
            check(client.gui.screen() == parent, "Late response reopened a closed volunteer screen");
        });
        context.setScreen(() -> new ScreenDiplomacyVolunteers(parent, initial, requests::add));
        context.waitTicks(2);
        double[] point = context.computeOnClient(client -> {
            var screen = (ScreenDiplomacyVolunteers)client.gui.screen();
            var button = button(screen, row.name());
            double pixels = client.getWindow().getGuiScale();
            return new double[]{(button.getX() + button.getWidth() * 83.0 / 489) * pixels, (button.getY() + button.getHeight() / 2.0) * pixels};
        });
        context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(10);
        context.takeScreenshot("hoi-diplomacy-volunteers-commander-tooltip-gui2");
        context.getInput().setCursorPos(2500, 1380); context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (ScreenDiplomacyVolunteers)client.gui.screen();
            var button = button(screen, row.name());
            int count = requests.size();
            double scale = button.getWidth() / 489.0;
            int index = initial.armies().indexOf(row) + (initial.unassignedText().isEmpty() ? 0 : 1);
            double titleX = button.getX() + 20 * scale;
            double titleY = button.getY() - (104 + index * 38) * scale;
            int oldX = button.getX(), oldY = button.getY();
            var titleStart = event(titleX, titleY, MOUSE_BUTTON_RIGHT);
            var titleEnd = event(titleX + 20, titleY + 10, MOUSE_BUTTON_RIGHT);
            screen.mouseClicked(titleStart, false); screen.mouseDragged(titleEnd, 20, 10); screen.mouseReleased(titleEnd);
            check(button(screen, row.name()).getX() == oldX && button(screen, row.name()).getY() == oldY, "Right-button drag moved the volunteer window");
            titleStart = event(titleX, titleY, MOUSE_BUTTON_LEFT);
            titleEnd = event(titleX + 20, titleY + 10, MOUSE_BUTTON_LEFT);
            screen.mouseClicked(titleStart, false); screen.mouseDragged(titleEnd, 20, 10); screen.mouseReleased(titleEnd);
            button = button(screen, row.name());
            check(button.getX() == oldX + 20 && button.getY() == oldY + 10, "Source moveable title did not drag the volunteer window");
            if (initial.armies().size() > 5) {
                double trackX = button.getX() + 501 * scale;
                double trackTop = button.getY() + (20 - index * 38) * scale;
                double trackBottom = button.getY() + (184 - index * 38) * scale;
                var scrollStart = event(trackX, trackTop, MOUSE_BUTTON_LEFT);
                var scrollEnd = event(trackX, trackBottom, MOUSE_BUTTON_LEFT);
                screen.mouseClicked(scrollStart, false); screen.mouseDragged(scrollEnd, 0, trackBottom - trackTop); screen.mouseReleased(scrollEnd);
                check(screen.scrollPosition() > 0, "Volunteer scrollbar drag did not move the list");
                screen.mouseScrolled(button.getX() + 2, button.getY() + 2, 0, 100);
                check(screen.scrollPosition() == 0, "Volunteer scroll wheel did not return to its first army");
                screen.mouseScrolled(button.getX() + 2, button.getY() + 2, 0, -100);
                check(screen.scrollPosition() > 0, "Volunteer army list did not scroll");
                check(screen.children().stream().anyMatch(child -> child instanceof Button entry && entry.getMessage().getString().equals(initial.armies().getLast().name())), "Last army is unreachable");
            }
            check(requests.size() == count, "Tooltip or scroll sent a mutation");
            ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);
            screen.submit(); screen.toggleAir(); screen.tick(); screen.update(issued(selectedSource, session, 30));
            check(requests.size() == count && client.gui.screen() == null, "Revoked campaign context retained private controls");
        });
    }
    private static Button button(ScreenDiplomacyVolunteers screen, String label) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().equals(label)).findFirst().orElseThrow(() -> new AssertionError("Missing volunteer button: " + label));
    }
    private static void click(ScreenDiplomacyVolunteers screen, Button button, int mouseButton) {
        var event = event(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, mouseButton);
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private static MouseButtonEvent event(double x, double y, int button) { return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0)); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
