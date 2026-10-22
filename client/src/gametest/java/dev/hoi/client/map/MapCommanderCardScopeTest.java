package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import dev.hoi.protocol.ProtocolCommanderOrders.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.*;

public final class MapCommanderCardScopeTest {
    private MapCommanderCardScopeTest() {}
    private static void clickCard(String id,boolean footer){
        var boxes=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_ARMY&&b.request().target().equals(id)).toList();var box=footer?boxes.getLast():boxes.getFirst();
        var screen=net.minecraft.client.Minecraft.getInstance().gui.screen();var event=new MouseButtonEvent(box.x()+box.width()/2.,box.y()+box.height()/2.,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        if(!screen.mouseClicked(event,false))throw new AssertionError("Army card click was not consumed");screen.mouseReleased(event);
    }
    private static void accept(ClientGameTestContext game,View source){game.runOnClient(client->MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true,source.theaters(),source.armyTheaters())));}
    private static void availability(ClientGameTestContext game,View source,int[] size){
        var queries=new ArrayList<Check>();var commands=new ArrayList<Command>();String denied="선택한 비행단의 기종과 무장으로는 수행할 수 없는 임무입니다.";
        var first=source.wings().getFirst();var second=source.wings().stream().filter(wing->!wing.icon().equals(first.icon())).findFirst().orElseThrow();
        try{
            for(boolean mixed:List.of(false,true)){
                var ids=mixed?List.of(first.id(),second.id()):List.of(first.id());
                game.runOnClient(client->{MapCommanderOrders.reset();MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.requestObserver=null;MapCommanderAvailability.queryObserver=queries::add;MapCommanderOrders.selectionObserver=ignored->{};MapCommanderOrders.commandObserver=commands::add;MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true,source.theaters(),source.armyTheaters()));MapCommanderOrders.accept(new Context(UUID.randomUUID().toString(),1,"KOR",client.level.dimension().identifier().toString(),""));MapCommanderPanels.showWings(ids);var q=queries.getLast();MapCommanderAvailability.accept(new Assessment(q.token(),q.revision(),q.sequence(),q.request(),Map.of("AIR/SUPERIORITY",new Decision(true,true,""),"AIR/CLOSE_AIR_SUPPORT",new Decision(mixed,mixed,mixed?"":denied),"AIR/NAVAL_STRIKE",new Decision(false,false,denied))));commands.clear();});
                game.getInput().setCursorPos(size[0]-30,90);game.waitTicks(2);game.takeScreenshot("hoi-air-availability-"+(mixed?"mixed":"single")+"-"+size[0]+"x"+size[1]+"-gui2");
                var disabled=game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("air_mission_NAVAL_STRIKE")).findFirst().orElseThrow());
                game.runOnClient(client->{if(!disabled.tip().contains(denied))throw new AssertionError("Disabled mission must retain the server reason");MapCommanderPanels.click(disabled.x()+1,disabled.y()+1);MapCommanderPanels.click(disabled.x()+1,disabled.y()+1);if(!commands.isEmpty())throw new AssertionError("Disabled repeated mission clicks must not send");});
                game.getInput().setCursorPos((disabled.x()+disabled.w()/2.)*2,(disabled.y()+disabled.h()/2.)*2);game.waitTicks(2);game.takeScreenshot("hoi-air-availability-reason-"+(mixed?"mixed":"single")+"-"+size[0]+"x"+size[1]+"-gui2");
                game.runOnClient(client->{var enabled=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("air_mission_SUPERIORITY")).findFirst().orElseThrow();MapCommanderPanels.click(enabled.x()+1,enabled.y()+1,InputConstants.MOUSE_BUTTON_RIGHT,false);if(!commands.isEmpty())throw new AssertionError("Right mission click must not send");MapCommanderPanels.click(enabled.x()+1,enabled.y()+1);MapCommanderPanels.click(enabled.x()+1,enabled.y()+1);if(commands.size()!=1||!commands.getFirst().action().equals("AIR/SUPERIORITY")||!commands.getFirst().selection().equals(ids))throw new AssertionError("Enabled mission must send the complete exact selection once");var context=MapCommanderOrders.currentContext();MapCommanderOrders.accept(new Context(context.token(),2,"KOR",context.dimension(),"AIR/SUPERIORITY"));if(!MapCommanderOrders.pending().equals("AIR/SUPERIORITY"))throw new AssertionError("Missing region must wait for server-directed airspace selection");if(!MapCommanderOrders.map(List.of(new Ray(0,-1,0)))||!commands.getLast().action().equals("MAP")||!commands.getLast().selection().equals(MapCommanderPanels.commandSelection()))throw new AssertionError("Airspace target must retain the selected wing scope");});
            }
        }finally{game.runOnClient(client->{MapCommanderAvailability.queryObserver=null;MapCommanderOrders.commandObserver=null;MapCommanderOrders.reset();MapCommanderHud.reset();MapUnitHud.reset();});}
    }
    public static void run(ClientGameTestContext game,ProtocolMenuView menu,String directory) throws java.io.IOException {
        var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-air-diagnostics.json"))).view();
        var attached=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-air-attached.json"))).view();
        var detached=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-air-detached.json"))).view();
        var card=source.cards().getFirst();var ids=source.wings().stream().filter(w->w.data()!=null&&w.data().attachedArmy().equals(card.id())).map(Member::id).toList();
        if(ids.isEmpty()||ids.size()==source.wings().size())throw new AssertionError("Source must include attached and unrelated wings");
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            var commands=new ArrayList<Command>();var opens=new ArrayList<Open>();game.getInput().resizeWindow(size[0],size[1]);
            game.runOnClient(client->{client.options.guiScale().set(2);MapCommanderHud.reset();MapUnitHud.reset();ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderHud.requestObserver=opens::add;MapCommanderOrders.selectionObserver=ignored->{};});
            accept(game,source);game.runOnClient(client->{for(var wing:source.wings()){var data=wing.data();var details=data.air().wing();if(data.experienceLevel()==null||data.experienceProgress()==null||details.missionState().isBlank()||details.efficiency()==null)throw new AssertionError("Updated authoritative air display fields are required");if(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+details.equipmentIcon()+".png")).isEmpty())throw new AssertionError("Authoritative equipment image is missing: "+details.equipmentIcon());}});game.setScreen(ScreenMapCursor::new);game.getInput().setCursorPos(size[0]-30,90);game.waitTicks(2);
            game.runOnClient(client->{if(MapCommanderPanels.hits().stream().anyMatch(h->!h.icon().startsWith("theater_"))||!MapCommanderPanels.commandSelection().isEmpty())throw new AssertionError("Unselected air view must not expose global lists or missions");});game.takeScreenshot("hoi-air-wings-unselected-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->MapCommanderPanels.showWings(source.wings().stream().limit(2).map(Member::id).toList()));game.waitTicks(2);game.runOnClient(client->{var count=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("air_selected_count")).findFirst().orElseThrow();var add=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("add_to_air_group")).findFirst().orElseThrow();var merge=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("nv_btn_merge")).findFirst().orElseThrow();var right=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("nv_btn_base_partial_toggle")).findFirst().orElseThrow();if(count.x()+count.w()>add.x()||merge.x()+merge.w()>right.x())throw new AssertionError("Full selection text and left buttons must fit before the fixed right toolbar");});game.takeScreenshot("hoi-air-wings-selected-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{var unsupported=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("air_mission_unsupported")).findFirst().orElseThrow();MapCommanderPanels.click(unsupported.x()+1,unsupported.y()+1);if(!opens.isEmpty())throw new AssertionError("Unsupported air mission must not send an action");MapCommanderPanels.closeSelection();clickCard(card.id(),false);});game.waitTicks(2);
            game.runOnClient(client->{
                if(!MapCommanderPanels.attachmentWings().equals(ids))throw new AssertionError("Army card must select only attached wings");
                if(MapCommanderPanels.hits().stream().anyMatch(h->h.icon().startsWith("bpt_")))throw new AssertionError("Air card must not expose army command strip");
                clickCard(card.id(),true);if(!MapCommanderPanels.attachmentWings().equals(ids)||!opens.isEmpty())throw new AssertionError("Card footer click must preserve local wing selection");
            });game.waitTicks(2);game.takeScreenshot("hoi-air-card-attached-"+size[0]+"x"+size[1]+"-gui2");
            var attach=game.computeOnClient(client->{var targets=MapCommanderHud.boxes().stream().filter(b->b.tip().equals(UiCommandLabels.text("ATTACH_AIR_WING_DESC",""))).toList();if(targets.isEmpty())throw new AssertionError("Empty category slots must retain attachment controls without a fourth box");return targets.getFirst();});
            game.getInput().setCursorPos((attach.x()+attach.width()/2.)*2,(attach.y()+attach.height()/2.)*2);game.waitTicks(2);game.takeScreenshot("hoi-air-attach-tooltip-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{MapCommanderOrders.commandObserver=commands::add;MapCommanderOrders.accept(new Context(UUID.randomUUID().toString(),1,"KOR",client.level.dimension().identifier().toString(),""));var expected=new ArrayList<String>();expected.add(card.id());expected.addAll(MapCommanderPanels.commandSelection());MapCommanderHud.clickAt(attach.x()+1,attach.y()+1,InputConstants.MOUSE_BUTTON_LEFT,false);MapCommanderHud.clickAt(attach.x()+1,attach.y()+1,InputConstants.MOUSE_BUTTON_LEFT,false);if(!opens.isEmpty()||commands.size()!=1||!commands.getFirst().action().equals("AIR_ATTACH")||!commands.getFirst().selection().equals(expected)||!commands.getFirst().rays().isEmpty()||!(client.gui.screen() instanceof ScreenMapCursor))throw new AssertionError("Attach button must directly send the exact published wings once without a dialog");var current=MapCommanderOrders.currentContext();MapCommanderOrders.accept(new Context(current.token(),2,"KOR",current.dimension(),""));commands.clear();});
            var detach=game.computeOnClient(client->MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.AIR_ATTACH&&b.request().selection().contains("action/detach")).findFirst().orElseThrow());
            game.getInput().setCursorPos((detach.x()+detach.width()/2.)*2,(detach.y()+detach.height()/2.)*2);game.waitTicks(2);game.takeScreenshot("hoi-air-attached-tooltip-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{MapCommanderHud.clickAt(detach.x()+1,detach.y()+1,InputConstants.MOUSE_BUTTON_RIGHT,false);MapCommanderHud.clickAt(detach.x()+1,detach.y()+1,InputConstants.MOUSE_BUTTON_RIGHT,false);var expected=detach.request().selection().stream().filter(id->!id.equals("action/detach")).map(id->"air/"+id).toList();if(!opens.isEmpty()||commands.size()!=1||!commands.getFirst().action().equals("AIR_DETACH")||!commands.getFirst().selection().equals(expected)||!commands.getFirst().rays().isEmpty()||!(client.gui.screen() instanceof ScreenMapCursor))throw new AssertionError("Right click must directly detach only the named wings once without a dialog");MapCommanderOrders.commandObserver=null;MapCommanderOrders.reset();});game.getInput().setCursorPos(size[0]-30,90);
            game.runOnClient(client->{
                var mission=MapCommanderPanels.hits().stream().filter(h->h.tip().startsWith(UiCommandLabels.withoutShortcuts(UiCommandLabels.text("AIRWING_MISSION_TYPE_AIR_SUPERIORITY","")))).findFirst().orElseThrow();
                MapCommanderPanels.click(mission.x()+1,mission.y()+1,InputConstants.MOUSE_BUTTON_RIGHT,false);if(!opens.isEmpty())throw new AssertionError("Mission right click must not issue a request");
                MapCommanderPanels.click(mission.x()+1,mission.y()+1);if(opens.size()!=1||opens.getFirst().mode()!=Mode.AIR_TOOLBAR||!opens.getFirst().selection().subList(1,opens.getFirst().selection().size()).equals(ids))throw new AssertionError("Air mission leaked outside the selected army");
                MapCommanderPanels.closeSelection();
            });game.waitTicks(2);
            game.runOnClient(client->{if(MapCommanderPanels.hits().stream().anyMatch(h->!h.icon().startsWith("theater_"))||!MapCommanderPanels.commandSelection().isEmpty())throw new AssertionError("Deselection must remove air missions and rows");});
            game.takeScreenshot("hoi-air-card-unselected-"+size[0]+"x"+size[1]+"-gui2");
            accept(game,attached);game.waitTicks(2);game.runOnClient(client->clickCard(attached.cards().getFirst().id(),false));game.waitTicks(2);
            game.runOnClient(client->{if(MapCommanderPanels.attachmentWings().isEmpty())throw new AssertionError("Attachment fixture must select a wing");});
            accept(game,detached);game.waitTicks(2);game.runOnClient(client->{if(!MapCommanderPanels.attachmentWings().isEmpty()||MapCommanderPanels.hits().stream().anyMatch(h->!h.icon().startsWith("theater_")))throw new AssertionError("Snapshot detachment must remove stale rows and commands");});
            availability(game,source,size);
            accept(game,new View(source.country(),source.dimension(),Mode.ARMY,source.cards(),source.wings(),false,List.of(),List.of()));game.waitTicks(2);game.runOnClient(client->{clickCard(card.id(),false);if(MapCommanderHud.boxes().stream().anyMatch(b->b.request()!=null&&b.request().mode()==Mode.AIR_ATTACH))throw new AssertionError("Default map must not expose attached air slots");});game.waitTicks(2);game.runOnClient(client->{if(MapCommanderPanels.hits().stream().anyMatch(h->h.icon().equals("army_select_wings_btn")))throw new AssertionError("Default map must not expose attached air header control");});game.takeScreenshot("hoi-default-map-attached-wings-hidden-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{MapCommanderHud.requestObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderHud.reset();MapUnitHud.reset();});
        }
    }
}
