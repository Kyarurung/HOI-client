package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolAtlasScene;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapSceneAssemblerTest {
    private static final ProtocolAtlasScene.Box BOX=new ProtocolAtlasScene.Box(0,66,0,1,.025f,1,0,ProtocolAtlasScene.Material.BLACK);
    @Test void publicationIsAtomicAndRejectsMixedOrOutOfOrderTransfers() {
        var assembler=new MapSceneAssembler();var id=UUID.randomUUID();var other=UUID.randomUUID();
        assertNull(assembler.accept(new ProtocolAtlasScene.Page(id,"hoi:army",0,2,List.of(BOX))));
        assertNull(assembler.accept(new ProtocolAtlasScene.Page(other,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new ProtocolAtlasScene.Page(id,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new ProtocolAtlasScene.Page(id,"hoi:army",0,2,List.of(BOX))));
        assertEquals(List.of(BOX,BOX),assembler.accept(new ProtocolAtlasScene.Page(id,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new ProtocolAtlasScene.Page(id,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new ProtocolAtlasScene.Page(id,"hoi:army",0,2,List.of(BOX))));
        assertNull(assembler.accept(new ProtocolAtlasScene.Page(id,"hoi:navy",1,2,List.of(BOX))));
        assertEquals(List.of(BOX),assembler.accept(new ProtocolAtlasScene.Page(other,"hoi:air",0,1,List.of(BOX))));
    }
    @Test void geometryAndPageAllocationAreBounded() {
        var id=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new ProtocolAtlasScene.Box(Float.NaN,0,0,1,1,1,0,ProtocolAtlasScene.Material.WATER));
        assertThrows(IllegalArgumentException.class,()->new ProtocolAtlasScene.Box(0,0,0,-1,1,1,0,ProtocolAtlasScene.Material.WATER));
        assertThrows(IllegalArgumentException.class,()->new ProtocolAtlasScene.Page(id,"hoi:army",0,257,List.of()));
        assertThrows(IllegalArgumentException.class,()->new ProtocolAtlasScene.Page(id,"hoi:army",0,1,Collections.nCopies(1025,BOX)));
        var mutable=new ArrayList<>(List.of(BOX));var page=new ProtocolAtlasScene.Page(id,"hoi:army",0,1,mutable);mutable.clear();
        assertEquals(1,page.boxes().size());
    }
    @Test void movementUvAndBoxesPublishTogetherAndMixedScenesDiscardBoth(){
        var assembler=new MapSceneAssembler();var first=UUID.randomUUID();var other=UUID.randomUUID();var q=new dev.hoi.protocol.ProtocolCommanderHud.MovementQuad(0,65,0,1,3,0,.125f,.5f,.7f,1);
        assertNull(assembler.accept(new ProtocolAtlasScene.Page(first,"hoi:army",0,2,List.of(BOX)),List.of(q)));assertNull(assembler.accept(new ProtocolAtlasScene.Page(other,"hoi:army",1,2,List.of()),List.of(q)));
        assertNull(assembler.accept(new ProtocolAtlasScene.Page(first,"hoi:army",0,2,List.of(BOX)),List.of(q)));var result=assembler.accept(new ProtocolAtlasScene.Page(first,"hoi:army",1,2,List.of()),List.of(q));assertEquals(List.of(BOX),result.boxes());assertEquals(List.of(q,q),result.movement());var mesh=MapSceneMesh.build("hoi:army",result.boxes(),result.movement());assertTrue(mesh.tiles().stream().anyMatch(MapSceneMesh.MapTile::movement));var vertices=mesh.tiles().stream().filter(MapSceneMesh.MapTile::movement).findFirst().orElseThrow().vertices();assertEquals(.125f,vertices[3]);assertEquals(.5f,vertices[4]);assertEquals(.7f,vertices[11]);
    }

}
