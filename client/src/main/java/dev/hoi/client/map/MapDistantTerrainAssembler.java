package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolAtlasTerrain;
import java.util.*;

final class MapDistantTerrainAssembler {
    private UUID revision;
    private String dimension;
    private int count, next;
    private final List<ProtocolAtlasTerrain.Strip> staging = new ArrayList<>();
    List<ProtocolAtlasTerrain.Strip> accept(ProtocolAtlasTerrain.Page page) {
        if (page.index() == 0) { clear(); revision = page.revision(); dimension = page.dimension(); count = page.count(); }
        if (!page.revision().equals(revision) || !page.dimension().equals(dimension) || page.count() != count || page.index() != next) {
            clear(); return null;
        }
        staging.addAll(page.strips()); next++;
        if (next != count) return null;
        var result = List.copyOf(staging); clear(); return result;
    }
    void clear() { revision = null; dimension = null; count = 0; next = 0; staging.clear(); }
}
