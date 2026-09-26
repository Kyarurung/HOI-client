package dev.hoi.protocol;

import java.util.List;


public enum ProtocolMenuTab {
    POLITICS("politics", "국가 정보"),
    DECISIONS("decisions", "결정"),
    INTELLIGENCE("intelligence", "정보기관"),
    RESEARCH("research", "연구"),
    TRADE("trade", "무역 & 경제"),
    CONSTRUCTION("construction", "건설"),
    PRODUCTION("production", "생산"),
    RECRUITMENT("recruitment", "모병 및 배치"),
    LOGISTICS("logistics", "군수"),
    OFFICER_CORPS("officer_corps", "장교단");

    public static final List<ProtocolMenuTab> ORDER = List.of(values());
    private final String id;
    private final String label;

    ProtocolMenuTab(String id, String label) { this.id = id; this.label = label; }
    public String id() { return id; }
    public String label() { return label; }
}
