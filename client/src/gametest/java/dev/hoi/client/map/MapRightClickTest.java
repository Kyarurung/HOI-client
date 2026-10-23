package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class MapRightClickTest {
    private MapRightClickTest() {}
    public static void run(ClientGameTestContext context,Consumer<String> command) {
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");
        var evidence=new ArrayList<Map<String,Object>>();
        try {
            command.accept("gamemode spectator @a");command.accept("tp @a 8 280 8 0 90");
            for(String kind:List.of("army","navy","air")) {
                var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-"+kind+".json"))).view();
                var member=source.branch()==Mode.AIR?source.wings().getFirst():source.cards().stream().filter(c->c.panel()!=null&&!c.panel().members().isEmpty()).findFirst().orElseThrow().panel().members().getFirst();
                for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720}))for(boolean counter:List.of(false,true)) {
                    context.getInput().resizeWindow(size[0],size[1]);
                    context.runOnClient(client->{client.options.guiScale().set(2);client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);if(client.gui.hud.isHidden())client.gui.hud.toggle();MapCommanderHud.reset();MapUnitHud.reset();ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",ProtocolCountryHud.UNKNOWN));MapCommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));});
                    context.setScreen(ScreenMapCursor::new);context.runOnClient(client->{source.cards().forEach(MapCommanderHud.groups::reveal);if(source.branch()==Mode.AIR&&!counter)MapCommanderPanels.showWings(List.of(member.id()));});context.waitTicks(3);
                    if(counter) {
                        String key=member.selectionKey();if(key.isEmpty())key=member.id()+"|"+(source.branch()==Mode.NAVY_HQ?"navy_destroyer":member.icon().substring("unit_hud/".length()));int split=key.lastIndexOf('|');
                        var marker=new ProtocolUnitHud.Counter(key.substring(0,split),"KOR","대한민국",key.substring(split+1),member.name(),1,8,250,8,1,1,1,Map.of());
                        context.runOnClient(client->MapUnitHud.accept(new ProtocolUnitHud.Snapshot(client.level.dimension().identifier().toString(),List.of(marker))));context.waitTicks(3);
                        context.runOnClient(client->{var box=MapUnitHud.boxes().getFirst();if(!MapUnitHud.selectAt(box.x()+box.width()/2.,box.y()+box.height()/2.))throw new AssertionError("Counter click must select");});
                    }else {
                        context.runOnClient(client->{if(source.branch()==Mode.AIR){var hit=MapCommanderPanels.hits().stream().filter(h->h.sound().equals("ui.division.select")&&h.tip().equals("§e"+member.name()+"§r\n"+member.detail())).findFirst().orElseThrow();MapCommanderHud.clickAt(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.);}else{var mode=source.branch()==Mode.ARMY?Mode.ASSIGN_ARMY:Mode.NAVY_FLEET;var hit=MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==mode).findFirst().orElseThrow();MapCommanderHud.clickAt(hit.x()+hit.width()/2.,hit.y()+hit.height()/2.);}});
                    }
                    context.waitTicks(2);
                    String suffix=kind+"-"+(counter?"counter":"card")+"-"+size[0]+"x"+size[1]+"-gui2";
                    var requests=new ArrayList<ProtocolCommanderOrders.Command>();
                    context.runOnClient(client->{MapCommanderOrders.commandObserver=requests::add;MapCommanderOrders.selectionObserver=value->{};MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(UUID.randomUUID().toString(),0,"KOR",client.level.dimension().identifier().toString(),""));var screen=client.gui.screen();var right=new MouseButtonEvent(screen.width*.78,screen.height*.65,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT,0));screen.mouseClicked(right,false);screen.mouseReleased(right);screen.mouseReleased(right);assertMap(requests,1);});
                    Path cursor=context.takeScreenshot("hoi-map-right-cursor-"+suffix);
                    context.setScreen(()->null);context.waitTicks(3);
                    context.runOnClient(client->{MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(UUID.randomUUID().toString(),0,"KOR",client.level.dimension().identifier().toString(),""));requests.clear();use(client);use(client);assertMap(requests,1);});
                    Path nativeImage=context.takeScreenshot("hoi-map-right-native-"+suffix);
                    evidence.add(Map.of("kind",kind,"selection",counter?"counter":"card","width",size[0],"height",size[1],"cursorRequests",1,"nativeRequests",requests.size(),"cursorImage",cursor.toString(),"nativeImage",nativeImage.toString()));
                    context.runOnClient(client->{var session=MapCommanderOrders.currentContext();MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(session.token(),1,"KOR",session.dimension(),""));use(client);assertMap(requests,2);client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);if(MapCommanderOrders.useMap(client))throw new AssertionError("Third-person input must remain vanilla");client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);MapCommanderHud.closeSelection();if(MapCommanderOrders.useMap(client))throw new AssertionError("No selection must retain vanilla input");MapCommanderOrders.commandObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderHud.reset();MapUnitHud.reset();});
                }
            }
            Files.writeString(Path.of((String)evidence.getFirst().get("nativeImage")).getParent().resolve("hoi-map-right-evidence.json"),new com.google.gson.Gson().toJson(evidence));
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}finally{context.runOnClient(client->{MapCommanderOrders.commandObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderHud.reset();MapUnitHud.reset();});}
    }
    private static void assertMap(List<ProtocolCommanderOrders.Command> requests,int count) {
        if(MapCommanderPanels.commandSelection().isEmpty()||requests.size()!=count||requests.stream().anyMatch(r->!r.action().equals("MAP")||r.rays().size()!=1||!r.selection().equals(MapCommanderPanels.commandSelection())))throw new AssertionError("Exact selected MAP once, expected="+count+" requests="+requests);
    }
    private static void use(net.minecraft.client.Minecraft client) {
        try{var method=client.getClass().getDeclaredMethod("startUseItem");method.setAccessible(true);method.invoke(client);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
}
