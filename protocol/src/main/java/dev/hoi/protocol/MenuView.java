package dev.hoi.protocol;

import java.util.List;


public record MenuView(String country, String countryName, String date, String speed, List<Page> pages, CountryHud hud, List<Manufacturer> manufacturers, Balance balance) {
    public MenuView(String country, String countryName, String date, String speed, List<Page> pages, CountryHud hud) {
        this(country, countryName, date, speed, pages, hud, List.of());
    }
    public MenuView(String country, String countryName, String date, String speed, List<Page> pages) {
        this(country, countryName, date, speed, pages, CountryHud.UNKNOWN);
    }
    public MenuView(String country, String countryName, String date, String speed, List<Page> pages, CountryHud hud, List<Manufacturer> manufacturers) {
        this(country, countryName, date, speed, pages, hud, manufacturers, null);
    }
    public record Balance(String id, String name, String left, String right, String leftIcon, String rightIcon, double value, List<Entry> ranges) {
        public Balance {
            bounded(id, 128); bounded(name, 256); bounded(left, 256); bounded(right, 256);
            bounded(leftIcon, 160); bounded(rightIcon, 160);
            if (!Double.isFinite(value) || value < -1 || value > 1) throw new IllegalArgumentException("Invalid power balance");
            ranges = List.copyOf(ranges);
            if (ranges.size() > 64) throw new IllegalArgumentException("Too many power ranges");
        }
    }
    public MenuView {
        if (hud == null) hud = CountryHud.UNKNOWN;
        manufacturers = manufacturers == null ? List.of() : List.copyOf(manufacturers);
        if (manufacturers.size() > 64 || manufacturers.stream().map(Manufacturer::id).distinct().count() != manufacturers.size())
            throw new IllegalArgumentException("Invalid manufacturer list");
        bounded(country, 64); bounded(countryName, 128); bounded(date, 64); bounded(speed, 16);
        pages = List.copyOf(pages);
        if (country.isBlank() || pages.size() != MenuTab.ORDER.size()) throw new IllegalArgumentException("Invalid menu pages");
        for (int i = 0; i < pages.size(); i++) if (pages.get(i).tab() != MenuTab.ORDER.get(i)) throw new IllegalArgumentException("Invalid menu order");
    }
    public Page page(MenuTab tab) { return pages.get(tab.ordinal()); }
    public record Manufacturer(String id, List<String> groups, Entry entry) {
        public Manufacturer {
            bounded(id,128); groups=List.copyOf(groups);
            if(id.isBlank() || entry==null || groups.isEmpty() || groups.size()>4
                    || !List.of("armor","navy","air","materiel").containsAll(groups)) throw new IllegalArgumentException("Invalid manufacturer");
        }
    }
    public record Page(MenuTab tab, List<Section> sections) {
        public Page {
            if (tab == null) throw new IllegalArgumentException("Missing menu tab");
            sections = List.copyOf(sections);
            if (sections.isEmpty() || sections.size() > 12) throw new IllegalArgumentException("Invalid menu sections");
        }
    }
    public record Section(String title, String icon, List<Entry> entries) {
        public Section {
            bounded(title, 128); bounded(icon, 32); entries = List.copyOf(entries);
            if (entries.size() > 64) throw new IllegalArgumentException("Too many menu entries");
        }
    }
    public record Entry(String name, String value, String detail, double progress, String icon) {
        public Entry {
            bounded(name, 128); bounded(value, 128); bounded(detail, 2_000);
            if (icon == null) icon = "";
            bounded(icon, 128);
            if (!icon.isEmpty() && !icon.matches("[a-z0-9_]+(?:/[a-z0-9_]+)*")) throw new IllegalArgumentException("Invalid menu icon");
            if (!Double.isFinite(progress) || progress < -1 || progress > 1) throw new IllegalArgumentException("Invalid menu progress");
        }
        public Entry(String name, String value, String detail, double progress) { this(name, value, detail, progress, ""); }
        public Entry(String name, String value, String detail) { this(name, value, detail, -1, ""); }
    }
    private static void bounded(String text, int limit) {
        if (text == null || text.length() > limit) throw new IllegalArgumentException("메뉴 데이터 한도 초과");
    }
}
