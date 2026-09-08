package dev.hoi.protocol;

/** Common camera-distance limits for the static client mesh and Polymer fallback. */
public final class AtlasVisibility {
    public static final double BOUNDARY_RANGE = 128;
    public static final double DETAIL_RANGE = 128;
    public static double range(AtlasSceneProtocol.Material material) {
        return material == AtlasSceneProtocol.Material.BLACK || material == AtlasSceneProtocol.Material.RED
                ? BOUNDARY_RANGE : DETAIL_RANGE;
    }
    private AtlasVisibility() {}
}
