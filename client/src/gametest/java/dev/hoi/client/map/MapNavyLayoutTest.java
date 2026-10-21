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

public final class MapNavyLayoutTest {
    private MapNavyLayoutTest() {}
    private static MouseButtonEvent mouse(double x,double y,int button){return new MouseButtonEvent(x,y,new MouseButtonInfo(button,0));}
    private static MapCommanderPanels.Hit hit(String tip){return MapCommanderPanels.hits().stream().filter(h->h.tip().equals(tip)).findFirst().orElseThrow(()->new AssertionError("Missing navy control: "+tip));}
    private static void click(ClientGameTestContext game,MapCommanderPanels.Hit hit,int button){game.runOnClient(client->{var e=mouse(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.,button);if(!client.gui.screen().mouseClicked(e,false))throw new AssertionError("Navy click was not consumed");client.gui.screen().mouseReleased(e);});}
    private static MapCommanderHud.Box portrait(String id){return MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.NAVY_FLEET&&b.request().target().equals(id)).findFirst().orElseThrow();}
    public static void run(ClientGameTestContext game,ProtocolMenuView menu){
        String directory=Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW"),"Authoritative navy fixtures are required");
        try{
            var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-navy.json"))).view();
            var fleets=source.cards().stream().filter(c->(c.role()==Mode.NAVY_FLEET||c.role()==null)&&c.panel()!=null&&!c.panel().members().isEmpty()).toList();
            if(fleets.size()<2||fleets.getFirst().panel().members().size()<2)throw new AssertionError("Navy fixture needs two fleets and multiple task forces");
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                var opens=new ArrayList<Open>();var commands=new ArrayList<ProtocolCommanderOrders.Command>();var fleet=fleets.getFirst();var force=fleet.panel().members().getFirst();
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true));MapCommanderHud.requestObserver=opens::add;MapCommanderOrders.commandObserver=commands::add;MapCommanderOrders.selectionObserver=ignored->{};MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(UUID.randomUUID().toString(),1,source.country(),client.level.dimension().identifier().toString(),""));MapCommanderPanels.select(fleet.id(),false);});
                game.setScreen(ScreenMapCursor::new);game.getInput().setCursorPos(size[0]-30,90);game.waitTicks(3);
                game.runOnClient(client->{
                    MapCommanderPanelsTest.assertCardLayout(new View(source.country(),source.dimension(),source.branch(),source.cards(),source.wings(),true),client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());
                    var select=hit("선택 · "+force.name());var close=hit("선택 해제 · "+force.name());if(select.y()!=close.y()||select.x()+select.w()>close.x()+2)throw new AssertionError("Task-force selection and close must occupy the first row");
                    var bands=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("naval_force_band")).toList();if(bands.size()!=fleet.panel().members().size())throw new AssertionError("All source task forces must fit in one band for this fixture");
                    for(var h:bands)if(h.x()<0||h.x()+h.w()>client.getWindow().getGuiScaledWidth())throw new AssertionError("Navy band escaped the viewport");
                    var a=portrait(fleets.get(0).id());var b=portrait(fleets.get(1).id());if(b.x()>a.x()+a.width())throw new AssertionError("Fleet cards must touch");
                });
                game.takeScreenshot("hoi-navy-layout-"+size[0]+"x"+size[1]+"-gui2");
                click(game,game.computeOnClient(client->hit("선택 · "+force.name())),InputConstants.MOUSE_BUTTON_RIGHT);
                game.runOnClient(client->{if(!opens.isEmpty()||!commands.isEmpty()||MapCommanderPanels.commandSelection().size()!=fleet.panel().members().size())throw new AssertionError("Right-click on select must not change or send orders");});
                click(game,game.computeOnClient(client->hit("선택 · "+force.name())),InputConstants.MOUSE_BUTTON_LEFT);game.waitTicks(2);
                game.runOnClient(client->{if(!MapCommanderPanels.commandSelection().equals(List.of("navy/"+force.id())))throw new AssertionError("Selection must retain exact task-force identity");});
                game.takeScreenshot("hoi-navy-single-force-"+size[0]+"x"+size[1]+"-gui2");
                click(game,game.computeOnClient(client->hit("선택 해제 · "+force.name())),InputConstants.MOUSE_BUTTON_LEFT);game.waitTicks(2);
                game.runOnClient(client->{if(!MapCommanderPanels.commandSelection().isEmpty())throw new AssertionError("X must deselect the task force without destroying it");});
                click(game,game.computeOnClient(client->hit(UiCommandLabels.text("SELECT_ALL","모두 선택"))),InputConstants.MOUSE_BUTTON_LEFT);game.waitTicks(2);
                game.runOnClient(client->{if(MapCommanderPanels.commandSelection().size()!=fleet.panel().members().size()||!opens.isEmpty()||!commands.isEmpty())throw new AssertionError("Select all is local selection only");});
                var patrol=game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.tip().startsWith(UiCommandLabels.text("NAVAL_MISSION_NAME_PATROL","초계"))).findFirst().orElseThrow());
                game.getInput().setCursorPos((patrol.x()+patrol.w()/2)*2,(patrol.y()+patrol.h()/2)*2);game.waitTicks(2);game.takeScreenshot("hoi-navy-patrol-tooltip-"+size[0]+"x"+size[1]+"-gui2");
                click(game,patrol,InputConstants.MOUSE_BUTTON_LEFT);game.runOnClient(client->{if(opens.size()!=1||opens.getFirst().mode()!=Mode.NAVAL_TOOLBAR||!opens.getFirst().selection().getFirst().equals("mission/PATROL")||opens.getFirst().selection().size()!=fleet.panel().members().size()+1)throw new AssertionError("Patrol must target every selected force exactly");});
                game.getInput().setCursorPos(size[0]-30,90);game.waitTicks(2);
                game.runOnClient(client->{var a=portrait(fleets.get(0).id());var b=portrait(fleets.get(1).id());double x=a.x()+a.width()/2.,y=a.y()+a.height()/2.,end=b.x()+b.width()-1;client.gui.screen().mouseClicked(mouse(x,y,InputConstants.MOUSE_BUTTON_LEFT),false);client.gui.screen().mouseDragged(mouse(end,y-30,InputConstants.MOUSE_BUTTON_LEFT),end-x,-30);client.gui.screen().mouseReleased(mouse(end,y-30,InputConstants.MOUSE_BUTTON_LEFT));if(commands.size()!=1||!commands.getFirst().action().equals("CARD_ORDER")||commands.getFirst().selection().getFirst().equals(fleet.id()))throw new AssertionError("Fleet drag must issue the existing saved card-order command: "+commands+" ready="+MapCommanderOrders.ready()+" context="+MapCommanderOrders.currentContext()+" opens="+opens);});
                game.waitTicks(2);game.takeScreenshot("hoi-navy-drag-pending-"+size[0]+"x"+size[1]+"-gui2");
                for(Boolean running:Arrays.asList(false,null)){
                    game.runOnClient(client->{MapCommanderHud.reset();MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),running));MapCommanderPanels.select(fleet.id(),false);});
                    game.waitTicks(2);
                    game.runOnClient(client->MapCommanderPanelsTest.assertCardLayout(new View(source.country(),source.dimension(),source.branch(),source.cards(),source.wings(),running),client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight()));
                }
                game.runOnClient(client->{MapCommanderHud.requestObserver=null;MapCommanderOrders.commandObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderHud.reset();MapUnitHud.reset();});
            }
            MapNavalRepairInputTest.run(game,menu,directory);
            MapNavalContractTest.run(game,menu,directory);
            MapCommanderPanelsTest.runNavyLayout(game,menu);
        }catch(java.io.IOException e){throw new AssertionError("Cannot read authoritative navy fixture",e);}
    }
}
