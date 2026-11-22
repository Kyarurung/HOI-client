package dev.hoi.protocol;

public final class ProtocolTooltip {
    public static final String DELAYED = "";
    private ProtocolTooltip() {}
    public static String delayed(String summary, String separator, String detail) {
        return detail == null || detail.isEmpty() ? summary : summary + DELAYED + separator + detail;
    }
}
