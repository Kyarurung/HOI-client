package dev.hoi.protocol;

import java.util.List;
import java.util.Objects;

/** Private, read-only presentation data. Contains no other country's state or executable effects. */
public record ResearchView(String session, long revision, String country, String countryName,
                           long day, String date, String speed, List<Slot> slots, List<Tech> technologies, String message, CountryHud hud) {
    public ResearchView(String session, long revision, String country, String countryName, long day, String date,
                        String speed, List<Slot> slots, List<Tech> technologies, String message) {
        this(session, revision, country, countryName, day, date, speed, slots, technologies, message, CountryHud.UNKNOWN);
    }
    public ResearchView {
        if (hud == null) hud = CountryHud.UNKNOWN;
        Objects.requireNonNull(session); Objects.requireNonNull(country); Objects.requireNonNull(countryName);
        Objects.requireNonNull(date); Objects.requireNonNull(speed); Objects.requireNonNull(message);
        slots = List.copyOf(slots); technologies = List.copyOf(technologies);
        if (slots.size() > 16 || technologies.size() > 512) throw new IllegalArgumentException("연구 화면 데이터 한도 초과");
        if (day < 0 || revision < 0 || !country.isEmpty() && slots.isEmpty()) throw new IllegalArgumentException("Invalid research view");
        var ids = new java.util.HashSet<String>();
        for (var technology : technologies) if (!ids.add(technology.id())) throw new IllegalArgumentException("Duplicate technology");
        var assigned = new java.util.HashSet<String>();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (slot.index() != i || slot.savedDays() < 0 || slot.savedDays() > 30 || slot.technology() == null
                    || !slot.technology().isEmpty() && (!ids.contains(slot.technology()) || !assigned.add(slot.technology())))
                throw new IllegalArgumentException("Invalid research slot");
        }
    }
    public enum Status { LOCKED, AVAILABLE, ACTIVE, COMPLETED }
    public record Slot(int index, String technology, int savedDays) {}
    public record Tech(String id, String category, String name, int year, int tier, int baseDays,
                       double progress, double dailyRate, List<String> prerequisites,
                       List<String> effects, List<String> unlocks, Status status) {
        public Tech {
            Objects.requireNonNull(id); Objects.requireNonNull(category); Objects.requireNonNull(name);
            Objects.requireNonNull(status);
            prerequisites = List.copyOf(prerequisites); effects = List.copyOf(effects); unlocks = List.copyOf(unlocks);
            if (baseDays < 1 || !Double.isFinite(progress) || progress < 0
                    || !Double.isFinite(dailyRate) || dailyRate <= 0) throw new IllegalArgumentException("Invalid research progress");
        }
        public double fraction() { return status == Status.COMPLETED ? 1 : Math.clamp(progress / baseDays, 0, 1); }
        /** Estimate at today's rate; future dates, modifiers and saved slot credit can change it. */
        public long remainingDays(int savedDays) {
            return status == Status.COMPLETED ? 0 : Math.max(1, (long)Math.ceil((baseDays - progress) / dailyRate - savedDays));
        }
    }
    public Tech technology(String id) {
        return technologies.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }
}
