package dev.hoi.client.screen.diplomacy;

import dev.hoi.protocol.diplomacy.DiplomacyPayloads;
import dev.hoi.protocol.diplomacy.DiplomacyView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import static dev.hoi.protocol.diplomacy.DiplomacyView.*;

public final class DiplomacyLeaseRequestTest {
    private DiplomacyLeaseRequestTest() {}
    public static void run(ClientGameTestContext context) {
        dev.hoi.client.screen.ScreenLeaseFixturesTest.run(context);
        routing(context);
        dev.hoi.client.screen.dialog.DialogLeaseNotificationTest.run(context);
        for (int width : List.of(2560, 1280)) {
            context.getInput().resizeWindow(width, width * 9 / 16);
            context.runOnClient(client -> client.options.guiScale().set(2));
            var requests = new ArrayList<DiplomacyPayloads.Request>();
            var choice = new EquipmentChoice("fixture", "요청 검증 장비", "", Category.INFANTRY, "12", "0", "", true, "", List.of(new ModeChoice(Mode.REQUEST, "", true, "", 0, "")));
            var request = fixture(List.of(choice), List.of(), false);
            context.setScreen(() -> new DiplomacyLease(null, request, DiplomacyLease.Presentation.REQUEST, requests::add));
            context.runOnClient(client -> {
                var screen = (DiplomacyLease)client.gui.screen();
                screen.add("fixture");
                check(screen.draft().equals(List.of(new LeaseLine("fixture", Mode.REQUEST, 0))), "Request must contain equipment only");
                check(screen.children().stream().noneMatch(EditBox.class::isInstance), "Request created an amount input");
                check(screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).noneMatch(button -> List.of("매월", "%", "일회성").contains(button.getMessage().getString())), "Request created mode buttons");
                for (int i = 0; i < 6; i++) screen.tick();
                check(requests.size() == 1 && requests.getFirst().lines().getFirst().mode() == Mode.REQUEST && requests.getFirst().lines().getFirst().amount() == 0, "Request preview changed the zero amount");
                var accepted = fixture(List.of(choice), screen.draft(), true);
                accepted = new DiplomacyView(request.session(), 2, accepted.viewer(), accepted.target(), accepted.title(), accepted.subtitle(), "", "", "", true, "", accepted.equipment(), accepted.lines());
                screen.update(DiplomacyPayloads.Response.of(requests.getFirst().token(), DiplomacyPayloads.Status.UPDATED, accepted));
                check(screen.canSubmit(), "Authoritative request preview did not enable submit");
                screen.submit(); screen.submit();
                check(requests.size() == 2 && requests.getLast().operation() == DiplomacyPayloads.Operation.SUBMIT, "Repeated request submit was duplicated");
            });
            context.takeScreenshot("hoi-lease-request-synthetic-" + width + "-gui2");
            requests.clear();
            var offerChoice = new EquipmentChoice("fixture", "응답 검증 장비", "", Category.INFANTRY, "12", "0", "", true, "", List.of(new ModeChoice(Mode.MONTHLY, "매월", true, "", 1, "개/매월"), new ModeChoice(Mode.PERCENT, "%", false, "미지원", 100, "%"), new ModeChoice(Mode.ONCE, "일회성", true, "", 1, "개")), "검증 장비 종류", "KOR", true);
            var answer = fixture(List.of(offerChoice), List.of(), true);
            context.setScreen(() -> new DiplomacyLease(null, answer, DiplomacyLease.Presentation.ANSWER, requests::add));
            context.runOnClient(client -> {
                var screen = (DiplomacyLease)client.gui.screen();
                check(screen.canSubmit() && screen.draft().isEmpty(), "Empty answer must be permitted by the server");
                screen.setAmount("fixture", "7");
                check(screen.draft().equals(List.of(new LeaseLine("fixture", Mode.MONTHLY, 7))), "Recipient could not choose the amount");
                screen.selectMode("fixture", Mode.ONCE);
                check(screen.draft().getFirst().mode() == Mode.ONCE, "Recipient could not select once");
                screen.selectMode("fixture", Mode.PERCENT);
                check(screen.draft().getFirst().mode() == Mode.ONCE, "Unsupported percentage was selectable");
                screen.setAmount("fixture", "0");
                check(screen.draft().isEmpty(), "Zero answer retained an equipment grant");
                for (int i = 0; i < 6; i++) screen.tick();
                check(requests.size() == 1 && requests.getFirst().lines().isEmpty(), "Empty answer preview invented equipment");
            });
            context.takeScreenshot("hoi-lease-answer-synthetic-" + width + "-gui2");
        }
    }
    private static void routing(ClientGameTestContext context) {
        var previous = context.computeOnClient(client -> dev.hoi.client.ClientCampaignHud.visible() ? dev.hoi.protocol.hud.HudPayloads.State.of(dev.hoi.client.ClientCampaignHud.country(), dev.hoi.client.ClientCampaignHud.hud()) : dev.hoi.protocol.hud.HudPayloads.State.HIDDEN);
        try {
            context.runOnClient(client -> dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.hud.HudPayloads.State.of("KOR", dev.hoi.protocol.country.CountryHud.UNKNOWN)));
            for (String action : List.of("request_lend_lease", "answer_lease:fixture")) {
                var sent = new ArrayList<dev.hoi.protocol.country.CountryPayloads.Action>();
                var reports = java.util.Arrays.stream(dev.hoi.protocol.agency.AgencyIntelDomain.values()).map(domain -> new dev.hoi.protocol.country.CountryView.Report(domain, 0, List.of())).toList();
                var country = new dev.hoi.protocol.country.CountryView("KOR", dev.hoi.protocol.country.CountryHud.UNKNOWN, "PRK", "검증 국가", false, List.of(), List.of(), List.of(), reports,
                        new dev.hoi.protocol.country.CountryView.Actions(UUID.randomUUID().toString(), 1, "", List.of(new dev.hoi.protocol.country.CountryView.Action(action, "편집기", "", true))));
                context.setScreen(() -> new DiplomacyCountryScreen(country, sent::add));
                context.runOnClient(client -> {
                    var screen = (DiplomacyCountryScreen)client.gui.screen();
                    var button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(value -> value.getMessage().getString().equals("편집기")).findFirst().orElseThrow();
                    button.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                    check(sent.size() == 1 && sent.getFirst().id().equals(action), "Editor action required an extra confirmation");
                    var editor = fixture(List.of(), List.of(), true);
                    screen.update(DiplomacyPayloads.Response.of(UUID.randomUUID().toString(), DiplomacyPayloads.Status.OPEN, editor));
                    check(client.gui.screen() == screen, "Stale editor response changed the screen");
                    screen.update(DiplomacyPayloads.Response.of(screen.token(), DiplomacyPayloads.Status.OPEN, editor));
                    check(client.gui.screen() instanceof DiplomacyLease, "Issued editor response did not open the lease screen");
                    var lease = (DiplomacyLease)client.gui.screen();
                    check(lease.canSubmit() == action.startsWith("answer_lease:"), "Editor action selected the wrong request or answer presentation");
                });
            }
        } finally { context.runOnClient(client -> dev.hoi.client.ClientCampaignHud.accept(previous)); }
    }
    private static DiplomacyView fixture(List<EquipmentChoice> equipment, List<LeaseLine> lines, boolean submit) {
        return new DiplomacyView(UUID.randomUUID().toString(), 1, "KOR", "PRK", "무기대여 요청", "", "", "", "", submit, "", equipment, lines);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
