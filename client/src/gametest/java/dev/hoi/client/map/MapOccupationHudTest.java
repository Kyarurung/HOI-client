package dev.hoi.client.map;

import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.protocol.occupation.OccupationPayloads;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class MapOccupationHudTest {
    private MapOccupationHudTest() {}
    public static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        for(double distance:List.of(12.0,24.0))for(boolean compliance:List.of(false,true)) {
            context.runOnClient(client->{
                var camera=client.gameRenderer.mainCamera();
                var center=camera.position().add(new Vec3(camera.forwardVector()).scale(distance));
                var rows=new ArrayList<OccupationPayloads.Row>();
                for(int i=0;i<3;i++)rows.add(new OccupationPayloads.Row("test/"+i,"점령지 "+i,"KOR","대한민국",
                        new double[]{0,.125,1}[i],new double[]{1,.3333,0}[i],0,0,"","","저항도","순응도",List.of(),List.of(),center.x,center.y-3.1,center.z));
                MapOccupationHud.accept(OccupationPayloads.MapResponse.of(new OccupationPayloads.MapView(client.level.dimension().identifier().toString(),compliance,rows)).view());
            });
            context.setScreen(MapCursor::new);context.waitTicks(3);
            context.runOnClient(client->{
                var labels=MapOccupationHud.labels();
                if(labels.size()!=3)throw new AssertionError("Three visible map percentages required: "+labels);
                var expected=compliance?Set.of("100%","33.33%","0%"):Set.of("0%","12.5%","100%");
                if(!new HashSet<>(labels.stream().map(MapOccupationHud.Label::value).toList()).equals(expected))throw new AssertionError("Fraction must become percent exactly once");
                for(int i=0;i<labels.size();i++)for(int j=i+1;j<labels.size();j++)if(labels.get(i).overlaps(labels.get(j)))throw new AssertionError("Projected percentage labels overlap");
            });
            context.takeScreenshot("hoi-map-"+(compliance?"compliance":"resistance")+"-percent-distance-"+(int)distance+"-gui2");
        }
        context.runOnClient(client->MapOccupationHud.accept(new OccupationPayloads.MapView(client.level.dimension().identifier().toString(),false,List.of())));
        context.waitTicks(2);
        context.runOnClient(client->{if(!MapOccupationHud.labels().isEmpty())throw new AssertionError("Closing occupation map retains stale percentages");MapOccupationHud.reset();});
        context.setScreen(()->null);context.getInput().resizeWindow(1600,1000);
    }
}
