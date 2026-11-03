package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import dev.hoi.protocol.ProtocolCommanderOrders.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class MapMissionButtonsTest {
    private MapMissionButtonsTest() {}
    static void run(ClientGameTestContext game,ProtocolMenuView menu,String directory)throws java.io.IOException {
        var queries=new ArrayList<Check>();var commands=new ArrayList<Command>();
        try{
            for(boolean navy:List.of(false,true))for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                String prefix="kor-"+(navy?"navy":"air")+"-mission-";
                var source=new Snapshot(Files.readString(Path.of(directory,prefix+"waiting-hud.json"))).view();var region=new Snapshot(Files.readString(Path.of(directory,prefix+"region-hud.json"))).view();var saved=new com.google.gson.Gson().fromJson(Files.readString(Path.of(directory,prefix+"waiting-context.json")),Context.class);var available=new Availability(Files.readString(Path.of(directory,prefix+"waiting-availability.json"))).assessment();
                var changedWings=source.wings().stream().filter(w->region.wings().stream().anyMatch(next->next.id().equals(w.id())&&!next.equals(w))).toList();var changedCards=source.cards().stream().filter(card->region.cards().stream().anyMatch(next->next.id().equals(card.id())&&!next.equals(card))).toList();
                if(navy?changedCards.size()!=1:changedWings.size()!=1)throw new AssertionError("Actual mission region assignment must change one selected unit");String action=navy?"NAVY/PATROL":"AIR/SUPERIORITY",icon=navy?"naval_mission_PATROL":"air_mission_SUPERIORITY";var issued=new Context[1];
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(client->{
                    client.options.guiScale().set(2);ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderHud.reset();MapUnitHud.reset();MapCommanderOrders.reset();queries.clear();commands.clear();
                    MapCommanderAvailability.queryObserver=queries::add;MapCommanderOrders.selectionObserver=q->{};MapCommanderOrders.commandObserver=commands::add;MapCommanderHud.requestObserver=null;
                    MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));
                    issued[0]=new Context(UUID.randomUUID().toString(),saved.revision()-1,source.country(),client.level.dimension().identifier().toString(),"");MapCommanderOrders.accept(issued[0]);
                    if(navy){var card=changedCards.getFirst();var next=region.cards().stream().filter(c->c.id().equals(card.id())).findFirst().orElseThrow();var force=card.panel().members().stream().filter(m->next.panel().members().stream().anyMatch(n->n.id().equals(m.id())&&!n.equals(m))).findFirst().orElseThrow();if(!MapCommanderPanels.openMission(new Open(Mode.NAVAL_MISSION,force.id())))throw new AssertionError("Select the actual mission task force");if(!MapCommanderPanels.commandSelection().equals(List.of("navy/"+force.id())))throw new AssertionError("Source availability belongs to one task force");if(!navalActivity(force).equals("ship_activity_no_assignment"))throw new AssertionError("Waiting fleet must show no assignment");}else MapCommanderPanels.selectWings(changedWings.stream().map(Member::id).toList());MapCommanderOrders.selectionChanged();
                    if(queries.isEmpty()||!MapCommanderAvailability.reason(action).isEmpty())throw new AssertionError("Mission must be enabled before an assessment arrives");
                });
                game.setScreen(ScreenMapCursor::new);game.getInput().setCursorPos(size[0]-20,80);game.waitTicks(3);
                game.runOnClient(client->{click(icon);click(icon);if(commands.size()!=1||!commands.getFirst().action().equals(action))throw new AssertionError("Pending assessment must allow one authenticated mission request, with repeated clicks locked");MapCommanderOrders.accept(new Context(issued[0].token(),saved.revision(),saved.country(),issued[0].dimension(),saved.pending()));var q=queries.getLast();MapCommanderAvailability.accept(new Assessment(q.token(),q.revision(),q.sequence(),q.request(),available.actions()));MapCommanderHud.accept(new View(source.country(),issued[0].dimension(),source.branch(),source.cards(),source.wings()));if(!MapCommanderAvailability.reason(action).isEmpty())throw new AssertionError("Actual region-free mission must remain enabled");});game.waitTicks(2);
                game.runOnClient(client->{if(!MapSelectorHud.hidesMessages()||!MapCommanderOrders.pending().equals(action))throw new AssertionError("Mission target mode must persist without an overlay explanation");});
                game.takeScreenshot("hoi-mission-await-region-"+(navy?"navy":"air")+"-"+size[0]+"x"+size[1]+"-gui2");
                game.runOnClient(client->{
                    var selected=MapCommanderPanels.commandSelection();if(!MapCommanderOrders.map(List.of(new Ray(0,-1,0)))||commands.size()!=2||!commands.getLast().action().equals("MAP")||!commands.getLast().selection().equals(selected))throw new AssertionError("Region right-click must retain exact unit selection");MapCommanderOrders.accept(new Context(issued[0].token(),saved.revision()+1,"KOR",issued[0].dimension(),""));MapCommanderHud.accept(new View(region.country(),issued[0].dimension(),region.branch(),region.cards(),region.wings()));
                });game.waitTicks(3);game.takeScreenshot("hoi-mission-region-assigned-"+(navy?"navy":"air")+"-"+size[0]+"x"+size[1]+"-gui2");
                game.runOnClient(client->{
                    var q=queries.getLast();MapCommanderAvailability.accept(new Assessment(q.token(),q.revision(),q.sequence(),q.request(),Map.of(action,new Decision(false,false,"장비 조건 불충족"))));
                });game.waitTicks(2);
                game.runOnClient(client->{int before=commands.size();click(icon);click(icon);if(commands.size()!=before||MapCommanderAvailability.reason(action).isEmpty())throw new AssertionError("Authoritative equipment rejection must block repeated mission clicks");var q=queries.getLast();MapCommanderAvailability.accept(new Assessment(q.token(),q.revision(),q.sequence(),q.request()-1,Map.of(action,new Decision(true,false,""))));if(MapCommanderAvailability.reason(action).isEmpty())throw new AssertionError("Stale availability cannot clear a current denial");});
                var hit=game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals(icon)).findFirst().orElseThrow());game.getInput().setCursorPos((hit.x()+hit.w()/2.)*2,(hit.y()+hit.h()/2.)*2);game.waitTicks(2);game.takeScreenshot("hoi-mission-disabled-contract-"+(navy?"navy":"air")+"-"+size[0]+"x"+size[1]+"-gui2");
                game.runOnClient(client->{var q=queries.getLast();MapCommanderAvailability.accept(new Assessment(q.token(),q.revision(),q.sequence(),q.request(),Map.of(action,new Decision(true,true,"","지역을 지정하세요.",true))));if(!MapCommanderAvailability.reason(action).isEmpty())throw new AssertionError("Enabled server warning must not disable a mission");click(icon);if(commands.size()!=3)throw new AssertionError("Enabled warning must permit mission assignment");});
                game.setScreen(()->null);
            }
        }finally{game.runOnClient(client->{MapCommanderAvailability.queryObserver=null;MapCommanderOrders.commandObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderOrders.reset();MapCommanderHud.reset();MapUnitHud.reset();});}
    }
    private static String navalActivity(Member member){return member.data().navy().activityIcon();}
    private static void click(String icon){var hit=MapCommanderPanels.hits().stream().filter(h->h.icon().equals(icon)).findFirst().orElseThrow();MapCommanderHud.clickAt(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.);}
}
