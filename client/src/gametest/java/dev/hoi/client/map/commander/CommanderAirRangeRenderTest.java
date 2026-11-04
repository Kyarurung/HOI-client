package dev.hoi.client.map.commander;

import com.google.gson.*;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.client.map.air.AirRangeOverlay;
import dev.hoi.client.map.naval.NavalAirHoverTest;
import dev.hoi.client.map.scene.SceneRenderer;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.hud.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class CommanderAirRangeRenderTest {
    private CommanderAirRangeRenderTest() {}
    public static void run(ClientGameTestContext game,Consumer<String> command){
        try{
            var file=Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-command-panel-air.json");var json=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            var original=new Snapshot(json.toString()).view();var first=original.wings().getFirst();var range=first.data().air().wing().range();if(range==null)throw new AssertionError("Authoritative air range required");
            String duplicate=original.wings().stream().filter(w->!w.id().equals(first.id())&&range.equals(w.data().air().wing().range())).findFirst().orElseThrow().id();
            String other=original.wings().stream().filter(w->w.data().air().wing().range()!=null&&!range.equals(w.data().air().wing().range())).findFirst().orElseThrow().id();
            for(var element:json.getAsJsonArray("wings")){var value=element.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("air").getAsJsonObject("wing").getAsJsonObject("range");if(value!=null){value.addProperty("x",value.get("x").getAsFloat()+48-range.x());value.addProperty("z",value.get("z").getAsFloat()+48-range.z());}}
            var source=new Snapshot(json.toString()).view();command.accept("gamemode spectator @a");command.accept("time set noon");command.accept("weather clear");command.accept("tp @a 48 300 48 0 90");game.waitTicks(30);command.accept("fill 0 249 0 96 249 96 minecraft:stone strict");game.waitTicks(5);
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(client->{client.options.guiScale().set(2);client.options.fov().set(60);client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);if(client.gui.hud.isHidden())client.gui.hud.toggle();SceneRenderer.clear();CommanderHud.reset();CommanderUnitHud.reset();ClientCampaignHud.accept(HudPayloads.State.of("KOR",CountryHud.UNKNOWN));CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),Mode.AIR,source.cards(),source.wings()));CommanderPanels.showWings(List.of(first.id()));});
                game.setScreen(MapCursor::new);game.getInput().setCursorPos(size[0]-3,size[1]-3);game.runOnClient(client->{client.player.setXRot(90);client.player.setYRot(0);});game.waitTicks(8);String suffix=size[0]+"x"+size[1]+"-gui2";check(game,1);Path single=game.takeScreenshot("hoi-air-range-single-"+suffix);
                game.runOnClient(client->CommanderPanels.showWings(List.of(first.id(),duplicate)));game.waitTicks(3);check(game,1);game.takeScreenshot("hoi-air-range-deduplicated-"+suffix);
                game.runOnClient(client->CommanderPanels.showWings(List.of(first.id(),duplicate,other)));game.waitTicks(3);check(game,2);Path multiple=game.takeScreenshot("hoi-air-range-multiple-"+suffix);
                game.runOnClient(client->CommanderHud.closeSelection());game.waitTicks(3);check(game,0);Path cleared=game.takeScreenshot("hoi-air-range-cleared-"+suffix);
                var green=javax.imageio.ImageIO.read(single.toFile());var yellow=javax.imageio.ImageIO.read(multiple.toFile());var empty=javax.imageio.ImageIO.read(cleared.toFile());int greenPixels=0,yellowPixels=0;
                for(int y=size[1]/4;y<size[1]*3/4;y++)for(int x=size[0]/2;x<size[0]*4/5;x++){int a=green.getRGB(x,y),b=yellow.getRGB(x,y);if((a>>8&255)>(a>>16&255)*1.15&&(a>>8&255)>(a&255)*1.1&&a!=empty.getRGB(x,y))greenPixels++;if((b>>16&255)>100&&(b>>8&255)>100&&(b&255)<(b>>8&255)*.8&&b!=empty.getRGB(x,y))yellowPixels++;}
                if(greenPixels<15||yellowPixels<15)throw new AssertionError("Original dashed range must render green/yellow and disappear: "+greenPixels+"/"+yellowPixels);
                int center=green.getRGB(size[0]/2,size[1]/2),plain=empty.getRGB(size[0]/2,size[1]/2);if((center>>8&255)<=(plain>>8&255))throw new AssertionError("Range interior must be brighter than the same cleared terrain");
                NavalAirHoverTest.run(game,source,size);
                game.runOnClient(client->{CommanderPanels.showWings(List.of(first.id()));ClientCampaignHud.accept(HudPayloads.State.of("PRK",CountryHud.UNKNOWN));});game.waitTicks(3);check(game,0);game.takeScreenshot("hoi-air-range-country-revoked-"+suffix);
                Files.writeString(cleared.getParent().resolve("hoi-air-range-evidence-"+suffix+".json"),new Gson().toJson(Map.of("source",file.toString(),"sha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))),"rangeKm",range.km(),"radiusBlocks",range.radius(),"sourceX",range.x(),"sourceZ",range.z(),"translatedX",48,"translatedZ",48,"greenPixels",greenPixels,"yellowPixels",yellowPixels)));
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}finally{game.runOnClient(client->{CommanderHud.reset();CommanderUnitHud.reset();SceneRenderer.clear();});}
    }
    private static void check(ClientGameTestContext game,int count){game.runOnClient(client->{if(Math.abs(client.getWindow().getGuiScale()-2)>.001||AirRangeOverlay.extract(client.level,CommanderPanels.airRanges()).outline().size()!=count)throw new AssertionError("Range scope, deduplication and GUI2 must match");});}
}
