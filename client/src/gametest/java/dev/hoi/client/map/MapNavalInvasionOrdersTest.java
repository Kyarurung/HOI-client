package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class MapNavalInvasionOrdersTest {
    private static String army;
    private MapNavalInvasionOrdersTest() {}
    private static ProtocolNavalInvasion.Plan plan(String id,String phase,boolean ready,double x){
        var actions=switch(phase){case "DRAFT"->List.of(ProtocolNavalInvasion.Action.RESERVE);case "RESERVED"->List.of(ProtocolNavalInvasion.Action.LAUNCH,ProtocolNavalInvasion.Action.UNRESERVE);default->List.of(ProtocolNavalInvasion.Action.ABORT);};
        return new ProtocolNavalInvasion.Plan(id,army,"a".repeat(64),phase,new ProtocolNavalInvasion.Point(x,8),new ProtocolNavalInvasion.Point(x+3,18),actions,ready,dev.hoi.client.ui.UiCommandLabels.text("UNITS_VIEW_BTN_EXECUTE_POINT_OUT",""));
    }
    public static void run(ClientGameTestContext game,ProtocolMenuView menu,java.util.function.Consumer<String> serverCommand){
        ProtocolCommanderHud.View source;
        try{source=new ProtocolCommanderHud.Snapshot(java.nio.file.Files.readString(java.nio.file.Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-command-panel-army.json"))).view();}
        catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
        army=source.cards().stream().filter(c->c.role()==ProtocolCommanderHud.Mode.ASSIGN_ARMY&&c.panel()!=null).findFirst().orElseThrow().id();
        serverCommand.accept("fill -10 63 0 10 63 25 minecraft:stone");
        serverCommand.accept("gamemode spectator @a");serverCommand.accept("tp @a 0 72 0 0 35");
        game.waitTicks(10);
        int scale=game.computeOnClient(client->client.options.guiScale().get());
        boolean before="1".equals(System.getenv("HOI_CAPTURE_BEFORE"));
        try{
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);var commands=new ArrayList<ProtocolCommanderOrders.Command>();var token=UUID.randomUUID().toString();
                game.runOnClient(client->{
                    client.options.guiScale().set(2);MapCommanderHud.reset();MapAgencyHud.reset();
                    ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));
                    MapCommanderOrders.selectionObserver=p->{};MapCommanderOrders.commandObserver=commands::add;
                    String dimension=client.level.dimension().identifier().toString();
                    MapCommanderHud.accept(new ProtocolCommanderHud.View("KOR",dimension,ProtocolCommanderHud.Mode.ARMY,source.cards(),source.wings()));
                    MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(token,10,"KOR",dimension,""));
                    MapNavalInvasionOrders.accept(new ProtocolNavalInvasion.View(token,10,"KOR",dimension,List.of(plan("plan_first","DRAFT",false,-4),plan("plan_second","DRAFT",false,4))));
                });
                game.setScreen(ScreenMapCursor::new);game.waitTicks(5);
                if(!before)game.runOnClient(client->{if(MapNavalInvasionOverlay.texture()==null||MapNavalInvasionOverlay.hits().isEmpty())throw new AssertionError("Original invasion arrow must render before input");});
                game.takeScreenshot("hoi-invasion-map-synthetic-"+size[0]+"-gui2");
                if(before){game.setScreen(()->null);continue;}
                game.getInput().holdKey(InputConstants.KEY_LSHIFT);game.waitTicks(2);
                try{game.runOnClient(client->{
                    var button=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==ProtocolCommanderHud.Mode.ARMY_TOOLBAR&&b.request().target().equals("ACTIVATE")&&b.request().selection().equals(List.of(army))).findFirst().orElseThrow();
                    MapCommanderHud.clickAt(button.x()+button.width()/2.,button.y()+button.height()/2.,InputConstants.MOUSE_BUTTON_RIGHT,false);
                    if(MapNavalInvasionOrders.targeting()||!commands.isEmpty())throw new AssertionError("Right click execute must not activate plans");
                    MapCommanderHud.clickAt(button.x()+button.width()/2.,button.y()+button.height()/2.,InputConstants.MOUSE_BUTTON_LEFT,false);
                    if(!MapNavalInvasionOrders.targeting()||!commands.isEmpty())throw new AssertionError("Shift execute must target a plan without launching all armies");
                });}finally{game.getInput().releaseKey(InputConstants.KEY_LSHIFT);}
                game.runOnClient(client->{
                    if(!MapNavalInvasionOrders.targeting())throw new AssertionError("Plan targeting did not survive Shift release");
                    var hit=MapNavalInvasionOverlay.hits().stream().filter(h->h.id().equals("plan_first")).filter(h->h.corners()[1]>70&&h.corners()[1]<client.getWindow().getGuiScaledHeight()-120).findFirst().orElseThrow();
                    double x=0,y=0;for(int i=0;i<4;i++){x+=hit.corners()[i*2]/4;y+=hit.corners()[i*2+1]/4;}
                    var click=new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
                    client.gui.screen().mouseClicked(click,false);client.gui.screen().mouseReleased(click);
                    MapNavalInvasionOrders.execute("plan_first");
                    if(commands.size()!=1||!commands.getFirst().action().equals("INVASION/RESERVE")||!commands.getFirst().selection().equals(List.of("plan_first","a".repeat(64))))throw new AssertionError("Arrow must send the one issued plan exactly once: "+commands);
                    String dimension=client.level.dimension().identifier().toString();
                    MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(token,11,"KOR",dimension,""));
                    if(!MapNavalInvasionOrders.plans().isEmpty())throw new AssertionError("Old invasion revision survived acknowledgement");
                    MapNavalInvasionOrders.accept(new ProtocolNavalInvasion.View(token,10,"KOR",dimension,List.of(plan("plan_first","DRAFT",false,-4))));
                    if(!MapNavalInvasionOrders.plans().isEmpty())throw new AssertionError("Stale snapshot reopened execution");
                    MapNavalInvasionOrders.accept(new ProtocolNavalInvasion.View(token,11,"KOR",dimension,List.of(plan("plan_first","RESERVED",false,-4))));
                    MapNavalInvasionOrders.begin(List.of(army),false);
                    if(MapNavalInvasionOrders.execute("plan_first")||commands.size()!=1)throw new AssertionError("Unready launch must not send");
                    MapNavalInvasionOverlay.click(x,y,InputConstants.MOUSE_BUTTON_RIGHT);
                    if(MapNavalInvasionOrders.targeting()||commands.size()!=1)throw new AssertionError("Right click must cancel targeting only");
                    MapNavalInvasionOrders.begin(List.of(army),true);MapNavalInvasionOrders.execute("plan_first");
                    if(commands.size()!=2||!commands.getLast().action().equals("INVASION/UNRESERVE"))throw new AssertionError("Stop reserved plan must unreserve exactly that plan");
                    MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(token,12,"KOR",dimension,""));
                    MapNavalInvasionOrders.accept(new ProtocolNavalInvasion.View(token,12,"KOR",dimension,List.of(plan("plan_first","RESERVED",true,-4))));
                    MapNavalInvasionOrders.begin(List.of(army),false);MapNavalInvasionOrders.execute("plan_first");
                    if(commands.size()!=3||!commands.getLast().action().equals("INVASION/LAUNCH"))throw new AssertionError("Ready plan must send its issued launch once");
                    MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(token,13,"KOR",dimension,""));
                    MapNavalInvasionOrders.accept(new ProtocolNavalInvasion.View(token,13,"KOR",dimension,List.of(plan("plan_first","TRANSIT",true,-4))));
                    MapNavalInvasionOrders.begin(List.of(army),true);MapNavalInvasionOrders.execute("plan_first");
                    if(commands.size()!=4||!commands.getLast().action().equals("INVASION/ABORT"))throw new AssertionError("Stop transit must abort exactly that plan");
                    ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);
                    if(!MapNavalInvasionOrders.plans().isEmpty()||MapNavalInvasionOrders.begin(List.of(army),false))throw new AssertionError("Country revocation retained private plans");
                });
                game.setScreen(()->null);
            }
        }finally{game.runOnClient(client->{MapCommanderHud.reset();MapCommanderOrders.commandObserver=null;MapCommanderOrders.selectionObserver=null;ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);client.options.guiScale().set(scale);});}
    }
}
