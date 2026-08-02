package com.mp3player.domain.port;

import java.io.IOException;

/**
 * Port for fetching lyrics text from a web source.
 */
public interface LyricsScraper {

    /**
     * Fetches the lyrics for the given artist/title.
     *
     * @return the lyrics text, or a human-readable "not found" message when absent.
     */
    String fetch(String artist, String title) throws IOException;
}