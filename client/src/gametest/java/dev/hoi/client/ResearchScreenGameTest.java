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
        AtlasSurfaceRenderChecks.run(context);
        context.getInput().resizeWindow(1600, 1000);
        context.runOnClient(client -> client.options.guiScale().set(2));
        try (var world = context.worldBuilder().adjustSettings(settings -> settings.setGameMode(
                net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var menu = fixtureMenu();
            ConstructionScreenChecks.run(context, menu.hud());
            context.setScreen(() -> new HoiMenuScreen(menu)); context.waitTicks(5);
            context.runOnClient(client -> check(((HoiMenuScreen)client.gui.screen()).panelWidth() == client.gui.screen().width * 726 / 2560, "Country panel follows the original 726-pixel container"));
            context.takeScreenshot("hoi-menu-country");
            check(HoiMenuBar.indicators(menu.hud()).stream().limit(13).map(HoiMenuBar.Indicator::icon).toList().equals(List.of(
                    "political_power", "stability", "war_support", "manpower", "factories", "fuel", "supplies", "convoys", "command_power",
                    "army_experience", "air_experience", "navy_experience", "party_support")), "National indicators follow the requested order");
            check(HoiMenuBar.indicators(menu.hud()).stream().anyMatch(i -> i.icon().equals("nuclear") && i.label().equals("핵폭탄")), "Nuclear bombs appear from campaign start without research");
            var armed = new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, .36, true, 0L, fixtureNational());
            check(HoiMenuBar.indicators(armed).stream().anyMatch(i -> i.icon().equals("nuclear") && i.value().equals("0")), "Completed nuclear research shows zero inventory");
            check(HoiMenuBar.maximumScroll(800, armed) == 0, "All national and financial indicators fit the normal viewport");
            check(HoiMenuBar.scroll(427, armed, 0, 0, -100) == HoiMenuBar.maximumScroll(427, armed), "Compact HUD can reach the final indicators");
            context.setScreen(() -> new HoiMenuScreen(new MenuView(menu.country(), menu.countryName(), menu.date(), menu.speed(), menu.pages(), armed)));
            context.waitTicks(2); context.takeScreenshot("hoi-hud-nuclear-researched-zero");
            var stocked = new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, .36, true, 12L, fixtureNational());
            context.setScreen(() -> new HoiMenuScreen(new MenuView(menu.country(), menu.countryName(), menu.date(), menu.speed(), menu.pages(), stocked)));
            context.waitTicks(2); context.takeScreenshot("hoi-hud-nuclear-stockpile");
            check(HoiMenuBar.defconFrame(null) == -1, "Unknown tension must not imply DEFCON 5");
            check(HoiMenuBar.defconFrame(0.0) == 0 && HoiMenuBar.defconFrame(1.0) == 9,
                    "DEFCON endpoints use the first and final original frames");
            for (int step = 1; step < 10; step++) {
                double boundary = step / 10.0;
                check(HoiMenuBar.defconFrame(boundary) == step
                        && HoiMenuBar.defconFrame(Math.nextDown(boundary)) == step - 1,
                        "DEFCON changes exactly at " + step * 10 + "%");
            }
            for (Double tension : new Double[]{0.0, .4, .6, 1.0, null}) {
                var hud = new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, tension, false, 0L, fixtureNational());
                context.setScreen(() -> new HoiMenuScreen(new MenuView(menu.country(), menu.countryName(), menu.date(), menu.speed(), menu.pages(), hud)));
                context.waitTicks(2); context.takeScreenshot("hoi-defcon-frame-" + HoiMenuBar.defconFrame(tension));
            }
            context.setScreen(() -> new HoiMenuScreen(menu)); context.waitTicks(2);
            click(context, "정치력 · 72"); context.waitTicks(2); context.takeScreenshot("hoi-menu-country-detail");
            context.runOnClient(client -> client.gui.screen().keyPressed(ESCAPE));
            for (String label : List.of("민간", "육군", "해군", "공군")) {
                click(context, label); context.waitTicks(2); context.takeScreenshot("hoi-country-" + label);
            }
            for (var tab : MenuTab.ORDER) if (tab != MenuTab.RESEARCH) {
                click(context, tab.label() + " 메뉴");
                context.runOnClient(client -> {
                    var screen = (HoiMenuScreen)client.gui.screen();
                    check(screen.selectedTab() == tab, "Menu tab " + tab);
                    int sourceWidth = switch (tab) { case POLITICS -> 726; case TRADE -> 620; case LOGISTICS -> 560; default -> 550; };
                    check(screen.panelWidth() == screen.width * sourceWidth / 2560, "Original container width for " + tab);
                });
            }
            context.waitTicks(2); context.takeScreenshot("hoi-menu-officers");
            click(context, "무역 & 경제 메뉴"); context.waitTicks(2); context.takeScreenshot("hoi-menu-economy");
            click(context, "무역"); context.waitTicks(2); context.takeScreenshot("hoi-menu-trade");
            context.getInput().resizeWindow(854, 480); context.waitTicks(3);
            context.runOnClient(client -> client.gui.screen().mouseScrolled(100, 5, 0, -100));
            context.waitTicks(2); context.takeScreenshot("hoi-hud-compact-final-indicators");
            context.runOnClient(client -> client.gui.screen().mouseScrolled(100, 5, 0, 100));
            for (var tab : List.of(MenuTab.POLITICS, MenuTab.TRADE, MenuTab.INTELLIGENCE, MenuTab.RECRUITMENT)) {
                click(context, tab.label() + " 메뉴");
                context.runOnClient(client -> {
                    var screen = (HoiMenuScreen)client.gui.screen(); checkToolbar(screen);
                    var controls = screen.children().stream().filter(c -> c instanceof HoiMenuButton).map(c -> (HoiMenuButton)c).toList();
                    check(controls.stream().allMatch(b -> b.getX() >= 0 && b.getRight() <= screen.panelWidth()
                            && b.getBottom() <= screen.height), "Compact menu controls remain inside the sidebar");
                    for (var a : controls) for (var b : controls) if (a != b)
                        check(a.getRight() <= b.getX() || b.getRight() <= a.getX() || a.getBottom() <= b.getY() || b.getBottom() <= a.getY(),
                                "Compact controls do not overlap");
                });
                context.waitTicks(2); context.takeScreenshot("hoi-menu-compact-" + tab.id());
            }
            context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
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
                    java.util.stream.IntStream.range(0, 5).mapToObj(i -> new ResearchView.Slot(i, i == 0 ? active.id() : "", 12)).toList(), List.of(active), "", view.hud());
            context.setScreen(() -> new ResearchScreen(centered, requests::add)); context.waitTicks(2);
            context.takeScreenshot("hoi-research-centered-image");
            context.runOnClient(client -> check(active.remainingDays(centered.slots().getFirst().savedDays()) == 58,
                    "Hidden saved-day text does not change remaining-time calculation"));
            context.getInput().resizeWindow(854, 480); context.waitTicks(2);
            context.takeScreenshot("hoi-research-centered-image-compact");
            fullTfrCatalog(context);
            context.setScreen(() -> null);
        }
    }

    private static void fullTfrCatalog(ClientGameTestContext context) {
        ResearchView view;
        try(var in=ResearchScreenGameTest.class.getResourceAsStream("/tfr-research-view.json")) {
            view=new ResearchProtocol.Response(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).view();
        } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
        var requests=new ArrayList<ResearchProtocol.Request>();
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
        context.setScreen(()->new ResearchScreen(view,requests::add));context.waitTicks(2);context.takeScreenshot("hoi-tfr-benefits-slots");
        click(context,"슬롯 1 · "+view.technology(view.slots().getFirst().technology()).name());context.waitTicks(2);
        click(context,"×");context.waitTicks(2);
        for(var tab:List.of("보병","지상 & 항공 지원","기갑","포","해군","해군 지원 장비","공군","산업","공학")) {
            click(context,tab);context.waitTicks(2);context.takeScreenshot("hoi-tfr-tree-"+List.of("보병","지상 & 항공 지원","기갑","포","해군","해군 지원 장비","공군","산업","공학").indexOf(tab));
        }
        context.getInput().resizeWindow(2560,1440); context.waitTicks(3);
        for (var tab : List.of("기갑", "공학", "산업")) {
            click(context, tab); context.waitTicks(2);
            context.takeScreenshot(tab.equals("기갑") ? "hoi-armor-rearranged" : tab.equals("공학") ? "hoi-engineering-reference" : "hoi-industry-rearranged");
            context.runOnClient(client -> {
                var screen = (ResearchScreen)client.gui.screen();
                var tabs=screen.children().stream().filter(w -> w instanceof ResearchTabButton).map(w -> (ResearchTabButton)w).toList();
                check(screen.treeTop()==tabs.getFirst().getBottom()-1,"Research tabs join the tree panel without a control-row gap");
                check(screen.children().stream().noneMatch(w -> w instanceof net.minecraft.client.gui.components.EditBox),"Technology search is removed");
                check(tabs.size()==9 && tabs.getFirst().getX()==10 && tabs.getLast().getRight()<screen.width/2,"Nine research tabs stay compact and left aligned");
                for(int i=1;i<tabs.size();i++) check(tabs.get(i).getX()==tabs.get(i-1).getRight(),"Tabs are adjacent");
                check(screen.children().stream().filter(w -> w instanceof Button).map(w -> ((Button)w).getMessage().getString())
                        .noneMatch(text -> text.equals("처음 위치") || text.equals("슬롯 목록")), "Removed tree controls stay absent");
            });
        }
        context.getInput().resizeWindow(854,480); context.waitTicks(3);
        context.takeScreenshot("hoi-industry-rearranged-compact");
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen(); screen.clearFocus();
            for (int i=0;i<60;i++) screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT,0,0));
            screen.keyPressed(ENTER);
        });
        context.waitTicks(2); context.takeScreenshot("hoi-industry-rearranged-compact-detail"); click(context,"×");
        context.getInput().resizeWindow(1600,1000); context.waitTicks(3); click(context,"공학"); context.waitTicks(2);
        context.runOnClient(client->{var screen=(ResearchScreen)client.gui.screen();screen.clearFocus();screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT,0,0));screen.keyPressed(ENTER);});
        context.waitTicks(2);context.takeScreenshot("hoi-tfr-source-detail");click(context,"연구");
        check(requests.size()==1&&requests.getFirst().slot()==1,"Occupied slot zero falls back to the first free slot without cancellation");
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
        context.runOnClient(client -> {
            for (var item : HoiMenuBar.indicators(CountryHud.UNKNOWN))
                check(client.getResourceManager().getResource(Identifier.fromNamespaceAndPath("hoi", "textures/gui/hud/" + item.icon() + ".png")).isPresent(),
                        "External HUD icon loaded: " + item.icon());
            for (int frame = 0; frame < 10; frame++)
                check(client.getResourceManager().getResource(Identifier.fromNamespaceAndPath("hoi", "textures/gui/hud/defcon/" + frame + ".png")).isPresent(),
                        "Original DEFCON frame loaded: " + frame);
            check(client.getResourceManager().getResource(Identifier.fromNamespaceAndPath("hoi", "textures/gui/hud/defcon/frame.png")).isPresent(),
                    "Original DEFCON panel loaded");
        });
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
        click(context, "국가 정보 메뉴"); click(context, "공군");
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
                && (b.getY() == 3 || b.getY() == 17) && b.getBottom() < HoiMenuBar.height(screen.width)), "Common navigation stays usable below the resource strip");
        check(tabs.getLast().getRight() < HoiMenuBar.dockWidth(screen.width)
                && HoiMenuBar.dockWidth(screen.width) <= screen.width, "Compact original-art toolbar fits the viewport");
        check(HoiMenuBar.tensionX(screen.width) + 30 == screen.width - 4
                && HoiMenuBar.tensionX(screen.width) > HoiMenuBar.dockWidth(screen.width), "Flag-sized tension display stays at the far right");
    }
    private static HoiMenuScreen fixtureMainMenu(MenuView menu, MenuTab selected, ResearchView research, List<ResearchProtocol.Request> requests) {
        return new HoiMenuScreen(menu, selected, () -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                new ResearchScreen(research, requests::add, tab -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                        fixtureMainMenu(menu, tab, research, requests)))));
    }
    private static MenuView fixtureMenu() {
        var pages = MenuTab.ORDER.stream().map(tab -> new MenuView.Page(tab, tab == MenuTab.POLITICS ? List.of(
                new MenuView.Section("국가 현황", "politics", List.of(new MenuView.Entry("정치력", "72", "화면 검증용 데이터"),
                        new MenuView.Entry("안정도", "62%", "화면 검증용 데이터"), new MenuView.Entry("전쟁 지지도", "48%", "화면 검증용 데이터"))),
                new MenuView.Section("민간 정보", "civilian", List.of(new MenuView.Entry("민간공장", "20", "화면 검증용 데이터"))),
                new MenuView.Section("육군", "army", List.of(new MenuView.Entry("육군 장비", "12", "화면 검증용 데이터"))),
                new MenuView.Section("해군", "navy", List.of(new MenuView.Entry("해군 장비", "12", "화면 검증용 데이터"))),
                new MenuView.Section("공군", "air", List.of(new MenuView.Entry("공군 장비", "12", "화면 검증용 데이터"))))
                : tab == MenuTab.TRADE ? List.of(
                        new MenuView.Section("경제 현황", "trade", List.of(new MenuView.Entry("민간공장", "20", "화면 검증용 데이터"),
                                new MenuView.Entry("군수공장", "18", "화면 검증용 데이터"))),
                        new MenuView.Section("자원 무역", "trade", List.of(new MenuView.Entry("석유", "38", "화면 검증용 데이터"),
                                new MenuView.Entry("강철", "200", "화면 검증용 데이터"))))
                : List.of(new MenuView.Section(tab.label(), tab.id(), List.of(new MenuView.Entry("진행 현황", "12", "검증용 데이터")))))).toList();
        return new MenuView("KOR", "대한민국", "2020년 1월 1일 00시", "PAUSED", pages,
                new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, .36, false, null, fixtureNational()));
    }
    private static CountryHud.NationalIndicators fixtureNational() {
        return new CountryHud.NationalIndicators(73.0, .62, .48, 45L, .85, 894_920.0, 580.0, .72, 1000L, .95, 150.0, .36, 280_900L);
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
                java.util.stream.IntStream.range(0, 5).mapToObj(i -> new ResearchView.Slot(i, i == 1 ? "hoi:test/active" : "", i == 0 ? 12 : 0)).toList(), technologies, "UI 검증용 데이터",
                new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, .36, true, 0L, fixtureNational()));
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
