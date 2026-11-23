package dev.hoi.client.screen.research;

import dev.hoi.client.screen.research.ResearchSourceLayout.ResearchSegment;
import dev.hoi.client.screen.research.ResearchTreeLines.ResearchJoint;
import dev.hoi.client.screen.research.ResearchTreeLines.ResearchPath;
import dev.hoi.protocol.research.ResearchView;
import dev.hoi.protocol.research.ResearchView.Status;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.hoi.client.screen.research.ResearchTreeLines.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchTreeLinesTest {
    private static ResearchPath path(int state, int... points) {
        var segments = new ArrayList<ResearchSegment>();
        for (int i = 0; i + 3 < points.length; i += 2) segments.add(new ResearchSegment(points[i], points[i + 1], points[i + 2], points[i + 3]));
        return new ResearchPath(state, segments);
    }
    private static ResearchView.Tech tech(String id, Status status, String... excludes) {
        var source = new ResearchView.Source(id, "infantry_folder", 0, 0, false, List.of(), List.of(excludes), List.of(), List.of(), "", List.of(), Map.of(), "");
        return new ResearchView.Tech(id, "INFANTRY", id, 2000, 1, 100, 0, 1, List.of(), List.of(), List.of(), status, source);
    }
    @Test void bendsUseTheOriginalCornerPiecesInTheChildState() {
        var joints = ResearchTreeLines.joints(List.of(path(AVAILABLE, 0, 0, 10, 0, 10, 20, 30, 20)));
        assertEquals(List.of(new ResearchJoint(10, 0, "top_right", AVAILABLE), new ResearchJoint(10, 20, "bottom_left", AVAILABLE)), joints);
        var upward = ResearchTreeLines.joints(List.of(path(ACTIVE, 0, 20, 10, 20, 10, 0, 30, 0)));
        assertEquals(List.of(new ResearchJoint(10, 20, "bottom_right", ACTIVE), new ResearchJoint(10, 0, "top_left", ACTIVE)), upward);
    }
    @Test void sharedTrunksAndPassingLinesBecomeJunctionPieces() {
        var split = ResearchTreeLines.joints(List.of(path(LOCKED, 0, 0, 10, 0, 10, 20, 30, 20), path(COMPLETED, 0, 0, 10, 0, 10, -20, 30, -20)));
        assertTrue(split.contains(new ResearchJoint(10, 0, "left", COMPLETED)), split.toString());
        var branch = ResearchTreeLines.joints(List.of(path(COMPLETED, 0, 0, 30, 0), path(BLOCKED, 0, 0, 10, 0, 10, 20, 30, 20)));
        assertTrue(branch.contains(new ResearchJoint(10, 0, "down", COMPLETED)), branch.toString());
        assertTrue(branch.contains(new ResearchJoint(10, 20, "bottom_left", BLOCKED)), branch.toString());
        var cross = ResearchTreeLines.joints(List.of(path(LOCKED, 0, 0, 10, 0, 30, 0), path(AVAILABLE, 10, -10, 10, 0, 10, 10)));
        assertTrue(cross.contains(new ResearchJoint(10, 0, "all", AVAILABLE)), cross.toString());
        assertTrue(ResearchTreeLines.joints(List.of(path(LOCKED, 0, 0, 30, 0), path(AVAILABLE, 10, -10, 10, 10))).isEmpty());
    }
    @Test void lineEdgesKeepBothShadowAndBorderRowsAtEveryThickness() {
        for (int size = 1; size <= 64; size++) {
            var rows = new ArrayList<Integer>();int next = 0;
            for (var chunk : ResearchTreeLines.cross(size)) {
                assertEquals(next, chunk[0], "rows must be contiguous at " + size);next = chunk[0] + chunk[1];
                assertTrue(chunk[3] == chunk[1] || chunk[3] == 1, "a chunk is drawn one texel per pixel or stretches a single texel at " + size);
                assertTrue(chunk[2] >= 0 && chunk[2] + chunk[3] <= 16, "chunk stays inside the 16 texel cross-section at " + size);
                for (int i = 0; i < chunk[1]; i++) rows.add(chunk[3] == 1 ? chunk[2] : chunk[2] + i);
            }
            assertEquals(size, next, "chunks fill the line thickness at " + size);
            assertEquals(Arrays.stream(ResearchTreeLines.texels(size)).boxed().toList(), rows, "chunks draw the planned texel of every row at " + size);
            for (int i = 1; i < rows.size(); i++) assertTrue(rows.get(i) >= rows.get(i - 1), "rows keep the source order at " + size);
            if (size >= 8) {
                assertTrue(rows.getFirst() == 0 && rows.getLast() == 15 && rows.contains(1) && rows.contains(14), "both shadow rows stay at " + size);
                assertTrue(rows.stream().anyMatch(t -> t == 2 || t == 3) && rows.stream().anyMatch(t -> t == 12 || t == 13), "both grey border rows stay at " + size);
                assertEquals(rows.stream().filter(t -> t <= 3).count(), rows.stream().filter(t -> t >= 12).count(), "edges stay symmetric at " + size);
            }
        }
        assertEquals(List.of(0, 1, 2, 4, 5, 7, 8, 10, 11, 13, 14, 15), Arrays.stream(ResearchTreeLines.texels(12)).boxed().toList());
        assertEquals(10, Arrays.stream(ResearchTreeLines.texels(20)).filter(t -> t >= 4 && t <= 11).count(), "the 8 texel colour band keeps its half of a 20 px line");
    }
    @Test void straightRunsAndLineEndsNeedNoCentrePiece() {
        assertTrue(ResearchTreeLines.joints(List.of(path(COMPLETED, 0, 0, 10, 0, 30, 0), path(LOCKED, 5, 5, 5, 5))).isEmpty());
    }
    @Test void fiveStatesFollowTheOriginalFramesAndExclusiveResearchBlocksTheRest() {
        var states = ResearchTreeLines.states(List.of(tech("done", Status.COMPLETED), tech("running", Status.ACTIVE), tech("open", Status.AVAILABLE),
                tech("next", Status.LOCKED, "open"), tech("after_done", Status.LOCKED, "done"), tech("beside_running", Status.LOCKED, "running")));
        assertEquals(Map.of("done", COMPLETED, "running", ACTIVE, "open", AVAILABLE, "next", LOCKED, "after_done", BLOCKED, "beside_running", BLOCKED), states);
        assertEquals(List.of(0, 1, 2, 3, 4), List.of(BLOCKED, LOCKED, AVAILABLE, COMPLETED, ACTIVE));
        assertEquals(0, ResearchTreeLines.frame(0));
        assertEquals(1, ResearchTreeLines.frame(83));
        assertEquals(0, ResearchTreeLines.frame(83 * 12));
    }
}
