package dev.hoi.client;

import dev.hoi.protocol.ResearchView.Tech;
import java.util.*;

/** Presentation coordinates from the supplied TFR armor/industry references, not gameplay years. */
final class TfrResearchLayout {
    private static final String PREFIX = "hoi:tfr/technology/";
    private static final Map<String, Position> ARMOR = armor();
    private static final Map<String, Position> INDUSTRY = industry();
    private static final List<Integer> ARMOR_YEARS = List.of(1980, 2018, 2020, 2022, 2024, 2028, 2032);
    private static final List<Integer> AMMUNITION_YEARS = List.of(1980, 2000, 2012, 2020, 2022, 2024, 2026);
    private static final List<Integer> INDUSTRY_YEARS = List.of(2020, 2021, 2023, 2025, 2027, 2029, 2031, 2033, 2035);

    record Position(int centerX, int centerY, boolean hull) {}
    record Label(String text, int x, int y) {}
    record Segment(int x1, int y1, int x2, int y2) {}
    record Connection(Tech parent, List<Segment> segments) {}
    record Choice(int x, int y, boolean horizontal) {}
    record Group(int x, int y, int width, int height, String caption) {}

    private TfrResearchLayout() {}

    private static Map<String, Position> armor() {
        var p = new LinkedHashMap<String, Position>();
        lane(p, 80, false, new int[]{84, 128, 216, 282}, "engine_tech_1", "engine_tech_2", "engine_tech_3", "engine_tech_4");
        lane(p, 202, true, new int[]{40, 84, 172, 216}, "gwtank_chassis", "basic_light_tank_chassis", "improved_light_tank_chassis", "advanced_light_tank_chassis");
        lane(p, 334, true, new int[]{40, 128, 216, 304}, "basic_medium_tank_chassis", "improved_medium_tank_chassis", "advanced_medium_tank_chassis", "main_battle_tank_chassis");
        lane(p, 268, false, new int[]{40, 128, 216}, "auto_loader_tech", "third_gen_nv_tech", "unmanned_turret_tech");
        lane(p, 400, false, new int[]{40, 216}, "blow_out_compartment_tech", "third_gen_thermal_tech");
        lane(p, 136, false, new int[]{40}, "legacy_armor_designs");
        lane(p, 422, true, new int[]{84}, "amphibious_tank_chassis");
        lane(p, 422, false, new int[]{128}, "amphibious_drive");
        lane(p, 570, false, new int[]{40, 84, 194}, "armor_tech_1", "armor_tech_2", "armor_tech_3");
        lane(p, 548, false, new int[]{282}, "armor_tech_4");
        lane(p, 592, false, new int[]{282}, "armor_tech_5");
        lane(p, 700, false, new int[]{62, 106, 172, 216}, "turret_mounted_mg_tech", "turret_mounted_atgm_tech", "auto_cannon_tech", "turret_mounted_manpad_tech");
        lane(p, 744, false, new int[]{62, 106, 172, 216}, "apds_tech", "heat_tech", "apfsds_tech", "heatfs_tech");
        return Map.copyOf(p);
    }

    private static Map<String, Position> industry() {
        var p = new LinkedHashMap<String, Position>();
        lane(p, 100, false, new int[]{40, 128, 172, 216, 304, 348, 392}, "basic_machine_tools", "improved_machine_tools", "advanced_machine_tools", "assembly_line_production", "industrial_software", "industrial_software2", "industrial_software3");
        lane(p, 78, false, new int[]{260, 436}, "flexible_line", "industrial_software4a");
        lane(p, 122, false, new int[]{260, 436}, "streamlined_line", "industrial_software4b");
        lane(p, 144, false, new int[]{172, 304}, "improved_equipment_conversion", "advanced_equipment_conversion");
        for (int i = 1; i <= 9; i++) {
            p.put("concentrated_industry" + (i == 1 ? "" : i), new Position(188, 40 + i * 44, false));
            p.put("dispersed_industry" + (i == 1 ? "" : i), new Position(232, 40 + i * 44, false));
            p.put("construction" + i, new Position(276, 40 + i * 44, false));
        }
        for (int i = 1; i <= 5; i++) {
            p.put("excavation" + i, new Position(320, 84 + (i - 1) * 88, false));
            p.put("fuel_refining" + (i == 1 ? "" : i), new Position(540, 84 + (i - 1) * 88, false));
        }
        lane(p, 518, false, new int[]{40}, "fuel_silos");
        lane(p, 474, false, new int[]{84}, "synth_oil_experiments");
        lane(p, 452, false, new int[]{128, 216, 304, 392}, "oil_processing", "improved_oil_processing", "advanced_oil_processing", "modern_oil_processing");
        lane(p, 496, false, new int[]{128, 216, 304, 392}, "rubber_processing", "improved_rubber_processing", "advanced_rubber_processing", "modern_rubber_processing");
        return Map.copyOf(p);
    }

