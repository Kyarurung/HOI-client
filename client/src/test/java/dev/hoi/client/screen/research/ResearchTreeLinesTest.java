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
