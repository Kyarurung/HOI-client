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
    }
    static List<Row> create(List<Equipment> equipment, List<IndustryView.Line> lines, String group) {
        var linesByEquipment = new HashMap<String, List<IndustryView.Line>>();
        for (var line : lines) linesByEquipment.computeIfAbsent(line.equipment(), key -> new ArrayList<>()).add(line);
        var families = new LinkedHashMap<String,List<Equipment>>();
        for (var item : equipment) {
            if ((!item.unlocked() && item.stockpile() == 0) || !group.equals("all") && !item.group().equals(group)) continue;
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
