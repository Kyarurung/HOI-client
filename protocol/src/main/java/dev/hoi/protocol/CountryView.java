package dev.hoi.protocol;

import java.util.*;

/** Already redacted by the server; never contains the target's full menu or HUD. */
public record CountryView(String viewer, CountryHud hud, String target, String name, boolean shared,
        List<Choice> countries, List<MenuView.Entry> diplomacy, List<MenuView.Entry> focuses, List<Report> reports) {
    public CountryView {
        tag(viewer); tag(target); text(name, 128); Objects.requireNonNull(hud);
        countries = bounded(countries, 64); diplomacy = bounded(diplomacy, 64); focuses = bounded(focuses, 64); reports = bounded(reports, 4);
        if (reports.size() != 4 || new HashSet<>(reports.stream().map(Report::domain).toList()).size() != 4)
            throw new IllegalArgumentException("Missing intelligence domains");
    }
    public record Choice(String tag, String name) { public Choice { CountryView.tag(tag); text(name, 128); } }
    public record Report(IntelDomain domain, double percent, List<MenuView.Entry> entries) {
        public Report {
            Objects.requireNonNull(domain);
            if (!Double.isFinite(percent) || percent < 0 || percent > 100) throw new IllegalArgumentException("Invalid intelligence");
            entries = bounded(entries, 128);
        }
    }
    public Report report(IntelDomain domain) { return reports.stream().filter(r -> r.domain() == domain).findFirst().orElseThrow(); }
    static void tag(String value) { if (value == null || !value.matches("[A-Z0-9_]{1,16}")) throw new IllegalArgumentException("Invalid country"); }
    static void text(String value, int max) { if (value == null || value.length() > max) throw new IllegalArgumentException("Text limit"); }
    static <T> List<T> bounded(List<T> value, int max) {
        var result = List.copyOf(value); if (result.size() > max) throw new IllegalArgumentException("List limit"); return result;
    }
}
