package dev.hoi.client.map;

import dev.hoi.protocol.AtlasSceneProtocol;
import java.util.*;


final class AtlasSceneAssembler {
    private UUID scene;
    private String dimension;
    private int count, next;
    private final List<AtlasSceneProtocol.Box> staging = new ArrayList<>();
    List<AtlasSceneProtocol.Box> accept(AtlasSceneProtocol.Page page) {
        if (page.index() == 0) { clear(); scene=page.scene(); dimension=page.dimension(); count=page.count(); }
        if (!page.scene().equals(scene) || !page.dimension().equals(dimension) || page.count()!=count || page.index()!=next) { clear(); return null; }
        staging.addAll(page.boxes()); next++;
        if (next != count) return null;
        var result=List.copyOf(staging); clear(); return result;
    }
    void clear() { scene=null; dimension=null; count=0; next=0; staging.clear(); }
}