    private static void lane(Map<String, Position> positions, int x, boolean hull, int[] rows, String... ids) {
        if (rows.length != ids.length) throw new IllegalArgumentException("Research lane length");
        for (int i = 0; i < ids.length; i++) positions.put(ids[i], new Position(x, rows[i], hull));
    }

    private static Map<String, Position> positions(String category) {
        return switch (category) { case "ARMOR" -> ARMOR; case "INDUSTRY" -> INDUSTRY; default -> TfrResearchPlacements.POSITIONS; };
    }

    static Position position(Tech tech) {
        return tech.source() != null && tech.id().equals(PREFIX + tech.source().id())
                ? positions(ResearchLayout.category(tech)).get(tech.source().id()) : null;
    }

    static boolean applies(ResearchLayout layout) {
        return layout.nodes().stream().anyMatch(n -> position(n.tech()) != null);
    }

    static boolean iconOnly(Tech tech) {
        var position = position(tech);
        return position != null && !position.hull();
    }

    static ResearchLayout create(List<Tech> technologies, String category, String filter) {
        var available = technologies.stream().filter(t -> ResearchLayout.category(t).equals(category)).toList();
        if (available.stream().noneMatch(t -> position(t) != null)) return null;
        var nodes = new ArrayList<ResearchLayout.Node>();
        int width = category.equals("ARMOR") ? 856 : category.equals("INDUSTRY") ? 640 : 160;
        int height = category.equals("ARMOR") ? 346 : category.equals("INDUSTRY") ? 478 : 100;
        for (var tech : available) {
            var position = position(tech);
            if (position == null) continue;
            int w = position.hull() ? 64 : 28, h = position.hull() ? 32 : 28;
            nodes.add(new ResearchLayout.Node(tech, position.centerX() - w / 2, position.centerY() - h / 2));
            width = Math.max(width, position.centerX() + w / 2 + 64);
            height = Math.max(height, position.centerY() + h / 2 + 38);
        }
        // Saved/custom additions stay accessible below the reference, without moving its lanes.
        var extra = ResearchLayout.sourceLayout(available.stream().filter(t -> position(t) == null).toList());
        for (var node : extra.nodes()) nodes.add(new ResearchLayout.Node(node.tech(), node.x(), height + node.y()));
        if (!extra.nodes().isEmpty()) { width = Math.max(width, extra.width()); height += extra.height(); }
        nodes.sort(Comparator.comparingInt(ResearchLayout.Node::y).thenComparingInt(ResearchLayout.Node::x).thenComparing(n -> n.tech().id()));
        var visible = nodes.stream().filter(n -> filter.isEmpty() || n.tech().name().toLowerCase(Locale.ROOT).contains(filter)
                || n.tech().id().toLowerCase(Locale.ROOT).contains(filter)).toList();
        return new ResearchLayout(visible, available.stream().map(Tech::year).distinct().sorted().toList(), width, height);
    }

    static double scale(ResearchLayout layout) {
        return layout.nodes().isEmpty() ? 1 : layout.nodes().getFirst().scale();
    }

