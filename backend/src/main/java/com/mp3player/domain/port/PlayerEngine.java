package com.mp3player.domain.port;

import java.io.IOException;
import java.util.Map;

/**
 * Port (contract) for the audio playback engine. Implementation owns the
 * low-level media decoding/threading (currently JLayer).
 */
public interface PlayerEngine {

    void play(String filePath) throws IOException;

    void play(String filePath, long startPositionMillis) throws IOException;

    void pause();

    void resume();

    void seekTo(long positionMillis);

    void stop();

    boolean isPlaying();

    boolean isPaused();

    String getCurrentFilePath();

    long getElapsedMillis();

    long getTotalMillis();

    /** ID3 tags of the currently playing track, as a wire map. */
    Map<String, String> getId3Tags();
}