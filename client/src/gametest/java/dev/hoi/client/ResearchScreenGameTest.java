package dev.hoi.client;

import dev.hoi.client.audio.AudioChecks;
import dev.hoi.client.map.AtlasSurfaceRenderChecks;
import dev.hoi.client.research.ResearchArtChecks;
import dev.hoi.client.research.ResearchDetails;
import dev.hoi.client.research.ResearchLayout;
import dev.hoi.client.research.ResearchPresentation;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.client.research.ResearchSlotButton;
import dev.hoi.client.research.ResearchTabButton;
import dev.hoi.client.research.ResearchTextChecks;
import dev.hoi.client.research.ResearchTreeLabels;
import dev.hoi.client.research.TfrResearchLayout;
import dev.hoi.client.screen.AgencyScreen;
import dev.hoi.client.screen.CompletionScreenChecks;
import dev.hoi.client.screen.ConstructionScreenChecks;
import dev.hoi.client.screen.CountryScreenChecks;
import dev.hoi.client.screen.DialogScreenChecks;
import dev.hoi.client.screen.HoiMenuScreen;
import dev.hoi.client.screen.IndustryScreen;
import dev.hoi.client.screen.IndustryScreenChecks;
import dev.hoi.client.screen.RegimentScreenChecks;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.HoiMenuButton;
import dev.hoi.client.ui.HoiPanelLayout;

import dev.hoi.protocol.*;
import net.minecraft.network.chat.Component;
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


