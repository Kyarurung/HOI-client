package dev.hoi.client.map;

import dev.hoi.protocol.AtlasSceneProtocol;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AtlasSceneAssemblerTest {
    private static final AtlasSceneProtocol.Box BOX=new AtlasSceneProtocol.Box(0,66,0,1,.025f,1,0,AtlasSceneProtocol.Material.BLACK);
    @Test void publicationIsAtomicAndRejectsMixedOrOutOfOrderTransfers() {
        var assembler=new AtlasSceneAssembler();var id=UUID.randomUUID();var other=UUID.randomUUID();
        assertNull(assembler.accept(new AtlasSceneProtocol.Page(id,"hoi:army",0,2,List.of(BOX))));
        assertNull(assembler.accept(new AtlasSceneProtocol.Page(other,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new AtlasSceneProtocol.Page(id,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new AtlasSceneProtocol.Page(id,"hoi:army",0,2,List.of(BOX))));
        assertEquals(List.of(BOX,BOX),assembler.accept(new AtlasSceneProtocol.Page(id,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new AtlasSceneProtocol.Page(id,"hoi:army",1,2,List.of(BOX))));
        assertNull(assembler.accept(new AtlasSceneProtocol.Page(id,"hoi:army",0,2,List.of(BOX))));
        assertNull(assembler.accept(new AtlasSceneProtocol.Page(id,"hoi:navy",1,2,List.of(BOX))));
        assertEquals(List.of(BOX),assembler.accept(new AtlasSceneProtocol.Page(other,"hoi:air",0,1,List.of(BOX))));
    }
    @Test void geometryAndPageAllocationAreBounded() {
        var id=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new AtlasSceneProtocol.Box(Float.NaN,0,0,1,1,1,0,AtlasSceneProtocol.Material.WATER));
        assertThrows(IllegalArgumentException.class,()->new AtlasSceneProtocol.Box(0,0,0,-1,1,1,0,AtlasSceneProtocol.Material.WATER));
        assertThrows(IllegalArgumentException.class,()->new AtlasSceneProtocol.Page(id,"hoi:army",0,257,List.of()));
        assertThrows(IllegalArgumentException.class,()->new AtlasSceneProtocol.Page(id,"hoi:army",0,1,Collections.nCopies(1025,BOX)));
        var mutable=new ArrayList<>(List.of(BOX));var page=new AtlasSceneProtocol.Page(id,"hoi:army",0,1,mutable);mutable.clear();
        assertEquals(1,page.boxes().size());
    }
}
