package dev.hoi.client.screen.dialog;

import com.google.gson.Gson;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.dialog.DialogView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class DialogEventFrameTest {
    record Example(String name, String id, DialogView view) {}
    record Fixtures(List<Example> cases) {}
    public static void showHudFixture(ClientGameTestContext context) {
        var input=Objects.requireNonNull(DialogEventFrameTest.class.getResourceAsStream("/event-frame-fixtures.json"));
        var examples=new Gson().fromJson(new InputStreamReader(input,StandardCharsets.UTF_8),Fixtures.class).cases();
        var view=examples.stream().map(Example::view).filter(v->v.kind()==DialogView.Kind.EVENT).findFirst().orElseThrow();
        context.setScreen(()->new DialogScreen(view,(v,choice)->{}));
    }
    public static void run(ClientGameTestContext context) {
        var input = Objects.requireNonNull(DialogEventFrameTest.class.getResourceAsStream("/event-frame-fixtures.json"));
        var examples = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Fixtures.class).cases();
        var measurements = new LinkedHashMap<String, Object>();
        for (int[] size : new int[][]{{2560, 1440}, {1280, 720}}) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.runOnClient(c -> c.options.guiScale().set(2));
            context.getInput().setCursorPos(10, 10);
            for (var example : examples) {
                var sent = new ArrayList<String>();
                var view = example.view().issued(UUID.randomUUID().toString(), 7);
                context.setScreen(() -> new DialogScreen(view) {
                    @Override protected void choose(String choice) { sent.add(view().token() + "/" + view().revision() + "/" + choice); }
                });
                context.runOnClient(c -> c.gui.screen().setFocused(null));
                context.getInput().setCursorPos(10, 10);
                context.waitTicks(4);
                int[] panel = context.computeOnClient(c -> {
                    var screen = (DialogScreen)c.gui.screen();
                    check(c.getWindow().getGuiScale() == 2, "Effective GUI scale 2 is required");
                    check(screen.panelLeft() >= 0 && screen.panelTop() >= 0 && screen.panelLeft() + screen.panelWidth() <= screen.width
                            && screen.panelTop() + screen.panelHeight() <= screen.height, "Entire source frame must fit on screen: " + example.id());
                    check(screen.textScale() == dev.hoi.client.ui.UiText.scale(c.font), "Retain body font size");
                    if (size[0] == 2560 && view.kind() == DialogView.Kind.EVENT)
                        check(screen.panelTop() == 134, "National event top stays fixed at native CENTER(-355,-450) plus top sprite offset");
                    if (size[0] == 2560 && view.kind() == DialogView.Kind.SUPER_EVENT) {
                        int[] offset = UiAssets.eventSuperOffset(view.image());
                        check(screen.panelLeft() == screen.width / 2 + (int)Math.round(offset[0] * .5)
                                && screen.panelTop() == screen.height / 2 + (int)Math.round(offset[1] * .5), "Native 982x594 root center and TFRKP show offset retained");
                    }
                    check(screen.bodyViewportHeight() >= 12, "Visible body line must remain available");
                    int[] body = screen.eventBodyBounds(), picture = screen.eventPictureBounds(), title = screen.eventHeadingBounds();
                    check(title[1] + title[3] <= (view.kind() == DialogView.Kind.GLOBAL_EVENT ? picture[1] : body[1]), "Title must clear body and picture");
                    var buttons = choiceButtons(screen, view);
                    if (view.kind() == DialogView.Kind.GLOBAL_EVENT) {
                        check(buttons.getFirst().getY() == body[1] + body[3] + (int)Math.round(18 * screen.frameScale()), "News choices follow the visible description with 20 native pixels to the visible sprite edge");
                        check(body[3] % 12 == 0, "News starts with complete body lines");
                        check(buttons.getLast().getBottom() <= screen.panelTop() + (int)Math.round(800 * screen.frameScale()), "News choices clear the bottom paper fade");
                    }
                    check(buttons.stream().map(Button::getX).distinct().count() == 1, "Original choices use one column");
                    for (var b : buttons) {
                        check(b.getY() >= body[1] + body[3], "Choices must clear body viewport");
                        if (view.kind() == DialogView.Kind.EVENT) check(b.getBottom() <= picture[1], "National event picture follows every visible choice");
                        check(b.getX() >= screen.panelLeft() && b.getRight() <= screen.panelLeft() + screen.panelWidth()
                                && b.getBottom() <= screen.panelTop() + screen.panelHeight(), "Choice must stay in its frame");
                        float scale = Math.max(screen.textScale(), screen.textScale() + 1f / c.font.lineHeight);
                        var lines = c.font.split(b.getMessage(), Math.max(1, (int)((b.getWidth() - 6) / scale)));
                        check((lines.size() * (c.font.lineHeight + 1) - 1) * scale <= b.getHeight() - 2, "Every option line fits its background");
                        var rendered = new StringBuilder();
                        for (var line : lines) {
                            check(c.font.width(line) * scale <= b.getWidth() - 6, "Choice text stays inside its background");
                            line.accept((index, style, point) -> { if (!Character.isWhitespace(point)) rendered.appendCodePoint(point); return true; });
                        }
                        check(rendered.toString().equals(b.getMessage().getString().replaceAll("\\s", "")), "Every choice character and punctuation retained");
                    }
                    if (!view.image().isEmpty()) {
                        int[] source = UiAssets.eventImageSize(view.image());
                        check(source[0] > 0 && source[1] > 0 && picture[2] > 0 && picture[3] > 0, "Source image dimensions available");
                        check(Math.abs(picture[2] / (double)picture[3] - source[0] / (double)source[1]) <= 2.0 / picture[3], "Original image ratio retained");
                    } else check(picture[2] == 0 && picture[3] == 0, "Missing source image stays empty");
                    measurements.put(example.name() + "-" + size[0], Map.of("id", example.id(), "panelGui", new int[]{screen.panelLeft(), screen.panelTop(), screen.panelWidth(), screen.panelHeight()},
                            "titleGui", title, "bodyGui", body, "pictureGui", picture, "scale", screen.frameScale(), "choicesGui", boxes(buttons),
                            "bodyLines", c.font.split(screen.bodyText(), Math.max(1, (int)(body[2] / screen.textScale()))).size(), "bodyScrollPixels", Math.max(0, c.font.split(screen.bodyText(), Math.max(1, (int)(body[2] / screen.textScale()))).size() * 12 - body[3])));
                    return new int[]{screen.panelLeft() * 2, screen.panelTop() * 2, screen.panelWidth() * 2, screen.panelHeight() * 2};
                });
                capture(context, "hoi-event-frame-" + example.name() + "-" + size[0] + "-gui2", panel);
                context.runOnClient(c -> {
                    var screen = (DialogScreen)c.gui.screen();
                    var reached = new LinkedHashSet<String>();
                    for (int page = 0; page < 24; page++) {
                        var buttons = choiceButtons(screen, view);
                        reached.addAll(screen.displayedEventChoices());
                        var next = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                                .filter(b -> b.getMessage().getString().equals("다음 선택지")).findFirst();
                        if (next.isEmpty() || !next.get().active) break;
                        var b = next.get();
                        screen.mouseClicked(mouse(b, InputConstants.MOUSE_BUTTON_RIGHT), false);
                        check(choiceButtons(screen, view).equals(buttons), "Right click does not turn a page");
                        screen.mouseClicked(mouse(b, InputConstants.MOUSE_BUTTON_LEFT), false);
                    }
                    check(reached.size() == view.choices().size(), "All source options must be reachable");
                    int[] body = screen.eventBodyBounds(), picture = screen.eventPictureBounds();
                    var choices = boxes(choiceButtons(screen, view));
                    check(!screen.mouseScrolled(picture[0], picture[1], 0, -100), "Picture does not scroll the body");
                    int lines = c.font.split(screen.bodyText(), Math.max(1, (int)(body[2] / screen.textScale()))).size();
                    int maximum = Math.max(0, lines * 12 - body[3]);
                    check(body[3] % 12 == 0, "Body viewport contains complete lines");
                    screen.mouseScrolled(body[0] + 1, body[1] + 1, 0, -1);
                    check(screen.scrollOffset() % 12 == 0, "Wheel scrolling cannot clip a partial first line");
                    if (maximum > 0) {
                        check(screen.keyPressed(new KeyEvent(InputConstants.KEY_END, 0, 0)) && screen.scrollOffset() == maximum, "End exposes the last complete source line");
                        check(body[1] + (lines - 1) * 12 - screen.scrollOffset() >= body[1]
                                && body[1] + (lines - 1) * 12 - screen.scrollOffset() + c.font.lineHeight * screen.textScale() <= body[1] + body[3], "Final glyph row clears scissor bounds");
                        check(screen.keyPressed(new KeyEvent(InputConstants.KEY_HOME, 0, 0)) && screen.scrollOffset() == 0, "Home returns to the first line");
                        screen.keyPressed(new KeyEvent(InputConstants.KEY_PAGEDOWN, 0, 0));
                        check(screen.scrollOffset() == Math.min(maximum, body[3]), "PageDown advances by a complete viewport");
                        screen.keyPressed(new KeyEvent(InputConstants.KEY_PAGEUP, 0, 0));
                        check(screen.scrollOffset() == 0 && sent.isEmpty(), "Reading controls never choose an option");
                    }
                    screen.mouseScrolled(body[0] + 1, body[1] + 1, 0, -100);
                    check(screen.scrollOffset() == maximum, "All source text remains reachable by wheel");
                    if (example.name().contains("long-body") || example.name().contains("long-quote") && size[0] == 1280)
                        check(screen.scrollOffset() > 0, "Long body/quote scrolls");
                    check(Arrays.equals(picture, screen.eventPictureBounds()) && choices.equals(boxes(choiceButtons(screen, view))), "Body scrolling leaves picture and choices fixed");
                    var first = choiceButtons(screen, view).getFirst();
                    screen.mouseClicked(mouse(first, InputConstants.MOUSE_BUTTON_RIGHT), false);
                    check(sent.isEmpty(), "Right click cannot choose an event option");
                    screen.mouseDragged(mouse(first, InputConstants.MOUSE_BUTTON_RIGHT), 10, 10);
                    check(sent.isEmpty(), "Dragging cannot submit an option");
                    first.active = false;
                    screen.mouseClicked(mouse(first, InputConstants.MOUSE_BUTTON_LEFT), false);
                    check(sent.isEmpty(), "Disabled option cannot submit");
                    first.active = true;
                    screen.mouseClicked(mouse(first, InputConstants.MOUSE_BUTTON_LEFT), false);
                    check(sent.size() == 1 && sent.getFirst().startsWith(view.token() + "/7/"), "Left click retains issued token/revision");
                    screen.mouseClicked(mouse(first, InputConstants.MOUSE_BUTTON_LEFT), true);
                    check(sent.size() == 2 && sent.getFirst().equals(sent.getLast()), "Repeated click cannot invent another option or revision");
                    screen.update(view.issued(view.token(), 6));
                    check(screen.view().revision() == 7, "Stale response rejected");
                    screen.setFocused(null);
                });
                context.getInput().setCursorPos(10, 10);
                context.waitTicks(3);
                capture(context, "hoi-event-frame-" + example.name() + "-" + size[0] + "-gui2-scrolled-last-page", panel);
                context.runOnClient(c -> {
                    var screen = (DialogScreen)c.gui.screen();
                    screen.setFocused(null);
                    screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                    check(c.gui.screen() == null && (view.kind() == DialogView.Kind.SUPER_EVENT ? sent.getLast().equals(view.token() + "/7/") : sent.size() == 2), "Esc minimizes answerable events without sending an empty choice; super events retain dismissal");
                });
            }
        }
        try { Files.writeString(Path.of("event-frame-measurements.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(measurements), StandardCharsets.UTF_8); }
        catch (IOException error) { throw new UncheckedIOException(error); }
        context.setScreen(() -> null);
        context.getInput().resizeWindow(1600, 1000);
        context.waitTicks(3);
    }
    private static List<Button> choiceButtons(DialogScreen screen, DialogView view) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> view.choices().stream().anyMatch(choice -> choice.label().equals(b.getMessage().getString()))).toList();
    }
    private static List<List<Integer>> boxes(List<Button> buttons) { return buttons.stream().map(b -> List.of(b.getX(), b.getY(), b.getWidth(), b.getHeight())).toList(); }
    private static MouseButtonEvent mouse(Button b, int button) { return new MouseButtonEvent(b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0, new MouseButtonInfo(button, 0)); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void capture(ClientGameTestContext context, String name, int[] panel) {
        var path = context.takeScreenshot(name);
        try {
            var image = javax.imageio.ImageIO.read(path.toFile());
            javax.imageio.ImageIO.write(image.getSubimage(panel[0], panel[1], panel[2], panel[3]), "png", path.resolveSibling(path.getFileName().toString().replace(".png", "-window.png")).toFile());
        } catch (IOException error) { throw new UncheckedIOException(error); }
    }
}
