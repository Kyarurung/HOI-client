package dev.hoi.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.ProtocolAgencyPayloads;
import dev.hoi.protocol.ProtocolAgencyView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ScreenAgencyOperationTest {
    private static MouseButtonEvent event(double x, double y, int button) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
    }
    private static Button button(Screen screen, String label) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(value -> value.getMessage().getString().equals(label)).findFirst().orElseThrow();
    }
    private static void click(Screen screen, Button button, int mouse) {
        var event = event(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, mouse);
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }
    private static int field(Screen screen, String name) {
        try { var field = screen.getClass().getDeclaredField(name); field.setAccessible(true); return field.getInt(screen); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    public static void run(ClientGameTestContext context) {
        String directory = System.getenv("HOI_AGENCY_OPERATION_PREVIEW");
        if (directory == null) return;
        ScreenAgencyCipherTest.run(context, directory);
        boolean before = "1".equals(System.getenv("HOI_CAPTURE_BEFORE"));
        ProtocolAgencyView view;
        try { view = new ProtocolAgencyPayloads.Response(Files.readString(Path.of(directory, "kor-agency-operatives.json"))).view(); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        int scale = context.computeOnClient(client -> client.options.guiScale().get());
        context.runOnClient(client -> {
            for (String sprite : List.of("gfx_operation_resource_progress_unfinished", "gfx_operation_resource_progress_finished", "gfx_group_add_operative_anim")) {
                if (client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/agency/operations/" + sprite + "/frame_0.png")).isEmpty())
                    throw new AssertionError("Missing operation control source asset: " + sprite);
            }
            var decorated = dev.hoi.client.ui.UiKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal("£text_infiltrate_civilian_token£"), false);
            if (decorated.getString().codePoints().noneMatch(value -> value >= 0xe000 && value <= 0xf8ff)) throw new AssertionError("Original reward token must resolve from the bounded runtime keyword catalog");
        });
        try {
            for (int[] size : new int[][]{{2560, 1440}, {1280, 720}}) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var sent = new ArrayList<ProtocolAgencyPayloads.Request>();
                context.setScreen(() -> new ScreenAgency(null, view.session(), view, sent::add));
                context.waitTicks(2);
                context.takeScreenshot("hoi-operation-list-source-" + size[0] + "-gui2");
                if (!before) startModes(context, view, size[0]);
                for (var offer : view.offers()) {
                    context.setScreen(() -> new ScreenAgency(null, view.session(), view, sent::add));
                    context.runOnClient(client -> ((ScreenAgency)client.gui.screen()).showMapItem(offer.item()));
                    context.waitTicks(2);
                    if (offer.type().equals("operation_infiltrate_civilian") || offer.type().equals("operation_coup_government"))
                        context.takeScreenshot("hoi-operation-offer-" + offer.type() + "-" + size[0] + "-gui2");
                    if (before) continue;
                    context.runOnClient(client -> {
                        var screen = (ScreenAgencyOperation)client.gui.screen();
                        if (offer.required() > 0 && button(screen, "준비하기").active) throw new AssertionError("Unselected operatives must keep preparation disabled");
                    });
                    if (offer.required() > 0 && offer.type().equals("operation_infiltrate_civilian")) {
                        context.runOnClient(client -> {
                            var screen = (ScreenAgencyOperation)client.gui.screen();
                            var slot = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                                    .filter(value -> value.getWidth() == 27 && value.getHeight() == 37).findFirst().orElseThrow();
                            click(screen, slot, InputConstants.MOUSE_BUTTON_RIGHT);
                            if (field(screen, "choosing") != -1) throw new AssertionError("Right click cannot open operative selection");
                            click(screen, slot, InputConstants.MOUSE_BUTTON_LEFT);
                            if (field(screen, "choosing") != 1 || button(screen, "준비하기").active) throw new AssertionError("Portrait slot must open candidate selection and suspend prepare");
                        });
                        context.waitTicks(2);
                        context.takeScreenshot("hoi-operation-operative-picker-" + size[0] + "-gui2");
                        context.runOnClient(client -> {
                            var screen = (ScreenAgencyCandidates)client.gui.screen();
                            var candidate = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                                    .filter(value -> value.getHeight() == 49 && value.active).findFirst().orElseThrow();
                            int count = sent.size();
                            click(screen, candidate, InputConstants.MOUSE_BUTTON_LEFT);
                            if (!(client.gui.screen() instanceof ScreenAgencyOperation operation) || field(operation, "choosing") != -1 || sent.size() != count) throw new AssertionError("Candidate selection changes only the local operation draft");
                        });
                    }
                    context.runOnClient(client -> {
                        var screen = (ScreenAgencyOperation)client.gui.screen();
                        selectOperatives(screen);
                        var action = button(screen, "준비하기");
                        int requests = sent.size();
                        click(screen, action, InputConstants.MOUSE_BUTTON_RIGHT);
                        if (sent.size() != requests) throw new AssertionError("Right click must not prepare an operation");
                        if (!offer.enabled() && action.active) throw new AssertionError("Locked source operation became enabled: " + offer.type());
                        if (action.active) {
                            click(screen, action, InputConstants.MOUSE_BUTTON_LEFT); click(screen, action, InputConstants.MOUSE_BUTTON_LEFT);
                            if (sent.size() != requests + 1) throw new AssertionError("Prepare request count: " + offer.type() + "=" + sent
                                    + " active=" + action.active + " screen=" + client.gui.screen().getClass().getSimpleName()
                                    + " pending=" + field(screen.backdrop(), "pendingTicks") + " bounds=" + action.getX() + "," + action.getY());
                            var request = sent.getLast();
                            if (sent.size() != requests + 1 || !request.item().equals(offer.item()) || request.revision() != view.revision()
                                    || !request.session().equals(view.session()) || request.arguments().size() != view.items().stream().filter(value -> value.id().equals(offer.item())).findFirst().orElseThrow().parameters().size()
                                    || request.arguments().subList(1, offer.required() + 1).stream().distinct().count() != offer.required())
                                throw new AssertionError("Source id, revision and distinct operative arguments must be sent once");
                        } else {
                            click(screen, action, InputConstants.MOUSE_BUTTON_LEFT); click(screen, action, InputConstants.MOUSE_BUTTON_LEFT);
                            if (sent.size() != requests) throw new AssertionError("Locked offer sent an action");
                        }
                    });
                }
                var operation = view.operations().getFirst();
                context.setScreen(() -> new ScreenAgency(null, view.session(), view, sent::add));
                context.runOnClient(client -> ((ScreenAgency)client.gui.screen()).showMapItem(operation.item()));
                context.waitTicks(2);
                context.takeScreenshot("hoi-operation-running-source-" + size[0] + "-gui2");
                if (before) continue;
                context.runOnClient(client -> {
                    var screen = (ScreenAgencyOperation)client.gui.screen();
                    for (var phase : operation.phases()) for (String source : List.of(phase.picture(), phase.icon())) {
                        var id = Identifier.parse("hoi:textures/gui/agency/operations/" + source.toLowerCase(java.util.Locale.ROOT) + "/frame_0.png");
                        if (client.getResourceManager().getResource(id).isEmpty()) throw new AssertionError("Missing source phase " + id);
                    }
                    int x = field(screen, "x"), y = field(screen, "y"), w = field(screen, "w"), h = field(screen, "h");
                    screen.mouseScrolled(x + w - 30, y + h - 75, 0, -1000);
                    if (field(screen, "contentHeight") > h - 86 && field(screen, "scroll") <= 0) throw new AssertionError("Overflowing phase descriptions require scrolling");
                    var start = event(x + w - 8, y + 60, InputConstants.MOUSE_BUTTON_LEFT);
                    var end = event(x + w - 8, y + h - 65, InputConstants.MOUSE_BUTTON_LEFT);
                    screen.mouseClicked(start, false); screen.mouseDragged(end, 0, 100); screen.mouseReleased(end);
                    int offset = field(screen, "scroll");
                    screen.mouseDragged(start, 0, -100);
                    if (field(screen, "scroll") != offset) throw new AssertionError("Released scrollbar must not keep dragging");
                    var title = event(x + 40, y + 8, InputConstants.MOUSE_BUTTON_LEFT);
                    screen.mouseClicked(title, false); screen.mouseDragged(event(x + 50, y + 15, InputConstants.MOUSE_BUTTON_LEFT), 10, 7); screen.mouseReleased(title);
                    if (field(screen, "x") != x + 10 || field(screen, "y") != y + 7) throw new AssertionError("Source window title must drag");
                    screen.mouseDragged(title, 20, 20);
                    if (field(screen, "x") != x + 10) throw new AssertionError("Released title must not keep dragging");
                });
                context.waitTicks(2);
                context.takeScreenshot("hoi-operation-running-scrolled-" + size[0] + "-gui2");
                context.runOnClient(client -> {
                    var screen = (ScreenAgencyOperation)client.gui.screen();
                    var parent = screen.backdrop();
                    parent.update(view);
                    if (client.gui.screen() != screen) throw new AssertionError("Stale reply changed operation window");
                    var action = button(screen, view.items().stream().filter(item -> item.id().equals(operation.item()))
                            .findFirst().orElseThrow().button());
                    int requests = sent.size();
                    click(screen, action, InputConstants.MOUSE_BUTTON_RIGHT);
                    if (sent.size() != requests) throw new AssertionError("Right click cannot cancel an operation");
                    click(screen, action, InputConstants.MOUSE_BUTTON_LEFT);
                    click(screen, action, InputConstants.MOUSE_BUTTON_LEFT);
                    if (action.active || sent.size() != requests) throw new AssertionError("Running operation cannot be cancelled");
                    parent.update(view);
                    click(screen, action, InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != requests) throw new AssertionError("Stale reply cannot unlock cancellation");
                    parent.update(new ProtocolAgencyView(view.session(), 0, "", "", "", List.of(), ""));
                    if (client.gui.screen() != null) throw new AssertionError("Revocation must close operation details");
                });
                stages(context, directory, size[0]);
                preparationFill(context, view, size[0]);
                recruitment(context, directory, size[0]);
            }
        } finally { context.setScreen(() -> null); context.runOnClient(client -> client.options.guiScale().set(scale)); }
    }
    private static void startModes(ClientGameTestContext context, ProtocolAgencyView view, int width) {
        var item = view.items().stream().filter(value -> value.id().startsWith("operation:") && value.enabled()
                && !value.parameters().isEmpty() && value.parameters().getLast().choices().stream().anyMatch(choice -> choice.value().equals("manual")))
                .findFirst().orElse(null);
        if (item == null) return;
        for (boolean resume : new boolean[]{false, true}) for (String mode : List.of("manual", "auto_commence", "auto_repeat", "auto_commence+auto_repeat")) {
            var sent = new ArrayList<ProtocolAgencyPayloads.Request>();
            context.setScreen(() -> new ScreenAgency(null, view.session(), view, sent::add));
            context.runOnClient(client -> {
                ((ScreenAgency)client.gui.screen()).showMapItem(item.id());
                var screen = (ScreenAgencyOperation)client.gui.screen();
                long portraits = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(value -> value.getWidth() == 27 && value.getHeight() == 37).count();
                if (portraits != item.parameters().size() - 2) throw new AssertionError("Start mode rendered as an operative slot");
                if (resume) {
                    var slot = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                            .filter(value -> value.getWidth() == 27 && value.getHeight() == 37).findFirst().orElseThrow();
                    click(screen, slot, InputConstants.MOUSE_BUTTON_LEFT);
                    var picker = client.gui.screen();
                    click(picker, button(picker, "임무 재개"), InputConstants.MOUSE_BUTTON_RIGHT);
                    if (screen.resumes(1)) throw new AssertionError("Resume defaults off and ignores right click");
                    click(picker, button(picker, "임무 재개"), InputConstants.MOUSE_BUTTON_LEFT);
                    if (!screen.resumes(1)) throw new AssertionError("Resume checkbox did not update the draft");
                    picker.onClose();
                }
                for (String flag : List.of("auto_commence", "auto_repeat")) {
                    String label = item.parameters().getLast().choices().stream().filter(choice -> choice.value().equals(flag)).findFirst().orElseThrow().label();
                    click(screen, button(screen, label), InputConstants.MOUSE_BUTTON_RIGHT);
                    click(screen, button(screen, label), InputConstants.MOUSE_BUTTON_LEFT);
                    click(screen, button(screen, label), InputConstants.MOUSE_BUTTON_LEFT);
                    if (mode.contains(flag)) click(screen, button(screen, label), InputConstants.MOUSE_BUTTON_LEFT);
                }
                if (!sent.isEmpty()) throw new AssertionError("Draft checkboxes must not send a server request");
            });
            context.waitTicks(2);
            context.takeScreenshot("hoi-operation-start-mode-" + mode.replace('+', '-') + (resume ? "-resume1" : "") + "-" + width + "-gui2");
            context.runOnClient(client -> {
                var screen = (ScreenAgencyOperation)client.gui.screen();
                if (item.parameters().size() > 2 && button(screen, "준비하기").active) throw new AssertionError("Start mode cannot auto-select operatives");
                selectOperatives(screen);
                var prepare = button(screen, "준비하기");
                if (!prepare.active) throw new AssertionError("Valid operation draft cannot prepare");
                click(screen, prepare, InputConstants.MOUSE_BUTTON_LEFT);
                click(screen, prepare, InputConstants.MOUSE_BUTTON_LEFT);
                if (sent.size() != 1 || !sent.getFirst().arguments().getLast().equals(mode + (resume ? "+resume1" : ""))
                        || sent.getFirst().arguments().size() != item.parameters().size()
                        || sent.getFirst().revision() != view.revision() || !sent.getFirst().session().equals(view.session()))
                    throw new AssertionError("Draft start mode must be sent once with issued arguments: " + mode);
            });
        }
    }
    private static void selectOperatives(Screen screen) {
        var slots = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(value -> value.getWidth() == 27 && value.getHeight() == 37).toList();
        for (int i = 0; i < slots.size(); i++) {
            var slot = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(value -> value.getWidth() == 27 && value.getHeight() == 37).toList().get(i);
            if (!slot.active) continue;
            click(screen, slot, InputConstants.MOUSE_BUTTON_LEFT);
            var picker = net.minecraft.client.Minecraft.getInstance().gui.screen();
            if (!(picker instanceof ScreenAgencyCandidates)) throw new AssertionError("Operative slot must open the source candidate window");
            var candidate = picker.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(value -> value.getHeight() == 49 && value.active).findFirst();
            if (candidate.isPresent()) click(picker, candidate.get(), InputConstants.MOUSE_BUTTON_LEFT);
            else picker.onClose();
        }
    }
    private static void stages(ClientGameTestContext context, String directory, int width) {
        ProtocolAgencyView view;
        try { view = new ProtocolAgencyPayloads.Response(Files.readString(Path.of(directory, "kor-agency-operation-stages.json"))).view(); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        for (var operation : view.operations()) {
            var sent = new ArrayList<ProtocolAgencyPayloads.Request>();
            context.setScreen(() -> new ScreenAgency(null, view.session(), view, sent::add));
            context.runOnClient(client -> ((ScreenAgency)client.gui.screen()).showMapItem(operation.item()));
            context.waitTicks(2);
            context.takeScreenshot("hoi-operation-stage-" + operation.stage().toLowerCase(java.util.Locale.ROOT) + "-" + operation.id() + "-" + width + "-gui2");
            if (operation.stage().equals("COMPLETED")) {
                context.runOnClient(client -> {
                    var screen = (ScreenAgencyOperation)client.gui.screen();
                    var close = button(screen, dev.hoi.client.ui.UiCommandLabels.text("CLOSE", "닫기"));
                    click(screen, close, InputConstants.MOUSE_BUTTON_RIGHT);
                    if (!sent.isEmpty()) throw new AssertionError("Right click dismissed a completed operation");
                    click(screen, close, InputConstants.MOUSE_BUTTON_LEFT);
                    click(screen, close, InputConstants.MOUSE_BUTTON_LEFT);
                    screen.backdrop().update(view);
                    click(screen, close, InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != 1 || !sent.getFirst().item().equals(operation.item())
                            || !sent.getFirst().session().equals(view.session()) || sent.getFirst().revision() != view.revision())
                        throw new AssertionError("Completed window must send the issued dismiss request once");
                });
                sent.clear();
                var json = new com.google.gson.Gson().toJsonTree(view).getAsJsonObject();
                var rows = json.getAsJsonArray("items");
                for (int index = rows.size() - 1; index >= 0; index--) {
                    var row = rows.get(index).getAsJsonObject();
                    if (row.get("group").getAsString().equals("operations") && !row.get("id").getAsString().equals(operation.item())) rows.remove(index);
                }
                var completedView = new ProtocolAgencyPayloads.Response(json.toString()).view();
                context.setScreen(() -> new ScreenAgency(null, view.session(), completedView, sent::add));
                context.waitTicks(2);
                context.takeScreenshot("hoi-operation-completed-list-" + width + "-gui2");
                context.runOnClient(client -> {
                    var screen = (ScreenAgency)client.gui.screen();
                    var action = button(screen, view.items().stream().filter(value -> value.id().equals(operation.item())).findFirst().orElseThrow().button());
                    click(screen, action, InputConstants.MOUSE_BUTTON_RIGHT);
                    if (!sent.isEmpty()) throw new AssertionError("Right list click dismissed a completed operation");
                    click(screen, action, InputConstants.MOUSE_BUTTON_LEFT);
                    click(screen, action, InputConstants.MOUSE_BUTTON_LEFT);
                    screen.update(completedView);
                    click(screen, action, InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != 1 || !sent.getFirst().item().equals(operation.item())) throw new AssertionError("Completed list action must dismiss once without opening details: " + sent + " active=" + action.active + " screen=" + client.gui.screen().getClass().getSimpleName());
                });
                continue;
            }
            if (operation.stage().equals("COLLECTING")) {
                context.runOnClient(client -> {
                    var screen = client.gui.screen();
                    screen.mouseScrolled(field(screen, "x") + field(screen, "w") - 30, field(screen, "y") + field(screen, "h") - 75, 0, -1000);
                });
                context.waitTicks(2);
                context.takeScreenshot("hoi-operation-description-bottom-" + operation.id() + "-" + width + "-gui2");
                context.runOnClient(client -> {
                    var screen = client.gui.screen();
                    screen.mouseScrolled(field(screen, "x") + field(screen, "w") - 30, field(screen, "y") + field(screen, "h") - 75, 0, 1000);
                    if (field(screen, "scroll") != 0) throw new AssertionError("Description scroll must return to the first line");
                });
            }
            if (!operation.preparation().label().isEmpty()) {
                int[] hover = context.computeOnClient(client -> new int[]{(field(client.gui.screen(), "x") + 80) * 2,
                        (field(client.gui.screen(), "y") + 119 + ((operation.operatives().size() + 3) / 4) * 38) * 2});
                context.getInput().setCursorPos(hover[0], hover[1]); context.waitTicks(2);
                context.takeScreenshot("hoi-operation-preparation-" + operation.stage().toLowerCase(java.util.Locale.ROOT) + "-" + operation.id() + "-" + width + "-gui2");
                context.getInput().setCursorPos(width - 3, 3);
            }
            if (operation.stage().equals("RUNNING")) {
                int[] hover = context.computeOnClient(client -> new int[]{(field(client.gui.screen(), "x") + 50) * 2,
                        (field(client.gui.screen(), "y") + 82 + ((operation.operatives().size() + 3) / 4) * 38) * 2});
                context.getInput().setCursorPos(hover[0], hover[1]); context.waitTicks(2);
                context.takeScreenshot("hoi-operation-outlook-" + width + "-gui2");
                context.getInput().setCursorPos(width - 3, 3);
            }
            for (String prefix : List.of("auto_commence:", "auto_repeat:", "commence_operation:", "cancel_operation:")) {
                var issued = view.items().stream().filter(value -> value.id().equals(prefix + operation.id())).findFirst().orElse(null);
                if (issued == null) continue;
                context.setScreen(() -> new ScreenAgency(null, view.session(), view, sent::add));
                context.runOnClient(client -> ((ScreenAgency)client.gui.screen()).showMapItem(operation.item()));
                context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = (ScreenAgencyOperation)client.gui.screen();
                    String label = prefix.startsWith("auto_") ? issued.title() : prefix.equals("commence_operation:") && operation.stage().equals("COLLECTING") ? "준비 중..." : issued.button();
                    var control = button(screen, label);
                    if (control.active != issued.enabled()) throw new AssertionError("Issued enabled state lost: " + issued.id());
                    int count = sent.size();
                    click(screen, control, InputConstants.MOUSE_BUTTON_RIGHT);
                    if (sent.size() != count) throw new AssertionError("Right click sent operation control");
                    click(screen, control, InputConstants.MOUSE_BUTTON_LEFT);
                    click(screen, control, InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != count + (issued.enabled() ? 1 : 0)) throw new AssertionError("Operation control repeated or bypassed disabled state: " + issued.id());
                    if (issued.enabled()) {
                        var request = sent.getLast();
                        if (!request.item().equals(issued.id()) || !request.session().equals(view.session()) || request.revision() != view.revision() || !request.arguments().isEmpty())
                            throw new AssertionError("Operation control lost authoritative request context");
                        screen.backdrop().update(view);
                        click(screen, control, InputConstants.MOUSE_BUTTON_LEFT);
                        if (sent.size() != count + 1) throw new AssertionError("Stale reply unlocked operation control");
                    }
                });
            }
        }
    }
    private static void preparationFill(ClientGameTestContext context, ProtocolAgencyView source, int width) {
        var captures = new ArrayList<java.awt.image.BufferedImage>();
        var frame = context.computeOnClient(client -> {
            var id = Identifier.parse("hoi:textures/gui/agency/operations/gfx_efficiency_progressbar_frame/frame_0.png");
            try (var input = client.getResourceManager().getResourceOrThrow(id).open()) { return javax.imageio.ImageIO.read(input); }
            catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        });
        for (double progress : new double[]{0, .4, 1}) {
            var json = new com.google.gson.Gson().toJsonTree(source).getAsJsonObject();
            json.getAsJsonArray("operations").get(0).getAsJsonObject().getAsJsonObject("preparation").addProperty("progress", progress);
            var view = new ProtocolAgencyPayloads.Response(json.toString()).view();
            var operation = view.operations().getFirst();
            context.setScreen(() -> new ScreenAgency(null, view.session(), view, request -> {}));
            context.runOnClient(client -> ((ScreenAgency)client.gui.screen()).showMapItem(operation.item()));
            context.getInput().setCursorPos(width - 3, 3);
            context.waitTicks(2);
            int[] origin = context.computeOnClient(client -> new int[]{(field(client.gui.screen(), "x") + 65) * 2 + 1,
                    (field(client.gui.screen(), "y") + 120 + ((operation.operatives().size() + 3) / 4) * 38) * 2});
            var capture = context.takeScreenshot("hoi-operation-gauge-" + (int)(progress * 100) + "-" + width + "-gui2");
            try {
                var rendered = javax.imageio.ImageIO.read(capture.toFile());
                captures.add(rendered.getSubimage(origin[0], origin[1], 93, 9));
                for (int py = 0; py < frame.getHeight(); py++) for (int px = 0; px < frame.getWidth(); px++) {
                    int expected = frame.getRGB(px, py);
                    if (expected >>> 24 == 255 && (rendered.getRGB(origin[0] - 3 + px, origin[1] - 3 + py) & 0xffffff) != (expected & 0xffffff))
                        throw new AssertionError("Preparation frame must preserve every opaque source pixel at GUI2: " + px + "," + py);
                }
            }
            catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        }
        int changedLeft = 0;
        for (int y = 0; y < 9; y++) for (int x = 0; x < 93; x++) {
            int empty = captures.get(0).getRGB(x, y), partial = captures.get(1).getRGB(x, y), full = captures.get(2).getRGB(x, y);
            if (x < 37 && partial != full || x >= 37 && partial != empty)
                throw new AssertionError("Preparation gauge must reveal the full original fill from its left edge only");
            if (x < 20 && partial != empty) changedLeft++;
        }
        if (changedLeft == 0) throw new AssertionError("Preparation fill must reach the actual framebuffer at its left end");
        if (captures.get(2).getRGB(10, 0) != captures.get(0).getRGB(10, 0))
            throw new AssertionError("Preparation fill must preserve the original one-pixel top gap");
        int[] colors = {0x5a695a, 0x5a695a, 0x5a695a, 0x60715a, 0x60715a, 0x4a614a, 0x394531, 0x394531};
        for (int row = 0; row < colors.length; row++) if ((captures.get(2).getRGB(10, row + 1) & 0xffffff) != colors[row])
            throw new AssertionError("Preparation fill source color differs in row " + row);
    }
    private static void recruitment(ClientGameTestContext context, String directory, int width) {
        ProtocolAgencyView view;
        try { view = new ProtocolAgencyPayloads.Response(Files.readString(Path.of(directory, "kor-agency-recruit.json"))).view(); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        var sent = new ArrayList<ProtocolAgencyPayloads.Request>();
        context.setScreen(() -> new ScreenAgency(null, view.session(), view, sent::add));
        boolean enabled = view.items().stream().anyMatch(item -> item.id().equals("recruit") && item.enabled());
        context.runOnClient(client -> {
            var parent = (ScreenAgency)client.gui.screen();
            parent.showMapItem("recruit");
            if (!enabled) {
                if (client.gui.screen() != parent || !sent.isEmpty()) throw new AssertionError("Recruitment cooldown must block opening candidates");
                client.gui.setScreen(new ScreenAgencyCandidates(parent, view, id -> { throw new AssertionError("Disabled candidate submitted"); }));
            }
        });
        context.waitTicks(2);
        context.takeScreenshot("hoi-agency-returned-candidates-" + (enabled ? "ready-" : "disabled-render-") + width + "-gui2");
        context.runOnClient(client -> {
            var screen = (ScreenAgencyCandidates)client.gui.screen();
            var candidate = view.candidates().getFirst();
            var rows = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(value -> value.getHeight() == 49).toList();
            if (!rows.getFirst().getMessage().getString().equals(candidate.name())) throw new AssertionError("Recruitment must preserve authoritative returned candidate order");
            int count = sent.size();
            click(screen, rows.getFirst(), InputConstants.MOUSE_BUTTON_RIGHT);
            if (sent.size() != count) throw new AssertionError("Right click must not hire returned candidate");
            click(screen, rows.getFirst(), InputConstants.MOUSE_BUTTON_LEFT);
            click(screen, rows.getFirst(), InputConstants.MOUSE_BUTTON_LEFT);
            if (enabled ? sent.size() != count + 1 || !sent.getLast().item().equals(candidate.item()) || sent.getLast().revision() != view.revision() : sent.size() != count) throw new AssertionError("Candidate must honor issued enabled state and identity");
        });
        var ranks = List.of("신병", "자산", "정보원", "비밀 요원", "전설");
        for (int level = 1; level <= ranks.size(); level++) {
            var json = new com.google.gson.Gson().toJsonTree(view).getAsJsonObject();
            for (var entry : json.getAsJsonArray("candidates")) {
                entry.getAsJsonObject().addProperty("level", level);
                entry.getAsJsonObject().addProperty("rank", ranks.get(level - 1));
            }
            var boundary = new ProtocolAgencyPayloads.Response(json.toString()).view();
            context.setScreen(() -> {
                var parent = new ScreenAgency(null, boundary.session(), boundary, sent::add);
                return new ScreenAgencyCandidates(parent, boundary, id -> { throw new AssertionError("Boundary capture cannot hire"); });
            });
            context.runOnClient(client -> {
                if (client.font.width(boundary.candidates().getFirst().rank()) > 50)
                    throw new AssertionError("Authoritative rank would shrink in the candidate row");
            });
            context.waitTicks(2);
            context.takeScreenshot("hoi-agency-candidate-rank-boundary-" + level + "-" + width + "-gui2");
        }
    }
}
