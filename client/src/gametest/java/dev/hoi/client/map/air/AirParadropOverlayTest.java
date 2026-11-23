package dev.hoi.client.map.air;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.client.map.commander.CommanderOrders;
import dev.hoi.client.map.naval.NavalSeaHover;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class AirParadropOverlayTest {
    private AirParadropOverlayTest() {}
    public static void run(ClientGameTestContext game,MenuView menu,java.util.function.Consumer<String> serverCommand){
        CommanderHudPayloads.View source;
        try{source=new CommanderHudPayloads.Snapshot(java.nio.file.Files.readString(java.nio.file.Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-command-panel-army.json"))).view();}
        catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
        String army=source.cards().stream().filter(c->c.role()==CommanderHudPayloads.Mode.ASSIGN_ARMY&&c.panel()!=null).findFirst().orElseThrow().id();
        serverCommand.accept("fill -10 63 0 10 63 25 minecraft:stone");
        serverCommand.accept("gamemode spectator @a");serverCommand.accept("tp @a 0 72 0 0 35");
        game.waitTicks(10);
        String label="타이중 - 3개 공수사단 - 9 군 - 수송기 50기",detail="§R공수 작전 준비 완료까지 앞으로 §R3§!/§Y7§!일.";
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            game.getInput().resizeWindow(size[0],size[1]);var token=UUID.randomUUID().toString();var queries=new ArrayList<CommanderHudPayloads.SeaHover>();
            game.runOnClient(client->{
                client.options.guiScale().set(2);CommanderHud.reset();AgencyHud.reset();
                ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));
                CommanderOrders.selectionObserver=p->{};CommanderOrders.commandObserver=p->{};NavalSeaHover.queryObserver=queries::add;
                String dimension=client.level.dimension().identifier().toString();
                CommanderHud.accept(new CommanderHudPayloads.View("KOR",dimension,CommanderHudPayloads.Mode.ARMY,source.cards(),source.wings()));
                CommanderOrders.accept(new CommanderOrderPayloads.Context(token,10,"KOR",dimension,"LAND/PARADROP"));
                AirParadropOverlay.accept(new CommanderParadrop.View(token,10,"KOR",dimension,List.of(new CommanderParadrop.Plan("plan_drop",army,label,new CommanderParadrop.Point(-4,8),new CommanderParadrop.Point(5,20),false,false,detail))));
            });
            game.setScreen(MapCursor::new);game.waitTicks(5);
            game.runOnClient(client->{
                if(AirParadropOverlay.plans().size()!=1)throw new AssertionError("Issued paradrop plan must be visible");
                if(AirParadropOverlay.labels.size()!=1||!AirParadropOverlay.labels.getFirst().text().equals(label))throw new AssertionError("Paradrop arrow keeps the original label");
                var hit=AirParadropOverlay.hits.stream().findFirst().orElseThrow(()->new AssertionError("Paradrop arc must render visible geometry"));
                float x=(hit.corners()[0]+hit.corners()[2]+hit.corners()[4]+hit.corners()[6])/4,y=(hit.corners()[1]+hit.corners()[3]+hit.corners()[5]+hit.corners()[7])/4;
                if(!AirParadropOverlay.tooltip(x,y).equals(label+"\n"+detail))throw new AssertionError("Paradrop arc tooltip shows the label and the readiness line");
                var other=new CommanderParadrop.View(UUID.randomUUID().toString(),10,"KOR",client.level.dimension().identifier().toString(),List.of());AirParadropOverlay.accept(other);
                if(AirParadropOverlay.plans().size()!=1)throw new AssertionError("A stale context must not replace the paradrop view");
            });
            game.takeScreenshot("hoi-paradrop-map-synthetic-"+size[0]+"-gui2");
            game.runOnClient(client->{
                NavalSeaHover.point(new CommanderHudPayloads.Ray(0,-1,0),client.gameRenderer.mainCamera().position(),"KOR/"+client.level.dimension().identifier()+"/ARMY");
                queries.clear();
                for(int tick=0;tick<4&&queries.isEmpty();tick++)NavalSeaHover.tick();
                if(queries.isEmpty())throw new AssertionError("Drawing a paradrop must ask the server for the original province tooltip");
                String text="§Y공수 출발 지점§!을 지정하려면 §G클릭§!하십시오.";
                NavalSeaHover.accept(new CommanderHudPayloads.SeaHoverTooltip(queries.getLast().sequence(),"PARADROP",text));
                if(!NavalSeaHover.text().equals(text))throw new AssertionError("Paradrop hover tooltip must be shown while drawing");
                NavalSeaHover.queryObserver=null;
            });
            game.setScreen(()->null);
        }
    }
}
