package com.mp3player.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain aggregate for a virtual playlist. Each song is referenced by the
 * absolute physical path of the underlying file, ordered by the playlist order.
 */
public final class Playlist {

    private final String name;
    private final List<String> songPaths;

    public Playlist(String name, List<String> songPaths) {
        this.name = name == null ? "" : name.trim();
        this.songPaths = songPaths == null ? List.of() : List.copyOf(songPaths);
    }

    public String getName() {
        return name;
    }

    public List<String> getSongPaths() {
        return Collections.unmodifiableList(songPaths);
    }

    /** Returns a new playlist with the given song appended at the end, if not already present. */
    public Playlist addSong(String path) {
        Objects.requireNonNull(path);
        if (songPaths.contains(path)) {
            return this;
        }
        List<String> next = new ArrayList<>(songPaths);
        next.add(path);
        return new Playlist(name, next);
    }

    /** Returns a new playlist without the given song. */
    public Playlist removeSong(String path) {
        List<String> next = new ArrayList<>(songPaths);
        next.remove(path);
        return new Playlist(name, next);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Playlist other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}