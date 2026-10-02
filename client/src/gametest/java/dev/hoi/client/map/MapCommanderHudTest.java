package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.*;

public final class MapCommanderHudTest {
    private MapCommanderHudTest() {}
    public static void run(ClientGameTestContext context,ProtocolCountryHud hud) {
        context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));
        var hierarchy=List.of(new Card("group","1 집단군","원수","politics/empty/leader","","",1,5,"집단군",Mode.ARMY_GROUP),
                new Card("army","군 1","장군","politics/empty/leader","원수","",57,18,"군",Mode.ASSIGN_ARMY));
        for(var branch:List.of(Mode.ARMY,Mode.AIR)) {
            context.runOnClient(client->{ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",hud));MapCommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),branch,hierarchy));});
            context.setScreen(ScreenMapCursor::new);context.waitTicks(2);
            context.runOnClient(client->{
                if(MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_GROUP_MARSHAL).count()!=1)throw new AssertionError("One independent marshal card per army group is required");
                if(MapCommanderHud.boxes().stream().noneMatch(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_ARMY&&b.request().target().equals("army")))throw new AssertionError("Army and air maps retain actual army commanders");
                var group=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ARMY_GROUP).findFirst().orElseThrow();
                if(!MapCommanderHud.clickAt(group.x()+2,group.y()+2))throw new AssertionError("Group management must consume actual HUD input");
            });context.takeScreenshot("hoi-army-group-bottom-"+branch.name().toLowerCase(Locale.ROOT)+"-gui2");
        }
        var cards=new ArrayList<Card>();
        for(int i=0;i<160;i++)cards.add(new Card("hoi:army/"+i,"군 "+(i+1),"미배치","politics/empty/leader","","",i%25,18,"지휘관 임명"));
        context.runOnClient(client->{
            ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",hud));
            MapCommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),Mode.ARMY,cards));
        });
        context.setScreen(ScreenMapCursor::new);context.waitTicks(3);context.takeScreenshot("hoi-commander-map-bottom-first-gui2");
        context.runOnClient(client->{
            var plans=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ARMY_PLANS).findFirst().orElseThrow();
            if(!plans.request().selection().isEmpty()||!MapCommanderHud.clickAt(plans.x()+2,plans.y()+2))throw new AssertionError("Army plan entry must consume HUD click with owned card only");
            var next=MapCommanderHud.boxes().stream().filter(b->b.pageDelta()==1).findFirst().orElseThrow();
            if(!MapCommanderHud.clickAt(next.x()+4,next.y()+4))throw new AssertionError("Next page must consume HUD click");
        });
        context.waitTicks(2);context.runOnClient(client->{
            var previous=MapCommanderHud.boxes().stream().filter(b->b.pageDelta()==-1).findFirst().orElseThrow();
            if(MapCommanderHud.boxes().stream().filter(b->b.request()!=null).anyMatch(b->b.request().target().equals("hoi:army/0")))throw new AssertionError("Paging must expose different commander cards");
            if(!MapCommanderHud.clickAt(previous.x()+4,previous.y()+4))throw new AssertionError("Previous page must consume HUD click");
        });
        context.waitTicks(2);context.runOnClient(client->{
            if(MapCommanderHud.boxes().stream().noneMatch(b->b.request()!=null&&b.request().target().equals("hoi:army/0")))throw new AssertionError("Previous page must restore first card");
            var forward=new net.minecraft.world.phys.Vec3(client.gameRenderer.mainCamera().forwardVector());
            var center=client.gameRenderer.mainCamera().position().add(forward.scale(12));
            var counter=new ProtocolUnitHud.Counter("commander-test-stack","KOR","대한민국","infantry","제1사단",1,center.x,center.y-3.1,center.z,1,1,1,Map.of()).withAllegiance(ProtocolUnitHud.Allegiance.OURS);
            MapUnitHud.accept(new ProtocolUnitHud.Snapshot(client.level.dimension().identifier().toString(),List.of(counter)));
        });
        context.waitTicks(3);context.runOnClient(client->{
            var unit=MapUnitHud.boxes().stream().filter(b->b.counter().key().equals("commander-test-stack|infantry")).findFirst().orElseThrow();
            if(!MapUnitHud.selectAt(unit.x()+2,unit.y()+2))throw new AssertionError("Actual own map counter selection is required");
        });
        context.waitTicks(2);context.runOnClient(client->{
            var move=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.MOVE_DIVISIONS).findFirst().orElseThrow();
            if(!move.request().selection().equals(List.of("commander-test-stack|infantry")))throw new AssertionError("HUD move request must contain exact selected keys");
            if(!MapCommanderHud.clickAt(move.x()+2,move.y()+2))throw new AssertionError("Move selection must consume HUD click");
        });
        context.takeScreenshot("hoi-commander-map-selected-move-gui2");
        for(var branch:List.of(Mode.NAVY_HQ,Mode.AIR)) {
            String id=branch==Mode.NAVY_HQ?"hoi:fleet/mission-test":"hoi:wing/mission-test";
            context.runOnClient(client->{
                MapUnitHud.reset();
                MapCommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),branch,
                    List.of(new Card(id,branch==Mode.NAVY_HQ?"함대":"비행단","","politics/empty/leader","","",12,0,"임무 선택"))));
            });
            context.waitTicks(2);context.runOnClient(client->{
                var mode=branch==Mode.NAVY_HQ?Mode.NAVAL_TASK_FORCES:Mode.AIR_MISSION;
                var box=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==mode).findFirst().orElseThrow();
                if(!box.request().target().equals(id)||!box.request().selection().isEmpty())throw new AssertionError("Mission HUD must send only its owned card target");
                if(MapCommanderHud.boxes().stream().anyMatch(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_MARSHAL))throw new AssertionError("Naval/air branches must not invent army marshal slots");
                if(!MapCommanderHud.clickAt(box.x()+2,box.y()+2))throw new AssertionError("Mission strip must consume HUD input");
            });
            context.takeScreenshot("hoi-mission-map-"+branch.name().toLowerCase(java.util.Locale.ROOT)+"-entry-gui2");
        }
        context.runOnClient(client->{ClientCampaignHud.accept(ProtocolHudPayloads.State.HIDDEN);if(MapCommanderHud.clickAt(0,0))throw new AssertionError("Revoked country must reject commander HUD input");MapCommanderHud.reset();MapUnitHud.reset();});
        context.setScreen(()->null);context.getInput().resizeWindow(1600,1000);
    }
}
