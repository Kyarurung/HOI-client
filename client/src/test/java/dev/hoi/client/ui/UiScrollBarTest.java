package dev.hoi.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiScrollBarTest {
    @Test void overflowStartsAtOnePixelAndThumbKeepsOriginalSize() {
        var fit = new UiScrollBar(100, 20, 220, 184, 184);
        assertEquals(0, fit.maximum());
        assertFalse(fit.contains(104, 100));
        var overflow = new UiScrollBar(100, 20, 220, 184, 185);
        assertEquals(1, overflow.maximum());
        assertTrue(overflow.contains(104, 100));
        assertEquals(8, overflow.thumbHeight());
        assertEquals(8, new UiScrollBar(100, 20, 720, 684, 10000).thumbHeight());
    }
    @Test void draggingReachesBothEndsAndClampsOutsideTrack() {
        var bar = new UiScrollBar(100, 20, 220, 184, 1000);
        assertEquals(0, bar.offset(-100));
        assertEquals(816, bar.offset(500));
        assertEquals(0, bar.offset(28 + bar.thumbHeight() / 2.0));
        assertEquals(816, bar.offset(212 - bar.thumbHeight() / 2.0));
        assertTrue(bar.contains(108, 219));
        assertFalse(bar.contains(109, 219));
        assertFalse(bar.contains(108, 220));
    }
    @Test void arrowClicksDoNotOvershootOrMoveAnUnscrollableList() {
        var bar = new UiScrollBar(100, 20, 220, 184, 200);
        assertEquals(0, bar.step(21, 8, 49));
        assertEquals(16, bar.step(219, 8, 49));
        var empty = new UiScrollBar(100, 20, 220, 184, 0);
        assertEquals(0, empty.offset(200));
        assertEquals(0, empty.step(219, 0, 49));
    }
}