public final class ResearchScreenGameTest implements FabricClientGameTest {
    public static void gui2Screenshot(ClientGameTestContext context, String name) {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2)); context.waitTicks(3);
        context.takeScreenshot(name + "-gui2");
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
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
        AudioChecks.run(context);
        AtlasSurfaceRenderChecks.run(context);
        dev.hoi.client.map.ArmyModelRenderChecks.run(context);
        context.getInput().resizeWindow(1600, 1000);
        context.runOnClient(client -> client.options.guiScale().set(2));
        try (var world = context.worldBuilder().adjustSettings(settings -> settings.setGameMode(
                net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            context.runOnClient(client -> {
                String[] modes = {"army", "navy", "air", "operatives", "supply", "resistance", "compliance", "resources", "factions"};
                String[] names = {"기본", "전략 해군", "전략 공군", "공작원", "보급", "저항도", "순응도", "자원", "세력"};
                for (int slot = 0; slot < modes.length; slot++) {
                    var item = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CARROT_ON_A_STICK);
                    item.set(net.minecraft.core.component.DataComponents.ITEM_MODEL, Identifier.parse("hoi:map/selector/" + modes[slot]));
                    item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(names[slot]).withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GOLD).withItalic(false))
                            .append(Component.literal(" 지도 모드").withStyle(net.minecraft.ChatFormatting.WHITE)));
                    client.player.getInventory().setItem(slot, item);
                    check(client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/map_selector/" + modes[slot] + ".png")).isPresent(), "Map selector icon is packaged: " + modes[slot]);
                }
                client.player.getInventory().setSelectedSlot(0);
            });
            context.setScreen(() -> null); context.waitTicks(3); gui2Screenshot(context, "hoi-map-selector-icons");
            dev.hoi.client.audio.KoreanMusicChecks.run(context);
            dev.hoi.client.audio.UnitAudioChecks.run(context);
            var menu = fixtureMenu();
            dev.hoi.client.screen.SelectionPreviewChecks.run(context, menu);
            context.runOnClient(client -> CampaignHud.accept(HudProtocol.State.of("KOR",menu.hud())));
            context.setScreen(() -> null); context.waitTicks(3); context.takeScreenshot("hoi-persistent-country-hud");
            context.setScreen(() -> new HoiMenuScreen(menu)); context.waitTicks(2);
            context.setScreen(() -> null); context.waitTicks(2);
            context.runOnClient(client -> {
                check(CampaignHud.visible(), "Closing politics keeps the country HUD");
                CampaignHud.accept(HudProtocol.State.HIDDEN);
                check(!CampaignHud.visible(), "Stop/unassignment immediately hides the country HUD");
            });
            ResearchArtChecks.run(context);
            ResearchTextChecks.run(context);
            CompletionScreenChecks.run(context);
            ConstructionScreenChecks.run(context,menu.hud());
            CountryScreenChecks.run(context,menu.hud());
            dev.hoi.client.screen.DecisionScreenChecks.run(context,menu.hud());
            dev.hoi.client.screen.FocusScreenChecks.run(context);
            dev.hoi.client.screen.WorldTensionChecks.run(context,menu.hud());
            DialogScreenChecks.run(context);
            IndustryScreenChecks.run(context,menu.hud());
            RegimentScreenChecks.run(context);
            context.setScreen(() -> new HoiMenuScreen(menu)); context.waitTicks(5);
            context.runOnClient(client -> check(((HoiMenuScreen)client.gui.screen()).panelWidth() == HoiPanelLayout.width(MenuTab.POLITICS,client.gui.screen().width), "Country panel follows the original 726-pixel container"));
            context.takeScreenshot("hoi-menu-country");
            check(HoiMenuBar.indicators(menu.hud()).stream().limit(13).map(HoiMenuBar.Indicator::icon).toList().equals(List.of(
                    "political_power", "stability", "war_support", "manpower", "factories", "fuel", "supplies", "convoys", "command_power",
                    "army_experience", "air_experience", "navy_experience", "party_support")), "National indicators follow the requested order");
            check(HoiMenuBar.indicators(menu.hud()).stream().anyMatch(i -> i.icon().equals("nuclear") && i.label().equals("핵폭탄")), "Nuclear bombs appear from campaign start without research");
            var armed = new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, .36, true, 0L, fixtureNational());
            check(HoiMenuBar.indicators(armed).stream().anyMatch(i -> i.icon().equals("nuclear") && i.value().equals("0")), "Completed nuclear research shows zero inventory");
            check(HoiMenuBar.maximumScroll(1024, armed) == 0, "All national and financial indicators fit the normal viewport");
            context.runOnClient(client -> {
                for (var item : HoiMenuBar.indicators(armed)) {
                    if (item.icon().equals("political_power") || item.icon().equals("command_power")) {
                        check(item.value().equals(item.icon().equals("political_power") ? "1350" : "150"),
                                "Power indicators truncate fractions without rounding");
                        check(client.font.width(item.value()) <= item.width() - 18,
                                "Power indicator fits without ellipsis: " + item.icon());
                    }
                }
                var convoys = new HoiMenuBar.Indicator("convoys", "수송", 10000L, HoiMenuBar.Format.NUMBER);
                check(convoys.value().equals("10.0K") && client.font.width(convoys.value()) <= convoys.width() - 18,
                        "Ten thousand convoys fit without ellipsis");
                for (double power : new double[]{-2000, 999.99, 1000, 1350, 1999.99, 2000}) {
                    var item = new HoiMenuBar.Indicator("political_power", "정치력", power, HoiMenuBar.Format.POLITICAL_POWER);
                    check(item.value().length() <= 5 && client.font.width(item.value()) <= item.width() - 18,
                            "Political power fits five characters throughout the server range");
                }
            });
            context.runOnClient(client -> check(HoiMenuBar.scroll(427, armed, 0, 0, -100) == HoiMenuBar.maximumScroll(427, armed), "Compact HUD can reach the final indicators"));
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
            context.takeScreenshot("hoi-menu-politics"); gui2Screenshot(context, "hoi-menu-politics");
            context.runOnClient(c -> check(c.gui.screen().children().stream().noneMatch(child -> child instanceof Button b && b.getMessage().getString().equals("권력의 균형")), "Inactive power balance remains hidden"));
            var balance = new MenuView.Balance("PRC_Liberal_Conservative_power_balance", "민주주의의 바람", "보수파", "개혁파", "bop/prc_conservate_side", "bop/prc_liberal_side", .985,
                    List.of(new MenuView.Entry("보수파의 통제", "-100% ~ -15%", "", -1), new MenuView.Entry("균형", "-15% ~ 15%", "", -.15), new MenuView.Entry("개혁파의 통제", "15% ~ 100%", "", .15)));
            context.setScreen(() -> new HoiMenuScreen(new MenuView(menu.country(), menu.countryName(), menu.date(), menu.speed(), menu.pages(), menu.hud(), menu.manufacturers(), balance)));
            click(context, "권력의 균형"); context.waitTicks(2); gui2Screenshot(context, "hoi-politics-power-balance");
            context.runOnClient(c -> ((HoiMenuScreen)c.gui.screen()).update(menu));
            context.waitTicks(2);
            context.runOnClient(c -> check(c.gui.screen().children().stream().noneMatch(child -> child instanceof Button b && b.getMessage().getString().equals("권력의 균형")), "Revoked power balance closes its controls"));
            CountryScreenChecks.politicsHover(context, menu);
            click(context, "정부 선택 0 · 미지정"); context.waitTicks(2); context.takeScreenshot("hoi-politics-slot-detail");
            context.runOnClient(client -> client.gui.screen().keyPressed(ESCAPE));
            click(context, "연구 & 생산 선택 0 · 적용 중");
            context.runOnClient(client -> check(client.gui.screen().children().stream().filter(c -> c instanceof Button b
                    && b.getMessage().getString().equals("세부 사항")).count() == 1, "Armor manufacturer filter"));
            context.waitTicks(2); context.takeScreenshot("hoi-politics-manufacturers-armor");
            click(context, "군수품 산업체");
            context.runOnClient(client -> check(client.gui.screen().children().stream().filter(c -> c instanceof Button b
                    && b.getMessage().getString().equals("세부 사항")).count() == 2, "Shared artillery manufacturer also appears in materiel"));
            context.waitTicks(2); context.takeScreenshot("hoi-politics-manufacturers-materiel");
            click(context, "세부 사항"); context.waitTicks(2); context.takeScreenshot("hoi-politics-manufacturer-detail");
            context.runOnClient(client -> client.gui.screen().keyPressed(ESCAPE));
            context.runOnClient(client -> client.gui.screen().keyPressed(ESCAPE));
            for (var tab : MenuTab.ORDER) if (tab != MenuTab.RESEARCH && tab != MenuTab.INTELLIGENCE) {
                click(context, tab.label() + " 메뉴");
                context.runOnClient(client -> {
                    var screen = (HoiMenuScreen)client.gui.screen();
                    check(screen.selectedTab() == tab, "Menu tab " + tab);
                    int sourceWidth = switch (tab) { case POLITICS -> 726; case TRADE -> 620; case LOGISTICS -> 560; default -> 550; };
                    check(screen.panelWidth() == HoiPanelLayout.width(tab,screen.width), "Original container width for " + tab);
                });
            }
            for (var captureTab : List.of(MenuTab.DECISIONS, MenuTab.LOGISTICS)) {
                click(context, captureTab.label() + " 메뉴"); context.waitTicks(2); gui2Screenshot(context, "hoi-menu-" + captureTab.id());
            }
            click(context, MenuTab.OFFICER_CORPS.label() + " 메뉴");
            context.waitTicks(2); context.takeScreenshot("hoi-menu-officers"); gui2Screenshot(context, "hoi-menu-officers");
            click(context, "무역 & 경제 메뉴"); context.waitTicks(2); context.takeScreenshot("hoi-menu-economy");
            click(context, "무역"); context.waitTicks(2); context.takeScreenshot("hoi-menu-trade");
            context.getInput().resizeWindow(854, 480); context.waitTicks(3);
            context.runOnClient(client -> client.gui.screen().mouseScrolled(100, 5, 0, -100));
            context.waitTicks(2); context.takeScreenshot("hoi-hud-compact-final-indicators");
            context.runOnClient(client -> client.gui.screen().mouseScrolled(100, 5, 0, 100));
            for (var tab : List.of(MenuTab.POLITICS, MenuTab.TRADE, MenuTab.RECRUITMENT)) {
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
        for (var tab : List.of("보병", "포", "해군", "공군", "기갑", "공학", "산업")) {
            click(context, tab); context.waitTicks(2);
            context.takeScreenshot("hoi-research-labels-" + tab);
            context.runOnClient(client -> {
                var screen = (ResearchScreen)client.gui.screen();
                var tabs=screen.children().stream().filter(w -> w instanceof ResearchTabButton).map(w -> (ResearchTabButton)w).toList();
                check(screen.treeTop()==tabs.getFirst().getBottom()-1,"Research tabs join the tree panel without a control-row gap");
                check(screen.children().stream().noneMatch(w -> w instanceof net.minecraft.client.gui.components.EditBox),"Technology search is removed");
                check(tabs.size()==9 && tabs.getFirst().getX()==10 && tabs.getLast().getRight()<screen.width/2,"Nine research tabs stay compact and left aligned");
                for(int i=1;i<tabs.size();i++) check(tabs.get(i).getX()==tabs.get(i-1).getRight(),"Tabs are adjacent");
                for (var category : List.of("INFANTRY", "SUPPORT", "ARMOR", "ARTILLERY", "NAVY", "NAVAL_SUPPORT", "AIR", "ENGINEERING", "INDUSTRY")) {
                    var tree = ResearchPresentation.apply(ResearchLayout.create(view.technologies(), category, ""), client.getResourceManager(), screen.width - 20);
                    checkResearchLabels(tree, client.font::width, client.font.lineHeight);
                }
                check(screen.children().stream().filter(w -> w instanceof Button).map(w -> ((Button)w).getMessage().getString())
                        .noneMatch(text -> text.equals("처음 위치") || text.equals("슬롯 목록")), "Removed tree controls stay absent");
            });
        }
        context.getInput().resizeWindow(854,480); context.waitTicks(3);
        for (var tab : List.of("보병", "포", "해군", "공군", "산업")) {
            click(context, tab); context.waitTicks(2); context.takeScreenshot("hoi-research-labels-compact-" + tab);
        }
        context.runOnClient(client -> {
            for (var category : List.of("INFANTRY", "SUPPORT", "ARMOR", "ARTILLERY", "NAVY", "NAVAL_SUPPORT", "AIR", "ENGINEERING", "INDUSTRY")) {
                var tree = ResearchPresentation.apply(ResearchLayout.create(view.technologies(), category, ""), client.getResourceManager(), client.gui.screen().width - 20);
                checkResearchLabels(tree, client.font::width, client.font.lineHeight);
            }
        });
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
        for (String id : List.of("advanced_ship_hull_light", "artillery2", "concentrated_industry", "tech_engineers4")) {
            context.runOnClient(client -> {
                var screen = (ResearchScreen)client.gui.screen(); screen.showDetail("hoi:tfr/technology/" + id);
                var tech = view.technology("hoi:tfr/technology/" + id);
                check(screen.children().stream().filter(w -> w instanceof Button).map(w -> ((Button)w).getMessage().getString())
                        .noneMatch(text -> text.startsWith("슬롯 ") && text.contains("▸")), "Detail slot cycling button is absent");
                var text = screen.detailLines(tech).stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));
                check(!text.contains("해금 장비·시설") && !text.contains("아래 선행 연구") && !text.contains("hoi:"), "Internal IDs and general prerequisites stay hidden");
                check(text.contains("택일 연구") == !tech.source().excludes().isEmpty(), "Only mutually exclusive choices receive the choice explanation");
                if (id.equals("advanced_ship_hull_light")) {
                    var cards = ResearchDetails.cards(tech);
                    check(cards.size() == 2 && cards.get(1).effects().contains("속도: +35%"), "Source hull and engine cards retain their separate stats");
                    for (var card : cards) check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + card.texture() + ".png")).isPresent(), "Source detail image exists");
                }
            });
            context.waitTicks(2); context.takeScreenshot("hoi-detail-cards-" + id);
            if (id.equals("advanced_ship_hull_light")) {
                context.runOnClient(client -> { var screen = (ResearchScreen)client.gui.screen(); screen.mouseScrolled(screen.width/3.0,screen.height/2.0,0,-7); });
                context.waitTicks(2); context.takeScreenshot("hoi-detail-engine-effects");
            }
        }
        context.getInput().resizeWindow(854,480); context.waitTicks(2); context.takeScreenshot("hoi-detail-cards-compact");
        context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION,"loading",null,r -> {}));
        context.waitTicks(2); context.takeScreenshot("hoi-loading-blank-flag");
        context.runOnClient(client -> check(HoiMenuBar.flagTexture("").isEmpty() && HoiMenuBar.flagTexture("KOR").equals("country/kor/flag"), "Loading flag stays blank until the country is known"));

    }

    private static void checkResearchLabels(ResearchLayout tree, java.util.function.ToIntFunction<String> textWidth, int lineHeight) {
        var labels = ResearchTreeLabels.create(tree, textWidth, lineHeight);
        check(labels.size() == TfrResearchLayout.yearLabels(tree).size() + TfrResearchLayout.headings(tree).size(), "Every year and equipment heading remains visible");
        for (var heading : TfrResearchLayout.headings(tree)) {
            var label = labels.stream().filter(l -> !l.year() && l.text().equals(heading.text())).findFirst().orElseThrow();
            check(label.y() <= heading.y(), "Equipment heading stays above its research row: " + label.text());
            check(heading.y() - label.y() <= lineHeight * 3, "Equipment heading stays near its research row: " + label.text());
        }
        if (ResearchLayout.category(tree.nodes().getFirst().tech()).equals("NAVY")) {
            var targets = java.util.Map.of("초계함", "early_ship_hull_light", "구축함", "early_ship_hull_cruiser", "방어력", "basic_cruiser_armor_scheme",
                    "미사일 순양함", "early_ship_hull_heavy", "항공모함", "early_ship_hull_carrier", "잠수함", "early_ship_hull_submarine");
            for (var target : targets.entrySet()) {
                var label = labels.stream().filter(l -> l.text().equals(target.getKey())).findFirst().orElseThrow();
                var card = tree.nodes().stream().filter(n -> n.tech().source().id().equals(target.getValue())).findFirst().orElseThrow();
                check(label.y() + label.height() + 4 <= card.y(), "Naval heading is above its own equipment card: " + label.text());
            }
        }
        for (var label : labels) {
            check(label.x() >= 0 && label.y() >= 0 && label.y() + label.height() <= tree.height(), "Caption remains in scrollable tree: " + label.text());
            for (var card : tree.nodes()) check(!(label.x() - 2 < card.x() + card.width() && label.x() + label.width() + 2 > card.x()
                    && label.y() - 2 < card.y() + card.height() && label.y() + label.height() + 2 > card.y()), "Caption overlaps card: " + label.text());
            for (var other : labels) if (label != other) check(!(label.x() - 2 < other.x() + other.width() && label.x() + label.width() + 2 > other.x()
                    && label.y() - 2 < other.y() + other.height() && label.y() + label.height() + 2 > other.y()), "Captions overlap: " + label.text());
        }
    }

    private static void loadPack(ClientGameTestContext context) {
        var pack = Path.of(System.getProperty("hoi.testResourcePack"));
        check(Files.isRegularFile(pack), "Build HOI-resourcepack first, or pass -PhoiResourcePack=<resources.zip>");
        var reload = new AtomicReference<CompletableFuture<Void>>();
        context.runOnClient(client -> {
            var selected = new ArrayList<>(client.getResourcePackRepository().getSelectedIds());
            selected.removeIf(id -> id.startsWith("file/hoi-test"));
            try {
                Files.createDirectories(client.getResourcePackDirectory());
                var parts = new ArrayList<Path>();
                var manifest = pack.resolveSibling("resource-packs.json");
                if (Files.isRegularFile(manifest)) {
                    var data = com.google.gson.JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
                    for (var row : data.getAsJsonArray("packs"))
                        parts.add(pack.resolveSibling(row.getAsJsonObject().get("file").getAsString()));
                } else parts.add(pack);

                for (int i = 0; i < parts.size(); i++) {
                    String name = "hoi-test-" + i + ".zip";
                    Files.copy(parts.get(i), client.getResourcePackDirectory().resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    selected.add("file/" + name);
                }

                com.google.gson.JsonObject fixture;
                try (var input = ResearchScreenGameTest.class.getResourceAsStream("/atlas-client-scene.json")) {
                    fixture = com.google.gson.JsonParser.parseString(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                }
                var target = client.getResourcePackDirectory().resolve("hoi-test-carriers.zip");
                try (var output = new java.util.zip.ZipOutputStream(Files.newOutputStream(target))) {
                    output.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));
                    output.write("{\"pack\":{\"description\":\"HOI test carriers\",\"min_format\":88,\"max_format\":88}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    output.closeEntry();
                    for (var entry : fixture.getAsJsonObject("blockstates").entrySet()) {
                        output.putNextEntry(new java.util.zip.ZipEntry(entry.getKey()));
                        output.write(entry.getValue().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); output.closeEntry();
                    }
                    for (var entry : fixture.getAsJsonObject("generatedAssets").entrySet()) {
                        output.putNextEntry(new java.util.zip.ZipEntry(entry.getKey()));
                        output.write(java.util.Base64.getDecoder().decode(entry.getValue().getAsString())); output.closeEntry();
                    }
                }
                selected.add("file/hoi-test-carriers.zip");
            } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
            var repository = client.getResourcePackRepository(); repository.reload();
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
        var create = new AgencyView.Item("create", "agents", "정보기관 설립", "민간공장 5개 · 30일", "", "agency/recruit", "설립", true, -1, List.of());
        var before = new AgencyView("agency-create", 1, "KOR", "국가정보원", "미설립", List.of(create), "");
        var creationRequests = new ArrayList<AgencyProtocol.Request>();
        context.setScreen(() -> new AgencyScreen(null, before.session(), before, creationRequests::add)); context.waitTicks(2);
        gui2Screenshot(context, "hoi-agency-before-creation");
        context.runOnClient(client -> check(client.gui.screen().children().stream().anyMatch(c -> c instanceof Button b && b.getMessage().getString().equals("정보공동체") && !b.active), "Unestablished agency disables upgrades"));
        click(context, "정보기관 창설");
        context.runOnClient(client -> {
            check(creationRequests.getLast().item().equals("create") && creationRequests.getLast().revision() == 1, "Creation uses issued action and revision");
            var project = new AgencyView.Item("cancel_project", "upgrades", "기관 설립", "기관 설립 · 15일 남음", "", "agency/upgrade", "취소", true, .5, List.of());
            ((AgencyScreen) client.gui.screen()).update(new AgencyView(before.session(), 2, "KOR", before.name(), "창설 중", List.of(project), ""));
        });
        context.waitTicks(2); gui2Screenshot(context, "hoi-agency-creation-progress");
        context.getInput().setCursorPos(220, 155); context.waitTicks(3);
        context.takeScreenshot("hoi-agency-creation-hover");
        context.getInput().setCursorPos(520, 96); context.waitTicks(3);
        context.takeScreenshot("hoi-agency-close-tooltip");
        context.getInput().setCursorPos(1500, 800);
        click(context, "기관 창설 취소");
        context.runOnClient(client -> check(creationRequests.getLast().item().equals("cancel_project") && creationRequests.getLast().revision() == 2, "Creation cancellation uses latest revision"));
        var items = new ArrayList<AgencyView.Item>();
        items.add(new AgencyView.Item("recruit", "agents", "첩보원 고용", "1 / 2", "새로운 요원을 고용합니다.", "agency/recruit", "고용", true, -1, List.of()));
        items.add(new AgencyView.Item("spy_master", "agents", "세력 첩보장", "", "", "agency/spy_master", "취임", false, -1, List.of()));
        String[] upgradeNames = {"외국 정보", "국내 정보", "군사 정보", "계획 및 지휘", "수집", "처리 및 활용", "분석", "전파", "인적 정보", "신호 정보", "계측기호정보", "공개출처정보", "지형공간정보", "점조직 체계", "통신 보안", "지원 서비스", "강화된 심문 기술", "제거", "전자정보", "통신정보", "암호 분석 공격 모델", "암호화 체계 연산법 개선", "양자 암호학"};
        int[] maxima = {4,4,4,1,1,1,1,1,1,1,2,2,3,3,3,3,3,2,1,3,4,4,3};
        for (int i = 0; i < AgencyScreen.UPGRADE_ORDER.size(); i++) {
            String upgrade = AgencyScreen.UPGRADE_ORDER.get(i);
            int level = i % 3 == 0 ? maxima[i] : i % 3 == 1 ? maxima[i] / 2 : 0;
            items.add(new AgencyView.Item("upgrade:" + upgrade, "upgrades", upgradeNames[i], level + " / " + maxima[i], "민간공장 5개 · 30일", "agency/" + upgrade, "개선", i % 3 == 1, level / (double)maxima[i], List.of()));
        }
        dev.hoi.client.screen.AgencyScreenChecks.run(context, items);
        items.add(new AgencyView.Item("decrypt:PRK", "cryptology", "북한 암호", "해독 중", "해독 진행: 1200 / 12000", "agency/cryptology", "일시 정지", true, .1, List.of()));
        items.add(new AgencyView.Item("operation:CAPTURE_CIPHER", "operations", "암호 탈취", "60일", "정보망 50과 대기 요원 2명이 필요합니다.\n민간공장 3개가 사용됩니다.", "agency/capture_cipher", "작전 준비", true, -1,
                List.of(new AgencyView.Parameter("대상 국가", List.of(new AgencyView.Choice("PRK", "북한"), new AgencyView.Choice("JAP", "일본"))))));
        var fixture = new AgencyView("agency-fixture", 1, "KOR", "국가정보원", "무선 감청 · 21일 남음", items, "검증용 기관 데이터");
        var requests = new ArrayList<AgencyProtocol.Request>();
        context.setScreen(() -> new AgencyScreen(null, fixture.session(), fixture, requests::add)); context.waitTicks(2);
        context.takeScreenshot("hoi-agency-operatives");
        context.runOnClient(client -> {
            for (var label : List.of("모집된 요원", "적에게 포획된 요원", "적에게 사살당한 정보원")) {
                var indicator = client.gui.screen().children().stream().filter(c -> c instanceof Button b && b.getMessage().getString().equals(label)).map(c -> (Button)c).findFirst().orElseThrow();
                check(!indicator.active, "Operative indicators keep tooltips without accepting clicks");
            }
        });
        click(context, "정보원 모집"); context.waitTicks(2); click(context, "고용");
        context.runOnClient(client -> {
            check(requests.size() == 1 && requests.getFirst().kind() == AgencyProtocol.Kind.CALL && requests.getFirst().revision() == 1, "Agency uses issued session and revision");
            check(client.gui.screen().children().stream().anyMatch(c -> c instanceof Button b && !b.active && b.getMessage().getString().equals("응답 대기…")), "Pending agency action disabled");
            ((AgencyScreen)client.gui.screen()).update(new AgencyView(fixture.session(), 2, "KOR", fixture.name(), fixture.status(), items, "고용 완료"));
        });
        click(context, "닫기"); click(context, "정보공동체"); context.waitTicks(2); context.takeScreenshot("hoi-agency-upgrades"); gui2Screenshot(context, "hoi-agency-upgrades");
        context.runOnClient(client -> check(client.gui.screen().children().stream().anyMatch(c -> c instanceof Button b && b.getMessage().getString().equals("암호학") && !b.active), "Upgrade overlay blocks background tabs"));
        click(context, "개선 창 닫기"); click(context, "암호학"); context.waitTicks(2); context.takeScreenshot("hoi-agency-cryptology"); gui2Screenshot(context, "hoi-agency-cryptology");
        click(context, "작전"); click(context, "암호 탈취"); context.waitTicks(2); context.takeScreenshot("hoi-agency-operation");
        click(context, "대상 국가: 북한"); context.waitTicks(1); context.takeScreenshot("hoi-agency-operation-target-japan"); click(context, "준비하기");
        context.runOnClient(client -> {
            check(requests.getLast().arguments().equals(List.of("JAP")), "Choice picker sends selected target without country authority");
            ((AgencyScreen)client.gui.screen()).update(new AgencyView(fixture.session(), 0, "", "", "", List.of(), "권한 만료"));
            check(client.gui.screen() == null, "Revocation closes even a newer private snapshot");
        });
        context.setScreen(() -> null);
    }
    private static void movement(ClientGameTestContext context) {
        click(context, "국가 정보 메뉴");
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
        click(context, "국가 중점");
        context.runOnClient(client -> check(!client.options.keyUp.isDown() && client.gui.screen() instanceof dev.hoi.client.screen.FocusScreen, "Full focus tree releases sidebar movement"));
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
        var pages = MenuTab.ORDER.stream().map(tab -> new MenuView.Page(tab, tab == MenuTab.POLITICS ? CountryScreenChecks.politics()
                : tab == MenuTab.TRADE ? List.of(
                        new MenuView.Section("경제 현황", "trade", List.of(new MenuView.Entry("민간공장", "20", "화면 검증용 데이터"),
                                new MenuView.Entry("군수공장", "18", "화면 검증용 데이터"))),
                        new MenuView.Section("자원 무역", "trade", List.of(new MenuView.Entry("석유", "38", "화면 검증용 데이터"),
                                new MenuView.Entry("강철", "200", "화면 검증용 데이터"))))
                : tab == MenuTab.OFFICER_CORPS ? fixtureOfficers()
                : List.of(new MenuView.Section(tab.label(), tab.id(), List.of(new MenuView.Entry("진행 현황", "12", "검증용 데이터")))))).toList();
        return new MenuView("KOR", "대한민국", "2020년 1월 1일 00시", "PAUSED", pages,
                new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, .36, false, null, fixtureNational()), List.of(
                        new MenuView.Manufacturer("fixture:artillery", List.of("armor", "materiel"), new MenuView.Entry("포 제조사", "포 · 기갑", "검증용: 포 연구 +5% · 기갑 연구 +10%", -1, "politics/tank_manufacturer")),
                        new MenuView.Manufacturer("fixture:infantry", List.of("materiel"), new MenuView.Entry("보병 장비 제조사", "보병", "검증용: 보병 연구 +15%", -1, "politics/materiel_manufacturer"))));
    }
    private static List<MenuView.Section> fixtureOfficers() {
        var sections = new ArrayList<MenuView.Section>();
        sections.add(new MenuView.Section("최고사령부", "high_command", java.util.stream.IntStream.range(0, 3)
                .mapToObj(i -> new MenuView.Entry("최고사령부 " + (i + 1), "미지정", "정치에서 임명", 0, "politics/high_command")).toList()));
        sections.add(new MenuView.Section("국가 선호 전술", "preferred_tactic", List.of(
                new MenuView.Entry("국가 선호 전술", "미지정", "육군 교리", 0, "officer/preferred_tactic"))));
        for (var branch : List.of("army", "navy", "air")) {
            String name = branch.equals("army") ? "육군" : branch.equals("navy") ? "해군" : "공군";
            var entries = new ArrayList<MenuView.Entry>();
            entries.add(new MenuView.Entry(name + " 참모총장", "미지정", "정치에서 임명", 0, "politics/" + branch + "_chief"));
            entries.add(new MenuView.Entry(name + " 교리", "미지정", "미지정 교리", 0, "officer/" + branch + "/doctrine"));
            for (var medal : branch.equals("air") ? List.of("air", "command") : List.of("academy", branch, "command"))
                entries.add(new MenuView.Entry("정신", "미지정", "정신 선택", 0, "officer/medal/" + medal));
            sections.add(new MenuView.Section(name + " 사령부", branch, entries));
        }
        sections.add(new MenuView.Section("특수부대", "special", List.of(
                new MenuView.Entry("특수부대 교리", "미지정", "미지정 교리", 0, "officer/special/doctrine"))));
        return sections;
    }
    private static CountryHud.NationalIndicators fixtureNational() {
        return new CountryHud.NationalIndicators(1350.75, .62, .48, 45L, .85, 894_920.0, 580.0, .72, 10000L, .95, 150.75, .36, 280_900L);
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
