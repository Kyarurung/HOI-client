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
                    var u=MapCommanderLayout.units(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());
                    if(a.y()!=MapCommanderLayout.cardsBottom(u)-u.size(58.5)+u.size(3.5)||MapCommanderLayout.commandTop(u,true)!=MapCommanderLayout.cardsBottom(u)-u.size(122.5)-1)throw new AssertionError("Only the navy mission strip and task-force band must move up one GUI; fleet cards retain their original position");
                });
                game.takeScreenshot("hoi-navy-layout-"+size[0]+"x"+size[1]+"-gui2");
                game.runOnClient(client->{for(var control:fleet.panel().controls())if(Set.of("LEADER_TRAITS","LEADER_ADVISOR").contains(control.id())){var target=MapCommanderPanels.hits().stream().filter(h->h.icon().equals(control.id().equals("LEADER_TRAITS")?"navy_leader_details":"navy_leader_advisor")).findFirst().orElseThrow();MapCommanderPanels.click(target.x()+1,target.y()+1);if(control.enabled()){if(opens.size()!=1||!opens.getFirst().equals(control.request()))throw new AssertionError("Leader control must use its authoritative request");}else if(!opens.isEmpty())throw new AssertionError("Unavailable leader control must not send");opens.clear();}});
                game.runOnClient(client->{var box=portrait(fleets.get(1).id());var e=mouse(box.x()+box.width()/2.,box.y()+box.height()/2.,InputConstants.MOUSE_BUTTON_LEFT);client.gui.screen().mouseClicked(e,false);client.gui.screen().mouseReleased(e);});game.waitTicks(2);
                game.runOnClient(client->{var expected=fleets.get(1).panel().members().stream().map(m->"navy/"+m.id()).sorted().toList();if(!MapCommanderPanels.commandSelection().equals(expected)||MapCommanderPanels.hits().stream().anyMatch(h->h.tip().equals("선택 · "+force.name())))throw new AssertionError("Switching fleet cards leaked another fleet's task forces");MapCommanderPanels.closeSelection();});game.waitTicks(2);
                game.runOnClient(client->{if(!MapCommanderPanels.hits().isEmpty()||!MapCommanderPanels.commandSelection().isEmpty())throw new AssertionError("Unselected navy view must hide missions and force lists");MapCommanderPanels.select(fleet.id(),false);});game.waitTicks(2);
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
            MapCommanderCardScopeTest.run(game,menu,directory);
            MapCommanderBranchTheatersTest.run(game,menu);
            tenForces(game,menu,directory);
            admiralTraits(game,menu,directory);
        }catch(java.io.IOException e){throw new AssertionError("Cannot read authoritative navy fixture",e);}
    }
    private static void admiralTraits(ClientGameTestContext game,ProtocolMenuView menu,String directory)throws java.io.IOException {
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720}))for(String scenario:List.of("actual","seven","army")){
            var json=com.google.gson.JsonParser.parseString(Files.readString(Path.of(directory,scenario.equals("army")?"kor-command-panel-army-assigned.json":"kor-command-panel-navy-ten-forces.json"))).getAsJsonObject();
            if(scenario.equals("seven"))for(var element:json.getAsJsonArray("cards")){var card=element.getAsJsonObject();if(card.get("count").getAsInt()!=10)continue;var traits=card.getAsJsonObject("panel").getAsJsonObject("leader").getAsJsonArray("traitDetails");var originals=traits.deepCopy();while(traits.size()<7){var trait=originals.get(traits.size()%originals.size()).deepCopy().getAsJsonObject();trait.addProperty("tooltip","Synthetic trait "+traits.size());traits.add(trait);}}
            var source=new Snapshot(json.toString()).view();var fleet=source.cards().stream().filter(c->c.panel()!=null&&c.panel().leader()!=null&&(scenario.equals("army")?c.role()==Mode.ASSIGN_ARMY:c.count()==10)).findFirst().orElseThrow();var opens=new ArrayList<Open>();
            game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true,source.theaters()));MapCommanderHud.requestObserver=opens::add;MapCommanderPanels.select(fleet.id(),false);});
            game.setScreen(ScreenMapCursor::new);game.getInput().setCursorPos(size[0]-30,90);game.waitTicks(3);game.takeScreenshot("hoi-admiral-header-"+scenario+"-"+size[0]+"x"+size[1]+"-gui2");
            if(!scenario.equals("army")){
                var traits=game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("navy_trait")).toList());
                if(traits.size()!=fleet.panel().leader().traitDetails().size()||scenario.equals("seven")&&traits.size()!=7)throw new AssertionError("Every authoritative or synthetic trait must remain visible");
                for(int n=0;n<traits.size();n++){var hit=traits.get(n);if(hit.w()<=0||n+1<traits.size()&&hit.x()+hit.w()!=traits.get(n+1).x())throw new AssertionError("Overlapping trait hover areas must partition the visible icons");click(game,hit,InputConstants.MOUSE_BUTTON_RIGHT);game.runOnClient(client->{if(!opens.isEmpty())throw new AssertionError("Trait right click must not send");});click(game,hit,InputConstants.MOUSE_BUTTON_LEFT);game.runOnClient(client->{if(opens.size()!=1||!opens.getFirst().equals(new Open(Mode.COMMANDER_TRAITS,"navy/"+fleet.panel().commander())))throw new AssertionError("Every visible trait must open its admiral");opens.clear();});}
                var first=traits.getFirst();game.getInput().setCursorPos((first.x()+first.w()/2)*2,(first.y()+first.h()/2)*2);game.waitTicks(2);game.takeScreenshot("hoi-admiral-header-"+scenario+"-tooltip-"+size[0]+"x"+size[1]+"-gui2");
            }
            game.runOnClient(client->{MapCommanderHud.requestObserver=null;MapCommanderHud.reset();MapUnitHud.reset();});
        }
    }
    private static void tenForces(ClientGameTestContext game,ProtocolMenuView menu,String directory)throws java.io.IOException {
        var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-navy-ten-forces.json"))).view();
        var fleet=source.cards().stream().filter(c->c.role()==Mode.NAVY_FLEET&&c.count()==10&&c.panel()!=null&&!c.panel().commander().isEmpty()).findFirst().orElseThrow(()->new AssertionError("Producer must supply an admiral with ten task forces"));
        var forces=fleet.panel().members();if(forces.size()!=10||fleet.capacity()!=10||!MapCommanderHud.countLabel(fleet).equals("10/10")||MapCommanderHud.countColor(fleet)!=dev.hoi.client.ui.UiMenuStyle.TEXT)throw new AssertionError("Ten-force card must retain the server count/capacity");
        for(var card:source.cards())if(card.role()==Mode.NAVY_HQ||card.role()==Mode.NAVY_FLEET&&card.panel()!=null&&card.panel().commander().isEmpty())if(!MapCommanderHud.countLabel(card).equals(Integer.toString(card.count())))throw new AssertionError("Unassigned and reserve cards must show only their count");
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            var commands=new ArrayList<ProtocolCommanderOrders.Command>();game.getInput().resizeWindow(size[0],size[1]);
            game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true,source.theaters()));MapCommanderOrders.selectionObserver=ignored->{};MapCommanderOrders.commandObserver=commands::add;MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(UUID.randomUUID().toString(),1,source.country(),client.level.dimension().identifier().toString(),""));MapCommanderPanels.select(fleet.id(),false);});
            game.setScreen(ScreenMapCursor::new);game.getInput().setCursorPos(20,60);game.waitTicks(3);
            game.runOnClient(client->{
                var ids=forces.stream().map(m->"theater_force/"+m.id()).toList();var badges=MapCommanderPanels.hits().stream().filter(h->ids.contains(h.icon())).toList();var band=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("naval_force_band")).toList();
                if(badges.size()!=10||band.size()!=10||badges.stream().map(MapCommanderPanels.Hit::y).distinct().count()!=1)throw new AssertionError("Ten task forces must fit a single theater row and command band");
                for(var row:List.of(badges,band)){var ordered=row.stream().sorted(Comparator.comparingDouble(MapCommanderPanels.Hit::visualX)).toList();float step=(ordered.get(1).visualX()-ordered.get(0).visualX())*2;for(int n=2;n<10;n++)if((ordered.get(n).visualX()-ordered.get(n-1).visualX())*2!=step||step!=Math.round(step))throw new AssertionError("Each overflowing row needs equal integer-pixel spacing");}
                if(MapCommanderPanels.hits().stream().anyMatch(h->h.tip().equals("다음")))throw new AssertionError("Task-force list must scroll without a next-page control");

                int edge=client.getWindow().getGuiScaledWidth()-14;for(var badge:badges)if(badge.x()+badge.w()>edge)throw new AssertionError("Task-force number escaped the original navy box");
                for(var h:MapCommanderPanels.hits())if(h.x()<0||h.y()<0||h.x()+h.w()>client.getWindow().getGuiScaledWidth()||h.y()+h.h()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Ten-force panel escaped the viewport");
            });
            game.takeScreenshot("hoi-navy-ten-forces-"+size[0]+"x"+size[1]+"-gui2");
            var seen=new HashSet<String>();for(int page=0;page<12;page++){
                game.runOnClient(client->{for(var force:forces)if(MapCommanderPanels.hits().stream().anyMatch(h->h.tip().equals("선택 · "+force.name())))seen.add(force.id());client.gui.screen().mouseScrolled(100,170,0,-1);});game.waitTicks(2);
                if(seen.size()==10)break;
            }
            if(seen.size()!=10)throw new AssertionError("Every task force must remain reachable in the left panel: "+seen.size());
            game.takeScreenshot("hoi-navy-ten-forces-scrolled-"+size[0]+"x"+size[1]+"-gui2");
            for(var force:forces){
                var badge=game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("theater_force/"+force.id())).findFirst().orElseThrow());
                click(game,badge,InputConstants.MOUSE_BUTTON_RIGHT);game.runOnClient(client->{if(!commands.isEmpty())throw new AssertionError("Theater badge right click sent an order");});
                game.runOnClient(client->{var e=mouse(badge.x()+badge.w()-1,badge.y()+badge.h()/2.,InputConstants.MOUSE_BUTTON_LEFT);client.gui.screen().mouseClicked(e,false);client.gui.screen().mouseReleased(e);});game.waitTicks(2);
                game.runOnClient(client->{if(!MapCommanderPanels.commandSelection().equals(List.of("navy/"+force.id())))throw new AssertionError("Overlapping badge selected the wrong force: "+force.id());});
            }
            game.runOnClient(client->{MapCommanderPanels.closeSelection();});game.waitTicks(2);game.takeScreenshot("hoi-navy-unselected-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{MapCommanderOrders.commandObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderHud.reset();MapUnitHud.reset();});
        }
    }
}
