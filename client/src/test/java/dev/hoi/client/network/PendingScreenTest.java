package dev.hoi.client.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PendingScreenTest {
    @Test void replacementRejectsOldTokensAndKeepsOnlyTheLatestTarget() {
        var pending = new PendingScreen();
        String old = pending.begin(); pending.target("old");
        String current = pending.begin(); pending.target("new");
        assertNotEquals(old, current); assertFalse(pending.matches(old));
        assertTrue(pending.matches(current)); assertEquals("new", pending.target());
        pending.clear(); assertFalse(pending.matches(current)); assertNull(pending.target());
    }
    @Test void expiresExactlyOnceAtFiveSecondsAndRejectsTheLateReply() {
        var pending = new PendingScreen(); String token = pending.begin();
        for (int tick = 1; tick < 100; tick++) { assertFalse(pending.tick()); assertTrue(pending.matches(token)); }
        assertTrue(pending.tick()); assertFalse(pending.matches(token)); assertFalse(pending.tick());
        assertTrue(pending.matches(pending.begin()));
    }
    @Test void closingOrDisconnectingCancelsBothTheReplyAndTheTimeoutNotice() {
        var pending = new PendingScreen(); String token = pending.begin();
        pending.clear();
        for (int tick = 0; tick < 101; tick++) assertFalse(pending.tick());
        assertFalse(pending.matches(token)); assertFalse(pending.waiting());
    }
}
