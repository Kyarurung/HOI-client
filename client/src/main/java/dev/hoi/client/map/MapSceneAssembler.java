package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolAtlasScene;
import java.util.*;

final class MapSceneAssembler {
    private UUID scene;
    private String dimension;
    private int count, next;
    private final List<ProtocolAtlasScene.Box> staging = new ArrayList<>();
    record Assembly(List<ProtocolAtlasScene.Box> boxes,List<dev.hoi.protocol.ProtocolCommanderHud.MovementQuad> movement,List<dev.hoi.protocol.ProtocolCommanderHud.AirArrow> air) {}
    private final List<dev.hoi.protocol.ProtocolCommanderHud.MovementQuad> movement=new ArrayList<>();
    private List<dev.hoi.protocol.ProtocolCommanderHud.AirArrow> air=List.of();
    List<ProtocolAtlasScene.Box> accept(ProtocolAtlasScene.Page page) {var result=accept(page,List.of());return result==null?null:result.boxes();}
    Assembly accept(ProtocolAtlasScene.Page page,List<dev.hoi.protocol.ProtocolCommanderHud.MovementQuad> quads) {
        return accept(page,quads,List.of());
    }
    Assembly accept(ProtocolAtlasScene.Page page,List<dev.hoi.protocol.ProtocolCommanderHud.MovementQuad> quads,List<dev.hoi.protocol.ProtocolCommanderHud.AirArrow> arrows) {
        if (page.index() == 0) { clear(); scene=page.scene(); dimension=page.dimension(); count=page.count(); }
        if (!page.scene().equals(scene) || !page.dimension().equals(dimension) || page.count()!=count || page.index()!=next) { clear(); return null; }
        if(arrows.size()>1024||page.index()!=0&&!arrows.isEmpty()){clear();return null;}
        if(page.index()==0)air=List.copyOf(arrows);
        staging.addAll(page.boxes()); movement.addAll(quads); next++;
        if (next != count) return null;
        var result=new Assembly(List.copyOf(staging),List.copyOf(movement),air); clear(); return result;
    }
    void clear() { scene=null; dimension=null; count=0; next=0; staging.clear(); movement.clear(); air=List.of(); }
}
