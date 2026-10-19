package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolDecisionPayloads;
import java.util.*;

public final class MapDecisionOverlay {
    private record Cached(MapSceneMesh mesh,int boxes) {}
    private final MapSceneAssembler assembler=new MapSceneAssembler();
    private final Map<String,Cached> cache=new LinkedHashMap<>();
    private String selected="",dimension="",assembling="";
    private int retainedBoxes;
    public void select(String row,String dimension,int color) {
        if(!this.dimension.equals(dimension))clear();
        this.dimension=dimension;selected=row;
        var cached=cache.get(row);MapSceneRenderer.decision(cached==null?null:cached.mesh(),color);
    }
    public boolean receive(ProtocolDecisionPayloads.Highlight packet,String dimension,int color) {
        if(!packet.page().dimension().equals(dimension) || !this.dimension.equals(dimension))return false;
        if(!assembling.equals(packet.row())) {assembler.clear();assembling=packet.row();}
        var boxes=assembler.accept(packet.page());
        if(boxes==null)return false;
        var mesh=MapSceneMesh.build(dimension,boxes);
        var previous=cache.remove(packet.row());if(previous!=null)retainedBoxes-=previous.boxes();
        while(!cache.isEmpty() && (cache.size()>=64 || retainedBoxes+boxes.size()>dev.hoi.protocol.ProtocolAtlasScene.PAGE_SIZE*dev.hoi.protocol.ProtocolAtlasScene.MAX_PAGES))
            retainedBoxes-=cache.remove(cache.keySet().iterator().next()).boxes();
        cache.put(packet.row(),new Cached(mesh,boxes.size()));retainedBoxes+=boxes.size();
        if(packet.row().equals(selected))MapSceneRenderer.decision(mesh,color);
        return true;
    }
    public void clear() {
        selected="";dimension="";assembling="";retainedBoxes=0;cache.clear();assembler.clear();MapSceneRenderer.decision(null,0xffffffff);
    }
}
