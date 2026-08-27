package com.mp3player.playlist.infrastructure;

import com.mp3player.playlist.domain.model.Playlist;
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
        assertEquals("a_b", repository.sanitize("a\\b"));
        assertEquals("a_b", repository.sanitize("a:b"));
        assertEquals("a_b", repository.sanitize("a*b"));
        assertEquals("a_b", repository.sanitize("a?b"));
        assertEquals("a_b", repository.sanitize("a\"b"));
        assertEquals("a_b", repository.sanitize("a<b"));
        assertEquals("a_b", repository.sanitize("a>b"));
        assertEquals("a_b", repository.sanitize("a|b"));
    }

    @Test
    void sanitizeFallsBackToPlaylistForBlankName() {
        assertEquals("playlist", repository.sanitize("   "));
    }

    @Test
    void listReturnsEmptyWhenDirectoryIsUnavailable() throws IOException {
        Files.delete(dir);

        assertTrue(repository.list().isEmpty());
    }

    @Test
    void saveThrowsIllegalStateExceptionWhenDirectoryIsUnavailable() throws IOException {
        Files.delete(dir);

        assertThrows(IllegalStateException.class,
                () -> repository.save(new Playlist("Rock", List.of("a.mp3"))));
    }

    @Test
    void renameThrowsIllegalArgumentExceptionWhenSourceMissing() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> repository.rename("NaoExiste", "Novo"));
        assertTrue(e.getMessage().contains("NaoExiste"));
    }

    @Test
    void listReturnsSortedNames() {
        repository.save(new Playlist("Zebra", List.of("z.mp3")));
        repository.save(new Playlist("Alpha", List.of("a.mp3")));

        List<String> names = repository.list();

        assertEquals(2, names.size());
        assertEquals("Alpha", names.get(0));
        assertEquals("Zebra", names.get(1));
    }

    @Test
    void saveThenLoadReturnsSamePaths() {
        repository.save(new Playlist("MyPlaylist", List.of("a.mp3", "b.mp3", "c.mp3")));

        assertEquals(List.of("a.mp3", "b.mp3", "c.mp3"), repository.load("MyPlaylist"));
    }

    @Test
    void loadSkipsBlankLines() throws IOException {
        Path file = dir.resolve("Mixed.txt");
        Files.write(file, List.of("a.mp3", "", "  ", "b.mp3"), StandardCharsets.UTF_8);

        assertEquals(List.of("a.mp3", "b.mp3"), repository.load("Mixed"));
    }

    @Test
    void renameOverwritesTargetIfExists() {
        repository.save(new Playlist("Source", List.of("source.mp3")));
        repository.save(new Playlist("Target", List.of("target.mp3")));

        repository.rename("Source", "Target");

        assertTrue(repository.list().contains("Target"));
        assertFalse(repository.list().contains("Source"));
        assertEquals(List.of("source.mp3"), repository.load("Target"));
    }
}