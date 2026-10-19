package dev.hoi.client.screen;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.ProtocolDialogPayloads;
import dev.hoi.protocol.ProtocolDialogView;
import dev.hoi.client.ui.UiMenuButton;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ScreenEventTooltipTest {
    private static final Map<String, Object> measurements = new LinkedHashMap<>();
    private static final Set<String> missingIcons = new TreeSet<>();
    public static void run(ClientGameTestContext context) {
        measurements.clear(); missingIcons.clear();
        try {
            var directory = Path.of(Objects.requireNonNull(System.getenv("HOI_LEADER_EVENT_PREVIEW")));
            for (int[] size : new int[][]{{2560, 1440}, {1280, 720}}) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(c -> { c.options.guiScale().set(2); ScreenDialogCoordinator.reset(); });
                for (String name : List.of("event-korea-21", "event-korea-504", "event-china-98", "china-705", "china-182")) {
                    var view = new ProtocolDialogPayloads.Show(Files.readString(directory.resolve(name + ".json")), true).view();
                    var sent = new ArrayList<ProtocolDialogPayloads.Action>();
                    context.setScreen(() -> { var stack = new ScreenDialogStack(null, (v, id) -> sent.add(new ProtocolDialogPayloads.Action(v.token(), v.revision(), id))); stack.add(view); return stack; });
                    for (int i = 0; i < view.choices().size(); i++) {
                        final int option = i;
                        int[] cursor = context.computeOnClient(c -> {
                            var screen = ((ScreenDialogStack)c.gui.screen()).find(view.token());
                            var button = choice(screen, view.choices().get(option));
                            return new int[]{button.getX() * 2 + button.getWidth(), button.getY() * 2 + button.getHeight()};
                        });
                        context.getInput().setCursorPos(cursor[0], cursor[1]); context.waitTicks(4);
                        String key = name + "-choice-" + i + "-" + size[0];
                        context.runOnClient(c -> {
                            var screen = ((ScreenDialogStack)c.gui.screen()).find(view.token());
                            var tooltip = screen.tooltipBounds();
                            var optionView = view.choices().get(option);
                            if (optionView.tooltip().isEmpty() != (tooltip == null)) throw new AssertionError("Empty effects must have no tooltip");
                            if (tooltip != null) {
                                if (tooltip.x() < 4 || tooltip.y() < 4 || tooltip.x() + tooltip.width() + 4 > screen.width || tooltip.y() + tooltip.height() + 4 > screen.height) throw new AssertionError("Effect tooltip leaves viewport");
                                var matcher = java.util.regex.Pattern.compile("£([A-Za-z0-9_]+)").matcher(optionView.tooltip());
                                while (matcher.find()) {
                                    String token = matcher.group();
                                    if (new ScreenDecisionText().component(token).getString().contains("£")) missingIcons.add(token);
                                }
                                measurements.put(key, Map.of("tooltipGui", new int[]{tooltip.x() - 4, tooltip.y() - 4, tooltip.width() + 8, tooltip.height() + 8}, "text", optionView.tooltip(), "choiceGui", box(choice(screen, optionView))));
                            }
                            if (!sent.isEmpty()) throw new AssertionError("Hovering cannot choose or dismiss an event");
                        });
                        context.takeScreenshot("hoi-event-tooltip-" + key + "-gui2");
                    }
                }
                var source = new ProtocolDialogPayloads.Show(Files.readString(directory.resolve("event-korea-21.json")), true).view();
                var second = new ProtocolDialogPayloads.Show(Files.readString(directory.resolve("event-korea-504.json")), true).view();
                lifecycle(context, source, second, size[0], false);
                var catalog = new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(Objects.requireNonNull(ScreenEventFrameTest.class.getResourceAsStream("/event-frame-fixtures.json")), java.nio.charset.StandardCharsets.UTF_8), ScreenEventFrameTest.Fixtures.class);
                var news = catalog.cases().stream().filter(example -> example.name().equals("news-coronavirus")).findFirst().orElseThrow().view().issued(UUID.randomUUID().toString(), 1);
                lifecycle(context, news, source, size[0], true);
                boundaries(context, source, size[0]);
                coordinator(context, source);
            }
            Files.writeString(Path.of("event-tooltip-measurements.json"), new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("renders", measurements, "missingIcons", missingIcons)));
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        finally { context.runOnClient(c -> ScreenDialogCoordinator.reset()); context.setScreen(() -> null); context.getInput().resizeWindow(1600, 1000); context.waitTicks(3); }
    }
    private static Button choice(ScreenDialog screen, ProtocolDialogView.Choice choice) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals(choice.label())).findFirst().orElseThrow();
    }
    private static int[] box(Button button) { return new int[]{button.getX(), button.getY(), button.getWidth(), button.getHeight()}; }
    private static MouseButtonEvent mouse(Button button, int key) { return new MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(key, 0)); }
    private static void lifecycle(ClientGameTestContext context, ProtocolDialogView view, ProtocolDialogView second, int width, boolean news) {
        var sent = new ArrayList<ProtocolDialogPayloads.Action>();
        context.setScreen(() -> {
            var panel = new ScreenDecision(null, request -> { throw new AssertionError("Reopening is local"); });
            var stack = new ScreenDialogStack(panel, (v, id) -> sent.add(new ProtocolDialogPayloads.Action(v.token(), v.revision(), id)));
            panel.eventStack(stack); stack.add(view); return stack;
        });
        context.getInput().setCursorPos(20, 20); context.waitTicks(3);
        String prefix = "hoi-event-minimize-" + (news ? "news-synthetic" : "national") + "-" + width;
        context.takeScreenshot(prefix + "-before-gui2");
        context.runOnClient(c -> {
            var stack = (ScreenDialogStack)c.gui.screen();
            var minimize = stack.find(view.token()).children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals("최소화")).findFirst().orElseThrow();
            stack.mouseClicked(mouse(minimize, InputConstants.MOUSE_BUTTON_RIGHT), false);
            stack.mouseDragged(mouse(minimize, InputConstants.MOUSE_BUTTON_LEFT), 10, 10);
            if (!stack.hasVisibleWindows()) throw new AssertionError("Right click or dragging minimized an event");
            stack.mouseClicked(mouse(minimize, InputConstants.MOUSE_BUTTON_LEFT), false); stack.mouseReleased(mouse(minimize, InputConstants.MOUSE_BUTTON_LEFT));
            if (stack.hasVisibleWindows() || stack.events().size() != 1 || !sent.isEmpty()) throw new AssertionError("Minimize must retain the session without a request");
        });
        context.waitTicks(3); context.takeScreenshot(prefix + "-hidden-gui2");
        context.runOnClient(c -> {
            var stack = (ScreenDialogStack)c.gui.screen();
            var panel = (ScreenDecision)stack.backdrop();
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
            var stack = (ScreenDialogStack)c.gui.screen();
            stack.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            if (stack.hasVisibleWindows() || !sent.isEmpty()) throw new AssertionError("Esc must retain the event with no request");
            stack.add(second.issued(UUID.randomUUID().toString(), second.revision()));
            if (!stack.hasVisibleWindows()) throw new AssertionError("New events must still popup");
            stack.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            if (stack.events().size() != 2 || stack.hasVisibleWindows()) throw new AssertionError("Two minimized sessions must be retained");
        });
        context.waitTicks(3); context.takeScreenshot(prefix + "-two-hidden-gui2");
        context.runOnClient(c -> {
            var stack = (ScreenDialogStack)c.gui.screen();
            stack.reopen(view.token());
            var screen = stack.find(view.token());
            var button = choice(screen, view.choices().getFirst());
            stack.mouseClicked(mouse(button, InputConstants.MOUSE_BUTTON_RIGHT), false);
            if (!sent.isEmpty()) throw new AssertionError("Right click chose an option");
            stack.mouseClicked(mouse(button, InputConstants.MOUSE_BUTTON_LEFT), false);
            stack.mouseClicked(mouse(button, InputConstants.MOUSE_BUTTON_LEFT), true);
            if (sent.size() != 1 || !sent.getFirst().equals(new ProtocolDialogPayloads.Action(view.token(), view.revision(), view.choices().getFirst().id()))) throw new AssertionError("Choice must send exactly one issued request");
            screen.update(view);
            if (choice(screen, view.choices().getFirst()).active) throw new AssertionError("Same revision unlocked pending choice");
            for (var pending : stack.events()) stack.close(pending.token());
            stack.reopen(view.token());
            if (!stack.events().isEmpty() || stack.hasVisibleWindows()) throw new AssertionError("Closed sessions cannot reopen");
        });
        context.waitTicks(3); context.takeScreenshot(prefix + "-server-closed-gui2");
        context.runOnClient(c -> { if (((ScreenDecision)((ScreenDialogStack)c.gui.screen()).backdrop()).eventCount() != 0) throw new AssertionError("Closed rows remained visible"); });
    }
    private static void boundaries(ClientGameTestContext context, ProtocolDialogView source, int width) {
        var choice = new ProtocolDialogView.Choice("long", "화면 경계 검사", ("§R첫 줄§!\n  £pol_power §G+60§!\n").repeat(16));
        var view = new ProtocolDialogView(UUID.randomUUID().toString(), 1, source.kind(), source.title(), "", source.body(), source.flag(), source.image(), "", source.tiles(), List.of(choice));
        context.setScreen(() -> new ScreenDialog(view));
        int[] cursor = context.computeOnClient(c -> { var b = choice((ScreenDialog)c.gui.screen(), choice); return new int[]{b.getX() * 2 + b.getWidth(), b.getY() * 2 + b.getHeight()}; });
        context.getInput().setCursorPos(cursor[0], cursor[1]); context.waitTicks(3);
        context.runOnClient(c -> {
            var screen = (ScreenDialog)c.gui.screen(); var tip = screen.tooltipBounds();
            if (tip == null || tip.x() < 4 || tip.y() < 4 || tip.x() + tip.width() + 4 > screen.width || tip.y() + tip.height() + 4 > screen.height) throw new AssertionError("Long tooltip must fit in the viewport");
            if (tip.maxScroll() > 0 && !screen.mouseScrolled(cursor[0] / 2.0, cursor[1] / 2.0, 0, -100)) throw new AssertionError("Long effect tooltip must scroll");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-event-tooltip-long-synthetic-" + width + "-gui2");
    }
    private static void coordinator(ClientGameTestContext context, ProtocolDialogView source) {
        context.setScreen(() -> new ScreenDecision(null, request -> {}));
        context.runOnClient(c -> {
            var view = source.issued(UUID.randomUUID().toString(), 1);
            ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(view, true));
            var stack = ScreenDialogCoordinator.eventStack();
            stack.reopen(view.token()); stack.find(view.token()).onClose();
            if (c.gui.screen() instanceof ScreenDialogStack || stack.events().size() != 1) throw new AssertionError("All hidden windows must release the overlay while retaining sessions");
            ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(view.issued(view.token(), 2), false));
            if (c.gui.screen() instanceof ScreenDialogStack || stack.find(view.token()).view().revision() != 2) throw new AssertionError("Refresh cannot unminimize an event");
            stack.reopen(view.token());
            if (c.gui.screen() != stack) throw new AssertionError("Reopening must restore the coordinator overlay");
            ScreenDialogCoordinator.close(view.token());
            if (ScreenDialogCoordinator.eventStack() != null || c.gui.screen() instanceof ScreenDialogStack) throw new AssertionError("Server close must remove the session and overlay");
            stack.reopen(view.token());
            if (c.gui.screen() instanceof ScreenDialogStack) throw new AssertionError("An expired session cannot reopen");
            ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(view.issued(UUID.randomUUID().toString(), 3), true));
            ScreenDialogCoordinator.reset();
            if (ScreenDialogCoordinator.eventStack() != null || c.gui.screen() != null) throw new AssertionError("Country/connection reset must clear pending events");
        });
    }
}
