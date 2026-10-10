package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import java.nio.file.*;
import java.util.*;

public final class ScreenDoctrineSourceTest {
    private ScreenDoctrineSourceTest() {}
    private static ProtocolDoctrineView fixture(String name) throws Exception {
        String directory = System.getenv("HOI_DOCTRINE_PREVIEW");
        String json;
        if (directory != null) json = Files.readString(Path.of(directory, name + ".json"));
        else try (var input = ScreenDoctrineSourceTest.class.getResourceAsStream("/doctrine/" + name + ".json")) {
            if (input == null) throw new AssertionError("Missing server doctrine fixture " + name);
            json = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return new ProtocolDoctrinePayloads.Response(json).view();
    }
    private static int[] position(ClientGameTestContext context, String name) {
        return context.computeOnClient(client -> {
            var button = client.gui.screen().children().stream().filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                .filter(c -> c.getMessage().getString().equals(name)).findFirst().orElseThrow();
            return new int[]{button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2};
        });
    }
    private static void click(ClientGameTestContext context, int[] position) {
        context.runOnClient(client -> {
            var click = new net.minecraft.client.input.MouseButtonEvent(position[0], position[1],
                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
            client.gui.screen().mouseClicked(click, false);
        });
    }
    private static void branchSelection(ClientGameTestContext context, String folder, String trackId, String selectedId) throws Exception {
        var before = fixture("doctrine-" + folder + "-ready"); var after = fixture("doctrine-" + folder + "-selected");
        if (!before.folder().equals(folder) || !before.session().equals(after.session()) || !before.experience().equals("300") || !after.experience().equals("200"))
            throw new AssertionError("Server-issued branch XP300 to200 pair required");
        var track = before.tracks().stream().filter(t -> t.id().equals(trackId)).findFirst().orElseThrow();
        var choice = track.candidates().stream().filter(c -> c.id().equals(selectedId)).findFirst().orElseThrow();
        if (!choice.enabled() || !choice.cost().equals("100")) throw new AssertionError("Server-owned branch quote required");
        var sent = new ArrayList<ProtocolDoctrinePayloads.Request>();
        context.setScreen(() -> new ScreenDoctrine(null, folder, before.session(), before, sent::add));
        click(context, position(context, track.name())); context.waitTicks(2);
        var target = position(context, choice.name());
        context.getInput().setCursorPos(target[0] * 2, target[1] * 2); context.waitTicks(3);
        context.takeScreenshot("hoi-doctrine-" + folder + "-candidate-gui2");
        click(context, target); click(context, target);
        context.runOnClient(client -> {
            if (sent.size() != 1 || !sent.getFirst().equals(new ProtocolDoctrinePayloads.Request(ProtocolDoctrinePayloads.Kind.SELECT_SUBDOCTRINE,
                    before.session(), before.revision(), folder, trackId, selectedId))) throw new AssertionError("Branch double click must issue exactly one selection");
            ((ScreenDoctrine)client.gui.screen()).update(before);
        });
        click(context, target);
        context.runOnClient(client -> {
            if (sent.size() != 1) throw new AssertionError("Stale branch response must not unlock selection");
            ((ScreenDoctrine)client.gui.screen()).update(after);
        });
        context.getInput().setCursorPos(2450, 1300); context.waitTicks(3);
        context.takeScreenshot("hoi-doctrine-" + folder + "-selected-gui2");
        context.setScreen(() -> null);
    }
    private static void nativeProgress(ClientGameTestContext context) throws Exception {
        var progress=fixture("doctrine-land-progress");var track=progress.tracks().stream().filter(t->t.id().equals("support_units")).findFirst().orElseThrow();
        if(!ScreenDoctrine.rewardThreshold(track,0).equals("150")||!ScreenDoctrine.rewardThreshold(track,1).equals("300")
                ||ScreenDoctrine.rewardProgress(track,0)!=1||ScreenDoctrine.rewardProgress(track,1)!=.5f||ScreenDoctrine.rewardProgress(track,2)!=0)
            throw new AssertionError("Cumulative source thresholds and partial authoritative mastery must share the native progress display");
        var actions=new ArrayList<ProtocolDoctrinePayloads.Request>();
        context.setScreen(()->new ScreenDoctrine(null,"land",progress.session(),progress,actions::add));
        context.runOnClient(client->net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterExtract(client.gui.screen()).register((screen,g,mx,my,delta)->{
            if(!g.containsPointInScissor(0,0)||!g.containsPointInScissor(screen.width-1,screen.height-1))
                throw new AssertionError("Partial mastery fill must restore the extractor scissor stack before tooltips and later UI");
        }));context.waitTicks(3);
        context.getInput().setCursorPos(2450,1300);context.waitTicks(2);context.takeScreenshot("hoi-doctrine-source-partial-mastery-gui2");
        var counter=position(context,track.name()+" mastery");context.getInput().setCursorPos(counter[0]*2,counter[1]*2);context.waitTicks(3);
        context.takeScreenshot("hoi-doctrine-source-mastery-tooltip-gui2");
        click(context,counter);context.runOnClient(client->{
            if(!actions.isEmpty())throw new AssertionError("Mastery counter inspection must not send a progression action");
            for(var child:client.gui.screen().children())if(child instanceof dev.hoi.client.ui.UiHoverRegion region)
                if(region.getY()<0||region.getBottom()>client.gui.screen().height)throw new AssertionError("Reward hover region must follow visible native bounds");
        });
        context.setScreen(()->null);
    }
    private static void confirmedGrandSwitch(ClientGameTestContext context) throws Exception {
        var before=fixture("doctrine-land-before-grand-switch");var after=fixture("doctrine-land-after-grand-switch");
        if(!before.grand().equals("nato_western_doctrine")||!after.grand().equals("csto_eastern_doctrine")
                ||!before.session().equals(after.session())||after.progressionRevision()!=before.progressionRevision()+1)
            throw new AssertionError("Current authoritative successful grand-switch pair required");
        for(String id:List.of("infantry","support_units")) {
            var prior=before.tracks().stream().filter(t->t.id().equals(id)).findFirst().orElseThrow();
            var reset=after.tracks().stream().filter(t->t.id().equals(id)).findFirst().orElseThrow();
            if(!reset.mastery().equals("0")||!prior.bank().equals(reset.bank())||!prior.selected().equals(reset.selected())
                    ||reset.rewards().stream().anyMatch(ProtocolDoctrineView.Reward::unlocked))throw new AssertionError("Confirmed progress reset must preserve selected subdoctrine and bank in the private response");
            for(int i=0;i<reset.rewards().size();i++)if(ScreenDoctrine.rewardProgress(reset,i)!=0)throw new AssertionError("Reset mastery must render an empty cumulative stage fill");
        }
        var actions=new ArrayList<ProtocolDoctrinePayloads.Request>();context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));context.setScreen(()->new ScreenDoctrine(null,"land",before.session(),before,actions::add));
        context.getInput().setCursorPos(2450,1300);context.waitTicks(3);context.takeScreenshot("hoi-doctrine-before-grand-switch-gui2");
        context.runOnClient(client->{var screen=(ScreenDoctrine)client.gui.screen();screen.update(after);screen.update(before);});
        context.waitTicks(3);context.takeScreenshot("hoi-doctrine-after-grand-switch-gui2");
        context.runOnClient(client->{if(!actions.isEmpty())throw new AssertionError("Authoritative grand-switch rendering cannot issue another selection or grant progress");});
        context.setScreen(()->null);
    }
    private static void centeredLayoutStress(ClientGameTestContext context) throws Exception {
        var source=fixture("doctrine-land-empty");var tracks=new ArrayList<ProtocolDoctrineView.Track>();
        var amounts=List.of("0","8.12","999999.125");var uiText=new HashMap<>(source.uiText());
        for(int i=0;i<source.tracks().size();i++) {
            var track=source.tracks().get(i);
            tracks.add(new ProtocolDoctrineView.Track(track.id(),track.name(),track.background(),track.icon(),track.decoration(),
                    track.selected(),track.nameplate(),track.mastery(),track.dailyMastery(),amounts.get(i%amounts.size()),track.rewards(),track.candidates()));
            uiText.put("mastery/"+i,"통달치: "+amounts.get(i%amounts.size()));
        }
        var stress=new ProtocolDoctrineView(source.session(),source.revision(),source.country(),source.progressionRevision(),source.folder(),source.branch(),
                source.experience(),source.grand(),source.folders(),source.grands(),tracks,source.message(),uiText);
        var actions=new ArrayList<ProtocolDoctrinePayloads.Request>();
        for(int[] size:new int[][]{{1280,720},{2560,1440}}) {
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
            context.setScreen(()->new ScreenDoctrine(null,stress.folder(),stress.session(),stress,actions::add));
            context.getInput().setCursorPos(size[0]-4,size[1]-4);context.waitTicks(3);
            context.runOnClient(client->{
                var screen=(ScreenDoctrine)client.gui.screen();
                if(client.options.guiScale().get()!=2||screen.width!=size[0]/2||screen.height!=size[1]/2)
                    throw new AssertionError("Centered doctrine layout must retain actual GUI2 dimensions");
                if(!actions.isEmpty())throw new AssertionError("Synthetic layout stress cannot issue progression requests");
            });
            context.takeScreenshot("hoi-doctrine-centered-layout-stress-"+size[0]+"x"+size[1]+"-gui2");
            if(size[0]==1280) {
                var counter=position(context,tracks.get(2).name()+" mastery");
                context.getInput().setCursorPos(counter[0]*2,counter[1]*2);context.waitTicks(3);
                context.runOnClient(client->{
                    var screen=(ScreenDoctrine)client.gui.screen();
                    if(screen.tooltipBounds()==null||!screen.hoveredDetail().equals("통달치: 999999.125"))
                        throw new AssertionError("Ellipsized mastery must retain the complete authoritative display string in its hovered tooltip");
                    if(!actions.isEmpty())throw new AssertionError("Reading a complete mastery amount cannot issue progression requests");
                });
                context.takeScreenshot("hoi-doctrine-centered-layout-stress-full-amount-tooltip-1280x720-gui2");
            }
            context.setScreen(()->null);
        }
    }
    public static void run(ClientGameTestContext context) {
        try {
            for (String selectedFolder : List.of("land", "naval", "air", "special_forces", "land", "air", "naval"))
                for (String tab : List.of("land", "naval", "air", "special_forces"))
                    if (ScreenDoctrine.tabFrame(tab, selectedFolder) != (tab.equals(selectedFolder) ? 0 : 1))
                        throw new AssertionError("Selected doctrine tab must use the original front frame");
            int[][] originalRewardFrames = {{0, 1}, {2, 3}, {4, 5}, {6, 7}};
            for (int branch = 0; branch < originalRewardFrames.length; branch++)
                if (ScreenDoctrine.rewardFrame(branch, true) != originalRewardFrames[branch][0]
                        || ScreenDoctrine.rewardFrame(branch, false) != originalRewardFrames[branch][1])
                    throw new AssertionError("Original reward backgrounds interleave each branch unlocked and locked art");
            var empty = fixture("doctrine-land-empty"); var selected = fixture("doctrine-land-selected"); var air = fixture("doctrine-air-empty");
            if (!empty.session().equals(selected.session()) || !empty.experience().equals("300") || !selected.experience().equals("200"))
                throw new AssertionError("Original server-issued XP300→200 selection pair required");
            var infantry = empty.tracks().stream().filter(t -> t.id().equals("infantry")).findFirst().orElseThrow();
            var choice = infantry.candidates().stream().filter(c -> c.id().equals("motorized_infantry")).findFirst().orElseThrow();
            if (!choice.enabled() || !choice.detail().contains("속도: §G+5%§!")) throw new AssertionError("Source speed ratio must survive the server wire");
            var sent = new ArrayList<ProtocolDoctrinePayloads.Request>();
            context.getInput().resizeWindow(2560, 1440); context.runOnClient(client -> client.options.guiScale().set(2));
            context.setScreen(() -> new ScreenDoctrine(null, "land", empty.session(), empty, sent::add));
            context.runOnClient(client -> {
                for (String asset : List.of("gfx_track_frame/frame_0", "gfx_grand_doctrine_frame/frame_0", "gfx_subdoctrine_button_and_text_frame/frame_0"))
                    if (client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/doctrine/" + asset + ".png")).isEmpty())
                        throw new AssertionError("Missing original doctrine asset " + asset);
            });
            context.getInput().setCursorPos(2450, 1300); context.waitTicks(3); context.takeScreenshot("hoi-doctrine-land-empty-gui2");
            click(context, position(context, infantry.name())); context.waitTicks(2);
            var candidatePosition = position(context, choice.name());
            context.getInput().setCursorPos(candidatePosition[0] * 2, candidatePosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-infantry-candidate-tooltip-gui2");
            click(context, position(context, "고급 세부 사항"));
            context.runOnClient(client -> { if (!sent.isEmpty()) throw new AssertionError("Advanced details must not issue an XP selection"); });
            context.getInput().setCursorPos(candidatePosition[0] * 2, candidatePosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-infantry-advanced-tooltip-gui2");
            click(context, position(context, "고급 세부 사항"));
            click(context, candidatePosition); click(context, candidatePosition);
            context.runOnClient(client -> {
                if (sent.size() != 1 || !sent.getFirst().equals(new ProtocolDoctrinePayloads.Request(ProtocolDoctrinePayloads.Kind.SELECT_SUBDOCTRINE,
                        empty.session(), empty.revision(), "land", "infantry", "motorized_infantry")))
                    throw new AssertionError("One click must issue exactly one authoritative subdoctrine selection");
                ((ScreenDoctrine)client.gui.screen()).update(empty);
            });
            click(context, candidatePosition);
            context.runOnClient(client -> {
                if (sent.size() != 1) throw new AssertionError("Old response must not unlock the pending selection");
                ((ScreenDoctrine)client.gui.screen()).update(selected);
            });
            context.getInput().setCursorPos(2450, 1300); context.waitTicks(3); context.takeScreenshot("hoi-doctrine-land-selected-gui2");
            var supportTrack = selected.tracks().stream().filter(t -> t.id().equals("support_units")).findFirst().orElseThrow();
            var engineering = supportTrack.candidates().stream().filter(c -> c.id().equals("complex_engineering")).findFirst().orElseThrow();
            if (!engineering.detail().contains("참호: §G+0.05§!") || !engineering.advancedDetail().contains("참호: §G+10.00%§!")
                    || !engineering.advancedDetail().contains("§G-1%§!"))
                throw new AssertionError("Native entrenchment point and percent displays must survive the server wire");
            click(context, position(context, supportTrack.name())); context.waitTicks(2);
            var engineeringPosition = position(context, engineering.name());
            context.getInput().setCursorPos(engineeringPosition[0] * 2, engineeringPosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-engineering-point-tooltip-gui2");
            click(context, position(context, "고급 세부 사항"));
            context.getInput().setCursorPos(engineeringPosition[0] * 2, engineeringPosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-engineering-percent-tooltip-gui2");
            context.runOnClient(client -> {
                var screen = (ScreenDoctrine)client.gui.screen(); var bounds = screen.tooltipBounds();
                if (bounds == null || bounds.x() - 4 < 0 || bounds.y() - 4 < 0
                        || bounds.x() + bounds.width() + 4 > screen.width || bounds.y() + bounds.height() + 4 > screen.height)
                    throw new AssertionError("All doctrine tooltip edges must remain inside the viewport");
                if (bounds.scroll() != 0) throw new AssertionError("Advanced details must open at their first line");
            });
            context.getInput().resizeWindow(2560, 800); context.waitTicks(2);
            var compactPosition = position(context, engineering.name());
            context.getInput().setCursorPos(compactPosition[0] * 2, compactPosition[1] * 2); context.waitTicks(2);
            context.takeScreenshot("hoi-doctrine-engineering-percent-tooltip-short-viewport-gui2");
            context.runOnClient(client -> {
                var screen = (ScreenDoctrine)client.gui.screen(); var bounds = screen.tooltipBounds();
                if (bounds == null || bounds.maxScroll() <= 0 || bounds.y() - 4 < 0 || bounds.y() + bounds.height() + 4 > screen.height)
                    throw new AssertionError("Short viewports must retain bounded scrollable native rewards");
                screen.mouseScrolled(compactPosition[0], compactPosition[1], 0, -1000);
            });
            context.waitTicks(2); context.takeScreenshot("hoi-doctrine-engineering-percent-tooltip-bottom-gui2");
            context.runOnClient(client -> {
                var bounds = ((ScreenDoctrine)client.gui.screen()).tooltipBounds();
                if (bounds.scroll() != bounds.maxScroll()) throw new AssertionError("Final reward must be reachable by scrolling");
            });
            context.getInput().resizeWindow(2560, 1440); context.waitTicks(2);
            click(context, position(context, "고급 세부 사항"));
            click(context, position(context, "선택 창 닫기"));
            var mechanizedTrack = selected.tracks().stream().filter(t -> t.id().equals("mechanized")).findFirst().orElseThrow();
            var ironWall = mechanizedTrack.candidates().stream().filter(c -> c.id().equals("iron_wall")).findFirst().orElseThrow();
            if (!ironWall.detail().contains("참호: §G+0.01§!")) throw new AssertionError("Iron-wall battalion addition must retain the native point display");
            click(context, position(context, mechanizedTrack.name())); context.waitTicks(2);
            var ironPosition = position(context, ironWall.name());
            context.getInput().setCursorPos(ironPosition[0] * 2, ironPosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-iron-wall-point-tooltip-gui2");
            context.runOnClient(client -> {
                if (sent.stream().filter(r -> r.kind() == ProtocolDoctrinePayloads.Kind.SELECT_SUBDOCTRINE).count() != 1
                        || sent.stream().anyMatch(r -> r.kind() != ProtocolDoctrinePayloads.Kind.SELECT_SUBDOCTRINE && r.kind() != ProtocolDoctrinePayloads.Kind.REFRESH))
                    throw new AssertionError("Inspecting and scrolling entrenchment rewards must not spend XP");
            });
            context.runOnClient(client -> ((ScreenDoctrine)client.gui.screen()).update(air));
            context.waitTicks(3); context.takeScreenshot("hoi-doctrine-air-empty-gui2");
            var grandName = air.grands().stream().filter(ProtocolDoctrineView.Choice::selected).findFirst().map(ProtocolDoctrineView.Choice::name).orElse("대교리 선택");
            click(context, position(context, grandName)); context.waitTicks(3); context.takeScreenshot("hoi-doctrine-air-grand-candidates-gui2");
            var alternative=air.grands().stream().filter(c->!c.selected()).findFirst().orElseThrow();
            if(!alternative.detail().contains("대교리를 교체하면 §R모든§! £doctrine_mastery §H통달치§!§R가 초기화됩니다.§!"))throw new AssertionError("Native grand-switch warning must retain its wording, icon and colors");
            context.runOnClient(client -> {
                var icon = dev.hoi.client.ui.UiKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal("£doctrine_mastery"), false).getString();
                if (icon.length() != 1 || icon.charAt(0) < 0xe000 || icon.charAt(0) > 0xf8ff) throw new AssertionError("Native mastery token must resolve to an actual resource-pack glyph");
            });
            var grandPosition=position(context,alternative.name());context.getInput().setCursorPos(grandPosition[0]*2,grandPosition[1]*2);context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-air-grand-switch-warning-gui2");
            click(context,position(context,"선택 창 닫기"));
            context.runOnClient(client->{if(sent.stream().anyMatch(r->r.kind()==ProtocolDoctrinePayloads.Kind.SELECT_GRAND))throw new AssertionError("Inspecting and closing a grand-doctrine choice cannot spend XP");});
            context.setScreen(() -> null);
            branchSelection(context, "naval", "submarines", "wolfpacks");
            branchSelection(context, "air", "strike_aircraft", "advanced_recon");
            nativeProgress(context);
            confirmedGrandSwitch(context);
            centeredLayoutStress(context);
        } catch (Exception failure) { throw new AssertionError("Server doctrine screen fixture failed", failure); }
    }
}
