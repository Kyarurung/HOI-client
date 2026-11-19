package dev.hoi.client.screen.industry;

import dev.hoi.protocol.industry.IndustryView;
import dev.hoi.protocol.industry.IndustryView.*;
import java.util.*;

final class IndustryTradeRows {
    private IndustryTradeRows() {}
    record IndustryTradeRow(Partner partner, ResourceTrade detail, double total, double remaining, double delivered, double requested, int factories) {}
    static IndustryTradeRow row(IndustryView view, Partner p, String resource) {
        var active = view.trades().stream().filter(t -> t.importing() && t.partner().equals(p.id()) && t.resource().equals(resource)).toList();
        var detail = p.detail() == null ? null : p.detail().resources().get(resource);
        double total = p.exports().getOrDefault(resource, 0.0), delivered = active.stream().mapToDouble(Trade::delivered).sum();
        int factories = active.stream().mapToInt(Trade::factories).sum();
        double requested = detail != null ? detail.requested() : factories * (p.resourcesPerFactory() == null ? 8 : p.resourcesPerFactory());
        double remaining = detail == null ? Math.max(0, total - delivered) : detail.remaining();
        return new IndustryTradeRow(p, detail, total, remaining, delivered, requested, factories);
    }
    static boolean region(Partner p, String region) {
        return switch (region) { case "faction" -> p.faction(); case "subject" -> p.subject(); default -> region.equals(p.continent()); };
    }
    static List<IndustryTradeRow> rows(IndustryView view, String resource, Set<String> regions, String sort, boolean reverse) {
        Comparator<IndustryTradeRow> order = switch (sort) {
            case "country" -> Comparator.comparing((IndustryTradeRow r) -> r.partner().name());
            case "request" -> Comparator.comparingDouble(IndustryTradeRow::delivered).reversed();
            case "convoys" -> Comparator.comparingInt((IndustryTradeRow r) -> r.partner().convoys() == null ? -1 : r.partner().convoys()).reversed();
            default -> Comparator.comparingDouble(IndustryTradeRow::remaining).reversed();
        };
        order = order.thenComparing(r -> r.partner().name());
        if (reverse) order = order.reversed();
        return view.partners().stream().filter(p -> Math.round(p.exports().getOrDefault(resource, 0.0)) > 0)
                .filter(p -> regions.isEmpty() || regions.stream().anyMatch(region -> region(p, region)))
                .map(p -> row(view, p, resource)).sorted(order).toList();
    }
}
