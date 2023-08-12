package top.nextnet.greekmythcoding.cmd;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class EpisodeCommandTest {

    @Test
    void getNextEpisodeSuggestion() {
        assertEquals(4,EpisodeCommand.getNextEpisodeSuggestion(Arrays.asList(1,2,3,5)));
        assertEquals(1,EpisodeCommand.getNextEpisodeSuggestion(Arrays.asList(2,3,5)));
        assertEquals(2,EpisodeCommand.getNextEpisodeSuggestion(Arrays.asList(1,3,4)));

    }
}