package com.ovelha.fy.player.domain.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raiz agregada representando um único arquivo de áudio e seus metadados ID3.
 */
@Data
public final class MusicFile {

    private final String path;
    private final Metadata metadata;

    /**
     * Construtor principal.
     *
     * @param path     caminho absoluto do arquivo de áudio
     * @param metadata metadados ID3 (se {@code null}, será criado vazio)
     */
    public MusicFile(String path, Metadata metadata) {
        this.path = path;
        this.metadata = metadata == null ? Metadata.empty() : metadata;
    }

    /**
     * Achata os metadados para o formato de troca (wire) esperado pelo frontend.
     *
     * @return mapa com os campos ID3 presentes, incluindo a duração em milissegundos.
     */
    public Map<String, String> toTagMap() {
        Map<String, String> map = new LinkedHashMap<>();
        metadata.putIfNotBlank(map, "title", metadata.title());
        metadata.putIfNotBlank(map, "artist", metadata.artist());
        metadata.putIfNotBlank(map, "album", metadata.album());
        metadata.putIfNotBlank(map, "year", metadata.year());
        metadata.putIfNotBlank(map, "genre", metadata.genre());
        metadata.putIfNotBlank(map, "track", metadata.track());
        metadata.putIfNotBlank(map, "disc", metadata.disc());
        if (metadata.durationMs() != null) {
            map.put("duration_ms", String.valueOf(metadata.durationMs()));
        }
        if (metadata.bitrateKbps() != null) {
            map.put("kbps", String.valueOf(metadata.bitrateKbps()));
        }
        return map;
    }

    /**
     * Value object com os campos ID3 editáveis além da duração.
     */
    public record Metadata(String title, String artist, String album, String year, String genre, String track,
                               String disc, Long durationMs, Integer bitrateKbps) {

        /**
         * Construtor com campos essenciais (disc e bitrate como {@code null}).
         *
         * @param title      título da música
         * @param artist     artista
         * @param album      álbum
         * @param year       ano de lançamento
         * @param genre      gênero
         * @param track      número da faixa
         * @param durationMs duração em milissegundos
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
        public Metadata { }

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

        /**
         * @return título da música
         */
        @Override
        public String title() {
            return title;
        }

        /**
         * @return artista
         */
        @Override
        public String artist() {
            return artist;
        }

        /**
         * @return álbum
         */
        @Override
        public String album() {
            return album;
        }

        /**
         * @return ano de lançamento
         */
        @Override
        public String year() {
            return year;
        }

        /**
         * @return gênero
         */
        @Override
        public String genre() {
            return genre;
        }

        /**
         * @return número da faixa
         */
        @Override
        public String track() {
            return track;
        }

        /**
         * @return número do disco
         */
        @Override
        public String disc() {
            return disc;
        }

        /**
         * @return duração em milissegundos
         */
        @Override
        public Long durationMs() {
            return durationMs;
        }

        /**
         * @return bitrate em kbps
         */
        @Override
        public Integer bitrateKbps() {
            return bitrateKbps;
        }

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