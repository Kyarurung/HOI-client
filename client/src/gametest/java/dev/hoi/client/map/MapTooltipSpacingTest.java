package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.client.ui.UiTooltipSpacingTest;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class MapTooltipSpacingTest {
    private MapTooltipSpacingTest() {}
    public static void run(ClientGameTestContext game,ProtocolMenuView menu) {
        try {
            Path directory=Path.of(Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW")));
            var source=new Snapshot(Files.readString(directory.resolve("kor-command-panel-navy.json"))).view();
            var actions=new ProtocolCommanderOrders.Availability(Files.readString(directory.resolve("kor-navy-assessment.json"))).assessment().actions();
            String detail=actions.get("NAVY/MINELAYING").details();
            UiTooltipSpacingTest.verify(game,detail);
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(client->{
                    client.options.guiScale().set(2);ClientCampaignHud.accept(ProtocolHudPayloads.State.of(source.country(),menu.hud()));MapCommanderHud.requestObserver=null;MapCommanderHud.reset();MapUnitHud.reset();
                    MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));
                    MapCommanderHud.open(new Open(Mode.NAVAL_TASK_FORCES,source.cards().stream().filter(card->card.role()==Mode.NAVY_FLEET&&card.panel()!=null).findFirst().orElseThrow().id()));
                    MapCommanderOrders.selectionObserver=selection->{};
                    MapCommanderAvailability.queryObserver=query->MapCommanderAvailability.accept(new ProtocolCommanderOrders.Assessment(query.token(),query.revision(),query.sequence(),query.request(),actions));
                    MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(UUID.randomUUID().toString(),0,source.country(),client.level.dimension().identifier().toString(),""));
                });
                game.setScreen(ScreenMapCursor::new);game.waitTicks(3);
                for(String mission:List.of("PATROL","MINELAYING","HOLD")) {
                    var point=game.computeOnClient(client->{var hit=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("naval_mission_"+mission)).findFirst().orElseThrow();if(!hit.tip().equals(MapCommanderAvailability.tooltip("NAVY/"+mission,"")))throw new AssertionError("Mission must keep source detail and its authoritative disabled reason");return new int[]{hit.x()+hit.w()/2,hit.y()+hit.h()/2};});
                    game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);
                    game.runOnClient(client->{if(!MapCommanderHud.renderedTooltip().equals(MapCommanderAvailability.tooltip("NAVY/"+mission,"")))throw new AssertionError("Mission hover did not retain the issued source");var rendered=new StringBuilder();for(var line:dev.hoi.client.ui.UiTooltips.sourceLines(client.font,MapCommanderHud.renderedTooltip(),324))line.accept((i,s,c)->{rendered.appendCodePoint(c);return true;});if(rendered.toString().contains("단축키")||rendered.toString().contains("£"))throw new AssertionError("Mission tooltip leaked shortcut labels or tokens");});
                    var image=game.takeScreenshot("hoi-spacing-"+mission.toLowerCase(Locale.ROOT)+"-"+size[0]+"x"+size[1]+"-gui2");
                    var evidence=game.computeOnClient(client->Map.of("source",MapCommanderHud.renderedTooltip(),"bounds",MapCommanderHud.tooltipBounds(),"cursor",List.of(point[0]*2,point[1]*2)));
                    Files.writeString(image.resolveSibling(image.getFileName()+".json"),new com.google.gson.Gson().toJson(evidence));
                }
            }
            var army=new Snapshot(Files.readString(directory.resolve("kor-command-panel-army-assigned.json"))).view();
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(client->{MapCommanderAvailability.queryObserver=null;MapCommanderHud.reset();MapUnitHud.reset();MapCommanderOrders.reset();client.options.guiScale().set(2);ClientCampaignHud.accept(ProtocolHudPayloads.State.of(army.country(),menu.hud()));MapCommanderHud.accept(new View(army.country(),client.level.dimension().identifier().toString(),army.branch(),army.cards(),army.wings(),army.running(),army.theaters(),army.armyTheaters()));});
                game.setScreen(ScreenMapCursor::new);game.waitTicks(3);
                game.runOnClient(client->{var box=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();MapCommanderHud.clickAt(box.x()+box.width()/2.,box.y()+box.height()/2.);});game.waitTicks(3);
                var point=game.computeOnClient(client->{var hit=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("bpt_order_assign_units_btn")).findFirst().orElseThrow();if(hit.tip().contains("Ctrl"))throw new AssertionError("Assignment tooltip retained keyboard hint");return new int[]{hit.x()+hit.w()/2,hit.y()+hit.h()/2};});
                game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);game.takeScreenshot("hoi-shortcuts-army-assign-"+size[0]+"x"+size[1]+"-gui2");
            }
        } catch(java.io.IOException error) {throw new java.io.UncheckedIOException(error);}
        finally {game.runOnClient(client->{MapCommanderAvailability.queryObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderHud.reset();MapUnitHud.reset();MapCommanderOrders.reset();});game.setScreen(()->null);}
    }
}
