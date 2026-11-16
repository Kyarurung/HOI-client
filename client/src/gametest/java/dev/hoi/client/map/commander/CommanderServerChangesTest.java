package dev.hoi.client.map.commander;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenServerChangesTest;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import dev.hoi.protocol.commander.CommanderOrderPayloads;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.hud.HudPayloads;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class CommanderServerChangesTest {
    private CommanderServerChangesTest() {}
    public static void run(ClientGameTestContext game,int[] size){
        try{for(String stage:List.of("ready","active","cooldown","force-only")){
            var view=ScreenServerChangesTest.source(stage.equals("force-only")?"prc-force-attack-focus-only":"prc-abilities-"+stage,CommanderHudPayloads.View.class);var hud=ScreenServerChangesTest.source("prc-hud-"+stage,CountryHud.class);var card=view.cards().stream().filter(c->c.panel()!=null&&c.panel().abilities().contains(stage.equals("force-only")?"PRC_force_attack":"super_force_attack")).findFirst().orElseThrow();
            if(card.panel().abilities().contains("requisition_winter_gear"))throw new AssertionError("Original has_ability must not be fabricated");
            game.runOnClient(c->{CommanderHud.reset();CommanderUnitHud.reset();ClientCampaignHud.accept(HudPayloads.State.of("PRC",hud));CommanderOrders.selectionObserver=x->{};CommanderOrders.commandObserver=x->{};CommanderHud.accept(new CommanderHudPayloads.View(view.country(),c.level.dimension().identifier().toString(),view.branch(),view.cards(),view.wings(),view.running(),view.theaters(),view.armyTheaters(),view.orderToken(),view.orderRevision()));CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(),7,"PRC",c.level.dimension().identifier().toString(),""));CommanderPanels.select(card.id(),false);});
            game.setScreen(MapCursor::new);game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);ScreenServerChangesTest.capture(game,"abilities-"+stage,size);
            for(String id:stage.equals("force-only")?List.of("PRC_force_attack"):List.of("super_force_attack","tactic_gas_bombardment")){
                var detail=card.panel().abilityDetails().stream().filter(a->a.id().equals(id)).findFirst().orElseThrow();ScreenServerChangesTest.clean(detail.tooltip());var hit=game.computeOnClient(c->{var value=CommanderPanels.hits().stream().filter(h->h.tip().equals(UiCommandLabels.withoutShortcuts(detail.tooltip()))).findFirst().orElseThrow();if(value.icon().equals("ability_icon_default")||dev.hoi.client.ui.UiAssets.imageSize("commander/commands/"+value.icon()+"/frame_0")[0]<=0)throw new AssertionError("Original ability sprite must be applied: "+id);return value;});
                game.getInput().setCursorPos((hit.x()+hit.w()/2.)*2,(hit.y()+hit.h()/2.)*2);game.waitTicks(3);game.runOnClient(c->{if(!CommanderHud.renderedTooltip().equals(UiCommandLabels.withoutShortcuts(hit.tip())))throw new AssertionError("Ability hover differs from authoritative server tooltip: "+id);});ScreenServerChangesTest.capture(game,"ability-"+id+"-"+stage+"-hover",size);
            }
        }}finally{game.runOnClient(c->{CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.reset();CommanderUnitHud.reset();});}
    }
}
