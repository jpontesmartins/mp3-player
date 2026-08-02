package com.mp3player.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class VirtualPlaylistService {

    private static final Logger log = LoggerFactory.getLogger(VirtualPlaylistService.class);

    private final Path baseDir;

    public VirtualPlaylistService() {
        this.baseDir = Paths.get(System.getProperty("user.home"), ".mp3-player", "playlists");
        init();
    }

    VirtualPlaylistService(Path baseDir) {
        this.baseDir = baseDir;
        init();
    }

    private void init() {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            log.error("Could not create playlists dir: {}", baseDir, e);
        }
    }

    public List<String> list() throws IOException {
        try (Stream<Path> stream = Files.list(baseDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                    .map(p -> p.getFileName().toString().replaceFirst("(?i)\\.txt$", ""))
                    .sorted()
                    .toList();
        }
    }

    public String sanitizeName(String name) {
        String cleaned = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return cleaned.isEmpty() ? "playlist" : cleaned;
    }

    public List<String> load(String name) throws IOException {
        Path file = fileFor(name);
        if (!Files.exists(file)) {
            throw new IOException("Playlist not found: " + name);
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return lines.stream().map(String::trim).filter(l -> !l.isEmpty()).toList();
    }

    public void save(String name, List<String> paths) throws IOException {
        Path file = fileFor(name);
        Files.write(file, paths, StandardCharsets.UTF_8);
        log.info("Playlist saved: {} ({} songs)", name, paths.size());
    }

    public void delete(String name) throws IOException {
        Path file = fileFor(name);
        if (!Files.exists(file)) {
            throw new IOException("Playlist not found: " + name);
        }
        Files.delete(file);
        log.info("Playlist deleted: {}", name);
    }

    public void rename(String oldName, String newName) throws IOException {
        Path oldFile = fileFor(oldName);
        if (!Files.exists(oldFile)) {
            throw new IOException("Playlist not found: " + oldName);
        }
        Files.move(oldFile, fileFor(newName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        log.info("Playlist renamed: {} -> {}", oldName, newName);
    }

    private Path fileFor(String name) {
        return baseDir.resolve(sanitizeName(name) + ".txt");
    }
}