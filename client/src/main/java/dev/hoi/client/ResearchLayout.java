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
    public record Node(Tech tech, int x, int y, double scale) {
        public Node(Tech tech, int x, int y) { this(tech, x, y, 1); }
        public int width() {
            var position = TfrResearchLayout.position(tech);
            return (int)Math.round(scale * (position != null ? (position.hull() ? 64 : 28) : tech.source() == null ? CARD_WIDTH : 88));
        }
        public int height() {
            var position = TfrResearchLayout.position(tech);
            return (int)Math.round(scale * (position != null ? (position.hull() ? 32 : 28) : tech.source() == null ? CARD_HEIGHT : 42));
        }
        public boolean contains(double x, double y) {
            return x >= this.x && x < this.x + width() && y >= this.y && y < this.y + height();
        }
    }
    public static ResearchLayout create(List<Tech> technologies, String category, String query) {
        String filter = query.strip().toLowerCase(Locale.ROOT);
        var selected = technologies.stream().filter(t -> category(t).equals(category)
                && (filter.isEmpty() || t.name().toLowerCase(Locale.ROOT).contains(filter) || t.id().toLowerCase(Locale.ROOT).contains(filter)))
                .sorted(Comparator.comparingInt(Tech::year).thenComparingInt(Tech::tier).thenComparing(Tech::id)).toList();
        var reference = TfrResearchLayout.create(technologies, category, filter);
        if (reference != null) return reference;
        if(selected.stream().anyMatch(t->t.source()!=null)) return sourceLayout(selected);
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
    public static String category(Tech tech) {
        return tech.source()!=null && tech.source().folder().equals("mtgnavalsupportfolder") ? "NAVAL_SUPPORT" : tech.category();
    }
    public static List<String> parents(Tech tech) {
        var ids=new LinkedHashSet<>(tech.prerequisites());
        if(tech.source()!=null) ids.addAll(tech.source().anyOf());
        return List.copyOf(ids);
    }
    ResearchLayout withVerticalPadding(int padding) {
        return new ResearchLayout(nodes.stream().map(node -> new Node(node.tech(), node.x(), node.y() + padding, node.scale())).toList(),
                years, width, height + padding * 2);
    }
    public boolean sourceTree() { return nodes.stream().anyMatch(n->n.tech().source()!=null); }
    public boolean vertical() { return nodes.stream().filter(n->n.tech().source()!=null).findFirst().map(n->switch(n.tech().source().folder()) {
        case "nsb_armour_folder", "artillery_folder", "bba_air_techs_folder", "electronics_folder", "industry_folder" -> true;
        default -> false;
    }).orElse(false); }
    public Map<Integer,Integer> yearGuides() {
        var guides=new TreeMap<Integer,Integer>();
        if (TfrResearchLayout.applies(this)) {
            for (var label : TfrResearchLayout.yearLabels(this)) if (label.x() == (int)Math.round(4 * TfrResearchLayout.scale(this)))
                guides.put(Integer.parseInt(label.text().replace("년", "")), label.y());
            return guides;
        }
        for(var n:nodes) if(n.tech().source()!=null) guides.merge(n.tech().year(),vertical()?n.y():n.x(),Math::min);
        // Several source branches share a row but have different exact unlock years.
        // Keep the earliest row label; each card/detail retains its own exact year.
        var byPosition=new TreeMap<Integer,Integer>();
        guides.forEach((year,position)->byPosition.merge(position,year,Math::min));
        guides.clear();byPosition.forEach((position,year)->guides.put(year,position));
        return guides;
    }
    static ResearchLayout sourceLayout(List<Tech> selected) {
        var nodes=new ArrayList<Node>(); int width=1,height=1;
        var sorted=selected.stream().sorted(Comparator.comparingInt((Tech t)->t.source()==null?-1:t.source().y())
                .thenComparingInt(t->t.source()==null?-1:t.source().x()).thenComparing(Tech::id)).toList();
        for(var tech:sorted) {
            var source=tech.source();
            int x=source==null||source.x()<0?90:90+(int)Math.round(source.x()*.8);
            int y=source==null||source.y()<0?height+ROW:30+(int)Math.round(source.y()*.8);
            // A few source grids overlap. Shift only those cards so every node remains clickable.
            int cardWidth=tech.source()==null?CARD_WIDTH:88,cardHeight=tech.source()==null?CARD_HEIGHT:42;
            boolean collision;
            do {
                collision=false;
                for(var old:nodes) if(x<old.x()+old.width()+8&&x+cardWidth+8>old.x()&&y<old.y()+old.height()+8&&y+cardHeight+8>old.y()) {
                    x=old.x()+old.width()+12;collision=true;break;
                }
            } while(collision);
            nodes.add(new Node(tech,x,y));width=Math.max(width,x+cardWidth+30);height=Math.max(height,y+cardHeight+30);
        }
        return new ResearchLayout(List.copyOf(nodes),selected.stream().map(Tech::year).distinct().sorted().toList(),width,height);
    }
    private static String root(Map<String, String> groups, String id) {
        while (!groups.get(id).equals(id)) id = groups.get(id);
        return id;
    }
    public Node at(double x, double y) { return nodes.stream().filter(n -> n.contains(x, y)).findFirst().orElse(null); }
    public static double clampScroll(double value, int content, int viewport) { return Math.clamp(value, 0, Math.max(0, content - viewport)); }
}
