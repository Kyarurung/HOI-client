package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.commander.CommanderOrderPayloads;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class CommanderContractRenderTest {
    static void run(ClientGameTestContext game,int[] size,View view){
        game.runOnClient(client->{String icons=dev.hoi.client.ui.UiKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal("£unit_infantry_icon_medium£ £unit_hud_headquarters£"),false).getString();if(icons.chars().filter(c->c>=0xe000&&c<=0xf8ff).count()!=2)throw new AssertionError("Pack must load both source unit tooltip glyphs within its resource bound");});
        var commands=new ArrayList<CommanderOrderPayloads.Command>();var requests=new ArrayList<Open>();
        var group=view.cards().stream().filter(c->c.role()==Mode.ARMY_GROUP).findFirst().orElseThrow();var army=view.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();
        game.runOnClient(client->{CommanderHud.reset();CommanderUnitHud.reset();CommanderHud.accept(view);CommanderHud.requestObserver=requests::add;CommanderOrders.commandObserver=commands::add;CommanderOrders.selectionObserver=s->{};revision(view);CommanderPanels.select(group.id(),false);});game.waitTicks(2);
        game.runOnClient(client->{var button=hit("army_group_disband");click(button,InputConstants.MOUSE_BUTTON_RIGHT);if(!commands.isEmpty()||!requests.isEmpty())throw new AssertionError("Group dissolution right click must do nothing");click(button,InputConstants.MOUSE_BUTTON_LEFT);click(button,InputConstants.MOUSE_BUTTON_LEFT);check(commands,"FORMATION/DISSOLVE_GROUP",List.of(group.id()));if(!requests.isEmpty())throw new AssertionError("Group dissolution must not open a confirmation");revision(view);commands.clear();});
        game.getInput().holdKey(o->o.keyShift);game.runOnClient(client->click(hit("army_group_disband"),InputConstants.MOUSE_BUTTON_LEFT));game.getInput().releaseKey(o->o.keyShift);
        game.runOnClient(client->{check(commands,"FORMATION/DISSOLVE_GROUP_ALL",List.of(group.id()));commands.clear();revision(view);click(hit("theater_settings"),InputConstants.MOUSE_BUTTON_LEFT);});game.waitTicks(2);
        capture(game,size,"settings-normal");
        game.runOnClient(client->{for(String level:List.of("HIGH","LOW","NORMAL")){var button=hit("theater_priority_"+level.toLowerCase(Locale.ROOT));click(button,InputConstants.MOUSE_BUTTON_RIGHT);if(!commands.isEmpty())throw new AssertionError("Priority right click must not issue commands");click(button,InputConstants.MOUSE_BUTTON_LEFT);click(button,InputConstants.MOUSE_BUTTON_LEFT);check(commands,"FORMATION/THEATER_PRIORITY/"+level,List.of(view.theaters().getFirst().id()));commands.clear();revision(view);}click(hit("theater_disband"),InputConstants.MOUSE_BUTTON_LEFT);if(!requests.isEmpty())throw new AssertionError("Only theater cannot be dissolved");});
        for(String level:List.of("LOW","HIGH","MIXED")){var response=priorityResponse(view,level);game.runOnClient(client->CommanderHud.accept(response));game.waitTicks(2);capture(game,size,"settings-"+level.toLowerCase(Locale.ROOT)+"-response");}
        game.runOnClient(client->CommanderHud.accept(view));game.waitTicks(2);
        game.runOnClient(client->click(hit("theater_settings_close"),InputConstants.MOUSE_BUTTON_LEFT));game.waitTicks(2);
        game.runOnClient(client->{if(CommanderPanels.hits().stream().anyMatch(h->h.icon().equals("theater_priority_high")))throw new AssertionError("Settings close must remove priority hits");CommanderPanels.select(army.id(),false);});game.waitTicks(2);
        var badge=game.computeOnClient(client->CommanderPanels.hits().stream().filter(h->h.icon().equals("theater_card")&&h.tip().equals(CommanderHud.cardTooltip(army))).findFirst().orElseThrow());
        game.runOnClient(client->{click(badge,InputConstants.MOUSE_BUTTON_RIGHT);var expected=new ArrayList<String>();expected.add("target/"+army.id());army.panel().divisionIds().stream().map(id->"unit/"+id).sorted().forEach(expected::add);check(commands,"FORMATION/MOVE_DIVISIONS",expected);commands.clear();revision(view);});
        game.getInput().setCursorPos((badge.x()+badge.w()/2.)*2,(badge.y()+badge.h()/2.)*2);game.waitTicks(2);
        game.runOnClient(client->{var bounds=CommanderHud.tooltipBounds();if(!CommanderHud.renderedTooltip().equals(badge.tip())||bounds==null||bounds.x()<0||bounds.y()<0||bounds.x()+bounds.width()>client.getWindow().getGuiScaledWidth()||bounds.y()+bounds.height()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Theater row tooltip must fit GUI2 and match its card");});game.takeScreenshot("hoi-army-contract-theater-tooltip-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{CommanderHud.requestObserver=null;CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;});
    }
    private static View priorityResponse(View view,String level){var theaters=view.theaters().stream().map(t->new Theater(t.id(),t.name(),t.divisions(),t.groups(),t.armies(),t.warnings(),t.controls().stream().map(c->c.id().startsWith("THEATER_SETTINGS/")?new Control("THEATER_SETTINGS/"+level,c.icon(),c.tooltip(),c.enabled(),c.request()):c).toList())).toList();return new View(view.country(),view.dimension(),view.branch(),view.cards(),view.wings(),view.running(),theaters);}
    static void state(ClientGameTestContext game,int[] size,View view,String name){
        var planned=view.cards().stream().filter(c->c.panel().controls().stream().anyMatch(k->Set.of("ACTIVATE","DEACTIVATE").contains(k.id()))).findFirst().orElseThrow();
        var other=view.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&!c.id().equals(planned.id())).findFirst().orElseThrow();
        game.runOnClient(client->{CommanderHud.reset();CommanderUnitHud.reset();CommanderHud.accept(view);CommanderPanels.select(other.id(),false);});game.waitTicks(2);
        game.runOnClient(client->{var execution=CommanderHud.boxes().stream().filter(b->b.request()!=null&&Set.of("ACTIVATE","DEACTIVATE","STOP_EXERCISE").contains(b.request().target())).toList();if(name.equals("defending")){if(!execution.isEmpty())throw new AssertionError("Defending marker must replace both controls");var marker=CommanderHud.boxes().stream().filter(b->b.tip().equals(planned.panel().controls().stream().filter(c->c.id().equals("DEFENDING_AREA")).findFirst().orElseThrow().tooltip())).findFirst().orElseThrow();if(marker.request()!=null)throw new AssertionError("Defense marker must never open AREA_STATES");}else{if(execution.isEmpty()||execution.stream().anyMatch(b->!b.request().selection().equals(List.of(planned.id()))))throw new AssertionError("Unselected card must use only its own orders");if(name.equals("training")&&execution.stream().noneMatch(b->b.request().target().equals("STOP_EXERCISE")))throw new AssertionError("Training must show stop training");}});
        capture(game,size,name+"-unselected");
        if(name.equals("orders")){
            var gson=new com.google.gson.Gson();var json=gson.toJsonTree(view).getAsJsonObject();String groupId="";
            for(var entry:json.getAsJsonArray("cards")){var card=entry.getAsJsonObject();if(card.get("id").getAsString().equals(planned.id()))card.getAsJsonObject("panel").addProperty("planEvaluation",0);if(!card.get("role").getAsString().equals("ARMY_GROUP"))continue;groupId=card.get("id").getAsString();var panel=card.getAsJsonObject("panel");for(var control:planned.panel().controls())if(Set.of("ACTIVATE","DEACTIVATE").contains(control.id()))panel.getAsJsonArray("controls").add(gson.toJsonTree(control));panel.addProperty("planEvaluation",2);}
            var synthetic=gson.fromJson(json,View.class);String group=groupId;game.runOnClient(client->{CommanderHud.accept(synthetic);CommanderPanels.select(group,false);});game.waitTicks(2);
            game.runOnClient(client->{for(String id:List.of(group,planned.id()))if(CommanderHud.boxes().stream().noneMatch(b->b.request()!=null&&b.request().target().equals("ACTIVATE")&&b.request().selection().equals(List.of(id))))throw new AssertionError("Army and synthetic group execution controls must retain their own target");});capture(game,size,"orders-army-and-group-synthetic");
        }
    }
    private static void check(List<CommanderOrderPayloads.Command> commands,String action,List<String> selection){if(commands.size()!=1||!commands.getFirst().action().equals(action)||!commands.getFirst().selection().equals(selection)||!commands.getFirst().rays().isEmpty())throw new AssertionError("Unexpected request "+commands+" expected "+action+selection);}
    private static void revision(View view){var previous=CommanderOrders.currentContext();CommanderOrders.accept(new CommanderOrderPayloads.Context(previous==null?UUID.randomUUID().toString():previous.token(),previous==null?0:previous.revision()+1,view.country(),view.dimension(),""));}
    private static CommanderPanels.Hit hit(String icon){return CommanderPanels.hits().stream().filter(h->h.icon().equals(icon)).findFirst().orElseThrow(()->new AssertionError("Missing "+icon));}
    private static void click(CommanderPanels.Hit hit,int button){CommanderHud.clickAt(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.,button,false);}
    private static void capture(ClientGameTestContext game,int[] size,String name){game.getInput().setCursorPos(size[0]/2.,size[1]/2.);game.waitTicks(2);game.takeScreenshot("hoi-army-contract-"+name+"-"+size[0]+"x"+size[1]+"-gui2");}
}
