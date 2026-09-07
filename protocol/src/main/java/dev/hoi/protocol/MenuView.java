package dev.hoi.protocol;

import java.util.List;

/** Private read-only presentation. Contains no executable effects or commands. */
public record MenuView(String country, String countryName, String date, String speed, List<Page> pages) {
    public MenuView {
        bounded(country, 64); bounded(countryName, 128); bounded(date, 64); bounded(speed, 16);
        pages = List.copyOf(pages);
        if (country.isBlank() || pages.size() != MenuTab.ORDER.size()) throw new IllegalArgumentException("Invalid menu pages");
        for (int i = 0; i < pages.size(); i++) if (pages.get(i).tab() != MenuTab.ORDER.get(i)) throw new IllegalArgumentException("Invalid menu order");
    }
    public Page page(MenuTab tab) { return pages.get(tab.ordinal()); }
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
    public record Entry(String name, String value, String detail, double progress) {
        public Entry {
            bounded(name, 128); bounded(value, 128); bounded(detail, 2_000);
            if (!Double.isFinite(progress) || progress < -1 || progress > 1) throw new IllegalArgumentException("Invalid menu progress");
        }
        public Entry(String name, String value, String detail) { this(name, value, detail, -1); }
    }
    private static void bounded(String text, int limit) {
        if (text == null || text.length() > limit) throw new IllegalArgumentException("메뉴 데이터 한도 초과");
    }
}
