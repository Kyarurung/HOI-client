package dev.hoi.client.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.ToIntFunction;


public final class ResearchTreeLabels {
    public record Label(String text, int x, int y, int width, int height, boolean year) {}
    private record Box(int x, int y, int width, int height) {
        boolean overlaps(Box other) {
            return x < other.x + other.width + 4 && x + width + 4 > other.x
                    && y < other.y + other.height + 4 && y + height + 4 > other.y;
        }
    }

    private ResearchTreeLabels() {}

    public static List<Label> create(ResearchLayout layout, ToIntFunction<String> textWidth, int lineHeight) {
        var occupied = new ArrayList<Box>();
        for (var node : layout.nodes()) occupied.add(new Box(node.x(), node.y(), node.width(), node.height()));
        var result = new ArrayList<Label>();
        place(TfrResearchLayout.yearLabels(layout), true, textWidth, lineHeight, occupied, result);
        place(TfrResearchLayout.headings(layout), false, textWidth, lineHeight, occupied, result);
        return List.copyOf(result);
    }

    private static void place(List<TfrResearchLayout.Label> anchors, boolean year, ToIntFunction<String> textWidth,
                              int lineHeight, List<Box> occupied, List<Label> result) {
        for (var anchor : anchors) {
            int width = textWidth.applyAsInt(anchor.text());
            int x = Math.max(2, anchor.x() - (year ? 0 : width / 2));
            var candidates = new TreeSet<Integer>(Comparator.<Integer>comparingInt(y -> Math.abs(y - anchor.y()))
                    .thenComparingInt(Integer::intValue));
            candidates.add(Math.max(2, anchor.y()));
            candidates.add(2);
            for (var box : occupied) {
                if (box.y() - lineHeight - 4 >= 2) candidates.add(box.y() - lineHeight - 4);
                candidates.add(box.y() + box.height() + 4);
            }

            int y = candidates.stream().filter(top -> year || top <= anchor.y()).filter(top -> occupied.stream()
                    .noneMatch(box -> box.overlaps(new Box(x, top, width, lineHeight)))).findFirst().orElseThrow();
            occupied.add(new Box(x, y, width, lineHeight));
            result.add(new Label(anchor.text(), x, y, width, lineHeight, year));
        }
    }
}
