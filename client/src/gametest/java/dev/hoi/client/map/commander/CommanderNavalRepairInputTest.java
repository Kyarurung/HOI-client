package dev.hoi.client.map.commander;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.*;
import static dev.hoi.protocol.commander.CommanderHudPayloads.*;
import static dev.hoi.protocol.commander.CommanderOrderPayloads.*;

final class CommanderNavalRepairInputTest {
    private CommanderNavalRepairInputTest() {}
    private static View source(String directory,int policy)throws java.io.IOException{
        var json=JsonParser.parseString(Files.readString(Path.of(directory,"kor-command-panel-navy.json"))).getAsJsonObject();
        for(var card:json.getAsJsonArray("cards")){var panel=card.getAsJsonObject().getAsJsonObject("panel");if(panel==null)continue;for(var member:panel.getAsJsonArray("members"))member.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("navy").addProperty("repairPolicy",policy);}
        return new Snapshot(json.toString()).view();
    }
    private static CommanderPanels.Hit repair(int policy){String title=UiCommandLabels.text("NAVY_REPAIR_POLICY_"+policy,"missing");return CommanderPanels.hits().stream().filter(h->h.tip().startsWith(title)).findFirst().orElseThrow();}
    private static void click(net.minecraft.client.Minecraft client,CommanderPanels.Hit hit,int button){if(!client.gui.screen().mouseClicked(new MouseButtonEvent(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.,new MouseButtonInfo(button,0)),false))throw new AssertionError("Repair policy click must be consumed by the actual screen");}
    static void run(ClientGameTestContext game,MenuView menu,String directory)throws java.io.IOException{
        var initial=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-navy.json"))).view();var updated=source(directory,1);
        if(initial.cards().getFirst().panel().members().stream().anyMatch(m->!Integer.valueOf(0).equals(m.data().navy().repairPolicy())))throw new AssertionError("Server fixture must publish default repair policy zero");
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            var commands=new ArrayList<Command>();var dialogs=new ArrayList<Open>();var context=new Context[1];game.getInput().resizeWindow(size[0],size[1]);
            game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));CommanderOrders.reset();CommanderHud.reset();CommanderUnitHud.reset();CommanderOrders.commandObserver=commands::add;CommanderOrders.selectionObserver=ignored->{};CommanderHud.requestObserver=dialogs::add;CommanderHud.accept(new View(initial.country(),client.level.dimension().identifier().toString(),initial.branch(),initial.cards(),initial.wings()));CommanderPanels.select(initial.cards().getFirst().id(),false);context[0]=new Context(UUID.randomUUID().toString(),10,"KOR",client.level.dimension().identifier().toString(),"");CommanderOrders.accept(context[0]);});
            game.setScreen(MapCursor::new);game.waitTicks(2);
            game.runOnClient(client->{var hit=repair(0);if(!hit.tip().contains(UiCommandLabels.text("NAVY_REPAIR_POLICY_CLICK_TO_CHANGE_TO","").replace("$LEVEL|$",UiCommandLabels.text("NAVY_REPAIR_POLICY_1","").split("\n",2)[0])))throw new AssertionError("Repair tooltip must show the actual next policy");click(client,hit,InputConstants.MOUSE_BUTTON_LEFT);click(client,hit,InputConstants.MOUSE_BUTTON_LEFT);click(client,hit,InputConstants.MOUSE_BUTTON_RIGHT);if(commands.size()!=1||!commands.getFirst().action().equals("NAVAL_SETTING/REPAIR_NEXT")||commands.getFirst().selection().size()!=2||!commands.getFirst().selection().get(1).equals("policy/0")||!dialogs.isEmpty())throw new AssertionError("Repair clicks must issue one exact direct command without opening a modal while awaiting acknowledgement");repair(0);});
            game.takeScreenshot("hoi-naval-repair-next-pending-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{CommanderHud.accept(new View(updated.country(),client.level.dimension().identifier().toString(),updated.branch(),updated.cards(),updated.wings()));CommanderOrders.accept(new Context(context[0].token(),11,context[0].country(),context[0].dimension(),""));});game.waitTicks(2);
            game.runOnClient(client->{click(client,repair(1),InputConstants.MOUSE_BUTTON_RIGHT);if(commands.size()!=2||!commands.getLast().action().equals("NAVAL_SETTING/REPAIR_PREVIOUS")||!commands.getLast().selection().equals(List.of(commands.getFirst().selection().getFirst(),"policy/1"))||!dialogs.isEmpty())throw new AssertionError("Right-click must use the refreshed policy and same task force only");});
            game.takeScreenshot("hoi-naval-repair-previous-pending-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.requestObserver=null;CommanderOrders.reset();CommanderHud.reset();CommanderUnitHud.reset();});
        }
    }
}
