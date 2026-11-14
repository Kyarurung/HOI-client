package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.*;

public final class CommanderPresentationTest {
    private CommanderPresentationTest() {}
    private static boolean before(){return "1".equals(System.getenv("HOI_CAPTURE_BEFORE"));}
    private static MouseButtonEvent mouse(double x,double y){return new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private static View view(Card card){var context=CommanderOrders.currentContext();return new View(context.country(),context.dimension(),Mode.ARMY,List.of(card),List.of(),null,null,null,context.token(),context.revision());}
    public static void run(ClientGameTestContext game,MenuView menu) {
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        var gson=new com.google.gson.Gson();
        try {
            var source=new Snapshot(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-command-panel-army-assigned.json"))).view();
            var assigned=source.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&c.panel()!=null&&!c.panel().commander().isEmpty()).findFirst().orElseThrow();
            var json=gson.toJsonTree(assigned).getAsJsonObject();json.addProperty("portrait","politics/empty/leader");json.addProperty("leader","");
            var panel=json.getAsJsonObject("panel");panel.addProperty("commander","");panel.addProperty("parent","");panel.remove("leader");
            var unassigned=gson.fromJson(json,Card.class);
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);
                var commands=new ArrayList<CommanderOrderPayloads.Command>();var requests=new ArrayList<Open>();
                game.runOnClient(c->{c.options.guiScale().set(2);ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));CommanderHud.reset();CommanderUnitHud.reset();CommanderHud.requestObserver=requests::add;CommanderOrders.commandObserver=commands::add;CommanderOrders.selectionObserver=s->{};CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(),0,"KOR",c.level.dimension().identifier().toString(),""));CommanderHud.accept(view(unassigned));});
                game.setScreen(MapCursor::new);game.getInput().setCursorPos(size[0]-4,80);game.waitTicks(2);
                game.runOnClient(c->{
                    var portrait=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals(unassigned.id())&&b.request().mode()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();
                    if(!before())for(var control:unassigned.panel().controls())if(control.id().equals("LEADER_TRAITS")&&CommanderHud.boxes().stream().anyMatch(b->b.action()!=null&&b.tip().equals(control.tooltip())&&b.x()>=portrait.x()&&b.x()<portrait.x()+portrait.width()))throw new AssertionError("Unassigned portrait exposes leader controls");
                    var plus=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.CREATE_ARMY).findFirst().orElseThrow();
                    if(!before()&&(plus.x()+plus.width()<=portrait.x()+portrait.width()||plus.y()!=portrait.y()||plus.width()!=portrait.width()))throw new AssertionError("Creation portrait must reuse army card dimensions");
                });
                game.takeScreenshot("hoi-presentation-unassigned-synthetic-"+size[0]+"-gui2");
                game.runOnClient(c->CommanderPanels.select(unassigned.id(),false));game.waitTicks(2);
                game.runOnClient(c->{if(!before()&&CommanderPanels.hits().stream().anyMatch(h->h.icon().equals("army_leader_details")||h.icon().equals("army_leader_advisor")))throw new AssertionError("An unassigned army panel cannot expose commander controls from a stale panel");});
                game.takeScreenshot("hoi-presentation-unassigned-panel-synthetic-"+size[0]+"-gui2");
                game.runOnClient(c->CommanderHud.accept(view(assigned)));
                game.waitTicks(2);game.takeScreenshot("hoi-presentation-assigned-border-"+size[0]+"-gui2");
                game.runOnClient(c->CommanderHud.accept(view(unassigned)));
                game.waitTicks(2);
                var origin=new double[3];
                game.runOnClient(c->{var card=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals(unassigned.id())&&b.request().mode()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();origin[0]=card.x();origin[1]=card.width();origin[2]=card.y()+card.height()/2.;var from=mouse(card.x()+card.width()/2.,origin[2]);c.gui.screen().mouseClicked(from,false);c.gui.screen().mouseDragged(mouse(from.x()+1000,from.y()),1000,0);});game.waitTicks(2);
                game.runOnClient(c->{var moved=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals(unassigned.id())&&b.request().mode()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();if(!before()&&(moved.x()<=origin[0]||moved.x()>origin[0]+origin[1]))throw new AssertionError("Army must follow the mouse into the plus slot within one card width");});
                game.takeScreenshot("hoi-presentation-plus-dragging-synthetic-"+size[0]+"-gui2");
                game.runOnClient(c->{c.gui.screen().mouseReleased(mouse(origin[0]+1000,origin[2]));if(!commands.isEmpty()||!requests.isEmpty())throw new AssertionError("Last army dropped on plus must not create or reparent an army");CommanderOrders.message("Campaign overlay probe");});game.waitTicks(2);
                game.takeScreenshot("hoi-presentation-messages-hidden-synthetic-"+size[0]+"-gui2");
                var groupJson=gson.toJsonTree(unassigned).getAsJsonObject();groupJson.addProperty("role","ARMY_GROUP");groupJson.addProperty("count",0);var groupPanel=groupJson.getAsJsonObject("panel");groupPanel.addProperty("total",0);groupPanel.add("members",new com.google.gson.JsonArray());groupPanel.add("divisionIds",new com.google.gson.JsonArray());var emptyGroup=gson.fromJson(groupJson,Card.class);
                game.runOnClient(c->{CommanderHud.accept(view(emptyGroup));CommanderPanels.select(emptyGroup.id(),false);});game.waitTicks(2);
                game.runOnClient(c->{if(!CommanderUnitHud.selectedKeys().isEmpty())throw new AssertionError("Empty group fixture has selected divisions");if(!before()&&CommanderPanels.hits().stream().anyMatch(h->h.icon().equals("army_portrait")))throw new AssertionError("Zero selected divisions must close the upper-left army panel");});
                game.takeScreenshot("hoi-presentation-zero-divisions-synthetic-"+size[0]+"-gui2");
            }
        } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        finally{game.runOnClient(c->{c.options.guiScale().set(previous);CommanderHud.requestObserver=null;CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.reset();CommanderUnitHud.reset();c.gui.hud.setOverlayMessage(net.minecraft.network.chat.Component.empty(),false);});game.setScreen(()->null);}
    }
}
