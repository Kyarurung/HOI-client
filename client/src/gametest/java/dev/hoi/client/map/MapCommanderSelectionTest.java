package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class MapCommanderSelectionTest {
    private MapCommanderSelectionTest() {}
    private static boolean before(){return "1".equals(System.getenv("HOI_CAPTURE_BEFORE"));}
    private static View fixture(String kind){try{return new Snapshot(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-command-panel-"+kind+".json"))).view();}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    private static void accept(View source){var c=net.minecraft.client.Minecraft.getInstance();MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(new View(source.country(),c.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),source.running()));}
    private static void click(MapCommanderPanels.Hit hit){MapCommanderPanels.click(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.);}
    public static void run(ClientGameTestContext game,ProtocolMenuView menu){
        var heard=new ArrayList<String>();
        net.minecraft.client.sounds.SoundEventListener listener=(sound,event,range)->{if(sound.getIdentifier().getNamespace().equals("hoi")){if(sound.getSource()!=net.minecraft.sounds.SoundSource.MASTER)throw new AssertionError("Selection must use Master");heard.add(sound.getIdentifier().getPath());}};
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        try{
            game.runOnClient(c->{ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));c.getSoundManager().addListener(listener);for(String hull:List.of("carrier","battleship","battlecruiser","heavy_cruiser","light_cruiser","destroyer","submarine"))if(dev.hoi.client.ui.UiAssets.imageHeight(MapCommanderPanels.navalHullIcon("unit_hud/navy_"+hull),30)!=12)throw new AssertionError("Missing original naval hull icon: "+hull);});
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);
                var navy=fixture("navy");var fleet=navy.cards().getFirst();var force=fleet.panel().members().getFirst();var other=navy.cards().get(1).panel().members().getFirst();
                game.runOnClient(c->{c.options.guiScale().set(2);accept(navy);MapUnitHud.replaceKeys(List.of(force.id()+"|navy_destroyer",other.id()+"|navy_destroyer"));MapCommanderPanels.mapSelectionChanged(true);});
                game.setScreen(ScreenMapCursor::new);game.getInput().setCursorPos(size[0]-4,80);game.waitTicks(2);
                game.runOnClient(c->{if(!before()&&MapCommanderPanels.hits().stream().anyMatch(h->h.icon().equals("naval_insignia")||h.icon().equals("navy_trait")))throw new AssertionError("Multi-fleet drag must render task forces without commander headers");});
                var multiple=game.takeScreenshot("hoi-selection-navy-multiple-"+size[0]+"-gui2");
                if(!before())assertForceFill(game,multiple,force);
                game.runOnClient(c->{heard.clear();click(MapCommanderPanels.hits().stream().filter(h->h.tip().equals("§e"+force.name()+"§r\n"+force.detail())).findFirst().orElseThrow());if(!before()&&!heard.equals(List.of("ui.fleet.select")))throw new AssertionError("Task force selection audio: "+heard);});game.waitTicks(2);
                game.runOnClient(c->{if(!before()&&!MapCommanderPanels.selectedCards().equals(Set.of(fleet.id())))throw new AssertionError("Direct force selection must scope its own fleet header");if(!before())assertBandSpacing();});
                var single=game.takeScreenshot("hoi-selection-navy-single-fleet-"+size[0]+"-gui2");
                if(!before())assertForceFill(game,single,force);
                game.runOnClient(c->{click(MapCommanderPanels.hits().stream().filter(h->h.icon().equals("naval_force_band")&&h.tip().startsWith(force.name()+"\n")).findFirst().orElseThrow());MapCommanderHud.accept(new View(navy.country(),c.level.dimension().identifier().toString(),navy.branch(),navy.cards(),navy.wings(),navy.running()));});game.waitTicks(2);
                var refreshed=game.takeScreenshot("hoi-selection-navy-reselected-refreshed-"+size[0]+"-gui2");
                if(!before())assertForceFill(game,refreshed,force);
                var air=fixture("air");var base=air.wings().stream().collect(java.util.stream.Collectors.groupingBy(MapCommanderAirBases::identity)).values().stream().filter(w->w.size()>1).min(Comparator.comparingInt(List::size)).orElseThrow();var wing=base.getFirst();
                game.runOnClient(c->{accept(air);MapCommanderPanels.showWings(base.stream().map(Member::id).toList());});game.waitTicks(2);
                game.runOnClient(c->{heard.clear();click(MapCommanderPanels.hits().stream().filter(h->h.icon().equals("air_mission_state")).findFirst().orElseThrow());if(!before()&&!heard.equals(List.of("ui.aircraft.select")))throw new AssertionError("Wing selection audio: "+heard);});game.waitTicks(2);
                game.runOnClient(c->{if(!before()&&(MapCommanderPanels.selectedWingIds().size()!=1||MapCommanderPanels.hits().stream().filter(h->h.icon().equals("air_mission_state")).count()!=base.size()))throw new AssertionError("One selected wing must retain all rows from its base: selected="+MapCommanderPanels.selectedWingIds()+" expected="+base.size()+" rows="+MapCommanderPanels.hits().stream().filter(h->h.icon().equals("air_mission_state")).count());});
                game.takeScreenshot("hoi-selection-air-partial-base-"+size[0]+"-gui2");
                game.runOnClient(c->{MapCommanderHud.accept(new View(air.country(),c.level.dimension().identifier().toString(),air.branch(),air.cards(),air.wings(),air.running()));});game.waitTicks(2);
                game.runOnClient(c->{if(!before()&&MapCommanderPanels.selectedWingIds().size()!=1)throw new AssertionError("Refresh changed partial wing selection");});
            }
        }finally{game.runOnClient(c->{c.getSoundManager().removeListener(listener);c.options.guiScale().set(previous);MapCommanderHud.reset();MapUnitHud.reset();});game.setScreen(()->null);}
    }
    private static void assertForceFill(ClientGameTestContext game,Path image,Member force){
        var bounds=game.computeOnClient(c->{var u=MapCommanderLayout.units(c.getWindow().getGuiScaledWidth(),c.getWindow().getGuiScaledHeight());var hit=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("naval_force_band")&&h.tip().startsWith(force.name()+"\n")).findFirst().orElseThrow();return new int[]{hit.x()*2,(hit.y()-u.size(1))*2,u.size(16.5)*2,u.size(16)*2};});
        try{var pixels=javax.imageio.ImageIO.read(image.toFile());int filled=0;for(int y=bounds[1];y<bounds[1]+bounds[3];y++)for(int x=bounds[0];x<bounds[0]+bounds[2];x++){int rgb=pixels.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;if(r>g+30&&r>b+30)filled++;}if(filled<10)throw new AssertionError("Selected red-fleet task force must retain its filled insignia beneath the selection outline: "+filled+" "+image);}
        catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static void assertBandSpacing(){
        var client=net.minecraft.client.Minecraft.getInstance();var u=MapCommanderLayout.units(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());var band=MapCommanderPanels.hits().stream().filter(h->h.icon().equals("naval_force_band")).toList();
        if(band.size()<2)throw new AssertionError("Spacing regression needs adjacent task forces");
        for(int i=1;i<band.size();i++){double expected=Math.ceil(u.length(3+(i+1)*15.5))-Math.ceil(u.length(3+i*15.5));if(band.get(i).visualX()-band.get(i-1).visualX()!=expected)throw new AssertionError("Task-force spacing changed from the established pixel-rounded layout");}
        for(var hit:band)if(hit.w()!=u.size(21))throw new AssertionError("Task-force input width changed from the established layout");
    }
}
