package dev.hoi.client.map.commander;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import dev.hoi.client.ui.tooltip.TooltipSpacingTest;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class CommanderTooltipSpacingTest {
    private CommanderTooltipSpacingTest() {}
    public static void run(ClientGameTestContext game,MenuView menu) {
        try {
            Path directory=Path.of(Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW")));
            var source=new Snapshot(Files.readString(directory.resolve("kor-command-panel-navy.json"))).view();
            var actions=new CommanderOrderPayloads.Availability(Files.readString(directory.resolve("kor-navy-assessment.json"))).assessment().actions();
            String detail=actions.get("NAVY/MINELAYING").details();
            TooltipSpacingTest.verify(game,TooltipDelay.summary(detail));TooltipSpacingTest.verify(game,TooltipDelay.full(detail));
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(client->{
                    client.options.guiScale().set(2);ClientCampaignHud.accept(HudPayloads.State.of(source.country(),menu.hud()));CommanderHud.requestObserver=null;CommanderHud.reset();CommanderUnitHud.reset();
                    CommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));
                    CommanderHud.open(new Open(Mode.NAVAL_TASK_FORCES,source.cards().stream().filter(card->card.role()==Mode.NAVY_FLEET&&card.panel()!=null).findFirst().orElseThrow().id()));
                    CommanderOrders.selectionObserver=selection->{};
                    CommanderAvailability.queryObserver=query->CommanderAvailability.accept(new CommanderOrderPayloads.Assessment(query.token(),query.revision(),query.sequence(),query.request(),actions));
                    CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(),0,source.country(),client.level.dimension().identifier().toString(),""));
                });
                game.setScreen(MapCursor::new);game.waitTicks(3);
                for(String mission:List.of("PATROL","MINELAYING","HOLD")) {
                    var point=game.computeOnClient(client->{var hit=CommanderPanels.hits().stream().filter(h->h.icon().equals("naval_mission_"+mission)).findFirst().orElseThrow();if(!hit.tip().equals(CommanderAvailability.tooltip("NAVY/"+mission,"")))throw new AssertionError("Mission must keep source detail and its authoritative disabled reason");return new int[]{hit.x()+hit.w()/2,hit.y()+hit.h()/2};});
                    game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);
                    game.runOnClient(client->{if(!CommanderHud.renderedTooltip().equals(TooltipDelay.summary(CommanderAvailability.tooltip("NAVY/"+mission,""))))throw new AssertionError("Mission hover must start with its summary");});
                    for(int step=0;step<12;step++){try{Thread.sleep(TooltipDelay.DELAY/10);}catch(InterruptedException error){Thread.currentThread().interrupt();throw new AssertionError(error);}game.waitTicks(1);}
                    game.runOnClient(client->{if(!CommanderHud.renderedTooltip().equals(TooltipDelay.full(CommanderAvailability.tooltip("NAVY/"+mission,""))))throw new AssertionError("Mission hover did not retain the issued source");var rendered=new StringBuilder();for(var line:dev.hoi.client.ui.tooltip.Tooltips.sourceLines(client.font,CommanderHud.renderedTooltip(),324))line.accept((i,s,c)->{rendered.appendCodePoint(c);return true;});if(rendered.toString().contains("단축키")||rendered.toString().contains("£"))throw new AssertionError("Mission tooltip leaked shortcut labels or tokens");});
                    var image=game.takeScreenshot("hoi-spacing-"+mission.toLowerCase(Locale.ROOT)+"-"+size[0]+"x"+size[1]+"-gui2");
                    var evidence=game.computeOnClient(client->Map.of("source",CommanderHud.renderedTooltip(),"bounds",CommanderHud.tooltipBounds(),"cursor",List.of(point[0]*2,point[1]*2)));
                    Files.writeString(image.resolveSibling(image.getFileName()+".json"),new com.google.gson.Gson().toJson(evidence));
                }
            }
            var army=new Snapshot(Files.readString(directory.resolve("kor-command-panel-army-assigned.json"))).view();
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(client->{CommanderAvailability.queryObserver=null;CommanderHud.reset();CommanderUnitHud.reset();CommanderOrders.reset();client.options.guiScale().set(2);ClientCampaignHud.accept(HudPayloads.State.of(army.country(),menu.hud()));CommanderHud.accept(new View(army.country(),client.level.dimension().identifier().toString(),army.branch(),army.cards(),army.wings(),army.running(),army.theaters(),army.armyTheaters()));});
                game.setScreen(MapCursor::new);game.waitTicks(3);
                game.runOnClient(client->{var box=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();CommanderHud.clickAt(box.x()+box.width()/2.,box.y()+box.height()/2.);});game.waitTicks(3);
                var point=game.computeOnClient(client->{var hit=CommanderPanels.hits().stream().filter(h->h.icon().equals("bpt_order_assign_units_btn")).findFirst().orElseThrow();if(hit.tip().contains("Ctrl"))throw new AssertionError("Assignment tooltip retained keyboard hint");return new int[]{hit.x()+hit.w()/2,hit.y()+hit.h()/2};});
                game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);game.takeScreenshot("hoi-shortcuts-army-assign-"+size[0]+"x"+size[1]+"-gui2");
            }
        } catch(java.io.IOException error) {throw new java.io.UncheckedIOException(error);}
        finally {game.runOnClient(client->{CommanderAvailability.queryObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.reset();CommanderUnitHud.reset();CommanderOrders.reset();});game.setScreen(()->null);}
    }
}
