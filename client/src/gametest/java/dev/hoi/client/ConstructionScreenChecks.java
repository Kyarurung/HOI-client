package dev.hoi.client;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

final class ConstructionScreenChecks {
    static void run(ClientGameTestContext context,CountryHud hud) {
        var requests=new ArrayList<ConstructionProtocol.Request>();String token=UUID.randomUUID().toString();
        var buildings=new ArrayList<ConstructionView.Building>();
        for(String id:List.of("infrastructure","air_base","anti_air","radar","military_factory","civilian_factory","dockyard","office_park","refinery","fuel_silo","nuclear_reactor","power_plant","energy_farm","fort","coastal_fort","port","hub","dam","dam_mountain"))
            buildings.add(new ConstructionView.Building(id,id.equals("civilian_factory")?"민간공장":id,"construction/"+id,!Set.of("office_park","nuclear_reactor","power_plant","energy_farm","fort","coastal_fort","port","hub","dam","dam_mountain").contains(id),"주를 선택하세요."));
        var projects=new ArrayList<ConstructionView.Project>();
        for(int i=0;i<16;i++)projects.add(new ConstructionView.Project("p"+i,"state"+i,i%2==0?"civilian_factory":"military_factory",i%2==0?"경상북도":"서울",1,i<2?15:0,2500,90,10800));
        var summary=new ConstructionView.Summary(43,7,2,30,4,0,4,1.31,7,12);
        var view=new ConstructionView(token,1,"KOR",hud,"civilian_factory",summary,buildings,projects,"");
        var ref=new AtomicReference<ConstructionScreen>();
        context.setScreen(()->{var screen=new ConstructionScreen(token,view,requests::add);ref.set(screen);return screen;});
        context.waitTicks(3);context.takeScreenshot("hoi-construction-queue");
        context.runOnClient(client->{
            for(String art:java.util.stream.Stream.concat(buildings.stream().map(ConstructionView.Building::texture),java.util.stream.Stream.of("construction/consumer_goods","construction/energy")).toList())
                if(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+art+".png")).isEmpty())throw new AssertionError("Missing construction art: "+art);
            var screen=ref.get();if(screen.panelWidth()!=HoiPanelLayout.width(MenuTab.CONSTRUCTION,screen.width))throw new AssertionError("Construction sidebar ratio");
            var button=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("민간공장")).findFirst().orElseThrow();
            button.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0));
            if(requests.size()!=1||requests.getFirst().action()!=ConstructionProtocol.Action.SELECT)throw new AssertionError("Building must issue a server selection");
            screen.update(new ConstructionView(token,2,"KOR",hud,"civilian_factory",summary,buildings,projects,""));
            var plus=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("+")).findFirst().orElseThrow();
            plus.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0));
            if(requests.getLast().action()!=ConstructionProtocol.Action.REPAIR||requests.getLast().amount()!=5)throw new AssertionError("Repair factory control must issue a bounded server request");
            screen.update(new ConstructionView(token,3,"KOR",hud,"civilian_factory",summary,buildings,projects,""));
            screen.mouseScrolled(30,170,0,-100);
        });
        context.waitTicks(2);context.takeScreenshot("hoi-construction-scrolled");
        context.getInput().resizeWindow(854,480);context.waitTicks(3);context.takeScreenshot("hoi-construction-compact");
        context.runOnClient(client->{
            var screen=ref.get();
            if(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("↑")))throw new AssertionError("Last queue row must remain accessible after compact resize");
            for(var w:screen.children())if(w instanceof Button b && (b.getX()<0||b.getY()<0||b.getX()+b.getWidth()>screen.width||b.getY()+b.getHeight()>screen.height))throw new AssertionError("Construction control escaped viewport");
            screen.mouseScrolled(screen.panelWidth()-8,170,0,-100);
            if(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("dam_mountain")))throw new AssertionError("Last building must remain accessible by palette scrolling");
        });
        context.waitTicks(2);context.takeScreenshot("hoi-construction-palette-scrolled");
        context.runOnClient(client->{
            ref.get().update(new ConstructionView(token,0,"",null,"",null,List.of(),List.of(),"권한 만료"));
            if(client.gui.screen()!=null)throw new AssertionError("Revoked construction must close");
        });
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
    }
}
