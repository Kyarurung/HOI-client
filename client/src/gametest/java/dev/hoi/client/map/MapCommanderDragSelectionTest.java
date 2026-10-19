package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class MapCommanderDragSelectionTest {
    static void run(ClientGameTestContext game,int[] size,View view){
        var armies=view.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY).toList();
        var group=view.cards().stream().filter(c->c.role()==Mode.ARMY_GROUP).findFirst().orElseThrow();
        var rows=armies.stream().flatMap(c->c.panel().members().stream()).toList();
        game.runOnClient(client->{MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(view);});game.waitTicks(2);
        game.runOnClient(client->hidden());capture(game,size,"none");
        drag(game,view,rows);game.runOnClient(client->stack(4));capture(game,size,"same-group");
        game.runOnClient(client->{var portraits=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("army_portrait")).toList();if(!portraits.getFirst().tip().equals(group.detail())&&!portraits.getFirst().tip().contains(group.leader()))throw new AssertionError("Shared parent must precede the selected armies");for(int n=1;n<portraits.size();n++)if(portraits.get(n).y()-portraits.get(n-1).y()!=52)throw new AssertionError("Compact headers must use their measured contiguous spacing");var close=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("nv_btn_close")).skip(1).findFirst().orElseThrow();MapCommanderHud.clickAt(close.x()+close.w()/2.,close.y()+close.h()/2.);for(String id:armies.getFirst().panel().divisionIds())if(MapUnitHud.selectedKeys().contains("unit/"+id))throw new AssertionError("Army X must remove its selected divisions");});game.waitTicks(2);game.runOnClient(client->stack(3));
        game.runOnClient(client->{var close=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("nv_btn_close")).findFirst().orElseThrow();MapCommanderHud.clickAt(close.x()+close.w()/2.,close.y()+close.h()/2.);if(!MapUnitHud.selectedKeys().isEmpty())throw new AssertionError("Group X must remove all remaining child armies");});game.waitTicks(2);game.runOnClient(client->hidden());
        var json=com.google.gson.JsonParser.parseString(Snapshot.of(view).json()).getAsJsonObject();
        for(var entry:json.getAsJsonArray("cards")){var card=entry.getAsJsonObject();if(card.get("id").getAsString().equals(armies.getFirst().id()))card.getAsJsonObject("panel").addProperty("parent","");}
        var detached=new Snapshot(json.toString()).view();drag(game,detached,rows);game.runOnClient(client->stack(3));capture(game,size,"mixed-groups");
        drag(game,view,armies.getFirst().panel().members());game.runOnClient(client->{hidden();if(MapCommanderPanels.hits().stream().filter(h->h.icon().equals("army_portrait")).count()!=1||MapCommanderPanels.hits().stream().noneMatch(h->h.icon().equals("deploy_hq_button")))throw new AssertionError("One dragged army retains its full panel");});capture(game,size,"single-army");
        game.runOnClient(client->{MapUnitHud.selectDivisions(rows.stream().map(Member::id).toList(),false);});game.waitTicks(2);game.runOnClient(client->{hidden();if(MapCommanderPanels.hits().stream().anyMatch(h->h.icon().equals("army_portrait")))throw new AssertionError("Direct selection must not enter the drag-only stack");});
        game.runOnClient(client->{MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(view);MapCommanderPanels.select(group.id(),false);});game.waitTicks(2);
    }
    private static void drag(ClientGameTestContext game,View view,List<Member> rows){
        game.runOnClient(client->{MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(view);var camera=client.gameRenderer.mainCamera();var center=camera.position().add(new net.minecraft.world.phys.Vec3(camera.forwardVector()).scale(12)).subtract(new net.minecraft.world.phys.Vec3(camera.leftVector()).scale(3));var counter=new dev.hoi.protocol.ProtocolUnitHud.Counter("cmp39-selection","KOR","대한민국","infantry","선택 검사",rows.size(),center.x,center.y-3.1,center.z,1,1,1,Map.of());MapUnitHud.accept(new dev.hoi.protocol.ProtocolUnitHud.Snapshot(view.dimension(),List.of(counter)));});game.waitTicks(2);
        game.runOnClient(client->{var box=MapUnitHud.boxes().getFirst();var screen=client.gui.screen();var start=mouse(box.x()-3,box.y()-3);var end=mouse(box.x()+box.width()+3,box.y()+box.height()+3);if(!screen.mouseClicked(start,false)||!screen.mouseDragged(end,end.x()-start.x(),end.y()-start.y())||!screen.mouseReleased(end))throw new AssertionError("Map drag must pass through actual screen input");MapCommanderPanels.acceptDivisionDetails(rows);});game.waitTicks(2);
    }
    private static MouseButtonEvent mouse(double x,double y){return new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private static void hidden(){if(MapCommanderPanels.armyCommandVisible()||MapCommanderPanels.hits().stream().anyMatch(h->h.icon().startsWith("ability_")||h.icon().startsWith("bpt_")))throw new AssertionError("Non-card selection must hide command and ability hits");if(MapCommanderOrders.confirmDraft())throw new AssertionError("Hidden command bar must not accept confirmation shortcuts");}
    private static void stack(int count){hidden();if(MapCommanderPanels.hits().stream().filter(h->h.icon().equals("army_portrait")).count()!=count)throw new AssertionError("Wrong drag panel count: "+count);if(MapCommanderPanels.hits().stream().anyMatch(h->h.icon().equals("deploy_hq_button")||h.icon().equals("change_template_button")||h.tip().contains("Ctrl+클릭")))throw new AssertionError("Drag stack must contain only commander headers and summaries");}
    private static void capture(ClientGameTestContext game,int[] size,String name){game.getInput().setCursorPos(size[0]/2.,size[1]/2.);game.waitTicks(2);game.takeScreenshot("hoi-drag-selection-"+name+"-"+size[0]+"x"+size[1]+"-gui2");}
}
