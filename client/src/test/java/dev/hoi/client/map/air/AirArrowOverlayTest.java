package dev.hoi.client.map.air;

import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AirArrowOverlayTest {
    @Test void originalUvsFaceUpwardAndGeometryIsReusedUntilTransferAnimationChanges(){
        for(var kind:AirArrowKind.values())for(var to:List.of(new PlanPoint(10,64,1),new PlanPoint(-10,64,-5),new PlanPoint(1,70,10))){
            var arrow=new AirArrow("wing",kind,AirArrowState.NONACTIVE,new PlanPoint(1,64,1),to);var meshes=AirArrowOverlay.build(List.of(arrow),0);var tile=meshes.getFirst().tile();var v=tile.vertices();
            for(int i=0;i<v.length;i+=32){double ax=v[i+8]-v[i],az=v[i+10]-v[i+2],bx=v[i+16]-v[i],bz=v[i+18]-v[i+2];assertTrue(az*bx-ax*bz>0,"Arrow must face up");}
            for(int i=0;i<v.length;i+=8){assertTrue(tile.bounds().contains(v[i],v[i+1],v[i+2]));assertTrue(v[i+3]>=0&&v[i+3]<=1);assertTrue(v[i+4]>=0&&v[i+4]<=1);}
            assertSame(meshes,AirArrowOverlay.build(List.of(arrow),.01));
            if(kind==AirArrowKind.MISSION)assertSame(meshes,AirArrowOverlay.build(List.of(arrow),10));else assertNotSame(meshes,AirArrowOverlay.build(List.of(arrow),10));
        }
        assertEquals(0x33ffffff,AirArrowOverlay.color(AirArrowState.NONACTIVE));assertTrue(AirArrowOverlay.build(List.of(),0).isEmpty());
        var same=new PlanPoint(1,64,1);assertTrue(AirArrowOverlay.build(List.of(new AirArrow("wing",AirArrowKind.MISSION,AirArrowState.ACTIVE,same,same)),0).isEmpty());
    }
}
