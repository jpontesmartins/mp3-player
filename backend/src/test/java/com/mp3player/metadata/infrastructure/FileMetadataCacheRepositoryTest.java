package com.mp3player.metadata.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileMetadataCacheRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void putAndGetRoundTrip() {
        FileMetadataCacheRepository repo = new FileMetadataCacheRepository(cacheFile());
        assertNull(repo.get("a.mp3"));

        repo.put("a.mp3", Map.of("title", "Musica", "artist", "Artista"));

        assertEquals("Musica", repo.get("a.mp3").get("title"));
    }

    @Test
    void putAllAndReloadPersistsEntries() {
        FileMetadataCacheRepository repo = new FileMetadataCacheRepository(cacheFile());
        repo.putAll(Map.of(
                "a.mp3", Map.of("title", "A"),
                "b.mp3", Map.of("title", "B")));

        FileMetadataCacheRepository reloaded = new FileMetadataCacheRepository(cacheFile());
        assertEquals("A", reloaded.get("a.mp3").get("title"));
        assertEquals("B", reloaded.get("b.mp3").get("title"));
    }

    @Test
    void overwritesExistingEntry() {
        FileMetadataCacheRepository repo = new FileMetadataCacheRepository(cacheFile());
        repo.put("a.mp3", Map.of("title", "Velho"));
        repo.put("a.mp3", Map.of("title", "Novo"));

        assertEquals("Novo", repo.get("a.mp3").get("title"));
    }

    @Test
    void persistWritesJsonFile() throws Exception {
        FileMetadataCacheRepository repo = new FileMetadataCacheRepository(cacheFile());
        repo.put("a.mp3", Map.of("title", "X"));

        assertNotNull(cacheFile().toFile());
        assertNotNull(Files.readString(cacheFile()));
    }

    @Test
    void locationReturnsCacheFilePath() {
        FileMetadataCacheRepository repo = new FileMetadataCacheRepository(cacheFile());
        assertEquals(cacheFile().toString(), repo.location());
    }

    @Test
    void putAllWithEmptyMapDoesNotPersist() {
        FileMetadataCacheRepository repo = new FileMetadataCacheRepository(cacheFile());

        repo.putAll(Map.of());

        assertFalse(Files.exists(cacheFile()));
    }

    @Test
    void loadWithCorruptJsonIsGraceful() throws Exception {
        Files.writeString(cacheFile(), "{{{not json}}}");

        FileMetadataCacheRepository repo = new FileMetadataCacheRepository(cacheFile());

        assertNull(repo.get("a.mp3"));
    }

    private Path cacheFile() {
        return tempDir.resolve("metadata-cache.json");
    }
}
