package dev.hoi.client.screen.industry;

import dev.hoi.protocol.industry.IndustryView;
import dev.hoi.protocol.industry.IndustryView.Equipment;
import java.util.*;

final class IndustryLogisticsRows {
    private static final Set<String> COMBAT_SHIP_FAMILIES = Set.of("destroyer", "light_cruiser", "heavy_cruiser", "battlecruiser", "battleship", "carrier", "submarine");
    static final double BAR = 62, SURPLUS_SHIFT = 5;
    record IndustryLogisticsRow(Equipment representative, List<Equipment> models, double daily, Double efficiency, Map<String, Double> resources, IndustryView.EquipmentLedger ledger) {
        long stockpile() { return models.stream().mapToLong(Equipment::stockpile).sum(); }
        long reserved() { return models.stream().mapToLong(Equipment::reserved).sum(); }
        long deployed() { return models.stream().mapToLong(Equipment::deployed).sum(); }
        long demand() { return models.stream().mapToLong(e -> e.demand() == null ? 0 : e.demand()).sum(); }
        double need() { return ledger == null ? 0 : ledger.need(); }
        double balance() { return daily - need(); }
        long inStock() { return stockpile() - demand(); }
        double coverage() { double need = need(); return need <= 0 ? daily > 0 ? 1 : 0 : Math.min(1, daily / need); }
        double marker() {
            double need = need(), total = need + daily;
            if (total <= 0) return 0;
            double ratio = (need - daily) / total;
            return ratio >= 0 ? BAR * ratio : SURPLUS_SHIFT * ratio;
        }
        boolean aircraft() { return representative.group().equals("air"); }
        String name() { return representative.familyName() == null || representative.familyName().isBlank() ? representative.name() : representative.familyName(); }
    }
    static String amount(double value) {
        if (Math.abs(value) >= 1000) return stockLabel(Math.round(value));
        return Math.abs(value) < 10 ? fixed(value) : Long.toString(Math.round(value));
    }
    static String fixed(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.equals("-0.0") ? "0.0" : text;
    }
    static String signedAmount(double value) {
        String body = Math.abs(value) >= 1000 ? stockLabel(Math.round(Math.abs(value))) : fixed(Math.abs(value));
        return body.equals("0.0") ? body : (value > 0 ? "+" : "-") + body;
    }
    static String signedStock(long value) {
        return value == 0 ? "0" : (value > 0 ? "+" : "-") + stockLabel(Math.abs(value));
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
    static String chartLabel(double value) {
        String[] units = {"K", "M", "B", "T"};
        double scaled = value / 1000;
        int unit = 0;
        while (scaled >= 999.95 && unit < units.length - 1) { scaled /= 1000; unit++; }
        return String.format(Locale.ROOT, "%.1f", scaled) + units[unit];
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
    static List<IndustryLogisticsRow> create(List<Equipment> equipment, List<IndustryView.Line> lines, String group) {
        return create(equipment, lines, group, List.of());
    }
    static List<IndustryLogisticsRow> create(List<Equipment> equipment, List<IndustryView.Line> lines, String group, List<IndustryView.EquipmentLedger> ledgers) {
        var linesByEquipment = new HashMap<String, List<IndustryView.Line>>();
        for (var line : lines) linesByEquipment.computeIfAbsent(line.equipment(), key -> new ArrayList<>()).add(line);
        var ledgerById = new HashMap<String, IndustryView.EquipmentLedger>();
        for (var ledger : ledgers) ledgerById.put(ledger.equipment(), ledger);
        var families = new LinkedHashMap<String,List<Equipment>>();
        for (var item : equipment) {
            if (item.naval() && COMBAT_SHIP_FAMILIES.contains(Objects.toString(item.family(), ""))) continue;
            if (!group.equals("all") && !item.group().equals(group)) continue;
            String family = item.family() == null || item.family().isBlank() ? item.id() : item.family();
            families.computeIfAbsent(item.group() + "/" + family, key -> new ArrayList<>()).add(item);
        }
        var rows = new ArrayList<IndustryLogisticsRow>();
        for (var models : families.values()) {
            if(models.stream().noneMatch(Equipment::unlocked)||models.stream().noneMatch(Equipment::everAcquired))continue;
            var representative = models.stream().filter(Equipment::unlocked)
                    .min(Comparator.comparing(Equipment::outdated).thenComparing(Equipment::id)).orElse(models.getFirst());
            double daily = 0, weightedEfficiency = 0;
            int factories = 0;
            var resources = new TreeMap<String, Double>();
            for (var model : models) for (var line : linesByEquipment.getOrDefault(model.id(), List.of())) {
                daily += line.daily();
                if (line.operatingFactories() > 0) {
                    factories += line.operatingFactories();
                    weightedEfficiency += line.efficiency() * line.operatingFactories();
                    (line.conversion() != null && line.conversion().source() != null ? line.conversion().resources() : model.resources()).forEach((resource, amount) -> resources.merge(resource, amount * line.operatingFactories(), Double::sum));
                }
            }
            Double efficiency = factories == 0 ? null : weightedEfficiency / factories;
            var ledger = models.stream().map(model -> ledgerById.get(model.id())).filter(Objects::nonNull).findFirst().orElse(null);
            rows.add(new IndustryLogisticsRow(representative, List.copyOf(models), daily, efficiency, Collections.unmodifiableMap(resources), ledger));
        }
        return List.copyOf(rows);
    }
    private IndustryLogisticsRows() {}
}
