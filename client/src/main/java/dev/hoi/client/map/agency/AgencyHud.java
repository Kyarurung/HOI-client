package dev.hoi.client.map.agency;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.screen.agency.AgencyScreen;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.UiTooltips;
import dev.hoi.client.ui.UiCountryFlags;
import dev.hoi.client.ui.UiCountryDisplay;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.agency.AgencyPayloads;
import dev.hoi.protocol.agency.AgencyView;
import dev.hoi.client.map.commander.CommanderOrders;
import dev.hoi.client.map.scene.SceneAssembler;
import dev.hoi.client.map.scene.SceneMesh;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AgencyHud {
    private record Hit(int x, int y, int width, int height, String operative, String item, String tooltip) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    private static String session = "", country = "", selected = "", dimension = "";
    private static AgencyView view;
    private static List<Hit> hits = List.of();
    private static int ticks, cooldown, page;
    private static long selectedAt;
    private static UiTooltips.ScrolledTooltip tooltip;
    private static String hovered = "";
    private static int tooltipScroll;
    private static String targeting = "", targetRay = "", targetingSession = "", targetingDimension = "";
    private static int highlightCooldown;
    private static boolean highlightQueued;
    private static final SceneAssembler HIGHLIGHT = new SceneAssembler();
    private static SceneMesh highlight;
    private static long targetingRevision = -1, pendingRevision = -1;
    private static java.util.function.Consumer<AgencyPayloads.Request> transport = request -> {
        if (ClientPlayNetworking.canSend(AgencyPayloads.Request.TYPE)) ClientPlayNetworking.send(request);
    };
    private static final AgencyOperations OPERATIONS = new AgencyOperations();
    private static final String[] MISSIONS = {"COUNTER_INTELLIGENCE", "BUILD_NETWORK", "QUIET_NETWORK", "BOOST_IDEOLOGY", "PROPAGANDA", "ROOT_OUT_RESISTANCE", "CONTROL_TRADE", "DIPLOMATIC_PRESSURE"};
    private static final String[] MISSION_TITLES = {"방첩 활동", "첩보망 구축", "첩보망 은폐", "이념 지지", "선전", "저항군 소탕", "무역 통제", "외교적 압력"};
    private static final int[] MISSION_FRAMES = {2, 0, 1, 4, 7, 3, 5, 6}, MISSION_X = {5, 63, 109, 155, 201, 258, 304, 350};
    private AgencyHud() {}
    public static boolean active() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !ClientCampaignHud.visible()
                || !client.level.dimension().identifier().toString().startsWith("hoi:atlas_")) return false;
        var model = client.player.getMainHandItem().get(DataComponents.ITEM_MODEL);
        return Identifier.parse("hoi:map/selector/operatives").equals(model);
    }
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Identifier.parse("hoi:agency_hud"), (g, delta) -> {
            if (Minecraft.getInstance().gui.screen() == null) draw(g, 0, g.guiWidth() / 2, g.guiHeight() / 2);
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof InputSidebarMovement.InputMovementScreen sidebar) {
                ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> {
                    if (sidebar.allowsMovement() || screen instanceof dev.hoi.client.screen.map.MapCursor) draw(g, sidebar.unitHudLeft(), mx, my);
                });
            }
        });
    }
    private static void send(AgencyPayloads.Kind kind) {
        if (ClientPlayNetworking.canSend(AgencyPayloads.Request.TYPE)) ClientPlayNetworking.send(
                new AgencyPayloads.Request(kind, session, view == null ? 0 : view.revision(), "", List.of()));
    }
    private static void tick() {
        if (!active()) { if (!session.isEmpty()) send(AgencyPayloads.Kind.CLOSE); reset(); cooldown = 0; return; }
        if (highlightCooldown > 0) highlightCooldown--;
        if (highlightQueued) requestHighlight();
        if (DialogCoordinator.contentScreen() instanceof AgencyScreen) { session = ""; view = null; ticks = 0; return; }
        if (cooldown > 0) { cooldown--; return; }
        var client = Minecraft.getInstance();
        String currentDimension = client.level.dimension().identifier().toString();
        if (!country.equals(ClientCampaignHud.country()) || !dimension.equals(currentDimension)) reset();
        if (session.isEmpty()) {
            country = ClientCampaignHud.country(); dimension = currentDimension; session = UUID.randomUUID().toString();
            send(AgencyPayloads.Kind.OPEN); ticks = 0;
        } else if (++ticks >= 40) { ticks = 0; send(AgencyPayloads.Kind.REFRESH); }
    }
    public static void accept(AgencyView next) {
        if (session.isEmpty() || !next.session().equals(session)) return;
        if (!active() || !next.country().equals(country) || !country.equals(ClientCampaignHud.country())) { reset(); cooldown = 100; return; }
        if (view != null && next.revision() <= view.revision()) return;
        if (pendingRevision >= 0 && !next.message().isEmpty()) CommanderOrders.message(next.message());
        pendingRevision = -1; clearTarget();
        view = next; hits = List.of();
        if (view.operatives().stream().noneMatch(operative -> operative.id().equals(selected))) selected = "";
    }
    public static void reset() { session = ""; country = ""; dimension = ""; selected = ""; clearTarget(); pendingRevision = -1; view = null; hits = List.of(); tooltip = null; hovered = ""; tooltipScroll = 0; OPERATIONS.clear(); ticks = 0; page = 0; }
    private static void clearTarget() { targeting = ""; targetRay = ""; targetingSession = ""; targetingDimension = ""; highlightQueued = false; highlight = null; HIGHLIGHT.clear(); }
    public static SceneMesh highlight() {
        var current = snapshot();
        var client = Minecraft.getInstance();
        if (!active() || current == null || !current.country().equals(ClientCampaignHud.country()) || targeting.isEmpty()
                || current.revision() != targetingRevision || !current.session().equals(targetingSession) || !client.level.dimension().identifier().toString().equals(targetingDimension) || highlight != null && !highlight.dimension().equals(client.level.dimension().identifier().toString())) clearTarget();
        return highlight;
    }
    public static void accept(AgencyPayloads.Highlight packet) {
        var current = snapshot();
        if (!active() || current == null || !current.country().equals(ClientCampaignHud.country()) || targeting.isEmpty()
                || !packet.session().equals(targetingSession) || !packet.page().dimension().equals(targetingDimension)
                || !packet.session().equals(current.session()) || packet.revision() != current.revision() || packet.revision() != targetingRevision
                || !packet.item().equals(targeting) || !packet.page().dimension().equals(Minecraft.getInstance().level.dimension().identifier().toString())) return;
        var boxes = HIGHLIGHT.accept(packet.page());
        if (boxes != null) highlight = SceneMesh.build(packet.page().dimension(), boxes);
    }
    public static boolean deselect() { if (!targeting.isEmpty()) { clearTarget(); return true; } boolean had = !selected.isEmpty(); selected = ""; return had; }
    public static boolean scrollAt(double x, double y, double amount) {
        if (active() && tooltip != null && tooltip.maxScroll() > 0) {
            tooltipScroll = Math.clamp(tooltipScroll - (int)(amount * 20), 0, tooltip.maxScroll()); return true;
        }
        if (!active() || hits.stream().noneMatch(hit -> hit.contains(x, y))) return false;
        page = Math.max(0, page + (amount < 0 ? 1 : amount > 0 ? -1 : 0)); return true;
    }
    public static AgencyView snapshot() {
        return DialogCoordinator.contentScreen() instanceof AgencyScreen agency ? agency.snapshot() : view;
    }
    public static void drawBackdrop(GuiGraphicsExtractor graphics) { draw(graphics, 0, -1, -1); }
    private static String art(AgencyView current, String source) {
        return "agency/operative/" + (source.toLowerCase(Locale.ROOT).contains("/kor/") ? "korea/" : "tfr/")
                + source.replace('\\', '/').replaceAll("\\.[^.]+$", "").toLowerCase(Locale.ROOT);
    }
    private static String sprite(String name) { return "agency/operations/gfx_" + name; }
    public static int missionFrame(AgencyView current, AgencyView.Operative operative) {
        var item = current.items().stream().filter(value -> value.id().equals("mission:" + operative.id())).findFirst().orElse(null);
        for (int i = 0; i < MISSIONS.length; i++) {
            String mission = MISSIONS[i], id = "assign:" + operative.id() + ":" + mission;
            if (current.items().stream().anyMatch(value -> value.id().equals(id) && value.value().equals("현재 임무"))
                    || item != null && !item.parameters().isEmpty() && item.parameters().getFirst().choices().stream()
                    .anyMatch(choice -> choice.value().equals(mission) && choice.label().equals(operative.mission()))) return MISSION_FRAMES[i];
        }
        return -1;
    }
    private static String status(AgencyView current, AgencyView.Operative operative) {
        if (!operative.statusText().isEmpty()) return operative.statusText();
        String text = operative.status();
        var mission = current.items().stream().filter(item -> item.id().equals("mission:" + operative.id()))
                .flatMap(item -> item.parameters().stream().limit(1)).flatMap(parameter -> parameter.choices().stream())
                .filter(choice -> choice.label().equals(operative.mission())).map(AgencyView.Choice::value).findFirst().orElse("");
        if (text.equals(operative.mission())) text = switch (mission) {
            case "COUNTER_INTELLIGENCE" -> UiCommandLabels.text("OPERATIVE_MISSION_COUNTER_INTELLIGENCE_ACTION", "적 파괴 활동 차단 중");
            case "BUILD_NETWORK" -> UiCommandLabels.text("OPERATIVE_MISSION_BUILD_INTEL_NETWORK_ACTION", "첩보망을 구축하는 중");
            case "QUIET_NETWORK" -> UiCommandLabels.text("OPERATIVE_MISSION_QUIET_INTEL_NETWORK_ACTION", "첩보망을 휴면 상태로 전환합니다");
            case "ROOT_OUT_RESISTANCE" -> UiCommandLabels.text("OPERATIVE_MISSION_ROOT_OUT_RESISTANCE_ACTION", "저항군을 소탕하는 중");
            case "BOOST_IDEOLOGY" -> UiCommandLabels.text("OPERATIVE_MISSION_BOOST_IDEOLOGY_ACTION", "이념 지지하기");
            case "CONTROL_TRADE" -> UiCommandLabels.text("OPERATIVE_MISSION_CONTROL_TRADE_ACTION", "무역 통제중");
            case "DIPLOMATIC_PRESSURE" -> UiCommandLabels.text("OPERATIVE_MISSION_DIPLOMATIC_PRESSURE_ACTION", "외교적 압력을 가하기");
            case "PROPAGANDA" -> UiCommandLabels.text("OPERATIVE_MISSION_PROPAGANDA_ACTION", "선전");
            default -> text;
        };
        String target = current.items().stream().filter(item -> item.id().equals("mission:" + operative.id()))
                .flatMap(item -> item.parameters().stream().skip(1).limit(1)).flatMap(parameter -> parameter.choices().stream())
                .filter(choice -> choice.value().equals(operative.country())).map(AgencyView.Choice::label)
                .findFirst().orElse(UiCountryDisplay.name(operative.country(), operative.country()));
        return operative.country().isEmpty() ? text : text + " - " + target;
    }
    private static String detail(AgencyView current, AgencyView.Operative operative) {
        if (!operative.tooltip().isEmpty()) return operative.tooltip();
        return current.items().stream().filter(item -> item.id().equals("mission:" + operative.id())).map(AgencyView.Item::detail)
                .findFirst().orElse(operative.name() + "\n" + operative.status());
    }
    private static void scrollText(GuiGraphicsExtractor g, String text, int x, int y, int width) {
        var client = Minecraft.getInstance();
        float scale = 1f;
        int textWidth = Math.round(client.font.width(text) * scale);
        long elapsed = Math.max(0, net.minecraft.util.Util.getMillis() - selectedAt);
        int offset = textWidth <= width ? 0 : Math.max(0, (int)((elapsed / 100) % (textWidth + 75)) - 30);
        g.enableScissor(x, y - 2, x + width, y + 12);
        g.pose().pushMatrix(); g.pose().translate(x - offset, y); g.pose().scale(scale);
        g.text(client.font, text, 0, 0, 0xFFE0E3DD, false);
        if (textWidth > width) g.text(client.font, text, Math.round((textWidth + 45) / scale), 0, 0xFFE0E3DD, false);
        g.pose().popMatrix();
        g.disableScissor();
    }
    public static void draw(GuiGraphicsExtractor g, int left, int mx, int my) {
        hits = List.of();
        tooltip = null;
        if (!active() || Minecraft.getInstance().gui.hud.isHidden()) return;
        AgencyRaidHud.drawMap(g);
        var current = snapshot();
        if (current == null || !current.country().equals(ClientCampaignHud.country())) return;
        if (targetingRevision != current.revision()) { clearTarget(); }
        if (!(DialogCoordinator.contentScreen() instanceof AgencyScreen)) OPERATIONS.draw(g, current, left, UiMenuBar.height(g.guiWidth()));
        else OPERATIONS.clear();
        var regions = new ArrayList<Hit>();
        var recruit = current.items().stream().filter(item -> item.id().equals("recruit")).findFirst().orElse(null);
        int count = Math.max(current.operatives().size(), current.operativeCapacity() == null ? 0 : current.operativeCapacity());
        int capacity = Math.max(1, (g.guiWidth() - left - 16) / 40);
        page = Math.clamp(page, 0, Math.max(0, count - capacity));
        int x = Math.max(left + 4, left + (g.guiWidth() - left - Math.min(capacity, count) * 40) / 2), y = g.guiHeight() - 75;
        for (var operative : current.operatives().subList(Math.min(page, current.operatives().size()), Math.min(current.operatives().size(), page + capacity))) {
            boolean highlighted = operative.id().equals(selected);
            var operation = current.operations().stream().filter(value -> value.id().equals(operative.operation())).findFirst().orElse(null);
            if (!operative.status().equals("포획됨")) {
                g.pose().pushMatrix();
                g.pose().translate(x, y);
                g.pose().scale(.5f);
                if (operation != null) {
                    int animationFrame = (int)(net.minecraft.util.Util.getMillis() * 24 / 1000 % 192);
                    g.blit(Identifier.parse("hoi:textures/gui/agency/operations/operation_status_scroll.png"),
                            0, 17, 78, 43, 0, 1, animationFrame / 192f, (animationFrame + 1) / 192f);
                    UiAssets.draw(g, "agency/operations/" + operation.icon().toLowerCase(Locale.ROOT) + "/frame_0", 5, 20, 21, 21);
                } else if (!operative.mission().isEmpty()) {
                    UiAssets.draw(g, sprite("execute_stop_operatives_small_bg") + "/frame_0", 0, 17, 78, 26);
                    int frame = missionFrame(current, operative);
                    if (frame >= 0) UiAssets.draw(g, sprite("operative_mission_icons_small") + "/frame_" + frame, 5, 20, 19, 19);
                    UiAssets.draw(g, sprite("active_operative_mission_anim") + "/frame_" + (net.minecraft.util.Util.getMillis() * 24 / 1000 % 11), 31, 19, 44, 20);
                }
                g.pose().popMatrix();
            }
            UiAssets.cover(g, art(current, operative.portrait()), x + 4, y + 23, 32, 42);
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(.5f);
            UiAssets.draw(g, sprite("operative_frame") + "/atlas", -3, 40, 87, 117, highlighted ? 86 : 0, highlighted ? 0 : 87);
            g.pose().popMatrix();
            if (operative.status().equals("포획됨")) UiAssets.draw(g, sprite("leader_portrait_captured") + "/frame_0", x, y + 20, 42, 59);
            regions.add(new Hit(x, y + 8, 40, 67, operative.id(), "", detail(current, operative)));
            var dismiss = current.items().stream().filter(item -> item.id().equals("dismiss:" + operative.id())).findFirst().orElse(null);
            if (dismiss != null) {
                g.pose().pushMatrix();
                g.pose().translate(x, y);
                g.pose().scale(.5f);
                UiAssets.drawLeaderControl(g, sprite("lg_disband_group_btn") + "/frame_0", 61, 44, 16, 16, dismiss.enabled());
                g.pose().popMatrix();
                regions.add(new Hit(x + 30, y + 22, 9, 8, "", dismiss.id(), dismiss.detail()));
            }
            x += 40;
        }
        for (int slot = Math.max(page, current.operatives().size()); slot < Math.min(count, page + capacity); slot++) {
            String slotId = "vacant:" + slot;
            var vacant = current.items().stream().filter(item -> item.id().equals(slotId)).findFirst().orElse(null);
            g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(.5f);
            if (vacant != null && vacant.enabled()) UiAssets.draw(g, "agency/operations/gfx_group_add_operative_bg/frame_0", 9, 47, 61, 83);
            else UiAssets.drawGrayscale(g, "agency/operations/gfx_group_add_operative_bg/frame_0", 9, 47, 61, 83);
            g.pose().popMatrix();
            UiAssets.draw(g, sprite("leader_group_frame_unselected") + "/frame_0", x, y + 20, 42, 55);
            if (vacant != null) {
                if (!vacant.enabled()) UiText.centered(g, Minecraft.getInstance().font, vacant.value(), x + 4, y + 65, 30, 10, 0xFFE0E3DD, false);
                regions.add(new Hit(x, y + 20, 40, 55, "", vacant.enabled() ? vacant.id() : "", vacant.detail()));
            } else if (recruit != null) regions.add(new Hit(x, y + 20, 40, 55, "", recruit.id(), recruit.detail()));
            x += 40;
        }
        var operative = current.operatives().stream().filter(value -> value.id().equals(selected)).findFirst().orElse(null);
        if (operative != null) {
            int barX = left + (g.guiWidth() - left - 201) / 2, barY = g.guiHeight() - 105;
            UiAssets.draw(g, sprite("operative_missionbar_bg") + "/frame_0", barX, barY, 201, 40);
            var missionItem = current.items().stream().filter(item -> item.id().equals("mission:" + operative.id())).findFirst().orElse(null);
            for (int i = 0; i < MISSIONS.length; i++) {
                int buttonX = barX + Math.round(MISSION_X[i] / 2f), buttonY = barY + 12;
                String assignmentId = "assign:" + operative.id() + ":" + MISSIONS[i];
                var assignment = current.items().stream().filter(value -> value.id().equals(assignmentId)).findFirst().orElse(null);
                boolean enabled = assignment != null && assignment.enabled() && pendingRevision < 0
                        && (!(DialogCoordinator.contentScreen() instanceof AgencyScreen agency) || !agency.awaitingResponse());
                String mission = MISSIONS[i];
                var choice = missionItem == null || missionItem.parameters().isEmpty() ? null : missionItem.parameters().getFirst().choices().stream()
                        .filter(value -> value.value().equals(mission)).findFirst().orElse(null);
                boolean currentMission = assignment != null && assignment.value().equals("현재 임무") || choice != null && operative.mission().equals(choice.label());
                UiAssets.drawLeaderControl(g, sprite("operative_mission_button_background") + "/frame_" + (currentMission ? 1 : 0), buttonX, buttonY, 24, 24, enabled);
                UiAssets.draw(g, sprite("operative_mission_icons") + "/frame_" + MISSION_FRAMES[i], buttonX + 4, buttonY + 4, 17, 17);
                UiAssets.draw(g, sprite("operative_mission_active") + "/frame_" + (currentMission ? 1 : 0), buttonX, buttonY, 24, 24);
                String key = mission.equals("BUILD_NETWORK") ? "BUILD_INTEL_NETWORK" : mission.equals("QUIET_NETWORK") ? "QUIET_INTEL_NETWORK" : mission;
                regions.add(new Hit(buttonX, buttonY, 24, 24, "", assignment == null ? "" : assignment.id(), assignment == null
                        ? UiCommandLabels.text("OPERATIVE_MISSION_" + key + "_TITLE", MISSION_TITLES[i]) : "§H" + assignment.title() + "§!\n\n" + assignment.detail()));
            }
            if (!targetRay.isEmpty()) {
                var assignment = current.items().stream().filter(value -> value.id().equals(targeting)).findFirst().orElse(null);
                if (assignment != null && assignment.parameters().size() == 2) {
                    var choices = assignment.parameters().get(1).choices();
                    int columns = Math.max(1, (g.guiWidth() - left - 16) / 95);
                    for (int i = 0; i < choices.size(); i++) {
                        var choice = choices.get(i);
                        int bx = left + 8 + i % columns * 95, by = barY - 23 - i / columns * 20;
                        UiAssets.nineSlice(g, sprite("button_123x34") + "/frame_0", bx, by, 92, 18, 4, .5);
                        if (Minecraft.getInstance().font.width(choice.label()) > 88) scrollText(g, choice.label(), bx + 2, by + 5, 88);
                        else UiText.centered(g, Minecraft.getInstance().font, choice.label(), bx + 2, by, 88, 18, 0xFFE0E3DD, false);
                        regions.add(new Hit(bx, by, 92, 18, "", "ideology:" + choice.value(), choice.label()));
                    }
                }
            }
        }
        if (operative != null && left == 0) {
            int top = UiMenuBar.height(g.guiWidth()) + 2;
            UiAssets.draw(g, sprite("operative_leader_badge") + "/frame_0", 2, top, 219, 55);
            UiAssets.draw(g, "country/" + current.country().toLowerCase(Locale.ROOT) + "/intelligence", 3, top, 48, 48);
            UiAssets.draw(g, art(current, operative.portrait()), 54, top + 7, 25, 34);
            UiAssets.draw(g, sprite("unit_leader_portrait_frame_badge") + "/frame_0", 53, top + 5, 27, 37);
            UiAssets.draw(g, sprite("land_battle_leader_skill_bg") + "/frame_0", 85, top + 9, 12, 18);
            if (operative.level() > 0) UiText.centered(g, Minecraft.getInstance().font, Integer.toString(operative.level()), 85, top + 9, 12, 18, 0xFFE0E3DD, false);
            scrollText(g, operative.codename(), 102, top + 3, 96);
            scrollText(g, operative.name(), 102, top + 13, 96);
            scrollText(g, operative.rank(), 102, top + 23, 96);
            regions.add(new Hit(54, top + 3, 145, 31, "", "", detail(current, operative)));
            String status = status(current, operative);
            String activity = switch (operative.activity()) {
                case "disabled" -> "gfx_operative_activity_progress_bar_disabled";
                case "captured" -> "gfx_operative_activity_progress_bar_captured";
                case "operation" -> "gfx_operative_activity_progress_bar_on_operation";
                default -> "gfx_operative_activity_porgress_bar";
            };
            String activityPath = "hoi:textures/gui/agency/operations/" + activity;
            g.pose().pushMatrix(); g.pose().translate(81, top + 40); g.pose().scale(.5f);
            g.blit(Identifier.parse(activityPath + "_background/frame_0.png"), 0, 0, 270, 17, 0, 1, 0, 1);
            int fill = (int)Math.round(270 * operative.activityProgress());
            if (fill > 0) g.blit(Identifier.parse(activityPath + "_fill/frame_0.png"), 0, 0, fill, 17, 0, fill / 270f, 0, 1);
            g.pose().popMatrix();
            int missionFrame = missionFrame(current, operative);
            if (missionFrame >= 0) UiAssets.draw(g, sprite("operative_mission_icons_small") + "/frame_" + missionFrame, 81, top + 40, 10, 10);
            scrollText(g, status, 93, top + 42, 110);
            if (!operative.country().isEmpty()) UiCountryFlags.draw(g, "country/" + operative.country().toLowerCase(Locale.ROOT) + "/flag", "smallest", 209, top + 43, 8, 6);
            UiAssets.draw(g, "country/" + current.country().toLowerCase(Locale.ROOT) + "/flag", 202, top + 19, 16, 11);
            regions.add(new Hit(82, top + 39, 135, 15, "", "", status));
            var dismiss = current.items().stream().filter(item -> item.id().equals("dismiss:" + operative.id())).findFirst().orElse(null);
            UiAssets.drawLeaderControl(g, sprite("nv_btn_delete") + "/frame_0", 54, top + 34, 11, 11, dismiss != null && dismiss.enabled());
            regions.add(new Hit(54, top + 34, 11, 11, "", dismiss == null ? "" : dismiss.id(), dismiss == null ? "" : dismiss.detail()));
            UiAssets.drawLeaderControl(g, sprite("nv_btn_close") + "/frame_0", 202, top + 2, 16, 16, true);
            regions.add(new Hit(202, top + 2, 16, 16, "", "deselect", ""));
            for (int i = 0; i < operative.traits().size(); i++) {
                var trait = operative.traits().get(i);
                UiAssets.draw(g, art(current, trait.icon()), 85 + i * 11, top + 31, 11, 11);
                regions.add(new Hit(85 + i * 11, top + 31, 11, 11, "", "", trait.tooltip()));
            }
        }
        hits = List.copyOf(regions);
        var hit = hits.reversed().stream().filter(value -> value.contains(mx, my) && !value.tooltip().isEmpty()).findFirst().orElse(null);
        String text = hit == null ? "" : hit.tooltip();
        if (!text.equals(hovered)) { hovered = text; tooltipScroll = 0; }
        if (hit != null) {
            tooltip = hit.y() > g.guiHeight() / 2
                    ? UiTooltips.drawSourceScrollableAbove(g, Minecraft.getInstance().font, text, mx, hit.y(), g.guiWidth(), g.guiHeight(), tooltipScroll)
                    : UiTooltips.drawSourceScrollable(g, Minecraft.getInstance().font, text, mx, my, g.guiWidth(), g.guiHeight(), tooltipScroll);
            tooltipScroll = tooltip.scroll();
        }
        OPERATIONS.tooltip(g, mx, my);
    }
    public static boolean containsControl(double x, double y) {
        return hits.stream().anyMatch(hit -> hit.contains(x, y)) || OPERATIONS.hits().stream().anyMatch(hit -> hit.contains(x, y));
    }
    public static boolean clickAt(double x, double y, int button) {
        if (!active()) return false;
        var current = snapshot();
        if (current == null || !current.country().equals(ClientCampaignHud.country())) return false;
        if (!(DialogCoordinator.contentScreen() instanceof AgencyScreen) && !dimension.equals(Minecraft.getInstance().level.dimension().identifier().toString())) { reset(); return false; }
        if (!targeting.isEmpty() && targetingRevision != current.revision()) { clearTarget(); }
        var hit = hits.reversed().stream().filter(value -> value.contains(x, y)).findFirst().orElse(null);
        if (hit == null && !targeting.isEmpty()) {
            if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) { clearTarget(); return true; }
            if (button != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || !targetRay.isEmpty()) return true;
            var client = Minecraft.getInstance();
            if (!client.options.getCameraType().isFirstPerson()) return true;
            var matrix = client.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
            var direction = matrix.transformProject(new org.joml.Vector3f((float)(x / client.getWindow().getGuiScaledWidth() * 2 - 1),
                    (float)(1 - y / client.getWindow().getGuiScaledHeight() * 2), 1)).normalize();
            try {
                var ray = new dev.hoi.protocol.commander.CommanderHudPayloads.Ray(direction.x, direction.y, direction.z);
                String argument = "ray:" + ray.x() + "," + ray.y() + "," + ray.z();
                var assignment = current.items().stream().filter(value -> value.id().equals(targeting)).findFirst().orElse(null);
                if (assignment != null && assignment.parameters().size() == 2) targetRay = argument;
                else assign(current, targeting, List.of(argument));
            } catch (IllegalArgumentException sky) { return true; }
            return true;
        }
        if (hit == null) return OPERATIONS.click(x, y, button, item -> openItem(current, item));
        if (hit.item().startsWith("assign:")) {
            var assignment = current.items().stream().filter(value -> value.id().equals(hit.item()) && value.enabled()).findFirst().orElse(null);
            if (assignment == null || pendingRevision >= 0 || assignment.parameters().isEmpty()
                    || DialogCoordinator.contentScreen() instanceof AgencyScreen agency && agency.awaitingResponse()) return true;
            if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && hit.item().endsWith(":COUNTER_INTELLIGENCE")
                    && assignment.parameters().getFirst().choices().stream().anyMatch(choice -> choice.value().equals(current.country())))
                assign(current, hit.item(), List.of(current.country()));
            else if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
                String next = targeting.equals(hit.item()) ? "" : hit.item(); clearTarget(); targeting = next; targetingRevision = current.revision();
                if (!targeting.isEmpty()) {
                    targetingSession = current.session(); targetingDimension = Minecraft.getInstance().level.dimension().identifier().toString();
                    highlightQueued = true; requestHighlight();
                }
                dev.hoi.client.audio.AudioUiSounds.play("ui.click");
            }
            return true;
        }
        if (button != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (hit.item().equals("deselect")) { clearTarget(); selected = ""; return true; }
        if (hit.item().startsWith("ideology:") && !targetRay.isEmpty()) { assign(current, targeting, List.of(targetRay, hit.item().substring(9))); return true; }
        if (!hit.operative().isEmpty()) { clearTarget(); selected = selected.equals(hit.operative()) ? "" : hit.operative(); selectedAt = net.minecraft.util.Util.getMillis(); return true; }
        if (!hit.item().isEmpty()) {
            openItem(current, hit.item());
        }
        return true;
    }
    private static void requestHighlight() {
        if (highlightCooldown > 0) return;
        highlight();
        if (!highlightQueued || targeting.isEmpty()) return;
        var current = snapshot();
        highlightQueued = false; highlightCooldown = 4;
        if (DialogCoordinator.contentScreen() instanceof AgencyScreen agency) agency.highlightMission(targeting);
        else transport.accept(new AgencyPayloads.Request(AgencyPayloads.Kind.HIGHLIGHT, current.session(), current.revision(), targeting, List.of()));
    }
    private static void assign(AgencyView current, String id, List<String> args) {
        if (pendingRevision >= 0) return;
        var item = current.items().stream().filter(value -> value.id().equals(id) && value.enabled()).findFirst().orElse(null);
        if (item == null || item.parameters().size() != args.size() || args.isEmpty()) return;
        if (!args.getFirst().startsWith("ray:") && item.parameters().getFirst().choices().stream().noneMatch(choice -> choice.value().equals(args.getFirst()))) return;
        if (args.size() == 2 && item.parameters().get(1).choices().stream().noneMatch(choice -> choice.value().equals(args.get(1)))) return;
        if (DialogCoordinator.contentScreen() instanceof AgencyScreen agency) {
            if (agency.awaitingResponse()) return;
            agency.assignMission(id, args);
        } else {
            pendingRevision = current.revision();
            transport.accept(new AgencyPayloads.Request(AgencyPayloads.Kind.CALL, current.session(), current.revision(), id, args));
        }
        clearTarget();
        dev.hoi.client.audio.AudioUiSounds.play("ui.click");
    }
    private static void openItem(AgencyView current, String item) {
        if (item.startsWith("dismiss:") && current.items().stream().noneMatch(value -> value.id().equals(item) && value.enabled())) return;
        if (DialogCoordinator.contentScreen() instanceof AgencyScreen agency) agency.showMapItem(item);
        else {
            boolean dismiss = item.startsWith("dismiss:");
            var screen = new AgencyScreen(dismiss ? new dev.hoi.client.screen.map.MapCursor() : null, current.session(), current, transport);
            if (!dismiss) reset();
            DialogCoordinator.setContent(screen); screen.showMapItem(item);
        }
    }
}
