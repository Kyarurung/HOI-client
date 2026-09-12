package dev.hoi.client.screen;

import dev.hoi.client.ResearchScreenGameTest;
import dev.hoi.client.ui.HoiPanelLayout;
import dev.hoi.client.ui.HoiTooltips;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class ConstructionScreenChecks {
    public static void run(ClientGameTestContext context,CountryHud hud) {
        var requests=new ArrayList<ConstructionProtocol.Request>();String token=UUID.randomUUID().toString();
        var buildings=new ArrayList<ConstructionView.Building>();
        for(String id:List.of("infrastructure","air_base","anti_air","radar","military_factory","civilian_factory","dockyard","office_park","refinery","fuel_silo","nuclear_reactor","power_plant","energy_farm","fort","coastal_fort","port","hub","railway","dam","dam_mountain"))
            buildings.add(new ConstructionView.Building(id,id.equals("civilian_factory")?"민간공장":id.equals("military_factory")?"군수공장":id,"construction/"+id,!Set.of("office_park","nuclear_reactor","power_plant","energy_farm","fort","coastal_fort","port","hub","railway","dam","dam_mountain").contains(id),"주를 선택하세요."));
        var projects=new ArrayList<ConstructionView.Project>();
        for(int i=0;i<16;i++)projects.add(new ConstructionView.Project("p"+i,"state"+i,i%2==0?"civilian_factory":"military_factory",i%2==0?"경상북도":"서울",1,i<2?15:0,2500,90,10800));
        var summary=new ConstructionView.Summary(43,7,2,30,4,0,4,1.31,10,21.862500000000004,2,
                "건설 속도\n무역법: +15%\n국가 보정: +16%\n최종 배율 = 최대(0, 1 + 0.31) = 1.31",
                "사용 가능한 에너지: 10\n공급 = 기본 10 + 석탄 0 × 9 = 10\n총수요: 21.8625\n잉여분 = 공급 − 수요 = −11.8625",
                "소비재: 7 공장\n필요 공장 = 내림((민간 43 + 군수 10) × 0.14) = 7\n최종 배정 = 최소(43, 7) = 7");
        var view=new ConstructionView(token,1,"KOR",hud,"civilian_factory",summary,buildings,projects,"");
        var ref=new AtomicReference<ConstructionScreen>();
        context.setScreen(()->{var screen=new ConstructionScreen(token,view,requests::add);ref.set(screen);return screen;});
        context.waitTicks(3);context.takeScreenshot("hoi-construction-queue");
        ResearchScreenGameTest.gui2Screenshot(context,"hoi-construction-queue");
        context.runOnClient(client -> ref.get().update(new ConstructionView(token,2,"KOR",hud,"civilian_factory",
                new ConstructionView.Summary(43,7,2,30,4,0,4,1.31,30,21.8625,2),buildings,projects,"")));
        context.waitTicks(2); context.takeScreenshot("hoi-construction-energy-surplus");
        context.runOnClient(client -> ref.get().update(new ConstructionView(token,3,"KOR",hud,"civilian_factory",
                new ConstructionView.Summary(43,7,2,30,4,0,4,1.31,10,10,2),buildings,projects,"")));
        context.waitTicks(2); context.takeScreenshot("hoi-construction-energy-zero");
        context.runOnClient(client -> ref.get().update(new ConstructionView(token,4,"KOR",hud,"civilian_factory",summary,buildings,projects,"")));
        context.getInput().setCursorPos(40,190); context.waitTicks(2); context.takeScreenshot("hoi-construction-multiline-tooltip");
        context.runOnClient(client -> {
            var lines=HoiTooltips.lines(client.font,"첫 줄\n사용 가능한 에너지 10 / 필요량 21.86\n긴 설명 "+"읽기 쉬운 설명 ".repeat(40),427);
            if(lines.size()<4)throw new AssertionError("Tooltips split explicit lines and wrap long text");
            for(var line:lines) {
                if(client.font.width(line)>300)throw new AssertionError("Tooltip width is bounded");
                line.accept((index,style,codepoint)->{if(codepoint==10||codepoint==13)throw new AssertionError("Never render newline control glyphs");return true;});
            }
        });
        context.getInput().setCursorPos(800,500);
        context.runOnClient(client->{
            for(String art:java.util.stream.Stream.concat(buildings.stream().map(ConstructionView.Building::texture),java.util.stream.Stream.of("construction/consumer_goods","construction/energy","construction/energy_deficit","construction/summary/civilian_factory","construction/summary/civilian_factory_repair")).toList())
                if(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+art+".png")).isEmpty())throw new AssertionError("Missing construction art: "+art);
            var screen=ref.get();if(screen.panelWidth()!=HoiPanelLayout.width(MenuTab.CONSTRUCTION,screen.width))throw new AssertionError("Construction sidebar ratio");
            assertNoDams(screen);
            var stateBottom=List.of("infrastructure","air_base","anti_air","radar").stream().map(id->buildingButton(screen,id)).mapToInt(b->b.getY()+b.getHeight()).max().orElseThrow();
            var sharedTop=buildingButton(screen,"군수공장").getY();
            var sharedBottom=buildingButton(screen,"energy_farm").getY()+buildingButton(screen,"energy_farm").getHeight();
            var localTop=buildingButton(screen,"hub").getY();
            var hub=buildingButton(screen,"hub");var rail=buildingButton(screen,"railway");
            var port=buildingButton(screen,"port");var fort=buildingButton(screen,"fort");var coast=buildingButton(screen,"coastal_fort");
            if(hub.getY()!=rail.getY()||hub.getX()>=rail.getX()||port.getY()!=fort.getY()||port.getX()>=fort.getX()||port.getY()<=hub.getY()||coast.getY()<=port.getY())
                throw new AssertionError("Province palette order: hub/railway, port/fort, coastal fort");
            if(sharedTop-stateBottom<7||localTop-sharedBottom<7)throw new AssertionError("State, shared, and local buildings need separate rows and divider space");
            var button=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("민간공장")).findFirst().orElseThrow();
            button.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0));
            if(requests.size()!=1||requests.getFirst().action()!=ConstructionProtocol.Action.SELECT)throw new AssertionError("Building must issue a server selection");
            screen.update(new ConstructionView(token,5,"KOR",hud,"civilian_factory",summary,buildings,projects,""));
            var plus=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("+")).findFirst().orElseThrow();
            plus.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0));
            if(requests.getLast().action()!=ConstructionProtocol.Action.REPAIR||requests.getLast().amount()!=5)throw new AssertionError("Repair factory control must issue a bounded server request");
            screen.update(new ConstructionView(token,6,"KOR",hud,"civilian_factory",summary,buildings,projects,""));
            for(int mouseButton:new int[]{1,0}) {
                screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(screen.panelWidth()+50,screen.height/2.0,
                        new net.minecraft.client.input.MouseButtonInfo(mouseButton,0)),false);
                var last=requests.getLast();
                if(last.action()!=(mouseButton==0?ConstructionProtocol.Action.PLACE:ConstructionProtocol.Action.CANCEL_AT))
                    throw new AssertionError("Left click builds and right click cancels on the map");
                if(Math.abs(last.dx()*last.dx()+last.dy()*last.dy()+last.dz()*last.dz()-1)>.001)
                    throw new AssertionError("Map input sends a normalized ray, not a trusted province");
                screen.update(new ConstructionView(token,last.revision()+1,"KOR",hud,"civilian_factory",summary,buildings,projects,""));
            }
            screen.mouseScrolled(30,170,0,-100);
        });
        context.waitTicks(2);context.takeScreenshot("hoi-construction-scrolled");
        context.getInput().resizeWindow(854,480);context.waitTicks(3);context.takeScreenshot("hoi-construction-compact");
        context.getInput().setCursorPos(40,190); context.waitTicks(2); context.takeScreenshot("hoi-construction-compact-tooltip");
        context.getInput().setCursorPos(500,400);
        context.runOnClient(client->{
            var screen=ref.get();
            if(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("↑")))throw new AssertionError("Last queue row must remain accessible after compact resize");
            for(var w:screen.children())if(w instanceof Button b && (b.getX()<0||b.getY()<0||b.getX()+b.getWidth()>screen.width||b.getY()+b.getHeight()>screen.height))throw new AssertionError("Construction control escaped viewport");
            screen.mouseScrolled(screen.panelWidth()-8,170,0,-100);
            if(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("coastal_fort")))throw new AssertionError("Last local building must remain accessible by palette scrolling");
            assertNoDams(screen);
        });
        context.waitTicks(2);context.takeScreenshot("hoi-construction-palette-scrolled");
        context.runOnClient(client->{
            ref.get().update(new ConstructionView(token,0,"",null,"",null,List.of(),List.of(),"권한 만료"));
            if(client.gui.screen()!=null)throw new AssertionError("Revoked construction must close");
        });
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
    }

    private static Button buildingButton(ConstructionScreen screen,String name) {
        return (Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(name)).findFirst().orElseThrow();
    }

    private static void assertNoDams(ConstructionScreen screen) {
        if(screen.children().stream().anyMatch(w->w instanceof Button b&&Set.of("dam","dam_mountain").contains(b.getMessage().getString())))
            throw new AssertionError("Neither dam type belongs in the construction palette");
    }
}
