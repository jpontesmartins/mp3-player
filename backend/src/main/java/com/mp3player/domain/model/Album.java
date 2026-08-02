package com.mp3player.domain.model;

/** Value object identifying an album by its name and artist. */
public final class Album {

    private final Artist artist;
    private final String name;

    public Album(Artist artist, String name) {
        this.artist = artist;
        this.name = name == null ? "" : name.trim();
    }

    public Artist getArtist() {
        return artist;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Album other)) return false;
        return name.equalsIgnoreCase(other.name)
                && (artist == null ? other.artist == null : artist.equals(other.artist));
    }

    @Override
    public int hashCode() {
        return 31 * name.toLowerCase().hashCode() + (artist == null ? 0 : artist.hashCode());
    }
}