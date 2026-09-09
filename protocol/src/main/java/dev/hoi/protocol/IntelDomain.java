package dev.hoi.protocol;

import java.util.List;


public enum IntelDomain {
    CIVILIAN("민간", "intel/civilian", List.of(
            new Tier(10, "예상 건물·가용 인력·수송선 수, 무역 상대"),
            new Tier(30, "예상 연료, 손상 건물 이력"),
            new Tier(50, "건물 수·자원 흐름, 완료한 중점, 민간 기술 수"),
            new Tier(70, "연료·가용 인력·수송선 수, 진행 중인 중점, 연구한 민간 기술"),
            new Tier(80, "비축 기록, 진행 중인 민간 연구"), new Tier(90, "수송선 경로 상세"))),
    ARMY("육군", "intel/army", List.of(
            new Tier(5, "예상 사단 수"), new Tier(10, "예상 배치 인력, 발사 시 습격 식별"),
            new Tier(30, "편제 목록, 장비 유형·예상 비축량, 교리 연구 진척도"),
            new Tier(50, "보급 경로, 편제별 예상 사단 수, 준비 중간 단계 습격 식별"),
            new Tier(70, "사단·인력·장비 수, 보급 상세, 연구한 육군 기술"),
            new Tier(80, "편제·장비 설계 상세, 진행 중인 육군 연구, 초기 습격 식별"),
            new Tier(90, "위장 부대 식별"))),
    NAVY("해군", "intel/navy", List.of(
            new Tier(5, "예상 배치 인력"), new Tier(10, "예상 함대·함선 수, 항구·해군기지 수준"),
            new Tier(30, "함대 수, 예상 기동부대·함급별 함선 수, 항구 위치, 교리 연구 진척도"),
            new Tier(40, "해상 지배 불이익 없음"), new Tier(50, "함선 파생형, 해군 활동 지역"),
            new Tier(70, "기동부대·함선·인력 수, 연구한 해군 기술"),
            new Tier(80, "함선 설계·기동부대 상세, 진행 중인 해군 연구"))),
    AIR("공군", "intel/air", List.of(
            new Tier(10, "예상 배치 인력, 발사 시 습격 식별"),
            new Tier(30, "항공기 유형·공군기지 위치, 예상 임무·기지별 항공기 수, 교리 연구 진척도"),
            new Tier(50, "기종별 예상 항공기 수, 준비 중간 단계 습격 식별"),
            new Tier(60, "활성 임무 수"), new Tier(70, "항공기·기종·인력·기지별 수, 연구한 공군 기술"),
            new Tier(80, "항공기 설계·기지 상세, 진행 중인 공군 연구, 초기 습격 식별")));

    public final String label, icon;
    public final List<Tier> tiers;
    IntelDomain(String label, String icon, List<Tier> tiers) { this.label = label; this.icon = icon; this.tiers = tiers; }
    public record Tier(int percent, String description) {}
    public static boolean permits(double percent, boolean shared, int threshold) {
        return Double.isFinite(percent) && percent >= 0 && percent <= 100 && (shared || percent >= threshold);
    }
}
