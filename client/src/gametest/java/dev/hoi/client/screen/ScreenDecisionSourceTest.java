package dev.hoi.client.screen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.protocol.ProtocolDecisionPayloads;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;

public final class ScreenDecisionSourceTest {
    private static final List<String> FIXTURES = List.of("prc", "kor", "jap", "prc-active", "prc-restored", "prc-completed");
    private static final List<String> HOVER_IDS = List.of("PRC_advocate_for_militarization", "PRC_hold_pro_government_rally");
    private static final Pattern BODY_IMAGE = Pattern.compile("£([A-Za-z0-9_]+)£?");
    private record Preview(String name, List<String> pages, List<ProtocolDecisionPayloads.Row> rows,
            List<ProtocolDecisionPayloads.Category> categories, JsonObject metadata, String sha256) {}
    private record MissingSourceAsset(String owner, String field, String sourceName, String fallback) {}
    private record Evidence(String fixture, int width, int height, int guiScale, int rowCount, int categoryCount,
            int declaredAssets, String fixtureSha256, JsonObject metadata, Set<String> observedNames,
            List<MissingSourceAsset> missingSourceAssets, Map<String, String> screenshots) {}
    private ScreenDecisionSourceTest() {}
    public static void run(ClientGameTestContext context) {
        String directory = System.getenv("HOI_DECISION_PREVIEW");
        if (directory == null || directory.isBlank())
            throw new AssertionError("HOI_DECISION_PREVIEW is required for server-exported decision source fixtures");
        var original = context.computeOnClient(client -> new int[]{client.options.guiScale().get(),
                client.getWindow().getScreenWidth(), client.getWindow().getScreenHeight()});
        var evidence = new ArrayList<Evidence>();
        try {
            var previews = new ArrayList<Preview>();
            for (String name : FIXTURES) previews.add(read(Path.of(directory), name));
            var assets = context.computeOnClient(ScreenDecisionSourceTest::assets);
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.waitTicks(3);
                for (var preview : previews) {
                    var requests = new ArrayList<ProtocolDecisionPayloads.Request>();
                    context.setScreen(() -> new ScreenDecision(null, requests::add));
                    context.runOnClient(client -> {
                        var screen = (ScreenDecision) client.gui.screen();
                        for (String page : preview.pages()) screen.update(new ProtocolDecisionPayloads.Response(screen.token(), page));
                        if (screen.rowCount() != preview.rows().size() || screen.pending())
                            throw new AssertionError("Incomplete source decision snapshot: " + preview.name());
                        if (client.getWindow().getGuiScale() != 2)
                            throw new AssertionError("Source decision rendering requires actual GUI scale 2");
                        for (var request : requests) if (request.action() != ProtocolDecisionPayloads.Action.REFRESH
                                || request.page() <= 0 || request.page() >= preview.pages().size())
                            throw new AssertionError("Unexpected source preview request: " + request);
                    });
                    margin(context);
                    var screenshots = new LinkedHashMap<String, String>();
                    String suffix = preview.name() + "-" + size[0] + "x" + size[1] + "-gui2";
                    screenshots.put("top", screenshot(context, "hoi-decisions-source-" + suffix, size));
                    var observed = context.computeOnClient(client -> inspectNames((ScreenDecision) client.gui.screen(), preview));
                    context.waitTicks(2);
                    screenshots.put("bottom", screenshot(context, "hoi-decisions-source-bottom-" + suffix, size));
                    for (String id : HOVER_IDS) {
                        var row = preview.rows().stream().filter(candidate -> candidate.id().equals(id)).findFirst();
                        if (row.isEmpty()) continue;
                        if (preview.rows().stream().filter(candidate -> candidate.name().equals(row.get().name())).count() != 1)
                            throw new AssertionError("Source tooltip row name is ambiguous: " + id);
                        margin(context);
                        int[] point = context.computeOnClient(client -> locate((ScreenDecision) client.gui.screen(), row.get().name()));
                        context.getInput().setCursorPos(point[0] * 2, point[1] * 2);
                        context.waitTicks(3);
                        screenshots.put(id, screenshot(context, "hoi-decisions-source-hover-" + id.toLowerCase(Locale.ROOT) + "-" + suffix, size));
                    }
                    var missing = context.computeOnClient(client -> missingAssets(client, preview, assets));
                    evidence.add(new Evidence(preview.name(), size[0], size[1], 2, preview.rows().size(), preview.categories().size(),
                            assets.size(), preview.sha256(), preview.metadata(), observed, missing, Map.copyOf(screenshots)));
                    Path report = Path.of(evidence.getFirst().screenshots().get("top")).getParent().resolve("hoi-decisions-source-evidence.json");
                    Files.writeString(report, new Gson().toJson(evidence), StandardCharsets.UTF_8);
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
    private static Preview read(Path directory, String name) throws IOException {
        byte[] bytes = Files.readAllBytes(directory.resolve(name + "-pages.json"));
        var raw = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonArray();
        var metadata = JsonParser.parseString(Files.readString(directory.resolve(name + "-metadata.json"))).getAsJsonObject();
        if (metadata.get("formatVersion").getAsInt() != 1 || !metadata.get("fixture").getAsBoolean()
                || !metadata.get("catalogSha256").getAsString().matches("[a-f0-9]{64}")
                || metadata.getAsJsonObject("sourceHashes").isEmpty() || metadata.get("catalogResource").getAsString().isBlank())
            throw new AssertionError("Source-backed sandbox fixture metadata required: " + name);
        var pages = new ArrayList<String>();
        var rows = new LinkedHashMap<String, ProtocolDecisionPayloads.Row>();
        var categories = new LinkedHashMap<String, ProtocolDecisionPayloads.Category>();
        var sourceIds = new ArrayList<String>();
        metadata.getAsJsonArray("sourceDecisionIds").forEach(value -> sourceIds.add(value.getAsString()));
        ProtocolDecisionPayloads.View first = null;
        for (var element : raw) {
            String json = element.toString();
            var view = new ProtocolDecisionPayloads.Response("00000000-0000-0000-0000-000000000000", json).view();
            if (first == null) first = view;
            if (view.page() != pages.size() || view.pageCount() != raw.size() || !view.country().equals(first.country())
                    || !view.session().equals(first.session()) || view.revision() != first.revision() || view.total() != first.total()
                    || !view.hud().equals(first.hud()) || !view.message().equals(first.message()))
                throw new AssertionError("Source fixture pages disagree: " + name);
            pages.add(json);
            for (var row : view.rows()) if (rows.putIfAbsent(row.id(), row) != null || !sourceIds.contains(row.id()))
                throw new AssertionError("Duplicate or unproven source decision: " + name + "/" + row.id());
            for (var category : view.categories()) if (categories.putIfAbsent(category.id(), category) != null)
                throw new AssertionError("Duplicate source category: " + name + "/" + category.id());
        }
        if (first == null || first.total() != rows.size() || rows.size() != metadata.get("rowCount").getAsInt()
                || categories.size() != metadata.get("categoryCount").getAsInt() || pages.size() != metadata.get("pageCount").getAsInt()
                || !first.country().equals(metadata.get("country").getAsString())
                || !first.country().equals(name.split("-")[0].toUpperCase(Locale.ROOT))
                || !sourceIds.equals(List.copyOf(rows.keySet())))
            throw new AssertionError("Source fixture aggregate disagrees with server metadata: " + name);
        try {
            return new Preview(name, List.copyOf(pages), List.copyOf(rows.values()), List.copyOf(categories.values()), metadata,
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
    private static Map<String, String> assets(Minecraft client) {
        var resource = client.getResourceManager().getResource(Identifier.parse("hoi:ui/decision_assets.json"))
                .orElseThrow(() -> new AssertionError("Missing original decision asset manifest"));
        try (var stream = resource.open()) {
            var root = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new AssertionError("Unsupported decision asset manifest");
            var result = new TreeMap<String, String>();
            for (var entry : root.getAsJsonObject("sprites").entrySet()) {
                String path = entry.getValue().getAsString();
                if (!path.matches("[a-z0-9_/.-]+") || path.contains("..") || !exists(client, path))
                    throw new AssertionError("Declared source decision asset is missing: " + entry.getKey() + " -> " + path);
                result.put(entry.getKey(), path);
            }
            if (result.isEmpty()) throw new AssertionError("Empty decision source asset manifest");
            for (String path : List.of("decision/category", "decision/row", "decision/row_disabled", "decision/timer",
                    "decision/select", "decision/category_generic", "menu/decisions"))
                if (!exists(client, path)) throw new AssertionError("Missing decision renderer fallback: " + path);
            return Map.copyOf(result);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
    private static List<MissingSourceAsset> missingAssets(Minecraft client, Preview preview, Map<String, String> assets) {
        var result = new ArrayList<MissingSourceAsset>();
        for (var category : preview.categories()) {
            missingAsset(client, assets, result, category.id(), "icon", category.icon(), "decision/category_generic");
            missingAsset(client, assets, result, category.id(), "picture", category.picture(), "omitted");
            for (var line : new ScreenDecisionText().rows(category.description())) {
                var match = BODY_IMAGE.matcher(line.getString());
                if (match.find() && line.getString().replace(match.group(), "").isBlank()
                        && !assets.containsKey(match.group(1)) && !assets.containsKey("GFX_" + match.group(1)))
                    result.add(new MissingSourceAsset(category.id(), "body", match.group(1), "source text"));
            }
        }
        for (var row : preview.rows()) {
            missingAsset(client, assets, result, row.id(), "icon", row.icon(), "menu/decisions");
            if (preview.categories().stream().noneMatch(category -> category.id().equals(row.category())))
                missingAsset(client, assets, result, row.category(), "icon", row.categoryIcon(), "decision/category_generic");
        }
        return List.copyOf(result);
    }
    private static void missingAsset(Minecraft client, Map<String, String> assets, List<MissingSourceAsset> result,
            String owner, String kind, String source, String fallback) {
        if (source.isEmpty() || assets.containsKey(source)) return;
        String path = source.matches("[A-Za-z0-9_]+") ? "decision/" + kind + "/" + source.toLowerCase(Locale.ROOT) : "";
        if (path.isEmpty() || !exists(client, path)) result.add(new MissingSourceAsset(owner, kind, source, fallback));
    }
    private static boolean exists(Minecraft client, String path) {
        return client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/" + path + ".png")).isPresent();
    }
    private static Set<String> inspectNames(ScreenDecision screen, Preview preview) {
        var text = new ScreenDecisionText();
        var expected = new TreeSet<String>();
        for (var category : preview.categories()) expected.add(text.component(category.name()).getString());
        for (var row : preview.rows()) {
            expected.add(text.component(row.name()).getString());
            if (preview.categories().stream().noneMatch(category -> category.id().equals(row.category())))
                expected.add(text.component(row.categoryName()).getString());
        }
        var observed = new TreeSet<String>();
        resetScroll(screen);
        for (int step = 0; step < 10_000; step++) {
            for (var child : screen.children()) if (child instanceof Button button && button.getY() >= UiMenuBar.height(screen.width) + 52) {
                if (button.getX() < 0 || button.getRight() > screen.width || button.getBottom() > screen.height - 10)
                    throw new AssertionError("Source decision control exceeds viewport: " + preview.name());
                observed.add(button.getMessage().getString());
            }
            if (!scroll(screen)) {
                var missing = new TreeSet<>(expected);
                missing.removeAll(observed);
                if (!missing.isEmpty()) throw new AssertionError("Source names unreachable by scrolling: " + preview.name() + " " + missing);
                return Set.copyOf(observed);
            }
        }
        throw new AssertionError("Source decision scrolling did not reach the end: " + preview.name());
    }
    private static int[] locate(ScreenDecision screen, String name) {
        String label = new ScreenDecisionText().component(name).getString();
        resetScroll(screen);
        for (int step = 0; step < 10_000; step++) {
            for (var child : screen.children()) if (child instanceof Button button && button.getX() == 7
                    && button.getY() >= UiMenuBar.height(screen.width) + 52 && button.getMessage().getString().equals(label))
                return new int[]{button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2};
            if (!scroll(screen)) break;
        }
        throw new AssertionError("Source tooltip row is unreachable: " + name);
    }
    private static boolean scroll(ScreenDecision screen) {
        int before = screen.scrollOffset();
        int top = UiMenuBar.height(screen.width) + 52;
        screen.mouseScrolled(3, top + 1, 0, -Math.max(28, screen.height - top - 74) / 28.0);
        return screen.scrollOffset() != before;
    }
    private static void resetScroll(ScreenDecision screen) {
        screen.mouseScrolled(3, UiMenuBar.height(screen.width) + 53, 0, Integer.MAX_VALUE / 28.0);
        if (screen.scrollOffset() != 0) throw new AssertionError("Source preview margin scroll was consumed by a tooltip");
    }
    private static void margin(ClientGameTestContext context) {
        int y = context.computeOnClient(client -> UiMenuBar.height(client.gui.screen().width) + 53);
        context.getInput().setCursorPos(6, y * 2);
        context.waitTicks(2);
    }
    private static String screenshot(ClientGameTestContext context, String name, int[] size) throws IOException {
        Path path = context.takeScreenshot(name);
        var image = javax.imageio.ImageIO.read(path.toFile());
        if (image == null || image.getWidth() != size[0] || image.getHeight() != size[1])
            throw new AssertionError("Source screenshot has unexpected dimensions: " + path);
        return path.toString();
    }
}
