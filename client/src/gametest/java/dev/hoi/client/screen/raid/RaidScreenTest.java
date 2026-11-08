package dev.hoi.client.screen.raid;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.diplomacy.DiplomacyCountryScreen;
import dev.hoi.client.screen.industry.IndustryScreen;
import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.protocol.raid.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.nio.file.*;
import java.util.*;

public final class RaidScreenTest {
    private static String read(String directory, String name) {
        try { return Files.readString(Path.of(directory, name + ".json")); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }
    private static Button button(Screen screen, String id) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
            .filter(value -> value.getMessage().getString().equals(id)).findFirst().orElseThrow(() -> new AssertionError("Missing raid control " + id));
    }
    private static void click(Screen screen, String id, int mouse) {
        var b = button(screen, id);
        var event = new MouseButtonEvent(b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0, new MouseButtonInfo(mouse == 0 ? InputConstants.MOUSE_BUTTON_LEFT : InputConstants.MOUSE_BUTTON_RIGHT, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private static void reportScroll(ClientGameTestContext context, String directory, int width) {
        var json = com.google.gson.JsonParser.parseString(read(directory, "kor-raids-ended")).getAsJsonObject();
        var feedback = json.getAsJsonObject("window").getAsJsonObject("feedback");
        feedback.addProperty("actorEffects", "합성 본문: §R95%§!\n".repeat(30) + "왼쪽 마지막 줄");
        feedback.addProperty("victimEffects", "오른쪽 첫 줄\n" + "합성 본문: §Y5%§!\n".repeat(30) + "오른쪽 마지막 줄");
        var view = new RaidPayloads.Response(json.toString()).view();
        var sent = new ArrayList<RaidPayloads.Request>();
        context.setScreen(() -> new RaidScreen(view.session(), view, sent::add)); context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = client.gui.screen(); var close = button(screen, view.window().feedback().dismiss().item());
            int x = close.getX() - 128, y = close.getY() - 234;
            screen.mouseScrolled(x + 30, y + 150, 0, -100);
            var drawn = new ArrayList<String>();
            var g = new net.minecraft.client.gui.GuiGraphicsExtractor(client, new net.minecraft.client.renderer.state.gui.GuiRenderState(), 0, 0) {
                @Override public void text(net.minecraft.client.gui.Font font, net.minecraft.util.FormattedCharSequence line, int px, int py, int color, boolean shadow) {
                    var text = new StringBuilder(); line.accept((index, style, code) -> { text.appendCodePoint(code); return true; }); drawn.add(text.toString());
                    super.text(font, line, px, py, color, shadow);
                }
            };
            screen.extractRenderState(g, 0, 0, 0);
            if (!drawn.contains("왼쪽 마지막 줄") || !drawn.contains("오른쪽 첫 줄") || drawn.contains("오른쪽 마지막 줄"))
                throw new AssertionError("Raid result bodies did not scroll independently: " + drawn);
            var press = new MouseButtonEvent(x + 120, y + 18, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            var drag = new MouseButtonEvent(press.x() + 12, press.y() + 8, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            screen.mouseClicked(press, false); screen.mouseDragged(drag, 12, 8); screen.mouseReleased(drag);
            var moved = button(screen, view.window().feedback().dismiss().item());
            if (moved.getX() != close.getX() + 12 || moved.getY() != close.getY() + 8) throw new AssertionError("Raid result report drag did not move controls");
            if (sent.stream().anyMatch(request -> request.kind() == RaidPayloads.Kind.CALL)) throw new AssertionError("Local report scrolling or drag emitted a request");
        });
        context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
        context.takeScreenshot("hoi-raids-report-scroll-synthetic-" + width + "-gui2");
    }
    private static void categories(ClientGameTestContext context, String directory, int width) {
        var json = com.google.gson.JsonParser.parseString(read(directory, "kor-raids-in-progress")).getAsJsonObject();
        json.remove("window"); json.remove("setup");
        var view = new RaidPayloads.Response(json.toString()).view();
        var sent = new ArrayList<RaidPayloads.Request>();
        context.setScreen(() -> new RaidScreen(view.session(), view, sent::add));
        context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = client.gui.screen();
            for (var raid : view.raids()) {
                long count = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(value -> value.getMessage().getString().equals(raid.open())).count();
                if (count != 1) throw new AssertionError("Raid must appear once beneath its category: " + raid.id() + " count=" + count);
            }
            for (var category : view.categories()) if (category.launchAll().enabled() != button(screen, category.launchAll().item()).active)
                throw new AssertionError("Category launch availability differs from server");
        });
        context.takeScreenshot("hoi-raids-categories-expanded-" + width + "-gui2");
        for (var category : view.categories()) if (view.raids().stream().noneMatch(raid -> raid.category().equals(category.id()))) {
            var point = context.computeOnClient(client -> {
                var b = button(client.gui.screen(), "expand:" + category.id());
                return new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
            });
            context.getInput().setCursorPos(point[0] * 2, point[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-raids-empty-category-tooltip-" + width + "-gui2");
        }
        context.getInput().setCursorPos(width - 2, 80);
        context.runOnClient(client -> {
            for (var category : view.categories()) click(client.gui.screen(), "expand:" + category.id(), 0);
            for (var raid : view.raids()) if (client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .anyMatch(value -> value.getMessage().getString().equals(raid.open()))) throw new AssertionError("Collapsed category kept a clickable raid");
            if (!sent.isEmpty()) throw new AssertionError("Category collapse emitted a server request");
        });
        context.waitTicks(2); context.takeScreenshot("hoi-raids-categories-collapsed-" + width + "-gui2");
        context.runOnClient(client -> {
            for (var category : view.categories()) {
                click(client.gui.screen(), "expand:" + category.id(), 1);
                if (client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                        .anyMatch(value -> view.raids().stream().anyMatch(raid -> raid.open().equals(value.getMessage().getString()))))
                    throw new AssertionError("Right click expanded raid category");
            }
            var screen = (RaidScreen)client.gui.screen();
            for (var category : view.categories()) if (!category.launchAll().enabled()) screen.call(category.launchAll().item(), List.of());
            if (!sent.isEmpty()) throw new AssertionError("Disabled category launch emitted request");
        });
        var launchJson = com.google.gson.JsonParser.parseString(read(directory, "kor-raids-launchable")).getAsJsonObject();
        launchJson.remove("window"); launchJson.remove("setup");
        var launchable = new RaidPayloads.Response(launchJson.toString()).view();
        if (launchable.categories().stream().noneMatch(category -> category.launchAll().enabled())) throw new AssertionError("Launchable fixture has no enabled category");
        for (var category : launchable.categories()) {
            context.setScreen(() -> new RaidScreen(launchable.session(), launchable, sent::add)); context.waitTicks(2);
            var point = context.computeOnClient(client -> {
                var b = button(client.gui.screen(), category.launchAll().item());
                if (b.active != category.launchAll().enabled()) throw new AssertionError("Launchable category availability differs");
                return new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
            });
            context.getInput().setCursorPos(point[0] * 2, point[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-raids-launch-" + (category.launchAll().enabled() ? "enabled" : "disabled") + "-" + width + "-gui2");
            context.runOnClient(client -> {
                sent.clear(); click(client.gui.screen(), category.launchAll().item(), 1);
                if (!sent.isEmpty()) throw new AssertionError("Right click launched category");
                sent.clear(); click(client.gui.screen(), category.launchAll().item(), 0); click(client.gui.screen(), category.launchAll().item(), 0);
                if (!category.launchAll().enabled()) {
                    if (!sent.isEmpty()) throw new AssertionError("Disabled category launched");
                } else if (sent.size() != 1 || !sent.getFirst().item().equals(category.launchAll().item()) || sent.getFirst().revision() != launchable.revision())
                    throw new AssertionError("Category launch lost scope/revision or duplicate lock");
            });
        }
    }
    public static void run(ClientGameTestContext context) {
        String directory = System.getenv("HOI_AGENCY_OPERATION_PREVIEW");
        if (directory == null || !Files.isRegularFile(Path.of(directory, "kor-raids-map.json"))) {
            if (System.getProperty("hoi.testSuite", "all").equals("raids")) throw new AssertionError("Validated raid input required");
            return;
        }
        var previous = context.computeOnClient(client -> ClientCampaignHud.country().isEmpty() ? HudPayloads.State.HIDDEN : HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()));
        var previousScale = context.computeOnClient(client -> client.options.guiScale().get());
        context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of("KOR", ClientCampaignHud.hud())));
        try {
            RaidMapTest.run(context, directory);
            for (int width : new int[]{2560, 1280}) {
                context.getInput().resizeWindow(width, width * 9 / 16);
                context.runOnClient(client -> client.options.guiScale().set(2));
                categories(context, directory, width);
                for (String state : List.of("map", "setup", "create", "organizing", "preparing", "prepared", "in-progress", "ended")) {
                    var view = new RaidPayloads.Response(read(directory, "kor-raids-" + state)).view();
                    var sent = new ArrayList<RaidPayloads.Request>();
                    context.setScreen(() -> new RaidScreen(view.session(), view, sent::add));
                    context.runOnClient(client -> {
                        if (client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                                .anyMatch(b -> b.getMessage().getString().startsWith("show:"))) throw new AssertionError("Removed raid filter control remains");
                    });
                    context.getInput().setCursorPos(width - 2, 80); context.waitTicks(3);
                    context.takeScreenshot("hoi-raids-" + state + "-" + width + "-gui2");
                    if (state.equals("create")) {
                        for (int step = 1; step <= 5; step++) {
                            context.waitTicks(30);
                            context.takeScreenshot("hoi-raids-create-name-scroll-" + step + "-" + width + "-gui2");
                        }
                    }
                    if (state.equals("setup")) {
                        context.runOnClient(client -> {
                            var screen = client.gui.screen();
                            screen.mouseScrolled(40, screen.height / 2.0, 0, -100);
                            var last = view.setup().units().getLast().select();
                            if (!button(screen, last).active) throw new AssertionError("Raid last unit not reachable by scrolling");
                        });
                        context.waitTicks(2);
                        context.takeScreenshot("hoi-raids-units-bottom-" + width + "-gui2");
                        context.runOnClient(client -> click(client.gui.screen(), "sources", 0)); context.waitTicks(2);
                        context.takeScreenshot("hoi-raids-sources-" + width + "-gui2");
                        context.runOnClient(client -> {
                            client.gui.setScreen(new RaidScreen(view.session(), view, sent::add)); sent.clear();
                            click(client.gui.screen(), "units", 0);
                            var id = view.setup().units().getFirst().select();
                            if (!ClientCampaignHud.country().equals(view.country()) || !button(client.gui.screen(), id).active) throw new AssertionError("Raid fixture authority/button changed: " + ClientCampaignHud.country() + " active=" + button(client.gui.screen(), id).active);
                            click(client.gui.screen(), id, 1);
                            if (!sent.isEmpty()) throw new AssertionError("Raid right click emitted a request: " + sent);
                            click(client.gui.screen(), id, 0); click(client.gui.screen(), id, 0);
                            if (sent.size() != 1 || !sent.getFirst().item().equals(id) || sent.getFirst().revision() != view.revision()) throw new AssertionError("Raid unit selection lost revision or request lock: " + sent + " expected " + id + " @ " + view.revision());
                        });
                    }
                    if (state.equals("create")) {
                        var point = context.computeOnClient(client -> {
                            var b = button(client.gui.screen(), "create"); return new int[]{b.getX() + 20, b.getY() - 125};
                        });
                        context.getInput().setCursorPos(point[0] * 2, point[1] * 2); context.waitTicks(2);
                        context.takeScreenshot("hoi-raids-chance-tooltip-" + width + "-gui2");
                        context.runOnClient(client -> {
                            var screen = client.gui.screen();
                            var create = button(screen, "create");
                            int x = create.getX(), y = create.getY();
                            var press = new MouseButtonEvent(x + 20, y - 250, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                            var move = new MouseButtonEvent(press.x() + 12, press.y() + 8, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                            screen.mouseClicked(press, false);
                            screen.mouseDragged(move, 12, 8);
                            screen.mouseReleased(move);
                            if (button(screen, "create").getX() != x + 12 || button(screen, "create").getY() != y + 8) throw new AssertionError("Raid window did not follow header drag");
                        });
                        context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
                        context.takeScreenshot("hoi-raids-dragged-" + width + "-gui2");
                        context.runOnClient(client -> {
                            client.gui.setScreen(new RaidScreen(view.session(), view, sent::add)); sent.clear();
                            var screen = (RaidScreen)client.gui.screen();
                            screen.call("unissued", List.of()); screen.call("create", List.of("invalid"));
                            if (!sent.isEmpty()) throw new AssertionError("Raid emitted unissued action");
                            click(screen, "create", 0); click(screen, "create", 0);
                            if (sent.size() != 1 || !sent.getFirst().item().equals("create")) throw new AssertionError("Raid create repeated request");
                            screen.update(view);
                            if (button(screen, "create").active) throw new AssertionError("Stale response cleared raid pending action");
                            ClientCampaignHud.accept(HudPayloads.State.HIDDEN);
                            screen.tick();
                            if (client.gui.screen() != null || screen.snapshot() != null) throw new AssertionError("Raid private snapshot survived authority revocation");
                            ClientCampaignHud.accept(HudPayloads.State.of("KOR", previous.hud()));
                        });
                    }
                    if (state.equals("prepared")) context.runOnClient(client -> {
                        client.gui.setScreen(new RaidScreen(view.session(), view, sent::add)); sent.clear();
                        var screen = (RaidScreen)client.gui.screen();
                        screen.call(view.window().start().item(), List.of());
                        if (!sent.isEmpty()) throw new AssertionError("Disabled raid start emitted request");
                    });
                    if (state.equals("organizing")) {
                        var disabledJson = com.google.gson.JsonParser.parseString(read(directory, "kor-raids-organizing")).getAsJsonObject();
                        var disabledWindow = disabledJson.getAsJsonObject("window");
                        disabledWindow.addProperty("autoEnabled", false); disabledWindow.addProperty("autoSelected", "");
                        disabledWindow.getAsJsonObject("auto").addProperty("enabled", false);
                        var disabled = new RaidPayloads.Response(disabledJson.toString()).view();
                        context.setScreen(() -> new RaidScreen(disabled.session(), disabled, sent::add));
                        context.runOnClient(client -> {
                            sent.clear();
                            click(client.gui.screen(), "auto-options", 0);
                            if (client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b -> b.getMessage().getString().startsWith("auto:"))) throw new AssertionError("Disabled auto dropdown opened");
                            click(client.gui.screen(), view.window().toggleAuto().item(), 1);
                            if (!sent.isEmpty()) throw new AssertionError("Right click toggled auto launch");
                            click(client.gui.screen(), view.window().toggleAuto().item(), 0);
                            click(client.gui.screen(), view.window().toggleAuto().item(), 0);
                            if (sent.size() != 1 || !sent.getFirst().item().equals(view.window().toggleAuto().item())) throw new AssertionError("Auto toggle lost request lock");
                        });
                        var enabledJson = com.google.gson.JsonParser.parseString(read(directory, "kor-raids-organizing")).getAsJsonObject();
                        var enabledWindow = enabledJson.getAsJsonObject("window");
                        enabledWindow.addProperty("autoEnabled", true);
                        enabledWindow.addProperty("autoSelected", view.window().autoOptions().stream().filter(option -> option.value().equals("HIGH")).findFirst().orElseThrow().value());
                        enabledWindow.getAsJsonObject("auto").addProperty("enabled", true);
                        var enabled = new RaidPayloads.Response(enabledJson.toString()).view();
                        context.setScreen(() -> new RaidScreen(enabled.session(), enabled, sent::add));
                        context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
                        context.takeScreenshot("hoi-raids-auto-closed-" + width + "-gui2");
                        context.runOnClient(client -> { sent.clear(); click(client.gui.screen(), "auto-options", 0); }); context.waitTicks(2);
                        context.takeScreenshot("hoi-raids-auto-options-" + width + "-gui2");
                        context.runOnClient(client -> {
                            client.gui.setScreen(new RaidScreen(enabled.session(), enabled, sent::add)); sent.clear();
                            click(client.gui.screen(), "auto-options", 0);
                            var choice = view.window().autoOptions().getFirst(); click(client.gui.screen(), "auto:" + choice.value(), 0);
                            if (sent.size() != 1 || !sent.getFirst().arguments().equals(List.of(choice.value()))) throw new AssertionError("Raid auto option lost server choice");
                        });
                    }
                }
                reportScroll(context, directory, width);
                sourceScenes(context, directory, width);
                var industry = new IndustryPayloads.Response(read(directory, "kor-industry-raid-priority")).view();
                context.setScreen(() -> new IndustryScreen(MenuTab.RECRUITMENT, industry.session(), industry, request -> {}));
                context.getInput().setCursorPos(width - 2, 80); context.waitTicks(3);
                context.takeScreenshot("hoi-raids-equipment-priority-" + width + "-gui2");
                for (String category : List.of("작전", "습격")) {
                    var point = context.computeOnClient(client -> {
                        var b = button(client.gui.screen(), category + " · 낮음 우선순위");
                        return new int[]{Math.max(10, b.getX() - 65), b.getY() + b.getHeight() / 2};
                    });
                    context.getInput().setCursorPos(point[0] * 2, point[1] * 2); context.waitTicks(2);
                    context.takeScreenshot("hoi-raids-priority-tooltip-" + (category.equals("작전") ? "operation" : "raid") + "-" + width + "-gui2");
                }
                var country = new CountryPayloads.Response(UUID.randomUUID().toString(), read(directory, "kor-diplomacy-JAP")).view();
                for (var domain : AgencyIntelDomain.values()) {
                    context.setScreen(() -> new DiplomacyCountryScreen(country)); context.waitTicks(2);
                    context.runOnClient(client -> { click(client.gui.screen(), "첩보 장부", 0); click(client.gui.screen(), domain.label + " 정보", 0); });
                    context.waitTicks(2); context.takeScreenshot("hoi-intel-high-" + domain.name().toLowerCase(Locale.ROOT) + "-" + width + "-gui2");
                    if (domain == AgencyIntelDomain.AIR) {
                        context.runOnClient(client -> {
                            try {
                                var clock = DiplomacyCountryScreen.class.getDeclaredField("ledgerOpenedAt"); clock.setAccessible(true);
                                long opened = clock.getLong(client.gui.screen());
                                if (net.minecraft.util.Util.getMillis() - opened > 1000) throw new AssertionError("Ledger did not start its own scroll clock");
                                clock.setLong(client.gui.screen(), net.minecraft.util.Util.getMillis() - 2000);
                                client.gui.screen().init(client.gui.screen().width, client.gui.screen().height);
                                if (clock.getLong(client.gui.screen()) == opened) throw new AssertionError("Ledger rebuild reset its scroll clock");
                            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                        });
                        context.waitTicks(2); context.takeScreenshot("hoi-intel-high-air-scroll-" + width + "-gui2");
                        context.runOnClient(client -> { click(client.gui.screen(), "외교", 0); click(client.gui.screen(), "첩보 장부", 0); });
                        context.waitTicks(2); context.takeScreenshot("hoi-intel-high-air-reopened-" + width + "-gui2");
                    }
                    if (domain != AgencyIntelDomain.CIVILIAN) {
                        context.runOnClient(client -> client.gui.screen().mouseScrolled(100, client.gui.screen().height - 80, 0, -100));
                        context.getInput().setCursorPos(width - 2, 80); context.waitTicks(2);
                        context.takeScreenshot("hoi-intel-high-bottom-" + domain.name().toLowerCase(Locale.ROOT) + "-" + width + "-gui2");
                    }
                }
            }
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> { ClientCampaignHud.accept(previous); client.options.guiScale().set(previousScale); });
        }
    }
    private static void sourceScenes(ClientGameTestContext context, String directory, int width) {
        if (!Files.isRegularFile(Path.of(directory, "prc-raids-create.json"))) return;
        var previous = context.computeOnClient(client -> HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()));
        context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of("PRC", previous.hud())));
        try {
            for (String state : List.of("setup", "create", "organizing", "preparing", "prepared", "launchable", "in-progress", "ended", "ended-success")) {
                var view = new RaidPayloads.Response(read(directory, "prc-raids-" + state)).view();
                context.setScreen(() -> new RaidScreen(view.session(), view, request -> {}));
                context.getInput().setCursorPos(width - 2, 80); context.waitTicks(3);
                context.takeScreenshot("hoi-raids-prc-" + state + "-" + width + "-gui2");
                if (state.equals("setup")) {
                    context.runOnClient(client -> click(client.gui.screen(), "sources", 0)); context.waitTicks(2);
                    context.takeScreenshot("hoi-raids-prc-sources-" + width + "-gui2");
                    context.runOnClient(client -> {
                        var screen = client.gui.screen();
                        var first = button(screen, view.setup().sources().getFirst().select());
                        var fifth = button(screen, view.setup().sources().get(4).select());
                        if (fifth.getY() != first.getY() + 124 || fifth.getHeight() != 21
                                || fifth.isMouseOver(fifth.getX() + 5, first.getY() + 145)) throw new AssertionError("Partial source row escaped list clipping");
                        screen.mouseScrolled(40, first.getY() + 30, 0, -1);
                    });
                    context.waitTicks(2);
                    context.takeScreenshot("hoi-raids-prc-sources-scrolled-" + width + "-gui2");
                }
                if (state.equals("preparing")) {
                    var origin = context.computeOnClient(client -> {
                        var b = button(client.gui.screen(), view.window().cancel().item());
                        return new int[]{b.getX() - 242, b.getY() - 269};
                    });
                    for (var tip : List.of(new Object[]{"chance", 80, 146}, new Object[]{"intel", 230, 249}, new Object[]{"auto", 148, 277})) {
                        context.getInput().setCursorPos((origin[0] + (int)tip[1]) * 2, (origin[1] + (int)tip[2]) * 2); context.waitTicks(2);
                        context.takeScreenshot("hoi-raids-prc-" + tip[0] + "-tooltip-" + width + "-gui2");
                    }
                }
            }
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> ClientCampaignHud.accept(previous));
        }
    }
}
