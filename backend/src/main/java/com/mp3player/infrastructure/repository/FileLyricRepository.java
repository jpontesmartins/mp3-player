package com.mp3player.infrastructure.repository;

import com.mp3player.domain.model.Lyric;
import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import com.mp3player.domain.repository.LyricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Implementação baseada em arquivos de {@link LyricRepository}. As letras são
 * armazenadas como um arquivo TXT na pasta do álbum (pai) da música, nomeado
 * "{artista} - {título}.txt" usando as tags ID3, com fallback para o nome do
 * arquivo quando as tags estão ausentes.
 */
@Repository
public class FileLyricRepository implements LyricRepository {

    private static final Logger log = LoggerFactory.getLogger(FileLyricRepository.class);

    private final Id3Codec id3Codec;

    public FileLyricRepository(Id3Codec id3Codec) {
        this.id3Codec = id3Codec;
    }

    @Override
    public Optional<Lyric> find(String musicPath) {
        Path file = resolveTxtFile(musicPath);
        if (file == null || !Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Lyric(musicPath, Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            log.error("Error reading lyrics for {}", musicPath, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String musicPath) {
        Path file = resolveTxtFile(musicPath);
        return file != null && Files.exists(file);
    }

    @Override
    public void save(Lyric lyric, Music music) {
        Path file = resolveTxtFile(lyric.getMusicPath());
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, lyric.getText(), StandardCharsets.UTF_8);
            log.info("Lyrics saved to {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.error("Error saving lyrics for {}", lyric.getMusicPath(), e);
        }
    }

    /** Resolve o arquivo TXT de letra para o áudio informado, na pasta do álbum. */
    private Path resolveTxtFile(String musicPath) {
        Path parent = Paths.get(musicPath).getParent();
        if (parent == null) return null;
        String fileName = resolveTxTFileName(musicPath);
        return parent.resolve(fileName);
    }

    /**
     * Nome do arquivo de letra: usa as tags ID3 (artista, título) se presentes;
     * senão infere do nome do arquivo MP3 ("Artista - Música" ou "Música").
     */
    private String resolveTxTFileName(String musicPath) {
        String id3Artist = id3Tag(musicPath, true);
        String id3Title = id3Tag(musicPath, false);

        if (!id3Artist.isBlank() && !id3Title.isBlank()) {
            return sanitizeFileName(id3Artist + " - " + id3Title) + ".txt";
        }

        String base = baseName(musicPath);
        int dash = base.indexOf(" - ");
        if (dash > 0 && id3Artist.isBlank() && id3Title.isBlank()) {
            return sanitizeFileName(base) + ".txt";
        }

        String artist = id3Artist.isBlank() ? "" : id3Artist;
        String title = id3Title.isBlank() ? base : id3Title;
        return fileNameFor(artist, title);
    }

    private String id3Tag(String musicPath, boolean artist) {
        try {
            Music music = id3Codec.read(musicPath);
            String value = artist ? music.getMetadata().getArtist() : music.getMetadata().getTitle();
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String fileNameFor(String artist, String title) {
        String prefix = artist == null || artist.isBlank() ? "" : artist.trim() + " - ";
        return sanitizeFileName(prefix + (title == null ? "" : title.trim())) + ".txt";
    }

    private static String sanitizeFileName(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String baseName(String filePath) {
        String name = Paths.get(filePath).getFileName().toString();
        if (name.toLowerCase().endsWith(".mp3")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }
}