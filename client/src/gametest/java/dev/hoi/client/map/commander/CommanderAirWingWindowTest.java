package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.protocol.commander.CommanderAirWingPayloads;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.commander.CommanderShipPayloads;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.industry.IndustryTankDesigner;
import dev.hoi.protocol.menu.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class CommanderAirWingWindowTest {
    private CommanderAirWingWindowTest() {}
    private static IndustryTankDesigner.Stat stat(String id,double value){return new IndustryTankDesigner.Stat(id,id,value,id.equals("reliability")||id.equals("night_penalty"));}
    static CommanderAirWingPayloads.View view(String token,Member wing) {
        var air=wing.data().air().wing();
        var columns=List.of(new CommanderShipPayloads.Column("BASE",List.of(stat("maximum_speed",600),stat("air_range",650),stat("supply_consumption",.2))),
                new CommanderShipPayloads.Column("COMBAT",List.of(stat("air_defence",30.5),stat("air_attack",5.5),stat("air_agility",15),stat("air_superiority",1),stat("naval_strike_attack",6),stat("naval_strike_targetting",10),stat("air_ground_attack",10),stat("air_bombing",0))),
                new CommanderShipPayloads.Column("MISC",List.of(stat("reliability",.8),stat("fuel_consumption",.2),stat("mines_planting",0),stat("mines_sweeping",0),stat("surface_detection",0),stat("sub_detection",0),stat("night_penalty",0))));
        return new CommanderAirWingPayloads.View(token,wing.id(),"KOR",wing.name(),air.equipmentIcon(),true,"창허 Z-10","200","100%",List.of("CLOSE_AIR_SUPPORT","NAVAL_STRIKE","EXERCISE"),columns,
                List.of(new CommanderAirWingPayloads.Equipment("2세대 공격 헬리콥터","창허 Z-10",air.equipmentIcon(),true,wing.data().quantity())),"","commander/commands/ace_none/frame_0",
                Map.of("delete","비행단 해산","carrier","항공모함에 배치 가능"),Map.of("AIR_WING_STATS","비행단 세부 사항","DESIGNER_BASE_STATS_LABEL","기본 능력치","DESIGNER_COMBAT_STATS_LABEL","전투 능력치","FE_MISC_STATS","기타 정보","CLOSE","닫기"),"");
    }
    private static void click(ClientGameTestContext game,double x,double y,int button){double scale=game.computeOnClient(client->client.getWindow().getGuiScale());game.getInput().setCursorPos(x*scale,y*scale);game.waitTicks(1);game.getInput().pressMouse(button);game.waitTicks(2);}
    private static double[] at(ClientGameTestContext game,double px,double py){return game.computeOnClient(client->{var frame=CommanderAirWingHud.window().frame();return new double[]{frame.x(px),frame.y(py)};});}
    static void run(ClientGameTestContext game,MenuView menu) {
        View source;try{source=new Snapshot(java.nio.file.Files.readString(java.nio.file.Path.of(Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW")),"kor-command-panel-air.json"))).view();}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
        var wing=source.wings().stream().sorted(Comparator.comparing(CommanderAirBases::identity).thenComparing(Member::id)).findFirst().orElseThrow();
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
            var requests=new ArrayList<CommanderAirWingPayloads.Request>();var opens=new ArrayList<Open>();game.getInput().resizeWindow(size[0],size[1]);
            game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));CommanderHud.reset();CommanderUnitHud.reset();CommanderAirWingHud.requestObserver=requests::add;CommanderHud.requestObserver=opens::add;CommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),source.running(),source.theaters(),source.armyTheaters()));CommanderPanels.showWings(source.wings().stream().map(Member::id).toList());});
            game.setScreen(MapCursor::new);game.waitTicks(3);
            var portrait=game.computeOnClient(client->CommanderPanels.hits().stream().filter(h->h.icon().equals("unitlist_unitinfo_button")&&h.tip().startsWith("§e"+wing.name()+"§r")).findFirst().orElseThrow(()->new AssertionError("Wing portrait must be clickable")));
            click(game,portrait.x()+portrait.w()/2.,portrait.y()+portrait.h()/2.,InputConstants.MOUSE_BUTTON_RIGHT);
            game.runOnClient(client->{if(!requests.isEmpty()||CommanderAirWingHud.window()!=null)throw new AssertionError("Right click must not open the air wing details");});
            click(game,portrait.x()+portrait.w()/2.,portrait.y()+portrait.h()/2.,InputConstants.MOUSE_BUTTON_LEFT);
            game.runOnClient(client->{if(requests.size()!=1||requests.getFirst().action()!=CommanderAirWingPayloads.Action.OPEN||!requests.getFirst().wing().equals(wing.id())||!opens.isEmpty())throw new AssertionError("The equipment portrait must open the native air wing details window: "+requests+" "+opens);CommanderAirWingHud.accept(view(requests.getFirst().token(),wing));requests.clear();});
            game.waitTicks(3);
            game.takeScreenshot("hoi-air-wing-details-"+size[0]+"x"+size[1]+"-gui2");
            game.runOnClient(client->{var frame=CommanderAirWingHud.window().frame();if(frame==null)throw new AssertionError("Air wing details window must be laid out");if(CommanderAirWingWindow.row(0,0).y()>=CommanderAirWingWindow.row(0,1).y())throw new AssertionError("Stat rows must follow the background rows");});
            var delete=at(game,561+15,3+15);click(game,delete[0],delete[1],InputConstants.MOUSE_BUTTON_LEFT);
            game.runOnClient(client->{if(!opens.equals(List.of(new Open(Mode.AIR_ORGANIZE,"",List.of("action/DISBAND",wing.id())))))throw new AssertionError("Delete must open the original wing disband confirmation: "+opens);opens.clear();});
            var close=at(game,821+15,3+15);click(game,close[0],close[1],InputConstants.MOUSE_BUTTON_LEFT);
            game.runOnClient(client->{if(CommanderAirWingHud.window()!=null||requests.size()!=1||requests.getFirst().action()!=CommanderAirWingPayloads.Action.CLOSE)throw new AssertionError("Close must close the window and release the server session: "+requests);requests.clear();});
            click(game,portrait.x()+portrait.w()/2.,portrait.y()+portrait.h()/2.,InputConstants.MOUSE_BUTTON_LEFT);game.getInput().pressKey(InputConstants.KEY_ESCAPE);game.waitTicks(2);
            game.runOnClient(client->{if(CommanderAirWingHud.window()!=null)throw new AssertionError("Escape must close the air wing details window");});
            game.runOnClient(client->{CommanderAirWingHud.requestObserver=null;CommanderHud.requestObserver=null;CommanderAirWingHud.reset();CommanderHud.reset();});
        }
    }
}
