package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.*;

public final class MapLeaderControlsTest {
    private MapLeaderControlsTest() {}
    public static void run(ClientGameTestContext game,ProtocolMenuView menu) {
        String directory=Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW"));
        try {
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720}))for(String branch:List.of("army-assigned","army-group-assigned","army-advisor","navy")){
                var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-"+(branch.equals("navy")?"navy-ten-forces":branch)+".json"))).view();
                Mode role=branch.equals("navy")?Mode.NAVY_FLEET:branch.equals("army-group-assigned")?Mode.ARMY_GROUP:Mode.ASSIGN_ARMY;
                var card=source.cards().stream().filter(c->c.role()==role&&c.panel()!=null&&c.panel().leader()!=null).findFirst().orElseThrow();
                var sent=new ArrayList<Open>();game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),source.running(),source.theaters(),source.armyTheaters()));MapCommanderHud.requestObserver=sent::add;MapCommanderPanels.select(card.id(),false);});
                game.setScreen(ScreenMapCursor::new);game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);
                game.takeScreenshot("hoi-leader-controls-"+branch+"-"+size[0]+"x"+size[1]+"-gui2");
                for(String id:List.of("LEADER_TRAITS","LEADER_ADVISOR")){
                    var control=card.panel().controls().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();
                    for(boolean panel:List.of(false,true)){
                        var point=game.computeOnClient(client->{
                            if(panel){var hit=MapCommanderPanels.hits().stream().filter(h->h.icon().equals((branch.equals("navy")?"navy":"army")+(id.equals("LEADER_TRAITS")?"_leader_details":"_leader_advisor"))).findFirst().orElseThrow();if(!hit.tip().equals(control.tooltip()))throw new AssertionError("Panel control must preserve source tooltip");return new int[]{hit.x()+hit.w()/2,hit.y()+hit.h()/2};}
                            var portrait=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals(card.id())).findFirst().orElseThrow();
                            var hit=MapCommanderHud.boxes().stream().filter(b->b.action()!=null&&b.tip().equals(control.tooltip())&&b.x()>=portrait.x()&&b.x()<portrait.x()+portrait.width()).findFirst().orElseThrow();return new int[]{hit.x()+hit.width()/2,hit.y()+hit.height()/2};
                        });
                        game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);
                        game.runOnClient(client->{if(!MapCommanderHud.renderedTooltip().equals(control.tooltip()))throw new AssertionError("Control must retain hover even when disabled");sent.clear();click(client.gui.screen(),point,InputConstants.MOUSE_BUTTON_RIGHT);if(!sent.isEmpty())throw new AssertionError("Right-click must not appoint an advisor");click(client.gui.screen(),point,InputConstants.MOUSE_BUTTON_LEFT);var expected=control.enabled()?List.of(control.request()):List.<Open>of();if(!sent.equals(expected))throw new AssertionError("Portrait control must send only its authoritative request: "+sent);});
                        game.takeScreenshot("hoi-leader-control-"+branch+"-"+id+"-"+(panel?"panel":"card")+"-"+size[0]+"x"+size[1]+"-gui2");
                    }
                }
            }
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
        finally{game.runOnClient(client->{MapCommanderHud.requestObserver=null;MapCommanderHud.reset();MapUnitHud.reset();});game.setScreen(()->null);}
    }
    private static void click(net.minecraft.client.gui.screens.Screen screen,int[] point,int button){var event=new MouseButtonEvent(point[0],point[1],new MouseButtonInfo(button,0));screen.mouseClicked(event,false);screen.mouseReleased(event);}
}
