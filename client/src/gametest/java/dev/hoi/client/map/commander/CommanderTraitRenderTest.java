package dev.hoi.client.map.commander;

import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class CommanderTraitRenderTest {
    static void run(ClientGameTestContext game,int[] size,View source){
        var json=com.google.gson.JsonParser.parseString(Snapshot.of(source).json()).getAsJsonObject();
        for(var entry:json.getAsJsonArray("cards")){
            var card=entry.getAsJsonObject();boolean group=card.get("role").getAsString().equals("ARMY_GROUP");
            for(var value:card.getAsJsonObject("panel").getAsJsonObject("leader").getAsJsonArray("traitDetails")){
                var trait=value.getAsJsonObject();String icon=trait.get("icon").getAsString();boolean inactive=!group&&Set.of("defensive_doctrine","unyielding_defender","organisational_leader").contains(icon.substring(icon.lastIndexOf('/')+1));
                trait.addProperty("active",!inactive);
                if(inactive)trait.addProperty("tooltip",trait.get("tooltip").getAsString()+"\n\n§R군을 지휘하는 중에는 특성이 활성화되지 않습니다.§!");
            }
        }
        var view=new Snapshot(json.toString()).view();var army=view.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&c.panel().leader().traitDetails().stream().filter(t->Boolean.FALSE.equals(t.active())).count()==2).findFirst().orElseThrow();
        game.runOnClient(client->{CommanderHud.reset();CommanderUnitHud.reset();CommanderHud.accept(view);CommanderPanels.select(army.id(),false);});game.waitTicks(2);game.getInput().setCursorPos(size[0]/2.,size[1]/2.);game.waitTicks(2);
        var image=game.takeScreenshot("hoi-army-traits-inactive-synthetic-"+size[0]+"x"+size[1]+"-gui2");
        var hits=game.computeOnClient(client->CommanderPanels.hits().stream().filter(h->h.icon().equals("army_trait")).toList());
        if(!hits.stream().map(CommanderPanels.Hit::tip).toList().equals(army.panel().leader().traitDetails().stream().map(dev.hoi.protocol.dialog.DialogView.LeaderTrait::tooltip).toList()))throw new AssertionError("Inactive trait tooltip must be preserved exactly");
        try{var pixels=javax.imageio.ImageIO.read(image.toFile());for(int n=0;n<hits.size();n++){var hit=hits.get(n);boolean inactive=Boolean.FALSE.equals(army.panel().leader().traitDetails().get(n).active());int red=0;for(int y=hit.y()*2;y<(hit.y()+hit.h())*2;y++)for(int x=hit.x()*2;x<(hit.x()+hit.w())*2;x++){int rgb=pixels.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;if(r>40&&r>g*1.7&&r>b*1.7)red++;}if(inactive&&red<30||!inactive&&red>10)throw new AssertionError("Only inactive marshal traits may have a red mask: "+n+" "+red);}}catch(java.io.IOException error){throw new AssertionError(error);}
        var inactive=hits.get(1);game.getInput().setCursorPos((inactive.x()+inactive.w()/2.)*2,(inactive.y()+inactive.h()/2.)*2);game.waitTicks(16);game.runOnClient(client->{if(!CommanderHud.renderedTooltip().equals(inactive.tip()))throw new AssertionError("Inactive trait hover must retain the server warning");});game.takeScreenshot("hoi-army-traits-inactive-tooltip-synthetic-"+size[0]+"x"+size[1]+"-gui2");
        CommanderDragSelectionTest.run(game,size,view);
        var group=view.cards().stream().filter(c->c.role()==Mode.ARMY_GROUP).findFirst().orElseThrow();game.runOnClient(client->{CommanderPanels.select(group.id(),false);});game.waitTicks(2);game.getInput().setCursorPos(size[0]/2.,size[1]/2.);game.waitTicks(2);game.takeScreenshot("hoi-marshal-traits-active-synthetic-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{CommanderHud.accept(source);CommanderPanels.select(group.id(),false);});game.waitTicks(2);
    }
}
