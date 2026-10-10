package dev.hoi.client.screen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.protocol.ProtocolCountryHud;
import dev.hoi.protocol.ProtocolDecisionPanel;
import dev.hoi.protocol.ProtocolDecisionPayloads;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;

public final class ScreenDecisionPanelTest {
    private static final Gson JSON = new Gson();
    private record Document(JsonObject root, String sha256) {}
    private record Fixture(String id, String country, ProtocolDecisionPanel panel) {}
    private record Assets(JsonObject root, Map<String, String> sprites) {}
    private record Probe(int top, int width, ScreenDecisionPanel.Result result) {}
    private record Evidence(String id, int width, int height, String fixtureSha256, int panelHeight,
            List<ScreenDecisionPanel.Hit> hits, Map<String, String> screenshots) {}
    private ScreenDecisionPanelTest() {}
    public static void run(ClientGameTestContext context, ProtocolCountryHud hud) {
        var original = context.computeOnClient(client -> new int[]{client.options.guiScale().get(),
                client.getWindow().getScreenWidth(), client.getWindow().getScreenHeight()});
        var source = fixtureDocument();
        var document = source.root();
        var fixtures = new ArrayList<Fixture>();
        for (var value : document.getAsJsonArray("panels")) {
            var item = value.getAsJsonObject();
            fixtures.add(new Fixture(item.get("id").getAsString(), item.get("country").getAsString(),
                    JSON.fromJson(item.get("panel"), ProtocolDecisionPanel.class)));
        }
        require(fixtures.stream().map(Fixture::id).collect(java.util.stream.Collectors.toSet())
                .equals(Set.of("inner_circle", "national_assembly", "ldp")), "Three original panel fixtures are required");
        String fixtureHash = source.sha256();
        var evidence = new ArrayList<Evidence>();
        try {
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.waitTicks(3);
                for (var fixture : fixtures) {
                    String session = UUID.randomUUID().toString();
                    var requests = new ArrayList<ProtocolDecisionPayloads.Request>();
                    context.setScreen(() -> new ScreenDecision(view(fixture, fixture.panel(), session, 1, hud), requests::add));
                    context.getInput().setCursorPos(2, 2);
                    context.waitTicks(3);
                    var assets = context.computeOnClient(ScreenDecisionPanelTest::assets);
                    var probe = context.computeOnClient(client -> probe(client, fixture, assets));
                    sourceGeometry(fixture, probe);
                    var screenshots = new LinkedHashMap<String, String>();
                    String suffix = fixture.id() + "-" + size[0] + "x" + size[1] + "-gui2";
                    screenshots.put("panel", screenshot(context, "hoi-decision-panel-" + suffix, size));
                    for (int hitIndex = 0; hitIndex < probe.result().hits().size(); hitIndex++) {
                        var hit = probe.result().hits().get(hitIndex);
                        int[] point = context.computeOnClient(client -> hoverPoint((ScreenDecision)client.gui.screen(), hit));
                        context.getInput().setCursorPos(point[0] * 2, point[1] * 2);
                        context.waitTicks(2);
                        context.runOnClient(client -> {
                            var screen = (ScreenDecision)client.gui.screen();
                            require(field(screen, "hover").equals("panel/" + fixture.id() + "/" + hit.id()), "Source panel hover missing: " + hit.id());
                            require(field(screen, "tooltipBounds") != null, "Source panel tooltip not rendered: " + hit.id());
                        });
                        screenshots.put(hit.id() + "/" + hitIndex, screenshot(context, "hoi-decision-panel-hover-" + hit.id().toLowerCase(java.util.Locale.ROOT) + "-" + hitIndex + "-" + suffix, size));
                    }
                    context.getInput().setCursorPos(2, 2);
                    context.runOnClient(client -> resetScroll((ScreenDecision)client.gui.screen()));
                    context.waitTicks(2);
                    if (fixture.id().equals("national_assembly")) {
                        var before = context.computeOnClient(client -> textures((ScreenDecision)client.gui.screen()));
                        require(before.size() == 5, "All five original radial layers must upload real textures");
                        context.waitTicks(3);
                        require(before.equals(context.computeOnClient(client -> textures((ScreenDecision)client.gui.screen()))), "Unchanged frames must reuse textures");
                        context.runOnClient(client -> ScreenDecision.resourcesReloaded());
                        context.waitTicks(3);
                        var after = context.computeOnClient(client -> textures((ScreenDecision)client.gui.screen()));
                        require(after.keySet().equals(before.keySet()) && before.values().stream().noneMatch(after::containsValue), "Resource reload must replace cached radial textures");
                        var reloaded = context.computeOnClient(client -> probe(client, fixture, assets(client)));
                        require(probe.result().equals(reloaded.result()), "Resource reload changed source panel geometry or tooltips");
                        screenshots.put("reloaded", screenshot(context, "hoi-decision-panel-reloaded-" + suffix, size));
                        var unknown = withoutFrames(fixture.panel());
                        context.runOnClient(client -> {
                            var screen = (ScreenDecision)client.gui.screen();
                            screen.update(ProtocolDecisionPayloads.Response.of(screen.token(), view(fixture, unknown, session, 2, hud)));
                        });
                        context.waitTicks(3);
                        require(after.equals(context.computeOnClient(client -> textures((ScreenDecision)client.gui.screen()))), "Unknown frames must not upload a fabricated zero frame");
                        screenshots.put("unknown", screenshot(context, "hoi-decision-panel-unknown-" + suffix, size));
                        require(changedPixels(Path.of(screenshots.get("reloaded")), Path.of(screenshots.get("unknown")), probe) > 20,
                                "Unknown radial frames must replace the previously visible source layers");
                    }
                    require(requests.isEmpty(), "Panel rendering and hover must not issue gameplay requests");
                    evidence.add(new Evidence(fixture.id(), size[0], size[1], fixtureHash, probe.result().height(), probe.result().hits(), Map.copyOf(screenshots)));
                    Path report = Path.of(screenshots.get("panel")).getParent().resolve("hoi-decision-panels-evidence.json");
                    Files.writeString(report, JSON.toJson(evidence), StandardCharsets.UTF_8);
                    var closed = context.computeOnClient(client -> (ScreenDecision)client.gui.screen());
                    context.setScreen(() -> null);
                    context.runOnClient(client -> require(textures(closed).isEmpty(), "Closed panel must release its radial cache"));
                }
            }
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> client.options.guiScale().set(original[0]));
            context.getInput().resizeWindow(original[1], original[2]);
            context.waitTicks(3);
        }
    }
    private static Document fixtureDocument() {
        String directory = System.getenv("HOI_DECISION_PREVIEW");
        require(directory != null && !directory.isBlank(), "HOI_DECISION_PREVIEW is required for server-exported decision panel fixtures");
        try {
            byte[] bytes = Files.readAllBytes(Path.of(directory).resolve("decision-panels.json"));
            var root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            require(root.get("version").getAsInt() == 1 && root.get("fixture").getAsBoolean(), "Source panel fixture metadata required");
            require(root.get("catalogSha256").getAsString().matches("[a-f0-9]{64}"), "Source catalog hash required");
            require(!root.getAsJsonObject("sourceHashes").isEmpty(), "Original GUI source hashes required");
            for (var entry : root.getAsJsonObject("sourceHashes").entrySet()) require(entry.getValue().getAsString().matches("[a-f0-9]{64}"), "Invalid source hash: " + entry.getKey());
            return new Document(root, hash(bytes));
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
    private static ProtocolDecisionPayloads.View view(Fixture fixture, ProtocolDecisionPanel panel, String session, long revision, ProtocolCountryHud hud) {
        var category = new ProtocolDecisionPayloads.Category(fixture.id(), fixture.id(), "", "", "", panel.scriptedGui(), true, panel);
        return new ProtocolDecisionPayloads.View(fixture.country(), session, revision, "", 0, 0, hud, List.of(), List.of(category), 1);
    }
    private static Assets assets(Minecraft client) {
        var resource = client.getResourceManager().getResource(Identifier.parse("hoi:ui/decision_assets.json")).orElseThrow();
        try (var input = resource.open()) {
            var root = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            var sprites = new HashMap<String, String>();
            for (var entry : root.getAsJsonObject("sprites").entrySet()) sprites.put(entry.getKey(), entry.getValue().getAsString());
            return new Assets(root, Map.copyOf(sprites));
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
    private static Probe probe(Minecraft client, Fixture fixture, Assets assets) {
        require(client.getWindow().getGuiScale() == 2, "Panel rendering requires GUI scale 2");
        var screen = (ScreenDecision)client.gui.screen();
        var button = screen.children().stream().filter(child -> child instanceof Button value && value.getMessage().getString().equals(fixture.id()))
                .map(child -> (Button)child).findFirst().orElseThrow();
        int top = button.getBottom() + 3, width = button.getWidth() - 10;
        var renderer = new ScreenDecisionPanel();
        var text = new ScreenDecisionText();
        renderer.assets(assets.root());
        text.assets(assets.root());
        try {
            return new Probe(top, width, renderer.render(null, client.font, fixture.panel(), assets.sprites(), text, 11, top, width));
        } finally {
            renderer.close();
        }
    }
    private static void sourceGeometry(Fixture fixture, Probe probe) {
        require(probe.result().height() > 0 && !probe.result().hits().isEmpty(), "Original panel is empty: " + fixture.id());
        for (var hit : probe.result().hits()) require(hit.width() > 0 && hit.height() > 0 && hit.x() >= 11 && hit.x() + hit.width() <= 12 + probe.width()
                && hit.y() >= probe.top() && hit.y() + hit.height() <= probe.top() + probe.result().height() + 1, "Source hover bounds exceed the panel: " + hit.id());
        switch (fixture.id()) {
            case "ldp" -> {
                require(fixture.panel().elements().getFirst().layout().width() == null, "LDP fixture must retain its absent root width");
                require(probe.result().height() == (int)Math.ceil(190.0 * probe.width() / 400), "Negative LDP offsets must form the original 400 by 190 canvas");
                for (var hit : probe.result().hits()) {
                    var element = elements(fixture.panel()).filter(value -> value.name().equals(hit.id())).findFirst().orElseThrow();
                    assertHit(hit, probe, 400, 5, element.layout().y() + 190, element.layout().width().value(), element.layout().height().value());
                }
                require(probe.result().hits().size() == 3, "Initial LDP source fixture must show three faction tooltips");
            }
            case "inner_circle" -> {
                require(fixture.panel().elements().getFirst().layout().width().value() == 480, "Inner circle source width must remain 480");
                require(probe.result().height() >= probe.width(), "Inner circle declared 480 by 480 canvas must survive");
                require(probe.result().hits().size() == 3, "All three original advisor trait tooltips are required");
                for (int index = 1; index <= 3; index++) {
                    var hit = hit(probe, "TFR_KOR_ascended_advisor_" + index + "_trait");
                    require(hit.x() == 11 + (int)Math.floor((6 + (index - 1) * 160) * probe.width() / 480.0), "Nested advisor anchor shifted: " + index);
                    require(hit.y() == probe.top() + (int)Math.floor(462 * probe.width() / 480.0), "Nested advisor vertical anchor shifted: " + index);
                }
            }
            case "national_assembly" -> {
                require(fixture.panel().elements().getFirst().layout().width().value() == 0, "Assembly fixture must retain its zero root width");
                require(probe.result().height() == (int)Math.ceil(570.0 * probe.width() / 490), "Zero-width assembly root must include its source quad extents");
                assertHit(hit(probe, "NatAsm_overlay"), probe, 490, 6, 139, 484, 257);
                assertHit(hit(probe, "NatAsm_logo"), probe, 490, 201, 298, 91, 91);
                require(elements(fixture.panel()).filter(value -> value.kind().equals("radial") && value.frame() != null).count() == 5, "Known source frame fixture must exercise all five original shaders");
            }
            default -> throw new AssertionError("Unknown panel fixture " + fixture.id());
        }
    }
    private static void assertHit(ScreenDecisionPanel.Hit hit, Probe probe, double sourceWidth, double x, double y, double width, double height) {
        double scale = probe.width() / sourceWidth;
        require(hit.x() == 11 + (int)Math.floor(x * scale) && hit.y() == probe.top() + (int)Math.floor(y * scale)
                && hit.width() == (int)Math.ceil(width * scale) && hit.height() == (int)Math.ceil(height * scale), "Original source hover rectangle changed: " + hit.id());
    }
    private static ScreenDecisionPanel.Hit hit(Probe probe, String name) {
        return probe.result().hits().stream().filter(value -> value.id().equals(name)).findFirst().orElseThrow();
    }
    private static Stream<ProtocolDecisionPanel.Element> elements(ProtocolDecisionPanel panel) {
        return walk(panel.elements());
    }
    private static Stream<ProtocolDecisionPanel.Element> walk(List<ProtocolDecisionPanel.Element> elements) {
        return elements.stream().flatMap(element -> Stream.concat(Stream.of(element), walk(element.children())));
    }
    private static int[] hoverPoint(ScreenDecision screen, ScreenDecisionPanel.Hit hit) {
        resetScroll(screen);
        int target = hit.y() + Math.min(hit.height() / 2, 8);
        for (int step = 0; target - screen.scrollOffset() > screen.height - 20 && step < 30; step++) {
            int before = screen.scrollOffset();
            screen.mouseScrolled(3, UiMenuBar.height(screen.width) + 53, 0, -3);
            if (screen.scrollOffset() == before) break;
        }
        int y = target - screen.scrollOffset();
        require(y >= UiMenuBar.height(screen.width) + 52 && y < screen.height - 10, "Panel tooltip cannot be reached by scrolling: " + hit.id());
        return new int[]{hit.x() + Math.min(hit.width() / 2, 8), y};
    }
    private static void resetScroll(ScreenDecision screen) {
        screen.mouseScrolled(3, UiMenuBar.height(screen.width) + 53, 0, Integer.MAX_VALUE / 28.0);
        require(screen.scrollOffset() == 0, "Panel scroll must reset at the margin");
    }
    private static Map<String, String> textures(ScreenDecision screen) {
        var radial = field(field(screen, "panels"), "radial");
        var result = new HashMap<String, String>();
        var cache = (Map<?, ?>)field(radial, "cache");
        require(cache.size() <= 5, "Radial cache exceeded five source layers");
        cache.forEach((name, cached) -> {
            Object texture = field(cached, "texture");
            if (texture != null) result.put(name.toString(), texture.toString());
        });
        return Map.copyOf(result);
    }
    private static Object field(Object instance, String name) {
        try {
            var field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Missing panel observation field: " + name, error);
        }
    }
    private static ProtocolDecisionPanel withoutFrames(ProtocolDecisionPanel panel) {
        var value = JSON.toJsonTree(panel).getAsJsonObject();
        clearFrames(value.getAsJsonArray("elements"));
        return JSON.fromJson(value, ProtocolDecisionPanel.class);
    }
    private static void clearFrames(com.google.gson.JsonArray elements) {
        for (var value : elements) {
            var element = value.getAsJsonObject();
            if (element.get("kind").getAsString().equals("radial")) element.add("frame", com.google.gson.JsonNull.INSTANCE);
            clearFrames(element.getAsJsonArray("children"));
        }
    }
    private static String screenshot(ClientGameTestContext context, String name, int[] size) throws IOException {
        Path path = context.takeScreenshot(name);
        var image = javax.imageio.ImageIO.read(path.toFile());
        require(image != null && image.getWidth() == size[0] && image.getHeight() == size[1], "Screenshot dimensions changed: " + path);
        return path.toString();
    }
    private static int changedPixels(Path before, Path after, Probe probe) throws IOException {
        var one = javax.imageio.ImageIO.read(before.toFile());
        var two = javax.imageio.ImageIO.read(after.toFile());
        int changed = 0, right = Math.min(one.getWidth(), (11 + probe.width()) * 2);
        int bottom = Math.min(one.getHeight() - 22, (probe.top() + probe.result().height()) * 2);
        for (int y = probe.top() * 2; y < bottom; y++) for (int x = 22; x < right; x++) if (one.getRGB(x, y) != two.getRGB(x, y)) changed++;
        return changed;
    }
    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
