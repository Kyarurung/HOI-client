package dev.hoi.client.map.scene;

import dev.hoi.protocol.map.MapAtlasScene;
import java.util.*;

public final class SceneAssembler {
    private UUID scene;
    private String dimension;
    private int count, next;
    private final List<MapAtlasScene.Box> staging = new ArrayList<>();
    record Assembly(List<MapAtlasScene.Box> boxes,List<dev.hoi.protocol.commander.CommanderHudPayloads.MovementQuad> movement,List<dev.hoi.protocol.commander.CommanderHudPayloads.AirArrow> air) {}
    private final List<dev.hoi.protocol.commander.CommanderHudPayloads.MovementQuad> movement=new ArrayList<>();
    private List<dev.hoi.protocol.commander.CommanderHudPayloads.AirArrow> air=List.of();
    public List<MapAtlasScene.Box> accept(MapAtlasScene.Page page) {var result=accept(page,List.of());return result==null?null:result.boxes();}
    Assembly accept(MapAtlasScene.Page page,List<dev.hoi.protocol.commander.CommanderHudPayloads.MovementQuad> quads) {
        return accept(page,quads,List.of());
    }
    Assembly accept(MapAtlasScene.Page page,List<dev.hoi.protocol.commander.CommanderHudPayloads.MovementQuad> quads,List<dev.hoi.protocol.commander.CommanderHudPayloads.AirArrow> arrows) {
        if (page.index() == 0) { clear(); scene=page.scene(); dimension=page.dimension(); count=page.count(); }
        if (!page.scene().equals(scene) || !page.dimension().equals(dimension) || page.count()!=count || page.index()!=next) { clear(); return null; }
        if(arrows.size()>1024||page.index()!=0&&!arrows.isEmpty()){clear();return null;}
        if(page.index()==0)air=List.copyOf(arrows);
        staging.addAll(page.boxes()); movement.addAll(quads); next++;
        if (next != count) return null;
        var result=new Assembly(List.copyOf(staging),List.copyOf(movement),air); clear(); return result;
    }
    public void clear() { scene=null; dimension=null; count=0; next=0; staging.clear(); movement.clear(); air=List.of(); }
}
