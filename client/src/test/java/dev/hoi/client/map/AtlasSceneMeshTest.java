package dev.hoi.client.map;

import dev.hoi.protocol.AtlasSceneProtocol.Box;
import dev.hoi.protocol.AtlasSceneProtocol.Material;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AtlasSceneMeshTest {
    @Test void indexedSelectionMatchesFullScanIncludingLargeRotatedAndNegativeTiles() {
        var boxes=new ArrayList<Box>();
        for(int i=-100;i<=100;i++)boxes.add(new Box(i*32,70,i%3*32,2,3,4,i*.1f,Material.BLACK));
        boxes.add(new Box(-512,70,8,1024,1,1,.3f,Material.DMZ));
        var scene=AtlasSceneMesh.build("hoi:army",boxes);
        for(var camera:List.of(new Vec3(-16,100,-16),new Vec3(0,140,0),new Vec3(3000,70,0))) {
            var selected=new ArrayList<AtlasSceneMesh.Tile>();
            int examined=scene.collect(camera,selected);
            var expected=scene.tiles().stream().filter(tile->tile.bounds().distanceToSqr(camera)<=4096).toList();
            assertEquals(new HashSet<>(expected),new HashSet<>(selected));
            assertEquals(selected.size(),new HashSet<>(selected).size());
            assertTrue(examined<scene.tiles().size()/4);
        }
    }
    @Test void cityModelsStaySeparateAndEmittedVerticesRemainWithinTileBounds() {
        var city=new Box(-1,70,-1,3,2,4,0,Material.CITY);
        var scene=AtlasSceneMesh.build("hoi:army",List.of(city,
                new Box(1,70,1,2,3,4,.7f,Material.FOREST),new Box(1,70,1,2,3,4,.7f,Material.BLACK)));
        var cityTile=scene.tiles().stream().filter(t->t.material()==Material.CITY).findFirst().orElseThrow();
        assertEquals(List.of(city),cityTile.cities());assertEquals(0,cityTile.vertices().length);
        for(var tile:scene.tiles()) {
            var v=tile.vertices();
            if(tile.material()==Material.FOREST)assertEquals(16*8,v.length);
            if(tile.material()==Material.BLACK)assertEquals(24*8,v.length);
            for(int i=0;i<v.length;i+=8)assertTrue(tile.bounds().inflate(.00001).contains(v[i],v[i+1],v[i+2]));
        }
    }
}
