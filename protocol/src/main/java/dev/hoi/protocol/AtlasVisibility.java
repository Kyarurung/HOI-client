package dev.hoi.protocol;


public final class AtlasVisibility {
    public static final double BOUNDARY_RANGE = 64;
    public static final double DETAIL_RANGE = 64;
    public static double range(AtlasSceneProtocol.Material material) {
        return material == AtlasSceneProtocol.Material.BLACK || material == AtlasSceneProtocol.Material.RED
                ? BOUNDARY_RANGE : DETAIL_RANGE;
    }
    private AtlasVisibility() {}
}
