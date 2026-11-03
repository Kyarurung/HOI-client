package dev.hoi.protocol;

public enum ProtocolIntelDomain {
    CIVILIAN("민간", "intel/civilian"), ARMY("육군", "intel/army"), NAVY("해군", "intel/navy"), AIR("공군", "intel/air");
    public final String label, icon;
    ProtocolIntelDomain(String label, String icon) { this.label = label; this.icon = icon; }
    public static boolean permits(double percent, boolean shared, int threshold) {
        return Double.isFinite(percent) && percent >= 0 && percent <= 100 && (shared || percent >= threshold);
    }
}
