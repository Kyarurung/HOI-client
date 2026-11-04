package dev.hoi.client;

import dev.hoi.client.audio.AudioChecks;
import dev.hoi.client.map.scene.SceneSurfaceRenderChecks;
import dev.hoi.client.research.ResearchArtChecks;
import dev.hoi.client.research.ResearchDetails;
import dev.hoi.client.research.ResearchLayout;
import dev.hoi.client.research.ResearchPresentation;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.client.research.ResearchSlotButton;
import dev.hoi.client.research.ResearchTabButton;
import dev.hoi.client.research.ResearchTextChecks;
import dev.hoi.client.research.ResearchTreeLabels;
import dev.hoi.client.research.ResearchSourceLayout;
import dev.hoi.client.screen.agency.AgencyScreen;
import dev.hoi.client.screen.dialog.DialogCompletionChecks;
import dev.hoi.client.screen.industry.IndustryConstructionTest;
import dev.hoi.client.screen.diplomacy.DiplomacyCountryScreenChecks;
import dev.hoi.client.screen.dialog.DialogScreenChecks;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.client.screen.industry.IndustryScreen;
import dev.hoi.client.screen.industry.IndustryScreenChecks;
import dev.hoi.client.screen.industry.IndustryRegimentChecks;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.protocol.research.*;