    static ResearchLayout fit(ResearchLayout layout, int viewport) {
        double scale = Math.clamp(viewport / 800.0, 1, 1.6);
        if (scale == 1) return layout;
        var nodes = layout.nodes().stream().map(n -> new ResearchLayout.Node(n.tech(),
                (int)Math.round((n.x() + n.width() / 2.0) * scale) - (int)Math.round(n.width() * scale) / 2,
                (int)Math.round((n.y() + n.height() / 2.0) * scale) - (int)Math.round(n.height() * scale) / 2, scale)).toList();
        return new ResearchLayout(nodes, layout.years(), (int)Math.ceil(layout.width() * scale), (int)Math.ceil(layout.height() * scale));
    }

    private static List<Label> scaleLabels(List<Label> labels, ResearchLayout layout) {
        double scale = scale(layout);
        return labels.stream().map(l -> new Label(l.text(), (int)Math.round(l.x() * scale), (int)Math.round(l.y() * scale))).toList();
    }

    static List<Label> yearLabels(ResearchLayout layout) {
        String category = ResearchLayout.category(layout.nodes().getFirst().tech());
        if (!Set.of("ARMOR", "INDUSTRY").contains(category)) return scaleLabels(referenceYears(category, layout), layout);
        boolean armor = layout.nodes().stream().anyMatch(n -> n.tech().category().equals("ARMOR"));
        var labels = new ArrayList<Label>();
        var years = armor ? ARMOR_YEARS : INDUSTRY_YEARS;
        for (int i = 0; i < years.size(); i++) {
            int y = 36 + (armor || i == 0 ? i : i + 1) * 44;
            labels.add(new Label(years.get(i) + "년", 4, y));
            labels.add(new Label((armor ? AMMUNITION_YEARS.get(i) : years.get(i)) + "년", armor ? 808 : 598, y));
        }
        return scaleLabels(labels, layout);
    }

    static List<Label> headings(ResearchLayout layout) {
        String category = ResearchLayout.category(layout.nodes().getFirst().tech());
        if (!Set.of("ARMOR", "INDUSTRY").contains(category)) return scaleLabels(referenceHeadings(category), layout);
        return scaleLabels(layout.nodes().stream().anyMatch(n -> n.tech().category().equals("ARMOR"))
                ? List.of(new Label("엔진", 80, 2), new Label("정찰전차", 202, 2), new Label("주력전차", 334, 2),
                    new Label("수륙양용", 422, 2), new Label("장갑", 570, 2), new Label("보조 무기 및 탄약", 722, 2))
                : List.of(new Label("생산", 100, 2), new Label("산업", 210, 46), new Label("건설", 298, 46), new Label("합성 석유", 518, 2)), layout);
    }

    static List<Connection> connections(ResearchLayout layout) {
        var nodes = new LinkedHashMap<String, ResearchLayout.Node>();
        layout.nodes().forEach(n -> nodes.put(n.tech().id(), n));
        var firstChildTop = new HashMap<String, Integer>();
        var firstChildLeft = new HashMap<String, Integer>();
        for (var child : layout.nodes()) for (var id : ResearchLayout.parents(child.tech())) {
            var parent = nodes.get(id);
            if (parent != null && child.x() > parent.x() + parent.width()) firstChildLeft.merge(id, child.x(), Math::min);
            if (parent != null && child.y() > parent.y() + parent.height()) firstChildTop.merge(id, child.y(), Math::min);
        }
        var result = new ArrayList<Connection>();
        for (var child : layout.nodes()) for (var id : ResearchLayout.parents(child.tech())) {
            var parent = nodes.get(id);
            if (parent == null) continue;
            int px = parent.x() + parent.width() / 2, py = parent.y() + parent.height() / 2;
            int cx = child.x() + child.width() / 2, cy = child.y() + child.height() / 2;
            var segments = new ArrayList<Segment>();
            if (!layout.vertical() && px != cx && py != cy) {
                int start = cx > px ? parent.x() + parent.width() : parent.x();
                int end = cx > px ? child.x() : child.x() + child.width();
                int bus = (start + (cx > px ? firstChildLeft.getOrDefault(id, end) : end)) / 2;
                if (child.tech().source().excludes().stream().map(nodes::get).anyMatch(other -> other != null && other.x() == child.x() && ResearchLayout.parents(other.tech()).contains(id))) bus = end - (int)Math.round(8 * scale(layout));
                segment(segments, start, py, bus, py);
                segment(segments, bus, py, bus, cy);
                segment(segments, bus, cy, end, cy);
            } else if (py == cy) {
                // Same-row modules connect directly to the side of their hull/ammunition node.
                segment(segments, px < cx ? parent.x() + parent.width() : parent.x(), py,
                        px < cx ? child.x() : child.x() + child.width(), cy);
            } else if (cy > py) {
                int start = parent.y() + parent.height(), end = child.y();
                int bus = (start + firstChildTop.getOrDefault(id, end)) / 2;
                if (child.tech().source() != null && child.tech().source().excludes().stream().map(nodes::get)
                        .anyMatch(other -> other != null && other.y() == child.y() && ResearchLayout.parents(other.tech()).contains(id)))
                    bus = end - (int)Math.round(8 * scale(layout));
                segment(segments, px, start, px, bus);
                segment(segments, px, bus, cx, bus);
                segment(segments, cx, bus, cx, end);
            } else {
                int start = parent.y(), end = child.y() + child.height(), bus = (start + end) / 2;
                segment(segments, px, start, px, bus);
                segment(segments, px, bus, cx, bus);
                segment(segments, cx, bus, cx, end);
            }
            if (segments.stream().anyMatch(line -> ResearchRoutes.blocked(line, layout.nodes())))
                segments = new ArrayList<>(ResearchRoutes.around(parent, child, layout));
            result.add(new Connection(parent.tech(), List.copyOf(segments)));
        }
        return List.copyOf(result);
    }

