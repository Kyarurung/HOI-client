package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolAtlasScene.Box;
import dev.hoi.protocol.ProtocolAtlasScene.Material;
import dev.hoi.protocol.ProtocolAtlasVisibility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

record MapSceneMesh(String dimension, List<MapSceneMesh.MapTile> tiles, Map<Long,List<Integer>> cells, List<Integer> large) {
    record MapTile(Material material, AABB bounds, float[] vertices, List<Box> cities,boolean movement) {MapTile(Material material,AABB bounds,float[] vertices,List<Box> cities){this(material,bounds,vertices,cities,false);}}
    private record MapCellKey(int x, int z, Material material) {}
    static MapSceneMesh build(String dimension, List<Box> boxes) {
        var groups = new LinkedHashMap<MapCellKey, List<Box>>();
        for (var box : boxes) groups.computeIfAbsent(new MapCellKey((int)Math.floor(box.x()/16), (int)Math.floor(box.z()/16), box.material()), unused -> new ArrayList<>()).add(box);
        var tiles = new ArrayList<MapTile>();
        groups.forEach((key, group) -> {
            var vertices = new FloatArrayList();
            AABB bounds = null;
            for (var box : group) {
                double radius = Math.hypot(box.sx(), box.sz());
                var extent = new AABB(box.x()-radius, box.y(), box.z()-radius, box.x()+radius, box.y()+box.sy(), box.z()+radius);
                bounds = bounds == null ? extent : bounds.minmax(extent);
                if (box.material() != Material.CITY) append(vertices, box);
            }
            tiles.add(new MapTile(key.material(), bounds, vertices.toFloatArray(), key.material() == Material.CITY ? List.copyOf(group) : List.of()));
        });
        return assemble(dimension,tiles);
    }
    static MapSceneMesh build(String dimension,List<Box> boxes,List<dev.hoi.protocol.ProtocolCommanderHud.MovementQuad> movement){
        var tiles=new ArrayList<>(build(dimension,boxes).tiles());var groups=new LinkedHashMap<Long,List<dev.hoi.protocol.ProtocolCommanderHud.MovementQuad>>();for(var quad:movement)groups.computeIfAbsent(cellKey(cell(quad.x()),cell(quad.z())),unused->new ArrayList<>()).add(quad);
        for(var group:groups.values()){
            var vertices=new FloatArrayList();AABB bounds=null;
            for(var q:group){float sin=(float)Math.sin(q.yaw()),cos=(float)Math.cos(q.yaw());double radius=Math.hypot(q.width(),q.length());var extent=new AABB(q.x()-radius,q.y(),q.z()-radius,q.x()+radius,q.y()+.01,q.z()+radius);bounds=bounds==null?extent:bounds.minmax(extent);float[][] points={{0,0},{0,1},{1,1},{1,0}};for(var point:points){float x=point[0]*q.width(),z=point[1]*q.length();vertices.add(q.x()+x*cos+z*sin);vertices.add(q.y());vertices.add(q.z()-x*sin+z*cos);vertices.add(q.u0()+point[1]*(q.u1()-q.u0()));vertices.add(q.v0()+point[0]*(q.v1()-q.v0()));vertices.add(0);vertices.add(1);vertices.add(0);}}
            tiles.add(new MapTile(Material.DEPLOYMENT,bounds,vertices.toFloatArray(),List.of(),true));
        }
        return assemble(dimension,tiles);
    }
    private static MapSceneMesh assemble(String dimension,List<MapTile> tiles){
        var cells = new HashMap<Long,List<Integer>>();var large=new ArrayList<Integer>();
        for (int i=0;i<tiles.size();i++) {
            var bounds=tiles.get(i).bounds();
            if((long)(cell(bounds.maxX)-cell(bounds.minX)+1)*(cell(bounds.maxZ)-cell(bounds.minZ)+1)>64) {
                large.add(i);continue;
            }
            for(int z=cell(bounds.minZ);z<=cell(bounds.maxZ);z++)for(int x=cell(bounds.minX);x<=cell(bounds.maxX);x++)
                cells.computeIfAbsent(cellKey(x,z),unused->new ArrayList<>()).add(i);
        }
        return new MapSceneMesh(dimension, List.copyOf(tiles), cells, List.copyOf(large));
    }
    private static int cell(double value) { return (int)Math.floor(value/16); }
    private static long cellKey(int x,int z) { return ((long)x<<32)|(z&0xffffffffL); }
    int collect(Vec3 camera,List<MapTile> result) {
        double radius=Math.max(ProtocolAtlasVisibility.BOUNDARY_RANGE,ProtocolAtlasVisibility.DETAIL_RANGE);
        var seen=new LinkedHashSet<Integer>(large);
        for(int z=cell(camera.z-radius);z<=cell(camera.z+radius);z++)for(int x=cell(camera.x-radius);x<=cell(camera.x+radius);x++) {
            var entries=cells.get(cellKey(x,z));if(entries!=null)seen.addAll(entries);
        }
        for(int id:seen) {
            var tile=tiles.get(id);double range=ProtocolAtlasVisibility.range(tile.material());
            if(tile.bounds().distanceToSqr(camera)<=range*range)result.add(tile);
        }
        return seen.size();
    }
    private static final float[][][] FACES = {
            {{0,1,0},{0,0,0},{1,0,1},{1,1,1}},
            {{1,1,0},{1,0,0},{0,0,1},{0,1,1}},
            {{1,1,1},{1,0,1},{0,0,0},{0,1,0}},
            {{0,1,1},{0,0,1},{1,0,0},{1,1,0}},
            {{0,1,0},{0,1,1},{1,1,1},{1,1,0}},
            {{0,0,1},{0,0,0},{1,0,0},{1,0,1}},
            {{1,1,0},{1,0,0},{0,0,0},{0,1,0}},
            {{0,1,1},{0,0,1},{1,0,1},{1,1,1}},
            {{0,1,0},{0,0,0},{0,0,1},{0,1,1}},
            {{1,1,1},{1,0,1},{1,0,0},{1,1,0}}
    };
    private static void append(FloatArrayList output, Box box) {
        float sin = (float)Math.sin(box.yaw()), cos = (float)Math.cos(box.yaw());
        if (box.material() == Material.FOREST || box.material() == Material.JUNGLE) {
            face(output, box, FACES[0], 0,0,1, sin, cos);
            face(output, box, FACES[1], 0,0,1, sin, cos);
            face(output, box, FACES[2], 0,0,-1, sin, cos);
            face(output, box, FACES[3], 0,0,-1, sin, cos);
            return;
        }
        face(output, box, FACES[4], 0,1,0, sin, cos);
        face(output, box, FACES[5], 0,-1,0, sin, cos);
        face(output, box, FACES[6], 0,0,-1, sin, cos);
        face(output, box, FACES[7], 0,0,1, sin, cos);
        face(output, box, FACES[8], -1,0,0, sin, cos);
        face(output, box, FACES[9], 1,0,0, sin, cos);
    }
    private static void face(FloatArrayList output, Box box, float[][] points, float nx, float ny, float nz, float sin, float cos) {
        for (int i = 0; i < 4; i++) {
            float x = points[i][0]*box.sx(), z = points[i][2]*box.sz();
            output.add(box.x()+x*cos+z*sin); output.add(box.y()+points[i][1]*box.sy()); output.add(box.z()-x*sin+z*cos);
            output.add(i<2?0:1); output.add(i==0||i==3?0:1);
            output.add(nx*cos+nz*sin); output.add(ny); output.add(-nx*sin+nz*cos);
        }
    }
}
