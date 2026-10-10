package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import dev.hoi.protocol.ProtocolCommanderOrders.Command;
import dev.hoi.protocol.ProtocolCommanderOrders.Context;
import dev.hoi.protocol.ProtocolCommanderOrders.Selection;
import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class MapHierarchySelectionTest {
    private MapHierarchySelectionTest() {}
    private static Card card(String id,Mode role,String parent,List<String> divisions){
        var rows=role==Mode.ARMY_GROUP?List.<Member>of():divisions.stream().limit(1).map(d->new Member(d,d,"대기 중","unit_hud/infantry","",1,new Open(Mode.ARMY_UNITS,id))).toList();
        return new Card(id,id,"","politics/empty/leader","","",divisions.size(),18,"",role,new Panel(parent,"",null,rows,divisions.size(),List.of(),divisions));
    }
    private static View view(String dimension,List<String> a,List<String> b){var group=new ArrayList<>(a);group.addAll(b);return new View("KOR",dimension,Mode.ARMY,List.of(card("G",Mode.ARMY_GROUP,"",group),card("A",Mode.ASSIGN_ARMY,"G",a),card("B",Mode.ASSIGN_ARMY,"G",b),card("OTHER",Mode.ASSIGN_ARMY,"",List.of("outside"))));}
    private static void selected(String... ids){var expected=Arrays.stream(ids).map(id->"unit/"+id).collect(java.util.stream.Collectors.toSet());if(!MapUnitHud.selectedKeys().equals(expected)||!new HashSet<>(MapCommanderPanels.commandSelection()).equals(expected))throw new AssertionError("Counter selection and direct command must exactly match "+expected+" but were "+MapCommanderPanels.commandSelection());}
    private static void click(String id){var box=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals(id)&&(b.request().mode()==Mode.ASSIGN_ARMY||b.request().mode()==Mode.ASSIGN_GROUP_MARSHAL)).findFirst().orElseThrow();if(!MapCommanderHud.clickAt(box.x()+box.width()/2.,box.y()+box.height()/2.))throw new AssertionError("Actual hierarchy portrait must accept click");}
    private static void move(List<Command> commands,Context issued,long revision,String... ids){
        var screen=net.minecraft.client.Minecraft.getInstance().gui.screen();double x=screen.width*.85,y=screen.height*.4;int before=commands.size();var event=new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT,0));screen.mouseClicked(event,false);screen.mouseReleased(event);
        if(commands.size()!=before+1||!commands.getLast().action().equals("MAP")||!new HashSet<>(commands.getLast().selection()).equals(Arrays.stream(ids).map(id->"unit/"+id).collect(java.util.stream.Collectors.toSet())))throw new AssertionError("Hierarchy right-click must issue exactly its selected division union");
        MapCommanderOrders.accept(new Context(issued.token(),revision,"KOR",issued.dimension(),""));
    }
    static void run(ClientGameTestContext game,ProtocolMenuView menu){
        try{for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            var commands=new ArrayList<Command>();var selections=new ArrayList<Selection>();var issued=new Context[1];var initial=new View[1];game.getInput().resizeWindow(size[0],size[1]);
            game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderHud.reset();MapUnitHud.reset();MapCommanderOrders.reset();MapCommanderHud.requestObserver=null;MapCommanderOrders.commandObserver=commands::add;MapCommanderOrders.selectionObserver=selections::add;initial[0]=view(client.level.dimension().identifier().toString(),List.of("a1","a2","a3"),List.of("b1","b2"));MapCommanderHud.accept(initial[0]);issued[0]=new Context(UUID.randomUUID().toString(),0,"KOR",initial[0].dimension(),"");MapCommanderOrders.accept(issued[0]);client.player.setXRot(60);client.player.setYRot(0);});
            game.setScreen(ScreenMapCursor::new);game.waitTicks(2);
            game.runOnClient(client->{selected();if(MapCommanderHud.boxes().stream().anyMatch(b->b.request()!=null&&Set.of("A","B").contains(b.request().target())))throw new AssertionError("Unselected group children must begin collapsed");click("G");selected("a1","a2","a3","b1","b2");});game.waitTicks(2);
            game.takeScreenshot("hoi-hierarchy-group-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{move(commands,issued[0],1,"a1","a2","a3","b1","b2");click("A");selected("a1","a2","a3");});game.waitTicks(2);
            game.takeScreenshot("hoi-hierarchy-army-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{move(commands,issued[0],2,"a1","a2","a3");var row=MapCommanderPanels.hits().stream().filter(h->h.tip().startsWith("a1\n")).findFirst().orElseThrow();MapCommanderHud.clickAt(row.x()+row.w()/2.,row.y()+row.h()/2.);selected("a1");MapCommanderHud.accept(initial[0]);selected("a1");move(commands,issued[0],3,"a1");});game.waitTicks(2);
            game.takeScreenshot("hoi-hierarchy-subset-"+size[0]+"x"+size[1]+"-gui2");
            game.getInput().holdKey(o->o.keyShift);game.runOnClient(client->{click("B");selected("a1","b1","b2");});game.getInput().releaseKey(o->o.keyShift);game.waitTicks(2);game.runOnClient(client->{MapCommanderHud.accept(initial[0]);selected("a1","b1","b2");if(!new HashSet<>(selections.getLast().keys()).equals(MapUnitHud.selectedKeys()))throw new AssertionError("Adding a full Army to a partial selection must publish only that mixed exact union");MapCommanderPanels.select("B",true);selected("a1");});
            game.runOnClient(client->{MapCommanderPanels.select("A",false);MapCommanderPanels.select("B",true);});game.waitTicks(2);
            game.runOnClient(client->{var close=MapCommanderPanels.hits().stream().filter(h->h.tip().equals("닫기")&&h.x()<client.gui.screen().width/3).findFirst().orElseThrow();MapCommanderHud.clickAt(close.x()+close.w()/2.,close.y()+close.h()/2.);selected("b1","b2");MapCommanderPanels.select("A",false);MapCommanderPanels.select("B",true);MapUnitHud.selectDivisions(List.of("a1","b1"),false);});game.waitTicks(2);
            game.runOnClient(client->{var close=MapCommanderPanels.hits().stream().filter(h->h.tip().equals("선택 해제")&&h.x()<client.gui.screen().width/3).findFirst().orElseThrow();MapCommanderHud.clickAt(close.x()+close.w()/2.,close.y()+close.h()/2.);selected();MapCommanderPanels.select("A",false);});
            game.runOnClient(client->{
                MapCommanderPanels.select("B",true);selected("a1","a2","a3","b1","b2");move(commands,issued[0],4,"a1","a2","a3","b1","b2");MapCommanderPanels.select("A",true);selected("b1","b2");MapCommanderPanels.select("B",true);selected();
                MapCommanderPanels.select("G",false);MapCommanderHud.accept(view(initial[0].dimension(),List.of("a2","a3","a4"),List.of("b1")));selected("a2","a3","a4","b1");move(commands,issued[0],5,"a2","a3","a4","b1");
                MapUnitHud.selectDivisions(List.of("a2","a3"),false);MapCommanderHud.accept(view(initial[0].dimension(),List.of("a2","a4","a5"),List.of("b1","b2")));selected("a2");MapUnitHud.clearSelection();MapCommanderOrders.selectionChanged();MapCommanderHud.accept(initial[0]);selected();
                MapCommanderPanels.select("G",false);MapCommanderOrders.accept(new Context(issued[0].token(),6,"KOR",issued[0].dimension(),"LAND/FRONT"));int before=commands.size();MapUnitHud.selectDivisions(List.of("a1"),false);if(commands.size()!=before+1||!commands.getLast().action().equals("CANCEL"))throw new AssertionError("Manual subset cancels the previous pending hierarchy target once");MapCommanderOrders.accept(new Context(issued[0].token(),7,"KOR",issued[0].dimension(),""));selected("a1");
                MapCommanderPanels.select("G",false);var full=initial[0].cards().getFirst();MapCommanderHud.accept(new View("KOR",initial[0].dimension(),Mode.ARMY,List.of(full)));selected("a1","a2","a3","b1","b2");move(commands,issued[0],8,"a1","a2","a3","b1","b2");
                var incomplete=new Card(full.id(),full.name(),full.leader(),full.portrait(),full.marshal(),full.marshalPortrait(),full.count(),full.capacity(),full.detail(),full.role(),new Panel("","",null,List.of(),5,List.of()));MapCommanderHud.accept(new View("KOR",initial[0].dimension(),Mode.ARMY,List.of(incomplete)));selected();MapCommanderHud.accept(initial[0]);selected("a1","a2","a3","b1","b2");
                MapUnitHud.selectDivisions(List.of("a1"),false);MapCommanderHud.accept(new View("KOR",initial[0].dimension(),Mode.ARMY,List.of(incomplete)));selected("a1");MapCommanderHud.accept(initial[0]);selected("a1");
                MapCommanderPanels.select("B",true);selected("a1","b1","b2");var partialCards=new ArrayList<>(initial[0].cards());var b=partialCards.get(2);partialCards.set(2,new Card(b.id(),b.name(),b.leader(),b.portrait(),b.marshal(),b.marshalPortrait(),b.count(),b.capacity(),b.detail(),b.role(),new Panel("G","",null,List.of(),2,List.of())));MapCommanderHud.accept(new View("KOR",initial[0].dimension(),Mode.ARMY,partialCards));selected();MapUnitHud.accept(new ProtocolUnitHud.Snapshot(initial[0].dimension(),List.of()));selected();MapCommanderHud.accept(initial[0]);selected("a1","b1","b2");
                MapCommanderHud.closeSelection();selected();if(!selections.getLast().keys().isEmpty())throw new AssertionError("Scope clearing must be published to remove private movement arrows");MapCommanderHud.accept(initial[0]);selected();MapCommanderPanels.select("A",false);MapCommanderHud.accept(new View("KOR",initial[0].dimension(),Mode.NAVY_HQ,List.of()));selected();if(MapCommanderOrders.ready())throw new AssertionError("Branch transition must retire the command context");MapCommanderHud.accept(initial[0]);selected();
                MapCommanderPanels.select("A",false);ClientCampaignHud.accept(ProtocolHudPayloads.State.of("PRK",menu.hud()));MapCommanderHud.accept(initial[0]);selected();
            });
            formation(game,menu,size);
        }}finally{game.runOnClient(client->{MapCommanderOrders.commandObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderOrders.reset();MapCommanderHud.reset();MapUnitHud.reset();ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));});}
    }
    private static void formation(ClientGameTestContext game,ProtocolMenuView menu,int[] size){
        var commands=new ArrayList<Command>();var issued=new Context[1];var source=new View[1];
        game.runOnClient(client->{ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderHud.reset();MapUnitHud.reset();MapCommanderOrders.reset();MapCommanderHud.requestObserver=null;MapCommanderOrders.commandObserver=commands::add;MapCommanderOrders.selectionObserver=s->{};source[0]=view(client.level.dimension().identifier().toString(),List.of("a1","a2","a3"),List.of("b1","b2"));MapCommanderHud.accept(source[0]);issued[0]=new Context(UUID.randomUUID().toString(),0,"KOR",source[0].dimension(),"");MapCommanderOrders.accept(issued[0]);MapCommanderPanels.select("G",false);MapUnitHud.selectDivisions(List.of("a1"),false);});game.waitTicks(2);
        game.runOnClient(client->{formationClick(Mode.ASSIGN_ARMY,"B",commands,issued[0],"MOVE_DIVISIONS",List.of("target/B","unit/a1"));selected("a1");MapCommanderHud.accept(source[0]);selected("a1");if(!MapCommanderHud.countLabel(source[0].cards().get(1)).equals("3"))throw new AssertionError("An unassigned commander card shows only the actual division count");});
        game.runOnClient(client->{MapCommanderPanels.select("A",false);MapCommanderPanels.select("B",true);});game.waitTicks(2);
        game.runOnClient(client->{formationClick(Mode.ASSIGN_GROUP_MARSHAL,"G",commands,issued[0],"MOVE_ARMIES",List.of("target/G","A","B"));selected("a1","a2","a3","b1","b2");});game.waitTicks(2);
        game.takeScreenshot("hoi-direct-existing-group-white-counts-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{formationClick(Mode.ARMY_GROUPS,"",commands,issued[0],"CREATE_GROUP",List.of("A","B"));MapUnitHud.selectDivisions(List.of("a1","b1"),false);});game.waitTicks(2);
        game.runOnClient(client->{formationClick(Mode.CREATE_ARMY,"",commands,issued[0],"CREATE_ARMY",List.of("unit/a1","unit/b1"));selected("a1","b1");if(!MapCommanderPanels.selectedDivisionRows().stream().map(Member::id).collect(java.util.stream.Collectors.toSet()).equals(Set.of("a1","b1")))throw new AssertionError("The manual selection panel must contain exactly the selected divisions across armies");MapCommanderHud.closeSelection();});game.waitTicks(2);
        game.runOnClient(client->{int count=commands.size();var box=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.CREATE_ARMY).findFirst().orElseThrow();MapCommanderHud.clickAt(box.x()+box.width()/2.,box.y()+box.height()/2.,InputConstants.MOUSE_BUTTON_RIGHT,false);if(commands.size()!=count)throw new AssertionError("Empty formation selection must not submit a mutation");MapCommanderHud.accept(new View("KOR",source[0].dimension(),Mode.AIR,List.of()));if(MapCommanderOrders.route(new Open(Mode.CREATE_ARMY,"",List.of("unit/a1")))||MapCommanderOrders.route(new Open(Mode.ARMY_GROUPS,"")))throw new AssertionError("Army direct formation must preserve existing non-army dialog routes");});
    }
    private static void formationClick(Mode mode,String target,List<Command> commands,Context issued,String action,List<String> keys){
        var client=net.minecraft.client.Minecraft.getInstance();var screen=client.gui.screen();var box=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==mode&&b.request().target().equals(target)).findFirst().orElseThrow();int before=commands.size();var event=new MouseButtonEvent(box.x()+box.width()/2.,box.y()+box.height()/2.,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT,0));screen.mouseClicked(event,false);screen.mouseReleased(event);
        if(commands.size()!=before+1||!commands.getLast().action().equals("FORMATION/"+action)||!commands.getLast().selection().equals(keys)||client.gui.screen()!=screen)throw new AssertionError("Direct formation must send only "+action+" "+keys+" without an intermediate dialog: "+commands);
        MapCommanderOrders.accept(new Context(issued.token(),commands.getLast().revision()+1,"KOR",issued.dimension(),""));
    }
}
