package dev.hoi.client.screen.industry;

import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.protocol.ProtocolNumberBounds;
import dev.hoi.protocol.industry.IndustryView;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IndustryLogisticsTooltips {
    private static final List<String> KEYS = List.of("EFFICIENCY", "EQUIPMENT_TYPE", "PRODUCED", "STATUS", "NEEDS", "BALANCE", "IN_STOCK", "RESOURCES");
    private static final Pattern VALUE = Pattern.compile("\\$([A-Z_]+)(?:\\|([^$]*))?\\$");
    static final int HISTORY_DAYS = 30;
    private IndustryLogisticsTooltips() {}
    static String text(String key) { return IndustryEquipmentDesigns.tooltip(key); }
    static String plain(String key) { return text(key).replaceAll("£[A-Za-z0-9_]+(?:\\|\\d+)?\\s*", "").replaceAll("§.", "").strip(); }
    static String format(String key, Map<String,Object> values) {
        Matcher matcher = VALUE.matcher(text(key));
        var out = new StringBuilder();
        while (matcher.find()) {
            Object value = values.get(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? matcher.group() : value(value, matcher.group(2) == null ? "" : matcher.group(2))));
        }
        return matcher.appendTail(out).toString();
    }
    private static String value(Object value, String flags) {
        String body;
        boolean signed = flags.contains("+"), byColor = flags.contains("=");
        double number = value instanceof Number n ? n.doubleValue() : 0;
        if (value instanceof Number) {
            boolean percent = flags.contains("%");
            double shown = Math.abs(percent ? number * 100 : number);
            var digits = flags.replaceAll("[^0-9]", "");
            int decimals = digits.isEmpty() ? (value instanceof Long || value instanceof Integer || shown == Math.rint(shown) ? 0 : 1) : Integer.parseInt(digits.substring(0, 1));
            body = flags.contains("*") && shown >= 1000 ? IndustryLogisticsRows.chartLabel(shown) : flags.contains("^") && shown >= 1000 ? IndustryLogisticsRows.stockLabel(Math.round(shown)) : String.format(Locale.ROOT, "%." + decimals + "f", shown);
            if (percent) body += "%";
            boolean zero = body.matches("[0.%]+");
            if (number < 0 && !zero) body = "-" + body;
            else if (signed && number > 0 && !zero) body = "+" + body;
        } else body = String.valueOf(value);
        String color = byColor ? number > 0 ? "§a" : number < 0 ? "§c" : "§e" : flags.contains("G") ? "§a" : flags.contains("R") ? "§c" : flags.matches(".*[YHT].*") ? "§e" : "";
        return color.isEmpty() ? body : color + body + "§f";
    }
    private static boolean low(double value) { return Math.abs(value) < 10; }
    static String header(int column) {
        String key = "LOGISTICS_" + KEYS.get(column);
        String title = "§e" + text(key + "_LABEL") + "§f";
        String description = column == 1 ? "" : text(key + (column == 2 ? "_LABEL_DESC" : "_DESC"));
        String sort = column == 2 ? "" : text(key + "_SORT_BY");
        var parts = new ArrayList<String>();parts.add(title);
        if (!description.isBlank()) parts.add(description);
        String head = String.join("\n\n", parts);
        return sort.isBlank() ? head : head + "\n\n" + sort;
    }
    static String row(IndustryLogisticsRows.IndustryLogisticsRow row, int column, List<IndustryView.Line> lines) {
        String name = row.name();
        var equipment = Map.<String,Object>of("EQUIPMENT", name);
        return switch (column) {
            case 0 -> row.efficiency() == null
                    ? format("LOGISTICS_NO_EQUIPMENT_PRODUCTION", equipment)
                    : dev.hoi.protocol.ProtocolTooltip.delayed(format("LOGISTICS_AVERAGE_EFFICIENCY", Map.of("EQUIPMENT", name, "EFFICIENCY", row.efficiency())), "\n\n", text("LOGISTICS_AVERAGE_EFFICIENCY_DELAYED"));
            case 1 -> format("LOGISTICS_EQUIPMENT_VALUE", equipment);
            case 2 -> produced(row, lines);
            case 3 -> format(low(row.need()) ? "LOGISTICS_STATUS_LOW_VALUE" : "LOGISTICS_STATUS_HIGH_VALUE", Map.of("EQUIPMENT", name, "NEED", row.need()));
            case 4 -> format(low(row.need()) ? "LOGISTICS_NEED_LOW_VALUE" : "LOGISTICS_NEED_HIGH_VALUE", Map.of("EQUIPMENT", name, "NEED", row.need())) + "\n"
                    + format((row.aircraft() ? "LOGISTICS_NEED_AIR_REINFORCEMENTS_" : "LOGISTICS_NEED_ARMY_REINFORCEMENTS_") + (low(row.need()) ? "LOW_VALUE" : "HIGH_VALUE"), Map.of("NEED", row.need()));
            case 5 -> balance(row);
            case 6 -> inStock(row);
            default -> throw new IllegalArgumentException("Unknown logistics column");
        };
    }
    private static String produced(IndustryLogisticsRows.IndustryLogisticsRow row, List<IndustryView.Line> lines) {
        String name = row.name();
        if (row.daily() <= 0) return format("LOGISTICS_NO_EQUIPMENT_PRODUCTION", Map.of("EQUIPMENT", name));
        var parts = new ArrayList<String>();
        parts.add(format(low(row.daily()) ? "LOGISTICS_ACQUIRED_TOTAL_LOW_VALUE" : "LOGISTICS_ACQUIRED_TOTAL_HIGH_VALUE", Map.of("EQUIPMENT", name, "PRODUCED", row.daily())));
        if (row.efficiency() != null && row.efficiency() < 1 - 1e-9) {
            double additional = row.efficiency() <= 0 ? 0 : row.daily() / row.efficiency() - row.daily();
            parts.add(format(low(additional) ? "LOGISTICS_EFFICIENCY_NOT_HUNDRED_LOW_VALUE" : "LOGISTICS_EFFICIENCY_NOT_HUNDRED_HIGH_VALUE", Map.of("EQUIPMENT", name, "EFFICIENCY", row.efficiency(), "ADDITIONAL", additional)));
        }
        var speeds = new ArrayList<String>();double weekly = 0;
        for (var model : row.models()) {
            double speed = lines.stream().filter(line -> line.equipment().equals(model.id())).mapToDouble(IndustryView.Line::daily).sum() * 7;
            if (speed <= 0) continue;
            weekly += speed;
            speeds.add(format("LOGISTICS_VARIANT_SPEED", Map.of("VARIANT", model.name(), "SPEED", speed)));
        }
        if (!speeds.isEmpty()) parts.add(format("LOGISTICS_PRODUCING", Map.of("PRODUCING", weekly)) + "\n" + String.join("\n", speeds));
        return String.join("\n\n", parts);
    }
    private static String balance(IndustryLogisticsRows.IndustryLogisticsRow row) {
        double balance = row.balance();
        String name = row.name();
        String head = Math.abs(balance) < 1e-9 ? "" : format((balance > 0 ? "LOGISTICS_SURPLUS_" : "LOGISTICS_DEFICIT_") + (low(balance) ? "LOW_VALUE" : "HIGH_VALUE"), Map.of("EQUIPMENT", name, "BALANCE", Math.abs(balance)));
        int days = row.ledger() == null || row.ledger().needDays() == 0 ? HISTORY_DAYS : row.ledger().needDays();
        String average = format("LOGISTICS_BALANCE", Map.of("DAYS", (long) days, "EQUIPMENT", name, "BALANCE", balance));
        return head.isEmpty() ? average : head + "\n" + average;
    }
    private static String inStock(IndustryLogisticsRows.IndustryLogisticsRow row) {
        long stock = row.inStock();
        String name = row.name();
        String head = stock == 0 ? format("LOGISTICS_INSTOCK_BALANCED", Map.of("EQUIPMENT", name))
                : format((stock > 0 ? "LOGISTICS_INSTOCK_SURPLUS_" : "LOGISTICS_INSTOCK_DEFICIT_") + (Math.abs(stock) < 10 ? "LOW_VALUE" : "HIGH_VALUE"), Map.of("EQUIPMENT", name, "INSTOCK", Math.abs(stock)));
        var parts = new ArrayList<String>();
        parts.add(strip(head));
        if (row.demand() > 0) parts.add(strip(format("LOGISTICS_STOCK_REQUEST", Map.of("COUNT", row.demand()))));
        double balance = row.balance();
        if (row.daily() <= 0) parts.add(format("LOGISTICS_ESTIMATED_NO_PRODUCTION", Map.of("EQUIPMENT", name)));
        else if (stock > 0 && balance < 0) parts.add(format("LOGISTICS_ESTIMATED_EMPTY", Map.of("EQUIPMENT", name, "DAYS", (long) Math.floor(stock / -balance))));
        else if (stock < 0 && balance > 0) parts.add(format("LOGISTICS_ESTIMATED_NO_SHORTAGE", Map.of("EQUIPMENT", name, "DAYS", (long) Math.ceil(-stock / balance))));
        return String.join("\n", parts);
    }
    private static String strip(String text) { return text.replaceAll("^\\n+|\\n+$", ""); }
    static String resource(IndustryLogisticsRows.IndustryLogisticsRow row, String resource, String resourceName, List<IndustryView.Line> lines, Map<String, IndustryView.Equipment> equipment) {
        var ids = row.models().stream().map(IndustryView.Equipment::id).toList();
        var parts = new ArrayList<String>();
        parts.add(strip(format("LOGISTICS_RESOURCE", Map.of("EQUIPMENT", row.name(), "RESOURCE", resourceName))));
        long others = lines.stream().filter(line -> !ids.contains(line.equipment()) && line.operatingFactories() > 0 && uses(line, resource, equipment.get(line.equipment()))).count();
        if (others > 0) parts.add(format("LOGISTICS_RESOURCE_OTHER_LINES", Map.of("EQUIPMENT", row.name(), "LINES", others, "RESOURCE", resourceName)));
        if (lines.stream().anyMatch(line -> ids.contains(line.equipment()) && line.missingResources().contains(resource)))
            parts.add(format("LOGISTICS_RESOURCE_SHORTAGE", Map.of("EQUIPMENT", row.name(), "RESOURCE", resourceName)));
        return String.join("\n", parts);
    }
    private static boolean uses(IndustryView.Line line, String resource, IndustryView.Equipment equipment) {
        var resources = line.conversion() != null && line.conversion().source() != null ? line.conversion().resources() : equipment == null ? Map.<String,Double>of() : equipment.resources();
        return resources.getOrDefault(resource, 0.0) > 0;
    }
    static String shortage(String resourceName, double have, double need, Map<String,Double> branches) {
        var parts = new ArrayList<String>();
        parts.add(format("LOGISTICS_RESOURCES_SUMMERY", Map.of("HAVE", rounded(have), "RESOURCE", resourceName, "NEED", rounded(need))));
        var detail = new ArrayList<String>();
        for (var branch : List.of("LAND", "NAVAL", "AIR")) {
            double value = branches.getOrDefault(branch, 0.0);
            if (value > 0) detail.add(format("LOGISTICS_RESOURCES_" + branch, Map.of("NEED", rounded(value), "RESOURCE", resourceName)));
        }
        if (!detail.isEmpty()) parts.add(String.join("\n", detail));
        parts.add(format("LOGISTICS_RESOURCE_NEED_SUMMERY", Map.of("NEED", rounded(Math.max(0, need - have)), "RESOURCE", resourceName)));
        return String.join("\n\n", parts);
    }
    private static Number rounded(double value) { return value == Math.rint(value) ? (Number) Math.round(value) : (Number) (Math.round(value * 10) / 10.0); }
    static String variant(IndustryView.Equipment equipment, List<IndustryView.Line> lines) {
        var producing = lines.stream().filter(line -> line.equipment().equals(equipment.id())).toList();
        double weekly = producing.stream().mapToDouble(IndustryView.Line::daily).sum() * 7;
        var text = new StringBuilder("§e").append(equipment.name()).append("§f");
        IndustryProductionTooltips.appendStats(text, equipment);
        text.append("\n\n").append(format("LOGISTICS_AMOUNT_PRODUCTION_LINES", Map.of("NUMBER", (long) producing.size(), "AMOUNT", weekly)));
        return text.toString();
    }
    static String deleteTitle() { return text("LOGISTICS_DELETE_EQUIPMENT_CONFIRM_TITLE"); }
    static String deleteBody(String equipment, long days) { return strip(format("LOGISTICS_DELETE_EQUIPMENT_CONFIRM_DESC", Map.of("EQUIPMENT", equipment, "NUM_DAYS", days))); }
    static String percent(double value) { return ProtocolNumberBounds.percent(value, 0); }
}
