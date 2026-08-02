package com.mp3player.infrastructure.repository;

import com.mp3player.domain.model.Playlist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilePlaylistRepositoryTest {

    private Path dir;
    private FilePlaylistRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        dir = Files.createTempDirectory("playlists-test");
        repository = new FilePlaylistRepository(dir);
    }

    @Test
    void savePersistsTxtWithOnePathPerLine() throws IOException {
        repository.save(new Playlist("Rock", List.of("C:\\a.mp3", "C:\\b.mp3")));

        Path file = dir.resolve("Rock.txt");
        assertTrue(Files.exists(file));
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(List.of("C:\\a.mp3", "C:\\b.mp3"), lines);
    }

    @Test
    void loadReturnsOrderedPaths() {
        repository.save(new Playlist("Rock", List.of("first.mp3", "second.mp3")));
        assertEquals(List.of("first.mp3", "second.mp3"), repository.load("Rock"));
    }

    @Test
    void loadingMissingPlaylistReturnsEmpty() {
        assertTrue(repository.load("NaoExiste").isEmpty());
    }

    @Test
    void deleteRemovesTheFile() {
        repository.save(new Playlist("Temp", List.of("a.mp3")));
        repository.delete("Temp");
        assertTrue(repository.list().isEmpty());
    }

    @Test
    void renameMovesTheFile() {
        repository.save(new Playlist("Velho", List.of("a.mp3")));
        repository.rename("Velho", "Novo");
        assertTrue(repository.list().contains("Novo"));
        assertFalse(repository.list().contains("Velho"));
        assertEquals(List.of("a.mp3"), repository.load("Novo"));
    }

    @Test
    void SanitizesIllegalFilenameCharacters() {
        assertEquals("a_b", repository.sanitize("a/b"));
        assertEquals("rock", repository.sanitize("rock"));
    }
}