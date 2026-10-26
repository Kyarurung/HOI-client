package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolAtlasScene.Box;
import dev.hoi.protocol.ProtocolAtlasScene.Material;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MapSceneMeshTest {
    @Test void colocatedOrdinaryAndMissionMovementKeepSeparateTexturesAndIdenticalUvs() {
        var ordinary=new dev.hoi.protocol.ProtocolCommanderHud.MovementQuad(1,64,1,2,4,0,.125f,.5f,1,1);
        var mission=new dev.hoi.protocol.ProtocolCommanderHud.MovementQuad(1,64,1,2,4,0,.125f,.5f,1,1,dev.hoi.protocol.ProtocolCommanderHud.Arrow.MISSION);
        var tiles=MapSceneMesh.build("hoi:army",List.of(),List.of(ordinary,mission)).tiles();
        assertEquals(2,tiles.size());assertTrue(tiles.stream().allMatch(MapSceneMesh.MapTile::movement));
        assertEquals(dev.hoi.protocol.ProtocolCommanderHud.Arrow.MOVE,tiles.getFirst().arrow());assertEquals(dev.hoi.protocol.ProtocolCommanderHud.Arrow.MISSION,tiles.getLast().arrow());
        assertArrayEquals(tiles.getFirst().vertices(),tiles.getLast().vertices());
        var retreat=new dev.hoi.protocol.ProtocolCommanderHud.MovementQuad(1,64,1,2,4,0,.125f,.5f,1,1,dev.hoi.protocol.ProtocolCommanderHud.Arrow.RETREAT);
        var third=MapSceneMesh.build("hoi:army",List.of(),List.of(ordinary,mission,retreat)).tiles();
        assertEquals(3,third.size());assertArrayEquals(third.getFirst().vertices(),third.getLast().vertices());
        assertEquals("hoi:textures/gui/map/retreat_arrow.png",MapSceneRenderer.movementTexture(third.getLast().arrow()).toString());
    }
    @Test void cubeFacesKeepTheirWindingUvAndNormals() {
        var mesh = MapSceneMesh.build("hoi:army", List.of(new Box(2, 3, 5, 7, 11, 13, 0, Material.BLACK)));
        var vertices = mesh.tiles().getFirst().vertices();
        assertArrayEquals(new float[]{2, 14, 5, 0, 0, 0, 1, 0, 2, 14, 18, 0, 1, 0, 1, 0,
                9, 14, 18, 1, 1, 0, 1, 0, 9, 14, 5, 1, 0, 0, 1, 0}, Arrays.copyOf(vertices, 32));
        assertEquals(24 * 8, vertices.length);
        for (int face = 0; face < 6; face++) {
            float[] normal = switch (face) {
                case 0 -> new float[]{0, 1, 0}; case 1 -> new float[]{0, -1, 0};
                case 2 -> new float[]{0, 0, -1}; case 3 -> new float[]{0, 0, 1};
                case 4 -> new float[]{-1, 0, 0}; default -> new float[]{1, 0, 0};
            };
            for (int vertex = 0; vertex < 4; vertex++)
                assertArrayEquals(normal, Arrays.copyOfRange(vertices, (face * 4 + vertex) * 8 + 5, (face * 4 + vertex) * 8 + 8));
        }
    }
    @Test void indexedSelectionMatchesFullScanIncludingLargeRotatedAndNegativeTiles() {
        var boxes=new ArrayList<Box>();
        for(int i=-100;i<=100;i++)boxes.add(new Box(i*32,70,i%3*32,2,3,4,i*.1f,Material.BLACK));
        boxes.add(new Box(-512,70,8,1024,1,1,.3f,Material.DMZ));
        var scene=MapSceneMesh.build("hoi:army",boxes);
        for(var camera:List.of(new Vec3(-16,100,-16),new Vec3(0,140,0),new Vec3(3000,70,0))) {
            var selected=new ArrayList<MapSceneMesh.MapTile>();
            int examined=scene.collect(camera,selected);
            var expected=scene.tiles().stream().filter(tile->tile.bounds().distanceToSqr(camera)<=4096).toList();
            assertEquals(new HashSet<>(expected),new HashSet<>(selected));
            assertEquals(selected.size(),new HashSet<>(selected).size());
            assertTrue(examined<scene.tiles().size()/4);
        }
    }
    @Test void cityModelsStaySeparateAndEmittedVerticesRemainWithinTileBounds() {
        var city=new Box(-1,70,-1,3,2,4,0,Material.CITY);
        var scene=MapSceneMesh.build("hoi:army",List.of(city,
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
