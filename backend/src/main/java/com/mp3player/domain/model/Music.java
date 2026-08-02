package com.mp3player.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregate root representing a single audio file and its ID3 metadata.
 * The absolute file path acts as its unique identity.
 */
public final class Music {

    private final String path;
    private final Metadata metadata;

    public Music(String path, Metadata metadata) {
        this.path = path;
        this.metadata = metadata == null ? Metadata.empty() : metadata;
    }

    public String getPath() {
        return path;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Music other)) return false;
        return path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /** Flattens the metadata into the wire format expected by the frontend. */
    public Map<String, String> toTagMap() {
        Map<String, String> map = new LinkedHashMap<>();
        metadata.putIfNotBlank(map, "title", metadata.getTitle());
        metadata.putIfNotBlank(map, "artist", metadata.getArtist());
        metadata.putIfNotBlank(map, "album", metadata.getAlbum());
        metadata.putIfNotBlank(map, "year", metadata.getYear());
        metadata.putIfNotBlank(map, "genre", metadata.getGenre());
        metadata.putIfNotBlank(map, "track", metadata.getTrack());
        if (metadata.getDurationMs() != null) {
            map.put("duration_ms", String.valueOf(metadata.getDurationMs()));
        }
        return map;
    }

    /** Value object with the editable ID3 fields plus duration. */
    public static final class Metadata {
        private final String title;
        private final String artist;
        private final String album;
        private final String year;
        private final String genre;
        private final String track;
        private final Long durationMs;

        public Metadata(String title, String artist, String album, String year, String genre, String track, Long durationMs) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.year = year;
            this.genre = genre;
            this.track = track;
            this.durationMs = durationMs;
        }

        public static Metadata empty() {
            return new Metadata(null, null, null, null, null, null, null);
        }

        public static Metadata fromTags(Map<String, String> tags) {
            String raw = tags.getOrDefault("duration_ms", "");
            Long duration = null;
            if (!raw.isBlank()) {
                try {
                    duration = Long.parseLong(raw.trim());
                } catch (NumberFormatException ignored) {
                }
            }
            return new Metadata(
                    blankToNull(tags.get("title")),
                    blankToNull(tags.get("artist")),
                    blankToNull(tags.get("album")),
                    blankToNull(tags.get("year")),
                    blankToNull(tags.get("genre")),
                    blankToNull(tags.get("track")),
                    duration
            );
        }

        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getAlbum() { return album; }
        public String getYear() { return year; }
        public String getGenre() { return genre; }
        public String getTrack() { return track; }
        public Long getDurationMs() { return durationMs; }

        private void putIfNotBlank(Map<String, String> map, String key, String value) {
            if (value != null && !value.isBlank()) {
                map.put(key, value.trim());
            }
        }

        private static String blankToNull(String v) {
            return v == null || v.isBlank() ? null : v;
        }
    }
}