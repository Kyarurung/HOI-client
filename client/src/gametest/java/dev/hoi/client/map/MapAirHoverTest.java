package dev.hoi.client.map;

import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.phys.Vec3;

final class MapAirHoverTest {
    private static final String BASE="§G우클릭하면 선택한 비행단이 이 기지로 이동합니다§!";
    private static final String REGION="§G선택된 비행단을 이 구역에 배치하려면 우클릭하십시오§!";
    private MapAirHoverTest() {}
    static void run(ClientGameTestContext game,View source,int[] size)throws java.io.IOException{
        var queries=new ArrayList<SeaHover>();String wing=source.wings().getFirst().id(),suffix=size[0]+"x"+size[1]+"-gui2";
        try{
            for(var mode:List.of(Mode.AIR,Mode.ARMY)){
                game.runOnClient(client->{MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),mode,source.cards(),source.wings()));MapCommanderPanels.showWings(List.of(wing));MapNavalSeaHover.queryObserver=queries::add;});
                game.setScreen(ScreenMapCursor::new);game.getInput().setCursorPos(size[0]*.70,size[1]*.42);game.waitTicks(4);
                for(String target:List.of("AIR_BASE/123","AIR_REGION/456")){
                    String expected=target.startsWith("AIR_BASE/")?BASE:REGION;
                    game.runOnClient(client->{MapNavalSeaHover.reset();MapNavalSeaHover.queryObserver=q->{queries.add(q);MapNavalSeaHover.accept(new SeaHoverTooltip(q.sequence(),target,expected));};});game.waitTicks(6);
                    game.runOnClient(client->{if(!MapNavalSeaHover.text().equals(expected)||!MapCommanderHud.renderedTooltip().equals(expected))throw new AssertionError("Exact authoritative air hint must render: "+mode+"/"+target);var box=MapCommanderHud.tooltipBounds();if(box==null||box.x()+box.width()>size[0]/2||box.y()+box.height()>size[1]/2||Math.abs(box.x()-size[0]*.35)>box.width()+24||Math.abs(box.y()-size[1]*.21)>box.height()+24)throw new AssertionError("Air hint must remain next to cursor and inside screen");});
                    var screenshot=game.takeScreenshot("hoi-air-hover-"+mode.name().toLowerCase(Locale.ROOT)+"-"+(target.startsWith("AIR_BASE/")?"base":"region")+"-"+suffix);var bounds=game.computeOnClient(client->MapCommanderHud.tooltipBounds());java.nio.file.Files.writeString(screenshot.resolveSibling(screenshot.getFileName()+".json"),new com.google.gson.Gson().toJson(Map.of("tooltip",expected,"region",target,"x",bounds.x()*2,"y",bounds.y()*2,"width",bounds.width()*2,"height",bounds.height()*2,"responseInjected",true)));
                }
                game.runOnClient(client->{MapNavalSeaHover.queryObserver=queries::add;MapNavalSeaHover.reset();MapNavalSeaHover.point(new Ray(0,-1,0),Vec3.ZERO,"test");MapNavalSeaHover.tick();var old=queries.getLast();MapNavalSeaHover.point(new Ray(.1,-Math.sqrt(.99),0),Vec3.ZERO,"test");MapNavalSeaHover.accept(new SeaHoverTooltip(old.sequence(),"AIR_BASE/123",BASE));if(!MapNavalSeaHover.text().isEmpty())throw new AssertionError("Moved cursor must reject stale air hint");for(int n=0;n<4;n++)MapNavalSeaHover.tick();var latest=queries.getLast();for(String invalid:List.of("sea-1","","AIR_OTHER/123")){MapNavalSeaHover.accept(new SeaHoverTooltip(latest.sequence(),invalid,BASE));if(!MapNavalSeaHover.text().isEmpty())throw new AssertionError("Invalid air target must remain hidden");}MapNavalSeaHover.accept(new SeaHoverTooltip(latest.sequence(),"AIR_BASE/123",BASE));MapCommanderPanels.showWings(List.of(source.wings().get(1).id()));MapNavalSeaHover.accept(new SeaHoverTooltip(latest.sequence(),"AIR_BASE/123",BASE));if(!MapNavalSeaHover.text().isEmpty())throw new AssertionError("Changed wing must reject stale hint before next render");MapCommanderHud.closeSelection();MapNavalSeaHover.tick();if(!MapNavalSeaHover.text().isEmpty())throw new AssertionError("Deselection must clear air hint");});game.waitTicks(3);
                game.takeScreenshot("hoi-air-hover-"+mode.name().toLowerCase(Locale.ROOT)+"-cleared-"+suffix);
            }
            game.runOnClient(client->{MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),Mode.AIR,source.cards(),source.wings()));MapCommanderPanels.showWings(List.of(wing));MapNavalSeaHover.reset();MapNavalSeaHover.queryObserver=q->MapNavalSeaHover.accept(new SeaHoverTooltip(q.sequence(),"AIR_BASE/123",BASE));});
            game.setScreen(()->null);game.waitTicks(6);game.runOnClient(client->{if(!MapNavalSeaHover.text().equals(BASE))throw new AssertionError("Basic screen must request air hint");});game.takeScreenshot("hoi-air-hover-basic-base-"+suffix);
            game.runOnClient(client->{client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);});game.waitTicks(3);game.runOnClient(client->{if(!MapNavalSeaHover.text().isEmpty())throw new AssertionError("Third-person must clear air hint");client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);});
            game.runOnClient(client->{MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),Mode.NAVY_HQ,List.of(),List.of()));MapNavalSeaHover.reset();MapNavalSeaHover.queryObserver=queries::add;MapNavalSeaHover.point(new Ray(0,-1,0),Vec3.ZERO,"navy");MapNavalSeaHover.tick();var q=queries.getLast();MapNavalSeaHover.accept(new SeaHoverTooltip(q.sequence(),"sea-1","authoritative sea text"));if(!MapNavalSeaHover.text().equals("authoritative sea text"))throw new AssertionError("Existing naval hover must remain available without selected forces");MapNavalSeaHover.accept(new SeaHoverTooltip(q.sequence(),"AIR_BASE/123",BASE));if(!MapNavalSeaHover.text().isEmpty())throw new AssertionError("Naval mode must reject air response");});
        }finally{game.runOnClient(client->{MapNavalSeaHover.queryObserver=null;MapNavalSeaHover.reset();MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),Mode.AIR,source.cards(),source.wings()));});game.setScreen(ScreenMapCursor::new);}
    }
}
