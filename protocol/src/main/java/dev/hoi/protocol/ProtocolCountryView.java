package dev.hoi.protocol;

import java.util.*;

public record ProtocolCountryView(String viewer, ProtocolCountryHud hud, String target, String name, boolean shared,
        List<Choice> countries, List<ProtocolMenuView.Entry> diplomacy, List<ProtocolMenuView.Entry> focuses, List<Report> reports, Actions actions, List<Relation> relations) {
    public ProtocolCountryView(String viewer, ProtocolCountryHud hud, String target, String name, boolean shared,
            List<Choice> countries, List<ProtocolMenuView.Entry> diplomacy, List<ProtocolMenuView.Entry> focuses, List<Report> reports, Actions actions) {
        this(viewer, hud, target, name, shared, countries, diplomacy, focuses, reports, actions, List.of());
    }
    public ProtocolCountryView(String viewer, ProtocolCountryHud hud, String target, String name, boolean shared,
            List<Choice> countries, List<ProtocolMenuView.Entry> diplomacy, List<ProtocolMenuView.Entry> focuses, List<Report> reports) {
        this(viewer, hud, target, name, shared, countries, diplomacy, focuses, reports, null);
    }
    public record Actions(String session, long revision, String message, List<Action> entries) {
        public Actions { UUID.fromString(session); if (revision < 0) throw new IllegalArgumentException("Invalid revision"); text(message, 1000); entries = bounded(entries, 24); }
    }
    public record Action(String id, String name, String detail, boolean enabled) {
        public Action { text(id, 64); text(name, 128); text(detail, 1000); }
    }
    public ProtocolCountryView {
        tag(viewer); tag(target); text(name, 128); Objects.requireNonNull(hud);
        countries = bounded(countries, 64); diplomacy = bounded(diplomacy, 64); focuses = bounded(focuses, 64); reports = bounded(reports, 4);
        relations = relations == null ? List.of() : bounded(relations, 24);
        if (reports.size() != 4 || new HashSet<>(reports.stream().map(Report::domain).toList()).size() != 4)
            throw new IllegalArgumentException("Missing intelligence domains");
    }
    public record Relation(String name, String icon, List<Choice> countries, String detail) {
        public Relation(String name, String icon, List<Choice> countries) { this(name, icon, countries, ""); }
        public Relation {
            text(name, 128); text(icon, 160);
            detail = detail == null ? "" : detail;
            text(detail, 1000);
            if (!icon.matches("[a-z0-9_/]+")) throw new IllegalArgumentException("Invalid relation icon");
            countries = bounded(countries, 64);
            if (countries.stream().map(Choice::tag).distinct().count() != countries.size()) throw new IllegalArgumentException("Duplicate relation country");
        }
    }
    public record Choice(String tag, String name) { public Choice { ProtocolCountryView.tag(tag); text(name, 128); } }
    public record Report(ProtocolIntelDomain domain, double percent, List<ProtocolMenuView.Entry> entries) {
        public Report {
            Objects.requireNonNull(domain);
            if (!Double.isFinite(percent) || percent < 0 || percent > 100) throw new IllegalArgumentException("Invalid intelligence");
            entries = bounded(entries, 128);
        }
    }
    public Report report(ProtocolIntelDomain domain) { return reports.stream().filter(r -> r.domain() == domain).findFirst().orElseThrow(); }
    static void tag(String value) { if (value == null || !value.matches("[A-Z0-9_]{1,16}")) throw new IllegalArgumentException("Invalid country"); }
    static void text(String value, int max) { if (value == null || value.length() > max) throw new IllegalArgumentException("Text limit"); }
    static <T> List<T> bounded(List<T> value, int max) {
        var result = List.copyOf(value); if (result.size() > max) throw new IllegalArgumentException("List limit"); return result;
    }
}
