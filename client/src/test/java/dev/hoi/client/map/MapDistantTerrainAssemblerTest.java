package dev.hoi.client.map;
import dev.hoi.protocol.ProtocolAtlasTerrain;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MapDistantTerrainAssemblerTest {
    private static ProtocolAtlasTerrain.Page page(UUID id, int index, int count, String dimension) {
        return new ProtocolAtlasTerrain.Page(id, dimension, index, count, List.of(new ProtocolAtlasTerrain.Strip(index, 0, 1, 65, 66, 1)));
    }
    @Test void publishesOnlyCompleteContiguousRevisions() {
        var assembler = new MapDistantTerrainAssembler(); var id = UUID.randomUUID();
        assertNull(assembler.accept(page(id, 0, 2, "hoi:atlas_army")));
        assertEquals(2, assembler.accept(page(id, 1, 2, "hoi:atlas_army")).size());
        assertNull(assembler.accept(page(id, 1, 2, "hoi:atlas_army")));
    }
    @Test void rejectsMixedDimensionsRevisionsAndMissingPages() {
        var assembler = new MapDistantTerrainAssembler(); var id = UUID.randomUUID();
        assembler.accept(page(id, 0, 2, "hoi:atlas_army"));
        assertNull(assembler.accept(page(id, 1, 2, "hoi:atlas_supply")));
        assembler.accept(page(id, 0, 3, "hoi:atlas_army"));
        assertNull(assembler.accept(page(id, 2, 3, "hoi:atlas_army")));
        assembler.accept(page(id, 0, 2, "hoi:atlas_army"));
        assertNull(assembler.accept(page(UUID.randomUUID(), 1, 2, "hoi:atlas_army")));
        assembler.accept(page(id, 0, 2, "hoi:atlas_army")); assembler.clear();
        assertNull(assembler.accept(page(id, 1, 2, "hoi:atlas_army")));
    }
}