    private static void segment(List<Segment> segments, int x1, int y1, int x2, int y2) {
        if (x1 != x2 || y1 != y2) segments.add(new Segment(x1, y1, x2, y2));
    }

    static List<Choice> choices(ResearchLayout layout) {
        var nodes = new HashMap<String, ResearchLayout.Node>();
        layout.nodes().forEach(n -> nodes.put(n.tech().id(), n));
        var result = new ArrayList<Choice>();
        for (var node : layout.nodes()) if (node.tech().source() != null) for (var id : node.tech().source().excludes()) {
            var other = nodes.get(id);
            if (other == null || node.tech().id().compareTo(id) >= 0 || Set.of("energy_farms3", "nuclear_reactors3", "power_plants4").contains(node.tech().source().id())) continue;
            int nx = node.x() + node.width()/2, ny = node.y() + node.height()/2;
            int ox = other.x() + other.width()/2, oy = other.y() + other.height()/2;
            int gap = (int)Math.round(8 * scale(layout));
            if (layout.vertical() && ny == oy) result.add(new Choice((nx + ox)/2, Math.min(node.y(), other.y()) - gap, true));
            if (!layout.vertical() && nx == ox) result.add(new Choice(Math.min(node.x(), other.x()) - gap, (ny + oy)/2, false));
        }
        return List.copyOf(result);
    }

    static List<Group> groups(ResearchLayout layout) {
        var result = new ArrayList<Group>();
        addGroup(result, layout, "이 기술 중 하나만 연구할 수 있습니다.", Set.of("energy_farms3", "nuclear_reactors3", "power_plants4"));
        addGroup(result, layout, "", Set.of("modern_small_airframe", "modern_medium_airframe", "modern_large_airframe"));
        addGroup(result, layout, "", Set.of("early_ship_hull_nuclear_submarine", "advanced_ship_hull_nuclear_submarine"));
        addGroup(result, layout, "", Set.of("tech_field_hospital", "tech_field_hospital2", "tech_field_hospital3", "tech_field_hospital4", "tech_logistics_company", "tech_logistics_company2", "tech_logistics_company3", "tech_logistics_company4", "tech_signal_company", "tech_signal_company2", "tech_signal_company3", "tech_signal_company4"));
        addGroup(result, layout, "", Set.of("tech_signal_company", "tech_signal_company2", "tech_signal_company3", "tech_signal_company4"));
        return List.copyOf(result);
    }
    private static void addGroup(List<Group> groups, ResearchLayout layout, String caption, Set<String> ids) {
        var nodes=layout.nodes().stream().filter(n->n.tech().source()!=null&&ids.contains(n.tech().source().id())).toList();
        if(nodes.isEmpty()) return;
        int margin=(int)Math.round(8*scale(layout));
        int x=nodes.stream().mapToInt(ResearchLayout.Node::x).min().orElseThrow()-margin;
        int y=nodes.stream().mapToInt(ResearchLayout.Node::y).min().orElseThrow()-margin;
        int right=nodes.stream().mapToInt(n->n.x()+n.width()).max().orElseThrow()+margin;
        int bottom=nodes.stream().mapToInt(n->n.y()+n.height()).max().orElseThrow()+margin;
        groups.add(new Group(x,y,right-x,bottom-y,caption));
    }