import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class ResearchScreenTest implements FabricClientGameTest {
    public static void gui2Screenshot(ClientGameTestContext context, String name) {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2)); context.waitTicks(3);
        context.takeScreenshot(name + "-gui2");
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
    private static final KeyEvent ENTER = new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
    private static final KeyEvent ESCAPE = new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE, 0, 0);
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>();
            HoiClient.registerCommands(dispatcher);
            check(dispatcher.getRoot().getChild("hoi") == null, "Never shadow the server /hoi command");
            check(dispatcher.getRoot().getChild("hoi-research") == null, "Removed research shortcut stays absent");
            check(dispatcher.getRoot().getChild("준비") == null && dispatcher.getRoot().getChild("시작") == null, "Lobby commands belong to the server");
            ResearchPayloads.registerPayloadTypes(); ResearchPayloads.registerPayloadTypes();
        });
        if(System.getProperty("hoi.testSuite","all").equals("network-campaign")) {
            dev.hoi.client.screen.ScreenNetworkCampaignTest.run(context);
            return;
        }
        loadPack(context);
        if(Set.of("all","naval-invasion-map").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.naval.NavalInvasionOrdersTest.run(context,fixtureMenu(),world.getServer()::runCommand);}
            if(System.getProperty("hoi.testSuite","all").equals("naval-invasion-map"))return;
        }
        if (Set.of("all", "campaign-source").contains(System.getProperty("hoi.testSuite", "all")) && System.getenv("HOI_CAMPAIGN_PREVIEW") != null) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.ScreenCampaignSourceTest.run(context); }
            if (System.getProperty("hoi.testSuite", "all").equals("campaign-source")) return;
        }
        if (Set.of("all", "ui-headers").contains(System.getProperty("hoi.testSuite", "all"))) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.ui.UiMenuHeadingTest.run(context, fixtureMenu()); }
            if (System.getProperty("hoi.testSuite", "all").equals("ui-headers")) return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("mio-availability")) {
            try(var world=context.worldBuilder().create()) { dev.hoi.client.screen.industry.IndustryMioAvailabilityTest.run(context); }
            return;
        }
        if(Set.of("all","design-team-candidates").contains(System.getProperty("hoi.testSuite","all"))) {
            try(var world=context.worldBuilder().create()) {
                dev.hoi.client.screen.agency.AgencyDesignTeamCandidatesTest.run(context);
                dev.hoi.client.research.ResearchSlotTooltipTest.runSource(context);
            }
            if(System.getProperty("hoi.testSuite","all").equals("design-team-candidates"))return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("selection-review")) {
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderSelectionTest.run(context,fixtureMenu());dev.hoi.client.map.commander.CommanderHudTest.runArmyGroups(context,fixtureMenu());}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("presentation-review")) {
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.industry.IndustryQueueDragTest.run(context,fixtureMenu().hud());dev.hoi.client.map.commander.CommanderPresentationTest.run(context,fixtureMenu());dev.hoi.client.map.commander.CommanderSelectionTest.run(context,fixtureMenu());dev.hoi.client.screen.map.MapSeaAccessTest.run(context);}
            return;
        }
        if(Set.of("all","commander-presentation").contains(System.getProperty("hoi.testSuite","all"))) {
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderPresentationTest.run(context,fixtureMenu());dev.hoi.client.map.commander.CommanderSelectionTest.run(context,fixtureMenu());}
            if(System.getProperty("hoi.testSuite","all").equals("commander-presentation"))return;
        }
        if(Set.of("all","sea-access").contains(System.getProperty("hoi.testSuite","all"))) {
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.map.MapSeaAccessTest.run(context);}
            if(System.getProperty("hoi.testSuite","all").equals("sea-access"))return;
        }
        if(Set.of("all","queue-drag").contains(System.getProperty("hoi.testSuite","all"))) {
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.industry.IndustryQueueDragTest.run(context,fixtureMenu().hud());}
            if(System.getProperty("hoi.testSuite","all").equals("queue-drag"))return;
        }
        if(Set.of("all","production-scrollbars").contains(System.getProperty("hoi.testSuite","all"))) {
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.screen.industry.IndustryProductionScrollTest.run(context);
                dev.hoi.client.screen.industry.IndustryConstructionScrollTest.run(context,fixtureMenu().hud());
            }
            if(System.getProperty("hoi.testSuite","all").equals("production-scrollbars"))return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("tooltip-layout")){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.ui.UiTooltipSpacingTest.geometry(context);
                dev.hoi.client.screen.ScreenTooltipSpacingTest.run(context);
                dev.hoi.client.screen.army.ArmyDivisionDialogsTest.run(context,fixtureMenu());
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("research-slots")) {
            try (var world = context.worldBuilder().create()) {
                context.getInput().resizeWindow(854, 480); context.waitTicks(3);
                for (int count : List.of(5, 6)) {
                    var view = fixtureResearch();
                    var slots = java.util.stream.IntStream.range(0, count).mapToObj(i -> new ResearchView.Slot(i, "", 0)).toList();
                    var compact = new ResearchView(view.session(), view.revision(), view.country(), view.countryName(), view.day(), view.date(), view.speed(), slots, view.technologies(), view.message());
                    context.setScreen(() -> new ResearchScreen(compact, request -> {})); context.waitTicks(2);
                    context.runOnClient(client -> checkCompactSlots(client.gui.screen(), count));
                    context.takeScreenshot("hoi-compact-research-slots-" + count);
                }
                dev.hoi.client.research.ResearchSlotTooltipTest.run(context, fixtureResearch());
            }
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("source-layout")){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.screen.industry.IndustryOrganizationAssignmentTest.run(context);
                dev.hoi.client.screen.commander.CommanderOfficerContractTest.run(context);
                dev.hoi.client.screen.industry.IndustryDivisionDesignerTest.run(context);
            }
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("portrait-research")){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.map.commander.CommanderLeaderControlsTest.run(context,fixtureMenu());
                dev.hoi.client.map.commander.CommanderPresentationTest.run(context,fixtureMenu());
                dev.hoi.client.research.ResearchSectionsTest.run(context);
            }
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("screen-polish")){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.screen.industry.IndustryDivisionDesignerTest.run(context);
                dev.hoi.client.screen.industry.IndustryProductionOfferTest.run(context);
                dev.hoi.client.research.ResearchSectionsTest.run(context);
            }
            return;
        }
        if(Set.of("all","division-designer").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.industry.IndustryDivisionDesignerTest.run(context);}
            if(System.getProperty("hoi.testSuite","all").equals("division-designer"))return;
        }
        if(Set.of("all","mio-types","mio-long-type").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.industry.IndustryOrganizationTest.types(context);}
            if(!System.getProperty("hoi.testSuite","all").equals("all"))return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("source-scrollbars")){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.screen.commander.CommanderOfficerContractTest.run(context);
                dev.hoi.client.screen.commander.CommanderRosterTest.run(context);
                dev.hoi.client.screen.industry.IndustryOrganizationAssignmentTest.run(context);
                dev.hoi.client.screen.industry.IndustryOrganizationTest.run(context);
                dev.hoi.client.screen.army.ArmyDivisionDialogsTest.run(context,fixtureMenu());
                dev.hoi.client.screen.commander.CommanderOfficerComparisonTest.run(context);
                dev.hoi.client.screen.commander.CommanderPreferredTacticsTest.run(context);
            }
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("officer-comparison")){try(var world=context.worldBuilder().create()){dev.hoi.client.screen.commander.CommanderOfficerComparisonTest.run(context);dev.hoi.client.screen.commander.CommanderPreferredTacticsTest.run(context);}return;}
        if(System.getProperty("hoi.testSuite","all").equals("final-review")){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.screen.industry.IndustryOrganizationAssignmentTest.run(context);
                dev.hoi.client.screen.industry.IndustryOrganizationTest.run(context);
                dev.hoi.client.map.commander.CommanderArmyTooltipTest.run(context,fixtureMenu());
                dev.hoi.client.screen.army.ArmyDivisionDialogsTest.run(context,fixtureMenu());
            }
            return;
        }
        if(Set.of("all","final-contract").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.research.ResearchSectionsTest.run(context);
                dev.hoi.client.screen.occupation.OccupationTerritoryReturnTest.run(context);
                dev.hoi.client.screen.commander.CommanderOfficerContractTest.run(context);
            }
            if(System.getProperty("hoi.testSuite","all").equals("final-contract"))return;
        }
        if(Set.of("all","production-offer").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.industry.IndustryProductionOfferTest.run(context);}
            if(System.getProperty("hoi.testSuite","all").equals("production-offer"))return;
        }
        if(Set.of("all","organization-assignment").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.industry.IndustryOrganizationAssignmentTest.run(context);}
            if(System.getProperty("hoi.testSuite","all").equals("organization-assignment"))return;
        }
        if(Set.of("all","organizations").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.industry.IndustryOrganizationTest.run(context);}
            if(System.getProperty("hoi.testSuite","all").equals("organizations"))return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("sea-transfer")){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.map.commander.CommanderMissionOverlayRenderTest.run(context,world.getServer()::runCommand);
                dev.hoi.client.map.scene.SceneTransportModelTest.run(context,world.getServer()::runCommand);
                dev.hoi.client.map.commander.CommanderSeaTransferTest.run(context,world.getServer()::runCommand);
            }
            return;
        }
        if(Set.of("all","advisor-politics").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.politics.PoliticsAdvisorTest.run(context);}
            if(System.getProperty("hoi.testSuite","all").equals("advisor-politics"))return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("research-background")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.research.ResearchBackgroundTest.run(context); }
            return;
        }
        if (Set.of("all", "asset-preparation").contains(System.getProperty("hoi.testSuite", "all"))) {
            try (var world = context.worldBuilder().create()) {
                if (System.getProperty("hoi.testSuite", "all").equals("asset-preparation")) dev.hoi.client.ui.UiCountryDisplayRenderTest.run(context, fixtureMenu());
                dev.hoi.client.research.ResearchBackgroundTest.run(context);
                dev.hoi.client.map.scene.SceneTransportModelTest.run(context, world.getServer()::runCommand);
                if(System.getProperty("hoi.testSuite","all").equals("all"))dev.hoi.client.map.commander.CommanderSeaTransferTest.run(context,world.getServer()::runCommand);
            }
            if (System.getProperty("hoi.testSuite", "all").equals("asset-preparation")) return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("country-assets")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.ui.UiCountryDisplayRenderTest.run(context,fixtureMenu());}
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("focus-detail")) {
            context.runOnClient(client -> client.options.guiScale().set(2));
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.focus.FocusScreenChecks.run(context);
            }
            return;
        }
        if (Set.of("all", "story-signals").contains(System.getProperty("hoi.testSuite", "all"))) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.ui.UiCountryDisplayRenderTest.run(context, fixtureMenu());
                dev.hoi.client.audio.AudioStorySignalsTest.run(context);
            }
            if (System.getProperty("hoi.testSuite", "all").equals("story-signals")) return;
        }
        if (Set.of("all", "event-tooltips").contains(System.getProperty("hoi.testSuite", "all"))) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.dialog.DialogEventTooltipTest.run(context); }
            if (System.getProperty("hoi.testSuite", "all").equals("event-tooltips")) return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("event-frames")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.dialog.DialogEventFrameTest.run(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("all")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.dialog.DialogEventFrameTest.run(context); }
        }
        if (System.getProperty("hoi.testSuite", "all").equals("scoreboard")) {
            try (var world = context.worldBuilder().create()) { ClientScoreboardChecks.run(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("leader-event")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.dialog.DialogLeaderEventTest.run(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("leader-event-regression")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.dialog.DialogLeaderEventTest.regression(context); }
            return;
        }
        if(Set.of("all","tooltip-spacing","review-tooltips").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.ui.UiTooltipSpacingTest.geometry(context);dev.hoi.client.map.commander.CommanderTooltipSpacingTest.run(context,fixtureMenu());dev.hoi.client.screen.ScreenTooltipSpacingTest.run(context);}
            if(System.getProperty("hoi.testSuite","all").equals("tooltip-spacing"))return;
        }
        if(Set.of("all","trait-experience","review-tooltips").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.ScreenTooltipSpacingTest.experience(context);}
            if(!System.getProperty("hoi.testSuite","all").equals("all"))return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("commander-roster")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.ui.UiTooltipCursorTest.run(context);dev.hoi.client.screen.commander.CommanderRosterTest.run(context);dev.hoi.client.screen.diplomacy.DiplomacyActionsTest.run(context);}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("commander-traits")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.commander.CommanderTraitTreeTest.run(context);dev.hoi.client.map.commander.CommanderHudTest.runTraitOverlay(context,fixtureMenu());}
            return;
        }
        if(Set.of("all","cursor-tooltips").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.ui.UiTooltipCursorTest.run(context);dev.hoi.client.map.commander.CommanderCursorTooltipTest.run(context,fixtureMenu());dev.hoi.client.screen.commander.CommanderTraitTreeTest.run(context);}
            if(System.getProperty("hoi.testSuite","all").equals("cursor-tooltips"))return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("diplomacy-actions")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.diplomacy.DiplomacyActionsTest.run(context);}
            return;
        }
        if(Set.of("all","army-tooltips").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderArmyTooltipTest.run(context,fixtureMenu());dev.hoi.client.screen.army.ArmyDivisionDialogsTest.run(context,fixtureMenu());}
            if(System.getProperty("hoi.testSuite","all").equals("army-tooltips"))return;
        }
        if(Set.of("all","commander-contract").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.commander.CommanderContractTest.run(context);dev.hoi.client.map.commander.CommanderLeaderControlsTest.runHeadquartersDeployment(context,fixtureMenu());}
            if(System.getProperty("hoi.testSuite","all").equals("commander-contract"))return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("all")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderLeaderControlsTest.run(context,fixtureMenu());}
        }
        if(System.getProperty("hoi.testSuite","all").equals("commander-layout")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.commander.CommanderRosterTest.run(context);dev.hoi.client.screen.commander.CommanderTraitTreeTest.run(context);dev.hoi.client.screen.commander.CommanderAdmiralSourceTest.run(context);dev.hoi.client.map.commander.CommanderLeaderControlsTest.run(context,fixtureMenu());dev.hoi.client.screen.diplomacy.DiplomacyActionsTest.run(context);dev.hoi.client.map.commander.CommanderHudTest.runTraitOverlay(context,fixtureMenu());}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("commander-controls")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderHudTest.runArmyLayout(context,fixtureMenu());}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("army-groups")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderHudTest.runArmyGroups(context,fixtureMenu());}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("headquarters-layout")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderHudTest.runHeadquartersLayout(context,fixtureMenu());}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("navy-layout")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderNavyLayoutTest.run(context,fixtureMenu());}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("army-layout")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.commander.CommanderRosterTest.run(context);dev.hoi.client.map.commander.CommanderHudTest.runArmyLayout(context,fixtureMenu());}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("map-orders")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderRightClickTest.run(context,world.getServer()::runCommand);}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("air-range")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderAirRangeRenderTest.run(context,world.getServer()::runCommand);}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("mission-overlays")){
            try(var world=context.worldBuilder().create()){
                var server=world.getServer();server.runCommand("gamerule minecraft:send_command_feedback false");server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamemode spectator @a");
                dev.hoi.client.map.commander.CommanderMissionOverlayRenderTest.run(context,server::runCommand);
            }
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("movement-controls")){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.map.commander.CommanderHudTest.run(context,fixtureMenu());
                var server=world.getServer();server.runCommand("gamerule minecraft:send_command_feedback false");server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamemode spectator @a");
                dev.hoi.client.map.commander.CommanderMissionOverlayRenderTest.run(context,server::runCommand);
            }
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("direct-movement")){
            try(var world=context.worldBuilder().create()){dev.hoi.client.map.commander.CommanderHudTest.runMovementInput(context,fixtureMenu(),world.getServer()::runCommand);}
            return;
        }
        if(System.getProperty("hoi.testSuite","all").equals("all")){
            try(var world=context.worldBuilder().create()){
                dev.hoi.client.map.commander.CommanderRightClickTest.run(context,world.getServer()::runCommand);
                dev.hoi.client.map.commander.CommanderAirRangeRenderTest.run(context,world.getServer()::runCommand);
            }
        }
        if(Set.of("all","captivity").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.commander.CommanderCaptivityTest.run(context,fixtureMenu());}
            catch(java.io.IOException error){throw new RuntimeException(error);}
        }
        if(Set.of("all","headquarters","captivity").contains(System.getProperty("hoi.testSuite","all"))){
            try(var world=context.worldBuilder().create()){dev.hoi.client.screen.army.ArmyHeadquartersTest.run(context,fixtureMenu());}
            catch(java.io.IOException error){throw new RuntimeException(error);}
            if(Set.of("headquarters","captivity").contains(System.getProperty("hoi.testSuite","all")))return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("diplomacy-lease")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.diplomacy.DiplomacyActionsTest.run(context);
                dev.hoi.client.screen.diplomacy.DiplomacyLeaseInputTest.run(context);
                dev.hoi.client.screen.diplomacy.DiplomacyLeaseTest.run(context); dev.hoi.client.screen.diplomacy.DiplomacyLeaseTest.runSource(context);
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("diplomacy")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.diplomacy.DiplomacyActionsTest.run(context); dev.hoi.client.screen.diplomacy.DiplomacyLeaseInputTest.run(context); dev.hoi.client.screen.diplomacy.DiplomacyLeaseTest.run(context); dev.hoi.client.screen.diplomacy.DiplomacyLeaseTest.runSource(context); dev.hoi.client.screen.diplomacy.DiplomacyVolunteersTest.run(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("decision-targets")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.decision.DecisionScreenTargetTest.run(context, fixtureMenu().hud());
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("decisions")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.decision.DecisionScreenChecks.run(context, fixtureMenu().hud());
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("disconnect")) {
            try (var world = context.worldBuilder().create()) {
                DialogScreenChecks.disconnectThread(context);
                dev.hoi.client.network.NetworkDisconnectTest.run(context, fixtureMenu());
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("missions")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.commander.CommanderBranchMissionSourceTest.run(context);
                dev.hoi.client.screen.commander.CommanderLandMissionSourceTest.run(context);
                dev.hoi.client.map.commander.CommanderHudTest.run(context,fixtureMenu());
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("doctrine-notifications")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.doctrine.DoctrineNotificationTest.run(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("doctrine-icons")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.doctrine.DoctrineScreenSourceTest.runIconReview(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("doctrine")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.doctrine.DoctrineScreenSourceTest.run(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("tactics")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.commander.CommanderPreferredTacticsTest.run(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("officers")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.ScreenOfficerSourceTest.run(context);
                dev.hoi.client.screen.commander.CommanderPreferredTacticsTest.run(context);
                dev.hoi.client.screen.commander.CommanderSourceTest.run(context);
                dev.hoi.client.map.commander.CommanderHudTest.run(context,fixtureMenu());
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("country-names")) { dev.hoi.client.map.MapCountryNameVisibilityTest.run(context); return; }
        if (System.getProperty("hoi.testSuite", "all").equals("chunk-fade")) { dev.hoi.client.map.scene.SceneChunkFadeTest.run(context); return; }
        if (System.getProperty("hoi.testSuite", "all").equals("construction")) {
            try (var world = context.worldBuilder().create()) {
                world.getServer().runCommand("gamemode spectator @a");
                world.getServer().runCommand("tp @a 0.5 100 0.5");
                world.getServer().runCommand("time set day");
                context.waitTicks(5);
                IndustryConstructionTest.run(context,fixtureMenu().hud());
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("naval-production")) {
            try(var world=context.worldBuilder().create()) {
                world.getServer().runCommand("gamemode spectator @a");
                world.getServer().runCommand("tp @a 0.5 150 0.5");
                world.getServer().runCommand("time set day");
                context.waitTicks(5);
                dev.hoi.client.screen.industry.IndustryNavalDeploymentTest.run(context);
            }
            return;
        }
        if (Set.of("all", "raids").contains(System.getProperty("hoi.testSuite", "all"))) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.raid.RaidScreenTest.run(context); }
            if (System.getProperty("hoi.testSuite", "all").equals("raids")) return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("agency")) {
            try (var world = context.worldBuilder().create()) {
                agency(context);
                dev.hoi.client.screen.agency.AgencyScreenSourceTest.run(context);
                dev.hoi.client.screen.agency.AgencyCapitalMapTest.run(context);
                dev.hoi.client.screen.agency.AgencyOperationTest.run(context);
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("agency-operations")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.agency.AgencyOperationTest.run(context);
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("agency-map")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.map.agency.AgencyPresentationTest.run(context, fixtureMenu(), world.getServer()::runCommand);
                dev.hoi.client.research.ResearchSlotTooltipTest.run(context, fixtureResearch());
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("economic-captivity")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.decision.DecisionScreenSourceTest.run(context);
                dev.hoi.client.screen.commander.CommanderCaptivityTest.run(context, fixtureMenu());
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").startsWith("politics-choice")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.politics.PoliticsChoiceTest.run(context);
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("politics")) {
            try (var world = context.worldBuilder().create()) { DiplomacyCountryScreenChecks.authoritativePoliticsTooltips(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("occupation")) {
            try (var world = context.worldBuilder().create()) { dev.hoi.client.screen.occupation.OccupationScreenTest.run(context); dev.hoi.client.screen.ScreenPoliticsTooltipsTest.run(context,fixtureMenu()); dev.hoi.client.map.MapOccupationHudTest.run(context); }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("officer-review")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.screen.ScreenOfficerSourceTest.run(context);
                dev.hoi.client.screen.commander.CommanderPreferredTacticsTest.run(context);
                dev.hoi.client.screen.doctrine.DoctrineScreenSourceTest.run(context);
                dev.hoi.client.map.commander.CommanderHudTest.run(context, fixtureMenu());
                dev.hoi.client.map.MapOccupationHudTest.run(context);
                dev.hoi.client.audio.AudioPlaylistGainTest.run(context);
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("audio")) {
            try (var world = context.worldBuilder().create()) {
                world.getServer().runCommand("gamemode spectator @a");
                dev.hoi.client.audio.AudioMusicRoutingChecks.run(context, world.getServer()::runCommand);
                dev.hoi.client.audio.AudioPlaylistGainTest.run(context);
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("industry")) {
            try (var world = context.worldBuilder().create()) {
                IndustryScreenChecks.run(context, fixtureMenu().hud());
                DialogCompletionChecks.run(context);
                IndustryRegimentChecks.run(context);
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("completion-design-team")) {
            context.waitFor(client -> client.gui.overlay() == null);
            dev.hoi.client.screen.dialog.DialogCompletionDesignTeamTest.run(context);
            context.setScreen(net.minecraft.client.gui.screens.TitleScreen::new);
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("designers")) {
            context.waitFor(client -> client.gui.overlay() == null);
            IndustryScreenChecks.designers(context);
            dev.hoi.client.screen.industry.IndustryDesignerButtonsTest.run(context);
            context.setScreen(net.minecraft.client.gui.screens.TitleScreen::new);
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("unit-hud")) {
            try (var world = context.worldBuilder().create()) {
                dev.hoi.client.map.commander.CommanderUnitHudChecks.run(context);
            }
            return;
        }
        if (System.getProperty("hoi.testSuite", "all").equals("maps")) {
            SceneSurfaceRenderChecks.run(context);
            return;
        }
        AudioChecks.run(context);
        dev.hoi.client.map.MapCountryNameVisibilityTest.run(context);
        dev.hoi.client.map.scene.SceneChunkFadeTest.run(context);
        SceneSurfaceRenderChecks.run(context);
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
            ClientScoreboardChecks.run(context);
            var menu = fixtureMenu();
            dev.hoi.client.audio.AudioMusicRoutingChecks.run(context, world.getServer()::runCommand);
            dev.hoi.client.audio.AudioPlaylistGainTest.run(context);
            dev.hoi.client.audio.AudioUnitVoicesChecks.run(context);
            dev.hoi.client.map.commander.CommanderUnitHudChecks.run(context);
            dev.hoi.client.screen.ScreenSelectionPreviewChecks.run(context, menu);
            context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud())));
            context.setScreen(() -> null); context.waitTicks(3); context.takeScreenshot("hoi-persistent-country-hud");
            context.runOnClient(client -> {
                var shiftF=new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F,0,com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT);
                check(!dev.hoi.client.input.InputMenuShortcut.handle(client,client.getWindow().handle(),com.mojang.blaze3d.platform.InputConstants.PRESS,shiftF), "Shift F is not a menu shortcut");
                check(client.gui.screen() == null, "Shift F does not open politics");
            });
            context.getInput().holdKey(o -> o.keySwapOffhand); context.waitTicks(3);
            context.runOnClient(client -> check(client.gui.screen() instanceof dev.hoi.client.screen.map.MapCursor, "F opens the map cursor"));
            context.waitTicks(8);
            context.runOnClient(client -> check(client.gui.screen() instanceof dev.hoi.client.screen.map.MapCursor, "Held F does not toggle repeatedly"));
            var hudPoint = context.computeOnClient(client -> {
                double scale=client.getWindow().getGuiScale();
                return new double[]{(dev.hoi.client.ui.UiMenuBar.flagWidth(client.gui.screen().width)+72)*scale,7*scale};
            });
            context.getInput().setCursorPos(hudPoint[0],hudPoint[1]);context.waitTicks(4);
            context.takeScreenshot("hoi-map-cursor-stability-tooltip");
            context.getInput().releaseKey(o -> o.keySwapOffhand);
            context.getInput().holdKey(o -> o.keySwapOffhand); context.waitTicks(2);
            context.getInput().releaseKey(o -> o.keySwapOffhand);
            context.runOnClient(client -> check(client.gui.screen() == null, "F closes the map cursor"));
            context.setScreen(() -> new ScreenMenu(menu)); context.waitTicks(2);
            context.setScreen(() -> null); context.waitTicks(2);
            context.runOnClient(client -> {
                check(ClientCampaignHud.visible(), "Closing politics keeps the country HUD");
                ClientCampaignHud.accept(HudPayloads.State.HIDDEN);
                check(!ClientCampaignHud.visible(), "Stop/unassignment immediately hides the country HUD");
            });
            ResearchArtChecks.run(context);
            ResearchTextChecks.run(context);
            convoyGauge(context,menu);
            fuelGauge(context,menu);
            hudHover(context,menu);
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("tp @a 0.5 150 0.5");
            world.getServer().runCommand("time set day");
            context.waitTicks(3);
            IndustryScreenChecks.run(context,menu.hud());
            DialogCompletionChecks.run(context);
            dev.hoi.client.screen.doctrine.DoctrineNotificationTest.run(context);
            dev.hoi.client.screen.industry.IndustryDesignerButtonsTest.run(context);
            IndustryConstructionTest.run(context,menu.hud());
            dev.hoi.client.screen.occupation.OccupationScreenTest.run(context); dev.hoi.client.screen.ScreenPoliticsTooltipsTest.run(context,fixtureMenu()); dev.hoi.client.map.MapOccupationHudTest.run(context);
            DiplomacyCountryScreenChecks.run(context,menu.hud());
            dev.hoi.client.screen.diplomacy.DiplomacyActionsTest.run(context); dev.hoi.client.screen.diplomacy.DiplomacyLeaseInputTest.run(context); dev.hoi.client.screen.diplomacy.DiplomacyLeaseTest.run(context); dev.hoi.client.screen.diplomacy.DiplomacyLeaseTest.runSource(context); dev.hoi.client.screen.diplomacy.DiplomacyVolunteersTest.run(context);
            dev.hoi.client.screen.decision.DecisionScreenChecks.run(context,menu.hud());
            dev.hoi.client.screen.focus.FocusScreenChecks.run(context);
            dev.hoi.client.screen.diplomacy.DiplomacyWorldTensionChecks.run(context,menu.hud());
            DialogScreenChecks.run(context);
            dev.hoi.client.screen.dialog.DialogLeaderEventTest.run(context);
            IndustryRegimentChecks.run(context);
            dev.hoi.client.screen.ScreenOfficerSourceTest.run(context);
                dev.hoi.client.screen.commander.CommanderPreferredTacticsTest.run(context);
                dev.hoi.client.screen.commander.CommanderSourceTest.run(context);
            dev.hoi.client.screen.doctrine.DoctrineScreenSourceTest.run(context);
            dev.hoi.client.screen.commander.CommanderBranchMissionSourceTest.run(context);
                dev.hoi.client.screen.commander.CommanderLandMissionSourceTest.run(context);
            world.getServer().runCommand("gamemode creative @a");context.waitTicks(3);
            dev.hoi.client.map.commander.CommanderHudTest.run(context,menu);
            dev.hoi.client.map.commander.CommanderNavyLayoutTest.run(context,menu);
            world.getServer().runCommand("gamemode spectator @a");context.waitTicks(3);
            context.setScreen(() -> new ScreenMenu(menu)); context.waitTicks(5);
            context.runOnClient(client -> check(((ScreenMenu)client.gui.screen()).panelWidth() == UiPanelLayout.width(MenuTab.POLITICS,client.gui.screen().width), "Country panel follows the original 726-pixel container"));
            context.takeScreenshot("hoi-menu-country");
            check(UiMenuBar.indicators(menu.hud()).stream().limit(13).map(UiMenuBar.UiIndicator::icon).toList().equals(List.of(
                    "political_power", "stability", "war_support", "manpower", "factories", "fuel", "supplies", "convoys", "command_power",
                    "army_experience", "navy_experience", "air_experience", "party_support")), "National indicators follow the requested order");
            check(UiMenuBar.indicators(menu.hud()).stream().anyMatch(i -> i.icon().equals("nuclear") && i.label().equals("핵폭탄")), "Nuclear bombs appear from campaign start without research");
            var armed = new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, .36, true, 0L, fixtureNational());
            check(UiMenuBar.indicators(armed).stream().anyMatch(i -> i.icon().equals("nuclear") && i.value().equals("0")), "Completed nuclear research shows zero inventory");
            check(UiMenuBar.maximumScroll(1024, armed) == 0, "All national and financial indicators fit the normal viewport");
            context.runOnClient(client -> {
                for (var item : UiMenuBar.indicators(armed)) {
                    if (item.icon().equals("political_power") || item.icon().equals("command_power")) {
                        check(item.value().equals(item.icon().equals("political_power") ? "1350" : "150"),
                                "Power indicators truncate fractions without rounding");
                        check(client.font.width(item.value()) <= item.width() - 18,
                                "Power indicator fits without ellipsis: " + item.icon());
                    }
                }
                var convoys = new UiMenuBar.UiIndicator("convoys", "수송", 10000L, UiMenuBar.UiFormat.NUMBER);
                check(convoys.value().equals("10.0K") && client.font.width(convoys.value()) <= convoys.width() - 18,
                        "Ten thousand convoys fit without ellipsis");
                for (double power : new double[]{-2000, 999.99, 1000, 1350, 1999.99, 2000}) {
                    var item = new UiMenuBar.UiIndicator("political_power", "정치력", power, UiMenuBar.UiFormat.POLITICAL_POWER);
                    check(item.value().length() <= 5 && client.font.width(item.value()) <= item.width() - 18,
                            "Political power fits five characters throughout the server range");
                }
            });
            context.runOnClient(client -> check(UiMenuBar.scroll(427, armed, 0, 0, -100) == UiMenuBar.maximumScroll(427, armed), "Compact HUD can reach the final indicators"));
            context.setScreen(() -> new ScreenMenu(new MenuView(menu.country(), menu.countryName(), menu.date(), menu.speed(), menu.pages(), armed)));
            context.waitTicks(2); context.takeScreenshot("hoi-hud-nuclear-researched-zero");
            var stocked = new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, .36, true, 12L, fixtureNational());
            context.setScreen(() -> new ScreenMenu(new MenuView(menu.country(), menu.countryName(), menu.date(), menu.speed(), menu.pages(), stocked)));
            context.waitTicks(2); context.takeScreenshot("hoi-hud-nuclear-stockpile");
            check(UiMenuBar.defconFrame(null) == -1, "Unknown tension must not imply DEFCON 5");
            check(UiMenuBar.defconFrame(0.0) == 0 && UiMenuBar.defconFrame(1.0) == 9,
                    "DEFCON endpoints use the first and final original frames");
            for (int step = 1; step < 10; step++) {
                double boundary = step / 10.0;
                check(UiMenuBar.defconFrame(boundary) == step
                        && UiMenuBar.defconFrame(Math.nextDown(boundary)) == step - 1,
                        "DEFCON changes exactly at " + step * 10 + "%");
            }
            for (Double tension : new Double[]{0.0, .4, .6, 1.0, null}) {
                var hud = new CountryHud(150.0, 119.0, 209.0, 1707.0, 704.314, tension, false, 0L, fixtureNational());
                context.setScreen(() -> new ScreenMenu(new MenuView(menu.country(), menu.countryName(), menu.date(), menu.speed(), menu.pages(), hud)));
                context.waitTicks(2); context.takeScreenshot("hoi-defcon-frame-" + UiMenuBar.defconFrame(tension));
            }
            context.setScreen(() -> new ScreenMenu(menu)); context.waitTicks(2);
            context.takeScreenshot("hoi-menu-politics"); gui2Screenshot(context, "hoi-menu-politics");
            context.runOnClient(c -> check(c.gui.screen().children().stream().noneMatch(child -> child instanceof Button b && b.getMessage().getString().equals("권력의 균형")), "Inactive power balance remains hidden"));
            var balance = new MenuView.Balance("PRC_Liberal_Conservative_power_balance", "민주주의의 바람", "보수파", "개혁파", "bop/prc_conservate_side", "bop/prc_liberal_side", .985,
                    List.of(new MenuView.Entry("보수파의 통제", "-100% ~ -15%", "", -1), new MenuView.Entry("균형", "-15% ~ 15%", "", -.15), new MenuView.Entry("개혁파의 통제", "15% ~ 100%", "", .15)));
            context.setScreen(() -> new ScreenMenu(new MenuView(menu.country(), menu.countryName(), menu.date(), menu.speed(), menu.pages(), menu.hud(), menu.manufacturers(), balance)));
            click(context, "권력의 균형"); context.waitTicks(2); gui2Screenshot(context, "hoi-politics-power-balance");
            context.runOnClient(c -> ((ScreenMenu)c.gui.screen()).update(menu));
            context.waitTicks(2);
            context.runOnClient(c -> check(c.gui.screen().children().stream().noneMatch(child -> child instanceof Button b && b.getMessage().getString().equals("권력의 균형")), "Revoked power balance closes its controls"));
            DiplomacyCountryScreenChecks.adviserFrames(context);
            DiplomacyCountryScreenChecks.authoritativePoliticsTooltips(context);
            DiplomacyCountryScreenChecks.politicsHover(context, menu);
            click(context, "정부 선택 0 · 미지정"); context.waitTicks(2); context.takeScreenshot("hoi-politics-slot-detail");
            context.runOnClient(client -> client.gui.screen().keyPressed(ESCAPE));
            click(context, "연구 & 생산 선택 0 · 적용 중");
            context.runOnClient(client -> check(client.gui.screen().children().stream().filter(c -> c instanceof Button b
                    && b.getMessage().getString().equals("세부 사항")).count() == 1, "Armor manufacturer filter"));
            context.waitTicks(2); context.takeScreenshot("hoi-politics-manufacturers-armor");
            click(context, "군수품");
            context.runOnClient(client -> check(client.gui.screen().children().stream().filter(c -> c instanceof Button b
                    && b.getMessage().getString().equals("세부 사항")).count() == 2, "Shared artillery manufacturer also appears in materiel"));
            context.waitTicks(2); context.takeScreenshot("hoi-politics-manufacturers-materiel");
            context.runOnClient(client -> client.gui.screen().keyPressed(ESCAPE));
            for (var tab : MenuTab.ORDER) if (tab != MenuTab.RESEARCH && tab != MenuTab.INTELLIGENCE) {
                click(context, tab.label() + " 메뉴");
                context.runOnClient(client -> {
                    var screen = (ScreenMenu)client.gui.screen();
                    check(screen.selectedTab() == tab, "Menu tab " + tab);
                    int sourceWidth = switch (tab) { case POLITICS -> 726; case TRADE -> 620; case LOGISTICS -> 720; default -> 550; };
                    check(screen.panelWidth() == UiPanelLayout.width(tab,screen.width), "Original container width for " + tab);
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
                    var screen = (ScreenMenu)client.gui.screen(); checkToolbar(screen);
                    var controls = screen.children().stream().filter(c -> c instanceof UiMenuButton).map(c -> (UiMenuButton)c).toList();
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
            dev.hoi.client.screen.agency.AgencyScreenSourceTest.run(context);
            dev.hoi.client.screen.agency.AgencyCapitalMapTest.run(context);
            dev.hoi.client.screen.agency.AgencyOperationTest.run(context);
            dev.hoi.client.map.agency.AgencyPresentationTest.run(context, fixtureMenu(), world.getServer()::runCommand);
            var requests = new ArrayList<ResearchPayloads.Request>();
            var view = fixtureResearch();
            dev.hoi.client.research.ResearchSlotTooltipTest.run(context, view);
            context.setScreen(() -> fixtureMainMenu(menu, MenuTab.RECRUITMENT, view, requests));
            click(context, "연구 메뉴"); context.waitTicks(3);
            context.takeScreenshot("hoi-research-slots"); gui2Screenshot(context, "hoi-research-slots");
            context.runOnClient(client -> {
                checkToolbar(client.gui.screen());
                var slots = client.gui.screen().children().stream().filter(c -> c instanceof ResearchSlotButton).map(c -> (ResearchSlotButton)c).toList();
                check(slots.size() == 5, "Only the five slots supplied by the server are displayed");
                check(slots.getFirst().getY() > UiMenuBar.height(client.gui.screen().width) + 88, "Spare vertical space enlarges the top research banner");
                check(slots.getLast().getBottom() <= client.gui.screen().height - 8, "Slots fit below persistent menu");
                check(client.gui.screen().children().stream().noneMatch(c -> c instanceof Button b
                        && List.of("<", ">", "1 / 1").contains(b.getMessage().getString())), "No page count or page navigation buttons");
            });
            click(context, "슬롯 1 · 연구 선택"); context.waitTicks(2);
            context.runOnClient(client -> {
                var screen = (ResearchScreen)client.gui.screen();
                checkToolbar(screen);
                check(!screen.allowsMovement(), "Full tree blocks world movement");
                screen.clearFocus(); screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT, 0, 0)); screen.keyPressed(ENTER);
            });
            context.waitTicks(2); context.takeScreenshot("hoi-research-detail");
            context.runOnClient(client -> checkToolbar(client.gui.screen()));
            click(context, "연구");
            context.runOnClient(client -> {
                check(requests.size() == 1, "Exactly one request");
                var request = requests.getFirst();
                check(request.action() == ResearchPayloads.Action.START && request.slot() == 0
                        && request.session().equals(view.session()) && request.revision() == view.revision(), "Authenticated research request");
                check(client.gui.screen().children().stream().anyMatch(c -> c instanceof ResearchSlotButton), "Research start returns to the overview");
                check(client.gui.screen().children().stream().noneMatch(c -> c instanceof Button b
                        && b.getMessage().getString().equals("연구")), "Closed detail cannot send a duplicate request");
            });
            click(context, "슬롯 1 · 연구 선택");
            context.waitTicks(2); context.takeScreenshot("hoi-research-tree");
            click(context, "기갑"); context.waitTicks(2); context.takeScreenshot("hoi-research-armor");
            click(context, "해군 지원 장비"); context.waitTicks(2);
            context.getInput().resizeWindow(854, 480); context.waitTicks(3);
            context.takeScreenshot("hoi-research-compact");
            context.runOnClient(client -> client.gui.screen().keyPressed(ESCAPE));
            context.waitTicks(2); context.takeScreenshot("hoi-research-compact-slots");
            context.runOnClient(client -> {
                checkToolbar(client.gui.screen());
                checkCompactSlots(client.gui.screen(), 5);
            });
            context.getInput().resizeWindow(1600, 1000); context.waitTicks(2);
            context.setScreen(() -> fixtureMainMenu(menu, MenuTab.POLITICS, view, requests));
            click(context, "연구 메뉴");
            click(context, "슬롯 2 · 진행 연구");
            click(context, "연구");
            context.runOnClient(client -> check(requests.getLast().action() == ResearchPayloads.Action.CANCEL
                    && requests.getLast().slot() == 1, "Cancel uses the actual active slot"));
            click(context, "무역 & 경제 메뉴");
            context.runOnClient(client -> {
                check(client.gui.screen() instanceof ScreenMenu screen && screen.selectedTab() == MenuTab.TRADE,
                        "Shared navigation works from research detail");
                check(requests.getLast().action() == ResearchPayloads.Action.CLOSE, "Leaving research closes its private session");
            });
            click(context, "연구 메뉴");
            context.runOnClient(client -> checkToolbar(client.gui.screen()));
            dev.hoi.client.network.NetworkMenuTransitionChecks.run(context, menu);
            dev.hoi.client.network.NetworkDisconnectTest.run(context, menu);
            var six = new ResearchView(view.session(), view.revision(), view.country(), view.countryName(), view.day(), view.date(), view.speed(),
                    java.util.stream.IntStream.range(0, 6).mapToObj(i -> new ResearchView.Slot(i, "", 0)).toList(), List.of(), view.message());
            context.getInput().resizeWindow(854, 480); context.waitTicks(2);
            context.setScreen(() -> new ResearchScreen(six, requests::add)); context.waitTicks(2);
            context.runOnClient(client -> {
                checkToolbar(client.gui.screen());
                checkCompactSlots(client.gui.screen(), 6);
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
    private static void checkCompactSlots(net.minecraft.client.gui.screens.Screen screen, int total) {
        var slots = screen.children().stream().filter(c -> c instanceof ResearchSlotButton).map(c -> (ResearchSlotButton)c).toList();
        check(!slots.isEmpty() && slots.getLast().getBottom() <= screen.height - 8, "Visible compact slots fit the sidebar");
        for (int i = 0; i < total; i++) screen.mouseScrolled(12, screen.height - 20, 0, -1);
        check(screen.children().stream().anyMatch(c -> c instanceof ResearchSlotButton slot
                && slot.getMessage().getString().startsWith("슬롯 " + total + " · ") && slot.getBottom() <= screen.height - 8),
                "Every supplied slot remains reachable by scrolling");
        for (int i = 0; i < total; i++) screen.mouseScrolled(12, screen.height - 20, 0, 1);
    }
    private static void fullTfrCatalog(ClientGameTestContext context) {
        ResearchView view;
        try(var in=ResearchScreenTest.class.getResourceAsStream("/tfr-research-view.json")) {
            view=new ResearchPayloads.Response(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).view();
        } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
        var requests=new ArrayList<ResearchPayloads.Request>();
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
        context.setScreen(()->new ResearchScreen(view,requests::add));context.waitTicks(2);context.takeScreenshot("hoi-tfr-benefits-slots");
        context.runOnClient(c -> check(c.gui.screen().children().stream().noneMatch(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().startsWith("제한된 혜택")),"Limited benefits is a passive indicator"));
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
            for (int i=0;i<60;i++) screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT,0,0));
            screen.keyPressed(ENTER);
        });
        context.waitTicks(2); context.takeScreenshot("hoi-industry-rearranged-compact-detail"); click(context,"×");
        context.getInput().resizeWindow(1600,1000); context.waitTicks(3); click(context,"공학"); context.waitTicks(2);
        context.runOnClient(client->{var screen=(ResearchScreen)client.gui.screen();screen.clearFocus();screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT,0,0));screen.keyPressed(ENTER);});
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
        context.runOnClient(client -> check(UiMenuBar.flagTexture("").isEmpty() && UiMenuBar.flagTexture("KOR").equals("country/kor/flag"), "Loading flag stays blank until the country is known"));
    }
    private static void checkResearchLabels(ResearchLayout tree, java.util.function.ToIntFunction<String> textWidth, int lineHeight) {
        var labels = ResearchTreeLabels.create(tree, textWidth, lineHeight);
        check(labels.size() == ResearchSourceLayout.yearLabels(tree).size() + ResearchSourceLayout.headings(tree).size(), "Every year and equipment heading remains visible");
        for (var heading : ResearchSourceLayout.headings(tree)) {
            var label = labels.stream().filter(l -> !l.year() && l.text().equals(heading.text())).findFirst().orElseThrow();
            check(label.y() <= heading.y(), "Equipment heading stays above its research row: " + label.text());
            check(heading.y() - label.y() <= lineHeight * 3, "Equipment heading stays near its research row: " + heading + " -> " + label);
        }
        if (ResearchLayout.category(tree.nodes().getFirst().tech()).equals("SUPPORT")) {
            var label = labels.stream().filter(l -> l.text().equals("다목적 / 공격 헬리콥터")).findFirst().orElseThrow();
            var card = tree.nodes().stream().filter(n -> n.tech().source().id().equals("utility_helicopter_1")).findFirst().orElseThrow();
            check(Math.abs(label.x() + label.width() / 2.0 - card.x() - card.width() / 2.0) <= .5,
                    "Helicopter heading is centered on the UH-60M card");
        }
        if (ResearchLayout.category(tree.nodes().getFirst().tech()).equals("NAVY")) {
            var targets = java.util.Map.of("초계함", "early_ship_hull_light", "구축함", "early_ship_hull_cruiser", "방어력", "basic_cruiser_armor_scheme",
                    "미사일 순양함", "early_ship_hull_heavy", "항공모함", "early_ship_hull_carrier", "잠수함", "early_ship_hull_submarine");
            for (var target : targets.entrySet()) {
                var label = labels.stream().filter(l -> l.text().equals(target.getKey())).findFirst().orElseThrow();
                var card = tree.nodes().stream().filter(n -> n.tech().source().id().equals(target.getValue())).findFirst().orElseThrow();
                check(label.y() + label.height() + 2 <= card.y(), "Naval heading is above its own equipment card: " + label.text());
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
                try (var input = ResearchScreenTest.class.getResourceAsStream("/atlas-client-scene.json")) {
                    fixture = com.google.gson.JsonParser.parseString(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                }
                var target = client.getResourcePackDirectory().resolve("hoi-test-carriers.zip");
                try (var output = new java.util.zip.ZipOutputStream(Files.newOutputStream(target))) {
                    output.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));
                    output.write("{\"pack\":{\"description\":\"HOI test carriers\",\"min_format\":[97,1],\"max_format\":[97,1]}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
            for (var item : UiMenuBar.indicators(CountryHud.UNKNOWN))
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
        var creationRequests = new ArrayList<AgencyPayloads.Request>();
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
        var commonUpgrades = AgencyScreen.UPGRADE_ORDER.stream().filter(id -> !id.equals("hidden_front")).toList();
        for (int i = 0; i < commonUpgrades.size(); i++) {
            String upgrade = commonUpgrades.get(i);
            int level = i % 3 == 0 ? maxima[i] : i % 3 == 1 ? maxima[i] / 2 : 0;
            items.add(new AgencyView.Item("upgrade:" + upgrade, "upgrades", upgradeNames[i], level + " / " + maxima[i], "민간공장 5개 · 30일", "agency/" + upgrade, "개선", i % 3 == 1, level / (double)maxima[i], List.of()));
        }
        dev.hoi.client.screen.agency.AgencyScreenChecks.run(context, items);
        items.add(new AgencyView.Item("decrypt:PRK", "cryptology", "북한 암호", "해독 중", "해독 진행: 1200 / 12000", "agency/cryptology", "일시 정지", true, .1, List.of()));
        items.add(new AgencyView.Item("operation:CAPTURE_CIPHER", "operations", "암호 탈취", "60일", "정보망 50과 대기 요원 2명이 필요합니다.\n민간공장 3개가 사용됩니다.", "agency/capture_cipher", "작전 준비", true, -1,
                List.of(new AgencyView.Parameter("대상 국가", List.of(new AgencyView.Choice("PRK", "북한"), new AgencyView.Choice("JAP", "일본"))))));
        items.add(new AgencyView.Item("hire:candidate/0", "agents", "검증 후보", "", "", "", "고용", true, -1, List.of()));
        var candidate = new AgencyView.Candidate("hire:candidate/0", "검증 후보", "", 1, "신병", "모집할 요원을 선택", List.of());
        var fixture = new AgencyView("agency-fixture", 1, "KOR", "국가정보원", "무선 감청 · 21일 남음", items, "검증용 기관 데이터", List.of(candidate));
        var requests = new ArrayList<AgencyPayloads.Request>();
        context.setScreen(() -> new AgencyScreen(null, fixture.session(), fixture, requests::add)); context.waitTicks(2);
        context.takeScreenshot("hoi-agency-operatives");
        context.runOnClient(client -> {
            for (var label : List.of("모집된 요원", "적에게 포획된 요원", "적에게 사살당한 정보원")) {
                var indicator = client.gui.screen().children().stream().filter(c -> c instanceof Button b && b.getMessage().getString().equals(label)).map(c -> (Button)c).findFirst().orElseThrow();
                check(!indicator.active, "Operative indicators keep tooltips without accepting clicks");
            }
        });
        click(context, "정보원 모집"); context.waitTicks(2); click(context, candidate.name());
        context.runOnClient(client -> {
            check(requests.size() == 1 && requests.getFirst().kind() == AgencyPayloads.Kind.CALL && requests.getFirst().item().equals(candidate.item()) && requests.getFirst().revision() == 1, "Agency uses issued candidate, session and revision");
            check(client.gui.screen().children().stream().anyMatch(c -> c instanceof Button b && !b.active && b.getMessage().getString().equals("정보원 모집")), "Pending agency action disabled");
            ((AgencyScreen)client.gui.screen()).update(new AgencyView(fixture.session(), 2, "KOR", fixture.name(), fixture.status(), items, "고용 완료"));
        });
        click(context, "정보공동체"); context.waitTicks(2); context.takeScreenshot("hoi-agency-upgrades"); gui2Screenshot(context, "hoi-agency-upgrades");
        context.runOnClient(client -> check(client.gui.screen().children().stream().anyMatch(c -> c instanceof Button b && b.getMessage().getString().equals("암호학") && !b.active), "Upgrade overlay blocks background tabs"));
        click(context, "개선 창 닫기"); click(context, "암호학"); context.waitTicks(2); context.takeScreenshot("hoi-agency-cryptology"); gui2Screenshot(context, "hoi-agency-cryptology");
        context.getInput().resizeWindow(1280, 720); context.runOnClient(client -> client.options.guiScale().set(2));
        context.waitTicks(2); context.takeScreenshot("hoi-agency-cryptology-1280-gui2");
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(2);
        click(context, "작전"); click(context, "암호 탈취"); context.waitTicks(2); context.takeScreenshot("hoi-agency-operation");
        click(context, "대상 국가: 북한"); context.waitTicks(1); click(context, "일본");
        context.waitTicks(1); context.takeScreenshot("hoi-agency-operation-target-japan"); click(context, "준비하기");
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
        context.runOnClient(client -> check(!client.options.keyUp.isDown() && client.gui.screen() instanceof dev.hoi.client.screen.focus.FocusScreen, "Full focus tree releases sidebar movement"));
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
        var tabs = screen.children().stream().filter(c -> c instanceof UiMenuBar.UiTabButton).map(c -> (UiMenuBar.UiTabButton)c).toList();
        check(tabs.size() == MenuTab.ORDER.size() && tabs.stream().allMatch(b -> b.active && b.visible
                && (b.getY() == 3 || b.getY() == 17) && b.getBottom() < UiMenuBar.height(screen.width)), "Common navigation stays usable below the resource strip");
        check(tabs.getLast().getRight() < UiMenuBar.dockWidth(screen.width)
                && UiMenuBar.dockWidth(screen.width) <= screen.width, "Compact original-art toolbar fits the viewport");
        check(UiMenuBar.tensionX(screen.width) + 30 == screen.width - 4
                && UiMenuBar.tensionX(screen.width) > UiMenuBar.dockWidth(screen.width), "Flag-sized tension display stays at the far right");
    }
    private static ScreenMenu fixtureMainMenu(MenuView menu, MenuTab selected, ResearchView research, List<ResearchPayloads.Request> requests) {
        return new ScreenMenu(menu, selected, () -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                new ResearchScreen(research, requests::add, tab -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                        fixtureMainMenu(menu, tab, research, requests)))));
    }
    private static MenuView fixtureMenu() {
        var pages = MenuTab.ORDER.stream().map(tab -> new MenuView.Page(tab, tab == MenuTab.POLITICS ? DiplomacyCountryScreenChecks.politics()
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
    private static void convoyGauge(ClientGameTestContext context, MenuView menu) {
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.getInput().setCursorPos(2400,1300);
        for (double ratio : new double[]{1,.5,0}) {
            var n=fixtureNational();
            var national=new CountryHud.NationalIndicators(n.politicalPower(),n.stability(),n.warSupport(),n.factories(),n.energyRatio(),n.fuel(),n.supplies(),n.supplyEfficiency(),n.convoys(),ratio,n.commandPower(),n.rulingPartySupport(),n.manpower(),n.fuelRatio());
            var hud=new CountryHud(150.0,119.0,209.0,1707.0,704.314,.36,false,0L,national);
            context.setScreen(() -> new ScreenMenu(new MenuView(menu.country(),menu.countryName(),menu.date(),menu.speed(),menu.pages(),hud)));
            context.waitTicks(3);
            try {
                var image=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-convoy-efficiency-"+(int)(ratio*100)+"-gui2").toFile());
                var items=UiMenuBar.indicators(hud);
                int left=UiMenuBar.flagWidth(1280)+10+items.stream().takeWhile(i->!i.icon().equals("convoys")).mapToInt(UiMenuBar.UiIndicator::width).sum()+15;
                int width=items.stream().filter(i->i.icon().equals("convoys")).findFirst().orElseThrow().width()-18;
                int filled=0;
                for(int x=left*2;x<(left+width)*2;x++)if((image.getRGB(x,23)&0xFFFFFF)==0x6D9C61)filled++;
                check(filled==(int)Math.round(width*ratio)*2,"Convoy gauge pixels follow transport efficiency without changing colour at "+ratio);
            } catch(java.io.IOException failure) { throw new AssertionError(failure); }
        }
        context.getInput().resizeWindow(1600,1000);
        context.waitTicks(3);
    }
    private static void fuelGauge(ClientGameTestContext context, MenuView menu) {
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.getInput().setCursorPos(2400,1300);
        for (double ratio : new double[]{1,.5,0}) {
            var n=fixtureNational();
            var national=new CountryHud.NationalIndicators(n.politicalPower(),n.stability(),n.warSupport(),n.factories(),n.energyRatio(),n.fuel(),n.supplies(),n.supplyEfficiency(),n.convoys(),n.transportEfficiency(),n.commandPower(),n.rulingPartySupport(),n.manpower(),ratio,ratio>.5?100.0:-100.0,5.0);
            var hud=new CountryHud(150.0,119.0,209.0,1707.0,704.314,.36,false,0L,national);
            context.setScreen(() -> new ScreenMenu(new MenuView(menu.country(),menu.countryName(),menu.date(),menu.speed(),menu.pages(),hud)));
            context.getInput().setCursorPos(2400,1300);context.waitTicks(3);
            check(UiMenuBar.indicators(hud).stream().filter(i->i.icon().equals("fuel")).findFirst().orElseThrow().value().equals(ratio>=1?"가득 참":"5일"),"Full fuel overrides day estimates with the TFR full label");
            try {
                var image=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-fuel-stock-"+(int)(ratio*100)+"-gui2").toFile());
                var items=UiMenuBar.indicators(hud);
                int left=UiMenuBar.flagWidth(1280)+10+items.stream().takeWhile(i->!i.icon().equals("fuel")).mapToInt(UiMenuBar.UiIndicator::width).sum()+15;
                int width=items.stream().filter(i->i.icon().equals("fuel")).findFirst().orElseThrow().width()-18;
                int filled=0;
                for(int x=left*2;x<(left+width)*2;x++)if((image.getRGB(x,23)&0xFFFFFF)==0x6D9C61)filled++;
                check(filled==(int)Math.round(width*ratio)*2,"Fuel gauge pixels follow authoritative stock capacity at "+ratio);
                int tint=ratio>.5?0x009F03:0xFF3232,colored=0;
                for(int y=2;y<20;y++)for(int x=left*2;x<(left+width)*2;x++)if((image.getRGB(x,y)&0xFFFFFF)==tint)colored++;
                check(colored>0,"Fuel day text uses the exact original TFR colour");
            } catch(java.io.IOException failure) { throw new AssertionError(failure); }
        }
        context.getInput().resizeWindow(1600,1000);
        context.waitTicks(3);
    }
    private static void hudHover(ClientGameTestContext context, MenuView menu) {
        context.runOnClient(client->{
            var rows=List.of(new dev.hoi.protocol.hud.HudDetail.Row("증감", "+0.125", dev.hoi.protocol.hud.HudDetail.Tone.GOOD),new dev.hoi.protocol.hud.HudDetail.Row("---", "", dev.hoi.protocol.hud.HudDetail.Tone.NORMAL),new dev.hoi.protocol.hud.HudDetail.Row("영점", "0.00", dev.hoi.protocol.hud.HudDetail.Tone.NORMAL),new dev.hoi.protocol.hud.HudDetail.Row("---", "", dev.hoi.protocol.hud.HudDetail.Tone.NORMAL));
            var source=new dev.hoi.protocol.hud.HudDetail(List.of(),"§a+0.125\n§a---\n0.00");
            for(var detail:List.of(new dev.hoi.protocol.hud.HudDetail(rows),source)) {
                var hud=new CountryHud(0.0,0.0,0.0,null,null,null,false,0L,null,Map.of("political_power",detail));
                var indicator=new UiMenuBar.UiIndicator("political_power","정치력",0.0,UiMenuBar.UiFormat.NUMBER);
                var body=dev.hoi.client.ui.UiHudTooltip.body(client.font,1280,hud,indicator);
                var text=new StringBuilder();
                for(var line:body)line.accept((index,style,character)->{
                    text.appendCodePoint(character);
                    if(detail==source)check(style.getColor()!=null&&style.getColor().getValue()==dev.hoi.client.ui.UiTooltips.GOOD,"Source color survives the separator paragraph");
                    return true;
                });
                check(text.toString().contains("+0.125")&&text.toString().contains("0.00")&&!text.toString().contains("---"),"HUD retains issued precision, sign and zero rows");
                check(body.stream().anyMatch(line->line==net.minecraft.util.FormattedCharSequence.EMPTY),"HUD separator becomes a blank line");
                check(body.getFirst()!=net.minecraft.util.FormattedCharSequence.EMPTY&&body.getLast()!=net.minecraft.util.FormattedCharSequence.EMPTY,"HUD trims outer blank lines");
            }
        });
        var issued=new MenuView(menu.country(),menu.countryName(),menu.date(),menu.speed(),menu.pages(),dev.hoi.client.screen.industry.IndustryScreenChecks.tankFixture("tank-empty-fixture.json").hud());
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        context.setScreen(()->new ScreenMenu(issued));context.waitTicks(3);
        context.runOnClient(client->{
            var indicator=UiMenuBar.indicators(issued.hud()).stream().filter(i->i.icon().equals("fuel")).findFirst().orElseThrow();
            var body=dev.hoi.client.ui.UiHudTooltip.body(client.font,1280,issued.hud(),indicator);
            var rows=new ArrayList<String>();
            for(var line:body) {
                var text=new StringBuilder();
                line.accept((index,style,character)->{text.appendCodePoint(character);return true;});
                rows.add(text.toString());
            }
            check(rows.getFirst().startsWith("현재 연료:"),"Fuel starts with content after trimming the outer blank line");
            check(rows.stream().filter(String::isEmpty).count()==7,"Fuel preserves all seven internal paragraph breaks");
        });
        try (var stream = ResearchScreenTest.class.getResourceAsStream("/hud-source-fixture.json")) {
            if (stream == null) throw new AssertionError("Missing source HUD fixture");
            var original = new com.google.gson.Gson().fromJson(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), CountryHud.class);
            context.runOnClient(client -> {
                var debt = dev.hoi.client.ui.UiKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal("GDP 대비 부채: +4.52%"), false);
                check(debt.getString().equals("GDP 대비 부채: +4.52%"), "Original debt row never inherits a keyword icon");
                check(dev.hoi.client.ui.UiKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal("GDP 대비 부채: +4.52%")).getString().codePoints().anyMatch(c -> c >= 0xe000 && c <= 0xf8ff), "Other tooltips keep their existing debt keyword icon");
                var inflation = dev.hoi.client.ui.UiKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal("£inflation 인플레이션: -0.95%"), false);
                check(!inflation.getString().contains("£") && inflation.getString().codePoints().anyMatch(c -> c >= 0xe000 && c <= 0xf8ff), "Original inflation token renders its actual icon");
            });
            var sourceMenu = new MenuView(menu.country(),menu.countryName(),menu.date(),menu.speed(),menu.pages(),original);
            context.setScreen(() -> new ScreenMenu(sourceMenu)); context.waitTicks(3);
            for (String key : List.of("political_power","stability","war_support","manpower","factories","fuel","supplies","convoys","command_power","army_experience","navy_experience","air_experience","party_support","nuclear","gdp","debt")) {
                context.runOnClient(client -> {
                    var indicator = UiMenuBar.indicators(original).stream().filter(i -> i.icon().equals(key)).findFirst().orElseThrow();
                    var body = dev.hoi.client.ui.UiHudTooltip.body(client.font,1280,original,indicator);
                    if (key.equals("gdp")) check(body.size() * (client.font.lineHeight + 1) + 12 <= 540,"Complete GDP tooltip fits GUI2 at 1920x1080 without scrolling");
                    long expected = original.details().get(key).sourceText().strip().lines().filter(String::isEmpty).count();
                    check(body.stream().filter(line -> line == net.minecraft.util.FormattedCharSequence.EMPTY).count() == expected,"Original HUD paragraph breaks survive rendering: " + key);
                    var text = new StringBuilder();
                    boolean[] white = {false}, gold = {false}, icon = {false};
                    for (var line : body) line.accept((index,style,character) -> {
                        text.appendCodePoint(character);
                        int color = style.getColor() == null ? 0 : style.getColor().getValue();
                        white[0] |= color == 0xFFFFFF; gold[0] |= color == dev.hoi.client.ui.UiTooltips.GOLD;
                        icon[0] |= character >= 0xe000 && character <= 0xf8ff;
                        return true;
                    });
                    check(!text.toString().contains("§")&&!text.toString().contains("£")&&!text.toString().contains("---"),"Source formatting is rendered, never printed: " + key);
                    check(white[0] && (gold[0] || key.equals("party_support")),"Source descriptions stay white with gold highlights: " + key);
                    if (original.details().get(key).sourceText().contains("£pol_idea")) check(icon[0],"Yellow political icon is rendered: " + key);
                });
                int point=UiMenuBar.flagWidth(1280)+10+UiMenuBar.indicators(original).stream().takeWhile(i->!i.icon().equals(key)).mapToInt(UiMenuBar.UiIndicator::width).sum()+20;
                if (List.of("gdp", "debt").contains(key)) point += Math.max(0, UiMenuBar.statsRight(1280) - (UiMenuBar.flagWidth(1280) + 10) - UiMenuBar.indicators(original).stream().mapToInt(UiMenuBar.UiIndicator::width).sum() - 3);
                context.getInput().setCursorPos(point*2,12); context.waitTicks(3);
                context.takeScreenshot("hoi-source-"+key+"-tooltip-gui2");
            }
            context.setScreen(() -> new ScreenMenu(issued)); context.waitTicks(3);
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
        int fuel=(UiMenuBar.flagWidth(1280)+10)+UiMenuBar.indicators(issued.hud()).stream().takeWhile(i->!i.icon().equals("fuel")).mapToInt(UiMenuBar.UiIndicator::width).sum()+20;
        context.getInput().setCursorPos(fuel*2,12);context.waitTicks(5);
        context.takeScreenshot("hoi-fuel-tooltip-hover-gui2");
        context.runOnClient(client->{
            var indicator=UiMenuBar.indicators(issued.hud()).stream().filter(i->i.icon().equals("supplies")).findFirst().orElseThrow();
            var body=dev.hoi.client.ui.UiHudTooltip.body(client.font,1280,issued.hud(),indicator);
            check(body.stream().filter(line->line==net.minecraft.util.FormattedCharSequence.EMPTY).count()==4,"Logistics renders all four paragraph breaks");
            body.getLast().accept((index,style,character)->{
                check(style.getColor()!=null&&style.getColor().getValue()==0xFFFFFF,"Logistics description is white like the source tooltip");
                return true;
            });
        });
        int supply=(UiMenuBar.flagWidth(1280)+10)+UiMenuBar.indicators(issued.hud()).stream().takeWhile(i->!i.icon().equals("supplies")).mapToInt(UiMenuBar.UiIndicator::width).sum()+20;
        context.getInput().setCursorPos(supply*2,12);context.waitTicks(5);
        context.takeScreenshot("hoi-supply-tooltip-spacing-gui2");
        context.getInput().setCursorPos((UiMenuBar.tensionX(1280)+15)*2,20);context.waitTicks(5);
        context.takeScreenshot("hoi-tension-tooltip-hover-gui2");
        context.getInput().setCursorPos(2200,1100);context.waitTicks(5);
        context.takeScreenshot("hoi-hud-tooltip-pointer-outside-gui2");
    }
    private static CountryHud.NationalIndicators fixtureNational() {
        return new CountryHud.NationalIndicators(1350.75, .62, .48, 45L, .85, 894_920.0, 580.0, .72, 10000L, .95, 150.75, .36, 280_900L, .27);
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
