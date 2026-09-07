package dev.hoi.client;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** Actual rendering and vanilla movement; snapshots/requests are fixtures, not multiplayer proof. */
public final class ResearchScreenGameTest implements FabricClientGameTest {
    private static final KeyEvent ENTER = new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0);
    private static final KeyEvent ESCAPE = new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0);
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>();
            HoiClient.registerCommands(dispatcher);
            check(dispatcher.getRoot().getChild("hoi") == null, "Never shadow the server /hoi command");
            check(dispatcher.getRoot().getChild("hoi-research") != null, "Independent research shortcut");
            ResearchProtocol.registerPayloadTypes(); ResearchProtocol.registerPayloadTypes();
        });
        loadPack(context);
        context.getInput().resizeWindow(1600, 1000);
        context.runOnClient(client -> client.options.guiScale().set(2));
        try (var world = context.worldBuilder().adjustSettings(settings -> settings.setGameMode(
                net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var menu = fixtureMenu();
            context.setScreen(() -> new HoiMenuScreen(menu)); context.waitTicks(5);
            context.runOnClient(client -> check(((HoiMenuScreen)client.gui.screen()).panelWidth() == client.gui.screen().width * 4 / 10, "Country panel is 40 percent"));
            context.takeScreenshot("hoi-menu-country");
            for (String label : List.of("민간", "육군", "해군", "공군")) {
                click(context, label); context.waitTicks(2); context.takeScreenshot("hoi-country-" + label);
            }
            for (var tab : MenuTab.ORDER) if (tab != MenuTab.RESEARCH) {
                click(context, tab.label() + " 메뉴");
                context.runOnClient(client -> {
                    var screen = (HoiMenuScreen)client.gui.screen();
                    check(screen.selectedTab() == tab, "Menu tab " + tab);
                    int percent = tab == MenuTab.POLITICS || tab == MenuTab.RECRUITMENT ? 40 : tab == MenuTab.TRADE ? 35 : 30;
                    check(screen.panelWidth() == screen.width * percent / 100, "Panel width for " + tab);
                });
            }
            context.waitTicks(2); context.takeScreenshot("hoi-menu-officers");
            movement(context);
            agency(context);
            var requests = new ArrayList<ResearchProtocol.Request>();
            var view = fixtureResearch();
            context.setScreen(() -> fixtureMainMenu(menu, MenuTab.RECRUITMENT, view, requests));
            click(context, "연구 메뉴"); context.waitTicks(3);
            context.takeScreenshot("hoi-research-slots");
            context.runOnClient(client -> {
                checkToolbar(client.gui.screen());
                var slots = client.gui.screen().children().stream().filter(c -> c instanceof ResearchSlotButton).map(c -> (ResearchSlotButton)c).toList();
                check(slots.size() == 5, "Only the five slots supplied by the server are displayed");
                check(slots.getFirst().getY() > HoiMenuBar.height(client.gui.screen().width) + 88, "Spare vertical space enlarges the top research banner");
                check(slots.getLast().getBottom() <= client.gui.screen().height - 8, "Slots fit below persistent menu");
                check(client.gui.screen().children().stream().noneMatch(c -> c instanceof Button b
                        && List.of("<", ">", "1 / 1").contains(b.getMessage().getString())), "No page count or page navigation buttons");
            });
            click(context, "슬롯 1 · 연구 선택"); context.waitTicks(2);
            context.runOnClient(client -> {
                var screen = (ResearchScreen)client.gui.screen();
                checkToolbar(screen);
                check(!screen.allowsMovement(), "Full tree blocks world movement");
                screen.clearFocus(); screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0)); screen.keyPressed(ENTER);
            });
            context.waitTicks(2); context.takeScreenshot("hoi-research-detail");
            context.runOnClient(client -> checkToolbar(client.gui.screen()));
            click(context, "연구");
            context.runOnClient(client -> {
                check(requests.size() == 1, "Exactly one request");
                var request = requests.getFirst();
                check(request.action() == ResearchProtocol.Action.START && request.slot() == 0
                        && request.session().equals(view.session()) && request.revision() == view.revision(), "Authenticated research request");
                check(client.gui.screen().children().stream().anyMatch(c -> c instanceof Button b
                        && b.getMessage().getString().equals("응답 대기…") && !b.active), "No duplicate request while pending");
                client.gui.screen().keyPressed(ESCAPE);
            });
            context.waitTicks(2); context.takeScreenshot("hoi-research-tree");
            click(context, "기갑"); context.waitTicks(2); context.takeScreenshot("hoi-research-armor");
            click(context, "해군 지원 장비"); context.waitTicks(2);
            context.getInput().resizeWindow(854, 480); context.waitTicks(3);
            context.takeScreenshot("hoi-research-compact");
            context.runOnClient(client -> client.gui.screen().keyPressed(ESCAPE));
            context.waitTicks(2); context.takeScreenshot("hoi-research-compact-slots");
            context.runOnClient(client -> {
                checkToolbar(client.gui.screen());
                check(client.gui.screen().children().stream().filter(c -> c instanceof ResearchSlotButton).count() == 5, "Five actual compact slots");
            });
            context.getInput().resizeWindow(1600, 1000); context.waitTicks(2);
            // A separate server snapshot starts the cancellation case; the START case had no server ACK.
            context.setScreen(() -> fixtureMainMenu(menu, MenuTab.POLITICS, view, requests));
            click(context, "연구 메뉴");
            click(context, "슬롯 2 · 진행 연구");
            click(context, "연구 중단");
            context.runOnClient(client -> check(requests.getLast().action() == ResearchProtocol.Action.CANCEL
                    && requests.getLast().slot() == 1, "Cancel uses the actual active slot"));
            click(context, "무역 & 경제 메뉴");
            context.runOnClient(client -> {
                check(client.gui.screen() instanceof HoiMenuScreen screen && screen.selectedTab() == MenuTab.TRADE,
                        "Shared navigation works from research detail");
                check(requests.getLast().action() == ResearchProtocol.Action.CLOSE, "Leaving research closes its private session");
            });
            click(context, "연구 메뉴");
            context.runOnClient(client -> checkToolbar(client.gui.screen()));
            context.setScreen(() -> new HoiMenuScreen(MenuTab.LOGISTICS));
            context.waitTicks(2); context.takeScreenshot("hoi-menu-loading");
            context.runOnClient(client -> {
                var screen = (HoiMenuScreen)client.gui.screen(); checkToolbar(screen);
                screen.update(menu);
                check(screen.selectedTab() == MenuTab.LOGISTICS, "Fresh authenticated menu retains the requested tab");
            });
            var six = new ResearchView(view.session(), view.revision(), view.country(), view.countryName(), view.day(), view.date(), view.speed(),
                    java.util.stream.IntStream.range(0, 6).mapToObj(i -> new ResearchView.Slot(i, "", 0)).toList(), List.of(), view.message());
            context.getInput().resizeWindow(854, 480); context.waitTicks(2);
            context.setScreen(() -> new ResearchScreen(six, requests::add)); context.waitTicks(2);
            context.runOnClient(client -> {
                checkToolbar(client.gui.screen());
                var slots = client.gui.screen().children().stream().filter(c -> c instanceof ResearchSlotButton).map(c -> (ResearchSlotButton)c).toList();
                check(slots.size() == 6 && slots.getLast().getBottom() <= client.gui.screen().height - 8,
                        "Six-slot capacity still fits at compact resolution");
            });
            context.takeScreenshot("hoi-research-six-slot-capacity");
            context.getInput().resizeWindow(1600, 1000); context.waitTicks(2);
            var source = view.technologies().getFirst();
            var active = new ResearchView.Tech(source.id(), source.category(), source.name(), source.year(), source.tier(),
                    source.baseDays(), 30, source.dailyRate(), source.prerequisites(), source.effects(), source.unlocks(), ResearchView.Status.ACTIVE);
            var centered = new ResearchView(view.session(), view.revision() + 1, view.country(), view.countryName(), view.day(), view.date(), view.speed(),
                    java.util.stream.IntStream.range(0, 5).mapToObj(i -> new ResearchView.Slot(i, i == 0 ? active.id() : "", 12)).toList(), List.of(active), "");
            context.setScreen(() -> new ResearchScreen(centered, requests::add)); context.waitTicks(2);
            context.takeScreenshot("hoi-research-centered-image");
            context.runOnClient(client -> check(active.remainingDays(centered.slots().getFirst().savedDays()) == 58,
                    "Hidden saved-day text does not change remaining-time calculation"));
            context.getInput().resizeWindow(854, 480); context.waitTicks(2);
            context.takeScreenshot("hoi-research-centered-image-compact");
            context.setScreen(() -> null);
        }
    }

    private static void loadPack(ClientGameTestContext context) {
        var pack = Path.of(System.getProperty("hoi.testResourcePack"));
        check(Files.isRegularFile(pack), "Build HOI-resourcepack first, or pass -PhoiResourcePack=<resources.zip>");
        var reload = new AtomicReference<CompletableFuture<Void>>();
        context.runOnClient(client -> {
            try {
                var target = client.getResourcePackDirectory().resolve("hoi-test.zip");
                Files.createDirectories(target.getParent()); Files.copy(pack, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
            var repository = client.getResourcePackRepository(); repository.reload();
            var selected = new ArrayList<>(repository.getSelectedIds()); selected.add("file/hoi-test.zip");
            repository.setSelected(selected); reload.set(client.reloadResourcePacks());
        });
        context.waitFor(client -> reload.get().isDone());
        reload.get().join();
        context.runOnClient(client -> check(client.getResourceManager().getResource(
                Identifier.fromNamespaceAndPath("hoi", "textures/gui/panel/research_banner.png")).isPresent(), "External art loaded"));
    }
    private static void agency(ClientGameTestContext context) {
        var items = new ArrayList<AgencyView.Item>();
        items.add(new AgencyView.Item("recruit", "agents", "첩보원 고용", "1 / 2", "새로운 요원을 고용합니다.", "agency/recruit", "고용", true, -1, List.of()));
        for (String upgrade : List.of("economy_intelligence", "army_intelligence", "navy_intelligence", "air_intelligence", "passive_defense", "form_cryptology"))
            items.add(new AgencyView.Item("upgrade:" + upgrade, "upgrades", "기관 개선 " + items.size(), "0 / 1", "민간공장 5개 · 30일", "agency/" + upgrade, "개선", true, 0, List.of()));
        items.add(new AgencyView.Item("decrypt:PRK", "cryptology", "북한 암호", "해독 중", "해독 진행: 1200 / 12000", "agency/cryptology", "일시 정지", true, .1, List.of()));
        items.add(new AgencyView.Item("operation:CAPTURE_CIPHER", "operations", "암호 탈취", "60일", "정보망 50과 대기 요원 2명이 필요합니다.\n민간공장 3개가 사용됩니다.", "agency/capture_cipher", "작전 준비", true, -1,
                List.of(new AgencyView.Parameter("대상 국가", List.of(new AgencyView.Choice("PRK", "북한"), new AgencyView.Choice("JAP", "일본"))))));
        var fixture = new AgencyView("agency-fixture", 1, "KOR", "국가정보원", "무선 감청 · 21일 남음", items, "검증용 기관 데이터");
        var requests = new ArrayList<AgencyProtocol.Request>();
        context.setScreen(() -> new AgencyScreen(null, fixture.session(), fixture, requests::add)); context.waitTicks(2);
        context.takeScreenshot("hoi-agency-operatives");
        click(context, "첩보원 고용"); context.waitTicks(2); click(context, "고용");
        context.runOnClient(client -> {
            check(requests.size() == 1 && requests.getFirst().kind() == AgencyProtocol.Kind.CALL && requests.getFirst().revision() == 1, "Agency uses issued session and revision");
            check(client.gui.screen().children().stream().anyMatch(c -> c instanceof Button b && !b.active && b.getMessage().getString().equals("응답 대기…")), "Pending agency action disabled");
            ((AgencyScreen)client.gui.screen()).update(new AgencyView(fixture.session(), 2, "KOR", fixture.name(), fixture.status(), items, "고용 완료"));
        });
        click(context, "닫기"); click(context, "기관 개선"); context.waitTicks(2); context.takeScreenshot("hoi-agency-upgrades");
        click(context, "암호학"); context.waitTicks(2); context.takeScreenshot("hoi-agency-cryptology");
        click(context, "작전"); click(context, "암호 탈취"); context.waitTicks(2); context.takeScreenshot("hoi-agency-operation");
        click(context, "대상 국가: 북한"); context.waitTicks(1); click(context, "일본"); click(context, "작전 준비");
        context.runOnClient(client -> {
            check(requests.getLast().arguments().equals(List.of("JAP")), "Choice picker sends selected target without country authority");
            ((AgencyScreen)client.gui.screen()).update(new AgencyView(fixture.session(), 0, "", "", "", List.of(), "권한 만료"));
            check(client.gui.screen() == null, "Revocation closes even a newer private snapshot");
        });
        context.setScreen(() -> null);
    }
    private static void movement(ClientGameTestContext context) {
        click(context, "국가 정보 메뉴"); click(context, "• 공군");
        context.runOnClient(client -> {
            client.player.setPos(client.player.getX(), client.player.getY() + 8, client.player.getZ());
            client.player.setOnGround(false);
            client.player.getAbilities().flying = true; client.player.onUpdateAbilities();
        });
        context.waitTicks(5);
        context.runOnClient(client -> check(client.player.getAbilities().flying, "Fixture is flying before ascent checks"));
        final double[] position = new double[3];
        context.runOnClient(client -> { position[0] = client.player.getX(); position[1] = client.player.getY(); position[2] = client.player.getZ(); });
        context.getInput().holdKey(o -> o.keyUp); context.waitTicks(15); context.getInput().releaseKey(o -> o.keyUp);
        context.runOnClient(client -> check(Math.hypot(client.player.getX() - position[0], client.player.getZ() - position[2]) > .2, "W moves while sidebar open"));
        context.runOnClient(client -> position[1] = client.player.getY());
        context.getInput().holdKey(o -> o.keyJump); context.waitTicks(12); context.getInput().releaseKey(o -> o.keyJump);
        context.runOnClient(client -> { check(client.player.getY() > position[1] + .2, "Space ascends while sidebar open"); position[1] = client.player.getY(); });
        context.getInput().holdKey(o -> o.keyShift); context.waitTicks(12); context.getInput().releaseKey(o -> o.keyShift);
        context.runOnClient(client -> check(client.player.getY() < position[1] - .2, "Shift descends while sidebar open"));
        context.getInput().holdKey(o -> o.keyUp); context.waitTicks(2);
        click(context, "공군 장비 · 12");
        context.runOnClient(client -> check(!client.options.keyUp.isDown() && !((HoiMenuScreen)client.gui.screen()).allowsMovement(), "Detail modal releases movement"));
        context.getInput().releaseKey(o -> o.keyUp); context.setScreen(() -> null); context.waitTicks(2);
        context.runOnClient(client -> check(!client.options.keyUp.isDown() && !client.options.keyJump.isDown() && !client.options.keyShift.isDown(), "No stuck movement on close"));
    }
    private static void click(ClientGameTestContext context, String label) {
        context.runOnClient(client -> {
            var button = client.gui.screen().children().stream().filter(c -> c instanceof Button b && b.active
                    && b.getMessage().getString().equals(label)).map(c -> (Button)c).findFirst().orElseThrow(() -> new AssertionError("Missing button: " + label));
            button.onPress(ENTER);
        });
    }
    private static void checkToolbar(net.minecraft.client.gui.screens.Screen screen) {
        var tabs = screen.children().stream().filter(c -> c instanceof HoiMenuBar.TabButton).map(c -> (HoiMenuBar.TabButton)c).toList();
        check(tabs.size() == MenuTab.ORDER.size() && tabs.stream().allMatch(b -> b.active && b.visible
                && b.getY() == 3 && b.getBottom() < HoiMenuBar.height(screen.width)), "All common menu buttons remain usable without a status row");
    }
    private static HoiMenuScreen fixtureMainMenu(MenuView menu, MenuTab selected, ResearchView research, List<ResearchProtocol.Request> requests) {
        return new HoiMenuScreen(menu, selected, () -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                new ResearchScreen(research, requests::add, tab -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                        fixtureMainMenu(menu, tab, research, requests)))));
    }
    private static MenuView fixtureMenu() {
        var pages = MenuTab.ORDER.stream().map(tab -> new MenuView.Page(tab, tab == MenuTab.POLITICS ? List.of(
                new MenuView.Section("국가 현황", "politics", List.of(new MenuView.Entry("정치력", "72", "화면 검증용 데이터"))),
                new MenuView.Section("민간 정보", "civilian", List.of(new MenuView.Entry("민간공장", "20", "화면 검증용 데이터"))),
                new MenuView.Section("육군", "army", List.of(new MenuView.Entry("육군 장비", "12", "화면 검증용 데이터"))),
                new MenuView.Section("해군", "navy", List.of(new MenuView.Entry("해군 장비", "12", "화면 검증용 데이터"))),
                new MenuView.Section("공군", "air", List.of(new MenuView.Entry("공군 장비", "12", "화면 검증용 데이터"))))
                : List.of(new MenuView.Section(tab.label(), tab.id(), List.of(new MenuView.Entry("진행 현황", "12", "검증용 데이터")))))).toList();
        return new MenuView("KOR", "대한민국", "2020년 1월 1일 00시", "PAUSED", pages);
    }
    private static ResearchView fixtureResearch() {
        var technologies = new ArrayList<ResearchView.Tech>();
        technologies.add(new ResearchView.Tech("hoi:infantry/modern_small_arms_1", "INFANTRY", "보병 장비", 2000, 1, 100, 0, 1,
                List.of(), List.of("시험 효과 +5%"), List.of("시험 장비"), ResearchView.Status.AVAILABLE));
        technologies.add(new ResearchView.Tech("hoi:test/active", "INFANTRY", "진행 연구", 2006, 2, 160, 63, 1.25,
                List.of("hoi:infantry/modern_small_arms_1"), List.of(), List.of(), ResearchView.Status.ACTIVE));
        for (int i = 0; i < 16; i++) technologies.add(new ResearchView.Tech("hoi:test/armor_" + i, "ARMOR", "기갑 연구 " + i,
                new int[]{1980,2018,2020,2022,2024,2028,2032}[i % 7], 1, 180, 0, 1,
                List.of(), List.of(), List.of(), ResearchView.Status.LOCKED));
        return new ResearchView("ui-test", 17, "KOR", "대한민국", 72, "2020년 1월 1일 00시", "PAUSED",
                java.util.stream.IntStream.range(0, 5).mapToObj(i -> new ResearchView.Slot(i, i == 1 ? "hoi:test/active" : "", i == 0 ? 12 : 0)).toList(), technologies, "UI 검증용 데이터");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
