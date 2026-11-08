package dev.hoi.protocol.country;

import dev.hoi.protocol.agency.AgencyIntelDomain;
import dev.hoi.protocol.menu.MenuView;
import java.util.*;

public record CountryView(String viewer, CountryHud hud, String target, String name, boolean shared,
        List<Choice> countries, List<MenuView.Entry> diplomacy, List<MenuView.Entry> focuses, List<Report> reports, Actions actions, List<Relation> relations, Opinion opinion, List<Art> art) {
    public CountryView(String viewer, CountryHud hud, String target, String name, boolean shared,
            List<Choice> countries, List<MenuView.Entry> diplomacy, List<MenuView.Entry> focuses, List<Report> reports, Actions actions, List<Relation> relations, Opinion opinion) {
        this(viewer, hud, target, name, shared, countries, diplomacy, focuses, reports, actions, relations, opinion, List.of());
    }
    public CountryView(String viewer, CountryHud hud, String target, String name, boolean shared,
            List<Choice> countries, List<MenuView.Entry> diplomacy, List<MenuView.Entry> focuses, List<Report> reports, Actions actions, List<Relation> relations) {
        this(viewer, hud, target, name, shared, countries, diplomacy, focuses, reports, actions, relations, null);
    }
    public CountryView(String viewer, CountryHud hud, String target, String name, boolean shared,
            List<Choice> countries, List<MenuView.Entry> diplomacy, List<MenuView.Entry> focuses, List<Report> reports, Actions actions) {
        this(viewer, hud, target, name, shared, countries, diplomacy, focuses, reports, actions, List.of());
    }
    public CountryView(String viewer, CountryHud hud, String target, String name, boolean shared,
            List<Choice> countries, List<MenuView.Entry> diplomacy, List<MenuView.Entry> focuses, List<Report> reports) {
        this(viewer, hud, target, name, shared, countries, diplomacy, focuses, reports, null);
    }
    public record Actions(String session, long revision, String message, List<Action> entries) {
        public Actions { UUID.fromString(session); if (revision < 0) throw new IllegalArgumentException("Invalid revision"); text(message, 1000); entries = bounded(entries, 64); }
    }
    public record AmountInput(String equipmentName, long stock) {
        public AmountInput {text(equipmentName,128);if(stock<0)throw new IllegalArgumentException("Invalid equipment stock");}
    }
    public record Action(String id, String name, String detail, boolean enabled, AmountInput amountInput, List<Cost> costs, boolean declined) {
        public Action(String id,String name,String detail,boolean enabled) {this(id,name,detail,enabled,null);}
        public Action(String id,String name,String detail,boolean enabled,AmountInput amountInput) {this(id,name,detail,enabled,amountInput,List.of(),false);}
        public Action { text(id, 64); text(name, 128); text(detail, 1000); costs = costs == null ? List.of() : bounded(costs, 4); }
    }
    public record Cost(String icon, String amount) {
        public static final List<String> ICONS = List.of("political_power", "command_power");
        public Cost { text(amount, 16); if (!ICONS.contains(icon)) throw new IllegalArgumentException("Invalid action cost"); }
    }
    public record Opinion(int ours, int theirs) {
        public Opinion { if (Math.abs(ours) > 1000 || Math.abs(theirs) > 1000) throw new IllegalArgumentException("Invalid opinion"); }
    }
    public record Art(String id, String icon, int x, int y, int width, int height, String title, String tooltip) {
        public Art {
            text(id, 64); text(icon, 128); text(title, 256); text(tooltip, 4000);
            if (!id.matches("[A-Za-z0-9_]+") || !icon.matches("[a-z0-9_/]+") || width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid country art");
        }
    }
    public CountryView {
        tag(viewer); tag(target); text(name, 128); Objects.requireNonNull(hud);
        countries = bounded(countries, 64); diplomacy = bounded(diplomacy, 64); focuses = bounded(focuses, 64); reports = bounded(reports, 4);
        relations = relations == null ? List.of() : bounded(relations, 24);
        art = art == null ? List.of() : bounded(art, 8);
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
    public record Choice(String tag, String name) { public Choice { CountryView.tag(tag); text(name, 128); } }
    public record Report(AgencyIntelDomain domain, double percent, List<MenuView.Entry> entries, String tooltip, List<Section> sections) {
        public Report(AgencyIntelDomain domain, double percent, List<MenuView.Entry> entries, String tooltip) { this(domain, percent, entries, tooltip, List.of()); }
        public Report(AgencyIntelDomain domain, double percent, List<MenuView.Entry> entries) { this(domain, percent, entries, ""); }
        public Report {
            Objects.requireNonNull(domain);
            if (!Double.isFinite(percent) || percent < 0 || percent > 100) throw new IllegalArgumentException("Invalid intelligence");
            entries = bounded(entries, 128);
            tooltip = tooltip == null ? "" : tooltip;
            text(tooltip, 4000);
            sections = sections == null ? List.of() : bounded(sections, 8);
            if (sections.stream().map(Section::id).distinct().count() != sections.size()) throw new IllegalArgumentException("Duplicate intelligence section");
        }
    }
    public record Section(String id, String state, String text, String tooltip, List<Row> rows) {
        public static final List<String> STATES = List.of("shown", "no_intel");
        public Section {
            CountryView.text(id, 32); CountryView.text(text, 128); tooltip = tooltip == null ? "" : tooltip; CountryView.text(tooltip, 1000);
            if (!id.matches("[a-z_]+") || !STATES.contains(state)) throw new IllegalArgumentException("Invalid intelligence section");
            rows = bounded(rows, 96);
        }
    }
    public record Row(String group, boolean header, String name, String value, String detail, String icon) {
        public Row {
            text(group, 64); text(name, 128); text(value, 64); text(detail, 1000); text(icon, 128);
            if (!icon.matches("[a-z0-9_:./-]*")) throw new IllegalArgumentException("Invalid intelligence row icon");
        }
    }
    public Report report(AgencyIntelDomain domain) { return reports.stream().filter(r -> r.domain() == domain).findFirst().orElseThrow(); }
    public static void tag(String value) { if (value == null || !value.matches("[A-Z0-9_]{1,16}")) throw new IllegalArgumentException("Invalid country"); }
    public static void text(String value, int max) { if (value == null || value.length() > max) throw new IllegalArgumentException("Text limit"); }
    public static <T> List<T> bounded(List<T> value, int max) {
        var result = List.copyOf(value); if (result.size() > max) throw new IllegalArgumentException("List limit"); return result;
    }
}
