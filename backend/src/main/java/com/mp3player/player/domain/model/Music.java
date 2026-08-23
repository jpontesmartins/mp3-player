package com.mp3player.player.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raiz agregada representando um único arquivo de áudio e seus metadados ID3.
 * O caminho absoluto do arquivo atua como identidade única.
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

    /**
     * Achata os metadados para o formato de troca (wire) esperado pelo frontend.
     *
     * @return mapa com os campos ID3 presentes, incluindo a duração em milissegundos.
     */
    public Map<String, String> toTagMap() {
        Map<String, String> map = new LinkedHashMap<>();
        metadata.putIfNotBlank(map, "title", metadata.getTitle());
        metadata.putIfNotBlank(map, "artist", metadata.getArtist());
        metadata.putIfNotBlank(map, "album", metadata.getAlbum());
        metadata.putIfNotBlank(map, "year", metadata.getYear());
        metadata.putIfNotBlank(map, "genre", metadata.getGenre());
        metadata.putIfNotBlank(map, "track", metadata.getTrack());
        metadata.putIfNotBlank(map, "disc", metadata.getDisc());
        if (metadata.getDurationMs() != null) {
            map.put("duration_ms", String.valueOf(metadata.getDurationMs()));
        }
        if (metadata.getBitrateKbps() != null) {
            map.put("kbps", String.valueOf(metadata.getBitrateKbps()));
        }
        return map;
    }

    /**
     * Value object com os campos ID3 editáveis além da duração.
     */
    public static final class Metadata {
        private final String title;
        private final String artist;
        private final String album;
        private final String year;
        private final String genre;
        private final String track;
        private final String disc;
        private final Long durationMs;
        private final Integer bitrateKbps;

        public Metadata(String title, String artist, String album, String year, String genre, String track, Long durationMs) {
            this(title, artist, album, year, genre, track, null, durationMs, null);
        }

        public Metadata(String title, String artist, String album, String year, String genre, String track, String disc, Long durationMs, Integer bitrateKbps) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.year = year;
            this.genre = genre;
            this.track = track;
            this.disc = disc;
            this.durationMs = durationMs;
            this.bitrateKbps = bitrateKbps;
        }

        public static Metadata empty() {
            return new Metadata(null, null, null, null, null, null, null, null, null);
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
            String rawBitrate = tags.getOrDefault("kbps", "");
            Integer bitrate = null;
            if (!rawBitrate.isBlank()) {
                try {
                    bitrate = Integer.parseInt(rawBitrate.trim());
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
                    blankToNull(tags.get("disc")),
                    duration,
                    bitrate
            );
        }

        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getAlbum() { return album; }
        public String getYear() { return year; }
        public String getGenre() { return genre; }
        public String getTrack() { return track; }
        public String getDisc() { return disc; }
        public Long getDurationMs() { return durationMs; }
        public Integer getBitrateKbps() { return bitrateKbps; }

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