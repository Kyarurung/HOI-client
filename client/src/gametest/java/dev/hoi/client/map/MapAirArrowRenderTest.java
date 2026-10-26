package dev.hoi.client.map;

import com.google.gson.Gson;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class MapAirArrowRenderTest {
    record Fixture(String mode,String country,List<String> selected,Map<String,List<AirArrow>> states) {}
    record Evidence(String fixture,String hash,String state,int width,int height,int arrows,int changedPixels,String image) {}
    private MapAirArrowRenderTest() {}
    static void run(ClientGameTestContext game,Consumer<String> command)throws java.io.IOException {
        String directory=System.getenv("HOI_MISSION_OVERLAY_FIXTURES");if(directory==null||directory.isBlank())throw new AssertionError("Air arrow fixtures required");
        var evidence=new ArrayList<Evidence>();
        for(String name:List.of("air-arrows","air-transfer")){
            var path=Path.of(directory,name+".json");String body=Files.readString(path);var fixture=new Gson().fromJson(body,Fixture.class);
            String hash;try{hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));}catch(java.security.NoSuchAlgorithmException failure){throw new IllegalStateException(failure);}
            var original=fixture.states().get("selected");if(original==null||original.isEmpty()||name.equals("air-arrows")&&!fixture.states().get("cleared").isEmpty())throw new AssertionError("Source selected/cleared air states required");
            if(name.equals("air-arrows")&&(!fixture.states().get("nonactive").stream().allMatch(a->a.state()==AirArrowState.NONACTIVE)||!fixture.states().get("unselected").stream().allMatch(a->a.state()==AirArrowState.ACTIVE)))throw new AssertionError("Source gray and green states required");
            var points=fixture.states().values().stream().flatMap(List::stream).flatMap(a->java.util.stream.Stream.of(a.from(),a.to())).toList();
            double minX=points.stream().mapToDouble(PlanPoint::x).min().orElseThrow(),minZ=points.stream().mapToDouble(PlanPoint::z).min().orElseThrow(),maxX=points.stream().mapToDouble(PlanPoint::x).max().orElseThrow(),maxZ=points.stream().mapToDouble(PlanPoint::z).max().orElseThrow();
            double dx=6-minX,dz=6-minZ,width=maxX-minX+12,depth=maxZ-minZ+12,span=Math.max(width,depth);if(span>128)throw new AssertionError("Local source air fixture required: "+span);
            int extent=(int)Math.ceil(span);command.accept("fill 0 249 0 "+extent+" 249 "+extent+" minecraft:stone strict");command.accept("tp @a "+width/2+" "+(250+Math.max(20,span*.95))+" "+depth/2+" 0 90");game.waitTicks(4);
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(client->{client.options.guiScale().set(2);if(!client.gui.hud.isHidden())client.gui.hud.toggle();MapCommanderHud.reset();MapUnitHud.reset();MapSceneRenderer.clear();ClientCampaignHud.accept(ProtocolHudPayloads.State.of(fixture.country(),ProtocolCountryHud.UNKNOWN));receive(fixture.country(),List.of());});game.waitTicks(4);
                String suffix=name+"-"+size[0]+"x"+size[1]+"-gui2";var clear=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-air-source-cleared-"+suffix).toFile());
                for(String state:name.equals("air-transfer")?List.of("selected"):List.of("selected","unselected","nonactive")){
                    var source=fixture.states().get(state);if(source==null)throw new AssertionError("Missing source state: "+name+"/"+state);
                    var arrows=source.stream().map(a->new AirArrow(a.wing(),a.kind(),a.state(),point(a.from(),dx,dz),point(a.to(),dx,dz))).toList();
                    game.runOnClient(client->{receive(fixture.country(),arrows);if(Math.abs(client.getWindow().getGuiScale()-2)>.0001)throw new AssertionError("Actual GUI2 required");});game.waitFor(client->MapSceneRenderer.visibleTileCount()>0);game.waitTicks(4);
                    Path shot=game.takeScreenshot("hoi-air-source-"+state+"-"+suffix);var picture=javax.imageio.ImageIO.read(shot.toFile());int changed=0,colored=0;
                    for(int y=0;y<picture.getHeight();y++)for(int x=0;x<picture.getWidth();x++){
                        int pixel=picture.getRGB(x,y),base=clear.getRGB(x,y),r=pixel>>16&255,g=pixel>>8&255,b=pixel&255,difference=Math.abs(r-(base>>16&255))+Math.abs(g-(base>>8&255))+Math.abs(b-(base&255));
                        if(difference>15){changed++;if(state.equals("nonactive")?Math.abs(r-g)<8&&Math.abs(g-b)<8:state.equals("unselected")?g>r*1.2&&g>b*1.2:r>b*1.2&&g>b*1.2)colored++;}
                    }
                    if(changed<30||colored<20)throw new AssertionError("Original masked air arrow must render "+state+": "+changed+"/"+colored);
                    evidence.add(new Evidence(name,hash,state,size[0],size[1],arrows.size(),changed,shot.toString()));
                    game.runOnClient(client->receive("PRK",List.of()));game.waitTicks(2);game.runOnClient(client->{if(MapSceneRenderer.visibleTileCount()==0)throw new AssertionError("Foreign cancellation removed private air arrows");MapSceneRenderer.resourcesReloaded();});game.waitTicks(3);
                    game.runOnClient(client->{if(MapSceneRenderer.visibleTileCount()==0||MapAirArrowOverlay.texture(arrows.getFirst().kind())==null)throw new AssertionError("Pack reload must restore original masked textures");});
                }
                game.runOnClient(client->receive(fixture.country(),List.of()));game.waitFor(client->MapSceneRenderer.visibleTileCount()==0);game.takeScreenshot("hoi-air-source-cancelled-"+suffix);
            }
            game.runOnClient(client->MapSceneRenderer.clearPlans());command.accept("fill 0 249 0 "+extent+" 249 "+extent+" minecraft:air strict");
        }
        Files.writeString(Path.of(evidence.getFirst().image()).getParent().resolve("hoi-air-source-evidence.json"),new Gson().toJson(evidence));
    }
    private static PlanPoint point(PlanPoint p,double dx,double dz){return new PlanPoint((float)(p.x()+dx),p.y()+186,(float)(p.z()+dz));}
    private static void receive(String country,List<AirArrow> air){MapSceneRenderer.receivePlans(country,new ProtocolAtlasScene.Page(UUID.randomUUID(),"minecraft:overworld",0,1,List.of()),List.of(),air);}
}
