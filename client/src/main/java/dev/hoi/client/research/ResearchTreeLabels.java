package dev.hoi.client.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.ToIntFunction;


public final class ResearchTreeLabels {
    public record Label(String text, int x, int y, int width, int height, boolean year) {}
    private record Box(int x, int y, int width, int height) {
        boolean overlaps(Box other, int gap) {
            return x < other.x + other.width + gap && x + width + gap > other.x
                    && y < other.y + other.height + gap && y + height + gap > other.y;
        }
    }

    private ResearchTreeLabels() {}

    public static List<Label> create(ResearchLayout layout, ToIntFunction<String> textWidth, int lineHeight) {
        var occupied = new ArrayList<Box>();
        for (var node : layout.nodes()) occupied.add(new Box(node.x(), node.y(), node.width(), node.height()));
        var result = new ArrayList<Label>();
        place(TfrResearchLayout.yearLabels(layout), true, textWidth, lineHeight, occupied, result, 4);
        place(centeredHeadings(layout), false, textWidth, lineHeight, occupied, result,
                layout.nodes().isEmpty() || java.util.Set.of("ENGINEERING", "INDUSTRY").contains(layout.nodes().getFirst().tech().category()) ? 4 : 2);
        return List.copyOf(result);
    }

    private static List<TfrResearchLayout.Label> centeredHeadings(ResearchLayout layout) {
        var headings = TfrResearchLayout.headings(layout);
        if (layout.nodes().isEmpty() || java.util.Set.of("ENGINEERING", "INDUSTRY")
                .contains(layout.nodes().getFirst().tech().category())) return headings;
        double reach = 90 * TfrResearchLayout.scale(layout);
        return headings.stream().map(label -> layout.nodes().stream()
                .filter(node -> node.y() >= label.y() && node.y() - label.y() <= reach
                        && label.x() >= node.x() && label.x() <= node.x() + node.width())
                .min(Comparator.<ResearchLayout.Node>comparingDouble(node ->
                        Math.abs(node.x() + node.width() / 2 - label.x()) + node.y() - label.y()))
                .map(node -> new TfrResearchLayout.Label(label.text(), node.x() + node.width() / 2, label.y()))
                .orElse(label)).toList();
    }

    private static void place(List<TfrResearchLayout.Label> anchors, boolean year, ToIntFunction<String> textWidth,
                              int lineHeight, List<Box> occupied, List<Label> result, int gap) {
        for (var anchor : anchors) {
            int width = textWidth.applyAsInt(anchor.text());
            int x = Math.max(2, anchor.x() - (year ? 0 : width / 2));
            var candidates = new TreeSet<Integer>(Comparator.<Integer>comparingInt(y -> Math.abs(y - anchor.y()))
                    .thenComparingInt(Integer::intValue));
            candidates.add(Math.max(2, anchor.y()));
            candidates.add(2);
            for (var box : occupied) {
                if (box.y() - lineHeight - gap >= 2) candidates.add(box.y() - lineHeight - gap);
                candidates.add(box.y() + box.height() + gap);
            }

            int y = candidates.stream().filter(top -> year || top <= anchor.y()).filter(top -> occupied.stream()
                    .noneMatch(box -> box.overlaps(new Box(x, top, width, lineHeight), gap))).findFirst().orElseThrow();
            occupied.add(new Box(x, y, width, lineHeight));
            result.add(new Label(anchor.text(), x, y, width, lineHeight, year));
        }
    }
}
