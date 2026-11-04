package dev.hoi.client.screen;

import com.google.gson.JsonParser;
import dev.hoi.client.ui.UiKeywordIcons;
import dev.hoi.client.screen.decision.DecisionScreen;
import dev.hoi.client.screen.decision.DecisionScreenSourceTest;
import dev.hoi.client.screen.dialog.DialogCompletion;
import dev.hoi.client.screen.politics.PoliticsChoice;
import dev.hoi.protocol.decision.*;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.menu.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;

public final class ScreenCampaignSourceTest {
    private ScreenCampaignSourceTest() {}
    public static void run(ClientGameTestContext context) {
        var root = Path.of(System.getenv("HOI_CAMPAIGN_PREVIEW"));
        int previous = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var completion = new DialogPayloads.Show(read(root.resolve("cmp80/kor-focus-complete-epidemiological-investigation.json")), true).view();
                if (!completion.title().equals("역학조사 실시") || !completion.presentation().equals("focus_complete")) throw new AssertionError("Expected the actual epidemiological investigation completion");
                context.setScreen(() -> new DialogCompletion(completion, (view, choice) -> { throw new AssertionError("Rendering must not acknowledge completion"); }));
                var completionCapture = capture(context, "completion", size);
                var titleOrigin = context.computeOnClient(client -> {
                    var screen = (DialogCompletion)client.gui.screen();
                    return new int[]{(screen.panelLeft()+35)*2,(screen.panelTop()+83)*2};
                });
                try {
                    var pixels = javax.imageio.ImageIO.read(completionCapture.toFile());
                    int outline=0,gold=0;
                    for(int y=titleOrigin[1];y<titleOrigin[1]+24;y++)for(int x=titleOrigin[0];x<titleOrigin[0]+360;x++){
                        int rgb=pixels.getRGB(x,y)&0xffffff;
                        if(rgb==0x16110b)outline++;
                        if(rgb==0xffbd00)gold++;
                    }
                    if(outline<40||gold<40)throw new AssertionError("Actual focus completion needs outlined orange-gold title pixels: "+outline+"/"+gold);
                } catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
                var menu = new MenuPayloads.OpenScreen(read(root.resolve("commander-source-fixtures/kor-selected-menu.json"))).view();
                for (String role : List.of("minister", "interior", "intelligence")) {
                    var view = new DialogPayloads.Show(read(root.resolve("politics-choice-fixtures/kor-" + role + "-ready.json")), true).view();
                    context.setScreen(() -> new PoliticsChoice(view, new ScreenMenu(menu, MenuTab.POLITICS, () -> {})));
                    capture(context, role, size);
                    context.runOnClient(client -> {
                        for (var tile : view.tiles()) {
                            String source = String.join("\n", PoliticsChoice.traits(tile));
                            if (source.contains("£GFX_KOR_DemocraticParty_texticon")) {
                                var rendered = UiKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal(source), false);
                                if (rendered.getString().contains("£") || rendered.getString().codePoints().noneMatch(c -> c >= 0xE000 && c <= 0xF8FF)) throw new AssertionError("Minister source party token must become an inline glyph");
                            }
                        }
                    });
                    var point = context.computeOnClient(client -> {
                        var button = (Button)client.gui.screen().children().get(1);
                        return new int[]{button.getX() + 5, button.getY() + 5};
                    });
                    context.getInput().setCursorPos(point[0] * 2, point[1] * 2);
                    context.waitTicks(5);
                    context.takeScreenshot("hoi-campaign-" + role + "-tooltip-" + size[0] + "-gui2");
                }
                for (int day : List.of(72, 504)) {
                    var pages = JsonParser.parseString(read(root.resolve("cmp80/kor-decisions-day" + day + "-pages.json"))).getAsJsonArray();
                    var first = new DecisionPayloads.Response(java.util.UUID.randomUUID().toString(), pages.get(0).toString()).view();
                    context.setScreen(() -> new DecisionScreen(first, request -> {}));
                    context.runOnClient(client -> {
                        var screen = (DecisionScreen)client.gui.screen();
                        for (int page = 1; page < pages.size(); page++) screen.update(new DecisionPayloads.Response(screen.token(), pages.get(page).toString()));
                    });
                    capture(context, "decisions-day" + day, size);
                    try (var paths = Files.list(root.resolve("cmp80"))) {
                        for (var path : paths.filter(p -> p.getFileName().toString().startsWith("kor-decision-detail-day" + day + "-")).sorted().toList()) {
                            String json = read(path);
                            var detail = new DecisionDetailPayloads.Response(java.util.UUID.randomUUID().toString(), json).view();
                            if (!detail.limitation().isEmpty()) throw new AssertionError("Source detail must not add unsupported-feature prose");
                            context.getInput().setCursorPos(size[0] - 2, size[1] - 2);
                            context.waitTicks(2);
                            var point = context.computeOnClient(client -> {
                                var screen = (DecisionScreen)client.gui.screen();
                                var location = DecisionScreenSourceTest.locate(screen, detail.name());
                                accept(screen, json, detail.id());
                                return location;
                            });
                            context.getInput().setCursorPos(point[0] * 2, point[1] * 2);
                            context.waitTicks(5);
                            context.takeScreenshot("hoi-campaign-day" + day + "-" + detail.id() + "-" + size[0] + "-gui2");
                        }
                    }
                }
            }
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> client.options.guiScale().set(previous));
        }
    }
    @SuppressWarnings("unchecked")
    private static void accept(DecisionScreen screen, String json, String id) {
        try {
            var requests = DecisionScreen.class.getDeclaredField("detailRequests");
            requests.setAccessible(true);
            ((Map<String, Integer>)requests.get(screen)).put(id, 0);
            var response = new DecisionDetailPayloads.Response(screen.token(), json);
            screen.update(response);
            var details = DecisionScreen.class.getDeclaredField("details");
            details.setAccessible(true);
            if (!response.view().equals(((Map<?, ?>)details.get(screen)).get(id))) throw new AssertionError("Matching source detail must be accepted: " + id);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
    private static Path capture(ClientGameTestContext context, String name, int[] size) {
        context.getInput().setCursorPos(size[0] - 2, size[1] - 2);
        context.waitTicks(3);
        return context.takeScreenshot("hoi-campaign-" + name + "-" + size[0] + "-gui2");
    }
    private static String read(Path path) {
        try { return Files.readString(path); }
        catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
}
