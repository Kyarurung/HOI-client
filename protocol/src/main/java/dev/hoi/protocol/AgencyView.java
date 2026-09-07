package dev.hoi.protocol;

import java.util.List;

/** Server-issued choices, never commands or client-calculated costs. */
public record AgencyView(String session, long revision, String country, String name, String status,
        List<Item> items, String message) {
    public AgencyView {
        text(session, 36); text(country, 64); text(name, 128); text(status, 256); text(message, 512);
        if (revision < 0) throw new IllegalArgumentException("Invalid revision");
        items = List.copyOf(items);
        if (items.size() > 512 || items.stream().map(Item::id).distinct().count() != items.size())
            throw new IllegalArgumentException("Invalid agency catalog");
    }
    public record Item(String id, String group, String title, String value, String detail, String texture,
            String button, boolean enabled, double progress, List<Parameter> parameters) {
        public Item {
            text(id, 128); text(group, 32); text(title, 128); text(value, 128); text(detail, 2000);
            text(texture, 128); text(button, 64); parameters = List.copyOf(parameters);
            if (parameters.size() > 4 || !Double.isFinite(progress) || progress < -1 || progress > 1)
                throw new IllegalArgumentException("Invalid agency item");
        }
    }
    public record Parameter(String label, List<Choice> choices) {
        public Parameter {
            text(label, 64); choices = List.copyOf(choices);
            if (choices.size() > 256 || choices.stream().map(Choice::value).distinct().count() != choices.size())
                throw new IllegalArgumentException("Invalid agency choices");
        }
    }
    public record Choice(String value, String label) {
        public Choice { text(value, 128); text(label, 128); }
    }
    static void text(String value, int limit) {
        if (value == null || value.length() > limit) throw new IllegalArgumentException("Agency data limit exceeded");
    }
}
