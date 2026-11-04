package dev.hoi.protocol.map;

public final class MapAtlasVisibility {
    public static final double BOUNDARY_RANGE = 64;
    public static final double DETAIL_RANGE = 64;
    public static double range(MapAtlasScene.Material material) {
        return material == MapAtlasScene.Material.BLACK || material == MapAtlasScene.Material.RED
                ? BOUNDARY_RANGE : DETAIL_RANGE;
    }
    private MapAtlasVisibility() {}
}
