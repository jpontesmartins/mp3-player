package com.mp3player.infrastructure.repository;

import com.mp3player.domain.model.Lyric;
import com.mp3player.domain.model.Music;
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
 * File-based implementation of {@link LyricRepository}. Lyrics are stored as a
 * TXT file next to the song, named after its artist/title.
 */
@Repository
public class FileLyricRepository implements LyricRepository {

    private static final Logger log = LoggerFactory.getLogger(FileLyricRepository.class);

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
            Files.writeString(file, lyric.getText(), StandardCharsets.UTF_8);
            log.info("Lyrics saved to {}", file.getFileName());
        } catch (IOException e) {
            log.error("Error saving lyrics for {}", lyric.getMusicPath(), e);
        }
    }

    /** Resolves the lyrics TXT file for the given audio, based on its filename. */
    private Path resolveTxtFile(String musicPath) {
        String artist = extractArtist(musicPath);
        String title = extractTitle(musicPath);

        Path parent = Paths.get(musicPath).getParent();
        if (parent == null) return null;

        String fileName = (artist.isEmpty() ? "" : artist + " - ") + title + ".txt";
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return parent.resolve(fileName);
    }

    private String extractArtist(String filePath) {
        String name = baseName(filePath);
        int dash = name.indexOf(" - ");
        return dash > 0 ? sanitize(name.substring(0, dash)) : "";
    }

    private String extractTitle(String filePath) {
        String name = baseName(filePath);
        int dash = name.indexOf(" - ");
        return dash > 0 ? sanitize(name.substring(dash + 3)) : sanitize(name);
    }

    private String baseName(String filePath) {
        String name = Paths.get(filePath).getFileName().toString();
        if (name.toLowerCase().endsWith(".mp3")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.trim();
    }
}