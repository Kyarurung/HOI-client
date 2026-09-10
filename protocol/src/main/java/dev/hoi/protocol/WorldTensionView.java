package dev.hoi.protocol;

import java.util.List;
import java.util.Objects;

public record WorldTensionView(String viewer, CountryHud hud, long day, int offset, int total,
                               List<Entry> entries, List<War> wars) {
    public static final int PAGE_SIZE = 64;
    public WorldTensionView {
        CountryView.tag(viewer); Objects.requireNonNull(hud);
        entries = List.copyOf(entries); wars = List.copyOf(wars);
        if (day < 0 || offset < 0 || total < 0 || offset > total || entries.size() > PAGE_SIZE
                || offset + entries.size() > total || wars.size() > PAGE_SIZE) throw new IllegalArgumentException("Invalid tension page");
    }
    public enum Sort { DATE, COUNTRY, TENSION, WARS }
    public record Entry(String id, String country, String name, long day, Double initial, double current, String reason) {
        public Entry {
            CountryView.text(id, 128); CountryView.tag(country); CountryView.text(name, 256); CountryView.text(reason, 2000);
            if (day < 0 || !Double.isFinite(current) || initial != null && !Double.isFinite(initial)) throw new IllegalArgumentException("Invalid tension entry");
        }
    }
    public record War(String id, String name, long day, Double balance, List<Participant> attackers, List<Participant> defenders) {
        public War {
            CountryView.text(id, 128); CountryView.text(name, 512); ratio(balance);
            attackers = List.copyOf(attackers); defenders = List.copyOf(defenders);
            if (day < 0 || attackers.size() > 64 || defenders.size() > 64) throw new IllegalArgumentException("Invalid war");
        }
    }
    public record Participant(String country, String name, String manpower, String divisions, String factories,
                              Long casualties, Double contribution, Double surrender, boolean capitulated,
                              boolean callable, boolean leader) {
        public Participant {
            CountryView.tag(country); CountryView.text(name, 256);
            for (String value : List.of(manpower, divisions, factories)) CountryView.text(value, 64);
            ratio(contribution); ratio(surrender);
            if (casualties != null && casualties < 0) throw new IllegalArgumentException("Negative casualties");
        }
    }
    private static void ratio(Double value) {
        if (value != null && (!Double.isFinite(value) || value < 0 || value > 1)) throw new IllegalArgumentException("Invalid ratio");
    }
}
