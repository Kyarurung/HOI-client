package dev.hoi.client.map.scene;

import dev.hoi.protocol.map.MapAtlasScene.Box;
import dev.hoi.protocol.map.MapAtlasScene.Material;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.world.phys.AABB;
import java.util.*;

record SceneMeshBaseline(String dimension, List<SceneMeshBaseline.SceneBaselineTile> tiles, Map<Long,List<Integer>> cells, List<Integer> large) {
    record SceneBaselineTile(Material material, AABB bounds, float[] vertices, List<Box> cities) {}
    private record SceneBaselineKey(int x, int z, Material material) {}
    static SceneMeshBaseline build(String dimension, List<Box> boxes) {
        var groups = new LinkedHashMap<SceneBaselineKey, List<Box>>();
        for (var box : boxes) groups.computeIfAbsent(new SceneBaselineKey((int)Math.floor(box.x()/16), (int)Math.floor(box.z()/16), box.material()), unused -> new ArrayList<>()).add(box);
        var tiles = new ArrayList<SceneBaselineTile>();
        groups.forEach((key, group) -> {
            var vertices = new FloatArrayList();
            AABB bounds = null;
            for (var box : group) {
                double radius = Math.hypot(box.sx(), box.sz());
                var extent = new AABB(box.x()-radius, box.y(), box.z()-radius, box.x()+radius, box.y()+box.sy(), box.z()+radius);
                bounds = bounds == null ? extent : bounds.minmax(extent);
                if (box.material() != Material.CITY) append(vertices, box);
            }
            tiles.add(new SceneBaselineTile(key.material(), bounds, vertices.toFloatArray(), key.material() == Material.CITY ? List.copyOf(group) : List.of()));
        });
        var cells = new HashMap<Long,List<Integer>>();var large=new ArrayList<Integer>();
        for (int i=0;i<tiles.size();i++) {
            var bounds=tiles.get(i).bounds();
            if((long)(cell(bounds.maxX)-cell(bounds.minX)+1)*(cell(bounds.maxZ)-cell(bounds.minZ)+1)>64) {
                large.add(i);continue;
            }
            for(int z=cell(bounds.minZ);z<=cell(bounds.maxZ);z++)for(int x=cell(bounds.minX);x<=cell(bounds.maxX);x++)
                cells.computeIfAbsent(cellKey(x,z),unused->new ArrayList<>()).add(i);
        }
        return new SceneMeshBaseline(dimension, List.copyOf(tiles), cells, List.copyOf(large));
    }
    private static int cell(double value) { return (int)Math.floor(value/16); }
    private static long cellKey(int x,int z) { return ((long)x<<32)|(z&0xffffffffL); }
    private static void append(FloatArrayList output, Box box) {
        if (box.material() == Material.FOREST || box.material() == Material.JUNGLE) {
            face(output, box, new float[][]{{0,1,0},{0,0,0},{1,0,1},{1,1,1}}, 0,0,1);
            face(output, box, new float[][]{{1,1,0},{1,0,0},{0,0,1},{0,1,1}}, 0,0,1);
            face(output, box, new float[][]{{1,1,1},{1,0,1},{0,0,0},{0,1,0}}, 0,0,-1);
            face(output, box, new float[][]{{0,1,1},{0,0,1},{1,0,0},{1,1,0}}, 0,0,-1);
            return;
        }
        face(output, box, new float[][]{{0,1,0},{0,1,1},{1,1,1},{1,1,0}}, 0,1,0);
        face(output, box, new float[][]{{0,0,1},{0,0,0},{1,0,0},{1,0,1}}, 0,-1,0);
        face(output, box, new float[][]{{1,1,0},{1,0,0},{0,0,0},{0,1,0}}, 0,0,-1);
        face(output, box, new float[][]{{0,1,1},{0,0,1},{1,0,1},{1,1,1}}, 0,0,1);
        face(output, box, new float[][]{{0,1,0},{0,0,0},{0,0,1},{0,1,1}}, -1,0,0);
        face(output, box, new float[][]{{1,1,1},{1,0,1},{1,0,0},{1,1,0}}, 1,0,0);
    }
    private static void face(FloatArrayList output, Box box, float[][] points, float nx, float ny, float nz) {
        float sin = (float)Math.sin(box.yaw()), cos = (float)Math.cos(box.yaw());
        for (int i = 0; i < 4; i++) {
            float x = points[i][0]*box.sx(), z = points[i][2]*box.sz();
            output.add(box.x()+x*cos+z*sin); output.add(box.y()+points[i][1]*box.sy()); output.add(box.z()-x*sin+z*cos);
            output.add(i<2?0:1); output.add(i==0||i==3?0:1);
            output.add(nx*cos+nz*sin); output.add(ny); output.add(-nx*sin+nz*cos);
        }
    }
}
