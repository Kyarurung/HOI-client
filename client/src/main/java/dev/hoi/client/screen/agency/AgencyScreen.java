package dev.hoi.client.screen.agency;

import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.HoiClient;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.UiPanelButton;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.country.CountryFlags;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiHoverRegion;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.agency.AgencyView.*;
import dev.hoi.client.screen.commander.CommanderConfirmation;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class AgencyScreen extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final int TEXT = 0xFFE0E3DD, GOLD = 0xFFFFAA00;
    public static final List<String> UPGRADE_ORDER = List.of("foreign_intelligence", "domestic_intelligence", "military_intelligence", "hidden_front", "planning_and_direction", "collection", "processing_and_exploitation", "analysis", "dissemination", "humint", "sigint", "masint", "osint", "geoint", "cell_system", "communication_security", "support_services", "enhanced_interrogation_techniques", "termination", "elint", "comint", "advanced_cryptoanalytical_attack_models", "cryptosystem_algorithm_upgrade", "quantum_cryptography");
    private static final String[] UPGRADE_GROUPS = {"정보공동체", "정보 순환", "정보 수집 분야", "휴민트", "신호 정보"};
    private final Screen parent;
    private final Consumer<AgencyPayloads.Request> transport;
    private final String token;
    private AgencyView view;
    private String group = "operations", selectedId;
    private final List<String> arguments = new ArrayList<>();
    private static final UiMenuStyle.Window WINDOW = UiMenuStyle.window(MenuTab.INTELLIGENCE);
    private UiMenuStyle.Frame frame;
    private int pane, top, scroll, detailScroll, choosing = -1, choiceScroll, pendingTicks, refreshTicks, overlayStart;
    private boolean cryptoScrolling;
    private String searchText = "";
    private EditBox search;
    private AgencyPayloads.Kind pendingKind;
    private String queuedUpgrade;
    private final AgencyCapitalMap capitalMap = new AgencyCapitalMap();
    private final dev.hoi.client.map.agency.AgencyOperations operationMap = new dev.hoi.client.map.agency.AgencyOperations();
    List<AgencyCapitalMap.Hit> capitalHits() { return capitalMap.hits(); }
    public AgencyScreen(Screen parent) {
        this(parent, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(AgencyPayloads.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    public AgencyScreen(Screen parent, String token, AgencyView fixture, Consumer<AgencyPayloads.Request> transport) {
        super(Component.literal("정보기관")); this.parent = parent; this.token = token; this.view = fixture; this.transport = transport;
    }
    public AgencyView snapshot() { return view; }
    public Screen dismissalBackdrop() { return parent instanceof MapCursor ? parent : this; }
    private void closeDismissal() { if (parent instanceof MapCursor) onClose(); else minecraft.gui.setScreen(this); }
    public boolean awaitingResponse() { return pendingTicks > 0; }
    public void highlightMission(String id) {
        if (view != null && pendingTicks == 0 && id.startsWith("assign:") && view.items().stream().anyMatch(item -> item.id().equals(id) && item.enabled()))
            transport.accept(new AgencyPayloads.Request(AgencyPayloads.Kind.HIGHLIGHT, token, view.revision(), id, List.of()));
    }
    public void assignMission(String id, List<String> args) {
        if (view == null || pendingTicks > 0 || !id.startsWith("assign:")) return;
        var item = view.items().stream().filter(value -> value.id().equals(id) && value.enabled()).findFirst().orElse(null);
        if (item != null && item.parameters().size() == args.size()) send(AgencyPayloads.Kind.CALL, id, args);
    }
    void operationAction(String id) {
        if (view == null || pendingTicks > 0) return;
        var action = view.items().stream().filter(value -> value.id().equals(id) && value.enabled() && value.parameters().isEmpty()).findFirst().orElse(null);
        if (action != null) send(AgencyPayloads.Kind.CALL, id, List.of());
    }
    public void showMapItem(String id) {
        if (view == null) return;
        var item = view.items().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (item == null) return;
        if (id.equals("recruit") || id.startsWith("vacant:")) {
            if (item.enabled() && !view.candidates().isEmpty() && pendingTicks == 0) minecraft.gui.setScreen(
                    new AgencyCandidates(this, view, key -> send(AgencyPayloads.Kind.CALL, key, List.of())));
        } else if (id.startsWith("dismiss:")) {
            if (item.enabled() && item.parameters().isEmpty() && pendingTicks == 0) {
                var confirm = view.confirms().stream().filter(value -> value.item().equals(id)).findFirst().orElse(null);
                if (confirm == null || minecraft.gui.screen() instanceof CommanderConfirmation) return;
                long revision = view.revision();
                var dialog = new DialogView(token, revision, DialogView.Kind.COMMANDER, confirm.title(), "", confirm.body(), "", "", "", List.of(),
                        List.of(new DialogView.Choice("cancel", dev.hoi.client.ui.UiCommandLabels.text("PDXO_CANCEL", "취소"), ""), new DialogView.Choice("ok", "OK", "")));
                var screen = new CommanderConfirmation(dialog, (ignored, choice) -> {
                    if (!view.country().equals(dev.hoi.client.ClientCampaignHud.country())) { minecraft.gui.setScreen(null); return; }
                    if (choice.equals("ok") && view.confirms().contains(confirm) && pendingTicks == 0
                            && view.items().stream().anyMatch(value -> value.id().equals(id) && value.enabled())) {
                        send(AgencyPayloads.Kind.CALL, id, List.of());
                        dev.hoi.client.audio.AudioUiSounds.play("ui.disband");
                    }
                    closeDismissal();
                });
                screen.backdrop(this);
                minecraft.gui.setScreen(screen);
            }
        } else selectItem(item);
    }
    public void open() { send(AgencyPayloads.Kind.OPEN, "", List.of()); }
    public void update(AgencyView next) {
        if (!next.session().equals(token)) return;
        if (next.country().isEmpty() || view != null && !next.country().equals(view.country())) { minecraft.gui.setScreen(null); return; }
        if (view != null && next.revision() <= view.revision()) return;
        capitalMap.clear();
        operationMap.clear();
        boolean confirmsChanged = view != null && !view.confirms().equals(next.confirms());
        view = next;
        if (confirmsChanged && minecraft.gui.screen() instanceof CommanderConfirmation confirmation && confirmation.backdrop() == this) closeDismissal();
        pendingTicks = 0;
        pendingKind = null;
        if (minecraft.gui.screen() instanceof AgencyOperation operation && operation.backdrop() == this) operation.update(next);
        if (minecraft.gui.screen() instanceof AgencyCandidates candidates && candidates.backdrop() == this) candidates.update(next);
        pendingTicks = 0;
        pendingKind = null;
        var item = selected();
        if (item == null) { selectedId = null; choosing = -1; }
        else normalizeArguments(item);
        String queued = queuedUpgrade;
        queuedUpgrade = null;
        if (queued != null && view.items().stream().anyMatch(i -> i.id().equals(queued) && i.enabled() && i.parameters().isEmpty())) {
            send(AgencyPayloads.Kind.CALL, queued, List.of());
        } else rebuildWidgets();
    }
    private void normalizeArguments(Item item) {
        while (arguments.size() > item.parameters().size()) arguments.removeLast();
        for (int i = 0; i < item.parameters().size(); i++) {
            var choices = item.parameters().get(i).choices();
            if (arguments.size() <= i) arguments.add(choices.isEmpty() ? "" : choices.getFirst().value());
            String value = arguments.get(i);
            if (choices.stream().noneMatch(c -> c.value().equals(value))) arguments.set(i, choices.isEmpty() ? "" : choices.getFirst().value());
        }
    }
    @Override protected void rebuildWidgets() {
        var focused = getFocused();
        super.rebuildWidgets();
        if (focused != null && children().contains(focused)) setFocused(focused);
    }
    @Override protected void init() {
        overlayStart = 0;
        InputSidebarMovement.release(minecraft);
        pane = UiPanelLayout.width(MenuTab.INTELLIGENCE, width);
        frame = UiMenuStyle.frame(WINDOW, width, pane);
        top = Math.round(frame.y());
        HudMenuBar.buttons(width, view == null ? "" : view.country(), MenuTab.INTELLIGENCE, tab -> {
            if (tab == MenuTab.INTELLIGENCE) return;
            if (tab == MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(UiMenuStyle.close(WINDOW, frame, this::onClose));
        if (view == null) return;
        String[] countLabels = {"모집된 요원", "적에게 포획된 요원", "적에게 사살당한 정보원"};
        String[] countIcons = {"total_operatives", "arrested_operatives", "dead_operatives"};
        for (int i = 0; i < countLabels.length; i++) {
            var indicator = new UiMenuButton(countLabels[i], frame.ix(COUNTERS[i][0]), frame.iy(COUNTERS[i][1]), frame.len(40), frame.len(40), () -> {})
                    .caption("").background("agency/" + countIcons[i]).art(frame.x(COUNTERS[i][0]), frame.y(COUNTERS[i][1]), frame.size(40), frame.size(40));
            indicator.setTooltip(null); indicator.active = false; addRenderableWidget(indicator);
        }
        if (unestablished()) {
            var action = view.items().stream().filter(i -> i.id().equals("create") || i.id().equals("cancel_project")).findFirst().orElse(null);
            if (action != null) {
                var button = new UiPanelButton(action.id().equals("create") ? "정보기관 창설" : "기관 창설 취소", frame.ix(25), frame.iy(56), frame.len(508), frame.len(99),
                        () -> send(AgencyPayloads.Kind.CALL, action.id(), List.of())) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    }
                };
                button.active = action.enabled() && pendingTicks == 0; addRenderableWidget(button);
            }
            for (int i=0;i<UPGRADE_GROUPS.length;i++) {
                var button=branchButton(i,()->{});
                button.active=false;addRenderableWidget(button);
            }
            for(int i=0;i<2;i++) addRenderableWidget(tabButton(i));
            return;
        }
        if (view.items().stream().noneMatch(i -> i.id().equals("recruit"))) return;
        for (int i = 0; i < UPGRADE_GROUPS.length; i++) {
            int category = i;
            var improveButton = branchButton(i, () -> {
                changeGroup("upgrades");
                var items = visibleItems();
                for (int n = 0; n < items.size(); n++) if (upgradeCategory(items.get(n)) == category) {
                    int columns = upgradeColumns();
                    scroll = upgradeOffset(items, n, columns) - 14;
                    rebuildWidgets(); break;
                }
            });
            improveButton.bold(); addRenderableWidget(improveButton);
        }
        for (int i = 0; i < 2; i++) addRenderableWidget(tabButton(i).bold());
        var recruit = view.items().stream().filter(i -> i.id().equals("recruit")).findFirst().orElseThrow();
        var days = view.items().stream().filter(i -> i.id().equals("recruit_days")).findFirst().orElse(null);
        String remaining = recruit.enabled() && !view.candidates().isEmpty() ? "준비 완료!" : days == null || days.progress() < 0 ? "" : days.value() + "일";
        var recruitButton = agencyGauge("정보원 모집", "정보원 모집중", remaining,
                363, days == null ? 0 : days.progress(), () -> { if (!view.candidates().isEmpty()) minecraft.gui.setScreen(new AgencyCandidates(this,view,item -> send(AgencyPayloads.Kind.CALL,item,List.of()))); });
        recruitButton.setTooltip(null);
        recruitButton.active = recruit.enabled() && !view.candidates().isEmpty() && pendingTicks == 0;
        addRenderableWidget(recruitButton);
        var project = view.items().stream().filter(v -> v.id().equals("cancel_project")).findFirst().orElse(null);
        String projectDays = project == null ? "" : project.value().substring(project.value().lastIndexOf('·') + 1).replace(" 남음", "").trim();
        var improvement = agencyGauge("기관 개선 진행", project == null ? "진행 중인 개선 없음." : project.title(), projectDays,
                330, project == null ? 0 : project.progress(), () -> {});
        improvement.active = false;
        addRenderableWidget(improvement);
        if (project != null) {
            var cancel = new UiMenuButton("사업 취소", frame.ix(509), frame.iy(333), frame.len(24.3), frame.len(24.3),
                    () -> send(AgencyPayloads.Kind.CALL, project.id(), List.of())).caption("").background("agency/ui/cancel").art(frame.x(509), frame.y(333), frame.size(24.3), frame.size(24.3)).sound("ui.close");
            cancel.active = pendingTicks == 0;
            addRenderableWidget(cancel);
        }
        overlayStart = children().size();
        if (group.equals("upgrades")) {
            for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget button) button.active = false;
            addRenderableWidget(new UiMenuButton("개선 창 닫기", upgradeX() + upgradeWidth() - 26, upgradeY() + 4, 20, 19, () -> changeGroup("operations")).caption("").background("menu/close").sound("ui.close"));
        }
        var items = visibleItems();
        boolean grid = group.equals("upgrades");
        boolean crypto = group.equals("cryptology");
        int x = grid ? upgradeX() + 17 : frame.ix(15), y = grid ? upgradeY() + 44 : frame.iy(crypto ? 505 : 450);
        int areaWidth = grid ? upgradeWidth() - 28 : frame.len(crypto ? 516 : 519);
        int columns = grid ? upgradeColumns() : 1;
        int pitch = grid ? 55 : crypto ? cryptoPitch() : frame.len(90);
        int contentHeight = grid ? (items.isEmpty() ? 0 : upgradeOffset(items, items.size() - 1, columns) + pitch) : crypto ? frame.len(items.size() * 83) : items.size() * pitch;
        int listBottom = grid ? upgradeY() + upgradeHeight() - 3 : listBottom();
        scroll = Math.clamp(scroll, 0, Math.max(0, contentHeight - (listBottom - y)));
        if (selectedId == null || !grid) for (int i = 0; i < items.size(); i++) {
            var item = items.get(i); int position = grid ? upgradePosition(items, i, columns) : i;
            int bx = x + (position % columns) * areaWidth / columns, by = y + (grid ? upgradeOffset(items, i, columns) : crypto ? frame.len(i * 83) : i * pitch) - scroll;
            if (crypto ? by + pitch <= y || by >= listBottom : by < y || by + pitch > listBottom) continue;
            if (grid) {
                addRenderableWidget(new AgencyUpgradeButton(item, bx, by, areaWidth / columns - 4, pitch, () -> selectItem(item)));
                continue;
            }
            int widgetTop = crypto ? Math.max(y, by) : by;
            int widgetHeight = crypto ? Math.max(1, Math.min(listBottom, by + pitch) - widgetTop) : frame.len(88);
            var button = new UiPanelButton(item.title(), bx, widgetTop, areaWidth, widgetHeight, () -> {
                selectItem(item);
            }) {
                @Override public boolean isMouseOver(double mouseX, double mouseY) {
                    if (item.id().startsWith("dismiss_operation:") && mouseX >= bx + getWidth() - 69
                            && mouseX < bx + getWidth() - 7 && mouseY >= by + getHeight() - 25
                            && mouseY < by + getHeight() - 7) return false;
                    return super.isMouseOver(mouseX, mouseY);
                }
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    int w = getWidth(), h = getHeight();
                    if (item.group().equals("operations")) {
                        var operation = view.operations().stream().filter(value -> value.item().equals(item.id())).findFirst().orElse(null);
                        var offer = view.offers().stream().filter(value -> value.item().equals(item.id())).findFirst().orElse(null);
                        var tokens = operation != null ? operation.tokens() : offer != null ? offer.tokens() : List.<AgencyView.Token>of();
                        boolean completed = operation != null && operation.stage().equals("COMPLETED");
                        UiAssets.drawExact(g, "agency/ui/operation_row" + (item.progress() >= 0 ? 1 : 0), bx, by, frame.size(519), frame.size(89));
                        int imageLeft = bx + Math.round(w * 10f / 519), imageRight = bx + Math.round(w * 96f / 519);
                        int textX = imageRight + 5, flagX = bx + w - 26;
                        int actionX = bx + w - 69, actionY = by + h - 25;
                        UiAssets.cover(g, AgencyOperation.texture(item.texture()), imageLeft, by + 1, imageRight - imageLeft, h - 4);
                        if (completed) UiAssets.draw(g, AgencyOperation.sprite("GFX_operation_complete_list"), bx + 4, by + 3, Math.round(w * 408f / 519), h - 6);
                        g.enableScissor(bx + 4, by + 4, bx + w - 4, by + h - 4);
                        var title = Tooltips.officerText(item.title());
                        int titleWidth = (int)(flagX - textX - 6 - tokens.stream().filter(token -> !token.required()).count() * 15);
                        UiText.marquee(g, font, title, textX, by + 7, titleWidth, UiText.scale(font), TEXT, true, -1);
                        float tokenX = textX + Math.min(titleWidth, Math.round(font.width(title) * UiText.scale(font))) + 6;
                        float tokenY = by + 5;
                        for (var token : tokens) if (!token.required()) { AgencyOperation.token(g, token, tokenX, tokenY); tokenX += 15; }
                        tokenX = bx + 107.5f;
                        for (var token : tokens) if (token.required()) { AgencyOperation.token(g, token, tokenX, by + 25); tokenX += 17; }
                        String target=operation != null ? operation.country() : item.parameters().isEmpty()?"":item.parameters().getFirst().choices().stream().findFirst().map(Choice::value).orElse("");
                        if(!target.isEmpty())dev.hoi.client.ui.country.CountryFlags.draw(g,"country/"+target.toLowerCase(Locale.ROOT)+"/flag","small",flagX,by+7,16,11);
                        if(offer != null || operation != null) {
                            int requirementY = by + 25;
                            g.pose().pushMatrix(); g.pose().translate(bx, requirementY); g.pose().scale(.5f);
                            UiAssets.draw(g,"agency/ui/required",98,0,33,29);
                            UiAssets.draw(g,"agency/ui/network",145,0,33,29);
                            g.pose().popMatrix();
                            UiText.text(g,font,Integer.toString(operation != null ? operation.required() : offer.required()),bx+67,requirementY+3,TEXT);
                            UiText.text(g,font,java.math.BigDecimal.valueOf(operation != null ? operation.network() : offer.network()).stripTrailingZeros().toPlainString() + "%",bx+91,requirementY+3,TEXT);
                            String duration = completed ? operation.completes() : (operation != null ? operation.total() - operation.days() : offer.days()) + "일";
                            int[] actionBounds = UiAssets.visibleBounds(AgencyOperation.sprite("GFX_launch_operation_button_anim"), actionX, actionY, 62, 18);
                            float durationRight = actionBounds[0] - 2.5f, durationWidth = font.width(duration) * UiText.scale(font);
                            float durationX = durationRight - durationWidth, overflow = durationWidth - (durationRight - bx - 127);
                            if (overflow > 0) {
                                double phase = net.minecraft.util.Util.getMillis() % 6000 / 3000.0;
                                durationX = bx + 127 - (float)(overflow * (phase <= 1 ? phase : 2 - phase));
                            }
                            g.enableScissor(bx + 127, actionY, (int)Math.ceil(durationRight), actionY + 18);
                            g.pose().pushMatrix();
                            g.pose().translate(durationX, actionY + 4);
                            UiText.text(g,font,duration,0,0,GOLD);
                            g.pose().popMatrix();
                            g.disableScissor();
                        } else UiText.marquee(g,font,Tooltips.officerText(item.value()),textX,actionY+4,actionX-textX-6,UiText.scale(font),GOLD,true,-1);
                        UiAssets.draw(g, AgencyOperation.sprite("GFX_launch_operation_button_anim"), actionX, actionY, 62, 18);
                        String caption = operation == null ? "준비하기"
                                : completed ? item.button()
                                : operation.stage().equals("RUNNING") ? dev.hoi.client.ui.UiCommandLabels.text("OPERATION_VIEW_PROGRESS", "작전 중...")
                                : operation.stage().equals("PREPARED") ? dev.hoi.client.ui.UiCommandLabels.text("PREPARED", "준비됨")
                                : dev.hoi.client.ui.UiCommandLabels.text("OPERATION_VIEW_COLLECTING", "준비 중...");
                        UiText.centered(g, font, caption, actionX, actionY, 62, 18, TEXT);
                        g.disableScissor();
                        return;
                    }
                    if (item.group().equals("cryptology")) {
                        g.enableScissor(bx, y, bx + w, listBottom);
                        float scale = cryptoScale();
                        g.pose().pushMatrix(); g.pose().translate(bx, by); g.pose().scale(scale);
                        UiAssets.draw(g, AgencyOperation.sprite("GFX_cryptology_country_entry_bg"), 0, 0, 516, 87);
                        String country = item.id().contains(":") ? item.id().split(":", 2)[1] : "";
                        CountryFlags.draw(g, "country/" + country.toLowerCase(Locale.ROOT) + "/flag", "small2", 14, 13, 41, 26);
                        UiAssets.draw(g, AgencyOperation.sprite("GFX_small_flag_frame_thin"), 11, 11, 47, 32);
                        UiAssets.nineSlice(g, AgencyOperation.sprite("GFX_decrypt_button_bg"), 320, 15, 151, 53, 4, 1);
                        var barBackground = net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + AgencyOperation.sprite("GFX_crypto_bar_bg") + ".png");
                        g.blit(barBackground, 10, 38, 205, 75, 0, 195 / 271f, 0, 1);
                        g.blit(barBackground, 205, 38, 241, 75, 235 / 271f, 1, 0, 1);
                        UiAssets.draw(g, AgencyOperation.sprite("GFX_crypto_bar_background"), 16, 50, 188, 14);
                        int filled = Math.round(188 * (float)Math.clamp(item.progress(), 0, 1));
                        if (filled > 0) g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + AgencyOperation.sprite(item.progress() >= 1 ? "GFX_crypto_bar_done" : "GFX_crypto_bar_fill") + ".png"),
                                16, 50, 16 + filled, 64, 0, filled / 188f, 0, 1);
                        if (item.progress() >= 1) UiAssets.draw(g, AgencyOperation.sprite("GFX_crypto_decryption_done_icon"), 232, 38, 39, 36);
                        else UiAssets.drawGrayscale(g, AgencyOperation.sprite("GFX_crypto_decryption_done_icon"), 232, 38, 39, 36);
                        g.pose().popMatrix();
                        UiText.marquee(g, font, Tooltips.officerText(item.title()), bx + Math.round(60 * scale), by + Math.round(11 * scale), Math.round(200 * scale), UiText.scale(font), TEXT, true, -1);
                        UiText.centered(g, font, item.value(), bx + Math.round(15 * scale), by + Math.round(49 * scale), Math.round(190 * scale), Math.round(18 * scale), TEXT);
                        var cipher = view.ciphers().stream().filter(value -> value.country().equals(country)).findFirst().orElse(null);
                        if (cipher != null) {
                            String[] lines = cipher.activateText().split("\n", -1);
                            for (int line = 0; line < lines.length; line++) UiText.centered(g, font, lines[line], bx + Math.round(328 * scale), by + Math.round((26 + line * 16) * scale), Math.round(135 * scale), Math.round(16 * scale), TEXT);
                        }
                        var reveal = view.items().stream().filter(v -> v.id().equals("reveal:" + country)).findFirst().orElse(null);
                        if (cipher == null && reveal != null) UiText.centered(g, font, reveal.button() + "\n" + reveal.value(), bx + Math.round(328 * scale), by + Math.round(17 * scale), Math.round(135 * scale), Math.round(48 * scale), TEXT);
                        g.disableScissor();return;
                    }
                }
            };
            button.active = selectedId == null && !item.group().equals("cryptology"); addRenderableWidget(button);
            if (item.id().startsWith("dismiss_operation:")) {
                var complete = new UiMenuButton(item.button(), bx + areaWidth - 69, by + widgetHeight - 25, 62, 18, () -> operationAction(item.id()))
                        .background(AgencyOperation.sprite("GFX_launch_operation_button_anim"));
                complete.active = item.enabled() && pendingTicks == 0;
                complete.setTooltip(Tooltips.create(Component.literal(item.detail())));
                addRenderableWidget(complete);
            }
            if (item.id().startsWith("cipher:")) {
                var cipher = view.ciphers().stream().filter(value -> item.id().equals("cipher:" + value.country())).findFirst().orElseThrow();
                cipherButton(cipher.toggle(), cipher.toggleTooltip(), !cipher.toggle().isEmpty(), "GFX_crypto_decryption_start_stop_button", cipher.state().equals("paused") ? 1 : 0, bx, by, 205, 38, 36, 36, y, listBottom);
                cipherButton(cipher.activate(), cipher.activateTooltip(), cipher.activateEnabled(), "GFX_crypto_decryption_activate_button_animated", 0, bx, by, 280, 18, 48, 48, y, listBottom);
                cipherButton(cipher.delete(), cipher.deleteTooltip(), cipher.deleteEnabled(), "GFX_NV_btn_delete", 0, bx, by, 474, 24, 32, 32, y, listBottom);
                var status = new UiHoverRegion(cipher.status(), bx + Math.round(15 * cryptoScale()), Math.max(y, by + Math.round(49 * cryptoScale())), Math.round(190 * cryptoScale()), Math.max(1, Math.min(listBottom, by + Math.round(67 * cryptoScale())) - Math.max(y, by + Math.round(49 * cryptoScale()))));
                status.setTooltip(Tooltips.createExact(cipher.statusTooltip())); addRenderableWidget(status);
            }
            if ((item.id().startsWith("decrypt:") || item.id().startsWith("pause:")) && by + Math.round(38 * cryptoScale()) >= y && by + Math.round(74 * cryptoScale()) <= listBottom) {
                var toggle = new UiMenuButton(item.button(), bx + Math.round(205 * cryptoScale()), by + Math.round(38 * cryptoScale()), Math.round(36 * cryptoScale()), Math.round(36 * cryptoScale()), () -> selectItem(item));
                toggle.caption("").background("agency/operations/gfx_crypto_decryption_start_stop_button/frame_" + (item.id().startsWith("pause:") ? 1 : 0));
                toggle.setTooltip(Tooltips.create(Component.literal(item.detail())));
                toggle.active = item.enabled() && pendingTicks == 0; addRenderableWidget(toggle);
                var reveal = view.items().stream().filter(v -> v.id().equals("reveal:" + item.id().split(":", 2)[1])).findFirst().orElse(null);
                if (reveal != null) {
                    var action = new UiMenuButton("암호 공개", bx + Math.round(280 * cryptoScale()), by + Math.round(18 * cryptoScale()), Math.round(48 * cryptoScale()), Math.round(48 * cryptoScale()), () -> selectItem(reveal));
                    action.caption("").background(AgencyOperation.sprite("GFX_crypto_decryption_activate_button_animated"));
                    action.setTooltip(Tooltips.create(Component.literal(reveal.detail())));
                    action.active = reveal.enabled() && pendingTicks == 0; addRenderableWidget(action);
                }
            }
        }
        var item = selected();
        if (item != null) {
            int dx = detailX(), dw = detailWidth();
            addRenderableWidget(new UiPanelButton("닫기", dx + dw - 52, 44, 44, 20, () -> { selectedId = null; choosing = -1; rebuildWidgets(); }));
            if (choosing >= 0) { initChoices(item); return; }
            int bottom = height - 70;
            for (int i = 0; i < item.parameters().size(); i++) {
                final int index = i; var parameter = item.parameters().get(i); String value = arguments.get(i);
                String name = parameter.choices().stream().filter(c -> c.value().equals(value)).map(Choice::label).findFirst().orElse("선택 없음");
                addRenderableWidget(new UiPanelButton(parameter.label() + ": " + name, dx + 10, bottom - (item.parameters().size() - i) * 25, dw - 20, 22,
                        () -> { choosing = index; searchText = ""; choiceScroll = 0; rebuildWidgets(); }));
            }
            if (!item.button().isBlank()) {
                var action = new UiPanelButton(pendingTicks > 0 ? "응답 대기…" : item.button(), dx + dw / 2 - 65, bottom, 130, 24,
                        () -> send(AgencyPayloads.Kind.CALL, item.id(), List.copyOf(arguments)));
                action.active = item.enabled() && pendingTicks == 0 && arguments.stream().noneMatch(String::isEmpty);
                addRenderableWidget(action);
            }
        }
    }
    private void initChoices(Item item) {
        int dx = detailX(), dw = detailWidth();
        if (search == null) {
            search = new EditBox(font, dx + 12, 84, dw - 92, 20, Component.literal("검색"));
            search.setMaxLength(128);
            search.setResponder(value -> { searchText = value; choiceScroll = 0; rebuildWidgets(); });
        }
        search.setPosition(dx + 12, 84); search.setWidth(dw - 92);
        if (!search.getValue().equals(searchText)) search.setValue(searchText);
        addRenderableWidget(search); setInitialFocus(search);
        addRenderableWidget(new UiPanelButton("뒤로", dx + dw - 74, 84, 62, 20, () -> { choosing = -1; rebuildWidgets(); }));
        var choices = matchingChoices(item);
        int count = Math.max(1, (height - 163) / 25);
        choiceScroll = Math.clamp(choiceScroll, 0, Math.max(0, choices.size() - count));
        for (int i = choiceScroll; i < Math.min(choices.size(), choiceScroll + count); i++) {
            var choice = choices.get(i);
            addRenderableWidget(new UiPanelButton(choice.label(), dx + 12, 113 + (i - choiceScroll) * 25, dw - 24, 22,
                    () -> { arguments.set(choosing, choice.value()); choosing = -1; rebuildWidgets(); }));
        }
    }
    private List<Choice> matchingChoices(Item item) {
        String query = searchText.toLowerCase(Locale.ROOT);
        return item.parameters().get(choosing).choices().stream().filter(c -> (c.label() + " " + c.value()).toLowerCase(Locale.ROOT).contains(query)).toList();
    }
    private Item selected() { return view == null ? null : view.items().stream().filter(i -> i.id().equals(selectedId)).findFirst().orElse(null); }
    private List<Item> visibleItems() {
        if (group.equals("cryptology") && !view.ciphers().isEmpty()) return view.ciphers().stream().map(cipher ->
                new Item("cipher:" + cipher.country(), "cryptology", cipher.name(), cipher.status(), cipher.statusTooltip(), "", "", false, cipher.progress(), List.of())).toList();
        return view.items().stream().filter(i -> i.group().equals(group) && !i.id().equals("cancel_project") && !i.id().equals("cipher_level") && !i.id().equals("spy_master") && !i.id().startsWith("reveal:")
                && !i.id().startsWith("auto_commence:") && !i.id().startsWith("auto_repeat:") && !i.id().startsWith("commence_operation:")
                && !(i.id().startsWith("cancel_operation:") && view.items().stream().anyMatch(o -> o.id().equals("dismiss_operation:" + i.id().substring("cancel_operation:".length()))))).sorted(group.equals("upgrades") ? Comparator.comparingInt(AgencyScreen::upgradeOrder) : Comparator.comparingInt(i -> 0)).toList();
    }
    private void changeGroup(String next) { capitalMap.clear(); queuedUpgrade = null; group = next; selectedId = null; choosing = -1; scroll = 0; rebuildWidgets(); }
    private void selectItem(Item item) {
        if ((item.id().startsWith("upgrade:") || item.group().equals("cryptology")) && item.parameters().isEmpty()) {
            if (item.enabled()) {
                if (pendingTicks == 0) send(AgencyPayloads.Kind.CALL, item.id(), List.of());
                else if (pendingKind == AgencyPayloads.Kind.REFRESH && item.id().startsWith("upgrade:") && queuedUpgrade == null) queuedUpgrade = item.id();
            }
            return;
        }
        if (item.id().startsWith("operation:") || view.operations().stream().anyMatch(operation -> operation.item().equals(item.id()))) {
            minecraft.gui.setScreen(new AgencyOperation(this, item, args -> send(AgencyPayloads.Kind.CALL, item.id(), args)));
            return;
        }
        selectedId = item.id(); arguments.clear(); normalizeArguments(item); detailScroll = 0; rebuildWidgets();
    }
    private int upgradeWidth() { return Math.min(540, width - 16); }
    private int upgradeHeight() { return Math.min(394, height - top - 12); }
    private int upgradeColumns() { return Math.max(1, Math.min(5, (upgradeWidth() - 16) / 100)); }
    private int upgradeX() { return (width - upgradeWidth()) / 2; }
    private int upgradeY() { return Math.max(top + 4, (height - upgradeHeight()) / 2); }
    static int upgradeOrder(Item item) {
        int index = UPGRADE_ORDER.indexOf(item.id().replace("upgrade:", "").toLowerCase(Locale.ROOT));
        return index < 0 ? UPGRADE_ORDER.size() : index;
    }
    static int upgradeStages(Item item) {
        if (!item.id().startsWith("upgrade:")) return 0;
        int slash = item.value().indexOf('/');
        if (slash < 0) return 0;
        try {
            int count = Integer.parseInt(item.value().substring(slash + 1).trim());
            return count > 0 && count <= 8 ? count : 0;
        } catch (NumberFormatException ignored) { return 0; }
    }
    static int upgradeLevel(Item item) {
        int slash = item.value().indexOf('/');
        if (slash < 0) return 0;
        try { return Math.clamp(Integer.parseInt(item.value().substring(0, slash).trim()), 0, upgradeStages(item)); }
        catch (NumberFormatException ignored) { return 0; }
    }
    static String upgradeIndicator(Item item) {
        int maximum = upgradeStages(item);
        return maximum < 1 || maximum > 4 ? "" : "agency/researched/" + (maximum * (maximum + 1) / 2 - 1 + upgradeLevel(item));
    }
    static int upgradeCategory(Item item) {
        int index = upgradeOrder(item);
        return index < 4 ? 0 : index < 9 ? 1 : index < 14 ? 2 : index < 19 ? 3 : 4;
    }
    private static int upgradePosition(List<Item> items, int index, int columns) {
        int pos = 0;
        for (int i = 0; i < index; i++) {
            pos++;
            if (upgradeCategory(items.get(i)) != upgradeCategory(items.get(i + 1))) pos = (pos + columns - 1) / columns * columns;
        }
        return pos;
    }
    private static int upgradeOffset(List<Item> items, int index, int columns) {
        int headers = 1;
        for (int i = 1; i <= index; i++) if (upgradeCategory(items.get(i - 1)) != upgradeCategory(items.get(i))) headers++;
        return upgradePosition(items, index, columns) / columns * 55 + headers * 14;
    }
    private int detailX() { return Math.min(pane + 12, width - 260); }
    private int detailWidth() { return Math.min(540, width - detailX() - 12); }
    private void send(AgencyPayloads.Kind kind, String item, List<String> args) {
        if (pendingTicks > 0 && kind == AgencyPayloads.Kind.CALL) return;
        boolean passive = kind == AgencyPayloads.Kind.REFRESH;
        pendingTicks = 100; pendingKind = kind;
        transport.accept(new AgencyPayloads.Request(kind, token, view == null ? 0 : view.revision(), item, args));
        if (!passive) rebuildWidgets();
    }
    @Override public void tick() {
        if (pendingTicks > 0 && --pendingTicks == 0) {
            send(view == null ? AgencyPayloads.Kind.OPEN : AgencyPayloads.Kind.REFRESH, "", List.of());
        } else if (++refreshTicks % 60 == 0 && pendingTicks == 0 && choosing < 0 && selectedId == null) send(AgencyPayloads.Kind.REFRESH, "", List.of());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (group.equals("cryptology") && selectedId == null && !cryptologyLocked()) capitalMap.draw(g, view, pane, top);
        else capitalMap.clear();
        if (group.equals("operations") && selectedId == null) operationMap.draw(g, view, pane, top);
        else operationMap.clear();
        UiMenuStyle.window(g, WINDOW, frame, height);
        HudMenuBar.draw(g, width, dev.hoi.client.ClientCampaignHud.hud(), mx, my, 0);
        UiMenuStyle.title(g, font, WINDOW, frame, "정보기관");
        if (unestablished()) {
            drawUnestablished(g); super.extractRenderState(g,mx,my,delta);
            frame.art(g,"agency/ui/opacity_layer",15,195,520,131);
            g.enableScissor(0,frame.iy(322),width,frame.iy(400));frame.art(g,"agency/ui/opacity_layer",15,322,520,121);g.disableScissor();
            return;
        }
        String flag = view == null ? "menu/intelligence" : Set.of("KOR", "PRC").contains(view.country()) ? "country/" + view.country().toLowerCase(Locale.ROOT) + "/intelligence" : "menu/intelligence";
        frame.art(g, "agency/ui/header", 15, 47);
        int[] logo = UiAssets.imageSize(flag);
        if (logo[0] == 116 && logo[1] == 119) frame.art(g, flag, 16, 53, 104.4, 107.1);
        else UiAssets.drawContent(g, flag, frame.ix(16 + 16), frame.iy(53 + 16), frame.len(72), frame.len(75));
        var agencyName = Component.literal(view == null ? "불러오는 중…" : view.name()).withStyle(net.minecraft.ChatFormatting.BOLD);
        UiText.marquee(g, font, agencyName, frame.x(125), frame.y(69.5) - font.lineHeight / 2f, frame.size(340), 1, TEXT, true, -1);
        frame.art(g, "agency/ui/counter_intelligence", 123, 106);
        String defense = view == null ? "—" : view.items().stream().filter(i -> i.id().equals("counter_intelligence"))
                .map(Item::value).findFirst().orElse("—");
        UiText.marquee(g, font, Tooltips.styled(Component.literal("방첩 활동: §Y" + defense + "§!"), false), frame.x(153), frame.y(116) - font.lineHeight / 2f, frame.size(165), 1, TEXT, true, -1);
        if (!unestablished()) frame.art(g, "agency/spy_master", 480, 61);
        drawOperationsChrome(g);
        frame.tile(g, AgencyOperation.sprite("GFX_tiled_paper_bg2"), 15, 450, 520, windowHeight() - 460, 64, UiMenuStyle.TILE);
        if (view != null && group.equals("cryptology")) {
            view.items().stream().filter(i -> i.id().equals("cipher_level")).findFirst().ifPresent(i -> {
                frame.art(g, "keywords/agency_crypto_defense", 33, 468);
                UiText.marquee(g, font, Tooltips.officerText(view.ciphers().isEmpty() ? i.title() + " " + i.value() : i.title()), frame.x(60), frame.y(478) - font.lineHeight / 2f, frame.size(330), 1, TEXT, true, -1);
            });
            view.items().stream().filter(i -> i.id().equals("decryption_power")).findFirst().ifPresent(i -> {
                float valueWidth = Math.min(font.width(i.value()), frame.size(100));
                frame.art(g, "keywords/agency_crypto", (frame.x(515) - valueWidth - frame.x(0)) / frame.scale() - 20, 468);
                UiText.marquee(g, font, Tooltips.officerText(i.value()), frame.x(415), frame.y(478) - font.lineHeight / 2f, frame.size(100), 1, TEXT, true, 1);
            });
        }
        if (group.equals("cryptology") && cryptologyLocked())
            wrappedNotice(g, "복호화는 전자정보가 정보부에\n업그레이드되어 있어야 가능합니다.", frame.ix(150), frame.iy(550), frame.len(280));
        if (view != null) for (int i = 0; i < UPGRADE_GROUPS.length; i++) {
            var upgrades = branchUpgrades(i);
            for (int j = 0; j < upgrades.size(); j++)
                frame.art(g, upgradeIndicator(upgrades.get(j)), 22 + 102 * i + 19 * j, 251, 21, 21);
        }
        boolean overlay = view != null && group.equals("upgrades");
        if (overlay) {
            for (int i = 0; i < overlayStart; i++) if (children().get(i) instanceof net.minecraft.client.gui.components.Renderable widget)
                widget.extractRenderState(g, mx, my, delta);
            g.nextStratum();
        }
        if (view != null && group.equals("upgrades") && selectedId == null) {
            int ux = upgradeX(), uw = upgradeWidth();
            UiMenuStyle.panel(g, ux, upgradeY(), uw, upgradeHeight());
            UiAssets.cover(g, "agency/ui/upgrade_background", ux, upgradeY(), uw, 43);
            g.pose().pushMatrix();
            g.pose().translate(ux + uw / 2f, upgradeY() + 11);
            g.pose().scale(1.35f);
            g.centeredText(font, Component.literal("첩보기관 개선").withStyle(net.minecraft.ChatFormatting.BOLD), 0, -font.lineHeight / 2, TEXT);
            g.pose().popMatrix();
            UiText.centeredAt(g, font, Tooltips.styled(Component.literal("기관 개선은 §630§r일 후 완료됩니다."), false), ux + uw / 2, upgradeY() + 27, TEXT);
            var items = visibleItems(); int columns = upgradeColumns();
            String[] titles = {"정보공동체", "정보 순환", "정보 수집 분야", "휴민트", "신호 정보"};
            for (int i = 0; i < items.size(); i++) if (i == 0 || upgradeCategory(items.get(i - 1)) != upgradeCategory(items.get(i))) {
                int by = upgradeY() + 44 + upgradeOffset(items, i, columns) - scroll - 14;
                if (by < upgradeY() + 34 || by + 14 > upgradeY() + upgradeHeight() - 3) continue;
                UiAssets.cover(g, "agency/ui/branch", ux + 8, by, uw - 16, 69);
                UiText.text(g, font, Tooltips.officerText(titles[upgradeCategory(items.get(i))], true), ux + 13, by + Math.round((14 - font.lineHeight * UiText.scale(font)) / 2f), TEXT);
            }
        }
        var item = selected();
        if (item != null) {
            int dx = detailX(), dw = detailWidth();
            g.fillGradient(dx, 38, dx + dw, height - 36, 0xFF343D46, 0xFF10151B); g.outline(dx, 38, dw, height - 74, 0xFF88919A);
            UiText.marquee(g, font, Tooltips.officerText(item.title()), dx + 10, 51, dw - 73, UiText.scale(font), GOLD, true, -1);
            if (choosing < 0) {
                if (!UiAssets.draw(g, item.texture(), dx + 12, 77, 72, 58)) UiAssets.draw(g, "menu/intelligence", dx + 12, 77, 72, 58);
                UiText.marquee(g, font, Tooltips.officerText(item.value()), dx + 97, 86, dw - 109, UiText.scale(font), TEXT, true, -1);
                if (item.group().equals("operations") && dw >= 360) {
                    String[] phases = {"phase_border", "phase_bribe", "phase_escape"};
                    int artWidth = Math.min(70, (dw - 124) / 3);
                    for (int i = 0; i < phases.length; i++) UiAssets.draw(g, "agency/" + phases[i], dx + 98 + i * (artWidth + 5), 104, artWidth, 37);
                }
                int bottom = height - 82 - item.parameters().size() * 25;
                var lines = Tooltips.split(font,Component.literal(item.detail()), dw - 26);
                detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - Math.max(0, bottom - 148)));
                g.enableScissor(dx + 10, 146, dx + dw - 10, Math.max(146, bottom));
                int y = 148 - detailScroll;
                for (var line : lines) { UiText.text(g, font, line, dx + 13, y, TEXT); y += 13; }
                g.disableScissor();
            }
        }
        if (overlay) {
            for (int i = overlayStart; i < children().size(); i++) if (children().get(i) instanceof net.minecraft.client.gui.components.Renderable widget)
                widget.extractRenderState(g, mx, my, delta);
        } else super.extractRenderState(g, mx, my, delta);
        if (group.equals("cryptology") && selectedId == null) cryptoBar().draw(g, scroll);
        drawHover(g, mx, my);
        capitalMap.tooltip(g, mx, my);
        operationMap.tooltip(g, mx, my);
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (view != null && group.equals("operations") && selectedId == null
                && operationMap.click(event.x(), event.y(), event.button(), this::showMapItem)) return true;
        if (group.equals("cryptology") && selectedId == null && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && cryptoBar().contains(event.x(), event.y())) {
            cryptoScrolling = !cryptoBar().arrow(event.y());
            scroll = cryptoScrolling ? cryptoBar().offset(event.y()) : cryptoBar().step(event.y(), scroll, cryptoPitch());
            rebuildWidgets();return true;
        }
        var hit = capitalMap.at(event.x(), event.y());
        if (hit != null && group.equals("cryptology") && selectedId == null && !cryptologyLocked()) {
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && pendingTicks == 0) {
                if (!hit.capital().item().isEmpty()) cipherAction(hit.capital().item());
                else if (hit.item() != null) view.items().stream().filter(item -> item.id().equals(hit.item().id()) && item.enabled()).findFirst().ifPresent(item -> {
                    dev.hoi.client.audio.AudioUiSounds.play("ui.agency.decrypt.select");
                    selectItem(item);
                });
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    private boolean cipherActionEnabled(String id) {
        return view != null && !id.isEmpty() && (view.capitals().stream().anyMatch(capital -> capital.item().equals(id))
                || view.ciphers().stream().anyMatch(cipher -> cipher.toggle().equals(id)
                || cipher.activate().equals(id) && cipher.activateEnabled() || cipher.delete().equals(id) && cipher.deleteEnabled()));
    }
    private void cipherAction(String id) {
        if (pendingTicks > 0 || !cipherActionEnabled(id)) return;
        var confirm = view.confirms().stream().filter(value -> value.item().equals(id)).findFirst().orElse(null);
        if (confirm == null) { send(AgencyPayloads.Kind.CALL, id, List.of()); return; }
        long revision = view.revision();
        String country = view.country();
        var dialog = new DialogView(token, revision, DialogView.Kind.COMMANDER, confirm.title(), "", confirm.body(), "", "", "", List.of(),
                List.of(new DialogView.Choice("cancel", UiCommandLabels.text("PDXO_CANCEL", "취소"), ""), new DialogView.Choice("ok", "OK", "")));
        var screen = new CommanderConfirmation(dialog, (ignored, choice) -> {
            if (!country.equals(dev.hoi.client.ClientCampaignHud.country())) { minecraft.gui.setScreen(null); return; }
            if (choice.equals("ok") && view.confirms().contains(confirm) && pendingTicks == 0 && cipherActionEnabled(id)) send(AgencyPayloads.Kind.CALL, id, List.of());
            minecraft.gui.setScreen(this);
        });
        screen.backdrop(this); minecraft.gui.setScreen(screen);
    }
    private void cipherButton(String id, String tooltip, boolean enabled, String sprite, int frame, int bx, int by, int x, int y, int w, int h, int clipTop, int clipBottom) {
        float scale = cryptoScale();
        int left = bx + Math.round(x * scale), rowTop = by + Math.round(y * scale), rowBottom = by + Math.round((y + h) * scale);
        int visibleTop = Math.max(clipTop, rowTop), visibleBottom = Math.min(clipBottom, rowBottom);
        if (visibleBottom <= visibleTop) return;
        String asset = "agency/operations/" + sprite.toLowerCase(Locale.ROOT) + "/frame_" + frame;
        var button = new UiPanelButton(id, left, visibleTop, Math.round(w * scale), visibleBottom - visibleTop, () -> cipherAction(id)) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                g.enableScissor(left, visibleTop, getRight(), visibleBottom);
                if (enabled) UiAssets.draw(g, asset, left, rowTop, getWidth(), rowBottom - rowTop);
                else UiAssets.drawGrayscale(g, asset, left, rowTop, getWidth(), rowBottom - rowTop);
                g.disableScissor();
            }
        };
        button.active = enabled && pendingTicks == 0;
        button.setTooltip(Tooltips.createExact(tooltip)); addRenderableWidget(button);
    }
    private int windowHeight() { return (int)Math.ceil((height - frame.y()) / frame.scale()); }
    private int listBottom() { return Math.min(height, frame.iy(windowHeight() - 10)); }
    private UiScrollBar cryptoBar() { return new UiScrollBar(frame.ix(512), frame.iy(505), listBottom(), Math.max(1, listBottom() - frame.iy(505)), frame.len(visibleItems().size() * 83)); }
    private float cryptoScale() { return frame.scale(); }
    private int cryptoPitch() { return frame.len(83); }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (cryptoScrolling) { scroll = cryptoBar().offset(event.y());rebuildWidgets();return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (cryptoScrolling) { cryptoScrolling = false;return true; }
        return super.mouseReleased(event);
    }
    private void drawHover(GuiGraphicsExtractor g, int mx, int my) {
        if (view == null || selectedId != null) return;
        if (group.equals("upgrades")) {
            for (var child : children()) if (child instanceof AgencyUpgradeButton button && button.isMouseOver(mx, my)) {
                Tooltips.drawExact(g, font, button.item().detail(), mx, my); return;
            }
            return;
        }
        String sidebar = null;
        if (group.equals("cryptology") && my >= frame.y(460) && my < frame.y(496) && mx >= frame.x(15) && mx < frame.x(535))
            sidebar = mx < frame.x(395) ? "cipher_level" : "decryption_power";
        else if (mx >= frame.x(123) && mx < frame.x(318) && my >= frame.y(106) && my < frame.y(133)) sidebar = "counter_intelligence";
        else if (mx >= frame.x(480) && mx < frame.x(529) && my >= frame.y(61) && my < frame.y(110)) sidebar = "spy_master";
        else if (mx >= frame.x(288) && mx < frame.x(511) && my >= frame.y(363) && my < frame.y(393)) sidebar = "recruit_days";
        else for (int i = 0; i < 3; i++) if (mx >= frame.x(COUNTERS[i][2]) && mx < frame.x(COUNTERS[i][2] + COUNTERS[i][3]) && my >= frame.y(COUNTERS[i][1]) && my < frame.y(389))
            sidebar = i == 0 ? "recruit" : i == 1 ? "captured_count" : "killed_count";
        if (sidebar != null) {
            String id = sidebar;
            view.items().stream().filter(i -> i.id().equals(id) && !i.detail().isBlank()).findFirst()
                    .ifPresent(i -> Tooltips.drawExact(g, font, i.detail(), mx, my));
            return;
        }
        for (int category = 0; category < UPGRADE_GROUPS.length; category++) {
            var upgrades = branchUpgrades(category);
            for (int j = 0; j < upgrades.size(); j++) {
                float x = frame.x(22 + 102 * category + 19 * j);
                if (my >= frame.y(251) && my < frame.y(272) && mx >= x && mx < x + frame.size(j == upgrades.size() - 1 ? 21 : 19)) {
                    Tooltips.drawExact(g, font, upgrades.get(j).detail(), mx, my); return;
                }
            }
        }
        if (mx >= frame.x(15) && mx < frame.x(535) && my > frame.y(450)) {
            if (group.equals("operations") && visibleItems().isEmpty())
                Tooltips.drawExact(g, font, "요원을 모집하여 작전을 활성화", mx, my);
            else if (group.equals("cryptology") && cryptologyLocked())
                Tooltips.drawExact(g, font, "암호 해독을 활성화하려면 전자정보 업그레이드를\n완료하세요.", mx, my);
        }
    }
    private boolean cryptologyLocked() {
        return view != null && view.items().stream().anyMatch(i -> i.id().equals("cryptology_unlocked") && i.value().equals("false"));
    }
    private static final int[][] COUNTERS = {{50, 325, 40, 60}, {126, 327, 127, 40}, {195, 327, 194, 40}};
    private List<Item> branchUpgrades(int category) {
        return view.items().stream().filter(item -> item.group().equals("upgrades") && !item.id().equals("cancel_project") && upgradeCategory(item) == category)
                .sorted(Comparator.comparingInt(AgencyScreen::upgradeOrder)).toList();
    }
    private UiMenuButton branchButton(int index, Runnable action) {
        var button = new UiMenuButton(UPGRADE_GROUPS[index], frame.ix(21 + 102 * index), frame.iy(195), frame.len(99), frame.len(57), action)
                .background("agency/ui/category").art(frame.x(21 + 102 * index), frame.y(195), frame.size(99), frame.size(80));
        button.textScale(.8f);
        return button;
    }
    private UiMenuButton tabButton(int index) {
        String id = index == 0 ? "operations" : "cryptology";
        return new UiMenuButton(index == 0 ? "작전" : "암호학", null, group.equals(id), frame.ix(17 + 256 * index), frame.iy(400), frame.len(262), frame.len(53), () -> changeGroup(id))
                .background("agency/ui/tab" + index).art(frame.x(17 + 256 * index), frame.y(400), frame.size(262), frame.size(53));
    }
    private void sectionHeader(GuiGraphicsExtractor g, String text, int y) {
        frame.art(g, "agency/ui/section", 10, y);
        UiText.marquee(g, font, Tooltips.officerText(text, true), frame.x(27), frame.y(y + 19) - font.lineHeight / 2f, frame.size(160), 1, TEXT, true, 0);
    }
    private void drawOperationsChrome(GuiGraphicsExtractor g) {
        sectionHeader(g, "정보기관", 153);
        sectionHeader(g, "작전", 283);
        frame.art(g, "agency/ui/operatives", 13, 322);
        if(view == null)return;
        String[] ids={"recruit","captured_count","killed_count"};
        for(int i=0;i<ids.length;i++) {
            String id=ids[i];
            String value=view.items().stream().filter(v->v.id().equals(id)).map(Item::value).findFirst().orElse(i==0?"0/0":"0");
            UiText.marquee(g,font,Component.literal(value.replace(" ","")),frame.x(COUNTERS[i][2]),UiText.centeredY(font,value,frame.y(379)-font.lineHeight/2f,font.lineHeight,1),frame.size(COUNTERS[i][3]),1,TEXT,true,0);
        }
        if(!unestablished() && (group.equals("operations") || group.equals("upgrades")) && view.items().stream().noneMatch(i -> i.group().equals("operations")))
            wrappedNotice(g,"작전은 정보원이 첩보망을 구축하여야\n시행할 수 있습니다.",frame.ix(150),frame.iy(550),frame.len(280));
    }
    private UiPanelButton agencyGauge(String accessible, String label, String days, int sourceY, double progress, Runnable action) {
        return new UiPanelButton(accessible, frame.ix(288), frame.iy(sourceY), frame.len(223), frame.len(30), action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                frame.art(g, "agency/ui/progress_empty", 291, sourceY + 2, 218, 21);
                double fraction = Math.clamp(progress, 0, 1);
                if (fraction > 0) {
                    g.pose().pushMatrix(); g.pose().translate(frame.x(291), frame.y(sourceY + 2)); g.pose().scale(frame.scale());
                    g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/agency/ui/progress_fill.png"), 0, 0, (int)Math.round(218 * fraction), 21, 0, (float)fraction, 0, 1);
                    g.pose().popMatrix();
                }
                frame.art(g, "agency/ui/progress_frame", 288, sourceY);
                float center = frame.y(sourceY + 15) - font.lineHeight / 2f;
                float daysWidth = days.equals("준비 완료!") ? frame.size(48) : font.width(days);
                UiText.marquee(g, font, dev.hoi.client.ui.tooltip.Tooltips.officerText(label), frame.x(296), center, Math.max(1, frame.size(207) - daysWidth - (days.isEmpty() ? 0 : 4)), 1, TEXT, true, -1);
                if (days.equals("준비 완료!")) {
                    UiText.centeredSingleLine(g, font, Component.literal("준비"), Math.round(frame.x(503) - daysWidth), frame.iy(sourceY + 1), Math.round(daysWidth), frame.len(14), GOLD);
                    UiText.centeredSingleLine(g, font, Component.literal("완료!"), Math.round(frame.x(503) - daysWidth), frame.iy(sourceY + 15), Math.round(daysWidth), frame.len(14), GOLD);
                }
                else UiText.marquee(g, font, Component.literal(days), frame.x(503) - daysWidth, UiText.centeredY(font, days, center, font.lineHeight, 1), daysWidth, 1, GOLD, true, -1);
            }
        };
    }
    private boolean unestablished() { return view != null && view.items().stream().noneMatch(i -> i.id().equals("recruit")); }
    private void drawUnestablished(GuiGraphicsExtractor g) {
        var project=view.items().stream().filter(i->i.id().equals("cancel_project")).findFirst().orElse(null);
        boolean available=project!=null||view.items().stream().anyMatch(i->i.id().equals("create")&&i.enabled());
        frame.art(g,"agency/ui/header",15,47);
        frame.art(g,available?"agency/ui/create":"agency/ui/create_disabled",25,56);
        UiText.marquee(g,font,Tooltips.officerText(project==null?"기관 창설":"기관 창설 중",true),frame.x(25),frame.y(85)-font.lineHeight/2f,frame.size(508),1,TEXT,true,0);
        frame.art(g,"construction/summary/civilian_factory",87,109,21.6,18.4);
        UiText.marquee(g,font,Component.literal("5"),frame.x(112),frame.y(118)-font.lineHeight/2f,frame.size(117),1,available?0xFFE0E3DD:0xFF000000|Tooltips.BAD,true,-1);
        String days=project==null?"30":project.value().substring(project.value().lastIndexOf('·')+1).replace("일 남음", "").trim();
        UiText.marquee(g,font,Tooltips.styled(Component.literal("§Y"+days+"§!일"),false),frame.x(307),frame.y(118)-font.lineHeight/2f,frame.size(155),1,TEXT,true,1);
        frame.art(g,"research/slot/gfx_research_progressbar_frame/frame_0",87,131);
        frame.art(g,"research/slot/gfx_research_progressbar_background/frame_0",89,133,377,4);
        if(project!=null&&project.progress()>0) {
            double fraction=Math.clamp(project.progress(),0,1);
            g.pose().pushMatrix();g.pose().translate(frame.x(89),frame.y(133));g.pose().scale(frame.scale());
            g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/research/slot/gfx_research_progressbar_fill/frame_0.png"),0,0,(int)Math.round(377*fraction),4,0,(float)fraction,1/3f,2/3f);
            g.pose().popMatrix();
        }
        drawOperationsChrome(g);
        frame.tile(g, AgencyOperation.sprite("GFX_tiled_paper_bg2"), 15, 450, 520, windowHeight() - 460, 64, UiMenuStyle.TILE);
        wrappedNotice(g,"기관을 창설하기 전에는 정보원을 모집할 수 없습니다.",frame.ix(290),frame.iy(337),frame.len(240));
        wrappedNotice(g,group.equals("cryptology")?"기관을 창설하면 암호 해독을 시작할 수 있습니다.":"작전은 정보원이 첩보망을 구축하여야\n시행할 수 있습니다.",frame.ix(150),frame.iy(550),frame.len(280));
    }
    private void wrappedNotice(GuiGraphicsExtractor g,String value,int x,int y,int width) {
        var lines=Tooltips.split(font,Component.literal(value),Math.max(1,(int)(width/UiText.scale(font))));
        for(int i=0;i<lines.size();i++) UiText.text(g, font,lines.get(i),x+Math.round((width-font.width(lines.get(i))*UiText.scale(font))/2),y+i*(font.lineHeight+2),TEXT);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (choosing >= 0) choiceScroll -= (int)(vertical * 3);
        else if (selectedId != null) { detailScroll -= (int)(vertical * 26); return true; }
        else scroll -= (int)(vertical * 51);
        rebuildWidgets(); return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && selectedId != null) {
            if (choosing >= 0) choosing = -1; else selectedId = null;
            rebuildWidgets(); return true;
        }
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && group.equals("upgrades")) { changeGroup("operations"); return true; }
        if (allowsMovement() && InputSidebarMovement.consumes(minecraft, event)) return true;
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (allowsMovement() && InputSidebarMovement.consumes(minecraft, event)) return true;
        return super.keyReleased(event);
    }
    @Override public void onClose() {
        transport.accept(new AgencyPayloads.Request(AgencyPayloads.Kind.CLOSE, token, view == null ? 0 : view.revision(), "", List.of()));
        minecraft.gui.setScreen(parent);
    }
    @Override public void removed() { InputSidebarMovement.release(minecraft); }
    @Override public int unitHudLeft(){return choosing<0?pane:width;}
    @Override public boolean allowsMovement() { return selectedId == null && !group.equals("upgrades"); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
