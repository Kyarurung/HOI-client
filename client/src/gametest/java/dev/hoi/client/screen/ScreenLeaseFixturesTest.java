package dev.hoi.client.screen;

import com.google.gson.Gson;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.dialog.DialogStack;
import dev.hoi.client.screen.diplomacy.DiplomacyCountryScreen;
import dev.hoi.client.screen.diplomacy.DiplomacyLease;
import dev.hoi.protocol.country.CountryView;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.protocol.diplomacy.DiplomacyView;
import dev.hoi.protocol.hud.HudPayloads;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class ScreenLeaseFixturesTest {
    private ScreenLeaseFixturesTest() {}
    public static boolean available() { return System.getenv("HOI_LEASE_REQUEST_PREVIEW") != null; }
    public static <T> T fixture(String name, Class<T> type) {
        try {
            var root = Path.of(System.getenv("HOI_LEASE_REQUEST_PREVIEW"));
            var manifest = Files.readAllLines(root.resolve("SHA256SUMS.txt"));
            var expected = manifest.stream().filter(row -> row.endsWith(" *" + name)).findFirst().orElseThrow();
            var bytes = Files.readAllBytes(root.resolve(name));
            var actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!expected.startsWith(actual + " *")) throw new AssertionError("Lease fixture hash differs: " + name);
            return new Gson().fromJson(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), type);
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }
    public static void screenshot(ClientGameTestContext context, String name, int width) {
        context.waitTicks(3);
        context.takeScreenshot("hoi-lease-source-" + System.getenv().getOrDefault("HOI_LEASE_CAPTURE_STAGE", "after") + "-" + name + "-" + width + "-gui2");
    }
    public static void run(ClientGameTestContext context) {
        if (!available()) return;
        var previous = context.computeOnClient(client -> ClientCampaignHud.visible() ? HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()) : HudPayloads.State.HIDDEN);
        try {
            for (int width : List.of(2560, 1280)) {
                context.getInput().resizeWindow(width, width * 9 / 16);
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.getInput().setCursorPos(width - 8, width * 9 / 16 - 8);
                for (String name : List.of("kor-diplomacy-JAP-at-war", "kor-diplomacy-JAP-lease-requested", "jap-diplomacy-KOR-lease-request")) {
                    var view = fixture(name + ".json", CountryView.class);
                    context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of(view.viewer(), view.hud())));
                    context.setScreen(() -> new DiplomacyCountryScreen(view, request -> {}));
                    screenshot(context, name, width);
                    context.runOnClient(client -> client.gui.screen().mouseScrolled(30, client.gui.screen().height - 60, 0, -100));
                    screenshot(context, name + "-bottom", width);
                }
                for (String name : List.of("kor-lease-request-empty", "kor-lease-request-selected", "jap-lease-answer-empty", "jap-lease-answer-selected")) {
                    var view = fixture(name + ".json", DiplomacyView.class);
                    boolean answer = name.startsWith("jap");
                    var country = fixture(answer ? "jap-diplomacy-KOR-lease-request.json" : "kor-diplomacy-JAP-at-war.json", CountryView.class);
                    context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of(country.viewer(), country.hud())));
                    context.setScreen(() -> new DiplomacyLease(new DiplomacyCountryScreen(country, request -> {}), view, answer ? DiplomacyLease.Presentation.ANSWER : DiplomacyLease.Presentation.REQUEST, request -> {}));
                    context.runOnClient(client -> {
                        if (!((DiplomacyLease)client.gui.screen()).draft().equals(view.lines())) throw new AssertionError("Source lease lines changed");
                    });
                    screenshot(context, name, width);
                }
                var offerRoot = Path.of(System.getProperty("hoiLeasePreview", System.getenv("HOI_LEASE_PREVIEW")));
                for (String name : List.of("monthly", "multiple")) {
                    DiplomacyView offer;
                    try { offer = new Gson().fromJson(Files.readString(offerRoot.resolve(name + ".json")), DiplomacyView.class); }
                    catch (IOException failure) { throw new UncheckedIOException(failure); }
                    var country = fixture("kor-diplomacy-JAP-at-war.json", CountryView.class);
                    context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of(offer.viewer(), country.hud())));
                    context.setScreen(() -> new DiplomacyLease(new DiplomacyCountryScreen(country, request -> {}), offer, DiplomacyLease.Presentation.OFFER, request -> {}));
                    screenshot(context, "kor-lease-offer-" + name, width);
                }
                for (String name : List.of("jap-lease-request-dialog", "kor-lease-reply-dialog", "kor-lease-reply-nothing-dialog")) {
                    var view = fixture(name + ".json", DialogView.class);
                    var country = fixture(name.startsWith("jap") ? "jap-diplomacy-KOR-lease-request.json" : "kor-diplomacy-JAP-at-war.json", CountryView.class);
                    context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of(country.viewer(), country.hud())));
                    context.setScreen(() -> {
                        var stack = new DialogStack(null, (dialog, choice) -> {});
                        stack.add(view);
                        return stack;
                    });
                    screenshot(context, name, width);
                    context.runOnClient(client -> {
                        var dialog = ((DialogStack)client.gui.screen()).find(view.token());
                        if (!dialog.view().body().equals(view.body())) throw new AssertionError("Source reply whitespace changed");
                        dialog.mouseScrolled(0, 0, 0, -100);
                    });
                    screenshot(context, name + "-bottom", width);
                }
            }
        } finally { context.runOnClient(client -> ClientCampaignHud.accept(previous)); }
    }
}
