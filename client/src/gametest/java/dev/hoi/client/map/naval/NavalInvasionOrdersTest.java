package dev.hoi.client.map.naval;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.client.map.commander.CommanderOrders;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class NavalInvasionOrdersTest {
    private static String army;
    private NavalInvasionOrdersTest() {}
    private static CommanderNavalInvasion.Plan plan(String id,String phase,boolean ready,double x){
        var actions=switch(phase){case "DRAFT"->List.of(CommanderNavalInvasion.Action.RESERVE);case "RESERVED"->List.of(CommanderNavalInvasion.Action.LAUNCH,CommanderNavalInvasion.Action.UNRESERVE);default->List.of(CommanderNavalInvasion.Action.ABORT);};
        return new CommanderNavalInvasion.Plan(id,army,"a".repeat(64),phase,new CommanderNavalInvasion.Point(x,8),new CommanderNavalInvasion.Point(x+3,18),actions,ready,dev.hoi.client.ui.UiCommandLabels.text("UNITS_VIEW_BTN_EXECUTE_POINT_OUT",""));
    }
    public static void run(ClientGameTestContext game,MenuView menu,java.util.function.Consumer<String> serverCommand){
        CommanderHudPayloads.View source;
        try{source=new CommanderHudPayloads.Snapshot(java.nio.file.Files.readString(java.nio.file.Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-command-panel-army.json"))).view();}
        catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
        army=source.cards().stream().filter(c->c.role()==CommanderHudPayloads.Mode.ASSIGN_ARMY&&c.panel()!=null).findFirst().orElseThrow().id();
        serverCommand.accept("fill -10 63 0 10 63 25 minecraft:stone");
        serverCommand.accept("gamemode spectator @a");serverCommand.accept("tp @a 0 72 0 0 35");
        game.waitTicks(10);
        int scale=game.computeOnClient(client->client.options.guiScale().get());
        boolean before="1".equals(System.getenv("HOI_CAPTURE_BEFORE"));
        try{
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);var commands=new ArrayList<CommanderOrderPayloads.Command>();var token=UUID.randomUUID().toString();
                game.runOnClient(client->{
                    client.options.guiScale().set(2);CommanderHud.reset();AgencyHud.reset();
                    ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));
                    CommanderOrders.selectionObserver=p->{};CommanderOrders.commandObserver=commands::add;
                    String dimension=client.level.dimension().identifier().toString();
                    CommanderHud.accept(new CommanderHudPayloads.View("KOR",dimension,CommanderHudPayloads.Mode.ARMY,source.cards(),source.wings()));
                    CommanderOrders.accept(new CommanderOrderPayloads.Context(token,10,"KOR",dimension,""));
                    NavalInvasionOrders.accept(new CommanderNavalInvasion.View(token,10,"KOR",dimension,List.of(plan("plan_first","DRAFT",false,-4),plan("plan_second","DRAFT",false,4))));
                });
                game.setScreen(MapCursor::new);game.waitTicks(5);
                if(!before)game.runOnClient(client->{if(NavalInvasionOverlay.texture()==null||NavalInvasionOverlay.hits().isEmpty())throw new AssertionError("Original invasion arrow must render before input");});
                game.takeScreenshot("hoi-invasion-map-synthetic-"+size[0]+"-gui2");
                if(before){game.setScreen(()->null);continue;}
                game.getInput().holdKey(InputConstants.KEY_LSHIFT);game.waitTicks(2);
                try{game.runOnClient(client->{
                    var button=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==CommanderHudPayloads.Mode.ARMY_TOOLBAR&&b.request().target().equals("ACTIVATE")&&b.request().selection().equals(List.of(army))).findFirst().orElseThrow();
                    CommanderHud.clickAt(button.x()+button.width()/2.,button.y()+button.height()/2.,InputConstants.MOUSE_BUTTON_RIGHT,false);
                    if(NavalInvasionOrders.targeting()||!commands.isEmpty())throw new AssertionError("Right click execute must not activate plans");
                    CommanderHud.clickAt(button.x()+button.width()/2.,button.y()+button.height()/2.,InputConstants.MOUSE_BUTTON_LEFT,false);
                    if(!NavalInvasionOrders.targeting()||!commands.isEmpty())throw new AssertionError("Shift execute must target a plan without launching all armies");
                });}finally{game.getInput().releaseKey(InputConstants.KEY_LSHIFT);}
                game.runOnClient(client->{
                    if(!NavalInvasionOrders.targeting())throw new AssertionError("Plan targeting did not survive Shift release");
                    var hit=NavalInvasionOverlay.hits().stream().filter(h->h.id().equals("plan_first")).filter(h->h.corners()[1]>70&&h.corners()[1]<client.getWindow().getGuiScaledHeight()-120).findFirst().orElseThrow();
                    double x=0,y=0;for(int i=0;i<4;i++){x+=hit.corners()[i*2]/4;y+=hit.corners()[i*2+1]/4;}
                    var click=new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
                    client.gui.screen().mouseClicked(click,false);client.gui.screen().mouseReleased(click);
                    NavalInvasionOrders.execute("plan_first");
                    if(commands.size()!=1||!commands.getFirst().action().equals("INVASION/RESERVE")||!commands.getFirst().selection().equals(List.of("plan_first","a".repeat(64))))throw new AssertionError("Arrow must send the one issued plan exactly once: "+commands);
                    String dimension=client.level.dimension().identifier().toString();
                    CommanderOrders.accept(new CommanderOrderPayloads.Context(token,11,"KOR",dimension,""));
                    if(!NavalInvasionOrders.plans().isEmpty())throw new AssertionError("Old invasion revision survived acknowledgement");
                    NavalInvasionOrders.accept(new CommanderNavalInvasion.View(token,10,"KOR",dimension,List.of(plan("plan_first","DRAFT",false,-4))));
                    if(!NavalInvasionOrders.plans().isEmpty())throw new AssertionError("Stale snapshot reopened execution");
                    NavalInvasionOrders.accept(new CommanderNavalInvasion.View(token,11,"KOR",dimension,List.of(plan("plan_first","RESERVED",false,-4))));
                    NavalInvasionOrders.begin(List.of(army),false);
                    if(NavalInvasionOrders.execute("plan_first")||commands.size()!=1)throw new AssertionError("Unready launch must not send");
                    NavalInvasionOverlay.click(x,y,InputConstants.MOUSE_BUTTON_RIGHT);
                    if(NavalInvasionOrders.targeting()||commands.size()!=1)throw new AssertionError("Right click must cancel targeting only");
                    NavalInvasionOrders.begin(List.of(army),true);NavalInvasionOrders.execute("plan_first");
                    if(commands.size()!=2||!commands.getLast().action().equals("INVASION/UNRESERVE"))throw new AssertionError("Stop reserved plan must unreserve exactly that plan");
                    CommanderOrders.accept(new CommanderOrderPayloads.Context(token,12,"KOR",dimension,""));
                    NavalInvasionOrders.accept(new CommanderNavalInvasion.View(token,12,"KOR",dimension,List.of(plan("plan_first","RESERVED",true,-4))));
                    NavalInvasionOrders.begin(List.of(army),false);NavalInvasionOrders.execute("plan_first");
                    if(commands.size()!=3||!commands.getLast().action().equals("INVASION/LAUNCH"))throw new AssertionError("Ready plan must send its issued launch once");
                    CommanderOrders.accept(new CommanderOrderPayloads.Context(token,13,"KOR",dimension,""));
                    NavalInvasionOrders.accept(new CommanderNavalInvasion.View(token,13,"KOR",dimension,List.of(plan("plan_first","TRANSIT",true,-4))));
                    NavalInvasionOrders.begin(List.of(army),true);NavalInvasionOrders.execute("plan_first");
                    if(commands.size()!=4||!commands.getLast().action().equals("INVASION/ABORT"))throw new AssertionError("Stop transit must abort exactly that plan");
                    ClientCampaignHud.accept(HudPayloads.State.HIDDEN);
                    if(!NavalInvasionOrders.plans().isEmpty()||NavalInvasionOrders.begin(List.of(army),false))throw new AssertionError("Country revocation retained private plans");
                });
                game.setScreen(()->null);
            }
        }finally{game.runOnClient(client->{CommanderHud.reset();CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;ClientCampaignHud.accept(HudPayloads.State.HIDDEN);client.options.guiScale().set(scale);});}
    }
}
