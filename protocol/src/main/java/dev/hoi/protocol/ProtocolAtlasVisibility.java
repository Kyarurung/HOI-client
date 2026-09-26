package dev.hoi.protocol;


public final class ProtocolAtlasVisibility {
    public static final double BOUNDARY_RANGE = 64;
    public static final double DETAIL_RANGE = 64;
    public static double range(ProtocolAtlasScene.Material material) {
        return material == ProtocolAtlasScene.Material.BLACK || material == ProtocolAtlasScene.Material.RED
                ? BOUNDARY_RANGE : DETAIL_RANGE;
    }
    private ProtocolAtlasVisibility() {}
}
