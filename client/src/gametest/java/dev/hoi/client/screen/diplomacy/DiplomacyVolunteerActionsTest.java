package dev.hoi.client.screen.diplomacy;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.country.CountryPayloads;
import dev.hoi.protocol.country.CountryView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class DiplomacyVolunteerActionsTest {
    private DiplomacyVolunteerActionsTest() {}
    static void run(ClientGameTestContext context, CountryView source) {
        var entries = source.actions().entries();
        var ids = entries.stream().map(CountryView.Action::id).toList();
        check(ids.stream().distinct().count() == ids.size(), "Source contains duplicate diplomatic action IDs");
        check(ids.indexOf("volunteers") >= 0 && ids.indexOf("recall_volunteers") == ids.indexOf("volunteers") + 1,
                "Source recall action must immediately follow volunteer dispatch");
        check(entries.stream().filter(action -> action.name().equals("의용군 파병")).count() == 1
                && entries.stream().filter(action -> action.name().equals("의용군 소환")).count() == 1,
                "Source volunteer titles must each occur once");
        var names = entries.stream().filter(action -> !action.id().equals("open_logistics")).map(CountryView.Action::name).toList();
        int scale = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            for (int width : List.of(2560, 1280)) {
                context.getInput().resizeWindow(width, width * 9 / 16);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var requests = new ArrayList<CountryPayloads.Action>();
                context.setScreen(() -> new DiplomacyCountryScreen(source, requests::add));
                context.waitTicks(3);
                context.takeScreenshot("hoi-diplomacy-volunteer-actions-source-top-" + width + "-gui2");
                var seen = new LinkedHashSet<String>();
                context.runOnClient(client -> {
                    var screen = (DiplomacyCountryScreen)client.gui.screen();
                    for (int step = 0; step <= names.size(); step++) {
                        seen.addAll(visible(screen, names));
                        screen.mouseScrolled(screen.panelWidth() - 15, screen.height - 70, 0, -1);
                    }
                    check(seen.equals(new LinkedHashSet<>(names)), "Scrolling omitted or duplicated the authoritative action list");
                    for (String title : List.of("의용군 파병", "의용군 소환"))
                        check(screen.children().stream().filter(child -> child instanceof Button button && button.getMessage().getString().equals(title)).count() == 1,
                                "Visible volunteer title was duplicated or missing: " + title);
                    var dispatch = button(screen, "의용군 파병");
                    var recall = button(screen, "의용군 소환");
                    check(recall.getY() > dispatch.getY(), "Rendered recall action moved ahead of dispatch");
                    check(screen.view().equals(source), "Rendering changed authoritative tooltip, cost or availability data");
                    click(screen, dispatch, InputConstants.MOUSE_BUTTON_RIGHT);
                    click(screen, recall, InputConstants.MOUSE_BUTTON_RIGHT);
                    check(requests.isEmpty() && screen.children().stream().noneMatch(child -> child instanceof Button button && button.getMessage().getString().equals("확인")),
                            "Right click activated a volunteer action");
                    if (!recall.active) {
                        click(screen, recall, InputConstants.MOUSE_BUTTON_LEFT);
                        check(requests.isEmpty(), "Disabled source recall sent a request");
                    }
                });
                context.waitTicks(3);
                context.takeScreenshot("hoi-diplomacy-volunteer-actions-source-bottom-" + width + "-gui2");
                for (var action : entries) {
                    if (action.id().equals("open_logistics")) continue;
                    var point = context.computeOnClient(client -> {
                        var screen = (DiplomacyCountryScreen)client.gui.screen();
                        screen.mouseScrolled(screen.panelWidth() - 15, screen.height - 70, 0, 50);
                        for (int step = 0; step < names.size() && screen.children().stream().noneMatch(child -> child instanceof Button button && button.getMessage().getString().equals(action.name())); step++)
                            screen.mouseScrolled(screen.panelWidth() - 15, screen.height - 70, 0, -1);
                        var button = button(screen, action.name());
                        return new double[]{(button.getX() + button.getWidth() / 2.0) * client.getWindow().getGuiScale(),
                                (button.getY() + button.getHeight() / 2.0) * client.getWindow().getGuiScale()};
                    });
                    context.getInput().setCursorPos(point[0], point[1]);
                    context.waitTicks(15);
                    context.takeScreenshot("hoi-diplomacy-action-source-tooltip-" + action.id() + "-" + width + "-gui2");
                }
                for (String id : List.of("volunteers", "recall_volunteers")) exercise(context, source, id, width);
            }
        } finally {
            context.runOnClient(client -> client.options.guiScale().set(scale));
        }
    }
    private static List<String> visible(DiplomacyCountryScreen screen, List<String> names) {
        var rendered = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .map(button -> button.getMessage().getString()).filter(names::contains).toList();
        check(rendered.stream().distinct().count() == rendered.size(), "Duplicate action control on one page");
        if (!rendered.isEmpty()) {
            int first = names.indexOf(rendered.getFirst());
            check(names.subList(first, first + rendered.size()).equals(rendered), "Controls changed the source action order");
        }
        return rendered;
    }
    private static void exercise(ClientGameTestContext context, CountryView source, String id, int width) {
        var entries = source.actions().entries().stream().map(action -> action.id().equals(id)
                ? new CountryView.Action(action.id(), action.name(), action.detail(), true, action.amountInput(), action.costs(), action.declined()) : action).toList();
        var view = new CountryView(source.viewer(), source.hud(), source.target(), source.name(), source.shared(), source.countries(),
                source.diplomacy(), source.focuses(), source.reports(), new CountryView.Actions(source.actions().session(), source.actions().revision(), "", entries),
                source.relations(), source.opinion(), source.art());
        var requests = new ArrayList<CountryPayloads.Action>();
        context.setScreen(() -> new DiplomacyCountryScreen(view, requests::add));
        context.runOnClient(client -> {
            var screen = (DiplomacyCountryScreen)client.gui.screen();
            screen.mouseScrolled(screen.panelWidth() - 15, screen.height - 70, 0, -50);
            var action = entries.stream().filter(entry -> entry.id().equals(id)).findFirst().orElseThrow();
            var control = button(screen, action.name());
            click(screen, control, InputConstants.MOUSE_BUTTON_LEFT);
            if (id.equals("recall_volunteers")) {
                check(requests.isEmpty(), "Opening recall confirmation sent a mutation");
                click(screen, button(screen, "확인"), InputConstants.MOUSE_BUTTON_LEFT);
                click(screen, button(screen, "확인"), InputConstants.MOUSE_BUTTON_LEFT);
            } else click(screen, control, InputConstants.MOUSE_BUTTON_LEFT);
            check(requests.size() == 1 && requests.getFirst().id().equals(id)
                    && requests.getFirst().session().equals(source.actions().session()) && requests.getFirst().revision() == source.actions().revision(),
                    "Volunteer action dispatched the wrong ID or repeated the request");
        });
        context.waitTicks(3);
        context.takeScreenshot("hoi-diplomacy-volunteer-actions-synthetic-enabled-" + id + "-" + width + "-gui2");
    }
    private static Button button(DiplomacyCountryScreen screen, String title) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().equals(title)).findFirst().orElseThrow();
    }
    private static void click(DiplomacyCountryScreen screen, Button button, int mouseButton) {
        var event = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(mouseButton, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
