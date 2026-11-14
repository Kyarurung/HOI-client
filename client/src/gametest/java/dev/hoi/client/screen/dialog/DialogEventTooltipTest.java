package dev.hoi.client.screen.dialog;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.dialog.DialogPayloads;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.client.screen.decision.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class DialogEventTooltipTest {
    private static final Map<String, Object> measurements = new LinkedHashMap<>();
    private static final Set<String> missingIcons = new TreeSet<>();
    public static void run(ClientGameTestContext context) {
        measurements.clear(); missingIcons.clear();
        try {
            var directory = Path.of(Objects.requireNonNull(System.getenv("HOI_LEADER_EVENT_PREVIEW")));
            for (int[] size : new int[][]{{2560, 1440}, {1280, 720}}) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(c -> {
                    c.options.guiScale().set(2); DialogCoordinator.reset(); sourceText();
                    ClientCampaignHud.accept(HudPayloads.State.of("KOR", CountryHud.UNKNOWN));
                    CommanderHud.accept(new CommanderHudPayloads.View("KOR", c.level.dimension().identifier().toString(), CommanderHudPayloads.Mode.ARMY, List.of(), List.of(), false));
                });
                for (String name : List.of("event-korea-21", "event-korea-504", "event-china-98", "china-705", "china-182", "event-korea-356", "news-turkey-2")) {
                    var view = new DialogPayloads.Show(Files.readString(directory.resolve(name + ".json")), true).view();
                    var sent = new ArrayList<DialogPayloads.Action>();
                    context.setScreen(() -> { var stack = new DialogStack(null, (v, id) -> sent.add(new DialogPayloads.Action(v.token(), v.revision(), id))); stack.add(view); return stack; });
                    for (int i = 0; i < view.choices().size(); i++) {
                        final int option = i;
                        int[] cursor = context.computeOnClient(c -> {
                            var screen = ((DialogStack)c.gui.screen()).find(view.token());
                            var button = choice(screen, view.choices().get(option));
                            return new int[]{button.getX() * 2 + button.getWidth(), button.getY() * 2 + button.getHeight()};
                        });
                        context.getInput().setCursorPos(cursor[0], cursor[1]); context.waitTicks(4);
                        String key = name + "-choice-" + i + "-" + size[0];
                        context.takeScreenshot("hoi-event-tooltip-" + key + "-gui2");
                        context.runOnClient(c -> {
                            var screen = ((DialogStack)c.gui.screen()).find(view.token());
                            var tooltip = screen.tooltipBounds();
                            var optionView = view.choices().get(option);
                            if (optionView.tooltip().isEmpty() != (tooltip == null)) throw new AssertionError("Effect tooltip visibility mismatch: " + key + ", empty=" + optionView.tooltip().isEmpty() + ", bounds=" + tooltip + ", button=" + Arrays.toString(box(choice(screen, optionView))) + ", cursor=" + Arrays.toString(cursor));
                            if (tooltip != null) {
                                DecisionPanelTest.verifyTooltipSpacing(c.font,new DecisionText(),optionView.tooltip());
                                if (tooltip.x() < 4 || tooltip.y() < 4 || tooltip.x() + tooltip.width() + 4 > screen.width || tooltip.y() + tooltip.height() + 4 > screen.height) throw new AssertionError("Effect tooltip leaves viewport");
                                var matcher = java.util.regex.Pattern.compile("£([A-Za-z0-9_]+)").matcher(optionView.tooltip());
                                while (matcher.find()) {
                                    String token = matcher.group();
                                    if (dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal(token), false).getString().isEmpty()) missingIcons.add(token);
                                }
                                measurements.put(key, Map.of("tooltipGui", new int[]{tooltip.x() - 4, tooltip.y() - 4, tooltip.width() + 8, tooltip.height() + 8}, "text", optionView.tooltip(), "choiceGui", box(choice(screen, optionView))));
                            }
                            if (!sent.isEmpty()) throw new AssertionError("Hovering cannot choose or dismiss an event");
                        });
                    }
                }
                var source = new DialogPayloads.Show(Files.readString(directory.resolve("event-korea-356.json")), true).view();
                var second = new DialogPayloads.Show(Files.readString(directory.resolve("event-korea-504.json")), true).view();
                var sourceTooltip = source.choices().getFirst().tooltip();
                if (!sourceTooltip.startsWith("£GFX_KOR_PPP_texticon §A보수정당§!§W의§! £GFX_KOR_NationalAssembly_texticon") || !sourceTooltip.contains("£GFX_KOR_Independent_texticon §g무소속§!§W이 의석을§!") || sourceTooltip.contains("[KOR_NatAsm")) throw new AssertionError("Actual korea.356 fixture must contain resolved source text and icons");
                lifecycle(context, source, second, size[0], false);
                var news = new DialogPayloads.Show(Files.readString(directory.resolve("news-turkey-2.json")), true).view();
                if (news.kind() != DialogView.Kind.GLOBAL_EVENT || !news.choices().equals(List.of(new DialogView.Choice("ack", "에르도안 저 자식, 진심이야?", "")))) throw new AssertionError("Actual turkey.2 fixture must supply the authoritative acknowledgement");
                lifecycle(context, news, source, size[0], true);
                boundaries(context, source, size[0]);
                coordinator(context, source);
            }
            Files.writeString(Path.of("event-tooltip-measurements.json"), new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("renders", measurements, "missingIcons", missingIcons)));
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        finally { context.runOnClient(c -> { DialogCoordinator.reset(); CommanderHud.reset(); ClientCampaignHud.accept(HudPayloads.State.HIDDEN); }); context.setScreen(() -> null); context.getInput().resizeWindow(1600, 1000); context.waitTicks(3); }
    }
    private static Button choice(DialogScreen screen, DialogView.Choice choice) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals(choice.label())).findFirst().orElseThrow();
    }
    private static void sourceText() {
        var text = new DecisionText();
        for (String token : List.of("KOR_PPP_texticon", "KOR_NationalAssembly_texticon", "KOR_Independent_texticon", "KOR_DemocraticParty_texticon")) {
            var icon = text.component("£GFX_" + token).getString();
            if (icon.length() != 1 || icon.charAt(0) < 0xe000 || !icon.equals(text.component("£" + token).getString())) throw new AssertionError("Missing source icon or GFX alias: " + token);
        }
        if (!text.component("앞 £GFX_missing_event_test 뒤").getString().equals("앞  뒤")) throw new AssertionError("Unknown icon tokens must be hidden");
        var colors = DecisionText.sourceRuns("§A보수§!§g무소속§!§8의석§!§3파랑§!");
        if (!colors.equals(List.of(new DecisionText.Run("보수", 0xCD3737), new DecisionText.Run("무소속", 0xB0B0B0), new DecisionText.Run("의석", 0xCCA333), new DecisionText.Run("파랑", 0x518FDC)))) throw new AssertionError("TFR event colors changed");
        text.assets(com.google.gson.JsonParser.parseString("{\"textImages\":{\"GFX_test_banner\":{\"path\":\"decision/test_banner\",\"width\":80,\"height\":20}}}").getAsJsonObject());
        if (!text.rows("£GFX_test_banner").getFirst().getString().equals("£GFX_test_banner")) throw new AssertionError("Registered image blocks must survive token decoration");
    }
    private static int[] box(Button button) { return new int[]{button.getX(), button.getY(), button.getWidth(), button.getHeight()}; }
    private static MouseButtonEvent mouse(Button button, int key) { return new MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(key, 0)); }
    private static void lifecycle(ClientGameTestContext context, DialogView view, DialogView second, int width, boolean news) {
        var sent = new ArrayList<DialogPayloads.Action>();
        context.setScreen(() -> {
            var panel = new DecisionScreen(null, request -> { throw new AssertionError("Reopening is local"); });
            var stack = new DialogStack(panel, (v, id) -> sent.add(new DialogPayloads.Action(v.token(), v.revision(), id)));
            panel.eventStack(stack); stack.add(view); return stack;
        });
        context.getInput().setCursorPos(20, 20); context.waitTicks(3);
        String prefix = "hoi-event-minimize-" + (news ? "news-turkey-2" : "national-korea-356") + "-" + width;
        context.takeScreenshot(prefix + "-before-gui2");
        context.runOnClient(c -> {
            var stack = (DialogStack)c.gui.screen();
            var window = stack.find(view.token());
            var minimize = window.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals("최소화")).findFirst().orElseThrow();
            var point = mouse(minimize, InputConstants.MOUSE_BUTTON_LEFT);
            if (stack.inputAt(point.x(), point.y()) != window) {
                if (stack.inputAt(point.x(), point.y()) != stack.backdrop()) throw new AssertionError("Unexpected layer above the event minimize control");
                var panel = (DecisionScreen)stack.backdrop();
                var eventRow = panel.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(button -> button.getMessage().getString().equals(view.title())).findFirst().orElseThrow();
                stack.mouseClicked(mouse(eventRow, InputConstants.MOUSE_BUTTON_LEFT), false); stack.mouseReleased(mouse(eventRow, InputConstants.MOUSE_BUTTON_LEFT));
                if (stack.inputAt(point.x(), point.y()) != window || !sent.isEmpty()) throw new AssertionError("The event row must expose its minimize control without a request");
            }
            stack.mouseClicked(mouse(minimize, InputConstants.MOUSE_BUTTON_RIGHT), false);
            stack.mouseDragged(mouse(minimize, InputConstants.MOUSE_BUTTON_LEFT), 10, 10);
            if (!stack.hasVisibleWindows()) throw new AssertionError("Right click or dragging minimized an event");
            stack.mouseClicked(mouse(minimize, InputConstants.MOUSE_BUTTON_LEFT), false); stack.mouseReleased(mouse(minimize, InputConstants.MOUSE_BUTTON_LEFT));
            if (stack.hasVisibleWindows() || stack.events().size() != 1 || !sent.isEmpty()) throw new AssertionError("Minimize must retain the session without a request");
        });
        context.waitTicks(3); context.takeScreenshot(prefix + "-hidden-gui2");
        context.runOnClient(c -> {
            var stack = (DialogStack)c.gui.screen();
            var panel = (DecisionScreen)stack.backdrop();
            if (panel.eventCount() != 1) throw new AssertionError("Pending event row missing");
            var row = panel.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals(view.title())).findFirst().orElseThrow();
            measurements.put(prefix, Map.of("eventRowGui", box(row), "kind", view.kind().name()));
            stack.mouseClicked(mouse(row, InputConstants.MOUSE_BUTTON_LEFT), false); stack.mouseReleased(mouse(row, InputConstants.MOUSE_BUTTON_LEFT));
            if (!stack.hasVisibleWindows() || stack.layers().getLast() != stack.find(view.token())) throw new AssertionError("Event row must bring its window to the front");
            stack.reopen(view.token());
            if (stack.events().size() != 1) throw new AssertionError("Repeated reopen duplicated a session");
        });
        context.waitTicks(3); context.takeScreenshot(prefix + "-reopened-gui2");
        context.runOnClient(c -> {
            var stack = (DialogStack)c.gui.screen();
            stack.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            if (stack.hasVisibleWindows() || !sent.isEmpty()) throw new AssertionError("Esc must retain the event with no request");
            stack.add(second.issued(UUID.randomUUID().toString(), second.revision()));
            if (!stack.hasVisibleWindows()) throw new AssertionError("New events must still popup");
            stack.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            if (stack.events().size() != 2 || stack.hasVisibleWindows()) throw new AssertionError("Two minimized sessions must be retained");
        });
        context.waitTicks(3); context.takeScreenshot(prefix + "-two-hidden-gui2");
        context.runOnClient(c -> {
            var stack = (DialogStack)c.gui.screen();
            stack.reopen(view.token());
            var screen = stack.find(view.token());
            var button = choice(screen, view.choices().getFirst());
            stack.mouseClicked(mouse(button, InputConstants.MOUSE_BUTTON_RIGHT), false);
            if (!sent.isEmpty()) throw new AssertionError("Right click chose an option");
            stack.mouseClicked(mouse(button, InputConstants.MOUSE_BUTTON_LEFT), false);
            stack.mouseClicked(mouse(button, InputConstants.MOUSE_BUTTON_LEFT), true);
            if (sent.size() != 1 || !sent.getFirst().equals(new DialogPayloads.Action(view.token(), view.revision(), view.choices().getFirst().id()))) throw new AssertionError("Choice must send exactly one issued request");
            screen.update(view);
            if (choice(screen, view.choices().getFirst()).active) throw new AssertionError("Same revision unlocked pending choice");
            screen.update(view.issued(view.token(), view.revision() - 1));
            if (choice(screen, view.choices().getFirst()).active) throw new AssertionError("Stale revision unlocked pending choice");
        });
        context.waitTicks(3); context.takeScreenshot(prefix + "-pending-gui2");
        context.runOnClient(c -> {
            var stack = (DialogStack)c.gui.screen();
            var screen = stack.find(view.token());
            var retry = view.issued(view.token(), view.revision() + 1);
            if (!news) c.gui.hud.setOverlayMessage(net.minecraft.network.chat.Component.literal("Script array index outside bounds"), false);
            screen.update(retry);
            var button = choice(screen, retry.choices().getFirst());
            if (!button.active) throw new AssertionError("Authoritative retry revision must unlock the choice");
        });
        int[] retryCursor = context.computeOnClient(c -> { var button = choice(((DialogStack)c.gui.screen()).find(view.token()), view.choices().getFirst()); return new int[]{button.getX() * 2 + button.getWidth(), button.getY() * 2 + button.getHeight()}; });
        context.getInput().setCursorPos(retryCursor[0], retryCursor[1]);
        context.waitTicks(3); context.takeScreenshot(prefix + "-retry-ready-gui2");
        context.runOnClient(c -> {
            var stack = (DialogStack)c.gui.screen();
            var retry = stack.find(view.token()).view();
            var button = choice(stack.find(view.token()), retry.choices().getFirst());
            stack.mouseClicked(mouse(button, InputConstants.MOUSE_BUTTON_LEFT), false);
            stack.mouseClicked(mouse(button, InputConstants.MOUSE_BUTTON_LEFT), true);
            if (sent.size() != 2 || !sent.getLast().equals(new DialogPayloads.Action(retry.token(), retry.revision(), retry.choices().getFirst().id()))) throw new AssertionError("Retry must send exactly one new revision request");
            stack.close(view.token());
            c.gui.hud.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
            if (stack.events().size() != 1 || stack.find(view.token()) != null) throw new AssertionError("Acknowledgement must remove only the chosen event");
        });
        context.waitTicks(3); context.takeScreenshot(prefix + "-chosen-removed-gui2");
        context.runOnClient(c -> {
            var stack = (DialogStack)c.gui.screen();
            if (((DecisionScreen)stack.backdrop()).eventCount() != 1) throw new AssertionError("Chosen event remained in the decisions tab");
            for (var pending : stack.events()) stack.close(pending.token());
            stack.reopen(view.token());
            if (!stack.events().isEmpty() || stack.hasVisibleWindows()) throw new AssertionError("Closed sessions cannot reopen");
        });
        context.waitTicks(3); context.takeScreenshot(prefix + "-server-closed-gui2");
        context.runOnClient(c -> { if (((DecisionScreen)((DialogStack)c.gui.screen()).backdrop()).eventCount() != 0) throw new AssertionError("Closed rows remained visible"); });
    }
    private static void boundaries(ClientGameTestContext context, DialogView source, int width) {
        var choice = new DialogView.Choice("long", "화면 경계 검사", ("§R첫 줄§!\n  £pol_power §G+60§!\n").repeat(16));
        var view = new DialogView(UUID.randomUUID().toString(), 1, source.kind(), source.title(), "", source.body(), source.flag(), source.image(), "", source.tiles(), List.of(choice));
        context.setScreen(() -> new DialogScreen(view));
        int[] cursor = context.computeOnClient(c -> { var b = choice((DialogScreen)c.gui.screen(), choice); return new int[]{b.getX() * 2 + b.getWidth(), b.getY() * 2 + b.getHeight()}; });
        context.getInput().setCursorPos(cursor[0], cursor[1]); context.waitTicks(3);
        context.runOnClient(c -> {
            var screen = (DialogScreen)c.gui.screen(); var tip = screen.tooltipBounds();
            if (tip == null || tip.x() < 4 || tip.y() < 4 || tip.x() + tip.width() + 4 > screen.width || tip.y() + tip.height() + 4 > screen.height) throw new AssertionError("Long tooltip must fit in the viewport");
            if (tip.maxScroll() > 0 && !screen.mouseScrolled(cursor[0] / 2.0, cursor[1] / 2.0, 0, -100)) throw new AssertionError("Long effect tooltip must scroll");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-event-tooltip-long-synthetic-" + width + "-gui2");
    }
    private static void coordinator(ClientGameTestContext context, DialogView source) {
        context.setScreen(() -> new DecisionScreen(null, request -> {}));
        context.runOnClient(c -> {
            var view = source.issued(UUID.randomUUID().toString(), 1);
            DialogCoordinator.receive(DialogPayloads.Show.of(view, true));
            var stack = DialogCoordinator.eventStack();
            var button = choice(stack.find(view.token()), view.choices().getFirst());
            stack.mouseClicked(mouse(button, InputConstants.MOUSE_BUTTON_LEFT), false);
            DialogCoordinator.receive(DialogPayloads.Show.of(view, false));
            if (choice(stack.find(view.token()), view.choices().getFirst()).active) throw new AssertionError("Duplicate Show must keep the pending lock");
            DialogCoordinator.receive(DialogPayloads.Show.of(view.issued(view.token(), 2), false));
            if (!choice(stack.find(view.token()), view.choices().getFirst()).active) throw new AssertionError("Retry Show must unlock the pending choice through the receiver");
            stack.reopen(view.token()); stack.find(view.token()).onClose();
            if (c.gui.screen() instanceof DialogStack || stack.events().size() != 1) throw new AssertionError("All hidden windows must release the overlay while retaining sessions");
            DialogCoordinator.receive(DialogPayloads.Show.of(view.issued(view.token(), 2), false));
            if (c.gui.screen() instanceof DialogStack || stack.find(view.token()).view().revision() != 2) throw new AssertionError("Refresh cannot unminimize an event");
            stack.reopen(view.token());
            if (c.gui.screen() != stack) throw new AssertionError("Reopening must restore the coordinator overlay");
            DialogCoordinator.close(view.token());
            if (DialogCoordinator.eventStack() != null || c.gui.screen() instanceof DialogStack) throw new AssertionError("Server close must remove the session and overlay");
            stack.reopen(view.token());
            if (c.gui.screen() instanceof DialogStack) throw new AssertionError("An expired session cannot reopen");
            DialogCoordinator.receive(DialogPayloads.Show.of(view.issued(UUID.randomUUID().toString(), 3), true));
            DialogCoordinator.reset();
            if (DialogCoordinator.eventStack() != null || c.gui.screen() != null) throw new AssertionError("Country/connection reset must clear pending events");
        });
    }
}
