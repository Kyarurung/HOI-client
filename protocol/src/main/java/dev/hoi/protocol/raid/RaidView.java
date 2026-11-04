package dev.hoi.protocol.raid;

import java.util.*;
import java.util.stream.Stream;

public record RaidView(String session, long revision, String country, String message, String title, String filterTooltip,
        String expandTooltip, String collapseTooltip, String emptyTooltip, List<Category> categories, List<Target> targets, Setup setup, Window window,
        String listTitle, List<Entry> raids, List<Line> lines) {
    public static final List<String> PHASES = List.of("CREATE", "ASSEMBLING", "PREPARING", "PREPARED", "IN_PROGRESS", "ENDED");
    public static final List<String> RISKS = List.of("LOW", "MEDIUM", "HIGH");
    public static final List<String> OUTCOMES = List.of("", "FAILURE", "LIMITED_SUCCESS", "SUCCESS", "CRITICAL_SUCCESS");
    public static final List<String> TERRAINS = List.of("urban", "plains", "mountain", "marsh", "jungle", "hills", "forest", "desert", "shallow_sea");
    public RaidView {
        text(session, 36); text(country, 64); text(message, 512); text(title, 128); text(filterTooltip, 2000); text(expandTooltip, 256); text(collapseTooltip, 256); text(emptyTooltip, 256); text(listTitle, 128);
        if (revision < 0) throw new IllegalArgumentException("Invalid raid revision");
        categories = categories == null ? List.of() : List.copyOf(categories); targets = targets == null ? List.of() : List.copyOf(targets);
        raids = raids == null ? List.of() : List.copyOf(raids); lines = lines == null ? List.of() : List.copyOf(lines);
        if (categories.size() > 16 || categories.stream().map(Category::id).distinct().count() != categories.size()
                || targets.size() > 512 || targets.stream().map(Target::id).distinct().count() != targets.size()
                || raids.size() > 256 || raids.stream().map(Entry::id).distinct().count() != raids.size() || lines.size() > 256
                || !categories.stream().map(Category::id).toList().containsAll(targets.stream().map(Target::category).toList())
                || !categories.stream().map(Category::id).toList().containsAll(raids.stream().map(Entry::category).toList()))
            throw new IllegalArgumentException("Invalid raid catalog");
        if (setup != null && targets.stream().noneMatch(t -> t.id().equals(setup.target()))) throw new IllegalArgumentException("Invalid raid setup target");
    }
    public List<String> issued() {
        var result = new ArrayList<String>();
        targets.forEach(t -> { result.add(t.select()); result.add(t.quick()); });
        if (setup != null) { setup.units().forEach(u -> result.add(u.select())); setup.sources().forEach(s -> result.add(s.select())); result.add(setup.close().item()); }
        if (window != null) window.actions().forEach(a -> result.add(a.item()));
        categories.forEach(c -> result.add(c.launchAll().item()));
        raids.forEach(r -> result.add(r.open()));
        return result.stream().filter(id -> !id.isEmpty()).distinct().toList();
    }
    public record Action(String item, String label, String tooltip, boolean enabled) {
        public static final Action NONE = new Action("", "", "", false);
        public Action { text(item, 128); text(label, 128); text(tooltip, 2000); if (item.isEmpty() && enabled) throw new IllegalArgumentException("Enabled raid action needs an item"); }
    }
    public record Category(String id, String name, String icon, String tooltip, String showTooltip, String hideTooltip, Action launchAll) {
        public Category { key(id); text(name, 128); text(icon, 160); text(tooltip, 2000); text(showTooltip, 256); text(hideTooltip, 256); launchAll = launchAll == null ? Action.NONE : launchAll; }
    }
    public record Target(String id, String type, String category, String icon, String state, int province, double x, double z, String name,
            String controller, boolean allowed, String tooltip, String select, String quick) {
        public Target {
            key(id); key(type); key(category); text(icon, 160); key(state); text(name, 256); text(controller, 8); text(tooltip, 4000); text(select, 128); text(quick, 128);
            if (province < 0 || !Double.isFinite(x) || !Double.isFinite(z) || allowed == select.isEmpty()) throw new IllegalArgumentException("Invalid raid target");
        }
    }
    public record Setup(String target, String type, String title, String unitTab, String sourceTab, String unitPrompt, String sourcePrompt,
            List<Unit> units, List<Source> sources, String unit, String source, Action close) {
        public Setup {
            key(target); key(type); text(title, 128); text(unitTab, 64); text(sourceTab, 64); text(unitPrompt, 128); text(sourcePrompt, 128); text(unit, 128); text(source, 128);
            Objects.requireNonNull(close); units = List.copyOf(units); sources = List.copyOf(sources);
            if (units.size() > 256 || units.stream().map(Unit::id).distinct().count() != units.size() || sources.size() > 256
                    || sources.stream().map(Source::province).distinct().count() != sources.size()
                    || !unit.isEmpty() && units.stream().noneMatch(u -> u.id().equals(unit)) || !source.isEmpty() && sources.stream().noneMatch(s -> s.province().equals(source)))
                throw new IllegalArgumentException("Invalid raid setup");
        }
    }
    public record Unit(String id, String name, boolean air, String icon, int level, String modifier, String tooltip, String select) {
        public Unit { text(id, 128); text(name, 128); text(icon, 160); text(modifier, 64); text(tooltip, 2000); text(select, 128); if (level < 0 || level > 6) throw new IllegalArgumentException("Invalid raid unit level"); }
    }
    public record Source(String province, String name, String distance, String tooltip, String select) {
        public Source { text(province, 128); text(name, 128); text(distance, 64); text(tooltip, 1000); text(select, 128); }
    }
    public record Equipment(String id, String name, String collected, String required, boolean complete) {
        public Equipment { text(id, 128); text(name, 128); text(collected, 32); text(required, 32); }
    }
    public record Choice(String value, String label, String tooltip) {
        public Choice { key(value); text(label, 128); text(tooltip, 1000); }
    }
    public record Risk(String id, String label, String tooltip, Action select) {
        public Risk { if (!RISKS.contains(id)) throw new IllegalArgumentException("Invalid raid risk"); text(label, 64); text(tooltip, 1000); Objects.requireNonNull(select); }
    }
    public record Feedback(String title, String outcome, String actor, String victim, String name, String unit, String target,
            String actorEffects, String victimEffects, String tooltip, Action dismiss) {
        public Feedback {
            text(title, 128); if (!OUTCOMES.contains(outcome) || outcome.isEmpty()) throw new IllegalArgumentException("Invalid raid outcome");
            text(actor, 8); text(victim, 8); text(name, 256); text(unit, 256); text(target, 256); text(actorEffects, 2000); text(victimEffects, 2000); text(tooltip, 1000); Objects.requireNonNull(dismiss);
        }
    }
    public record Window(String raid, String phase, String title, String type, String unit, String unitIcon, String target, String targetIcon, String terrain,
            String chanceHeader, String chanceLevel, String chance, String chanceTooltip, String disaster, String disasterTooltip,
            String equipmentHeader, List<String> equipmentColumns, List<Equipment> equipment, String equipmentTooltip,
            String riskHeader, String risk, List<Risk> risks, String preparationHeader, String preparation, String status, String intel, String intelTooltip,
            double progress, String progressText, String remaining, Action create, Action start, Action cancel, Action close,
            Action auto, String autoLabel, List<Choice> autoOptions, String autoSelected, Feedback feedback) {
        public Window {
            text(raid, 128); if (!PHASES.contains(phase) || phase.equals("CREATE") != raid.isEmpty()) throw new IllegalArgumentException("Invalid raid window phase");
            text(title, 128); text(type, 128); text(unit, 256); text(unitIcon, 160); text(target, 256); text(targetIcon, 160);
            if (!TERRAINS.contains(terrain)) throw new IllegalArgumentException("Invalid raid terrain");
            text(chanceHeader, 64); text(chanceLevel, 64); text(chance, 32); text(chanceTooltip, 4000); text(disaster, 32); text(disasterTooltip, 2000);
            text(equipmentHeader, 64); equipmentColumns = List.copyOf(equipmentColumns); equipmentColumns.forEach(c -> text(c, 64)); equipment = List.copyOf(equipment); text(equipmentTooltip, 1000);
            text(riskHeader, 64); if (!RISKS.contains(risk)) throw new IllegalArgumentException("Invalid raid risk"); risks = List.copyOf(risks);
            text(preparationHeader, 64); text(preparation, 128); text(status, 128); text(intel, 64); text(intelTooltip, 2000);
            if (!Double.isFinite(progress) || progress < 0 || progress > 1) throw new IllegalArgumentException("Invalid raid progress");
            text(progressText, 128); text(remaining, 128); Stream.of(create, start, cancel, close, auto).forEach(Objects::requireNonNull);
            text(autoLabel, 64); autoOptions = List.copyOf(autoOptions); text(autoSelected, 64);
            if (equipmentColumns.size() != 3 || equipment.size() > 32 || risks.size() != 3 || autoOptions.size() > 8
                    || !autoSelected.isEmpty() && autoOptions.stream().noneMatch(o -> o.value().equals(autoSelected)) || phase.equals("ENDED") != (feedback != null))
                throw new IllegalArgumentException("Invalid raid window");
        }
        public List<Action> actions() {
            var result = new ArrayList<>(List.of(create, start, cancel, close, auto));
            risks.forEach(r -> result.add(r.select()));
            if (feedback != null) result.add(feedback.dismiss());
            return result;
        }
    }
    public record Entry(String id, String category, String phase, String icon, String description, String status, String outcome, String chance, String tooltip, String open, double progress) {
        public Entry {
            text(id, 128); key(category); if (!PHASES.contains(phase) || phase.equals("CREATE") || !OUTCOMES.contains(outcome)) throw new IllegalArgumentException("Invalid raid entry");
            text(icon, 160); text(description, 256); text(status, 128); text(chance, 32); text(tooltip, 2000); text(open, 128);
            if (!Double.isFinite(progress) || progress < 0 || progress > 1) throw new IllegalArgumentException("Invalid raid entry progress");
        }
    }
    public record Line(String raid, String arrow, double fromX, double fromZ, double toX, double toZ, double progress) {
        public Line {
            text(raid, 128); if (!Set.of("air", "land", "sea").contains(arrow) || !Stream.of(fromX, fromZ, toX, toZ, progress).allMatch(Double::isFinite) || progress < 0 || progress > 1)
                throw new IllegalArgumentException("Invalid raid line");
        }
    }
    private static void key(String value) { if (value == null || !value.matches("[A-Za-z0-9_:/|.-]{1,128}")) throw new IllegalArgumentException("Invalid raid key"); }
    static void text(String value, int limit) { if (value == null || value.length() > limit) throw new IllegalArgumentException("Raid data limit exceeded"); }
}
