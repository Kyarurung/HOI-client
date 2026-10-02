package dev.hoi.client.ui;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiFlagStackTest {
    @Test void retainedFlagsKeepTheirOrderAndNewFlagsFollowIncomingOrder() {
        var stack = new UiFlagStack();
        var original = stack.update(List.of("KOR", "JAP", "CHI"));
        assertEquals(List.of("KOR", "JAP", "CHI"), original);
        assertEquals(List.of("KOR", "CHI", "AST", "NZL"), stack.update(List.of("AST", "CHI", "KOR", "AST", "NZL")));
        assertEquals(List.of("CHI", "NZL", "JAP"), stack.update(List.of("JAP", "NZL", "CHI")));
        assertEquals(List.of("KOR", "JAP", "CHI"), original);
        assertThrows(UnsupportedOperationException.class, () -> original.add("AST"));
        assertEquals(List.of(), stack.update(List.of()));
        assertEquals(List.of("JAP", "KOR"), stack.update(List.of("JAP", "KOR", "JAP")));
    }
    @Test void oneFlagAndCrowdedFlagsStayWithinTheAllocatedWidth() {
        assertEquals(10, UiFlagStack.left(0, 1, 10, 40, 20));
        assertEquals(20, UiFlagStack.visibleWidth(0, 1, 10, 40, 20));
        assertEquals(10, UiFlagStack.left(0, 4, 10, 40, 20));
        assertEquals(30, UiFlagStack.left(3, 4, 10, 40, 20));
        assertEquals(10, UiFlagStack.left(2, 4, 10, 8, 20));
    }
}
