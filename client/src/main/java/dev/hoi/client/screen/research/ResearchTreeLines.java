package dev.hoi.client.screen.research;

import dev.hoi.client.screen.research.ResearchSourceLayout.ResearchSegment;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.research.ResearchView;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class ResearchTreeLines {
    record ResearchPath(int state, List<ResearchSegment> segments) {}
    record ResearchJoint(int x, int y, String kind, int state) {}
    static final int BLOCKED = 0, LOCKED = 1, AVAILABLE = 2, COMPLETED = 3, ACTIVE = 4;
    private static final int LEFT = 1, RIGHT = 2, UP = 4, DOWN = 8;
    private static final Map<Integer, String> KINDS = Map.of(RIGHT | DOWN, "top_left", LEFT | DOWN, "top_right", RIGHT | UP, "bottom_left",
            LEFT | UP, "bottom_right", LEFT | RIGHT | DOWN, "down", LEFT | RIGHT | UP, "up", LEFT | UP | DOWN, "left", RIGHT | UP | DOWN, "right",
            LEFT | RIGHT | UP | DOWN, "all");
    private ResearchTreeLines() {}
    public static int frame(long millis) { return (int)(millis / 83 % 12); }
    static Map<String, Integer> states(List<ResearchView.Tech> technologies) {
        var taken = new HashSet<String>();
        for (var t : technologies) if (t.status() == ResearchView.Status.COMPLETED || t.status() == ResearchView.Status.ACTIVE) taken.add(t.id());
        var result = new HashMap<String, Integer>();
        for (var t : technologies) result.put(t.id(), switch (t.status()) {
            case COMPLETED -> COMPLETED; case ACTIVE -> ACTIVE; case AVAILABLE -> AVAILABLE;
            case LOCKED -> t.source() != null && t.source().excludes().stream().anyMatch(taken::contains) ? BLOCKED : LOCKED;
        });
        return Map.copyOf(result);
    }
    private static boolean straight(ResearchSegment s) {
        return s.y1() == s.y2() ? s.x1() != s.x2() : s.x1() == s.x2();
    }
    static List<ResearchJoint> joints(List<ResearchPath> paths) {
        var joints = new LinkedHashMap<Long, int[]>();
        for (var path : paths) for (var s : path.segments()) if (straight(s)) {
            boolean horizontal = s.y1() == s.y2();
            int forward = horizontal ? s.x2() > s.x1() ? RIGHT : LEFT : s.y2() > s.y1() ? DOWN : UP;
            int backward = forward == RIGHT ? LEFT : forward == LEFT ? RIGHT : forward == DOWN ? UP : DOWN;
            mark(joints, s.x1(), s.y1(), forward, path.state());
            mark(joints, s.x2(), s.y2(), backward, path.state());
        }
        var result = new ArrayList<ResearchJoint>();
        joints.forEach((key, value) -> {
            int x = (int)(key >> 32), y = (int)(long)key;
            for (var path : paths) for (var s : path.segments()) if (straight(s) && through(s, x, y)) {
                value[0] |= s.y1() == s.y2() ? LEFT | RIGHT : UP | DOWN;
                value[1] = Math.max(value[1], path.state());
            }
            var kind = KINDS.get(value[0]);
            if (kind != null) result.add(new ResearchJoint(x, y, kind, value[1]));
        });
        return List.copyOf(result);
    }
    private static void mark(Map<Long, int[]> joints, int x, int y, int direction, int rank) {
        var value = joints.computeIfAbsent((long)x << 32 | (y & 0xFFFFFFFFL), k -> new int[]{0, 0});
        value[0] |= direction;
        value[1] = Math.max(value[1], rank);
    }
    private static boolean through(ResearchSegment s, int x, int y) {
        return s.y1() == s.y2()
                ? y == s.y1() && x > Math.min(s.x1(), s.x2()) && x < Math.max(s.x1(), s.x2())
                : x == s.x1() && y > Math.min(s.y1(), s.y2()) && y < Math.max(s.y1(), s.y2());
    }
    static void draw(GuiGraphicsExtractor g, List<ResearchPath> paths, double scale, long millis) {
        int thick = Math.max(2, (int)Math.round(16 * .4 * scale)), frame = frame(millis);
        double tile = 36 * .4 * scale;
        var ordered = paths.stream().sorted(Comparator.comparingInt(ResearchPath::state)).toList();
        for (var path : ordered) for (var s : path.segments()) if (straight(s)) strip(g, s, path.state(), thick, tile, frame);
        for (var joint : joints(ordered)) {
            String art = "research/line/center/" + joint.kind() + (joint.state() == ACTIVE ? "/researching/frame_" + frame : "/frame_" + joint.state());
            if (UiAssets.imageSize(art)[0] > 0) blit(g, art, joint.x() - thick / 2, joint.y() - thick / 2, thick, thick, 0, 1, 0, 1);
        }
    }
    private static void strip(GuiGraphicsExtractor g, ResearchSegment s, int state, int thick, double tile, int frame) {
        boolean horizontal = s.y1() == s.y2();
        int start = horizontal ? s.x1() : s.y1(), end = horizontal ? s.x2() : s.y2(), sign = end > start ? 1 : -1, length = Math.abs(end - start);
        String art = state == ACTIVE
                ? "research/line/" + (horizontal ? sign > 0 ? "right" : "left" : sign > 0 ? "down" : "up") + "/frame_" + frame
                : "research/line/" + (horizontal ? "horizontal" : "vertical") + "/frame_" + state;
        if (UiAssets.imageSize(art)[0] <= 0) return;
        int across = (horizontal ? s.y1() : s.x1()) - thick / 2;
        for (int i = 0; i * tile < length; i++) {
            boolean last = (i + 1) * tile >= length;
            int near = (int)Math.round(i * tile), far = last ? length : (int)Math.round((i + 1) * tile);
            float cut = last ? (float)((length - i * tile) / tile) : 1;
            int a = start + sign * near, b = start + sign * far;
            float t0 = sign > 0 ? 0 : 1 - cut, t1 = sign > 0 ? cut : 1;
            if (horizontal) blit(g, art, Math.min(a, b), across, Math.abs(b - a), thick, t0, t1, 0, 1);
            else blit(g, art, across, Math.min(a, b), thick, Math.abs(b - a), 0, 1, t0, t1);
        }
    }
    private static void blit(GuiGraphicsExtractor g, String art, int x, int y, int w, int h, float u0, float u1, float v0, float v1) {
        if (w > 0 && h > 0) g.blit(Identifier.parse("hoi:textures/gui/" + art + ".png"), x, y, x + w, y + h, u0, u1, v0, v1);
    }
}
