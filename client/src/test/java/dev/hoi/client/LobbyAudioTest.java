package dev.hoi.client;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LobbyAudioTest {
    private static final class Output implements LobbyAudio.Output {
        final List<String> calls = new ArrayList<>();
        public void stopTheme() { calls.add("stop"); }
        public void playTheme() { calls.add("theme"); }
        public void playStart() { calls.add("start"); }
    }
    @Test void startStopsPlayingThemeBeforeLoadingSoundAndNeverRestartsIt() {
        var out=new Output();var lobby=new LobbyAudio(out);
        lobby.select();lobby.tick(true,false);lobby.select();lobby.tick(true,true);
        assertEquals(List.of("theme"),out.calls);
        lobby.start();lobby.tick(true,false);
        assertEquals(List.of("theme","stop","start"),out.calls);
        assertFalse(lobby.requested());
    }
    @Test void startCancelsThemeWaitingForResourcePackAndDisconnectCancelsItToo() {
        var out=new Output();var lobby=new LobbyAudio(out);
        lobby.select();lobby.tick(false,false);assertTrue(out.calls.isEmpty());
        lobby.start();lobby.tick(true,false);assertEquals(List.of("stop","start"),out.calls);
        out.calls.clear();lobby.select();lobby.reset();lobby.tick(true,false);
        assertEquals(List.of("stop"),out.calls);
    }
    @Test void reloadingResourcesRecoversOnlyWhileStillInLobby() {
        var out=new Output();var lobby=new LobbyAudio(out);
        lobby.select();lobby.tick(false,false);lobby.tick(true,false);lobby.tick(true,true);
        lobby.tick(false,false);lobby.tick(true,false);
        assertEquals(List.of("theme","theme"),out.calls);
    }
}
