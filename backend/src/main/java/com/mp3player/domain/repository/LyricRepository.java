package com.mp3player.domain.repository;

import com.mp3player.domain.model.Lyric;
import com.mp3player.domain.model.Music;

import java.util.Optional;

/**
 * Repository for persisting song lyrics. Implementations decide where the
 * lyric text is stored (currently a TXT file next to the audio), so swapping to
 * a database later only requires a new implementation.
 */
public interface LyricRepository {

    /**
     * Loads the cached lyrics for the given music, if the lyrics were previously saved.
     */
    Optional<Lyric> find(String musicPath);

    /**
     * Persist/saves the lyrics. The repository uses the music metadata to choose
     * the storage location/filename.
     */
    void save(Lyric lyric, Music music);

    /** Whether a cached lyric file already exists for the music. */
    boolean exists(String musicPath);
}