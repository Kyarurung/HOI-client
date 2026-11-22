package dev.hoi.client.map.commander;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.protocol.unit.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.*;

public final class CommanderHudTest {
    private CommanderHudTest() {}
    public static double[] armyCardCenter() {
        var box=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();
        return new double[]{box.x()+box.width()/2.,box.y()+box.height()/2.};
    }
    public static void runMovementInput(ClientGameTestContext context,MenuView menu,java.util.function.Consumer<String> command){
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)throw new IllegalStateException("Authoritative commander fixtures are required");
        try{CommanderOrdersTest.run(context,menu,directory);CommanderMissionButtonsTest.run(context,menu,directory);}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
        CommanderHierarchySelectionTest.run(context,menu);CommanderSelectionDetailsTest.run(context,menu);CommanderRightClickTest.run(context,command);
    }
    public static void runArmyGroups(ClientGameTestContext context,MenuView menu){CommanderGroupsRenderTest.run(context,menu);}
    public static void runArmyLayout(ClientGameTestContext context,MenuView menu){CommanderPanelsTest.runArmyLayout(context,menu);CommanderGroupsRenderTest.run(context,menu);CommanderCombatLogTest.run(context,menu);}
    public static void runHeadquartersLayout(ClientGameTestContext context,MenuView menu){CommanderGroupsRenderTest.run(context,menu);}
    public static void runTraitOverlay(ClientGameTestContext game,MenuView menu){
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try{for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720},new int[]{2560,1369}))for(boolean navy:List.of(false,true)){
            game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));});
            var source=new Snapshot(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"kor-command-panel-"+(navy?"navy":"army-assigned")+".json"))).view();
            game.runOnClient(client->{CommanderHud.reset();CommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),source.running(),source.theaters(),source.armyTheaters()));CommanderPanels.select(source.cards().stream().filter(c->c.panel()!=null).findFirst().orElseThrow().id(),false);});
            game.setScreen(MapCursor::new);game.waitTicks(2);dev.hoi.client.screen.commander.CommanderTraitTreeTest.overlay(game,navy?"kor-navy-trait-tree":"kor-marshal-trait-tree");
            game.getInput().setCursorPos(size[0]-5,size[1]-5);game.waitTicks(2);game.takeScreenshot("hoi-traits-over-command-hud-"+(navy?"navy":"army")+"-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{var screen=client.gui.screen();var units=new dev.hoi.client.ui.UiUnits(screen.width,screen.height);double x=screen.width/2.+units.length(300),y=screen.height/2.+units.length(190);var commands=new ArrayList<CommanderOrderPayloads.Command>();CommanderOrders.commandObserver=commands::add;try{if(!screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false)||!commands.isEmpty())throw new AssertionError("Trait background must consume overlapping HUD clicks");}finally{CommanderOrders.commandObserver=null;}});
        }}catch(java.io.IOException error){throw new AssertionError(error);}finally{game.runOnClient(client->{CommanderHud.reset();CommanderUnitHud.reset();});game.setScreen(()->null);}
    }
    public static void run(ClientGameTestContext context,MenuView menu) {
        CommanderPanelsTest.run(context,menu);
        CommanderGroupsRenderTest.run(context,menu);
        CommanderCombatLogTest.run(context,menu);
        CommanderAirBasePanelsTest.run(context,menu);
        var hud=menu.hud();
        context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));
        var hierarchy=List.of(new Card("group","1 집단군","원수","politics/empty/leader","","",1,5,"집단군",Mode.ARMY_GROUP),
                new Card("army","군 1","장군","politics/empty/leader","원수","",57,18,"군",Mode.ASSIGN_ARMY));
        for(var branch:List.of(Mode.ARMY,Mode.AIR)) {
            context.runOnClient(client->{ClientCampaignHud.accept(HudPayloads.State.of("KOR",hud));CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),branch,hierarchy));});
            context.setScreen(MapCursor::new);context.waitTicks(2);
            context.runOnClient(client->{
                if(CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_GROUP_MARSHAL).map(b->List.of(b.x(),b.width(),b.request().target())).distinct().count()!=1)throw new AssertionError("One independent marshal card per army group is required");
                if(CommanderHud.boxes().stream().noneMatch(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_ARMY&&b.request().target().equals("army")))throw new AssertionError("Army and air maps retain actual army commanders");
                var group=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals("group")&&(b.request().mode()==Mode.ARMY_GROUP||b.request().mode()==Mode.ASSIGN_GROUP_MARSHAL)).max(Comparator.comparingInt(b->b.y()+b.height())).orElseThrow();
                var army=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();
                if(group.y()+group.height()!=army.y()+army.height()+new dev.hoi.client.ui.UiUnits(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight()).size(14))throw new AssertionError("Marshal and army cards must share one bottom-aligned strip");
                {
                    var addGroup=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ARMY_GROUPS).findFirst().orElseThrow();
                    if(addGroup.height()<=addGroup.width()||addGroup.y()+addGroup.height()!=group.y()+group.height())throw new AssertionError("Create group must use a full source portrait in the same strip");
                    if(CommanderHud.boxes().stream().noneMatch(b->b.request()!=null&&b.request().mode()==Mode.CREATE_ARMY))throw new AssertionError("Army and air maps must retain native army creation controls for actual army cards");
                }
                if(!CommanderHud.clickAt(group.x()+2,group.y()+2))throw new AssertionError("Group management must consume actual HUD input");
                if(branch==Mode.AIR&&!CommanderPanels.selected("group"))throw new AssertionError("Air map group footer must select its army group");
            });context.takeScreenshot("hoi-army-group-bottom-"+branch.name().toLowerCase(Locale.ROOT)+"-gui2");
        }
        var anchored = context.computeOnClient(client -> List.copyOf(CommanderHud.boxes()));
        for (var tab : List.of(MenuTab.POLITICS, MenuTab.OFFICER_CORPS, MenuTab.TRADE, MenuTab.POLITICS)) {
            context.runOnClient(client -> CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),Mode.AIR,hierarchy)));
            context.setScreen(() -> new dev.hoi.client.screen.ScreenMenu(menu,tab,() -> {}));
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (!layout(CommanderHud.boxes()).equals(layout(anchored))) throw new AssertionError("Left panel moved commander HUD anchor or hitboxes: " + tab);
                for (var box : CommanderHud.boxes()) {
                    if (box.x()<0 || box.y()<0 || box.x()+box.width()>client.getWindow().getGuiScaledWidth()
                            || box.y()+box.height()>client.getWindow().getGuiScaledHeight())
                        throw new AssertionError("Commander HUD hitbox escaped viewport");
                }
            });
            context.takeScreenshot("hoi-commander-anchor-panel-"+tab.name().toLowerCase(Locale.ROOT)+"-gui2");
        }
        context.setScreen(MapCursor::new);
        context.waitTicks(2);
        context.runOnClient(client -> {
            if (!layout(CommanderHud.boxes()).equals(layout(anchored))) throw new AssertionError("Closing a panel moved commander HUD");
        });
        context.takeScreenshot("hoi-commander-anchor-panel-closed-gui2");
        for(int[] viewport:List.of(new int[]{2560,1369,2},new int[]{1280,720,2},new int[]{1920,1080,2},new int[]{2560,1440,1},new int[]{2560,1440,3},new int[]{1280,720,3})) {
            context.getInput().resizeWindow(viewport[0],viewport[1]);context.runOnClient(client->{client.options.guiScale().set(viewport[2]);CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),Mode.ARMY,hierarchy));});
            context.setScreen(MapCursor::new);context.waitTicks(3);
            var resized=context.computeOnClient(client->List.copyOf(CommanderHud.boxes()));
            context.runOnClient(client->{
                var units=new dev.hoi.client.ui.UiUnits(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight(),1);
                int bottom=CommanderHud.boxes().stream().mapToInt(b->b.y()+b.height()).max().orElseThrow();
                if(bottom!=units.height())throw new AssertionError("Paused commander strip must touch the bottom edge: actual="+bottom+", expected="+(units.height()));
            });
            context.setScreen(()->new dev.hoi.client.screen.ScreenMenu(menu,MenuTab.OFFICER_CORPS,()->{}));context.waitTicks(2);
            context.runOnClient(client->{
                if(!layout(CommanderHud.boxes()).equals(layout(resized)))throw new AssertionError("Panel width moved commander hitboxes after viewport change");
                for(var box:CommanderHud.boxes())if(box.x()<0||box.y()<0||box.x()+box.width()>client.getWindow().getGuiScaledWidth()||box.y()+box.height()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Resized commander box escaped viewport");
            });
            context.takeScreenshot("hoi-commander-anchor-"+viewport[0]+"x"+viewport[1]+"-gui"+viewport[2]);
        }
        context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));
        var cards=new ArrayList<Card>();
        for(int i=0;i<160;i++)cards.add(new Card("hoi:army/"+i,"군 "+(i+1),"미배치","politics/empty/leader","","",i%25,18,"지휘관 임명"));
        context.runOnClient(client->{
            ClientCampaignHud.accept(HudPayloads.State.of("KOR",hud));
            CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),Mode.ARMY,cards));
        });
        context.setScreen(MapCursor::new);context.waitTicks(3);context.takeScreenshot("hoi-commander-map-bottom-first-gui2");
        context.runOnClient(client->{
            var plans=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ARMY_PLANS).findFirst().orElseThrow();
            if(!plans.request().selection().isEmpty()||!CommanderHud.clickAt(plans.x()+2,plans.y()+2))throw new AssertionError("Army plan entry must consume HUD click with owned card only");
            var next=CommanderHud.boxes().stream().filter(b->b.pageDelta()==1).findFirst().orElseThrow();
            if(!CommanderHud.clickAt(next.x()+4,next.y()+4))throw new AssertionError("Next page must consume HUD click");
        });
        context.waitTicks(2);context.runOnClient(client->{
            var previous=CommanderHud.boxes().stream().filter(b->b.pageDelta()==-1).findFirst().orElseThrow();
            if(CommanderHud.boxes().stream().filter(b->b.request()!=null).anyMatch(b->b.request().target().equals("hoi:army/0")))throw new AssertionError("Paging must expose different commander cards");
            if(!CommanderHud.clickAt(previous.x()+4,previous.y()+4))throw new AssertionError("Previous page must consume HUD click");
        });
        context.waitTicks(2);
        context.runOnClient(client->{var card=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals("hoi:army/0")).findFirst().orElseThrow();if(!CommanderHud.scrollAt(card.x()+card.width()/2.,card.y()+card.height()/2.,-1))throw new AssertionError("Wheel over a card must consume input and page a long strip");});context.waitTicks(2);
        context.runOnClient(client->{if(CommanderHud.boxes().stream().anyMatch(b->b.request()!=null&&b.request().target().equals("hoi:army/0")))throw new AssertionError("Card wheel did not advance");var card=CommanderHud.boxes().stream().filter(b->b.request()!=null&&!b.request().target().isEmpty()).findFirst().orElseThrow();CommanderHud.scrollAt(card.x()+card.width()/2.,card.y()+card.height()/2.,1);});context.waitTicks(2);
        for(int[] viewport:List.of(new int[]{2560,1369,2},new int[]{1280,720,2},new int[]{1920,1080,2},new int[]{2560,1440,1},new int[]{2560,1440,3},new int[]{1280,720,3})) {
            context.getInput().resizeWindow(viewport[0],viewport[1]);
            context.runOnClient(client->{client.options.guiScale().set(viewport[2]);CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),Mode.ARMY,cards));});
            context.setScreen(MapCursor::new);context.waitTicks(3);
            context.runOnClient(client->{
                for(var box:CommanderHud.boxes())if(box.x()<0||box.y()<0||box.x()+box.width()>client.getWindow().getGuiScaledWidth()||box.y()+box.height()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Scaled pagination escaped viewport");
                var next=CommanderHud.boxes().stream().filter(b->b.pageDelta()==1).findFirst().orElseThrow();
                for(var box:CommanderHud.boxes())if(box.request()!=null&&box.x()<next.x()+next.width()&&box.x()+box.width()>next.x()&&box.y()<next.y()+next.height()&&box.y()+box.height()>next.y())throw new AssertionError("Scaled page button overlaps a commander hitbox");
                if(!CommanderHud.clickAt(next.x()+next.width()/2.0,next.y()+next.height()/2.0))throw new AssertionError("Scaled next-page input failed");
            });
            context.waitTicks(2);
            context.runOnClient(client->{
                if(CommanderHud.boxes().stream().anyMatch(b->b.request()!=null&&b.request().target().equals("hoi:army/0")))throw new AssertionError("Scaled page input was intercepted by a commander");
                var previous=CommanderHud.boxes().stream().filter(b->b.pageDelta()==-1).findFirst().orElseThrow();
                for(var box:CommanderHud.boxes())if(box.request()!=null&&box.x()<previous.x()+previous.width()&&box.x()+box.width()>previous.x()&&box.y()<previous.y()+previous.height()&&box.y()+box.height()>previous.y())throw new AssertionError("Scaled previous-page button overlaps a commander hitbox");
                if(!CommanderHud.clickAt(previous.x()+previous.width()/2.0,previous.y()+previous.height()/2.0))throw new AssertionError("Scaled previous-page input failed");
            });context.waitTicks(2);
            if(viewport[0]==1280)context.takeScreenshot("hoi-commander-paging-1280x720-gui3");
        }
        context.getInput().resizeWindow(2560,1440);context.runOnClient(client->{client.options.guiScale().set(2);CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),Mode.ARMY,cards));});
        context.setScreen(MapCursor::new);context.waitTicks(3);context.runOnClient(client->{
            if(CommanderHud.boxes().stream().noneMatch(b->b.request()!=null&&b.request().target().equals("hoi:army/0")))throw new AssertionError("Previous page must restore first card");
            var forward=new net.minecraft.world.phys.Vec3(client.gameRenderer.mainCamera().forwardVector());
            var center=client.gameRenderer.mainCamera().position().add(forward.scale(12));
            var counter=new UnitHud.Counter("commander-test-stack","KOR","대한민국","infantry","제1사단",1,center.x,center.y-3.1,center.z,1,1,1,Map.of()).withAllegiance(UnitHud.Allegiance.OURS);
            CommanderUnitHud.accept(new UnitHud.Snapshot(client.level.dimension().identifier().toString(),List.of(counter)));
        });
        context.waitTicks(3);context.runOnClient(client->{
            var unit=CommanderUnitHud.boxes().stream().filter(b->b.counter().key().equals("commander-test-stack|infantry")).findFirst().orElseThrow();
            if(!CommanderUnitHud.selectAt(unit.x()+2,unit.y()+2))throw new AssertionError("Actual own map counter selection is required");
        });
        context.waitTicks(2);context.runOnClient(client->{
            var move=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.MOVE_DIVISIONS).findFirst().orElseThrow();
            if(!move.request().selection().equals(List.of("commander-test-stack|infantry")))throw new AssertionError("HUD move request must contain exact selected keys");
            if(!CommanderHud.clickAt(move.x()+2,move.y()+2))throw new AssertionError("Move selection must consume HUD click");
        });
        context.takeScreenshot("hoi-commander-map-selected-move-gui2");
        for(var branch:List.of(Mode.NAVY_HQ,Mode.AIR)) {
            String id=branch==Mode.NAVY_HQ?"hoi:fleet/mission-test":"hoi:wing/mission-test";
            context.runOnClient(client->{
                CommanderUnitHud.reset();
                CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),branch,
                    List.of(new Card(id,branch==Mode.NAVY_HQ?"함대":"비행단","","politics/empty/leader","","",12,0,"임무 선택"))));
            });
            context.waitTicks(2);
            if(branch==Mode.NAVY_HQ) {
                context.runOnClient(client->{
                    for(int frame=0;frame<2;frame++)if(dev.hoi.client.ui.UiAssets.imageHeight("commander/hud/navy_frame/frame_"+frame,44)<=0)throw new AssertionError("Original naval normal/highlight frames are missing");
                });
                context.getInput().setCursorPos(2400,300);context.waitTicks(2);context.takeScreenshot("hoi-native-navy-bottom-unselected-gui2");
                var point=context.computeOnClient(client->{
                    var box=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.NAVY_FLEET).findFirst().orElseThrow();
                    return new int[]{box.x()+box.width()/2,box.y()+box.height()/2};
                });
                context.getInput().setCursorPos(point[0]*2,point[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-native-navy-bottom-hover-gui2");
                context.runOnClient(client->{if(!CommanderHud.clickAt(point[0],point[1]))throw new AssertionError("Native naval portrait must retain fleet request routing");});
                context.getInput().setCursorPos(2400,300);context.waitTicks(2);context.takeScreenshot("hoi-native-navy-bottom-selected-gui2");
            }
            context.runOnClient(client->{
                var mode=branch==Mode.NAVY_HQ?Mode.NAVAL_TASK_FORCES:Mode.AIR_MISSION;
                var box=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==mode).findFirst().orElseThrow();
                if(!box.request().target().equals(id)||!box.request().selection().isEmpty())throw new AssertionError("Mission HUD must send only its owned card target");
                if(CommanderHud.boxes().stream().anyMatch(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_MARSHAL))throw new AssertionError("Naval/air branches must not invent army marshal slots");
                if(!CommanderHud.clickAt(box.x()+2,box.y()+2))throw new AssertionError("Mission strip must consume HUD input");
            });
            context.takeScreenshot("hoi-mission-map-"+branch.name().toLowerCase(java.util.Locale.ROOT)+"-entry-gui2");
        }
        context.runOnClient(client->{ClientCampaignHud.accept(HudPayloads.State.HIDDEN);CommanderHud.accept(new View("",client.level.dimension().identifier().toString(),Mode.ARMY,List.of()));});
        context.waitTicks(2);
        context.runOnClient(client->{if(!CommanderHud.boxes().isEmpty()||!CommanderPanels.selectedCards().isEmpty()||!CommanderPanels.hits().isEmpty())throw new AssertionError("Inactive empty-country snapshots must hide all cards, creation entries and cached controls");});
        context.takeScreenshot("hoi-commander-before-start-hidden-gui2");
        context.runOnClient(client->{ClientCampaignHud.accept(HudPayloads.State.of("KOR",hud));CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),Mode.ARMY,hierarchy));});context.waitTicks(2);
        context.runOnClient(client->{if(CommanderHud.boxes().isEmpty())throw new AssertionError("Fresh authoritative active snapshot must restore commander cards");CommanderHud.accept(new View("",client.level.dimension().identifier().toString(),Mode.ARMY,List.of()));if(!CommanderHud.boxes().isEmpty()||!CommanderPanels.selectedCards().isEmpty())throw new AssertionError("Server revocation must clear cards immediately even before country HUD refresh");});
        context.runOnClient(client->{ClientCampaignHud.accept(HudPayloads.State.HIDDEN);if(CommanderHud.clickAt(0,0))throw new AssertionError("Revoked country must reject commander HUD input");CommanderHud.reset();CommanderUnitHud.reset();});
        context.setScreen(()->null);context.getInput().resizeWindow(1600,1000);
    }
    private static List<List<Object>> layout(List<CommanderHud.Box> boxes){return boxes.stream().map(b->Arrays.<Object>asList(b.x(),b.y(),b.width(),b.height(),b.request(),b.pageDelta(),b.tip(),b.action()!=null)).toList();}

}
