package dev.hoi.client.screen;

import dev.hoi.protocol.IndustryView;
import dev.hoi.protocol.IndustryView.Equipment;
import java.util.*;


final class LogisticsRows {
    record Row(Equipment representative, List<Equipment> models, double daily, Double efficiency, Map<String, Double> resources) {
        long stockpile() { return models.stream().mapToLong(Equipment::stockpile).sum(); }
        long reserved() { return models.stream().mapToLong(Equipment::reserved).sum(); }
        long deployed() { return models.stream().mapToLong(Equipment::deployed).sum(); }
        long deficit() { return models.stream().mapToLong(Equipment::deficit).sum(); }
        Long demand() { return models.stream().anyMatch(e -> e.demand() == null) ? null : models.stream().mapToLong(Equipment::demand).sum(); }
        Long balance() { Long demand = demand(); return demand == null ? null : stockpile() - demand; }
        String status() { Long balance = balance(); return balance == null ? "—" : balance >= 0 ? "충족" : daily > 0 ? "생산" : "부족"; }
        String demandLabel() { Long demand = demand(); return demand == null ? "—" : stockLabel(demand); }
        String balanceLabel() { Long balance = balance(); return balance == null ? "—" : (balance < 0 ? "−" : balance > 0 ? "+" : "") + stockLabel(Math.abs(balance)); }
        String supplyDetail() {
            Long demand = demand();
            if (demand == null) return "서버가 충원 수요를 제공하지 않았습니다.";
            long balance = stockpile() - demand;
            String estimate = balance >= 0 ? "현재 비축량으로 충원 가능" : daily > 0
                    ? "부족분 생산 예상: " + productionLabel(Math.ceil(-balance / daily)) + "일" : "생산 라인이나 가용 자원이 필요합니다.";
            return "충원 수요: " + demand + "\n비축량: " + stockpile() + "\n균형: " + balance
                    + "\n일일 생산: " + productionLabel(daily) + "\n" + estimate
                    + "\n신규 손실·수요 변화 없이 현재 생산량이 유지될 때의 예상입니다."
                    + "\n인력·보급로·우선순위에 따라 실제 지급 시점이 달라집니다.";
        }
    }
    static String productionLabel(double value) {
        String[] units = {"", "K", "M", "B", "T", "P", "E"};
        int unit = 0;
        while (value >= 1000 && unit < units.length - 1) { value /= 1000; unit++; }
        value = Math.round(value * 10) / 10.0;
        if (value >= 1000 && unit < units.length - 1) { value /= 1000; unit++; }
        String number = String.format(Locale.ROOT, "%.1f", value);
        return (number.endsWith(".0") ? number.substring(0, number.length() - 2) : number) + units[unit];
    }
    static String stockLabel(long value) {
        if (value < 1000) return Long.toString(value);
        String[] units = {"K", "M", "B", "T", "P", "E"};
        double scaled = value / 1000.0;
        int unit = 0;
        while (scaled >= 999.5 && unit < units.length - 1) { scaled /= 1000; unit++; }
        String number = String.format(Locale.ROOT, scaled < 9.95 ? "%.1f" : "%.0f", scaled);
        return (number.endsWith(".0") ? number.substring(0, number.length() - 2) : number) + units[unit];
    }
    static List<Row> create(List<Equipment> equipment, List<IndustryView.Line> lines, String group) {
        var linesByEquipment = new HashMap<String, List<IndustryView.Line>>();
        for (var line : lines) linesByEquipment.computeIfAbsent(line.equipment(), key -> new ArrayList<>()).add(line);
        var families = new LinkedHashMap<String,List<Equipment>>();
        for (var item : equipment) {
            if ((!item.unlocked() && item.stockpile() == 0 && item.reserved() == 0 && item.deployed() == 0 && (item.demand() == null || item.demand() == 0) && item.deficit() == 0 && !linesByEquipment.containsKey(item.id())) || !group.equals("all") && !item.group().equals(group)) continue;
            String family = item.family() == null || item.family().isBlank() ? item.id() : item.family();
            families.computeIfAbsent(item.group() + "/" + family, key -> new ArrayList<>()).add(item);
        }
        var rows = new ArrayList<Row>();
        for (var models : families.values()) {

            var representative = models.stream().filter(Equipment::unlocked)
                    .min(Comparator.comparing(Equipment::outdated).thenComparing(Equipment::id)).orElse(models.getFirst());
            double daily = 0, weightedEfficiency = 0;
            int factories = 0;
            var resources = new TreeMap<String, Double>();
            for (var model : models) for (var line : linesByEquipment.getOrDefault(model.id(), List.of())) {
                daily += line.daily();
                if (line.factories() > 0) {
                    factories += line.factories();
                    weightedEfficiency += line.efficiency() * line.factories();
                    model.resources().forEach((resource, amount) -> resources.merge(resource, amount * line.factories(), Double::sum));
                }
            }
            Double efficiency = factories == 0 ? null : weightedEfficiency / factories;
            rows.add(new Row(representative, List.copyOf(models), daily, efficiency, Collections.unmodifiableMap(resources)));
        }
        return List.copyOf(rows);
    }
    private LogisticsRows() {}
}
