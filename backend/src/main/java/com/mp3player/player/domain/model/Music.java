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

    /**
     * Construtor principal.
     *
     * @param path     caminho absoluto do arquivo de áudio
     * @param metadata metadados ID3 (se {@code null}, será criado vazio)
     */
    public Music(String path, Metadata metadata) {
        this.path = path;
        this.metadata = metadata == null ? Metadata.empty() : metadata;
    }

    /**
     * Retorna o caminho absoluto do arquivo de áudio.
     *
     * @return caminho do arquivo
     */
    public String getPath() {
        return path;
    }

    /**
     * Retorna os metadados ID3 associados à música.
     *
     * @return metadados da música
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Duas músicas são iguais se possuírem o mesmo caminho absoluto.
     *
     * @param o objeto a ser comparado
     * @return {@code true} se forem iguais
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Music other)) return false;
        return path.equals(other.path);
    }

    /**
     * Hash code baseado no caminho do arquivo.
     *
     * @return hash code
     */
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

        /**
         * Construtor com campos essenciais (disc e bitrate como {@code null}).
         *
         * @param title       título da música
         * @param artist      artista
         * @param album       álbum
         * @param year        ano de lançamento
         * @param genre       gênero
         * @param track       número da faixa
         * @param durationMs  duração em milissegundos
         */
        public Metadata(String title, String artist, String album, String year, String genre, String track, Long durationMs) {
            this(title, artist, album, year, genre, track, null, durationMs, null);
        }

        /**
         * Construtor completo com todos os campos de metadados.
         *
         * @param title       título da música
         * @param artist      artista
         * @param album       álbum
         * @param year        ano de lançamento
         * @param genre       gênero
         * @param track       número da faixa
         * @param disc        número do disco
         * @param durationMs  duração em milissegundos
         * @param bitrateKbps bitrate em kbps
         */
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

        /**
         * Cria instância de metadados vazios.
         *
         * @return instância com todos os campos {@code null}
         */
        public static Metadata empty() {
            return new Metadata(null, null, null, null, null, null, null, null, null);
        }

        /**
         * Constrói metadados a partir de um mapa de tags ID3 (wire format).
         *
         * @param tags mapa de tags no formato de troca
         * @return instância de {@link Metadata} com os campos preenchidos
         */
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

        /** @return título da música */
        public String getTitle() { return title; }
        /** @return artista */
        public String getArtist() { return artist; }
        /** @return álbum */
        public String getAlbum() { return album; }
        /** @return ano de lançamento */
        public String getYear() { return year; }
        /** @return gênero */
        public String getGenre() { return genre; }
        /** @return número da faixa */
        public String getTrack() { return track; }
        /** @return número do disco */
        public String getDisc() { return disc; }
        /** @return duração em milissegundos */
        public Long getDurationMs() { return durationMs; }
        /** @return bitrate em kbps */
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