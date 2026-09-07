package dev.hoi.client;

import dev.hoi.protocol.ResearchView.Tech;
import java.util.*;

/** Deterministic presentation layout, independent of Minecraft and of gameplay mutations. */
public record ResearchLayout(List<Node> nodes, List<Integer> years, int width, int height) {
    public static final int CARD_WIDTH = 152, CARD_HEIGHT = 58, COLUMN = 188, ROW = 88;
    // Screenshot year guides are presentation coordinates, independent of the campaign epoch.
    // Unlisted years from a server's saved catalog are retained as additional columns.
    private static final Map<String, List<Integer>> TIMELINES = Map.of(
            "INFANTRY", List.of(2000, 2006, 2012, 2018, 2021, 2024, 2027, 2030, 2033, 2036),
            "SUPPORT", List.of(2018, 2020, 2024, 2028, 2032),
            "ARTILLERY", List.of(2000, 2006, 2012, 2018, 2021, 2024, 2027, 2030, 2033, 2036),
            "ARMOR", List.of(1980, 2018, 2020, 2022, 2024, 2028, 2032),
            "NAVY", List.of(1980, 2000, 2020, 2024, 2028, 2032),
            "AIR", List.of(2000, 2020, 2024, 2028, 2032, 2036, 2040),
            "ENGINEERING", List.of(2015, 2019, 2021, 2023, 2025, 2027, 2029, 2031, 2033, 2035),
            "INDUSTRY", List.of(2020, 2021, 2023, 2025, 2027, 2029, 2031, 2033, 2035));
    public record Node(Tech tech, int x, int y) {
        public boolean contains(double x, double y) {
            return x >= this.x && x < this.x + CARD_WIDTH && y >= this.y && y < this.y + CARD_HEIGHT;
        }
    }
    public static ResearchLayout create(List<Tech> technologies, String category, String query) {
        String filter = query.strip().toLowerCase(Locale.ROOT);
        var selected = technologies.stream().filter(t -> t.category().equals(category)
                && (filter.isEmpty() || t.name().toLowerCase(Locale.ROOT).contains(filter) || t.id().toLowerCase(Locale.ROOT).contains(filter)))
                .sorted(Comparator.comparingInt(Tech::year).thenComparingInt(Tech::tier).thenComparing(Tech::id)).toList();
        var yearSet = new TreeSet<Integer>();
        if (filter.isEmpty()) yearSet.addAll(TIMELINES.getOrDefault(category, List.of()));
        selected.forEach(tech -> yearSet.add(tech.year()));
        var years = List.copyOf(yearSet);
        // Keep prerequisite families on a stable lane instead of independently sorting each year.
        var ids = new HashMap<String, Tech>(); selected.forEach(t -> ids.put(t.id(), t));
        var groups = new HashMap<String, String>(); selected.forEach(t -> groups.put(t.id(), t.id()));
        for (var tech : selected) for (var parent : tech.prerequisites()) if (ids.containsKey(parent)) {
            String a = root(groups, tech.id()), b = root(groups, parent);
            if (!a.equals(b)) groups.put(a.compareTo(b) > 0 ? a : b, a.compareTo(b) > 0 ? b : a);
        }
        var sizes = new HashMap<String, Integer>(); selected.forEach(t -> sizes.merge(root(groups, t.id()), 1, Integer::sum));
        var lanes = new TreeMap<String, Integer>();
        sizes.entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).sorted().forEach(id -> lanes.put(id, lanes.size()));
        var occupied = new HashMap<Integer, Set<Integer>>(); var nodes = new ArrayList<Node>(); int maxRow = 0;
        for (var tech : selected) {
            int column = years.indexOf(tech.year()), row = lanes.getOrDefault(root(groups, tech.id()), lanes.size());
            var used = occupied.computeIfAbsent(column, key -> new HashSet<>());
            while (used.contains(row)) row++;
            used.add(row); maxRow = Math.max(maxRow, row + 1);
            nodes.add(new Node(tech, 20 + column * COLUMN, 18 + row * ROW));
        }
        return new ResearchLayout(List.copyOf(nodes), years, Math.max(1, years.size()) * COLUMN + 20, maxRow * ROW + 28);
    }
    private static String root(Map<String, String> groups, String id) {
        while (!groups.get(id).equals(id)) id = groups.get(id);
        return id;
    }
    public Node at(double x, double y) { return nodes.stream().filter(n -> n.contains(x, y)).findFirst().orElse(null); }
    public static double clampScroll(double value, int content, int viewport) { return Math.clamp(value, 0, Math.max(0, content - viewport)); }
}