    private static List<Label> referenceYears(String category, ResearchLayout layout) {
        int[] years, coordinates;
        switch (category) {
            case "INFANTRY" -> { years = new int[]{2000,2006,2012,2018,2021,2024,2027,2030,2033,2036}; coordinates = new int[]{0,210,420,630,840,1050,1260,1470,1680,1890}; }
            case "SUPPORT" -> { years = new int[]{2018,2020,2024,2028,2032}; coordinates = new int[]{0,210,490,910,1330}; }
            case "NAVY" -> { years = new int[]{2000,2020,2024,2028}; coordinates = new int[]{70,490,910,1330}; }
            case "NAVAL_SUPPORT" -> { years = new int[]{1980,2020,2028,2032}; coordinates = new int[]{0,420,840,1260}; }
            case "ARTILLERY" -> { years = new int[]{2000,2006,2012,2018,2021,2024,2027,2030,2033,2036}; coordinates = new int[]{0,140,280,420,560,700,840,980,1120,1260}; }
            case "AIR" -> { years = new int[]{2000,2020,2024,2028,2032,2040}; coordinates = new int[]{0,140,280,420,700,840}; }
            default -> { years = new int[]{2015,2019,2021,2023,2025,2027,2029,2031,2033,2035}; coordinates = new int[]{0,240,360,480,600,720,840,960,1080,1200}; }
        }
        var result = new ArrayList<Label>();
        for (int i=0;i<years.length;i++) {
            int coordinate = (int)Math.round(coordinates[i]*.4);
            result.add(new Label(years[i]+"년", layout.vertical()?4:50+coordinate, layout.vertical()?40+coordinate:4));
            if (category.equals("ARTILLERY")) result.add(new Label(years[i]+"년", 680, 40+coordinate));
            if (category.equals("AIR")) result.add(new Label((i==5?2036:years[i])+"년", 534, 40+coordinate));
        }
        return result;
    }

    private static List<Label> referenceHeadings(String category) {
        return switch (category) {
            case "INFANTRY" -> List.of(new Label("화기 & 장비",64,22),new Label("특수부대",64,448));
            case "SUPPORT" -> List.of(new Label("열차",64,426),new Label("다목적 / 공격 헬리콥터",160,608));
            case "ARTILLERY" -> List.of(new Label("자주대공포",64,74),new Label("대공포",176,74),new Label("자주포",260,74),new Label("야포",344,20),new Label("로켓포",428,74),new Label("대전차포",512,74),new Label("ATGM 탑재",624,74));
            case "NAVY" -> List.of(new Label("초계함",64,52),new Label("구축함",64,150),new Label("방어력",64,210),new Label("미사일 순양함",82,288),new Label("항공모함",64,380),new Label("잠수함",64,472));
            case "NAVAL_SUPPORT" -> List.of(new Label("무장",64,22),new Label("어뢰",64,238),new Label("피해 통제",64,354),new Label("사격 통제 방식",64,408),new Label("수송선",64,466),new Label("기뢰전",64,522));
            case "AIR" -> List.of(new Label("기체",166,20),new Label("엔진",668,20),new Label("폭장",820,20),new Label("공대공 미사일",900,74),new Label("공대지 미사일",1060,74));
            case "ENGINEERING" -> List.of(new Label("전자공학",150,12),new Label("전력 생산",382,12));
            default -> List.of();
        };
    }
}
