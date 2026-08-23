package com.mp3player.metadata.infrastructure;

import com.mpatric.mp3agic.ID3v1Genres;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;
import com.mp3player.player.domain.model.Music;
import com.mp3player.metadata.domain.port.Id3Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Codec ID3 baseado em mp3agic. Lê/grava as tags ID3 editáveis e a duração.
 */
@Component
public class Id3MagicCodec implements Id3Codec {

    private static final Logger log = LoggerFactory.getLogger(Id3MagicCodec.class);

    /**
     * Lê as tags ID3 do arquivo no caminho informado e monta um agregado {@link Music}.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @return agregado de música com os metadados lidos
     */
    @Override
    public Music read(String filePath) {
        try {
            Mp3File mp3file = new Mp3File(filePath);
            String title = null, artist = null, album = null, year = null, genre = null, track = null, disc = null;

            if (mp3file.hasId3v2Tag()) {
                var id3 = mp3file.getId3v2Tag();
                title = id3.getTitle();
                artist = id3.getArtist();
                album = id3.getAlbum();
                year = id3.getYear();
                genre = id3.getGenreDescription();
                track = id3.getTrack();
                disc = id3.getPartOfSet();
            }
            if (mp3file.hasId3v1Tag()) {
                var id3 = mp3file.getId3v1Tag();
                title = pick(title, id3.getTitle());
                artist = pick(artist, id3.getArtist());
                album = pick(album, id3.getAlbum());
                year = pick(year, id3.getYear());
                genre = pick(genre, id3.getGenreDescription());
                track = pick(track, id3.getTrack());
            }
            long durationMs = mp3file.getLengthInMilliseconds();
            int bitrate = mp3file.getBitrate();
            Integer bitrateKbps = bitrate > 0 ? bitrate : null;

            Music.Metadata metadata = new Music.Metadata(
                    title, artist, album, year, genre, track, disc,
                    durationMs > 0 ? durationMs : null, bitrateKbps);

            Music music = new Music(filePath, metadata);
            if (music.toTagMap().isEmpty()) {
                return new Music(filePath, new Music.Metadata(fileName(filePath), null, null, null, null, null, null,
                        durationMs > 0 ? durationMs : null, bitrateKbps));
            }
            return music;
        } catch (Exception e) {
            log.warn("[Metadata] Não foi possível ler ID3 de {}", filePath, e);
            return new Music(filePath, Music.Metadata.empty());
        }
    }

    /**
     * Atualiza as tags editáveis do arquivo e retorna o {@link Music} resultante.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @param tags mapa de tags a serem atualizadas
     * @return agregado de música com os metadados atualizados
     * @throws IllegalStateException se ocorrer erro ao salvar o arquivo
     */
    @Override
    public Music update(String filePath, Map<String, String> tags) {
        try {
            Mp3File mp3file = new Mp3File(filePath);
            ID3v24Tag tag = new ID3v24Tag();
            tag.setTitle(tags.getOrDefault("title", null));
            tag.setArtist(tags.getOrDefault("artist", null));
            tag.setAlbum(tags.getOrDefault("album", null));
            tag.setYear(tags.getOrDefault("year", null));
            String genre = tags.getOrDefault("genre", null);
            if (genre != null && !genre.trim().isEmpty()) {
                int genreId = ID3v1Genres.matchGenreDescription(genre.trim());
                if (genreId >= 0) {
                    tag.setGenre(genreId);
                } else {
                    tag.setGenreDescription(genre.trim());
                }
            }
            tag.setTrack(tags.getOrDefault("track", null));
            tag.setPartOfSet(tags.getOrDefault("disc", null));
            mp3file.setId3v2Tag(tag);
            mp3file.removeId3v1Tag();

            Path tmp = Paths.get(filePath + ".mp3tmp");
            mp3file.save(tmp.toString());
            Files.move(tmp, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
            log.info("[Metadata] ID3 atualizado: {}", filePath);
            return read(filePath);
        } catch (Exception e) {
            throw new IllegalStateException("Error updating ID3 tags", e);
        }
    }

    /**
     * Retorna o valor primário se não for nulo ou em branco, senão o fallback.
     *
     * @param primary valor primário
     * @param fallback valor alternativo
     * @return valor primário ou fallback
     */
    private static String pick(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary.trim() : (fallback != null && !fallback.isBlank() ? fallback.trim() : null);
    }

    /**
     * Extrai o nome do arquivo a partir do caminho completo.
     *
     * @param path caminho completo do arquivo
     * @return nome do arquivo
     */
    private static String fileName(String path) {
        String filename = path.substring(path.lastIndexOf('\\') + 1);
        int lastSlash = filename.lastIndexOf('/');
        if (lastSlash >= 0) {
            filename = filename.substring(lastSlash + 1);
        }
        return filename;
    }
}