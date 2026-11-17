package dev.hoi.client.screen.dialog;

import com.google.gson.Gson;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.protocol.dialog.DialogPayloads;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public final class DialogLeaderEventTest {
    record Fixtures(List<DialogView> views) {}
    public static void run(ClientGameTestContext context) {
        regression(context);
        String directory = System.getenv("HOI_LEADER_EVENT_PREVIEW");
        if (directory == null) throw new AssertionError("Authoritative leader event fixtures are required");
        var measurements = new LinkedHashMap<String, Object>();
        try {
            for (int[] size : new int[][]{{2560, 1440}, {1280, 720}}) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(c -> c.options.guiScale().set(2));
                for (String name : List.of("china-705", "china-182", "china-182-long-date")) {
                    var source = new DialogPayloads.Show(Files.readString(Path.of(directory, name.equals("china-182-long-date") ? "china-182.json" : name + ".json")), true).view();
                    if (name.startsWith("china-182") && !source.body().split("\n", -1)[1].equals("날짜: 2020년 1월 1일 00시")) throw new AssertionError("Authoritative event date must include Korean hours without minutes");
                    var body = source.body().split("\n", -1);
                    if (name.equals("china-182-long-date")) body[1] = "날짜: 2022년 12월 31일 23시";
                    var view = name.equals("china-182-long-date") ? new DialogView(source.token(), source.revision(), source.kind(), source.title(), source.subtitle(), String.join("\n", body), source.flag(), source.image(), source.sound(), source.tiles(), source.choices(), source.presentation()) : source;
                    if (!view.leaderEvent() || view.completion() || view.kind() != DialogView.Kind.EVENT) throw new AssertionError("Wrong server presentation");
                    var sent = new ArrayList<DialogPayloads.Action>();
                    context.setScreen(() -> {
                        var stack = new DialogStack(null, (v, choice) -> sent.add(new DialogPayloads.Action(v.token(), v.revision(), choice)));
                        stack.add(view);
                        return stack;
                    });
                    context.getInput().setCursorPos(20, size[1] - 20);
                    context.waitTicks(4);
                    int[] panel = context.computeOnClient(c -> {
                        var stack = (DialogStack)c.gui.screen();
                        if (!(stack.find(view.token()) instanceof DialogLeaderEvent screen)) throw new AssertionError("Coordinator stack must route leader events to the original report renderer");
                        if (c.getWindow().getGuiScale() != 2) throw new AssertionError("Actual GUI scale must be 2");
                        if (!screen.portraitPath().equals(view.image())) throw new AssertionError("Issued commander portrait is missing");
                        for (String sprite : List.of("top", "middle", "options", "end", "frame", "choice", "minimize"))
                            if (dev.hoi.client.ui.UiAssets.imageHeight("events/leader/" + sprite, 605) == 0) throw new AssertionError("Missing original sprite: " + sprite);
                        if (screen.panelLeft() < 0 || screen.panelLeft() + screen.panelWidth() > screen.width || screen.panelTop() < 0 || screen.panelTop() + screen.panelHeight() > screen.height)
                            throw new AssertionError("Original report must remain in the viewport");
                        if (size[0] == 2560 && (screen.panelLeft() != 499 || screen.panelTop() != (screen.height - screen.panelHeight()) / 2)) throw new AssertionError("The report keeps its original CENTER x offset and is vertically centred");
                        if (screen.textScale() != dev.hoi.client.ui.UiText.scale(c.font)) throw new AssertionError("Existing event font size must remain unchanged");
                        var buttons = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> view.choices().stream().anyMatch(choice -> choice.label().equals(b.getMessage().getString()))).toList();
                        if (buttons.size() != view.choices().size() || buttons.stream().map(Button::getX).distinct().count() != 1) throw new AssertionError("Original choices use one column");
                        for (var button : buttons) if (button.getY() < 0 || button.getBottom() > screen.height || button.getRight() > screen.width) throw new AssertionError("Choice outside the screen");
                        assertChoiceLabels(c.font, screen, view);
                        int textWidth = Math.max(1, (int)(screen.bounds(198, 121, 350, 1)[2] / screen.textScale()));
                        int bodyHeight = c.font.split(net.minecraft.network.chat.Component.literal(view.body()), textWidth).size() * 12;
                        if (bodyHeight > screen.bodyViewportHeight()) throw new AssertionError("Source and longest-date report bodies must fit above the choices");
                        int dateLines = name.startsWith("china-182") ? c.font.split(net.minecraft.network.chat.Component.literal(body[1]), textWidth).size() : 0;
                        if (name.startsWith("china-182") && dateLines != 1) throw new AssertionError("Event dates without minutes must occupy one line at both GUI2 resolutions");
                        int[] titleBounds = screen.bounds(14, 70, 551, 32);
                        float titleScale = screen.eventTitleScale(titleBounds[2]);
                        int titleHeight = Math.max(titleBounds[3], (int)Math.ceil(c.font.lineHeight * titleScale));
                        if (titleBounds[1] + titleHeight > screen.bounds(198, 121, 350, 1)[1]) throw new AssertionError("Enlarged title must end above the body and portrait");
                        measurements.put(name + "-" + size[0] + "-title", java.util.Map.of("scale", titleScale, "bold", true, "boxHeightGui", titleHeight, "fontHeightGui", c.font.lineHeight * titleScale));
                        measurements.put(name + "-" + size[0], java.util.Map.of("guiScale", 2, "panelGui", new int[]{screen.panelLeft(), screen.panelTop(), screen.panelWidth(), screen.panelHeight()}, "titleGui", screen.bounds(14, 70, 551, 32), "portraitGui", screen.bounds(43, 125, 156 * .85, 210 * .85), "frameGui", screen.bounds(35, 116, 172 * .85, 232 * .85), "bodyViewportGui", screen.bodyViewportHeight(), "choicesGui", buttons.stream().map(b -> new int[]{b.getX(), b.getY(), b.getWidth(), b.getHeight()}).toList(), "fontGuiHeight", c.font.lineHeight * screen.textScale(), "bodyGuiHeight", bodyHeight, "dateLines", dateLines));
                        return new int[]{screen.panelLeft() * 2, screen.panelTop() * 2, screen.panelWidth() * 2 + 2, screen.panelHeight() * 2};
                    });
                    var full = context.takeScreenshot("hoi-leader-event-source-" + name + "-" + size[0] + "-gui2");
                    var image = javax.imageio.ImageIO.read(full.toFile());
                    javax.imageio.ImageIO.write(image.getSubimage(panel[0], panel[1], panel[2], panel[3]), "png", full.resolveSibling(full.getFileName().toString().replace(".png", "-window.png")).toFile());
                    if (name.equals("china-705") && size[0] == 1280) {
                        var ordinary = new DialogView(view.token(), view.revision(), view.kind(), view.title(), view.subtitle(), view.body(), view.flag(), view.image(), view.sound(), view.tiles(), view.choices());
                        context.setScreen(() -> new DialogScreen(ordinary));
                        context.runOnClient(c -> assertChoiceLabels(c.font, (DialogScreen)c.gui.screen(), ordinary));
                        context.waitTicks(4);
                        var ordinaryFull = context.takeScreenshot("hoi-leader-event-ordinary-long-choices-1280-gui2");
                        int[] bounds = context.computeOnClient(c -> { var screen = (DialogScreen)c.gui.screen(); return new int[]{screen.panelLeft() * 2, screen.panelTop() * 2, screen.panelWidth() * 2, screen.panelHeight() * 2}; });
                        var ordinaryImage = javax.imageio.ImageIO.read(ordinaryFull.toFile());
                        javax.imageio.ImageIO.write(ordinaryImage.getSubimage(bounds[0], bounds[1], bounds[2], bounds[3]), "png", ordinaryFull.resolveSibling(ordinaryFull.getFileName().toString().replace(".png", "-window.png")).toFile());
                        context.setScreen(() -> { var stack = new DialogStack(null, (v, choice) -> sent.add(new DialogPayloads.Action(v.token(), v.revision(), choice))); stack.add(view); return stack; });
                    }
                    context.runOnClient(c -> {
                        var stack = (DialogStack)c.gui.screen();
                        var screen = (DialogLeaderEvent)stack.find(view.token());
                        for (int i = 0; i < view.choices().size(); i++) {
                            var current = view.issued(view.token(), view.revision() + i);
                            screen.update(current);
                            var choice = current.choices().get(i);
                            var button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals(choice.label())).findFirst().orElseThrow();
                            double x = button.getX() + button.getWidth() / 2.0, y = button.getY() + button.getHeight() / 2.0;
                            var left = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                            var right = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0));
                            int before = sent.size();
                            stack.mouseClicked(right, false);
                            stack.mouseReleased(right);
                            stack.mouseDragged(left, 5, 5);
                            if (sent.size() != before) throw new AssertionError("Right-click and dragging must not choose an event option");
                            stack.mouseClicked(left, false);
                            stack.mouseReleased(left);
                            stack.mouseClicked(left, true);
                            if (sent.size() != before + 1 || !sent.getLast().equals(new DialogPayloads.Action(current.token(), current.revision(), choice.id()))) throw new AssertionError("Exactly the issued event token, revision and choice ID must be sent once");
                            screen.update(current);
                            if (screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b -> b.active && current.choices().stream().anyMatch(option -> option.label().equals(b.getMessage().getString())))) throw new AssertionError("Equal revision cannot unlock pending choices");
                            screen.update(view);
                            if (screen.view().revision() != current.revision()) throw new AssertionError("Old revisions cannot replace the active event");
                        }
                        var fresh = view.issued(view.token(), view.revision() + 10);
                        screen.update(fresh);
                        screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_NUMPADENTER, 0, 0));
                        if (view.choices().size() == 1 && !sent.getLast().equals(new DialogPayloads.Action(fresh.token(), fresh.revision(), fresh.choices().getFirst().id()))) throw new AssertionError("Single-choice Enter uses the issued request");
                        var legacy = new DialogView(java.util.UUID.randomUUID().toString(), 0, view.kind(), view.title(), view.subtitle(), view.body(), view.flag(), view.image(), view.sound(), view.tiles(), view.choices());
                        stack.add(legacy);
                        if (stack.find(legacy.token()).getClass() != DialogScreen.class) throw new AssertionError("Legacy events keep their existing renderer");
                    });
                }
            }
            var view = new DialogPayloads.Show(Files.readString(Path.of(directory, "china-182.json")), true).view();
            dragging(context, view);
            titleComparisons(context, view);
            var missing = new DialogView(view.token(), view.revision(), view.kind(), view.title(), view.subtitle(), view.body(), view.flag(), "commander/tfr/missing/army", view.sound(), view.tiles(), view.choices(), view.presentation());
            context.setScreen(() -> new DialogLeaderEvent(missing, (v, choice) -> {}));
            context.runOnClient(c -> { if (!((DialogLeaderEvent)c.gui.screen()).portraitPath().equals("politics/empty/leader")) throw new AssertionError("Missing portrait uses the original GFX_leader_unknown"); });
            context.waitTicks(4);
            context.takeScreenshot("hoi-leader-event-synthetic-missing-portrait-gui2");
            context.runOnClient(c -> {
                var choices = java.util.stream.IntStream.range(0, 24).mapToObj(i -> new DialogView.Choice("issued-" + i, "선택 " + i)).toList();
                var longView = new DialogView(view.token(), 20, view.kind(), view.title(), view.subtitle(), (view.body() + "\n").repeat(20), view.flag(), view.image(), view.sound(), view.tiles(), choices, view.presentation());
                var screen = new DialogLeaderEvent(longView, (v, choice) -> {});
                c.gui.setScreen(screen);
                var reached = new java.util.HashSet<String>();
                for (int page = 0; page < 24; page++) {
                    for (var child : screen.children()) if (child instanceof Button button) {
                        if (button.getX() < 0 || button.getY() < 0 || button.getRight() > screen.width || button.getBottom() > screen.height) throw new AssertionError("Paged report choices must stay within the viewport");
                        if (button.getMessage().getString().startsWith("선택 ")) reached.add(button.getMessage().getString());
                    }
                    var next = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals("다음 선택지")).findFirst().orElseThrow();
                    var previous = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals("이전 선택지")).findFirst().orElseThrow();
                    if (previous.getRight() > next.getX()) throw new AssertionError("Previous and next choice controls must not overlap");
                    if (!next.active) break;
                    next.onPress(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                }
                if (reached.size() != 24) throw new AssertionError("Every bounded server choice must remain reachable");
                int[] body = screen.bounds(198, 121, 350, 1);
                screen.mouseScrolled(body[0] + 1, body[1] + 1, 0, -100);
                if (screen.scrollOffset() <= 0) throw new AssertionError("Long reports must scroll without covering the portrait or choices");
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_HOME, 0, 0));
                if (screen.scrollOffset() != 0) throw new AssertionError("Home must reach the report beginning");
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_END, 0, 0));
                int end = screen.scrollOffset();
                if (end <= 0) throw new AssertionError("End must reach the report ending");
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_PAGEUP, 0, 0));
                if (screen.scrollOffset() != Math.max(0, end - screen.bodyViewportHeight())) throw new AssertionError("PageUp must scroll the report viewport");
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_PAGEDOWN, 0, 0));
                if (screen.scrollOffset() != end) throw new AssertionError("PageDown must restore the report ending");
            });
            context.waitTicks(4);
            context.takeScreenshot("hoi-leader-event-synthetic-paged-long-report-gui2");
            Path output = Path.of("leader-event-measurements.json");
            Files.writeString(output, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(measurements), StandardCharsets.UTF_8);
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        finally { context.setScreen(() -> null); context.getInput().resizeWindow(1600, 1000); context.waitTicks(3); }
    }
    private static void dragging(ClientGameTestContext context, DialogView view) {
        for (int[] size : new int[][]{{2560, 1440, 2}, {1280, 720, 2}, {1920, 1080, 3}}) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.runOnClient(c -> c.options.guiScale().set(size[2]));
            var sent = new ArrayList<String>();
            context.setScreen(() -> { var stack = new DialogStack(null, (v, choice) -> sent.add(choice)); stack.add(view); return stack; });
            context.runOnClient(c -> {
                var stack = (DialogStack)c.gui.screen();
                var screen = (DialogLeaderEvent)stack.find(view.token());
                int left = screen.panelLeft(), top = screen.panelTop();
                int[] header = screen.bounds(300, 30, 1, 1);
                var right = new MouseButtonEvent(header[0], header[1], new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0));
                stack.mouseClicked(right, false); stack.mouseDragged(right, 25, 10); stack.mouseReleased(right);
                if (screen.panelLeft() != left || screen.panelTop() != top) throw new AssertionError("Right drag must not move an event");
                var press = new MouseButtonEvent(header[0], header[1], new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                if (!stack.mouseClicked(press, false) || !stack.mouseDragged(press, 25, 10)) throw new AssertionError("Stack must route header dragging");
                stack.mouseReleased(press);
                if (screen.panelLeft() != Math.min(left + 25, screen.width - screen.panelWidth() - 8)) throw new AssertionError("Header drag must move the event and its buttons");
                int movedLeft = screen.panelLeft(), movedTop = screen.panelTop();
                screen.update(view.issued(view.token(), view.revision() + 1));
                if (screen.panelLeft() != movedLeft || screen.panelTop() != movedTop) throw new AssertionError("Revision refresh must preserve the dragged location");
                stack.minimize(view.token()); stack.reopen(view.token());
                if (screen.panelLeft() != movedLeft || screen.panelTop() != movedTop) throw new AssertionError("Minimize and reopen must preserve the dragged location");
                header = screen.bounds(300, 30, 1, 1);
                press = new MouseButtonEvent(header[0], header[1], new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                stack.mouseClicked(press, false); stack.mouseDragged(press, -100000, -100000); stack.mouseReleased(press);
                if (screen.panelLeft() != 8 || screen.panelTop() != 8) throw new AssertionError("Dragging must stop at the viewport boundary");
                for (var child : screen.children()) if (child instanceof Button button && (button.getX() < screen.panelLeft() || button.getY() < screen.panelTop() || button.getRight() > screen.panelLeft() + screen.panelWidth() || button.getBottom() > screen.panelTop() + screen.panelHeight())) throw new AssertionError("Dragged buttons must follow the event");
                if (!sent.isEmpty()) throw new AssertionError("Dragging and minimizing must not issue a choice");
            });
            context.waitTicks(2);
            context.takeScreenshot("hoi-leader-event-dragged-" + size[0] + "-gui" + size[2]);
        }
        context.runOnClient(c -> c.options.guiScale().set(2));
    }
    private static void assertChoiceLabels(net.minecraft.client.gui.Font font, DialogScreen screen, DialogView view) {
        float scale = Math.max(dev.hoi.client.ui.UiText.scale(font), screen.textScale() + 1f / font.lineHeight);
        for (var choice : view.choices()) {
            var button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals(choice.label())).findFirst().orElseThrow();
            var lines = font.split(net.minecraft.network.chat.Component.literal(choice.label()), Math.max(1, (int)((button.getWidth() - 6) / scale)));
            if ((lines.size() * (font.lineHeight + 1) - 1) * scale > button.getHeight() - 2) throw new AssertionError("Every choice line must fit at the existing button font size: " + choice.label());
            var displayed = new StringBuilder();
            for (var line : lines) {
                if (font.width(line) * scale > button.getWidth() - 6) throw new AssertionError("Wrapped choice exceeds the button width");
                line.accept((index, style, codePoint) -> { if (!Character.isWhitespace(codePoint)) displayed.appendCodePoint(codePoint); return true; });
            }
            if (!displayed.toString().equals(choice.label().replaceAll("\\s", ""))) throw new AssertionError("Wrapped choices must retain all characters, including final punctuation");
            if (button.getBottom() > screen.panelTop() + screen.panelHeight()) throw new AssertionError("Expanded choices must fit inside the event panel");
            for (var child : screen.children()) if (child instanceof Button other && other != button && button.getX() < other.getRight() && other.getX() < button.getRight() && button.getY() < other.getBottom() && other.getY() < button.getBottom()) throw new AssertionError("Expanded event choices must not overlap another button");
        }
    }
    public static void regression(ClientGameTestContext context) {
        var fixture = new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(DialogLeaderEventTest.class.getResourceAsStream("/dialog-fixtures.json")), StandardCharsets.UTF_8), Fixtures.class);
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2));
        for (var kind : List.of(DialogView.Kind.EVENT, DialogView.Kind.GLOBAL_EVENT, DialogView.Kind.SUPER_EVENT)) {
            var view = fixture.views().stream().filter(v -> v.kind() == kind).findFirst().orElseThrow();
            context.setScreen(() -> new DialogScreen(view));
            context.getInput().setCursorPos(20, 1400);
            context.waitTicks(4);
            context.takeScreenshot("hoi-leader-regression-" + kind.name().toLowerCase() + "-gui2");
        }
        var event = fixture.views().stream().filter(v -> v.kind() == DialogView.Kind.EVENT).findFirst().orElseThrow();
        context.getInput().resizeWindow(1280, 720);
        context.setScreen(() -> new DialogScreen(event));
        context.waitTicks(4);
        captureWindow(context, "hoi-event-title-event-1280-gui2");
        context.getInput().resizeWindow(2560, 1440);
        var complete = new DialogView(java.util.UUID.randomUUID().toString(), 3, DialogView.Kind.DIPLOMACY, "완료된 연구·중점 이름", "", "", "", "category/infantry", "", List.of(), List.of(new DialogView.Choice("details", "세부 사항"), new DialogView.Choice("ack", "확인")), "research_complete");
        context.setScreen(() -> new DialogCompletion(complete, (v, choice) -> {}));
        context.waitTicks(4);
        context.takeScreenshot("hoi-leader-regression-completion-gui2");
        context.setScreen(() -> null);
    }
    private static void titleComparisons(ClientGameTestContext context, DialogView source) throws java.io.IOException {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2));
        for (String title : List.of("조선반도 내 상황 보고서", "코로나19 바이러스")) {
            for (float ratio : new float[]{1.5f, 2f}) {
                var view = new DialogView(source.token(), source.revision(), source.kind(), title, "", "", "", "", "", List.of(), List.of());
                context.setScreen(() -> new DialogScreen(view) {
                    @Override float eventTitleRatio() { return ratio; }
                    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float delta) {
                        g.fill(0, 0, width, height, 0xffe7e5e8);
                        eventTitle(g, width / 2 - 200, height / 2 - 25, 400, 50, 0xff000000, false);
                    }
                });
                context.waitTicks(4);
                var screenshot = context.takeScreenshot("hoi-event-title-candidate-" + (title.contains("19") ? "mixed" : "korean") + "-" + ratio + "-gui2");
                var image = javax.imageio.ImageIO.read(screenshot.toFile());
                javax.imageio.ImageIO.write(image.getSubimage(880, 670, 800, 100), "png", screenshot.resolveSibling(screenshot.getFileName().toString().replace(".png", "-title.png")).toFile());
            }
        }
        String longest = "윤석열 전 대통령 등에 의한 내란외환 행위의 진상규명을 위한 특별검사 임명 등에 관한 법률";
        for (boolean leader : new boolean[]{false, true}) {
            var view = new DialogView(source.token(), source.revision(), source.kind(), longest, source.subtitle(), source.body(), source.flag(), source.image(), source.sound(), source.tiles(), source.choices(), leader ? source.presentation() : "");
            context.setScreen(() -> leader ? new DialogLeaderEvent(view, (v, choice) -> {}) : new DialogScreen(view));
            context.runOnClient(c -> {
                var screen = (DialogScreen)c.gui.screen();
                int titleWidth = leader ? ((DialogLeaderEvent)screen).bounds(14, 70, 551, 32)[2] : screen.eventHeadingBounds()[2];
                if (screen.eventTitleScale(titleWidth) != screen.textScale()) throw new AssertionError("Longest source title must retain bold at the body size before ellipsis");
                if (screen.panelTop() < 0 || screen.panelTop() + screen.panelHeight() > screen.height) throw new AssertionError("Longest-title panel outside viewport");
            });
            context.waitTicks(4);
            captureWindow(context, "hoi-event-title-longest-" + (leader ? "leader" : "event") + "-2560-gui2");
        }
        context.getInput().resizeWindow(1280, 720);
    }
    private static void captureWindow(ClientGameTestContext context, String name) {
        var screenshot = context.takeScreenshot(name);
        int[] bounds = context.computeOnClient(c -> { var screen = (DialogScreen)c.gui.screen(); return new int[]{screen.panelLeft() * 2, screen.panelTop() * 2, screen.panelWidth() * 2, screen.panelHeight() * 2}; });
        try {
            var image = javax.imageio.ImageIO.read(screenshot.toFile());
            javax.imageio.ImageIO.write(image.getSubimage(bounds[0], bounds[1], bounds[2], bounds[3]), "png", screenshot.resolveSibling(screenshot.getFileName().toString().replace(".png", "-window.png")).toFile());
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
}
