package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
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
    private static void accept(ClientGameTestContext game,View source){game.runOnClient(client->MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true)));}
    public static void run(ClientGameTestContext game,ProtocolMenuView menu,String directory) throws java.io.IOException {
        var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-air-diagnostics.json"))).view();
        var attached=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-air-attached.json"))).view();
        var detached=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-air-detached.json"))).view();
        var card=source.cards().getFirst();var ids=source.wings().stream().filter(w->w.data()!=null&&w.data().attachedArmy().equals(card.id())).map(Member::id).toList();
        if(ids.isEmpty()||ids.size()==source.wings().size())throw new AssertionError("Source must include attached and unrelated wings");
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            var opens=new ArrayList<Open>();game.getInput().resizeWindow(size[0],size[1]);
            game.runOnClient(client->{client.options.guiScale().set(2);MapCommanderHud.reset();MapUnitHud.reset();ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderHud.requestObserver=opens::add;MapCommanderOrders.selectionObserver=ignored->{};});
            accept(game,source);game.setScreen(ScreenMapCursor::new);game.getInput().setCursorPos(size[0]-30,90);game.waitTicks(2);
            game.runOnClient(client->{if(!MapCommanderPanels.hits().isEmpty()||!MapCommanderPanels.commandSelection().isEmpty())throw new AssertionError("Unselected air view must not expose global lists or missions");clickCard(card.id(),false);});game.waitTicks(2);
            game.runOnClient(client->{
                if(!MapCommanderPanels.attachmentWings().equals(ids))throw new AssertionError("Army card must select only attached wings");
                if(MapCommanderPanels.hits().stream().anyMatch(h->h.icon().startsWith("bpt_")))throw new AssertionError("Air card must not expose army command strip");
                clickCard(card.id(),true);if(!MapCommanderPanels.attachmentWings().equals(ids)||!opens.isEmpty())throw new AssertionError("Card footer click must preserve local wing selection");
            });game.waitTicks(2);game.takeScreenshot("hoi-air-card-attached-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{
                var mission=MapCommanderPanels.hits().stream().filter(h->h.tip().startsWith(UiCommandLabels.withoutShortcuts(UiCommandLabels.text("AIRWING_MISSION_TYPE_AIR_SUPERIORITY","")))).findFirst().orElseThrow();
                MapCommanderPanels.click(mission.x()+1,mission.y()+1,InputConstants.MOUSE_BUTTON_RIGHT,false);if(!opens.isEmpty())throw new AssertionError("Mission right click must not issue a request");
                MapCommanderPanels.click(mission.x()+1,mission.y()+1);if(opens.size()!=1||opens.getFirst().mode()!=Mode.AIR_TOOLBAR||!opens.getFirst().selection().subList(1,opens.getFirst().selection().size()).equals(ids))throw new AssertionError("Air mission leaked outside the selected army");
                MapCommanderPanels.closeSelection();
            });game.waitTicks(2);
            game.runOnClient(client->{if(!MapCommanderPanels.hits().isEmpty()||!MapCommanderPanels.commandSelection().isEmpty())throw new AssertionError("Deselection must remove air missions and rows");});
            game.takeScreenshot("hoi-air-card-unselected-"+size[0]+"x"+size[1]+"-gui2");
            accept(game,attached);game.waitTicks(2);game.runOnClient(client->clickCard(attached.cards().getFirst().id(),false));game.waitTicks(2);
            game.runOnClient(client->{if(MapCommanderPanels.attachmentWings().isEmpty())throw new AssertionError("Attachment fixture must select a wing");});
            accept(game,detached);game.waitTicks(2);game.runOnClient(client->{if(!MapCommanderPanels.attachmentWings().isEmpty()||!MapCommanderPanels.hits().isEmpty())throw new AssertionError("Snapshot detachment must remove stale rows and commands");});
            game.runOnClient(client->{MapCommanderHud.requestObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderHud.reset();MapUnitHud.reset();});
        }
    }
}
