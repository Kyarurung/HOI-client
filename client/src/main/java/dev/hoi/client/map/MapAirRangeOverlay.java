package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolAtlasScene.Material;
import dev.hoi.protocol.ProtocolCommanderHud.AirRange;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

final class MapAirRangeOverlay {
    record Circle(float x,float z,float radius) {}
    record Frame(List<MapSceneMesh.MapTile> fill,List<MapSceneMesh.MapTile> outline,int color) {}
    private static final Frame EMPTY=new Frame(List.of(),List.of(),0);
    private static ClientLevel level;
    private static List<Circle> circles=List.of();
    private static Frame frame=EMPTY;
    private MapAirRangeOverlay() {}
    static List<Circle> circles(List<AirRange> ranges){return ranges.stream().map(r->new Circle(r.x(),r.z(),r.radius())).distinct().toList();}
    static void clear(){level=null;circles=List.of();frame=EMPTY;}
    static Frame extract(ClientLevel world,List<AirRange> ranges){
        var next=circles(ranges);if(world==level&&next.equals(circles))return frame;
        level=world;circles=next;if(next.isEmpty())return frame=EMPTY;
        var fill=new ArrayList<MapSceneMesh.MapTile>();var outline=new ArrayList<MapSceneMesh.MapTile>();
        for(var circle:next){
            float fallback=world.getHeight(Heightmap.Types.WORLD_SURFACE,(int)Math.floor(circle.x()),(int)Math.floor(circle.z()));
            float extent=circle.radius()*512/494;var pixels=new FloatArrayList();var disk=new FloatArrayList();
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int[] corner:new int[][]{{0,0},{0,1},{1,1},{1,0}}){float u=(x+corner[0])/16f,v=(z+corner[1])/16f;vertex(pixels,world,circle.x()+(u*2-1)*extent,circle.z()+(v*2-1)*extent,u,v,fallback,.12f);}
            for(int i=0;i<128;i++){double a=i*Math.TAU/128,b=(i+1)*Math.TAU/128;vertex(disk,world,circle.x(),circle.z(),0,0,fallback,.1f);vertex(disk,world,circle.x()+(float)Math.sin(a)*circle.radius(),circle.z()+(float)Math.cos(a)*circle.radius(),0,0,fallback,.1f);vertex(disk,world,circle.x()+(float)Math.sin(b)*circle.radius(),circle.z()+(float)Math.cos(b)*circle.radius(),0,0,fallback,.1f);vertex(disk,world,circle.x(),circle.z(),0,0,fallback,.1f);}
            var bounds=new AABB(circle.x()-extent,world.getMinY(),circle.z()-extent,circle.x()+extent,world.getMaxY(),circle.z()+extent);
            fill.add(new MapSceneMesh.MapTile(Material.AIR_SELECTED,bounds,disk.toFloatArray(),List.of()));outline.add(new MapSceneMesh.MapTile(Material.AIR_SELECTED,bounds,pixels.toFloatArray(),List.of()));
        }
        return frame=new Frame(List.copyOf(fill),List.copyOf(outline),next.size()==1?0xff66cc7f:0xffffff00);
    }
    private static void vertex(FloatArrayList vertices,ClientLevel world,float x,float z,float u,float v,float fallback,float lift){
        float y=world.hasChunkAt(BlockPos.containing(x,0,z))?world.getHeight(Heightmap.Types.WORLD_SURFACE,(int)Math.floor(x),(int)Math.floor(z)):fallback;
        vertices.add(x);vertices.add(y+lift);vertices.add(z);vertices.add(u);vertices.add(v);vertices.add(0);vertices.add(1);vertices.add(0);
    }
}
