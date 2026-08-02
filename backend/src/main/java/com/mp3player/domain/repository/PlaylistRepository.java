package com.mp3player.domain.repository;

import com.mp3player.domain.model.Playlist;

import java.util.List;

/**
 * Repository port for persisting and loading playlists. Current implementation
 * stores each playlist as a TXT file of absolute paths, but an implementation
 * could later back it with a database without changing the domain.
 */
public interface PlaylistRepository {

    /** Names of all saved playlists. */
    List<String> list();

    /** Loads the ordered song paths of a playlist. */
    List<String> load(String name);

    /** Creates or overwrites a playlist. */
    void save(Playlist playlist);

    /** Deletes a playlist. */
    void delete(String name);

    /** Renames an existing playlist. */
    void rename(String currentName, String newName);
}