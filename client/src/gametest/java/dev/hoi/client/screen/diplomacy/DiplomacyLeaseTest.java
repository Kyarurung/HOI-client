package dev.hoi.client.screen.diplomacy;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import dev.hoi.protocol.diplomacy.DiplomacyPayloads;
import dev.hoi.protocol.diplomacy.DiplomacyView;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
import static dev.hoi.protocol.diplomacy.DiplomacyPayloads.Operation.*;
import static dev.hoi.protocol.diplomacy.DiplomacyPayloads.Status.*;
import static dev.hoi.protocol.diplomacy.DiplomacyView.*;

public final class DiplomacyLeaseTest {
    private DiplomacyLeaseTest() {}
    public static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        var requests = new ArrayList<DiplomacyPayloads.Request>();
        String session = UUID.randomUUID().toString();
        Screen parent = context.computeOnClient(client -> new Screen(Component.literal("외교 배경 fixture")) {});
        context.setScreen(() -> new DiplomacyLease(parent, fixture(session, 1, List.of(), false), requests::add));
        context.waitTicks(3);
        context.runOnClient(client -> {
            for (String path : List.of("hoi:ui/diplomacy_labels.json", "hoi:textures/gui/diplomacy/lease/background.png",
                    "hoi:textures/gui/diplomacy/lease/selected_row.png", "hoi:textures/gui/diplomacy/lease/catalog_row/frame_0.png",
                    "hoi:textures/gui/diplomacy/dialog/remove/frame_2.png", "hoi:textures/gui/diplomacy/dialog/panel_dark.png",
                    "hoi:textures/gui/diplomacy/dialog/panel_plain.png", "hoi:textures/gui/diplomacy/dialog/panel_border.png"))
                check(client.getResourceManager().getResource(Identifier.parse(path)).isPresent(), "Missing source asset: " + path);
            var screen = (DiplomacyLease)client.gui.screen();
            var infantry = buttonContaining(screen, "보병 및 야포");
            click(screen, infantry, MOUSE_BUTTON_RIGHT);
            check(screen.children().stream().noneMatch(widget -> widget instanceof Button button && button.getMessage().getString().equals("검증 보병 장비")), "Right click opened equipment catalog");
            click(screen, infantry, MOUSE_BUTTON_LEFT);
            click(screen, button(screen, "검증 보병 장비"), MOUSE_BUTTON_LEFT);
            check(screen.draft().equals(List.of(new LeaseLine("infantry", Mode.MONTHLY, 1))), "Equipment choice lost its server default");
            check(requests.isEmpty() && !screen.canSubmit(), "Local draft was submitted without preview");
            var amount = input(screen);
            for (String value : List.of("", "0", "-1", "+1", " 1", "1.5", "１", "9223372036854775808")) {
                amount.setValue(value);
                for (int i = 0; i < 6; i++) screen.tick();
                check(requests.isEmpty() && !screen.canSubmit(), "Invalid draft amount reached transport: " + value);
            }
            amount.setValue("25");
            var focusClick = new MouseButtonEvent(amount.getX() + amount.getWidth() / 2.0, amount.getY() + amount.getHeight() / 2.0, new MouseButtonInfo(MOUSE_BUTTON_LEFT, 0));
            screen.mouseClicked(focusClick, false);
            screen.mouseReleased(focusClick);
            check(amount.isFocused(), "Actual amount click did not focus the current input");
            amount.setCursorPosition(1);
            amount.setHighlightPos(2);
            for (int i = 0; i < 6; i++) screen.tick();
            check(requests.size() == 1 && requests.getFirst().operation() == PREVIEW, "Valid local edit did not request preview");
            check(input(screen).isFocused(), "Preview request lost amount focus");
            check(input(screen).getCursorPosition() == 1 && input(screen).getHighlighted().equals("5"), "Preview request changed cursor or forward selection");
            var first = requests.getFirst();
            input(screen).setValue("30");
            input(screen).setCursorPosition(1);
            input(screen).setHighlightPos(0);
            screen.update(DiplomacyPayloads.Response.of(first.token(), UPDATED, fixture(session, 2, first.lines(), true)));
            check(input(screen).isFocused() && input(screen).getValue().equals("30"), "Preview response replaced a newer local edit");
            check(input(screen).getCursorPosition() == 1 && input(screen).getHighlighted().equals("3"), "Preview response changed cursor or backward selection");
            check(!screen.canSubmit(), "Old quote enabled a changed draft");
            for (int i = 0; i < 6; i++) screen.tick();
            check(requests.size() == 2 && requests.getLast().lines().getFirst().amount() == 30, "Latest edit did not receive its own preview");
            var second = requests.getLast();
            screen.update(DiplomacyPayloads.Response.of(first.token(), UPDATED, fixture(session, 50, first.lines(), true)));
            check(screen.view().revision() == 2 && screen.pending(), "Wrong token replaced pending quote");
            screen.update(DiplomacyPayloads.Response.of(second.token(), UPDATED, fixture(session, 3, second.lines(), true)));
            check(screen.canSubmit(), "Matching authoritative quote remained disabled");
            screen.update(fixture(session, 2, second.lines(), true));
            check(screen.view().revision() == 3, "Older revision replaced current quote");
            click(screen, button(screen, "실행"), MOUSE_BUTTON_LEFT);
            click(screen, button(screen, "실행"), MOUSE_BUTTON_LEFT);
            check(requests.size() == 3 && requests.getLast().operation() == SUBMIT && requests.getLast().lines().equals(second.lines()), "Submission duplicated or changed the quoted batch");
            for (int i = 0; i < 108; i++) screen.tick();
            check(requests.stream().filter(request -> request.operation() == SUBMIT).count() == 1, "Timeout retransmitted a mutation");
        });
        context.takeScreenshot("hoi-diplomacy-lease-synthetic-editor-gui2");
        modesAndClosure(context);
        missingEquipment(context);
        cancelRefresh(context);
        compact(context);
        scrollbars(context);
    }
    public static void runSource(ClientGameTestContext context) {
        String directory = System.getProperty("hoiLeasePreview", System.getenv("HOI_LEASE_PREVIEW"));
        check(directory != null && !directory.isBlank(), "Actual lease fixtures require hoiLeasePreview or HOI_LEASE_PREVIEW");
        var snapshots = List.of("empty.json", "once.json", "monthly.json", "multiple.json");
        var views = new LinkedHashMap<String, DiplomacyView>();
        try {
            var metadata = JsonParser.parseString(Files.readString(Path.of(directory, "fixture-source.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            check(metadata.get("schemaVersion").getAsInt() == 1 && metadata.get("sourceKind").getAsString().equals("actual-lease-sessions"), "Lease fixture provenance is not actual server state");
            String baseline = metadata.get("runtimeBaseline").getAsString();
            check(!baseline.isBlank() && metadata.get("snapshotUnchanged").getAsBoolean(), "Lease fixture baseline or unchanged-state evidence is missing");
            for (String name : List.of("catalogSHA256", "producerClassSHA256", "sessionClassSHA256"))
                check(metadata.has(name) && metadata.get(name).getAsString().matches("(?i)[0-9a-f]{64}"), "Missing actual lease producer hash: " + name);
            Path sessionSource = Path.of(metadata.get("sessionSourcePath").getAsString());
            check(sessionSource.isAbsolute() && sessionSource.getFileName().toString().equals("DiplomacyLeaseSessions.java"), "Lease production source path is missing or unexpected");
            check(sourceHash(sessionSource).equalsIgnoreCase(metadata.get("sessionSourceSHA256").getAsString()), "Lease production source changed after fixture export");
            var hashes = metadata.getAsJsonObject("snapshotSHA256");
            var declared = new ArrayList<String>();
            for (var item : metadata.getAsJsonArray("snapshots")) declared.add(item.getAsString());
            check(declared.size() == snapshots.size() && declared.containsAll(snapshots), "Lease fixture manifest does not contain all four authoritative snapshots");
            for (String name : snapshots) {
                Path snapshot = Path.of(directory, name);
                check(hashes.has(name) && sourceHash(snapshot).equalsIgnoreCase(hashes.get(name).getAsString()), "Actual lease snapshot hash mismatch: " + name);
                var view = new Gson().fromJson(Files.readString(snapshot, StandardCharsets.UTF_8), DiplomacyView.class);
                check(view != null && view.viewer().equals("KOR") && view.target().equals("PRK"), "Unexpected actual lease fixture countries: " + name);
                check(view.equipment().stream().anyMatch(choice -> choice.available() && !choice.icon().isBlank()), "Actual lease catalog contains no source equipment: " + name);
                views.put(name, view);
            }
            check(views.get("empty.json").lines().isEmpty(), "Empty source lease fixture already contains a draft");
            check(!views.get("once.json").lines().isEmpty() && views.get("once.json").lines().stream().allMatch(line -> line.mode() == Mode.ONCE), "One-time source fixture is not an issued one-time preview");
            check(!views.get("monthly.json").lines().isEmpty() && views.get("monthly.json").lines().stream().allMatch(line -> line.mode() == Mode.MONTHLY), "Monthly source fixture is not an issued monthly preview");
            check(views.get("multiple.json").lines().size() > 1, "Multiple source fixture contains fewer than two issued lines");
            System.out.println("Actual lease render fixture runtime baseline: " + baseline);
        } catch (IOException | RuntimeException error) {
            throw new AssertionError("Unable to load complete actual lease fixtures from " + directory, error);
        }
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        var labels = context.computeOnClient(client -> {
            var resource = client.getResourceManager().getResource(Identifier.parse("hoi:ui/diplomacy_labels.json")).orElseThrow();
            try (var input = resource.open(); var reader = new java.io.InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException error) {
                throw new AssertionError("Unable to read original diplomacy labels", error);
            }
        });
        for (var entry : views.entrySet()) {
            var view = entry.getValue();
            String stage = entry.getKey().replace(".json", "");
            var requests = new ArrayList<DiplomacyPayloads.Request>();
            context.getInput().setCursorPos(2558, 1438);
            context.setScreen(() -> new DiplomacyLease(null, view, requests::add));
            context.waitTicks(3);
            context.runOnClient(client -> {
                var screen = (DiplomacyLease)client.gui.screen();
                check(screen.view().equals(view) && screen.draft().equals(view.lines()), "Source render changed the issued lease view: " + stage);
                check(screen.canSubmit() == (view.submitEnabled() && !view.lines().isEmpty()), "Source render changed server submission permission: " + stage);
                for (var choice : view.equipment()) if (!choice.icon().isEmpty())
                    check(client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/" + choice.icon() + ".png")).isPresent(), "Missing issued lease equipment icon: " + choice.icon());
                check(requests.isEmpty(), "Opening source lease view sent a request: " + stage);
            });
            context.takeScreenshot("hoi-diplomacy-lease-source-" + stage + "-gui2");
            for (var category : Category.values()) {
                String key = "LEND_LEASE_" + switch (category) {
                    case INFANTRY -> "INFANTRY_BUTTON";
                    case ARMOR -> "VEHICLES_BUTTON";
                    case AIR -> "AIRCRAFT_BUTTON";
                    case NAVAL -> "NAVAL_BUTTON";
                };
                context.runOnClient(client -> click((DiplomacyLease)client.gui.screen(), button((DiplomacyLease)client.gui.screen(), labels.get(key).getAsString()), MOUSE_BUTTON_LEFT));
                context.waitTicks(3);
                context.takeScreenshot("hoi-diplomacy-lease-source-" + stage + "-catalog-" + category.name().toLowerCase(java.util.Locale.ROOT) + "-gui2");
            }
            if (stage.equals("empty")) {
                var disabled = view.equipment().stream().filter(choice -> !choice.available() && !choice.reason().isBlank()).findFirst().orElseThrow();
                check(disabled.category() == Category.INFANTRY, "Actual disabled fuel source choice is absent");
                context.runOnClient(client -> click((DiplomacyLease)client.gui.screen(), button((DiplomacyLease)client.gui.screen(), labels.get("LEND_LEASE_INFANTRY_BUTTON").getAsString()), MOUSE_BUTTON_LEFT));
                context.waitTicks(3);
                sourceDisabledTooltip(context, disabled.name(), "hoi-diplomacy-lease-source-fuel-disabled-gui2");
            }
            if (stage.equals("once")) {
                var selected = view.equipment().stream().filter(choice -> choice.key().equals(view.lines().getFirst().key())).findFirst().orElseThrow();
                var percent = selected.modes().stream().filter(mode -> mode.mode() == Mode.PERCENT).findFirst().orElseThrow();
                check(!percent.supported() && !percent.reason().isBlank(), "Actual unsupported percentage reason is missing");
                sourceDisabledTooltip(context, percent.label(), "hoi-diplomacy-lease-source-percent-disabled-gui2");
            }
            context.runOnClient(client -> {
                var screen = (DiplomacyLease)client.gui.screen();
                check(requests.isEmpty() && screen.view().equals(view) && screen.draft().equals(view.lines()), "Source render/catalog/disabled tooltip changed the actual issued draft: " + stage);
            });
        }
    }
    private static String sourceHash(Path path) throws IOException {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (java.security.NoSuchAlgorithmException unavailable) {
            throw new AssertionError("SHA-256 is required for actual lease fixture verification", unavailable);
        }
    }
    private static void sourceDisabledTooltip(ClientGameTestContext context, String label, String screenshot) {
        int[] point = context.computeOnClient(client -> {
            var target = button((DiplomacyLease)client.gui.screen(), label);
            check(!target.active, "Server-disabled lease control became active: " + label);
            double scale = client.getWindow().getGuiScale();
            return new int[]{(int)Math.round((target.getX() + target.getWidth() / 2.0) * scale), (int)Math.round((target.getY() + target.getHeight() / 2.0) * scale)};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.waitTicks(8);
        context.takeScreenshot(screenshot);
    }
    private static void modesAndClosure(ClientGameTestContext context) {
        var requests = new ArrayList<DiplomacyPayloads.Request>();
        String session = UUID.randomUUID().toString();
        Screen parent = context.computeOnClient(client -> new Screen(Component.literal("외교 배경 fixture")) {});
        context.setScreen(() -> new DiplomacyLease(parent, fixture(session, 1, List.of(), false), requests::add));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (DiplomacyLease)client.gui.screen();
            screen.add("infantry");
            screen.add("air");
            screen.add("fuel");
            check(screen.draft().size() == 2, "Unavailable source choice entered the draft");
            screen.selectMode("infantry", Mode.PERCENT);
            check(screen.draft().getFirst().mode() == Mode.MONTHLY, "Unsupported percentage mode changed the draft");
            screen.selectMode("infantry", Mode.ONCE);
            check(screen.draft().getFirst().equals(new LeaseLine("infantry", Mode.ONCE, 12)), "One-time mode ignored its issued default");
            screen.setAmount("infantry", Long.toString(Long.MAX_VALUE));
            for (int i = 0; i < 6; i++) screen.tick();
            check(requests.getLast().lines().getFirst().amount() == Long.MAX_VALUE, "One-time input was capped by displayed stock");
            screen.remove("air");
            check(screen.draft().size() == 1, "Remove changed unrelated draft lines");
            screen.clearDraft();
            check(screen.draft().isEmpty() && !screen.canSubmit(), "Clear all retained a submitted batch");
            screen.onClose();
            screen.onClose();
            check(requests.stream().filter(request -> request.operation() == CLOSE).count() == 1 && client.gui.screen() == parent, "Cancel failed to close once and return to its parent");
            check(requests.stream().noneMatch(request -> request.operation() == SUBMIT), "Cancel submitted a mutation");
        });
        context.setScreen(() -> new DiplomacyLease(parent, fixture(session, 1, List.of(new LeaseLine("infantry", Mode.MONTHLY, 1)), true), requests::add));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (DiplomacyLease)client.gui.screen();
            screen.submit();
            String token = requests.getLast().token();
            screen.update(DiplomacyPayloads.Response.closed(UUID.randomUUID().toString(), COMPLETED, ""));
            check(client.gui.screen() == screen, "Wrong completion token closed the editor");
            screen.update(DiplomacyPayloads.Response.closed(token, COMPLETED, ""));
            check(client.gui.screen() == parent, "Completed draft failed to return to parent");
            var revoked = new DiplomacyLease(parent, fixture(session, 1, List.of(), false), requests::add);
            client.gui.setScreen(revoked);
            revoked.add("infantry");
            for (int i = 0; i < 6; i++) revoked.tick();
            String revokedToken = requests.getLast().token();
            revoked.update(DiplomacyPayloads.Response.closed(revokedToken, REVOKED, ""));
            check(client.gui.screen() == null && revoked.draft().isEmpty(), "Revoked draft remained visible");
            revoked.update(DiplomacyPayloads.Response.of(revokedToken, UPDATED, fixture(session, 10, List.of(), false)));
            check(client.gui.screen() == null, "Late response reopened a revoked editor");
        });
    }
    private static void missingEquipment(ClientGameTestContext context) {
        var requests = new ArrayList<DiplomacyPayloads.Request>();
        String session = UUID.randomUUID().toString();
        context.setScreen(() -> new DiplomacyLease(null, fixture(session, 1, List.of(), false), requests::add));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (DiplomacyLease)client.gui.screen();
            screen.add("infantry");
            screen.add("air");
            for (int i = 0; i < 6; i++) screen.tick();
            var first = requests.getLast();
            screen.setAmount("infantry", "");
            screen.setAmount("air", "7");
            String reason = "발급된 장비의 재고 또는 생산 상태가 변경되었습니다. 다시 선택하세요.";
            var base = fixture(session, 2, List.of(new LeaseLine("air", Mode.MONTHLY, 1)), false);
            var remaining = base.equipment().stream().filter(choice -> !choice.key().equals("infantry")).toList();
            var changed = new DiplomacyView(base.session(), base.revision(), base.viewer(), base.target(), base.title(), base.subtitle(),
                    base.currentStatus(), base.acceptanceText(), base.acceptanceDetail(), false, reason, remaining, base.lines());
            screen.update(DiplomacyPayloads.Response.of(first.token(), UPDATED, changed));
            check(screen.draft().equals(List.of(new LeaseLine("air", Mode.MONTHLY, 7))), "Removed server choice left a hidden row or overwrote another local edit");
            check(screen.view().submitReason().equals(reason) && !screen.canSubmit(), "Server removal reason was lost or bypassed");
            check(input(screen).getValue().equals("7"), "Removed row retained its invalid input");
            for (int i = 0; i < 6; i++) screen.tick();
            check(requests.size() == 2 && requests.getLast().lines().equals(screen.draft()), "Preview resent an equipment key removed by the server");
            var second = requests.getLast();
            var rejected = new DiplomacyView(base.session(), 3, base.viewer(), base.target(), base.title(), base.subtitle(),
                    base.currentStatus(), base.acceptanceText(), base.acceptanceDetail(), false, reason, remaining, second.lines());
            screen.update(DiplomacyPayloads.Response.of(second.token(), UPDATED, rejected));
            for (int i = 0; i < 12; i++) screen.tick();
            check(requests.size() == 2 && screen.view().submitReason().equals(reason), "Rejected unchanged draft repeatedly requested preview");
            click(screen, button(screen, "닫기 · 검증 항공기"), MOUSE_BUTTON_LEFT);
            check(screen.draft().isEmpty(), "Remaining invalid draft cannot be removed");
        });
    }
    private static void cancelRefresh(ClientGameTestContext context) {
        var requests = new ArrayList<DiplomacyPayloads.Request>();
        var parent = context.computeOnClient(client -> new DiplomacyCountryScreen("PRK"));
        context.setScreen(() -> new DiplomacyLease(parent, fixture(UUID.randomUUID().toString(), 1, List.of(), false), requests::add));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (DiplomacyLease)client.gui.screen();
            String previous = parent.token();
            screen.onClose();
            check(client.gui.screen() == parent && !parent.token().equals(previous), "Cancel restored the consumed country session without requesting refresh");
            String refreshed = parent.token();
            screen.onClose();
            check(requests.size() == 1 && requests.getFirst().operation() == CLOSE && parent.token().equals(refreshed), "Repeated cancellation reopened or refreshed twice");
        });
    }
    private static void compact(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.setScreen(() -> new DiplomacyLease(null, fixture(UUID.randomUUID().toString(), 1, List.of(), false), request -> {}));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (DiplomacyLease)client.gui.screen();
            click(screen, buttonContaining(screen, "항공기 지원"), MOUSE_BUTTON_LEFT);
            click(screen, button(screen, "검증 항공기"), MOUSE_BUTTON_LEFT);
            for (var child : screen.children()) if (child instanceof AbstractWidget widget)
                check(widget.getX() >= 0 && widget.getY() >= 0 && widget.getRight() <= screen.width && widget.getBottom() <= screen.height, "Editor widget exceeds compact GUI2 bounds");
            check(!screen.allowsMovement(), "Modal editor allowed map movement");
        });
        context.takeScreenshot("hoi-diplomacy-lease-synthetic-editor-compact-gui2");
    }
    private static void scrollbars(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560, 1440);
        var base = fixture(UUID.randomUUID().toString(), 1, List.of(), false);
        var catalog = new ArrayList<EquipmentChoice>();
        for (int index = 0; index < 18; index++) catalog.add(new EquipmentChoice("row_" + index, "검증 장비 " + index, "", Category.INFANTRY,
                Integer.toString(index + 1), "생산량 없음", "검증 장비 설명", true, "", base.equipment().getFirst().modes()));
        var expanded = new DiplomacyView(base.session(), base.revision(), base.viewer(), base.target(), base.title(), base.subtitle(),
                base.currentStatus(), base.acceptanceText(), base.acceptanceDetail(), false, base.submitReason(), catalog, List.of());
        var requests = new ArrayList<DiplomacyPayloads.Request>();
        context.setScreen(() -> new DiplomacyLease(null, expanded, requests::add));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (DiplomacyLease)client.gui.screen();
            click(screen, buttonContaining(screen, "보병 및 야포"), MOUSE_BUTTON_LEFT);
            var close = button(screen, "닫기");
            var first = button(screen, "검증 장비 0");
            double x = close.getX() + 16, upper = first.getY() + 15, lower = close.getY() + 263;
            screen.mouseClicked(new MouseButtonEvent(x, upper, new MouseButtonInfo(MOUSE_BUTTON_RIGHT, 0)), false);
            check(button(screen, "검증 장비 0") != null && screen.draft().isEmpty(), "Right scrollbar click changed catalog or draft");
            screen.mouseClicked(new MouseButtonEvent(x, upper, new MouseButtonInfo(MOUSE_BUTTON_LEFT, 0)), false);
            screen.mouseDragged(new MouseButtonEvent(x, lower, new MouseButtonInfo(MOUSE_BUTTON_LEFT, 0)), 0, lower - upper);
            screen.mouseReleased(new MouseButtonEvent(x, lower, new MouseButtonInfo(MOUSE_BUTTON_LEFT, 0)));
            check(screen.children().stream().anyMatch(widget -> widget instanceof Button button && button.getMessage().getString().equals("검증 장비 17")), "Catalog scrollbar cannot reach last issued equipment");
            check(requests.isEmpty() && screen.draft().isEmpty(), "Catalog navigation sent a mutation");
            click(screen, button(screen, "검증 장비 17"), MOUSE_BUTTON_LEFT);
            check(screen.draft().size() == 1 && screen.draft().getFirst().key().equals("row_17"), "Scrolled catalog row selected another equipment key");
        });
        context.takeScreenshot("hoi-diplomacy-lease-synthetic-catalog-scrolled-gui2");
    }
    private static DiplomacyView fixture(String session, long revision, List<LeaseLine> lines, boolean submit) {
        var modes = List.of(new ModeChoice(Mode.MONTHLY, "매월", true, "매월 보낼 장비의 수량을 선택하십시오.", 1, "개/매월"),
                new ModeChoice(Mode.PERCENT, "%", false, "검증용 미지원 사유", 100, "월 생산량의 %"),
                new ModeChoice(Mode.ONCE, "일회성", true, "일회성으로 제공할 장비의 수량을 선택하십시오.", 12, "개"));
        var equipment = List.of(new EquipmentChoice("infantry", "검증 보병 장비", "", Category.INFANTRY, "12", "매일 3.20\n생산", "검증 설명\n----\n§Y정밀도 3.20§!", true, "", modes),
                new EquipmentChoice("air", "검증 항공기", "", Category.AIR, "5", "생산량 없음", "검증 항공기 설명", true, "", modes),
                new EquipmentChoice("fuel", "검증 연료", "", Category.INFANTRY, "?", "?", "", false, "검증용 연료 미지원 사유", modes));
        return new DiplomacyView(session, revision, "KOR", "PRK", "무기대여 시작", "매월 보낼 장비를 선택하십시오.",
                "현재 무기대여 없음", "검증 수락 전망", "검증 문단\n----\n§Y100.00§!", submit, submit ? "" : "검증 초안 대기", equipment, lines);
    }
    private static EditBox input(DiplomacyLease screen) {
        return (EditBox)screen.children().stream().filter(widget -> widget instanceof EditBox).findFirst().orElseThrow();
    }
    private static Button button(DiplomacyLease screen, String label) {
        return (Button)screen.children().stream().filter(widget -> widget instanceof Button button && button.getMessage().getString().equals(label)).findFirst().orElseThrow();
    }
    private static Button buttonContaining(DiplomacyLease screen, String label) {
        return (Button)screen.children().stream().filter(widget -> widget instanceof Button button && button.getMessage().getString().contains(label)).findFirst().orElseThrow();
    }
    private static void click(DiplomacyLease screen, Button button, int mouseButton) {
        var event = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(mouseButton, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}