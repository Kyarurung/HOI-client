package dev.hoi.client.map.scene;

import dev.hoi.protocol.map.MapAtlasScene.Box;
import dev.hoi.protocol.map.MapAtlasScene.Material;
import dev.hoi.protocol.map.MapAtlasVisibility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public record SceneMesh(String dimension, List<SceneMesh.SceneTile> tiles, Map<Long,List<Integer>> cells, List<Integer> large) {
    public record SceneTile(Material material, AABB bounds, float[] vertices, List<Box> cities,boolean movement,dev.hoi.protocol.commander.CommanderHudPayloads.Arrow arrow) {public SceneTile(Material material,AABB bounds,float[] vertices,List<Box> cities){this(material,bounds,vertices,cities,false,dev.hoi.protocol.commander.CommanderHudPayloads.Arrow.MOVE);}}
    private record MovementCellKey(long cell,dev.hoi.protocol.commander.CommanderHudPayloads.Arrow arrow) {}
    private record SceneCellKey(int x, int z, Material material) {}
    public static SceneMesh build(String dimension, List<Box> boxes) {
        var groups = new LinkedHashMap<SceneCellKey, List<Box>>();
        for (var box : boxes) groups.computeIfAbsent(new SceneCellKey((int)Math.floor(box.x()/16), (int)Math.floor(box.z()/16), box.material()), unused -> new ArrayList<>()).add(box);
        var tiles = new ArrayList<SceneTile>();
        groups.forEach((key, group) -> {
            var vertices = new FloatArrayList();
            AABB bounds = null;
            for (var box : group) {
                double radius = Math.hypot(box.sx(), box.sz());
                var extent = new AABB(box.x()-radius, box.y(), box.z()-radius, box.x()+radius, box.y()+box.sy(), box.z()+radius);
                bounds = bounds == null ? extent : bounds.minmax(extent);
                if (box.material() != Material.CITY) append(vertices, box);
            }
            tiles.add(new SceneTile(key.material(), bounds, vertices.toFloatArray(), key.material() == Material.CITY ? List.copyOf(group) : List.of()));
        });
        return assemble(dimension,tiles);
    }
    public static SceneMesh build(String dimension,List<Box> boxes,List<dev.hoi.protocol.commander.CommanderHudPayloads.MovementQuad> movement){
        var tiles=new ArrayList<>(build(dimension,boxes).tiles());var groups=new LinkedHashMap<MovementCellKey,List<dev.hoi.protocol.commander.CommanderHudPayloads.MovementQuad>>();for(var quad:movement)groups.computeIfAbsent(new MovementCellKey(cellKey(cell(quad.x()),cell(quad.z())),quad.arrow()),unused->new ArrayList<>()).add(quad);
        for(var group:groups.values()){
            var vertices=new FloatArrayList();AABB bounds=null;
            for(var q:group){float sin=(float)Math.sin(q.yaw()),cos=(float)Math.cos(q.yaw());double radius=Math.hypot(q.width(),q.length());var extent=new AABB(q.x()-radius,q.y(),q.z()-radius,q.x()+radius,q.y()+.01,q.z()+radius);bounds=bounds==null?extent:bounds.minmax(extent);float[][] points={{0,0},{0,1},{1,1},{1,0}};for(var point:points){float x=point[0]*q.width(),z=point[1]*q.length();vertices.add(q.x()+x*cos+z*sin);vertices.add(q.y());vertices.add(q.z()-x*sin+z*cos);vertices.add(q.u0()+point[1]*(q.u1()-q.u0()));vertices.add(q.v0()+point[0]*(q.v1()-q.v0()));vertices.add(0);vertices.add(1);vertices.add(0);}}
            tiles.add(new SceneTile(Material.DEPLOYMENT,bounds,vertices.toFloatArray(),List.of(),true,group.getFirst().arrow()));
        }
        return assemble(dimension,tiles);
    }
    private static SceneMesh assemble(String dimension,List<SceneTile> tiles){
        var cells = new HashMap<Long,List<Integer>>();var large=new ArrayList<Integer>();
        for (int i=0;i<tiles.size();i++) {
            var bounds=tiles.get(i).bounds();
            if((long)(cell(bounds.maxX)-cell(bounds.minX)+1)*(cell(bounds.maxZ)-cell(bounds.minZ)+1)>64) {
                large.add(i);continue;
            }
            for(int z=cell(bounds.minZ);z<=cell(bounds.maxZ);z++)for(int x=cell(bounds.minX);x<=cell(bounds.maxX);x++)
                cells.computeIfAbsent(cellKey(x,z),unused->new ArrayList<>()).add(i);
        }
        return new SceneMesh(dimension, List.copyOf(tiles), cells, List.copyOf(large));
    }
    private static int cell(double value) { return (int)Math.floor(value/16); }
    private static long cellKey(int x,int z) { return ((long)x<<32)|(z&0xffffffffL); }
    int collect(Vec3 camera,List<SceneTile> result) {
        double radius=Math.max(MapAtlasVisibility.BOUNDARY_RANGE,MapAtlasVisibility.DETAIL_RANGE);
        var seen=new LinkedHashSet<Integer>(large);
        for(int z=cell(camera.z-radius);z<=cell(camera.z+radius);z++)for(int x=cell(camera.x-radius);x<=cell(camera.x+radius);x++) {
            var entries=cells.get(cellKey(x,z));if(entries!=null)seen.addAll(entries);
        }
        for(int id:seen) {
            var tile=tiles.get(id);double range=MapAtlasVisibility.range(tile.material());
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
