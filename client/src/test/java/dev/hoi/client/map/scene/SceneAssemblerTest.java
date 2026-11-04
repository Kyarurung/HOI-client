package dev.hoi.client.map.scene;

import dev.hoi.protocol.map.MapAtlasScene;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SceneAssemblerTest {
    private static final MapAtlasScene.Box BOX=new MapAtlasScene.Box(0,66,0,1,.025f,1,0,MapAtlasScene.Material.BLACK);
    @Test void publicationIsAtomicAndRejectsMixedOrOutOfOrderTransfers() {
        var assembler=new SceneAssembler();var id=UUID.randomUUID();var other=UUID.randomUUID();
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:army",0,2,List.of(BOX))));
        assertNull(assembler.accept(new MapAtlasScene.Page(other,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:army",0,2,List.of(BOX))));
        assertEquals(List.of(BOX,BOX),assembler.accept(new MapAtlasScene.Page(id,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:army",0,2,List.of(BOX))));
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:navy",1,2,List.of(BOX))));
        assertEquals(List.of(BOX),assembler.accept(new MapAtlasScene.Page(other,"hoi:air",0,1,List.of(BOX))));
    }
    @Test void geometryAndPageAllocationAreBounded() {
        var id=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new MapAtlasScene.Box(Float.NaN,0,0,1,1,1,0,MapAtlasScene.Material.WATER));
        assertThrows(IllegalArgumentException.class,()->new MapAtlasScene.Box(0,0,0,-1,1,1,0,MapAtlasScene.Material.WATER));
        assertThrows(IllegalArgumentException.class,()->new MapAtlasScene.Page(id,"hoi:army",0,257,List.of()));
        assertThrows(IllegalArgumentException.class,()->new MapAtlasScene.Page(id,"hoi:army",0,1,Collections.nCopies(1025,BOX)));
        var mutable=new ArrayList<>(List.of(BOX));var page=new MapAtlasScene.Page(id,"hoi:army",0,1,mutable);mutable.clear();
        assertEquals(1,page.boxes().size());
    }
    @Test void movementUvAndBoxesPublishTogetherAndMixedScenesDiscardBoth(){
        var assembler=new SceneAssembler();var first=UUID.randomUUID();var other=UUID.randomUUID();var q=new dev.hoi.protocol.commander.CommanderHudPayloads.MovementQuad(0,65,0,1,3,0,.125f,.5f,.7f,1);
        assertNull(assembler.accept(new MapAtlasScene.Page(first,"hoi:army",0,2,List.of(BOX)),List.of(q)));assertNull(assembler.accept(new MapAtlasScene.Page(other,"hoi:army",1,2,List.of()),List.of(q)));
        assertNull(assembler.accept(new MapAtlasScene.Page(first,"hoi:army",0,2,List.of(BOX)),List.of(q)));var result=assembler.accept(new MapAtlasScene.Page(first,"hoi:army",1,2,List.of()),List.of(q));assertEquals(List.of(BOX),result.boxes());assertEquals(List.of(q,q),result.movement());var mesh=SceneMesh.build("hoi:army",result.boxes(),result.movement());assertTrue(mesh.tiles().stream().anyMatch(SceneMesh.SceneTile::movement));var vertices=mesh.tiles().stream().filter(SceneMesh.SceneTile::movement).findFirst().orElseThrow().vertices();assertEquals(.125f,vertices[3]);assertEquals(.5f,vertices[4]);assertEquals(.7f,vertices[11]);
    }
    @Test void airArrowsPublishWithCompleteSceneAndRejectLaterPageInjection(){
        var assembler=new SceneAssembler();var id=UUID.randomUUID();var arrow=new dev.hoi.protocol.commander.CommanderHudPayloads.AirArrow("wing",dev.hoi.protocol.commander.CommanderHudPayloads.AirArrowKind.MISSION,dev.hoi.protocol.commander.CommanderHudPayloads.AirArrowState.NONACTIVE,new dev.hoi.protocol.commander.CommanderHudPayloads.PlanPoint(1,64,1),new dev.hoi.protocol.commander.CommanderHudPayloads.PlanPoint(10,64,5));
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:air",0,2,List.of()),List.of(),List.of(arrow)));
        var complete=assembler.accept(new MapAtlasScene.Page(id,"hoi:air",1,2,List.of()),List.of(),List.of());assertEquals(List.of(arrow),complete.air());
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:air",0,2,List.of()),List.of(),List.of(arrow)));
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:air",1,2,List.of()),List.of(),List.of(arrow)));
        assertNull(assembler.accept(new MapAtlasScene.Page(id,"hoi:air",1,2,List.of()),List.of(),List.of()));
        assertTrue(assembler.accept(new MapAtlasScene.Page(UUID.randomUUID(),"hoi:air",0,1,List.of()),List.of(),List.of()).air().isEmpty());
    }

}
