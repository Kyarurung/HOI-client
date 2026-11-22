package dev.hoi.client.screen.agency;

import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiText;
import dev.hoi.protocol.country.CountryView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

record AgencyIntelChart(List<String> days, List<Series> series, double maximum) {
    record Series(List<Double> values, int color, String label) {}
    static AgencyIntelChart read(CountryView.Section section, int history) {
        if (!section.state().equals("shown")) return new AgencyIntelChart(List.of(), List.of(), 0);
        var dates = section.rows().stream().filter(row -> row.header() && row.group().equals("days")).findFirst();
        if (dates.isEmpty() || dates.get().detail().isBlank()) return new AgencyIntelChart(List.of(), List.of(), 0);
        try {
            var allDays = List.of(dates.get().detail().strip().split("\\s+"));
            if (allDays.size() > 79) return new AgencyIntelChart(List.of(), List.of(), 0);
            for (var day : allDays) LocalDate.parse(day);
            int count = new int[]{7, 27, 79}[Math.clamp(history, 0, 2)];
            int start = Math.max(0, allDays.size() - count);
            var lines = new ArrayList<Series>();
            double maximum = 0;
            for (var row : section.rows()) {
                if (row.header()) continue;
                if (!List.of("industry", "industry_lost", "convoys_produced", "convoys_lost", "bombers_produced", "bombers_lost", "trucks", "trains").contains(row.group())) continue;
                var tokens = row.detail().strip().split("\\s+");
                if (tokens.length != allDays.size()) return new AgencyIntelChart(List.of(), List.of(), 0);
                var values = new ArrayList<Double>();
                for (int i = start; i < tokens.length; i++) {
                    double value = Double.parseDouble(tokens[i]);
                    if (!Double.isFinite(value) || value < 0) return new AgencyIntelChart(List.of(), List.of(), 0);
                    values.add(value);
                    maximum = Math.max(maximum, value);
                }
                lines.add(new Series(List.copyOf(values), row.group().endsWith("_lost") ? 0x99FF0000 : 0x9900FF00, row.name()));
            }
            return new AgencyIntelChart(List.copyOf(allDays.subList(start, allDays.size())), List.copyOf(lines), maximum);
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException error) {
            return new AgencyIntelChart(List.of(), List.of(), 0);
        }
    }
    String tooltip(double position) {
        if (days.isEmpty() || series.isEmpty() || position < 0 || position > 1) return "";
        int index = (int)Math.round(position * (days.size() - 1));
        var day = LocalDate.parse(days.get(index));
        var lines = new ArrayList<String>(List.of("§H" + day.getYear() + "년 " + day.getMonthValue() + "월 " + day.getDayOfMonth() + "일§!", ""));
        for (var line : series) if (!line.label().isBlank()) lines.add(line.label().replace("$NUM|H$", "§H" + number(line.values().get(index)) + "§!")
                .replace("$UNTIL_TODAY|H$", "§H" + number(line.values().stream().skip(index).mapToDouble(Double::doubleValue).sum()) + "§!"));
        return lines.size() == 2 ? "" : String.join("\n", lines);
    }
    private static String number(double value) { return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
    void draw(GuiGraphicsExtractor g, Font font, int x, int y, double scale) {
        if (days.isEmpty() || series.isEmpty()) return;
        double divisor = Math.max(1, maximum);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale((float)scale);
        for (var line : series) for (int i = 0; i < line.values().size(); i++) {
            double x2 = line.values().size() == 1 ? 0 : i * 380.0 / (line.values().size() - 1);
            double y2 = 120 * (1 - line.values().get(i) / divisor);
            double x1 = i == 0 ? x2 : (i - 1) * 380.0 / (line.values().size() - 1);
            double y1 = i == 0 ? y2 : 120 * (1 - line.values().get(i - 1) / divisor);
            int steps = Math.max(1, (int)Math.ceil(Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1))));
            for (int step = 0; step <= steps; step++) {
                g.pose().pushMatrix();
                g.pose().translate((float)(x1 + (x2 - x1) * step / steps - .75), (float)(y1 + (y2 - y1) * step / steps - .75));
                g.pose().scale(1.5f);
                g.fill(0, 0, 1, 1, line.color());
                g.pose().popMatrix();
            }
        }
        g.pose().popMatrix();
        String max = number(maximum);
        UiText.scrollingCell(g, font, net.minecraft.network.chat.Component.literal(max), x + (int)Math.round(385 * scale), y - (int)Math.round(20 * scale), (int)Math.round(42 * scale), (int)Math.round(30 * scale), UiMenuStyle.TEXT, false);
        UiText.text(g, font, "0", x + (int)Math.round(385 * scale), y + (int)Math.round(105 * scale), UiMenuStyle.TEXT);
        UiText.scrollingCell(g, font, net.minecraft.network.chat.Component.literal(days.getFirst()), x, y + (int)Math.round(130 * scale), (int)Math.round(82 * scale), (int)Math.round(20 * scale), UiMenuStyle.TEXT, false);
        UiText.scrollingCell(g, font, net.minecraft.network.chat.Component.literal(UiCommandLabels.text("TODAY", "오늘")), x + (int)Math.round(330 * scale), y + (int)Math.round(130 * scale), (int)Math.round(80 * scale), (int)Math.round(20 * scale), UiMenuStyle.TEXT, true);
    }
}
