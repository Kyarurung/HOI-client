package dev.hoi.protocol.audio;

import dev.hoi.protocol.audio.AudioPayloads.*;
import dev.hoi.protocol.map.MapFocus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioStoryTest {
    @Test void storyMusicAndSoundEffectsAcceptOnlyTheirOwnEventNames() {
        assertEquals("music.story.one_minute_of_silence",new Music("music.story.one_minute_of_silence").event());
        assertEquals("sound.story.tfr_se_nato_victory_2",new Sound("sound.story.tfr_se_nato_victory_2").event());
        for(String invalid:new String[]{"music.story.x","sound.korea.x","sound.story.Bad","sound.story.","sound.story."+"a".repeat(120)})
            assertThrows(IllegalArgumentException.class,()->new Sound(invalid));
        assertThrows(IllegalArgumentException.class,()->new Music("sound.story.x"));
        assertNotEquals(Music.TYPE.id(),Sound.TYPE.id());
        assertEquals(1332,new MapFocus.State(1332,10,20,5,15,30,40).state());
        assertThrows(IllegalArgumentException.class,()->new MapFocus.State(0,10,20,5,15,30,40));
        assertThrows(IllegalArgumentException.class,()->new MapFocus.State(1332,4,20,5,15,30,40));
        assertThrows(IllegalArgumentException.class,()->new MapFocus.State(1332,10,41,5,15,30,40));
    }
}
